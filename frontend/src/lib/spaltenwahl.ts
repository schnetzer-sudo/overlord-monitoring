/**
 * **Spaltenwahl nach dem Platz** — die Rechnung hinter den Container-Schwellen
 * der Tabellen (`docs/spaltenwahl.md`, E‑147).
 *
 * Die Spaltenmenge einer Tabelle folgt der Breite **ihres Containers** und nicht
 * der des Fensters. Eine Fensterschwelle müsste die Breite der Navigationsspalte
 * und die des Panels festschreiben und bräche beim nächsten Umbau erneut; der
 * Container weiß beides von selbst (M176: bei 768 px erscheint die
 * Navigationsspalte, `main` hat 560 px, und im selben Moment kam eine Spalte
 * dazu, die auf 0 px fiel).
 *
 * ## Was hier steht und was nicht
 *
 * Hier steht nur die **Rechnung**: Eine Schwelle ist die Summe der
 * Mindestbreiten aller Spalten, die ab dort sichtbar sind, plus Rinne — oder,
 * wo ein {@link Umbau} einer Spalte ab dieser Stufe eine feste Breite gibt, die
 * Summe mit dieser Breite (E‑162). Sie ist keine Zahl, die gut aussieht,
 * sondern eine Summe von Messungen. Dazu, seit dem 17.11.2026, die Breite jeder
 * Spalte bei einer Hüllenbreite ({@link spaltenbreiten}) — auch dort, wo eine
 * Klasse ihren Anteil aus der Hülle rechnet.
 *
 * **Die Klassen stehen nicht hier**, sondern als wörtliche Zeichenketten an den
 * Zellen: Tailwind findet eine Klasse nur, wenn sie im Quelltext steht, und eine
 * zusammengesetzte Zeichenkette erzeugte kein CSS. Dass die wörtliche Klasse und
 * die Rechnung dieselbe Zahl tragen, sichert `tests/spaltenwahl.test.tsx` zu —
 * auf den Pixel, in beide Richtungen.
 *
 * Reine Funktionen, ohne React.
 */

/**
 * Pixel je `rem` bei 100 % Wurzelschrift. **Die Mindestbreiten sind in Dichte `m`
 * gemessen** und werden in `rem` geschrieben: Schrift und Spaltenbreite skalieren
 * dann in allen vier Dichtestufen mit derselben Zahl
 * (`docs/visuelles-konzept.md` §5).
 */
export const PX_JE_REM = 16;

export type Spalte = {
  /** Derselbe Schlüssel wie in der Sprachdatei, `…spalten.<schluessel>`. */
  schluessel: string;
  /**
   * Die **gemessene** Mindestbreite in ganzen Pixeln, einschließlich Innenabstand:
   * das Größte aus Kopfbeschriftung (beide Sprachen), längstem Zellinhalt ohne
   * Notumbruch und der Rechnung über eine endliche Beschriftungsmenge —
   * aufgerundet (`docs/spaltenwahl.md` §3).
   */
  mindestbreite: number;
};

export type Spaltenwahl = {
  /** Der Name des Containers: `@container/<container>`. */
  container: string;
  /** Die Spalten, die bei jeder Breite stehen. */
  grundmenge: readonly Spalte[];
  /** Die zuschaltbaren Stufen, in der Reihenfolge, in der sie dazukommen. */
  stufen: readonly (readonly Spalte[])[];
  /**
   * Die eine Spalte **ohne** feste Breite — sofern die Tabelle eine hat. Sie
   * bekommt, was übrig bleibt, und an ihrer Schwelle genau ihre Mindestbreite.
   *
   * **`undefined` heißt: keine** (E‑149, `docs/spaltenwahl.md` §5.5). Dann trägt
   * jede Spalte ihre Mindestbreite, und `table-layout: fixed` teilt den
   * Überschuss **anteilig** auf alle sichtbaren Spalten auf. Eine freie Spalte
   * lohnt sich nur, wo ihr Inhalt den Platz auch nutzt: Ein gekürzter Ablaufname
   * zeigt mit jedem Pixel mehr, eine Reihe kurzer Marken nicht.
   */
  frei?: string;
  /**
   * Was sich an einer Stufe für Spalten ändert, die **schon dastehen** — sofern
   * die Tabelle so etwas hat (E‑162, `docs/nachrichtenliste.md` §8.1). Ohne
   * Eintrag trägt jede feste Spalte bei jeder Breite ihre Mindestbreite.
   */
  umbau?: readonly Umbau[];
  /** Was zwischen Containerkante und Spaltensumme liegt, in Pixeln. */
  rinne: number;
};

