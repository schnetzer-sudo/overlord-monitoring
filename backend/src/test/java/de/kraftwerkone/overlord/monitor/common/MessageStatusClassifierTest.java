package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueft die Einordnung der Statuswerte und die eine SQL-Fehlerbedingung — ohne Datenbank. */
class MessageStatusClassifierTest {

  private final MessageStatusClassifier classifier = new MessageStatusClassifier();

  @Test
  @DisplayName("Bekannte Werte werden korrekt eingeordnet")
  void bekannte_werte() {
    assertThat(classifier.einordnung("FINISHED")).isEqualTo(MessageStatusKind.ABGESCHLOSSEN);
    assertThat(classifier.einordnung("EERP_RECEIVED")).isEqualTo(MessageStatusKind.QUITTIERT);
    assertThat(classifier.einordnung("COMMIT_RECEIVED")).isEqualTo(MessageStatusKind.QUITTIERT);
    assertThat(classifier.einordnung("MERGED")).isEqualTo(MessageStatusKind.ZWISCHENSCHRITT);
    assertThat(classifier.einordnung("SPLITTED")).isEqualTo(MessageStatusKind.ZWISCHENSCHRITT);
    assertThat(classifier.einordnung("SUSPENDED")).isEqualTo(MessageStatusKind.WARTEND);
    assertThat(classifier.einordnung("RUNNING")).isEqualTo(MessageStatusKind.LAEUFT);
  }

  @Test
  @DisplayName("COMMIT_REJECTED zaehlt als Fehler, obwohl der Wert kein ERROR_-Praefix hat")
  void commit_rejected_ist_fehler() {
    assertThat(classifier.einordnung("COMMIT_REJECTED")).isEqualTo(MessageStatusKind.FEHLER);
    assertThat(classifier.einordnung("ERROR_DUPLICATE")).isEqualTo(MessageStatusKind.FEHLER);
    assertThat(classifier.einordnung("ERROR_TIMEOUT")).isEqualTo(MessageStatusKind.FEHLER);
  }

  @Test
  @DisplayName("Unbekannte und ungeklaerte Werte werden UNGEKLAERT, niemals geraten")
  void unbekannte_werte_sind_ungeklaert() {
    assertThat(classifier.einordnung("COMMIT_SENT")).isEqualTo(MessageStatusKind.UNGEKLAERT);
    assertThat(classifier.einordnung("CHECKED")).isEqualTo(MessageStatusKind.UNGEKLAERT);
    assertThat(classifier.einordnung("CKECKED")).isEqualTo(MessageStatusKind.UNGEKLAERT);
    assertThat(classifier.einordnung("VOELLIG_UNBEKANNT")).isEqualTo(MessageStatusKind.UNGEKLAERT);
    assertThat(classifier.einordnung(null)).isEqualTo(MessageStatusKind.UNGEKLAERT);
  }

  @Test
  @DisplayName("Die bekannte Menge umfasst genau die 13 dokumentierten Werte")
  void bekannte_menge() {
    assertThat(classifier.bekannteStatuswerte())
        .hasSize(13)
        .contains("FINISHED", "MERGED", "SPLITTED", "EERP_RECEIVED", "COMMIT_RECEIVED", "RUNNING")
        .contains("COMMIT_SENT", "ERROR_DUPLICATE", "SUSPENDED", "CHECKED", "COMMIT_REJECTED")
        .contains("ERROR_TIMEOUT", "CKECKED");
  }

  @Test
  @DisplayName("Die Fehlerbedingung nutzt LIKE ... ESCAPE, nicht LEFT()")
  void fehlerbedingung_ist_like_escape() {
    Field<String> statusFeld = DSL.field(DSL.name("MessageStatus"), String.class);
    String sql =
        DSL.using(SQLDialect.MARIADB).renderInlined(classifier.fehlerBedingung(statusFeld));

    assertThat(sql).containsIgnoringCase("like").containsIgnoringCase("escape");
    assertThat(sql).contains("COMMIT_REJECTED");
    assertThat(sql).contains("ERROR");
    // Die naive Variante ist ausdruecklich unerwuenscht (kann den Index nicht nutzen).
    assertThat(sql).doesNotContainIgnoringCase("left(");
  }
}
