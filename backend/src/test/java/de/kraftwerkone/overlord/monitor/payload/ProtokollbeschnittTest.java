package de.kraftwerkone.overlord.monitor.payload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Der Beschnitt gegen <b>alle fuenf Faelle</b> aus {@code docs/rohdaten.md} §6.
 *
 * <p><b>Jede Protokolldatei hier ist frei erfunden.</b> Kein echter Partner, kein echter Knoten,
 * kein echter Pfad, keine echte Kennung — genau wie bei den drei Selbstpruefungsdateien unter
 * {@code scripts/messung-schritt8/testdaten/}. Erfundenes ist als solches benannt ({@code
 * .invalid}, {@code ERFUNDEN-}, {@code BEISPIELPROD42}).
 *
 * <p>Der Test braucht keine Datenbank und keinen Filestore und traegt deshalb kein
 * {@code @Tag("db")}.
 */
class ProtokollbeschnittTest {

  private static String datei(String... zeilen) {
    return String.join("\n", zeilen);
  }

  @Nested
  @DisplayName("Fall 1 — vollstaendiges Paar")
  class VollstaendigesPaar {

    @Test
    @DisplayName("Der Innenbereich kommt heraus, ohne die Markenzeilen")
    void innenbereich_ohne_marken() {
      String log =
          datei(
              "davor: wird nicht gezeigt",
              "***StartOfLog***",
              "Erfundene Zeile 1",
              "Erfundene Zeile 2",
              "***EndOfLog***",
              "danach: wird nicht gezeigt");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.beschnitten()).isTrue();
      assertThat(ergebnis.text()).isEqualTo("Erfundene Zeile 1\nErfundene Zeile 2");
      assertThat(ergebnis.text())
          .as("Die Markenzeilen selbst erscheinen nicht in der Ausgabe")
          .doesNotContain(Protokollbeschnitt.STARTMARKE)
          .doesNotContain(Protokollbeschnitt.ENDMARKE);
      assertThat(ergebnis.text())
          .as("Was ausserhalb steht, bleibt draussen")
          .doesNotContain("davor")
          .doesNotContain("danach");
    }

