import { useLayoutEffect, useState, type RefObject } from "react";

/**
 * **Was neu erscheint, kommt ins Bild** — E‑114, die vierte Bedingung des
 * Anwendungsrahmens (`docs/frontend-grundlagen.md` §7).
 *
 * Es gibt genau einen Scrollbereich, das `main` des Rahmens. Baum, Liste und
 * Panel teilen ihn sich, und wer weiter unten klickt, bekommt das Ergebnis
 * seiner Handlung oberhalb des Sichtfensters: Der neue Inhalt beginnt oben im
 * Scrollbereich, die Scrollposition steht aber unten (`docs/process-view.md`
 * Punkt 118, M125). Die Kehrseite des einen Scrollbereichs ist deshalb diese
 * eine Regel: **Wird Inhalt neu eingeblendet oder ausgetauscht, kommt dessen
 * Oberkante ins Bild.** Sie steht hier und nirgends sonst.
 *
 * > ### Nachgezogen am 10.09.2026 — E‑115, eine Ebene genauer
 * >
 * > Die Regel oben holt das Neue ins Bild, indem sie **`main` bewegt** — und in
 * > `main` stehen auch Baum und Liste. Wer weit unten öffnet, fand danach seine
 * > Stelle nicht wieder (`docs/process-view.md` §47). Seither gilt sie
 * > **in dem Scrollbereich, in dem das Neue sitzt, und es bewegt sich genau
 * > dieser eine**:
 * >
 * > - Sitzt das Neue in einer **klebenden Spalte** (`lib/klebende-spalte.ts`),
 * >   steht diese von sich aus am oberen Rand des sichtbaren Bereichs. Zu tun
 * >   bleibt allein ihr eigener Scrollbereich: Er beginnt oben
 * >   ({@link useBeginntOben}). **`main` bewegt sich nicht.**
 * > - Sonst gilt sie unverändert wie am 09.09.2026 gebaut ({@link useInSicht}).
 * >
 * > Entschieden wird das **an berechnetem Stil und an Rechtecken**, nicht an
 * > Umbruchpunkten — eine Abfrage der Fensterbreite in JavaScript wäre ein
 * > zweiter Umbruchpunkt neben dem der Ansicht.
 * >
 * > ⚠️ **Und deshalb steht hier kein `scrollIntoView` mehr:** Es bewegt
 * > **jeden** scrollenden Vorfahren. Nach dem Schließen eines Panels in der
 * > Prozessansicht holte es die Zeile in ihrer Liste ins Bild **und** verschob
 * > dabei `main`, also den Baum daneben. Gerechnet wird die Bewegung seither
 * > selbst, an genau einem Kasten ({@link verschiebe}).
 *
 * ## Wie sie eingehängt wird
 *
 * Eine Referenz auf das Element und ein Schlüssel. Wechselt der Schlüssel, wird
 * das Element ins Bild geholt — beim ersten Rendern mit gesetztem Schlüssel
 * genauso wie bei jedem späteren Wechsel. Ohne Schlüssel (`null`) geschieht
 * nichts, ohne Element auch nicht. Drei Ziele je Ansicht: die rechte Spalte der
 * Prozessansicht (Schlüssel `prozess`) und das Panel (Schlüssel `nachricht`)
 * über {@link useBeginntOben}; der Rückweg über {@link useInSicht}, siehe
 * {@link useZuletztGeschlossen}. Dazu die gewählte Baumzeile beim Einstieg über
 * eine Adresse (E‑115, `prozess-baum.tsx`).
 *
 * **`useLayoutEffect`, nicht `useEffect`:** Der Sprung geschieht, bevor der
 * Browser den neuen Zustand einmal falsch gemalt hat. Kein `setState` darin.
 *
 * **Kein `behavior: "smooth"`.** Der Sprung ist die Folge eines Klicks, keine
 * Animation (`docs/visuelles-konzept.md` §7).
 *
 * ## Warum nicht einfach `block: "nearest"` — gemessen, nicht angenommen
 *
 * `nearest` ist die richtige Wahl für ein Ziel, das in den Scrollbereich
 * **passt**: Steht es schon im Bild, bewegt sich nichts; steht es nicht im
 * Bild, ist der Weg der kürzeste. Für ein Ziel, das **höher ist als der
 * Scrollbereich** — die Übertragungsliste mit fünfzig Zeilen, das Panel mit
 * Zeitleiste —, tut `nearest` etwas anderes, und zwar nach Spezifikation
 * (CSSOM View, *scroll an element into view*), in Chrome 152 nachgemessen
 * (`docs/process-view.md` M172, Vorprobe):
 *
 * | Lage des hohen Ziels | `nearest` | Oberkante im Bild? |
 * |---|---|---|
 * | ganz oberhalb des Sichtfensters — **der Fall aus Punkt 118** | richtet die **Unterkante** am unteren Rand aus | **nein** |
 * | Oberkante sichtbar, Unterkante unten hinaus | schiebt die Oberkante an den oberen Rand | ja, aber eine Bewegung ohne Anlass |
 * | deckt das Sichtfenster ab | nichts | ja |
 * | ganz unterhalb | richtet die Oberkante oben aus | ja |
 *
 * Für den Fall, für den es diese Regel gibt, zeigte `nearest` also das **Ende**
 * der Liste. Deshalb entscheidet {@link bringeInSicht} nach der Höhe: Passt das
 * Ziel in den Scrollbereich, gilt die Wirkung von `nearest`. Ist es höher, gilt
 * die Oberkante — steht sie im Bild, geschieht nichts; steht sie nicht im Bild,
 * wird sie an den oberen Rand geholt. Beides zusammen ist genau der Satz aus
 * §7: *Die Oberkante kommt ins Bild, und wenn sie schon steht, bewegt sich
 * nichts.*
 */
