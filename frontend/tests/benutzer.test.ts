import { describe, expect, it } from "vitest";

import { LEERER_ENTWURF, anfrageAus, type Entwurf } from "@/features/benutzer/anlegen";
import type { Nutzerzeile } from "@/features/benutzer/api";
import {
  PASSWORT_MINDESTLAENGE,
  amEigenenKontoZulaessig,
  brauchtVorwarnung,
  istEigenesKonto,
  passwortBrauchbar,
  type Vorgang,
} from "@/features/benutzer/selbstschutz";
import { MASKE, darfOeffnen, mitAktualisierterZeile } from "@/features/benutzer/zeilen";
import {
  istLetzteZuordnung,
  mengeGeaendert,
  umschalten,
  wahlmoeglichkeiten,
} from "@/features/benutzer/zuordnung";
import { einsetzen } from "@/i18n";
import { de } from "@/i18n/de";
import { en } from "@/i18n/en";
import { fehleranzeige } from "@/lib/fehlertext";
import { ProblemFehler, istKeinZugriff } from "@/lib/http";

/**
 * Die Entscheidungen der Benutzerverwaltung — **ohne Ansicht**.
 *
 * Was hier steht, sind reine Funktionen: die Regel hinter der Vorwarnung (E19),
 * die Mengenersetzung samt der letzten Zuordnung (E10), das Setzen der
 * geänderten Zeile und die Fehlerabbildung. Was sich nur an einem gerenderten
 * Baum belegen lässt, steht in `tests/benutzer-tabelle.test.tsx` — die Bedingung
 * dafür ist in `tests/hilfe/rendern.tsx` beschrieben und wird hier nicht
 * gedehnt.
 */

const ANGEMELDET = "lukas";

function zeile(werte: Partial<Nutzerzeile> = {}): Nutzerzeile {
  return {
    id: 7,
    username: "beispielnutzer",
    role: "MANDANT",
    tenants: ["VOTG"],
    locked: false,
    lockedUntil: null,
    active: true,
    mustChangePassword: false,
    lastLogin: null,
    ...werte,
  };
}

describe("Das eigene Konto (E19)", () => {
  it("wird über den Benutzernamen erkannt", () => {
    expect(istEigenesKonto("lukas", ANGEMELDET)).toBe(true);
    expect(istEigenesKonto("jemand-anders", ANGEMELDET)).toBe(false);
  });

  /**
   * **Der Grenzfall, für den die Regel überhaupt da ist.** `app_user.username`
   * trägt `utf8mb4_general_ci` und vergleicht in der Datenbank ohne Rücksicht
   * auf die Schreibweise; ein `===` in JavaScript tut das nicht. Ein Konto
   * `Admin`, das sich als `admin` anmeldet, sähe seine eigene Zeile sonst als
   * fremde — und bekäme die Warnung genau dann nicht, wenn sie zählt.
   */
  it("erkennt es auch bei abweichender Schreibweise", () => {
    expect(istEigenesKonto("LUKAS", "lukas")).toBe(true);
    expect(istEigenesKonto("Lukas", "lUkAs")).toBe(true);
    expect(istEigenesKonto("lukas", "LUKAS")).toBe(true);
  });

  /**
   * Solange die Selbstauskunft lädt, gibt es keinen Namen. Dann ist die Antwort
   * `false` — der Hinweis bleibt aus, statt falsch zu erscheinen.
   */
  it("ist ohne angemeldeten Namen niemals das eigene", () => {
    expect(istEigenesKonto("lukas", undefined)).toBe(false);
  });
});

/**
 * **Am Backend abgelesen und nicht geraten.** `pruefeEntwertung` läuft nur in
 * der entwertenden Richtung; welche das ist, steht je Vorgang in
 * `BenutzerverwaltungService`. Die Gegenprobe trägt den Test: Ohne die
 * durchlaufenden Richtungen bewiese er nur, dass immer abgelehnt wird.
 */
