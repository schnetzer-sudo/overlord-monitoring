package de.kraftwerkone.overlord.monitor.bam;

import java.util.List;

/**
 * Die Antwort von {@code GET /api/nachrichten/&#123;messageId&#125;/bam} — <b>welcher Beleg ist
 * das</b>.
 *
 * <p>Sie beantwortet die erste Frage des Leitsatzes an einer einzelnen Nachricht: Die Belegnummern
 * sind die Hauptinformation dieser Ansicht und nicht Beiwerk. Was mit der Nachricht <i>passiert</i>
 * ist, steht in der Zeitleiste; was an ihr <i>haengt</i>, im Kettenblock.
 *
 * @param messageId die angefragte Kennung, gespiegelt
 * @param gruppen je Typ eine Gruppe, in der Reihenfolge aus {@link BamService#ORDNUNG} — zuerst die
 *     fuer diesen Mandanten konfigurierten Typen nach ihrem {@code MessageBAMTypeSortIndex}, danach
 *     die uebrigen nach Typnummer. <b>Immer vorhanden, leer statt fehlend</b>: Eine Nachricht ohne
 *     BAM-Wert ist der Normalfall (80,6 Prozent in Fenster B, M41), und das ist eine Aussage und
 *     kein fehlender Wert
 */
public record BamResponse(String messageId, List<BamGruppeResponse> gruppen) {}
