package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.Baumfenster;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.LiveRestEntscheidung;
import de.kraftwerkone.overlord.monitor.common.LiveRestErgebnis;
import de.kraftwerkone.overlord.monitor.common.LiveRestKorrektur;
import de.kraftwerkone.overlord.monitor.common.LiveRestService;
import de.kraftwerkone.overlord.monitor.common.LiveRestZeile;
import de.kraftwerkone.overlord.monitor.common.LiveRestZustand;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Dienst aus den Rohzeilen macht — <b>ohne Datenbank</b>.
 *
 * <p>Die Statements prueft {@code ProzessbaumStatementsTest}, die Plaene {@code
 * ProzessbaumPlanDbIT}, die Trennung {@code ProzessbaumIsolationDbIT}. <b>Hier steht, was zwischen
 * Rohzeile und Antwort passiert:</b> die Schachtelung, die drei Zustaende, die Stilleschwelle, die
 * Summen und die Reihenfolge — seit dem 15.09.2026 fuer beide Gliederungen.
 *
 * <p><b>Alle Pruefwerte sind erfunden</b> (Regel G1) — der Test haengt an keiner Zahl aus dem
 * Bestand und an keinem Pflegestand (Regel T2). Er baut sich jede Zeile selbst.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProzessbaumServiceTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  /**
   * Der Stichtag. In dieser Zone entspricht {@code 2025-12-30T04:09:47Z} der Wanduhrzeit {@code
   * 2025-12-30T05:09:47} — die Zeitzone steht hier bewusst schief zur UTC-Angabe, damit ein
   * versehentliches Rechnen in UTC auffiele.
   */
  private static final Instant JETZT = Instant.parse("2025-12-30T04:09:47Z");

  private static final LocalDateTime WANDUHR = LocalDateTime.parse("2025-12-30T05:09:47");

  /**
   * Eine erfundene Projektbeschreibung fuer die Faelle, in denen das Projekt nichts entscheidet.
   */
  private static final String EIN_PROJEKT = "Ein erfundenes Projekt";

  @Mock private ProzessbaumRepository repository;

  /**
   * Der Live-Rest als Attrappe. <b>Ohne Stellung liefert er „ausgesetzt, kein Lauf"</b> — die
   * Faelle von vor dem 17.09.2026 sehen damit dieselben Zahlen wie damals; was die Verrechnung tut,
   * steht in {@link LiveRestVerrechnung}.
   */
  @Mock private LiveRestService liveRest;

  private ProzessbaumService service() {
    when(liveRest.ermittle(any(), any()))
        .thenReturn(LiveRestErgebnis.ohneKorrektur(LiveRestEntscheidung.ausgesetztOhneLauf()));
    return new ProzessbaumService(
        repository, new MessageStatusClassifier(), Clock.fixed(JETZT, ZONE), liveRest);
  }

  private void bestandMit(List<Prozessgeruestzeile> geruest, List<Prozesskennzahlzeile> zahlen) {
    when(repository.geruest(any())).thenReturn(geruest);
    when(repository.kennzahlen(any(), any())).thenReturn(zahlen);
  }

  /** Der Partnerbaum ohne Fenster — die Gliederung, die alle Faelle vor dem 15.09.2026 meinen. */
  private ProzessbaumResponse antwort() {
    return service().baum(MANDANT, null, Baumgliederung.PARTNER);
  }

  private ProzessbaumResponse projektbaum() {
    return service().baum(MANDANT, null, Baumgliederung.PROJEKT);
  }

  /** Eine gepflegte Zeile mit Partner und Richtung. */
  private static Prozessgeruestzeile gepflegt(
      String id, String name, String partner, String richtung, LocalDateTime letzteBewegung) {
    return new Prozessgeruestzeile(
        id, name, EIN_PROJEKT, partner, richtung, Pflegestatus.GEPFLEGT.name(), letzteBewegung);
  }

  /** Eine Zeile in einem Projekt — ohne Katalogzeile, denn die Projektgliederung braucht keine. */
  private static Prozessgeruestzeile imProjekt(String id, String name, String beschreibung) {
    return new Prozessgeruestzeile(id, name, beschreibung, null, null, null, WANDUHR);
  }

  private static Prozesskennzahlzeile zahl(String id, String status, long anzahl) {
    return new Prozesskennzahlzeile(id, status, anzahl);
  }

  // ─── Die rekursive Form lesen ─────────────────────────────────────────────────

  private static List<GruppenknotenResponse> gruppen(List<BaumknotenResponse> knoten) {
    assertThat(knoten).allSatisfy(k -> assertThat(k).isInstanceOf(GruppenknotenResponse.class));
    return knoten.stream().map(GruppenknotenResponse.class::cast).toList();
  }

  private static List<ProzessknotenResponse> blaetter(List<BaumknotenResponse> knoten) {
    assertThat(knoten).allSatisfy(k -> assertThat(k).isInstanceOf(ProzessknotenResponse.class));
    return knoten.stream().map(ProzessknotenResponse.class::cast).toList();
  }

  private List<GruppenknotenResponse> partner() {
    return gruppen(antwort().knoten());
  }

  private static List<GruppenknotenResponse> richtungen(GruppenknotenResponse partner) {
    return gruppen(partner.kinder());
  }

  private static List<ProzessknotenResponse> prozesse(GruppenknotenResponse gruppe) {
    return blaetter(gruppe.kinder());
  }

  private ProzessknotenResponse erstesBlatt() {
    return prozesse(richtungen(partner().getFirst()).getFirst()).getFirst();
  }

  /** Alle Blattkennungen, rekursiv — gleich in welcher Gliederung. */
  private static List<String> kennungen(List<BaumknotenResponse> knoten) {
    List<String> gefunden = new ArrayList<>();
    for (BaumknotenResponse einzeln : knoten) {
      switch (einzeln) {
        case GruppenknotenResponse gruppe -> gefunden.addAll(kennungen(gruppe.kinder()));
        case ProzessknotenResponse blatt -> gefunden.add(blatt.processId());
      }
    }
    return gefunden;
  }

  // ─── Die Schachtelung ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Partner → Richtung → Prozess")
  class Schachtelung {

    @Test
    @DisplayName("Unter einem Partner erscheinen nur die Richtungen, die vorkommen")
    void kein_leerer_ast() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      List<GruppenknotenResponse> partner = partner();

      assertThat(partner).hasSize(1);
      assertThat(richtungen(partner.getFirst())).hasSize(1);
      assertThat(richtungen(partner.getFirst()).getFirst().name()).isEqualTo("EINGEHEND");
      assertThat(prozesse(richtungen(partner.getFirst()).getFirst())).hasSize(2);
    }

    /**
     * Der Fall, der die Gliederung ueberhaupt zu einer Entscheidung macht: Partner ungepflegt ist
     * eine <b>eigene Gruppe auf oberster Ebene</b> und wird nicht auf die Partner verteilt.
     */
    @Test
    @DisplayName("Ohne Partner: eigene Gruppe ganz oben, am Ende")
    void ohne_partner_eigene_gruppe() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", null, "EINGEHEND", WANDUHR)),
          List.of());

      List<GruppenknotenResponse> partner = partner();

      assertThat(partner).hasSize(2);
      assertThat(partner.getFirst().name()).isEqualTo("ALPHA");
      assertThat(partner.getLast().name()).isNull();
      assertThat(partner.getLast().schluessel()).isNull();
      assertThat(partner.getLast().anzahlProzesse()).isEqualTo(1);
    }

    /**
     * Und der zweite, davon verschiedene Fall: Partner gepflegt, Richtung ungepflegt — eine eigene
     * Gruppe <b>unter diesem Partner</b>, neben eingehend und ausgehend.
     */
    @Test
    @DisplayName("Ohne Richtung: eigene Gruppe unter dem Partner, nicht ganz oben")
    void ohne_richtung_unter_dem_partner() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", null, WANDUHR)),
          List.of());

      List<GruppenknotenResponse> partner = partner();

      assertThat(partner).hasSize(1);
      assertThat(richtungen(partner.getFirst())).hasSize(2);
      assertThat(richtungen(partner.getFirst()).getFirst().name()).isEqualTo("EINGEHEND");
      assertThat(richtungen(partner.getFirst()).getLast().name()).isNull();
    }

    /**
     * <b>Entscheidung E-i.</b> Ein Regelvorschlag der Heuristik steht mit {@code OFFEN} in
     * derselben Spalte wie eine kuratierte Zuordnung. Gruppierte der Baum darueber, stuenden
     * Prozesse unter einem Partner, den niemand bestaetigt hat.
     */
    @Test
    @DisplayName("Ein offener Vorschlag ist keine Zuordnung")
    void offener_vorschlag_gilt_nicht() {
      bestandMit(
          List.of(
              new Prozessgeruestzeile(
                  "p1",
                  "A",
                  EIN_PROJEKT,
                  "VORSCHLAG",
                  "EINGEHEND",
                  Pflegestatus.OFFEN.name(),
                  WANDUHR)),
          List.of());

      List<GruppenknotenResponse> partner = partner();

      assertThat(partner).hasSize(1);
      assertThat(partner.getFirst().name()).isNull();
      assertThat(richtungen(partner.getFirst()).getFirst().name()).isNull();
    }

    /** „Gepflegt und leer" faellt fachlich mit „nicht zugeordnet" zusammen (E4). */
    @Test
    @DisplayName("Gepflegt mit leerem Feld zaehlt wie nicht zugeordnet")
    void gepflegt_und_leer() {
      bestandMit(List.of(gepflegt("p1", "A", "", "", WANDUHR)), List.of());

      assertThat(partner().getFirst().name()).isNull();
    }

    /**
     * <b>M117, als Zusicherung.</b> {@code process_catalog.partner} traegt {@code
     * utf8mb4_general_ci}: Fuer die Datenbank sind zwei Schreibweisen desselben Namens ein Wert.
     * Gruppierte der Baum ueber {@link String#equals}, zeigte er den Partner zweimal mit geteilten
     * Zahlen — und widerspraeche der Verteilung des Dashboards, die in SQL gruppiert.
     *
     * <p><b>Der Fall ist nicht erfunden</b>: Bei {@code NEXANS} steht genau ein Partner in zwei
     * Schreibweisen im Katalog (M117). Die Zahl steht nicht in diesem Test — er baut sich seinen
     * Fall selbst (Regel T2).
     */
    @Test
    @DisplayName("Zwei Schreibweisen desselben Partners sind ein Knoten")
    void schreibweisen_fallen_zusammen() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "AUDI", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "Audi", "EINGEHEND", WANDUHR)),
          List.of(zahl("p1", "FINISHED", 7), zahl("p2", "FINISHED", 5)));

      List<GruppenknotenResponse> partner = partner();

      assertThat(partner).hasSize(1);
      assertThat(partner.getFirst().anzahlProzesse()).isEqualTo(2);
      assertThat(partner.getFirst().nachrichten()).isEqualTo(12);
      // Angezeigt wird die zuerst angetroffene Schreibweise — der Katalog wird nicht umgeschrieben.
      assertThat(partner.getFirst().name()).isEqualTo("AUDI");
      assertThat(partner.getFirst().schluessel()).isEqualTo("AUDI");
    }

    /** Dasselbe eine Ebene tiefer: Die Richtung gruppiert nach derselben Gleichheit. */
    @Test
    @DisplayName("Zwei Schreibweisen derselben Richtung sind ein Knoten")
    void richtungsschreibweisen_fallen_zusammen() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", "eingehend", WANDUHR)),
          List.of());

      List<GruppenknotenResponse> richtungen = richtungen(partner().getFirst());

      assertThat(richtungen).hasSize(1);
      assertThat(richtungen.getFirst().anzahlProzesse()).isEqualTo(2);
      assertThat(richtungen.getFirst().name()).isEqualTo("EINGEHEND");
    }

    /** Ohne Katalogzeile — {@code SUTTONS} und {@code WOC} sind genau dieser Fall. */
    @Test
    @DisplayName("Ohne Katalogzeile faellt alles in die eine Gruppe")
    void ohne_katalogzeile() {
      bestandMit(
          List.of(imProjekt("p1", "A", EIN_PROJEKT), imProjekt("p2", "B", EIN_PROJEKT)), List.of());

      List<GruppenknotenResponse> partner = partner();

      assertThat(partner).hasSize(1);
      assertThat(partner.getFirst().name()).isNull();
      assertThat(partner.getFirst().anzahlProzesse()).isEqualTo(2);
    }
  }

  // ─── Die Reihenfolge ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Reihenfolge")
  class Reihenfolge {

    @Test
    @DisplayName("Partner alphabetisch, „nicht zugeordnet\" am Ende")
    void partner_alphabetisch() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ZULU", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", null, "EINGEHEND", WANDUHR),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(partner())
          .extracting(GruppenknotenResponse::name)
          .containsExactly("ALPHA", "ZULU", null);
    }

    /**
     * Die Reihenfolge ist die der Aufzaehlung {@link Richtung} und danach „nicht ermittelt" — ueber
     * alle Partner dieselbe. Eine Oberflaeche, die Symbole nach Position vergibt, bekaeme sonst
     * unter jedem Partner eine andere.
     */
    @Test
    @DisplayName("Richtungen: eingehend, ausgehend, dann „nicht ermittelt\"")
    void richtungen_in_der_reihenfolge_der_aufzaehlung() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", null, WANDUHR),
              gepflegt("p2", "B", "ALPHA", "AUSGEHEND", WANDUHR),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(richtungen(partner().getFirst()))
          .extracting(GruppenknotenResponse::name)
          .containsExactly("EINGEHEND", "AUSGEHEND", null);
    }

    /**
     * Ein gepflegter, aber unbekannter Richtungswert — die Spalte ist {@code varchar}, die
     * Whitelist steht im Code. Er faellt hinter die bekannten und <b>vor</b> „nicht ermittelt": Er
     * verschwindet nicht und wird nicht mit dem leeren Feld verwechselt.
     */
    @Test
    @DisplayName("Ein unbekannter Richtungswert steht hinter den bekannten, vor „nicht ermittelt\"")
    void unbekannte_richtung() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", null, WANDUHR),
              gepflegt("p2", "B", "ALPHA", "QUERGEHEND", WANDUHR),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(richtungen(partner().getFirst()))
          .extracting(GruppenknotenResponse::name)
          .containsExactly("EINGEHEND", "QUERGEHEND", null);
    }

    /**
     * Die Blaetter behalten die Reihenfolge der Abfrage — dort steht {@code ORDER BY ProcessName}.
     */
    @Test
    @DisplayName("Die Blaetter behalten die Reihenfolge der Abfrage")
    void blaetter_behalten_die_reihenfolge() {
      bestandMit(
          List.of(
              gepflegt("p1", "aaa", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "bbb", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p3", "ccc", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(prozesse(richtungen(partner().getFirst()).getFirst()))
          .extracting(ProzessknotenResponse::name)
          .containsExactly("aaa", "bbb", "ccc");
    }
  }

  // ─── Die zweite Gliederung ────────────────────────────────────────────────────

  /**
   * <b>Projekt → Prozess</b> (E-139 bis E-144, 15.09.2026). Dieselbe Schleife wie der Partnerbaum,
   * eine Ebene weniger — und kein Katalog.
   */
  @Nested
  @DisplayName("Projekt → Prozess")
  class Projektgliederung {

    @Test
    @DisplayName("Zwei Ebenen: Projekt und darunter die Prozesse, und die Antwort nennt beide")
    void zwei_ebenen() {
      bestandMit(
          List.of(imProjekt("p1", "A", "Eingang"), imProjekt("p2", "B", "Ausgang")), List.of());

      ProzessbaumResponse antwort = projektbaum();

      assertThat(antwort.gliederung()).isEqualTo(Baumgliederung.PROJEKT);
      assertThat(antwort.ebenen()).containsExactly(Baumebene.PROJEKT, Baumebene.PROZESS);
      List<GruppenknotenResponse> projekte = gruppen(antwort.knoten());
      assertThat(projekte).hasSize(2);
      for (GruppenknotenResponse projekt : projekte) {
        assertThat(blaetter(projekt.kinder())).hasSize(1);
      }
    }

    @Test
    @DisplayName("Jede Gruppe traegt die Summe ihrer Blaetter, die Kopfzahl die des ganzen Baums")
    void summen() {
      bestandMit(
          List.of(
              imProjekt("p1", "A", "Eingang"),
              imProjekt("p2", "B", "Eingang"),
              imProjekt("p3", "C", "Ausgang")),
          List.of(
              zahl("p1", "FINISHED", 10),
              zahl("p1", "ERROR_TIMEOUT", 1),
              zahl("p2", "FINISHED", 20),
              zahl("p3", "ERROR_TIMEOUT", 2)));

      ProzessbaumResponse antwort = projektbaum();
      List<GruppenknotenResponse> projekte = gruppen(antwort.knoten());
      GruppenknotenResponse eingang = projekte.getLast();

      assertThat(eingang.name()).isEqualTo("Eingang");
      assertThat(eingang.anzahlProzesse()).isEqualTo(2);
      assertThat(eingang.nachrichten()).isEqualTo(31);
      assertThat(eingang.fehler()).isEqualTo(1);
      assertThat(projekte.getFirst().nachrichten()).isEqualTo(2);
      assertThat(projekte.getFirst().fehler()).isEqualTo(2);

      assertThat(antwort.gesamt().nachrichten()).isEqualTo(33);
      assertThat(antwort.gesamt().fehler()).isEqualTo(3);
      assertThat(antwort.gesamt().anzahlProzesse()).isEqualTo(3);
    }

    /**
     * <b>Alphabetisch nach dem hochgestellten Schluessel</b> (E-142) — ein kleingeschriebener
     * Anfang steht nicht hinter allen grossgeschriebenen. Die Blaetter behalten die Reihenfolge der
     * Abfrage, wie im Partnerbaum.
     */
    @Test
    @DisplayName("Projekte alphabetisch, die Blaetter in der Reihenfolge der Abfrage")
    void reihenfolge() {
      bestandMit(
          List.of(
              imProjekt("p1", "aaa", "Zulu"),
              imProjekt("p2", "bbb", "alpha"),
              imProjekt("p3", "ccc", "Beta"),
              imProjekt("p4", "ddd", "alpha")),
          List.of());

      List<GruppenknotenResponse> projekte = gruppen(projektbaum().knoten());

      assertThat(projekte)
          .extracting(GruppenknotenResponse::name)
          .containsExactly("alpha", "Beta", "Zulu");
      assertThat(blaetter(projekte.getFirst().kinder()))
          .extracting(ProzessknotenResponse::name)
          .containsExactly("bbb", "ddd");
    }

    /**
     * <b>E-41 gilt auch hier</b> (E-141): {@code ProjectDescription} traegt {@code
     * utf8mb4_general_ci}, zwei Schreibweisen sind fuer die Datenbank ein Wert. Angezeigt wird die
     * zuerst angetroffene.
     */
    @Test
    @DisplayName("Zwei Schreibweisen derselben Beschreibung sind ein Knoten")
    void schreibweisen_fallen_zusammen() {
      bestandMit(
          List.of(
              imProjekt("p1", "A", "Eingehend von Kunden"),
              imProjekt("p2", "B", "EINGEHEND VON KUNDEN")),
          List.of(zahl("p1", "FINISHED", 4), zahl("p2", "FINISHED", 6)));

      List<GruppenknotenResponse> projekte = gruppen(projektbaum().knoten());

      assertThat(projekte).hasSize(1);
      assertThat(projekte.getFirst().name()).isEqualTo("Eingehend von Kunden");
      assertThat(projekte.getFirst().schluessel()).isEqualTo("EINGEHEND VON KUNDEN");
      assertThat(projekte.getFirst().anzahlProzesse()).isEqualTo(2);
      assertThat(projekte.getFirst().nachrichten()).isEqualTo(10);
    }

    /**
     * <b>Gruppiert wird ueber den Text und nicht ueber {@code ProjectID}</b> (E-141). Die
     * Geruestzeile traegt die Kennung des Projekts gar nicht — zwei Prozesse aus zwei Projekten mit
     * derselben Beschreibung sind hier zwei Zeilen mit demselben Text, und sie werden ein Knoten.
     * Nach Kennung gruppiert stuende dieselbe Beschriftung zweimal untereinander.
     */
    @Test
    @DisplayName("Mehrere Projekte mit derselben Beschreibung sind ein Knoten")
    void mehrere_projekte_ein_knoten() {
      bestandMit(
          List.of(
              imProjekt("aus-projekt-1", "A", "Rechnungen"),
              imProjekt("aus-projekt-2", "B", "Rechnungen"),
              imProjekt("aus-projekt-3", "C", "Lieferscheine")),
          List.of());

      List<GruppenknotenResponse> projekte = gruppen(projektbaum().knoten());

      assertThat(projekte)
          .extracting(GruppenknotenResponse::name)
          .containsExactly("Lieferscheine", "Rechnungen");
      assertThat(blaetter(projekte.getLast().kinder()))
          .extracting(ProzessknotenResponse::processId)
          .containsExactly("aus-projekt-1", "aus-projekt-2");
    }

    /**
     * <b>Die Projektgliederung haengt nicht am Katalog</b> — das ist ihr ganzer Anlass. Ein Prozess
     * ohne Katalogzeile, einer mit offenem Vorschlag und einer mit gepflegtem Partner stehen alle
     * unter ihrem Projekt; ein Knoten „nicht zugeordnet" entsteht nicht.
     */
    @Test
    @DisplayName("Kein Katalog noetig und kein Knoten „nicht zugeordnet\"")
    void unabhaengig_vom_katalog() {
      bestandMit(
          List.of(
              imProjekt("p1", "A", "Eingang"),
              new Prozessgeruestzeile(
                  "p2", "B", "Eingang", "VORSCHLAG", null, Pflegestatus.OFFEN.name(), WANDUHR),
              new Prozessgeruestzeile(
                  "p3",
                  "C",
                  "Eingang",
                  "ALPHA",
                  "EINGEHEND",
                  Pflegestatus.GEPFLEGT.name(),
                  WANDUHR)),
          List.of());

      List<GruppenknotenResponse> projekte = gruppen(projektbaum().knoten());

      assertThat(projekte).hasSize(1);
      assertThat(projekte.getFirst().name()).isEqualTo("Eingang");
      assertThat(projekte.getFirst().anzahlProzesse()).isEqualTo(3);
    }

    /**
     * <b>Keine Gliederung verliert einen Prozess</b>, und die Kopfzahl ist in beiden dieselbe —
     * dieselben Zeilen, anders geschachtelt.
     */
    @Test
    @DisplayName("Beide Gliederungen tragen dieselben Blaetter und dieselbe Kopfzahl")
    void beide_gliederungen_dieselben_blaetter() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", null, null, WANDUHR),
              imProjekt("p3", "C", "Anderes"),
              imProjekt("p4", "D", "Anderes")),
          List.of(zahl("p1", "FINISHED", 3), zahl("p4", "ERROR_X", 1)));

      ProzessbaumResponse partnerbaum = antwort();
      ProzessbaumResponse projektbaum = projektbaum();

      assertThat(kennungen(projektbaum.knoten()))
          .containsExactlyInAnyOrderElementsOf(kennungen(partnerbaum.knoten()))
          .hasSize(4);
      assertThat(projektbaum.gesamt()).isEqualTo(partnerbaum.gesamt());
    }

    @Test
    @DisplayName("Ohne Gliederung gibt es keinen Baum — der Dienst setzt keine Vorgabe ein")
    void ohne_gliederung_kein_baum() {
      bestandMit(List.of(), List.of());

      assertThatThrownBy(() -> service().baum(MANDANT, null, null))
          .isInstanceOf(NullPointerException.class);
    }
  }

  // ─── Die Antwortform ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Antwortform (E-140)")
  class Antwortform {

    @Test
    @DisplayName("Der Partnerbaum nennt drei Ebenen und seine Gliederung")
    void partnerbaum_nennt_seine_ebenen() {
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)), List.of());

      ProzessbaumResponse antwort = antwort();

      assertThat(antwort.gliederung()).isEqualTo(Baumgliederung.PARTNER);
      assertThat(antwort.ebenen())
          .containsExactly(Baumebene.PARTNER, Baumebene.RICHTUNG, Baumebene.PROZESS);
    }

    /**
     * Ein Blatt traegt denselben Schluessel wie jeder Knoten — die {@code ProcessID} —, und seinen
     * Klarnamen als {@code name}, nicht die Kennung.
     */
    @Test
    @DisplayName("Ein Blatt traegt die ProcessID als Schluessel und den ProcessName als Namen")
    void blatt_schluessel_und_name() {
      bestandMit(
          List.of(gepflegt("40000_ERFUNDEN", "Erfunden (VDA)", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      ProzessknotenResponse blatt = erstesBlatt();

      assertThat(blatt.schluessel()).isEqualTo("40000_ERFUNDEN");
      assertThat(blatt.processId()).isEqualTo("40000_ERFUNDEN");
      assertThat(blatt.name()).isEqualTo("Erfunden (VDA)");
    }
  }

  // ─── Die drei Zustaende ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die drei Zustaende")
  class Zustaende {

    @Test
    @DisplayName("Keine Rollupzeile heisst NIE und nicht STILL")
    void nie() {
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", null)), List.of());

      ProzessknotenResponse blatt = erstesBlatt();
      assertThat(blatt.zustand()).isEqualTo(Prozesszustand.NIE);
      assertThat(blatt.letzteBewegung()).isNull();
    }

    @Test
    @DisplayName("Aelter als die Schwelle heisst STILL")
    void still() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(4))), List.of());

      assertThat(erstesBlatt().zustand()).isEqualTo(Prozesszustand.STILL);
    }

    @Test
    @DisplayName("Juenger als die Schwelle heisst BEWEGT")
    void bewegt() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(2))), List.of());

      assertThat(erstesBlatt().zustand()).isEqualTo(Prozesszustand.BEWEGT);
    }

    /**
     * <b>Die Grenze liegt zugunsten von „bewegt".</b> Genau auf der Schwelle wird nicht markiert;
     * eine Sekunde davor schon. Die Grenze muss irgendwo liegen, und im Zweifel <b>nicht</b> zu
     * markieren haelt die Markierung aussagekraeftig.
     */
    @Test
    @DisplayName("Genau auf der Schwelle gilt ein Prozess als bewegt")
    void die_grenze_selbst() {
      LocalDateTime schwelle = WANDUHR.minus(ProzessbaumService.STILLE_SCHWELLE);

      assertThat(ProzessbaumService.zustand(schwelle, WANDUHR)).isEqualTo(Prozesszustand.BEWEGT);
      assertThat(ProzessbaumService.zustand(schwelle.minusSeconds(1), WANDUHR))
          .isEqualTo(Prozesszustand.STILL);
    }

    /**
     * <b>Der Zustand haengt nicht am Zeitfenster.</b> Derselbe Prozess, drei Fensterbreiten, ein
     * Zustand — das ist die Festlegung, und sie ist ohne diesen Test nur eine Absichtserklaerung.
     */
    @Test
    @DisplayName("Der Zustand ist ueber alle drei Zeitraeume derselbe")
    void zustand_ist_fensterunabhaengig() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(4))), List.of());

      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        ProzessbaumResponse antwort =
            service().baum(MANDANT, Baumfenster.paar(zeitraum), Baumgliederung.PARTNER);
        GruppenknotenResponse partner = gruppen(antwort.knoten()).getFirst();
        assertThat(prozesse(richtungen(partner).getFirst()).getFirst().zustand())
            .as("Paar %s", zeitraum.code())
            .isEqualTo(Prozesszustand.STILL);
      }
    }

    /** Die drei Zaehler sind disjunkt und vollstaendig: Ihre Summe ist die Prozesszahl. */
    @Test
    @DisplayName("bewegt + still + nie ergibt die Prozesszahl")
    void die_drei_zaehler_sind_vollstaendig() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(4)),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR.minusYears(2)),
              gepflegt("p4", "D", "ALPHA", "EINGEHEND", null)),
          List.of());

      BaumsummeResponse gesamt = antwort().gesamt();

      assertThat(gesamt.anzahlProzesse()).isEqualTo(4);
      assertThat(gesamt.bewegt()).isEqualTo(1);
      assertThat(gesamt.still()).isEqualTo(2);
      assertThat(gesamt.nie()).isEqualTo(1);
      assertThat(gesamt.bewegt() + gesamt.still() + gesamt.nie())
          .isEqualTo(gesamt.anzahlProzesse());
    }

    /** Die Schwelle steht in der Antwort, damit die Oberflaeche sie nicht zweitens kennen muss. */
    @Test
    @DisplayName("Die Stilleschwelle steht in der Antwort")
    void schwelle_steht_in_der_antwort() {
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)), List.of());

      assertThat(antwort().stilleSchwelleMonate())
          .isEqualTo((int) ProzessbaumService.STILLE_SCHWELLE.toTotalMonths());
    }
  }

  // ─── Die Kennzahlen ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Kennzahlen")
  class Kennzahlen {

    /**
     * <b>Die Einordnung wird gerufen, nicht nachgebaut.</b> {@code ERROR_TIMEOUT} und {@code
     * COMMIT_REJECTED} sind Fehler, {@code FINISHED} und {@code SUSPENDED} nicht — und {@code
     * ERROR_ETWAS_NEUES} ist einer, obwohl der Klassifizierer den Wert nicht kennt.
     */
    @Test
    @DisplayName("Fehler entstehen aus dem Rohwert, ueber den Klassifizierer")
    void fehler_aus_dem_rohwert() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(
              zahl("p1", "FINISHED", 100),
              zahl("p1", "SUSPENDED", 10),
              zahl("p1", "ERROR_TIMEOUT", 3),
              zahl("p1", "COMMIT_REJECTED", 2),
              zahl("p1", "ERROR_ETWAS_NEUES", 1)));

      ProzessknotenResponse blatt = erstesBlatt();

      assertThat(blatt.nachrichten()).isEqualTo(116);
      assertThat(blatt.fehler()).isEqualTo(6);
    }

    /** Ein Prozess ohne Zeile im Fenster steht trotzdem im Baum — mit Null. */
    @Test
    @DisplayName("Ohne Zeile im Fenster steht der Prozess mit Null da")
    void ohne_zeile_im_fenster() {
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)), List.of());

      ProzessknotenResponse blatt = erstesBlatt();

      assertThat(blatt.nachrichten()).isZero();
      assertThat(blatt.fehler()).isZero();
      assertThat(blatt.zustand()).isEqualTo(Prozesszustand.BEWEGT);
    }

    /**
     * Die Summen jeder Ebene sind die Summen ihrer Kinder — der Aufrufer soll nichts
     * zusammenrechnen muessen (Richtlinie §5.1).
     */
    @Test
    @DisplayName("Jede Ebene traegt die Summe ihrer Kinder")
    void summen_auf_jeder_ebene() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", "AUSGEHEND", WANDUHR),
              gepflegt("p3", "C", "BETA", "EINGEHEND", WANDUHR)),
          List.of(
              zahl("p1", "FINISHED", 10),
              zahl("p1", "ERROR_TIMEOUT", 1),
              zahl("p2", "FINISHED", 20),
              zahl("p3", "FINISHED", 5),
              zahl("p3", "ERROR_TIMEOUT", 2)));

      ProzessbaumResponse antwort = antwort();
      GruppenknotenResponse alpha = gruppen(antwort.knoten()).getFirst();

      assertThat(alpha.name()).isEqualTo("ALPHA");
      assertThat(alpha.anzahlProzesse()).isEqualTo(2);
      assertThat(alpha.nachrichten()).isEqualTo(31);
      assertThat(alpha.fehler()).isEqualTo(1);
      assertThat(richtungen(alpha).getFirst().nachrichten()).isEqualTo(11);
      assertThat(richtungen(alpha).getLast().nachrichten()).isEqualTo(20);

      assertThat(antwort.gesamt().nachrichten()).isEqualTo(38);
      assertThat(antwort.gesamt().fehler()).isEqualTo(3);
    }

    /**
     * Eine Kennzahlzeile zu einem Prozess, der nicht im Geruest steht, darf die Kopfzahl nicht
     * heben. Der Fall kann nur auftreten, wenn die Mandantenkette der beiden Statements
     * auseinanderliefe — und dann soll die Zahl <b>nicht</b> stillschweigend mitwachsen.
     */
    @Test
    @DisplayName("Eine Zeile ohne Prozess im Geruest zaehlt nirgends mit")
    void fremde_kennzahlzeile_zaehlt_nicht() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(zahl("p1", "FINISHED", 10), zahl("fremd", "FINISHED", 999)));

      assertThat(antwort().gesamt().nachrichten()).isEqualTo(10);
    }
  }

  // ─── Zeitraum und Fenster ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("Zeitraum und Fenster")
  class ZeitraumUndFenster {

    @Test
    @DisplayName("Ohne Parameter gilt die Vorgabe, und die Antwort nennt sie")
    void vorgabe_wird_genannt() {
      bestandMit(List.of(), List.of());

      assertThat(antwort().zeitraum()).isEqualTo(ProzessbaumService.VORGABE.code());
    }

    @Test
    @DisplayName("Mit Parameter steht das gewaehlte Paar in der Antwort")
    void gewaehltes_paar_wird_genannt() {
      bestandMit(List.of(), List.of());

      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        assertThat(
                service()
                    .baum(MANDANT, Baumfenster.paar(zeitraum), Baumgliederung.PARTNER)
                    .zeitraum())
            .isEqualTo(zeitraum.code());
      }
    }

    /**
     * Die Grenzen kommen aus {@code Rollupzeitraum.fenster} und werden in UTC uebersetzt — nicht in
     * der Zone des Servers, auf dem der Test laeuft.
     */
    @Test
    @DisplayName("Das Fenster steht in UTC in der Antwort")
    void fenster_in_utc() {
      bestandMit(List.of(), List.of());

      ZeitfensterResponse fenster =
          service()
              .baum(MANDANT, Baumfenster.paar(Rollupzeitraum.STUNDEN_48), Baumgliederung.PARTNER)
              .fenster();

      assertThat(fenster.von())
          .isEqualTo(LocalDateTime.parse("2025-12-28T06:00:00").atZone(ZONE).toInstant());
      assertThat(fenster.bis())
          .isEqualTo(LocalDateTime.parse("2025-12-30T06:00:00").atZone(ZONE).toInstant());
    }

    /**
     * <b>Ein Mandant ohne Prozesse ist ein leerer Baum und kein Fehler.</b> Die Antwort ist
     * vollstaendig, alle Zaehler stehen auf null — in beiden Gliederungen.
     */
    @Test
    @DisplayName("Ein Mandant ohne Prozesse bekommt einen leeren Baum")
    void leerer_baum() {
      bestandMit(List.of(), List.of());

      for (ProzessbaumResponse antwort : List.of(antwort(), projektbaum())) {
        assertThat(antwort.knoten()).isEmpty();
        assertThat(antwort.gesamt().anzahlProzesse()).isZero();
        assertThat(antwort.gesamt().nachrichten()).isZero();
        assertThat(antwort.fenster()).isNotNull();
      }
    }
  }

  // ─── Das freie Fenster ────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Das freie Fenster")
  class FreiesFenster {

    /**
     * Die Antwort nennt {@code FREI} und die Grenzen des Fensters — {@code bis} ausschliessend, in
     * UTC. Das Fenster ist absolut und haengt an keiner Uhr: Der Stichtag der Anwendungsuhr liegt
     * hier Monate hinter dem Fenster, und die Antwort traegt trotzdem das Fenster.
     */
    @Test
    @DisplayName("Die Antwort nennt FREI und das Fenster, bis ausschliessend, in UTC")
    void frei_in_der_antwort() {
      bestandMit(List.of(), List.of());
      Baumfenster fenster =
          Baumfenster.frei(
              LocalDateTime.parse("2026-03-10T14:00"), LocalDateTime.parse("2026-03-12T03:00"));

      ProzessbaumResponse antwort = service().baum(MANDANT, fenster, Baumgliederung.PARTNER);

      assertThat(antwort.zeitraum()).isEqualTo("FREI");
      // Winterzeit in Europe/Berlin: eine Stunde Versatz.
      assertThat(antwort.fenster().von()).isEqualTo(Instant.parse("2026-03-10T13:00:00Z"));
      assertThat(antwort.fenster().bis()).isEqualTo(Instant.parse("2026-03-12T02:00:00Z"));
    }

    /**
     * Der Dienst reicht die <b>Zerlegung</b> durch und rechnet nichts nach — das Repository bekommt
     * genau die Segmente, die {@code Baumfenster.zerlegung} liefert.
     */
    @Test
    @DisplayName("Das Repository bekommt die zerlegten Segmente, nichts anderes")
    void segmente_werden_durchgereicht() {
      bestandMit(List.of(), List.of());
      LocalDateTime von = LocalDateTime.parse("2025-03-10T14:00");
      LocalDateTime bis = LocalDateTime.parse("2025-03-12T03:00");

      service().baum(MANDANT, Baumfenster.frei(von, bis), Baumgliederung.PARTNER);

      org.mockito.Mockito.verify(repository)
          .kennzahlen(
              org.mockito.ArgumentMatchers.eq(MANDANT),
              org.mockito.ArgumentMatchers.eq(Baumfenster.zerlegung(von, bis)));
    }

    /** Die Kennzahlen eines freien Fensters werden wie die eines Paares verdichtet. */
    @Test
    @DisplayName("Die Kennzahlen werden je Prozess verdichtet, gleich woher die Segmente kommen")
    void kennzahlen_wie_bei_einem_paar() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(zahl("p1", "DONE", 5), zahl("p1", "ERROR_X", 2)));

      ProzessbaumResponse antwort =
          service()
              .baum(
                  MANDANT,
                  Baumfenster.frei(
                      LocalDateTime.parse("2025-03-10T14:00"),
                      LocalDateTime.parse("2025-03-12T03:00")),
                  Baumgliederung.PARTNER);

      assertThat(antwort.gesamt().nachrichten()).isEqualTo(7);
      assertThat(antwort.gesamt().fehler()).isEqualTo(2);
    }
  }

  // ─── Der Live-Rest ────────────────────────────────────────────────────────────

  /**
   * <b>Die Verrechnung des Live-Rests</b> ({@code docs/live-rest.md}) — die Korrektur kommt als
   * gestelltes Ergebnis herein, geprueft wird, was der Dienst daraus macht: nur die Stunden im
   * Fenster, die Einordnung ueber den Klassifizierer, kein negativer Endwert, die letzte Bewegung
   * als Maximum, der Block in der Antwort. Alle Zeitpunkte und Zahlen erfunden (T2).
   */
  @Nested
  @DisplayName("Der Live-Rest")
  class LiveRestVerrechnung {

    /** G: der Eimer des Laufs, in Wanduhrzeit — die Stunde vor der Stichtagsstunde. */
    private static final LocalDateTime G = LocalDateTime.parse("2025-12-30T04:00:00");

    private static final LocalDateTime NAECHSTE = G.plusHours(1);

    private LiveRestZeile zeile(LocalDateTime stunde, String prozess, String status, long anzahl) {
      return new LiveRestZeile(stunde, prozess, status, anzahl);
    }

    /** Ein angewandter Live-Rest ueber [G, G + 2 h) mit den gegebenen Korrekturzeilen. */
    private void liveRestMit(List<LiveRestZeile> korrektur, Map<String, LocalDateTime> juengste) {
      when(liveRest.ermittle(any(), any()))
          .thenReturn(
              new LiveRestErgebnis(
                  LiveRestEntscheidung.angewandt(G, G.plusHours(2)),
                  new LiveRestKorrektur(korrektur, juengste)));
    }

    private ProzessbaumService dienst() {
      ProzessbaumService dienst = service();
      // service() stellt die Attrappe auf „ausgesetzt"; die Faelle hier stellen sie danach um.
      return dienst;
    }

    /**
     * Der Fall, fuer den es die Korrektur gibt: Der Lauf sah die Nachricht als {@code RUNNING},
     * seither ist sie {@code ERROR_TIMEOUT}. Die Nachrichtenzahl bleibt, die Fehlerzahl steigt.
     */
    @Test
    @DisplayName(
        "Ein Statuswechsel innerhalb von G laesst die Nachrichtenzahl gleich und hebt die Fehlerzahl")
    void statuswechsel_innerhalb_von_g() {
      ProzessbaumService dienst = dienst();
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(zahl("p1", "FINISHED", 9), zahl("p1", "RUNNING", 1)));
      liveRestMit(
          List.of(zeile(G, "p1", "RUNNING", -1), zeile(G, "p1", "ERROR_TIMEOUT", 1)),
          Map.of("p1", G));

      ProzessbaumResponse antwort = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);
      ProzessknotenResponse blatt =
          prozesse(richtungen(gruppen(antwort.knoten()).getFirst()).getFirst()).getFirst();

      assertThat(blatt.nachrichten()).isEqualTo(10);
      assertThat(blatt.fehler()).isEqualTo(1);
      assertThat(antwort.gesamt().nachrichten()).isEqualTo(10);
      assertThat(antwort.gesamt().fehler()).isEqualTo(1);
      assertThat(antwort.liveRest().zustand()).isEqualTo(LiveRestZustand.ANGEWANDT);
      assertThat(antwort.liveRest().vollstaendigBis()).isNull();
    }

    /**
     * Nur die Stunden, die im Fenster liegen: Das 48-Stunden-Fenster am Stichtag reicht bis 06:00
     * ausschliessend; eine Zeile bei 06:00 gehoert nicht dazu, eine bei 05:00 schon.
     */
    @Test
    @DisplayName("Verrechnet wird nur, was im Fenster liegt")
    void nur_innerhalb_des_fensters() {
      ProzessbaumService dienst = dienst();
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(zahl("p1", "FINISHED", 5)));
      liveRestMit(
          List.of(
              zeile(NAECHSTE, "p1", "FINISHED", 3),
              zeile(LocalDateTime.parse("2025-12-30T06:00:00"), "p1", "FINISHED", 100)),
          Map.of());

      ProzessbaumResponse antwort = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);

      assertThat(antwort.gesamt().nachrichten()).isEqualTo(8);
    }

    /** Minus und plus kommen aus zwei Lesungen; laeuft der Bestand dazwischen weg, bleibt null. */
    @Test
    @DisplayName("Kein negativer Endwert")
    void kein_negativer_endwert() {
      ProzessbaumService dienst = dienst();
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(zahl("p1", "ERROR_TIMEOUT", 2)));
      liveRestMit(List.of(zeile(G, "p1", "ERROR_TIMEOUT", -5)), Map.of());

      ProzessbaumResponse antwort = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);
      ProzessknotenResponse blatt =
          prozesse(richtungen(gruppen(antwort.knoten()).getFirst()).getFirst()).getFirst();

      assertThat(blatt.nachrichten()).isZero();
      assertThat(blatt.fehler()).isZero();
      assertThat(antwort.gesamt().nachrichten()).isZero();
    }

    /**
     * Ein Prozess, den der Rollup noch nie gesehen hat und der in der laufenden Stunde Verkehr
     * traegt: Er steht mit seinen Live-Zahlen da, seine letzte Bewegung ist die Live-Stunde, und er
     * ist {@code BEWEGT} — nie „nie".
     */
    @Test
    @DisplayName("Nur Live-Verkehr, keine Rollupzeile: Zahlen, letzte Bewegung und BEWEGT")
    void nur_live_verkehr() {
      ProzessbaumService dienst = dienst();
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", null)), List.of());
      liveRestMit(
          List.of(zeile(NAECHSTE, "p1", "FINISHED", 3), zeile(NAECHSTE, "p1", "ERROR_X", 1)),
          Map.of("p1", NAECHSTE));

      ProzessbaumResponse antwort = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);
      ProzessknotenResponse blatt =
          prozesse(richtungen(gruppen(antwort.knoten()).getFirst()).getFirst()).getFirst();

      assertThat(blatt.nachrichten()).isEqualTo(4);
      assertThat(blatt.fehler()).isEqualTo(1);
      assertThat(blatt.zustand()).isEqualTo(Prozesszustand.BEWEGT);
      assertThat(blatt.letzteBewegung()).isEqualTo(NAECHSTE.atZone(ZONE).toInstant());
      assertThat(antwort.gesamt().bewegt()).isEqualTo(1);
      assertThat(antwort.gesamt().nie()).isZero();
    }

    /** Das Maximum aus E-34 und der juengsten Live-Stunde — in beide Richtungen. */
    @Test
    @DisplayName("Die letzte Bewegung ist das Maximum aus Rollup und Live-Stunde")
    void letzte_bewegung_ist_das_maximum() {
      ProzessbaumService dienst = dienst();
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", G.minusHours(5)),
              gepflegt("p2", "B", "ALPHA", "EINGEHEND", NAECHSTE)),
          List.of());
      liveRestMit(List.of(), Map.of("p1", NAECHSTE, "p2", G));

      ProzessbaumResponse antwort = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);
      List<ProzessknotenResponse> blaetter =
          prozesse(richtungen(gruppen(antwort.knoten()).getFirst()).getFirst());

      assertThat(blaetter.get(0).letzteBewegung()).isEqualTo(NAECHSTE.atZone(ZONE).toInstant());
      assertThat(blaetter.get(1).letzteBewegung()).isEqualTo(NAECHSTE.atZone(ZONE).toInstant());
    }

    /**
     * Ein stiller Prozess mit Verkehr in der laufenden Stunde ist nicht mehr still — unabhaengig
     * vom gewaehlten Fenster (E-35).
     */
    @Test
    @DisplayName(
        "Verkehr in der laufenden Stunde macht einen stillen Prozess bewegt, in jedem Fenster")
    void still_wird_bewegt() {
      ProzessbaumService dienst = dienst();
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(4))), List.of());
      liveRestMit(List.of(zeile(G, "p1", "FINISHED", 1)), Map.of("p1", G));

      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        ProzessbaumResponse antwort =
            dienst.baum(MANDANT, Baumfenster.paar(zeitraum), Baumgliederung.PARTNER);
        assertThat(
                prozesse(richtungen(gruppen(antwort.knoten()).getFirst()).getFirst())
                    .getFirst()
                    .zustand())
            .as("Paar %s", zeitraum.code())
            .isEqualTo(Prozesszustand.BEWEGT);
        assertThat(antwort.gesamt().still()).as("Paar %s", zeitraum.code()).isZero();
      }
    }

    /**
     * Das freie Fenster in drei Lagen zu G: ganz davor (keine Korrektur, aber die letzte Bewegung),
     * ueber G hinweg (nur die Stunden ab G) und ab G (alles).
     */
    @Test
    @DisplayName("Freies Fenster vor G, ueber G hinweg und ab G")
    void freies_fenster_in_drei_lagen() {
      ProzessbaumService dienst = dienst();
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", G.minusMonths(4))),
          List.of(zahl("p1", "FINISHED", 10)));
      liveRestMit(
          List.of(zeile(G, "p1", "FINISHED", 2), zeile(NAECHSTE, "p1", "FINISHED", 3)),
          Map.of("p1", NAECHSTE));

      ProzessbaumResponse vorG =
          dienst.baum(
              MANDANT, Baumfenster.frei(G.minusDays(2), G.minusDays(1)), Baumgliederung.PARTNER);
      ProzessbaumResponse ueberG =
          dienst.baum(MANDANT, Baumfenster.frei(G.minusHours(3), NAECHSTE), Baumgliederung.PARTNER);
      ProzessbaumResponse abG =
          dienst.baum(MANDANT, Baumfenster.frei(G, G.plusHours(2)), Baumgliederung.PARTNER);

      assertThat(vorG.gesamt().nachrichten()).as("vor G: keine Korrektur").isEqualTo(10);
      assertThat(ueberG.gesamt().nachrichten()).as("ueber G hinweg: nur der Eimer G").isEqualTo(12);
      assertThat(abG.gesamt().nachrichten()).as("ab G: beide Eimer").isEqualTo(15);
      for (ProzessbaumResponse antwort : List.of(vorG, ueberG, abG)) {
        ProzessknotenResponse blatt =
            prozesse(richtungen(gruppen(antwort.knoten()).getFirst()).getFirst()).getFirst();
        assertThat(blatt.zustand())
            .as("letzte Bewegung aus dem Live-Teil")
            .isEqualTo(Prozesszustand.BEWEGT);
        assertThat(blatt.letzteBewegung()).isEqualTo(NAECHSTE.atZone(ZONE).toInstant());
      }
    }

    /** Der Block in der Antwort, in allen drei Zustaenden; G in UTC. */
    @Test
    @DisplayName("Der Block liveRest nennt den Zustand und G nur bei ausgesetzt mit Lauf")
    void block_in_der_antwort() {
      ProzessbaumService dienst = dienst();
      bestandMit(List.of(), List.of());

      when(liveRest.ermittle(any(), any()))
          .thenReturn(LiveRestErgebnis.ohneKorrektur(LiveRestEntscheidung.ausgesetztAb(G)));
      ProzessbaumResponse ausgesetzt = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);
      assertThat(ausgesetzt.liveRest().zustand()).isEqualTo(LiveRestZustand.AUSGESETZT);
      // Winterzeit in Europe/Berlin: 04:00 Wanduhr ist 03:00Z.
      assertThat(ausgesetzt.liveRest().vollstaendigBis())
          .isEqualTo(Instant.parse("2025-12-30T03:00:00Z"));

      when(liveRest.ermittle(any(), any()))
          .thenReturn(LiveRestErgebnis.ohneKorrektur(LiveRestEntscheidung.ausgesetztOhneLauf()));
      ProzessbaumResponse ohneLauf = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);
      assertThat(ohneLauf.liveRest().zustand()).isEqualTo(LiveRestZustand.AUSGESETZT);
      assertThat(ohneLauf.liveRest().vollstaendigBis()).isNull();

      when(liveRest.ermittle(any(), any()))
          .thenReturn(LiveRestErgebnis.ohneKorrektur(LiveRestEntscheidung.nichtNoetig()));
      ProzessbaumResponse nichtNoetig = dienst.baum(MANDANT, null, Baumgliederung.PARTNER);
      assertThat(nichtNoetig.liveRest().zustand()).isEqualTo(LiveRestZustand.NICHT_NOETIG);
      assertThat(nichtNoetig.liveRest().vollstaendigBis()).isNull();
    }

    /** Ein Uhrenschlag je Anfrage: Der Live-Rest bekommt dasselbe jetzt wie das Fenster. */
    @Test
    @DisplayName("Der Live-Rest bekommt denselben Stichtag wie das Fenster")
    void derselbe_stichtag() {
      ProzessbaumService dienst = dienst();
      bestandMit(List.of(), List.of());

      dienst.baum(MANDANT, null, Baumgliederung.PARTNER);

      org.mockito.Mockito.verify(liveRest)
          .ermittle(
              org.mockito.ArgumentMatchers.eq(MANDANT), org.mockito.ArgumentMatchers.eq(WANDUHR));
    }
  }
}
