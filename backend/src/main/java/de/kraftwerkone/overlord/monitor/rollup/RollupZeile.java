package de.kraftwerkone.overlord.monitor.rollup;

import java.time.LocalDateTime;

/**
 * Eine Zeile des Rollups, so wie sie aus der Aggregation faellt und so wie sie in {@code
 * message_rollup} steht — Feld fuer Feld dasselbe.
 *
 * <p>Der Typ ist bewusst <b>keine</b> Antwortklasse: Er verlaesst das Paket {@code rollup} nicht.
 * Was das Dashboard sieht, entsteht in 10b aus einer eigenen Leseabfrage mit Mandantenfilter.
 *
 * @param stunde Anfang des Stundeneimers, <b>Wanduhrzeit des Quellservers</b> (siehe {@link
 *     RollupFenster})
 * @param processId {@code Message.ProcessID} — auf der Testkopie tragen 738 der 1.503 Prozesse
 *     ueberhaupt Nachrichten (M87)
 * @param messageStatus der <b>Rohwert</b> aus {@code Message.MessageStatus}, nicht die Einordnung
 *     (E-g). Der {@code MessageStatusClassifier} laeuft in 10b beim Lesen
 * @param anzahl wie viele Nachrichten in diesem Eimer stehen
 */
public record RollupZeile(
    LocalDateTime stunde, String processId, String messageStatus, int anzahl) {}
