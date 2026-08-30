package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.ROLLUP_LAUF;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Die beiden Abfragen, mit denen die Fensterverengung den Rollup befragt.
 *
 * <p>Getrennt von {@link Fensterverengung}, weil <b>nur Repository-Klassen {@code jooq.glassfish}
 * anfassen duerfen</b> ({@code PaketstrukturTest.jooq_glassfish_nur_in_repository_klassen}): Am
 * Import einer Tabelle aus dem Quellschema muss sofort erkennbar sein, dass sie nur gelesen wird.
 * Hier steht das SQL, dort die Entscheidung, ob ueberhaupt gefragt werden darf.
 *
 * <p>Gelesen wird ueber {@code glassfishDsl} — den <b>Lese</b>-Pool. Er darf beide Schemata; das
 * tut {@code BamSucheRepository} seit Schritt 7 genauso ({@code BAM_SOLLAENGE} aus {@code
 * overlord_monitor}). Auf {@code GlassfishDB} wird auch hier ausschliesslich gelesen (Regel S1).
 */
@Repository
public class VerengungRepository {

  /**
   * Eigener Alias fuer die Mandantenkette. In der Listenabfrage haengt {@code PROCESS} schon fuer
   * den Anzeigenamen; ein zweiter Gebrauch derselben Tabelle braucht einen eigenen Namen.
   */
  private static final Process VERENGUNG_PROCESS = PROCESS.as("verengung_process");

  private final DSLContext glassfishDsl;
  private final MessageStatusClassifier statusClassifier;

  VerengungRepository(
      @Qualifier("glassfishDsl") DSLContext glassfishDsl,
      MessageStatusClassifier statusClassifier) {
    this.glassfishDsl = glassfishDsl;
    this.statusClassifier = statusClassifier;
  }

  /**
   * Bis wohin der Rollup Bescheid weiss — {@code null}, wenn er noch nie gerechnet hat.
   *
   * <p>Gezaehlt wird nur, was <b>abgeschlossen</b> ({@code beendet_am IS NOT NULL}) und
   * <b>fehlerfrei</b> ({@code fehler IS NULL}) ist. Ein abgebrochener oder laufender Lauf hat
   * keinen Wasserstand erarbeitet, und ein abgeschlossener mit Fehler ist nicht verlaesslich —
   * beide duerfen nicht behaupten, ihr Fenster sei gerechnet. Genau dafuer traegt {@code
   * rollup_lauf} den Index {@code (beendet_am, fenster_bis)}.
   *
   * <h2>Warum sie paketprivat ist und keinen {@code MandantContext} nimmt</h2>
   *
   * <p>Weil sie keine Mandantendimension <b>hat</b>. Sie liest {@code rollup_lauf} — das Protokoll
   * des Rollup-Laufs in {@code overlord_monitor} —, und dort steht kein Mandant, keine Nachricht
   * und kein Prozess, sondern wann welches Zeitfenster gerechnet worden ist. Ein {@code
   * MandantContext}, den sie ignorierte, waere genau der Schein-Kontext, den {@code
   * PaketstrukturTest.mandantcontext_ist_erster_parameter} ausdruecklich verbietet;
   * {@code @OhneMandantenkontext} wiederum ist auf {@code MandantRepository} beschraenkt und gilt
   * Methoden, die den Kontext erst <i>herstellen</i> — das tut diese nicht.
   *
   * <p>Also bleibt sie aus der oeffentlichen Flaeche heraus. Sie hat genau einen Aufrufer, {@link
   * Fensterverengung}, und der liegt in diesem Paket.
   */
  LocalDateTime wasserstand() {
    return glassfishDsl
        .select(DSL.max(ROLLUP_LAUF.FENSTER_BIS))
        .from(ROLLUP_LAUF)
        .where(ROLLUP_LAUF.BEENDET_AM.isNotNull())
        .and(ROLLUP_LAUF.FEHLER.isNull())
        .fetchOne(0, LocalDateTime.class);
  }

  /**
   * Was der Rollup ueber eine Stufe sagt.
   *
   * @param zeilen wie viele Zeilen in den beruehrten und gerechneten Stunden liegen — die Zahl fuer
   *     den Nullfall
   * @param untergrenze die aelteste Stunde, ab der {@code schwelle} Zeilen zusammenkommen, oder
   *     {@code null}, wenn die Schwelle nicht erreicht wird
   */
  public record Auskunft(long zeilen, LocalDateTime untergrenze) {}

