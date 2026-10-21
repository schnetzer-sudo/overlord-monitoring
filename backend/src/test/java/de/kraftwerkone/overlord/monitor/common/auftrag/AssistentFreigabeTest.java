package de.kraftwerkone.overlord.monitor.common.auftrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Die eine Stelle, die je Mandant ueber den Assistenten entscheidet ({@code docs/tasks.md} §3.9,
 * E‑1126, E‑1130): eingeschaltet und mindestens ein angebotener Typ, den der Assistent anbieten
 * kann. Ohne Datenbank; den Typkatalog ersetzt ein Mock.
 */
class AssistentFreigabeTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");

  private static final TypKatalog.Angebot WERK =
      new TypKatalog.Angebot(Testdefinitionen.werk(), Testdefinitionen.werkStruktur());
  private static final TypKatalog.Angebot GATTUNG =
      new TypKatalog.Angebot(Testdefinitionen.gattung(), Testdefinitionen.GATTUNG_STRUKTUR);

  /** Steht hier fuer {@code Werkzeugbau::geeignet}: nur {@code WERK_ABLADESTELLE} ist anbietbar. */
  private static final Predicate<TypDefinition> NUR_WERK =
      typ -> typ.typ().equals(Testdefinitionen.WERK);

  private TypKatalog katalog;

  @BeforeEach
  void katalog() {
    katalog = mock(TypKatalog.class);
  }

  @Test
  @DisplayName("Ausgeschaltet: nichts angeboten, Gespraeche und Anlegen 404, kein Datenbankzugriff")
  void ausgeschaltet() {
    AssistentFreigabe freigabe = new AssistentFreigabe(false, katalog, typ -> true);

    assertThat(freigabe.angebot(MANDANT)).isEmpty();
    assertThatThrownBy(() -> freigabe.verlangeAngebot(MANDANT)).satisfies(this::istNichtGefunden);
    assertThatThrownBy(() -> freigabe.verlangeKatalog(MANDANT)).satisfies(this::istNichtGefunden);
    verifyNoInteractions(katalog);
  }

  @Test
  @DisplayName("Eingeschaltet, ohne freigeschalteten Typ: nicht verfuegbar")
  void ohne_freischaltung() {
    when(katalog.angeboten(any(MandantContext.class))).thenReturn(List.of());
    AssistentFreigabe freigabe = new AssistentFreigabe(true, katalog, typ -> true);

    assertThat(freigabe.angebot(MANDANT)).isEmpty();
    assertThatThrownBy(() -> freigabe.verlangeAngebot(MANDANT)).satisfies(this::istNichtGefunden);
    assertThatThrownBy(() -> freigabe.verlangeKatalog(MANDANT)).satisfies(this::istNichtGefunden);
  }

  @Test
  @DisplayName(
      "Nur Typen, die der Assistent nicht anbieten kann: nicht verfuegbar, auch fuer das Anlegen —"
          + " eine Entscheidung, nicht zwei")
  void nur_unanbietbare_typen() {
    when(katalog.angeboten(any(MandantContext.class))).thenReturn(List.of(GATTUNG));
    AssistentFreigabe freigabe = new AssistentFreigabe(true, katalog, NUR_WERK);

    assertThat(freigabe.angebot(MANDANT)).isEmpty();
    assertThatThrownBy(() -> freigabe.verlangeKatalog(MANDANT)).satisfies(this::istNichtGefunden);
  }

  @Test
  @DisplayName(
      "Verfuegbar: der Assistent bietet nur das Anbietbare an, das Anlegen waehlt aus dem ganzen"
          + " Katalog (E‑1054)")
  void verfuegbar() {
    when(katalog.angeboten(any(MandantContext.class))).thenReturn(List.of(GATTUNG, WERK));
    AssistentFreigabe freigabe = new AssistentFreigabe(true, katalog, NUR_WERK);

    assertThat(freigabe.angebot(MANDANT)).containsExactly(WERK.definition());
    assertThat(freigabe.verlangeAngebot(MANDANT)).containsExactly(WERK.definition());
    assertThat(freigabe.verlangeKatalog(MANDANT)).containsExactly(GATTUNG, WERK);
  }

  /**
   * Derselbe feste Rumpf wie fuer jede unbekannte Ressource — wie ein nicht zulaessiger Mandant.
   */
  private void istNichtGefunden(Throwable fehler) {
    assertThat(fehler).isInstanceOf(RessourceNichtGefundenException.class);
    RessourceNichtGefundenException nichtGefunden = (RessourceNichtGefundenException) fehler;
    assertThat(nichtGefunden.status()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(nichtGefunden.problemTyp()).isEqualTo("nicht-gefunden");
  }
}
