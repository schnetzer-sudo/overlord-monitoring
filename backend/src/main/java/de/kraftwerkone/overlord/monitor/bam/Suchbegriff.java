package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import org.springframework.http.HttpStatus;

/**
 * Ein einzelner Suchbegriff der BAM-Suche: ein Wert und <b>wahlweise</b> der BAM-Typ, unter dem er
 * stehen soll.
 *
 * <p><b>Die Form ist {@code <typ>:<wert>}, und der Doppelpunkt ist Pflicht.</b> Ohne Typ lautet der
 * Parameter {@code :4711815} — der Trenner steht auch dann da.
 *
 * <h2>Warum der Trenner Pflicht ist</h2>
 *
 * <p>Ohne ihn müsste die Anwendung raten, ob eine führende Ziffernfolge ein Typ ist oder Teil des
 * Werts. Beides kommt vor: Die BAM-Typen sind Zahlen ({@code 0}, {@code 1}, {@code 2}, {@code
 * 2000}, {@code 9018}), und die Werte sind es ebenfalls — M38 misst sie als rein numerisch bei fast
 * allen Typen.
 *
 * <p><b>Ob ein BAM-Wert selbst einen Doppelpunkt enthalten kann, ist nicht gemessen.</b> Mit
 * Pflichttrenner und Aufteilung am <b>ersten</b> Doppelpunkt ist die Frage gegenstandslos statt
 * nach Regel Q4 beantwortet: Alles hinter dem ersten Trenner ist Wert, einschließlich weiterer
 * Doppelpunkte.
 *
 * @param typ {@code MessageBAM.MessageBAMType} oder {@code null} für „unter jedem Typ". Die
 *     Typangabe ist <b>Ergebnisverfeinerung und keine Entlastung</b>: M36 misst +1,5 bis +4 Prozent
 *     Laufzeit, nicht weniger. Sie darf deshalb nirgends als Grund gelten, eine Grenze zu lockern.
 * @param wert der gesuchte Wert, an den Rändern beschnitten und mindestens ein Zeichen lang
 */
public record Suchbegriff(Short typ, String wert) {

  /** Der Pflichttrenner zwischen Typ und Wert. */
  public static final char TRENNER = ':';

  public Suchbegriff {
    if (wert == null || wert.isEmpty()) {
      throw new IllegalArgumentException("Ein Suchbegriff ohne Wert gibt es nicht");
    }
  }

  /** Ob dieser Begriff auf einen BAM-Typ eingeschränkt ist. */
  public boolean mitTyp() {
    return typ != null;
  }

  /**
   * Liest einen rohen Parameter {@code <typ>:<wert>}.
   *
   * <p><b>Geteilt wird am ersten Doppelpunkt</b>, nicht am letzten und nicht an jedem. Ein leerer
   * Typteil heißt „ohne Typ"; ein leerer Wertteil ergibt {@code null}, damit der Aufrufer die
   * Mindestbedingung an einer Stelle prüfen kann ({@link BamSuchfilter#aus}).
   *
   * @return der geprüfte Begriff, oder {@code null}, wenn nach dem Beschneiden kein Zeichen bleibt
   * @throws FachlicheAusnahme {@code 400}, wenn der Trenner fehlt oder der Typteil keine Zahl ist
   */
  public static Suchbegriff ausParameter(String roh) {
    int trenner = roh.indexOf(TRENNER);
    if (trenner < 0) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "suchbegriff-ohne-typtrenner",
          "Suchbegriff ohne Trenner",
          "Ein Suchbegriff hat die Form typ:wert — ohne Typ schreibe :wert.",
          "Suchbegriff ohne Pflichttrenner");
    }
    String typteil = roh.substring(0, trenner).strip();
    String wert = roh.substring(trenner + 1).strip();
    if (wert.isEmpty()) {
      return null;
    }
    return new Suchbegriff(typteil.isEmpty() ? null : typ(typteil), wert);
  }

  /**
   * <b>{@code MessageBAMType} ist {@code smallint(6)}</b>, gegen {@code information_schema} erhoben
   * und nicht übernommen (M46‑0, Regel L8). Ein Wert außerhalb des Bereichs ist deshalb keine Zahl,
   * die es geben kann — und wird abgewiesen, statt still zu überlaufen.
   */
  private static Short typ(String typteil) {
    try {
      return Short.valueOf(typteil);
    } catch (NumberFormatException ex) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "suchbegriff-typ-ungueltig",
          "Belegart unbekannt",
          "Vor dem Doppelpunkt steht die Nummer der Belegart — oder gar nichts.",
          "Typteil des Suchbegriffs ist keine gueltige Typnummer");
    }
  }
}
