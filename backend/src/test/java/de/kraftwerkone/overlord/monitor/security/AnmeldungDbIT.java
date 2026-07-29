package de.kraftwerkone.overlord.monitor.security;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Anmeldung, Abmeldung, Sperre und Passwortwechsel gegen die Testkopie. */
class AnmeldungDbIT extends SicherheitsTestbasis {

  private static final String NUTZER = PRAEFIX + "anmeldung";
  private static final String PASSWORT = "einLangesPasswort1";

  @Test
  @DisplayName("Anmeldung liefert eine Sitzung, Abmeldung beendet sie")
  void anmeldung_und_abmeldung() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);

    Sitzung sitzung = anmelden(NUTZER, PASSWORT);
    Antwort ich = sitzung.hole("/api/auth/me");
    assertThat(ich.status()).isEqualTo(200);
    assertThat(ich.<String>json("$.username")).isEqualTo(NUTZER);
    assertThat(ich.<String>json("$.role")).isEqualTo("MANDANT");
    // Genau ein zulaessiger Mandant: der wird beim Anmelden gesetzt.
    assertThat(ich.<String>json("$.mandant.id")).isEqualTo(MANDANT_A);
    assertThat(ich.<Boolean>json("$.mustChangePassword")).isFalse();

    assertThat(sitzung.sende("/api/auth/logout", "{}").status()).isEqualTo(204);

    Antwort danach = sitzung.hole("/api/auth/me");
    assertThat(danach.status()).isEqualTo(401);
    assertThat(danach.<String>json("$.title")).isEqualTo("Nicht angemeldet");
    assertThat(danach.<String>json("$.traceId")).isNotBlank();
  }

  @Test
  @DisplayName("Das Sitzungs-Cookie heisst OVERLORD_SESSION, ist HttpOnly und SameSite=Lax")
  void cookie_traegt_die_vereinbarten_attribute() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);

    Sitzung sitzung = neueSitzung();
    Antwort anmeldung = sitzung.sende("/api/auth/login", anmeldung(NUTZER, PASSWORT));

    String sitzungsCookie =
        anmeldung.setCookieHeader().stream()
            .filter(kopf -> kopf.startsWith(SITZUNGSCOOKIE + "="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Kein " + SITZUNGSCOOKIE + " in der Antwort"));

    assertThat(sitzungsCookie).contains("HttpOnly").contains("SameSite=Lax");
    // Im Profil dev bewusst OHNE Secure — sonst schickt der Browser es ueber
    // http://localhost nicht mit. Dass es ausserhalb von dev gesetzt ist, prueft
    // CookieAttributeTest ohne Datenbank.
    assertThat(sitzungsCookie).doesNotContain("Secure");
  }

  @Test
  @DisplayName("Ein unangemeldeter Aufruf legt gar keine Sitzung an")
  void unangemeldeter_aufruf_legt_keine_sitzung_an() throws Exception {
    Sitzung sitzung = neueSitzung();

    assertThat(sitzung.hole("/api/auth/me").status()).isEqualTo(401);

    // Die staerkste Form des Schutzes gegen Sitzungsfestschreibung: Es gibt vor der Anmeldung
    // nichts, was sich festschreiben liesse. Das CSRF-Token liegt im Cookie, nicht in der Sitzung.
    assertThat(sitzung.sitzungsId()).isNull();
  }

  @Test
  @DisplayName("Eine untergeschobene Sitzungs-ID wird beim Anmelden nicht uebernommen")
  void untergeschobene_sitzungs_id_wird_nicht_uebernommen() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);

    Sitzung sitzung = neueSitzung();
    String vomAngreifer = "0000angreifer-0000-0000-000000000000";
    sitzung.setzeCookie(SITZUNGSCOOKIE, vomAngreifer);

    Antwort angemeldet = sitzung.sende("/api/auth/login", anmeldung(NUTZER, PASSWORT));

    // Gelesen aus dem Set-Cookie der Antwort, nicht aus dem Cookie-Speicher: Der Speicher haelt
    // die untergeschobene Zeile noch, bis der Client sie ersetzt — der Beweis ist, was der
    // Server ausstellt.
    String ausgestellt =
        angemeldet.setCookieHeader().stream()
            .filter(kopf -> kopf.startsWith(SITZUNGSCOOKIE + "="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Die Anmeldung stellt kein Sitzungs-Cookie aus"));
    assertThat(ausgestellt)
        .as("Die angemeldete Sitzung darf niemals unter der vorgegebenen ID laufen")
        .doesNotContain(vomAngreifer);

    // Und mit der untergeschobenen ID kommt niemand hinein.
    Sitzung angreifer = neueSitzung();
    angreifer.setzeCookie(SITZUNGSCOOKIE, vomAngreifer);
    assertThat(angreifer.hole("/api/auth/me").status()).isEqualTo(401);
  }

  @Test
  @DisplayName("Eine erneute Anmeldung auf laufender Sitzung tauscht die Sitzungs-ID")
  void erneute_anmeldung_tauscht_die_sitzungs_id() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);

    Sitzung sitzung = anmelden(NUTZER, PASSWORT);
    String vorher = sitzung.sitzungsId();
    assertThat(vorher).isNotNull();

    // Jetzt existiert eine Sitzung — erst hier laeuft die Erneuerung ueberhaupt an.
    sitzung.sende("/api/auth/login", anmeldung(NUTZER, PASSWORT));

    assertThat(sitzung.sitzungsId())
        .as("Jede Rechteaenderung bekommt eine neue Sitzungs-ID")
        .isNotEqualTo(vorher);
  }

  @Test
  @DisplayName("Der fuenfte Fehlversuch sperrt, der sechste laeuft gegen die Sperre")
  void sechster_fehlversuch_laeuft_gegen_die_sperre() throws Exception {
    long id = legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung sitzung = neueSitzung();

    for (int versuch = 1; versuch <= 5; versuch++) {
      Antwort antwort = sitzung.sende("/api/auth/login", anmeldung(NUTZER, "falsch"));
      assertThat(antwort.status()).isEqualTo(401);
      // Bis hierher bleibt die Meldung unspezifisch.
      assertThat(antwort.<String>json("$.title")).isEqualTo("Anmeldung fehlgeschlagen");
    }

    assertThat(appUserRepository.findeNachId(id).orElseThrow().istGesperrt(jetztUtc()))
        .as("Nach dem fuenften Fehlversuch muss locked_until stehen")
        .isTrue();

    // Der sechste Versuch — jetzt mit dem RICHTIGEN Passwort. Erst hier darf der Grund fallen.
    Antwort sechster = sitzung.sende("/api/auth/login", anmeldung(NUTZER, PASSWORT));
    assertThat(sechster.status()).isEqualTo(401);
    assertThat(sechster.<String>json("$.title")).isEqualTo("Konto gesperrt");
  }

  @Test
  @DisplayName("Ein falsches Passwort auf ein gesperrtes Konto bleibt unspezifisch")
  void falsches_passwort_auf_gesperrtem_konto_bleibt_unspezifisch() throws Exception {
    long id = legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    sperre(id, jetztUtc().plusMinutes(15));

    Antwort antwort = neueSitzung().sende("/api/auth/login", anmeldung(NUTZER, "falsch"));

    assertThat(antwort.status()).isEqualTo(401);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Anmeldung fehlgeschlagen");
  }

  @Test
  @DisplayName("Eine abgelaufene Sperre laesst die Anmeldung wieder zu")
  void abgelaufene_sperre_laesst_anmeldung_zu() throws Exception {
    long id = legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    sperre(id, jetztUtc().minusMinutes(1));

    Sitzung sitzung = anmelden(NUTZER, PASSWORT);

    assertThat(sitzung.hole("/api/auth/me").status()).isEqualTo(200);
    assertThat(appUserRepository.findeNachId(id).orElseThrow().fehlversuche()).isZero();
  }

  @Test
  @DisplayName("Ein unbekannter Benutzername liefert dieselbe Antwort wie ein falsches Passwort")
  void unbekannter_name_und_falsches_passwort_sind_ununterscheidbar() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung sitzung = neueSitzung();

    Antwort unbekannt =
        sitzung.sende("/api/auth/login", anmeldung(PRAEFIX + "gibtesnicht", "irgendwas1234"));
    Antwort falschesPasswort = sitzung.sende("/api/auth/login", anmeldung(NUTZER, "falsch"));

    assertThat(unbekannt.status()).isEqualTo(401);
    assertThat(falschesPasswort.status()).isEqualTo(401);
    assertThat(unbekannt.rumpfOhneTraceId())
        .as("Unterschieden sich die Antworten, liessen sich Benutzernamen durchprobieren")
        .isEqualTo(falschesPasswort.rumpfOhneTraceId());
  }

  @Test
  @DisplayName("Solange der Aenderungszwang steht, lehnt jeder andere Endpunkt ab")
  void aenderungszwang_sperrt_die_uebrigen_endpunkte() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, true, MANDANT_A);

    Sitzung sitzung = anmelden(NUTZER, PASSWORT);
    assertThat(sitzung.hole("/api/auth/me").<Boolean>json("$.mustChangePassword")).isTrue();

    Antwort gesperrt = sitzung.hole("/api/mandanten");
    assertThat(gesperrt.status()).isEqualTo(403);
    assertThat(gesperrt.<String>json("$.title")).isEqualTo("Passwortwechsel erforderlich");

    Antwort wechsel =
        sitzung.sende(
            "/api/auth/password",
            """
            {"oldPassword":"%s","newPassword":"nochEinLangesPasswort2"}"""
                .formatted(PASSWORT));
    assertThat(wechsel.status()).isEqualTo(200);
    assertThat(wechsel.<Boolean>json("$.mustChangePassword")).isFalse();

    assertThat(sitzung.hole("/api/mandanten").status()).isEqualTo(200);

    // Und das neue Passwort gilt wirklich: abmelden und neu anmelden.
    assertThat(sitzung.sende("/api/auth/logout", "{}").status()).isEqualTo(204);
    anmelden(NUTZER, "nochEinLangesPasswort2");
  }

  @Test
  @DisplayName("Die Sitzungs-ID wird auch beim Passwortwechsel erneuert")
  void sitzungs_id_wird_beim_passwortwechsel_erneuert() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung sitzung = anmelden(NUTZER, PASSWORT);
    String vorher = sitzung.sitzungsId();

    sitzung.sende(
        "/api/auth/password",
        """
        {"oldPassword":"%s","newPassword":"nochEinLangesPasswort2"}"""
            .formatted(PASSWORT));

    assertThat(sitzung.sitzungsId()).isNotEqualTo(vorher);
  }

  @Test
  @DisplayName("Ein zu kurzes neues Passwort wird abgelehnt")
  void zu_kurzes_passwort_abgelehnt() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung sitzung = anmelden(NUTZER, PASSWORT);

    Antwort antwort =
        sitzung.sende(
            "/api/auth/password",
            """
            {"oldPassword":"%s","newPassword":"kurz"}"""
                .formatted(PASSWORT));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Passwort zu kurz");
  }

  @Test
  @DisplayName("Ein falsches altes Passwort aendert nichts")
  void falsches_altes_passwort_aendert_nichts() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung sitzung = anmelden(NUTZER, PASSWORT);

    Antwort antwort =
        sitzung.sende(
            "/api/auth/password",
            """
            {"oldPassword":"garnichtdas","newPassword":"nochEinLangesPasswort2"}""");

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Altes Passwort falsch");
    // Das alte Passwort gilt weiter.
    anmelden(NUTZER, PASSWORT);
  }

  @Test
  @DisplayName("Ohne CSRF-Token wird kein schreibender Aufruf angenommen")
  void ohne_csrf_kein_schreibender_aufruf() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);

    Antwort antwort = neueSitzung().sendeOhneCsrf("/api/auth/login", anmeldung(NUTZER, PASSWORT));

    assertThat(antwort.status()).isEqualTo(403);
  }

  private void sperre(long id, LocalDateTime bisUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.LOCKED_UNTIL, bisUtc)
        .set(APP_USER.FAILED_ATTEMPTS, 5)
        .where(APP_USER.ID.eq(id))
        .execute();
  }
}
