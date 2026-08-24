"use client";

import { useId, useState } from "react";

import { useAngemeldeterName } from "@/components/angemeldet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Fehler } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";

import { ROLLEN, istRolle, type Nutzerzeile, type Rolle } from "../api";
import { useVorgang } from "../hooks";
import {
  PASSWORT_MINDESTLAENGE,
  brauchtVorwarnung,
  istEigenesKonto,
  passwortBrauchbar,
  type Vorgang,
} from "../selbstschutz";
import { MandantenAuswahl } from "./mandanten-auswahl";
import { Vorwarnung } from "./vorwarnung";

/**
 * Alles, was sich an **einer** Zeile ändern lässt — die fünf Vorgänge aus
 * `docs/benutzerverwaltung.md` §5, jeder ein Aufruf auf genau diese Zeile.
 *
 * ## Warum das Formular unter der Zeile steht und nicht in ihr
 *
 * Dieselbe Bauform und dieselbe Begründung wie bei der Katalogpflege
 * (`docs/prozess-katalog-frontend.md` §5), hier aber mit einem zusätzlichen
 * Grund, der allein trägt:
 *
 * **Unter `md` sind Rolle und Mandanten ausgeblendet, unter `lg` zusätzlich
 * Passwortwechsel und letzte Anmeldung.** Stünden die Bedienelemente in den
 * Zellen, wären drei der fünf Vorgänge am schmalen Fenster gar nicht
 * erreichbar. Im Formular sind sie es bei jeder Breite — ausgeblendet wird nur
 * **Anzeige**.
 *
 * Der bisherige Stand der Zeile bleibt dabei sichtbar, statt durch Eingabefelder
 * ersetzt zu werden. Das ist bei einer Verwaltungsmaske mehr wert als bei einer
 * Pflegeliste: Wer eine Sperre umlegt, will die Zeile, die er umlegt, noch
 * lesen können.
 *
 * ## Ein Vorgang zur Zeit, und die Meldung steht an ihm
 *
 * Alle Bedienelemente hängen an **einer** Mutation ({@link useVorgang}), und
 * solange sie läuft, sind sie gesperrt. Das ist keine Sparsamkeit: Zwei
 * gleichzeitige Aufrufe auf dieselbe Zeile bekämen zwei Antworten mit
 * demselben Anspruch, den Zwischenspeicher zu setzen — und welche zuletzt
 * ankommt, entscheidet dann das Netz.
 *
 * **Die Fehlermeldung steht am auslösenden Abschnitt** und nicht über dem
 * Formular. Vier der neun Problemtypen dieser Seite sind `409` und sagen etwas
 * über den *Zustand* des Kontos; welcher Vorgang gemeint ist, geht ohne die Nähe
 * verloren.
 *
 * ## Die verbotenen Richtungen werden aufgefangen und nicht vorweggenommen
 *
 * Sich selbst zu sperren, zu deaktivieren oder herabzustufen verbietet E12 mit
 * `409 selbstschutz`. Die Oberfläche sperrt diese Schalter trotzdem **nicht** —
 * und das ist eine Entscheidung mit einem Grund, der am Backend hängt:
 *
 * > `pruefeEntwertung` prüft **zuerst** den letzten nutzbaren Administrator und
 * > **danach** das eigene Konto. Wer als einziger nutzbarer Admin angemeldet
 * > ist, bekommt deshalb `letzter-admin` und nicht `selbstschutz` — und diese
 * > Meldung sagt mehr: Sie nennt die Bedingung, unter der es ginge. Ein selbst
 * > gebauter Riegel „nicht am eigenen Konto" zeigte an dieser Stelle den
 * > *falschen* der beiden Sätze.
 *
 * Was die Oberfläche sehr wohl vorwegnimmt, ist der Fall, den sie **allein aus
 * der Zeile** ablesen kann: eine Herabstufung ohne Mandantenzuordnung (E11).
 */
