package de.kraftwerkone.overlord.monitor;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.AUDIT_LOG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.config.DatabaseProperties;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Datenbankgebundene Nachweise fuer Schritt 2. {@code @Tag("db")}: in der CI ohne Datenbankzugriff
 * ausgeschlossen. Laeuft gegen die Testkopie.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class DatenzugriffDbIT {

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired
  @Qualifier("glassfishDataSource") private DataSource glassfishDataSource;

  @Autowired private MessageStatusClassifier classifier;
  @Autowired private DatabaseProperties props;

  @Test
  @DisplayName("Rauchtest: liest echte Mandanten und Projekte aus GlassfishDB")
  void rauchtest_liest_mandanten_und_projekte() {
    assertThat(glassfishDsl.fetchCount(MANDANT)).isGreaterThan(0);
    assertThat(glassfishDsl.fetchCount(PROJECT)).isGreaterThan(0);
  }

  @Test
  @DisplayName("Schemauebergreifender Join laeuft in einem einzigen Statement durch")
  void schemauebergreifender_join_in_einem_statement() {
    // GlassfishDB.Process gegen overlord_monitor.audit_log — ueber den Lese-Kontext, eine
    // Verbindung. Das Ergebnis ist erwartungsgemaess leer; geprueft wird der Mechanismus und die
    // Vertraeglichkeit der Sortierungen beider Schemata. Daran haengt ab Schritt 9 die
    // Partnerzuordnung.
    var ergebnis =
        glassfishDsl
            .select(PROCESS.PROCESSID, AUDIT_LOG.ID)
            .from(PROCESS)
            .join(AUDIT_LOG)
            .on(AUDIT_LOG.TARGET_ID.eq(PROCESS.PROCESSID))
            .limit(1)
            .fetch();
    assertThat(ergebnis).isEmpty();
  }

  @Test
  @DisplayName("Statuskatalog: DISTINCT MessageStatus enthaelt keine unbekannten Werte")
  void statuskatalog_entspricht_dokumentierter_menge() {
    Set<String> vorhanden =
        new HashSet<>(
            glassfishDsl
                .selectDistinct(MESSAGE.MESSAGESTATUS)
                .from(MESSAGE)
                .fetch(MESSAGE.MESSAGESTATUS));
    vorhanden.remove(null);

    Set<String> bekannt = classifier.bekannteStatuswerte();
    // Die eigentliche Sicherung: kein unbekannter Wert taucht auf.
    assertThat(bekannt)
        .as("Neuer, undokumentierter MessageStatus in der Testkopie: %s", vorhanden)
        .containsAll(vorhanden);
    // Und genau die 12 vorhandenen Werte = alle bekannten ausser RUNNING (fluechtig, in der
    // Testkopie null Mal). Weicht das ab, gehoert docs/message-status.md geprueft.
    Set<String> erwartet = new HashSet<>(bekannt);
    erwartet.remove("RUNNING");
    assertThat(vorhanden).isEqualTo(erwartet);
  }

  @Test
  @DisplayName("Zeitzonen-Rundlauf: der Treiber konvertiert Zeitstempel nicht")
  void zeitzonen_rundlauf() {
    Record2<LocalDateTime, String> zeile =
        glassfishDsl
            .select(
                MESSAGE.MESSAGELASTUPDATE,
                DSL.field("date_format(MessageLastUpdate, '%Y-%m-%d %H:%i:%s')", String.class))
            .from(MESSAGE)
            .where(MESSAGE.MESSAGELASTUPDATE.isNotNull())
            .limit(1)
            .fetchOne();

    assertThat(zeile).isNotNull();
    String alsTimestamp = zeile.value1().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    assertThat(alsTimestamp)
        .as("Weicht der Wert ab, konvertiert der Treiber und die JDBC-URL braucht eine Zeitzone.")
        .isEqualTo(zeile.value2());
  }

  @Test
  @DisplayName("Schemaabgleich: alle generierten Message-Spalten existieren in der verbundenen DB")
  void schemaabgleich_message() {
    Set<String> generiert =
        Arrays.stream(MESSAGE.fields())
            .map(Field::getName)
            .collect(Collectors.toCollection(HashSet::new));

    Set<String> inDatenbank =
        new HashSet<>(
            glassfishDsl
                .select(DSL.field("COLUMN_NAME", String.class))
                .from("information_schema.COLUMNS")
                .where(DSL.field("TABLE_SCHEMA", String.class).eq(props.glassfishSchema()))
                .and(DSL.field("TABLE_NAME", String.class).eq(MESSAGE.getName()))
                .fetch(DSL.field("COLUMN_NAME", String.class)));

    assertThat(inDatenbank)
        .as("Das generierte Modell kennt Spalten, die in der verbundenen DB fehlen (Drift).")
        .containsAll(generiert);
  }

  @Test
  @DisplayName("Schreibverbot: ein Schreibversuch auf GlassfishDB scheitert (roher JDBC)")
  void schreibverbot_auf_glassfish_scheitert() throws SQLException {
    // Bewusst ueber eine rohe Verbindung aus dem Lese-Pool, NICHT ueber jOOQ — sonst griffe der
    // Listener und die DB-Rechte wuerden nie geprueft. Die Bedingung trifft null Zeilen und
    // MariaDB prueft vor der Ausfuehrung: der Test ist harmlos.
    try (Connection c = glassfishDataSource.getConnection()) {
      c.setAutoCommit(false);
      try (Statement s = c.createStatement()) {
        assertThatThrownBy(
                () ->
                    s.executeUpdate(
                        "UPDATE GlassfishDB.Message SET MessageStatus = MessageStatus WHERE 1 = 0"))
            .isInstanceOfSatisfying(
                SQLException.class,
                // 1290 = server read-only, 1142 = fehlende Schreibrechte. Beides weist den
                // Schreibversuch ab.
                e -> assertThat(e.getErrorCode()).isIn(1290, 1142));
      } finally {
        c.rollback();
      }
    }
  }
}