describe("Was am eigenen Konto durchläuft", () => {
  const faelle: readonly [Vorgang, boolean][] = [
    [{ art: "sperre", gesperrt: true }, false],
    [{ art: "sperre", gesperrt: false }, true],
    [{ art: "aktiv", aktiv: false }, false],
    [{ art: "aktiv", aktiv: true }, true],
    [{ art: "rolle", rolle: "MANDANT" }, false],
    [{ art: "rolle", rolle: "ADMIN" }, true],
    [{ art: "mandanten", mandanten: ["VOTG"] }, true],
    [{ art: "passwort", passwort: "geheimgeheim" }, true],
  ];

  for (const [vorgang, erwartet] of faelle) {
    it(`${vorgang.art} ${JSON.stringify(vorgang)} → ${erwartet ? "läuft" : "409"}`, () => {
      expect(amEigenenKontoZulaessig(vorgang)).toBe(erwartet);
    });
  }
});

describe("Die Vorwarnung", () => {
  const passwort: Vorgang = { art: "passwort", passwort: "geheimgeheim" };
  const sperren: Vorgang = { art: "sperre", gesperrt: true };

  it("erscheint beim eigenen Konto", () => {
    expect(brauchtVorwarnung(passwort, "lukas", ANGEMELDET)).toBe(true);
  });

  it("erscheint auch bei abweichender Schreibweise des Namens", () => {
    expect(brauchtVorwarnung(passwort, "LUKAS", ANGEMELDET)).toBe(true);
  });

  it("erscheint nicht bei einem fremden Konto", () => {
    expect(brauchtVorwarnung(passwort, "jemand-anders", ANGEMELDET)).toBe(false);
    expect(brauchtVorwarnung(sperren, "jemand-anders", ANGEMELDET)).toBe(false);
  });

  /**
   * **Für die verbotenen Richtungen gibt es keine Warnung**, auch am eigenen
   * Konto nicht: Sie verspräche, dass es nach dem Bestätigen passiert, und es
   * passiert nicht. Dort zeigt die Oberfläche die Übersetzung des Problemtyps.
   */
  it("erscheint nicht für eine Richtung, die das Backend ohnehin ablehnt", () => {
    expect(brauchtVorwarnung(sperren, "lukas", ANGEMELDET)).toBe(false);
    expect(brauchtVorwarnung({ art: "aktiv", aktiv: false }, "lukas", ANGEMELDET)).toBe(false);
    expect(brauchtVorwarnung({ art: "rolle", rolle: "MANDANT" }, "lukas", ANGEMELDET)).toBe(false);
  });

  it("erscheint nicht, solange die Selbstauskunft noch lädt", () => {
    expect(brauchtVorwarnung(passwort, "lukas", undefined)).toBe(false);
  });
});

describe("Das Einmalpasswort (E13)", () => {
  it("braucht mindestens zwölf Zeichen", () => {
    expect(PASSWORT_MINDESTLAENGE).toBe(12);
    expect(passwortBrauchbar("a".repeat(11))).toBe(false);
    expect(passwortBrauchbar("a".repeat(12))).toBe(true);
  });

  /**
   * Ob es sich vom bisherigen unterscheidet, prüft **nur** das Backend — per
   * BCrypt-Vergleich gegen den gespeicherten Hash. Ein Nachbau hier wäre nicht
   * nur unmöglich, er wäre die zweite Stelle für dieselbe Regel.
   */
  it("prüft im Browser nichts außer der Länge", () => {
    expect(passwortBrauchbar("dasselbewieschon")).toBe(true);
  });
});

/**
 * **Der Entwurf der Anlegemaske** (9c, E23/E25).
 *
 * Geprüft wird in beide Richtungen: Jede der vier Bedingungen einzeln
 * weggenommen ergibt `null`, der vollständige Entwurf ergibt den Rumpf. Ein Test,
 * der nur den Erfolgsfall zeigt, bestünde auch gegen ein `return rumpf` ohne
 * jede Prüfung.
 */
