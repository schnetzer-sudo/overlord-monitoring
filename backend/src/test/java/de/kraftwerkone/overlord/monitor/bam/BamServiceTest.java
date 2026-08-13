package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Service aus den Rohzeilen macht: <b>ordnen, zusammenlegen, die Restangabe rechnen</b> —
 * <b>ohne Datenbank</b>. Die Statements pruefen {@link BamStatementsTest}, der Isolationstest und
 * die Messungen in {@code docs/bam-werte.md}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BamServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final String MESSAGE_ID = "eine-nachricht";

  @Mock private BamRepository bamRepository;

  private BamService service() {
    return new BamService(bamRepository);
  }

  private void gib(List<BamTypZeile> typen, List<BamWertZeile> werte) {
    when(bamRepository.existiert(any(), anyString())).thenReturn(true);
    when(bamRepository.zaehleJeTyp(any(), anyString())).thenReturn(typen);
    when(bamRepository.findeWerte(any(), anyString())).thenReturn(werte);
  }

  private static BamWertZeile wert(int typ, String wert) {
    return new BamWertZeile((short) typ, wert);
  }

  // ─── Fehlerfall ────────────────────────────────────────────────────────────────

  /**
   * <b>Fremd und nicht vorhanden sind dasselbe</b> — beides ist dasselbe Statement mit null Zeilen.
   * Der Service kennt den Unterschied gar nicht erst.
   */
  @Test
  @DisplayName("Eine nicht sichtbare Nachricht ist 404 und keine leere Gruppenliste")
  void nicht_sichtbar_ist_404() {
    when(bamRepository.existiert(any(), anyString())).thenReturn(false);

    assertThatThrownBy(() -> service().werte(MANDANT, MESSAGE_ID))
        .isInstanceOf(RessourceNichtGefundenException.class);
    verify(bamRepository, never()).zaehleJeTyp(any(), anyString());
    verify(bamRepository, never()).findeWerte(any(), anyString());
  }

  /**
   * <b>Leer statt fehlend</b> — und ohne zweiten Zugriff. 80,6 Prozent aller Nachrichten tragen
   * keinen BAM-Wert (M41); dort gibt es nichts zu deckeln.
   */
  @Test
  @DisplayName("Ohne BAM-Werte kommt eine leere Gruppenliste — und keine zweite Abfrage")
  void ohne_werte_leere_liste_ohne_zweiten_zugriff() {
    when(bamRepository.existiert(any(), anyString())).thenReturn(true);
    when(bamRepository.zaehleJeTyp(any(), anyString())).thenReturn(List.of());

    BamResponse antwort = service().werte(MANDANT, MESSAGE_ID);

    assertThat(antwort.messageId()).isEqualTo(MESSAGE_ID);
    assertThat(antwort.gruppen()).as("immer vorhanden, leer statt fehlend").isEmpty();
    verify(bamRepository, never()).findeWerte(any(), anyString());
  }

  // ─── Ordnung ───────────────────────────────────────────────────────────────────

  /**
   * <b>Die konfigurierten Typen zuerst, in ihrer {@code MessageBAMTypeSortIndex}-Reihenfolge.</b>
   * Nicht nach Typnummer: Die Konfiguration ist genau die Stelle, an der ein Mandant sagt, welcher
   * Beleg fuer ihn vorn steht.
   */
  @Test
  @DisplayName("Konfigurierte Typen stehen vorn, in der Reihenfolge ihres Sortierindex")
  void konfigurierte_typen_in_ihrer_reihenfolge() {
    gib(
        List.of(
            // Die Typnummern steigen, der Sortierindex faellt — nur so ist unterscheidbar,
            // wonach tatsaechlich sortiert wird.
            new BamTypZeile((short) 9014, "Lieferantennummer", 1, (short) 30),
            new BamTypZeile((short) 9018, "Kundenmaterialnummer", 1, (short) 10),
            new BamTypZeile((short) 9033, "Empfaengercode", 1, (short) 20)),
        List.of(wert(9014, "a"), wert(9018, "b"), wert(9033, "c")));

    List<Integer> reihenfolge =
        service().werte(MANDANT, MESSAGE_ID).gruppen().stream()
            .map(BamGruppeResponse::typ)
            .toList();

    assertThat(reihenfolge)
        .as("sortiert wird nach MessageBAMTypeSortIndex und nicht nach der Typnummer")
        .containsExactly(9018, 9033, 9014);
  }

  /**
   * <b>Der unkonfigurierte Typ erscheint — und zwar hinten.</b> Der Fall ist real: {@code WOC} hat
   * keinen konfigurierten Typ und traegt 2.067 BAM-Zeilen unter Typ 9014 (M40). Folgte der Block
   * der Konfiguration als Filter, saehe dieser Mandant nichts.
   */
  @Test
  @DisplayName("Ein nicht konfigurierter Typ erscheint, hinten und ohne Markierung")
  void unkonfigurierter_typ_erscheint_hinten() {
    gib(
        List.of(
            new BamTypZeile((short) 9014, "Lieferantennummer", 1, null),
            new BamTypZeile((short) 3, "Rechnungsnummer", 1, null),
            new BamTypZeile((short) 9018, "Kundenmaterialnummer", 1, (short) 9025)),
        List.of(wert(9014, "a"), wert(3, "b"), wert(9018, "c")));

    List<BamGruppeResponse> gruppen = service().werte(MANDANT, MESSAGE_ID).gruppen();

    assertThat(gruppen.stream().map(BamGruppeResponse::typ).toList())
        .as("konfiguriert zuerst, danach die uebrigen nach Typnummer")
        .containsExactly(9018, 3, 9014);
    assertThat(gruppen)
        .as(
            "und nichts an der Antwort verraet, welcher Typ konfiguriert ist — das ist eine"
                + " interne Angabe und geht den Nutzer nichts an")
        .allSatisfy(
            gruppe ->
                assertThat(gruppe.bezeichnung())
                    .doesNotContain("nicht konfiguriert")
                    .doesNotContain("unbekannt"));
  }

  // ─── Deckelung ─────────────────────────────────────────────────────────────────

  /**
   * <b>Der Nachweis der Deckelung.</b> Die Zaehlung nennt die wahre Zahl, die Werte kommen
   * gedeckelt — und {@code weitereVorhanden} bringt beides zusammen.
   */
  @Test
  @DisplayName("Eine Gruppe mit mehr als 20 Werten liefert genau 20 und ein groesseres gesamt")
  void gruppe_ueber_der_grenze_wird_gedeckelt() {
    int grenze = BamRepository.WERTE_JE_GRUPPE;
    List<BamWertZeile> gedeckelt = new ArrayList<>();
    for (int nummer = 0; nummer < grenze; nummer++) {
      gedeckelt.add(wert(3, "wert-%02d".formatted(nummer)));
    }
    gib(List.of(new BamTypZeile((short) 3, "Rechnungsnummer", 3007, (short) 3)), gedeckelt);

    BamGruppeResponse gruppe = service().werte(MANDANT, MESSAGE_ID).gruppen().getFirst();

    assertThat(gruppe.werte()).hasSize(grenze);
    assertThat(gruppe.gesamt()).as("die wahre Zahl aus der Zaehlung").isEqualTo(3007);
    assertThat(gruppe.weitereVorhanden()).isTrue();
    assertThat(gruppe.gesamt() - gruppe.werte().size())
        .as("die Restangabe der Oberflaeche — 2.987 weitere")
        .isEqualTo(2987);
  }

  /**
   * <b>Genau an der Grenze gibt es keine Restangabe.</b> Verglichen wird die wahre Zahl mit der
   * gelieferten und nicht mit der Konstanten — sonst behauptete eine Gruppe von exakt zwanzig
   * Werten, es gebe mehr.
   */
  @Test
  @DisplayName("Eine Gruppe von genau 20 Werten meldet keine weiteren")
  void gruppe_genau_an_der_grenze_meldet_nichts() {
    int grenze = BamRepository.WERTE_JE_GRUPPE;
    List<BamWertZeile> alle = new ArrayList<>();
    for (int nummer = 0; nummer < grenze; nummer++) {
      alle.add(wert(3, "wert-%02d".formatted(nummer)));
    }
    gib(List.of(new BamTypZeile((short) 3, "Rechnungsnummer", grenze, (short) 3)), alle);

    BamGruppeResponse gruppe = service().werte(MANDANT, MESSAGE_ID).gruppen().getFirst();

    assertThat(gruppe.werte()).hasSize(grenze);
    assertThat(gruppe.weitereVorhanden()).isFalse();
  }

  /** Eine kleine Gruppe kommt vollstaendig und ohne Restangabe. */
  @Test
  @DisplayName("Eine Gruppe unter der Grenze kommt vollstaendig")
  void kleine_gruppe_kommt_vollstaendig() {
    gib(
        List.of(new BamTypZeile((short) 9018, "Kundenmaterialnummer", 2, (short) 9025)),
        List.of(wert(9018, "a"), wert(9018, "b")));

    BamGruppeResponse gruppe = service().werte(MANDANT, MESSAGE_ID).gruppen().getFirst();

    assertThat(gruppe.werte()).containsExactly("a", "b");
    assertThat(gruppe.gesamt()).isEqualTo(2);
    assertThat(gruppe.weitereVorhanden()).isFalse();
  }

  /**
   * Die Reihenfolge innerhalb einer Gruppe ist die der Abfrage. Hier wird <b>nicht ein zweites Mal
   * sortiert</b> — eine zweite Ordnung an einer zweiten Stelle ist die Drift, bei der zwei Aufrufe
   * dasselbe verschieden zeigen.
   */
  @Test
  @DisplayName("Die Werte behalten die Reihenfolge der Abfrage")
  void werte_behalten_ihre_reihenfolge() {
    gib(
        List.of(new BamTypZeile((short) 3, "Rechnungsnummer", 3, (short) 3)),
        List.of(wert(3, "001"), wert(3, "010"), wert(3, "100")));

    assertThat(service().werte(MANDANT, MESSAGE_ID).gruppen().getFirst().werte())
        .containsExactly("001", "010", "100");
  }

  /**
   * <b>Derselbe Wert unter zwei Typen ist zwei Gruppen und keine Dublette.</b> Bei 4,17 Prozent der
   * Paare steht derselbe Wert unter mehreren Typen (M37) — der Schluessel ist {@code (typ, wert)},
   * niemals der Wert allein.
   */
  @Test
  @DisplayName("Derselbe Wert unter zwei Typen erscheint in beiden Gruppen")
  void derselbe_wert_unter_zwei_typen() {
    gib(
        List.of(
            new BamTypZeile((short) 9014, "Lieferantennummer", 1, (short) 9021),
            new BamTypZeile((short) 9015, "Kundenwerk", 1, (short) 9022)),
        List.of(wert(9014, "0050"), wert(9015, "0050")));

    List<BamGruppeResponse> gruppen = service().werte(MANDANT, MESSAGE_ID).gruppen();

    assertThat(gruppen).hasSize(2);
    assertThat(gruppen.get(0).werte()).containsExactly("0050");
    assertThat(gruppen.get(1).werte()).containsExactly("0050");
  }

  // ─── Beschriftung ──────────────────────────────────────────────────────────────

  /**
   * <b>Ohne Beschreibung erscheint die Typnummer</b>, sichtbar unfertig. Dieselbe Regel wie bei
   * einem kuratierten Eigenschaftsnamen ohne Uebersetzung.
   */
  @Test
  @DisplayName("Ein Typ ohne Beschreibung traegt seine Typnummer als Beschriftung")
  void typ_ohne_beschreibung_zeigt_die_nummer() {
    gib(
        List.of(
            new BamTypZeile((short) 9014, null, 1, (short) 1),
            new BamTypZeile((short) 9015, "   ", 1, (short) 2)),
        List.of(wert(9014, "a"), wert(9015, "b")));

    assertThat(
            service().werte(MANDANT, MESSAGE_ID).gruppen().stream()
                .map(BamGruppeResponse::bezeichnung)
                .toList())
        .as("ein leerer Text ist so wenig eine Beschriftung wie ein fehlender")
        .containsExactly("9014", "9015");
  }

  /**
   * <b>Die Endungen bleiben stehen.</b> Sie sehen nach internem Beiwerk aus, tragen aber
   * mutmasslich Bedeutung — und ob die Beschreibungen ohne sie noch eindeutig sind, ist nicht
   * gemessen. Eine Kuerzungsregel waere nach Regel Q4 geraten.
   */
  @Test
  @DisplayName("Die Beschreibung kommt unveraendert, samt ihrer Endung")
  void beschreibung_bleibt_unveraendert() {
    gib(
        List.of(new BamTypZeile((short) 9014, "Lieferantennummer beim Kunden_K_SAP", 1, (short) 1)),
        List.of(wert(9014, "a")));

    assertThat(service().werte(MANDANT, MESSAGE_ID).gruppen().getFirst().bezeichnung())
        .isEqualTo("Lieferantennummer beim Kunden_K_SAP");
  }

  /**
   * Eine Gruppe, zu der die Wertabfrage nichts liefert, verschwindet nicht — sie zeigt ihre wahre
   * Zahl und keine Werte. Der Fall kann nicht auftreten, solange beide Abfragen denselben Filter
   * tragen; er ist trotzdem behandelt, weil eine Gruppe ohne Zahl schlechter waere als eine ohne
   * Werte.
   */
  @Test
  @DisplayName("Eine Gruppe ohne gelieferte Werte behaelt ihre Zahl")
  void gruppe_ohne_werte_behaelt_ihre_zahl() {
    gib(List.of(new BamTypZeile((short) 3, "Rechnungsnummer", 7, (short) 3)), List.of());

    BamGruppeResponse gruppe = service().werte(MANDANT, MESSAGE_ID).gruppen().getFirst();

    assertThat(gruppe.werte()).isEmpty();
    assertThat(gruppe.gesamt()).isEqualTo(7);
    assertThat(gruppe.weitereVorhanden()).isTrue();
  }
}
