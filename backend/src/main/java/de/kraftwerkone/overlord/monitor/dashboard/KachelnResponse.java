package de.kraftwerkone.overlord.monitor.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Die Kacheln des Dashboards (Bloecke 2, 3, 4 und 4a).
 *
 * <p><b>Die ersten beiden kommen aus demselben Lesevorgang wie der Verlauf</b> — die Kachel
 * <i>Nachrichten</i> ist dessen Summe, die Kachel <i>Fehler</i> dessen Teilsumme. Die beiden
 * uebrigen lesen <b>live ueber {@code Message}</b> und sind die zwei benannten Ausnahmen von
 * Leistungsregel L2 ({@link OffeneKachelResponse}).
 *
 * <p><b>Die Kachel <i>Ueberfaellig</i> ist am 03.09.2026 ersatzlos entfallen</b> (E‑71). Die
 * Kategorie ist durch eine fachliche Auskunft des Auftraggebers widerlegt; sie hat im
 * eingeschwungenen Zustand keine wahren Treffer. Ausgeschrieben in {@code PROJEKTBESCHREIBUNG.md}
 * §4.2 und {@code docs/message-status.md}.
 *
 * @param nachrichten {@code SUM(anzahl)} ueber das ganze Fenster. <b>Sie zaehlt Aktivitaet und
 *     nicht Nachrichten</b>, und das ist eine bekannte Grenze: {@code message_rollup} gruppiert
 *     nach {@code MessageLastUpdate}, und eine Nachricht, die ihren Status wechselt, wandert in
 *     einen anderen Eimer. Sie verschwindet dabei aus dem alten — doppelt gezaehlt wird also nichts
 *     —, aber sie erscheint in der Zaehlung eines Zeitraums, in dem sie nicht entstanden ist.
 *     Ausgeschrieben in {@code docs/dashboard.md}
 * @param fehler Zahl und Aufschluesselung nach Art
 * @param laeuft wie viele Nachrichten gerade laufen ({@code RUNNING}) und seit wann die aelteste —
 *     <b>live, ohne Zeitfenster</b>. <b>Immer da</b>, auch mit {@code anzahl = 0}: Dass gerade
 *     nichts laeuft, ist eine Auskunft, und jeder Mandant kann laufende Nachrichten haben
 * @param wartend dasselbe fuer {@code SUSPENDED} — aber <b>{@code null}, wenn der Mandant keinen
 *     Prozess hat, dessen geplanter Ablauf ueberhaupt suspendiert</b> (E‑74). Das Feld faellt dann
 *     ueber {@link JsonInclude} <b>ganz aus der Antwort</b>: Es ist entweder vollstaendig da oder
 *     gar nicht — kein {@code null}, kein {@code sichtbar: false}. Ein Feld, das seine eigene
 *     Abwesenheit beschriebe, verlangte von der Oberflaeche zwei Pruefungen statt einer
 */
public record KachelnResponse(
    long nachrichten,
    FehlerkachelResponse fehler,
    OffeneKachelResponse laeuft,
    @JsonInclude(JsonInclude.Include.NON_NULL) OffeneKachelResponse wartend) {}
