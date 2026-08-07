package de.kraftwerkone.overlord.monitor.message;

import java.time.LocalDateTime;

/**
 * Eine Zeile, wie sie aus {@code GlassfishDB} kommt: Rohwerte, keine Einordnung, keine Umrechnung.
 *
 * <p>Der Zeitpunkt ist die <b>Wanduhrzeit des Datenbankservers</b> und wird erst im Service zu
 * einem UTC-Zeitpunkt der API ({@code common/Zeitpunkte}). Der Status ist der Rohwert; seine
 * Einordnung entsteht ausschliesslich im {@code MessageStatusClassifier}.
 *
 * <p>{@code processName}, {@code projectName} und {@code sosName} duerfen {@code null} sein — die
 * Spalten sind nullable. Sie werden <b>nicht</b> im Backend durch einen Ersatztext gefuellt: Was
 * der Nutzer anstelle einer fehlenden Zuordnung liest, ist eine Oberflaechenentscheidung und
 * gehoert in die Sprachdateien, nicht in eine Abfrage (Regel Q4).
 *
 * <p><b>{@code sosName} ist der Anzeigename</b> (Projektbeschreibung §3.2) und seit der
 * Nachbesserung zu Schritt 4 die Spalte „Ablauf". Gemessen ist er in L14: durchgaengig gepflegt
 * (1.818 Zeilen, kein {@code NULL}, kein Leerwert) und auf allen 180.251 Nachrichten des dichten
 * Monats aufloesbar. Die Spalte bleibt trotzdem nullable — die Produktion muss sich nicht daran
 * halten, was die Testkopie zufaellig enthaelt.
 */
public record NachrichtZeile(
    String messageId,
    LocalDateTime zeitpunkt,
    String status,
    String processId,
    String processName,
    String projectName,
    String sosName) {}
