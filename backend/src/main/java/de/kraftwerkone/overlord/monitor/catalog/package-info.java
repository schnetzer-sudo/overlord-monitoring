/**
 * Der Prozess-Katalog: kuratierte Stammdaten in {@code process_catalog} — <b>Partner und
 * Richtung</b> werden gepflegt, nicht aus Namen geraten (Regel Q4).
 *
 * <p>Dazu die Prozessauswahl aus Schritt 4, die dieselben Stammdaten von der anderen Seite liest.
 *
 * <p><i>Berichtigt am 20.08.2026:</i> Hier standen vier kuratierte Felder — „Partner, Standort,
 * Richtung und Belegart" — und eine zweite Tabelle {@code partner}. Beides ist entfallen: Standort
 * und Belegart liest im MVP nichts (E1), und die Partner-Auswahlliste wird ueber {@code SELECT
 * DISTINCT} aus den Katalogzeilen des aktiven Mandanten abgeleitet (E2). Begruendung in {@code
 * docs/prozess-katalog.md}.
 */
package de.kraftwerkone.overlord.monitor.catalog;
