package de.kraftwerkone.overlord.monitor.bam;

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
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Die Property-Suche am laufenden Endpunkt — <b>beide Pfade, die Verundung mit einer Belegnummer
 * und die Parametergrenzen</b>.
 *
 * <p><b>Kein Pruefwert steht in dieser Datei</b> (Regeln G1 und T2). Der Anker ist die erste
 * Nachricht von {@code NEXANS} am dichtesten Tag; Eigenschaften, Prozess, Ablauf und Status werden
 * von ihr gelesen. Geprueft werden Eigenschaften — „die Nachricht ist dabei", „genau diese eine" —
 * und keine Zahlen aus dem Pflegestand.
 */
class FeldSucheDbIT extends SicherheitsTestbasis {

  private static final String NEXANS = "NEXANS";
  private static final String NUTZER = PRAEFIX + "feldsuche-nexans";
  private static final String PASSWORT = "einLangesPasswort1";

  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-11-30T00:00:00");
  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");
  private static final LocalDateTime TAG_VON = LocalDateTime.parse("2025-12-29T00:00:00");
  private static final LocalDateTime TAG_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung aufNexans;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(NEXANS)).isTrue();
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, NEXANS);
    aufNexans = anmelden(NUTZER, PASSWORT);
  }

  // ─── Herleitung ───────────────────────────────────────────────────────────────

  /** Der Anker: eine Nachricht samt allem, was ihre Spaltenfelder tragen. */
  private record Anker(
      String messageId,
      String processId,
      String processName,
      String sosId,
      String sosName,
      String status) {}

  private Anker anker() {
    Record fund =
        glassfishDsl
            .resultQuery(
                """
                select m.MessageID as kennung, m.ProcessID as prozess, p.ProcessName as prozessname,
                       m.SOSID as sos, s.SOSName as sosname, m.MessageStatus as status
                from Message m
                join Process p         on p.ProcessID  = m.ProcessID
                join ProjectMandant pm on pm.ProjectID = p.ProjectID
                left join SOS s        on s.SOSID      = m.SOSID
                where pm.MandantID = ?
                  and m.MessageLastUpdate >= ?
                  and m.MessageLastUpdate <  ?
                  and m.MessageStatus is not null
                  and s.SOSName is not null
                order by m.MessageID
                limit 1
                """,
                NEXANS,
                TAG_VON,
                TAG_BIS)
            .fetchOne();
    assertThat(fund)
        .as("NEXANS hat am dichtesten Tag keine Nachricht mit Ablauf — Testkopie geaendert?")
        .isNotNull();
    return new Anker(
        fund.get("kennung", String.class),
        fund.get("prozess", String.class),
        fund.get("prozessname", String.class),
        fund.get("sos", String.class),
        fund.get("sosname", String.class),
        fund.get("status", String.class));
  }

  /** Eine seltene Eigenschaft des Ankers — siehe {@code FeldSucheIsolationDbIT}. */
  private String[] selteneEigenschaft(Anker anker) {
    List<Record> kandidaten =
        glassfishDsl.fetch(
            """
            select MessagePropertyName as name, MessagePropertyValue as wert
            from MessageProperty
            where MessageID = ?
              and char_length(MessagePropertyValue) between 8 and 50
            order by char_length(MessagePropertyValue) desc, MessagePropertyName
            """,
            anker.messageId());
    for (Record kandidat : kandidaten) {
      String name = kandidat.get("name", String.class);
      String wert = kandidat.get("wert", String.class);
      Integer traeger =
          glassfishDsl
              .resultQuery(
                  "select count(distinct MessageID) from MessageProperty"
                      + " where MessagePropertyName = ? and MessagePropertyValue = ?",
                  name,
                  wert)
              .fetchOne(0, Integer.class);
      if (traeger != null && traeger <= BamSucheRepository.HOECHSTENS_TREFFER) {
        return new String[] {name, wert};
      }
    }
    throw new AssertionError("Keine seltene Eigenschaft auf dem Anker — Bestand geaendert?");
  }

  /** Ein BAM-Wert des Ankers, falls er einen traegt. */
  private String einBamWert(Anker anker) {
    return glassfishDsl
        .resultQuery(
            "select MessageBAMValue from MessageBAM where MessageID = ?"
                + " order by MessageBAMType, MessageBAMValue limit 1",
            anker.messageId())
        .fetchOne(0, String.class);
  }

  // ─── Aufrufe ──────────────────────────────────────────────────────────────────

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  /**
   * {@code begriff=} und {@code feld=} gemischt: ein Eintrag mit fuehrendem {@code #} ist ein
   * BAM-Begriff.
   */
  private String suche(String... eintraege) {
    StringBuilder pfad = new StringBuilder("/api/bam/suche?von=");
    pfad.append(URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8));
    pfad.append("&bis=").append(URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8));
    for (String eintrag : eintraege) {
      if (eintrag.startsWith("#")) {
        pfad.append("&begriff=")
            .append(URLEncoder.encode(eintrag.substring(1), StandardCharsets.UTF_8));
      } else {
        pfad.append("&feld=").append(URLEncoder.encode(eintrag, StandardCharsets.UTF_8));
      }
    }
    return pfad.toString();
  }

  private static List<String> kennungen(Antwort antwort) {
    return antwort.json("$.nachrichten[*].messageId");
  }

  // ─── Der EAV-Zugriff ──────────────────────────────────────────────────────────

  /** <b>Das Abnahmekriterium:</b> Eine Suche ueber Name und Wert findet die Nachricht. */
  @Test
  @DisplayName("Eine Suche ueber Name und Wert findet die Nachricht und zitiert das Feld")
  void eigenschaft_wird_gefunden() throws Exception {
    Anker anker = anker();
    String[] eigenschaft = selteneEigenschaft(anker);

    Antwort antwort = aufNexans.hole(suche(eigenschaft[0] + ":" + eigenschaft[1]));

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(200);
    assertThat(kennungen(antwort)).contains(anker.messageId());
    assertThat(antwort.<List<String>>json("$.felder[*].name")).containsExactly(eigenschaft[0]);
    assertThat(antwort.<List<String>>json("$.felder[*].wert")).containsExactly(eigenschaft[1]);
    assertThat(antwort.<List<Boolean>>json("$.felder[*].spalte")).containsExactly(false);
    assertThat(antwort.<List<String>>json("$.begriffe[*].eingabe")).isEmpty();
    assertThat(antwort.<List<List<?>>>json("$.nachrichten[*].treffer"))
        .as("ohne BAM-Begriff gibt es keinen BAM-Treffer — leer statt fehlend")
        .allSatisfy(treffer -> assertThat(treffer).isEmpty());
  }

  // ─── Das Spaltenpraedikat ─────────────────────────────────────────────────────

  /** {@code Message.MessageID} findet genau die eine Nachricht — der Primaerschluessel. */
  @Test
  @DisplayName("Message.MessageID findet genau die eine Nachricht, als Spalte")
  void messageid_findet_genau_eine() throws Exception {
    Anker anker = anker();

    Antwort antwort = aufNexans.hole(suche("Message.MessageID:" + anker.messageId()));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(kennungen(antwort)).containsExactly(anker.messageId());
    assertThat(antwort.<List<Boolean>>json("$.felder[*].spalte")).containsExactly(true);
    assertThat(antwort.<Boolean>json("$.abgeschnitten")).isFalse();
  }

  /**
   * <b>Die Spaltenfelder, die einen Join brauchen, und der Status — je verundet mit der
   * Kennung.</b> Passt der zweite Begriff zur Nachricht, bleibt sie; passt er nicht, ist die Liste
   * leer.
   */
  @Test
  @DisplayName("ProcessName, SOSName, ProcessID, SOSID und Status greifen als Spaltenpraedikat")
  void die_uebrigen_spaltenfelder_greifen() throws Exception {
    Anker anker = anker();
    String kennung = "Message.MessageID:" + anker.messageId();

    for (String passend :
        List.of(
            "Message.ProcessName:" + anker.processName(),
            "Message.SOSName:" + anker.sosName(),
            "Message.ProcessID:" + anker.processId(),
            "Message.SOSID:" + anker.sosId(),
            "Message.Status:" + anker.status())) {
      Antwort antwort = aufNexans.hole(suche(kennung, passend));
      assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(200);
      assertThat(kennungen(antwort)).as(passend).containsExactly(anker.messageId());
    }

    Antwort unpassend = aufNexans.hole(suche(kennung, "Message.Status:" + anker.status() + "X"));
    assertThat(kennungen(unpassend)).as("ein Status, den es nicht gibt").isEmpty();
  }

  // ─── Die Verundung mit einer Belegnummer ──────────────────────────────────────

  /**
   * <b>BAM-Begriff und Feldbegriff werden verundet</b> — auf derselben Nachricht. Die Trefferzeile
   * beschriftet den BAM-Treffer wie bisher; das Feld steht im Zitat.
   */
  @Test
  @DisplayName("Belegnummer und Feld werden verundet, der BAM-Treffer bleibt beschriftet")
  void belegnummer_und_feld_werden_verundet() throws Exception {
    Anker anker = anker();
    String bam = einBamWert(anker);
    assertThat(bam)
        .as("der Anker traegt keinen BAM-Wert — dann hat sich der Bestand geaendert")
        .isNotNull();

    Antwort beide = aufNexans.hole(suche("#:" + bam, "Message.MessageID:" + anker.messageId()));
    assertThat(beide.status()).as(beide.rumpf()).isEqualTo(200);
    assertThat(kennungen(beide)).containsExactly(anker.messageId());
    assertThat(beide.<List<String>>json("$.nachrichten[0].treffer[*].wert"))
        .as("der BAM-Treffer wird wie bisher beschriftet")
        .contains(bam);
    assertThat(beide.<List<String>>json("$.begriffe[*].eingabe")).containsExactly(bam);
    assertThat(beide.<List<String>>json("$.felder[*].name")).containsExactly("Message.MessageID");

    Antwort widerspruch = aufNexans.hole(suche("#:" + bam, "Message.MessageID:" + "X".repeat(36)));
    assertThat(kennungen(widerspruch)).as("Verundung, nicht Veroderung").isEmpty();
  }

  // ─── Die Parametergrenzen ─────────────────────────────────────────────────────

  /** Die vier Fehlerfaelle der Parameterform am laufenden Endpunkt. */
  @Test
  @DisplayName("Die Parametergrenzen gelten am Endpunkt: feldname-fehlt, ohne Trenner, zu viele")
  void parametergrenzen_am_endpunkt() throws Exception {
    Antwort ohneName = aufNexans.hole(suche(":4711"));
    assertThat(ohneName.status()).isEqualTo(400);
    assertThat(ohneName.<String>json("$.type")).endsWith("/feldname-fehlt");

    Antwort ohneTrenner = aufNexans.hole(suche("MessageGUID"));
    assertThat(ohneTrenner.status()).isEqualTo(400);
    assertThat(ohneTrenner.<String>json("$.type")).endsWith("/feldbegriff-ohne-trenner");

    Antwort leer = aufNexans.hole(suche("Message.GUID:"));
    assertThat(leer.status()).isEqualTo(400);
    assertThat(leer.<String>json("$.type")).endsWith("/suchbegriff-fehlt");

    String[] neun = new String[9];
    neun[0] = "#:4711";
    for (int i = 1; i < 9; i++) {
      neun[i] = "Message.GUID:" + i;
    }
    Antwort zuViele = aufNexans.hole(suche(neun));
    assertThat(zuViele.status()).isEqualTo(400);
    assertThat(zuViele.<String>json("$.type")).endsWith("/zu-viele-suchbegriffe");
  }

  /** Das Fenster gilt fuer beide Begriffsarten, und die Antwort nennt es. */
  @Test
  @DisplayName("Auch ohne Fenster wird die Vorgabe genannt, und ein Jahr plus ein Tag ist 400")
  void fenster_gilt_auch_fuer_felder() throws Exception {
    Anker anker = anker();
    Antwort ohneFenster =
        aufNexans.hole(
            "/api/bam/suche?feld="
                + URLEncoder.encode(
                    "Message.MessageID:" + anker.messageId(), StandardCharsets.UTF_8));
    assertThat(ohneFenster.status()).isEqualTo(200);
    assertThat(ohneFenster.hatFeld("$.von")).isTrue();
    assertThat(ohneFenster.hatFeld("$.bis")).isTrue();

    Antwort zuGross =
        aufNexans.hole(
            "/api/bam/suche?von="
                + URLEncoder.encode(
                    iso(FENSTER_BIS.minusYears(1).minusDays(1)), StandardCharsets.UTF_8)
                + "&bis="
                + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8)
                + "&feld="
                + URLEncoder.encode(
                    "Message.MessageID:" + anker.messageId(), StandardCharsets.UTF_8));
    assertThat(zuGross.status()).isEqualTo(400);
    assertThat(zuGross.<String>json("$.type")).endsWith("/zeitfenster-zu-gross");
  }
}
