"use client";

import { useEffect, useRef, type KeyboardEvent, type PointerEvent, type RefObject } from "react";

import { useTexte } from "@/i18n/provider";
import {
  panelbreiteCookie,
  PANELBREITE_STUFE_REM as STUFE,
  PANELBREITE_TASTE_REM,
  rundePanelbreite,
} from "@/lib/panelbreite";
import { cn } from "@/lib/utils";

/** Breite und Grenzen der Hülle, in rem. */
type Lage = { jetzt: number; min: number; max: number };

function wurzelPx(): number {
  const px = parseFloat(getComputedStyle(document.documentElement).fontSize);
  return Number.isFinite(px) && px > 0 ? px : 16;
}

/** Die Breite, die das CSS der Hülle bei einer bestimmten Wunschbreite gibt, in px. */
function breiteBei(huelle: HTMLElement, wunsch: string): number {
  const vorher = huelle.style.width;
  huelle.style.width = wunsch;
  const px = huelle.getBoundingClientRect().width;
  huelle.style.width = vorher;
  return px;
}

/**
 * **Liest, was das CSS der Hülle gibt** — ihre Breite jetzt, bei der Wunschbreite
 * 0 (das Minimum) und bei einer sehr großen (das Maximum). Das Maximum rechnet
 * das CSS gegen die Zeile (`lib/panelbreite.ts`); hier wird es nur abgelesen,
 * nie nachgerechnet, und weder Fenster noch `main` werden gemessen.
 *
 * **Warum nicht `getComputedStyle(…).maxWidth`:** Für `calc(100% - …)` liefert
 * es den Ausdruck und keine Zahl — Prozente löst erst das Layout auf. Die
 * Wunschbreite wird synchron gesetzt und zurückgenommen; dazwischen wird nicht
 * gezeichnet, und ein `ResizeObserver` sieht keine Änderung.
 */
function lies(huelle: HTMLElement): Lage {
  const px = wurzelPx();
  const jetzt = huelle.getBoundingClientRect().width / px;
  const min = breiteBei(huelle, "0px") / px;
  // Liegt das Maximum unter dem Minimum (um 1.280 px), gilt das Minimum —
  // `min-width` schlägt `max-width`, das liefert die Messung von selbst.
  const max = breiteBei(huelle, "100000px") / px;
  const untere = Math.ceil(min / STUFE - 1e-9) * STUFE;
  return {
    jetzt: rundePanelbreite(jetzt),
    min: untere,
    max: Math.max(untere, Math.floor(max / STUFE + 1e-9) * STUFE),
  };
}

const begrenzt = (wert: number, { min, max }: Lage) => Math.min(max, Math.max(min, wert));

/**
 * **Der Griff zwischen Liste und Panel** *(17.11.2026, E‑1208 bis E‑1210,
 * `docs/nachrichtendetail.md` §10.21)* — ein eigener Baustein, kein
 * Generatorbaustein: `resizable` führt Liste und Panel als Gruppe mit
 * Prozentbreiten und verträgt sich weder mit der klebenden Spalte (E‑115) noch
 * mit dem `display: none` der Liste unter `xl`; `separator` ist eine statische
 * Linie und nicht bedienbar.
 *
 * **Wo er steht:** als Geschwister **vor** der Hülle, in der Fuge `gap-4`
 * zwischen Liste und Panel — `-mx-4 w-4` legt ihn genau in diese Fuge, ohne
 * den Abstand zu ändern. `self-stretch`: so hoch wie die Zeile, also über die
 * ganze sichtbare Höhe der klebenden Spalte, und **außerhalb** ihres
 * Scrollinhalts. Er klebt nicht und scrollt nicht — keine vierte klebende Spalte
 * (`lib/klebende-spalte.ts`). Nur ab `xl`; darunter gibt es keine Breitenwahl.
 *
 * **Ohne React-Zustand.** Beim Ziehen setzt er je Bewegung allein die Variable
 * `--panelbreite` an der Hülle; erst am Ende geht der Wert ans `<html>` und ins
 * Cookie — einmal beim Loslassen, je Tastendruck einmal. Die Werte für
 * Vorleseprogramme (`aria-valuenow`, `-min`, `-max`, in rem) schreibt er selbst
 * ans Element, nach jeder Änderung und beim Fokus.
 */
