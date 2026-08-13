package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Typ auf einer Nachricht, roh aus der <b>Zaehlung</b> — Abfrage (a) des Endpunkts.
 *
 * <p><b>{@code gesamt} ist die wahre Zahl</b> und nicht die Zahl der ausgelieferten Werte. Sie
 * entsteht aus einem {@code GROUP BY} ueber den Index und ist deshalb unabhaengig davon, wie viele
 * Werte die Antwort zeigt. Genau darin liegt die Zweiteilung: Ohne sie muesste die Deckelung
 * entweder alles laden oder eine Restangabe erfinden.
 *
 * @param typ {@code MessageBAM.MessageBAMType}
 * @param bezeichnung {@code MessageBAMType.MessageBAMTypeDescription}. <b>Nullbar</b> — fehlt die
 *     Zeile in {@code MessageBAMType}, gibt es keine Beschriftung, und der Service setzt die
 *     Typnummer ein. Sichtbar unfertig, dieselbe Regel wie bei einem kuratierten Eigenschaftsnamen
 *     ohne Uebersetzung
 * @param gesamt wie viele Werte dieses Typs auf der Nachricht stehen
 * @param sortIndex {@code MessageBAMMandant.MessageBAMTypeSortIndex} des <b>aktiven</b> Mandanten.
 *     <b>{@code null} heisst „fuer diesen Mandanten nicht konfiguriert"</b> — und das ist eine
 *     Aussage ueber die <i>Sichtbarkeitskonfiguration</i>, nicht ueber den Bestand (M40).
 *     <p>Das Feld verlaesst das Backend nicht. Es ordnet die Gruppen ({@link BamService#ORDNUNG}),
 *     und es <b>markiert nichts</b>: Ob ein Typ konfiguriert ist, ist eine interne Angabe und geht
 *     den Nutzer nichts an
 */
public record BamTypZeile(short typ, String bezeichnung, int gesamt, Short sortIndex) {}
