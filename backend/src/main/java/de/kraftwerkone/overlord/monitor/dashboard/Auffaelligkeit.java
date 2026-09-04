package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Warum eine Nachricht in „Zuletzt aufgefallen" steht.
 *
 * <h2>⚠️ Es ist seit dem 03.09.2026 nur noch ein Wert, und das Feld bleibt trotzdem</h2>
 *
 * <p>Hier stand daneben {@code UEBERFAELLIG}. Der Wert ist mit E‑71 entfallen: Die Kategorie ist
 * durch eine fachliche Auskunft des Auftraggebers widerlegt, und der Block liest seither <b>nur
 * noch die Fehlerbedingung</b> ({@code DashboardRepository.zuletztAufgefallen}). Ein Enum-Wert, den
 * keine Zeile mehr tragen kann, waere eine Auswahl, die es nicht gibt.
 *
 * <p><b>Das Feld {@code kategorie} in {@link AuffaelligeNachrichtResponse} bleibt.</b> Der
 * Antwortblock ist in diesem Schritt ausdruecklich unveraendert, und die Oberflaeche liest ihn
 * heute — sie darf nicht an einem fehlenden Feld brechen, bevor Schritt 10b‑5 sie anfasst. <b>Dass
 * hier eine Aufzaehlung mit einem Wert steht, ist damit ein benannter Zwischenstand</b> und kein
 * Entwurf: offener Punkt 133.
 *
 * <p>Der urspruengliche Grund fuer die Trennung gilt unveraendert und ist der Grund, warum das Feld
 * nicht stillschweigend verschwindet: Regel Q3 verlangt, dass Problemkategorien getrennt und nie zu
 * „Problem" zusammengefasst werden. Kommt je eine zweite Auffaelligkeit zurueck, steht hier ihr
 * Platz.
 */
public enum Auffaelligkeit {

  /** {@code MessageStatus} beginnt mit {@code ERROR_} oder ist {@code COMMIT_REJECTED}. */
  FEHLER
}
