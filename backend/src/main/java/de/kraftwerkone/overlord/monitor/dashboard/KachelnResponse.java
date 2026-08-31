package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Die Kacheln des Dashboards (Bloecke 2 und 3).
 *
 * <p><b>Beide kommen aus demselben Lesevorgang wie der Verlauf</b> — die Kachel <i>Nachrichten</i>
 * ist dessen Summe, die Kachel <i>Fehler</i> dessen Teilsumme.
 *
 * @param nachrichten {@code SUM(anzahl)} ueber das ganze Fenster. <b>Sie zaehlt Aktivitaet und
 *     nicht Nachrichten</b>, und das ist eine bekannte Grenze: {@code message_rollup} gruppiert
 *     nach {@code MessageLastUpdate}, und eine Nachricht, die ihren Status wechselt, wandert in
 *     einen anderen Eimer. Sie verschwindet dabei aus dem alten — doppelt gezaehlt wird also nichts
 *     —, aber sie erscheint in der Zaehlung eines Zeitraums, in dem sie nicht entstanden ist.
 *     Ausgeschrieben in {@code docs/dashboard.md}
 * @param fehler Zahl und Aufschluesselung nach Art
 * @param ueberfaellig zwei Zahlen, beide <b>live</b> ueber {@code Message} — die erste benannte
 *     Ausnahme von Leistungsregel L2, und die einzigen beiden Felder der ganzen Antwort, die „nicht
 *     ermittelbar" sein duerfen
 */
public record KachelnResponse(
    long nachrichten, FehlerkachelResponse fehler, UeberfaelligkachelResponse ueberfaellig) {}
