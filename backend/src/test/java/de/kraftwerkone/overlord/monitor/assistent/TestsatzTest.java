package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.AuftragsPruefung;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Der Testsatz selbst, ohne Modell ({@code docs/assistent.md} §8): Form der Faelle, Abdeckung der
 * 14 Gruppen (13 aus dem Auftrag, dazu die Anpassung mit nur einem Wert), jede erwartete Karte
 * besteht die Pruefung — und die strukturelle Bewertung vergleicht so, wie beschrieben.
 */
class TestsatzTest {

  private static final TypDefinition WERK = Testdefinitionen.werk();

  @Test
  @DisplayName("17 synthetische Faelle, jede der 14 Gruppen mindestens einmal, Kennungen eindeutig")
  void form_und_abdeckung() throws IOException {
    JsonNode faelle = lies().get("faelle");
    Set<String> kennungen = new HashSet<>();
    Set<Integer> gruppen = new HashSet<>();
    for (JsonNode fall : faelle) {
      assertThat(kennungen.add(fall.get("kennung").stringValue())).isTrue();
      assertThat(fall.get("herkunft").stringValue()).isIn("synthetisch", "echt");
      assertThat(fall.get("nachrichten").size()).isPositive();
      int erwartungen =
          (fall.has("erwartet") ? 1 : 0)
              + (fall.has("erwartetNach") ? 1 : 0)
              + (fall.has("erwartetEinerVon") ? 1 : 0);
      assertThat(erwartungen).as("Fall %s", fall.get("kennung")).isEqualTo(1);
      if (fall.has("erwartetNach")) {
        assertThat(fall.get("erwartetNach").size()).isEqualTo(fall.get("nachrichten").size());
      }
      gruppen.add(fall.get("gruppe").asInt());
    }
    assertThat(faelle.size()).isEqualTo(17);
    assertThat(gruppen).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
  }

  @Test
  @DisplayName(
      "Jede erwartete Karte besteht die Pruefung aus A1 — sonst waere der Fall unerfuellbar")
  void erwartete_karten_sind_gueltig() throws IOException {
    for (JsonNode fall : lies().get("faelle")) {
      for (JsonNode erwartung : erwartungen(fall)) {
        JsonNode vorschlag = erwartung.get("vorschlag");
        if (vorschlag.isNull()) {
          continue;
        }
        assertThat(
                AuftragsPruefung.pruefe(
                    WERK, vorschlag.get("aktion").stringValue(), eintraege(vorschlag)))
            .as("Fall %s", fall.get("kennung"))
            .isEmpty();
      }
    }
  }

  @Test
  @DisplayName("Begriffe nur ohne Vorschlag, als Liste nicht leerer Texte; S12 und S17 tragen sie")
  void begriffe_nur_ohne_vorschlag() throws IOException {
    List<String> mitBegriffen = new ArrayList<>();
    for (JsonNode fall : lies().get("faelle")) {
      for (JsonNode erwartung : erwartungen(fall)) {
        assertThat(erwartung.propertyNames())
            .as("Fall %s", fall.get("kennung"))
            .isSubsetOf("vorschlag", "begriffe");
        JsonNode begriffe = erwartung.get("begriffe");
        if (begriffe == null) {
          continue;
        }
        mitBegriffen.add(fall.get("kennung").stringValue());
        assertThat(erwartung.get("vorschlag").isNull()).as("Fall %s", fall.get("kennung")).isTrue();
        assertThat(begriffe.isArray() && !begriffe.isEmpty()).isTrue();
        begriffe.forEach(begriff -> assertThat(begriff.stringValue()).isNotBlank());
      }
    }
    assertThat(mitBegriffen).containsExactlyInAnyOrder("S12", "S17");
  }

