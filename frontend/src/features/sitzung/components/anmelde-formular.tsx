"use client";

import { useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Fehler } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";
import { WEITER_PARAMETER, sicheresZiel } from "@/lib/routen";

import { useAnmelden } from "../hooks";

/**
 * Anmeldeformular.
 *
 * **Die Meldungen bleiben unspezifisch.** Ob der Benutzername unbekannt oder das
 * Passwort falsch war, steht hier nicht — weil es das Backend auch nicht
 * verrät. Die einzige Ausnahme ist ein gesperrtes oder deaktiviertes Konto bei
 * korrektem Passwort; die liefert das Backend als eigenen Problemtyp, und dann
 * wird sie gezeigt.
 *
 * Nach der Anmeldung geht es an den ursprünglich angefragten Ort zurück — aber
 * nur, wenn der geprüft ist (`sicheresZiel`); sonst wäre die Anmeldeseite eine
 * offene Weiterleitung.
 */
export function AnmeldeFormular() {
  const texte = useTexte();
  const router = useRouter();
  const parameter = useSearchParams();
  const anmelden = useAnmelden();

  const [benutzername, setzeBenutzername] = useState("");
  const [passwort, setzePasswort] = useState("");

  const unvollstaendig = benutzername.trim() === "" || passwort === "";

  function absenden(ereignis: FormEvent<HTMLFormElement>) {
    ereignis.preventDefault();
    if (unvollstaendig || anmelden.isPending) {
      return;
    }
    anmelden.mutate(
      { username: benutzername.trim(), password: passwort },
      {
        onSuccess: () => {
          // Wohin genau, entscheidet danach der Anwendungsrahmen: Änderungszwang
          // und fehlender Mandant gehen vor.
          router.replace(sicheresZiel(parameter.get(WEITER_PARAMETER)));
        },
      },
    );
  }

  return (
    <form onSubmit={absenden} className="space-y-4" noValidate>
      <div className="space-y-1.5">
        <Label htmlFor="benutzername">{texte.anmeldung.benutzername}</Label>
        <Input
          id="benutzername"
          name="username"
          autoComplete="username"
          autoCapitalize="none"
          spellCheck={false}
          required
          className="min-h-feld"
          value={benutzername}
          onChange={(ereignis) => setzeBenutzername(ereignis.target.value)}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="passwort">{texte.anmeldung.passwort}</Label>
        <Input
          id="passwort"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          className="min-h-feld"
          value={passwort}
          onChange={(ereignis) => setzePasswort(ereignis.target.value)}
        />
      </div>

      {anmelden.error ? <Fehler fehler={anmelden.error} /> : null}

      <Button type="submit" className="min-h-beruehrung w-full" disabled={anmelden.isPending}>
        {anmelden.isPending ? texte.anmeldung.laeuft : texte.anmeldung.absenden}
      </Button>

      {unvollstaendig ? (
        <p className="text-muted-foreground text-beiwerk">{texte.anmeldung.felderFehlen}</p>
      ) : null}
    </form>
  );
}