    @Test
    @DisplayName("CRLF und LF gemischt aendern nichts — die Zeilenenden sind uneinheitlich (M61)")
    void gemischte_zeilenenden() {
      String log =
          "davor\r\n***StartOfLog***\r\nErfundene Zeile 1\nErfundene Zeile 2\r\n***EndOfLog***\n";

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.text())
          .as("Kein haengengebliebenes \\r am Zeilenende")
          .isEqualTo("Erfundene Zeile 1\nErfundene Zeile 2");
    }
  }

  @Nested
  @DisplayName("Fall 2 — mehrere Paare")
  class MehrerePaare {

    @Test
    @DisplayName("Nur das erste Paar, nicht alle Bloecke aneinander")
    void nur_das_erste_paar() {
      String log =
          datei(
              "***StartOfLog***",
              "Erfundene Zeile aus Block A",
              "***EndOfLog***",
              "dazwischen",
              "***StartOfLog***",
              "Erfundene Zeile aus Block B",
              "***EndOfLog***");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.text()).isEqualTo("Erfundene Zeile aus Block A");
      assertThat(ergebnis.text())
          .as(
              "Das Altsystem haengt hier alle Bloecke aneinander (:841). Diese Regel ist"
                  + " ausdruecklich strenger — sonst gibt eine eingeschleuste Marke Bereiche frei,"
                  + " die aussen liegen")
          .doesNotContain("Block B");
    }
  }

  @Nested
  @DisplayName("Fall 3 — Startmarke ohne Endmarke")
  class StartOhneEnde {

    @Test
    @DisplayName("Nichts, mit benanntem Zustand — nicht alles ab der Startmarke")
    void nichts_statt_alles() {
      String log =
          datei(
              "davor",
              "***StartOfLog***",
              "Erfundene Zeile 1",
              "Erfundene Zeile 2",
              "Erfundene Zeile 3");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand())
          .as(
              "Das Altsystem zeigt hier alles ab der Startmarke (:823). Diese Regel ist"
                  + " ausdruecklich strenger")
          .isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
      assertThat(ergebnis.text()).isEmpty();
      assertThat(ergebnis.beschnitten()).isTrue();
    }
  }

  @Nested
  @DisplayName("Fall 4 — keine Startmarke")
  class KeineStartmarke {

    @Test
    @DisplayName("Nichts, mit benanntem Zustand — nicht „dann eben alles\"")
    void nichts_statt_alles() {
      String log = datei("Erfundene Zeile 1", "Erfundene Zeile 2", "***EndOfLog***", "noch etwas");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
      assertThat(ergebnis.text()).isEmpty();
    }

    @Test
    @DisplayName("Das ist der gemessene Normalfall bei HTTPSender und FTPSender (M63)")
    void familien_ganz_ohne_marken() {
      String log =
          datei(
              "2025-12-29 10:00:00 ERFUNDEN-Verbindung aufgebaut",
              "2025-12-29 10:00:01 ERFUNDEN-Datei uebertragen",
              "2025-12-29 10:00:02 ERFUNDEN-Verbindung beendet");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand())
          .as(
              "HTTPSender traegt in 30 von 30 Faellen keine Marken, FTPSender in 28 von 30 —"
                  + " MANDANT sieht dort nie ein Protokoll. Ausdruecklich hingenommen")
          .isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
    }

    @Test
    @DisplayName("Eine leere Datei ebenso")
    void leere_datei() {
      assertThat(Protokollbeschnitt.beschneide("").zustand())
          .isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
      assertThat(Protokollbeschnitt.beschneide(null).zustand())
          .isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
    }
  }

  @Nested
  @DisplayName("Fall 5 — Paar ohne Inhalt")
  class PaarOhneInhalt {

    @Test
    @DisplayName("Auch hier nichts — und kein leerer Kasten")
    void unmittelbar_geschlossen() {
      String log = datei("***StartOfLog***", "***EndOfLog***", "danach");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand())
          .as("§8: keiner der Zustaende ist ein leeres Feld")
          .isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
      assertThat(ergebnis.text()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Die Marken sind von aussen beeinflussbar")
  class EingeschleusteMarke {

    /**
     * Derselbe Fall, den die Selbstpruefung des Auswertungsskripts an {@code 3.log} erzeugt hat:
     * Ein Dateiname mit {@code ***EndOfLog***} darin schliesst das Paar sofort.
     *
     * <p>Er belegt zugleich, warum die beiden strengeren Regeln richtig sind — und dass der
     * Beschnitt trotzdem <b>keine Vertraulichkeitszusage</b> ist: Wer eine Marke einschleusen kann,
     * bestimmt mit, wo geschnitten wird.
     */
    @Test
    @DisplayName("Eine echote Endmarke schliesst das Paar sofort")
    void echote_endmarke() {
      String log =
          datei(
              "***StartOfLog***",
              "Message.DestinationFilename=erfunden***EndOfLog***.txt",
              "Erfundene Zeile, die nicht mehr erscheint",
              "***EndOfLog***");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);
      assertThat(ergebnis.text()).isEmpty();
    }

    @Test
    @DisplayName("Erkannt wird mit „enthaelt\", nicht mit Gleichheit — wie im Altsystem (Q2)")
    void enthaelt_statt_gleichheit() {
      String log =
          datei(
              "10:00:00 ***StartOfLog*** Beginn",
              "Erfundene Zeile 1",
              "10:00:09 ***EndOfLog*** Ende");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(ergebnis.text()).isEqualTo("Erfundene Zeile 1");
    }
  }

  @Nested
  @DisplayName("Pfadmaskierung — nur im beschnittenen Zweig (Q3)")
  class Maskierung {

    @Test
    @DisplayName("Beide Praefixe werden im Innenbereich zu /IS/")
    void beide_praefixe() {
      String log =
          datei(
              "***StartOfLog***",
              "lese /opt/txp/users/ERFUNDENESKONTO/eingang/datei.001",
              "schreibe /srv/lobster/IS/ausgang/datei.002",
              "***EndOfLog***");

      Protokollbeschnitt.Ergebnis ergebnis = Protokollbeschnitt.beschneide(log);

      assertThat(ergebnis.text())
          .isEqualTo("lese /IS/eingang/datei.001\nschreibe /IS/ausgang/datei.002");
      assertThat(ergebnis.text()).doesNotContain("ERFUNDENESKONTO").doesNotContain("/opt/txp");
    }

    @Test
    @DisplayName("Eine Zeile mit beiden Praefixen wird ganz maskiert")
    void beide_in_einer_zeile() {
      String log =
          datei(
              "***StartOfLog***",
              "kopiere /opt/txp/users/ERFUNDENESKONTO/a nach /srv/lobster/IS/b",
              "***EndOfLog***");

      assertThat(Protokollbeschnitt.beschneide(log).text())
          .as(
              "Das Altsystem verwendet else if und maskiert je Zeile hoechstens eines der beiden"
                  + " Praefixe")
          .isEqualTo("kopiere /IS/a nach /IS/b");
    }

    @Test
    @DisplayName("Was ausserhalb der Marken steht, wird gar nicht erst erreicht")
    void ausserhalb_wird_nicht_maskiert() {
      String log =
          datei(
              "aussen: /opt/txp/users/ERFUNDENESKONTO/geheim",
              "***StartOfLog***",
              "innen: ohne Pfad",
              "***EndOfLog***");

      assertThat(Protokollbeschnitt.beschneide(log).text()).isEqualTo("innen: ohne Pfad");
    }
  }
}
