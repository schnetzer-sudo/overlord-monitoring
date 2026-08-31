package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.Pflegestatus;

/**
 * Was die Uebernahme der Partnervorschlaege betrifft — <b>in beiden Modi dieselbe Auskunft</b>
 * (E24).
 *
 * <p>Die Zahlen stammen in {@link Massenmodus#VORSCHAU} und in {@link Massenmodus#AUSFUEHREN} aus
 * <b>derselben Lesung</b> ({@code ProzessKatalogRepository#findeUebernehmbareVorschlaege}), und die
 * Ausfuehrung schreibt genau die Liste, die sie gezaehlt hat. Getrennt gebaut driften die beiden
 * auseinander, und der Nutzer bestaetigt dann eine Zahl, die nicht die ist, die passiert.
 *
 * <p><b>{@code betroffen} ist die Groesse der gelesenen Liste und nicht die vom {@code UPDATE}
 * gemeldete Zeilenzahl.</b> Nur so nennen Vorschau und Ausfuehrung dieselbe Zahl. Weichen die
 * beiden voneinander ab, steht eine {@code WARN}-Zeile im Protokoll — Lesung und Schreiben laufen
 * auf verschiedenen Verbindungen, eine Abweichung ist damit moeglich und waere ein Befund.
 *
 * <p><b>Die Aufschluesselung ist kein Schmuck.</b> Sie ist die Kontrolle, an der sich ein Lauf
 * gegen §3.5 der Festlegung halten laesst: Bei {@code NEXANS} muessen 509 aus Regel A und 0 aus
 * Regel B kommen, bei {@code IBISGUS} 0 aus Regel A und 88 aus Regel B. {@code regelA + regelB =
 * betroffen} ist eine Invariante und in {@code ProzessKatalogDbIT} geprueft.
 *
 * <p><b>Ein {@code davonGepflegt} gibt es hier nicht</b>, anders als bei der Massenzuordnung. Es
 * waere konstruktionsbedingt immer {@code 0}, weil die Bedingung aus E22 nur {@link
 * Pflegestatus#OFFEN}-Zeilen erfasst — und eine Zahl, die nie etwas anderes sagen kann, sagt
 * nichts.
 *
 * @param modus welcher Modus gelaufen ist — {@link Massenmodus#VORSCHAU} hat <b>nichts</b>
 *     geschrieben und keinen {@code audit_log}-Eintrag hinterlassen
 * @param betroffen wie viele Zeilen die Bedingung aus E22 erfuellen: offen <b>und</b> mit einem
 *     Partnervorschlag aus Regel A oder Regel B
 * @param regelA davon mit {@link VorschlagHerkunft#REGEL_A}
 * @param regelB davon mit {@link VorschlagHerkunft#REGEL_B}
 */
public record VorschlagsuebernahmeResponse(
    Massenmodus modus, int betroffen, int regelA, int regelB) {}
