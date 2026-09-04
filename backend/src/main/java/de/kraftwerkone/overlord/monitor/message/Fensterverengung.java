package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verengt das Zeitfenster der Nachrichtenliste vorab ueber {@code message_rollup}.
 *
 * <h2>Der Gedanke</h2>
 *
 * <p>Der Rollup weiss, in welchen Stunden ein Mandant Nachrichten hat. Fragt man die Quelle nur
 * nach diesen Stunden, waehlt der Optimierer die zeitgetriebene Form <b>von selbst</b> — ohne Hint,
 * ohne {@code STRAIGHT_JOIN}, ohne dass ein Index auf {@code GlassfishDB} angefasst wuerde.
 * Gemessen am 27.08.2026 ({@code docs/nachrichtenliste.md} §5c): Der teuerste bekannte Fall des
 * Projekts, der Prozessfilter mit verkehrsreichen Prozessen, faellt von <b>7.519,513 ms auf 6,027
 * ms</b>. Und der Optimierer <b>bleibt</b> prozessgetrieben, wo das richtig ist ({@code WOC}
 * behaelt seinen Plan, obwohl sein Fenster von 720 auf 452 Stunden schrumpft). <b>Ein Fenster zu
 * geben ist etwas anderes, als eine Form zu erzwingen</b> — das ist der Unterschied zu den sechs
 * Fassungen, die §5a gemessen und verworfen hat.
 *
 * <h2>Sie ist eine Optimierung und kein Filter</h2>
 *
 * <p><b>Sie darf nie eine Zeile entfernen, die die unverengte Abfrage geliefert haette.</b> Dafuer
 * sorgen drei Dinge, und alle drei zeigen in dieselbe Richtung: Die gezaehlte Menge ist stets eine
 * <b>Unterschranke</b> der wirklichen.
 *
 * <ol>
 *   <li><b>Nur vollstaendig im Fenster liegende Stunden zaehlen</b> — {@link Verengungsgrenzen}.
 *   <li><b>Nie ueber den Wasserstand hinaus</b> — ebenda. Ohne diese Grenze machte eine
 *       hinterherhinkende Rolluptabelle Nachrichten unsichtbar, und das waere schlimmer als jede
 *       Laufzeit.
 *   <li><b>Nur bei Merkmalen, die der Rollup mittraegt</b> — {@link Abfragemerkmal} und {@link
 *       #traegtDerRollup}.
 * </ol>
 *
 * <h2>⚠️ Warum die dritte Bedingung die wichtigste ist</h2>
 *
 * <p>Gemessen am 27.08.2026 (offener Punkt 72): Eine Verengung, die den <b>Statusfilter</b> nicht
 * mitrechnet, liefert fuer {@code SUTTONS} <b>null statt fuenf</b> Zeilen und fuer {@code NEXANS}
 * <b>49 statt 51</b> — <b>ohne Fehlermeldung</b>. Der Rollup meldet 51 Nachrichten in der letzten
 * Stunde, die Verengung schneidet darauf zu, und in dieser Stunde liegt kein einziger Fehler. Ein
 * Mandant saehe eine leere Liste und schloesse daraus, dass alles in Ordnung ist. <b>In einem
 * Ueberwachungswerkzeug ist „keine Fehler" die schlimmste falsche Antwort, die es gibt.</b>
 *
 * <p>Deshalb ist die Entscheidung <b>umgekehrt</b> gebaut: Sie greift nicht standardmaessig und
 * setzt bei bekannten Ausnahmen aus, sondern greift <b>nur</b>, wenn fuer <b>jedes</b> gesetzte
 * Merkmal ausdruecklich entschieden ist, dass der Rollup es traegt. Alles andere faellt auf den
 * heutigen Pfad zurueck.
 *
 * <h2>Warum sie gestuft fragt</h2>
 *
 * <p>Eine einzige Abfrage ueber das ganze Fenster ist fuer dichte Mandanten zu teuer: {@code
 * NEXANS} zahlt dafuer 91 bis 119 ms, waehrend seine Antwort in der letzten <b>Stunde</b> steht.
 * Gefragt wird deshalb von eng nach weit, und es wird abgebrochen, sobald eine Stufe traegt.
 */
@Component
public class Fensterverengung {

  private static final Logger LOG = LoggerFactory.getLogger(Fensterverengung.class);

  /**
   * Die Stufen, von eng nach weit — danach folgt immer das ganze Fenster.
   *
   * <p>Gemessen in M99 und M104 ({@code docs/messungen-liste-verengung.md}). Eine Stufe bei sieben
   * Tagen ist erwogen und verworfen: Sie traefe nur {@code ZAST} und spart dort gegen die weite
   * Stufe 0,5 ms, kostet aber jeden duennen Mandanten eine zusaetzliche Netzwerkrunde.
   */
  private static final List<Duration> STUFEN = List.of(Duration.ofHours(1), Duration.ofHours(24));

  private final VerengungRepository repository;
  private final NachrichtenlisteEigenschaften eigenschaften;

  Fensterverengung(VerengungRepository repository, NachrichtenlisteEigenschaften eigenschaften) {
    this.repository = repository;
    this.eigenschaften = eigenschaften;
  }

  /**
   * Das Ergebnis der Verengung.
   *
   * @param abfrage die — moeglicherweise verengte — Abfrage. Bei {@link Verengungsgrund#NULLFALL}
   *     unveraendert; sie wird dann gar nicht gestellt.
   * @param grund warum es so ausgegangen ist. Nur daran laesst sich im Test unterscheiden, ob die
   *     Verengung gegriffen hat oder stillschweigend ausgefallen ist — beide liefern dieselben
   *     Zeilen, und genau das ist die Falle.
   */
  public record Ergebnis(Nachrichtenabfrage abfrage, Verengungsgrund grund) {

    /** Ob die Quellabfrage ueberhaupt noch gestellt werden muss. */
    public boolean sicherLeer() {
      return grund == Verengungsgrund.NULLFALL;
    }
  }

  /**
   * Verengt das Fenster der Abfrage, wenn das nachweislich zulaessig ist.
   *
   * <p><b>Jede Seite verengt fuer sich.</b> Es wird nichts ueber den Cursor weitergereicht: Die
   * zweite Seite rechnet gegen den Cursor-Zeitpunkt als Obergrenze neu.
   */
  public Ergebnis verenge(MandantContext mandant, Nachrichtenabfrage abfrage) {
    if (!eigenschaften.verengung()) {
      return new Ergebnis(abfrage, Verengungsgrund.ABGESCHALTET);
    }
    for (Abfragemerkmal merkmal : Abfragemerkmal.values()) {
      if (gesetzt(merkmal, abfrage) && !traegtDerRollup(merkmal, abfrage)) {
        return new Ergebnis(abfrage, Verengungsgrund.MERKMAL_NICHT_TRAGBAR);
      }
    }
    try {
      return frageDenRollup(mandant, abfrage);
    } catch (DataAccessException fehler) {
      // Der Rollup ist eine Beschleunigung und keine Voraussetzung. Faellt er aus, laeuft die Liste
      // unverengt weiter -- langsamer, aber richtig. Das ist der Punkt des ganzen Baus: Die Liste
      // ist die Frage, wegen der es dieses Werkzeug gibt.
      LOG.warn("Fensterverengung ausgefallen, die Liste laeuft unverengt weiter.", fehler);
      return new Ergebnis(abfrage, Verengungsgrund.KEINE_UNTERGRENZE);
    }
  }

  // ── Die Entscheidung, Merkmal fuer Merkmal ────────────────────────────────

  /**
   * Ob das Merkmal in dieser Abfrage ueberhaupt gesetzt ist.
   *
   * <p>Vollstaendiges {@code switch} ohne {@code default}: Ein neuer Wert in {@link Abfragemerkmal}
   * loest hier einen Compilerfehler aus.
   */
  private static boolean gesetzt(Abfragemerkmal merkmal, Nachrichtenabfrage abfrage) {
    return switch (merkmal) {
      case ZEITFENSTER, SORTIERUNG, LIMIT -> true;
      case STATUS -> !abfrage.status().isEmpty();
      case PROZESSE -> !abfrage.prozessIds().isEmpty();
      case SUCHBEGRIFF -> abfrage.suchtreffer() != null;
      case CURSOR -> abfrage.cursor() != null;
    };
  }

  /**
   * Ob die Verengung dieses Merkmal mittragen kann.
   *
   * <p>Vollstaendiges {@code switch} ohne {@code default} — der zweite Compilerfehler bei einem
   * neuen {@link Abfragemerkmal}. <b>Wer hier einen Wert ergaenzt, muss ihn beantworten</b>, und
   * die sichere Antwort ist {@code false}: Ein Merkmal, das der Rollup nicht mittraegt, laesst die
   * Verengung ausfallen — das kostet Laufzeit. Ein Merkmal, das faelschlich als tragbar gilt,
   * kostet Zeilen.
   */
  private static boolean traegtDerRollup(Abfragemerkmal merkmal, Nachrichtenabfrage abfrage) {
    return switch (merkmal) {
      // Die Verengung IST eine Operation auf dem Zeitfenster.
      case ZEITFENSTER -> true;

      // message_rollup traegt den ROHWERT des Status (Entscheidung E-g in V9), und
      // MessageStatusClassifier.bedingung nimmt ein beliebiges Field<String>. Der Filter auf dem
      // Rollup ist damit derselbe Ausdruck wie auf Message, nur mit anderem Feld -- die einzige
      // Bauform, die nicht auseinanderlaeuft. Die Sortierung stimmt ueberein (utf8mb4_general_ci).
      case STATUS -> true;

      // Die Tabelle ist nach (stunde, process_id, message_status) geschluesselt, und seit V11
      // traegt sie zusaetzlich (process_id, stunde).
      case PROZESSE -> true;

      // Er setzt nur die Schwelle: gebraucht werden limit + 1 Zeilen, so viele liest das
      // Repository fuer die Frage "gibt es eine naechste Seite".
      case LIMIT -> true;

      // Der Cursor verschiebt die OBERGRENZE. Weil die angebrochene Stunde, in die er faellt, mit 0
      // gewertet wird, bleibt die gezaehlte Menge eine Unterschranke -- der Tiebreaker ueber
      // MessageID kann also keine Zeile ausschliessen, die gezaehlt worden waere. Belegt in M101:
      // zeichengleiche Pruefsummen ueber zwei Seiten und beide Mandanten.
      case CURSOR -> true;

      // NUR absteigend. Bei AELTESTE stehen die ersten Zeilen am ANFANG des Fensters -- eine
      // Untergrenze verschoebe dort die erste Seite und nicht den Suchraum. Die gespiegelte
      // Rechnung waere richtig, ist aber nicht gemessen; bis dahin gilt die sichere Antwort.
      case SORTIERUNG -> abfrage.absteigend();

      // Der Suchtreffer wirkt als `ProcessID IN (...) OR SOSID IN (...)`, und message_rollup hat
      // KEINE sos_id. Eine Verengung, die nur den Prozesszweig betrachtete, waere zu eng und
      // verloere genau die Zeilen, die allein ueber SOSID treffen.
      case SUCHBEGRIFF -> false;
    };
  }

  // ── Die Rechnung ─────────────────────────────────────────────────────────

  private Ergebnis frageDenRollup(MandantContext mandant, Nachrichtenabfrage abfrage) {
    LocalDateTime wasserstand = repository.wasserstand();
    if (wasserstand == null) {
      // Noch nie gerechnet -- der Rollup weiss ueber keine einzige Stunde etwas.
      return new Ergebnis(abfrage, Verengungsgrund.KEINE_UNTERGRENZE);
    }

    Verengungsgrenzen grenzen = Verengungsgrenzen.aus(abfrage, wasserstand);
    if (grenzen == null) {
      // Keine einzige Stunde, die vollstaendig im Fenster liegt und zugleich gerechnet ist.
      return new Ergebnis(abfrage, Verengungsgrund.KEINE_UNTERGRENZE);
    }

    int schwelle = abfrage.limit() + 1;
    for (LocalDateTime stufeVon : stufen(grenzen)) {
      VerengungRepository.Auskunft auskunft =
          repository.frageStufe(mandant, abfrage, grenzen, stufeVon, schwelle);

      if (auskunft.untergrenze() != null) {
        return auskunft.untergrenze().isAfter(abfrage.fenster().von())
            ? new Ergebnis(mitFenster(abfrage, auskunft.untergrenze()), Verengungsgrund.VERENGT)
            : new Ergebnis(abfrage, Verengungsgrund.KEINE_UNTERGRENZE);
      }

      // Der Nullfall ist erst ueber dem ganzen Fenster aussagbar -- und nur, wenn der Rollup ueber
      // das ganze Fenster Bescheid weiss. Sonst koennten oberhalb des Wasserstands Zeilen liegen,
      // von denen er nichts weiss.
      if (stufeVon.equals(grenzen.hAllVon())
          && auskunft.zeilen() == 0
          && grenzen.fensterGanzUnterWasserstand()) {
        return new Ergebnis(abfrage, Verengungsgrund.NULLFALL);
      }
    }
    return new Ergebnis(abfrage, Verengungsgrund.KEINE_UNTERGRENZE);
  }

  /**
   * Die Stufen als Untergrenzen, von eng nach weit, ohne Wiederholung; die letzte ist immer das
   * ganze Fenster.
   *
   * <p>Paketprivat statt privat, damit {@code FensterverengungGrenzenTest} sie ohne Datenbank
   * pruefen kann. Das ist Absicht: Stufenbildung und Grenzrechnung sind die Stellen, an denen ein
   * Fehler <b>Zeilen kostet</b> — sie gehoeren an einen Test, der bei jedem Build laeuft, und nicht
   * nur an einen, der eine Testkopie braucht.
   */
  static List<LocalDateTime> stufen(Verengungsgrenzen grenzen) {
    Set<LocalDateTime> stufen = new LinkedHashSet<>();
    for (Duration breite : STUFEN) {
      LocalDateTime stufe = Verengungsgrenzen.volleStunde(grenzen.bis().minus(breite));
      if (stufe.isAfter(grenzen.hAllVon())) {
        stufen.add(stufe);
      }
    }
    stufen.add(grenzen.hAllVon());
    return List.copyOf(stufen);
  }

  private static Nachrichtenabfrage mitFenster(Nachrichtenabfrage abfrage, LocalDateTime von) {
    return new Nachrichtenabfrage(
        new Zeitfenster(von, abfrage.fenster().bis()),
        abfrage.status(),
        abfrage.prozessIds(),
        abfrage.suchtreffer(),
        abfrage.absteigend(),
        abfrage.cursor(),
        abfrage.limit());
  }
}
