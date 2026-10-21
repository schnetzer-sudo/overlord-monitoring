package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE_MANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.jooq.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/**
 * <b>Eine Definition per SQL, ohne Neustart</b> (E‑1019, {@code docs/tasks.md} §3.6).
 *
 * <p>Der Test legt einen eigenen Typ an — die Gattung aus {@link Testdefinitionen}, unter einer
 * Kennung mit dem Praefix {@code it-}, per reinem {@code INSERT} —, schaltet ihn fuer {@code
 * NEXANS} frei und fuehrt ihn durch: inaktiv, aktiviert, geaendert, ungueltig. Ein Auftrag aus der
 * Zeit davor behaelt seine Einordnung, weil er seine Kopie liest (E‑1023). Der Typ verschwindet
 * danach restlos ({@link AuftragsTestbasis}); die Zeile {@code WERK_ABLADESTELLE} fasst der Test
 * nicht an, und er prueft das.
 */
class TypdefinitionDbIT extends AuftragsTestbasis {

  private static final String TESTTYP = PRAEFIX + "gattung";
  private static final String NUTZER = PRAEFIX + "typdefinition";

  private Sitzung sitzung;

  @BeforeEach
  void vorbereiten() throws Exception {
    assertThat(monitorDsl.fetchCount(TASK_TYPE, TASK_TYPE.TASK_TYP.eq(TESTTYP)))
        .as("Ein Lauf davor hat aufgeraeumt")
        .isZero();
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, NEXANS);
    sitzung = anmelden(NUTZER, PASSWORT);
  }

  @Test
  @DisplayName(
      "inaktiv 404, aktiviert angeboten, Aenderung wirkt, ungueltig weg, alter Auftrag liest seine Kopie")
  void definition_per_sql_ohne_neustart() throws Exception {
    LocalDateTime werkVorher = werkGeaendertAm();

    // 1. Anlegen, inaktiv (Vorgabe), und freischalten.
    monitorDsl
        .insertInto(TASK_TYPE)
        .set(TASK_TYPE.TASK_TYP, TESTTYP)
        .set(TASK_TYPE.NAME, "Gattung aus dem Test")
        .set(TASK_TYPE.BESCHREIBUNG, "Besteht nur im Test.")
        .set(TASK_TYPE.STRUKTUR, JSON.valueOf(Testdefinitionen.GATTUNG_STRUKTUR))
        .execute();
    monitorDsl
        .insertInto(TASK_TYPE_MANDANT)
        .set(TASK_TYPE_MANDANT.TASK_TYP, TESTTYP)
        .set(TASK_TYPE_MANDANT.MANDANT_ID, NEXANS)
        .execute();

    assertThat(typen()).doesNotContain(TESTTYP);
    assertThat(sitzung.sende("/api/tasks", sperren(2)).status()).as("inaktiv").isEqualTo(404);

    // 2. Aktivieren: beim naechsten Zugriff im Angebot, ohne Neustart.
    monitorDsl
        .update(TASK_TYPE)
        .set(TASK_TYPE.AKTIV, true)
        .where(TASK_TYPE.TASK_TYP.eq(TESTTYP))
        .execute();

    Antwort typliste = sitzung.hole("/api/tasks/typen");
    assertThat(typliste.<List<String>>json("$[*].typ")).contains(TESTTYP);
    assertThat(typliste.<List<String>>json("$[?(@.typ == '" + TESTTYP + "')].aktionen[*]"))
        .containsExactly("Sperren", "Freigeben", "Umbenennen");
    assertThat(typliste.<List<String>>json("$[?(@.typ == '" + TESTTYP + "')].schluessel.trenner"))
        .containsExactly("-");

    Antwort angelegt = sitzung.sende("/api/tasks", sperren(2));
    assertThat(angelegt.status()).as(angelegt.rumpf()).isEqualTo(201);
    String alterAuftrag = angelegt.json("$.taskId");
    assertThat(
            monitorDsl
                .select(TASK.PARAMETER)
                .from(TASK)
                .where(TASK.TASK_ID.eq(alterAuftrag))
                .fetchOne(TASK.PARAMETER))
        .isEqualTo("Aktion=Sperren\nNO-1;Grund 1\nNO-2;Grund 2");

    // 3. maxEintraege auf 1, einen anderen Erfolgswert und ein anderes label: wirkt beim
    // naechsten Anlegen. Das label prueft seit A4 die Kopie in der Antwort (E‑1120, Schritt 5).
    aendere(
        struktur -> {
          struktur.put("maxEintraege", 1);
          ((ObjectNode) struktur.get("rueckgaben")).put("ok", "ANDERS");
          ((ObjectNode) struktur.get("felder").get(1)).put("label", "Konto, heute anders");
        });

    Antwort zuViele = sitzung.sende("/api/tasks", sperren(2));
    assertThat(zuViele.status()).as(zuViele.rumpf()).isEqualTo(400);
    assertThat(zuViele.<String>json("$.errors[0].code")).isEqualTo("ZU_VIELE_EINTRAEGE");
    assertThat(zuViele.<String>json("$.errors[0].meldung")).contains("höchstens 1");

    // 4. Ungueltig: aus dem Angebot, Anlegen 404.
    aendere(struktur -> struktur.put("unbekannt", true));

    assertThat(typen()).doesNotContain(TESTTYP);
    assertThat(sitzung.sende("/api/tasks", sperren(1)).status()).as("ungueltig").isEqualTo(404);

    // 5. Der Auftrag von vorher liest seine Kopie: "JA" ist dort Ok, auch wenn die Zeile heute
    // "ANDERS" sagt und ungueltig ist.
    assertThat(monitorDsl.execute(Plattformvertrag.ABHOLEN, alterAuftrag)).isEqualTo(1);
    assertThat(monitorDsl.execute(Plattformvertrag.ERLEDIGT_MELDEN, "JA\nJA", alterAuftrag))
        .isEqualTo(1);

    Antwort alt = sitzung.hole("/api/tasks/" + alterAuftrag);
    assertThat(alt.status()).isEqualTo(200);
    assertThat(alt.<String>json("$.einordnung")).isEqualTo("ERLEDIGT");
    assertThat(alt.<String>json("$.name")).isEqualTo("Gattung aus dem Test");
    assertThat(alt.<List<String>>json("$.eintraege[*].schluessel")).containsExactly("NO-1", "NO-2");
    assertThat(alt.<List<String>>json("$.felder[*].name"))
        .containsExactly("bereich", "konto", "grund", "neuername");
    assertThat(alt.<List<String>>json("$.felder[*].label"))
        .containsExactly("Bereich", "Konto", "Grund", "Neuer Name");

    assertThat(werkGeaendertAm()).as("WERK_ABLADESTELLE unberuehrt").isEqualTo(werkVorher);
  }

  private List<String> typen() throws Exception {
    return sitzung.hole("/api/tasks/typen").json("$[*].typ");
  }

  /** Eine Sperrung mit {@code anzahl} Eintraegen. */
  private static String sperren(int anzahl) {
    List<Map<String, String>> eintraege =
        IntStream.rangeClosed(1, anzahl)
            .mapToObj(
                i -> Map.of("bereich", "NO", "konto", String.valueOf(i), "grund", "Grund " + i))
            .toList();
    return auftrag(neueAnfrageId(), TESTTYP, "Sperren", eintraege);
  }

  /** Aendert die Struktur des Testtyps per {@code UPDATE}; {@code geaendert_am} zieht nach. */
  private void aendere(Consumer<ObjectNode> aenderung) {
    String bisher =
        monitorDsl
            .select(TASK_TYPE.STRUKTUR)
            .from(TASK_TYPE)
            .where(TASK_TYPE.TASK_TYP.eq(TESTTYP))
            .fetchOne(TASK_TYPE.STRUKTUR)
            .data();
    ObjectNode struktur = (ObjectNode) Testdefinitionen.JSON.readTree(bisher);
    aenderung.accept(struktur);
    assertThat(
            monitorDsl
                .update(TASK_TYPE)
                .set(
                    TASK_TYPE.STRUKTUR,
                    JSON.valueOf(Testdefinitionen.JSON.writeValueAsString(struktur)))
                .where(TASK_TYPE.TASK_TYP.eq(TESTTYP))
                .execute())
        .isEqualTo(1);
  }

  private LocalDateTime werkGeaendertAm() {
    return monitorDsl
        .select(TASK_TYPE.GEAENDERT_AM)
        .from(TASK_TYPE)
        .where(TASK_TYPE.TASK_TYP.eq(WERK))
        .fetchOne(TASK_TYPE.GEAENDERT_AM);
  }
}
