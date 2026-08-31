package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Ein Eimer der Verteilung, so wie er aus der Abfrage kommt.
 *
 * @param schluessel der kuratierte Partner beziehungsweise die kuratierte Richtung — <b>{@code
 *     null} heisst „nicht zugeordnet"</b>. Die drei Faelle, die darin zusammenfallen, sind
 *     Entscheidung E-i: keine Katalogzeile, Katalogzeile nicht {@code GEPFLEGT}, oder gepflegt mit
 *     leerem Wert. <b>Der Text „nicht zugeordnet" entsteht nicht hier</b> — die Oberflaeche
 *     beschriftet, das Backend stellt fest (Regel Q4)
 * @param anzahl {@code SUM(anzahl)} ueber das ganze Fenster; die Eimerbreite des Paares spielt fuer
 *     die Verteilung keine Rolle, die Fensterbreite schon
 */
record Verteilungssumme(String schluessel, long anzahl) {}
