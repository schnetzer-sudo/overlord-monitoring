package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Eine Fehlerart der Kachel <i>Fehler</i>, mit ihrem Rohwert daneben.
 *
 * <p><b>Beide Felder, und keines ersetzt das andere.</b> {@code art} ist die abgeleitete Auskunft
 * ({@code MessageStatusClassifier.fehlerart}), {@code rohwert} die Tatsache aus dem Altsystem. Die
 * Oberflaeche zeigt die Art und kann ueber den Rohwert jederzeit uebersetzen oder verlinken; ohne
 * den Rohwert waere <i>„Vom Partner abgelehnt"</i> eine Zeichenkette, an der sich nichts mehr
 * festmachen liesse.
 *
 * @param rohwert {@code Message.MessageStatus}, unveraendert — etwa {@code ERROR_DUPLICATE} oder
 *     {@code COMMIT_REJECTED}
 * @param art der Namensteil hinter {@code ERROR_}, fuer {@code COMMIT_REJECTED} der feste Text
 *     {@code Vom Partner abgelehnt}, sonst der Rohwert selbst (Regel Q4)
 * @param anzahl wie viele Nachrichten im Fenster diesen Rohwert tragen
 */
public record FehlerartResponse(String rohwert, String art, long anzahl) {}
