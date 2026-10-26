package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeug;
import de.kraftwerkone.overlord.monitor.common.auftrag.DefinitionsPruefung;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Die Werkzeuge aus der Struktur der Typen (E‑1034, {@code docs/assistent.md} §4.5): das Schema
 * fuer {@code WERK_ABLADESTELLE} vollstaendig, die Gattung aus A1, und <b>kein
 * Mandantenparameter</b>.
 */
class WerkzeugbauTest {

  private final Werkzeugbau werkzeugbau = new Werkzeugbau(Skriptmodell.JSON);

  @Test
  @DisplayName("WERK_ABLADESTELLE: Name, Beschreibung und Schema wie in der Testressource")
  void werk_abladestelle() throws IOException {
    Werkzeug werkzeug = werkzeugbau.werkzeug(Testdefinitionen.werk());

    JsonNode erwartet;
    try (InputStream quelle =
        getClass().getResourceAsStream("/assistent/werkzeug-werk-abladestelle.json")) {
      erwartet =
          Skriptmodell.JSON.readTree(new String(quelle.readAllBytes(), StandardCharsets.UTF_8));
    }
    assertThat(werkzeug.name()).isEqualTo(erwartet.get("name").stringValue());
    assertThat(werkzeug.beschreibung()).isEqualTo(erwartet.get("description").stringValue());
    assertThat(werkzeug.eingabeschema()).isEqualTo(erwartet.get("input_schema"));
  }

  @Test
  @DisplayName("Gattung: Aktionen als enum, maxItems aus maxEintraege, genau die Felder")
  void gattung() {
    JsonNode schema = werkzeugbau.werkzeug(Testdefinitionen.gattung()).eingabeschema();

    assertThat(texte(schema.at("/properties/aktion/enum")))
        .containsExactly("Sperren", "Freigeben", "Umbenennen");
    assertThat(schema.at("/properties/eintraege/maxItems").asInt()).isEqualTo(3);
    assertThat(schema.at("/properties/eintraege/items/properties").propertyNames())
        .containsExactly("bereich", "konto", "grund", "neuername");
    assertThat(schema.at("/properties/eintraege/items/additionalProperties").asBoolean()).isFalse();
    assertThat(schema.at("/properties/eintraege/items/properties/neuername/description").asString())
        .contains("Neuer Name.", "Pflichtfeld.", "Nur bei „Umbenennen“");
    assertThat(werkzeugbau.werkzeug(Testdefinitionen.gattung()).beschreibung())
        .contains("besteht aus Bereich, Konto");
  }

  /** Der Satz, der folgt, sobald ein Teil des Schluessels optional ist (E‑1170). */
  private static final String WENIGER_TEILE =
      " Hat ein zusammengesetzter Schlüssel weniger Teile als der Schlüssel Felder, zerlege ihn"
          + " nur, wenn eindeutig ist, welche fehlen; sonst frage nach, statt zu raten.";

  @Test
  @DisplayName(
      "Zusammengesetzter Schluessel: zerlegen mit dem Trenner des Typs; der optionale Teil mit"
          + " seinem Label")
  void zusammengesetzter_schluessel() {
    assertThat(werkzeugbau.werkzeug(Testdefinitionen.gattung()).beschreibung())
        .contains(
            "Schreibt der Kunde einen Schlüssel zusammengesetzt mit dem Trenner „-“, zerlegst du"
                + " ihn ohne Rückfrage in diese Felder in ihrer Reihenfolge. Optional ist der Teil"
                + " Bereich."
                + WENIGER_TEILE
                + " Schlüsselteile übergibst du")
        .doesNotContain("„/“", "fehlt ein Teil", "optionale erste");
  }

  @Test
  @DisplayName("Optionale Teile an jeder Position: genannt mit Label, in der Reihenfolge")
  void optionale_teile_an_jeder_position() {
    assertThat(beschreibung(mitPflicht(false, true, true)))
        .contains(" Optional ist der Teil Lieferantennummer." + WENIGER_TEILE);
    assertThat(beschreibung(mitPflicht(true, false, true)))
        .contains(" Optional ist der Teil Werk." + WENIGER_TEILE);
    assertThat(beschreibung(mitPflicht(true, true, false)))
        .contains(" Optional ist der Teil Abladestelle." + WENIGER_TEILE);
    assertThat(beschreibung(mitPflicht(false, true, false)))
        .contains(" Optional sind die Teile Lieferantennummer und Abladestelle." + WENIGER_TEILE);

    // Vier Teile, drei davon optional: Die Aufzaehlung trennt mit Komma und „und“.
    ObjectNode vier = mitPflicht(false, false, false);
    ((ArrayNode) vier.get("felder"))
        .addObject()
        .put("name", "lager")
        .put("label", "Lager")
        .put("pflicht", true);
    ((ArrayNode) vier.get("schluessel").get("felder")).add("lager");
    assertThat(beschreibung(vier))
        .contains(
            "besteht aus Lieferantennummer, Werk, Abladestelle, Lager, in dieser Reihenfolge.")
        .contains(
            " Optional sind die Teile Lieferantennummer, Werk und Abladestelle." + WENIGER_TEILE);
  }

