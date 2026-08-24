"use client";

import { useCallback, useState } from "react";
import { Info, SquarePen } from "lucide-react";

import { KeinZugriff } from "@/components/kein-zugriff";
import { Button } from "@/components/ui/button";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { istKeinZugriff } from "@/lib/http";

import { useNutzer, useVorgang } from "../hooks";
import { darfOeffnen } from "../zeilen";
import { BenutzerTabelle } from "./benutzer-tabelle";
import { ZeilenFormular } from "./zeilen-formular";

/**
 * Die Benutzerverwaltung: **alle Konten, mandantenfrei** (E2).
 *
 * ## Der Hinweis über der Liste, und warum er kein Beiwerk ist
 *
 * **Der Mandantenumschalter in der Kopfzeile bleibt stehen** — er wird für
 * diese Seite nicht ausgeblendet, weil er zum Rahmen gehört und nicht zur
 * Ansicht. Damit steht über einer mandantenfreien Liste ein Umschalter, der
 * aussieht, als filtere er sie. Er tut es nicht, und ein Nutzer, der das
 * ausprobiert, sieht **keine Änderung** — die schlechteste Art, es zu erfahren.
 *
 * Deshalb trägt die Liste den Satz, dass sie mandantenübergreifend gilt (E21).
 * Er steht **über** der Tabelle und nicht an einer Zeile: Er handelt von der
 * Liste als Ganzem.
 *
 * > **Der Umschalter wird nicht ausgeblendet**, und das ist eine Entscheidung.
 * > Er steht in der Kopfzeile auf **jeder** Seite; ihn hier verschwinden zu
 * > lassen hieße, den Rahmen von einer Ansicht abhängig zu machen — und der
 * > aktive Mandant darf nach `docs/visuelles-konzept.md` §6 nie unsichtbar
 * > werden, weil er in der Sitzung steht und nicht in der URL. Ein Satz kostet
 * > weniger als eine Ausnahme im Rahmen.
 *
 * ## Die Reihenfolge der Zustände
 *
 * Wie bei der Katalogpflege steht **„kein Zugriff" vor allem anderen und
 * außerhalb der Kette** (`docs/prozess-katalog-frontend.md` §9): Er ist kein
 * Fehler — nichts ist kaputt, der Nutzer steht vor einer Grenze, die für ihn
 * gilt —, er ist kein Leerzustand, und er kennt keinen zweiten Versuch (`403`
 * steht beim ersten Aufruf fest, `lib/query-client.ts`). Er nimmt alles
 * Bedienbare mit weg: Wer die Liste nicht sehen darf, hat nichts zu pflegen.
 *
 * **Erkannt am Problemtyp und nicht am Statuscode.** `403` ist in diesem Backend
 * dreifach vergeben, und die drei bedeuten Verschiedenes — eine Statusprüfung
 * zeigte allen dreien dieselbe Meldung, und zwei davon wären falsch
 * (`lib/http.ts`).
 *
 * Danach die übliche Kette: **Laden, Fehler, Leer, Daten.**
 *
 * ## „Leer" hat hier genau eine Ursache — und trotzdem einen eigenen Satz
 *
 * Anders als die Pflegeliste kennt diese Ansicht keinen Filter (E16): Ist die
 * Antwort leer, gibt es keine Konten. Das kann im Betrieb nicht vorkommen — wer
 * die Liste sieht, ist selbst eines —, und genau deshalb steht dort ein Satz,
 * der das sagt, statt eines allgemeinen „nichts anzuzeigen": Ein leerer
 * Bildschirm ohne Erklärung ließe den Nutzer den Bestand verdächtigen, wo in
 * Wahrheit etwas nicht stimmen kann.
 */
export function BenutzerAnsicht() {
  const texte = useTexte();
  const liste = useNutzer();
  const [offen, setOffen] = useState<number | null>(null);

  /*
   * Die Mutation liegt **hier** und nicht im Formular, obwohl nur das Formular
   * sie auslöst. Der Grund steht eine Ebene tiefer: Solange ein Vorgang läuft,
   * darf die Zeile nicht zugeklappt werden — sonst verschwindet die einzige
   * Stelle, an der seine Antwort gemeldet wird, und ein `409` sähe aus wie „es
   * ist nichts passiert". Wer das verhindern will, muss beim Knopf wissen, ob
   * gerade etwas läuft.
   */
  const vorgang = useVorgang();

  const umschalten = useCallback(
    (id: number) => setOffen((bisher) => (bisher === id ? null : id)),
    [],
  );

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-ueberschrift font-semibold">
        {texte.administration.bereiche.benutzer.titel}
      </h1>

      {istKeinZugriff(liste.error) ? (
        <KeinZugriff />
      ) : (
        <>
          <p className="text-muted-foreground text-beiwerk flex max-w-prose items-start gap-2">
            <Info aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
            <span>{texte.benutzer.mandantenfrei}</span>
          </p>

          {liste.isPending ? (
            <Laden zeilen={6} />
          ) : liste.error ? (
            <Fehler fehler={liste.error} aufWiederholen={() => void liste.refetch()} />
          ) : liste.data.length === 0 ? (
            <Leer titel={texte.benutzer.leer.titel} hinweis={texte.benutzer.leer.hinweis} />
          ) : (
            <BenutzerTabelle
              zeilen={liste.data}
              aktionenFuer={(zeile) => (
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  aria-expanded={offen === zeile.id}
                  disabled={!darfOeffnen(offen, zeile.id) || vorgang.isPending}
                  onClick={() => umschalten(zeile.id)}
                  title={
                    darfOeffnen(offen, zeile.id)
                      ? texte.benutzer.bearbeiten
                      : texte.benutzer.bearbeitenGesperrt
                  }
                  className="min-h-bedienelement"
                >
                  <SquarePen aria-hidden="true" />
                  <span className="sr-only">
                    {einsetzen(texte.benutzer.bearbeitenFuer, { benutzer: zeile.username })}
                  </span>
                </Button>
              )}
              formularFuer={(zeile) =>
                offen === zeile.id ? <ZeilenFormular zeile={zeile} vorgang={vorgang} /> : null
              }
            />
          )}
        </>
      )}
    </div>
  );
}
