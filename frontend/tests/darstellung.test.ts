import { describe, expect, it } from "vitest";

import {
  DARSTELLUNGEN,
  DARSTELLUNG_VORGABE,
  FORMATE,
  HEX_GRENZE_BYTES,
  darstellungWaehlbar,
  darstellungsvermerke,
  erkenneFormat,
  stelleDar,
  type Format,
} from "@/features/nachrichten/darstellung";
import { erkannteFormate } from "@/features/nachrichten/darstellung/erkennung";
import {
  DARSTELLUNG_PARAMETER,
  parseAsDarstellung,
} from "@/features/nachrichten/darstellung/parameter";

/**
 * **Die Darstellungen der Dateiansicht — als reine Funktionen, ohne Baum**
 * (`docs/dateiansicht-darstellung.md` §7, Teil A).
 *
 * Je Darstellung drei Klassen von Fällen:
 *
 * 1. **Die Eigenschaft aus E‑197:** Eine Darstellung fügt nur Leerraum ein
 *    oder nimmt Zeilenumbrüche weg — jedes andere Zeichen bleibt, wie und wo es
 *    ist. EDIFACT, X12 und VDA ohne alle Zeilenumbrüche gleich dem Original
 *    ohne alle Zeilenumbrüche; IDoc ebenso nach Entfernen der Einrückung; XML
 *    nach Entfernen des Leerraums zwischen Token; JSON nach Entfernen des
 *    Leerraums außerhalb von Zeichenketten; Hex zurückgerechnet gleich dem
 *    Original, über alle Zeichencodes `0x00` bis `0xFF`.
 * 2. **Die Fallen** je Format — das Freigabezeichen, die abweichende `UNA`,
 *    die umbrochene Datei, der Zeilenumbruch als Segmentende, die Länge, die
 *    kein Vielfaches ist, `12.50` und die 20-stellige Zahl, `&#228;` und das
 *    `>` im Attributwert, die Kappungsgrenze und der Zeichencode über `0xFF`.
 * 3. **Die Kreuzprobe:** jedes Beispiel gegen jede Erkennung mit höchstens
 *    einem Treffer, und jede nicht passende Darstellung außer Hex meldet
 *    „passt nicht".
 *
 * `JSON.parse` kommt hier als **Gegenprobe** vor, nie in der Ausgabe (E‑198).
 * Keine Zeitmessung (Regel T1). **Alle Inhalte sind erfunden** — keine echten
 * Partner, Kennungen, Belegnummern oder Pfade.
 */

/* ─────────────────────────────────────────────────────────────────────────────
   Die Beispiele
   ───────────────────────────────────────────────────────────────────────────── */

const APOSTROPH = "'";

/** Einzeilig, Standardtrennzeichen, mit `?'` und `??` in den Daten. */
const EDIFACT_EINZEILIG = [
  "UNB+UNOC:3+ERFUNDEN1+ERFUNDEN2+250101:1200+1'",
  "UNH+1+ORDERS:D:96A:UN'",
  "BGM+220+ERF?'123+9'",
  "FTX+AAI+++Frage??'",
  "UNT+4+1'",
  "UNZ+1+1'",
].join("");

const EDIFACT_EINZEILIG_FORMATIERT = [
  "UNB+UNOC:3+ERFUNDEN1+ERFUNDEN2+250101:1200+1'",
  "UNH+1+ORDERS:D:96A:UN'",
  "BGM+220+ERF?'123+9'",
  "FTX+AAI+++Frage??'",
  "UNT+4+1'",
  "UNZ+1+1'",
].join("\n");

/** Abweichende `UNA`: Komponente `;`, Datenelement `*`, Dezimal `,`, Freigabe `\`, Segmentende `~`. */
const RUECKSTRICH = String.fromCharCode(92);
const EDIFACT_UNA =
  "UNA;*,%s ~".replace("%s", RUECKSTRICH) +
  "UNB*UNOC;3*ERFUNDEN1*ERFUNDEN2*250101;1200*1~" +
  "UNH*1*ORDERS;D;96A;UN~" +
  `BGM*220*X${RUECKSTRICH}~Y*9~` +
  "UNT*3*1~" +
  "UNZ*1*1~";

const EDIFACT_UNA_FORMATIERT = [
  "UNA;*,%s ~".replace("%s", RUECKSTRICH),
  "UNB*UNOC;3*ERFUNDEN1*ERFUNDEN2*250101;1200*1~",
  "UNH*1*ORDERS;D;96A;UN~",
  `BGM*220*X${RUECKSTRICH}~Y*9~`,
  "UNT*3*1~",
  "UNZ*1*1~",
].join("\n");

/** Umbrochen: ein Segment läuft über zwei Zeilen, dazu eine Zeile je Segment und ein Schlussumbruch. */
const EDIFACT_UMBROCHEN =
  "UNB+UNOC:3+ERFUNDEN1+ERFUNDEN2+250101:1200+1'\r\n" +
  "UNH+1+ORD\r\nERS:D:96A:UN'\r\n" +
  "UNT+2+1'\r\n" +
  "UNZ+1+1'\r\n";