/**
 * **Ein Umbau an einer Stufe** (E‑162): Ab ihrer Schwelle tragen Spalten, die
 * schon dastehen, eine andere Breite, und die freie Spalte kann wechseln.
 *
 * Der Fall, für den es ihn gibt, ist die Nachrichtenliste: Sobald das Projekt
 * dazukommt, wird die Statusspalte breit genug für den Schritt neben der
 * Plakette, und der Ablauf bekommt an der Schwelle die Breite seines längsten
 * Namens.
 *
 * **Korrektur vom 17.11.2026:** Bis dahin nahm ab der Schwelle das Projekt die
 * Überbreite auf, als freie Spalte am Zeilenende. Projektnamen sind kurz, und
 * am breiten Fenster stand rechts eine große Lücke, während der Schritt neben
 * der Plakette kürzte. Seither **teilen Status und Ablauf den Überschuss**
 * ({@link Umbau.anteilig}), Zeitpunkt und Projekt bleiben auf ihrer
 * Mindestbreite — E‑149, auf zwei Spalten beschränkt.
 */
export type Umbau = {
  /** Die Stufe, ab deren Schwelle der Umbau gilt. */
  stufe: number;
  /**
   * Breiten in ganzen Pixeln, die **an der Schwelle** gelten — auch für die
   * Spalte, die bis dahin frei war. Sie gehen in die Schwelle ein wie
   * Mindestbreiten. Ohne {@link Umbau.anteilig} gelten sie bei jeder Breite ab dort.
   */
  breiten: Readonly<Record<string, number>>;
  /**
   * Die Spalten, die den **Überschuss über der Schwelle** unter sich teilen, im
   * Verhältnis ihrer Breiten an der Schwelle (17.11.2026). Jede übrige sichtbare
   * Spalte bleibt auf ihrer Breite. Ohne Eintrag geht der Überschuss an die
   * freie Spalte.
   *
   * **`table-layout: fixed` teilt so nicht:** Es gäbe den Überschuss allen
   * bemaßten Spalten, auch Zeitpunkt und Projekt. Die Klasse an jeder dieser
   * Spalten außer der freien rechnet ihren Anteil deshalb selbst, aus der Breite
   * der Hülle (`100cqw`); die freie bekommt den Rest, und das ist genau ihr
   * Anteil.
   */
  anteilig?: readonly string[];
  /**
   * Die freie Spalte ab dort. Sie trägt keine Breitenklasse und bekommt, was die
   * übrigen lassen — an der Schwelle genau ihre Breite aus {@link Umbau.breiten}
   * bzw. ihre Mindestbreite. Steht sie in {@link Umbau.anteilig}, ist der Rest
   * ihr Anteil.
   */
  frei: string;
};

/**
 * Die Breite, mit der eine Spalte in die Schwelle einer Stufe eingeht: die aus
 * dem letzten Umbau bis zu dieser Stufe, sonst ihre Mindestbreite.
 */
function breiteAufStufe(wahl: Spaltenwahl, spalte: Spalte, stufe: number): number {
  let breite = spalte.mindestbreite;
  for (const umbau of wahl.umbau ?? []) {
    const px = umbau.breiten[spalte.schluessel];
    if (umbau.stufe <= stufe && px !== undefined) {
      breite = px;
    }
  }
  return breite;
}

const summe = (spalten: readonly Spalte[]) =>
  spalten.reduce((gesamt, spalte) => gesamt + spalte.mindestbreite, 0);

/** Die Breite der Grundmenge in Pixeln — darunter gilt die Bauform der Tabelle. */
export function grundmengeInPixeln(wahl: Spaltenwahl): number {
  return summe(wahl.grundmenge) + wahl.rinne;
}

/**
 * Die Schwelle einer Stufe in Pixeln: **die Summe der Breiten aller Spalten, die
 * ab dort sichtbar sind, plus Rinne** — jede mit ihrer Mindestbreite oder mit der
 * Breite, die ein Umbau bis zu dieser Stufe ihr gibt.
 */
export function schwelleInPixeln(wahl: Spaltenwahl, stufe: number): number {
  if (!Number.isInteger(stufe) || stufe < 0 || stufe >= wahl.stufen.length) {
    throw new RangeError(`Stufe ${stufe} gibt es nicht (${wahl.stufen.length} Stufen)`);
  }
  const sichtbar = [...wahl.grundmenge, ...wahl.stufen.slice(0, stufe + 1).flat()];
  return sichtbar.reduce((g, s) => g + breiteAufStufe(wahl, s, stufe), 0) + wahl.rinne;
}

