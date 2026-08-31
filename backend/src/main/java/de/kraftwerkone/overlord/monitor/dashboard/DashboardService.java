package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Setzt die Landingpage zusammen — <b>aus einem Lesevorgang je Block und nicht aus einem je
 * Kennzahl</b>.
 *
 * <h2>Die Einordnung entsteht hier und wird gerufen, nicht nachgebaut</h2>
 *
 * <p>Im Rollup steht der <b>Rohwert</b> (Entscheidung E-g). Die Kategorie bildet {@link
 * MessageStatusClassifier#einordnung(String)} — dieselbe Stelle, die Liste, Detailansicht und
 * spaeter der Chatbot benutzen. <b>Ein zweites {@code switch} ueber Statuswoerter darf in diesem
 * Paket nicht entstehen:</b> Es liefe beim naechsten neuen Statuswert von der Liste weg, und der
 * Unterschied fiele erst auf, wenn jemand zwei Zahlen nebeneinanderlegt.
 *
 * <p>Dasselbe gilt fuer die <b>Fehlerart</b>: {@link MessageStatusClassifier#fehlerart(String)},
 * gerufen und nicht nachgebaut.
 *
 * <h2>Ein Uhrenschlag je Anfrage</h2>
 *
 * <p>{@code jetzt} kommt genau einmal aus der <b>Anwendungsuhr</b> (Regel Z1) und wird danach
 * herumgereicht. Zwei Schlaege innerhalb derselben Anfrage waeren zwei Stichtage — und bei einem
 * Aufruf, der eine Eimergrenze streift, zwei verschiedene Fenster.
 */
@Service
public class DashboardService {

  private final DashboardRepository dashboardRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  DashboardService(
      DashboardRepository dashboardRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.dashboardRepository = dashboardRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
  }

  /**
   * Wie viele benannte Werte der Verteilungsblock einzeln zeigt.
   *
   * <p><b>Zehn, und die Zahl ist gewaehlt und nicht gemessen.</b> Sie ist die Grenze, an der die
   * Restzeile entsteht; M98 hat sie fuer vier Mandanten durchgerechnet (Top 10 traegt bei {@code
   * NEXANS} 76,4 %, bei {@code VOTG} 76,5 %, bei {@code IBIS} 72,1 %). Sie steht hier als Konstante
   * und nicht in der Konfiguration: Ein Schalter dafuer waere eine Gestaltungsentscheidung, die
   * niemand getroffen hat.
   */
  static final int VERTEILUNG_TOP = 10;

  /**
   * Die ganze Landingpage fuer den aktiven Mandanten.
   *
   * @param mandant Regel M2 — erster Pflichtparameter, und er kommt aus der Sitzung (Regel M1)
   * @param gewaehlt das Paar aus der URL, oder {@code null}
   * @param sicht wonach der Verteilungsblock gruppiert
   */
  public DashboardResponse landingpage(
      MandantContext mandant, Dashboardzeitraum gewaehlt, Verteilungssicht sicht) {
    LocalDateTime jetzt = LocalDateTime.now(anwendungsuhr);
    Dashboardzeitraum zeitraum = gewaehlt == null ? Dashboardzeitraum.STUNDEN_48 : gewaehlt;
    Zeitfenster fenster = zeitraum.fenster(jetzt);

    List<Rollupsumme> summen = dashboardRepository.verlauf(mandant, zeitraum, fenster);
    List<Verteilungssumme> verteilt =
        dashboardRepository.verteilung(mandant, zeitraum, fenster, sicht);

    return new DashboardResponse(
        zeitraum.code(),
        fensterAntwort(fenster),
        verlauf(summen),
        kacheln(summen),
        verteilung(verteilt, sicht));
  }

  /**
   * Block 5: Top 10, dann die beiden Restzeilen — <b>und die stehen immer unten</b>.
   *
   * <p>Die Abfrage liefert die benannten Werte bereits absteigend und „nicht zugeordnet" am Ende.
   * <b>Sortiert wird hier trotzdem noch einmal</b>, und zwar mit dem Wert als zweitem Schluessel:
   * Bei Gleichstand haengt die Reihenfolge sonst davon ab, in welcher Reihenfolge die Datenbank die
   * Gruppen zurueckgibt, und der Block spraenge zwischen zwei Aufrufen.
   *
   * <p><b>„Übrige" fehlt, wenn es keinen Rang 11 gibt</b> — eine Null ist dort reines Rangartefakt.
   * <b>„Nicht zugeordnet" erscheint immer, auch bei null:</b> Das ist eine Aussage ueber den
   * Katalog, und ohne sie waere <i>vollstaendig gepflegt</i> nicht von <i>diese Ansicht zeigt das
   * nicht</i> zu unterscheiden.
   */
  private VerteilungResponse verteilung(List<Verteilungssumme> summen, Verteilungssicht sicht) {
    long nichtZugeordnet = 0;
    List<Verteilungssumme> benannt = new ArrayList<>();
    for (Verteilungssumme summe : summen) {
      if (summe.schluessel() == null) {
        nichtZugeordnet += summe.anzahl();
      } else {
        benannt.add(summe);
      }
    }
    benannt.sort(
        Comparator.comparingLong(Verteilungssumme::anzahl)
            .reversed()
            .thenComparing(Verteilungssumme::schluessel));

    List<VerteilungszeileResponse> zeilen = new ArrayList<>();
    for (Verteilungssumme summe : benannt.subList(0, Math.min(VERTEILUNG_TOP, benannt.size()))) {
      zeilen.add(VerteilungszeileResponse.wert(summe.schluessel(), summe.anzahl()));
    }
    if (benannt.size() > VERTEILUNG_TOP) {
      List<Verteilungssumme> rest = benannt.subList(VERTEILUNG_TOP, benannt.size());
      zeilen.add(
          VerteilungszeileResponse.uebrige(
              rest.size(), rest.stream().mapToLong(Verteilungssumme::anzahl).sum()));
    }
    zeilen.add(VerteilungszeileResponse.nichtZugeordnet(nichtZugeordnet));
    return new VerteilungResponse(sicht, zeilen);
  }

  private FensterResponse fensterAntwort(Zeitfenster fenster) {
    return new FensterResponse(
        Zeitpunkte.nachUtc(fenster.von(), anwendungsuhr.getZone()),
        Zeitpunkte.nachUtc(fenster.bis(), anwendungsuhr.getZone()));
  }

  /**
   * Block 1: je Eimer die Aufschluesselung nach Einordnung.
   *
   * <p><b>Ein {@link TreeMap} und kein {@code HashMap}:</b> Die Eimer kommen zwar sortiert aus der
   * Abfrage, aber die Reihenfolge der Antwort darf nicht davon abhaengen, dass jemand das {@code
   * ORDER BY} stehen laesst.
   *
   * <p><b>Nur Einordnungen, die vorkommen.</b> Alle acht je Eimer waeren bei 48 Stunden 384
   * Eintraege, die meisten null. Die Reihenfolge ist die der Aufzaehlung ({@link EnumMap}) und
   * damit ueber alle Eimer dieselbe — eine Oberflaeche, die Farben nach Position vergibt, bekommt
   * sonst in jedem Balken eine andere.
   */
  private List<VerlaufspunktResponse> verlauf(List<Rollupsumme> summen) {
    Map<LocalDateTime, Map<MessageStatusKind, Long>> jeEimer = new TreeMap<>();
    for (Rollupsumme summe : summen) {
      jeEimer
          .computeIfAbsent(summe.eimer(), eimer -> new EnumMap<>(MessageStatusKind.class))
          .merge(statusClassifier.einordnung(summe.messageStatus()), summe.anzahl(), Long::sum);
    }

    List<VerlaufspunktResponse> punkte = new ArrayList<>(jeEimer.size());
    for (Map.Entry<LocalDateTime, Map<MessageStatusKind, Long>> eintrag : jeEimer.entrySet()) {
      List<EinordnungszahlResponse> einordnungen =
          eintrag.getValue().entrySet().stream()
              .map(je -> new EinordnungszahlResponse(je.getKey(), je.getValue()))
              .toList();
      long gesamt = einordnungen.stream().mapToLong(EinordnungszahlResponse::anzahl).sum();
      punkte.add(
          new VerlaufspunktResponse(
              Zeitpunkte.nachUtc(eintrag.getKey(), anwendungsuhr.getZone()), gesamt, einordnungen));
    }
    return punkte;
  }

  /**
   * Bloecke 2 und 3 — <b>aus denselben Zeilen wie der Verlauf</b>, ohne einen zweiten Lesevorgang.
   *
   * <p>Die Fehlerarten kommen aus dem Rohwert und nicht aus der Einordnung: Die Einordnung sagt
   * <i>Fehler</i>, der Rohwert sagt <i>welcher</i>.
   */
  private KachelnResponse kacheln(List<Rollupsumme> summen) {
    long nachrichten = 0;
    long fehler = 0;
    Map<String, Long> jeRohwert = new LinkedHashMap<>();
    for (Rollupsumme summe : summen) {
      nachrichten += summe.anzahl();
      if (statusClassifier.einordnung(summe.messageStatus()) == MessageStatusKind.FEHLER) {
        fehler += summe.anzahl();
        jeRohwert.merge(summe.messageStatus(), summe.anzahl(), Long::sum);
      }
    }

    List<FehlerartResponse> arten =
        jeRohwert.entrySet().stream()
            .map(
                eintrag ->
                    new FehlerartResponse(
                        eintrag.getKey(),
                        statusClassifier.fehlerart(eintrag.getKey()),
                        eintrag.getValue()))
            .sorted(
                Comparator.comparingLong(FehlerartResponse::anzahl)
                    .reversed()
                    .thenComparing(
                        FehlerartResponse::rohwert,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    return new KachelnResponse(nachrichten, new FehlerkachelResponse(fehler, arten));
  }
}
