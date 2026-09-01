"use client";

import { Card } from "@/components/ui/card";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";

import { hervorgehobenerZeitraum } from "../filter";
import { useDashboard, useDashboardzustand } from "../hooks";
import { Kacheln } from "./kacheln";
import { VerteilungBlock } from "./verteilung-block";
import { StandZeile } from "./stand-zeile";
import { VerlaufDiagramm } from "./verlauf-diagramm";
import { ZeitraumUmschalter } from "./zeitraum-umschalter";

/**
 * Die Landingpage (`docs/dashboard-frontend.md`).
 *
 * **Sie liegt auf `/` und nicht auf `/dashboard`** (Entscheidung E‑r). Die
 * bewusst leere Startseite ist gefüllt worden, nicht ersetzt; der
 * Navigationseintrag war seit Schritt 3 dafür vorgesehen (`lib/navigation.ts`)
 * und heißt jetzt „Übersicht".
 *
 * ## Ein Aufruf, sieben Blöcke
 *
 * Die Seite holt **eine** Antwort und baut alles daraus. Kein Block lädt nach,
 * auch die Verteilung beim Umschalten der Sicht nicht — das ist ein neuer Aufruf
 * derselben Adresse mit anderem Parameter (`hooks.ts`).
 *
 * ## Die Reihenfolge der Blöcke folgt dem Leitsatz
 *
 * Zuerst die drei Zahlen, dann das Bild, dann die einzelnen Zeilen, zuletzt der
 * Hintergrund. *Das Werkzeug wird geöffnet, wenn etwas nicht stimmt* — wer es
 * öffnet, will zuerst wissen **ob**, dann **seit wann**, dann **welche**. Die
 * Verteilung beantwortet keine dieser drei Fragen; sie steht deshalb unten und
 * am breiten Fenster neben den Zeilen statt über ihnen.
 *
 * ## Die Zustände
 *
 * Laden, Fehler, Leer, Daten — in dieser Reihenfolge, wie in der
 * Nachrichtenliste. **Der Umschalter steht außerhalb der Kette**: Er bleibt im
 * Leerzustand bedienbar (Entscheidung E‑p), denn er ist der einzige Weg
 * herauszufinden, ob es am Zeitraum liegt. Im Ladezustand ist er gesperrt — eine
 * zweite Anfrage, während die erste läuft, beantwortete nur die zweite Frage und
 * ließe die erste Antwort trotzdem ankommen.
 *
 * **„Leer" sieht nicht aus wie ein Fehler**, und es ist einer von zwei Fällen
 * zugleich: „im Zeitraum nichts" und „dieser Mandant hat keine Daten" werden
 * **nicht unterschieden** (Entscheidung E‑p, `docs/dashboard.md` §6). Der Satz
 * ist in beiden wahr. Bekannte Folge: Ein stiller Sonntag und ein Mandant ohne
 * jeden Verkehr sehen gleich aus.
 *
 * **Im Leerzustand steht sonst nichts** — keine Kacheln mit Nullen, kein leeres
 * Diagramm, und ausdrücklich auch nicht die Zeile „nicht zugeordnet" aus der
 * Verteilung. Sie sagt etwas über den *Katalog*; über einen Mandanten ohne
 * Nachrichten sagt sie nichts.
 */
export function DashboardAnsicht() {
  const texte = useTexte();
  const { zustand, setzeZeitraum, setzeSicht } = useDashboardzustand();
  const antwort = useDashboard(zustand);

  const zeitraum = hervorgehobenerZeitraum(zustand, antwort.data?.zeitraum);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <h1 className="text-ueberschrift font-semibold">{texte.dashboard.titel}</h1>
        <ZeitraumUmschalter
          gewaehlt={zeitraum}
          aufAuswahl={setzeZeitraum}
          gesperrt={antwort.isPending}
        />
      </div>

      {antwort.isPending ? (
        <Laden zeilen={6} />
      ) : antwort.isError ? (
        <Fehler fehler={antwort.error} aufWiederholen={() => void antwort.refetch()} />
      ) : antwort.data.leer ? (
        <>
          <Leer titel={texte.dashboard.leerTitel} hinweis={texte.dashboard.leerHinweis} />
          <StandZeile stand={antwort.data.stand} />
        </>
      ) : (
        <>
          <Kacheln kacheln={antwort.data.kacheln} fenster={antwort.data.fenster} />

          <Card size="sm" className="px-4">
            <VerlaufDiagramm punkte={antwort.data.verlauf} zeitraum={antwort.data.zeitraum} />
          </Card>

          <Card size="sm" className="px-4">
            <VerteilungBlock
              verteilung={antwort.data.verteilung}
              aufSicht={setzeSicht}
              gesperrt={antwort.isFetching}
            />
          </Card>

          <StandZeile stand={antwort.data.stand} />
        </>
      )}
    </div>
  );
}
