package de.kraftwerkone.overlord.monitor.common;

/**
 * Der Block {@code fehlerLive} einer Antwort: ob die Fehler der Zahlen aus der Live-Lesung kommen
 * ({@code docs/fehler-live.md}).
 *
 * <p>Die Oberflaeche sagt bei {@link FehlerLiveZustand#AUSGESETZT}, dass die Fehlerzahlen aus der
 * stuendlichen Aggregation stammen und nachverarbeitete Nachrichten darin noch als Fehler zaehlen
 * koennen. Bei {@link FehlerLiveZustand#ANGEWANDT} sagt sie nichts.
 *
 * <p><b>In {@code common} und nicht im Dashboard</b>, gebaut wie {@link LiveRestResponse} (E-189):
 * Mit Teil B traegt der Prozessbaum denselben Block, und Fachpakete kennen einander nicht ({@code
 * docs/PROJEKTBESCHREIBUNG.md} §6). <b>Keine Zeitangabe</b> — anders als beim Live-Rest gibt es
 * keinen Zeitpunkt, bis zu dem die Fehler stimmen: Ein Abgang kann jeden Eimer vor dem letzten
 * Volllauf treffen.
 *
 * @param zustand einer der zwei Zustaende
 */
public record FehlerLiveResponse(FehlerLiveZustand zustand) {

  /** Der Block aus dem Ergebnis des Bausteins. */
  public static FehlerLiveResponse aus(FehlerLiveErgebnis ergebnis) {
    return new FehlerLiveResponse(ergebnis.zustand());
  }
}
