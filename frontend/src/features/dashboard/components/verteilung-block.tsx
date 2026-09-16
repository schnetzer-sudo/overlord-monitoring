"use client";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";

import type { Verteilung, Verteilungssicht, Verteilungszeile } from "../api";
import { verteilungszeileText } from "../beschriftung";
import { SICHT_REIHE } from "../filter";

/**
 * Die Verteilung — **ein Block, zwei Sichten** (Partner ⇄ Richtung).
 *
 * ## Das Frontend rechnet hier nichts
 *
 * Der Endpunkt liefert die Zeilen **fertig sortiert und gebündelt**: Rang 1 bis
 * 10 absteigend, dann „Übrige", dann „nicht zugeordnet". Diese Komponente
 * schreibt sie in dieser Reihenfolge hin — sie sortiert nicht, sie summiert
 * nicht, und sie lässt keine weg.
 *
 * **Beide Restzeilen stehen deshalb immer unten**, unabhängig von ihrer Größe.
 * Bei `IBIS` ist „Übrige (40)" mit 27,92 % der größte Balken des Blocks; nach
 * Größe sortiert stünde sie auf Rang 1, als gäbe es einen Partner dieses Namens
 * (`docs/dashboard.md` §4, M98 Befund 21). Genau deshalb wird hier nicht
 * sortiert.
 *
 * ## Die beiden Restzeilen verhalten sich verschieden, und das ist der Punkt
 *
 * | Zeile | Wann sie erscheint |
 * |---|---|
 * | `UEBRIGE` | **nur, wenn der Endpunkt sie liefert.** Ohne Rang 11 gibt es sie nicht, und eine Null sagte dort nichts — sie wäre reines Rangartefakt |
 * | `NICHT_ZUGEORDNET` | **immer, auch bei null** |
 *
 * **Warum die Nullzeile bleibt:** Sie ist eine Aussage über den **Katalog**.
 * Null heißt *alles kuratiert*. Wird sie bei null ausgeblendet, ist
 * *vollständig gepflegt* nicht mehr von *diese Ansicht zeigt das nicht* zu
 * unterscheiden.
 *
 * Beide Regeln liegen im Backend und werden hier **nicht nachgebaut**: Diese
 * Komponente zeigt, was kommt. Der Test hält genau das fest — dass „Übrige"
 * fehlt, wenn sie nicht geliefert wird, und dass die Nullzeile stehen bleibt.
 *
 * ## Keine Zeile ist klickbar
 *
 * Die Nachrichtenliste kennt **keinen Partnerfilter**, und der Umweg über die
 * Prozess-IDs eines Partners ist in
 * [`nachrichtenliste.md`](../../../../docs/nachrichtenliste.md) §5a mit
 * **7.459 ms** gemessen. Ein Verweis, der sieben Sekunden kostet, ist keiner.
 *
 * **Angekündigt wird das seit dem 16.09.2026 nicht mehr.** Der Satz „diese
 * Zeilen klicken nicht" ist auf Wunsch des Auftraggebers entfallen
 * (`docs/dashboard-frontend.md` §5.5); an den Zeilen selbst ändert das nichts.
 *
 * ## Der Umschalter fragt nichts an *(seit dem 16.09.2026, E‑161)*
 *
 * **Die Antwort trägt beide Sichten**, und `sicht` sagt, welche zu sehen ist —
 * aus der URL, sonst `PARTNER` (`filter.ts`, `hervorgehobeneSicht`). Ein
 * Wechsel zeigt die andere Hälfte dessen, was schon da ist: keine Anfrage, kein
 * Ladezustand, kein Neuaufbau eines anderen Blocks.
 *
 * *Bis zu diesem Tag stand hier: „Ein Sichtwechsel ist ein neuer Aufruf
 * derselben Adresse mit anderem Parameter; die Antwort trägt beide Sichten nie
 * zugleich." Genau dieser Aufruf ließ die ganze Seite neu laden, und die
 * Schaltflächen trugen deshalb `gesperrt`, solange er lief. Beides ist
 * entfallen.*
 */