  /**
   * Fragt eine Stufe ab: von {@code stufeVon} bis zur Obergrenze der {@link Verengungsgrenzen}.
   *
   * <h2>Die Form ist nicht beliebig</h2>
   *
   * <p><b>Die Mandantenkette steht als {@code EXISTS} und nicht als {@code JOIN}.</b> Zwei Gruende,
   * und beide zaehlen: {@code ProjectMandant} ist n:m — ein {@code JOIN} vervielfachte
   * Rollupzeilen, die Summe waere zu gross und die Verengung ginge <b>zu weit</b>. Und der Index
   * {@code (process_id, stunde)} aus {@code V11} wirkt nur in dieser Form; mit einer
   * Prozess-Literalliste macht derselbe Index die engen Stufen sogar teurer (M104: {@code NEXANS}
   * 2,204 → 7,813 ms).
   *
   * <p><b>Oberhalb des Wasserstands faellt alles heraus</b>, nicht erst aus der kumulierten Summe:
   * Sonst zaehlte der Nullfall Stunden mit, ueber die der Rollup nichts weiss.
   *
   * <p><b>Der Statusfilter wird aufgerufen und nicht nachgebaut.</b> {@code
   * MessageStatusClassifier.bedingung} nimmt ein beliebiges {@code Field<String>}; der Rollup
   * traegt den Rohwert (Entscheidung E-g in {@code V9}) mit derselben Sortierung wie die Quelle.
   * Damit ist es <b>derselbe Ausdruck</b> wie auf {@code Message}, nur mit anderem Feld — die
   * einzige Bauform, die nicht auseinanderlaeuft.
   */
  public Auskunft frageStufe(
      MandantContext mandant,
      Nachrichtenabfrage abfrage,
      Verengungsgrenzen grenzen,
      LocalDateTime stufeVon,
      int schwelle) {

    List<Condition> wo = new ArrayList<>();
    wo.add(MESSAGE_ROLLUP.STUNDE.ge(stufeVon));
    wo.add(MESSAGE_ROLLUP.STUNDE.le(grenzen.hAllBis()));
    wo.add(MESSAGE_ROLLUP.STUNDE.lt(grenzen.wasserstand()));
    wo.add(mandantenkette(mandant));
    if (!abfrage.status().isEmpty()) {
      wo.add(statusClassifier.bedingung(abfrage.status(), MESSAGE_ROLLUP.MESSAGE_STATUS));
    }
    if (!abfrage.prozessIds().isEmpty()) {
      wo.add(MESSAGE_ROLLUP.PROCESS_ID.in(abfrage.prozessIds()));
    }

    // Nur vollstaendig im Fenster liegende Stunden gehen in die kumulierte Summe ein. Alles
    // andere waere eine Oberschranke -- und damit die Erlaubnis, zu weit zu schneiden.
    Field<Boolean> voll =
        DSL.field(
            MESSAGE_ROLLUP
                .STUNDE
                .ge(grenzen.hVollVon())
                .and(MESSAGE_ROLLUP.STUNDE.le(grenzen.hVollBis())));

    Table<?> jeStunde =
        DSL.select(
                MESSAGE_ROLLUP.STUNDE.as("stunde"),
                DSL.sum(MESSAGE_ROLLUP.ANZAHL).as("anzahl"),
                voll.as("voll"))
            .from(MESSAGE_ROLLUP)
            .where(wo)
            .groupBy(MESSAGE_ROLLUP.STUNDE)
            .asTable("je_stunde");

    Field<LocalDateTime> stunde = jeStunde.field("stunde", LocalDateTime.class);
    Field<BigDecimal> anzahl = jeStunde.field("anzahl", BigDecimal.class);
    Field<Boolean> istVoll = jeStunde.field("voll", Boolean.class);

    Table<?> kumuliert =
        DSL.select(
                stunde,
                anzahl,
                DSL.sum(DSL.when(DSL.condition(istVoll), anzahl).otherwise(BigDecimal.ZERO))
                    .over(DSL.orderBy(stunde.desc()))
                    .as("kumuliert"))
            .from(jeStunde)
            .asTable("kumuliert");

    Field<LocalDateTime> kStunde = kumuliert.field("stunde", LocalDateTime.class);
    Field<BigDecimal> kAnzahl = kumuliert.field("anzahl", BigDecimal.class);
    Field<BigDecimal> kSumme = kumuliert.field("kumuliert", BigDecimal.class);

    Record2<BigDecimal, LocalDateTime> satz =
        glassfishDsl
            .select(
                DSL.sum(kAnzahl),
                DSL.max(DSL.when(kSumme.ge(BigDecimal.valueOf(schwelle)), kStunde)))
            .from(kumuliert)
            .fetchOne();

    if (satz == null || satz.value1() == null) {
      return new Auskunft(0L, null);
    }
    return new Auskunft(satz.value1().longValue(), satz.value2());
  }

  private static Condition mandantenkette(MandantContext mandant) {
    return DSL.exists(
        DSL.selectOne()
            .from(VERENGUNG_PROCESS)
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(VERENGUNG_PROCESS.PROJECTID))
            .where(VERENGUNG_PROCESS.PROCESSID.eq(MESSAGE_ROLLUP.PROCESS_ID))
            .and(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())));
  }
}
