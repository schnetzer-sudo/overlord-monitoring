package de.kraftwerkone.overlord.monitor.assistent.anthropic;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ein lokaler HTTP-Stub an Stelle von {@code api.anthropic.com}: antwortet der Reihe nach, was der
 * Test vorgibt, und merkt sich jede Anfrage. Nur auf der Loopback-Adresse.
 */
public final class AnthropicStub implements AutoCloseable {

  /** Eine aufgezeichnete Anfrage. */
  public record Anfrage(String methode, String pfad, Map<String, List<String>> kopf, String rumpf) {

    public String kopfwert(String name) {
      return kopf.entrySet().stream()
          .filter(eintrag -> eintrag.getKey().equalsIgnoreCase(name))
          .map(eintrag -> eintrag.getValue().getFirst())
          .findFirst()
          .orElse(null);
    }
  }

  private record Antwort(int status, String rumpf, Duration verzoegerung) {}

  private final HttpServer server;
  private final ExecutorService ausfuehrung = Executors.newCachedThreadPool();
  private final Deque<Antwort> antworten = new ArrayDeque<>();
  private final List<Anfrage> anfragen = new ArrayList<>();

  public AnthropicStub() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", this::bediene);
    server.setExecutor(ausfuehrung);
    server.start();
  }

  public String basisUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public synchronized AnthropicStub antworte(int status, String rumpf) {
    antworten.add(new Antwort(status, rumpf, Duration.ZERO));
    return this;
  }

  public synchronized AnthropicStub antworteNach(Duration verzoegerung, int status, String rumpf) {
    antworten.add(new Antwort(status, rumpf, verzoegerung));
    return this;
  }

  public synchronized List<Anfrage> anfragen() {
    return List.copyOf(anfragen);
  }

  private void bediene(HttpExchange austausch) throws IOException {
    String rumpf = new String(austausch.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    Antwort antwort;
    synchronized (this) {
      anfragen.add(
          new Anfrage(
              austausch.getRequestMethod(),
              austausch.getRequestURI().getPath(),
              Map.copyOf(austausch.getRequestHeaders()),
              rumpf));
      antwort = antworten.pollFirst();
    }
    if (antwort == null) {
      antwort =
          new Antwort(
              500, "{\"type\":\"error\",\"error\":{\"type\":\"skript_leer\"}}", Duration.ZERO);
    }
    if (!antwort.verzoegerung().isZero()) {
      try {
        Thread.sleep(antwort.verzoegerung());
      } catch (InterruptedException unterbrochen) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    byte[] bytes = antwort.rumpf().getBytes(StandardCharsets.UTF_8);
    austausch.getResponseHeaders().add("content-type", "application/json");
    try {
      austausch.sendResponseHeaders(antwort.status(), bytes.length);
      try (OutputStream aus = austausch.getResponseBody()) {
        aus.write(bytes);
      }
    } catch (IOException abgebrochen) {
      // Der Client hat aufgegeben (Zeitgrenze) — kein Fehler des Stubs.
    }
  }

  @Override
  public void close() {
    server.stop(0);
    ausfuehrung.shutdownNow();
  }
}
