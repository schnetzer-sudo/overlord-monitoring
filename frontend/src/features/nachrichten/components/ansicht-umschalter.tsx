"use client";

import { Maximize2, Minimize2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useTexte } from "@/i18n/provider";

/**
 * Wohin ein Klick führt — und damit zugleich, wo die Ansicht gerade steht.
 *
 * `ohneListe` steht im Panel und führt auf die eigene Route, `nebenListe` steht
 * auf der eigenen Route und führt zurück ans Panel.
 */
export type Umschaltziel = "ohneListe" | "nebenListe";

/**
 * Der Umschalter zwischen den **beiden bestehenden Einhängepunkten** der
 * Detailansicht.
 *
 * ## Warum es ihn gibt
 *
 * Das Panel hat eine feste Breite in `rem`, die Liste bekommt den Rest — dadurch
 * wird das Panel relativ **schmaler, je breiter das Fenster ist**: 26 rem von
 * 1072 px Inhaltsbreite sind 39 Prozent bei 1280 px, 30 rem von 1712 px nur noch
 * 28 Prozent bei 1920 px. Ein Klick auf eine Zeile setzt den Fokus ins Panel, der
 * optische Schwerpunkt bleibt aber auf der Liste — und am großen Monitor, wo das
 * Werkzeug betrieben wird, ist das Missverhältnis am größten.
 *
 * **Es entsteht dabei kein neuer Mechanismus und keine neue Route.** Beide Ziele
 * gibt es seit Schritt 5; gebaut ist nur der Weg dazwischen.
 *
 * ## Er tritt neben den Schließen-Knopf, nicht an seine Stelle
 *
 * Zwei Knöpfe, zwei verschiedene Aussagen: *diese Nachricht anders zeigen* gegen
 * *diese Nachricht schließen*. Der Schließen-Knopf bleibt in beiden Modi
 * unverändert.
 *
 * ## Unter `xl` erscheint er nicht
 *
 * Dort füllt die Detailansicht ohnehin die Stelle der Liste
 * (`nachrichtendetail.md` §10.7); ein Schalter, der nichts Sichtbares ändert,
 * verspricht etwas, das er nicht hält. Umgesetzt über die Klassen und **nicht**
 * über eine Abfrage der Fensterbreite in JavaScript — eine solche Abfrage wäre
 * ein zweiter Umbruchpunkt neben dem in `nachrichten-ansicht.tsx`, und zwei
 * Umbruchpunkte laufen auseinander.
 *
 * ## Beschriftung und Zeichen kommen aus demselben Wert
 *
 * Sie sind zwei Hälften derselben Aussage. Käme die Beschriftung von der
 * aufrufenden Seite und das Zeichen von hier, wäre das die Stelle, an der
 * irgendwann „Ohne Liste anzeigen" neben einem Verkleinern-Zeichen steht.
 *
 * @param aufUmschalten was der Klick bedeutet, entscheidet der Einhängepunkt —
 *   dieselbe Aufteilung wie beim Schließen und beim Öffnen eines Kettenglieds.
 */
export function AnsichtUmschalter({
  zu,
  aufUmschalten,
}: {
  zu: Umschaltziel;
  aufUmschalten: () => void;
}) {
  const texte = useTexte();

  const beschriftung =
    zu === "ohneListe"
      ? texte.nachrichten.detail.ansichtOhneListe
      : texte.nachrichten.detail.ansichtNebenListe;

  const Zeichen = zu === "ohneListe" ? Maximize2 : Minimize2;

  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      // `hidden xl:inline-flex` und nicht `xl:block`: Der Knopf bringt seine
      // Ausrichtung aus `inline-flex` mit, und `tailwind-merge` löst den
      // Widerspruch zwischen ihm und `hidden` zugunsten des Letzteren auf.
      className="min-h-bedienelement hidden shrink-0 xl:inline-flex"
      onClick={aufUmschalten}
      // `aria-label` **und** `title`: das eine für das Vorleseprogramm, das
      // andere für den Zeiger. Ein Icon-Knopf ohne beides ist für die eine oder
      // die andere Seite stumm.
      aria-label={beschriftung}
      title={beschriftung}
    >
      <Zeichen aria-hidden="true" />
    </Button>
  );
}
