package de.kraftwerkone.overlord.monitor.admin;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die fachlichen Regeln der Benutzerverwaltung an den Endpunkten: Liste, Sperre, Aktivzustand,
 * Rolle, Mandanten, Passwort-Reset.
 *
 * <p>Der Sitzungsentzug (E5, E6) hat einen eigenen Test, {@code SitzungsentzugDbIT}, weil er nicht
 * eine Regel <i>eines</i> Endpunkts ist, sondern dieselbe Regel <i>aller fuenf</i>.
 *
 * <p><b>Der handelnde Admin ist ein Testkonto und nicht das echte.</b> Jeder Test legt sich sein
 * eigenes {@code it-}-Konto an; die Konten des Betreibers werden nicht angefasst, gelesen oder
 * veraendert.
 */
class BenutzerverwaltungDbIT extends SicherheitsTestbasis {

  private static final String ADMIN = PRAEFIX + "verw-admin";
  private static final String ZWEITER_ADMIN = PRAEFIX + "verw-admin2";
  private static final String KUNDE = PRAEFIX + "verw-kunde";
  private static final String PASSWORT = "einLangesPasswort1";
  private static final String NEUES_PASSWORT = "einAnderesLangesPasswort";

  private long adminId;
  private long zweiterAdminId;
  private long kundeId;
  private Sitzung alsAdmin;

  @BeforeEach
  void kontenAnlegen() throws IOException, InterruptedException {
    adminId = legeNutzerAn(ADMIN, PASSWORT, Rolle.ADMIN, MANDANT_A);
    // Ein zweiter nutzbarer Administrator, damit die Tests nicht davon abhaengen, welche echten
    // Konten die Testkopie gerade traegt.
    zweiterAdminId = legeNutzerAn(ZWEITER_ADMIN, PASSWORT, Rolle.ADMIN, MANDANT_A);
    kundeId = legeNutzerAn(KUNDE, PASSWORT, Rolle.MANDANT, MANDANT_A);
    alsAdmin = anmelden(ADMIN, PASSWORT);
  }

