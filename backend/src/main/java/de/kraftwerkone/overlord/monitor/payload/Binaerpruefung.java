package de.kraftwerkone.overlord.monitor.payload;

/**
 * Ob eine Datei als Text angezeigt werden kann.
 *
 * <p><b>Der Fall ist gemessen, nicht befuerchtet:</b> 18,2 % der Nutzdateien sind binaer, 0 % der
 * Protokolle (M61). Das Altsystem zeigt hier Zeichenmuell, der wie ein Fehler aussieht. Hier
 * bekommt der Fall einen eigenen, benannten Zustand ({@link Artefaktzustand#BINAERDATEI}) und der
 * Download bleibt moeglich.
 *
 * <h2>Zwei Merkmale, das erste allein entscheidet meistens</h2>
 *
 * <ol>
 *   <li><b>Ein Nullbyte</b> genuegt. In keiner der gemessenen Textdateien kommt eines vor (M61,
 *       Nullbytes 0,00 % ueber alle nicht-binaeren Dateien); in einer Textdatei hat es nichts zu
 *       suchen.
 *   <li><b>Anteil druckbarer Zeichen unter der Schwelle.</b> Faengt die Faelle ohne Nullbyte ab.
 * </ol>
 *
 * <h2>Warum die Pruefung auf den Bytes laeuft und nicht auf dem dekodierten Text</h2>
 *
 * <p>Weil {@code ISO-8859-1} <b>jedes</b> Byte auf ein Zeichen abbildet und niemals scheitert. Ein
 * dekodierter Text traegt deshalb keine Information darueber, ob die Bytes Text waren — die
 * Entscheidung muss vor der Dekodierung fallen. Genau daran haengt der Unterschied zum Altsystem,
 * das erst dekodiert und dann anzeigt.
 *
 * <h2>Was als druckbar gilt</h2>
 *
 * <p>Die Steuerzeichen {@code TAB}, {@code LF} und {@code CR} zaehlen mit — sie stehen in jeder
 * Protokolldatei, und die Zeilenenden sind uneinheitlich (M61: LF 95, CRLF 38, gemischt 37, keines
 * 36 von 206). Bytes ab {@code 0xA0} zaehlen ebenfalls als druckbar: In {@code ISO-8859-1} sind das
 * Umlaute und Akzente, und <b>8 von 8 Protokollen und 9 von 16 Nutzdateien sind kein gueltiges
 * UTF-8</b> (M61) — wer diese Bytes als Muell zaehlte, erklaerte die Mehrheit der echten Protokolle
 * zu Binaerdateien. Der Bereich {@code 0x80}–{@code 0x9F} zaehlt <b>nicht</b> mit: Dort liegen in
 * {@code ISO-8859-1} nicht belegte Steuerzeichen.
 */
public final class Binaerpruefung {

  private Binaerpruefung() {}

  /**
   * Ab welchem Anteil druckbarer Zeichen eine Datei als Text gilt.
   *
   * <p>Bewusst hoch angesetzt. Die gemessenen Textdateien liegen bei <b>100 %</b> druckbaren
   * Zeichen (M71, Befund 3; M61 fuer die uebrigen); eine Datei, die zu jedem zwanzigsten Byte etwas
   * Unlesbares traegt, ist keine, die man jemandem als Text hinlegt. Zwischen 100 % und 95 % ist im
   * gemessenen Bestand nichts — die Schwelle trennt also nichts Echtes auseinander.
   */
  static final double SCHWELLE_DRUCKBAR = 0.95;

  /**
   * Wie viele Bytes vom Anfang geprueft werden.
   *
   * <p>Das groesste gemessene Artefakt hat 609.995 Byte (M60); 64 KiB sind ein Zehntel davon und
   * bei den ueblichen Groessen die ganze Datei. Ein Praefix genuegt, weil das Merkmal, um das es
   * geht — ist das ueberhaupt Text — sich nicht in der Mitte einer Datei aendert. Die Grenze haelt
   * die Pruefung ausserdem billig genug, dass sie bei jedem Abruf laufen kann.
   */
  static final int PRUEFLAENGE = 64 * 1024;

  /** Ob die Bytes als Binaerdatei einzustufen sind. */
  public static boolean istBinaer(byte[] daten) {
    if (daten == null || daten.length == 0) {
      // Eine leere Datei ist keine Binaerdatei. Sie ist leer — und das kommt vor: das kleinste
      // gemessene Artefakt hat 2 Byte (M60).
      return false;
    }
    int laenge = Math.min(daten.length, PRUEFLAENGE);
    int druckbar = 0;
    for (int i = 0; i < laenge; i++) {
      int wert = daten[i] & 0xFF;
      if (wert == 0x00) {
        return true;
      }
      if (istDruckbar(wert)) {
        druckbar++;
      }
    }
    return (double) druckbar / laenge < SCHWELLE_DRUCKBAR;
  }

  private static boolean istDruckbar(int wert) {
    if (wert == '\t' || wert == '\n' || wert == '\r') {
      return true;
    }
    // 0x20 bis 0x7E: druckbares ASCII. 0xA0 bis 0xFF: der belegte obere Bereich von ISO-8859-1.
    return (wert >= 0x20 && wert <= 0x7E) || wert >= 0xA0;
  }
}
