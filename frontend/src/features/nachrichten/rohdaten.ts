import { einsetzen, type Texte } from "@/i18n";

import type {
  Artefakt,
  Artefaktanzeige,
  Artefaktart,
  Artefaktliste,
  Artefaktzustand,
  Schritt,
} from "./api";
import { METADATEN_POSITION } from "./detail";

/**
 * Rohdaten und Protokolle — **die Entscheidungen, als reine Funktionen.**
 *
 * Dieselbe Aufteilung wie bei `detail.ts`: Was entschieden wird, steht hier und
 * ist geprüft (`tests/rohdaten.test.ts`); was dargestellt wird, steht in den
 * Komponenten. Vier Dinge sind es, und alle vier sind Regeln und kein Markup:
 *
 * 1. **Die Beschriftung** eines Artefakts — der Punkt, an dem sich dieses
 *    Feature entscheidet, siehe unten.
 * 2. **Welches Ziel an welcher Zeile der Zeitleiste hängt** — die Einteilung
 *    nach Schritt und der Rest, für den es keine Zeile gibt (18.08.2026).
 * 3. **Ob ein Download angeboten werden darf** — der Gleichlauf aus
 *    `docs/rohdaten.md` §3, Entscheidung 9.
 * 4. **Welche Vermerke die Anzeige trägt** — Ausschnitt, Kappung, mehrere
 *    Archiveinträge.
 */

/* ─────────────────────────────────────────────────────────────────────────────
   1. Die Beschriftung
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Ein Artefakt samt der Auskunft, ob es der **Eingang** ist.
 *
 * Der Eingang (`Message.Payload.GUID`) steht in der Antwort in einem eigenen
 * Feld und nicht in einer der beiden Listen. Er ist damit keine Ableitung dieser
 * Datei, sondern eine Einordnung des Backends — das ist der Unterschied zwischen
 * „erkannt" und „geraten" (Regel Q4).
 */
export type Artefakteintrag = {
  artefakt: Artefakt;
  /**
   * Wahr für die eingegangene Datei. Sie hängt auf Schritt `0`, dem Ort der
   * Metadaten, und ist **kein Ablaufschritt** (M57, M17 3) — sie steht deshalb
   * in der Eingangszeile über der Zeitleiste und in keiner ihrer Zeilen.
   */
  istEingang: boolean;
};

/**
 * Alle Artefakte einer Nachricht in **einer** Liste, in Anzeigereihenfolge:
 * Eingang, Nutzdaten, Protokolle.
 *
 * Gebraucht wird sie zum Nachschlagen einer Kennung — die Ansicht auf ihrer
 * eigenen Route bekommt aus der URL nur die `artefaktId` und muss daraus dieselbe
 * Beschriftung gewinnen, die eine Zeile weiter oben in der Liste steht.
 *
 * **Es wird nicht umsortiert.** Die Reihenfolge innerhalb der beiden Listen ist
 * die des Backends (`ORDER BY MessageActionID, MessagePropertyName`, gemessen in
 * `docs/rohdaten-backend.md` §9) und steht damit an genau einer Stelle. Eine
 * zweite Sortierung hier wäre die Drift, gegen die diese Regel gerichtet ist.
 */
export function artefakteintraege(liste: Artefaktliste): Artefakteintrag[] {
  const eintraege: Artefakteintrag[] = [];
  if (liste.eingang !== null) {
    eintraege.push({ artefakt: liste.eingang, istEingang: true });
  }
  for (const artefakt of liste.nutzdaten) {
    eintraege.push({ artefakt, istEingang: false });
  }
  for (const artefakt of liste.protokolle) {
    eintraege.push({ artefakt, istEingang: false });
  }
  return eintraege;
}

/**
 * Der Eintrag zu einer Kennung, oder `null`.
 *
 * **`null` ist kein Fehlerzustand dieser Funktion**, sondern der Normalfall,
 * solange die Liste noch lädt. Die Ansicht kommt auch ohne sie aus — dann fehlt
 * ihr die Beschriftung, nicht der Inhalt.
 */
export function findeArtefakt(
  liste: Artefaktliste | undefined,
  artefaktId: string,
): Artefakteintrag | null {
  if (liste === undefined) {
    return null;
  }
  return (
    artefakteintraege(liste).find((eintrag) => eintrag.artefakt.artefaktId === artefaktId) ?? null
  );
}

