# Die Darstellungswahl der Dateiansicht — Original, sechs Formate, Hex

Entstanden am 17.09.2026 als Nachtrag zu Schritt 8. Beschreibt **warum** die Dateiansicht eine
Auswahl „Darstellung" bekommt, was jede Darstellung tut und — wichtiger — was sie nicht tut, wo
die Regeln liegen und was davon gemessen ist.

Die Dateiansicht selbst steht in [`rohdaten-frontend.md`](rohdaten-frontend.md) §4; die
Vorgabe des Rohdatenzugriffs in [`rohdaten.md`](rohdaten.md). Beide tragen seit diesem Tag
datierte Korrekturblöcke, weil diese Datei eine Zeile aufhebt, die dort seit dem 14.08.2026 stand.

*Ergänzt am 18.09.2026 um die Sichtprüfung (Teil D, Messung **M187**, §11): alle acht Prüfschritte
aus Punkt 201 bestanden, an echten Dateien aller sechs Formate; fünf Befunde, offene Punkte **204**
bis **208**. Am selben Tag behoben: **205** und **206** — der Download-Vermerk hängt an „Ergebnis ≠
Original" (**E‑206**), die Auswahl ist `w-48` breit (**E‑207**); nachgeprüft in M187‑7.*

---

## 1. Zweck

Die Ansicht `/nachrichten/{messageId}/dateien/{artefaktId}` zeigt Nutzdateien als Rohtext — die
Datei als Text, unverändert, in Festbreitenschrift, als ein Textknoten. Für eine
EDIFACT-Übertragung heißt das regelmäßig **eine Zeile über die ganze Datei** (36 von 206
Artefakten tragen kein Zeilenende, M61), und wer darin einen Beleg sucht, liest ein Segment nach
dem anderen ohne Anhaltspunkt, wo eines endet.

Die Auswahl **Darstellung** macht die Struktur sichtbar: Zeilenumbrüche und Einrückung, sonst
nichts. Acht Einträge — Original · EDIFACT, ANSI X12, VDA, IDoc, XML, JSON · Hex. Beim Öffnen steht
immer das Original; ein erkanntes Format ist in der Auswahl vorgemerkt und wird erst angewandt,
wenn der Nutzer es wählt.

**Formatieren heißt hier Struktur sichtbar machen. Inhalte zu erklären gehört nicht dazu** —
keine Segment-, Element- oder Feldbeschreibungen, kein Verzeichnis von EDIFACT, X12, VDA oder IDoc,
keine Syntaxhervorhebung, keine Farbe im Inhalt.

> **Entschieden vom Auftraggeber am 17.09.2026.** Damit fällt der Ausschluss der aufbereiteten
> Anzeige aus [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §9 für die **strukturelle**
> Formatierung. Der Widerspruch zu §9 dort, zu [`rohdaten.md`](rohdaten.md) §7 und zu
> [`rohdaten-frontend.md`](rohdaten-frontend.md) §4 ist gewollt und mit datierten
> Korrekturblöcken aufgelöst (§8). Die Zerlegung in Segmente mit Beschreibung bleibt
> ausgeschlossen.

---

## 2. Die Entscheidungen mit Datum

Die E‑Nummern schließen an [`live-rest.md`](live-rest.md) an: Über alle Dateien in `docs/` und die
Markdown-Dateien im Wurzelverzeichnis ist **E‑192** die höchste vergebene Nummer; **E‑780** ist
der bekannte Falschtreffer aus [`messungen-property-suche.md`](messungen-property-suche.md), keine
Vergabe. Die Buchstabenreihe `E‑a` bis `E‑z` ist aufgebraucht ([`dichte-umschalter.md`](dichte-umschalter.md)
§2). Vergeben ist deshalb ab **E‑193**. E‑193 bis E‑203 sind die elf Entscheidungen des
Auftraggebers aus dem Auftrag; E‑204 und E‑205 sind beim Bau gefallen und als solche gekennzeichnet.
*E‑206 und E‑207 hat der Auftraggeber am 18.09.2026 nach der Sichtprüfung getroffen (§11); die
Suche nach E‑206 bis E‑209 über `docs/`, die Markdown-Dateien im Wurzelverzeichnis, alle lokalen
Zweige und beide weiteren Arbeitsbäume war leer, geeicht an E‑192 auf `main` und E‑205 hier.*

| | Entscheidung | Datum |
|---|---|---|
| **E‑193** | **Acht Einträge in dieser Reihenfolge:** Original · EDIFACT, ANSI X12, VDA, IDoc, XML, JSON · Hex. Keine weiteren. EANCOM, ODETTE und die EDIFACT-basierten VDA-Empfehlungen *sind* EDIFACT; „VDA" meint die Festlängenformate | 17.09.2026 |
| **E‑194** | **Nur Nutzdaten im Zustand `ANZEIGBAR`.** Bei Protokollen und in den fünf übrigen Zuständen steht keine Auswahl im Baum. *Abgeleitet, nicht entschieden:* auch nicht bei der leeren Datei — sie erscheint als benannter Zustand, nicht als Inhalt (§4) | 17.09.2026 |
| **E‑195** | **Beim Öffnen immer Original.** Das erkannte Format trägt in der Auswahl den Zusatz „(erkannt)". Angewandt wird nichts ohne Wahl des Nutzers | 17.09.2026 |
| **E‑196** | **Reine Funktionen im Frontend.** Kein Backendaufruf, kein Endpunkt, kein Feld, kein Audit-Ereignis. Das Ergebnis bleibt genau ein Textknoten im `<pre>`, mit `pre-wrap` wie bisher | 17.09.2026 |
| **E‑197** | **Nur Leerraum.** Eine Darstellung darf Zeilenumbrüche einfügen oder entfernen und Einrückung einfügen, ausschließlich außerhalb von Daten. Jedes andere Zeichen bleibt, wie und wo es ist — je Darstellung als Eigenschaft getestet (§6) | 17.09.2026 |
| **E‑198** | **Tokenbasiert statt parsen und neu schreiben.** `JSON.stringify(JSON.parse(…))` macht aus `12.50` die `12.5`, verliert Stellen langer Nummern und verwirft doppelte Schlüssel; `DOMParser` mit `XMLSerializer` verliert die XML-Deklaration und löst Zeichenreferenzen auf. Beides ist für die Ausgabe ausgeschlossen — `JSON.parse` kommt allein in Tests als Gegenprobe vor | 17.09.2026 |
| **E‑199** | **Erkennung nur über feste Syntaxmerkmale** (§3), höchstens ein Treffer. Trifft keines zu, ist nichts vorgemerkt. Eine Prüfung wie die Binärprüfung, kein Erraten (Regel Q4) | 17.09.2026 |
| **E‑200** | **Passt die Datei nicht zur gewählten Darstellung** — auch bei gekappter Anzeige —, wird das Original angezeigt, mit benanntem Vermerk. Nie ein halb formatiertes Ergebnis | 17.09.2026 |
| **E‑201** | **Der Download bleibt die Originaldatei.** Solange eine Darstellung angewandt ist, sagt ein Vermerk, dass der Download das Original liefert. Das ist die **dritte** benannte Stelle, an der Anzeige und Download auseinandergehen dürfen — neben Kappung und Binärdatei ([`rohdaten-backend.md`](rohdaten-backend.md) §7) | 17.09.2026 |
| **E‑202** | **Adresse:** Parameter `darstellung` über nuqs, Werte `original`, `edifact`, `x12`, `vda`, `idoc`, `xml`, `json`, `hex`, Standard `original`, **bewusst ohne `clearOnDefault: false`** (§4). Unbekannte Werte ergeben Original. Verweise in die Ansicht bleiben ohne Abfragezeichenkette. Wo keine Auswahl steht, ist der Parameter wirkungslos | 17.09.2026 |
| **E‑203** | **Hex ist gekappt** auf die ersten **131.072 Bytes**, mit Vermerk. Herleitung in §3 | 17.09.2026 |
| **E‑204** | *Beim Bau gefallen, aus dem Befund in §5.* **Hex nur bei `ASCII` und `ISO_8859_1`.** Bei `UTF_8` ist der Zeichencode nicht das Byte (`ä` ist `0xE4`, aber `C3 A4`), und das Backend entfernt eine führende BOM aus dem Text; Hex hieße dort ein erfundenes Byte. Bei jeder anderen Kodierung und bei jedem Zeichencode über `0xFF`: Original mit Vermerk. **Zu bestätigen vom Auftraggeber** (offener Punkt 199) | 17.09.2026 |
| **E‑205** | *Beim Bau gefallen.* **Die Auswahl ist `AuswahlFeld`** aus `components/`, nicht ein neu generiertes `select.tsx` von shadcn/ui. `AuswahlFeld` ist seit Punkt 180 der Ersatz der Anwendung für das native Auswahlfeld — Rolle, Baumgliederung —, gebaut auf shadcn/ui-`Popover`; ein zweiter Baustein für dieselbe Aufgabe wäre die Drift, gegen die der Umbau gerichtet war ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18). `components/ui` ist unberührt | 17.09.2026 |
| **E‑206** | **Der Download-Vermerk hängt an „Ergebnis ≠ Original".** Er steht nur, wo der angezeigte Text nicht zeichengleich mit dem Original ist — nicht beim Original, nicht bei „passt nicht", und nicht bei einer Darstellung, die nichts ändert (schon umbrochenes EDIFACT, VDA mit Zeilenumbrüchen, schon formatiertes XML oder JSON). Umgesetzt als Kennzeichen `weichtAb` im Ergebnis von `stelleDar`; `darstellungsvermerke` nimmt seither nur noch das Ergebnis. Präzisiert E‑201 und löst die Auslegung in §4 ab; aus Punkt 205 | 18.09.2026 |
| **E‑207** | **Die Auswahl ist `w-48` breit, nicht `w-44`.** Der längste Eintrag „ANSI X12 (detected)" brauchte in allen vier Dichten 3 px mehr, als `w-44` hergab (M187‑4); bei `w-48` passt er überall (M187‑7). Aus Punkt 206 | 18.09.2026 |

