"use client";

import { useId, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Fehler, Laden } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";

import { ROLLEN } from "../api";
import { LEERER_ENTWURF, anfrageAus, type Entwurf } from "../anlegen";
import { useMandanten, type useAnlegen } from "../hooks";
import { PASSWORT_MINDESTLAENGE } from "../selbstschutz";

/**
 * **Ein Konto anlegen — über die Oberfläche statt über den Endpunkt** (9c).
 *
 * `POST /api/admin/users` steht seit Schritt 3 und ist unangetastet (E4). Was
 * fehlte, war der Bedienweg: Bis zum 07.09.2026 entstand jedes Konto von Hand
 * am Endpunkt, auch das Wegwerfkonto der Sichtprüfung.
 *
 * ## Aufklappbar über der Liste, mit derselben Sperre wie die Zeilen (E22)
 *
 * Kein Dialog, keine eigene Route. Die Seite hat bereits **genau ein** Muster
 * für „etwas Bedienbares aufklappen und dabei alles andere sperren"; ein zweites
 * daneben wäre ein zweiter Bedienweg für dieselbe Sache.
 *
 * **Die Sperre gilt in beide Richtungen** — solange diese Maske offen ist, lässt
 * sich keine Zeile öffnen, und solange eine Zeile offen ist, nicht die Maske
 * (`darfOeffnen` in `../zeilen.ts`, um `MASKE` erweitert und nicht dupliziert).
 * Der Grund ist derselbe wie bei den Zeilen und wiegt hier genauso schwer: Das
 * Formular hält ein **getipptes Einmalpasswort**, das danach an keiner Stelle
 * mehr steht, auch nicht im Protokoll.
 *
 * ## Genau ein Mandant (E23)
 *
 * Der Endpunkt nimmt einen. **Verworfen ist, die Maske als Mehrfachauswahl zu
 * bauen und intern `POST` und anschließend `PUT {id}/tenants` zu fahren:** Das
 * wären zwei Vorgänge und zwei Protokollzeilen für eine Handlung, und scheiterte
 * der zweite, stünde ein halb angelegtes Konto in der Liste. Die Maske sagt
 * stattdessen selbst, wo weitere Mandanten hinzukommen.
 *
 * ## Was nach dem Erfolg passiert (E24, E25)
 *
 * **Die Liste wird neu geholt, der Zwischenspeicher nicht gesetzt** — die
 * Begründung steht bei {@link useAnlegen}, wo die Ausnahme gemacht wird.
 *
 * **Die Maske bleibt offen, geleert, mit der Meldung darin.** Sie steht am
 * auslösenden Abschnitt wie überall sonst auf dieser Seite; schlösse sich die
 * Maske, hätte die Meldung keinen Ort. Und ein Admin, der ein Konto anlegt, legt
 * oft ein zweites an. **Zuerst geleert wird das Passwortfeld** — es ist das
 * eine, das nirgends stehen bleiben darf.
 *
 * ## Kein Selbstschutz und keine Vorwarnung
 *
 * Anlegen trifft nie das eigene Konto und verwirft keine Sitzung (E5 gilt für
 * die fünf schreibenden Vorgänge an einer bestehenden Zeile). E19 hier
 * einzuhängen wäre eine Regel ohne Fall.
 *
 * ## Und keine Berechtigungsentscheidung
 *
 * Die Seite prüft die Rolle nicht (`benutzer-ansicht.tsx`). Ein `403` des
 * Backends nimmt die ganze Ansicht mit, diese Maske eingeschlossen — sie steht
 * unterhalb von „kein Zugriff" und nicht daneben.
 */
