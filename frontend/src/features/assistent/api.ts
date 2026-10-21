import { entferne, hole, sende } from "@/lib/http";

/**
 * Der Assistent im Frontend (A3, `docs/assistent-frontend.md`): die Endpunkte
 * aus A2 (`docs/assistent.md` §6) und die beiden der Aufträge, die die Karte
 * braucht (`docs/tasks.md` §6.2, `docs/assistent.md` §6.5).
 *
 * **Keine Anfrage hier trägt eine Mandanten-ID** (Regel M1). Den Mandanten kennt
 * das Backend aus der Sitzung; die Karte nennt ihn nur (E‑1069).
 */

/** Antwort von `GET /api/assistent`. */
export type AssistentStand = {
  /** Schalter an, Schlüssel gesetzt, mindestens ein Typ angeboten (E‑1068). */
  verfuegbar: boolean;
  /** Die Namen der angebotenen Typen, nach Kennung sortiert — für die Begrüßung. */
  typen: string[];
};

/** Ein Feld, das bei der Aktion des Vorschlags vorkommt (E‑1041). */
export type VorschlagFeld = {
  name: string;
  label: string;
  /** Was das Feld bedeutet, etwa „Bei Anpassung heißt leer: der Wert wird entfernt.“ */
  hinweis: string | null;
};

/** Feldname auf Wert. Leer ist `null` oder leerer Text (`docs/tasks.md` §5.3). */
export type Werte = Readonly<Record<string, string | null>>;

export type VorschlagEintrag = {
  /** **Jedes** Feld aus {@link Vorschlag.felder}, leere als `null`. */
  werte: Werte;
  /** Der Schlüssel nach der Definition, `null` ohne Schlüssel. */
  schluessel: string | null;
};

/** Ein gültiger Vorschlag des Assistenten — die Karte (`docs/assistent.md` §4.7). */
export type Vorschlag = {
  typ: string;
  name: string;
  aktion: string;
  felder: VorschlagFeld[];
  eintraege: VorschlagEintrag[];
};

/** Antwort auf eine Nachricht. */
export type NachrichtAntwort = {
  text: string;
  vorschlag: Vorschlag | null;
};

/**
 * Ein Fehler der Prüfung (`docs/tasks.md` §5.3): `index` ab 0 oder `null` für
 * den Auftrag als Ganzes, `feld` oder `null`, ein stabiler `code` und der
 * Klartext des Backends. Angezeigt wird `meldung`, unverändert (E‑1071).
 */
export type Prueffehler = {
  index: number | null;
  feld: string | null;
  code: string;
  meldung: string;
};

/** Der Auftrag nach dem Anlegen (`docs/tasks.md` §6.5), soweit die Karte ihn liest. */
export type AngelegterAuftrag = {
  taskId: string;
  einordnung: string;
  eintraege: { werte: Readonly<Record<string, string>> }[];
};

/** Rumpf von `POST /api/tasks/pruefung` — derselbe wie beim Anlegen, ohne `anfrageId`. */
export type Pruefanfrage = {
  typ: string;
  aktion: string;
  eintraege: Werte[];
};

export const ASSISTENT_SCHLUESSEL = {
  /**
   * **Je Mandant ein Eintrag** (1.1.0, `docs/tasks.md` §3.9): Die Verfügbarkeit
   * hängt an der Freischaltung des Mandanten. Nach einem Wechsel liest das
   * Symbol neu, auch wenn der Zwischenspeicher dabei nicht geleert würde. Der
   * Mandant steht nur im Schlüssel, nie in der Anfrage (Regel M1).
   */
  stand: (mandantId: string) => ["assistent", "stand", mandantId] as const,
};

export function holeStand(): Promise<AssistentStand> {
  return hole<AssistentStand>("/assistent");
}

export function eroeffneGespraech(): Promise<{ gespraechId: string }> {
  return sende<{ gespraechId: string }>("/assistent/gespraeche");
}

export function sendeNachricht(gespraechId: string, text: string): Promise<NachrichtAntwort> {
  return sende<NachrichtAntwort>(
    `/assistent/gespraeche/${encodeURIComponent(gespraechId)}/nachrichten`,
    { text },
  );
}

export function beendeGespraech(gespraechId: string): Promise<void> {
  return entferne<void>(`/assistent/gespraeche/${encodeURIComponent(gespraechId)}`);
}

export async function pruefeAuftrag(anfrage: Pruefanfrage): Promise<Prueffehler[]> {
  const antwort = await sende<{ errors?: unknown }>("/tasks/pruefung", anfrage);
  return prueffehlerAus(antwort?.errors);
}

export function legeAuftragAn(
  anfrage: Pruefanfrage & { anfrageId: string },
): Promise<AngelegterAuftrag> {
  return sende<AngelegterAuftrag>("/tasks", anfrage);
}

function alsText(wert: unknown): string | null {
  return typeof wert === "string" && wert.length > 0 ? wert : null;
}

/**
 * Die Fehlerliste der Prüfung — aus der Antwort von `POST /api/tasks/pruefung`
 * und aus dem Rumpf eines `400` von `POST /api/tasks`, beide in derselben Form.
 * Ein Eintrag ohne `meldung` sagt dem Kunden nichts und fällt weg.
 */
export function prueffehlerAus(rohwert: unknown): Prueffehler[] {
  if (!Array.isArray(rohwert)) {
    return [];
  }
  return rohwert.flatMap((eintrag: unknown) => {
    if (typeof eintrag !== "object" || eintrag === null) {
      return [];
    }
    const satz = eintrag as Record<string, unknown>;
    const meldung = alsText(satz.meldung);
    if (meldung === null) {
      return [];
    }
    return [
      {
        index: typeof satz.index === "number" && Number.isInteger(satz.index) ? satz.index : null,
        feld: alsText(satz.feld),
        code: alsText(satz.code) ?? "",
        meldung,
      },
    ];
  });
}

/**
 * Die `anfrageId` einer Karte (E‑1072): eine UUID der Version 4.
 *
 * `crypto.randomUUID` gibt es nur in einem sicheren Kontext (HTTPS oder
 * `localhost`); hinter einem Proxy ohne TLS fehlte sie still. Der Rückfall baut
 * dieselbe Form aus `crypto.getRandomValues`, das es überall gibt.
 */
export function neueAnfrageId(): string {
  if (typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
