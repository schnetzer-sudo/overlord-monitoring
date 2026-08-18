package de.kraftwerkone.overlord.monitor.payload;

import java.util.Optional;

/**
 * Der zerlegte Artefaktverweis: Ablagenkennung und nackte UUID.
 *
 * <p>Die Wertform ist durchgaengig {@code <Ablagenkennung>|<UUID>} (M54). Die Kennung vor der Pipe
 * <b>ist</b> eine {@code Service.ServiceID} — lesbare Codes, Primaerschluesselzugriff (M52). Was
 * dahinter steht, geht als <b>nackte UUID</b> an die Ablage, ohne Praefix und ohne Pipe (Q1, {@code
 * JsonServlet.java:784}–{@code :786}).
 *
 * <p><b>Dieser Typ verlaesst das Backend nie.</b> Er steht in keiner Antwort, in keiner Kennung und
 * in keiner Protokollzeile — weder die UUID noch die Ablagenkennung. Er entsteht aus einer bereits
 * mandantengefilterten Zeile und lebt bis zum Ende des Abrufs.
 *
 * @param ablage die {@code Service.ServiceID} vor der Pipe
 * @param uuid die Kennung dahinter, unveraendert
 */
public record Artefaktverweis(String ablage, String uuid) {

  private static final char PIPE = '|';

  public Artefaktverweis {
    if (ablage == null || ablage.isBlank() || uuid == null || uuid.isBlank()) {
      throw new IllegalArgumentException("Ein Artefaktverweis ohne beide Haelften gibt es nicht");
    }
  }

  /**
   * Zerlegt den Wert aus {@code MessageProperty} an der Pipe.
   *
   * <p><b>An der ersten Pipe und ohne Reparatur.</b> Ein Wert ohne Pipe, mit leerer Haelfte oder
   * mit mehr als einer Pipe ist ein Datenfehler und kein Sonderfall, den man erraten kann; er
   * ergibt {@link Optional#empty()} und daraus den benannten Zustand {@link
   * Artefaktzustand#ABLAGE_NICHT_ERREICHBAR}. In den gemessenen Werten kommt die Form ausnahmslos
   * so vor (M54) — die Pruefung kostet nichts und haelt eine stille Fehldeutung heraus.
   */
  public static Optional<Artefaktverweis> zerlege(String wert) {
    if (wert == null) {
      return Optional.empty();
    }
    String getrimmt = wert.trim();
    int pipe = getrimmt.indexOf(PIPE);
    if (pipe <= 0 || pipe != getrimmt.lastIndexOf(PIPE) || pipe == getrimmt.length() - 1) {
      return Optional.empty();
    }
    String ablage = getrimmt.substring(0, pipe).trim();
    String uuid = getrimmt.substring(pipe + 1).trim();
    if (ablage.isEmpty() || uuid.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new Artefaktverweis(ablage, uuid));
  }

  /**
   * Bewusst ueberschrieben: Der Verweis darf in keiner Protokollzeile landen, auch nicht
   * versehentlich ueber ein {@code log.debug("{}", verweis)}.
   */
  @Override
  public String toString() {
    return "Artefaktverweis[verdeckt]";
  }
}
