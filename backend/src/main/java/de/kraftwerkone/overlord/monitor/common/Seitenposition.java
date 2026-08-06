package de.kraftwerkone.overlord.monitor.common;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.springframework.http.HttpStatus;

/**
 * Die Position, ab der die naechste Seite gelesen wird — der Inhalt des <b>Cursors</b> aus Regel
 * L3. Nie {@code OFFSET}: Bei Millionen Zeilen wird das Ueberspringen linear teurer, waehrend ein
 * Cursor ueber den Sortierschluessel einen Indexbereich ergibt.
 *
 * <p><b>Der Cursor ist undurchsichtig.</b> Nach aussen ist er Base64URL und wird als Ganzes
 * zurueckgeschickt; das Format bleibt damit aenderbar, ohne einen Aufrufer zu brechen. Das Backend
 * verlaesst sich <b>nicht</b> darauf, dass er unveraendert zurueckkommt: Was hereinkommt, wird
 * geprueft, und Unlesbares ist {@code 400} statt stillschweigend „von vorne".
 *
 * <p>Er ist bewusst <b>nicht signiert</b>. Er traegt keine Berechtigung: Der Mandantenfilter steht
 * im Statement (Regel M3), und ein manipulierter Cursor verschiebt hoechstens die eigene
 * Seitenposition innerhalb des eigenen Datenausschnitts.
 *
 * <p><b>Er gehoert nicht in die geteilte URL.</b> Filter und Zeitfenster ja, die Seitenposition
 * nein — ein Link auf Seite sieben eines relativen Fensters zeigt beim Empfaenger auf andere
 * Zeilen.
 */
public record Seitenposition(LocalDateTime zeitpunkt, String id) {

  /**
   * Obergrenze fuer den kodierten Wert. Ein Cursor besteht aus einem Zeitstempel und einer {@code
   * varchar(36)}-Kennung; alles jenseits davon ist keine gekuerzte Eingabe, sondern eine fremde.
   */
  private static final int HOECHSTLAENGE = 200;

  private static final char TRENNER = '|';

  public Seitenposition {
    if (zeitpunkt == null || id == null || id.isBlank()) {
      throw new IllegalArgumentException(
          "Eine Seitenposition ohne Zeitpunkt und Kennung gibt es" + " nicht");
    }
  }

  /** Base64URL ohne Auffuellzeichen — damit der Wert ohne Kodierung in eine URL passt. */
  public String kodiere() {
    String klartext = zeitpunkt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + TRENNER + id;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(klartext.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * @throws FachlicheAusnahme {@code 400}, wenn der Cursor nicht lesbar ist. <b>Nicht</b>
   *     stillschweigend ignoriert: Ein ignorierter Cursor liefert wortlos wieder die erste Seite,
   *     und der Aufrufer blaettert endlos im Kreis, ohne dass jemand den Fehler bemerkt.
   */
  public static Seitenposition dekodiere(String cursor) {
    if (cursor == null || cursor.isBlank() || cursor.length() > HOECHSTLAENGE) {
      throw ungueltig();
    }
    String klartext;
    try {
      klartext = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      throw ungueltig();
    }
    int trenner = klartext.indexOf(TRENNER);
    if (trenner <= 0 || trenner == klartext.length() - 1) {
      throw ungueltig();
    }
    try {
      return new Seitenposition(
          LocalDateTime.parse(
              klartext.substring(0, trenner), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
          klartext.substring(trenner + 1));
    } catch (RuntimeException ex) {
      throw ungueltig();
    }
  }

  private static FachlicheAusnahme ungueltig() {
    return new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "cursor-ungueltig",
        "Seitenposition ungueltig",
        "Die Seitenposition ist nicht mehr gueltig. Rufe die Liste erneut auf.",
        "Cursor nicht lesbar oder ausserhalb des Zeitfensters");
  }

  /**
   * Ein Cursor, dessen Zeitpunkt ausserhalb des Fensters liegt, gehoert zu einer anderen Anfrage —
   * etwa weil der Nutzer das Zeitfenster gewechselt und den alten Cursor mitgeschickt hat. Auch das
   * ist {@code 400} und mit derselben Antwort wie ein unlesbarer Cursor: Beide bedeuten „hol dir
   * die Liste neu".
   */
  public Seitenposition imFenster(Zeitfenster fenster) {
    if (!fenster.enthaelt(zeitpunkt)) {
      throw ungueltig();
    }
    return this;
  }
}
