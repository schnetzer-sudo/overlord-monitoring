package de.kraftwerkone.overlord.monitor.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Der Sitzungsentzug am laufenden System (E5, E6).
 *
 * <p><b>Das ist der Test, an dem die Zusage haengt, mit der dieses Projekt JWT abgelehnt hat.</b>
 * Ohne ihn wirkte eine Sperre erst beim naechsten Anmelden — also genau dann nicht, wenn sie
 * gebraucht wird. Und der Fehler waere lautlos: {@code findByPrincipalName} liefert bei einem
 * unbekannten Index eine <i>leere</i> Menge statt einer Ausnahme, ein wirkungsloser Entzug sieht
 * also aus wie einer, der nichts zu tun hatte.
 *
 * <p><b>Zwei Sitzungen je Konto, nicht eine.</b> Der Fehler, den ein Test mit nur einer Sitzung
 * nicht faende, ist der naheliegendste: nur die zuletzt angelegte zu verwerfen. Genau davor
 * schuetzt E5 — ein Angreifer, der sich vor der Sperre eine zweite Sitzung geholt hat, behielte
 * sie.
 *
 * <p>Der Gegenbeweis ist immer derselbe: Die Sitzung fragt {@code /api/auth/me}. Vorher {@code
 * 200}, nachher {@code 401}.
 */
class SitzungsentzugDbIT extends SicherheitsTestbasis {

  private static final String ADMIN = PRAEFIX + "entzug-admin";
  private static final String ZIEL = PRAEFIX + "entzug-ziel";
  private static final String PASSWORT = "einLangesPasswort1";
  private static final String NEUES_PASSWORT = "einAnderesLangesPasswort";

  private long zielId;
  private Sitzung alsAdmin;
  private Sitzung zielErste;
  private Sitzung zielZweite;

  @BeforeEach
  void kontenUndSitzungen() throws IOException, InterruptedException {
    legeNutzerAn(ADMIN, PASSWORT, Rolle.ADMIN, MANDANT_A);
    zielId = legeNutzerAn(ZIEL, PASSWORT, Rolle.MANDANT, MANDANT_A);
    alsAdmin = anmelden(ADMIN, PASSWORT);
    zielErste = anmelden(ZIEL, PASSWORT);
    zielZweite = anmelden(ZIEL, PASSWORT);
    lebt(zielErste, "erste Sitzung vor dem Vorgang");
    lebt(zielZweite, "zweite Sitzung vor dem Vorgang");
  }

  private void lebt(Sitzung sitzung, String was) throws IOException, InterruptedException {
    assertThat(sitzung.hole("/api/auth/me").status()).as(was).isEqualTo(200);
  }

  private void verworfen(Sitzung sitzung, String was) throws IOException, InterruptedException {
    assertThat(sitzung.hole("/api/auth/me").status()).as(was).isEqualTo(401);
  }

  private void beideSitzungenVerworfen() throws IOException, InterruptedException {
    verworfen(zielErste, "erste Sitzung nach dem Vorgang");
    verworfen(zielZweite, "zweite Sitzung nach dem Vorgang — nicht nur die zuletzt angelegte");
    lebt(alsAdmin, "die Sitzung des handelnden Admins bleibt unberuehrt");
  }

  @Test
  @DisplayName("Sperren verwirft alle Sitzungen des Kontos")
  void sperren_verwirft_alle_sitzungen() throws Exception {
    assertThat(
            alsAdmin.aendere("/api/admin/users/" + zielId + "/lock", "{\"locked\":true}").status())
        .isEqualTo(200);
    beideSitzungenVerworfen();
  }

  @Test
  @DisplayName("Deaktivieren verwirft alle Sitzungen des Kontos")
  void deaktivieren_verwirft_alle_sitzungen() throws Exception {
    assertThat(
            alsAdmin
                .aendere("/api/admin/users/" + zielId + "/active", "{\"active\":false}")
                .status())
        .isEqualTo(200);
    beideSitzungenVerworfen();
  }

