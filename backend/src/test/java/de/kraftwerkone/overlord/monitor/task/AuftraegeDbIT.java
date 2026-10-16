package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.AUDIT_LOG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.jooq.monitor.tables.records.TaskRecord;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Anlegen, Wiederholen und die simulierte Plattform ({@code docs/tasks.md} §6, {@code
 * docs/task-schnittstelle.md}).
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

  private Sitzung sitzung;

  @BeforeEach
  void anmelden() throws Exception {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, NEXANS);
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
}
