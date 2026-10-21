package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Ausgang;
import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Ergebnis;
import de.kraftwerkone.overlord.monitor.assistent.Gespraechsfuehrung.Zaehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellanfrage;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellantwort;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Kundennachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Modellnachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Werkzeugergebnisse;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugaufruf;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugergebnis;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Die Gespraechsfuehrung gegen ein geskriptetes Modell ({@code docs/assistent.md} §4.6, §9.1). Es
 * geht kein Aufruf hinaus, und es gibt keine Datenbank.
 */
class GespraechsfuehrungTest {

  private static final String WERK = Testdefinitionen.WERK;

  private static final String UNGUELTIG =
      """
      {"aktion":"Neuanlage","eintraege":[{"werk":"72332","abladestelle":"DDB0","sndprn":"12076"}]}
      """;

  private static final String ANPASSUNG =
      """
      {"aktion":"Anpassung","eintraege":[{"werk":"010","abladestelle":"18VZE","sndprn":"123456"}]}
      """;

  private Skriptmodell modell;
  private Gespraechsfuehrung fuehrung;
  private Gespraech gespraech;
  private Map<String, TypDefinition> angebot;

  @BeforeEach
  void aufbau() {
    modell = new Skriptmodell();
    fuehrung = new Gespraechsfuehrung(modell, Skriptmodell.JSON, 3, 1024);
    TypDefinition werk = Testdefinitionen.werk();
    TypDefinition gattung = Testdefinitionen.gattung();
    angebot = new LinkedHashMap<>();
    angebot.put(werk.typ(), werk);
    angebot.put(gattung.typ(), gattung);
    gespraech =
        new Gespraech(
            UUID.randomUUID(),
            "sitzung",
            1L,
            "NEXANS",
            Anweisung.fuer("Beispielkunde GmbH", List.of(werk, gattung)),
            new Werkzeugbau(Skriptmodell.JSON).werkzeuge(List.of(werk, gattung)),
            Instant.EPOCH);
  }

  private Ergebnis fuehre(String text) {
    return fuehrung.fuehre(gespraech, text, angebot, new Zaehler());
  }

  @Test
  @DisplayName("Ungueltiger Vorschlag: Werkzeugergebnis mit Fehlern, keine Karte")
  void ungueltiger_vorschlag() {
    modell.werkzeug(WERK, UNGUELTIG).text("Die SNDPRN hat nur fünf Stellen. Wie lautet sie?");

    Ergebnis ergebnis = fuehre("Bitte Werk 72332, Abladestelle DDB0, SNDPRN 12076 anlegen");

    assertThat(ergebnis.vorschlag()).isNull();
    assertThat(ergebnis.ausgang()).isEqualTo(Ausgang.TEXT);
    assertThat(ergebnis.text()).isEqualTo("Die SNDPRN hat nur fünf Stellen. Wie lautet sie?");
    Werkzeugergebnis antwort = einzigesErgebnis(modell.letzteAnfrage());
    assertThat(antwort.fehler()).isTrue();
    assertThat(antwort.aufrufId()).isEqualTo("aufruf-1");
    JsonNode inhalt = Skriptmodell.JSON.readTree(antwort.inhalt());
    assertThat(inhalt.get("gueltig").asBoolean()).isFalse();
    assertThat(inhalt.at("/fehler/0/code").asString()).isEqualTo("FORMAT");
    assertThat(inhalt.at("/fehler/0/feld").asString()).isEqualTo("sndprn");
    assertThat(inhalt.at("/fehler/0/meldung").asString()).contains("SNDPRN");
  }

  @Test
  @DisplayName("Gueltiger Vorschlag: Karte mit allen Feldern der Aktion, leere ausdruecklich null")
  void gueltiger_vorschlag() {
    modell.werkzeug(WERK, ANPASSUNG).text("Bitte prüfen Sie die Karte.");

    Ergebnis ergebnis = fuehre("SNDPRN für 010/18VZE auf 123456 ändern, 4908 entfernen");

    assertThat(ergebnis.ausgang()).isEqualTo(Ausgang.VORSCHLAG);
    Vorschlag vorschlag = ergebnis.vorschlag();
    assertThat(vorschlag.typ()).isEqualTo(WERK);
    assertThat(vorschlag.name()).isEqualTo(Testdefinitionen.WERK_NAME);
    assertThat(vorschlag.aktion()).isEqualTo("Anpassung");
    assertThat(vorschlag.felder())
        .extracting(Vorschlag.FeldInfo::name)
        .containsExactly("lieferantennummer", "werk", "abladestelle", "sndprn", "sndprn4908");
    assertThat(vorschlag.felder())
        .filteredOn(feld -> feld.name().equals("sndprn4908"))
        .singleElement()
        .satisfies(
            feld -> {
              assertThat(feld.label()).isEqualTo("SNDPRN_4908");
              assertThat(feld.hinweis()).contains("leer: der Wert wird entfernt");
            });
    Vorschlag.Eintrag eintrag = vorschlag.eintraege().getFirst();
    assertThat(eintrag.werte().keySet())
        .containsExactly("lieferantennummer", "werk", "abladestelle", "sndprn", "sndprn4908");
    assertThat(eintrag.werte())
        .containsEntry("werk", "010")
        .containsEntry("abladestelle", "18VZE")
        .containsEntry("sndprn", "123456");
    assertThat(eintrag.werte().get("lieferantennummer")).isNull();
    assertThat(eintrag.werte().get("sndprn4908")).isNull();
    assertThat(eintrag.schluessel()).isEqualTo("010/18VZE");
    assertThat(einzigesErgebnis(modell.letzteAnfrage()).fehler()).isFalse();
  }

