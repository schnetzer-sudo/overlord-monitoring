package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Ergebnis;
import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Zaehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.auftrag.AssistentFreigabe;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Der Assistent hinter den Endpunkten ({@code docs/assistent.md} §6): verfuegbar oder nicht,
 * Gespraech eroeffnen, Nachricht beantworten, Gespraech beenden.
 *
 * <p><b>Ob der Assistent fuer den Mandanten verfuegbar ist, entscheidet {@link
 * AssistentFreigabe}</b> (E‑1126, E‑1130): Schalter, Schluessel und mindestens ein freigeschalteter
 * Typ. Eroeffnen und Nachricht fragen sie zuerst; nicht verfuegbar ist {@code 404}, und es geht
 * kein Aufruf hinaus. Abgeschaltet steht hinter {@link Sprachmodell} ohnehin kein Adapter (E‑1030).
 *
 * <p><b>Was das Modell bekommt</b> (E‑1035, E‑1059): die Anweisung mit dem Namen des aktiven
 * Mandanten, die Werkzeuge, den Verlauf. Benutzer und Mandanten-ID dienen nur der Bindung des
 * Gespraechs und dem Protokoll; ins Modell geht keiner von beiden. <b>Protokolliert werden nur
 * Metadaten</b> (E‑1039), nie ein Text.
 */
@Service
public class AssistentService {

  private static final Logger log = LoggerFactory.getLogger(AssistentService.class);

  private final AssistentEigenschaften eigenschaften;
  private final AssistentFreigabe freigabe;
  private final Gespraechsspeicher speicher;
  private final Nachrichtenbegrenzung begrenzung;
  private final Gespraechsfuehrung fuehrung;
  private final Werkzeugbau werkzeugbau;
  private final Clock uhr;

  AssistentService(
      AssistentEigenschaften eigenschaften,
      Sprachmodell sprachmodell,
      AssistentFreigabe freigabe,
      Gespraechsspeicher speicher,
      Nachrichtenbegrenzung begrenzung,
      JsonMapper json,
      @Qualifier("systemClock") Clock uhr) {
    this.eigenschaften = eigenschaften;
    this.freigabe = freigabe;
    this.speicher = speicher;
    this.begrenzung = begrenzung;
    this.fuehrung =
        new Gespraechsfuehrung(
            sprachmodell,
            json,
            eigenschaften.werkzeugrundenJeNachricht(),
            eigenschaften.maxTokens());
    this.werkzeugbau = new Werkzeugbau(json);
    this.uhr = uhr;
  }

  /**
   * Ob der Assistent fuer diesen Mandanten verfuegbar ist, und mit welchen Typen — die
   * Selbstauskunft, nach der das Frontend das Symbol zeigt. Nicht verfuegbar ist hier {@code 200}
   * mit {@code verfuegbar: false}. Abgeschaltet ohne Datenbankzugriff.
   */
  public AssistentResponse stand(MandantContext mandant, String sitzungId) {
    speicher.beendeNachMandantenwechsel(mandant, sitzungId);
    List<String> typen = freigabe.angebot(mandant).stream().map(TypDefinition::name).toList();
    return new AssistentResponse(!typen.isEmpty(), typen);
  }

  /**
   * Eroeffnet ein Gespraech; Anweisung und Werkzeuge sind ab hier fest (E‑1045).
   *
   * @param mandantenname der Name des aktiven Mandanten fuer die Anweisung (E‑1059), {@code null}
   *     ohne Namen; nie die Mandanten-ID
   */
  public GespraechResponse eroeffne(
      AngemeldeterNutzer nutzer, MandantContext mandant, String mandantenname, String sitzungId) {
    speicher.beendeNachMandantenwechsel(mandant, sitzungId);
    List<TypDefinition> typen = freigabe.verlangeAngebot(mandant);
    Gespraech gespraech =
        speicher.eroeffne(
            mandant,
            nutzer.id(),
            sitzungId,
            Anweisung.fuer(mandantenname, typen),
            werkzeugbau.werkzeuge(typen));
    return new GespraechResponse(gespraech.id().toString());
  }

