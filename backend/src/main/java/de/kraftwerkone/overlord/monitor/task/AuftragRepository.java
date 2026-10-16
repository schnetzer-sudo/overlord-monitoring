package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.SelectField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Der Datenzugriff der Auftraege — ausschliesslich {@code overlord_monitor}, lesend und schreibend
 * ueber {@code monitorDsl}. {@code GlassfishDB} kommt hier nicht vor.
 *
 * <p><b>Jede Methode traegt den Mandanten als ersten Parameter</b> (Regel M2) und <b>jedes
 * Statement hat ihn im {@code WHERE}</b> (Regel M3). Eine fremde oder erfundene Kennung ergibt
 * dasselbe Statement mit null Zeilen. Das Laden der Typen liegt seit A2 in {@code
 * common.auftrag.AuftragstypRepository} (E‑1037).
 *
 * <p><b>Keine Uhr in Java</b> (E‑1011): {@code eingestellt_am} und die Grenze der Liste kommen aus
 * {@code UTC_TIMESTAMP(3)} der Datenbank.
 *
 * <p>Ohne {@code @Transactional}: Jedes Statement laeuft fuer sich. Das ist fuer die Wiederholung
 * gewollt — nach einem verlorenen Wettlauf am eindeutigen Index muss die zweite Lesung den Auftrag
 * der ersten Anfrage sehen, und eine Transaktion mit ihrem Schnappschuss saehe ihn nicht.
 */
@Repository
public class AuftragRepository {

  /** MariaDB: Doppelter Eintrag in einem eindeutigen Index. */
  private static final int DOPPELTER_SCHLUESSEL = 1062;

  /** Der eindeutige Index, an dem eine Wiederholung scheitert. */
  private static final String ANFRAGE_INDEX = "uq_task_anfrage";

  private static final Field<LocalDateTime> UTC_JETZT =
      DSL.field("UTC_TIMESTAMP(3)", SQLDataType.LOCALDATETIME(3));

  private static final Field<LocalDateTime> VOR_SIEBEN_TAGEN =
      DSL.field("UTC_TIMESTAMP(3) - INTERVAL 7 DAY", SQLDataType.LOCALDATETIME(3));

  private static final List<SelectField<?>> AUFTRAGSSPALTEN =
      List.of(
          TASK.TASK_ID,
          TASK.TASK_TYP,
          TASK.DEFINITION,
          TASK.AKTION,
          TASK.EINTRAEGE,
          TASK.STATUS,
          TASK.RUECKGABE,
          TASK.FEHLERCODE,
          TASK.FEHLERTEXT,
          TASK.EINGESTELLT_AM,
          TASK.ABGEHOLT_AM,
          TASK.AUSGEFUEHRT_AM);

  private final DSLContext monitorDsl;

  AuftragRepository(@Qualifier("monitorDsl") DSLContext monitorDsl) {
    this.monitorDsl = monitorDsl;
  }

  /** Eine Zeile aus {@code task}, soweit die Antwort sie braucht. */
  public record Auftragszeile(
      String taskId,
      String typ,
      String definition,
      String aktion,
      String eintraege,
      String status,
      String rueckgabe,
      String fehlercode,
      String fehlertext,
      LocalDateTime eingestelltAm,
      LocalDateTime abgeholtAm,
      LocalDateTime ausgefuehrtAm) {}

  /** Was beim Anlegen geschrieben wird; die JSON-Spalten als Text. */
  public record NeuerAuftrag(
      String taskId,
      String typ,
      String definition,
      String aktion,
      String eintraege,
      String anfrageId,
      String parameter,
      String angelegtVon) {}

  /** Ein Auftrag des Mandanten. Fremd und unbekannt sind dasselbe Statement mit null Zeilen. */
  public Optional<Auftragszeile> finde(MandantContext mandant, String taskId) {
    return monitorDsl
        .select(AUFTRAGSSPALTEN)
        .from(TASK)
        .where(TASK.TASK_ID.eq(taskId))
        .and(TASK.MANDANT_ID.eq(mandant.mandantId()))
        .fetchOptional(AuftragRepository::auftragszeile);
  }

