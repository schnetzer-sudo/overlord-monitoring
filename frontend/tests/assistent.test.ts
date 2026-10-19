import { afterEach, describe, expect, it, vi } from "vitest";

import {
  neueAnfrageId,
  prueffehlerAus,
  type AngelegterAuftrag,
  type Prueffehler,
  type Vorschlag,
} from "@/features/assistent/api";
import {
  aktuelleFehler,
  ansagetext,
  bietetNeuesGespraech,
  darfAnlegen,
  darfUebernehmen,
  gespraechSchritt,
  hinweisAus,
  hinweistext,
  karteVon,
  kannSenden,
  neuesGespraech,
  ortVon,
  pruefanfrage,
  sichtbareEintraege,
  ueberschrift,
  type Aktion,
  type Gespraech,
  type Karte,
} from "@/features/assistent/gespraech";
import { texteFuer } from "@/i18n";
import { ProblemFehler } from "@/lib/http";

/**
 * **Die Entscheidungen des Assistenten im Frontend ohne Baum** (A3,
 * `docs/assistent-frontend.md`): die Übergänge des Gesprächs samt Karte, wohin
 * ein Prüffehler gehört, die Überschrift mit Rückfall (E‑1080), wie viele
 * Einträge offen stehen (F6), welcher Hinweis zu welcher Antwort gehört
 * (E‑1081) und die `anfrageId` (E‑1072). Ob jemand diese Regeln abfragt, steht
 * in `tests/assistent-chatbox.test.tsx`.
 */

const DE = texteFuer("de");
const EN = texteFuer("en");

function vorschlag(anzahl = 1): Vorschlag {
  return {
    typ: "WERK_ABLADESTELLE",
    name: "Anpassen der Werk-/Abladestellentabelle",
    aktion: "Anpassung",
    felder: [
      { name: "werk", label: "Werk", hinweis: null },
      { name: "abladestelle", label: "Abladestelle", hinweis: null },
      {
        name: "sndprn",
        label: "SNDPRN",
        hinweis: "Bei Anpassung heißt leer: der Wert wird entfernt.",
      },
    ],
    eintraege: Array.from({ length: anzahl }, (_, nummer) => ({
      werte: { werk: String(72332 + nummer), abladestelle: "DDB0", sndprn: null },
      schluessel: `${72332 + nummer}/DDB0`,
    })),
  };
}

function lauf(start: Gespraech, ...aktionen: Aktion[]): Gespraech {
  return aktionen.reduce(gespraechSchritt, start);
}

/** Ein Gespräch mit einer Antwort samt Karte; die Karte heißt wie ihr Beitrag. */
function mitKarte(anzahl = 1): { zustand: Gespraech; karteId: string } {
  const zustand = lauf(
    neuesGespraech(),
    { art: "entwurf", text: "Bitte ändern" },
    { art: "senden", text: "Bitte ändern" },
    { art: "eroeffnet", gespraechId: "g-1" },
    {
      art: "beantwortet",
      antwort: { text: "Bitte prüfen Sie die Karte.", vorschlag: vorschlag(anzahl) },
      anfrageId: "8f0c2a4e-1d3b-4c5a-9e7f-0a1b2c3d4e5f",
    },
  );
  const letzter = zustand.beitraege.at(-1);
  if (letzter?.art !== "assistent") {
    throw new Error("Keine Antwort im Verlauf");
  }
  return { zustand, karteId: letzter.id };
}

function karte(zustand: Gespraech, karteId: string): Karte {
  const gefunden = karteVon(zustand, karteId);
  if (gefunden === null) {
    throw new Error(`Keine Karte ${karteId}`);
  }
  return gefunden;
}

function problem(status: number, typ: string, angaben: Record<string, unknown> = {}) {
  return new ProblemFehler({ status, typ, angaben });
}

const FEHLER_SNDPRN: Prueffehler = {
  index: 0,
  feld: "sndprn",
  code: "FORMAT",
  meldung: "Eintrag 1, SNDPRN: Der Wert „12076“ hat nicht die erwartete Form.",
};

