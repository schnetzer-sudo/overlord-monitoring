package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Die geprüften Anfrageparameter der BAM-Suche. Alles, was hier ankommt, ist gültig — geprüft wird
 * einmal in {@link #aus} und nicht verstreut im Service.
 *
 * <p><b>Kein Feld für den Mandanten.</b> Er kommt ausschließlich aus der Sitzung (Regel M1); ein
 * Parameter dafür existiert nicht und darf nicht entstehen.
 *
 * @param begriffe mindestens einer, höchstens {@link #HOECHSTENS_BEGRIFFE}
 * @param fenster das Pflicht-Zeitfenster (Regel L1), Vorgabe {@link #FENSTER_VORGABE}
 * @param modus exakt oder über den Anfang des Werts, Vorgabe {@link Suchmodus#VORGABE}
 */
public record BamSuchfilter(List<Suchbegriff> begriffe, Zeitfenster fenster, Suchmodus modus) {

  /**
   * Wie viele Begriffe eine Suche höchstens trägt — <b>Schutzgeländer, keine fachliche Grenze</b>.
   *
   * <p>Fachlich wäre keine nötig: M42‑2 misst <b>0,094 ms</b> je zusätzlichem Begriff, und die
   * Reihe verhält sich beim kleinen Mandanten genauso (M42‑4). Was die Zahl begrenzt, ist die Zahl
   * der <b>Join-Reihenfolgen</b>, die der Optimierer durchprobiert: Jeder Begriff ist ein weiterer
   * Selbstjoin auf {@code MessageBAM}, und die Suche verlässt sich ausdrücklich darauf, dass der
   * Optimierer die Reihenfolge selbst wählt (kein {@code STRAIGHT_JOIN}, M42‑1).
   *
   * <p><b>Und sie ist über fünf hinaus nicht gemessen.</b> M42 endet bei fünf Begriffen; acht ist
   * die Zahl, die das Geländer trägt, und sie steht dort nicht. Deshalb ein Geländer und keine
   * Zusage.
   */
  public static final int HOECHSTENS_BEGRIFFE = 8;

  /**
   * Die Vorgabe des Zeitfensters: <b>30 Tage</b> statt der 24 Stunden aus Regel L1 — und die
   * Abweichung ist gemessen begründet.
   *
   * <p><b>Der Nutzer mit einer Belegnummer hat kein Datum.</b> Ein Tagesfenster findet beim
   * schlimmsten Wert der Erhebung <b>279 von 234.159</b> Nachrichten (M35) — es verändert also
   * nicht den Preis, sondern die <i>Antwort</i>, und zwar ohne dass der Nutzer es merkte. Deshalb
   * steht das tatsächlich verwendete Fenster in der Antwort.
   *
   * <p><b>Und kein offenes Fenster.</b> Dieselbe Messung: ohne Fenster kostet derselbe Wert
   * <b>10.752,8 ms</b> und reißt damit die Zeitgrenze des Lese-Pools (10 s, {@code
   * docs/datenzugriff.md} §1); ein Monat kostet <b>1.652,0 ms</b>, ein Jahr <b>8.664,4 ms</b> und
   * liegt damit bei 87 Prozent der Grenze. 30 Tage lassen Faktor 6 Luft.
   */
  public static final Duration FENSTER_VORGABE = Duration.ofDays(30);

  /**
   * Das Fenstermaximum <b>im Präfixmodus</b>: 30 Tage. Das Jahresfenster steht ihm nicht offen.
   *
   * <p><b>Die Zahl ist gemessen und war vor der Messung nicht bekannt.</b> M50 hat den schlimmsten
   * bekannten Präfix des Bestands — <b>1.332.180</b> Zeilen, hergeleitet und nicht abgeschrieben —
   * durch das gebaute Statement geschickt:
   *
   * <table border="1">
   *   <caption>M50, {@code NEXANS}, ein Begriff</caption>
   *   <tr><th>Fenster</th><th>Laufzeit</th></tr>
   *   <tr><td>30 Tage</td><td><b>3,851 s</b> — 38,5 % der Zeitgrenze des Lese-Pools</td></tr>
   *   <tr><td>ein Jahr</td><td><b>Abbruch an der 60‑Sekunden-Grenze</b></td></tr>
   * </table>
   *
   * <p>Die Lesart stand vor der Messung fest: unter 9 Sekunden erbt der Präfixmodus das Fenster der
   * exakten Suche, ab 9 Sekunden oder bei Abbruch ist er auf 30 Tage gedeckelt. <b>Der Abbruch ist
   * eingetreten und ist das Ergebnis</b> — er ist nicht wiederholt und die Grenze nicht ausgesetzt
   * worden.
   *
   * <p><b>Die exakte Suche behält ihr Jahresmaximum.</b> Sie ist ein anderer Zugriff: M47 misst für
   * sie im schlimmsten Fall 8,940 s über ein Jahr — knapp, aber durchgelaufen.
   *
   * <p>Numerisch ist die Zahl dieselbe wie {@link #FENSTER_VORGABE}, inhaltlich nicht: Die eine
   * sagt, was gilt, wenn niemand etwas nennt, die andere, wie weit jemand gehen darf. Sie stehen
   * deshalb getrennt und dürfen sich auseinander bewegen.
   */
  public static final Duration PRAEFIX_FENSTER_MAXIMUM = Duration.ofDays(30);

  public BamSuchfilter {
    begriffe = List.copyOf(begriffe);
  }

  /**
   * Baut den Filter aus den rohen Parametern und weist alles Unbrauchbare mit {@code 400} ab.
   *
   * <p><b>Es gibt keine Mindestlänge des Suchbegriffs</b>, obwohl Regel L5 sie nennt. Die
   * Begründung steht in {@code docs/bam-suche.md} und in zwei Messungen: E6 zeigt, dass nicht die
   * Zeichenlänge die Kostengröße ist, sondern die <b>Trefferzahl</b> — und M38, dass die Werte je
   * Typ zwischen <b>1 und 35</b> Zeichen lang sind. Eine feste Mindestlänge von vier machte neun
   * Typen unsuchbar, eine von drei ließe beim gemessenen Prüfwert 264.469 Treffer zu. Was trägt,
   * sind das Zeitfenster (M35) und das harte Limit.
   *
   * @param begriff die wiederholten {@code begriff}-Parameter, je {@code <typ>:<wert>}
   * @param modus {@code exakt} oder {@code praefix}; fehlt er, gilt {@link Suchmodus#VORGABE} und
   *     der Endpunkt verhält sich Zeichen für Zeichen wie vor Teil 4
   * @param anwendungsuhr die Anwendungsuhr — die Vorgabe wird <b>im Backend</b> aufgelöst (Regel
   *     Z1). Käme das Fenster aus der Browseruhr, wäre die Suche lokal immer leer.
   */
  public static BamSuchfilter aus(
      List<String> begriff, String von, String bis, String modus, Clock anwendungsuhr) {

    List<Suchbegriff> begriffe = begriffe(begriff);
    if (begriffe.isEmpty()) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "suchbegriff-fehlt",
          "Kein Suchbegriff",
          "Gib mindestens eine Belegnummer an, in der Form typ:wert oder :wert.",
          "Suche ohne einen einzigen brauchbaren Begriff");
    }
    if (begriffe.size() > HOECHSTENS_BEGRIFFE) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zu-viele-suchbegriffe",
          "Zu viele Suchbegriffe",
          "Es lassen sich höchstens " + HOECHSTENS_BEGRIFFE + " Belegnummern gleichzeitig suchen.",
          "Mehr als " + HOECHSTENS_BEGRIFFE + " Suchbegriffe angefragt");
    }

    Suchmodus gewaehlt = Suchmodus.ausParameter(modus);

    Zeitfenster fenster =
        Zeitfenster.mitVorgabe(
            von == null ? null : Zeitpunkte.ausIso(von, anwendungsuhr.getZone(), "von"),
            bis == null ? null : Zeitpunkte.ausIso(bis, anwendungsuhr.getZone(), "bis"),
            FENSTER_VORGABE,
            anwendungsuhr);
    pruefePraefixfenster(gewaehlt, fenster);

    return new BamSuchfilter(begriffe, fenster, gewaehlt);
  }

  /**
   * Der Deckel des Präfixmodus — {@code 400} mit eigenem Problemtyp, wenn er überschritten ist.
   *
   * <p><b>Es wird nichts gekappt.</b> Ein stillschweigend verkleinertes Fenster wäre hier besonders
   * schlecht: Das Fenster verändert bei dieser Suche nicht den Preis, sondern die <i>Antwort</i>
   * (M35, 279 von 234.159). Wer ein Jahr anfragt und dreißig Tage bekommt, ohne es zu erfahren,
   * hält das Gefundene für alles, was es gibt.
   *
   * <p><b>Die Antwort nennt beide Zahlen</b> — die geltende Grenze und die angefragte Spanne. Damit
   * kann eine Oberfläche eine konkrete Meldung bauen, ohne die Grenze ein zweites Mal zu kennen;
   * dieselbe Bauform wie bei {@code suche-fenster-zu-gross} in der Nachrichtenliste.
   *
   * <p><b>Die exakte Suche bleibt unberührt.</b> Ihr Maximum ist das Jahr aus Regel L1, und dieser
   * Zweig läuft für sie nie.
   */
  private static void pruefePraefixfenster(Suchmodus modus, Zeitfenster fenster) {
    if (modus != Suchmodus.PRAEFIX || fenster.spanne().compareTo(PRAEFIX_FENSTER_MAXIMUM) <= 0) {
      return;
    }
    long grenzeTage = Zeitfenster.tageAufgerundet(PRAEFIX_FENSTER_MAXIMUM);
    long angefragtTage = Zeitfenster.tageAufgerundet(fenster.spanne());
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "praefixsuche-fenster-zu-gross",
        "Zeitfenster für die Suche über den Anfang zu groß",
        "Die Suche über den Anfang einer Belegnummer ist auf "
            + grenzeTage
            + " Tage begrenzt; angefragt sind "
            + angefragtTage
            + ". Verkleinere das Zeitfenster oder suche die vollständige Nummer.",
        "Praefixsuche ueber der Fenstergrenze: " + angefragtTage + " statt " + grenzeTage + " Tage",
        Map.of("grenzeTage", grenzeTage, "angefragtTage", angefragtTage));
  }

  /**
   * Die Begriffe in der Reihenfolge der Anfrage.
   *
   * <p><b>Ein vollständig leerer Parameter fällt weg</b> — {@code ?begriff=} ist keine Suche nach
   * dem leeren Wert, genauso wie {@code ?prozess=} in der Nachrichtenliste kein Filter auf den
   * leeren Namen ist. Ein Parameter <i>mit</i> Trenner, dessen Wertteil leer bleibt, fällt aus
   * demselben Grund weg; bleibt am Ende keiner übrig, ist das {@code 400} und keine leere Antwort.
   *
   * <p><b>Die Reihenfolge bleibt, wie der Nutzer sie getippt hat</b>, und das ist folgenlos für die
   * Leistung: Der Optimierer steigt in jeder gemessenen Konstellation über den <i>seltensten</i>
   * Begriff ein und tauscht die Tabellen selbst (M42‑1). Genau deshalb steht in der Abfrage kein
   * {@code STRAIGHT_JOIN} — mit ihm würde die Eingabereihenfolge zur Leistungsfrage (Faktor 219 bis
   * 1.094).
   */
  private static List<Suchbegriff> begriffe(List<String> roh) {
    if (roh == null) {
      return List.of();
    }
    List<Suchbegriff> begriffe = new ArrayList<>();
    for (String eintrag : roh) {
      if (eintrag == null || eintrag.isBlank()) {
        continue;
      }
      Suchbegriff begriff = Suchbegriff.ausParameter(eintrag);
      if (begriff != null) {
        begriffe.add(begriff);
      }
    }
    return List.copyOf(begriffe);
  }
}
