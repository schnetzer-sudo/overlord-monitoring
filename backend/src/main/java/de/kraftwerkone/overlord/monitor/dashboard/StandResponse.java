package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Instant;

/**
 * Block 7: <b>wann zuletzt gerechnet wurde, und von welcher Laufart</b>.
 *
 * <p><b>Warum die Laufart dabeisteht.</b> Ein {@code DELTA}-Lauf schreibt die letzten Stunden fort,
 * ein {@code VOLL}-Lauf den ganzen Bestand. Steht dort seit Tagen nur {@code DELTA}, ist der
 * Nachtlauf ausgeblieben — und das sieht man der Uhrzeit allein nicht an.
 *
 * <p><b>Nur abgeschlossene, fehlerfreie Laeufe.</b> Dieselbe Bedingung wie der Wasserstand des
 * Jobs: Ein abgebrochener Lauf hat nichts fortgeschrieben, und ein abgeschlossener mit Fehler ist
 * nicht verlaesslich gerechnet.
 *
 * @param beendetAm der Zeitpunkt in UTC
 * @param art {@code DELTA} oder {@code VOLL}, als Rohwert aus der Spalte
 */
public record StandResponse(Instant beendetAm, String art) {}
