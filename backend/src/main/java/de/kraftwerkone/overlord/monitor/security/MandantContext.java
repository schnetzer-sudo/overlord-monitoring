package de.kraftwerkone.overlord.monitor.security;

/**
 * Der aktive Mandant — die Sicherheitsgrenze des Systems.
 *
 * <p><b>Genau eine Mandanten-ID, fuer jede Rolle.</b> Auch ADMIN arbeitet in genau einem
 * Mandantenkontext und wechselt ihn, statt alle gleichzeitig zu sehen. Damit sind die
 * Repository-Signaturen fuer beide Rollen identisch, es gibt keinen Codepfad ohne Mandantenfilter,
 * und der Isolationstest gilt fuer ADMIN unveraendert.
 *
 * <p><b>Aufgeloest wird er ausschliesslich aus der Session</b> ({@link MandantContextProvider}) —
 * niemals aus Pfad, Query, Header oder Cookie. Kein Endpunkt nimmt eine Mandanten-ID entgegen
 * (Regel M1); die genau zwei Ausnahmen sind namentlich in {@code docs/mandantentrennung.md}
 * gefuehrt.
 *
 * <p><b>Er ist erster Pflichtparameter jeder Methode, die {@code jooq.glassfish} anfasst</b> (Regel
 * M2). Es gibt keine Ueberladung ohne ihn, auch nicht {@code private}, auch nicht „nur fuer den
 * Test". {@code PaketstrukturTest} prueft das maschinell; die wenigen Methoden, die den Kontext
 * erst herstellen, tragen {@link OhneMandantenkontext}.
 *
 * <p><b>Hinweis fuer Schritt 10.</b> Der Rollup-Job laeuft ohne Sitzung und wird sich seinen
 * Kontext je Mandant <b>explizit erzeugen</b> muessen — deshalb ist der Konstruktor oeffentlich und
 * der Typ nicht an die Web-Schicht gebunden. Diese Faehigkeit wird hier nicht gebaut, aber auch
 * nicht verbaut.
 */
public record MandantContext(String mandantId) {

  public MandantContext {
    if (mandantId == null || mandantId.isBlank()) {
      throw new IllegalArgumentException("MandantContext ohne MandantID gibt es nicht");
    }
  }
}
