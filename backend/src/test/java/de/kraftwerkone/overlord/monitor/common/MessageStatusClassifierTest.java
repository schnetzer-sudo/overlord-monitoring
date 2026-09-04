package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
   * Die Gegenprobe zur Aufteilung: Sie hat den <b>Status</b> geteilt und nicht die Frage, was offen
   * ist. Eine gesplittete Nachricht ist als Zeile fertig — sonst waeren bei {@code NEXANS} 39,6
   * Prozent aller Zeilen offen.
   *
   * <p><b>Bis zum 03.09.2026 pruefte dieser Fall ueber {@code istUeberfaellig}</b>, dass keiner der
   * beiden je ueberfaellig wird. Die Methode ist mit E‑71 entfallen; gemeint war immer {@code
   * istEndstatus}, und die Zusicherung ist unveraendert dieselbe.
   */
  @Test
  @DisplayName("Weder SPLITTED noch MERGED sind je offen")
  void aufgeteilt_und_zusammengefuehrt_sind_nie_offen() {
    assertThat(classifier.istEndstatus("SPLITTED")).isTrue();
    assertThat(classifier.istEndstatus("MERGED")).isTrue();
    assertThat(classifier.istEndstatus("SUSPENDED"))
        .as("die Gegenprobe — sonst pruefte der Test nur, dass alles Endstatus ist")
        .isFalse();
  }

  @Test
  @DisplayName("UNGEKLAERT bleibt aus den Problemkategorien heraus")
  void ungeklaertes_bleibt_draussen() {
    for (String status : new String[] {"CHECKED", "CKECKED", "COMMIT_SENT", "VOELLIG_UNBEKANNT"}) {
      assertThat(classifier.istEndstatus(status))
          .as(
              "%s ist ungeklaert. Es als offen zu fuehren waere die Behauptung, wir wuessten, dass"
                  + " die Nachricht nicht fertig ist.",
              status)
          .isTrue();
    }
  }

  // ─── Der eine Rohwert je offener Einordnung (Schritt 10b-4) ────────────────────────────────

  /**
   * <b>Die Kacheln <i>Laeuft</i> und <i>Wartend</i> haengen daran.</b> Sie vergleichen mit {@code
   * =} auf den Rohwert, und das ist nur zulaessig, solange jede der beiden Einordnungen genau einen
   * traegt.
   */
  @Test
  @DisplayName("LAEUFT und WARTEND tragen je genau einen bekannten Rohwert")
  void einziger_rohwert_je_offener_einordnung() {
    assertThat(classifier.einzigerRohwert(MessageStatusKind.LAEUFT)).isEqualTo("RUNNING");
    assertThat(classifier.einzigerRohwert(MessageStatusKind.WARTEND)).isEqualTo("SUSPENDED");
  }

  /**
   * <b>Die Gegenprobe, und sie ist der eigentliche Wert dieser Methode.</b> Sie wirft, statt eine
   * halbe Antwort zu geben: Bekaeme {@code WARTEND} je einen zweiten Rohwert, zaehlte die Kachel
   * stillschweigend nur noch die Haelfte.
   */
  @Test
  @DisplayName("Eine Einordnung mit mehreren oder keinem Rohwert wird abgewiesen")
  void einziger_rohwert_weist_mehrdeutige_einordnungen_ab() {
    assertThatThrownBy(() -> classifier.einzigerRohwert(MessageStatusKind.QUITTIERT))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("2")
        .hasMessageContaining("COMMIT_RECEIVED");
  }

  /**
   * <b>Die Einheit bleibt, obwohl sie im Anwendungscode keinen Verbraucher mehr hat.</b> Mit E‑71
   * sind {@code istUeberfaellig}, {@code timeoutZeitpunkt} und {@code ueberfaelligBedingung}
   * entfallen — und mit ihnen jede Stelle, die {@link MessageStatusClassifier#TIMEOUT_EINHEIT}
   * liest. <b>Der Test ist damit ihr einziger Verbraucher, und das ist Absicht:</b> Die Belegkette
   * aus M8 ist teuer erarbeitet, und die Einheit wird an dem Tag wieder gebraucht, an dem eine
   * echte Schwelle fuer {@code RUNNING} zurueckkommt. Dieselbe Behandlung wie {@code
   * --ueberfaellig} in {@code globals.css} (E‑77).
   */
  @Test
  @DisplayName("MessageTimeout ist eine Dauer in Sekunden — auch ohne Verbraucher (M8)")
  void timeout_ist_in_sekunden() {
    assertThat(MessageStatusClassifier.TIMEOUT_EINHEIT).isEqualTo(ChronoUnit.SECONDS);
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
