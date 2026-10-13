package de.kraftwerkone.overlord.monitor.dashboard;

import java.util.List;

/**
 * Die Kachel <i>Fehler</i> (Block 3): eine Zahl <b>und</b> ihre Aufschluesselung nach Art.
 *
 * <p><b>Die Aufschluesselung kostet keinen zweiten Lesevorgang.</b> Sie entsteht aus demselben
 * Rohwert, den der Verlauf ohnehin liest — deshalb gruppiert {@code DashboardRepository.verlauf}
 * nach Rohstatus und nicht schon nach Einordnung.
 *
 * <p><b>„Unquittiert" gibt es hier nicht</b> — kein Feld, kein Platzhalter, keine leere Liste
 * (Entscheidung E-d vom 24.08.2026). Die Kategorie ist aus dem MVP genommen, weil sie keine
 * operative Definition hat. {@code COMMIT_REJECTED} gehoert ohnehin nicht dorthin, sondern hierher:
 * Es ist eine Quittung, nur eine negative. {@code EERP_FAILED} ebenso (E‑233 vom 09.10.2026): Es
 * ist ein Fehler, und derselbe Beleg stuende sonst in zwei Kategorien.
 *
 * @param anzahl alle Nachrichten im Fenster, deren Einordnung {@code FEHLER} ist
 * @param arten absteigend nach {@code anzahl}, bei Gleichstand nach Rohwert — damit die Reihenfolge
 *     zwischen zwei Aufrufen nicht springt
 */
public record FehlerkachelResponse(long anzahl, List<FehlerartResponse> arten) {

  public FehlerkachelResponse {
    arten = List.copyOf(arten);
  }
}
