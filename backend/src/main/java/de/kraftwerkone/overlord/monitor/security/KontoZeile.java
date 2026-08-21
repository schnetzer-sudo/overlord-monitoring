package de.kraftwerkone.overlord.monitor.security;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ein Konto, wie es die <b>Verwaltung</b> braucht — im Unterschied zu {@link AppUserZeile}, das die
 * <b>Anmeldung</b> braucht.
 *
 * <p><b>Der Unterschied ist der Passwort-Hash, und er ist der ganze Grund fuer den zweiten Typ.</b>
 * {@link AppUserZeile} traegt ihn und verlaesst deshalb niemals das Paket {@code security}. Die
 * Kontenliste geht dagegen als Antwort nach draussen. Diesen Typ um den Hash zu erweitern oder
 * jenen zum Antwort-DTO auszubauen waere beides derselbe Fehler in verschiedene Richtungen; zwei
 * Typen sind hier billiger als eine Regel, an die sich jemand erinnern muss.
 *
 * @param mandanten die zugeordneten {@code MandantID}s, aufsteigend sortiert. <b>Nur die Kennungen,
 *     keine Anzeigenamen</b> — die laegen in {@code GlassfishDB.Mandant} und kosteten einen
 *     schemauebergreifenden Join je Zeile, waehrend die Kennung selbst der sprechende Code ist
 *     ({@code VOTG}, {@code NEXANS}), den auch {@code POST /api/admin/users} entgegennimmt.
 * @param adminGesperrt die administrative Sperre. <b>Nicht</b> die automatische nach fuenf
 *     Fehlversuchen: die laeuft nach fuenfzehn Minuten von selbst ab und ist kein Zustand, den eine
 *     Verwaltungsliste zeigen sollte, weil er beim Hinsehen schon wieder anders ist ({@code
 *     V7__benutzerverwaltung.sql}, E14).
 * @param letzteAnmeldungUtc {@code null}, wenn sich das Konto noch <b>nie</b> angemeldet hat — bei
 *     ueber zwanzig externen Nutzern die haeufigste Supportfrage (E17). Der Wert kommt aus {@code
 *     audit_log}, nicht aus einer Spalte an {@code app_user}.
 */
public record KontoZeile(
    long id,
    String username,
    Rolle rolle,
    List<String> mandanten,
    boolean adminGesperrt,
    boolean aktiv,
    boolean passwortwechselErforderlich,
    LocalDateTime letzteAnmeldungUtc) {

  public KontoZeile {
    mandanten = List.copyOf(mandanten);
  }

  /**
   * Ob dieses Konto ein <b>nutzbarer</b> Administrator ist — aktiv und nicht gesperrt (E12).
   *
   * <p>Die Definition steht hier und nicht als Bedingung in einer Abfrage, weil sie an zwei Stellen
   * gebraucht wird (Selbstschutz beim Sperren, Deaktivieren und Herabstufen) und weil sie der Punkt
   * ist, an dem E12 kippt: Zaehlte man gesperrte Administratoren mit, waere der zweite Admin ein
   * Feigenblatt.
   */
  public boolean istNutzbarerAdmin() {
    return rolle == Rolle.ADMIN && aktiv && !adminGesperrt;
  }
}
