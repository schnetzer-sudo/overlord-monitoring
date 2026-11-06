package de.kraftwerkone.overlord.monitor.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Die Einordnung des Erstellers ({@code docs/tasks.md} §6.5, F55). */
class AngelegtVonTest {

  private static final String KENNUNG = "beispielnutzer";

  @Test
  @DisplayName("Rolle MANDANT: BENUTZER mit dem Benutzernamen")
  void benutzer() {
    assertThat(AngelegtVon.einordnen(KENNUNG, Rolle.MANDANT.name()))
        .isEqualTo(new AngelegtVon(AngelegtVon.Art.BENUTZER, KENNUNG));
  }

  @Test
  @DisplayName("Rolle ADMIN: ADMIN, der Benutzername bleibt im Backend")
  void admin() {
    assertThat(AngelegtVon.einordnen(KENNUNG, Rolle.ADMIN.name()))
        .isEqualTo(new AngelegtVon(AngelegtVon.Art.ADMIN, null));
  }

  @Test
  @DisplayName("Kein Konto: UNBEKANNT mit dem gespeicherten Wert")
  void unbekannt() {
    assertThat(AngelegtVon.einordnen(KENNUNG, null))
        .isEqualTo(new AngelegtVon(AngelegtVon.Art.UNBEKANNT, KENNUNG));
  }

  @Test
  @DisplayName("Eine fremde Rolle ist nicht ADMIN und wirft nicht: BENUTZER")
  void fremde_rolle() {
    assertThat(AngelegtVon.einordnen(KENNUNG, "GIBT_ES_NICHT"))
        .isEqualTo(new AngelegtVon(AngelegtVon.Art.BENUTZER, KENNUNG));
  }
}
