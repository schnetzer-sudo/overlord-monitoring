"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";

import { useTexte } from "@/i18n/provider";
import { ROUTEN, ansichtNebenListe } from "@/lib/routen";

import { useEscapeSchliesst } from "../hooks";
import { NachrichtDetail } from "./nachricht-detail";

/**
 * Die Detailansicht als **eigene Seite** — `/nachrichten/<id>`.
 *
 * Derselbe Baustein wie im Panel; hier nur ohne die Liste daneben. Die Route
 * existiert, weil ein Link auf einen Beleg die eigentliche Anwendung ist
 * („schick mir mal den Link") und weil die BAM-Suche in Schritt 7 einen Einstieg
 * ohne Liste braucht.
 *
 * ## Schließen führt zurück zur Liste
 *
 * Und zwar **mit dem Filterzustand, der in der URL steht**. Der wird nicht
 * nebenher gemerkt: Was die URL nicht ausdrückt, existiert nicht
 * (`frontend-grundlagen.md` §8). Trägt der geöffnete Link Filter — etwa weil er
 * aus einer geteilten Ansicht stammt —, kommen sie mit; trägt er keine, gilt auf
 * der Liste wieder das Standardfenster des Servers.
 *
 * Gelesen wird die Abfragezeichenkette **im Ereignis** aus `window.location`
 * und nicht über `useSearchParams`. Der Hook zwingt die Seite unter eine
 * Suspense-Grenze; gebraucht wird der Wert aber erst beim Klick, und dort ist
 * der Browser ohnehin da.
 *
 * ## `Escape` wirkt hier wie der Schließen-Knopf
 *
 * Nachgetragen am 10.08.2026. Bis dahin trug nur das Panel die Taste, und die
 * offene Notiz dazu lautete, auf der Route sei der Schließen-Knopf ohnehin der
 * erste Tabstopp — ein Kürzel also entbehrlich. Das stimmt, macht die Taste aber
 * nicht falsch: Wer die Ansicht im Panel mit `Escape` schließt und denselben
 * Beleg später über einen geteilten Link öffnet, drückt dieselbe Taste und
 * erwartet dasselbe. Eine Taste, die je nach Einhängepunkt wirkt oder nicht,
 * lernt niemand.
 *
 * **Es ist derselbe Vorgang wie der Knopf**, mitsamt der Abfragezeichenkette —
 * nicht ein zweiter Weg mit eigenem Verhalten.
 */
export function NachrichtSeite({ messageId }: { messageId: string }) {
  const texte = useTexte();
  const router = useRouter();

  const zurueck = useCallback(() => {
    router.push(`${ROUTEN.nachrichten}${window.location.search}`);
  }, [router]);

  /**
   * Ein Glied der Kette öffnet sich **auf demselben Einhängepunkt** — dieselbe
   * Route mit der neuen Kennung, samt der Abfragezeichenkette, die schon in der
   * URL steht.
   *
   * Der Parameter `nachricht` wäre hier falsch: Er gehört zur Liste, und diese
   * Route hat keine. Es ist trotzdem derselbe Weg wie im Panel — die Ansicht
   * steht in der URL und bleibt teilbar —, und der Zurück-Knopf des Browsers
   * führt Glied für Glied zurück.
   */
  const oeffne = useCallback(
    (kennung: string) => {
      router.push(`${ROUTEN.nachrichten}/${encodeURIComponent(kennung)}${window.location.search}`);
    },
    [router],
  );

  /**
   * Zurück ans Panel — **derselbe Mechanismus wie beim Schließen**, nur mit
   * einem anderen Ziel: `window.location.search` im Ereignis gelesen, kein
   * `useSearchParams`, keine neue Suspense-Grenze.
   *
   * Der Preis ist, dass der Umschalter kein Mittelklick-Ziel ist und sich nicht
   * in einem neuen Tab öffnen lässt. Das wird bewusst getragen — Konsistenz mit
   * dem vorhandenen Weg wiegt hier schwerer, und die Ansicht ist über die
   * Adresszeile weiterhin vollständig erreichbar und teilbar.
   *
   * Die Kennung wird dabei als Parameter **gesetzt**, alles andere bleibt
   * unverändert und in seiner Reihenfolge stehen (`lib/routen.ts`).
   */
  const nebenListe = useCallback(() => {
    router.push(ansichtNebenListe(messageId, window.location.search));
  }, [router, messageId]);

  // Auf der eigenen Route ist die Ansicht immer offen — anders als im Panel, wo
  // der Parameter darüber entscheidet.
  useEscapeSchliesst(true, zurueck);

  return (
    // Eine Maximalbreite **innerhalb** der Ansicht, nie am Rahmen
    // (`visuelles-konzept.md` §5): Ohne sie liefe der Kopf über 1920 px
    // auseinander, und die Zeitleiste ist kein Datenraster, das Breite braucht.
    //
    // **Linksbündig, nicht zentriert** (kein `mx-auto`): Der Lesebeginn bleibt
    // an derselben x-Position wie Listenkopf und Panelkopf. Beim Umschalten
    // springt der Inhalt dadurch nicht seitwärts, sondern wird nur breiter.
    //
    // `beschriftung-breit` hebt `--dichte-beschriftung` von 10 auf 16 rem —
    // **der Deckel gehört zum Einhängepunkt und nicht zum Block**
    // (`globals.css`, `bam-werte.md` §11a). Die 10 rem sind für das Panel
    // gemessen; hier ist dieselbe Gruppenzeile gemessene 1.126 px breit statt
    // 454, und die Beschriftungen brachen um, ohne dass der Wert dadurch Platz
    // gewann. Der Belegdaten-Block lernt dabei **nicht**, wo er hängt: Er liest
    // den Wert wie bisher.
    <div className="max-w-inhalt beschriftung-breit flex flex-col gap-4">
      <NachrichtDetail
        messageId={messageId}
        aufSchliessen={zurueck}
        schliessenText={texte.nachrichten.detail.zurueckZurListe}
        aufOeffnen={oeffne}
        umschaltenZu="nebenListe"
        aufUmschalten={nebenListe}
      />
    </div>
  );
}
