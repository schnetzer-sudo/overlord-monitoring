package de.kraftwerkone.overlord.monitor.common;

/**
 * Die drei Ebenen des Rollups — <b>Stunde, Tag, Monat</b> — als Name und nicht als Tabelle.
 *
 * <p><i>(seit 07.09.2026, Schritt 10c-4b.)</i> Ein {@link Baumfenster.Segment} muss sagen, auf
 * welcher Ebene es gelesen wird, ohne die generierte jOOQ-Tabelle zu nennen: Die Zuordnung Ebene →
 * {@code message_rollup} / {@code message_rollup_tag} / {@code message_rollup_monat} bleibt im
 * Repository, wo die generierten Typen liegen (offener Punkt 113 in {@code docs/process-view.md},
 * fortgeschrieben und nicht geschlossen). Diese Aufzaehlung ist der Name, an dem beide Seiten sich
 * treffen.
 *
 * <p><b>Kein vierter Wert.</b> Eine vierte Rollup-Ebene loest im Repository an einem vollstaendigen
 * {@code switch} ohne {@code default} einen Compilerfehler aus — genau wie ein viertes Paar in
 * {@link Rollupzeitraum}.
 */
public enum Rollupebene {

  /** Stundeneimer, {@code message_rollup.stunde} ({@code DATETIME}). */
  STUNDE,

  /** Tageseimer, {@code message_rollup_tag.tag} ({@code DATE}). */
  TAG,

  /** Monatseimer, {@code message_rollup_monat.monat} ({@code DATE}, erster Tag des Monats). */
  MONAT
}
