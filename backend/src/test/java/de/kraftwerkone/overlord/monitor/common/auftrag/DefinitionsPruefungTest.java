package de.kraftwerkone.overlord.monitor.common.auftrag;

import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.JSON;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.pruefe;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.werkBaum;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Die Pruefung einer Definition beim Laden ({@code docs/tasks.md} §3.6): je Regel mindestens ein
 * ungueltiger Fall, und die beiden gueltigen Definitionen bleiben gueltig.
 */
class DefinitionsPruefungTest {

  @Test
  @DisplayName("Die Definition von WERK_ABLADESTELLE ist gueltig und so gelesen, wie sie dasteht")
  void werk_ist_gueltig() {
    DefinitionsPruefung.Ergebnis ergebnis =
        DefinitionsPruefung.pruefe(
            Testdefinitionen.WERK,
            Testdefinitionen.WERK_NAME,
            Testdefinitionen.werkBeschreibung(),
            werkBaum());

    assertThat(ergebnis.gruende()).isEmpty();
    TypDefinition werk = ergebnis.definition();
    assertThat(werk.aktionen()).containsExactly("Neuanlage", "Anpassung", "Löschung");
    assertThat(werk.maxEintraege()).isEqualTo(20);
    assertThat(werk.felder())
        .extracting(TypDefinition.Feld::name)
        .containsExactly("lieferantennummer", "werk", "abladestelle", "sndprn", "sndprn4908");
    assertThat(werk.schluessel().felder())
        .containsExactly("lieferantennummer", "werk", "abladestelle");
    assertThat(werk.schluessel().trenner()).isEqualTo("/");
    assertThat(werk.rueckgaben().ok()).isEqualTo("Ok");
    assertThat(werk.rueckgaben().weitere())
        .containsExactly("Schlüssel nicht gefunden", "Schlüssel bereits vorhanden");
    assertThat(werk.woertlicheZeichen()).containsExactlyInAnyOrder((int) ',', (int) '/');
  }

  @Test
  @DisplayName("Die Gattung aus dem Test ist gueltig")
  void gattung_ist_gueltig() {
    assertThat(pruefe(Testdefinitionen.gattungBaum()).gruende()).isEmpty();
  }

  @Test
  @DisplayName("Alle Gruende kommen zurueck, nicht nur der erste")
  void alle_gruende() {
    ObjectNode struktur = werkBaum();
    struktur.put("maxEintraege", 0);
    struktur.put("unbekannt", true);
    ((ObjectNode) struktur.get("rueckgaben")).remove("ok");

    assertThat(pruefe(struktur).gruende()).hasSize(3);
  }

  @Nested
  @DisplayName("Aufbau")
  class Aufbau {

    @Test
    void struktur_ist_kein_objekt() {
      DefinitionsPruefung.Ergebnis ergebnis =
          DefinitionsPruefung.pruefe("PROBE", "Probe", "", JSON.readTree("[1, 2]"));
      assertThat(ergebnis.gueltig()).isFalse();
      assertThat(ergebnis.gruende()).anyMatch(grund -> grund.contains("kein JSON-Objekt"));
    }

    @Test
    void name_leer() {
      assertThat(DefinitionsPruefung.pruefe("PROBE", " ", "", werkBaum()).gruende())
          .containsExactly("Der Name ist leer.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aktionen", "maxEintraege", "felder", "zeile", "rueckgaben"})
    void pflichtschluessel_fehlt(String schluessel) {
      ObjectNode struktur = werkBaum();
      struktur.remove(schluessel);
      assertThat(pruefe(struktur).gruende())
          .contains("Der Pflichtschlüssel „" + schluessel + "“ fehlt.");
    }

