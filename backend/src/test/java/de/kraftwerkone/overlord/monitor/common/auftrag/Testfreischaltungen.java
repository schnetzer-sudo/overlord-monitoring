package de.kraftwerkone.overlord.monitor.common.auftrag;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE_MANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;

/**
 * Freischaltungen, die ein Test sich selbst anlegt und wieder wegraeumt (Regel T2).
 *
 * <p><b>Seit {@code V103} ist {@code WERK_ABLADESTELLE} fuer keinen Kundenmandanten mehr
 * freigeschaltet</b> ({@code docs/tasks.md} §3.9, E‑1125). Freischaltungen sind Pflegedaten; ein
 * Test, der sich auf die der Testkopie verliesse, hinge an veraenderlichen Daten (Regel 12).
 *
 * <p><b>Reines {@code INSERT} auf eine freie Zeile, geloescht wird genau diese.</b> Steht die Zeile
 * schon da — eine echte Freischaltung oder der Rest eines abgebrochenen Laufs —, schlaegt der Test
 * fehl, statt sie hinterher mitzuloeschen.
 */
public final class Testfreischaltungen {

  private final DSLContext monitorDsl;
  private final List<String[]> angelegt = new ArrayList<>();

  public Testfreischaltungen(DSLContext monitorDsl) {
    this.monitorDsl = monitorDsl;
  }

  /** Schaltet den Typ fuer die Mandanten frei; jede Zeile muss vorher frei sein. */
  public void schalteFrei(String typ, String... mandanten) {
    for (String mandant : mandanten) {
      assertThat(
              monitorDsl.fetchCount(
                  TASK_TYPE_MANDANT,
                  TASK_TYPE_MANDANT.TASK_TYP.eq(typ).and(TASK_TYPE_MANDANT.MANDANT_ID.eq(mandant))))
          .as(
              "Die Freischaltung %s fuer %s ist vor dem Test frei — sonst steht sie auf der"
                  + " Testkopie oder ist von einem abgebrochenen Lauf geblieben",
              typ, mandant)
          .isZero();
      monitorDsl
          .insertInto(TASK_TYPE_MANDANT)
          .set(TASK_TYPE_MANDANT.TASK_TYP, typ)
          .set(TASK_TYPE_MANDANT.MANDANT_ID, mandant)
          .execute();
      angelegt.add(new String[] {typ, mandant});
    }
  }

  /** Loescht genau die Zeilen, die {@link #schalteFrei} angelegt hat. */
  public void raeumeAuf() {
    for (String[] zeile : angelegt) {
      monitorDsl
          .deleteFrom(TASK_TYPE_MANDANT)
          .where(TASK_TYPE_MANDANT.TASK_TYP.eq(zeile[0]))
          .and(TASK_TYPE_MANDANT.MANDANT_ID.eq(zeile[1]))
          .execute();
    }
    angelegt.clear();
  }
}
