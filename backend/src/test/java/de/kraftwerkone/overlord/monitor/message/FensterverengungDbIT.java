package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Die Fensterverengung gegen die Testkopie — <b>und der eine Test, der den ganzen Bau traegt</b>.
 *
 * <h2>Was hier bewiesen wird</h2>
 *
 * <p>Dass verengt und unverengt <b>dieselben Zeilen in derselben Reihenfolge</b> liefern. Alles
 * andere an diesem Bau ist Laufzeit; das hier ist Richtigkeit. Eine Verengung, die zu weit
 * schneidet, faellt nicht auf: Sie liefert eine kuerzere Liste, keinen Fehler. Genau so ist am
 * 27.08.2026 gemessen worden, dass eine Fassung ohne Statusfilter fuer {@code SUTTONS} <b>null
 * statt fuenf</b> Zeilen liefert (offener Punkt 72).
 *
 * <h2>Warum am Bauteil und nicht am Endpunkt</h2>
 *
 * <p>Weil der Endpunkt <b>nicht unterscheiden kann</b>, ob die Verengung gegriffen hat. Er liefert
 * in beiden Faellen dasselbe — das ist ja der Sinn. Hier laufen deshalb beide Fassungen
 * <b>nebeneinander</b> gegen dieselbe Datenlage, und {@link Verengungsgrund} belegt, dass die
 * verengte Fassung wirklich verengt hat. Ein Test, der nur das Ergebnis prueft, waere gruen, auch
 * wenn die Verengung stillschweigend nie greift.
 *
 * <p>Das Verhalten am Endpunkt prueft {@code NachrichtenlisteDbIT}, die Mandantentrennung {@code
 * NachrichtenIsolationDbIT} und den Plan {@code NachrichtenPlanDbIT}.
 *
 * <h2>Alle zehn Mandanten</h2>
 *
 * <p>Nicht zwei. Genau daran ist Fassung F6 in Schritt 10b‑1 gescheitert: Sie erfuellte das Tor
 * fuer die zwei beauftragten Mandanten und zerstoerte drei der acht ungemessenen ({@code
 * docs/nachrichtenliste.md} §5a). Die zehn decken jeden Zweig der Verengung ab — {@code NEXANS}
 * loest auf der 1‑Stunden‑Stufe auf, {@code SUTTONS} auf der 24‑Stunden‑Stufe, {@code WOC} erst
 * ueber dem ganzen Fenster, {@code SYSTEM} gar nicht (unter der Schwelle), {@code NXHBE} und {@code
 * EDITIONLINGERI} sind der Nullfall.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class FensterverengungDbIT {

  /** Der Anker der Anwendungsuhr im Profil {@code dev}. */
  private static final LocalDateTime ANKER = LocalDateTime.parse("2025-12-30T04:09:47");

  private static final String[] ALLE_MANDANTEN = {
    "NEXANS", "SUTTONS", "VOTG", "IBIS", "IBISGUS",
    "ZAST", "WOC", "SYSTEM", "NXHBE", "EDITIONLINGERI"
  };

  @Autowired private VerengungRepository verengungRepository;
  @Autowired private NachrichtenRepository nachrichtenRepository;

  private Fensterverengung an() {
    return new Fensterverengung(verengungRepository, new NachrichtenlisteEigenschaften(true));
  }

  private Fensterverengung aus() {
    return new Fensterverengung(verengungRepository, new NachrichtenlisteEigenschaften(false));
  }

  private static Nachrichtenabfrage abfrage(int tage) {
    return abfrage(tage, Set.of(), List.of(), null);
  }

  private static Nachrichtenabfrage abfrage(
      int tage, Set<MessageStatusKind> status, List<String> prozesse, Seitenposition cursor) {
    return new Nachrichtenabfrage(
        new Zeitfenster(ANKER.minusDays(tage), ANKER),
        status,
        prozesse,
        null,
        false,
        ANKER,
        true,
        cursor,
        50);
  }

  /** Die Zeilen, die der Endpunkt mit dieser Verengung liefern wuerde. */
  private List<NachrichtZeile> zeilen(
      Fensterverengung verengung, String mandantId, Nachrichtenabfrage abfrage) {
    MandantContext mandant = new MandantContext(mandantId);
    Fensterverengung.Ergebnis ergebnis = verengung.verenge(mandant, abfrage);
    return ergebnis.sicherLeer()
        ? List.of()
        : nachrichtenRepository.finde(mandant, ergebnis.abfrage());
  }

  private static List<String> kennungen(List<NachrichtZeile> zeilen) {
    return zeilen.stream().map(NachrichtZeile::messageId).toList();
  }

  // ── Der Gleichheitsnachweis ──────────────────────────────────────────────

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "NEXANS", "SUTTONS", "VOTG", "IBIS", "IBISGUS",
        "ZAST", "WOC", "SYSTEM", "NXHBE", "EDITIONLINGERI"
      })
  @DisplayName("Verengt und unverengt liefern dieselben Zeilen — 30 Tage, ohne Filter")
  void gleiche_zeilen_ueber_dreissig_tage(String mandantId) {
    Nachrichtenabfrage abfrage = abfrage(30);

    assertThat(kennungen(zeilen(an(), mandantId, abfrage)))
        .as("verengt gegen unverengt bei %s", mandantId)
        .isEqualTo(kennungen(zeilen(aus(), mandantId, abfrage)));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"NEXANS", "SUTTONS", "ZAST", "WOC", "EDITIONLINGERI"})
  @DisplayName("Verengt und unverengt liefern dieselben Zeilen — das Vorgabefenster von 24 Stunden")
  void gleiche_zeilen_ueber_vierundzwanzig_stunden(String mandantId) {
    Nachrichtenabfrage abfrage = abfrage(1);

    assertThat(kennungen(zeilen(an(), mandantId, abfrage)))
        .as("verengt gegen unverengt bei %s", mandantId)
        .isEqualTo(kennungen(zeilen(aus(), mandantId, abfrage)));
  }

  // ── ⚠️ Der Fall aus offenem Punkt 72 ─────────────────────────────────────

  @Test
  @DisplayName("⚠️ SUTTONS mit Statusfilter FEHLER liefert seine Zeilen — nicht null")
  void statusfilter_verliert_keine_zeilen_bei_suttons() {
    Nachrichtenabfrage mitFehlern = abfrage(30, Set.of(MessageStatusKind.FEHLER), List.of(), null);

    List<String> unverengt = kennungen(zeilen(aus(), "SUTTONS", mitFehlern));
    List<String> verengt = kennungen(zeilen(an(), "SUTTONS", mitFehlern));

    // Schlaegt das fehl, hat sich die Testkopie geaendert -- nicht der Code. Ohne Fehlerzeilen
    // koennte dieser Test nichts beweisen: Null gleich null waere immer gruen.
    assertThat(unverengt)
        .as("Vorbedingung: SUTTONS hat im Fenster ueberhaupt Fehlerzeilen")
        .isNotEmpty();
    assertThat(verengt)
        .as(
            "Eine Verengung, die den Statusfilter nicht mittraegt, liefert hier null statt %d"
                + " Zeilen -- ohne Fehlermeldung. Das ist offener Punkt 72.",
            unverengt.size())
        .isEqualTo(unverengt);
  }

  @Test
  @DisplayName("⚠️ NEXANS mit Statusfilter FEHLER verliert keine Zeile am Rand")
  void statusfilter_verliert_keine_zeilen_bei_nexans() {
    Nachrichtenabfrage mitFehlern = abfrage(30, Set.of(MessageStatusKind.FEHLER), List.of(), null);

    List<String> unverengt = kennungen(zeilen(aus(), "NEXANS", mitFehlern));
    List<String> verengt = kennungen(zeilen(an(), "NEXANS", mitFehlern));

    assertThat(unverengt).as("Vorbedingung: NEXANS hat Fehlerzeilen im Fenster").isNotEmpty();
    assertThat(verengt).isEqualTo(unverengt);
  }

  @Test
  @DisplayName("Jede Statusart einzeln — keine darf Zeilen verlieren")
  void jede_statusart_verliert_keine_zeilen() {
    for (MessageStatusKind art : MessageStatusKind.values()) {
      Nachrichtenabfrage abfrage = abfrage(30, Set.of(art), List.of(), null);
      for (String mandantId : new String[] {"NEXANS", "SUTTONS"}) {
        assertThat(kennungen(zeilen(an(), mandantId, abfrage)))
            .as("Statusart %s bei %s", art, mandantId)
            .isEqualTo(kennungen(zeilen(aus(), mandantId, abfrage)));
      }
    }
  }

  // ── Dass die Verengung ueberhaupt greift ─────────────────────────────────

  @Test
  @DisplayName("Bei NEXANS und SUTTONS verengt sie wirklich — sonst bewiese der Rest nichts")
  void die_verengung_greift() {
    for (String mandantId : new String[] {"NEXANS", "SUTTONS", "VOTG", "IBIS", "IBISGUS"}) {
      Fensterverengung.Ergebnis ergebnis = an().verenge(new MandantContext(mandantId), abfrage(30));

      assertThat(ergebnis.grund())
          .as("Verengung bei %s", mandantId)
          .isEqualTo(Verengungsgrund.VERENGT);
      assertThat(ergebnis.abfrage().fenster().von())
          .as("Das Fenster von %s ist enger geworden", mandantId)
          .isAfter(ANKER.minusDays(30));
      assertThat(ergebnis.abfrage().fenster().bis())
          .as("Die Obergrenze bleibt unangetastet")
          .isEqualTo(ANKER);
    }
  }

  @Test
  @DisplayName("Der Nullfall: EDITIONLINGERI stellt keine Quellabfrage")
  void nullfall_stellt_keine_quellabfrage() {
    Fensterverengung.Ergebnis ergebnis =
        an().verenge(new MandantContext("EDITIONLINGERI"), abfrage(30));

    assertThat(ergebnis.grund()).isEqualTo(Verengungsgrund.NULLFALL);
    assertThat(ergebnis.sicherLeer()).isTrue();
    // Und die Gegenprobe: Die unverengte Fassung liefert tatsaechlich nichts -- der Nullfall
    // behauptet also nichts Falsches.
    assertThat(zeilen(aus(), "EDITIONLINGERI", abfrage(30))).isEmpty();
  }

  @Test
  @DisplayName("Unter der Schwelle wird nicht verengt — SYSTEM hat keine 51 Zeilen")
  void unter_der_schwelle_bleibt_das_fenster() {
    Fensterverengung.Ergebnis ergebnis = an().verenge(new MandantContext("SYSTEM"), abfrage(30));

    assertThat(ergebnis.grund()).isEqualTo(Verengungsgrund.KEINE_UNTERGRENZE);
    assertThat(ergebnis.abfrage().fenster().von()).isEqualTo(ANKER.minusDays(30));
  }

  // ── Der Cursor: jede Seite verengt fuer sich ─────────────────────────────

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"NEXANS", "SUTTONS"})
  @DisplayName("Zwei Seiten hintereinander — gleiche Zeilen, keine Luecke, keine Dopplung")
  void zwei_seiten_blaettern_richtig(String mandantId) {
    Nachrichtenabfrage seite1 = abfrage(30);

    List<NachrichtZeile> ersteVerengt = zeilen(an(), mandantId, seite1);
    List<NachrichtZeile> ersteUnverengt = zeilen(aus(), mandantId, seite1);
    assertThat(kennungen(ersteVerengt)).isEqualTo(kennungen(ersteUnverengt));
    assertThat(ersteVerengt).as("Die erste Seite ist voll").hasSize(51);

    // Der Cursor, wie ihn der Service setzt: die 50. Zeile -- die 51. ist die Sondierzeile.
    NachrichtZeile fuenfzigste = ersteVerengt.get(49);
    Seitenposition cursor = new Seitenposition(fuenfzigste.zeitpunkt(), fuenfzigste.messageId());
    Nachrichtenabfrage seite2 = abfrage(30, Set.of(), List.of(), cursor);

    List<NachrichtZeile> zweiteVerengt = zeilen(an(), mandantId, seite2);
    assertThat(kennungen(zweiteVerengt))
        .as("Die zweite Seite verengt fuer sich und liefert dasselbe wie unverengt")
        .isEqualTo(kennungen(zeilen(aus(), mandantId, seite2)));

    // Keine Luecke, keine Dopplung unter den GEZEIGTEN Zeilen: Die 51. der ersten Seite wird nie
    // angezeigt und ist zugleich die erste der zweiten. Das ist das Blaettern, nicht die Verengung
    // -- die unverengte Fassung zeigt dasselbe Bild.
    List<String> gezeigtErste = kennungen(ersteVerengt).subList(0, 50);
    List<String> gezeigtZweite = kennungen(zweiteVerengt).subList(0, 50);
    assertThat(gezeigtErste).doesNotContainAnyElementsOf(gezeigtZweite);
    assertThat(zweiteVerengt.getFirst().messageId())
        .as("Die zweite Seite setzt genau hinter der 50. Zeile an")
        .isEqualTo(ersteVerengt.get(50).messageId());
  }

  @Test
  @DisplayName("Auch die zweite Seite verengt — sie rechnet gegen den Cursor, nicht gegen jetzt")
  void zweite_seite_verengt_ebenfalls() {
    MandantContext mandant = new MandantContext("SUTTONS");
    List<NachrichtZeile> erste = zeilen(an(), "SUTTONS", abfrage(30));
    NachrichtZeile fuenfzigste = erste.get(49);

    Fensterverengung.Ergebnis zweite =
        an().verenge(
                mandant,
                abfrage(
                    30,
                    Set.of(),
                    List.of(),
                    new Seitenposition(fuenfzigste.zeitpunkt(), fuenfzigste.messageId())));

    assertThat(zweite.grund()).isEqualTo(Verengungsgrund.VERENGT);
    assertThat(zweite.abfrage().fenster().von())
        .as("Die Untergrenze der zweiten Seite liegt unter dem Cursor")
        .isBefore(fuenfzigste.zeitpunkt());
  }

  // ── Der Rueckfall ────────────────────────────────────────────────────────

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"NEXANS", "SUTTONS"})
  @DisplayName("ueberfaellig: unveraendert, und die Verengung wird nicht betreten")
  void ueberfaellig_bleibt_unveraendert(String mandantId) {
    Nachrichtenabfrage ueberfaellig =
        new Nachrichtenabfrage(
            new Zeitfenster(ANKER.minusDays(30), ANKER),
            Set.of(),
            List.of(),
            null,
            true,
            ANKER,
            true,
            null,
            50);

    Fensterverengung.Ergebnis ergebnis = an().verenge(new MandantContext(mandantId), ueberfaellig);

    assertThat(ergebnis.grund()).isEqualTo(Verengungsgrund.MERKMAL_NICHT_TRAGBAR);
    assertThat(ergebnis.abfrage()).isSameAs(ueberfaellig);
    assertThat(kennungen(zeilen(an(), mandantId, ueberfaellig)))
        .isEqualTo(kennungen(zeilen(aus(), mandantId, ueberfaellig)));
  }

  @Test
  @DisplayName(
      "Der Schalter aus: dieselben Zeilen wie vor diesem Branch, ohne jeden Rollup-Zugriff")
  void schalter_aus_verhaelt_sich_wie_vorher() {
    for (String mandantId : ALLE_MANDANTEN) {
      Fensterverengung.Ergebnis ergebnis =
          aus().verenge(new MandantContext(mandantId), abfrage(30));
      assertThat(ergebnis.grund()).as(mandantId).isEqualTo(Verengungsgrund.ABGESCHALTET);
      assertThat(ergebnis.abfrage().fenster().von()).as(mandantId).isEqualTo(ANKER.minusDays(30));
    }
  }
}
