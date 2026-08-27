package de.kraftwerkone.overlord.monitor.rollup;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.ROLLUP_LAUF;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Messlaeufer M96 — wie gross ist der naechtliche Sprung?</b>
 *
 * <p><b>Das ist Messwerkzeug und kein Anwendungscode.</b> Er gehoert zur Messrunde M94–M98 vor
 * Schritt 10b ({@code docs/messungen-schritt10b.md}, Sitzung 5b) und ist dort als solcher benannt.
 * Er steht unter {@code src/test}, weil der Auftrag genau das als Ausnahme freigibt: Die Frage
 * laesst sich nicht abfragen, sie muss nachgespielt werden.
 *
 * <h2>Warum sie sich nicht abfragen laesst</h2>
 *
 * <p>{@code Message} haelt nur den heutigen Stand. Was eine Folge stuendlicher Delta-Laeufe
 * geschrieben haette, steht nirgends — es entsteht erst, wenn man sie faehrt. Gemessen wird der
 * Abstand zwischen dem <b>inkrementellen</b> Stand nach 48 Delta-Laeufen und dem Stand nach einem
 * <b>Volllauf</b> ueber dasselbe Fenster, beide gegen die direkte Zeilenzahl aus {@code Message}.
 *
 * <h2>Das Fenster</h2>
 *
 * <p>Die 48 Stunden vor dem Anker der Anwendungsuhr ({@code 2025-12-30 04:09:47}, {@code
 * docs/datenzugriff.md} §6), auf ganze Stunden ausgedehnt wie {@link RollupFenster} es tut: {@code
 * 2025-12-28 05:00} bis {@code 2025-12-30 05:00}. Dasselbe Fenster wie Paar P1 in M94 und M95.
 * <b>Die Grenzen stehen als Literal</b> und kommen nicht aus der Uhr — Regel Z1 gilt auch fuer
 * Messwerkzeug, und ein verstellter {@code Clock} wuerde ausserdem die Protokollzeiten in {@code
 * rollup_lauf} verfaelschen.
 *
 * <h2>Wie die 48 Laeufe geschnitten sind</h2>
 *
 * <p>Nicht als 48 saubere Einzelstunden, sondern so, wie der stuendliche Job sie wirklich
 * schneidet: {@link RollupFenster#delta(LocalDateTime, LocalDateTime)} greift {@value
 * RollupFenster#NACHLAUF_MINUTEN} Minuten hinter den Wasserstand zurueck, und weil der Wasserstand
 * selbst ein Stundenanfang ist, faellt der Rueckgriff <b>stets in die vorige Stunde</b>. Jeder Lauf
 * ab dem zweiten umfasst deshalb <b>zwei</b> Eimer und ueberlappt seinen Vorgaenger um einen. Genau
 * dieser Ueberlapp ist der Grund, warum {@code RollupSchreibRepository.ersetzeFenster} loescht und
 * neu schreibt statt hochzuzaehlen — und genau deshalb ist er hier nachgespielt und nicht
 * wegvereinfacht.
 *
 * <p><b>Der erste Lauf ist der Bootstrap.</b> Er hat keinen Vorgaenger, hinter den er
 * zurueckgreifen koennte, und deckt deshalb nur seine eigene Stunde ab. In der Wirklichkeit ist das
 * der Lauf, der nach {@code MIN(Message.MessageLastUpdate)} zurueckfaellt und den ganzen Bestand
 * nachholt; hier beginnt die Folge am Fensteranfang, damit sie das Fenster nicht verlaesst.
 *
 * <h2>Was er auf der Testkopie hinterlaesst</h2>
 *
 * <p>Er schreibt ausschliesslich in {@code overlord_monitor}; {@code GlassfishDB} wird nur gelesen
 * (Regel S1). Seine {@code rollup_lauf}-Zeilen raeumt er <b>namentlich</b> ab — nur die Kennungen,
 * die er selbst bekommen hat. Der Rollupbereich des Fensters ist vor dem Lauf in {@code
 * message_rollup_m96_sicherung} gesichert (Sitzung 5a) und wird in Sitzung 8 daraus
 * wiederhergestellt.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class MessungM96DbIT {

  /** Untere Grenze, einschliesslich — 48 Stunden vor dem Anker, auf die Stunde ausgedehnt. */
  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-28T05:00");

  /** Obere Grenze, ausschliesslich — der Anfang der Stunde nach dem Anker. */
  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T05:00");

  private static final int STUNDEN = 48;

  @Autowired private RollupJob job;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired
  @Qualifier("monitorDsl") private DSLContext monitorDsl;

  /** Die Laufzeilen, die dieser Laeufer angelegt hat — und nur die werden abgeraeumt. */
  private final List<Long> eigeneLaeufe = new ArrayList<>();

  @AfterEach
  void raeumeDieEigenenSpurenAb() {
    if (!eigeneLaeufe.isEmpty()) {
      monitorDsl.deleteFrom(ROLLUP_LAUF).where(ROLLUP_LAUF.ID.in(eigeneLaeufe)).execute();
      melde("rollup_lauf abgeraeumt (namentlich)", String.valueOf(eigeneLaeufe.size()));
      eigeneLaeufe.clear();
    }
  }

  @Test
  @DisplayName("M96 — 48 Delta-Laeufe nachgespielt, gegen Volllauf und direkte Zahl gehalten")
  void nachtsprung() {
    melde("fenster_von", FENSTER_VON.toString());
    melde("fenster_bis", FENSTER_BIS.toString());

    long direkt = direkteZahl();
    melde("direkt_aus_message", String.valueOf(direkt));

    long vorherZeilen = rollupZeilen();
    long vorherSumme = rollupSumme();
    melde("vorher_rollupzeilen", String.valueOf(vorherZeilen));
    melde("vorher_summe_anzahl", String.valueOf(vorherSumme));

    // ── 3. Bereich leeren ────────────────────────────────────────────────────
    int geloescht =
        monitorDsl
            .deleteFrom(MESSAGE_ROLLUP)
            .where(MESSAGE_ROLLUP.STUNDE.ge(FENSTER_VON))
            .and(MESSAGE_ROLLUP.STUNDE.lt(FENSTER_BIS))
            .execute();
    melde("geleert_zeilen", String.valueOf(geloescht));
    melde("nach_dem_leeren_rollupzeilen", String.valueOf(rollupZeilen()));

    // ── 4. 48 Delta-Laeufe nachspielen ───────────────────────────────────────
    long gelesenSumme = 0;
    long geschriebenSumme = 0;
    long dauerSumme = 0;
    long dauerErster = 0;
    long dauerGroesste = 0;
    long dauerKleinste = Long.MAX_VALUE;
    long eimerSumme = 0;

    for (int i = 0; i < STUNDEN; i++) {
      RollupFenster fenster = deltaFenster(i);
      RollupErgebnis ergebnis = lauf(fenster, LaufArt.DELTA);
      gelesenSumme += ergebnis.nachrichten();
      geschriebenSumme += ergebnis.zeilenGeschrieben();
      eimerSumme += fenster.stundeneimer();
      long ms = ergebnis.dauer().toMillis();
      dauerSumme += ms;
      if (i == 0) {
        dauerErster = ms;
      } else {
        dauerGroesste = Math.max(dauerGroesste, ms);
        dauerKleinste = Math.min(dauerKleinste, ms);
      }
    }

    long inkrementellZeilen = rollupZeilen();
    long inkrementellSumme = rollupSumme();
    melde("delta_laeufe", String.valueOf(STUNDEN));
    melde("delta_eimer_verarbeitet", String.valueOf(eimerSumme));
    melde("delta_gelesen_summe", String.valueOf(gelesenSumme));
    melde("delta_geschrieben_summe", String.valueOf(geschriebenSumme));
    melde("delta_dauer_erster_ms", String.valueOf(dauerErster));
    melde("delta_dauer_kleinste_ms", String.valueOf(dauerKleinste));
    melde("delta_dauer_groesste_ms", String.valueOf(dauerGroesste));
    melde("delta_dauer_summe_ms", String.valueOf(dauerSumme));
    melde("inkrementell_rollupzeilen", String.valueOf(inkrementellZeilen));
    melde("inkrementell_summe_anzahl", String.valueOf(inkrementellSumme));

    // ── 5. Volllauf ueber dasselbe Fenster ───────────────────────────────────
    RollupErgebnis voll = lauf(new RollupFenster(FENSTER_VON, FENSTER_BIS), LaufArt.VOLL);
    long vollZeilen = rollupZeilen();
    long vollSumme = rollupSumme();
    melde("voll_scheiben", String.valueOf(voll.scheiben()));
    melde("voll_dauer_ms", String.valueOf(voll.dauer().toMillis()));
    melde("voll_rollupzeilen", String.valueOf(vollZeilen));
    melde("voll_summe_anzahl", String.valueOf(vollSumme));

    // ── 6. Die Zahlen gegeneinander ──────────────────────────────────────────
    melde("ueberzaehlung_absolut", String.valueOf(inkrementellSumme - vollSumme));
    melde(
        "ueberzaehlung_prozent_x10000",
        String.valueOf(anteil(inkrementellSumme - vollSumme, vollSumme)));
    melde("ueberlapp_vermieden_absolut", String.valueOf(gelesenSumme - vollSumme));
    melde(
        "ueberlapp_vermieden_prozent_x10000",
        String.valueOf(anteil(gelesenSumme - vollSumme, vollSumme)));
    melde("voll_gegen_direkt", String.valueOf(vollSumme - direkt));

    // ── Die Aussagen, die tragen muessen ─────────────────────────────────────
    assertThat(vollSumme)
        .as("Der Volllauf trifft die direkte Zeilenzahl aus Message auf die Einheit")
        .isEqualTo(direkt);
    assertThat(inkrementellSumme)
        .as("48 Delta-Laeufe ergeben denselben Stand wie ein Volllauf")
        .isEqualTo(vollSumme);
    assertThat(inkrementellZeilen).as("... auch in der Zeilenzahl").isEqualTo(vollZeilen);
    assertThat(vollZeilen)
        .as("Der Bestand des Fensters ist wiederhergestellt")
        .isEqualTo(vorherZeilen);
    assertThat(vollSumme).as("... und seine Summe ebenso").isEqualTo(vorherSumme);
    assertThat(gelesenSumme)
        .as("Der Ueberlapp ist wirklich gefahren worden und nicht wegvereinfacht")
        .isGreaterThan(vollSumme);
  }

  /**
   * Das Fenster des i-ten Laufs, so wie der stuendliche Job es schneiden wuerde.
   *
   * <p>Lauf 0 ist der Bootstrap ohne Rueckgriff. Ab Lauf 1 kommt {@link RollupFenster#delta} zum
   * Zug, mit dem Wasserstand des Vorgaengers als {@code W} und derselben Stunde als {@code jetzt}.
   */
  private static RollupFenster deltaFenster(int i) {
    if (i == 0) {
      return new RollupFenster(FENSTER_VON, FENSTER_VON.plusHours(1));
    }
    LocalDateTime wasserstand = FENSTER_VON.plusHours(i);
    return RollupFenster.delta(wasserstand, wasserstand);
  }

  private RollupErgebnis lauf(RollupFenster fenster, LaufArt art) {
    RollupErgebnis ergebnis = job.fuehreAus(fenster.von(), fenster.bis(), art);
    eigeneLaeufe.add(ergebnis.laufId());
    return ergebnis;
  }

  private long direkteZahl() {
    Integer zahl =
        glassfishDsl
            .selectCount()
            .from(MESSAGE)
            .where(MESSAGE.MESSAGELASTUPDATE.ge(FENSTER_VON))
            .and(MESSAGE.MESSAGELASTUPDATE.lt(FENSTER_BIS))
            .fetchOne(0, Integer.class);
    return zahl == null ? 0L : zahl;
  }

  private long rollupZeilen() {
    Integer zahl =
        monitorDsl
            .selectCount()
            .from(MESSAGE_ROLLUP)
            .where(MESSAGE_ROLLUP.STUNDE.ge(FENSTER_VON))
            .and(MESSAGE_ROLLUP.STUNDE.lt(FENSTER_BIS))
            .fetchOne(0, Integer.class);
    return zahl == null ? 0L : zahl;
  }

  private long rollupSumme() {
    Long summe =
        monitorDsl
            .select(DSL.sum(MESSAGE_ROLLUP.ANZAHL))
            .from(MESSAGE_ROLLUP)
            .where(MESSAGE_ROLLUP.STUNDE.ge(FENSTER_VON))
            .and(MESSAGE_ROLLUP.STUNDE.lt(FENSTER_BIS))
            .fetchOne(0, Long.class);
    return summe == null ? 0L : summe;
  }

  /** Anteil in Hundertstel-Promille, damit der Bericht ohne Gleitkomma auskommt. */
  private static long anteil(long teil, long ganzes) {
    return ganzes == 0 ? 0 : Math.round(teil * 1_000_000.0 / ganzes);
  }

  /** Eine Zeile des Messberichts. Der Praefix macht sie aus der Maven-Ausgabe greifbar. */
  private static void melde(String schluessel, String wert) {
    System.out.println("M96 | " + schluessel + " | " + wert);
  }
}
