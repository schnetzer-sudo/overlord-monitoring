package de.kraftwerkone.overlord.monitor.common;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

/**
 * Das <b>Pflicht-Zeitfenster</b> jedes Listen-Endpunkts (Regel L1). Zwei Zeitpunkte in der
 * Wanduhrzeit des Datenbankservers — genau so, wie {@code Message.MessageLastUpdate} sie fuehrt
 * (siehe {@code docs/datenzugriff.md} §7).
 *
 * <p><b>Ohne Zeitfenster wird nicht gelesen.</b> Jede Messung aus {@code
 * docs/messungen-schritt4.md} zeigt dasselbe Muster: Mit Zeitfenster arbeitet MariaDB im {@code
 * range}-Zugriff ueber {@code MessageLastUpdateIDX} und braucht Millisekunden, ohne Zeitfenster
 * wird jede Abfrage ueber {@code Message} zu einem vollen Durchlauf von 1,5 bis 20 Sekunden. Fehlt
 * die Angabe, gilt die Vorgabe von 24 Stunden — es wird nie unbegrenzt gelesen.
 *
 * <p>Das Fenster ist <b>beidseitig geschlossen</b>: {@code von <= MessageLastUpdate <= bis}. Dass
 * eine Zeile damit theoretisch auf zwei aneinandergrenzenden Fenstern liegt, ist harmlos; die
 * Paginierung laeuft ueber den Cursor und nicht ueber die Fenstergrenze.
 */
public record Zeitfenster(LocalDateTime von, LocalDateTime bis) {

  /** Die Vorgabe, wenn der Aufrufer nichts nennt (Regel L1). */
  public static final Zeitraum VORGABE = Zeitraum.LETZTE_24_STUNDEN;

  public Zeitfenster {
    if (von == null || bis == null) {
      throw new IllegalArgumentException("Ein Zeitfenster ohne Grenzen gibt es nicht");
    }
  }

  /**
   * Loest die Angaben des Aufrufers zu einem Fenster auf.
   *
   * <p><b>Die beiden Modi schliessen einander aus.</b> Sind {@code zeitraum} und {@code von}/{@code
   * bis} zugleich gesetzt, ist das {@code 400} — und keine stille Vorrangregel. Wer beides schickt,
   * hat eine Vorstellung davon, welches gewinnt; raet das Backend, bekommt er ohne Hinweis ein
   * anderes Fenster als gedacht.
   *
   * @param zeitraum relativer Zeitraum oder {@code null}
   * @param von absolute Untergrenze oder {@code null} — nur zusammen mit {@code bis}
   * @param bis absolute Obergrenze oder {@code null} — nur zusammen mit {@code von}
   * @param anwendungsuhr die <b>Anwendungsuhr</b> aus {@link ZeitConfig}, niemals die Systemuhr:
   *     Die Testkopie liegt Monate hinter der realen Zeit, ein relatives Fenster ab der Systemuhr
   *     waere dort immer leer (Regel Z1).
   */
  public static Zeitfenster aufloesen(
      Zeitraum zeitraum, LocalDateTime von, LocalDateTime bis, Clock anwendungsuhr) {
    boolean absolut = von != null || bis != null;
    if (zeitraum != null && absolut) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-mehrdeutig",
          "Zeitfenster mehrdeutig",
          "Gib entweder einen Zeitraum oder die beiden Zeitpunkte an, nicht beides.",
          "zeitraum und von/bis gleichzeitig gesetzt");
    }
    if (absolut) {
      return absolutes(von, bis);
    }
    Zeitraum gewaehlt = zeitraum == null ? VORGABE : zeitraum;
    LocalDateTime jetzt = LocalDateTime.now(anwendungsuhr);
    return new Zeitfenster(jetzt.minus(gewaehlt.dauer()), jetzt);
  }

  private static Zeitfenster absolutes(LocalDateTime von, LocalDateTime bis) {
    if (von == null || bis == null) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-unvollstaendig",
          "Zeitfenster unvollstaendig",
          "Ein absolutes Zeitfenster braucht beide Zeitpunkte: von und bis.",
          "Nur eine der beiden Grenzen gesetzt");
    }
    if (von.isAfter(bis)) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-ungueltig",
          "Zeitfenster ungueltig",
          "Der Zeitpunkt bis muss nach von liegen.",
          "von liegt hinter bis");
    }
    // Ein Jahr als Kalenderjahr, nicht als 365 Tage — sonst haengt die Grenze am Schaltjahr.
    if (von.isBefore(bis.minusYears(1))) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-zu-gross",
          "Zeitfenster zu gross",
          "Das Zeitfenster darf hoechstens ein Jahr umfassen.",
          "Zeitfenster ueber ein Jahr angefragt");
    }
    return new Zeitfenster(von, bis);
  }

  /** Liegt der Zeitpunkt im Fenster? Grundlage der Cursor-Pruefung. */
  public boolean enthaelt(LocalDateTime zeitpunkt) {
    return !zeitpunkt.isBefore(von) && !zeitpunkt.isAfter(bis);
  }

  /**
   * Die Spanne des Fensters — unabhaengig davon, ob sie aus einem relativen Zeitraum oder aus
   * {@code von}/{@code bis} entstanden ist.
   *
   * <p>Grundlage jeder Grenze, die an der <b>Groesse</b> des Fensters haengt und nicht am Modus:
   * Ein Filter, der ueber ein langes Fenster teuer wird, wird es unabhaengig davon, wie das Fenster
   * zustande kam.
   */
  public Duration spanne() {
    return Duration.between(von, bis);
  }

  /**
   * Die Spanne in Tagen, <b>aufgerundet</b> — die Zahl, die in einer Fehlermeldung steht.
   *
   * <p>Aufgerundet, weil ein Fenster von 30 Tagen und einer Sekunde sonst als „30 Tage" in einer
   * Meldung erschiene, die es mit der Begruendung „hoechstens 30 Tage" gerade abgewiesen hat.
   */
  public static long tageAufgerundet(Duration dauer) {
    long ganze = dauer.toDays();
    return dauer.equals(Duration.ofDays(ganze)) ? ganze : ganze + 1;
  }
}