export function useInSicht(ref: RefObject<Element | null>, schluessel: string | null): void {
  useLayoutEffect(() => {
    if (schluessel === null) {
      return;
    }
    const ziel = ref.current;
    if (ziel === null) {
      return;
    }
    bringeInSicht(ziel);
  }, [ref, schluessel]);
}

/**
 * **Was neu erscheint, beginnt oben — in seinem eigenen Scrollbereich**
 * (E‑115). Für das Panel und für die rechte Spalte der Prozessansicht.
 *
 * Sitzt das Ziel in einer klebenden Spalte, steht es dort bereits am oberen
 * Rand des sichtbaren Bereichs von `main`; ins Bild zu holen ist nichts, und
 * `main` darf sich nicht bewegen, weil Baum und Liste darin stehen. Zu tun
 * bleibt der **eigene** Scrollbereich: Ein Panel, in dem gerade gelesen wurde,
 * und eine Liste, die zu einem anderen Prozess gehörte, beginnen wieder oben.
 *
 * Steht das Ziel **nicht** in einer klebenden Spalte — unterhalb der
 * Umbruchpunkte, wo alles im einen Scrollbereich sitzt —, gilt E‑114
 * unverändert ({@link bringeInSicht}).
 */
export function useBeginntOben(ref: RefObject<Element | null>, schluessel: string | null): void {
  useLayoutEffect(() => {
    if (schluessel === null) {
      return;
    }
    const ziel = ref.current;
    if (ziel === null) {
      return;
    }
    beginneOben(ziel);
  }, [ref, schluessel]);
}

/**
 * Holt die Oberkante eines Elements in den Scrollbereich, in dem es liegt — und
 * bewegt nichts, wenn sie schon dort steht. Die Begründung steht an
 * {@link useInSicht}.
 *
 * **Bewegt wird genau ein Kasten**, der nächste scrollende Vorfahr. Alles
 * darüber bleibt stehen; in der Prozessansicht ist „darüber" der Baum.
 */
export function bringeInSicht(ziel: Element): void {
  const kasten = scrollkasten(ziel);
  const sicht = sichtfenster(kasten);
  const lage = ziel.getBoundingClientRect();
  if (lage.height <= sicht.height) {
    // Die Wirkung von `nearest`: nur bewegen, wenn nötig, und dann den kürzeren
    // der beiden Wege.
    if (lage.top < sicht.top) {
      verschiebe(kasten, lage.top - sicht.top);
    } else if (lage.bottom > sicht.bottom) {
      verschiebe(kasten, lage.bottom - sicht.bottom);
    }
    return;
  }
  if (lage.top >= sicht.top && lage.top < sicht.bottom) {
    return;
  }
  verschiebe(kasten, lage.top - sicht.top);
}

/**
 * Der eigene Scrollbereich des Ziels beginnt oben — und sonst bewegt sich
 * nichts. Die Begründung steht an {@link useBeginntOben}.
 */
export function beginneOben(ziel: Element): void {
  if (!inKlebenderSpalte(ziel)) {
    bringeInSicht(ziel);
    return;
  }
  setzeAnfang(ziel);
}

/**
 * Ob das Ziel in einer klebenden Spalte sitzt — es selbst eingeschlossen.
 *
 * Gesucht wird bis zum nächsten scrollenden Vorfahren, also bis `main`: Was
 * darüber klebt, gehört nicht mehr zu diesem Bereich. Entschieden wird am
 * **berechneten** Stil, nicht an einer Klasse und nicht an der Fensterbreite —
 * unterhalb der Umbruchpunkte tragen die Hüllen dieselben Klassen und kleben
 * trotzdem nicht.
 */
function inKlebenderSpalte(ziel: Element): boolean {
  for (let el: Element | null = ziel; el !== null; el = el.parentElement) {
    if (getComputedStyle(el).position === "sticky") {
      return true;
    }
    if (istScrollkasten(el) && el !== ziel) {
      return false;
    }
  }
  return false;
}

