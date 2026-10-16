package de.kraftwerkone.overlord.monitor.assistent.modell;

import java.util.List;

/**
 * Die Antwort eines Aufrufs.
 *
 * @param text der Text des Modells, leer wenn es keinen geschrieben hat
 * @param aufrufe die Werkzeugaufrufe, leer wenn es keine gibt
 * @param ende warum das Modell aufgehoert hat
 * @param modell das Modell, das geantwortet hat, so wie der Anbieter es nennt
 * @param rohinhalt die Antwort in der Form des Anbieters; siehe {@link Nachricht.Modellnachricht}
 */
public record Modellantwort(
    String text,
    List<Werkzeugaufruf> aufrufe,
    Ende ende,
    long tokenEin,
    long tokenAus,
    String modell,
    String rohinhalt) {

  public Modellantwort {
    text = text == null ? "" : text;
    aufrufe = List.copyOf(aufrufe);
  }

  /** Warum das Modell aufgehoert hat. */
  public enum Ende {
    /** Die Antwort ist fertig. */
    FERTIG,
    /** Das Modell wartet auf die Ergebnisse seiner Werkzeugaufrufe. */
    WERKZEUG,
    /**
     * An der Obergrenze der Ausgabe abgeschnitten; eine Werkzeugeingabe ist dann unvollstaendig.
     */
    ABGESCHNITTEN,
    /** Das Modell hat die Antwort verweigert. */
    VERWEIGERT
  }

  /** Die Antwort als Eintrag im Verlauf. */
  public Nachricht.Modellnachricht alsNachricht() {
    return new Nachricht.Modellnachricht(text, aufrufe, rohinhalt);
  }
}
