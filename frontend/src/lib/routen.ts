/**
 * Die Routen der Anwendung an einer Stelle. **Deutsch**, weil der Nutzer sie
 * sieht und teilt.
 */
export const ROUTEN = {
  anmeldung: "/anmeldung",
  passwort: "/passwort",
  mandantenauswahl: "/mandantenauswahl",
  // Kein eigener `/dashboard`-Pfad: Das Dashboard entsteht ab Schritt 10 auf
  // der Startseite, nicht daneben.
  startseite: "/",
  nachrichten: "/nachrichten",
  prozesse: "/prozesse",
  administration: "/administration",
} as const;

/** Query-Parameter, der nach der Anmeldung an den ursprünglichen Ort zurückführt. */
export const WEITER_PARAMETER = "weiter";

/**
 * Prüft ein `weiter`-Ziel, **bevor** dorthin umgeleitet wird.
 *
 * Ohne diese Prüfung wäre die Anmeldeseite eine offene Weiterleitung: Ein Link
 * auf `/anmeldung?weiter=https://…` führte nach erfolgreicher Anmeldung auf eine
 * fremde Seite — und zwar mit dem Vertrauen, das der Nutzer gerade dieser
 * Anwendung entgegengebracht hat.
 *
 * Erlaubt ist deshalb ausschließlich ein Pfad innerhalb dieser Anwendung: genau
 * ein führender Schrägstrich, kein Protokoll, kein Backslash (den einige Browser
 * wie einen Schrägstrich behandeln).
 */
export function istSicheresZiel(ziel: string | null | undefined): ziel is string {
  if (!ziel) {
    return false;
  }
  if (!ziel.startsWith("/")) {
    return false;
  }
  if (ziel.startsWith("//") || ziel.startsWith("/\\")) {
    return false;
  }
  if (ziel.includes("\\")) {
    return false;
  }
  // Zurück auf die Anmeldung wäre eine Schleife.
  return ziel !== ROUTEN.anmeldung && !ziel.startsWith(`${ROUTEN.anmeldung}?`);
}

/** Das geprüfte Ziel oder die Startseite. */
export function sicheresZiel(ziel: string | null | undefined): string {
  return istSicheresZiel(ziel) ? ziel : ROUTEN.startseite;
}
