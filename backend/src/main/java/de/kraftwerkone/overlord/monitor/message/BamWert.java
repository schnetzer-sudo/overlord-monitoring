package de.kraftwerkone.overlord.monitor.message;

/**
 * Ein einzelner BAM-Wert einer Nachricht, so wie er in {@code MessageBAM} steht.
 *
 * <p>Ein Typ kann je Nachricht <b>mehrfach</b> vorkommen: Der Wert ist Teil des Primaerschluessels
 * {@code (MessageID, MessageBAMType, MessageBAMValue)}. Eine gesplittete Sammelrechnung traegt so
 * mehrere Lieferscheinnummern.
 */
public record BamWert(String messageId, short typ, String wert) {}