  /**
   * Beantwortet eine Nachricht des Kunden. Zuerst die Freigabe: Ist der Assistent fuer den
   * Mandanten nicht (mehr) verfuegbar, ist das {@code 404} wie ein unbekanntes Gespraech, auch fuer
   * ein Gespraech, das noch offen ist. Geprueft wird jeder Vorschlag gegen das Angebot von jetzt
   * (E‑1045).
   */
  public NachrichtResponse sende(
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String sitzungId,
      String gespraechId,
      String text) {
    speicher.beendeNachMandantenwechsel(mandant, sitzungId);
    Map<String, TypDefinition> angebot = nachKennung(freigabe.verlangeAngebot(mandant));
    Gespraech gespraech = gespraech(nutzer, mandant, sitzungId, gespraechId);
    pruefeText(text);
    if (gespraech.kundennachrichten() >= eigenschaften.nachrichtenJeGespraech()) {
      throw new FachlicheAusnahme(
          HttpStatus.TOO_MANY_REQUESTS,
          "gespraech-ausgeschoepft",
          "Gespräch ausgeschöpft",
          "Dieses Gespräch hat die Höchstzahl von "
              + eigenschaften.nachrichtenJeGespraech()
              + " Nachrichten erreicht. Bitte beginnen Sie ein neues Gespräch.",
          "Gespraech mit " + gespraech.kundennachrichten() + " Nachrichten",
          Map.of("hoechstensNachrichten", eigenschaften.nachrichtenJeGespraech()));
    }
    if (!gespraech.beginne()) {
      throw new FachlicheAusnahme(
          HttpStatus.CONFLICT,
          "nachricht-laeuft",
          "Nachricht läuft",
          "Der Assistent beantwortet noch Ihre vorige Nachricht. Bitte warten Sie die Antwort ab.",
          "Zweite Nachricht im selben Gespraech, waehrend eine laeuft");
    }

    long beginn = uhr.millis();
    Zaehler zaehler = new Zaehler();
    String ergebnisName = "FEHLER";
    try {
      begrenzung.pruefeUndZaehle(nutzer.id());
      gespraech.beruehre(uhr.instant());
      Ergebnis ergebnis = fuehrung.fuehre(gespraech, text, angebot, zaehler);
      ergebnisName = ergebnis.ausgang().name();
      return new NachrichtResponse(ergebnis.text(), ergebnis.vorschlag());
    } catch (Modellfehler fehler) {
      ergebnisName = "FEHLER_" + fehler.art().name();
      log.warn("Assistent: Anbieter gescheitert: {}", fehler.getMessage());
      throw gestoert(fehler);
    } catch (FachlicheAusnahme abgewiesen) {
      ergebnisName = "ABGEWIESEN_" + abgewiesen.problemTyp();
      throw abgewiesen;
    } finally {
      gespraech.beruehre(uhr.instant());
      gespraech.gibFrei();
      log.info(
          "Assistent: nutzer={} mandant={} gespraech={} modell={} tokenEin={} tokenAus={}"
              + " dauerMs={} werkzeugaufrufe={} ergebnis={}",
          nutzer.username(),
          mandant.mandantId(),
          gespraech.id(),
          zaehler.modell() == null ? eigenschaften.modell() : zaehler.modell(),
          zaehler.tokenEin(),
          zaehler.tokenAus(),
          uhr.millis() - beginn,
          zaehler.werkzeugaufrufe(),
          ergebnisName);
    }
  }

  /** Beendet das Gespraech. Unbekannt oder fremd: {@code 404}. */
  public void beende(
      AngemeldeterNutzer nutzer, MandantContext mandant, String sitzungId, String gespraechId) {
    speicher.beendeNachMandantenwechsel(mandant, sitzungId);
    UUID id = kennung(gespraechId);
    if (!speicher.beende(mandant, nutzer.id(), sitzungId, id)) {
      throw nichtGefunden();
    }
  }

  // ─── Hilfen ────────────────────────────────────────────────────────────────────────────────

  private static Map<String, TypDefinition> nachKennung(List<TypDefinition> typen) {
    Map<String, TypDefinition> angebot = new LinkedHashMap<>();
    for (TypDefinition typ : typen) {
      angebot.put(typ.typ(), typ);
    }
    return angebot;
  }

  private Gespraech gespraech(
      AngemeldeterNutzer nutzer, MandantContext mandant, String sitzungId, String gespraechId) {
    return speicher
        .finde(mandant, nutzer.id(), sitzungId, kennung(gespraechId))
        .orElseThrow(AssistentService::nichtGefunden);
  }

  /** Eine Kennung, die keine UUID ist, ist ein unbekanntes Gespraech — dieselbe Antwort. */
  private static UUID kennung(String gespraechId) {
    try {
      return UUID.fromString(gespraechId);
    } catch (IllegalArgumentException keineUuid) {
      throw nichtGefunden();
    }
  }

  private void pruefeText(String text) {
    int hoechstens = eigenschaften.nachrichtHoechstensZeichen();
    if (text == null || text.isBlank() || text.codePointCount(0, text.length()) > hoechstens) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "nachricht-ungueltig",
          "Nachricht nicht angenommen",
          "Bitte schreiben Sie eine Nachricht mit höchstens " + hoechstens + " Zeichen.",
          text == null || text.isBlank()
              ? "Nachricht leer"
              : "Nachricht mit " + text.codePointCount(0, text.length()) + " Zeichen",
          Map.of("hoechstensZeichen", hoechstens));
    }
  }

  private static RessourceNichtGefundenException nichtGefunden() {
    return new RessourceNichtGefundenException(
        "Gespraech unbekannt, abgelaufen, beendet oder fremd");
  }

  /** Ohne Einzelheiten des Anbieters: Die stehen nur im Protokoll. */
  private static FachlicheAusnahme gestoert(Modellfehler fehler) {
    if (fehler.art().voruebergehend()) {
      return new FachlicheAusnahme(
          HttpStatus.SERVICE_UNAVAILABLE,
          "assistent-voruebergehend-gestoert",
          "Assistent vorübergehend gestört",
          "Der Assistent antwortet gerade nicht. Bitte schicken Sie Ihre Nachricht in einigen"
              + " Minuten noch einmal.",
          "Modellfehler " + fehler.art());
    }
    return new FachlicheAusnahme(
        HttpStatus.BAD_GATEWAY,
        "assistent-gestoert",
        "Assistent gestört",
        "Der Assistent konnte Ihre Nachricht nicht beantworten. Bitte versuchen Sie es später noch"
            + " einmal.",
        "Modellfehler " + fehler.art());
  }
}
