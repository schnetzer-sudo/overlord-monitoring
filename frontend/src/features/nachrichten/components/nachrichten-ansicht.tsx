"use client";

import { useState } from "react";

import { Fehler, Laden, Leer } from "@/components/zustand";
import { Button } from "@/components/ui/button";
import { useTexte } from "@/i18n/provider";

import {
  feldFehler,
  sucheTraegt,
  suchfeldFehler,
  zeitfensterFehler,
  zeitfensterHalb,
  type Nachrichtenfilter,
} from "../filter";
import { useNachrichtenSeite, useNachrichtenfilter } from "../hooks";
import { Blaettern } from "./blaettern";
import { Filterleiste } from "./filterleiste";
import { NachrichtenTabelle } from "./nachrichten-tabelle";

/**
 * Die Nachrichtenliste — der Zusammenbau.
 *
 * **Vier Zustände, alle sichtbar:** Laden, Leer, Fehler, Daten. „Leer" ist kein
 * Fehler und sieht auch nicht so aus; wer bei jedem leeren Zeitfenster eine rote
 * Meldung sieht, hört auf, rote Meldungen ernst zu nehmen.
 *
 * **Die Filterleiste bleibt in jedem Zustand stehen.** Sie ist der Weg aus dem
 * leeren Zustand heraus — sie mit den Daten zu verstecken hieße, dem Nutzer genau
 * dann das Werkzeug wegzunehmen, wenn er es braucht.
 */
export function NachrichtenAnsicht() {
  const texte = useTexte();
  const { filter, ...steuerung } = useNachrichtenfilter();
  // Nicht in der URL: Der Schalter betrifft die Arbeitsweise des Betrachters,
  // nicht den gezeigten Ausschnitt.
  const [aktualisierungAn, setAktualisierungAn] = useState(false);
  const liste = useNachrichtenSeite(filter, aktualisierungAn);

  /*
   * Eine Rückmeldung zu einer **Eingabe** ist kein Fehler der ganzen Ansicht.
   * Sie gehört an das Feld, das sie ausgelöst hat — beim Suchbegriff ebenso wie
   * beim freien Zeitfenster, wo zwischen „Von" und „Bis" zwangsläufig ein
   * Moment liegt, in dem nur einer der beiden Zeitpunkte dasteht.
   */
  const amSuchfeld = suchfeldFehler(liste.fehler);
  const amZeitfenster = zeitfensterFehler(liste.fehler);
  const anEinemFeld = feldFehler(liste.fehler);
  const ansichtsfehler = anEinemFeld === undefined ? liste.fehler : undefined;

  /*
   * Bei einer Rückmeldung an einem Feld bleibt die Liste stehen, so wie sie war.
   * Sonst verschwände sie unter dem Nutzer, während er tippt: Die neue Abfrage
   * hat einen eigenen Schlüssel, für den nie Daten ankamen — und der Leerzustand
   * behauptete dann, im Zeitfenster stünde nichts, obwohl die Eingabe noch gar
   * nicht fertig ist.
   *
   * Das halb ausgefüllte freie Fenster gehört **schon vor der Antwort** dazu.
   * Sonst zeigte die Ansicht für die Dauer der Anfrage ein Ladeskelett, obwohl
   * schon feststeht, dass die Antwort nur „es fehlt noch etwas" lauten kann —
   * die Liste verschwände also genau für den Moment zwischen „Von" und „Bis".
   */
  const ruhigeRueckmeldung = anEinemFeld !== undefined || zeitfensterHalb(filter);
  const zeigeSeite = liste.seite ?? (ruhigeRueckmeldung ? liste.letzteSeite : undefined);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-ueberschrift font-semibold">{texte.navigation.eintraege.nachrichten}</h1>

      <Filterleiste
        filter={filter}
        steuerung={steuerung}
        suchfehler={amSuchfeld}
        zeitfensterfehler={amZeitfenster}
        aufLangeSuche={() => steuerung.setzeLangeSuche(true)}
      />

      {ansichtsfehler ? (
        <Fehler fehler={ansichtsfehler} aufWiederholen={liste.aktualisiere} />
      ) : /* Das Skelett nur, wenn es wirklich nichts zu zeigen gibt. Liegt eine
             vorige Seite vor, bleibt sie stehen — siehe `ruhigeRueckmeldung`. */
      liste.laedt && zeigeSeite === undefined ? (
        <Laden zeilen={8} />
      ) : (zeigeSeite?.items.length ?? 0) === 0 ? (
        <LeerMitUrsache filter={filter} auf30Tage={() => steuerung.setzeZeitraum("30d")} />
      ) : (
        <div className="border-border bg-card overflow-x-auto rounded-lg border">
          <NachrichtenTabelle
            zeilen={zeigeSeite?.items ?? []}
            sortierung={filter.sortierung ?? "neueste"}
            aufSortierung={steuerung.setzeSortierung}
          />
          <UngeklaertFusszeile zeilen={zeigeSeite?.items ?? []} />
        </div>
      )}

      <Blaettern
        kannZurueck={liste.kannZurueck}
        kannVor={liste.kannVor}
        aufZurueck={liste.zurueck}
        aufVor={liste.vor}
        aufSeiteEins={liste.aufSeiteEins}
        standVon={liste.standVon}
        laeuft={liste.laeuft}
        aktualisierungAn={aktualisierungAn}
        aufAktualisierung={setAktualisierungAn}
        aufAktualisieren={liste.aktualisiere}
      />
    </div>
  );
}

