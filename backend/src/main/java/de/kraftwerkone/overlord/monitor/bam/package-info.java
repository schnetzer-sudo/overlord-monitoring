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
 *   <li><b>Ueber den Wert</b> — die Suche, Schritt 7 Teil 2b bis 4, mit hartem Ergebnislimit und
 *       ohne Mindestlaenge (begruendet in {@code docs/bam-suche.md} §1). Gebaut in {@link
 *       de.kraftwerkone.overlord.monitor.bam.BamSucheRepository}.
 * </ul>
 *
 * <p>Die Trennung ist der Grund fuer den Schnitt in zwei Teile: Der erste Pfad ist gemessen, der
 * zweite steigt umgekehrt ein und traegt das Risiko (M33: Maximum 234.159 Treffer auf einem Wert).
 * Beide in einen Schritt zu legen hiesse, das sichere Stueck an das riskante zu binden.
 *
 * <p><b>Seit dem 08.09.2026 traegt dasselbe Paket auch die Property-Suche</b> ({@code
 * docs/property-suche.md}): eine Suchflaeche, zwei Quellen (E-99). Der Suchendpunkt nimmt neben
 * {@code begriff} den Parameter {@code feld} entgegen, das Angebot {@code GET /api/bam/suchfelder}
 * liefert beide Quellen in zwei Gruppen. Sie liegt hier und nicht in einem eigenen Fachpaket, weil
 * sie denselben Endpunkt, dasselbe Statement und dieselbe Antwort teilt — ein Nachbarpaket duerfte
 * davon nichts importieren, und alles nach {@code common} zu heben hiesse, die BAM-Suche zu
 * zerlegen, um sie an derselben Stelle wieder zusammenzusetzen.
 */
package de.kraftwerkone.overlord.monitor.bam;
