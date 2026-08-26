package de.kraftwerkone.overlord.monitor.rollup;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.ROLLUP_LAUF;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Der <b>schreibende</b> Datenzugriff des Rollups — ausschliesslich auf {@code overlord_monitor},
 * ausschliesslich ueber den Schreib-Kontext {@code monitorDsl}.
 *
 * <h2>Warum das eine zweite Klasse neben {@link RollupLeseRepository} ist</h2>
 *
 * <p>Der Katalog aus Schritt 9b haelt beide Kontexte in <b>einer</b> Repository-Klasse, und das ist
 * dort richtig. Hier nicht: {@link RollupLeseRepository} traegt die <b>namentliche Ausnahme</b> von
 * Regel M2 — es liest bewusst ueber alle Mandanten. Laegen beide Haelften in einer Klasse, fiele
 * <b>auch der Schreibpfad</b> unter diese Ausnahme, und die Ausnahme waere breiter als ihr Grund.
 * Die Trennung haelt sie so eng wie moeglich: Diese Klasse fasst {@code jooq.glassfish} nie an und
 * steht deshalb gar nicht erst zur Debatte.
 *
 * <p><b>Kein {@code MandantContext} — und hier ist das keine Ausnahme, sondern gegenstandslos.</b>
 * {@code message_rollup} und {@code rollup_lauf} tragen keine Mandantenspalte (Entscheidung E-a),
 * und Regel M2 greift nur fuer Methoden, die das Quellschema anfassen. Die Mandantentrennung
 * entsteht in 10b beim Join ueber {@code ProjectMandant}.
 *
 * <h2>Transaktionen</h2>
 *
 * <p>Die Methoden dieser Klasse laufen bewusst <b>ausserhalb</b> einer Transaktion, mit einer
 * benannten Ausnahme (siehe {@code RollupJob}, Schritt 3). Insbesondere muss {@link #starteLauf}
 * <b>sofort sichtbar</b> sein: Sie ist die Sperre, an der ein zweiter Lauf erkennt, dass bereits
 * einer laeuft. Eine Zeile, die erst am Ende der Fachtransaktion sichtbar wuerde, sperrte nichts.
 */
@Repository
public class RollupSchreibRepository {

  /**
   * Wie alt eine unfertige Laufzeile werden darf, bevor sie nicht mehr als „laeuft gerade" zaehlt.
   *
   * <p><b>Eine Stunde, weil das der Takt des Delta-Laufs ist.</b> Ohne diese Grenze sperrte ein
   * einziger abgestuerzter Lauf — der seine Zeile ohne {@code beendet_am} hinterlaesst — den Job
   * dauerhaft, und zwar lautlos: Jeder folgende Lauf wuerde uebersprungen, das Dashboard veraltete,
   * und im Protokoll stuende nichts als „uebersprungen". Nach einer Stunde laeuft der Job wieder
   * an; die alte Zeile bleibt als Beleg des Abbruchs stehen und hebt den Wasserstand nicht.
   *
   * <p><b>Belegvermerk (Regel L10).</b> <i>Gemessen ist:</i> der Delta-Lauf kostet in der
   * dichtesten Stunde des Bestands 88,167 ms und der Volllauf ueber den Gesamtbestand 51,242 s
   * (M88, M89) — beides weit unter einer Stunde. <i>Behauptet wird:</i> eine Stunde ist lang genug,
   * dass kein laufender Lauf faelschlich fuer abgestuerzt gehalten wird. Die Luecke: Auf der
   * Testkopie dauert ein {@code COMMIT} zwischen 10 und 25 Sekunden; wie lange er in Produktion
   * dauert, ist nicht gemessen.
   */
  static final Duration LAUFT_NOCH_HOECHSTENS = Duration.ofHours(1);

  /** Wie viele Zeilen ein Einfuegestapel traegt. Begruendung an {@link #fuegeEin}. */
  static final int STAPELGROESSE = 1_000;

  private final DSLContext monitorDsl;

  RollupSchreibRepository(@Qualifier("monitorDsl") DSLContext monitorDsl) {
    this.monitorDsl = monitorDsl;
  }

  /**
   * <b>Ersetzt alle Rollup-Zeilen im Fenster: erst loeschen, dann einfuegen — in genau einer
   * Transaktion.</b>
   *
   * <h2>Warum geloescht und neu geschrieben wird und niemals {@code anzahl = anzahl + n}</h2>
   *
   * <p>Zwei Gruende, und beide waeren stille Fehler:
   *
   * <ol>
   *   <li><b>Der 15-Minuten-Rueckgriff ueberlappt mit dem letzten Lauf.</b> Hochzaehlen verdoppelte
   *       den Ueberlapp — und zwar bei jedem Lauf aufs Neue, sodass der Fehler waechst statt
   *       aufzufallen.
   *   <li><b>Wechselt eine Nachricht innerhalb desselben Eimers den Status</b>, verschwindet die
   *       alte Statuszeile nicht von selbst. Ein {@code INSERT … ON DUPLICATE KEY UPDATE} schriebe
   *       die neue Zeile und liesse die alte mit ihrem alten Zaehlstand stehen. Die Summe waere zu
   *       hoch, und die Verteilung nach Status waere falsch — beides ohne jede Fehlermeldung.
   * </ol>
   *
   * <h2>Eine Transaktion, und beim Volllauf ausdruecklich eine grosse</h2>
   *
   * <p>{@code @Transactional} bindet den Schreib-Kontext ({@code docs/datenzugriff.md} §1) — hier
   * ist das genau richtig. Beim Volllauf umfasst sie <b>335.610 Zeilen und 21,6 MiB</b> (M89). Das
   * ist gewollt: <b>Eine halb geleerte Rolluptabelle um 03:00 waere ein leeres Dashboard.</b>
   *
   * <p><b>Das Lesen liegt bewusst NICHT in dieser Transaktion.</b> Es laeuft ueber den Lese-Pool
   * und damit auf einer anderen Verbindung — es waere ohnehin nicht Teil von ihr (§2). Der Aufrufer
   * liest deshalb erst vollstaendig und ruft danach diese Methode.
   *
   * @param fenster der Bereich, der ersetzt wird — {@code stunde >= von} und {@code stunde < bis}
   * @param zeilen die neuen Zeilen. Sie muessen vollstaendig in das Fenster fallen; sonst
   *     entstuenden Zeilen, die der naechste Lauf nicht mehr loeschen kann
   * @return die Zahl der eingefuegten Zeilen
   */
  @Transactional
  public int ersetzeFenster(RollupFenster fenster, List<RollupZeile> zeilen) {
    monitorDsl
        .deleteFrom(MESSAGE_ROLLUP)
        .where(MESSAGE_ROLLUP.STUNDE.ge(fenster.von()))
        .and(MESSAGE_ROLLUP.STUNDE.lt(fenster.bis()))
        .execute();
    return fuegeEin(zeilen);
  }

  /**
   * Fuegt die Zeilen stapelweise ein.
   *
   * <p><b>{@value #STAPELGROESSE} Zeilen je Stapel.</b> Ein einziger Stapel ueber 335.610 Zeilen
   * haelt 1,3 Millionen Bindewerte gleichzeitig im Speicher, ohne dafuer schneller zu sein; ein
   * Stapel je Zeile zahlt 335.610-mal die Netzwerkrunde. Die Groesse ist eine Groessenordnung und
   * kein gemessenes Optimum — <b>gemessen ist der Lauf als Ganzes</b>, und der steht in {@code
   * docs/rollup.md} neben den 51,242 s aus M89.
   *
   * <p>Die Vorlage traegt je Spalte einen Bindeplatz; gebunden wird unten in genau dieser
   * Reihenfolge. Dasselbe Muster wie in {@code ProzessKatalogRepository.speichereBestandsflags}.
   */
  private int fuegeEin(List<RollupZeile> zeilen) {
    int eingefuegt = 0;
    for (int anfang = 0; anfang < zeilen.size(); anfang += STAPELGROESSE) {
      List<RollupZeile> stapel =
          zeilen.subList(anfang, Math.min(anfang + STAPELGROESSE, zeilen.size()));
      BatchBindStep bindung =
          monitorDsl.batch(
              monitorDsl
                  .insertInto(MESSAGE_ROLLUP)
                  .set(MESSAGE_ROLLUP.STUNDE, (LocalDateTime) null)
                  .set(MESSAGE_ROLLUP.PROCESS_ID, (String) null)
                  .set(MESSAGE_ROLLUP.MESSAGE_STATUS, (String) null)
                  .set(MESSAGE_ROLLUP.ANZAHL, (Integer) null));
      for (RollupZeile zeile : stapel) {
        bindung =
            bindung.bind(zeile.stunde(), zeile.processId(), zeile.messageStatus(), zeile.anzahl());
      }
      for (int betroffen : bindung.execute()) {
        if (betroffen >= 0) {
          eingefuegt++;
        }
      }
    }
    return eingefuegt;
  }

  /**
   * {@code SUM(anzahl)} ueber das Fenster — die Groesse der Summenprobe.
   *
   * <p>Ueber den Gesamtbestand muss sie <b>3.341.519</b> ergeben, also die gezaehlte Zeilenzahl von
   * {@code Message} (M0, bestaetigt in M89). Weicht sie ab, ist das ein Befund und kein
   * Rundungsfehler: Es hiesse, dass eine Nachricht doppelt oder gar nicht gezaehlt wird.
   */
  public long summiereAnzahl(RollupFenster fenster) {
    Long summe =
        monitorDsl
            .select(DSL.sum(MESSAGE_ROLLUP.ANZAHL))
            .from(MESSAGE_ROLLUP)
            .where(MESSAGE_ROLLUP.STUNDE.ge(fenster.von()))
            .and(MESSAGE_ROLLUP.STUNDE.lt(fenster.bis()))
            .fetchOne(0, Long.class);
    return summe == null ? 0L : summe;
  }

  /**
   * Der <b>Wasserstand</b>: bis wohin ist gerechnet?
   *
   * <p>{@code MAX(fenster_bis)} ueber alle Laeufe, die <b>abgeschlossen</b> ({@code beendet_am IS
   * NOT NULL}) und <b>fehlerfrei</b> ({@code fehler IS NULL}) sind.
   *
   * <p><b>Warum das nicht {@code MAX(stunde)} aus {@code message_rollup} ist.</b> Eine Stunde ohne
   * Verkehr erzeugt <b>keine</b> Rollup-Zeile. {@code MAX(stunde)} sagt darum „letzte Stunde mit
   * Verkehr" und nicht „bis hierhin ist gerechnet" — der Unterschied zwischen <i>nichts
   * passiert</i> und <i>noch nicht nachgesehen</i>. Auf der Testkopie ist das keine Feinheit: Fuenf
   * zusammenhaengende Monate tragen null Zeilen (M92). Ein Wasserstand aus {@code MAX(stunde)}
   * bliebe dort auf dem 30.12.2025 stehen und liesse den Job jede Nacht dieselben fuenf Monate neu
   * rechnen.
   *
   * <p><b>Ein abgebrochener Lauf hebt den Wasserstand nicht.</b> Genau dafuer steht {@code
   * beendet_am IS NULL} — sonst uebersprungen der naechste Lauf einen Bereich, der nur zur Haelfte
   * geschrieben ist, und die Luecke faende niemand mehr.
   *
   * @return leer, wenn noch nie gerechnet wurde. Der Aufrufer faellt dann auf {@code
   *     MIN(Message.MessageLastUpdate)} zurueck
   */
  public Optional<LocalDateTime> wasserstand() {
    return Optional.ofNullable(
        monitorDsl
            .select(DSL.max(ROLLUP_LAUF.FENSTER_BIS))
            .from(ROLLUP_LAUF)
            .where(ROLLUP_LAUF.BEENDET_AM.isNotNull())
            .and(ROLLUP_LAUF.FEHLER.isNull())
            .fetchOne(0, LocalDateTime.class));
  }

  /**
   * Laeuft gerade schon einer? Eine Zeile ohne {@code beendet_am}, deren {@code gestartet_am}
   * juenger ist als {@link #LAUFT_NOCH_HOECHSTENS}.
   *
   * <p>Das ist eine <b>Absprache</b> und keine Datenbanksperre. Sie genuegt fuer den Zweck: Der Job
   * laeuft in einer Instanz, und die beiden Ausloeser (stuendlich, naechtlich) koennen sich nur
   * dann ueberholen, wenn ein Lauf ungewoehnlich lange braucht. Zwei gleichzeitige Laeufe waeren
   * fachlich nicht einmal falsch — sie loeschen und schreiben dieselben Eimer —, aber sie
   * verdoppelten die Last auf einer Instanz, die sich mit der Produktion teilt (Leistungsregel L6).
   *
   * @param protokollzeitUtc der Referenzzeitpunkt aus {@code systemClock}, nicht aus der
   *     Anwendungsuhr: verglichen wird gegen {@code gestartet_am}, und das ist Protokollzeit
   */
  public boolean laeuftBereits(LocalDateTime protokollzeitUtc) {
    return monitorDsl.fetchExists(
        monitorDsl
            .selectOne()
            .from(ROLLUP_LAUF)
            .where(ROLLUP_LAUF.BEENDET_AM.isNull())
            .and(ROLLUP_LAUF.GESTARTET_AM.gt(protokollzeitUtc.minus(LAUFT_NOCH_HOECHSTENS))));
  }

  /**
   * Legt die Protokollzeile an, <b>bevor</b> gerechnet wird. {@code beendet_am} bleibt {@code
   * NULL}.
   *
   * <p>Die Reihenfolge ist der Punkt: Stuerzt die Anwendung mitten im Lauf ab, steht die Zeile
   * trotzdem da — ohne {@code beendet_am}, damit sie den Wasserstand nicht hebt, und mit ihrem
   * Fenster, damit ablesbar ist, woran der Lauf war.
   *
   * @param gestartetAmUtc Protokollzeit aus {@code systemClock} (siehe {@link RollupUhren})
   * @return die vergebene Kennung der Laufzeile
   */
  public long starteLauf(LaufArt art, RollupFenster fenster, LocalDateTime gestartetAmUtc) {
    Long id =
        monitorDsl
            .insertInto(ROLLUP_LAUF)
            .set(ROLLUP_LAUF.ART, art.name())
            .set(ROLLUP_LAUF.FENSTER_VON, fenster.von())
            .set(ROLLUP_LAUF.FENSTER_BIS, fenster.bis())
            .set(ROLLUP_LAUF.GESTARTET_AM, gestartetAmUtc)
            .returningResult(ROLLUP_LAUF.ID)
            .fetchOne(0, Long.class);
    if (id == null) {
      throw new IllegalStateException(
          "Die Laufzeile ist angelegt worden, aber ihre Kennung ist nicht zurueckgekommen."
              + " Ohne sie liesse sich der Lauf nicht abschliessen.");
    }
    return id;
  }

  /**
   * Schliesst die Protokollzeile ab. Erst danach zaehlt ihr Fenster zum Wasserstand.
   *
   * @param beendetAmUtc Protokollzeit aus {@code systemClock}
   * @param zeilenGeschrieben wie viele Rollup-Zeilen der Lauf geschrieben hat — nicht, wie viele
   *     Nachrichten er gelesen hat
   */
  public void beendeLauf(long laufId, LocalDateTime beendetAmUtc, int zeilenGeschrieben) {
    monitorDsl
        .update(ROLLUP_LAUF)
        .set(ROLLUP_LAUF.BEENDET_AM, beendetAmUtc)
        .set(ROLLUP_LAUF.ZEILEN_GESCHRIEBEN, zeilenGeschrieben)
        .where(ROLLUP_LAUF.ID.eq(laufId))
        .execute();
  }

  /**
   * Vermerkt einen gescheiterten Lauf: {@code fehler} wird gefuellt, {@code beendet_am} gesetzt.
   *
   * <p><b>Beides zusammen, und trotzdem zaehlt die Zeile nicht zum Wasserstand</b> — der fragt nach
   * {@code beendet_am IS NOT NULL} <b>und</b> {@code fehler IS NULL}. Das ist der Unterschied
   * zwischen „abgeschlossen" und „verlaesslich gerechnet". {@code beendet_am} wird gesetzt, damit
   * die Zeile nicht als „laeuft gerade" den naechsten Lauf sperrt.
   *
   * <p>Die Meldung wird auf die Spaltenbreite eines {@code TEXT} nicht geprueft: 65.535 Byte fassen
   * jede Datenbankmeldung.
   */
  public void vermerkeFehler(long laufId, LocalDateTime beendetAmUtc, String fehler) {
    monitorDsl
        .update(ROLLUP_LAUF)
        .set(ROLLUP_LAUF.BEENDET_AM, beendetAmUtc)
        .set(ROLLUP_LAUF.FEHLER, fehler)
        .where(ROLLUP_LAUF.ID.eq(laufId))
        .execute();
  }
}
