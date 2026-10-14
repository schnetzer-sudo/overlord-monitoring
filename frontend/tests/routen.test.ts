import { describe, expect, it } from "vitest";

import { NACHRICHTEN_PARAMETER } from "@/features/nachrichten/filter";
import { NAVIGATION, istAktiv } from "@/lib/navigation";
import {
  NACHRICHT_PARAMETER,
  ROUTEN,
  ansichtNebenListe,
  ansichtOhneListe,
  artefaktAnsicht,
  dateiansichtAus,
  sicheresZiel,
  zeigtNachricht,
} from "@/lib/routen";

/**
 * Der `weiter`-Parameter bringt den Nutzer nach der Anmeldung dorthin zurück,
 * wo er hinwollte. Ungeprüft wäre er eine offene Weiterleitung: Ein Link auf
 * `/anmeldung?weiter=https://…` führte nach erfolgreicher Anmeldung auf eine
 * fremde Seite — mit dem Vertrauen, das der Nutzer gerade dieser Anwendung
 * entgegengebracht hat.
 */
describe("weiter-Ziel", () => {
  it("nimmt einen Pfad innerhalb der Anwendung", () => {
    expect(sicheresZiel("/nachrichten")).toBe("/nachrichten");
    expect(sicheresZiel("/nachrichten?von=2026-07-08T00:00:00Z")).toBe(
      "/nachrichten?von=2026-07-08T00:00:00Z",
    );
  });

  it("weist alles zurück, was nach draußen führen könnte", () => {
    for (const ziel of [
      "https://beispiel.invalid/",
      "//beispiel.invalid/",
      "/\\beispiel.invalid",
      "/pfad\\mit\\backslash",
      "javascript:alert(1)",
      "",
      null,
      undefined,
    ]) {
      expect(sicheresZiel(ziel), `Ziel: ${String(ziel)}`).toBe(ROUTEN.startseite);
    }
  });

  it("führt nicht auf die Anmeldung zurück", () => {
    expect(sicheresZiel(ROUTEN.anmeldung)).toBe(ROUTEN.startseite);
    expect(sicheresZiel("/anmeldung?weiter=/")).toBe(ROUTEN.startseite);
  });
});

/**
 * Der Umschalter zwischen den beiden Einhängepunkten der Detailansicht
 * (`docs/nachrichtendetail.md` §10.7).
 *
 * **Die Entscheidung ist die Zielroute**, und die steht deshalb als reine
 * Funktion in `lib/routen.ts` und nicht als Ausdruck in einer Komponente. Zwei
 * Dinge können dabei schiefgehen, und beide sind still: die Kennung zweimal im
 * Ziel, und ein Filter, der beim Hin- und Herschalten verloren geht.
 */
const KENNUNG = "8f3a1c2e-0000-4000-8000-000000000001";

/**
 * Ein Filterstand, wie er tatsächlich in der Adresszeile steht — mit einem
 * `von`, dessen Doppelpunkte **nicht** umkodiert werden dürfen, und mit zwei
 * gleichnamigen `status`-Paaren, die eine Neuordnung sofort auffallen ließe.
 */
const FILTER =
  "zeitraum=24h&von=2026-07-08T00:00:00Z&status=FEHLER&status=WARTEND&sortierung=neueste";

