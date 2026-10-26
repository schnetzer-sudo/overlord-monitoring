package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicEigenschaften;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * <b>Produktion und Testsatzlaeufer fahren ohne Umgebungsvariable dasselbe</b> (E‑1153, E‑1154):
 * {@code denken} und {@code max-tokens} stehen in {@code application.yml} gleich den Vorgaben der
 * Eigenschaften und gleich {@link AssistentTestsatzLauf#MAX_TOKENS}. Der Laeufer liest {@code
 * application.yml} nicht; ohne diesen Test liefe er nach einer Aenderung an nur einer Stelle still
 * mit anderen Einstellungen als die Produktion.
 *
 * <p>Bauform wie {@code FlywayOutOfOrderTest}: {@code application.yml} wird direkt gebunden, ohne
 * Anwendungskontext, ohne Datenbank und ohne Umgebungsvariablen.
 */
class AssistentVorgabenTest {

  @ParameterizedTest
  @ValueSource(strings = {"dev", "prod"})
  @DisplayName("denken: adaptive und max-tokens: 8192, in application.yml, Vorgaben und Laeufer")
  void gleiche_vorgaben(String profil) {
    Binder binder = Binder.get(umgebung(profil));
    AnthropicEigenschaften anthropic =
        binder
            .bind("overlord.assistent.anthropic", AnthropicEigenschaften.class)
            .orElseThrow(() -> new AssertionError("overlord.assistent.anthropic fehlt"));
    AssistentEigenschaften assistent =
        binder
            .bind("overlord.assistent", AssistentEigenschaften.class)
            .orElseThrow(() -> new AssertionError("overlord.assistent fehlt"));
    AnthropicEigenschaften ohneKonfiguration = new AnthropicEigenschaften(null, null);
    AssistentEigenschaften ohneWerte =
        new AssistentEigenschaften(false, null, null, null, null, null, 0, 0, 0, 0, 0, null, 0);

    assertThat(anthropic.denken()).isEqualTo("adaptive").isEqualTo(ohneKonfiguration.denken());
    assertThat(assistent.maxTokens())
        .isEqualTo(8192)
        .isEqualTo(ohneWerte.maxTokens())
        .isEqualTo(AssistentTestsatzLauf.MAX_TOKENS);
  }

  /** {@code application.yml} fuer genau ein Profil, ohne Systemeigenschaften und Umgebung. */
  private static ConfigurableEnvironment umgebung(String profil) {
    StandardEnvironment umgebung = new StandardEnvironment();
    umgebung
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    umgebung
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    umgebung.setActiveProfiles(profil);
    ConfigDataEnvironmentPostProcessor.applyTo(umgebung, null, null, profil);
    assertThat(umgebung.getActiveProfiles()).containsExactly(profil);
    return umgebung;
  }
}
