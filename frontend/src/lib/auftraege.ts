/**
 * Der Abfrageschlüssel der Aufträge (`GET /api/tasks`) — **die Naht** zwischen
 * Übersicht und Assistent (Linie `assistent`, A4, F19 in
 * `docs/assistent-frontend.md` §12.3).
 *
 * Die Übersicht zeigt die Liste im Block „Ihre Aufträge“, und der Assistent
 * lässt sie nach dem Anlegen neu laden (E‑1109). Ein Feature importiert nicht
 * aus dem Nachbarfeature (`docs/frontend-grundlagen.md` §8) — deshalb steht
 * hier nur der Schlüssel; Typen und Aufrufe bleiben in `features/dashboard`.
 *
 * **Kein Mandant im Schlüssel** (E‑1123): Beim Mandantenwechsel wird der ganze
 * Zwischenspeicher geleert und nicht invalidiert (`lib/zwischenspeicher.ts`),
 * und deshalb steht der Mandant in keinem Schlüssel des Projekts.
 */
export const AUFTRAEGE_SCHLUESSEL = {
  liste: ["auftraege", "liste"] as const,
};