describe("Der Entwurf der Anlegemaske (E23)", () => {
  const VOLLSTAENDIG: Entwurf = {
    username: "neuer.nutzer",
    role: "MANDANT",
    mandantId: "VOTG",
    initialPassword: "zwoelfzeichen",
  };

  it("ergibt den Rumpf des Endpunkts — vier Felder, ein Mandant", () => {
    expect(anfrageAus(VOLLSTAENDIG)).toEqual({
      username: "neuer.nutzer",
      role: "MANDANT",
      mandantId: "VOTG",
      initialPassword: "zwoelfzeichen",
    });
  });

  it("lässt auch ADMIN zu — die Rolle darf vergeben werden", () => {
    expect(anfrageAus({ ...VOLLSTAENDIG, role: "ADMIN" })?.role).toBe("ADMIN");
  });

  const fehlt: readonly (readonly [string, Partial<Entwurf>])[] = [
    ["ohne Benutzernamen", { username: "" }],
    ["mit einem Benutzernamen aus Leerzeichen", { username: "   " }],
    ["ohne gewählte Rolle", { role: "" }],
    ["mit einer unbekannten Rolle", { role: "BETREUER" }],
    ["ohne gewählten Mandanten", { mandantId: "" }],
    ["mit einem zu kurzen Passwort", { initialPassword: "elfzeichen!" }],
  ];

  for (const [lage, abweichung] of fehlt) {
    it(`ist ${lage} nicht abschickbar`, () => {
      expect(anfrageAus({ ...VOLLSTAENDIG, ...abweichung })).toBeNull();
    });
  }

  /**
   * **Die Länge am Rand**, und zwar an allen drei Stellen: Elf reicht nicht,
   * zwölf reicht, dreizehn erst recht. Eine Grenze, die nur „zu kurz" und „lang
   * genug" kennt, ginge auch mit `>` statt `>=` durch.
   */
  const raender = [
    [11, false],
    [PASSWORT_MINDESTLAENGE, true],
    [13, true],
  ] as const;

  for (const [laenge, abschickbar] of raender) {
    it(`nimmt ein Passwort mit ${laenge} Zeichen ${abschickbar ? "an" : "nicht an"}`, () => {
      const entwurf = { ...VOLLSTAENDIG, initialPassword: "x".repeat(laenge) };
      expect(anfrageAus(entwurf) !== null).toBe(abschickbar);
    });
  }

  it("schickt den Benutzernamen so, wie er getippt wurde", () => {
    // Geprüft wird gegen die getrimmte Fassung, gesendet wird der Wert selbst:
    // Eine Eingabe stillschweigend zu verändern wäre die schlechtere Ehrlichkeit.
    expect(anfrageAus({ ...VOLLSTAENDIG, username: " neuer.nutzer " })?.username).toBe(
      " neuer.nutzer ",
    );
  });

  /**
   * **Der leere Entwurf ist der Zustand nach dem Erfolg** (E25) — und das
   * Passwortfeld ist der Teil, auf den es dabei ankommt: Es steht danach an
   * keiner Stelle mehr, auch nicht im Protokoll.
   */
  it("ist nach dem Zurücksetzen in jedem Feld leer — das Passwort zuerst", () => {
    expect(LEERER_ENTWURF.initialPassword).toBe("");
    expect(Object.values(LEERER_ENTWURF).every((wert) => wert === "")).toBe(true);
    expect(anfrageAus(LEERER_ENTWURF)).toBeNull();
  });
});

/**
 * **Die Sperre gilt in beide Richtungen** (E22) — und es ist dieselbe Regel
 * geblieben, nicht eine zweite daneben. Erweitert worden ist allein, was „offen"
 * sein kann.
 */
describe("Ein Vorgang zur Zeit (E22)", () => {
  it("lässt jede Zeile öffnen, solange nichts offen ist", () => {
    expect(darfOeffnen(null, 7)).toBe(true);
    expect(darfOeffnen(null, MASKE)).toBe(true);
  });

  it("lässt die offene Zeile zu und keine andere auf", () => {
    expect(darfOeffnen(7, 7)).toBe(true);
    expect(darfOeffnen(7, 8)).toBe(false);
  });

  it("sperrt die Maske, solange eine Zeile offen ist", () => {
    expect(darfOeffnen(7, MASKE)).toBe(false);
  });

  it("sperrt jede Zeile, solange die Maske offen ist", () => {
    expect(darfOeffnen(MASKE, 7)).toBe(false);
    expect(darfOeffnen(MASKE, 8)).toBe(false);
  });

  it("lässt die offene Maske zu — das ist ihr Weg wieder zu", () => {
    expect(darfOeffnen(MASKE, MASKE)).toBe(true);
  });
});

