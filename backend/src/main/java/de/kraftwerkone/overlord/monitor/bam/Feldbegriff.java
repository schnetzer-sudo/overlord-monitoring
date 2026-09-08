package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import org.springframework.http.HttpStatus;

/**
 * Ein Suchbegriff der Property-Suche: ein <b>Feldname</b> und der Wert, der unter ihm stehen soll.
 *
 * <p><b>Die Form ist {@code <name>:<wert>}, und der Name ist Pflicht</b> (E‑100). Eine typlose
 * Suche erreicht ausschließlich BAM und nie eine Property — {@code feld=:4711} ist deshalb kein
 * Begriff ohne Feld, sondern {@code 400}. Der Doppelpunkt ist wie beim BAM-Begriff der
 * Pflichttrenner, und geteilt wird am <b>ersten</b>: Alles dahinter ist Wert, einschließlich
 * weiterer Doppelpunkte.
 *
 * <p><b>Ein eigener Parameter, nicht der bestehende — und das ist keine Geschmacksfrage.</b> Ein
 * gemeinsamer Parameter müsste aus der Zeichenkette raten, ob {@code 9018} ein BAM-Typ oder ein
 * Feldname ist; Raten ist nach Regel Q4 ausgeschlossen. E‑99 ist eine Aussage über die
 * <i>Fläche</i>, nicht über die Parameterform: Die Oberfläche führt beide in einem Feld zusammen,
 * der Endpunkt hält sie auseinander. Der BAM-Parameter {@code begriff} bleibt Zeichen für Zeichen
 * unverändert (E‑106).
 *
 * <p><b>Ob ein Feldname einen Doppelpunkt tragen kann, ist keine Frage an den Bestand</b>: Die
 * Namen sind Konfiguration mit vierzehn Zeilen (M161), keiner trägt einen, und {@code
 * Typ0AbbildungDbIT} liest die Tabelle bei jedem Lauf. Ein Wert darf einen tragen — er steht hinter
 * dem ersten Trenner.
 *
 * @param name {@code MessagePropertyName}, an den Rändern beschnitten, mindestens ein Zeichen. Ob
 *     er eine Spalte oder eine Zeile benennt, entscheidet {@link Typ0Feld} — nicht dieser Typ
 * @param wert der gesuchte Wert, an den Rändern beschnitten und mindestens ein Zeichen lang
 */
public record Feldbegriff(String name, String wert) {

  /** Der Pflichttrenner zwischen Name und Wert — derselbe wie bei {@link Suchbegriff}. */
  public static final char TRENNER = Suchbegriff.TRENNER;

  public Feldbegriff {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Ein Feldbegriff ohne Namen gibt es nicht");
    }
    if (wert == null || wert.isEmpty()) {
      throw new IllegalArgumentException("Ein Feldbegriff ohne Wert gibt es nicht");
    }
  }

  /**
   * Liest einen rohen Parameter {@code <name>:<wert>}.
   *
   * <p>Ein leerer Wertteil ergibt {@code null} — dieselbe Regel wie bei {@link
   * Suchbegriff#ausParameter}: {@code feld=Message.GUID:} ist keine Suche nach dem leeren Wert und
   * fällt weg; bleibt am Ende kein Begriff übrig, ist das {@code 400} in {@link BamSuchfilter#aus}.
   *
   * @return der geprüfte Begriff, oder {@code null}, wenn nach dem Beschneiden kein Wert bleibt
   * @throws FachlicheAusnahme {@code 400}, wenn der Trenner fehlt oder der Name leer ist
   */
  public static Feldbegriff ausParameter(String roh) {
    int trenner = roh.indexOf(TRENNER);
    if (trenner < 0) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "feldbegriff-ohne-trenner",
          "Feldbegriff ohne Trenner",
          "Ein Feldbegriff hat die Form feldname:wert.",
          "Feldbegriff ohne Pflichttrenner");
    }
    String name = roh.substring(0, trenner).strip();
    String wert = roh.substring(trenner + 1).strip();
    if (name.isEmpty()) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "feldname-fehlt",
          "Feldname fehlt",
          "Vor dem Doppelpunkt steht der Feldname — ohne Feld wird nur nach Belegnummern gesucht.",
          "Feldbegriff mit leerem Namen (E-100)");
    }
    if (wert.isEmpty()) {
      return null;
    }
    return new Feldbegriff(name, wert);
  }
}
