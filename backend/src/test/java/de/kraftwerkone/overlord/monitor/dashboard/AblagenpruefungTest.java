package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.Ablagezugriff;
import de.kraftwerkone.overlord.monitor.common.Abrufergebnis;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Der Durchgang der Ablagenpruefung — <b>mit ersetztem {@link Ablagezugriff} und fester Uhr</b>.
 *
 * <p><b>Kein Test dieses Projekts spricht eine Ablage an</b>; die einzige Ausnahme ist die Messung
 * {@code MessungM174DbIT}. Hier wird genau die Schnittstelle ersetzt, die nach draussen fuehrt —
 * und damit ist zugleich gezeigt, dass die Einordnung <b>aus dem vorhandenen Ergebnis</b> kommt und
 * nicht aus einer zweiten Auswertung der SOAP-Antwort.
 *
 * <p>Alle Kennungen sind erfunden (Regel T2, Regel G1), die Adressen ebenso.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AblagenpruefungTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
  private static final Instant JETZT = Instant.parse("2025-12-30T04:09:47Z");
  private static final Duration TAKT = Duration.ofSeconds(60);

  private static final String ZIEL_A = "ABLAGE_ERFUNDEN_A";
  private static final String ZIEL_B = "ABLAGE_ERFUNDEN_B";

  /** Eine erfundene Adresse. Sie ist kein Hostname der Anlage (Regel G1) und existiert nicht. */
  private static final String ADRESSE_A = "http://erfunden-a.invalid/Empfaenger";

  private static final String ADRESSE_B = "http://erfunden-b.invalid/Empfaenger";

  @Mock private DienstLeseRepository dienstLeseRepository;
  @Mock private Ablagezugriff ablagezugriff;

  private Ablagenpruefung pruefung() {
    return new Ablagenpruefung(
        dienstLeseRepository,
        ablagezugriff,
        Clock.fixed(JETZT, ZONE),
        new AblagenpruefungEigenschaften(true, TAKT));
  }

  private void zieleSind(Ablagenziel... ziele) {
    when(dienstLeseRepository.pruefziele()).thenReturn(List.of(ziele));
  }

  @Test
  @DisplayName("Vor dem ersten Durchgang gibt es keinen Stand")
  void kein_stand_vor_dem_ersten_durchgang() {
    assertThat(pruefung().letzterStand()).isEmpty();
  }

  @Test
  @DisplayName("„Datei nicht vorhanden\" heisst erreichbar — das ist der Befund aus M174")
  void datei_nicht_vorhanden_heisst_erreichbar() {
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A));
    when(ablagezugriff.hole(eq(ADRESSE_A), any())).thenReturn(Abrufergebnis.nichtVorhanden());

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();

    assertThat(pruefung.letzterStand()).isPresent();
    assertThat(pruefung.letzterStand().orElseThrow().ziele())
        .containsExactly(new Zielstand(ZIEL_A, Ablagenzustand.ERREICHBAR));
  }

  @Test
  @DisplayName("„Ablage nicht erreichbar\" heisst nicht erreichbar")
  void nicht_erreichbar_bleibt_nicht_erreichbar() {
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A));
    when(ablagezugriff.hole(eq(ADRESSE_A), any())).thenReturn(Abrufergebnis.nichtErreichbar());

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();

    assertThat(pruefung.letzterStand().orElseThrow().ziele())
        .containsExactly(new Zielstand(ZIEL_A, Ablagenzustand.NICHT_ERREICHBAR));
  }

  @Test
  @DisplayName("Gelieferte Daten heissen ungeklaert und nicht erreichbar")
  void gelieferte_daten_heissen_ungeklaert() {
    // Die Null-UUID zeigt mit Sicherheit auf keine Datei. Kommt trotzdem eine, verhaelt sich der
    // Knoten anders als gemessen -- das gehoert gezeigt und nicht als "alles gut" eingeordnet.
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A));
    when(ablagezugriff.hole(eq(ADRESSE_A), any()))
        .thenReturn(Abrufergebnis.geholt(new byte[] {1, 2, 3}));

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();

    assertThat(pruefung.letzterStand().orElseThrow().ziele())
        .containsExactly(new Zielstand(ZIEL_A, Ablagenzustand.UNGEKLAERT));
  }

  @Test
  @DisplayName("Gefragt wird mit der Null-UUID und nie mit einer echten Kennung")
  void gefragt_wird_mit_der_null_uuid() {
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A));
    when(ablagezugriff.hole(any(), any())).thenReturn(Abrufergebnis.nichtVorhanden());

    pruefung().pruefe();

    verify(ablagezugriff).hole(ADRESSE_A, "00000000-0000-0000-0000-000000000000");
  }

  @Test
  @DisplayName("Ein nicht aufloesbares Ziel heisst nicht erreichbar — und wird nicht gefragt")
  void nicht_aufloesbares_ziel() {
    zieleSind(new Ablagenziel(ZIEL_A, null), new Ablagenziel(ZIEL_B, "   "));

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();

    assertThat(pruefung.letzterStand().orElseThrow().ziele())
        .containsExactly(
            new Zielstand(ZIEL_A, Ablagenzustand.NICHT_ERREICHBAR),
            new Zielstand(ZIEL_B, Ablagenzustand.NICHT_ERREICHBAR));
    verify(ablagezugriff, never()).hole(any(), any());
  }

  @Test
  @DisplayName("Mehrere Ziele werden einzeln gefragt, in der Reihenfolge der Kennungen")
  void mehrere_ziele() {
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A), new Ablagenziel(ZIEL_B, ADRESSE_B));
    when(ablagezugriff.hole(eq(ADRESSE_A), any())).thenReturn(Abrufergebnis.nichtVorhanden());
    when(ablagezugriff.hole(eq(ADRESSE_B), any())).thenReturn(Abrufergebnis.nichtErreichbar());

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();

    assertThat(pruefung.letzterStand().orElseThrow().ziele())
        .containsExactly(
            new Zielstand(ZIEL_A, Ablagenzustand.ERREICHBAR),
            new Zielstand(ZIEL_B, Ablagenzustand.NICHT_ERREICHBAR));
  }

  @Test
  @DisplayName("Der Pruefzeitpunkt kommt aus der Anwendungsuhr (Regel Z1)")
  void pruefzeitpunkt_aus_der_anwendungsuhr() {
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A));
    when(ablagezugriff.hole(any(), any())).thenReturn(Abrufergebnis.nichtVorhanden());

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();

    // 04:09:47Z in Europe/Berlin ist im Dezember 05:09:47 Ortszeit -- und Ortszeit ist, was die
    // Anwendungsuhr liefert.
    assertThat(pruefung.letzterStand().orElseThrow().geprueftAm())
        .isEqualTo(LocalDateTime.parse("2025-12-30T05:09:47"));
  }

  @Test
  @DisplayName("Die Ziele werden je Durchgang neu gelesen und nicht einmal beim Start")
  void ziele_je_durchgang_neu() {
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A));
    when(ablagezugriff.hole(any(), any())).thenReturn(Abrufergebnis.nichtVorhanden());

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();
    // Der Eintrag wechselt -- genau der Vorgang, bei dem eine Ueberwachung nicht auf dem alten
    // Ziel stehen bleiben darf.
    zieleSind(new Ablagenziel(ZIEL_B, ADRESSE_B));
    when(ablagezugriff.hole(eq(ADRESSE_B), any())).thenReturn(Abrufergebnis.nichtErreichbar());
    pruefung.pruefe();

    assertThat(pruefung.letzterStand().orElseThrow().ziele())
        .containsExactly(new Zielstand(ZIEL_B, Ablagenzustand.NICHT_ERREICHBAR));
  }

  @Test
  @DisplayName("Scheitert der Durchgang, bleibt der alte Stand stehen und veraltet von selbst")
  void gescheiterter_durchgang_setzt_keinen_stand() {
    zieleSind(new Ablagenziel(ZIEL_A, ADRESSE_A));
    when(ablagezugriff.hole(any(), any())).thenReturn(Abrufergebnis.nichtVorhanden());
    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();
    Ablagenstand erster = pruefung.letzterStand().orElseThrow();

    when(dienstLeseRepository.pruefziele()).thenThrow(new IllegalStateException("erfunden"));
    pruefung.pruefe();

    assertThat(pruefung.letzterStand())
        .as("Ein leerer Stand machte aus einem Datenbankfehler eine Aussage ueber die Ablagen")
        .contains(erster);
  }

  @Test
  @DisplayName("Ohne eingetragenes Ziel entsteht ein Stand mit leerer Zielliste")
  void kein_ziel_eingetragen() {
    // Er ist etwas anderes als "kein Durchgang": Die Kachel unterscheidet beide Gruende.
    zieleSind();

    Ablagenpruefung pruefung = pruefung();
    pruefung.pruefe();

    assertThat(pruefung.letzterStand()).isPresent();
    assertThat(pruefung.letzterStand().orElseThrow().ziele()).isEmpty();
  }

  @Test
  @DisplayName("Der Takt kommt aus den Eigenschaften, die Vorgabe sind 60 Sekunden")
  void takt_aus_den_eigenschaften() {
    assertThat(pruefung().takt()).isEqualTo(TAKT);
    assertThat(new AblagenpruefungEigenschaften(true, null).takt())
        .isEqualTo(AblagenpruefungEigenschaften.VORGABE_TAKT);
    assertThat(new AblagenpruefungEigenschaften(true, Duration.ZERO).takt())
        .as("Ein Takt von null waere ein Dauerlauf gegen fremde Knoten")
        .isEqualTo(AblagenpruefungEigenschaften.VORGABE_TAKT);
  }
}
