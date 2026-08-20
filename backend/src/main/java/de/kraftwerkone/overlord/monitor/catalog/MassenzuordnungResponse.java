package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Was eine Massenzuordnung betrifft — <b>in beiden Modi dieselbe Auskunft</b>.
 *
 * <p>Die Zahlen stammen in {@link Massenmodus#VORSCHAU} und in {@link Massenmodus#AUSFUEHREN} aus
 * <b>demselben Statement mit derselben Bedingung</b> ({@code
 * ProzessKatalogRepository#findeProjektbestand}). Getrennt gebaut driften die beiden auseinander,
 * und der Nutzer bestaetigt dann eine Zahl, die nicht die ist, die passiert (§5).
 *
 * @param modus welcher Modus gelaufen ist — {@link Massenmodus#VORSCHAU} hat <b>nichts</b>
 *     geschrieben
 * @param projectId das Projekt, ueber das zugeordnet wurde
 * @param feld das eine gesetzte Feld (E11)
 * @param wert der gesetzte Wert. {@code null} ist zulaessig und bedeutet „leeren"
 * @param betroffen wie viele Zeilen betroffen sind bzw. waren
 * @param davonGepflegt wie viele der betroffenen Zeilen bereits {@link Pflegestatus#GEPFLEGT}
 *     waren. <b>Das ist die Zahl, die verloren geht</b> — die Massenzuordnung ueberschreibt sie
 *     bewusst (E12)
 */
public record MassenzuordnungResponse(
    Massenmodus modus,
    String projectId,
    Zuordnungsfeld feld,
    String wert,
    int betroffen,
    int davonGepflegt) {}