  @Test
  @DisplayName("Ohne optionalen Teil: der Text wie vor dem 26.10.2026, Wort fuer Wort")
  void schluessel_ohne_optionalen_teil() {
    ObjectNode struktur = Testdefinitionen.gattungBaum();
    ((ObjectNode) struktur.get("felder").get(0)).put("pflicht", true);

    assertThat(werkzeugbau.werkzeug(gueltig("NUR_PFLICHT", struktur)).beschreibung())
        .isEqualTo(
            "Reicht einen Vorschlag für den Auftrag „Name NUR_PFLICHT“ ein. Rufe dieses Werkzeug,"
                + " sobald die Angaben des Kunden vollständig sind. Das Backend prüft den Vorschlag:"
                + " Meldet es Fehler, erkläre sie dem Kunden und frage nach; ist er gültig, sieht der"
                + " Kunde ihn als Karte und legt den Auftrag dort selbst an. Felder ohne Wert lässt"
                + " du weg. Werte übergibst du als Text, genau so, wie der Kunde sie nennt. Der"
                + " Schlüssel eines Eintrags besteht aus Bereich, Konto, in dieser Reihenfolge."
                + " Schreibt der Kunde einen Schlüssel zusammengesetzt mit dem Trenner „-“,"
                + " zerlegst du ihn ohne Rückfrage in diese Felder in ihrer Reihenfolge."
                + " Schlüsselteile übergibst du zeichengenau, mit jedem Leerzeichen, auch mehreren"
                + " hintereinander; Leerzeichen am Anfang und Ende entfernt das Backend.");
    assertThat(beschreibung(mitPflicht(true, true, true))).doesNotContain("Optional");
  }

  @Test
  @DisplayName(
      "Schluesselteile zeichengenau, mit jedem Leerzeichen — nur bei einem Typ mit Schluessel")
  void schluesselteile_zeichengenau() {
    assertThat(werkzeugbau.werkzeug(Testdefinitionen.gattung()).beschreibung())
        .endsWith(
            " Schlüsselteile übergibst du zeichengenau, mit jedem Leerzeichen, auch mehreren"
                + " hintereinander; Leerzeichen am Anfang und Ende entfernt das Backend.");

    ObjectNode ohneSchluessel = Testdefinitionen.gattungBaum();
    ohneSchluessel.remove("schluessel");
    ObjectNode zeile = (ObjectNode) ohneSchluessel.get("zeile");
    zeile.put("Sperren", "{konto};{grund}");
    zeile.put("Umbenennen", "{konto}|{neuername}");
    assertThat(werkzeugbau.werkzeug(gueltig("OHNE_SCHLUESSEL", ohneSchluessel)).beschreibung())
        .doesNotContain("Schlüssel");
  }

  @Test
  @DisplayName("Kein Schema enthaelt eine Eigenschaft, deren Name mit mandant beginnt")
  void kein_mandantenparameter() {
    for (TypDefinition typ : List.of(Testdefinitionen.werk(), Testdefinitionen.gattung())) {
      List<String> namen = new ArrayList<>();
      sammleEigenschaften(werkzeugbau.werkzeug(typ).eingabeschema(), namen);
      assertThat(namen).as("Eigenschaften von %s", typ.typ()).isNotEmpty();
      assertThat(namen)
          .as("Eigenschaften von %s", typ.typ())
          .noneMatch(name -> name.toLowerCase().startsWith("mandant"));
    }
  }

  @Test
  @DisplayName("Ein Typ mit einem Feld mandant… oder ungueltigem Werkzeugnamen entfaellt (E‑1054)")
  void ungeeignete_typen_entfallen() {
    ObjectNode mitMandant = Testdefinitionen.gattungBaum();
    ((ArrayNode) mitMandant.get("felder"))
        .addObject()
        .put("name", "mandantid")
        .put("label", "Mandant");
    TypDefinition mandantFeld = gueltig("MIT_MANDANT", mitMandant);
    TypDefinition falscherName = gueltig("MIT LEERZEICHEN", Testdefinitionen.gattungBaum());
    TypDefinition gut = gueltig("GUT", Testdefinitionen.gattungBaum());

    assertThat(Werkzeugbau.geeignete(List.of(mandantFeld, falscherName, gut)))
        .extracting(TypDefinition::typ)
        .containsExactly("GUT");
  }

  private String beschreibung(ObjectNode struktur) {
    return werkzeugbau.werkzeug(gueltig("PROBE", struktur)).beschreibung();
  }

  /**
   * Die Struktur von {@code WERK_ABLADESTELLE} mit {@code pflicht} an den drei Schluesselteilen, in
   * ihrer Reihenfolge — unabhaengig davon, was die Testressource gerade traegt.
   */
  private static ObjectNode mitPflicht(boolean lieferant, boolean werk, boolean abladestelle) {
    ObjectNode struktur = Testdefinitionen.werkBaum();
    boolean[] pflicht = {lieferant, werk, abladestelle};
    for (int i = 0; i < pflicht.length; i++) {
      ObjectNode feld = (ObjectNode) struktur.get("felder").get(i);
      if (pflicht[i]) {
        feld.put("pflicht", true);
      } else {
        feld.remove("pflicht");
      }
    }
    return struktur;
  }

  private static TypDefinition gueltig(String typ, ObjectNode struktur) {
    DefinitionsPruefung.Ergebnis ergebnis =
        DefinitionsPruefung.pruefe(typ, "Name " + typ, "Beschreibung", struktur);
    assertThat(ergebnis.gruende()).isEmpty();
    return ergebnis.definition();
  }

  /** Alle Namen unter jedem {@code properties} des Schemas, rekursiv. */
  private static void sammleEigenschaften(JsonNode knoten, List<String> namen) {
    if (knoten.isObject()) {
      for (Map.Entry<String, JsonNode> eintrag : knoten.properties()) {
        if (eintrag.getKey().equals("properties")) {
          namen.addAll(eintrag.getValue().propertyNames());
        }
        sammleEigenschaften(eintrag.getValue(), namen);
      }
    } else if (knoten.isArray()) {
      knoten.forEach(kind -> sammleEigenschaften(kind, namen));
    }
  }

  private static List<String> texte(JsonNode liste) {
    List<String> texte = new ArrayList<>();
    liste.forEach(knoten -> texte.add(knoten.stringValue()));
    return texte;
  }
}
