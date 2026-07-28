package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.common.ReadOnlyViolationException;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteType;

/**
 * Dritte Schicht des Schreibschutzes auf {@code GlassfishDB}: ein jOOQ-{@link ExecuteListener}, der
 * auf dem Lese-{@code DSLContext} alles abweist, was nicht {@link ExecuteType#READ} ist. Er faengt
 * den Fehler dort, wo er entsteht — im Code, nicht erst im Netz.
 *
 * <p>Diese Schicht ist <b>Zusatz</b>. Die Garantie tragen die DB-Rechte: kein Benutzer dieser
 * Anwendung besitzt Schreibrecht auf {@code GlassfishDB}. Siehe {@code docs/datenzugriff.md}.
 */
public class ReadOnlyExecuteListener implements ExecuteListener {

  @Override
  public void start(ExecuteContext ctx) {
    if (ctx.type() != ExecuteType.READ) {
      throw new ReadOnlyViolationException(
          "Schreibender Zugriff auf den Lese-DSLContext (GlassfishDB) ist verboten. ExecuteType: "
              + ctx.type());
    }
  }
}
