package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.AUDIT_LOG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE_MANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.jooq.monitor.tables.records.TaskRecord;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.Zugriffszaehler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * <b>Der Pflicht-Isolationstest der Endpunkte unter {@code /api/tasks}</b> (Regel M4), nach der
 * Vorlage in {@code docs/mandantentrennung.md} §5 — die vier aus A1 und seit A4 {@code DELETE
 * /api/tasks/{taskId}}.
 *
 * <p><b>Paarung {@code NEXANS} gegen {@code IBIS}</b>: zwei Haeuser, beide fuer {@code
 * WERK_ABLADESTELLE} freigeschaltet — ein Leck zwischen ihnen waere also nicht schon durch die
 * Freischaltung verdeckt. <b>{@code SUTTONS}</b> ist der Gegentest ohne Freischaltung (Regel L7:
 * neben dem grossen Mandanten immer ein kleiner); fuer ihn ist der Assistent nicht verfuegbar.
 *
 * <p><b>Die Daten legt der Test selbst an</b> (Regel T2): Der Auftrag, dessen Unerreichbarkeit
 * geprueft wird, entsteht hier unter {@code NEXANS}, die beiden Freischaltungen seit {@code V103}
 * in {@link AuftragsTestbasis}. Vom Bestand haengt nur ab, dass {@code SUTTONS} keine hat; das wird
 * zu Beginn geprueft — schlaegt es fehl, hat sich die Testkopie geaendert, nicht der Code.
 *
 * <p><b>Ununterscheidbar heisst: dieselben Statements.</b> Fremd und erfunden werden ueber den
 * Rumpf <b>und</b> ueber die Folge der Statements auf dem Schreib-Kontext verglichen ({@link
 * SchreibkontextZaehlung}); eine nachgelagerte Existenzpruefung fiele dort sofort auf.
 */
@Import(SchreibkontextZaehlung.class)
class AuftraegeIsolationDbIT extends AuftragsTestbasis {

  private static final String NUTZER_NEXANS = PRAEFIX + "isolation-nexans";
  private static final String NUTZER_IBIS = PRAEFIX + "isolation-ibis";
  private static final String NUTZER_SUTTONS = PRAEFIX + "isolation-suttons";
  private static final String NUTZER_ADMIN = PRAEFIX + "isolation-admin";

  @Autowired private Zugriffszaehler zugriffe;

  private Sitzung aufNexans;
  private Sitzung aufIbis;
  private Sitzung aufSuttons;

