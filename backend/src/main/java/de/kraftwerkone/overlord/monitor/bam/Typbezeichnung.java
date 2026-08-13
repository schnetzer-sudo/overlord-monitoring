package de.kraftwerkone.overlord.monitor.bam;

/**
 * Die Beschriftung eines BAM-Typs — {@code MessageBAMType.MessageBAMTypeDescription}, unverändert.
 *
 * <p><b>Fehlt sie, erscheint die Typnummer</b>, sichtbar unfertig. Dieselbe Regel wie bei einem
 * kuratierten Eigenschaftsnamen ohne Übersetzung ({@code docs/nachrichtendetail.md} §10.3): Ein
 * neuer Typ aus dem Altsystem fällt beim ersten Blick auf, statt lautlos als leere Zeile zu
 * erscheinen.
 *
 * <p>Ein <b>leerer</b> Text wird wie ein fehlender behandelt. Die Spalte lässt ihn zu, und eine
 * Angabe ohne jede Überschrift wäre die eine Darstellung, die schlechter ist als die Typnummer.
 *
 * <p><b>Warum die Regel eine eigene Klasse hat.</b> Sie wird ab Teil 2b an zwei Stellen gebraucht —
 * vom Belegdaten-Block ({@link BamService}) und von der Trefferliste der Suche ({@link
 * BamSucheService}). Ein zweiter Nachbau wäre die Drift, bei der zwei Antworten desselben Endpunkts
 * denselben fehlenden Typ verschieden beschriften. Sie bleibt in {@code bam} und wandert
 * ausdrücklich <b>nicht</b> nach {@code common}: Dort gehört hin, was ein <i>zweites Fachpaket</i>
 * braucht, und das ist hier nicht der Fall.
 */
public final class Typbezeichnung {

  private Typbezeichnung() {}

  /**
   * @param typ die Typnummer — sie ist der Ersatz, wenn keine Beschreibung dasteht
   * @param beschreibung der Rohwert aus dem Altsystem, {@code null} oder leer erlaubt
   */
  public static String fuer(short typ, String beschreibung) {
    if (beschreibung == null || beschreibung.isBlank()) {
      return Short.toString(typ);
    }
    return beschreibung;
  }
}
