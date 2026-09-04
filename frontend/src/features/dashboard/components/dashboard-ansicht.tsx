"use client";

import { Card } from "@/components/ui/card";
import { ZeitraumUmschalter } from "@/components/zeitraum-umschalter";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";

import { hervorgehobenerZeitraum } from "../filter";
import { useDashboard, useDashboardzustand } from "../hooks";
import { AufgefallenBlock } from "./aufgefallen-block";
import { Kacheln } from "./kacheln";
import { VerteilungBlock } from "./verteilung-block";
import { StandZeile } from "./stand-zeile";
import { VerlaufDiagramm } from "./verlauf-diagramm";

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
 * Zuerst die Kacheln, dann das Bild, dann die einzelnen Zeilen, zuletzt der
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
          {/*
           * **`zeitraum` kommt aus der Antwort und nicht aus dem Zustand.** Die
           * Kachel *Wartend* rechnet daraus die Eimerbreite, mit der ihr eigenes
           * Fenster Luft nach hinten bekommt (`verweise.ts`) — und maßgeblich
           * ist das Paar, mit dem der Endpunkt geantwortet hat, nicht das, was
           * jemand angeklickt hat. Solange nichts gewählt ist, gibt es das
           * zweite gar nicht (E‑n).
           */}
          <Kacheln
            kacheln={antwort.data.kacheln}
            fenster={antwort.data.fenster}
            zeitraum={antwort.data.zeitraum}
          />

          <Card size="sm" className="px-4">
            <VerlaufDiagramm punkte={antwort.data.verlauf} zeitraum={antwort.data.zeitraum} />
          </Card>

          {/*
           * Am breiten Fenster **nebeneinander**, darunter untereinander. Beide
           * Blöcke sind Nachschlagewerke und keine Meldungen; sie um die volle
           * Breite streiten zu lassen kostete eine Bildschirmhöhe, ohne dass
           * eine Zeile mehr zu sehen wäre.
           *
           * Die Zeilen stehen links: Sie sind das Konkrete — eine Kennung, auf
           * die man klickt. Die Verteilung ist Hintergrund.
           */}
          <div className="grid gap-4 xl:grid-cols-[3fr_2fr]">
            <Card size="sm" className="px-4">
              <AufgefallenBlock zeilen={antwort.data.zuletztAufgefallen} />
            </Card>
            <Card size="sm" className="px-4">
              <VerteilungBlock
                verteilung={antwort.data.verteilung}
                aufSicht={setzeSicht}
                gesperrt={antwort.isFetching}
              />
            </Card>
          </div>

          <StandZeile stand={antwort.data.stand} />
        </>
      )}
    </div>
  );
}
