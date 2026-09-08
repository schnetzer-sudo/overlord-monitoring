package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Eintrag der <b>BAM-Gruppe</b> des Suchangebots — dieselbe Zeile wie in {@code GET
 * /api/bam/typen}, mit der Quelle davor.
 *
 * <p><b>Die Quelle steht je Eintrag</b>, obwohl die Antwort bereits zwei Gruppen trägt: Die
 * Oberfläche führt beide Quellen in <i>einem</i> Feld zusammen (E‑99), und ein Eintrag, der seine
 * Herkunft selbst nennt, bleibt auch in einer gemischten Liste eindeutig.
 *
 * @param quelle immer {@link SuchfelderResponse#QUELLE_BAM}
 * @param typ die Typnummer — der erste Teil des Suchparameters {@code begriff=<typ>:<wert>}
 * @param bezeichnung {@code MessageBAMType.MessageBAMTypeDescription}, unverändert; nie {@code
 *     null} ({@link Typbezeichnung})
 * @param sortIndex die Ordnung des Altsystems
 */
public record SuchfeldBamResponse(String quelle, short typ, String bezeichnung, short sortIndex) {}