describe("Die Mandantenmenge (E4, E10)", () => {
  const waehlbar = [
    { id: "VOTG", name: "VOTG Tanktainer GmbH" },
    { id: "NEXANS", name: "Nexans autoelectric GmbH" },
    { id: "SYSTEM", name: "Systemmandant" },
    { id: "WOC", name: "Without Contract" },
  ];

  /**
   * **`SYSTEM` und `WOC` stehen mit in der Auswahl.** Dieselbe Quelle und
   * dieselbe Behandlung wie die Mandantenauswahl seit Schritt 3, die ebenfalls
   * nichts nachfiltert — wer für einen technischen Mandanten zuständig ist, muss
   * ihm zugeordnet werden können.
   */
  it("siebt die technischen Mandanten nicht aus", () => {
    const auswahl = wahlmoeglichkeiten([], waehlbar).map((eintrag) => eintrag.id);
    expect(auswahl).toContain("SYSTEM");
    expect(auswahl).toContain("WOC");
  });

  it("sortiert nach Kennung", () => {
    expect(wahlmoeglichkeiten([], waehlbar).map((eintrag) => eintrag.id)).toEqual([
      "NEXANS",
      "SYSTEM",
      "VOTG",
      "WOC",
    ]);
  });

  /**
   * Die Menge wird vollständig ersetzt. Eine Auswahl, die einen aktuellen Wert
   * gar nicht darstellen kann, löschte ihn beim nächsten Speichern — lautlos.
   */
  it("nimmt einen zugeordneten Mandanten auf, den es nicht mehr zur Wahl gibt", () => {
    const auswahl = wahlmoeglichkeiten(["ABGESCHALTET"], waehlbar);
    const fremd = auswahl.find((eintrag) => eintrag.id === "ABGESCHALTET");

    expect(fremd).toBeDefined();
    expect(fremd?.nichtWaehlbar).toBe(true);
    expect(fremd?.name).toBeNull();
    expect(auswahl.filter((eintrag) => eintrag.nichtWaehlbar)).toHaveLength(1);
  });

  it("schaltet einen Mandanten an und wieder ab", () => {
    expect(umschalten(["VOTG"], "NEXANS")).toEqual(["VOTG", "NEXANS"]);
    expect(umschalten(["VOTG", "NEXANS"], "VOTG")).toEqual(["NEXANS"]);
  });

  it("lässt die letzte Zuordnung nicht abwählen — für beide Rollen", () => {
    expect(istLetzteZuordnung(["VOTG"], "VOTG")).toBe(true);
    expect(istLetzteZuordnung(["VOTG", "NEXANS"], "VOTG")).toBe(false);
    expect(istLetzteZuordnung(["VOTG"], "NEXANS")).toBe(false);
  });

  /**
   * **Der Tausch bleibt möglich, und das ist die eigentliche Aussage.** Wer von
   * `{VOTG}` auf `{NEXANS}` will, hakt erst `NEXANS` an — dann sind es zwei, und
   * `VOTG` lässt sich wieder abwählen. Ohne diesen Fall wäre die Sperre ein
   * Riegel und keine Bedingung.
   */
  it("erlaubt den Tausch über den Zwischenschritt", () => {
    const zwei = umschalten(["VOTG"], "NEXANS");
    expect(istLetzteZuordnung(zwei, "VOTG")).toBe(false);
    expect(umschalten(zwei, "VOTG")).toEqual(["NEXANS"]);
  });

  /**
   * Verglichen wird als **Menge** und nicht als Liste: Ein Vergleich über die
   * Reihenfolge meldete eine Änderung, wo keine ist — und schickte ein `PUT`,
   * das alle Sitzungen des Kontos verwirft (E5), ohne dass sich etwas ändert.
   */
  it("erkennt eine Änderung als Menge und nicht als Reihenfolge", () => {
    expect(mengeGeaendert(["VOTG", "NEXANS"], ["NEXANS", "VOTG"])).toBe(false);
    expect(mengeGeaendert(["VOTG"], ["VOTG"])).toBe(false);
    expect(mengeGeaendert(["VOTG", "NEXANS"], ["VOTG"])).toBe(true);
    expect(mengeGeaendert(["VOTG"], ["NEXANS"])).toBe(true);
  });
});

