package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Ein Plantest, keine Zeitmessung.</b> Er liest {@code EXPLAIN} und prueft Treibertabelle und
 * Index — nicht die Laufzeit.
 *
 * <h2>Warum kein Zeitvergleich</h2>
 *
 * <p>{@code BamIsolationDbIT} misst Wanduhrzeit gegen eine Faktor-10-Schranke und wird gelegentlich
 * grundlos rot (279 ms gegen 20 ms, mit hoher Wahrscheinlichkeit der erste Aufruf einer JVM).
 * <b>Ein Sicherheitstest, der zufaellig rot wird, wird nach der dritten Wiederholung nicht mehr
 * gelesen.</b> Ein Plantest ist deterministisch und prueft die Ursache statt ihres Schattens.
 *
 * <h2>Was er festhaelt und was das <i>nicht</i> heisst</h2>
 *
 * <p><b>Er hielt bis zum 03.09.2026 fest, dass der Parameter {@code ueberfaellig} die Abfrage auf
 * {@code MessageStatusIDX} zieht und die Sortierung zum {@code filesort} macht</b> — nicht, weil
 * das gut waere, sondern damit eine Aenderung daran auffaellt (offener Punkt 56, {@code
 * docs/nachrichtenliste.md} §5b). Der Parameter ist mit E-71 entfallen, und mit ihm die zweite
 * Abfrageform.
 *
 * <p><b>Was bleibt, ist die Gegenprobe — und sie ist jetzt die Zusicherung selbst:</b> Die Liste
 * laeuft <i>nicht</i> ueber {@code MessageStatusIDX}. Sie hat wieder <b>genau einen</b>
 * Zugriffspfad, und damit gilt die Cursor-Messung aus M4/L8 wieder fuer jede Fassung. Wird der Test
 * rot, steht die zweite Form wieder da, ohne dass jemand sie gebaut haette.
 *
 * <h2>Beide Mandanten (Regel L7)</h2>
 *
 * <p>{@code NEXANS} und {@code SUTTONS} — dieselbe Paarung wie in M97. Sie ist hier <b>nicht</b>
 * Zierde: Ohne den Parameter laufen die beiden ueber <b>verschiedene</b> Plaene ({@code NEXANS}
 * ueber den Zeitindex, {@code SUTTONS} ueber die Mandantenkette, §5a), mit ihm ueber denselben. Ein
 * Test an nur einem Mandanten uebersaehe genau das.
 *
 * <h2>Wie das Statement hierher kommt</h2>
 *
 * <p>Es wird nicht abgeschrieben, sondern <b>vom Repository gerendert</b> — gegen eine
 * jOOQ-Attrappe, wie in {@code NachrichtenStatementsTest}. Nur so prueft der Test den Plan
 * <b>der</b> Abfrage und nicht den einer aehnlichen (Regel L7). {@link
 * StatementType#STATIC_STATEMENT} setzt die Werte als Literale ein; ein {@code EXPLAIN} ueber
 * Fragezeichen ergaebe keine Bereichsanalyse.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class NachrichtenPlanDbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev} — derselbe Stichtag wie in M97. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  /** Das 30-Tage-Fenster, gegen den Anker aufgeloest — Zeichen fuer Zeichen das aus M97. */
  private static final Zeitfenster FENSTER = new Zeitfenster(ANKER.minusDays(30), ANKER);

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private final List<String> gerendert = new ArrayList<>();
  private NachrichtenRepository attrappe;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider mock =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    attrappe =
        new NachrichtenRepository(
            DSL.using(
                new MockConnection(mock),
                SQLDialect.MARIADB,
                new Settings().withStatementType(StatementType.STATIC_STATEMENT)),
            new MessageStatusClassifier());
  }

  /** Der Plan des Statements, das das Repository fuer diese Abfrage schickt. */
  private List<Plan> plan(String mandantId, Seitenposition cursor) {
    gerendert.clear();
    attrappe.finde(
        new MandantContext(mandantId),
        new Nachrichtenabfrage(FENSTER, Set.of(), List.of(), null, true, cursor, 50));
    assertThat(gerendert).hasSize(1);

    List<Plan> zeilen = new ArrayList<>();
    for (Record satz : glassfishDsl.fetch("explain " + gerendert.getFirst())) {
      zeilen.add(
          new Plan(
              String.valueOf(satz.get("table")),
              String.valueOf(satz.get("type")),
              String.valueOf(satz.get("key")),
              String.valueOf(satz.get("Extra")).toLowerCase(Locale.ROOT)));
    }
    assertThat(zeilen).isNotEmpty();
    return zeilen;
  }

  /** Die Zeile, mit der der Optimierer einsteigt — die Treibertabelle. */
  private Plan treiber(String mandantId, Seitenposition cursor) {
    return plan(mandantId, cursor).getFirst();
  }

  private record Plan(String tabelle, String zugriff, String index, String extra) {}

  /*
   * Hier standen bis zum 03.09.2026 zwei Faelle zum Parameter `ueberfaellig`: dass er die Abfrage
   * auf MessageStatusIDX zieht (Befund aus M97) und dass der Cursor daran nichts aendert. Beide
   * sind mit E-71 entfallen -- der Parameter existiert nicht mehr.
   *
   * WAS SIE FESTHIELTEN, IST DAMIT NICHT WERTLOS GEWORDEN, SONDERN GEGENSTANDSLOS: Es gibt keine
   * zweite Abfrageform mehr, deren Plan man gegen die erste halten koennte. Der Fall darunter --
   * frueher die Gegenprobe -- traegt seither die ganze Zusicherung.
   */

  /**
   * Die Gegenprobe, ohne die der Test nur zeigte, dass irgendein Plan herauskommt: <b>Ohne</b> den
   * Parameter laeuft dieselbe Abfrage <b>nicht</b> ueber {@code MessageStatusIDX}. Der Parameter
   * ist damit nachweislich die Ursache und nicht ein zufaelliger Begleiter.
   *
   * <p><b>Seit dem 03.09.2026 ist er nicht mehr die Gegenprobe, sondern die Zusicherung.</b> Es
   * gibt keinen Parameter mehr, gegen den er gegenprueft; er haelt jetzt fest, dass die Liste
   * <b>einen</b> Zugriffspfad hat und der Statusindex in keinem Plan steht.
   *
   * <p><b>Auf welchem Plan die Referenzliste laeuft, prueft dieser Test bewusst nicht.</b> Er
   * haengt am Mandanten — {@code NEXANS} ueber {@code MessageLastUpdateIDX}, {@code SUTTONS} ueber
   * die Mandantenkette (§5a) — und diese Wahl ist heute ein Zufallstreffer der Statistik (offener
   * Punkt 65). Einen Zufall festzuschreiben hiesse, den Test bei der ersten Statistikaenderung rot
   * zu machen, ohne dass jemand etwas falsch gemacht haette.
   */
  @Test
  @DisplayName("Die Liste laeuft nicht ueber MessageStatusIDX — sie hat einen Zugriffspfad")
  void liste_laeuft_nicht_ueber_den_statusindex() {
    for (String mandant : List.of("NEXANS", "SUTTONS")) {
      assertThat(plan(mandant, null))
          .as("Der Statusindex darf in keiner Planzeile der Liste stehen (%s)", mandant)
          .noneMatch(zeile -> "MessageStatusIDX".equals(zeile.index()));
    }
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Die Fensterverengung (30.08.2026, docs/nachrichtenliste.md §5d)
  // ───────────────────────────────────────────────────────────────────────────

  /** Der Plan derselben Listenabfrage ueber ein <b>verengtes</b> Fenster. */
  private List<Plan> planVerengt(String mandantId, LocalDateTime von) {
    gerendert.clear();
    attrappe.finde(
        new MandantContext(mandantId),
        new Nachrichtenabfrage(
            new Zeitfenster(von, ANKER), Set.of(), List.of(), null, true, null, 50));
    assertThat(gerendert).hasSize(1);

    List<Plan> zeilen = new ArrayList<>();
    for (Record satz : glassfishDsl.fetch("explain " + gerendert.getFirst())) {
      zeilen.add(
          new Plan(
              String.valueOf(satz.get("table")),
              String.valueOf(satz.get("type")),
              String.valueOf(satz.get("key")),
              String.valueOf(satz.get("Extra")).toLowerCase(Locale.ROOT)));
    }
    return zeilen;
  }

  /**
   * <b>Der Kern von §5c, als Test.</b> {@code SUTTONS} laeuft ueber dreissig Tage ueber die
   * <b>Mandantenkette</b> — der Plan, der ihn 1.043 ms kostet. Ueber ein Fenster von zwei Stunden
   * waehlt derselbe Optimierer <b>von selbst</b> den Zeitindex und braucht 6,5 ms.
   *
   * <p>Das ist die ganze Behauptung des Baus, und sie steht und faellt mit dieser einen Zeile:
   * <b>Ein Fenster zu geben ist etwas anderes, als eine Form zu erzwingen.</b> Kein Hint, kein
   * {@code STRAIGHT_JOIN}, kein Index auf {@code GlassfishDB} — nur ein engeres {@code von}.
   *
   * <p>Wird er rot, ist das kein Fehler, sondern ein Befund: Dann waehlt der Optimierer bei engem
   * Fenster anders als am 27.08.2026, und die Verengung bringt nicht mehr, was sie bringen soll.
   * Die <b>Richtigkeit</b> haengt nicht daran ({@code FensterverengungDbIT}) — nur der Nutzen.
   */
  @Test
  @DisplayName("Bei engem Fenster wechselt SUTTONS von der Mandantenkette auf den Zeitindex")
  void verengtes_fenster_zieht_suttons_auf_den_zeitindex() {
    Plan weit = plan("SUTTONS", null).getFirst();
    Plan eng = planVerengt("SUTTONS", ANKER.truncatedTo(ChronoUnit.HOURS).minusHours(1)).getFirst();

    assertThat(weit.tabelle())
        .as("Ueber dreissig Tage steigt SUTTONS ueber die Mandantenkette ein — der teure Plan")
        .isEqualTo("ProjectMandant");
    assertThat(eng.tabelle())
        .as("Ueber zwei Stunden steigt derselbe Mandant ueber Message ein")
        .isEqualTo("Message");
    assertThat(eng.index()).isEqualTo("MessageLastUpdateIDX");
    assertThat(eng.zugriff()).isEqualTo("range");
  }

  /**
   * Und die Gegenprobe: {@code NEXANS} laeuft schon ueber dreissig Tage ueber den Zeitindex. Ein
   * enges Fenster <b>aendert seine Planfamilie nicht</b> — es macht nur den Bereich kleiner. Ohne
   * diesen Test bewiese der vorige nur, dass irgendein Plan herauskommt.
   */
  @Test
  @DisplayName("NEXANS behaelt seine Planfamilie — das enge Fenster macht nur den Bereich kleiner")
  void verengtes_fenster_aendert_nexans_planfamilie_nicht() {
    Plan weit = plan("NEXANS", null).getFirst();
    Plan eng = planVerengt("NEXANS", ANKER.truncatedTo(ChronoUnit.HOURS).minusHours(1)).getFirst();

    assertThat(weit.tabelle()).isEqualTo("Message");
    assertThat(eng.tabelle()).isEqualTo("Message");
    assertThat(eng.index()).isEqualTo(weit.index()).isEqualTo("MessageLastUpdateIDX");
  }

  /**
   * Der Plan der <b>Vorabfrage</b> selbst (Regel L7: gemessen wird die Abfrage, die der Code
   * schickt). Sie muss ueber {@code message_rollup} einsteigen — und dort ueber einen der beiden
   * Schluessel, die es gibt: den Primaerschluessel ueber {@code stunde} oder den Index {@code
   * message_rollup_prozess_idx} aus {@code V11}. Steht dort {@code ALL}, liest sie die ganze
   * Tabelle, und der ganze Bau waere teurer als das, was er spart.
   */
  @Test
  @DisplayName("Die Vorabfrage steigt ueber message_rollup ein, nicht ueber einen vollen Durchlauf")
  void vorabfrage_steigt_ueber_den_rollup_ein() {
    VerengungRepository verengungAttrappe =
        new VerengungRepository(
            DSL.using(
                new MockConnection(
                    ausfuehrung -> {
                      gerendert.add(ausfuehrung.sql());
                      DSLContext leer = DSL.using(SQLDialect.MARIADB);
                      return new MockResult[] {new MockResult(0, leer.newResult())};
                    }),
                SQLDialect.MARIADB,
                new Settings().withStatementType(StatementType.STATIC_STATEMENT)),
            new MessageStatusClassifier());

    Nachrichtenabfrage abfrage =
        new Nachrichtenabfrage(FENSTER, Set.of(), List.of(), null, true, null, 50);
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(abfrage, LocalDateTime.parse("2026-08-27T15:00:00"));
    assertThat(grenzen).isNotNull();

    gerendert.clear();
    verengungAttrappe.frageStufe(
        new MandantContext("SUTTONS"), abfrage, grenzen, grenzen.hAllVon(), 51);
    assertThat(gerendert).hasSize(1);

    List<Plan> zeilen = new ArrayList<>();
    for (Record satz : glassfishDsl.fetch("explain " + gerendert.getFirst())) {
      zeilen.add(
          new Plan(
              String.valueOf(satz.get("table")),
              String.valueOf(satz.get("type")),
              String.valueOf(satz.get("key")),
              String.valueOf(satz.get("Extra")).toLowerCase(Locale.ROOT)));
    }

    assertThat(zeilen)
        .as("message_rollup muss im Plan vorkommen")
        .anyMatch(zeile -> "message_rollup".equals(zeile.tabelle()));
    assertThat(zeilen)
        .filteredOn(zeile -> "message_rollup".equals(zeile.tabelle()))
        .allSatisfy(
            zeile -> {
              assertThat(zeile.zugriff())
                  .as("Kein voller Durchlauf ueber die Rolluptabelle")
                  .isNotEqualTo("ALL");
              assertThat(zeile.index())
                  .as("Einstieg ueber den Primaerschluessel oder den Index aus V11")
                  .isIn("PRIMARY", "message_rollup_prozess_idx");
            });
  }
}
