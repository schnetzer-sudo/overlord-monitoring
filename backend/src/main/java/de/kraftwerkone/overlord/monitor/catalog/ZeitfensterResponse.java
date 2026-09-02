package de.kraftwerkone.overlord.monitor.catalog;

import java.time.Instant;

/**
 * Die gelesenen Fenstergrenzen, wie sie in der Antwort stehen — <b>UTC, {@code bis}
 * ausschliessend</b>.
 *
 * <p>Beide Grenzen liegen auf einer <b>Eimergrenze</b> des gewaehlten Paares, und die obere ist der
 * Anfang des <i>naechsten</i> Eimers ({@code common/Rollupzeitraum}). Der angebrochene Eimer, in
 * dem {@code jetzt} liegt, gehoert dazu.
 *
 * <p><b>Warum die Antwort sie ueberhaupt nennt:</b> Der Aufrufer schickt einen Code wie {@code 48H}
 * und keine Zeitpunkte — aufgeloest wird gegen die <b>Anwendungsuhr</b> (Regel Z1), und die ist im
 * Profil {@code dev} um den Rueckstand der Testkopie zurueckversetzt. Ohne diese beiden Felder
 * koennte die Oberflaeche nicht beschriften, welchen Zeitraum sie gerade zeigt.
 */
public record ZeitfensterResponse(Instant von, Instant bis) {}
