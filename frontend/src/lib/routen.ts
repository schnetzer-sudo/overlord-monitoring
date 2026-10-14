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
  /**
   * Der Administrationsbereich — **ein** Navigationseintrag, zwei Unterseiten
   * (`docs/frontend-grundlagen.md` §2). Die Basisroute ist seit Schritt 4
   * vergeben und trägt seit Schritt 9b eine Übersicht statt eines Platzhalters.
   *
   * **Beschriftung und Route heißen beide „Administration"** — ein Wort, das der
   * Nutzer liest und das in der Adresszeile steht. Der Auftrag zu Schritt 9b hat
   * das am 24.08.2026 entschieden; der Weg dorthin steht im Korrekturkasten von
   * `docs/frontend-grundlagen.md` §2.
   */
  administration: "/administration",
  /** Die Katalogpflege (Schritt 9b). Das Backend verlangt dahinter die Rolle `ADMIN`. */
  administrationKatalog: "/administration/katalog",
  /** Die Benutzerverwaltung (Schritt 9a). Inhalt entsteht in ihrem eigenen Auftrag. */
  administrationBenutzer: "/administration/benutzer",
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

/** Das Pfadstück zwischen Nachricht und Datei — an einer Stelle, weil es auch zerlegt wird. */
const DATEIEN_SEGMENT = "dateien";

/**
 * Die Ansicht **eines Artefakts** — `/nachrichten/<id>/dateien/<artefaktId>`.
 *
 * **Eine eigene Route und kein Sheet** (`docs/rohdaten.md` §3, Entscheidung 7).
 * Der Grund ist derselbe wie beim Nachrichtendetail und wiegt hier schwerer: Der
 * Inhalt ist bis zu 610 KB Text (M60) und braucht eine eigene Fläche mit eigenem
 * Bildlauf. Ein Sheet über der Detailansicht ist ausdrücklich eine spätere
 * Zugabe.
 *
 * **Der Inhalt ist Pfad, nicht Abfrage.** Er beschreibt, *was* man sieht, und
 * nicht, wie man es filtert — dieselbe Regel, nach der `nachricht` in der URL
 * steht und `cursor` nicht (`docs/frontend-grundlagen.md` §8).
 *
 * ## Sie trägt bewusst **keine** Abfragezeichenkette
 *
 * Anders als {@link ansichtOhneListe} und {@link ansichtNebenListe}, die den
 * Filterzustand der Liste durchreichen. Drei Gründe, und der erste ist der
 * tragende:
 *
 * 1. **Es soll ein echter Verweis sein.** Der Filterzustand steht nur in
 *    `window.location.search` bereit; ihn beim Rendern zu lesen hieße
 *    `useSearchParams`, und der zwingt die Seite unter eine Suspense-Grenze —
 *    genau deshalb liest `nachricht-seite.tsx` ihn im *Ereignis*. Das geht bei
 *    einer Schaltfläche, nicht bei einem `<a>`. Und ein Dateiverweis, den man
 *    weder mit der mittleren Maustaste öffnen noch kopieren kann, verfehlt
 *    Entscheidung 7: „verlinkbar" ist der ganze Grund für die eigene Route.
 * 2. **Ein geteilter Verweis auf eine Datei handelt von der Datei.** Welches
 *    Zeitfenster derjenige eingestellt hatte, der ihn verschickt, gehört nicht
 *    dazu.
 * 3. Der Weg zurück bleibt gangbar: Die Ansicht führt an die **Nachricht** auf
 *    ihrer eigenen Route, und der Zurück-Knopf des Browsers führt Station für
 *    Station dorthin, wo jemand tatsächlich herkam — samt Filtern.
 *
 *    **Seit dem 14.10.2026 (E‑241) gehen `Escape` und der Verweis denselben
 *    Schritt zurück**, sobald der Verlauf belegt, dass davor dieselbe Nachricht
 *    stand ({@link zeigtNachricht}). Eine Abfragezeichenkette braucht es dafür
 *    weiterhin nicht: Die Adresse von vorher steht im Verlauf.
 */
export function artefaktAnsicht(messageId: string, artefaktId: string): string {
  return `${ROUTEN.nachrichten}/${encodeURIComponent(messageId)}/${DATEIEN_SEGMENT}/${encodeURIComponent(
    artefaktId,
  )}`;
}

/**
 * Die Nachricht auf ihrer eigenen Route — der Weg zurück aus der Dateiansicht,
 * **wenn der Verlauf keinen Schritt zurück belegt** (E‑241): bei einem geteilten
 * Verweis im neuen Tab, in einem Browser ohne Navigation API, aus einer anderen
 * Ansicht. Nach F5 im selben Tab ist der Schritt weiterhin belegt — der Verlauf
 * übersteht das Neuladen.
 */
