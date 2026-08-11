package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Die Verkettung gegen die <b>Testkopie</b>: echte Wurzeln, echte Merge-Ergebnisse, echte Breite.
 *
 * <p><b>Keine fest eingetragene {@code MessageID}.</b> Die Bezugszeilen werden nach ihrer
 * <b>Gestalt</b> gesucht — „die breiteste Wurzel im Fenster", „ein Merge-Eingang" —, nicht nach
 * ihrer Kennung. Eine eingetragene Kennung waere beim naechsten Befuellen der Testkopie ein rot
 * gewordener Test, der nichts ueber den Code aussagt.
 *
 * <p>Mandant ist {@code NEXANS}: Er traegt praktisch das gesamte Kettenaufkommen. {@code SUTTONS}
 * hat 639 Wurzeln mit hoechstens zehn Kindern, {@code VOTG} und {@code WOC} haben ueberhaupt keine
 * Kettenzeile (M28‑1) — an ihnen waere der Breitfall nicht pruefbar.
 *
 * <p><b>Das Zeitfenster der Suche ist absolut</b> (29.–31.12.2025, Fenster A plus Ueberhang).
 * Kinder entstehen <i>nach</i> ihrem Elternteil; ohne den Ueberhang fehlten die der letzten
 * Stunden. Ein relatives Fenster hinge am Datenstand der Testkopie.
 */
class KettenDbIT extends SicherheitsTestbasis {

  private static final String MANDANT = "NEXANS";
  private static final String NUTZER = PRAEFIX + "kette-nexans";
  private static final String PASSWORT = "einLangesPasswort1";

  private static final LocalDateTime SUCHE_VON = LocalDateTime.parse("2025-12-29T00:00:00");
  private static final LocalDateTime SUCHE_BIS = LocalDateTime.parse("2025-12-31T00:00:00");

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung sitzung;

