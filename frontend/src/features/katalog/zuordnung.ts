import type { Katalogzeile, Richtung, ZuordnenAnfrage } from "./api";
import { istRichtung } from "./api";

/**
 * Die Entscheidungen der Zeilenbearbeitung (E19) — **frei von React**, damit sie
 * sich prüfen lassen, ohne einen Baum zu rendern.
 *
 * Hier steht, was beim Bearbeiten einer Zeile *entschieden* wird: wann sich eine
 * zweite öffnen lässt, was aus einem Formularentwurf für eine Anfrage wird, und
 * wie die Antwort in den Zwischenspeicher zurückkommt. Wie es aussieht, steht in
 * `components/zeilen-formular.tsx`.
 */

/**
 * Der Wert, den die Richtungsauswahl für „keine Richtung" trägt.
 *
 * **Ein eigener Wert und nicht die leere Zeichenkette.** Eine `ToggleGroup` mit
 * `type="single"` meldet die Abwahl eines Eintrags als `""` — beides wäre dann
 * dasselbe Zeichen für zwei verschiedene Dinge: „der Nutzer hat *keine* Richtung
 * gewählt" und „der Nutzer hat gerade den aktiven Knopf abgewählt". Mit einem
 * dritten Knopf ist „keine Richtung" eine sichtbare, klickbare und mit der
 * Tastatur erreichbare Wahl statt eines Zustands, in den man aus Versehen fällt.
 *
 * Dieselbe Überlegung wie beim leeren Partner: Was „nichts" bedeutet, muss man
 * **wählen** können.
 */
export const OHNE_RICHTUNG = "OHNE";

export type Richtungswahl = Richtung | typeof OHNE_RICHTUNG;

/** Der Bearbeitungsstand einer Zeile, wie er im Formular steht. */
export type Zeilenentwurf = {
  partner: string;
  richtung: Richtungswahl;
};

/** Der Entwurf, mit dem eine Zeile geöffnet wird — ihr aktueller Stand. */
export function entwurfAus(zeile: Katalogzeile): Zeilenentwurf {
  return {
    partner: zeile.partner ?? "",
    richtung: zeile.richtung ?? OHNE_RICHTUNG,
  };
}

/**
 * Der Entwurf als Anfrage.
 *
 * **Leerraum wird zu `null`.** Ein Feld, das nur Leerzeichen enthält, wäre ein
 * dritter Zustand durch die Hintertür — das Backend macht dieselbe Umsetzung
 * (`docs/prozess-katalog-backend.md` §4), und beide Seiten dürfen sich hier
 * nicht widersprechen. Das Frontend hält die Anfrage deshalb nicht zurück und
 * prüft nichts nach; es schickt, was gemeint ist.
 */
export function alsAnfrage(entwurf: Zeilenentwurf): ZuordnenAnfrage {
  const partner = entwurf.partner.trim();
  return {
    partner: partner === "" ? null : partner,
    richtung: istRichtung(entwurf.richtung) ? entwurf.richtung : null,
  };
}

/**
 * Bedeutet das Speichern dieses Entwurfs „hingesehen, es gibt keinen Partner"?
 *
 * Der Satz, der das dem Nutzer sagt, erscheint genau dann — und nur dann. Er ist
 * die einzige Stelle der Oberfläche, an der die Bedeutung des leeren Feldes
 * ausgesprochen wird, und **er erscheint erst, wenn das Feld wirklich leer ist**:
 * eine dauerhafte Erklärung neben einem gefüllten Feld wäre Rauschen.
 */
export function speichertOhnePartner(entwurf: Zeilenentwurf): boolean {
  return entwurf.partner.trim() === "";
}

/**
 * Darf diese Zeile geöffnet werden?
 *
 * **Solange eine Zeile offen ist, lässt sich keine zweite öffnen** (E19). Der
 * Grund ist nicht Ordnungsliebe: Der Entwurf lebt im Komponentenzustand, und
 * eine zweite Zeile zu öffnen hieße, den ersten stillschweigend zu verwerfen —
 * ausgerechnet den Teil der Arbeit, den noch niemand geschrieben hat.
 *
 * **Der offene Zeilenschlüssel steht bewusst nicht in der URL**, obwohl sie ihn
 * ausdrücken könnte. Der Zweischritt aus `docs/frontend-grundlagen.md` §8:
 * Er beschreibt keinen Ausschnitt, sondern eine **begonnene Eingabe** — und die
 * Hälfte, auf die es ankommt (der getippte Text), lässt sich ohnehin nicht
 * teilen. Eine geteilte URL zeigte dem Empfänger ein leeres Formular über einer
 * Zeile und behauptete damit etwas, das der Absender nie gesehen hat.
 */
