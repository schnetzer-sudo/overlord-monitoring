package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Was der Service aus den Rohzeilen macht: aufsteigen, absteigen, begrenzen. <b>Ohne Datenbank</b>
 * — die Statements pruefen {@code KettenStatementsTest}, {@code KettenDbIT} und die Messungen.
 *
 * <p><b>Der Zyklus wird hier und nur hier geprueft.</b> In der Testkopie kommt keiner vor (M30‑3:
 * Stufe fuenf ist leer, null Selbstverweise ueber 3,34 Millionen Zeilen) — ein Datenbanktest
 * koennte den Schutz also gar nicht ausloesen. Mit erfundenen Daten geht es, und deshalb steht er
 * hier.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KettenServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
  private static final LocalDateTime T0 = LocalDateTime.parse("2025-12-29T10:00:00");

  @Mock private KettenRepository kettenRepository;

  private final MessageStatusClassifier statusClassifier = new MessageStatusClassifier();

  /** Die Glieder, die {@code findeGlied} kennt — eine Landkarte statt einer Kette von Stubs. */
  private final Map<String, Kettengliedzeile> bestand = new HashMap<>();

  /**
   * Die Grundhaltung der Attrappe: Glieder kommen aus {@link #bestand}, beide Abwaertsrichtungen
   * sind leer.
   *
   * <p><b>Vor jedem Test und nicht in {@code service()}.</b> Mockito laesst eine spaetere Stubbung
   * eine fruehere ueberschreiben — stuenden diese Zeilen in der Fabrikmethode, loeschte jeder
   * Aufruf die zuvor gesetzten Sonderfaelle wieder aus.
   */
  @BeforeEach
  void grundstubs() {
    when(kettenRepository.findeGlied(any(), anyString()))
        .thenAnswer(aufruf -> bestand.get(aufruf.<String>getArgument(1)));
    when(kettenRepository.findeKinder(any(), anyString(), any(), anyInt())).thenReturn(List.of());
    when(kettenRepository.findeMergeEingaenge(any(), anyString(), any(), anyInt()))
        .thenReturn(List.of());
  }

  private KettenService service() {
    return new KettenService(
        kettenRepository, statusClassifier, Clock.fixed(T0.atZone(ZONE).toInstant(), ZONE));
  }

  /** Ein Glied ohne jede Verkettung. */
  private Kettengliedzeile glied(String id) {
    return glied(id, "FINISHED", false, null, null, false, 0);
  }

  private Kettengliedzeile glied(
      String id,
      String status,
      boolean source,
      String sourceMessageId,
      String targetMessageId,
      boolean target,
      int minuten) {
    Kettengliedzeile zeile =
        new Kettengliedzeile(
            id,
            status,
            T0.plusMinutes(minuten),
            "Ablauf " + id,
            source,
            target,
            sourceMessageId,
            targetMessageId);
    bestand.put(id, zeile);
    return zeile;
  }

  /** Eine Kette aus {@code laenge} Gliedern, jedes ueber {@code SourceMessageID} am naechsten. */
  private void kette(int laenge) {
    for (int i = 0; i < laenge; i++) {
      boolean hatEltern = i < laenge - 1;
      glied("g" + i, "FINISHED", i > 0, hatEltern ? "g" + (i + 1) : null, null, false, i);
    }
  }

  // ─── Aufstieg ──────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Der Aufstieg")
  class Aufstieg {

    @Test
    @DisplayName("loest bis zur Wurzel auf und zaehlt die Ebenen negativ")
    void loest_bis_zur_wurzel_auf() {
      kette(4);

      KetteResponse kette = service().kette(MANDANT, "g0");

      assertThat(kette.aufwaerts())
          .extracting(KettengliedResponse::messageId)
          .containsExactly("g1", "g2", "g3");
      assertThat(kette.aufwaerts())
          .extracting(KettengliedResponse::ebene)
          .containsExactly(-1, -2, -3);
      assertThat(kette.tiefeErreicht()).isFalse();
      assertThat(kette.zyklusErkannt()).isFalse();
    }

    /**
     * Vier Glieder sind die tiefste Kette der Testkopie (M30‑3) — der Fall, den die Anwendung
     * tatsaechlich sieht.
     */
    @Test
    @DisplayName("bleibt leer, wenn die Nachricht selbst am oberen Ende steht")
    void bleibt_leer_am_oberen_ende() {
      glied("allein");

      KetteResponse kette = service().kette(MANDANT, "allein");

      assertThat(kette.aufwaerts()).isEmpty();
      assertThat(kette.rollen()).isEmpty();
      assertThat(kette.tiefeErreicht()).isFalse();
    }

    @Test
    @DisplayName("folgt bei einem Merge-Eingang der TargetMessageID")
    void folgt_der_target_message_id() {
      glied("ergebnis", "EERP_RECEIVED", false, null, null, true, 5);
      glied("eingang", "MERGED", false, null, "ergebnis", false, 0);

      KetteResponse kette = service().kette(MANDANT, "eingang");

      assertThat(kette.aufwaerts()).hasSize(1);
      assertThat(kette.aufwaerts().getFirst().messageId()).isEqualTo("ergebnis");
      assertThat(kette.aufwaerts().getFirst().beziehung())
          .isEqualTo(Kettenbeziehung.ZUSAMMENFUEHRUNG);
      assertThat(kette.aufwaerts().getFirst().rollen()).containsExactly(Kettenrolle.MERGE_ERGEBNIS);
    }

    @Test
    @DisplayName("bricht an der Tiefengrenze ab und sagt das")
    void bricht_an_der_tiefengrenze_ab() {
      kette(KettenService.TIEFE_GRENZE + 5);

      KetteResponse kette = service().kette(MANDANT, "g0");

      assertThat(kette.aufwaerts()).hasSize(KettenService.TIEFE_GRENZE);
      assertThat(kette.tiefeErreicht())
          .as("die Kette wird nicht als vollstaendig ausgegeben")
          .isTrue();
      assertThat(kette.zyklusErkannt()).isFalse();
    }

    @Test
    @DisplayName("erkennt einen Zyklus und nennt ihn nicht „tief\"")
    void erkennt_einen_zyklus() {
      glied("a", "FINISHED", true, "b", null, false, 0);
      glied("b", "FINISHED", true, "a", null, false, 1);

      KetteResponse kette = service().kette(MANDANT, "a");

      assertThat(kette.zyklusErkannt())
          .as("die Tiefengrenze allein braeche zwar ab, saegte aber „tief\" statt „im Kreis\"")
          .isTrue();
      assertThat(kette.tiefeErreicht()).isFalse();
      assertThat(kette.aufwaerts()).extracting(KettengliedResponse::messageId).containsExactly("b");
    }

    @Test
    @DisplayName("erkennt auch einen Selbstverweis als Zyklus")
    void erkennt_einen_selbstverweis() {
      glied("ich", "FINISHED", true, "ich", null, false, 0);

      KetteResponse kette = service().kette(MANDANT, "ich");

      assertThat(kette.zyklusErkannt()).isTrue();
      assertThat(kette.aufwaerts()).isEmpty();
    }

    /**
     * Der Zyklus schlaegt die Tiefe: Traefen beide zu, waere „tief" die schwaechere Auskunft — sie
     * beschreibt, wo abgebrochen wurde, nicht warum.
     */
    @Test
    @DisplayName("meldet bei einem Zyklus jenseits der Tiefengrenze den Zyklus")
    void zyklus_schlaegt_tiefe() {
      int laenge = KettenService.TIEFE_GRENZE + 1;
      kette(laenge);
      // Das oberste Glied zeigt zurueck auf das unterste — der Kreis schliesst sich genau dort,
      // wo auch die Tiefengrenze greifen wuerde.
      glied("g" + (laenge - 1), "FINISHED", true, "g0", null, false, laenge - 1);

      KetteResponse kette = service().kette(MANDANT, "g0");

      assertThat(kette.zyklusErkannt()).isTrue();
      assertThat(kette.tiefeErreicht()).isFalse();
    }

    /**
     * Ein Glied darueber, das nicht aufloest, beendet den Aufstieg still — ein Verweis ins Leere
     * oder, und das ist der wichtigere Fall, ein Glied eines fremden Mandanten. Den darf es fuer
     * diesen Aufrufer nicht geben, auch nicht als Hinweis „hier geht es weiter".
     */
    @Test
    @DisplayName("endet still, wenn das Glied darueber nicht sichtbar ist")
    void endet_still_bei_unsichtbarem_glied_darueber() {
      glied("kind", "FINISHED", false, "fremder-elternteil", null, false, 0);

      KetteResponse kette = service().kette(MANDANT, "kind");

      assertThat(kette.aufwaerts()).isEmpty();
      assertThat(kette.tiefeErreicht()).isFalse();
      assertThat(kette.zyklusErkannt()).isFalse();
    }
  }

  // ─── Abstieg ───────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Der Abstieg")
  class Abstieg {

    private void kinder(int anzahl) {
      List<Kettengliedzeile> kinder = new ArrayList<>();
      for (int i = 0; i < anzahl; i++) {
        kinder.add(
            new Kettengliedzeile(
                "k" + i, "FINISHED", T0.plusSeconds(i), "Ablauf", false, false, "wurzel", null));
      }
      when(kettenRepository.findeKinder(any(), eq("wurzel"), any(), anyInt()))
          .thenAnswer(
              aufruf -> {
                int limit = aufruf.getArgument(3);
                return kinder.subList(0, Math.min(kinder.size(), limit + 1));
              });
    }

    @Test
    @DisplayName("liefert hoechstens die Breitengrenze und meldet den Rest")
    void liefert_hoechstens_die_breitengrenze() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      kinder(KettenService.BREITE_GRENZE + 20);
      when(kettenRepository.zaehleKinder(any(), eq("wurzel")))
          .thenReturn(KettenService.BREITE_GRENZE + 20);

      KetteResponse kette = service().kette(MANDANT, "wurzel");

      assertThat(kette.abwaerts()).hasSize(KettenService.BREITE_GRENZE);
      assertThat(kette.abwaertsGesamt()).isEqualTo(KettenService.BREITE_GRENZE + 20);
      assertThat(kette.weitereVorhanden()).isTrue();
      assertThat(kette.abwaerts()).allSatisfy(glied -> assertThat(glied.ebene()).isEqualTo(1));
    }

    @Test
    @DisplayName("meldet keine weiteren, wenn alle geliefert wurden")
    void meldet_keine_weiteren_wenn_alle_da_sind() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      kinder(3);
      when(kettenRepository.zaehleKinder(any(), eq("wurzel"))).thenReturn(3);

      KetteResponse kette = service().kette(MANDANT, "wurzel");

      assertThat(kette.abwaerts()).hasSize(3);
      assertThat(kette.abwaertsGesamt()).isEqualTo(3);
      assertThat(kette.weitereVorhanden()).isFalse();
    }

    /**
     * Beide Abwaertsrichtungen kommen in <b>einer</b> Liste — 25 Zeilen sind zugleich Wurzel und
     * Merge-Ergebnis (M30‑4), und fuer die waere eine Aufteilung in zwei Listen eine Entscheidung,
     * die die Oberflaeche treffen muesste.
     */
    @Test
    @DisplayName("mischt Kinder und Merge-Eingaenge nach dem Sortierschluessel")
    void mischt_kinder_und_eingaenge() {
      glied("beides", "SPLITTED", true, null, null, true, 0);
      when(kettenRepository.findeKinder(any(), eq("beides"), any(), anyInt()))
          .thenReturn(
              List.of(
                  new Kettengliedzeile(
                      "kind-spaet",
                      "FINISHED",
                      T0.plusMinutes(10),
                      "A",
                      false,
                      false,
                      "beides",
                      null)));
      when(kettenRepository.findeMergeEingaenge(any(), eq("beides"), any(), anyInt()))
          .thenReturn(
              List.of(
                  new Kettengliedzeile(
                      "eingang-frueh",
                      "MERGED",
                      T0.plusMinutes(1),
                      "B",
                      false,
                      false,
                      null,
                      "beides")));
      when(kettenRepository.zaehleKinder(any(), eq("beides"))).thenReturn(1);
      when(kettenRepository.zaehleMergeEingaenge(any(), eq("beides"))).thenReturn(1);

      KetteResponse kette = service().kette(MANDANT, "beides");

      assertThat(kette.abwaerts())
          .extracting(KettengliedResponse::messageId)
          .as("sortiert nach (MessageLastUpdate, MessageID), nicht nach Richtung")
          .containsExactly("eingang-frueh", "kind-spaet");
      assertThat(kette.abwaerts())
          .extracting(KettengliedResponse::beziehung)
          .containsExactly(Kettenbeziehung.ZUSAMMENFUEHRUNG, Kettenbeziehung.AUFTEILUNG);
      assertThat(kette.abwaertsGesamt()).isEqualTo(2);
      assertThat(kette.rollen())
          .containsExactly(Kettenrolle.SPLIT_WURZEL, Kettenrolle.MERGE_ERGEBNIS);
    }

    /**
     * <b>Beide Richtungen werden immer gefragt</b>, auch wenn die Flags sagen, es gaebe nichts. E4
     * hat gemessen, dass sie sich exakt mit der Verkettung decken — das ist die richtige Grundlage
     * fuer die Liste, die ohne Abfrage entscheiden muss. Hier waere es das Vertrauen in eine
     * Beobachtung, erkauft fuer eine halbe Millisekunde.
     */
    @Test
    @DisplayName("fragt beide Richtungen, auch wenn Source und Target null sind")
    void fragt_beide_richtungen_unabhaengig_von_den_flags() {
      glied("ohne-kette");

      service().kette(MANDANT, "ohne-kette");

      verify(kettenRepository).findeKinder(any(), eq("ohne-kette"), any(), anyInt());
      verify(kettenRepository).findeMergeEingaenge(any(), eq("ohne-kette"), any(), anyInt());
      verify(kettenRepository).zaehleKinder(any(), eq("ohne-kette"));
      verify(kettenRepository).zaehleMergeEingaenge(any(), eq("ohne-kette"));
    }
  }

  // ─── Der Blaetter-Endpunkt ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("Der Blaetter-Endpunkt")
  class Abwaertsseite {

    @Test
    @DisplayName("blaettert cursor-basiert und liefert einen Cursor, solange es weitergeht")
    void blaettert_cursor_basiert() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      List<Kettengliedzeile> kinder = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        kinder.add(
            new Kettengliedzeile(
                "k" + i, "FINISHED", T0.plusSeconds(i), "A", false, false, "wurzel", null));
      }
      when(kettenRepository.findeKinder(any(), eq("wurzel"), any(), anyInt())).thenReturn(kinder);

      Seite<KettengliedResponse> seite = service().abwaerts(MANDANT, "wurzel", null, 3);

      assertThat(seite.items())
          .extracting(KettengliedResponse::messageId)
          .containsExactly("k0", "k1", "k2");
      assertThat(seite.hasMore()).isTrue();
      assertThat(seite.nextCursor()).isNotBlank();
      assertThat(Seitenposition.dekodiere(seite.nextCursor()).id()).isEqualTo("k2");
    }

    @Test
    @DisplayName("reicht den Cursor an beide Richtungen weiter")
    void reicht_den_cursor_an_beide_richtungen_weiter() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      String cursor = new Seitenposition(T0, "k0").kodiere();

      service().abwaerts(MANDANT, "wurzel", cursor, 10);

      verify(kettenRepository)
          .findeKinder(any(), eq("wurzel"), any(Seitenposition.class), anyInt());
      verify(kettenRepository)
          .findeMergeEingaenge(any(), eq("wurzel"), any(Seitenposition.class), anyInt());
    }

    /**
     * Eine Zeile <b>ohne</b> {@code MessageLastUpdate} sortiert nach vorn und wird ausgeliefert —
     * der Cursor entsteht aus der <i>letzten</i> Zeile der Seite, und die traegt einen Zeitpunkt.
     * Gemessen kommt der Fall nicht vor ({@code 0} von 3.341.519, M30‑6); die Spalte laesst ihn zu.
     */
    @Test
    @DisplayName("liefert eine Zeile ohne Zeitpunkt aus, ohne am Cursor zu scheitern")
    void zeile_ohne_zeitpunkt_bricht_das_blaettern_nicht() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      when(kettenRepository.findeKinder(any(), eq("wurzel"), any(), anyInt()))
          .thenReturn(
              List.of(
                  new Kettengliedzeile(
                      "ohne-zeitpunkt", "FINISHED", null, "A", false, false, "wurzel", null),
                  new Kettengliedzeile(
                      "mit-zeitpunkt", "FINISHED", T0, "A", false, false, "wurzel", null),
                  new Kettengliedzeile(
                      "spaeter",
                      "FINISHED",
                      T0.plusMinutes(1),
                      "A",
                      false,
                      false,
                      "wurzel",
                      null)));

      Seite<KettengliedResponse> seite = service().abwaerts(MANDANT, "wurzel", null, 2);

      assertThat(seite.items())
          .extracting(KettengliedResponse::messageId)
          .as("null sortiert zuerst — wie in MariaDB")
          .containsExactly("ohne-zeitpunkt", "mit-zeitpunkt");
      assertThat(seite.items().getFirst().zeitpunkt()).isNull();
      assertThat(seite.hasMore()).isTrue();
      assertThat(Seitenposition.dekodiere(seite.nextCursor()).id()).isEqualTo("mit-zeitpunkt");
    }

    /**
     * Der verbleibende Fall: eine <b>ganze Seite</b> ohne Zeitpunkt. Dann gibt es keine Position,
     * an der weitergeblaettert werden koennte — das ist ein technischer Fehler ({@code 500} mit
     * {@code traceId}) und keine beschoenigte Seite. Die Meldung nennt die gebrochene Zusicherung,
     * damit im Protokoll steht, <i>welche</i> es war.
     */
    @Test
    @DisplayName("nennt die gebrochene Zusicherung, wenn eine ganze Seite keinen Zeitpunkt hat")
    void ganze_seite_ohne_zeitpunkt_nennt_die_zusicherung() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      when(kettenRepository.findeKinder(any(), eq("wurzel"), any(), anyInt()))
          .thenReturn(
              List.of(
                  new Kettengliedzeile(
                      "ohne-a", "FINISHED", null, "A", false, false, "wurzel", null),
                  new Kettengliedzeile(
                      "ohne-b", "FINISHED", null, "A", false, false, "wurzel", null)));

      KettenService service = service();

      assertThatThrownBy(() -> service.abwaerts(MANDANT, "wurzel", null, 1))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("MessageLastUpdate")
          .hasMessageContaining("M30-6");
    }

    @Test
    @DisplayName("weist einen unlesbaren Cursor ab, statt still von vorne zu liefern")
    void weist_einen_unlesbaren_cursor_ab() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);

      assertThatThrownBy(() -> service().abwaerts(MANDANT, "wurzel", "kein-cursor", null))
          .isInstanceOf(FachlicheAusnahme.class);
    }

    @Test
    @DisplayName("weist eine unzulaessige Seitengroesse ab")
    void weist_eine_unzulaessige_seitengroesse_ab() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);

      assertThatThrownBy(() -> service().abwaerts(MANDANT, "wurzel", null, 0))
          .isInstanceOf(FachlicheAusnahme.class);
      assertThatThrownBy(() -> service().abwaerts(MANDANT, "wurzel", null, 100_000))
          .isInstanceOf(FachlicheAusnahme.class);
    }

    /**
     * Die Existenz wird <b>zuerst</b> geprueft. Eine leere Liste waere eine andere Auskunft als
     * {@code 404} — und damit eine Auskunft ueber den Bestand.
     */
    @Test
    @DisplayName("ist 404 fuer eine unsichtbare Nachricht, nicht eine leere Liste")
    void ist_404_fuer_eine_unsichtbare_nachricht() {
      KettenService service = service();

      assertThatThrownBy(() -> service.abwaerts(MANDANT, "gibt-es-nicht", null, null))
          .isInstanceOf(RessourceNichtGefundenException.class);
      verify(kettenRepository, never()).findeKinder(any(), anyString(), any(), anyInt());
    }
  }

  // ─── Der Cursor der Ketten-Antwort ─────────────────────────────────────────────

  /**
   * <b>Das Feld, das den Umweg beseitigt.</b> Bis zum 11.08.2026 lieferte {@code /kette} keine
   * Position, ab der sich weiterblaettern liesse — die Oberflaeche holte deshalb beim ersten
   * Nachladen die erste Seite ein zweites Mal, nur um an einen Cursor zu kommen.
   *
   * <p>Der wichtigste dieser Tests ist der dritte: Er haelt fest, dass es <b>eine</b> Kodierung ist
   * und nicht zwei. Ein zweiter Kodierer faellt sonst erst auf, wenn eine Seite Zeilen ueberspringt
   * oder wiederholt — und dann in Produktion.
   */
  @Nested
  @DisplayName("abwaertsCursor")
  class AbwaertsCursor {

    /**
     * {@code anzahl} Kinder in Sortierreihenfolge; die ersten {@code ohneZeitpunkt} tragen keinen
     * {@code MessageLastUpdate}. Sie stehen vorn, weil {@code null} zuerst sortiert — in MariaDB
     * wie im Vergleicher des Service.
     */
    private void kinder(int anzahl, int ohneZeitpunkt) {
      List<Kettengliedzeile> kinder = new ArrayList<>();
      for (int i = 0; i < anzahl; i++) {
        kinder.add(
            new Kettengliedzeile(
                String.format("k%03d", i),
                "FINISHED",
                i < ohneZeitpunkt ? null : T0.plusSeconds(i),
                "Ablauf",
                false,
                false,
                "wurzel",
                null));
      }
      when(kettenRepository.findeKinder(any(), eq("wurzel"), any(), anyInt()))
          .thenAnswer(
              aufruf -> {
                int limit = aufruf.getArgument(3);
                return kinder.subList(0, Math.min(kinder.size(), limit + 1));
              });
      when(kettenRepository.zaehleKinder(any(), eq("wurzel"))).thenReturn(anzahl);
    }

    /** Ein Cursor ohne naechste Seite behauptete, es gaebe eine. */
    @Test
    @DisplayName("fehlt, wenn es nichts mehr zu blaettern gibt")
    void fehlt_ohne_weitere() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      kinder(3, 0);

      KetteResponse kette = service().kette(MANDANT, "wurzel");

      assertThat(kette.weitereVorhanden()).isFalse();
      assertThat(kette.abwaertsCursor())
          .as("ein Cursor ohne naechste Seite behauptet, es gaebe eine")
          .isNull();
    }

    /**
     * <b>Der Sonderfall aus M30‑6.</b> Der Sortierschluessel ist {@code (MessageLastUpdate,
     * MessageID)}; eine Zeile ohne Zeitpunkt hat darin keine Position. Weil {@code null} zuerst
     * sortiert, tritt der Fall erst ein, wenn eine <i>ganze Seite</i> ohne Zeitpunkt ausgeliefert
     * wird — gemessen {@code 0} von 3.341.519 Mal.
     *
     * <p>Die Kette ist trotzdem vollstaendig beantwortet: {@code weitereVorhanden} bleibt wahr, nur
     * der Cursor fehlt. Anders als beim Blaetter-Endpunkt ist das <b>kein</b> {@code 500} — der
     * Aufrufer bekommt seine Antwort und keine Schaltflaeche.
     */
    @Test
    @DisplayName("fehlt, wenn die letzte ausgelieferte Zeile keinen Zeitpunkt traegt")
    void fehlt_ohne_zeitpunkt_auf_der_letzten_zeile() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      kinder(KettenService.BREITE_GRENZE + 10, KettenService.BREITE_GRENZE);

      KetteResponse kette = service().kette(MANDANT, "wurzel");

      assertThat(kette.abwaerts())
          .as("die ganze Seite steht am Anfang der Ordnung — null sortiert zuerst")
          .hasSize(KettenService.BREITE_GRENZE)
          .allSatisfy(glied -> assertThat(glied.zeitpunkt()).isNull());
      assertThat(kette.weitereVorhanden())
          .as("die Zaehlung weiss, dass es weitergeht — nur adressieren laesst es sich nicht")
          .isTrue();
      assertThat(kette.abwaertsCursor()).isNull();
    }

    /**
     * <b>Eine Kodierung, nicht zwei.</b> Der Cursor der Ketten-Antwort ist derselbe, den der
     * Blaetter-Endpunkt nach seiner ersten Seite liefert — Zeichen fuer Zeichen. Waeren es zwei
     * Kodierer, liefen sie irgendwann auseinander, und das Blaettern uebersprenge oder wiederholte
     * eine Zeile.
     */
    @Test
    @DisplayName("ist derselbe, den der Blaetter-Endpunkt nach seiner ersten Seite liefert")
    void ist_derselbe_wie_der_des_blaetter_endpunkts() {
      glied("wurzel", "SPLITTED", true, null, null, false, 0);
      kinder(KettenService.BREITE_GRENZE + 10, 0);

      KetteResponse kette = service().kette(MANDANT, "wurzel");
      Seite<KettengliedResponse> ersteSeite =
          service().abwaerts(MANDANT, "wurzel", null, KettenService.BREITE_GRENZE);

      assertThat(kette.abwaertsCursor()).isNotBlank().isEqualTo(ersteSeite.nextCursor());
      assertThat(Seitenposition.dekodiere(kette.abwaertsCursor()).id())
          .as("er zeigt auf die letzte ausgelieferte Abwaertszeile")
          .isEqualTo(kette.abwaerts().getLast().messageId());
      assertThat(kette.abwaerts())
          .extracting(KettengliedResponse::messageId)
          .as("und die Ketten-Antwort liefert genau die Seite, hinter der er ansetzt")
          .containsExactlyElementsOf(
              ersteSeite.items().stream().map(KettengliedResponse::messageId).toList());
    }
  }

  // ─── Die Antwort selbst ────────────────────────────────────────────────────────

  @Test
  @DisplayName("Eine unsichtbare Nachricht ist 404 — und nennt keinen Grund")
  void unsichtbare_nachricht_ist_404() {
    KettenService service = service();

    assertThatThrownBy(() -> service.kette(MANDANT, "gibt-es-nicht"))
        .isInstanceOf(RessourceNichtGefundenException.class);
  }

  @Test
  @DisplayName("rollen ist immer vorhanden — leer statt fehlend")
  void rollen_sind_immer_vorhanden() {
    glied("ohne-kette");

    KetteResponse kette = service().kette(MANDANT, "ohne-kette");

    assertThat(kette.rollen()).isNotNull().isEmpty();
    assertThat(kette.aufwaerts()).isNotNull().isEmpty();
    assertThat(kette.abwaerts()).isNotNull().isEmpty();
    assertThat(kette.abwaertsGesamt()).isZero();
  }

  /** Der Zeitpunkt geht als UTC hinaus, umgerechnet ueber die Zone der Anwendungsuhr. */
  @Test
  @DisplayName("Der Zeitpunkt eines Glieds ist UTC")
  void zeitpunkt_ist_utc() {
    glied("ergebnis", "EERP_RECEIVED", false, null, null, true, 0);
    glied("eingang", "MERGED", false, null, "ergebnis", false, 0);

    KetteResponse kette = service().kette(MANDANT, "eingang");

    assertThat(kette.aufwaerts().getFirst().zeitpunkt()).isEqualTo(T0.atZone(ZONE).toInstant());
  }

  /**
   * Die Einordnung kommt aus dem {@code MessageStatusClassifier} und wird hier nicht nachgebaut.
   */
  @Test
  @DisplayName("statusKind kommt aus dem Classifier")
  void status_kind_kommt_aus_dem_classifier() {
    glied("ergebnis", "EERP_RECEIVED", false, null, null, true, 0);
    glied("eingang", "MERGED", false, null, "ergebnis", false, 0);

    KetteResponse kette = service().kette(MANDANT, "eingang");

    assertThat(kette.aufwaerts().getFirst().statusKind()).isEqualTo("QUITTIERT");
    assertThat(kette.aufwaerts().getFirst().status()).isEqualTo("EERP_RECEIVED");
  }
}