export function ZeilenFormular({ zeile }: { zeile: Nutzerzeile }) {
  const texte = useTexte();
  const angemeldet = useAngemeldeterName();
  const vorgang = useVorgang();
  const [wartend, setWartend] = useState<Vorgang | null>(null);

  const sperreId = useId();
  const aktivId = useId();
  const rolleId = useId();
  const passwortId = useId();

  const [passwort, setPasswort] = useState("");
  const eigenes = istEigenesKonto(zeile.username, angemeldet);
  const gesperrt = vorgang.isPending;

  /**
   * **Der eine Weg, auf dem ein Vorgang losläuft.** Erst steht fest, was getan
   * werden soll, dann fällt die Entscheidung über die Vorwarnung, dann läuft es
   * — nie umgekehrt und nie daran vorbei (E19).
   */
  function starte(neu: Vorgang) {
    if (brauchtVorwarnung(neu, zeile.username, angemeldet)) {
      setWartend(neu);
      return;
    }
    fuehreAus(neu);
  }

  function fuehreAus(neu: Vorgang) {
    vorgang.mutate(
      { id: zeile.id, vorgang: neu },
      {
        onSuccess: () => {
          setWartend(null);
          if (neu.art === "passwort") {
            setPasswort("");
          }
        },
        // Bei einem Fehler bleibt der Dialog nicht stehen: Die Meldung gehört an
        // den Abschnitt, aus dem der Vorgang kam, und dort sieht sie der Nutzer
        // nur, wenn nichts darüber liegt.
        onError: () => setWartend(null),
      },
    );
  }

  /** Was gerade lief, als es schiefging — für die Meldung am richtigen Abschnitt. */
  const fehlerBei = vorgang.error ? vorgang.variables?.vorgang.art : undefined;
  const meldung = (art: Vorgang["art"]) =>
    fehlerBei === art ? <Fehler fehler={vorgang.error} /> : null;

  // E11: Eine Herabstufung ohne Mandantenzuordnung lehnt das Backend ab. Der
  // Fall steht vollständig in der Zeile und ist deshalb vorherzusehen, statt
  // ihn erst über `409 rolle-ohne-mandant` zu lernen.
  const herabstufungMoeglich = zeile.tenants.length > 0;

  return (
    <div className="flex flex-col gap-4 px-2 py-3">
      <div className="flex flex-col gap-4 md:flex-row md:flex-wrap md:items-start md:gap-6">
        <Abschnitt>
          <div className="min-h-beruehrung flex items-center gap-2">
            <Switch
              id={sperreId}
              checked={zeile.locked}
              disabled={gesperrt}
              onCheckedChange={(an) => starte({ art: "sperre", gesperrt: an })}
            />
            <Label htmlFor={sperreId}>{texte.benutzer.formular.sperre}</Label>
          </div>
          <p className="text-muted-foreground text-beiwerk max-w-prose">
            {texte.benutzer.formular.sperreHinweis}
          </p>
          {meldung("sperre")}
        </Abschnitt>

        <Abschnitt>
          <div className="min-h-beruehrung flex items-center gap-2">
            <Switch
              id={aktivId}
              checked={zeile.active}
              disabled={gesperrt}
              onCheckedChange={(an) => starte({ art: "aktiv", aktiv: an })}
            />
            <Label htmlFor={aktivId}>{texte.benutzer.formular.aktiv}</Label>
          </div>
          <p className="text-muted-foreground text-beiwerk max-w-prose">
            {texte.benutzer.formular.aktivHinweis}
          </p>
          {meldung("aktiv")}
        </Abschnitt>

        <Abschnitt>
          <Label htmlFor={rolleId}>{texte.benutzer.formular.rolle}</Label>
          {/*
           * Ein natives Auswahlfeld — dieselbe Wahl und dieselbe Begründung wie
           * bei der Projektauswahl der Massenzuordnung: Der Bestand kennt keinen
           * `Select`-Baustein, die Menge hat zwei Einträge, und ein natives Feld
           * bedient sich am Finger und mit der Tastatur besser als jeder Nachbau.
           */}
          <select
            id={rolleId}
            value={istRolle(zeile.role) ? zeile.role : ""}
            disabled={gesperrt}
            onChange={(ereignis) => starte({ art: "rolle", rolle: ereignis.target.value as Rolle })}
            className="border-input focus-visible:border-ring focus-visible:ring-ring/50 h-feld w-full min-w-0 rounded-lg border bg-transparent px-2.5 py-1 outline-none focus-visible:ring-3"
          >
            {/*
             * Ein unbekannter Rollenwert bekommt einen eigenen Eintrag, statt
             * still auf einen der beiden bekannten zu fallen. Sonst zeigte das
             * Feld eine Rolle an, die das Konto nicht hat.
             */}
            {istRolle(zeile.role) ? null : <option value="">{zeile.role}</option>}
            {ROLLEN.map((rolle) => (
              <option
                key={rolle}
                value={rolle}
                disabled={rolle === "MANDANT" && !herabstufungMoeglich}
              >
                {texte.rolle[rolle]}
              </option>
            ))}
          </select>
          {herabstufungMoeglich ? null : (
            <p className="text-muted-foreground text-beiwerk max-w-prose">
              {texte.benutzer.formular.rolleOhneMandant}
            </p>
          )}
          {meldung("rolle")}
        </Abschnitt>
      </div>

      {/*
       * Die Mandantenmenge steht auf voller Breite und nicht neben den drei
       * Abschnitten darüber: Sie ist die einzige mit einem eigenen Entwurf und
       * einem eigenen Speichern — die anderen drei laufen beim Umlegen los. Und
       * bei zehn Mandanten braucht sie die Breite.
       *
       * **Der `key` setzt den Entwurf nach dem Speichern zurück.** Ändert sich
       * die gespeicherte Menge, hängt React die Auswahl neu ein; ein `setState`
       * im Effekt wäre der naheliegende und der falsche Weg.
       */}
      <MandantenAuswahl
        key={zeile.tenants.join(" ")}
        zeile={zeile}
        gesperrt={gesperrt}
        aufSpeichern={(mandanten) => starte({ art: "mandanten", mandanten })}
        fehler={meldung("mandanten")}
      />

      <form
        className="flex flex-col gap-2"
        onSubmit={(ereignis) => {
          ereignis.preventDefault();
          if (!gesperrt && passwortBrauchbar(passwort)) {
            starte({ art: "passwort", passwort });
          }
        }}
      >
        <Label htmlFor={passwortId}>{texte.benutzer.formular.passwort}</Label>
        <div className="flex flex-wrap items-center gap-2">
          <Input
            id={passwortId}
            type="password"
            value={passwort}
            disabled={gesperrt}
            autoComplete="new-password"
            onChange={(ereignis) => setPasswort(ereignis.target.value)}
            className="w-full max-w-xs"
          />
          <Button
            type="submit"
            variant="outline"
            disabled={gesperrt || !passwortBrauchbar(passwort)}
            className="min-h-beruehrung"
          >
            {texte.benutzer.formular.passwortSetzen}
          </Button>
        </div>
        {/*
         * Beide Sätze stehen immer da, und beide sind die Antwort auf eine Frage,
         * die sonst nachher käme: wie lang es sein muss (E13), und dass das Konto
         * danach beim nächsten Anmelden wechseln muss.
         */}
        <p className="text-muted-foreground text-beiwerk max-w-prose">
          {einsetzen(texte.benutzer.formular.passwortHinweis, {
            laenge: String(PASSWORT_MINDESTLAENGE),
          })}
        </p>
        {meldung("passwort")}
      </form>

      {eigenes ? (
        <p className="text-beiwerk max-w-prose">{texte.benutzer.formular.eigenesKonto}</p>
      ) : null}

      <Vorwarnung
        vorgang={wartend}
        laeuft={gesperrt}
        aufAbbrechen={() => setWartend(null)}
        aufBestaetigen={() => {
          if (wartend !== null) {
            fuehreAus(wartend);
          }
        }}
      />
    </div>
  );
}

function Abschnitt({ children }: { children: React.ReactNode }) {
  return <div className="flex min-w-0 flex-1 basis-56 flex-col gap-1.5">{children}</div>;
}
