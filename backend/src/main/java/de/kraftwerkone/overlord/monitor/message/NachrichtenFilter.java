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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
 * @param ueberfaellig nur ueberfaellige Nachrichten (E-j). <b>Kein Filter, sondern eine zweite
 *     Abfrageform</b> — Begruendung in {@code docs/nachrichtenliste.md} §5b
 * @param suche Freitext oder {@code null}; die Aufloesung zu IDs macht das Repository
 * @param langeSuche ob die Fenstergrenze der Suche bewusst aufgehoben wurde. Wirkt nur zusammen mit
 *     {@code suche} und hebt sie nur bis {@link #SUCHE_FENSTER_LANG}.
 * @param cursor Seitenposition oder {@code null} fuer die erste Seite
 */
public record NachrichtenFilter(
    Zeitfenster fenster,
    Set<MessageStatusKind> status,
    List<String> prozessIds,
    boolean ueberfaellig,
    String suche,
    boolean langeSuche,
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

  /*
   * Hier stand bis zum 11.08.2026 `ZWISCHENSCHRITTE_VORGABE = false` — die Vorgabe des
   * Ausblende-Schalters. Sie ist ersatzlos entfallen (docs/nachrichtenliste.md §5): Gemessen hat
   * ein Stellungspraedikat bei `IBISGUS` 100 Prozent und bei `ZAST` 92,93 Prozent aller Zeilen
   * ausgeblendet (M28-1), und die alte Statusvorgabe versteckte ausgerechnet die Zeile, die die
   * Belegnummer traegt (M26: 96,9 gegen 2,4 Prozent). **Die Liste filtert nicht mehr nach Status,
   * ausser der Nutzer sagt es ausdruecklich.**
   */

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

  /** Die Vorgabe von {@code ueberfaellig}: aus. Die Liste zeigt jede Zeile des Fensters. */
  public static final boolean UEBERFAELLIG_VORGABE = false;

  /**
   * Die Einordnungen, die ueberhaupt ueberfaellig werden koennen — {@link
   * MessageStatusKind#WARTEND} und {@link MessageStatusKind#LAEUFT}.
   *
   * <p><b>Aufgezaehlt und nicht ueber {@code MessageStatusClassifier.istEndstatus} gezogen</b>, und
   * das ist hier bewusst anders als in {@code MessageStatusClassifier.offeneRohwerte()}: Diese
   * Klasse ist ein reiner Wertetyp ohne Spring-Bean im Ruecken, und die Pruefung unten braucht die
   * Menge, bevor irgendein Dienst beteiligt ist. Damit die beiden nicht auseinanderlaufen, haelt
   * {@code NachrichtenFilterTest} sie gegeneinander.
   */
  static final Set<MessageStatusKind> UEBERFAELLIG_MOEGLICH =
      Set.of(MessageStatusKind.WARTEND, MessageStatusKind.LAEUFT);

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
      Boolean ueberfaellig,
      String suche,
      Boolean langeSuche,
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

    Set<MessageStatusKind> gewaehlt = einordnungen(status);
    boolean nurUeberfaellige = ueberfaellig == null ? UEBERFAELLIG_VORGABE : ueberfaellig;
    pruefeUeberfaelligMitStatus(nurUeberfaellige, gewaehlt);

    return new NachrichtenFilter(
        fenster,
        gewaehlt,
        werte(prozess),
        nurUeberfaellige,
        begriff,
        langes,
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

  /**
   * Die eine unvereinbare Kombination — {@code 400} mit eigenem Problemtyp.
   *
   * <p><b>Ueberfaellig sein kann nur, was nicht in einem Endstatus steht</b> ({@code
   * MessageStatusClassifier.istUeberfaellig}), also allein {@link MessageStatusKind#WARTEND} und
   * {@link MessageStatusKind#LAEUFT}. Waehlt jemand {@code ueberfaellig=true} zusammen mit einem
   * Statusfilter, der keine dieser beiden enthaelt, ist die Antwort <b>ohne Ruecksicht auf die
   * Daten</b> leer.
   *
   * <p><b>Und genau deshalb ist eine leere Liste hier die falsche Antwort.</b> Sie hiesse „in
   * diesem Zeitfenster gibt es nichts" — eine Auskunft ueber den Bestand. Wahr ist etwas anderes:
   * Die Frage widerspricht sich selbst, und kein Zeitfenster der Welt aendert daran etwas. Das ist
   * derselbe Fall wie „Suchbegriff zu kurz": Die Anfrage ist so, wie sie gestellt wurde, nicht
   * beantwortbar, und was sich aendern muss, ist die Anfrage (Richtlinie §5.5).
   *
   * <p><b>Ohne Statusfilter greift die Pruefung nicht.</b> Eine leere Auswahl heisst „alle" und
   * enthaelt damit auch die offenen Status — {@code ueberfaellig=true} allein ist der Normalfall
   * und der einzige, den die Oberflaeche heute erzeugt.
   *
   * <p>Die Antwort nennt die zulaessigen Werte, damit die Oberflaeche nicht dieselbe Menge ein
   * zweites Mal kennen muss.
   */
  private static void pruefeUeberfaelligMitStatus(
      boolean ueberfaellig, Set<MessageStatusKind> status) {
    if (!ueberfaellig || status.isEmpty() || !Collections.disjoint(status, UEBERFAELLIG_MOEGLICH)) {
      return;
    }
    String moeglich =
        UEBERFAELLIG_MOEGLICH.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "ueberfaellig-und-status-unvereinbar",
        "Ueberfaellig und Statusfilter passen nicht zusammen",
        "Ueberfaellig kann nur sein, was noch laeuft oder wartet. Waehle einen dieser Status ("
            + moeglich
            + ") oder nimm den Statusfilter heraus.",
        "ueberfaellig=true mit einem Statusfilter ohne offenen Status",
        Map.of("moeglicheStatus", moeglich));
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