const EDIFACT_UMBROCHEN_FORMATIERT =
  "UNB+UNOC:3+ERFUNDEN1+ERFUNDEN2+250101:1200+1'\r\n" +
  "UNH+1+ORDERS:D:96A:UN'\r\n" +
  "UNT+2+1'\r\n" +
  "UNZ+1+1'\r\n";

/** Das Segmentende ist der Zeilenumbruch selbst — die Datei bleibt, wie sie ist. */
const EDIFACT_ZEILENENDE_ALS_SEGMENTENDE =
  "UNA:+.? \nUNB+UNOC:3+ERFUNDEN1+ERFUNDEN2+250101:1200+1\nUNH+1+ORDERS:D:96A:UN\nUNT+2+1\nUNZ+1+1\n";

/** Ein ISA-Segment in fester Länge; die Feldbreiten stehen in `x12.ts`. */
function isaSegment(element: string, komponente: string, segmentende: string): string {
  const felder = [
    "00",
    "          ",
    "00",
    "          ",
    "ZZ",
    "ERFUNDEN1      ",
    "ZZ",
    "ERFUNDEN2      ",
    "250101",
    "1200",
    "U",
    "00401",
    "000000001",
    "0",
    "T",
    komponente,
  ];
  return "ISA" + element + felder.join(element) + segmentende;
}

function x12(segmentende: string, dazwischen = ""): string {
  return (
    [
      isaSegment("*", ":", segmentende),
      "GS*PO*ERFUNDEN1*ERFUNDEN2*20250101*1200*1*X*004010" + segmentende,
      "ST*850*0001" + segmentende,
      "BEG*00*NE*ERF123**20250101" + segmentende,
      "SE*2*0001" + segmentende,
      "GE*1*1" + segmentende,
      "IEA*1*000000001" + segmentende,
    ].join(dazwischen) + dazwischen
  );
}

const X12_TILDE = x12("~");
const X12_TILDE_FORMATIERT = x12("~", "\n").slice(0, -1);
const X12_TILDE_MIT_ZEILEN = x12("~", "\n");
const X12_ZEILENENDE_ALS_SEGMENTENDE = x12("\n");

/** Ein VDA-Satz: Satzart in drei Ziffern, auf 128 Zeichen aufgefüllt. */
function vdaSatz(art: string, inhalt: string): string {
  return (art + inhalt).padEnd(128, " ");
}

const VDA_SATZ_1 = vdaSatz("511", "ERFUNDEN1 ERFUNDEN2 250101");
const VDA_SATZ_2 = vdaSatz("512", "ERF123 00001");
const VDA_SATZ_3 = vdaSatz("519", "00003");
const VDA_OHNE_ZEILEN = VDA_SATZ_1 + VDA_SATZ_2 + VDA_SATZ_3;
const VDA_MIT_ZEILEN = [VDA_SATZ_1, VDA_SATZ_2, VDA_SATZ_3].join("\n") + "\n";
const VDA_MIT_ZEILEN_CRLF = [VDA_SATZ_1, VDA_SATZ_2, VDA_SATZ_3].join("\r\n") + "\r\n";

/** Ein IDoc-Kontrollsatz (524) und Datensätze (1063) nach der SAP-Satzstruktur. */
function idocKontrollsatz(): string {
  return ("EDI_DC40" + "100" + "0000000000000001" + "740 " + "30" + "2").padEnd(524, " ");
}

function idocDatensatz(segment: string, nummer: string, ebene: string, daten: string): string {
  const satz =
    segment.padEnd(30, " ") +
    "100" +
    "0000000000000001" +
    nummer.padStart(6, "0") +
    "000000" +
    ebene +
    daten.padEnd(1000, " ");
  if (satz.length !== 1063) {
    throw new Error("Datensatz nicht 1063 Zeichen: " + satz.length);
  }
  return satz;
}

const IDOC_SAETZE = [
  idocKontrollsatz(),
  idocDatensatz("E1ERFK01", "1", "01", "ERFUNDEN KOPF"),
  idocDatensatz("E1ERFP01", "2", "02", "ERFUNDEN POSITION 1"),
  idocDatensatz("Z1ERF_UNTER/1", "3", "03", "ERFUNDEN UNTERPOSITION"),
  idocDatensatz("E1ERFP01", "4", "02", "ERFUNDEN POSITION 2"),
];
const IDOC_OHNE_ZEILEN = IDOC_SAETZE.join("");
const IDOC_MIT_ZEILEN = IDOC_SAETZE.join("\n") + "\n";
const IDOC_FORMATIERT_ZEILEN = [
  IDOC_SAETZE[0],
  IDOC_SAETZE[1],
  "  " + IDOC_SAETZE[2],
  "    " + IDOC_SAETZE[3],
  "  " + IDOC_SAETZE[4],
];

const XML_KOMPAKT =
  '<?xml version="1.0" encoding="ISO-8859-1"?>\n' +
  "<!DOCTYPE bestellung [<!ELEMENT bestellung ANY>]>\n" +
  '<bestellung nr="&#228;-1"><kopf><partner>ERFUNDEN</partner><leer/></kopf>' +
  "<text>Hallo <b>Welt</b> &#228;</text><a></a>" +
  '<wert t="x > y">1</wert>' +
  "<![CDATA[<kein>Tag</kein>]]><!-- Kommentar --></bestellung>\n";

