package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die Namensaufloesung, geprueft <b>ohne Datenbank</b>.
 *
 * <p>Das ist der Grund, warum {@link Schrittnamen} keine eigenen Statements hat: Die Regel, die
 * darueber entscheidet, ob ein Nutzer „Datei konvertiert" oder {@code NXS_FILE_CONVERT|E2A|UNWRAP}
 * liest, ist reine Rechenlogik und gehoert einzeln pruefbar. Kein {@code @Tag("db")} — dieser Test
 * laeuft in der CI mit.
 *
 * <p>Die Ablaufkennungen sind hier kurze Platzhalter statt echter UUIDs; die Aufloesung vergleicht
 * sie nur auf Gleichheit.
 */
class SchrittnamenTest {

  private static final String ABLAUF_A = "ablauf-a";
  private static final String ABLAUF_B = "ablauf-b";

  @Nested
  @DisplayName("Stufe 1 — DIREKT")
  class Direkt {

    @Test
    @DisplayName("Die passende SOSAction-Zeile liefert den Namen")
    void passende_zeile_liefert_den_namen() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(
                      ABLAUF_A, (short) 1, "Datei konvertiert", "NXS_FILE_CONVERT|E2A")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 1, "NXS_FILE_CONVERT|E2A|UNWRAP");

