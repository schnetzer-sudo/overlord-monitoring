package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
 */
public record BamSuchfilter(List<Suchbegriff> begriffe, Zeitfenster fenster) {

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
   * @param anwendungsuhr die Anwendungsuhr — die Vorgabe wird <b>im Backend</b> aufgelöst (Regel
   *     Z1). Käme das Fenster aus der Browseruhr, wäre die Suche lokal immer leer.
   */
  public static BamSuchfilter aus(
      List<String> begriff, String von, String bis, Clock anwendungsuhr) {

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

    Zeitfenster fenster =
        Zeitfenster.mitVorgabe(
            von == null ? null : Zeitpunkte.ausIso(von, anwendungsuhr.getZone(), "von"),
            bis == null ? null : Zeitpunkte.ausIso(bis, anwendungsuhr.getZone(), "bis"),
            FENSTER_VORGABE,
            anwendungsuhr);

    return new BamSuchfilter(begriffe, fenster);
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
