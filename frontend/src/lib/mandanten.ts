import { hole } from "./http";

/**
 * Die wählbaren Mandanten — **eine Quelle, zwei Verwender.**
 *
 * `GET /api/mandanten` liefert *genau* die Menge, aus der der angemeldete
 * Nutzer wählen darf: für `ADMIN` alle aus `GlassfishDB.Mandant`, für `MANDANT`
 * die eigenen Einträge aus `app_user_mandant`. **Die Liste ist selbst eine
 * Auskunft** — das Frontend filtert nichts nach und kennt keine Regel darüber,
 * wer was sehen darf (`docs/mandantentrennung.md` §1).
 *
 * ## Warum das hier liegt und nicht in `features/sitzung`
 *
 * Seit Schritt 3 gehörte der Aufruf dorthin, weil ihn nur die Mandantenauswahl
 * brauchte. Seit dem 24.08.2026 braucht ihn ein zweiter: Die Benutzerverwaltung
 * pflegt die Mandantenmenge fremder Konten und wählt aus derselben Liste.
 *
 * **Ein Feature importiert nicht aus einem Nachbarfeature**
 * (`docs/frontend-grundlagen.md` §8), und dieselbe Datei nennt auch, was
 * stattdessen gilt: *„Kommt in Schritt 10 eine eigene Prozessansicht, wandert
 * der gemeinsame Teil nach `components/` oder `lib/` — nicht ins
 * Nachbarfeature."* Genau dieser Fall ist eingetreten, nur früher und an einer
 * anderen Stelle.
 *
 * **Der Schlüssel wandert mit, und das ist der eigentliche Punkt.** Blieben zwei
 * Schlüssel nebeneinander stehen, hielte der Zwischenspeicher dieselbe Antwort
 * zweimal und holte sie zweimal — für eine Liste, die sich zwischen den beiden
 * Ansichten nicht unterscheidet.
 *
 * ## `SYSTEM` und `WOC` werden **nicht** ausgesiebt
 *
 * Beide sind technische Mandanten und kein Kunde
 * (`docs/PROJEKTBESCHREIBUNG.md` §3). Die Mandantenauswahl aus Schritt 3 zeigt
 * sie trotzdem, und das bleibt so: Wer für sie berechtigt ist, muss zu ihnen
 * wechseln können, und wessen Konto für sie zuständig ist, muss ihm zugeordnet
 * werden können. Eine zweite Behandlung nur für die Benutzerverwaltung wäre eine
 * Regel im Browser darüber, wer was sehen darf — genau das, was die
 * Mandantenauswahl seit Schritt 3 ausdrücklich nicht tut.
 */
export type Mandant = {
  /** Sprechender Code aus dem Altsystem, etwa `VOTG` — keine UUID. */
  id: string;
  name: string;
};

/**
 * Der Schlüssel im Zwischenspeicher. **Nicht unter `sitzung`**, seit ihn zwei
 * Features lesen — er beschreibt die Liste und nicht ihren ersten Verwender.
 */
export const MANDANTEN_SCHLUESSEL = ["mandanten"] as const;

/** Genau die Menge, aus der dieser Nutzer wählen darf — die Liste ist selbst eine Auskunft. */
export function holeMandanten(): Promise<Mandant[]> {
  return hole<Mandant[]>("/mandanten");
}
