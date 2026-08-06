package de.kraftwerkone.overlord.monitor.message;

import java.time.Instant;

/**
 * Eine Zeile der Nachrichtenliste, so wie der Aufrufer sie sieht.
 *
 * <p><b>Status doppelt, und das mit Absicht.</b> {@code status} ist der Rohwert des Altsystems —
 * damit ein Anwender ihn gegen die alte Oberflaeche halten kann und damit ein unbekannter Wert
 * ueberhaupt sichtbar wird. {@code statusKind} ist die fachliche Einordnung, an der die Oberflaeche
 * Farbe und Sortierung festmacht. Die Zuordnung entsteht ausschliesslich im {@code
 * MessageStatusClassifier} und wird nirgends nachgebaut.
 *
 * <p>Kein {@code SOSName}, keine Verkettung, kein Timeout: Die Liste beantwortet „wo steht mein
 * Beleg", nicht „was ist im Einzelnen passiert" (Schritt 5) und nicht „was haengt daran" (Schritt
 * 6).
 *
 * @param zeitpunkt {@code MessageLastUpdate} als UTC-Zeitpunkt. <b>Kein Anlagedatum</b> — die
 *     Quelle hat keines (Regel Q2).
 * @param bedeutungNichtVerifiziert bei {@code UNGEKLAERT}: Der Wert kommt so aus dem Altsystem,
 *     seine fachliche Bedeutung ist nicht belegt. Die Oberflaeche kennzeichnet das, statt einen
 *     plausiblen Text zu erfinden.
 * @param processName {@code null}, wenn die Quelle keinen Namen fuehrt — nicht zugeordnet heisst
 *     nicht zugeordnet (Regel Q4); den Ersatztext waehlt die Oberflaeche
 */
public record NachrichtResponse(
    String messageId,
    Instant zeitpunkt,
    String status,
    String statusKind,
    boolean bedeutungNichtVerifiziert,
    String processId,
    String processName,
    String projectName) {}