    @Test
    void ohne_schluessel_und_regeln_gueltig_wenn_keine_vorlage_ihn_nennt() {
      ObjectNode struktur = werkBaum();
      struktur.remove("schluessel");
      struktur.remove("regeln");
      ObjectNode zeile = (ObjectNode) struktur.get("zeile");
      zeile.put("Neuanlage", "{werk}/{abladestelle},{sndprn},{sndprn4908}");
      zeile.put("Anpassung", "{werk}/{abladestelle},{sndprn},{sndprn4908}");
      zeile.put("Löschung", "{werk}/{abladestelle}");
      assertThat(pruefe(struktur).gruende()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Unbekannte Schluessel machen die Definition ungueltig")
  class UnbekannteSchluessel {

    @Test
    void in_der_struktur() {
      fall(struktur -> struktur.put("maxEintrage", 5), "Unbekannter Schlüssel „maxEintrage“");
    }

    @Test
    void in_einem_feld() {
      fall(
          struktur -> feld(struktur, 1).put("muser", "^x$"),
          "Unbekannter Schlüssel „muser“ in felder[1]");
    }

    @Test
    void im_schluessel() {
      fall(
          struktur -> ((ObjectNode) struktur.get("schluessel")).put("trennzeichen", "/"),
          "Unbekannter Schlüssel „trennzeichen“");
    }

    @Test
    void in_einer_regel() {
      fall(
          struktur -> regel(struktur, 1).putArray("bei").add("Neuanlage"),
          "Unbekannter Schlüssel „bei“ in regeln[1]");
    }

    @Test
    void in_den_rueckgaben() {
      fall(
          struktur -> ((ObjectNode) struktur.get("rueckgaben")).put("fehler", "x"),
          "Unbekannter Schlüssel „fehler“");
    }
  }

  @Nested
  @DisplayName("aktionen und maxEintraege")
  class AktionenUndGrenzen {

    @Test
    void keine_aktion() {
      fall(struktur -> struktur.putArray("aktionen"), "1 bis 10 Aktionen");
    }

    @Test
    void elf_aktionen() {
      fall(
          struktur -> {
            ArrayNode aktionen = (ArrayNode) struktur.get("aktionen");
            for (int i = 0; i < 8; i++) {
              aktionen.add("Weitere " + i);
            }
          },
          "1 bis 10 Aktionen");
    }

    @Test
    void aktion_doppelt() {
      fall(
          struktur -> ((ArrayNode) struktur.get("aktionen")).add("Neuanlage"),
          "steht doppelt in „aktionen“");
    }

    @Test
    void aktion_mit_zeilenumbruch() {
      fall(
          struktur ->
              ((ArrayNode) struktur.get("aktionen")).set(0, JSON.valueToTree("Neu\nanlage")),
          "Zeilenumbruch oder ein Steuerzeichen");
    }

    @Test
    void aktion_zu_lang() {
      fall(
          struktur ->
              ((ArrayNode) struktur.get("aktionen")).set(0, JSON.valueToTree("A".repeat(65))),
          "nicht 1 bis 64 Zeichen");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "501", "-1", "1.5", "\"50\""})
    void max_eintraege_ausserhalb(String wert) {
      fall(struktur -> struktur.set("maxEintraege", JSON.readTree(wert)), "„maxEintraege“");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 500})
    void max_eintraege_an_den_grenzen(int wert) {
      ObjectNode struktur = werkBaum();
      struktur.put("maxEintraege", wert);
      assertThat(pruefe(struktur).gruende()).isEmpty();
    }
  }

  @Nested
  @DisplayName("felder")
  class Felder {

    @Test
    void muster_nicht_uebersetzbar() {
      fall(struktur -> feld(struktur, 3).put("muster", "^[0-9{6}$"), "nicht übersetzbar");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Werk", "1werk", "werk_nummer", ""})
    void feldname_falsch(String name) {
      fall(struktur -> feld(struktur, 1).put("name", name), "folgt nicht [a-z][a-z0-9]*");
    }

    @Test
    void feldname_doppelt() {
      fall(struktur -> feld(struktur, 2).put("name", "werk"), "steht doppelt in „felder“");
    }

    @Test
    void feldname_schluessel_ist_reserviert() {
      fall(struktur -> feld(struktur, 4).put("name", "schluessel"), "reserviert");
    }

    @Test
    void label_fehlt() {
      fall(struktur -> feld(struktur, 1).remove("label"), "„label“ fehlt");
    }

    @Test
    void pflicht_kein_wahrheitswert() {
      fall(struktur -> feld(struktur, 1).put("pflicht", "ja"), "kein Wahrheitswert");
    }

    @Test
    void nur_bei_unbekannte_aktion() {
      fall(
          struktur -> ((ArrayNode) feld(struktur, 3).get("nurBei")).add("Anpasung"),
          "„Anpasung“ ist keine Aktion dieses Typs");
    }

    @Test
    void keine_felder() {
      fall(struktur -> struktur.putArray("felder"), "mindestens einem Feld");
    }
  }

  @Nested
  @DisplayName("schluessel")
  class Schluessel {

    @Test
    void optionaler_teil_nicht_vorn() {
      fall(
          struktur -> {
            ArrayNode felder = (ArrayNode) ((ObjectNode) struktur.get("schluessel")).get("felder");
            felder.removeAll();
            felder.add("werk").add("lieferantennummer").add("abladestelle");
          },
          "nur der erste Teil des Schlüssels darf optional sein");
    }

    @Test
    void kein_teil_pflicht() {
      fall(
          struktur -> {
            ArrayNode felder = (ArrayNode) ((ObjectNode) struktur.get("schluessel")).get("felder");
            felder.removeAll();
            felder.add("lieferantennummer");
          },
          "Mindestens ein Teil des Schlüssels muss Pflicht sein");
    }

    @Test
    void unbekanntes_feld() {
      fall(
          struktur ->
              ((ArrayNode) ((ObjectNode) struktur.get("schluessel")).get("felder")).add("lager"),
          "unbekannte Feld „lager“");
    }