/**
 * **Die Beschriftung eines Artefakts — der Punkt, an dem dieses Feature steht
 * oder fällt.**
 *
 * ## Warum sie hier entsteht und nicht im Backend
 *
 * Die Artefaktliste liefert `schritt` (die `MessageActionID`) und `familie`
 * (`Converter`, `FTPSender`, …), aber **keinen lesbaren Namen** — Abweichung 1
 * aus `docs/rohdaten-backend.md` §10, begründet mit der Paketgrenze: Die
 * dreistufige Namensauflösung liegt im Paket `message`, und Fachpakete kennen
 * einander nicht.
 *
 * Die Zuordnung ist trotzdem möglich und kostet **keine zweite Abfrage**:
 * `GET /api/nachrichten/{messageId}` liefert die Schrittfolge mit `position` —
 * und `position` *ist* die `MessageActionID`. Dieselbe Verbindung, aus der seit
 * dem 17.08.2026 die Gruppenköpfe des Eigenschaftenblocks entstehen
 * (`detail.ts` `gruppiereEigenschaften`).
 *
 * ## Die Regel, in vier Zeilen
 *
 * | Lage | Beschriftung |
 * |---|---|
 * | Der Eingang (`Message.Payload.GUID`) | *Eingegangene Datei* |
 * | Sonst auf Schritt `0` — die Lesedienste (M57) | die **Familie allein**, `SAPReader` |
 * | Der Schritt löst zu einem `SOSActionName` auf | dieser Name |
 * | Er löst nicht auf — **55,98 % der Artefakte** (M57) | `Schritt <n> · <Familie>` |
 *
 * ### Warum Schritt `0` seit dem 18.08.2026 eine eigene Zeile hat
 *
 * Auf Schritt `0` liegt **nicht nur** der Eingang. Gemessen sitzen dort die
 * Artefakte der Lesedienste, je Nachricht ein Paar aus Datei und Protokoll —
 * `SAPReader`, `FileReader`, `FTPReader`, `AS2Reader`, `MailReader`,
 * `OFTPReader`, `OFTP2Reader`, `HTTPReader`, `SSHReader` (M57, Fenster A).
 *
 * Für sie griff bis dahin der allgemeine Rückfall und schrieb
 * `Schritt 0 · SAPReader`. **Das ist technisch richtig und fachlich falsch** —
 * dieselbe Begründung, aus der der Eingang nicht „Schritt 0 · Message" heißt:
 * Schritt `0` ist der Ort der Metadaten und **kein Ablaufschritt** (M57, M17 3);
 * er kommt in `schritte[]` gar nicht vor und steht deshalb in keiner Zeile der
 * Zeitleiste. Eine Nummer zu nennen, die der Nutzer nirgends wiederfindet, ist
 * keine Auskunft. Übrig bleibt, was ohne sie über das Artefakt bekannt ist: die
 * Familie.
 *
 * **Nichts wird geraten** (Regel Q4). Keine hübsche Umschrift der Familie, keine
 * erfundene Bezeichnung, kein „vermutlich Umwandlung". Die Familie ist ein
 * technischer Wert und wird als solcher gezeigt — genauso, wie die
 * Eigenschaftsnamen im Nachbarblock Rohwerte bleiben.
 *
 * Die Dienst*namen* aus `Service.ServiceName` bleiben unsichtbar (M15 3); sie
 * kommen ohnehin nicht über die Schnittstelle.
 *
 * @param schritte die Schrittfolge aus dem Detail, **allein zum Beschriften**.
 *   Fehlt sie — etwa solange das Detail lädt oder weil es nicht geladen werden
 *   konnte —, greift für jedes Artefakt der Rückfall. Das ist der richtige
 *   Zustand und kein halber: Die Nummer und die Familie sind das, was ohne die
 *   Schrittfolge über das Artefakt bekannt ist.
 */
export function artefaktBeschriftung(
  eintrag: Artefakteintrag,
  schritte: readonly Schritt[],
  texte: Texte,
): string {
  const bausteine = texte.nachrichten.detail.dateien;
  if (eintrag.istEingang) {
    return bausteine.eingangTitel;
  }
  if (eintrag.artefakt.schritt === METADATEN_POSITION) {
    return eintrag.artefakt.familie;
  }
  const schritt = schritte.find((kandidat) => kandidat.position === eintrag.artefakt.schritt);
  if (schritt !== undefined && schritt.name !== "") {
    return schritt.name;
  }
  return einsetzen(bausteine.schrittFamilie, {
    nummer: eintrag.artefakt.schritt,
    familie: eintrag.artefakt.familie,
  });
}