  /** Der Auftrag, den dieser Mandant mit dieser {@code anfrageId} schon angelegt hat. */
  public Optional<Auftragszeile> findeNachAnfrage(MandantContext mandant, String anfrageId) {
    return monitorDsl
        .select(AUFTRAGSSPALTEN)
        .from(TASK)
        .where(TASK.MANDANT_ID.eq(mandant.mandantId()))
        .and(TASK.ANFRAGE_ID.eq(anfrageId))
        .fetchOptional(AuftragRepository::auftragszeile);
  }

  /**
   * Die laufenden Auftraege unabhaengig vom Alter, dazu die uebrigen der letzten 7 Tage
   * (Datenbankzeit), neueste zuerst ({@code docs/tasks.md} §6.3). Kein Zeitfenster-Parameter und
   * kein Cursor: {@code task} ist nicht aus {@code GlassfishDB} abgeleitet (E‑1016).
   */
  public List<Auftragszeile> findeAktuelle(MandantContext mandant, int hoechstens) {
    return monitorDsl
        .select(AUFTRAGSSPALTEN)
        .from(TASK)
        .where(TASK.MANDANT_ID.eq(mandant.mandantId()))
        .and(
            TASK.STATUS
                .in(TaskStatusClassifier.LAUFEND)
                .or(DSL.coalesce(TASK.AUSGEFUEHRT_AM, TASK.EINGESTELLT_AM).ge(VOR_SIEBEN_TAGEN)))
        .orderBy(TASK.EINGESTELLT_AM.desc(), TASK.TASK_ID.desc())
        .limit(hoechstens)
        .fetch(AuftragRepository::auftragszeile);
  }

  /**
   * Legt den Auftrag mit Status {@code OFFEN} an.
   *
   * @return {@code false}, wenn dieser Mandant dieselbe {@code anfrageId} inzwischen schon angelegt
   *     hat (verlorener Wettlauf zweier gleicher Anfragen). Jeder andere Fehler fliegt.
   */
  public boolean legeAn(MandantContext mandant, NeuerAuftrag auftrag) {
    try {
      monitorDsl
          .insertInto(TASK)
          .set(TASK.TASK_ID, auftrag.taskId())
          .set(TASK.MANDANT_ID, mandant.mandantId())
          .set(TASK.TASK_TYP, auftrag.typ())
          .set(TASK.DEFINITION, JSON.valueOf(auftrag.definition()))
          .set(TASK.AKTION, auftrag.aktion())
          .set(TASK.EINTRAEGE, JSON.valueOf(auftrag.eintraege()))
          .set(TASK.ANFRAGE_ID, auftrag.anfrageId())
          .set(TASK.PARAMETER, auftrag.parameter())
          .set(TASK.STATUS, TaskStatusClassifier.OFFEN)
          .set(TASK.EINGESTELLT_AM, UTC_JETZT)
          .set(TASK.ANGELEGT_VON, auftrag.angelegtVon())
          .execute();
      return true;
    } catch (DataAccessException fehler) {
      SQLException ursache = fehler.getCause(SQLException.class);
      if (ursache != null
          && ursache.getErrorCode() == DOPPELTER_SCHLUESSEL
          && String.valueOf(ursache.getMessage()).contains(ANFRAGE_INDEX)) {
        return false;
      }
      throw fehler;
    }
  }

  private static Auftragszeile auftragszeile(Record zeile) {
    return new Auftragszeile(
        zeile.get(TASK.TASK_ID),
        zeile.get(TASK.TASK_TYP),
        text(zeile.get(TASK.DEFINITION)),
        zeile.get(TASK.AKTION),
        text(zeile.get(TASK.EINTRAEGE)),
        zeile.get(TASK.STATUS),
        zeile.get(TASK.RUECKGABE),
        zeile.get(TASK.FEHLERCODE),
        zeile.get(TASK.FEHLERTEXT),
        zeile.get(TASK.EINGESTELLT_AM),
        zeile.get(TASK.ABGEHOLT_AM),
        zeile.get(TASK.AUSGEFUEHRT_AM));
  }

  private static String text(JSON json) {
    return json == null ? null : json.data();
  }
}
