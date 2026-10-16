package de.kraftwerkone.overlord.monitor.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Die Pruefung eines Auftrags ({@code docs/tasks.md} §5.3): jeder Fehlercode mindestens einmal, die
 * Grenzfaelle aus dem Auftrag zu A1, der Zeichenschutz und eine zweite Definition, die nur im Test
 * besteht.
 */
class AuftragsPruefungTest {

  private static final TypDefinition WERK = Testdefinitionen.werk();
  private static final TypDefinition GATTUNG = Testdefinitionen.gattung();

  @Nested
  @DisplayName("WERK_ABLADESTELLE")
  class Werk {

    @Test
    @DisplayName("Die drei Beispiele der Quelle sind gueltig")
    void beispiele_sind_gueltig() {
      assertThat(pruefe(WERK, "Neuanlage", ParametertextTest.neuanlage())).isEmpty();
      assertThat(pruefe(WERK, "Löschung", ParametertextTest.loeschung())).isEmpty();
      assertThat(pruefe(WERK, "Anpassung", ParametertextTest.anpassung())).isEmpty();
    }

    @Test
    void aktion_unbekannt() {
      List<Prueffehler> fehler =
          pruefe(
              WERK,
              "Anpasung",
              List.of(eintrag("werk", "1", "abladestelle", "A", "sndprn", "123456")));
      assertThat(codes(fehler)).containsExactly(Fehlercode.AKTION_UNBEKANNT);
      assertThat(fehler.getFirst().index()).isNull();
      assertThat(fehler.getFirst().meldung())
          .isEqualTo(
              "Die Aktion „Anpasung“ gibt es für diesen Auftrag nicht. Möglich sind: Neuanlage,"
                  + " Anpassung, Löschung.");
    }

    @Test
    void aktion_fehlt() {
      assertThat(codes(pruefe(WERK, null, List.of(schluessel("1", "A")))))
          .containsExactly(Fehlercode.AKTION_UNBEKANNT);
    }

    @Test
    void leere_liste() {
      assertThat(codes(pruefe(WERK, "Löschung", List.of())))
          .containsExactly(Fehlercode.ZU_WENIGE_EINTRAEGE);
      assertThat(codes(pruefe(WERK, "Löschung", null)))
          .containsExactly(Fehlercode.ZU_WENIGE_EINTRAEGE);
    }

    @Test
    @DisplayName("50 Eintraege gehen, 51 nicht")
    void fuenfzig_und_einundfuenfzig() {
      assertThat(pruefe(WERK, "Löschung", schluessel(50))).isEmpty();
      List<Prueffehler> fehler = pruefe(WERK, "Löschung", schluessel(51));
      assertThat(codes(fehler)).containsExactly(Fehlercode.ZU_VIELE_EINTRAEGE);
      assertThat(fehler.getFirst().meldung())
          .isEqualTo("Der Auftrag enthält 51 Einträge; möglich sind höchstens 50.");
    }

    @Test
    void feld_unbekannt() {
      Map<String, String> eintrag = schluessel("1", "A");
      eintrag.put("lieferant", "4711");
      List<Prueffehler> fehler = pruefe(WERK, "Löschung", List.of(eintrag));
      assertThat(fehler)
          .containsExactly(
              new Prueffehler(
                  0,
                  "lieferant",
                  Fehlercode.FELD_UNBEKANNT,
                  "Eintrag 1: Das Feld „lieferant“ gibt es in diesem Auftrag nicht."));
    }

    @Test
    @DisplayName("Loeschung mit SNDPRN: Das Feld ist bei Loeschung nicht erlaubt")
    void loeschung_mit_sndprn() {
      Map<String, String> eintrag = schluessel("1", "A");
      eintrag.put("sndprn", "120764");
      assertThat(pruefe(WERK, "Löschung", List.of(eintrag)))
          .containsExactly(
              new Prueffehler(
                  0,
                  "sndprn",
                  Fehlercode.FELD_NICHT_ERLAUBT,
                  "Eintrag 1, SNDPRN: Bei „Löschung“ wird hier kein Wert angegeben."));
    }

