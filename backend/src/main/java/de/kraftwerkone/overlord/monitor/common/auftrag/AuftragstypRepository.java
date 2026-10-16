package de.kraftwerkone.overlord.monitor.common.auftrag;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE_MANDANT;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Das Lesen der Auftragstypen aus {@code overlord_monitor} — ausschliesslich lesend ueber {@code
 * monitorDsl}. {@code GlassfishDB} kommt hier nicht vor.
 *
 * <p><b>Stand bis A2 in {@code task.AuftragRepository}</b> und ist mit dem Typkatalog nach {@code
 * common} gewandert, weil der Assistent ihn ebenfalls braucht (E‑1037). Das Statement ist
 * unveraendert; gemessen in M1000 ({@code docs/tasks.md} §9.3, L1).
 *
 * <p><b>Der Mandant ist erster Parameter</b> (Regel M2) und <b>steht im {@code WHERE}</b> (Regel
 * M3): Die Freischaltung ist Teil des Statements, nicht ein nachgelagerter Filter.
 */
@Repository
public class AuftragstypRepository {

  private final DSLContext monitorDsl;

  AuftragstypRepository(@Qualifier("monitorDsl") DSLContext monitorDsl) {
    this.monitorDsl = monitorDsl;
  }

  /** Eine Zeile aus {@code task_type}, noch ungeprueft. */
  public record Typzeile(
      String typ, String name, String beschreibung, String struktur, LocalDateTime geaendertAm) {}

  /**
   * Die aktiven Typen, die fuer den Mandanten freigeschaltet sind, samt Struktur. Ob sie gueltig
   * sind, entscheidet {@link TypKatalog}.
   */
  public List<Typzeile> angeboteneTypen(MandantContext mandant) {
    return monitorDsl
        .select(
            TASK_TYPE.TASK_TYP,
            TASK_TYPE.NAME,
            TASK_TYPE.BESCHREIBUNG,
            TASK_TYPE.STRUKTUR,
            TASK_TYPE.GEAENDERT_AM)
        .from(TASK_TYPE_MANDANT)
        .join(TASK_TYPE)
        .on(TASK_TYPE.TASK_TYP.eq(TASK_TYPE_MANDANT.TASK_TYP))
        .where(TASK_TYPE_MANDANT.MANDANT_ID.eq(mandant.mandantId()))
        .and(TASK_TYPE.AKTIV.isTrue())
        .orderBy(TASK_TYPE.TASK_TYP)
        .fetch(
            zeile ->
                new Typzeile(
                    zeile.get(TASK_TYPE.TASK_TYP),
                    zeile.get(TASK_TYPE.NAME),
                    zeile.get(TASK_TYPE.BESCHREIBUNG),
                    text(zeile.get(TASK_TYPE.STRUKTUR)),
                    zeile.get(TASK_TYPE.GEAENDERT_AM)));
  }

  private static String text(JSON json) {
    return json == null ? null : json.data();
  }
}