describe("Ohne Liste anzeigen", () => {
  it("führt ohne Filter auf die nackte Route", () => {
    expect(ansichtOhneListe(KENNUNG, "")).toBe(`/nachrichten/${KENNUNG}`);
    // Auch das leere `?` erzeugt keinen Anhang: `window.location.search` ist
    // dann `""`, aber eine von Hand gebaute URL kann `"?"` liefern.
    expect(ansichtOhneListe(KENNUNG, "?")).toBe(`/nachrichten/${KENNUNG}`);
  });

  it("nimmt jeden Filter unverändert und in seiner Reihenfolge mit", () => {
    expect(ansichtOhneListe(KENNUNG, `?${FILTER}`)).toBe(`/nachrichten/${KENNUNG}?${FILTER}`);
  });

  it("entfernt `nachricht` — die Kennung steht danach genau einmal im Ziel", () => {
    const ziel = ansichtOhneListe(
      KENNUNG,
      `?zeitraum=24h&${NACHRICHT_PARAMETER}=${KENNUNG}&sortierung=neueste`,
    );

    expect(ziel).toBe(`/nachrichten/${KENNUNG}?zeitraum=24h&sortierung=neueste`);
    expect(ziel).not.toContain(`${NACHRICHT_PARAMETER}=`);
    expect(ziel.split(KENNUNG)).toHaveLength(2);
  });

  it("bleibt bei einem `nachricht` am Anfang und am Ende dabei", () => {
    // Die drei Stellungen einzeln, weil das Zerlegen an `&` bei der ersten und
    // der letzten je einen Rand hat, an dem sich ein Fehler versteckt.
    expect(ansichtOhneListe(KENNUNG, `?${NACHRICHT_PARAMETER}=${KENNUNG}`)).toBe(
      `/nachrichten/${KENNUNG}`,
    );
    expect(ansichtOhneListe(KENNUNG, `?zeitraum=24h&${NACHRICHT_PARAMETER}=${KENNUNG}`)).toBe(
      `/nachrichten/${KENNUNG}?zeitraum=24h`,
    );
  });
});

describe("Neben der Liste anzeigen", () => {
  it("führt ohne Filter auf die Liste mit gesetzter Kennung", () => {
    expect(ansichtNebenListe(KENNUNG, "")).toBe(`/nachrichten?${NACHRICHT_PARAMETER}=${KENNUNG}`);
  });

  it("setzt die Kennung und lässt alles andere unverändert stehen", () => {
    expect(ansichtNebenListe(KENNUNG, `?${FILTER}`)).toBe(
      `/nachrichten?${NACHRICHT_PARAMETER}=${KENNUNG}&${FILTER}`,
    );
  });

  it("setzt sie auch dann nur einmal, wenn schon eine dasteht", () => {
    const ziel = ansichtNebenListe(KENNUNG, `?${NACHRICHT_PARAMETER}=alt&zeitraum=24h`);

    expect(ziel).toBe(`/nachrichten?${NACHRICHT_PARAMETER}=${KENNUNG}&zeitraum=24h`);
    expect(ziel.split(`${NACHRICHT_PARAMETER}=`)).toHaveLength(2);
  });
});

describe("Kennungen mit Sonderzeichen", () => {
  /**
   * `MessageID` ist eine `varchar(36)`-UUID und trägt heute keine Sonderzeichen.
   * Der Wert kommt aber aus einem fremden Schema und wird hier in einen **Pfad**
   * geschrieben — ein ungeschütztes `?` oder `#` schnitte die halbe URL ab.
   */
  const ROH = "a b/c?d&e=f#g";
  const KODIERT = "a%20b%2Fc%3Fd%26e%3Df%23g";

  it("kodiert im Pfad genau einmal", () => {
    const ziel = ansichtOhneListe(ROH, "?zeitraum=24h");

    expect(ziel).toBe(`/nachrichten/${KODIERT}?zeitraum=24h`);
    // Nicht doppelt: Ein zweiter Durchlauf machte aus `%20` ein `%2520`.
    expect(ziel).not.toContain("%25");
    expect(decodeURIComponent(ziel.slice("/nachrichten/".length).split("?")[0])).toBe(ROH);
  });

  it("kodiert im Parameter genau einmal", () => {
    const ziel = ansichtNebenListe(ROH, "?zeitraum=24h");

    expect(ziel).toBe(`/nachrichten?${NACHRICHT_PARAMETER}=${KODIERT}&zeitraum=24h`);
    expect(ziel).not.toContain("%25");
    expect(new URLSearchParams(ziel.split("?")[1]).get(NACHRICHT_PARAMETER)).toBe(ROH);
  });
});

