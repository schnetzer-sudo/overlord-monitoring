package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein für den Mandanten konfigurierter BAM-Typ, roh aus {@code MessageBAMMandant}.
 *
 * <p>Das Gegenstück zu {@link BamTypZeile}, und die beiden werden nicht zusammengelegt: Jener trägt
 * die Zählung <i>auf einer Nachricht</i> und einen <b>nullbaren</b> Sortierindex („für diesen
 * Mandanten nicht konfiguriert"). Hier ist die Konfiguration die Quelle selbst — der Sortierindex
 * ist deshalb nie {@code null}, und eine Zeile ohne ihn gibt es nicht.
 *
 * @param typ {@code MessageBAMMandant.MessageBAMType}
 * @param bezeichnung {@code MessageBAMType.MessageBAMTypeDescription}. <b>Nullbar</b> — fehlt die
 *     Zeile in {@code MessageBAMType}, gibt es keine Beschriftung, und {@link Typbezeichnung} setzt
 *     die Typnummer ein
 * @param sortIndex {@code MessageBAMMandant.MessageBAMTypeSortIndex}, {@code SMALLINT NOT NULL}
 */
public record BamTypenZeile(short typ, String bezeichnung, short sortIndex) {}