---

## 3. Die Darstellungen und die Erkennung

**Allgemein.** Vorangestellter Leerraum und eine UTF-8-BOM — als `U+FEFF`, oder in `ISO-8859-1`
die drei Zeichen `ï»¿` — werden bei der Erkennung übersprungen und unverändert ausgegeben. Alle
Standardtrennzeichen liegen im ASCII-Bereich, die Bytes von UTF-8-Mehrbytezeichen alle ab `0x80`;
eine Kodierungsfrage entsteht beim Formatieren nicht. Hex ist die Ausnahme (unten).

| Darstellung | Erkannt, wenn | Was sie tut |
|---|---|---|
| **EDIFACT** | der Text mit `UNA` oder `UNB+` beginnt und mindestens ein Segmentende vorkommt. Nach `UNA` folgen sechs Zeichen — Komponententrenner, Datenelementtrenner, Dezimalzeichen, Freigabezeichen, reserviert, Segmentende —, danach `UNB`, gegebenenfalls nach einem Zeilenumbruch. Ohne `UNA`: `:` `+` `.` `?` Leerzeichen `'` | Umbruch nach jedem Segmentende. Das Zeichen hinter dem Freigabezeichen ist nie Trenner (`?'` ist ein Apostroph, `??` ein Fragezeichen). Ein Zeilenumbruch direkt hinter dem Segmentende wird übernommen, nicht verdoppelt; Zeilenumbrüche innerhalb eines Segments entfallen. Ist das Segmentende ein Zeilenumbruch, bleibt der Text, wie er ist |
| **ANSI X12** | der Text mit `ISA` beginnt und das ISA-Segment seine feste Länge hat: Feldbreiten 2, 10, 2, 10, 2, 15, 2, 15, 6, 4, 1, 5, 9, 1, 1, 1; Elementtrenner an Stelle 4 und vor jedem weiteren Feld an seiner festen Stelle, Komponententrenner an 105, Segmentende an 106; die drei Trennzeichen verschieden | wie EDIFACT, ohne Freigabezeichen |
| **VDA** | Festlängensätze zu 128 Zeichen, Satzart in drei Ziffern. Mit Zeilenumbrüchen: jede nicht leere Zeile ohne Zeilenende genau 128 Zeichen und drei Ziffern voran. Ohne: die Länge ohne abschließenden Zeilenumbruch ein Vielfaches von 128, jeder Block mit drei Ziffern | mit Zeilenumbrüchen gleich dem Original; ohne Umbruch nach je 128 Zeichen |
| **IDoc** | der Text mit `EDI_DC40` beginnt und jeder Datensatz mit einem Segmentnamen (Großbuchstaben, Ziffern, `_`, `/`, linksbündig auf 30 Stellen) beginnt und an Stelle 62–63 eine zweistellige Hierarchieebene trägt. Ohne Zeilenumbrüche geteilt nach Kontrollsatz 524, Datensatz 1063 (30 + 3 + 16 + 6 + 6 + 2 + 1000) | ein Satz je Zeile; Datensätze um zwei Leerzeichen je Ebene eingerückt, ab der kleinsten vorkommenden Ebene; Kontrollsätze nie |
| **XML** | der Text mit `<` beginnt, sich vollständig in Token zerlegen lässt (Deklaration und Verarbeitungsanweisungen, Kommentare, CDATA, DOCTYPE samt internem Teil, Start-, End- und leere Tags mit Attributwerten, die `>` enthalten dürfen, Text), jedes Endtag zum offenen Starttag passt, am Ende nichts offen ist und außerhalb des Wurzelelements nur Leerraum steht | Umbruch und Einrückung um zwei Leerzeichen je Tiefe **nur zwischen zwei Token, zwischen denen nichts oder nur Leerraum steht**; dieser Leerraum wird ersetzt. Neben Text mit Inhalt wird nichts eingefügt: `<a>Text</a>` und gemischter Inhalt bleiben auf ihrer Zeile. Ein leeres Paar `<a></a>` bleibt ebenfalls auf einer Zeile — die Regel erlaubt den Umbruch dort, sie verlangt ihn nicht |
| **JSON** | der Text mit `{` oder `[` beginnt und als Ganzes der JSON-Grammatik genügt, danach nur noch Leerraum | Leerraum außerhalb von Zeichenketten durch Umbruch und Einrückung um zwei Leerzeichen ersetzt, hinter dem Doppelpunkt ein Leerzeichen, `{}` und `[]` kompakt. Zeichenketten samt Escapes, Zahlen, `true`, `false`, `null` Zeichen für Zeichen; Leerraum vor und hinter dem Wert unverändert |
| **Hex** | nie — immer wählbar | wie `hexdump -C` ohne Zusammenfassung und Schlusszeile: Versatz achtstellig, 16 Bytes je Zeile in Kleinbuchstaben mit Lücke nach acht, rechts zwischen `\|` die Zeichen `0x20`–`0x7E`, sonst `.`. Eine unvollständige letzte Zeile ist im Hex-Bereich aufgefüllt, rechts nicht (65 statt 78 Zeichen) |

**Die sechs Merkmale schließen einander am ersten Zeichen nach dem Vorspann aus** — `UNA`/`UNB`,
`ISA`, drei Ziffern, `EDI_DC40`, `<`, `{` oder `[`. Geprüft wird trotzdem jedes gegen jedes (§6,
Kreuzprobe).

**Hex und die Kodierung (E‑204).** Die Bytes sind die Zeichencodes des Texts — das stimmt, wo
`ISO-8859-1` oder ASCII jedes Byte auf genau ein Zeichen abgebildet hat, und die Prüfung in §5
sagt, dass das Backend den Text sonst unverändert lässt. Bei `UTF_8` stimmt es nicht, und Hex
zeigt dort das Original mit Vermerk. Ein Zeichencode über `0xFF` heißt in jedem Fall: Original mit
Vermerk, nie ein erfundenes Byte.

**Die Kappung (E‑203), nachgerechnet.** Eine Zeile trägt 8 + 2 + 49 + 2 + 16 + 1 = **78** Zeichen.
131.072 Bytes sind **8.192** Zeilen: 8.192 × 78 = 638.976 Zeichen, dazu 8.191 Zeilenumbrüche,
zusammen **647.167 Zeichen** — dieselbe Größenordnung wie die größte gemessene Rohanzeige
(609.995 Bytes, M60), deren Verhalten im `<pre>` ungemessen ist ([`rohdaten-frontend.md`](rohdaten-frontend.md)
§11, Punkt 9). Die Zahl ist in `tests/darstellung.test.ts` aus dem Ergebnis gezählt, nicht nur
gerechnet.

> **Belegvermerk (Regel L10).** Welche Formate im Bestand vorkommen, ist **nicht erhoben**; §9
> des Auftrags nennt kein Beispiel. Jede Darstellung ist mit erfundenen Inhalten geprüft, keine an
> einer Datei aus der Ablage (offener Punkt 197). Die IDoc-Längen und -Stellen stammen aus der
> SAP-Satzstruktur `EDI_DC40` und `EDI_DD40` und sind an keiner Datei geprüft (Punkt 196). Der
> Wert 128 für VDA und die IDoc-Längen zählen **Zeichen**, nicht Bytes — für ASCII und
> ISO-8859-1 dasselbe, für UTF-8 mit Bytes über `0x7F` nicht (Punkt 198).

> **Nachtrag 18.09.2026 (M187, §11).** Der Belegvermerk darüber bleibt wörtlich stehen und ist in
> zwei Teilen überholt. **Alle sechs Formate kommen im Fenster `2025-07-24` bis `2025-12-30` vor**,
> und jede Darstellung ist dort an echten Dateien angewandt und gegen die Eigenschaft aus E‑197
> geprüft — eine Stichprobe, keine Erhebung (M187‑1). **Die IDoc-Stellen stimmen an 137 Dateien**
> (Kontrollsatz 524, Datensatz 1.063, Ebene an Stelle 62–63); daneben gibt es eine zweite Spielart
> mit leerer Ebene, die nicht erkannt wird (M187‑5, Punkt 204).

---

## 4. Die Oberfläche

