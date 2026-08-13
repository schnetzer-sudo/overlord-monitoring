package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Parameterprüfung der BAM-Suche — <b>ohne Datenbank</b>.
 *
 * <p>Alle Prüfwerte sind erfunden (Regel G1); geprüft werden Form und Grenzen, nicht Inhalte.
 */
class BamSuchfilterTest {

  /**
   * Eine feste Uhr statt der Systemuhr — die Testkopie liegt Monate hinter der realen Zeit, und die
   * Vorgabe soll hier nachrechenbar sein und nicht vom Tag abhängen (Regel Z1).
   */
  private static final Clock UHR =
      Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("Europe/Berlin"));

  private static BamSuchfilter filter(List<String> begriffe) {
    return BamSuchfilter.aus(begriffe, null, null, UHR);
  }

  private static void weistAb(ThrowingCallable aufruf, String problemTyp) {
    assertThatThrownBy(aufruf)
        .isInstanceOf(FachlicheAusnahme.class)
        .extracting(fehler -> ((FachlicheAusnahme) fehler).problemTyp())
        .isEqualTo(problemTyp);
  }

  // ─── Der Pflichttrenner ───────────────────────────────────────────────────────

  /**
   * <b>Ohne Trenner müsste die Anwendung raten</b>, ob eine führende Ziffernfolge ein Typ ist oder
   * Teil des Werts. Beides sind Zahlen. Der Pflichttrenner macht die Frage gegenstandslos, statt
   * sie nach Regel Q4 zu beantworten.
   */
  @Test
  @DisplayName("Ein Begriff ohne Doppelpunkt ist 400")
  void ohne_trenner_ist_400() {
    weistAb(() -> filter(List.of("4711815")), "suchbegriff-ohne-typtrenner");
  }

  @Test
  @DisplayName("Ohne Typ steht der Doppelpunkt trotzdem da")
  void ohne_typ_mit_trenner() {
    BamSuchfilter filter = filter(List.of(":4711815"));

    assertThat(filter.begriffe()).containsExactly(new Suchbegriff(null, "4711815"));
    assertThat(filter.begriffe().getFirst().mitTyp()).isFalse();
  }

  /**
   * <b>Geteilt wird am ersten Doppelpunkt.</b> Damit ist die ungemessene Frage, ob ein BAM-Wert
   * selbst einen tragen kann, gegenstandslos: Alles dahinter ist Wert.
   */
  @Test
  @DisplayName("Geteilt wird am ERSTEN Doppelpunkt, der Rest bleibt Wert")
  void geteilt_wird_am_ersten_doppelpunkt() {
    assertThat(filter(List.of("9018:AB:CD")).begriffe())
        .containsExactly(new Suchbegriff((short) 9018, "AB:CD"));
  }

  @Test
  @DisplayName("Ein Typteil, der keine Typnummer ist, ist 400")
  void typ_ungueltig_ist_400() {
    weistAb(() -> filter(List.of("neun:4711815")), "suchbegriff-typ-ungueltig");
    weistAb(
        () -> filter(List.of("99999:4711815")),
        // MessageBAMType ist smallint(6) — 99999 passt nicht hinein (M46-0).
        "suchbegriff-typ-ungueltig");
  }

  @Test
  @DisplayName("Die Raender werden beschnitten, in beiden Teilen")
  void raender_werden_beschnitten() {
    assertThat(filter(List.of("  9018 :  4711815  ")).begriffe())
        .containsExactly(new Suchbegriff((short) 9018, "4711815"));
  }

  // ─── Mindestens einer ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("Ohne Begriff ist die Suche 400 und keine leere Antwort")
  void ohne_begriff_ist_400() {
    weistAb(() -> filter(List.of()), "suchbegriff-fehlt");
    weistAb(() -> BamSuchfilter.aus(null, null, null, UHR), "suchbegriff-fehlt");
    weistAb(() -> filter(List.of("  ")), "suchbegriff-fehlt");
    weistAb(() -> filter(List.of(":", ":   ")), "suchbegriff-fehlt");
  }

  /**
   * <b>Keine Mindestlänge</b> — Regel L5 nennt sie, E6 und M38 widerlegen sie als Schutz. Was
   * trägt, sind das Zeitfenster und das harte Limit.
   */
  @Test
  @DisplayName("Ein einziges Zeichen genuegt — es gibt keine Mindestlaenge")
  void ein_zeichen_genuegt() {
    assertThat(filter(List.of(":7")).begriffe()).containsExactly(new Suchbegriff(null, "7"));
  }

  // ─── Das Schutzgelaender ──────────────────────────────────────────────────────

  private static List<String> begriffe(int anzahl) {
    List<String> begriffe = new ArrayList<>(anzahl);
    for (int i = 1; i <= anzahl; i++) {
      begriffe.add(":wert" + i);
    }
    return begriffe;
  }

  @Test
  @DisplayName("Acht Begriffe gehen, neun sind 400")
  void hoechstens_acht_begriffe() {
    assertThat(filter(begriffe(BamSuchfilter.HOECHSTENS_BEGRIFFE)).begriffe())
        .hasSize(BamSuchfilter.HOECHSTENS_BEGRIFFE);
    weistAb(() -> filter(begriffe(BamSuchfilter.HOECHSTENS_BEGRIFFE + 1)), "zu-viele-suchbegriffe");
  }

  // ─── Das Zeitfenster ──────────────────────────────────────────────────────────

  /**
   * <b>30 Tage statt der 24 Stunden aus Regel L1</b>, und die Abweichung ist gemessen: Der Nutzer
   * mit einer Belegnummer hat kein Datum, und ein Tagesfenster findet beim schlimmsten Wert 279 von
   * 234.159 Nachrichten (M35).
   */
  @Test
  @DisplayName("Ohne von/bis gilt die Vorgabe von 30 Tagen")
  void vorgabe_sind_dreissig_tage() {
    BamSuchfilter filter = filter(List.of(":4711815"));

    assertThat(filter.fenster().spanne()).isEqualTo(Duration.ofDays(30));
    assertThat(BamSuchfilter.FENSTER_VORGABE).isEqualTo(Duration.ofDays(30));
  }

  /**
   * <b>Die Obergrenze wird nicht neu erfunden</b> — es ist die aus {@code common/Zeitfenster}, samt
   * ihres Problemtyps. Und es wird nichts gekappt: Ein zu großes Fenster ist {@code 400}.
   */
  @Test
  @DisplayName("Ueber einem Jahr ist 400 — dieselbe Grenze wie in der Nachrichtenliste")
  void fenstermaximum_ist_ein_jahr() {
    weistAb(
        () ->
            BamSuchfilter.aus(
                List.of(":4711815"), "2024-01-01T00:00:00Z", "2026-01-01T00:00:00Z", UHR),
        "zeitfenster-zu-gross");

    assertThat(
            BamSuchfilter.aus(
                    List.of(":4711815"), "2025-01-01T00:00:00Z", "2025-12-31T00:00:00Z", UHR)
                .fenster())
        .isNotNull();
  }

  @Test
  @DisplayName("Nur eine der beiden Grenzen ist 400")
  void halbes_fenster_ist_400() {
    weistAb(
        () -> BamSuchfilter.aus(List.of(":4711815"), "2025-01-01T00:00:00Z", null, UHR),
        "zeitfenster-unvollstaendig");
  }

  @Test
  @DisplayName("Ein unlesbarer Zeitpunkt ist 400")
  void unlesbarer_zeitpunkt_ist_400() {
    weistAb(
        () -> BamSuchfilter.aus(List.of(":4711815"), "gestern", "heute", UHR),
        "zeitpunkt-ungueltig");
  }
}
