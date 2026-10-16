package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.assistent.modell.Modellanfrage;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellantwort;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugaufruf;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ein geskriptetes Ersatzmodell: liefert der Reihe nach, was der Test vorgibt, und merkt sich jede
 * Anfrage. Es geht kein Aufruf hinaus.
 */
public final class Skriptmodell implements Sprachmodell {

  public static final JsonMapper JSON = JsonMapper.builder().build();

  private final Deque<Function<Modellanfrage, Modellantwort>> schritte = new ArrayDeque<>();
  private final List<Modellanfrage> anfragen = new ArrayList<>();
  private int werkzeugnummer;

  @Override
  public synchronized Modellantwort rufe(Modellanfrage anfrage) {
    anfragen.add(anfrage);
    Function<Modellanfrage, Modellantwort> schritt = schritte.pollFirst();
    if (schritt == null) {
      throw new IllegalStateException("Skript erschoepft nach " + anfragen.size() + " Aufrufen");
    }
    return schritt.apply(anfrage);
  }

  /** Antwortet mit Text und endet. */
  public synchronized Skriptmodell text(String text) {
    schritte.add(
        anfrage ->
            new Modellantwort(text, List.of(), Modellantwort.Ende.FERTIG, 10, 5, "skript", null));
    return this;
  }

  /** Ruft ein Werkzeug mit dieser Eingabe (JSON). */
  public synchronized Skriptmodell werkzeug(String name, String eingabe) {
    String id = "aufruf-" + ++werkzeugnummer;
    JsonNode knoten = JSON.readTree(eingabe);
    schritte.add(
        anfrage ->
            new Modellantwort(
                "",
                List.of(new Werkzeugaufruf(id, name, knoten)),
                Modellantwort.Ende.WERKZEUG,
                10,
                5,
                "skript",
                null));
    return this;
  }

  /** Ruft zwei Werkzeuge in einer Antwort. */
  public synchronized Skriptmodell zweiWerkzeuge(String name, String erste, String zweite) {
    String id1 = "aufruf-" + ++werkzeugnummer;
    String id2 = "aufruf-" + ++werkzeugnummer;
    schritte.add(
        anfrage ->
            new Modellantwort(
                "",
                List.of(
                    new Werkzeugaufruf(id1, name, JSON.readTree(erste)),
                    new Werkzeugaufruf(id2, name, JSON.readTree(zweite))),
                Modellantwort.Ende.WERKZEUG,
                10,
                5,
                "skript",
                null));
    return this;
  }

  /** Eine beliebige Antwort. */
  public synchronized Skriptmodell antwort(Modellantwort antwort) {
    schritte.add(anfrage -> antwort);
    return this;
  }

  /** Scheitert mit dieser Fehlerart. */
  public synchronized Skriptmodell fehler(Modellfehler.Art art) {
    schritte.add(
        anfrage -> {
          throw new Modellfehler(art, "geskripteter Fehler " + art);
        });
    return this;
  }

  public synchronized List<Modellanfrage> anfragen() {
    return List.copyOf(anfragen);
  }

  public synchronized Modellanfrage letzteAnfrage() {
    return anfragen.getLast();
  }

  public synchronized void zuruecksetzen() {
    schritte.clear();
    anfragen.clear();
  }
}
