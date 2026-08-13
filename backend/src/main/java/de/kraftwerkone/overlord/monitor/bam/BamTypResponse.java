package de.kraftwerkone.overlord.monitor.bam;

/**
 * Eine Belegart, die der Mandant der Sitzung zur <b>Auswahl</b> bekommt.
 *
 * <p><b>Das ist ein Angebot und keine Spaltenauswahl.</b> Die Unterscheidung ist der Grund, warum
 * es diesen Endpunkt überhaupt gibt: Der Belegdaten-Block folgt der Konfiguration ausdrücklich
 * <i>nicht</i> als Filter, weil sie nichts über den Bestand sagt ({@code docs/bam-werte.md} §6) —
 * hier greift der Nutzer selbst, sieht was er wählt und kann es jederzeit wieder abwählen. Für ein
 * Angebot genügt {@code MessageBAMMandant}; eine zweite Kuratierungstabelle entsteht dafür nicht.
 *
 * @param typ die Typnummer. Sie ist zugleich der erste Teil des Suchparameters {@code <typ>:<wert>}
 *     ({@code docs/bam-suche.md} §1) — deshalb steht sie in der Antwort und nicht nur die
 *     Beschriftung
 * @param bezeichnung {@code MessageBAMType.MessageBAMTypeDescription}, unverändert. <b>Nie {@code
 *     null}</b>: Fehlt die Zeile im Altsystem, steht hier die Typnummer ({@link Typbezeichnung}).
 *     <p><b>Die Endungen {@code _K_SAP}, {@code _L_SAP} und {@code _FORS} bleiben stehen</b> — M45
 *     misst, dass sie <i>unterscheiden</i>: Ohne sie fallen 62 Beschreibungen auf 57
 * @param sortIndex {@code MessageBAMMandant.MessageBAMTypeSortIndex}, die Ordnung des Altsystems.
 *     <p><b>Er verrät hier nichts</b>, anders als im Belegdaten-Block: Dort wäre sein Fehlen die
 *     Auskunft „für diesen Mandanten nicht konfiguriert" und damit eine interne Angabe ({@code
 *     docs/bam-werte.md} §6). In dieser Liste ist <i>jeder</i> Eintrag konfiguriert — sonst stünde
 *     er nicht darin. Er steht dabei, weil die Antwort ihre eigene Ordnung benennen soll, statt sie
 *     nur zu haben
 */
public record BamTypResponse(short typ, String bezeichnung, short sortIndex) {}
