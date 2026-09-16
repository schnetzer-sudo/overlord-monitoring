"use client";

import type { Ref } from "react";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

import type { BamTreffer, BamTrefferWert, Kettenrolle } from "../api";
import { trefferTypen } from "../suche";
import { TREFFER_SICHTBAR } from "../treffer-spalten";
import { AblaufZelle, ZeitpunktZelle } from "./nachrichten-tabelle";
import { StatusPlakette } from "./status-plakette";

/**
 * Die Trefferliste der Belegsuche.
 *
 * ## Dieselbe Zeilengestalt wie die Nachrichtenliste — und dieselben Zellen
 *
 * Zeitpunkt, Status und Ablauf sind hier keine Nachbauten, sondern **dieselben
 * Komponenten**: Der Endpunkt sagt zu, dass die zehn Listenfelder wortgleich mit
 * denen der Liste sind (`docs/bam-suche.md` §1), und zwei Nachbauten derselben
 * Zelle machten aus dieser Zusage mit der Zeit zwei Zeilengestalten. Wer aus der
 * Suche heraus weiterarbeitet, soll dieselbe Zeile sehen wie aus der Liste.
 *
 * Es gilt deshalb auch dieselbe Regel: **Jede Zelle ist eine Zeile hoch. Was
 * nicht hineinpasst, wird gekürzt; der Vollwert steht im `title`.**
 *
 * ## Zwei Spalten kommen dazu
 *
 * **„Treffer" zeigt den Typ, nicht den Wert** — die Begründung steht bei
 * {@link trefferTypen}. Die Beschriftungen sind lang und bleiben es: M45 misst,
 * dass die Endungen `_K_SAP`, `_L_SAP` und `_FORS` **unterscheiden** — ohne sie
 * fallen 62 Beschreibungen auf 57, und `Abladestelle_L_SAP` und
 * `Abladestelle_K_SAP` stehen über einen Monat auf 3.405 Nachrichten gemeinsam.
 * Gekürzt wird deshalb in der Zelle und nicht am Text.
 *
 * **„Kette" ist hier wichtiger als in der Liste.** Die Suche findet fast immer
 * die **Wurzel**: 96,87 Prozent der Wurzeln tragen BAM-Werte, nur 2,42 Prozent
 * der Kinder (M26‑1b). Und die Wurzel trägt bei einer Aufteilung einen
 * Endstatus, der die eigentliche Frage — *ist der Beleg beim Partner angekommen*
 * — gerade nicht beantwortet. Ohne diesen Hinweis hielte sich der Nutzer für
 * fertig. Er kostet **keinen** zusätzlichen Zugriff: Die vier
 * Verkettungsspalten stehen auf der Zeile, die die Abfrage ohnehin liest (E4).
 *
 * **Kein Sammelstatus über die Kette und keine Zählung der Folgenachrichten.**
 * Beides kostete je Zeile eine eigene Auflösung, und die Zahl stünde bei 87
 * Prozent der Wurzeln ohnehin auf Eins. Wer wissen will, was daran hängt, öffnet
 * die Nachricht.
 *
 * ## Der Ablauf kommt, wenn die Hülle ihn trägt (E‑147)
 *
 * Zeitpunkt, Status, Treffer und Kette stehen bei jeder Breite; der Ablauf kommt
 * dazu, sobald **die Hülle dieser Tabelle** — nicht das Fenster — die
 * Mindestbreiten aller fünf trägt: ab 1.000 px, ohne die Spalte „Treffer" ab
 * 720 px. Welche Spalte weicht, ist dieselbe Wahl wie vorher und folgt derselben
 * Frage: Was beantwortet *welcher Beleg ist das* und *bin ich fertig*? Der
 * Ablaufname beantwortet keines von beiden und steht im Detail vollständig da.
 *
 * **Jede feste Spalte trägt ihre gemessene Mindestbreite** (M177,
 * `../treffer-spalten.ts`). Vorher kam der Ablauf an der Fensterschwelle `md` mit
 * 0 px dazu und zeichnete seine Beschriftung trotzdem (Punkt 173). Unter der
 * Grundmenge — 696 px, ohne „Treffer" 416 px — scrollt die Tabelle in ihrer
 * Hülle, wie sie es vorher unter 768 px tat (`property-suche.md` §14, Punkt 11).
 * Herleitung und Zahlen: `docs/spaltenwahl.md`.
 *
 * ## Bei einer reinen Feldsuche entfällt die Spalte „Treffer" (E‑110)
 *
 * `treffer` je Zeile enthält ausschließlich BAM-Treffer; wurde keine Belegnummer
 * gesucht, ist sie in jeder Zeile leer — und das ist richtig so, der Feldtreffer
 * ist der getippte Wert selbst und steht als Marke über der Liste. Eine
 * Überschrift über fünfzig leeren Zellen wäre keine Auskunft, sondern ein
 * Rätsel; die Spalte fällt deshalb ganz weg, und der Ablauf bekommt ihre Breite.
 * **Entschieden wird das an der Frage und nicht an den Zellen**
 * (`suche.ts` `zeigtTrefferspalte`), damit die Tabelle nicht je nach Daten
 * ihre Gestalt wechselt.
 */
