package de.kraftwerkone.overlord.monitor.common.error;

import org.springframework.http.HttpStatus;

/**
 * Es ist kein Mandant aktiv. {@code 403}.
 *
 * <p>Der {@code MandantContext} traegt immer <b>genau eine</b> Mandanten-ID und wird
 * ausschliesslich aus der Session aufgeloest. Hat ein Nutzer mehrere zulaessige Mandanten — bei
 * ADMIN sind es alle —, ist beim Anmelden noch keiner gesetzt. In diesem Zustand lehnt jeder
 * fachliche Endpunkt ab, <b>nicht</b> weil eine Rolle fehlt, sondern weil es ohne Mandant keinen
 * zulaessigen Ausschnitt gibt. Es gibt bewusst keinen Codepfad ohne Mandantenfilter (Regel M1/M2).
 */
public class KeinMandantGewaehltException extends FachlicheAusnahme {

  public KeinMandantGewaehltException() {
    super(
        HttpStatus.FORBIDDEN,
        "kein-mandant-gewaehlt",
        "Kein Mandant gewaehlt",
        "Waehle zuerst einen Mandanten aus.");
  }
}
