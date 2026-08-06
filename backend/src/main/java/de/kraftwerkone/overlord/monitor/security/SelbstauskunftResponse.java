package de.kraftwerkone.overlord.monitor.security;

import java.time.ZoneId;

/**
 * Antwort von {@code GET /api/auth/me} — und dieselbe Antwort nach Anmeldung, Passwortaenderung und
 * Mandantenwechsel. Der Aufrufer soll seinen Zustand nie aus mehreren Antworten zusammensetzen
 * muessen.
 *
 * @param mandant {@code null}, solange kein Mandant gewaehlt ist. Das ist bei jedem ADMIN und bei
 *     jedem Nutzer mit mehreren Mandanten der Zustand direkt nach dem Anmelden — kein Fehler,
 *     sondern eine offene Auswahl.
 * @param mustChangePassword solange {@code true}, lehnt jeder Endpunkt ausser Selbstauskunft,
 *     Passwortaenderung und Abmeldung ab
 * @param anzeigezone die IANA-Kennung der Zone, in der Zeitstempel <b>anzuzeigen</b> sind, etwa
 *     {@code Europe/Berlin} — siehe {@link #fuer}
 */
public record SelbstauskunftResponse(
    String username,
    String role,
    MandantResponse mandant,
    boolean mustChangePassword,
    boolean downloadAllowed,
    String anzeigezone) {

  /**
   * Baut die Selbstauskunft — die eine Stelle, an der die Anzeigezone entsteht.
   *
   * <p><b>Warum die Zone ueberhaupt uebertragen wird.</b> Auf der Leitung stehen ausschliesslich
   * UTC-Zeitpunkte (Richtlinie §5.3); dort ist der Wert eindeutig, und {@code von}/{@code bis}
   * brauchen genau das. Die Quelle fuehrt ihre Zeitstempel aber als <b>Wanduhrzeit</b> des
   * Altsystem-Servers ({@code docs/datenzugriff.md} §7), und {@code common/Zeitpunkte} rechnet sie
   * mit der Zone der Anwendungsuhr nach UTC um. Formatierte das Frontend anschliessend in der Zone
   * des <i>Browsers</i>, kaeme die Wanduhrzeit nur dort wieder heraus, wo Browser und Server
   * zufaellig dieselbe Zone haben: Aus 23:53 in der Datenbank wuerde in Antwerpen 22:53 und im
   * Sommer 21:53 — plausibel genug, dass es niemand merkt, und falsch genug, dass die Suche nach
   * dem Zeitpunkt aus dem Altwerkzeug ins Leere geht.
   *
   * <p><b>Deshalb kommt die Zone vom Backend und nicht aus einer Konstante im Frontend.</b> Sie
   * waere sonst an zwei Stellen gepflegt und liefe beim Umzug des Servers auseinander — und zwar
   * lautlos. Es ist dieselbe Zone, mit der {@code Zeitpunkte.nachUtc} rechnet; die Kette
   * Wanduhrzeit → UTC → Anzeige schliesst sich damit ueber genau einen Wert.
   *
   * <p>Ein Nutzer in Muenchen und einer in Antwerpen sehen dieselbe Uhrzeit, und beide dieselbe wie
   * im Altsystem. Das ist Absicht: Der Zeitpunkt ist hier eine <b>Eigenschaft des Belegs</b>, kein
   * Termin im Kalender des Betrachters.
   */
  public static SelbstauskunftResponse fuer(
      AngemeldeterNutzer nutzer, MandantResponse mandant, ZoneId anzeigezone) {
    return new SelbstauskunftResponse(
        nutzer.username(),
        nutzer.rolle().name(),
        mandant,
        nutzer.mustChangePassword(),
        nutzer.downloadAllowed(),
        anzeigezone.getId());
  }
}