/* ─────────────────────────────────────────────────────────────────────────────
   1a. Die Ziele an der Zeitleiste (18.08.2026)
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Ein Ziel: ein Artefakt, so wie es als Zeichen an einer Zeile hängt.
 *
 * **Es trägt keinen sichtbaren Text.** Was hier `name` heißt, ist der Name für
 * Vorleseprogramme; sichtbar ist allein das Zeichen. Der Grund steht in
 * `docs/rohdaten-frontend.md` §3: Eine Nachricht trägt bis zu fünfzehn
 * Artefakte (M55), und fünfzehn Textzeilen neben den Schrittnamen wären genau
 * die Wiederholung, die der eigene Block war.
 */
export type Artefaktziel = {
  artefaktId: string;
  art: Artefaktart;
  /** Der Name für Vorleseprogramme — *Protokoll · Datei gelesen*. */
  name: string;
  /** Der Rohname im `title`, dazu die Ausschnitt-Ankündigung, wo sie greift. */
  titel: string;
};

/**
 * Ein Artefakt als Ziel.
 *
 * **Der Name kommt aus derselben Funktion wie die Überschrift der Ansicht**
 * ({@link artefaktBeschriftung}) — die Lektion vom 17.08.2026: *Zwei Stellen,
 * die denselben Schritt verschieden benennen, sind der Fehler.* Davor steht die
 * Art, weil an einer Zeile zwei Ziele hängen und die beiden sich sonst nur über
 * ihr Zeichen unterschieden; **beim Eingang steht sie nicht** — *Nutzdaten ·
 * Eingegangene Datei* sagte zweimal dasselbe.
 *
 * **Der Ausschnitt wird angekündigt, bevor jemand klickt.** Das Backend liefert
 * `beschnittMoeglich` je Zeile genau dafür. Er steht im Namen *und* im `title`:
 * Ein `title` erscheint auf einem Berührungsgerät nicht, und ein
 * Vorleseprogramm liest ihn nicht verlässlich.
 */
export function artefaktziel(
  eintrag: Artefakteintrag,
  schritte: readonly Schritt[],
  texte: Texte,
): Artefaktziel {
  const bausteine = texte.nachrichten.detail.dateien;
  const beschriftung = artefaktBeschriftung(eintrag, schritte, texte);
  const ohneMarke = eintrag.istEingang
    ? beschriftung
    : einsetzen(bausteine.ziel, {
        art: bausteine.art[eintrag.artefakt.art],
        name: beschriftung,
      });

  return {
    artefaktId: eintrag.artefakt.artefaktId,
    art: eintrag.artefakt.art,
    name: eintrag.artefakt.beschnittMoeglich
      ? einsetzen(bausteine.zielAusschnitt, { ziel: ohneMarke, marke: bausteine.ausschnittMarke })
      : ohneMarke,
    titel: eintrag.artefakt.beschnittMoeglich
      ? `${eintrag.artefakt.name}\n${bausteine.ausschnittAnkuendigung}`
      : eintrag.artefakt.name,
  };
}

/**
 * Die Ziele einer Nachricht, **nach Schritt in Eimer geteilt**.
 *
 * Der Schlüssel ist die `MessageActionID` — dieselbe Zahl, die an einer Zeile
 * der Zeitleiste `position` heißt und nach der der Eigenschaftenblock gruppiert
 * (`detail.ts` `gruppiereEigenschaften`). Seit dem 18.08.2026 gibt es nur noch
 * **eine** Ordnung, und das ist die der Zeitleiste; diese Funktion ordnet
 * deshalb selbst nichts, sie teilt nur ein.
 *
 * **Innerhalb eines Eimers bleibt die Reihenfolge die des Backends** — Eingang,
 * Nutzdaten, Protokolle, jeweils `ORDER BY MessageActionID,
 * MessagePropertyName` (`docs/rohdaten-backend.md` §9). Eine zweite Sortierung
 * hier wäre die Drift, gegen die diese Regel gerichtet ist.
 *
 * **Eimer `0` ist der Eingang** und gehört in keine Zeile der Zeitleiste.
 */
export function zieleJeSchritt(
  liste: Artefaktliste | undefined,
  schritte: readonly Schritt[],
  texte: Texte,
): Map<number, Artefaktziel[]> {
  const eimer = new Map<number, Artefaktziel[]>();
  if (liste === undefined) {
    return eimer;
  }
  for (const eintrag of artefakteintraege(liste)) {
    const ziel = artefaktziel(eintrag, schritte, texte);
    const vorhandene = eimer.get(eintrag.artefakt.schritt);
    if (vorhandene === undefined) {
      eimer.set(eintrag.artefakt.schritt, [ziel]);
    } else {
      vorhandene.push(ziel);
    }
  }
  return eimer;
}

