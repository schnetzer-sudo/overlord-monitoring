package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Die Regeln, nach denen aus einem {@link Ablagenstand} eine {@link AblagenResponse} wird.
 *
 * <h2>Die Zusammenfassung, in der Reihenfolge ihrer Prüfung</h2>
 *
 * <ol>
 *   <li><b>Es gibt keinen Beleg</b> — die Pruefung ist abgeschaltet, hat noch keinen Durchgang oder
 *       ihr letzter Stand ist aelter als <b>zwei Takte</b>: {@link Ablagenzustand#UNGEKLAERT} mit
 *       benanntem Grund.
 *   <li><b>Es ist kein Ziel eingetragen</b>: ebenfalls ungeklaert. „Kein Ziel ist nicht erreichbar"
 *       waere formal wahr und als Auskunft wertlos.
 *   <li><b>Ein Ziel ist nicht erreichbar</b> → nicht erreichbar.
 *   <li><b>Sonst ein Ziel ungeklaert</b> → ungeklaert.
 *   <li><b>Sonst</b> → erreichbar.
 * </ol>
 *
 * <h2>Warum der Beleg vor dem Inhalt geprueft wird (E‑125)</h2>
 *
 * <p><b>Gruen darf seinen Beleg nicht ueberleben</b> — und Rot ebenso wenig. Ein Stand, der aelter
 * ist als zwei Takte, sagt <i>nichts</i> ueber die Ablagen, und zwar in jede Richtung: Eine Kachel,
 * die nach dem Ausfall der Pruefung auf ihrem letzten roten Stand stehen bliebe, behauptete eine
 * Stoerung, die niemand mehr geprueft hat. Deshalb steht die Altersfrage <b>vor</b> der Frage nach
 * dem Inhalt und nicht daneben.
 *
 * <p><b>Zwei Takte und nicht einer:</b> Ein Durchgang, der etwas laenger braucht als sein Takt, ist
 * kein Befund — ein Abruf gegen eine abgeschaltete Ablage dauert allein rund 2,7 Sekunden (M174).
 * Zwei Takte lassen einen vollstaendigen Durchgang ausfallen, bevor die Kachel schweigt.
 *
 * <h2>Warum die Klasse nicht in {@link Ablagenpruefung} steht</h2>
 *
 * <p>Weil es die Pruefung im abgeschalteten Fall <b>gar nicht gibt</b> — und genau dann muss die
 * Kachel trotzdem eine Auskunft geben. Ein Zustand, der von einer fehlenden Bean abhaengt, gehoert
 * nicht in diese Bean.
 */
final class Ablagenkachel {

  /** Ab wann ein Stand keinen Beleg mehr traegt: nach so vielen Takten. */
  private static final int TAKTE_BIS_VERALTET = 2;

  private Ablagenkachel() {}

  /**
   * Die Pruefung ist abgeschaltet — es gibt {@link Ablagenpruefung} nicht.
   *
   * <p><b>Die Kachel verschwindet deshalb nicht.</b> Eine fehlende Kachel waere Abwesenheit, und
   * Abwesenheit sieht genauso aus wie „nichts zu melden". Hier steht ein benannter Grund.
   */
  static AblagenResponse abgeschaltet() {
    return new AblagenResponse(
        Ablagenzustand.UNGEKLAERT, Ablagengrund.ABGESCHALTET, List.of(), null, null);
  }

  /**
   * Die Kachel aus dem letzten Stand.
   *
   * @param stand der letzte Durchgang, oder {@code null}, wenn noch keiner beendet ist
   * @param takt der eingestellte Abstand zwischen zwei Durchgaengen
   * @param jetzt derselbe Uhrenschlag, den auch der Rest der Antwort benutzt (Regel Z1)
   * @param zone die Zone der Anwendungsuhr — die Zeitpunkte aus der Quelle sind Wanduhrzeit und
   *     werden erst hier nach UTC gerechnet
   */
  static AblagenResponse aus(Ablagenstand stand, Duration takt, LocalDateTime jetzt, ZoneId zone) {
    if (stand == null) {
      return new AblagenResponse(
          Ablagenzustand.UNGEKLAERT, Ablagengrund.NOCH_KEIN_DURCHGANG, List.of(), null, null);
    }

    java.time.Instant geprueftAm = Zeitpunkte.nachUtc(stand.geprueftAm(), zone);
    Long alter = Alter.sekunden(stand.geprueftAm(), jetzt);
    List<AblagenzielResponse> ziele =
        stand.ziele().stream()
            .map(ziel -> new AblagenzielResponse(ziel.serviceId(), ziel.zustand()))
            .toList();

    if (veraltet(stand.geprueftAm(), takt, jetzt)) {
      return new AblagenResponse(
          Ablagenzustand.UNGEKLAERT, Ablagengrund.STAND_VERALTET, ziele, geprueftAm, alter);
    }
    if (ziele.isEmpty()) {
      return new AblagenResponse(
          Ablagenzustand.UNGEKLAERT, Ablagengrund.KEIN_ZIEL_EINGETRAGEN, ziele, geprueftAm, alter);
    }
    if (enthaelt(stand, Ablagenzustand.NICHT_ERREICHBAR)) {
      return new AblagenResponse(Ablagenzustand.NICHT_ERREICHBAR, null, ziele, geprueftAm, alter);
    }
    if (enthaelt(stand, Ablagenzustand.UNGEKLAERT)) {
      return new AblagenResponse(
          Ablagenzustand.UNGEKLAERT, Ablagengrund.ZIEL_UNGEKLAERT, ziele, geprueftAm, alter);
    }
    return new AblagenResponse(Ablagenzustand.ERREICHBAR, null, ziele, geprueftAm, alter);
  }

  /**
   * <b>Ein Stand aus der Zukunft gilt als frisch</b> und nicht als veraltet.
   *
   * <p>Er entsteht nicht im Betrieb — Pruefzeitpunkt und {@code jetzt} kommen aus derselben Uhr —,
   * und wenn doch, ist er kein Grund, eine gerade erst erhobene Auskunft wegzuwerfen. Dass das
   * Alter dann {@code null} ist, sagt es bereits (E‑75).
   */
  private static boolean veraltet(LocalDateTime geprueftAm, Duration takt, LocalDateTime jetzt) {
    return geprueftAm.plus(takt.multipliedBy(TAKTE_BIS_VERALTET)).isBefore(jetzt);
  }

  private static boolean enthaelt(Ablagenstand stand, Ablagenzustand gesucht) {
    return stand.ziele().stream().anyMatch(ziel -> ziel.zustand() == gesucht);
  }
}
