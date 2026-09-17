package de.kraftwerkone.overlord.monitor.payload;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * <b>Die eine Stelle, die die Bytes eines ZIP-Eintrags einstuft:</b> Text — und mit welcher
 * Kodierung —, Binaerdatei, oder EBCDIC-Muster. Anzeige und Download rufen sie; keiner baut sie
 * nach.
 *
 * <p>Sie ist am 17.09.2026 aus {@code Binaerpruefung} hervorgegangen. Die hat auf den rohen Bytes
 * geprueft, weil {@code ISO-8859-1} jedes Byte auf ein Zeichen abbildet und ein dekodierter Text
 * deshalb nichts mehr darueber sagt, ob die Bytes Text waren. Das gilt weiter — nur ist die
 * Dekodierung jetzt keine feste mehr, und deshalb faellt die Kodierung <b>hier</b> und nicht danach
 * im Service.
 *
 * <h2>Die Reihenfolge, und sie ist nicht verhandelbar</h2>
 *
 * <ol>
 *   <li><b>Ein Nullbyte</b> → {@link Artefaktzustand#BINAERDATEI}. Wie bisher; in keiner der
 *       gemessenen Textdateien kommt eines vor (M61). Das gilt auch fuer eine EBCDIC-Datei mit
 *       Nullbytes — sie bleibt Binaerdatei, siehe {@code docs/rohdaten.md} §13.
 *   <li><b>Kein Byte ueber {@code 0x7F}</b> → {@link Kodierung#ASCII}. Die Mehrheit: 162 von 206
 *       Dateien (M61, Befund 3). Sie entscheidet nichts und muss deshalb zuerst kommen — ASCII ist
 *       zugleich gueltiges UTF-8 und gueltiges ISO-8859-1, und ein Teil davon laege auch im
 *       EBCDIC-Vorrat.
 *   <li><b>Streng gueltiges UTF-8</b> (Decoder mit {@code REPORT}) → {@link Kodierung#UTF_8}. Ein
 *       BOM {@code EF BB BF} am Anfang faellt aus dem angezeigten Text; der Download bleibt
 *       byteweise unveraendert.
 *   <li><b>EBCDIC-Muster</b> → {@link Artefaktzustand#EBCDIC_DATEI}, kein Text. Siehe unten.
 *   <li><b>Alles andere</b> → {@link Kodierung#ISO_8859_1}, dekodiert wie bisher.
 *   <li><b>Anteil druckbarer Zeichen</b> fuer 2, 3 und 5: unter {@value #SCHWELLE_DRUCKBAR_PROZENT}
 *       % → {@link Artefaktzustand#BINAERDATEI}. Gezaehlt wird ueber die <b>dekodierten Zeichen</b>
 *       mit derselben Zeichenklasse wie bisher. Fuer ASCII und ISO-8859-1 ist das zeichengleich mit
 *       der alten Zaehlung auf den Bytes; bei UTF-8 zaehlen Folgebytes nicht mehr einzeln als
 *       Steuerzeichen.
 * </ol>
 *
 * <p>Der Beschnitt laeuft <b>danach</b>, auf dem Text aus 2, 3 oder 5 — der Zweck der alten
 * Reihenfolge bleibt: kein Beschnitt auf Binaerbytes, keine Markensuche in Zeichenmuell.
 *
 * <h2>Das EBCDIC-Muster</h2>
 *
 * <p>EBCDIC-Text liegt grossteils auf Bytes, die die Zeichenklasse als druckbar zaehlt — das
 * Leerzeichen auf {@code 0x40}, Grossbuchstaben und Ziffern ab {@code 0xC1}. Eine Datei in
 * Grossschrift erschien deshalb bis zum 17.09.2026 als Text, und aus „VDA 4905" wurde „åÄÁ@ôùðõ".
 * Die Kleinbuchstaben a bis r liegen auf {@code 0x81}–{@code 0x99} und zaehlten als nicht druckbar.
 * Beides ist aus der Regel abgeleitet, nicht gemessen.
 *
 * <p>Erkannt wird ueber den <b>invarianten</b> Zeichenvorrat, der in IBM 037, 273, 500 und 1141
 * gleich liegt: Leerzeichen, Ziffern, Gross- und Kleinbuchstaben, neunzehn Satzzeichen und die drei
 * Zeilenenden {@code 0x0D}, {@code 0x15}, {@code 0x25}. Liegen mindestens {@value
 * #SCHWELLE_EBCDIC_PROZENT} % aller Bytes darin, traegt die Datei das Muster. <b>Dekodiert wird
 * nicht</b> — welche Codepage es waere, sagen die Bytes nicht, und die Oberflaeche behauptet
 * deshalb auch nicht, dass es sicher EBCDIC ist.
 *
 * <p><b>Bekannte Grenze, hingenommen:</b> Die Reihenfolge prueft UTF-8 vor EBCDIC. Ein EBCDIC-Text,
 * dessen Bytes zufaellig gueltiges UTF-8 ergeben — Grossbuchstabe und Kleinbuchstabe im Wechsel
 * sind Fuehrungs- und Folgebyte —, wuerde als UTF-8 gelesen; die entstehenden C1-Steuerzeichen
 * zaehlen aber nicht als druckbar, und die Datei endet dann als Binaerdatei, nicht als
 * Zeichenmuell. Gemessen ist der Fall nicht.
 *
 * <h2>Was als druckbar gilt</h2>
 *
 * <p>Unveraendert: {@code TAB}, {@code LF} und {@code CR}, {@code 0x20}–{@code 0x7E} und alles ab
 * {@code 0xA0}. Der Bereich {@code 0x80}–{@code 0x9F} zaehlt <b>nicht</b> — in ISO-8859-1 und in
 * Unicode liegen dort unbelegte Steuerzeichen. Bytes ab {@code 0xA0} zaehlen, weil dort in
 * ISO-8859-1 Umlaute und Akzente liegen und 8 von 8 Protokollen kein gueltiges UTF-8 sind (M61):
 * wer sie als Muell zaehlte, erklaerte die Mehrheit der echten Protokolle zu Binaerdateien.
 *
 * <h2>Die ganze Datei, nicht ein Praefix</h2>
 *
 * <p>{@code Binaerpruefung} sah sich die ersten 64 KiB an. Hier laeuft die Einstufung ueber
 * <b>alle</b> Bytes: Der UTF-8-Decoder muss die ganze Datei ohnehin lesen, um den Text zu liefern,
 * und eine Folge, die an einer Praefixgrenze zerschnitten wuerde, saehe faelschlich ungueltig aus.
 * Die Obergrenze liegt bei 8 MiB ({@code Ablagegrenzen}), das groesste gemessene Artefakt bei
 * 609.995 Byte (M60) — ein linearer Durchlauf darueber ist billig genug fuer jeden Abruf.
 */
public final class Inhaltseinstufung {

  private Inhaltseinstufung() {}

  /**
   * Ab welchem Anteil druckbarer Zeichen eine Datei als Text gilt — in Prozent, damit die Grenze
   * ganzzahlig und ohne Gleitkommarundung prueft.
   *
   * <p>Bewusst hoch angesetzt und <b>unveraendert seit Schritt 8</b>. Die gemessenen Textdateien
   * liegen bei <b>100 %</b> druckbaren Zeichen (M71, Befund 3; M61 fuer die uebrigen); eine Datei,
   * die zu jedem zwanzigsten Byte etwas Unlesbares traegt, ist keine, die man jemandem als Text
   * hinlegt. Zwischen 100 % und 95 % ist im gemessenen Bestand nichts — die Schwelle trennt also
   * nichts Echtes auseinander.
   */
  static final int SCHWELLE_DRUCKBAR_PROZENT = 95;

  /**
   * Ab welchem Anteil von Bytes im invarianten EBCDIC-Vorrat eine Datei das Muster traegt.
   *
   * <p><b>Gesetzt, nicht gemessen.</b> Die Begruendung ist eine Ableitung aus dem Vorrat selbst:
   * Text auf ASCII-Basis liegt ueberwiegend ausserhalb — die haeufigen Kleinbuchstaben {@code
   * 0x62}–{@code 0x6A} und {@code 0x70}–{@code 0x79} gehoeren nicht dazu, ebenso wenig die
   * Grossbuchstaben ausser K bis N und P —, und gleichverteilte Binaerdaten treffen den Vorrat von
   * 85 der 256 Byte-Werte bei rund einem Drittel. Ob im Bestand ueberhaupt EBCDIC vorkommt, ist
   * nicht gemessen ({@code docs/rohdaten.md} §13).
   */
  static final int SCHWELLE_EBCDIC_PROZENT = 90;

  /** Die 85 Byte-Werte des invarianten EBCDIC-Vorrats, als Nachschlagetabelle ueber 256 Werte. */
  private static final boolean[] EBCDIC_VORRAT = ebcdicVorrat();

  private static boolean[] ebcdicVorrat() {
    boolean[] vorrat = new boolean[256];
    vorrat[0x40] = true; // Leerzeichen
    bereich(vorrat, 0xF0, 0xF9); // Ziffern
    bereich(vorrat, 0xC1, 0xC9); // A–I
    bereich(vorrat, 0xD1, 0xD9); // J–R
    bereich(vorrat, 0xE2, 0xE9); // S–Z
    bereich(vorrat, 0x81, 0x89); // a–i
    bereich(vorrat, 0x91, 0x99); // j–r
    bereich(vorrat, 0xA2, 0xA9); // s–z
    bereich(vorrat, 0x4B, 0x4E); // . < ( +
    vorrat[0x50] = true; // &
    bereich(vorrat, 0x5C, 0x5E); // * ) ;
    vorrat[0x60] = true; // -
    vorrat[0x61] = true; // /
    bereich(vorrat, 0x6B, 0x6F); // , % _ > ?
    vorrat[0x7A] = true; // :
    bereich(vorrat, 0x7D, 0x7F); // ' = "
    vorrat[0x0D] = true; // CR
    vorrat[0x15] = true; // NL
    vorrat[0x25] = true; // LF
    return vorrat;
  }

  private static void bereich(boolean[] vorrat, int von, int bis) {
    for (int wert = von; wert <= bis; wert++) {
      vorrat[wert] = true;
    }
  }

  /**
   * Das Ergebnis der Einstufung.
   *
   * @param zustand {@link Artefaktzustand#ANZEIGBAR}, {@link Artefaktzustand#BINAERDATEI} oder
   *     {@link Artefaktzustand#EBCDIC_DATEI}
   * @param kodierung womit gelesen wurde. {@code null}, wenn kein Text entstanden ist
   * @param text der dekodierte, noch unbeschnittene und ungekappte Text — ohne BOM. Leer, wenn kein
   *     Text entstanden ist
   */
  public record Ergebnis(Artefaktzustand zustand, Kodierung kodierung, String text) {

    /** Ob die Bytes als Text lesbar sind. Nur dann traegt das Ergebnis Kodierung und Text. */
    public boolean istText() {
      return zustand == Artefaktzustand.ANZEIGBAR;
    }

    static Ergebnis ohneText(Artefaktzustand zustand) {
      return new Ergebnis(zustand, null, "");
    }

    static Ergebnis text(Kodierung kodierung, String text) {
      return new Ergebnis(Artefaktzustand.ANZEIGBAR, kodierung, text);
    }

    /** Bewusst ueberschrieben: kein Zeichen des Inhalts geraet ins Protokoll. */
    @Override
    public String toString() {
      return "Einstufung[" + zustand + ", " + kodierung + ", " + text.length() + " Zeichen]";
    }
  }

  /** Stuft die Bytes eines ZIP-Eintrags ein, in der Reihenfolge aus der Klassenbeschreibung. */
  public static Ergebnis stufeEin(byte[] daten) {
    if (daten == null || daten.length == 0) {
      // Eine leere Datei ist keine Binaerdatei. Sie ist leer — und das kommt vor: das kleinste
      // gemessene Artefakt hat 2 Byte (M60). Kein Byte liegt ueber 0x7F, also ASCII.
      return Ergebnis.text(Kodierung.ASCII, "");
    }

    boolean nurAscii = true;
    long imEbcdicVorrat = 0;
    for (byte b : daten) {
      int wert = b & 0xFF;
      if (wert == 0x00) {
        return Ergebnis.ohneText(Artefaktzustand.BINAERDATEI);
      }
      if (wert > 0x7F) {
        nurAscii = false;
      }
      if (EBCDIC_VORRAT[wert]) {
        imEbcdicVorrat++;
      }
    }

    if (nurAscii) {
      return mitDruckbarkeit(Kodierung.ASCII, new String(daten, StandardCharsets.US_ASCII));
    }

    String utf8 = strengAlsUtf8(daten);
    if (utf8 != null) {
      return mitDruckbarkeit(Kodierung.UTF_8, ohneBom(utf8));
    }

    if (imEbcdicVorrat * 100 >= (long) SCHWELLE_EBCDIC_PROZENT * daten.length) {
      return Ergebnis.ohneText(Artefaktzustand.EBCDIC_DATEI);
    }

    return mitDruckbarkeit(Kodierung.ISO_8859_1, new String(daten, StandardCharsets.ISO_8859_1));
  }

  /** Der Text, wenn die Bytes streng gueltiges UTF-8 sind — sonst {@code null}. */
  private static String strengAlsUtf8(byte[] daten) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(daten))
          .toString();
    } catch (CharacterCodingException ungueltig) {
      return null;
    }
  }

  /** Der Text ohne ein fuehrendes {@code U+FEFF} — das ist der dekodierte BOM. */
  private static String ohneBom(String text) {
    return !text.isEmpty() && text.charAt(0) == '\uFEFF' ? text.substring(1) : text;
  }

  /** Schritt 6: unter der Schwelle druckbarer Zeichen ist es keine Textdatei. */
  private static Ergebnis mitDruckbarkeit(Kodierung kodierung, String text) {
    int laenge = text.length();
    long druckbar = 0;
    for (int i = 0; i < laenge; i++) {
      if (istDruckbar(text.charAt(i))) {
        druckbar++;
      }
    }
    if (druckbar * 100 < (long) SCHWELLE_DRUCKBAR_PROZENT * laenge) {
      return Ergebnis.ohneText(Artefaktzustand.BINAERDATEI);
    }
    return Ergebnis.text(kodierung, text);
  }

  private static boolean istDruckbar(char zeichen) {
    if (zeichen == '\t' || zeichen == '\n' || zeichen == '\r') {
      return true;
    }
    // 0x20 bis 0x7E: druckbares ASCII. Ab 0xA0: der belegte obere Bereich von ISO-8859-1 und
    // alles, was UTF-8 darueber hinaus liefert. 0x80 bis 0x9F sind Steuerzeichen und zaehlen nicht.
    return (zeichen >= 0x20 && zeichen <= 0x7E) || zeichen >= 0xA0;
  }
}
