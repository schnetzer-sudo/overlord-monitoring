package de.kraftwerkone.overlord.monitor.common;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.ROLLUP_LAUF;

import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * <b>Die eine lesende Stelle fuer den Wasserstand</b> — verhaltensgleich mit der Fassung, die bis
 * zum 17.09.2026 in {@code message/VerengungRepository.wasserstand()} stand: dasselbe Statement,
 * derselbe Kontext.
 *
 * <p>Gezaehlt wird nur, was <b>abgeschlossen</b> ({@code beendet_am IS NOT NULL}) und
 * <b>fehlerfrei</b> ({@code fehler IS NULL}) ist. Ein abgebrochener oder laufender Lauf hat keinen
 * Wasserstand erarbeitet, und ein abgeschlossener mit Fehler ist nicht verlaesslich — beide duerfen
 * nicht behaupten, ihr Fenster sei gerechnet. Genau dafuer traegt {@code rollup_lauf} den Index
 * {@code (beendet_am, fenster_bis)}.
 *
 * <h2>Warum die Methode oeffentlich ist und keinen {@code MandantContext} nimmt</h2>
 *
 * <p>Weil sie keine Mandantendimension <b>hat</b>: {@code rollup_lauf} liegt in {@code
 * overlord_monitor} und traegt keinen Mandanten, keine Nachricht und keinen Prozess, sondern wann
 * welches Zeitfenster gerechnet worden ist. Ein {@code MandantContext}, den sie ignorierte, waere
 * genau der Schein-Kontext, den {@code PaketstrukturTest.mandantcontext_ist_erster_parameter}
 * verbietet. Diese Regel bindet Klassen, die {@code jooq.glassfish} anfassen — diese Klasse fasst
 * ausschliesslich {@code jooq.monitor} an. In {@code message} war dieselbe Methode paketprivat,
 * weil {@code VerengungRepository} das Quellschema anfasst und die Regel dort fuer jede
 * oeffentliche Methode der Klasse gilt; hier steht sie allein, und {@code common} kennt den {@code
 * MandantContext} aus {@code security} ohnehin nicht ({@code
 * PaketstrukturTest.common_haengt_an_keinem_anderen_anwendungspaket}).
 *
 * <p>Gelesen wird ueber {@code glassfishDsl}, den Lese-Pool — wie bisher in {@code message}. Der
 * Rollup-Job liest denselben Wert an eigener Stelle ueber den Schreib-Kontext ({@code
 * RollupSchreibRepository.wasserstand()}); siehe {@link Wasserstand}.
 */
@Repository
public class WasserstandRepository implements Wasserstand {

  private final DSLContext glassfishDsl;

  WasserstandRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  @Override
  public Optional<LocalDateTime> wasserstand() {
    return Optional.ofNullable(
        glassfishDsl
            .select(DSL.max(ROLLUP_LAUF.FENSTER_BIS))
            .from(ROLLUP_LAUF)
            .where(ROLLUP_LAUF.BEENDET_AM.isNotNull())
            .and(ROLLUP_LAUF.FEHLER.isNull())
            .fetchOne(0, LocalDateTime.class));
  }
}
