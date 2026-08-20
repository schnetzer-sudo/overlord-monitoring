package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Bestandszeile;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Projektzeile;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Vorschlagszeile;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Die Fachlogik des Prozess-Katalogs: was gepflegt wird, was vorgeschlagen wird, und was
 * unangetastet bleibt.
 *
 * <p><b>Die Uhr ist die Systemuhr</b> ({@code systemClock}) und nicht die Anwendungsuhr. Die
 * Anwendungsuhr ist im Profil {@code dev} um den Rueckstand der Testkopie zurueckversetzt — ein
 * Aenderungszeitpunkt, der Wochen in der Vergangenheit liegt, waere kein Aenderungszeitpunkt. Es
 * ist dieselbe Wahl wie beim {@code audit_log} (Regel A5).
 */
@Service
public class ProzessKatalogService {

  /** {@code process_catalog.partner} ist {@code varchar(100)}. */
  private static final int PARTNER_MAXLAENGE = 100;

  private final ProzessKatalogRepository repository;
  private final AuditLogWriter auditLogWriter;
  private final Clock systemClock;

  ProzessKatalogService(
      ProzessKatalogRepository repository,
      AuditLogWriter auditLogWriter,
      @Qualifier("systemClock") Clock systemClock) {
    this.repository = repository;
    this.auditLogWriter = auditLogWriter;
    this.systemClock = systemClock;
  }

  /**
   * Die Pflegeliste. <b>Ohne Fortschrittszahl daneben</b> (E8): Die volle Liste kommt zurueck und
   * wird vorne gezaehlt. Ein {@code COUNT(*)} als zweite Abfrage fuehrte denselben Filter ein
   * zweites Mal — und zwei Abfragen koennen zwei Antworten geben.
   */
  public List<KatalogzeileResponse> pflegeliste(MandantContext mandant, boolean nurOffene) {
    return repository.findePflegeliste(mandant, nurOffene);
  }

  /** Die abgeleitete Partner-Auswahlliste des aktiven Mandanten (E2). */
  public List<String> partner(MandantContext mandant) {
    return repository.findePartner(mandant);
  }

  /**
   * Setzt Partner und Richtung eines Prozesses und macht ihn gepflegt.
   *
   * <p><b>Ein leerer Partner ist gueltig</b> und bedeutet „hingesehen, es gibt nichts" (E4). Ohne
   * diese Speicherbarkeit stuenden Auffangprozesse dauerhaft auf offen und der Fortschritt
   * erreichte nie sein Ende.
   *
   * <p>Eine {@code processId} ausserhalb des aktiven Mandanten ergibt {@code 404} und nicht {@code
   * 403} — ein {@code 403} verriete, dass es den Prozess gibt.
   */
  @Transactional
  public KatalogzeileResponse zuordnen(
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String processId,
      String partnerRoh,
      String richtungRoh,
      String ip) {
    KatalogzeileResponse vorher =
        repository
            .findeZeile(mandant, processId)
            .orElseThrow(
                () ->
                    new RessourceNichtGefundenException(
                        "Prozess nicht sichtbar oder nicht vorhanden bei der Katalogpflege"));

    String partner = partnerOderNull(partnerRoh);
    Richtung richtung = Richtung.ausText(richtungRoh);

    repository.speichereZuordnung(
        mandant, processId, partner, richtung, jetztUtc(), nutzer.username());
    protokolliere(
        nutzer,
        mandant,
        processId,
        "zugeordnet: Partner "
            + beschriftung(partner)
            + ", Richtung "
            + beschriftung(richtung == null ? null : richtung.name()),
        ip);

    // Die Antwort wird gebaut und NICHT nachgelesen — und das ist keine Sparsamkeit, sondern
    // notwendig: @Transactional bindet ausschliesslich den Schreib-Kontext. Ein Nachlesen liefe
    // ueber den Lese-Pool, also ueber eine ANDERE Verbindung ausserhalb dieser Transaktion, und
    // saehe die eben geschriebene Zeile noch gar nicht (docs/datenzugriff.md §1). Der erste
    // Entwurf hat genau das getan und "OFFEN" zurueckgegeben, obwohl "GEPFLEGT" geschrieben war.
    //
    // Gebaut ist die Antwort trotzdem vollstaendig richtig: Die Stammdatenfelder stammen aus dem
    // Lesevorgang oben, Partner und Richtung sind die eben gesetzten Werte, der Status ist per
    // Definition GEPFLEGT — und die Herkunft bleibt, was sie war. Das gilt in beiden Faellen: Beim
    // Anlegen schreibt das Repository KEINE, und genau das steht in einer Zeile ohne Katalogeintrag
    // auch vorher schon.
    return new KatalogzeileResponse(
        vorher.processId(),
        vorher.projectId(),
        vorher.projectName(),
        vorher.processName(),
        partner,
        richtung,
        Pflegestatus.GEPFLEGT,
        vorher.vorschlagHerkunft());
  }