  @BeforeEach
  void vorbedingungenUndAnmeldung() throws Exception {
    assertThat(mandantRepository.existiert(NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(IBIS)).isTrue();
    assertThat(mandantRepository.existiert(SUTTONS)).isTrue();
    assertThat(freigeschaltet(SUTTONS)).as("kein aktiver Typ fuer SUTTONS").isEmpty();

    legeNutzerAn(NUTZER_NEXANS, PASSWORT, Rolle.MANDANT, NEXANS);
    legeNutzerAn(NUTZER_IBIS, PASSWORT, Rolle.MANDANT, IBIS);
    legeNutzerAn(NUTZER_SUTTONS, PASSWORT, Rolle.MANDANT, SUTTONS);
    aufNexans = anmelden(NUTZER_NEXANS, PASSWORT);
    aufIbis = anmelden(NUTZER_IBIS, PASSWORT);
    aufSuttons = anmelden(NUTZER_SUTTONS, PASSWORT);
  }

  // ─── GET /api/tasks/typen ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Typliste: NEXANS sieht WERK_ABLADESTELLE, SUTTONS bekommt 200 mit leerer Liste")
  void typliste() throws Exception {
    assertThat(aufNexans.hole("/api/tasks/typen").<List<String>>json("$[*].typ")).contains(WERK);

    Antwort suttons = aufSuttons.hole("/api/tasks/typen");
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(suttons.rumpf()).isEqualTo("[]");
  }

  // ─── POST /api/tasks ───────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Anlegen ohne Freischaltung: 404, ununterscheidbar von einem erfundenen Typ")
  void anlegen_ohne_freischaltung() throws Exception {
    zugriffe.zuruecksetzen();
    Antwort nichtFreigeschaltet = aufSuttons.sende("/api/tasks", neuanlage(neueAnfrageId()));
    List<String> statementsFreigeschaltet = zugriffe.abgesetzt();

    zugriffe.zuruecksetzen();
    Antwort erfunden =
        aufSuttons.sende(
            "/api/tasks",
            auftrag(
                neueAnfrageId(),
                "WERK_ABLADESTELLX",
                "Neuanlage",
                List.of(Map.of("werk", "72332", "abladestelle", "DDB0", "sndprn", "120764"))));
    List<String> statementsErfunden = zugriffe.abgesetzt();

    assertThat(nichtFreigeschaltet.status()).isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(nichtFreigeschaltet.rumpfOhneTraceId()).isEqualTo(erfunden.rumpfOhneTraceId());
    assertThat(statementsFreigeschaltet).isNotEmpty().isEqualTo(statementsErfunden);
    assertThat(monitorDsl.fetchCount(TASK, TASK.ANGELEGT_VON.eq(NUTZER_SUTTONS))).isZero();
  }

  @Test
  @DisplayName("Ein untergeschobenes mandantId im Rumpf wird ignoriert (Regel M1)")
  void mandant_id_im_rumpf_wird_ignoriert() throws Exception {
    String rumpf =
        neuanlage(neueAnfrageId()).replaceFirst("\\{", "{\"mandantId\":\"" + IBIS + "\",");

    Antwort antwort = aufNexans.sende("/api/tasks", rumpf);

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(201);
    String taskId = antwort.json("$.taskId");
    assertThat(
            monitorDsl
                .select(TASK.MANDANT_ID)
                .from(TASK)
                .where(TASK.TASK_ID.eq(taskId))
                .fetchOne(TASK.MANDANT_ID))
        .isEqualTo(NEXANS);
    assertThat(aufIbis.hole("/api/tasks").rumpf()).doesNotContain(taskId);
  }

  @Test
  @DisplayName(
      "Eine fremde anfrageId liefert nicht den fremden Auftrag, sondern legt einen eigenen an")
  void fremde_anfrage_id() throws Exception {
    String anfrageId = neueAnfrageId();
    String fremd = aufNexans.sende("/api/tasks", neuanlage(anfrageId)).json("$.taskId");

    Antwort eigen = aufIbis.sende("/api/tasks", neuanlage(anfrageId));

    assertThat(eigen.status()).as(eigen.rumpf()).isEqualTo(201);
    assertThat(eigen.<String>json("$.taskId")).isNotEqualTo(fremd);
    assertThat(eigen.rumpf()).doesNotContain(fremd);
  }

  // ─── GET /api/tasks und GET /api/tasks/{taskId} ────────────────────────────────────────────

  @Test
  @DisplayName("Ein Auftrag unter NEXANS ist fuer IBIS weder in der Liste noch einzeln zu sehen")
  void fremder_auftrag_unerreichbar() throws Exception {
    String taskId = legeNeuanlageAn(aufNexans);

    // Gegenprobe: Der Eigentuemer sieht ihn — sonst bewiese der Test nur, dass leer leer ist.
    assertThat(aufNexans.hole("/api/tasks").<List<String>>json("$[*].taskId")).contains(taskId);
    assertThat(aufNexans.hole("/api/tasks/" + taskId).status()).isEqualTo(200);

    Antwort liste = aufIbis.hole("/api/tasks");
    assertThat(liste.status()).isEqualTo(200);
    assertThat(liste.rumpf()).doesNotContain(taskId);
    assertThat(aufSuttons.hole("/api/tasks").rumpf()).doesNotContain(taskId);

    String erfundeneId = UUID.randomUUID().toString();
    zugriffe.zuruecksetzen();
    Antwort fremd = aufIbis.hole("/api/tasks/" + taskId);
    List<String> statementsFremd = zugriffe.abgesetzt();
    zugriffe.zuruecksetzen();
    Antwort erfunden = aufIbis.hole("/api/tasks/" + erfundeneId);
    List<String> statementsErfunden = zugriffe.abgesetzt();

    assertThat(fremd.status()).isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    // instance spiegelt den angefragten Pfad und ist damit das Zitat der Frage, keine Auskunft —
    // zeichenweise geprueft, dann fuer den Vergleich gleichgesetzt.
    assertThat(fremd.<String>json("$.instance")).isEqualTo("/api/tasks/" + taskId);
    assertThat(erfunden.<String>json("$.instance")).isEqualTo("/api/tasks/" + erfundeneId);
    assertThat(fremd.rumpfOhneTraceId().replace(taskId, "<id>"))
        .isEqualTo(erfunden.rumpfOhneTraceId().replace(erfundeneId, "<id>"));
    assertThat(statementsFremd).hasSize(1).isEqualTo(statementsErfunden);
  }

  @Test
  @DisplayName("ADMIN sieht nur den jeweils aktiven Mandanten")
  void admin_sieht_nur_den_aktiven_mandanten() throws Exception {
    String taskId = legeNeuanlageAn(aufNexans);
    legeNutzerAn(NUTZER_ADMIN, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(NUTZER_ADMIN, PASSWORT);

    // Ohne aktiven Mandanten gibt es nichts — nicht etwa alles.
    assertThat(alsAdmin.hole("/api/tasks").status()).isEqualTo(403);
    assertThat(alsAdmin.hole("/api/tasks/typen").status()).isEqualTo(403);
    assertThat(alsAdmin.loesche("/api/tasks/" + taskId).status()).isEqualTo(403);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + IBIS + "\"}");
    assertThat(alsAdmin.hole("/api/tasks").rumpf()).doesNotContain(taskId);
    assertThat(alsAdmin.hole("/api/tasks/" + taskId).status()).isEqualTo(404);
    assertThat(alsAdmin.loesche("/api/tasks/" + taskId).status()).isEqualTo(404);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + SUTTONS + "\"}");
    assertThat(alsAdmin.hole("/api/tasks/typen").rumpf()).isEqualTo("[]");
    assertThat(alsAdmin.sende("/api/tasks", neuanlage(neueAnfrageId())).status()).isEqualTo(404);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + NEXANS + "\"}");
    assertThat(alsAdmin.hole("/api/tasks").<List<String>>json("$[*].taskId")).contains(taskId);
    assertThat(alsAdmin.hole("/api/tasks/" + taskId).status()).isEqualTo(200);
    // Geloescht wird nur im aktiven Mandanten, dort aber auch von ADMIN (E‑1110, E‑1113).
    assertThat(alsAdmin.loesche("/api/tasks/" + taskId).status()).isEqualTo(204);
  }

  @Test
  @DisplayName("Der Aenderungszwang beim Passwort sperrt die Endpunkte wie alle uebrigen")
  void aenderungszwang_sperrt() throws Exception {
    String nutzer = PRAEFIX + "isolation-zwang";
    legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, true, NEXANS);
    Sitzung sitzung = anmelden(nutzer, PASSWORT);

    assertThat(sitzung.hole("/api/tasks").status()).isEqualTo(403);
    assertThat(sitzung.hole("/api/tasks/typen").status()).isEqualTo(403);
    assertThat(sitzung.sende("/api/tasks", neuanlage(neueAnfrageId())).status()).isEqualTo(403);

    // Seit A4 auch das Loeschen — an einem echten offenen Auftrag desselben Mandanten, damit
    // die Sperre und nicht eine unbekannte Kennung die Ablehnung ist.
    String taskId = legeNeuanlageAn(aufNexans);
    assertThat(sitzung.loesche("/api/tasks/" + taskId).status()).isEqualTo(403);
    assertThat(monitorDsl.fetchCount(TASK, TASK.TASK_ID.eq(taskId))).isEqualTo(1);
  }

  // ─── DELETE /api/tasks/{taskId} (A4) ──────────────────────────────────────────────────────

  @Test
  @DisplayName("IBIS loescht einen offenen Auftrag von NEXANS: 404 wie erfunden, die Zeile bleibt")
  void fremden_auftrag_loeschen() throws Exception {
    String taskId = legeNeuanlageAn(aufNexans);
    TaskRecord vorher = monitorDsl.selectFrom(TASK).where(TASK.TASK_ID.eq(taskId)).fetchOne();

    String erfundeneId = UUID.randomUUID().toString();
    zugriffe.zuruecksetzen();
    Antwort fremd = aufIbis.loesche("/api/tasks/" + taskId);
    List<String> statementsFremd = zugriffe.abgesetzt();
    zugriffe.zuruecksetzen();
    Antwort erfunden = aufIbis.loesche("/api/tasks/" + erfundeneId);
    List<String> statementsErfunden = zugriffe.abgesetzt();

    assertThat(fremd.status()).isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(fremd.<String>json("$.instance")).isEqualTo("/api/tasks/" + taskId);
    assertThat(fremd.rumpfOhneTraceId().replace(taskId, "<id>"))
        .isEqualTo(erfunden.rumpfOhneTraceId().replace(erfundeneId, "<id>"));
    // Das Loeschen ohne Treffer und die Nachlesung im eigenen Mandanten, in beiden Faellen gleich.
    // Ein 409 fuer den fremden Auftrag verriete, dass es ihn gibt.
    assertThat(statementsFremd).hasSize(2).isEqualTo(statementsErfunden);

    // Ein untergeschobener Mandant in der Anfrage aendert nichts (Regel M1).
    assertThat(aufIbis.loesche("/api/tasks/" + taskId + "?mandantId=" + NEXANS).status())
        .isEqualTo(404);
    assertThat(aufSuttons.loesche("/api/tasks/" + taskId).status()).isEqualTo(404);

    assertThat(monitorDsl.selectFrom(TASK).where(TASK.TASK_ID.eq(taskId)).fetchOne())
        .isEqualTo(vorher);
    assertThat(
            monitorDsl.fetchCount(
                AUDIT_LOG,
                AUDIT_LOG
                    .EVENT_TYPE
                    .eq(AuditEventType.AUFTRAG_GELOESCHT.name())
                    .and(AUDIT_LOG.TARGET_ID.eq(taskId))))
        .isZero();

    // Gegenprobe: Der Eigentuemer loescht ihn — sonst bewiese der Test nur, dass Loeschen nie geht.
    assertThat(aufNexans.loesche("/api/tasks/" + taskId).status()).isEqualTo(204);
  }

  /** Die aktiven, fuer den Mandanten freigeschalteten Typen — direkt aus der Datenbank. */
  private List<String> freigeschaltet(String mandant) {
    return monitorDsl
        .select(TASK_TYPE.TASK_TYP)
        .from(TASK_TYPE_MANDANT)
        .join(TASK_TYPE)
        .on(TASK_TYPE.TASK_TYP.eq(TASK_TYPE_MANDANT.TASK_TYP))
        .where(TASK_TYPE_MANDANT.MANDANT_ID.eq(mandant))
        .and(TASK_TYPE.AKTIV.isTrue())
        .and(TASK_TYPE.TASK_TYP.notLike(PRAEFIX + "%"))
        .fetch(TASK_TYPE.TASK_TYP);
  }
}
