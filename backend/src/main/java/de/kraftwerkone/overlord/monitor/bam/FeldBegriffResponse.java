package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Feldbegriff, so wie die Suche ihn verstanden hat — <b>das Zitat der Frage</b>, wie {@link
 * BamBegriffResponse} für die Belegnummern.
 *
 * <p><b>Keine Varianten.</b> Anders als ein BAM-Wert wird ein Feldwert nicht normalisiert: Es gibt
 * keine Sollänge und keine Kuratierung dafür, und gesucht wird genau der eingegebene Wert.
 *
 * @param name der Feldname, wie eingegeben
 * @param wert der Wert, wie eingegeben, an den Rändern beschnitten
 * @param spalte ob der Name als <b>Spalte</b> gesucht wurde (Typ 0) — dieselbe Angabe wie im
 *     Angebot ({@code SuchfeldFeldResponse}), damit die Oberfläche weiß, was der Treffer bedeutet
 */
public record FeldBegriffResponse(String name, String wert, boolean spalte) {}
