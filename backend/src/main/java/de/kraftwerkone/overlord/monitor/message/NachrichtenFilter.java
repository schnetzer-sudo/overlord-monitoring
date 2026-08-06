package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Sortierrichtung;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.Zeitraum;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * Die geprueften Anfrageparameter der Nachrichtenliste. Alles, was hier ankommt, ist gueltig — die
 * Pruefung passiert einmal in {@link #aus} und nicht verstreut im Service.
 *
 * <p><b>Kein Feld fuer den Mandanten.</b> Er kommt ausschliesslich aus der Sitzung (Regel M1); ein
 * Parameter dafuer existiert nicht und darf nicht entstehen.
 *
 * @param fenster das Pflicht-Zeitfenster (Regel L1)
 * @param status leere Menge heisst „alle"
 * @param prozessIds ausdruecklich gewaehlte {@code ProcessID}s, leer heisst „alle"
 * @param suche Freitext oder {@code null}; die Aufloesung zu IDs macht das Repository
 * @param zwischenschritte ob {@code SPLITTED}/{@code MERGED} mitkommen
 * @param cursor Seitenposition oder {@code null} fuer die erste Seite
 */
public record NachrichtenFilter(
    Zeitfenster fenster,
    Set<MessageStatusKind> status,
    List<String> prozessIds,
    String suche,
    boolean zwischenschritte,
    Sortierrichtung sortierung,
    Seitenposition cursor,
    int limit) {

  public static final int LIMIT_VORGABE = 50;

  /**
   * Hartes Maximum der Seitengroesse. Es ist zugleich die Obergrenze der {@code IN}-Liste, mit der
   * die BAM-Werte einer Seite nachgeladen werden.
   */
  public static final int LIMIT_MAXIMUM = 200;

  /**
   * Mindestlaenge des Suchbegriffs (Regel L5). Kuerzer waere kein Filter, sondern ein Durchlauf
   * durch alle Prozess- und Ablaufnamen des Mandanten.
   */
  public static final int SUCHE_MINDESTLAENGE = 3;

  /**
   * Wie viele Prozesse beziehungsweise Ablaeufe ein Suchbegriff hoechstens treffen darf.
   *
   * <p>Trifft er mehr, wird <b>nicht</b> abgeschnitten: Eine gekuerzte Trefferliste sieht aus wie
   * ein vollstaendiges Ergebnis, und der Nutzer sucht anschliessend in einem Ausschnitt, von dem er
   * nichts weiss. Stattdessen die Bitte, den Begriff zu verengen. Der Wert liegt deutlich ueber
   * dem, was ein sinnvoller Begriff trifft — {@code Process} hat 1.490 Zeilen, {@code SOS} 1.818.
   */
  public static final int SUCHE_HOECHSTENS_TREFFER = 200;

  /** Die Vorgabe: Zwischenschritte bleiben draussen. */
  public static final boolean ZWISCHENSCHRITTE_VORGABE = false;

  public NachrichtenFilter {
    status = Set.copyOf(status);
    prozessIds = List.copyOf(prozessIds);
  }

  /**
   * Baut den Filter aus den rohen Parametern und weist alles Unbrauchbare mit {@code 400} ab.
   *
   * @param anwendungsuhr die Anwendungsuhr — relative Zeitraeume werden <b>im Backend</b>
   *     aufgeloest (Regel Z1). Kaeme das Fenster aus der Browseruhr, waere die Liste lokal immer
   *     leer.
   */
  public static NachrichtenFilter aus(
      String zeitraum,
      String von,
      String bis,
      List<String> status,
      List<String> prozess,
      String suche,
      Boolean zwischenschritte,
      String sortierung,
      String cursor,
      Integer limit,
      Clock anwendungsuhr) {

    Zeitfenster fenster =
        Zeitfenster.aufloesen(
            zeitraum == null ? null : Zeitraum.ausCode(zeitraum),
            von == null ? null : Zeitpunkte.ausIso(von, anwendungsuhr.getZone(), "von"),
            bis == null ? null : Zeitpunkte.ausIso(bis, anwendungsuhr.getZone(), "bis"),
            anwendungsuhr);

    return new NachrichtenFilter(
        fenster,
        einordnungen(status),
        werte(prozess),
        suchbegriff(suche),
        zwischenschritte == null ? ZWISCHENSCHRITTE_VORGABE : zwischenschritte,
        sortierung == null ? Sortierrichtung.NEUESTE : Sortierrichtung.ausCode(sortierung),
        cursor == null ? null : Seitenposition.dekodiere(cursor).imFenster(fenster),
        seitengroesse(limit));
  }

  private static Set<MessageStatusKind> einordnungen(List<String> status) {
    Set<MessageStatusKind> gewaehlt = new LinkedHashSet<>();
    for (String wert : werte(status)) {
      try {
        gewaehlt.add(MessageStatusKind.valueOf(wert.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ex) {
        throw new FachlicheAusnahme(
            HttpStatus.BAD_REQUEST,
            "status-unbekannt",
            "Status unbekannt",
            "Der Statusfilter kennt diesen Wert nicht.",
            "Unbekannte Einordnung im Statusfilter");
      }
    }
    return gewaehlt;
  }

  /** Leere und blanke Werte fallen weg — {@code ?prozess=} ist kein Filter auf den leeren Namen. */
  private static List<String> werte(List<String> roh) {
    if (roh == null) {
      return List.of();
    }
    List<String> gefiltert = new ArrayList<>();
    for (String wert : roh) {
      if (wert != null && !wert.isBlank()) {
        gefiltert.add(wert.trim());
      }
    }
    return List.copyOf(gefiltert);
  }

  private static String suchbegriff(String suche) {
    if (suche == null || suche.isBlank()) {
      return null;
    }
    String begriff = suche.trim();
    if (begriff.length() < SUCHE_MINDESTLAENGE) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "suchbegriff-zu-kurz",
          "Suchbegriff zu kurz",
          "Der Suchbegriff braucht mindestens " + SUCHE_MINDESTLAENGE + " Zeichen.",
          "Suchbegriff unter der Mindestlaenge");
    }
    return begriff;
  }

  private static int seitengroesse(Integer limit) {
    if (limit == null) {
      return LIMIT_VORGABE;
    }
    if (limit < 1 || limit > LIMIT_MAXIMUM) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "limit-ungueltig",
          "Seitengroesse ungueltig",
          "Die Seitengroesse muss zwischen 1 und " + LIMIT_MAXIMUM + " liegen.",
          "Seitengroesse ausserhalb des zulaessigen Bereichs");
    }
    return limit;
  }
}
