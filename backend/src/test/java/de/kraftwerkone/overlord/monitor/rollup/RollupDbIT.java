package de.kraftwerkone.overlord.monitor.rollup;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.ROLLUP_LAUF;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
}
