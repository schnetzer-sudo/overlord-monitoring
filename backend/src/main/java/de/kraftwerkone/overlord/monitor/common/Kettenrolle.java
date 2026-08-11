package de.kraftwerkone.overlord.monitor.common;

/**
 * Die Stellung einer Nachricht in der Verkettung — <b>eine von vier</b>, und eine Zeile kann
 * mehrere davon tragen.
 *
 * <p>Die vier Werte sind <b>nicht</b> vier Sichten auf dieselbe Beziehung, sondern <b>zwei
 * Beziehungen mal zwei Richtungen</b> (gemessen in {@code docs/messungen-schritt6.md} M25‑2, M23‑1,
 * E4):
 *
 * <table>
 *   <caption>Die vier Spalten und was sie sagen</caption>
 *   <tr><th>Rolle</th><th>Spalte</th><th>steht auf</th><th>bedeutet</th></tr>
 *   <tr><td>{@link #SPLIT_WURZEL}</td><td>{@code Source}</td><td>der Wurzel</td>
 *       <td>„ich habe Kinder"</td></tr>
 *   <tr><td>{@link #SPLIT_KIND}</td><td>{@code SourceMessageID}</td><td>dem Kind</td>
 *       <td>„mein Elternteil ist …"</td></tr>
 *   <tr><td>{@link #MERGE_EINGANG}</td><td>{@code TargetMessageID}</td><td>dem Eingang</td>
 *       <td>„ich bin zusammengefuehrt worden nach …"</td></tr>
 *   <tr><td>{@link #MERGE_ERGEBNIS}</td><td>{@code Target}</td><td>dem Ergebnis</td>
 *       <td>„ich bin aus einer Zusammenfuehrung entstanden"</td></tr>
 * </table>
 *
 * <p><b>Der {@code MessageStatus} sagt ueber die Stellung nichts Verlaessliches.</b> Meist traegt
 * die Wurzel {@code SPLITTED} und das Kind {@code FINISHED} — bei {@code IBIS}, {@code IBISGUS} und
 * {@code ZAST} traegt die Wurzel aber {@code FINISHED}, und das sind genau die Mandanten, die ueber
 * den ganzen Bestand keine einzige Zwischenschritt-Zeile haben (M24‑3). Verlaesslich ist die
 * Spalte, nicht der Status.
 *
 * <p><b>Die Reihenfolge der Konstanten ist die Anzeigereihenfolge.</b> {@link java.util.EnumSet}
 * laeuft in Deklarationsreihenfolge, und {@link Kettenrollen} liefert ein solches — damit ist die
 * Reihenfolge in der Antwort festgelegt und haengt nicht daran, in welcher Reihenfolge die Spalten
 * gelesen wurden.
 *
 * <p>Wo diese Rollen herkommen und warum sie eine <b>Menge</b> sind, steht in {@code
 * docs/verkettung.md}.
 */
public enum Kettenrolle {

  /**
   * Auf diese Nachricht zeigen Kinder — sie ist die Wurzel einer Aufteilung. Traegt in 96,9 Prozent
   * der Faelle die Belegnummer (M26‑1b), ist also die Zeile, die der Nutzer wiedererkennt.
   */
  SPLIT_WURZEL,

  /** Diese Nachricht ist ein Teil einer Aufteilung und zeigt auf ihren Elternteil. */
  SPLIT_KIND,

  /**
   * Diese Nachricht ist in eine andere hineingeflossen. Traegt in der Testkopie <b>keinen
   * einzigen</b> BAM-Wert (M26‑1b) — beim Merge sitzt die Belegnummer auf dem Ergebnis.
   */
  MERGE_EINGANG,

  /** Diese Nachricht ist aus einer Zusammenfuehrung entstanden; auf sie zeigen die Eingaenge. */
  MERGE_ERGEBNIS
}
