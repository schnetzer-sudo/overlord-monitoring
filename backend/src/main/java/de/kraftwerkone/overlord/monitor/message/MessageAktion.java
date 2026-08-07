package de.kraftwerkone.overlord.monitor.message;

import java.time.LocalDateTime;

/**
 * Ein <b>ausgefuehrter</b> Schritt — eine Zeile aus {@code MessageAction}, roh und unbewertet.
 *
 * <p>Gegenstueck zu {@link Ablaufschritt}, dem geplanten Schritt. Der Unterschied ist nicht
 * kosmetisch: {@link #messageActionId()} ist die <b>Position</b> in dieser Nachricht, {@link
 * #sosActionId()} der <b>Schluessel</b> in die Ablaufdefinition. Beide beginnen bei {@code 0} und
 * laufen trotzdem auseinander — der dritte Schritt einer Nachricht traegt je nach Ablauf die
 * Kennung 2, 3 oder 10 (M15).
 *
 * <p>Die Zeitstempel sind <b>Wanduhrzeit des Datenbankservers</b> und werden erst beim Bau der
 * Antwort nach UTC gerechnet, mit derselben Umrechnung wie in der Nachrichtenliste ({@code
 * common/Zeitpunkte}).
 *
 * @param messageActionId laufende Position je Nachricht, beginnt bei {@code 0} (M15)
 * @param sosId der Ablauf <b>dieser Aktion</b>. In 1,55 Prozent der Faelle ein anderer als {@code
 *     Message.SOSID} (M20) — deshalb wird ueber diese Spalte aufgeloest und nicht ueber jene
 * @param sosActionId Schluessel in die Ablaufdefinition. {@code 0} kennzeichnet den
 *     Metadaten-Schritt (S1)
 * @param start {@code MessageActionStart}. In der Testkopie auf keiner einzigen der 10,3 Millionen
 *     Zeilen {@code NULL} (M22) — die Spalte laesst es zu
 * @param ende {@code MessageActionEnd}. {@code NULL} heisst „nicht beendet"; im Gesamtbestand
 *     tragen 95 von 10.308.590 Aktionen dieses Merkmal (M22)
 * @param bausteine {@code SOSActionServiceProperties}, pipe-getrennt. Ausschliesslich auf dem
 *     Metadaten-Schritt {@code NULL} (S1)
 * @param timeoutSekunden {@code SOSActionTimeout}, Dauer in <b>Sekunden</b> wie {@code
 *     Message.MessageTimeout} (Regel Z2). Im Tagesfenster kommen genau zwei Werte vor: {@code 1800}
 *     auf jedem echten Schritt und {@code 0} fast ausschliesslich auf dem Metadaten-Schritt (M16 4)
 */
public record MessageAktion(
    short messageActionId,
    String sosId,
    short sosActionId,
    LocalDateTime start,
    LocalDateTime ende,
    String bausteine,
    Short timeoutSekunden) {

  /**
   * Der Metadaten-Schritt — <b>kein Prozessschritt</b> und deshalb nicht in der Schrittfolge.
   *
   * <p>Das Kriterium ist {@code SOSActionID = 0} und nicht {@code MessageActionID = 0} oder „traegt
   * keine Bausteine". S1 hat alle drei gegeneinander gemessen: Sie treffen ueber 704.427 Aktionen
   * beider Fenster <b>dieselbe</b> Menge, null Abweichungen in allen drei Paarvergleichen. Gewaehlt
   * ist {@code SOSActionID}, weil es die Spalte ist, ueber die gejoint wird — der Ausschluss und
   * seine Wirkung stehen damit in derselben Spalte.
   *
   * <p>An ihm haengen die Metadaten der Nachricht: 58,8 Prozent aller {@code MessageProperty}-
   * Zeilen und ausnahmslos die ganze {@code Message.*}-Familie (M17 3). <b>Seine Eigenschaften
   * gehen deshalb nicht verloren</b> — sie werden ueber die {@code MessageID} gelesen, nicht ueber
   * die Aktion.
   */
  public boolean istMetadatenSchritt() {
    return sosActionId == 0;
  }

  /** Ob fuer diese Aktion kein Ende protokolliert ist. */
  public boolean ohneEnde() {
    return ende == null;
  }
}
