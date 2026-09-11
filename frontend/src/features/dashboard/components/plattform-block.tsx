"use client";

import { Server, type LucideIcon } from "lucide-react";

import { useTexte } from "@/i18n/provider";
import { ablagenVordergrund, dienstVordergrund } from "@/lib/status-farbe";
import { cn } from "@/lib/utils";

import type { Plattform } from "../api";
import {
  ABLAGENZEICHEN,
  DIENSTZEICHEN,
  lampenauskunft,
  sammelzeilenauskunft,
  traegtZielfarbe,
} from "../plattform";
import { Kachel, Kopf } from "./kachel";

/**
 * Die Kachel **Plattform** — je eine Zeile für jeden Dienst mit einer
 * Zeitgrenze, danach die geprüften Ablagen (`docs/dienste.md`,
 * `docs/dashboard-frontend.md` §5.8).
 *
 * > ### ⚠️ Umbau vom 10.09.2026 — aus dem Block ist die fünfte Kachel geworden
 * >
 * > **Der Befund der Durchsicht:** Der Block stand als eigener Kasten unter der
 * > Kachelreihe, war so hoch wie der Verlauf und schob alles Wichtige nach
 * > unten. E‑126 wollte ihn ohne Scrollen erreichbar haben — erreicht hat er
 * > das, indem er den **Verlauf** unter die Falz schob. Die Auskunft über die
 * > Anlage ist die *seltenere* Frage; sie darf den Platz der häufigeren nicht
 * > nehmen.
 * >
 * > **Jetzt ist sie die fünfte Kachel:** *Fehler · Läuft · Wartend ·
 * > Nachrichten · **Plattform***. Sie steht damit weiterhin oben und ohne
 * > Scrollen, kostet aber keine eigene Zeile mehr — und unter der Reihe folgt
 * > wieder direkt der Verlauf.
 * >
 * > | | bis 10.09.2026 | jetzt |
 * > |---|---|---|
 * > | Ort | eigener Kasten unter der Reihe | **fünfte Kachel der Reihe** |
 * > | je Dienst | Plakette mit Wort, Kennung, Zeitpunkt, Alter | **Zeichen und Kennung** |
 * > | Zustand als Wort | sichtbar in der Plakette | **`title` und `sr-only`** (E‑91) |
 * > | Zeiten | „zuletzt geändert", Alter, Prüfzeitpunkt | **keine** |
 * > | Grund | sichtbarer Satz | **`title` und `sr-only`** |
 * > | Satz unter der Überschrift | „für jeden Mandanten gleich …" | **keiner** |
 *
 * ## Die Bauform ist die von E‑91 und nicht die der Plakette
 *
 * **Das Zeichen trägt die Vordergrundfarbe der Rolle, sonst nichts** — keine
 * Plakette, keine Fläche, keine Kontur. **E‑79 bleibt unberührt:** Gefüllt ist
 * allein die Fehlerkachel. Das ist dieselbe Abwägung, die „Zuletzt aufgefallen"
 * schon trägt: Ein Zeichen kostet die Zeile nichts an Breite, ein Wort kostete
 * sie — und die Breite gehört hier der **Kennung**, dem Einzigen, was den
 * Dienst benennt (E‑122).
 *
 * **Das Wort ist damit nicht verschwunden**, es steht im `title` und im
 * Vorlese-Markup. `docs/visuelles-konzept.md` §3 verlangt *„zusätzlich eine
 * Beschriftung **oder** ein Zeichen"* — und die vier Zeichen unterscheiden sich
 * in der **Form**: offener Strich, Dreieck, Viereck, Kreis (`../plattform.ts`).
 *
 * ## Zwei Spalten, damit die Kachel nicht höher wird als ihre Nachbarn
 *
 * Sieben Dienste und eine Ablagenzeile untereinander wären acht Zeilen und
 * damit höher als jede andere Kachel der Reihe. **Der Umbruch entscheidet die
 * Breite und nicht ein Haltepunkt des Fensters:** `auto-fit` legt zwei Spalten
 * an, sobald beide vollständige Kennungen tragen können, und sonst eine. Eine
 * feste Zweispaltigkeit schnitte bei schmaler Kachel `HTTPSERVICEPROD00` auf
 * `HTTPSERVICEPR…` zusammen — und die Ziffern am Ende sind genau das, was die
 * sieben Kennungen unterscheidet.
 *
 * **Kleiner gesetzt wird nichts.** Die Schrift ist `text-beiwerk` wie in den
 * übrigen Kacheln; eine eigene, kleinere Größe wäre eine Ausnahme im
 * Schriftsystem für eine Kachel (`docs/visuelles-konzept.md` §4).
 *
 * ## Sie ist nicht klickbar, und sie lädt nichts nach
 *
 * Es gibt keine Dienstansicht, und `plattform` kommt im selben Aufruf wie alles
 * andere (§7.1, §7.3 nachgemessen). Die Kachel bekommt den Block als
 * Eigenschaft und stellt keine eigene Anfrage.
 */
