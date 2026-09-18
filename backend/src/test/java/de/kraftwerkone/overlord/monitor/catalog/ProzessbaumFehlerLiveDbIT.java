package de.kraftwerkone.overlord.monitor.catalog;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveErgebnis;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveRepository;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZustand;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.dashboard.DashboardResponse;
import de.kraftwerkone.overlord.monitor.dashboard.DashboardService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * <b>Die Probe von Fehler live im Prozessbaum gegen die Quelle</b> (Teil B, {@code
 * docs/fehler-live.md} §5b).
 *
 * <p>Drei Fragen, je Mandant, je Paar und fuer ein freies Fenster, am Anker des Profils {@code
 * dev}:
 *
 * <ol>
 *   <li>Sind {@code gesamt.fehler} und die Fehler je Prozess genau {@code COUNT(*)} aus {@code
 *       Message} mit Fehlerbedingung, Fenster und Mandantenkette?
 *   <li><b>Baum gleich Uebersicht:</b> Wo beide Antworten fuer ein Paar dasselbe Fenster tragen,
 *       ist {@code gesamt.fehler} die Kachel <i>Fehler</i> und {@code gesamt.nachrichten} die
 *       Kachel <i>Nachrichten</i>. Das schliesst Punkt 209 — bis Teil B zaehlten beide Fehler
 *       verschieden.
 *   <li><b>Die Dev-Zeile als Eichung:</b> Auf der Testkopie ist der Rollup vollstaendig, und ein
 *       Abgang ist nicht herstellbar ({@code RUNNING} kommt null Mal vor, Tests schreiben nicht).
 *       Der Ersatz ist hier deshalb eine Identitaet — der Baum mit angewandter Lesung traegt Feld
 *       fuer Feld den Baum, bei dem sie ausgesetzt ist, also den von vor Teil B, bis auf den Block
 *       {@code fehlerLive}.
 * </ol>
 *
 * <p><b>Die Uhr steht am Anker</b> — ueber {@code @TestBean} an {@code devClock}, damit Baum und
 * Uebersicht denselben Stichtag haben; der Wasserstand ist der der Testkopie (Live-Rest {@code
 * NICHT_NOETIG}). <b>Keine Zahl aus dem Bestand in einer Zusicherung</b> (Regel T2): verglichen
 * werden Lesungen desselben Bestands; die Zahlen werden ausgegeben. <b>Geschrieben wird
 * nirgends</b> (S1).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class ProzessbaumFehlerLiveDbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie M146 und M188. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  /** Die Zone der Anwendungsuhr im Profil {@code dev}: die Systemzone, wie {@code ZeitConfig}. */
  private static final ZoneId ZONE = ZoneId.systemDefault();

  /** Die drei Mandanten mit Fehlern im Bestand (M188, Sitzung 0) — in verschiedenen Paaren. */
  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS", "VOTG");

  /**
   * Das freie Fenster: der Boesfall aus M152 — {@code 2024-12-30 14:00} bis {@code 2025-12-30
   * 03:00}, fuenf Segmente ueber alle drei Ebenen.
   */
  private static final Baumfenster FREI =
      Baumfenster.frei(
          LocalDateTime.parse("2024-12-30T14:00"), LocalDateTime.parse("2025-12-30T03:00"));

  @TestBean(name = "devClock", methodName = "ankeruhr")
  private Clock anwendungsuhr;

  static Clock ankeruhr() {
    return Clock.fixed(ANKER.atZone(ZONE).toInstant(), ZONE);
  }

  /** Der Baum, wie im Betrieb — mit der Lesung. */
  @Autowired private ProzessbaumService prozessbaumService;

  /** Die Uebersicht aus Teil A, an derselben Uhr — nur fuer den Vergleich. */
  @Autowired private DashboardService dashboardService;

  @Autowired private ProzessbaumRepository prozessbaumRepository;
  @Autowired private LiveRestService liveRestService;
  @Autowired private FehlerLiveRepository fehlerLiveRepository;
  @Autowired private MessageStatusClassifier statusClassifier;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  /** Der Baum, als waere die Lesung ausgefallen — also der Baum von vor Teil B. */
  private ProzessbaumService ausgesetzt() {
    return new ProzessbaumService(
        prozessbaumRepository,
        statusClassifier,
        anwendungsuhr,
        liveRestService,
        new FehlerLiveService(fehlerLiveRepository) {
          @Override
          public FehlerLiveErgebnis ermittle(
              MandantContext mandant, LocalDateTime von, LocalDateTime bis) {
            return FehlerLiveErgebnis.ausgesetzt();
          }
        });
  }

  /** Die drei Paare und das freie Fenster. */
  private static List<Baumfenster> fenster() {
    List<Baumfenster> alle = new ArrayList<>();
    for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
      alle.add(Baumfenster.paar(paar));
    }
    alle.add(FREI);
    return alle;
  }

  /** Fehlerbedingung, Fenster und Kette — von Hand und mit der einen Fehlerbedingung. */
  private Condition fehlerImFenster(String mandant, Zeitfenster fenster) {
    return statusClassifier
        .fehlerBedingung(MESSAGE.MESSAGESTATUS)
        .and(MESSAGE.MESSAGELASTUPDATE.ge(fenster.von()))
        .and(MESSAGE.MESSAGELASTUPDATE.lt(fenster.bis()))
        .and(
            DSL.exists(
                DSL.selectOne()
                    .from(PROCESS)
                    .join(PROJECTMANDANT)
                    .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
                    .where(PROCESS.PROCESSID.eq(MESSAGE.PROCESSID))
                    .and(PROJECTMANDANT.MANDANTID.eq(mandant))));
  }

  /** {@code COUNT(*)} aus {@code Message} je Prozess — ein Testzugriff, kein Anwendungscode. */
  private Map<String, Long> fehlerJeProzessAusDerQuelle(String mandant, Zeitfenster fenster) {
    Map<String, Long> jeProzess = new HashMap<>();
    glassfishDsl
        .select(MESSAGE.PROCESSID, DSL.count())
        .from(MESSAGE)
        .where(fehlerImFenster(mandant, fenster))
        .groupBy(MESSAGE.PROCESSID)
        .forEach(satz -> jeProzess.put(satz.value1(), (long) satz.value2()));
    return jeProzess;
  }

  /**
   * Die Fehler je Prozess, wie der Baum sie an seinen Blaettern traegt — nur, wo sie nicht null
   * sind.
   */
  private static Map<String, Long> fehlerJeBlatt(ProzessbaumResponse antwort) {
    Map<String, Long> jeProzess = new HashMap<>();
    sammle(antwort.knoten(), jeProzess);
    return jeProzess;
  }

  private static void sammle(List<BaumknotenResponse> knoten, Map<String, Long> jeProzess) {
    for (BaumknotenResponse einzeln : knoten) {
      switch (einzeln) {
        case GruppenknotenResponse gruppe -> sammle(gruppe.kinder(), jeProzess);
        case ProzessknotenResponse blatt -> {
          if (blatt.fehler() != 0) {
            jeProzess.put(blatt.processId(), blatt.fehler());
          }
        }
      }
    }
  }

  @Test
  @DisplayName(
      "Je Paar und frei: gesamt.fehler und die Fehler je Prozess sind COUNT(*) aus Message")
  void fehler_sind_die_quelle() {
    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      for (Baumfenster modus : fenster()) {
        Zeitfenster fenster = modus.fenster(ANKER);
        Map<String, Long> quelle = fehlerJeProzessAusDerQuelle(mandant, fenster);
        long summe = quelle.values().stream().mapToLong(Long::longValue).sum();

        ProzessbaumResponse antwort =
            prozessbaumService.baum(kontext, modus, Baumgliederung.PARTNER);

        assertThat(antwort.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
        assertThat(antwort.gesamt().fehler())
            .as("%s/%s: gesamt.fehler gleich COUNT(*) aus Message", mandant, modus.code())
            .isEqualTo(summe);
        assertThat(fehlerJeBlatt(antwort))
            .as("%s/%s: die Fehler je Prozess gleich der Quelle", mandant, modus.code())
            .isEqualTo(quelle);
        System.out.printf(
            "Fehler live Baum | %-7s | %-4s | Fenster %s bis %s | gesamt.fehler %d = COUNT(*) %d"
                + " | %d Prozesse mit Fehlern%n",
            mandant,
            modus.code(),
            fenster.von(),
            fenster.bis(),
            antwort.gesamt().fehler(),
            summe,
            quelle.size());
      }
    }
  }

  /**
   * <b>Baum gleich Uebersicht</b> — die Aussage, die Punkt 209 schliesst. Die Fenster werden zuerst
   * verglichen: Tragen die beiden Antworten fuer dasselbe Paar verschiedene Fenster, ist das ein
   * eigener Befund, und der Vergleich der Zahlen bewiese nichts.
   */
  @Test
  @DisplayName("Baum gleich Uebersicht: Kopfzahl Fehler und Nachrichten gleich den Kacheln")
  void baum_gleich_uebersicht() {
    assertThat(anwendungsuhr.instant())
        .as("Die Uhr steht am Anker")
        .isEqualTo(ANKER.atZone(ZONE).toInstant());
    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        ProzessbaumResponse baum =
            prozessbaumService.baum(kontext, Baumfenster.paar(paar), Baumgliederung.PARTNER);
        DashboardResponse uebersicht = dashboardService.landingpage(kontext, paar);

        assertThat(List.of(baum.fenster().von(), baum.fenster().bis()))
            .as("%s/%s: Baum und Uebersicht tragen dasselbe Fenster", mandant, paar.code())
            .isEqualTo(List.of(uebersicht.fenster().von(), uebersicht.fenster().bis()));
        assertThat(uebersicht.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
        assertThat(baum.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
        assertThat(baum.gesamt().fehler())
            .as("%s/%s: gesamt.fehler gleich der Kachel Fehler", mandant, paar.code())
            .isEqualTo(uebersicht.kacheln().fehler().anzahl());
        assertThat(baum.gesamt().nachrichten())
            .as("%s/%s: gesamt.nachrichten gleich der Kachel Nachrichten", mandant, paar.code())
            .isEqualTo(uebersicht.kacheln().nachrichten());
        System.out.printf(
            "Baum gleich Uebersicht | %-7s | %-3s | Nachrichten %d = %d | Fehler %d = %d%n",
            mandant,
            paar.code(),
            baum.gesamt().nachrichten(),
            uebersicht.kacheln().nachrichten(),
            baum.gesamt().fehler(),
            uebersicht.kacheln().fehler().anzahl());
      }
    }
  }

  /**
   * <b>Die Eichung der Dev-Zeile:</b> Keine Zahl des Baums aendert sich — Kopfzahlen, Knoten,
   * Blaetter, letzte Bewegung, Zustaende, in beiden Gliederungen. Einzig der Block {@code
   * fehlerLive} sagt etwas anderes. Die Vorprobe davor: Die Fehler des Rollups treffen {@code
   * Message} im Fenster — sonst taugt das Fenster nicht, und der Vergleich bewiese nichts ueber den
   * Ersatz.
   */
  @Test
  @DisplayName(
      "Die Dev-Zeile: angewandt und ausgesetzt zeigen denselben Baum — der Ersatz ist eine Identitaet")
  void die_dev_zeile_ist_eine_identitaet() {
    ProzessbaumService vorTeilB = ausgesetzt();
    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      for (Baumfenster modus : fenster()) {
        long quelle =
            fehlerJeProzessAusDerQuelle(mandant, modus.fenster(ANKER)).values().stream()
                .mapToLong(Long::longValue)
                .sum();
        for (Baumgliederung gliederung : Baumgliederung.values()) {
          String lage = mandant + "/" + modus.code() + "/" + gliederung;
          ProzessbaumResponse vorher = vorTeilB.baum(kontext, modus, gliederung);
          ProzessbaumResponse nachher = prozessbaumService.baum(kontext, modus, gliederung);

          assertThat(vorher.gesamt().fehler())
              .as(
                  "%s: Fenster ungeeignet — die Fehler des Rollups treffen Message nicht; den"
                      + " Wasserstand pruefen und berichten",
                  lage)
              .isEqualTo(quelle);
          assertThat(vorher.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.AUSGESETZT);
          assertThat(nachher.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
          assertThat(nachher)
              .as("%s: keine Zahl des Baums darf sich aendern", lage)
              .usingRecursiveComparison()
              .ignoringFields("fehlerLive")
              .isEqualTo(vorher);
        }
        ProzessbaumResponse antwort =
            prozessbaumService.baum(kontext, modus, Baumgliederung.PARTNER);
        System.out.printf(
            "Fehler live Baum | %-7s | %-4s | Dev-Zeile gleich | Nachrichten %d | Fehler %d%n",
            mandant, modus.code(), antwort.gesamt().nachrichten(), antwort.gesamt().fehler());
      }
    }
  }
}
