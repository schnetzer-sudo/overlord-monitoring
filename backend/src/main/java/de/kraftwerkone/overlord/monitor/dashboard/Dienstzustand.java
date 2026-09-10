package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Wie ein Dienst des Altsystems dasteht — <b>die Einordnung, nicht der Rohwert</b>.
 *
 * <p>Gebildet wird sie an genau einer Stelle: {@link DienstStatusClassifier}. Der Rohwert steht in
 * der Antwort daneben und wird nicht ersetzt (Regel Q4) — ohne ihn liesse sich an {@link
 * #UNGEKLAERT} nichts mehr festmachen.
 *
 * <h2>Warum nicht {@code MessageStatusKind}</h2>
 *
 * <p><b>Gleicher Rohwert, anderer Gegenstand</b> (E‑124). {@code ERROR_TIMEOUT} steht in {@code
 * Message.MessageStatus} <i>und</i> in {@code Service.ServiceStatus}; bei einer Nachricht heisst es
 * „der Waechter hat zugeschlagen", bei einem Dienst „dieser Dienst meldet sich nicht mehr". Zwei
 * Fragen mit einem Wort — eine gemeinsame Aufzaehlung haette beide Bedeutungen unter einem Namen
 * gefuehrt, und die naechste Erweiterung haette eine davon still mitgeaendert.
 */
public enum Dienstzustand {

  /**
   * {@code HEARTBEAT} — der Dienst meldet sich.
   *
   * <p><b>Gegen die Testkopie ist dieser Pfad nicht erreichbar</b>, und zwar aus demselben Grund
   * wie {@code RUNNING} bei den Nachrichten: Kein Dienst mit {@code ServiceTimeout > 0} steht dort
   * auf {@code HEARTBEAT} (M52, eingefrorener Stand). Belegt ist er allein durch Tests.
   */
  MELDET_SICH,

  /**
   * {@code ERROR_TIMEOUT} — der Dienst hat sich innerhalb seiner Zeitgrenze nicht gemeldet.
   *
   * <p><b>Die Frist setzt das Altsystem und nicht dieses Werkzeug</b> (E‑118). Es wird nichts aus
   * {@code ServiceLastUpdate} gerechnet und nichts aus {@code ServiceTimeout} umgerechnet; hier
   * steht, was die Anlage selbst eingetragen hat.
   */
  ZEITUEBERSCHRITTEN,

  /** {@code SHUTDOWN} — der Dienst ist geordnet herunterfahren worden. Kein Fehler. */
  HERUNTERGEFAHREN,

  /**
   * Jeder andere Wert und {@code NULL}.
   *
   * <p><b>Er faellt nicht still in einen bekannten Eimer</b> ({@code PROJEKTBESCHREIBUNG.md} §4.1
   * sinngemaess): Der Rohwert wird mitgeliefert, und {@code DienstkatalogDbIT} wird rot, sobald im
   * Altsystem ein unbekannter Wert auftaucht. Nur deshalb ist es vertretbar, unbekannte Werte
   * neutral zu behandeln.
   */
  UNGEKLAERT
}
