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
    assertThat(classifier.einordnung("MERGED")).isEqualTo(MessageStatusKind.ZUSAMMENGEFUEHRT);
    assertThat(classifier.einordnung("SPLITTED")).isEqualTo(MessageStatusKind.AUFGETEILT);
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
    assertThat(classifier.istEndstatus(MessageStatusKind.AUFGETEILT)).isTrue();
    assertThat(classifier.istEndstatus(MessageStatusKind.ZUSAMMENGEFUEHRT)).isTrue();
    assertThat(classifier.istEndstatus(MessageStatusKind.UNGEKLAERT)).isTrue();
  }

  /**
   * Die Entscheidung, die 34,38 Prozent aller Zeilen betrifft: {@code SPLITTED} und {@code MERGED}
   * sind fertig. Zaehlten sie als offen, waere jede dritte Zeile ein Kandidat fuer „ueberfaellig".
   *
   * <p><b>Der Test haelt fest, dass die Aufteilung von {@code ZWISCHENSCHRITT} daran nichts
   * geaendert hat</b> (11.08.2026). Aus einem Wert wurden zwei; die Antwort auf „kann fuer diese
   * Zeile noch eine Frist ablaufen" ist fuer beide dieselbe wie vorher.
   */
  @Test
  @DisplayName("SPLITTED und MERGED sind Endstatus, SUSPENDED nicht")
  void aufgeteilt_und_zusammengefuehrt_sind_endstatus() {
    assertThat(classifier.istEndstatus("SPLITTED")).isTrue();
    assertThat(classifier.istEndstatus("MERGED")).isTrue();
    assertThat(classifier.istEndstatus("SUSPENDED")).isFalse();
    assertThat(classifier.istEndstatus("RUNNING")).isFalse();
  }

  /**
   * Die Gegenprobe zur Aufteilung: Sie hat den <b>Status</b> geteilt und nicht die
   * Ueberfaelligkeitsrechnung. Eine gesplittete Nachricht mit laengst abgelaufener Frist bleibt
   * nicht ueberfaellig — sonst waeren bei {@code NEXANS} 39,6 Prozent aller Zeilen Kandidaten.
   */
  @Test
  @DisplayName("Weder SPLITTED noch MERGED werden je ueberfaellig")
  void aufgeteilt_und_zusammengefuehrt_werden_nicht_ueberfaellig() {
    LocalDateTime langeHer = LocalDateTime.parse("2025-01-01T00:00:00");
    LocalDateTime jetzt = LocalDateTime.parse("2025-12-30T04:09:47");

    assertThat(classifier.istUeberfaellig("SPLITTED", langeHer, 1800, jetzt)).isFalse();
    assertThat(classifier.istUeberfaellig("MERGED", langeHer, 1800, jetzt)).isFalse();
    assertThat(classifier.istUeberfaellig("SUSPENDED", langeHer, 1800, jetzt))
        .as("die Gegenprobe — sonst pruefte der Test nur, dass nichts ueberfaellig wird")
        .isTrue();
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

  /**
   * <b>Die SQL-Fassung der Ueberfaelligkeit gegen die Java-Fassung.</b> Beide gehoeren derselben
   * Frage, und beide sind aus {@link MessageStatusClassifier#istEndstatus} gezogen — waeren sie es
   * nicht, rechnete die Liste anders als die Detailansicht.
   */
  @Test
  @DisplayName("Die offenen Rohwerte des SQL sind genau die, die istEndstatus offen laesst")
  void offene_rohwerte_stammen_aus_istEndstatus() {
    assertThat(classifier.offeneRohwerte()).containsExactly("RUNNING", "SUSPENDED");
    for (String rohwert : classifier.bekannteStatuswerte()) {
      assertThat(classifier.offeneRohwerte().contains(rohwert))
          .as("Rohwert %s", rohwert)
          .isEqualTo(!classifier.istEndstatus(rohwert));
    }
  }

  /**
   * Die Einheit von {@code MessageTimeout} steht an zwei Stellen — als {@link ChronoUnit} fuer Java
   * und als {@code DatePart} fuer SQL. Ein Auseinanderlaufen waere im Betrieb unsichtbar: Aus
   * dreissig Minuten wuerden dreissig Stunden, und die Kachel „Ueberfaellig" bliebe leer.
   */
  @Test
  @DisplayName("Java und SQL rechnen mit derselben Einheit: Sekunden")
  void timeout_einheit_ist_in_beiden_fassungen_dieselbe() {
    assertThat(MessageStatusClassifier.TIMEOUT_EINHEIT).isEqualTo(ChronoUnit.SECONDS);
    assertThat(sql(ueberfaelligBedingung(JETZT))).contains(" second)");
  }

  /**
   * Die SQL-Bedingung, Bedingungsteil fuer Bedingungsteil gegen {@link
   * MessageStatusClassifier#istUeberfaellig} gehalten.
   */
  @Test
  @DisplayName("Die SQL-Bedingung traegt alle vier Teile der Java-Fassung")
  void ueberfaelligBedingung_traegt_alle_vier_teile() {
    String sql = sql(ueberfaelligBedingung(JETZT));

    assertThat(sql)
        .contains("in ('RUNNING', 'SUSPENDED')")
        .contains("`MessageTimeout` is not null")
        .contains("`MessageTimeout` > 0")
        .contains("date_add(`MessageLastUpdate`, interval `MessageTimeout` second)")
        .contains("timestamp '2025-12-30 04:09:47.0'");
    // Kein IS NOT NULL auf MessageLastUpdate: NULL + INTERVAL ist selbst NULL und damit nicht
    // wahr. Die Bedingung waere wirkungslos und wiche von der gemessenen Fassung ab (M97).
    assertThat(sql).doesNotContain("`MessageLastUpdate` is not null");
  }

  /**
   * Die eine Zeile, die beide Fassungen zusammenhaelt: Was {@link
   * MessageStatusClassifier#istUeberfaellig} fuer einen Rohwert sagt, muss die Statusmenge des SQL
   * ebenso sagen — fuer <b>jeden</b> bekannten Wert und fuer einen unbekannten dazu.
   */
  @Test
  @DisplayName("Java und SQL sind sich ueber jeden bekannten Statuswert einig")
  void java_und_sql_stimmen_je_statuswert_ueberein() {
    LocalDateTime laengstFaellig = LocalDateTime.parse("2025-12-30T00:00:00");
    for (String rohwert : classifier.bekannteStatuswerte()) {
      boolean javaSagtUeberfaellig = classifier.istUeberfaellig(rohwert, laengstFaellig, 60, JETZT);
      assertThat(classifier.offeneRohwerte().contains(rohwert))
          .as("Rohwert %s", rohwert)
          .isEqualTo(javaSagtUeberfaellig);
    }
    // Ein unbekannter Wert ist Endstatus und steht folgerichtig in keiner der beiden Fassungen.
    assertThat(classifier.istUeberfaellig("GIBTESNICHT", laengstFaellig, 60, JETZT)).isFalse();
    assertThat(sql(ueberfaelligBedingung(JETZT))).doesNotContain("GIBTESNICHT");
  }

  private org.jooq.Condition ueberfaelligBedingung(LocalDateTime jetzt) {
    return classifier.ueberfaelligBedingung(STATUS, LETZTE_AENDERUNG, FRIST, jetzt);
  }

  /** Der Anker der Anwendungsuhr im Profil {@code dev}. */
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  private static final Field<LocalDateTime> LETZTE_AENDERUNG =
      DSL.field(DSL.name("MessageLastUpdate"), LocalDateTime.class);

  private static final Field<Short> FRIST = DSL.field(DSL.name("MessageTimeout"), Short.class);

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
    assertThat(sql(classifier.bedingung(MessageStatusKind.AUFGETEILT, STATUS)))
        .contains("SPLITTED")
        .doesNotContain("MERGED");
    assertThat(sql(classifier.bedingung(MessageStatusKind.ZUSAMMENGEFUEHRT, STATUS)))
        .contains("MERGED")
        .doesNotContain("SPLITTED");
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

  @Test
  @DisplayName("Die Rohwerte je Einordnung stammen aus derselben Zuordnung wie die Anzeige")
  void rohwerte_und_einordnung_stimmen_ueberein() {
    for (MessageStatusKind einordnung : MessageStatusKind.values()) {
      for (String rohwert : classifier.rohwerte(einordnung)) {
        assertThat(classifier.einordnung(rohwert)).as("Rohwert %s", rohwert).isEqualTo(einordnung);
      }
    }
    assertThat(classifier.rohwerte(MessageStatusKind.AUFGETEILT)).containsExactly("SPLITTED");
    assertThat(classifier.rohwerte(MessageStatusKind.ZUSAMMENGEFUEHRT)).containsExactly("MERGED");
  }
}
