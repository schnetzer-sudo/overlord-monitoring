"use client";

import { useId, useMemo, useState } from "react";
import { Layers } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { Fehler } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import {
  RICHTUNGEN,
  ZUORDNUNGSFELDER,
  type Katalogzeile,
  type Massenmodus,
  type MassenzuordnungAntwort,
  type Zuordnungsfeld,
} from "../api";
import { useMassenzuordnung } from "../hooks";
import { projekteAus } from "../kennzahlen";
import { OHNE_RICHTUNG, massenwert, type Richtungswahl } from "../zuordnung";
import { PartnerFeld } from "./partner-feld";

/**
 * Die Massenzuordnung nach Projekt — **in zwei Schritten** (E11, E12).
 *
 * ## Warum ein Dialog, und warum der einzige der Ansicht
 *
 * Die Zeilenbearbeitung meidet ihn ausdrücklich: Sie braucht die Nachbarzeilen.
 * Diese Handlung braucht das Gegenteil — sie trifft bis zu 226 Zeilen auf
 * einmal, und wer sie auslöst, soll für einen Moment nichts anderes tun. Ein
 * Dialog ist genau die Form dafür: Er unterbricht, er verlangt eine Antwort, und
 * er ist wieder weg.
 *
 * **Ohne die Schließen-Schaltfläche des Generators** (`showCloseButton={false}`).
 * Sie trägt eine feste englische Zeichenkette — dasselbe, was
 * `docs/frontend-grundlagen.md` §10 für `sheet.tsx` als offenen Punkt führt.
 * Statt den Generatorbereich von Hand zu ändern, wird sie weggelassen; das
 * Formular hat seinen eigenen Abbrechen-Knopf, und `Escape` schließt weiterhin.
 *
 * ## Die Vorschau nennt die Zahl, die verloren geht — in Worten
 *
 * `betroffen` **und** `davonGepflegt`. Die zweite ist die eigentliche Auskunft:
 * Dass gepflegte Zeilen überschrieben werden, ist gewollt (E12) — ein
 * Schutzmodus „nur offene Zeilen" machte genau die Korrektur unmöglich, für die
 * man das Werkzeug braucht. Deshalb steht sie als Satz da und nicht als Zahl in
 * einer Tabelle, und deshalb ist „Ausführen" gesperrt, bis eine Vorschau
 * vorliegt.
 *
 * **Jede Änderung am Formular verwirft die Vorschau.** Sonst bestätigte der
 * Nutzer eine Zahl, die zu einer anderen Anfrage gehört — genau der Fehler, den
 * das gemeinsame Statement im Backend auf seiner Seite ausschließt.
 *
 * **Der Modus wird immer ausdrücklich mitgeschickt**, auch die Vorschau. Ohne
 * Angabe nähme das Backend `VORSCHAU`; sich darauf zu verlassen hieße, die
 * harmloseste Wirkung dem Weglassen zu überlassen.
 */
export function Massenzuordnung({
  zeilen,
  vorschlaege,
  gesperrt,
}: {
  /** Die **volle** Liste — sonst fehlten die vollständig gepflegten Projekte. */
  zeilen: readonly Katalogzeile[];
  vorschlaege: readonly string[];
  gesperrt: boolean;
}) {
  const texte = useTexte();
  const [offen, setOffen] = useState(false);

  return (
    <Dialog open={offen} onOpenChange={setOffen}>
      <DialogTrigger asChild>
        <Button type="button" variant="outline" disabled={gesperrt} className="min-h-beruehrung">
          <Layers aria-hidden="true" />
          {texte.katalog.masse.oeffnen}
        </Button>
      </DialogTrigger>
      <DialogContent showCloseButton={false} className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{texte.katalog.masse.titel}</DialogTitle>
          <DialogDescription>{texte.katalog.masse.einleitung}</DialogDescription>
        </DialogHeader>
        {/*
         * Der Inhalt hängt am geöffneten Dialog: Radix hängt ihn beim Schließen
         * wieder aus, und damit beginnt jede Massenzuordnung mit einem leeren
         * Formular und ohne alte Vorschau.
         */}
        <MassenzuordnungFormular
          zeilen={zeilen}
          vorschlaege={vorschlaege}
          aufFertig={() => setOffen(false)}
        />
      </DialogContent>
    </Dialog>
  );
}

