package de.kraftwerkone.overlord.monitor.assistent.modell;

import java.util.List;

/** Ein Eintrag im Verlauf eines Gespraechs. */
public sealed interface Nachricht {

  /** Was der Kunde geschrieben hat, unveraendert. */
  record Kundennachricht(String text) implements Nachricht {}

  /**
   * Eine Antwort des Modells.
   *
   * @param text der Text, leer wenn das Modell nur Werkzeuge gerufen hat
   * @param rohinhalt die Antwort in der Form des Anbieters, oder {@code null}. Die
   *     Gespraechsfuehrung liest sie nie; der Adapter, der sie geliefert hat, schickt sie
   *     unveraendert zurueck (Anthropic verlangt das fuer Denkbloecke). Fehlt sie, baut der Adapter
   *     die Nachricht aus Text und Werkzeugaufrufen.
   */
  record Modellnachricht(String text, List<Werkzeugaufruf> aufrufe, String rohinhalt)
      implements Nachricht {

    public Modellnachricht {
      text = text == null ? "" : text;
      aufrufe = List.copyOf(aufrufe);
    }

    /** Eine feste Antwort des Backends an Stelle des Modells, ohne Werkzeugaufruf. */
    public static Modellnachricht nurText(String text) {
      return new Modellnachricht(text, List.of(), null);
    }
  }

  /** Die Antworten des Backends auf alle Werkzeugaufrufe einer Runde, in einer Nachricht. */
  record Werkzeugergebnisse(List<Werkzeugergebnis> ergebnisse) implements Nachricht {

    public Werkzeugergebnisse {
      ergebnisse = List.copyOf(ergebnisse);
    }
  }
}
