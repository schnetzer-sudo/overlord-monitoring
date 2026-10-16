package de.kraftwerkone.overlord.monitor.assistent;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Schalter, Anbieter und Grenzen des Assistenten ({@code overlord.assistent}, {@code
 * docs/assistent.md} §7). Anbieterspezifisches steht beim Adapter.
 *
 * @param aktiv der Schalter (E‑1030). <b>Vorgabe aus</b>, in jedem Profil; ohne Schluessel gilt der
 *     Assistent ebenfalls als aus
 * @param anbieter welcher Adapter, heute nur {@code anthropic}
 * @param modell die Modellkennung beim Anbieter
 * @param apiKey der Schluessel, nur aus der Umgebung (Regel G1)
 * @param basisUrl das Ziel der Aufrufe
 * @param zeitgrenze je Aufruf des Modells
 * @param maxTokens Obergrenze der Ausgabe je Aufruf
 * @param nachrichtHoechstensZeichen Laenge einer Nachricht des Kunden
 * @param nachrichtenJeGespraech Nachrichten des Kunden je Gespraech
 * @param werkzeugrundenJeNachricht Werkzeugrunden je Nachricht
 * @param nachrichtenJeStunde je Nutzer, gleitend ueber die letzte Stunde
 * @param gespraechAblauf Ablauf eines Gespraechs ohne Nachricht
 * @param gespraecheJeSitzung offene Gespraeche je Sitzung
 */
@ConfigurationProperties("overlord.assistent")
public record AssistentEigenschaften(
    boolean aktiv,
    String anbieter,
    String modell,
    String apiKey,
    String basisUrl,
    Duration zeitgrenze,
    int maxTokens,
    int nachrichtHoechstensZeichen,
    int nachrichtenJeGespraech,
    int werkzeugrundenJeNachricht,
    int nachrichtenJeStunde,
    Duration gespraechAblauf,
    int gespraecheJeSitzung) {

  /** Die Vorgaben aus E‑1036 und E‑1038, falls nichts konfiguriert ist. */
  public AssistentEigenschaften {
    anbieter = leer(anbieter) ? "anthropic" : anbieter;
    modell = leer(modell) ? "claude-sonnet-5-5" : modell;
    basisUrl = leer(basisUrl) ? "https://api.anthropic.com" : basisUrl;
    zeitgrenze = zeitgrenze == null ? Duration.ofSeconds(30) : zeitgrenze;
    maxTokens = maxTokens <= 0 ? 1024 : maxTokens;
    nachrichtHoechstensZeichen =
        nachrichtHoechstensZeichen <= 0 ? 4000 : nachrichtHoechstensZeichen;
    nachrichtenJeGespraech = nachrichtenJeGespraech <= 0 ? 20 : nachrichtenJeGespraech;
    werkzeugrundenJeNachricht = werkzeugrundenJeNachricht <= 0 ? 3 : werkzeugrundenJeNachricht;
    nachrichtenJeStunde = nachrichtenJeStunde <= 0 ? 60 : nachrichtenJeStunde;
    gespraechAblauf = gespraechAblauf == null ? Duration.ofMinutes(30) : gespraechAblauf;
    gespraecheJeSitzung = gespraecheJeSitzung <= 0 ? 3 : gespraecheJeSitzung;
  }

  /** Schalter an <b>und</b> Schluessel gesetzt (E‑1030). Sonst geht kein Aufruf hinaus. */
  public boolean eingeschaltet() {
    return aktiv && !leer(apiKey);
  }

  /** Ohne den Schluessel: {@code record.toString} nennte ihn sonst in jedem Protokoll. */
  @Override
  public String toString() {
    return "AssistentEigenschaften[aktiv="
        + aktiv
        + ", anbieter="
        + anbieter
        + ", modell="
        + modell
        + ", schluessel="
        + (leer(apiKey) ? "fehlt" : "gesetzt")
        + "]";
  }

  private static boolean leer(String wert) {
    return wert == null || wert.isBlank();
  }
}
