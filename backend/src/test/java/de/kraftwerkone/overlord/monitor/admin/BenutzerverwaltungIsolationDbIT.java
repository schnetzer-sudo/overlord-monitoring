package de.kraftwerkone.overlord.monitor.admin;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Pflicht-Isolationstests der Benutzerverwaltung — <b>einer je Endpunkt</b> (Regel M4). Ohne
 * sie wird nicht gemergt.
 *
 * <p><b>Hier ist die Grenze eine andere als sonst, und das ist die Uebertragung der Regel und keine
 * Aufweichung.</b> Die Nutzerverwaltung ist mandantenfrei (E2): {@code app_user} liegt in {@code
 * overlord_monitor}, Regel M2 bindet nur Repositories auf {@code jooq.glassfish}. Es gibt also
 * keine Mandantengrenze, an der ein Leck entstehen koennte. Geprueft wird stattdessen die
 * <b>Rollengrenze</b>. Das ist die zweite Uebertragung derselben Regel, nach der Verschiebung von
 * der Eingabe auf die Ausgabe bei {@code ProzesseIsolationDbIT}.
 *
 * <p><b>Der Kern ist das eigene Konto.</b> Jeder Endpunkt gibt einem MANDANT-Nutzer {@code 403} —
 * auch dann, wenn er auf dessen <i>eigenes</i> Konto zeigt. Genau dieser Fall ist der einzige, bei
 * dem eine Berechtigungspruefung ueber den Kontoinhaber plausibel aussieht und trotzdem falsch ist:
 * Wer sein eigenes Konto entsperren, sich selbst zum ADMIN machen oder sich Mandanten zuordnen
 * darf, braucht die Benutzerverwaltung nicht mehr zu ueberwinden — er <i>ist</i> sie.
 *
 * <p><b>Und die Gegenprobe, die den Beweis traegt:</b> Dieselben Aufrufe mit einer
 * <b>erfundenen</b> Konto-ID liefern <i>dieselbe</i> Antwort. Waere es anders — {@code 403} fuer
 * ein existierendes, {@code 404} fuer ein erfundenes Konto —, liesse sich ueber die Endpunkte
 * durchzaehlen, welche Konto-IDs es gibt. Die Rollengrenze greift <b>vor</b> jedem
 * Datenbankzugriff, weil sie in {@code SecurityConfig} steht und nicht im Service.
 */
class BenutzerverwaltungIsolationDbIT extends SicherheitsTestbasis {

  private static final String KUNDE = PRAEFIX + "verwaltung-kunde";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Eine Konto-ID, die es garantiert nicht gibt. */
  private static final long ERFUNDENE_ID = 999_888_777L;

  private long eigeneId;
  private Sitzung alsKunde;

  @BeforeEach
  void kundeAnlegenUndAnmelden() throws IOException, InterruptedException {
    eigeneId = legeNutzerAn(KUNDE, PASSWORT, Rolle.MANDANT, MANDANT_A);
    alsKunde = anmelden(KUNDE, PASSWORT);
  }

  /** Der Rumpf ohne die beiden Angaben, die je Anfrage verschieden sind und sein muessen. */
  private static String vergleichbar(Antwort antwort) {
    return antwort
        .rumpfOhneTraceId()
        .replaceAll("\"instance\"\\s*:\\s*\"[^\"]*\"", "\"instance\":\"-\"");
  }

  private void verschlossen(Antwort antwort) {
    assertThat(antwort.status()).isEqualTo(403);
    assertThat(antwort.<String>json("$.title")).isEqualTo("Zugriff verweigert");
  }

  @Test
  @DisplayName("GET /api/admin/users ist fuer MANDANT verschlossen")
  void liste_ist_verschlossen() throws Exception {
    verschlossen(alsKunde.hole("/api/admin/users"));
  }

