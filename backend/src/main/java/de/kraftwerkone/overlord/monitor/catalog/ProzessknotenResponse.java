package de.kraftwerkone.overlord.monitor.catalog;

import java.time.Instant;

/**
 * Ein Blatt des Baums: <b>ein Prozess</b> mit seinen Kennzahlen im Fenster und seinem
 * fensterunabhaengigen Zustand.
 *
 * @param processId die {@code ProcessID} — der Wert, der als {@code prozess} an {@code
 *     /api/nachrichten} zurueckgeht. Fuer den Nutzer ist sie Beiwerk
 * @param processName der <b>Klarname aus der Datenbank</b> ({@code Process.ProcessName}, etwa
 *     {@code 40000_AMG_LAB_VDA}) — nicht die {@code ProcessID} und nicht der {@code SOSName}. Darf
 *     {@code null} sein; was der Nutzer dann liest, gehoert in die Sprachdateien (Regel Q4)
 * @param nachrichten die Summe im gewaehlten Fenster. <b>Null ist eine Aussage</b> und kein
 *     fehlender Wert: Der Prozess steht im Baum, weil er dem Mandanten gehoert, nicht weil er
 *     Verkehr hatte
 * @param fehler davon eingeordnet als {@code FEHLER} ({@code common/MessageStatusClassifier},
 *     gerufen und nicht nachgebaut). <b>Nicht</b> ueberfaellig und nicht unquittiert — die
 *     Problemkategorien bleiben getrennt (Regel Q3)
 * @param letzteBewegung der juengste Rollupeimer dieses Prozesses, <b>ohne jedes Zeitfenster</b>.
 *     {@code null} heisst {@link Prozesszustand#NIE}
 * @param zustand {@link Prozesszustand} — abgeleitet allein aus {@code letzteBewegung}
 */
public record ProzessknotenResponse(
    String processId,
    String processName,
    long nachrichten,
    long fehler,
    Instant letzteBewegung,
    Prozesszustand zustand) {}
