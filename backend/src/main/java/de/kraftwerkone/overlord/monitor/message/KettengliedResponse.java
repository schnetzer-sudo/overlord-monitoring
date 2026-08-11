package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import java.time.Instant;
import java.util.List;

/**
 * Ein Glied der Kette — <b>genug fuer eine Zeile, nicht mehr</b>.
 *
 * <p>Keine Schrittfolge, keine Eigenschaften, keine BAM-Werte. Wer ein Glied ansehen will, oeffnet
 * es, und dafuer gibt es {@code GET /api/nachrichten/&#123;messageId&#125;}. Eine Kette mit 50
 * Gliedern waere sonst 50 Detailantworten in einer.
 *
 * @param messageId {@code Message.MessageID} — die Kennung, mit der sich dieses Glied oeffnen
 *     laesst
 * @param status der Rohwert des Altsystems, damit ein Anwender ihn gegen die alte Oberflaeche
 *     halten kann
 * @param statusKind die fachliche Einordnung aus {@code common/MessageStatusClassifier}. <b>Hier
 *     wird nicht neu klassifiziert</b> — die Einordnung entsteht an genau einer Stelle im Code
 * @param zeitpunkt {@code MessageLastUpdate} als UTC-Zeitpunkt. Kein Anlagedatum (Regel Q2);
 *     zugleich die erste Haelfte des Sortierschluessels, nach dem die Abwaertsglieder geblaettert
 *     werden
 * @param sosName der Anzeigename des Ablaufs, {@code SOS.SOSName}. Nullbar
 * @param rollen die Stellung dieses Glieds in der Verkettung — <b>immer vorhanden, leer statt
 *     fehlend</b>. Eine Zeile kann zwei Rollen tragen (514 von 214.330, M28‑1c), nie mehr
 * @param ebene <b>negativ aufwaerts, {@code +1} abwaerts</b>. Der Aufstieg wird vollstaendig
 *     aufgeloest, der Abstieg genau eine Ebene — die Zahl ist damit im Abstieg immer {@code 1} und
 *     steht trotzdem da, weil sie es in einer geschachtelten Darstellung nicht mehr waere
 * @param beziehung ob dieses Glied durch <b>Aufteilung</b> oder durch <b>Zusammenfuehrung</b>
 *     zusammenhaengt. Ableitbar aus {@code rollen} und {@code ebene} — steht trotzdem ausdruecklich
 *     hier, weil genau dieser Unterschied das ist, was die Oberflaeche dem Nutzer sagen muss
 */
public record KettengliedResponse(
    String messageId,
    String status,
    String statusKind,
    Instant zeitpunkt,
    String sosName,
    List<Kettenrolle> rollen,
    int ebene,
    Kettenbeziehung beziehung) {}