  /** Die Zeile eines Kontos aus der Liste, nach Benutzername gesucht. */
  private Map<String, Object> zeile(Antwort liste, String username) {
    List<Map<String, Object>> zeilen = liste.json("$[?(@.username == '" + username + "')]");
    assertThat(zeilen).as("Konto %s fehlt in der Liste", username).hasSize(1);
    return zeilen.getFirst();
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Die Liste
  // ───────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Die Liste zeigt alle Konten mandantenfrei, mit Mandanten und letzter Anmeldung")
  void liste_zeigt_alle_konten() throws Exception {
    // Der Kunde meldet sich an — damit hat er eine letzte Anmeldung, der zweite Admin nicht.
    anmelden(KUNDE, PASSWORT);

    Antwort liste = alsAdmin.hole("/api/admin/users");

    assertThat(liste.status()).isEqualTo(200);
    Map<String, Object> kunde = zeile(liste, KUNDE);
    assertThat(kunde.get("role")).isEqualTo("MANDANT");
    assertThat(kunde.get("tenants")).isEqualTo(List.of(MANDANT_A));
    assertThat(kunde.get("locked")).isEqualTo(false);
    assertThat(kunde.get("active")).isEqualTo(true);
    assertThat(kunde.get("mustChangePassword")).isEqualTo(false);
    assertThat((String) kunde.get("lastLogin"))
        .as("Die letzte Anmeldung kommt aus dem audit_log (E17)")
        .isNotNull()
        .endsWith("Z");

    assertThat(zeile(liste, ZWEITER_ADMIN).get("lastLogin"))
        .as("Wer sich nie angemeldet hat, hat keine letzte Anmeldung — das ist die Supportfrage")
        .isNull();

    assertThat(liste.rumpf())
        .as("Die Liste traegt niemals einen Passwort-Hash")
        .doesNotContain("$2a$", "passwordHash", "passwortHash");
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Sperren und Entsperren
  // ───────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Sperren verhindert die Anmeldung, Entsperren gibt sie zurueck")
  void sperren_und_entsperren() throws Exception {
    Antwort gesperrt =
        alsAdmin.aendere("/api/admin/users/" + kundeId + "/lock", "{\"locked\":true}");

    assertThat(gesperrt.status()).isEqualTo(200);
    assertThat(gesperrt.<Boolean>json("$.locked")).isTrue();

    Sitzung versuch = neueSitzung();
    Antwort abgewiesen = versuch.sende("/api/auth/login", anmeldung(KUNDE, PASSWORT));
    assertThat(abgewiesen.status()).isEqualTo(401);
    assertThat(abgewiesen.<String>json("$.title")).isEqualTo("Konto gesperrt");
    assertThat(abgewiesen.<String>json("$.detail"))
        .as("Eine Admin-Sperre laeuft nicht ab — der Text darf nicht auf Fehlversuche zeigen")
        .doesNotContain("Fehlversuch")
        .doesNotContain("spaeter");

    Antwort entsperrt =
        alsAdmin.aendere("/api/admin/users/" + kundeId + "/lock", "{\"locked\":false}");
    assertThat(entsperrt.status()).isEqualTo(200);
    assertThat(entsperrt.<Boolean>json("$.locked")).isFalse();
    assertThat(anmelden(KUNDE, PASSWORT).hole("/api/auth/me").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("Entsperren raeumt auch eine laufende Zeitsperre nach Fehlversuchen ab")
  void entsperren_raeumt_die_zeitsperre_ab() throws Exception {
    // Fuenf Fehlversuche setzen locked_until — das ist der haeufigere Supportfall.
    for (int versuch = 0; versuch < 5; versuch++) {
      neueSitzung().sende("/api/auth/login", anmeldung(KUNDE, "falschesPasswort1"));
    }
    setzeIpBegrenzungZurueck();
    assertThat(neueSitzung().sende("/api/auth/login", anmeldung(KUNDE, PASSWORT)).status())
        .as("Nach fuenf Fehlversuchen laeuft die Zeitsperre")
        .isEqualTo(401);

    assertThat(
            alsAdmin
                .aendere("/api/admin/users/" + kundeId + "/lock", "{\"locked\":false}")
                .status())
        .isEqualTo(200);

    setzeIpBegrenzungZurueck();
    assertThat(anmelden(KUNDE, PASSWORT).hole("/api/auth/me").status())
        .as(
            "Wer anruft, ist meist zeitgesperrt und nicht administrativ — Entsperren muss das loesen")
        .isEqualTo(200);
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Aktivzustand, Rolle, Mandanten
  // ───────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Deaktivieren und Reaktivieren — geloescht wird nie")
  void deaktivieren_und_reaktivieren() throws Exception {
    assertThat(
            alsAdmin
                .aendere("/api/admin/users/" + kundeId + "/active", "{\"active\":false}")
                .<Boolean>json("$.active"))
        .isFalse();

    Antwort abgewiesen = neueSitzung().sende("/api/auth/login", anmeldung(KUNDE, PASSWORT));
    assertThat(abgewiesen.status()).isEqualTo(401);
    assertThat(abgewiesen.<String>json("$.title")).isEqualTo("Konto deaktiviert");

    assertThat(appUserRepository.findeKonto(kundeId))
        .as("Das Konto ist da — sonst waeren seine Protokollzeilen unlesbar (E8)")
        .isPresent();

    assertThat(
            alsAdmin
                .aendere("/api/admin/users/" + kundeId + "/active", "{\"active\":true}")
                .<Boolean>json("$.active"))
        .isTrue();
    assertThat(anmelden(KUNDE, PASSWORT).hole("/api/auth/me").status()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "Herabstufung ohne Mandantenzuordnung wird abgelehnt und sagt, was zuerst zu tun ist")
  void herabstufung_ohne_zuordnung_abgelehnt() throws Exception {
    // Wie das Bootstrap-Konto: ADMIN ohne jede Zeile in app_user_mandant.
    long ohneMandant = legeNutzerAn(PRAEFIX + "verw-ohnemandant", PASSWORT, Rolle.ADMIN);
    assertThat(appUserRepository.mandantenVon(ohneMandant)).isEmpty();

    Antwort antwort =
        alsAdmin.aendere("/api/admin/users/" + ohneMandant + "/role", "{\"role\":\"MANDANT\"}");

    assertThat(antwort.status()).isEqualTo(409);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Erst Mandanten zuordnen");
    assertThat(antwort.<String>json("$.detail")).contains("zuerst");
    assertThat(appUserRepository.findeKonto(ohneMandant).orElseThrow().rolle())
        .isEqualTo(Rolle.ADMIN);
  }

  @Test
  @DisplayName("Herabstufung mit Zuordnung geht, und die Rolle wirkt")
  void herabstufung_mit_zuordnung() throws Exception {
    Antwort antwort =
        alsAdmin.aendere("/api/admin/users/" + zweiterAdminId + "/role", "{\"role\":\"MANDANT\"}");

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<String>json("$.role")).isEqualTo("MANDANT");
    assertThat(anmelden(ZWEITER_ADMIN, PASSWORT).hole("/api/admin/users").status())
        .as("Nach der Herabstufung ist die Verwaltung verschlossen")
        .isEqualTo(403);
  }

  @Test
  @DisplayName("Die Mandantenmenge laesst sich pflegen — die dritte M1-Ausnahme")
  void mandantenmenge_pflegen() throws Exception {
    Antwort antwort =
        alsAdmin.aendere(
            "/api/admin/users/" + kundeId + "/tenants",
            "{\"tenants\":[\"%s\",\"%s\"]}".formatted(MANDANT_A, MANDANT_B));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.tenants"))
        .as("aufsteigend nach MandantID, unabhaengig von der Reihenfolge der Eingabe")
        .containsExactly(MANDANT_B, MANDANT_A);
    assertThat(mandantRepository.findeFuerNutzer(kundeId)).hasSize(2);
  }

  @Test
  @DisplayName("Eine unbekannte Mandanten-ID liefert 404 und aendert nichts")
  void unbekannter_mandant_liefert_404() throws Exception {
    Antwort antwort =
        alsAdmin.aendere(
            "/api/admin/users/" + kundeId + "/tenants",
            "{\"tenants\":[\"%s\",\"GIBTESGARANTIERTNICHT\"]}".formatted(MANDANT_B));

    assertThat(antwort.status()).isEqualTo(404);
    assertThat(appUserRepository.mandantenVon(kundeId))
        .as("Die bestehende Menge bleibt unangetastet — nicht halb ersetzt")
        .containsExactly(MANDANT_A);
  }

  @Test
  @DisplayName("Die letzte Mandantenzuordnung laesst sich nicht entfernen — fuer beide Rollen")
  void letzte_zuordnung_bleibt_fuer_beide_rollen() throws Exception {
    for (long id : new long[] {kundeId, zweiterAdminId}) {
      Antwort antwort = alsAdmin.aendere("/api/admin/users/" + id + "/tenants", "{\"tenants\":[]}");

      assertThat(antwort.status()).as("Konto %s", id).isEqualTo(409);
      assertThat(antwort.<String>json("$.title")).isEqualTo("Mindestens ein Mandant");
      assertThat(appUserRepository.mandantenVon(id))
          .as("Auch beim ADMIN bleibt sie stehen — sonst faellt sie bei der Herabstufung ins Leere")
          .containsExactly(MANDANT_A);
    }
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Selbstschutz (E12, erste Stufe)
  // ───────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Ein Admin kann sich selbst nicht sperren, deaktivieren oder herabstufen")
  void selbstschutz_greift_fuer_alle_drei_vorgaenge() throws Exception {
    Antwort gesperrt =
        alsAdmin.aendere("/api/admin/users/" + adminId + "/lock", "{\"locked\":true}");
    Antwort deaktiviert =
        alsAdmin.aendere("/api/admin/users/" + adminId + "/active", "{\"active\":false}");
    Antwort herabgestuft =
        alsAdmin.aendere("/api/admin/users/" + adminId + "/role", "{\"role\":\"MANDANT\"}");

    for (Antwort antwort : List.of(gesperrt, deaktiviert, herabgestuft)) {
      assertThat(antwort.status()).isEqualTo(409);
      assertThat(antwort.<String>json("$.title")).isEqualTo("Nicht am eigenen Konto");
    }
    var eigenes = appUserRepository.findeKonto(adminId).orElseThrow();
    assertThat(eigenes.adminGesperrt()).isFalse();
    assertThat(eigenes.aktiv()).isTrue();
    assertThat(eigenes.rolle()).isEqualTo(Rolle.ADMIN);
    assertThat(alsAdmin.hole("/api/admin/users").status())
        .as("Die Sitzung des Admins ist unversehrt — es hat kein Vorgang stattgefunden")
        .isEqualTo(200);
  }

  /**
   * Die andere Richtung ist erlaubt — und sie kostet den Handelnden seine eigene Sitzung.
   *
   * <p><b>Das ist kein Defekt, sondern der Preis von E5</b>, und er gehoert festgehalten: „Alle
   * Sitzungen eines Kontos werden verworfen" ist <i>eine Regel, keine Fallunterscheidung</i>. Wer
   * einen der fuenf Vorgaenge auf sein eigenes Konto anwendet, faellt darunter wie jeder andere.
   * Eine Ausnahme fuer den Handelnden waere genau die Fallunterscheidung, die E5 vermeidet — und
   * sie waere die eine, an die sich beim sechsten Vorgang niemand erinnert.
   *
   * <p>Dasselbe gilt fuer die eigene Mandantenaenderung und den Reset des eigenen Passworts. Fuer
   * das eigene Passwort gibt es deshalb {@code POST /api/auth/password}: Der behaelt die aktuelle
   * Sitzung (E6).
   */
  @Test
  @DisplayName(
      "Das eigene Konto zu entsperren ist erlaubt — kostet nach E5 aber die eigene Sitzung")
  void selbstschutz_greift_nur_in_der_entwertenden_richtung() throws Exception {
    assertThat(
            alsAdmin
                .aendere("/api/admin/users/" + adminId + "/lock", "{\"locked\":false}")
                .status())
        .as("Sich selbst zu entsperren ist wirkungslos, aber kein Verstoss")
        .isEqualTo(200);

    assertThat(alsAdmin.hole("/api/auth/me").status())
        .as("E5 kennt keine Ausnahme fuer den Handelnden — auch seine Sitzung faellt")
        .isEqualTo(401);

    Sitzung erneut = anmelden(ADMIN, PASSWORT);
    assertThat(
            erneut.aendere("/api/admin/users/" + adminId + "/active", "{\"active\":true}").status())
        .as("Nach erneuter Anmeldung geht es unveraendert weiter")
        .isEqualTo(200);
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Passwort zuruecksetzen (E13)
  // ───────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Der Admin tippt ein Einmalpasswort; das Konto startet mit Aenderungszwang")
  void passwort_zuruecksetzen_setzt_aenderungszwang() throws Exception {
    Antwort antwort =
        alsAdmin.sende(
            "/api/admin/users/" + kundeId + "/password",
            "{\"initialPassword\":\"%s\"}".formatted(NEUES_PASSWORT));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<Boolean>json("$.mustChangePassword")).isTrue();
    assertThat(antwort.rumpf())
        .as("Das Passwort steht in keiner Antwort")
        .doesNotContain(NEUES_PASSWORT);

    Antwort ich = anmelden(KUNDE, NEUES_PASSWORT).hole("/api/auth/me");
    assertThat(ich.status()).isEqualTo(200);
    assertThat(ich.<Boolean>json("$.mustChangePassword")).isTrue();
  }

  @Test
  @DisplayName("Ein Reset auf das aktuelle Passwort wird abgelehnt")
  void reset_auf_das_aktuelle_passwort_abgelehnt() throws Exception {
    Antwort antwort =
        alsAdmin.sende(
            "/api/admin/users/" + kundeId + "/password",
            "{\"initialPassword\":\"%s\"}".formatted(PASSWORT));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Passwort unveraendert");
    assertThat(anmelden(KUNDE, PASSWORT).hole("/api/auth/me").<Boolean>json("$.mustChangePassword"))
        .as("Ein abgelehnter Reset setzt auch keinen Aenderungszwang")
        .isFalse();
  }

  @Test
  @DisplayName("Ein zu kurzes Einmalpasswort wird abgelehnt — dieselbe Grenze wie beim Anlegen")
  void zu_kurzes_passwort_abgelehnt() throws Exception {
    Antwort antwort =
        alsAdmin.sende(
            "/api/admin/users/" + kundeId + "/password", "{\"initialPassword\":\"kurz\"}");

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Passwort zu kurz");
  }

  /**
   * <b>Der leere Rumpf darf nicht still „false" bedeuten.</b> {@code locked} und {@code active}
   * sind primitive {@code boolean}; unter Jackson 2 waere ein fehlendes Feld lautlos zu {@code
   * false} geworden — ein {@code PUT} mit {@code &#123;&#125;} haette dann entsperrt
   * beziehungsweise deaktiviert, ohne dass es jemand so gemeint hat. Jackson 3 lehnt das ab, und
   * dieser Test haelt fest, dass das so bleibt: Das Projekt laeuft bewusst nicht auf Mustern der
   * Vorgaengergeneration.
   */
  @Test
  @DisplayName("Ein leerer Rumpf bedeutet nicht still false, sondern 400")
  void leerer_rumpf_wird_abgelehnt() throws Exception {
    Antwort sperre = alsAdmin.aendere("/api/admin/users/" + kundeId + "/lock", "{}");
    Antwort aktiv = alsAdmin.aendere("/api/admin/users/" + kundeId + "/active", "{}");

    assertThat(sperre.status()).isEqualTo(400);
    assertThat(aktiv.status()).isEqualTo(400);
    var unveraendert = appUserRepository.findeKonto(kundeId).orElseThrow();
    assertThat(unveraendert.adminGesperrt()).isFalse();
    assertThat(unveraendert.aktiv()).isTrue();
  }

  @Test
  @DisplayName("Ein unbekanntes Konto liefert 404")
  void unbekanntes_konto_liefert_404() throws Exception {
    assertThat(alsAdmin.aendere("/api/admin/users/999888777/lock", "{\"locked\":true}").status())
        .isEqualTo(404);
  }
}
