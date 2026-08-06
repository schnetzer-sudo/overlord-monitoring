package de.kraftwerkone.overlord.monitor.message;

import java.time.LocalDateTime;

/**
 * Eine Zeile, wie sie aus {@code GlassfishDB} kommt: Rohwerte, keine Einordnung, keine Umrechnung.
 *
 * <p>Der Zeitpunkt ist die <b>Wanduhrzeit des Datenbankservers</b> und wird erst im Service zu
 * einem UTC-Zeitpunkt der API ({@code common/Zeitpunkte}). Der Status ist der Rohwert; seine
 * Einordnung entsteht ausschliesslich im {@code MessageStatusClassifier}.
 *
 * <p>{@code processName} und {@code projectName} duerfen {@code null} sein — die Spalten sind
 * nullable. Sie werden <b>nicht</b> im Backend durch einen Ersatztext gefuellt: Was der Nutzer
 * anstelle einer fehlenden Zuordnung liest, ist eine Oberflaechenentscheidung und gehoert in die
 * Sprachdateien, nicht in eine Abfrage (Regel Q4).
 */
public record NachrichtZeile(
    String messageId,
    LocalDateTime zeitpunkt,
    String status,
    String processId,
    String processName,
    String projectName) {}
