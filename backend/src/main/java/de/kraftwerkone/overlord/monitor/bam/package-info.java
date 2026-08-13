/**
 * Die fachlichen Belegschluessel aus {@code MessageBAM} — der Einstiegspunkt des Fachanwenders.
 *
 * <p><b>Zwei Zugriffsrichtungen, und sie sind absichtlich getrennt gebaut.</b>
 *
 * <ul>
 *   <li><b>Ueber die {@code MessageID}</b> — die Belegdaten <i>einer</i> Nachricht, Schritt 7 Teil
 *       1. Das ist der Pfad, den dieses Projekt mehrfach gemessen hat: {@code ref} auf {@code
 *       PRIMARY} mit {@code Using index} (M11, M26‑1b, M28‑2, M39‑1). Gebaut in {@link
 *       de.kraftwerkone.overlord.monitor.bam.BamRepository}, beschrieben in {@code
 *       docs/bam-werte.md}.
 *   <li><b>Ueber den Wert</b> — die Suche, Schritt 7 Teil 2, mit hartem Ergebnislimit und
 *       Mindestlaenge des Suchbegriffs. <b>Noch nicht gebaut.</b>
 * </ul>
 *
 * <p>Die Trennung ist der Grund fuer den Schnitt in zwei Teile: Der erste Pfad ist gemessen, der
 * zweite steigt umgekehrt ein und traegt das Risiko (M33: Maximum 234.159 Treffer auf einem Wert).
 * Beide in einen Schritt zu legen hiesse, das sichere Stueck an das riskante zu binden.
 */
package de.kraftwerkone.overlord.monitor.bam;