      assertThat(aufgeloest.name()).isEqualTo("Datei konvertiert");
      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.DIREKT);
    }

    @Test
    @DisplayName("Der Rohwert kommt auch bei DIREKT mit")
    void rohwert_kommt_auch_bei_direkt_mit() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(new Ablaufschritt(ABLAUF_A, (short) 1, "Datei konvertiert", null)));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 1, "NXS_FILE_CONVERT|E2A|UNWRAP");

      assertThat(aufgeloest.rohwert())
          .as("ohne ihn ist im Zweifelsfall nicht pruefbar, ob der Name zur Sache passt")
          .isEqualTo("NXS_FILE_CONVERT|E2A|UNWRAP");
    }

    @Test
    @DisplayName("Die Kennung eines anderen Ablaufs zaehlt nicht")
    void kennung_eines_anderen_ablaufs_zaehlt_nicht() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(new Ablaufschritt(ABLAUF_B, (short) 1, "Fremder Schritt", null)));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 1, "FTPSender|x");

      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.ROHWERT);
    }
  }

  @Nested
  @DisplayName("Stufe 2 — HERGELEITET")
  class Hergeleitet {

    /**
     * Der gemessene Regelfall aus M20: Der Ablauf definiert die Kennungen 1, 98 und 99, die
     * Ausfuehrung schreibt die Position 2 — und beide tragen die Marke {@code FTPSender}.
     */
    @Test
    @DisplayName("Die erste Marke findet den Schritt unter anderer Kennung")
    void erste_marke_findet_den_schritt_unter_anderer_kennung() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(
                      ABLAUF_A, (short) 1, "Datei konvertiert", "NXS_FILE_CONVERT|E2A"),
                  new Ablaufschritt(
                      ABLAUF_A, (short) 98, "Datei per FTP versendet", "FTPSender|host"),
                  new Ablaufschritt(ABLAUF_A, (short) 99, "An SAP uebergeben", "SAPSender|x")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "FTPSender|host|PASV");

      assertThat(aufgeloest.name()).isEqualTo("Datei per FTP versendet");
      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.HERGELEITET);
      assertThat(aufgeloest.rohwert()).isEqualTo("FTPSender|host|PASV");
    }

    @Test
    @DisplayName("Ein Wert ohne Trennzeichen ist selbst die Marke")
    void wert_ohne_trennzeichen_ist_selbst_die_marke() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(ABLAUF_A, (short) 98, "Empfang bestaetigt", "EERP received")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "EERP received");

      assertThat(aufgeloest.name()).isEqualTo("Empfang bestaetigt");
      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.HERGELEITET);
    }

    /**
     * <b>Die Bedingung, die nicht verhandelbar ist.</b> {@code FTPSender} loest anderswo auf 25
     * verschiedene {@code SOSActionName} auf (M19) — bei zwei Treffern im selben Ablauf waere jede
     * Wahl geraten, und geraten wird nicht (Regel Q4).
     */
    @Test
    @DisplayName("Zwei Schritte mit derselben Marke leiten nicht her")
    void zwei_schritte_mit_derselben_marke_leiten_nicht_her() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(ABLAUF_A, (short) 98, "An Partner A versendet", "FTPSender|a"),
                  new Ablaufschritt(
                      ABLAUF_A, (short) 99, "An Partner B versendet", "FTPSender|b")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "FTPSender|a");

      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.ROHWERT);
      assertThat(aufgeloest.name()).isEqualTo("FTPSender|a");
    }

    @Test
    @DisplayName("Eine Marke ohne Treffer leitet nicht her")
    void marke_ohne_treffer_leitet_nicht_her() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(
                      ABLAUF_A, (short) 1, "Datei konvertiert", "NXS_FILE_CONVERT|E2A")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "Message has been sent");

      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.ROHWERT);
      assertThat(aufgeloest.name()).isEqualTo("Message has been sent");
    }

    @Test
    @DisplayName("Dieselbe Marke in einem anderen Ablauf leitet nicht her")
    void marke_im_anderen_ablauf_leitet_nicht_her() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(new Ablaufschritt(ABLAUF_B, (short) 98, "Fremd versendet", "FTPSender|b")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "FTPSender|a");

      assertThat(aufgeloest.herkunft())
          .as("gesucht wird ausschliesslich im selben Ablauf")
          .isEqualTo(Namensherkunft.ROHWERT);
    }

    @Test
    @DisplayName("Die Schreibweise der Marke entscheidet nicht")
    void schreibweise_der_marke_entscheidet_nicht() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(
                      ABLAUF_A, (short) 98, "Datei per FTP versendet", "FTPSENDER|host")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "ftpsender|host");

      assertThat(aufgeloest.herkunft())
          .as("die Sortierung des Quellschemas ist utf8mb4_general_ci — SQL faende die Zeile")
          .isEqualTo(Namensherkunft.HERGELEITET);
    }
  }

  @Nested
  @DisplayName("Stufe 3 — ROHWERT")
  class Rohwert {

    @Test
    @DisplayName("Ohne jede Ablaufdefinition bleibt der Rohwert")
    void ohne_ablaufdefinition_bleibt_der_rohwert() {
      Schrittnamen namen = Schrittnamen.aus(List.of());

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "FTPSender|host");

      assertThat(aufgeloest.name()).isEqualTo("FTPSender|host");
      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.ROHWERT);
    }

    @Test
    @DisplayName("Ein Rohwert von null bleibt null — es wird kein Ersatztext erfunden")
    void rohwert_null_bleibt_null() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(ABLAUF_A, (short) 1, "Datei konvertiert", "NXS_FILE_CONVERT")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, null);

      assertThat(aufgeloest.name()).isNull();
      assertThat(aufgeloest.rohwert()).isNull();
      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.ROHWERT);
    }

    @Test
    @DisplayName("Ein Wert, der mit dem Trennzeichen beginnt, hat keine erste Marke")
    void wert_ohne_erste_marke() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(new Ablaufschritt(ABLAUF_A, (short) 98, "Irgendein Schritt", "|WAIT|30M")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "|WAIT|30M");

      assertThat(aufgeloest.herkunft())
          .as("aus nichts laesst sich nichts herleiten")
          .isEqualTo(Namensherkunft.ROHWERT);
    }

    @Test
    @DisplayName("Eine Zeile ohne Namen faellt auf den Rohwert durch, statt leer zu bleiben")
    void zeile_ohne_namen_faellt_durch() {
      Schrittnamen namen =
          Schrittnamen.aus(List.of(new Ablaufschritt(ABLAUF_A, (short) 2, "  ", "FTPSender|host")));

      Schrittname aufgeloest = namen.loese(ABLAUF_A, (short) 2, "FTPSender|host");

      assertThat(aufgeloest.name()).isEqualTo("FTPSender|host");
      assertThat(aufgeloest.herkunft()).isEqualTo(Namensherkunft.ROHWERT);
    }
  }

  @Nested
  @DisplayName("Mehrere Ablaeufe in einer Nachricht (M20: 2,2 bis 2,5 Prozent)")
  class MehrereAblaeufe {

    @Test
    @DisplayName("Jede Aktion wird in ihrem eigenen Ablauf aufgeloest")
    void jede_aktion_in_ihrem_eigenen_ablauf() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(ABLAUF_A, (short) 1, "Aus Datei gelesen", "FileReader|in"),
                  new Ablaufschritt(ABLAUF_A, (short) 98, "Per FTP versendet (A)", "FTPSender|a"),
                  new Ablaufschritt(ABLAUF_B, (short) 1, "Zusammengefuehrt", "NXS_MERGE|x"),
                  new Ablaufschritt(ABLAUF_B, (short) 98, "Per FTP versendet (B)", "FTPSender|b")));

      Schrittname ausA = namen.loese(ABLAUF_A, (short) 2, "FTPSender|a");
      Schrittname ausB = namen.loese(ABLAUF_B, (short) 2, "FTPSender|b");
      Schrittname direktAusB = namen.loese(ABLAUF_B, (short) 1, "NXS_MERGE|x");

      assertThat(ausA.name()).isEqualTo("Per FTP versendet (A)");
      assertThat(ausA.herkunft()).isEqualTo(Namensherkunft.HERGELEITET);
      assertThat(ausB.name())
          .as("dieselbe Marke, anderer Ablauf — und deshalb ein anderer Name")
          .isEqualTo("Per FTP versendet (B)");
      assertThat(ausB.herkunft()).isEqualTo(Namensherkunft.HERGELEITET);
      assertThat(direktAusB.name()).isEqualTo("Zusammengefuehrt");
      assertThat(direktAusB.herkunft()).isEqualTo(Namensherkunft.DIREKT);
    }

    @Test
    @DisplayName("Dieselbe Kennung in zwei Ablaeufen wird nicht verwechselt")
    void gleiche_kennung_in_zwei_ablaeufen() {
      Schrittnamen namen =
          Schrittnamen.aus(
              List.of(
                  new Ablaufschritt(ABLAUF_A, (short) 1, "Schritt aus A", "MARKE_A"),
                  new Ablaufschritt(ABLAUF_B, (short) 1, "Schritt aus B", "MARKE_B")));

      assertThat(namen.loese(ABLAUF_A, (short) 1, "MARKE_A").name()).isEqualTo("Schritt aus A");
      assertThat(namen.loese(ABLAUF_B, (short) 1, "MARKE_B").name()).isEqualTo("Schritt aus B");
    }
  }

  @Nested
  @DisplayName("Die erste Marke")
  class ErsteMarke {

    @Test
    @DisplayName("alles vor dem ersten Trennzeichen, sonst der ganze Wert")
    void erste_marke_wird_richtig_geschnitten() {
      assertThat(Schrittnamen.ersteMarke("NXS_MERGE|KE|WAIT|30M")).isEqualTo("NXS_MERGE");
      assertThat(Schrittnamen.ersteMarke("EERP received")).isEqualTo("EERP received");
      assertThat(Schrittnamen.ersteMarke("FTPSender|")).isEqualTo("FTPSender");
      assertThat(Schrittnamen.ersteMarke(null)).isNull();
      assertThat(Schrittnamen.ersteMarke("")).isNull();
      assertThat(Schrittnamen.ersteMarke("|WAIT")).isNull();
      assertThat(Schrittnamen.ersteMarke("   |WAIT")).isNull();
    }
  }

  @Test
  @DisplayName("Eine Aktion ohne Ablaufkennung faellt auf den Rohwert")
  void aktion_ohne_ablaufkennung() {
    Schrittnamen namen =
        Schrittnamen.aus(List.of(new Ablaufschritt(ABLAUF_A, (short) 1, "Datei konvertiert", "X")));

    assertThat(namen.loese(null, (short) 1, "X").herkunft()).isEqualTo(Namensherkunft.ROHWERT);
  }
}
