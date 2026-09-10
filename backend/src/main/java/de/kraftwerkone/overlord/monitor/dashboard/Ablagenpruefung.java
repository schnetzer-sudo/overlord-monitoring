package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.Ablagezugriff;
import de.kraftwerkone.overlord.monitor.common.Abrufergebnis;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Der zeitgesteuerte Lauf, der die Ablagen tatsaechlich fragt — <b>im Hintergrund, nie in der
 * Anfrage</b> (E‑120).
 *
 * <h2>Warum ueberhaupt gefragt und nicht abgelesen wird</h2>
 *
 * <p>Ablagen senden keinen Heartbeat (Auskunft vom 10.09.2026, nicht gemessen). Ihr {@code
 * ServiceStatus} steht bei allen elf seit dem <b>07.06.2012</b> auf {@code HEARTBEAT} (M52) — eine
 * Lampe daraus waere gruen seit vierzehn Jahren und saehe aus wie eine Messung. <b>Was diese Klasse
 * feststellt, ist dagegen belegt</b>, und der Beleg ist hoechstens zwei Takte alt.
 *
 * <h2>Warum im Hintergrund und nicht beim Aufruf</h2>
 *
 * <p><b>Weil es gemessen ist.</b> Ein Abruf gegen eine erreichbare Ablage kostet rund 130 ms, einer
 * gegen eine abgeschaltete <b>rund 2,7 Sekunden</b> (M174, {@code docs/dienste.md} §4). Das
 * Leistungsbudget der Landingpage liegt bei 500 ms; ein Dashboard, das beim Aufruf zwei fremde
 * Knoten anspricht, haengt an deren Zeitgrenzen und nicht mehr an den eigenen.
 *
 * <h2>{@code fixedDelay} und nicht {@code fixedRate}</h2>
 *
 * <p><b>Ein Durchgang ueberholt den naechsten nie.</b> Bei {@code fixedRate} wuerde ein Durchgang,
 * der laenger braucht als sein Takt, den naechsten sofort hinterherschicken — bei zwei nicht
 * erreichbaren Zielen und einem knappen Takt liefen die Durchgaenge ineinander. Die Zeitgrenzen
 * sind die des Rohdatenabrufs ({@code common/Ablagegrenzen}); mehr Schutz braucht es nicht.
 *
 * <h2>Zwei Riegel, wie beim Rollup</h2>
 *
 * <ol>
 *   <li><b>{@code overlord.ablagenpruefung.aktiv}.</b> Fehlt der Schluessel, gibt es diese Bean
 *       nicht — und die Kachel sagt {@link Ablagengrund#ABGESCHALTET}. Im Profil {@code dev} ist
 *       sie ausdruecklich aus: Ein lokaler Start soll nicht unaufgefordert im Minutentakt fremde
 *       Knoten anfragen.
 *   <li><b>Der Takt steht als Platzhalter</b> und nicht als Literal. Fehlt er bei eingeschalteter
 *       Pruefung, scheitert der Start — besser als ein Zeitplan, den niemand kennt.
 * </ol>
 *
 * <p><b>Kein Profil als dritter Riegel</b>, anders als beim Rollup: Der Job dort <i>schreibt</i> in
 * die geteilte Testkopie, diese Pruefung liest zwei Stammdatenzeilen und schickt eine Anfrage, die
 * konstruktionsbedingt nichts ablegen kann ({@code RETRIEVE}, M-Abschnitt QT1 in {@code
 * messungen-schritt8.md}).
 */
@Component
@ConditionalOnProperty(prefix = "overlord.ablagenpruefung", name = "aktiv", havingValue = "true")
public class Ablagenpruefung {

  private static final Logger log = LoggerFactory.getLogger(Ablagenpruefung.class);

  /**
   * Die Kennung, mit der gefragt wird — <b>nicht die eines Artefakts</b>.
   *
   * <p>Sie ist eine syntaktisch gueltige UUID und zeigt mit Sicherheit auf keine Datei. Der Sinn
   * ist genau der: Die Pruefung will wissen, ob der Knoten antwortet, und nicht, was er hat. Eine
   * echte Kennung zu nehmen hiesse, eine fremde Nutzdatei im Minutentakt anzufassen — und gemessen
   * ist ohnehin, dass die Ablage die Null-UUID wie jeden fehlenden Verweis behandelt (M174).
   */
  static final String NULL_UUID = "00000000-0000-0000-0000-000000000000";

  private final DienstLeseRepository dienstLeseRepository;
  private final Ablagezugriff ablagezugriff;
  private final Clock anwendungsuhr;
  private final Duration takt;

  /**
   * Der letzte Stand — <b>im Speicher und sonst nirgends</b>.
   *
   * <p>{@link AtomicReference} und kein synchronisiertes Feld: Geschrieben wird von einem einzigen
   * Zeitgeberfaden, gelesen von jedem Anfragefaden. Gebraucht wird davon nur die
   * <b>Sichtbarkeit</b> — dass ein Leser den vollstaendigen neuen Stand sieht und nicht einen halb
   * geschriebenen. Der Stand selbst ist unveraenderlich, also gibt es nichts zu sperren.
   */
  private final AtomicReference<Ablagenstand> stand = new AtomicReference<>();

  Ablagenpruefung(
      DienstLeseRepository dienstLeseRepository,
      Ablagezugriff ablagezugriff,
      Clock anwendungsuhr,
      AblagenpruefungEigenschaften eigenschaften) {
    this.dienstLeseRepository = dienstLeseRepository;
    this.ablagezugriff = ablagezugriff;
    this.anwendungsuhr = anwendungsuhr;
    this.takt = eigenschaften.takt();
  }

  /**
   * Ein Durchgang: die Ziele neu lesen, dann je Ziel einmal fragen.
   *
   * <p><b>Die Ziele werden je Durchgang neu gelesen</b> und nicht beim Start einmal. Ein Eintrag in
   * {@code ServiceDefaultFileStore} kann sich aendern, ohne dass dieses Werkzeug davon erfaehrt —
   * und ein Wechsel der Ablage ist genau der Vorgang, bei dem eine Ueberwachung nicht auf dem alten
   * Ziel stehen bleiben darf. Zwei Stammdatenzeilen je Minute kosten nichts (M175).
   *
   * <p><b>Faellt der Durchgang aus, wird kein Stand gesetzt.</b> Das ist Absicht: Der alte Stand
   * altert weiter und wird nach zwei Takten {@link Ablagengrund#STAND_VERALTET} — die Kachel sagt
   * dann „ungeklaert" statt einer Zahl, fuer die niemand mehr einsteht (E‑125). Ein {@code catch},
   * das hier einen leeren Stand schriebe, machte aus einem Datenbankfehler eine Aussage ueber die
   * Ablagen.
   */
  @Scheduled(fixedDelayString = "${overlord.ablagenpruefung.takt}")
  public void pruefe() {
    try {
      List<Ablagenziel> ziele = dienstLeseRepository.pruefziele();
      List<Zielstand> ergebnisse = new ArrayList<>(ziele.size());
      for (Ablagenziel ziel : ziele) {
        ergebnisse.add(new Zielstand(ziel.serviceId(), pruefe(ziel)));
      }
      stand.set(new Ablagenstand(LocalDateTime.now(anwendungsuhr), ergebnisse));
    } catch (RuntimeException fehler) {
      // Der Zeitplan laeuft weiter. Der Stand bleibt der alte und veraltet von selbst; die Kachel
      // sagt danach "ungeklaert" mit Grund und nicht "erreichbar".
      log.warn(
          "Die Ablagenpruefung ist gescheitert. Der letzte Stand bleibt stehen und gilt noch"
              + " hoechstens zwei Takte; danach zeigt die Kachel ungeklaert.",
          fehler);
    }
  }

  /**
   * Ein Ziel, eine Frage — <b>und die Einordnung kommt aus dem vorhandenen Ergebnis</b>.
   *
   * <p>Es wird nichts neu geparst: Was der Rohdatenabruf als {@code DATEI_NICHT_VORHANDEN}
   * einordnet, heisst hier <i>erreichbar</i>; was er als {@code ABLAGE_NICHT_ERREICHBAR} einordnet,
   * heisst <i>nicht erreichbar</i>; gelieferte Daten heissen <i>ungeklaert</i>. <b>Die Zuordnung
   * ist mit M174 belegt</b> und steht an genau dieser Stelle — ein zweites Auswerten der
   * SOAP-Antwort waere dieselbe Regel ein zweites Mal, und sie driftete beim naechsten Zustand von
   * der ersten weg.
   *
   * <p><b>Ein nicht aufloesbares Ziel heisst nicht erreichbar</b>, genau wie beim Rohdatenabruf
   * ({@code docs/rohdaten-backend.md} §3): Es gibt keinen Weg dorthin. Gefragt wird dann gar nicht
   * erst.
   *
   * <p>Die gelieferten Bytes werden <b>nicht angefasst</b> — kein Entpacken, kein Lesen, kein
   * Protokollieren. Die Pruefung will wissen, ob geantwortet wird, und nicht, was da liegt.
   */
  private Ablagenzustand pruefe(Ablagenziel ziel) {
    if (!ziel.aufloesbar()) {
      return Ablagenzustand.NICHT_ERREICHBAR;
    }
    Abrufergebnis ergebnis = ablagezugriff.hole(ziel.verbindung(), NULL_UUID);
    return switch (ergebnis.zustand()) {
      case DATEI_NICHT_VORHANDEN -> Ablagenzustand.ERREICHBAR;
      case ABLAGE_NICHT_ERREICHBAR -> Ablagenzustand.NICHT_ERREICHBAR;
      case GELIEFERT -> Ablagenzustand.UNGEKLAERT;
    };
  }

  /** Der letzte Durchgang, oder leer, solange keiner beendet ist. */
  Optional<Ablagenstand> letzterStand() {
    return Optional.ofNullable(stand.get());
  }

  /** Der eingestellte Takt — die Kachel rechnet ihre Altersgrenze daraus (zwei Takte). */
  Duration takt() {
    return takt;
  }
}
