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
 * ## Wie sie eingehängt wird
 *
 * Eine Referenz auf das Element und ein Schlüssel. Wechselt der Schlüssel, wird
 * das Element ins Bild geholt — beim ersten Rendern mit gesetztem Schlüssel
 * genauso wie bei jedem späteren Wechsel. Ohne Schlüssel (`null`) geschieht
 * nichts, ohne Element auch nicht. Zwei Ziele je Ansicht: die rechte Spalte der
 * Prozessansicht (Schlüssel `prozess`) und das Panel (Schlüssel `nachricht`);
 * dazu der Rückweg, siehe {@link useZuletztGeschlossen}.
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
 * Ziel in den Scrollbereich, gilt `nearest`. Ist es höher, gilt die Oberkante —
 * steht sie im Bild, geschieht nichts; steht sie nicht im Bild, wird sie mit
 * `block: "start"` an den oberen Rand geholt. Beides zusammen ist genau der
 * Satz aus §7: *Die Oberkante kommt ins Bild, und wenn sie schon steht, bewegt
 * sich nichts.*
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
 * Holt die Oberkante eines Elements in den Scrollbereich, in dem es liegt — und
 * bewegt nichts, wenn sie schon dort steht. Die Begründung steht an
 * {@link useInSicht}.
 */
export function bringeInSicht(ziel: Element): void {
  const kasten = scrollkasten(ziel);
  const lage = ziel.getBoundingClientRect();
  if (lage.height <= kasten.height) {
    ziel.scrollIntoView({ block: "nearest" });
    return;
  }
  if (lage.top >= kasten.top && lage.top < kasten.bottom) {
    return;
  }
  ziel.scrollIntoView({ block: "start" });
}

/**
 * Der nächste Vorfahr, der senkrecht scrollt — im Anwendungsrahmen ist das
 * `main`. Gibt es keinen, ist es das Fenster. Ein Vorfahr, der zwar
 * `overflow-y: auto` trägt, aber nichts zu scrollen hat (der waagerecht
 * scrollende Tabellenkasten stuft `overflow-y` still auf `auto` hoch), zählt
 * nicht: Er ist kein Sichtfenster, und seine Höhe sagt nichts darüber, ob ein
 * Ziel hineinpasst.
 */
function scrollkasten(ziel: Element): { top: number; bottom: number; height: number } {
  for (let el = ziel.parentElement; el !== null; el = el.parentElement) {
    const overflowY = getComputedStyle(el).overflowY;
    if ((overflowY === "auto" || overflowY === "scroll") && el.scrollHeight > el.clientHeight) {
      const r = el.getBoundingClientRect();
      return { top: r.top, bottom: r.bottom, height: r.height };
    }
  }
  const hoehe = document.documentElement.clientHeight;
  return { top: 0, bottom: hoehe, height: hoehe };
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
