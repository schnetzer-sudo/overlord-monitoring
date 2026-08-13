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
  /**
   * Die Belegsuche. **Ohne Navigationseintrag** — sie wird über das Feld in der
   * Kopfzeile erreicht, und das steht auf jeder Seite; ein Menüpunkt daneben wäre
   * eine zweite Tür in denselben Raum (`docs/bam-suche.md`).
   */
  suche: "/suche",
  prozesse: "/prozesse",
  administration: "/administration",
} as const;

/** Query-Parameter, der nach der Anmeldung an den ursprünglichen Ort zurückführt. */
export const WEITER_PARAMETER = "weiter";

/**
 * Der Suchparameter, unter dem die gewählte Nachricht **neben der Liste** steht.
 *
 * Er steht hier und nicht nur in `features/nachrichten/filter.ts`, weil die
 * beiden Funktionen darunter ihn kennen müssen, um ihn zu setzen und wieder zu
 * entfernen — und `lib` importiert nicht aus `features`. Dass beide Stellen
 * denselben Namen meinen, hält `tests/routen.test.ts` fest; ein Auseinanderlaufen
 * wäre sonst genau die Art Fehler, die sich still verhält.
 */
export const NACHRICHT_PARAMETER = "nachricht";

/**
 * Die Abfragezeichenkette in ihre Paare zerlegt — **ohne `nachricht`.**
 *
 * Zerlegt und nicht über `URLSearchParams` geführt, und das ist der Punkt: Jene
 * Klasse baut die Zeichenkette beim Ausgeben neu auf und kodiert dabei um
 * (`von=2026-07-08T00:00:00Z` würde zu `von=2026-07-08T00%3A00%3A00Z`). Die
 * Zusage lautet aber, dass **alles andere unverändert und in seiner Reihenfolge**
 * stehen bleibt — ein umkodierter Wert ist zwar gleichwertig, aber nicht
 * unverändert, und in einer geteilten URL sieht man den Unterschied.
 *
 * Verglichen wird der **rohe** Name vor dem `=`, ohne ihn zu dekodieren: Ein
 * `%6Eachricht=…` aus einer von Hand gebauten URL bliebe damit stehen. Das ist in
 * Kauf genommen — `decodeURIComponent` wirft bei kaputter Kodierung, und ein
 * Umschalter, der an einer fremden URL abstürzt, wäre der schlechtere Tausch.
 */
function uebrigeParameter(abfragezeichenkette: string): string[] {
  const roh = abfragezeichenkette.startsWith("?")
    ? abfragezeichenkette.slice(1)
    : abfragezeichenkette;

  return roh
    .split("&")
    .filter((paar) => paar !== "")
    .filter((paar) => {
      const gleich = paar.indexOf("=");
      const name = gleich === -1 ? paar : paar.slice(0, gleich);
      return name !== NACHRICHT_PARAMETER;
    });
}

/**
 * Das Ziel für „Ohne Liste anzeigen" — die Detailansicht auf ihrer **eigenen
 * Route**.
 *
 * **`nachricht` wird entfernt.** Sonst stünde die Kennung zweimal im Ziel: einmal
 * im Pfad, einmal als Parameter. Welche von beiden dann gälte, wäre eine Frage,
 * die niemand stellen soll.
 *
 * Der Rest bleibt stehen, wie er stand — Zeitfenster, Status, Prozessauswahl,
 * Sortierung. Der Weg zurück zur Liste führt über genau diese Zeichenkette
 * (`nachricht-seite.tsx`), und was hier verloren ginge, käme dort nicht wieder.
 */
export function ansichtOhneListe(messageId: string, abfragezeichenkette: string): string {
  const uebrig = uebrigeParameter(abfragezeichenkette);
  const ziel = `${ROUTEN.nachrichten}/${encodeURIComponent(messageId)}`;
  return uebrig.length === 0 ? ziel : `${ziel}?${uebrig.join("&")}`;
}

/**
 * Das Ziel für „Neben der Liste anzeigen" — die Liste mit geöffnetem Panel.
 *
 * **`nachricht` steht vorn und genau einmal.** Vorn, weil die Kennung aus dem
 * Pfad kommt und die übrigen Parameter ihre Reihenfolge behalten sollen; genau
 * einmal, weil ein bereits vorhandener gleichnamiger Parameter vorher entfernt
 * wird statt einen zweiten danebenzustellen.
 *
 * > Damit steht die Kennung hier **vor** den Filtern, während
 * > `alsSuchparameter` sie ans Ende setzt. Beides sind gültige URLs mit
 * > demselben Zustand; `nuqs` schreibt die Reihenfolge beim nächsten Filterklick
 * > ohnehin nach seiner eigenen Ordnung um.
 */
export function ansichtNebenListe(messageId: string, abfragezeichenkette: string): string {
  const paare = [
    `${NACHRICHT_PARAMETER}=${encodeURIComponent(messageId)}`,
    ...uebrigeParameter(abfragezeichenkette),
  ];
  return `${ROUTEN.nachrichten}?${paare.join("&")}`;
}

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
