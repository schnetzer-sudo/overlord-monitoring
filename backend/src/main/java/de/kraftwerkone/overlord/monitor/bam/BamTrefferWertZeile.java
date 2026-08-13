package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Treffer auf einer Nachricht: <b>welcher Typ mit welchem Wert getroffen hat</b>.
 *
 * <p>Eine Nachricht kann mehrere davon tragen. M37 misst, dass bei <b>4,17 Prozent</b> der Paare
 * {@code (MessageID, MessageBAMValue)} derselbe Wert unter mehreren Typen steht — nie unter mehr
 * als vier. Die größte Kombination ist 9028/9029 mit 15.790 gemeinsamen Werten.
 *
 * @param typ {@code MessageBAM.MessageBAMType}
 * @param wert der Wert, <b>wie er im Bestand steht</b> — mit führender Null, wenn er eine trägt.
 *     Genau daran erkennt der Nutzer, dass die Normalisierung gegriffen hat
 * @param bezeichnung {@code MessageBAMType.MessageBAMTypeDescription}, roh. {@code null}, wenn die
 *     Zeile im Altsystem fehlt — den Ersatz wählt {@link Typbezeichnung}
 */
public record BamTrefferWertZeile(String messageId, short typ, String wert, String bezeichnung) {}
