package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Stundengrenzen der {@link Fensterverengung} — <b>ohne Datenbank, bei jedem Build</b>.
 *
 * <h2>Warum diese Rechnung einen eigenen Test verdient</h2>
 *
 * <p>Sie ist die Stelle, an der ein Fehler <b>Zeilen kostet</b> statt Laufzeit. Die Stundeneimer
 * des Rollups sind halboffen ({@code [stunde, stunde+1h)}), das Zeitfenster beginnt und endet aber
 * mitten in einer Stunde. Zaehlte man eine angebrochene Randstunde voll mit, waere die gezaehlte
 * Menge eine <b>Ober</b>schranke — und die Verengung schnitte zu weit: Laege im obersten Eimer eine
 * Stunde mit sechzig Nachrichten, von denen nur fuenf vor dem Fensterende liegen, hielte man 51
 * Zeilen fuer gefunden und lieferte fuenf.
 *
 * <p>Jeder Test hier prueft dieselbe eine Eigenschaft aus einem anderen Blickwinkel: <b>Was
 * gezaehlt wird, liegt vollstaendig im Fenster und ist vollstaendig gerechnet.</b>
 */
class FensterverengungGrenzenTest {

  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-30T04:09:47");

  /** Der Wasserstand der Testkopie liegt weit ueber dem Anker — hier der Normalfall. */
  private static final LocalDateTime WEIT_OBEN = LocalDateTime.parse("2026-08-27T15:00:00");

  private static Nachrichtenabfrage abfrage(LocalDateTime von, LocalDateTime bis) {
    return abfrage(von, bis, null);
  }

  private static Nachrichtenabfrage abfrage(
      LocalDateTime von, LocalDateTime bis, Seitenposition cursor) {
    return new Nachrichtenabfrage(
        new Zeitfenster(von, bis), Set.of(), List.of(), null, true, cursor, 50);
  }

  // ── Die angebrochenen Randstunden ────────────────────────────────────────

