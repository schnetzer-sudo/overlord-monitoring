package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.LocalDateTime;

/**
 * Eine Zeile aus „Zuletzt aufgefallen", so wie sie aus {@code Message} kommt.
 *
 * <p><b>Der Rohstatus bleibt roh.</b> Einordnung und Kategorie entstehen im {@code
 * DashboardService} ueber den {@code MessageStatusClassifier} — gerufen, nicht nachgebaut.
 *
 * @param zeitpunkt {@code MessageLastUpdate}, Wanduhrzeit des Quellservers
 * @param sosName der Anzeigename des Ablaufs; darf {@code null} sein. <b>Kein Ersatztext im
 *     Backend</b> (Regel Q4) — was nicht da ist, ist nicht da
 */
record Auffaelligkeitszeile(
    String messageId,
    LocalDateTime zeitpunkt,
    String messageStatus,
    String processId,
    String sosName) {}
