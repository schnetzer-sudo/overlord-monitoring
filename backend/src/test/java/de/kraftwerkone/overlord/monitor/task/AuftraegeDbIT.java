package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.AUDIT_LOG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.jooq.monitor.tables.records.TaskRecord;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Anlegen, Wiederholen, die simulierte Plattform und seit A4 das Loeschen offener Auftraege ({@code
 * docs/tasks.md} §6, {@code docs/task-schnittstelle.md}).
 *
 * <p><b>Die Plattform wird mit ihren eigenen Statements simuliert</b> — woertlich die aus {@link
 * Plattformvertrag}, die {@code PlattformvertragTest} mit der Datei fuer das Plattform-Team
 * gleichhaelt. Damit ist der Vertrag ausfuehrbar belegt und nicht nur beschrieben.
 *
 * <p><b>Keine Wanduhr in Zusicherungen</b> (Regel T1): Zeitstempel werden auf vorhanden und
 * Reihenfolge geprueft, nicht auf Werte.
 */
class AuftraegeDbIT extends AuftragsTestbasis {

  private static final String NUTZER = PRAEFIX + "auftrag-nexans";

  @Autowired private AuftragService auftragService;
  @Autowired private PlatformTransactionManager transactionManager;

  private long nutzerId;
  private Sitzung sitzung;

