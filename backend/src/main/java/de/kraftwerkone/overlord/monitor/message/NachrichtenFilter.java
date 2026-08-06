package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Sortierrichtung;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.Zeitraum;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * @param langeSuche ob die Fenstergrenze der Suche bewusst aufgehoben wurde. Wirkt nur zusammen mit
 *     {@code suche} und hebt sie nur bis {@link #SUCHE_FENSTER_LANG}.
 * @param zwischenschritte ob {@code SPLITTED}/{@code MERGED} mitkommen
 * @param cursor Seitenposition oder {@code null} fuer die erste Seite
 */
public record NachrichtenFilter(
    Zeitfenster fenster,
    Set<MessageStatusKind> status,
    List<String> prozessIds,
    String suche,
    boolean langeSuche,
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

  /**
   * Die Fenstergrenze <b>bei gesetztem Suchbegriff</b> (Messung L13, 06.08.2026).
   *
   * <p>Der Freitextfilter ist der teuerste Fall dieses Endpunkts, und beide vorgesehenen Umbauten
   * sind gemessen und verworfen (M10 und L11). Was bleibt, ist eine Grenze — und zwar an der
   * <b>Spanne</b>, nicht am Modus: Praktisch beisst sie nur im {@code von}/{@code bis}-Modus, weil
   * {@code zeitraum} ohnehin nur bis 30 Tage reicht; das ist kein Grund, sie dort wegzulassen.
   *
   * <p><b>Warum eine Grenze auf das Fenster und nicht auf die Trefferzahl.</b> Teuer ist nicht der
   * breite Begriff, sondern der seltene: Ein Begriff, dessen Prozesse im Fenster viele Zeilen
   * tragen, fuellt die Seite sofort (2,3 ms). Ein Begriff, dessen Prozesse kaum Zeilen tragen,
   * zwingt MariaDB durch das ganze Fenster. Eine Grenze auf die Zahl der aufgeloesten Prozesse
   * bestrafte damit genau den schnellen Fall und liesse den langsamen durch.
   */
  public static final Duration SUCHE_FENSTER = Duration.ofDays(30);

  /**
   * Bis hierher — und nicht weiter — hebt {@code langeSuche=true} die Grenze auf.
   *
   * <p><b>Neunzig Tage, weil das die groesste gemessene Spanne mit Sicherheitsabstand ist</b>
   * (L13). Der schlimmste Fall ist der Begriff, der Stammdaten trifft, aber im Fenster keine
   * einzige Zeile: voller Durchlauf, leeres Ergebnis. Gemessen gegen die Testkopie kostet er 1,2 s
   * ueber 30 Tage, <b>3,9 s ueber 90 Tage</b>, 8,0 s ueber 180 Tage und 15,6 s ueber ein Jahr — der
   * Verlauf ist linear bei rund 173.000 Zeilen je Sekunde. Der Lese-Pool bricht bei 10 s ab ({@code
   * max_statement_time}, {@code docs/datenzugriff.md} §1): Ein Jahr <b>reisst</b> die Grenze, 180
   * Tage liegen mit 80 Prozent daneben und damit einen dichteren Tag davon entfernt. 90 Tage lassen
   * Faktor 2,5 Luft.
   *
   * <p>Das Maximum von einem Jahr aus Regel L1 bleibt darueber bestehen ({@code Zeitfenster}) — bei
   * gesetztem Suchbegriff greift es nur nie, weil diese Grenze frueher zieht.
   */
  public static final Duration SUCHE_FENSTER_LANG = Duration.ofDays(90);

  /** Die Vorgabe: Die Grenze steht. */
  public static final boolean LANGE_SUCHE_VORGABE = false;

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
      Boolean langeSuche,
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

    String begriff = suchbegriff(suche);
    boolean langes = langeSuche == null ? LANGE_SUCHE_VORGABE : langeSuche;
    pruefeSuchfenster(begriff, fenster, langes);

    return new NachrichtenFilter(
        fenster,
        einordnungen(status),
        werte(prozess),
        begriff,
        langes,
        zwischenschritte == null ? ZWISCHENSCHRITTE_VORGABE : zwischenschritte,
        sortierung == null ? Sortierrichtung.NEUESTE : Sortierrichtung.ausCode(sortierung),
        cursor == null ? null : Seitenposition.dekodiere(cursor).imFenster(fenster),
        seitengroesse(limit));
  }

  /**
   * Die Fenstergrenze der Suche — {@code 400} mit eigenem Problemtyp, wenn sie ueberschritten ist.
   *
   * <p><b>Ohne Suchbegriff greift sie nicht.</b> Ein Jahresfenster ohne Suche kostet dieselben 2,7
   * ms wie ein Tagesfenster (Messungen L1 bis L3); teuer wird erst die ODER-Bedingung ueber {@code
   * ProcessID} und {@code SOSID}, die den Zeitindex als einzigen Zugriffspfad uebrig laesst.
   *
   * <p><b>Die Antwort nennt beide Zahlen</b> — die geltende Grenze und die angefragte Spanne. Damit
   * kann die Oberflaeche eine konkrete Meldung samt Schaltflaeche „Trotzdem suchen" bauen, ohne die
   * Grenze ein zweites Mal zu kennen; steht sie nur im Backend, laeuft ihr kein Sprachtext
   * hinterher.
   */
  private static void pruefeSuchfenster(String suche, Zeitfenster fenster, boolean langeSuche) {
    if (suche == null) {
      return;
    }
    Duration grenze = langeSuche ? SUCHE_FENSTER_LANG : SUCHE_FENSTER;
    Duration spanne = fenster.spanne();
    if (spanne.compareTo(grenze) <= 0) {
      return;
    }
    long grenzeTage = Zeitfenster.tageAufgerundet(grenze);
    long angefragtTage = Zeitfenster.tageAufgerundet(spanne);
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "suche-fenster-zu-gross",
        "Zeitfenster fuer die Suche zu gross",
        "Die Suche ist auf "
            + grenzeTage
            + " Tage begrenzt; angefragt sind "
            + angefragtTage
            + ". Verkleinere das Zeitfenster"
            + (langeSuche ? "." : " oder suche ausdruecklich ueber den laengeren Zeitraum."),
        "Suchfenster ueber der Grenze: " + angefragtTage + " statt " + grenzeTage + " Tage",
        Map.of("grenzeTage", grenzeTage, "angefragtTage", angefragtTage));
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
