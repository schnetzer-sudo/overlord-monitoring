import { NextResponse, type NextRequest } from "next/server";

import { ROUTEN, WEITER_PARAMETER } from "@/lib/routen";

/**
 * Routensperre — **Bequemlichkeit, kein Schutz.**
 *
 * Diese Datei prüft ausschließlich, ob ein Sitzungs-Cookie **vorhanden** ist.
 * Nicht, ob es gültig ist. Nicht, welche Rolle dahintersteht. Nicht, welcher
 * Mandant aktiv ist. Sie kann das gar nicht: Das Cookie ist `HttpOnly` und sein
 * Inhalt ist eine undurchsichtige Sitzungs-ID, die nur das Backend auflösen
 * kann.
 *
 * Ihr einziger Zweck ist, einem nicht angemeldeten Nutzer den Ladevorgang einer
 * Seite zu ersparen, die ihm sofort ein `401` einbrächte.
 *
 * **Hier wird niemals eine Berechtigung geprüft.** Wer versucht ist, an dieser
 * Stelle eine Rolle oder einen Mandanten abzufragen, baut die
 * Sicherheitsentscheidung in den Browser des Nutzers — verbindlich entscheidet
 * ausschließlich das Backend, in jedem einzelnen SQL-Statement.
 *
 * Seit Next.js 16 heißt diese Datei `proxy.ts` statt `middleware.ts`; die
 * Aufgabe ist dieselbe geblieben.
 */

/** Name aus `server.servlet.session.cookie.name` im Backend. */
const SITZUNGS_COOKIE = "OVERLORD_SESSION";

export function proxy(request: NextRequest): NextResponse {
  const angemeldet = request.cookies.has(SITZUNGS_COOKIE);
  const pfad = request.nextUrl.pathname;

  if (pfad === ROUTEN.anmeldung) {
    // Wer ein Sitzungs-Cookie hat, braucht die Anmeldeseite nicht — es sei denn,
    // er wurde gerade hierher geschickt. Genau das verhindert eine Schleife bei
    // einem **abgelaufenen** Cookie: Das Backend antwortet mit 401, der
    // QueryClient leitet mit `weiter` hierher, und dann bleiben wir hier.
    if (angemeldet && !request.nextUrl.searchParams.has(WEITER_PARAMETER)) {
      return NextResponse.redirect(new URL(ROUTEN.startseite, request.url));
    }
    return NextResponse.next();
  }

  if (!angemeldet) {
    const ziel = new URL(ROUTEN.anmeldung, request.url);
    ziel.searchParams.set(WEITER_PARAMETER, `${pfad}${request.nextUrl.search}`);
    return NextResponse.redirect(ziel);
  }

  return NextResponse.next();
}

/*
 * Kein `Cache-Control: no-store` von hier aus — es hätte keine Wirkung.
 *
 * Gemessen am 29.07.2026 gegen Next.js 16.2.11: Eigene Kopfzeilen aus dieser
 * Datei kommen beim Browser an, `Cache-Control` aber nicht. Next.js setzt für
 * dynamisch gerenderte Seiten seinen eigenen Wert (`no-cache, must-revalidate`)
 * und überschreibt sowohl die Kopfzeile von hier als auch `headers()` aus
 * `next.config.ts`. Beides wurde ausprobiert, beides wirkungslos.
 *
 * `no-cache` verhindert das Ausliefern aus dem HTTP-Zwischenspeicher, aber
 * **nicht** die Wiederherstellung aus dem Zurück-Vorwärts-Zwischenspeicher
 * (bfcache) — dafür bräuchte es `no-store`. Deshalb liegt der Schutz gegen
 * „Zurück zeigt die Seite des abgemeldeten Nutzers" im Anwendungsrahmen: Er
 * lädt neu, sobald die Seite aus dem bfcache zurückkommt.
 */

export const config = {
  // `/api` bleibt ausgespart: Diese Aufrufe gehören dem Rewrite auf das Backend.
  // Würden sie hier umgeleitet, bekäme der HTTP-Client HTML statt eines `401`
  // und die Fehlerbehandlung liefe ins Leere.
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
