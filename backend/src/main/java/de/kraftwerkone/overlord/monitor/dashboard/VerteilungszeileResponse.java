package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Eine Zeile des Verteilungsblocks.
 *
 * @param art {@link Verteilungszeilenart#WERT}, {@link Verteilungszeilenart#UEBRIGE} oder {@link
 *     Verteilungszeilenart#NICHT_ZUGEORDNET}
 * @param wert der Partnername beziehungsweise die Richtung — <b>nur bei {@code WERT} gesetzt</b>,
 *     sonst {@code null}. Die Beschriftung der beiden Restzeilen macht die Oberflaeche: „nicht
 *     zugeordnet" ist ein Anzeigetext und keine Tatsache aus der Datenbank (Regel Q4)
 * @param anzahl die Summe dieser Zeile
 * @param enthaltene wie viele Werte in dieser Zeile zusammengefasst sind — <b>nur bei {@code
 *     UEBRIGE} gesetzt</b>. Sie steht dort, weil „Übrige" ohne Zahl nicht einzuordnen ist: Bei
 *     {@code IBIS} sind es vierzig
 */
public record VerteilungszeileResponse(
    Verteilungszeilenart art, String wert, long anzahl, Integer enthaltene) {

  static VerteilungszeileResponse wert(String wert, long anzahl) {
    return new VerteilungszeileResponse(Verteilungszeilenart.WERT, wert, anzahl, null);
  }

  static VerteilungszeileResponse uebrige(int enthaltene, long anzahl) {
    return new VerteilungszeileResponse(Verteilungszeilenart.UEBRIGE, null, anzahl, enthaltene);
  }

  static VerteilungszeileResponse nichtZugeordnet(long anzahl) {
    return new VerteilungszeileResponse(Verteilungszeilenart.NICHT_ZUGEORDNET, null, anzahl, null);
  }
}