export function nachrichtAnsicht(messageId: string): string {
  return `${ROUTEN.nachrichten}/${encodeURIComponent(messageId)}`;
}

/**
 * Eine Adresse aus dem Verlauf, zerlegt — oder `null`, wenn es keine ist oder
 * sie eine **andere Herkunft** hat. Nur was nachweislich in dieser Anwendung
 * liegt, taugt als Ziel eines Schritts zurück.
 */
function eigeneAdresse(adresse: string | null, herkunft: string): URL | null {
  if (adresse === null) {
    return null;
  }
  try {
    const url = new URL(adresse);
    return url.origin === herkunft ? url : null;
  } catch {
    return null;
  }
}

/**
 * Die Stücke des Pfads, **erst getrennt, dann dekodiert** — so bleibt ein
 * kodierter Schrägstrich in einer Kennung Teil der Kennung. `null` bei kaputter
 * Kodierung: `decodeURIComponent` wirft dort, und eine fremde Adresse darf das
 * Schließen nicht zum Absturz bringen.
 */
function pfadstuecke(url: URL): string[] | null {
  try {
    return url.pathname
      .split("/")
      .slice(1)
      .map((stueck) => decodeURIComponent(stueck));
  } catch {
    return null;
  }
}

/**
 * Die Ansichten, die eine Nachricht als **Panel neben sich** tragen — alle über
 * denselben Parameter {@link NACHRICHT_PARAMETER}: die Nachrichtenliste (E‑241),
 * die Belegsuche und die Prozessansicht (E‑243).
 */
const PANEL_ANSICHTEN: readonly string[] = [ROUTEN.nachrichten, ROUTEN.suche, ROUTEN.prozesse];

/**
 * **Zeigt diese Adresse dieselbe Nachricht?** — der Beleg für den Schritt
 * zurück aus der Dateiansicht (E‑241).
 *
 * Ja heißt: **dieselbe Herkunft** und entweder eine Ansicht mit dieser Nachricht
 * im Panel (`/nachrichten?…nachricht=<id>…`, seit E‑243 ebenso `/suche?…` und
 * `/prozesse?…`) oder ihre eigene Route (`/nachrichten/<id>…`). Welche Filter
 * daneben stehen, spielt keine Rolle — der Schritt zurück stellt genau die
 * Adresse wieder her, die dort stand: in der Suche samt `begriff` und `feld`, in
 * der Prozessansicht samt Zeitraum, Gliederung und Prozess.
 *
 * **Nein heißt nicht „falsch", sondern „nicht belegt"**, und dann bleibt es beim
 * Verweis auf die eigene Route. **Bis zum Folgeauftrag vom 14.10.2026 galten
 * Prozessansicht und Belegsuche nicht als Beleg** (Auftrag: „Kein Umbau anderer
 * Ansichten"); seit E‑243 gelten sie. Weiterhin verlangt ist **dieselbe
 * Nachricht** — eine Lockerung auf „irgendeine Seite der Anwendung" ist
 * ausgeschlossen.
 *
 * @param adresse die volle Adresse eines Verlaufseintrags, `null` ohne Beleg
 * @param herkunft die eigene Herkunft, `window.location.origin`
 */
export function zeigtNachricht(
  adresse: string | null,
  messageId: string,
  herkunft: string,
): boolean {
  const url = eigeneAdresse(adresse, herkunft);
  if (url === null) {
    return false;
  }
  if (PANEL_ANSICHTEN.includes(url.pathname)) {
    return url.searchParams.get(NACHRICHT_PARAMETER) === messageId;
  }
  const stuecke = pfadstuecke(url);
  return (
    stuecke !== null &&
    stuecke.length === 2 &&
    `/${stuecke[0]}` === ROUTEN.nachrichten &&
    stuecke[1] === messageId
  );
}

/**
 * **Die Datei dieser Nachricht, die eine Adresse aus dem Verlauf zeigt** — als
 * Pfad, **zeichengleich** mit dem `href` ihres Verweises ({@link artefaktAnsicht}),
 * denn daran findet die Nachricht nach der Rückkehr ihren Auslöser (E‑241, S4).
 * Die gewählte Darstellung (`?darstellung=…`) zählt nicht mit.
 *
 * `null` bei einer anderen Nachricht, einer anderen Herkunft oder ohne Beleg.
 */
export function dateiansichtAus(
  adresse: string | null,
  messageId: string,
  herkunft: string,
): string | null {
  const url = eigeneAdresse(adresse, herkunft);
  const stuecke = url === null ? null : pfadstuecke(url);
  if (
    stuecke === null ||
    stuecke.length !== 4 ||
    `/${stuecke[0]}` !== ROUTEN.nachrichten ||
    stuecke[1] !== messageId ||
    stuecke[2] !== DATEIEN_SEGMENT
  ) {
    return null;
  }
  return artefaktAnsicht(messageId, stuecke[3]);
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