describe("Das Gespräch", () => {
  it("beginnt mit der Begrüßung, ohne Gespräch beim Backend (E‑1082)", () => {
    const zustand = neuesGespraech();
    expect(zustand.beitraege.map((beitrag) => beitrag.art)).toEqual(["begruessung"]);
    expect(zustand.gespraechId).toBeNull();
    expect(kannSenden(zustand)).toBe(false);
  });

  it("nimmt die Nachricht beim Senden in den Verlauf, leert das Feld und wartet", () => {
    const zustand = lauf(
      neuesGespraech(),
      { art: "entwurf", text: "Zeile 1\nZeile 2" },
      { art: "senden", text: "Zeile 1\nZeile 2" },
    );
    expect(zustand.beitraege.at(-1)).toMatchObject({ art: "kunde", text: "Zeile 1\nZeile 2" });
    expect(zustand.entwurf).toBe("");
    expect(zustand.wartet).toBe(true);
    expect(zustand.ansage).toEqual({ art: "wartet" });
    // Während der Antwort: kein zweites Senden, kein Tippen (E‑1075).
    expect(kannSenden({ ...zustand, entwurf: "noch eine" })).toBe(false);
    expect(gespraechSchritt(zustand, { art: "entwurf", text: "x" })).toBe(zustand);
    expect(gespraechSchritt(zustand, { art: "senden", text: "x" })).toBe(zustand);
  });

  it("sendet nur Leerraum nicht", () => {
    const zustand = lauf(neuesGespraech(), { art: "entwurf", text: "  \n " });
    expect(kannSenden(zustand)).toBe(false);
    expect(gespraechSchritt(zustand, { art: "senden", text: "  \n " })).toBe(zustand);
  });

  it("legt eine Antwort mit Vorschlag als Karte mit ihrer anfrageId in den Verlauf", () => {
    const { zustand, karteId } = mitKarte();
    const neu = karte(zustand, karteId);
    expect(zustand.wartet).toBe(false);
    expect(neu.anfrageId).toBe("8f0c2a4e-1d3b-4c5a-9e7f-0a1b2c3d4e5f");
    expect(neu.modus).toBe("ansicht");
    expect(neu.werte).toEqual([{ werk: "72332", abladestelle: "DDB0", sndprn: null }]);
    expect(zustand.ansage).toEqual({ art: "antwort", beitragId: karteId });
  });

  it("nimmt eine gescheiterte Nachricht aus dem Verlauf und legt sie zurück ins Feld (F4)", () => {
    const zustand = lauf(
      neuesGespraech(),
      { art: "entwurf", text: "Bitte ändern" },
      { art: "senden", text: "Bitte ändern" },
      { art: "gescheitert", text: "Bitte ändern", hinweis: { art: "nichtErreichbar" } },
    );
    expect(zustand.beitraege.map((beitrag) => beitrag.art)).toEqual(["begruessung"]);
    expect(zustand.entwurf).toBe("Bitte ändern");
    expect(zustand.wartet).toBe(false);
    expect(zustand.hinweis).toEqual({ art: "nichtErreichbar" });
    expect(kannSenden(zustand)).toBe(true);
  });

  it("sperrt das Senden nach einem abgelaufenen Gespräch, bis ein neues beginnt (E‑1076)", () => {
    const abgelaufen = lauf(
      neuesGespraech(),
      { art: "entwurf", text: "Hallo" },
      { art: "senden", text: "Hallo" },
      { art: "eroeffnet", gespraechId: "g-1" },
      { art: "gescheitert", text: "Hallo", hinweis: { art: "abgelaufen" } },
    );
    expect(abgelaufen.abgelaufen).toBe(true);
    expect(kannSenden(abgelaufen)).toBe(false);

    const neu = gespraechSchritt(abgelaufen, { art: "neu" });
    expect(neu.abgelaufen).toBe(false);
    expect(neu.gespraechId).toBeNull();
    expect(neu.hinweis).toBeNull();
    // Der Entwurf bleibt — der Kunde schickt ihn im neuen Gespräch.
    expect(neu.entwurf).toBe("Hallo");
    expect(kannSenden(neu)).toBe(true);
  });

  it("beginnt mit „Neues Gespräch“ bei der Begrüßung, mit neuen Kennungen", () => {
    const { zustand, karteId } = mitKarte();
    const neu = gespraechSchritt(zustand, { art: "neu" });
    expect(neu.beitraege.map((beitrag) => beitrag.art)).toEqual(["begruessung"]);
    expect(neu.beitraege[0].id).not.toBe(zustand.beitraege[0].id);
    // Die Antwort einer Prüfung für die alte Karte kommt ins Leere.
    const spaeter = gespraechSchritt(neu, {
      art: "geprueft",
      karteId,
      entwurf: [],
      ergebnis: { stand: "frei" },
    });
    expect(spaeter).toBe(neu);
  });

  it("nimmt eine Antwort, die nach dem Scheitern noch kommt, nicht mehr an", () => {
    const zustand = lauf(
      neuesGespraech(),
      { art: "entwurf", text: "Hallo" },
      { art: "senden", text: "Hallo" },
      { art: "gescheitert", text: "Hallo", hinweis: null },
    );
    const spaeter = gespraechSchritt(zustand, {
      art: "beantwortet",
      antwort: { text: "zu spät", vorschlag: null },
      anfrageId: "x",
    });
    expect(spaeter).toBe(zustand);
  });
});

