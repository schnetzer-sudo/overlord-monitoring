package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.Katalogzuordnung;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Setzt den Prozessbaum zusammen — <b>aus zwei Lesevorgaengen, und die Gliederung entsteht hier und
 * nicht in SQL</b>.
 *
 * <h2>Warum die Gruppierung in Java steht</h2>
 *
 * <p>Der Baum ist eine <b>Schachtelung</b> und keine Gruppierung: Partner → Richtung → Prozess, mit
 * Summen auf jeder Ebene. In SQL waeren das entweder drei Abfragen mit drei Bereichszugriffen auf
 * dieselben Rollupzeilen oder ein {@code ROLLUP}, dessen Zwischenzeilen der Aufrufer erst wieder
 * auseinandersortieren muesste. Hier stehen die Zeilen ohnehin schon im Speicher — die Schachtelung
 * kostet einen Durchlauf ueber hoechstens 733 Zeilen.
 *
 * <h2>Ein Uhrenschlag je Anfrage</h2>
 *
 * <p>{@code jetzt} kommt genau einmal aus der <b>Anwendungsuhr</b> (Regel Z1) und wird danach
 * herumgereicht — an das Zeitfenster <b>und</b> an die Stilleschwelle. Zwei Schlaege waeren zwei
 * Stichtage, und ein Prozess koennte im selben Aufruf im Fenster liegen und trotzdem gegen einen
 * anderen Stichtag als still gelten.
 *
 * <h2>Die Einordnung wird gerufen, nicht nachgebaut</h2>
 *
 * <p>Im Rollup steht der <b>Rohwert</b> (Entscheidung E-g). Ob eine Zeile Fehler ist, entscheidet
 * {@link MessageStatusClassifier#einordnung(String)} — dieselbe Stelle, die Liste, Dashboard und
 * spaeter der Chatbot benutzen. <b>Ein zweites {@code switch} ueber Statuswoerter darf in diesem
 * Paket nicht entstehen:</b> Es liefe beim naechsten neuen Statuswert von der Liste weg, und der
 * Unterschied fiele erst auf, wenn jemand zwei Zahlen nebeneinanderlegt.
 */
@Service
public class ProzessbaumService {

  /**
   * <b>Ab wann ein Prozess als still gilt — die fachliche Festlegung dieses Schritts.</b>
   *
   * <p>Drei Monate, und die Zahl steht hier als benannter Wert und nicht als Zahl in einem
   * Statement: Sie ist eine fachliche Festlegung und wird sich aendern.
   *
   * <p><b>Sie gilt unabhaengig vom gewaehlten Zeitfenster.</b> Waehlt jemand 48 Stunden, heisst
   * „still" trotzdem <i>seit drei Monaten nichts</i>. Eine fensterabhaengige Schwelle markierte bei
   * 48 Stunden den Normalfall — bei {@code NEXANS} traegt in einem 48-Stunden-Fenster nur ein
   * Bruchteil der 733 Prozesse Verkehr, und 733 Markierungen sind keine Markierung mehr.
   *
   * <p><b>Warum nicht kuerzer:</b> Gemessen am Stichtag der Testkopie sind bei drei Monaten 28 von
   * 733 Prozessen ({@code NEXANS}) still — 3,8 %. Das ist eine Menge, die jemand durchsieht.
   *
   * <p><b>Warum ein {@link Period} und keine {@code Duration}:</b> Drei Monate sind keine feste
   * Zahl von Tagen. {@code Duration.ofDays(90)} waere eine Naeherung mit einem Fehler von bis zu
   * zwei Tagen, und die Schwelle traegt eine fachliche Aussage und keine Rechnung.
   */
  static final Period STILLE_SCHWELLE = Period.ofMonths(3);

  /**
   * Das Paar, das ohne Parameter gilt.
   *
   * <p><b>Der Endpunkt waehlt es nicht selbst</b>, anders als das Dashboard. Dessen Belegungsprobe
   * gibt es hier bewusst nicht, und der Grund ist die Bauform der Ansicht: Das Dashboard zeigt ein
   * <b>Diagramm</b>, und ein Diagramm mit ueberwiegend leeren Eimern zeigt nichts. Der Baum zeigt
   * <b>alle Prozesse des Mandanten</b>, unabhaengig vom Fenster — er ist auch bei einem leeren
   * Fenster vollstaendig, nur stehen dann ueberall Nullen. Eine Probe, die ein Fenster sucht, in
   * dem etwas passiert ist, loeste hier ein Problem, das die Ansicht nicht hat, und kostete dafuer
   * eine bis drei zusaetzliche Abfragen je Aufruf.
   */
  static final Rollupzeitraum VORGABE = Rollupzeitraum.STUNDEN_48;

  private final ProzessbaumRepository prozessbaumRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  ProzessbaumService(
      ProzessbaumRepository prozessbaumRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.prozessbaumRepository = prozessbaumRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
  }

  /**
   * Der ganze Baum fuer den aktiven Mandanten.
   *
   * @param mandant Regel M2 — erster Pflichtparameter, und er kommt aus der Sitzung (Regel M1)
   * @param gewaehlt das Paar aus der URL, oder {@code null} fuer {@link #VORGABE}
   */
  public ProzessbaumResponse baum(MandantContext mandant, Rollupzeitraum gewaehlt) {
    LocalDateTime jetzt = LocalDateTime.now(anwendungsuhr);
    Rollupzeitraum zeitraum = gewaehlt == null ? VORGABE : gewaehlt;
    Zeitfenster fenster = zeitraum.fenster(jetzt);

    List<Prozessgeruestzeile> geruest = prozessbaumRepository.geruest(mandant);
    Map<String, Kennzahl> jeProzess =
        kennzahlenJeProzess(prozessbaumRepository.kennzahlen(mandant, zeitraum, fenster));

    return new ProzessbaumResponse(
        zeitraum.code(),
        new ZeitfensterResponse(
            Zeitpunkte.nachUtc(fenster.von(), anwendungsuhr.getZone()),
            Zeitpunkte.nachUtc(fenster.bis(), anwendungsuhr.getZone())),
        (int) STILLE_SCHWELLE.toTotalMonths(),
        gesamt(geruest, jeProzess, jetzt),
        partnerknoten(geruest, jeProzess, jetzt));
  }

  /**
   * Verdichtet die Zeilen der Kennzahlenabfrage auf eine Zahl je Prozess.
   *
   * <p>Aus <i>Prozess und Rohstatus</i> wird hier <i>Prozess</i>: Was Fehler ist, entscheidet der
   * Klassifizierer am Rohwert — <b>ein Lesevorgang, zwei Zahlen</b>.
   */
  private Map<String, Kennzahl> kennzahlenJeProzess(List<Prozesskennzahlzeile> zeilen) {
    Map<String, Kennzahl> jeProzess = new HashMap<>();
    for (Prozesskennzahlzeile zeile : zeilen) {
      boolean fehler =
          statusClassifier.einordnung(zeile.messageStatus()) == MessageStatusKind.FEHLER;
      jeProzess.merge(
          zeile.processId(),
          new Kennzahl(zeile.anzahl(), fehler ? zeile.anzahl() : 0),
          Kennzahl::plus);
    }
    return jeProzess;
  }

  /**
   * Die oberste Ebene, aufgebaut in zwei Schritten: erst schachteln, dann sortieren.
   *
   * <p><b>Gruppiert wird ueber {@link Katalogzuordnung#schluessel(String, String)}</b> und nicht
   * ueber den Rohwert. Der Unterschied ist Entscheidung E-i und keine Feinheit: Ein
   * <i>Regelvorschlag</i> der Heuristik steht mit {@code pflegestatus = OFFEN} in derselben Spalte
   * wie eine kuratierte Zuordnung. Gruppierte der Baum darueber, stuenden Prozesse unter einem
   * Partner, den niemand bestaetigt hat — und der Nutzer saehe der Gruppe nicht an, dass sie
   * geraten ist (Regel Q4).
   *
   * <p><b>Sortiert wird alphabetisch und nicht nach Volumen.</b> Ein Baum ist zum <i>Finden</i> da:
   * Wer wissen will, ob von einem bestimmten Partner etwas kam, sucht dessen Namen. Die Rangfolge
   * nach Volumen beantwortet eine andere Frage, und die beantwortet der Verteilungsblock des
   * Dashboards. Bei {@code NEXANS} stehen 154 Partner nebeneinander — in einer Volumenordnung waere
   * der gesuchte nirgends.
   */
  private List<PartnerknotenResponse> partnerknoten(
      List<Prozessgeruestzeile> geruest, Map<String, Kennzahl> jeProzess, LocalDateTime jetzt) {
    // Zwei Ebenen Schachtelung, Einfuegereihenfolge erhalten: Die Blaetter kommen bereits nach
    // ProcessName sortiert aus der Abfrage und behalten diese Reihenfolge.
    Map<String, Map<String, List<ProzessknotenResponse>>> geschachtelt = new LinkedHashMap<>();
    for (Prozessgeruestzeile zeile : geruest) {
      String partner = Katalogzuordnung.schluessel(zeile.pflegestatus(), zeile.partner());
      String richtung = Katalogzuordnung.schluessel(zeile.pflegestatus(), zeile.richtung());
      geschachtelt
          .computeIfAbsent(partner, schluessel -> new LinkedHashMap<>())
          .computeIfAbsent(richtung, schluessel -> new ArrayList<>())
          .add(blatt(zeile, jeProzess, jetzt));
    }

    List<PartnerknotenResponse> knoten = new ArrayList<>(geschachtelt.size());
    for (Map.Entry<String, Map<String, List<ProzessknotenResponse>>> eintrag :
        geschachtelt.entrySet()) {
      List<RichtungsknotenResponse> richtungen = richtungsknoten(eintrag.getValue());
      knoten.add(
          new PartnerknotenResponse(
              eintrag.getKey(),
              richtungen.stream().mapToInt(RichtungsknotenResponse::anzahlProzesse).sum(),
              richtungen.stream().mapToLong(RichtungsknotenResponse::nachrichten).sum(),
              richtungen.stream().mapToLong(RichtungsknotenResponse::fehler).sum(),
              richtungen));
    }
    knoten.sort(
        Comparator.comparing(
            PartnerknotenResponse::partner, Comparator.nullsLast(Comparator.naturalOrder())));
    return List.copyOf(knoten);
  }

  /**
   * Die mittlere Ebene. <b>Nur die Richtungen, die vorkommen</b> — kein leerer Ast.
   *
   * <p>Die Reihenfolge ist die der Aufzaehlung {@link Richtung} und danach „nicht ermittelt". Sie
   * ist damit ueber alle Partner dieselbe; eine Oberflaeche, die Symbole nach Position vergibt,
   * bekaeme sonst unter jedem Partner eine andere.
   */
  private static List<RichtungsknotenResponse> richtungsknoten(
      Map<String, List<ProzessknotenResponse>> jeRichtung) {
    List<RichtungsknotenResponse> knoten = new ArrayList<>(jeRichtung.size());
    for (Map.Entry<String, List<ProzessknotenResponse>> eintrag : jeRichtung.entrySet()) {
      List<ProzessknotenResponse> blaetter = List.copyOf(eintrag.getValue());
      knoten.add(
          new RichtungsknotenResponse(
              eintrag.getKey(),
              blaetter.size(),
              blaetter.stream().mapToLong(ProzessknotenResponse::nachrichten).sum(),
              blaetter.stream().mapToLong(ProzessknotenResponse::fehler).sum(),
              blaetter));
    }
    knoten.sort(
        Comparator.comparingInt(
                (RichtungsknotenResponse knoten2) -> richtungsrang(knoten2.richtung()))
            .thenComparing(
                RichtungsknotenResponse::richtung,
                Comparator.nullsLast(Comparator.naturalOrder())));
    return List.copyOf(knoten);
  }

  /**
   * Der Rang einer Richtung in der Anzeige.
   *
   * <p>Drei Faelle, und der mittlere ist der Grund fuer diese Methode: Ein <b>gepflegter, aber
   * unbekannter</b> Wert — den die Spalte zulaesst, weil sie {@code varchar} ist und die Whitelist
   * im Code steht — faellt hinter die bekannten und <b>vor</b> „nicht ermittelt". Er verschwindet
   * damit nicht und wird auch nicht mit dem leeren Feld verwechselt.
   */
  private static int richtungsrang(String richtung) {
    if (richtung == null) {
      return Richtung.values().length + 1;
    }
    for (Richtung bekannt : Richtung.values()) {
      if (bekannt.name().equals(richtung)) {
        return bekannt.ordinal();
      }
    }
    return Richtung.values().length;
  }

  private ProzessknotenResponse blatt(
      Prozessgeruestzeile zeile, Map<String, Kennzahl> jeProzess, LocalDateTime jetzt) {
    Kennzahl kennzahl = jeProzess.getOrDefault(zeile.processId(), Kennzahl.LEER);
    return new ProzessknotenResponse(
        zeile.processId(),
        zeile.processName(),
        kennzahl.nachrichten(),
        kennzahl.fehler(),
        Zeitpunkte.nachUtc(zeile.letzteBewegung(), anwendungsuhr.getZone()),
        zustand(zeile.letzteBewegung(), jetzt));
  }

  /**
   * <b>Die drei Zustaende, abgeleitet aus einem Wert.</b>
   *
   * <p>Die Reihenfolge der Pruefung ist die Festlegung: {@code NIE} vor {@code STILL} vor {@code
   * BEWEGT}. Ein Prozess, der nie etwas trug, ist <b>nicht still — er war nie laut</b>, und die
   * Handlung daraus ist eine andere.
   *
   * <p><b>{@code isBefore} und nicht {@code isEqual}-tolerant:</b> Genau auf der Schwelle gilt ein
   * Prozess als bewegt. Die Grenze muss irgendwo liegen; sie zugunsten von „bewegt" zu ziehen
   * heisst, im Zweifel <b>nicht</b> zu markieren — eine Markierung, die auch im Grenzfall
   * erscheint, wird schneller uebersehen als eine, die es nicht tut.
   */
  static Prozesszustand zustand(LocalDateTime letzteBewegung, LocalDateTime jetzt) {
    if (letzteBewegung == null) {
      return Prozesszustand.NIE;
    }
    return letzteBewegung.isBefore(jetzt.minus(STILLE_SCHWELLE))
        ? Prozesszustand.STILL
        : Prozesszustand.BEWEGT;
  }

  private BaumsummeResponse gesamt(
      List<Prozessgeruestzeile> geruest, Map<String, Kennzahl> jeProzess, LocalDateTime jetzt) {
    int bewegt = 0;
    int still = 0;
    int nie = 0;
    long nachrichten = 0;
    long fehler = 0;
    for (Prozessgeruestzeile zeile : geruest) {
      switch (zustand(zeile.letzteBewegung(), jetzt)) {
        case BEWEGT -> bewegt++;
        case STILL -> still++;
        case NIE -> nie++;
      }
      Kennzahl kennzahl = jeProzess.getOrDefault(zeile.processId(), Kennzahl.LEER);
      nachrichten += kennzahl.nachrichten();
      fehler += kennzahl.fehler();
    }
    return new BaumsummeResponse(geruest.size(), bewegt, still, nie, nachrichten, fehler);
  }

  /**
   * Die beiden Zahlen eines Prozesses im Fenster, waehrend die Rohstatuszeilen verdichtet werden.
   */
  private record Kennzahl(long nachrichten, long fehler) {

    private static final Kennzahl LEER = new Kennzahl(0, 0);

    Kennzahl plus(Kennzahl weitere) {
      return new Kennzahl(nachrichten + weitere.nachrichten, fehler + weitere.fehler);
    }
  }
}
