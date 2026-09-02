package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
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

  @Mock private DashboardRepository repository;

  private DashboardService service() {
    return new DashboardService(
        repository, new MessageStatusClassifier(), Clock.fixed(JETZT, ZONE));
  }

  /** Der Normalfall: Es gibt Verkehr, das erste Paar traegt, nichts stirbt. */
  private void bestandMit(List<Rollupsumme> verlauf, List<Verteilungssumme> verteilung) {
    when(repository.belegung(any(), any(), any())).thenReturn(new Belegung(48, 999));
    when(repository.verlauf(any(), any(), any())).thenReturn(verlauf);
    when(repository.verteilung(any(), any(), any(), any())).thenReturn(verteilung);
    when(repository.ueberfaelligImFenster(any(), any(), any())).thenReturn(OptionalLong.of(3));
    when(repository.ueberfaelligInsgesamt(any(), any())).thenReturn(OptionalLong.of(7));
    when(repository.zuletztAufgefallen(any(), any(), any(), anyInt())).thenReturn(List.of());
    when(repository.letzterLauf()).thenReturn(java.util.Optional.empty());
  }

  private DashboardResponse antwort() {
    return service().landingpage(MANDANT, null, Verteilungssicht.PARTNER);
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
              UeberfaelligkachelResponse.class)) {
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

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().zeilen();

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

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().zeilen();

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

      assertThat(antwort().verteilung().zeilen().getLast())
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

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().zeilen();

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

      List<VerteilungszeileResponse> zeilen = antwort().verteilung().zeilen();

      assertThat(zeilen.get(10))
          .as("Rang 11 gehoert einem benannten Partner, nicht dem Eimer ohne Namen")
          .isEqualTo(VerteilungszeileResponse.uebrige(1, 989));
      assertThat(zeilen.getLast()).isEqualTo(VerteilungszeileResponse.nichtZugeordnet(10_000));
    }

    @Test
    @DisplayName("Die Sicht steht in der Antwort, auch wenn der Parameter fehlte")
    void sicht_steht_in_der_antwort() {
      bestandMit(List.of(), List.of());

      assertThat(
              service().landingpage(MANDANT, null, Verteilungssicht.RICHTUNG).verteilung().sicht())
          .isEqualTo(Verteilungssicht.RICHTUNG);
    }
  }

  // ─── Ueberfaellig ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Kachel Ueberfaellig")
  class Ueberfaellig {

    @Test
    @DisplayName("Beide Zahlen stehen da, wenn beide Abfragen durchlaufen")
    void beide_zahlen() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 1)), List.of());

      assertThat(antwort().kacheln().ueberfaellig())
          .isEqualTo(new UeberfaelligkachelResponse(3L, 7L, true));
    }

    /**
     * <b>Der Fall, den D.5 verlangt:</b> Stirbt die Live-Abfrage, liefert die Antwort die uebrigen
     * Bloecke und die zwei Felder als „nicht ermittelbar" — <b>und nicht als Null</b>. Eine Null
     * hiesse „es haengt nichts", und das waere in einem Ueberwachungswerkzeug die schlimmste
     * falsche Antwort.
     */
    @Test
    @DisplayName("Stirbt die Live-Abfrage, steht die Seite und die Kachel sagt „nicht ermittelbar“")
    void nicht_ermittelbar() {
      bestandMit(List.of(new Rollupsumme(EIMER, "FINISHED", 42)), List.of(partner("P", 42)));
      when(repository.ueberfaelligImFenster(any(), any(), any())).thenReturn(OptionalLong.empty());

      DashboardResponse antwort = antwort();

      assertThat(antwort.kacheln().ueberfaellig())
          .isEqualTo(new UeberfaelligkachelResponse(null, null, false));
      assertThat(antwort.kacheln().nachrichten())
          .as("Die uebrigen Bloecke kommen aus message_rollup und sind unberuehrt")
          .isEqualTo(42);
      assertThat(antwort.verteilung().zeilen()).isNotEmpty();
      assertThat(antwort.verlauf()).isNotEmpty();
    }

    /** Faellt nur die zweite Abfrage, faellt die Kachel als Ganzes — sie ist ein Paar. */
    @Test
    @DisplayName("Auch wenn nur eine der beiden Zahlen faellt, ist die Kachel nicht ermittelbar")
    void eine_zahl_genuegt_zum_fallen() {
      bestandMit(List.of(), List.of());
      when(repository.ueberfaelligInsgesamt(any(), any())).thenReturn(OptionalLong.empty());

      assertThat(antwort().kacheln().ueberfaellig().ermittelbar()).isFalse();
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

      DashboardResponse antwort =
          service().landingpage(MANDANT, Rollupzeitraum.MONATE_12, Verteilungssicht.PARTNER);

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
      assertThat(antwort.verteilung().zeilen())
          .as("Auch im Leerzustand sagt der Katalog etwas: alles nicht zugeordnet, naemlich null")
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
}
