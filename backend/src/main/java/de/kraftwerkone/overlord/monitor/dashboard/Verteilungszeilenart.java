package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Was fuer eine Zeile der Verteilungsblock zeigt — <b>drei Arten, und zwei davon sind
 * Restzeilen</b>.
 *
 * <p>Die Unterscheidung steht in der Antwort und nicht in der Oberflaeche, weil sie fachlich ist:
 * {@link #WERT} ist ein Partner, {@link #UEBRIGE} ist ein Rangartefakt, und {@link
 * #NICHT_ZUGEORDNET} ist eine Aussage ueber den <b>Katalog</b>. Wer die drei am Text unterscheiden
 * muesste, unterschiede sie irgendwann falsch.
 */
public enum Verteilungszeilenart {

  /** Ein benannter Partner beziehungsweise eine benannte Richtung. Rang 1 bis 10. */
  WERT,

  /**
   * Alles ab Rang 11, zusammengefasst.
   *
   * <p><b>Sie fehlt, wenn es keinen Rang 11 gibt</b> — eine Null sagt dort nichts, sie ist reines
   * Rangartefakt. Bei drei von vier gemessenen Mandanten gibt es sie nicht (M98).
   */
  UEBRIGE,

  /**
   * Was keinem Wert zugeordnet ist (Entscheidung E-i).
   *
   * <p><b>Sie erscheint immer, auch bei null.</b> Das ist eine Aussage ueber den Katalog: Null
   * heisst „alles kuratiert", und {@code IBIS} liefert sie gerade. Wird die Zeile bei null
   * ausgeblendet, ist <i>vollstaendig gepflegt</i> nicht mehr von <i>diese Ansicht zeigt das
   * nicht</i> zu unterscheiden.
   */
  NICHT_ZUGEORDNET
}
