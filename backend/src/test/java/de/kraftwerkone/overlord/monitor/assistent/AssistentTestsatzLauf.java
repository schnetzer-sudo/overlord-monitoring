package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assumptions.assumeThat;

import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Ergebnis;
import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Zaehler;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicEigenschaften;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicSprachmodell;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.common.auftrag.DefinitionsPruefung;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * <b>Der Testsatz des Assistenten</b> ({@code docs/assistent.md} §8): misst, wie gut ein Modell
 * echte Anfragen versteht — das Mass fuer jeden Wechsel des Anbieters und jede Aenderung an
 * Anweisung oder Typbeschreibung.
 *
 * <p><b>Laeuft nur ausdruecklich</b>: Der Klassenname passt auf kein Muster von Surefire oder
 * Failsafe, die Klasse traegt {@code @Tag("llm")}, und ohne {@code OVERLORD_ASSISTENT_API_KEY} wird
 * sie uebersprungen. Aufruf:
 *
 * <pre>
 * OVERLORD_ASSISTENT_API_KEY=… ./mvnw test -Djooq.codegen.skip=true -Dtest=AssistentTestsatzLauf \
 *     -Dsurefire.failIfNoSpecifiedTests=false
 * </pre>
 *
 * <p><b>Kein Test im Sinn einer Zusicherung</b>: Er faellt nicht, wenn Faelle scheitern, sondern
 * schreibt einen Bericht nach {@code target/assistent-testsatz/}. Bewertet wird nur strukturell
 * (Typ, Aktion, Eintraege nach Normalisierung, oder wie erwartet kein Vorschlag). Ohne Datenbank:
 * Die Definition von {@code WERK_ABLADESTELLE} kommt aus den Testressourcen.
 *
 * <p><b>Was hinausgeht:</b> die Anweisung, die Werkzeuge und die synthetischen Nachrichten aus
 * {@code faelle.json} — keine Daten aus der Datenbank.
 */
@Tag("llm")
class AssistentTestsatzLauf {

  private static final String FAELLE = "/assistent/testsatz/faelle.json";

  /** Was ein Fall nach einer Nachricht ergeben hat. */
  private record Schritt(
      String nachricht, String antwort, Vorschlag vorschlag, String ausgang, String fehler) {}

  private record Ausgang(String kennung, boolean bestanden, String grund, List<Schritt> schritte) {}

  @Test
  void fahre_den_testsatz() throws IOException {
    String schluessel = System.getenv("OVERLORD_ASSISTENT_API_KEY");
    assumeThat(schluessel).as("OVERLORD_ASSISTENT_API_KEY ist nicht gesetzt").isNotBlank();
    String modellname = umgebung("OVERLORD_ASSISTENT_MODELL", "claude-sonnet-5-5");
    String basisUrl = umgebung("OVERLORD_ASSISTENT_BASIS_URL", "https://api.anthropic.com");

    Sprachmodell modell =
        new AnthropicSprachmodell(
            basisUrl,
            schluessel,
            modellname,
            Duration.ofSeconds(30),
            new AnthropicEigenschaften(umgebung("OVERLORD_ASSISTENT_DENKEN", null), null),
            Skriptmodell.JSON);
    Gespraechsfuehrung fuehrung = new Gespraechsfuehrung(modell, Skriptmodell.JSON, 3, 1024);

    TypDefinition werk =
        DefinitionsPruefung.pruefe(
                Testdefinitionen.WERK,
                Testdefinitionen.WERK_NAME,
                Testdefinitionen.werkBeschreibung(),
                Testdefinitionen.werkBaum())
            .definition();
    Map<String, TypDefinition> angebot = Map.of(werk.typ(), werk);
    String anweisung = Anweisung.fuer(List.of(werk));
    var werkzeuge = new Werkzeugbau(Skriptmodell.JSON).werkzeuge(List.of(werk));

    JsonNode satz = lies();
    List<Ausgang> ausgaenge = new ArrayList<>();
    Zaehler gesamt = new Zaehler();
    long beginn = System.nanoTime();

    for (JsonNode fall : satz.get("faelle")) {
      Gespraech gespraech =
          new Gespraech(
              UUID.randomUUID(), "testsatz", 0L, "TESTSATZ", anweisung, werkzeuge, Instant.EPOCH);
      List<Schritt> schritte = new ArrayList<>();
      for (JsonNode nachricht : fall.get("nachrichten")) {
        Zaehler zaehler = new Zaehler();
        try {
          Ergebnis ergebnis = fuehrung.fuehre(gespraech, nachricht.stringValue(), angebot, zaehler);
          schritte.add(
              new Schritt(
                  nachricht.stringValue(),
                  ergebnis.text(),
                  ergebnis.vorschlag(),
                  ergebnis.ausgang().name(),
                  null));
        } catch (Modellfehler fehler) {
          schritte.add(
              new Schritt(nachricht.stringValue(), null, null, "FEHLER", fehler.art().name()));
        }
        summiere(gesamt, zaehler);
      }
      ausgaenge.add(bewerte(fall, schritte));
    }

    Duration dauer = Duration.ofNanos(System.nanoTime() - beginn);
    schreibeBericht(modellname, ausgaenge, gesamt, dauer);
  }

