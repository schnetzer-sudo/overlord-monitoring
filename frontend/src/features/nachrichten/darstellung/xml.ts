import { vorspannEnde } from "./vorspann";

/**
 * **XML** — Umbruch und Einrückung zwischen Token, Zeichen für Zeichen sonst
 * (`docs/dateiansicht-darstellung.md` §3).
 *
 * ## Tokenbasiert, nicht geparst und neu geschrieben (E‑198)
 *
 * `DOMParser` mit `XMLSerializer` verlöre die XML-Deklaration und löste
 * Zeichenreferenzen auf — aus `&#xE4;` würde `ä`. Hier wird der Text nur in
 * Token zerlegt: Deklaration und Verarbeitungsanweisungen, Kommentare, CDATA,
 * DOCTYPE samt internem Teil, Start-, End- und leere Tags mit Attributwerten,
 * die `>` enthalten dürfen, und Text. Jedes Token geht Zeichen für Zeichen
 * durch.
 *
 * ## Erkennung
 *
 * Der Text beginnt mit `<`, lässt sich vollständig in Token zerlegen, jedes
 * Endtag passt zum offenen Starttag, am Ende ist kein Tag mehr offen, und
 * außerhalb des Wurzelelements steht nur Leerraum (E‑199).
 *
 * ## Wo Leerraum eingefügt wird — und wo nicht
 *
 * Umbruch und Einrückung um zwei Leerzeichen je Tiefe stehen **nur zwischen
 * zwei Token, zwischen denen nichts oder nur Leerraum steht**; dieser
 * Leerraum wird ersetzt. Neben Text, der mehr als Leerraum enthält, wird
 * nichts eingefügt: `<a>Text</a>` und gemischter Inhalt bleiben auf ihrer
 * Zeile (E‑197).
 */

type Token =
  | { art: "text"; text: string }
  | { art: "start"; text: string; name: string }
  | { art: "ende"; text: string; name: string }
  | { art: "leer"; text: string }
  | { art: "sonst"; text: string };

function istLeerraum(text: string): boolean {
  return /^[ \t\r\n]*$/.test(text);
}

function istNamensanfang(zeichen: string): boolean {
  return /[A-Za-z_:\u0080-\uFFFF]/.test(zeichen);
}

function istNamenszeichen(zeichen: string): boolean {
  return /[A-Za-z0-9_:.\-\u0080-\uFFFF]/.test(zeichen);
}

/** Liest einen Namen ab `ab`; gibt das Ende zurück, oder `ab`, wenn dort keiner steht. */
function name(text: string, ab: number): number {
  if (ab >= text.length || !istNamensanfang(text.charAt(ab))) {
    return ab;
  }
  let i = ab + 1;
  while (i < text.length && istNamenszeichen(text.charAt(i))) {
    i += 1;
  }
  return i;
}

/** Das Ende eines DOCTYPE — der interne Teil in `[…]` darf `>` enthalten, Anführungszeichen auch. */
function doctypeEnde(text: string, ab: number): number {
  let inTeil = false;
  let anfuehrung: string | null = null;
  for (let i = ab; i < text.length; i += 1) {
    const zeichen = text.charAt(i);
    if (anfuehrung !== null) {
      if (zeichen === anfuehrung) {
        anfuehrung = null;
      }
    } else if (zeichen === '"' || zeichen === "'") {
      anfuehrung = zeichen;
    } else if (zeichen === "[") {
      inTeil = true;
    } else if (zeichen === "]") {
      inTeil = false;
    } else if (zeichen === ">" && !inTeil) {
      return i + 1;
    }
  }
  return -1;
}

/** Das Ende eines Start- oder leeren Tags; Attributwerte dürfen `>` enthalten. */
function tagEnde(text: string, ab: number): { ende: number; leer: boolean } | null {
  let anfuehrung: string | null = null;
  for (let i = ab; i < text.length; i += 1) {
    const zeichen = text.charAt(i);
    if (anfuehrung !== null) {
      if (zeichen === anfuehrung) {
        anfuehrung = null;
      }
    } else if (zeichen === '"' || zeichen === "'") {
      anfuehrung = zeichen;
    } else if (zeichen === ">") {
      return { ende: i + 1, leer: text.charAt(i - 1) === "/" };
    }
  }
  return null;
}

function abgeschlossen(text: string, ab: number, oeffner: string, schliesser: string): number {
  const ende = text.indexOf(schliesser, ab + oeffner.length);
  return ende < 0 ? -1 : ende + schliesser.length;
}

