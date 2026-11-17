import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { zeilenbedienung } from "@/features/nachrichten/zeilenbedienung";

/**
 * **Strg-, Cmd- und Mittelklick auf eine Zeile** *(17.11.2026, E‑1195,
 * `docs/nachrichtendetail.md` §10.2)* — die Entscheidung je Ereignis, ohne Baum.
 *
 * Die Zeile bleibt eine Zeile und wird kein Verweis. Was ein Ereignis auf ihr
 * bedeutet, entscheidet `zeilenbedienung`; beide Tabellen (Nachrichtenliste samt
 * Übertragungsliste und Trefferliste) benutzen dieselbe. Dass sie es tun, belegt
 * `tests/detail-vollansicht.test.tsx` am gerenderten Baum.
 *
 * Die Ereignisse sind gestellt: Gebraucht werden nur die Felder, die die
 * Entscheidung liest, und ein `target`, das `closest` beantwortet.
 */

const ZIEL = "/nachrichten/a-2?zeitraum=7d";

let oeffne: ReturnType<typeof vi.fn>;

beforeEach(() => {
  oeffne = vi.fn(() => null);
  vi.stubGlobal("window", { open: oeffne });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/** Ein Ziel in der Zeile — eine Zelle oder, mit `verweis`, ein Verweis darin. */
function ziel(verweis = false) {
  return { closest: (auswahl: string) => (verweis && auswahl.includes("a") ? {} : null) };
}

function maus(teile: { button?: number; ctrlKey?: boolean; metaKey?: boolean; verweis?: boolean }) {
  const ereignis = {
    button: teile.button ?? 0,
    ctrlKey: teile.ctrlKey ?? false,
    metaKey: teile.metaKey ?? false,
    target: ziel(teile.verweis),
    preventDefault: vi.fn(),
  };
  return ereignis;
}

function taste(teile: { key: string; ctrlKey?: boolean; metaKey?: boolean; vomKind?: boolean }) {
  const zeile = {};
  return {
    key: teile.key,
    ctrlKey: teile.ctrlKey ?? false,
    metaKey: teile.metaKey ?? false,
    currentTarget: zeile,
    target: teile.vomKind ? {} : zeile,
    preventDefault: vi.fn(),
  };
}

function bedienung(mitTab = true) {
  const aufAuswahl = vi.fn();
  const handler = zeilenbedienung("a-2", aufAuswahl, mitTab ? () => ZIEL : undefined);
  // Die Handler erwarten React-Ereignisse; gestellt sind nur die gelesenen Felder.
  const h = handler as unknown as Record<string, (ereignis: unknown) => void>;
  return { aufAuswahl, h };
}

describe("Ein Klick auf die Zeile", () => {
  it("öffnet gewöhnlich das Panel und keinen Tab", () => {
    const { aufAuswahl, h } = bedienung();
    h.onClick(maus({}));

    expect(aufAuswahl).toHaveBeenCalledExactlyOnceWith("a-2");
    expect(oeffne).not.toHaveBeenCalled();
  });

  it("öffnet mit Strg und mit Cmd das Ziel im neuen Tab, mit `noopener`, und nicht das Panel", () => {
    for (const zusatz of [{ ctrlKey: true }, { metaKey: true }]) {
      const { aufAuswahl, h } = bedienung();
      oeffne.mockClear();
      h.onClick(maus(zusatz));

      expect(oeffne, JSON.stringify(zusatz)).toHaveBeenCalledExactlyOnceWith(
        ZIEL,
        "_blank",
        "noopener",
      );
      expect(aufAuswahl).not.toHaveBeenCalled();
    }
  });

  it("lässt einen Klick auf einen Verweis in der Zeile dessen Sache sein", () => {
    const { aufAuswahl, h } = bedienung();
    h.onClick(maus({ verweis: true }));
    h.onClick(maus({ verweis: true, ctrlKey: true }));

    expect(aufAuswahl).not.toHaveBeenCalled();
    expect(oeffne).not.toHaveBeenCalled();
  });

  it("bleibt ohne Ziel für einen neuen Tab beim Panel", () => {
    const { aufAuswahl, h } = bedienung(false);
    h.onClick(maus({ ctrlKey: true }));

    expect(aufAuswahl).toHaveBeenCalledExactlyOnceWith("a-2");
    expect(oeffne).not.toHaveBeenCalled();
  });
});

describe("Die mittlere Maustaste", () => {
  it("öffnet das Ziel im neuen Tab", () => {
    const { aufAuswahl, h } = bedienung();
    const ereignis = maus({ button: 1 });
    h.onAuxClick(ereignis);

    expect(oeffne).toHaveBeenCalledExactlyOnceWith(ZIEL, "_blank", "noopener");
    expect(aufAuswahl).not.toHaveBeenCalled();
  });

  it("unterdrückt beim Drücken das automatische Scrollen — außer auf einem Verweis", () => {
    const { h } = bedienung();

    const aufDerZeile = maus({ button: 1 });
    h.onMouseDown(aufDerZeile);
    expect(aufDerZeile.preventDefault).toHaveBeenCalledOnce();

    const aufDemVerweis = maus({ button: 1, verweis: true });
    h.onMouseDown(aufDemVerweis);
    expect(aufDemVerweis.preventDefault).not.toHaveBeenCalled();

    // Die linke Taste bleibt unberührt: Sie setzt den Fokus auf die Zeile.
    const links = maus({ button: 0 });
    h.onMouseDown(links);
    expect(links.preventDefault).not.toHaveBeenCalled();
  });

  it("übergeht einen Verweis in der Zeile und die rechte Taste", () => {
    const { aufAuswahl, h } = bedienung();
    h.onAuxClick(maus({ button: 1, verweis: true }));
    h.onAuxClick(maus({ button: 2 }));

    expect(oeffne).not.toHaveBeenCalled();
    expect(aufAuswahl).not.toHaveBeenCalled();
  });
});

describe("Die Tastatur auf der fokussierten Zeile", () => {
  it("öffnet mit Enter und Leertaste wie bisher das Panel", () => {
    for (const key of ["Enter", " "]) {
      const { aufAuswahl, h } = bedienung();
      const ereignis = taste({ key });
      h.onKeyDown(ereignis);

      expect(aufAuswahl, key).toHaveBeenCalledExactlyOnceWith("a-2");
      expect(ereignis.preventDefault, key).toHaveBeenCalledOnce();
    }
    expect(oeffne).not.toHaveBeenCalled();
  });

  it("öffnet mit Strg+Enter und Cmd+Enter das Ziel im neuen Tab", () => {
    for (const zusatz of [{ ctrlKey: true }, { metaKey: true }]) {
      const { aufAuswahl, h } = bedienung();
      oeffne.mockClear();
      h.onKeyDown(taste({ key: "Enter", ...zusatz }));

      expect(oeffne, JSON.stringify(zusatz)).toHaveBeenCalledExactlyOnceWith(
        ZIEL,
        "_blank",
        "noopener",
      );
      expect(aufAuswahl).not.toHaveBeenCalled();
    }
  });

  it("überlässt eine Taste aus einem Kind der Zeile dem Kind", () => {
    const { aufAuswahl, h } = bedienung();
    h.onKeyDown(taste({ key: "Enter", vomKind: true }));
    h.onKeyDown(taste({ key: "Enter", ctrlKey: true, vomKind: true }));

    expect(aufAuswahl).not.toHaveBeenCalled();
    expect(oeffne).not.toHaveBeenCalled();
  });
});
