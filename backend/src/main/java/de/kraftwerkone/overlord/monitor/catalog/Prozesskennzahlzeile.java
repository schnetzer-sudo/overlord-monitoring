package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Eine Zeile der <b>Kennzahlenabfrage</b>: je Prozess und <b>Rohstatus</b> eine Summe im gewaehlten
 * Zeitfenster.
 *
 * <p><b>Rohstatus und nicht Einordnung</b> — Entscheidung E-g aus {@code docs/rollup.md} §2: Die
 * Einordnung ist eine Regel, die sich aendern kann, der Rohwert ist eine Tatsache. Sie entsteht
 * beim Lesen ueber {@code common/MessageStatusClassifier.einordnung(String)}, <b>gerufen und nicht
 * nachgebaut</b>.
 *
 * <p>Daraus folgt unmittelbar, dass <i>Nachrichten</i> und <i>Fehler</i> aus <b>einem</b>
 * Lesevorgang entstehen und nicht aus zweien: Was Fehler ist, entscheidet sich am selben Rohwert,
 * den die Zeile ohnehin traegt.
 *
 * @param processId die {@code ProcessID} — Schluessel gegen {@link Prozessgeruestzeile}
 * @param messageStatus der Rohwert aus dem Rollup, etwa {@code FINISHED} oder {@code ERROR_TIMEOUT}
 * @param anzahl die Summe ueber alle Eimer des Fensters
 */
public record Prozesskennzahlzeile(String processId, String messageStatus, long anzahl) {}