  @Test
  @DisplayName(
      "Begriffe: ohne Ruecksicht auf Gross- und Kleinschreibung, jeder einzeln, ohne Antwort fehlen alle")
  void begriffe_bewertung() {
    JsonNode erwartet =
        Skriptmodell.JSON.readTree(
            "{\"vorschlag\":null,\"begriffe\":[\"SNDPRN_4908\",\"Mandant\"]}");

    assertThat(
            AssistentTestsatzLauf.fehlendeBegriffe(
                erwartet, "Soll sndprn_4908 entfernt werden? Der MANDANT bleibt."))
        .isEmpty();
    assertThat(AssistentTestsatzLauf.fehlendeBegriffe(erwartet, "Soll SNDPRN_4908 entfallen?"))
        .containsExactly("Mandant");
    assertThat(AssistentTestsatzLauf.fehlendeBegriffe(erwartet, null))
        .containsExactly("SNDPRN_4908", "Mandant");
    assertThat(
            AssistentTestsatzLauf.fehlendeBegriffe(
                Skriptmodell.JSON.readTree("{\"vorschlag\":null}"), "beliebig"))
        .isEmpty();
  }

  @Test
  @DisplayName("Bewertung: Reihenfolge der Eintraege egal, leere Felder entfallen, Werte genau")
  void bewertung() {
    Vorschlag karte =
        Vorschlag.aus(
            WERK,
            "Neuanlage",
            List.of(
                Testdefinitionen.eintrag("werk", "1", "abladestelle", "A", "sndprn", "123456"),
                Testdefinitionen.eintrag(
                    "werk", "2", "abladestelle", "B", "sndprn4908", "654321")));
    JsonNode umgekehrt =
        Skriptmodell.JSON.readTree(
            """
            {"vorschlag":{"typ":"WERK_ABLADESTELLE","aktion":"Neuanlage","eintraege":[
              {"werk":"2","abladestelle":"B","sndprn4908":"654321"},
              {"werk":"1","abladestelle":"A","sndprn":"123456"}]}}
            """);

    assertThat(AssistentTestsatzLauf.passt(umgekehrt, karte)).isTrue();
    assertThat(
            AssistentTestsatzLauf.passt(Skriptmodell.JSON.readTree("{\"vorschlag\":null}"), null))
        .isTrue();
    assertThat(
            AssistentTestsatzLauf.passt(Skriptmodell.JSON.readTree("{\"vorschlag\":null}"), karte))
        .isFalse();
    assertThat(
            AssistentTestsatzLauf.passt(
                Skriptmodell.JSON.readTree(umgekehrt.toString().replace("654321", "654322")),
                karte))
        .isFalse();
    assertThat(
            AssistentTestsatzLauf.passt(
                Skriptmodell.JSON.readTree(umgekehrt.toString().replace("Neuanlage", "Anpassung")),
                karte))
        .isFalse();
  }

  /** Alle Erwartungen eines Falls, gleich in welcher der drei Formen. */
  private static List<JsonNode> erwartungen(JsonNode fall) {
    List<JsonNode> erwartet = new ArrayList<>();
    for (String feld : List.of("erwartet", "erwartetNach", "erwartetEinerVon")) {
      JsonNode knoten = fall.get(feld);
      if (knoten == null) {
        continue;
      }
      if (knoten.isArray()) {
        knoten.forEach(erwartet::add);
      } else {
        erwartet.add(knoten);
      }
    }
    return erwartet;
  }

  private static List<Map<String, String>> eintraege(JsonNode vorschlag) {
    List<Map<String, String>> eintraege = new ArrayList<>();
    for (JsonNode eintrag : vorschlag.get("eintraege")) {
      Map<String, String> werte = new HashMap<>();
      eintrag.properties().forEach(feld -> werte.put(feld.getKey(), feld.getValue().stringValue()));
      eintraege.add(werte);
    }
    return eintraege;
  }

  private static JsonNode lies() throws IOException {
    try (InputStream quelle =
        TestsatzTest.class.getResourceAsStream("/assistent/testsatz/faelle.json")) {
      return Skriptmodell.JSON.readTree(new String(quelle.readAllBytes(), StandardCharsets.UTF_8));
    }
  }
}
