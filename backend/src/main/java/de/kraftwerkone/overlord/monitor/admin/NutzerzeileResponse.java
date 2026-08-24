package de.kraftwerkone.overlord.monitor.admin;

import de.kraftwerkone.overlord.monitor.security.KontoZeile;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Eine Zeile der Kontenliste — und dieselbe Antwort nach jedem der fuenf schreibenden Vorgaenge, so
 * wie {@code SelbstauskunftResponse} nach Anmeldung, Passwortaenderung und Mandantenwechsel
 * dieselbe bleibt. Der Aufrufer soll seinen Zustand nie aus mehreren Antworten zusammensetzen
 * muessen.
 *
 * <p><b>Enthaelt niemals einen Passwort-Hash</b> und niemals ein Passwort — auch nicht das eben
 * vergebene Einmalpasswort. Das geht ausschliesslich auf dem Weg zum Nutzer, den der Admin selbst
 * waehlt.
 *
 * @param tenants die Mandantenkennungen, aufsteigend. Bei einem ADMIN steht hier ebenfalls etwas,
 *     und es ist <b>wirkungslos</b>: Ein Administrator ist fuer alle Mandanten berechtigt. Die
 *     Zuordnung wird trotzdem gefuehrt, damit sie bei einer spaeteren Herabstufung nicht ins Leere
 *     faellt (E10) — und genau deshalb ist die Liste mandantenfrei: In einer gefilterten Liste
 *     stuende das Konto unter einem Mandanten, fuer den es nicht gilt (E2)
 * @param locked die <b>administrative</b> Sperre, die {@code PUT /api/admin/users/{id}/lock}
 *     umschaltet — unbefristet, aufgehoben nur durch einen zweiten Verwaltungsakt. <b>Niemals</b>
 *     die automatische nach fuenf Fehlversuchen; die steht daneben in {@link #lockedUntil} und wird
 *     hier <b>nicht</b> verrechnet (E14, E20)
 * @param lockedUntil das Ende der <b>automatischen</b> Sperre nach fuenf Fehlversuchen — und <b>nur
 *     dann gesetzt, wenn es in der Zukunft liegt</b>. Sonst {@code null}. Siehe den Korrekturkasten
 *     am Kopf von {@link #fuer}
 * @param lastLogin die letzte <b>erfolgreiche</b> Anmeldung, {@code null} wenn es keine gab (E17).
 *     Der Wert kommt aus {@code audit_log} und nicht aus einer Spalte an {@code app_user}; bei
 *     ueber zwanzig externen Nutzern ist „hat der sich ueberhaupt je angemeldet" die haeufigste
 *     Supportfrage, und das Protokoll hat die Antwort bereits
 */
public record NutzerzeileResponse(
    long id,
    String username,
    String role,
    List<String> tenants,
    boolean locked,
    Instant lockedUntil,
    boolean active,
    boolean mustChangePassword,
    Instant lastLogin) {

  /**
   * Baut die Antwortzeile.
   *
   * <h2>{@code lockedUntil} — eine umgekehrte Entscheidung, mit dem alten Wortlaut daneben</h2>
   *
   * <p><b>Bis zum 24.08.2026 trug dieser Record das Feld nicht, und das war ausdruecklich
   * begruendet.</b> Der alte Wortlaut stand am Parameter {@code locked} und lautete woertlich:
   *
   * <blockquote>
   *
   * „{@code locked} ist die <b>administrative</b> Sperre, die {@code PUT
   * /api/admin/users/{id}/lock} umschaltet. <b>Nicht</b> die automatische nach fuenf Fehlversuchen:
   * die laeuft nach fuenfzehn Minuten von selbst ab, und ein Zustand, der beim Hinsehen schon
   * wieder anders ist, gehoert nicht in eine Verwaltungsliste (E14)."
   *
   * </blockquote>
   *
   * <p><b>Der Einwand ist nach wie vor richtig — er trifft nur nicht, was gebaut ist.</b> Er
   * richtet sich gegen einen <i>Zustand</i>, der als „gesperrt" in der Liste steht und beim
   * naechsten Blick verschwunden ist. Was hier steht, ist kein Zustand, sondern ein
   * <b>Zeitpunkt</b>: nicht „gesperrt", sondern <i>bis wann</i>. Genau dafuer traegt E20 die
   * Bedingung „nur wenn in der Zukunft" — ein abgelaufener Wert wird gar nicht erst uebertragen,
   * statt als abgelaufene Sperre erklaert werden zu muessen.
   *
   * <p><b>Wozu er gebraucht wird, ist der Fall, fuer den 9a ueberhaupt gebaut wird</b> (E1): Ein
   * Admin, den einer von ueber zwanzig externen Nutzern anruft, weil er nicht hineinkommt, muss
   * zwischen „ich habe dich gesperrt" und „du hast dich fuenfmal vertippt" unterscheiden koennen.
   * Ohne das Feld sieht er {@code locked: false} und hat keine Erklaerung.
   *
   * <p><b>Die beiden werden nie zusammengefasst</b>, weder hier noch in der Oberflaeche. Taeten sie
   * es, fiele E14 auf Antwortebene zusammen: Ein Angriff und ein Verwaltungsakt haben verschiedene
   * Ursachen und verschiedene Behebungen — der eine laeuft nach fuenfzehn Minuten ab, den anderen
   * hebt nur ein zweiter Verwaltungsakt auf. Der Umschalter fuer {@code locked} raeumt beim
   * Entsperren zusaetzlich die Zeitsperre ab; ein <i>eigener</i> Knopf, der nur sie loescht,
   * entsteht ausdruecklich nicht (E21).
   *
   * <h2>Zwei Zeitwerte, zwei verschiedene Umrechnungen — und beide gehen an {@code
   * common/Zeitpunkte} vorbei</h2>
   *
   * <p><b>Die Umrechnung geht ueber {@link ZoneOffset#UTC} und nicht ueber {@code
   * common/Zeitpunkte}.</b> Der Unterschied ist wesentlich und keine Geschmacksfrage: {@code
   * Zeitpunkte.nachUtc} rechnet die <i>Wanduhrzeit des Altsystem-Servers</i> mit der Zone der
   * Anwendungsuhr um. {@code audit_log.occurred_at} ist dagegen bereits UTC — {@code
   * AuditLogWriter} schreibt es aus der Systemuhr in UTC, weil Protokollzeit keinen Dev-Versatz
   * tragen darf. Die Anwendungsuhr daraufzulegen verschoebe den Wert um den Zonenoffset, und zwar
   * lautlos.
   *
   * <p><b>Fuer {@code app_user.locked_until} gilt dasselbe aus demselben Grund:</b> {@code
   * AnmeldeService} schreibt die Spalte aus der <i>Systemuhr</i> in UTC — Sperrfristen sind
   * sicherheitsnahe Zeit ({@code common/ZeitConfig}, {@code PROJEKTBESCHREIBUNG.md} §7). Sie wird
   * hier deshalb ebenso als UTC gelesen und gegen einen Vergleichszeitpunkt aus derselben Uhr
   * gehalten.
   *
   * @param jetztUtc der Vergleichszeitpunkt, in UTC und aus der <b>Systemuhr</b>. Als Parameter und
   *     nicht als {@code LocalDateTime.now()} — Regel Z1, und obendrein die einzige Bauform, in der
   *     „laeuft die Sperre noch" ohne laufende Uhr pruefbar ist. Der Aufrufer ist {@code
   *     BenutzerverwaltungService}, der die Uhr als {@code systemClock} hat
   */
  public static NutzerzeileResponse fuer(KontoZeile zeile, LocalDateTime jetztUtc) {
    return new NutzerzeileResponse(
        zeile.id(),
        zeile.username(),
        zeile.rolle().name(),
        zeile.mandanten(),
        zeile.adminGesperrt(),
        nochLaufendeZeitsperre(zeile.gesperrtBisUtc(), jetztUtc),
        zeile.aktiv(),
        zeile.passwortwechselErforderlich(),
        alsUtc(zeile.letzteAnmeldungUtc()));
  }

  /**
   * Das Ende der Zeitsperre — <b>oder {@code null}, wenn es schon vorbei ist</b> (E20).
   *
   * <p><b>Die Pruefung steht hier und nicht in der Oberflaeche.</b> Ob eine Sperre noch laeuft, ist
   * eine Aussage ueber sicherheitsnahe Zeit; sie rechnet nach {@code PROJEKTBESCHREIBUNG.md} §7 mit
   * der Systemuhr und niemals mit der Uhr eines Browsers. Ein verstellter Rechner sähe sonst eine
   * abgelaufene Sperre als laufende oder umgekehrt — beides waere eine falsche Auskunft in genau
   * dem Gespraech, fuer das das Feld existiert.
   *
   * <p><b>Nicht {@code isAfter} auf die Sekunde genau umkaempft:</b> Ein Wert, der exakt jetzt
   * ablaeuft, gilt als abgelaufen. Die Sperre dauert fuenfzehn Minuten; welche Seite der Gleichheit
   * gewinnt, ist an keiner Stelle spuerbar, und die strikte Fassung ist die, die „nur wenn es in
   * der Zukunft liegt" woertlich nimmt.
   */
  private static Instant nochLaufendeZeitsperre(
      LocalDateTime gesperrtBisUtc, LocalDateTime jetztUtc) {
    return gesperrtBisUtc == null || !gesperrtBisUtc.isAfter(jetztUtc)
        ? null
        : alsUtc(gesperrtBisUtc);
  }

  private static Instant alsUtc(LocalDateTime wert) {
    return wert == null ? null : wert.toInstant(ZoneOffset.UTC);
  }
}
