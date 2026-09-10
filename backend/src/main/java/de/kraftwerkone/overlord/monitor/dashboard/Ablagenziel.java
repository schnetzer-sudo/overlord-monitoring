package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Ein Ziel der Ablagenpruefung: die Kennung aus {@code ServiceDefaultFileStore} und die Adresse, an
 * der sie erreichbar waere.
 *
 * <h2>Die Adresse verlaesst dieses Paket nur in eine Richtung</h2>
 *
 * <p>Sie geht ausschliesslich an {@code common/Ablagezugriff} und steht in <b>keiner</b> Antwort,
 * in keiner Protokollzeile oberhalb von {@code DEBUG} und in keiner Fehlermeldung (Regel G1).
 * Deshalb ist {@link #toString()} ueberschrieben — genau wie bei {@code payload/Artefaktverweis}:
 * Auch ein versehentliches {@code log.info("{}", ziel)} gibt nichts preis.
 *
 * <p><b>Die Kennung selbst ist kein Geheimnis</b> — sie steht in der Antwort der Kachel und in
 * {@code docs/dienste.md}. Sie bleibt deshalb in {@code toString()} stehen: Eine Protokollzeile,
 * die nicht sagt, <i>welches</i> Ziel gemeint ist, waere nutzlos.
 *
 * @param serviceId die Kennung der Ablage, so wie sie in {@code ServiceDefaultFileStore} steht
 * @param verbindung der {@code ServiceConnectString} der aufgeloesten Zeile — <b>{@code null}, wenn
 *     die Kennung auf keine Zeile in {@code Service} auflöst oder deren Spalte leer ist</b>. Fuer
 *     die Pruefung ist das dasselbe wie ein Knoten, der nicht antwortet: Es gibt keinen Weg dorthin
 *     (so haelt es auch der Rohdatenabruf, {@code docs/rohdaten-backend.md} §3)
 */
public record Ablagenziel(String serviceId, String verbindung) {

  /** Loest die Kennung ueberhaupt auf eine brauchbare Adresse auf? */
  boolean aufloesbar() {
    return verbindung != null && !verbindung.isBlank();
  }

  /** Bewusst ueberschrieben: keine Verbindungszeichenkette ueber eine Protokollzeile (G1). */
  @Override
  public String toString() {
    return "Ablagenziel["
        + serviceId
        + ", Verbindung "
        + (aufloesbar() ? "verdeckt" : "fehlt")
        + "]";
  }
}
