package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.jooq.monitor.tables.records.TaskTypeRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Was die Migrationen {@code V100} bis {@code V102} und das Skript {@code
 * scripts/werk-abladestelle-schluessel-leerzeichen.sql} in {@code task_type} geschrieben haben
 * ({@code docs/tasks.md} §4.3): nach {@code V101} hoechstens 20 Eintraege, nach {@code V102} die
 * Beschreibung mit dem Absatz zu „neu oder aendern“, nach dem Skript Leerzeichen in den
 * Schluesselteilen (E‑1136, E‑1142; auf der Testkopie gelaufen am 23.10.2026, E‑1145).
 *
 * <p><b>Warum es diesen Test gibt.</b> Die Struktur steht als JSON in einem SQL-Text. MariaDB liest
 * dort einen Backslash als Escapezeichen, und ein Muster wie {@code \d} kaeme verstuemmelt an —
 * oder die Zeile scheiterte an {@code JSON_VALID}. Der Test holt die Muster <b>in der Datenbank</b>
 * heraus ({@code JSON_EXTRACT}) und vergleicht sie mit dem Text aus dem Auftrag, nicht mit einer
 * zweiten Abschrift. Seit dem Skript traegt das Muster der Schluesselteile einen Backslash ({@code
 * \p{Z}}); der Test haelt fest, dass genau einer ankommt.
 *
 * <p><b>Die Freischaltungen prueft der Test nicht.</b> Bis 1.1.0 stand hier, dass {@code V100} den
 * Typ fuer {@code NEXANS} und {@code IBIS} freischaltet; {@code V103} nimmt genau diese beiden
 * zurueck ({@code docs/tasks.md} §3.9, E‑1125). Seitdem sind Freischaltungen Pflegedaten (Regel
 * 12), und die Tests legen sich ihre selbst an.
 *
 * <p><b>Grenze (Regel T2):</b> Die Zeile ist per SQL pflegbar. Aendert die Betreuung sie auf der
 * Testkopie, wird dieser Test rot — dann hat sich die Testkopie geaendert, nicht der Code, und die
 * Erwartung ist die der Migrationen und der eingecheckten Skripte, nicht eines anderen
 * Pflegestands. Eine Testkopie nur mit den Migrationen, ohne das Skript, macht ihn ebenfalls rot.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class TaskMigrationDbIT {

  @Autowired
  @Qualifier("monitorDsl") private DSLContext monitorDsl;

  @Test
  @DisplayName(
      "WERK_ABLADESTELLE: JSON_VALID, Muster zeichengleich, Struktur und Beschreibung wie im Auftrag")
  void werk_abladestelle_wie_geschrieben() {
    TaskTypeRecord zeile =
        monitorDsl
            .selectFrom(TASK_TYPE)
            .where(TASK_TYPE.TASK_TYP.eq(Testdefinitionen.WERK))
            .fetchOne();
    assertThat(zeile).isNotNull();

    assertThat(
            monitorDsl
                .select(DSL.field("JSON_VALID({0})", Integer.class, TASK_TYPE.STRUKTUR))
                .from(TASK_TYPE)
                .where(TASK_TYPE.TASK_TYP.eq(Testdefinitionen.WERK))
                .fetchOne(0, Integer.class))
        .isEqualTo(1);

    String schluesselteil = "^(?:[^/,\\p{Z}]| ){1,35}$";
    String[] erwartet = {
      schluesselteil, schluesselteil, schluesselteil, "^[0-9]{6}$", "^[0-9]{6}$"
    };
    for (int i = 0; i < erwartet.length; i++) {
      String muster =
          monitorDsl
              .select(
                  DSL.field(
                      "JSON_UNQUOTE(JSON_EXTRACT({0}, {1}))",
                      String.class, TASK_TYPE.STRUKTUR, DSL.inline("$.felder[" + i + "].muster")))
              .from(TASK_TYPE)
              .where(TASK_TYPE.TASK_TYP.eq(Testdefinitionen.WERK))
              .fetchOne(0, String.class);
      assertThat(muster).as("felder[%d].muster", i).isEqualTo(erwartet[i]);
    }

    assertThat(Testdefinitionen.JSON.readTree(zeile.getStruktur().data()))
        .isEqualTo(Testdefinitionen.werkBaum());
    // Im JSON-Text steht ein Backslash nur maskiert vor p{Z}; die Muster oben zeigen, dass daraus
    // genau einer wird.
    assertThat(zeile.getStruktur().data().replace("\\\\p{Z}", "p{Z}")).doesNotContain("\\");
    assertThat(zeile.getName()).isEqualTo(Testdefinitionen.WERK_NAME);
    assertThat(zeile.getBeschreibung()).isEqualTo(Testdefinitionen.werkBeschreibung());
    assertThat(zeile.getAktiv()).isTrue();
    assertThat(zeile.getGeaendertAm()).isNotNull();
  }
}