const XML_FORMATIERT =
  '<?xml version="1.0" encoding="ISO-8859-1"?>\n' +
  "<!DOCTYPE bestellung [<!ELEMENT bestellung ANY>]>\n" +
  '<bestellung nr="&#228;-1">\n' +
  "  <kopf>\n" +
  "    <partner>ERFUNDEN</partner>\n" +
  "    <leer/>\n" +
  "  </kopf>\n" +
  "  <text>Hallo <b>Welt</b> &#228;</text>\n" +
  "  <a></a>\n" +
  '  <wert t="x > y">1</wert>\n' +
  "  <![CDATA[<kein>Tag</kein>]]>\n" +
  "  <!-- Kommentar -->\n" +
  "</bestellung>\n";

const ESCAPE_AE = RUECKSTRICH + "u00e4";

const JSON_KOMPAKT =
  '{"betrag":12.50,"menge":1e5,"nr":12345678901234567890,' +
  `"text":"${ESCAPE_AE}","k":1,"k":2,"leer":{},"liste":[],` +
  '"tief":[1,{"a":null,"b":true,"c":false}]}';

const JSON_FORMATIERT = [
  "{",
  '  "betrag": 12.50,',
  '  "menge": 1e5,',
  '  "nr": 12345678901234567890,',
  `  "text": "${ESCAPE_AE}",`,
  '  "k": 1,',
  '  "k": 2,',
  '  "leer": {},',
  '  "liste": [],',
  '  "tief": [',
  "    1,",
  "    {",
  '      "a": null,',
  '      "b": true,',
  '      "c": false',
  "    }",
  "  ]",
  "}",
].join("\n");

/** Je Format ein Beispiel für die Kreuzprobe. */
const BEISPIELE: Record<Format, string> = {
  edifact: EDIFACT_EINZEILIG,
  x12: X12_TILDE,
  vda: VDA_OHNE_ZEILEN,
  idoc: IDOC_OHNE_ZEILEN,
  xml: XML_KOMPAKT,
  json: JSON_KOMPAKT,
};

/* ─────────────────────────────────────────────────────────────────────────────
   Die Hilfen für die Eigenschaften
   ───────────────────────────────────────────────────────────────────────────── */

const ohneZeilenumbrueche = (text: string) => text.replace(/[\r\n]/g, "");

/** Kein Satz beginnt mit einem Leerzeichen — die Einrückung ist deshalb Zeichen für Zeichen entfernbar. */
const ohneEinrueckung = (text: string) =>
  text
    .split("\n")
    .map((zeile) => zeile.replace(/^ +/, ""))
    .join("\n");

/** Leerraum zwischen zwei Token — zwischen `>` und `<` — weggenommen. */
const xmlOhneLeerraumZwischenToken = (text: string) => text.replace(/>[ \t\r\n]+</g, "><");

/** Leerraum außerhalb von Zeichenketten weggenommen; Escapes bleiben, wie sie sind. */
function jsonOhneLeerraum(text: string): string {
  let ergebnis = "";
  let inZeichenkette = false;
  for (let i = 0; i < text.length; i += 1) {
    const zeichen = text.charAt(i);
    if (inZeichenkette) {
      ergebnis += zeichen;
      if (zeichen === RUECKSTRICH) {
        ergebnis += text.charAt(i + 1);
        i += 1;
      } else if (zeichen === '"') {
        inZeichenkette = false;
      }
    } else if (zeichen === '"') {
      inZeichenkette = true;
      ergebnis += zeichen;
    } else if (!/[ \t\r\n]/.test(zeichen)) {
      ergebnis += zeichen;
    }
  }
  return ergebnis;
}

/** Aus einer Hex-Darstellung die Zeichencodes zurückgerechnet. */
function ausHex(text: string): number[] {
  const codes: number[] = [];
  for (const zeile of text.split("\n")) {
    const hexBereich = zeile.slice(10, 59);
    for (const paar of hexBereich.split(" ")) {
      if (paar !== "") {
        codes.push(Number.parseInt(paar, 16));
      }
    }
  }
  return codes;
}

function darstellung(text: string, format: Format) {
  return stelleDar(text, format, "ASCII");
}

/* ─────────────────────────────────────────────────────────────────────────────
   EDIFACT
   ───────────────────────────────────────────────────────────────────────────── */

