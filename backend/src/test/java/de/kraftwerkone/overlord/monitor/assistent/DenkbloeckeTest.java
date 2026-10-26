package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Ausgang;
import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Ergebnis;
import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Zaehler;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicEigenschaften;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicSprachmodell;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicStub;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Denkbloecke ueber Werkzeugrunden und Nachrichten hinweg ({@code docs/assistent.md} §4.3, E‑1153):
 * die Gespraechsfuehrung mit dem echten Adapter gegen einen lokalen HTTP-Stub.
 *
 * <p>Mit {@code adaptive} denkt das Modell vor jeder Antwort, auch vor einer reinen Textantwort.
 * Was es denkt, ist nicht fuer den Kunden: <b>Kein Denkblock landet im Antworttext.</b> Und
 * Anthropic verlangt, dass jeder Denkblock <b>unveraendert</b> zurueckkommt, in jeder folgenden
 * Anfrage desselben Gespraechs — mit Signatur bzw. verschluesselten Daten, an seiner Stelle
 * zwischen den anderen Bloecken. Kein Aufruf verlaesst den Rechner.
 */
class DenkbloeckeTest {

  /** Runde 1: Denken, verschluesseltes Denken, dann der Vorschlag. */
  private static final String RUNDE_1 =
      """
      {"id":"msg_1","type":"message","role":"assistant","model":"claude-sonnet-5-5",
       "content":[
         {"type":"thinking","thinking":"Der Kunde nennt 010/18VZE. Ich muss genau lesen.",
          "signature":"sig-1"},
         {"type":"redacted_thinking","data":"verschluesselt-1"},
         {"type":"tool_use","id":"toolu_1","name":"WERK_ABLADESTELLE",
          "input":{"aktion":"Anpassung",
                   "eintraege":[{"werk":"010","abladestelle":"18VZE","sndprn":"123456"}]}}],
       "stop_reason":"tool_use","usage":{"input_tokens":900,"output_tokens":120}}
      """;

  /** Runde 2: Denken vor der Textantwort nach dem gueltigen Vorschlag. */
  private static final String RUNDE_2 =
      """
      {"id":"msg_2","type":"message","role":"assistant","model":"claude-sonnet-5-5",
       "content":[
         {"type":"thinking","thinking":"Laut Regel kurz antworten.","signature":"sig-2"},
         {"type":"text","text":"Bitte prüfen Sie die Einträge auf der Karte."}],
       "stop_reason":"end_turn","usage":{"input_tokens":1100,"output_tokens":40}}
      """;

  /** Die zweite Nachricht des Kunden: nur verschluesseltes Denken vor dem Text. */
  private static final String NACHRICHT_2 =
      """
      {"id":"msg_3","type":"message","role":"assistant","model":"claude-sonnet-5-5",
       "content":[
         {"type":"redacted_thinking","data":"verschluesselt-3"},
         {"type":"text","text":"Gern."}],
       "stop_reason":"end_turn","usage":{"input_tokens":1200,"output_tokens":10}}
      """;

  private AnthropicStub stub;
  private Gespraechsfuehrung fuehrung;
  private Gespraech gespraech;
  private Map<String, TypDefinition> angebot;

  @BeforeEach
  void aufbau() throws IOException {
    stub = new AnthropicStub();
    AnthropicSprachmodell adapter =
        new AnthropicSprachmodell(
            stub.basisUrl(),
            "test-schluessel-ohne-wert",
            "claude-sonnet-5-5",
            Duration.ofSeconds(5),
            new AnthropicEigenschaften("adaptive", Duration.ofMillis(10)),
            Skriptmodell.JSON);
    fuehrung = new Gespraechsfuehrung(adapter, Skriptmodell.JSON, 3, 8192);
    TypDefinition werk = Testdefinitionen.werk();
    angebot = Map.of(werk.typ(), werk);
    gespraech =
        new Gespraech(
            UUID.randomUUID(),
            "sitzung",
            1L,
            "NEXANS",
            Anweisung.fuer("Beispielkunde GmbH", List.of(werk)),
            new Werkzeugbau(Skriptmodell.JSON).werkzeuge(List.of(werk)),
            Instant.EPOCH);
  }