describe("Die geänderte Zeile im Zwischenspeicher", () => {
  it("steht an ihrer Stelle und wird nicht angehängt", () => {
    const liste = [zeile({ id: 1, username: "a" }), zeile({ id: 2, username: "b" })];
    const neu = zeile({ id: 1, username: "a", locked: true });

    const danach = mitAktualisierterZeile(liste, neu);

    expect(danach).toHaveLength(2);
    expect(danach[0].locked).toBe(true);
    expect(danach[1].username).toBe("b");
  });

  /**
   * Verglichen wird über die `id` und nicht über den Benutzernamen: Der ist in
   * der Datenbank ohne Rücksicht auf die Schreibweise eindeutig, ein Vergleich
   * in JavaScript ist es nicht.
   */
  it("vergleicht über die id", () => {
    const liste = [zeile({ id: 1, username: "Lukas" })];
    const danach = mitAktualisierterZeile(liste, zeile({ id: 1, username: "lukas", locked: true }));

    expect(danach[0].locked).toBe(true);
    expect(danach[0].username).toBe("lukas");
  });

  it("lässt die Liste unverändert, wenn die Zeile nicht darin steht", () => {
    const liste = [zeile({ id: 1 })];
    expect(mitAktualisierterZeile(liste, zeile({ id: 99 }))).toEqual(liste);
  });
});

/**
 * **Jeder Problemtyp dieser Seite hat eine eigene Übersetzung**, und zwar in
 * beiden Sprachen (`docs/frontend-grundlagen.md` §6). Ohne sie fiele die Anzeige
 * auf `detail` zurück — deutsch, aber in einer englischen Oberfläche.
 */
describe("Die Fehlerabbildung", () => {
  const TYPEN = [
    "selbstschutz",
    "letzter-admin",
    "letzte-mandantenzuordnung",
    "rolle-ohne-mandant",
    "unbekannte-rolle",
    "passwort-zu-kurz",
    "passwort-unveraendert",
    "nicht-gefunden",
    "konto-administrativ-gesperrt",
    // Seit 9c erreichbar: Die Maske ruft `POST /api/admin/users`, und beide
    // Antworten kommen von dort. `benutzername-zu-lang` steht in keiner
    // Auftragsvorgabe — er ist am Code abgelesen (`AdminUserService`) und wäre
    // ohne Schlüssel ein deutscher Satz in einer englischen Oberfläche.
    "benutzername-vergeben",
    "benutzername-zu-lang",
  ] as const;

  for (const typ of TYPEN) {
    it(`übersetzt ${typ} in beiden Sprachen`, () => {
      const antwort = new ProblemFehler({
        status: 409,
        typ,
        detail: "Text aus dem Backend.",
      });

      for (const sprachdatei of [de, en]) {
        const katalog: Record<string, string | undefined> = sprachdatei.fehler;
        expect(katalog[typ], `${typ} fehlt`).toBeDefined();
        expect(fehleranzeige(antwort, sprachdatei).text).toBe(katalog[typ]);
      }
    });
  }

  /**
   * **`409` ist kein Rechteproblem.** Die Eingabe ist in Ordnung, der Zustand
   * verbietet sie — deshalb nennt jeder der vier, was zu tun ist, damit es doch
   * geht. Genau das unterscheidet ihn von `zugriff-verweigert`, wo es nichts zu
   * tun gibt; klängen sie gleich, wäre die Unterscheidung im Backend umsonst.
   */
  it("lässt die vier 409 nicht wie ein 403 klingen", () => {
    const vier = [
      "selbstschutz",
      "letzter-admin",
      "letzte-mandantenzuordnung",
      "rolle-ohne-mandant",
    ] as const;

    for (const sprachdatei of [de, en]) {
      const katalog: Record<string, string> = sprachdatei.fehler;
      for (const typ of vier) {
        expect(katalog[typ]).not.toBe(katalog["zugriff-verweigert"]);
        expect(katalog[typ].length).toBeGreaterThan(katalog["zugriff-verweigert"].length / 2);
      }
    }
  });

  /**
   * Die administrative Sperre bekommt einen **eigenen** Text auf der
   * Anmeldeseite. Der bestehende sagt „nach mehreren Fehlversuchen" und wäre bei
   * einem Verwaltungsakt eine falsche Auskunft — und der Unterschied, auf den es
   * ankommt, ist, dass diese Sperre **nicht** von selbst abläuft.
   */
  it("unterscheidet die administrative von der automatischen Sperre", () => {
    for (const sprachdatei of [de, en]) {
      expect(sprachdatei.fehler["konto-administrativ-gesperrt"]).not.toBe(
        sprachdatei.fehler["konto-gesperrt"],
      );
    }
  });
});