/** Die freie Spalte bei einer Containerbreite (Pixel) — `undefined` heißt: keine. */
export function freieSpalte(wahl: Spaltenwahl, breite: number): string | undefined {
  let frei = wahl.frei;
  for (const umbau of wahl.umbau ?? []) {
    if (breite >= schwelleInPixeln(wahl, umbau.stufe)) {
      frei = umbau.frei;
    }
  }
  return frei;
}

/** Die Stufe, in der eine Spalte dazukommt — `null` für die Grundmenge. */
export function stufeVon(wahl: Spaltenwahl, schluessel: string): number | null {
  if (wahl.grundmenge.some((spalte) => spalte.schluessel === schluessel)) {
    return null;
  }
  const stufe = wahl.stufen.findIndex((s) => s.some((spalte) => spalte.schluessel === schluessel));
  if (stufe < 0) {
    throw new RangeError(`Spalte ${schluessel} gehört nicht zu ${wahl.container}`);
  }
  return stufe;
}

/** Die Mindestbreite einer Spalte in Pixeln. */
export function mindestbreiteVon(wahl: Spaltenwahl, schluessel: string): number {
  const spalte = [...wahl.grundmenge, ...wahl.stufen.flat()].find(
    (s) => s.schluessel === schluessel,
  );
  if (spalte === undefined) {
    throw new RangeError(`Spalte ${schluessel} gehört nicht zu ${wahl.container}`);
  }
  return spalte.mindestbreite;
}

/** Die Spalten, die bei einer Containerbreite (Pixel) sichtbar sind, in Stufenfolge. */
export function sichtbareSpalten(wahl: Spaltenwahl, breite: number): Spalte[] {
  const sichtbar = [...wahl.grundmenge];
  wahl.stufen.forEach((stufe, index) => {
    if (breite >= schwelleInPixeln(wahl, index)) {
      sichtbar.push(...stufe);
    }
  });
  return sichtbar;
}

/**
 * **Die Breite jeder sichtbaren Spalte** bei einer Containerbreite (Pixel), in
 * Stufenfolge — das, was die Klassen an den Zellen im Browser ergeben sollen
 * (17.11.2026). `tests/spaltenwahl.test.tsx` hält die gerenderten Klassen
 * dagegen.
 *
 * - Jede Spalte trägt ihre Mindestbreite oder die Breite, die ihr der letzte
 *   erreichte {@link Umbau} gibt.
 * - **Teilen Spalten den Überschuss** ({@link Umbau.anteilig}), bekommt jede
 *   davon ihren Anteil an dem, was die Hülle über alle übrigen Breiten hinaus
 *   hat — im Verhältnis ihrer Breiten an der Schwelle.
 * - **Sonst nimmt die freie Spalte den Rest**, und zwar auch unter der
 *   Grundmenge — dort nie weniger als 0 px, die Tabelle läuft dann über.
 * - **Ohne sichtbare freie Spalte** teilt sich der Überschuss auf alle
 *   sichtbaren Spalten, ein Faktor für alle (E‑149); unter der Summe bleibt
 *   jede auf ihrer Breite.
 *
 * **Eine eigene Bauform unter der Grundmenge kennt sie nicht** — die des
 * Katalogs (`KATALOG_BAUFORM`) steht in seinem Feature.
 */
export function spaltenbreiten(wahl: Spaltenwahl, breite: number): Map<string, number> {
  const sichtbar = sichtbareSpalten(wahl, breite);
  let stufe = -1;
  wahl.stufen.forEach((_, index) => {
    if (breite >= schwelleInPixeln(wahl, index)) {
      stufe = index;
    }
  });
  const umbau = (wahl.umbau ?? []).filter((u) => u.stufe <= stufe).at(-1);
  const anteilig = umbau?.anteilig ?? [];
  const frei = freieSpalte(wahl, breite);

  const breiten = new Map(sichtbar.map((s) => [s.schluessel, breiteAufStufe(wahl, s, stufe)]));
  const summe = [...breiten.values()].reduce((g, px) => g + px, 0);
  const platz = breite - wahl.rinne;

  if (anteilig.length > 0) {
    const summeAnteilig = anteilig.reduce((g, s) => g + (breiten.get(s) ?? 0), 0);
    for (const schluessel of anteilig) {
      const px = breiten.get(schluessel)!;
      breiten.set(schluessel, px + ((platz - summe) * px) / summeAnteilig);
    }
  } else if (frei !== undefined && breiten.has(frei)) {
    const fest = summe - breiten.get(frei)!;
    breiten.set(frei, Math.max(0, platz - fest));
  } else if (platz > summe) {
    for (const [schluessel, px] of breiten) {
      breiten.set(schluessel, (px * platz) / summe);
    }
  }
  return breiten;
}
