package de.kraftwerkone.overlord.monitor.message;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Die Stundengrenzen, aus denen die Fensterverengung rechnet.
 *
 * <h2>Warum das ein eigener Typ ist</h2>
 *
 * <p>Weil hier die <b>Sicherheitseigenschaft</b> des ganzen Baus steckt und sonst nirgends: Was
 * gezaehlt wird, muss vollstaendig im Fenster liegen und vollstaendig gerechnet sein. Nur dann ist
 * die gezaehlte Menge eine <b>Unterschranke</b> der wirklichen — und nur dann kann die Verengung
 * keine Zeile verlieren. Als eigener Typ laesst sich das ohne Datenbank pruefen, bei jedem Build
 * ({@code FensterverengungGrenzenTest}).
 *
 * <h2>Die Rechnung in einem Satz</h2>
 *
 * <p>Die Stundeneimer des Rollups sind halboffen ({@code [stunde, stunde+1h)}); ein Zeitfenster
 * beginnt und endet aber fast immer mitten in einer Stunde. Die beiden <b>angebrochenen</b>
 * Randstunden zaehlen deshalb nicht mit, und oberhalb des Wasserstands zaehlt gar nichts.
 *
 * @param hAllVon erste Stunde, die das Fenster ueberhaupt beruehrt — in aller Regel angebrochen,
 *     sie zaehlt nur fuer den Nullfall
 * @param hAllBis letzte Stunde, die das Fenster beruehrt — ebenso
 * @param hVollVon erste Stunde, die <b>vollstaendig</b> im Fenster liegt
 * @param hVollBis letzte Stunde, die vollstaendig im Fenster liegt <b>und</b> gerechnet ist
 * @param bis die wirksame Obergrenze — das Fensterende oder der Cursor, je nachdem, was frueher
 *     liegt
 * @param wasserstand bis wohin der Rollup Bescheid weiss; {@code fenster_bis} ist ausschliessend,
 *     gerechnet ist also alles <b>unterhalb</b>
 * @param fensterGanzUnterWasserstand ob der Rollup ueber das <b>ganze</b> Fenster Bescheid weiss —
 *     nur dann ist der Nullfall aussagbar
 */
record Verengungsgrenzen(
    LocalDateTime hAllVon,
    LocalDateTime hAllBis,
    LocalDateTime hVollVon,
    LocalDateTime hVollBis,
    LocalDateTime bis,
    LocalDateTime wasserstand,
    boolean fensterGanzUnterWasserstand) {

  /**
   * Rechnet die Grenzen aus — oder {@code null}, wenn es keine einzige Stunde gibt, die
   * vollstaendig im Fenster liegt und gerechnet ist. Dann darf gar nicht erst verengt werden.
   */
  static Verengungsgrenzen aus(Nachrichtenabfrage abfrage, LocalDateTime wasserstand) {
    LocalDateTime von = abfrage.fenster().von();
    LocalDateTime bis = abfrage.fenster().bis();
    // Jede Seite verengt fuer sich: Die zweite rechnet gegen den Cursor-Zeitpunkt, nicht gegen das
    // Fensterende. Weitergereicht wird nichts.
    if (abfrage.cursor() != null && abfrage.cursor().zeitpunkt().isBefore(bis)) {
      bis = abfrage.cursor().zeitpunkt();
    }
    if (!von.isBefore(bis)) {
      return null;
    }

    LocalDateTime hAllVon = volleStunde(von);
    LocalDateTime hAllBis = volleStunde(bis);
    // Die Stunde, in der `von` liegt, gehoert nur dann ganz dazu, wenn `von` genau auf ihr beginnt.
    LocalDateTime hVollVon = hAllVon.equals(von) ? hAllVon : hAllVon.plusHours(1);
    // Die Stunde, in der `bis` liegt, gehoert nie ganz dazu. Der Sonderfall "bis endet auf der
    // letzten Sekunde der Stunde" ist bewusst nicht ausgenutzt: Eine Stunde zu verschenken ist
    // billiger als ein Sonderfall, den beim naechsten Umbau niemand nachrechnet.
    LocalDateTime hVollBis = hAllBis.minusHours(1);
    // Und nie ueber den Wasserstand hinaus.
    LocalDateTime gerechnetBis = volleStunde(wasserstand).minusHours(1);
    if (gerechnetBis.isBefore(hVollBis)) {
      hVollBis = gerechnetBis;
    }
    if (hVollBis.isBefore(hVollVon)) {
      return null;
    }
    return new Verengungsgrenzen(
        hAllVon, hAllBis, hVollVon, hVollBis, bis, wasserstand, bis.isBefore(wasserstand));
  }

  static LocalDateTime volleStunde(LocalDateTime zeitpunkt) {
    return zeitpunkt.truncatedTo(ChronoUnit.HOURS);
  }
}