describe("Die Karte", () => {
  it("prüft im Änderungsmodus und sperrt „Übernehmen“, solange die Prüfung läuft oder Fehler meldet (E‑1071)", () => {
    const { zustand, karteId } = mitKarte();
    const geoeffnet = gespraechSchritt(zustand, { art: "aendern", karteId });
    const vorher = karte(geoeffnet, karteId);
    expect(vorher.modus).toBe("aendern");
    expect(vorher.entwurf).toEqual(vorher.werte);
    expect(vorher.entwurf).not.toBe(vorher.werte);
    expect(darfUebernehmen(vorher)).toBe(true);
    expect(darfAnlegen(vorher)).toBe(false);

    const entwurf = [{ werk: "72332", abladestelle: "DDB0", sndprn: "12076" }];
    const getippt = gespraechSchritt(geoeffnet, { art: "feld", karteId, entwurf });
    expect(karte(getippt, karteId).pruefung).toEqual({ stand: "laeuft" });
    expect(darfUebernehmen(karte(getippt, karteId))).toBe(false);

    const geprueft = gespraechSchritt(getippt, {
      art: "geprueft",
      karteId,
      entwurf,
      ergebnis: { stand: "fehler", fehler: [FEHLER_SNDPRN] },
    });
    expect(darfUebernehmen(karte(geprueft, karteId))).toBe(false);
    expect(aktuelleFehler(karte(geprueft, karteId))).toEqual([FEHLER_SNDPRN]);

    const korrigiert = [{ werk: "72332", abladestelle: "DDB0", sndprn: "120764" }];
    const frei = lauf(
      geprueft,
      { art: "feld", karteId, entwurf: korrigiert },
      { art: "geprueft", karteId, entwurf: korrigiert, ergebnis: { stand: "frei" } },
    );
    expect(darfUebernehmen(karte(frei, karteId))).toBe(true);

    const uebernommen = gespraechSchritt(frei, { art: "uebernehmen", karteId });
    expect(karte(uebernommen, karteId)).toMatchObject({
      modus: "ansicht",
      entwurf: null,
      werte: korrigiert,
    });
    expect(darfAnlegen(karte(uebernommen, karteId))).toBe(true);
  });

  it("verwirft die Antwort auf einen älteren Stand (F5)", () => {
    const { zustand, karteId } = mitKarte();
    const alt = [{ werk: "1", abladestelle: "DDB0", sndprn: null }];
    const neu = [{ werk: "12", abladestelle: "DDB0", sndprn: null }];
    const getippt = lauf(
      zustand,
      { art: "aendern", karteId },
      { art: "feld", karteId, entwurf: alt },
      { art: "feld", karteId, entwurf: neu },
    );
    const spaet = gespraechSchritt(getippt, {
      art: "geprueft",
      karteId,
      entwurf: alt,
      ergebnis: { stand: "frei" },
    });
    expect(spaet).toBe(getippt);
    expect(darfUebernehmen(karte(spaet, karteId))).toBe(false);
  });

  it("stellt mit „Verwerfen“ den vorigen Stand wieder her", () => {
    const { zustand, karteId } = mitKarte();
    const verworfen = lauf(
      zustand,
      { art: "aendern", karteId },
      { art: "feld", karteId, entwurf: [{ werk: "", abladestelle: "", sndprn: "" }] },
      { art: "verwerfen", karteId },
    );
    expect(karte(verworfen, karteId).werte).toEqual(karte(zustand, karteId).werte);
    expect(karte(verworfen, karteId).modus).toBe("ansicht");
    expect(darfUebernehmen(karte(verworfen, karteId))).toBe(false);
  });

  it("übernimmt nicht, solange die Prüfung den Typ nicht kennt oder gestört ist", () => {
    const { zustand, karteId } = mitKarte();
    const entwurf = [{ werk: "1", abladestelle: "DDB0", sndprn: null }];
    for (const ergebnis of [{ stand: "typUnbekannt" }, { stand: "gestoert" }] as const) {
      const danach = lauf(
        zustand,
        { art: "aendern", karteId },
        { art: "feld", karteId, entwurf },
        { art: "geprueft", karteId, entwurf, ergebnis },
      );
      expect(darfUebernehmen(karte(danach, karteId))).toBe(false);
      expect(gespraechSchritt(danach, { art: "uebernehmen", karteId })).toBe(danach);
    }
  });

  it("legt einmal an und zeigt danach die Werte aus der Antwort (E‑1073, F7)", () => {
    const { zustand, karteId } = mitKarte();
    const laeuft = gespraechSchritt(zustand, { art: "anlegenBeginnt", karteId });
    expect(karte(laeuft, karteId).anlegen).toEqual({ stand: "laeuft" });
    expect(darfAnlegen(karte(laeuft, karteId))).toBe(false);
    // Ein zweiter Beginn während des Aufrufs ändert nichts.
    expect(gespraechSchritt(laeuft, { art: "anlegenBeginnt", karteId })).toBe(laeuft);

    const auftrag: AngelegterAuftrag = {
      taskId: "t-1",
      einordnung: "OFFEN",
      // Die Antwort trägt nur Felder mit Wert — und hier einen anderen Wert, wie
      // nach einer Zeitüberschreitung mit derselben anfrageId.
      eintraege: [{ werte: { werk: "11111", abladestelle: "DDB0" } }],
    };
    const angelegt = gespraechSchritt(laeuft, { art: "angelegt", karteId, auftrag });
    const fertig = karte(angelegt, karteId);
    expect(fertig.anlegen).toEqual({ stand: "angelegt", einordnung: "OFFEN" });
    expect(fertig.werte).toEqual([{ werk: "11111", abladestelle: "DDB0", sndprn: null }]);
    expect(darfAnlegen(fertig)).toBe(false);
    expect(gespraechSchritt(angelegt, { art: "aendern", karteId })).toBe(angelegt);
    expect(gespraechSchritt(angelegt, { art: "anlegenBeginnt", karteId })).toBe(angelegt);
  });

  it("sperrt „Auftrag anlegen“ nach Prüffehlern bis zur nächsten Übernahme (F8)", () => {
    const { zustand, karteId } = mitKarte();
    const fehlgeschlagen = lauf(
      zustand,
      { art: "anlegenBeginnt", karteId },
      {
        art: "anlegenGescheitert",
        karteId,
        ergebnis: { stand: "fehler", fehler: [FEHLER_SNDPRN] },
      },
    );
    expect(darfAnlegen(karte(fehlgeschlagen, karteId))).toBe(false);
    expect(aktuelleFehler(karte(fehlgeschlagen, karteId))).toEqual([FEHLER_SNDPRN]);

    // Im Änderungsmodus stehen dieselben Fehler am Feld, bis eine Prüfung etwas anderes sagt.
    const geoeffnet = gespraechSchritt(fehlgeschlagen, { art: "aendern", karteId });
    expect(aktuelleFehler(karte(geoeffnet, karteId))).toEqual([FEHLER_SNDPRN]);
    expect(darfUebernehmen(karte(geoeffnet, karteId))).toBe(false);

    // „Verwerfen“ lässt die Sperre stehen …
    const verworfen = gespraechSchritt(geoeffnet, { art: "verwerfen", karteId });
    expect(darfAnlegen(karte(verworfen, karteId))).toBe(false);

    // … erst eine geprüfte Übernahme hebt sie auf.
    const entwurf = [{ werk: "72332", abladestelle: "DDB0", sndprn: "120764" }];
    const uebernommen = lauf(
      verworfen,
      { art: "aendern", karteId },
      { art: "feld", karteId, entwurf },
      { art: "geprueft", karteId, entwurf, ergebnis: { stand: "frei" } },
      { art: "uebernehmen", karteId },
    );
    expect(darfAnlegen(karte(uebernommen, karteId))).toBe(true);
  });

  it("lässt „Auftrag anlegen“ nach einer Störung frei, mit derselben anfrageId (E‑1072)", () => {
    const { zustand, karteId } = mitKarte();
    const gestoert = lauf(
      zustand,
      { art: "anlegenBeginnt", karteId },
      { art: "anlegenGescheitert", karteId, ergebnis: { stand: "gestoert" } },
    );
    expect(darfAnlegen(karte(gestoert, karteId))).toBe(true);
    expect(karte(gestoert, karteId).anfrageId).toBe(karte(zustand, karteId).anfrageId);
  });

  it("schickt typ, aktion und die Werte — sonst nichts", () => {
    const { zustand, karteId } = mitKarte();
    const offen = karte(zustand, karteId);
    expect(pruefanfrage(offen.vorschlag, offen.werte)).toEqual({
      typ: "WERK_ABLADESTELLE",
      aktion: "Anpassung",
      eintraege: [{ werk: "72332", abladestelle: "DDB0", sndprn: null }],
    });
  });
});