/**
 * **Die Ziele, für die es weder eine Zeile der Zeitleiste noch den Eingang
 * gibt** — aufsteigend nach Schritt.
 *
 * ## Sie ist normalerweise leer, und trotzdem gebaut
 *
 * Gemessen kommt der Fall nicht vor: `ohne_schrittzeile` ist in allen 63
 * Kombinationen aus Fenster A **0** und in Fenster B über 1.516.642 Zeilen
 * ebenfalls **0** (M57, Befund 1) — zu jedem Artefakt gibt es die zugehörige
 * `MessageAction`-Zeile, und alle außer Schritt `0` stehen in `schritte[]`.
 *
 * **Gebaut wird sie, weil die Messung keine Zusage des Schemas ist.** Seit dem
 * 18.08.2026 hängen die Artefakte an der Zeitleiste; ohne diesen Rest fiele ein
 * Artefakt, dessen Schritt dort keine Zeile hat, **lautlos aus der Oberfläche**.
 * Es gäbe keine Meldung, keinen leeren Kasten und keinen Hinweis — nur eine
 * Datei, die niemand mehr findet. Genau dafür trägt die Beschriftungsregel
 * ihren Rückfall `Schritt <n> · <Familie>` weiter.
 *
 * **Solange sie leer ist, steht in der Oberfläche nichts davon.**
 */
export function zieleOhneZeile(
  eimer: Map<number, Artefaktziel[]>,
  schritte: readonly Schritt[],
): Artefaktziel[] {
  const bekannt = new Set<number>([METADATEN_POSITION]);
  for (const schritt of schritte) {
    bekannt.add(schritt.position);
  }
  return [...eimer.keys()]
    .filter((position) => !bekannt.has(position))
    .sort((eine, andere) => eine - andere)
    .flatMap((position) => eimer.get(position) ?? []);
}

/* ─────────────────────────────────────────────────────────────────────────────
   2. Der Download
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Der Pfad des Download-Endpunkts.
 *
 * **Über das Backend, niemals ein Link auf den Filestore** (`docs/rohdaten.md`
 * §9). Er kennt unsere Nutzer nicht, und ein durchgereichter Verweis wäre ein
 * offener Proxy vor einer Produktionsablage.
 *
 * Der Aufruf geht an `/api/…` auf der Next.js-Adresse; der Rewrite reicht ihn
 * weiter (`docs/frontend-grundlagen.md` §1). Es ist ein **gewöhnlicher Verweis**
 * — keine Blob-URL, kein `window.open`, kein selbstgebauter Datenstrom: Der
 * Browser sieht `Content-Disposition: attachment` und legt die Datei ab, ohne
 * die Seite zu verlassen.
 */
export function downloadPfad(messageId: string, artefaktId: string): string {
  return `/api/nachrichten/${encodeURIComponent(messageId)}/dateien/${encodeURIComponent(
    artefaktId,
  )}/download`;
}

/**
 * **Ob überhaupt ein Download angeboten werden darf.**
 *
 * Das ist die Umsetzung von Entscheidung 9 in der Oberfläche: *Der Download
 * liefert, was die Anzeige liefert.* Im Altsystem steht der Download-Knopf über
 * einem leeren Feld und liefert die vollständige Datei — genau diese Lücke
 * entsteht hier nicht.
 *
 * | Zustand | Download | warum |
 * |---|---|---|
 * | `ANZEIGBAR` | **ja** | es gibt Bytes |
 * | `BINAERDATEI`, nicht beschnitten | **ja** | Nutzdatei oder `ADMIN` — beide bekommen die Datei ohnehin vollständig; die Anzeige kann Bytes nur nicht als Text darstellen |
 * | `BINAERDATEI`, beschnitten | nein | eine binäre Datei hat keinen Innenbereich zwischen Marken; der Endpunkt antwortet `409` |
 * | `KEIN_ANZEIGBARER_PROTOKOLLTEIL` | nein | `409` |
 * | `DATEI_NICHT_VORHANDEN` | nein | `404` |
 * | `ABLAGE_NICHT_ERREICHBAR` | nein | `502` |
 *
 * **`beschnitten` ist hier das verlässliche Kennzeichen und nicht die Rolle.**
 * Die Oberfläche fragt nicht, wer der Nutzer ist — das Backend hat die Frage
 * schon beantwortet, als es den Beschnitt anwandte. Eine zweite Ableitung aus
 * der Rolle wäre eine zweite Wahrheit, und die falsche davon stünde im Browser.
 *
 * > **Ein Knopf, der nichts liefern kann, wird nicht angeboten** — nicht
 * > ausgegraut, nicht mit einer Erklärung dahinter. Ein Bedienelement, das
 * > verspricht, was die Anzeige gerade verneint hat, ist genau der Widerspruch,
 * > gegen den Entscheidung 9 gerichtet ist.
 */
