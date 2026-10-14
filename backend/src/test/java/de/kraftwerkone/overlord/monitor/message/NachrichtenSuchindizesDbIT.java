package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.config.DatabaseProperties;
import java.util.List;
import java.util.Locale;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Die Suche haengt an zwei Indexnamen des Altsystems</b> — dieser Test haelt fest, dass es sie
 * in der verbundenen Datenbank gibt und dass sie auf {@code ProcessID} liegen.
 *
 * <p>Der Suchpfad der Nachrichtenliste schickt {@code IGNORE INDEX FOR JOIN (ProejctIDIDX,
 * Message_ProcessFK)} auf {@code Message} (E‑237, {@link NachrichtenRepository#PROZESSINDIZES}).
 * MariaDB 10.6 beantwortet einen unbekannten Namen darin mit Fehler 1176, auch wenn nur einer der
 * beiden fehlt — gemessen am 13.10.2026 gegen die Testkopie. Benennt das Altsystem einen der beiden
 * um, scheitert jede Suche mit {@code 500}. Dieser Test wird dann rot, und zwar mit dem Namen.
 *
 * <p><b>Warum zusaetzlich zu {@code IndexbestandDbIT}.</b> Die Sollliste bewacht den ganzen
 * Bestand, aber nicht, dass es <i>diese</i> Konstanten sind, die der Code schickt. Hier steht die
 * Verbindung von Konstante und Datenbank, nichts weiter.
 *
 * <p><b>Gross- und Kleinschreibung zaehlt nicht</b>, weil MariaDB den Namen im Hinweis ohne sie
 * aufloest (gemessen am 13.10.2026: {@code proejctididx} wirkt wie {@code ProejctIDIDX}). Ein Test,
 * der an der Schreibweise rot wuerde, meldete einen Ausfall, den es nicht gibt.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class NachrichtenSuchindizesDbIT {

  private static final Field<String> SCHEMA = DSL.field("TABLE_SCHEMA", String.class);
  private static final Field<String> TABELLE = DSL.field("TABLE_NAME", String.class);
  private static final Field<String> INDEXNAME = DSL.field("INDEX_NAME", String.class);
  private static final Field<String> SPALTE = DSL.field("COLUMN_NAME", String.class);
  private static final Field<Integer> POSITION = DSL.field("SEQ_IN_INDEX", Integer.class);

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private DatabaseProperties props;

  @Test
  @DisplayName(
      "Beide Indizes aus dem Hinweis der Suche existieren auf Message und liegen auf ProcessID")
  void beide_prozessindizes_existieren_auf_processid() {
    assertThat(NachrichtenRepository.PROZESSINDIZES)
        .as("Genau die beiden Indizes aus §3.2 der Projektbeschreibung")
        .hasSize(2);

    for (String name : NachrichtenRepository.PROZESSINDIZES) {
      List<Record2<String, Integer>> spalten =
          glassfishDsl
              .select(SPALTE, POSITION)
              .from("information_schema.STATISTICS")
              .where(SCHEMA.eq(props.glassfishSchema()))
              .and(TABELLE.eq("Message"))
              .and(DSL.lower(INDEXNAME).eq(name.toLowerCase(Locale.ROOT)))
              .orderBy(POSITION)
              .fetch();

      assertThat(spalten)
          .as(
              "Index %s fehlt auf Message. Die Suche schickt ihn im Hinweis und scheitert dann mit"
                  + " Fehler 1176 (500).",
              name)
          .hasSize(1);
      assertThat(spalten.getFirst().get(SPALTE))
          .as("Index %s liegt nicht (allein) auf ProcessID", name)
          .isEqualTo("ProcessID");
    }
  }
}
