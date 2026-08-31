package de.kraftwerkone.overlord.monitor.rollup;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <b>Der Rollup-Lauf. Eine Methode fuer beide Laufarten.</b>
 *
 * <p>Delta-Lauf und Volllauf rufen {@link #fuehreAus} mit verschiedenen Fenstern auf und laufen
 * danach durch <b>denselben</b> Code und <b>dieselbe</b> Abfrage.
 *
 * <p><b>Zwei getrennte Abfragen zu schreiben ist ausdruecklich verboten.</b> Sie liefen
 * auseinander, und der Nachtlauf aenderte dann still die Zahlen, die tagsueber jemand gesehen hat.
 * Genau deshalb ist der Volllauf hier kein eigener Zweig, sondern nur ein anderes {@link
 * RollupFenster}.
 *
 * <h2>Der Ablauf, und warum er in dieser Reihenfolge steht</h2>
 *
 * <ol>
 *   <li><b>Protokollzeile anlegen</b> ({@code gestartet_am}, {@code art}, Fenster), {@code
 *       beendet_am} bleibt {@code NULL}. <b>Vor</b> dem Rechnen, damit ein Absturz eine Spur
 *       hinterlaesst — und damit ein zweiter Lauf die Sperre sieht.
 *   <li><b>Aggregation ueber den Lese-Pool holen</b>, scheibenweise (siehe {@link
 *       RollupFenster#monatsscheiben()}). Das liegt <b>ausserhalb</b> jeder Transaktion: Der
 *       Lesezugriff laeuft auf einer anderen Verbindung und waere ohnehin nicht Teil von ihr
 *       ({@code docs/datenzugriff.md} §2).
 *   <li><b>Loeschen und Einfuegen in genau einer Transaktion</b> ({@link
 *       RollupSchreibRepository#ersetzeFenster}).
 *   <li><b>Die Tageseimer der beruehrten Kalendertage neu rechnen</b> — <b>in derselben
 *       Transaktion</b> und aus der eben geschriebenen Stundenebene, nicht aus {@code Message}.
 *       Seit Schritt 10b-1; die Begruendung steht an {@code
 *       RollupSchreibRepository.rechneTageEbeneNeu}.
 *   <li><b>Die Monatseimer der beruehrten Kalendermonate neu rechnen</b> — ebenfalls in derselben
 *       Transaktion und aus der eben geschriebenen <b>Tagesebene</b>. Seit Schritt 10b-2. <b>Die
 *       Reihenfolge ist der Punkt:</b> Jede Ebene entsteht aus der naechstfeineren, nachdem diese
 *       geschrieben ist. Stuende sie davor, truege sie den Stand von vor diesem Lauf — und der
 *       Fehler waere still.
 *   <li><b>{@code beendet_am} und {@code zeilen_geschrieben} nachtragen.</b> Erst danach zaehlt das
 *       Fenster zum Wasserstand.
 *   <li><b>Beim Volllauf: den Bestandsanfang pruefen</b> — <i>neu am 31.08.2026</i>, {@link
 *       #meldeEingefroreneEimer}. Stehen Rollup-Eimer unterhalb von {@code
 *       MIN(Message.MessageLastUpdate)}, erreicht sie kein Lauf mehr; das wird <b>gemeldet und
 *       nicht geloescht</b> (offener Punkt 54). Der Schritt steht <b>nach</b> {@code beendet_am},
 *       weil ein Befund erst dann einer ist: Was jetzt noch unterhalb liegt, hat dieser Lauf
 *       nachweislich nicht angefasst. Er ist ausserdem ausdruecklich <b>ausserhalb</b> der
 *       Transaktion aus Schritt 3 bis 5 — eine Diagnose darf einen erfolgreichen Lauf nicht
 *       zuruecknehmen.
 * </ol>
 *
 * <p>Bei einer Ausnahme wird {@code fehler} gefuellt, {@code beendet_am} gesetzt und die Ausnahme
 * <b>weitergereicht</b>. Eine Laufzeile ohne {@code beendet_am} bedeutet <b>abgebrochen</b> und
 * zaehlt nicht zum Wasserstand; eine mit {@code fehler} ebenfalls nicht.
 *
 * <h2>Was dieser Job nicht tut</h2>
 *
 * <p>Er prueft <b>nicht</b>, ob bereits ein Lauf laeuft — das tut der Ausloeser ({@code
 * RollupPlaner}), weil die Entscheidung „ueberspringen" eine des Zeitplans ist und keine des Laufs.
 * Wer {@link #fuehreAus} von Hand aufruft, bekommt einen Lauf.
 */
@Service
public class RollupJob {

  private static final Logger log = LoggerFactory.getLogger(RollupJob.class);

  /**
   * Wie viele Zeichen einer Fehlermeldung in {@code rollup_lauf.fehler} landen.
   *
   * <p>Die Spalte ist {@code TEXT} und fasst 65.535 Byte. Abgeschnitten wird trotzdem: Eine
   * Meldung, die laenger ist, ist keine Meldung mehr, und ein Einfuegefehler beim Protokollieren
   * eines Fehlers verschluckte den urspruenglichen.
   */
  private static final int FEHLER_HOECHSTENS = 8_000;

  private final RollupLeseRepository leseRepository;
  private final RollupSchreibRepository schreibRepository;
  private final RollupUhren uhren;
  private final RollupEigenschaften eigenschaften;

  RollupJob(
      RollupLeseRepository leseRepository,
      RollupSchreibRepository schreibRepository,
      RollupUhren uhren,
      RollupEigenschaften eigenschaften) {
    this.leseRepository = leseRepository;
    this.schreibRepository = schreibRepository;
    this.uhren = uhren;
    this.eigenschaften = eigenschaften;
  }

  /**
   * Ermittelt das Fenster einer Laufart aus dem Wasserstand beziehungsweise dem Bestandsanfang und
   * der <b>Anwendungsuhr</b>.
   *
   * <p><b>Delta:</b> Wasserstand {@code W} = {@code MAX(fenster_bis)} ueber die abgeschlossenen,
   * fehlerfreien Laeufe. Ist {@code rollup_lauf} leer, ist {@code W} = {@code
   * MIN(Message.MessageLastUpdate)} — der erste Delta-Lauf holt damit den ganzen Bestand nach, ohne
   * dass jemand einen Volllauf ansteuern muesste.
   *
   * <p><b>Voll:</b> immer vom fruehesten {@code MessageLastUpdate} bis jetzt.
   *
   * <p>Ist {@code Message} leer, gibt es nichts zu rechnen. Das Fenster ist dann die laufende
   * Stunde ab der Anwendungsuhr — beim Volllauf genau ein Eimer, beim Delta-Lauf in den ersten
   * {@value RollupFenster#NACHLAUF_MINUTEN} Minuten einer Stunde zwei, weil der Rueckgriff dort in
   * die vorige greift. Beide sind leer; der Unterschied kostet nichts und steht hier nur, damit die
   * Beschreibung stimmt.
   */
  public RollupFenster ermittleFenster(LaufArt art) {
    LocalDateTime jetzt = uhren.datenzeit();
    if (art == LaufArt.VOLL) {
      return leseRepository
          .fruehesteAenderung()
          .map(fruehest -> RollupFenster.voll(fruehest, jetzt))
          .orElseGet(() -> RollupFenster.voll(jetzt, jetzt));
    }
    LocalDateTime wasserstand =
        schreibRepository
            .wasserstand()
            .orElseGet(() -> leseRepository.fruehesteAenderung().orElse(jetzt));
    return RollupFenster.delta(wasserstand, jetzt);
  }

  /**
   * Fuehrt einen Lauf ueber das angegebene Fenster aus.
   *
   * @param von untere Grenze, einschliesslich — ein voller Stundenanfang
   * @param bis obere Grenze, ausschliesslich — ein voller Stundenanfang
   * @param art nur fuer das Protokoll; sie aendert am Ablauf nichts
   * @throws IllegalArgumentException wenn die Grenzen nicht auf ganzen Stunden liegen oder das
   *     Fenster rueckwaerts laeuft
   */
  public RollupErgebnis fuehreAus(LocalDateTime von, LocalDateTime bis, LaufArt art) {
    RollupFenster fenster = new RollupFenster(von, bis);
    List<RollupFenster> scheiben = fenster.monatsscheiben();
    LocalDateTime gestartetAm = uhren.protokollzeit();
    long laufId = schreibRepository.starteLauf(art, fenster, gestartetAm);

    log.info(
        "Rollup-Lauf {} (Nr. {}) gestartet: {} in {} Scheibe(n), Protokollzeit {}",
        art,
        laufId,
        fenster,
        scheiben.size(),
        gestartetAm);

    try {
      List<RollupZeile> zeilen = lies(scheiben);
      RollupZeilenzahlen geschrieben = schreibRepository.ersetzeFenster(fenster, zeilen);
      long nachrichten = zeilen.stream().mapToLong(RollupZeile::anzahl).sum();
      LocalDateTime beendetAm = uhren.protokollzeit();
      schreibRepository.beendeLauf(laufId, beendetAm, geschrieben.stundenzeilen());
      meldeEingefroreneEimer(art, laufId);

      Duration dauer = Duration.between(gestartetAm, beendetAm);
      log.info(
          "Rollup-Lauf {} (Nr. {}) fertig: {} Stundenzeilen, {} Tageszeilen und {} Monatszeilen"
              + " fuer {} Nachrichten in {} ms",
          art,
          laufId,
          geschrieben.stundenzeilen(),
          geschrieben.tageszeilen(),
          geschrieben.monatszeilen(),
          nachrichten,
          dauer.toMillis());
      return new RollupErgebnis(
          laufId,
          art,
          fenster,
          scheiben.size(),
          geschrieben.stundenzeilen(),
          geschrieben.tageszeilen(),
          geschrieben.monatszeilen(),
          nachrichten,
          dauer);
    } catch (RuntimeException fehler) {
      vermerke(laufId, art, fehler);
      throw fehler;
    }
  }

  /**
   * <b>Die Erkennung zu offenem Punkt 54</b>, entschieden am 27.08.2026 und gebaut am 31.08.2026.
   *
   * <p>Der Volllauf vergleicht {@code MIN(message_rollup.stunde)} mit {@code
   * MIN(Message.MessageLastUpdate)}. Liegt der Bestandsanfang <b>spaeter</b>, stehen Rollup-Eimer
   * unterhalb von ihm, die kein Lauf je wieder anfasst — die untere Grenze beider Laufarten kommt
   * aus dem Quellbestand, und geloescht wird ausschliesslich innerhalb des Fensters.
   *
   * <p><b>Gemeldet und nicht geloescht.</b> Die Begruendung steht an {@link EingefroreneEimer};
   * kurz: Der Rollup ist dann die einzige Stelle, an der die Zahlen jenes Zeitraums noch stehen,
   * und Nichtloeschen ist umkehrbar. <b>Der Schaden aus Punkt 54 ist nicht das Dastehen der Zeilen,
   * sondern dass es niemand merkt</b> — genau das behebt diese Zeile.
   *
   * <h2>Nur der Volllauf, und das ist keine Nachlaessigkeit</h2>
   *
   * <p>Der Delta-Lauf beginnt beim Wasserstand und damit noch spaeter; er saehe denselben Befund
   * und meldete ihn stuendlich. <b>Eine WARN-Zeile, die jede Stunde kommt, wird nach dem zweiten
   * Tag nicht mehr gelesen.</b> Der naechtliche Volllauf meldet sie einmal je Nacht, und das ist
   * die Frequenz, in der ein Bestandsanfang wandert.
   *
   * <h2>Was sie im Normalbetrieb kostet</h2>
   *
   * <p><b>Zwei Indexspitzen.</b> {@code MIN(Message.MessageLastUpdate)} ist {@code Select tables
   * optimized away} (0,272 ms), {@code MIN/MAX(stunde)} laeuft ueber den Primaerschluessel. Die
   * drei Zaehlungen laufen <b>nur</b>, wenn der Fall eingetreten ist.
   */
  private void meldeEingefroreneEimer(LaufArt art, long laufId) {
    if (art != LaufArt.VOLL) {
      return;
    }
    EingefroreneEimer eingefroren = eingefroreneEimer();
    if (!eingefroren.vorhanden()) {
      return;
    }
    log.warn(
        "Rollup-Lauf {} (Nr. {}): {} Stunden-, {} Tages- und {} Monatseimer stehen unterhalb des"
            + " Bestandsanfangs {} und werden von keinem Lauf mehr erreicht. Sie bleiben"
            + " absichtlich stehen (offener Punkt 54) — aber die Summenprobe gilt ab hier nur noch"
            + " ueber den ueberlappenden Bereich, nicht ueber die ganze Tabelle.",
        art,
        laufId,
        eingefroren.stundeneimer(),
        eingefroren.tageseimer(),
        eingefroren.monatseimer(),
        leseRepository.fruehesteAenderung().orElse(null));
  }

  /**
   * Der Befund zu Punkt 54, ohne Protokollzeile — dieselbe Rechnung, die {@link
   * #meldeEingefroreneEimer} meldet.
   *
   * <p>Paketprivat und nicht oeffentlich: Sie ist die pruefbare Ursache hinter der WARN-Zeile und
   * kein Dienst, den jemand von aussen aufruefen soll. Wer den Zustand anzeigen will, bekommt ihn
   * ueber das Protokoll — eine Spalte in {@code rollup_lauf} kommt, wenn das Frontend sie braucht,
   * und nicht auf Verdacht.
   *
   * @return {@link EingefroreneEimer#KEINE}, wenn die Quelle oder die Stundenebene leer ist oder
   *     der Rollup nirgends vor dem Bestandsanfang beginnt
   */
  EingefroreneEimer eingefroreneEimer() {
    Optional<LocalDateTime> bestandsanfang = leseRepository.fruehesteAenderung();
    Optional<LocalDateTime> ersteStunde =
        schreibRepository.bereichDerStundenebene().map(RollupFenster::von);
    if (bestandsanfang.isEmpty() || ersteStunde.isEmpty()) {
      return EingefroreneEimer.KEINE;
    }
    // Abgerundet vergleichen: MIN(MessageLastUpdate) ist sekundengenau (2024-10-01T02:00:28),
    // `stunde` immer ein Stundenanfang. Ohne das Abrunden meldete jeder einzelne Lauf einen
    // Fehlbefund, weil 02:00 vor 02:00:28 liegt.
    if (!ersteStunde.get().isBefore(bestandsanfang.get().truncatedTo(ChronoUnit.HOURS))) {
      return EingefroreneEimer.KEINE;
    }
    return schreibRepository.zaehleEingefroreneEimer(bestandsanfang.get());
  }

  /**
   * Liest die Scheiben nacheinander und sammelt sie im Speicher.
   *
   * <p><b>Warum in den Speicher und nicht per {@code INSERT … SELECT} ueber die Schemagrenze.</b>
   * M89 hat die Probetabelle genau so gefuellt und dafuer 51,242 s gebraucht. Dieser Bau macht es
   * anders, weil {@code PROJEKTBESCHREIBUNG.md} §6 sagt: <i>„Der Schreib-DSLContext darf
   * ausschliesslich {@code overlord_monitor}."</i> Ein {@code INSERT … SELECT} aus {@code
   * GlassfishDB} ueber den Schreib-Pool widerspricht dem Satz, auch wenn {@code monitor_write} dort
   * {@code SELECT} besitzt und die Schreibgarantie unberuehrt bliebe. Der gebaute Weg ist gegen die
   * 51,242 s gemessen; das Ergebnis steht in {@code docs/rollup.md}.
   *
   * <p>Beim Volllauf sind das <b>335.610 Zeilen</b> auf einmal (M89). <b>Gerechnet, nicht
   * gemessen:</b> Eine {@link RollupZeile} traegt neben dem Datensatzkopf einen {@code
   * LocalDateTime} (drei Objekte) und zwei {@code String} — rund 220 Byte, also etwa <b>70 MiB</b>
   * fuer den Gesamtbestand der Testkopie, im Spitzenwert mehr, weil die Scheibenliste und die
   * Gesamtliste kurz nebeneinanderstehen. Das ist die Kehrseite der einen Transaktion aus Schritt 3
   * und ausdruecklich so gewollt; die Zahl steht als offener Punkt in {@code docs/rollup.md}.
   */
  private List<RollupZeile> lies(List<RollupFenster> scheiben) {
    List<RollupZeile> zeilen = new ArrayList<>();
    for (int i = 0; i < scheiben.size(); i++) {
      if (i > 0) {
        drossle();
      }
      RollupFenster scheibe = scheiben.get(i);
      List<RollupZeile> gelesen = leseRepository.aggregiere(scheibe.von(), scheibe.bis());
      log.debug("Rollup-Scheibe {} liefert {} Zeilen", scheibe, gelesen.size());
      zeilen.addAll(gelesen);
    }
    return zeilen;
  }

  /**
   * <b>Leistungsregel L6, an der einen Stelle, an der sie greift.</b> „Der Rollup-Job laeuft
   * gedrosselt. Er teilt sich die Instanz mit der Produktion."
   *
   * <p>Gewartet wird <b>zwischen</b> zwei Scheiben und nicht vor der ersten oder nach der letzten.
   * Damit wartet ein Delta-Lauf so gut wie nie — er hat in aller Regel genau eine Scheibe. <b>Die
   * Ausnahme ist der Monatswechsel</b>: Faellt ein Delta-Fenster ueber den Monatsersten, schneidet
   * der Kalender es in zwei, und der Lauf wartet einmal. Das trifft einen Lauf im Monat und kostet
   * dort eine Sekunde.
   *
   * <p>Das ist kein Zufall, sondern der Punkt: Beim Delta-Lauf ist L6 nach M88 Zeremonie statt
   * Schutz (88 ms fuer die dichteste Stunde von rund 15.000, also 0,0024 % Instanzbelegung), beim
   * Volllauf ueber gut zwanzig Scheiben ist sie es nicht.
   *
   * <p>Die Dauer ist konfigurierbar und <b>ungemessen</b> — siehe {@link
   * RollupEigenschaften#scheibenPause()}.
   */
  private void drossle() {
    long millis = eigenschaften.scheibenPause().toMillis();
    if (millis <= 0) {
      return;
    }
    try {
      Thread.sleep(millis);
    } catch (InterruptedException unterbrochen) {
      // Die Unterbrechung gehoert weitergereicht und nicht verschluckt: Beim Herunterfahren soll
      // der Lauf abbrechen und nicht die Drosselung zu Ende warten.
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Rollup-Lauf waehrend der Drosselung unterbrochen", unterbrochen);
    }
  }

  /**
   * Traegt den Fehler in die Laufzeile nach.
   *
   * <p>Schlaegt <b>das</b> fehl, wird es der urspruenglichen Ausnahme angehaengt und nicht an ihre
   * Stelle gesetzt: Ein Protokollierfehler, der den eigentlichen Fehler verschluckt, ist die
   * schlimmste Art von Protokoll.
   */
  private void vermerke(long laufId, LaufArt art, RuntimeException fehler) {
    String meldung = fehler.getClass().getSimpleName() + ": " + fehler.getMessage();
    if (meldung.length() > FEHLER_HOECHSTENS) {
      meldung = meldung.substring(0, FEHLER_HOECHSTENS);
    }
    log.error("Rollup-Lauf {} (Nr. {}) abgebrochen: {}", art, laufId, meldung, fehler);
    try {
      schreibRepository.vermerkeFehler(laufId, uhren.protokollzeit(), meldung);
    } catch (RuntimeException beimVermerken) {
      fehler.addSuppressed(beimVermerken);
    }
  }
}
