package de.kraftwerkone.overlord.monitor.message;

import java.util.List;

/**
 * Was ein Suchbegriff in den Stammdaten getroffen hat, aufgeloest zu Kennungen.
 *
 * @param prozessIds Treffer in {@code Process.ProcessName} und {@code Project.ProjectName}
 * @param sosIds Treffer in {@code SOS.SOSName}
 * @param zuUnscharf ob die Grenze ueberschritten wurde. Dann wird <b>nicht</b> abgeschnitten,
 *     sondern abgewiesen — eine gekuerzte Trefferliste sieht aus wie ein vollstaendiges Ergebnis.
 */
public record Suchtreffer(List<String> prozessIds, List<String> sosIds, boolean zuUnscharf) {

  public Suchtreffer {
    prozessIds = List.copyOf(prozessIds);
    sosIds = List.copyOf(sosIds);
  }

  /** Kein einziger Treffer — dann ist die Liste leer, ohne dass {@code Message} angefasst wird. */
  public boolean leer() {
    return prozessIds.isEmpty() && sosIds.isEmpty();
  }
}
