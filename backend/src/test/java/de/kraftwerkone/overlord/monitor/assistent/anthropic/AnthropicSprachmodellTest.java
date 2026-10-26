package de.kraftwerkone.overlord.monitor.assistent.anthropic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.assistent.modell.Modellanfrage;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellantwort;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Kundennachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Modellnachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Werkzeugergebnisse;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeug;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugaufruf;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugergebnis;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Der Adapter fuer Anthropic gegen einen lokalen HTTP-Stub ({@code docs/assistent.md} §4.3, §9.1):
 * Kopfzeilen, Aufbau der Anfrage, Auswertung der Antwort, Wiederholung, Fehlerarten. Kein Aufruf
 * verlaesst den Rechner.
 */
class AnthropicSprachmodellTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final String SCHLUESSEL = "test-schluessel-ohne-wert";

  private static final String ANTWORT_TEXT =
      """
      {"id":"msg_1","type":"message","role":"assistant","model":"claude-sonnet-5-5",
       "content":[{"type":"text","text":"Wie lautet das Werk?"}],
       "stop_reason":"end_turn","usage":{"input_tokens":120,"output_tokens":9}}
      """;

  private static final String ANTWORT_WERKZEUG =
      """
      {"id":"msg_2","type":"message","role":"assistant","model":"claude-sonnet-5-5",
       "content":[
         {"type":"thinking","thinking":"Notiz","signature":"sig-1"},
         {"type":"text","text":"Ich reiche den Vorschlag ein."},
         {"type":"tool_use","id":"toolu_1","name":"WERK_ABLADESTELLE",
          "input":{"aktion":"Neuanlage","eintraege":[{"werk":"72332"}]}}],
       "stop_reason":"tool_use","usage":{"input_tokens":200,"output_tokens":40}}
      """;

  private AnthropicStub stub;

  @BeforeEach
  void starte() throws IOException {
    stub = new AnthropicStub();
  }

  @AfterEach
  void stoppe() {
    stub.close();
  }

  private AnthropicSprachmodell adapter(String denken, Duration zeitgrenze) {
    return new AnthropicSprachmodell(
        stub.basisUrl(),
        SCHLUESSEL,
        "claude-sonnet-5-5",
        zeitgrenze,
        new AnthropicEigenschaften(denken, Duration.ofMillis(10)),
        JSON);
  }

  private AnthropicSprachmodell adapter() {
    return adapter("between_tools", Duration.ofSeconds(5));
  }

  private static Modellanfrage anfrage(List<Nachricht> verlauf) {
    Werkzeug werkzeug =
        new Werkzeug(
            "WERK_ABLADESTELLE",
            "Reicht einen Vorschlag ein.",
            JSON.readTree("{\"type\":\"object\",\"properties\":{}}"));
    return new Modellanfrage("Die Anweisung.", verlauf, List.of(werkzeug), 1024);
  }

  private static Modellanfrage einfach() {
    return anfrage(List.of(new Kundennachricht("Hallo")));
  }

  @Test
  @DisplayName("Kopfzeilen: x-api-key, anthropic-version 2023-06-01, JSON, POST /v1/messages")
  void kopfzeilen() {
    stub.antworte(200, ANTWORT_TEXT);

    adapter().rufe(einfach());

    AnthropicStub.Anfrage anfrage = stub.anfragen().getFirst();
    assertThat(anfrage.methode()).isEqualTo("POST");
    assertThat(anfrage.pfad()).isEqualTo("/v1/messages");
    assertThat(anfrage.kopfwert("x-api-key")).isEqualTo(SCHLUESSEL);
    assertThat(anfrage.kopfwert("anthropic-version")).isEqualTo("2023-06-01");
    assertThat(anfrage.kopfwert("content-type")).startsWith("application/json");
  }

  @Test
  @DisplayName("Aufbau: Modell, max_tokens, system, thinking, tools und der ganze Verlauf")
  void aufbau_der_anfrage() {
    stub.antworte(200, ANTWORT_TEXT);
    String rohinhalt =
        "[{\"type\":\"thinking\",\"thinking\":\"Notiz\",\"signature\":\"sig-1\"},"
            + "{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"WERK_ABLADESTELLE\",\"input\":{}}]";
    List<Nachricht> verlauf =
        List.of(
            new Kundennachricht("Bitte anlegen"),
            new Modellnachricht(
                "",
                List.of(new Werkzeugaufruf("toolu_1", "WERK_ABLADESTELLE", JSON.readTree("{}"))),
                rohinhalt),
            new Werkzeugergebnisse(
                List.of(new Werkzeugergebnis("toolu_1", "{\"gueltig\":false}", true))),
            Modellnachricht.nurText("Wie lautet das Werk?"),
            new Kundennachricht("72332"),
            new Modellnachricht(
                "Ich reiche ein.",
                List.of(
                    new Werkzeugaufruf("toolu_2", "WERK_ABLADESTELLE", JSON.readTree("{\"a\":1}"))),
                null),
            new Werkzeugergebnisse(
                List.of(new Werkzeugergebnis("toolu_2", "{\"gueltig\":true}", false))));

    adapter().rufe(anfrage(verlauf));

    JsonNode rumpf = JSON.readTree(stub.anfragen().getFirst().rumpf());
    assertThat(rumpf.get("model").asString()).isEqualTo("claude-sonnet-5-5");
    assertThat(rumpf.get("max_tokens").asInt()).isEqualTo(1024);
    assertThat(rumpf.get("system").asString()).isEqualTo("Die Anweisung.");
    assertThat(rumpf.get("thinking")).isEqualTo(JSON.readTree("{\"type\":\"between_tools\"}"));
    assertThat(rumpf.at("/tools/0/name").asString()).isEqualTo("WERK_ABLADESTELLE");
    assertThat(rumpf.at("/tools/0/description").asString())
        .isEqualTo("Reicht einen Vorschlag ein.");
    assertThat(rumpf.at("/tools/0/input_schema/type").asString()).isEqualTo("object");

    JsonNode nachrichten = rumpf.get("messages");
    assertThat(nachrichten.size()).isEqualTo(7);
    assertThat(nachrichten.get(0))
        .isEqualTo(
            JSON.readTree(
                "{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"Bitte anlegen\"}]}"));
    // Der Rohinhalt geht unveraendert zurueck, Denkblock samt Signatur.
    assertThat(nachrichten.get(1).get("role").asString()).isEqualTo("assistant");
    assertThat(nachrichten.get(1).get("content")).isEqualTo(JSON.readTree(rohinhalt));
    assertThat(nachrichten.get(2))
        .isEqualTo(
            JSON.readTree(
                "{\"role\":\"user\",\"content\":[{\"type\":\"tool_result\","
                    + "\"tool_use_id\":\"toolu_1\",\"content\":\"{\\\"gueltig\\\":false}\","
                    + "\"is_error\":true}]}"));
    assertThat(nachrichten.get(3))
        .isEqualTo(
            JSON.readTree(
                "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\","
                    + "\"text\":\"Wie lautet das Werk?\"}]}"));
    assertThat(nachrichten.get(5))
        .isEqualTo(
            JSON.readTree(
                "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Ich reiche ein.\"},"
                    + "{\"type\":\"tool_use\",\"id\":\"toolu_2\",\"name\":\"WERK_ABLADESTELLE\","
                    + "\"input\":{\"a\":1}}]}"));
    assertThat(nachrichten.get(6).at("/content/0/is_error").isMissingNode()).isTrue();
  }

  @Test
  @DisplayName("Einstellung denken: adaptive setzt den Typ, keins laesst das Feld weg")
  void denken() {
    stub.antworte(200, ANTWORT_TEXT).antworte(200, ANTWORT_TEXT);

    adapter("adaptive", Duration.ofSeconds(5)).rufe(einfach());
    adapter("keins", Duration.ofSeconds(5)).rufe(einfach());

    assertThat(JSON.readTree(stub.anfragen().get(0).rumpf()).at("/thinking/type").asString())
        .isEqualTo("adaptive");
    assertThat(JSON.readTree(stub.anfragen().get(1).rumpf()).has("thinking")).isFalse();
  }

  @Test
  @DisplayName("Antwort mit Text: FERTIG, Token und Modell")
  void antwort_text() {
    stub.antworte(200, ANTWORT_TEXT);

    Modellantwort antwort = adapter().rufe(einfach());

    assertThat(antwort.text()).isEqualTo("Wie lautet das Werk?");
    assertThat(antwort.aufrufe()).isEmpty();
    assertThat(antwort.ende()).isEqualTo(Modellantwort.Ende.FERTIG);
    assertThat(antwort.tokenEin()).isEqualTo(120);
    assertThat(antwort.tokenAus()).isEqualTo(9);
    assertThat(antwort.modell()).isEqualTo("claude-sonnet-5-5");
  }

  /**
   * Mit {@code adaptive} denkt das Modell auch vor einer reinen Textantwort (E‑1153). Was es dabei
   * denkt, ist nicht fuer den Kunden: Der Text der Antwort entsteht nur aus {@code text}-Bloecken,
   * der Rohinhalt traegt alle Bloecke in ihrer Reihenfolge, mit Signatur und Daten.
   */
  @Test
  @DisplayName(
      "Denkbloecke (thinking, redacted_thinking) stehen nie im Text, aber vollstaendig im Rohinhalt")
  void denkbloecke_nie_im_text() {
    String antwortMitDenken =
        """
        {"id":"msg_3","type":"message","role":"assistant","model":"claude-sonnet-5-5",
         "content":[
           {"type":"thinking","thinking":"Der Kunde nennt drei Zeilen. Ich muss genau lesen.",
            "signature":"sig-3"},
           {"type":"redacted_thinking","data":"verschluesselt-3"},
           {"type":"text","text":"Welche Abladestelle gilt für 40NP?"},
           {"type":"thinking","thinking":"","signature":"sig-4"}],
         "stop_reason":"end_turn","usage":{"input_tokens":300,"output_tokens":80}}
        """;
    stub.antworte(200, antwortMitDenken);

    Modellantwort antwort = adapter("adaptive", Duration.ofSeconds(5)).rufe(einfach());

    assertThat(antwort.text()).isEqualTo("Welche Abladestelle gilt für 40NP?");
    assertThat(antwort.text()).doesNotContain("Kunde", "genau lesen", "verschluesselt", "sig-");
    assertThat(antwort.aufrufe()).isEmpty();
    assertThat(antwort.ende()).isEqualTo(Modellantwort.Ende.FERTIG);
    assertThat(antwort.rohinhalt())
        .isEqualTo(JSON.writeValueAsString(JSON.readTree(antwortMitDenken).get("content")));
  }

  @Test
  @DisplayName("Nur Denkbloecke und ein Werkzeugaufruf: Der Text bleibt leer")
  void nur_denken_und_werkzeug_ohne_text() {
    stub.antworte(
        200,
        """
        {"model":"claude-sonnet-5-5","content":[
           {"type":"redacted_thinking","data":"verschluesselt-5"},
           {"type":"thinking","thinking":"Laut Regel reiche ich ein.","signature":"sig-5"},
           {"type":"tool_use","id":"toolu_5","name":"WERK_ABLADESTELLE","input":{}}],
         "stop_reason":"tool_use","usage":{"input_tokens":10,"output_tokens":20}}
        """);

    Modellantwort antwort = adapter("adaptive", Duration.ofSeconds(5)).rufe(einfach());

    assertThat(antwort.text()).isEmpty();
    assertThat(antwort.aufrufe()).extracting(Werkzeugaufruf::id).containsExactly("toolu_5");
    assertThat(JSON.readTree(antwort.rohinhalt()).size()).isEqualTo(3);
  }

  @Test
  @DisplayName(
      "Antwort mit tool_use: WERKZEUG, Aufruf mit Kennung und Eingabe, Rohinhalt vollstaendig")
  void antwort_werkzeug() {
    stub.antworte(200, ANTWORT_WERKZEUG);

    Modellantwort antwort = adapter().rufe(einfach());

    assertThat(antwort.ende()).isEqualTo(Modellantwort.Ende.WERKZEUG);
    assertThat(antwort.text()).isEqualTo("Ich reiche den Vorschlag ein.");
    assertThat(antwort.aufrufe()).hasSize(1);
    Werkzeugaufruf aufruf = antwort.aufrufe().getFirst();
    assertThat(aufruf.id()).isEqualTo("toolu_1");
    assertThat(aufruf.name()).isEqualTo("WERK_ABLADESTELLE");
    assertThat(aufruf.eingabe().at("/eintraege/0/werk").asString()).isEqualTo("72332");
    assertThat(JSON.readTree(antwort.rohinhalt()))
        .isEqualTo(JSON.readTree(ANTWORT_WERKZEUG).get("content"));
  }

  @Test
  @DisplayName("Abbruchgruende: max_tokens wird ABGESCHNITTEN, refusal wird VERWEIGERT")
  void abbruchgruende() {
    stub.antworte(
            200, ANTWORT_WERKZEUG.replace("\"tool_use\",\"usage\"", "\"max_tokens\",\"usage\""))
        .antworte(
            200,
            """
            {"model":"claude-sonnet-5-5","content":[],"stop_reason":"refusal",
             "stop_details":{"type":"refusal","category":"general_harms"},
             "usage":{"input_tokens":5,"output_tokens":0}}
            """);

    assertThat(adapter().rufe(einfach()).ende()).isEqualTo(Modellantwort.Ende.ABGESCHNITTEN);
    assertThat(adapter().rufe(einfach()).ende()).isEqualTo(Modellantwort.Ende.VERWEIGERT);
  }

  @Test
  @DisplayName("429: ein Wiederholungsversuch, dann die Antwort")
  void wiederholung_bei_429() {
    stub.antworte(429, "{\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\"}}")
        .antworte(200, ANTWORT_TEXT);

    Modellantwort antwort = adapter().rufe(einfach());

    assertThat(antwort.text()).isEqualTo("Wie lautet das Werk?");
    assertThat(stub.anfragen()).hasSize(2);
    assertThat(stub.anfragen().get(1).rumpf()).isEqualTo(stub.anfragen().get(0).rumpf());
  }

  @Test
  @DisplayName("529 zweimal: UEBERLASTET nach genau einem Wiederholungsversuch")
  void ueberlastet_nach_einer_wiederholung() {
    String ueberlastet = "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\"}}";
    stub.antworte(529, ueberlastet).antworte(529, ueberlastet).antworte(200, ANTWORT_TEXT);

    assertThatThrownBy(() -> adapter().rufe(einfach()))
        .isInstanceOfSatisfying(
            Modellfehler.class,
            fehler -> {
              assertThat(fehler.art()).isEqualTo(Modellfehler.Art.UEBERLASTET);
              assertThat(fehler.getMessage()).contains("529", "overloaded_error");
            });
    assertThat(stub.anfragen()).hasSize(2);
  }

  @Test
  @DisplayName(
      "500 und 401: keine Wiederholung, STOERUNG bzw. ABGELEHNT, ohne Meldung des Anbieters")
  void keine_wiederholung_sonst() {
    stub.antworte(
            500, "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"Geheim\"}}")
        .antworte(401, "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\"}}");

    assertThatThrownBy(() -> adapter().rufe(einfach()))
        .isInstanceOfSatisfying(
            Modellfehler.class,
            fehler -> {
              assertThat(fehler.art()).isEqualTo(Modellfehler.Art.STOERUNG);
              assertThat(fehler.getMessage()).doesNotContain("Geheim");
            });
    assertThat(stub.anfragen()).hasSize(1);
    assertThatThrownBy(() -> adapter().rufe(einfach()))
        .isInstanceOfSatisfying(
            Modellfehler.class,
            fehler -> {
              assertThat(fehler.art()).isEqualTo(Modellfehler.Art.ABGELEHNT);
              assertThat(fehler.getMessage()).doesNotContain(SCHLUESSEL);
            });
    assertThat(stub.anfragen()).hasSize(2);
  }

  @Test
  @DisplayName("Unlesbare Antwort: UNLESBAR")
  void unlesbar() {
    stub.antworte(200, "kein json").antworte(200, "{\"model\":\"x\"}");

    assertThatThrownBy(() -> adapter().rufe(einfach()))
        .extracting(fehler -> ((Modellfehler) fehler).art())
        .isEqualTo(Modellfehler.Art.UNLESBAR);
    assertThatThrownBy(() -> adapter().rufe(einfach()))
        .extracting(fehler -> ((Modellfehler) fehler).art())
        .isEqualTo(Modellfehler.Art.UNLESBAR);
  }

  /**
   * Die Zeitgrenze. Zugesichert wird die <b>Fehlerart</b>, nicht eine Dauer (Regel T1): Der Stub
   * antwortet erst nach einer Zeit, die das Zwanzigfache der Zeitgrenze betraegt.
   */
  @Test
  @DisplayName("Zeitgrenze: Antwortet der Anbieter nicht rechtzeitig, ZEITGRENZE")
  void zeitgrenze() {
    stub.antworteNach(Duration.ofSeconds(4), 200, ANTWORT_TEXT);

    assertThatThrownBy(() -> adapter("between_tools", Duration.ofMillis(200)).rufe(einfach()))
        .extracting(fehler -> ((Modellfehler) fehler).art())
        .isEqualTo(Modellfehler.Art.ZEITGRENZE);
  }

  @Test
  @DisplayName("Nicht erreichbar: NICHT_ERREICHBAR")
  void nicht_erreichbar() throws IOException {
    int freierPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      freierPort = socket.getLocalPort();
    }
    AnthropicSprachmodell adapter =
        new AnthropicSprachmodell(
            "http://127.0.0.1:" + freierPort,
            SCHLUESSEL,
            "claude-sonnet-5-5",
            Duration.ofSeconds(2),
            new AnthropicEigenschaften(null, null),
            JSON);

    assertThatThrownBy(() -> adapter.rufe(einfach()))
        .extracting(fehler -> ((Modellfehler) fehler).art())
        .isEqualTo(Modellfehler.Art.NICHT_ERREICHBAR);
  }
}
