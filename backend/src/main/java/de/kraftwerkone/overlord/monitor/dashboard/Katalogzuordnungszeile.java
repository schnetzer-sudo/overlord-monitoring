package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Eine Zeile der Katalog-Nachlesung fuer den Live-Rest (E-191): zu einem Prozess der Schluessel je
 * Sicht nach Entscheidung E-i — {@code null}, wo der Prozess <i>nicht zugeordnet</i> ist.
 *
 * <p>Ein Prozess <b>ohne</b> Katalogzeile liefert gar keine Zeile; der Dienst behandelt ihn wie
 * eine Zeile mit zwei {@code null} — dieselbe Wirkung wie der {@code LEFT JOIN} im
 * Verteilungsstatement.
 */
record Katalogzuordnungszeile(String processId, String partner, String richtung) {}
