package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.LocalDateTime;

/**
 * Der letzte abgeschlossene, fehlerfreie Rollup-Lauf, so wie er in {@code rollup_lauf} steht.
 *
 * @param art {@code DELTA} oder {@code VOLL} — als <b>Rohwert</b> aus der Spalte und nicht als
 *     Aufzaehlung. {@code LaufArt} liegt im Fachpaket {@code rollup}, und Fachpakete kennen
 *     einander nicht; einen Wert dorthin zu uebersetzen, waere ausserdem eine Deutung, die die
 *     Spalte selbst nicht hergibt (Regel Q4)
 * @param beendetAm <b>UTC</b> und nicht Wanduhrzeit des Quellservers. Die beiden Zeitstempel in
 *     {@code rollup_lauf} kommen aus {@code systemClock} und heissen deshalb bewusst anders als
 *     {@code stunde}, {@code tag} und {@code monat} ({@code docs/rollup.md} §4)
 */
record Standzeile(String art, LocalDateTime beendetAm) {}