/**
 * Was „Bedeutung nicht verifiziert" heißt — **einmal unter der Tabelle statt nur
 * im Tooltip.**
 *
 * Der Hinweis stand bisher ausschließlich im `title`-Attribut der Plakette. **Auf
 * einem Touchgerät gibt es keinen Hover**: Dort sieht der Nutzer eine Plakette mit
 * einem Rohwert und einem Fragezeichen und erfährt nie, was das bedeutet — er
 * müsste annehmen, die Anwendung sei kaputt.
 *
 * **Nur, wenn eine solche Zeile auf der Seite steht.** Der Fall ist selten:
 * `CHECKED`, `CKECKED` und `COMMIT_SENT` sind zusammen 0,04 Prozent aller Zeilen.
 * Eine dauerhaft stehende Fußzeile für 0,04 Prozent wäre Rauschen — und Rauschen
 * unter einer Tabelle liest irgendwann niemand mehr, auch nicht, wenn es einmal
 * zählt.
 *
 * **Eine Zeile, nicht eine je Vorkommen.** Sie erklärt eine Kennzeichnung, nicht
 * eine Zeile.
 */
function UngeklaertFusszeile({ zeilen }: { zeilen: { bedeutungNichtVerifiziert: boolean }[] }) {
  const texte = useTexte();

  if (!zeilen.some((zeile) => zeile.bedeutungNichtVerifiziert)) {
    return null;
  }

  return (
    <p className="border-border text-muted-foreground text-beiwerk border-t px-2 py-1.5">
      {texte.nachrichten.ungeklaertFusszeile}
    </p>
  );
}

/**
 * Der Leerzustand **nennt eine Ursache** — und zwar die, die der Nutzer selbst
 * gesetzt hat.
 *
 * Er ist hier besonders wichtig: Im Profil `dev` enthält das
 * 24-Stunden-Standardfenster je nach Mandant sehr wenige oder null Zeilen, und
 * außer `NEXANS` hat kein Mandant Daten nach dem 30.12.2025 (Messung M3). „Nichts
 * gefunden" allein ließe den Nutzer glauben, das Werkzeug sei kaputt.
 *
 * Genannt werden **alle** greifenden Einschränkungen, nicht nur die erste: Wer
 * den Suchbegriff leert und immer noch nichts sieht, weil auch der Statusfilter
 * steht, kommt sonst zweimal an dieselbe Wand.
 */
function LeerMitUrsache({
  filter,
  auf30Tage,
}: {
  filter: Nachrichtenfilter;
  auf30Tage: () => void;
}) {
  const texte = useTexte();

  const ursachen = [
    texte.nachrichten.leer.zeitfenster,
    filter.zwischenschritte ? undefined : texte.nachrichten.leer.zwischenschritte,
    sucheTraegt(filter.suche) ? texte.nachrichten.leer.suche : undefined,
    (filter.status?.length ?? 0) > 0 ? texte.nachrichten.leer.status : undefined,
    (filter.prozess?.length ?? 0) > 0 ? texte.nachrichten.leer.prozess : undefined,
  ].filter((text): text is string => text !== undefined);

  return (
    <div className="flex flex-col items-center gap-3">
      <Leer titel={texte.nachrichten.leer.titel} hinweis={ursachen.join(" ")} />
      {filter.zeitraum === "30d" ? null : (
        <Button type="button" variant="outline" className="min-h-beruehrung" onClick={auf30Tage}>
          {texte.nachrichten.leer.fensterErweitern}
        </Button>
      )}
    </div>
  );
}