export function TrefferTabelle({
  zeilen,
  gewaehlt,
  aufAuswahl,
  mitTrefferspalte,
  gewaehlteZeile,
}: {
  zeilen: BamTreffer[];
  /** Die geöffnete Nachricht — sie kommt aus der URL, nicht aus dieser Tabelle. */
  gewaehlt: string | null;
  aufAuswahl: (messageId: string) => void;
  /** Ob eine Belegnummer gesucht wurde — nur dann gibt es etwas zu beschriften. */
  mitTrefferspalte: boolean;
  /**
   * Bekommt die **geöffnete** Zeile gereicht — für den Rückweg nach dem
   * Schließen des Panels (E‑114, `lib/in-sicht-bringen.ts`), wie in der
   * Nachrichtentabelle. Freiwillig, und die Tabelle tut selbst nichts damit.
   */
  gewaehlteZeile?: Ref<HTMLTableRowElement>;
}) {
  const texte = useTexte();

  // Die Schwelle des Ablaufs hängt an der Spalte „Treffer": Ohne sie trägt die
  // Hülle ihn früher.
  const ablauf = mitTrefferspalte
    ? TREFFER_SICHTBAR.ablaufMitTreffer
    : TREFFER_SICHTBAR.ablaufOhneTreffer;

  return (
    // Der Container ist die eigene Hülle — nicht `main` und nicht der Wrapper in
    // `components/ui/table.tsx`: Die Spaltenmenge folgt dem Platz, den diese
    // Tabelle hat, auch neben dem Panel (E‑147). Benannt, damit eine Tabelle in
    // einem anderen Container nie dessen Breite abfragt.
    <div className="@container/trefferliste">
      {/* `table-fixed` ist die Voraussetzung der festen Zeilenhöhe: Nur mit festen
          Spaltenbreiten hat eine Zelle eine Breite, auf die sich kürzen lässt. */}
      <Table className="text-basis table-fixed">
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            {/* Jede feste Spalte trägt ihre gemessene Mindestbreite (M177): Zeitpunkt
              187 px, Status 155 px — die Plakette, der Zusatz daneben kürzt —,
              Treffer 280 px — die längste Belegart-Bezeichnung, mit `+2` kürzt
              sie weiterhin —, Kette 74 px. */}
            <TableHead className="h-8 w-[11.6875rem]">
              {texte.nachrichten.spalten.zeitpunkt}
            </TableHead>
            <TableHead className="h-8 w-[9.6875rem]">{texte.nachrichten.spalten.status}</TableHead>
            {mitTrefferspalte ? (
              <TableHead className="h-8 w-[17.5rem]">{texte.suche.spalten.treffer}</TableHead>
            ) : null}
            <TableHead className="h-8 w-[4.625rem]">{texte.suche.spalten.kette}</TableHead>
            {/* Ohne Breitenangabe: Der Ablaufname bekommt, was übrig bleibt — an
              seiner Schwelle genau seine Mindestbreite. */}
            <TableHead className={cn("h-8", ablauf)}>{texte.nachrichten.spalten.ablauf}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {zeilen.map((zeile) => (
            <TableRow
              key={zeile.messageId}
              ref={zeile.messageId === gewaehlt ? gewaehlteZeile : undefined}
              tabIndex={0}
              aria-label={texte.nachrichten.zeileOeffnen}
              aria-current={zeile.messageId === gewaehlt ? "true" : undefined}
              onClick={(ereignis) => {
                if ((ereignis.target as HTMLElement).closest("a, button, input, label")) {
                  return;
                }
                aufAuswahl(zeile.messageId);
              }}
              onKeyDown={(ereignis) => {
                if (ereignis.target !== ereignis.currentTarget) {
                  return;
                }
                if (ereignis.key === "Enter" || ereignis.key === " ") {
                  ereignis.preventDefault();
                  aufAuswahl(zeile.messageId);
                }
              }}
              className={cn(
                "h-zeile focus-visible:ring-ring cursor-pointer focus-visible:ring-2 focus-visible:-outline-offset-2 focus-visible:outline-none",
                zeile.messageId === gewaehlt ? "bg-accent hover:bg-accent" : "hover:bg-muted",
              )}
            >
              <TableCell className="px-2 py-0 align-middle">
                <ZeitpunktZelle wert={zeile.zeitpunkt} />
              </TableCell>
              <TableCell className="px-2 py-0 align-middle">
                <StatusPlakette
                  statusKind={zeile.statusKind}
                  rohwert={zeile.status}
                  bedeutungNichtVerifiziert={zeile.bedeutungNichtVerifiziert}
                  schritt={zeile.schritt}
                />
              </TableCell>
              {mitTrefferspalte ? (
                <TableCell className="px-2 py-0 align-middle">
                  <TrefferZelle treffer={zeile.treffer} />
                </TableCell>
              ) : null}
              <TableCell className="px-2 py-0 align-middle">
                <KettenZelle rollen={zeile.rollen} />
              </TableCell>
              <TableCell className={cn("px-2 py-0 align-middle", ablauf)}>
                <AblaufZelle sosName={zeile.sosName} processName={zeile.processName} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

/**
 * **Worauf die Nummer getroffen hat** — der Typ, nie der Wert.
 *
 * Die Längenregel ist die der Liste: eine Zeile hoch, gekürzt, der vollständige
 * Text im `title`. Er nennt dort **alle** Typen und nicht nur den ersten; das
 * `+2` in der Zelle ist eine Zahl, kein Ersatz für die Auskunft.
 */
function TrefferZelle({ treffer }: { treffer: BamTrefferWert[] }) {
  const texte = useTexte();
  const typen = trefferTypen(treffer);

  if (typen === null) {
    return null;
  }

  const kurz =
    typen.weitere === 0
      ? typen.erste
      : einsetzen(texte.suche.treffer.weitere, { erste: typen.erste, anzahl: typen.weitere });
  const voll = einsetzen(texte.suche.treffer.alleTypen, { typen: typen.alle.join(", ") });

  return (
    <span className="block truncate" title={voll}>
      {kurz}
      {typen.weitere === 0 ? null : <span className="sr-only"> — {voll}</span>}
    </span>
  );
}

/**
 * **Die Stellung in der Verkettung** — und sonst nichts.
 *
 * Sie sagt, dass an dieser Nachricht etwas hängt, und behauptet nicht, *was*
 * daraus geworden ist. Der kurze Text steht in der Zelle, der ganze Satz im
 * `title`: Auf einem Touchgerät gibt es keinen Hover, und ein Wort allein wäre
 * dort eine Andeutung.
 *
 * **Ohne Rolle bleibt die Zelle leer.** Keine Kette ist keine fehlende Angabe —
 * ein „nicht zugeordnet" behauptete hier eine Lücke, wo keine ist.
 *
 * **Ohne eigene Farbrolle.** Eine Farbe wäre eine Aussage über *gut oder
 * schlecht*, und die macht die Stellung in der Kette nicht
 * (`docs/visuelles-konzept.md` §3).
 */
function KettenZelle({ rollen }: { rollen: Kettenrolle[] }) {
  const texte = useTexte();

  // Eine Rolle, die diese Fassung nicht kennt, wird übergangen statt als
  // „undefined" gezeigt — dieselbe Vorsicht wie bei einem unbekannten Statuswert
  // in der Plakette. Der Typ schließt den Fall aus, die Leitung nicht.
  const bekannt = rollen.filter((rolle) => rolle in texte.suche.kette.kurz);
  if (bekannt.length === 0) {
    return null;
  }

  const kurz = bekannt.map((rolle) => texte.suche.kette.kurz[rolle]).join(" · ");
  const voll = bekannt.map((rolle) => texte.suche.kette.satz[rolle]).join(" ");

  return (
    <span className="text-muted-foreground text-beiwerk block truncate" title={voll}>
      {kurz}
      <span className="sr-only"> — {voll}</span>
    </span>
  );
}