    @Test
    @DisplayName("Ein leeres SNDPRN-Feld bei Loeschung ist fehlend, nicht verboten")
    void loeschung_mit_leerem_sndprn() {
      Map<String, String> eintrag = schluessel("1", "A");
      eintrag.put("sndprn", "");
      eintrag.put("sndprn4908", null);
      assertThat(pruefe(WERK, "Löschung", List.of(eintrag))).isEmpty();
    }

    @Test
    void pflicht_fehlt() {
      Map<String, String> eintrag = eintrag("werk", "", "sndprn", "120764");
      List<Prueffehler> fehler = pruefe(WERK, "Neuanlage", List.of(eintrag));
      assertThat(fehler)
          .extracting(Prueffehler::feld, Prueffehler::code)
          .containsExactly(
              tuple("werk", Fehlercode.PFLICHT_FEHLT),
              tuple("abladestelle", Fehlercode.PFLICHT_FEHLT));
      assertThat(fehler.getFirst().meldung()).isEqualTo("Eintrag 1, Werk: Der Wert fehlt.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "12345a", " 123456"})
    @DisplayName("SNDPRN mit fuenf oder sieben Ziffern, Buchstabe, Leerzeichen: FORMAT")
    void sndprn_format(String wert) {
      List<Prueffehler> fehler =
          pruefe(
              WERK,
              "Neuanlage",
              List.of(eintrag("werk", "1", "abladestelle", "A", "sndprn", wert)));
      assertThat(fehler)
          .containsExactly(
              new Prueffehler(
                  0,
                  "sndprn",
                  Fehlercode.FORMAT,
                  "Eintrag 1, SNDPRN: Der Wert „"
                      + wert
                      + "“ hat nicht die erwartete Form, zum Beispiel „120764“."));
    }

    @Test
    @DisplayName("Schluesselteil mit 35 Zeichen geht, mit 36 nicht")
    void schluesselteil_35_und_36() {
      assertThat(pruefe(WERK, "Löschung", List.of(schluessel("W".repeat(35), "A")))).isEmpty();
      assertThat(codes(pruefe(WERK, "Löschung", List.of(schluessel("W".repeat(36), "A")))))
          .containsExactly(Fehlercode.FORMAT);
    }

    @Test
    @DisplayName("Leerzeichen im Schluesselteil: FORMAT, kein unzulaessiges Zeichen")
    void leerzeichen_im_schluessel() {
      assertThat(pruefe(WERK, "Löschung", List.of(schluessel("723 32", "A"))))
          .extracting(Prueffehler::feld, Prueffehler::code)
          .containsExactly(tuple("werk", Fehlercode.FORMAT));
    }

    @Test
    @DisplayName(
        "Tabulator im Schluesselteil: das Muster laesst ihn durch, der Zeichenschutz nicht")
    void tabulator_im_schluessel() {
      List<Prueffehler> fehler = pruefe(WERK, "Löschung", List.of(schluessel("723\t32", "A")));
      assertThat(fehler)
          .containsExactly(
              new Prueffehler(
                  0,
                  "werk",
                  Fehlercode.UNZULAESSIGES_ZEICHEN,
                  "Eintrag 1, Werk: Der Wert enthält ein unzulässiges Zeichen (Steuerzeichen"
                      + " U+0009)."));
    }

    @Test
    @DisplayName("Ein Wert wird nicht gekuerzt: Leerraum am Rand faellt ueber das Muster")
    void kein_kuerzen() {
      assertThat(codes(pruefe(WERK, "Löschung", List.of(schluessel(" 72332", "DDB0")))))
          .containsExactly(Fehlercode.FORMAT);
    }

    @Test
    @DisplayName("Anpassung mit beiden Werten leer: MINDESTENS_EINS")
    void anpassung_beide_leer() {
      Map<String, String> eintrag = schluessel("010", "18VZE");
      eintrag.put("sndprn", "");
      List<Prueffehler> fehler = pruefe(WERK, "Anpassung", List.of(schluessel("1", "A"), eintrag));
      assertThat(fehler).hasSize(2);
      assertThat(fehler.get(1))
          .isEqualTo(
              new Prueffehler(
                  1,
                  null,
                  Fehlercode.MINDESTENS_EINS,
                  "Eintrag 2: Mindestens eines der Felder SNDPRN oder SNDPRN_4908 braucht einen"
                      + " Wert."));
    }

    @Test
    void neuanlage_ohne_werte_aber_loeschung_ohne_regel() {
      assertThat(codes(pruefe(WERK, "Neuanlage", List.of(schluessel("1", "A")))))
          .containsExactly(Fehlercode.MINDESTENS_EINS);
      assertThat(pruefe(WERK, "Löschung", List.of(schluessel("1", "A")))).isEmpty();
    }

    @Test
    @DisplayName("Schluessel doppelt: DOPPELT am spaeteren Eintrag")
    void schluessel_doppelt() {
      List<Prueffehler> fehler =
          pruefe(
              WERK,
              "Löschung",
              List.of(
                  schluessel("010", "18VZE"),
                  schluessel("72332", "DDB0"),
                  schluessel("010", "18VZE")));
      assertThat(fehler)
          .containsExactly(
              new Prueffehler(
                  2,
                  null,
                  Fehlercode.DOPPELT,
                  "Eintrag 3 hat denselben Schlüssel wie Eintrag 1 („010/18VZE“)."));
    }

    @Test
    @DisplayName("Mit und ohne Lieferantennummer, oder anders geschrieben: nicht doppelt")
    void verschiedene_schluessel() {
      Map<String, String> mitLieferant = schluessel("22", "001N0");
      mitLieferant.put("lieferantennummer", "0101159903");
      assertThat(
              pruefe(
                  WERK,
                  "Löschung",
                  List.of(schluessel("22", "001N0"), mitLieferant, schluessel("22", "001n0"))))
          .isEmpty();
    }

    @Test
    @DisplayName("Alle Fehler gesammelt, in der Reihenfolge der Pruefung")
    void alle_fehler_gesammelt() {
      Map<String, String> eintrag = eintrag("werk", "7/2", "sndprn", "1", "lager", "x");
      List<Prueffehler> fehler = pruefe(WERK, "Neuanlage", List.of(eintrag, eintrag));
      assertThat(codes(fehler))
          .containsExactly(
              Fehlercode.FELD_UNBEKANNT,
              Fehlercode.FORMAT,
              Fehlercode.UNZULAESSIGES_ZEICHEN,
              Fehlercode.PFLICHT_FEHLT,
              Fehlercode.FORMAT,
              Fehlercode.FELD_UNBEKANNT,
              Fehlercode.FORMAT,
              Fehlercode.UNZULAESSIGES_ZEICHEN,
              Fehlercode.PFLICHT_FEHLT,
              Fehlercode.FORMAT,
              Fehlercode.DOPPELT);
    }

    @Test
    @DisplayName("Unbekannte Aktion: Pflicht und nurBei entfallen, Muster und Zeichen nicht")
    void unbekannte_aktion_prueft_den_rest() {
      List<Prueffehler> fehler =
          pruefe(WERK, "Umbau", List.of(eintrag("werk", "7 2", "sndprn", "120764")));
      assertThat(codes(fehler)).containsExactly(Fehlercode.AKTION_UNBEKANNT, Fehlercode.FORMAT);
    }
  }

  @Nested
  @DisplayName("Zeichenschutz (E-1022) an einem Feld ohne Muster")
  class Zeichenschutz {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "a-b", // der Schluesseltrenner
          "a;b", // steht in der Vorlage von Sperren
          "a|b", // steht in der Vorlage von Umbenennen — jede Vorlage zaehlt
          "a\nb",
          "a\rb",
          "a\u0007b",
          "a\u0085b",
          "a b"
        })
    void verbotene_zeichen(String grund) {
      List<Prueffehler> fehler =
          pruefe(GATTUNG, "Sperren", List.of(eintrag("konto", "17", "grund", grund)));
      assertThat(fehler)
          .extracting(Prueffehler::feld, Prueffehler::code)
          .containsExactly(tuple("grund", Fehlercode.UNZULAESSIGES_ZEICHEN));
    }

