package de.kraftwerkone.overlord.monitor.payload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Entpackt den Anhang und nimmt den <b>ersten</b> Eintrag.
 *
 * <p>Die Auslieferung ist gepackt, ausnahmslos (Q4, M71). In <b>693 geholten Dateien kein einziger
 * Fall mit mehr als einem Eintrag</b> (Teil B). Der Entpackschritt ist trotzdem nicht wegzudenken,
 * und der Mehrfachfall wird behandelt.
 *
 * <h2>Warum die Zahl der Eintraege mitkommt</h2>
 *
 * <p>Das Altsystem verwirft alles nach dem ersten Eintrag stillschweigend ({@code
 * JsonServlet.java:801}–{@code :802}). Hier wird die Zahl <b>im Ergebnis vermerkt und
 * protokolliert</b>: Ein Fall, der in 693 Dateien nicht vorkam, ist beim ersten Auftreten ein
 * Befund und kein Randfall. Was dann geschieht — ob alle Eintraege angeboten werden —, ist offen
 * ({@code docs/rohdaten.md} §13, Punkt 6) und wird hier nicht entschieden.
 *
 * <h2>Grenzen</h2>
 *
 * <p>Der Eintrag wird <b>gedeckelt gelesen</b>, nicht gedeckelt geprueft. {@code
 * ZipEntry.getSize()} ist eine Angabe aus dem Archiv und damit eine Behauptung der Gegenseite; wer
 * ihr glaubt, hat keine Grenze, sondern eine Bitte. Gezaehlt werden die Bytes, die tatsaechlich
 * herauskommen.
 */
public final class Zipentnahme {

  private Zipentnahme() {}

  /**
   * Das Ergebnis des Entpackens.
   *
   * @param daten die Bytes des ersten Eintrags
   * @param eintraege wie viele Eintraege das Archiv insgesamt traegt. Alles ueber {@code 1} ist ein
   *     bisher nie beobachteter Fall und erscheint in der Antwort
   * @param name der Name des ersten Eintrags im Archiv, oder {@code null}. <b>Er wird nicht als
   *     Dateiname verwendet</b> — siehe {@link Downloaddateiname}
   */
  public record Inhalt(byte[] daten, int eintraege, String name) {}

  /**
   * Entpackt und liefert den ersten Eintrag.
   *
   * @param zip die Bytes des Anhangs
   * @param grenzeBytes Obergrenze fuer den entpackten Eintrag
   * @return {@code null}, wenn das Archiv unlesbar ist, keinen Eintrag traegt oder der erste
   *     Eintrag die Grenze reisst. Der Aufrufer macht daraus einen benannten Zustand
   */
  public static Inhalt ersterEintrag(byte[] zip, long grenzeBytes) {
    if (zip == null || zip.length == 0) {
      return null;
    }
    byte[] erster = null;
    String name = null;
    int eintraege = 0;
    try (ZipInputStream strom = new ZipInputStream(new ByteArrayInputStream(zip))) {
      ZipEntry eintrag;
      while ((eintrag = strom.getNextEntry()) != null) {
        if (eintrag.isDirectory()) {
          // Verzeichniseintraege zaehlen nicht als Datei. In 693 Archiven kam keiner vor.
          continue;
        }
        eintraege++;
        if (erster == null) {
          erster = lies(strom, grenzeBytes);
          if (erster == null) {
            return null;
          }
          name = eintrag.getName();
        }
        // Die folgenden Eintraege werden nicht gelesen, nur gezaehlt. Ihr Inhalt interessiert
        // nicht — dass es sie gibt, schon.
      }
    } catch (IOException unlesbar) {
      return null;
    }
    if (erster == null) {
      return null;
    }
    return new Inhalt(erster, eintraege, name);
  }

  /** Liest einen Eintrag gedeckelt. {@code null}, sobald die Grenze gerissen ist. */
  private static byte[] lies(ZipInputStream strom, long grenzeBytes) throws IOException {
    ByteArrayOutputStream gesammelt = new ByteArrayOutputStream();
    byte[] puffer = new byte[8192];
    long gesamt = 0;
    int gelesen;
    while ((gelesen = strom.read(puffer)) != -1) {
      gesamt += gelesen;
      if (gesamt > grenzeBytes) {
        return null;
      }
      gesammelt.write(puffer, 0, gelesen);
    }
    return gesammelt.toByteArray();
  }
}