  @AfterEach
  void stoppe() {
    stub.close();
  }

  @Test
  @DisplayName(
      "Denkbloecke landen nie im Antworttext und gehen in jeder folgenden Anfrage unveraendert"
          + " zurueck, auch ueber eine Werkzeugrunde")
  void denkbloecke_ueber_werkzeugrunden() {
    stub.antworte(200, RUNDE_1).antworte(200, RUNDE_2).antworte(200, NACHRICHT_2);

    Ergebnis erste =
        fuehrung.fuehre(gespraech, "Bei 010/18VZE SNDPRN auf 123456", angebot, new Zaehler());
    Ergebnis zweite = fuehrung.fuehre(gespraech, "Danke", angebot, new Zaehler());

    // Der Kunde sieht nur den Text.
    assertThat(erste.ausgang()).isEqualTo(Ausgang.VORSCHLAG);
    assertThat(erste.text()).isEqualTo("Bitte prüfen Sie die Einträge auf der Karte.");
    assertThat(zweite.text()).isEqualTo("Gern.");
    for (String text : List.of(erste.text(), zweite.text())) {
      assertThat(text).doesNotContain("Kunde", "genau lesen", "Laut Regel", "verschluesselt");
    }

    List<AnthropicStub.Anfrage> anfragen = stub.anfragen();
    assertThat(anfragen).hasSize(3);
    for (AnthropicStub.Anfrage anfrage : anfragen) {
      assertThat(rumpf(anfrage).at("/thinking/type").asString()).isEqualTo("adaptive");
    }

    // Die Werkzeugrunde: Runde 1 kommt zeichengleich zurueck, dahinter das Ergebnis.
    JsonNode zweiteAnfrage = rumpf(anfragen.get(1)).get("messages");
    assertThat(zweiteAnfrage.size()).isEqualTo(3);
    assertThat(zweiteAnfrage.get(1).get("role").asString()).isEqualTo("assistant");
    assertThat(inhalt(zweiteAnfrage.get(1))).isEqualTo(inhaltDerAntwort(RUNDE_1));
    assertThat(zweiteAnfrage.at("/2/content/0/type").asString()).isEqualTo("tool_result");
    assertThat(zweiteAnfrage.at("/2/content/0/tool_use_id").asString()).isEqualTo("toolu_1");
    assertThat(zweiteAnfrage.at("/2/content/0/content").asString()).isEqualTo("{\"gueltig\":true}");

    // Die naechste Nachricht des Kunden: beide Antworten der ersten stehen unveraendert davor.
    JsonNode dritteAnfrage = rumpf(anfragen.get(2)).get("messages");
    assertThat(dritteAnfrage.size()).isEqualTo(5);
    assertThat(dritteAnfrage.get(0)).isEqualTo(zweiteAnfrage.get(0));
    assertThat(inhalt(dritteAnfrage.get(1))).isEqualTo(inhaltDerAntwort(RUNDE_1));
    assertThat(dritteAnfrage.get(2)).isEqualTo(zweiteAnfrage.get(2));
    assertThat(dritteAnfrage.get(3).get("role").asString()).isEqualTo("assistant");
    assertThat(inhalt(dritteAnfrage.get(3))).isEqualTo(inhaltDerAntwort(RUNDE_2));
    assertThat(dritteAnfrage.at("/4/content/0/text").asString()).isEqualTo("Danke");

    // Im Gespraech stehen danach beide Nachrichten mit allem bis zu ihrer Antwort: 4 + 2.
    assertThat(gespraech.verlauf()).hasSize(6);
  }

  private static JsonNode rumpf(AnthropicStub.Anfrage anfrage) {
    return Skriptmodell.JSON.readTree(anfrage.rumpf());
  }

  /** Der Inhalt einer Nachricht als Text: zeichengleich heisst auch in derselben Reihenfolge. */
  private static String inhalt(JsonNode nachricht) {
    return Skriptmodell.JSON.writeValueAsString(nachricht.get("content"));
  }

  private static String inhaltDerAntwort(String antwort) {
    return Skriptmodell.JSON.writeValueAsString(Skriptmodell.JSON.readTree(antwort).get("content"));
  }
}