    @Test
    void erlaubte_zeichen() {
      assertThat(
              pruefe(
                  GATTUNG,
                  "Sperren",
                  List.of(eintrag("konto", "17", "grund", "Mahnung offen, Prüfung {konto} / 5"))))
          .isEmpty();
    }

    @Test
    void meldung_nennt_das_zeichen() {
      assertThat(
              pruefe(GATTUNG, "Sperren", List.of(eintrag("konto", "17", "grund", "a;b")))
                  .getFirst()
                  .meldung())
          .isEqualTo("Eintrag 1, Grund: Der Wert enthält ein unzulässiges Zeichen („;“).");
      assertThat(
              pruefe(GATTUNG, "Sperren", List.of(eintrag("konto", "17", "grund", "a\nb")))
                  .getFirst()
                  .meldung())
          .isEqualTo(
              "Eintrag 1, Grund: Der Wert enthält ein unzulässiges Zeichen (Zeilenumbruch).");
    }
  }

  @Nested
  @DisplayName("Gattung: eine Definition, die nur im Test besteht")
  class Gattung {

    @Test
    void gueltig() {
      assertThat(
              pruefe(
                  GATTUNG,
                  "Sperren",
                  List.of(
                      eintrag("bereich", "NO", "konto", "17", "grund", "Mahnung"),
                      eintrag("konto", "42", "grund", "Prüfung"))))
          .isEmpty();
    }

