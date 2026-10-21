package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.assistent.AssistentEigenschaften;
import de.kraftwerkone.overlord.monitor.assistent.Werkzeugbau;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicEigenschaften;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicSprachmodell;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.common.auftrag.AssistentFreigabe;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypKatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verdrahtung des Assistenten: welcher Adapter hinter {@link Sprachmodell} steht ({@code
 * docs/assistent.md} §5), und die Freigabe je Mandant ({@link AssistentFreigabe}, {@code
 * docs/tasks.md} §3.9).
 *
 * <p><b>Nur hier wird der Adapter gewaehlt</b>, nach {@code overlord.assistent.anbieter}. Die
 * Gespraechsfuehrung kennt nur das Interface; ein neuer Anbieter ist ein neues Unterpaket und ein
 * Zweig hier.
 *
 * <p><b>Abgeschaltet</b> (Schalter aus oder kein Schluessel, E‑1030) gibt es keinen Adapter,
 * sondern {@link Sprachmodell#ABGESCHALTET}: Selbst ein Fehler in der Gespraechsfuehrung fuehrte
 * dann nicht zu einem Aufruf beim Anbieter.
 */
@Configuration
@EnableConfigurationProperties({AssistentEigenschaften.class, AnthropicEigenschaften.class})
public class AssistentConfig {

  private static final Logger log = LoggerFactory.getLogger(AssistentConfig.class);

  @Bean
  public Sprachmodell sprachmodell(
      AssistentEigenschaften eigenschaften, AnthropicEigenschaften anthropic, JsonMapper json) {
    if (!eigenschaften.eingeschaltet()) {
      log.info(
          "Assistent fuer alle Mandanten aus: {} ({})",
          eigenschaften.aktiv() ? "kein Schluessel" : "Schalter aus",
          eigenschaften);
      return Sprachmodell.ABGESCHALTET;
    }
    return switch (eigenschaften.anbieter()) {
      case "anthropic" -> {
        log.warn(
            "Assistent eingeschaltet ({}): Nachrichten der Kunden gehen an {}",
            eigenschaften,
            eigenschaften.basisUrl());
        yield new AnthropicSprachmodell(
            eigenschaften.basisUrl(),
            eigenschaften.apiKey(),
            eigenschaften.modell(),
            eigenschaften.zeitgrenze(),
            anthropic,
            json);
      }
      default ->
          throw new IllegalStateException(
              "Unbekannter Anbieter fuer den Assistenten: " + eigenschaften.anbieter());
    };
  }

  /**
   * Die eine Stelle, die je Mandant ueber den Assistenten entscheidet (E‑1126, E‑1130). Hier, weil
   * nur {@code config} beides sieht: was „eingeschaltet“ heisst ({@code assistent}) und den
   * Typkatalog ({@code common}). Anbietbar ist, was {@link Werkzeugbau#geeignet} als Werkzeug
   * annimmt (E‑1054).
   */
  @Bean
  public AssistentFreigabe assistentFreigabe(
      AssistentEigenschaften eigenschaften, TypKatalog typKatalog) {
    return new AssistentFreigabe(eigenschaften.eingeschaltet(), typKatalog, Werkzeugbau::geeignet);
  }
}
