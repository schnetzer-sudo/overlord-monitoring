package de.kraftwerkone.overlord.monitor.task;

/**
 * Ein Fehler der Pruefung eines Auftrags, so wie er in der Liste {@code errors} der Antwort steht.
 *
 * @param index der Eintrag, ab 0; {@code null} fuer den Auftrag als Ganzes
 * @param feld der Feldname aus der Definition (oder {@code anfrageId}); {@code null}, wenn die
 *     Meldung kein einzelnes Feld betrifft
 * @param meldung deutscher Klartext, der das Feld mit seinem {@code label} nennt und den Eintrag ab
 *     1 zaehlt
 */
public record Prueffehler(Integer index, String feld, Fehlercode code, String meldung) {}
