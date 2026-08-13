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
 * @param sosId {@code Message.SOSID} — der Ablauf, auf den der Verweis der Nachricht zeigt
 * @param sosActionId {@code Message.SOSActionID} — der Schritt, auf den er zeigt. Zusammen mit
 *     {@code sosId} entscheidet er ueber {@link OffenerZustand#WARTET_IN} gegen {@link
 *     OffenerZustand#WARTET_VOR}. <b>Beide Spalten stehen im Kopf, seit die Aufteilung im Backend
 *     faellt</b> (M29, 10.08.2026) — vorher brauchte sie nur der {@code LEFT JOIN} auf {@code
 *     SOSAction}, und die Oberflaeche verglich stattdessen zwei Namen
 * @param naechsterSchrittName {@code SOSAction.SOSActionName} zu {@code (Message.SOSID,
 *     Message.SOSActionID)}. Nullbar: Ueber den Gesamtbestand laeuft dieser Verweis zu 43,9 Prozent
 *     ins Leere (M13) — die Ursache ist die lueckenhafte Nummerierung der Ablaufdefinition und
 *     nicht ein geaenderter Ablauf (M20). Bei allen 538 wartenden Nachrichten loest er auf (M29 3)
 * @param eigenschaftenAnzahl Anzahl der {@code MessageProperty}-Zeilen dieser Nachricht
 * @param bamAnzahl Anzahl der {@code MessageBAM}-Zeilen dieser Nachricht — die Belegdaten, die
 *     unter {@code GET /api/nachrichten/&#123;id&#125;/bam} liegen. Dieselbe Bauform wie {@code
 *     eigenschaftenAnzahl}: eine zaehlende Unterabfrage ueber den Praefix des Primaerschluessels,
 *     die keinen einzigen Wert liest. Sie steht hier, weil <b>80,6 Prozent</b> aller Nachrichten
 *     keinen BAM-Wert tragen (M41) — ohne sie zeichnete die Oberflaeche einen Block, um
 *     festzustellen, dass er leer ist
 * @param source {@code Message.Source} als {@link Boolean} — der Codegen bildet {@code bit(1)} per
 *     {@code forcedType} ab ({@code datenzugriff.md} §9). Der Rueckwaertsindex der Aufteilung: Er
 *     deckt sich exakt mit „hat mindestens ein Kind" (E4), <b>ohne Abfrage</b>
 * @param sourceMessageId {@code Message.SourceMessageID} — der Verweis des Kindes auf seinen
 *     Elternteil
 * @param targetMessageId {@code Message.TargetMessageID} — der Verweis des Eingangs auf das
 *     Ergebnis
 * @param target {@code Message.Target}, ebenso {@link Boolean} — der Rueckwaertsindex der
 *     Zusammenfuehrung (E4)
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
    String sosId,
    Short sosActionId,
    String naechsterSchrittName,
    int eigenschaftenAnzahl,
    int bamAnzahl,
    Boolean source,
    String sourceMessageId,
    String targetMessageId,
    Boolean target) {}
