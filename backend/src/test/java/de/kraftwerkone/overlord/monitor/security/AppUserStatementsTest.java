package de.kraftwerkone.overlord.monitor.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Statements der Benutzerverwaltung, <b>gerendert statt nachgebildet</b>: Das Repository laeuft
 * gegen eine jOOQ-Attrappe, und geprueft wird der Text, der wirklich herausfaellt. Vorbild ist
 * {@code ProzessKatalogStatementsTest}; die Bauform ist dieselbe.
 *
 * <p><b>Wozu er neben den Integrationstests steht.</b> Er beantwortet die eine Frage, die kein
 * Integrationstest gegen die Testkopie beantworten kann: <i>Traegt {@code
 * existiertAndererNutzbarerAdmin} wirklich die Bedingung „aktiv und nicht administrativ
 * gesperrt"?</i> Die Testkopie enthaelt die echten Administratorkonten des Betreibers; der Zustand
 * „genau ein nutzbarer ADMIN" liesse sich dort nur herstellen, indem ein Test sie sperrt — ein
 * Eingriff in eine gemeinsam genutzte Umgebung, den kein Testlauf wert ist. Ein Einheitstest mit
 * gestelltem Repository wiederum prueft nur, <b>dass</b> gefragt wird, nicht <b>was</b>.
 *
 * <p>Der gerenderte Text schliesst die Luecke: Faellt die Bedingung aus dem Statement — beim
 * naechsten Umbau, lautlos —, wird dieser Test rot, und E12 verliert nicht still seine zweite
 * Stufe.
 *
 * <p><b>{@code StatementType.STATIC_STATEMENT}</b>, weil jOOQ Werte sonst <i>bindet</i> statt sie
 * einzusetzen: {@code 'ADMIN'} taucht im gerenderten Text gar nicht auf, und eine Textpruefung
 * darauf ginge still ins Leere.
 */
class AppUserStatementsTest {

  private final List<String> gerendert = new ArrayList<>();
  private AppUserRepository repository;

  @BeforeEach
  void attrappeAufbauen() {
    gerendert.clear();
    MockDataProvider attrappe =
        ausfuehrung -> {
          gerendert.add(ausfuehrung.sql());
          // Eine Existenzabfrage MUSS eine Zeile bekommen: fetchExists liest den Wert aus, und ein
          // leeres Ergebnis liefe in einen NullPointer, bevor der gerenderte Text geprueft waere.
          // Alles andere darf leer bleiben — hier zaehlt der Text und nicht das Ergebnis.
          if (ausfuehrung.sql().toLowerCase(Locale.ROOT).startsWith("select exists")) {
            DSLContext k = DSL.using(SQLDialect.MARIADB);
            Field<Integer> vorhanden = DSL.field("exists", Integer.class);
            Result<Record1<Integer>> eine = k.newResult(vorhanden);
            eine.add(k.newRecord(vorhanden).value1(1));
            return new MockResult[] {new MockResult(1, eine)};
          }
          return new MockResult[] {new MockResult(0, DSL.using(SQLDialect.MARIADB).newResult())};
        };
    DSLContext attrappenKontext =
        DSL.using(
            new MockConnection(attrappe),
            SQLDialect.MARIADB,
            new Settings().withStatementType(StatementType.STATIC_STATEMENT));
    repository = new AppUserRepository(attrappenKontext);
  }

  private String einziges() {
    assertThat(gerendert).hasSize(1);
    return gerendert.getFirst().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  @Test
  @DisplayName("Die Frage nach dem zweiten ADMIN traegt aktiv UND nicht gesperrt im Statement")
  void nutzbarer_admin_traegt_beide_bedingungen() {
    repository.existiertAndererNutzbarerAdmin(42L);

    String sql = einziges();
    assertThat(sql)
        .as("Ohne die Rolle fragte die Bedingung nach irgendeinem Konto")
        .contains("`role` = 'admin'");
    assertThat(sql)
        .as("Ohne enabled zaehlte ein deaktivierter Admin als nutzbar")
        .contains("`enabled` = true");
    assertThat(sql)
        .as(
            "Ohne locked_by_admin waere der zweite Admin ein Feigenblatt — genau der Satz, an dem"
                + " E12 haengt")
        .contains("`locked_by_admin` = false");
    assertThat(sql)
        .as("Ohne den Ausschluss faende das Konto sich selbst und der Selbstschutz griffe nie")
        .contains("`id` <> 42");
  }

  @Test
  @DisplayName("Die Kontenliste holt die letzte Anmeldung im selben Statement aus dem audit_log")
  void kontenliste_aggregiert_im_statement() {
    repository.findeAlleKonten();

    // Zwei Statements: die Mandantenzuordnungen und die Liste selbst.
    assertThat(gerendert).hasSize(2);
    String liste = gerendert.get(1).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    assertThat(liste)
        .as("Die Aggregation steht als abgeleitete Tabelle im Statement (E17), nicht in Java")
        .contains("max(`overlord_monitor`.`audit_log`.`occurred_at`)")
        .contains("'anmeldung_erfolg'")
        .contains("group by `overlord_monitor`.`audit_log`.`actor_user_id`");
    assertThat(liste)
        .as("Gruppiert wird ueber die Kennung, nicht ueber den Benutzernamen")
        .doesNotContain("group by `overlord_monitor`.`audit_log`.`actor_username`");
    assertThat(liste)
        .as("Ein linker Verbund — wer sich nie angemeldet hat, faellt nicht aus der Liste")
        .contains("left outer join");
  }

  /**
   * Der Befund einer Nachpruefung vom 21.08.2026: Bis dahin zog auch die Einzelabfrage die volle
   * Aggregation ueber den gesamten Protokollbestand und warf sie bis auf ein Konto wieder weg —
   * zweimal je Schreibvorgang, gemessene 17,9 ms je Lauf (M82).
   */
  @Test
  @DisplayName("Die Einzelabfrage schraenkt die Aggregation auf das eine Konto ein")
  void einzelabfrage_aggregiert_nicht_ueber_alles() {
    repository.findeKonto(42L);

    // Zwei Statements: die Mandanten dieses einen Kontos und die Zeile selbst.
    assertThat(gerendert).hasSize(2);
    String mandanten = gerendert.get(0).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    String zeile = gerendert.get(1).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

    assertThat(mandanten)
        .as("Nicht der Gesamtbestand aus app_user_mandant, sondern die Zeilen dieses Kontos")
        .contains("`user_id` = 42");
    assertThat(zeile)
        .as(
            "Die Einschraenkung gehoert IN die abgeleitete Tabelle — steht sie nur aussen,"
                + " gruppiert die Datenbank trotzdem ueber das ganze Protokoll")
        .contains("`actor_user_id` = 42");
  }
}
