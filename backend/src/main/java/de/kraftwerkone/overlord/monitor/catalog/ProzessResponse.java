package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Ein Prozess des aktiven Mandanten, so wie ihn die Prozessauswahl braucht.
 *
 * <p>Drei Felder und kein viertes. Die Auswahl beantwortet genau eine Frage — „welchen Prozess
 * meinst du" —, und dafuer braucht der Nutzer den Namen und die Gruppe, in der er steht. Zahlen
 * (wie viele Nachrichten, wie viele Fehler) gehoeren nicht hierher: Sie waeren eine Aggregation
 * ueber {@code Message} ohne Zeitfenster und damit genau das, was die Regeln L1 und L2
 * ausschliessen.
 *
 * @param processId die {@code ProcessID} — der Wert, der als {@code prozess} an {@code
 *     /api/nachrichten} zurueckgeht. Fuer den Nutzer ist sie Beiwerk und wird nicht angezeigt.
 * @param processName der Anzeigename. Darf {@code null} sein — die Spalte laesst es zu, und was der
 *     Nutzer anstelle einer fehlenden Zuordnung liest, ist eine Oberflaechenentscheidung und
 *     gehoert in die Sprachdateien, nicht in eine Abfrage (Regel Q4).
 * @param projectName die Gruppe, in der der Prozess steht. Ebenfalls {@code null}-faehig, aus
 *     demselben Grund.
 */
public record ProzessResponse(String processId, String processName, String projectName) {}