export function PanelGriff({ huelle }: { huelle: RefObject<HTMLElement | null> }) {
  const texte = useTexte().nachrichten.detail.panelbreite;
  const griff = useRef<HTMLDivElement>(null);
  const ziehen = useRef<{
    x: number;
    px: number;
    lage: Lage;
    zuletzt: number | null;
  } | null>(null);

  function schreibeAria() {
    const element = griff.current;
    const h = huelle.current;
    if (element === null || h === null) return;
    const { jetzt, min, max } = lies(h);
    element.setAttribute("aria-valuenow", String(begrenzt(jetzt, { jetzt, min, max })));
    element.setAttribute("aria-valuemin", String(min));
    element.setAttribute("aria-valuemax", String(max));
  }

  // Kein setState: Die Werte gehen unmittelbar ans Element — nach jedem
  // Rendern, und das ist selten: Der Griff hat keinen Zustand. **Ein passiver
  // Effekt und kein Layout-Effekt:** Der Griff steht im Baum vor der Hülle, und
  // ihre Referenz ist erst gesetzt, wenn alle Layout-Effekte gelaufen sind.
  useEffect(() => {
    schreibeAria();
  });

  /** Übernimmt eine Breite — oder setzt mit `null` zurück. */
  function wende(rem: number | null) {
    const wurzel = document.documentElement.style;
    if (rem === null) {
      wurzel.removeProperty("--panelbreite");
    } else {
      wurzel.setProperty("--panelbreite", `${rundePanelbreite(rem)}rem`);
    }
    huelle.current?.style.removeProperty("--panelbreite");
    document.cookie = panelbreiteCookie(rem);
    schreibeAria();
  }

  function beende() {
    const z = ziehen.current;
    if (z === null) return;
    ziehen.current = null;
    document.documentElement.style.removeProperty("user-select");
    document.documentElement.style.removeProperty("cursor");
    griff.current?.removeAttribute("data-zieht");
    // Ein Klick ohne Bewegung schreibt nichts.
    if (z.zuletzt !== null) wende(z.zuletzt);
  }

  function beimDruecken(ereignis: PointerEvent<HTMLDivElement>) {
    const h = huelle.current;
    if (ereignis.button !== 0 || h === null) return;
    // Keine Textauswahl beim Ziehen — weder beim Druck noch unterwegs.
    ereignis.preventDefault();
    ereignis.currentTarget.setPointerCapture(ereignis.pointerId);
    document.documentElement.style.setProperty("user-select", "none");
    document.documentElement.style.setProperty("cursor", "col-resize");
    ereignis.currentTarget.setAttribute("data-zieht", "");
    ziehen.current = { x: ereignis.clientX, px: wurzelPx(), lage: lies(h), zuletzt: null };
  }

  function beimBewegen(ereignis: PointerEvent<HTMLDivElement>) {
    const z = ziehen.current;
    const h = huelle.current;
    if (z === null || h === null) return;
    // Der Griff steht links vom Panel: nach links ziehen macht es breiter.
    const rem = begrenzt(rundePanelbreite(z.lage.jetzt + (z.x - ereignis.clientX) / z.px), z.lage);
    if (rem === (z.zuletzt ?? z.lage.jetzt)) return;
    z.zuletzt = rem;
    h.style.setProperty("--panelbreite", `${rem}rem`);
  }

  function beiTaste(ereignis: KeyboardEvent<HTMLDivElement>) {
    if (ereignis.altKey || ereignis.ctrlKey || ereignis.metaKey || ereignis.shiftKey) return;
    const richtung = ereignis.key === "ArrowLeft" ? 1 : ereignis.key === "ArrowRight" ? -1 : null;
    const h = huelle.current;
    if (richtung === null || h === null) return;
    ereignis.preventDefault();
    const lage = lies(h);
    wende(begrenzt(rundePanelbreite(lage.jetzt + richtung * PANELBREITE_TASTE_REM), lage));
  }

  return (
    <div
      ref={griff}
      role="separator"
      aria-orientation="vertical"
      aria-label={texte.griff}
      title={texte.hinweis}
      tabIndex={0}
      data-panel-griff=""
      onPointerDown={beimDruecken}
      onPointerMove={beimBewegen}
      onPointerUp={beende}
      onPointerCancel={beende}
      onLostPointerCapture={beende}
      onKeyDown={beiTaste}
      onDoubleClick={() => wende(null)}
      onFocus={schreibeAria}
      className={cn(
        "group/griff relative -mx-4 hidden w-4 shrink-0 cursor-col-resize self-stretch xl:block",
        "touch-none rounded-sm select-none",
        "focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none",
      )}
    >
      {/* Die Haarlinie, mittig in der Fuge. Kräftiger beim Überfahren (nur
          unter `(hover: hover)`), beim Fokus und während des Ziehens — ohne
          Übergang (`visuelles-konzept.md` §7). Farben nur über Tokens. */}
      <span
        aria-hidden="true"
        className={cn(
          "bg-border pointer-events-none absolute inset-y-0 left-1/2 w-px -translate-x-1/2",
          "group-hover/griff:bg-muted-foreground group-hover/griff:w-0.5",
          "group-data-zieht/griff:bg-muted-foreground group-data-zieht/griff:w-0.5",
          "group-focus-visible/griff:bg-ring group-focus-visible/griff:w-0.5",
        )}
      />
    </div>
  );
}