  @Test
  @DisplayName("Beide Raender angebrochen: die erste und die letzte Stunde zaehlen nicht mit")
  void angebrochene_raender_fallen_heraus() {
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT), WEIT_OBEN);

    assertThat(grenzen).isNotNull();
    // Die Stunde 04:00 des ersten Tages beginnt VOR dem Fenster -- sie zaehlt nur fuer den
    // Nullfall, nicht fuer die kumulierte Summe.
    assertThat(grenzen.hAllVon()).isEqualTo(LocalDateTime.parse("2025-11-30T04:00:00"));
    assertThat(grenzen.hVollVon()).isEqualTo(LocalDateTime.parse("2025-11-30T05:00:00"));
    // Die Stunde 04:00 des letzten Tages reicht ueber das Fensterende hinaus -- ebenso.
    assertThat(grenzen.hAllBis()).isEqualTo(LocalDateTime.parse("2025-12-30T04:00:00"));
    assertThat(grenzen.hVollBis()).isEqualTo(LocalDateTime.parse("2025-12-30T03:00:00"));
  }

  @Test
  @DisplayName("Beginnt das Fenster genau auf einer Stunde, gehoert sie ganz dazu")
  void volle_stunde_am_anfang_zaehlt_mit() {
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-12-29T00:00:00"), JETZT), WEIT_OBEN);

    assertThat(grenzen).isNotNull();
    assertThat(grenzen.hVollVon()).isEqualTo(LocalDateTime.parse("2025-12-29T00:00:00"));
    assertThat(grenzen.hVollVon()).isEqualTo(grenzen.hAllVon());
  }

  @Test
  @DisplayName("Auch ein Fensterende genau auf der Stunde laesst seine Stunde heraus")
  void volle_stunde_am_ende_zaehlt_trotzdem_nicht() {
    // 04:00:00 als Obergrenze heisst: von der Stunde 04:00 ist genau eine Sekunde drin. Die
    // Rechnung optimiert bewusst NICHT auf den Sonderfall "bis endet auf der letzten Sekunde" --
    // eine Stunde zu verschenken ist billiger als ein Sonderfall, den niemand nachrechnet.
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(
                LocalDateTime.parse("2025-12-29T00:00:00"),
                LocalDateTime.parse("2025-12-30T04:00:00")),
            WEIT_OBEN);

    assertThat(grenzen).isNotNull();
    assertThat(grenzen.hVollBis()).isEqualTo(LocalDateTime.parse("2025-12-30T03:00:00"));
  }

  // ── Der Wasserstand ──────────────────────────────────────────────────────

  @Test
  @DisplayName("Wasserstand mitten im Fenster: darueber wird nichts gezaehlt")
  void wasserstand_deckelt_die_obergrenze() {
    LocalDateTime wasserstand = LocalDateTime.parse("2025-12-20T00:00:00");

    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT), wasserstand);

    assertThat(grenzen).isNotNull();
    // fenster_bis ist AUSSCHLIESSEND: gerechnet ist alles unterhalb, die letzte gerechnete volle
    // Stunde ist also 23:00 des Vortags.
    assertThat(grenzen.hVollBis()).isEqualTo(LocalDateTime.parse("2025-12-19T23:00:00"));
    assertThat(grenzen.hVollBis()).isBefore(wasserstand);
    assertThat(grenzen.fensterGanzUnterWasserstand()).isFalse();
  }

  @Test
  @DisplayName("Wasserstand unter dem Fenster: es gibt keine gerechnete volle Stunde")
  void wasserstand_unter_dem_fenster_laesst_nichts_uebrig() {
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT),
            LocalDateTime.parse("2025-11-01T00:00:00"));

    assertThat(grenzen)
        .as("Ohne gerechnete Stunde im Fenster darf gar nicht erst verengt werden")
        .isNull();
  }

  @Test
  @DisplayName("Der Nullfall ist nur aussagbar, wenn das ganze Fenster gerechnet ist")
  void nullfall_nur_unter_dem_wasserstand() {
    Verengungsgrenzen ganzDrunter =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT), WEIT_OBEN);
    assertThat(ganzDrunter).isNotNull();
    assertThat(ganzDrunter.fensterGanzUnterWasserstand()).isTrue();

    Verengungsgrenzen halbDrunter =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT),
            LocalDateTime.parse("2025-12-20T00:00:00"));
    assertThat(halbDrunter).isNotNull();
    assertThat(halbDrunter.fensterGanzUnterWasserstand()).isFalse();
  }

  // ── Der Cursor ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("Der Cursor senkt die Obergrenze — jede Seite rechnet fuer sich")
  void cursor_senkt_die_obergrenze() {
    Seitenposition cursor =
        new Seitenposition(LocalDateTime.parse("2025-12-30T02:23:50"), "irgendeine-id");

    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT, cursor), WEIT_OBEN);

    assertThat(grenzen).isNotNull();
    assertThat(grenzen.bis()).isEqualTo(cursor.zeitpunkt());
    assertThat(grenzen.hAllBis()).isEqualTo(LocalDateTime.parse("2025-12-30T02:00:00"));
    // Die Stunde, in die der Cursor faellt, zaehlt nicht mit. Genau deshalb kann der Tiebreaker
    // ueber MessageID keine Zeile ausschliessen, die gezaehlt worden waere.
    assertThat(grenzen.hVollBis()).isEqualTo(LocalDateTime.parse("2025-12-30T01:00:00"));
  }

  @Test
  @DisplayName("Ein Cursor oberhalb des Fensterendes senkt nichts")
  void cursor_ausserhalb_wirkt_nicht() {
    Seitenposition cursor = new Seitenposition(JETZT.plusDays(1), "irgendeine-id");

    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT, cursor), WEIT_OBEN);

    assertThat(grenzen).isNotNull();
    assertThat(grenzen.bis()).isEqualTo(JETZT);
  }

  // ── Die Stufen ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("Dreissig Tage ergeben drei Stufen: eine Stunde, ein Tag, das ganze Fenster")
  void drei_stufen_ueber_dreissig_tage() {
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(
            abfrage(LocalDateTime.parse("2025-11-30T04:09:47"), JETZT), WEIT_OBEN);

    assertThat(Fensterverengung.stufen(grenzen))
        .containsExactly(
            LocalDateTime.parse("2025-12-30T03:00:00"),
            LocalDateTime.parse("2025-12-29T04:00:00"),
            LocalDateTime.parse("2025-11-30T04:00:00"));
  }

  @Test
  @DisplayName("Das Vorgabefenster von 24 Stunden braucht nur zwei Stufen")
  void zwei_stufen_ueber_vierundzwanzig_stunden() {
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(abfrage(JETZT.minusHours(24), JETZT), WEIT_OBEN);

    assertThat(Fensterverengung.stufen(grenzen))
        .as("Die 24-Stunden-Stufe faellt mit dem ganzen Fenster zusammen")
        .containsExactly(
            LocalDateTime.parse("2025-12-30T03:00:00"), LocalDateTime.parse("2025-12-29T04:00:00"));
  }

  @Test
  @DisplayName("Ein Fenster unter einer Stunde hat genau eine Stufe")
  void eine_stufe_bei_kurzem_fenster() {
    Verengungsgrenzen grenzen =
        Verengungsgrenzen.aus(abfrage(JETZT.minusMinutes(30), JETZT), WEIT_OBEN);

    // 30 Minuten enthalten keine volle Stunde -- es gibt gar nichts zu zaehlen.
    assertThat(grenzen).isNull();
  }

  @Test
  @DisplayName("Die letzte Stufe ist immer das ganze Fenster")
  void letzte_stufe_ist_das_ganze_fenster() {
    for (int stunden : new int[] {2, 6, 25, 24 * 7, 24 * 30, 24 * 365}) {
      Verengungsgrenzen grenzen =
          Verengungsgrenzen.aus(abfrage(JETZT.minusHours(stunden), JETZT), WEIT_OBEN);
      assertThat(grenzen).as("%d Stunden", stunden).isNotNull();
      List<LocalDateTime> stufen = Fensterverengung.stufen(grenzen);
      assertThat(stufen).as("%d Stunden", stunden).isNotEmpty();
      assertThat(stufen.get(stufen.size() - 1))
          .as("%d Stunden", stunden)
          .isEqualTo(grenzen.hAllVon());
      assertThat(stufen).as("%d Stunden, keine Stufe doppelt", stunden).doesNotHaveDuplicates();
    }
  }
}
