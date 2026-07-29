package de.kraftwerkone.overlord.monitor.security;

/**
 * Die beiden Rollen des Werkzeugs. In {@code app_user.role} wird {@link #name()} gespeichert —
 * bewusst als Text mit Whitelist im Code, nicht als {@code ENUM} in der Datenbank.
 *
 * <p><b>Die Rolle entscheidet nicht ueber Daten, sondern ueber die Menge der waehlbaren
 * Mandanten.</b> ADMIN sieht alle Mandanten, aber nacheinander: Es ist immer genau ein Mandant
 * aktiv, fuer jede Rolle. Damit gibt es keinen Codepfad ohne Mandantenfilter, die
 * Repository-Signaturen sind fuer beide Rollen identisch und der Isolationstest gilt fuer ADMIN
 * unveraendert.
 */
public enum Rolle {

  /** Kundenmitarbeiter, extern. Waehlbar sind die Eintraege aus {@code app_user_mandant}. */
  MANDANT,

  /** Interne EDI-Betreuung. Waehlbar sind alle Mandanten aus {@code GlassfishDB.Mandant}. */
  ADMIN;

  /** Der Name der Spring-Security-Berechtigung: {@code ROLE_MANDANT} bzw. {@code ROLE_ADMIN}. */
  public String alsAuthority() {
    return "ROLE_" + name();
  }

  /**
   * Wandelt den in {@code app_user.role} gespeicherten Text um. Ein unbekannter Wert ist ein
   * Datenfehler und keine harmlose Abweichung — er wuerde sonst still zur schwaecheren oder,
   * schlimmer, zur staerkeren Rolle.
   */
  public static Rolle ausDatenbank(String wert) {
    for (Rolle rolle : values()) {
      if (rolle.name().equals(wert)) {
        return rolle;
      }
    }
    throw new IllegalStateException("Unbekannte Rolle in app_user.role: " + wert);
  }
}
