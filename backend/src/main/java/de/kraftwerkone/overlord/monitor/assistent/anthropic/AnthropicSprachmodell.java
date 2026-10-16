package de.kraftwerkone.overlord.monitor.assistent.anthropic;

import de.kraftwerkone.overlord.monitor.assistent.modell.Modellanfrage;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellantwort;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler.Art;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Kundennachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Modellnachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Werkzeugergebnisse;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeug;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugaufruf;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugergebnis;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Der Adapter fuer die Messages-API von Anthropic — einfaches HTTP mit dem {@code RestClient} von
 * Spring und Jackson 3, <b>ohne SDK</b> (E‑1031, {@code docs/assistent.md} §4.3).
 *
 * <p><b>Der Rohinhalt jeder Antwort geht unveraendert zurueck.</b> Claude Sonnet 5.5 liefert
 * zwischen Werkzeugaufrufen Denkbloecke, die beim naechsten Aufruf unveraendert im Verlauf stehen
 * muessen; die Gespraechsfuehrung reicht sie als {@link Modellnachricht#rohinhalt()} durch, ohne
 * sie zu lesen.
 *
 * <p><b>Ein Wiederholungsversuch bei {@code 429} und {@code 529}</b> nach kurzer Pause, sonst
 * keiner. Was der Anbieter im Fehlerfall schreibt, kommt nicht ins Protokoll — nur Status und
 * Fehlertyp; der Client sieht beides nicht.
 *
 * <p>Der API-Schluessel steht in keiner Meldung und in keinem Protokoll (Regel G1).
 */
public class AnthropicSprachmodell implements Sprachmodell {

  private static final Logger log = LoggerFactory.getLogger(AnthropicSprachmodell.class);

  static final String PFAD = "/v1/messages";
  static final String VERSION = "2023-06-01";

  private static final int UEBERLASTET = 529;
  private static final int ZU_VIELE_ANFRAGEN = 429;

  private final RestClient http;
  private final String apiKey;
  private final String modell;
  private final String denken;
  private final Duration wiederholungPause;
  private final JsonMapper json;

  public AnthropicSprachmodell(
      String basisUrl,
      String apiKey,
      String modell,
      Duration zeitgrenze,
      AnthropicEigenschaften eigenschaften,
      JsonMapper json) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(zeitgrenze).build();
    JdkClientHttpRequestFactory fabrik = new JdkClientHttpRequestFactory(client);
    fabrik.setReadTimeout(zeitgrenze);
    this.http = RestClient.builder().baseUrl(basisUrl).requestFactory(fabrik).build();
    this.apiKey = apiKey;
    this.modell = modell;
    this.denken = eigenschaften.denken();
    this.wiederholungPause = eigenschaften.wiederholungPause();
    this.json = json;
  }

  @Override
  public Modellantwort rufe(Modellanfrage anfrage) {
    String rumpf = json.writeValueAsString(rumpf(anfrage));
    Rohantwort antwort = sende(rumpf);
    if (antwort.status() == ZU_VIELE_ANFRAGEN || antwort.status() == UEBERLASTET) {
      log.info(
          "Anthropic antwortet {} ({}), ein Wiederholungsversuch in {} ms",
          antwort.status(),
          fehlertyp(antwort.rumpf()),
          wiederholungPause.toMillis());
      warte();
      antwort = sende(rumpf);
      if (antwort.status() == ZU_VIELE_ANFRAGEN || antwort.status() == UEBERLASTET) {
        throw fehler(Art.UEBERLASTET, antwort);
      }
    }
    if (antwort.status() >= 500) {
      throw fehler(Art.STOERUNG, antwort);
    }
    if (antwort.status() >= 400) {
      throw fehler(Art.ABGELEHNT, antwort);
    }
    if (antwort.status() < 200 || antwort.status() >= 300) {
      throw fehler(Art.UNLESBAR, antwort);
    }
    return lies(antwort.rumpf());
  }

  // ─── Anfrage ───────────────────────────────────────────────────────────────────────────────

  ObjectNode rumpf(Modellanfrage anfrage) {
    ObjectNode rumpf = json.createObjectNode();
    rumpf.put("model", modell);
    rumpf.put("max_tokens", anfrage.maxTokens());
    rumpf.put("system", anfrage.anweisung());
    if (!"keins".equals(denken)) {
      rumpf.putObject("thinking").put("type", denken);
    }
    ArrayNode nachrichten = rumpf.putArray("messages");
    for (Nachricht nachricht : anfrage.verlauf()) {
      nachrichten.add(nachricht(nachricht));
    }
    if (!anfrage.werkzeuge().isEmpty()) {
      ArrayNode werkzeuge = rumpf.putArray("tools");
      for (Werkzeug werkzeug : anfrage.werkzeuge()) {
        ObjectNode eintrag = werkzeuge.addObject();
        eintrag.put("name", werkzeug.name());
        eintrag.put("description", werkzeug.beschreibung());
        eintrag.set("input_schema", werkzeug.eingabeschema());
      }
    }
    return rumpf;
  }

  private ObjectNode nachricht(Nachricht nachricht) {
    ObjectNode eintrag = json.createObjectNode();
    switch (nachricht) {
      case Kundennachricht kunde -> {
        eintrag.put("role", "user");
        eintrag.putArray("content").addObject().put("type", "text").put("text", kunde.text());
      }
      case Modellnachricht antwort -> {
        eintrag.put("role", "assistant");
        if (antwort.rohinhalt() != null) {
          eintrag.set("content", json.readTree(antwort.rohinhalt()));
        } else {
          ArrayNode inhalt = eintrag.putArray("content");
          if (!antwort.text().isEmpty()) {
            inhalt.addObject().put("type", "text").put("text", antwort.text());
          }
          for (Werkzeugaufruf aufruf : antwort.aufrufe()) {
            ObjectNode block = inhalt.addObject();
            block.put("type", "tool_use");
            block.put("id", aufruf.id());
            block.put("name", aufruf.name());
            block.set("input", aufruf.eingabe());
          }
        }
      }
      case Werkzeugergebnisse ergebnisse -> {
        eintrag.put("role", "user");
        ArrayNode inhalt = eintrag.putArray("content");
        for (Werkzeugergebnis ergebnis : ergebnisse.ergebnisse()) {
          ObjectNode block = inhalt.addObject();
          block.put("type", "tool_result");
          block.put("tool_use_id", ergebnis.aufrufId());
          block.put("content", ergebnis.inhalt());
          if (ergebnis.fehler()) {
            block.put("is_error", true);
          }
        }
      }
    }
    return eintrag;
  }

  // ─── Antwort ───────────────────────────────────────────────────────────────────────────────

  private Modellantwort lies(String rumpf) {
    JsonNode antwort;
    try {
      antwort = json.readTree(rumpf);
    } catch (JacksonException unlesbar) {
      throw new Modellfehler(Art.UNLESBAR, "Antwort von Anthropic ist kein JSON");
    }
    JsonNode inhalt = antwort.get("content");
    if (inhalt == null || !inhalt.isArray()) {
      throw new Modellfehler(Art.UNLESBAR, "Antwort von Anthropic ohne content");
    }
    List<String> texte = new ArrayList<>();
    List<Werkzeugaufruf> aufrufe = new ArrayList<>();
    for (JsonNode block : inhalt) {
      String typ = text(block.get("type"));
      if ("text".equals(typ)) {
        String text = text(block.get("text"));
        if (text != null && !text.isEmpty()) {
          texte.add(text);
        }
      } else if ("tool_use".equals(typ)) {
        aufrufe.add(
            new Werkzeugaufruf(text(block.get("id")), text(block.get("name")), block.get("input")));
      }
    }
    JsonNode usage = antwort.path("usage");
    return new Modellantwort(
        String.join("\n\n", texte),
        aufrufe,
        ende(text(antwort.get("stop_reason")), aufrufe),
        usage.path("input_tokens").asLong(0),
        usage.path("output_tokens").asLong(0),
        text(antwort.get("model")),
        json.writeValueAsString(inhalt));
  }

  private static Modellantwort.Ende ende(String grund, List<Werkzeugaufruf> aufrufe) {
    if (grund == null) {
      return aufrufe.isEmpty() ? Modellantwort.Ende.FERTIG : Modellantwort.Ende.WERKZEUG;
    }
    return switch (grund) {
      case "tool_use" -> Modellantwort.Ende.WERKZEUG;
      case "max_tokens", "model_context_window_exceeded" -> Modellantwort.Ende.ABGESCHNITTEN;
      case "refusal" -> Modellantwort.Ende.VERWEIGERT;
      default -> Modellantwort.Ende.FERTIG;
    };
  }

  // ─── Transport ─────────────────────────────────────────────────────────────────────────────

  private record Rohantwort(int status, String rumpf) {}

  private Rohantwort sende(String rumpf) {
    try {
      return http.post()
          .uri(PFAD)
          .header("x-api-key", apiKey)
          .header("anthropic-version", VERSION)
          .contentType(MediaType.APPLICATION_JSON)
          .body(rumpf)
          .exchangeForRequiredValue(
              (anfrage, antwort) ->
                  new Rohantwort(
                      antwort.getStatusCode().value(),
                      new String(antwort.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    } catch (ResourceAccessException fehler) {
      if (istZeitgrenze(fehler)) {
        throw new Modellfehler(Art.ZEITGRENZE, "Anthropic: Zeitgrenze ueberschritten", fehler);
      }
      throw new Modellfehler(Art.NICHT_ERREICHBAR, "Anthropic nicht erreichbar", fehler);
    }
  }

  private static boolean istZeitgrenze(Throwable fehler) {
    for (Throwable ursache = fehler; ursache != null; ursache = ursache.getCause()) {
      if (ursache instanceof HttpTimeoutException || ursache instanceof SocketTimeoutException) {
        return true;
      }
      if (ursache instanceof IOException && ursache.getMessage() != null) {
        if (ursache.getMessage().toLowerCase(Locale.ROOT).contains("timed out")) {
          return true;
        }
      }
    }
    return false;
  }

  private void warte() {
    try {
      Thread.sleep(wiederholungPause);
    } catch (InterruptedException unterbrochen) {
      Thread.currentThread().interrupt();
      throw new Modellfehler(Art.UEBERLASTET, "Wiederholungsversuch unterbrochen");
    }
  }

  private Modellfehler fehler(Art art, Rohantwort antwort) {
    return new Modellfehler(
        art, "Anthropic antwortet " + antwort.status() + " (" + fehlertyp(antwort.rumpf()) + ")");
  }

  /** Nur der Fehlertyp des Anbieters, nie seine Meldung: Sie koennte Teile der Anfrage zitieren. */
  private String fehlertyp(String rumpf) {
    try {
      String typ = text(json.readTree(rumpf).path("error").get("type"));
      return typ == null ? "ohne Fehlertyp" : typ;
    } catch (JacksonException unlesbar) {
      return "ohne Fehlertyp";
    }
  }

  private static String text(JsonNode knoten) {
    return knoten != null && knoten.isString() ? knoten.stringValue() : null;
  }
}