  @Test
  @DisplayName("Loeschung: die Karte nennt nur die Felder, die bei der Loeschung vorkommen")
  void loeschung_ohne_sndprn_felder() {
    modell
        .werkzeug(
            WERK,
            """
            {"aktion":"Löschung","eintraege":[{"werk":"010","abladestelle":"18VZE"}]}
            """)
        .text("Bitte prüfen Sie die Karte.");

    Vorschlag vorschlag = fuehre("010/18VZE löschen").vorschlag();

    assertThat(vorschlag.felder())
        .extracting(Vorschlag.FeldInfo::name)
        .containsExactly("lieferantennummer", "werk", "abladestelle");
    assertThat(vorschlag.eintraege().getFirst().werte()).containsKeys("lieferantennummer");
  }

  @Test
  @DisplayName("Nach drei Werkzeugrunden ist Schluss: feste Antwort, keine Karte, Verlauf kurz")
  void abbruch_nach_drei_runden() {
    modell
        .werkzeug(WERK, UNGUELTIG)
        .werkzeug(WERK, UNGUELTIG)
        .werkzeug(WERK, UNGUELTIG)
        .werkzeug(WERK, ANPASSUNG);

    Ergebnis ergebnis = fuehre("Bitte anlegen");

    assertThat(modell.anfragen()).hasSize(4);
    assertThat(ergebnis.ausgang()).isEqualTo(Ausgang.RUNDEN_AUSGESCHOEPFT);
    assertThat(ergebnis.vorschlag()).isNull();
    assertThat(ergebnis.text()).isEqualTo(Gespraechsfuehrung.RUNDEN_AUSGESCHOEPFT);
    assertThat(gespraech.verlauf())
        .containsExactly(
            new Kundennachricht("Bitte anlegen"),
            Modellnachricht.nurText(Gespraechsfuehrung.RUNDEN_AUSGESCHOEPFT));
  }

  @Test
  @DisplayName("Werkzeugaufruf fuer einen nicht angebotenen Typ wird abgewiesen")
  void nicht_angebotener_typ() {
    angebot.remove("GATTUNG");
    modell
        .werkzeug(
            "GATTUNG",
            """
            {"aktion":"Freigeben","eintraege":[{"konto":"1"}]}
            """)
        .werkzeug("ERFUNDEN", ANPASSUNG)
        .text("Das kann ich nicht.");

    Ergebnis ergebnis = fuehre("Konto 1 freigeben");

    assertThat(ergebnis.vorschlag()).isNull();
    List<Modellanfrage> anfragen = modell.anfragen();
    for (Modellanfrage anfrage : anfragen.subList(1, 3)) {
      Werkzeugergebnis antwort = einzigesErgebnis(anfrage);
      assertThat(antwort.fehler()).isTrue();
      assertThat(antwort.inhalt()).contains(Gespraechsfuehrung.NICHT_ANGEBOTEN);
    }
  }

  @Test
  @DisplayName("Ein zweiter Vorschlag in derselben Nachricht wird abgewiesen, der erste bleibt")
  void ein_vorschlag_je_nachricht() {
    modell
        .zweiWerkzeuge(
            WERK,
            ANPASSUNG,
            """
            {"aktion":"Löschung","eintraege":[{"werk":"72332","abladestelle":"DDB0"}]}
            """)
        .text("Zuerst die Anpassung.");

    Ergebnis ergebnis = fuehre("Anpassen und löschen");

    assertThat(ergebnis.vorschlag().aktion()).isEqualTo("Anpassung");
    List<Werkzeugergebnis> ergebnisse = ergebnisse(modell.letzteAnfrage());
    assertThat(ergebnisse).extracting(Werkzeugergebnis::fehler).containsExactly(false, true);
    assertThat(ergebnisse.get(1).inhalt()).contains(Gespraechsfuehrung.SCHON_EIN_VORSCHLAG);
  }

  @Test
  @DisplayName("Ein Wert als Zahl wird nicht umgewandelt, sondern abgewiesen (E‑1047)")
  void zahl_wird_abgewiesen() {
    modell
        .werkzeug(
            WERK,
            """
            {"aktion":"Neuanlage","eintraege":[{"werk":72332,"abladestelle":"DDB0","sndprn":"120764"}]}
            """)
        .text("Bitte nennen Sie das Werk noch einmal.");

    Ergebnis ergebnis = fuehre("Werk 72332 …");

    assertThat(ergebnis.vorschlag()).isNull();
    Werkzeugergebnis antwort = einzigesErgebnis(modell.letzteAnfrage());
    assertThat(antwort.fehler()).isTrue();
    assertThat(antwort.inhalt()).contains("Eintrag 1, Werk: Werte werden als Text übergeben");
  }

