package de.kraftwerkone.overlord.monitor.admin;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.MandantResponse;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code POST /api/admin/users} — Anlegen eines Kontos, ausschliesslich fuer ADMIN.
 *
 * <p>Ohne diesen Endpunkt gaebe es keinen Nutzer der Rolle MANDANT und damit auch keinen
 * Isolationstest.
 */
class AdminUserDbIT extends SicherheitsTestbasis {

  private static final String ADMIN = PRAEFIX + "admin";
  private static final String KUNDE = PRAEFIX + "kunde";
  private static final String PASSWORT = "einLangesPasswort1";
  private static final String NEUES_KONTO = PRAEFIX + "neu";
  private static final String EINMALPASSWORT = "einmalPasswort99";

  private static String anlegen(String username, String rolle, String mandantId) {
    return """
        {"username":"%s","role":"%s","mandantId":"%s","initialPassword":"%s"}"""
        .formatted(username, rolle, mandantId, EINMALPASSWORT);
  }

  private Sitzung alsAdmin() throws Exception {
    legeNutzerAn(ADMIN, PASSWORT, Rolle.ADMIN);
    return anmelden(ADMIN, PASSWORT);
  }

  @Test
  @DisplayName("Als ADMIN laesst sich ein Konto anlegen, das sich anschliessend anmelden kann")
  void admin_legt_konto_an() throws Exception {
    Sitzung admin = alsAdmin();

    Antwort angelegt = admin.sende("/api/admin/users", anlegen(NEUES_KONTO, "MANDANT", MANDANT_A));

    assertThat(angelegt.status()).isEqualTo(201);
    assertThat(angelegt.<String>json("$.username")).isEqualTo(NEUES_KONTO);
    assertThat(angelegt.<String>json("$.role")).isEqualTo("MANDANT");
    assertThat(angelegt.<String>json("$.mandantId")).isEqualTo(MANDANT_A);
    assertThat(angelegt.rumpf())
        .as("Die Antwort enthaelt niemals das Passwort")
        .doesNotContain(EINMALPASSWORT);

    // Das neue Konto meldet sich an — und steht sofort unter Aenderungszwang.
    Antwort ich = anmelden(NEUES_KONTO, EINMALPASSWORT).hole("/api/auth/me");
    assertThat(ich.status()).isEqualTo(200);
    assertThat(ich.<Boolean>json("$.mustChangePassword")).isTrue();
    assertThat(ich.<String>json("$.mandant.id")).isEqualTo(MANDANT_A);
  }

  @Test
  @DisplayName("Derselbe Benutzername ein zweites Mal wird abgelehnt und ueberschreibt nichts")
  void doppelter_benutzername_abgelehnt() throws Exception {
    Sitzung admin = alsAdmin();

    assertThat(admin.sende("/api/admin/users", anlegen(NEUES_KONTO, "MANDANT", MANDANT_A)).status())
        .isEqualTo(201);
    Antwort zweiter = admin.sende("/api/admin/users", anlegen(NEUES_KONTO, "MANDANT", MANDANT_B));

    assertThat(zweiter.status()).isEqualTo(409);
    assertThat(zweiter.<String>json("$.title")).isEqualTo("Benutzername vergeben");

    long id = appUserRepository.findeNachBenutzername(NEUES_KONTO).orElseThrow().id();
    assertThat(mandantRepository.findeFuerNutzer(id))
        .as("Ueberschrieben wurde nichts: die Zuordnung ist noch die erste")
        .extracting(MandantResponse::id)
        .containsExactly(MANDANT_A);
  }

  @Test
  @DisplayName("Eine unbekannte Mandanten-ID liefert 404 und legt nichts an")
  void unbekannter_mandant_liefert_404() throws Exception {
    Sitzung admin = alsAdmin();

    Antwort antwort =
        admin.sende("/api/admin/users", anlegen(NEUES_KONTO, "MANDANT", "GIBTESGARANTIERTNICHT"));

    assertThat(antwort.status()).isEqualTo(404);
    assertThat(appUserRepository.findeNachBenutzername(NEUES_KONTO)).isEmpty();
  }

  @Test
  @DisplayName("Als MANDANT ist der Endpunkt verschlossen")
  void mandant_darf_nicht_anlegen() throws Exception {
    legeNutzerAn(KUNDE, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung kunde = anmelden(KUNDE, PASSWORT);

    Antwort antwort = kunde.sende("/api/admin/users", anlegen(NEUES_KONTO, "MANDANT", MANDANT_A));

    assertThat(antwort.status()).isEqualTo(403);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Zugriff verweigert");
    assertThat(appUserRepository.findeNachBenutzername(NEUES_KONTO)).isEmpty();
  }

  @Test
  @DisplayName("Eine unbekannte Rolle wird abgelehnt")
  void unbekannte_rolle_abgelehnt() throws Exception {
    Sitzung admin = alsAdmin();

    Antwort antwort = admin.sende("/api/admin/users", anlegen(NEUES_KONTO, "SUPERUSER", MANDANT_A));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Unbekannte Rolle");
  }

  @Test
  @DisplayName("Ein fehlendes Feld liefert eine feldbezogene Fehlerliste")
  void fehlendes_feld_liefert_feldliste() throws Exception {
    Sitzung admin = alsAdmin();

    Antwort antwort =
        admin.sende(
            "/api/admin/users",
            """
            {"username":"","role":"MANDANT","mandantId":"%s","initialPassword":"%s"}"""
                .formatted(MANDANT_A, EINMALPASSWORT));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<java.util.List<String>>json("$.errors[*].feld")).contains("username");
  }
}