describe("Wohin ein Prüffehler gehört", () => {
  const { zustand, karteId } = mitKarte(2);
  const offen = karte(zustand, karteId);

  it("an das Feld, an den Eintrag oder an die Karte", () => {
    expect(ortVon(FEHLER_SNDPRN, offen)).toEqual({ art: "feld", index: 0, feld: "sndprn" });
    expect(ortVon({ ...FEHLER_SNDPRN, index: 1, feld: null, code: "DOPPELT" }, offen)).toEqual({
      art: "eintrag",
      index: 1,
    });
    expect(ortVon({ ...FEHLER_SNDPRN, index: null, feld: null }, offen)).toEqual({ art: "karte" });
  });

  it("an den Eintrag, wenn die Karte das Feld nicht zeigt, und an die Karte bei einem fremden Index", () => {
    expect(ortVon({ ...FEHLER_SNDPRN, feld: "unbekannt" }, offen)).toEqual({
      art: "eintrag",
      index: 0,
    });
    expect(ortVon({ ...FEHLER_SNDPRN, index: 7 }, offen)).toEqual({ art: "karte" });
    expect(ortVon({ ...FEHLER_SNDPRN, index: null, feld: "anfrageId" }, offen)).toEqual({
      art: "karte",
    });
  });
});

describe("Die Überschrift eines Eintrags (E‑1080)", () => {
  it("ist der Schlüssel, solange der Eintrag unverändert ist, danach keiner", () => {
    const { zustand, karteId } = mitKarte();
    expect(ueberschrift(karte(zustand, karteId), 0)).toBe("72332/DDB0");

    const entwurf = [{ werk: "72333", abladestelle: "DDB0", sndprn: null }];
    const geaendert = lauf(
      zustand,
      { art: "aendern", karteId },
      { art: "feld", karteId, entwurf },
      { art: "geprueft", karteId, entwurf, ergebnis: { stand: "frei" } },
      { art: "uebernehmen", karteId },
    );
    expect(ueberschrift(karte(geaendert, karteId), 0)).toBeNull();
  });

  it("hält leeren Text und null für gleich, wie das Backend", () => {
    const { zustand, karteId } = mitKarte();
    const entwurf = [{ werk: "72332", abladestelle: "DDB0", sndprn: "" }];
    const geleert = lauf(
      zustand,
      { art: "aendern", karteId },
      { art: "feld", karteId, entwurf },
      { art: "geprueft", karteId, entwurf, ergebnis: { stand: "frei" } },
      { art: "uebernehmen", karteId },
    );
    expect(ueberschrift(karte(geleert, karteId), 0)).toBe("72332/DDB0");
  });

  it("ist keiner, wenn der Vorschlag keinen Schlüssel hat", () => {
    const ohne = { ...vorschlag(), eintraege: [{ ...vorschlag().eintraege[0], schluessel: null }] };
    const zustand = lauf(
      neuesGespraech(),
      { art: "entwurf", text: "x" },
      { art: "senden", text: "x" },
      { art: "beantwortet", antwort: { text: "", vorschlag: ohne }, anfrageId: "a" },
    );
    const karteId = zustand.beitraege.at(-1)?.id ?? "";
    expect(ueberschrift(karte(zustand, karteId), 0)).toBeNull();
  });
});