describe("EDIFACT", () => {
  it("bricht nach jedem Segmentende um und lässt ?' und ?? in den Daten stehen", () => {
    const ergebnis = darstellung(EDIFACT_EINZEILIG, "edifact");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(EDIFACT_EINZEILIG_FORMATIERT);
    expect(ergebnis.text).toContain("ERF?" + APOSTROPH + "123");
    expect(ergebnis.text).toContain("Frage??" + APOSTROPH);
  });

  it("liest die Trennzeichen aus einer abweichenden UNA", () => {
    const ergebnis = darstellung(EDIFACT_UNA, "edifact");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(EDIFACT_UNA_FORMATIERT);
    // Der Apostroph ist hier kein Segmentende — kein Umbruch dahinter.
    expect(ergebnis.text.split("\n")).toHaveLength(6);
  });

  it("übernimmt einen Zeilenumbruch nach UNA, ohne ihn zu verdoppeln", () => {
    const text = "UNA:+.? '\r\n" + EDIFACT_EINZEILIG;
    const ergebnis = darstellung(text, "edifact");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe("UNA:+.? '\r\n" + EDIFACT_EINZEILIG_FORMATIERT);
  });

  it("fügt die umbrochene Datei je Segment zu einer Zeile zusammen und verdoppelt keinen Umbruch", () => {
    const ergebnis = darstellung(EDIFACT_UMBROCHEN, "edifact");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(EDIFACT_UMBROCHEN_FORMATIERT);
  });

  it("lässt die Datei, wie sie ist, wenn das Segmentende ein Zeilenumbruch ist", () => {
    const ergebnis = darstellung(EDIFACT_ZEILENENDE_ALS_SEGMENTENDE, "edifact");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(EDIFACT_ZEILENENDE_ALS_SEGMENTENDE);
  });

  it.each([
    ["einzeilig", EDIFACT_EINZEILIG],
    ["mit UNA", EDIFACT_UNA],
    ["umbrochen", EDIFACT_UMBROCHEN],
    ["Zeilenumbruch als Segmentende", EDIFACT_ZEILENENDE_ALS_SEGMENTENDE],
  ])("ändert außer Zeilenumbrüchen kein Zeichen (%s)", (_, text) => {
    const ergebnis = darstellung(text, "edifact");
    expect(ohneZeilenumbrueche(ergebnis.text)).toBe(ohneZeilenumbrueche(text));
  });

  it("erkennt nur, was mit UNA oder UNB+ beginnt und ein Segmentende trägt", () => {
    expect(erkenneFormat(EDIFACT_EINZEILIG)).toBe("edifact");
    expect(erkenneFormat(EDIFACT_UNA)).toBe("edifact");
    expect(erkenneFormat(EDIFACT_UMBROCHEN)).toBe("edifact");
    expect(erkenneFormat(EDIFACT_ZEILENENDE_ALS_SEGMENTENDE)).toBe("edifact");
    // Kein Segmentende — keine Übertragung.
    expect(erkenneFormat("UNB+UNOC:3+ERFUNDEN1+ERFUNDEN2")).toBeNull();
    // Ohne Datenelementtrenner hinter UNB.
    expect(erkenneFormat("UNBEKANNT'")).toBeNull();
    // UNA ohne folgendes UNB.
    expect(erkenneFormat("UNA:+.? 'UNH+1'")).toBeNull();
    // Gekappt hinter dem letzten Segmentende: passt trotzdem — das letzte Segment ist nur unvollständig.
    expect(darstellung(EDIFACT_EINZEILIG.slice(0, -4), "edifact").passtNicht).toBe(false);
  });
});

/* ─────────────────────────────────────────────────────────────────────────────
   ANSI X12
   ───────────────────────────────────────────────────────────────────────────── */

describe("ANSI X12", () => {
  it("bricht nach jedem ~ um", () => {
    const ergebnis = darstellung(X12_TILDE, "x12");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(X12_TILDE_FORMATIERT);
    expect(ergebnis.text.split("\n")).toHaveLength(7);
  });

  it("übernimmt den Zeilenumbruch hinter dem ~ und verdoppelt ihn nicht", () => {
    const ergebnis = darstellung(X12_TILDE_MIT_ZEILEN, "x12");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(X12_TILDE_MIT_ZEILEN);
  });

  it("lässt die Datei, wie sie ist, wenn das Segmentende ein Zeilenumbruch ist", () => {
    const ergebnis = darstellung(X12_ZEILENENDE_ALS_SEGMENTENDE, "x12");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(X12_ZEILENENDE_ALS_SEGMENTENDE);
  });

  it.each([
    ["mit ~", X12_TILDE],
    ["mit ~ und Zeilen", X12_TILDE_MIT_ZEILEN],
    ["Zeilenumbruch als Segmentende", X12_ZEILENENDE_ALS_SEGMENTENDE],
  ])("ändert außer Zeilenumbrüchen kein Zeichen (%s)", (_, text) => {
    const ergebnis = darstellung(text, "x12");
    expect(ohneZeilenumbrueche(ergebnis.text)).toBe(ohneZeilenumbrueche(text));
  });

  it("erkennt nur ein ISA-Segment in fester Länge", () => {
    expect(erkenneFormat(X12_TILDE)).toBe("x12");
    expect(erkenneFormat(X12_ZEILENENDE_ALS_SEGMENTENDE)).toBe("x12");
    // Ein Feld zu lang: Die Trennerstellen stimmen nicht mehr.
    expect(erkenneFormat(X12_TILDE.replace("ERFUNDEN1      ", "ERFUNDEN1       "))).toBeNull();
    // Zu kurz für ein ISA-Segment.
    expect(erkenneFormat("ISA*00*")).toBeNull();
    // Trennzeichen fallen zusammen.
    expect(erkenneFormat(isaSegment("*", "*", "~"))).toBeNull();
  });
});

/* ─────────────────────────────────────────────────────────────────────────────
   VDA
   ───────────────────────────────────────────────────────────────────────────── */

