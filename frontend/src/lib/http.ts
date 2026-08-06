/**
 * Der einzige Weg, auf dem dieses Frontend mit dem Backend spricht.
 *
 * **Gleiche Herkunft, kein CORS.** Der Browser ruft immer `/api/…` auf der
 * Next.js-Adresse auf; ein Rewrite in `next.config.ts` leitet das an das Backend
 * weiter. Deshalb kein `credentials: "include"`, keine CORS-Konfiguration und
 * keine Sonderregel für `SameSite` — Entwicklung und Betrieb verhalten sich
 * gleich, weil im Betrieb ohnehin ein Reverse Proxy beides unter eine Domain
 * zieht.
 *
 * **CSRF.** Das Backend legt den Token per Cookie ab (`XSRF-TOKEN`, nicht
 * `HttpOnly`) und erwartet ihn als Kopfzeile `X-XSRF-TOKEN`. Sein
 * `CsrfCookieFilter` hängt vor der Autorisierung, das Cookie entsteht also auch
 * bei einer `401`-Antwort. Genau das nutzt {@link csrfKopf}: Fehlt der Token vor
 * dem ersten schreibenden Aufruf, wird er mit einem `GET` geholt — statt den
 * ersten Versuch scheitern zu lassen.
 */

/** Basis aller Aufrufe. Relativ, weil der Rewrite gleiche Herkunft herstellt. */
const BASIS = "/api";

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_KOPF = "X-XSRF-TOKEN";

/** Ein feldbezogener Prüffehler aus `errors: [{ feld, meldung }]`. */
export type Feldfehler = { feld: string; meldung: string };

/**
 * Eine Fehlerantwort im Format RFC 9457, typisiert gelesen.
 *
 * Angezeigt wird **nach {@link typ}**, nicht nach {@link detail}: Der Typ ist der
 * maschinenlesbare Schlüssel und lässt sich übersetzen, der Text nicht. Fehlt
 * eine Übersetzung, ist `detail` die Rückfallebene — deutsch, aber richtig.
 */
export class ProblemFehler extends Error {
  readonly status: number;
  readonly typ: string;
  readonly titel: string | undefined;
  readonly detail: string | undefined;
  readonly traceId: string | undefined;
  readonly felder: readonly Feldfehler[];
  /**
   * Die **eigenen** Felder der Antwort neben `type`, `title` und `detail` —
   * RFC 9457 lässt sie ausdrücklich zu.
   *
   * Sie existieren, damit die Oberfläche eine konkrete Meldung bauen kann, ohne
   * `detail` zu zerlegen und ohne dieselbe Zahl ein zweites Mal zu kennen: Steht
   * die geltende Grenze im Rumpf, ändert man sie im Backend, ohne dass ein
   * Sprachtext hinterherläuft.
   */
  readonly angaben: Readonly<Record<string, unknown>>;

  constructor(werte: {
    status: number;
    typ: string;
    titel?: string;
    detail?: string;
    traceId?: string;
    felder?: readonly Feldfehler[];
    angaben?: Readonly<Record<string, unknown>>;
  }) {
    super(werte.detail ?? werte.titel ?? werte.typ);
    this.name = "ProblemFehler";
    this.status = werte.status;
    this.typ = werte.typ;
    this.titel = werte.titel;
    this.detail = werte.detail;
    this.traceId = werte.traceId;
    this.felder = werte.felder ?? [];
    this.angaben = werte.angaben ?? {};
  }

  /** Eine Zahl aus {@link angaben} — oder `undefined`, wenn sie fehlt oder keine ist. */
  zahl(name: string): number | undefined {
    const wert = this.angaben[name];
    return typeof wert === "number" && Number.isFinite(wert) ? wert : undefined;
  }
}

/** Statuscodes, bei denen ein zweiter Versuch nichts ändern kann. */
const ENDGUELTIG = [401, 403, 404];

export function istEndgueltig(fehler: unknown): boolean {
  return fehler instanceof ProblemFehler && ENDGUELTIG.includes(fehler.status);
}

/**
 * Die Suche ist an der Zeitgrenze der Datenbank abgebrochen worden.
 *
 * Der eine Fall, in dem ein zweiter Versuch nicht nur nichts bringt, sondern
 * **schadet**: Er stellt dieselbe Abfrage noch einmal, und sie läuft wieder in
 * dieselbe Grenze — zehn weitere Sekunden auf der Produktionsdatenbank für eine
 * Antwort, die schon feststeht. Was hilft, ist ein engerer Zeitraum oder ein
 * schärferer Begriff, und das kann nur der Nutzer entscheiden.
 */
export const SUCHE_ABGEBROCHEN = "suche-abgebrochen";

export function istZeitgrenze(fehler: unknown): boolean {
  return fehler instanceof ProblemFehler && fehler.typ === SUCHE_ABGEBROCHEN;
}

