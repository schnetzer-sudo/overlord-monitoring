package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Der Abbruch an {@code max_statement_time} — und nur der.
 *
 * <p>Die Werte sind nicht geraten: Nachgeprueft gegen die Testkopie am 06.08.2026 meldet MariaDB
 * {@code 1969} mit SQLState {@code 70100}, Connector/J 3.5 macht daraus eine {@link
 * SQLTimeoutException}, und jOOQ verpackt sie in eine {@link DataAccessException} mit genau dieser
 * Ursache. Der Test baut diese Kette nach, statt eine echte Zeitgrenze zu provozieren: Dafuer
 * brauchte es ein Statement, das zehn Sekunden auf der geteilten Testkopie laeuft — je Lauf.
 *
 * <p>Der eigentliche Punkt sind die beiden Gegenproben: Ohne Suchbegriff und bei einer anderen
 * Ursache bleibt der Fehler technisch und wird zu {@code 500}. Sonst verschwaende ein
 * Verbindungsabriss hinter einem freundlichen Hinweis, den Suchbegriff zu schaerfen.
 */
class NachrichtenRepositoryZeitgrenzeTest {

  private static DataAccessException abbruch() {
    return new DataAccessException(
        "SQL [select …]; Query execution was interrupted (max_statement_time exceeded)",
        new SQLTimeoutException(
            "Query execution was interrupted (max_statement_time exceeded)", "70100", 1969));
  }

  @Test
  @DisplayName("Mit Suchbegriff wird der Abbruch zu einem fachlichen Fehler mit eigenem Typ")
  void mit_suchbegriff_ist_der_abbruch_fachlich() {
    RuntimeException uebersetzt = NachrichtenRepository.anDerZeitgrenze(abbruch(), true);

    assertThat(uebersetzt).isInstanceOf(FachlicheAusnahme.class);
    FachlicheAusnahme fachlich = (FachlicheAusnahme) uebersetzt;
    assertThat(fachlich.problemTyp()).isEqualTo("suche-abgebrochen");
    assertThat(fachlich.status().value()).isEqualTo(400);
    assertThat(fachlich.detail())
        .as("Der Text sagt, was zu tun ist")
        .contains("Zeitraum")
        .contains("Suchbegriff");
    assertThat(fachlich.detail())
        .as("Und nennt weder Tabelle noch Zeitgrenze noch SQL")
        .doesNotContain("Message")
        .doesNotContain("max_statement_time")
        .doesNotContain("select");
  }

  @Test
  @DisplayName("Ohne Suchbegriff bleibt der Abbruch ein technischer Fehler")
  void ohne_suchbegriff_bleibt_er_technisch() {
    DataAccessException original = abbruch();

    assertThat(NachrichtenRepository.anDerZeitgrenze(original, false)).isSameAs(original);
  }

  @Test
  @DisplayName("Eine andere Ursache wird nicht mit umgedeutet")
  void andere_ursachen_bleiben_technisch() {
    DataAccessException syntax =
        new DataAccessException("SQL [select …]", new SQLSyntaxErrorException("kaputt", "42000"));

    assertThat(NachrichtenRepository.anDerZeitgrenze(syntax, true)).isSameAs(syntax);
  }
}
