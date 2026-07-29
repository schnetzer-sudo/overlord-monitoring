package de.kraftwerkone.overlord.monitor.security;

import de.kraftwerkone.overlord.monitor.common.error.KeinMandantGewaehltException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loest den {@link MandantContext} <b>ausschliesslich aus der Session</b> auf und setzt ihn beim
 * Wechsel.
 *
 * <p>Es gibt hier bewusst keine Methode, die eine Mandanten-ID aus Pfad, Query, Header oder Cookie
 * liest. Die einzige Stelle, an der eine Mandanten-ID von aussen entgegengenommen wird, ist der
 * Umschalt-Endpunkt — und der prueft sie zuerst gegen die zulaessige Menge (Regel M1).
 *
 * <p>Die Session wird hier <b>nie angelegt</b> ({@code getSession(false)}): Ein Lesezugriff auf den
 * Mandanten darf keine Sitzung erzeugen.
 */
@Component
public class MandantContextProvider {

  /**
   * Attributname in der Session. Der Wert ist die reine {@code MandantID}; er liegt serialisiert in
   * {@code SPRING_SESSION_ATTRIBUTES}.
   */
  static final String SESSION_ATTRIBUT = "overlord.aktiverMandant";

  private final HttpServletRequest request;

  MandantContextProvider(HttpServletRequest request) {
    this.request = request;
  }

  /**
   * Der aktive Mandant. <b>Der Einstieg fuer jeden fachlichen Endpunkt ab Schritt 4.</b>
   *
   * @throws KeinMandantGewaehltException wenn noch keiner gewaehlt ist — bei einem Nutzer mit
   *     mehreren zulaessigen Mandanten (und damit bei jedem ADMIN) ist das direkt nach dem Anmelden
   *     der Normalfall. Ein Zugriff „einfach ohne Filter" existiert nicht.
   */
  public MandantContext aktuellerKontext() {
    return aktuelleMandantId()
        .map(MandantContext::new)
        .orElseThrow(KeinMandantGewaehltException::new);
  }

  /** Der aktive Mandant, sofern gesetzt — fuer die Selbstauskunft, die auch ohne auskommt. */
  public Optional<String> aktuelleMandantId() {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return Optional.empty();
    }
    return Optional.ofNullable((String) session.getAttribute(SESSION_ATTRIBUT));
  }

  /**
   * Setzt den aktiven Mandanten. Aufrufer <b>muessen</b> vorher geprueft haben, dass die ID in der
   * zulaessigen Menge des Nutzers liegt — diese Methode prueft das nicht und kann es nicht.
   */
  void setze(String mandantId) {
    request.getSession(true).setAttribute(SESSION_ATTRIBUT, mandantId);
  }
}
