package de.kraftwerkone.overlord.monitor.payload;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Die Fachlogik des Rohdatenzugriffs: auflisten, abrufen, aufbereiten, protokollieren.
 *
 * <h2>Die Kette</h2>
 *
 * <ol>
 *   <li>Mandantenpruefung <b>im Statement</b> — {@link ArtefaktRepository}, nicht hier.
 *   <li>Artefakte lesen, ausschliesslich ueber die {@code MessageID} (Regel L4).
 *   <li>Verweis an der Pipe zerlegen, Kennung ueber {@code Service} aufloesen. Keine Aufloesung →
 *       {@link Artefaktzustand#ABLAGE_NICHT_ERREICHBAR}, <b>kein Rueckfall auf eine andere
 *       Ablage</b> (M68).
 *   <li>SOAP-{@code RETRIEVE} mit der nackten UUID, gegen den unveraenderten {@code
 *       ServiceConnectString} (Q1).
 *   <li>Anhang lesen, ZIP entpacken, ersten Eintrag verwenden. Mehr als einer wird <b>vermerkt und
 *       protokolliert</b>, nicht stillschweigend verworfen.
 *   <li>Binaerpruefung, dann Kodierung {@code ISO-8859-1}, dann gegebenenfalls Beschnitt.
 * </ol>
 *
 * <h2>Die Rolle kommt aus der Sitzung</h2>
 *
 * <p><b>Niemals aus einem Parameter.</b> Im Altsystem entscheidet {@code
 * request.getParameter("downloadUser")} darueber, wer beschnitten bekommt ({@code
 * JsonServlet.java:788}–{@code :790}, Q5) — wer den Parameter aendert, aendert damit seine Rolle.
 * Hier kommt sie aus {@link AngemeldeterNutzer}, und der stammt aus dem serverseitigen {@code
 * SecurityContext}. Es gibt keinen Weg, sie von aussen zu setzen; ein Test belegt das.
 *
 * <h2>Gleichlauf</h2>
 *
 * <p>Anzeige und Download gehen durch <b>dieselbe</b> Aufbereitung ({@link #hole}). Fuer {@code
 * MANDANT} bei Protokollen ist das die beschnittene und maskierte Fassung — auf beiden Wegen. Es
 * gibt keinen Pfad, auf dem ein Mandantennutzer die vollstaendige Protokolldatei bekommt.
 */
@Service
public class ArtefaktService {

  private static final Logger log = LoggerFactory.getLogger(ArtefaktService.class);

  /**
   * Die Kodierung, mit der Artefakte dekodiert werden.
   *
   * <p><b>Gemessen, nicht geraten:</b> 8 von 8 Protokollen und 9 von 16 Nutzdateien sind
   * <b>kein</b> gueltiges UTF-8 (M61). Das Altsystem dekodiert an derselben Stelle hart mit {@code
   * ISO-8859-1} ({@code JsonServlet.java:812}–{@code :813}, Q4). Sie wird nicht zur Laufzeit
   * erraten: Ein Erkennungsversuch fiele bei reinem ASCII — und das ist die Mehrheit — willkuerlich
   * aus, weil ASCII zugleich gueltiges UTF-8 <b>und</b> gueltiges {@code ISO-8859-1} ist (M71,
   * Befund 3).
   */
  static final Charset KODIERUNG = StandardCharsets.ISO_8859_1;

  static final String KODIERUNG_NAME = "ISO-8859-1";

  private static final String ZIEL_TYP = "rohdaten-artefakt";

  private final ArtefaktRepository artefaktRepository;
  private final Ablagezugriff ablagezugriff;
  private final AuditLogWriter auditLogWriter;
  private final RohdatenEigenschaften eigenschaften;

  ArtefaktService(
      ArtefaktRepository artefaktRepository,
      Ablagezugriff ablagezugriff,
      AuditLogWriter auditLogWriter,
      RohdatenEigenschaften eigenschaften) {
    this.artefaktRepository = artefaktRepository;
    this.ablagezugriff = ablagezugriff;
    this.auditLogWriter = auditLogWriter;
    this.eigenschaften = eigenschaften;
  }

  // ─── Endpunkt 1: die Artefaktliste ────────────────────────────────────────────

  /**
   * Alle Artefakte einer Nachricht, zweigeteilt.
   *
   * <p><b>Kein Abruf bei der Ablage.</b> Die Liste entsteht ausschliesslich aus {@code
   * MessageProperty}; ob hinter einem Verweis noch eine Datei liegt, sagt sie nicht. Sie zu
   * beantworten kostete drei bis fuenfzehn SOAP-Aufrufe je Nachricht (M55) und liesse den Endpunkt
   * an der Erreichbarkeit einer fremden Anlage haengen.
   *
   * @throws RessourceNichtGefundenException {@code 404} — fuer eine erfundene {@code MessageID}
   *     genauso wie fuer die eines fremden Mandanten, mit <b>derselben</b> Antwort
   */
  public ArtefaktlisteResponse liste(MandantContext mandant, Rolle rolle, String messageId) {
    List<Artefaktzeile> zeilen = artefaktRepository.findeArtefakte(mandant, messageId);
    if (zeilen.isEmpty() && !artefaktRepository.existiert(mandant, messageId)) {
      throw new RessourceNichtGefundenException("Nachricht nicht sichtbar oder nicht vorhanden");
    }

    List<ArtefaktResponse> nutzdaten = new ArrayList<>();
    List<ArtefaktResponse> protokolle = new ArrayList<>();

    for (Artefaktzeile zeile : zeilen) {
      Artefaktart art = Artefaktnamen.art(zeile.name());
      if (art == null) {
        // Kann nicht vorkommen: das Repository laesst nur die beiden Muster durch. Die Zeile steht
        // hier, damit eine kuenftige Aenderung dort nicht still eine NullPointerException erzeugt.
        continue;
      }
      ArtefaktResponse antwort =
          new ArtefaktResponse(
              zeile.id().kodiere(),
              zeile.name(),
              Artefaktnamen.familie(zeile.name()),
              art,
              zeile.schritt(),
              beschnittGreift(art, rolle));
      if (art == Artefaktart.NUTZDATEN) {
        nutzdaten.add(antwort);
      } else {
        protokolle.add(antwort);
      }
    }
    return new ArtefaktlisteResponse(messageId, List.copyOf(nutzdaten), List.copyOf(protokolle));
  }

  // ─── Endpunkt 2: die Anzeige ──────────────────────────────────────────────────

  /**
   * Ein Artefakt als Text.
   *
   * <p>Antwortet in <b>allen</b> Zustaenden mit {@code 200} und dem benannten Zustand — auch dann,
   * wenn nichts anzuzeigen ist. Ein Fehlerstatus waere hier falsch: „Protokoll ohne Marken" ist
   * kein Fehler, sondern bei {@code FTPSender} der Normalfall (M63), und er darf sich fuer den
   * Nutzer nicht von „Nachricht gibt es nicht" ununterscheidbar anfuehlen.
   */
  public AnzeigeResponse anzeige(
      MandantContext mandant,
      AngemeldeterNutzer nutzer,
      String messageId,
      String artefaktId,
      String ip) {
    Artefaktzeile zeile = zeile(mandant, messageId, artefaktId);
    Artefaktart art = Artefaktnamen.art(zeile.name());
    Artefaktinhalt inhalt = hole(mandant, nutzer.rolle(), messageId, zeile);

    protokolliere(
        inhalt.zustand() == Artefaktzustand.ABLAGE_NICHT_ERREICHBAR
                || inhalt.zustand() == Artefaktzustand.DATEI_NICHT_VORHANDEN
            ? AuditEventType.ROHDATEN_ABRUF_FEHLGESCHLAGEN
            : AuditEventType.ROHDATEN_ANGESEHEN,
        nutzer,
        mandant,
        messageId,
        zeile,
        inhalt,
        ip);

    return new AnzeigeResponse(
        zeile.id().kodiere(),
        zeile.name(),
        art,
        inhalt.zustand(),
        inhalt.text(),
        inhalt.groesseBytes(),
        inhalt.gekuerzt(),
        inhalt.beschnitten(),
        KODIERUNG_NAME,
        inhalt.zipEintraege());
  }

  // ─── Endpunkt 3: der Download ─────────────────────────────────────────────────

  /**
   * Was der Download ausliefert.
   *
   * @param dateiname der Originalname, wo vorhanden, sonst ein konstruierter
   * @param bytes der Inhalt — fuer {@code MANDANT} bei Protokollen die beschnittene und maskierte
   *     Fassung, sonst die Datei
   */
  public record Download(String dateiname, byte[] bytes) {}

  /**
   * Ein Artefakt als Datei.
   *
   * @throws AbrufFehlgeschlagenException wenn es nichts auszuliefern gibt — mit eigenem Problemtyp
   *     je Zustand
   */
  public Download download(
      MandantContext mandant,
      AngemeldeterNutzer nutzer,
      String messageId,
      String artefaktId,
      String ip) {
    Artefaktzeile zeile = zeile(mandant, messageId, artefaktId);
    Artefaktinhalt inhalt = hole(mandant, nutzer.rolle(), messageId, zeile);

    if (!inhalt.lieferbar()) {
      protokolliere(
          AuditEventType.ROHDATEN_ABRUF_FEHLGESCHLAGEN,
          nutzer,
          mandant,
          messageId,
          zeile,
          inhalt,
          ip);
      throw new AbrufFehlgeschlagenException(inhalt.zustand());
    }

    protokolliere(AuditEventType.ROHDATEN_DOWNLOAD, nutzer, mandant, messageId, zeile, inhalt, ip);

    String originalname =
        artefaktRepository.findeOriginaldateiname(mandant, messageId, zeile.schritt()).orElse(null);

    return new Download(
        Downloaddateiname.baue(originalname, zeile.name(), zeile.schritt(), messageId),
        inhalt.bytes());
  }

  // ─── Der gemeinsame Weg ───────────────────────────────────────────────────────

  /**
   * Die Zeile zu einer Kennung — oder {@code 404}.
   *
   * <p><b>Hier wird der Verweis hergeleitet und nicht entgegengenommen.</b> Gesucht wird in der
   * Menge der Artefakte <i>dieser</i> Nachricht, und die ist bereits mandantengefiltert. Eine
   * erfundene Kennung findet nichts; eine Kennung aus einer fremden Nachricht ebenso wenig, weil
   * die Menge fremde Zeilen gar nicht erst enthaelt.
   */
  private Artefaktzeile zeile(MandantContext mandant, String messageId, String artefaktId) {
    ArtefaktId kennung =
        ArtefaktId.entschluessle(artefaktId)
            .orElseThrow(() -> new RessourceNichtGefundenException("Artefaktkennung unbrauchbar"));
    return artefaktRepository.findeArtefakte(mandant, messageId).stream()
        .filter(
            zeile -> zeile.schritt() == kennung.schritt() && zeile.name().equals(kennung.name()))
        .findFirst()
        .orElseThrow(
            () ->
                new RessourceNichtGefundenException(
                    "Artefakt nicht sichtbar oder nicht vorhanden"));
  }

  /**
   * Abrufen und aufbereiten — die eine Fassung fuer Anzeige und Download.
   *
   * <p>Die Reihenfolge ist die aus {@code docs/rohdaten.md} §4 und nicht verhandelbar:
   * <b>Binaerpruefung, dann Kodierung, dann Beschnitt.</b> Umgekehrt liefe der Beschnitt auf einem
   * Text, der aus Binaerbytes entstanden ist, und suchte Marken in Zeichenmuell.
   */
  private Artefaktinhalt hole(
      MandantContext mandant, Rolle rolle, String messageId, Artefaktzeile zeile) {
    Artefaktart art = Artefaktnamen.art(zeile.name());
    boolean beschneiden = beschnittGreift(art, rolle);

    Optional<Artefaktverweis> verweis = Artefaktverweis.zerlege(zeile.verweis());
    if (verweis.isEmpty()) {
      log.warn("Artefaktverweis nicht zerlegbar: {} auf Schritt {}", zeile.name(), zeile.schritt());
      return Artefaktinhalt.ohneInhalt(Artefaktzustand.ABLAGE_NICHT_ERREICHBAR, beschneiden);
    }

    Optional<String> verbindung =
        artefaktRepository.findeVerbindung(mandant, messageId, verweis.get().ablage());
    if (verbindung.isEmpty()) {
      // Keine Aufloesung. KEIN Rueckfall auf eine andere Ablage: 20 von 20 Kreuzabrufen scheitern,
      // dieselben Verweise gelingen am eigenen Knoten (M68). Die Ablagen sind keine Spiegel.
      log.warn("Ablagenkennung loest nicht auf. Kein Rueckfall auf eine andere Ablage.");
      return Artefaktinhalt.ohneInhalt(Artefaktzustand.ABLAGE_NICHT_ERREICHBAR, beschneiden);
    }

    Abrufergebnis abruf = ablagezugriff.hole(verbindung.get(), verweis.get().uuid());
    if (!abruf.erfolgreich()) {
      return Artefaktinhalt.ohneInhalt(abruf.zustand(), beschneiden);
    }

    Zipentnahme.Inhalt entpackt =
        Zipentnahme.ersterEintrag(abruf.zip(), eigenschaften.maximalgroesseBytes());
    if (entpackt == null) {
      // Unlesbares Archiv, kein Eintrag, oder ueber der Obergrenze. Die Ablage hat geliefert, aber
      // wir koennen damit nichts anfangen — fuer den Betrieb dieselbe Lage wie ein Knoten, der
      // nicht antwortet.
      log.warn("Der Anhang liess sich nicht entpacken oder ueberschreitet die Obergrenze.");
      return Artefaktinhalt.ohneInhalt(Artefaktzustand.ABLAGE_NICHT_ERREICHBAR, beschneiden);
    }
    if (entpackt.eintraege() > 1) {
      // In 693 geholten Dateien nie vorgekommen. Das Altsystem verwirft den Rest stillschweigend
      // (:801–:802); hier steht die Zahl in der Antwort und diese Zeile im Protokoll.
      log.warn(
          "Archiv mit {} Eintraegen fuer {} auf Schritt {} — verwendet wird der erste."
              + " Dieser Fall ist in 693 gemessenen Dateien nie aufgetreten.",
          entpackt.eintraege(),
          zeile.name(),
          zeile.schritt());
    }

    byte[] daten = entpackt.daten();
    long groesse = daten.length;
    boolean binaer = Binaerpruefung.istBinaer(daten);

    if (!beschneiden) {
      if (binaer) {
        // Nicht anzeigen, aber benennen — und herunterladen lassen. Hier ist das unbedenklich:
        // Dieser Zweig laeuft nur fuer Nutzdaten oder fuer ADMIN, und beide bekommen die Datei
        // ohnehin vollstaendig.
        return new Artefaktinhalt(
            Artefaktzustand.BINAERDATEI, daten, "", groesse, false, false, entpackt.eintraege());
      }
      String text = new String(daten, KODIERUNG);
      // Vollstaendig. Der Download liefert die Bytes des ZIP-Eintrags unveraendert — nicht den
      // zurueckkodierten Text: Was aus der Ablage kam, soll auch ankommen.
      return new Artefaktinhalt(
          Artefaktzustand.ANZEIGBAR,
          daten,
          gekuerzt(text),
          groesse,
          kuerzungGreift(text),
          false,
          entpackt.eintraege());
    }

    // ─── Ab hier ausschliesslich: MANDANT, Protokoll ──────────────────────────────
    //
    // An dieser Stelle darf KEIN Zweig die rohen Bytes zurueckgeben. Der Binaerfall ist genau
    // deshalb hierher gewandert und steht nicht mehr davor: Stuende er vorn, bekaeme ein
    // Mandantennutzer die vollstaendige, unmaskierte Protokolldatei, sobald sie als binaer
    // eingestuft wird — und die Einstufung haengt am Inhalt, den das Protokoll teilweise aus der
    // EDI-Datei des Partners echot. Ein einziges Nullbyte in einem echoten Wert genuegte.
    //
    // Dass 0 % der gemessenen Protokolle binaer sind (M61, 8 von 8), ist eine Beobachtung an acht
    // Dateien und keine Zusage. Entscheidung 9 sagt „es gibt keinen Pfad" — dann darf es auch
    // keinen geben, der nur selten begangen wird.
    if (binaer) {
      // Eine binaere Datei hat keinen Innenbereich zwischen Marken. Es gibt also nichts, was
      // dieser Aufrufer bekommen koennte — benannt, aber ohne Bytes.
      log.warn(
          "Protokoll {} auf Schritt {} ist binaer und damit fuer MANDANT nicht beschneidbar."
              + " In 8 von 8 gemessenen Protokollen ist dieser Fall nicht vorgekommen (M61).",
          zeile.name(),
          zeile.schritt());
      return Artefaktinhalt.ohneInhalt(Artefaktzustand.BINAERDATEI, true);
    }

    String text = new String(daten, KODIERUNG);
    Protokollbeschnitt.Ergebnis beschnitten = Protokollbeschnitt.beschneide(text);
    if (beschnitten.zustand() != Artefaktzustand.ANZEIGBAR) {
      return Artefaktinhalt.ohneInhalt(beschnitten.zustand(), true);
    }
    // Der Download bekommt genau denselben Ausschnitt, nur zurueckkodiert. Das ist der Gleichlauf:
    // es gibt keinen Pfad, auf dem MANDANT die vollstaendige Protokolldatei bekommt.
    return new Artefaktinhalt(
        Artefaktzustand.ANZEIGBAR,
        beschnitten.text().getBytes(KODIERUNG),
        gekuerzt(beschnitten.text()),
        groesse,
        kuerzungGreift(beschnitten.text()),
        true,
        entpackt.eintraege());
  }

  /**
   * Der Beschnitt greift <b>nur</b> bei Protokollen und <b>nur</b> fuer {@code MANDANT} — ueber die
   * Rolle, nicht ueber ein Kennzeichen aus der Anfrage ({@code docs/rohdaten.md} §3, Entscheidung
   * 3).
   */
  private static boolean beschnittGreift(Artefaktart art, Rolle rolle) {
    return art == Artefaktart.PROTOKOLL && rolle == Rolle.MANDANT;
  }

  /**
   * Kappt die <b>Anzeige</b> an der Laengengrenze.
   *
   * <p>Gerechnet wird in Bytes der Zielkodierung, nicht in Zeichen — die Grenze soll die Antwort
   * begrenzen, und die traegt Bytes. Da {@code ISO-8859-1} jedes Zeichen auf genau ein Byte
   * abbildet, sind die beiden Zahlen hier ohnehin gleich; die Rechnung steht trotzdem so da, damit
   * ein Wechsel der Kodierung sie nicht still verschiebt.
   */
  private String gekuerzt(String text) {
    long grenze = eigenschaften.anzeigeGrenzeBytes();
    return text.length() > grenze ? text.substring(0, (int) grenze) : text;
  }

  private boolean kuerzungGreift(String text) {
    return text.length() > eigenschaften.anzeigeGrenzeBytes();
  }

  /**
   * Schreibt den Protokolleintrag.
   *
   * <p><b>Die Fassung ist der Punkt</b> ({@code docs/rohdaten.md} §10). Ein Eintrag, der
   * beschnitten und vollstaendig nicht unterscheidet, ist bei einer Rueckfrage wertlos — und die
   * Rueckfrage ist genau der Grund, warum es das Protokoll gibt. Beim Fehlschlag steht statt der
   * Fassung der Zustand.
   *
   * <p>Im Detail steht <b>kein</b> Dateiinhalt, kein Dateiname, keine UUID, keine Ablagenkennung
   * und keine Adresse. Geschrieben wird ueber den Schreib-Kontext auf {@code overlord_monitor};
   * {@code GlassfishDB} wird in keiner Form beschrieben.
   */
  private void protokolliere(
      AuditEventType typ,
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String messageId,
      Artefaktzeile zeile,
      Artefaktinhalt inhalt,
      String ip) {
    String detail =
        typ == AuditEventType.ROHDATEN_ABRUF_FEHLGESCHLAGEN
            ? "Zustand: " + inhalt.zustand()
            : "Fassung: " + (inhalt.beschnitten() ? "beschnitten" : "vollstaendig");
    auditLogWriter.schreibe(
        new AuditEvent(
            typ,
            nutzer.id(),
            nutzer.username(),
            mandant.mandantId(),
            ZIEL_TYP,
            messageId + "#" + zeile.id().kodiere(),
            ip,
            detail));
  }
}
