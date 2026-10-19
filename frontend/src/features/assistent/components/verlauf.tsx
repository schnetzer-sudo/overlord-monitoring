"use client";

import { useRef } from "react";
import { AlertCircle, Hourglass } from "lucide-react";

import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { useSprache, useTexte } from "@/i18n/provider";
import { useInSicht } from "@/lib/in-sicht-bringen";
import type { Mandant } from "@/lib/mandanten";

import { bietetNeuesGespraech, hinweistext, type Beitrag } from "../gespraech";
import type { Gespraechssteuerung } from "../hooks";
import { VorschlagKarte } from "./vorschlag-karte";

/**
 * **Der Verlauf** — der einzige Scrollbereich der Chatbox
 * (`docs/assistent-frontend.md` §4). Er liegt im Portal des Overlays und nicht
 * unter `main`; `relative` trägt er trotzdem, aus dem Grund in
 * `docs/frontend-grundlagen.md` §7, Bedingung 3: Die verborgenen Beschriftungen
 * der Sprecher sind `sr-only` und damit absolut positioniert.
 *
 * **Was neu erscheint, kommt ins Bild** (Bedingung 4): die eigene Nachricht,
 * die Warteanzeige, die Antwort, ein Hinweis — Oberkante zuerst, über
 * `useInSicht`, und bewegt wird nur dieser Kasten. Der Schlüssel ist `stand`,
 * der bei jeder Änderung am Ende des Verlaufs weiterzählt.
 *
 * **Texte des Assistenten sind ein Textknoten** (E‑1074): ein Absatz mit
 * `white-space: pre-wrap` und dem Text als einzigem Kind. Was wie HTML oder
 * Markdown aussieht, steht wörtlich da.
 */
export function Verlauf({
  steuerung,
  typen,
  mandant,
}: {
  steuerung: Gespraechssteuerung;
  typen: readonly string[];
  mandant: Mandant;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const t = texte.assistent;
  const { zustand } = steuerung;

  const ende = useRef<Element | null>(null);
  useInSicht(ende, String(zustand.stand));
  const alsEnde = (element: Element | null) => {
    if (element !== null) {
      ende.current = element;
    }
  };

  const letzter = zustand.beitraege.length - 1;
  const endeIstBeitrag = !zustand.wartet && zustand.hinweis === null;

  return (
    <div data-teil="verlauf" className="relative min-h-0 flex-1 overflow-y-auto px-3 py-3">
      <ol aria-label={t.verlauf} className="flex flex-col gap-3">
        {zustand.beitraege.map((beitrag, nummer) => (
          <li
            key={beitrag.id}
            ref={endeIstBeitrag && nummer === letzter ? alsEnde : undefined}
            data-beitrag={beitrag.art}
            className={beitrag.art === "kunde" ? "flex justify-end" : "flex flex-col gap-2"}
          >
            <BeitragInhalt
              beitrag={beitrag}
              typen={typen}
              mandant={mandant}
              steuerung={steuerung}
            />
          </li>
        ))}
        {zustand.wartet ? (
          <li
            ref={alsEnde}
            data-teil="wartet"
            className="text-muted-foreground flex items-center gap-2"
          >
            {/* Ein Zeichen und ein Satz, kein Drehen (`docs/visuelles-konzept.md` §7). */}
            <Hourglass aria-hidden="true" className="size-4 shrink-0" />
            {t.wartet}
          </li>
        ) : null}
      </ol>

      {zustand.hinweis !== null ? (
        <div ref={alsEnde} className="mt-3">
          <Alert data-teil="hinweis" data-hinweis={zustand.hinweis.art}>
            <AlertCircle aria-hidden="true" />
            <AlertDescription className="flex flex-col items-start gap-2">
              <p>{hinweistext(zustand.hinweis, texte, sprache)}</p>
              {bietetNeuesGespraech(zustand.hinweis) ? (
                <Button
                  type="button"
                  variant="outline"
                  className="min-h-bedienelement"
                  onClick={steuerung.neu}
                >
                  {t.neuesGespraech}
                </Button>
              ) : null}
            </AlertDescription>
          </Alert>
        </div>
      ) : null}
    </div>
  );
}

function BeitragInhalt({
  beitrag,
  typen,
  mandant,
  steuerung,
}: {
  beitrag: Beitrag;
  typen: readonly string[];
  mandant: Mandant;
  steuerung: Gespraechssteuerung;
}) {
  const texte = useTexte();
  const t = texte.assistent;

  switch (beitrag.art) {
    case "begruessung":
      // Ein fester Text aus der Sprachdatei, kein Aufruf des Modells; die Typen
      // kommen aus `GET /api/assistent`.
      return (
        <div className="flex flex-col gap-1.5">
          <span className="sr-only">{t.sprecher.assistent} </span>
          <p>{t.begruessung.einleitung}</p>
          <ul className="list-disc pl-5">
            {typen.map((name, nummer) => (
              <li key={`${nummer}:${name}`}>{name}</li>
            ))}
          </ul>
          <p>{t.begruessung.schluss}</p>
        </div>
      );

    case "kunde":
      return (
        <div className="bg-muted max-w-[85%] min-w-0 rounded-lg px-3 py-2">
          <span className="sr-only">{t.sprecher.kunde} </span>
          <p data-teil="text" className="break-words whitespace-pre-wrap">
            {beitrag.text}
          </p>
        </div>
      );

    case "assistent":
      return (
        <>
          {beitrag.text.trim() === "" ? null : (
            <div className="min-w-0">
              <span className="sr-only">{t.sprecher.assistent} </span>
              <p data-teil="text" className="break-words whitespace-pre-wrap">
                {beitrag.text}
              </p>
            </div>
          )}
          {beitrag.karte === null ? null : (
            <VorschlagKarte
              karteId={beitrag.id}
              karte={beitrag.karte}
              mandant={mandant}
              steuerung={steuerung.karte}
            />
          )}
        </>
      );
  }
}
