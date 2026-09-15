package de.kraftwerkone.overlord.monitor.common;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import org.springframework.http.HttpStatus;

/**
 * Die zwei Gliederungen des Prozessbaums — <b>eine Vorgabe und keine Berechtigung</b>.
 *
 * <table border="1">
 *   <caption>Gliederung und Ebenen</caption>
 *   <tr><th>Wert</th><th>Ebenen, von aussen nach innen</th><th>haengt am Katalog</th></tr>
 *   <tr><td>{@link #PARTNER}</td><td>Partner, Richtung, Prozess</td><td>ja</td></tr>
 *   <tr><td>{@link #PROJEKT}</td><td>Projektbeschreibung, Prozess</td><td>nein</td></tr>
 * </table>
 *
 * <p><i>(seit 15.09.2026.)</i> Der Partnerbaum bleibt der bessere Einstieg, er haengt aber
 * vollstaendig am kuratierten Katalog: {@code SUTTONS} und {@code WOC} tragen keine einzige
 * Katalogzeile, bei {@code NEXANS} tragen 216 von 733 Prozessen keinen Partner (M110). Die
 * Projektgliederung braucht keine Kuratierung und traegt fuer jeden Mandanten sofort.
 *
 * <h2>Warum dieser Typ in {@code common} steht</h2>
 *
 * <p>Zwei Fachpakete brauchen ihn: {@code catalog} gliedert den Baum danach, {@code admin} pflegt
 * die Vorgabe je Konto ({@code app_user.tree_layout}, {@code V13}). <b>Fachpakete kennen einander
 * nicht</b> — braucht ein zweites einen Typ, wandert er nach {@code common} und nicht ins
 * Nachbarpaket ({@code PaketstrukturTest.fachpakete_kennen_einander_nicht}). Dieselbe Bewegung wie
 * bei {@link Pflegestatus} und {@link Rollupzeitraum}.
 *
 * <h2>Keine Berechtigung</h2>
 *
 * <p>Welche Gliederung ein Nutzer beim ersten Aufruf sieht, setzt ein ADMIN je Konto; <b>wechseln
 * darf jeder</b>, ueber {@code ?gliederung=} am Baum-Endpunkt. Es gibt keine Rollengrenze und keine
 * Moeglichkeit, das Wechseln zu sperren ({@code docs/process-view.md} §48).
 */
public enum Baumgliederung {

  /** Partner, Richtung, Prozess — die Gliederung am kuratierten Katalog, seit Schritt 10c. */
  PARTNER,

  /** Projektbeschreibung, Prozess — ohne Kuratierung. */
  PROJEKT;

  /**
   * Liest die Gliederung aus einem Anfrageparameter.
   *
   * <p><b>Ohne Angabe {@code null}</b> — und das heisst <i>nicht angegeben</i>, nicht {@link
   * #PARTNER}. Welche Gliederung dann gilt, entscheidet die Vorgabe des angemeldeten Kontos, und
   * die kennt dieser Typ nicht.
   *
   * <p><b>Ein unbekannter Wert ist {@code 400} und faellt nicht stillschweigend auf die Vorgabe
   * zurueck</b> — dieselbe Bauform wie {@code modus-unbekannt} bei der Massenzuordnung. Sonst
   * zeigte ein Tippfehler in einem geteilten Link die Vorgabe des Empfaengers, und niemand wuesste
   * warum.
   *
   * <p>Gross- und Kleinschreibung zaehlen nicht, wie bei {@link Rollupzeitraum#ausCode}.
   *
   * @return die Gliederung, oder {@code null}, wenn keine angegeben ist
   * @throws FachlicheAusnahme {@code 400 gliederung-unbekannt}
   */
  public static Baumgliederung ausText(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String gesucht = text.trim();
    for (Baumgliederung gliederung : values()) {
      if (gliederung.name().equalsIgnoreCase(gesucht)) {
        return gliederung;
      }
    }
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "gliederung-unbekannt",
        "Gliederung unbekannt",
        "Erlaubt sind PARTNER und PROJEKT.",
        "Unbekannte Baumgliederung in der Anfrage");
  }

  /**
   * Liest den gespeicherten Wert aus {@code app_user.tree_layout}.
   *
   * <p><b>Streng und ohne Rueckfall</b>, wie {@code security/Rolle.ausDatenbank}: Die Spalte ist
   * {@code NOT NULL} mit Vorgabe {@code PARTNER}, und geschrieben wird sie nur ueber diesen Typ.
   * Ein anderer Wert ist ein Datenfehler und keine Eingabe.
   */
  public static Baumgliederung ausDatenbank(String wert) {
    for (Baumgliederung gliederung : values()) {
      if (gliederung.name().equals(wert)) {
        return gliederung;
      }
    }
    throw new IllegalStateException("Unbekannte Baumgliederung in app_user.tree_layout: " + wert);
  }
}
