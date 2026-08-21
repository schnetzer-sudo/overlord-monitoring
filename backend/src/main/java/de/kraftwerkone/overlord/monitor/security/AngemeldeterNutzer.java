package de.kraftwerkone.overlord.monitor.security;

import java.io.Serializable;
import java.util.List;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Der angemeldete Nutzer — das Principal im {@code SecurityContext}.
 *
 * <p>Traegt <b>niemals</b> den Passwort-Hash. Was hier steht, liegt serialisiert in {@code
 * SPRING_SESSION_ATTRIBUTES} und wird bei jeder Anfrage gelesen; ein Hash haette dort nichts zu
 * suchen. Deshalb {@link Serializable}.
 *
 * <p><b>Nicht</b> enthalten ist der aktive Mandant. Der steht als eigenes Attribut in der Session
 * und aendert sich beim Umschalten, ohne dass die Anmeldung angefasst wird — siehe {@link
 * MandantContextProvider}.
 *
 * <p>{@code mustChangePassword} wird bei der Passwortaenderung mit einem neuen Principal
 * ueberschrieben. Wird ein Konto deaktiviert oder gesperrt, wirkt das nicht rueckwirkend auf eine
 * laufende Sitzung — dort wird die Sitzung des Nutzers verworfen ({@link Sitzungsentzug}, Schritt
 * 9a). Das ist der Grund, weshalb die Sitzung serverseitig liegt und kein JWT verwendet wird.
 *
 * <p><b>Das Feld {@code downloadAllowed} ist am 21.08.2026 entfallen</b> (Schritt 9a, E18). Es war
 * seit Schritt 3 tot: {@code rohdaten.md} §3 E2 schliesst eine zweite Berechtigungsstufe aus, die
 * Endpunkte aus Schritt 8 haben es nie geprueft. Mit ihm ist {@code app_user.download_allowed}
 * gefallen (V7).
 *
 * <p><b>Laufende Sitzungen ueberleben den Wegfall</b>, und das ist nachgemessen und nicht
 * angenommen: Die {@code serialVersionUID} eines Record ist {@code 0} und haengt <b>nicht</b> an
 * seinen Komponenten; ein mit fuenf Komponenten geschriebener Strom liest sich in denselben Record
 * mit vier Komponenten sauber ein, das ueberzaehlige Feld wird verworfen. Wer eine Komponente
 * <i>hinzufuegt</i>, bekommt sie beim Lesen alter Stroeme als Vorgabewert — auch das ist kein
 * Fehler, aber es gehoert gewusst, bevor jemand ein Pflichtfeld daraus macht.
 */
public record AngemeldeterNutzer(long id, String username, Rolle rolle, boolean mustChangePassword)
    implements AuthenticatedPrincipal, Serializable {

  /**
   * Der Name der Anmeldung — <b>nicht kosmetisch</b>. Ohne diese Methode nimmt Spring Security
   * {@code toString()} des Principals und schreibt es nach {@code SPRING_SESSION.PRINCIPAL_NAME}
   * (100 Zeichen): Die Darstellung eines Records ist dafuer zu lang, und das Anmelden schluege mit
   * einem Datenbankfehler fehl. Mit dem Benutzernamen bleibt ausserdem der Index auf dieser Spalte
   * brauchbar — ab Schritt 9 lassen sich damit die Sitzungen eines gesperrten Nutzers gezielt
   * verwerfen.
   */
  @Override
  public String getName() {
    return username;
  }

  public List<GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority(rolle.alsAuthority()));
  }

  /** Derselbe Nutzer ohne Aenderungszwang — nach erfolgreicher Passwortaenderung. */
  public AngemeldeterNutzer ohneAenderungszwang() {
    return new AngemeldeterNutzer(id, username, rolle, false);
  }
}