  @BeforeEach
  void anmelden() throws Exception {
    nutzerId = legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, NEXANS);
    sitzung = anmelden(NUTZER, PASSWORT);
  }

  @Test
  @DisplayName(
      "Anlegen: 201, eine Zeile OFFEN mit eingestellt_am, Kopie der Definition, Eintraegen")
  void anlegen() throws Exception {
    String anfrageId = neueAnfrageId();
    Antwort antwort = sitzung.sende("/api/tasks", neuanlage(anfrageId));

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(201);
    String taskId = antwort.json("$.taskId");
    assertThat(antwort.roh().headers().firstValue("location")).hasValue("/api/tasks/" + taskId);
    assertThat(antwort.<String>json("$.status")).isEqualTo("OFFEN");
    assertThat(antwort.<String>json("$.einordnung")).isEqualTo("OFFEN");
    assertThat(antwort.<String>json("$.eingestelltAm")).endsWith("Z");
    assertThat(antwort.<String>json("$.name")).isEqualTo(Testdefinitionen.WERK_NAME);
    assertThat(antwort.<List<String>>json("$.eintraege[*].schluessel"))
        .containsExactly("72332/DDB0", "0101159903/22/001N0");

    TaskRecord zeile = monitorDsl.selectFrom(TASK).where(TASK.TASK_ID.eq(taskId)).fetchOne();
    assertThat(zeile).isNotNull();
    assertThat(zeile.getMandantId()).isEqualTo(NEXANS);
    assertThat(zeile.getTaskTyp()).isEqualTo(WERK);
    assertThat(zeile.getStatus()).isEqualTo("OFFEN");
    assertThat(zeile.getEingestelltAm()).isNotNull();
    assertThat(zeile.getAbgeholtAm()).isNull();
    assertThat(zeile.getAusgefuehrtAm()).isNull();
    assertThat(zeile.getAnfrageId()).isEqualTo(anfrageId);
    assertThat(zeile.getAngelegtVon()).isEqualTo(NUTZER);
    assertThat(zeile.getAktion()).isEqualTo("Neuanlage");
    assertThat(zeile.getParameter())
        .isEqualTo("Aktion=Neuanlage\n72332/DDB0,120764,\n0101159903/22/001N0,,100050");

    // Die Kopie: Name und Struktur, inhaltlich gleich der Typzeile zum Anlagezeitpunkt.
    JsonNode kopie = Testdefinitionen.JSON.readTree(zeile.getDefinition().data());
    assertThat(kopie.get("name").stringValue()).isEqualTo(Testdefinitionen.WERK_NAME);
    String struktur =
        monitorDsl
            .select(TASK_TYPE.STRUKTUR)
            .from(TASK_TYPE)
            .where(TASK_TYPE.TASK_TYP.eq(WERK))
            .fetchOne(TASK_TYPE.STRUKTUR)
            .data();
    assertThat(kopie.get("struktur")).isEqualTo(Testdefinitionen.JSON.readTree(struktur));

    // Die Eintraege: nur Felder mit Wert, in der Reihenfolge der Definition.
    assertThat(Testdefinitionen.JSON.readTree(zeile.getEintraege().data()))
        .isEqualTo(
            Testdefinitionen.JSON.readTree(
                """
                [ {"werk": "72332", "abladestelle": "DDB0", "sndprn": "120764"},
                  {"lieferantennummer": "0101159903", "werk": "22", "abladestelle": "001N0",
                   "sndprn4908": "100050"} ]"""));

    // Genau ein Ereignis, ohne Parametertext.
    List<String> ereignisse =
        monitorDsl
            .select(AUDIT_LOG.DETAIL)
            .from(AUDIT_LOG)
            .where(AUDIT_LOG.EVENT_TYPE.eq(AuditEventType.AUFTRAG_ANGELEGT.name()))
            .and(AUDIT_LOG.TARGET_ID.eq(taskId))
            .fetch(AUDIT_LOG.DETAIL);
    assertThat(ereignisse).containsExactly("Typ WERK_ABLADESTELLE, Aktion Neuanlage, 2 Einträge");
  }

  @Test
  @DisplayName("Gleiche anfrageId noch einmal: 200 mit dem vorhandenen Auftrag, kein zweiter")
  void wiederholung() throws Exception {
    String anfrageId = neueAnfrageId();
    Antwort erste = sitzung.sende("/api/tasks", neuanlage(anfrageId));
    assertThat(erste.status()).as(erste.rumpf()).isEqualTo(201);

    // Derselbe Klick, diesmal mit anderem Rumpf und gross geschriebener Kennung: Es zaehlt die
    // Kennung, nicht der Inhalt.
    Antwort zweite =
        sitzung.sende(
            "/api/tasks",
            auftrag(
                anfrageId.toUpperCase(),
                WERK,
                "Löschung",
                List.of(Map.of("werk", "1", "abladestelle", "A"))));

    assertThat(zweite.status()).as(zweite.rumpf()).isEqualTo(200);
    assertThat(zweite.<String>json("$.taskId")).isEqualTo(erste.<String>json("$.taskId"));
    assertThat(zweite.<String>json("$.aktion")).isEqualTo("Neuanlage");
    assertThat(monitorDsl.fetchCount(TASK, TASK.ANFRAGE_ID.eq(anfrageId))).isEqualTo(1);
    assertThat(
            monitorDsl.fetchCount(
                AUDIT_LOG,
                AUDIT_LOG
                    .EVENT_TYPE
                    .eq(AuditEventType.AUFTRAG_ANGELEGT.name())
                    .and(AUDIT_LOG.TARGET_ID.eq(erste.<String>json("$.taskId")))))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("Pruefehler: 400 im vorhandenen Fehlerformat, mit Index, Feld, Code und Klartext")
  void pruefehler() throws Exception {
    Antwort antwort =
        sitzung.sende(
            "/api/tasks",
            auftrag(
                neueAnfrageId(),
                WERK,
                "Löschung",
                List.of(Map.of("werk", "1", "abladestelle", "A", "sndprn", "12076"))));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/eingabe-ungueltig");
    assertThat(antwort.<Integer>json("$.errors[0].index")).isZero();
    assertThat(antwort.<String>json("$.errors[0].feld")).isEqualTo("sndprn");
    assertThat(antwort.<String>json("$.errors[0].code")).isEqualTo("FELD_NICHT_ERLAUBT");
    assertThat(antwort.<String>json("$.errors[0].meldung"))
        .isEqualTo("Eintrag 1, SNDPRN: Bei „Löschung“ wird hier kein Wert angegeben.");
    assertThat(antwort.hatFeld("$.traceId")).isTrue();
    assertThat(monitorDsl.fetchCount(TASK, TASK.ANGELEGT_VON.eq(NUTZER))).isZero();
  }

  @Test
  @DisplayName("anfrageId fehlt oder ist keine UUID: 400 mit feld anfrageId")
  void anfrage_id_ungueltig() throws Exception {
    Antwort ohne =
        sitzung.sende("/api/tasks", auftrag(null, WERK, "Löschung", List.of(Map.of("werk", "1"))));
    assertThat(ohne.status()).isEqualTo(400);
    assertThat(ohne.<String>json("$.errors[0].feld")).isEqualTo("anfrageId");
    assertThat(ohne.<String>json("$.errors[0].code")).isEqualTo("PFLICHT_FEHLT");

    Antwort kaputt =
        sitzung.sende(
            "/api/tasks", auftrag("keine-uuid", WERK, "Löschung", List.of(Map.of("werk", "1"))));
    assertThat(kaputt.<String>json("$.errors[0].code")).isEqualTo("FORMAT");
  }

  // ─── Die simulierte Plattform ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Plattform: lesen, abholen, Ok fuer alle — ERLEDIGT; ein zweites Abholen greift nicht")
  void plattform_erledigt() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);

    Record offen =
        monitorDsl.fetch(Plattformvertrag.OFFENE_LESEN).stream()
            .filter(zeile -> taskId.equals(zeile.get("task_id", String.class)))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Der Auftrag fehlt in der offenen Liste"));
    assertThat(offen.get("mandant_id", String.class)).isEqualTo(NEXANS);
    assertThat(offen.get("task_typ", String.class)).isEqualTo(WERK);
    assertThat(offen.get("aktion", String.class)).isEqualTo("Neuanlage");
    assertThat(offen.get("parameter", String.class)).startsWith("Aktion=Neuanlage\n");

    assertThat(monitorDsl.execute(Plattformvertrag.ABHOLEN, taskId)).isEqualTo(1);
    assertThat(monitorDsl.execute(Plattformvertrag.ABHOLEN, taskId))
        .as("doppeltes Abholen: null heisst, hat schon jemand")
        .isZero();
    assertThat(
            monitorDsl.fetch(Plattformvertrag.OFFENE_LESEN).stream()
                .map(zeile -> zeile.get("task_id", String.class)))
        .doesNotContain(taskId);

    Antwort abgeholt = sitzung.hole("/api/tasks/" + taskId);
    assertThat(abgeholt.<String>json("$.einordnung")).isEqualTo("IN_AUSFUEHRUNG");
    assertThat(abgeholt.hatFeld("$.abgeholtAm")).isTrue();

    assertThat(monitorDsl.execute(Plattformvertrag.ERLEDIGT_MELDEN, "Ok\nOk", taskId)).isEqualTo(1);

    Antwort erledigt = sitzung.hole("/api/tasks/" + taskId);
    assertThat(erledigt.<String>json("$.status")).isEqualTo("ERLEDIGT");
    assertThat(erledigt.<String>json("$.einordnung")).isEqualTo("ERLEDIGT");
    assertThat(erledigt.<List<String>>json("$.eintraege[*].ergebnis")).containsExactly("OK", "OK");
    assertThat(erledigt.<List<String>>json("$.eintraege[*].rueckgabe")).containsExactly("Ok", "Ok");
    Instant eingestellt = Instant.parse(erledigt.json("$.eingestelltAm"));
    Instant abgeholtAm = Instant.parse(erledigt.json("$.abgeholtAm"));
    Instant ausgefuehrt = Instant.parse(erledigt.json("$.ausgefuehrtAm"));
    assertThat(eingestellt).isBeforeOrEqualTo(abgeholtAm);
    assertThat(abgeholtAm).isBeforeOrEqualTo(ausgefuehrt);

    // Zurueckmelden geht nur aus IN_AUSFUEHRUNG.
    assertThat(monitorDsl.execute(Plattformvertrag.ERLEDIGT_MELDEN, "Ok\nOk", taskId)).isZero();
  }

  @Test
  @DisplayName("Plattform: ein Eintrag nicht Ok, Rueckgabe mit CRLF — TEILWEISE_ERLEDIGT")
  void plattform_teilweise() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    monitorDsl.execute(Plattformvertrag.ABHOLEN, taskId);
    monitorDsl.execute(
        Plattformvertrag.ERLEDIGT_MELDEN, "Ok\r\nSchlüssel bereits vorhanden\r\n", taskId);

    Antwort antwort = sitzung.hole("/api/tasks/" + taskId);
    assertThat(antwort.<String>json("$.status")).isEqualTo("ERLEDIGT");
    assertThat(antwort.<String>json("$.einordnung")).isEqualTo("TEILWEISE_ERLEDIGT");
    assertThat(antwort.<List<String>>json("$.eintraege[*].ergebnis"))
        .containsExactly("OK", "NICHT_OK");
    assertThat(antwort.<List<String>>json("$.eintraege[*].rueckgabe"))
        .containsExactly("Ok", "Schlüssel bereits vorhanden");
  }

  @Test
  @DisplayName("Plattform: Rueckgabe mit falscher Zeilenzahl — UNSTIMMIG")
  void plattform_unstimmig() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    monitorDsl.execute(Plattformvertrag.ABHOLEN, taskId);
    monitorDsl.execute(Plattformvertrag.ERLEDIGT_MELDEN, "Ok", taskId);

    Antwort antwort = sitzung.hole("/api/tasks/" + taskId);
    assertThat(antwort.<String>json("$.einordnung")).isEqualTo("UNSTIMMIG");
    assertThat(antwort.<List<String>>json("$.eintraege[*].rueckgabe")).containsExactly("Ok", null);
  }

  @Test
  @DisplayName("Plattform: nicht ausfuehrbar — FEHLGESCHLAGEN mit Code und Klartext")
  void plattform_fehlgeschlagen() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    assertThat(monitorDsl.execute(Plattformvertrag.ABHOLEN, taskId)).isEqualTo(1);
    assertThat(
            monitorDsl.execute(
                Plattformvertrag.FEHLGESCHLAGEN_MELDEN,
                "TABELLE_GESPERRT",
                "Die Tabelle konnte nicht gespeichert werden.",
                taskId))
        .isEqualTo(1);

    Antwort antwort = sitzung.hole("/api/tasks/" + taskId);
    assertThat(antwort.<String>json("$.einordnung")).isEqualTo("FEHLGESCHLAGEN");
    assertThat(antwort.<String>json("$.fehlercode")).isEqualTo("TABELLE_GESPERRT");
    assertThat(antwort.<String>json("$.fehlertext"))
        .isEqualTo("Die Tabelle konnte nicht gespeichert werden.");
    assertThat(antwort.hatFeld("$.ausgefuehrtAm")).isTrue();
    assertThat(antwort.<List<String>>json("$.eintraege[*].ergebnis")).containsOnlyNulls();
  }

  @Test
  @DisplayName("Liste: der eigene Auftrag steht darin, neueste zuerst")
  void liste() throws Exception {
    String erster = legeNeuanlageAn(sitzung);
    String zweiter = legeNeuanlageAn(sitzung);

    List<String> kennungen = sitzung.hole("/api/tasks").json("$[*].taskId");

    assertThat(kennungen).containsSubsequence(zweiter, erster);
  }

  // ─── Felder mit label (A4, E‑1120) ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("Die Antwort traegt je Feld der Kopie Name und label, in deren Reihenfolge")
  void felder_aus_der_kopie() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);

    // Verglichen wird mit der Kopie in der eigenen Zeile, nicht mit einer Liste im Test: Welche
    // Felder WERK_ABLADESTELLE hat, ist Pflegestand (Regel T2).
    JsonNode felder =
        Testdefinitionen.JSON
            .readTree(
                monitorDsl
                    .select(TASK.DEFINITION)
                    .from(TASK)
                    .where(TASK.TASK_ID.eq(taskId))
                    .fetchOne(TASK.DEFINITION)
                    .data())
            .get("struktur")
            .get("felder");
    List<String> namen = felder.valueStream().map(feld -> feld.get("name").stringValue()).toList();
    List<String> label = felder.valueStream().map(feld -> feld.get("label").stringValue()).toList();
    assertThat(namen).isNotEmpty();

    Antwort einzeln = sitzung.hole("/api/tasks/" + taskId);
    assertThat(einzeln.<List<String>>json("$.felder[*].name")).containsExactlyElementsOf(namen);
    assertThat(einzeln.<List<String>>json("$.felder[*].label")).containsExactlyElementsOf(label);

    Antwort liste = sitzung.hole("/api/tasks");
    String auswahl = "$[?(@.taskId == '" + taskId + "')]";
    assertThat(liste.<List<String>>json(auswahl + ".felder[*].name"))
        .containsExactlyElementsOf(namen);
    assertThat(liste.<List<String>>json(auswahl + ".felder[*].label"))
        .containsExactlyElementsOf(label);
  }

  // ─── Loeschen (A4, docs/tasks.md §6.6) ─────────────────────────────────────────────────────

  @Test
  @DisplayName("Loeschen eines offenen Auftrags: 204, Zeile weg, genau ein Ereignis ohne Parameter")
  void loeschen_offen() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);

    Antwort antwort = sitzung.loesche("/api/tasks/" + taskId);

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(204);
    assertThat(antwort.rumpf()).isEmpty();
    assertThat(monitorDsl.fetchCount(TASK, TASK.TASK_ID.eq(taskId))).isZero();
    assertThat(sitzung.hole("/api/tasks/" + taskId).status()).isEqualTo(404);

    var ereignisse =
        monitorDsl
            .select(
                AUDIT_LOG.ACTOR_USERNAME,
                AUDIT_LOG.MANDANT_ID,
                AUDIT_LOG.TARGET_TYPE,
                AUDIT_LOG.DETAIL)
            .from(AUDIT_LOG)
            .where(AUDIT_LOG.EVENT_TYPE.eq(AuditEventType.AUFTRAG_GELOESCHT.name()))
            .and(AUDIT_LOG.TARGET_ID.eq(taskId))
            .fetch();
    assertThat(ereignisse).hasSize(1);
    var ereignis = ereignisse.getFirst();
    assertThat(ereignis.get(AUDIT_LOG.ACTOR_USERNAME)).isEqualTo(NUTZER);
    assertThat(ereignis.get(AUDIT_LOG.MANDANT_ID)).isEqualTo(NEXANS);
    assertThat(ereignis.get(AUDIT_LOG.TARGET_TYPE)).isEqualTo("task");
    String detail = ereignis.get(AUDIT_LOG.DETAIL);
    assertThat(detail)
        .startsWith("Typ WERK_ABLADESTELLE, Aktion Neuanlage, 2 Einträge, eingestellt ");
    // Der Zeitpunkt des Einstellens steht als UTC darin — gelesen, nicht gegen die Uhr geprueft
    // (Regel T1).
    assertThat(Instant.parse(detail.substring(detail.lastIndexOf(' ') + 1))).isNotNull();
    assertThat(detail).doesNotContain("Aktion=").doesNotContain("72332").doesNotContain("120764");
  }

  /**
   * <b>Loeschen und Ereignis stehen oder fallen gemeinsam</b> (E‑1114, F26). Der Service laeuft
   * hier in einer aeusseren Transaktion, die zuruecksetzt: Danach steht die Zeile wieder, und es
   * gibt kein Ereignis. Schriebe der Service das Ereignis in einer eigenen Transaktion ({@code
   * schreibe} statt {@code schreibeInTransaktion}), bliebe es stehen — fuer ein Loeschen, das nie
   * geschehen ist. Ueber HTTP ist das nicht zu pruefen: Dort committet das Loeschen immer.
   */
  @Test
  @DisplayName("Loeschen und Ereignis in einer Transaktion: rollt sie zurueck, bleibt beides aus")
  void loeschen_und_ereignis_fallen_gemeinsam() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    AngemeldeterNutzer nutzer = new AngemeldeterNutzer(nutzerId, NUTZER, Rolle.MANDANT, false);

    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              auftragService.loesche(nutzer, new MandantContext(NEXANS), taskId, "127.0.0.1");
              // Innerhalb ist die Zeile weg und das Ereignis da — sonst bewiese der Test nur,
              // dass nichts geschehen ist.
              assertThat(monitorDsl.fetchCount(TASK, TASK.TASK_ID.eq(taskId))).isZero();
              assertThat(geloeschtEreignisse(taskId)).isEqualTo(1);
              status.setRollbackOnly();
            });

    assertThat(monitorDsl.fetchCount(TASK, TASK.TASK_ID.eq(taskId))).isEqualTo(1);
    assertThat(geloeschtEreignisse(taskId)).isZero();
  }

  @Test
  @DisplayName("Loeschen darf jeder Benutzer des Mandanten, nicht nur der, der angelegt hat")
  void loeschen_durch_anderen_benutzer() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    String kollege = PRAEFIX + "auftrag-nexans-kollege";
    legeNutzerAn(kollege, PASSWORT, Rolle.MANDANT, NEXANS);

    Antwort antwort = anmelden(kollege, PASSWORT).loesche("/api/tasks/" + taskId);

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(204);
    assertThat(monitorDsl.fetchCount(TASK, TASK.TASK_ID.eq(taskId))).isZero();
    assertThat(
            monitorDsl
                .select(AUDIT_LOG.ACTOR_USERNAME)
                .from(AUDIT_LOG)
                .where(AUDIT_LOG.EVENT_TYPE.eq(AuditEventType.AUFTRAG_GELOESCHT.name()))
                .and(AUDIT_LOG.TARGET_ID.eq(taskId))
                .fetch(AUDIT_LOG.ACTOR_USERNAME))
        .containsExactly(kollege);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"IN_AUSFUEHRUNG", "ERLEDIGT", "FEHLGESCHLAGEN"})
  @DisplayName("Nicht mehr offen: 409 auftrag-nicht-offen, Zeile unveraendert, kein Ereignis")
  void loeschen_nicht_offen(String stand) throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    bringeInStand(taskId, stand);
    TaskRecord vorher = monitorDsl.selectFrom(TASK).where(TASK.TASK_ID.eq(taskId)).fetchOne();
    assertThat(vorher.getStatus()).isEqualTo(stand);

    Antwort antwort = sitzung.loesche("/api/tasks/" + taskId);

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(409);
    assertThat(antwort.<String>json("$.type")).endsWith("/auftrag-nicht-offen");
    assertThat(antwort.hatFeld("$.traceId")).isTrue();
    assertThat(antwort.rumpf()).doesNotContain(stand);
    assertThat(monitorDsl.selectFrom(TASK).where(TASK.TASK_ID.eq(taskId)).fetchOne())
        .isEqualTo(vorher);
    assertThat(geloeschtEreignisse(taskId)).isZero();
  }

  @Test
  @DisplayName(
      "Abgeholt zwischen Anzeige und Klick: die Liste zeigt OFFEN, die Plattform holt, 409")
  void abgeholt_zwischen_anzeige_und_klick() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    assertThat(
            sitzung
                .hole("/api/tasks")
                .<List<String>>json("$[?(@.taskId == '" + taskId + "')].einordnung"))
        .containsExactly("OFFEN");

    assertThat(monitorDsl.execute(Plattformvertrag.ABHOLEN, taskId)).isEqualTo(1);
    Antwort antwort = sitzung.loesche("/api/tasks/" + taskId);

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(409);
    assertThat(antwort.<String>json("$.type")).endsWith("/auftrag-nicht-offen");
    assertThat(sitzung.hole("/api/tasks/" + taskId).<String>json("$.einordnung"))
        .isEqualTo("IN_AUSFUEHRUNG");
    assertThat(geloeschtEreignisse(taskId)).isZero();
  }

  @Test
  @DisplayName("Ein zweites DELETE auf denselben Auftrag ist 404, und es bleibt ein Ereignis")
  void zweimal_loeschen() throws Exception {
    String taskId = legeNeuanlageAn(sitzung);
    assertThat(sitzung.loesche("/api/tasks/" + taskId).status()).isEqualTo(204);

    Antwort zweites = sitzung.loesche("/api/tasks/" + taskId);

    assertThat(zweites.status()).isEqualTo(404);
    assertThat(geloeschtEreignisse(taskId)).isEqualTo(1);
  }

  @Test
  @DisplayName("Unbekannte Kennung: 404, kein Ereignis")
  void loeschen_unbekannt() throws Exception {
    String erfunden = UUID.randomUUID().toString();

    Antwort antwort = sitzung.loesche("/api/tasks/" + erfunden);

    assertThat(antwort.status()).isEqualTo(404);
    assertThat(geloeschtEreignisse(erfunden)).isZero();
  }

  /** Bringt einen eben angelegten Auftrag mit den Statements der Plattform in einen Stand. */
  private void bringeInStand(String taskId, String stand) {
    assertThat(monitorDsl.execute(Plattformvertrag.ABHOLEN, taskId)).isEqualTo(1);
    switch (stand) {
      case "IN_AUSFUEHRUNG" -> {}
      case "ERLEDIGT" ->
          assertThat(monitorDsl.execute(Plattformvertrag.ERLEDIGT_MELDEN, "Ok\nOk", taskId))
              .isEqualTo(1);
      case "FEHLGESCHLAGEN" ->
          assertThat(
                  monitorDsl.execute(
                      Plattformvertrag.FEHLGESCHLAGEN_MELDEN, "TEST", "Im Test.", taskId))
              .isEqualTo(1);
      default -> throw new IllegalArgumentException(stand);
    }
  }

  private int geloeschtEreignisse(String taskId) {
    return monitorDsl.fetchCount(
        AUDIT_LOG,
        AUDIT_LOG
            .EVENT_TYPE
            .eq(AuditEventType.AUFTRAG_GELOESCHT.name())
            .and(AUDIT_LOG.TARGET_ID.eq(taskId)));
  }
}