  @Test
  @DisplayName("Eine Rollenaenderung verwirft alle Sitzungen des Kontos")
  void rollenaenderung_verwirft_alle_sitzungen() throws Exception {
    assertThat(
            alsAdmin
                .aendere("/api/admin/users/" + zielId + "/role", "{\"role\":\"ADMIN\"}")
                .status())
        .isEqualTo(200);
    beideSitzungenVerworfen();
  }

  /**
   * Der Fall, der ohne Entzug ein Leck waere: Wer einem Nutzer einen von zwei Mandanten nimmt,
   * nimmt ihm moeglicherweise genau den, den er gerade aktiv hat. E5 und E9 greifen hier
   * ineinander.
   */
  @Test
  @DisplayName("Eine Mandantenaenderung verwirft alle Sitzungen des Kontos")
  void mandantenaenderung_verwirft_alle_sitzungen() throws Exception {
    assertThat(
            alsAdmin
                .aendere(
                    "/api/admin/users/" + zielId + "/tenants",
                    "{\"tenants\":[\"%s\"]}".formatted(MANDANT_B))
                .status())
        .isEqualTo(200);
    beideSitzungenVerworfen();
  }

  @Test
  @DisplayName("Ein Passwort-Reset durch den Admin verwirft alle Sitzungen des Kontos")
  void passwort_reset_verwirft_alle_sitzungen() throws Exception {
    assertThat(
            alsAdmin
                .sende(
                    "/api/admin/users/" + zielId + "/password",
                    "{\"initialPassword\":\"%s\"}".formatted(NEUES_PASSWORT))
                .status())
        .isEqualTo(200);
    beideSitzungenVerworfen();
  }

  /**
   * E6 — die Ungleichbehandlung zum Admin-Reset, und sie ist begruendet: Wer sein Passwort aendert,
   * weil er einen fremden Zugriff vermutet, will die fremden Sitzungen los sein und nicht sich
   * selbst.
   */
  @Test
  @DisplayName(
      "Die eigene Passwortaenderung verwirft die uebrigen Sitzungen und behaelt die eigene")
  void eigene_passwortaenderung_behaelt_die_eigene_sitzung() throws Exception {
    String vorher = zielErste.sitzungsId();

    Antwort geaendert =
        zielErste.sende(
            "/api/auth/password",
            "{\"oldPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(PASSWORT, NEUES_PASSWORT));

    assertThat(geaendert.status()).isEqualTo(200);
    lebt(zielErste, "die Sitzung, aus der die Aenderung kam, bleibt");
    verworfen(zielZweite, "jede andere Sitzung desselben Kontos ist weg");
    assertThat(zielErste.sitzungsId())
        .as("Die Sitzungs-ID wird zusaetzlich erneuert — zwei Angriffe, zwei Massnahmen")
        .isNotEqualTo(vorher);
    assertThat(zielErste.hole("/api/auth/me").<Boolean>json("$.mustChangePassword")).isFalse();
  }

  @Test
  @DisplayName(
      "Die Zahl verworfener Sitzungen steht am ausloesenden Ereignis, nicht in einer eigenen Art")
  void die_zahl_steht_am_ausloesenden_ereignis() throws Exception {
    alsAdmin.aendere("/api/admin/users/" + zielId + "/lock", "{\"locked\":true}");

    List<String> details =
        monitorDsl
            .fetch(
                "select detail from audit_log where event_type = ? and target_id = ?"
                    + " order by id desc",
                "SPERRE_DURCH_ADMIN",
                String.valueOf(zielId))
            .map(satz -> satz.get("detail", String.class));

    assertThat(details).isNotEmpty();
    assertThat(details.getFirst())
        .as("Ohne diese Zahl waere ein reihenweise wirkungsloser Entzug unsichtbar (E15)")
        .contains("Sitzungen verworfen: 2");
    assertThat(details.getFirst())
        .as("In keiner Protokollzeile steht jemals ein Passwort")
        .doesNotContain(PASSWORT, NEUES_PASSWORT);
  }
}
