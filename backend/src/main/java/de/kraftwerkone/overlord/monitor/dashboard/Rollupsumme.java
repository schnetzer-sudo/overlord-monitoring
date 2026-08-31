package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.LocalDateTime;

/**
 * Eine Zeile des Verlaufs, so wie sie aus der Rolluptabelle kommt: ein Eimer, ein <b>Rohstatus</b>,
 * eine Summe.
 *
 * <p><b>Der Rohwert bleibt roh</b> (Entscheidung E-g): Im Rollup steht {@code FINISHED}, nicht
 * {@code ABGESCHLOSSEN}. Die Einordnung entsteht erst im {@code DashboardService} und
 * ausschliesslich ueber {@code MessageStatusClassifier} — sie wird <b>gerufen, nicht nachgebaut</b>
 * ({@code PROJEKTBESCHREIBUNG.md} §4.1). Waere sie hier schon aufgeloest, gaebe es zwei Stellen, an
 * denen aus einem Statuswort eine Kategorie wird.
 *
 * <p><b>Der Eimer ist immer ein {@code LocalDateTime}</b>, auch wenn die Tages- und die Monatsebene
 * ein {@code DATE} tragen: Der Anfang des Tages beziehungsweise des Monats. Damit hat der Verlauf
 * ueber alle drei Paare dieselbe Form, und die Umrechnung nach UTC steht an einer Stelle.
 *
 * <p>Wanduhrzeit des Quellservers, nicht UTC ({@code docs/datenzugriff.md} §7).
 */
record Rollupsumme(LocalDateTime eimer, String messageStatus, long anzahl) {}
