package de.kraftwerkone.overlord.monitor.message;

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
 * Das Nachrichtendetail gegen die <b>Testkopie</b>: echte Schrittfolgen, echte Namen, echter
 * Wartezustand.
 *
 * <p><b>Keine fest eingetragene {@code MessageID}.</b> Die Bezugsnachrichten werden ueber den
 * Listen-Endpunkt <i>gefunden</i> — nach ihrer <b>Gestalt</b>, nicht nach ihrer Kennung. Eine
 * eingetragene Kennung waere beim naechsten Befuellen der Testkopie ein rot gewordener Test, der
 * nichts ueber den Code aussagt.
 *
 * <p>Mandant ist {@code NEXANS}: Er traegt praktisch das gesamte Nachrichtenaufkommen, und <b>alle
 * 538 wartenden Nachrichten der Testkopie gehoeren ihm</b> (gemessen, 23. bis 29.12.2025). Ohne ihn
 * waere der wichtigste Fall dieses Schritts — {@code WARTET_IN} — nicht pruefbar.
 */
class NachrichtendetailDbIT extends SicherheitsTestbasis {

  private static final String MANDANT = "NEXANS";
  private static final String NUTZER = PRAEFIX + "detail-nexans";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Die wartenden Nachrichten liegen zwischen dem 23. und dem 29.12.2025. */
  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-23T00:00:00");

  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  private Sitzung sitzung;

