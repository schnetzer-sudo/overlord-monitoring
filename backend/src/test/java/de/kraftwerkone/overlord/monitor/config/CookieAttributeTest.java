package de.kraftwerkone.overlord.monitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Das Attribut {@code Secure} am Sitzungs-Cookie ist per Profil schaltbar — und nur im Profil
 * {@code dev} aus.
 *
 * <p>Ohne diese Schaltbarkeit funktioniert die lokale Entwicklung ueber {@code http://localhost}
 * nicht, weil der Browser ein {@code Secure}-Cookie dort nicht mitschickt. Ohne diesen Test
 * verschwindet die Ausnahme fuer {@code dev} irgendwann still in die Produktion.
 *
 * <p>Der Test bindet {@code application.yml} direkt und startet <b>keinen</b> Anwendungskontext: Er
 * muss auch ohne Datenbank laufen.
 *
 * <p><b>Der Test bestimmt das Profil selbst, statt es vorzufinden.</b> Ein {@link
 * StandardEnvironment} bringt {@code systemProperties} und {@code systemEnvironment} mit. Steht in
 * der Shell {@code SPRING_PROFILES_ACTIVE=dev} — auf einem Entwicklungsrechner der Normalfall —,
 * dann war neben dem geprueften Profil auch {@code dev} aktiv, dessen Dokument in {@code
 * application.yml} weiter unten steht und {@code secure: false} gewann. Der Test schlug also auf
 * genau den Rechnern fehl, auf denen entwickelt wird, und belegte im Erfolgsfall nur, dass die
 * Variable gerade nicht gesetzt war. Deshalb werden beide Quellen entfernt, bevor irgendetwas
 * geladen wird.
 */
class CookieAttributeTest {

  private static final String SCHLUESSEL = "server.servlet.session.cookie.secure";

  /**
   * Bindet {@code application.yml} fuer genau ein Profil — und nur fuer dieses.
   *
   * <p>Die Zusicherung auf {@code getActiveProfiles()} ist kein Beiwerk: Sie ist die Stelle, an der
   * ein zurueckkehrender Umgebungseinfluss auffliegt. Ohne sie liefe der Test weiter und pruefte
   * stillschweigend eine andere Konfiguration als die benannte.
   */
  private static boolean secureImProfil(String profil) {
    ConfigurableEnvironment umgebung = ohneUmgebungszustand();
    umgebung.setActiveProfiles(profil);
    ConfigDataEnvironmentPostProcessor.applyTo(umgebung, null, null, profil);

    assertThat(umgebung.getActiveProfiles())
        .as(
            "Es soll ausschliesslich '%s' aktiv sein. Ist hier mehr aktiv, stammt es aus der"
                + " Umgebung und der Test prueft eine andere Konfiguration als die benannte.",
            profil)
        .containsExactly(profil);

    return Binder.get(umgebung)
        .bind(SCHLUESSEL, Boolean.class)
        .orElseThrow(
            () -> new AssertionError(SCHLUESSEL + " ist im Profil " + profil + " nicht gesetzt"));
  }

  /** Eine Umgebung ohne {@code systemProperties} und ohne {@code systemEnvironment}. */
  private static ConfigurableEnvironment ohneUmgebungszustand() {
    StandardEnvironment umgebung = new StandardEnvironment();
    umgebung
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    umgebung
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    return umgebung;
  }

  @Test
  @DisplayName("Im Profil dev ist Secure nicht gesetzt")
  void dev_ohne_secure() {
    assertThat(secureImProfil("dev")).isFalse();
  }

  @Test
  @DisplayName("Im Profil prod ist Secure gesetzt")
  void prod_mit_secure() {
    assertThat(secureImProfil("prod")).isTrue();
  }

  @Test
  @DisplayName("Auch in einem beliebigen anderen Profil ist Secure gesetzt")
  void unbekanntes_profil_mit_secure() {
    // Der Standard ist AN. Nur dev schaltet ab — ein neues Profil erbt damit die sichere
    // Einstellung, statt sie zu vergessen.
    assertThat(secureImProfil("bootstrap")).isTrue();
  }

  /**
   * Die Absicherung der Absicherung.
   *
   * <p>{@code spring.profiles.active} als Systemeigenschaft ist der prozessinterne Zwilling der
   * Umgebungsvariablen {@code SPRING_PROFILES_ACTIVE} — Spring liest beide unter demselben Namen.
   * Dieser Test setzt sie auf {@code dev} und verlangt, dass die Pruefung fuer {@code prod}
   * trotzdem {@code true} liefert. Er waere der Test, der den urspruenglichen Fehler gefunden
   * haette.
   */
  @Test
  @DisplayName("Ein aktives Profil aus der Umgebung aendert das Ergebnis nicht")
  void umgebungszustand_wirkt_nicht() {
    String vorher = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
    System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "dev");
    try {
      assertThat(secureImProfil("prod")).isTrue();
      assertThat(secureImProfil("bootstrap")).isTrue();
    } finally {
      if (vorher == null) {
        System.clearProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
      } else {
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, vorher);
      }
    }
  }
}