export function VerteilungBlock({
  verteilung,
  sicht,
  aufSicht,
}: {
  verteilung: Verteilung;
  sicht: Verteilungssicht;
  aufSicht: (sicht: Verteilungssicht) => void;
}) {
  const texte = useTexte();
  const zeilen = sicht === "RICHTUNG" ? verteilung.richtung.zeilen : verteilung.partner.zeilen;
  // Der Bezugswert der Balken ist der **größte** Wert des Blocks und nicht die
  // Summe: Gefragt ist der Vergleich der Zeilen untereinander, nicht ihr Anteil
  // am Ganzen — und „Übrige" wäre in einer Anteilsrechnung doppelt enthalten.
  const groesster = zeilen.reduce((groesst, zeile) => Math.max(groesst, zeile.anzahl), 0);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <h2 className="text-basis font-semibold">
          {sicht === "RICHTUNG"
            ? texte.dashboard.verteilung.titelRichtung
            : texte.dashboard.verteilung.titelPartner}
        </h2>
        <ToggleGroup
          type="single"
          variant="outline"
          aria-label={texte.dashboard.verteilung.bezeichnung}
          value={sicht}
          onValueChange={(wert) => {
            if (wert === "") {
              return;
            }
            aufSicht(wert as Verteilungssicht);
          }}
        >
          {SICHT_REIHE.map((knopf) => (
            <ToggleGroupItem key={knopf} value={knopf} className="min-h-bedienelement px-2.5">
              {knopf === "RICHTUNG"
                ? texte.dashboard.verteilung.richtung
                : texte.dashboard.verteilung.partner}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>

      <ul className="flex flex-col gap-1.5">
        {zeilen.map((zeile) => (
          <Zeile
            key={`${zeile.art}:${zeile.wert ?? ""}`}
            zeile={zeile}
            sicht={sicht}
            groesster={groesster}
          />
        ))}
      </ul>
    </div>
  );
}

/**
 * Eine Zeile: Beschriftung, Balken, Zahl.
 *
 * **Der Balken trägt keine Statusfarbe.** Eine Verteilung sagt nichts über *gut
 * oder schlecht*; eine Farbe dort wäre eine Aussage, die es nicht gibt
 * (`docs/visuelles-konzept.md` §3). Er nimmt die gedämpfte Fläche der
 * Anwendung.
 *
 * **Die Restzeilen sind gedämpft beschriftet**, damit ein Partnername und eine
 * Sammelzeile nicht wie zwei Partner aussehen — und „nicht zugeordnet" trägt
 * einen Satz dazu, weil eine Null dort sonst wie ein Fehler aussieht.
 */
function Zeile({
  zeile,
  sicht,
  groesster,
}: {
  zeile: Verteilungszeile;
  sicht: Verteilungssicht;
  groesster: number;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const rest = zeile.art !== "WERT";
  const anteil = groesster === 0 ? 0 : (zeile.anzahl / groesster) * 100;

  return (
    <li className="grid grid-cols-[minmax(6rem,10rem)_1fr_auto] items-center gap-x-3">
      <span
        className={rest ? "text-muted-foreground truncate italic" : "truncate"}
        title={
          zeile.art === "NICHT_ZUGEORDNET"
            ? texte.dashboard.verteilung.nichtZugeordnetHinweis
            : (zeile.wert ?? undefined)
        }
      >
        {verteilungszeileText(zeile, sicht, texte)}
      </span>
      <span aria-hidden="true" className="bg-muted h-2 rounded-xs">
        <span
          className="bg-muted-foreground/40 block h-2 rounded-xs"
          style={{ width: `${anteil}%` }}
        />
      </span>
      <span className="text-beiwerk tabular-nums">{formatiereZahl(zeile.anzahl, sprache)}</span>
    </li>
  );
}
