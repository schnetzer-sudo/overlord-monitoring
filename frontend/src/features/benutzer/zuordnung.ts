import type { Mandant } from "@/lib/mandanten";

/**
 * Die Entscheidungen der Mandantenpflege — **reine Funktionen, frei von React.**
 *
 * `PUT /api/admin/users/{id}/tenants` ist die **dritte und derzeit letzte
 * Ausnahme von Regel M1** (E4): Der Endpunkt nimmt Mandanten-IDs entgegen. Er
 * ist zulässig aus demselben Grund wie die beiden anderen — hier wird eine
 * Berechtigung **definiert** und kein Datenausschnitt **abgefragt**.
 */

/** Ein Mandant, wie er in der Auswahl steht. */
export type Wahlmoeglichkeit = {
  id: string;
  /** Der Anzeigename, oder `null` für einen, der nur noch am Konto hängt. */
  name: string | null;
  /**
   * **Er hängt am Konto, steht aber nicht mehr in der wählbaren Menge.**
   *
   * Im Betrieb kommt das nicht vor: `GET /api/mandanten` liefert einem ADMIN
   * *alle* Mandanten aus `GlassfishDB.Mandant`, und die Zuordnungen eines Kontos
   * sind eine Teilmenge davon. Es bleibt trotzdem stehen, und der Grund ist
   * nicht Vorsicht, sondern die Bauart des Endpunkts: **Die Menge wird
   * vollständig ersetzt.** Eine Auswahl, die einen aktuellen Wert gar nicht
   * darstellen kann, löschte ihn beim nächsten Speichern — lautlos und ohne dass
   * jemand ihn je gesehen hätte.
   */
  nichtWaehlbar: boolean;
};

/**
 * Die Auswahl: **alle wählbaren Mandanten, plus jeder, der schon am Konto
 * hängt.**
 *
 * ## `SYSTEM` und `WOC` werden nicht ausgesiebt
 *
 * Beide sind technische Mandanten und kein Kunde
 * (`docs/PROJEKTBESCHREIBUNG.md` §3), und die Mandantenauswahl aus Schritt 3
 * zeigt sie trotzdem: *„Das Frontend filtert nichts nach und kennt keine Regel
 * darüber, wer was sehen darf."* Hier gilt dasselbe, und hier wiegt es schwerer
 * — wer für einen technischen Mandanten zuständig ist, muss ihm zugeordnet
 * werden können. **Dieselbe Quelle, dieselbe Behandlung**, statt einer zweiten
 * Regel daneben.
 *
 * ## Sortiert nach Kennung
 *
 * Nach der Kennung und nicht nach dem Anzeigenamen: Die Kennung ist das, was in
 * der Zeile steht und was der Endpunkt entgegennimmt, und sie ist eindeutig.
 */
export function wahlmoeglichkeiten(
  zugeordnet: readonly string[],
  waehlbar: readonly Mandant[],
): Wahlmoeglichkeit[] {
  const nachId = new Map(waehlbar.map((mandant) => [mandant.id, mandant]));
  const alle = new Set([...nachId.keys(), ...zugeordnet]);

  return [...alle].sort().map((id) => ({
    id,
    name: nachId.get(id)?.name ?? null,
    nichtWaehlbar: !nachId.has(id),
  }));
}

/**
 * Einen Mandanten in der Zielmenge an- oder abwählen.
 *
 * **Es entsteht immer eine neue Menge**, nie eine geänderte — der Entwurf ist
 * Komponentenzustand, und React vergleicht Referenzen.
 */
export function umschalten(menge: readonly string[], id: string): string[] {
  return menge.includes(id) ? menge.filter((eintrag) => eintrag !== id) : [...menge, id];
}

/**
 * **Die letzte Zuordnung lässt sich nicht entfernen — für beide Rollen** (E10).
 *
 * Schritt 3 speichert sie auch für einen ADMIN, *„damit sie bei einer späteren
 * Herabstufung nicht ins Leere fällt"*; dürfte man sie dort entfernen, entstünde
 * beim nächsten Rollenwechsel genau der Zustand, den E11 verbietet.
 *
 * **Der Fall wird erkennbar gemacht und nicht erst über `409
 * letzte-mandantenzuordnung` gelernt.** Er steht vollständig im Entwurf, den der
 * Nutzer vor sich hat — ihn dorthin laufen zu lassen wäre eine Fehlermeldung für
 * etwas, das die Auswahl selbst sagen kann. Der Serverfehler bleibt trotzdem
 * übersetzt: Er ist die verbindliche Prüfung, und das hier ist der Handlauf
 * davor.
 *
 * **Ein Tausch bleibt möglich**, und das ist der Grund für die Formulierung
 * „genau diese eine": Wer von `{VOTG}` auf `{NEXANS}` will, hakt erst `NEXANS`
 * an — dann sind es zwei, und `VOTG` lässt sich wieder abwählen.
 */
export function istLetzteZuordnung(menge: readonly string[], id: string): boolean {
  return menge.length === 1 && menge[0] === id;
}

/**
 * Ob sich der Entwurf von der gespeicherten Menge unterscheidet.
 *
 * **Verglichen wird als Menge und nicht als Liste:** Die Reihenfolge ist keine
 * Aussage — das Backend liefert aufsteigend sortiert und speichert eine Menge.
 * Ein Vergleich über die Reihenfolge meldete eine Änderung, wo keine ist, und
 * schickte ein `PUT` für nichts. Jedes `PUT` verwirft alle Sitzungen des Kontos
 * (E5); eines für nichts ist deshalb kein leerer Aufruf, sondern eine Abmeldung
 * ohne Anlass.
 */
export function mengeGeaendert(
  entwurf: readonly string[],
  gespeichert: readonly string[],
): boolean {
  if (entwurf.length !== gespeichert.length) {
    return true;
  }
  const vorhanden = new Set(gespeichert);
  return entwurf.some((id) => !vorhanden.has(id));
}