  // ─── Bewertung ─────────────────────────────────────────────────────────────────────────────

  private static Ausgang bewerte(JsonNode fall, List<Schritt> schritte) {
    String kennung = fall.get("kennung").stringValue();
    for (Schritt schritt : schritte) {
      if (schritt.fehler() != null) {
        return new Ausgang(kennung, false, "Fehler des Anbieters: " + schritt.fehler(), schritte);
      }
    }
    if (fall.has("erwartetNach")) {
      JsonNode erwartungen = fall.get("erwartetNach");
      for (int i = 0; i < schritte.size(); i++) {
        if (!passt(erwartungen.get(i), schritte.get(i).vorschlag())) {
          return new Ausgang(kennung, false, "Nachricht " + (i + 1) + " weicht ab", schritte);
        }
      }
      return new Ausgang(kennung, true, null, schritte);
    }
    Vorschlag letzter = schritte.getLast().vorschlag();
    if (fall.has("erwartetEinerVon")) {
      for (JsonNode erwartet : fall.get("erwartetEinerVon")) {
        if (passt(erwartet, letzter)) {
          return new Ausgang(kennung, true, null, schritte);
        }
      }
      return new Ausgang(kennung, false, "keiner der erlaubten Vorschlaege", schritte);
    }
    boolean bestanden = passt(fall.get("erwartet"), letzter);
    return new Ausgang(kennung, bestanden, bestanden ? null : "weicht ab", schritte);
  }

  /** Strukturell: Typ, Aktion und Eintraege als Menge, Felder ohne Wert entfallen. */
  static boolean passt(JsonNode erwartet, Vorschlag tatsaechlich) {
    JsonNode vorschlag = erwartet.get("vorschlag");
    if (vorschlag == null || vorschlag.isNull()) {
      return tatsaechlich == null;
    }
    if (tatsaechlich == null) {
      return false;
    }
    if (!vorschlag.get("typ").stringValue().equals(tatsaechlich.typ())
        || !vorschlag.get("aktion").stringValue().equals(tatsaechlich.aktion())) {
      return false;
    }
    List<Map<String, String>> erwarteteEintraege = new ArrayList<>();
    for (JsonNode eintrag : vorschlag.get("eintraege")) {
      Map<String, String> werte = new HashMap<>();
      eintrag.properties().forEach(feld -> werte.put(feld.getKey(), feld.getValue().stringValue()));
      erwarteteEintraege.add(werte);
    }
    List<Map<String, String>> tatsaechlicheEintraege = new ArrayList<>();
    for (Vorschlag.Eintrag eintrag : tatsaechlich.eintraege()) {
      Map<String, String> werte = new HashMap<>();
      eintrag
          .werte()
          .forEach(
              (feld, wert) -> {
                if (wert != null && !wert.isEmpty()) {
                  werte.put(feld, wert);
                }
              });
      tatsaechlicheEintraege.add(werte);
    }
    if (erwarteteEintraege.size() != tatsaechlicheEintraege.size()) {
      return false;
    }
    List<Map<String, String>> rest = new ArrayList<>(tatsaechlicheEintraege);
    for (Map<String, String> eintrag : erwarteteEintraege) {
      if (!rest.remove(eintrag)) {
        return false;
      }
    }
    return true;
  }