export function darfOeffnen(offeneZeile: string | null, processId: string): boolean {
  return offeneZeile === null || offeneZeile === processId;
}

/**
 * Die Liste mit der geänderten Zeile an ihrer Stelle.
 *
 * **Die Reihenfolge bleibt, und die Zeile bleibt stehen.** Sie ist jetzt
 * `GEPFLEGT` und gehörte streng genommen nicht mehr in eine Liste mit
 * `nurOffene` — sie unter der Hand verschwinden zu lassen wäre trotzdem falsch:
 * Der Nutzer hat gerade dort gearbeitet, und das Wort „gepflegt" in der Zeile
 * *ist* die Bestätigung, dass es geschrieben wurde. Beim nächsten Holen ist sie
 * fort, und dann als Folge einer Abfrage und nicht als Zaubertrick.
 *
 * Findet sich die Kennung nicht, kommt die Liste **unverändert** zurück — ohne
 * sie anzuhängen. Eine Zeile, die der Server nicht in dieser Liste hat, gehört
 * auch nicht hinein.
 */
export function mitAktualisierterZeile(
  zeilen: readonly Katalogzeile[],
  neu: Katalogzeile,
): Katalogzeile[] {
  return zeilen.map((zeile) => (zeile.processId === neu.processId ? neu : zeile));
}

/**
 * Die Vorschlagsliste mit einem neu getippten Partner darin, alphabetisch.
 *
 * Wer zwanzig Zeilen demselben neuen Partner zuordnet, soll ihn ab der zweiten
 * vorgeschlagen bekommen. Der Endpunkt leitet die Liste aus den Katalogzeilen
 * ab (E2) und wüsste es erst beim nächsten Holen.
 *
 * **Der umgekehrte Fall wird nicht nachgeführt:** Wer den letzten Partner eines
 * Namens entfernt, sieht ihn bis zum nächsten Holen weiter in der Auswahl. Das
 * ist in Kauf genommen — die Vorschläge sind ein Geländer gegen Schreibvarianten
 * und keine Schranke (E21); ein Name zu viel kostet nichts, ein fehlender Name
 * kostet einen Tippfehler.
 *
 * Sortiert wird mit `localeCompare` in derselben Ordnung, die das Backend über
 * `utf8mb4_general_ci` liefert — annähernd; die Auswahl ist eine Hilfe und keine
 * Zusicherung über Sortierung.
 */
export function mitPartner(vorschlaege: readonly string[], name: string | null): string[] {
  const wert = name?.trim() ?? "";
  if (wert === "" || vorschlaege.includes(wert)) {
    return [...vorschlaege];
  }
  return [...vorschlaege, wert].sort((a, b) => a.localeCompare(b));
}

/**
 * Die Vorschläge, die zu dem passen, was im Feld steht.
 *
 * **Ein leeres Feld zeigt alle** — dann sucht der Nutzer noch, statt zu tippen.
 * Verglichen wird ohne Rücksicht auf Groß- und Kleinschreibung und an beliebiger
 * Stelle: Partnernamen sind Kürzel, und wer `BAY` tippt, meint auch `NXS_BAYER`.
 *
 * Der Wert selbst wird nicht herausgefiltert. Steht er schon genau so in der
 * Liste, ist das die Auskunft, dass es ihn gibt — und genau dafür sind die
 * Vorschläge da.
 */
export function passendeVorschlaege(vorschlaege: readonly string[], eingabe: string): string[] {
  const begriff = eingabe.trim().toLowerCase();
  if (begriff === "") {
    return [...vorschlaege];
  }
  return vorschlaege.filter((name) => name.toLowerCase().includes(begriff));
}

/**
 * Der Wert, den eine Massenzuordnung setzt — **ein** Feld, ein Wert (E11).
 *
 * `null` bedeutet „leeren" und ist ausdrücklich zulässig. Für den Partner gilt
 * dieselbe Umsetzung wie in {@link alsAnfrage}: Leerraum ist leer, sonst
 * entstünde über die Massenzuordnung ein Zustand, den die Einzelbearbeitung
 * nicht erzeugen kann.
 */
export function massenwert(
  feld: "PARTNER" | "RICHTUNG",
  partner: string,
  richtung: Richtungswahl,
): string | null {
  if (feld === "PARTNER") {
    const wert = partner.trim();
    return wert === "" ? null : wert;
  }
  return istRichtung(richtung) ? richtung : null;
}