export function KontoAnlegen({
  anlegen,
}: {
  /**
   * Die Mutation liegt in der Ansicht, weil auch der Aufklapp-Knopf wissen muss,
   * ob gerade etwas läuft: Solange der Aufruf läuft, darf die Maske nicht
   * zugeklappt werden — sonst wäre ein `409` nirgends zu sehen. Sie wird
   * durchgereicht und nicht hier angelegt, genau wie beim Zeilenformular.
   */
  anlegen: ReturnType<typeof useAnlegen>;
}) {
  const texte = useTexte();
  /*
   * **Erst beim Aufklappen**, nicht beim Betreten der Seite: Die Komponente wird
   * nur eingehängt, solange die Maske offen ist. Dieselbe Abfrage unter
   * demselben Schlüssel wie die Mengenpflege an der Zeile — wer vorher eine
   * Zeile offen hatte, findet sie schon beantwortet vor.
   */
  const mandanten = useMandanten();
  const T = texte.benutzer.anlegen;

  const benutzernameId = useId();
  const rolleId = useId();
  const mandantId = useId();
  const passwortId = useId();

  const [entwurf, setEntwurf] = useState<Entwurf>(LEERER_ENTWURF);
  const anfrage = anfrageAus(entwurf);
  const laeuft = anlegen.isPending;

  function setze(feld: keyof Entwurf, wert: string) {
    setEntwurf((bisher) => ({ ...bisher, [feld]: wert }));
  }

  function abschicken() {
    if (anfrage === null || laeuft) {
      return;
    }
    anlegen.mutate(anfrage, {
      // Das Leeren gehört an das **Ereignis** und nicht in einen Effekt: Der
      // Erfolg ist etwas, das passiert, und kein Zustand, mit dem sich etwas
      // abgleichen ließe (`react-hooks/set-state-in-effect`). Ein Objekt wird in
      // einem Zug gesetzt — das Passwortfeld kann den Erfolg nicht überleben.
      onSuccess: () => setEntwurf(LEERER_ENTWURF),
    });
  }

  if (mandanten.isPending) {
    return <Laden zeilen={2} />;
  }

  if (mandanten.error) {
    return <Fehler fehler={mandanten.error} aufWiederholen={() => void mandanten.refetch()} />;
  }

  return (
    <form
      className="border-border flex flex-col gap-4 rounded-lg border px-3 py-3"
      onSubmit={(ereignis) => {
        ereignis.preventDefault();
        abschicken();
      }}
    >
      <h2 className="text-basis font-medium">{T.titel}</h2>

      {/*
       * Drei Sätze, und alle drei stehen **über** den Feldern: Sie beantworten
       * Fragen, die sonst erst nach dem Tippen kämen — wie lang das Passwort
       * sein muss (E13), dass das Konto es bei der ersten Anmeldung selbst
       * ändern muss, und wo weitere Mandanten hinzukommen (E23).
       */}
      <div className="text-muted-foreground text-beiwerk flex max-w-prose flex-col gap-1">
        <p>{einsetzen(T.passwortHinweis, { laenge: String(PASSWORT_MINDESTLAENGE) })}</p>
        <p>{T.mandantenHinweis}</p>
      </div>

      <div className="flex flex-col gap-4 md:flex-row md:flex-wrap md:items-start md:gap-6">
        <Feld>
          <Label htmlFor={benutzernameId}>{T.benutzername}</Label>
          <Input
            id={benutzernameId}
            value={entwurf.username}
            disabled={laeuft}
            autoComplete="off"
            onChange={(ereignis) => setze("username", ereignis.target.value)}
          />
        </Feld>

        <Feld>
          <Label htmlFor={rolleId}>{T.rolle}</Label>
          {/*
           * Native Auswahlfelder, dieselbe Wahl und dieselbe Begründung wie im
           * Zeilenformular: Der Bestand kennt keinen `Select`-Baustein, und ein
           * natives Feld bedient sich am Finger und mit der Tastatur besser als
           * jeder Nachbau.
           */}
          <select
            id={rolleId}
            value={entwurf.role}
            disabled={laeuft}
            onChange={(ereignis) => setze("role", ereignis.target.value)}
            className={AUSWAHL}
          >
            <option value="">{T.waehlen}</option>
            {ROLLEN.map((rolle) => (
              <option key={rolle} value={rolle}>
                {texte.rolle[rolle]}
              </option>
            ))}
          </select>
        </Feld>

        <Feld>
          <Label htmlFor={mandantId}>{T.mandant}</Label>
          {/*
           * **Einfachauswahl**, weil der Endpunkt einen Mandanten nimmt (E23) —
           * und aus derselben Quelle wie die Mengenpflege an der Zeile, ohne
           * Nachfilterung: `SYSTEM` und `WOC` stehen mit drin. Eine zweite
           * Behandlung nur hier wäre eine Regel im Browser darüber, wer was
           * sehen darf.
           */}
          <select
            id={mandantId}
            value={entwurf.mandantId}
            disabled={laeuft}
            onChange={(ereignis) => setze("mandantId", ereignis.target.value)}
            className={AUSWAHL}
          >
            <option value="">{T.waehlen}</option>
            {mandanten.data.map((mandant) => (
              <option key={mandant.id} value={mandant.id}>
                {mandant.id} — {mandant.name}
              </option>
            ))}
          </select>
        </Feld>

        <Feld>
          <Label htmlFor={passwortId}>{T.passwort}</Label>
          <Input
            id={passwortId}
            type="password"
            value={entwurf.initialPassword}
            disabled={laeuft}
            autoComplete="new-password"
            onChange={(ereignis) => setze("initialPassword", ereignis.target.value)}
          />
        </Feld>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Button
          type="submit"
          variant="outline"
          disabled={laeuft || anfrage === null}
          className="min-h-beruehrung"
        >
          {laeuft ? T.laeuft : T.absenden}
        </Button>
      </div>

      {/*
       * **Die Meldung steht im Formular** und nicht über der Liste — dieselbe
       * Regel wie an der Zeile: Ein `409 benutzername-vergeben` sagt etwas über
       * genau diese Eingabe, und über der Liste ginge der Bezug verloren.
       */}
      {anlegen.error ? <Fehler fehler={anlegen.error} /> : null}

      {anlegen.data ? (
        <p
          role="status"
          className="border-border bg-card text-beiwerk max-w-prose rounded-lg border px-3 py-2"
        >
          <span className="font-medium">{T.erfolgTitel}</span>{" "}
          <span className="text-muted-foreground">
            {einsetzen(T.erfolgText, {
              benutzer: anlegen.data.username,
              // Ein unbekannter Rollenwert bleibt **roh** stehen, statt auf
              // einen bekannten gebogen zu werden (Regel Q4).
              rolle:
                texte.rolle[anlegen.data.role as keyof typeof texte.rolle] ?? anlegen.data.role,
              mandant: anlegen.data.mandantId,
            })}
          </span>
        </p>
      ) : null}
    </form>
  );
}

/** Dieselben Klassen wie das Auswahlfeld des Zeilenformulars — ein Feld, ein Aussehen. */
const AUSWAHL =
  "border-input focus-visible:border-ring focus-visible:ring-ring/50 h-feld w-full min-w-0 rounded-lg border bg-transparent px-2.5 py-1 outline-none focus-visible:ring-3";

function Feld({ children }: { children: React.ReactNode }) {
  return <div className="flex min-w-0 flex-1 basis-56 flex-col gap-1.5">{children}</div>;
}
