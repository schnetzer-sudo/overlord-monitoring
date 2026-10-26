package de.kraftwerkone.overlord.monitor.assistent.anthropic;

import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Was nur der Adapter fuer Anthropic braucht ({@code overlord.assistent.anthropic}, {@code
 * docs/assistent.md} §7). Alles Anbieterneutrale steht in {@code AssistentEigenschaften}.
 *
 * @param denken was im Feld {@code thinking} steht: {@code adaptive} (Vorgabe, das Modell denkt
 *     verborgen, auch vor einer reinen Textantwort, E‑1153), {@code between_tools} (kein
 *     ausfuehrliches Denken — nur Claude Sonnet 5.5 kennt den Wert, Vorgabe bis E‑1153) oder {@code
 *     keins} (das Feld entfaellt). Denken zaehlt gegen {@code max_tokens} (E‑1154). Die Vorgabe
 *     steht gleich in {@code application.yml}, damit der Testsatzlaeufer ohne Umgebungsvariable
 *     dasselbe faehrt wie die Produktion
 * @param wiederholungPause Pause vor dem einen Wiederholungsversuch bei {@code 429} und {@code 529}
 */
@ConfigurationProperties("overlord.assistent.anthropic")
public record AnthropicEigenschaften(String denken, Duration wiederholungPause) {

  public AnthropicEigenschaften {
    denken = denken == null || denken.isBlank() ? "adaptive" : denken.toLowerCase(Locale.ROOT);
    if (wiederholungPause == null || wiederholungPause.isNegative()) {
      wiederholungPause = Duration.ofSeconds(1);
    }
  }
}
