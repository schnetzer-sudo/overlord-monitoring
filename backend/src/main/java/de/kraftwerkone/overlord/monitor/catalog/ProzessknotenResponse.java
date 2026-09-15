package de.kraftwerkone.overlord.monitor.catalog;

import java.time.Instant;

/**
 * Ein Blatt des Baums: <b>ein Prozess</b> mit seinen Kennzahlen im Fenster und seinem
 * fensterunabhaengigen Zustand — <b>in beiden Gliederungen dasselbe Blatt</b>.
 *
 * <p><i>(seit 15.09.2026.)</i> Beide Gliederungen enden hier und uebergeben dieselbe {@code
 * processId} an dieselbe Uebertragungsliste. Der Knoten traegt dafuer die Felder aller Knoten
 * ({@link BaumknotenResponse}) und dazu die eines Prozesses.
 *
 * <p><b>Korrektur vom 15.09.2026.</b> An {@code processName} stand bis dahin: <i>„der Klarname aus
 * der Datenbank ({@code Process.ProcessName}, etwa {@code 40000_AMG_LAB_VDA})"</i>. Das Beispiel
 * war der Wert von {@code ProcessID}; der Klarname desselben Prozesses ist {@code AMG LAB (VDA)}
 * (nachgesehen an der Testkopie am 15.09.2026). Der Code las schon immer {@code ProcessName}, nur
 * das Beispiel war falsch. Das Feld heisst seither {@code name}, wie an jedem Knoten.
 *
 * @param schluessel die {@code ProcessID}. Sie steht neben {@link #processId} ein zweites Mal,
 *     damit jeder Knoten des Baums denselben Schluessel traegt — ueber den ganzen Baum eindeutig
 * @param name der Klarname aus {@code Process.ProcessName} — nicht die {@code ProcessID} und nicht
 *     der {@code SOSName}. Darf {@code null} sein; was der Nutzer dann liest, gehoert in die
 *     Sprachdateien (Regel Q4)
 * @param processId die {@code ProcessID} — der Wert, der als {@code prozess} an {@code
 *     /api/nachrichten} zurueckgeht. Fuer den Nutzer ist sie Beiwerk
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
    String schluessel,
    String name,
    String processId,
    long nachrichten,
    long fehler,
    Instant letzteBewegung,
    Prozesszustand zustand)
    implements BaumknotenResponse {}
