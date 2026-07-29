package de.kraftwerkone.overlord.monitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
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
 */
class CookieAttributeTest {

  private static final String SCHLUESSEL = "server.servlet.session.cookie.secure";

  private static boolean secureImProfil(String profil) {
    ConfigurableEnvironment umgebung = new StandardEnvironment();
    ConfigDataEnvironmentPostProcessor.applyTo(umgebung, null, null, profil);
    return Binder.get(umgebung)
        .bind(SCHLUESSEL, Boolean.class)
        .orElseThrow(
            () -> new AssertionError(SCHLUESSEL + " ist im Profil " + profil + " nicht gesetzt"));
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
}
