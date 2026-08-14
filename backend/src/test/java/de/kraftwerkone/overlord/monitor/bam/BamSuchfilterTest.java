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

  private static final List<String> EIN_BEGRIFF = List.of(":4711815");

  private static BamSuchfilter filter(List<String> begriffe) {
    return BamSuchfilter.aus(begriffe, null, null, null, UHR);
  }

  private static BamSuchfilter filter(List<String> begriffe, String modus) {
    return BamSuchfilter.aus(begriffe, null, null, modus, UHR);
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
    weistAb(() -> BamSuchfilter.aus(null, null, null, null, UHR), "suchbegriff-fehlt");
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
                List.of(":4711815"), "2024-01-01T00:00:00Z", "2026-01-01T00:00:00Z", null, UHR),
        "zeitfenster-zu-gross");

    assertThat(
            BamSuchfilter.aus(
                    List.of(":4711815"), "2025-01-01T00:00:00Z", "2025-12-31T00:00:00Z", null, UHR)
                .fenster())
        .isNotNull();
  }

  @Test
  @DisplayName("Nur eine der beiden Grenzen ist 400")
  void halbes_fenster_ist_400() {
    weistAb(
        () -> BamSuchfilter.aus(List.of(":4711815"), "2025-01-01T00:00:00Z", null, null, UHR),
        "zeitfenster-unvollstaendig");
  }

  @Test
  @DisplayName("Ein unlesbarer Zeitpunkt ist 400")
  void unlesbarer_zeitpunkt_ist_400() {
    weistAb(
        () -> BamSuchfilter.aus(List.of(":4711815"), "gestern", "heute", null, UHR),
        "zeitpunkt-ungueltig");
  }

  // ─── Der Suchmodus (Teil 4) ───────────────────────────────────────────────────

  /**
   * <b>Die Vorgabe ist exakt, und sie ist die Zusage des Teils</b>: Fehlt der Parameter, verhält
   * sich der Endpunkt wie vor Teil 4.
   */
  @Test
  @DisplayName("Ohne modus-Parameter gilt exakt")
  void ohne_modus_gilt_exakt() {
    assertThat(filter(EIN_BEGRIFF).modus()).isEqualTo(Suchmodus.EXAKT);
    assertThat(filter(EIN_BEGRIFF, null).modus()).isEqualTo(Suchmodus.EXAKT);
    assertThat(filter(EIN_BEGRIFF, "   ").modus())
        .as("ein leerer Parameter ist keine Angabe — wie ?begriff= kein Begriff ist")
        .isEqualTo(Suchmodus.EXAKT);
    assertThat(Suchmodus.VORGABE).isEqualTo(Suchmodus.EXAKT);
  }

  @Test
  @DisplayName("modus=praefix und modus=exakt werden gelesen, auch in Grossschreibung")
  void beide_modi_werden_gelesen() {
    assertThat(filter(EIN_BEGRIFF, "praefix").modus()).isEqualTo(Suchmodus.PRAEFIX);
    assertThat(filter(EIN_BEGRIFF, "exakt").modus()).isEqualTo(Suchmodus.EXAKT);
    assertThat(filter(EIN_BEGRIFF, "PRAEFIX").modus()).isEqualTo(Suchmodus.PRAEFIX);
    assertThat(filter(EIN_BEGRIFF, " praefix ").modus()).isEqualTo(Suchmodus.PRAEFIX);
  }

  /**
   * <b>Kein stiller Rückfall.</b> Wer sich vertippt, sucht sonst exakt und hält das Ergebnis für
   * das der Präfixsuche.
   */
  @Test
  @DisplayName("Ein unbekannter Modus ist 400 mit eigenem Problemtyp")
  void unbekannter_modus_ist_400() {
    weistAb(() -> filter(EIN_BEGRIFF, "prefix"), "suchmodus-ungueltig");
    weistAb(() -> filter(EIN_BEGRIFF, "praefixsuche"), "suchmodus-ungueltig");
    weistAb(() -> filter(EIN_BEGRIFF, "1"), "suchmodus-ungueltig");
  }

  /**
   * <b>Zweig B aus M50.</b> Der schlimmste bekannte Präfix des Bestands ist über ein Jahr an der
   * 60‑Sekunden-Grenze abgebrochen; über 30 Tage kostet er 3,851 s. Der Präfixmodus ist deshalb auf
   * 30 Tage gedeckelt — <b>und es wird nichts gekappt</b>, ein größeres Fenster ist {@code 400}.
   */
  @Test
  @DisplayName("Im Praefixmodus ist ein Fenster ueber 30 Tagen 400")
  void praefix_ist_auf_dreissig_tage_gedeckelt() {
    weistAb(
        () ->
            BamSuchfilter.aus(
                EIN_BEGRIFF, "2025-01-01T00:00:00Z", "2025-12-31T00:00:00Z", "praefix", UHR),
        "praefixsuche-fenster-zu-gross");

    assertThat(BamSuchfilter.PRAEFIX_FENSTER_MAXIMUM).isEqualTo(Duration.ofDays(30));
  }

  /** Genau auf der Grenze geht es noch — und einen Tag darüber nicht mehr. */
  @Test
  @DisplayName("Genau 30 Tage gehen im Praefixmodus, 31 nicht")
  void die_grenze_liegt_bei_genau_dreissig_tagen() {
    assertThat(
            BamSuchfilter.aus(
                    EIN_BEGRIFF, "2025-11-30T00:00:00Z", "2025-12-30T00:00:00Z", "praefix", UHR)
                .modus())
        .isEqualTo(Suchmodus.PRAEFIX);

    weistAb(
        () ->
            BamSuchfilter.aus(
                EIN_BEGRIFF, "2025-11-29T00:00:00Z", "2025-12-30T00:00:00Z", "praefix", UHR),
        "praefixsuche-fenster-zu-gross");
  }

  /**
   * <b>Ohne von/bis greift der Deckel nie</b> — die Vorgabe sind dieselben 30 Tage. Der Rückfall
   * aus einer leeren exakten Suche über das Standardfenster läuft damit ohne Sonderfall.
   */
  @Test
  @DisplayName("Der Praefixmodus mit der Fenstervorgabe ist gueltig")
  void praefix_mit_der_vorgabe_geht() {
    BamSuchfilter filter = filter(EIN_BEGRIFF, "praefix");

    assertThat(filter.modus()).isEqualTo(Suchmodus.PRAEFIX);
    assertThat(filter.fenster().spanne()).isEqualTo(Duration.ofDays(30));
  }

  /** <b>Die exakte Suche behält ihr Jahresmaximum</b> — der Deckel gilt nur dem neuen Pfad. */
  @Test
  @DisplayName("Der Deckel gilt nur fuer praefix, nicht fuer exakt")
  void der_deckel_gilt_nur_dem_praefixmodus() {
    assertThat(
            BamSuchfilter.aus(
                    EIN_BEGRIFF, "2025-01-01T00:00:00Z", "2025-12-31T00:00:00Z", "exakt", UHR)
                .fenster()
                .spanne())
        .isGreaterThan(BamSuchfilter.PRAEFIX_FENSTER_MAXIMUM);
  }
}