  @BeforeEach
  void anmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(MANDANT)).isTrue();
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT);
    sitzung = anmelden(NUTZER, PASSWORT);
  }

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String liste(String zusatz) {
    return "/api/nachrichten?limit=50&von="
        + URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8)
        + zusatz;
  }

  private List<String> kennungen(String zusatz) throws IOException, InterruptedException {
    return sitzung.hole(liste(zusatz)).json("$.items[*].messageId");
  }

  private Antwort detail(String messageId) throws IOException, InterruptedException {
    Antwort antwort = sitzung.hole("/api/nachrichten/" + messageId);
    assertThat(antwort.status()).as("Rumpf: %s", antwort.rumpf()).isEqualTo(200);
    return antwort;
  }

  @Test
  @DisplayName("Der Kopf traegt Rohstatus, Einordnung, Ablaufnamen und den fachlichen Start")
  void kopf_ist_vollstaendig() throws Exception {
    Antwort detail = detail(kennungen("").getFirst());

    assertThat(detail.<String>json("$.messageId")).isNotBlank();
    assertThat(detail.<String>json("$.status")).isNotBlank();
    assertThat(detail.<String>json("$.statusKind")).isNotBlank();
    assertThat(detail.<String>json("$.sosName"))
        .as("SOSName ist der Anzeigename des Ablaufs und durchgaengig gepflegt (L14)")
        .isNotBlank();
    assertThat(detail.<String>json("$.zeitpunkt")).endsWith("Z");
    assertThat(detail.<String>json("$.start"))
        .as("der fachliche Start ist MIN(MessageActionStart) und nicht MessageLastUpdate (Q2)")
        .endsWith("Z");
    assertThat(detail.<Integer>json("$.eigenschaftenAnzahl"))
        .as("gemessen sind rund 22,6 Eigenschaften je Nachricht (M17 1)")
        .isPositive();
    assertThat(detail.<String>json("$.offenerZustand")).isNotBlank();
  }

  /**
   * <b>Die Rollen im Kopf</b> (Schritt 6, Teil 2b) — <b>immer vorhanden, leer statt fehlend</b>.
   *
   * <p>{@code JsonPath.read} wirft, wenn das Feld fehlt: Genau das ist hier die Pruefung. Ein
   * fehlendes Feld hiesse „unbekannt", ein leeres heisst „nicht in einer Kette" — und daran
   * entscheidet die Oberflaeche, ob sie {@code /kette} ueberhaupt ruft.
   *
   * <p><b>Beide Sortierrichtungen</b>, damit die Stichprobe nicht an einem Ende des Fensters
   * haengt. Dass darin sowohl verkettete als auch unverkettete Nachrichten vorkommen, ruht auf der
   * gemessenen Verteilung: Ueber Fenster B tragen 39.090 von 214.330 Zeilen keine Rolle (18 %) und
   * 104.049 die Rolle {@code SPLIT_KIND} (M30‑4).
   */
  @Test
  @DisplayName("Die Rollen stehen im Kopf — immer vorhanden, leer statt fehlend")
  void rollen_stehen_im_kopf() throws Exception {
    List<String> erlaubt = List.of("SPLIT_WURZEL", "SPLIT_KIND", "MERGE_EINGANG", "MERGE_ERGEBNIS");
    int mitKette = 0;
    int ohneKette = 0;

    for (String zusatz : List.of("", "&sortierung=aelteste")) {
      for (String kennung : kennungen(zusatz)) {
        List<String> rollen = detail(kennung).json("$.rollen");

        assertThat(rollen).as("unbekannte Rolle bei %s", kennung).isSubsetOf(erlaubt);
        assertThat(rollen)
            .as("nie mehr als zwei Rollen je Zeile — gemessen, nicht garantiert (M28‑1c)")
            .hasSizeLessThanOrEqualTo(2);
        if (rollen.isEmpty()) {
          ohneKette++;
        } else {
          mitKette++;
        }
      }
    }

    assertThat(mitKette).as("keine einzige verkettete Nachricht in der Stichprobe").isPositive();
    assertThat(ohneKette)
        .as(
            "keine einzige unverkettete Nachricht — dann belegt die Stichprobe die leere Menge nicht")
        .isPositive();
  }

  /**
   * <b>Abnahmekriterium 2.</b> Jeder Schritt traegt einen Namen oder einen Rohwert, keiner steht
   * leer — und die Herkunft ist an jedem ablesbar.
   */
  @Test
  @DisplayName("Kein Schritt steht leer, und jeder traegt seine Namensherkunft")
  void kein_schritt_steht_leer() throws Exception {
    int geprueft = 0;
    for (String kennung : kennungen("")) {
      Antwort detail = detail(kennung);
      List<String> namen = detail.json("$.schritte[*].name");
      List<String> herkunft = detail.json("$.schritte[*].namensherkunft");
      List<Integer> positionen = detail.json("$.schritte[*].position");

      assertThat(namen).as("Nachricht ohne Schritt: %s", kennung).isNotEmpty();
      assertThat(namen).allSatisfy(name -> assertThat(name).isNotBlank());
      assertThat(herkunft)
          .allSatisfy(wert -> assertThat(wert).isIn("DIREKT", "HERGELEITET", "ROHWERT"));
      assertThat(herkunft).hasSameSizeAs(namen);
      assertThat(positionen)
          .as("Der Metadaten-Schritt SOSActionID = 0 erscheint nie in der Schrittfolge (S1)")
          .doesNotContain(0);
      geprueft++;
    }
    assertThat(geprueft).isPositive();
  }

  @Test
  @DisplayName("Die Schrittfolge ist nach Start sortiert und traegt Dauern in ganzen Sekunden")
  void schrittfolge_ist_sortiert_und_traegt_dauern() throws Exception {
    Antwort detail = detail(kennungen("").getFirst());

    List<String> starts = detail.json("$.schritte[*].start");
    assertThat(starts).isSorted();
    assertThat(starts).allSatisfy(start -> assertThat(start).endsWith("Z"));

    List<Integer> dauern = detail.json("$.schritte[*].dauerSekunden");
    assertThat(dauern)
        .as("eine negative Dauer wird null geliefert, niemals als negative Zahl")
        .allSatisfy(dauer -> assertThat(dauer == null || dauer >= 0).isTrue());
  }

  /**
   * <b>Abnahmekriterium 3.</b> Fuer eine wartende Nachricht liefert der Endpunkt {@code WARTET_IN}
   * samt dem Schritt, auf den der Verweis zeigt, und der Wartedauer.
   *
   * <p><b>{@code WARTET_IN} und nicht {@code WARTET_VOR}</b> — geaendert am 10.08.2026 mit M29. Der
   * Verweis aus {@code Message.SOSID}/{@code SOSActionID} zeigt bei <b>allen 538</b> wartenden
   * Nachrichten auf den Schritt, der <i>zuletzt gelaufen</i> ist: den mit {@code
   * WAITUNTIL|…|SUSPEND}, der die Nachricht schlafen legt. {@code WARTET_VOR} kommt in der
   * Testkopie null Mal vor und ist deshalb hier nicht pruefbar (siehe {@code nachrichtendetail.md}
   * §10.12).
   */
  @Test
  @DisplayName("Eine wartende Nachricht wartet IN ihrem letzten Schritt — WARTET_IN mit Wartedauer")
  void wartende_nachricht_liefert_wartet_in() throws Exception {
    List<String> wartende = kennungen("&status=WARTEND");
    assertThat(wartende)
        .as("Ohne eine wartende Nachricht im Fenster prueft dieser Test nichts")
        .isNotEmpty();

    int geprueft = 0;
    for (String kennung : wartende) {
      Antwort detail = detail(kennung);

      assertThat(detail.<String>json("$.offenerZustand"))
          .as("Nachricht %s: der Verweis zeigt auf den zuletzt gelaufenen Schritt (M29)", kennung)
          .isEqualTo("WARTET_IN");
      assertThat(detail.<String>json("$.naechsterSchritt"))
          .as("bei allen 538 wartenden Nachrichten loest der Verweis auf (M13, M29 3)")
          .isNotBlank();
      assertThat(detail.<List<Boolean>>json("$.schritte[*].laeuftAuf"))
          .as("wer wartet, laeuft auf keinem Schritt")
          .allSatisfy(laeuftAuf -> assertThat(laeuftAuf).isFalse());
      assertThat(detail.<Integer>json("$.wartetSeitSekunden"))
          .as("die Wartedauer wird im Backend gegen die Anwendungsuhr gerechnet, nie im Browser")
          .isNotNull()
          .isNotNegative();
      geprueft++;
    }
    assertThat(geprueft).isPositive();
  }

  /**
   * Die Problemkategorie <b>Ueberfaellig</b> (§4.2 Nr. 2), an echten Daten.
   *
   * <p>Der Test verlangt <b>keinen</b> bestimmten Wert: Ob eine Nachricht ueberfaellig ist, haengt
   * am Stand der Anwendungsuhr und damit am Befuellstand der Testkopie. Geprueft wird die
   * <i>Kopplung</i> — ohne Frist keine Ueberfaelligkeit, und wer ueberfaellig ist, ist offen. Genau
   * die beiden Bedingungen, die {@code MessageStatusClassifier.istUeberfaellig} verbindet.
   */
  @Test
  @DisplayName("Ueberfaellig setzt eine Frist und einen offenen Zustand voraus")
  void ueberfaellig_haengt_an_frist_und_offenheit() throws Exception {
    int geprueft = 0;
    for (String kennung : kennungen("")) {
      Antwort detail = detail(kennung);
      boolean ueberfaellig = Boolean.TRUE.equals(detail.<Boolean>json("$.ueberfaellig"));

      if (ueberfaellig) {
        assertThat(detail.<Integer>json("$.fristSekunden"))
            .as("Nachricht %s: ohne Frist gibt es keine Ueberfaelligkeit", kennung)
            .isNotNull()
            .isPositive();
        assertThat(detail.<String>json("$.offenerZustand"))
            .as("Nachricht %s: ein Endstatus kann nicht ueberfaellig werden", kennung)
            .isNotEqualTo("KEINER");
      }
      geprueft++;
    }
    assertThat(geprueft).isPositive();
  }

  /**
   * Die Gesamtdauer ist die Abdeckung fuer den Fall, den die gestrichene Lueckenzeile fangen
   * sollte: Zeit, die zwischen zwei Schritten steckt und in keiner Schrittdauer auftaucht.
   */
  @Test
  @DisplayName("Die Gesamtdauer deckt die Summe der Schrittdauern ab")
  void gesamtdauer_deckt_die_schrittdauern_ab() throws Exception {
    int geprueft = 0;
    for (String kennung : kennungen("")) {
      Antwort detail = detail(kennung);
      Integer gesamt = detail.json("$.gesamtdauerSekunden");
      if (gesamt == null) {
        continue;
      }
      long summe =
          detail.<List<Integer>>json("$.schritte[*].dauerSekunden").stream()
              .filter(java.util.Objects::nonNull)
              .mapToLong(Integer::longValue)
              .sum();

      assertThat((long) gesamt)
          .as(
              "Nachricht %s: die Gesamtdauer laeuft vom fachlichen Start bis MessageLastUpdate und"
                  + " umfasst damit jede Schrittdauer",
              kennung)
          .isGreaterThanOrEqualTo(summe);
      geprueft++;
    }
    assertThat(geprueft).isPositive();
  }

  @Test
  @DisplayName("Eine abgeschlossene Nachricht traegt KEINER und keinen naechsten Schritt")
  void abgeschlossene_nachricht_traegt_keinen_zustand() throws Exception {
    List<String> abgeschlossene = kennungen("&status=ABGESCHLOSSEN");
    assertThat(abgeschlossene).isNotEmpty();

    Antwort detail = detail(abgeschlossene.getFirst());

    assertThat(detail.<String>json("$.offenerZustand")).isEqualTo("KEINER");
    assertThat(detail.hatFeld("$.naechsterSchritt"))
        .as("er benennt den Schritt, VOR dem gewartet wird — ohne Warten gibt es ihn nicht")
        .isFalse();
  }

  /**
   * Die dreistufige Aufloesung traegt gemessen zu 99,72 Prozent (dichter Tag) einen echten Namen.
   * Der Test verlangt keine Quote, sondern nur, dass <b>beide</b> auffuellenden Stufen ueberhaupt
   * vorkommen — sonst waere Stufe 2 ungeprueftes Beiwerk.
   *
   * <p><b>Gesucht wird am alten Ende des Fensters</b> ({@code sortierung=aelteste}), und das ist
   * kein Zufall: Die namenlosen Schritte haengen an einem einzigen Ablauf (M20), und der laeuft in
   * diesem Fenster bis zum <b>29.12.2025 um 23:03</b>. Die <i>neuesten</i> fuenfzig Nachrichten
   * liegen danach und tragen nachweislich keinen einzigen — gegen sie geprueft bewiese der Test
   * nur, dass Stufe 2 nichts zu tun hatte. Am alten Ende tragen 49 von 50 Nachrichten einen
   * namenlosen Schritt.
   */
  @Test
  @DisplayName("Es gibt sowohl direkt aufgeloeste als auch hergeleitete Schritte")
  void beide_aufloesungsstufen_kommen_vor() throws Exception {
    boolean direkt = false;
    boolean hergeleitet = false;
    for (String kennung : kennungen("&sortierung=aelteste")) {
      List<String> herkunft = detail(kennung).json("$.schritte[*].namensherkunft");
      direkt |= herkunft.contains("DIREKT");
      hergeleitet |= herkunft.contains("HERGELEITET");
      if (direkt && hergeleitet) {
        break;
      }
    }
    assertThat(direkt).as("Stufe 1 traegt 71,46 Prozent der echten Schritte (M18)").isTrue();
    assertThat(hergeleitet)
        .as(
            "Stufe 2 traegt weitere 28,26 Prozent — ohne sie zeigte jeder vierte Schritt einen"
                + " Rohwert")
        .isTrue();
  }

  @Test
  @DisplayName("Die Eigenschaften kommen ueber den zweiten Endpunkt, und ihre Zahl stimmt")
  void eigenschaften_stimmen_mit_der_anzahl_im_kopf() throws Exception {
    String kennung = kennungen("").getFirst();
    int angekuendigt = detail(kennung).json("$.eigenschaftenAnzahl");

    Antwort eigenschaften = sitzung.hole("/api/nachrichten/" + kennung + "/eigenschaften");

    assertThat(eigenschaften.status()).isEqualTo(200);
    assertThat(eigenschaften.<List<String>>json("$[*].name"))
        .as("die Zahl im Kopf beschriftet den eingeklappten Block — sie muss stimmen")
        .hasSize(angekuendigt);
    assertThat(eigenschaften.<List<String>>json("$[*].wert"))
        .allSatisfy(wert -> assertThat(wert).isNotNull());
    assertThat(eigenschaften.<List<Boolean>>json("$[*].gekappt"))
        .as(
            "der laengste im dichten Monat gemessene Wert liegt bei 12.732 Byte, die Grenze bei"
                + " 16.384")
        .allSatisfy(gekappt -> assertThat(gekappt).isFalse());
  }

  /**
   * Die Metadaten-Eigenschaften gehen nicht verloren, obwohl ihr Schritt aus der Schrittfolge
   * fliegt: Sie werden ueber die {@code MessageID} gelesen und nicht ueber die Aktion (M17 3).
   */
  @Test
  @DisplayName("Die Eigenschaften des Metadaten-Schritts sind trotzdem da")
  void eigenschaften_des_metadatenschritts_sind_da() throws Exception {
    String kennung = kennungen("").getFirst();

    Antwort eigenschaften = sitzung.hole("/api/nachrichten/" + kennung + "/eigenschaften");

    assertThat(eigenschaften.<List<Integer>>json("$[*].position"))
        .as("Schritt 0 traegt 58,8 Prozent aller MessageProperty-Zeilen")
        .contains(0);
    assertThat(eigenschaften.<List<String>>json("$[*].name")).contains("Message.GUID");
  }

  /*
   * Hier stand bis zum 11.08.2026 ein Test, der `GET /api/nachrichten/merkmale` gegen die
   * Pfadvariable `/api/nachrichten/{messageId}` abgrenzte — Spring bevorzugt das woertliche
   * Segment. Der Endpunkt ist entfallen (docs/nachrichtenliste.md §5); die Regel selbst wird
   * weiterhin festgehalten, jetzt in `KettenDbIT.bestehende_endpunkte_bleiben_erreichbar`.
   */

  @Test
  @DisplayName("Ein Aufruf ohne Anmeldung kommt gar nicht erst durch")
  void ohne_anmeldung_kein_detail() throws Exception {
    Sitzung ohneAnmeldung = neueSitzung();

    assertThat(ohneAnmeldung.hole("/api/nachrichten/beliebig").status()).isEqualTo(401);
    assertThat(ohneAnmeldung.hole("/api/nachrichten/beliebig/eigenschaften").status())
        .isEqualTo(401);
  }
}