describe("VDA", () => {
  it("bricht nach je 128 Zeichen um, wenn die Datei keine Zeilenumbrüche trägt", () => {
    const ergebnis = darstellung(VDA_OHNE_ZEILEN, "vda");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe([VDA_SATZ_1, VDA_SATZ_2, VDA_SATZ_3].join("\n"));
    // Ein abschließender Zeilenumbruch geht mit.
    expect(darstellung(VDA_OHNE_ZEILEN + "\n", "vda").text).toBe(
      [VDA_SATZ_1, VDA_SATZ_2, VDA_SATZ_3].join("\n") + "\n",
    );
  });

  it("gleicht dem Original, wenn die Sätze schon je auf einer Zeile stehen", () => {
    expect(darstellung(VDA_MIT_ZEILEN, "vda").text).toBe(VDA_MIT_ZEILEN);
    expect(darstellung(VDA_MIT_ZEILEN_CRLF, "vda").text).toBe(VDA_MIT_ZEILEN_CRLF);
  });

  it("passt nicht bei einer Länge, die kein Vielfaches von 128 ist", () => {
    const text = VDA_SATZ_1 + VDA_SATZ_2.slice(0, 100);
    const ergebnis = darstellung(text, "vda");
    expect(ergebnis.passtNicht).toBe(true);
    expect(ergebnis.text).toBe(text);
    expect(erkenneFormat(text)).toBeNull();
    // Mit Zeilen: eine Zeile mit 127 Zeichen.
    const zeilen = VDA_SATZ_1 + "\n" + VDA_SATZ_2.slice(0, 127) + "\n";
    expect(darstellung(zeilen, "vda").passtNicht).toBe(true);
    // Ein Satz, der nicht mit drei Ziffern beginnt.
    expect(erkenneFormat(vdaSatz("51A", "x") + VDA_SATZ_2)).toBeNull();
  });

  it.each([
    ["ohne Zeilen", VDA_OHNE_ZEILEN],
    ["mit Zeilen", VDA_MIT_ZEILEN],
    ["mit CRLF", VDA_MIT_ZEILEN_CRLF],
  ])("ändert außer Zeilenumbrüchen kein Zeichen (%s)", (_, text) => {
    expect(ohneZeilenumbrueche(darstellung(text, "vda").text)).toBe(ohneZeilenumbrueche(text));
  });
});

/* ─────────────────────────────────────────────────────────────────────────────
   IDoc
   ───────────────────────────────────────────────────────────────────────────── */

describe("IDoc", () => {
  it("teilt ohne Zeilenumbrüche nach 524 und 1063 und rückt nach Ebene ein", () => {
    const ergebnis = darstellung(IDOC_OHNE_ZEILEN, "idoc");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(IDOC_FORMATIERT_ZEILEN.join("\n"));
  });

  it("nimmt mit Zeilenumbrüchen jede Zeile als Satz und rückt ebenso ein", () => {
    const ergebnis = darstellung(IDOC_MIT_ZEILEN, "idoc");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(IDOC_FORMATIERT_ZEILEN.join("\n") + "\n");
  });

  it("rechnet die Einrückung ab der kleinsten vorkommenden Ebene", () => {
    const text = [
      idocKontrollsatz(),
      idocDatensatz("E1ERFP01", "1", "02", "ERFUNDEN"),
      idocDatensatz("E1ERFU01", "2", "03", "ERFUNDEN"),
    ].join("\n");
    const zeilen = darstellung(text, "idoc").text.split("\n");
    expect(zeilen[1]?.startsWith("E1ERFP01")).toBe(true);
    expect(zeilen[2]?.startsWith("  E1ERFU01")).toBe(true);
  });

  it("passt nicht, wenn ein Satz keinen Segmentnamen oder keine Ebene trägt", () => {
    const ohneEbene = idocKontrollsatz() + idocDatensatz("E1ERFK01", "1", "xx", "ERFUNDEN");
    expect(darstellung(ohneEbene, "idoc").passtNicht).toBe(true);
    const falscherName = idocKontrollsatz() + idocDatensatz("e1erfk01", "1", "01", "ERFUNDEN");
    expect(darstellung(falscherName, "idoc").passtNicht).toBe(true);
    // Ohne Zeilen und mit einem Rest, der kein ganzer Satz ist.
    expect(darstellung(IDOC_OHNE_ZEILEN + "E1ERF", "idoc").passtNicht).toBe(true);
    // Ein nacktes EDI_DC40 ist kuerzer als ein Kontrollsatz; erst der volle Satz zaehlt.
    expect(erkenneFormat("EDI_DC40")).toBeNull();
    expect(erkenneFormat(idocKontrollsatz())).toBe("idoc");
    expect(erkenneFormat("EDI_DC41" + " ".repeat(600))).toBeNull();
  });

  it.each([
    ["ohne Zeilen", IDOC_OHNE_ZEILEN],
    ["mit Zeilen", IDOC_MIT_ZEILEN],
  ])("ändert außer Zeilenumbrüchen und Einrückung kein Zeichen (%s)", (_, text) => {
    const ergebnis = darstellung(text, "idoc");
    expect(ohneZeilenumbrueche(ohneEinrueckung(ergebnis.text))).toBe(ohneZeilenumbrueche(text));
  });
});

/* ─────────────────────────────────────────────────────────────────────────────
   XML
   ───────────────────────────────────────────────────────────────────────────── */

