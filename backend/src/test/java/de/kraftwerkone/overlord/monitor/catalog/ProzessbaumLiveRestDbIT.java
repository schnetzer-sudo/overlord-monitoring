package de.kraftwerkone.overlord.monitor.catalog;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.LiveRestRepository;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.LiveRestZeile;
import de.kraftwerkone.overlord.monitor.common.LiveRestZustand;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <b>Die Summenprobe des Live-Rests</b> ({@code docs/live-rest.md}): Fuer ein vollstaendig
 * gerechnetes Fenster aus dem dichten Bestand und ein <b>gesetztes</b> G gilt je {@code
 * (process_id, message_status)}:
 *
 * <pre>
 * Rollup(Fenster) − Rollup(Live-Bereich ∩ Fenster) + Quelle(Live-Bereich ∩ Fenster) = COUNT(*) aus Message im Fenster
 * </pre>
 *
 * <p><b>Uhr und Wasserstand werden gesetzt</b> — die Uhr auf den Anker der Testkopie, der
 * Wasserstand ueber die Schnittstelle {@code common/Wasserstand} —, damit historische Stunden aus
 * dem dichten Bestand als laufende Stunde dienen. <b>Kein Schreibzugriff</b> auf {@code
 * rollup_lauf} oder {@code message_rollup} der geteilten Testkopie; gelesen wird nur.
 *
 * <p><b>Die Vorprobe entscheidet, ob das Fenster taugt:</b> Stimmt schon der Rollup allein nicht
 * mit {@code Message} ueberein, ist das Fenster ungeeignet, und der Test sagt es — dann ist ein
 * anderes zu waehlen und zu berichten, nicht der Erwartungswert nachzuziehen.
 *
 * <p><b>Regel T2:</b> Kein Erwartungswert aus dem Bestand. Verglichen werden Lesungen desselben
 * Bestands miteinander; die Zahlen werden <b>ausgegeben und nicht behauptet</b>. <b>Regel T1:</b>
 * keine Laufzeit in einer Zusicherung.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class ProzessbaumLiveRestDbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — als gesetzte Uhr, nicht gelesen. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  /** W so gesetzt, dass G = 02:00 ist und der Live-Bereich drei Eimer traegt: 02:00 bis 05:00. */
  private static final LocalDateTime WASSERSTAND = LocalDateTime.parse("2025-12-30T03:00:00");

  private static final LocalDateTime G = WASSERSTAND.minusHours(1);

  /** Der dominante und ein kleiner Mandant (Regel L7); beide tragen Verkehr im Live-Bereich. */
  private static final List<String> MANDANTEN = List.of("NEXANS", "SUTTONS");

  @Autowired private ProzessbaumRepository prozessbaumRepository;
  @Autowired private LiveRestRepository liveRestRepository;
  @Autowired private MessageStatusClassifier statusClassifier;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private static String schluessel(String processId, String messageStatus) {
    return processId + " " + messageStatus;
  }

  /** Die Kennzahlen des Paares als Abbildung Schluessel → Summe. */
  private Map<String, Long> rollupImFenster(String mandant, Zeitfenster fenster) {
    Map<String, Long> summe = new HashMap<>();
    for (Prozesskennzahlzeile zeile :
        prozessbaumRepository.kennzahlen(
            new MandantContext(mandant),
            Baumfenster.frei(fenster.von(), fenster.bis()).segmente(ANKER))) {
      summe.merge(schluessel(zeile.processId(), zeile.messageStatus()), zeile.anzahl(), Long::sum);
    }
    return summe;
  }

  /**
   * {@code COUNT(*)} aus {@code Message} im Fenster, je Prozess und Rohstatus, mit Mandantenkette —
   * die Zahl, die die Uebertragungsliste zeigt. Ein Testzugriff, kein Anwendungscode (Regel L2).
   */
  private Map<String, Long> quelleImFenster(String mandant, Zeitfenster fenster) {
    Map<String, Long> summe = new HashMap<>();
    glassfishDsl
        .select(MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS, DSL.count())
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
        .groupBy(MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS)
        .forEach(
            satz ->
                summe.merge(
                    schluessel(satz.value1(), satz.value2()), (long) satz.value3(), Long::sum));
    return summe;
  }

  private static void verrechne(
      Map<String, Long> summe, List<LiveRestZeile> zeilen, Zeitfenster fenster, int vorzeichen) {
    for (LiveRestZeile zeile : zeilen) {
      if (!zeile.stunde().isBefore(fenster.von()) && zeile.stunde().isBefore(fenster.bis())) {
        summe.merge(
            schluessel(zeile.processId(), zeile.messageStatus()),
            vorzeichen * zeile.anzahl(),
            Long::sum);
      }
    }
  }

  private static Map<String, Long> ohneNullen(Map<String, Long> summe) {
    Map<String, Long> bereinigt = new HashMap<>();
    summe.forEach(
        (schluessel, wert) -> {
          if (wert != 0) {
            bereinigt.put(schluessel, wert);
          }
        });
    return bereinigt;
  }

  @Test
  @DisplayName("Rollup minus Live-Eimer plus Live-Zaehlung ist COUNT(*) aus Message, je Schluessel")
  void summenprobe_je_schluessel() {
    Zeitfenster fenster = Rollupzeitraum.STUNDEN_48.fenster(ANKER);
    LocalDateTime liveBis = ANKER.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);
    System.out.printf(
        "Summenprobe | Fenster %s bis %s | G %s | Live-Bereich bis %s%n",
        fenster.von(), fenster.bis(), G, liveBis);

    for (String mandant : MANDANTEN) {
      MandantContext kontext = new MandantContext(mandant);
      Map<String, Long> rollup = rollupImFenster(mandant, fenster);
      Map<String, Long> quelle = quelleImFenster(mandant, fenster);

      assertThat(rollup).as("%s: Ohne Rollupzeilen bewiese die Probe nichts", mandant).isNotEmpty();
      // Die Vorprobe: Stimmt der Rollup allein nicht mit Message ueberein, taugt das Fenster nicht.
      assertThat(rollup)
          .as(
              "%s: Fenster ungeeignet — der Rollup allein trifft Message hier nicht; ein anderes"
                  + " Fenster waehlen und berichten",
              mandant)
          .containsExactlyInAnyOrderEntriesOf(quelle);

      List<LiveRestZeile> minus = liveRestRepository.ausDemRollup(kontext, G, liveBis);
      List<LiveRestZeile> plus = liveRestRepository.ausDerQuelle(kontext, G, liveBis);
      assertThat(minus)
          .as("%s: Ohne Rollupzeilen im Live-Bereich bewiese die Probe nichts", mandant)
          .isNotEmpty();
      assertThat(plus).as("%s: Ohne Live-Zeilen bewiese die Probe nichts", mandant).isNotEmpty();

      Map<String, Long> verrechnet = new HashMap<>(rollup);
      verrechne(verrechnet, minus, fenster, -1);
      verrechne(verrechnet, plus, fenster, +1);

      assertThat(ohneNullen(verrechnet))
          .as("%s: Rollup − Live-Eimer + Live-Zaehlung, je (process_id, message_status)", mandant)
          .containsExactlyInAnyOrderEntriesOf(quelle);

      long nachrichtenRollup = rollup.values().stream().mapToLong(Long::longValue).sum();
      long nachrichtenMinus = minus.stream().mapToLong(LiveRestZeile::anzahl).sum();
      long nachrichtenPlus = plus.stream().mapToLong(LiveRestZeile::anzahl).sum();
      System.out.printf(
          "Summenprobe | %-7s | Schluessel im Fenster %d | Nachrichten %d | Live-Eimer: %d Rollupzeilen"
              + " mit %d Nachrichten, %d Quellzeilen mit %d Nachrichten%n",
          mandant,
          rollup.size(),
          nachrichtenRollup,
          minus.size(),
          nachrichtenMinus,
          plus.size(),
          nachrichtenPlus);
    }
  }

  /**
   * Dieselbe Probe <b>durch den Dienst</b>, mit gesetzter Uhr und gesetztem Wasserstand: Die
   * Kopfzahl des Baums ist die Nachrichtenzahl aus {@code Message} im Fenster — die Zahl, die die
   * Liste daneben zeigt —, und der Block sagt {@code ANGEWANDT}.
   */
  @Test
  @DisplayName("Durch den Dienst: die Kopfzahl des Baums ist COUNT(*) aus Message im Fenster")
  void durch_den_dienst() {
    ZoneId zone = ZoneId.systemDefault();
    Clock uhr = Clock.fixed(ANKER.atZone(zone).toInstant(), zone);
    ProzessbaumService dienst =
        new ProzessbaumService(
            prozessbaumRepository,
            statusClassifier,
            uhr,
            new LiveRestService(liveRestRepository, () -> Optional.of(WASSERSTAND)));
    Zeitfenster fenster = Rollupzeitraum.STUNDEN_48.fenster(ANKER);

    for (String mandant : MANDANTEN) {
      ProzessbaumResponse antwort =
          dienst.baum(
              new MandantContext(mandant),
              Baumfenster.paar(Rollupzeitraum.STUNDEN_48),
              Baumgliederung.PARTNER);
      long ausDerQuelle =
          quelleImFenster(mandant, fenster).values().stream().mapToLong(Long::longValue).sum();

      assertThat(antwort.liveRest().zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(antwort.liveRest().vollstaendigBis()).isNull();
      assertThat(antwort.gesamt().nachrichten())
          .as("%s: Baum und Liste zeigen dieselbe Zahl", mandant)
          .isEqualTo(ausDerQuelle);
      System.out.printf(
          "Summenprobe | %-7s | durch den Dienst: %d Nachrichten, %d Fehler, liveRest %s%n",
          mandant,
          antwort.gesamt().nachrichten(),
          antwort.gesamt().fehler(),
          antwort.liveRest().zustand());
    }
  }
}
