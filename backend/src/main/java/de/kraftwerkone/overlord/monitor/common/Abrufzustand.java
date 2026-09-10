package de.kraftwerkone.overlord.monitor.common;

/**
 * Was ein Abruf bei einer Ablage ergeben hat — <b>die drei Faelle, die der Transport selbst
 * unterscheiden kann</b>.
 *
 * <h2>Warum eine eigene Menge und nicht {@code Artefaktzustand}</h2>
 *
 * <p>{@code payload/Artefaktzustand} kennt fuenf Zustaende, und zwei davon entstehen <b>nach</b>
 * dem Abruf: {@code BINAERDATEI} faellt bei der Binaerpruefung an, {@code
 * KEIN_ANZEIGBARER_PROTOKOLLTEIL} beim Beschnitt. Der Transport sieht beide nie — er weiss nur, ob
 * er Bytes bekommen hat, ob die Ablage geantwortet und nichts geliefert hat, oder ob er gar nicht
 * hingekommen ist.
 *
 * <p><b>Die Aufzaehlung ist damit nicht die halbe {@code Artefaktzustand}, sondern die
 * vollstaendige Antwortmenge einer anderen Frage.</b> {@code common} kennt {@code payload} nicht
 * (Abschnitt 6 der Projektbeschreibung: „Fachpakete kennen einander nicht. Gemeinsames liegt in
 * {@code common}"), und ein zweiter Verbraucher — die Ablagenpruefung des Dashboards — braucht
 * genau diese drei und keinen der beiden anderen.
 *
 * <p>Die Abbildung auf {@code Artefaktzustand} steht in {@code payload} und nicht hier: Wer die
 * engere Menge in die weitere uebersetzt, ist der, der die weitere kennt.
 */
public enum Abrufzustand {

  /** Die Ablage hat geantwortet und einen Anhang geliefert. */
  GELIEFERT,

  /**
   * Die Ablage antwortet, liefert aber nichts — {@code Error (Skipped)} im Rumpf, nicht im
   * HTTP-Status (M68, M66 (2)). Produktiv ein Fehlerzustand; <b>in der Entwicklung der
   * Normalfall</b>, weil 63,2 % des Bestands vor der Rotationsgrenze liegen und die zugehoerigen
   * Ablagen aus sind.
   */
  DATEI_NICHT_VORHANDEN,

  /**
   * Die Kennung loest nicht auf, der Knoten antwortet nicht, oder der Anhang ist unlesbar
   * beziehungsweise zu gross. <b>Etwas anderes als „Datei weg"</b> — und fuer den Betrieb die
   * wichtigere Unterscheidung. Ein Rueckfall auf eine andere Ablage gibt es nicht: 20 von 20
   * Kreuzabrufen scheitern, die Ablagen sind keine Spiegel (M68).
   */
  ABLAGE_NICHT_ERREICHBAR
}