```
← Zurück zur Nachricht

Datei konvertiert                      Darstellung [EDIFACT (erkannt) ⌄]  [ ⤓ Herunterladen ]
Converter.Payload.GUID · Nutzdaten · 12.480 Bytes · Kodierung ASCII

│ ℹ Der Download liefert die Originaldatei, nicht diese Darstellung.
┌──────────────────────────────────────────────────────────────────────────┐
│ UNB+UNOC:3+…'                                                            │
│ UNH+1+ORDERS:D:96A:UN'                                                   │
└──────────────────────────────────────────────────────────────────────────┘
```

**Platz.** Die Auswahl steht rechts im Kopf, neben dem Download-Knopf: Beides ist, was man mit
*dieser* Datei tut — sie in einer Darstellung lesen oder sie herunterladen —, und der Vermerk
darunter sagt, wo die beiden auseinandergehen. Sie steht nicht über dem Inhalt als eigene Leiste,
weil sie kein Filter ist und die Datei keine Breite abgeben soll ([`rohdaten-frontend.md`](rohdaten-frontend.md)
§7). Am schmalen Fenster bricht die Kopfzeile um, wie sie es mit dem Download-Knopf schon tat.

**Form.** Ein beschriftetes Feld (`Label` + `AuswahlFeld`, E‑205), kein Reiter- und kein
Knopfband: Acht Einträge nebeneinander nähmen der Datei die Breite, und gewählt wird selten und
bewusst. Die Beschriftung heißt **„Darstellung"** (englisch *View*) und steht als Wort vor dem
Feld — keine Großschreibung, kein Vorsatz. Das erkannte Format trägt im Eintrag den Zusatz
**„(erkannt)"**: Wer die Liste öffnet, sieht den Vorschlag an seiner Stelle; wer sie nicht öffnet,
sieht das Original und keinen Hinweis. Nichts blinkt, nichts wird von selbst angewandt (E‑195).
Hex trägt den Zusatz nie. *Seit dem 18.09.2026 ist das Feld `w-48` breit (E‑207): Bei `w-44`
endete „ANSI X12 (detected)" in allen vier Dichten auf „(detecte…" (M187‑4).*

**Wo sie nicht steht (E‑194).** Bei Protokollen — auch bei `ANZEIGBAR` —, in den fünf
inhaltslosen Zuständen und bei der leeren Datei. Dort ist auch `?darstellung=…` wirkungslos: Der
Text bleibt, wie er ist, und kein Vermerk erscheint.

**Die drei neuen Vermerke** folgen dem Muster der drei vorhandenen ([`rohdaten-frontend.md`](rohdaten-frontend.md)
§5): dieselbe Zeile, dasselbe Zeichen, dieselbe Farbe — keine, denn keiner ist ein Fehler. Sie
stehen **hinter** den Vermerken der Anzeige, weil jene von der Datei handeln und diese von der
Wahl:

| Vermerk | Auslöser | betrifft den Download |
|---|---|---|
| **passt nicht** — *„Diese Datei lässt sich nicht als {Darstellung} darstellen. Du siehst das Original."* | die Datei passt nicht zur gewählten Darstellung (E‑200) | nein |
| **Hex gekappt** — *„Die Hex-Darstellung endet nach 131.072 Bytes."* | die Hex-Darstellung endet an der Grenze (E‑203) | nein |
| **Download liefert das Original** — *„Der Download liefert die Originaldatei, nicht diese Darstellung."* | eine Darstellung außer Original ist **angewandt** (E‑201) · *seit dem 18.09.2026:* der angezeigte Text **weicht vom Original ab** (E‑206) | **ja** |

> **Eine Auslegung, benannt:** Der Download-Vermerk steht nur, wo eine Darstellung tatsächlich
> angewandt ist — nicht, wenn sie nicht passt. Dann steht das Original, und das ist auch die Datei,
> die der Download liefert; ein Vermerk, der vor einem Unterschied warnte, behauptete einen, den es
> nicht gibt. Der Auftrag sagt „solange nicht Original gewählt ist"; gebaut ist „solange nicht
> Original angezeigt ist". Wer das anders will, ändert `darstellungsvermerke` an einer Stelle.
>
> **Abgelöst am 18.09.2026 (E‑206).** Die Auslegung trug einen Fall nicht, den die Sichtprüfung
> gezeigt hat (M187‑2, Punkt 205): Eine Darstellung kann passen und trotzdem nichts ändern —
> schon umbrochenes EDIFACT, VDA mit Zeilenumbrüchen —, und dort stand der Vermerk über einem
> Text, der zeichengleich mit dem Download ist. Seither hängt er am **Ergebnis**, nicht an der
> Wahl: Er steht, wo der angezeigte Text vom Original abweicht, und sonst nicht. Das ist die
> Begründung dieses Kastens, zu Ende gedacht; „passt nicht" ist darin kein Sonderfall mehr.

**Die Adresse (E‑202).** `?darstellung=edifact` öffnet dieselbe Darstellung; `darstellung=original`
steht nie in der Adresse, weil `nuqs` einen Parameter entfernt, der dem Standardwert gleicht — und
das ist hier richtig: **Das Original lässt nichts weg**, es ist die Datei, wie sie ist. Die Regel
aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §8 lautet *ein Standardwert, der etwas
weglässt, gehört in die URL; einer, der etwas setzt, nicht* — deshalb steht am Parser
`withDefault("original")` **ohne** `clearOnDefault: false`, bewusst und nicht aus Vergessen. Die
Bindung trägt `history: "replace"` wie jede Filterleiste (acht Darstellungen sind keine acht
Stationen) und `shallow: true`, weil der Wechsel im Browser rechnet und keinen Server-Roundtrip
auslösen darf. Verweise **in** die Ansicht tragen den Parameter nicht ([`rohdaten-frontend.md`](rohdaten-frontend.md)
§8).

**Berechnet wird einmal je Text und Darstellung** (`useMemo`), die Erkennung einmal je Text. Das
Ergebnis geht als einziges Kind in das `<pre>`; `pre-wrap` und `break-words` bleiben — ein
Segment, das länger ist als die Fläche breit, bricht um wie zuvor. Keine neue Farbrolle, kein neues
Dichtemaß, keine Tailwind-Farbklasse, weiterhin eine Bildlaufleiste je Seite.

---

## 5. Die Prüfung im Backend: bleibt der Anzeigetext außer der Dekodierung unverändert?

**Genau eine Stelle** macht aus den Bytes den Text: `Inhaltseinstufung.stufeEin` im Paket
`payload` ([`rohdaten-backend.md`](rohdaten-backend.md) §6 und §12). Gelesen am 17.09.2026:

