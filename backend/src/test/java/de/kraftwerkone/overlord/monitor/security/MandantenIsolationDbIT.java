package de.kraftwerkone.overlord.monitor.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Die Vorlage.</b> Ab Schritt 4 wird dieser Test fuer jeden neuen Endpunkt kopiert — ein neuer
 * Endpunkt ohne Isolationstest wird nicht gemergt (Regel M4).
 *
 * <p>Das Muster in vier Schritten:
 *
 * <ol>
 *   <li>Ein bekannter Datensatz von <b>Mandant B</b> wird ermittelt.
 *   <li>Der Endpunkt wird als Nutzer von <b>Mandant A</b> aufgerufen — mit genau dieser Kennung.
 *   <li>Erwartet wird {@code 404} beziehungsweise eine leere Liste. <b>Niemals {@code 403}.</b>
 *   <li>Zusaetzlich: Kein Ergebnis der Antwort gehoert zu Mandant B.
 * </ol>
 *
 * <p>Schritt 4 tauscht dafuer nur den Aufruf und die Kennung aus; Aufbau, Nutzer und die Gegenprobe
 * mit der erfundenen ID bleiben. Die Gegenprobe ist der eigentliche Kern: Ein {@code 404} allein
 * genuegt nicht, es muss von „gibt es nicht" <b>ununterscheidbar</b> sein.
 */
class MandantenIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "isolation-a";
  private static final String NUTZER_B = PRAEFIX + "isolation-b";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Eine ID, die es garantiert nicht gibt — die Gegenprobe zum fremden, aber echten Mandanten. */
  private static final String ERFUNDEN = "GIBTESGARANTIERTNICHT";

  private long idA;

  @BeforeEach
  void mandantenUndNutzerAnlegen() {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_A)).as("Mandant %s fehlt", MANDANT_A).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).as("Mandant %s fehlt", MANDANT_B).isTrue();
    assertThat(mandantRepository.existiert(ERFUNDEN)).isFalse();

    idA = legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, MANDANT_A);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
  }

  @Test
  @DisplayName("Repository: Mandant A liefert null Zeilen fuer Daten von Mandant B")
  void repository_liefert_keine_fremden_zeilen() {
    List<MandantResponse> fuerA = mandantRepository.findeFuerNutzer(idA);

    assertThat(fuerA).extracting(MandantResponse::id).containsExactly(MANDANT_A);
    assertThat(fuerA)
        .as("Kein Ergebnis darf zu Mandant B gehoeren")
        .noneMatch(mandant -> mandant.id().equals(MANDANT_B));
  }

  @Test
  @DisplayName("Endpunkt: die Mandantenliste zeigt Mandant B nicht")
  void endpunkt_zeigt_fremden_mandanten_nicht() throws Exception {
    Antwort antwort = anmelden(NUTZER_A, PASSWORT).hole("/api/mandanten");

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$[*].id")).containsExactly(MANDANT_A);
    assertThat(antwort.rumpf()).doesNotContain(MANDANT_B);
  }

  @Test
  @DisplayName(
      "Ein fremder, existierender Mandant ist von einer erfundenen ID nicht zu unterscheiden")
  void fremder_mandant_und_erfundene_id_sind_ununterscheidbar() throws Exception {
    Sitzung sitzung = anmelden(NUTZER_A, PASSWORT);

    Antwort fremdAberEcht = sitzung.sende("/api/auth/mandant", wechsel(MANDANT_B));
    Antwort erfunden = sitzung.sende("/api/auth/mandant", wechsel(ERFUNDEN));

    assertThat(fremdAberEcht.status()).as("Niemals 403 — das verriete die Existenz").isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(fremdAberEcht.rumpfOhneTraceId())
        .as(
            "Unterschieden sich die beiden Antworten, liesse sich ueber den Umschalt-Endpunkt die"
                + " gesamte Mandantenliste abfragen")
        .isEqualTo(erfunden.rumpfOhneTraceId());

    // Und der aktive Mandant hat sich dabei nicht bewegt.
    assertThat(sitzung.hole("/api/auth/me").<String>json("$.mandant.id")).isEqualTo(MANDANT_A);
  }

  private static String wechsel(String mandantId) {
    return """
        {"mandantId":"%s"}"""
        .formatted(mandantId);
  }
}
