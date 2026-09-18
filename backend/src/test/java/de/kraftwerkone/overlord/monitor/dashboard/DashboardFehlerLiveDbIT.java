package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.FehlerLiveErgebnis;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveRepository;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveService;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZustand;
import de.kraftwerkone.overlord.monitor.common.LiveRestRepository;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Wasserstand;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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

/**
 * <b>Die Probe von Fehler live gegen die Quelle</b> (E-208, {@code docs/fehler-live.md} §7, §9).
 *
 * <p>Drei Fragen, je Mandant und Paar, am Anker des Profils {@code dev}:
 *
 * <ol>
 *   <li>Ist die Kachel <i>Fehler</i> genau {@code COUNT(*)} aus {@code Message} mit
 *       Fehlerbedingung, Fenster und Mandantenkette?
 *   <li>Ist sie die Summe ueber „Zuletzt aufgefallen", solange hoechstens zehn Prozesse betroffen
 *       sind? Beide lesen jetzt dieselbe Menge live — der eine je Stunde, der andere je Prozess.
 *   <li><b>Die Dev-Zeile als Eichung:</b> Auf der Testkopie ist der Rollup vollstaendig, und ein
 *       Abgang ist nicht herstellbar ({@code RUNNING} kommt null Mal vor, Tests schreiben nicht).
 *       Der Ersatz ist hier deshalb eine Identitaet — die Seite mit angewandter Lesung traegt
 *       dieselben Zahlen wie die Seite, bei der sie ausgesetzt ist, also wie vor diesem Schritt.
 * </ol>
 *
 * <p><b>Die Vorprobe entscheidet, ob ein Fenster taugt:</b> Die Fehler des Rollups muessen die
 * Fehler aus {@code Message} treffen. Tun sie es nicht — etwa weil der Bestand seit dem Volllauf
 * angefasst worden ist —, sagt der Test das und nicht „der Ersatz ist falsch".
 *
 * <p><b>Keine Zahl aus dem Bestand in einer Zusicherung</b> (Regel T2): verglichen werden Lesungen
 * desselben Bestands; die Zahlen werden ausgegeben. Die Uhr steht am Anker, der Wasserstand ist der
 * der Testkopie; <b>geschrieben wird nirgends</b>.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class DashboardFehlerLiveDbIT {

  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  /** Die drei Mandanten mit Fehlern im Bestand (Sitzung 0 von M188) — in verschiedenen Paaren. */
  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS", "VOTG");

  @Autowired private DashboardRepository dashboardRepository;
  @Autowired private LiveRestRepository liveRestRepository;
  @Autowired private FehlerLiveRepository fehlerLiveRepository;
  @Autowired private Wasserstand wasserstand;
  @Autowired private MessageStatusClassifier statusClassifier;
  @Autowired private DienstLeseRepository dienstLeseRepository;
  @Autowired private DienstStatusClassifier dienstClassifier;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private DashboardService dienst(FehlerLiveService fehlerLive) {
    ZoneId zone = ZoneId.systemDefault();
    return new DashboardService(
        dashboardRepository,
        statusClassifier,
        Clock.fixed(ANKER.atZone(zone).toInstant(), zone),
        dienstLeseRepository,
        dienstClassifier,
        Optional.empty(),
        new LiveRestService(liveRestRepository, wasserstand),
        fehlerLive);
  }

  /** Die Seite mit der Lesung, wie im Betrieb. */
  private DashboardService angewandt() {
    return dienst(new FehlerLiveService(fehlerLiveRepository));
  }

  /** Die Seite, als waere die Lesung ausgefallen — also die Seite von vor diesem Schritt. */
  private DashboardService ausgesetzt() {
    return dienst(
        new FehlerLiveService(fehlerLiveRepository) {
          @Override
          public FehlerLiveErgebnis ermittle(
              MandantContext mandant, LocalDateTime von, LocalDateTime bis) {
            return FehlerLiveErgebnis.ausgesetzt();
          }
        });
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

  private long fehlerAusDerQuelle(String mandant, Zeitfenster fenster) {
    Integer anzahl =
        glassfishDsl
            .selectCount()
            .from(MESSAGE)
            .where(fehlerImFenster(mandant, fenster))
            .fetchOne(0, Integer.class);
    return anzahl == null ? 0 : anzahl;
  }

  private long betroffeneProzesse(String mandant, Zeitfenster fenster) {
    Integer anzahl =
        glassfishDsl
            .select(DSL.countDistinct(MESSAGE.PROCESSID))
            .from(MESSAGE)
            .where(fehlerImFenster(mandant, fenster))
            .fetchOne(0, Integer.class);
    return anzahl == null ? 0 : anzahl;
  }

  @Test
  @DisplayName("Je Paar: die Kachel Fehler ist COUNT(*) aus Message mit Fehlerbedingung und Kette")
  void kachel_fehler_ist_die_quelle() {
    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = paar.fenster(ANKER);
        long quelle = fehlerAusDerQuelle(mandant, fenster);

        DashboardResponse antwort = angewandt().landingpage(kontext, paar);

        assertThat(antwort.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
        assertThat(antwort.kacheln().fehler().anzahl())
            .as("%s/%s: Kachel Fehler gleich COUNT(*) aus Message", mandant, paar.code())
            .isEqualTo(quelle);
        assertThat(
                antwort.kacheln().fehler().arten().stream()
                    .mapToLong(FehlerartResponse::anzahl)
                    .sum())
            .as("%s/%s: die Fehlerarten zaehlen, was die Kachel zaehlt", mandant, paar.code())
            .isEqualTo(quelle);
        System.out.printf(
            "Fehler live | %-7s | %-3s | Fenster %s bis %s | Kachel Fehler %d = COUNT(*) %d%n",
            mandant,
            paar.code(),
            fenster.von(),
            fenster.bis(),
            antwort.kacheln().fehler().anzahl(),
            quelle);
      }
    }
  }

  @Test
  @DisplayName("Je Paar: die Kachel Fehler ist die Summe ueber Zuletzt aufgefallen (bis zehn)")
  void kachel_fehler_ist_zuletzt_aufgefallen() {
    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = paar.fenster(ANKER);
        long prozesse = betroffeneProzesse(mandant, fenster);
        DashboardResponse antwort = angewandt().landingpage(kontext, paar);
        long ausDemBlock =
            antwort.zuletztAufgefallen().stream()
                .mapToLong(AuffaelligerProzessResponse::anzahl)
                .sum();

        System.out.printf(
            "Fehler live | %-7s | %-3s | betroffene Prozesse %d | Zuletzt aufgefallen %d Zeilen,"
                + " %d Nachrichten | Kachel Fehler %d%n",
            mandant,
            paar.code(),
            prozesse,
            antwort.zuletztAufgefallen().size(),
            ausDemBlock,
            antwort.kacheln().fehler().anzahl());
        if (prozesse > DashboardService.AUFFAELLIG_HOECHSTENS) {
          continue;
        }
        assertThat(antwort.zuletztAufgefallen())
            .as("%s/%s: hoechstens zehn betroffene Prozesse — alle stehen da", mandant, paar.code())
            .hasSize((int) prozesse);
        assertThat(ausDemBlock)
            .as("%s/%s: Kachel und Block lesen dieselbe Menge live", mandant, paar.code())
            .isEqualTo(antwort.kacheln().fehler().anzahl());
      }
    }
  }

  /**
   * <b>Die Eichung der Dev-Zeile:</b> Keine Zahl der Uebersicht aendert sich — Verlauf je Eimer und
   * Einordnung, beide Kacheln samt Fehlerarten, beide Sichten der Verteilung, der Leerzustand und
   * alle uebrigen Bloecke. Einzig der Block {@code fehlerLive} sagt etwas anderes.
   */
  @Test
  @DisplayName(
      "Die Dev-Zeile: angewandt und ausgesetzt zeigen dieselbe Seite — der Ersatz ist hier eine Identitaet")
  void die_dev_zeile_ist_eine_identitaet() {
    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = paar.fenster(ANKER);
        DashboardResponse vorher = ausgesetzt().landingpage(kontext, paar);
        DashboardResponse nachher = angewandt().landingpage(kontext, paar);

        // Die Vorprobe: Die Fehler des Rollups treffen die Fehler aus Message. Sonst taugt das
        // Fenster nicht, und der Vergleich unten bewiese nichts ueber den Ersatz.
        assertThat(vorher.kacheln().fehler().anzahl())
            .as(
                "%s/%s: Fenster ungeeignet — die Fehler des Rollups treffen Message nicht; den"
                    + " Wasserstand pruefen und berichten",
                mandant, paar.code())
            .isEqualTo(fehlerAusDerQuelle(mandant, fenster));

        assertThat(vorher.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.AUSGESETZT);
        assertThat(nachher.fehlerLive().zustand()).isEqualTo(FehlerLiveZustand.ANGEWANDT);
        assertThat(nachher)
            .as("%s/%s: keine Zahl der Uebersicht darf sich aendern", mandant, paar.code())
            .usingRecursiveComparison()
            .ignoringFields("fehlerLive")
            .isEqualTo(vorher);
        System.out.printf(
            "Fehler live | %-7s | %-3s | Dev-Zeile gleich | Nachrichten %d | Fehler %d | %d Arten%n",
            mandant,
            paar.code(),
            nachher.kacheln().nachrichten(),
            nachher.kacheln().fehler().anzahl(),
            nachher.kacheln().fehler().arten().size());
      }
    }
  }
}
