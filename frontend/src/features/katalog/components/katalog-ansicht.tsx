"use client";

import { useCallback, useMemo, useState } from "react";

import { KeinZugriff } from "@/components/kein-zugriff";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";
import { istKeinZugriff } from "@/lib/http";

import { useKatalogfilter, useKatalogzeilen, usePartner } from "../hooks";
import { sichtbareZeilen } from "../filter";
import { LaufKnopf } from "./lauf-knopf";
import { KatalogFilterleiste } from "./katalog-filterleiste";
import { KatalogTabelle } from "./katalog-tabelle";

/**
 * Die Katalogpflege: **alle Prozesse des aktiven Mandanten**, auch die ohne
 * Nachrichten (E5) und auch die ohne Katalogzeile.
 *
 * ## Die Reihenfolge der Zustände, und warum sie hier eine andere ist
 *
 * `docs/frontend-grundlagen.md` §5 nennt vier Zustände und keine Reihenfolge —
 * die Nachrichtenliste prüft den Fehler zuerst, der Eigenschaftenblock das
 * Laden. Hier steht **„kein Zugriff" vor allem anderen**, und zwar außerhalb
 * der Kette:
 *
 * - Er ist **kein Fehler**. Nichts ist kaputt; der Nutzer steht vor einer
 *   Grenze, die für ihn gilt. `403` steht beim ersten Aufruf fest, ein zweiter
 *   Versuch findet nicht statt (`lib/query-client.ts`), und ein Knopf „Erneut
 *   versuchen" verspräche das Gegenteil.
 * - Er nimmt **die Filterleiste mit weg**. Wer die Liste nicht sehen darf, hat
 *   nichts zu filtern; zwei bedienbare Kästchen über einer Absage wären eine
 *   Einladung ins Leere.
 *
 * Danach kommt die übliche Kette: Laden, Fehler, Leer, Daten.
 *
 * ## „Leer" hat hier drei Ursachen und sagt jede einzeln
 *
 * | Lage | Was dasteht |
 * |---|---|
 * | Antwort leer, `nurOffene` gesetzt | „Jede Zeile ist gepflegt" — der Erfolgsfall, und er sieht auch so aus |
 * | Antwort leer, ohne Filter | Der Mandant hat keine Prozesse |
 * | Antwort voll, Browserfilter leert sie | Der Filter, nicht der Bestand |
 *
 * Die drei zu verschmelzen hieße, dem Nutzer im Erfolgsfall dasselbe zu sagen
 * wie bei einem leeren Mandanten — und ihn im dritten Fall den Bestand
 * verdächtigen zu lassen, obwohl sein eigener Haken die Ursache ist.
 *
 * ## Die offene Zeile steht im Komponentenzustand und nicht in der URL
 *
 * Der Zweischritt aus §8: Die URL *könnte* die Kennung ausdrücken — aber der
 * Zustand ist kein Ausschnitt, sondern eine **begonnene Eingabe**, und die
 * Hälfte, auf die es ankommt (der getippte Text), lässt sich ohnehin nicht
 * teilen. Ein geteilter Link zeigte dem Empfänger ein leeres Formular über einer
 * Zeile und behauptete damit etwas, das der Absender nie gesehen hat. Die
 * ableitende Regel steht als reine Funktion daneben ({@link darfOeffnen}).
 *
 * ## Die Partnervorschläge werden geholt, sobald die Seite steht
 *
 * Nicht erst beim Öffnen einer Zeile. Der Endpunkt kostet bei `NEXANS`
 * gemessene 4,1 ms (M80), gilt fünfzehn Minuten und ist genau das, wofür diese
 * Seite geöffnet wurde. Eine Auswahl, die beim ersten Aufklappen noch lädt, ist
 * beim ersten Aufklappen keine.
 */
export function KatalogAnsicht() {
  const texte = useTexte();
  const { filter, setzeNurOffene, setzeNurMitNachrichten } = useKatalogfilter();
  const liste = useKatalogzeilen(filter.nurOffene);
  const partner = usePartner();
  const [bearbeitet, setBearbeitet] = useState<string | null>(null);

  const zeilen = useMemo(() => liste.data ?? [], [liste.data]);
  const sichtbar = useMemo(() => sichtbareZeilen(zeilen, filter), [zeilen, filter]);
  const schliessen = useCallback(() => setBearbeitet(null), []);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-ueberschrift font-semibold">
        {texte.administration.bereiche.katalog.titel}
      </h1>

      {istKeinZugriff(liste.error) ? (
        <KeinZugriff />
      ) : (
        <>
          <LaufKnopf gesperrt={bearbeitet !== null} />

          <KatalogFilterleiste
            filter={filter}
            aufNurOffene={setzeNurOffene}
            aufNurMitNachrichten={setzeNurMitNachrichten}
            gesperrt={bearbeitet !== null}
          />

          {liste.isPending ? (
            <Laden zeilen={8} />
          ) : liste.error ? (
            <Fehler fehler={liste.error} aufWiederholen={() => void liste.refetch()} />
          ) : zeilen.length === 0 ? (
            <Leer
              titel={texte.katalog.leer.titel}
              hinweis={
                filter.nurOffene
                  ? texte.katalog.leer.allesGepflegt
                  : texte.katalog.leer.ohneProzesse
              }
            />
          ) : sichtbar.length === 0 ? (
            <Leer titel={texte.katalog.leer.titel} hinweis={texte.katalog.leer.filterLeer} />
          ) : (
            <KatalogTabelle
              zeilen={sichtbar}
              offeneZeile={bearbeitet}
              aufOeffnen={setBearbeitet}
              aufSchliessen={schliessen}
              vorschlaege={partner.data ?? []}
            />
          )}
        </>
      )}
    </div>
  );
}
