package de.kraftwerkone.overlord.monitor.assistent.modell;

import java.util.List;

/**
 * Ein Aufruf des Modells: die feste Anweisung, der bisherige Verlauf samt der neuen Nachricht, die
 * Werkzeuge und die Obergrenze der Ausgabe. Die Zeitgrenze gehoert zum Adapter, nicht zur Anfrage.
 *
 * <p><b>Kein Benutzername, keine Mandanten-ID</b> (E‑1035): Es gibt dafuer kein Feld.
 */
public record Modellanfrage(
    String anweisung, List<Nachricht> verlauf, List<Werkzeug> werkzeuge, int maxTokens) {

  public Modellanfrage {
    verlauf = List.copyOf(verlauf);
    werkzeuge = List.copyOf(werkzeuge);
  }
}
