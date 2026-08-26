package de.kraftwerkone.overlord.monitor.rollup;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Der <b>lesende</b> Datenzugriff des Rollups: die Aggregation aus {@code GlassfishDB.Message}.
 *
 * <p>Gelesen wird ueber den <b>Lese-Kontext</b> {@code glassfishDsl}. Er traegt den {@code
 * ReadOnlyExecuteListener} und weist jeden Nicht-Lesezugriff ab; sein Benutzer {@code monitor_read}
 * hat auf {@code GlassfishDB} ohnehin nur {@code SELECT}.
 *
 * <h2>Diese Klasse bekommt keinen {@code MandantContext} — die dritte benannte Ausnahme</h2>
 *
 * <p>{@code docs/mandantentrennung.md} §4 verlangt, dass <b>jede</b> oeffentliche Methode einer
 * Klasse, die {@code jooq.glassfish} anfasst, den {@link
 * de.kraftwerkone.overlord.monitor.security.MandantContext} als ersten Pflichtparameter traegt
 * (Regel M2), und {@code PaketstrukturTest} setzt das maschinell durch.
 *
 * <p><b>Der Rollup-Job hat keinen Mandanten.</b> Er liest bewusst ueber alle Mandanten, weil {@code
 * message_rollup} nach Entscheidung E-a keinen Mandanten kennt — der wird erst beim Lesen in 10b
 * ueber {@code Process -> ProjectMandant} gejoint, im Statement und nicht nachgelagert.
 *
 * <p><b>Ein {@code MandantContext.alle()} waere eine Luege im Typsystem</b> und wuerde die Regel
 * entwerten, deren einziger Zweck es ist, dass so etwas nicht existiert. Deshalb steht in {@code
 * PaketstrukturTest} stattdessen eine <b>namentliche</b> Ausnahme — sie nennt diese Klasse beim
 * Namen und gilt fuer keine andere, auch nicht fuer eine zweite im selben Paket. Gefuehrt wird sie
 * in {@code docs/mandantentrennung.md} §4 und in {@code docs/rollup.md}.
 *
 * <p><b>Was die Ausnahme nicht aufweicht:</b> Sie gilt fuer <b>diese</b> Klasse und nicht fuer das
 * Paket. {@link RollupSchreibRepository} fasst {@code jooq.glassfish} gar nicht erst an — deshalb
 * sind die beiden Haelften getrennt und nicht, wie beim Katalog, in einer Klasse.
 */
@Repository
public class RollupLeseRepository {

  /**
   * Der Stundeneimer, <b>woertlich die Fassung aus M88</b>. Sie ist mit {@code EXPLAIN} und
   * Laufzeit belegt (Regel L7) und wird uebernommen, nicht nachgebaut.
   *
   * <p><b>Warum ein {@code VARCHAR} und kein {@code DATETIME}:</b> {@code DATE_FORMAT} liefert eine
   * Zeichenkette. Ein {@code CAST(... AS DATETIME)} darum herum waere lesbarer, aber es waere eine
   * Abweichung von dem, was gemessen ist — und der Gewinn waere null, weil die Umwandlung in Java
   * eine Zeile kostet ({@link #STUNDENFORMAT}).
   *
   * <p><b>Kein {@code STRAIGHT_JOIN}, in keiner Fassung</b> (M42: Faktor 219 bis 1094).
   */
  private static final Field<String> STUNDE =
      DSL.field(
          "date_format({0}, '%Y-%m-%d %H:00:00')", SQLDataType.VARCHAR, MESSAGE.MESSAGELASTUPDATE);

  /** Das Format, das {@link #STUNDE} liefert. */
  private static final DateTimeFormatter STUNDENFORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final DSLContext glassfishDsl;

  RollupLeseRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Die Aggregation eines Zeitfensters: je Stundeneimer, Prozess und Rohstatus eine Zeile mit ihrer
   * Anzahl.
   *
   * <h2>Die gemessene Abfrage (M88)</h2>
   *
   * <pre>{@code
   * SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
   *        ProcessID, MessageStatus, COUNT(*) AS anzahl
   * FROM GlassfishDB.Message
   * WHERE MessageLastUpdate >= ? AND MessageLastUpdate < ?
   * GROUP BY stunde, ProcessID, MessageStatus;
   * }</pre>
   *
   * <p><b>Ein Unterschied zur Messfassung, und er ist der bessere Weg.</b> jOOQ schreibt in {@code
   * GROUP BY} den <b>vollen Ausdruck</b> aus, wo M88 den Alias {@code stunde} verwendet. Semantisch
   * ist das dasselbe — und es ist genau die Fassung, die Befund 11 empfiehlt: Dort hat {@code GROUP
   * BY} an eine gleichnamige <i>Tabellenspalte</i> gebunden statt an den Ausdrucksalias, und bei
   * zwei von drei Mandanten sah das Ergebnis trotzdem richtig aus. {@code Message} hat zwar keine
   * Spalte {@code stunde}, aber die sichere Form kostet hier nichts.
   *
   * <h2>Zugriffspfad und Laufzeit (Regel L7)</h2>
   *
   * <p>{@code range} ueber {@code MessageLastUpdateIDX}, {@code Using index condition; Using
   * temporary; Using filesort} — bei allen sechs in M88 gemessenen Fenstern derselbe Plan. Der
   * zusammengesetzte {@code MessageLastUpdateProcessMessageIDX} bringt dem Rollup <b>nichts</b>: Er
   * ist fuer den Cursor der Nachrichtenliste gebaut, und {@code MessageStatus} steht in keinem der
   * beiden Indizes, jede Fassung braucht also den Rueckgriff auf die Tabellenzeile.
   *
   * <p><b>Oberhalb einer geschaetzten Bereichsgroesse von rund einer halben Million Saetzen kippt
   * der Optimierer</b> doch auf den zusammengesetzten Index (M92, Monatsscheiben). Auf die Laufzeit
   * wirkt sich das nicht sichtbar aus: 11,4 µs je Zeile mit dem schmalen, 11,1 µs mit dem breiten.
   * Fuer den stuendlichen Delta-Lauf bleibt es bei {@code MessageLastUpdateIDX}.
   *
   * <table border="1">
   *   <caption>Gemessene Laufzeiten, beste von fuenf</caption>
   *   <tr><th>Fenster</th><th>Zeilen</th><th>Laufzeit</th></tr>
   *   <tr><td>dichteste Stunde des Gesamtbestands</td><td>8.630</td><td>88,167 ms</td></tr>
   *   <tr><td>letzte Stunde des Bestands</td><td>285</td><td>3,190 ms</td></tr>
   *   <tr><td>groesste Monatsscheibe (2025-07)</td><td>248.320</td><td>2,745 s</td></tr>
   *   <tr><td>leere Monatsscheibe (2026-01)</td><td>0</td><td>0,001 s</td></tr>
   * </table>
   *
   * <p><b>Die Kosten sind ueber drei Groessenordnungen linear: 10,2 bis 11,4 µs je gelesener
   * Zeile</b> (M88 und M92 unabhaengig voneinander). Die Laufzeit haengt an der Zeilenzahl, nicht
   * an der Fensterbreite — ein 48-Stunden-Fenster mit 256 Zeilen kostet 2,889 ms.
   *
   * <h2>Die Obergrenze, die der Aufrufer kennen muss</h2>
   *
   * <p><b>Der Lese-Pool setzt {@code SET SESSION max_statement_time=10}</b> ({@code
   * docs/datenzugriff.md} §1). Ein Fenster ueber den Gesamtbestand reisst diese Grenze: <b>gemessen
   * am 26.08.2026, Fehler 1969 „Query execution was interrupted"</b> nach zehn Sekunden. Eine
   * Monatsscheibe kostet dagegen <b>2,575 s</b> (groesste Scheibe 2025-07, 248.320 Zeilen, beste
   * von fuenf) — Faktor 3,9 Luft.
   *
   * <p><b>Deshalb schneidet der Aufrufer, nicht diese Methode.</b> {@code RollupJob} zerlegt sein
   * Fenster in Monatsscheiben und ruft hier je Scheibe einmal auf — die Bauvorgabe aus M92: „Er
   * iteriert ueber einen Kalender, nicht ueber die vorhandenen Daten." Ein Delta-Fenster von
   * wenigen Stunden ist dabei genau eine Scheibe.
   *
   * <p><b>Kein {@code MandantContext}, kein Mandantenfilter</b> — siehe der Kasten an der Klasse.
   *
   * @param von untere Grenze, einschliesslich
   * @param bis obere Grenze, <b>ausschliesslich</b>. Beide sind volle Stundenanfaenge; wer sie aus
   *     {@link RollupFenster} nimmt, bekommt das zugesichert
   */
  public List<RollupZeile> aggregiere(LocalDateTime von, LocalDateTime bis) {
    return glassfishDsl
        .select(STUNDE, MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS, DSL.count())
        .from(MESSAGE)
        .where(MESSAGE.MESSAGELASTUPDATE.ge(von))
        .and(MESSAGE.MESSAGELASTUPDATE.lt(bis))
        .groupBy(STUNDE, MESSAGE.PROCESSID, MESSAGE.MESSAGESTATUS)
        .fetch(
            satz ->
                new RollupZeile(
                    LocalDateTime.parse(satz.value1(), STUNDENFORMAT),
                    satz.value2(),
                    satz.value3(),
                    satz.value4()));
  }

  /**
   * Der fruehester Zeitstempel des Bestands — die untere Grenze des Volllaufs und der Rueckfall,
   * wenn {@code rollup_lauf} leer ist.
   *
   * <p><b>Zugriffspfad und Laufzeit (Regel L7):</b> {@code Select tables optimized away} — der Wert
   * kommt aus dem ersten Blatt von {@code MessageLastUpdateIDX}, ohne einen einzigen
   * Tabellenzugriff. {@code docs/datenzugriff.md} §12 misst die Gegenrichtung ({@code MAX}) mit
   * rund 7 ms; die Messung dieser Fassung steht in {@code docs/rollup.md}.
   *
   * <p><b>Ohne Zeitfenster, und das ist hier gedeckt</b> (Regel L9): Gefragt ist der Anfang des
   * Bestands, und ein Zeitfenster schnitte genau die Zeile weg, um die es geht. Es ist ausserdem
   * keine Aggregation ueber Zeilen, sondern eine Indexspitze — dieselbe Form, die die Dev-Uhr seit
   * Schritt 2 beim Start liest.
   *
   * @return leer, wenn {@code Message} leer ist. Dann gibt es nichts zu rechnen
   */
  public Optional<LocalDateTime> fruehesteAenderung() {
    return Optional.ofNullable(
        glassfishDsl
            .select(DSL.min(MESSAGE.MESSAGELASTUPDATE))
            .from(MESSAGE)
            .fetchOne(0, LocalDateTime.class));
  }
}
