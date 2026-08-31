package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import java.time.Instant;

/**
 * Eine Zeile des Blocks „Zuletzt aufgefallen" (Block 6).
 *
 * <p><b>Sie traegt genau so viel, wie ein Klick braucht.</b> Der typische Nutzer sucht einen Beleg
 * und will wissen, wo er steht — {@code messageId} fuehrt ins Detail, {@code sosName} sagt ihm,
 * worum es geht, {@code zeitpunkt} und {@code kategorie} sagen, warum die Zeile hier steht. Mehr
 * gehoert in die Liste und nicht auf die Landingpage.
 *
 * @param messageId die Kennung, mit der die Detailansicht erreichbar ist
 * @param zeitpunkt {@code MessageLastUpdate} als UTC-Zeitpunkt
 * @param status der <b>Rohwert</b> aus dem Altsystem — {@code ERROR_DUPLICATE}, {@code SUSPENDED}
 *     und so fort
 * @param statusKind die Einordnung dazu ({@code MessageStatusClassifier.einordnung})
 * @param kategorie warum die Zeile aufgefallen ist. Fehler und ueberfaellig schliessen einander aus
 * @param processId die Prozesskennung
 * @param sosName der Anzeigename des Ablaufs; darf {@code null} sein (Regel Q4)
 */
public record AuffaelligeNachrichtResponse(
    String messageId,
    Instant zeitpunkt,
    String status,
    MessageStatusKind statusKind,
    Auffaelligkeit kategorie,
    String processId,
    String sosName) {}
