"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Fehler } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";
import { ROUTEN } from "@/lib/routen";

import { usePasswortAendern } from "../hooks";

/**
 * Passwortänderung — derselbe Pfad für den erzwungenen und den freiwilligen
 * Fall. Ein zweites Formular für „freiwillig" hieße zwei Stellen, an denen die
 * Regeln stehen.
 *
 * Die Wiederholung wird **hier** geprüft, nicht im Backend: Sie ist kein
 * fachliches Kriterium, sondern ein Tippfehlerschutz. Länge und Ungleichheit zum
 * alten Passwort prüft dagegen das Backend, und nur dort.
 */
export function PasswortFormular({ zwang }: { zwang: boolean }) {
  const texte = useTexte();
  const router = useRouter();
  const aendern = usePasswortAendern();

  const [alt, setzeAlt] = useState("");
  const [neu, setzeNeu] = useState("");
  const [wiederholung, setzeWiederholung] = useState("");

  const wiederholungFalsch = wiederholung !== "" && wiederholung !== neu;
  const unvollstaendig = alt === "" || neu === "" || wiederholung === "";

  function absenden(ereignis: FormEvent<HTMLFormElement>) {
    ereignis.preventDefault();
    if (unvollstaendig || wiederholungFalsch || aendern.isPending) {
      return;
    }
    aendern.mutate(
      { oldPassword: alt, newPassword: neu },
      {
        // Wohin es danach geht, entscheidet der Anwendungsrahmen: Ist noch kein
        // Mandant gewählt, führt er von hier weiter zur Auswahl.
        onSuccess: () => router.replace(ROUTEN.startseite),
      },
    );
  }

  return (
    <form onSubmit={absenden} className="space-y-4" noValidate>
      <p className="text-muted-foreground">
        {zwang ? texte.passwort.zwangEinleitung : texte.passwort.freiwilligEinleitung}
      </p>

      <div className="space-y-1.5">
        <Label htmlFor="passwort-alt">{texte.passwort.alt}</Label>
        <Input
          id="passwort-alt"
          type="password"
          autoComplete="current-password"
          required
          className="min-h-feld"
          value={alt}
          onChange={(ereignis) => setzeAlt(ereignis.target.value)}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="passwort-neu">{texte.passwort.neu}</Label>
        <Input
          id="passwort-neu"
          type="password"
          autoComplete="new-password"
          required
          aria-describedby="passwort-regel"
          className="min-h-feld"
          value={neu}
          onChange={(ereignis) => setzeNeu(ereignis.target.value)}
        />
        <p id="passwort-regel" className="text-muted-foreground text-beiwerk">
          {texte.passwort.regel}
        </p>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="passwort-wiederholung">{texte.passwort.wiederholung}</Label>
        <Input
          id="passwort-wiederholung"
          type="password"
          autoComplete="new-password"
          required
          aria-invalid={wiederholungFalsch}
          className="min-h-feld"
          value={wiederholung}
          onChange={(ereignis) => setzeWiederholung(ereignis.target.value)}
        />
        {wiederholungFalsch ? (
          <p className="text-status-fehler text-beiwerk">{texte.passwort.wiederholungFalsch}</p>
        ) : null}
      </div>

      {aendern.error ? <Fehler fehler={aendern.error} /> : null}

      <Button
        type="submit"
        className="min-h-beruehrung w-full"
        disabled={aendern.isPending || unvollstaendig || wiederholungFalsch}
      >
        {aendern.isPending ? texte.passwort.laeuft : texte.passwort.absenden}
      </Button>
    </form>
  );
}
