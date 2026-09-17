package de.kraftwerkone.overlord.monitor.payload;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die Einstufung der Bytes eines ZIP-Eintrags — <b>jede Stufe der Reihenfolge einzeln, die Grenzen
 * genau, und die Gegenproben, die nicht als EBCDIC gelten duerfen.</b>
 *
 * <p>Ohne Datenbank, ohne Filestore, <b>nur erfundene Bytes</b>. Kein Inhalt hier ist echt; die
 * EBCDIC-Bytes entstehen aus erfundenem Text ueber die JDK-Codepage {@code IBM037}, die DOS-Bytes
 * ueber {@code IBM850} — beide sind Teil des JDK-Moduls {@code jdk.charsets}.
 *
 * <p>Die fuenf Regressionsfaelle der alten {@code Binaerpruefung} stehen unveraendert in {@code
 * ArtefaktbausteineTest}.
 */
class InhaltseinstufungTest {

  private static final Charset EBCDIC = Charset.forName("IBM037");
  private static final Charset DOS = Charset.forName("IBM850");
  private static final Charset WINDOWS = Charset.forName("windows-1252");

  private static Inhaltseinstufung.Ergebnis stufeEin(String text, Charset charset) {
    return Inhaltseinstufung.stufeEin(text.getBytes(charset));
  }

  /**
   * {@code anzahl} Bytes des Werts {@code a}, gefolgt von {@code rest} Bytes des Werts {@code b}.
   */
  private static byte[] bytes(int anzahl, int a, int rest, int b) {
    byte[] daten = new byte[anzahl + rest];
    Arrays.fill(daten, 0, anzahl, (byte) a);
    Arrays.fill(daten, anzahl, anzahl + rest, (byte) b);
    return daten;
  }

  @Nested
  @DisplayName("Die Reihenfolge, Stufe fuer Stufe")
  class Reihenfolge {

    @Test
    @DisplayName("1 — Ein Nullbyte ist Binaerdatei, ohne Kodierung und ohne Text")
    void nullbyte() {
      Inhaltseinstufung.Ergebnis ergebnis =
          stufeEin("Erfundener Text mit\0Nullbyte", StandardCharsets.US_ASCII);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.BINAERDATEI);
      assertThat(ergebnis.istText()).isFalse();
      assertThat(ergebnis.kodierung()).isNull();
      assertThat(ergebnis.text()).isEmpty();
    }

    @Test
    @DisplayName("1 vor 4 — Eine EBCDIC-Datei mit Nullbytes bleibt Binaerdatei (offener Punkt)")
    void ebcdic_mit_nullbyte_bleibt_binaer() {
      byte[] ebcdic = "ERFUNDENE LIEFERUNG 4711".getBytes(EBCDIC);
      byte[] mitNullbytes = Arrays.copyOf(ebcdic, ebcdic.length + 4);

      assertThat(Inhaltseinstufung.stufeEin(mitNullbytes).zustand())
          .as("Das Nullbyte entscheidet zuerst; das Muster wird danach nicht mehr gesucht")
          .isEqualTo(Artefaktzustand.BINAERDATEI);
    }