  @Test
  @DisplayName("PUT .../lock ist verschlossen — auch auf das eigene Konto")
  void sperre_ist_verschlossen() throws Exception {
    Antwort eigenes =
        alsKunde.aendere("/api/admin/users/" + eigeneId + "/lock", "{\"locked\":false}");
    Antwort erfunden =
        alsKunde.aendere("/api/admin/users/" + ERFUNDENE_ID + "/lock", "{\"locked\":false}");

    verschlossen(eigenes);
    assertThat(vergleichbar(eigenes))
        .as("Das eigene Konto und ein erfundenes muessen ununterscheidbar antworten")
        .isEqualTo(vergleichbar(erfunden));
    assertThat(appUserRepository.findeKonto(eigeneId).orElseThrow().adminGesperrt()).isFalse();
  }

  @Test
  @DisplayName("PUT .../active ist verschlossen — auch auf das eigene Konto")
  void aktivzustand_ist_verschlossen() throws Exception {
    Antwort eigenes =
        alsKunde.aendere("/api/admin/users/" + eigeneId + "/active", "{\"active\":false}");
    Antwort erfunden =
        alsKunde.aendere("/api/admin/users/" + ERFUNDENE_ID + "/active", "{\"active\":false}");

    verschlossen(eigenes);
    assertThat(vergleichbar(eigenes)).isEqualTo(vergleichbar(erfunden));
    assertThat(appUserRepository.findeKonto(eigeneId).orElseThrow().aktiv())
        .as("Ein abgelehnter Aufruf aendert nichts")
        .isTrue();
  }

  @Test
  @DisplayName("PUT .../role ist verschlossen — niemand macht sich selbst zum ADMIN")
  void rolle_ist_verschlossen() throws Exception {
    Antwort eigenes =
        alsKunde.aendere("/api/admin/users/" + eigeneId + "/role", "{\"role\":\"ADMIN\"}");
    Antwort erfunden =
        alsKunde.aendere("/api/admin/users/" + ERFUNDENE_ID + "/role", "{\"role\":\"ADMIN\"}");

    verschlossen(eigenes);
    assertThat(vergleichbar(eigenes)).isEqualTo(vergleichbar(erfunden));
    assertThat(appUserRepository.findeKonto(eigeneId).orElseThrow().rolle())
        .isEqualTo(Rolle.MANDANT);
  }

  @Test
  @DisplayName("PUT .../tenants ist verschlossen — niemand ordnet sich selbst einen Mandanten zu")
  void mandanten_sind_verschlossen() throws Exception {
    Antwort eigenes =
        alsKunde.aendere(
            "/api/admin/users/" + eigeneId + "/tenants",
            "{\"tenants\":[\"%s\",\"%s\"]}".formatted(MANDANT_A, MANDANT_B));
    Antwort erfunden =
        alsKunde.aendere(
            "/api/admin/users/" + ERFUNDENE_ID + "/tenants",
            "{\"tenants\":[\"%s\",\"%s\"]}".formatted(MANDANT_A, MANDANT_B));

    verschlossen(eigenes);
    assertThat(vergleichbar(eigenes)).isEqualTo(vergleichbar(erfunden));
    assertThat(appUserRepository.mandantenVon(eigeneId))
        .as("Der fremde Mandant darf nicht dazugekommen sein")
        .containsExactly(MANDANT_A);
  }

  @Test
  @DisplayName("POST .../password ist verschlossen — auch fuer das eigene Konto")
  void passwort_zuruecksetzen_ist_verschlossen() throws Exception {
    Antwort eigenes =
        alsKunde.sende(
            "/api/admin/users/" + eigeneId + "/password",
            "{\"initialPassword\":\"nochEinLangesPasswort\"}");
    Antwort erfunden =
        alsKunde.sende(
            "/api/admin/users/" + ERFUNDENE_ID + "/password",
            "{\"initialPassword\":\"nochEinLangesPasswort\"}");

    verschlossen(eigenes);
    assertThat(vergleichbar(eigenes)).isEqualTo(vergleichbar(erfunden));
    assertThat(anmelden(KUNDE, PASSWORT).hole("/api/auth/me").status())
        .as("Das bisherige Passwort gilt unveraendert weiter")
        .isEqualTo(200);
  }
}