    @Test
    @DisplayName("eindeutig ueber ein Feld, nicht ueber den Schluessel")
    void eindeutig_ueber_feld() {
      assertThat(
              pruefe(
                  GATTUNG,
                  "Freigeben",
                  List.of(eintrag("bereich", "NO", "konto", "17"), eintrag("konto", "17"))))
          .containsExactly(
              new Prueffehler(
                  1,
                  "konto",
                  Fehlercode.DOPPELT,
                  "Eintrag 2 hat dieselben Werte in Konto wie Eintrag 1."));
    }

    @Test
    void mindestens_eins_mit_einem_feld_nur_bei_sperren() {
      assertThat(codes(pruefe(GATTUNG, "Sperren", List.of(eintrag("konto", "17")))))
          .containsExactly(Fehlercode.MINDESTENS_EINS);
      assertThat(pruefe(GATTUNG, "Freigeben", List.of(eintrag("konto", "17")))).isEmpty();
    }

    @Test
    void pflicht_nur_bei_der_eigenen_aktion() {
      assertThat(codes(pruefe(GATTUNG, "Umbenennen", List.of(eintrag("konto", "17")))))
          .containsExactly(Fehlercode.PFLICHT_FEHLT);
      assertThat(
              codes(
                  pruefe(
                      GATTUNG, "Freigeben", List.of(eintrag("konto", "17", "neuername", "Neu")))))
          .containsExactly(Fehlercode.FELD_NICHT_ERLAUBT);
    }

    @Test
    void eigene_hoechstzahl() {
      assertThat(codes(pruefe(GATTUNG, "Freigeben", konten(4))))
          .containsExactly(Fehlercode.ZU_VIELE_EINTRAEGE);
      assertThat(pruefe(GATTUNG, "Freigeben", konten(3))).isEmpty();
    }
  }

  // ─── Hilfen ────────────────────────────────────────────────────────────────────────────────

  static List<Prueffehler> pruefe(
      TypDefinition definition, String aktion, List<Map<String, String>> eintraege) {
    return AuftragsPruefung.pruefe(definition, aktion, eintraege);
  }

  static List<Fehlercode> codes(List<Prueffehler> fehler) {
    return fehler.stream().map(Prueffehler::code).toList();
  }

  /** Ein veraenderbarer Eintrag aus Paaren Feldname, Wert; {@code null}-Werte erlaubt. */
  static Map<String, String> eintrag(String... paare) {
    Map<String, String> eintrag = new HashMap<>();
    for (int i = 0; i < paare.length; i += 2) {
      eintrag.put(paare[i], paare[i + 1]);
    }
    return eintrag;
  }

  static Map<String, String> schluessel(String werk, String abladestelle) {
    return eintrag("werk", werk, "abladestelle", abladestelle);
  }

  /** {@code anzahl} verschiedene Schluessel fuer eine Loeschung. */
  static List<Map<String, String>> schluessel(int anzahl) {
    List<Map<String, String>> liste = new ArrayList<>();
    for (int i = 0; i < anzahl; i++) {
      liste.add(schluessel("W" + i, "A"));
    }
    return liste;
  }

  private static List<Map<String, String>> konten(int anzahl) {
    List<Map<String, String>> liste = new ArrayList<>();
    for (int i = 0; i < anzahl; i++) {
      liste.add(eintrag("konto", String.valueOf(i)));
    }
    return liste;
  }
}
