package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.FehlerLiveErgebnis;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveResponse;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZeile;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZustand;
import de.kraftwerkone.overlord.monitor.common.LiveRestEntscheidung;
import de.kraftwerkone.overlord.monitor.common.LiveRestErgebnis;
import de.kraftwerkone.overlord.monitor.common.LiveRestKorrektur;
import de.kraftwerkone.overlord.monitor.common.LiveRestResponse;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.LiveRestZeile;
import de.kraftwerkone.overlord.monitor.common.LiveRestZustand;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Service aus den Rohzeilen macht — <b>ohne Datenbank</b>.
 *
 * <p>Die Statements prueft {@code DashboardStatementsTest}, die Plaene {@code DashboardPlanDbIT},
 * die Trennung {@code DashboardIsolationDbIT}. <b>Hier steht, was zwischen Rohwert und Antwort
 * passiert:</b> die Einordnung, die Fehlerarten, die beiden Restzeilen und der Umgang mit einer
 * gestorbenen Live-Abfrage.
 *
 * <p><b>Alle Pruefwerte sind erfunden</b> (Regel G1) — der Test haengt an keiner Zahl aus dem
 * Bestand und an keinem Pflegestand (Regel T2).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
  private static final Instant JETZT = Instant.parse("2025-12-30T04:09:47Z");
  private static final LocalDateTime EIMER = LocalDateTime.parse("2025-12-30T03:00:00");

  /**
   * 412 Sekunden vor dem Anker — die Groesse, die die Kachel <i>Laeuft</i> als Alter ausweist.
   *
   * <p><b>In Ortszeit und nicht in UTC:</b> Die Uhr steht auf {@code 04:09:47Z}, die Zone ist
   * {@code Europe/Berlin}, und im Dezember sind das {@code 05:09:47} — dieselbe Bruchstelle, die
   * {@code docs/datenzugriff.md} §7 beschreibt. Zeitstempel aus {@code GlassfishDB} sind
   * Wanduhrzeit des Servers und werden nirgends konvertiert.
   */
  private static final LocalDateTime LAEUFT_SEIT = LocalDateTime.parse("2025-12-30T05:02:55");

  /**
   * 579.934 Sekunden vor dem Anker, also 6 Tage 17 Stunden. <b>Die Zahl ist erfunden und trotzdem
   * nicht beliebig:</b> Sie ist die Groessenordnung der aeltesten wartenden Zeile der Testkopie
   * (M144 b) — ein Prueffall, der aussieht wie der Ernstfall, ohne an ihm zu haengen (Regel T2).
   */
  private static final LocalDateTime WARTET_SEIT = LocalDateTime.parse("2025-12-23T12:04:13");

  @Mock private DashboardRepository repository;

  @Mock private DienstLeseRepository dienstRepository;

  /**
   * Der Baustein aus {@code common} als Attrappe. <b>Ohne Stellung sagt er „ausgesetzt, kein
   * Lauf“</b> — dann gibt es keine Korrektur, und die Faelle von vor Teil B sehen dieselben Zahlen
   * wie vorher. Die Faelle unter „Der Live-Rest“ stellen ihn um.
   */
  @Mock private LiveRestService liveRest;

  /**
   * Die Fehlerlesung aus {@code common} als Attrappe (E-208). <b>Ohne Stellung ist sie
   * ausgesetzt</b> — dann bleiben die Fehler beim Rollup, und die Faelle von vor dem 18.09.2026
   * sehen dieselben Zahlen wie vorher. Die Faelle unter „Fehler live" stellen sie um.
   */
  @Mock private FehlerLiveService fehlerLive;

  @BeforeEach
  void liveRestOhneLauf() {
    when(liveRest.ermittle(any(), any()))
        .thenReturn(LiveRestErgebnis.ohneKorrektur(LiveRestEntscheidung.ausgesetztOhneLauf()));
    when(fehlerLive.ermittle(any(), any(), any())).thenReturn(FehlerLiveErgebnis.ausgesetzt());
  }

  /**
   * Der Dienst <b>ohne</b> Ablagenpruefung — sie ist im Profil {@code dev} und in diesem Test aus.
   *
   * <p>Das leere {@link java.util.Optional} ist kein Vorbehalt gegen eine fehlende Bean, sondern
   * der Zustand selbst: abgeschaltet. Die Kachel sagt das dann mit benanntem Grund, und genau das
   * prueft {@link #ablagenkachel_ist_abgeschaltet}.
   */
  private DashboardService service() {
    return service(JETZT);
  }

  /** Derselbe Dienst an einem anderen Stichtag — fuer den Fall aus der Produktion (Fehler live). */
  private DashboardService service(Instant jetzt) {
    return new DashboardService(
        repository,
        new MessageStatusClassifier(),
        Clock.fixed(jetzt, ZONE),
        dienstRepository,
        new DienstStatusClassifier(),
        java.util.Optional.empty(),
        liveRest,
        fehlerLive);
  }

  /** Der Normalfall: Es gibt Verkehr, das erste Paar traegt, nichts stirbt. */
  private void bestandMit(List<Rollupsumme> verlauf, List<Verteilungssumme> verteilung) {
    when(repository.belegung(any(), any(), any())).thenReturn(new Belegung(48, 999));
    when(repository.verlauf(any(), any(), any())).thenReturn(verlauf);
    when(repository.verteilung(any(), any(), any(), any())).thenReturn(verteilung);
    when(repository.offeneNachrichten(any(), eq(MessageStatusKind.LAEUFT)))
        .thenReturn(java.util.Optional.of(new Offenstand(3, LAEUFT_SEIT)));
    when(repository.offeneNachrichten(any(), eq(MessageStatusKind.WARTEND)))
        .thenReturn(java.util.Optional.of(new Offenstand(7, WARTET_SEIT)));
    when(repository.hatWartendeAblaeufe(any())).thenReturn(true);
    when(repository.zuletztAufgefallen(any(), any(), anyInt())).thenReturn(List.of());
    when(repository.letzterLauf()).thenReturn(java.util.Optional.empty());
    when(dienstRepository.dienste()).thenReturn(List.of());
  }

  private DashboardResponse antwort() {
    return service().landingpage(MANDANT, null);
  }

  private static Verteilungssumme partner(String name, long anzahl) {
    return new Verteilungssumme(name, anzahl);
  }

  // ─── Einordnung ───────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Einordnung — der Klassifizierer wird gerufen, nicht nachgebaut")
  class Einordnung {

    /**
     * <b>Die Probe darauf, dass hier kein zweites {@code switch} ueber Statuswoerter steht.</b> Ein
     * nachgebautes wuerde einen unbekannten Wert entweder verschlucken oder raten; der
     * Klassifizierer legt ihn nach {@code UNGEKLAERT} — und ein unbekannter Wert <i>mit</i> {@code
     * ERROR_}-Praefix nach {@code FEHLER}, weil dieselbe Regel in SQL gilt.
     */
    @Test
    @DisplayName("Ein unbekannter Rohwert faellt nach UNGEKLAERT und in keinen bekannten Eimer")
    void unbekannter_rohwert() {
      bestandMit(
          List.of(
              new Rollupsumme(EIMER, "FINISHED", 10), new Rollupsumme(EIMER, "IT_GIBTESNICHT", 4)),
          List.of());

      List<EinordnungszahlResponse> einordnungen = antwort().verlauf().getFirst().einordnungen();

      assertThat(einordnungen)
          .as("Der unbekannte Wert steht fuer sich und nicht bei den abgeschlossenen")
          .containsExactlyInAnyOrder(
              new EinordnungszahlResponse(MessageStatusKind.ABGESCHLOSSEN, 10),
              new EinordnungszahlResponse(MessageStatusKind.UNGEKLAERT, 4));
    }

    /**
     * Die Gegenrichtung: Ein unbekannter Wert mit {@code ERROR_}-Praefix <b>ist</b> ein Fehler.
     * Ohne diese Zeile fielen Java und SQL auseinander — der Statusfilter der Liste faende die
     * Zeile, das Dashboard zaehlte sie nicht mit.
     */
    @Test
    @DisplayName("Ein unbekanntes ERROR_ zaehlt als Fehler, weil dieselbe Regel in SQL gilt")
    void unbekannter_fehler() {
      bestandMit(List.of(new Rollupsumme(EIMER, "ERROR_NEUARTIG", 2)), List.of());

      DashboardResponse antwort = antwort();

      assertThat(antwort.kacheln().fehler().anzahl()).isEqualTo(2);
      assertThat(antwort.verlauf().getFirst().einordnungen())
          .containsExactly(new EinordnungszahlResponse(MessageStatusKind.FEHLER, 2));
    }

    @Test
    @DisplayName("Mehrere Rohwerte derselben Einordnung werden je Eimer zusammengefasst")
    void rohwerte_derselben_einordnung() {
      bestandMit(
          List.of(
              new Rollupsumme(EIMER, "EERP_RECEIVED", 5),
              new Rollupsumme(EIMER, "COMMIT_RECEIVED", 6)),
          List.of());

      VerlaufspunktResponse punkt = antwort().verlauf().getFirst();

      assertThat(punkt.einordnungen())
          .containsExactly(new EinordnungszahlResponse(MessageStatusKind.QUITTIERT, 11));
      assertThat(punkt.gesamt()).isEqualTo(11);
    }
  }

  // ─── Fehlerarten ──────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Fehlerarten kommen aus demselben Rohwert")
  class Fehlerarten {

    @Test
    @DisplayName("COMMIT_REJECTED erscheint als „Vom Partner abgelehnt“, ERROR_ als Namensteil")
    void arten() {
      bestandMit(
          List.of(
              new Rollupsumme(EIMER, "ERROR_DUPLICATE", 9),
              new Rollupsumme(EIMER, "COMMIT_REJECTED", 4),
              new Rollupsumme(EIMER, "FINISHED", 100)),
          List.of());

      FehlerkachelResponse fehler = antwort().kacheln().fehler();

      assertThat(fehler.anzahl()).as("FINISHED zaehlt nicht mit").isEqualTo(13);
      assertThat(fehler.arten())
          .as("Absteigend nach Anzahl, und der Rohwert steht daneben")
          .containsExactly(
              new FehlerartResponse("ERROR_DUPLICATE", "DUPLICATE", 9),
              new FehlerartResponse("COMMIT_REJECTED", "Vom Partner abgelehnt", 4));
    }

    /**
     * <b>Entscheidung E-d, maschinell festgehalten.</b> „Unquittiert" ist aus dem MVP genommen —
     * kein Feld, kein Platzhalter. {@code COMMIT_REJECTED} gehoerte ohnehin nicht dorthin: Das ist
     * eine Quittung, nur eine negative.
     */
    @Test
    @DisplayName("Es gibt kein Feld „unquittiert“ — nirgends in der Antwort")
    void kein_unquittiert() {
      List<String> felder = new ArrayList<>();
      for (Class<?> klasse :
          List.of(
              DashboardResponse.class,
              KachelnResponse.class,
              FehlerkachelResponse.class,
              FehlerartResponse.class,
              OffeneKachelResponse.class)) {
        Arrays.stream(klasse.getRecordComponents())
            .map(RecordComponent::getName)
            .forEach(felder::add);
      }

      assertThat(felder)
          .as("E-d: nicht gebaut, nicht gezaehlt, nicht angezeigt — und kein Platzhalter")
          .noneMatch(feld -> feld.toLowerCase(java.util.Locale.ROOT).contains("unquittiert"));
    }
  }

  // ─── Restzeilen ───────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die beiden Restzeilen des Verteilungsblocks")
  class Restzeilen {

    private List<Verteilungssumme> partnerReihe(int wieViele) {
      List<Verteilungssumme> summen = new ArrayList<>();
      for (int i = 1; i <= wieViele; i++) {
        summen.add(partner("P" + i, 1000L - i));
      }
      return summen;
    }

    @Test
    @DisplayName("Bei zwoelf Partnern gibt es Top 10 und „Übrige (2)“, und die steht unten")
    void uebrige_bei_rang_elf() {
      bestandMit(List.of(), partnerReihe(12));

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().partner().zeilen();

      assertThat(zeilen).hasSize(12);
      assertThat(zeilen.subList(0, 10)).allMatch(zeile -> zeile.art() == Verteilungszeilenart.WERT);
      assertThat(zeilen.get(10))
          .as("Die Restzeile fasst genau die Raenge 11 und 12 zusammen")
          .isEqualTo(VerteilungszeileResponse.uebrige(2, 989 + 988));
      assertThat(zeilen.getLast().art()).isEqualTo(Verteilungszeilenart.NICHT_ZUGEORDNET);
    }

    @Test
    @DisplayName("Ohne Rang 11 fehlt „Übrige“ — eine Null waere reines Rangartefakt")
    void keine_uebrige_ohne_rang_elf() {
      bestandMit(List.of(), partnerReihe(10));

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().partner().zeilen();

      assertThat(zeilen)
          .as("Zehn Werte und die eine Restzeile, die immer da ist")
          .hasSize(11)
          .noneMatch(zeile -> zeile.art() == Verteilungszeilenart.UEBRIGE);
    }

    /**
     * <b>Die Zeile, die auch bei null erscheint.</b> Null heisst „alles kuratiert", und {@code
     * IBIS} liefert sie gerade. Wird sie bei null ausgeblendet, ist <i>vollstaendig gepflegt</i>
     * nicht mehr von <i>diese Ansicht zeigt das nicht</i> zu unterscheiden.
     */
    @Test
    @DisplayName("„Nicht zugeordnet“ erscheint auch bei null")
    void nicht_zugeordnet_auch_bei_null() {
      bestandMit(List.of(), partnerReihe(3));

      assertThat(antwort().verteilung().partner().zeilen().getLast())
          .isEqualTo(VerteilungszeileResponse.nichtZugeordnet(0));
    }

    /**
     * Der Fall {@code IBIS}: Die Restzeile ist groesser als jeder benannte Wert. <b>Sie steht
     * trotzdem unten</b> — sonst stuende „Übrige (40)" auf Rang 1, als gaebe es einen Partner
     * dieses Namens.
     */
    @Test
    @DisplayName("Beide Restzeilen stehen unten, auch wenn sie die groessten sind")
    void restzeilen_stehen_unten_auch_wenn_gross() {
      List<Verteilungssumme> summen = new ArrayList<>();
      for (int i = 1; i <= 11; i++) {
        summen.add(partner("P" + i, 10));
      }
      summen.add(new Verteilungssumme(null, 10_000));
      bestandMit(List.of(), summen);

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().partner().zeilen();

      assertThat(zeilen.get(10))
          .as("Rang 11 ist die kleinste Zeile und steht trotzdem vor den Restzeilen")
          .isEqualTo(VerteilungszeileResponse.uebrige(1, 10));
      assertThat(zeilen.getLast())
          .as("Die groesste Zeile des Blocks steht unten, weil sie keine Rangposition ist")
          .isEqualTo(VerteilungszeileResponse.nichtZugeordnet(10_000));
    }

    @Test
    @DisplayName("„Nicht zugeordnet“ faellt nie in „Übrige“, auch nicht bei elf Partnern")
    void nicht_zugeordnet_ist_keine_rangposition() {
      List<Verteilungssumme> summen = new ArrayList<>(partnerReihe(11));
      summen.add(new Verteilungssumme(null, 10_000));
      bestandMit(List.of(), summen);

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().partner().zeilen();

      assertThat(zeilen.get(10))
          .as("Rang 11 gehoert einem benannten Partner, nicht dem Eimer ohne Namen")
          .isEqualTo(VerteilungszeileResponse.uebrige(1, 989));
      assertThat(zeilen.getLast()).isEqualTo(VerteilungszeileResponse.nichtZugeordnet(10_000));
    }

    // Hier stand bis zum 16.09.2026 „Die Sicht steht in der Antwort, auch wenn der Parameter
    // fehlte". Den Parameter gibt es nicht mehr, und das Feld `sicht` auch nicht: Die Antwort
    // traegt beide Sichten, und welche welche ist, sagt der Schluessel. An seine Stelle tritt die
    // Klasse `BeideSichten` darunter.
  }

  // ─── Beide Sichten ────────────────────────────────────────────────────────────

  /**
   * <b>Die Antwort traegt beide Sichten</b> (seit dem 16.09.2026, {@code docs/dashboard.md} §4).
   *
   * <p>Die Faelle darueber pruefen die Regeln an der Partnersicht. <b>Hier steht, dass sie je Sicht
   * getrennt gelten</b>: Jede Sicht bekommt ihre eigenen Summen, ihre eigene Restzeile „Übrige" nur
   * bei eigenem Rang 11 und ihr eigenes „nicht zugeordnet" — und nichts wandert von einer in die
   * andere. Die beiden gestellten Reihen sind deshalb absichtlich verschieden gebaut.
   */
  @Nested
  @DisplayName("Beide Sichten in einer Antwort")
  class BeideSichten {

    private List<Verteilungssumme> reihe(String praefix, int wieViele, long start) {
      List<Verteilungssumme> summen = new ArrayList<>();
      for (int i = 1; i <= wieViele; i++) {
        summen.add(partner(praefix + i, start - i));
      }
      return summen;
    }

    /**
     * Zwoelf Partner ohne unzugeordnete Zeile, aber nur zwei Richtungen und eine unzugeordnete
     * Summe. <b>Die Partnersicht braucht „Übrige", die Richtungssicht darf keine haben</b> — ein
     * Zusammenbau, der die Restzeilen einmal fuer beide rechnet, faellt an genau dieser Gestalt.
     */
    @Test
    @DisplayName("Jede Sicht hat ihre eigenen Zeilen und ihre eigenen Restzeilen")
    void restzeilen_je_sicht() {
      bestandMit(List.of(), List.of());
      when(repository.verteilung(any(), any(), any(), eq(Verteilungssicht.PARTNER)))
          .thenReturn(reihe("P", 12, 1000));
      List<Verteilungssumme> richtungen = new ArrayList<>(reihe("R", 2, 500));
      richtungen.add(new Verteilungssumme(null, 77));
      when(repository.verteilung(any(), any(), any(), eq(Verteilungssicht.RICHTUNG)))
          .thenReturn(richtungen);

      VerteilungResponse verteilung = antwort().verteilung();

      List<VerteilungszeileResponse> partner = verteilung.partner().zeilen();
      assertThat(partner).hasSize(12);
      assertThat(partner.subList(0, 10))
          .allMatch(zeile -> zeile.art() == Verteilungszeilenart.WERT)
          .allMatch(zeile -> zeile.wert().startsWith("P"));
      assertThat(partner.get(10))
          .as("Die Partnersicht hat einen Rang 11 und 12 und fasst beide zusammen")
          .isEqualTo(VerteilungszeileResponse.uebrige(2, 989 + 988));
      assertThat(partner.getLast())
          .as("Nicht zugeordnet steht auch hier, und zwar mit der eigenen Null")
          .isEqualTo(VerteilungszeileResponse.nichtZugeordnet(0));

      assertThat(verteilung.richtung().zeilen())
          .as("Die Richtungssicht hat keinen Rang 11 — also keine „Übrige“, und nichts von Partner")
          .containsExactly(
              VerteilungszeileResponse.wert("R1", 499),
              VerteilungszeileResponse.wert("R2", 498),
              VerteilungszeileResponse.nichtZugeordnet(77));
    }

    /**
     * <b>Je Sicht genau ein Statement, und beide in jeder Antwort</b> — auch bei ausdruecklich
     * genanntem Zeitraum und auch ohne ihn. Ein Service, der nur die Partnersicht liest und die
     * Richtung daraus ableitet oder weglaesst, faellt hier.
     */
    @Test
    @DisplayName("Das Verteilungsstatement laeuft je Sicht genau einmal")
    void je_sicht_ein_statement() {
      bestandMit(List.of(), List.of());

      service().landingpage(MANDANT, Rollupzeitraum.TAGE_30);

      verify(repository, org.mockito.Mockito.times(1))
          .verteilung(
              eq(MANDANT),
              eq(Rollupzeitraum.TAGE_30),
              any(Zeitfenster.class),
              eq(Verteilungssicht.PARTNER));
      verify(repository, org.mockito.Mockito.times(1))
          .verteilung(
              eq(MANDANT),
              eq(Rollupzeitraum.TAGE_30),
              any(Zeitfenster.class),
              eq(Verteilungssicht.RICHTUNG));
      verify(repository, org.mockito.Mockito.times(2)).verteilung(any(), any(), any(), any());
    }

    /**
     * <b>Das Feld {@code sicht} ist entfallen, und kein anderes hat seinen Platz genommen.</b> Die
     * Sicht ist der Schluessel; ein Feld daneben saehe aus, als sei eine gewaehlt worden.
     */
    @Test
    @DisplayName("Der Block hat genau die Felder partner und richtung, und keine Sicht")
    void felder_des_blocks() {
      assertThat(
              Arrays.stream(VerteilungResponse.class.getRecordComponents())
                  .map(RecordComponent::getName))
          .containsExactly("partner", "richtung");
      assertThat(
              Arrays.stream(VerteilungszeilenResponse.class.getRecordComponents())
                  .map(RecordComponent::getName))
          .containsExactly("zeilen");
    }
  }

  // ─── Ueberfaellig ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Kacheln Laeuft und Wartend")
  class OffeneKacheln {

    @Test
    @DisplayName("Beide Kacheln tragen Zahl und Alter, wenn beide Abfragen durchlaufen")
    void beide_kacheln() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 1)), List.of());

      // LAEUFT_SEIT liegt 412 s vor dem Anker, WARTET_SEIT 579.934 s (6 d 17 h) — dieselbe
      // Groessenordnung wie die aelteste wartende Zeile der Testkopie (M144 b).
      assertThat(antwort().kacheln().laeuft()).isEqualTo(new OffeneKachelResponse(3L, 412L, true));
      assertThat(antwort().kacheln().wartend())
          .isEqualTo(new OffeneKachelResponse(7L, 579934L, true));
    }

    /**
     * <b>{@code aeltesteSekunden} ist {@code null}, wenn es keine Zeile gibt</b> — nicht {@code 0}.
     * Eine Null hiesse „seit null Sekunden", und das ist etwas anderes als „es gibt nichts".
     */
    @Test
    @DisplayName("Bei anzahl = 0 ist aeltesteSekunden null und nicht null Sekunden")
    void kein_alter_ohne_zeile() {
      bestandMit(List.of(), List.of());
      when(repository.offeneNachrichten(any(), eq(MessageStatusKind.LAEUFT)))
          .thenReturn(java.util.Optional.of(new Offenstand(0, null)));

      assertThat(antwort().kacheln().laeuft()).isEqualTo(new OffeneKachelResponse(0L, null, true));
    }

    /**
     * <b>Der Fall, den {@code docs/dashboard.md} §5 verlangt:</b> Stirbt die Live-Abfrage, liefert
     * die Antwort die uebrigen Bloecke und die Kachel als „nicht ermittelbar" — <b>und nicht als
     * Null</b>. Eine Null hiesse „es laeuft nichts", und das waere in einem Ueberwachungswerkzeug
     * die schlimmste falsche Antwort.
     */
    @Test
    @DisplayName(
        "Stirbt eine Live-Abfrage, steht die Seite und die Kachel sagt „nicht ermittelbar“")
    void nicht_ermittelbar() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 42)), List.of(partner("P", 42)));
      when(repository.offeneNachrichten(any(), eq(MessageStatusKind.LAEUFT)))
          .thenReturn(java.util.Optional.empty());

      DashboardResponse antwort = antwort();

      assertThat(antwort.kacheln().laeuft()).isEqualTo(new OffeneKachelResponse(null, null, false));
      assertThat(antwort.kacheln().nachrichten())
          .as("Die uebrigen Bloecke kommen aus message_rollup und sind unberuehrt")
          .isEqualTo(42);
      assertThat(antwort.verteilung().partner().zeilen()).isNotEmpty();
      assertThat(antwort.verteilung().richtung().zeilen()).isNotEmpty();
      assertThat(antwort.verlauf()).isNotEmpty();
    }

    /**
     * <b>Und das ist der Unterschied zur alten Ueberfaellig-Kachel:</b> Dort fielen zwei Zahlen
     * <i>derselben</i> Kachel zusammen, weil man sie nebeneinander liest. <i>Laeuft</i> und
     * <i>Wartend</i> sind zwei verschiedene Auskuenfte und keine Rechnung — faellt eine, steht die
     * andere.
     */
    @Test
    @DisplayName("Faellt eine der beiden Kacheln, bleibt die andere ermittelbar")
    void die_kacheln_fallen_nicht_zusammen() {
      bestandMit(List.of(), List.of());
      when(repository.offeneNachrichten(any(), eq(MessageStatusKind.LAEUFT)))
          .thenReturn(java.util.Optional.empty());

      assertThat(antwort().kacheln().laeuft().ermittelbar()).isFalse();
      assertThat(antwort().kacheln().wartend().ermittelbar())
          .as("Zwei Statements, zwei Auskuenfte")
          .isTrue();
    }
  }

  // ─── Die Erscheinungsbedingung der Kachel Wartend ─────────────────────────────

  @Nested
  @DisplayName("Die Kachel Wartend erscheint strukturell und nicht nach der Zahl (E-74)")
  class ErscheinungsbedingungWartend {

    /**
     * <b>Der Kern von E-74.</b> Ein Mandant, dessen Ablaeufe suspendieren, sieht die Kachel — auch
     * wenn gerade nichts wartet. <i>Heute wartet nichts</i> und <i>dieser Mandant wartet nie</i>
     * sind zwei verschiedene Auskuenfte, und die Kachel unterscheidet sie.
     */
    @Test
    @DisplayName(
        "Bei anzahl = 0 erscheint sie trotzdem, wenn der Mandant suspendierende Ablaeufe hat")
    void null_ist_eine_auskunft() {
      bestandMit(List.of(), List.of());
      when(repository.hatWartendeAblaeufe(any())).thenReturn(true);
      when(repository.offeneNachrichten(any(), eq(MessageStatusKind.WARTEND)))
          .thenReturn(java.util.Optional.of(new Offenstand(0, null)));

      assertThat(antwort().kacheln().wartend())
          .as("Eine Null, die etwas sagt — nicht eine fehlende Kachel")
          .isEqualTo(new OffeneKachelResponse(0L, null, true));
    }

    /**
     * <b>Fehlt sie, dann ganz.</b> Kein {@code null}, kein {@code sichtbar: false} — ein Feld, das
     * seine eigene Abwesenheit beschriebe, verlangte von der Oberflaeche zwei Pruefungen statt
     * einer.
     */
    @Test
    @DisplayName("Ohne suspendierende Ablaeufe fehlt die Kachel ganz")
    void ohne_suspendierende_ablaeufe_fehlt_sie() {
      bestandMit(List.of(), List.of());
      when(repository.hatWartendeAblaeufe(any())).thenReturn(false);

      assertThat(antwort().kacheln().wartend()).isNull();
      assertThat(antwort().kacheln().laeuft())
          .as("Laeuft ist davon unberuehrt und immer da")
          .isNotNull();
    }

    /**
     * <b>Die Bedingung wird bei jedem Aufruf mitgelesen, nicht bedingt.</b> Sonst haenge die Zahl
     * der Statements am Mandanten, und {@code DashboardStatementsTest} waere nicht mehr
     * deterministisch — die Zahl der Zugriffe ist die Groesse, die dieses Projekt statt der Uhr
     * prueft (Regel T1).
     */
    @Test
    @DisplayName("Beide Statements laufen auch dann, wenn die Kachel nicht erscheint")
    void beide_statements_laufen_immer() {
      bestandMit(List.of(), List.of());
      when(repository.hatWartendeAblaeufe(any())).thenReturn(false);

      antwort();

      verify(repository).hatWartendeAblaeufe(any());
      verify(repository).offeneNachrichten(any(), eq(MessageStatusKind.WARTEND));
      verify(repository).offeneNachrichten(any(), eq(MessageStatusKind.LAEUFT));
    }
  }

  // ─── Standardfenster und Leerzustand ──────────────────────────────────────────

  @Nested
  @DisplayName("Das Standardfenster richtet sich nach dem Mandanten")
  class Standardfenster {

    @Test
    @DisplayName("Traegt das engste Paar, wird nicht weitergesucht")
    void erstes_paar_traegt() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 1)), List.of());

      assertThat(antwort().zeitraum()).isEqualTo("48H");
      verify(repository).belegung(any(), eq(Rollupzeitraum.STUNDEN_48), any());
      verify(repository, org.mockito.Mockito.never())
          .belegung(any(), eq(Rollupzeitraum.TAGE_30), any());
    }

    @Test
    @DisplayName("Zu wenige belegte Eimer: das naechstweitere Paar kommt dran")
    void zu_wenige_eimer() {
      bestandMit(List.of(), List.of());
      when(repository.belegung(any(), eq(Rollupzeitraum.STUNDEN_48), any()))
          .thenReturn(new Belegung(23, 9_999));
      when(repository.belegung(any(), eq(Rollupzeitraum.TAGE_30), any()))
          .thenReturn(new Belegung(30, 9_999));

      assertThat(antwort().zeitraum()).isEqualTo("30T");
    }

    @Test
    @DisplayName("Genau die Haelfte belegt genuegt — die Schwelle ist einschliessend")
    void haelfte_genuegt() {
      bestandMit(List.of(), List.of());
      when(repository.belegung(any(), eq(Rollupzeitraum.STUNDEN_48), any()))
          .thenReturn(new Belegung(24, 6));

      assertThat(antwort().zeitraum()).isEqualTo("48H");
    }

    @Test
    @DisplayName(
        "Fuenf Nachrichten im groessten Eimer genuegen nicht — verlangt ist mehr als fuenf")
    void zweite_bedingung_greift() {
      bestandMit(List.of(), List.of());
      when(repository.belegung(any(), eq(Rollupzeitraum.STUNDEN_48), any()))
          .thenReturn(new Belegung(48, 5));
      when(repository.belegung(any(), eq(Rollupzeitraum.TAGE_30), any()))
          .thenReturn(new Belegung(30, 6));

      assertThat(antwort().zeitraum()).isEqualTo("30T");
    }

    @Test
    @DisplayName("Erfuellt keines der drei beide Bedingungen, steht das erste Paar da")
    void keines_traegt() {
      bestandMit(List.of(), List.of());
      when(repository.belegung(any(), any(), any())).thenReturn(new Belegung(0, 0));

      assertThat(antwort().zeitraum())
          .as("Die Oberflaeche braucht ein Paar zum Hervorheben, auch im Leerzustand")
          .isEqualTo("48H");
    }

    @Test
    @DisplayName("Ein ausdruecklich genanntes Paar wird nicht ueberstimmt — und nicht geprueft")
    void ausdruecklich_genannt() {
      bestandMit(List.of(), List.of());

      DashboardResponse antwort = service().landingpage(MANDANT, Rollupzeitraum.MONATE_12);

      assertThat(antwort.zeitraum()).isEqualTo("12M");
      verify(repository, org.mockito.Mockito.never()).belegung(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("Der Leerzustand")
  class Leerzustand {

    @Test
    @DisplayName("Ohne eine einzige Nachricht ist die Antwort leer — und trotzdem vollstaendig")
    void leer() {
      bestandMit(List.of(), List.of());
      when(repository.belegung(any(), any(), any())).thenReturn(new Belegung(0, 0));

      DashboardResponse antwort = antwort();

      assertThat(antwort.leer()).isTrue();
      assertThat(antwort.zeitraum()).isNotNull();
      assertThat(antwort.fenster()).isNotNull();
      assertThat(antwort.verteilung().partner().zeilen())
          .as("Auch im Leerzustand sagt der Katalog etwas: alles nicht zugeordnet, naemlich null")
          .containsExactly(VerteilungszeileResponse.nichtZugeordnet(0));
      assertThat(antwort.verteilung().richtung().zeilen())
          .as("Und in der Richtungssicht dasselbe")
          .containsExactly(VerteilungszeileResponse.nichtZugeordnet(0));
    }

    @Test
    @DisplayName("Eine einzige Nachricht genuegt, und der Leerzustand faellt weg")
    void nicht_leer() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 1)), List.of());

      assertThat(antwort().leer()).isFalse();
    }
  }

  // ─── Ein Aufruf ───────────────────────────────────────────────────────────────

  /**
   * <b>Ein Aufruf, eine Antwort.</b> Der Service liest genau einmal je Block und nicht einmal je
   * Kennzahl — insbesondere kommen Verlauf, Kachel <i>Nachrichten</i> und Kachel <i>Fehler</i> aus
   * <b>einem</b> Lesevorgang.
   */
  @Test
  @DisplayName("Verlauf und beide Rollup-Kacheln kosten zusammen genau einen Lesevorgang")
  void ein_lesevorgang_fuer_drei_bloecke() {
    bestandMit(
        List.of(new Rollupsumme(EIMER, "FINISHED", 10), new Rollupsumme(EIMER, "ERROR_TIMEOUT", 1)),
        List.of());

    DashboardResponse antwort = antwort();

    verify(repository, org.mockito.Mockito.times(1))
        .verlauf(eq(MANDANT), any(Rollupzeitraum.class), any(Zeitfenster.class));
    assertThat(antwort.kacheln().nachrichten()).isEqualTo(11);
    assertThat(antwort.kacheln().fehler().anzahl()).isEqualTo(1);
    assertThat(antwort.verlauf()).hasSize(1);
  }

  @Test
  @DisplayName("Ohne abgeschlossenen Lauf ist der Stand null und nicht ein erfundener Zeitpunkt")
  void stand_ohne_lauf() {
    bestandMit(List.of(), List.of());

    assertThat(antwort().stand()).isNull();
  }

  @Test
  @DisplayName("Mit Lauf traegt der Stand Zeitpunkt und Laufart, den Zeitpunkt in UTC")
  void stand_mit_lauf() {
    bestandMit(List.of(), List.of());
    when(repository.letzterLauf())
        .thenReturn(
            java.util.Optional.of(
                new Standzeile("VOLL", LocalDateTime.parse("2026-08-31T02:15:30"))));

    assertThat(antwort().stand())
        .as("rollup_lauf traegt UTC und nicht die Wanduhrzeit des Quellservers")
        .isEqualTo(new StandResponse(Instant.parse("2026-08-31T02:15:30Z"), "VOLL"));
  }

  // ─── Block 8: der plattformweite Teil (Schritt 10d) ──────────────────────────

  /**
   * Drei erfundene Dienste: einer meldet sich, einer ist ueber der Zeit, einer traegt ein Wort, das
   * niemand kennt. <b>Keine Kennung und keine Zahl aus dem Bestand</b> (Regeln G1, T2).
   */
  private void diensteSind() {
    when(dienstRepository.dienste())
        .thenReturn(
            List.of(
                new Dienstzeile("DIENST_ERFUNDEN_A", "HEARTBEAT", MELDET_SICH_SEIT),
                new Dienstzeile("DIENST_ERFUNDEN_B", "ERROR_TIMEOUT", MELDET_SICH_SEIT),
                new Dienstzeile("DIENST_ERFUNDEN_C", "WAS_AUCH_IMMER", null)));
  }

  /** 92 Sekunden vor dem Anker — eine erfundene Groesse wie jede andere in diesem Test. */
  private static final LocalDateTime MELDET_SICH_SEIT = LocalDateTime.parse("2025-12-30T05:08:15");

  @Test
  @DisplayName("Jeder Dienst wird eingeordnet, der Rohwert steht daneben")
  void dienste_werden_eingeordnet() {
    bestandMit(List.of(), List.of());
    diensteSind();

    List<DienstResponse> dienste = antwort().plattform().dienste();

    assertThat(dienste)
        .extracting(DienstResponse::serviceId, DienstResponse::zustand, DienstResponse::rohwert)
        .containsExactly(
            tuple("DIENST_ERFUNDEN_A", Dienstzustand.MELDET_SICH, "HEARTBEAT"),
            tuple("DIENST_ERFUNDEN_B", Dienstzustand.ZEITUEBERSCHRITTEN, "ERROR_TIMEOUT"),
            tuple("DIENST_ERFUNDEN_C", Dienstzustand.UNGEKLAERT, "WAS_AUCH_IMMER"));
  }

  @Test
  @DisplayName("Das Alter einer Lampe rechnet gegen die Anwendungsuhr (E-75)")
  void alter_einer_lampe() {
    bestandMit(List.of(), List.of());
    diensteSind();

    List<DienstResponse> dienste = antwort().plattform().dienste();

    assertThat(dienste.getFirst().alterSekunden()).isEqualTo(92L);
    assertThat(dienste.getFirst().stand()).isEqualTo(Instant.parse("2025-12-30T04:08:15Z"));
  }

  @Test
  @DisplayName("Ohne ServiceLastUpdate gibt es keinen Stand und kein Alter")
  void ohne_stand_kein_alter() {
    bestandMit(List.of(), List.of());
    diensteSind();

    DienstResponse ohneStand = antwort().plattform().dienste().getLast();

    assertThat(ohneStand.stand()).isNull();
    assertThat(ohneStand.alterSekunden())
        .as("Eine Null waere hier \u201eseit null Sekunden\u201c und damit eine Erfindung")
        .isNull();
  }

  @Test
  @DisplayName("Ein Stand nach jetzt hat kein Alter — lokal der Fall von MPSERVICEPROD01")
  void stand_nach_jetzt() {
    bestandMit(List.of(), List.of());
    when(dienstRepository.dienste())
        .thenReturn(
            List.of(
                new Dienstzeile(
                    "DIENST_ERFUNDEN_A", "HEARTBEAT", LocalDateTime.parse("2026-07-13T15:01:44"))));

    DienstResponse dienst = antwort().plattform().dienste().getFirst();

    assertThat(dienst.stand()).as("Der Zeitpunkt selbst wird geliefert").isNotNull();
    assertThat(dienst.alterSekunden())
        .as("\u201emeldet sich seit minus drei Wochen\u201c ist schlechter als keine Angabe")
        .isNull();
  }

  @Test
  @DisplayName("Ist die Pruefung abgeschaltet, sagt die Kachel das mit Grund")
  void ablagenkachel_ist_abgeschaltet() {
    bestandMit(List.of(), List.of());

    AblagenResponse ablagen = antwort().plattform().ablagen();

    assertThat(ablagen.zustand()).isEqualTo(Ablagenzustand.UNGEKLAERT);
    assertThat(ablagen.grund()).isEqualTo(Ablagengrund.ABGESCHALTET);
    assertThat(ablagen.ziele()).isEmpty();
    assertThat(ablagen.geprueftAm()).isNull();
  }

  @Test
  @DisplayName("Der Block steht auch dann da, wenn es keinen Dienst mit Zeitgrenze gibt")
  void plattform_steht_immer() {
    bestandMit(List.of(), List.of());

    assertThat(antwort().plattform()).isNotNull();
    assertThat(antwort().plattform().dienste()).isEmpty();
    assertThat(antwort().plattform().ablagen())
        .as(
            "Eine fehlende Kachel waere Abwesenheit, und die sieht aus wie \u201enichts zu melden\u201c")
        .isNotNull();
  }

  // ─── Der Live-Rest der laufenden Stunde (Teil B, E-190, E-191) ───────────────

  /**
   * <b>Die Korrektur des Bausteins in den Eimern des Dashboards.</b> Die Zeilen des Bausteins sind
   * erfunden und schon verrechnet (vorzeichenbehaftet je Stunde, Prozess und Rohstatus); gestellt
   * wird nur, was der Dienst daraus macht. <b>Kein Wert aus dem Bestand</b> (Regel T2), keine
   * Wanduhr (Regel T1, die Uhr steht).
   *
   * <p>Das Fenster am gestellten {@code jetzt} (05:09:47 in Europe/Berlin): {@code 48H} liest
   * {@code 2025-12-28 06:00} bis {@code 2025-12-30 06:00}, ausschliessend.
   */
  @Nested
  @DisplayName("Der Live-Rest — die Korrektur in den Eimern des Dashboards")
  class LiveRest {

    private static final LocalDateTime G = LocalDateTime.parse("2025-12-30T03:00:00");
    private static final LocalDateTime LIVE_BIS = LocalDateTime.parse("2025-12-30T06:00:00");

    private void angewandt(LiveRestZeile... zeilen) {
      when(liveRest.ermittle(any(), any()))
          .thenReturn(
              new LiveRestErgebnis(
                  LiveRestEntscheidung.angewandt(G, LIVE_BIS),
                  new LiveRestKorrektur(List.of(zeilen), Map.of())));
    }

    private static LiveRestZeile zeile(String stunde, String prozess, String status, long anzahl) {
      return new LiveRestZeile(LocalDateTime.parse(stunde), prozess, status, anzahl);
    }

    private DashboardResponse antwortFuer(Rollupzeitraum zeitraum) {
      return service().landingpage(MANDANT, zeitraum);
    }

    @Test
    @DisplayName("48H: die Korrektur landet im Stundeneimer, nach Einordnung und Fehlerart")
    void stundeneimer() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 10)), List.of());
      angewandt(
          zeile("2025-12-30T03:00", "P-1", "FINISHED", 2),
          zeile("2025-12-30T03:00", "P-1", "ERROR_TIMEOUT", 1));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.verlauf()).hasSize(1);
      assertThat(antwort.verlauf().getFirst().eimer())
          .isEqualTo(Instant.parse("2025-12-30T02:00:00Z"));
      assertThat(antwort.verlauf().getFirst().gesamt()).isEqualTo(13);
      assertThat(antwort.verlauf().getFirst().einordnungen())
          .extracting(EinordnungszahlResponse::einordnung, EinordnungszahlResponse::anzahl)
          .containsExactlyInAnyOrder(
              tuple(MessageStatusKind.ABGESCHLOSSEN, 12L), tuple(MessageStatusKind.FEHLER, 1L));
      assertThat(antwort.kacheln().nachrichten()).isEqualTo(13);
      assertThat(antwort.kacheln().fehler().anzahl()).isEqualTo(1);
      assertThat(antwort.kacheln().fehler().arten())
          .extracting(FehlerartResponse::rohwert, FehlerartResponse::art, FehlerartResponse::anzahl)
          .containsExactly(tuple("ERROR_TIMEOUT", "TIMEOUT", 1L));
      assertThat(antwort.leer()).isFalse();
    }

    @Test
    @DisplayName("30T: zwei Stunden desselben Tages landen im Tageseimer")
    void tageseimer() {
      LocalDateTime tag = LocalDateTime.parse("2025-12-30T00:00:00");
      bestandMit(List.of(new Rollupsumme(tag, "FINISHED", 10)), List.of());
      angewandt(
          zeile("2025-12-30T03:00", "P-1", "FINISHED", 2),
          zeile("2025-12-30T04:00", "P-1", "FINISHED", 3));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.TAGE_30);

      assertThat(antwort.verlauf()).hasSize(1);
      assertThat(antwort.verlauf().getFirst().eimer())
          .as("Der Tageseimer, wie der Rollup ihn bildet: DATE(stunde)")
          .isEqualTo(Instant.parse("2025-12-29T23:00:00Z"));
      assertThat(antwort.verlauf().getFirst().gesamt()).isEqualTo(15);
      assertThat(antwort.kacheln().nachrichten()).isEqualTo(15);
    }

    @Test
    @DisplayName("12M: die Stunde landet im Eimer des Monatsersten")
    void monatseimer() {
      LocalDateTime monat = LocalDateTime.parse("2025-12-01T00:00:00");
      bestandMit(List.of(new Rollupsumme(monat, "FINISHED", 10)), List.of());
      angewandt(zeile("2025-12-30T04:00", "P-1", "FINISHED", 5));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.MONATE_12);

      assertThat(antwort.verlauf()).hasSize(1);
      assertThat(antwort.verlauf().getFirst().eimer())
          .isEqualTo(Instant.parse("2025-11-30T23:00:00Z"));
      assertThat(antwort.verlauf().getFirst().gesamt()).isEqualTo(15);
    }

    @Test
    @DisplayName("Nur Zeilen im Fenster zaehlen — von einschliessend, bis ausschliessend")
    void nur_im_fenster() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 10)), List.of());
      angewandt(
          zeile("2025-12-28T05:00", "P-1", "FINISHED", 7),
          zeile("2025-12-28T06:00", "P-1", "FINISHED", 1),
          zeile("2025-12-30T06:00", "P-1", "FINISHED", 7));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.kacheln().nachrichten())
          .as("Die Stunde vor dem Fenster und die Stunde an seiner oberen Grenze zaehlen nicht")
          .isEqualTo(11);
      assertThat(antwort.verlauf()).hasSize(2);
    }

    @Test
    @DisplayName("Kein negativer Endwert: was auf null faellt, verschwindet — auch der Eimer")
    void kein_negativer_endwert() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 3)), List.of());
      angewandt(zeile("2025-12-30T03:00", "P-1", "FINISHED", -5));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.kacheln().nachrichten()).isZero();
      assertThat(antwort.verlauf()).isEmpty();
      assertThat(antwort.leer()).as("Ohne eine einzige Zahl ist die Seite leer").isTrue();
    }

    @Test
    @DisplayName(
        "Nur Live-Verkehr, keine Rollupzeile: der Eimer entsteht, die Seite ist nicht leer")
    void nur_live_verkehr() {
      bestandMit(List.of(), List.of());
      angewandt(zeile("2025-12-30T05:00", "P-1", "FINISHED", 4));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.leer()).isFalse();
      assertThat(antwort.verlauf()).hasSize(1);
      assertThat(antwort.verlauf().getFirst().eimer())
          .isEqualTo(Instant.parse("2025-12-30T04:00:00Z"));
      assertThat(antwort.kacheln().nachrichten()).isEqualTo(4);
    }

    @Test
    @DisplayName("Block 5 bekommt die Korrektur je Schluessel — ueber die Katalog-Nachlesung")
    void verteilung_je_schluessel() {
      bestandMit(
          List.of(new Rollupsumme(EIMER, "FINISHED", 105)),
          List.of(partner("BMW", 100), new Verteilungssumme(null, 5)));
      when(repository.verteilung(any(), any(), any(), eq(Verteilungssicht.RICHTUNG)))
          .thenReturn(
              List.of(new Verteilungssumme("EINGEHEND", 100), new Verteilungssumme(null, 5)));
      angewandt(
          zeile("2025-12-30T03:00", "P-1", "FINISHED", 3),
          zeile("2025-12-30T04:00", "P-2", "FINISHED", 2));
      when(repository.katalogzuordnung(any(), any()))
          .thenReturn(List.of(new Katalogzuordnungszeile("P-1", "BMW", "EINGEHEND")));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      verify(repository).katalogzuordnung(eq(MANDANT), eq(Set.of("P-1", "P-2")));
      assertThat(antwort.verteilung().partner().zeilen())
          .extracting(VerteilungszeileResponse::wert, VerteilungszeileResponse::anzahl)
          .as("P-2 hat keine Katalogzeile und zaehlt als nicht zugeordnet")
          .containsExactly(tuple("BMW", 103L), tuple(null, 7L));
      assertThat(antwort.verteilung().richtung().zeilen())
          .extracting(VerteilungszeileResponse::wert, VerteilungszeileResponse::anzahl)
          .containsExactly(tuple("EINGEHEND", 103L), tuple(null, 7L));
      assertThat(antwort.kacheln().nachrichten())
          .as("Kachel und beide Sichten zaehlen dieselbe Zahl")
          .isEqualTo(110);
    }

    @Test
    @DisplayName("Ohne Korrekturzeilen im Fenster laeuft keine Nachlesung")
    void keine_nachlesung_ohne_korrekturzeilen() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 10)), List.of());
      angewandt();
      antwortFuer(Rollupzeitraum.STUNDEN_48);

      angewandt(zeile("2025-12-28T05:00", "P-1", "FINISHED", 7));
      antwortFuer(Rollupzeitraum.STUNDEN_48);

      verify(repository, never()).katalogzuordnung(any(), any());
    }

    @Test
    @DisplayName("Auch die Verteilung klemmt auf null: ein Wert, der auf null faellt, verschwindet")
    void verteilung_klemmt_auf_null() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 5)), List.of(partner("BMW", 5)));
      angewandt(zeile("2025-12-30T03:00", "P-1", "FINISHED", -8));
      when(repository.katalogzuordnung(any(), any()))
          .thenReturn(List.of(new Katalogzuordnungszeile("P-1", "BMW", null)));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.verteilung().partner().zeilen())
          .extracting(VerteilungszeileResponse::art, VerteilungszeileResponse::anzahl)
          .as("Kein BMW mehr, aber nicht zugeordnet steht immer — auch mit null")
          .containsExactly(tuple(Verteilungszeilenart.NICHT_ZUGEORDNET, 0L));
    }

    /**
     * Die Datenbank gruppiert unter {@code utf8mb4_general_ci}; zwei Schreibweisen sind dort eine
     * Gruppe. Die Nachlesung liefert die Schreibweise der Katalogzeile — und die darf keine zweite
     * Zeile eroeffnen.
     */
    @Test
    @DisplayName("Zwei Schreibweisen desselben Schluessels bleiben eine Zeile")
    void schreibweisen_fallen_zusammen() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 10)), List.of(partner("BMW", 10)));
      angewandt(zeile("2025-12-30T03:00", "P-1", "FINISHED", 2));
      when(repository.katalogzuordnung(any(), any()))
          .thenReturn(List.of(new Katalogzuordnungszeile("P-1", "Bmw", null)));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.verteilung().partner().zeilen())
          .extracting(VerteilungszeileResponse::wert, VerteilungszeileResponse::anzahl)
          .containsExactly(tuple("BMW", 12L), tuple(null, 0L));
    }

    @Test
    @DisplayName("Der Block liveRest steht in allen drei Zustaenden, G in UTC")
    void block_in_drei_zustaenden() {
      bestandMit(List.of(), List.of());

      angewandt();
      assertThat(antwortFuer(Rollupzeitraum.STUNDEN_48).liveRest())
          .isEqualTo(new LiveRestResponse(LiveRestZustand.ANGEWANDT, null));

      when(liveRest.ermittle(any(), any()))
          .thenReturn(LiveRestErgebnis.ohneKorrektur(LiveRestEntscheidung.nichtNoetig()));
      assertThat(antwortFuer(Rollupzeitraum.STUNDEN_48).liveRest())
          .isEqualTo(new LiveRestResponse(LiveRestZustand.NICHT_NOETIG, null));

      when(liveRest.ermittle(any(), any()))
          .thenReturn(
              LiveRestErgebnis.ohneKorrektur(
                  LiveRestEntscheidung.ausgesetztAb(LocalDateTime.parse("2025-12-30T02:00"))));
      assertThat(antwortFuer(Rollupzeitraum.STUNDEN_48).liveRest())
          .as("G ist Wanduhrzeit der Quelle und wird nach UTC gerechnet")
          .isEqualTo(
              new LiveRestResponse(
                  LiveRestZustand.AUSGESETZT, Instant.parse("2025-12-30T01:00:00Z")));
    }

    @Test
    @DisplayName("Fenster und Live-Rest bekommen denselben Uhrenschlag")
    void derselbe_stichtag() {
      bestandMit(List.of(), List.of());

      antwortFuer(Rollupzeitraum.STUNDEN_48);

      verify(liveRest).ermittle(eq(MANDANT), eq(LocalDateTime.now(Clock.fixed(JETZT, ZONE))));
    }

    @Test
    @DisplayName("Die Belegungsprobe bleibt ohne Korrektur — sie entscheidet vor dem Live-Rest")
    void belegungsprobe_ohne_korrektur() {
      bestandMit(List.of(), List.of());
      when(repository.belegung(any(), any(), any())).thenReturn(new Belegung(0, 0));
      angewandt(zeile("2025-12-30T05:00", "P-1", "FINISHED", 4));

      DashboardResponse antwort = antwort();

      assertThat(antwort.zeitraum()).as("Kein Paar traegt: das erste der Reihe").isEqualTo("48H");
      assertThat(antwort.leer())
          .as("… und trotzdem ist die Seite mit dem Live-Verkehr nicht leer")
          .isFalse();
      verify(repository).belegung(any(), eq(Rollupzeitraum.MONATE_12), any());
    }
  }

  // ─── Fehler live (E-208, 18.09.2026) ─────────────────────────────────────────

  /**
   * <b>Die Fehler aus der Live-Lesung</b> ({@code docs/fehler-live.md} §5). Die Lesung ist eine
   * Attrappe mit erfundenen Zeilen, die Uhr steht (Regeln T1, T2). Die Faelle hier stellen sie auf
   * „angewandt" oder lassen sie ausdruecklich „ausgesetzt".
   *
   * <p>Das Fenster am gestellten {@code jetzt} (05:09:47 in Europe/Berlin): {@code 48H} liest
   * {@code 2025-12-28 06:00} bis {@code 2025-12-30 06:00}, {@code 30T} den Dezember bis zum 30.,
   * {@code 12M} das Jahr 2025.
   */
  @Nested
  @DisplayName("Fehler live — die Fehler aus der Lesung, ersetzt nach dem Live-Rest")
  class FehlerLive {

    private void angewandt(FehlerLiveZeile... zeilen) {
      when(fehlerLive.ermittle(any(), any(), any()))
          .thenReturn(FehlerLiveErgebnis.angewandt(List.of(zeilen)));
    }

    private static FehlerLiveZeile fehler(
        String stunde, String prozess, String status, long anzahl) {
      return new FehlerLiveZeile(LocalDateTime.parse(stunde), prozess, status, anzahl);
    }

    private static LiveRestZeile korrektur(
        String stunde, String prozess, String status, long anzahl) {
      return new LiveRestZeile(LocalDateTime.parse(stunde), prozess, status, anzahl);
    }

    /** Der Verteilungsblock ohne die Fehler des Rollups — nur bei angewandter Lesung gerufen. */
    private void verteilungOhneFehler(List<Verteilungssumme> zeilen) {
      when(repository.verteilungOhneFehler(any(), any(), any(), any())).thenReturn(zeilen);
    }

    private static long summe(List<VerteilungszeileResponse> zeilen) {
      return zeilen.stream().mapToLong(VerteilungszeileResponse::anzahl).sum();
    }

    /**
     * <b>Der Fall aus der Produktion, gemeldet am 18.09.2026.</b> Eine Nachricht stand um 09:00 auf
     * {@code ERROR_TIMEOUT}; nach der Nachverarbeitung steht sie auf {@code RUNNING} und ist in die
     * laufende Stunde gewandert. Der Rollup haelt den Fehler im Eimer 09:00 bis zum Volllauf, der
     * Live-Rest kennt die Nachricht in der Stunde 14:00, und die Lesung findet keinen Fehler mehr.
     *
     * <p><b>Vorher und nachher, mit denselben Zeilen:</b> Ausgesetzt zaehlt die Seite die Nachricht
     * zweimal und einmal als Fehler — das ist die Meldung. Angewandt zaehlt sie sie einmal, und
     * nicht als Fehler.
     */
    @Test
    @DisplayName("Der Fall aus der Produktion: nachverarbeitet, kein Fehler mehr, einmal gezaehlt")
    void der_fall_aus_der_produktion() {
      // 14:30 in Europe/Berlin; 48H liest 2025-12-28 15:00 bis 2025-12-30 15:00, G ist 13:00.
      Instant umHalbDrei = Instant.parse("2025-12-30T13:30:00Z");
      LocalDateTime neunUhr = LocalDateTime.parse("2025-12-30T09:00:00");
      bestandMit(List.of(new Rollupsumme(neunUhr, "ERROR_TIMEOUT", 1)), List.of());
      when(liveRest.ermittle(any(), any()))
          .thenReturn(
              new LiveRestErgebnis(
                  LiveRestEntscheidung.angewandt(
                      LocalDateTime.parse("2025-12-30T13:00"),
                      LocalDateTime.parse("2025-12-30T15:00")),
                  new LiveRestKorrektur(
                      List.of(korrektur("2025-12-30T14:00", "P-1", "RUNNING", 1)), Map.of())));

      DashboardResponse vorher =
          service(umHalbDrei).landingpage(MANDANT, Rollupzeitraum.STUNDEN_48);

      assertThat(vorher.kacheln().fehler().anzahl())
          .as("Ausgesetzt: der alte Eimer behaelt den Fehler — die Meldung aus der Produktion")
          .isEqualTo(1);
      assertThat(vorher.kacheln().nachrichten())
          .as("… und die Nachricht zaehlt doppelt")
          .isEqualTo(2);

      angewandt();
      verteilungOhneFehler(List.of());
      DashboardResponse nachher =
          service(umHalbDrei).landingpage(MANDANT, Rollupzeitraum.STUNDEN_48);

      assertThat(nachher.kacheln().fehler().anzahl()).isZero();
      assertThat(nachher.kacheln().fehler().arten()).isEmpty();
      assertThat(nachher.kacheln().nachrichten()).isEqualTo(1);
      assertThat(nachher.verlauf())
          .as(
              "Im Eimer 09:00 steht kein Fehleranteil mehr — und ohne Zeile gibt es den Eimer nicht")
          .extracting(VerlaufspunktResponse::eimer)
          .containsExactly(Instant.parse("2025-12-30T13:00:00Z"));
      assertThat(nachher.verlauf().getFirst().einordnungen())
          .containsExactly(new EinordnungszahlResponse(MessageStatusKind.LAEUFT, 1));
      assertThat(summe(nachher.verteilung().partner().zeilen()))
          .as("Kachel und Sicht zaehlen dieselbe Nachricht")
          .isEqualTo(1);
      assertThat(nachher.fehlerLive())
          .isEqualTo(new FehlerLiveResponse(FehlerLiveZustand.ANGEWANDT));
    }

    /**
     * Ein Fehler in der laufenden Stunde: Der Live-Rest rechnet ihn dazu, und die Lesung kennt ihn
     * auch. <b>Er zaehlt einmal</b> — der Ersatz nimmt die Fehlerzeile des Live-Rests heraus.
     */
    @Test
    @DisplayName("Ein Fehler im Live-Bereich zaehlt einmal, obwohl Live-Rest und Lesung ihn kennen")
    void fehler_im_live_bereich_zaehlt_einmal() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 10)), List.of());
      when(liveRest.ermittle(any(), any()))
          .thenReturn(
              new LiveRestErgebnis(
                  LiveRestEntscheidung.angewandt(
                      LocalDateTime.parse("2025-12-30T03:00"),
                      LocalDateTime.parse("2025-12-30T06:00")),
                  new LiveRestKorrektur(
                      List.of(korrektur("2025-12-30T04:00", "P-1", "ERROR_TIMEOUT", 1)),
                      Map.of())));
      angewandt(fehler("2025-12-30T04:00", "P-1", "ERROR_TIMEOUT", 1));
      verteilungOhneFehler(List.of(partner("BMW", 10)));

      DashboardResponse antwort = service().landingpage(MANDANT, Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.kacheln().fehler().anzahl()).isEqualTo(1);
      assertThat(antwort.kacheln().nachrichten()).isEqualTo(11);
      assertThat(summe(antwort.verteilung().partner().zeilen()))
          .as("Auch Block 5 zaehlt ihn einmal: die Fehlerzeile der Korrektur geht nicht hinein")
          .isEqualTo(11);
    }

    @Test
    @DisplayName("Hebung je Paar: 48H die Stunde, 30T der Tag, 12M der Monatserste")
    void hebung_je_paar() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 10)), List.of());
      verteilungOhneFehler(List.of());
      angewandt(fehler("2025-12-30T03:00", "P-1", "ERROR_DUPLICATE", 2));
      DashboardResponse stunde = service().landingpage(MANDANT, Rollupzeitraum.STUNDEN_48);
      assertThat(stunde.verlauf()).hasSize(1);
      assertThat(stunde.verlauf().getFirst().eimer())
          .isEqualTo(Instant.parse("2025-12-30T02:00:00Z"));
      assertThat(stunde.verlauf().getFirst().einordnungen())
          .as("In der Reihenfolge der Aufzaehlung, wie in jedem Eimer")
          .containsExactly(
              new EinordnungszahlResponse(MessageStatusKind.FEHLER, 2),
              new EinordnungszahlResponse(MessageStatusKind.ABGESCHLOSSEN, 10));

      LocalDateTime tag = LocalDateTime.parse("2025-12-29T00:00:00");
      bestandMit(List.of(new Rollupsumme(tag, "FINISHED", 10)), List.of());
      angewandt(
          fehler("2025-12-29T03:00", "P-1", "ERROR_DUPLICATE", 2),
          fehler("2025-12-29T17:00", "P-2", "ERROR_DUPLICATE", 1));
      DashboardResponse tage = service().landingpage(MANDANT, Rollupzeitraum.TAGE_30);
      assertThat(tage.verlauf()).as("Zwei Stunden desselben Tages, ein Tageseimer").hasSize(1);
      assertThat(tage.verlauf().getFirst().eimer())
          .as("Der Tageseimer, wie der Rollup ihn bildet: DATE(stunde)")
          .isEqualTo(Instant.parse("2025-12-28T23:00:00Z"));
      assertThat(tage.kacheln().fehler().anzahl()).isEqualTo(3);

      LocalDateTime monat = LocalDateTime.parse("2025-12-01T00:00:00");
      bestandMit(List.of(new Rollupsumme(monat, "FINISHED", 10)), List.of());
      angewandt(fehler("2025-12-10T08:00", "P-1", "COMMIT_REJECTED", 1));
      DashboardResponse monate = service().landingpage(MANDANT, Rollupzeitraum.MONATE_12);
      assertThat(monate.verlauf()).hasSize(1);
      assertThat(monate.verlauf().getFirst().eimer())
          .as("Der Eimer des Monatsersten")
          .isEqualTo(Instant.parse("2025-11-30T23:00:00Z"));
      assertThat(monate.verlauf().getFirst().gesamt()).isEqualTo(11);
    }

    @Test
    @DisplayName("Kachel Fehler und Fehlerarten kommen aus der Lesung, nicht aus dem Rollup")
    void kachel_und_fehlerarten_aus_der_lesung() {
      bestandMit(
          List.of(
              new Rollupsumme(EIMER, "FINISHED", 10), new Rollupsumme(EIMER, "ERROR_TIMEOUT", 5)),
          List.of());
      verteilungOhneFehler(List.of());
      angewandt(
          fehler("2025-12-30T03:00", "P-1", "ERROR_DUPLICATE", 2),
          fehler("2025-12-30T03:00", "P-2", "COMMIT_REJECTED", 1));

      DashboardResponse antwort = antwort();

      assertThat(antwort.kacheln().fehler().anzahl()).isEqualTo(3);
      assertThat(antwort.kacheln().fehler().arten())
          .extracting(FehlerartResponse::rohwert, FehlerartResponse::art, FehlerartResponse::anzahl)
          .as("Das ERROR_TIMEOUT des Rollups ist ersetzt — es steht nirgends mehr")
          .containsExactly(
              tuple("ERROR_DUPLICATE", "DUPLICATE", 2L),
              tuple("COMMIT_REJECTED", MessageStatusClassifier.ABGELEHNT_VOM_PARTNER, 1L));
      assertThat(antwort.kacheln().nachrichten()).isEqualTo(13);
    }

    /**
     * <b>Block 5 bei angewandter Lesung:</b> Die Statements lesen den Rollup ohne Fehler, die
     * Fehlerzeilen der Lesung gehen ueber die Nachlesung dem Schluessel zu — ein Prozess ohne
     * Katalogzeile als <i>nicht zugeordnet</i>. Kachel und beide Sichten zaehlen dieselbe Zahl.
     */
    @Test
    @DisplayName("Angewandt: Block 5 ohne die Fehler des Rollups, die Lesung ueber die Nachlesung")
    void block5_angewandt() {
      bestandMit(
          List.of(
              new Rollupsumme(EIMER, "FINISHED", 10), new Rollupsumme(EIMER, "ERROR_TIMEOUT", 5)),
          List.of());
      when(repository.verteilungOhneFehler(any(), any(), any(), eq(Verteilungssicht.PARTNER)))
          .thenReturn(List.of(partner("BMW", 8), new Verteilungssumme(null, 2)));
      when(repository.verteilungOhneFehler(any(), any(), any(), eq(Verteilungssicht.RICHTUNG)))
          .thenReturn(List.of(new Verteilungssumme("EINGEHEND", 8), new Verteilungssumme(null, 2)));
      angewandt(
          fehler("2025-12-30T03:00", "P-1", "ERROR_DUPLICATE", 2),
          fehler("2025-12-30T03:00", "P-9", "COMMIT_REJECTED", 1));
      when(repository.katalogzuordnung(any(), any()))
          .thenReturn(List.of(new Katalogzuordnungszeile("P-1", "BMW", "EINGEHEND")));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      verify(repository, org.mockito.Mockito.times(2))
          .verteilungOhneFehler(any(), any(), any(), any());
      verify(repository, never()).verteilung(any(), any(), any(), any());
      verify(repository).katalogzuordnung(eq(MANDANT), eq(Set.of("P-1", "P-9")));
      assertThat(antwort.verteilung().partner().zeilen())
          .extracting(VerteilungszeileResponse::wert, VerteilungszeileResponse::anzahl)
          .as("P-9 hat keine Katalogzeile und zaehlt als nicht zugeordnet")
          .containsExactly(tuple("BMW", 10L), tuple(null, 3L));
      assertThat(antwort.verteilung().richtung().zeilen())
          .extracting(VerteilungszeileResponse::wert, VerteilungszeileResponse::anzahl)
          .containsExactly(tuple("EINGEHEND", 10L), tuple(null, 3L));
      assertThat(antwort.kacheln().nachrichten())
          .as("Kachel und beide Sichten zaehlen dieselbe Zahl")
          .isEqualTo(13);
    }

    /**
     * <b>Block 5 bei ausgesetzter Lesung:</b> beide Statements im heutigen Wortlaut, keine
     * Nachlesung — Kachel und Sichten nehmen die Fehler beide aus dem Rollup und zaehlen gleich.
     */
    @Test
    @DisplayName("Ausgesetzt: Block 5 im heutigen Wortlaut, ohne Nachlesung, und er zaehlt gleich")
    void block5_ausgesetzt() {
      bestandMit(
          List.of(
              new Rollupsumme(EIMER, "FINISHED", 10), new Rollupsumme(EIMER, "ERROR_TIMEOUT", 5)),
          List.of(partner("BMW", 12), new Verteilungssumme(null, 3)));

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      verify(repository, org.mockito.Mockito.times(2)).verteilung(any(), any(), any(), any());
      verify(repository, never()).verteilungOhneFehler(any(), any(), any(), any());
      verify(repository, never()).katalogzuordnung(any(), any());
      assertThat(summe(antwort.verteilung().partner().zeilen())).isEqualTo(15);
      assertThat(antwort.kacheln().nachrichten()).isEqualTo(15);
    }

    @Test
    @DisplayName(
        "Der Leerzustand folgt der Kachel Nachrichten nach dem Ersatz — in beide Richtungen")
    void leerzustand_nach_dem_ersatz() {
      bestandMit(List.of(new Rollupsumme(EIMER, "ERROR_TIMEOUT", 1)), List.of());
      verteilungOhneFehler(List.of());
      angewandt();

      DashboardResponse nurDerAlteFehler = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(nurDerAlteFehler.leer())
          .as("Der einzige Eintrag war ein Fehler, den es nicht mehr gibt: die Seite ist leer")
          .isTrue();
      assertThat(nurDerAlteFehler.verlauf()).isEmpty();

      bestandMit(List.of(), List.of());
      angewandt(fehler("2025-12-30T03:00", "P-1", "ERROR_TIMEOUT", 1));

      DashboardResponse nurDieLesung = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(nurDieLesung.leer())
          .as("Ein Fehler, den der Rollup noch nicht kennt, macht die Seite nicht leer")
          .isFalse();
      assertThat(nurDieLesung.kacheln().fehler().anzahl()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ausgesetzt ergibt die Zahlen von vorher — samt Block fehlerLive")
    void ausgesetzt_ergibt_die_zahlen_von_vorher() {
      bestandMit(
          List.of(
              new Rollupsumme(EIMER, "FINISHED", 10), new Rollupsumme(EIMER, "ERROR_TIMEOUT", 5)),
          List.of(partner("BMW", 15)));
      when(fehlerLive.ermittle(any(), any(), any())).thenReturn(FehlerLiveErgebnis.ausgesetzt());

      DashboardResponse antwort = antwortFuer(Rollupzeitraum.STUNDEN_48);

      assertThat(antwort.kacheln().nachrichten()).isEqualTo(15);
      assertThat(antwort.kacheln().fehler().anzahl()).isEqualTo(5);
      assertThat(antwort.kacheln().fehler().arten())
          .extracting(FehlerartResponse::rohwert)
          .containsExactly("ERROR_TIMEOUT");
      assertThat(antwort.verlauf().getFirst().einordnungen())
          .containsExactly(
              new EinordnungszahlResponse(MessageStatusKind.FEHLER, 5),
              new EinordnungszahlResponse(MessageStatusKind.ABGESCHLOSSEN, 10));
      assertThat(antwort.fehlerLive())
          .isEqualTo(new FehlerLiveResponse(FehlerLiveZustand.AUSGESETZT));

      angewandt();
      verteilungOhneFehler(List.of(partner("BMW", 10)));
      assertThat(antwortFuer(Rollupzeitraum.STUNDEN_48).fehlerLive())
          .isEqualTo(new FehlerLiveResponse(FehlerLiveZustand.ANGEWANDT));
    }

    /**
     * <b>Ein Uhrenschlag:</b> Die Lesung bekommt genau das Fenster, das die Seite aus ihrem einen
     * {@code jetzt} bildet — und der Live-Rest dasselbe {@code jetzt}.
     */
    @Test
    @DisplayName("Fenster, Live-Rest und Fehlerlesung bekommen denselben Uhrenschlag")
    void ein_uhrenschlag() {
      bestandMit(List.of(), List.of());

      antwortFuer(Rollupzeitraum.STUNDEN_48);

      LocalDateTime jetzt = LocalDateTime.now(Clock.fixed(JETZT, ZONE));
      Zeitfenster fenster = Rollupzeitraum.STUNDEN_48.fenster(jetzt);
      verify(fehlerLive).ermittle(eq(MANDANT), eq(fenster.von()), eq(fenster.bis()));
      verify(liveRest).ermittle(eq(MANDANT), eq(jetzt));
      verify(repository).verlauf(eq(MANDANT), eq(Rollupzeitraum.STUNDEN_48), eq(fenster));
    }

    private DashboardResponse antwortFuer(Rollupzeitraum zeitraum) {
      return service().landingpage(MANDANT, zeitraum);
    }
  }
}
