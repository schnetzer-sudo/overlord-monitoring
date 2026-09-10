package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>M175 — die zwei Statements auf {@code Service}</b> (Schritt 10d Teil A, Teil 3).
 *
 * <h2>Die Frage</h2>
 *
 * <p><b>Regel 7 aus {@code PROJEKTBESCHREIBUNG.md} §8:</b> „Jede neue Abfrage wird vor dem Merge
 * gegen die Testkopie gemessen ({@code EXPLAIN} plus Laufzeit)." Neu sind zwei: die <b>Lampen</b>
 * ({@code ServiceTimeout > 0}) und die <b>Pruefziele</b> (die verschiedenen, nicht leeren Werte von
 * {@code ServiceDefaultFileStore} mit der aufgeloesten Zeile).
 *
 * <h2>Die Erwartung, und sie steht vor dem Lauf fest</h2>
 *
 * <p><b>Vollzugriff ueber 20 Zeilen in der Groessenordnung der 1,348 ms aus M52</b> — das ist die
 * bei Leistungsregel L9 benannte Ausnahme fuer {@code Service}. Ein Index auf {@code
 * ServiceTimeout} oder {@code ServiceDefaultFileStore} existiert nicht und waere bei dieser Groesse
 * ohne Wirkung; ein Plan mit {@code type = ALL} ist deshalb <b>kein Befund</b>, sondern das
 * Erwartete. <b>Zu melden waere das Gegenteil:</b> eine Laufzeit in der Groessenordnung von
 * Zehntelsekunden, ein {@code filesort} ueber mehr als 20 Zeilen oder ein Plan, der eine andere
 * Tabelle als {@code Service} anfasst.
 *
 * <h2>Sie ist eine Messung und kein Test</h2>
 *
 * <p>Sie sichert nichts ueber Wanduhrzeit zu (Regel T1): Die Zeiten gehen nach {@code System.out}
 * und in keine Zusicherung. Zugesichert ist die <b>Ursache</b> — dass der Plan ausschliesslich
 * {@code Service} liest und keine zweite Tabelle dazukommt. Das ist dieselbe Bauform wie M146.
 *
 * <h2>Regel G1</h2>
 *
 * <p>Ausgegeben werden der Plan, die Laufzeit und die <b>Zahl</b> der gelesenen Zeilen — <b>kein
 * Wert einer Spalte</b>. Das Statement der Pruefziele liest {@code ServiceConnectString}, weil die
 * Anwendung die Adresse braucht; die Messung gibt davon nichts aus und zaehlt nur.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class MessungM175DbIT {

  /** Ein Aufwaermlauf, dann die beste von fuenf — dieselbe Bauform wie M94, M108 und M146. */
  private static final int LAEUFE = 5;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private DienstLeseRepository repository;

  private final List<String> gerendert = new ArrayList<>();

  private static void melde(String schluessel, String wert) {
    System.out.println("M175 | " + schluessel + " | " + wert);
  }

  private static String ms(long nanos) {
    return String.format(Locale.GERMANY, "%.3f", nanos / 1_000_000.0);
  }

  /**
   * Ein Repository, das nur rendert und nichts ausfuehrt — die Quelle fuer den {@code EXPLAIN}.
   *
   * <p>{@code STATIC_STATEMENT} ist noetig, damit im Text Werte statt {@code ?} stehen: Ein {@code
   * EXPLAIN} mit Platzhaltern ist kein gueltiges Statement.
   */
  private DienstLeseRepository attrappe() {
    gerendert.clear();
    MockDataProvider mock =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          DSLContext leer = DSL.using(SQLDialect.MARIADB);
          return new MockResult[] {new MockResult(0, leer.newResult())};
        };
    return new DienstLeseRepository(
        DSL.using(
            new MockConnection(mock),
            SQLDialect.MARIADB,
            new Settings().withStatementType(StatementType.STATIC_STATEMENT)));
  }

  private long besteVonFuenf(Supplier<Integer> aufruf) {
    aufruf.get();
    long beste = Long.MAX_VALUE;
    for (int i = 0; i < LAEUFE; i++) {
      long vorher = System.nanoTime();
      aufruf.get();
      beste = Math.min(beste, System.nanoTime() - vorher);
    }
    return beste;
  }

  /**
   * Der Plan des gerenderten Statements — und die <b>Zusicherung</b>, dass nur {@code Service}
   * gelesen wird.
   *
   * <p>Das ist keine Zeitmessung und nach Regel T1 deshalb zusicherbar: Geprueft wird die Ursache.
   * Ein Plan, der eine zweite Tabelle nennt, waere der Befund — die Ausnahme von Regel M2 beruht
   * darauf, dass <i>diese eine</i> Tabelle keinen Mandanten kennt.
   */
  private void planVon(String bezeichnung, String sql) {
    List<String> zeilen = new ArrayList<>();
    List<String> tabellen = new ArrayList<>();
    for (Record satz : glassfishDsl.fetch("explain " + sql)) {
      zeilen.add(
          satz.get("table")
              + " | "
              + satz.get("type")
              + " | key="
              + satz.get("key")
              + " | rows="
              + satz.get("rows")
              + " | "
              + satz.get("Extra"));
      tabellen.add(String.valueOf(satz.get("table")));
    }
    assertThat(zeilen).as("Ein leerer Plan waere kein Plan").isNotEmpty();
    for (String zeile : zeilen) {
      melde("plan." + bezeichnung, zeile);
    }
    assertThat(tabellen)
        .as(
            "Der Plan von %s nennt eine andere Tabelle als Service. Die dritte benannte Ausnahme von"
                + " Regel M2 beruht darauf, dass Service keinen Mandanten kennt — eine zweite"
                + " Quelltabelle hebt die Begruendung auf",
            bezeichnung)
        .allSatisfy(
            tabelle ->
                assertThat(tabelle).isIn("Service", "ablagenziel", "GlassfishDB.Service", "null"));
  }

  @Test
  @DisplayName("M175 — Lampen und Pruefziele: Plan, Laufzeit und gelesene Zeilen")
  void plan_und_laufzeit() {
    // ─── (a) Die gerenderten Statements ─────────────────────────────────────────────────
    DienstLeseRepository attrappe = attrappe();
    attrappe.dienste();
    String dienste = gerendert.getFirst();
    attrappe.pruefziele();
    String pruefziele = gerendert.getLast();

    melde("sql.dienste", dienste.replaceAll("\\s+", " ").trim());
    melde("sql.pruefziele", pruefziele.replaceAll("\\s+", " ").trim());

    // ─── (b) Die Plaene ─────────────────────────────────────────────────────────────────
    planVon("dienste", dienste);
    planVon("pruefziele", pruefziele);

    // ─── (c) Die Laufzeiten, je beste von fuenf ─────────────────────────────────────────
    long zeitDienste = besteVonFuenf(() -> repository.dienste().size());
    long zeitZiele = besteVonFuenf(() -> repository.pruefziele().size());

    melde("laufzeit.dienste", ms(zeitDienste) + " ms (beste von " + LAEUFE + ")");
    melde("laufzeit.pruefziele", ms(zeitZiele) + " ms (beste von " + LAEUFE + ")");

    // ─── (d) Der Umfang: Zahlen, keine Werte (G1) ───────────────────────────────────────
    int lampen = repository.dienste().size();
    int ziele = repository.pruefziele().size();
    melde("zeilen.dienste", String.valueOf(lampen));
    melde("zeilen.pruefziele", String.valueOf(ziele));

    assertThat(lampen).as("Ohne eine einzige Lampe maesse dieser Lauf nichts").isPositive();
  }
}
