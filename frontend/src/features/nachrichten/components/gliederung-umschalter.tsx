"use client";

import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { useTexte } from "@/i18n/provider";
import type { Baumgliederung } from "@/lib/baumgliederung";

/**
 * Die Gliederung des Prozessbaums als **Schalter**: aus heißt Partner, an heißt
 * Projekt (`docs/process-view.md` §48).
 *
 * ## Seit dem 15.09.2026 ein Schalter und keine zwei Knöpfe
 *
 * Zuerst stand hier eine `ToggleGroup` über die Spaltenbreite, als eigene Zeile
 * über der Eingrenzung. Die Zeile kostete im klebenden Kopf Höhe, die dem Baum
 * fehlt. Jetzt steht der Schalter **neben „Nur mit Daten"** und teilt sich
 * dessen Zeile; bei 208 px Spaltenbreite (768 px) bricht er darunter um.
 *
 * **Die Grenze der Bauform ist benannt:** Ein Schalter kennt aus und an. Er
 * trägt, solange es genau zwei Gliederungen gibt; eine dritte bräuchte wieder
 * eine Auswahl.
 *
 * ## E‑146 gilt weiter
 *
 * Er steht in der Baumspalte und nicht neben dem Zeitraum: Der Zeitraum gilt
 * für beide Spalten (E‑50), die Gliederung nur für den Baum, und unter `md`
 * verschwindet er mit der Spalte.
 *
 * ## An ist, was gilt — nicht, was in der URL steht
 *
 * Fehlt der Parameter, zeigt der Schalter die Gliederung der Antwort — die
 * Vorgabe des Kontos (E‑143) — und schreibt sie **nicht** zurück. Ohne jede
 * Angabe steht er auf aus; in der Ansicht kommt das nicht vor, weil die
 * Baumspalte erst mit der Antwort erscheint.
 */
export function GliederungUmschalter({
  gewaehlt,
  aufAuswahl,
  gesperrt = false,
}: {
  /** Die Gliederung, die gilt — aus der URL oder aus der Antwort. */
  gewaehlt: Baumgliederung | null;
  aufAuswahl: (gliederung: Baumgliederung) => void;
  gesperrt?: boolean;
}) {
  const texte = useTexte();

  return (
    <div className="flex items-center gap-2">
      <Switch
        id="baum-gliederung"
        checked={gewaehlt === "PROJEKT"}
        onCheckedChange={(an) => aufAuswahl(an ? "PROJEKT" : "PARTNER")}
        disabled={gesperrt}
        aria-describedby="baum-gliederung-hinweis"
      />
      {/*
       * Dieselbe Berührungsfläche wie bei „Nur mit Daten": Die Beschriftung
       * trägt sie (`min-h-beruehrung`) und schaltet über `htmlFor` mit — der
       * Baustein selbst bleibt unter 44 px.
       */}
      <Label
        htmlFor="baum-gliederung"
        className="min-h-beruehrung flex cursor-pointer items-center font-normal"
      >
        {texte.prozesse.gliederung.schalter}
      </Label>
      <span id="baum-gliederung-hinweis" className="sr-only">
        {texte.prozesse.gliederung.hinweis}
      </span>
    </div>
  );
}
