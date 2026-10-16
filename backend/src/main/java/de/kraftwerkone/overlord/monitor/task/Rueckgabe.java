package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.task.TypDefinition.Rueckgaben;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Die Rueckgabe der Plattform, gelesen gegen die Kopie der Definition im Auftrag ({@code
 * docs/tasks.md} §5.4).
 *
 * <p>An LF trennen, ein abschliessendes CR je Zeile entfernen, Leerraum am Rand entfernen. Endet
 * die Rueckgabe mit einem Zeilenende, entsteht dadurch keine leere Zeile mehr. <b>Der Rohwert wird
 * immer mitgeliefert und nie umgedeutet</b>: Ein unbekannter Wert heisst {@link
 * Zeilenergebnis#UNBEKANNT}, steht aber unveraendert daneben.
 *
 * @param zeilen eine je Eintrag, in derselben Reihenfolge
 */
public record Rueckgabe(List<Zeile> zeilen) {

  public Rueckgabe {
    zeilen = List.copyOf(zeilen);
  }

  /**
   * @param wert die Zeile ohne CR und ohne Leerraum am Rand
   */
  public record Zeile(String wert, Zeilenergebnis ergebnis) {}

  /**
   * @param roh die Spalte {@code task.rueckgabe}
   * @param rueckgaben die Rueckgabewerte der Kopie; {@code null} macht jede Zeile {@link
   *     Zeilenergebnis#UNBEKANNT}
   * @return leer, wenn die Rueckgabe fehlt oder nur aus Leerraum besteht
   */
  public static Optional<Rueckgabe> lies(String roh, Rueckgaben rueckgaben) {
    if (roh == null || roh.isBlank()) {
      return Optional.empty();
    }
    String text = roh.endsWith("\n") ? roh.substring(0, roh.length() - 1) : roh;
    List<Zeile> zeilen = new ArrayList<>();
    for (String zeile : text.split("\n", -1)) {
      String ohneCr = zeile.endsWith("\r") ? zeile.substring(0, zeile.length() - 1) : zeile;
      String wert = ohneCr.strip();
      zeilen.add(new Zeile(wert, ergebnis(wert, rueckgaben)));
    }
    return Optional.of(new Rueckgabe(zeilen));
  }

  public boolean alleOk() {
    return zeilen.stream().allMatch(zeile -> zeile.ergebnis() == Zeilenergebnis.OK);
  }

  private static Zeilenergebnis ergebnis(String wert, Rueckgaben rueckgaben) {
    if (rueckgaben != null && wert.equals(rueckgaben.ok())) {
      return Zeilenergebnis.OK;
    }
    if (rueckgaben != null && rueckgaben.weitere().contains(wert)) {
      return Zeilenergebnis.NICHT_OK;
    }
    return Zeilenergebnis.UNBEKANNT;
  }
}
