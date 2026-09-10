/**
 * **Die klebende Spalte** — E‑115, die benannte Ausnahme von
 * `docs/frontend-grundlagen.md` §7, Bedingung 2.
 *
 * ## Warum es sie gibt
 *
 * E‑114 holt, was neu erscheint, ins Bild, indem es **`main` bewegt** — und in
 * `main` stehen auch Baum und Liste. Wer weit unten eine Zeile öffnet, findet
 * das Panel im Bild und seine Stelle in der Liste nicht mehr wieder
 * (`docs/process-view.md` §47, Anlass). **Umgedreht:** Was neu erscheint,
 * klebt am oberen Rand des sichtbaren Bereichs von `main` und scrollt für
 * sich; Baum und Liste bleiben stehen, ein Klick bewegt sie nicht.
 *
 * E‑114 hat `sticky` erwogen und **verworfen**, weil Liste und Panel höher sind
 * als der Scrollbereich (1.813 px Liste, bis 2.321 px Panel bei 1.032 px
 * `main`, M172): Der untere Teil wäre unerreichbar gewesen. **Der eigene
 * Scrollbereich nimmt genau diesen Grund weg** — und nur ihn; alles andere an
 * §7 bleibt, wie es gemessen ist.
 *
 * ## Die fünf Eigenschaften, und warum jede einzelne dasteht
 *
 * | Klasse | wofür |
 * |---|---|
 * | `sticky top-0` | klebt am oberen Rand des sichtbaren Bereichs von `main` |
 * | `relative` (nur, wo nichts klebt) | **§7, Bedingung 3: Jeder Scrollbereich ist zugleich Bezugspunkt.** Ohne die Angabe hängt ein absolut positioniertes Kind — Tailwinds `sr-only` ist `position: absolute` — am nächsten positionierten Vorfahren *oberhalb* des Kastens, wird von ihm **nicht** beschnitten, und sein Platz zählt zur Scrollfläche darüber. In M173 vor der Korrektur gemessen: `main.scrollHeight` **1.859** statt 1.077 px, verursacht von der `sr-only`-Beschriftung „Jetzt aktualisieren" des Blätter-Blocks, die 811 px tief in der gescrollten Liste stand. **Die klebende Spalte selbst braucht die Angabe nicht** — `position: sticky` ist bereits ein positionierter Wert und damit Bezugspunkt; ein zweites `position` daneben wäre ein Wettlauf um die Reihenfolge im erzeugten CSS. Sie steht deshalb nur an den Kästen **im Rahmen**, die nicht kleben |
 * | `self-start` | **ein gestrecktes Flex-Kind ist so hoch wie seine Zeile** und hat damit keinen Weg zum Kleben; die Zeilen tragen zwar `items-start`, die Angabe steht hier trotzdem, damit die Spalte für sich vollständig ist |
 * | `max-h-[100cqh]` | höchstens so hoch wie der sichtbare Bereich — **an `main` bemessen und nie am Fenster** (unten) |
 * | `overflow-y-auto` | darüber hinaus scrollt sie für sich |
 * | `overscroll-contain` | gibt das Scrollen an ihrem Ende **nicht** an `main` weiter; sonst bewegte sich die Liste doch |
 *
 * ## `top-0` und nicht `-top-4` — gemessen, nicht gerechnet
 *
 * `main` trägt `py-4`. In einem Scrollbereich mit Innenabstand klebt `top: 0`
 * an dessen **Inhaltskante**, nicht an der Außenkante: In der Vorprobe zu V1
 * (kopfloses Chrome 152, gestellter Aufbau — `main` 1.032 px, Innenabstand
 * 16 px) stand die Spalte mit `top: 0` bei **16 px** und zwar bei
 * `scrollTop` 0, 400 und am Anschlag gleichermaßen; mit `-top-4` bei **0 px**,
 * und sie sprang beim Ankleben um 16 px hoch. Zusammen mit `max-h-[100cqh]`
 * (= 1.000 px, die **Inhaltshöhe**) steht sie damit oben und unten je 16 px vom
 * Rand — dieselbe Luft wie überall sonst auf der Seite.
 *
 * Die klebenden Tabellenköpfe von Katalog und Benutzerverwaltung nehmen
 * deshalb `-top-4` und nicht dasselbe: Sie **sollen** die Außenkante erreichen,
 * damit über ihnen keine Zeile durchscheint.
 *
 * ## Warum `cqh` und nicht `dvh`
 *
 * Die Höhe bemisst sich an `main` und **nie am Fenster** — dazwischen liegen
 * Kopfzeile und Innenabstand, und die Kopfzeile wächst mit der Dichtestufe. Der
 * Rahmen erklärt `main` deshalb zum Größencontainer (`container-type: size`,
 * `components/anwendungsrahmen.tsx`), und `100cqh` ist dessen Inhaltshöhe.
 *
 * ⚠️ **Ohne diesen Größencontainer fiele `cqh` still auf das kleine
 * Sichtfenster zurück** — also auf eine Höhe am Fenster, und damit auf genau
 * das, was §7 verbietet. Die Angabe am Rahmen ist deshalb kein Beiwerk; sie ist
 * die Hälfte dieser Datei. Gemessen ist der Zusammenhang in V1
 * (`docs/process-view.md` §47).
 *
 * ## Wo sie gilt — und die drei Stellen sind abschließend
 *
 * | Ansicht | Zustand | klebende Spalte | ab |
 * |---|---|---|---|
 * | `/nachrichten`, `/suche` | Panel offen | Panelhülle | `xl` |
 * | `/prozesse` | keine Nachricht offen | rechte Spalte (Leerzustand oder Kopf und Liste) | `md` |
 * | `/prozesse` | Nachricht offen | Liste und Panelhülle im Rahmen, jede für sich | `xl` |
 *
 * **Eine vierte Stelle ist ein Signal und keine Kleinigkeit.** Jede weitere
 * klebende Spalte ist ein weiterer Scrollbereich unter `main`, und die Zahl
 * dieser Bereiche ist die Zahl, die §7 seit dem 06.08.2026 führt.
 */

