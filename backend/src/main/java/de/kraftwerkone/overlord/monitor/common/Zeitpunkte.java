package de.kraftwerkone.overlord.monitor.common;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;

/**
 * Die Bruecke zwischen der <b>Wanduhrzeit des Datenbankservers</b> und dem <b>UTC-Zeitpunkt der
 * API</b> — an genau einer Stelle.
 *
 * <p>Zwei Festlegungen treffen hier aufeinander:
 *
 * <ul>
 *   <li>{@code docs/datenzugriff.md} §7: Zeitstempel aus {@code GlassfishDB} werden als Wanduhrzeit
 *       des Servers behandelt und <b>nirgends konvertiert</b>. {@code MessageLastUpdate} ist ein
 *       {@code LocalDateTime} ohne Zone.
 *   <li>Richtlinie §5.3: In der Uebertragung ausschliesslich ISO 8601 mit <b>UTC</b>.
 * </ul>
 *
 * <p>Dazwischen braucht es eine Zone. Genommen wird die der <b>Anwendungsuhr</b> — dieselbe, mit
 * der die Uhr aus {@code common/ZeitConfig} relative Zeitfenster bildet. Damit kommt keine neue
 * Annahme hinzu: Dass Anwendungs- und Datenbankserver in derselben Zone laufen, setzt die
 * Anwendungsuhr bereits voraus, sobald sie ein 24-Stunden-Fenster gegen {@code MessageLastUpdate}
 * haelt. Fuer die Testkopie ist das gemessen (Messung M0: Serverzeit und Arbeitsplatzuhr gehen
 * gleich).
 *
 * <p>Die Umrechnung ist bewusst <b>nicht</b> Jackson ueberlassen: Ein {@code LocalDateTime} haette
 * dort keine Zone und ein {@code Instant} nur die, die zufaellig konfiguriert ist. Hier steht sie
 * im Code und ist pruefbar.
 */
public final class Zeitpunkte {

  private Zeitpunkte() {}

  /**
   * Liest einen ISO-8601-Zeitpunkt der API ({@code 2025-12-29T00:00:00Z}) als Wanduhrzeit des
   * Datenbankservers.
   *
   * @param feld Name des Anfrageparameters — er steht im Text, damit der Aufrufer weiss, welcher
   *     der beiden Zeitpunkte gemeint ist
   * @throws FachlicheAusnahme {@code 400}, wenn der Wert nicht lesbar ist
   */
  public static LocalDateTime ausIso(String wert, ZoneId zone, String feld) {
    try {
      return LocalDateTime.ofInstant(OffsetDateTime.parse(wert).toInstant(), zone);
    } catch (DateTimeParseException ex) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitpunkt-ungueltig",
          "Zeitpunkt ungueltig",
          "Der Wert fuer " + feld + " muss ein Zeitpunkt in der Form 2026-07-24T18:30:00Z sein.",
          "Nicht lesbarer Zeitpunkt im Parameter " + feld);
    }
  }

  /** Die Gegenrichtung: Wanduhrzeit der Quelle als UTC-Zeitpunkt fuer die Antwort. */
  public static Instant nachUtc(LocalDateTime wanduhrzeit, ZoneId zone) {
    return wanduhrzeit == null ? null : wanduhrzeit.atZone(zone).toInstant();
  }
}