/**
 * **Eine Ebene zurück — nur mit Beleg** *(14.10.2026, E‑241)*.
 *
 * Die Dateiansicht geht per `router.back()` zurück, wenn der vorige Eintrag im
 * Verlauf **dieselbe Nachricht** zeigt: neben der Liste oder auf ihrer eigenen
 * Route. Dann ist die Adresse danach zeichengleich die von vorher, und die
 * Nachricht steht in derselben Darstellung wie vorher. Fehlt der Beleg, bleibt
 * es beim Ziel des Verweises. Ein Schritt zurück ohne Beleg könnte aus der
 * Anwendung hinausführen (S5).
 *
 * Geprüft wird die Entscheidung als reine Funktion; woher die Adresse kommt
 * (Navigation API), steht in `lib/browserverlauf.ts` und in den gerenderten
 * Fällen von `tests/artefakt-ansicht.test.tsx`.
 */
describe("Der Beleg für den Schritt zurück", () => {
  const HERKUNFT = "https://overlord.example";
  const ID = "8f3a1c2e-0000-4000-8000-000000000001";

  it("erkennt die Nachricht neben der Liste, mit jedem Filter daneben", () => {
    expect(zeigtNachricht(`${HERKUNFT}/nachrichten?nachricht=${ID}`, ID, HERKUNFT)).toBe(true);
    expect(
      zeigtNachricht(
        `${HERKUNFT}/nachrichten?zeitraum=7d&status=FEHLER&prozess=P-0815&suche=Versand&nachricht=${ID}`,
        ID,
        HERKUNFT,
      ),
    ).toBe(true);
  });

  it("erkennt die Nachricht auf ihrer eigenen Route, mit und ohne Filter", () => {
    expect(zeigtNachricht(`${HERKUNFT}/nachrichten/${ID}`, ID, HERKUNFT)).toBe(true);
    expect(zeigtNachricht(`${HERKUNFT}/nachrichten/${ID}?status=FEHLER`, ID, HERKUNFT)).toBe(true);
  });

  it("verlangt dieselbe Nachricht", () => {
    expect(zeigtNachricht(`${HERKUNFT}/nachrichten?nachricht=andere`, ID, HERKUNFT)).toBe(false);
    expect(zeigtNachricht(`${HERKUNFT}/nachrichten/andere`, ID, HERKUNFT)).toBe(false);
    // Die Liste ohne geöffnete Nachricht ist eine Ebene zu weit.
    expect(zeigtNachricht(`${HERKUNFT}/nachrichten?status=FEHLER`, ID, HERKUNFT)).toBe(false);
  });

  it("nimmt keine andere Ansicht und keine andere Station", () => {
    for (const adresse of [
      // Prozessansicht und Belegsuche bleiben, wie sie sind (Auftrag, „Was
      // nicht gebaut wird").
      `${HERKUNFT}/prozesse?prozess=P-0815&nachricht=${ID}`,
      `${HERKUNFT}/suche?begriff=ERFUNDEN&nachricht=${ID}`,
      `${HERKUNFT}${artefaktAnsicht(ID, "2-Converter.Payload.GUID")}`,
      `${HERKUNFT}/`,
    ]) {
      expect(zeigtNachricht(adresse, ID, HERKUNFT), adresse).toBe(false);
    }
  });

  it("verlangt dieselbe Herkunft und überhaupt einen Beleg", () => {
    expect(zeigtNachricht(`https://fremd.example/nachrichten?nachricht=${ID}`, ID, HERKUNFT)).toBe(
      false,
    );
    expect(zeigtNachricht(null, ID, HERKUNFT)).toBe(false);
    expect(zeigtNachricht("keine Adresse", ID, HERKUNFT)).toBe(false);
  });

  it("vergleicht Kennungen mit Sonderzeichen dekodiert", () => {
    const ROH = "a b/c?d&e=f#g";

    expect(zeigtNachricht(`${HERKUNFT}${ansichtOhneListe(ROH, "")}`, ROH, HERKUNFT)).toBe(true);
    expect(zeigtNachricht(`${HERKUNFT}${ansichtNebenListe(ROH, "")}`, ROH, HERKUNFT)).toBe(true);
  });
});

