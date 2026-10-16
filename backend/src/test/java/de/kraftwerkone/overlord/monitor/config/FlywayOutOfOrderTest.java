package de.kraftwerkone.overlord.monitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Flyway laeuft nur im Profil {@code dev} mit {@code out-of-order} — und ignoriert dort wie
 * ueberall fremde, noch unbekannte Migrationen.
 *
 * <p>{@code main} und {@code assistent} teilen sich die Testkopie; die Assistent-Linie zaehlt ihre
 * Migrationen ab {@code V100} (Entscheidung 6 vom 16.10.2026, {@code DEVELOPMENT_GUIDELINES.md}
 * §12). Ein Fix auf {@code main} mit einer niedrigeren Nummer liegt dann unter einer bereits
 * angewandten {@code V100}; ohne {@code out-of-order} scheitert der lokale Start an der
 * Validierung. In der Produktion laeuft nur eine Linie, dort bleibt die strenge Reihenfolge. Ohne
 * diesen Test wandert die Lockerung irgendwann still in ein anderes Profil.
 *
 * <p>Die Gegenrichtung traegt eine Vorgabe, die nirgends gesetzt ist: {@code
 * ignore-migration-patterns} ist {@code *:future} (Flyway 12.4.0 und Boot 4.1.0). Damit startet
 * {@code main} gegen eine Testkopie, auf der {@code assistent} schon {@code V100} angewandt hat.
 * Der Test haelt fest, dass niemand sie ueberschreibt.
 *
 * <p>Bauform wie {@link CookieAttributeTest}: {@code application.yml} wird direkt gebunden, ohne
 * Anwendungskontext und ohne Datenbank, und das Profil bestimmt der Test selbst.
 */
class FlywayOutOfOrderTest {

  /** Bindet {@code spring.flyway} fuer genau ein Profil, mit Boots eigenen Vorgabewerten. */
  private static FlywayProperties flywayImProfil(String profil) {
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
        .bind("spring.flyway", Bindable.ofInstance(new FlywayProperties()))
        .orElseThrow(
            () -> new AssertionError("spring.flyway ist im Profil " + profil + " nicht gesetzt"));
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
  @DisplayName("Im Profil dev ist out-of-order gesetzt")
  void dev_mit_out_of_order() {
    assertThat(flywayImProfil("dev").isOutOfOrder()).isTrue();
  }

  @Test
  @DisplayName("Im Profil prod ist out-of-order nicht gesetzt")
  void prod_ohne_out_of_order() {
    assertThat(flywayImProfil("prod").isOutOfOrder()).isFalse();
  }

  @Test
  @DisplayName("Auch in den uebrigen Profilen ist out-of-order nicht gesetzt")
  void uebrige_profile_ohne_out_of_order() {
    // rollup und bootstrap sind die Profile, an denen Beans haengen; ein beliebiges neues Profil
    // erbt die strenge Reihenfolge.
    assertThat(flywayImProfil("rollup").isOutOfOrder()).isFalse();
    assertThat(flywayImProfil("bootstrap").isOutOfOrder()).isFalse();
    assertThat(flywayImProfil("unbekannt").isOutOfOrder()).isFalse();
  }

  @Test
  @DisplayName("Fremde, noch unbekannte Migrationen werden in dev und prod ignoriert")
  void zukuenftige_migrationen_werden_ignoriert() {
    assertThat(flywayImProfil("dev").getIgnoreMigrationPatterns()).containsExactly("*:future");
    assertThat(flywayImProfil("prod").getIgnoreMigrationPatterns()).containsExactly("*:future");
  }

  /** Wie in {@link CookieAttributeTest}: {@code SPRING_PROFILES_ACTIVE=dev} wirkt nicht hinein. */
  @Test
  @DisplayName("Ein aktives Profil aus der Umgebung aendert das Ergebnis nicht")
  void umgebungszustand_wirkt_nicht() {
    String vorher = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
    System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "dev");
    try {
      assertThat(flywayImProfil("prod").isOutOfOrder()).isFalse();
    } finally {
      if (vorher == null) {
        System.clearProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
      } else {
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, vorher);
      }
    }
  }
}