/**
 * Setzt den Scrollbereich am oder im Ziel auf seinen Anfang.
 *
 * Das Ziel **ist** der Kasten (das Panel, das für sich scrollt) oder es enthält
 * ihn (die rechte Spalte der Prozessansicht enthält die Liste). Der Abstieg
 * hält am ersten Kasten je Ast: Was in einer Liste steckt, gehört zu ihr und
 * wird nicht einzeln zurückgesetzt.
 */
function setzeAnfang(ziel: Element): void {
  if (istScrollkasten(ziel)) {
    ziel.scrollTop = 0;
    return;
  }
  for (const kind of ziel.children) {
    setzeAnfang(kind);
  }
}

/**
 * Der nächste Vorfahr, der ein Scrollkasten **ist** — auch wenn gerade nichts
 * überläuft. Für den Fall, dass ein Stand über einen Zustand hinweg gemerkt
 * wird, in dem der Kasten kurz nichts zu scrollen hat
 * ({@link useStandUeberDenZustand}).
 */
export function scrollbereichVon(ziel: Element | null): Element | null {
  for (let el = ziel?.parentElement ?? null; el !== null; el = el.parentElement) {
    if (istScrollkasten(el)) {
      return el;
    }
  }
  return null;
}

/** Ob das Element ein Scrollkasten ist — unabhängig davon, ob gerade etwas überläuft. */
function istScrollkasten(el: Element): boolean {
  const overflowY = getComputedStyle(el).overflowY;
  return overflowY === "auto" || overflowY === "scroll";
}

/**
 * Der nächste Vorfahr, der senkrecht scrollt — im Anwendungsrahmen ist das
 * `main`, in einer klebenden Spalte die Spalte selbst. `null` heißt: keiner,
 * also das Fenster. Ein Vorfahr, der zwar `overflow-y: auto` trägt, aber nichts
 * zu scrollen hat (der waagerecht scrollende Tabellenkasten stuft `overflow-y`
 * still auf `auto` hoch), zählt nicht: Er ist kein Sichtfenster, und seine Höhe
 * sagt nichts darüber, ob ein Ziel hineinpasst.
 */
function scrollkasten(ziel: Element): Element | null {
  for (let el = ziel.parentElement; el !== null; el = el.parentElement) {
    if (istScrollkasten(el) && el.scrollHeight > el.clientHeight) {
      return el;
    }
  }
  return null;
}

/** Das Sichtfenster eines Kastens — ohne Kasten das des Dokuments. */
function sichtfenster(kasten: Element | null): { top: number; bottom: number; height: number } {
  if (kasten === null) {
    const hoehe = document.documentElement.clientHeight;
    return { top: 0, bottom: hoehe, height: hoehe };
  }
  const r = kasten.getBoundingClientRect();
  return { top: r.top, bottom: r.bottom, height: r.height };
}

/** Verschiebt **einen** Kasten und nichts darüber. */
function verschiebe(kasten: Element | null, um: number): void {
  if (um === 0) {
    return;
  }
  if (kasten === null) {
    window.scrollBy(0, um);
    return;
  }
  kasten.scrollTop += um;
}

/**
 * **Der Rückweg:** Welche Kennung gerade **geschlossen** worden ist — und nur
 * dann.
 *
 * Beim Schließen des Panels kommt die zuvor gewählte Listenzeile über
 * {@link useInSicht} ins Bild; ohne das steht der Nutzer unter `xl` nach dem
 * Schließen am Listenanfang und hat seine Stelle verloren. Der Haken braucht
 * dafür einen Schlüssel, der **genau beim Schließen** wechselt und beim Öffnen
 * nicht — sonst zöge er beim Öffnen die Zeile ins Bild und dem Panel, das im
 * selben Augenblick ins Bild kommt, den Boden weg.
 *
 * Liefert nach dem Wechsel von `"x"` auf `null` den Wert `"x"`, sonst `null`;
 * öffnet sich danach wieder etwas, fällt der Wert auf `null` zurück. Angepasst
 * **während des Renderns** und nicht in einem Effekt — dasselbe Muster wie
 * `letzteSeite` in `features/nachrichten/hooks.ts`: React verwirft den
 * begonnenen Durchlauf und rendert sofort neu, ein Zwischenstand erscheint nie.
 */
export function useZuletztGeschlossen(offen: string | null): string | null {
  const [verlauf, setVerlauf] = useState<{ offen: string | null; geschlossen: string | null }>({
    offen,
    geschlossen: null,
  });
  if (verlauf.offen !== offen) {
    const geschlossen = offen === null ? verlauf.offen : null;
    setVerlauf({ offen, geschlossen });
    return geschlossen;
  }
  return verlauf.geschlossen;
}