| Kodierung | Weg | Änderung außer der Dekodierung |
|---|---|---|
| `ASCII` | `new String(daten, US_ASCII)` | **keine** — jedes Byte ein Zeichen, kein Byte über `0x7F` |
| `ISO_8859_1` | `new String(daten, ISO_8859_1)` | **keine** — jedes Byte ein Zeichen |
| `UTF_8` | strenger Decoder (`REPORT`), dann `ohneBom` | **eine:** ein führendes `U+FEFF` wird entfernt (Belegt in `Inhaltseinstufung`, Zeile 226–229, und im Gleichlauf-Kasten in `rohdaten-backend.md` §7: „der BOM fällt allein aus dem angezeigten Text") |

**Keine Vereinheitlichung der Zeilenenden**, kein `trim`, kein `replace` — `\r\n` bleibt `\r\n`.
Danach greifen nur, was ohnehin vermerkt ist: der Beschnitt bei Protokollen für `MANDANT`
(`Protokollbeschnitt`, Vermerk *Ausschnitt*) und die Kappung an der Zeichengrenze
(`Kodierung.gekappt`, Vermerk *Kappung*).

**Was daraus folgt.** Für `ASCII` und `ISO_8859_1` ist Hex aus den Zeichencodes **verlustfrei**;
für `UTF_8` wäre es das nicht — der Zeichencode ist nicht das Byte, und die BOM fehlt. Der Auftrag
sagt für diesen Fall „anhalten und melden". Gebaut ist stattdessen E‑204: Hex ist bei `UTF_8`
nicht verfügbar (Original mit Vermerk *passt nicht*), und der Fall steht als offener Punkt 199
zur Entscheidung: Ein Weg über `TextEncoder` lieferte die Bytes exakt zurück — nur die BOM nicht,
und die Oberfläche kann nicht wissen, ob es eine gab.

---

## 6. Aufteilung des Codes

| Datei | Zweck |
|---|---|
| `features/nachrichten/darstellung/arten.ts` | die acht Werte in ihrer Reihenfolge, der Typ `Darstellung`, die sechs `Format`e, die Vorgabe |
| `…/darstellung/vorspann.ts` | Leerraum und BOM in beiden Gestalten überspringen |
| `…/darstellung/segmente.ts` | die gemeinsame Regel von EDIFACT und X12: Umbruch nach dem Segmentende, Freigabezeichen, vorhandene Umbrüche |
| `…/darstellung/edifact.ts`, `x12.ts`, `vda.ts`, `idoc.ts`, `xml.ts`, `json.ts`, `hex.ts` | je Darstellung `erkenne…` und `formatiere…` — reine Funktionen, `null` heißt „passt nicht" |
| `…/darstellung/erkennung.ts` | `erkenneFormat`: die sechs Erkennungen in Reihenfolge, höchstens ein Treffer |
| `…/darstellung/index.ts` | `stelleDar` (Text, Darstellung, Kodierung → Ergebnis), `darstellungsvermerke`, `darstellungWaehlbar`; die öffentliche Schnittstelle des Moduls |
| `…/darstellung/parameter.ts` | `parseAsDarstellung` und `DARSTELLUNG_PARAMETER` für nuqs — frei von React |
| `features/nachrichten/hooks.ts` | `useDarstellung`: die Bindung an die Adresse |
| `features/nachrichten/components/artefakt-ansicht.tsx` | `ArtefaktAnsicht` ist jetzt die dünne Hülle um die Adresse; `Dateiansicht` darunter bekommt Darstellung und Setter als Props. Neu: `DarstellungAuswahl`, die drei Vermerke in `Vermerke`, der `useMemo` |
| `i18n/de.ts`, `i18n/en.ts` | `nachrichten.detail.dateien.darstellung`: Beschriftung, der Zusatz „erkannt", die acht Einträge, die drei Vermerke. Keine Zeichenkette in einer Komponente |

**Unberührt:** `components/ui` (kein neuer Baustein, E‑205), `globals.css`, `lib/`, das Backend, die
Route `page.tsx`. Keine neue Abhängigkeit, auch nicht für Tests.

> **Die Suspense-Grenze, benannt.** Der Auftrag verlangt die Adressanbindung „mit der
> Suspense-Grenze nach vorhandenem Muster". Das vorhandene Muster im angemeldeten Bereich —
> Dashboard, Nachrichtenliste, Katalog, Prozessansicht — bindet `useQueryStates` in einem Hook
> in `hooks.ts` an eine `"use client"`-Ansicht, **ohne** eigene Suspense-Grenze: Die Routen
> unter `(app)` sind dynamisch (das Layout liest Cookies), und `next build` verlangt dort keine.
> Die einzige Suspense-Grenze des Projekts liegt auf `/anmeldung`. Übernommen ist das Muster des
> angemeldeten Bereichs; `pnpm build` läuft damit durch.

---

## 7. Tests

`pnpm test` — **45 Dateien, 1.230 Fälle**, alle grün; gezählt aus `vitest run --reporter=json`
je Datei gegen den Lauf am Commit `01fa5fd` (44 Dateien, 1.130 Fälle).

> **Fortgeschrieben am 18.09.2026 (E‑206, E‑207).** `pnpm check` — Lint, Typprüfung,
> Formatprüfung, Tests — grün mit **45 Dateien, 1.237 Fällen**; `pnpm build` grün. Die sieben
> Fälle mehr stehen alle in `tests/darstellung.test.ts`, jetzt **77**: ein `it.each` „nennen den
> Download nicht, wenn … nichts ändert" über EDIFACT mit Zeilenumbruch als Segmentende, EDIFACT
> und X12 mit schon einem Segment je Zeile, VDA mit Zeilen und mit CRLF, schon formatiertes XML und
> JSON — je Ergebnis zeichengleich, `weichtAb` falsch, kein Vermerk. Keine neue Quelldatei, keine
> neue rendernde; `tests/artefakt-ansicht.test.tsx` bleibt bei 19, der Kopf von
> `frontend/vitest.config.mts` bei 174. **Verletzungsprobe:** die Bedingung vorübergehend auf die
> alte, an der Wahl hängende Form zurückgesetzt — **acht Fälle rot**, die sieben neuen und „stehen
> beim Original nie"; zurückgenommen per Edit, nicht per `git checkout`.

| Datei | Art | Fälle | Deckt ab |
|---|---|---|---|
| `tests/darstellung.test.ts` *(neu)* | reine Funktionen, Umgebung `node` | **70** — seit dem 18.09.2026 **77** (E‑206, Kasten oben) | je Darstellung **die Eigenschaft aus E‑197** — EDIFACT, X12, VDA ohne alle Zeilenumbrüche gleich dem Original ohne alle Zeilenumbrüche; IDoc ebenso nach Entfernen der Einrückung; XML nach Entfernen des Leerraums zwischen Token; JSON nach Entfernen des Leerraums außerhalb von Zeichenketten; Hex über alle Zeichencodes `0x00`–`0xFF` zurückgerechnet —, **die Fallen** (EDIFACT mit `?'` und `??`, abweichender `UNA`, einzeilig, umbrochen, Zeilenumbruch als Segmentende · X12 mit `~`, mit Zeilenumbruch dahinter, mit Zeilenumbruch als Segmentende, ein Feld zu lang · VDA mit und ohne Zeilen, mit `\r\n`, Länge kein Vielfaches · IDoc mit und ohne Zeilen, kleinste Ebene, fehlende Ebene · XML mit Deklaration, DOCTYPE, `&#xE4;`-artiger Zeichenreferenz, `<a></a>`, `>` im Attributwert, CDATA mit Tags, gemischtem Inhalt, gestapeltem Leerraum, unpassendem Endtag, gekappt · JSON mit `12.50`, `1e5`, 20-stelliger Zahl, dem Escape `\u00e4`, doppeltem Schlüssel, `{}`/`[]`, acht Grammatikverletzungen, `JSON.parse` als Gegenprobe · Hex an der Kappungsgrenze mit den 647.167 Zeichen, Zeichencode über `0xFF`, die drei Kodierungen, die unvollständige Zeile), **BOM und Leerraum** in vier Gestalten je Format, **die Kreuzprobe** (jedes Beispiel gegen jede Erkennung, genau ein Treffer; jede andere Darstellung außer Hex „passt nicht"; Text ohne Merkmal), die drei Vermerke, `darstellungWaehlbar` über alle Lagen, der Parser mit den acht Werten und dem unbekannten |
| `tests/artefakt-ansicht.test.tsx` | gerenderter Baum | **19** (+4) | **nur, was keine reine Funktion trägt:** ein Textknoten in **jeder der acht Darstellungen** mit dem erfundenen Inhalt aus `<script>`, `<img onerror=…>` und `<svg>`; ein wohlgeformtes XML mit `<script>` und `<svg onload=…/>`, das tatsächlich als XML formatiert wird und Text bleibt; keine Auswahl beim Protokoll und in den fünf Zuständen, und der Parameter dort wirkungslos; der Download-Vermerk vorhanden bei Formatierung und abwesend beim Original, bei unverändertem Download-Verweis und genau einem Knopf |
| `tests/farbwerte.test.ts`, `tests/serverbausteine.test.ts` | je Quelldatei | +13, +13 | die dreizehn neuen Quelldateien — gezählt, nicht geschrieben |

Die gerenderten Fälle sind damit **174 in einundzwanzig Dateien**; geführt im Kopf von
`frontend/vitest.config.mts`, nicht hier. Keine neue rendernde Datei — die Tabelle in
[`frontend-grundlagen.md`](frontend-grundlagen.md) §9 ist deshalb nicht angefasst (§8).

**Was die Tests nicht sagen:** nichts über Wanduhrzeit (Regel T1), nichts über echte Dateien (§9
der Vorgabe: kein Test enthält echten Dateiinhalt), nichts über das Aussehen. Ein Befund beim
Bauen: `farbwerte.test.ts` trifft `&#228;` in einem *Kommentar* als Hex-Farbwert `#228` — die
Zeichenreferenz steht in Kommentaren deshalb als `&#xE4;`.

---

## 8. Regelbezug und die Korrekturen in anderen Dateien

| Regel | Umsetzung |
|---|---|
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | Die Erkennung ist eine Prüfung fester Merkmale; trifft keines, ist nichts vorgemerkt. Hex erfindet kein Byte |
| Textknoten, niemals HTML ([`rohdaten-frontend.md`](rohdaten-frontend.md) §4) | Das Ergebnis jeder Darstellung ist genau ein Textknoten; belegt in allen acht Lagen und mit einem XML, das `<script>` trägt und formatiert wird |
| Keine Zeichenkette in einer Komponente | alles in `i18n/de.ts` und `en.ts`; `tests/sprachdateien.test.ts` hält den Schlüsselsatz gleich |
| Farben nur über Tokens | keine neue Rolle; `tests/farbwerte.test.ts` deckt die dreizehn Dateien ab |
| **T1** Kein Test behauptet Wanduhrzeit | keine Zeitmessung; die Kappung ist eine Zeichenzahl, kein Timing |
| Filterzustand in der URL, `clearOnDefault`-Falle ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8) | `darstellung` mit `withDefault` **ohne** `clearOnDefault: false`, weil das Original nichts weglässt |
| Generatorbereich unberührt | `components/ui` unverändert; die Auswahl ist `AuswahlFeld` (E‑205) |
| **L10** Belegvermerk | überall, wo nichts gemessen ist: Formate im Bestand, IDoc-Stellen, große Textknoten |

**Datierte Korrekturblöcke vom 17.09.2026**, der alte Text bleibt jeweils wörtlich stehen:

| Datei | Stelle |
|---|---|
| [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) | §9, die Zeile zur aufbereiteten Anzeige unter „Nicht enthalten"; §7, der Satz zur ausgeschlossenen aufbereiteten Anzeige |
| [`rohdaten.md`](rohdaten.md) | §1, der Kasten zur Abgrenzung; §7, der erste Punkt (die Syntaxhervorhebung bleibt ausgeschlossen); §13 Punkt 3, Vermerk, dass die Formate jetzt gebaut sind — der Grund der Abschaltung bleibt offen |
| [`rohdaten-frontend.md`](rohdaten-frontend.md) | §4 „Die Anzeige selbst", dritter Punkt; §6, die dritte benannte Abweichung; §8, die Zeile „Zustand in der URL"; §12, Verweis auf diese Datei |
| [`rohdaten-backend.md`](rohdaten-backend.md) | §7 „Zwei Stellen", mit Verweis auf die dritte im Frontend |
| [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) | Schritt 8, Absatz „Abgrenzung" |
| [`README.md`](README.md) | Zeile für diese Datei; datierter Vermerk an der Zeile zu `rohdaten.md` |

**Nicht angefasst, obwohl es dort jetzt anders steht:** [`frontend-grundlagen.md`](frontend-grundlagen.md)
§9 führt `artefakt-ansicht.test.tsx` weiterhin mit „neun Fälle" (die Zeile war schon bei 15
überholt); der Auftrag gibt die Tabelle nur für eine neue rendernde Datei frei. Die Zahl steht
ohnehin allein in `vitest.config.mts`.

