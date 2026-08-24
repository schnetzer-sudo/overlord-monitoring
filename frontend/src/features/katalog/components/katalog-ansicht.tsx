"use client";

import { useMemo } from "react";

import { KeinZugriff } from "@/components/kein-zugriff";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";
import { istKeinZugriff } from "@/lib/http";

import { useKatalogfilter, useKatalogzeilen } from "../hooks";
import { sichtbareZeilen } from "../filter";
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
 */
export function KatalogAnsicht() {
  const texte = useTexte();
  const { filter, setzeNurOffene, setzeNurMitNachrichten } = useKatalogfilter();
  const liste = useKatalogzeilen(filter.nurOffene);

  const zeilen = useMemo(() => liste.data ?? [], [liste.data]);
  const sichtbar = useMemo(() => sichtbareZeilen(zeilen, filter), [zeilen, filter]);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-ueberschrift font-semibold">
        {texte.administration.bereiche.katalog.titel}
      </h1>

      {istKeinZugriff(liste.error) ? (
        <KeinZugriff />
      ) : (
        <>
          <KatalogFilterleiste
            filter={filter}
            aufNurOffene={setzeNurOffene}
            aufNurMitNachrichten={setzeNurMitNachrichten}
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
            <KatalogTabelle zeilen={sichtbar} />
          )}
        </>
      )}
    </div>
  );
}
