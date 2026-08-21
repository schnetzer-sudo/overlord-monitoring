package de.kraftwerkone.overlord.monitor.admin;

import de.kraftwerkone.overlord.monitor.security.KontoZeile;
import java.time.Instant;
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
 *     umschaltet. <b>Nicht</b> die automatische nach fuenf Fehlversuchen: die laeuft nach fuenfzehn
 *     Minuten von selbst ab, und ein Zustand, der beim Hinsehen schon wieder anders ist, gehoert
 *     nicht in eine Verwaltungsliste (E14)
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
    boolean active,
    boolean mustChangePassword,
    Instant lastLogin) {

  /**
   * Baut die Antwortzeile.
   *
   * <p><b>Die Umrechnung geht ueber {@link ZoneOffset#UTC} und nicht ueber {@code
   * common/Zeitpunkte}.</b> Der Unterschied ist wesentlich und keine Geschmacksfrage: {@code
   * Zeitpunkte.nachUtc} rechnet die <i>Wanduhrzeit des Altsystem-Servers</i> mit der Zone der
   * Anwendungsuhr um. {@code audit_log.occurred_at} ist dagegen bereits UTC — {@code
   * AuditLogWriter} schreibt es aus der Systemuhr in UTC, weil Protokollzeit keinen Dev-Versatz
   * tragen darf. Die Anwendungsuhr daraufzulegen verschoebe den Wert um den Zonenoffset, und zwar
   * lautlos.
   */
  public static NutzerzeileResponse fuer(KontoZeile zeile) {
    return new NutzerzeileResponse(
        zeile.id(),
        zeile.username(),
        zeile.rolle().name(),
        zeile.mandanten(),
        zeile.adminGesperrt(),
        zeile.aktiv(),
        zeile.passwortwechselErforderlich(),
        zeile.letzteAnmeldungUtc() == null
            ? null
            : zeile.letzteAnmeldungUtc().toInstant(ZoneOffset.UTC));
  }
}