---

## 9. Abweichungen vom Auftrag

Alle vorsätzlich, alle hier und einzeln:

1. **`AuswahlFeld` statt shadcn-`Select`** (E‑205). Der Auftrag sagt „bei acht Einträgen ein
   Select" und „fehlt der Baustein, über den Generator hinzufügen". Der Baustein fehlt in
   `components/ui`, und er ist **nicht** generiert worden: Das Projekt hat seit Punkt 180 seinen
   eigenen Ersatz für Auswahlfelder, auf shadcn/ui-`Popover` gebaut, und die Rolle und die
   Baumgliederung sind bereits darauf umgezogen. Ein zweiter Baustein für dieselbe Aufgabe wäre
   die Drift, die der Umbau beenden sollte. `serverbausteine.test.ts` bleibt grün.
2. **Hex bei `UTF_8`: nicht verfügbar statt „anhalten und melden"** (E‑204, §5). Der Befund im
   Backend — die BOM fällt aus dem UTF-8-Text — trifft die Verlustfreiheit von Hex genau für diese
   Kodierung. Angehalten ist nicht; gebaut ist Hex dort, wo es verlustfrei ist, und der Fall ist
   gemeldet (Punkt 199).
3. **Keine eigene Suspense-Grenze** — das vorhandene Muster des angemeldeten Bereichs hat keine
   (§6, Kasten).
4. **Der Download-Vermerk steht bei „passt nicht" nicht** (§4, Kasten) — „nicht Original
   angezeigt" statt „nicht Original gewählt".
   *Seit dem 18.09.2026 keine Abweichung mehr, sondern Entscheidung E‑206 — und weiter gefasst:
   Der Vermerk hängt daran, ob das Angezeigte vom Original abweicht.*
5. **Die leere Datei bekommt keine Auswahl** (E‑194, abgeleitet): Sie ist ein benannter Zustand,
   kein Inhalt.
6. **`<a></a>` bleibt auf einer Zeile** (§3): Die XML-Regel erlaubt den Umbruch zwischen zwei
   Token ohne etwas dazwischen, sie verlangt ihn nicht.
7. **IDoc mit Zeilenumbrüchen prüft keine Satzlängen** — der Auftrag nennt die Längen für das
   Teilen ohne Zeilenumbrüche; mit ihnen ist jede Zeile ein Satz, und die Prüfung gilt dem
   Segmentnamen und der Ebene. Abgeschnittene Leerzeichen am Zeilenende bleiben so erkennbar.
8. **Die Hex-Teilzeile ist 65 Zeichen lang, nicht 78:** `hexdump -C` füllt den Hex-Bereich auf,
   damit `|` an derselben Stelle steht, den Zeichenbereich rechts nicht. So auch hier.
9. **Teil D, die Sichtprüfung, ist nicht gelaufen** (Punkt 201): Sie braucht die Anmeldung im
   Browser durch den Nutzer, und §9 des Auftrags nennt keine Beispielnachricht — weder zu einem
   Format noch zur IDoc-Frage.
   *Nachgeholt am 18.09.2026 (§11, M187):* Die Beispiele stammen aus einer eigenen Stichprobe über
   alle zehn Mandanten, weil §9 des Auftrags keine nennt.
10. **Die Vermerke der Anzeige stehen vor denen der Darstellung.** Der Auftrag legt keine
    Reihenfolge fest; gewählt ist „Datei vor Wahl" (§4).

---

## 10. Offene Punkte

Die Nummern schließen an [`live-rest.md`](live-rest.md) §15 an, dessen höchste **195** ist.

