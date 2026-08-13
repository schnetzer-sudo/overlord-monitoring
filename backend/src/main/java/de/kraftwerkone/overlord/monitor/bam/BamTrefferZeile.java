package de.kraftwerkone.overlord.monitor.bam;

import java.time.LocalDateTime;

/**
 * Eine Trefferzeile, wie sie aus {@code GlassfishDB} kommt: Rohwerte, keine Einordnung, keine
 * Umrechnung. Dieselbe Gestalt wie {@code message/NachrichtZeile} — <b>zuzüglich der vier
 * Verkettungsspalten</b>.
 *
 * <p><b>Warum die vier Spalten hier stehen.</b> Sie kosten <b>keinen</b> zusätzlichen Zugriff (E4):
 * Sie stehen auf der {@code Message}-Zeile, die die Abfrage ohnehin liest. Und sie werden
 * gebraucht, weil die Suche fast immer die <b>Wurzel</b> findet — 96,87 Prozent der Wurzeln tragen
 * BAM-Werte, aber nur 2,42 Prozent der Kinder (M26‑1b, M39). Ohne die Rolle stünde in der
 * Trefferliste eine Nachricht, von der der Nutzer nicht weiß, dass unter ihr noch fünfzig weitere
 * hängen.
 *
 * <p>Der Zeitpunkt ist die <b>Wanduhrzeit des Datenbankservers</b> und wird erst im Service zu
 * einem UTC-Zeitpunkt der API ({@code common/Zeitpunkte}). Der Status ist der Rohwert; seine
 * Einordnung entsteht ausschließlich im {@code MessageStatusClassifier}.
 *
 * <p>{@code processName}, {@code projectName}, {@code sosName} und {@code schritt} dürfen {@code
 * null} sein — die Spalten sind nullable, und was der Nutzer anstelle einer fehlenden Zuordnung
 * liest, ist eine Oberflächenentscheidung (Regel Q4).
 */
public record BamTrefferZeile(
    String messageId,
    LocalDateTime zeitpunkt,
    String status,
    String processId,
    String processName,
    String projectName,
    String sosName,
    String schritt,
    Boolean source,
    String sourceMessageId,
    String targetMessageId,
    Boolean target) {}