    @Test
    void schluesselfeld_mit_nur_bei() {
      fall(
          struktur -> feld(struktur, 1).putArray("nurBei").add("Neuanlage"),
          "darf kein „nurBei“ tragen");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "//", "\t"})
    void trenner_nicht_genau_ein_zeichen(String trenner) {
      fall(
          struktur -> ((ObjectNode) struktur.get("schluessel")).put("trenner", trenner),
          "„schluessel.trenner“");
    }
  }

  @Nested
  @DisplayName("regeln")
  class Regeln {

    @Test
    void unbekannte_art() {
      fall(struktur -> regel(struktur, 0).put("art", "hoechstensEins"), "andere Regelbausteine");
    }

    @Test
    void unbekanntes_feld() {
      fall(
          struktur -> ((ArrayNode) regel(struktur, 0).get("felder")).add("sndprn9999"),
          "„sndprn9999“ ist kein bekanntes Feld");
    }

    @Test
    void unbekannte_aktion_in_bei() {
      fall(
          struktur -> ((ArrayNode) regel(struktur, 0).get("bei")).add("Löschen"),
          "„Löschen“ ist keine Aktion dieses Typs");
    }

    @Test
    void eindeutig_ueber_schluessel_ohne_schluessel() {
      fall(
          struktur -> {
            struktur.remove("schluessel");
            ObjectNode zeile = (ObjectNode) struktur.get("zeile");
            zeile.put("Neuanlage", "{werk}");
            zeile.put("Anpassung", "{werk}");
            zeile.put("Löschung", "{werk}");
          },
          "„schluessel“ ist kein bekanntes Feld");
    }
  }

  @Nested
  @DisplayName("zeile")
  class Zeile {

    @Test
    void aktion_ohne_zeilenvorlage() {
      fall(
          struktur -> ((ObjectNode) struktur.get("zeile")).remove("Löschung"),
          "Für die Aktion „Löschung“ fehlt die Zeilenvorlage");
    }

    @Test
    void vorlage_fuer_unbekannte_aktion() {
      fall(
          struktur -> ((ObjectNode) struktur.get("zeile")).put("Anpasung", "{schluessel}"),
          "unbekannte Aktion „Anpasung“");
    }

    @Test
    void platzhalter_ohne_feld() {
      fall(
          struktur -> ((ObjectNode) struktur.get("zeile")).put("Löschung", "{schluessel},{lager}"),
          "Der Platzhalter {lager} nennt kein Feld");
    }

    @Test
    void schluessel_platzhalter_ohne_schluessel() {
      fall(
          struktur -> struktur.remove("schluessel"),
          "Der Platzhalter {schluessel} nennt kein Feld");
    }

    @Test
    void einzelne_klammer() {
      fall(
          struktur -> ((ObjectNode) struktur.get("zeile")).put("Löschung", "{schluessel}}"),
          "geschweifte Klammer");
    }

    @Test
    void zeilenumbruch_in_der_vorlage() {
      fall(
          struktur -> ((ObjectNode) struktur.get("zeile")).put("Löschung", "{schluessel}\n{werk}"),
          "Zeilenumbruch oder ein Steuerzeichen");
    }
  }

  @Nested
  @DisplayName("rueckgaben")
  class Rueckgaben {

    @Test
    void ok_fehlt() {
      fall(struktur -> ((ObjectNode) struktur.get("rueckgaben")).remove("ok"), "„rueckgaben.ok“");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " Ok", "Ok ", "O\nk"})
    void ok_leer_oder_mit_leerraum(String ok) {
      fall(struktur -> ((ObjectNode) struktur.get("rueckgaben")).put("ok", ok), "„rueckgaben.ok“");
    }

    @Test
    void ok_auch_in_weitere() {
      fall(
          struktur ->
              ((ArrayNode) ((ObjectNode) struktur.get("rueckgaben")).get("weitere")).add("Ok"),
          "steht auch in „rueckgaben.weitere“");
    }

    @Test
    void weitere_ist_optional() {
      ObjectNode struktur = werkBaum();
      ((ObjectNode) struktur.get("rueckgaben")).remove("weitere");
      assertThat(pruefe(struktur).gruende()).isEmpty();
    }
  }

  // ─── Hilfen ────────────────────────────────────────────────────────────────────────────────

  /** Veraendert die Struktur von WERK_ABLADESTELLE und verlangt einen Grund mit diesem Text. */
  private static void fall(Consumer<ObjectNode> aenderung, String erwarteterGrund) {
    ObjectNode struktur = werkBaum();
    aenderung.accept(struktur);
    DefinitionsPruefung.Ergebnis ergebnis = pruefe(struktur);
    assertThat(ergebnis.gueltig()).as("ungueltig: %s", erwarteterGrund).isFalse();
    assertThat(ergebnis.gruende()).anyMatch(grund -> grund.contains(erwarteterGrund));
  }

  private static ObjectNode feld(ObjectNode struktur, int index) {
    return (ObjectNode) struktur.get("felder").get(index);
  }

  private static ObjectNode regel(ObjectNode struktur, int index) {
    return (ObjectNode) struktur.get("regeln").get(index);
  }
}
