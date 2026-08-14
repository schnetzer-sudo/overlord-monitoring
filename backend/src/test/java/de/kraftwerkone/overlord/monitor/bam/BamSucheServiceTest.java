package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Service aus den Rohzeilen macht: <b>normalisieren, deckeln, beschriften</b> — <b>ohne
 * Datenbank</b>. Die Statements prüft {@link BamSucheStatementsTest}, die Trennung {@code
 * BamSucheIsolationDbIT}, die Laufzeiten M47.
 *
 * <p>Alle Prüfwerte sind erfunden (Regel G1).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BamSucheServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  private static final Zeitfenster FENSTER =
      new Zeitfenster(
          LocalDateTime.parse("2025-11-30T00:00:00"), LocalDateTime.parse("2025-12-30T00:00:00"));

  @Mock private BamSucheRepository repository;

  @Captor private ArgumentCaptor<List<Suchbedingung>> bedingungen;

  private BamSucheService service() {
    return new BamSucheService(
        repository, new MessageStatusClassifier(), Clock.fixed(Instant.EPOCH, ZONE));
  }

  private static BamSuchfilter filter(Suchbegriff... begriffe) {
    return new BamSuchfilter(List.of(begriffe), FENSTER, Suchmodus.EXAKT);
  }

  private static BamSuchfilter praefixfilter(Suchbegriff... begriffe) {
    return new BamSuchfilter(List.of(begriffe), FENSTER, Suchmodus.PRAEFIX);
  }

  private static BamTrefferZeile zeile(String messageId, int minute) {
    return new BamTrefferZeile(
        messageId,
        LocalDateTime.parse("2025-12-29T12:00:00").plusMinutes(minute),
        "FINISHED",
        "ein-prozess",
        "EIN_PROZESS",
        "Ein Projekt",
        "Ein Ablauf",
        "Ein Schritt",
        null,
        null,
        null,
        null);
  }

  private void gib(List<BamSollaengeZeile> kuratiert, List<BamTrefferZeile> treffer) {
    when(repository.findeSollaengen(any())).thenReturn(kuratiert);
    when(repository.findeTreffer(any(), anyList(), any())).thenReturn(treffer);
    when(repository.findeTrefferWerte(any(), anyList(), anyList())).thenReturn(List.of());
  }

  // ─── Die Varianten stehen in der Antwort ──────────────────────────────────────

  /**
   * <b>Keine stille Korrektur.</b> Wer ohne führende Null tippt und mit ihr findet, muss erfahren,
   * wonach gesucht wurde — sonst fände er die Nummer im Altwerkzeug nicht wieder.
   */
  @Test
  @DisplayName("Die Antwort nennt die Eingabe und die aufgefuellte Fassung")
  void die_antwort_nennt_die_varianten() {
    gib(List.of(new BamSollaengeZeile((short) 9012, 10, false)), List.of());

    BamSucheResponse antwort =
        service().suche(MANDANT, filter(new Suchbegriff((short) 9012, "12345678")));

    assertThat(antwort.begriffe()).hasSize(1);
    assertThat(antwort.begriffe().getFirst().eingabe()).isEqualTo("12345678");
    assertThat(antwort.begriffe().getFirst().typ()).isEqualTo((short) 9012);
    assertThat(antwort.begriffe().getFirst().varianten()).containsExactly("12345678", "0012345678");
  }

  /**
   * <b>Gesucht wird mit ihr, gemeldet wird sie nicht.</b> Ein führendes Leerzeichen sieht der
   * Nutzer weder in seiner Eingabe noch im angezeigten Wert.
   */
  @Test
  @DisplayName("Die Leerzeichen-Fassung wird gesucht, aber nicht gemeldet")
  void die_leerzeichen_fassung_wird_nicht_gemeldet() {
    gib(List.of(new BamSollaengeZeile((short) 9018, null, true)), List.of());

    BamSucheResponse antwort =
        service().suche(MANDANT, filter(new Suchbegriff((short) 9018, "12345")));

    assertThat(antwort.begriffe().getFirst().varianten()).containsExactly("12345");
    verify(repository).findeTreffer(any(), bedingungen.capture(), any());
    assertThat(bedingungen.getValue().getFirst().werte()).containsExactly("12345", " 12345");
  }

  // ─── Die Abschneidung ─────────────────────────────────────────────────────────

  /**
   * <b>Hartes Limit, erkannt an der (n+1)-ten Zeile</b> — und damit <b>nach</b> dem
   * Mandantenfilter, weil der im Statement steht.
   */
  @Test
  @DisplayName("Bei mehr Treffern als dem Limit wird gedeckelt und das Kennzeichen gesetzt")
  void abschneidung_wird_gemeldet() {
    List<BamTrefferZeile> zuViele = new ArrayList<>();
    for (int i = 0; i <= BamSucheRepository.HOECHSTENS_TREFFER; i++) {
      zuViele.add(zeile("nachricht-" + i, -i));
    }
    gib(List.of(), zuViele);

    BamSucheResponse antwort = service().suche(MANDANT, filter(new Suchbegriff(null, "1234567")));

    assertThat(antwort.nachrichten()).hasSize(BamSucheRepository.HOECHSTENS_TREFFER);
    assertThat(antwort.abgeschnitten()).isTrue();
    verify(repository)
        .findeTrefferWerte(
            any(),
            org.mockito.ArgumentMatchers.argThat(
                kennungen -> kennungen.size() == BamSucheRepository.HOECHSTENS_TREFFER),
            anyList());
  }

  @Test
  @DisplayName("Genau am Limit ist nichts abgeschnitten")
  void genau_am_limit_ist_nichts_abgeschnitten() {
    List<BamTrefferZeile> genau = new ArrayList<>();
    for (int i = 0; i < BamSucheRepository.HOECHSTENS_TREFFER; i++) {
      genau.add(zeile("nachricht-" + i, -i));
    }
    gib(List.of(), genau);

    BamSucheResponse antwort = service().suche(MANDANT, filter(new Suchbegriff(null, "1234567")));

    assertThat(antwort.nachrichten()).hasSize(BamSucheRepository.HOECHSTENS_TREFFER);
    assertThat(antwort.abgeschnitten()).isFalse();
  }

  @Test
  @DisplayName("Ohne Treffer gibt es keine zweite Abfrage")
  void ohne_treffer_keine_zweite_abfrage() {
    gib(List.of(), List.of());

    BamSucheResponse antwort = service().suche(MANDANT, filter(new Suchbegriff(null, "1234567")));

    assertThat(antwort.nachrichten()).isEmpty();
    assertThat(antwort.abgeschnitten()).isFalse();
    verify(repository)
        .findeTrefferWerte(any(), org.mockito.ArgumentMatchers.argThat(List::isEmpty), anyList());
  }

  // ─── Die Verdichtung ──────────────────────────────────────────────────────────

  /**
   * <b>Ein Wert unter zwei Typen ist eine Zeile mit zwei Treffern, nicht zwei Zeilen.</b> Bei 4,17
   * Prozent der Paare kommt das vor (M37).
   */
  @Test
  @DisplayName("Derselbe Wert unter zwei Typen ergibt eine Zeile mit zwei Treffern")
  void ein_wert_unter_zwei_typen_ist_eine_zeile() {
    gib(List.of(), List.of(zeile("eine-nachricht", 0)));
    when(repository.findeTrefferWerte(any(), anyList(), anyList()))
        .thenReturn(
            List.of(
                new BamTrefferWertZeile("eine-nachricht", (short) 9028, "1234567", "Kunde"),
                new BamTrefferWertZeile("eine-nachricht", (short) 9029, "1234567", "Lieferant")));

    BamSucheResponse antwort = service().suche(MANDANT, filter(new Suchbegriff(null, "1234567")));

    assertThat(antwort.nachrichten()).hasSize(1);
    assertThat(antwort.nachrichten().getFirst().treffer())
        .extracting(BamTrefferWertResponse::typ, BamTrefferWertResponse::bezeichnung)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple((short) 9028, "Kunde"),
            org.assertj.core.groups.Tuple.tuple((short) 9029, "Lieferant"));
  }

  /** Fehlt die Beschreibung im Altsystem, erscheint die Typnummer — sichtbar unfertig. */
  @Test
  @DisplayName("Ohne Beschreibung steht die Typnummer da")
  void ohne_beschreibung_die_typnummer() {
    gib(List.of(), List.of(zeile("eine-nachricht", 0)));
    when(repository.findeTrefferWerte(any(), anyList(), anyList()))
        .thenReturn(
            List.of(
                new BamTrefferWertZeile("eine-nachricht", (short) 4711, "1234567", null),
                new BamTrefferWertZeile("eine-nachricht", (short) 4712, "1234567", "  ")));

    BamSucheResponse antwort = service().suche(MANDANT, filter(new Suchbegriff(null, "1234567")));

    assertThat(antwort.nachrichten().getFirst().treffer())
        .extracting(BamTrefferWertResponse::bezeichnung)
        .containsExactly("4711", "4712");
  }

  // ─── Rollen und Listenfelder ──────────────────────────────────────────────────

  /**
   * <b>Die Rollen kosten keinen zusätzlichen Zugriff</b> (E4) und sind trotzdem der Unterschied
   * zwischen „ich habe die Nachricht" und „ich habe das Bündel": 96,87 Prozent der Wurzeln tragen
   * BAM-Werte, nur 2,42 Prozent der Kinder.
   */
  @Test
  @DisplayName("Die Rollen entstehen aus den vier Verkettungsspalten derselben Zeile")
  void rollen_kommen_aus_der_zeile() {
    BamTrefferZeile wurzel =
        new BamTrefferZeile(
            "eine-nachricht",
            LocalDateTime.parse("2025-12-29T12:00:00"),
            "SPLITTED",
            "ein-prozess",
            null,
            null,
            null,
            null,
            Boolean.TRUE,
            null,
            null,
            Boolean.FALSE);
    gib(List.of(), List.of(wurzel));

    BamSucheResponse antwort = service().suche(MANDANT, filter(new Suchbegriff(null, "1234567")));

    assertThat(antwort.nachrichten().getFirst().rollen()).containsExactly(Kettenrolle.SPLIT_WURZEL);
  }

  @Test
  @DisplayName("Ohne Verkettung ist rollen leer statt fehlend")
  void ohne_verkettung_leere_rollen() {
    gib(List.of(), List.of(zeile("eine-nachricht", 0)));

    assertThat(
            service()
                .suche(MANDANT, filter(new Suchbegriff(null, "1234567")))
                .nachrichten()
                .getFirst()
                .rollen())
        .isEmpty();
  }

  /**
   * <b>Der Schritt steht nur bei offenen Nachrichten</b> — dieselbe fachliche Entscheidung wie in
   * der Liste: {@code SOSActionID} ist auf jeder Zeile gesetzt (M13), benennt bei abgeschlossenen
   * aber den letzten Schritt.
   */
  @Test
  @DisplayName("Der Schritt steht nur bei WARTEND und LAEUFT")
  void der_schritt_steht_nur_bei_offenen() {
    gib(List.of(), List.of(zeile("abgeschlossen", 0)));
    assertThat(
            service()
                .suche(MANDANT, filter(new Suchbegriff(null, "1234567")))
                .nachrichten()
                .getFirst()
                .schritt())
        .isNull();

    BamTrefferZeile laeuft =
        new BamTrefferZeile(
            "laeuft",
            LocalDateTime.parse("2025-12-29T12:00:00"),
            "RUNNING",
            "ein-prozess",
            null,
            null,
            null,
            "Ein Schritt",
            null,
            null,
            null,
            null);
    gib(List.of(), List.of(laeuft));
    assertThat(
            service()
                .suche(MANDANT, filter(new Suchbegriff(null, "1234567")))
                .nachrichten()
                .getFirst()
                .schritt())
        .isEqualTo("Ein Schritt");
  }

  // ─── Das Zeitfenster in der Antwort ───────────────────────────────────────────

  /**
   * <b>Das verwendete Fenster steht in der Antwort</b>, nicht nur in der Anfrage — es verändert
   * nicht die Laufzeit, sondern die Antwort (M35: 279 von 234.159 Treffern bei einem Tagesfenster).
   */
  @Test
  @DisplayName("Die Antwort nennt das tatsaechlich verwendete Zeitfenster in UTC")
  void die_antwort_nennt_das_fenster() {
    gib(List.of(), List.of());

    BamSucheResponse antwort = service().suche(MANDANT, filter(new Suchbegriff(null, "1234567")));

    assertThat(antwort.von()).isEqualTo(FENSTER.von().atZone(ZONE).toInstant());
    assertThat(antwort.bis()).isEqualTo(FENSTER.bis().atZone(ZONE).toInstant());
  }

  /** Ein Mandant ohne Kuratierung sucht roh — und es entsteht trotzdem genau ein Abzug. */
  @Test
  @DisplayName("Die Kuratierung wird einmal je Suche gelesen")
  void kuratierung_wird_einmal_gelesen() {
    gib(List.of(), List.of());

    service()
        .suche(
            MANDANT,
            filter(new Suchbegriff(null, "1234567"), new Suchbegriff((short) 9018, "7654321")));

    verify(repository).findeSollaengen(MANDANT);
    verify(repository, never()).findeSollaengen(new MandantContext("SUTTONS"));
  }

  // ─── Der Suchmodus (Teil 4) ───────────────────────────────────────────────────

  /**
   * <b>Der verwendete Modus steht in der Antwort</b>, aus demselben Grund wie das Fenster: Er
   * verändert nicht den Preis, sondern die Antwort. M49‑3 misst, dass schon ein vollständig
   * eingetippter Wert als Präfix 23 Nachrichten statt einer findet.
   */
  @Test
  @DisplayName("Die Antwort nennt den tatsaechlich verwendeten Modus")
  void die_antwort_nennt_den_modus() {
    gib(List.of(), List.of());

    assertThat(service().suche(MANDANT, filter(new Suchbegriff(null, "1234567"))).modus())
        .isEqualTo(Suchmodus.EXAKT);
    assertThat(service().suche(MANDANT, praefixfilter(new Suchbegriff(null, "1234567"))).modus())
        .isEqualTo(Suchmodus.PRAEFIX);
  }

  /**
   * <b>Der Modus geht an jede Bedingung durch</b> — er gilt für die ganze Suche, nicht je Begriff.
   */
  @Test
  @DisplayName("Jede Suchbedingung traegt den Modus der Suche")
  void jede_bedingung_traegt_den_modus() {
    gib(List.of(), List.of());

    service()
        .suche(
            MANDANT,
            praefixfilter(
                new Suchbegriff(null, "1234567"), new Suchbegriff((short) 9012, "7654321")));

    verify(repository).findeTreffer(any(), bedingungen.capture(), any());
    assertThat(bedingungen.getValue())
        .hasSize(2)
        .allSatisfy(bedingung -> assertThat(bedingung.modus()).isEqualTo(Suchmodus.PRAEFIX));
  }

  /**
   * <b>Im Präfixmodus wandern die Nullen ins Muster</b> — und die gemeldeten Fassungen bleiben
   * lesbare Werte ohne Platzhalter. Die Gegenprobe daneben ist derselbe Begriff im exakten Modus.
   */
  @Test
  @DisplayName("Mit Typ meldet der Praefixmodus die Nullen-Fassungen, ohne Typ genau eine")
  void die_gemeldeten_fassungen_folgen_dem_modus() {
    gib(List.of(new BamSollaengeZeile((short) 9012, 10, false)), List.of());

    BamSucheResponse mitTyp =
        service().suche(MANDANT, praefixfilter(new Suchbegriff((short) 9012, "1234567")));
    assertThat(mitTyp.begriffe().getFirst().varianten())
        .containsExactly("1234567", "01234567", "001234567");

    BamSucheResponse ohneTyp =
        service().suche(MANDANT, praefixfilter(new Suchbegriff(null, "1234567")));
    assertThat(ohneTyp.begriffe().getFirst().varianten())
        .as("ohne Typ genau eine Fassung — M49-1 und die drei Gruende in Sollaengen")
        .containsExactly("1234567");

    BamSucheResponse exakt =
        service().suche(MANDANT, filter(new Suchbegriff((short) 9012, "1234567")));
    assertThat(exakt.begriffe().getFirst().varianten())
        .as("die Gegenprobe: exakt wird auf die Sollaenge aufgefuellt")
        .containsExactly("1234567", "0001234567");
  }
}