/**
 * Die klebende Spalte, wie sie **für sich** steht: klebt in `main` und scrollt
 * selbst. So hängt das Nachrichtenpanel auf `/nachrichten` und auf `/suche`
 * (Beleg- wie Eigenschaftssuche) — ab `xl`, wo es neben der Liste steht.
 */
export const KLEBENDE_SPALTE_AB_XL =
  "xl:sticky xl:top-0 xl:self-start xl:max-h-[100cqh] xl:overflow-y-auto xl:overscroll-contain";

/**
 * Dieselbe Spalte als **Rahmen**: Sie klebt und ist höchstens so hoch wie der
 * sichtbare Bereich, scrollt aber nicht selbst — das tun ihre Kinder, jedes für
 * sich ({@link IM_RAHMEN_SCROLLT_AB_MD}, {@link IM_RAHMEN_SCROLLT_AB_XL}).
 *
 * **Der Rahmen ist die Prozessansicht, und er hat einen gemessenen Grund.** Dort
 * wechselt beim Öffnen einer Nachricht die Aufteilung: vorher Kopf und Liste,
 * nachher Kopf, Liste und Panel. Klebte in beiden Zuständen ein *anderes*
 * Element, wechselte damit auch das Element, das die Liste scrollt — und ein
 * Scrollstand springt nicht von einem Element auf ein anderes: Die Liste stünde
 * nach dem Klick wieder am Anfang, also genau da, wo die Meldung vom 10.09.2026
 * sie nicht mehr haben will. Mit dem Rahmen ist die Liste in **beiden**
 * Zuständen dasselbe scrollende Element, und ihr Stand bleibt von selbst
 * stehen; der Kopf des Prozesses bleibt nebenbei auch bei offenem Panel
 * sichtbar.
 *
 * `flex flex-col`, damit die Kinder unter dem Deckel schrumpfen können statt
 * ihn zu sprengen; jedes Kind auf dem Weg dorthin braucht `min-h-0`.
 */
export const KLEBENDER_RAHMEN_AB_MD =
  "md:sticky md:top-0 md:self-start md:max-h-[100cqh] md:flex md:flex-col";

/**
 * Der Rahmen erst ab `xl` — der Zustand mit offener Nachricht.
 *
 * **Warum nicht auch hier ab `md`:** Zwischen `md` und `xl` weichen bei offener
 * Nachricht Baum *und* Liste (E‑57, §15); es steht nichts nebeneinander, das
 * Panel füllt die Ansicht, und dort gilt E‑114 unverändert weiter. Ein Rahmen
 * wäre in diesem Zustand ein Scrollbereich ohne Anlass.
 */
export const KLEBENDER_RAHMEN_AB_XL =
  "xl:sticky xl:top-0 xl:self-start xl:max-h-[100cqh] xl:flex xl:flex-col";

/**
 * Was in einem klebenden Rahmen **für sich** scrollt, ab `md` — die
 * Übertragungsliste der Prozessansicht. Sie trägt es in *beiden* Zuständen,
 * auch bei offenem Panel: Genau das hält ihren Scrollstand über das Öffnen
 * hinweg (siehe {@link KLEBENDER_RAHMEN_AB_MD}).
 */
export const IM_RAHMEN_SCROLLT_AB_MD =
  "md:relative md:min-h-0 md:overflow-y-auto md:overscroll-contain";

/**
 * Was in einem klebenden Rahmen für sich scrollt, ab `xl` — das Panel der
 * Prozessansicht. Darunter steht es nicht neben der Liste, sondern an ihrer
 * Stelle, und dort gilt E‑114 unverändert.
 */
export const IM_RAHMEN_SCROLLT_AB_XL =
  "xl:relative xl:min-h-0 xl:overflow-y-auto xl:overscroll-contain";
