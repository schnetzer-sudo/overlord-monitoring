package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Treffer auf einer Nachricht, so wie der Aufrufer ihn sieht: <b>worauf getroffen wurde</b>.
 *
 * <p>Der Nutzer hat den Wert getippt. Was er <b>nicht</b> weiß, ist, worauf er getroffen hat — ob
 * seine Nummer als Lieferschein-Nr., als Charge oder als Kundenmaterialnummer auf dieser Nachricht
 * steht. Genau das steht hier, und deshalb ist es keine Wiederholung der Eingabe.
 *
 * @param typ {@code MessageBAM.MessageBAMType}. Er wird <b>nicht</b> angezeigt — er ist zusammen
 *     mit dem Wert der Schlüssel der Liste, dieselbe Regel wie im Belegdaten-Block
 * @param bezeichnung die Beschriftung aus dem Altsystem; fehlt sie, die Typnummer ({@link
 *     Typbezeichnung}). <b>Nie {@code null}</b>
 * @param wert der Wert, <b>wie er im Bestand steht</b>. Trägt er eine führende Null, steht sie hier
 *     — daran erkennt der Nutzer, dass die Normalisierung gegriffen hat
 */
public record BamTrefferWertResponse(short typ, String bezeichnung, String wert) {}
