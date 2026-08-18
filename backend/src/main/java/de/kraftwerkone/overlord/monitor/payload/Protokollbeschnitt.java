package de.kraftwerkone.overlord.monitor.payload;

import java.util.ArrayList;
import java.util.List;

/**
 * Der Beschnitt bei Protokollen: <b>erste Startmarke bis zur naechsten darauffolgenden
 * Endmarke</b>, ohne die Markenzeilen, mit Pfadmaskierung.
 *
 * <p>Er greift <b>nur</b> bei Artefakten mit {@code Log.GUID} im Namen und <b>nur</b> fuer die
 * Rolle {@code MANDANT} ({@code docs/rohdaten.md} §6, Entscheidung 3). Ueber die Rolle, nie ueber
 * ein Kennzeichen aus der Anfrage.
 *
 * <h2>Die fuenf Faelle</h2>
 *
 * <table border="1">
 *   <caption>Verhalten je Fall, und das Verhaeltnis zum Altsystem</caption>
 *   <tr><th>Fall</th><th>Ergebnis</th><th>gegenueber dem Altsystem</th></tr>
 *   <tr><td>Vollstaendiges Paar</td><td>Innenbereich ohne die Markenzeilen</td><td>identisch (Q2)</td></tr>
 *   <tr><td>Mehrere Paare</td><td><b>nur das erste</b></td>
 *       <td><b>strenger</b> — das Altsystem haengt alle Bloecke aneinander ({@code :841})</td></tr>
 *   <tr><td>Startmarke ohne Endmarke</td><td><b>nichts</b>, benannter Zustand</td>
 *       <td><b>strenger</b> — das Altsystem zeigt alles ab der Startmarke ({@code :823})</td></tr>
 *   <tr><td>Keine Startmarke</td><td><b>nichts</b>, benannter Zustand</td><td>identisch (Q2)</td></tr>
 *   <tr><td>Paar ohne Inhalt</td><td><b>nichts</b>, benannter Zustand</td>
 *       <td>identisch im Ergebnis; das Altsystem liefert dort eine leere Ausgabe</td></tr>
 * </table>
 *
 * <p><b>Der fuenfte Fall ist eine Ableitung, keine eigene Entscheidung.</b> {@code
 * docs/rohdaten.md} §6 nennt ihn nicht getrennt, weil er dasselbe Ergebnis hat wie „kein
 * vollstaendiges Paar": Es gibt nichts zu zeigen. Er bekommt deshalb denselben benannten Zustand
 * statt eines leeren Kastens — §8 sagt ausdruecklich, dass keiner der Zustaende ein leeres Feld
 * ist. Er kommt vor: Die Selbstpruefung des Auswertungsskripts hat ihn an {@code 3.log} erzeugt, wo
 * eine echote Zeile das Paar sofort schliesst.
 *
 * <h2>Warum zwei Faelle strenger sind</h2>
 *
 * <p>Das Protokoll gibt Werte aus der Nachricht aus — {@code Message.DestinationFilename}, {@code
 * Message.ReceivingPartner}, {@code Message.SourceMessageID}. Diese Werte kommen aus der EDI-Datei
 * und damit <b>vom Partner</b>. Ein Dateiname, der {@code ***EndOfLog***} enthaelt, landet als
 * echote Zeile im Protokoll. Die Marken sind also von aussen beeinflussbar; die beiden
 * grosszuegigeren Regeln liessen sich durch eine eingeschleuste Marke dazu bringen, Bereiche
 * freizugeben, die ausserhalb liegen. Gemessen ist der Fall bisher nicht (M63: kein Echo-Fall in
 * 395 Dateien) — die Regel kostet nichts und schliesst ihn aus.
 *
 * <h2>Was der Beschnitt nicht ist</h2>
 *
 * <p><b>Keine Vertraulichkeitszusage.</b> Er ist eine Lesbarkeitsregel und wird nirgends als
 * Sicherheitsgrenze dokumentiert oder kommentiert. Er haelt weniger zurueck, als sein Name
 * nahelegt: 808 Pfadzeilen und 331 Dienstkennungen liegen <b>innerhalb</b> der Marken (M65), und
 * der Innenbereich ist mit 90,4 % der Bytes der ueberwiegende Teil der Datei (M67). Er verkuerzt
 * also kaum. Er ist trotzdem gebaut, weil das die Entscheidung ist ({@code docs/rohdaten.md} §3,
 * Entscheidung 5) — aber niemand soll spaeter eine Zusage darauf gruenden.
 *
 * <h2>Bekannte Folge</h2>
 *
 * <p>Bei {@code HTTPSender} und {@code FTPSender} sieht {@code MANDANT} damit <b>nie</b> ein
 * Protokoll: {@code HTTPSender} traegt in 30 von 30 Faellen keine Marken, {@code FTPSender} in 28
 * von 30 (M63) — und {@code FTPSender} haengt an rund 69 % der Nachrichten. Das ist ausdruecklich
 * hingenommen. Die Oberflaeche zeigt dort keinen leeren Kasten, sondern {@link
 * Artefaktzustand#KEIN_ANZEIGBARER_PROTOKOLLTEIL}.
 */