describe("XML", () => {
  it("bricht zwischen Token um und lässt Deklaration, Zeichenreferenzen, CDATA und gemischten Inhalt unverändert", () => {
    const ergebnis = darstellung(XML_KOMPAKT, "xml");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(XML_FORMATIERT);
    // Die Zeichenreferenz ist nicht aufgelöst, das `>` im Attributwert hat
    // kein Tag beendet, und im CDATA ist nichts umgebrochen.
    expect(ergebnis.text).toContain("&#228;");
    expect(ergebnis.text).not.toContain("ä");
    expect(ergebnis.text).toContain('<wert t="x > y">1</wert>');
    expect(ergebnis.text).toContain("<![CDATA[<kein>Tag</kein>]]>");
  });

  it("ersetzt vorhandenen Leerraum zwischen Token statt ihn zu stapeln", () => {
    const text = "<a>\n\n\n   <b>x</b>   \n</a>";
    expect(darstellung(text, "xml").text).toBe("<a>\n  <b>x</b>\n</a>");
  });

  it("fügt neben Text mit Inhalt nichts ein", () => {
    expect(darstellung("<a>Text</a>", "xml").text).toBe("<a>Text</a>");
    expect(darstellung("<p>Hallo <b>fett</b> Welt</p>", "xml").text).toBe(
      "<p>Hallo <b>fett</b> Welt</p>",
    );
  });

  it("passt nicht, wenn ein Endtag nicht zum offenen Starttag passt oder ein Tag offen bleibt", () => {
    for (const text of ["<a><b></a>", "<a><b>x</b>", "<a>x</a><b/>y", "<a x=1>", "< a></a>"]) {
      const ergebnis = darstellung(text, "xml");
      expect(ergebnis.passtNicht, text).toBe(true);
      expect(ergebnis.text, text).toBe(text);
      expect(erkenneFormat(text), text).toBeNull();
    }
    // Gekappt mitten im Dokument: Original mit Vermerk, nie halb formatiert.
    expect(darstellung(XML_KOMPAKT.slice(0, 120), "xml").passtNicht).toBe(true);
  });

  it("ändert außer dem Leerraum zwischen Token kein Zeichen", () => {
    for (const text of [
      XML_KOMPAKT,
      "<a>\n\n\n   <b>x</b>   \n</a>",
      "<p>Hallo <b>fett</b> Welt</p>",
    ]) {
      const ergebnis = darstellung(text, "xml");
      expect(xmlOhneLeerraumZwischenToken(ergebnis.text)).toBe(xmlOhneLeerraumZwischenToken(text));
    }
  });
});

/* ─────────────────────────────────────────────────────────────────────────────
   JSON
   ───────────────────────────────────────────────────────────────────────────── */

describe("JSON", () => {
  it("rückt ein und lässt 12.50, 1e5, die 20-stellige Zahl, das Escape und den doppelten Schlüssel stehen", () => {
    const ergebnis = darstellung(JSON_KOMPAKT, "json");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.text).toBe(JSON_FORMATIERT);
    expect(ergebnis.text).toContain("12.50");
    expect(ergebnis.text).toContain("12345678901234567890");
    expect(ergebnis.text).toContain(ESCAPE_AE);
    expect(ergebnis.text.match(/"k": /g)).toHaveLength(2);
  });

  it("stimmt als Gegenprobe mit JSON.parse überein", () => {
    const ergebnis = darstellung(JSON_KOMPAKT, "json");
    expect(JSON.parse(ergebnis.text)).toEqual(JSON.parse(JSON_KOMPAKT));
    // Und zeigt, warum die Ausgabe nicht diesen Weg nimmt: `JSON.stringify`
    // verlöre genau die drei Dinge, die oben stehen bleiben.
    const neuGeschrieben = JSON.stringify(JSON.parse(JSON_KOMPAKT));
    expect(neuGeschrieben).toContain("12.5,");
    expect(neuGeschrieben).not.toContain("12345678901234567890");
    expect(neuGeschrieben.match(/"k":/g)).toHaveLength(1);
  });

  it("nimmt Listen, Leerraum im Original und einen Vorspann an", () => {
    expect(darstellung("[1,[],[2,3]]", "json").text).toBe(
      "[\n  1,\n  [],\n  [\n    2,\n    3\n  ]\n]",
    );
    expect(darstellung('{ "a" :\n\t1 }\n', "json").text).toBe('{\n  "a": 1\n}\n');
  });

  it("passt nicht bei einer Verletzung der Grammatik oder bei Text hinter dem Wert", () => {
    for (const text of [
      '{"a": }',
      '{"a": 1} x',
      '{"a": [1, 2',
      "{'a': 1}",
      '{"a": 01}',
      '{"a": "x\ny"}',
      '{"a": tru}',
      "[1,]",
    ]) {
      const ergebnis = darstellung(text, "json");
      expect(ergebnis.passtNicht, text).toBe(true);
      expect(ergebnis.text, text).toBe(text);
      expect(erkenneFormat(text), text).toBeNull();
    }
  });

  it("ändert außer dem Leerraum außerhalb von Zeichenketten kein Zeichen", () => {
    for (const text of [JSON_KOMPAKT, "[1,[],[2,3]]", '{ "a b" :\n\t"c  d" }\n']) {
      expect(jsonOhneLeerraum(darstellung(text, "json").text)).toBe(jsonOhneLeerraum(text));
    }
  });
});

