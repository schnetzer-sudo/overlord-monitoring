package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Instant;

/**
 * Eine Lampe des plattformweiten Blocks: ein Dienst mit Zeitgrenze.
 *
 * @param serviceId die Kennung — <b>das Einzige, was diesen Dienst benennt</b> (E‑122). {@code
 *     ServiceName}, {@code ServiceDescription} und {@code ServiceLastStatusMessage} stehen in
 *     keiner Antwort; die letzte steht bei allen fuenf Diensten mit {@code ERROR_TIMEOUT} weiterhin
 *     auf „Heartbeat" und saehe neben einer roten Lampe aus wie ein Widerspruch (<i>gesehen am
 *     10.09.2026, nicht erhoben</i>)
 * @param zustand die Einordnung aus {@link DienstStatusClassifier}
 * @param rohwert {@code ServiceStatus}, unveraendert und <b>immer mitgeliefert</b> (Regel Q4). Er
 *     ist der Anker fuer {@link Dienstzustand#UNGEKLAERT} — ohne ihn liesse sich an einem
 *     unbekannten Wert nichts festmachen, keine Uebersetzung, kein Filter, keine Rueckfrage. {@code
 *     null}, wenn die Spalte leer ist
 * @param stand {@code ServiceLastUpdate} in UTC. <b>Kein Urteil</b>: Ob der Zeitpunkt alt ist,
 *     entscheidet niemand im Backend (E‑118)
 * @param alterSekunden der Abstand zur <b>Anwendungsuhr</b> in ganzen Sekunden (Regel Z1),
 *     gerechnet wie {@code aeltesteSekunden} bei den offenen Kacheln (E‑75). <b>{@code null}, wenn
 *     der Stand nach {@code jetzt} liegt</b> — „meldet sich seit minus drei Sekunden" ist
 *     schlechter als gar keine Angabe. Lokal trifft das zu: Ein Dienst traegt den 13.07.2026 und
 *     liegt damit hinter dem Anker der Anwendungsuhr
 */
public record DienstResponse(
    String serviceId, Dienstzustand zustand, String rohwert, Instant stand, Long alterSekunden) {}
