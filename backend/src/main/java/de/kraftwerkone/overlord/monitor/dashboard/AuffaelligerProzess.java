package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.LocalDateTime;

/**
 * Eine Zeile aus „Zuletzt aufgefallen" — <b>ein Prozess, nicht eine Nachricht</b>.
 *
 * <h2>Hier stand {@code Auffaelligkeitszeile}, und der Unterschied ist der ganze Schritt</h2>
 *
 * <p>Bis zum 04.09.2026 trug dieser Block <b>eine Zeile je Nachricht</b>. Am laufenden System sah
 * das so aus: zehn Zeilen, zehnmal derselbe Zeitstempel, zehnmal derselbe Ablauf — bei {@code
 * NEXANS} ueber 48 Stunden stammen <b>49 der 50 Fehler aus einem einzigen Prozess</b>. Der Block
 * listete Nachrichten, wo er Prozesse listen sollte, und ein einziger Prozess fuellte die Liste
 * allein. <b>Keine Zeile trug eine eigene Auskunft.</b>
 *
 * <p>Seit E‑90 traegt jede Zeile <b>Anzahl</b> und <b>juengsten Zeitpunkt</b> je Prozess. Aus zehn
 * gleichen Zeilen werden zwei verschiedene, und die sagen mehr als die zehn davor.
 *
 * <h2>Der Name kommt aus {@code Process.ProcessName} und nicht mehr aus {@code SOS.SOSName}</h2>
 *
 * <p><b>Das folgt aus der Verdichtung und ist keine Geschmacksfrage.</b> Das Verhaeltnis Process zu
 * SOS ist meist 1:1, gelegentlich 1:n ({@code docs/datenmodell.md}) — eine Gruppe je Prozess hat
 * damit unter Umstaenden <i>mehrere</i> Ablaufnamen, und einen davon auszuwaehlen hiesse raten
 * (Regel Q4). {@code ProcessName} gehoert dem Prozess allein, ist in Klartext gepflegt und ist
 * derselbe Anzeigename, den der Prozesskatalog und die Prozessansicht tragen ({@code
 * ProzessknotenResponse}).
 *
 * @param processId die Prozesskennung; nie {@code null} — eine Zeile ohne sie kaeme durch die
 *     Mandantenkette nicht hindurch, weil die auf {@code Process.ProcessID} verbindet
 * @param processName der Klarname aus {@code Process.ProcessName}. <b>Darf {@code null} sein</b> —
 *     die Spalte ist im Altsystem nicht pflichtig, und was nicht da ist, ist nicht da (Regel Q4).
 *     Kein Ersatztext im Backend
 * @param anzahl wie viele auffaellige Nachrichten dieser Prozess im Fenster hat
 * @param zuletzt der juengste {@code MessageLastUpdate} darunter, Wanduhrzeit des Quellservers
 */
record AuffaelligerProzess(
    String processId, String processName, int anzahl, LocalDateTime zuletzt) {}
