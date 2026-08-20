package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Die zwei Pflegezustaende einer Katalogzeile — und es sind bewusst nur zwei (E4).
 *
 * <p>Zusammen mit dem Partnerfeld tragen sie <b>drei</b> Bedeutungen, ohne dass ein dritter Zustand
 * noetig waere:
 *
 * <table border="1">
 *   <caption>Zustand und Bedeutung</caption>
 *   <tr><td>{@code OFFEN}, Feld leer</td><td>noch nicht angesehen, Heuristik hat nichts
 *       gefunden</td></tr>
 *   <tr><td>{@code OFFEN}, Feld gefuellt</td><td><b>Vorschlag</b> der Heuristik,
 *       unbestaetigt</td></tr>
 *   <tr><td>{@code GEPFLEGT}, Feld gefuellt</td><td>kuratiert</td></tr>
 *   <tr><td>{@code GEPFLEGT}, Feld leer</td><td><b>hingesehen, es gibt nichts</b></td></tr>
 * </table>
 *
 * <p>Ein dritter Zustand „nicht zuordenbar" ist ausdruecklich verworfen ({@code
 * docs/prozess-katalog.md} §9): „gepflegt mit leerem Partner" sagt dasselbe mit den Feldern, die
 * ohnehin da sind.
 */
public enum Pflegestatus {
  OFFEN,
  GEPFLEGT
}
