package de.kraftwerkone.overlord.monitor.catalog;

import java.util.List;

/**
 * Die oberste Ebene des Baums: ein <b>kuratierter Partner</b>.
 *
 * <p><b>„Nicht zugeordnet" ist eine eigene Gruppe auf dieser Ebene und wird nicht auf die Partner
 * verteilt</b> (Regel Q4). Sie steht am Ende — sie ist keine Rangposition, sondern eine Aussage
 * ueber den Katalog.
 *
 * @param partner der kuratierte Rohwert, oder {@code null} fuer <b>nicht zugeordnet</b>. Was der
 *     Nutzer anstelle des fehlenden Werts liest, gehoert in die Sprachdateien und nicht in eine
 *     Abfrage
 * @param anzahlProzesse wie viele Blaetter unter diesem Knoten haengen, ueber alle Richtungen
 * @param nachrichten die Summe der Blaetter
 * @param fehler die Summe der Blaetter
 * @param richtungen die Richtungsknoten — nur die, die vorkommen
 */
public record PartnerknotenResponse(
    String partner,
    int anzahlProzesse,
    long nachrichten,
    long fehler,
    List<RichtungsknotenResponse> richtungen) {}