describe("Wie viele Einträge offen stehen (F6)", () => {
  it("bis fünf alle, darüber die ersten fünf", () => {
    expect(sichtbareEintraege(karte(mitKarte(5).zustand, mitKarte(5).karteId))).toBe(5);
    const { zustand, karteId } = mitKarte(20);
    expect(sichtbareEintraege(karte(zustand, karteId))).toBe(5);
  });

  it("alle, wenn „Alle anzeigen“ gewählt ist, im Änderungsmodus und bei einem Fehler in einem verborgenen Eintrag", () => {
    const { zustand, karteId } = mitKarte(20);
    const aufgeklappt = gespraechSchritt(zustand, { art: "aufklappen", karteId, offen: true });
    expect(sichtbareEintraege(karte(aufgeklappt, karteId))).toBe(20);

    const geoeffnet = gespraechSchritt(zustand, { art: "aendern", karteId });
    expect(sichtbareEintraege(karte(geoeffnet, karteId))).toBe(20);

    const fehlerHinten = lauf(
      zustand,
      { art: "anlegenBeginnt", karteId },
      {
        art: "anlegenGescheitert",
        karteId,
        ergebnis: { stand: "fehler", fehler: [{ ...FEHLER_SNDPRN, index: 12 }] },
      },
    );
    expect(sichtbareEintraege(karte(fehlerHinten, karteId))).toBe(20);

    const fehlerVorn = lauf(
      zustand,
      { art: "anlegenBeginnt", karteId },
      {
        art: "anlegenGescheitert",
        karteId,
        ergebnis: { stand: "fehler", fehler: [FEHLER_SNDPRN] },
      },
    );
    expect(sichtbareEintraege(karte(fehlerVorn, karteId))).toBe(5);
  });
});

