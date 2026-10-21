import type { NextConfig } from "next";

/**
 * Adresse des Backends. Kommt aus der Umgebung, nicht aus dem Code — der
 * Standard gilt ausschließlich für die lokale Entwicklung gegen
 * `./mvnw spring-boot:run`.
 *
 * Vorlage: `.env.example`. Eigene Werte in `.env.local` (nicht eingecheckt).
 */
const BACKEND = process.env.OVERLORD_BACKEND_URL ?? "http://localhost:8080";

/**
 * **Zeitgrenze der `/api`-Weiterleitung: 300 s statt der Vorgabe von 30 s**
 * (1.1.0, `docs/betrieb.md` §5a).
 *
 * Next.js 16.2.11 bricht eine Weiterleitung ab, wenn das Backend so lange
 * nichts schickt (`proxyTimeout`, Vorgabe 30.000 ms), und antwortet dann mit
 * `500`. Der Assistent schickt seine Antwort am Stück, ohne Streaming
 * (E‑1043, E‑1075): Bis sie fertig ist, fließt kein Byte. Eine Nachricht kann
 * bis zu vier Modellaufrufe brauchen (`werkzeugrunden-je-nachricht` 3 plus die
 * Antwort), jeder bis zu 30 s (`zeitgrenze`) und bei Überlastung ein zweiter
 * Versuch nach 1 s: 4 × (30 + 1 + 30) s = 244 s. Die 300 s liegen darüber.
 *
 * Alle übrigen Endpunkte enden weit früher (Laufzeitgrenze der Datenbank 10 s
 * bzw. 30 s); für sie ändert die höhere Grenze nichts. Wer `zeitgrenze` oder
 * `werkzeugrunden-je-nachricht` im Backend anhebt, rechnet hier nach.
 */
const WEITERLEITUNG_ZEITGRENZE_MS = 300_000;

const nextConfig: NextConfig = {
  experimental: {
    proxyTimeout: WEITERLEITUNG_ZEITGRENZE_MS,
  },

  /**
   * **Der Browser spricht ausschließlich mit Next.js.**
   *
   * Jeder `/api`-Aufruf geht an die Next-Adresse und wird von hier an das
   * Backend weitergereicht. Damit ist alles gleiche Herkunft: kein CORS, kein
   * `credentials: "include"`, keine Sonderregel für `SameSite`, keine
   * Cookie-Domain, die zwischen Entwicklung und Betrieb auseinanderläuft.
   *
   * Im Betrieb zieht ohnehin ein Reverse Proxy Frontend und Backend unter eine
   * Domain. Der Rewrite bildet genau das schon lokal ab — was in der
   * Entwicklung funktioniert, funktioniert deshalb auch danach.
   *
   * Bewusst hier und nicht in einer Route-Handler-Datei: Ein eigener Handler
   * müsste Kopfzeilen, Cookies, Statuscodes und Datenströme selbst durchreichen
   * und wäre eine zweite Stelle, an der etwas verloren gehen kann.
   */
  async rewrites() {
    return [
      {
        source: "/api/:pfad*",
        destination: `${BACKEND}/api/:pfad*`,
      },
    ];
  },
};

export default nextConfig;
