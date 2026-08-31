package de.kraftwerkone.overlord.monitor.rollup;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_MONAT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_TAG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.ROLLUP_LAUF;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Der Rollup-Lauf gegen die Testkopie.
 *
 * <h2>Der wichtigste Test dieses Schritts ist {@link #derselbe_lauf_zweimal_ergibt_dasselbe}</h2>
 *
 * <p>Er ist der einzige Waechter ueber die Entscheidung „loeschen und neu schreiben statt
 * hochzaehlen". Faellt sie, ist der Fehler <b>still</b>: Die Zahlen sind zu hoch, aber sie sehen
 * plausibel aus, und sie werden mit jedem Lauf ein bisschen falscher.
 *
 * <h2>Kein Mandantentrennungstest, und das ist Absicht</h2>
 *
 * <p>Schritt 10a hat <b>keine Flaeche, die einen Mandanten kennt</b>: {@code message_rollup} traegt
 * nach Entscheidung E-a keine Mandantenspalte, es gibt keinen Endpunkt, und der Job liest
 * ausdruecklich ueber alle Mandanten (die dritte benannte Ausnahme, siehe {@link
 * RollupLeseRepository}). Die Mandantentrennung entsteht in 10b beim Join ueber {@code
 * ProjectMandant} — dort ist der Isolationstest Pflicht (Regel M4). <b>Er fehlt hier nicht, es gibt
 * ihn hier nicht.</b>
 *
 * <h2>Was dieser Test auf der Testkopie hinterlaesst</h2>
 *
 * <p>Er schreibt ausschliesslich in {@code overlord_monitor} — {@code GlassfishDB} wird nur gelesen
 * (Regel S1). Seine {@code rollup_lauf}-Zeilen raeumt er <b>namentlich</b> wieder ab: nur die
 * Kennungen, die er selbst bekommen hat. Ein Aufraeumen ueber ein Zeitfenster oder ueber die
 * Laufart loeschte fremde Zeilen — genau der Fehler, der in {@code BestandslaufDbIT} schon einmal
 * kuratierte Daten gekostet hat.
 *
 * <p>Die geschriebenen {@code message_rollup}-Zeilen bleiben stehen, und das ist richtig so: Sie
 * sind <b>korrekt</b> gerechnet. Es gibt hier nichts wiederherzustellen, was besser waere als das
 * Ergebnis des Laufs.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class RollupDbIT {

  /**
   * Ein kleines Fenster im dichten Bestand: <b>358 Nachrichten</b> (gemessen 26.08.2026). Es endet
   * auf dem Anker der Testkopie und liegt damit sicher im abrufbaren Bereich.
   */
  private static final LocalDateTime DICHT_VON = LocalDateTime.parse("2025-12-30T03:00");

  private static final LocalDateTime DICHT_BIS = LocalDateTime.parse("2025-12-30T05:00");

  /** Ein 24-Stunden-Fenster im dichten Bestand: <b>6.248 Nachrichten</b> (gemessen 26.08.2026). */
  private static final LocalDateTime TAG_VON = LocalDateTime.parse("2025-12-29T05:00");

  private static final LocalDateTime TAG_BIS = LocalDateTime.parse("2025-12-30T05:00");

  /**
   * Eine Stunde ohne jeden Verkehr. Sie liegt in den <b>fuenf leeren Monaten</b> 2026-01 bis
   * 2026-05, die M92 als eigene Scheiben ausweist — dort traegt der Bestand null Zeilen.
   */
  private static final LocalDateTime LEER_VON = LocalDateTime.parse("2026-02-15T03:00");

  private static final LocalDateTime LEER_BIS = LocalDateTime.parse("2026-02-15T04:00");

  /**
   * Ein Statuswert, den es im Quellbestand nicht gibt. M87 zaehlt <b>zwoelf</b> verschiedene {@code
   * MessageStatus}; dieser ist keiner davon und kann deshalb nie aus der Aggregation fallen.
   */
  private static final String ERFUNDENER_STATUS = "IT_ALTLAST";

  @Autowired private RollupJob job;
  @Autowired private RollupSchreibRepository schreibRepository;
  @Autowired private RollupLeseRepository leseRepository;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired
  @Qualifier("monitorDsl") private DSLContext monitorDsl;

  @Autowired private PlatformTransactionManager transaktionen;

  /** Die Laufzeilen, die dieser Test angelegt hat — und nur die werden abgeraeumt. */
  private final List<Long> eigeneLaeufe = new ArrayList<>();

  @AfterEach
  void raeumeDieEigenenSpurenAb() {
    if (!eigeneLaeufe.isEmpty()) {
      monitorDsl.deleteFrom(ROLLUP_LAUF).where(ROLLUP_LAUF.ID.in(eigeneLaeufe)).execute();
      eigeneLaeufe.clear();
    }
    // Die erfundene Statuszeile faellt im Erfolgsfall schon dem Lauf zum Opfer. Bleibt sie stehen,
    // ist der Test rot — und dann soll sie trotzdem nicht liegenbleiben.
    monitorDsl
        .deleteFrom(MESSAGE_ROLLUP)
        .where(MESSAGE_ROLLUP.MESSAGE_STATUS.eq(ERFUNDENER_STATUS))
        .execute();
    monitorDsl
        .deleteFrom(MESSAGE_ROLLUP_TAG)
        .where(MESSAGE_ROLLUP_TAG.MESSAGE_STATUS.eq(ERFUNDENER_STATUS))
        .execute();
    monitorDsl
        .deleteFrom(MESSAGE_ROLLUP_MONAT)
        .where(MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS.eq(ERFUNDENER_STATUS))
        .execute();
  }

  /** Der Inhalt eines Tages in der Tagesebene, sortiert. */
  private Result<Record3<String, String, Integer>> tagesInhalt(LocalDate tag) {
    return monitorDsl
        .select(
            MESSAGE_ROLLUP_TAG.PROCESS_ID,
            MESSAGE_ROLLUP_TAG.MESSAGE_STATUS,
            MESSAGE_ROLLUP_TAG.ANZAHL)
        .from(MESSAGE_ROLLUP_TAG)
        .where(MESSAGE_ROLLUP_TAG.TAG.eq(tag))
        .orderBy(MESSAGE_ROLLUP_TAG.PROCESS_ID, MESSAGE_ROLLUP_TAG.MESSAGE_STATUS)
        .fetch();
  }

  /** Dasselbe, aber aus der Stundenebene gerechnet — die Gegenprobe zur materialisierten Ebene. */
  private Result<Record3<String, String, Integer>> tagesInhaltAusStunden(LocalDate tag) {
    return monitorDsl
        .select(
            MESSAGE_ROLLUP.PROCESS_ID,
            MESSAGE_ROLLUP.MESSAGE_STATUS,
            DSL.sum(MESSAGE_ROLLUP.ANZAHL).cast(Integer.class))
        .from(MESSAGE_ROLLUP)
        .where(MESSAGE_ROLLUP.STUNDE.ge(tag.atStartOfDay()))
        .and(MESSAGE_ROLLUP.STUNDE.lt(tag.plusDays(1).atStartOfDay()))
        .groupBy(MESSAGE_ROLLUP.PROCESS_ID, MESSAGE_ROLLUP.MESSAGE_STATUS)
        .orderBy(MESSAGE_ROLLUP.PROCESS_ID, MESSAGE_ROLLUP.MESSAGE_STATUS)
        .fetch();
  }

  /** Der Inhalt eines Monats in der Monatsebene, sortiert. */
  private Result<Record3<String, String, Integer>> monatsInhalt(LocalDate monat) {
    return monitorDsl
        .select(
            MESSAGE_ROLLUP_MONAT.PROCESS_ID,
            MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS,
            MESSAGE_ROLLUP_MONAT.ANZAHL)
        .from(MESSAGE_ROLLUP_MONAT)
        .where(MESSAGE_ROLLUP_MONAT.MONAT.eq(monat))
        .orderBy(MESSAGE_ROLLUP_MONAT.PROCESS_ID, MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS)
        .fetch();
  }

  /** Dasselbe, aber aus der Tagesebene gerechnet — die Gegenprobe zur materialisierten Ebene. */
  private Result<Record3<String, String, Integer>> monatsInhaltAusTagen(LocalDate monat) {
    return monitorDsl
        .select(
            MESSAGE_ROLLUP_TAG.PROCESS_ID,
            MESSAGE_ROLLUP_TAG.MESSAGE_STATUS,
            DSL.sum(MESSAGE_ROLLUP_TAG.ANZAHL).cast(Integer.class))
        .from(MESSAGE_ROLLUP_TAG)
        .where(MESSAGE_ROLLUP_TAG.TAG.ge(monat))
        .and(MESSAGE_ROLLUP_TAG.TAG.lt(monat.plusMonths(1)))
        .groupBy(MESSAGE_ROLLUP_TAG.PROCESS_ID, MESSAGE_ROLLUP_TAG.MESSAGE_STATUS)
        .orderBy(MESSAGE_ROLLUP_TAG.PROCESS_ID, MESSAGE_ROLLUP_TAG.MESSAGE_STATUS)
        .fetch();
  }

  /**
   * Legt eine Monatszeile an, die die Tagesebene <b>nicht</b> hergibt.
   *
   * <p><b>Ohne sie waeren mehrere Monatstests zahnlos.</b> Sie vergleichen die materialisierte
   * Monatsebene mit der aus der Tagesebene gerechneten — und eine Monatszeile, die aus einem
   * <i>frueheren</i> Lauf korrekt dasteht, besteht diesen Vergleich auch dann, wenn der Lauf sie
   * gar nicht angefasst hat. Die Altlast macht den Unterschied sichtbar: Sie verschwindet nur, wenn
   * wirklich geloescht und neu geschrieben worden ist.
   */
  private void legeMonatsAltlastAn(LocalDate monat) {
    monitorDsl
        .insertInto(MESSAGE_ROLLUP_MONAT)
        .set(MESSAGE_ROLLUP_MONAT.MONAT, monat)
        .set(MESSAGE_ROLLUP_MONAT.PROCESS_ID, "it-altlast-prozess")
        .set(MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS, ERFUNDENER_STATUS)
        .set(MESSAGE_ROLLUP_MONAT.ANZAHL, 4711)
        .execute();
    assertThat(monatsInhalt(monat))
        .as("Die Vorbedingung: Die Altlast steht wirklich in der Monatsebene")
        .anySatisfy(zeile -> assertThat(zeile.value2()).isEqualTo(ERFUNDENER_STATUS));
  }

  private RollupErgebnis lauf(LocalDateTime von, LocalDateTime bis, LaufArt art) {
    RollupErgebnis ergebnis = job.fuehreAus(von, bis, art);
    eigeneLaeufe.add(ergebnis.laufId());
    return ergebnis;
  }

  /** Der Inhalt des Fensters, sortiert — die Form, in der sich zwei Laeufe vergleichen lassen. */
  private Result<Record4<LocalDateTime, String, String, Integer>> inhalt(
      LocalDateTime von, LocalDateTime bis) {
    return monitorDsl
        .select(
            MESSAGE_ROLLUP.STUNDE,
            MESSAGE_ROLLUP.PROCESS_ID,
            MESSAGE_ROLLUP.MESSAGE_STATUS,
            MESSAGE_ROLLUP.ANZAHL)
        .from(MESSAGE_ROLLUP)
        .where(MESSAGE_ROLLUP.STUNDE.ge(von))
        .and(MESSAGE_ROLLUP.STUNDE.lt(bis))
        .orderBy(MESSAGE_ROLLUP.STUNDE, MESSAGE_ROLLUP.PROCESS_ID, MESSAGE_ROLLUP.MESSAGE_STATUS)
        .fetch();
  }

  /** Die Gegenprobe: direkt aus {@code Message} gezaehlt, ohne den Rollup. */
  private int nachrichtenLautQuelle(LocalDateTime von, LocalDateTime bis) {
    return glassfishDsl.fetchCount(
        MESSAGE, MESSAGE.MESSAGELASTUPDATE.ge(von).and(MESSAGE.MESSAGELASTUPDATE.lt(bis)));
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Idempotenz: Derselbe Lauf zweimal ergibt dieselben Zahlen")
  void derselbe_lauf_zweimal_ergibt_dasselbe() {
    RollupErgebnis erster = lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);
    Result<?> nachDemErsten = inhalt(DICHT_VON, DICHT_BIS);

    RollupErgebnis zweiter = lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);
    Result<?> nachDemZweiten = inhalt(DICHT_VON, DICHT_BIS);

    assertThat(nachDemZweiten)
        .as(
            "Der einzige Waechter ueber 'loeschen und neu schreiben'. Wuerde hochgezaehlt, staenden"
                + " hier dieselben Schluessel mit doppelter Anzahl — und das saehe plausibel aus")
        .isEqualTo(nachDemErsten);
    assertThat(zweiter.zeilenGeschrieben()).isEqualTo(erster.zeilenGeschrieben());
    assertThat(zweiter.nachrichten()).isEqualTo(erster.nachrichten());
  }

  @Test
  @DisplayName("Ueberlapp: Zwei aufeinanderfolgende Delta-Laeufe verdoppeln ihn nicht")
  void zwei_delta_laeufe_verdoppeln_den_ueberlapp_nicht() {
    // Der echte Ablauf, nachgestellt: Der zweite Lauf greift 15 Minuten hinter den Wasserstand des
    // ersten zurueck und rechnet dessen letzten Eimer damit noch einmal.
    RollupFenster ersterLauf = RollupFenster.delta(DICHT_VON, DICHT_BIS.minusMinutes(1));
    lauf(ersterLauf.von(), ersterLauf.bis(), LaufArt.DELTA);

    RollupFenster zweiterLauf = RollupFenster.delta(ersterLauf.bis(), DICHT_BIS.plusHours(1));
    assertThat(zweiterLauf.von())
        .as("Ohne Ueberlapp prueft dieser Test nichts")
        .isBefore(ersterLauf.bis());
    lauf(zweiterLauf.von(), zweiterLauf.bis(), LaufArt.DELTA);

    LocalDateTime von = ersterLauf.von();
    LocalDateTime bis = zweiterLauf.bis();
    assertThat(schreibRepository.summiereAnzahl(new RollupFenster(von, bis)))
        .as(
            "SUM(anzahl) ueber den gesamten von beiden Laeufen beruehrten Bereich muss die direkte"
                + " Zeilenzahl aus Message treffen. Wuerde der Ueberlapp doppelt gezaehlt, laege"
                + " sie darueber")
        .isEqualTo(nachrichtenLautQuelle(von, bis));
  }

  @Test
  @DisplayName("Statuswechsel im Eimer: Keine alte Statuszeile bleibt stehen")
  void alte_statuszeile_bleibt_nicht_stehen() {
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);
    String prozess = inhalt(DICHT_VON, DICHT_BIS).getFirst().get(MESSAGE_ROLLUP.PROCESS_ID);

    // Der Zustand, den ein Statuswechsel innerhalb desselben Eimers hinterlaesst: eine Zeile, die
    // die Quelle nicht mehr hergibt. Ein INSERT ... ON DUPLICATE KEY UPDATE liesse sie stehen.
    monitorDsl
        .insertInto(MESSAGE_ROLLUP)
        .set(MESSAGE_ROLLUP.STUNDE, DICHT_VON)
        .set(MESSAGE_ROLLUP.PROCESS_ID, prozess)
        .set(MESSAGE_ROLLUP.MESSAGE_STATUS, ERFUNDENER_STATUS)
        .set(MESSAGE_ROLLUP.ANZAHL, 4711)
        .execute();
    assertThat(inhalt(DICHT_VON, DICHT_BIS))
        .as("Die Vorbedingung: Die Altlast steht wirklich in der Tabelle")
        .anySatisfy(
            zeile ->
                assertThat(zeile.get(MESSAGE_ROLLUP.MESSAGE_STATUS)).isEqualTo(ERFUNDENER_STATUS));

    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    assertThat(inhalt(DICHT_VON, DICHT_BIS))
        .as(
            "Der Eimer wird ersetzt und nicht ergaenzt — eine Statuszeile, die die Quelle nicht"
                + " mehr hergibt, verschwindet")
        .noneSatisfy(
            zeile ->
                assertThat(zeile.get(MESSAGE_ROLLUP.MESSAGE_STATUS)).isEqualTo(ERFUNDENER_STATUS));
  }

  @Test
  @DisplayName("Summenprobe: SUM(anzahl) trifft die direkte Zeilenzahl aus Message")
  void summenprobe_ueber_ein_abgeschlossenes_fenster() {
    RollupErgebnis ergebnis = lauf(TAG_VON, TAG_BIS, LaufArt.DELTA);
    int ausDerQuelle = nachrichtenLautQuelle(TAG_VON, TAG_BIS);

    assertThat(ausDerQuelle)
        .as("Das Fenster traegt Verkehr — sonst prueft die Summenprobe nichts")
        .isPositive();
    assertThat(schreibRepository.summiereAnzahl(new RollupFenster(TAG_VON, TAG_BIS)))
        .as(
            "Jede Nachricht genau einmal gezaehlt, keine doppelt, keine verloren. Dieselbe Probe"
                + " hat M89 ueber den Gesamtbestand gefuehrt: 3.341.519")
        .isEqualTo(ausDerQuelle);
    assertThat(ergebnis.nachrichten())
        .as("Und der Lauf berichtet dieselbe Zahl, die er geschrieben hat")
        .isEqualTo(ausDerQuelle);
    assertThat(ergebnis.zeilenGeschrieben())
        .as(
            "Der Rollup verdichtet — es stehen weniger Zeilen da als Nachrichten (M87: Faktor 9,96)")
        .isLessThan(ausDerQuelle);
  }

  @Test
  @DisplayName("Delta gegen Voll: Ueber dasselbe Fenster liefern beide dasselbe")
  void delta_und_voll_liefern_ueber_dasselbe_fenster_dasselbe() {
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);
    Result<?> nachDelta = inhalt(DICHT_VON, DICHT_BIS);

    lauf(DICHT_VON, DICHT_BIS, LaufArt.VOLL);
    Result<?> nachVoll = inhalt(DICHT_VON, DICHT_BIS);

    assertThat(nachVoll)
        .as(
            "Beide Laufarten gehen durch denselben Code und dieselbe Abfrage — die Laufart steht"
                + " nur im Protokoll. Waeren es zwei Abfragen, liefen sie auseinander, und der"
                + " Nachtlauf aenderte still die Zahlen, die tagsueber jemand gesehen hat")
        .isEqualTo(nachDelta);
  }

  @Test
  @DisplayName("Wasserstand: Ein abgebrochener Lauf hebt ihn nicht")
  void abgebrochener_lauf_hebt_den_wasserstand_nicht() {
    Optional<LocalDateTime> vorher = schreibRepository.wasserstand();
    LocalDateTime weitInDerZukunft = LocalDateTime.parse("2099-01-01T00:00");

    // Ein Lauf, der mitten im Schreiben abgestuerzt ist: Zeile da, beendet_am fehlt.
    long abgestuerzt =
        schreibRepository.starteLauf(
            LaufArt.VOLL,
            new RollupFenster(weitInDerZukunft, weitInDerZukunft.plusHours(1)),
            LocalDateTime.parse("2026-08-26T12:00:00"));
    eigeneLaeufe.add(abgestuerzt);

    assertThat(schreibRepository.wasserstand())
        .as(
            "Sonst uebersprungen der naechste Lauf einen Bereich, der nur zur Haelfte geschrieben"
                + " ist — und die Luecke faende niemand mehr")
        .isEqualTo(vorher);

    // Und derselbe Lauf, abgeschlossen, aber mit Fehler: abgeschlossen ist nicht verlaesslich.
    schreibRepository.vermerkeFehler(
        abgestuerzt, LocalDateTime.parse("2026-08-26T12:00:01"), "IT: absichtlich gescheitert");

    assertThat(schreibRepository.wasserstand())
        .as("beendet_am allein genuegt nicht — fehler muss NULL sein")
        .isEqualTo(vorher);
  }

  @Test
  @DisplayName("Leere Stunde: keine Zeile, und der Wasserstand steigt trotzdem")
  void leere_stunde_erzeugt_keine_zeile_und_hebt_den_wasserstand() {
    assertThat(nachrichtenLautQuelle(LEER_VON, LEER_BIS))
        .as("Die Vorbedingung: Diese Stunde traegt wirklich keinen Verkehr (M92, leere Monate)")
        .isZero();

    RollupErgebnis ergebnis = lauf(LEER_VON, LEER_BIS, LaufArt.DELTA);

    assertThat(ergebnis.zeilenGeschrieben()).isZero();
    assertThat(inhalt(LEER_VON, LEER_BIS))
        .as("Eine Stunde ohne Verkehr erzeugt KEINE Zeile")
        .isEmpty();
    assertThat(schreibRepository.wasserstand())
        .as(
            "Und genau deshalb kann der Wasserstand nicht MAX(stunde) sein: Der saehe hier"
                + " 'nichts passiert' und 'noch nicht nachgesehen' als dasselbe")
        .isPresent()
        .get()
        .satisfies(stand -> assertThat(stand).isAfterOrEqualTo(LEER_BIS));
  }

  @Test
  @DisplayName("Das Fenster des Delta-Laufs baut auf dem Wasserstand des vorigen auf")
  void das_naechste_fenster_baut_auf_dem_wasserstand_auf() {
    RollupErgebnis erster = lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    RollupFenster naechstes = job.ermittleFenster(LaufArt.DELTA);

    assertThat(schreibRepository.wasserstand())
        .as(
            "Nach einem abgeschlossenen, fehlerfreien Lauf steht der Wasserstand mindestens auf"
                + " dessen Fensterende")
        .isPresent()
        .get()
        .satisfies(stand -> assertThat(stand).isAfterOrEqualTo(erster.fenster().bis()));
    assertThat(naechstes.von())
        .as(
            "Und der naechste Lauf beginnt hoechstens eine Stunde davor — der Rueckgriff, der den"
                + " zuletzt geschriebenen Eimer noch einmal rechnet")
        .isAfterOrEqualTo(erster.fenster().bis().minusHours(1));
  }

  @Test
  @DisplayName("Der fruehester Zeitstempel der Quelle ist der Anfang des Volllaufs")
  void fruehester_zeitstempel_ist_der_anfang_des_volllaufs() {
    assertThat(leseRepository.fruehesteAenderung())
        .as(
            "M92 nennt fuer die Scheibe 2024-10 den fruehesten Zeitstempel 2024-10-01 02:00:28."
                + " Weicht er ab, ist die Testkopie neu befuellt worden — dann sind auch die"
                + " Zahlen dieses Tests neu zu erheben")
        .isPresent()
        .get()
        .satisfies(
            fruehest -> assertThat(fruehest).isEqualTo(LocalDateTime.parse("2024-10-01T02:00:28")));
  }

  // ── Die Tagesebene (Schritt 10b-1, Teil C) ─────────────────────────────────────────────────

  /**
   * <b>Der wichtigste Test der Tagesebene.</b> Sie ist aus der Stundenebene abgeleitet und muss
   * deshalb Zeile fuer Zeile deren Summe sein — nicht ungefaehr, sondern genau.
   */
  @Test
  @DisplayName("Tagesebene: Zeile fuer Zeile die Summe der Stundenebene")
  void tagesebene_ist_die_summe_der_stundenebene() {
    lauf(TAG_VON, TAG_BIS, LaufArt.DELTA);

    for (LocalDate tag : List.of(LocalDate.parse("2025-12-29"), LocalDate.parse("2025-12-30"))) {
      assertThat(tagesInhaltAusStunden(tag))
          .as("Der Tag %s traegt Verkehr — sonst prueft der Vergleich nichts", tag)
          .isNotEmpty();
      assertThat(tagesInhalt(tag))
          .as("Die materialisierte Tagesebene fuer %s", tag)
          .isEqualTo(tagesInhaltAusStunden(tag));
    }
  }

  /**
   * <b>Ein Tageseimer wird ueber den GANZEN Tag gerechnet, auch wenn das Fenster zwei Stunden
   * umfasst.</b> Das ist die Eigenschaft, an der es sonst still schiefginge: Ein Tageseimer aus
   * zwei Stunden truege zwei Stunden und behauptete, ein Tag zu sein.
   *
   * <p>Der Nachweis braucht ein Fenster, das <b>echt kleiner</b> ist als der Tag — und einen Tag,
   * der ausserhalb dieses Fensters weiteren Verkehr traegt. Beides trifft auf den 30.12.2025 zu.
   */
  @Test
  @DisplayName("Ein Zwei-Stunden-Fenster rechnet den ganzen Tageseimer, nicht nur seine Stunden")
  void tageseimer_umfasst_den_ganzen_tag() {
    LocalDate tag = LocalDate.parse("2025-12-30");
    // Erst den ganzen Tag rechnen, damit die Stundenebene vollstaendig dasteht.
    lauf(tag.atStartOfDay(), tag.plusDays(1).atStartOfDay(), LaufArt.DELTA);
    int ganzerTag = tagesInhaltAusStunden(tag).stream().mapToInt(zeile -> zeile.value3()).sum();
    Integer nurImFenster =
        monitorDsl
            .select(DSL.sum(MESSAGE_ROLLUP.ANZAHL))
            .from(MESSAGE_ROLLUP)
            .where(MESSAGE_ROLLUP.STUNDE.ge(DICHT_VON))
            .and(MESSAGE_ROLLUP.STUNDE.lt(DICHT_BIS))
            .fetchOne(0, Integer.class);

    assertThat(nurImFenster)
        .as("Die Vorbedingung: Das Zwei-Stunden-Fenster deckt nur einen Teil des Tages ab")
        .isNotNull()
        .isPositive()
        .isLessThan(ganzerTag);

    // Und jetzt nur die zwei Stunden neu rechnen.
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    assertThat(tagesInhalt(tag).stream().mapToInt(zeile -> zeile.value3()).sum())
        .as(
            "Der Tageseimer ist die Summe seiner 24 Stundeneimer und nicht die der Stunden, die"
                + " zufaellig im Fenster lagen")
        .isEqualTo(ganzerTag);
  }

  /**
   * Dasselbe fuer die Tagesebene, was {@link #alte_statuszeile_bleibt_nicht_stehen} fuer die
   * Stundenebene prueft: Geloescht und neu geschrieben, nie hochgezaehlt.
   */
  @Test
  @DisplayName("Tagesebene: Eine Zeile, die die Stundenebene nicht mehr hergibt, bleibt nicht")
  void alte_tageszeile_bleibt_nicht_stehen() {
    LocalDate tag = LocalDate.parse("2025-12-30");
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);
    String prozess = tagesInhalt(tag).getFirst().value1();

    monitorDsl
        .insertInto(MESSAGE_ROLLUP_TAG)
        .set(MESSAGE_ROLLUP_TAG.TAG, tag)
        .set(MESSAGE_ROLLUP_TAG.PROCESS_ID, prozess)
        .set(MESSAGE_ROLLUP_TAG.MESSAGE_STATUS, ERFUNDENER_STATUS)
        .set(MESSAGE_ROLLUP_TAG.ANZAHL, 4711)
        .execute();
    assertThat(tagesInhalt(tag))
        .as("Die Vorbedingung: Die Altlast steht wirklich in der Tagesebene")
        .anySatisfy(zeile -> assertThat(zeile.value2()).isEqualTo(ERFUNDENER_STATUS));

    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    assertThat(tagesInhalt(tag))
        .noneSatisfy(zeile -> assertThat(zeile.value2()).isEqualTo(ERFUNDENER_STATUS));
  }

  @Test
  @DisplayName("Tagesebene: Derselbe Lauf zweimal ergibt zeilengleich dasselbe")
  void tagesebene_ist_idempotent() {
    LocalDate tag = LocalDate.parse("2025-12-30");
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);
    Result<Record3<String, String, Integer>> nachDemErsten = tagesInhalt(tag);

    RollupErgebnis zweiter = lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    assertThat(nachDemErsten).isNotEmpty();
    assertThat(tagesInhalt(tag)).isEqualTo(nachDemErsten);
    assertThat(zweiter.tageszeilenGeschrieben())
        .as("Und der Lauf berichtet, wie viele Tageszeilen er geschrieben hat")
        .isEqualTo(nachDemErsten.size());
  }

  /**
   * Ein Fenster ueber die Mitternachtsgrenze beruehrt <b>zwei</b> Tage, und beide muessen danach
   * stimmen. Ohne die Unterscheidung „letzte verarbeitete Stunde" statt „obere Fenstergrenze"
   * bliebe hier ein Tageseimer geloescht und ungeschrieben zurueck.
   */
  @Test
  @DisplayName("Ein Fenster ueber Mitternacht schreibt beide Tageseimer richtig")
  void fenster_ueber_mitternacht_schreibt_beide_tage() {
    lauf(
        LocalDateTime.parse("2025-12-29T23:00"),
        LocalDateTime.parse("2025-12-30T01:00"),
        LaufArt.DELTA);

    for (LocalDate tag : List.of(LocalDate.parse("2025-12-29"), LocalDate.parse("2025-12-30"))) {
      assertThat(tagesInhalt(tag))
          .as("Beide beruehrten Tage stehen und stimmen (%s)", tag)
          .isNotEmpty()
          .isEqualTo(tagesInhaltAusStunden(tag));
    }
  }

  /**
   * Die Summenprobe ueber <b>beide</b> Ebenen: Ueber ein abgeschlossenes Fenster tragen beide
   * dieselbe Zahl — und beide dieselbe wie {@code Message} selbst.
   */
  @Test
  @DisplayName("Summenprobe: Beide Ebenen tragen dieselbe Zahl wie die Quelle")
  void beide_ebenen_tragen_dieselbe_summe() {
    LocalDate tag = LocalDate.parse("2025-12-29");
    lauf(tag.atStartOfDay(), tag.plusDays(1).atStartOfDay(), LaufArt.DELTA);

    int ausDerQuelle = nachrichtenLautQuelle(tag.atStartOfDay(), tag.plusDays(1).atStartOfDay());
    long ausDerStundenebene =
        schreibRepository.summiereAnzahl(
            new RollupFenster(tag.atStartOfDay(), tag.plusDays(1).atStartOfDay()));
    int ausDerTagesebene = tagesInhalt(tag).stream().mapToInt(zeile -> zeile.value3()).sum();

    assertThat(ausDerQuelle).isPositive();
    assertThat(ausDerStundenebene).isEqualTo(ausDerQuelle);
    assertThat(ausDerTagesebene)
        .as("Die Tagesebene verdichtet die Zeilenzahl, nicht die Nachrichtenzahl")
        .isEqualTo(ausDerQuelle);
    assertThat(tagesInhalt(tag).size())
        .as("Weniger Zeilen als die Stundenebene — M87 misst dafuer Faktor 2,73")
        .isLessThan(inhalt(tag.atStartOfDay(), tag.plusDays(1).atStartOfDay()).size());
  }

  // ─── Die Monatsebene (Schritt 10b-2) ───────────────────────────────────────────────────────

  /**
   * <b>Die Monatsebene ist Zeile fuer Zeile die Summe der Tagesebene</b> — dieselbe Probe wie eine
   * Ebene tiefer, und aus demselben Grund die wichtigste der drei: Waere sie es nicht, waere der
   * Fehler still.
   */
  @Test
  @DisplayName("Monatsebene: Zeile fuer Zeile die Summe der Tagesebene")
  void monatsebene_ist_die_summe_der_tagesebene() {
    LocalDate monat = LocalDate.parse("2025-12-01");
    // Ohne die Altlast bestuende der Vergleich unten auch dann, wenn der Lauf die Monatsebene gar
    // nicht angefasst haette — eine korrekte Zeile aus einem frueheren Lauf saehe genauso aus.
    legeMonatsAltlastAn(monat);

    lauf(TAG_VON, TAG_BIS, LaufArt.DELTA);

    assertThat(monatsInhaltAusTagen(monat))
        .as("Der Monat %s traegt Verkehr — sonst prueft der Vergleich nichts", monat)
        .isNotEmpty();
    assertThat(monatsInhalt(monat))
        .as("Die materialisierte Monatsebene fuer %s", monat)
        .isEqualTo(monatsInhaltAusTagen(monat));
  }

  /**
   * <b>Ein Monatseimer wird ueber den GANZEN Monat gerechnet, auch wenn das Fenster zwei Stunden
   * umfasst.</b> Das Gegenstueck zu {@link #tageseimer_umfasst_den_ganzen_tag}, eine Ebene hoeher —
   * und die Eigenschaft, an der es sonst still schiefginge.
   */
  @Test
  @DisplayName("Ein Zwei-Stunden-Fenster rechnet den ganzen Monatseimer, nicht nur seine Tage")
  void monatseimer_umfasst_den_ganzen_monat() {
    LocalDate monat = LocalDate.parse("2025-12-01");
    LocalDate tag = LocalDate.parse("2025-12-30");

    // Erst einen GANZEN Tag rechnen. Damit traegt der Monatseimer nachweislich mehr als das
    // Zwei-Stunden-Fenster darunter -- und zwar aus einem Lauf dieses Tests, nicht aus dem, was
    // ein anderer Lauf in der geteilten Testkopie hinterlassen hat (Regel T2).
    lauf(tag.atStartOfDay(), tag.plusDays(1).atStartOfDay(), LaufArt.DELTA);
    int ganzerMonat = monatsInhalt(monat).stream().mapToInt(zeile -> zeile.value3()).sum();
    Integer nurDasFenster =
        monitorDsl
            .select(DSL.sum(MESSAGE_ROLLUP.ANZAHL))
            .from(MESSAGE_ROLLUP)
            .where(MESSAGE_ROLLUP.STUNDE.ge(DICHT_VON))
            .and(MESSAGE_ROLLUP.STUNDE.lt(DICHT_BIS))
            .fetchOne(0, Integer.class);

    assertThat(nurDasFenster)
        .as("Die Vorbedingung: Der ganze Tag traegt mehr als die zwei Stunden in ihm")
        .isNotNull()
        .isPositive()
        .isLessThan(ganzerMonat);

    // Und jetzt nur zwei Stunden dieses Tages neu rechnen.
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    assertThat(monatsInhalt(monat).stream().mapToInt(zeile -> zeile.value3()).sum())
        .as(
            "Der Monatseimer ist die Summe seiner Tageseimer und nicht die der Tage, die"
                + " zufaellig im Fenster lagen")
        .isEqualTo(ganzerMonat);
  }

  /**
   * Dasselbe fuer die Monatsebene, was {@link #alte_tageszeile_bleibt_nicht_stehen} fuer die
   * Tagesebene prueft: Geloescht und neu geschrieben, nie hochgezaehlt.
   */
  @Test
  @DisplayName("Monatsebene: Eine Zeile, die die Tagesebene nicht mehr hergibt, bleibt nicht")
  void alte_monatszeile_bleibt_nicht_stehen() {
    LocalDate monat = LocalDate.parse("2025-12-01");
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    legeMonatsAltlastAn(monat);

    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    assertThat(monatsInhalt(monat))
        .noneSatisfy(zeile -> assertThat(zeile.value2()).isEqualTo(ERFUNDENER_STATUS));
  }

  @Test
  @DisplayName("Monatsebene: Derselbe Lauf zweimal ergibt zeilengleich dasselbe")
  void monatsebene_ist_idempotent() {
    LocalDate monat = LocalDate.parse("2025-12-01");
    lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);
    Result<Record3<String, String, Integer>> nachDemErsten = monatsInhalt(monat);

    RollupErgebnis zweiter = lauf(DICHT_VON, DICHT_BIS, LaufArt.DELTA);

    assertThat(nachDemErsten).isNotEmpty();
    assertThat(monatsInhalt(monat)).isEqualTo(nachDemErsten);
    assertThat(zweiter.monatszeilenGeschrieben())
        .as("Und der Lauf berichtet, wie viele Monatszeilen er geschrieben hat")
        .isEqualTo(nachDemErsten.size());
  }

  /**
   * <b>Der Monatswechsel im Fenster</b> — das Gegenstueck zu {@link
   * #fenster_ueber_mitternacht_schreibt_beide_tage}, eine Ebene hoeher.
   *
   * <p>Das Fenster laeuft vom 30.11.2025 23:00 bis zum 01.12.2025 01:00 und beruehrt damit zwei
   * Kalendermonate. Beide muessen danach vollstaendig dastehen — und zwar ueber ihre <b>ganze</b>
   * Laenge, nicht nur ueber die zwei Stunden.
   */
  @Test
  @DisplayName("Ein Fenster ueber den Monatswechsel schreibt beide Monatseimer richtig")
  void fenster_ueber_den_monatswechsel_schreibt_beide_monate() {
    List<LocalDate> beide = List.of(LocalDate.parse("2025-11-01"), LocalDate.parse("2025-12-01"));
    // In BEIDE Monate eine Altlast. Ohne sie bliebe der Test gruen, wenn der Lauf einen der beiden
    // gar nicht anfasste — die korrekte Zeile aus einem frueheren Lauf staende ja noch da.
    beide.forEach(this::legeMonatsAltlastAn);

    lauf(
        LocalDateTime.parse("2025-11-30T23:00"),
        LocalDateTime.parse("2025-12-01T01:00"),
        LaufArt.DELTA);

    for (LocalDate monat : beide) {
      assertThat(monatsInhalt(monat))
          .as("Beide beruehrten Monate stehen und stimmen (%s)", monat)
          .isNotEmpty()
          .isEqualTo(monatsInhaltAusTagen(monat));
    }
  }

  /**
   * <b>Die Summenprobe ueber alle drei Ebenen.</b> Sie ist der einzige Grund, den Verdichtungen zu
   * trauen: Jede Ebene traegt dieselbe Zahl wie die Quelle und weniger Zeilen als die vorige.
   */
  @Test
  @DisplayName("Summenprobe: Alle drei Ebenen tragen dieselbe Zahl wie die Quelle")
  void alle_drei_ebenen_tragen_dieselbe_summe() {
    LocalDate monat = LocalDate.parse("2025-12-01");
    // Der ganze Monat, damit die Monatsebene mit der Quelle vergleichbar ist.
    lauf(monat.atStartOfDay(), monat.plusMonths(1).atStartOfDay(), LaufArt.DELTA);

    int ausDerQuelle =
        nachrichtenLautQuelle(monat.atStartOfDay(), monat.plusMonths(1).atStartOfDay());
    long ausDerStundenebene =
        schreibRepository.summiereAnzahl(
            new RollupFenster(monat.atStartOfDay(), monat.plusMonths(1).atStartOfDay()));
    int ausDerTagesebene =
        monitorDsl
            .select(DSL.sum(MESSAGE_ROLLUP_TAG.ANZAHL))
            .from(MESSAGE_ROLLUP_TAG)
            .where(MESSAGE_ROLLUP_TAG.TAG.ge(monat))
            .and(MESSAGE_ROLLUP_TAG.TAG.lt(monat.plusMonths(1)))
            .fetchOne(0, Integer.class);
    int ausDerMonatsebene = monatsInhalt(monat).stream().mapToInt(zeile -> zeile.value3()).sum();

    assertThat(ausDerQuelle).isPositive();
    assertThat(ausDerStundenebene).isEqualTo(ausDerQuelle);
    assertThat(ausDerTagesebene).isEqualTo(ausDerQuelle);
    assertThat(ausDerMonatsebene)
        .as("Die Monatsebene verdichtet die Zeilenzahl, nicht die Nachrichtenzahl")
        .isEqualTo(ausDerQuelle);

    int stundenzeilen = inhalt(monat.atStartOfDay(), monat.plusMonths(1).atStartOfDay()).size();
    int tageszeilen =
        monitorDsl.fetchCount(
            MESSAGE_ROLLUP_TAG,
            MESSAGE_ROLLUP_TAG.TAG.ge(monat).and(MESSAGE_ROLLUP_TAG.TAG.lt(monat.plusMonths(1))));
    assertThat(tageszeilen).isLessThan(stundenzeilen);
    assertThat(monatsInhalt(monat).size())
        .as("M87 misst fuer Tag gegen Monat Faktor 10,29 — hier genuegt: es sind weniger")
        .isLessThan(tageszeilen);
  }

  /**
   * <b>Keine Ebene bleibt bei einem Abbruch zurueck.</b>
   *
   * <p>Die drei Ebenen entstehen in <b>einer</b> Transaktion, und der Grund steht an {@code
   * RollupSchreibRepository.ersetzeFenster}: Braeche es dazwischen ab, stuenden sie auf
   * verschiedenen Staenden, und niemand saehe es. Ein Test, der das prueft, kann den Abbruch nicht
   * im Anwendungscode ausloesen, ohne ihn zu aendern — <b>er kann aber die Transaktion von aussen
   * zuruecknehmen</b>.
   *
   * <p><b>Was er damit beweist:</b> Keine der drei Ebenen macht eine <i>eigene</i> Transaktion auf
   * und committet frueh. Bekaeme {@code rechneMonatsEbeneNeu} ein
   * {@code @Transactional(REQUIRES_NEW)}, wuerde dieser Test rot.
   *
   * <p><b>Was er nicht beweist, und das gehoert hierher:</b> dass {@code ersetzeFenster} ueberhaupt
   * eine Transaktion mitbringt. Er bringt seine eigene mit, und {@code monitorDsl} haengt ueber
   * {@code TransactionAwareDataSourceProxy} an ihr — ohne die Annotation waere er genauso gruen.
   * <b>Diese Haelfte prueft {@code RollupTransaktionsgrenzenTest}</b>, und sie ist am 31.08.2026
   * aus einem Befund entstanden, der genau durch diese Luecke gefallen war.
   *
   * <p><b>Die Zaehne stecken im ersten Teil:</b> Innerhalb der Transaktion muss die Zeile in
   * <b>jeder</b> Ebene sichtbar sein. Ohne diesen Nachweis pruefte der zweite Teil nur, dass nichts
   * da ist, was nie da war.
   *
   * <p>Gearbeitet wird im leeren Februar 2026 mit dem erfundenen Status: Dort schreibt der Lauf von
   * sich aus nichts, und die Zeile ist eindeutig die des Tests.
   */
  @Test
  @DisplayName("Ein Abbruch nimmt alle drei Ebenen zurueck, nicht nur eine")
  void keine_ebene_bleibt_bei_einem_abbruch_zurueck() {
    LocalDate tag = LEER_VON.toLocalDate();
    LocalDate monat = tag.withDayOfMonth(1);
    List<RollupZeile> zeilen =
        List.of(new RollupZeile(LEER_VON, "it-abbruch-prozess", ERFUNDENER_STATUS, 3));

    new TransactionTemplate(transaktionen)
        .executeWithoutResult(
            status -> {
              RollupZeilenzahlen geschrieben =
                  schreibRepository.ersetzeFenster(new RollupFenster(LEER_VON, LEER_BIS), zeilen);

              assertThat(geschrieben.stundenzeilen()).isEqualTo(1);
              assertThat(geschrieben.tageszeilen())
                  .as("Ohne eine Zeile in jeder Ebene bewiese die Ruecknahme unten nichts")
                  .isEqualTo(1);
              assertThat(geschrieben.monatszeilen()).isEqualTo(1);
              assertThat(tagesInhalt(tag)).hasSize(1);
              assertThat(monatsInhalt(monat)).hasSize(1);

              status.setRollbackOnly();
            });

    assertThat(
            monitorDsl.fetchCount(
                MESSAGE_ROLLUP, MESSAGE_ROLLUP.MESSAGE_STATUS.eq(ERFUNDENER_STATUS)))
        .as("Die Stundenebene ist zurueckgenommen")
        .isZero();
    assertThat(tagesInhalt(tag)).as("Und die Tagesebene ebenfalls").isEmpty();
    assertThat(monatsInhalt(monat))
        .as("Und die Monatsebene — sonst haetten die drei Ebenen nicht dieselbe Transaktion")
        .isEmpty();
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Offener Punkt 54: Eimer unterhalb des Bestandsanfangs (31.08.2026, Schritt 10b-2)
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Eine Rollupzeile in jeder Ebene, <b>vor</b> dem Bestandsanfang der Quelle.
   *
   * <p>Der frueheste {@code MessageLastUpdate} der Testkopie ist {@code 2024-10-01T02:00:28}; der
   * September 2024 liegt sicher davor und ist in allen drei Ebenen leer. <b>Es ist dieselbe Lage,
   * die entstuende, wenn das Altsystem alte Nachrichten entfernt</b> — nur hergestellt statt
   * abgewartet, denn auf der Testkopie wird nichts entfernt (offener Punkt 54, Belegvermerk).
   */
  private static final LocalDateTime VOR_DEM_BESTAND = LocalDateTime.parse("2024-09-01T00:00");

  /**
   * Die Anzahl der angelegten Zeilen — dieselbe in allen drei Ebenen, damit die Differenz eindeutig
   * ist.
   */
  private static final int EINGEFRORENE_ANZAHL = 17;

  private void legeEingefroreneEimerAn() {
    monitorDsl
        .insertInto(MESSAGE_ROLLUP)
        .set(MESSAGE_ROLLUP.STUNDE, VOR_DEM_BESTAND)
        .set(MESSAGE_ROLLUP.PROCESS_ID, "it-eingefroren-prozess")
        .set(MESSAGE_ROLLUP.MESSAGE_STATUS, ERFUNDENER_STATUS)
        .set(MESSAGE_ROLLUP.ANZAHL, EINGEFRORENE_ANZAHL)
        .execute();
    monitorDsl
        .insertInto(MESSAGE_ROLLUP_TAG)
        .set(MESSAGE_ROLLUP_TAG.TAG, VOR_DEM_BESTAND.toLocalDate())
        .set(MESSAGE_ROLLUP_TAG.PROCESS_ID, "it-eingefroren-prozess")
        .set(MESSAGE_ROLLUP_TAG.MESSAGE_STATUS, ERFUNDENER_STATUS)
        .set(MESSAGE_ROLLUP_TAG.ANZAHL, EINGEFRORENE_ANZAHL)
        .execute();
    monitorDsl
        .insertInto(MESSAGE_ROLLUP_MONAT)
        .set(MESSAGE_ROLLUP_MONAT.MONAT, VOR_DEM_BESTAND.toLocalDate())
        .set(MESSAGE_ROLLUP_MONAT.PROCESS_ID, "it-eingefroren-prozess")
        .set(MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS, ERFUNDENER_STATUS)
        .set(MESSAGE_ROLLUP_MONAT.ANZAHL, EINGEFRORENE_ANZAHL)
        .execute();
  }

  /**
   * Fuehrt {@code aufgabe} aus und sammelt die Protokollzeilen ein, die {@link RollupJob} dabei
   * geschrieben hat.
   *
   * <p><b>Warum ueber einen Logback-Anhang und nicht ueber eine Rueckgabe:</b> Die Erkennung
   * <i>ist</i> die Protokollzeile — sie schreibt bewusst keine Spalte und aendert am Lauf nichts.
   * Ein Test, der nur {@link RollupJob#eingefroreneEimer()} riefe, bewiese, dass die Rechnung
   * stimmt, aber nicht, dass der Lauf sie anstellt. Der Anhang ist deterministisch und behauptet
   * nichts ueber Wanduhrzeit (Regel T1).
   */
  private <T> T mitMitschrift(List<ILoggingEvent> ziel, Supplier<T> aufgabe) {
    Logger protokoll = (Logger) LoggerFactory.getLogger(RollupJob.class);
    ListAppender<ILoggingEvent> anhang = new ListAppender<>();
    anhang.start();
    protokoll.addAppender(anhang);
    try {
      return aufgabe.get();
    } finally {
      protokoll.detachAppender(anhang);
      anhang.stop();
      ziel.addAll(anhang.list);
    }
  }

  private static List<String> warnungen(List<ILoggingEvent> mitschrift) {
    return mitschrift.stream()
        .filter(zeile -> zeile.getLevel().toInt() >= Level.WARN.toInt())
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
  }

  /**
   * <b>Der Fall aus offenem Punkt 54, hergestellt statt abgewartet</b> — entschieden am 27.08.2026
   * (<i>nicht loeschen, erkennen</i>), gebaut am 31.08.2026.
   *
   * <p>Der Test legt in jeder der drei Ebenen eine Zeile vor dem Bestandsanfang an und verlangt vom
   * Volllauf <b>zweierlei</b>: Er <i>meldet</i> sie, und er <i>laesst sie stehen</i>. Beides
   * gehoert zusammen — eine Meldung, die den Zustand anschliessend beseitigt, waere genau das
   * Loeschen, das der Auftraggeber abgelehnt hat.
   *
   * <p><b>Alle drei Ebenen</b> (offener Punkt 68): Die abgeleiteten Ebenen frieren mit ein, weil
   * ihre Rechenbereiche ebenfalls aus dem Fenster kommen. Ein Erkennungsweg, der nur die
   * Stundenebene prueft, meldete eine Abweichung nur fuer eine von dreien.
   *
   * <p>Der Lauf geht ueber das kleine dichte Fenster und nicht ueber den Gesamtbestand: Die
   * Erkennung haengt am Bestandsanfang und nicht am Fenster, und ein echter Volllauf kostete auf
   * der Testkopie eine Dreiviertelminute.
   */
  @Test
  @DisplayName(
      "Punkt 54: Der Volllauf meldet Eimer unterhalb des Bestandsanfangs und laesst sie stehen")
  void eingefrorene_eimer_werden_gemeldet_und_bleiben_stehen() {
    legeEingefroreneEimerAn();

    EingefroreneEimer vorher = job.eingefroreneEimer();
    assertThat(vorher.vorhanden())
        .as("Die Vorbedingung: Der Fall ist hergestellt — sonst prueft der Rest nichts")
        .isTrue();
    assertThat(vorher.stundeneimer()).isPositive();
    assertThat(vorher.tageseimer()).isPositive();
    assertThat(vorher.monatseimer()).isPositive();

    List<ILoggingEvent> mitschrift = new ArrayList<>();
    RollupErgebnis ergebnis =
        mitMitschrift(mitschrift, () -> lauf(DICHT_VON, DICHT_BIS, LaufArt.VOLL));

    assertThat(ergebnis.zeilenGeschrieben())
        .as("Der Lauf selbst ist unberuehrt — die Erkennung ist eine Diagnose und kein Zweig")
        .isPositive();

    assertThat(warnungen(mitschrift))
        .as(
            "Der Schaden aus Punkt 54 ist nicht, dass die Zeilen dastehen, sondern dass es niemand"
                + " merkt. Genau diese Zeile behebt das.")
        .anySatisfy(
            zeile ->
                assertThat(zeile)
                    .contains("unterhalb des")
                    .contains("Bestandsanfangs")
                    .contains("Stunden-")
                    .contains("Tages-")
                    .contains("Monatseimer"));

    assertThat(
            monitorDsl.fetchCount(
                MESSAGE_ROLLUP, MESSAGE_ROLLUP.MESSAGE_STATUS.eq(ERFUNDENER_STATUS)))
        .as(
            "Und sie bleibt stehen: Der Rollup ist die einzige Stelle, an der diese Zahl noch steht")
        .isEqualTo(1);
    assertThat(
            monitorDsl.fetchCount(
                MESSAGE_ROLLUP_TAG, MESSAGE_ROLLUP_TAG.MESSAGE_STATUS.eq(ERFUNDENER_STATUS)))
        .as("Die Tagesebene friert mit ein und wird deshalb mitgezaehlt (Punkt 68)")
        .isEqualTo(1);
    assertThat(
            monitorDsl.fetchCount(
                MESSAGE_ROLLUP_MONAT, MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS.eq(ERFUNDENER_STATUS)))
        .as("Und die Monatsebene ebenso")
        .isEqualTo(1);
  }

  /**
   * <b>Die Gegenprobe, ohne die der Test darueber nichts bewiese.</b> Ohne eine eingefrorene Zeile
   * darf keine Warnung erscheinen — sonst zeigte die Meldung oben nur, dass der Lauf immer warnt.
   *
   * <p>Zugleich der Nachweis, dass der abgerundete Vergleich noetig ist: {@code
   * MIN(Message.MessageLastUpdate)} ist {@code 2024-10-01T02:00:28}, {@code MIN(stunde)} ist {@code
   * 2024-10-01T02:00}. Ohne das Abrunden laege die Stundenebene <i>immer</i> davor, und jeder
   * einzelne Lauf meldete einen Fehlbefund.
   */
  @Test
  @DisplayName("Punkt 54: Ohne eingefrorene Eimer meldet der Volllauf nichts")
  void ohne_eingefrorene_eimer_keine_warnung() {
    assertThat(job.eingefroreneEimer())
        .as(
            "Der Bestandsanfang der Testkopie liegt in derselben Stunde wie MIN(stunde) —"
                + " abgerundet verglichen ist das kein Befund")
        .isEqualTo(EingefroreneEimer.KEINE);

    List<ILoggingEvent> mitschrift = new ArrayList<>();
    mitMitschrift(mitschrift, () -> lauf(DICHT_VON, DICHT_BIS, LaufArt.VOLL));

    assertThat(warnungen(mitschrift))
        .as("Eine Warnung, die immer kommt, wird nach dem zweiten Tag nicht mehr gelesen")
        .isEmpty();
  }

  /**
   * <b>Die Summenprobe gilt ab jetzt ueber den ueberlappenden Bereich</b> — nicht ueber die ganze
   * Tabelle <i>(31.08.2026)</i>.
   *
   * <p>Ohne diese Eingrenzung waere die schaerfste Kontrolle dieses Baus nach dem ersten
   * produktiven Loeschlauf <b>dauerhaft rot</b>, und ein dauerhaft roter Test wird abgeschaltet.
   * Sie ist deshalb keine Abschwaechung, sondern der Preis dafuer, dass die Probe ueberhaupt
   * bestehen bleibt.
   *
   * <p><b>Der Test nennt keine Zahl aus dem Bestand</b> (Regel T2). Er misst beide Summen vor und
   * nach dem Anlegen und prueft nur die <i>Differenz</i>: Ueber den ueberlappenden Bereich aendert
   * sich nichts, ueber die ganze Tabelle genau die angelegte Anzahl. Damit haengt er an keinem
   * Pflegestand und an keinem Rollup-Stand.
   */
  @Test
  @DisplayName(
      "Punkt 54: Die Summenprobe gilt ueber den ueberlappenden Bereich, nicht ueber die ganze Tabelle")
  void summenprobe_gilt_ueber_den_ueberlappenden_bereich() {
    LocalDateTime bestandsanfang =
        leseRepository
            .fruehesteAenderung()
            .orElseThrow(
                () -> new AssertionError("Die Quelle ist leer — dann prueft der Test nichts"));
    RollupFenster ganzeTabelle =
        new RollupFenster(VOR_DEM_BESTAND, LocalDateTime.parse("2099-01-01T00:00"));

    RollupFenster ueberlappungVorher =
        schreibRepository
            .ueberlappenderBereich(bestandsanfang)
            .orElseThrow(() -> new AssertionError("Ohne Ueberlappung prueft der Test nichts"));
    long imUeberlappVorher = schreibRepository.summiereAnzahl(ueberlappungVorher);
    long imGanzenVorher = schreibRepository.summiereAnzahl(ganzeTabelle);
    assertThat(imUeberlappVorher)
        .as("Der ueberlappende Bereich traegt Verkehr — sonst waere jede Differenz unten null")
        .isPositive();

    legeEingefroreneEimerAn();

    RollupFenster ueberlappungNachher =
        schreibRepository
            .ueberlappenderBereich(bestandsanfang)
            .orElseThrow(() -> new AssertionError("Ohne Ueberlappung prueft der Test nichts"));
    assertThat(ueberlappungNachher.von())
        .as(
            "Der Bereich beginnt am Bestandsanfang und nicht am Anfang der Tabelle — genau deshalb"
                + " zaehlt die eingefrorene Zeile nicht mit")
        .isEqualTo(bestandsanfang.truncatedTo(ChronoUnit.HOURS));

    assertThat(schreibRepository.summiereAnzahl(ueberlappungNachher))
        .as("Ueber dem Bestandsanfang ist die Probe unveraendert scharf")
        .isEqualTo(imUeberlappVorher);
    assertThat(schreibRepository.summiereAnzahl(ganzeTabelle))
        .as(
            "Und ueber die ganze Tabelle laeuft sie auseinander — das ist der Zustand aus Punkt 54"
                + " und kein Fehler")
        .isEqualTo(imGanzenVorher + EINGEFRORENE_ANZAHL);
  }
}
