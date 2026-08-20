package de.kraftwerkone.overlord.monitor.catalog;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Die beiden Regeln, mit denen aus Prozess- und Projektkennungen ein Partner- und
 * Richtungsvorschlag entsteht — <b>reine Funktionen</b>: Zeichenketten hinein, ein Vorschlag oder
 * nichts heraus.
 *
 * <p><b>Warum diese Klasse nichts kennt.</b> Kein Datenbankzugriff, keine Spring-Bean, kein
 * Zustand. Die Regeln stehen unter offenen Punkten ({@code docs/prozess-katalog.md} §10) und werden
 * sich aendern; das darf keinen Service anfassen und keine Verbindung brauchen.
 *
 * <p><b>Sie schlaegt vor, sie entscheidet nicht.</b> Ein Vorschlag landet in einer Zeile mit Status
 * {@link Pflegestatus#OFFEN}. Was sie nicht sicher ableiten kann, laesst sie <b>leer</b> — Regel
 * Q4, es wird nichts geraten.
 *
 * <h2>Warum es zwei Regeln sind</h2>
 *
 * <p>M75 hat gemessen, ob die {@code ProcessID} mit {@code Ziffern} und Unterstrich beginnt: 100 %
 * bei drei Mandanten, 0 % bei sieben, kein Zwischenwert. Daraus folgt <i>nicht</i>, dass die
 * Ableitung fuer drei traegt und fuer sieben nicht — M76 zeigt beide Haelften als falsch: {@code
 * SUTTONS} hat den Praefix und keinen Partner im Prozessnamen; {@code IBIS}/{@code IBISGUS} haben
 * keinen Praefix und tragen Richtung <i>und</i> Partner. <b>Die Zweiteilung ist eine ueber
 * Zeichensetzung, nicht ueber Ableitbarkeit.</b>
 */
public final class Partnerheuristik {

  /**
   * Der Mandant, den Regel A namentlich ausnimmt.
   *
   * <p>Siebzehn Prozesse mit perfektem Muster, aus dem nichts zu holen ist: Bei {@code SUTTONS}
   * traegt der Prozessname einen <b>Vorgang</b> (Buchung, Status) und keinen Partner (M76). Die
   * Ausnahme steht namentlich und nicht als Musterverfeinerung, weil das Muster eben passt — nur
   * die Bedeutung dahinter nicht.
   */
  static final String AUSGENOMMENER_MANDANT = "SUTTONS";

  /** Der fuehrende Nummernpraefix, an dem Regel A ueberhaupt erst greift. */
  private static final Pattern NUMMERNPRAEFIX = Pattern.compile("^[0-9]+_");

  /**
   * Das Ankerwort in CamelCase, als <b>Wort</b> und nicht als Teilzeichenkette.
   *
   * <p>Die Vorgabe lautet „an Grossbuchstaben zerlegen, {@code Eingehend} oder {@code Ausgehend}
   * suchen". Genau das steht hier: Ein Grossbuchstabe <b>ist</b> in CamelCase der Anfang eines
   * Wortes, es braucht davor also keine weitere Bedingung. Gebraucht wird nur die Grenze
   * <b>dahinter</b> — {@code (?![a-z])} —, damit ein laengeres Wort wie {@code Eingehende} nicht
   * als Anker durchgeht: Dort gehoert das Folgezeichen noch zum selben Wort, und was dann „dahinter
   * steht", waere geraten.
   *
   * <p><b>Korrigiert am 20.08.2026, an den Daten.</b> Die erste Fassung verlangte zusaetzlich einen
   * Kleinbuchstaben oder einen Nicht-Buchstaben <i>vor</i> dem Anker. Das war eine Erfindung und
   * kostete <b>26</b> Prozesse: {@code IBIS} 20 und {@code IBISGUS} 6 tragen unmittelbar vor dem
   * Anker ein zweibuchstabiges Kuerzel in Grossschreibung (Gestalt {@code
   * <Wort><XX>Eingehend<PARTNER>}). Gemessen war der Befund eindeutig — von den verfehlten Namen
   * trugen <b>alle</b> den Anker in exakter CamelCase-Schreibweise, keiner in einer anderen.
   *
   * <p>Bewusst <b>fallunterscheidend</b>: Die gemessene Gestalt ist CamelCase ({@code
   * AuslagerungAusgehend} plus Partner, M76). Eine durchgaengig grossgeschriebene Schreibweise
   * waere ein anderer Fall und wird nicht mitgenommen — im Bestand kommt sie kein einziges Mal vor.
   */
  private static final Pattern ANKER = Pattern.compile("(Eingehend|Ausgehend)(?![a-z])");

  /** Ein fuehrender Trenner am Partnerkandidaten gehoert zur Zeichensetzung, nicht zum Namen. */
  private static final Pattern FUEHRENDER_TRENNER = Pattern.compile("^[_-]+");

  private Partnerheuristik() {}

  /**
   * Der Vorschlag zu einem Prozess.
   *
   * <p>Reihenfolge und Zusammenspiel der Regeln:
   *
   * <ol>
   *   <li><b>Regel A</b> liefert den Partner aus der Prozesskennung. Die Richtung kommt dann aus
   *       dem <b>Projektnamen</b> — bei {@code NEXANS} traegt genau der sie ({@code
   *       300_KundenEingehend}), waehrend die Prozesskennung den Partner traegt.
   *   <li><b>Regel B</b> liefert beides aus der Prozesskennung: was hinter dem Anker steht, ist der
   *       Partner, das Ankerwort selbst ist die Richtung.
   *   <li>Trifft keine der beiden, bleibt der Partner leer — die Richtung kann trotzdem aus dem
   *       Projektnamen kommen. Das ist der Fall der 224 {@code NEXANS}-Prozesse mit vier oder fuenf
   *       Unterstrichen: kein Partner, aber ein Projekt mit Richtungsanker.
   * </ol>
   *
   * @param mandantId der Mandant — gebraucht ausschliesslich fuer die namentliche Ausnahme
   * @param processId die Prozesskennung, Traeger des Partners
   * @param projectId die Projektkennung, Traeger der Richtung
   * @return immer ein Vorschlag, notfalls {@link Partnervorschlag#KEINER}
   */
  public static Partnervorschlag vorschlag(String mandantId, String processId, String projectId) {
    Optional<String> ausRegelA = regelA(mandantId, processId);
    if (ausRegelA.isPresent()) {
      return new Partnervorschlag(
          ausRegelA.get(), richtungAus(projectId).orElse(null), VorschlagHerkunft.REGEL_A);
    }
    Optional<Ankerfund> ausRegelB = anker(processId);
    if (ausRegelB.isPresent()) {
      Ankerfund fund = ausRegelB.get();
      // Steht hinter dem Anker nichts, gibt es eine Richtung und keinen Partner. Die Herkunft
      // beschreibt den Partner — also KEINE, obwohl der Anker getroffen hat.
      return new Partnervorschlag(
          fund.rest(),
          fund.richtung(),
          fund.rest() == null ? VorschlagHerkunft.KEINE : VorschlagHerkunft.REGEL_B);
    }
    return new Partnervorschlag(null, richtungAus(projectId).orElse(null), VorschlagHerkunft.KEINE);
  }

  /**
   * <b>Regel A — Praefix und Position.</b> Fuehrender Nummernpraefix, dann ist <b>Token 2 der
   * {@code ProcessID}</b> der Partnerkandidat ({@code 40000_AMG_LAB_VDA} liefert {@code AMG}).
   *
   * <p><b>Sie feuert bei genau zwei oder drei Unterstrichen</b>, gezaehlt ueber die volle {@code
   * ProcessID} einschliesslich des Praefix-Trenners. Bei vier und fuenf ist der Partnername
   * mehrteilig ({@code KE_OSTROV}, {@code DAS_DRAEXLMAIER}, {@code DELFINGEN_DE_HA}) und
   * unentscheidbar, wo er endet; bei einem gibt es kein Token 2.
   *
   * <p><b>Der Nummernpraefix ist Bedingung und nicht nur Namensgeber.</b> Ohne ihn faellt die Regel
   * aus, und das ist an den Zahlen belegt statt angenommen: {@code NXHBE} traegt bei 14 von 17
   * Prozessen genau zwei Unterstriche und {@code WOC} bei dreien zwei oder drei — beide Mandanten
   * fuehrt {@code docs/prozess-katalog.md} §3.5 aber vollstaendig unter „ohne Vorschlag", und beide
   * haben laut M75 keinen Praefix. Mit der Bedingung ergibt die Regel {@code NEXANS} 9+500 = 509
   * und {@code VOTG} 118+260 = 378, zusammen die dort ausgewiesenen <b>887</b>.
   */
  public static Optional<String> regelA(String mandantId, String processId) {
    if (processId == null || processId.isBlank()) {
      return Optional.empty();
    }
    if (AUSGENOMMENER_MANDANT.equalsIgnoreCase(mandantId)) {
      return Optional.empty();
    }
    if (!NUMMERNPRAEFIX.matcher(processId).find()) {
      return Optional.empty();
    }
    long unterstriche = processId.chars().filter(zeichen -> zeichen == '_').count();
    if (unterstriche < 2 || unterstriche > 3) {
      return Optional.empty();
    }
    String kandidat = processId.split("_", -1)[1].trim();
    return kandidat.isEmpty() ? Optional.empty() : Optional.of(kandidat);
  }

  /**
   * <b>Regel B — CamelCase mit Richtungsanker.</b> An {@code Eingehend} oder {@code Ausgehend}
   * geteilt: was dahinter steht, ist der Partner; das Ankerwort selbst ist die Richtung ({@code
   * AuslagerungAusgehendBAYER} liefert {@code BAYER}, ausgehend).
   *
   * <p><b>Fehlt der Anker, gibt es keinen Vorschlag</b> — weder Partner noch Richtung.
   *
   * <p>Bewusst <b>keine Bedingung „ohne Trennzeichen"</b>, obwohl die Regel fuer solche Namen
   * gedacht ist. Auch das ist an den Zahlen entschieden: {@code docs/prozess-katalog.md} §3.5
   * fuehrt fuer Regel B {@code IBIS} 192 und {@code IBISGUS} 89, waehrend M75 dort 188 bzw. 88
   * Prozesse <i>ohne</i> Unterstrich zaehlt — die restlichen fuenf tragen einen und sind
   * mitgezaehlt. Der Anker ist die Bedingung, nicht die Abwesenheit von Trennzeichen.
   *
   * <p>Bei mehreren Ankern gilt der <b>erste</b>: Was dahinter steht, ist der laengere und damit
   * vollstaendigere Kandidat.
   */
  static Optional<Ankerfund> anker(String kennung) {
    if (kennung == null || kennung.isBlank()) {
      return Optional.empty();
    }
    Matcher treffer = ANKER.matcher(kennung);
    if (!treffer.find()) {
      return Optional.empty();
    }
    Richtung richtung =
        "Eingehend".equals(treffer.group(1)) ? Richtung.EINGEHEND : Richtung.AUSGEHEND;
    String rest =
        FUEHRENDER_TRENNER.matcher(kennung.substring(treffer.end(1))).replaceFirst("").trim();
    return Optional.of(new Ankerfund(richtung, rest.isEmpty() ? null : rest));
  }

  /**
   * <b>Die Richtung aus einer Kennung</b> — derselbe Anker wie Regel B, angewandt auf die
   * Projektkennung ({@code 300_KundenEingehend}, {@code OrdersVerarbeitungEingehendNL}).
   *
   * <p>Er deckt rund 82 der etwa 140 Projekte ab ({@code docs/prozess-katalog.md} §3.4). <b>Der
   * {@code SOSName} wird nicht angefasst</b> — ausdruecklich verworfen (§9): Er braeuchte den Join
   * auf {@code SOS} und eine Konfliktregel fuer die acht Prozesse mit mehreren SOS. {@code VOTG}
   * bleibt damit der einzige Mandant, dessen Richtung von Hand kommt.
   */
  public static Optional<Richtung> richtungAus(String kennung) {
    return anker(kennung).map(Ankerfund::richtung);
  }

  /**
   * Ein Ankertreffer: die Richtung aus dem Ankerwort und das, was dahinter steht.
   *
   * @param richtung nie {@code null}
   * @param rest der Partnerkandidat, oder {@code null}, wenn hinter dem Anker nichts mehr steht
   */
  record Ankerfund(Richtung richtung, String rest) {}
}