  /**
   * Setzt <b>ein</b> Feld fuer alle Prozesse eines Projekts (E11) — in einem von zwei Modi.
   *
   * <p><b>Beide Modi teilen ein Statement und dieselbe Bedingung:</b> Die Vorschau ruft {@code
   * findeProjektbestand}, und die Ausfuehrung ruft ueber {@code speichereFeld} genau dieselbe
   * Methode. Getrennt gebaut driften die beiden auseinander, und der Nutzer bestaetigt dann eine
   * Zahl, die nicht die ist, die passiert.
   */
  @Transactional
  public MassenzuordnungResponse massenzuordnung(
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String projectId,
      String feldRoh,
      String wertRoh,
      String modusRoh,
      String ip) {
    if (projectId == null || projectId.isBlank()) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "projekt-fehlt",
          "Projekt fehlt",
          "Waehle das Projekt, fuer das zugeordnet werden soll.",
          "Massenzuordnung ohne Projekt");
    }
    Zuordnungsfeld feld = Zuordnungsfeld.ausText(feldRoh);
    Massenmodus modus = Massenmodus.ausText(modusRoh);
    String wert = wertFuer(feld, wertRoh);

    if (!repository.projektGehoertZumMandanten(mandant, projectId)) {
      throw new RessourceNichtGefundenException(
          "Projekt nicht sichtbar oder nicht vorhanden bei der Massenzuordnung");
    }

    List<Projektzeile> betroffen =
        modus == Massenmodus.VORSCHAU
            ? repository.findeProjektbestand(mandant, projectId)
            : repository.speichereFeld(
                mandant, projectId, feld, wert, jetztUtc(), nutzer.username());
    int gepflegt =
        (int)
            betroffen.stream()
                .filter(zeile -> zeile.pflegestatus() == Pflegestatus.GEPFLEGT)
                .count();

    if (modus == Massenmodus.AUSFUEHREN) {
      protokolliere(
          nutzer,
          mandant,
          projectId,
          "Massenzuordnung: Feld "
              + feld.name()
              + " auf "
              + beschriftung(wert)
              + " fuer "
              + betroffen.size()
              + " Prozesse, davon "
              + gepflegt
              + " zuvor gepflegt",
          ip);
    }
    return new MassenzuordnungResponse(modus, projectId, feld, wert, betroffen.size(), gepflegt);
  }

  /**
   * Der Heuristik-Lauf fuer den aktiven Mandanten (E13) — <b>wiederholbar, per Knopf</b>.
   *
   * <table border="1">
   *   <caption>Was er mit welcher Zeile macht</caption>
   *   <tr><td>legt an</td><td>fehlende Zeilen</td></tr>
   *   <tr><td>frischt auf</td><td>Zeilen mit {@link Pflegestatus#OFFEN}, <b>auch wenn sie schon
   *       einen Vorschlag tragen</b> — sonst friert der erste Lauf jeden spaeteren Regelfehler
   *       ein</td></tr>
   *   <tr><td>ruehrt nie an</td><td>Zeilen mit {@link Pflegestatus#GEPFLEGT}</td></tr>
   * </table>
   *
   * <p><b>Nicht beim Anwendungsstart.</b> Er liefe bei jedem Neustart ueber 1.503 Zeilen, obwohl
   * neue Prozesse selten entstehen — und ein Schreibzugriff im Startpfad ist die Sorte
   * Nebenwirkung, die man ein Jahr spaeter nicht mehr erwartet. Es gibt deshalb keinen {@code
   * ApplicationRunner}, kein {@code @PostConstruct} und keinen Scheduler.
   */
  @Transactional
  public VorschlagslaufResponse vorschlagen(
      AngemeldeterNutzer nutzer, MandantContext mandant, String ip) {
    List<Bestandszeile> bestand = repository.findeBestand(mandant);

    List<Vorschlagszeile> zuSchreiben = new ArrayList<>();
    int angelegt = 0;
    int aufgefrischt = 0;
    int unberuehrt = 0;
    int regelA = 0;
    int regelB = 0;
    int keine = 0;

    for (Bestandszeile zeile : bestand) {
      if (zeile.gespeicherterStatus() == Pflegestatus.GEPFLEGT) {
        unberuehrt++;
        continue;
      }
      if (zeile.gespeicherterStatus() == null) {
        angelegt++;
      } else {
        aufgefrischt++;
      }
      Partnervorschlag vorschlag =
          Partnerheuristik.vorschlag(mandant.mandantId(), zeile.processId(), zeile.projectId());
      switch (vorschlag.herkunft()) {
        case REGEL_A -> regelA++;
        case REGEL_B -> regelB++;
        case KEINE -> keine++;
      }
      zuSchreiben.add(new Vorschlagszeile(zeile.processId(), vorschlag));
    }

    repository.speichereVorschlaege(mandant, zuSchreiben, jetztUtc(), nutzer.username());
    protokolliere(
        nutzer,
        mandant,
        mandant.mandantId(),
        "Heuristik-Lauf: "
            + angelegt
            + " angelegt, "
            + aufgefrischt
            + " aufgefrischt, "
            + unberuehrt
            + " unberuehrt (Regel A "
            + regelA
            + ", Regel B "
            + regelB
            + ", keine "
            + keine
            + ")",
        ip);
    return new VorschlagslaufResponse(angelegt, aufgefrischt, unberuehrt, regelA, regelB, keine);
  }

  /**
   * Leerraum ist keine Angabe. Ein Partner aus Leerzeichen waere ein gefuelltes Feld ohne Inhalt —
   * und damit ein dritter Zustand durch die Hintertuer.
   */
  private static String partnerOderNull(String roh) {
    if (roh == null || roh.isBlank()) {
      return null;
    }
    String wert = roh.trim();
    if (wert.length() > PARTNER_MAXLAENGE) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "partner-zu-lang",
          "Partner zu lang",
          "Ein Partnername darf hoechstens " + PARTNER_MAXLAENGE + " Zeichen haben.",
          "Partnername ueber der Spaltenbreite");
    }
    return wert;
  }

  /**
   * Der zu setzende Wert, je Feld verschieden geprueft: Der Partner ist freier Text, die Richtung
   * eine geschlossene Menge. Ein unbekannter Richtungswert ist {@code 400} und nicht
   * stillschweigend leer — sonst leerte ein Tippfehler ein ganzes Projekt.
   */
  private static String wertFuer(Zuordnungsfeld feld, String roh) {
    if (feld == Zuordnungsfeld.RICHTUNG) {
      Richtung richtung = Richtung.ausText(roh);
      return richtung == null ? null : richtung.name();
    }
    return partnerOderNull(roh);
  }

  /** Ein leeres Feld heisst im Protokoll „leer" und nicht {@code null}. */
  private static String beschriftung(String wert) {
    return wert == null ? "(leer)" : wert;
  }

  private LocalDateTime jetztUtc() {
    return LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
  }

  /**
   * <b>Jede schreibende Katalogaenderung wird protokolliert</b>, nicht nur die einzelne Zuordnung:
   * Eine Massenzuordnung ueberschreibt bis zu 226 gepflegte Zeilen (E12) und ein Heuristik-Lauf
   * beruehrt jede offene Zeile eines Mandanten. Waeren die beiden nicht im Protokoll, waere
   * ausgerechnet die folgenreichste Aenderung die unsichtbarste.
   */
  private void protokolliere(
      AngemeldeterNutzer nutzer, MandantContext mandant, String ziel, String detail, String ip) {
    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.KATALOG_GEAENDERT,
            nutzer.id(),
            nutzer.username(),
            mandant.mandantId(),
            "process_catalog",
            ziel,
            ip,
            detail));
  }
}
