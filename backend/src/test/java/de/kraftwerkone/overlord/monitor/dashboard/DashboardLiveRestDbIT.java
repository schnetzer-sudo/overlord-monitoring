package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.LiveRestRepository;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.LiveRestZustand;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
 * <b>Die Summenprobe des Live-Rests am Dashboard</b> (Teil B, 17.09.2026, {@code docs/live-rest.md}
 * §9b) — dieselbe Frage wie {@code ProzessbaumLiveRestDbIT.durch_den_dienst}, nur je Paar: Ist die
 * Kachel <i>Nachrichten</i> nach der Korrektur {@code COUNT(*)} aus {@code Message} im Fenster? Und
 * zaehlen Verlauf, Kachel und beide Sichten der Verteilung dieselbe Zahl?
 *
 * <p>Uhr und Wasserstand sind gestellt — der Anker des Profils {@code dev} und ein Wasserstand von
 * {@code 03:00}, also G = {@code 02:00} und ein Live-Bereich von drei Eimern bis {@code 05:00}. Der
 * Dienst wird von Hand gebaut, damit der Wasserstand aus dem Test kommt und nicht aus {@code
 * rollup_lauf}; <b>geschrieben wird nirgends</b>.
 *
 * <p><b>Die Vorprobe entscheidet, ob das Fenster taugt:</b> Stimmt der Rollup allein nicht mit
 * {@code Message} ueberein — etwa weil der Bestand seit dem Volllauf angefasst worden ist —, sagt
 * der Test das und nicht „die Korrektur ist falsch".
 *
 * <p><b>Keine Zahl aus dem Bestand in einer Zusicherung</b> (Regel T2): verglichen werden zwei
 * Lesungen desselben Bestands; die Zahlen selbst werden ausgegeben.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class DashboardLiveRestDbIT {

  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");
  private static final LocalDateTime WASSERSTAND = LocalDateTime.parse("2025-12-30T03:00:00");
  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS");

  @Autowired private DashboardRepository dashboardRepository;
  @Autowired private LiveRestRepository liveRestRepository;
  @Autowired private MessageStatusClassifier statusClassifier;
  @Autowired private DienstLeseRepository dienstLeseRepository;
  @Autowired private DienstStatusClassifier dienstClassifier;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private DashboardService dienst(Optional<LocalDateTime> wasserstand) {
    ZoneId zone = ZoneId.systemDefault();
    return new DashboardService(
        dashboardRepository,
        statusClassifier,
        Clock.fixed(ANKER.atZone(zone).toInstant(), zone),
        dienstLeseRepository,
        dienstClassifier,
        Optional.empty(),
        new LiveRestService(liveRestRepository, () -> wasserstand));
  }

  private long ausDerQuelle(String mandant, Zeitfenster fenster) {
    Integer anzahl =
        glassfishDsl
            .selectCount()
            .from(MESSAGE)
            .where(MESSAGE.MESSAGELASTUPDATE.ge(fenster.von()))
            .and(MESSAGE.MESSAGELASTUPDATE.lt(fenster.bis()))
            .and(
                DSL.exists(
                    DSL.selectOne()
                        .from(PROCESS)
                        .join(PROJECTMANDANT)
                        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
                        .where(PROCESS.PROCESSID.eq(MESSAGE.PROCESSID))
                        .and(PROJECTMANDANT.MANDANTID.eq(mandant))))
            .fetchOne(0, Integer.class);
    return anzahl == null ? 0 : anzahl;
  }

  private static long summe(List<VerteilungszeileResponse> zeilen) {
    return zeilen.stream().mapToLong(VerteilungszeileResponse::anzahl).sum();
  }

  @Test
  @DisplayName("Je Paar: die Kachel Nachrichten ist nach der Korrektur COUNT(*) aus Message")
  void kachel_ist_die_quelle() {
    LocalDateTime liveBis = ANKER.truncatedTo(ChronoUnit.HOURS).plusHours(1);
    System.out.printf(
        "Summenprobe Dashboard | Anker %s | W %s | Live-Bereich %s bis %s%n",
        ANKER, WASSERSTAND, WASSERSTAND.minusHours(1), liveBis);

    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      for (Rollupzeitraum paar : Rollupzeitraum.reihe()) {
        Zeitfenster fenster = paar.fenster(ANKER);
        long quelle = ausDerQuelle(mandant, fenster);

        // Die Vorprobe: Der Rollup allein muss Message ausserhalb des Live-Bereichs treffen.
        // Gerechnet als: Rollup im Fenster minus Rollup im Live-Bereich gleich Message im Fenster
        // minus Message im Live-Bereich. Trifft das nicht zu, taugt das Fenster nicht.
        DashboardResponse ohne =
            dienst(Optional.of(liveBis.plusHours(1))).landingpage(kontext, paar);
        assertThat(ohne.liveRest().zustand()).isEqualTo(LiveRestZustand.NICHT_NOETIG);
        long rollupImFenster = ohne.kacheln().nachrichten();
        long rollupImLiveBereich =
            liveRestRepository.ausDemRollup(kontext, WASSERSTAND.minusHours(1), liveBis).stream()
                .filter(
                    z -> !z.stunde().isBefore(fenster.von()) && z.stunde().isBefore(fenster.bis()))
                .mapToLong(z -> z.anzahl())
                .sum();
        long quelleImLiveBereich =
            liveRestRepository.ausDerQuelle(kontext, WASSERSTAND.minusHours(1), liveBis).stream()
                .filter(
                    z -> !z.stunde().isBefore(fenster.von()) && z.stunde().isBefore(fenster.bis()))
                .mapToLong(z -> z.anzahl())
                .sum();
        assertThat(rollupImFenster - rollupImLiveBereich)
            .as(
                "%s/%s: Fenster ungeeignet — der Rollup allein trifft Message ausserhalb des"
                    + " Live-Bereichs nicht; ein anderes Fenster waehlen und berichten",
                mandant, paar.code())
            .isEqualTo(quelle - quelleImLiveBereich);

        DashboardResponse mit = dienst(Optional.of(WASSERSTAND)).landingpage(kontext, paar);
        assertThat(mit.liveRest().zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
        assertThat(mit.liveRest().vollstaendigBis()).isNull();
        assertThat(mit.kacheln().nachrichten())
            .as(
                "%s/%s: Kachel Nachrichten nach der Korrektur gleich COUNT(*) aus Message",
                mandant, paar.code())
            .isEqualTo(quelle);
        assertThat(mit.verlauf().stream().mapToLong(VerlaufspunktResponse::gesamt).sum())
            .as("%s/%s: der Verlauf zaehlt, was die Kachel zaehlt", mandant, paar.code())
            .isEqualTo(quelle);
        assertThat(summe(mit.verteilung().partner().zeilen()))
            .as("%s/%s: die Partnersicht zaehlt, was die Kachel zaehlt", mandant, paar.code())
            .isEqualTo(quelle);
        assertThat(summe(mit.verteilung().richtung().zeilen()))
            .as("%s/%s: die Richtungssicht zaehlt, was die Kachel zaehlt", mandant, paar.code())
            .isEqualTo(quelle);
        assertThat(mit.leer()).isEqualTo(quelle == 0);

        System.out.printf(
            "Summenprobe Dashboard | %-7s | %-3s | Fenster %s bis %s | Rollup allein %d | Live-Bereich"
                + " Rollup %d, Quelle %d | verrechnet %d = COUNT(*) %d | Fehler %d | Partnerzeilen %d,"
                + " Richtungszeilen %d%n",
            mandant,
            paar.code(),
            fenster.von(),
            fenster.bis(),
            rollupImFenster,
            rollupImLiveBereich,
            quelleImLiveBereich,
            mit.kacheln().nachrichten(),
            quelle,
            mit.kacheln().fehler().anzahl(),
            mit.verteilung().partner().zeilen().size(),
            mit.verteilung().richtung().zeilen().size());
      }
    }
  }
}