/**
 * **Woher man zurückkam** *(14.10.2026, E‑241)* — der Pfad der Datei, deren
 * Auslöser nach der Rückkehr den Fokus bekommt (S4). Er muss **zeichengleich**
 * das `href` des Verweises sein, denn daran wird der Auslöser gefunden.
 */
describe("Die Datei, aus der man zurückkam", () => {
  const HERKUNFT = "https://overlord.example";
  const ID = "8f3a1c2e-0000-4000-8000-000000000001";
  const ARTEFAKT = "2-Converter.Payload.GUID";

  it("liefert den Pfad, den der Verweis auf die Datei trägt", () => {
    expect(dateiansichtAus(`${HERKUNFT}${artefaktAnsicht(ID, ARTEFAKT)}`, ID, HERKUNFT)).toBe(
      artefaktAnsicht(ID, ARTEFAKT),
    );
  });

  it("übergeht die gewählte Darstellung", () => {
    expect(
      dateiansichtAus(`${HERKUNFT}${artefaktAnsicht(ID, ARTEFAKT)}?darstellung=hex`, ID, HERKUNFT),
    ).toBe(artefaktAnsicht(ID, ARTEFAKT));
  });

  it("liefert nichts für eine andere Nachricht, eine andere Herkunft oder ohne Beleg", () => {
    expect(
      dateiansichtAus(`${HERKUNFT}${artefaktAnsicht("andere", ARTEFAKT)}`, ID, HERKUNFT),
    ).toBeNull();
    expect(
      dateiansichtAus(`https://fremd.example${artefaktAnsicht(ID, ARTEFAKT)}`, ID, HERKUNFT),
    ).toBeNull();
    expect(dateiansichtAus(`${HERKUNFT}/nachrichten/${ID}`, ID, HERKUNFT)).toBeNull();
    expect(dateiansichtAus(null, ID, HERKUNFT)).toBeNull();
    expect(dateiansichtAus("keine Adresse", ID, HERKUNFT)).toBeNull();
  });

  it("kodiert Kennungen mit Sonderzeichen genau wie der Verweis", () => {
    const ROH = "a b/c?d&e=f#g";

    expect(dateiansichtAus(`${HERKUNFT}${artefaktAnsicht(ROH, ROH)}`, ROH, HERKUNFT)).toBe(
      artefaktAnsicht(ROH, ROH),
    );
  });
});

/**
 * **Der Navigationspunkt bleibt auf dem ganzen Weg aktiv** *(14.10.2026, S4)* —
 * Liste, Nachricht neben der Liste, Nachricht auf eigener Route, Datei. Das
 * Panel steht in der Abfragezeichenkette, und die zählt für den Pfad nicht.
 */
it("hält „Nachrichten“ auf dem ganzen Weg aktiv", () => {
  const ID = "8f3a1c2e-0000-4000-8000-000000000001";
  const nachrichten = NAVIGATION.find((eintrag) => eintrag.pfad === ROUTEN.nachrichten);
  expect(nachrichten).toBeDefined();
  if (nachrichten === undefined) {
    return;
  }

  for (const pfad of [
    ROUTEN.nachrichten,
    ansichtOhneListe(ID, ""),
    artefaktAnsicht(ID, "2-Converter.Payload.GUID"),
  ]) {
    expect(istAktiv(nachrichten, pfad), pfad).toBe(true);
  }
});

/**
 * **Zwei Stellen, ein Name.** `lib` darf nicht aus `features` importieren, der
 * Parametername steht deshalb zweimal: einmal als Konstante für die beiden
 * Funktionen oben, einmal als Schlüssel des `nuqs`-Parsers. Liefen sie
 * auseinander, entfernte der Umschalter einen Parameter, den es nicht gibt — und
 * die Kennung stünde doppelt im Ziel, ohne dass irgendwo etwas fehlschlüge.
 */
it("meint denselben Parameter wie die Nachrichtenliste", () => {
  expect(NACHRICHTEN_PARAMETER).toHaveProperty(NACHRICHT_PARAMETER);
});
