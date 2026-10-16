package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Ergebnis;
import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Zaehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypKatalog;
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
 * <p><b>Abgeschaltet geht kein Aufruf hinaus</b> (E‑1030): Jeder Weg zum Modell prueft zuerst den
 * Schalter, und abgeschaltet steht hinter {@link Sprachmodell} ohnehin kein Adapter.
 *
 * <p><b>Was das Modell bekommt</b> (E‑1035): die Anweisung, die Werkzeuge, den Verlauf. Benutzer
 * und Mandant dienen nur der Bindung des Gespraechs und dem Protokoll; ins Modell geht keiner von
 * beiden. <b>Protokolliert werden nur Metadaten</b> (E‑1039), nie ein Text.
 */
@Service
public class AssistentService {

  private static final Logger log = LoggerFactory.getLogger(AssistentService.class);

  static final String NICHT_VERFUEGBAR = "assistent-nicht-verfuegbar";

  private final AssistentEigenschaften eigenschaften;
  private final TypKatalog typKatalog;
  private final Gespraechsspeicher speicher;
  private final Nachrichtenbegrenzung begrenzung;
  private final Gespraechsfuehrung fuehrung;
  private final Werkzeugbau werkzeugbau;
  private final Clock uhr;

  AssistentService(
      AssistentEigenschaften eigenschaften,
      Sprachmodell sprachmodell,
      TypKatalog typKatalog,
      Gespraechsspeicher speicher,
      Nachrichtenbegrenzung begrenzung,
      JsonMapper json,
      @Qualifier("systemClock") Clock uhr) {
    this.eigenschaften = eigenschaften;
    this.typKatalog = typKatalog;
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
   * Ob der Assistent fuer diesen Mandanten verfuegbar ist, und mit welchen Typen. Abgeschaltet ohne
   * Datenbankzugriff.
   */
  public AssistentResponse stand(MandantContext mandant, String sitzungId) {
    speicher.beendeNachMandantenwechsel(mandant, sitzungId);
    if (!eigenschaften.eingeschaltet()) {
      return new AssistentResponse(false, List.of());
    }
    List<String> typen = angeboten(mandant).stream().map(TypDefinition::name).toList();
    return new AssistentResponse(!typen.isEmpty(), typen);
  }

  /** Eroeffnet ein Gespraech; Anweisung und Werkzeuge sind ab hier fest (E‑1045). */
  public GespraechResponse eroeffne(
      AngemeldeterNutzer nutzer, MandantContext mandant, String sitzungId) {
    speicher.beendeNachMandantenwechsel(mandant, sitzungId);
    if (!eigenschaften.eingeschaltet()) {
      throw nichtVerfuegbar("Assistent abgeschaltet oder ohne Schluessel");
    }
    List<TypDefinition> typen = angeboten(mandant);
    if (typen.isEmpty()) {
      throw nichtVerfuegbar("Kein Auftragstyp fuer den Assistenten angeboten");
    }
    Gespraech gespraech =
        speicher.eroeffne(
            mandant, nutzer.id(), sitzungId, Anweisung.fuer(typen), werkzeugbau.werkzeuge(typen));
    return new GespraechResponse(gespraech.id().toString());
  }

  /** Beantwortet eine Nachricht des Kunden. */
  public NachrichtResponse sende(
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String sitzungId,
      String gespraechId,
      String text) {
    speicher.beendeNachMandantenwechsel(mandant, sitzungId);
    Gespraech gespraech = gespraech(nutzer, mandant, sitzungId, gespraechId);
    if (!eigenschaften.eingeschaltet()) {
      throw nichtVerfuegbar("Assistent abgeschaltet oder ohne Schluessel");
    }
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
      Ergebnis ergebnis = fuehrung.fuehre(gespraech, text, angebot(mandant), zaehler);
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

  /** Die angebotenen Typen, die der Assistent anbieten kann (E‑1054). */
  private List<TypDefinition> angeboten(MandantContext mandant) {
    return Werkzeugbau.geeignete(
        typKatalog.angeboten(mandant).stream().map(TypKatalog.Angebot::definition).toList());
  }

  private Map<String, TypDefinition> angebot(MandantContext mandant) {
    Map<String, TypDefinition> angebot = new LinkedHashMap<>();
    for (TypDefinition typ : angeboten(mandant)) {
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

  private static FachlicheAusnahme nichtVerfuegbar(String interneUrsache) {
    return new FachlicheAusnahme(
        HttpStatus.SERVICE_UNAVAILABLE,
        NICHT_VERFUEGBAR,
        "Assistent nicht verfügbar",
        "Der Assistent ist zurzeit nicht verfügbar.",
        interneUrsache);
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
