import { describe, expect, it } from "vitest";

import type { Katalogzeile } from "@/features/katalog/api";
import {
  AUFFANGKENNUNG,
  LEERER_FILTER,
  alsSuchparameter,
  ausSuchparametern,
  istAuffangprozess,
  sichtbareZeilen,
  type Katalogfilter,
} from "@/features/katalog/filter";

/**
 * Die zwei Filter der Pflegeliste — **beide in der URL**, damit man zeigen kann,
 * was man sieht.
 *
 * Geprüft werden die Entscheidungen und nicht das Markup: dass ein geteilter
 * Link denselben Ausschnitt herstellt, dass eine Vorgabe **nicht** in der URL
 * steht — und vor allem, dass der Filter „nur mit Nachrichten" die **nie
 * geprüften** Zeilen stehen lässt.
 */

function zeile(werte: Partial<Katalogzeile> & { processId: string }): Katalogzeile {
  return {
    projectId: "300_KundenEingehend",
    projectName: "Kunden eingehend",
    processName: null,
    partner: null,
    richtung: null,
    pflegestatus: "OFFEN",
    vorschlagHerkunft: "KEINE",
    traegtNachrichten: null,
    bestandGeprueftAm: null,
    ...werte,
  };
}

const LEBEND = zeile({ processId: "40000_AMG_LAB_VDA", traegtNachrichten: true });
const TOT = zeile({ processId: "40001_AMG_LAB_EDI", traegtNachrichten: false });
const UNGEPRUEFT = zeile({ processId: "40002_AMG_LAB_XML", traegtNachrichten: null });

describe("URL → Zustand", () => {
  it("stellt einen geteilten Link vollständig wieder her", () => {
    const url = new URLSearchParams("nurOffene=true&nurMitNachrichten=true");

    expect(ausSuchparametern(url)).toEqual({ nurOffene: true, nurMitNachrichten: true });
  });

  it("liest eine leere URL als beide Vorgaben — und die zeigen alles", () => {
    expect(ausSuchparametern(new URLSearchParams())).toEqual(LEERER_FILTER);
    expect(LEERER_FILTER).toEqual({ nurOffene: false, nurMitNachrichten: false });
  });

  it("übergeht einen unbrauchbaren Wert, statt die Liste zu sprengen", () => {
    // Ein alter oder von Hand gebauter Link. `ja` ist kein Wahrheitswert und
    // verhält sich deshalb wie ein fehlender Parameter.
    const url = new URLSearchParams("nurOffene=ja&nurMitNachrichten=1");

    expect(ausSuchparametern(url)).toEqual(LEERER_FILTER);
  });

  it("liest die beiden Filter unabhängig voneinander", () => {
    expect(ausSuchparametern(new URLSearchParams("nurOffene=true"))).toEqual({
      nurOffene: true,
      nurMitNachrichten: false,
    });
    expect(ausSuchparametern(new URLSearchParams("nurMitNachrichten=true"))).toEqual({
      nurOffene: false,
      nurMitNachrichten: true,
    });
  });
});

describe("Zustand → URL", () => {
  it("schreibt eine Vorgabe nicht in die URL", () => {
    // Kein `clearOnDefault: false`: Beide Vorgaben zeigen alles und lassen
    // nichts weg. Ein Standardwert, der etwas weglässt, gehörte in die URL —
    // dieser setzt nichts weg, und eine leere URL zeigt die volle Liste.
    expect([...alsSuchparameter(LEERER_FILTER).entries()]).toEqual([]);
  });

  it("schreibt beide gesetzten Filter, auch in Kombination", () => {
    const url = alsSuchparameter({ nurOffene: true, nurMitNachrichten: true });

    expect(url.get("nurOffene")).toBe("true");
    expect(url.get("nurMitNachrichten")).toBe("true");
  });

  it("läuft in beide Richtungen rund", () => {
    const faelle: Katalogfilter[] = [
      { nurOffene: false, nurMitNachrichten: false },
      { nurOffene: true, nurMitNachrichten: false },
      { nurOffene: false, nurMitNachrichten: true },
      { nurOffene: true, nurMitNachrichten: true },
    ];

    for (const filter of faelle) {
      expect(ausSuchparametern(alsSuchparameter(filter))).toEqual(filter);
    }
  });
});

