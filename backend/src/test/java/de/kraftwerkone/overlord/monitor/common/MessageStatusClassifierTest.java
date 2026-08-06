package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

  /**
   * Die eine Ausnahme von „Unbekanntes ist ungeklaert" — und der Grund dafuer: Waere sie nicht da,
   * faende der Statusfilter {@code FEHLER} ueber {@link MessageStatusClassifier#fehlerBedingung}
   * eine Zeile, die die Liste anschliessend als „Bedeutung nicht verifiziert" beschriftet.
   */
  @Test
  @DisplayName("Ein unbekannter ERROR_-Wert ist ein Fehler — dieselbe Regel wie in SQL")
  void unbekanntes_error_praefix_ist_fehler() {
    assertThat(classifier.einordnung("ERROR_GIBTESNOCHNICHT")).isEqualTo(MessageStatusKind.FEHLER);
    assertThat(classifier.einordnung("ERRORX")).isEqualTo(MessageStatusKind.UNGEKLAERT);
    assertThat(classifier.einordnung("ERROR")).isEqualTo(MessageStatusKind.UNGEKLAERT);
  }

  /**
   * Die Schreibweise darf Java und SQL nicht auseinanderlaufen lassen (Aufgabe 10b, 06.08.2026).
   *
   * <p>Das Quellschema sortiert mit {@code utf8mb4_general_ci}: {@code error_x} faellt dort unter
   * {@link MessageStatusClassifier#fehlerBedingung} und kaeme mit dem Statusfilter {@code FEHLER}
   * zurueck. Ordnete Java denselben Wert als {@code UNGEKLAERT} ein, beschriftete die Liste ihn
   * anschliessend mit „Bedeutung nicht verifiziert" — gefiltert nach Fehler, angezeigt als
   * ungeklaert.
   */
  @Test
  @DisplayName("Die Schreibweise entscheidet nicht ueber die Einordnung")
  void schreibweise_aendert_die_einordnung_nicht() {
    // Das ERROR_-Praefix — der Fall, der Java und SQL auseinandertreibt.
    assertThat(classifier.einordnung("error_x")).isEqualTo(MessageStatusKind.FEHLER);
    assertThat(classifier.einordnung("Error_Duplicate")).isEqualTo(MessageStatusKind.FEHLER);
    // Und der zweite Zweig derselben Bedingung, der ohne Praefix auskommt.
    assertThat(classifier.einordnung("commit_rejected")).isEqualTo(MessageStatusKind.FEHLER);

    // Die uebrigen bekannten Werte ebenso — sonst haetten wir zwei Regeln statt einer.
    assertThat(classifier.einordnung("finished")).isEqualTo(MessageStatusKind.ABGESCHLOSSEN);
    assertThat(classifier.einordnung("Suspended")).isEqualTo(MessageStatusKind.WARTEND);
    assertThat(classifier.istEndstatus("suspended")).isFalse();

    // Was unbekannt ist, bleibt unbekannt — hochgestellt wird verglichen, nicht geraten.
    assertThat(classifier.einordnung("errorx")).isEqualTo(MessageStatusKind.UNGEKLAERT);
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

  // ─── Endstatus (Schritt 4) ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Offen sind allein WARTEND und LAEUFT")
  void offen_sind_nur_wartend_und_laeuft() {
    assertThat(classifier.istEndstatus(MessageStatusKind.WARTEND)).isFalse();
    assertThat(classifier.istEndstatus(MessageStatusKind.LAEUFT)).isFalse();

    assertThat(classifier.istEndstatus(MessageStatusKind.ABGESCHLOSSEN)).isTrue();
    assertThat(classifier.istEndstatus(MessageStatusKind.QUITTIERT)).isTrue();
    assertThat(classifier.istEndstatus(MessageStatusKind.FEHLER)).isTrue();
    assertThat(classifier.istEndstatus(MessageStatusKind.ZWISCHENSCHRITT)).isTrue();
    assertThat(classifier.istEndstatus(MessageStatusKind.UNGEKLAERT)).isTrue();
  }

  /**
   * Die Entscheidung, die 34,38 Prozent aller Zeilen betrifft: {@code SPLITTED} und {@code MERGED}
   * sind fertig. Zaehlten sie als offen, waere jede dritte Zeile ein Kandidat fuer „ueberfaellig".
   */
  @Test
  @DisplayName("SPLITTED und MERGED sind Endstatus, SUSPENDED nicht")
  void zwischenschritte_sind_endstatus() {
    assertThat(classifier.istEndstatus("SPLITTED")).isTrue();
    assertThat(classifier.istEndstatus("MERGED")).isTrue();
    assertThat(classifier.istEndstatus("SUSPENDED")).isFalse();
    assertThat(classifier.istEndstatus("RUNNING")).isFalse();
  }

  @Test
  @DisplayName("UNGEKLAERT bleibt aus den Problemkategorien heraus")
  void ungeklaertes_bleibt_draussen() {
    LocalDateTime lange_her = LocalDateTime.parse("2025-01-01T00:00:00");
    LocalDateTime jetzt = LocalDateTime.parse("2025-12-30T04:09:47");

    for (String status : new String[] {"CHECKED", "CKECKED", "COMMIT_SENT", "VOELLIG_UNBEKANNT"}) {
      assertThat(classifier.istUeberfaellig(status, lange_her, 1800, jetzt))
          .as(
              "%s ist ungeklaert. Es als ueberfaellig zu fuehren waere die Behauptung, wir"
                  + " wuessten, dass die Nachricht nicht fertig ist.",
              status)
          .isFalse();
    }
  }

  // ─── Ueberfaelligkeit (Schritt 4) ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("MessageTimeout ist eine Dauer in Sekunden")
  void timeout_ist_in_sekunden() {
    assertThat(MessageStatusClassifier.TIMEOUT_EINHEIT).isEqualTo(ChronoUnit.SECONDS);

    // 1800 Sekunden sind 30 Minuten — nicht 30 Stunden (Messung M8).
    assertThat(classifier.timeoutZeitpunkt(LocalDateTime.parse("2025-12-30T04:00:00"), 1800))
        .contains(LocalDateTime.parse("2025-12-30T04:30:00"));
  }

  @Test
  @DisplayName("MessageTimeout 0 heisst kein Timeout — solche Zeilen sind nie ueberfaellig")
  void timeout_null_wert_ist_kein_timeout() {
    LocalDateTime lange_her = LocalDateTime.parse("2024-10-01T02:00:28");
    LocalDateTime jetzt = LocalDateTime.parse("2025-12-30T04:09:47");

    assertThat(classifier.timeoutZeitpunkt(lange_her, 0)).isEmpty();
    assertThat(classifier.istUeberfaellig("SUSPENDED", lange_her, 0, jetzt)).isFalse();
  }

  @Test
  @DisplayName("MessageTimeout NULL wird behandelt, obwohl es in der Testkopie nie vorkommt")
  void timeout_null_wird_behandelt() {
    LocalDateTime lange_her = LocalDateTime.parse("2024-10-01T02:00:28");
    LocalDateTime jetzt = LocalDateTime.parse("2025-12-30T04:09:47");

    assertThat(classifier.timeoutZeitpunkt(lange_her, null)).isEmpty();
    assertThat(classifier.istUeberfaellig("SUSPENDED", lange_her, null, jetzt)).isFalse();
    // Auch ein fehlender Zeitstempel darf nicht zu einer Behauptung fuehren.
    assertThat(classifier.timeoutZeitpunkt(null, 1800)).isEmpty();
  }

  @Test
  @DisplayName("Ueberfaellig ist, was offen ist und dessen Frist abgelaufen ist")
  void ueberfaellig_nur_wenn_offen_und_abgelaufen() {
    LocalDateTime jetzt = LocalDateTime.parse("2025-12-30T04:09:47");
    LocalDateTime vorEinerStunde = jetzt.minusHours(1);
    LocalDateTime vorEinerMinute = jetzt.minusMinutes(1);

    // Offen (WARTEND) und die 30-Minuten-Frist ist abgelaufen.
    assertThat(classifier.istUeberfaellig("SUSPENDED", vorEinerStunde, 1800, jetzt)).isTrue();
    // Offen, aber die Frist laeuft noch.
    assertThat(classifier.istUeberfaellig("SUSPENDED", vorEinerMinute, 1800, jetzt)).isFalse();
    // Frist abgelaufen, aber die Zeile ist fertig — kein Problemfall.
    assertThat(classifier.istUeberfaellig("FINISHED", vorEinerStunde, 1800, jetzt)).isFalse();
    assertThat(classifier.istUeberfaellig("MERGED", vorEinerStunde, 1800, jetzt)).isFalse();
    // Ein Fehler ist ein Fehler und keine Ueberfaelligkeit (Regel Q3, drei getrennte Kategorien).
    assertThat(classifier.istUeberfaellig("ERROR_DUPLICATE", vorEinerStunde, 1800, jetzt))
        .isFalse();
  }

  @Test
  @DisplayName("Die Frist laeuft genau ab, nicht vorher")
  void frist_laeuft_genau_ab() {
    LocalDateTime start = LocalDateTime.parse("2025-12-30T04:00:00");
    // 1800 Sekunden spaeter: exakt 04:30:00 — noch nicht ueberfaellig.
    assertThat(classifier.istUeberfaellig("SUSPENDED", start, 1800, start.plusMinutes(30)))
        .isFalse();
    // Eine Sekunde danach schon.
    assertThat(
            classifier.istUeberfaellig(
                "SUSPENDED", start, 1800, start.plusMinutes(30).plusSeconds(1)))
        .isTrue();
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

  // ─── Uebersetzung Einordnung → SQL (Schritt 4) ──────────────────────────────────────────────

  private static String sql(org.jooq.Condition bedingung) {
    return DSL.using(SQLDialect.MARIADB).renderInlined(bedingung);
  }

  private static final Field<String> STATUS = DSL.field(DSL.name("MessageStatus"), String.class);

  @Test
  @DisplayName("Geschlossene Einordnungen werden aufgezaehlt, FEHLER bleibt die eine Bedingung")
  void bedingung_je_einordnung() {
    assertThat(sql(classifier.bedingung(MessageStatusKind.ABGESCHLOSSEN, STATUS)))
        .contains("FINISHED")
        .doesNotContainIgnoringCase("like");
    assertThat(sql(classifier.bedingung(MessageStatusKind.ZWISCHENSCHRITT, STATUS)))
        .contains("MERGED")
        .contains("SPLITTED");
    assertThat(sql(classifier.bedingung(MessageStatusKind.FEHLER, STATUS)))
        .isEqualTo(sql(classifier.fehlerBedingung(STATUS)));
  }

  @Test
  @DisplayName("UNGEKLAERT ist der Rest — samt NULL und samt allem Unbekannten")
  void ungeklaert_ist_der_rest() {
    String sql = sql(classifier.bedingung(MessageStatusKind.UNGEKLAERT, STATUS));

    assertThat(sql).containsIgnoringCase("is null");
    assertThat(sql).containsIgnoringCase("not in");
    assertThat(sql).containsIgnoringCase("not (");
    // Die bekannten ungeklaerten Werte duerfen nicht aufgezaehlt sein — sie fallen unter „Rest".
    assertThat(sql).doesNotContain("COMMIT_SENT").doesNotContain("CHECKED");
    assertThat(sql).contains("FINISHED").contains("SUSPENDED");
  }

  @Test
  @DisplayName("Mehrere Einordnungen werden mit ODER verbunden, keine filtert nicht")
  void mehrere_einordnungen() {
    String sql =
        sql(
            classifier.bedingung(
                java.util.List.of(MessageStatusKind.FEHLER, MessageStatusKind.WARTEND), STATUS));

    assertThat(sql).containsIgnoringCase(" or ").contains("SUSPENDED").contains("COMMIT_REJECTED");
    assertThat(sql(classifier.bedingung(java.util.List.of(), STATUS)))
        .isEqualTo(sql(DSL.noCondition()));
  }

  /**
   * {@code zwischenschritte=false}: Ohne das vorangestellte {@code IS NULL} waere {@code NOT
   * (status IN (…))} fuer eine Zeile ohne Status selbst {@code NULL} — sie fiele aus der Liste,
   * sobald irgendetwas ausgeschlossen wird.
   */
  @Test
  @DisplayName("Der Ausschluss laesst Zeilen ohne Status stehen")
  void ausschluss_behaelt_zeilen_ohne_status() {
    String sql = sql(classifier.ohne(MessageStatusKind.ZWISCHENSCHRITT, STATUS));

    assertThat(sql).containsIgnoringCase("is null");
    assertThat(sql).containsIgnoringCase("not (");
    assertThat(sql).contains("MERGED").contains("SPLITTED");
  }

  @Test
  @DisplayName("Die Rohwerte je Einordnung stammen aus derselben Zuordnung wie die Anzeige")
  void rohwerte_und_einordnung_stimmen_ueberein() {
    for (MessageStatusKind einordnung : MessageStatusKind.values()) {
      for (String rohwert : classifier.rohwerte(einordnung)) {
        assertThat(classifier.einordnung(rohwert)).as("Rohwert %s", rohwert).isEqualTo(einordnung);
      }
    }
    assertThat(classifier.rohwerte(MessageStatusKind.ZWISCHENSCHRITT))
        .containsExactly("MERGED", "SPLITTED");
  }
}