export function downloadMoeglich(anzeige: {
  zustand: Artefaktzustand;
  beschnitten: boolean;
}): boolean {
  if (anzeige.zustand === "ANZEIGBAR") {
    return true;
  }
  return anzeige.zustand === "BINAERDATEI" && !anzeige.beschnitten;
}

/* ─────────────────────────────────────────────────────────────────────────────
   3. Die Vermerke
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Ein sichtbarer Vermerk über der Anzeige. **Jeder sagt, dass der Nutzer nicht
 * die ganze Datei sieht** — und jeder sagt es aus einem anderen Grund.
 *
 * | Vermerk | Grund | betrifft den Download |
 * |---|---|---|
 * | `AUSSCHNITT` | der Markenbeschnitt hat gegriffen (`MANDANT`, Protokoll) | **ja** — er liefert denselben Ausschnitt (Gleichlauf) |
 * | `GEKAPPT` | die Anzeige ist an der Längengrenze abgeschnitten | **nein** — die Kappung schützt den Browser, nicht die Vertraulichkeit |
 * | `MEHRERE_EINTRAEGE` | das Archiv trug mehr als einen Eintrag; gezeigt wird der erste | — |
 */
export type Anzeigevermerk = "AUSSCHNITT" | "GEKAPPT" | "MEHRERE_EINTRAEGE";

/**
 * Die Vermerke einer Anzeige, in Anzeigereihenfolge.
 *
 * **Der Ausschnitt steht vorn**, weil er als einziger etwas über den *Inhalt*
 * aussagt: Was fehlt, fehlt für diesen Nutzer und fehlt auch im Download. Die
 * Kappung ist eine Eigenschaft der Anzeige, und der Archivvermerk eine
 * Beobachtung über die Quelle.
 *
 * **`MEHRERE_EINTRAEGE` ist in 693 geholten Dateien nie vorgekommen** — er wird
 * trotzdem gezeigt. Das Altsystem verwirft den Rest stillschweigend
 * (`docs/rohdaten.md` §4); hier steht die Zahl in der Antwort, und die Antwort
 * steht auf dem Bildschirm.
 *
 * Leere Liste heißt: Der Nutzer sieht die Datei, wie sie ist.
 */
export function anzeigevermerke(anzeige: Artefaktanzeige): Anzeigevermerk[] {
  const vermerke: Anzeigevermerk[] = [];
  if (anzeige.beschnitten) {
    vermerke.push("AUSSCHNITT");
  }
  if (anzeige.gekuerzt) {
    vermerke.push("GEKAPPT");
  }
  if (anzeige.zipEintraege > 1) {
    vermerke.push("MEHRERE_EINTRAEGE");
  }
  return vermerke;
}

/**
 * Ob dieser Zustand einen erneuten Versuch lohnt.
 *
 * **Genau einer tut es**, und das ist der ganze Unterschied zwischen den beiden
 * Zuständen, die `docs/rohdaten.md` §8 ausdrücklich getrennt hält: *Datei nicht
 * vorhanden* heißt, die Ablage hat geantwortet und nichts geliefert — daran
 * ändert ein zweiter Versuch nichts. *Ablage nicht erreichbar* ist ein
 * Betriebszustand; er kann in einer Minute vorbei sein.
 *
 * Die Unterscheidung wird deshalb **nicht über Farbe** getragen, sondern über
 * das Angebot: Nur der eine Zustand bekommt eine Schaltfläche. Rot hat in diesem
 * Farbsystem genau eine Bedeutung — `MessageStatus` ist fehlgeschlagen
 * (`docs/visuelles-konzept.md` §3) —, und keiner dieser vier Zustände ist das.
 */
export function erneutVersuchenSinnvoll(zustand: Artefaktzustand): boolean {
  return zustand === "ABLAGE_NICHT_ERREICHBAR";
}
