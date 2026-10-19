package de.kraftwerkone.overlord.monitor.assistent;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.jooq.monitor.tables.records.TaskTypeRecord;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Der Testsatz misst mit der Definition, die gilt ({@code docs/assistent.md} §8): Beschreibung und
 * Struktur von {@code WERK_ABLADESTELLE} in den Testressourcen sind <b>zeichengleich</b> mit der
 * Zeile in {@code task_type}.
 *
 * <p>{@code AssistentTestsatzLauf} laeuft ohne Datenbank und liest beide aus den Testressourcen.
 * Aendert eine Migration die Zeile, ohne dass die Ressourcen nachgezogen werden, misst der Testsatz
 * still eine andere Beschreibung — dieser Test wird dann rot. Die Struktur steht seit {@code V101}
 * ({@code JSON_SET}) in der kompakten Form von MariaDB; die Ressource traegt genau diese Form.
 *
 * <p><b>Grenze (Regel T2):</b> wie {@code TaskMigrationDbIT}. Die Zeile ist per SQL pflegbar;
 * aendert die Betreuung sie auf der Testkopie, hat sich die Testkopie geaendert, nicht der Code.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class TestsatzDefinitionDbIT {

  @Autowired
  @Qualifier("monitorDsl") private DSLContext monitorDsl;

  @Test
  @DisplayName("Beschreibung und Struktur des Testsatzes sind zeichengleich mit task_type")
  void zeichengleich_mit_task_type() {
    TaskTypeRecord zeile =
        monitorDsl
            .selectFrom(TASK_TYPE)
            .where(TASK_TYPE.TASK_TYP.eq(Testdefinitionen.WERK))
            .fetchOne();
    assertThat(zeile).isNotNull();

    assertThat(zeile.getBeschreibung()).isEqualTo(Testdefinitionen.werkBeschreibung());
    assertThat(zeile.getStruktur().data()).isEqualTo(Testdefinitionen.werkStruktur());
    assertThat(zeile.getName()).isEqualTo(Testdefinitionen.WERK_NAME);
    assertThat(AssistentTestsatzLauf.werk().beschreibung()).isEqualTo(zeile.getBeschreibung());
  }
}