describe("Der Hinweis zu einem gescheiterten Aufruf (E‑1081)", () => {
  it("bittet bei 429 zu warten — außer beim ausgeschöpften Gespräch", () => {
    expect(hinweisAus(problem(429, "zu-viele-nachrichten", { hoechstensJeStunde: 60 }))).toEqual({
      art: "zuVieleNachrichten",
    });
    // Jede andere 429, etwa die Begrenzung je IP, ebenso.
    expect(hinweisAus(problem(429, "zu-viele-anfragen"))).toEqual({ art: "zuVieleNachrichten" });
    expect(
      hinweisAus(problem(429, "gespraech-ausgeschoepft", { hoechstensNachrichten: 20 })),
    ).toEqual({ art: "ausgeschoepft", hoechstens: 20 });
  });

  it("nennt bei 400 die Höchstzahl aus dem Fehlerformat", () => {
    expect(hinweisAus(problem(400, "nachricht-ungueltig", { hoechstensZeichen: 4000 }))).toEqual({
      art: "nachrichtUngueltig",
      hoechstensZeichen: 4000,
    });
    expect(hinweisAus(problem(400, "nachricht-ungueltig"))).toEqual({
      art: "nachrichtUngueltig",
      hoechstensZeichen: undefined,
    });
  });

  it("sagt bei 502, 503 und ohne Netz: nicht erreichbar", () => {
    for (const fehler of [
      problem(502, "assistent-gestoert"),
      problem(503, "assistent-voruebergehend-gestoert"),
      problem(503, "assistent-nicht-verfuegbar"),
      problem(0, "netzwerk"),
      new Error("kein ProblemFehler"),
    ]) {
      expect(hinweisAus(fehler)).toEqual({ art: "nichtErreichbar" });
    }
  });

  it("heißt bei 404 abgelaufen, sagt bei 409 und 401 nichts", () => {
    expect(hinweisAus(problem(404, "nicht-gefunden"))).toEqual({ art: "abgelaufen" });
    expect(hinweisAus(problem(409, "nachricht-laeuft"))).toBeNull();
    expect(hinweisAus(problem(401, "nicht-angemeldet"))).toBeNull();
  });

  it("fällt bei allem anderen auf den Fehlerkatalog zurück", () => {
    const fehler = problem(403, "kein-mandant-gewaehlt");
    const hinweis = hinweisAus(fehler);
    expect(hinweis).toEqual({ art: "sonstiges", fehler });
    expect(hinweistext(hinweis!, DE, "de")).toBe(DE.fehler["kein-mandant-gewaehlt"]);
  });

  it("bietet „Neues Gespräch“ nur beim abgelaufenen und beim ausgeschöpften Gespräch an", () => {
    expect(bietetNeuesGespraech({ art: "abgelaufen" })).toBe(true);
    expect(bietetNeuesGespraech({ art: "ausgeschoepft", hoechstens: 20 })).toBe(true);
    expect(bietetNeuesGespraech({ art: "zuVieleNachrichten" })).toBe(false);
    expect(bietetNeuesGespraech({ art: "nichtErreichbar" })).toBe(false);
    expect(bietetNeuesGespraech(null)).toBe(false);
  });

  it("formatiert die Zahl in der Sprache und lässt sie weg, wenn sie fehlt", () => {
    expect(hinweistext({ art: "nachrichtUngueltig", hoechstensZeichen: 4000 }, DE, "de")).toBe(
      "Die Nachricht ist leer oder zu lang. Bitte schreiben Sie höchstens 4.000 Zeichen.",
    );
    expect(hinweistext({ art: "nachrichtUngueltig", hoechstensZeichen: 4000 }, EN, "en")).toContain(
      "4,000",
    );
    expect(hinweistext({ art: "nachrichtUngueltig", hoechstensZeichen: undefined }, DE, "de")).toBe(
      DE.assistent.hinweis.nachrichtUngueltigOhneZahl,
    );
    expect(hinweistext({ art: "ausgeschoepft", hoechstens: 20 }, DE, "de")).toContain(
      "20 Nachrichten",
    );
    expect(hinweistext({ art: "ausgeschoepft", hoechstens: undefined }, DE, "de")).toBe(
      DE.assistent.hinweis.ausgeschoepftOhneZahl,
    );
  });
});