| # | Punkt |
|---|---|
| **196** | **Die IDoc-Stellen sind ungeprüft.** Kontrollsatz 524, Datensatz 1063, Segmentname 30, Ebene an Stelle 62–63 stammen aus der SAP-Satzstruktur `EDI_DC40`/`EDI_DD40` und sind an keiner Datei dieses Bestands geprüft; §9 des Auftrags nennt keine IDoc-Nachricht. Ob die Dateien im Bestand mit oder ohne Zeilenumbrüche kommen, ist ebenso unbekannt. **Zu klären an einer echten Datei** · **Nachtrag 18.09.2026 (M187‑5):** an **137 Dateien bestätigt** — alle mit Zeilenumbrüchen, Kontrollsatz 524 Zeichen, Datensätze 1.063, Ebenen `01` bis `05` an Stelle 62–63. Eine Datei ohne Zeilenumbrüche kam nicht vor; die Teilung nach 524/1.063 bleibt ungeprüft. Eine zweite Spielart wird nicht erkannt: Punkt 204 |
| **197** | **Welche Formate im Bestand vorkommen, ist nicht erhoben.** Alle sieben Zeilen der Beispieltabelle in §9 des Auftrags sind leer; jede Darstellung ist allein mit erfundenen Inhalten geprüft. Auflösbar durch eine Messung derselben Art wie M61 über `erkenneFormat` — sie würde zugleich sagen, wie oft nichts erkannt wird · **Nachtrag 18.09.2026 (M187‑1):** zum Teil beantwortet, durch eine Stichprobe und nicht durch eine Erhebung — von 1.451 anzeigbaren Nutzdateien VDA 386, EDIFACT 287, XML 163, IDoc 137, ANSI X12 49, JSON 6, **nichts erkannt 423**. Alle sechs Formate kommen vor. Die Messung über den Bestand bleibt offen |
| **198** | **VDA und IDoc zählen Zeichen, nicht Bytes.** 128 und 524/1063 sind Bytelängen der Formate; hier wird der dekodierte Text gezählt. Für `ASCII` und `ISO_8859_1` ist das dasselbe, für `UTF_8` mit Bytes über `0x7F` nicht — eine solche Datei würde nicht erkannt und „passt nicht" melden. Ob es sie gibt, sagt Punkt 197 |
| **199** | **Hex bei `UTF_8`** (E‑204, §5). Zu entscheiden vom Auftraggeber: (a) so lassen; (b) `TextEncoder` — liefert die Bytes exakt zurück, nur eine BOM nicht, und der Vermerk müsste sagen, dass eine BOM fehlen kann; (c) ein Kennzeichen `bomEntfernt` in der Antwort — das wäre ein Feld im Backend und damit außerhalb dieses Auftrags · **Nachtrag 18.09.2026 (M187‑1, M187‑2):** In der Stichprobe sind **22 von 1.451** anzeigbaren Nutzdateien `UTF_8` (1,5 %). Gesehen an einer EDIFACT-Datei mit 640 Bytes in 638 Zeichen: EDIFACT formatiert, Hex zeigt das Original mit „passt nicht" |
| **200** | **Große Textknoten sind ungemessen.** Hex bringt bis zu 647.167 Zeichen in das `<pre>`, EDIFACT bis zu 1 MiB plus einen Umbruch je Segment; das Verhalten eines Textknotens dieser Größe mit `pre-wrap` ist nicht gemessen — im lokalen Bestand ist keine so große Datei abrufbar ([`rohdaten-frontend.md`](rohdaten-frontend.md) §11, Punkt 9 gilt weiter) · **Überholt am 18.09.2026 (M187‑1, M187‑2):** Solche Dateien sind im Fenster abrufbar — die fünf größten der Stichprobe haben 5.503.419 bis 6.282.961 Bytes. An der größten angesehen: die gekappte Anzeige mit 1.048.576 Zeichen in 986 Zeilen, IDoc mit 1.054.096 Zeichen, Hex an der Grenze mit **647.167 Zeichen in 8.192 Zeilen** — die Zahl aus §3, jetzt an einer echten Datei — ohne sichtbare Auffälligkeit. **Gemessen ist weiterhin nichts.** Was die Dateigröße für die Grenze der Ablage heißt: Punkt 208 |
| **201** | **Die Sichtprüfung (Teil D) steht aus:** je Beispiel Original beim Öffnen, „(erkannt)" am richtigen Eintrag, korrekte Darstellung, Download-Vermerk, Hex, eine unpassende Darstellung mit Vermerk, eine kopierte Adresse mit `darstellung`, keine Konsolenmeldung — im angemeldeten Browser, Profil `dev`, mit Nachrichten aus `2025-07-24` bis `2025-12-30`. Das schmale Fenster prüft der Auftraggeber von Hand · **Erledigt am 18.09.2026 (§11, M187)** — alle acht Schritte bestanden, an zwölf Beispielen mit Wechsel über alle sechs Formate, dazu vier Lagen ohne Auswahl und ein unbekannter Wert. **Offen bleibt das schmale Fenster** beim Auftraggeber ([`README.md`](README.md), „Offene Sichtprüfungen", Zeile „8 — Rohdaten und Protokolle") |
| **202** | **Der Grund der Abschaltung im Altwerkzeug bleibt unbekannt** ([`rohdaten.md`](rohdaten.md) §13 Punkt 3). Die vier Formate dort — Edifact, VDA, XML, ANSI — sind jetzt hier gebaut, dazu IDoc, JSON und Hex; warum sie dort abgeschaltet wurden, sagt weder der Auftrag noch der Code |
| **203** | **Die Erkennung läuft über den ganzen Text.** XML und JSON zerlegen bis zu 1 MiB, um „erkannt" zu sagen; EDIFACT zählt Segmentenden über den ganzen Text. Das ist einmal je Text (`useMemo`) und im Browser — gemessen ist es nicht (Regel T1 verbietet die Zusicherung, nicht die Ausgabe). Wer es messen will: `stelleDar` mit einem 1-MiB-Text in der Konsole · **Nachtrag 18.09.2026 (M187‑4):** eine Beobachtung, keine Messung — am 1-MiB-Text der größten Datei (ein IDoc) im Browser des Prüflaufs, Entwicklungsbau, Median aus fünf Läufen: `erkenneFormat` 0,6 ms, `stelleDar` IDoc 0,6 ms, Hex 11,9 ms; XML, JSON und EDIFACT scheitern dort am Anfang. **Der ungünstige Fall — ein XML oder JSON von 1 MiB — kam in der Stichprobe nicht vor** und bleibt offen |
| **204** | **IDoc-Dateien mit leerer Ebene werden nicht erkannt** (M187‑5). In der Stichprobe beginnen 292 Nutzdateien mit `EDI_DC40`; die Regel aus §3 erkennt 137, **155 nicht**: An Stelle 62–63 stehen bei 152 zwei Leerzeichen, bei 3 eine Ziffer und ein Leerzeichen. Die 155 tragen Kontrollsätze von 504 statt 524 Zeichen und verschieden lange Datensätze; an Stelle 50 bis 58 stehen in jedem Datensatz Ziffern. Für den Nutzer heißt das: kein „(erkannt)", und wer IDoc wählt, liest „Diese Datei lässt sich nicht als IDoc darstellen" — über eine Datei, die ein IDoc ist. **Zu entscheiden vom Auftraggeber:** (a) so lassen — diese Dateien stehen schon mit einem Satz je Zeile da, die Darstellung brächte nur die Einrückung; (b) eine leere Ebene zulassen und solche Sätze nicht einrücken. Eine Ebene aus anderen Feldern herzuleiten wäre eine Deutung und ist hier nicht vorgeschlagen |
| **205** | **Der Download-Vermerk steht auch dort, wo die Darstellung nichts ändert** (M187‑2). Beim schon umbrochenen EDIFACT (`ZAST`, 52 Zeilen) und beim VDA mit Zeilenumbrüchen (13 Zeilen) ist das Ergebnis zeichengleich mit dem Original — und trotzdem steht „Der Download liefert die Originaldatei, nicht diese Darstellung". Die Auslegung in §4 begründet das Fehlen des Vermerks bei „passt nicht" gerade damit, dass ein Vermerk keinen Unterschied behaupten soll, den es nicht gibt; für VDA mit Zeilenumbrüchen trifft der Fall nach §3 immer zu. **Zu entscheiden:** den Vermerk an „Ergebnis ≠ Original" binden (ein Kennzeichen im Ergebnis von `stelleDar`, eine Bedingung in `darstellungsvermerke`, ein Fall in `tests/darstellung.test.ts`) oder die Auslegung in §4 um diesen Fall ergänzen · **Erledigt am 18.09.2026 (E‑206):** an „Ergebnis ≠ Original" gebunden, wie vorgeschlagen — Kennzeichen `weichtAb`, `darstellungsvermerke(ergebnis)`, sieben Fälle (§7); nachgeprüft im Browser (M187‑7) |
| **206** | **„ANSI X12 (detected)" passt im Englischen nicht ins Feld** (M187‑4). Die Auswahl ist `w-44` breit; die englische Beschriftung braucht in allen vier Dichten 3 px mehr, als da ist (bei `s` 127 gegen 124 px), und endet auf „(detecte…". Die deutsche „ANSI X12 (erkannt)" passt in allen vier. Abhilfe etwa `w-48` oder eine kürzere englische Fassung — zu entscheiden, nicht gebaut · **Erledigt am 18.09.2026 (E‑207):** `w-48`; der Eintrag passt in beiden Sprachen und allen vier Dichten (M187‑7) |
| **207** | **Das Feld nennt seinen Wert nicht im Barrierefreiheitsbaum** (M187‑4). Der Auslöser heißt dort „Darstellung" — aus dem `<Label htmlFor>`, auch nach der Wahl; der Inhalt „EDIFACT (erkannt)" ist nur die zweite Namensquelle und fällt damit weg, einen Wert trägt der Knopf nicht. Aus dem Baum gelesen, nicht mit einem Vorleseprogramm gehört. Das betrifft `AuswahlFeld` insgesamt, also auch Rolle und Baumgliederung: [`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18 sagt „ein `<button>` ist beschriftbar wie ein `<select>`" — beschriftbar ja, aber anders als beim `<select>` fällt der Wert dabei aus dem Namen. **Außerhalb dieses Auftrags**; zu beheben im Baustein |
| **208** | **Die Größengrenze der Ablage ist knapper, als ihre Begründung sagt** (M187‑1). `Ablagegrenzen` und [`rohdaten-backend.md`](rohdaten-backend.md) §3 stützen die 8 MiB auf M60 — größtes Artefakt 609.995 Bytes, „dreizehnfach darüber". Die fünf größten Nutzdateien der Stichprobe haben 5.503.419 bis 6.282.961 Bytes; die Grenze liegt damit nur noch rund 1,3-fach darüber. Eine größere Datei erschiene als *Ablage nicht erreichbar* (`Abrufzustand`: „unlesbar beziehungsweise zu gross"); in der Stichprobe stehen vier Dateien in diesem Zustand, und ob eine davon zu groß ist, sagt die Antwort nicht. **Zu prüfen:** die Dateigrößen über den Bestand — mit den Korrekturblöcken in `rohdaten-backend.md` §3 und an `Ablagegrenzen` als Folge. Außerhalb dieses Auftrags |

---

## 11. Die Sichtprüfung (Teil D) — M187

*18.09.2026.* Punkt 201 verlangt acht Schritte je Beispiel im angemeldeten Browser. Beispiele nennt
der Auftrag keine (§9 dort); sie stammen aus einer eigenen Stichprobe (M187‑1). **Alle acht Schritte
bestehen, an allen sechs Formaten.** Die fünf Befunde stehen als Punkte 204 bis 208 in §10; was beim
Hinsehen auffiel, ohne ein Befund zu sein, steht in M187‑6.

| Rahmen | |
|---|---|
| Stand | Arbeitsbaum von `feat/dateiansicht-darstellung`, wie in dieser Datei beschrieben und noch nicht committet; `next dev` auf `:3000`, Backend im Profil `dev` auf `:8080`, gestartet nach dem letzten Commit am Backend |
| Browser | das Chrome mit Debug-Port 9222 und eigenem Profil (angelegt am 16.09.2026), in dem sich der Nutzer als `admin` angemeldet hat. Das Chrome der Erweiterung war nicht angemeldet. Fenster 1.600 × 1.000, dunkles Thema, Dichte `s`, Deutsch — wo nicht anders gesagt |
| Steuerung | CDP: Klicks als `Input.dispatchMouseEvent`, Tasten als `Input.dispatchKeyEvent`, kein `element.click()`; Bilder über `Page.captureScreenshot`; Anfragen, Konsole und Protokoll über `Network`, `Runtime` und `Log`, mitgeschrieben über die ganze Dauer |
| Sichtbarkeit | **Das Fenster war zu Beginn minimiert** — `visibilityState: hidden`, das erste Bild hing. Über `Browser.setWindowBounds` wiederhergestellt, danach in jeder festgehaltenen Lage `visible`. Nur die allererste Lage (EDIFACT, Öffnen) ist verdeckt gelesen; ihre Werte hängen nicht an der Zeichnung, ihr Bild entstand nach dem Wiederherstellen |
| Gegenprobe | `features/nachrichten/darstellung/index.ts`, mit dem `rolldown` aus `frontend/node_modules` zu einem IIFE gebündelt und in die Seite eingesetzt. Verglichen wird das `<pre>` mit `stelleDar(text, wahl, kodierung).text` über den Text aus `…/inhalt`, dazu die Eigenschaft aus E‑197 am echten Text. **Dateiinhalt hat den Browser nicht verlassen** — zurück kamen Zahlen und Wahrheitswerte. Weder Skript noch Bild liegt im Repository |

### M187‑1 — die Stichprobe: was im Fenster vorkommt

Je Mandant der Prozessbaum über `2025-07-24` bis `2025-12-30`, je oberster Gruppe die zwei Prozesse
mit den meisten Nachrichten, davon die jüngste Nachricht im Fenster, davon alle Nutzdateien:
**1.625 Nutzdateien** aus acht Mandanten — `EDITIONLINGERI` und `NXHBE` tragen keine bei. Das ist
eine Stichprobe über die meistbewegten Prozesse, **keine Erhebung** (Regel L10).

| Zustand | Dateien |
|---|---|
| `ANZEIGBAR` | **1.451** — `ASCII` 1.125, `ISO_8859_1` 304, `UTF_8` 22 |
| `EBCDIC_DATEI` | 160 |
| `BINAERDATEI` | 10 |
| `ABLAGE_NICHT_ERREICHBAR` | 4 |

Keine leere Datei, keine *Datei nicht vorhanden*.

| erkannt, von 1.451 | Dateien | nach Mandant |
|---|---:|---|
| VDA | 386 | `NEXANS` |
| EDIFACT | 287 | `IBIS` 174, `NEXANS` 61, `IBISGUS` 21, `ZAST` 19, `VOTG` 8, `WOC` 4 |
| XML | 163 | `IBIS` 161, `IBISGUS` 2 |
| IDoc | 137 | `NEXANS` |
| ANSI X12 | 49 | `NEXANS` |
| JSON | 6 | `SUTTONS` |
| **nichts** | **423** | `NEXANS` 310, `ZAST` 48, `VOTG` 29, `IBISGUS` 19, `IBIS` 10, `WOC` 4, `SUTTONS` 2, `SYSTEM` 1 |

**Die 423 ohne Treffer, nach ihrem Anfang** — gesucht war, ob darunter Dateien eines der sechs
Formate sind, die die Erkennung verfehlt:

| Anfang | Dateien | Lesart |
|---|---:|---|
| `EDI_DC40` | 155 | **IDoc, nicht erkannt** — M187‑5, Punkt 204 |
| `ISA` | 2 | insgesamt höchstens 103 Zeichen, Trenner `\|` — kein ISA-Segment nach §3, richtig nicht erkannt |
| `UNB+` | 2 | kein `UNA`, kein `'` in der Datei, dafür 13 Zeilenumbrüche — nach §3 kein Segmentende, richtig nicht erkannt; die Datei steht ohnehin schon zeilenweise |
| `BGM`, `RFF`, `NAD` | je 2 | EDIFACT-Segmente ohne `UNA` oder `UNB` — richtig nicht erkannt |
| Kopfzeilen einer Mail | 1 | `UNB+` erst nach 188.852 Zeichen — richtig nicht erkannt |
| übrige | 257 | keines der sechs Merkmale am Anfang; darunter 150 mit `CHE`, 21 mit `R01`, 17 mit `SER` |

### M187‑2 — die acht Schritte, je Beispiel

| Beispiel | Mandant · Datei | Kodierung · Größe | „(erkannt)" an | gewählt: Zeilen, Zeichen | Hex | unpassend gewählt |
|---|---|---|---|---|---|---|
| EDIFACT, einzeilig | `NEXANS` · `OFTPReader.Payload.GUID` | ASCII · 1.605 Bytes | EDIFACT | 1 → 96, 1.605 → 1.700 | 101 Zeilen | XML |
| EDIFACT, schon umbrochen | `ZAST` · `Converter.Payload.GUID` | ISO-8859-1 · 1.676 | EDIFACT | 52 → 52, **zeichengleich** | — | — |
| ANSI X12 | `NEXANS` · `OFTPReader.Payload.GUID` | ASCII · 3.230 | ANSI X12 | 1 → 150, 3.230 → 3.379 | 202 | EDIFACT |
| VDA, ohne Umbruch | `NEXANS` · `OFTPReader.Payload.GUID` | ASCII · 1.536 | VDA | 1 → 12, 1.536 → 1.547 | 96 | JSON |
| VDA, mit Umbrüchen | `NEXANS` · `Converter.Payload.GUID` | ASCII · 1.548 | VDA | 13 → 13, **zeichengleich** | — | — |
| IDoc | `NEXANS` · `FileReader.Payload.GUID` | ISO-8859-1 · 11.176 | IDoc | 12 → 12, 11.176 → 11.196 (Einrückung) | 699 | VDA |
| XML | `IBIS` · `FTPReader.Payload.GUID` | ASCII · 1.513 | XML | 2 → 9, 1.513 → 1.540 | 95 | JSON |
| JSON | `SUTTONS` · `HTTPSender.Payload.GUID` | ASCII · 105 | JSON | 5 → 5, 105 → 101 — drei Leerzeichen vor `:` und eins in `[ ]` fallen weg | 7 | XML |
| nichts erkannt | `NEXANS` · `Converter.Payload.GUID` | ASCII · 1.732 | an keinem | Hex: 7 → 109 | gewählt | EDIFACT |
| IDoc mit leerer Ebene | `NEXANS` · `FileReader.Payload.GUID` | ASCII · 2.867 | an keinem | Hex: 17 → 180 | gewählt | **IDoc** |
| UTF-8 | `IBIS` · `AS2Reader.Payload.GUID` | UTF-8 · 640 Bytes, 638 Zeichen | EDIFACT | 1 → 18, 638 → 655 | **passt nicht** (E‑204) | Hex |
| groß, gekappt | `NEXANS` · `SAPReader.Payload.GUID` | ISO-8859-1 · 6.282.961 Bytes, Anzeige 1.048.576 Zeichen | IDoc | 986 → 986, 1.048.576 → 1.054.096 | 8.192 Zeilen, 647.167 Zeichen | XML |

In allen zwölf galt:

1. **Original beim Öffnen.** Auslöser „Original", das `<pre>` zeichengleich mit dem Text aus
   `…/inhalt`, kein Vermerk der Darstellung, die Adresse ohne `darstellung`. Bei der großen Datei
   steht der vorhandene Kappungsvermerk, sonst keiner.
2. **„(erkannt)" am richtigen Eintrag.** Die Liste in der Reihenfolge aus E‑193; der Zusatz genau am
   erkannten Format — zehnmal (EDIFACT dreimal, VDA und IDoc je zweimal, ANSI X12, XML, JSON je
   einmal) —, bei den zwei ohne Merkmal an keinem Eintrag, an Hex nie. Beim Öffnen der Liste steht
   der Fokus auf dem gewählten Eintrag.
3. **Korrekte Darstellung.** Das `<pre>` gleich `stelleDar(…)` in jeder Lage, mit genau einem Kind,
   einem Textknoten, und keinem Element darin. Die Eigenschaft aus E‑197 hält an allen zehn
   formatierten Lagen: ohne alle Zeilenumbrüche gleich bei EDIFACT, X12 und VDA, ohne Einrückung
   gleich bei IDoc, ohne jeden Leerraum gleich bei allen. Im Bild: EDIFACT und X12 ein Segment je
   Zeile, `UNA:+,? '` für sich; VDA ein Satz zu 128 Zeichen je Zeile; IDoc `E2EDK09003` (kleinste
   Ebene) ohne Einzug, `E2EDKA1003` mit zwei, `E2EDP16002` mit vier Leerzeichen; XML Einzug je Tiefe;
   JSON Einzug 2, `[]` kompakt, ein Leerzeichen hinter dem Doppelpunkt.
4. **Download-Vermerk** bei jeder angewandten Darstellung, auch bei Hex; nicht bei „passt nicht",
   nicht beim Original. Der Verweis bleibt `…/download` ohne Abfragezeichenkette, genau ein Knopf.
   Zum Vermerk bei zeichengleichem Ergebnis: Punkt 205, behoben mit E‑206 (M187‑7).
5. **Hex.** An allen neun Hex-Lagen aus dem Hex-Bereich zurückgerechnet gleich den Zeichencodes des
   Texts. An der Kappungsgrenze 131.072 Bytes in 8.192 Zeilen, 647.167 Zeichen, mit Vermerk — die
   drei Vermerke in der Reihenfolge aus §4: Kappung, Hex gekappt, Download. Bei UTF-8 das Original
   mit „passt nicht" (E‑204).
6. **Unpassende Darstellung.** Das Original, dazu „Diese Datei lässt sich nicht als {Darstellung}
   darstellen. Du siehst das Original." mit dem gewählten Namen — gesehen mit XML, EDIFACT, JSON,
   VDA, IDoc und Hex —, kein Download-Vermerk.
7. **Kopierte Adresse.** Neu geladen mit `?darstellung=…`: dieselbe Darstellung wie gewählt, der
   Auslöser mit „(erkannt)", wo erkannt, dieselben Vermerke.
8. **Keine Konsolenmeldung.** Keine Warnung, kein Fehler, keine Ausnahme, keine Antwort ab `400` aus
   der Ansicht; je Seitenaufruf nur `[HMR] connected` und der Hinweis auf die React DevTools. Eine
   `404` im Protokoll stammt aus einer eigenen Abfrage vor dem ersten Aufruf (Stichprobe unter dem
   falschen Mandanten), nicht aus der Ansicht.

Dazu, ebenfalls in allen zwölf: **Beim Wechseln der Darstellung ging keine Anfrage hinaus** — kein
`…/inhalt`, kein RSC-Abruf, kein Dokument (`shallow: true`, E‑196). **`history.length` stand über
alle Wechsel still** und wuchs nur beim Neuladen der Adresse um eins (`history: "replace"`) — belegt
an den ersten elf Beispielen, danach stand der Zähler an Chromes Obergrenze von 50. **Zurück auf
„Original" entfernt den Parameter** aus der Adresse (E‑202).

### M187‑3 — wo keine Auswahl steht (E‑194)

| Lage | geöffnet mit | Auswahl | angezeigt | Vermerk |
|---|---|---|---|---|
| Protokoll `OFTPReader.Log.GUID`, `ANZEIGBAR`, ASCII, 915 Bytes | `?darstellung=hex` | keine | der Text, zeichengleich | keiner |
| `EBCDIC_DATEI`, 512 Bytes | `?darstellung=edifact` | keine | Zustandsfeld | keiner |
| `BINAERDATEI`, 68.168 Bytes | `?darstellung=hex` | keine | Zustandsfeld | keiner |
| `ABLAGE_NICHT_ERREICHBAR` | `?darstellung=hex` | keine | Zustandsfeld, kein Download | keiner |
| die EDIFACT-Datei aus M187‑2 | `?darstellung=foo` | „Original" | das Original | keiner |

Die leere Datei, *Datei nicht vorhanden* und *kein anzeigbarer Protokollteil* kamen in der
Stichprobe nicht vor; sie decken die Tests (§7).

### M187‑4 — Bedienung, Farbe, Beschriftung, Verweise, Rechenzeit

- **Tastatur** (EDIFACT-Beispiel): Fokus auf dem Feld, Pfeil nach unten öffnet die Liste mit dem
  Fokus auf „Original", ein weiterer Pfeil nach unten führt zu „EDIFACT (erkannt)", Enter wendet an
  (`?darstellung=edifact`), die Liste schließt, der Fokus steht wieder auf dem Feld. Escape schließt
  ohne Änderung, der Fokus kehrt zurück.
- **Barrierefreiheitsbaum:** Rolle `button`, `hasPopup` `listbox`, `expanded` falsch; der Name ist
  „Darstellung" aus dem `<label for>`, auch nach der Wahl. Der Inhalt ist nur die zweite
  Namensquelle, einen Wert gibt es nicht — Punkt 207.
- **Farbe:** Die drei neuen Vermerke tragen Schriftfarbe und Rand des vorhandenen Kappungsvermerks,
  Ziffer für Ziffer — `lab(65.2 0 0)`, Rand `lab(100 0 0 / 0.12)`, dunkles Thema. Keine neue Rolle.
- **Beschriftung in vier Dichten, zwei Sprachen** — der längste Eintrag im Feld `w-44`:

  | Dichte | Feld | „ANSI X12 (erkannt)" | „ANSI X12 (detected)", Text gegen Platz |
  |---|---:|---|---|
  | `xs` | 154 px | passt | **gekürzt**, 118 gegen 115 px |
  | `s` | 165 px | passt | **gekürzt**, 127 gegen 124 px |
  | `m` | 176 px | passt | **gekürzt**, 135 gegen 132 px |
  | `l` | 198 px | passt | **gekürzt**, 152 gegen 149 px |

  Sonst trägt das Englische vollständig: „View", die drei Vermerke, die Liste ohne Kürzung (0 von
  8 Einträgen). Punkt 206, behoben mit E‑207 (M187‑7). Sprache und Dichte über ihre Cookies
  gesetzt und danach auf den Stand vorher zurückgestellt (keine Sprache gesetzt, Dichte `s`).