export function PlattformKachel({ plattform }: { plattform: Plattform }) {
  const texte = useTexte();

  return (
    <Kachel>
      <Kopf zeichen={Server} titel={texte.dashboard.plattform.titel} />

      {/*
       * **Entscheidung E‑135: ein sichtbarer Satz statt einer leeren Stelle.**
       * Eine leere Liste sähe aus wie ein Fehler im Bau — dieselbe Begründung
       * wie bei E‑81: Abwesenheit ist der schwächste Kanal, den eine Auskunft
       * haben kann. **Die Ablagenzeilen stehen darunter trotzdem**; die Kachel
       * zieht sich nicht auf die halbe Auskunft zusammen.
       */}
      {plattform.dienste.length === 0 ? (
        <p className="text-muted-foreground text-beiwerk">{texte.dashboard.plattform.dienstLeer}</p>
      ) : null}

      <Zeilen plattform={plattform} />
    </Kachel>
  );
}

/**
 * Dienste und Ablagen in **einer** Liste.
 *
 * **Nicht zwei Listen nebeneinander.** Zwei `<ul>` brächten zwei Umbrüche, und
 * die Ablagenzeile stünde bei sieben Diensten allein in einer neunten Zeile —
 * genau die Höhe, die dieser Umbau loswerden wollte. Der Schlüssel trägt die
 * **Art voran** (`dienst:` / `ablage:`), damit dieselbe Kennung in beiden
 * Mengen keinen doppelten React-Schlüssel ergibt — dieselbe Lehre wie bei den
 * Marken der Suche und den BAM-Werten.
 *
 * **Die Reihenfolge ist die der Antwort** (E‑130): Das Backend liefert nach
 * `ServiceID` sortiert und ausdrücklich nicht nach Zustand. Sortierte die
 * Ansicht nach Zustand, spränge eine Zeile an eine andere Stelle, sobald sich
 * ihr Zustand ändert — und genau dann sucht jemand sie an ihrem alten Platz.
 */
function Zeilen({ plattform }: { plattform: Plattform }) {
  const texte = useTexte();
  const { ablagen } = plattform;

  const mitFarbe = traegtZielfarbe(ablagen.zustand, ablagen.grund);
  const sammelzeile = sammelzeilenauskunft(ablagen, texte);

  return (
    <ul className="text-beiwerk grid grid-cols-[repeat(auto-fit,minmax(10rem,1fr))] gap-x-3">
      {plattform.dienste.map((dienst) => (
        <Zeile
          key={`dienst:${dienst.serviceId}`}
          zeichen={DIENSTZEICHEN[dienst.zustand]}
          farbe={dienstVordergrund(dienst.zustand)}
          kennung={dienst.serviceId}
          auskunft={lampenauskunft(dienst, texte)}
        />
      ))}

      {ablagen.ziele.map((ziel) => (
        <Zeile
          key={`ablage:${ziel.serviceId}`}
          zeichen={ABLAGENZEICHEN[ziel.zustand]}
          /*
           * **Die Farbe trägt die Zielzeile nur, solange der Stand gilt**
           * (E‑133). Bei einem veralteten Stand steht hier das **letzte** Wort,
           * gedämpft und ohne Rolle — grün überlebt seinen Beleg auch in der
           * Anzeige nicht. Das Wort bleibt in beiden Fällen im `title`;
           * genommen wird ihr nur die Farbe, und die war ohnehin nie die ganze
           * Aussage.
           */
          farbe={mitFarbe ? ablagenVordergrund(ziel.zustand) : "text-muted-foreground"}
          kennung={ziel.serviceId}
          auskunft={texte.dashboard.plattform.ablage[ziel.zustand]}
        />
      ))}

      {sammelzeile === null ? null : (
        <Zeile
          zeichen={ABLAGENZEICHEN.UNGEKLAERT}
          farbe={ablagenVordergrund("UNGEKLAERT")}
          kennung={texte.dashboard.plattform.ablagen}
          auskunft={sammelzeile}
        />
      )}
    </ul>
  );
}

/**
 * Eine Zeile: **Zeichen und Kennung** — die Bauform aus **E‑91**.
 *
 * Das Zeichen ist `aria-hidden` und damit schmückend; die Auskunft daneben
 * steht einmal im `title` (für die Maus) und einmal in einer `sr-only`-Spanne
 * (für ein Vorleseprogramm). **Ein `title` allein genügte nicht** — auf einem
 * Berührungsgerät gibt es kein Überfahren (bekannte Grenze 3), und dort trägt
 * die Zeile ihre Auskunft über Farbe **und Form** des Zeichens.
 */
function Zeile({
  zeichen: Zeichen,
  farbe,
  kennung,
  auskunft,
}: {
  zeichen: LucideIcon;
  farbe: string;
  kennung: string;
  auskunft: string;
}) {
  return (
    <li className="flex min-w-0 items-center gap-1.5">
      <span className={cn("shrink-0", farbe)} title={auskunft}>
        <Zeichen aria-hidden="true" className="size-3.5" />
        <span className="sr-only">{auskunft}</span>
      </span>
      <span className="min-w-0 flex-1 truncate">{kennung}</span>
    </li>
  );
}
