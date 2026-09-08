package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <b>Ein Anfrageparameter ist ein Begriff</b> — die Bindung von {@code begriff}, <b>ohne
 * Datenbank</b>.
 *
 * <p>Dieser Test prüft die eine Zeile, die {@code BamSucheDbIT} gegen echte Daten prüft, und er
 * prüft sie dort, wo sie in der CI läuft: Die Tests der Testkopie tragen {@code @Tag("db")} und
 * sind dort ausgeschlossen. Ein Defekt, der ein Jahr lang unbemerkt blieb, soll nicht in demselben
 * Netz hängen, das ihn nicht gefangen hat.
 *
 * <h2>Warum ein zweiter Controller mitläuft</h2>
 *
 * <p>Der Auftrag verlangt, dass die Änderung <b>nur an diesem Endpunkt</b> greift. Beide Controller
 * hängen hier an <i>einem</i> {@code MockMvc}-Aufbau und teilen sich damit denselben {@code
 * RequestMappingHandlerAdapter} und dieselbe Umwandlung. Trennt der Nachbar weiterhin am Komma und
 * {@code begriff} nicht mehr, dann liegt die Änderung am Controller und nicht an einer gemeinsamen
 * Verdrahtung. <b>Der Nachbar ist ein Stellvertreter</b>: Dass die echte Nachrichtenliste weiterhin
 * trennt, weist {@code BamSucheDbIT} am laufenden Endpunkt nach.
 *
 * <p>Alle Prüfwerte sind erfunden (Regel G1).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BamSucheKommabindungTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  private static final AngemeldeterNutzer NUTZER =
      new AngemeldeterNutzer(1L, "pruefer", Rolle.MANDANT, false);

  /** Die Antwort trägt keine Zeitpunkte — dieser Test misst die Bindung und nicht die Ausgabe. */
  private static final BamSucheResponse LEER =
      new BamSucheResponse(List.of(), List.of(), List.of(), null, null, false, Suchmodus.EXAKT);

  @Mock private BamSucheService bamSucheService;
  @Mock private MandantService mandantService;
  @Mock private SitzungsVerwaltung sitzungsVerwaltung;

  @Captor private ArgumentCaptor<BamSuchfilter> filter;

  private MockMvc mvc;

  @BeforeEach
  void aufsetzen() {
    when(sitzungsVerwaltung.aktuellerNutzer()).thenReturn(Optional.of(NUTZER));
    when(mandantService.aktuellerKontext(any())).thenReturn(MANDANT);
    when(bamSucheService.suche(any(), any())).thenReturn(LEER);

    Clock uhr = Clock.fixed(Instant.parse("2026-08-14T09:00:00Z"), ZoneId.of("Europe/Berlin"));
    mvc =
        MockMvcBuilders.standaloneSetup(
                new BamSucheController(bamSucheService, mandantService, sitzungsVerwaltung, uhr),
                new NachbarController())
            .build();
  }

  /** Die Begriffe, so wie {@link BamSuchfilter} sie gesehen hat. */
  private List<Suchbegriff> gebundeneBegriffe(String... begriffe) throws Exception {
    mvc.perform(get("/api/bam/suche").param("begriff", begriffe)).andExpect(status().isOk());
    verify(bamSucheService).suche(any(), filter.capture());
    return filter.getValue().begriffe();
  }

  // ─── Der behobene Defekt ──────────────────────────────────────────────────────

  /**
   * <b>Das Abnahmekriterium.</b> Ein Komma im Wert ist Teil des Werts. Vor der Behebung zerlegte
   * Spring diesen einen Parameter in zwei Begriffe, von denen der zweite keinen Pflichttrenner mehr
   * trug — die Antwort war {@code 400 suchbegriff-ohne-typtrenner}.
   */
  @Test
  @DisplayName("Ein Parameter mit Komma ist ein Begriff, nicht zwei")
  void ein_komma_trennt_nicht() throws Exception {
    assertThat(gebundeneBegriffe(":4711,815")).containsExactly(new Suchbegriff(null, "4711,815"));
  }

  /** Das Komma trennt auch dann nicht, wenn der Begriff einen Typ trägt. */
  @Test
  @DisplayName("Auch mit Typ bleibt der Wert mit Komma ein Wert")
  void ein_komma_trennt_auch_mit_typ_nicht() throws Exception {
    assertThat(gebundeneBegriffe("9003:4711,815"))
        .containsExactly(new Suchbegriff((short) 9003, "4711,815"));
  }

  /** Mehrere Kommas ebenso wenig — die Zerlegung ist weg und nicht auf eines begrenzt. */
  @Test
  @DisplayName("Zwei Kommas in einem Wert ergeben weiterhin einen Begriff")
  void zwei_kommas_ergeben_einen_begriff() throws Exception {
    assertThat(gebundeneBegriffe(":4,711,815")).containsExactly(new Suchbegriff(null, "4,711,815"));
  }

  // ─── Was unverändert bleibt ───────────────────────────────────────────────────

  /**
   * <b>Mehrere Begriffe kommen weiterhin als mehrfach gesetzter Parameter.</b> Das ist die Form,
   * unter der die Verundung seit Teil 2b angeboten wird, und sie ist von der Behebung nicht
   * berührt.
   */
  @Test
  @DisplayName("Zwei getrennte begriff-Parameter bleiben zwei Begriffe")
  void zwei_parameter_bleiben_zwei_begriffe() throws Exception {
    assertThat(gebundeneBegriffe(":4711", ":815"))
        .containsExactly(new Suchbegriff(null, "4711"), new Suchbegriff(null, "815"));
  }

  /**
   * <b>Der gemischte Fall — und er ist der eigentliche Nachweis.</b> Ein Begriff mit Komma und
   * einer ohne, gemeinsam gesetzt, ergeben zwei Begriffe und nicht drei. Eine Behebung, die
   * schlicht die ganze Zerlegung überspringt, könnte hier noch stimmen; eine, die stattdessen alle
   * Werte wieder zusammenzieht, fiele auf.
   */
  @Test
  @DisplayName("Ein Begriff mit Komma und einer ohne ergeben zwei Begriffe")
  void gemischt_ergibt_zwei_begriffe() throws Exception {
    assertThat(gebundeneBegriffe(":4711,815", ":0815"))
        .containsExactly(new Suchbegriff(null, "4711,815"), new Suchbegriff(null, "0815"));
  }

  /** Die Reihenfolge bleibt die der Anfrage, auch wenn der kommahaltige Wert hinten steht. */
  @Test
  @DisplayName("Die Reihenfolge der Parameter bleibt erhalten")
  void die_reihenfolge_bleibt() throws Exception {
    assertThat(gebundeneBegriffe(":0815", ":4711,815"))
        .containsExactly(new Suchbegriff(null, "0815"), new Suchbegriff(null, "4711,815"));
  }

  /** Ein leerer Parameter fällt weiterhin weg — die Bindung ändert daran nichts. */
  @Test
  @DisplayName("Ein leerer begriff-Parameter faellt weiterhin weg")
  void ein_leerer_parameter_faellt_weg() throws Exception {
    assertThat(gebundeneBegriffe(":4711,815", ":"))
        .containsExactly(new Suchbegriff(null, "4711,815"));
  }

  // ─── Derselbe Nachweis im Praefixmodus ────────────────────────────────────────

  /**
   * <b>Der Defekt lag in beiden Modi</b>, weil er vor dem Modus liegt: Er sitzt in der Annahme des
   * Parameters und nicht im Vergleich. Deshalb derselbe Nachweis für {@code modus=praefix}.
   */
  @Test
  @DisplayName("Im Praefixmodus trennt das Komma ebenfalls nicht")
  void im_praefixmodus_trennt_das_komma_nicht() throws Exception {
    mvc.perform(get("/api/bam/suche").param("begriff", "9003:4711,81").param("modus", "praefix"))
        .andExpect(status().isOk());

    verify(bamSucheService).suche(any(), filter.capture());
    assertThat(filter.getValue().begriffe())
        .containsExactly(new Suchbegriff((short) 9003, "4711,81"));
    assertThat(filter.getValue().modus()).isEqualTo(Suchmodus.PRAEFIX);
  }

  /**
   * <b>Maskierung und Kommabehandlung greifen unabhängig voneinander.</b> Der Unterstrich wird zum
   * maskierten Zeichen, das Komma bleibt eines — es ist in {@code LIKE} kein Platzhalter und wird
   * deshalb nicht maskiert.
   */
  @Test
  @DisplayName("Ein Wert mit Komma und Unterstrich: das eine maskiert, das andere nicht")
  void komma_und_unterstrich_stoeren_einander_nicht() throws Exception {
    mvc.perform(get("/api/bam/suche").param("begriff", ":47_11,815").param("modus", "praefix"))
        .andExpect(status().isOk());

    verify(bamSucheService).suche(any(), filter.capture());
    assertThat(filter.getValue().begriffe()).containsExactly(new Suchbegriff(null, "47_11,815"));
    assertThat(Suchbedingung.alsMuster("47_11,815"))
        .as("der Unterstrich wird maskiert, das Komma bleibt ein gewoehnliches Zeichen")
        .isEqualTo("47\\_11,815%");
  }

  // ─── Die Abgrenzung ───────────────────────────────────────────────────────────

  /**
   * <b>Die Änderung greift nur an diesem Controller.</b> Der Nachbar bindet eine Liste aus einem
   * Anfrageparameter genauso, wie {@code /api/bam/suche} es bis heute tat — und er trennt weiterhin
   * am Komma. Beide hängen an demselben {@code MockMvc}-Aufbau: Läge die Behebung an einer
   * gemeinsamen Umwandlung, träfe sie ihn mit.
   */
  @Test
  @DisplayName("Ein Nachbar-Endpunkt im selben Aufbau trennt weiterhin am Komma")
  void der_nachbar_trennt_weiterhin() throws Exception {
    mvc.perform(get("/test/nachbarliste").param("wert", "a,b"))
        .andExpect(status().isOk())
        .andExpect(content().string("2"));
  }

  /**
   * Ein Endpunkt, der eine Liste so bindet, wie es im Projekt sonst überall geschieht — {@code
   * status} und {@code prozess} der Nachrichtenliste sehen genauso aus.
   */
  @RestController
  static class NachbarController {

    @GetMapping("/test/nachbarliste")
    int nachbarliste(@RequestParam(required = false) List<String> wert) {
      return wert == null ? 0 : wert.size();
    }
  }
}