describe("Die Ansage (§7)", () => {
  it("sagt das Warten, danach die Antwort und zur Karte nur Typ und Zahl", () => {
    const wartend = lauf(
      neuesGespraech(),
      { art: "entwurf", text: "x" },
      { art: "senden", text: "x" },
    );
    expect(ansagetext(wartend, DE)).toBe(DE.assistent.wartet);

    const { zustand } = mitKarte(3);
    expect(ansagetext(zustand, DE)).toBe(
      "Assistent: Bitte prüfen Sie die Karte. Vorschlag auf der Karte: Anpassen der Werk-/Abladestellentabelle, 3 Einträge.",
    );
  });

  it("schweigt nach einem Fehler — der Hinweis sagt sich selbst an", () => {
    const zustand = lauf(
      mitKarte().zustand,
      { art: "entwurf", text: "y" },
      { art: "senden", text: "y" },
      { art: "gescheitert", text: "y", hinweis: { art: "nichtErreichbar" } },
    );
    expect(ansagetext(zustand, DE)).toBe("");
  });
});

describe("Die Fehlerliste der Prüfung", () => {
  it("liest index, feld, code und meldung und lässt Einträge ohne Meldung weg", () => {
    expect(
      prueffehlerAus([
        { index: 0, feld: "sndprn", code: "FORMAT", meldung: "Eintrag 1, SNDPRN: …" },
        { index: null, feld: null, code: "ZU_VIELE_EINTRAEGE", meldung: "Höchstens 20." },
        { index: 1.5, feld: "", code: "X", meldung: "krumm" },
        { index: 0, feld: "werk", code: "FORMAT" },
        "kein Objekt",
      ]),
    ).toEqual([
      { index: 0, feld: "sndprn", code: "FORMAT", meldung: "Eintrag 1, SNDPRN: …" },
      { index: null, feld: null, code: "ZU_VIELE_EINTRAEGE", meldung: "Höchstens 20." },
      { index: null, feld: null, code: "X", meldung: "krumm" },
    ]);
    expect(prueffehlerAus(undefined)).toEqual([]);
  });
});

describe("Die anfrageId (E‑1072)", () => {
  const UUID4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("ist eine UUID der Version 4", () => {
    expect(neueAnfrageId()).toMatch(UUID4);
    expect(neueAnfrageId()).not.toBe(neueAnfrageId());
  });

  it("entsteht auch ohne sicheren Kontext, aus getRandomValues", () => {
    const echt = globalThis.crypto;
    vi.stubGlobal("crypto", {
      getRandomValues: <T extends ArrayBufferView>(feld: T) => echt.getRandomValues(feld),
    });
    expect(typeof globalThis.crypto.randomUUID).toBe("undefined");
    expect(neueAnfrageId()).toMatch(UUID4);
  });
});
