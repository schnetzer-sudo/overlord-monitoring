package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Feldbegriff, so wie das Statement ihn sieht: Name, Wert — und ob der Name eine <b>Spalte</b>
 * benennt oder eine <b>Zeile</b> in {@code MessageProperty} (E‑101).
 *
 * <p>Die Zuordnung geschieht im Service über {@link Typ0Feld#fuer}: Kennt die Abbildung den Namen,
 * baut das Repository ein Spaltenprädikat auf {@code Message}, {@code Process} oder {@code SOS} und
 * fasst {@code MessageProperty} für diesen Begriff <b>nicht</b> an. Kennt sie ihn nicht, ist es der
 * EAV-Zugriff über Name und Wert — der Sucheinstieg, für den Leistungsregel 4 ihre benannte
 * Ausnahme hat (E‑102).
 *
 * <p><b>Immer exakt.</b> Der Vergleich ist {@code =} in beiden Fällen; das ist die Fassung aus
 * M166, und nur für sie liegen Zahlen vor. Ein Präfixmodus über {@code MessagePropertyValue} ist
 * nicht gebaut und nicht gemessen — {@code modus=praefix} wirkt ausschließlich auf die
 * BAM-Begriffe.
 *
 * @param name der Feldname, wie eingegeben
 * @param wert der Wert, wie eingegeben
 * @param spalte die Spalte, wenn der Name eine benennt; {@code null} für den EAV-Zugriff
 */
public record Feldbedingung(String name, String wert, Typ0Feld spalte) {

  public Feldbedingung {
    if (name == null || name.isEmpty() || wert == null || wert.isEmpty()) {
      throw new IllegalArgumentException("Eine Feldbedingung ohne Name oder Wert gibt es nicht");
    }
  }

  /** Ob dieser Begriff als Spaltenprädikat läuft und {@code MessageProperty} nicht anfasst. */
  public boolean istSpalte() {
    return spalte != null;
  }
}