function MassenzuordnungFormular({
  zeilen,
  vorschlaege,
  aufFertig,
}: {
  zeilen: readonly Katalogzeile[];
  vorschlaege: readonly string[];
  aufFertig: () => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const projektId = useId();
  const feldId = useId();
  const wertId = useId();

  const projekte = useMemo(() => projekteAus(zeilen), [zeilen]);
  const [projectId, setProjectId] = useState("");
  const [feld, setFeld] = useState<Zuordnungsfeld>("PARTNER");
  const [partner, setPartner] = useState("");
  const [richtung, setRichtung] = useState<Richtungswahl>(OHNE_RICHTUNG);
  const [vorschau, setVorschau] = useState<MassenzuordnungAntwort | null>(null);
  const zuordnung = useMassenzuordnung();

  const wert = massenwert(feld, partner, richtung);
  const bereit = projectId !== "";

  function frage(modus: Massenmodus) {
    zuordnung.mutate(
      { projectId, feld, wert, modus },
      {
        onSuccess: (antwort) => {
          if (antwort.modus === "AUSFUEHREN") {
            aufFertig();
            return;
          }
          setVorschau(antwort);
        },
      },
    );
  }

  return (
    <form
      onSubmit={(ereignis) => {
        ereignis.preventDefault();
        if (bereit && !zuordnung.isPending) {
          frage(vorschau === null ? "VORSCHAU" : "AUSFUEHREN");
        }
      }}
      className="flex flex-col gap-4"
    >
      <div className="flex flex-col gap-1">
        <Label htmlFor={projektId}>{texte.katalog.masse.projekt}</Label>
        {/*
         * Ein natives Auswahlfeld. Das Projekt muss **existieren** — frei
         * tippbar wie der Partner wäre es ein sicheres `404`. Und ein eigener
         * Baustein dafür entsteht nicht: Der Bestand kennt keinen, die Liste ist
         * je Mandant höchstens 39 Einträge lang, und ein natives Feld bedient
         * sich am Finger und mit der Tastatur besser als jeder Nachbau.
         */}
        <select
          id={projektId}
          value={projectId}
          onChange={(ereignis) => {
            setProjectId(ereignis.target.value);
            setVorschau(null);
          }}
          className="border-input focus-visible:border-ring focus-visible:ring-ring/50 h-feld w-full min-w-0 rounded-lg border bg-transparent px-2.5 py-1 outline-none focus-visible:ring-3"
        >
          <option value="">{texte.katalog.masse.projektWaehlen}</option>
          {projekte.map((projekt) => (
            <option key={projekt.projectId} value={projekt.projectId}>
              {projekt.projectId}
              {projekt.projectName === null ? "" : ` · ${projekt.projectName}`} (
              {formatiereZahl(projekt.anzahl, sprache)})
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1">
        <span id={feldId} className="text-basis font-medium">
          {texte.katalog.masse.feld}
        </span>
        <ToggleGroup
          type="single"
          variant="outline"
          aria-labelledby={feldId}
          value={feld}
          onValueChange={(neu) => {
            // Die Abwahl des aktiven Knopfes wird verworfen: Eine
            // Massenzuordnung ohne Feld hat keinen Gegenstand, und das Backend
            // antwortet darauf mit `feld-unbekannt`.
            if (neu === "") {
              return;
            }
            setFeld(neu as Zuordnungsfeld);
            setVorschau(null);
          }}
        >
          {ZUORDNUNGSFELDER.map((eintrag) => (
            <ToggleGroupItem key={eintrag} value={eintrag} className="min-h-bedienelement px-2.5">
              {texte.katalog.masse.felder[eintrag]}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>

      <div className="flex flex-col gap-1">
        {feld === "PARTNER" ? (
          <>
            <Label htmlFor={wertId}>{texte.katalog.masse.wert}</Label>
            <PartnerFeld
              kennung={wertId}
              wert={partner}
              vorschlaege={vorschlaege}
              aufAenderung={(neu) => {
                setPartner(neu);
                setVorschau(null);
              }}
            />
          </>
        ) : (
          <>
            <span id={wertId} className="text-basis font-medium">
              {texte.katalog.masse.wert}
            </span>
            <ToggleGroup
              type="single"
              variant="outline"
              aria-labelledby={wertId}
              value={richtung}
              onValueChange={(neu) => {
                setRichtung((neu === "" ? OHNE_RICHTUNG : neu) as Richtungswahl);
                setVorschau(null);
              }}
            >
              {RICHTUNGEN.map((eintrag) => (
                <ToggleGroupItem
                  key={eintrag}
                  value={eintrag}
                  className="min-h-bedienelement px-2.5"
                >
                  {texte.katalog.richtungen[eintrag]}
                </ToggleGroupItem>
              ))}
              <ToggleGroupItem value={OHNE_RICHTUNG} className="min-h-bedienelement px-2.5">
                {texte.katalog.bearbeiten.ohneRichtung}
              </ToggleGroupItem>
            </ToggleGroup>
          </>
        )}
        {wert === null ? (
          <p className="text-muted-foreground text-beiwerk">{texte.katalog.masse.leerHinweis}</p>
        ) : null}
      </div>

      {zuordnung.error ? <Fehler fehler={zuordnung.error} /> : null}

      <VorschauSatz vorschau={vorschau} />

      <div className="flex flex-wrap items-center gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={!bereit || zuordnung.isPending}
          onClick={() => frage("VORSCHAU")}
          className="min-h-beruehrung"
        >
          {zuordnung.isPending && vorschau === null
            ? texte.katalog.masse.vorschauLaeuft
            : texte.katalog.masse.vorschauHolen}
        </Button>
        <Button
          type="button"
          disabled={vorschau === null || vorschau.betroffen === 0 || zuordnung.isPending}
          onClick={() => frage("AUSFUEHREN")}
          className="min-h-beruehrung"
        >
          {zuordnung.isPending && vorschau !== null
            ? texte.katalog.masse.ausfuehrenLaeuft
            : texte.katalog.masse.ausfuehren}
        </Button>
        <Button
          type="button"
          variant="ghost"
          disabled={zuordnung.isPending}
          onClick={aufFertig}
          className="min-h-beruehrung"
        >
          {texte.katalog.masse.abbrechen}
        </Button>
      </div>
    </form>
  );
}

/**
 * Was die Vorschau sagt — **zwei Sätze, und der zweite ist der wichtige.**
 *
 * Ohne Vorschau steht dort, dass es eine braucht. Das ist keine Belehrung,
 * sondern die Erklärung für den gesperrten Ausführen-Knopf daneben.
 */
function VorschauSatz({ vorschau }: { vorschau: MassenzuordnungAntwort | null }) {
  const texte = useTexte();
  const sprache = useSprache();

  if (vorschau === null) {
    return (
      <p className="text-muted-foreground text-beiwerk">{texte.katalog.masse.vorschauNoetig}</p>
    );
  }

  if (vorschau.betroffen === 0) {
    return <p className="text-beiwerk">{texte.katalog.masse.betroffenKeine}</p>;
  }

  const betroffen =
    vorschau.betroffen === 1
      ? texte.katalog.masse.betroffenEins
      : einsetzen(texte.katalog.masse.betroffenViele, {
          betroffen: formatiereZahl(vorschau.betroffen, sprache),
        });

  const verloren =
    vorschau.davonGepflegt === 0
      ? texte.katalog.masse.verlorenKeine
      : vorschau.davonGepflegt === 1
        ? texte.katalog.masse.verlorenEins
        : einsetzen(texte.katalog.masse.verlorenViele, {
            gepflegt: formatiereZahl(vorschau.davonGepflegt, sprache),
          });

  return (
    <p role="status" className="text-beiwerk">
      {betroffen}{" "}
      <span className={cn(vorschau.davonGepflegt > 0 && "font-medium")}>{verloren}</span>
    </p>
  );
}
