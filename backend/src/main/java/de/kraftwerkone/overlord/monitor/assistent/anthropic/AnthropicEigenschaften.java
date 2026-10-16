package de.kraftwerkone.overlord.monitor.assistent.anthropic;

import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Was nur der Adapter fuer Anthropic braucht ({@code overlord.assistent.anthropic}, {@code
 * docs/assistent.md} §7). Alles Anbieterneutrale steht in {@code AssistentEigenschaften}.
 *
 * @param denken was im Feld {@code thinking} steht: {@code between_tools} (Vorgabe, das Modell
 *     denkt nicht ausfuehrlich — nur Claude Sonnet 5.5 kennt den Wert), {@code adaptive} oder
 *     {@code keins} (das Feld entfaellt). Denken zaehlt gegen {@code max_tokens}; bei 1.024 bliebe
 *     sonst wenig fuer Antwort und Vorschlag (E‑1046)
 * @param wiederholungPause Pause vor dem einen Wiederholungsversuch bei {@code 429} und {@code 529}
 */
@ConfigurationProperties("overlord.assistent.anthropic")
public record AnthropicEigenschaften(String denken, Duration wiederholungPause) {

  public AnthropicEigenschaften {
    denken = denken == null || denken.isBlank() ? "between_tools" : denken.toLowerCase(Locale.ROOT);
    if (wiederholungPause == null || wiederholungPause.isNegative()) {
      wiederholungPause = Duration.ofSeconds(1);
    }
  }
}
