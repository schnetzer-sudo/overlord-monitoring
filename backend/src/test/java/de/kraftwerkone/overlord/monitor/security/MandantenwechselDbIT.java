package de.kraftwerkone.overlord.monitor.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Der Mandantenwechsel — die einzige Stelle im System, die eine Mandanten-ID entgegennimmt und
 * damit einen Datenausschnitt bestimmt.
 *
 * <p>Geprueft wird die <b>Menge</b>, nicht die Rolle. Deshalb steht ADMIN hier nicht als
 * Sonderfall, sondern als der Nutzer mit der groessten Menge.
 */
class MandantenwechselDbIT extends SicherheitsTestbasis {

  private static final String PASSWORT = "einLangesPasswort1";

  @Test
  @DisplayName("Ein Nutzer mit zwei Mandanten waehlt selbst")
  void zwei_mandanten_zulaessig() throws Exception {
    String nutzer = PRAEFIX + "wechsel-zwei";
    legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, MANDANT_A, MANDANT_B);

    Sitzung sitzung = anmelden(nutzer, PASSWORT);
    // Zwei zulaessige Mandanten: die Auswahl bleibt beim Anmelden offen.
    assertThat(sitzung.hole("/api/auth/me").hatFeld("$.mandant")).isFalse();
    assertThat(sitzung.hole("/api/mandanten").<List<String>>json("$[*].id"))
        .containsExactlyInAnyOrder(MANDANT_A, MANDANT_B);

    Antwort gewechselt = sitzung.sende("/api/auth/mandant", wechsel(MANDANT_B));
    assertThat(gewechselt.status()).isEqualTo(200);
    assertThat(gewechselt.<String>json("$.mandant.id")).isEqualTo(MANDANT_B);

    assertThat(sitzung.hole("/api/auth/me").<String>json("$.mandant.id")).isEqualTo(MANDANT_B);
  }

  @Test
  @DisplayName("Ein ADMIN sieht alle Mandanten, aber keinen aktiven, bis er waehlt")
  void admin_sieht_alle_und_waehlt() throws Exception {
    String admin = PRAEFIX + "wechsel-admin";
    // Bewusst ohne Zuordnung in app_user_mandant: Fuer ADMIN ergibt sich die Menge aus
    // GlassfishDB.Mandant.
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);

    Sitzung sitzung = anmelden(admin, PASSWORT);
    assertThat(sitzung.hole("/api/auth/me").hatFeld("$.mandant")).isFalse();

    int anzahl = mandantRepository.findeAlle().size();
    assertThat(anzahl).as("Die Testkopie kennt Mandanten").isPositive();
    assertThat(sitzung.hole("/api/mandanten").<List<String>>json("$[*].id")).hasSize(anzahl);

    Antwort gewechselt = sitzung.sende("/api/auth/mandant", wechsel(MANDANT_B));
    assertThat(gewechselt.status()).isEqualTo(200);
    assertThat(gewechselt.<String>json("$.mandant.id")).isEqualTo(MANDANT_B);
  }

  @Test
  @DisplayName("Ohne Anmeldung gibt es weder Liste noch Wechsel")
  void ohne_anmeldung_kein_zugriff() throws Exception {
    Sitzung sitzung = neueSitzung();

    Antwort liste = sitzung.hole("/api/mandanten");
    assertThat(liste.status()).isEqualTo(401);
    assertThat(liste.<String>json("$.title")).isEqualTo("Nicht angemeldet");
    assertThat(sitzung.sende("/api/auth/mandant", wechsel(MANDANT_A)).status()).isEqualTo(401);
  }

  private static String wechsel(String mandantId) {
    return """
        {"mandantId":"%s"}"""
        .formatted(mandantId);
  }
}