  // ─── Bericht ───────────────────────────────────────────────────────────────────────────────

  private static void schreibeBericht(
      String modell, List<Ausgang> ausgaenge, Zaehler gesamt, Duration dauer) throws IOException {
    long bestanden = ausgaenge.stream().filter(Ausgang::bestanden).count();
    Instant jetzt = Clock.systemUTC().instant();
    StringBuilder bericht = new StringBuilder();
    bericht
        .append("# Testsatz des Assistenten\n\n")
        .append("- Zeitpunkt (UTC): ")
        .append(jetzt)
        .append("\n- Modell: ")
        .append(modell)
        .append("\n- Bestanden: ")
        .append(bestanden)
        .append(" von ")
        .append(ausgaenge.size())
        .append(String.format(" (%.0f %%)", 100.0 * bestanden / ausgaenge.size()))
        .append("\n- Token ein / aus: ")
        .append(gesamt.tokenEin())
        .append(" / ")
        .append(gesamt.tokenAus())
        .append("\n- Werkzeugaufrufe: ")
        .append(gesamt.werkzeugaufrufe())
        .append("\n- Dauer: ")
        .append(dauer.toMillis())
        .append(" ms\n\n| Fall | Ergebnis | Grund |\n|---|---|---|\n");
    for (Ausgang ausgang : ausgaenge) {
      bericht
          .append("| ")
          .append(ausgang.kennung())
          .append(" | ")
          .append(ausgang.bestanden() ? "bestanden" : "**nicht bestanden**")
          .append(" | ")
          .append(ausgang.grund() == null ? "" : ausgang.grund())
          .append(" |\n");
    }
    for (Ausgang ausgang : ausgaenge) {
      bericht.append("\n## ").append(ausgang.kennung()).append("\n");
      for (Schritt schritt : ausgang.schritte()) {
        bericht
            .append("\n- Ausgang: ")
            .append(schritt.ausgang())
            .append(schritt.fehler() == null ? "" : " (" + schritt.fehler() + ")")
            .append("\n- Antwort: ")
            .append(schritt.antwort() == null ? "—" : schritt.antwort().replace("\n", " "))
            .append("\n- Vorschlag: ")
            .append(
                schritt.vorschlag() == null
                    ? "keiner"
                    : "`" + Skriptmodell.JSON.writeValueAsString(schritt.vorschlag()) + "`")
            .append("\n");
      }
    }
    Path ziel = Path.of("target", "assistent-testsatz");
    Files.createDirectories(ziel);
    String stempel =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(jetzt);
    Path datei = ziel.resolve("bericht-" + stempel + ".md");
    Files.writeString(datei, bericht, StandardCharsets.UTF_8);
    System.out.println("Testsatz: " + bestanden + " von " + ausgaenge.size() + " -> " + datei);
  }

  private static void summiere(Zaehler gesamt, Zaehler einzeln) {
    gesamt.addiere(einzeln);
  }

  private static JsonNode lies() throws IOException {
    try (InputStream quelle = AssistentTestsatzLauf.class.getResourceAsStream(FAELLE)) {
      return Skriptmodell.JSON.readTree(new String(quelle.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  private static String umgebung(String name, String vorgabe) {
    String wert = System.getenv(name);
    return wert == null || wert.isBlank() ? vorgabe : wert;
  }
}
