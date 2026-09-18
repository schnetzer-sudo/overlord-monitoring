import { vorspannEnde } from "./vorspann";

/**
 * **JSON** — Umbruch und Einrückung außerhalb von Zeichenketten, Zeichen für
 * Zeichen sonst (`docs/dateiansicht-darstellung.md` §3).
 *
 * ## Tokenbasiert, nicht `JSON.stringify(JSON.parse(…))` (E‑198)
 *
 * Der Umweg über einen Wert machte aus `12.50` die `12.5`, verlöre die Stellen
 * einer 20-stelligen Nummer und verwürfe doppelte Schlüssel. Hier wird der
 * Text entlang der JSON-Grammatik gelesen und dabei ausgegeben: Zeichenketten
 * samt Escapes, Zahlen sowie `true`, `false` und `null` gehen unverändert
 * durch. `JSON.parse` kommt in den Tests als Gegenprobe vor, hier nicht.
 *
 * ## Erkennung
 *
 * Der Text beginnt mit `{` oder `[` und genügt als Ganzes der JSON-Grammatik,
 * danach nur noch Leerraum (E‑199). Ein Text, der die Grammatik verletzt —
 * auch eine gekappte Datei —, ist kein JSON und bleibt Original mit Vermerk.
 *
 * ## Form
 *
 * Leerraum außerhalb von Zeichenketten wird durch Umbruch und Einrückung um
 * zwei Leerzeichen ersetzt, hinter dem Doppelpunkt steht ein Leerzeichen, `{}`
 * und `[]` bleiben kompakt (E‑197).
 *
 * Der Durchlauf ist **nicht rekursiv**: Die Tiefe steht auf einem eigenen
 * Stapel, damit ein tief geschachteltes Dokument den Aufrufstapel der Laufzeit
 * nicht sprengt.
 */

const ZAHL = /-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/y;
const ESCAPES = '"\\/bfnrt';
const HEX4 = /^[0-9a-fA-F]{4}$/;

function istLeerraum(zeichen: string): boolean {
  return zeichen === " " || zeichen === "\t" || zeichen === "\n" || zeichen === "\r";
}

/** Das Ende einer Zeichenkette ab dem öffnenden `"` — oder `-1`, wenn sie der Grammatik nicht genügt. */
function zeichenkettenEnde(text: string, ab: number): number {
  let i = ab + 1;
  while (i < text.length) {
    const zeichen = text.charAt(i);
    if (zeichen === '"') {
      return i + 1;
    }
    if (zeichen === "\\") {
      const escape = text.charAt(i + 1);
      if (escape !== "" && ESCAPES.includes(escape)) {
        i += 2;
      } else if (escape === "u" && HEX4.test(text.slice(i + 2, i + 6))) {
        i += 6;
      } else {
        return -1;
      }
    } else if (zeichen.charCodeAt(0) < 0x20) {
      return -1;
    } else {
      i += 1;
    }
  }
  return -1;
}

/** Der Text mit Umbruch und Einrückung — oder `null`, wenn er kein JSON ist. */
export function formatiereJson(text: string): string | null {
  const anfang = vorspannEnde(text);
  const erstes = text.charAt(anfang);
  if (erstes !== "{" && erstes !== "[") {
    return null;
  }

  const teile: string[] = [text.slice(0, anfang)];
  const laenge = text.length;
  /** Die offenen Behälter, je das schließende Zeichen. */
  const stapel: string[] = [];
  let i = anfang;

  const einrueckung = (): string => "\n" + "  ".repeat(stapel.length);
  const leerraum = (): void => {
    while (i < laenge && istLeerraum(text.charAt(i))) {
      i += 1;
    }
  };
  const zeichenkette = (): boolean => {
    const ende = zeichenkettenEnde(text, i);
    if (ende < 0) {
      return false;
    }
    teile.push(text.slice(i, ende));
    i = ende;
    return true;
  };

  let schluesselErwartet = false;

  for (;;) {
    leerraum();

    if (schluesselErwartet) {
      if (text.charAt(i) !== '"' || !zeichenkette()) {
        return null;
      }
      leerraum();
      if (text.charAt(i) !== ":") {
        return null;
      }
      i += 1;
      teile.push(": ");
      leerraum();
    }

    // Ein Wert.
    const zeichen = text.charAt(i);
    if (zeichen === "{" || zeichen === "[") {
      const schluss = zeichen === "{" ? "}" : "]";
      i += 1;
      leerraum();
      if (text.charAt(i) === schluss) {
        i += 1;
        teile.push(zeichen + schluss);
      } else {
        stapel.push(schluss);
        teile.push(zeichen, einrueckung());
        schluesselErwartet = zeichen === "{";
        continue;
      }
    } else if (zeichen === '"') {
      if (!zeichenkette()) {
        return null;
      }
    } else if (zeichen === "-" || (zeichen >= "0" && zeichen <= "9")) {
      ZAHL.lastIndex = i;
      const treffer = ZAHL.exec(text);
      if (treffer === null) {
        return null;
      }
      teile.push(treffer[0]);
      i += treffer[0].length;
    } else if (text.startsWith("true", i)) {
      teile.push("true");
      i += 4;
    } else if (text.startsWith("false", i)) {
      teile.push("false");
      i += 5;
    } else if (text.startsWith("null", i)) {
      teile.push("null");
      i += 4;
    } else {
      return null;
    }

    // Nach einem Wert: Komma, schließendes Zeichen — oder das Ende.
    for (;;) {
      const vorLeerraum = i;
      leerraum();
      const schluss = stapel[stapel.length - 1];
      if (schluss === undefined) {
        // Hinter dem Wert nur noch Leerraum — und der geht unverändert mit,
        // wie der Vorspann davor.
        if (i !== laenge) {
          return null;
        }
        teile.push(text.slice(vorLeerraum, i));
        return teile.join("");
      }
      const naechstes = text.charAt(i);
      if (naechstes === ",") {
        i += 1;
        teile.push(",", einrueckung());
        schluesselErwartet = schluss === "}";
        break;
      }
      if (naechstes === schluss) {
        i += 1;
        stapel.pop();
        teile.push(einrueckung(), schluss);
        continue;
      }
      return null;
    }
  }
}

export function erkenneJson(text: string): boolean {
  return formatiereJson(text) !== null;
}
