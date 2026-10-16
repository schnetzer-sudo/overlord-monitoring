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