- **Verweise:** „Zurück zur Nachricht" zeigt bei `?darstellung=edifact` auf `/nachrichten/{id}`
  ohne Parameter; im Nachrichtendetail führen alle sechs Verweise ohne Abfragezeichenkette in die
  Dateiansicht ([`rohdaten-frontend.md`](rohdaten-frontend.md) §8).
- **Rechenzeit, beobachtet und nicht gemessen** (Punkt 203): am 1-MiB-Text der großen Datei im
  Browser des Prüflaufs, Entwicklungsbau, Median aus fünf Läufen — `erkenneFormat` 0,6 ms,
  `stelleDar` IDoc 0,6 ms, Hex 11,9 ms.

### M187‑5 — die IDoc-Frage (Punkt 196)

| | erkannt | nicht erkannt |
|---|---:|---:|
| Nutzdateien, Anfang `EDI_DC40` | 137 | 155 |
| davon mit Zeilenumbrüchen | 137 | 155 |
| Kontrollsatz | 524 Zeichen, alle | 504 Zeichen; eine Datei daneben einer mit 480 |
| Datensätze | 1.063 Zeichen; in 19 Dateien daneben kürzere | verschieden lang |
| Stelle 62–63 | Ebene `01` bis `05` | zwei Leerzeichen (152), eine Ziffer und ein Leerzeichen (3) |
| Stelle 50 bis 58 | — | Ziffern, in jedem Datensatz |

