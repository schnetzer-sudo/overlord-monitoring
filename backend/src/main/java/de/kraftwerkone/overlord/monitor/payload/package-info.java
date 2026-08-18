/**
 * Rohdaten und Protokolle: die Dateien einer Nachricht auflisten, im Browser anzeigen und
 * herunterladen. Erst Mandantenpruefung im Statement, dann Aufloesung der Ablage, dann SOAP-Abruf,
 * Entpacken und — fuer {@code MANDANT} bei Protokollen — Beschnitt. Niemals ein durchgereichter
 * Link, und niemals ein Verweis vom Aufrufer.
 *
 * <p><b>Der Paketname passt nur noch halb.</b> {@code payload} stammt aus Abschnitt 6 der
 * Projektbeschreibung, als das Feature ein reiner Download-Proxy fuer die Nutzdatei sein sollte.
 * Gebaut ist mehr: Protokolle gehoeren dazu, die Anzeige ist der Regelfall und der Proxy ist
 * keiner, weil die Ablage SOAP spricht und ein ZIP liefert. <b>Umbenannt wird trotzdem nicht</b> —
 * ein Paketumbenennen gehoert nicht in denselben Diff wie ein neues Feature, und der Name steht in
 * der Projektbeschreibung, im Implementierungsplan und in {@code PaketstrukturTest}. Der Vorschlag
 * ist als offener Punkt in {@code docs/rohdaten-backend.md} notiert.
 */
package de.kraftwerkone.overlord.monitor.payload;