/**
 * Der Zustand „kein Zugriff" — **erkannt am Problemtyp und nicht am
 * Statuscode.** `403` ist in diesem Backend dreifach vergeben, und die drei
 * bedeuten Verschiedenes; eine Prüfung auf den Status zeigte allen dreien
 * dieselbe Meldung, und zwei davon wären falsch.
 */
describe("Kein Zugriff", () => {
  function dreihundertdrei(typ: string) {
    return new ProblemFehler({ status: 403, typ });
  }

  it("greift bei zugriff-verweigert", () => {
    expect(istKeinZugriff(dreihundertdrei("zugriff-verweigert"))).toBe(true);
  });

  it("greift bei keinem der beiden anderen 403", () => {
    expect(istKeinZugriff(dreihundertdrei("csrf-token-ungueltig"))).toBe(false);
    expect(istKeinZugriff(dreihundertdrei("kein-mandant-gewaehlt"))).toBe(false);
  });

  it("greift nicht bei einem Fehler ohne Problemtyp", () => {
    expect(istKeinZugriff(new Error("kaputt"))).toBe(false);
    expect(istKeinZugriff(undefined)).toBe(false);
  });
});

/**
 * **Die Texte der Anlegemaske, in beiden Sprachen** (9c).
 *
 * Dass beide Sprachdateien denselben Schlüsselsatz tragen, prüft
 * `tests/sprachdateien.test.ts` — und dass keiner leer ist, ebenfalls. Was dort
 * **nicht** auffiele, ist eine Übersetzung, die eine Einsetzstelle verliert:
 * `{laenge}` oder `{benutzer}` fehlt, der Satz bleibt lesbar, und die Zahl bzw.
 * der Name steht einfach nicht mehr da. Genau das ist hier geprüft, samt der
 * Gegenprobe, dass die Einsetzung wirklich etwas ersetzt.
 */
describe("Die Texte der Anlegemaske", () => {
  for (const [sprache, sprachdatei] of [
    ["de", de],
    ["en", en],
  ] as const) {
    it(`nennt die Mindestlänge über eine Einsetzstelle (${sprache})`, () => {
      const roh = sprachdatei.benutzer.anlegen.passwortHinweis;
      expect(roh).toContain("{laenge}");
      expect(einsetzen(roh, { laenge: String(PASSWORT_MINDESTLAENGE) })).toContain("12");
    });

    it(`nennt im Erfolgssatz Konto, Rolle und Mandant (${sprache})`, () => {
      const roh = sprachdatei.benutzer.anlegen.erfolgText;
      for (const stelle of ["{benutzer}", "{rolle}", "{mandant}"]) {
        expect(roh, `${stelle} fehlt`).toContain(stelle);
      }
      const gesetzt = einsetzen(roh, {
        benutzer: "neuer.nutzer",
        rolle: sprachdatei.rolle.MANDANT,
        mandant: "VOTG",
      });
      expect(gesetzt).not.toContain("{");
      expect(gesetzt).toContain("neuer.nutzer");
      expect(gesetzt).toContain("VOTG");
    });

    /**
     * **Der Hinweis aus E23 sagt, wo weitere Mandanten hinzukommen** — und nicht
     * nur, dass es hier einer ist. Ohne den zweiten Teil läse sich die Maske wie
     * eine Grenze des Werkzeugs statt wie eine Reihenfolge.
     */
    it(`sagt, wo weitere Mandanten hinzukommen (${sprache})`, () => {
      expect(sprachdatei.benutzer.anlegen.mandantenHinweis.length).toBeGreaterThan(40);
    });
  }
});