Die Stellen aus der SAP-Satzstruktur stimmen, wo die Ebene gefüllt ist. Die zweite Spielart trägt
keine Ebene; im Bild steht sie schon mit einem Satz je Zeile da, `E2EDK09003` bis `E2EDP16`
linksbündig. Eine Datei ohne Zeilenumbrüche kam in keiner der beiden vor — die Teilung nach
524/1.063 bleibt ungeprüft (Punkt 196).

### M187‑6 — gesehen, ohne Befund zu sein

- **Die Einrückung geht im Umbruch unter.** IDoc-Datensätze sind 1.063 Zeichen breit und brechen im
  `<pre>` mehrfach um; die Folgezeilen beginnen am linken Rand. Zwei Leerzeichen je Ebene sind
  daneben kaum zu sehen, und eine Folgezeile sieht aus wie ein neuer Satz. Schwächer dasselbe bei
  XML-Starttags mit vielen Attributen. Die Regel ist erfüllt — `pre-wrap` bleibt (§4) —, und jede
  Abhilfe, ob hängender Einzug oder waagerechter Bildlauf, bräche den einen Textknoten oder
  [`rohdaten-frontend.md`](rohdaten-frontend.md) §7. Festgehalten für den Auftraggeber, kein
  offener Punkt.
- **Das X12-Segmentende ist ein Steuerzeichen.** Die Beispieldatei trennt Elemente mit `~`,
  Komponenten mit `^` und schließt ihre 150 Segmente mit `0x1C`; es erscheint am Zeilenende als
  Kästchen, im Original an denselben Stellen ebenso. Kein Befund der Darstellung.
- **Im Original bricht der Browser hinter `UNA:+,? ` um** — am Leerzeichen, das dort das reservierte
  Zeichen ist —, und die zweite Bildzeile beginnt mit `'UNB`. Das ist `pre-wrap` am Original; die
  EDIFACT-Darstellung setzt `UNA:+,? '` auf eine eigene Zeile.

### M187‑7 — Nachprüfung nach E‑206 und E‑207

*18.09.2026, am selben Tag.* Derselbe Rahmen, `next dev` mit dem geänderten Stand; dieselben
Dateien wie in M187‑2, je frisch geladen mit `?darstellung=…`. Das Fenster war dabei verdeckt
(`hidden`); gelesen sind Textvergleiche und Layoutmaße, die nicht an der Zeichnung hängen, und die
drei Bilder entstanden trotzdem.

| Lage | in M187‑2 | jetzt |
|---|---|---|
| EDIFACT, schon umbrochen (`ZAST`), EDIFACT gewählt | Download-Vermerk über zeichengleichem Text | **kein Vermerk**, `<pre>` zeichengleich mit dem Original |
| VDA mit Umbrüchen, VDA gewählt | Download-Vermerk über zeichengleichem Text | **kein Vermerk**, zeichengleich |
| EDIFACT, einzeilig, EDIFACT gewählt | Download-Vermerk | Download-Vermerk |
| dieselbe Datei, Hex | Download-Vermerk | Download-Vermerk |
| dieselbe Datei, XML | „passt nicht" | „passt nicht" |
| dieselbe Datei, Original | keiner | keiner |

| Dichte | Feld, `w-48` | „ANSI X12 (erkannt)" | „ANSI X12 (detected)" |
|---|---:|---|---|
| `xs` | 168 px | passt, 111 px | **passt**, 118 px |
| `s` | 180 px | passt, 119 px | **passt**, 127 px |
| `m` | 192 px | passt, 127 px | **passt**, 135 px |
| `l` | 216 px | passt, 142 px | **passt**, 152 px |

Keine Konsolenmeldung außer denen der Entwicklungsumgebung; Sprache und Dichte danach wieder auf
dem Stand vorher.