  @BeforeEach
  void anmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(MANDANT)).isTrue();
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT);
    sitzung = anmelden(NUTZER, PASSWORT);
  }

  // ─── Bezugszeilen nach Gestalt ─────────────────────────────────────────────────

  /**
   * Der Ausschnitt des Mandanten — dieselbe Kette wie im Anwendungscode.
   *
   * <p><b>Ohne ihn suchte der Test Bezugszeilen im ganzen Bestand</b> und fragte sie anschliessend
   * als {@code NEXANS}-Nutzer ab: Die Antwort waere korrekterweise {@code 404}, und der Test
   * scheiterte an seiner eigenen Auswahl statt an einem Fehler im Code.
   */
  private Condition beimMandanten() {
    return DSL.exists(
        DSL.selectOne()
            .from(PROCESS)
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
            .where(PROCESS.PROCESSID.eq(MESSAGE.PROCESSID))
            .and(PROJECTMANDANT.MANDANTID.eq(MANDANT)));
  }

  private Condition imFenster() {
    return MESSAGE.MESSAGELASTUPDATE.ge(SUCHE_VON).and(MESSAGE.MESSAGELASTUPDATE.lt(SUCHE_BIS));
  }

  /** Die Kennung mit den meisten Verweisen auf sich, ueber eine der beiden Verkettungsspalten. */
  private Record2<String, Integer> breiteste(org.jooq.TableField<?, String> verweis) {
    return glassfishDsl
        .select(verweis, DSL.count())
        .from(MESSAGE)
        .where(imFenster())
        .and(verweis.isNotNull())
        .and(verweis.ne(""))
        .and(beimMandanten())
        .groupBy(verweis)
        .orderBy(DSL.count().desc())
        .limit(1)
        .fetchOne();
  }

  /** Eine Zeile ganz ohne Verkettung — keine der vier Spalten ist belegt. */
  private String ohneKette() {
    return glassfishDsl
        .select(MESSAGE.MESSAGEID)
        .from(MESSAGE)
        .where(imFenster())
        .and(MESSAGE.SOURCEMESSAGEID.isNull())
        .and(MESSAGE.TARGETMESSAGEID.isNull())
        .and(MESSAGE.SOURCE.isFalse())
        .and(MESSAGE.TARGET.isFalse())
        .and(beimMandanten())
        .limit(1)
        .fetchOne(MESSAGE.MESSAGEID);
  }

  /** Eine Zeile, die ueber die genannte Spalte auf etwas zeigt. */
  private String eineZeileMit(org.jooq.TableField<?, String> verweis) {
    return glassfishDsl
        .select(MESSAGE.MESSAGEID)
        .from(MESSAGE)
        .where(imFenster())
        .and(verweis.isNotNull())
        .and(verweis.ne(""))
        .and(beimMandanten())
        .limit(1)
        .fetchOne(MESSAGE.MESSAGEID);
  }

  private Antwort kette(String messageId) throws IOException, InterruptedException {
    Antwort antwort = sitzung.hole("/api/nachrichten/" + messageId + "/kette");
    assertThat(antwort.status()).as("Rumpf: %s", antwort.rumpf()).isEqualTo(200);
    return antwort;
  }

  // ─── Aufstieg ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Ein Split-Kind sieht seinen Elternteil auf Ebene -1")
  void split_kind_sieht_seinen_elternteil() throws Exception {
    String kind = eineZeileMit(MESSAGE.SOURCEMESSAGEID);
    assertThat(kind).as("ohne Split-Kind im Fenster bewiese der Test nichts").isNotNull();

    Antwort kette = kette(kind);

    assertThat(kette.<List<String>>json("$.rollen")).contains("SPLIT_KIND");
    assertThat(kette.<List<String>>json("$.aufwaerts[*].messageId")).isNotEmpty();
    assertThat(kette.<List<Integer>>json("$.aufwaerts[*].ebene").getFirst()).isEqualTo(-1);
    assertThat(kette.<List<String>>json("$.aufwaerts[*].beziehung").getFirst())
        .isEqualTo("AUFTEILUNG");
    assertThat(kette.<List<String>>json("$.aufwaerts[*].rollen[*]"))
        .as("der Elternteil traegt Source = 1 — der Rueckwaertsindex der Aufteilung (E4)")
        .contains("SPLIT_WURZEL");
  }

  @Test
  @DisplayName("Ein Merge-Eingang sieht sein Ergebnis auf Ebene -1")
  void merge_eingang_sieht_sein_ergebnis() throws Exception {
    String eingang = eineZeileMit(MESSAGE.TARGETMESSAGEID);
    assertThat(eingang).isNotNull();

    Antwort kette = kette(eingang);

    assertThat(kette.<List<String>>json("$.rollen")).contains("MERGE_EINGANG");
    assertThat(kette.<List<String>>json("$.aufwaerts[*].beziehung").getFirst())
        .isEqualTo("ZUSAMMENFUEHRUNG");
    assertThat(kette.<List<String>>json("$.aufwaerts[*].rollen[*]"))
        .as("das Ergebnis traegt Target = 1 (E4)")
        .contains("MERGE_ERGEBNIS");
  }

  /**
   * In der Testkopie ist die tiefste Kette vier Glieder lang und es gibt keinen Zyklus (M30‑3).
   * Beide Grenzen duerfen hier also nie ansprechen — spricht doch eine an, hat sich die Quelle
   * geaendert und nicht der Code.
   */
  @Test
  @DisplayName("Weder Tiefengrenze noch Zyklusschutz sprechen in der Testkopie an")
  void keine_grenze_spricht_an() throws Exception {
    for (String kennung :
        List.of(eineZeileMit(MESSAGE.SOURCEMESSAGEID), eineZeileMit(MESSAGE.TARGETMESSAGEID))) {
      Antwort kette = kette(kennung);
      assertThat(kette.<Boolean>json("$.tiefeErreicht")).isFalse();
      assertThat(kette.<Boolean>json("$.zyklusErkannt")).isFalse();
    }
  }

  // ─── Abstieg und Breite ────────────────────────────────────────────────────────

  /**
   * <b>Der Breitenfall.</b> An der breitesten Wurzel des Fensters greift die Grenze von 50, {@code
   * weitereVorhanden} steht, und {@code abwaertsGesamt} nennt die genaue Zahl.
   */
  @Test
  @DisplayName("An der breitesten Wurzel greift die Breitengrenze und nennt die genaue Zahl")
  void breiteste_wurzel_meldet_weitere() throws Exception {
    Record2<String, Integer> breiteste = breiteste(MESSAGE.SOURCEMESSAGEID);
    assertThat(breiteste).isNotNull();
    assertThat(breiteste.value2())
        .as("ohne eine Wurzel ueber der Grenze pruefte der Test die Grenze nicht")
        .isGreaterThan(KettenService.BREITE_GRENZE);

    Antwort kette = kette(breiteste.value1());

    assertThat(kette.<List<String>>json("$.abwaerts[*].messageId"))
        .hasSize(KettenService.BREITE_GRENZE);
    assertThat(kette.<Integer>json("$.abwaertsGesamt")).isEqualTo(breiteste.value2());
    assertThat(kette.<Boolean>json("$.weitereVorhanden")).isTrue();
    assertThat(kette.<List<Integer>>json("$.abwaerts[*].ebene"))
        .allSatisfy(ebene -> assertThat(ebene).isEqualTo(1));
    assertThat(kette.<List<String>>json("$.abwaerts[*].beziehung"))
        .allSatisfy(beziehung -> assertThat(beziehung).isEqualTo("AUFTEILUNG"));
  }

  /**
   * <b>Abnahmekriterium 8.</b> Die ueber den Cursor erreichbare Menge stimmt mit {@code
   * abwaertsGesamt} ueberein — beide gefiltert, beide ueber dieselbe Wurzel. Faellt eines von
   * beiden auseinander, nennt die Antwort eine Zahl, zu der sie keine Zeilen liefert.
   *
   * <p><b>Die Runde beginnt seit dem 11.08.2026 mit {@code abwaertsCursor}</b> und nicht mehr mit
   * einem cursorlosen Erstaufruf des Blaetter-Endpunkts. Damit prueft der Test genau den Weg, den
   * die Oberflaeche geht: erste Seite aus {@code /kette}, alle weiteren ueber den Cursor. Vorher
   * war die erste geblaetterte Seite inhaltsgleich mit der schon gezeigten — die Dopplung fiel hier
   * nicht auf, weil der Test die Kette gar nicht erst mitzaehlte.
   */
  @Test
  @DisplayName("abwaertsGesamt und die ueber abwaertsCursor erreichbare Menge stimmen ueberein")
  void cursor_erreicht_genau_die_genannte_menge() throws Exception {
    Record2<String, Integer> breiteste = breiteste(MESSAGE.SOURCEMESSAGEID);
    assertThat(breiteste.value2())
        .as("ohne eine Wurzel ueber der Grenze gaebe es nichts zu blaettern")
        .isGreaterThan(KettenService.BREITE_GRENZE);
    String wurzel = breiteste.value1();

    Antwort kette = kette(wurzel);
    int gesamt = kette.json("$.abwaertsGesamt");

    // Die erste Seite ist die der Kette selbst — kein zweiter Aufruf fuer dieselben Zeilen.
    Set<String> erreicht = new LinkedHashSet<>(kette.<List<String>>json("$.abwaerts[*].messageId"));
    assertThat(erreicht).hasSize(KettenService.BREITE_GRENZE);

    String cursor = kette.json("$.abwaertsCursor");
    assertThat(cursor)
        .as("ohne ihn muesste die Oberflaeche die erste Seite ein zweites Mal holen")
        .isNotBlank();

    int seiten = 1;
    while (cursor != null) {
      Antwort seite =
          sitzung.hole(
              "/api/nachrichten/"
                  + wurzel
                  + "/kette/abwaerts?cursor="
                  + URLEncoder.encode(cursor, StandardCharsets.UTF_8));
      assertThat(seite.status()).as("Rumpf: %s", seite.rumpf()).isEqualTo(200);

      List<String> kennungen = seite.json("$.items[*].messageId");
      int vorher = erreicht.size();
      erreicht.addAll(kennungen);
      assertThat(erreicht.size() - vorher)
          .as(
              "keine Seite liefert eine Kennung zweimal — auch die erste nicht, und genau das war"
                  + " vor dem abwaertsCursor anders")
          .isEqualTo(kennungen.size());

      cursor =
          Boolean.TRUE.equals(seite.<Boolean>json("$.hasMore")) ? seite.json("$.nextCursor") : null;
      seiten++;
      assertThat(seiten).as("der Cursor darf nicht im Kreis laufen").isLessThan(200);
    }

    assertThat(erreicht)
        .as("gefunden ueber %d Seiten; die Antwort nennt %d", seiten, gesamt)
        .hasSize(gesamt);
    assertThat(seiten)
        .as("bei mehr als 50 Gliedern braucht es mehr als eine Seite")
        .isGreaterThan(1);
  }

  /**
   * <b>Der Cursor steht genau dann, wenn es weitergeht</b> — geprueft ueber fuenf Zeilen
   * verschiedener Gestalt statt an einer ausgesuchten. Ein Cursor ohne naechste Seite behauptete,
   * es gaebe eine; eine naechste Seite ohne Cursor waere eine Schaltflaeche ins Leere.
   *
   * <p>Die eine Ausnahme aus M30‑6 — eine ganze Seite ohne {@code MessageLastUpdate} — kann hier
   * nicht auftreten: Gemessen sind {@code 0} von 3.341.519 Zeilen betroffen. Wird dieser Test doch
   * einmal rot, hat sich die Quelle geaendert und nicht der Code.
   */
  @Test
  @DisplayName("abwaertsCursor steht genau dann, wenn weitereVorhanden wahr ist")
  void cursor_genau_dann_wenn_es_weitergeht() throws Exception {
    List<String> kennungen =
        List.of(
            breiteste(MESSAGE.SOURCEMESSAGEID).value1(),
            breiteste(MESSAGE.TARGETMESSAGEID).value1(),
            eineZeileMit(MESSAGE.SOURCEMESSAGEID),
            eineZeileMit(MESSAGE.TARGETMESSAGEID),
            ohneKette());

    for (String kennung : kennungen) {
      Antwort kette = kette(kennung);
      assertThat(kette.rumpf())
          .as("das Feld ist immer da — fehlend hiesse „unbekannt\"")
          .contains("\"abwaertsCursor\"");

      if (Boolean.TRUE.equals(kette.<Boolean>json("$.weitereVorhanden"))) {
        assertThat(kette.<String>json("$.abwaertsCursor"))
            .as("es geht weiter, also gibt es eine Position dafuer")
            .isNotBlank();
      } else {
        assertThat(kette.<String>json("$.abwaertsCursor"))
            .as("ein Cursor ohne naechste Seite behauptet, es gaebe eine")
            .isNull();
      }
    }
  }

  /**
   * Beim Merge greift die Grenze <b>regelmaessig</b>: 11,38 Prozent der Merge-Ergebnisse haben mehr
   * als 50 Eingaenge, gegen 1,07 Prozent der Wurzeln (M30‑2).
   */
  @Test
  @DisplayName("Auch am breitesten Merge-Ergebnis stimmen Zahl und Zeilen zusammen")
  void breitestes_merge_ergebnis() throws Exception {
    Record2<String, Integer> breiteste = breiteste(MESSAGE.TARGETMESSAGEID);
    assertThat(breiteste).isNotNull();

    Antwort kette = kette(breiteste.value1());

    assertThat(kette.<Integer>json("$.abwaertsGesamt")).isEqualTo(breiteste.value2());
    assertThat(kette.<List<String>>json("$.abwaerts[*].beziehung"))
        .isNotEmpty()
        .allSatisfy(beziehung -> assertThat(beziehung).isEqualTo("ZUSAMMENFUEHRUNG"));
    assertThat(kette.<List<String>>json("$.rollen")).contains("MERGE_ERGEBNIS");
  }

  // ─── Die Antwortgestalt ────────────────────────────────────────────────────────

  @Test
  @DisplayName("rollen ist immer vorhanden — leer statt fehlend")
  void rollen_sind_immer_vorhanden() throws Exception {
    String ohneKette = ohneKette();
    assertThat(ohneKette).isNotNull();

    Antwort kette = kette(ohneKette);

    assertThat(kette.hatFeld("$.rollen")).isTrue();
    assertThat(kette.<List<String>>json("$.rollen")).isEmpty();
    assertThat(kette.<List<String>>json("$.aufwaerts[*].messageId")).isEmpty();
    assertThat(kette.<List<String>>json("$.abwaerts[*].messageId")).isEmpty();
    assertThat(kette.<Integer>json("$.abwaertsGesamt")).isZero();
    assertThat(kette.<Boolean>json("$.weitereVorhanden")).isFalse();
  }

  @Test
  @DisplayName("Jedes Glied traegt Status, Einordnung, Zeitpunkt in UTC und seine Rollen")
  void jedes_glied_ist_vollstaendig() throws Exception {
    Antwort kette = kette(breiteste(MESSAGE.SOURCEMESSAGEID).value1());

    assertThat(kette.<List<String>>json("$.abwaerts[*].status"))
        .isNotEmpty()
        .allSatisfy(status -> assertThat(status).isNotBlank());
    assertThat(kette.<List<String>>json("$.abwaerts[*].statusKind"))
        .allSatisfy(kind -> assertThat(kind).isNotBlank());
    assertThat(kette.<List<String>>json("$.abwaerts[*].zeitpunkt"))
        .allSatisfy(zeitpunkt -> assertThat(zeitpunkt).endsWith("Z"));
  }

  /**
   * <b>Die uebrigen Endpunkte laufen weiter.</b> Spring loest ueber alle Controller hinweg auf —
   * {@code /api/nachrichten}, {@code /api/nachrichten/&#123;messageId&#125;} und {@code
   * /api/nachrichten/&#123;messageId&#125;/kette} bestehen damit nebeneinander. Das ist kein
   * Zufall, auf den man hofft, sondern etwas, das festgehalten gehoert, damit es bei einer
   * Umstellung nicht still kippt.
   *
   * <p>Bis zum 11.08.2026 trug dieser Test zusaetzlich {@code /api/nachrichten/merkmale} — den
   * schaerferen Fall, weil dort ein woertliches Segment an derselben Stelle stand wie die
   * Pfadvariable. Der Endpunkt ist entfallen (docs/nachrichtenliste.md §5).
   *
   * <p><b>Der umbenannte Blaetter-Pfad ist seit dem 11.08.2026 mit drin</b> — die Form des Pfades
   * aendert sich nicht, aber der Test ist billig und haelt fest, dass Spring weiterhin das
   * woertliche Segment vor der Pfadvariablen bevorzugt. Dass der alte Pfad <b>weg</b> ist und nicht
   * als Weiche gehalten wird, steht nicht hier: Er soll im Quellverzeichnis nirgends mehr
   * vorkommen, auch nicht als Zeichenkette in einem Test. Geprueft wird er bei der Abnahme.
   */
  @Test
  @DisplayName("Liste, Detail und der umbenannte Blaetter-Pfad bleiben erreichbar")
  void bestehende_endpunkte_bleiben_erreichbar() throws Exception {
    String kennung = eineZeileMit(MESSAGE.SOURCEMESSAGEID);

    assertThat(sitzung.hole("/api/nachrichten?limit=1").status()).isEqualTo(200);
    assertThat(sitzung.hole("/api/nachrichten/" + kennung).status()).isEqualTo(200);
    assertThat(sitzung.hole("/api/nachrichten/" + kennung + "/eigenschaften").status())
        .isEqualTo(200);
    assertThat(sitzung.hole("/api/nachrichten/" + kennung + "/kette/abwaerts").status())
        .isEqualTo(200);
  }

  /** Die Antwort traegt kein Feld, das den Umfang eines Detailaufrufs hat. */
  @Test
  @DisplayName("Ein Kettenglied traegt keine Schrittfolge und keine Eigenschaften")
  void ein_glied_ist_nicht_das_detail() throws Exception {
    Antwort kette = kette(breiteste(MESSAGE.SOURCEMESSAGEID).value1());

    assertThat(kette.rumpf())
        .as("wer ein Glied ansehen will, oeffnet es — dafuer gibt es den Detail-Endpunkt")
        .doesNotContain("\"schritte\"")
        .doesNotContain("\"kuratierteEigenschaften\"")
        .doesNotContain("\"eigenschaftenAnzahl\"");
  }
}