export function istNichtAngemeldet(fehler: unknown): boolean {
  return fehler instanceof ProblemFehler && fehler.status === 401;
}

function cookieWert(name: string): string | undefined {
  if (typeof document === "undefined") {
    return undefined;
  }
  const treffer = document.cookie
    .split("; ")
    .find((eintrag) => eintrag.startsWith(`${name}=`))
    ?.slice(name.length + 1);
  return treffer ? decodeURIComponent(treffer) : undefined;
}

async function csrfKopf(): Promise<Record<string, string>> {
  let token = cookieWert(CSRF_COOKIE);
  if (!token) {
    // Ein beliebiges GET genügt: Der Filter im Backend setzt das Cookie auch
    // dann, wenn die Antwort 401 lautet. Fehler werden hier bewusst
    // verschluckt — der eigentliche Aufruf meldet gleich selbst, was los ist.
    await fetch(`${BASIS}/auth/me`, { method: "GET", cache: "no-store" }).catch(() => undefined);
    token = cookieWert(CSRF_COOKIE);
  }
  return token ? { [CSRF_KOPF]: token } : {};
}

function alsText(wert: unknown): string | undefined {
  return typeof wert === "string" && wert.length > 0 ? wert : undefined;
}

/** Der letzte Pfadabschnitt der Problemtyp-URI — der Schlüssel für die Übersetzung. */
function typAus(rohwert: unknown, status: number): string {
  const uri = alsText(rohwert);
  if (uri && uri !== "about:blank") {
    const abschnitt = uri.split("/").pop();
    if (abschnitt) {
      return abschnitt;
    }
  }
  return status >= 500 ? "technischer-fehler" : "anfrage-ungueltig";
}

function felderAus(rohwert: unknown): Feldfehler[] {
  if (!Array.isArray(rohwert)) {
    return [];
  }
  return rohwert.flatMap((eintrag) => {
    if (typeof eintrag !== "object" || eintrag === null) {
      return [];
    }
    const satz = eintrag as Record<string, unknown>;
    const feld = alsText(satz.feld);
    const meldung = alsText(satz.meldung);
    return feld && meldung ? [{ feld, meldung }] : [];
  });
}

/** Die Felder, die RFC 9457 selbst vergibt. Alles andere gehört dem Fall. */
const RFC_FELDER = ["type", "title", "status", "detail", "instance", "traceId", "errors"];

function angabenAus(koerper: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(koerper).filter(([name]) => !RFC_FELDER.includes(name)));
}

async function fehlerAus(antwort: Response): Promise<ProblemFehler> {
  const typKopf = antwort.headers.get("content-type") ?? "";
  if (!typKopf.includes("problem+json") && !typKopf.includes("application/json")) {
    // Kommt nicht vom Backend, sondern etwa vom Rewrite selbst — dann steht dort
    // HTML, das niemandem hilft.
    return new ProblemFehler({
      status: antwort.status,
      typ: antwort.status >= 500 ? "technischer-fehler" : "anfrage-ungueltig",
    });
  }
  const koerper = (await antwort.json().catch(() => ({}))) as Record<string, unknown>;
  return new ProblemFehler({
    status: antwort.status,
    typ: typAus(koerper.type, antwort.status),
    titel: alsText(koerper.title),
    detail: alsText(koerper.detail),
    traceId: alsText(koerper.traceId),
    felder: felderAus(koerper.errors),
    angaben: angabenAus(koerper),
  });
}

async function anfrage<T>(pfad: string, init: RequestInit): Promise<T> {
  let antwort: Response;
  try {
    antwort = await fetch(`${BASIS}${pfad}`, { ...init, cache: "no-store" });
  } catch {
    // Netzwerk weg, Backend aus, Rewrite ins Leere. Kein Statuscode, also auch
    // kein Problemtyp — die Oberfläche zeigt dafür einen eigenen Text.
    throw new ProblemFehler({ status: 0, typ: "netzwerk" });
  }

  if (!antwort.ok) {
    throw await fehlerAus(antwort);
  }

  if (antwort.status === 204 || antwort.headers.get("content-length") === "0") {
    return undefined as T;
  }
  return (await antwort.json()) as T;
}

/** Lesender Aufruf. Braucht keinen CSRF-Token. */
export function hole<T>(pfad: string): Promise<T> {
  return anfrage<T>(pfad, { method: "GET", headers: { Accept: "application/json" } });
}

/** Schreibender Aufruf. Holt den CSRF-Token vorher, statt am ersten Versuch zu scheitern. */
export async function sende<T>(pfad: string, koerper?: unknown): Promise<T> {
  const kopf = await csrfKopf();
  return anfrage<T>(pfad, {
    method: "POST",
    headers: {
      Accept: "application/json",
      ...(koerper === undefined ? {} : { "Content-Type": "application/json" }),
      ...kopf,
    },
    body: koerper === undefined ? undefined : JSON.stringify(koerper),
  });
}
