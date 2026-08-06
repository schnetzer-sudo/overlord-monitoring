package de.kraftwerkone.overlord.monitor.common;

/**
 * Eine der hoechstens zwei BAM-Spalten eines Mandanten: die fachliche Kennung, an der ein
 * Sachbearbeiter seinen Beleg wiedererkennt (Lieferschein-Nr., Bestellnummer, Transport-Nummer).
 *
 * <p>Welche Typen das sind, entscheidet {@link BamSpaltenRegel}. Die Beschreibung stammt aus {@code
 * GlassfishDB.MessageBAMType} und wird mitgeliefert, damit der Aufrufer nichts nachschlagen muss
 * (Richtlinie §5.1).
 *
 * @param position 1 oder 2 — die Reihenfolge in der Liste
 * @param typ der {@code MessageBAMType}
 * @param beschreibung der lesbare Name des Typs
 */
public record BamSpalte(int position, short typ, String beschreibung) {

  /**
   * Baut eine Spalte und faengt die fehlende Beschreibung ab.
   *
   * <p>Ein kuratierter Typ muss in {@code MessageBAMType} nicht vorkommen — die Kuratierung traegt
   * bewusst keinen Fremdschluessel ueber die Schemagrenze ({@code docs/datenzugriff.md} §5). Fehlt
   * der Name, steht dort die Typnummer und nicht ein geratener Klartext (Regel Q4): „BAM-Typ 9006"
   * ist eine technische Bezeichnung, keine Behauptung ueber die Bedeutung.
   */
  public static BamSpalte mitBeschreibung(int position, short typ, String beschreibung) {
    String name =
        beschreibung == null || beschreibung.isBlank() ? "BAM-Typ " + typ : beschreibung.trim();
    return new BamSpalte(position, typ, name);
  }
}