    @Test
    @DisplayName("2 — Kein Byte ueber 0x7F ist ASCII")
    void ascii() {
      Inhaltseinstufung.Ergebnis ergebnis =
          stufeEin("2025-12-29 10:00:00 ERFUNDEN-Zeile\nnoch eine\n", StandardCharsets.US_ASCII);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.ASCII);
      assertThat(ergebnis.text()).isEqualTo("2025-12-29 10:00:00 ERFUNDEN-Zeile\nnoch eine\n");
    }

    @Test
    @DisplayName("3 — Streng gueltiges UTF-8 mit ß, Ä, Ö, Ü und € ist UTF-8")
    void utf_8() {
      String text = "Erfundene Grüße: ß Ä Ö Ü € 12,50\n";
      Inhaltseinstufung.Ergebnis ergebnis = stufeEin(text, StandardCharsets.UTF_8);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.UTF_8);
      assertThat(ergebnis.text())
          .as("Bis zum 17.09.2026 stand hier „GrÃ¼ÃŸe“ — und es sah nicht kaputt aus")
          .isEqualTo(text);
    }

    @Test
    @DisplayName("3 — Ein UTF-8-BOM am Anfang steht nicht im Text")
    void utf_8_bom() {
      byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
      byte[] inhalt = "Erfundene Grüße\n".getBytes(StandardCharsets.UTF_8);
      byte[] daten = Arrays.copyOf(bom, bom.length + inhalt.length);
      System.arraycopy(inhalt, 0, daten, bom.length, inhalt.length);

      Inhaltseinstufung.Ergebnis ergebnis = Inhaltseinstufung.stufeEin(daten);

      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.UTF_8);
      assertThat(ergebnis.text()).isEqualTo("Erfundene Grüße\n");
      assertThat(ergebnis.text()).doesNotStartWith("\uFEFF");
    }

    @Test
    @DisplayName("3 — Ein BOM allein ist eine leere Textdatei, keine Binaerdatei")
    void nur_bom() {
      byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

      Inhaltseinstufung.Ergebnis ergebnis = Inhaltseinstufung.stufeEin(bom);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.UTF_8);
      assertThat(ergebnis.text()).isEmpty();
    }

    @Test
    @DisplayName("3 — Nicht streng: ein ueberlanges oder abgeschnittenes UTF-8 ist kein UTF-8")
    void utf_8_streng() {
      // 0xC0 0xAF ist ein ueberlang kodierter Schraegstrich; der Decoder mit REPORT lehnt ihn ab.
      byte[] ueberlang = {'a', (byte) 0xC0, (byte) 0xAF, 'b'};
      // 0xC3 ohne Folgebyte am Ende.
      byte[] abgeschnitten = {'a', 'b', (byte) 0xC3};

      assertThat(Inhaltseinstufung.stufeEin(ueberlang).kodierung()).isEqualTo(Kodierung.ISO_8859_1);
      assertThat(Inhaltseinstufung.stufeEin(abgeschnitten).kodierung())
          .isEqualTo(Kodierung.ISO_8859_1);
    }

    @Test
    @DisplayName("4 — Grossschrift im EBCDIC-Muster: bisher als Text mit Zeichenmuell angezeigt")
    void ebcdic_grossschrift() {
      Inhaltseinstufung.Ergebnis ergebnis = stufeEin("VDA 4905 ERFUNDENE LIEFERUNG 0815", EBCDIC);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.EBCDIC_DATEI);
      assertThat(ergebnis.istText()).isFalse();
      assertThat(ergebnis.kodierung()).isNull();
      assertThat(ergebnis.text()).as("Kein Text, kein Zeichenmuell wie „åÄÁ@ôùðõ“").isEmpty();
    }

    @Test
    @DisplayName("4 — Gemischte Schreibung: bisher wegen der Kleinbuchstaben Binaerdatei")
    void ebcdic_gemischt() {
      // a bis i liegen auf 0x81 bis 0x89 und zaehlten bisher als nicht druckbar — die Datei war
      // damit eine Binaerdatei und trug keinen Namen fuer das, was sie ist.
      Inhaltseinstufung.Ergebnis ergebnis =
          stufeEin("Erfundene Lieferung 4711 an Beispielstrasse 12, Menge 30", EBCDIC);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.EBCDIC_DATEI);
    }

    @Test
    @DisplayName("4 — Die EBCDIC-Zeilenenden 0x15, 0x25 und 0x0D zaehlen zum Vorrat")
    void ebcdic_zeilenenden() {
      byte[] zeile = "ERFUNDENE ZEILE 4711".getBytes(EBCDIC);
      byte[] daten = new byte[zeile.length * 3 + 3];
      int[] enden = {0x15, 0x25, 0x0D};
      for (int i = 0; i < 3; i++) {
        System.arraycopy(zeile, 0, daten, i * (zeile.length + 1), zeile.length);
        daten[i * (zeile.length + 1) + zeile.length] = (byte) enden[i];
      }

      assertThat(Inhaltseinstufung.stufeEin(daten).zustand())
          .isEqualTo(Artefaktzustand.EBCDIC_DATEI);
    }

    @Test
    @DisplayName("5 — Alles andere wird als ISO-8859-1 gelesen, wie bisher")
    void iso_8859_1() {
      String text = "Erfundene Grüße aus Köln\n";
      Inhaltseinstufung.Ergebnis ergebnis = stufeEin(text, StandardCharsets.ISO_8859_1);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.ISO_8859_1);
      assertThat(ergebnis.text()).isEqualTo(text);
    }

    @Test
    @DisplayName("6 — Unter 95 % druckbaren Zeichen ist es eine Binaerdatei, in jeder Kodierung")
    void druckbarkeit_je_kodierung() {
      // ASCII: 50 Buchstaben, 50 unbelegte Steuerzeichen.
      assertThat(Inhaltseinstufung.stufeEin(bytes(50, 'a', 50, 0x01)).zustand())
          .isEqualTo(Artefaktzustand.BINAERDATEI);
      // ISO-8859-1: 50 Umlaute (0xFC, druckbar ab 0xA0), 50 Steuerzeichen aus 0x80–0x9F. Beide
      // Werte liegen ausserhalb des EBCDIC-Vorrats, und 0xFC ist kein gueltiges
      // UTF-8-Fuehrungsbyte.
      assertThat(Inhaltseinstufung.stufeEin(bytes(50, 0xFC, 50, 0x8A)).zustand())
          .isEqualTo(Artefaktzustand.BINAERDATEI);
      // UTF-8: 50 „ä" (je zwei Bytes) und 50 Steuerzeichen 0x01.
      byte[] utf8 = new byte[150];
      for (int i = 0; i < 50; i++) {
        utf8[2 * i] = (byte) 0xC3;
        utf8[2 * i + 1] = (byte) 0xA4;
      }
      Arrays.fill(utf8, 100, 150, (byte) 0x01);
      assertThat(Inhaltseinstufung.stufeEin(utf8).zustand()).isEqualTo(Artefaktzustand.BINAERDATEI);
    }

    @Test
    @DisplayName("6 — Bei UTF-8 zaehlen Folgebytes nicht mehr einzeln als Steuerzeichen")
    void folgebytes_zaehlen_als_zeichen() {
      // U+0100 „Ā" ist C4 80: Das Folgebyte 0x80 zaehlte auf den Bytes als Steuerzeichen; eine
      // Datei aus hundert davon lag bei 50 % und war eine Binaerdatei. Als Zeichen gezaehlt ist
      // sie zu 100 % druckbar.
      String text = "Ā".repeat(100);

      Inhaltseinstufung.Ergebnis ergebnis = stufeEin(text, StandardCharsets.UTF_8);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.UTF_8);
      assertThat(ergebnis.text()).isEqualTo(text);
    }

    @Test
    @DisplayName("Eine leere Datei ist eine leere Textdatei in ASCII")
    void leer() {
      assertThat(Inhaltseinstufung.stufeEin(new byte[0]))
          .isEqualTo(Inhaltseinstufung.stufeEin(null))
          .isEqualTo(
              new Inhaltseinstufung.Ergebnis(Artefaktzustand.ANZEIGBAR, Kodierung.ASCII, ""));
    }
  }

  @Nested
  @DisplayName("Die Grenzen, genau und knapp darunter")
  class Grenzen {

    /** 0xC1 ist „A" im EBCDIC-Vorrat; 0x5A liegt ausserhalb, unter 0x7F und ist kein Nullbyte. */
    private static final int IM_VORRAT = 0xC1;

    private static final int AUSSERHALB = 0x5A;

    @Test
    @DisplayName("Genau 90 % im EBCDIC-Vorrat ist das Muster")
    void ebcdic_genau_90() {
      assertThat(Inhaltseinstufung.stufeEin(bytes(90, IM_VORRAT, 10, AUSSERHALB)).zustand())
          .isEqualTo(Artefaktzustand.EBCDIC_DATEI);
    }

    @Test
    @DisplayName("89 % ist es nicht — und faellt auf ISO-8859-1")
    void ebcdic_knapp_darunter() {
      Inhaltseinstufung.Ergebnis ergebnis =
          Inhaltseinstufung.stufeEin(bytes(89, IM_VORRAT, 11, AUSSERHALB));

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.ISO_8859_1);
    }

    @Test
    @DisplayName("Genau 95 % druckbare Zeichen ist Text")
    void druckbar_genau_95() {
      Inhaltseinstufung.Ergebnis ergebnis = Inhaltseinstufung.stufeEin(bytes(95, 'a', 5, 0x01));

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.ASCII);
    }

    @Test
    @DisplayName("94 % ist Binaerdatei")
    void druckbar_knapp_darunter() {
      assertThat(Inhaltseinstufung.stufeEin(bytes(94, 'a', 6, 0x01)).zustand())
          .isEqualTo(Artefaktzustand.BINAERDATEI);
    }

    @Test
    @DisplayName("Die Grenzen gelten ueber die ganze Datei, nicht ueber ein Praefix")
    void ganze_datei() {
      // 70.000 druckbare Bytes, dann 5.000 Steuerzeichen: Auf den ersten 64 KiB laege die Datei
      // bei 100 %, ueber alles bei 93,3 %.
      assertThat(Inhaltseinstufung.stufeEin(bytes(70_000, 'a', 5_000, 0x01)).zustand())
          .isEqualTo(Artefaktzustand.BINAERDATEI);
    }
  }

  @Nested
  @DisplayName("Gegenproben — nichts davon ist EBCDIC")
  class Gegenproben {

    private static final String DEUTSCH =
        "Erfundene Lieferung 4711 an die Beispielstrasse 12, mit Grüßen aus Köln, Menge 30 Stück,"
            + " Preis 12,50 je Stück, Lieferdatum 29.12.2025, Bestellnummer ERFUNDEN-0815.\n";

    @Test
    @DisplayName("Deutscher Text in ISO-8859-1")
    void iso_8859_1() {
      Inhaltseinstufung.Ergebnis ergebnis = stufeEin(DEUTSCH, StandardCharsets.ISO_8859_1);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.ISO_8859_1);
    }

    @Test
    @DisplayName("Windows-1252 mit € (0x80) — gelesen als ISO-8859-1, nicht als EBCDIC")
    void windows_1252() {
      Inhaltseinstufung.Ergebnis ergebnis = stufeEin(DEUTSCH + "Summe 375 €\n", WINDOWS);

      assertThat(ergebnis.zustand())
          .as("0x80 ist in ISO-8859-1 ein Steuerzeichen, eines von ueber 150 — unter der Schwelle")
          .isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung())
          .as("Nicht erkannt und nicht erraten: Windows-1252 wird nicht unterschieden")
          .isEqualTo(Kodierung.ISO_8859_1);
    }

    @Test
    @DisplayName("DOS-Codepage 850 mit Umlauten — nicht als EBCDIC")
    void cp850() {
      Inhaltseinstufung.Ergebnis ergebnis = stufeEin(DEUTSCH, DOS);

      assertThat(ergebnis.zustand()).isNotEqualTo(Artefaktzustand.EBCDIC_DATEI);
      assertThat(ergebnis.kodierung())
          .as("Auch CP850 wird nicht unterschieden — der Rueckfall bleibt ISO-8859-1")
          .isEqualTo(Kodierung.ISO_8859_1);
    }

    @Test
    @DisplayName("UTF-8")
    void utf_8() {
      assertThat(stufeEin(DEUTSCH, StandardCharsets.UTF_8).kodierung()).isEqualTo(Kodierung.UTF_8);
    }

    @Test
    @DisplayName("EDIFACT in ASCII — Grossschrift, Ziffern und Trennzeichen")
    void edifact() {
      String edifact =
          "UNB+UNOA:2+ERFUNDEN+ERFUNDEN+250101:1200+1'UNH+1+ORDERS:D:96A:UN'BGM+220+0815+9'"
              + "DTM+137:20250101:102'NAD+BY+4711::9'LIN+1++4000000000001:EN'QTY+21:30'UNS+S'"
              + "UNT+8+1'UNZ+1+1'";

      Inhaltseinstufung.Ergebnis ergebnis = stufeEin(edifact, StandardCharsets.US_ASCII);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.kodierung()).isEqualTo(Kodierung.ASCII);
    }

    @Test
    @DisplayName(
        "Gleichverteilte Zufallsbytes ohne Nullbyte: Binaerdatei, rund ein Drittel im Vorrat")
    void zufallsbytes() {
      byte[] daten = new byte[4096];
      Random zufall = new Random(4711);
      long imVorrat = 0;
      for (int i = 0; i < daten.length; i++) {
        int wert = 1 + zufall.nextInt(255);
        daten[i] = (byte) wert;
        if (imEbcdicVorrat(wert)) {
          imVorrat++;
        }
      }

      assertThat(Inhaltseinstufung.stufeEin(daten).zustand())
          .isEqualTo(Artefaktzustand.BINAERDATEI);
      assertThat((double) imVorrat / daten.length)
          .as("85 von 255 Werten liegen im Vorrat — die Begruendung der 90 %-Schwelle")
          .isBetween(0.25, 0.42);
    }

    /**
     * Der invariante Vorrat, unabhaengig von der Klasse nachgebaut, damit die Zahl etwas belegt.
     */
    private static boolean imEbcdicVorrat(int wert) {
      return wert == 0x40
          || (wert >= 0xF0 && wert <= 0xF9)
          || (wert >= 0xC1 && wert <= 0xC9)
          || (wert >= 0xD1 && wert <= 0xD9)
          || (wert >= 0xE2 && wert <= 0xE9)
          || (wert >= 0x81 && wert <= 0x89)
          || (wert >= 0x91 && wert <= 0x99)
          || (wert >= 0xA2 && wert <= 0xA9)
          || (wert >= 0x4B && wert <= 0x4E)
          || wert == 0x50
          || (wert >= 0x5C && wert <= 0x5E)
          || wert == 0x60
          || wert == 0x61
          || (wert >= 0x6B && wert <= 0x6F)
          || wert == 0x7A
          || (wert >= 0x7D && wert <= 0x7F)
          || wert == 0x0D
          || wert == 0x15
          || wert == 0x25;
    }
  }

  @Nested
  @DisplayName("Kodierung — Rueckweg und Kappung auf einer Zeichengrenze")
  class KodierungTest {

    @Test
    @DisplayName("Zurueckkodiert wird mit derselben Kodierung")
    void kodiere() {
      assertThat(Kodierung.UTF_8.kodiere("ä")).containsExactly(0xC3, 0xA4);
      assertThat(Kodierung.ISO_8859_1.kodiere("ä")).containsExactly(0xE4);
      assertThat(Kodierung.ASCII.kodiere("a")).containsExactly(0x61);
    }

    @Test
    @DisplayName("UTF-8: Der Schnitt geht bis zur Zeichengrenze zurueck")
    void utf_8_zeichengrenze() {
      String text = "ä".repeat(10); // 20 Bytes

      assertThat(Kodierung.UTF_8.ueberschreitet(text, 5)).isTrue();
      assertThat(Kodierung.UTF_8.gekappt(text, 5))
          .as("5 Bytes fielen mitten in das dritte „ä“ — es faellt ganz weg")
          .isEqualTo("ää");
      assertThat(Kodierung.UTF_8.gekappt(text, 6)).isEqualTo("äää");
      assertThat(Kodierung.UTF_8.ueberschreitet(text, 20)).isFalse();
      assertThat(Kodierung.UTF_8.gekappt(text, 20)).isEqualTo(text);
    }

    @Test
    @DisplayName("UTF-8: Ein Surrogatpaar wird nicht zerschnitten")
    void utf_8_surrogatpaar() {
      String text = "😀".repeat(3); // U+1F600, je vier Bytes

      assertThat(Kodierung.UTF_8.gekappt(text, 6)).isEqualTo("😀");
      assertThat(Kodierung.UTF_8.gekappt(text, 8)).isEqualTo("😀😀");
    }

    @Test
    @DisplayName("ISO-8859-1 und ASCII: Byte-Schnitt ist Zeichenschnitt — kein Zurueckgehen")
    void einbyte() {
      String umlaute = "ä".repeat(10);

      assertThat(Kodierung.ISO_8859_1.ueberschreitet(umlaute, 5)).isTrue();
      assertThat(Kodierung.ISO_8859_1.gekappt(umlaute, 5))
          .as("0xE4 ist in ISO-8859-1 ein ganzes Zeichen, kein Folgebyte")
          .isEqualTo("äääää");
      assertThat(Kodierung.ASCII.gekappt("x".repeat(50), 10)).isEqualTo("x".repeat(10));
      assertThat(Kodierung.ASCII.ueberschreitet("x".repeat(10), 10)).isFalse();
    }
  }
}