describe("Der Filter „nur mit Nachrichten“", () => {
  it("zeigt ohne Haken alle drei Zustände", () => {
    const alle = sichtbareZeilen([LEBEND, TOT, UNGEPRUEFT], LEERER_FILTER);

    expect(alle.map((z) => z.processId)).toEqual([
      LEBEND.processId,
      TOT.processId,
      UNGEPRUEFT.processId,
    ]);
  });

  it("lässt die **nie geprüften** Zeilen stehen und wirft nur die toten weg", () => {
    // Die Stelle, an der die drei Zustände aus E14 tragen: `null` heißt „noch
    // nie geprüft" und nicht „ohne Nachrichten". Eine Zeile, für die nie ein
    // Bestandslauf lief, verschwände sonst aus BEIDEN Filterstellungen und wäre
    // über die Oberfläche nicht mehr erreichbar.
    const uebrig = sichtbareZeilen([LEBEND, TOT, UNGEPRUEFT], {
      nurOffene: false,
      nurMitNachrichten: true,
    });

    expect(uebrig.map((z) => z.processId)).toEqual([LEBEND.processId, UNGEPRUEFT.processId]);
  });

  it("wirft vor dem ersten Bestandslauf nichts weg", () => {
    // Die Gegenprobe zur vorigen: Stünde `null` für „ohne Nachrichten", wäre
    // die Liste hier leer — und der Filter der einzige Weg in eine Ansicht, die
    // keinen Prozess mehr zeigt, obwohl es welche gibt.
    const frisch = [UNGEPRUEFT, zeile({ processId: "40003_X_Y", traegtNachrichten: null })];

    expect(sichtbareZeilen(frisch, { nurOffene: false, nurMitNachrichten: true })).toHaveLength(2);
  });

  it("gibt eine eigene Liste zurück und nicht die übergebene", () => {
    const eingabe = [LEBEND];
    const ausgabe = sichtbareZeilen(eingabe, LEERER_FILTER);

    expect(ausgabe).toEqual(eingabe);
    expect(ausgabe).not.toBe(eingabe);
  });
});

describe("Der Auffangprozess", () => {
  it("wird an `Undefined` erkannt, in beiden gemessenen Schreibweisen", () => {
    // Echte Auffangprozesse sind im Bestand zwei (M78): `00001_Undefined` bei
    // `VOTG` und `Undefined` bei `SYSTEM`.
    expect(istAuffangprozess("00001_Undefined")).toBe(true);
    expect(istAuffangprozess("Undefined")).toBe(true);
    expect(AUFFANGKENNUNG).toBe("Undefined");
  });

  it("greift **niemals** auf einen Nummernpräfix aus Nullen", () => {
    // Der zuerst beauftragte Ausdruck `^0+_` hatte sechs Treffer, von denen
    // vier regulär benannte Prozesse sind — darunter einer mit 1.602
    // Nachrichten. Ein Filter darauf erklärte den größten davon zum
    // Auffangbecken.
    expect(istAuffangprozess("00023_BAYER_ORDERS")).toBe(false);
    expect(istAuffangprozess("0_VTG_SalesInvoice")).toBe(false);
    expect(istAuffangprozess("000_KE_OSTROV_INVOIC")).toBe(false);
  });

  it("unterscheidet Groß- und Kleinschreibung", () => {
    // Gemessen ist die Schreibweise `Undefined`. Eine unscharfe Prüfung wäre
    // eine Annahme über Werte, die niemand erhoben hat (Regel Q4).
    expect(istAuffangprozess("00001_undefined")).toBe(false);
    expect(istAuffangprozess("00001_UNDEFINED")).toBe(false);
  });
});
