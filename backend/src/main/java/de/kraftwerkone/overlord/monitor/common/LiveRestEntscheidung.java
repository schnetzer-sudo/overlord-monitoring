package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;

/**
 * Das Ergebnis von {@link LiveRest#entscheide}: der Zustand und, je nach Zustand, die Zeitpunkte,
 * an denen ein Verbraucher weiterrechnet.
 *
 * <p>Alle Zeitpunkte sind <b>Datenzeit der Anwendungsuhr</b> — Wanduhrzeit des Quellservers, wie
 * {@code message_rollup.stunde} und {@code Message.MessageLastUpdate} sie fuehren.
 *
 * @param zustand einer der drei Zustaende
 * @param vollstaendigBis <b>G</b>, der Eimer des letzten Laufs — nur bei {@link
 *     LiveRestZustand#AUSGESETZT} mit vorhandenem Lauf; die Eimer davor sind vollstaendig
 *     gerechnet, ab hier fehlt Verkehr. Sonst {@code null}
 * @param liveVon <b>G</b>, der Anfang des Live-Bereichs (einschliessend) — nur bei {@link
 *     LiveRestZustand#ANGEWANDT}, sonst {@code null}
 * @param liveBis der Anfang der Stunde nach {@code jetzt}, das Ende des Live-Bereichs
 *     (ausschliessend) — nur bei {@link LiveRestZustand#ANGEWANDT}, sonst {@code null}
 */
public record LiveRestEntscheidung(
    LiveRestZustand zustand,
    LocalDateTime vollstaendigBis,
    LocalDateTime liveVon,
    LocalDateTime liveBis) {

  public LiveRestEntscheidung {
    if (zustand == null) {
      throw new IllegalArgumentException("Eine Entscheidung ohne Zustand gibt es nicht");
    }
    // Die Felder haengen am Zustand, und ein Widerspruch dazwischen soll hier fallen und nicht
    // erst in einem Verbraucher, der ein null nicht erwartet hat.
    switch (zustand) {
      case ANGEWANDT -> {
        if (liveVon == null || liveBis == null || !liveVon.isBefore(liveBis)) {
          throw new IllegalArgumentException(
              "ANGEWANDT braucht einen nicht leeren Live-Bereich, war: "
                  + liveVon
                  + " bis "
                  + liveBis);
        }
        if (vollstaendigBis != null) {
          throw new IllegalArgumentException("ANGEWANDT traegt kein vollstaendigBis");
        }
      }
      case NICHT_NOETIG, AUSGESETZT -> {
        if (liveVon != null || liveBis != null) {
          throw new IllegalArgumentException(zustand + " traegt keinen Live-Bereich");
        }
        if (zustand == LiveRestZustand.NICHT_NOETIG && vollstaendigBis != null) {
          throw new IllegalArgumentException("NICHT_NOETIG traegt kein vollstaendigBis");
        }
      }
    }
  }

  /** Ohne Lauf: ausgesetzt, ohne Zeitangabe. */
  public static LiveRestEntscheidung ausgesetztOhneLauf() {
    return new LiveRestEntscheidung(LiveRestZustand.AUSGESETZT, null, null, null);
  }

  /** Mit Lauf, aber zu lange her: ausgesetzt, vollstaendig bis {@code g}. */
  public static LiveRestEntscheidung ausgesetztAb(LocalDateTime g) {
    return new LiveRestEntscheidung(LiveRestZustand.AUSGESETZT, g, null, null);
  }

  public static LiveRestEntscheidung nichtNoetig() {
    return new LiveRestEntscheidung(LiveRestZustand.NICHT_NOETIG, null, null, null);
  }

  public static LiveRestEntscheidung angewandt(LocalDateTime g, LocalDateTime liveBis) {
    return new LiveRestEntscheidung(LiveRestZustand.ANGEWANDT, null, g, liveBis);
  }

  /** Ob ein Verbraucher den Live-Bereich lesen und verrechnen soll. */
  public boolean angewandt() {
    return zustand == LiveRestZustand.ANGEWANDT;
  }
}
