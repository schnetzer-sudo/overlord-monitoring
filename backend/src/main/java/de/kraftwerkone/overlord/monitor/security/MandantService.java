package de.kraftwerkone.overlord.monitor.security;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Die zulaessige Mandantenmenge eines Nutzers und der Wechsel darin.
 *
 * <p><b>Geprueft wird die Menge, nicht die Rolle.</b> Regel: <i>Du darfst zu jedem Mandanten
 * wechseln, fuer den du berechtigt bist.</i> ADMIN ist fuer alle aus {@code GlassfishDB.Mandant}
 * berechtigt, MANDANT fuer die eigenen Eintraege in {@code app_user_mandant}. Damit ist ADMIN kein
 * Sonderfall, sondern der Nutzer mit der groessten Menge — und ein MANDANT-Nutzer mit zwei
 * Mandanten funktioniert ohne Zusatzbau.
 *
 * <p><b>Eine ID ausserhalb der Menge liefert immer {@code 404}</b>, gleichgueltig ob es sie gibt.
 * Ein existierender, aber fremder Mandant und eine erfundene ID muessen ununterscheidbar
 * beantwortet werden — sonst laesst sich ueber den Umschalt-Endpunkt die Mandantenliste abfragen.
 * Deshalb wird hier auch <b>nicht</b> gegen {@code GlassfishDB.Mandant} geprueft, sondern
 * ausschliesslich gegen die zulaessige Menge: eine Existenzpruefung waere genau der Unterschied,
 * den es nicht geben darf.
 */
@Service
public class MandantService {

  private final MandantRepository mandantRepository;
  private final MandantContextProvider mandantContextProvider;
  private final AuditLogWriter auditLogWriter;

  MandantService(
      MandantRepository mandantRepository,
      MandantContextProvider mandantContextProvider,
      AuditLogWriter auditLogWriter) {
    this.mandantRepository = mandantRepository;
    this.mandantContextProvider = mandantContextProvider;
    this.auditLogWriter = auditLogWriter;
  }

  /** Genau die Menge, aus der dieser Nutzer waehlen darf — nichts darueber hinaus. */
  public List<MandantResponse> zulaessige(AngemeldeterNutzer nutzer) {
    return nutzer.rolle() == Rolle.ADMIN
        ? mandantRepository.findeAlle()
        : mandantRepository.findeFuerNutzer(nutzer.id());
  }

  /** Der aktive Mandant mit Anzeigenamen, sofern gewaehlt. */
  public Optional<MandantResponse> aktiver(AngemeldeterNutzer nutzer) {
    Optional<String> aktiv = mandantContextProvider.aktuelleMandantId();
    if (aktiv.isEmpty()) {
      return Optional.empty();
    }
    // Auch hier ueber die zulaessige Menge: verliert ein Nutzer seine Zuordnung, waehrend die
    // Sitzung laeuft, faellt der Mandant damit von selbst weg.
    return zulaessige(nutzer).stream().filter(m -> m.id().equals(aktiv.get())).findFirst();
  }

  /**
   * Wechselt den aktiven Mandanten.
   *
   * @throws RessourceNichtGefundenException wenn die ID nicht in der zulaessigen Menge liegt —
   *     unabhaengig davon, ob es den Mandanten gibt
   */
  public MandantResponse wechsle(AngemeldeterNutzer nutzer, String mandantId, String ip) {
    MandantResponse ziel =
        zulaessige(nutzer).stream()
            .filter(m -> m.id().equals(mandantId))
            .findFirst()
            .orElseThrow(
                () ->
                    new RessourceNichtGefundenException(
                        "Mandantenwechsel auf nicht zulaessige oder unbekannte ID durch Nutzer "
                            + nutzer.id()));

    String vorher = mandantContextProvider.aktuelleMandantId().orElse(null);
    mandantContextProvider.setze(ziel.id());
    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.MANDANT_GEWECHSELT,
            nutzer.id(),
            nutzer.username(),
            ziel.id(),
            "mandant",
            ziel.id(),
            ip,
            "vorher: " + (vorher == null ? "keiner" : vorher)));
    return ziel;
  }
}