public final class Protokollbeschnitt {

  private Protokollbeschnitt() {}

  /** Die Startmarke. Erkannt mit <i>enthaelt</i>, nicht mit Gleichheit — wie im Altsystem (Q2). */
  static final String STARTMARKE = "***StartOfLog***";

  /** Die Endmarke, ebenfalls mit <i>enthaelt</i>. */
  static final String ENDMARKE = "***EndOfLog***";

  /**
   * Das Ergebnis eines Beschnitts.
   *
   * @param text der anzuzeigende Text. Leer, wenn {@code zustand} nicht {@link
   *     Artefaktzustand#ANZEIGBAR} ist
   * @param beschnitten ob ueberhaupt beschnitten wurde. <b>Diese Angabe geht ins {@code
   *     audit_log}</b> — ein Eintrag, der beschnitten und vollstaendig nicht unterscheidet, ist bei
   *     einer Rueckfrage wertlos, und die Rueckfrage ist der Grund fuer das Protokoll
   * @param zustand {@link Artefaktzustand#ANZEIGBAR} oder {@link
   *     Artefaktzustand#KEIN_ANZEIGBARER_PROTOKOLLTEIL}
   */
  public record Ergebnis(String text, boolean beschnitten, Artefaktzustand zustand) {

    static Ergebnis nichts() {
      return new Ergebnis("", true, Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
    }
  }

  /**
   * Beschneidet und maskiert.
   *
   * <p>Die Zeilen werden mit {@code \n} wieder zusammengesetzt, unabhaengig davon, welche
   * Zeilenenden die Datei traegt — die sind uneinheitlich (M61: LF 95, CRLF 38, gemischt 37, keines
   * 36 von 206), und der beschnittene Text ist ohnehin ein neu gebauter Ausschnitt und nicht die
   * Datei. Im <b>unbeschnittenen</b> Zweig wird dagegen nichts angefasst; dort ist die Datei die
   * Antwort.
   *
   * @param text die vollstaendige, bereits nach {@code ISO-8859-1} dekodierte Datei
   */
  public static Ergebnis beschneide(String text) {
    if (text == null || text.isEmpty()) {
      return Ergebnis.nichts();
    }
    List<String> zeilen = zeilen(text);

    int start = -1;
    for (int i = 0; i < zeilen.size(); i++) {
      if (zeilen.get(i).contains(STARTMARKE)) {
        start = i;
        break;
      }
    }
    if (start < 0) {
      // Fall „keine Startmarke". Identisch zum Altsystem.
      return Ergebnis.nichts();
    }

    List<String> innen = new ArrayList<>();
    boolean geschlossen = false;
    for (int i = start + 1; i < zeilen.size(); i++) {
      String zeile = zeilen.get(i);
      if (zeile.contains(ENDMARKE)) {
        geschlossen = true;
        break;
      }
      innen.add(Pfadmaskierung.maskiere(zeile));
    }

    if (!geschlossen || innen.isEmpty()) {
      // Fall „Startmarke ohne Endmarke" — strenger als das Altsystem, das ab hier alles zeigt —
      // und Fall „Paar ohne Inhalt". Beide haben dasselbe Ergebnis: es gibt nichts zu zeigen.
      return Ergebnis.nichts();
    }

    // Hier endet der Beschnitt. Eine weitere Startmarke wird NICHT gesucht: „erste Start- bis
    // naechste Endmarke", nicht „alle Bloecke aneinander".
    return new Ergebnis(String.join("\n", innen), true, Artefaktzustand.ANZEIGBAR);
  }

  /**
   * Zerlegt in Zeilen, so wie {@code BufferedReader.readLine} es tut: an {@code \r\n}, {@code \n}
   * und {@code \r}. Die Zeilenenden sind uneinheitlich (M61), und das Altsystem liest ueber
   * denselben Reader — wer hier nur an {@code \n} traennte, faende in einer CRLF-Datei die Marken
   * zwar noch (sie werden mit <i>enthaelt</i> gesucht), haengte aber ein {@code \r} an jede Zeile.
   */
  private static List<String> zeilen(String text) {
    List<String> zeilen = new ArrayList<>();
    int anfang = 0;
    int i = 0;
    while (i < text.length()) {
      char zeichen = text.charAt(i);
      if (zeichen == '\n') {
        zeilen.add(text.substring(anfang, i));
        i++;
        anfang = i;
      } else if (zeichen == '\r') {
        zeilen.add(text.substring(anfang, i));
        i++;
        if (i < text.length() && text.charAt(i) == '\n') {
          i++;
        }
        anfang = i;
      } else {
        i++;
      }
    }
    if (anfang < text.length()) {
      zeilen.add(text.substring(anfang));
    }
    return zeilen;
  }
}
