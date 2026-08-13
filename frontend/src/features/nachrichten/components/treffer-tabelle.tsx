"use client";

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
 * ## Am schmalen Fenster fällt der Ablauf weg
 *
 * Unter dem vorhandenen Umbruchpunkt des Projekts (768 px) bleiben Zeitpunkt,
 * Status, Treffer und Kette. Das ist eine andere Wahl als in der Liste, die dort
 * das **Projekt** weglässt — und sie folgt derselben Frage: Was beantwortet
 * *welcher Beleg ist das* und *bin ich fertig*? Der Ablaufname beantwortet
 * keines von beiden und steht im Detail vollständig da. **Kein neuer
 * Umbruchpunkt.**
 */
export function TrefferTabelle({
  zeilen,
  gewaehlt,
  aufAuswahl,
}: {
  zeilen: BamTreffer[];
  /** Die geöffnete Nachricht — sie kommt aus der URL, nicht aus dieser Tabelle. */
  gewaehlt: string | null;
  aufAuswahl: (messageId: string) => void;
}) {
  const texte = useTexte();

  return (
    // `table-fixed` ist die Voraussetzung der festen Zeilenhöhe: Nur mit festen
    // Spaltenbreiten hat eine Zelle eine Breite, auf die sich kürzen lässt.
    <Table className="text-basis table-fixed">
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead className="h-8 w-[11.5rem]">{texte.nachrichten.spalten.zeitpunkt}</TableHead>
          <TableHead className="h-8 w-[10.5rem] lg:w-[15rem]">
            {texte.nachrichten.spalten.status}
          </TableHead>
          {/* Breiter als die übrigen Zusatzspalten: Die längste gemessene
              Beschreibung hat 35 Zeichen, mit einem `+2` dahinter 38. Sie kürzt
              trotzdem — die Hauptinformation der Zeile weicht dafür nicht. */}
          <TableHead className="h-8 w-[13rem] lg:w-[16rem]">
            {texte.suche.spalten.treffer}
          </TableHead>
          <TableHead className="h-8 w-[8.5rem]">{texte.suche.spalten.kette}</TableHead>
          {/* Ohne Breitenangabe: Der Ablaufname bekommt, was übrig bleibt. */}
          <TableHead className="hidden h-8 md:table-cell">
            {texte.nachrichten.spalten.ablauf}
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {zeilen.map((zeile) => (
          <TableRow
            key={zeile.messageId}
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
            <TableCell className="px-2 py-0 align-middle">
              <TrefferZelle treffer={zeile.treffer} />
            </TableCell>
            <TableCell className="px-2 py-0 align-middle">
              <KettenZelle rollen={zeile.rollen} />
            </TableCell>
            <TableCell className="hidden px-2 py-0 align-middle md:table-cell">
              <AblaufZelle sosName={zeile.sosName} processName={zeile.processName} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
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
