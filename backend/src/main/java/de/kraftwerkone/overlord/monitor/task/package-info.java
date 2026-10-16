/**
 * Auftraege ueber den Assistenten (Linie {@code assistent}, Teilschritt A1): Typdefinitionen als
 * Daten, ihre Pruefung beim Laden, die Pruefung eines Auftrags, der Parametertext fuer die
 * EDI-Plattform, das Lesen ihrer Rueckgabe und die Einordnung.
 *
 * <p><b>Kein Typ steht im Code</b> (E‑1018). Was einen Typ ausmacht — Aktionen, Felder, Muster,
 * Schluessel, Regeln, Zeilenvorlagen, Rueckgabewerte —, steht als JSON in {@code
 * overlord_monitor.task_type} und wird hier gegen einen festen Satz von Regelbausteinen gelesen.
 * Ein Test haelt fest, dass der erste Typ im Hauptcode nirgends vorkommt.
 *
 * <p><b>Ausschliesslich {@code overlord_monitor}</b>, gelesen und geschrieben ueber {@code
 * monitorDsl}; {@code GlassfishDB} fasst dieses Paket nicht an. Die Regeln 1 bis 6 aus {@code
 * PROJEKTBESCHREIBUNG.md} §8 gelten hier nicht (E‑1016), Regel 7 schon.
 *
 * <p>Beschrieben in {@code docs/tasks.md}, der Vertrag fuer die Plattform in {@code
 * docs/task-schnittstelle.md}.
 */
package de.kraftwerkone.overlord.monitor.task;