  @Test
  @DisplayName("Abgeschnitten mit Werkzeugaufruf: feste Antwort, die Eingabe wird nicht geprueft")
  void abgeschnitten() {
    modell.antwort(
        new Modellantwort(
            "",
            List.of(new Werkzeugaufruf("aufruf-x", WERK, Skriptmodell.JSON.readTree(ANPASSUNG))),
            Modellantwort.Ende.ABGESCHNITTEN,
            10,
            1024,
            "skript",
            null));

    Ergebnis ergebnis = fuehre("60 Einträge …");

    assertThat(ergebnis.ausgang()).isEqualTo(Ausgang.ABGESCHNITTEN);
    assertThat(ergebnis.vorschlag()).isNull();
    assertThat(ergebnis.text()).isEqualTo(Gespraechsfuehrung.ABGESCHNITTEN);
    assertThat(modell.anfragen()).hasSize(1);
  }

  @Test
  @DisplayName("Verweigert: feste Antwort mit den Namen der Typen, keine Karte")
  void verweigert() {
    modell.antwort(
        new Modellantwort("", List.of(), Modellantwort.Ende.VERWEIGERT, 10, 0, "skript", null));

    Ergebnis ergebnis = fuehre("…");

    assertThat(ergebnis.ausgang()).isEqualTo(Ausgang.VERWEIGERT);
    assertThat(ergebnis.text()).contains(Testdefinitionen.WERK_NAME);
    assertThat(ergebnis.vorschlag()).isNull();
  }

  @Test
  @DisplayName("Fehler des Anbieters: Ausnahme, Verlauf und Zaehlung bleiben unveraendert")
  void fehler_des_anbieters_aendert_nichts() {
    modell.text("Gern.");
    fuehre("Hallo");
    List<Nachricht> vorher = gespraech.verlauf();

    modell.werkzeug(WERK, UNGUELTIG).fehler(Modellfehler.Art.UEBERLASTET);
    assertThatThrownBy(() -> fuehre("Bitte anlegen"))
        .isInstanceOf(Modellfehler.class)
        .extracting(fehler -> ((Modellfehler) fehler).art())
        .isEqualTo(Modellfehler.Art.UEBERLASTET);

    assertThat(gespraech.verlauf()).isEqualTo(vorher);
    assertThat(gespraech.kundennachrichten()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "Jeder Aufruf traegt die festgelegte Anweisung, die Werkzeuge und den ganzen Verlauf")
  void verlauf_waechst_am_ende() {
    modell.text("Wie lautet das Werk?").werkzeug(WERK, ANPASSUNG).text("Bitte prüfen.");

    fuehre("SNDPRN ändern");
    fuehre("Werk 010, Abladestelle 18VZE, SNDPRN 123456");

    List<Modellanfrage> anfragen = modell.anfragen();
    assertThat(anfragen).hasSize(3);
    assertThat(anfragen)
        .allSatisfy(
            anfrage -> {
              assertThat(anfrage.anweisung()).isEqualTo(gespraech.anweisung());
              assertThat(anfrage.werkzeuge()).isEqualTo(gespraech.werkzeuge());
              assertThat(anfrage.maxTokens()).isEqualTo(1024);
            });
    assertThat(anfragen.get(1).verlauf())
        .startsWith(anfragen.get(0).verlauf().toArray(Nachricht[]::new));
    assertThat(anfragen.get(2).verlauf())
        .startsWith(anfragen.get(1).verlauf().toArray(Nachricht[]::new));
    assertThat(gespraech.verlauf()).hasSize(6);
    assertThat(gespraech.kundennachrichten()).isEqualTo(2);
  }

  @Test
  @DisplayName("Der Zaehler fuer das Protokoll summiert Token und Werkzeugaufrufe")
  void zaehler() {
    modell.werkzeug(WERK, UNGUELTIG).werkzeug(WERK, ANPASSUNG).text("Bitte prüfen.");
    Zaehler zaehler = new Zaehler();

    fuehrung.fuehre(gespraech, "Bitte", angebot, zaehler);

    assertThat(zaehler.tokenEin()).isEqualTo(30);
    assertThat(zaehler.tokenAus()).isEqualTo(15);
    assertThat(zaehler.werkzeugaufrufe()).isEqualTo(2);
    assertThat(zaehler.modell()).isEqualTo("skript");
  }

  private static Werkzeugergebnis einzigesErgebnis(Modellanfrage anfrage) {
    List<Werkzeugergebnis> ergebnisse = ergebnisse(anfrage);
    assertThat(ergebnisse).hasSize(1);
    return ergebnisse.getFirst();
  }

  private static List<Werkzeugergebnis> ergebnisse(Modellanfrage anfrage) {
    Nachricht letzte = anfrage.verlauf().getLast();
    assertThat(letzte).isInstanceOf(Werkzeugergebnisse.class);
    return ((Werkzeugergebnisse) letzte).ergebnisse();
  }
}