/* ─────────────────────────────────────────────────────────────────────────────
   Hex
   ───────────────────────────────────────────────────────────────────────────── */

describe("Hex", () => {
  const ALLE_CODES = Array.from({ length: 256 }, (_, code) => String.fromCharCode(code)).join("");

  it("rechnet über alle Zeichencodes 0x00 bis 0xFF auf das Original zurück", () => {
    const ergebnis = stelleDar(ALLE_CODES, "hex", "ISO_8859_1");
    expect(ergebnis.passtNicht).toBe(false);
    expect(ergebnis.hexGekappt).toBe(false);
    expect(ausHex(ergebnis.text)).toEqual(Array.from({ length: 256 }, (_, code) => code));
    const zeilen = ergebnis.text.split("\n");
    expect(zeilen).toHaveLength(16);
    expect(zeilen[0]).toBe(
      "00000000  00 01 02 03 04 05 06 07  08 09 0a 0b 0c 0d 0e 0f  |................|",
    );
    expect(zeilen[4]).toBe(
      "00000040  40 41 42 43 44 45 46 47  48 49 4a 4b 4c 4d 4e 4f  |@ABCDEFGHIJKLMNO|",
    );
    // 0x7F und alles darüber steht rechts als Punkt.
    expect(zeilen[7]?.endsWith("|pqrstuvwxyz{|}~.|")).toBe(true);
    expect(zeilen[15]?.endsWith("|................|")).toBe(true);
    for (const zeile of zeilen) {
      expect(zeile).toHaveLength(78);
    }
  });

  it("füllt eine unvollständige letzte Zeile auf, wie hexdump -C es tut", () => {
    const ergebnis = stelleDar("ABC", "hex", "ASCII");
    // Der Hex-Bereich ist aufgefuellt, damit das `|` an derselben Stelle steht wie in
    // einer vollen Zeile; der Zeichenbereich rechts ist es nicht (65 statt 78).
    expect(ergebnis.text).toHaveLength(65);
    expect(ergebnis.text.indexOf("|")).toBe(60);
    expect(ergebnis.text.startsWith("00000000  41 42 43 ")).toBe(true);
    expect(ergebnis.text.endsWith(" |ABC|")).toBe(true);
    expect(stelleDar("", "hex", "ASCII").text).toBe("");
  });

  it("endet an der Kappungsgrenze und sagt es", () => {
    const genau = "x".repeat(HEX_GRENZE_BYTES);
    const ohne = stelleDar(genau, "hex", "ASCII");
    expect(ohne.hexGekappt).toBe(false);
    expect(ohne.text.split("\n")).toHaveLength(8192);
    // 8.192 Zeilen zu 78 Zeichen plus 8.191 Zeilenumbrüche — die Zahl aus der Begründung von E‑203.
    expect(ohne.text).toHaveLength(647_167);

    const mehr = stelleDar(genau + "y", "hex", "ASCII");
    expect(mehr.hexGekappt).toBe(true);
    expect(mehr.text).toBe(ohne.text);
    expect(darstellungsvermerke("hex", mehr)).toEqual(["HEX_GEKAPPT", "DOWNLOAD_ORIGINAL"]);
  });

  it("zeigt bei einem Zeichencode über 0xFF das Original mit Vermerk und erfindet kein Byte", () => {
    const text = "Betrag: 12,50 " + String.fromCharCode(0x20ac);
    const ergebnis = stelleDar(text, "hex", "ISO_8859_1");
    expect(ergebnis.passtNicht).toBe(true);
    expect(ergebnis.text).toBe(text);
    expect(darstellungsvermerke("hex", ergebnis)).toEqual(["PASST_NICHT"]);
  });

  it("ist nur bei ASCII und ISO-8859-1 eine Darstellung der Bytes (E‑204)", () => {
    expect(stelleDar("UNB+", "hex", "ASCII").passtNicht).toBe(false);
    expect(stelleDar("UNB+", "hex", "ISO_8859_1").passtNicht).toBe(false);
    // Bei UTF-8 wäre der Zeichencode nicht das Byte — und die BOM fehlte.
    expect(stelleDar("UNB+", "hex", "UTF_8").passtNicht).toBe(true);
    expect(stelleDar("UNB+", "hex", "X_UNBEKANNT").passtNicht).toBe(true);
    expect(stelleDar("UNB+", "hex", null).passtNicht).toBe(true);
  });

  it("wird nie erkannt und ist auch für Text ohne Format wählbar", () => {
    expect(erkenneFormat("Hallo Welt")).toBeNull();
    expect(stelleDar("Hallo Welt", "hex", "ASCII").passtNicht).toBe(false);
  });
});

/* ─────────────────────────────────────────────────────────────────────────────
   Vorspann, Kreuzprobe, Vermerke, Auswahl, Parameter
   ───────────────────────────────────────────────────────────────────────────── */

