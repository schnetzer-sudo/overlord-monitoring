package de.kraftwerkone.overlord.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.config.DatabaseProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Haelt den Indexbestand beider Schemata gegen die Sollliste {@code
 * src/test/resources/indizes-sollliste.txt} — gegen {@code information_schema.STATISTICS} der
 * <b>jeweils verbundenen</b> Datenbank. Lokal ist das die Testkopie, im Betrieb die Produktion.
 *
 * <p><b>Warum es diesen Test gibt.</b> {@code PROJEKTBESCHREIBUNG.md} §3.2 fuehrte achtzehn Tage
 * lang drei Indizes auf {@code Message}, waehrend acht existierten — darunter zwei eigenstaendige
 * auf {@code ProcessID}. Die Messung lag seit M1 vom 01.08.2026 vor; die verbindliche Datei hat sie
 * nie uebernommen. Die falsche Liste ist am 20.08.2026 in ein Abbruchkriterium eines
 * Arbeitsauftrags eingegangen.
 *
 * <p><b>Und deshalb feuert er in beide Richtungen.</b> In der Datenbank fehlte damals nichts — es
 * fehlten fuenf Eintraege in der Dokumentation. Ein Test, der nur prueft, ob ein dokumentierter
 * Index existiert, waere die ganzen achtzehn Tage gruen geblieben und damit wertlos. Die zweite
 * Richtung ({@link #undokumentierter_index_faellt_auf()}) ist der eigentliche Gegenstand.
 *
 * <p><b>Ein Befund ist keine Aufgabe.</b> Schlaegt der Test an, wird zuerst geklaert, wer sich
 * geaendert hat: die Datenbank oder die Sollliste. Es wird kein Index angelegt, geaendert oder
 * geloescht, um einen Lauf gruen zu bekommen — auf {@code GlassfishDB} ohnehin nicht (Regel S1).
 *
 * <p>{@code @Tag("db")}: Die CI erreicht das interne Netz nicht und schliesst die Gruppe ueber
 * {@code -DexcludedGroups=db} aus.
 *
 * <p>Der Gegenstand ist ausschliesslich der <b>Index</b>. Fuer die Spalten gibt es den Abgleich in
 * {@link DatenzugriffDbIT}; Fremdschluessel und Sortierungen sind hier nicht Gegenstand.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class IndexbestandDbIT {

  /** Liegt im Testklassenpfad, also unter {@code src/test/resources}. */
  private static final String DATEI = "/indizes-sollliste.txt";

  /** Die logischen Schemanamen der Sollliste. Die echten kommen aus {@link DatabaseProperties}. */
  private static final String GLASSFISH = "glassfish";

  private static final String MONITOR = "monitor";

  private static final Field<String> TABELLE = DSL.field("TABLE_NAME", String.class);
  private static final Field<String> INDEXNAME = DSL.field("INDEX_NAME", String.class);
  private static final Field<String> SPALTE = DSL.field("COLUMN_NAME", String.class);
  private static final Field<Integer> POSITION = DSL.field("SEQ_IN_INDEX", Integer.class);
  private static final Field<Integer> NICHT_EINDEUTIG = DSL.field("NON_UNIQUE", Integer.class);
  private static final Field<String> SCHEMA = DSL.field("TABLE_SCHEMA", String.class);
  private static final Field<String> TABELLENART = DSL.field("TABLE_TYPE", String.class);

  /** Einmal gelesen: Die Datei aendert sich waehrend eines Laufs nicht. */
  private static final Sollliste SOLL = lesen();

  /**
   * Der <b>Lese</b>-Kontext. Er darf beide Schemata lesen, und {@code information_schema} liegt auf
   * derselben Instanz — eine Verbindung genuegt fuer beide Erhebungen.
   */
  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private DatabaseProperties props;

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Die beiden Richtungen
  // ───────────────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Richtung 1: jeder Eintrag der Sollliste existiert in der Datenbank")
  void fehlender_index_faellt_auf() {
    Map<String, Indexeintrag> ist = istBestand();

    List<String> befunde = new ArrayList<>();
    for (Indexeintrag soll : SOLL.indizes().values()) {
      if (!ist.containsKey(soll.schluessel())) {
        befunde.add(soll.befund("FEHLT in der Datenbank, steht aber in der Sollliste"));
      }
    }

    assertThat(befunde)
        .as(
            "Richtung Sollliste -> Datenbank. Ein Index verschwindet nicht von selbst: Entweder ist"
                + " er geloescht oder umbenannt worden, oder die verbundene Datenbank ist nicht"
                + " die, die hier erwartet wird. Erst klaeren, dann die Sollliste anfassen — und"
                + " keinen Index anlegen, um den Lauf gruen zu bekommen.")
        .isEmpty();
  }

  @Test
  @DisplayName("Richtung 2: kein Index der Datenbank fehlt in der Sollliste")
  void undokumentierter_index_faellt_auf() {
    List<String> befunde = new ArrayList<>();
    for (Indexeintrag ist : istBestand().values()) {
      if (!SOLL.indizes().containsKey(ist.schluessel())) {
        befunde.add(ist.befund("existiert in der Datenbank, FEHLT in der Sollliste"));
      }
    }

    assertThat(befunde)
        .as(
            "Richtung Datenbank -> Sollliste. Das ist der Fall, wegen dem es diesen Test gibt: Am"
                + " 20.08.2026 fehlte nichts in der Datenbank, es fehlten fuenf Eintraege in der"
                + " Dokumentation — achtzehn Tage lang, und die falsche Liste ist in einen"
                + " Arbeitsauftrag eingegangen. Den Eintrag in indizes-sollliste.txt nachtragen,"
                + " den Namen dabei woertlich uebernehmen, und pruefen, ob eine der beiden"
                + " Indexlisten in docs/ dieselbe Luecke hat.")
        .isEmpty();
  }

  @Test
  @DisplayName("Gestalt: Spalten, ihre Reihenfolge und die Eindeutigkeit stimmen ueberein")
  void abweichende_gestalt_faellt_auf() {
    Map<String, Indexeintrag> ist = istBestand();

    List<String> befunde = new ArrayList<>();
    for (Indexeintrag soll : SOLL.indizes().values()) {
      Indexeintrag gefunden = ist.get(soll.schluessel());
      if (gefunden != null && !gefunden.equals(soll)) {
        befunde.add(
            soll.schema()
                + " / Tabelle "
                + soll.tabelle()
                + " / Index "
                + soll.name()
                + " — WEICHT AB. Sollliste: "
                + soll.gestalt()
                + " · Datenbank: "
                + gefunden.gestalt());
      }
    }

    assertThat(befunde)
        .as(
            "Der Index heisst noch so, ist aber ein anderer geworden. Die Reihenfolge der Spalten"
                + " ist dabei kein Beiwerk — an ihr haengt, welchen Zugriffspfad der Optimierer"
                + " waehlen kann (Regel L7).")
        .isEmpty();
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Damit die Gegenrichtung nicht ausgehebelt werden kann
  // ───────────────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Das eigene Schema ist vollstaendig gefuehrt — jede Tabelle, die Flyway anlegt")
  void tabellenliste_des_eigenen_schemas_ist_vollstaendig() {
    Set<String> gefuehrt = bewacht(MONITOR);
    Set<String> vorhanden = basistabellen(props.monitorSchema());

    List<String> befunde = new ArrayList<>();
    for (String tabelle : vorhanden) {
      if (!gefuehrt.contains(tabelle)) {
        befunde.add(
            "monitor / Tabelle " + tabelle + " — existiert im Schema, FEHLT in der Sollliste");
      }
    }
    for (String tabelle : gefuehrt) {
      if (!vorhanden.contains(tabelle)) {
        befunde.add("monitor / Tabelle " + tabelle + " — steht in der Sollliste, FEHLT im Schema");
      }
    }

    assertThat(befunde)
        .as(
            "overlord_monitor gehoert uns; was Flyway darin anlegt, gehoert bewacht. Ohne diese"
                + " Pruefung entkaeme eine neue Migration der Gegenrichtung schon dadurch, dass"
                + " ihre Tabelle in der Sollliste nicht vorkommt. Fuer glassfish gilt das"
                + " ausdruecklich nicht: Das Quellschema gehoert uns nicht, dort sind nur die"
                + " Tabellen gefuehrt, die in Anwendungscode vorkommen.")
        .isEmpty();
  }

  @Test
  @DisplayName("Die Sollliste ist in sich stimmig: jeder Index gehoert zu einer gefuehrten Tabelle")
  void sollliste_ist_in_sich_stimmig() {
    List<String> befunde = new ArrayList<>();
    for (Indexeintrag eintrag : SOLL.indizes().values()) {
      if (!bewacht(eintrag.schema()).contains(eintrag.tabelle())) {
        befunde.add(
            eintrag.befund(
                "steht in der Sollliste, aber seine Tabelle hat dort keine tabelle-Zeile"));
      }
    }

    assertThat(befunde)
        .as(
            "Ein Index ohne tabelle-Zeile ist ein blinder Fleck: Richtung 2 prueft nur Tabellen,"
                + " die gefuehrt sind. Ohne diese Pruefung liesse sich die Gegenrichtung"
                + " abschalten, indem man eine einzige Zeile loescht.")
        .isEmpty();
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Erhebung aus information_schema
  // ───────────────────────────────────────────────────────────────────────────────────────────

  /**
   * Der Ist-Bestand beider Schemata, eingeschraenkt auf die in der Sollliste gefuehrten Tabellen.
   */
  private Map<String, Indexeintrag> istBestand() {
    Map<String, Indexeintrag> alle = new LinkedHashMap<>();
    alle.putAll(ausDatenbank(GLASSFISH, props.glassfishSchema()));
    alle.putAll(ausDatenbank(MONITOR, props.monitorSchema()));
    return alle;
  }

  /**
   * Liest {@code information_schema.STATISTICS} eines Schemas und fasst die Zeilen je Index
   * zusammen. Eine Zeile je Spalte; {@code SEQ_IN_INDEX} gibt die Reihenfolge vor, und genau die
   * ist der Gegenstand — deshalb wird danach sortiert und nicht nach Spaltennamen.
   */
  private Map<String, Indexeintrag> ausDatenbank(String logischesSchema, String schemaName) {
    Set<String> bewacht = bewacht(logischesSchema);
    Map<String, Indexeintrag> gesammelt = new LinkedHashMap<>();

    for (Record zeile :
        glassfishDsl
            .select(TABELLE, INDEXNAME, SPALTE, NICHT_EINDEUTIG)
            .from("information_schema.STATISTICS")
            .where(SCHEMA.eq(schemaName))
            .orderBy(TABELLE, INDEXNAME, POSITION)
            .fetch()) {

      String tabelle = zeile.get(TABELLE);
      if (!bewacht.contains(tabelle)) {
        continue;
      }
      String name = zeile.get(INDEXNAME);
      String spalte = zeile.get(SPALTE);
      boolean eindeutig = zeile.get(NICHT_EINDEUTIG) == 0;

      String schluessel = Indexeintrag.schluessel(logischesSchema, tabelle, name);
      Indexeintrag bisher = gesammelt.get(schluessel);
      gesammelt.put(
          schluessel,
          bisher == null
              ? new Indexeintrag(logischesSchema, tabelle, name, spalte, eindeutig)
              : new Indexeintrag(
                  logischesSchema, tabelle, name, bisher.spalten() + "," + spalte, eindeutig));
    }
    return gesammelt;
  }

  /** Die Basistabellen eines Schemas. Views tragen keine Indizes und bleiben aussen vor. */
  private Set<String> basistabellen(String schemaName) {
    return new TreeSet<>(
        glassfishDsl
            .select(TABELLE)
            .from("information_schema.TABLES")
            .where(SCHEMA.eq(schemaName))
            .and(TABELLENART.eq("BASE TABLE"))
            .fetch(TABELLE));
  }

  private static Set<String> bewacht(String logischesSchema) {
    return SOLL.tabellen().getOrDefault(logischesSchema, Set.of());
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Die Sollliste
  // ───────────────────────────────────────────────────────────────────────────────────────────

  /**
   * Ein Index — gleich ob aus der Sollliste gelesen oder aus {@code information_schema} erhoben.
   * Beide Seiten benutzen denselben Typ, damit der Vergleich {@code equals} ist und nicht eine
   * Aneinanderreihung von Einzelvergleichen.
   *
   * @param spalten komma-getrennt in der Reihenfolge des Index
   */
  private record Indexeintrag(
      String schema, String tabelle, String name, String spalten, boolean eindeutig) {

    static String schluessel(String schema, String tabelle, String name) {
      return schema + "." + tabelle + "." + name;
    }

    String schluessel() {
      return schluessel(schema, tabelle, name);
    }

    String gestalt() {
      return "(" + spalten + "), " + (eindeutig ? "eindeutig" : "nicht eindeutig");
    }

    /** Tabelle, Indexname und Richtung der Abweichung — in einer Zeile, ohne zweiten Lauf. */
    String befund(String richtung) {
      return schema
          + " / Tabelle "
          + tabelle
          + " / Index "
          + name
          + " "
          + gestalt()
          + " — "
          + richtung;
    }
  }

  /**
   * @param tabellen logisches Schema auf die dort bewachten Tabellen
   * @param indizes Schluessel auf Eintrag
   */
  private record Sollliste(Map<String, Set<String>> tabellen, Map<String, Indexeintrag> indizes) {}

  private static Sollliste lesen() {
    Map<String, Set<String>> tabellen = new LinkedHashMap<>();
    Map<String, Indexeintrag> indizes = new LinkedHashMap<>();

    try (InputStream quelle = IndexbestandDbIT.class.getResourceAsStream(DATEI)) {
      if (quelle == null) {
        throw new IllegalStateException(
            "Die Sollliste " + DATEI + " liegt nicht im Testklassenpfad.");
      }
      try (BufferedReader leser =
          new BufferedReader(new InputStreamReader(quelle, StandardCharsets.UTF_8))) {
        String zeile;
        int nummer = 0;
        while ((zeile = leser.readLine()) != null) {
          nummer++;
          String satz = zeile.strip();
          if (satz.isEmpty() || satz.startsWith("#")) {
            continue;
          }
          String[] teile = satz.split(";", -1);
          switch (teile[0]) {
            case "tabelle" -> {
              pruefe(teile.length == 3, nummer, satz, "tabelle;<schema>;<Tabelle>");
              tabellen.computeIfAbsent(teile[1], s -> new TreeSet<>()).add(teile[2]);
            }
            case "index" -> {
              pruefe(
                  teile.length == 6,
                  nummer,
                  satz,
                  "index;<schema>;<Tabelle>;<Index>;<Spalten>;<ja|nein>");
              pruefe(
                  "ja".equals(teile[5]) || "nein".equals(teile[5]),
                  nummer,
                  satz,
                  "das letzte Feld ist ja oder nein");
              Indexeintrag eintrag =
                  new Indexeintrag(teile[1], teile[2], teile[3], teile[4], "ja".equals(teile[5]));
              pruefe(
                  indizes.put(eintrag.schluessel(), eintrag) == null,
                  nummer,
                  satz,
                  "dieser Index steht schon weiter oben — ein doppelter Eintrag deckt eine"
                      + " Abweichung zu");
            }
            default -> pruefe(false, nummer, satz, "die Zeile beginnt mit tabelle oder mit index");
          }
        }
      }
    } catch (IOException fehler) {
      throw new UncheckedIOException("Die Sollliste " + DATEI + " ist nicht lesbar.", fehler);
    }
    return new Sollliste(tabellen, indizes);
  }

  private static void pruefe(boolean bedingung, int nummer, String satz, String erwartet) {
    if (!bedingung) {
      throw new IllegalStateException(
          DATEI + ", Zeile " + nummer + ": \"" + satz + "\" — erwartet war: " + erwartet);
    }
  }
}
