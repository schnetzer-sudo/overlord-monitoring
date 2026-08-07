package de.kraftwerkone.overlord.monitor.message;

import java.time.LocalDateTime;

/**
 * Der Kopf einer Nachricht, roh aus dem Quellschema — vor Einordnung und Zeitumrechnung.
 *
 * @param messageId {@code Message.MessageID}
 * @param status der Rohwert {@code MessageStatus}. Freier Text, kein Aufzaehlungstyp
 * @param zeitpunkt {@code MessageLastUpdate} — Zeitpunkt der <b>letzten Aenderung</b>, kein
 *     Anlagedatum (Regel Q2)
 * @param timeoutSekunden {@code MessageTimeout}, Dauer in Sekunden (Regel Z2)
 * @param processId {@code Message.ProcessID}
 * @param processName aus {@code Process}; nullbar (Regel Q4)
 * @param projectName aus {@code Project}; nullbar
 * @param sosName {@code SOS.SOSName} — der <b>Anzeigename</b> des Ablaufs. Durchgaengig gepflegt
 *     (L14), trotzdem nullbar
 * @param naechsterSchrittName {@code SOSAction.SOSActionName} zu {@code (Message.SOSID,
 *     Message.SOSActionID)}. Nullbar: Ueber den Gesamtbestand laeuft dieser Verweis zu 43,9 Prozent
 *     ins Leere (M13) — die Ursache ist die lueckenhafte Nummerierung der Ablaufdefinition und
 *     nicht ein geaenderter Ablauf (M20). Bei allen 538 wartenden Nachrichten loest er auf
 * @param eigenschaftenAnzahl Anzahl der {@code MessageProperty}-Zeilen dieser Nachricht
 */
public record NachrichtKopfZeile(
    String messageId,
    String status,
    LocalDateTime zeitpunkt,
    Short timeoutSekunden,
    String processId,
    String processName,
    String projectName,
    String sosName,
    String naechsterSchrittName,
    int eigenschaftenAnzahl) {}