describe("BOM und vorangestellter Leerraum", () => {
  const BOM_UTF_8 = String.fromCharCode(0xfeff);
  const BOM_ALS_ISO_8859_1 = "ï»¿";

  it.each(FORMATE)("bleiben bei %s erhalten und stören die Erkennung nicht", (format) => {
    const beispiel = BEISPIELE[format];
    const formatiert = darstellung(beispiel, format).text;
    for (const vorspann of [BOM_UTF_8, BOM_ALS_ISO_8859_1, "\n  ", BOM_ALS_ISO_8859_1 + "\r\n"]) {
      const text = vorspann + beispiel;
      expect(erkenneFormat(text), JSON.stringify(vorspann)).toBe(format);
      const ergebnis = darstellung(text, format);
      expect(ergebnis.passtNicht).toBe(false);
      expect(ergebnis.text).toBe(vorspann + formatiert);
    }
  });
});

describe("Die Kreuzprobe", () => {
  it.each(FORMATE)("das Beispiel für %s trifft genau seine Erkennung", (format) => {
    expect(erkannteFormate(BEISPIELE[format])).toEqual([format]);
    expect(erkenneFormat(BEISPIELE[format])).toBe(format);
  });

  it.each(FORMATE)("jede andere Darstellung außer Hex meldet für %s „passt nicht“", (format) => {
    for (const andere of FORMATE) {
      if (andere === format) {
        continue;
      }
      const ergebnis = darstellung(BEISPIELE[format], andere);
      expect(ergebnis.passtNicht, andere).toBe(true);
      expect(ergebnis.text, andere).toBe(BEISPIELE[format]);
    }
    expect(stelleDar(BEISPIELE[format], "hex", "ASCII").passtNicht).toBe(false);
    expect(stelleDar(BEISPIELE[format], "original", "ASCII").text).toBe(BEISPIELE[format]);
  });

  it("merkt für Text ohne Merkmal nichts vor und meldet für jede Darstellung „passt nicht“", () => {
    const text = "Erfundener Text ohne jedes Format.\n";
    expect(erkannteFormate(text)).toEqual([]);
    expect(erkenneFormat(text)).toBeNull();
    for (const format of FORMATE) {
      expect(darstellung(text, format).passtNicht, format).toBe(true);
    }
  });
});

describe("Die Vermerke der Darstellung", () => {
  it("stehen beim Original nie", () => {
    expect(
      darstellungsvermerke("original", stelleDar(EDIFACT_EINZEILIG, "original", "ASCII")),
    ).toEqual([]);
  });

  it("nennen den Download, sobald eine Darstellung angewandt ist", () => {
    expect(darstellungsvermerke("edifact", darstellung(EDIFACT_EINZEILIG, "edifact"))).toEqual([
      "DOWNLOAD_ORIGINAL",
    ]);
    expect(darstellungsvermerke("hex", stelleDar("ABC", "hex", "ASCII"))).toEqual([
      "DOWNLOAD_ORIGINAL",
    ]);
  });

  it("nennen bei „passt nicht“ nur das — angezeigt ist das Original, und das liefert auch der Download", () => {
    expect(darstellungsvermerke("json", darstellung(EDIFACT_EINZEILIG, "json"))).toEqual([
      "PASST_NICHT",
    ]);
  });
});

describe("Ob die Auswahl im Baum steht", () => {
  it("nur bei Nutzdaten im Zustand ANZEIGBAR mit Inhalt", () => {
    expect(darstellungWaehlbar({ zustand: "ANZEIGBAR", art: "NUTZDATEN", text: "x" })).toBe(true);
    expect(darstellungWaehlbar({ zustand: "ANZEIGBAR", art: "PROTOKOLL", text: "x" })).toBe(false);
    expect(darstellungWaehlbar({ zustand: "ANZEIGBAR", art: "NUTZDATEN", text: "" })).toBe(false);
    for (const zustand of [
      "BINAERDATEI",
      "EBCDIC_DATEI",
      "KEIN_ANZEIGBARER_PROTOKOLLTEIL",
      "DATEI_NICHT_VORHANDEN",
      "ABLAGE_NICHT_ERREICHBAR",
    ] as const) {
      expect(darstellungWaehlbar({ zustand, art: "NUTZDATEN", text: "x" }), zustand).toBe(false);
    }
  });
});

describe("Der Parameter darstellung", () => {
  it("kennt genau die acht Werte in der Reihenfolge der Auswahl", () => {
    expect(DARSTELLUNGEN).toEqual([
      "original",
      "edifact",
      "x12",
      "vda",
      "idoc",
      "xml",
      "json",
      "hex",
    ]);
    for (const wert of DARSTELLUNGEN) {
      expect(parseAsDarstellung.parse(wert)).toBe(wert);
      expect(parseAsDarstellung.serialize(wert)).toBe(wert);
    }
  });

  it("macht aus einem unbekannten Wert das Original", () => {
    expect(parseAsDarstellung.parse("edi")).toBeNull();
    expect(parseAsDarstellung.parse("EDIFACT")).toBeNull();
    expect(parseAsDarstellung.parse("")).toBeNull();
    expect(DARSTELLUNG_PARAMETER.darstellung.defaultValue).toBe(DARSTELLUNG_VORGABE);
    expect(DARSTELLUNG_VORGABE).toBe("original");
  });
});