/** Zerlegt den Text ab `anfang` in Token — oder `null`, wenn das nicht restlos gelingt. */
function zerlege(text: string, anfang: number): Token[] | null {
  const token: Token[] = [];
  let i = anfang;
  while (i < text.length) {
    if (text.charAt(i) !== "<") {
      let ende = text.indexOf("<", i);
      if (ende < 0) {
        ende = text.length;
      }
      token.push({ art: "text", text: text.slice(i, ende) });
      i = ende;
      continue;
    }

    let ende: number;
    if (text.startsWith("<?", i)) {
      ende = abgeschlossen(text, i, "<?", "?>");
    } else if (text.startsWith("<!--", i)) {
      ende = abgeschlossen(text, i, "<!--", "-->");
    } else if (text.startsWith("<![CDATA[", i)) {
      ende = abgeschlossen(text, i, "<![CDATA[", "]]>");
    } else if (text.startsWith("<!DOCTYPE", i)) {
      ende = doctypeEnde(text, i + "<!DOCTYPE".length);
    } else if (text.startsWith("</", i)) {
      const nameEnde = name(text, i + 2);
      if (nameEnde === i + 2) {
        return null;
      }
      let j = nameEnde;
      while (j < text.length && istLeerraum(text.charAt(j))) {
        j += 1;
      }
      if (text.charAt(j) !== ">") {
        return null;
      }
      token.push({ art: "ende", text: text.slice(i, j + 1), name: text.slice(i + 2, nameEnde) });
      i = j + 1;
      continue;
    } else {
      const nameEnde = name(text, i + 1);
      if (nameEnde === i + 1) {
        return null;
      }
      const tag = tagEnde(text, nameEnde);
      if (tag === null) {
        return null;
      }
      const tagText = text.slice(i, tag.ende);
      token.push(
        tag.leer
          ? { art: "leer", text: tagText }
          : { art: "start", text: tagText, name: text.slice(i + 1, nameEnde) },
      );
      i = tag.ende;
      continue;
    }

    if (ende < 0) {
      return null;
    }
    token.push({ art: "sonst", text: text.slice(i, ende) });
    i = ende;
  }
  return token;
}

/** Ob die Token ein wohlgeformtes Dokument bilden: passende Endtags, nichts offen, ein Wurzelelement, außerhalb nur Leerraum. */
function wohlgeformt(token: Token[]): boolean {
  const offen: string[] = [];
  let elemente = 0;
  for (const t of token) {
    if (t.art === "start") {
      offen.push(t.name);
      elemente += 1;
    } else if (t.art === "leer") {
      elemente += 1;
    } else if (t.art === "ende") {
      if (offen.pop() !== t.name) {
        return false;
      }
    } else if (t.art === "text" && offen.length === 0 && !istLeerraum(t.text)) {
      return false;
    }
  }
  return offen.length === 0 && elemente > 0;
}

function zerlegeDokument(text: string): { vorspann: string; token: Token[] } | null {
  const anfang = vorspannEnde(text);
  if (text.charAt(anfang) !== "<") {
    return null;
  }
  const token = zerlege(text, anfang);
  if (token === null || !wohlgeformt(token)) {
    return null;
  }
  return { vorspann: text.slice(0, anfang), token };
}

export function erkenneXml(text: string): boolean {
  return zerlegeDokument(text) !== null;
}

/** Ein Token je Zeile, wo Leerraum dazwischen steht; gemischter Inhalt unverändert — oder `null`, wenn der Text kein XML ist. */
export function formatiereXml(text: string): string | null {
  const dokument = zerlegeDokument(text);
  if (dokument === null) {
    return null;
  }
  const { token } = dokument;
  const teile: string[] = [dokument.vorspann];
  let tiefe = 0;
  const letztes = token.length - 1;

  /** Die Einrückung, mit der das Token `t` beginnt — ein Endtag eine Stufe tiefer. */
  const einrueckung = (t: Token): string =>
    "\n" + "  ".repeat(t.art === "ende" ? tiefe - 1 : tiefe);

  token.forEach((t, stelle) => {
    if (t.art === "text") {
      const naechstes = token[stelle + 1];
      if (istLeerraum(t.text) && stelle > 0 && stelle < letztes && naechstes !== undefined) {
        // Nur Leerraum zwischen zwei Token: ersetzt durch Umbruch und Einrückung.
        teile.push(einrueckung(naechstes));
      } else {
        teile.push(t.text);
      }
      return;
    }

    const voriges = token[stelle - 1];
    // Ein leeres Paar `<a></a>` bleibt auf seiner Zeile: Die Regel erlaubt den
    // Umbruch zwischen zwei Token ohne etwas dazwischen, sie verlangt ihn nicht
    // — und ein Element ohne Inhalt liest sich als eines.
    const leeresPaar = voriges?.art === "start" && t.art === "ende" && voriges.name === t.name;
    if (voriges !== undefined && voriges.art !== "text" && !leeresPaar) {
      // Zwei Token ohne etwas dazwischen: Umbruch und Einrückung eingefügt.
      teile.push(einrueckung(t));
    }
    if (t.art === "ende") {
      tiefe -= 1;
    }
    teile.push(t.text);
    if (t.art === "start") {
      tiefe += 1;
    }
  });

  return teile.join("");
}
