package de.kraftwerkone.overlord.monitor.dashboard;

import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Die Einordnung eines {@code Service.ServiceStatus} — <b>an genau einer Stelle</b>.
 *
 * <h2>Warum ein eigener Klassifizierer und nicht der {@code MessageStatusClassifier}</h2>
 *
 * <p><b>Entscheidung E‑124.</b> Beide kennen den Rohwert {@code ERROR_TIMEOUT}, und beide meinen
 * etwas anderes damit: Bei einer Nachricht heisst er „der Waechter auf {@code RUNNING} hat
 * zugeschlagen" ({@code docs/message-status.md}), bei einem Dienst „dieser Dienst hat sich
 * innerhalb seiner Zeitgrenze nicht gemeldet". <b>Ein gemeinsamer Klassifizierer haette zwei
 * Gegenstaende unter einem Namen gefuehrt</b> — und die naechste Ergaenzung haette einen davon
 * still mitgeaendert.
 *
 * <p>Was beide teilen, ist die <b>Bauform</b> und nicht der Inhalt: exakter Vergleich auf den
 * Rohwert, unbekannte Werte fallen nach {@code UNGEKLAERT} und werden mit Rohwert durchgereicht,
 * und ein Drift-Test haelt die bekannte Menge gegen {@code SELECT DISTINCT ServiceStatus}.
 *
 * <h2>Exakter Vergleich, kein Praefix</h2>
 *
 * <p><b>Kein {@code LEFT(ServiceStatus, 6) = 'ERROR_'} und erst recht kein {@code LIKE
 * 'ERROR_%'}</b> (Regel Q1 — {@code _} ist ein Platzhalter). Der Grund ist hier aber ein anderer
 * als bei den Nachrichten: Die Menge der Dienststatuswerte ist klein, vollstaendig bekannt und
 * bewacht. Ein Praefix fuehrte eine Verallgemeinerung ein, fuer die es keinen Anlass gibt — und er
 * machte aus einem kuenftigen {@code ERROR_STARTUP} stillschweigend eine Zeitueberschreitung.
 */
@Component
public class DienstStatusClassifier {

  /** Der Dienst meldet sich. Bei {@code ServiceTimeout > 0} ist das der gruene Fall (E‑117). */
  static final String HEARTBEAT = "HEARTBEAT";

  /** Der Waechter des Altsystems hat zugeschlagen. */
  static final String ERROR_TIMEOUT = "ERROR_TIMEOUT";

  /** Geordnet heruntergefahren. */
  static final String SHUTDOWN = "SHUTDOWN";

  /**
   * Die Einordnung zum Rohwert.
   *
   * @param rohwert {@code Service.ServiceStatus}, auch {@code null} zulaessig
   * @return niemals {@code null} — ein unbekannter Wert wird {@link Dienstzustand#UNGEKLAERT}
   */
  public Dienstzustand einordnung(String rohwert) {
    if (rohwert == null) {
      return Dienstzustand.UNGEKLAERT;
    }
    return switch (rohwert) {
      case HEARTBEAT -> Dienstzustand.MELDET_SICH;
      case ERROR_TIMEOUT -> Dienstzustand.ZEITUEBERSCHRITTEN;
      case SHUTDOWN -> Dienstzustand.HERUNTERGEFAHREN;
      default -> Dienstzustand.UNGEKLAERT;
    };
  }

  /**
   * Die dokumentierte Menge, gegen die {@code DienstkatalogDbIT} {@code SELECT DISTINCT
   * ServiceStatus} haelt.
   *
   * <p><b>Sie ist die Begruendung dafuer, dass {@link Dienstzustand#UNGEKLAERT} neutral behandelt
   * werden darf</b> ({@code PROJEKTBESCHREIBUNG.md} §4.1, „Sicherung gegen neue Statuswerte"):
   * Taucht im Altsystem ein vierter Wert auf, wird der Test rot, und reagiert wird durch Pflege
   * dieser Liste und von {@code docs/dienste.md} — nie durch Raten.
   */
  public Set<String> bekannteStatuswerte() {
    return Set.of(HEARTBEAT, ERROR_TIMEOUT, SHUTDOWN);
  }
}
