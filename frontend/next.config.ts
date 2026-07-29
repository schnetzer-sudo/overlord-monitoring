import type { NextConfig } from "next";

/**
 * Adresse des Backends. Kommt aus der Umgebung, nicht aus dem Code — der
 * Standard gilt ausschließlich für die lokale Entwicklung gegen
 * `./mvnw spring-boot:run`.
 *
 * Vorlage: `.env.example`. Eigene Werte in `.env.local` (nicht eingecheckt).
 */
const BACKEND = process.env.OVERLORD_BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
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
