package de.kraftwerkone.overlord.monitor.payload;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Die drei Pflicht-Isolationstests des Rohdatenzugriffs</b> (Regel M4) — einer je Endpunkt. Ohne
 * sie wird nicht gemergt.
 *
 * <p>Er kopiert das Muster aus {@code NachrichtendetailIsolationDbIT} und tauscht Aufruf und
 * Kennung. Die Paarung ist dieselbe: <b>{@code VOTG} gegen {@code SUTTONS}</b> — zwei Mandanten aus
 * <b>verschiedenen Haeusern</b>.
 *
 * <p><b>Der Kern ist nicht der Statuscode, sondern die Ununterscheidbarkeit.</b> Eine fremde,
 * <i>existierende</i> {@code MessageID} und eine <i>erfundene</i> muessen dieselbe Antwort liefern
 * — gleicher Status <b>und</b> gleicher Rumpf.
 *
 * <p><b>Der Filestore wird dabei nicht angesprochen.</b> Alle Pruefungen enden vor dem Abruf: Die
 * Artefaktliste holt ohnehin nichts, und Anzeige und Download werden mit fremden beziehungsweise
 * erfundenen Kennungen aufgerufen, die schon an der Mandantenpruefung scheitern. Das ist kein
 * Zufall, sondern die Aussage: <b>Wer die Nachricht nicht sehen darf, loest keinen Abruf aus.</b>
 *
 * <p><b>Das Zeitfenster ist absolut</b> (29.12.2025), damit beide Mandanten Daten haben; ausser
 * {@code NEXANS} endet jeder Mandant am 30.12.2025 (M3).
 */
class RohdatenIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "rohdaten-votg";
  private static final String NUTZER_B = PRAEFIX + "rohdaten-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Eine MessageID, die es garantiert nicht gibt. */
  private static final String ERFUNDEN = "00000000-0000-0000-0000-gibtesnicht";

  /** Eine Artefaktkennung, die es garantiert nicht gibt. */
  private static final String ERFUNDENES_ARTEFAKT = "0-Gibt.Es.Nicht";

  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-29T00:00:00");
  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  private Sitzung aufVotg;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(MANDANT_A)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, MANDANT_A);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
    aufVotg = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String liste() {
    return "/api/nachrichten?limit=5&von="
        + URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8);
  }

  private String eineNachrichtVon(Sitzung sitzung) throws IOException, InterruptedException {
    Antwort liste = sitzung.hole(liste());
    List<String> kennungen = liste.json("$.items[*].messageId");
    assertThat(kennungen)
        .as("Ohne Daten im Fenster bewiese der Test nur, dass leer leer ist")
        .isNotEmpty();
    return kennungen.getFirst();
  }

  /** Eine echte Artefaktkennung einer Nachricht, die diese Sitzung sehen darf. */
  private String einArtefaktVon(Sitzung sitzung, String messageId)
      throws IOException, InterruptedException {
    Antwort dateien = sitzung.hole("/api/nachrichten/" + messageId + "/dateien");
    assertThat(dateien.status()).isEqualTo(200);
    List<String> kennungen = dateien.json("$..artefaktId");
    assertThat(kennungen)
        .as("Jede Nachricht traegt 3 bis 15 Artefakte (M55) — ohne welche prueft der Test nichts")
        .isNotEmpty();
    return kennungen.getFirst();
  }

  /** Der Rumpf ohne die beiden Angaben, die je Anfrage verschieden sind und sein muessen. */
  private static String vergleichbar(Antwort antwort) {
    return antwort
        .rumpfOhneTraceId()
        .replaceAll("\"instance\"\\s*:\\s*\"[^\"]*\"", "\"instance\":\"-\"");
  }

  /**
   * Die Voraussetzung, ohne die alles Folgende wertlos waere: In diesem Fenster haben beide
   * Mandanten Daten, und beide erreichen ihre <b>eigene</b> Dateiliste.
   */
  @Test
  @DisplayName("Beide Mandanten haben Daten im Fenster und sehen ihre eigenen Dateien")
  void beide_mandanten_sehen_ihre_eigenen_dateien() throws Exception {
    String eigeneVonVotg = eineNachrichtVon(aufVotg);
    String eigeneVonSuttons = eineNachrichtVon(aufSuttons);

    Antwort votg = aufVotg.hole("/api/nachrichten/" + eigeneVonVotg + "/dateien");
    Antwort suttons = aufSuttons.hole("/api/nachrichten/" + eigeneVonSuttons + "/dateien");

    assertThat(votg.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(votg.<List<String>>json("$..artefaktId")).isNotEmpty();
  }

  @Test
  @DisplayName("Die Liste traegt keinen Filestore-Verweis")
  void liste_ohne_verweis() throws Exception {
    String eigene = eineNachrichtVon(aufVotg);

    Antwort antwort = aufVotg.hole("/api/nachrichten/" + eigene + "/dateien");

    assertThat(antwort.rumpf())
        .as(
            "Weder GUID noch Ablagenkennung. Naehme ein Endpunkt sie entgegen, waere er ein offener"
                + " Proxy vor einer Produktionsablage")
        .doesNotContain("FILESTORE")
        .doesNotContain("|");
    assertThat(antwort.rumpf())
        .as("Auch keine Adresse — der ServiceConnectString traegt einen Hostnamen (G1)")
        .doesNotContain("http://")
        .doesNotContain("https://");
  }

  // ─── Endpunkt 1: GET /api/nachrichten/{messageId}/dateien ──────────────────────

  @Test
  @DisplayName(
      "Dateien: eine fremde Nachricht ist 404 und von einer erfundenen nicht zu" + " unterscheiden")
  void dateien_fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    String fremd = eineNachrichtVon(aufSuttons);

    Antwort fremdAberEcht = aufVotg.hole("/api/nachrichten/" + fremd + "/dateien");
    Antwort erfunden = aufVotg.hole("/api/nachrichten/" + ERFUNDEN + "/dateien");

    assertThat(fremdAberEcht.status())
        .as("Niemals 403 — das verriete, dass die Nachricht existiert")
        .isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht))
        .as(
            "Eine leere Liste waere eine andere Auskunft als „gibt es nicht\" — und damit eine"
                + " Auskunft ueber den Bestand")
        .isEqualTo(vergleichbar(erfunden));
  }

  @Test
  @DisplayName("Dateien: die Trennung gilt in beide Richtungen")
  void dateien_trennung_gilt_in_beide_richtungen() throws Exception {
    String vonVotg = eineNachrichtVon(aufVotg);

    Antwort antwort = aufSuttons.hole("/api/nachrichten/" + vonVotg + "/dateien");

    assertThat(antwort.status())
        .as("sonst bewiese der Test nur, dass VOTG nichts sieht")
        .isEqualTo(404);
  }

  // ─── Endpunkt 2: .../dateien/{artefaktId}/inhalt ───────────────────────────────

  @Test
  @DisplayName("Inhalt: fremde Nachricht mit echter Artefaktkennung ist 404")
  void inhalt_fremde_nachricht() throws Exception {
    String fremd = eineNachrichtVon(aufSuttons);
    String fremdesArtefakt = einArtefaktVon(aufSuttons, fremd);

    Antwort mitEchterKennung =
        aufVotg.hole("/api/nachrichten/" + fremd + "/dateien/" + fremdesArtefakt + "/inhalt");
    Antwort mitErfundener =
        aufVotg.hole(
            "/api/nachrichten/" + ERFUNDEN + "/dateien/" + ERFUNDENES_ARTEFAKT + "/inhalt");

    assertThat(mitEchterKennung.status())
        .as(
            "Die Artefaktkennung ist keine Berechtigung. Wer sie aus einer geteilten URL kennt,"
                + " bekommt trotzdem nichts")
        .isEqualTo(404);
    assertThat(mitErfundener.status()).isEqualTo(404);
    assertThat(vergleichbar(mitEchterKennung)).isEqualTo(vergleichbar(mitErfundener));
  }

  @Test
  @DisplayName("Inhalt: die Trennung gilt in beide Richtungen")
  void inhalt_trennung_gilt_in_beide_richtungen() throws Exception {
    String vonVotg = eineNachrichtVon(aufVotg);
    String artefaktVonVotg = einArtefaktVon(aufVotg, vonVotg);

    Antwort antwort =
        aufSuttons.hole("/api/nachrichten/" + vonVotg + "/dateien/" + artefaktVonVotg + "/inhalt");

    assertThat(antwort.status()).isEqualTo(404);
  }

  // ─── Endpunkt 3: .../dateien/{artefaktId}/download ─────────────────────────────

  @Test
  @DisplayName("Download: fremde Nachricht mit echter Artefaktkennung ist 404")
  void download_fremde_nachricht() throws Exception {
    String fremd = eineNachrichtVon(aufSuttons);
    String fremdesArtefakt = einArtefaktVon(aufSuttons, fremd);

    Antwort mitEchterKennung =
        aufVotg.hole("/api/nachrichten/" + fremd + "/dateien/" + fremdesArtefakt + "/download");
    Antwort mitErfundener =
        aufVotg.hole(
            "/api/nachrichten/" + ERFUNDEN + "/dateien/" + ERFUNDENES_ARTEFAKT + "/download");

    assertThat(mitEchterKennung.status()).isEqualTo(404);
    assertThat(mitErfundener.status()).isEqualTo(404);
    assertThat(vergleichbar(mitEchterKennung)).isEqualTo(vergleichbar(mitErfundener));
    assertThat(mitEchterKennung.roh().headers().firstValue("content-disposition"))
        .as("Eine 404 traegt keinen Anhang")
        .isEmpty();
  }

  @Test
  @DisplayName("Download: die Trennung gilt in beide Richtungen")
  void download_trennung_gilt_in_beide_richtungen() throws Exception {
    String vonVotg = eineNachrichtVon(aufVotg);
    String artefaktVonVotg = einArtefaktVon(aufVotg, vonVotg);

    Antwort antwort =
        aufSuttons.hole(
            "/api/nachrichten/" + vonVotg + "/dateien/" + artefaktVonVotg + "/download");

    assertThat(antwort.status()).isEqualTo(404);
  }

  // ─── Was kein Endpunkt entgegennimmt ──────────────────────────────────────────

  @Test
  @DisplayName("Kein Endpunkt nimmt eine Mandanten-ID entgegen (Regel M1)")
  void keine_mandanten_id() throws Exception {
    String fremd = eineNachrichtVon(aufSuttons);

    Antwort mitParameter =
        aufVotg.hole("/api/nachrichten/" + fremd + "/dateien?mandant=" + MANDANT_B);
    Antwort ohneParameter = aufVotg.hole("/api/nachrichten/" + fremd + "/dateien");

    assertThat(mitParameter.status()).isEqualTo(404);
    assertThat(vergleichbar(mitParameter))
        .as("Ein unbekannter Parameter aendert nichts — der Mandant kommt aus der Sitzung")
        .isEqualTo(vergleichbar(ohneParameter));
  }

  @Test
  @DisplayName("Kein Endpunkt nimmt eine Rolle entgegen — das ist der Fehler des Altsystems (Q5)")
  void keine_rolle_als_parameter() throws Exception {
    String eigene = eineNachrichtVon(aufVotg);
    String artefakt = einArtefaktVon(aufVotg, eigene);

    Antwort ohne = aufVotg.hole("/api/nachrichten/" + eigene + "/dateien/" + artefakt + "/inhalt");
    Antwort mitVersuch =
        aufVotg.hole(
            "/api/nachrichten/"
                + eigene
                + "/dateien/"
                + artefakt
                + "/inhalt?role=Admin&downloadUser="
                + NUTZER_B);

    assertThat(mitVersuch.status()).isEqualTo(ohne.status());
    if (ohne.status() == 200) {
      assertThat(mitVersuch.<Boolean>json("$.beschnitten"))
          .as(
              "Im Altsystem entscheidet request.getParameter(\"downloadUser\") ueber den Beschnitt"
                  + " (JsonServlet.java:788–:790). Hier kommt die Rolle aus der Sitzung")
          .isEqualTo(ohne.<Boolean>json("$.beschnitten"));
    }
  }

  @Test
  @DisplayName("Kein Endpunkt nimmt einen Filestore-Verweis entgegen")
  void kein_verweis_als_pfad() throws Exception {
    String eigene = eineNachrichtVon(aufVotg);

    // Eine Ablagenkennung samt UUID als Artefaktkennung: Der Endpunkt kennt diese Form nicht,
    // und sie trifft auch keine Zeile. Genau das ist der Unterschied zum Altsystem.
    Antwort antwort =
        aufVotg.hole(
            "/api/nachrichten/"
                + eigene
                + "/dateien/"
                + URLEncoder.encode(
                    "FILESTOREPROD09|deadbeef-0000-4000-8000-0123456789ab", StandardCharsets.UTF_8)
                + "/inhalt");

    assertThat(antwort.status()).isEqualTo(404);
  }
}
