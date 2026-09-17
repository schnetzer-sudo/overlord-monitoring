package de.kraftwerkone.overlord.monitor.payload;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Die Kodierung, mit der ein Artefakt <b>gelesen</b> worden ist — je Datei, nicht fest.
 *
 * <h2>Nur, was an den Bytes feststeht</h2>
 *
 * <p>Zwei der drei Werte sind an den Bytes <b>ablesbar</b>: {@link #ASCII} heisst, kein Byte liegt
 * ueber {@code 0x7F}; {@link #UTF_8} heisst, die Bytes sind streng gueltiges UTF-8 und tragen
 * mindestens eines ueber {@code 0x7F}. Der dritte ist es <b>nicht</b>: {@link #ISO_8859_1} ist der
 * Rueckfall fuer alles, was weder das eine noch das andere ist — und {@code ISO-8859-1} bildet
 * jedes Byte auf ein Zeichen ab, es kann also gar nicht scheitern. Dass eine solche Datei
 * tatsaechlich {@code ISO-8859-1} ist und nicht Windows-1252 oder eine DOS-Codepage, sagt keine
 * Messung: M61 belegt nur „kein gueltiges UTF-8", und das trifft auf alle drei zu. Die Oberflaeche
 * beschriftet den Wert deshalb mit <i>gelesen als</i> und nicht mit <i>Kodierung</i> (Regel Q4).
 *
 * <p>Bis zum 17.09.2026 wurde jede Datei fest mit {@code ISO-8859-1} dekodiert. Nach M61 sind 7 von
 * 16 entscheidbaren Nutzdateien gueltiges UTF-8 mit Bytes ueber {@code 0x7F} — die wurden damit
 * falsch angezeigt, aus „fuer" wurde „fÃ¼r", und es sah nicht kaputt aus.
 *
 * <h2>Was sie kann</h2>
 *
 * <p>Sie kodiert den Text fuer den beschnittenen Download <b>zurueck</b> — mit derselben Kodierung,
 * mit der gelesen wurde (Gleichlauf, Entscheidung 9) — und sie kappt die Anzeige <b>auf einer
 * Zeichengrenze</b>: Die Anzeigegrenze zaehlt Bytes, und ein Byte-Schnitt mitten in einer
 * UTF-8-Folge erzeugte ein Ersatzzeichen, das in der Datei nie stand. Dieselbe Regel wie bei der
 * Kappung eines Eigenschaftswerts ({@code docs/nachrichtendetail.md}).
 */
public enum Kodierung {

  /** Kein Byte ueber {@code 0x7F}. Zugleich gueltiges UTF-8 und gueltiges ISO-8859-1. */
  ASCII(StandardCharsets.US_ASCII),

  /** Streng gueltiges UTF-8 mit mindestens einem Byte ueber {@code 0x7F}. */
  UTF_8(StandardCharsets.UTF_8),

  /** Der Rueckfall. Nicht festgestellt, sondern angenommen — deshalb <i>gelesen als</i>. */
  ISO_8859_1(StandardCharsets.ISO_8859_1);

  private final Charset charset;

  Kodierung(Charset charset) {
    this.charset = charset;
  }

  public Charset charset() {
    return charset;
  }

  /** Der Text zurueck in Bytes — fuer den beschnittenen Download, mit derselben Kodierung. */
  public byte[] kodiere(String text) {
    return text.getBytes(charset);
  }

  /**
   * Ob der Text in dieser Kodierung mehr als {@code grenzeBytes} Bytes belegt.
   *
   * <p>Ein Zeichen belegt nie weniger als eine UTF-16-Einheit und in keiner der drei Kodierungen
   * weniger als ein Byte je Einheit. {@code text.length()} ist damit eine Untergrenze fuer die
   * Bytezahl; bei den beiden Einbyte-Kodierungen ist es die Bytezahl selbst, und nur bei UTF-8 muss
   * — und nur unterhalb der Grenze — nachgezaehlt werden.
   */
  public boolean ueberschreitet(String text, long grenzeBytes) {
    if (text.length() > grenzeBytes) {
      return true;
    }
    return this == UTF_8 && text.getBytes(charset).length > grenzeBytes;
  }

  /**
   * Kappt den Text auf hoechstens {@code grenzeBytes} Bytes in dieser Kodierung — <b>auf einer
   * Zeichengrenze</b>.
   *
   * <p>Bei UTF-8 wird vom Schnitt so weit zurueckgegangen, bis kein Folgebyte mehr dasteht. Eine
   * Vier-Byte-Folge ist ein Codepunkt und in Java ein Surrogatpaar; wer sie ganz laesst oder ganz
   * wegnimmt, zerschneidet auch das Paar nicht. Bei ASCII und ISO-8859-1 ist jedes Byte ein
   * Zeichen, dort ist der Byte-Schnitt bereits der Zeichenschnitt — und dort darf auch <b>nicht</b>
   * zurueckgegangen werden: {@code 0x80}–{@code 0xBF} sind in ISO-8859-1 eigene Zeichen und keine
   * Folgebytes.
   */
  public String gekappt(String text, long grenzeBytes) {
    if (!ueberschreitet(text, grenzeBytes)) {
      return text;
    }
    if (this != UTF_8) {
      return text.substring(0, (int) grenzeBytes);
    }
    byte[] roh = text.getBytes(charset);
    int ende = (int) grenzeBytes;
    while (ende > 0 && istFolgebyte(roh[ende])) {
      ende--;
    }
    return new String(roh, 0, ende, charset);
  }

  /** Ein UTF-8-Folgebyte traegt das Bitmuster {@code 10xxxxxx}. */
  private static boolean istFolgebyte(byte wert) {
    return (wert & 0xC0) == 0x80;
  }
}
