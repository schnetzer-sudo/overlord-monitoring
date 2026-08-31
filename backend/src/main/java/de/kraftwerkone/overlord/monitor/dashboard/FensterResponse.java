package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Instant;

/**
 * Das tatsaechlich gelesene Zeitfenster, in UTC.
 *
 * <p><b>Es steht in der Antwort und nicht nur in der Anfrage</b> — dieselbe Bauform wie bei der
 * BAM-Suche. Der Endpunkt waehlt das Paar unter Umstaenden selbst, und die Grenzen liegen auf
 * <b>Eimergrenzen</b> und nicht auf der Sekunde, in der die Anfrage eintraf. Ohne diese Angabe
 * muesste die Oberflaeche beides nachrechnen und laege dabei irgendwann daneben.
 *
 * @param von einschliesslich
 * @param bis <b>ausschliessend</b> — der Anfang des Eimers <i>nach</i> dem letzten gezeigten
 */
public record FensterResponse(Instant von, Instant bis) {}
