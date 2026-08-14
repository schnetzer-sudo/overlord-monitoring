# Messungen vor Schritt 7 — BAM-Suche

Erhoben am **11.08.2026** gegen die Testkopie (MariaDB 10.6.22); **M42 und M43 nachgetragen am
12.08.2026**, **M44 ebenfalls am 12.08.2026** — beide Nachträge gegen dieselbe, unverändert
nachgewiesene Testkopie.
Bezugsdokumente: [`messungen-schritt4.md`](messungen-schritt4.md) (M0–M13, L1–L15),
[`messungen-schritt5.md`](messungen-schritt5.md) (M14–M22, M29),
[`messungen-schritt6.md`](messungen-schritt6.md) (M23–M28, M30, M31, E1–E5),
[`datenzugriff.md`](datenzugriff.md) §5, [`nachrichtenliste.md`](nachrichtenliste.md) §6,
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 und §8.

> ### 📌 Nachtrag vom 12.08.2026 — M42 und M43
>
> **M42** (Kosten der Verundung mehrerer Suchbegriffe) und **M43** (Womit wird normalisiert?) sind
> am **12.08.2026** nachgereicht worden. Sie stehen unten zwischen M41 und der Zusammenfassung.
>
> **Der Anlass sind zwei Entscheidungen aus dem Sparring nach der Hauptrunde**, die je eine Zahl
> offen ließen, die es noch nicht gab: Die Suche verknüpft mehrere Begriffe mit **UND**, ohne
> Deckelung bei zwei — dafür fehlte, was ein zusätzlicher Begriff kostet. Und sie arbeitet
> **exakt**, mit einer typgebundenen, sichtbar gemachten Normalisierung — dafür fehlte die
> **Sollänge je Typ**, weil M38 nur den Anteil der Werte mit führender Null misst und nicht die
> Länge dahinter.
>
> **Die Hauptrunde vom 11.08.2026 bleibt unverändert.** Keine Zahl aus M32 bis M41 und E6 ist
> angefasst worden. Fortgeschrieben sind ausschließlich die zusammenfassenden Abschnitte am Ende
> (Frage → Antwort, Laufzeiten, „Wo die vorformulierte Zeile nicht passte", „Was diese Runde
> ausdrücklich nicht misst", „Offene Fragen") und dieser Kopf. Der Nachtrag fand **keinen**
> Widerspruch zur Hauptrunde; die Kontrolle steht unter „Rahmen des Nachtrags vom 12.08.2026".

> ### 📌 Zweiter Nachtrag vom 12.08.2026 — M44
>
> **M44** (Wie groß ist `MessageProperty` wirklich?) ist am **12.08.2026** nachgereicht worden, nach
> M42 und M43 und in einer eigenen Sitzungsfolge. Sie steht unten zwischen M43 und der
> Zusammenfassung.
>
> **Der Anlass ist M33‑0 und die Entscheidung, die daraus folgte.** M33‑0 fand `MessageBAM` um 41,9 %
> größer als in der verbindlichen Datei ausgewiesen und stellte die Korrektur als **offene Frage 1**
> ins Sparring. Die Antwort lautet: korrigieren — und dabei ist aufgefallen, dass nicht eine Zeile
> des Mengengerüsts falsch war, sondern **seine Überschrift**. `MessageProperty` war als einzige der
> vier großen Tabellen nie gezählt worden; M14 hatte den Zähllauf ausdrücklich als den Aufwand nicht
> wert abgelehnt. M44 holt ihn nach.
>
> **Hauptrunde und erster Nachtrag bleiben unverändert.** Keine Zahl aus M32 bis M43 und E6 ist
> angefasst worden. Fortgeschrieben sind erneut nur die zusammenfassenden Abschnitte am Ende und
> dieser Kopf. Auch der zweite Nachtrag fand **keinen** Widerspruch zu den beiden früheren: Die
> Bytegrößen sind zum dritten Mal byteidentisch, und `MessageProperty` gleicht darüber hinaus auf das
> Byte der Erhebung vom 07.08.2026 — fünf Messtage ohne Neubefüllung.
>
> **Anders als die beiden Nachträge zuvor ändert dieser die verbindlichen Dateien.** Was daraus in
> `PROJEKTBESCHREIBUNG.md`, `datenmodell.md` und `annahmen-korrekturen.md` geworden ist, steht dort
> datiert und mit den alten Zahlen daneben; diese Datei bleibt die Erhebung und trifft keine
> Entscheidung.

> ### 📌 Dritter Nachtrag vom 13.08.2026 — M45 und E7
>
> **M45** (Sind die Typbeschreibungen ohne ihre Endung noch eindeutig?) und **E7** (Wie viele
> BAM-Werte tragen ein Leerzeichen innen?) sind am **13.08.2026** nachgereicht worden, in einer
> eigenen Sitzungsfolge. Sie stehen unten zwischen M44 und der Zusammenfassung.
>
> **E7 stand nicht im Auftrag und ist trotzdem entstanden**, weil die Nacharbeit eine Bauform mit
> einem Satz begründete, den niemand gemessen hatte: *„Werte enthalten Leerzeichen."* Nach Regel L10
> ist ein solcher Satz ohne Beleg ein Befund ohne Vermerk. Die Messung kostet zwei Statements.
>
> **Der Anlass ist ein offener Punkt aus [`bam-werte.md`](bam-werte.md) §13**, und er ist in der
> Nacharbeit zu Schritt 7, Teil 1 sichtbar geworden: Auf der abgenommenen Nachricht enden **alle
> fünf** Beschriftungen auf `_K_SAP`. Die Endung unterscheidet dort nichts und kostet in jeder Zeile
> Platz — nach dem Leitsatz ist sie Beiwerk. Ob die Beschreibungen **ohne** sie eindeutig bleiben,
> war nie gemessen; eine Kürzungsregel wäre nach Regel Q4 geraten gewesen.
>
> **Diese Messung ist die kleinste der ganzen Runde** und die einzige, die ihren Hauptbefund aus
> einer reinen **Stammdatentabelle** von 62 Zeilen zieht. Zwei Kontrollabfragen gehen darüber
> hinaus — sie klären, ob eine Kollision je auf dem Schirm zusammenträfe, und genau die eine tut es.
>
> **Hauptrunde und die beiden früheren Nachträge bleiben unverändert.** Keine Zahl aus M32 bis M44
> und E6 ist angefasst worden. Fortgeschrieben sind erneut nur die zusammenfassenden Abschnitte am
> Ende und dieser Kopf. **Die Anzeige ändert dieser Nachtrag nicht**: Die Beschreibungen bleiben
> vollständig stehen, und M45 trifft keine Entscheidung darüber.

> ### 📌 Vierter Nachtrag vom 13.08.2026 — M46
>
> **M46** (Woher kommt die Sollänge, und gilt sie je Mandant?) ist am **13.08.2026** nachgereicht
> worden, nach M45 und E7 und in einer eigenen Sitzungsfolge. Sie steht unten zwischen E7 und der
> Zusammenfassung.
>
> **Der Anlass ist Schritt 7, Teil 2a** — die erste Messung dieser Runde, die **gebaut** wird und
> nicht nur erhebt: Aus ihr entsteht `overlord_monitor.bam_sollaenge` (`V5__bam_sollaenge.sql`).
> M43 hatte die Sollänge für sechs Typen über den Bestand belegt und dabei gezeigt, dass eine aus
> einem Monat abgeleitete Kuratierung falsch sein kann (Typ 2001). Zwei Zahlen fehlten trotzdem:
> die Dominanz über den Bestand für **alle** Typen, und die Antwort darauf, ob die Sollänge je
> **Mandant** dieselbe ist. Die zweite war ungemessen — und sie fällt anders aus als erwartet.
>
> **Diese Messung trifft, anders als die drei Nachträge zuvor, eine Entscheidung** — nämlich welche
> Zeilen in der Kuratierungstabelle stehen. Sie fällt mechanisch nach der 95-Prozent-Regel aus dem
> Auftrag; wo die Regel an eine Grenze stößt, steht das als Befund und nicht als Nachbesserung.
> Was daraus gebaut worden ist, steht in [`bam-sollaengen.md`](bam-sollaengen.md).
>
> **Hauptrunde und die drei früheren Nachträge bleiben unverändert.** Keine Zahl aus M32 bis M45,
> E6 und E7 ist angefasst worden. Fortgeschrieben sind erneut nur die zusammenfassenden Abschnitte
> am Ende und dieser Kopf. Auch der vierte Nachtrag fand **keinen** Widerspruch: Die Bytegrößen sind
> zum **sechsten** Mal byteidentisch, und die sechs Bestandszeilen aus M43‑1 reproduzieren Zeile für
> Zeile.

> ### 📌 Fünfter Nachtrag vom 13.08.2026 — M47
>
> **M47** (Was kostet das gebaute Suchstatement?) ist am **13.08.2026** nachgereicht worden, nach M46
> und in fünf eigenen Sitzungen. Sie steht unten zwischen M46 und der Zusammenfassung.
>
> **Der Anlass ist Schritt 7, Teil 2b** — und der Unterschied zu allen vorherigen Messungen dieser
> Runde ist der Gegenstand: **M47 misst nicht, was gebaut werden könnte, sondern was gebaut ist.**
> Der gemessene Text ist aus dem Repository gegen eine jOOQ-Attrappe gerendert; er unterscheidet sich
> an keiner Stelle vom ausgelieferten. Sie schließt damit zwei ausdrücklich offene Zahlen: ob das
> Pflicht-Zeitfenster die beiden Bösfälle der Verundung auffängt (offene Frage 10) und was eine
> Suchvariante kostet, die nichts trifft ([`bam-sollaengen.md`](bam-sollaengen.md) §8, Punkt 5).
>
> **Hauptrunde und die vier früheren Nachträge bleiben unverändert.** Keine Zahl aus M32 bis M46,
> E6 und E7 ist angefasst worden. Fortgeschrieben sind erneut nur die zusammenfassenden Abschnitte
> am Ende und dieser Kopf. Die Bytegrößen sind zum **siebten** Mal byteidentisch, und beide
> Prüfwert-Herleitungen aus M42 reproduzieren Zeile für Zeile — Trefferzahlen, Wertzahl der fetten
> Nachricht und die 2.499 BAM-Werte je Kandidatennachricht.

**Nummerierung ab M32.** Geprüft über `docs\` und die Dateien im Wurzelverzeichnis: **M31** ist die
höchste projektweit vergebene Nummer (`messungen-schritt6.md`, Block 4); **M32 bis M41** kommen in
keiner anderen Datei vor. Die Zählung läuft projektweit fort und nicht je Datei. Die ergänzende
Messung dieser Runde heißt **E6**, weil E1 bis E5 in `messungen-schritt6.md` vergeben sind.
**M42 und M43** sind am 12.08.2026 nachgetragen; vor der Vergabe geprüft, dass sie projektweit in
keiner Datei vorkommen — **M41** war die höchste vergebene Nummer, **E6** die höchste ergänzende.
**M44** ist am selben Tag nachgetragen; ebenso geprüft — **M43** war zu diesem Zeitpunkt die höchste
projektweit vergebene Nummer, **E6** unverändert die höchste ergänzende.
**M45** und **E7** sind am 13.08.2026 nachgetragen; vor der Vergabe über `docs\` und das
Wurzelverzeichnis geprüft — **M44** war die höchste projektweit vergebene Nummer, **E6** die höchste
ergänzende.
**M46** ist am selben Tag nachgetragen; ebenso geprüft — **M45** war zu diesem Zeitpunkt die höchste
projektweit vergebene Nummer, **E7** die höchste ergänzende.
**M47** ist am selben Tag nachgetragen; ebenso geprüft — **M46** war zu diesem Zeitpunkt die höchste
projektweit vergebene Nummer, **E7** unverändert die höchste ergänzende. Eine neue ergänzende Nummer
ist nicht vergeben worden.

**Diese Runde baut nichts.** Kein Endpunkt, keine Migration, keine Oberfläche, keine Änderung an
vorhandenem Code — und **keine Entscheidung**. Die Lesarten standen vor der Erhebung fest; welche
Fassung gebaut wird, entscheidet der Auftraggeber danach. Das gilt für den Nachtrag ebenso: Er
stellt fest, was die Verundung kostet und womit normalisiert werden **könnte** — er wählt weder die
Bauform noch die Kuratierung.

> **Ab dem vierten Nachtrag gilt dieser Satz nicht mehr uneingeschränkt**, und das ist keine
> Aufweichung, sondern eine Zäsur: **M46** entscheidet, welche Zeilen in der Kuratierung stehen, und
> **M47** misst ein Statement, das bereits **gebaut** ist. Beide bleiben trotzdem hier statt in einer
> eigenen Datei — die Nummernfolge läuft projektweit, und eine Messung von ihrer Runde zu trennen
> hieße, ihre Bezugszahlen zu verlieren.

---

## Warum diese Runde nötig war

Jeder bisherige Zugriff auf `MessageBAM` lief **ausschließlich über `MessageID`** — M11, M26‑1b und
M28‑2 jedes Mal als `ref` über das Präfix des Primärschlüssels, jedes Mal `Using index`. Schritt 7
dreht diesen Zugriff um und steigt über `MessageBAMValue` ein. Für diesen Pfad existierte in diesem
Projekt keine einzige Zahl. `MessageBAM` war zudem die einzige nicht erhobene Tabelle, die einen
MVP-Schritt trägt (Regel L8, Warnhinweis in `DEVELOPMENT_GUIDELINES.md` §4.4).

> **Das Ergebnis vorweg, in vier Sätzen.** Der Index auf `MessageBAMValue` ist ein **Vollindex** über
> 70 Zeichen, die Suche ist ohne Zusatzbau möglich, und die Rückfalloption aus Annahme A9 wird nicht
> gebraucht. Das **99. Perzentil liegt bei 75 Treffern**, das Maximum bei **234.159** — und genau
> dieser eine Wert kostet ohne Zeitfenster **10,6 Sekunden** und reißt damit die Zeitgrenze des
> Lese-Pools. Das Pflicht-Zeitfenster aus Regel L1 **wirkt hier** — aber nur eng: ein Tag senkt den
> schlimmsten Fall auf 90 ms, ein Jahr nur auf 8,7 s. Und die Kostengröße ist durchgängig die
> **Trefferzahl**, nicht die Suchform: Präfix kostet so viel wie exakt, solange das Präfix so lang
> ist wie der Wert — bei vier statt acht Zeichen steigt die Trefferzahl von 1 auf 155.871.

---

## Die Lesarten standen vor der Messung fest

Jede Messung trägt einen Abschnitt **„Was daraus folgt"** mit den Befunden, die **vor** der Erhebung
aufgeschrieben wurden. Nachträglich ergänzt ist ausschließlich die Spalte **„trifft zu"**. Wo ein
gemessener Befund in keine vorformulierte Zeile passt, steht er als eigener Absatz darunter — und
nicht als passend gemachte Zeile. Das ist an **einundzwanzig** Stellen der Fall (elf in der
Hauptrunde, sieben im ersten und drei im zweiten Nachtrag vom 12.08.2026); sie sind am Ende gezählt
und einzeln benannt.

---

## 0. Rahmen der Messung

Übernommen aus [`messungen-schritt6.md`](messungen-schritt6.md) §0, unverändert:

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` (Ubuntu 22.04) — niemals die Produktion |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**. **Erste Abfrage der Sitzung**, vor jedem anderen Statement. Am Ende der Runde erneut geprüft: wieder **`1`** |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Serverzeit zu Beginn | `2026-08-11 15:43:52` (`UTC_TIMESTAMP` `13:43:52`, also UTC+2) |
| Serverzeit am Ende | `2026-08-11 16:57:39` (`UTC_TIMESTAMP` `14:57:39`) |
| Client | `mysql` `8.0.46` aus MySQL Workbench mit `--ssl-mode=DISABLED` — derselbe wie in Schritt 4, 5 und 6, damit die Zahlen vergleichbar bleiben |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `SHOW PROFILES` — nie um den Client-Aufruf herum. `SET profiling_history_size = 100`, weil die Vorgabe 15 für die Reihenläufe nicht reicht |
| Wiederholungen | beste von fünf nach einem Aufwärmlauf; bei Statements über zehn Sekunden beste von drei |
| Obergrenze | kein Statement über **60 Sekunden**. **Zweimal gerissen und einmal abgebrochen** — die drei Fälle stehen unten und sind Ergebnisse, keine Fehlschläge |
| Zugangsdaten | ausschließlich aus `OVERLORD_DB_*`, an den Client über `MYSQL_PWD` |

### `SHOW GRANTS FOR CURRENT_USER()`

Drei Zeilen, unverändert gegenüber Schritt 4, 5 und 6. Der Passwort-Hash der ersten wird nach
Regel G1 **nicht** abgedruckt:

```
GRANT USAGE ON *.* TO `monitor_read`@`%` IDENTIFIED BY PASSWORD '<Hash>'
GRANT SELECT ON `GlassfishDB`.* TO `monitor_read`@`%`
GRANT SELECT ON `overlord_monitor`.* TO `monitor_read`@`%`
```

Zusätzlich erhoben, weil beides für diese Runde zählt: `@@global.max_statement_time` ist **`0`**
(keine serverseitige Grenze; die 60 Sekunden mussten je Sitzung über `SET max_statement_time = 60`
gesetzt werden), `@@global.event_scheduler` ist **`ON`** — unverändert gegenüber M31‑3. `@@sql_mode`
enthält **kein** `ONLY_FULL_GROUP_BY`, weshalb das `GROUP BY m.MessageID` der gemessenen Statements
so laufen darf, wie es unten steht.

### Bezugsfenster — unverändert übernommen

| | Zeitraum | Bezug |
|---|---|---|
| **Fenster A (Tag)** | `2025-12-29 00:00:00` ≤ x < `2025-12-30 00:00:00` | **6.249** Nachrichten |
| **Fenster B (Monat)** | `2025-11-30 00:00:00` ≤ x < `2025-12-30 00:00:00` | **214.330** Nachrichten |
| **ein Jahr** | `2024-12-30 00:00:00` ≤ x < `2025-12-30 00:00:00` | nur in M35; endet wie Fenster B, Form wie L13 |

**Die Testkopie ist seit dem 07.08.2026 unverändert**, geprüft und nicht angenommen:

| | `DATA_LENGTH` | `INDEX_LENGTH` |
|---|---:|---:|
| `Message` | 740.851.712 | 2.157.330.432 |
| `MessageAction` | 2.226.634.752 | 819.855.360 |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 |

`Message` und `MessageAction` sind **byteidentisch** mit M23‑1 (10.08.) und M31‑0 (11.08.). Die
Zahlen dieser Runde dürfen deshalb ohne Vorbehalt gegen M0 bis M31 gehalten werden. Eine zweite,
unabhängige Kontrolle liefert **M39**: Die Rollenverteilung je Mandant über Fenster B reproduziert
M28‑1 (a) **Zeile für Zeile**.

**Regel L7 (mindestens zwei Mandanten, darunter ein kleiner)** ist in M34 (`NEXANS`, `IBIS`, `ZAST`),
M39 (alle acht Mandanten mit Nachrichten), M40 (alle zehn) und M41 erfüllt.
**Regel S1:** kein `CREATE`, kein `ALTER`, kein Index — ausschließlich `SELECT`, `SHOW` und
`EXPLAIN`. Der einzige nicht lesende Befehl der ganzen Runde war ein `KILL QUERY` auf die **eigene**
verwaiste Abfrage (siehe „Die Auswahl der Prüfwerte"); er fasst `GlassfishDB` nicht an.

### Anonymisierung

Keine Belegnummer und kein BAM-Wert steht in diesem Dokument. Die Prüfwerte der Messungen M34 bis
M36 sind **nie aus der Datenbank herausgekommen**: Sie stehen in Sitzungsvariablen (`@w_typisch`,
`@w_unangenehm`, `@w_schlimm`, `@w_ibis`, `@w_zast`), die auf dem Server gesetzt und dort auch
verbraucht werden. Abgedruckt sind nur die Auswahlabfrage und die Eigenschaften.

Typnummern, die Beschreibungen aus `MessageBAMType`, Mandantenkürzel und alle Zahlen sind nach der
Regel in [`README.md`](README.md) Konfigurationsvokabular und bleiben stehen.

---

## Die Auswahl der Prüfwerte

Verfahren wie in L13, wo dieselbe Lage bei den Freitextbegriffen bestand.

### Die Auswahlabfrage

Alle fünf Werte entstehen aus **derselben** Abfrage mit anderer Sortierung. Die Kandidatenmenge sind
die BAM-Werte auf den Nachrichten eines Mandanten im Bezugsfenster; die Trefferzahl daneben ist die
**globale** über `MessageBAM_BAMValueOnly` — dieselbe Zahl, die der spätere Endpunkt bezahlt:

```sql
SELECT k.v INTO @w_typisch FROM (
  SELECT b.MessageBAMValue AS v,
         (SELECT COUNT(*) FROM MessageBAM b2 WHERE b2.MessageBAMValue = b.MessageBAMValue) AS treffer
  FROM Message m JOIN MessageBAM b ON b.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
    AND EXISTS (SELECT 1 FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
                WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
  GROUP BY b.MessageBAMValue) k
ORDER BY ABS(k.treffer - 1), k.v LIMIT 1;      -- typisch:    Median aus M33 = 1
-- ORDER BY ABS(k.treffer - 75), k.v LIMIT 1;  -- unangenehm: 99. Perzentil aus M33 = 75
-- ORDER BY k.treffer DESC, k.v LIMIT 1;       -- schlimm:    hoechste Trefferzahl
```

`IBIS` und `ZAST` laufen über **Fenster B** statt A, weil `ZAST` in Fenster A keine Zeile hat. Die
zweite Sortierstufe `k.v` macht die Auswahl reproduzierbar; ohne sie wäre bei Gleichstand der
Trefferzahl offen, welcher Wert gewinnt.

**Laufzeit:** 1,569 bis 1,680 s je Wert.

### Was die fünf Werte sind

| Rolle im Test | Mandant | Typ | Trefferzahl | Länge | Gestalt |
|---|---|---:|---:|---:|---|
| **der typische** | `NEXANS` | 9017 ((JIT-) Abrufnummer_K_SAP) | **1** | 8 | rein numerisch, **mit führender Null** |
| **der unangenehme** | `NEXANS` | 9017 | **75** | 9 | rein numerisch, **mit führender Null** |
| **der schlimmste** | `NEXANS` | 9014 (Lieferantennummer beim Kunden_K_SAP) | **234.159** | 8 | rein numerisch, ohne führende Null |
| typisch | `IBIS` | 0 (Bestellnummer) | **1** | 10 | rein numerisch, **mit führender Null** |
| typisch | `ZAST` | 3 (Rechnungsnummer) | **1** | 8 | rein numerisch, ohne führende Null |

Jeder der fünf Werte kommt unter **genau einem** Typ vor.

**Der schlimmste ist wirklich der schlimmste.** Seine Trefferzahl **234.159** ist identisch mit dem
Maximum, das M33 über den **gesamten** Bestand misst. Dass die Kandidatenmenge aus einem einzigen
Tag den globalen Ausreißer enthält, ist kein Zufall — Typ 9014 ist eine Kundenkennung und steht auf
fast jeder `NEXANS`-Nachricht.

> **Drei der fünf Werte tragen eine führende Null.** Das ist an dieser Stelle noch ein Nebenbefund;
> M38 misst, wie systematisch es ist.

### Die erste Fassung der Auswahlabfrage — abgebrochen nach 679 s

Die naheliegende Form suchte Werte mit **exakt** der Zielhäufigkeit und prüfte danach den Mandanten:

```sql
SELECT MIN(t.v) INTO @w_unangenehm
FROM (SELECT MessageBAMValue AS v FROM MessageBAM GROUP BY MessageBAMValue
      HAVING COUNT(*) = 75 ORDER BY v LIMIT 200) t
JOIN MessageBAM b ON b.MessageBAMValue = t.v
JOIN Message m ON m.MessageID = b.MessageID
JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID AND pm.MandantID = 'NEXANS';
```

Sie lief **679 Sekunden** ohne Ergebnis und wurde abgebrochen. `LIMIT 200` in der abgeleiteten
Tabelle begrenzt die **Ausgabe** der Untermenge, nicht die Arbeit des Optimierers darüber — derselbe
Mechanismus, den M30‑1 für `LIMIT 51` beschreibt, eine Ebene höher.

Der Abbruch ist **client-seitig** erfolgt; die Serverabfrage lief weiter, weil ein
`SELECT … INTO @var` keine Zeile schreibt und den Verbindungsabbruch deshalb nicht bemerkt. Beendet
wurde sie mit `KILL QUERY` auf die **eigene** Verbindung — dazu genügen die Rechte des Lesebenutzers,
und `GlassfishDB` ist davon nicht berührt (Regel S1). Danach war der Server nachweislich frei
(`information_schema.PROCESSLIST` leer), bevor die erste Laufzeitmessung startete.

> **Das gehört hierhin und nicht in eine Fußnote.** Es ist der einzige Abbruch der Runde, und es ist
> die Form, die man beim Nachvollziehen zuerst probieren würde.

---

# M32 — Wie ist der Index auf `MessageBAMValue` beschaffen?

**Frage.** Voll- oder Präfixindex, welche Kardinalität, und gibt es einen zusammengesetzten Index,
der `MessageBAMType` mitträgt?

## Statement

```sql
SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART, CARDINALITY, NULLABLE, INDEX_TYPE, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageBAM'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLLATION_NAME, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageBAM'
ORDER BY ORDINAL_POSITION;

SHOW INDEX FROM GlassfishDB.MessageBAM;
SHOW CREATE TABLE GlassfishDB.MessageBAM;
```

## Ergebnis — Spalten

| Spalte | Typ | `NULL`? | Vorgabe | Sortierung | Schlüssel |
|---|---|---|---|---|---|
| `MessageID` | `varchar(36)` | NO | — | `utf8mb4_general_ci` | PRI |
| `MessageBAMType` | `smallint(6)` | NO | — | — | PRI |
| `MessageBAMValue` | `varchar(70)` | NO | — | `utf8mb4_general_ci` | PRI |

Die Angaben aus [`datenmodell.md`](datenmodell.md) §3 — Primärschlüssel
`(MessageID, MessageBAMType, MessageBAMValue)`, `MessageBAMValue varchar(70)`, eigener Index — sind
damit **bestätigt**. Es ist die erste Erhebung dieser Tabelle; bisher war die Angabe übernommen
(Regel L8).

**Alle drei Spalten sind `NOT NULL`.** Ein leerer BAM-Wert wäre also der leere String und nicht
`NULL` — im Gegensatz zu den Verkettungsspalten aus M23‑2, wo es umgekehrt ist.

## Ergebnis — Indizes

**Fünf Indizes, keiner davon ein Präfixindex.** `SUB_PART` ist durchgängig `NULL`:

| `INDEX_NAME` | `SEQ_IN_INDEX` | `COLUMN_NAME` | `SUB_PART` | `CARDINALITY` | eindeutig |
|---|---:|---|---|---:|---|
| `PRIMARY` | 1 | `MessageID` | — | 1.357.458 | ja |
| `PRIMARY` | 2 | `MessageBAMType` | — | 10.859.666 | ja |
| `PRIMARY` | 3 | `MessageBAMValue` | — | 10.859.666 | ja |
| `MessageBAM_MessageID` | 1 | `MessageID` | — | 1.809.944 | nein |
| `MessageBAM_MessageID` | 2 | `MessageBAMType` | — | 10.859.666 | nein |
| **`MessageBAM_BAMValue`** | 1 | **`MessageBAMType`** | — | 18 | nein |
| **`MessageBAM_BAMValue`** | 2 | **`MessageBAMValue`** | — | 3.619.888 | nein |
| `MessageBAM_MessageFK` | 1 | `MessageID` | — | 2.714.916 | nein |
| **`MessageBAM_BAMValueOnly`** | 1 | **`MessageBAMValue`** | — | 5.429.833 | nein |

```sql
CREATE TABLE `MessageBAM` (
  `MessageID` varchar(36) NOT NULL,
  `MessageBAMType` smallint(6) NOT NULL,
  `MessageBAMValue` varchar(70) NOT NULL,
  PRIMARY KEY (`MessageID`,`MessageBAMType`,`MessageBAMValue`),
  KEY `MessageBAM_MessageID` (`MessageID`,`MessageBAMType`),
  KEY `MessageBAM_BAMValue` (`MessageBAMType`,`MessageBAMValue`),
  KEY `MessageBAM_MessageFK` (`MessageID`),
  KEY `MessageBAM_BAMValueOnly` (`MessageBAMValue`),
  CONSTRAINT `MessageBAM_MessageFK` FOREIGN KEY (`MessageID`) REFERENCES `Message` (`MessageID`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```

**`SHOW CREATE TABLE` ist nicht verweigert worden.** Der Vorbehalt aus dem Auftrag — die Verweigerung
wäre selbst das Ergebnis, wie bei M31‑3 — kam nicht zum Tragen: Der Lesebenutzer sieht die Definition.
Das ist erwähnenswert, weil `SHOW EVENTS` demselben Benutzer am Vortag mit `ERROR 1044` verweigert
wurde; das Recht hängt am Objekt und nicht pauschal am Benutzer.

> **`CARDINALITY` ist wie `TABLE_ROWS` eine Schätzung.** Die Zeile `PRIMARY / MessageBAMValue` steht
> auf 10.859.666, also exakt auf dem geschätzten Tabellenumfang — gezählt hat die Tabelle
> **15.406.350** Zeilen (M33). Für die Frage dieser Messung („welche Gestalt hat der Index?") ist das
> folgenlos; als Mengengerüst sind die gezählten Werte zu nehmen. Dieselbe Warnung wie in M23‑1.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| `SUB_PART` ist `NULL` — **Vollindex über 70 Zeichen** | **ja**, bei allen fünf Indizes | Exakte Suche und Präfixsuche sind beide indexfähig, der Index liefert die Zeile ohne Nachschlagen. M34 bestätigt es: `Using index` in jedem Plan, der über den Wert einsteigt |
| `SUB_PART` < 70 — Präfixindex | **nein** | — Die befürchtete Verteuerung der **exakten** Suche entfällt. `MessageBAM` verhält sich hier anders als `MessageProperty` (M14: Präfix über 50 Zeichen) |
| Es gibt einen Index `(MessageBAMValue, MessageBAMType)` **oder umgekehrt** | **ja — umgekehrt** | `MessageBAM_BAMValue` ist `(MessageBAMType, MessageBAMValue)`. Die Einschränkung auf einen Typ ist damit indexfähig — M36 misst, ob sie auch etwas bringt |
| Es gibt keinen solchen Index | nein | — |

**Wo die vorformulierte Zeile nicht passt — die Reihenfolge im zusammengesetzten Index ist die
ungünstige.** Der Auftrag behandelt `(Wert, Typ)` und `(Typ, Wert)` als denselben Fall. Sie sind es
nicht:

- **`(MessageBAMType, MessageBAMValue)`** trägt den Typ als führende Spalte. Für eine Suche **ohne**
  Typangabe — den Normalfall der Oberfläche — ist er damit **nicht** benutzbar; sie braucht
  `MessageBAM_BAMValueOnly`.
- Wäre die Reihenfolge umgekehrt, bediente **ein** Index beide Fälle, und `MessageBAM_BAMValueOnly`
  wäre überflüssig.
- Praktisch ist das folgenlos, weil beide Indizes existieren. Es erklärt aber, warum der Optimierer
  in M36 je nach Wert einen **anderen** Index wählt.

**Und ein zweiter Befund ohne vorformulierte Zeile: drei der fünf Indizes stehen auf `MessageID`.**
`PRIMARY` (Präfix), `MessageBAM_MessageID` und `MessageBAM_MessageFK` beantworten dieselbe Frage.
Der Index-Anteil der Tabelle ist mit **5,25 GB gegen 1,83 GB Daten** fast das Dreifache — und ein
merklicher Teil davon ist Doppelung. **Das ist eine Feststellung und keine Forderung:**
`GlassfishDB` gehört uns nicht (Regel S1), es wird kein Index angelegt und keiner entfernt. Es
gehört hierhin, weil die 7,1 GB aus `datenmodell.md` §8 sonst wie Nutzlast aussehen.

**Laufzeit:** unter 10 ms (Katalogabfragen, nicht weiter gemessen).

---

# M33 — Wie viele Treffer hat ein Wert?

**Frage.** Die tragende Zahl der ganzen Runde: die Verteilung der Trefferzahl je `MessageBAMValue`
über den Gesamtbestand.

**Ohne Zeitfenster, und das ist begründungspflichtig (Regel L9).** Ein Fenster kann die Frage
grundsätzlich nicht beantworten: Gefragt ist, wie viele Zeilen der Index hinter einem Wert liefert —
und der Index kennt keine Zeit (`MessageBAM` hat keinen Zeitstempel, `datenmodell.md` §3). Eine über
ein Fenster gerechnete Verteilung beschriebe die Kosten **nach** dem Join und damit genau nicht die
Größe, die den Einstieg teuer macht. Die Kosten stehen unten und sind erheblich.

## M33‑0 Vorabbefund: die Tabelle ist um 42 Prozent größer als dokumentiert

```sql
SELECT COUNT(*) AS zeilen_gezaehlt FROM MessageBAM;
```

| | Ergebnis |
|---|---:|
| **gezählt** | **15.406.350** |
| dokumentiert in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, [`datenmodell.md`](datenmodell.md) §3 und §8, M23‑1 | 10.859.666 |
| Abweichung | **+ 4.546.684 (+ 41,9 %)** |

Zweimal gezählt, beide Male identisch. Die dokumentierte Zahl ist die `TABLE_ROWS`-Schätzung aus
`information_schema` — dieselbe, die auch heute noch dort steht (`TABLE_ROWS` = 10.859.666,
unverändert seit dem 27.07.2026).

**Die Zahl steht in der verbindlichen Datei als „gemessen".** `PROJEKTBESCHREIBUNG.md` §8 führt sie
unter „Gemessenes Mengengerüst, 27.07.2026", `datenmodell.md` §3 unter „Gemessen am 27.07.2026". Für
`Message` und `MessageAction` ist die Schätzung inzwischen gegen gezählte Werte gehalten worden
(M0, M14, M30‑6); für `MessageBAM` ist das bis heute nicht geschehen — sie war die Tabelle, die nie
erhoben wurde.

> **Nichts daran wird hier geändert.** Dieselbe Haltung wie bei M31‑3 zum Takt von
> `MatchInterchange`: Die Runde stellt fest, die Korrektur der verbindlichen Datei ist eine
> Entscheidung und gehört ins Sparring. Sie steht als **offene Frage 1**.

**Laufzeit:** **18,795 s** kalt, **3,623 s** warm — zwei Läufe, ausdrücklich nicht „beste von fünf".
Der Unterschied ist der Punkt, siehe unten.

## M33‑1 Die Verteilung in Klassen

```sql
SELECT CASE WHEN t.n = 1      THEN '1'
            WHEN t.n <= 5     THEN '2-5'
            WHEN t.n <= 20    THEN '6-20'
            WHEN t.n <= 100   THEN '21-100'
            WHEN t.n <= 1000  THEN '101-1.000'
            WHEN t.n <= 10000 THEN '1.001-10.000'
            ELSE                   'ueber 10.000' END AS klasse,
       COUNT(*) AS werte, SUM(t.n) AS zeilen, MIN(t.n) AS kleinste, MAX(t.n) AS groesste
FROM (SELECT MessageBAMValue AS v, COUNT(*) AS n FROM MessageBAM GROUP BY MessageBAMValue) t
GROUP BY klasse ORDER BY kleinste;
```

**`EXPLAIN`** — die abgeleitete Tabelle ist ein vollständiger, **index-naher** Durchlauf; die
Tabelle selbst wird nie angefasst:

| id | select_type | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|---:|---|
| 1 | PRIMARY | `<derived2>` | `ALL` | — | — | 10.859.666 | |
| 2 | DERIVED | `MessageBAM` | **`index`** | **`MessageBAM_BAMValueOnly`** | 282 | 10.859.666 | **`Using index`** |

### Ergebnis

**3.024.820 verschiedene Werte auf 15.406.350 Zeilen** — im Schnitt 5,09 Treffer je Wert.

| Klasse (Treffer je Wert) | Werte | Anteil der Werte | Zeilen | Anteil der Zeilen | kleinste | größte |
|---|---:|---:|---:|---:|---:|---:|
| **1** | **2.321.251** | **76,74 %** | 2.321.251 | 15,07 % | 1 | 1 |
| 2–5 | 560.461 | 18,53 % | 1.361.252 | 8,84 % | 2 | 5 |
| 6–20 | 76.284 | 2,52 % | 659.122 | 4,28 % | 6 | 20 |
| 21–100 | 43.631 | 1,44 % | 2.344.543 | 15,22 % | 21 | 100 |
| 101–1.000 | 22.438 | 0,74 % | 5.923.234 | **38,45 %** | 101 | 998 |
| 1.001–10.000 | 726 | 0,02 % | 1.483.603 | 9,63 % | 1.002 | 9.840 |
| **über 10.000** | **29** | **0,001 %** | 1.313.345 | 8,53 % | 10.227 | **234.159** |

Die Summen gehen auf: 3.024.820 Werte und 15.406.350 Zeilen.

### Perzentile

```sql
WITH h AS (SELECT t.n AS n, COUNT(*) AS werte
           FROM (SELECT MessageBAMValue AS v, COUNT(*) AS n FROM MessageBAM GROUP BY MessageBAMValue) t
           GROUP BY t.n),
     c AS (SELECT n, SUM(werte) OVER (ORDER BY n) AS kum, SUM(werte) OVER () AS gesamt FROM h)
SELECT MIN(CASE WHEN kum >= 0.50  * gesamt THEN n END) AS p50,
       MIN(CASE WHEN kum >= 0.90  * gesamt THEN n END) AS p90,
       MIN(CASE WHEN kum >= 0.99  * gesamt THEN n END) AS p99,
       MIN(CASE WHEN kum >= 0.999 * gesamt THEN n END) AS p999,
       MAX(n) AS maximum, MAX(gesamt) AS verschiedene_werte
FROM c;
```

| 50. | 90. | 99. | 99,9. | Maximum | verschiedene Werte |
|---:|---:|---:|---:|---:|---:|
| **1** | **2** | **75** | **544** | **234.159** | 3.024.820 |

> Die Klassentabelle und die Perzentile sind **zweimal unabhängig** entstanden: einmal serverseitig
> wie abgedruckt, einmal offline aus dem vollständigen Histogramm der Trefferzahl (1.506 Zeilen,
> eigenes Statement, 10,633 s). Beide Wege liefern dieselben Zahlen bis auf die letzte Stelle.

## M33‑2 Die Trefferzahl je Typ

```sql
SELECT t.typ, bt.MessageBAMTypeDescription AS beschreibung,
       COUNT(*) AS verschiedene_werte, SUM(t.n) AS zeilen,
       ROUND(AVG(t.n), 2) AS schnitt_je_wert, MAX(t.n) AS groesste
FROM (SELECT MessageBAMType AS typ, MessageBAMValue AS v, COUNT(*) AS n
      FROM MessageBAM GROUP BY MessageBAMType, MessageBAMValue) t
LEFT JOIN MessageBAMType bt ON bt.MessageBAMType = t.typ
GROUP BY t.typ, bt.MessageBAMTypeDescription
ORDER BY schnitt_je_wert DESC;
```

Der innere Teil läuft hier über `MessageBAM_BAMValue` (`index`, `key_len` 284, `Using index`) statt
über `MessageBAM_BAMValueOnly` — die Gruppierung nach Typ und Wert bedient genau dieser Index.

### Ergebnis — die zwanzig Typen mit der höchsten mittleren Trefferzahl je Wert

| Typ | Beschreibung | Werte | Zeilen | **Ø Treffer je Wert** | größte |
|---:|---|---:|---:|---:|---:|
| **9000** | Abladestelle_L_SAP | 24 | 151.063 | **6.294,29** | 60.767 |
| 9005 | Werk_L_SAP | 35 | 162.893 | 4.654,09 | 52.117 |
| **9014** | Lieferantennummer beim Kunden_K_SAP | 100 | 432.227 | 4.322,27 | **234.159** |
| 9033 | Empfaengercode_K_SAP | 37 | 157.848 | 4.266,16 | 124.748 |
| 9032 | Sendercode_K_SAP | 40 | 157.848 | 3.946,20 | 100.343 |
| 9036 | Lagerort Kunde_L_SAP | 8 | 22.847 | 2.855,88 | 20.620 |
| 9015 | Kundenwerk_K_SAP | 285 | 435.690 | 1.528,74 | 62.011 |
| 9016 | Abladestelle_K_SAP | 528 | 412.886 | 781,98 | 60.845 |
| **9001** | Abrufnummer_L_SAP *(kuratiert, Position 2)* | 1.385 | 465.143 | 335,84 | 1.663 |
| 9039 | Daten-Sender-Nummer_L_SAP | 138 | 33.347 | 241,64 | 3.524 |
| **9018** | Kundenmaterialnummer_K_SAP | 15.603 | 2.311.236 | 148,13 | 7.004 |
| 9037 | Packmittelnummer Kunde_L_SAP | 348 | 41.562 | 119,43 | 11.882 |
| 9004 | Unsere Material-Nr._L_SAP | 12.846 | 1.081.129 | 84,16 | 9.779 |
| 9019 | Bestellnummer vom Kunden_K_SAP | 22.762 | 1.852.237 | 81,37 | 2.956 |
| 9030 / 9031 | Sender_/Empf_Ident_FORS | je 2 | je 147 | 73,50 | 82 |
| 9028 / 9029 | Material-Nr. Kunde/Lieferant_FORS | je 4.159 | je 215.116 | 51,72 | 65 |
| 9027 | Bestellnummer_FORS | 4.161 | 215.117 | 51,70 | 65 |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 20.906 | 1.071.249 | 51,24 | 9.779 |
| 9038 | Packmittelnummer Lieferant_L_SAP | 1.186 | 59.802 | 50,42 | 2.229 |
| 9025 | Abladestelle_FORS | 6 | 295 | 49,17 | 82 |

Und das andere Ende, weil es die Suche trägt:

| Typ | Beschreibung | Werte | Zeilen | Ø je Wert | größte |
|---:|---|---:|---:|---:|---:|
| **3** | Rechnungsnummer | **1.969.264** | 2.268.697 | **1,15** | 4 |
| **9006** | Lieferschein-Nr._L_SAP *(kuratiert, Position 1)* | 143.060 | 155.809 | **1,09** | 4 |
| 9022 | Gutschriftsanzeigen-Nummer_K_SAP | 131.561 | 135.240 | 1,03 | 4 |
| 9023 | Übertragungsnummer Gutschrift_K_SAP | 115.890 | 127.959 | 1,10 | 4 |
| 9021 | Transportnummer_K_SAP | 55.144 | 55.250 | 1,00 | 6 |
| 9017 | (JIT-) Abrufnummer_K_SAP | 49.336 | 666.806 | 13,52 | 1.059 |
| 2 | Lieferscheinnummer | 38.877 | 62.079 | 1,60 | 3 |
| 9007 | Transport-Nummer_L_SAP | 35.351 | 40.603 | 1,15 | 34 |

Die Spalte „Zeilen" summiert sich über alle 55 vorkommenden Typen auf **15.406.350** — dieselbe Zahl
wie M33‑0. Ein Wert kann unter mehreren Typen stehen (M37), deshalb ist die Summe der Spalte „Werte"
über die Typen größer als die 3.024.820 verschiedenen Werte insgesamt.

## Laufzeiten und die gerissene Obergrenze

| Statement | kalt (einmalig) | warm (beste von 3) | Faktor |
|---|---:|---:|---:|
| `COUNT(*)` über `MessageBAM` | 18,795 s | **3,623 s** | 5,2 |
| Grundaggregation (Werte, Zeilen, Maximum) | **62,780 s** ⚠️ | **10,421 s** | **6,0** |
| je Typ, mit Join auf `MessageBAMType` | **198,596 s** ⚠️ | **17,631 s** | **11,3** |
| Histogramm der Trefferzahl | — | 10,633 s | — |
| Klassenverteilung | — | 13,403 s | — |
| Perzentile | — | 10,694 s | — |

Die drei Kaltläufe sind vor jeder Wiederholung gelaufen und deshalb **einmalig**; die warmen Zahlen
stammen aus einem eigenen Lauf **desselben** Statements, jeweils beste von drei nach einem
Aufwärmlauf.

> ⚠️ **Zwei Statements haben die 60-Sekunden-Grenze gerissen, und sie sind nicht abgebrochen
> worden.** Beide waren Kaltläufe, beide liefen durch, bevor eine Zwischenmeldung möglich war — der
> Client blockiert bis zum Ergebnis. Das ist eine **Abweichung vom Rahmen** und wird als solche
> benannt statt weggelassen. Ab dem dritten Statement lief jede Sitzung mit
> `SET max_statement_time = 60`; danach ist kein Statement mehr über die Grenze gekommen.
>
> **Der Grund ist der Pufferpool und nicht die Abfrage.** Dasselbe Statement kostet warm **17,631 s**
> statt 198,596 s — **Faktor 11,3**; bei der Grundaggregation sind es 10,421 s gegen 62,780 s,
> **Faktor 6,0**. Der Kaltlauf las `MessageBAM_BAMValue` beziehungsweise
> `MessageBAM_BAMValueOnly` zum ersten Mal von der Platte; beide sind Teil der 5,25 GB aus M32.
> Für die Bewertung der Suche ist die **warme** Zahl die richtige (der Endpunkt läuft auf einer
> laufenden Instanz), für die Bewertung des ersten Aufrufs nach einem Neustart die kalte.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **99. Perzentil unter rund 100 Treffern** | **ja — 75** | **Der direkte Weg trägt.** Die Rückfalloption aus Annahme A9 (eigener BAM-Index in `overlord_monitor`, vom Rollup-Job mitgeführt) wird **nicht gebraucht**; der Vorbehalt „der Rollup-Job entsteht erst in Schritt 10" wird nicht eingelöst. Der zweite Halbsatz — „Regel L1 ist an diesem Endpunkt Zeremonie statt Schutz" — ist durch **M35 widerlegt**, siehe dort |
| 99. Perzentil vier- oder fünfstellig | **nein** | — A9 wird nicht gezogen |
| **Das Maximum liegt bei sechsstelligen Trefferzahlen** | **ja — 234.159** | Bestätigt die Abladestelle `050` aus [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 als realen Fall und nicht als Anekdote. Der Ausreißer ist sogar ein anderer Typ als vermutet — siehe Absatz unten |
| **Die Verteilung ist zweigipflig** — viele Werte mit einem Treffer, wenige mit sehr vielen | **ja, ausgeprägt** | 76,74 % der Werte haben genau einen Treffer und machen 15,07 % der Zeilen aus; **29 Werte** (0,001 %) machen 8,53 % der Zeilen aus. **Die Trefferzahl und nicht die Zeichenlänge ist das richtige Schutzkriterium**, und die Mindestlänge aus Regel L5 ist der falsche Hebel — bestätigt durch M38 (die Längen streuen je Typ von 1 bis 35) und durch E6 |

**Wo die vorformulierte Zeile nicht passt — die Abladestelle ist nicht der schlimmste Fall.**
§3.2 nennt „die Abladestelle `050`" als das Beispiel für einen millionenfach vorkommenden Wert.
Gemessen ist die Abladestelle zwar der Typ mit dem **höchsten Schnitt** (9000, 6.294,29 Treffer je
Wert bei nur 24 verschiedenen Werten), aber der **absolut schlimmste Einzelwert** gehört zu Typ
**9014** (Lieferantennummer beim Kunden) mit 234.159 Treffern. Das ändert an der Warnung nichts —
es verschiebt nur, worauf sie zeigt: Nicht ein Feld ist gefährlich, sondern **jedes Feld, das eine
Kennung statt einer Belegnummer trägt**. Betroffen sind mindestens 9000, 9005, 9014, 9032, 9033,
9036, 9015 und 9016 — alles Werke, Codes, Kennungen und Stellen.

**Und ein zweiter Befund ohne vorformulierte Zeile: die Zeilenzahl.** Er steht oben in M33‑0 und ist
der folgenreichste Einzelbefund der Runde für die Dokumentation, weil er eine als „gemessen"
ausgewiesene Zahl in der verbindlichen Datei betrifft.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Verteilung der Trefferzahl je `MessageBAMValue` über **alle** 15.406.350 Zeilen von
> `MessageBAM`, ohne Mandanten- und ohne Zeitfilter (n = 3.024.820 verschiedene Werte).
>
> *Behauptet wird:* dass daraus die Kosten der **Suche eines Mandanten** folgen.
>
> **Die Lücke:** Der Index kennt keinen Mandanten. Die Trefferzahl aus M33 ist die Zahl der Zeilen,
> die der Index liefert, **bevor** der Mandantenfilter greift — sie ist damit die richtige
> Kostengröße und ausdrücklich **nicht** die Zahl der Zeilen, die der Nutzer sieht. Bei dem
> schlimmsten Wert fallen beide zufällig zusammen (alle 234.159 gehören `NEXANS`, M35); bei einem
> Wert, den zwei Mandanten teilen, täten sie es nicht. Für die **Antwortgröße** ist M33 deshalb
> keine Auskunft, für die **Laufzeit** ist es die einzige.

---

# M34 — Was kostet der Einstieg über den Wert?

**Frage.** Die Kernmessung. Drei Suchformen, je Prüfwert, je Mandant, mit `EXPLAIN` und Laufzeit.

## Statement

In der Form, die der spätere Endpunkt erzeugen würde:

```sql
SELECT m.MessageID, m.MessageStatus, m.MessageLastUpdate, m.ProcessID,
       m.Source, m.Target, m.SourceMessageID, m.TargetMessageID
FROM MessageBAM b
JOIN Message m ON m.MessageID = b.MessageID
JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID AND pm.MandantID = ?
WHERE b.MessageBAMValue = ?                              -- exakt
--   b.MessageBAMValue LIKE CONCAT(?, '%')               -- Praefix
--   b.MessageBAMValue LIKE CONCAT('%', ?, '%')          -- enthaelt
GROUP BY m.MessageID
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 51;
```

Jede Form zusätzlich **mit `STRAIGHT_JOIN`** über `MessageBAM → Message → Process → ProjectMandant`.
Der Anlass ist L15: identisches Statement, 13,2 s gegen 11,9 ms, allein wegen der
Kardinalitätsstatistik.

> **Der Mandantenfilter steht hier als `JOIN` und nicht als `EXISTS`** — anders als in M30‑1 und im
> `NachrichtenRepository`. Der Grund ist die Vorgabe des Auftrags, und er ist folgenlos: Der
> `EXPLAIN` weist `pm` in jeder Fassung als `eq_ref` mit `Using index` aus, und das `GROUP BY
> m.MessageID` fängt die Vervielfachung ab, die ein Join gegenüber einem `EXISTS` erzeugen könnte.
> In den Daten ist `ProjectMandant` ohnehin durchgängig 1:1 (M3).

## `EXPLAIN`

**Exakt — mit und ohne `STRAIGHT_JOIN` identisch.** Der Optimierer wählt von sich aus die Reihenfolge,
die `STRAIGHT_JOIN` erzwingen würde:

| id | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---:|---|
| 1 | `b` | **`ref`** | `PRIMARY`, `MessageBAM_MessageID`, `MessageBAM_MessageFK`, `MessageBAM_BAMValueOnly` | **`MessageBAM_BAMValueOnly`** | 282 | `const` | 443.830 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | `m` | `eq_ref` | `PRIMARY`, `ProejctIDIDX`, `Message_ProcessFK` | `PRIMARY` | 146 | `b.MessageID` | 1 | `Using where` |
| 1 | `p` | `eq_ref` | `PRIMARY`, `Process_ProjectFK` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | `pm` | `eq_ref` | `PRIMARY`, `ProjectMandant_Mandant_idx` | `PRIMARY` | 292 | `p.ProjectID`, `const` | 1 | `Using where; Using index` |

Beim typischen Wert steht in der Zeile `b` **`rows` 1** statt 443.830 — dieselbe Gestalt, andere
Schätzung.

**Präfix** — identische Gestalt, nur `type` `range` statt `ref` und `ref` `NULL` statt `const`.
`CONCAT(@wert, '%')` ist ein konstanter Ausdruck und verhindert die Bereichsoptimierung **nicht**.

**Enthält (`LIKE '%…%'`) — der Plan kippt vollständig.** `MessageBAM` steht nicht mehr vorn, die
Suche steigt über `Message` ein:

| id | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---:|---|
| 1 | `m` | **`range`** | `PRIMARY`, `ProejctIDIDX`, `Message_ProcessFK` | **`ProejctIDIDX`** | 147 | NULL | 1.780.243 | `Using index condition; Using temporary; Using filesort` |
| 1 | `p` | `eq_ref` | `PRIMARY`, `Process_ProjectFK` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | `pm` | `eq_ref` | `PRIMARY`, `ProjectMandant_Mandant_idx` | `PRIMARY` | 292 | `p.ProjectID`, `const` | 1 | `Using where; Using index` |
| 1 | `b` | `ref` | `PRIMARY`, `MessageBAM_MessageID`, `MessageBAM_MessageFK` | `PRIMARY` | 146 | `m.MessageID` | 8 | `Using where; Using index` |

## Ergebnis — Laufzeiten

Alle Zeiten in Millisekunden, serverseitig über `SHOW PROFILES`.

### `NEXANS`

| Prüfwert | Treffer | exakt | exakt + `STRAIGHT_JOIN` | Präfix | Präfix + `STRAIGHT_JOIN` |
|---|---:|---:|---:|---:|---:|
| typisch | 1 | **0,804** | 0,704 | 1,022 | 0,954 |
| unangenehm | 75 | **1,426** | 1,423 | 1,534 | 1,485 |
| schlimm | 234.159 | **10.617,6** | 10.667,3 | 11.020,5 | 11.048,1 |

*beste von 5 nach einem Aufwärmlauf; beim schlimmsten Wert beste von 3.*

### `IBIS` und `ZAST` (Regel L7)

| Mandant | Prüfwert | Treffer | exakt | exakt + `SJ` | Präfix | Präfix + `SJ` |
|---|---|---:|---:|---:|---:|---:|
| `IBIS` | typisch | 1 | 0,712 | 0,707 | 0,744 | 0,730 |
| `ZAST` | typisch | 1 | 0,708 | 0,708 | 0,709 | 0,681 |

### Die dritte Form — `enthält`

Gemessen, obwohl der Plan sie schon verurteilt: „kann den Index nicht nutzen" ist eine Behauptung,
solange keine Zahl danebensteht (Regel Q4).

| Prüfwert | Treffer | enthält | enthält + `STRAIGHT_JOIN` |
|---|---:|---:|---:|
| typisch | 1 | **40.246,9 ms** | **9.273,3 ms** |
| schlimm | 234.159 | **48.504,6 ms** | **20.108,3 ms** |

*beste von 3 nach einem Aufwärmlauf, mit `SET max_statement_time = 60`. Keiner der vier Fälle ist
abgebrochen worden; der teuerste blieb mit 48,5 s unter der Grenze.*

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **Exakt bleibt beim typischen Wert unter 100 ms** | **ja, weit** (0,804 ms) | Der Endpunkt ist ohne Zusatzbau möglich |
| **… und beim schlimmsten unter der Zeitgrenze** | **nein** | 10,618 s reißen die 10-Sekunden-Grenze des Lese-Pools. **Das ist der Fall, der einen Schutz braucht** — und M35 zeigt, dass das Zeitfenster ihn leistet |
| **Präfix kostet nicht wesentlich mehr als exakt** | **ja** | Bei gleicher Trefferzahl kostet Präfix 3 bis 27 % mehr (typisch 1,022 gegen 0,804 ms; schlimm 11.020 gegen 10.618 ms). Fachlich wäre Präfix damit das bessere Angebot, und die Trefferspalte trüge **Typ und Wert** — **aber nur bei gleicher Trefferzahl**, siehe E6 |
| Präfix kostet ein Vielfaches | nein | — |
| **Die Optimiererreihenfolge kippt zwischen den Mandanten** | **nein** | Über `NEXANS`, `IBIS` und `ZAST` ist der Plan identisch, und `STRAIGHT_JOIN` ändert bei exakt und Präfix **nichts** (Abweichung unter 12 %, in beide Richtungen). Die L15-Falle greift hier nicht |
| **`enthält` liegt im Sekundenbereich oder darüber** | **ja** | **Ausgeschlossen, und zwar mit Zahl statt mit Vermutung.** 40,2 s für einen Wert mit **einem** Treffer — Faktor 50.000 gegenüber der exakten Form |

**Wo die vorformulierte Zeile nicht passt — `STRAIGHT_JOIN` ändert sehr wohl etwas, nur woanders.**
Die Zeile erwartet ein Kippen **zwischen Mandanten**. Gemessen kippt der Plan **zwischen Suchformen**,
und dort ist der Unterschied groß:

| | ohne `STRAIGHT_JOIN` | mit `STRAIGHT_JOIN` | Faktor |
|---|---:|---:|---:|
| `enthält`, typischer Wert | 40.246,9 ms | **9.273,3 ms** | **4,34** |
| `enthält`, schlimmster Wert | 48.504,6 ms | **20.108,3 ms** | **2,41** |

Der Optimierer wählt bei `%…%` den Einstieg über `Message` (voller Durchlauf über `ProejctIDIDX`,
1,78 Mio. Zeilen) und liegt damit **falsch**: Der erzwungene Einstieg über `MessageBAM` — ein
vollständiger, index-naher Durchlauf des Wertindex — ist vier Mal billiger. **Für die Entscheidung
ist das folgenlos**, weil beide Fassungen weit jenseits des Tragbaren liegen; es gehört trotzdem
hierhin, weil es zeigt, dass die L15-Falle bei dieser Suche existiert und nur an einer anderen
Stelle sitzt als vermutet.

---

# E6 — Was kostet ein **verkürztes** Präfix? *(ergänzt, nicht im Plan)*

**Warum das nachgezogen wurde.** M34 misst die Präfixsuche mit dem **vollständigen** Wert als
Präfix — das ist der faire Vergleich zur exakten Suche, aber es ist nicht, was ein Nutzer tut. Wer
Präfixsuche anbietet, bekommt abgeschnittene Eingaben. Ohne diese Zahl hieße der Satz „Präfix kostet
kaum mehr als exakt" etwas anderes, als er zu sagen scheint.

```sql
SELECT 6 AS praefixlaenge,
       (SELECT COUNT(*) FROM MessageBAM WHERE MessageBAMValue LIKE CONCAT(LEFT(@w_typisch, 6), '%')) AS treffer
UNION ALL SELECT 4, (SELECT COUNT(*) FROM MessageBAM WHERE MessageBAMValue LIKE CONCAT(LEFT(@w_typisch, 4), '%'))
UNION ALL SELECT 3, (SELECT COUNT(*) FROM MessageBAM WHERE MessageBAMValue LIKE CONCAT(LEFT(@w_typisch, 3), '%'));
```

Der typische Prüfwert ist acht Zeichen lang und hat **einen** Treffer.

| Präfixlänge | Treffer | Suche ohne Fenster | Suche mit Fenster B |
|---:|---:|---:|---:|
| 8 (vollständig) | **1** | 1,022 ms | — |
| 6 | **5.425** | **59 ms** | 48 ms |
| 4 | **155.871** | **3.441 ms** | 1.299 ms |
| 3 | **264.469** | (nicht gemessen) | — |

*Laufzeiten beste von 5 nach einem Aufwärmlauf.*

**Zwei Zeichen weniger kosten den Faktor 5.425, vier Zeichen weniger den Faktor 155.871.** Die
Laufzeit folgt der Trefferzahl fast linear — 155.871 Treffer kosten 3,44 s, die 234.159 des
schlimmsten Werts kosten 10,6 s.

**Damit ist die Kostengröße benannt, und sie ist nicht die Suchform.** Exakt, Präfix und selbst
`enthält` unterscheiden sich bei **gleicher Trefferzahl** um Prozente; was die Laufzeit macht, ist
allein die Zahl der Zeilen hinter dem Schlüssel. Eine Entscheidung zwischen exakt und Präfix ist
deshalb keine Leistungsfrage, sondern die Frage, **wie viele Treffer man zulässt** — und genau dafür
gibt es die Mindestlänge aus Regel L5.

> **Die Mindestlänge muss dann aber je Typ gelten und nicht global.** M38 misst Werte mit Länge 1
> (Typen 9015, 9016, 9017, 9020, 9000, 9005, 9034, 9037, 9038) neben Werten mit Länge 35 (9003).
> Eine feste Mindestlänge von vier Zeichen machte neun Typen unsuchbar; eine von drei Zeichen ließe
> beim typischen Prüfwert 264.469 Treffer zu.

---

# M35 — Schützt das Pflicht-Zeitfenster hier überhaupt etwas?

**Frage.** Die Entscheidung, die am weitesten reicht: Regel L1 verlangt an jedem Listen-Endpunkt ein
Zeitfenster. Der typische Nutzer der BAM-Suche hat eine Belegnummer und **kein Datum**.

## Statement

Die aus M34 siegreiche Form — **exakt, ohne `STRAIGHT_JOIN`** (der Zusatz ändert dort nichts, also
bleibt er weg) —, viermal mit unterschiedlichem Zeitfenster:

```sql
SELECT m.MessageID, m.MessageStatus, m.MessageLastUpdate, m.ProcessID,
       m.Source, m.Target, m.SourceMessageID, m.TargetMessageID
FROM MessageBAM b
JOIN Message m ON m.MessageID = b.MessageID
JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID AND pm.MandantID = 'NEXANS'
WHERE b.MessageBAMValue = ?
  AND m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?     -- bzw. ohne diese Zeile
GROUP BY m.MessageID
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 51;
```

## `EXPLAIN` — der Plan kippt genau einmal

| Fenster | führende Tabelle | `key` | `rows` |
|---|---|---|---:|
| ohne | `b` | `MessageBAM_BAMValueOnly` | 443.830 |
| **A (Tag)** | **`m`** | **`MessageLastUpdateIDX`** | **11.812** |
| B (Monat) | `b` | `MessageBAM_BAMValueOnly` | 443.830 |
| ein Jahr | `b` | `MessageBAM_BAMValueOnly` | 443.830 |

Beim **typischen** Wert bleibt der Einstieg auch mit Fenster A bei `b` (`rows` 1) — der Optimierer
kippt nur, wenn der Wertindex teurer aussieht als das Fenster.

## Ergebnis

| Fenster | **typischer** Wert | **schlimmster** Wert | Treffer des schlimmsten (`NEXANS`) |
|---|---:|---:|---:|
| **ohne** | 0,729 ms | **10.752,8 ms** | 234.159 |
| **A (Tag)** | 0,757 ms | **90,5 ms** | 279 |
| **B (Monat)** | 0,796 ms | **1.652,0 ms** | 18.748 |
| **ein Jahr** | 0,787 ms | **8.664,4 ms** | 170.997 |

*typisch: beste von 5; schlimm: beste von 3, je nach einem Aufwärmlauf.*

**Alle 234.159 Treffer des schlimmsten Werts gehören `NEXANS`**, und zwar auf 234.159
**verschiedenen** Nachrichten — der Wert steht genau einmal je Nachricht.

**Warum das Fenster wirkt, obwohl der Plan bei B und Jahr derselbe bleibt.** Nicht der Indexzugriff
wird billiger, sondern das, was danach kommt: Das `GROUP BY` und das `ORDER BY … DESC` legen eine
temporäre Tabelle an und sortieren sie, und deren Größe ist die Zahl der **überlebenden** Zeilen.
Die Laufzeit folgt ihr fast linear — 234.159 Zeilen kosten 10,75 s, 170.997 Zeilen 8,66 s, 18.748
Zeilen 1,65 s. Bei Fenster A ist das Fenster selektiver als der Wert, und der Plan kippt: 279 Zeilen
kosten 90,5 ms.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **Beim typischen Wert ändert das Fenster nichts** | **ja** | Erwartet. 0,729 gegen 0,787 ms über alle vier Fassungen — der Unterschied liegt im Rauschen. Es filtert erst nach dem Join, und der ist bei einem Treffer billig |
| Beim schlimmsten Wert ändert das Fenster ebenfalls nichts → **L1 wirkungslos** | **nein** | — Die benannte Ausnahme von L1 wird **nicht** gebraucht |
| **Beim schlimmsten Wert senkt das Fenster die Laufzeit deutlich** | **ja, sehr** | Faktor **119** (Tag), Faktor **6,5** (Monat). **L1 bleibt** — aber siehe den Absatz unten zur Weite |
| **Ohne Fenster reißt der schlimmste Wert die Zeitgrenze des Lese-Pools (10 s)** | **ja** | 10,753 s ohne Fenster, 10,618 s in M34. Ein Fenster ist Pflicht, **und** die Suche braucht zusätzlich den Abbruchpfad, den die Nachrichtenliste als `suche-abgebrochen` schon kennt — denn auch mit einem Jahresfenster bleiben 8,664 s, und das ist keine Reserve |

**Wo die vorformulierte Zeile nicht passt — „weite Vorgabe" wäre genau die falsche Folgerung.**
Die Zeile sagt: senkt das Fenster die Laufzeit deutlich, bleibt L1 „mit einer **weiten** Vorgabe,
nicht mit 24 Stunden". Gemessen ist das Gegenteil:

| Fenster | Laufzeit (schlimmster Wert) | Abstand zur 10-s-Grenze |
|---|---:|---|
| 24 Stunden | 90,5 ms | Faktor 110 Reserve |
| 30 Tage | 1.652,0 ms | Faktor 6 Reserve |
| ein Jahr | 8.664,4 ms | **13 % Reserve** |
| ohne | 10.752,8 ms | **gerissen** |

**Die 24 Stunden aus L1 sind hier nicht zu eng, sondern die einzige Fassung mit echtem Abstand.**
Ein Jahr — das nach L1 zulässige Maximum — liegt 13 Prozent unter der Zeitgrenze und damit
innerhalb dessen, was ein belasteter Server verschiebt. Wer die Vorgabe weit setzt, verlagert den
Schutz vollständig auf den Abbruchpfad.

**Und der zweite Befund ohne vorformulierte Zeile: das Fenster verändert die Antwort, nicht nur den
Preis.** Bei 24 Stunden findet der schlimmste Wert **279 von 234.159** Nachrichten. Der Nutzer, der
eine Kennung sucht und kein Datum hat, bekommt 0,1 Prozent des Bestands — ohne zu wissen, dass er
sie bekommt. Das ist die Kehrseite, die die Laufzeittabelle nicht zeigt.

> **Diese Messung entscheidet nicht.** Sie sagt, was das Fenster kostet und spart. Ob es fachlich
> zumutbar ist, dass eine Belegsuche ohne Datumsangabe standardmäßig einen Tag absucht, entscheidet
> der Auftraggeber — **offene Frage 2**.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Laufzeit **eines** Statements über **zwei** Prüfwerte (1 und 234.159 Treffer), vier
> Fenster, Mandant `NEXANS`, auf einer ruhigen Testkopie.
>
> *Behauptet wird:* dass die 24 Stunden aus L1 hier „Reserve" haben und ein Jahr nicht.
>
> **Die Lücke:** Die Reserve ist gegen die 10-Sekunden-Grenze des Lese-Pools gerechnet, und die
> gilt in **Produktion**, nicht auf der Testkopie. Die Produktionsdatenbank trägt gleichzeitig den
> Verkehr des Altsystems; die hier gemessenen Zahlen sind Untergrenzen und keine Zusagen. Was
> gemessen ist: *Auf der ruhenden Testkopie liegt der Jahresfall bei 87 Prozent der Grenze.* Was
> **nicht** gemessen ist: wie sich derselbe Fall unter Last verhält — dafür bräuchte es eine Messung
> gegen die Produktion, die dieses Projekt bewusst nicht macht.

---

# M36 — Trägt die Einschränkung auf einen BAM-Typ?

**Frage.** Der Plan sieht eine optionale Einschränkung auf einen Typ vor. Beschleunigt sie, oder ist
sie ein Filter nach dem Indexzugriff?

## Statement

M34 in der siegreichen Form, einmal mit und einmal ohne `AND b.MessageBAMType = ?`, ohne Zeitfenster:

```sql
… WHERE b.MessageBAMValue = ?
      AND b.MessageBAMType = ?          -- bzw. ohne diese Zeile
  GROUP BY m.MessageID …
```

## `EXPLAIN` — der Optimierer wählt je nach Wert einen **anderen** Index

| Prüfwert | `key` | `key_len` | `ref` | `rows` |
|---|---|---:|---|---:|
| typisch, mit Typ 9017 | **`MessageBAM_BAMValue`** | 284 | `const, const` | **1** |
| schlimm, mit Typ 9014 | `MessageBAM_BAMValueOnly` | 282 | `const` | 443.830 |

Beim **typischen** Wert greift der zusammengesetzte Index aus M32 und liefert `rows` 1. Beim
**schlimmsten** bleibt der Optimierer beim reinen Wertindex — die Typangabe schneidet dort nichts ab,
weil der Wert ohnehin nur unter einem Typ vorkommt.

## Ergebnis

| Prüfwert | ohne Typ | mit Typ | Unterschied |
|---|---:|---:|---|
| typisch (1 Treffer, Typ 9017) | 0,738 ms | 0,769 ms | + 4 % |
| schlimm (234.159 Treffer, Typ 9014) | 10.459,3 ms | 10.617,0 ms | + 1,5 % |

*typisch: beste von 5; schlimm: beste von 3.*

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Deutlich schneller | **nein** | — Die Typwahl ist **kein** Leistungsmerkmal und darf nicht als solches beworben werden |
| **Gleich schnell** | **ja** | Sie ist eine reine **Ergebnisverfeinerung**. Sie bleibt sinnvoll — sie beantwortet „ich suche eine Lieferscheinnummer, keine Materialnummer" —, darf aber **nicht als Entlastung beworben oder in einer Grenze vorausgesetzt** werden. Insbesondere darf keine Mindestlänge und kein Limit mit dem Argument gelockert werden, es sei ja ein Typ gewählt |
| Langsamer | nein (+1,5 bis +4 %, im Rauschen) | — Die Verfeinerung darf in der `WHERE`-Klausel stehen und muss nicht hinter das Limit |

**Wo die vorformulierte Zeile nicht passt — „gleich schnell" trifft zu, aber nicht aus dem
angenommenen Grund.** Die Zeile unterstellt, die Typbedingung sei „ein Filter nach dem
Indexzugriff", also ein Nachlauf ohne Indexunterstützung. Gemessen ist beides falsch und richtig
zugleich:

- Beim **typischen** Wert ist sie **kein** Nachlauf: Der zusammengesetzte Index
  `(MessageBAMType, MessageBAMValue)` wird benutzt, `ref const,const`. Sie bringt trotzdem nichts,
  weil der Wert allein schon auf eine Zeile führt.
- Beim **schlimmsten** Wert ist sie ein Nachlauf, und auch das kostet nichts, weil sie keine Zeile
  entfernt.

**Der Fall, in dem sie tatsächlich etwas brächte, kommt in den Prüfwerten nicht vor**: ein Wert, der
unter **mehreren** Typen mit sehr unterschiedlicher Häufigkeit steht. M37 misst, wie oft es solche
Werte überhaupt gibt — bei 4,17 % der Paare. Ob darunter einer mit stark ungleicher Verteilung ist,
ist **nicht** gemessen.

---

# M37 — Steht derselbe Wert auf einer Nachricht unter mehreren Typen?

**Frage.** In M28‑2 stehen `9032`/`9033` mit **je 13.738** und `9022`/`9023` mit **je 12.419**.
Identische Anzahlen legen nahe, dass dieselben Werte unter zwei Typen liegen. Gemessen war das
nicht — es sind Anzahlen, keine Wertevergleiche.

## Statement

```sql
SELECT paar.typen, COUNT(*) AS paare
FROM (SELECT b.MessageID AS mid, b.MessageBAMValue AS v, COUNT(DISTINCT b.MessageBAMType) AS typen
      FROM Message m JOIN MessageBAM b ON b.MessageID = m.MessageID
      WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
      GROUP BY b.MessageID, b.MessageBAMValue) paar
GROUP BY paar.typen ORDER BY paar.typen;
```

**`EXPLAIN`** — `MessageBAM` wird wie in M11 und M28‑2 über das Präfix des Primärschlüssels erreicht
(Regeln L4/L5), `Using index`:

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---:|---|
| 1 | PRIMARY | `<derived2>` | `ALL` | — | — | — | 3.278.048 | `Using temporary; Using filesort` |
| 2 | DERIVED | `m` | `range` | `MessageLastUpdateIDX` | 5 | NULL | 409.756 | `Using where; Using index; Using temporary; Using filesort` |
| 2 | DERIVED | `b` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 8 | `Using index` |

## Ergebnis — Fenster B

| Typen je Paar `(MessageID, MessageBAMValue)` | Paare | Anteil |
|---:|---:|---:|
| 1 | 858.311 | 95,83 % |
| **2** | **33.938** | **3,79 %** |
| **3** | **3.402** | **0,38 %** |
| **4** | **34** | **0,004 %** |
| **zusammen ≥ 2** | **37.374** | **4,17 %** |

Gesamtzahl der Paare: **895.685**. Kein Paar trägt mehr als vier Typen.

### Die häufigsten Typenkombinationen

| Kombination | Paare | Kombination | Paare |
|---|---:|---|---:|
| **9028, 9029** | **15.790** | 9037, 9038 | 362 |
| 9004, 9018 | 4.247 | 0, 1 | 343 |
| 9001, 9017 | 3.362 | 9003, 9038 | 322 |
| 9002, 9019 | 3.362 | 9008, 9010 | 207 |
| 9000, 9015, 9016 | 3.332 | 9020, 9021 | 173 |
| 1, 2 | 3.233 | 9003, 9004 | 132 |
| 9000, 9005 | 560 | 9019, 9034 | 73 |
| 9014, 9033 | 541 | 9018, 9037, 9038 | 70 |
| 9006, 9007 | 507 | 9015, 9032 | 62 |
| 9015, 9016 | 433 | 9014, 9039 | 43 |

### Die Gegenprobe zur Vermutung des Auftrags

```sql
SELECT '9032/9033' AS paar, COUNT(*) AS gemeinsame_werte FROM Message m
JOIN MessageBAM a ON a.MessageID = m.MessageID AND a.MessageBAMType = 9032
JOIN MessageBAM b ON b.MessageID = m.MessageID AND b.MessageBAMType = 9033
                 AND b.MessageBAMValue = a.MessageBAMValue
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
-- dasselbe fuer 9022/9023 und, als Kontrolle, fuer 9028/9029
```

| Typenpaar | gemeinsame Werte in Fenster B |
|---|---:|
| **9032 / 9033** (Sender- und Empfaengercode) | **0** |
| **9022 / 9023** (Gutschriftsanzeige und Übertragungsnummer) | **0** |
| 9028 / 9029 (Material-Nr. Kunde und Lieferant, `_FORS`) | **15.790** |

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **Kommt regelmäßig vor** | **ja, aber am unteren Rand** (4,17 %) | Das Statement braucht die **Verdichtung auf `MessageID`** — sie steht in jedem Statement dieser Runde als `GROUP BY m.MessageID` und kostet nichts. Die Trefferspalte muss mehrere Typen tragen können; die Form `Kundenmaterialnummer +2` ist **nicht** der Normalfall, aber sie kommt bei jedem 24. Paar vor und darf nicht abgeschnitten werden |
| Kommt praktisch nie vor | nein | — Die Oberfläche braucht die Mehrfachform. Sie muss sie aber nicht optimieren |
| **Die Paare `9032`/`9033` und `9022`/`9023` erklären den Großteil** | **nein — beide erklären gar nichts** | Beide teilen **null** Werte. Die identischen Anzahlen in M28‑2 stammen daher, dass beide Typen **gemeinsam gesetzt** werden, nicht daher, dass sie denselben Wert tragen. Siehe Absatz unten |

**Wo die vorformulierte Zeile nicht passt — die Erklärung ist richtig, das Paar ist falsch.** Der
Gedanke „identische Anzahlen deuten auf gemeinsame Werte" trifft zu, aber für ein anderes Paar:
**9028/9029** teilen alle 15.790 Werte und sind damit die größte Kombination überhaupt. Bei
9032/9033 und 9022/9023 sind die Anzahlen identisch, **weil die Konfiguration beide Typen zugleich
befüllt** — Sendercode und Empfaengercode stehen auf derselben Nachricht, tragen aber verschiedene
Werte, wie man es von einem Sender und einem Empfänger erwartet.

**Das ist ein Muster der Konfiguration und nicht der Daten**, und es gehört als Befund in die
Kuratierung von M39: Wer aus zwei Typen mit identischer Abdeckung schließt, es sei derselbe
Sachverhalt, irrt in zwei von drei Fällen.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Zahl der Paare `(MessageID, MessageBAMValue)` mit zwei oder mehr Typen über
> **Fenster B** (n = 895.685 Paare auf 214.330 Nachrichten).
>
> *Behauptet wird:* dass die Trefferspalte der Suche mehrere Typen tragen können muss.
>
> **Die Lücke:** Fenster B ist ein Monat und zu 84 Prozent `NEXANS`. Die 4,17 Prozent sind das
> Mischungsverhältnis **dieses** Monats; ein Mandant mit anderer Konfiguration hat eine andere Quote
> — bei `IBIS` sind es die Kombinationen `0,1` und `1,2` mit zusammen 3.576 Paaren, bei `SUTTONS`
> und `ZAST` kommt im Fenster keine vor. Was trägt: *Mehrfachtypen sind kein Randfall und müssen
> darstellbar sein.* Was nicht gemessen ist: *wie häufig sie bei einem einzelnen kleinen Mandanten
> sind.*

---

# M38 — Wie sehen die Werte aus?

**Frage.** Entscheidet, ob exakte Suche fachlich benutzbar ist. Trägt eine Lieferscheinnummer im
Bestand führende Nullen, die der Nutzer nicht mittippt, findet die exakte Suche nichts.

## Statement

```sql
SELECT b.MessageBAMType, bt.MessageBAMTypeDescription AS beschreibung, COUNT(*) AS werte,
       MIN(CHAR_LENGTH(b.MessageBAMValue)) AS laenge_min,
       MAX(CHAR_LENGTH(b.MessageBAMValue)) AS laenge_max,
       ROUND(AVG(CHAR_LENGTH(b.MessageBAMValue)), 2) AS laenge_schnitt,
       ROUND(100 * AVG(b.MessageBAMValue REGEXP '^[0-9]+$'), 2) AS nur_ziffern_pz,
       ROUND(100 * AVG(LEFT(b.MessageBAMValue, 1) = '0'), 2) AS fuehrende_null_pz,
       ROUND(100 * AVG(CHAR_LENGTH(b.MessageBAMValue) <> CHAR_LENGTH(TRIM(b.MessageBAMValue))), 2) AS rand_leer_pz
FROM Message m JOIN MessageBAM b ON b.MessageID = m.MessageID
LEFT JOIN MessageBAMType bt ON bt.MessageBAMType = b.MessageBAMType
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY b.MessageBAMType, bt.MessageBAMTypeDescription
ORDER BY werte DESC;
```

> **Die Randleerzeichen werden über die Länge geprüft und nicht über `<>`.** Unter
> `utf8mb4_general_ci` gilt `'a ' = 'a'` — ein Vergleich mit `TRIM` liefert dort immer `false` und
> hätte die Frage nicht beantwortet, sondern still mit „nein" beantwortet. Dieselbe Falle wie
> `SUM(bit)` in M23‑2.

**`EXPLAIN`** — Einstieg über das Fenster, `MessageBAM` als `ref` über den Primärschlüssel,
`Using index`:

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---:|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | NULL | 409.756 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | `b` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 8 | `Using index` |
| 1 | `bt` | `eq_ref` | `PRIMARY` | 2 | `b.MessageBAMType` | 1 | |

## Ergebnis — Fenster B, alle 46 vorkommenden Typen

Die Spalte „Werte" zählt **Zeilen**, nicht verschiedene Werte. Summe: **936.529** — dieselbe Zahl,
die M40 unabhängig über die Mandanten erreicht.

| Typ | Beschreibung | Werte | Länge min–max (Ø) | nur Ziffern | **führende Null** | **Rand­leerzeichen** |
|---:|---|---:|---|---:|---:|---:|
| 9018 | Kundenmaterialnummer_K_SAP | 143.213 | 3–22 (14,11) | 9,16 % | 1,30 % | **25,88 %** |
| 3 | Rechnungsnummer | 128.716 | 7–8 (7,98) | 100 % | 0 % | 0 % |
| 9019 | Bestellnummer vom Kunden_K_SAP | 110.707 | 6–16 (9,12) | 32,93 % | 1,26 % | 0 % |
| 9020 | Lieferschein, Entnahme, PUS_K_SAP | 76.512 | 1–15 (8,20) | 99,70 % | 9,93 % | 0,01 % |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 55.833 | **3–35** (9,99) | 44,40 % | 3,49 % | 0 % |
| 9004 | Unsere Material-Nr._L_SAP | 55.697 | 8–13 (8,30) | 94,09 % | 5,89 % | 0 % |
| 9002 | Lieferplannummer_L_SAP | 41.333 | 10–10 (10,00) | 100 % | 0 % | 0 % |
| **9017** | (JIT-) Abrufnummer_K_SAP | 39.412 | **1–18** (4,96) | 88,11 % | **28,95 %** | 0 % |
| 9015 | Kundenwerk_K_SAP | 29.659 | 1–14 (3,48) | 38,92 % | 18,81 % | 0 % |
| 9014 | Lieferantennummer beim Kunden_K_SAP | 29.380 | 2–12 (7,68) | 91,28 % | 6,59 % | 0 % |
| 9016 | Abladestelle_K_SAP | 27.753 | 1–11 (5,16) | 33,66 % | 2,11 % | 0 % |
| 9001 | Abrufnummer_L_SAP | 24.136 | 1–4 (2,70) | 100 % | 0 % | 0 % |
| 9027 | Bestellnummer_FORS | 15.790 | 9–15 (9,27) | 4,51 % | 0 % | 0 % |
| 9028 / 9029 | Material-Nr. Kunde/Lieferant_FORS | je 15.790 | 6–16 (12,68) | 4,51 % | 0,25 % | 0 % |
| 9032 | Sendercode_K_SAP | 14.864 | 5–16 (10,07) | 0,76 % | 0,89 % | 2,15 % |
| 9033 | Empfaengercode_K_SAP | 14.864 | 5–25 (22,86) | 6,26 % | 1,39 % | 2,15 % |
| 9022 | Gutschriftsanzeigen-Nummer_K_SAP | 14.496 | 6–12 (11,01) | 100 % | 0,04 % | 0 % |
| 9023 | Übertragungsnummer Gutschrift_K_SAP | 12.966 | 2–10 (5,89) | 100 % | 0,17 % | 0 % |
| 0 | Bestellnummer | 9.578 | 3–13 (9,56) | 96,12 % | 1,24 % | 0 % |
| 1 | Auftragsnummer | 9.529 | 5–10 (7,00) | 87,14 % | 0 % | 0 % |
| 9005 | Werk_L_SAP | 8.246 | 1–4 (3,84) | 100 % | 0,05 % | 0 % |
| **9000** | Abladestelle_L_SAP | 7.764 | 1–4 (3,02) | 100 % | **29,39 %** | 0 % |
| **9006** | Lieferschein-Nr._L_SAP | 7.057 | 8–10 (8,10) | 99,96 % | **33,88 %** | 0 % |
| 9034 | Bestellnummer_L_SAP | 6.092 | 1–12 (10,00) | 99,69 % | 0 % | 0 % |
| 2 | Lieferscheinnummer | 3.404 | 5–8 (7,00) | 99,27 % | 0 % | 0 % |
| 9038 | Packmittelnummer Lieferant_L_SAP | 2.639 | 1–22 (8,84) | 63,81 % | 23,23 % | 0 % |
| **9021** | Transportnummer_K_SAP | 2.454 | 10–10 (10,00) | 100 % | **92,95 %** | 0 % |
| 9037 | Packmittelnummer Kunde_L_SAP | 1.846 | 1–22 (7,86) | 81,80 % | 7,31 % | 0 % |
| **9007** | Transport-Nummer_L_SAP | 1.727 | 8–18 (8,56) | 96,35 % | **47,83 %** | 0 % |
| 9039 | Daten-Sender-Nummer_L_SAP | 1.443 | 6–6 (6,00) | 100 % | 0 % | 0 % |
| 2000 | OrderNumber | 1.329 | 3–12 (6,22) | 95,64 % | 1,20 % | 0 % |
| **2001** | VendorReference | 1.253 | 14–14 (14,00) | 100 % | **100 %** | 0 % |
| 9010 | Materialbeleg (Entnahme)_L_SAP | 1.151 | 10–10 (10,00) | 100 % | 0 % | 0 % |
| **9024** | Rechnungsnummer_K_SAP | 988 | 10–10 (10,00) | 100 % | **100 %** | 0 % |
| **9036** | Lagerort Kunde_L_SAP | 953 | 3–4 (3,99) | 100 % | **99,06 %** | 0 % |
| **9012** | Charge_L_SAP | 767 | 10–10 (10,00) | 100 % | **100 %** | 0 % |
| 9009 | Beleg-Nr. GS_L_SAP | 739 | 10–10 (10,00) | 100 % | 31,66 % | 0 % |
| 9008 | Beleg-Nr. TSL_L_SAP | 234 | 8–10 (9,77) | 100 % | 0 % | 0 % |
| 9013 | Nr. TSL_L_SAP | 234 | 10–10 (10,00) | 100 % | 11,54 % | 0 % |
| **2002** | InvoiceNumber VTG | 92 | 10–10 (10,00) | 100 % | **100 %** | 0 % |
| **9011** | Anlieferungs-Nr. ae_L_SAP | 58 | 10–10 (10,00) | 100 % | **100 %** | 0 % |
| 9025 | Abladestelle_FORS | 20 | 3–4 (3,50) | 100 % | 50 % | 0 % |
| 9030 | Sender_Ident_FORS | 10 | 8–10 (9,00) | 0 % | 0 % | 0 % |
| 9031 | Empf_Ident_FORS | 10 | 11–12 (11,50) | 0 % | 0 % | 0 % |
| 2003 | OrderLoadNo | 1 | 10–10 (10,00) | 100 % | 0 % | 0 % |

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Werte sind einheitlich lang und ohne führende Nullen | **nein** | — Exakte Suche allein ist **nicht** zumutbar |
| **Führende Nullen sind verbreitet** | **ja, sehr** | **Sechs Typen tragen sie auf 100 % ihrer Werte** (2001, 9024, 9012, 2002, 9011 und, mit 99,06 %, 9036), einer auf 92,95 % (9021), einer auf 47,83 % (9007), und die kuratierte Lieferschein-Nr. **9006 auf 33,88 %**. Exakte Suche allein reicht nicht. Entweder Präfixsuche (M34 trägt sie, E6 nennt den Preis) **oder** eine dokumentierte Normalisierung — die dann **nicht geraten**, sondern aus dieser Messung begründet wird |
| **Werte tragen Leerzeichen am Rand** | **ja, bei einem Typ massiv** | **9018 (Kundenmaterialnummer) trägt sie auf 25,88 % der Werte** — ausgerechnet der Typ, der bei `NEXANS` auf 92,26 % der Wurzeln sitzt (M39) und damit der naheliegendste Suchtyp ist. Dazu 9032 und 9033 mit je 2,15 %. Das ist vor jedem Vergleich zu behandeln und gehört in die Antwortform, nicht in eine stille Korrektur |
| **Die Längen streuen je Typ stark** | **ja** | Von 1 bis 35 Zeichen. Neun Typen haben Werte der Länge **1**, fünfzehn Typen eine feste Länge. **Eine feste Mindestlänge über alle Typen ist der falsche Hebel** — sie sperrte neun Typen aus oder ließe 264.469 Treffer zu (E6). Bestätigt M33 |

**Wo die vorformulierte Zeile nicht passt — „rein numerisch" ist keine brauchbare Vorprüfung.** Der
Auftrag erhebt den Anteil rein numerischer Werte, ohne eine Lesart dafür zu formulieren. Gemessen
streut er über die volle Breite: 100 % bei sechzehn Typen, **0,76 %** bei 9032 und **0 %** bei 9030
und 9031. Eine Oberfläche, die aus der Eingabe auf den Typ schließen wollte („nur Ziffern → das ist
eine Belegnummer"), hätte damit keine Grundlage. **Das ist die Vorprüfung, die es nicht geben
wird** — und Regel Q4 hätte sie ohnehin verboten.

**Und ein Randbefund, der zur Normalisierungsfrage gehört:** Die Randleerzeichen von 9018 sind unter
`utf8mb4_general_ci` für den **Vergleich** folgenlos — `'4711 ' = '4711'` ist wahr, weil die
Sortierung Leerzeichen am Ende auffüllt. Sie sind es **nicht** für die Anzeige und **nicht** für ein
Präfixmuster: `LIKE '4711 %'` und `LIKE '4711%'` sind verschieden. Wer die Werte anzeigt oder
präfixweise sucht, muss sie behandeln; wer nur exakt vergleicht, muss es nicht.

---

# M39 — Welcher Typ trägt bei welchem Mandanten, je Rolle?

**Frage.** Die Grundlage für die Neukuratierung der Suchfelder. Die Auswahl aus Schritt 4
(`9006`, `9001`) ist durch M28‑2 widerlegt: Sie trifft auf den Wurzeln 5,02 % und 11,79 %, während
`9018` dort 92,26 % erreicht.

M28‑2, erweitert auf **alle Mandanten mit Nachrichten** statt nur `NEXANS`, und um die Rolle
**ohne Verkettung** ergänzt.

## M39‑0 Die Nenner — und die Kontrolle gegen M28‑1

```sql
SELECT pm.MandantID, COUNT(*) AS zeilen,
  SUM(m.Source + 0 = 1) AS wurzel,
  SUM(m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '') AS kind,
  SUM(m.TargetMessageID IS NOT NULL AND m.TargetMessageID <> '') AS eingang,
  SUM(m.Target + 0 = 1) AS ergebnis,
  SUM(m.Source + 0 = 0 AND m.Target + 0 = 0
      AND (m.SourceMessageID IS NULL OR m.SourceMessageID = '')
      AND (m.TargetMessageID IS NULL OR m.TargetMessageID = '')) AS ohne_kette
FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY pm.MandantID ORDER BY zeilen DESC;
```

| `MandantID` | Zeilen | Wurzel | Kind | Eingang | Ergebnis | ohne Kette |
|---|---:|---:|---:|---:|---:|---:|
| `NEXANS` | 180.251 | 28.524 | 101.270 | 38.628 | 1.669 | 10.652 |
| `SUTTONS` | 21.516 | 639 | 1.247 | 0 | 0 | 19.630 |
| `VOTG` | 6.104 | 0 | 0 | 0 | 0 | 6.104 |
| `IBIS` | 4.331 | 1.714 | 36 | 0 | 0 | 2.581 |
| `IBISGUS` | 1.722 | 22 | 1.722 | 0 | 0 | 0 |
| `ZAST` | 283 | 20 | 263 | 0 | 0 | 0 |
| `WOC` | 118 | 0 | 0 | 0 | 0 | 118 |
| `SYSTEM` | 5 | 0 | 0 | 0 | 0 | 5 |

**Zeile für Zeile identisch mit M28‑1 (a)** vom 10.08.2026. Die Kontrolle ist bestanden; alles
Weitere steht auf demselben Boden.

**Laufzeit:** 3,384 s, beste von 3.

## M39‑1 Der Typ je Rolle, je Mandant

```sql
SELECT pm.MandantID, b.MessageBAMType AS typ, bt.MessageBAMTypeDescription AS beschreibung,
  COUNT(DISTINCT m.MessageID) AS nachrichten,
  COUNT(DISTINCT CASE WHEN m.Source + 0 = 1 THEN m.MessageID END) AS auf_wurzeln,
  COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '' THEN m.MessageID END) AS auf_kindern,
  COUNT(DISTINCT CASE WHEN m.TargetMessageID IS NOT NULL AND m.TargetMessageID <> '' THEN m.MessageID END) AS auf_eingaengen,
  COUNT(DISTINCT CASE WHEN m.Target + 0 = 1 THEN m.MessageID END) AS auf_ergebnissen,
  COUNT(DISTINCT CASE WHEN m.Source + 0 = 0 AND m.Target + 0 = 0
                       AND (m.SourceMessageID IS NULL OR m.SourceMessageID = '')
                       AND (m.TargetMessageID IS NULL OR m.TargetMessageID = '') THEN m.MessageID END) AS ohne_kette
FROM Message m
JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
JOIN MessageBAM b ON b.MessageID = m.MessageID
LEFT JOIN MessageBAMType bt ON bt.MessageBAMType = b.MessageBAMType
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY pm.MandantID, b.MessageBAMType, bt.MessageBAMTypeDescription
HAVING nachrichten >= 10
ORDER BY pm.MandantID, auf_wurzeln DESC, nachrichten DESC;
```

**`HAVING nachrichten >= 10`** ist aus M28‑2 beibehalten und schneidet Rauschen ab; die Grenze steht
hier, damit sie nicht verschwiegen ist. **Die fünf Rollen laufen in einem einzigen Durchlauf** — die
im Auftrag vorgesehene Aufteilung je Mandant war nicht nötig.

**`EXPLAIN`** — dieselbe Gestalt wie M28‑2, `MessageBAM` als `ref` über den Primärschlüssel-Präfix:

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---:|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | NULL | 409.756 | `Using index condition; Using where; Using temporary; Using filesort` |
| 1 | `p` | `eq_ref` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | `pm` | `ref` | `PRIMARY` | 146 | `p.ProjectID` | 1 | `Using index` |
| 1 | `b` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 8 | `Using index` |

### Ergebnis — `NEXANS`

Die Quoten sind gegen die Nenner aus M39‑0 gerechnet.

| Typ | Beschreibung | **je Wurzel** | je Kind | je Ergebnis | **je Zeile ohne Kette** |
|---:|---|---:|---:|---:|---:|
| **9018** | Kundenmaterialnummer_K_SAP | **92,26 %** | 0,62 % | 4,13 % | 24,15 % |
| 9014 | Lieferantennummer beim Kunden_K_SAP | 92,23 % | 0,59 % | 3,59 % | 24,14 % |
| 9015 | Kundenwerk_K_SAP | 92,20 % | 0,62 % | 4,13 % | 24,14 % |
| 9020 | Lieferschein, Entnahme, PUS_K_SAP | 85,31 % | **0,72 %** | 3,18 % | 24,15 % |
| 9016 | Abladestelle_K_SAP | 83,50 % | 0,29 % | 3,77 % | 21,92 % |
| 9032 / 9033 | Sender-/Empfaengercode_K_SAP | 48,16 % | 0,67 % | 2,40 % | 6,39 % |
| 9019 | Bestellnummer vom Kunden_K_SAP | 46,24 % | 0,62 % | 4,13 % | 19,04 % |
| 9017 | (JIT-) Abrufnummer_K_SAP | 45,99 % | 0,05 % | 0,96 % | 0,27 % |
| 9022 / 9023 | Gutschrift_K_SAP (zwei Typen) | 43,54 % | 0 % | 0 % | 5,08 % |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 16,82 % | 0 % | **95,87 %** | 14,37 % |
| 9004 | Unsere Material-Nr._L_SAP | 16,80 % | 0 % | **95,87 %** | 14,37 % |
| 9000 | Abladestelle_L_SAP | 16,80 % | 0 % | **95,87 %** | 7,41 % |
| 9005 | Werk_L_SAP | 16,17 % | 0 % | **95,87 %** | 14,92 % |
| 9002 | Lieferplannummer_L_SAP | 11,79 % | 0 % | **95,87 %** | 9,41 % |
| **9001** | Abrufnummer_L_SAP *(kuratiert, Position 2)* | **11,79 %** | 0 % | **95,87 %** | 4,67 % |
| **9006** | Lieferschein-Nr._L_SAP *(kuratiert, Position 1)* | **5,02 %** | 0 % | 0 % | 4,70 % |
| 9039 / 9007 | Daten-Sender-/Transport-Nummer_L_SAP | je 5,02 % | 0 % | 0 % | 0 % / 0,54 % |
| 9034 | Bestellnummer_L_SAP | 4,39 % | 0 % | 0 % | 0 % |
| 9037 | Packmittelnummer Kunde_L_SAP | 4,30 % | 0 % | 0 % | 0 % |
| 9038 | Packmittelnummer Lieferant_L_SAP | 4,05 % | 0 % | 0 % | 0 % |
| 9036 | Lagerort Kunde_L_SAP | 3,27 % | 0 % | 0 % | 0 % |
| 9021 | Transportnummer_K_SAP | 1,59 % | 0,36 % | 0 % | **17,02 %** |
| 9024 | Rechnungsnummer_K_SAP | 0,03 % | 0,20 % | 3,18 % | 2,01 % |
| 9025 / 9027–9031 | sechs Typen `_FORS` | je 0,02 % | 0,00 % | 0 % | 0,05 % |
| 9009 / 9010 | Beleg-Nr. GS / Materialbeleg_L_SAP | 0 % | 0 % | 0 % | **6,94 %** |
| 9012 | Charge_L_SAP | 0 % | 0 % | 0 % | 2,74 % |
| 9008 / 9013 | Beleg-Nr. / Nr. TSL_L_SAP | 0 % | 0 % | 0 % | 2,20 % |
| 9011 | Anlieferungs-Nr. ae_L_SAP | 0 % | 0 % | 0 % | 0,54 % |

**Die Spalte „je Wurzel" reproduziert M28‑2 auf die zweite Nachkommastelle** — dieselbe Messung,
einen Tag später, mit einem anders gebauten Statement.

### Ergebnis — die übrigen Mandanten

| Mandant | Typ | Beschreibung | je Wurzel | je Kind | je Zeile ohne Kette |
|---|---:|---|---:|---:|---:|
| `IBIS` (1.714 / 36 / 2.581) | **0** | Bestellnummer | **92,59 %** | **100 %** | 59,55 % |
| | 1 | Auftragsnummer | 7,41 % | 0 % | 45,14 % |
| | 2 | Lieferscheinnummer | 0 % | 0 % | 22,04 % |
| | 3 | Rechnungsnummer | 0 % | 0 % | 11,27 % |
| `IBISGUS` (22 / 1.722 / 0) | **0** | Bestellnummer | **100 %** | **92,63 %** | — |
| | 1 | Auftragsnummer | 0 % | 7,38 % | — |
| `SUTTONS` (639 / 1.247 / 19.630) | 2000 | OrderNumber | **0 %** | **100 %** | **0,33 %** |
| | 2001 | VendorReference | **0 %** | 99,76 % | **0,04 %** |
| `ZAST` (20 / 263 / 0) | 3 | Rechnungsnummer | **0 %** | **100 %** | — |
| `VOTG` (0 / 0 / 6.104) | 2002 | InvoiceNumber VTG | — | — | **1,51 %** |
| `WOC` (0 / 0 / 118) | 9014 | Lieferantennummer beim Kunden_K_SAP | — | — | **74,58 %** |
| `SYSTEM` (0 / 0 / 5) | — | keine BAM-Zeile | — | — | — |

**Laufzeit:** 13,925 s, beste von 3 nach einem Aufwärmlauf. Die Kostenerwartung aus M28‑2 (12,682 s
für **einen** Mandanten) ist für **alle acht** um 10 % überschritten worden; die 60-Sekunden-Grenze
war nie in Sicht, und die im Auftrag vorgesehene Aufteilung je Mandant war nicht nötig.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **Je Mandant gibt es zwei bis fünf Typen über 80 % auf den Wurzeln** | **ja, wo es Wurzeln gibt** | `NEXANS` **fünf** (9018, 9014, 9015, 9020, 9016 — 83,50 bis 92,26 %), `IBIS` **einen** (Typ 0, 92,59 %), `IBISGUS` **einen** (Typ 0, 100 %). Die Liste ist kurz genug für eine Auswahl, und **die Suchfelder werden je Mandant kuratiert** |
| **Bei einem Mandanten trägt kein Typ** | **ja, bei dreien — aber nicht auf der Wurzel** | Bei `SUTTONS`, `ZAST` und `VOTG` erreicht auf den **Wurzeln** kein Typ auch nur ein Prozent. Bei `SUTTONS` und `ZAST` liegt die Belegnummer auf den **Kindern** (100 %), bei `VOTG` gibt es keine Kette. Dort bietet die Suche keine Typwahl an, sondern sucht über alle — kein Platzhalter, keine leere Auswahl. Dieselbe Regel wie bei der leeren Spalte in [`nachrichtenliste.md`](nachrichtenliste.md) §8.1 |
| **Die tragenden Typen unterscheiden sich stark zwischen den Mandanten** | **ja** | 9018 bei `NEXANS`, Typ 0 bei `IBIS`/`IBISGUS`, 2000/2001 bei `SUTTONS`, Typ 3 bei `ZAST`, 2002 bei `VOTG`, 9014 bei `WOC`. **Bestätigt, dass die Kuratierung je Mandant gehört und nicht global** |

**Wo die vorformulierte Zeile nicht passt — die tragende Rolle ist nicht bei jedem Mandanten die
Wurzel.** Die drei Zeilen fragen ausschließlich nach den Wurzeln. Gemessen verteilt sich die
Belegnummer bei den acht Mandanten auf **vier verschiedene Rollen**:

| Rolle, auf der der beste Typ sitzt | Mandanten |
|---|---|
| **Wurzel** | `NEXANS` (92,26 %), `IBIS` (92,59 %), `IBISGUS` (100 %) |
| **Kind** | `SUTTONS` (100 %), `ZAST` (100 %), `IBISGUS` (92,63 %) |
| **Merge-Ergebnis** | `NEXANS` (95,87 %, sechs `_L_SAP`-Typen) |
| **ohne Kette** | `WOC` (74,58 %), `IBIS` (59,55 %) |

Eine Kuratierung, die „den Typ auf der Wurzel" wählt, träfe bei `SUTTONS` und `ZAST` **null Prozent**.

**Und ein zweiter Befund ohne vorformulierte Zeile: `auf_eingaengen` ist über alle Mandanten und alle
46 Typen durchgängig null.** M26 hat das für Fenster A und `NEXANS` gemessen (432 Merge-Eingänge, 0
BAM-Werte). Hier ist es über **Fenster B und alle Mandanten** bestätigt: **38.628 Merge-Eingänge,
kein einziger BAM-Wert.** M41 zählt dieselbe Null aus der Gegenrichtung. Damit ist die Aussage aus
`messungen-schritt6.md` — „beim Merge kostet das Ausblenden keine Belegnummer" — von einer
Tagesstichprobe auf einen Monat gehoben.

**Ein dritter: auf den Zeilen *ohne Kette* trägt bei `NEXANS` kein Typ.** Der beste erreicht dort
**24,15 %** (9018, 9020) — das ist derselbe Befund wie M11 über alle Zeilen, nur schärfer
geschnitten. Für 10.652 der 180.251 `NEXANS`-Zeilen des Fensters gibt es also **keinen** tragfähigen
Suchtyp, und zwar nicht wegen der Auswahl, sondern wegen der Datenlage.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Abdeckung je Typ, Rolle und Mandant über **Fenster B** — 214.330 Nachrichten, acht
> Mandanten, 46 Typen, `HAVING nachrichten >= 10`.
>
> *Behauptet wird:* dass die Suchfelder danach je Mandant kuratiert werden können.
>
> **Die Lücke, und sie ist doppelt.** Erstens ist die Grundmenge ein Monat: Bei `ZAST` stehen 283
> Zeilen und bei `SYSTEM` fünf — die 100 % von `ZAST` ruhen auf 263 Kindern und sind eine Aussage
> über diesen Monat, nicht über den Bestand. Zweitens schneidet `HAVING nachrichten >= 10` genau die
> Typen weg, die für einen kleinen Mandanten relevant sein könnten; bei `VOTG` und `WOC` bleibt
> deshalb je ein Typ übrig, und ob es dort weitere gibt, sagt diese Messung **nicht**. M40 beantwortet
> genau diese Frage über den ganzen Bestand, und die Antwort weicht ab.

---

# M40 — Ist `MessageBAMMandant` Sichtbarkeit oder Bestand?

**Frage.** `EDITIONLINGERI`, `SYSTEM` und `WOC` haben **keinen** konfigurierten BAM-Typ, `ZAST` genau
einen. Haben diese Mandanten trotzdem `MessageBAM`-Zeilen? Und gibt es Typen in den Daten, die
**nicht** konfiguriert sind?

## M40‑1 Was konfiguriert ist

```sql
SELECT bm.MandantID, COUNT(*) AS konfigurierte_typen,
       GROUP_CONCAT(bm.MessageBAMType ORDER BY bm.MessageBAMType) AS typen
FROM MessageBAMMandant bm GROUP BY bm.MandantID ORDER BY konfigurierte_typen DESC, bm.MandantID;
```

| `MandantID` | konfigurierte Typen | Bereich |
|---|---:|---|
| `NEXANS` | 40 | 9000–9039 |
| `VOTG` | 12 | 2000–2011 |
| `NXHBE` | 6 | 1000–1005 |
| `IBIS` | 4 | 0–3 |
| `IBISGUS` | 4 | 0–3 |
| `SUTTONS` | 2 | 2000, 2001 |
| `ZAST` | 1 | 3 |
| **`EDITIONLINGERI`, `SYSTEM`, `WOC`** | **0** | — |

Deckt sich mit M7. Die Gegenüberstellung mit den Stammdaten:

| `MandantID` | Projekte | Prozesse | konfigurierte Typen |
|---|---:|---:|---:|
| `EDITIONLINGERI` | 3 | 9 | **0** |
| `IBIS` | 46 | 192 | 4 |
| `IBISGUS` | 19 | 89 | 4 |
| `NEXANS` | 17 | 733 | 40 |
| `NXHBE` | 2 | 17 | 6 |
| `SUTTONS` | 1 | 17 | 2 |
| `SYSTEM` | 2 | 4 | **0** |
| `VOTG` | 39 | 390 | 12 |
| `WOC` | 1 | 4 | **0** |
| `ZAST` | 4 | 35 | 1 |

**Laufzeiten:** 0,001 s und 0,003 s — reine Stammdaten.

## M40‑2 Was vorkommt — Fenster B

```sql
SELECT pm.MandantID, COUNT(*) AS bam_zeilen,
       COUNT(DISTINCT b.MessageBAMType) AS vorkommende_typen,
       COUNT(DISTINCT CASE WHEN bm.MandantID IS NOT NULL THEN b.MessageBAMType END) AS schnittmenge,
       COUNT(DISTINCT CASE WHEN bm.MandantID IS NULL THEN b.MessageBAMType END) AS nicht_konfiguriert,
       GROUP_CONCAT(DISTINCT CASE WHEN bm.MandantID IS NULL THEN b.MessageBAMType END
                    ORDER BY b.MessageBAMType) AS unkonfigurierte_typen
FROM Message m
JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
JOIN MessageBAM b ON b.MessageID = m.MessageID
LEFT JOIN MessageBAMMandant bm ON bm.MandantID = pm.MandantID AND bm.MessageBAMType = b.MessageBAMType
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY pm.MandantID ORDER BY bam_zeilen DESC;
```

| `MandantID` | BAM-Zeilen | vorkommende Typen | konfiguriert | **nicht konfiguriert** |
|---|---:|---:|---:|---|
| `NEXANS` | 782.539 | 38 | 38 | — |
| **`ZAST`** | **125.942** | 1 | 1 | — |
| `IBIS` | 22.348 | 4 | 4 | — |
| `IBISGUS` | 2.937 | 2 | 2 | — |
| `SUTTONS` | 2.581 | 2 | 2 | — |
| `VOTG` | 94 | 3 | 3 | — |
| **`WOC`** | **88** | 1 | **0** | **9014** |
| `SYSTEM` | — | — | — | keine BAM-Zeile |

Summe: **936.529** BAM-Zeilen — dieselbe Zahl, die M38 unabhängig über die Typen erreicht.

**Laufzeit:** 7,847 s, beste von 3.

## M40‑3 Die vier Mandanten ohne beziehungsweise mit einem Typ — über den **Bestand**

**Ohne Zeitfenster, begründet (Regel L9).** Die Frage lautet „hat dieser Mandant überhaupt
BAM-Zeilen"; ein Fenster kann sie grundsätzlich nicht beantworten, weil eine leere Antwort dann zwei
Bedeutungen hätte. Die Erhebung ist einmalig und **kein** Statement der Anwendung. Sie steigt über
`ProjectMandant` ein und nicht über `Message`, damit sie nur die Prozesse dieser Mandanten anfasst:

```sql
SELECT pm.MandantID, COUNT(DISTINCT m.MessageID) AS nachrichten,
       COUNT(b.MessageBAMValue) AS bam_zeilen,
       COUNT(DISTINCT b.MessageBAMType) AS vorkommende_typen,
       GROUP_CONCAT(DISTINCT b.MessageBAMType ORDER BY b.MessageBAMType) AS typen
FROM ProjectMandant pm
JOIN Process p ON p.ProjectID = pm.ProjectID
JOIN Message m ON m.ProcessID = p.ProcessID
LEFT JOIN MessageBAM b ON b.MessageID = m.MessageID
WHERE pm.MandantID IN ('EDITIONLINGERI','SYSTEM','WOC','ZAST','NXHBE')
GROUP BY pm.MandantID ORDER BY nachrichten DESC;
```

| `MandantID` | konfigurierte Typen | Nachrichten | **BAM-Zeilen** | vorkommende Typen |
|---|---:|---:|---:|---|
| `ZAST` | 1 | 5.036 | **2.222.395** | 3 |
| **`WOC`** | **0** | 2.529 | **2.067** | **9014** |
| `SYSTEM` | 0 | 151 | **0** | — |
| `NXHBE` | 6 | 9 | 176 | 1001, 1002, 1003, 1004 |
| **`EDITIONLINGERI`** | 0 | **0** | — | — |

`EDITIONLINGERI` hat drei Projekte und neun Prozesse, aber **keine einzige Nachricht** in der
Testkopie. Der `EXPLAIN` weist die Erhebung als `index`-Durchlauf über `ProejctIDIDX` aus
(`rows` 3.560.486) mit `Using index` — sie liest den Sekundärindex vollständig und fasst `Message`
nie an, dieselbe Gestalt wie M30‑2.

**Laufzeit:** 21,698 s, beste von 3. Kein Abbruch.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Mandanten ohne Konfiguration haben **keine** BAM-Zeilen | **teilweise — bei `SYSTEM` ja, bei `WOC` nein** | Die Konfiguration ist **keine** verlässliche Aussage über den Bestand. Suche und Detail-Block dürfen ihr nicht blind folgen |
| **Mandanten ohne Konfiguration haben BAM-Zeilen** | **ja, bei `WOC`** | **Die Konfiguration ist eine Sichtbarkeitsentscheidung.** Folgt der Detail-Block ihr, verschweigt er bei `WOC` **alle** vorhandenen Werte — 2.067 Zeilen im Bestand, 88 in Fenster B, auf 74,58 % der Zeilen ohne Kette. Zeigt er alle, ordnet die Konfiguration nur |
| **Es kommen Typen vor, die nirgends konfiguriert sind** | **ja, genau einer** | Typ **9014** bei `WOC`. Bei allen sieben konfigurierten Mandanten ist die Schnittmenge über Fenster B **vollständig** — es kommt kein unkonfigurierter Typ vor. Der Befund betrifft also **nicht** auch die großen Mandanten, anders als der Auftrag vermutet hat |

**Wo die vorformulierte Zeile nicht passt — die beiden ersten Zeilen treffen gleichzeitig zu, für
verschiedene Mandanten.** Der Auftrag behandelt sie als Alternative. Gemessen sind es drei Fälle
statt zwei:

- **`SYSTEM`** — keine Konfiguration, 151 Nachrichten, **null** BAM-Zeilen. Hier deckt sich die
  Konfiguration mit dem Bestand.
- **`WOC`** — keine Konfiguration, 2.529 Nachrichten, **2.067** BAM-Zeilen unter einem Typ, der für
  keinen Mandanten dieses Kürzels konfiguriert ist. Hier deckt sie sich nicht.
- **`EDITIONLINGERI`** — keine Konfiguration, **keine Nachricht**. Die Frage stellt sich nicht.

Der Fall `WOC` ist zusätzlich fachlich erklärbar und deshalb kein Ausreißer, den man wegdiskutieren
sollte: `WOC` ist das Auffangbecken für Verkehr **ohne hinterlegten Vertrag**
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2). Dass dort Nachrichten mit der BAM-Prägung
eines anderen Mandanten landen — 9014 ist ein `NEXANS`-Typ —, ist genau das, was ein Auffangbecken
tut. Er ist damit **nicht** der Beleg dafür, dass die Konfiguration allgemein unvollständig ist,
sondern dafür, dass sie an der Mandantengrenze nicht greift.

**Und der Befund, der in keine Zeile passt und der schwerer wiegt als alle drei: `ZAST` trägt
2.222.395 BAM-Zeilen auf 5.036 Nachrichten** — **441 Werte je Nachricht**, unter einem einzigen
konfigurierten Typ (3, Rechnungsnummer). Das sind 96,1 Prozent aller Zeilen des Typs 3 im gesamten
Bestand (2.268.697, M33‑2). `ZAST` ist nach Nachrichtenzahl der zweitkleinste Mandant und nach
BAM-Zeilen je Nachricht mit Abstand der größte. Für den Detail-Block heißt das: **Die Deckelung wird
nicht vom größten Mandanten erzwungen, sondern vom kleinsten** — und M41 beziffert es.

> Die drei vorkommenden Typen bei `ZAST` über den Bestand gegen den einen in Fenster B sind kein
> Widerspruch: Über den Monat kommt nur Typ 3 vor, über 22 Monate kommen zwei weitere hinzu. Welche
> das sind und ob sie konfiguriert sind, ist **nicht** erhoben — die Abfrage nennt nur die Anzahl.

---

# M41 — Wie viele Werte hat eine Nachricht insgesamt?

**Frage.** Der Deckel für den BAM-Block am Detail. M11 hat Werte **je Typ** gezählt (bis 3.035 bei
9027), M26‑1b **je Nachricht auf Wurzeln** (Schnitt 18,15, Maximum 765). Für den Block zählt die
Summe über alle Typen einer Nachricht.

## Statement

```sql
SELECT je.werte,
       SUM(je.wurzel) AS wurzel, SUM(je.kind) AS kind, SUM(je.eingang) AS eingang,
       SUM(je.ergebnis) AS ergebnis, SUM(je.ohne) AS ohne_kette, COUNT(*) AS gesamt
FROM (
  SELECT m.MessageID,
         (m.Source + 0 = 1) AS wurzel,
         (m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '') AS kind,
         (m.TargetMessageID IS NOT NULL AND m.TargetMessageID <> '') AS eingang,
         (m.Target + 0 = 1) AS ergebnis,
         (m.Source + 0 = 0 AND m.Target + 0 = 0
          AND (m.SourceMessageID IS NULL OR m.SourceMessageID = '')
          AND (m.TargetMessageID IS NULL OR m.TargetMessageID = '')) AS ohne,
         COUNT(b.MessageBAMValue) AS werte
  FROM Message m LEFT JOIN MessageBAM b ON b.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
  GROUP BY m.MessageID) je
GROUP BY je.werte ORDER BY je.werte;
```

Dasselbe ein zweites Mal mit `COUNT(DISTINCT b.MessageBAMType)` statt `COUNT(b.MessageBAMValue)`.
Der `LEFT JOIN` ist Absicht: Nachrichten **ohne** BAM-Wert gehören in die Verteilung, sonst
beschriebe der Median nur die Nachrichten, die man ohnehin schon gefunden hat.

## Ergebnis — Werte je Nachricht

| Rolle | n | davon mit Werten | Median | 90. | **99.** | 99,9. | **Maximum** |
|---|---:|---:|---:|---:|---:|---:|---:|
| **Wurzel** | 30.919 | 29.619 (95,8 %) | **9** | 13 | **186** | 931 | **9.109** |
| **Kind** | 104.538 | 4.285 (4,1 %) | 0 | 0 | 2 | 514 | **9.296** |
| **Merge-Eingang** | 38.628 | **0** | 0 | 0 | 0 | 0 | **0** |
| **Merge-Ergebnis** | 1.669 | 1.669 (**100 %**) | **20** | 193 | **1.345** | 3.295 | 3.409 |
| **ohne Kette** | 39.090 | 6.504 (16,6 %) | 0 | 6 | 36 | 199 | 447 |
| **gesamt** | 214.330 | 41.584 (19,4 %) | 0 | 9 | 42 | 508 | **9.296** |

Die Rollenspalten summieren sich auf 214.844 = 214.330 + **514** — genau die Zahl der Zeilen mit
zwei Rollen aus M28‑1c. Eine dritte unabhängige Kontrolle, dass die Rollen richtig geschnitten sind.

### In Klassen

| Werte je Nachricht | Wurzel | Kind | Eingang | Ergebnis | ohne Kette | gesamt |
|---|---:|---:|---:|---:|---:|---:|
| 0 | 1.300 | 100.253 | **38.628** | 0 | 32.586 | 172.746 |
| 1 | 1.743 | 1.773 | 0 | 0 | 1.336 | 4.664 |
| 2–5 | 477 | 1.626 | 0 | 0 | 410 | 2.513 |
| **6–20** | **25.456** | 439 | 0 | **861** | 4.052 | 30.752 |
| 21–50 | 912 | 99 | 0 | 310 | 451 | 1.742 |
| 51–100 | 474 | 87 | 0 | 169 | 115 | 807 |
| 101–500 | 447 | 152 | 0 | 259 | 140 | 887 |
| **über 500** | **110** | **109** | 0 | **70** | 0 | **219** |

**82,3 % der Wurzeln liegen in der Klasse 6–20.** Die 219 Nachrichten über 500 Werten sind 0,10 %
des Fensters — und sie sind der Fall, für den der Deckel gebaut wird.

## Ergebnis — verschiedene Typen je Nachricht

| Typen je Nachricht | Wurzel | Kind | Eingang | Ergebnis | ohne Kette | gesamt |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 1.300 | 100.253 | 38.628 | 0 | 32.586 | 172.746 |
| 1 | 1.909 | 2.190 | 0 | 0 | 1.534 | 5.445 |
| 2 | 0 | 1.462 | 0 | 0 | 129 | 1.591 |
| 3 | 1 | 0 | 0 | 0 | 736 | 737 |
| 5 | 524 | 0 | 0 | 0 | 2 | 526 |
| **6** | 754 | 49 | 0 | **1.625** | 1.112 | 3.492 |
| 7 | 8.427 | 192 | 0 | 4 | 2.283 | 10.854 |
| 8 | 3.078 | 262 | 0 | 6 | 245 | 3.413 |
| **9** | **10.476** | 130 | 0 | 34 | 463 | 11.076 |
| 10 | 424 | 0 | 0 | 0 | 0 | 424 |
| 11 | 621 | 0 | 0 | 0 | 0 | 621 |
| **13** | 3.362 | 0 | 0 | 0 | 0 | 3.362 |
| 17 | 1 | 0 | 0 | 0 | 0 | 1 |
| **18** | **42** | 0 | 0 | 0 | 0 | 42 |

Über die 41.584 Nachrichten **mit** mindestens einem Wert: Median **8**, 90. Perzentil **10**,
99. Perzentil **13**, Maximum **18**. Auf den Wurzeln allein: Median 9, 90. und 99. Perzentil je 13.
Jedes Merge-Ergebnis trägt **genau sechs** Typen (1.625 von 1.669) — die sechs `_L_SAP`-Typen mit
95,87 % aus M39.

**Laufzeiten:** Werte je Nachricht **17,352 s**, Typen je Nachricht **5,001 s**, je beste von 3.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| 99. Perzentil unter rund 50 Werten | **nein** | — Der Block kann **nicht** vollständig laden. Über alle Zeilen sind es 42, über die Wurzeln 186, über die Merge-Ergebnisse **1.345** |
| **99. Perzentil dreistellig** | **ja — drei- bis vierstellig** | **Der Block wird je Typgruppe gedeckelt, mit ehrlicher Restangabe** — nicht die Nachricht als Ganzes. Bei 9.296 Werten auf einer Nachricht wäre eine Gesamtdeckelung unbrauchbar: Sie schnitte willkürlich mitten in eine Gruppe |
| **Die Typenzahl je Nachricht bleibt klein (unter zehn)** | **im Median ja, im Rand nein** | Median 8, 90. Perzentil 10, Maximum **18**. **Die Gruppierung ist die richtige Gliederung und bleibt überschaubar** — 18 Gruppen sind eine lange, aber lesbare Seite; 9.296 Werte sind es nicht. Der Deckel gehört in die Gruppe, nicht über sie |
| **Die Rollen unterscheiden sich stark** | **ja, extrem** | Von **0 %** (Merge-Eingang, 38.628 von 38.628) bis **100 %** (Merge-Ergebnis, 1.669 von 1.669). **Der Deckel gilt trotzdem einheitlich** — eine rollenabhängige Grenze wäre für den Nutzer unerklärlich, und die Rolle steht ohnehin schon im Kettenblock |

**Wo die vorformulierte Zeile nicht passt — das Maximum liegt eine Größenordnung über M11.** Der
Auftrag nennt als Bezug „bis 3.035 Werte je Nachricht (M11, je Typ)" und „765 (M26‑1b, je
Nachricht)". Gemessen sind **9.109** auf einer Wurzel und **9.296** auf einem Kind. Die 765 aus
M26‑1b stammen aus **Fenster A**; über Fenster B ist der Rand zwölfmal höher. Das ändert die
Konsequenz nicht — gedeckelt wird ohnehin —, aber es ändert die Größenordnung, gegen die der Deckel
zu prüfen ist.

**Und ein zweiter Befund ohne vorformulierte Zeile: das Kind mit 9.296 Werten.** Nach M26 tragen
Kinder fast nie BAM-Werte (2,42 % in Fenster A, hier 4,1 %). Trotzdem steht das absolute Maximum des
Fensters auf einem **Kind**, und 109 Kinder tragen über 500 Werte. Die Erklärung liefert M40:
`ZAST` und `SUTTONS` haben ihre Belegnummern auf den Kindern, und `ZAST` trägt 441 Werte je
Nachricht. **Der Deckel darf also nicht an der Rolle hängen** — genau das sagt die letzte Zeile
oben, und hier ist der Grund dafür.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Zahl der BAM-Werte und der verschiedenen Typen je Nachricht über **Fenster B**,
> nach Rolle (n = 214.330 Nachrichten, davon 41.584 mit mindestens einem Wert).
>
> *Behauptet wird:* dass eine Deckelung je Typgruppe genügt.
>
> **Die Lücke:** Gemessen ist die Zahl der Werte je Nachricht, **nicht** die Zahl je Typgruppe. Aus
> „18 Typen und 9.296 Werte" folgt nicht, dass die größte Gruppe 9.296/18 Werte hat — sie kann
> nahezu alle tragen. M11 legt genau das nahe: Dort stehen auf **zehn** Nachrichten je 15.790 Werte
> **eines einzigen** Typs (9027, 9028, 9029), bis zu 3.035 auf einer Nachricht. **Eine Deckelung je
> Gruppe ist damit notwendig und nachweislich nicht hinreichend belegt**; die Zahl, die sie
> bemessen würde — die Verteilung der Werte je (Nachricht, Typ) über Fenster B —, ist in dieser
> Runde **nicht** erhoben. Sie steht als **offene Frage 6**.

---

# Rahmen des Nachtrags vom 12.08.2026

M42 und M43 sind am **12.08.2026** nachgereicht worden. Der Rahmen ist aus §0 übernommen und
erneut protokolliert — er ist nicht angenommen:

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` — dieselbe Instanz wie am 11.08. |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**. **Erste Abfrage der Sitzung**, vor jedem anderen Statement. Am Ende des Nachtrags erneut geprüft: wieder **`1`** |
| Benutzer | **Lesebenutzer** `monitor_read@%`, unveränderte Rechte |
| Serverzeit zu Beginn | `2026-08-12 10:56:47` (`UTC_TIMESTAMP` `08:56:47`, also UTC+2) |
| Serverzeit am Ende | `2026-08-12 11:24:30` (`UTC_TIMESTAMP` `09:24:30`) |
| Grenze | `SET max_statement_time = 60` **in jeder Sitzung, als erstes oder zweites Statement**. Sie ist in diesem Nachtrag **kein einziges Mal** gerissen worden; das teuerste Statement lag bei 10,6 s |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `SHOW PROFILES`, `SET profiling_history_size = 100` |
| Wiederholungen | beste von fünf nach einem Aufwärmlauf; über zehn Sekunden beste von drei. Der Aufwärmlauf ist in jeder Auswertung **verworfen** und nicht bloß mitgemittelt |
| Regel S1 | kein `CREATE`, kein `ALTER`, kein Index — ausschließlich `SELECT`, `SHOW` und `EXPLAIN`. Anders als in der Hauptrunde war auch kein `KILL QUERY` nötig |
| Regel G1 | keine Belegnummer und kein BAM-Wert in dieser Datei. Alle Prüfwerte stehen in Sitzungsvariablen, die auf dem Server gesetzt und dort verbraucht werden |

**Eine Abweichung vom Client der Hauptrunde, und sie gehört benannt.** Der Client ist derselbe
(`mysql` `8.0.46` mit `--ssl-mode=DISABLED`), er lief hier aber mit
`--default-character-set=utf8mb4` statt mit der Vorgabe `cp850` der Windows-Konsole. Der Grund ist
M43‑3: Eine Messung über Leerzeichen und führende Nullen soll unter **derselben** Kollation laufen
wie die Spalte (`utf8mb4_general_ci`, M32). Die Kollationsprobe ist zur Kontrolle **beide** Male
gelaufen und liefert in beiden Fassungen dasselbe Ergebnis; sie steht in M43‑3.

### Die Kontrolle vorab — die Testkopie ist unverändert

| | `DATA_LENGTH` | `INDEX_LENGTH` | gegen §0 |
|---|---:|---:|---|
| `Message` | 740.851.712 | 2.157.330.432 | **byteidentisch** |
| `MessageAction` | 2.226.634.752 | 819.855.360 | **byteidentisch** |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 | **byteidentisch** |

Am Ende des Nachtrags erneut geprüft, erneut identisch. **Die Zahlen dieses Nachtrags stehen damit
auf demselben Boden wie M32 bis M41** — der Vorbehalt aus dem Auftrag („dann ist das der erste
Befund") kommt nicht zum Tragen. Eine zweite, unabhängige Kontrolle liefert der Vergleichsanker in
M42‑1: Der Wert mit 234.159 Treffern kostet dort **10.646,2 ms**, in M34 **10.617,6 ms** — 0,3 %
Abweichung bei einer Messung, die einen Tag und mehrere Sitzungen auseinanderliegt.

**Bezugsfenster unverändert:** Fenster A `2025-12-29` (ein Tag), Fenster B `2025-11-30` bis
`2025-12-30` (30 Tage). **Regel L7** ist über `NEXANS` und `IBIS` erfüllt (M42‑4).

---

# M42 — Was kostet die Verundung mehrerer Begriffe?

**Frage.** Was kostet jeder zusätzliche Begriff, steigt der Optimierer verlässlich über den
*seltensten* Begriff ein, und wie verhält sich die Verundung, wenn die Kandidatennachrichten viele
BAM-Werte tragen?

> **Das Ergebnis vorweg, in vier Sätzen.** Jeder zusätzliche Begriff kostet **0,09 ms** — die
> tragende Vermutung hält, und die Deckelung bei zwei Begriffen ist fachlich nicht nötig. Der
> Optimierer steigt **in jeder gemessenen Konstellation** über den seltensten Begriff ein,
> unabhängig von der Reihenfolge der Eingabe; die Verundung eines seltenen mit dem schlimmsten Wert
> der Runde kostet **0,672 ms** statt **10.646 ms**. Aber die Begründung der Vermutung ist falsch:
> Der Zusatzbegriff ist **kein** Nachschlag über den Primärschlüssel-Präfix mit `rows` 8, sondern
> ein Zugriff über `MessageBAM_BAMValueOnly` mit `rows` 1 — **solange der führende Begriff selektiv
> ist**. Ist er es nicht, kippt der Plan auf genau jenen `rows`-8-Zugriff, und auf einer Nachricht
> mit 2.499 BAM-Werten kostet der zweite Begriff dann den **Faktor 112**.

## M42‑0 Die Auswahl der Prüfwerte

**Die Werte müssen gemeinsam auf mindestens einer Nachricht vorkommen**, sonst misst die Abfrage
den billigen Fall „leeres Ergebnis". Die Auswahl geht deshalb von der Nachricht aus und nicht vom
Wert. Sie steigt über `Message` mit Zeitfenster ein und **nicht** über eine Gruppierung des ganzen
`MessageBAM` — die Warnung aus der Hauptrunde (679 s ohne Ergebnis) ist beachtet, und die Auswahl
kostet hier **83 ms**.

```sql
-- Ankernachricht: NEXANS-Wurzel in Fenster A mit genau neun BAM-Werten, kleinste MessageID
SELECT t.mid INTO @msg FROM (
  SELECT m.MessageID AS mid, COUNT(*) AS n
  FROM Message m
  JOIN Process p ON p.ProcessID = m.ProcessID
  JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID AND pm.MandantID = 'NEXANS'
  JOIN MessageBAM b ON b.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY m.MessageID
  HAVING n = 9
  ORDER BY mid
  LIMIT 1) t;

-- je Typ genau ein Wert, in eine eigene Sitzungsvariable
SELECT b.MessageBAMValue INTO @v9014 FROM MessageBAM b
WHERE b.MessageID = @msg AND b.MessageBAMType = 9014;   -- fuer jeden der neun Typen
```

**Alle vier Konstellationen K1 bis K4 stammen aus *einer* Nachricht.** Das war nicht geplant, es ist
das Ergebnis der Auswahl: Eine gewöhnliche `NEXANS`-Wurzel trägt neun Werte unter neun Typen, und
deren globale Trefferzahlen decken fünf Größenordnungen ab. `ORDER BY mid LIMIT 1` macht die Auswahl
reproduzierbar; jede Messsitzung leitet die Werte damit neu her, statt sie zu übertragen.

| Rolle im Test | Typ | Beschreibung | Länge | **globale Trefferzahl** |
|---|---:|---|---:|---:|
| selten **a** | 9022 | Gutschriftsanzeigen-Nummer_K_SAP | 12 | **1** |
| selten **b** | 9023 | Übertragungsnummer Gutschrift_K_SAP | 6 | **1** |
| fast selten | 9020 | Lieferschein, Entnahme, PUS_K_SAP | 8 | 4 |
| mittel | 9018 | Kundenmaterialnummer_K_SAP | 10 | 540 |
| häufig **a** | 9015 | Kundenwerk_K_SAP | 4 | 1.182 |
| häufig **b** | 9016 | Abladestelle_K_SAP | 8 | 1.182 |
| sehr häufig **a** | 9032 | Sendercode_K_SAP | 10 | 100.343 |
| sehr häufig **b** | 9033 | Empfaengercode_K_SAP | 25 | 124.793 |
| **der schlimmste** | 9014 | Lieferantennummer beim Kunden_K_SAP | 8 | **234.159** |

**Der schlimmste Wert der Hauptrunde steht auf dieser Nachricht.** Trefferzahl, Typ und Länge sind
identisch mit `@w_schlimm` aus M34 — und es ist **derselbe Wert** und nicht bloß ein gleich
häufiger: Unter Typ 9014 gibt es **genau einen** Wert mit 234.159 Treffern (eigens geprüft, nicht
geschlossen). Das ist Zufall der Auswahl und ein Glücksfall für die Vergleichbarkeit: K2 misst damit
die Verundung gegen genau den Wert, dessen Einzelkosten schon dastehen.

**Abweichung vom Auftrag, benannt statt stillgeschwiegen.** Für K2 verlangt er einen Wert mit
*fünfstelliger* Trefferzahl. Gemessen ist er mit einem **sechsstelligen** — 234.159 statt einer
Zahl zwischen 10.000 und 99.999. Der Fall wird dadurch härter und nicht weicher, und er bindet die
Messung an M34 und M35 an.

**Und eine zweite: K3 ist dreimal gemessen statt einmal.** Die vom Auftrag vorgeschlagenen Paare
(9000/9005, 9014/9015) haben eine Eigenschaft, die erst nach dem ersten Lauf auffiel — sie werden
**gemeinsam gesetzt** (M37) und schneiden deshalb einander nichts weg. Gemessen sind daher:

| | Paar | globale Trefferzahlen | Nachrichten nach der Verundung |
|---|---|---|---:|
| **K3a** | 9015 × 9016 | 1.182 × 1.182 | **1.182** — kein Schnitt |
| **K3b** | 9032 × 9033 | 100.343 × 124.793 | **100.343** — kein Schnitt |
| **K3c** | 9015 × 9014 | 1.182 × 234.159 | **1.182** — der häufige Begriff schneidet nichts weg |

Ohne K3c stünde in diesem Abschnitt nur der Fall, in dem die Verundung **nicht** hilft.

## M42‑1 Zwei Begriffe — die Grundform

```sql
SELECT m.MessageID, m.MessageStatus, m.MessageLastUpdate, m.ProcessID,
       m.Source, m.Target, m.SourceMessageID, m.TargetMessageID
FROM MessageBAM b1
JOIN MessageBAM b2 ON b2.MessageID = b1.MessageID
JOIN Message m     ON m.MessageID  = b1.MessageID
JOIN Process p     ON p.ProcessID  = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID AND pm.MandantID = 'NEXANS'
WHERE b1.MessageBAMValue = @a
  AND b2.MessageBAMValue = @b
GROUP BY m.MessageID
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 51;
```

> **Was „beide Reihenfolgen" hier heißt.** Nicht die Textreihenfolge der `AND`-Glieder — die ist dem
> Optimierer gleichgültig, und eine Messung darüber hätte nichts gesagt. Getauscht ist, **welcher
> Wert auf `b1` und welcher auf `b2` steht**, denn `b1` ist die Tabelle, die `STRAIGHT_JOIN`
> voranstellt. Das ist die Reihenfolge, in der der Nutzer seine Chips setzt.

### Die Vergleichsanker — jeder Begriff einzeln

Form aus M34, `NEXANS`, ohne Zeitfenster. Ohne diese Spalte ist nicht erkennbar, ob die Verundung
entlastet oder belastet:

| Typ | Treffer | Laufzeit |
|---:|---:|---:|
| 9022 | 1 | **0,611 ms** |
| 9023 | 1 | 0,612 ms |
| 9020 | 4 | 0,679 ms |
| 9018 | 540 | 6,349 ms |
| 9015 | 1.182 | 11,191 ms |
| 9016 | 1.182 | 11,117 ms |
| 9032 | 100.343 | 3.830,9 ms |
| 9033 | 124.793 | 4.906,5 ms |
| **9014** | **234.159** | **10.646,2 ms** |

*beste von 5 nach einem Aufwärmlauf; 9014 beste von 3.*

**Die Laufzeit folgt der Trefferzahl, nicht der Zahl der Begriffe** — dasselbe Gesetz wie in E6.

### Ergebnis — die Verundung

Alle Zeiten in Millisekunden, `NEXANS`, ohne Zeitfenster.

| | Treffer | **ohne `SJ`, a zuerst** | ohne `SJ`, b zuerst | mit `SJ`, a zuerst | mit `SJ`, b zuerst |
|---|---:|---:|---:|---:|---:|
| **K1** 9022 × 9023 (1 × 1) | 1 | **0,663** | 0,710 | 0,669 | 0,635 |
| **K2** 9022 × 9014 (1 × 234.159) | 1 | **0,672** | 0,710 | 0,681 | **155,563** ⚠ |
| **K3a** 9015 × 9016 (1.182 × 1.182) | 1.182 | 28,770 | **27,310** | 27,829 | 28,317 |
| **K3b** 9032 × 9033 (100.343 × 124.793) | 100.343 | 5.312,6 | **5.275,4** | 5.258,5 | 5.685,4 |
| **K3c** 9015 × 9014 (1.182 × 234.159) | 1.182 | 27,546 | **26,656** | 27,692 | **2.641,5** ⚠ |

*„a zuerst" heißt: der erstgenannte Typ steht auf `b1`. Beste von 5; K3b und die beiden
`SJ`-Ausreißer beste von 3.*

**Die drei Zahlen nebeneinander, wie der Auftrag es verlangt:**

| Konstellation | seltener Begriff allein | häufiger Begriff allein | **verundet** | gegenüber dem häufigen allein |
|---|---:|---:|---:|---|
| K2 | 0,611 ms | **10.646,2 ms** | **0,672 ms** | **Faktor 15.843 billiger** |
| K3c | 11,191 ms | **10.646,2 ms** | **26,656 ms** | Faktor 399 billiger |
| K3a | 11,117 ms | 11,191 ms | 27,310 ms | **Faktor 2,4 teurer** |
| K3b | 3.830,9 ms | 4.906,5 ms | 5.275,4 ms | **Faktor 1,08 teurer** |

## `EXPLAIN` — der eigentliche Befund

**Die führende Tabelle ist in *jeder* Fassung ohne `STRAIGHT_JOIN` die mit dem selteneren Wert** —
auch dann, wenn sie im Statement hinten steht. Der Optimierer tauscht `b1` und `b2`:

| Fassung | führende Tabelle | `key` | `rows` | zweiter Begriff: `key` / `key_len` / `ref` / `rows` |
|---|---|---|---:|---|
| K1, beide Reihenfolgen | die mit dem 1er-Wert | `MessageBAM_BAMValueOnly` | 1 | `MessageBAM_BAMValueOnly` / **428** / `const, MessageID` / **1** |
| K2, selten zuerst | `b1` (1er-Wert) | `MessageBAM_BAMValueOnly` | 1 | `MessageBAM_BAMValueOnly` / 428 / `const, MessageID` / **1** |
| **K2, häufig zuerst** | **`b2`** (1er-Wert) | `MessageBAM_BAMValueOnly` | 1 | dieselbe Gestalt — **der Plan ist identisch** |
| K2, häufig zuerst **mit `SJ`** | `b1` (234.159) | `MessageBAM_BAMValueOnly` | 443.830 | `range`, `NULL`, **`Using join buffer (flat, BNL join)`** |
| K3a / K3b / K3c ohne `SJ` | die mit dem selteneren Wert; bei K3a sind beide gleich häufig, dort führt die zuerst geschriebene | `MessageBAM_BAMValueOnly` | 1.182 / 177.506 / 1.182 | **`PRIMARY` / 146 / `MessageID` / `rows` 8** |

`Using index` steht in jeder Zeile; keine der Fassungen fasst die Tabelle an.

**Der `key_len` 428 ist der Punkt.** `MessageBAM_BAMValueOnly` ist als
`KEY (MessageBAMValue)` angelegt (M32), trägt in InnoDB aber den Primärschlüssel mit — der Zugriff
läuft deshalb als `ref (MessageBAMValue = const, MessageID = b1.MessageID)` über 282 + 146 Bytes
und liefert **genau eine** Zeile. Solange der führende Begriff selektiv ist, ist ein Zusatzbegriff
damit ein einzelner Indexsprung.

**Ist er es nicht, wählt der Optimierer `PRIMARY` mit `rows` 8** — und genau dieser Zugriff ist der,
den der Auftrag für den Normalfall gehalten hat. Er ist der Ausnahmefall, und M42‑1 K5 zeigt, was er
kostet, wenn die 8 nicht stimmt.

## M42‑1 K5 — die fette Nachricht

Prüfnachricht: das `NEXANS`-**Merge-Ergebnis** mit den meisten BAM-Werten in Fenster B — **3.409
Werte unter neun Typen**. Das ist zugleich das Maximum, das M41 für Merge-Ergebnisse misst; die
Auswahl hat unabhängig dieselbe Nachricht getroffen. Sie ist außerdem ein Ausreißer in der Gestalt:
M41 misst, dass 1.625 von 1.669 Merge-Ergebnissen **genau sechs** Typen tragen, und zwar die
`_L_SAP`-Typen. Diese hier trägt neun, und keiner davon ist ein `_L_SAP`-Typ.

| Typ | Werte auf dieser Nachricht | Länge des Prüfwerts | globale Trefferzahl |
|---:|---:|---:|---:|
| 9020 | 23 | 10 | **1** |
| 9024 | 23 | 10 | **1** |
| 9032 / 9033 | je 1 | 15 | 444 |
| 9019 | 1.679 | 9 | **654** |
| 9018 | 1.679 | 13 | **719** |
| 9015 / 9016 | je 1 | 3 | 4.010 |
| 9014 | 1 | 4 | 5.870 |

**Die Zahl, auf die es ankommt:** Der 9018-Prüfwert steht auf **654 verschiedenen Nachrichten**, und
diese 654 Nachrichten tragen zusammen **1.634.605 BAM-Werte** — **2.499 je Nachricht**. Das ist die
Menge, die ein Zusatzbegriff durchsehen muss, wenn der Plan auf `PRIMARY` kippt.

| | Treffer | ohne `SJ`, a zuerst | ohne `SJ`, b zuerst | mit `SJ`, a zuerst | mit `SJ`, b zuerst |
|---|---:|---:|---:|---:|---:|
| **K5a** 9020 × 9024 (1 × 1) | 1 | **0,677** | 0,695 | 0,661 | 0,689 |
| **K5b** 9020 × 9018 (1 × 719) | 1 | **0,692** | 0,699 | 0,656 | 1,184 |
| **K5c** 9018 × 9019 (719 × 654) | 654 | 940,8 | **926,7** | 1.278,5 | 934,2 |
| *Anker 9020 / 9024, je einzeln* | 1 / 1 | *0,632 / 0,593* | — | — | — |
| *Anker 9018 / 9019, je einzeln* | 719 / 654 | ***8,289 / 7,797*** | — | — | — |

**K5c kostet 926,7 ms, wo jeder der beiden Begriffe allein 8 ms kostet — Faktor 112.** Der `EXPLAIN`
nennt den Grund ohne Umschweife: Der zweite Begriff läuft über `PRIMARY` mit **`rows` 8**, und die
tatsächliche Zahl ist 2.499. Die Schätzung liegt um den Faktor 312 daneben, und die Laufzeit folgt
der Wirklichkeit: 1.634.605 Indexeinträge in 926,7 ms sind 1,76 Millionen Zeilen je Sekunde und
damit genau die Größenordnung, die M33 für index-nahe Durchläufe misst.

**K5a und K5b kosten nichts extra.** Dort ist der führende Begriff ein Einzeltreffer, der Plan
bleibt bei `MessageBAM_BAMValueOnly` mit `rows` 1, und die Fettleibigkeit der Nachricht ist
folgenlos. **Nicht die Nachricht ist teuer, sondern die Kombination aus einem unselektiven
Einstieg und einer fetten Kandidatenmenge.**

## M42‑2 Drei und fünf Begriffe

Dieselbe Menge, wachsend: 9022 (1) → + 9014 (234.159) → + 9018 (540) → + 9016 (1.182) →
+ 9032 (100.343). Nach jeder Erweiterung bleibt genau **eine** Nachricht übrig.

| Zahl der Begriffe | ohne `STRAIGHT_JOIN` | mit `STRAIGHT_JOIN` |
|---:|---:|---:|
| 1 (Anker 9022) | 0,611 ms | — |
| **2** | **0,666 ms** | 0,645 ms |
| **3** | **0,759 ms** | 0,730 ms |
| **5** | **0,947 ms** | 0,876 ms |
| 5, **seltener Begriff zuletzt** | **1,031 ms** | **1.128,3 ms** ⚠ |

*beste von 5; die letzte Zeile mit `SJ` beste von 3.*

**Von zwei auf fünf Begriffe kostet 0,281 ms — 0,094 ms je zusätzlichem Begriff.** Der `EXPLAIN`
zeigt fünf `ref`-Zugriffe mit `rows` 1 hintereinander, alle `Using index`, und der Optimierer ordnet
sie selbst um: Steht der seltenste Wert im Statement an **fünfter** Stelle (`b5`), führt der Plan
trotzdem `b5` an, und die übrigen vier hängen als `ref (const, b5.MessageID)` daran. Der
Laufzeitunterschied — 1,031 gegen 0,947 ms — ist die Umsortierung selbst und nicht ein anderer Plan.

## M42‑3 Die andere Bauform — Verundung ohne Selbstjoin

```sql
SELECT m.MessageID, m.MessageStatus, m.MessageLastUpdate, m.ProcessID,
       m.Source, m.Target, m.SourceMessageID, m.TargetMessageID
FROM (SELECT b.MessageID FROM MessageBAM b
      WHERE b.MessageBAMValue IN (@a, @b, @c, @d, @e)
      GROUP BY b.MessageID
      HAVING COUNT(DISTINCT b.MessageBAMValue) = 5) t
JOIN Message m ON m.MessageID = t.MessageID
JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID AND pm.MandantID = 'NEXANS'
GROUP BY m.MessageID
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 51;
```

`EXPLAIN`: `<derived2>` als `ALL`, darin `MessageBAM` als `range` über
`MessageBAM_BAMValueOnly` mit `Using index`. Bei fünf Begriffen schätzt der Optimierer die
abgeleitete Tabelle auf **623.059** Zeilen — gezählt sind es 336.225, die Summe der Trefferzahlen
aller fünf Begriffe. Der vermutete Nachteil ist damit im Plan sichtbar, bevor die Uhr läuft.

Gemessen ist die Bauform nicht nur bei K1 und K4, sondern bei allen Konstellationen — sonst stünde
die Empfehlung auf zwei Punkten:

| Konstellation | Grundform (Selbstjoin) | **Bauform ohne Selbstjoin** | |
|---|---:|---:|---|
| K1 (1 × 1) | 0,663 ms | 0,858 ms | 1,3× langsamer |
| **K2** (1 × 234.159) | 0,672 ms | **586,4 ms** | **873× langsamer** |
| K3a (1.182 × 1.182) | 27,310 ms | **15,962 ms** | 1,7× schneller |
| K3b (100.343 × 124.793) | 5.275,4 ms | **4.348,5 ms** | 1,2× schneller |
| K3c (1.182 × 234.159) | 26,656 ms | 612,4 ms | 23× langsamer |
| **K4, fünf Begriffe** | 0,947 ms | **903,4 ms** | **954× langsamer** |
| **K5c** (fette Nachrichten) | 926,7 ms | **10,654 ms** | **87× schneller** |
| `IBIS`, fünf Begriffe | 0,992 ms | 0,878 ms | 1,1× schneller |

**Die beiden Bauformen sind gegenläufig, nicht gestuft.** Die Grundform bezahlt
*Trefferzahl des seltensten Begriffs × BAM-Werte je Kandidatennachricht*; die andere bezahlt die
*Summe der Trefferzahlen aller Begriffe*. Deshalb gewinnt die eine genau dort, wo die andere
verliert.

## M42‑4 Der kleine Mandant (Regel L7)

`IBIS`, Fenster B, dieselbe Auswahlmethodik (1,588 s).

**Zuerst die Zahl, die den ganzen Abschnitt einordnet:** Über die **9.017** verschiedenen Werte, die
auf `IBIS`-Nachrichten in Fenster B stehen, ist die **größte globale Trefferzahl 216** und der
Schnitt **3,40**. Der schlimmste Fall der Hauptrunde — 234.159 Treffer — hat bei `IBIS` kein
Gegenstück. Die Frage „steigt der Optimierer über den seltensten Begriff ein" ist dort deshalb keine
Leistungsfrage.

Die Prüfnachricht trägt fünf Werte unter drei Typen (0, 1, 2) mit den globalen Trefferzahlen
**1, 5, 4, 5, 4**. **Abweichung, benannt:** Der Auftrag verlangt für K1 zwei Werte mit Trefferzahl
**1**; auf der gewählten Nachricht hat genau **einer** die Trefferzahl 1, der zweitseltenste hat 4.
Gemessen ist deshalb 1 × 4. Für K4 verlangt er gemischte Häufigkeit — bei `IBIS` gibt es sie nicht,
alle fünf Werte liegen zwischen 1 und 5 Treffern.

| | ohne `SJ`, selten zuerst | ohne `SJ`, umgekehrt | mit `SJ`, selten zuerst | mit `SJ`, umgekehrt |
|---|---:|---:|---:|---:|
| **K1** (1 × 4) | **0,713** | 0,686 | 0,694 | 0,681 |

| Zahl der Begriffe | ohne `SJ` | mit `SJ` |
|---:|---:|---:|
| 1 (Anker) | 0,606 / 0,655 / 0,647 ms | — |
| 2 | **0,675 ms** | — |
| 3 | **0,753 ms** | — |
| 5 | **0,992 ms** | 0,986 ms |
| 5, Bauform ohne Selbstjoin | 0,878 ms | — |

**Die Reihe ist bei `IBIS` dieselbe wie bei `NEXANS`** — 0,675 → 0,753 → 0,992 gegen 0,666 → 0,759
→ 0,947. Die Verundung verhält sich beim kleinen Mandanten nicht anders; sie ist nur nirgends
gefährdet.

**Die Annahme des Auftrags über `IBIS` trifft nicht zu.** Er rechnet damit, dass dort keine
Nachricht fünf BAM-Werte trägt, weil M40 nur vier konfigurierte Typen ausweist. Gemessen trägt
`IBIS` in Fenster B Nachrichten mit **bis zu 158 Werten**: 3.048 Nachrichten mit einem Typ, 121 mit
zweien, 674 mit dreien. Die Zahl der **Typen** ist klein, die Zahl der **Werte** ist es nicht — der
Rückschluss von der Konfiguration auf den Umfang trägt hier so wenig wie in M40.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **Jeder zusätzliche Begriff kostet im einstelligen Millisekundenbereich** | **ja, und deutlich darunter** — 0,094 ms | **Die Vermutung trägt.** Eine Deckelung ist fachlich nicht nötig; die technische Grenze von acht bis zehn Begriffen bleibt reines Schutzgeländer. **Aber nur, solange der erste Begriff selektiv ist** — siehe K5 |
| **Die Verundung ist schneller als der häufigere Begriff allein** (K2) | **ja, um den Faktor 15.843** | Die Kombination entlastet tatsächlich, und zwar dramatisch. Sie bleibt trotzdem kein Ersatz für das Zeitfenster, weil die Suche beim **ersten** Begriff einmal einbegriffig läuft — und dort steht der Wert mit 10,6 s |
| Die Verundung ist so teuer wie der häufigere Begriff allein (K2) | **nein** | — Der Optimierer steigt über den **richtigen** Begriff ein. Der unangenehmste Ausgang der Messung ist nicht eingetreten; der Endpunkt braucht **keine** eigene Reihenfolgeentscheidung und keine vorab unbekannte Trefferzahl |
| Die beiden Reihenfolgen der `WHERE`-Bedingungen unterscheiden sich | **nein — ohne `STRAIGHT_JOIN`.** Mit ihm **ja, um den Faktor 219 bis 1.094** | **Die Leistung hängt nicht an der Eingabereihenfolge des Nutzers** — solange man den Optimierer arbeiten lässt. Siehe den Absatz unten: Die vorgesehene Abhilfe ist hier die Ursache |
| **K3 (häufig × häufig) bleibt unter der Zeitgrenze** | **ja** — 5,275 s | Der Bösfall der Verundung ist beherrschbar, aber er hat nur **Faktor 1,9 Reserve** zur 10-Sekunden-Grenze des Lese-Pools. Auf einer belasteten Produktionsdatenbank ist das dieselbe Lage, die M35 für das Jahresfenster beschreibt |
| K3 reißt die Zeitgrenze | **nein** | — Eine Prüfung, ob 30 Tage dafür reichen, ist nicht nötig geworden. **Ungemessen bleibt**, ob das Zeitfenster K3b ebenso senkt wie in M35 — alle M42-Zahlen sind **ohne** Fenster |
| **K5 (fette Nachricht) kostet deutlich mehr je Zusatzbegriff als K1** | **ja — Faktor 112** | **Die `rows`-Schätzung von 8 trägt nicht durchgängig.** 926,7 ms gegen 8,3 ms. Die Größenordnung steht damit da, bevor sich jemand auf „ein Zusatzbegriff ist umsonst" verlässt |
| **Fünf Begriffe kosten nicht wesentlich mehr als zwei** | **ja** — 0,947 gegen 0,666 ms | Die Form skaliert, und die Bedienung mit `+` ist ohne Zusatzbau tragfähig. Bestätigt bei `IBIS` mit denselben Zahlen |
| Die Bauform aus M42‑3 ist der Grundform ebenbürtig oder überlegen | **teilweise — in vier von acht gemessenen Fällen** (K3a, K3b, K5c, `IBIS`) | Sie ist **nicht** pauschal zu bevorzugen und **nicht** pauschal zu verwerfen. Wo sie gewinnt, gewinnt sie deutlich (K5c: Faktor 87); wo sie verliert, verliert sie deutlicher (K4: Faktor 954) |
| **Die Bauform aus M42‑3 fällt bei einem häufigen Begriff ab** | **ja, sehr** | Erwartet, und mit Zahl statt mit Vermutung: 903,4 ms gegen 0,947 ms bei fünf Begriffen. Der Selbstjoin bleibt die Bauform für den Normalfall |

**Wo die vorformulierte Zeile nicht passt — der Zusatzbegriff läuft nicht über den
Primärschlüssel-Präfix.** Die tragende Vermutung des Auftrags lautet, jeder weitere Begriff sei ein
Nachschlag über `(MessageID, MessageBAMType, MessageBAMValue)` mit `rows` 8 und `Using index`, wie
M34 ihn ausweist. Gemessen läuft er über **`MessageBAM_BAMValueOnly` mit `key_len` 428** und
`ref (const, b1.MessageID)` — also über *Wert **und** MessageID* — und liefert `rows` **1**. Der
Schluss des Auftrags ist damit richtig und seine Begründung falsch, und der Unterschied ist keine
Feinheit: Der `rows`-8-Zugriff **existiert**, er tritt nur unter der umgekehrten Bedingung auf. Er
erscheint, sobald der führende Begriff nicht mehr selektiv ist — und dann ist die 8 auch noch falsch
(K5: tatsächlich 2.499).

**Und ein zweiter Befund ohne vorformulierte Zeile: `STRAIGHT_JOIN` erzeugt hier das Problem,
gegen das er vorgesehen war.** Der Auftrag hält ihn für die Abhilfe, falls die Reihenfolge der
Eingabe die Leistung bestimmt. Gemessen ist das Gegenteil:

| | ohne `STRAIGHT_JOIN` | mit `STRAIGHT_JOIN` | Faktor |
|---|---:|---:|---:|
| K2, häufiger Begriff zuerst | **0,710 ms** | 155,563 ms | **219** |
| K3c, häufiger Begriff zuerst | **26,656 ms** | 2.641,5 ms | **99** |
| fünf Begriffe, seltener zuletzt | **1,031 ms** | 1.128,3 ms | **1.094** |

Ohne den Zusatz ist der Plan in **beiden** Reihenfolgen identisch; der Optimierer tauscht `b1` und
`b2` und steigt über den selteneren Wert ein. Mit dem Zusatz ist die Reihenfolge bindend, und die
Eingabereihenfolge des Nutzers wird zur Leistungsfrage. **Für die Bauentscheidung heißt das: kein
`STRAIGHT_JOIN` in der Verundung** — es sei denn, jemand bestimmt die Reihenfolge vorher, und dafür
bräuchte er die Trefferzahl, die er nicht kennt. Dieselbe Lage wie in M34, nur mit umgekehrtem
Vorzeichen: Dort half der Zusatz bei `enthält` um den Faktor 4,3.

**Ein dritter: die Verundung entlastet nicht immer, sie kann auch belasten.** Die vorformulierten
Zeilen kennen nur „billiger" und „so teuer wie". Gemessen gibt es einen dritten Ausgang, und er ist
der Normalfall bei zwei Begriffen ähnlicher Häufigkeit: K3a kostet **27,310 ms**, während jeder
Begriff allein **11 ms** kostet — Faktor 2,4 **teurer**. Bei K3b sind es 5.275 ms gegen 4.906 ms.
Der Grund steht in der Trefferspalte: 9015 und 9016 stehen auf **denselben** 1.182 Nachrichten,
9032 und 9033 auf denselben 100.343. Die Verundung schneidet dort nichts weg und bezahlt trotzdem
den zweiten Zugriff. **Zwei Typen, die gemeinsam gesetzt werden, sind als Suchpaar wertlos** — und
M37 sagt, welche das sind.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Laufzeit der Verundung über **acht** Konstellationen bei `NEXANS` und **zwei** bei
> `IBIS`, in zwei Bauformen, auf einer ruhenden Testkopie, **ohne Zeitfenster**. Die Prüfwerte
> stammen aus **drei** Nachrichten (n = 3): einer gewöhnlichen `NEXANS`-Wurzel, dem fettesten
> `NEXANS`-Merge-Ergebnis aus Fenster B und einer `IBIS`-Nachricht mit drei Typen.
>
> *Behauptet wird:* dass die Verundung mehrerer Begriffe billig ist und keine Deckelung braucht.
>
> **Die Lücke, und sie ist dreifach.** Erstens trifft die Auswahl ausschließlich Werte, die
> **gemeinsam vorkommen**. Über Kombinationen, die es im Bestand **nicht** gibt, sagt keine dieser
> Zahlen etwas — und genau die tippt ein Nutzer, der sich vertut. Der Plan legt nahe, dass dieser
> Fall billig ist (der Join bricht ab), aber *nahelegen* ist nicht *messen*. Zweitens ist „billig"
> nur wahr, solange **ein** Begriff selektiv ist: K5c widerlegt den Satz mit dem Faktor 112, und
> die Bedingung dafür — 2.499 BAM-Werte je Kandidatennachricht — ist bei `ZAST` (441 je Nachricht,
> M40) und bei Merge-Ergebnissen (99. Perzentil 1.345, M41) keine Ausnahme. Drittens ist die Reihe
> bei **fünf** Begriffen zu Ende gemessen; acht oder zehn sind die Zahlen, die das Schutzgeländer
> tragen soll, und sie stehen hier nicht. Was trägt: *Bei einem selektiven ersten Begriff kostet
> jeder weitere 0,094 ms.* Was **nicht** gemessen ist: *derselbe Satz ohne diese Bedingung.*

---

# M43 — Womit wird normalisiert?

**Frage.** M38 misst den **Anteil** der Werte mit führender Null je Typ. Für eine Normalisierung
fehlen drei Dinge: die **Sollänge** je Typ, die Antwort darauf, ob dieselbe Nummer im Bestand
**beide** Schreibweisen trägt, und die Unterscheidung zwischen **führenden** und **folgenden**
Leerzeichen.

> **Das Ergebnis vorweg, in vier Sätzen.** Die Sollänge existiert: Bei fünf der sechs Typen mit
> durchgängig führender Null gibt es über den **Bestand** genau **eine** Länge, und `LPAD` trifft
> den Originalwert. Beim sechsten — Typ **2001** — trägt über den Bestand nur **67,21 %** der Werte
> eine führende Null statt der 100 % aus Fenster B; die aus einem Monat abgeleitete Kuratierung
> wäre dort falsch gewesen, und das ist gemessen und nicht befürchtet. Die Randleerzeichen bei 9018
> sind zu **24,83 %** folgende und damit unter `utf8mb4_general_ci` für den `=`-Vergleich
> **folgenlos** — aber **1,05 %** sind führende, und die sind es nicht. Und der Bestand selbst ist
> uneinheitlich: Bei Typ **9017** tragen **52,98 %** der numerischen Werte eine zweite Schreibweise
> desselben Kerns, bis zu **fünf** Varianten je Nummer.

## M43‑1 Die Längenverteilung je Typ, getrennt nach führender Null

Über Fenster B, je `MessageBAMType`, in der Fassung des Auftrags (4,625 s). Die vollständige
Rohtabelle steht nicht in diesem Dokument; verdichtet wurde mit einem zweiten Statement über
`ROW_NUMBER() OVER (PARTITION BY typ, fuehrende_null ORDER BY zeilen DESC, laenge)` (4,633 s), damit
die Verdichtung nachvollziehbar ist und nicht von Hand entsteht.

```sql
SELECT b.MessageBAMType,
       LEFT(b.MessageBAMValue, 1) = '0' AS fuehrende_null,
       CHAR_LENGTH(b.MessageBAMValue) AS laenge,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT b.MessageBAMValue) AS werte
FROM Message m JOIN MessageBAM b ON b.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00' AND m.MessageLastUpdate < '2025-12-30 00:00:00'
GROUP BY b.MessageBAMType, fuehrende_null, laenge
ORDER BY b.MessageBAMType, fuehrende_null, laenge;
```

### Ergebnis — Fenster B, die Typen, an denen etwas hängt

„Längen" ist die Zahl **verschiedener** Längen, „häufigste" die häufigste Länge mit ihrem Anteil
an der jeweiligen Gruppe. Die Zeilensummen je Typ stimmen mit M38 überein.

| Typ | Beschreibung | **ohne** führende Null | | | **mit** führender Null | | |
|---:|---|---:|---:|---|---:|---:|---|
| | | Zeilen | Längen | häufigste | Zeilen | Längen | häufigste |
| **2001** | VendorReference | — | — | — | 1.253 | **1** | **14 (100 %)** |
| **2002** | InvoiceNumber VTG | — | — | — | 92 | **1** | **10 (100 %)** |
| **9011** | Anlieferungs-Nr. ae_L_SAP | — | — | — | 58 | **1** | **10 (100 %)** |
| **9012** | Charge_L_SAP | — | — | — | 767 | **1** | **10 (100 %)** |
| **9024** | Rechnungsnummer_K_SAP | — | — | — | 988 | **1** | **10 (100 %)** |
| **9036** | Lagerort Kunde_L_SAP | 9 | 2 | 3 (66,67 %) | 944 | **1** | **4 (100 %)** |
| 9021 | Transportnummer_K_SAP | 173 | 1 | 10 (100 %) | 2.281 | **1** | **10 (100 %)** |
| **9006** | Lieferschein-Nr._L_SAP *(kuratiert, Position 1)* | 4.666 | 2 | 8 (96,70 %) | 2.391 | 2 | 8 (92,14 %) |
| **9001** | Abrufnummer_L_SAP *(kuratiert, Position 2)* | 24.136 | 4 | 3 (72,58 %) | — | — | — |
| 9007 | Transport-Nummer_L_SAP | 901 | 6 | 8 (82,46 %) | 826 | 2 | 8 (90,07 %) |
| 9009 | Beleg-Nr. GS_L_SAP | 505 | 1 | 10 (100 %) | 234 | 1 | 10 (100 %) |
| 9002 | Lieferplannummer_L_SAP | 41.333 | 1 | 10 (100 %) | — | — | — |
| 9020 | Lieferschein, Entnahme, PUS_K_SAP | 68.911 | 12 | 8 (99,02 %) | 7.601 | 3 | 10 (90,91 %) |
| **9018** | Kundenmaterialnummer_K_SAP | 141.345 | **16** | 13 (**44,49 %**) | 1.868 | 8 | 8 (31,96 %) |
| 9019 | Bestellnummer vom Kunden_K_SAP | 109.316 | 9 | 9 (53,56 %) | 1.391 | 6 | 12 (42,85 %) |
| 9017 | (JIT-) Abrufnummer_K_SAP | 28.001 | **16** | 3 (**56,60 %**) | 11.411 | 8 | 9 (72,59 %) |
| 9016 | Abladestelle_K_SAP | 27.167 | 11 | 8 (**39,66 %**) | 586 | 5 | 3 (49,83 %) |
| 9015 | Kundenwerk_K_SAP | 24.080 | 8 | 4 (50,22 %) | 5.579 | 7 | 3 (97,69 %) |
| 9014 | Lieferantennummer beim Kunden_K_SAP | 27.444 | 9 | 8 (71,86 %) | 1.936 | 5 | 10 (61,52 %) |
| **9003** | Material-Nr. beim Lieferanten_L_SAP | 53.883 | **33** | 9 (**19,09 %**) | 1.950 | 19 | 18 (36,72 %) |
| 9038 | Packmittelnummer Lieferant_L_SAP | 2.026 | 18 | 8 (**28,73 %**) | 613 | 10 | 9 (34,91 %) |
| 3 | Rechnungsnummer | 128.716 | 2 | 8 (97,84 %) | — | — | — |
| 0 | Bestellnummer | 9.459 | 3 | 10 (69,77 %) | 119 | 3 | 7 (45,38 %) |
| 1 | Auftragsnummer | 9.529 | 5 | 7 (99,28 %) | — | — | — |
| 2 | Lieferscheinnummer | 3.404 | 4 | 7 (99,27 %) | — | — | — |

**Die Antwort ist nicht eine, sondern zwei.** Bei den sechs Typen mit durchgängig führender Null
gibt es in der Gruppe **mit** führender Null **genau eine** Länge — dort existiert die Sollänge und
ist nicht einmal eine Wahl (bei 9036 stehen daneben neun Zeilen ohne Null). Bei den
tragenden Suchtypen von `NEXANS` streuen die Längen: 9018 über 16 Längen mit 44,49 % auf der
häufigsten, 9003 über 33 Längen mit 19,09 %. Dort gibt es keine Sollänge, und `LPAD` wäre eine
Rechenvorschrift ohne Bezugsgröße.

### Dieselbe Frage über den **Bestand** — und der Befund, der die Kuratierung trifft

**Ohne Zeitfenster, und das ist begründungspflichtig (Regel L9).** Eine Kuratierung, die aus einem
Monat abgeleitet wird, gilt anschließend für 22 Monate. Ein Fenster kann die Frage „ist die Länge
dieses Typs stabil" grundsätzlich nicht beantworten, weil es genau die Zeiträume ausblendet, in
denen sie sich geändert haben könnte. Die Erhebung steigt über `MessageBAM_BAMValue` ein
(`(MessageBAMType, MessageBAMValue)`, M32), fasst nur sechs Typen an und kostet **0,210 s** — sie
ist damit die mit Abstand billigste fensterlose Erhebung dieser Runde (M40‑3: 21,698 s, M33: bis zu
198,6 s kalt).

| Typ | Beschreibung | ohne führende Null | | | mit führender Null | | | **Anteil f. Null** |
|---:|---|---:|---:|---|---:|---:|---|---:|
| | | Zeilen | Längen | häufigste | Zeilen | Längen | häufigste | |
| **2001** | VendorReference | **3.670** | **3** | 10 (63,46 %) | 7.521 | 1 | 14 (100 %) | **67,21 %** |
| 2002 | InvoiceNumber VTG | — | — | — | 456 | 1 | 10 (100 %) | **100 %** |
| 9011 | Anlieferungs-Nr. ae_L_SAP | — | — | — | 1.412 | 1 | 10 (100 %) | **100 %** |
| 9012 | Charge_L_SAP | — | — | — | 17.934 | 1 | 10 (100 %) | **100 %** |
| 9024 | Rechnungsnummer_K_SAP | — | — | — | 25.359 | 1 | 10 (100 %) | **100 %** |
| 9036 | Lagerort Kunde_L_SAP | 243 | 2 | 3 (72,84 %) | 22.604 | 1 | 4 (100 %) | 98,94 % |

**Fünf von sechs halten. Der sechste nicht.** Typ **2001** (`VendorReference`, `SUTTONS`) trägt in
Fenster B auf 100 % seiner 1.253 Zeilen eine führende Null. Über den Bestand sind es **7.521 von
11.191 — 67,21 %**, und die restlichen 3.670 Zeilen haben nicht nur keine führende Null, sondern
auch nicht die Länge 14: Sie verteilen sich auf drei Längen zwischen 7 und 14, häufigste 10 mit
63,46 %. Eine Normalisierung, die aus Fenster B „Typ 2001 wird auf 14 Stellen aufgefüllt" ableitet,
hätte für ein Drittel des Bestands eine Länge erzwungen, die es dort nie gab.

## M43‑2 Trägt dieselbe Nummer beide Schreibweisen?

Die entscheidende Frage. Fenster B, nur rein numerische Werte, gruppiert über
`TRIM(LEADING '0' FROM …)` (4,834 s).

> **Eine Korrektur am Statement des Auftrags, damit es läuft.** Die dort abgedruckte Fassung
> selektiert und gruppiert im äußeren Teil über `b.MessageBAMType`, obwohl `b` außerhalb der
> abgeleiteten Tabelle nicht sichtbar ist. Gemessen ist dieselbe Abfrage mit `t.MessageBAMType`;
> an der Frage ändert das nichts.

Der Nenner steht daneben, weil „4.213 Gruppen" ohne ihn keine Größe ist (eigenes Statement, 4,944 s):

| Typ | Beschreibung | Kerne mit mehreren Schreibweisen | betroffene Werte | **meiste Varianten** | numerische Werte gesamt | **Anteil** |
|---:|---|---:|---:|---:|---:|---:|
| **9020** | Lieferschein, Entnahme, PUS_K_SAP | 4.213 | 8.426 | 2 | 41.917 | **20,10 %** |
| **9017** | (JIT-) Abrufnummer_K_SAP | 1.081 | 2.870 | **5** | 5.417 | **52,98 %** |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 269 | 538 | 2 | 6.096 | 8,83 % |
| 9015 | Kundenwerk_K_SAP | 10 | 20 | 2 | 147 | 13,61 % |
| 9016 | Abladestelle_K_SAP | 6 | 14 | 3 | 132 | 10,61 % |
| 9000 | Abladestelle_L_SAP | 1 | 2 | 2 | 16 | 12,50 % |
| 9014 | Lieferantennummer beim Kunden_K_SAP | 1 | 2 | 2 | 69 | 2,90 % |
| 0 | Bestellnummer | 32 | 64 | 2 | 3.112 | 2,06 % |
| 9018 | Kundenmaterialnummer_K_SAP | 2 | 4 | 2 | 1.561 | 0,26 % |

**Neun Typen, und bei zweien ist es kein Randfall.** Bei 9017 trägt **jede zweite** numerische
Nummer eine zweite Schreibweise, und eine Nummer kommt in **fünf** verschiedenen Schreibweisen vor
— also mit null, einer, zwei, drei und vier führenden Nullen. Bei 9020 ist jede fünfte betroffen,
dort durchgängig mit genau zwei Varianten.

**Die Gegenprobe zur Vollständigkeit:** Keiner der sechs Typen mit durchgängig führender Null
(2001, 2002, 9011, 9012, 9024, 9036) steht in dieser Tabelle. Dort ist der Bestand einheitlich —
und genau dort wirkt eine Normalisierung sauber.

## M43‑3 Führende oder folgende Leerzeichen? — der Kollationsbefund

`utf8mb4_general_ci` ist eine **PAD SPACE**-Kollation. Unter ihr sind **folgende** Leerzeichen für
den `=`-Vergleich unsichtbar, **führende** nicht. M38 misst beide zusammen und kann deshalb nicht
sagen, ob die 25,88 % bei Typ 9018 ein Problem sind (3,250 s):

| Typ | Beschreibung | Zeilen | **führend** | **folgend** | beides |
|---:|---|---:|---:|---:|---:|
| **9018** | Kundenmaterialnummer_K_SAP | 143.213 | **1,05 %** | **24,83 %** | 0,00 % |
| 9033 | Empfaengercode_K_SAP | 14.864 | 0,00 % | 2,15 % | 0,00 % |
| 9032 | Sendercode_K_SAP | 14.864 | 0,00 % | 2,15 % | 0,00 % |
| 9020 | Lieferschein, Entnahme, PUS_K_SAP | 76.512 | 0,00 % | 0,01 % | 0,00 % |

1,05 + 24,83 = 25,88 — **exakt** der Wert, den M38 für 9018 misst. Die Spalte „beides" rundet bei
allen vier auf 0,00 %; führende und folgende Leerzeichen treten also praktisch nie am selben Wert
auf. Außer diesen vier trägt in Fenster B **kein** Typ Randleerzeichen — die Abfrage filtert über
`HAVING fuehrend_pz > 0 OR folgend_pz > 0` und liefert genau diese vier Zeilen.

### Der Beleg statt der Behauptung

```sql
SELECT 'a ' = 'a', 'a' LIKE 'a ', 'a ' LIKE 'a%';
```

| Ausdruck | Ergebnis | Bedeutung |
|---|---:|---|
| `'a ' = 'a'` | **1** | PAD SPACE: folgende Leerzeichen sind beim `=`-Vergleich unsichtbar |
| **`'a' LIKE 'a '`** | **0** | **`LIKE` folgt der PAD-SPACE-Regel nicht** |
| `'a ' LIKE 'a%'` | 1 | Präfixmuster greift, aber über den ganzen Rest |
| `' a' = 'a'` | **0** | **führende** Leerzeichen sind **nicht** gepolstert |

Alle vier zusätzlich mit erzwungener Kollation gemessen
(`_utf8mb4'…' COLLATE utf8mb4_general_ci`) — **identische Ergebnisse**, also unabhängig von der
Zeichensatzeinstellung des Clients. Das war der Grund für die Doppelmessung im Rahmen oben.

### Die Gegenprobe an echten Werten

Zwei Werte des Typs 9018 aus dem Bestand, einer mit folgendem und einer mit führendem Leerzeichen,
je einmal roh und einmal getrimmt gesucht:

| Fall | Länge original | Länge getrimmt | Treffer **mit** Leerzeichen | Treffer **getrimmt** |
|---|---:|---:|---:|---:|
| **folgendes** Leerzeichen | 22 | 10 | **12** | **12** |
| **führendes** Leerzeichen | 12 | 10 | **3** | **23** |

**Damit ist es keine Kollationstheorie mehr.** Beim folgenden Leerzeichen findet die exakte Suche
in beiden Fassungen dieselben zwölf Zeilen — die Kollation erledigt es. Beim führenden findet der
Nutzer, der ohne Leerzeichen tippt, **23** Zeilen und erreicht die **3** mit Leerzeichen nicht.

## M43‑4 Die Gegenprobe — findet das Auffüllen wirklich etwas?

Je Typ der kleinste Wert **mit** führender Null aus dem Bestand; davon die führenden Nullen
entfernt; dann beide Fassungen gezählt. Die Sollänge stammt aus M43‑1. Gezählt wird über
`MessageBAM` **ohne** Typeinschränkung, weil die Suche typlos ist (M32, M36) — das ist die
Trefferzahl, die der Nutzer wirklich bekommt.

| Typ | Sollänge | Länge original | Länge ohne Null | `LPAD` trifft das Original | **Treffer roh** | **Treffer aufgefüllt** |
|---:|---:|---:|---:|---|---:|---:|
| **2001** | 14 | 14 | 12 | ja | **0** | **3** |
| **2002** | 10 | 10 | 9 | ja | **0** | **1** |
| **9011** | 10 | 10 | 8 | ja | **0** | **1** |
| **9012** | 10 | 10 | 8 | ja | **0** | **1** |
| **9024** | 10 | 10 | 8 | ja | **0** | **1** |
| 9021 | 10 | 10 | 7 | ja | **0** | **1** |
| **9036** | 4 | 4 | 1 | ja | **2.371** | 800 |
| **9006** | 8 | 8 | 3 | ja | **1.642** | 4 |

*9021 und 9006 sind über den Auftrag hinaus mitgemessen: 9021 trägt auf 92,95 % führende Nullen,
9006 ist die kuratierte Lieferschein-Nr. mit 33,88 % (M38).*

**`LPAD` auf die Sollänge trifft in allen acht Fällen exakt den Originalwert.** Die Rechenvorschrift
stimmt also. Was sie leistet, ist je Typ verschieden:

- **Bei sechs von acht Typen findet die rohe Fassung *nichts*** — 0 Treffer gegen 1 bis 3. Der
  Nutzer, der die führende Null nicht mittippt, ginge ohne Normalisierung leer aus, und zwar nicht
  „mit weniger Treffern", sondern mit **keinem**.
- **Bei 9036 und 9006 findet die rohe Fassung *mehr*, aber das Falsche.** Deren Kerne sind ein
  beziehungsweise drei Zeichen lang, und derart kurze Zeichenketten kommen unter anderen Typen
  vielfach vor: 2.371 und 1.642 Treffer gegen 800 und 4 bei der aufgefüllten Fassung. Die rohe Fassung ist
  hier nicht die großzügigere, sondern die unbrauchbare — sie liefert überwiegend Werte fremder
  Typen. Es ist derselbe Mechanismus wie bei E6: **die Trefferzahl ist die Kostengröße, und kurze
  Eingaben sind teuer.**

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **Je Typ dominiert genau eine Länge** | **ja — bei den sechs Typen, um die es geht** | Die Sollänge existiert. Die Normalisierung ist ein `LPAD` auf diese Länge und wird je Typ kuratiert. M43‑4 belegt, dass `LPAD` in allen acht geprüften Fällen den Originalwert trifft |
| **Je Typ streuen die Längen** | **ja — bei allen tragenden Suchtypen** | Bei 9018 (16 Längen, häufigste 44,49 %) und 9003 (33 Längen, 19,09 %) gibt es keine Sollänge. **Beide Zeilen treffen zu, für verschiedene Typen** — siehe den Absatz unten. Für diese Typen bliebe nur, beide Fassungen zu suchen, was nach E6 die Trefferzahl und damit den Preis verdoppelt |
| M43‑2 findet keine gemischten Schreibweisen | **nein** | — Die Uneinheitlichkeit liegt **nicht** allein bei der Eingabe |
| **M43‑2 findet gemischte Schreibweisen in nennenswertem Umfang** | **ja, bei zweien massiv** | 9017 mit **52,98 %** der numerischen Werte und bis zu **fünf** Varianten je Nummer, 9020 mit 20,10 %. **Der Bestand selbst ist uneinheitlich. Dann findet *keine* Fassung der Suche alles, und das muss der Nutzer erfahren** — es ist keine Sache, die man wegnormalisiert. Umgekehrt gilt: bei den sechs Typen mit durchgängig führender Null tritt der Fall **nicht** auf, dort wirkt die Normalisierung sauber |
| **Die Leerzeichen bei 9018 sind überwiegend folgende** | **ja — 24,83 von 25,88 Prozentpunkten** | **Frage 4 aus der Hauptrunde ist damit erheblich entlastet.** Die Kollation erledigt sie beim `=`-Vergleich, gemessen an echten Werten: 12 Treffer mit und ohne Leerzeichen. Nicht erledigt sind sie für die **Anzeige** und für ein **Präfixmuster** — `LIKE` folgt der PAD-SPACE-Regel nicht |
| Die Leerzeichen bei 9018 sind überwiegend führende | **nein — 1,05 %** | — Aber **nicht null**: 1,05 % von 143.213 Zeilen sind rund 1.500 Zeilen, die eine exakte Suche ohne Behandlung **nicht** findet. Die Gegenprobe zeigt es an einem Wert: 23 gefundene gegen 3 unerreichbare |
| **Die Gegenprobe zeigt Treffer nur bei der aufgefüllten Fassung** | **ja, bei sechs von acht Typen** | Die Normalisierung wirkt, und der Nutzer, der ohne Null tippt, wäre ohne sie **leer** ausgegangen — nicht bloß schlechter bedient |
| **Die Gegenprobe zeigt bei beiden Fassungen Treffer** | **ja, bei 9036 und 9006** | Dieselbe Lage wie bei M43‑2, aber aus einem anderen Grund: nicht weil der Bestand gemischt ist, sondern weil der Kern zu kurz ist. Siehe den Absatz unten |

**Wo die vorformulierte Zeile nicht passt — die beiden ersten Zeilen sind keine Alternative,
sondern eine Aufteilung.** Der Auftrag behandelt „eine Länge dominiert" und „die Längen streuen"
als Entweder-oder für die ganze Tabelle. Gemessen trennt der Befund die Typen sauber in zwei Lager,
und die Trennlinie ist dieselbe wie bei der führenden Null:

Die Bänder sind der Anteil der **häufigsten** Länge an der größeren der beiden Schreibweisengruppen:

| Band | Typen | Sollänge | Normalisierung |
|---|---|---|---|
| **100 %** | 2001, 2002, 9002, 9009, 9010, 9011, 9012, 9013, 9021, 9024, 9025, 9036, 9039, 2003 | **existiert** | `LPAD` trägt |
| **90 bis 99 %** | 9000 (96,37), 9004 (99,98), **9006 (96,70)**, 9020 (99,02), 9023 (95,40), 9027 (95,49), 9028/9029 (94,45), 9034 (99,64), 3 (97,84), 1 (99,28), 2 (99,27) | plausibel | `LPAD` trägt mit Restrisiko |
| **unter 90 %** | **9003 (19,09)**, 9038 (28,73), **9016 (39,66)**, **9018 (44,49)**, **9015 (50,22)**, 9030/9031 (50,00), **9019 (53,56)**, 9017 (56,60), 0 (69,77), 9037 (70,72), **9014 (71,86)**, **9001 (72,58)**, 9022 (73,66), 9007 (82,46), 9005 (84,65), 9033 (87,36), 9032 (88,03), 9008 (88,46) | **existiert nicht** | `LPAD` ist die falsche Form |

Das dritte Band enthält ausgerechnet die fünf Typen, die bei `NEXANS` die Wurzeln tragen (M39) —
9018, 9014, 9015, 9020 und 9016 —, mit Ausnahme von 9020. Und es enthält beide bisher kuratierten
Typen: 9001 und, nur eine Stufe darüber, 9006.

**Die Normalisierung ist damit dort am wenigsten anwendbar, wo am meisten gesucht wird** — und
genau dort ist die führende Null mit 1,30 % (9018), 2,11 % (9016), 6,59 % (9014), 9,93 % (9020) und
18,81 % (9015) auch am seltensten (M38). Beides passt zusammen: Wo die Werte einheitlich sind, ist
die Null systematisch; wo sie streuen, ist sie es nicht. **Der Typ, an dem die Normalisierung
gebraucht wird, und der Typ, an dem sie möglich ist, sind selten derselbe.**

**Und ein zweiter Befund ohne vorformulierte Zeile: Typ 2001 widerlegt die Ableitung aus einem
Monat, und zwar in derselben Runde, in der sie gezogen würde.** Fenster B sagt 100 % führende Null
bei einer Länge; der Bestand sagt 67,21 % und drei weitere Längen. Die Lücke, die der Belegvermerk
unten als Risiko benennen sollte, ist damit **kein Risiko, sondern ein Messwert**. Der Grund ist
fachlich plausibel und macht es nicht besser: 2001 gehört `SUTTONS`, und `SUTTONS` hat in Fenster B
2.581 BAM-Zeilen bei 21.516 Nachrichten (M39, M40) — ein Monat ist dort keine Stichprobe, sondern
ein Ausschnitt.

**Ein dritter: bei kurzen Kernen dreht die Gegenprobe ihr Vorzeichen um.** Die vorformulierte Zeile
„beide Fassungen zeigen Treffer" liest sich wie ein Gleichstand. Bei 9036 (Kern ein Zeichen) und
9006 (Kern drei Zeichen) findet die **rohe** Fassung 2.371 und 1.642 Treffer, die aufgefüllte 800
und 4 — die rohe findet also *mehr*, und praktisch alles davon gehört fremden Typen. Ohne
Normalisierung bekäme der Nutzer bei einer achtstelligen Lieferscheinnummer, von der er die fünf
führenden Nullen weglässt, 1.642 Treffer aus dem ganzen Bestand statt der vier, die er sucht.
**Das ist kein Normalisierungsproblem, sondern die Mindestlänge aus Regel L5** — und es zeigt zum
zweiten Mal nach E6, dass sie je Typ gelten muss.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Längenverteilung je Typ und je Schreibweise über **Fenster B** (n = 936.529
> Zeilen, 46 Typen) und für **sechs** Typen zusätzlich über den **Bestand** (n = 79.199 Zeilen).
> Die gemischten Schreibweisen und die Randleerzeichen sind **ausschließlich** über Fenster B
> gemessen. Die Gegenprobe M43‑4 steht auf **acht** Werten — einem je Typ (n = 8).
>
> *Behauptet wird:* dass daraus eine **Kuratierungsregel für die Zukunft** wird — eine Sollänge je
> Typ, die die Suche dauerhaft auffüllt.
>
> **Die Lücke, und sie ist diesmal beziffert.** Ein Typ kann seine Gestalt ändern, ohne dass es
> jemand merkt, und die Suche findet dann still weniger. Das ist hier keine Befürchtung: **2001
> zeigt es innerhalb derselben Erhebung** — 100 % über einen Monat, 67,21 % über den Bestand. Für
> die übrigen 40 Typen ist die Frage **nicht** gestellt worden; sie sind nur über Fenster B
> gemessen, und ob ihre Sollänge über 22 Monate hält, sagt diese Runde nicht. Ebenso wenig sagt
> sie, ob die gemischten Schreibweisen bei 9017 und 9020 über den Bestand häufiger oder seltener
> sind als die gemessenen 52,98 % und 20,10 %. Was trägt: *Bei den sechs Typen mit durchgängig
> führender Null existiert über den Bestand genau eine Länge, und `LPAD` trifft den Originalwert.*
> Was **nicht** gemessen ist: *ob das morgen noch gilt, und was der Bestand für die übrigen Typen
> sagt.* **Ob dafür eine Sicherung nötig wäre — eine wiederkehrende Prüfung der Sollänge gegen den
> Bestand —, ist eine Entscheidung und wird hier nicht getroffen.** Sie steht als **offene Frage
> 11**.

---

# M44 — Wie groß ist `MessageProperty` wirklich?

*Nachgetragen am 12.08.2026, zweiter Nachtrag desselben Tages.*

**Frage.** `MessageProperty` ist die einzige der vier großen Tabellen, deren Zeilenzahl nie gezählt
wurde. M14 hat den Zähllauf ausdrücklich abgelehnt („die Schätzung genügt, ein Zähllauf über
47 Millionen Zeilen ist den Aufwand nicht wert"). Nach M33‑0 — `MessageBAM` um 41,9 % größer als
dokumentiert — ist das keine haltbare Auskunft mehr: Die Zeile steht in
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 unter der Überschrift **„Gemessenes
Mengengerüst"**, und gemessen war sie nie.

> **Das Ergebnis vorweg, in vier Sätzen.** `MessageProperty` hat **75.571.462** Zeilen und ist damit
> **60,9 % größer** als dokumentiert — die größte Abweichung, die dieses Projekt an einer als
> „gemessen" ausgewiesenen Zahl gefunden hat, und die vierte in Folge. Die Zählung kostet **199,380 s**
> kalt und **20,633 s** warm; sie reißt die 60-Sekunden-Grenze des Rahmens um Faktor 3,3, bleibt aber
> weit unter der für diesen Zweck gesetzten Grenze von 15 Minuten. **Der folgenreichste Nebenbefund
> ist nicht die Zahl selbst, sondern was sie auflöst:** Die dokumentierten „rund vierzehn Zeilen je
> Nachricht" und die in M17 gemessenen 22,57 galten bisher als Zeitraumeffekt — mit der gezählten
> Zahl sind es **22,62**, und der Widerspruch verschwindet. Und die Zählung findet nebenbei zwei
> weitere geschätzte Zeilen im Mengengerüst: `Process` (**1.503** statt 1.490) und `Project`
> (**140** statt 142).

**Ohne Zeitfenster, und das ist begründungspflichtig (Regel L9).** Gefragt ist der **Bestand** einer
Tabelle. Ein Zeitfenster kann diese Frage grundsätzlich nicht beantworten — `MessageProperty` hat
keinen Zeitstempel, und selbst über den Umweg `Message` gerechnet wäre das Ergebnis die Zeilenzahl
eines Ausschnitts und nicht die der Tabelle. Dieselbe Argumentation wie bei M33 und M40‑3. Die
Kosten stehen unten und sind mit **199,380 s die höchsten aller durchgelaufenen Statements dieser
Runde** — knapp vor den 198,596 s von M33‑2. Teurer war nur die Auswahlabfrage, die nach 679 s
**ohne Ergebnis** abgebrochen wurde.

**Die 60-Sekunden-Grenze des Rahmens ist hier bewusst ausgesetzt.** `MessageBAM` kostete bei 7,08 GB
**18,795 s** kalt (M33‑0); `MessageProperty` ist mit 61,03 GB rund neunmal so groß, die Grenze ist
konstruktionsbedingt nicht einzuhalten. Gesetzt wurde stattdessen **`SET max_statement_time = 900`**
(15 Minuten) mit der Vorgabe, dass ein Abbruch das Ergebnis wäre und kein Fehlschlag. Er ist nicht
eingetreten: Die Zählung braucht **22,2 %** der gesetzten Grenze. **Dies ist eine einmalige
Bestandsaufnahme und kein Statement der Anwendung** — Regel L4 bleibt davon unberührt.

## M44‑0 Rahmen und die Gegenkontrolle

Aus §0 übernommen und erneut protokolliert, nicht angenommen:

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` — dieselbe Instanz wie am 11.08. und wie im ersten Nachtrag |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**. **Erste Abfrage jeder der vier Sitzungen**, vor jedem anderen Statement. Am Ende jeder Sitzung erneut geprüft: **achtmal `1`**, kein einziges `0` |
| Benutzer | **Lesebenutzer** `monitor_read@%`. `SHOW GRANTS FOR CURRENT_USER()` liefert dieselben drei Zeilen wie in §0, unverändert; der Passwort-Hash wird nach Regel G1 nicht abgedruckt |
| Sitzungen | **vier**: Rahmen und `EXPLAIN` (12:50), die Zählung (12:51–12:55), die übrigen Zeilen des Mengengerüsts (12:56), die Spaltenerhebung (13:08). Jede ist eine eigene Verbindung; die Prüfwerte werden je Sitzung neu hergeleitet und nicht übertragen |
| Serverzeit zu Beginn | `2026-08-12 12:50:16` (`UTC_TIMESTAMP` `10:50:16`, also UTC+2) |
| Serverzeit am Ende | `2026-08-12 13:08:44` (`UTC_TIMESTAMP` `11:08:44`) |
| Client | `mysql` `8.0.46` aus MySQL Workbench, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4` — wie im ersten Nachtrag |
| Grenze | **`SET max_statement_time = 900`** statt der 60 s des Rahmens, begründet oben. `@@global.max_statement_time` ist unverändert **`0`** |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `SHOW PROFILES`, `SET profiling_history_size = 100` |
| Wiederholungen | **ein Kaltlauf und ein warmer Lauf**, wie bei M33‑0 und ausdrücklich nicht „beste von fünf" — der Unterschied zwischen beiden ist hier Teil des Befunds |
| Regel S1 | kein `CREATE`, kein `ALTER`, kein Index — und **kein `ANALYZE TABLE`**, auch das schriebe auf `GlassfishDB`. Ausschließlich `SELECT` und `EXPLAIN`. Kein `KILL QUERY` nötig |
| Regel G1 | keine Belegnummer, kein BAM-Wert, kein `MessagePropertyValue` in dieser Datei. M44 liest **keinen einzigen Wert**, nur Zeilen zählende Aggregate |

### Die Testkopie ist unverändert

```sql
SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1073741824, 2) AS gb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Message','MessageAction','MessageBAM','MessageProperty','Process','Project')
ORDER BY DATA_LENGTH + INDEX_LENGTH DESC;
```

| | `DATA_LENGTH` | `INDEX_LENGTH` | gegen §0 |
|---|---:|---:|---|
| `Message` | 740.851.712 | 2.157.330.432 | **byteidentisch** |
| `MessageAction` | 2.226.634.752 | 819.855.360 | **byteidentisch** |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 | **byteidentisch** |
| `MessageProperty` | 15.088.615.424 | 45.945.946.112 | **byteidentisch mit M14** (07.08.2026) |

**Der Vorbehalt aus dem Auftrag kommt nicht zum Tragen.** Die drei Zeilen aus §0 stehen auf das Byte
gleich, und `MessageProperty` gleicht zusätzlich auf das Byte der Erhebung vom 07.08.2026 — über
**fünf** Messtage hinweg. Die Zahlen von M44 stehen damit auf demselben Boden wie M0 bis M43, und
die Textänderungen an den verbindlichen Dateien sind zulässig.

> **Die 61,0 GB der Dokumentation sind dezimal gerechnet, nicht binär.** 15.088.615.424 + 45.945.946.112
> = 61.034.561.536 Byte — das sind **61,03 GB** zu 10⁹ und **56,84 GiB** zu 2³⁰. Beide Angaben in
> `datenmodell.md` §8 (61,0 GB gesamt, 45,9 GB Index, 15,1 GB Daten) treffen die dezimale Lesart
> exakt. Das steht hier, weil eine spätere Messung mit `/1073741824` sonst wie eine Abweichung
> aussieht und keine ist.

## M44‑1 Welchen Weg nimmt der Server?

### Die Spalten von `MessageProperty` (Form wie M32)

```sql
SELECT ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLLATION_NAME, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageProperty'
ORDER BY ORDINAL_POSITION;
```

| Spalte | Typ | `NULL`? | Vorgabe | Sortierung | Schlüssel |
|---|---|---|---|---|---|
| `MessageID` | `varchar(36)` | NO | — | `utf8mb4_general_ci` | PRI |
| `MessageActionID` | `smallint(6)` | NO | — | — | PRI |
| `MessagePropertyName` | **`varchar(100)`** | NO | — | `utf8mb4_general_ci` | PRI |
| `MessagePropertyValue` | `mediumtext` | **YES** | — | `utf8mb4_general_ci` | MUL |

**Vier Spalten**, und die Angabe aus [`datenmodell.md`](datenmodell.md) §3 — Primärschlüssel
`(MessageID, MessagePropertyName, MessageActionID)`, `MessagePropertyValue mediumtext` — ist damit
**bestätigt**. `MessagePropertyValue` ist die **einzige `NULL`-fähige** Spalte der Tabelle; das ist
genau umgekehrt zu `MessageBAM`, wo M32 alle drei Spalten `NOT NULL` fand. Diese Zeile steht hier,
weil sie ohne Erhebung eine Vermutung wäre: `key_len` allein sagt nichts über `NULL`.

**Die Reihenfolge der Spalten ist nicht die des Schlüssels.** `MessageActionID` steht an
Position 2 der Tabelle, aber an Position 3 des Primärschlüssels. Für den Zugriff zählt die zweite.

### Die Indizes von `MessageProperty` (Form wie M32)

```sql
SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART, CARDINALITY, NULLABLE, INDEX_TYPE, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageProperty'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

**Sechs Indizes**, alle `BTREE`:

| `INDEX_NAME` | `SEQ_IN_INDEX` | `COLUMN_NAME` | `SUB_PART` | `CARDINALITY` | `NULL`? | eindeutig |
|---|---:|---|---:|---:|---|---|
| `PRIMARY` | 1 | `MessageID` | — | 4.696.427 | NO | ja |
| `PRIMARY` | 2 | `MessagePropertyName` | — | 46.964.279 | NO | ja |
| `PRIMARY` | 3 | `MessageActionID` | — | 46.964.279 | NO | ja |
| `MessagePropertyNameIDX` | 1 | `MessagePropertyName` | — | 18 | NO | nein |
| **`MessagePropertyNameValueIDX`** | 1 | `MessagePropertyName` | — | 18 | NO | nein |
| **`MessagePropertyNameValueIDX`** | 2 | **`MessagePropertyValue`** | **50** | 46.964.279 | YES | nein |
| **`MessagePropertyValueIDX`** | 1 | **`MessagePropertyValue`** | **50** | 46.964.279 | YES | nein |
| `MessageProperty_MessageActionFK` | 1 | `MessageID` | — | 5.218.253 | NO | nein |
| `MessageProperty_MessageActionFK` | 2 | `MessageActionID` | — | 15.654.759 | NO | nein |
| `MessageProperty_MessageFK` | 1 | `MessageID` | — | 7.827.379 | NO | nein |

**Unverändert gegenüber M14** ([`messungen-schritt5.md`](messungen-schritt5.md)) — dieselben sechs
Indizes, dieselben Kardinalitäten, dieselben zwei **Präfix-Indizes über 50 Zeichen**, auf denen
Regel L4 ruht.

> **`CARDINALITY` ist wie `TABLE_ROWS` eine Schätzung, und hier ist es dieselbe.** Vier der zehn
> Zeilen stehen auf **46.964.279** — exakt auf dem geschätzten Tabellenumfang. Gezählt sind
> **75.571.462**. Dieselbe Warnung wie in M32 und M23‑1, nur mit größerem Abstand.

### `EXPLAIN`

```sql
EXPLAIN SELECT COUNT(*) FROM GlassfishDB.MessageProperty;
```

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---:|---|---:|---|
| 1 | SIMPLE | `MessageProperty` | **`index`** | NULL | **`PRIMARY`** | **550** | NULL | 46.964.279 | **`Using index`** |

**Der Optimierer wählt `PRIMARY`** — und `PRIMARY` ist bei InnoDB der geclusterte Index, also die
**vollständigen Nutzdaten von 15,09 GB**. Nicht einer der schmaleren Sekundärindizes. Das erklärt die
Kosten: Die Zählung liest die ganze Tabelle und nicht eine Spalte davon. `Using index` heißt hier
nicht „ein schmaler Index genügt", sondern nur, dass keine zusätzliche Zeile nachgeschlagen wird.

`key_len` **550** geht mit den oben **erhobenen** Spaltentypen genau auf: `MessageID` `varchar(36)`
in `utf8mb4` (144 + 2 = 146) + `MessagePropertyName` `varchar(100)` (400 + 2 = 402) +
`MessageActionID` `smallint(6)` (2) = **550**. Der Plan liest also den vollständigen
Primärschlüssel und keinen Präfix davon.

> **Ob ein Sekundärindex billiger gewesen wäre, ist nicht gemessen.** Die fünf Sekundärindizes
> belegen zusammen 45,95 GB, im Mittel also rund 9 GB je Index gegen 15,09 GB des geclusterten —
> rechnerisch günstiger. Erzwungen und gegengemessen wurde das **nicht**; die Frage dieser Messung
> ist die Zeilenzahl und nicht die billigste Art, sie zu bekommen.

**Und der Plan selbst trägt die falsche Zahl.** `rows` steht auf **46.964.279**, tatsächlich sind es
**75.571.462** — der Optimierer unterschätzt die Arbeit dieses Statements um **37,9 %**. Das ist
kein Nebenbefund, sondern dieselbe Ursache wie bei L15 und M42‑1: Eine `rows`-Angabe im `EXPLAIN`
ist eine Stichprobenschätzung und keine Zählung.

## M44‑2 Die Zählung

```sql
SET max_statement_time = 900;
SELECT COUNT(*) AS gezaehlt FROM GlassfishDB.MessageProperty;
```

| | Ergebnis |
|---|---:|
| **gezählt** | **75.571.462** |
| dokumentiert in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 und §8, [`datenmodell.md`](datenmodell.md) §3 und §8, M14 | 46.964.279 |
| Abweichung | **+ 28.607.183 (+ 60,91 %)** |

**Zweimal gezählt, beide Male identisch** — der Kaltlauf und der warme Lauf liefern dieselbe Zahl.
Die dokumentierte Zahl ist die `TABLE_ROWS`-Schätzung aus `information_schema`; sie steht dort
unverändert seit dem 27.07.2026 und auch heute noch.

**Laufzeit:** **199,380 s** kalt, **20,633 s** warm — **Faktor 9,66**. Dieselbe Gestalt wie bei M33
(Faktor 5,2 bis 11,3) und aus demselben Grund: der Pufferpool. Kalt liest die Zählung 15,09 GB
erstmals von der Platte.

| Statement | kalt (einmalig) | warm | Faktor | Gelesen |
|---|---:|---:|---:|---|
| `COUNT(*)` über `MessageProperty` (M44) | **199,380 s** | **20,633 s** | **9,66** | `PRIMARY`, 15,09 GB |
| `COUNT(*)` über `MessageBAM` (M33‑0) | 18,795 s | 3,623 s | 5,2 | 7,08 GB gesamt |
| `COUNT(*)` über `MessageAction` (M14) | 28,341 s | — | — | 3,05 GB gesamt |

## M44‑3 Die übrigen Zeilen des Mengengerüsts

Weil sie zum selben Zweck erhoben werden und zusammen **4 ms** kosten:

```sql
SELECT COUNT(*) FROM GlassfishDB.Process;
SELECT COUNT(*) FROM GlassfishDB.`User`;
SELECT COUNT(*) FROM GlassfishDB.Project;
```

| Tabelle | `TABLE_ROWS` (Schätzung) | **gezählt** | Abweichung der Schätzung | Laufzeit |
|---|---:|---:|---|---:|
| `Process` | 1.490 | **1.503** | **0,9 % zu niedrig** | 1,592 ms |
| `Project` | 142 | **140** | **1,4 % zu hoch** | 0,678 ms |
| `User` | 36 | **36** | keine | 1,386 ms |

**Beide Abweichungen waren längst gemessen — und beide sind in der verbindlichen Datei nie
angekommen.** Die Fundstellen:

| Zahl | seit wann gezählt | wo sie stand |
|---|---|---|
| `Project` **140** | 28.07.2026 | [`annahmen-korrekturen.md`](annahmen-korrekturen.md); ab 01.08.2026 zusätzlich [`messungen-schritt4.md`](messungen-schritt4.md) Auffälligkeit G — dort **ausdrücklich** mit dem Satz „`PROJEKTBESCHREIBUNG.md` §8 nennt 142 Projekte, `annahmen-korrekturen.md` nennt 140 — die 140 sind die gezählten" |
| `Process` **1.503** | 01.08.2026 | [`messungen-schritt4.md`](messungen-schritt4.md) M10 („`information_schema` nennt 1.490 Zeilen, gezählt sind es 1.503") und seit Schritt 4 in der Feature-Datei [`prozessauswahl.md`](prozessauswahl.md) — dort **dreimal**, einmal als eigener „Nebenbefund" |

**Das ist die unangenehmste Zeile dieser Messung.** Bei `MessageBAM` und `MessageProperty` fehlte
die Zählung; hier lag sie vor, war benannt, war sogar mit der abweichenden Fundstelle in der
verbindlichen Datei nebeneinandergestellt — und ist trotzdem **elf** (`Process`) beziehungsweise
**fünfzehn** Tage (`Project`) lang nicht nachgezogen worden. Der Fehler ist an dieser Stelle also
**nicht** die fehlende Messung, sondern der fehlende Weg von der Messung in die Datei, die
Widersprüche entscheidet.

Die Zählung von heute reproduziert beide Werte exakt und macht sie damit zu bestätigten Werten und
nicht zu einmaligen Beobachtungen.

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Die Zählung liegt nahe an 46.964.279 (unter 5 % Abweichung) | **nein** | — Die Schätzung war für diese Tabelle **nicht** brauchbar |
| **Die Zählung weicht deutlich ab** | **ja — 60,91 %** | Wie bei `MessageBAM`, nur stärker. Die abgeleitete Kennzahl ist neu zu rechnen (siehe unten), und **das Muster ist bestätigt: `information_schema` ist bei den großen Tabellen unbrauchbar und nicht nur ungenau.** Vier von vier großen Tabellen sind jetzt gezählt, und in **allen vier** Fällen lag die Schätzung daneben: `Message` 6,5 % zu hoch, `MessageAction` 0,9 % zu niedrig, `MessageBAM` 41,9 % und `MessageProperty` 60,9 % zu niedrig |
| **Die Abweichung geht nach oben** | **ja** | `MessageProperty` ist **noch größer** als dokumentiert. **Regel L4 wird dadurch fester und nicht lockerer** — jede Abfrage, die anders als über `MessageID` einsteigt, wälzt 75,6 Millionen Zeilen um und nicht 47 Millionen |
| Die Zählung reißt die 15 Minuten | **nein — 199,380 s, also 22,2 % der Grenze** | — Der Abbruch als Ergebnis ist nicht eingetreten. Die Zeile im Mengengerüst wird **gezählt** und nicht „geschätzt, nie gezählt" |

**Wo die vorformulierte Zeile nicht passt — die neue Zahl löst einen dokumentierten Widerspruch
auf, statt einen zu erzeugen.** Das ist der eigentliche Befund von M44, und er stand in keiner
vorbereiteten Zeile:

| Angabe | Fundstelle | Wert |
|---|---|---|
| Zeilen je Nachricht, **Gesamtbestand**, aus der Schätzung gerechnet | `PROJEKTBESCHREIBUNG.md` §3.2 („rund vierzehn") | 46.964.279 / 3.341.519 = **14,05** |
| Zeilen je Nachricht, **dichter Bestand**, direkt gemessen | M17 ([`messungen-schritt5.md`](messungen-schritt5.md)) | **22,57** (Tag), **22,88** (Monat) |
| Zeilen je Nachricht, **Gesamtbestand**, aus der **Zählung** gerechnet | **M44** | 75.571.462 / 3.341.519 = **22,62** |

`datenmodell.md` §3 und `annahmen-korrekturen.md` erklären die Lücke zwischen 14,05 und 22,57 seit
dem 07.08.2026 mit **verschiedenen Nennern**: der eine sei das Mittel über den gesamten
Aufbewahrungszeitraum einschließlich der fünfmonatigen Datenlücke, der andere der dichte Bestand.
**Diese Erklärung war plausibel und ist falsch.** Mit der gezählten Zahl liegt das Mittel über den
Gesamtbestand bei **22,62** und damit **zwischen** den beiden direkt gemessenen Werten 22,57 und
22,88. Es gab nie einen Dichteeffekt — es gab eine um 60,9 % zu niedrige Schätzung.

> **Das ist die unangenehmere Sorte Fehler.** Eine falsche Zahl fällt auf, wenn sie widerspricht.
> Diese hier hat **nicht** widersprochen: Sie hat einen Widerspruch erzeugt, für den jemand eine
> plausible Erklärung gefunden hat — und die Erklärung hat den Fehler anschließend zugedeckt. Der
> Satz „verschiedene Nenner" steht seit fünf Tagen in zwei Dateien und ist mit der Zählung von heute
> gegenstandslos.

**Ein zweiter Befund ohne vorformulierte Zeile: die Bytes je Zeile.** Sie sind aus derselben
Schätzung gerechnet und ändern sich mit:

| | dokumentiert | gerechnet aus M44 |
|---|---:|---:|
| Speicherbedarf je Zeile | „1,3 Kilobyte" (`PROJEKTBESCHREIBUNG.md` §3.2) | **808 Byte** |
| davon Daten | 321 B (`annahmen-korrekturen.md`, 07.08.2026) | **200 B** |
| davon Index | 978 B (ebenda) | **608 B** |
| Index-Anteil | „zu drei Vierteln Index" (`datenmodell.md` §3) | **75,3 % — unverändert richtig** |

Die **Verhältnisaussage** hält, die **absoluten** Werte nicht. Beide alten Zahlen waren
Byte-Summen geteilt durch die geschätzte Zeilenzahl; mehr Zeilen bei gleichen Bytes heißt weniger
Byte je Zeile.

**Ein dritter: zwei weitere Zeilen des Mengengerüsts sind Schätzungen**, und beide sind seit dem
01.08.2026 gezählt, ohne dass die verbindliche Datei es erfahren hätte — siehe M44‑3. Damit sind von
den sieben Zeilen in §8 **sechs** aus `information_schema` übernommen und nur **eine** (`Message`)
gezählt gewesen.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Zeilenzahl von `MessageProperty` auf der **Testkopie**, Datenstand **08.07.2026**,
> zweimal gezählt mit identischem Ergebnis (n = 75.571.462 Zeilen, vollständig — dies ist keine
> Stichprobe). Dazu die Zeilenzahlen von `Process`, `Project` und `User` auf derselben Kopie.
>
> *Behauptet wird:* dass daraus eine Angabe über das Mengengerüst der **Produktion** wird — denn
> genau das ist `PROJEKTBESCHREIBUNG.md` §8, und dorthin geht die Zahl.
>
> **Die Lücke: die Vollkopie-Annahme, und sie ist nirgends geprüft.** Dass die Testkopie eine
> vollständige Kopie der Produktion ist, steht seit dem 27.07.2026 in beiden verbindlichen Dateien
> und ist in diesem Projekt **nie** gegen die Produktion gehalten worden — es besteht kein Zugang
> dorthin. Zwei Dinge sind darüber hinaus sicher **nicht** gemessen: Die Kopie ist vom 08.07.2026,
> die Produktion ist seither weitergewachsen; und der Bestand der Kopie hat eine **fünfmonatige
> Datenlücke** (M0, M9), die es in der Produktion so kaum gegeben haben dürfte. **Beide wirken in
> dieselbe Richtung: Die 75.571.462 sind eher eine Untergrenze für die Produktion als eine
> Punktschätzung.** Was trägt: *Auf der Testkopie hat `MessageProperty` 75.571.462 Zeilen, und die
> dokumentierten 46.964.279 waren die `information_schema`-Schätzung.* Was **nicht** gemessen ist:
> *wie viele Zeilen die Produktion hat.* Für Regel L4 ist das folgenlos — sie ruht auf der
> Bytegröße, und die Richtung des Fehlers macht sie strenger, nicht milder.

---

# M45 — Sind die Typbeschreibungen ohne ihre Endung noch eindeutig?

*Nachgetragen am 13.08.2026, dritter Nachtrag. Anlass: der offene Punkt 1 in
[`bam-werte.md`](bam-werte.md) §13.*

**Frage.** Die Beschriftungen der Belegdaten-Gruppen tragen Endungen (`_K_SAP`, `_L_SAP`, `_FORS`).
Auf der in der Abnahme geöffneten Nachricht enden **alle fünf** auf `_K_SAP` — dort unterscheidet die
Endung nichts und kostet in jeder Zeile Platz. Nach dem Leitsatz ist sie Beiwerk. Ob eine
Kürzungsregel *„Endung entfernen"* verlustfrei wäre, ist nie gemessen worden.

> **Das Ergebnis vorweg, in vier Sätzen.** `MessageBAMType` hat **62** Zeilen, alle mit Beschreibung,
> und die 62 Beschreibungen sind **paarweise verschieden**. Nach der Kürzung sind es **57** — fünf
> gehen verloren, verteilt auf **drei** Kollisionen über **acht** Typen, und zwei der drei treffen
> nicht etwa zwei 9xxx-Typen, sondern die **endungslosen Grundtypen 0 und 3**. Die Kürzung ist damit
> **nicht verlustfrei**; die Endung trägt bei diesen acht Typen die ganze Unterscheidung.
> **Der entscheidende Befund ist aber nicht die Kollisionszahl, sondern dass eine der Kollisionen
> tatsächlich zusammentrifft:** `Abladestelle_L_SAP` (9000) und `Abladestelle_K_SAP` (9016) stehen
> über Fenster B auf **3.405 Nachrichten gemeinsam** — nach einer Kürzung stünden dort zwei Gruppen
> mit **identischer** Überschrift untereinander.

**Diese Messung entscheidet nichts.** Sie stellt fest, was eine Kürzung kostete; ob gekürzt wird,
entscheidet der Auftraggeber. Die Anzeige bleibt bis dahin, wie sie ist.

## M45‑0 Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` — dieselbe Instanz wie am 11. und 12.08. |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**, **erste Abfrage jeder Sitzung**, vor jedem anderen Statement; in der Schlusssitzung am Ende erneut geprüft: wieder **`1`** |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Sitzungen | **sechs**, jede eine eigene Verbindung. Serverzeit zu Beginn `2026-08-13 10:22:25` |
| Client | `mysql` `8.0.46` aus MySQL Workbench, `--ssl-mode=DISABLED`, **`--default-character-set=utf8mb4`** — hier keine Formalie, sondern Bedingung: Gemessen werden Zeichen, Leerzeichen und Groß-/Kleinschreibung |
| Grenze | `SET max_statement_time = 60` in den beiden Sitzungen mit Zugriff auf `MessageBAM`. **Nicht erreicht** — teuerstes Statement 4,650 s |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `SHOW PROFILES` |
| Regel S1 | ausschließlich `SELECT`. Kein `CREATE`, kein `ALTER`, kein `ANALYZE TABLE` |
| Regel G1 | **kein BAM-Wert und keine Belegnummer.** M45 liest ausschließlich Stammdaten und zählende Aggregate. Typnummern und Beschreibungen sind nach der Regel in [`README.md`](README.md) Konfigurationsvokabular und bleiben stehen |
| Regel L9 | Die beiden Kontrollabfragen über `MessageBAM` tragen **Fenster B**. Ohne Fenster läuft nichts |

**Die Spalten, gegen `information_schema` erhoben** (Regel L8; die Tabelle gilt seit M7/M40 als
erhoben, die Spaltenliste steht hier trotzdem, weil M45 auf ihr rechnet):

| COLUMN_NAME | COLUMN_TYPE | IS_NULLABLE | COLUMN_KEY | COLLATION_NAME |
|---|---|---|---|---|
| `MessageBAMType` | `smallint(6)` | NO | PRI | — |
| `MessageBAMTypeDescription` | `varchar(70)` | **YES** | | **`utf8mb4_general_ci`** |

> **Die Kollation ist Teil des Befunds und keine Randnotiz.** `utf8mb4_general_ci` vergleicht
> **ohne** Rücksicht auf Groß- und Kleinschreibung und ist **PAD SPACE**, ignoriert also folgende
> Leerzeichen. Die Kollisionszählung unten ist deshalb **dreimal** gerechnet worden — unter der
> Spaltenkollation, zeichengenau (`utf8mb4_bin`) und zeichengenau nach `TRIM`. **Alle drei liefern
> 57.** Die Kollisionen hängen also nicht an einer Kollationsfeinheit; sie sind echte Dopplungen.

## M45‑1 Alle 62 Zeilen

Vollständig abgedruckt, wie die Aufgabenstellung es verlangt. `MessageBAMTypeDescription` ist
nullbar — **keine** der 62 Zeilen ist `NULL` oder leer.

| Typ | Beschreibung | Endung | gekürzt |
|---:|---|---|---|
| 0 | `Bestellnummer` | — | `Bestellnummer` |
| 1 | `Auftragsnummer` | — | `Auftragsnummer` |
| 2 | `Lieferscheinnummer` | — | `Lieferscheinnummer` |
| 3 | `Rechnungsnummer` | — | `Rechnungsnummer` |
| 1000 | `InvoiceNumber` | — | `InvoiceNumber` |
| 1001 | `PartNumber` | — | `PartNumber` |
| 1002 | `PONumber` | — | `PONumber` |
| 1003 | `TradingPartnerID` | — | `TradingPartnerID` |
| 1004 | `ShippingAdress` | — | `ShippingAdress` |
| 1005 | `ShipperNumber` | — | `ShipperNumber` |
| 2000 | `OrderNumber` | — | `OrderNumber` |
| 2001 | `VendorReference` | — | `VendorReference` |
| 2002 | `InvoiceNumber VTG` | — | `InvoiceNumber VTG` |
| 2003 | `OrderLoadNo` | — | `OrderLoadNo` |
| 2004 | `YourReference` | — | `YourReference` |
| 2005 | `CustRef1` | — | `CustRef1` |
| 2006 | `CustRef2` | — | `CustRef2` |
| 2007 | `LoadNo` | — | `LoadNo` |
| 2008 | `Tanknummer` | — | `Tanknummer` |
| 2009 | `Statusreason` | — | `Statusreason` |
| 2010 | `Statuscode` | — | `Statuscode` |
| 2011 | `InvoiceNumber Vendor` | — | `InvoiceNumber Vendor` |
| 9000 | `Abladestelle_L_SAP` | `_L_SAP` | **`Abladestelle`** ⚠ |
| 9001 | `Abrufnummer_L_SAP` | `_L_SAP` | `Abrufnummer` |
| 9002 | `Lieferplannummer_L_SAP` | `_L_SAP` | `Lieferplannummer` |
| 9003 | `Material-Nr. beim Lieferanten_L_SAP` | `_L_SAP` | `Material-Nr. beim Lieferanten` |
| 9004 | `Unsere Material-Nr._L_SAP` | `_L_SAP` | `Unsere Material-Nr.` |
| 9005 | `Werk_L_SAP` | `_L_SAP` | `Werk` |
| 9006 | `Lieferschein-Nr._L_SAP` | `_L_SAP` | `Lieferschein-Nr.` |
| 9007 | `Transport-Nummer_L_SAP` | `_L_SAP` | `Transport-Nummer` |
| 9008 | `Beleg-Nr.  TSL _L_SAP` † | `_L_SAP` | `Beleg-Nr.  TSL ` † |
| 9009 | `Beleg-Nr. GS_L_SAP` | `_L_SAP` | `Beleg-Nr. GS` |
| 9010 | `Materialbeleg (Entnahme)_L_SAP` | `_L_SAP` | `Materialbeleg (Entnahme)` |
| 9011 | `Anlieferungs-Nr. ae_L_SAP` | `_L_SAP` | `Anlieferungs-Nr. ae` |
| 9012 | `Charge_L_SAP` | `_L_SAP` | `Charge` |
| 9013 | `Nr. TSL_L_SAP` | `_L_SAP` | `Nr. TSL` |
| 9014 | `Lieferantennummer beim Kunden_K_SAP` | `_K_SAP` | `Lieferantennummer beim Kunden` |
| 9015 | `Kundenwerk_K_SAP` | `_K_SAP` | `Kundenwerk` |
| 9016 | `Abladestelle_K_SAP` | `_K_SAP` | **`Abladestelle`** ⚠ |
| 9017 | `(JIT-) Abrufnummer_K_SAP` | `_K_SAP` | `(JIT-) Abrufnummer` |
| 9018 | `Kundenmaterialnummer_K_SAP` | `_K_SAP` | `Kundenmaterialnummer` |
| 9019 | `Bestellnummer vom Kunden_K_SAP` | `_K_SAP` | `Bestellnummer vom Kunden` |
| 9020 | `Lieferschein, Entnahme , PUS_K_SAP` | `_K_SAP` | `Lieferschein, Entnahme , PUS` |
| 9021 | `Transportnummer_K_SAP` | `_K_SAP` | `Transportnummer` |
| 9022 | `Gutschriftsanzeigen-Nummer_K_SAP` | `_K_SAP` | `Gutschriftsanzeigen-Nummer` |
| 9023 | `Übertragungsnummer Gutschrift_K_SAP` | `_K_SAP` | `Übertragungsnummer Gutschrift` |
| 9024 | `Rechnungsnummer_K_SAP` | `_K_SAP` | **`Rechnungsnummer`** ⚠ |
| 9025 | `Abladestelle_FORS` | `_FORS` | **`Abladestelle`** ⚠ |
| 9026 | `LS/RE-Nummer_FORS` | `_FORS` | `LS/RE-Nummer` |
| 9027 | `Bestellnummer_FORS` | `_FORS` | **`Bestellnummer`** ⚠ |
| 9028 | `Material-Nr. beim Kunden_FORS` | `_FORS` | `Material-Nr. beim Kunden` |
| 9029 | `Material-Nr.beim Lieferanten_FORS` | `_FORS` | `Material-Nr.beim Lieferanten` |
| 9030 | `Sender_Ident_FORS` | `_FORS` | `Sender_Ident` |
| 9031 | `Empf_Ident_FORS` | `_FORS` | `Empf_Ident` |
| 9032 | `Sendercode_K_SAP` | `_K_SAP` | `Sendercode` |
| 9033 | `Empfaengercode_K_SAP` | `_K_SAP` | `Empfaengercode` |
| 9034 | `Bestellnummer_L_SAP` | `_L_SAP` | **`Bestellnummer`** ⚠ |
| 9035 | `Werk Kunde_L_SAP` | `_L_SAP` | `Werk Kunde` |
| 9036 | `Lagerort Kunde_L_SAP` | `_L_SAP` | `Lagerort Kunde` |
| 9037 | `Packmittelnummer Kunde_L_SAP` | `_L_SAP` | `Packmittelnummer Kunde` |
| 9038 | `Packmittelnummer Lieferant_L_SAP` | `_L_SAP` | `Packmittelnummer Lieferant` |
| 9039 | `Daten-Sender-Nummer_L_SAP` | `_L_SAP` | `Daten-Sender-Nummer` |

† **Typ 9008 trägt zwei aufeinanderfolgende Leerzeichen und ein Leerzeichen vor dem Unterstrich**
(`Beleg-Nr.··TSL·_L_SAP`, gemessene Länge 21 Zeichen). Markdown zieht das beim Rendern zusammen; die
gemessene Zeichenkette ist die mit den beiden Leerzeichen. Er ist zugleich der einzige Typ, dessen
gekürzte Form auf ein Leerzeichen endet — deshalb die Gegenrechnung mit `TRIM` in M45‑3.

**Längen** (n = 62, vollständig):

| | alle 62 | die 40 mit Endung |
|---|---:|---:|
| Länge im Mittel | **19,0** | **22,6** |
| Länge maximal | 35 | 35 |
| gekürzte Länge im Mittel | 15,3 | 16,8 |
| gekürzte Länge maximal | 29 | 29 |
| Endung im Mittel | 3,8 | **5,8** |

Die Kürzung nähme einer Beschriftung **mit** Endung im Mittel 5,8 von 22,6 Zeichen — **rund ein
Viertel**. Das ist die Größenordnung, um die es beim Platz geht; sie steht hier als Zahl und nicht
als Eindruck.

## M45‑2 Welche Endungen kommen vor

**Nicht drei angenommen, sondern abgelesen.** Die Endung ist dafür als *abschließender Lauf aus
`_GROSSBUCHSTABEN`-Segmenten* bestimmt — `REGEXP_SUBSTR(beschreibung COLLATE utf8mb4_bin,
'(_[A-Z]+)+$')`. Die Kollation im Ausdruck ist zwingend: Unter der Spaltenkollation `general_ci`
träfe `[A-Z]` auch Kleinbuchstaben, und `Sender_Ident_FORS` bekäme fälschlich die Endung
`_Ident_FORS`.

**Die Gegenprobe zur Regel steht daneben** — die Zahl der Unterstriche je Beschreibung, roh gezählt:

| Unterstriche | Typen |
|---:|---:|
| 0 | 22 |
| 1 | 5 |
| 2 | 35 |

| Endung | Typen | Typnummern von–bis |
|---|---:|---|
| **(ohne Endung)** | **22** | 0 – 2011 |
| **`_L_SAP`** | **20** | 9000 – 9039 |
| **`_K_SAP`** | **13** | 9014 – 9033 |
| **`_FORS`** | **7** | 9025 – 9031 |

**Es sind genau drei, und die Vermutung aus §13 ist damit bestätigt** — aber erst jetzt gemessen.
Zwei Beobachtungen kommen hinzu, die dort nicht standen:

1. **22 von 62 Typen tragen überhaupt keine Endung** — die Grundtypen 0–3 und die Blöcke 1000er und
   2000er. Eine Kürzungsregel beträfe also nur **40** Typen, kollidierte aber ausgerechnet mit den
   endungslosen (M45‑3).
2. **Die beiden `_FORS`-Typen 9030 und 9031 tragen einen zweiten Unterstrich im Namensteil**
   (`Sender_Ident`, `Empf_Ident`). Der Unterstrich ist in dieser Tabelle also **kein** verlässliches
   Trennzeichen zwischen Name und Endung; nur die Großschreibung der Endungssegmente ist es. Das ist
   der Grund, warum die Regel oben so und nicht über „ab dem letzten Unterstrich" formuliert ist —
   die naive Fassung lieferte für 33 Typen `_SAP` statt `_L_SAP`/`_K_SAP` und verlöre genau die
   Unterscheidung, um die es geht.

## M45‑3 Die Kürzungsregel: 62 werden 57

| | |
|---:|---|
| Typen | **62** |
| verschiedene Beschreibungen **ungekürzt** | **62** |
| verschiedene Beschreibungen **gekürzt**, Spaltenkollation `general_ci` | **57** |
| verschiedene Beschreibungen **gekürzt**, zeichengenau `utf8mb4_bin` | **57** |
| verschiedene Beschreibungen **gekürzt**, zeichengenau **nach `TRIM`** | **57** |

**Fünf Formen gehen verloren, und alle drei Rechnungen sind sich einig.** Die Kürzung ist damit
**nicht verlustfrei**.

### Die Kollisionen, namentlich

| gekürzte Form | Typen | Typnummern | Endungen | ursprünglich |
|---|---:|---|---|---|
| **`Abladestelle`** | **3** | 9000, 9016, 9025 | `_L_SAP`, `_K_SAP`, `_FORS` | `Abladestelle_L_SAP` · `Abladestelle_K_SAP` · `Abladestelle_FORS` |
| **`Bestellnummer`** | **3** | **0**, 9027, 9034 | **(ohne)**, `_FORS`, `_L_SAP` | `Bestellnummer` · `Bestellnummer_FORS` · `Bestellnummer_L_SAP` |
| **`Rechnungsnummer`** | **2** | **3**, 9024 | **(ohne)**, `_K_SAP` | `Rechnungsnummer` · `Rechnungsnummer_K_SAP` |

**Acht Typen fallen auf drei Formen** — 62 − 8 + 3 = 57, die Zahl geht auf.

> **Der Befund, den die Aufgabenstellung nicht vorgesehen hat: zwei der drei Kollisionen treffen
> einen Typ *ohne* Endung.** Die Frage lautete, ob die Beschreibungen ohne ihre Endung noch
> eindeutig sind — gedacht war dabei an die 9xxx-Typen untereinander. Gemessen kollidiert
> `Bestellnummer_L_SAP` aber mit dem **Grundtyp 0** und `Rechnungsnummer_K_SAP` mit dem **Grundtyp
> 3**, und diese beiden sind von der Kürzung gar nicht betroffen. **Eine Kürzungsregel kann diesen
> Fall nicht durch eine Ausnahme an den 9xxx-Typen heilen** — sie müsste den unveränderten
> Grundtyp mitprüfen. Fachlich ist die Doppelung ohnehin bemerkenswert: Typ 0 gehört `IBIS` und
> `IBISGUS`, 9034 gehört `NEXANS` (M45‑5).

## M45‑4 Beinahe-Kollisionen — zwei weitere Paare, die nur an einem Satzzeichen hängen

Dieselbe Rechnung noch einmal, die gekürzte Form zusätzlich auf **Buchstaben und Ziffern**
eingedampft (kleingeschrieben, alles andere entfernt). Sie fördert Paare zutage, die eine exakte
Zeichenkette trennt und ein Leser nicht:

| eingedampft | Typen | Typnummern | Endungen | ursprünglich |
|---|---:|---|---|---|
| `abladestelle` | 3 | 9000, 9016, 9025 | `_L_SAP`, `_K_SAP`, `_FORS` | *(schon oben)* |
| `bestellnummer` | 3 | 0, 9027, 9034 | (ohne), `_FORS`, `_L_SAP` | *(schon oben)* |
| `rechnungsnummer` | 2 | 3, 9024 | (ohne), `_K_SAP` | *(schon oben)* |
| **`materialnrbeimlieferanten`** | **2** | **9003, 9029** | `_L_SAP`, `_FORS` | `Material-Nr. beim Lieferanten_L_SAP` · `Material-Nr.beim Lieferanten_FORS` |
| **`transportnummer`** | **2** | **9007, 9021** | `_L_SAP`, `_K_SAP` | `Transport-Nummer_L_SAP` · `Transportnummer_K_SAP` |

**Die beiden neuen Paare unterscheiden sich um genau ein Zeichen** — ein fehlendes Leerzeichen nach
dem Punkt (9029) und ein Bindestrich (9007 gegen 9021). Sie sind **keine** Kollisionen der
Kürzungsregel: Ungekürzt wie gekürzt sind beide Zeichenketten verschieden, und die Anzeige zeigt sie
verschieden. **Sie stehen hier, weil sie die Vermutung in M45‑5 tragen** und weil sie zeigen, dass
die Beschreibungen aus dem Altsystem nicht nach einer Regel gepflegt worden sind.

## M45‑5 Bezeichnen zwei Typen dasselbe Feld aus verschiedenen Systemen?

**Als benannte Vermutung, nicht als Behauptung** — die Aufgabenstellung verlangt genau diese Form.

> **Vermutung.** Die Endung sagt nicht, *welches Feld* gemeint ist, sondern **aus wessen Sicht** es
> geführt wird: `_L_SAP` die Lieferantenseite, `_K_SAP` die Kundenseite, `_FORS` ein drittes System.
> Zwei Typen mit gleichem Namensteil und verschiedener Endung wären dann **dasselbe Feld, zweimal
> geführt**.

Was die Beschreibungen dazu hergeben — nebeneinandergestellt, ohne Deutung:

| Namensteil | `_L_SAP` | `_K_SAP` | `_FORS` |
|---|---|---|---|
| Abladestelle | 9000 `Abladestelle_L_SAP` | 9016 `Abladestelle_K_SAP` | 9025 `Abladestelle_FORS` |
| Bestellnummer | 9034 `Bestellnummer_L_SAP` | — *(9019 `Bestellnummer vom Kunden_K_SAP`)* | 9027 `Bestellnummer_FORS` |
| Transportnummer | 9007 `Transport-Nummer_L_SAP` | 9021 `Transportnummer_K_SAP` | — |
| Material-Nr. beim Lieferanten | 9003 `Material-Nr. beim Lieferanten_L_SAP` | — | 9029 `Material-Nr.beim Lieferanten_FORS` |

**Zwei Beobachtungen sprechen für die Vermutung**, beide gemessen:

1. **Dieselbe gekürzte Form kommt unter mehr als einer Endung vor — bei allen drei Kollisionen**
   (gezählt: 3 von 3). Es gibt **keine** Kollision zweier Typen mit *derselben* Endung. Wäre die
   Endung bedeutungslos, wäre das ein Zufall; wäre sie ein Systemkennzeichen, ist es die Regel.
2. **Die Beschriftungen sagen es an einer Stelle selbst.** 9014 heißt
   `Lieferantennummer beim Kunden_K_SAP`, 9003 `Material-Nr. beim Lieferanten_L_SAP` — die Endung
   und der Zusatz *„beim …"* zeigen in dieselbe Richtung.

**Was dagegen spricht, und es bleibt offen:** Die Zuordnung `_L_SAP` → Lieferant, `_K_SAP` → Kunde
ist aus den Beschreibungen **erschlossen** und steht nirgends geschrieben. `_FORS` ist gar nicht
gedeutet. **Bestätigen könnte das nur das Altsystem oder sein Betreuer, nicht diese Datenbank**
(Regel Q4: nichts raten).

### Die Kontrollabfragen — trifft eine Kollision je zusammen?

Die Kollisionszahl allein sagt noch nicht, ob ein Nutzer je zwei gleich beschriftete Gruppen sähe.
Zwei Abfragen klären das, und die zweite ist der eigentliche Befund dieser Messung.

**(1) Konfiguriert ein Mandant zwei kollidierende Typen zugleich?** Über `MessageBAMMandant`,
Laufzeit 3,3 ms:

| gekürzte Form | Mandant | konfigurierte Typen | welche |
|---|---|---:|---|
| **`Abladestelle`** | **`NEXANS`** | **3** | **9000, 9016, 9025** |
| **`Bestellnummer`** | **`NEXANS`** | **2** | **9027, 9034** |
| `Bestellnummer` | `IBIS` | 1 | 0 |
| `Bestellnummer` | `IBISGUS` | 1 | 0 |
| `Rechnungsnummer` | `IBIS` | 1 | 3 |
| `Rechnungsnummer` | `IBISGUS` | 1 | 3 |
| `Rechnungsnummer` | `NEXANS` | 1 | 9024 |
| `Rechnungsnummer` | `ZAST` | 1 | 3 |

**Ja — `NEXANS` konfiguriert alle drei `Abladestelle`-Typen und beide `Bestellnummer`-Typen.** Die
Kollision ist damit kein mandantenübergreifendes Kuriosum: Sie liegt vollständig innerhalb eines
einzigen Mandanten, und zwar des größten (M39).

> Die Konfiguration ist nach §6 von [`bam-werte.md`](bam-werte.md) **kein Filter** — der Block zeigt
> auch unkonfigurierte Typen. Diese Abfrage ist deshalb nur der Vorbefund; entscheidend ist die
> zweite.

**(2) Stehen zwei kollidierende Typen auf *derselben Nachricht*?** Über Fenster B
(`2025-11-30` ≤ x < `2025-12-30`, 214.330 Nachrichten), Laufzeit 3,516 s:

| gekürzte Form | Typen auf der Nachricht | Nachrichten |
|---|---:|---:|
| `Abladestelle` | 1 | 26.840 |
| **`Abladestelle`** | **2** | **3.405** |
| `Bestellnummer` | 1 | 6.016 |
| `Rechnungsnummer` | 1 | 987 |

Und welche zwei es sind (Laufzeit 4,650 s):

| Kombination | Nachrichten |
|---|---:|
| 9016 allein | 23.055 |
| 9000 allein | 3.775 |
| **9000 + 9016** | **3.405** |
| 9025 allein | 10 |

> **Das ist der Befund.** `Abladestelle_L_SAP` und `Abladestelle_K_SAP` stehen über einen Monat auf
> **3.405 Nachrichten gemeinsam** — auf **11,3 %** der 30.245 Nachrichten des Fensters, die
> überhaupt eine `Abladestelle` tragen. **Nach einer Kürzung stünden dort zwei Gruppen mit
> identischer Überschrift `Abladestelle` untereinander, mit verschiedenen Werten darin und ohne
> jedes Unterscheidungsmerkmal.** Das ist genau die Gestalt, an der M37 den Listenschlüssel
> zerbrochen hätte — nur diesmal nicht für React, sondern für den Leser.
>
> **`Bestellnummer` und `Rechnungsnummer` treffen im Fenster nicht zusammen** (je nur ein Typ je
> Nachricht). Dort bliebe die Kürzung unsichtbar — solange sich der Bestand nicht ändert.

## Was daraus folgt

| Frage der Aufgabenstellung | Antwort |
|---|---|
| 1. Alle Zeilen mit Typnummer und Beschreibung | **62**, vollständig in M45‑1. Keine `NULL`, keine leere |
| 2. Welche Endungen kommen vor, wie oft | **Genau drei**: `_L_SAP` (20), `_K_SAP` (13), `_FORS` (7); **22** ohne Endung (M45‑2) |
| 3. Bleiben die gekürzten Formen eindeutig, und welche kollidieren | **Nein — 62 werden 57.** Drei Kollisionen über acht Typen: `Abladestelle` (9000/9016/9025), `Bestellnummer` (0/9027/9034), `Rechnungsnummer` (3/9024) (M45‑3) |
| 4. Bezeichnen zwei Typen dasselbe Feld aus verschiedenen Systemen | **Als Vermutung ja**, gestützt darauf, dass **jede** der drei Kollisionen verschiedene Endungen paart und keine dieselbe. Bestätigen kann das nur das Altsystem (M45‑5) |

**Für die Anzeige heißt das nichts** — sie ändert sich in dieser Nacharbeit nicht. Für eine
**spätere** Entscheidung heißt es dreierlei:

1. **Eine pauschale Kürzung ist ausgeschlossen.** Sie erzeugt auf 3.405 Nachrichten je Monat zwei
   ununterscheidbare Überschriften.
2. **Eine Kürzung „nur wo eindeutig" wäre möglich** — 54 der 62 Typen sind nach der Kürzung
   weiterhin eindeutig —, hätte aber den Preis, dass die Beschriftung mal mit und mal ohne Endung
   erschiene. Ob das besser ist als durchgängig mit, ist eine Gestaltungsfrage und **hier nicht
   entschieden**.
3. **Die Prüfung muss die 22 endungslosen Typen einschließen.** Zwei der drei Kollisionen entstehen
   erst mit ihnen.

### Belegvermerk (Regel L10)

> *Gemessen:* Alle **62** Zeilen von `MessageBAMType` auf der **Testkopie** (Datenstand 08.07.2026),
> vollständig und nicht als Stichprobe; die Endungsverteilung; die Zahl der verschiedenen gekürzten
> Formen unter drei Vergleichsarten; die Konfiguration der acht kollidierenden Typen je Mandant; und
> das gemeinsame Vorkommen zweier kollidierender Typen auf derselben Nachricht über **Fenster B**
> (n = 214.330 Nachrichten).
>
> *Behauptet wird:* dass eine Kürzungsregel *„Endung entfernen"* **nicht verlustfrei** wäre und dass
> die Endung bei diesen acht Typen die Unterscheidung trägt.
>
> **Die Lücken, und es sind drei.** (a) **Eindeutigkeit ist gemessen, Verständlichkeit nicht.**
> Gezählt sind identische Zeichenketten. Ob ein Nutzer `Transport-Nummer` und `Transportnummer`
> auseinanderhält, ist damit **nicht** beantwortet — M45‑4 zeigt, dass es solche Paare gibt, und
> zählt sie nicht als Kollision. (b) **Fenster B ist ein Monat, nicht der Bestand.** Dass
> `Bestellnummer` und `Rechnungsnummer` dort nicht zusammentreffen, heißt nicht, dass sie es nie
> tun; für `Abladestelle` ist die Aussage dagegen belastbar, weil sie **positiv** ist — 3.405 Fälle
> genügen, um die Möglichkeit zu belegen. (c) **Stammdaten der Kopie sind nicht Stammdaten der
> Produktion.** Ein seit dem 08.07.2026 in der Produktion angelegter Typ ist hier nicht enthalten,
> und schon einer genügte, um aus einer eindeutigen Form eine kollidierende zu machen. **Eine
> Kürzungsregel hinge damit an einer Tabelle, die sich ohne unser Zutun ändern kann** — das ist das
> stärkste Argument gegen sie und zugleich das einzige, das diese Messung nicht selbst liefert.

---

# E7 — Wie viele BAM-Werte tragen ein Leerzeichen **innen**?

*Ergänzende Messung, 13.08.2026, dritter Nachtrag. Sie gehört zur selben Sitzungsfolge wie M45.*

**Frage, und warum sie gestellt wurde.** Die Nacharbeit zu Schritt 7, Teil 1 setzt die Werte einer
Gruppe **nebeneinander** statt untereinander. Das geht nur, wenn die Wortgrenze sichtbar ist — sonst
liefe ein Wert aus vier durch Leerzeichen getrennten Blöcken neben dem nächsten Wert zu einem
unlesbaren Band zusammen. Die Begründung
für die gewählte Bauform (eine Marke je Wert) lautete bis hierhin *„Werte enthalten Leerzeichen"*,
und das war **behauptet und nicht gemessen**. M38 misst Leerzeichen **am Rand**; nach innen hat
niemand gesehen.

**Statement.** Fenster B, `TRIM` vor dem Vergleich, damit die Randleerzeichen aus M38 nicht
mitgezählt werden. **Regel G1: kein Wert wird ausgegeben, nur gezählt.**

```sql
SELECT b.MessageBAMType, COUNT(*)
  FROM GlassfishDB.MessageBAM b
  JOIN GlassfishDB.Message m ON m.MessageID = b.MessageID
 WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
   AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
   AND TRIM(b.MessageBAMValue) LIKE '% %'
 GROUP BY b.MessageBAMType;
```

## Ergebnis — Fenster B

| | |
|---:|---|
| BAM-Zeilen im Fenster | **936.529** |
| davon mit Leerzeichen **innen** | **27.792** |
| Anteil | **2,97 %** |
| betroffene Typen | **16** |

| Typ | Beschreibung | Werte mit innerem Leerzeichen |
|---:|---|---:|
| **9016** | `Abladestelle_K_SAP` | **11.360** |
| **9003** | `Material-Nr. beim Lieferanten_L_SAP` | **10.059** |
| **9018** | `Kundenmaterialnummer_K_SAP` | **5.166** |
| 9032 | `Sendercode_K_SAP` | 781 |
| 9033 | `Empfaengercode_K_SAP` | 257 |
| 9038 | `Packmittelnummer Lieferant_L_SAP` | 48 |
| 9017 | `(JIT-) Abrufnummer_K_SAP` | 47 |
| 9037 | `Packmittelnummer Kunde_L_SAP` | 35 |
| 9034 | `Bestellnummer_L_SAP` | 8 |
| 9019 | `Bestellnummer vom Kunden_K_SAP` | 8 |
| 9004 | `Unsere Material-Nr._L_SAP` | 6 |
| 9030 | `Sender_Ident_FORS` | 5 |
| 9031 | `Empf_Ident_FORS` | 5 |
| 9028 | `Material-Nr. beim Kunden_FORS` | 3 |
| 9029 | `Material-Nr.beim Lieferanten_FORS` | 3 |
| 0 | `Bestellnummer` | 1 |

## Was daraus folgt

**Drei Prozent klingen wenig und sind es nicht.** Entscheidend ist, **wo** sie liegen: Auf Platz drei
steht **9018**, und das ist der Typ, den `NEXANS` auf **92,26 %** seiner Wurzeln trägt (M39). Platz
eins und zwei — 9016 und 9003 — gehören demselben Mandanten. **Die Werte mit Leerzeichen sind also
keine Randerscheinung eines kleinen Typs, sondern stehen auf den Nachrichten des größten Mandanten.**

**Ein Trennzeichen wäre damit ausgeschlossen**, und zwar unabhängig von der Frage, ob ein Wert je ein
Komma enthält: Ein Leerzeichen als Trenner zerschnitte 27.792 Werte je Monat in Teile. Die Marke
macht die Grenze zu einer Eigenschaft der **Darstellung**; sie braucht über die Daten gar keine
Annahme.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Zahl der `MessageBAM`-Zeilen über **Fenster B** (n = 936.529, vollständig), deren
> Wert nach `TRIM` ein Leerzeichen enthält, aufgeschlüsselt nach Typ.
>
> *Behauptet wird:* dass eine Darstellung nebeneinander ohne sichtbare Wortgrenze unlesbar wäre.
>
> **Die Lücke:** Gemessen ist das **Vorkommen** von Leerzeichen, nicht die **Lesbarkeit**. Dass
> ein vierblockiger Wert neben einem zweiten Wert ohne Marke als vier oder fünf Angaben gelesen
> würde, ist
> ein Schluss über den Leser und keine Messung. Er ist am 13.08.2026 an echten Daten *angesehen*
> worden ([`bam-werte.md`](bam-werte.md) §15), aber nicht geprüft. Zwei weitere Zeichenklassen sind
> **nicht** gemessen: ob Werte Kommata, Semikola oder Pipes tragen. Genau deshalb wählt die Anzeige
> kein Trennzeichen — die Frage muss dann nicht beantwortet werden.

---

# M46 — Woher kommt die Sollänge, und gilt sie je Mandant?

**Frage.** M43 hat die Sollänge für **sechs** Typen über den Bestand belegt und für alle übrigen nur
über Fenster B. Genau dort ist der Fehler gemessen worden: Typ 2001 lag über den Monat bei 100 % und
über den Bestand bei 67,21 %. Eine Kuratierung aus einem Monat gilt anschließend für 22 Monate.
M46 holt drei Zahlen nach, die die Kuratierung trägt: die Dominanz über den **Bestand** für **jeden**
Typ, die Frage, ob die Sollänge je **Mandant** dieselbe ist, und den Anteil **führender** Leerzeichen.

> **Das Ergebnis vorweg, in vier Sätzen.** Die Sollänge ist **keine Eigenschaft des Typs** — sie
> weicht je Mandant ab, und zwar an zwei Stellen unabhängig voneinander: Typ 2000 liegt bei
> `SUTTONS` auf Länge 6 und bei `VOTG` auf Länge 7, und Typ 9014 erreicht über den Bestand nur
> 58,45 %, bei `WOC` aber **95,21 %**. Die 95-Prozent-Regel ergibt über den Mandantenschnitt
> **vierzehn** kuratierte Paare aus **45** gemessenen. **Zwei der vierzehn können nachweislich nicht
> wirken**, weil bei ihnen kein Wert *mit* führender Null auf der Sollänge vorkommt. Und führende
> Leerzeichen tragen über den Bestand nur **zwei** Typen: 9018 mit 1,349624 % und 9020 mit einer
> einzigen Zeile.

## M46‑0 Rahmen

Unverändert aus §0 übernommen, am **13.08.2026** in eigenen Sitzungen erhoben. Jeder Aufruf des
Clients ist eine neue Sitzung; **`SELECT @@global.read_only` steht deshalb in jedem Skript als erste
Abfrage** und lieferte jedes Mal **`1`**. `SET max_statement_time = 60` vor dem ersten Statement,
Laufzeit serverseitig über `SET profiling = 1` / `SHOW PROFILES`, Zugangsdaten ausschließlich aus
`OVERLORD_DB_*` (Regel G1). Serverzeit zu Beginn `2026-08-13 11:42:46` (`UTC_TIMESTAMP` `09:42:46`).

**Die Testkopie ist unverändert — der sechste Messtag in Folge, byteidentisch:**

| | `DATA_LENGTH` | `INDEX_LENGTH` |
|---|---:|---:|
| `Message` | 740.851.712 | 2.157.330.432 |
| `MessageAction` | 2.226.634.752 | 819.855.360 |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 |

### Der Spaltentyp — erhoben, nicht übernommen

Die Migration braucht den Typ des BAM-Typs. Er ist gegen `information_schema` geprüft worden und
nicht aus `datenmodell.md` abgeschrieben (Regel L8):

| Tabelle | Spalte | `COLUMN_TYPE` | `NULL`? |
|---|---|---|---|
| `MessageBAM` | `MessageBAMType` | **`smallint(6)`** | NO |
| `MessageBAMType` | `MessageBAMType` | **`smallint(6)`** | NO |

### Eine PAD-SPACE-Falle, die M43‑3 noch nicht hatte

M46‑3 fragt nach dem **führenden** Leerzeichen. Die naheliegende Form `LEFT(v, 1) = ' '` ist
**falsch**, und das ist gemessen und nicht überlegt:

```sql
SELECT '' = ' ', '' LIKE ' %', ' a' LIKE ' %', LEFT('', 1) = ' ';
```

| Ausdruck | Ergebnis | Bedeutung |
|---|---:|---|
| `'' = ' '` | **1** | PAD SPACE: der Leerstring ist dem Leerzeichen **gleich** |
| **`LEFT('', 1) = ' '`** | **1** | **Ein leerer Wert zählte als „führendes Leerzeichen"** |
| `'' LIKE ' %'` | **0** | `LIKE` polstert nicht — dieselbe Regel wie in M43‑3 |
| `' a' LIKE ' %'` | 1 | und trifft, was es treffen soll |

M46‑3 misst deshalb über `LIKE ' %'`. **Praktisch wäre es hier folgenlos geblieben** — die Erhebung
zählt zugleich die Leerstrings und findet über alle 55 Typen **null**. Der Beleg steht trotzdem hier:
Er ist der Grund für die Form, und ohne ihn stünde sie als Geschmacksfrage da.

## M46‑1 Die Dominanz über den **Bestand**, nicht über einen Monat

**Ohne Zeitfenster, und das ist begründungspflichtig (Regel L9).** Dieselbe Begründung wie bei
M43‑1: Ein Fenster kann die Frage „ist die Länge dieses Typs stabil" grundsätzlich nicht
beantworten, weil es genau die Zeiträume ausblendet, in denen sie sich geändert haben könnte. M43
hat das nicht als Risiko, sondern als Messwert gezeigt (Typ 2001).

```sql
SELECT r.typ, SUM(r.zeilen) AS zeilen,
       ROUND(100 * SUM(r.zeilen_fn) / SUM(r.zeilen), 2) AS f_null_pz,
       COUNT(*) AS laengen,
       MAX(CASE WHEN r.rn = 1 THEN r.laenge END) AS haeufigste,
       ROUND(100 * MAX(CASE WHEN r.rn = 1 THEN r.zeilen END) / SUM(r.zeilen), 2) AS dominanz_pz
FROM (SELECT g.*, ROW_NUMBER() OVER (PARTITION BY g.typ ORDER BY g.zeilen DESC, g.laenge) AS rn
      FROM (SELECT MessageBAMType AS typ, CHAR_LENGTH(MessageBAMValue) AS laenge,
                   COUNT(*) AS zeilen, SUM(LEFT(MessageBAMValue,1) = '0') AS zeilen_fn
            FROM MessageBAM GROUP BY 1,2) g) r
GROUP BY r.typ ORDER BY dominanz_pz DESC, r.typ;
```

**`EXPLAIN`** — Vollscan über einen deckenden Index, `Using index`. Der Optimierer wählt `PRIMARY`;
der erzwungene `MessageBAM_BAMValue` ist **4,4 % schneller** und damit im Rauschen. **Regel L15 ist
damit geprüft und nicht angenommen:** Hier wählt der Optimierer nicht falsch.

| Fassung | key | rows | Extra | Laufzeit |
|---|---|---:|---|---:|
| Optimiererwahl | `PRIMARY` | 10.859.666 | `Using index; Using temporary; Using filesort` | 12,238 s |
| `FORCE INDEX (MessageBAM_BAMValue)` | `MessageBAM_BAMValue` | 10.859.666 | `Using index; Using temporary; Using filesort` | **11,703 s** |

> `rows` steht auf **10.859.666** — der `information_schema`-Schätzung. Gezählt hat die Tabelle
> **15.406.350** (M33‑0). Dieselbe Warnung wie in M32 und M42‑1: Eine `rows`-Angabe trägt kein
> Vorzeichen.

Die Grundaggregation liefert **480** Gruppen aus `Typ × führende Null × Länge` über **55** Typen.
Die Verdichtung entsteht mit `ROW_NUMBER()` und nicht von Hand, wie in M43‑1.

### Ergebnis — alle 55 Typen des Bestands, nach Dominanz sortiert

„Dominanz" ist der Anteil der häufigsten Länge an **allen** Zeilen des Typs, „f. Null" der Anteil
der Werte mit führender Null. Die Trennlinie bei 95 % steht in der Tabelle.

| Typ | Beschreibung | Zeilen | f. Null | Längen | häufigste | **Dominanz** |
|---:|---|---:|---:|---:|---:|---:|
| 1002 | PONumber | 84 | 0 % | 1 | 9 | **100,00 %** |
| 1003 | TradingPartnerID | 4 | 0 % | 1 | 2 | **100,00 %** |
| **2002** | InvoiceNumber VTG | 456 | **100 %** | 1 | 10 | **100,00 %** |
| 2003 | OrderLoadNo | 15 | 0 % | 1 | 10 | **100,00 %** |
| 2004 | YourReference | 1 | 0 % | 1 | 38 | **100,00 %** |
| **2005** | CustRef1 | 9 | **100 %** | 1 | 10 | **100,00 %** |
| 2006 | CustRef2 | 5 | 0 % | 1 | 10 | **100,00 %** |
| **2007** | LoadNo | 9 | **100 %** | 1 | 10 | **100,00 %** |
| 2008 | Tanknummer | 108 | 0 % | 1 | 11 | **100,00 %** |
| 9002 | Lieferplannummer_L_SAP | 793.588 | 0 % | 1 | 10 | **100,00 %** |
| **9009** | Beleg-Nr. GS_L_SAP | 17.913 | 34,35 % | 1 | 10 | **100,00 %** |
| 9010 | Materialbeleg (Entnahme)_L_SAP | 26.044 | 0 % | 1 | 10 | **100,00 %** |
| **9011** | Anlieferungs-Nr. ae_L_SAP | 1.412 | **100 %** | 1 | 10 | **100,00 %** |
| **9012** | Charge_L_SAP | 17.934 | **100 %** | 1 | 10 | **100,00 %** |
| **9013** | Nr. TSL_L_SAP | 6.148 | 9,48 % | 1 | 10 | **100,00 %** |
| **9024** | Rechnungsnummer_K_SAP | 25.359 | **100 %** | 1 | 10 | **100,00 %** |
| 9039 | Daten-Sender-Nummer_L_SAP | 33.347 | 0 % | 3 | 6 | 99,98 % |
| 9034 | Bestellnummer_L_SAP | 135.074 | 0 % | 12 | 10 | 99,80 % |
| 2 | Lieferscheinnummer | 62.079 | 0 % | 4 | 7 | 99,40 % |
| **9021** | Transportnummer_K_SAP | 55.250 | 94,67 % | 2 | 10 | 99,24 % |
| **9036** | Lagerort Kunde_L_SAP | 22.847 | 98,94 % | 2 | 4 | 99,23 % |
| **1** | Auftragsnummer | 178.773 | 0,00 % *(1 Zeile)* | 17 | 7 | 99,03 % |
| 3 | Rechnungsnummer | 2.268.697 | 0 % | 2 | 8 | 97,96 % |
| 1001 | PartNumber | 82 | 0 % | 2 | 8 | 97,56 % |
| **9000** | Abladestelle_L_SAP | 151.063 | 32,05 % | 5 | 3 | 96,68 % |
| **2000** | OrderNumber | 12.434 | 1,41 % | 8 | 6 | 96,25 % |
| 9027 | Bestellnummer_FORS | 215.117 | 0 % | 2 | 9 | 95,53 % |
| | | | | | | *— Schwelle 95 % —* |
| 9028 | Material-Nr. beim Kunden_FORS | 215.116 | 0,21 % | 5 | 13 | 94,52 % |
| 9029 | Material-Nr. beim Lieferanten_FORS | 215.116 | 0,21 % | 5 | 13 | 94,52 % |
| 9004 | Unsere Material-Nr._L_SAP | 1.081.129 | 5,71 % | 9 | 8 | 94,29 % |
| **9006** | Lieferschein-Nr._L_SAP *(kuratiert)* | 155.809 | 32,99 % | 2 | 8 | **94,21 %** |
| 9008 | Beleg-Nr. TSL_L_SAP | 6.164 | 0 % | 2 | 10 | 90,48 % |
| 9007 | Transport-Nummer_L_SAP | 40.603 | 48,43 % | 13 | 8 | 83,52 % |
| 9020 | Lieferschein, Entnahme, PUS_K_SAP | 998.686 | 18,10 % | 15 | 8 | 82,31 % |
| 9005 | Werk_L_SAP | 162.893 | 0,06 % | 4 | 4 | 82,12 % |
| 9032 | Sendercode_K_SAP | 157.848 | 1,75 % | 11 | 10 | 81,14 % |
| 9023 | Übertragungsnummer Gutschrift_K_SAP | 127.959 | 0,81 % | 9 | 6 | 79,95 % |
| **2001** | VendorReference | 11.191 | **67,21 %** | 3 | 14 | **79,12 %** |
| 9033 | Empfaengercode_K_SAP | 157.848 | 3,38 % | 14 | 25 | 79,03 % |
| **9001** | Abrufnummer_L_SAP *(kuratiert)* | 465.143 | 0 % | 4 | 3 | 72,06 % |
| 1004 | ShippingAdress | 6 | 0 % | 2 | 6 | 66,67 % |
| 0 | Bestellnummer | 167.463 | 1,40 % | 16 | 10 | 66,59 % |
| 9037 | Packmittelnummer Kunde_L_SAP | 41.562 | 6,75 % | 22 | 8 | 64,96 % |
| 9015 | Kundenwerk_K_SAP | 435.690 | 25,34 % | 10 | 3 | 58,51 % |
| 9014 | Lieferantennummer beim Kunden_K_SAP | 432.227 | 8,37 % | 9 | 8 | **58,45 %** |
| 9030 | Sender_Ident_FORS | 147 | 0 % | 2 | 8 | 55,78 % |
| 9031 | Empf_Ident_FORS | 147 | 0 % | 2 | 12 | 55,78 % |
| 9025 | Abladestelle_FORS | 295 | 50,51 % | 2 | 3 | 50,51 % |
| 9019 | Bestellnummer vom Kunden_K_SAP | 1.852.237 | 1,35 % | 17 | 9 | 49,87 % |
| 9022 | Gutschriftsanzeigen-Nummer_K_SAP | 135.240 | 0,54 % | 6 | 10 | 43,67 % |
| 9018 | Kundenmaterialnummer_K_SAP | 2.311.236 | 1,71 % | 20 | 13 | 43,61 % |
| 9017 | (JIT-) Abrufnummer_K_SAP | 666.806 | 28,09 % | 17 | 3 | 42,15 % |
| 9016 | Abladestelle_K_SAP | 412.886 | 2,61 % | 12 | 2 | 27,29 % |
| 9038 | Packmittelnummer Lieferant_L_SAP | 59.802 | 22,55 % | 21 | 8 | 24,07 % |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 1.071.249 | 3,89 % | 35 | 9 | 17,95 % |

**Der Bestand kennt 55 Typen, Fenster B kannte 46** (M38). Neu sind 1001 bis 1004 und 2003 bis 2008
— durchweg kleine Typen, die im Monat nicht vorkamen. **Die Kontrolle gegen M43 ist bestanden:** Die
sechs dort über den Bestand gemessenen Typen reproduzieren Zeile für Zeile, 2001 mit **67,21 %** und
9036 mit **98,94 %** führender Null.

### Warum die Entscheidung an der Dominanz über **alle** Werte hängt

Der Auftrag verlangt die Erhebung „getrennt nach Werten mit und ohne führende Null" und entscheidet
über „eine dominante Länge ≥ 95 %". Beides ist erhoben; die **Entscheidung** fällt an der Dominanz
über alle Werte des Paares, und das ist eine Festlegung, die hier offen steht:

- Die Anwendung füllt eine **Eingabe** auf die Sollänge auf und weiß dabei nicht, ob der gesuchte
  Wert im Bestand mit oder ohne Null steht. Maßgeblich ist deshalb, ob die Sollänge für den **ganzen
  Typ** gilt.
- Die Trennung nach Schreibweise steht in der Tabelle unten und in M46‑1c; wer die Regel anders
  schneiden will, findet dort die Zahlen dafür.
- **Die drei Lesarten fallen auseinander**, und zwar nicht selten: 9020 hat über alle Werte 82,31 %,
  in der Gruppe **ohne** Null aber 98,78 %; 9015 hat 58,51 % gegen 97,56 % in der Gruppe **mit**
  Null. Eine Kuratierung nach der Gruppendominanz ergäbe andere Einträge. Das ist keine
  Feinheit — es ist der Unterschied zwischen 14 und deutlich mehr Zeilen.

### Ergebnis — dieselbe Erhebung, getrennt nach Schreibweise (Auszug)

Vollständig für alle 55 Typen erhoben; abgedruckt sind die Typen, an denen die Entscheidung hängt.

| Typ | **ohne** führende Null | | | **mit** führender Null | | |
|---:|---:|---:|---|---:|---:|---|
| | Zeilen | Längen | häufigste | Zeilen | Längen | häufigste |
| 1 | 178.772 | 17 | 7 (99,03 %) | **1** | 1 | **8 (100 %)** |
| 2000 | 12.259 | 7 | 6 (97,63 %) | 175 | 3 | **12 (78,86 %)** |
| 2001 | 3.670 | 3 | 10 (63,46 %) | 7.521 | 1 | 14 (100 %) |
| 2002 | — | — | — | 456 | 1 | 10 (100 %) |
| 9000 | 102.641 | 4 | 3 (95,17 %) | 48.422 | 2 | 3 (99,87 %) |
| 9006 | 104.401 | 2 | 8 (96,16 %) | 51.408 | 2 | 8 (90,23 %) |
| 9014 | 396.068 | 9 | 8 (63,71 %) | 36.159 | 5 | 10 (58,82 %) |
| 9015 | 325.287 | 8 | 3 (45,26 %) | 110.403 | 8 | **3 (97,56 %)** |
| 9020 | 817.934 | 14 | **8 (98,78 %)** | 180.752 | 6 | 10 (92,13 %) |
| 9021 | 2.943 | 2 | 10 (85,73 %) | 52.307 | 1 | 10 (100 %) |
| 9036 | 243 | 2 | 3 (72,84 %) | 22.604 | 1 | 4 (100 %) |

## M46‑1c Kommt bei der Sollänge überhaupt ein Wert **mit** führender Null vor?

**Diese Frage stellt der Auftrag nicht, und sie entscheidet trotzdem, ob ein Eintrag wirken kann.**
Die Sollänge ist die Länge, auf die aufgefüllt wird. Liegt im Bestand kein Wert **mit** führender
Null auf dieser Länge, findet die aufgefüllte Fassung nichts — der Eintrag kostet dann eine
Suchvariante ohne Gegenwert.

Typgebunden über `MessageBAM_BAMValue` als `range`, **0,177 s**:

```sql
SELECT MessageBAMType AS typ, CHAR_LENGTH(MessageBAMValue) AS laenge, COUNT(*) AS zeilen
FROM MessageBAM
WHERE MessageBAMType IN (…die Kandidaten…) AND MessageBAMValue LIKE '0%'
GROUP BY 1, 2 ORDER BY 1, 3 DESC, 2;
```

| Typ | Sollänge | Längen **mit** führender Null (Zeilen) | trifft die Sollänge |
|---:|---:|---|---|
| **1** | 7 | 8 (1) | **nein** |
| **2000** | 6 | 12 (138), 8 (30), 10 (7) | **nein** |
| 2002 | 10 | 10 (456) | ja |
| 2005 | 10 | 10 (9) | ja |
| 2007 | 10 | 10 (9) | ja |
| 9000 | 3 | 3 (48.360), 1 (62) | ja |
| 9009 | 10 | 10 (6.153) | ja |
| 9011 | 10 | 10 (1.412) | ja |
| 9012 | 10 | 10 (17.934) | ja |
| 9013 | 10 | 10 (583) | ja |
| 9021 | 10 | 10 (52.307) | ja |
| 9024 | 10 | 10 (25.359) | ja |
| 9036 | 4 | 4 (22.604) | ja |

**Bei Typ 1 und Typ 2000 zeigt die Sollänge ins Leere.** Typ 1 trägt im ganzen Bestand **eine
einzige** Zeile mit führender Null, und die ist acht statt sieben Zeichen lang. Bei 2000 liegen die
175 Zeilen mit Null auf den Längen 12, 8 und 10 — die dominante Länge 6 ist nicht darunter.

## M46‑2 Ist die Sollänge je Mandant dieselbe?

**Die Frage, um die es geht.** Es liegt nahe, die Sollänge als Eigenschaft des Typs zu behandeln;
dieselbe Annahme ist in diesem Projekt schon einmal für eine kuratierte Eigenschaft getroffen und
später korrigiert worden.

Fensterlos, aus demselben Grund wie M46‑1. Einstieg über `MessageBAM_BAMValue` als `range`, dann die
Kette über `eq_ref` hinauf:

| id | table | type | key | rows | Extra |
|---|---|---|---|---:|---|
| 1 | `b` | `range` | `MessageBAM_BAMValue` | 1.036.196 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | `m` | `eq_ref` | `PRIMARY` | 1 | `Using where` |
| 1 | `p` | `eq_ref` | `PRIMARY` | 1 | `Using where` |
| 1 | `pm` | `ref` | `PRIMARY` | 1 | `Using index` |

**Erhoben für alle 36 Typen, die im Bestand überhaupt einen Wert mit führender Null tragen** — rund
11,4 Millionen Zeilen, **in fünf Stapeln**, weil ein Lauf über alle 36 die 60-Sekunden-Grenze reißt.
Das Ergebnis sind **45 Paare** aus Mandant und Typ. **Regel L7 ist damit übererfüllt:** Die Erhebung
umfasst jeden Mandanten, der einen solchen Typ trägt, `WOC` mit 2.067 Zeilen eingeschlossen.

### Ergebnis — die Paare, an denen die Antwort hängt

| Typ | Mandant | Zeilen | Längen | häufigste | Dominanz | f. Null |
|---:|---|---:|---:|---:|---:|---:|
| **2000** | **`SUTTONS`** | 12.296 | 8 | **6** | **97,33 %** | 1,24 % |
| **2000** | **`VOTG`** | 138 | 2 | **7** | 84,06 % | 15,94 % |
| 1 | `IBIS` | 155.875 | 17 | 7 | 99,00 % | 0,00 % |
| 1 | `IBISGUS` | 22.898 | 4 | 7 | 99,20 % | 0,00 % |
| 2001 | `SUTTONS` | 11.183 | 2 | 14 | 79,17 % | 67,25 % |
| 2001 | `VOTG` | 8 | 1 | 7 | 100,00 % | **0 %** |
| **9014** | **`NEXANS`** | 430.160 | 9 | 8 | 58,73 % | 8,03 % |
| **9014** | **`WOC`** | 2.067 | 2 | **6** | **95,21 %** | 79,15 % |
| 0 | `IBIS` | 137.898 | 16 | 10 | 66,58 % | 1,44 % |
| 0 | `IBISGUS` | 29.565 | 6 | 10 | 66,63 % | 1,23 % |

Die übrigen 35 Paare gehören zu Typen, die nur **ein** Mandant trägt; dort ist die Zahl je Mandant
identisch mit der über den Bestand und steht in M46‑1.

**Die Antwort ist: nein — und sie hängt nicht an einem einzelnen Ausreißer.**

- **Typ 2000 weicht in der Länge ab.** `SUTTONS` dominiert mit Länge **6** bei 97,33 %, `VOTG` mit
  Länge **7** bei 84,06 %. Eine typweite Sollänge von 6 wäre für `VOTG` schlicht falsch.
- **Typ 9014 weicht in der Entscheidung ab.** Über den Bestand kommt er auf 58,45 % und fiele durch.
  Bei `WOC` erreicht er **95,21 %** und trägt dort auf **79,15 %** der Zeilen eine führende Null.
  Ein typweiter Schnitt hätte den Eintrag verworfen, der für diesen Mandanten der nützlichste ist.
- **Typ 2001 zeigt es von der anderen Seite.** Bei `VOTG` hat er 100 % Dominanz — und **null** Werte
  mit führender Null. Der Mandantenschnitt schließt ihn dort aus, ein typweiter hätte über die
  Gesamtdominanz von 79,12 % entschieden und die 8 Zeilen nie gesehen.

**Damit ist die Kuratierung nach `(mandant_id, bam_typ)` zu schlüsseln.** Das ist der teurere Schnitt
— und der einzige, der die drei Fälle richtig trifft.

### M46‑2c Wirksamkeit je Paar

Dieselbe Frage wie M46‑1c, aber je Mandant (**3,033 s**). Für `WOC`/9014 liegen alle 1.636 Zeilen mit
führender Null auf Länge **6** — die Sollänge trifft. Für `IBIS`/1 und `SUTTONS`/2000 bleibt es beim
Befund aus M46‑1c: Die Sollänge trifft **keinen** Wert mit führender Null. `IBISGUS`/1 trägt gar
keinen und ist damit kein Kandidat.

## M46‑3 Führende Leerzeichen, getrennt von folgenden

M43‑3 hat für Fenster B belegt, dass die 25,88 % Randleerzeichen bei 9018 zu 24,83 Prozentpunkten
**folgende** sind — unter PAD SPACE beim `=`-Vergleich folgenlos — und zu 1,05 Prozentpunkten
**führende**. M46‑3 holt die führenden über den **Bestand** und für **alle** Typen nach.

**Die erste Fassung hat die 60-Sekunden-Grenze gerissen** und ist serverseitig abgebrochen worden
(`ERROR 1969: Query execution was interrupted`). Sie trug einen `LEFT JOIN` auf `MessageBAMType`,
gruppierte über die Beschreibung und rechnete zusätzlich `TRIM(TRAILING …)` je Zeile. Die schlanke
Fassung ohne Join und ohne `TRIM` kostet **9,558 s**:

```sql
SELECT MessageBAMType AS typ, COUNT(*) AS zeilen,
       SUM(MessageBAMValue LIKE ' %') AS fuehrend,
       SUM(CHAR_LENGTH(MessageBAMValue) = 0) AS leerstring
FROM MessageBAM GROUP BY 1 HAVING fuehrend > 0 OR leerstring > 0;
```

### Ergebnis — über den Bestand tragen **zwei** Typen ein führendes Leerzeichen

| Typ | Beschreibung | Zeilen | **führend** | Anteil | Leerstrings |
|---:|---|---:|---:|---:|---:|
| **9018** | Kundenmaterialnummer_K_SAP | 2.311.236 | **31.193** | **1,349624 %** | 0 |
| **9020** | Lieferschein, Entnahme, PUS_K_SAP | 998.686 | **1** | 0,000100 % | 0 |

Über alle 55 Typen: **kein einziger Leerstring**. Beide Typen gehören ausschließlich `NEXANS` — ihre
Zeilenzahl je Mandant aus M46‑2 ist identisch mit der über den Bestand, also kann keine der Zeilen
einem anderen Mandanten gehören.

**Der Bestand liegt über dem Monat, und das ist kein Widerspruch zu M43‑3.** Dort sind es **1,05 %**
über Fenster B, hier **1,349624 %** über den Bestand — dieselbe Größenordnung, andere Grundmenge.
Bemerkenswerter ist, was **verschwindet**: 9032 und 9033 tragen über Fenster B je 2,15 %
Randleerzeichen, davon nach M43‑3 **0,00 % führende**. Über den Bestand bestätigt sich das —
sie stehen nicht in dieser Tabelle. Die 2,15 % sind vollständig folgende und damit unter PAD SPACE
beim `=`-Vergleich unsichtbar.

> **Die Anteile sind zweimal gemessen worden, und die erste Zahl war falsch.** Die naheliegende Form
> `ROUND(100 * AVG(v LIKE ' %'), 4)` liefert für 9018 **1,3500 %** und für 9020 **0,0000 %**. Ursache
> ist `@@div_precision_increment = 4`: `AVG` über einen Wahrheitswert rechnet dezimal und wird auf
> vier Nachkommastellen gekürzt, **bevor** mit 100 multipliziert wird. Der Beleg steht daneben —
> `AVG(x)` liefert `0.3333`, wo `100.0 * SUM(x) / COUNT(*)` auf `33,333333` kommt. Gemessen und
> abgedruckt sind die Zahlen aus der Summenform: **1,349624 %** und **0,000100 %**. Die *Zählwerte*
> waren in beiden Fassungen dieselben und richtig. **Für M46‑1 und M46‑2 ist die Falle folgenlos**,
> weil dort durchgängig `100 * SUM(…) / SUM(…)` steht und keine der Spalten über `AVG` entsteht.

## Was daraus folgt

Die Zeilen dieser Tabelle standen — bis auf die Spalte „trifft zu" — **vor** der Erhebung fest; sie
sind aus dem Auftrag zu Schritt 7, Teil 2a übernommen.

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Ein Typ hat über den Bestand eine dominante Länge ≥ **95 %** → er bekommt eine Sollänge | **ja, bei 13 von 36 Typen mit führender Null** | Über den Mandantenschnitt sind es **14 Paare**. Die Schwelle wirkt wie beabsichtigt: 2001 fällt mit 79,12 % durch, und die Zahl aus dem 2001-Fall (67,21 % führende Null) liegt noch tiefer |
| Ein Typ liegt darunter → **keine Sollänge** | **ja, bei 23 von 36** | Für sie wird nicht aufgefüllt. Der Auftrag nennt das keinen Mangel — und die Messung stützt das nur zum Teil, siehe den Absatz zu 9006 unten |
| Die dominante Länge ist je Mandant **gleich** → Schlüssel `bam_typ` | **nein** | — |
| Sie **unterscheidet sich** je Mandant → Schlüssel `(mandant_id, bam_typ)` | **ja, an drei unabhängigen Stellen** | 2000 (Länge 6 gegen 7), 9014 (58,45 % gegen 95,21 %), 2001 (`VOTG` ohne eine einzige führende Null). **Der teurere Schnitt ist der richtige** |
| Führende Leerzeichen treten bei **mehreren** Typen auf → auch sie gehören kuratiert | **ja, bei zweien — und der zweite ist eine einzige Zeile** | 9018 mit 31.193 von 2.311.236, 9020 mit **1** von 998.686. Die Kuratierung führt beide; dass der zweite praktisch nie wirkt, steht daneben |

**Wo die vorformulierte Zeile nicht passt — die Regel kennt die Wirksamkeit nicht.** Sie fragt nach
der Längendominanz und trifft damit zwei Paare, bei denen kein Wert **mit** führender Null auf der
Sollänge liegt: `IBIS`/1 (eine Zeile, Länge 8 statt 7) und `SUTTONS`/2000 (175 Zeilen auf den Längen
12, 8 und 10 statt 6). Beide Einträge sind mechanisch angelegt worden, weil die Befüllung mechanisch
ist; **ob die Regel um die Bedingung „bei der Sollänge kommt eine führende Null vor" ergänzt wird,
ist eine Entscheidung und wird hier nicht getroffen.**

**Und ein zweiter Befund ohne vorformulierte Zeile: die Regel schließt ausgerechnet 9006 aus.** Die
kuratierte Lieferschein-Nr. liegt über den Bestand bei **94,21 %** — 0,79 Prozentpunkte unter der
Schwelle. Ausgerechnet für sie hat M43‑4 gezeigt, dass das Auffüllen trägt: roh 1.642 Treffer,
aufgefüllt **4**. Und ausgerechnet sie trägt auf 32,99 % ihrer Werte eine führende Null. **Das ist
der teuerste Einzelfall dieser Regel**, und er ist keine Panne, sondern ihr Preis: Eine Schwelle,
die 2001 fängt, fängt auch 9006. Wer sie auf 94 % senkte, nähme 9028, 9029 und 9004 mit — und die
tragen 0,21 %, 0,21 % und 5,71 % führende Nullen.

**Ein dritter: der Bestand kennt neun Typen mehr als Fenster B**, und alle neun sind winzig (1 bis
108 Zeilen). Drei davon — 2005, 2007 und 2002 — tragen auf **100 %** ihrer Werte eine führende Null
und bekommen eine Sollänge. Eine Kuratierung aus Fenster B hätte sie nicht gekannt.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Längenverteilung je Typ über den **gesamten Bestand** (n = 15.406.350 Zeilen,
> 55 Typen, 480 Gruppen) und je **(Mandant, Typ)** für die 36 Typen mit führender Null (n ≈ 11,4
> Mio. Zeilen, 45 Paare). Der Anteil führender Leerzeichen über den Bestand für alle 55 Typen.
> Die Wirksamkeitsprüfung M46‑1c/2c steht auf den 211.062 Zeilen mit führender Null der
> Kandidatentypen.
>
> *Behauptet wird:* dass daraus eine **Kuratierungsregel für die Zukunft** wird — eine Sollänge je
> Mandant und Typ, die die Suche dauerhaft auffüllt.
>
> **Die Lücke, und sie ist dieselbe wie bei M43 — nur größer geworden.** Ein Typ kann seine Gestalt
> ändern, ohne dass es jemand merkt, und die Suche findet dann still weniger. M43 konnte das für 40
> Typen nicht sagen; M46 hat den Bestand nun für **alle** erhoben und die Lücke damit von „welche
> Typen" auf „welcher Zeitpunkt" verschoben: **Gemessen ist der Stand der Testkopie vom 08.07.2026.
> Ob eine Sollänge morgen noch gilt, sagt diese Runde nicht** — sie sagt nur, dass es an einem Typ
> (2001) und an einem Mandantenpaar (2000) schon einmal auseinandergelaufen ist. Genau dagegen ist
> `BamSollaengeDriftDbIT` gebaut, und dessen Garantiestufe ist ausdrücklich begrenzt: Er läuft nicht
> in der CI. **Nicht gemessen ist außerdem, ob die Produktion dieselbe Verteilung trägt** — es
> besteht kein Zugang zu ihr, dieselbe benannte Lücke wie im Belegvermerk zu M44.
>
> **Und eine zweite Lücke, die diese Runde neu aufmacht:** Die Entscheidung fällt an der Dominanz
> über **alle** Werte eines Paares. Ob das die richtige der drei möglichen Lesarten ist, ist eine
> Festlegung und **keine Messung** — die Zahlen für die beiden anderen stehen daneben, und sie
> ergäben eine andere Kuratierung.

---

# M47 — Was kostet das **gebaute** Suchstatement?

*Fünfter Nachtrag, 13.08.2026. Schritt 7, Teil 2b — die Messung vor dem Merge (Regel L7, §8 Regel 7).*

**Frage.** Alle bisherigen Zahlen dieser Runde stammen von Statements, die den späteren Endpunkt
*nachbilden*. M47 misst den, der gebaut ist — mit Normalisierung, mit Deckelung, mit Anzeigespalten
und mit dem Zeitfenster, das M35 begründet hat.

> **Gemessen wird der Text, den jOOQ tatsächlich schickt**, nicht eine nachgebaute Fassung. Er ist
> aus dem Repository gegen eine jOOQ-Attrappe **gerendert** und mit Sitzungsvariablen statt der
> Prüfwerte in die Messsitzung übernommen (Regel G1). Damit unterscheidet sich der gemessene
> Buchstabe an keiner Stelle vom ausgelieferten.

> **Das Ergebnis vorweg, in vier Sätzen.** Das 30-Tage-Fenster fängt **beide** offenen Bösfälle: K3b
> fällt von 5.275 auf **882 ms**, K5c von 926,7 auf **6,9 ms** — offene Frage 10 ist damit
> beantwortet. Der Optimierer steigt auch im gebauten Statement **in jeder** Konstellation über den
> seltensten Begriff ein, selbst wenn er an fünfter Stelle steht. Die Normalisierung kostet
> **Zehntelmillisekunden**: von einer auf drei Fassungen 0,956 → 1,221 ms, und bei `NEXANS` sind
> **fünf** Fassungen die Obergrenze überhaupt. Und der Befund, der nicht im Plan stand: **Die
> Anzeigespalten unter statt über der Deckelung kosten den Faktor 1,48 — bei identisch gutem
> `EXPLAIN`**, genau wie in Teil 1.

## M47‑0 Rahmen

Unverändert aus §0 übernommen, am **13.08.2026** in fünf eigenen Sitzungen erhoben. Jeder Aufruf des
Clients ist eine neue Sitzung; **`SELECT @@global.read_only` steht deshalb in jedem Skript als erste
Abfrage** und lieferte jedes Mal **`1`**. `SET max_statement_time = 60` vor dem ersten Statement,
`SET profiling_history_size = 100`, Laufzeit serverseitig über `SET profiling = 1` / `SHOW PROFILES`,
Zugangsdaten ausschließlich aus `OVERLORD_DB_*`. Serverzeit zu Beginn `2026-08-13 13:27:35`
(`UTC_TIMESTAMP` `11:27:35`).

**Die Testkopie ist unverändert — der siebte Messtag in Folge, byteidentisch:**

| | `DATA_LENGTH` | `INDEX_LENGTH` |
|---|---:|---:|
| `Message` | 740.851.712 | 2.157.330.432 |
| `MessageAction` | 2.226.634.752 | 819.855.360 |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 |

### `@@sql_mode` — erhoben, weil das Statement daran hängt

```
STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION
```

**Kein `ONLY_FULL_GROUP_BY`.** Das `GROUP BY m.MessageID` neben den übrigen `Message`-Spalten ist
damit zulässig — dieselbe Form, die schon M34, M35 und M42 gemessen haben. §0 nennt die Einstellung
seit der Hauptrunde; sie steht hier ein zweites Mal, weil sie ab jetzt in **Anwendungscode** steht
und nicht nur in einer Erhebung.

### Wiederholungen

Beste von fünf nach einem Aufwärmlauf; bei Statements über **einer** Sekunde beste von drei nach
einem Aufwärmlauf. Die sechs teuren Fälle sind zusätzlich in einer eigenen Sitzung mit vier Läufen
wiederholt worden, nachdem die erste Runde dort nur zwei Messläufe ergab. **Beide Runden stimmen bis
auf 1,2 Prozent überein** (1.661,99 gegen 1.655,83 ms; 8.992,56 gegen 8.939,75; 872,11 gegen 881,98;
4.158,18 gegen 4.202,70; 929,66 gegen 935,05; 1,771 gegen 1,834) — abgedruckt ist jeweils die
Dreierfassung.

## M47‑1 Die Prüfwerte

**Alle Prüfwerte werden je Sitzung neu hergeleitet**, nie übertragen (dieselbe Methodik wie M42‑0).
Abgedruckt sind ausschließlich ihre **Eigenschaften**.

### Die Ankernachricht (Herleitung wie M42‑0)

`NEXANS`-Wurzel in Fenster A mit genau neun BAM-Werten, kleinste `MessageID`. Sie liefert dieselben
neun Werte wie in M42 — die Kontrolle geht auf, Zeichen für Zeichen der Trefferzahlen:

| Rolle | Typ | Länge | globale Trefferzahl | in M42‑0 |
|---|---:|---:|---:|---:|
| selten | 9022 | 12 | **1** | 1 ✔ |
| der schlimmste | 9014 | 8 | **234.159** | 234.159 ✔ |
| sehr häufig a | 9032 | 10 | 100.343 | 100.343 ✔ |
| sehr häufig b | 9033 | 25 | 124.793 | 124.793 ✔ |
| mittel | 9018 | 10 | 540 | 540 ✔ |
| häufig | 9016 | 8 | 1.182 | 1.182 ✔ |

### Die fette Nachricht (Herleitung wie M42‑1 K5)

Das `NEXANS`-**Merge-Ergebnis** mit den meisten BAM-Werten in Fenster B. Auch hier reproduziert die
Herleitung M42 vollständig:

| | gemessen | in M42‑1 K5 |
|---|---:|---:|
| BAM-Werte auf der Nachricht | **3.409** | 3.409 ✔ |
| Prüfwert 9018, globale Trefferzahl | **719** | 719 ✔ |
| Prüfwert 9019, globale Trefferzahl | **654** | 654 ✔ |
| Kandidatennachrichten hinter 9018 | **654** | 654 ✔ |
| BAM-Werte auf diesen 654 Nachrichten | **1.634.605** | 1.634.605 ✔ |
| also je Nachricht | **2.499** | 2.499 ✔ |

### Der Wert für die Normalisierung — neu in dieser Runde

Hergeleitet aus einem **kuratierten Paar** von `NEXANS` mit 100 % Längendominanz und 100 % führender
Null (Sollänge 10): der kleinste Wert dieses Paares im Fenster, der mit einer Null beginnt und auf
der Sollänge liegt. Daraus die Fassungen, die die Anwendung bilden würde:

| Fassung | Länge | globale Trefferzahl |
|---|---:|---:|
| **der Kern** (Eingabe ohne die führenden Nullen) | 8 | **0** |
| auf die Sollänge aufgefüllt | 10 | **2** |
| mit führendem Leerzeichen | 9 | 0 |

**Das ist M43‑4 an einem eigenen Wert reproduziert:** Die rohe Fassung findet **nichts**, die
aufgefüllte die richtigen. Der Nutzer, der die Null nicht mittippt, ginge ohne Normalisierung leer
aus — nicht „mit weniger Treffern", sondern mit **keinem**.

Für den Fall mit den **meisten** Varianten zusätzlich die ersten zwei Zeichen desselben Kerns:

| Fassung | Länge | globale Trefferzahl |
|---|---:|---:|
| zwei Zeichen, roh | 2 | **2.256** |
| auf 3 aufgefüllt | 3 | 143 |
| auf 4 aufgefüllt | 4 | 0 |
| auf 10 aufgefüllt | 10 | 0 |
| mit führendem Leerzeichen | 3 | 0 |

### Die Kuratierung von `NEXANS`, gezählt

| | |
|---|---:|
| Zeilen in `bam_sollaenge` für `NEXANS` | **10** |
| davon mit einer Sollänge | **8** |
| **verschiedene** Sollängen | **3** (10, 4, 3) |
| Zeilen ohne Sollänge | 2 |
| Zeilen mit Leerzeichen-Kennzeichen | 2 |

### `IBIS` (Regel L7)

`IBIS`-Nachricht in Fenster B mit mindestens zwei BAM-Werten, kleinste `MessageID` — sie trägt drei
Werte; die beiden Prüfwerte haben **3** und **5** globale Treffer. Das entspricht M42‑4: Bei `IBIS`
ist die größte globale Trefferzahl über das Fenster **216**, der Bösfall existiert dort nicht.

## M47‑2 `EXPLAIN`

### Die Gestalt, die alle Fälle teilen

| id | select_type | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---:|---:|---|
| 1 | PRIMARY | `<derived2>` | `ALL` | — | — | 2 … 51 | `Using filesort` |
| 1 | PRIMARY | `Process` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 1 | PRIMARY | `Project` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 1 | PRIMARY | `SOS` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 1 | PRIMARY | `SOSAction` | `eq_ref` | `PRIMARY` | 148 | 1 | `Using where` |
| 2 | DERIVED | **`b1`** | **`ref`** | **`MessageBAM_BAMValueOnly`** | 282 | 1 … 443.830 | `Using where; Using index; Using temporary; Using filesort` |
| 2 | DERIVED | `Message` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 2 | DERIVED | `mandanten_process` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 2 | DERIVED | `ProjectMandant` | `eq_ref` | `PRIMARY` | 292 | 1 | `Using where; Using index` |

**Die vier Anzeigetabellen stehen in der äußeren Abfrage und laufen auf höchstens 51 Zeilen.** Das
ist die Gestalt, die §4 von [`bam-suche.md`](bam-suche.md) beschreibt, und sie ist im Plan sichtbar.

### Die führende Tabelle ist **immer** die mit dem selteneren Begriff — belegt, nicht angenommen

| Fall | führende Tabelle | `rows` | zweiter Begriff: `key` / `key_len` / `ref` / `rows` |
|---|---|---:|---|
| zwei Begriffe, seltener **zuerst** geschrieben | `b1` | **1** | `MessageBAM_BAMValueOnly` / **428** / `const, b1.MessageID` / **1** |
| zwei Begriffe, seltener **zuletzt** geschrieben | **`b2`** | **1** | dieselbe Gestalt — **der Plan ist identisch** |
| **fünf** Begriffe, seltener an **fünfter** Stelle | **`b5`** | **1** | vier × `MessageBAM_BAMValueOnly` / 428 / `const, b5.MessageID` / **1** |
| K3b (häufig × häufig) | `b1` | **177.506** | **`PRIMARY` / 146 / `MessageID` / `rows` 8** |
| K5c (fette Nachricht) | **`b2`** (654) | **654** | `MessageBAM_BAMValueOnly` / **428** / `const, b2.MessageID` / **1** |

**Regel L15 ist damit erfüllt und nicht bloß behauptet:** Der Optimierer ordnet die Selbstjoins
selbst um, und zwar auch dann, wenn der seltene Begriff im Statement an letzter Stelle steht. Genau
deshalb steht kein `STRAIGHT_JOIN` darin.

**Ein Befund gegenüber M42:** Bei **K5c kippt der Plan mit Zeitfenster *nicht* auf `PRIMARY`/`rows` 8.**
M42‑1 hat dort genau diesen Zugriff gemessen (und die 8 als um Faktor 312 zu klein entlarvt); mit dem
Fenster steht zwischen den beiden Begriffen ein `eq_ref` auf `Message`, und der zweite Begriff läuft
wieder als `ref` mit `key_len` 428 und `rows` 1. **Die Laufzeit folgt:** 926,7 ms ohne Fenster gegen
**6,9 ms** mit dreißig Tagen.

### Was die `IN`-Liste am Plan ändert — die offene Frage des Auftrags

| Varianten je Begriff | `type` auf `b1` | `key` | `key_len` | `rows` |
|---:|---|---|---:|---:|
| 1 | **`ref`** | `MessageBAM_BAMValueOnly` | 282 | 1 |
| 2 | **`range`** | `MessageBAM_BAMValueOnly` | 282 | 3 |
| 5 | **`range`** | `MessageBAM_BAMValueOnly` | 282 | 4.656 |

**Die Zugriffsart wechselt von `ref` auf `range`, die Indexwahl und die führende Tabelle bleiben.**
Die Frage, ob eine `IN`-Liste die Wahl der Einstiegstabelle beeinflusst, ist damit mit **nein**
beantwortet — und sie war offen und nicht rhetorisch: Ein `range` ist für den Optimierer teurer als
ein `ref`, und bei zwei Begriffen mit je mehreren Varianten hätte er umschwenken können.

### Die Typangabe

Mit `AND b1.MessageBAMType = ?` erscheint `MessageBAM_BAMValue` in `possible_keys` — **gewählt wird
trotzdem der reine Wertindex** (`MessageBAM_BAMValueOnly`, `range`, `rows` 3). Bei einer `IN`-Liste
über mehrere Werte ist der zusammengesetzte Index also nicht die bessere Wahl; M36 hatte ihn bei
**einem** Wert gewählt gesehen.

### Die zweite Abfrage (Trefferwerte)

| table | type | key | key_len | rows | Extra |
|---|---|---|---:|---:|---|
| `Message` | `range` | `PRIMARY` | 146 | 2 | `Using where; Using temporary; Using filesort` |
| **`MessageBAM`** | **`ref`** | **`MessageBAM_MessageFK`** | 146 | 4 | **`Using index`** |
| `ProjectMandant` | `ref` | `ProjectMandant_Mandant_idx` | 146 | 17 | `Using index` |
| `MessageBAMType` | `eq_ref` | `PRIMARY` | 2 | 1 | |
| `mandanten_process` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |

**Der Einstieg über die Kennungen ist `Using index`** — die Tabelle wird nicht angefasst. Gewählt ist
`MessageBAM_MessageFK` (`MessageID` allein) und nicht das Präfix des Primärschlüssels; für den Zugriff
ist das dasselbe, für die vorformulierte Erwartung nicht (unten).

### Der Vollabzug der Kuratierung

| table | type | key | key_len | rows | Extra |
|---|---|---|---:|---:|---|
| `bam_sollaenge` | `ref` | `PRIMARY` | 146 | **10** | `Using where` |

Zehn Zeilen über den Primärschlüssel-Präfix. **Kein Join gegen `GlassfishDB`** — das Statement nennt
das Quellschema nicht.

## M47‑3 Laufzeiten

Alle Werte in Millisekunden.

### Ein Begriff

| Fall | Treffer des Werts | **30 Tage** | **ein Jahr** |
|---|---:|---:|---:|
| typischer Wert | 1 | **1,095** | **1,089** |
| **der schlimmste Wert** | 234.159 | **1.655,827** | **8.939,745** |
| Kern ohne führende Null, 1 Fassung | 0 | 0,956 | — |
| Kern, **2 Fassungen** (roh + aufgefüllt) | 0 / 2 | 1,131 | — |
| Kern, **3 Fassungen** (+ Leerzeichen) | 0 / 2 / 0 | **1,221** | 1,263 |
| Kern, 2 Fassungen **mit Typangabe** | | 1,187 | — |
| zwei Zeichen, 1 Fassung | 2.256 | 20,228 | — |
| zwei Zeichen, **5 Fassungen** | 2.256 / 143 / 0 / 0 / 0 | **22,439** | — |

### Verundung

| Fall | **30 Tage** | **ein Jahr** | ohne Fenster (M42) |
|---|---:|---:|---:|
| **K2** selten × schlimmster Wert | **1,209** | — | 0,672 |
| K2, umgekehrte Reihenfolge | **1,202** | — | 0,710 |
| **K3b** häufig × häufig | **881,975** | 4.202,697 | **5.275,4** |
| **K5c** fette Nachricht | **6,911** | 935,046 | **926,7** |
| **fünf Begriffe** | **1,622** | 1,834 | 0,947 |
| fünf Begriffe, seltener zuletzt | **1,607** | — | 1,031 |

### `IBIS`, zweite Abfrage, Kuratierung

| Fall | 30 Tage |
|---|---:|
| `IBIS`, ein Begriff | **1,185** |
| `IBIS`, zwei Begriffe | **1,214** |
| die zweite Abfrage (Trefferwerte) | **0,650** |
| der Vollabzug der Kuratierung | **0,336** |

### Die Gegenform: Anzeigespalten neben statt über der Deckelung

Dieselbe Frage, dieselben Daten, der schlimmste Wert, 30 Tage — einmal wie gebaut, einmal flach:

| Fassung | Laufzeit | `EXPLAIN` |
|---|---:|---|
| **gebaut** (Joins über der Deckelung) | **1.655,827** | `b1` `ref` `Using index`, danach vier `eq_ref` auf **51 Zeilen** |
| Gegenform (Joins neben `MessageBAM`) | **2.443,334** | `b1` `ref` `Using index`, danach **sieben** `eq_ref` — auf **234.159 Zeilen** |
| Faktor | **1,48** | — |

## Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| **K3b liegt mit 30-Tage-Fenster unter einer Sekunde** | **ja — 882 ms** | **Offene Frage 10 ist beantwortet.** Das Pflicht-Zeitfenster fängt den Bösfall der Verundung; er fällt von 5.275 auf 882 ms, Faktor 6,0 — dieselbe Größenordnung, die M35 für den schlimmsten Einzelwert misst. Eine **eigene** Grenze für die Verundung wird nicht gebraucht |
| **K5c liegt mit 30-Tage-Fenster unter einer Sekunde** | **ja, weit — 6,9 ms** | Faktor **134** gegenüber der fensterlosen Messung. Der Grund steht im Plan und nicht in der Uhr: Mit Fenster kippt der zweite Begriff nicht auf den `rows`-8-Zugriff |
| K3b oder K5c reißen die Sekunde | **nein** | — **Abnahmepunkt 3 ist erfüllt**, und zwar in beiden Fällen |
| **Der Optimierer steigt über den seltensten Begriff ein** | **ja, in jeder gemessenen Konstellation** | Belegt am `EXPLAIN` und an der Laufzeit: 1,209 gegen 1,202 ms bei getauschter Reihenfolge, 1,622 gegen 1,607 ms bei fünf Begriffen. **Kein `STRAIGHT_JOIN`** — Regel L15 ist geprüft und nicht angenommen |
| **Eine typlose Suche bei `NEXANS` erzeugt wenige Varianten** | **ja — höchstens fünf** | Und die fünf erreicht nur eine ein- oder zweistellige Eingabe. Der Grund ist gezählt: **drei verschiedene** Sollängen auf acht kuratierten Zeilen. Für eine siebenstellige Eingabe sind es **drei** Fassungen, für eine zehnstellige **zwei** |
| **Die Normalisierung kostet wenig** | **ja — Zehntelmillisekunden** | 0,956 → 1,221 ms von einer auf drei Fassungen; 20,228 → 22,439 ms von einer auf fünf bei einem Wert mit 2.256 Treffern. **Damit ist auch beziffert, was eine wirkungslose Variante kostet** (offene Frage aus M46 §8): rund 0,1 ms |
| **Die `IN`-Liste ändert die Wahl der führenden Tabelle** | **nein** | Die Zugriffsart wechselt von `ref` auf `range`, Index und Einstiegstabelle bleiben. Die Frage war offen und ist mit Zahl statt mit Vermutung beantwortet |
| **Die Typangabe beschleunigt nicht** | **ja** (1,131 gegen 1,187 ms) | M36 am gebauten Statement bestätigt. Sie bleibt Ergebnisverfeinerung |
| **Der schlimmste Wert bleibt mit 30 Tagen weit unter der Zeitgrenze** | **ja** — 1,656 s, Faktor 6,0 Reserve | Die Vorgabe von 30 Tagen ist damit gemessen tragfähig |
| **Mit einem Jahr bleibt Reserve** | **kaum** — 8,940 s, **11 % Reserve** | Bestätigt M35 (8,664 s) am gebauten Statement. **Der Abbruchpfad wird gebraucht**, und Regel L1 bleibt mit ihrem Maximum die richtige Grenze |
| **`IBIS` verhält sich wie `NEXANS`** | **ja** | 1,185 und 1,214 ms gegen 1,095 und 1,209 ms. Regel L7 ist erfüllt |

**Wo die vorformulierte Zeile nicht passt — die Anzeigespalten sind der teuerste Fehler, den man hier
machen kann, und er steht in keiner Zeile des Auftrags.** Der Auftrag beschreibt die Zweiteilung in
Trefferzeilen und Trefferwerte, nicht aber, **wo** die vier Anzeigetabellen hängen. Gemessen: 1.655,8
gegen 2.443,3 ms, Faktor 1,48 — und **beide Pläne sehen gleich gut aus**, in beiden ist jeder Zugriff
`eq_ref` auf `PRIMARY`. Es ist derselbe Fehler wie in Teil 1 ([`bam-werte.md`](bam-werte.md) §4, dort
Faktor 18,4), aus demselben Grund: Der Plan sagt nicht, **wie oft** eine Zeile angefasst wird.

**Ein zweiter: Bei K5c greift die Falle aus M42‑1 K5 mit Zeitfenster nicht mehr.** M42 hat sie als
den Fall beschrieben, in dem „ein Zusatzbegriff nicht umsonst ist" — Faktor 112. Mit dreißig Tagen
kostet derselbe Fall **6,9 ms**. Das entwertet M42 nicht: Dort war das Fenster ausdrücklich weggelassen,
damit die Zahlen gegen M34 stehen. **Es verschiebt nur, wer den Fall auffängt** — nicht die Bauform,
sondern Regel L1.

**Ein dritter: die Kuratierung von `NEXANS` hat acht Sollängen, nicht elf.** Der Auftrag zu Teil 2b
nennt „elf Sollängen, aber nur drei verschiedene". Gezählt sind **zehn Zeilen**, davon **acht** mit
einer Sollänge und **zwei** nur mit dem Leerzeichen-Kennzeichen; die Zahl der *verschiedenen* ist mit
drei richtig, und nur auf die kommt es an. Der Code und seine Tests nennen die gezählte Zahl.

**Ein vierter: die zweite Abfrage wählt `MessageBAM_MessageFK` und nicht das Präfix des
Primärschlüssels.** Der Auftrag erwartet den Zugriff „als `ref` über das Präfix des
Primärschlüssels", so wie ihn M11, M26‑1b, M28‑2 und M39‑1 gemessen haben. Gewählt ist der
Fremdschlüsselindex auf `MessageID` allein — **derselbe Zugriffstyp, `Using index`, dieselbe
Größenordnung**; `MessageBAM` hat nach M32 drei Indizes, die auf `MessageID` beginnen und dieselbe
Frage beantworten. Der Unterschied ist eine Feststellung und keine Folge.

### Belegvermerk (Regel L10)

> *Gemessen:* Die Laufzeit des **gebauten** Statements über **19** Fälle bei `NEXANS` und **zwei** bei
> `IBIS`, je mit 30-Tage- und teils mit Jahresfenster, auf einer ruhenden Testkopie; dazu der
> `EXPLAIN` in **13** Fassungen. Die Prüfwerte stammen aus **vier** hergeleiteten Nachrichten (n = 4):
> einer gewöhnlichen `NEXANS`-Wurzel, dem fettesten `NEXANS`-Merge-Ergebnis aus Fenster B, einer
> `IBIS`-Nachricht und der Nachricht hinter einem kuratierten Paar.
>
> *Behauptet wird:* dass der Endpunkt mit seiner Vorgabe von 30 Tagen tragfähig ist.
>
> **Die Lücke, und sie ist dreifach.** Erstens gilt die 10-Sekunden-Grenze in **Produktion**, und
> gemessen ist eine **ruhende** Testkopie — die Zahlen sind Untergrenzen und keine Zusagen; dieselbe
> benannte Lücke wie im Belegvermerk zu M35. Zweitens ist der teuerste Fall **ein** Wert: der
> schlimmste, den M33 über den Bestand kennt. Ob ein Nutzer im Betrieb überhaupt danach sucht, sagt
> keine Zahl dieser Runde. Drittens misst M47 die **Statements** und nicht den **Aufruf**: Was HTTP,
> Sitzungsprüfung und Serialisierung dazulegen, steht hier nicht — in Teil 1 waren es bei drei
> Statements rund 2 ms ([`bam-werte.md`](bam-werte.md) §4). Was trägt: *Mit 30 Tagen kostet der
> schlimmste bekannte Wert 1,66 s und jeder gemessene Normalfall unter 2 ms.* Was **nicht** gemessen
> ist: *derselbe Satz unter Last.*

---

# M48 — Was kostet die Typenauswahl?

*Sechster Nachtrag, 13.08.2026. Schritt 7, Teil 3 — die Messung vor dem Merge (Regel L7, §8
Regel 7).*

**Frage.** Teil 3 bringt einen zweiten Endpunkt mit: `GET /api/bam/typen` liefert die für den
Mandanten der Sitzung konfigurierten BAM-Typen als **Auswahl** neben dem Suchfeld. Er fasst
ausschließlich Stammdaten an — was kostet er, und über welchen Pfad?

> **Das Ergebnis vorweg.** **0,534 ms** für den größten Mandanten (`NEXANS`, 40 konfigurierte
> Typen), **0,410 ms** für einen ohne jede Konfiguration (`WOC`). Der Zugriff ist ein `ref` über
> `MandantIDSortIndexBAMTYpeIDX` mit **`Using index`** — und der Index trägt Filter *und* beide
> Sortierschlüssel, es entsteht **kein `filesort`**. Der Befund, der nicht im Auftrag stand:
> **`MessageBAMTypeSortIndex` ist nicht eindeutig** — bei `VOTG` tragen die Typen 2002 und 2011
> beide den Index 2002.

## M48‑0 Rahmen

Unverändert aus §0. Erhoben am **13.08.2026** in einer eigenen Sitzung, Lesebenutzer
`monitor_read@%`, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`.

| | |
|---|---|
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**, erste **und** letzte Abfrage der Sitzung |
| Serverzeit | `2026-08-13 14:22:04` |
| `@@sql_mode` | `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION` — unverändert gegenüber M47 |
| Laufzeitmessung | `SET profiling = 1` / `SHOW PROFILES`, **beste von fünf** nach einem Aufwärmlauf |
| Gemessen wurde | der **gerenderte** Text, den jOOQ tatsächlich schickt — abgegriffen gegen die Attrappe aus `BamTypenStatementsTest` |
| Regel S1 | ausschließlich `SELECT` und `EXPLAIN` |

## M48‑1 Die Größenordnung der beiden Stammdatentabellen

```sql
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES
WHERE TABLE_SCHEMA='GlassfishDB' AND TABLE_NAME IN ('MessageBAMMandant','MessageBAMType');
```

| Tabelle | Zeilen |
|---|---:|
| `MessageBAMMandant` | **69** |
| `MessageBAMType` | **62** |

Zusammen 131 Zeilen — weniger als eine einzige Seite der Nachrichtenliste.

### Konfigurierte Typen je Mandant

```sql
SELECT MandantID, COUNT(*) AS typen, COUNT(DISTINCT MessageBAMTypeSortIndex) AS versch_idx
FROM MessageBAMMandant GROUP BY MandantID ORDER BY typen DESC;
```

| Mandant | Typen | verschiedene Sortierindizes |
|---|---:|---:|
| `NEXANS` | 40 | 40 |
| **`VOTG`** | **12** | **11** |
| `NXHBE` | 6 | 6 |
| `IBISGUS` | 4 | 4 |
| `IBIS` | 4 | 4 |
| `SUTTONS` | 2 | 2 |
| `ZAST` | 1 | 1 |

**`EDITIONLINGERI`, `SYSTEM` und `WOC` kommen nicht vor** — sie haben keinen konfigurierten Typ.
Das bestätigt M40 aus der Gegenrichtung und ist zugleich der Prüffall „leere Auswahl" des
Endpunkts.

> **Der Befund, der in keiner vorformulierten Zeile stand: Der Sortierindex ist nicht eindeutig.**
> `VOTG` hat 12 Typen auf 11 verschiedenen Indizes — 2002 (`InvoiceNumber VTG`) und 2011
> (`InvoiceNumber Vendor`) tragen beide den Index 2002. Ohne einen zweiten Sortierschlüssel
> entschiede dort die Reihenfolge der Speicherung, und zwei Aufrufe zeigten dieselbe Auswahl
> verschieden. Das Statement sortiert deshalb über `(MessageBAMTypeSortIndex, MessageBAMType)`, und
> `BamTypenDbIT.die_ordnung_ist_auch_bei_gleichem_index_eindeutig` hält den Fall fest — samt der
> Vorprüfung, dass es ihn überhaupt noch gibt.

## M48‑2 `EXPLAIN`

Gemessen wurde der gerenderte Text:

```sql
select `GlassfishDB`.`MessageBAMMandant`.`MessageBAMType`,
       `GlassfishDB`.`MessageBAMType`.`MessageBAMTypeDescription`,
       `GlassfishDB`.`MessageBAMMandant`.`MessageBAMTypeSortIndex`
from `GlassfishDB`.`MessageBAMMandant`
  left outer join `GlassfishDB`.`MessageBAMType`
    on `GlassfishDB`.`MessageBAMType`.`MessageBAMType` = `GlassfishDB`.`MessageBAMMandant`.`MessageBAMType`
where `GlassfishDB`.`MessageBAMMandant`.`MandantID` = ?
order by `GlassfishDB`.`MessageBAMMandant`.`MessageBAMTypeSortIndex` asc,
         `GlassfishDB`.`MessageBAMMandant`.`MessageBAMType` asc
```

**`NEXANS`** (40 Typen):

| table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---:|---|
| `MessageBAMMandant` | `ref` | `MandantIDSortIndexBAMTYpeIDX` | 146 | const | 40 | **`Using where; Using index`** |
| `MessageBAMType` | `eq_ref` | `PRIMARY` | 2 | `…MessageBAMType` | 1 | |

**`WOC`** (0 Typen): derselbe Plan, `rows` **1**.

**Der Index ist genau der richtige, und das ist kein Zufall:**

```
MandantIDSortIndexBAMTYpeIDX = (MandantID, MessageBAMTypeSortIndex, MessageBAMType)
```

Er trägt den Filter **und** beide Sortierschlüssel in dieser Reihenfolge — deshalb `Using index` und
**kein `filesort`**. Die Ordnung, die den doppelten Sortierindex auflöst, kostet damit **nichts**;
sie ist die Ordnung des Index.

## M48‑3 Laufzeiten

Beste von fünf nach einem Aufwärmlauf, in Millisekunden.

| Fall | Laufzeit |
|---|---:|
| `NEXANS` — 40 konfigurierte Typen | **0,534** |
| `WOC` — keine Konfiguration | **0,410** |

Der Unterschied zwischen dem größten und dem leeren Fall beträgt **0,12 ms**. Das ist die
erwartete Gestalt: Beide Fälle bezahlen den Indexzugriff, und die 40 Zeilen darüber sind
40 `eq_ref`-Zugriffe auf eine Tabelle mit 62 Zeilen.

## Was daraus folgt

1. **Der Endpunkt ist billig genug, um auf jeder Seite zu laden.** Das Suchfeld steht in der
   Kopfzeile und braucht die Auswahl überall; gehalten wird sie 15 Minuten lang
   (`useBamTypen`), gezahlt wird sie einmal je Sitzung und Mandant.

2. **Er fasst `Message` und `MessageBAM` nicht an, und das ist eine Entscheidung.** Die Versuchung,
   aus der Auswahl eine Aussage über den *Bestand* zu machen („biete nur Typen an, die tatsächlich
   vorkommen"), wäre eine Existenzfrage über den Gesamtbestand eines Mandanten — genau die Gestalt,
   die in [`nachrichtenliste.md`](nachrichtenliste.md) §1 als **L15-Falle** geführt ist: 13,2
   Sekunden für `IBIS` gegen 11,9 Millisekunden für `WOC`, beide ohne ein einziges Ergebnis.
   `BamTypenStatementsTest.nur_stammdaten` hält es fest.

3. **Der doppelte Sortierindex ist der Grund für den zweiten Sortierschlüssel** — und er kostet
   nichts, weil der Index ihn ohnehin trägt.

4. **Ein Zeitfenster gibt es hier nicht, und Regel L1 ist nicht berührt.** Sie gilt für *Listen*
   über `Message`; hier stehen zwei Stammdatentabellen mit zusammen 131 Zeilen, von denen keine
   einen Zeitstempel trägt.

---

# M49 — Trägt eine Präfixsuche in der BAM-Suche?

***Siebter** Nachtrag, 13.08.2026. Schritt 7 — **eine Erhebung, keine Entscheidung** und kein Code.*

> **Abweichung von der Aufgabenstellung, benannt statt stillgeschwiegen.** Der Auftrag verlangt
> diesen Abschnitt als **sechsten** Nachtrag. Der sechste ist bereits vergeben: **M48** (Teil 3, die
> Typenauswahl) trägt diese Nummer seit demselben Tag. Der Auftrag hat das nicht bemerkt, obwohl er
> M48 als höchste **Messnummer** richtig erwartet — Messnummern und Nachtragsnummern laufen in
> dieser Datei getrennt. Gewählt ist deshalb **siebter**; zwei Abschnitte mit derselben Ordnungszahl
> wären die schlechtere Auflösung. **Die Messnummer ist wie erwartet M49.**

**Frage.** `GET /api/bam/suche` sucht exakt. Ob sich eine Präfixsuche nachrüsten lässt, steht als
offener Punkt 1 in [`bam-suche.md`](bam-suche.md) §9. M34, E6 und M43‑3 haben die Kosten- und die
Kollationsfrage bereits beantwortet; **ungemessen war viererlei**, und darum geht es hier:

1. ob die Normalisierung aus Teil 2a — die vorn mit Nullen auffüllt — mit einem Muster, das
   ebenfalls vorn ankert, überhaupt zusammengeht (**M49‑1**);
2. ob es eine Präfixlänge gibt, ab der die schlimmste Trefferzahl beherrschbar wird (**M49‑2**);
3. wie sich das **gebaute** Statement mit `LIKE` statt `IN` verhält (**M49‑3**);
4. ob im Bestand Zeichen vorkommen, die unter `LIKE` zu Platzhaltern werden (**M49‑4**).

> **Das Ergebnis vorweg, in fünf Sätzen.** Die Ableitung stimmt: **In allen acht geprüften Fällen
> findet die rohe Präfixfassung den aufgefüllten Originalwert nicht** — Auffüllen und Präfix ankern
> gegeneinander. Der Ausweg existiert und ist bezifferbar: Die Nullen ins Muster gezogen, findet
> jeder Fall sein Original, und der Preis sind **drei bis sieben** Fassungen statt der heutigen
> Obergrenze von fünf. Eine schützende Mindestlänge gibt es **nicht** — die schlimmste Trefferzahl
> fällt von k = 4 bis k = 6 nur von 1.332.180 auf **234.159** und kann darunter nie fallen, weil
> genau ein *exakter* Wert diese 234.159 Zeilen trägt. Je Typ liegt die Grenze völlig verschieden
> (bei 2002 schon bei vier Zeichen unter 1.000, bei 9014 nie), **und die Suche ist typlos
> voreingestellt.** Und der Befund, der in keiner Zeile des Auftrags stand: **Schon der
> vollständige Wert als Präfix findet 23 Nachrichten statt einer.**

## M49‑0 Rahmen und Anker

Unverändert aus §0 übernommen, am **13.08.2026** in dreizehn eigenen Sitzungen erhoben. Jeder Aufruf
des Clients ist eine neue Sitzung; **`SELECT @@global.read_only` steht deshalb in jedem Skript als
erste Abfrage** und lieferte **jedes Mal `1`** — auch in der Schlusssitzung.

| | |
|---|---|
| Ziel | **Testkopie**, `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| `@@global.read_only` **zu Beginn** | **`1`** (erste Abfrage der Runde, 16:43:40) |
| `@@global.read_only` **am Ende** | **`1`** (letzte Abfrage der Runde, 17:02:03) |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Serverzeit zu Beginn | `2026-08-13 16:43:40` (`UTC_TIMESTAMP` `14:43:40`, also UTC+2) |
| Serverzeit am Ende | `2026-08-13 17:02:03` (`UTC_TIMESTAMP` `15:02:03`) |
| `@@global.max_statement_time` | **`0`** — die 60 Sekunden je Sitzung über `SET max_statement_time = 60` |
| `@@global.event_scheduler` | **`ON`**, unverändert |
| Laufzeitmessung | serverseitig, `SET profiling = 1` / `information_schema.PROFILING`, `profiling_history_size = 100` |
| Zugangsdaten | ausschließlich aus `OVERLORD_DB_*`, an den Client über `MYSQL_PWD` |

### `@@sql_mode` — unverändert

```
STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION
```

**Kein `ONLY_FULL_GROUP_BY`**, wie in §0 und M47.

### `SHOW GRANTS FOR CURRENT_USER()`

Drei Zeilen, unverändert gegenüber Schritt 4 bis 6 und gegenüber M47. Der Hash der ersten wird nach
Regel G1 **nicht** abgedruckt:

```
GRANT USAGE ON *.* TO `monitor_read`@`%` IDENTIFIED BY PASSWORD '<Hash>'
GRANT SELECT ON `GlassfishDB`.* TO `monitor_read`@`%`
GRANT SELECT ON `overlord_monitor`.* TO `monitor_read`@`%`
```

### Die Testkopie ist unverändert — der **achte** Messtag in Folge

| | `DATA_LENGTH` | `INDEX_LENGTH` |
|---|---:|---:|
| `Message` | 740.851.712 | 2.157.330.432 |
| `MessageAction` | 2.226.634.752 | 819.855.360 |
| **`MessageBAM`** | **1.826.422.784** | **5.254.217.728** |

**Byteidentisch mit M23‑1, M31‑0, §0, M44‑0, M46‑0 und M47‑0.** Die Zahlen dieser Runde dürfen ohne
Vorbehalt gegen M32 bis M48 gehalten werden.

**Vier weitere Kontrollen sind nebenbei mitgelaufen und gehen sämtlich auf** — sie stehen hier, weil
eine byteidentische Tabelle noch nicht heißt, dass dieselben Zeilen darin stehen:

| Kontrolle | erwartet aus | gemessen |
|---|---|---|
| Zeilen in `MessageBAM` (dreimal unabhängig) | **15.406.350** (M33) | **15.406.350** ✔ |
| Kernlängen der acht Prüftypen | 12 / 9 / 8 / 8 / 8 / 7 / 1 / 3 (M43‑4) | Zeichen für Zeichen ✔ |
| Präfix des typischen Werts auf 6 / 4 Zeichen | **5.425** / **155.871** (E6) | **5.425** / **155.871** ✔ |
| Die sechs Werte der Ankernachricht | 1 / 1.182 / 234.159 / 540 / 100.343 (M42‑0) | Zeile für Zeile ✔ |

### Der Vergleichsanker aus M47

Ein Statement aus M47 **unverändert**: das gebaute, gerenderte Suchstatement mit der `IN`-Liste, ein
Begriff, typischer Wert, 30 Tage.

| | M47 (13.08., frühere Sitzung) | M49 | Reproduktion |
|---|---:|---:|---:|
| ein Begriff, typischer Wert, 30 T | **1,095 ms** | **1,160 ms** | **105,9 %** |

**Die Abweichung beträgt +5,9 Prozent** und liegt damit unter der Streuung, die M47 zwischen seinen
eigenen beiden Messrunden ausweist (bis 1,2 % bei den teuren, hier bei einem Statement von rund
einer Millisekunde). Die neuen Zahlen sind mit den alten vergleichbar.

### Die Messnummer

Höchste bisher vergebene Nummer in dieser Datei: **M48** — wie erwartet. Diese Runde ist damit
**M49**, mit den Teilen M49‑0 bis M49‑4.

### Skills

**Es ist keiner benutzt worden.** Installiert sind `edi-field-mapping`, `find-skills`, `shadcn`
(Benutzerebene) und das Plugin `frontend-design`; projektlokale Skills gibt es nicht. Diese Runde
schreibt keinen Code und fasst keine Oberfläche an — `frontend-design` und `shadcn` sind damit
gegenstandslos, die übrigen betreffen andere Werkzeuge.

### Anonymisierung

Wie in §0: **Kein BAM-Wert und keine Belegnummer steht in diesem Nachtrag.** Alle Prüfwerte stehen in
Sitzungsvariablen, die auf dem Server gesetzt und dort verbraucht werden. **Neu gegenüber §0 ist
eine Verschärfung**, und sie betrifft M49‑2a: Die häufigsten Präfixe *sind* Anfangsstücke echter
Belegnummern und werden deshalb **maskiert** (`P1` bis `P10`); abgedruckt sind ihre Gestalt, ihre
Trefferzahl und — wo die Frage daran hängt — der **Typ**, unter dem sie stehen. Typnummern und
Beschreibungen aus `MessageBAMType` sind nach der Regel in [`README.md`](README.md)
Konfigurationsvokabular und bleiben stehen.

---

## M49‑1 Sind Auffüllen und Präfix vereinbar?

**Frage.** Findet ein Präfixmuster einen Wert, der im Bestand mit führenden Nullen steht, wenn die
Eingabe verkürzt ist? Und findet ihn die Fassung mit den Nullen **im Muster**?

### Die Kuratierung, gelesen statt abgeschrieben

Erste Erkenntnis, noch vor jeder Trefferzahl: **Zwei der acht Typen aus M43‑4 haben in
`overlord_monitor.bam_sollaenge` überhaupt keine Zeile.**

```sql
SELECT mandant_id, message_bam_type, sollaenge, fuehrendes_leerzeichen, dominanz_prozent, zeilen
FROM overlord_monitor.bam_sollaenge ORDER BY message_bam_type, mandant_id;
```

| Typ | Beschreibung | kuratiertes Paar | Sollänge |
|---:|---|---|---:|
| 2001 | VendorReference | **keins** | — |
| 2002 | InvoiceNumber VTG | `VOTG` | 10 |
| 9006 | Lieferschein-Nr._L_SAP | **keins** | — |
| 9011 | Anlieferungs-Nr. ae_L_SAP | `NEXANS` | 10 |
| 9012 | Charge_L_SAP | `NEXANS` | 10 |
| 9021 | Transportnummer_K_SAP | `NEXANS` | 10 |
| 9024 | Rechnungsnummer_K_SAP | `NEXANS` | 10 |
| 9036 | Lagerort Kunde_L_SAP | `NEXANS` | 4 |

Das ist kein Fehler der Erhebung, sondern die Kuratierung selbst: **2001 und 9006 sind durch die
95‑Prozent‑Regel gefallen** ([`bam-sollaengen.md`](bam-sollaengen.md)); 9006 mit 94,21 %. Damit die
beiden trotzdem messbar sind, ist ihre Sollänge **in der Sitzung hergeleitet** und ausdrücklich
**nicht** aus der Kuratierung gelesen:

```sql
SELECT LENGTH(MessageBAMValue) AS laenge, COUNT(*) AS n
FROM MessageBAM WHERE MessageBAMType = 2001 GROUP BY laenge ORDER BY n DESC LIMIT 3;
```

| Typ | dominante Länge | n | Anteil |
|---:|---:|---:|---:|
| 2001 | **14** | 8.854 von 11.191 | 79,12 % |
| 9006 | **8** | 146.780 von 155.809 | **94,21 %** |

Die 94,21 % reproduzieren M46 auf zwei Nachkommastellen. **Beide hergeleiteten Sollängen sind
identisch mit denen, die M43‑4 benutzt hat** (14 und 8) — die Vergleichbarkeit steht.

### Die Prüfwerte

Bauform wie in `BamSollaengeDriftDbIT`, je Typ neu hergeleitet, nie übertragen:

```sql
SET @typ := 9011; SET @soll := 10;
SELECT MIN(MessageBAMValue) INTO @wert FROM MessageBAM
 WHERE MessageBAMType = @typ AND MessageBAMValue LIKE '0%' AND LENGTH(MessageBAMValue) = @soll;
SET @kern := TRIM(LEADING '0' FROM @wert);
SET @p    := LEFT(@kern, LENGTH(@kern) - 2);   -- bzw. - 4
```

**Für jeden der acht Typen ist ein Wert gefunden worden** — die Herleitung ist nirgends leer
ausgegangen.

### Statement

Je Prüfwert **ein** Statement, als `UNION ALL` über die Nullenzahl `j`, ohne Typeinschränkung (die
Suche ist typlos — M32, M36), hier für vier Fassungen:

```sql
SELECT 0 AS nullen, COUNT(*) AS treffer FROM MessageBAM WHERE MessageBAMValue LIKE CONCAT(@p,'%')
UNION ALL SELECT 1, COUNT(*) FROM MessageBAM WHERE MessageBAMValue LIKE CONCAT('0',@p,'%')
UNION ALL SELECT 2, COUNT(*) FROM MessageBAM WHERE MessageBAMValue LIKE CONCAT('00',@p,'%')
UNION ALL SELECT 3, COUNT(*) FROM MessageBAM WHERE MessageBAMValue LIKE CONCAT('000',@p,'%');

SELECT @wert LIKE CONCAT(@p,'%')                          AS roh_trifft,
       @wert LIKE CONCAT(REPEAT('0',@soll-LENGTH(@kern)),@p,'%') AS mit_nullen_trifft;
```

### `EXPLAIN` — jeder Zweig bleibt ein Bereichszugriff

Für **jedes** der zehn Statements dieselbe Gestalt, ein Zweig je Fassung (Regel L15 — belegt, nicht
angenommen):

| id | select_type | table | type | key | key_len | Extra |
|---|---|---|---|---|---:|---|
| 1 | `PRIMARY` | `MessageBAM` | **`range`** | **`MessageBAM_BAMValueOnly`** | 282 | `Using where; Using index` |
| 2…n | `UNCACHEABLE UNION` | `MessageBAM` | **`range`** | **`MessageBAM_BAMValueOnly`** | 282 | `Using where; Using index` |

`UNCACHEABLE UNION` steht dort, weil die Sitzungsvariable den Zweig für den Abfrage-Cache
unbrauchbar macht — für den Zugriffspfad folgenlos.

### Ergebnis

`ø` markiert die Fassung, die den Originalwert trifft.

| Typ | Sollänge | Herkunft | Länge Original | Kern­länge | führende Nullen | Präfix­länge | **Fassungen** | Treffer je Fassung (j = 0, 1, 2, …) | **Summe** | roh trifft | mit Nullen trifft |
|---:|---:|---|---:|---:|---:|---:|---:|---|---:|---|---|
| **2001** | 14 | abgeleitet | 14 | 12 | 2 | 10 | **4** | 0 · 0 · **3 ø** · 0 | **3** | **nein** | **ja** |
| 2001 (−4) | 14 | abgeleitet | 14 | 12 | 2 | 8 | **6** | 0 · 0 · **5 ø** · 0 · 0 · 0 | **5** | **nein** | **ja** |
| **2002** | 10 | kuratiert | 10 | 9 | 1 | 7 | **3** | 0 · **4 ø** · 0 | **4** | **nein** | **ja** |
| **9011** | 10 | kuratiert | 10 | 8 | 2 | 6 | **4** | 0 · 0 · **2 ø** · 0 | **2** | **nein** | **ja** |
| **9012** | 10 | kuratiert | 10 | 8 | 2 | 6 | **4** | 0 · 0 · **1 ø** · 0 | **1** | **nein** | **ja** |
| 9012 (−4) | 10 | kuratiert | 10 | 8 | 2 | 4 | **6** | **90** · 2 · **1 ø** · 0 · 0 · 0 | **93** | **nein** | **ja** |
| **9024** | 10 | kuratiert | 10 | 8 | 2 | 6 | **4** | 0 · 0 · **1 ø** · 0 | **1** | **nein** | **ja** |
| **9021** | 10 | kuratiert | 10 | 7 | 3 | 5 | **5** | 10 · 54 · 0 · **5 ø** · 0 | **69** | **nein** | **ja** |
| **9036** | 4 | kuratiert | 4 | **1** | 3 | — | — | *Verkürzung unmöglich* | — | — | — |
| 9036 (ungekürzt) | 4 | kuratiert | 4 | 1 | 3 | 1 | **3** | **1.086.626** · 34.928 · **34.806 ø** | **1.156.360** | **nein** | **ja** |
| **9006** | 8 | abgeleitet | 8 | 3 | 5 | 1 | **7** | **1.086.626** · 34.928 · 34.806 · 65.353 · 1.598 · **5.425 ø** · 16.882 | **1.245.618** | **nein** | **ja** |

**In allen acht Fällen, in denen sich die Frage stellen lässt, ist `roh_trifft` gleich `0` und
`mit_nullen_trifft` gleich `1`.**

### Befunde — vor der Messung formuliert

| Befund | trifft zu | Konsequenz |
|---|---|---|
| **Die rohe Präfixfassung findet den Originalwert nicht** | **ja — in allen acht prüfbaren Fällen** | **Die Ableitung stimmt: Auffüllen und Präfix ankern gegeneinander.** Präfix ist bei kuratierten Typen ohne Zusatzbau **wirkungslos** — nicht „ungenauer", sondern ergebnislos |
| Die rohe Präfixfassung findet ihn doch | **nein**, in keinem Fall | — |
| **Die Nullen-im-Muster-Fassung findet ihn** | **ja — in allen acht** | Es gibt einen Ausweg, und sein Preis ist die Zahl der Fassungen: **3 bis 7** |
| **Die Zahl der nötigen Fassungen wächst mit der Verkürzung** | **ja** | 2001: 4 → 6 Fassungen bei zwei zusätzlichen Zeichen weniger; 9012 ebenso. **Die heutige Obergrenze von fünf Fassungen (M47) trägt nicht mehr** — schon 9006 braucht **sieben**, und das bei *einem* Begriff. Bei acht Begriffen wären es bis zu 56 `LIKE`-Zweige |
| **Die Trefferzahl der Nullen-Fassungen bleibt im zweistelligen Bereich** | **nein — sie spreizt über fünf Größenordnungen** | 1 bis **1.245.618**. Sie bleibt zweistellig, solange der **Kern lang** ist (9011, 9012, 9024, 2001, 2002, 9021: 1 bis 93), und bricht zusammen, sobald er **kurz** ist (9006, 9036: über eine Million). **Der Ausweg ist begehbar — aber nicht überall** |

**Wo die vorformulierte Zeile nicht passt (1) — zwei der acht Typen füllen gar nicht auf, und dort
gibt es den Widerspruch nicht.** Der Auftrag behandelt alle acht Typen aus M43‑4 als kuratiert. **2001
und 9006 stehen nicht in `bam_sollaenge`**, weil sie durch die 95‑Prozent‑Regel gefallen sind. Für sie
gilt der Befund der ersten Zeile **nicht aus dem gemessenen Grund**: Die Anwendung füllt ihre Eingabe
nicht auf, ein Präfixmuster ankert also nicht gegen eine Normalisierung, sondern steht allein. Dass
die rohe Fassung den Originalwert trotzdem nicht findet, liegt am **Bestand** (die Werte tragen dort
führende Nullen) und nicht an der Anwendung. **Für diese beiden Typen wäre eine Präfixsuche also
sofort wirksam — und sofort teuer**: 9006 ist ausgerechnet der Typ mit der Millionen-Trefferzahl.

**Wo die vorformulierte Zeile nicht passt (2) — bei 9036 lässt sich der Prüfwert nicht verkürzen.**
Sein Kern ist **ein Zeichen** lang; `LEFT(kern, LENGTH(kern) - 2)` ist die leere Zeichenkette, und
`LIKE '%'` wäre der ganze Bestand. Der Auftrag setzt voraus, dass eine Verkürzung um zwei Zeichen
immer möglich ist. Gemessen ist deshalb die **ungekürzte** Fassung, und sie zeigt dasselbe Bild in
schärferer Form: Schon der vollständige Kern als Präfix kostet **1.086.626** Treffer in der ersten
Fassung. Bei 9006 fällt dieselbe Grenze bei der zweiten Verkürzung (Kern 3 Zeichen, `−4` unmöglich).

### Laufzeiten

| Prüfwert | Fassungen | Laufzeit |
|---|---:|---:|
| 2002 | 3 | **0,661 ms** |
| 9024 | 4 | 0,787 ms |
| 9012 | 4 | 0,833 ms |
| 9011 | 4 | 0,855 ms |
| 2001 | 4 | 0,984 ms |
| 9021 | 5 | 1,013 ms |
| 2001 (−4) | 6 | 1,050 ms |
| 9012 (−4) | 6 | 1,174 ms |
| **9036 (ungekürzt)** | 3 | **679,599 ms** |
| **9006** | 7 | **739,704 ms** |

*einmalig je Prüfwert; die Frage gilt der Trefferzahl und nicht der Uhr.*

**Die Laufzeit folgt der Trefferzahl und nicht der Zahl der Fassungen** — sechs Fassungen mit 93
Treffern kosten 1,2 ms, drei Fassungen mit 1.156.360 Treffern kosten 680 ms. Das ist E6, an einer
zweiten Stelle bestätigt.

### Belegvermerk (Regel L10)

> *Gemessen:* Für **acht** BAM-Typen (n = 8, davon sechs kuratiert und zwei mit in der Sitzung
> hergeleiteter Sollänge) je **ein** Prüfwert — der kleinste Wert mit führender Null auf der
> Sollänge —, dazu für drei Typen eine zweite, stärkere Verkürzung; insgesamt **zehn** Statements mit
> je einem `EXPLAIN`, auf einer ruhenden Testkopie, **ohne Zeitfenster und ohne Mandantenfilter**.
>
> *Behauptet wird:* dass Auffüllen und Präfixsuche einander ausschließen und dass die
> Nullen-im-Muster-Fassung der einzige gemessene Ausweg ist.
>
> **Die Lücke, und sie ist dreifach.** Erstens ist je Typ **ein** Wert gemessen, nicht die
> Verteilung: Dass `roh_trifft` achtmal `0` ist, ist ein starkes Indiz und kein Beweis für *alle*
> Werte dieser Typen — die Aussage folgt allerdings zusätzlich aus der Zeichenlogik, denn ein Kern
> beginnt nach `TRIM(LEADING '0')` nie mit einer Null und ein aufgefüllter Wert immer. Zweitens sagt
> keine dieser Zahlen, ob die **einzige** Ausweg-Bauform die Nullen im Muster sind; gemessen ist,
> dass **diese** funktioniert, nicht dass keine andere existiert. Und drittens sind die Trefferzahlen
> **typlos und ohne Zeitfenster** gezählt — der Endpunkt filtert danach über Mandant und dreißig
> Tage, die Zahlen sind also **Kandidatenmengen** und keine Trefferlisten. Was trägt: *Wer verkürzt
> tippt, findet ohne die Nullen im Muster nichts.* Was **nicht** gemessen ist: *wie viele Fassungen
> die Anwendung im Betrieb wirklich bilden müsste, weil das von der Eingabelänge des Nutzers
> abhängt.*

---

## M49‑2 Die Trefferzahl je Präfixlänge

**Frage.** E6 zeigt an **einem** Wert, was Verkürzung kostet. Was fehlt, ist die Verteilung: Gibt es
eine Präfixlänge, ab der die **schlimmste** Trefferzahl beherrschbar wird?

> **Regel L9 ist berührt, und die Begründung steht vorher.** Beide Teile laufen **ohne Zeitfenster
> über eine große Tabelle**. Ein Zeitfenster kann die Frage nicht beantworten: Gefragt ist, wie sich
> die Werte des **Bestands** auf Präfixe verteilen — ein Monatsausschnitt sagte, wie sich die Werte
> *eines Monats* verteilen, und genau das ist nicht die Größe, die eine Mindestlänge schützen müsste.
> Die Kosten stehen unten in der Laufzeittabelle, und ein Statement hat die Grenze gerissen.

### M49‑2a Typlos, über den ganzen Bestand

```sql
SELECT LEFT(MessageBAMValue,4) AS p, COUNT(*) AS n
FROM MessageBAM GROUP BY p ORDER BY n DESC LIMIT 10;
```

**`EXPLAIN`** — für alle vier Längen dieselbe Gestalt, und der Wertindex wird **nicht** benutzt:

| id | table | type | key | key_len | rows | Extra |
|---|---|---|---|---:|---:|---|
| 1 | `MessageBAM` | **`index`** | **`PRIMARY`** | 430 | 10.859.666 | `Using index; Using temporary; Using filesort` |

Der Optimierer läuft den **Primärschlüssel** vollständig durch, nicht `MessageBAM_BAMValueOnly`.
Für eine Gruppierung über einen **Ausdruck** (`LEFT(v,k)`) ist kein Index sortierend brauchbar; der
schmalere Wertindex wäre die billigere Wahl gewesen. **Das ist eine Feststellung und keine
Forderung** — an `GlassfishDB` wird nichts geändert (Regel S1).

**Die Sonde bei `k = 4` kostet 16,144 s** und bleibt damit unter der 30‑Sekunden-Marke des Auftrags;
die übrigen Längen sind deshalb gelaufen.

#### Ergebnis — die zehn häufigsten Präfixe je Länge

Präfixe **maskiert** (siehe Anonymisierung); abgedruckt sind Gestalt und Trefferzahl.

| Rang | k = 3 | k = 4 | k = 5 | k = 6 |
|---:|---:|---:|---:|---:|
| 1 | 1.414.772 | **1.332.180** | 554.353 | **234.159** |
| 2 | 1.118.693 | 1.118.693 | 505.104 | 224.481 |
| 3 | 441.835 | 234.162 | 383.310 | 199.139 |
| 4 | 369.267 | 230.264 | 251.261 | 185.591 |
| 5 | 276.405 | 200.705 | 245.365 | **167.734** |
| 6 | **264.469** | 199.139 | **234.159** | 139.341 |
| 7 | 260.705 | 190.004 | 224.481 | 135.981 |
| 8 | 249.553 | 188.928 | 204.506 | 124.793 |
| 9 | 238.941 | **167.734** | 199.139 | 124.715 |
| 10 | 234.819 | **155.871** | **167.734** | 119.717 |

**Drei dieser Zahlen sind Wiedererkennungen und keine neuen Werte:**

- **155.871** (k = 4, Rang 10) ist der Präfix aus **vier Nullen** — und damit exakt die Zahl, die
  **E6** für das auf vier Zeichen verkürzte Präfix des typischen Prüfwerts misst. E6 ist damit an
  einer unabhängigen Stelle reproduziert.
- **264.469** (k = 3, Rang 6) ist dasselbe für **drei** Nullen — ebenfalls E6, dort als „nicht
  gemessen" für die Laufzeit, aber mit derselben Trefferzahl.
- **234.159** ist der **schlimmste Einzelwert des ganzen Bestands** (M33, M34, M42, M47). Er
  erscheint ab k = 5 unverändert.

**167.734 ist der Fall, den der Auftrag ausdrücklich in die Deutung gehoben haben wollte.** Diese
Zahl steht bei k = 4, 5 **und** 6 identisch da. `LEFT(v,k)` liefert bei Werten, die **kürzer** als
`k` sind, den ganzen Wert zurück — dieser Wert ist höchstens vier Zeichen lang, und für ihn ist die
„Präfixsuche" bereits die **exakte** Suche. Das ist richtig so und keine Verzerrung; es heißt aber,
dass eine Mindestlänge diesen Wert überhaupt nicht erreichen kann.

#### Ergebnis — die Bänder

```sql
SELECT COUNT(*) AS praefixe, MAX(n) AS schlimmster,
       SUM(n >= 1000) AS ab_1k, SUM(n >= 10000) AS ab_10k, SUM(n >= 100000) AS ab_100k,
       SUM(n) AS zeilen_gesamt
FROM (SELECT LEFT(MessageBAMValue,4) AS p, COUNT(*) AS n FROM MessageBAM GROUP BY p) x;
```

| k | Präfixe | **schlimmster** | ≥ 1.000 | ≥ 10.000 | ≥ 100.000 | Summe |
|---:|---:|---:|---:|---:|---:|---:|
| **4** | 12.915 | **1.332.180** | **1.731** | **139** | **19** | 15.406.350 |
| **6** | 241.737 | **234.159** | **1.624** | **136** | **15** | 15.406.350 |
| **8** | — | — | — | — | — | **Abbruch bei 60 s** |

`SUM(n)` ist beide Male **15.406.350** — die gezählte Zeilenzahl aus M33, ein drittes Mal unabhängig
reproduziert.

**Zwei zusätzliche Zeichen entwaffnen fast nichts.** Die Zahl der Präfixe wächst um Faktor 18,7 —
die Zahl der **gefährlichen** Präfixe fällt von 1.731 auf 1.624 (−6,2 %), die der sehr gefährlichen
von 139 auf 136 (−2,2 %). **Wer von vier auf sechs Zeichen erhöht, verkleinert den Bestand der
Bösfälle um sechs Prozent.**

**Der Abbruch bei `k = 8` ist ein Ergebnis und kein Fehlschlag.** Er ist unten unter den Abweichungen
ausgewiesen. Er verhindert die Fortsetzung der Reihe — **beantwortet die Frage aber nicht offen**,
denn die Antwort folgt aus zwei bereits gemessenen Zahlen: Bei k = 6 ist die schlimmste Trefferzahl
**234.159**, und M33 misst, dass **genau ein exakter Wert** diese 234.159 Zeilen trägt. Da `MAX(n)`
mit wachsendem `k` nicht steigen kann und nie unter die Trefferzahl des häufigsten *exakten* Werts
fällt, ist **234.159 ein Boden, den keine Präfixlänge unterschreitet.**

#### Welchem Typ die schlimmsten Präfixe gehören

```sql
SELECT g.MessageBAMType, t.MessageBAMTypeDescription, g.n
FROM (SELECT LEFT(MessageBAMValue,4) AS p, MessageBAMType, COUNT(*) AS n
      FROM MessageBAM GROUP BY p, MessageBAMType ORDER BY n DESC LIMIT 10) g
LEFT JOIN MessageBAMType t ON t.MessageBAMType = g.MessageBAMType;
```

| Rang | Typ | Beschreibung | Zeilen | Kennung oder Beleg? |
|---:|---:|---|---:|---|
| 1 | 9002 | Lieferplannummer_L_SAP | 789.416 | Kennung |
| 2 | 9018 | Kundenmaterialnummer_K_SAP | 766.103 | Kennung |
| **3** | **9019** | **Bestellnummer vom Kunden_K_SAP** | **410.030** | **Beleg** |
| 4 | 9014 | Lieferantennummer beim Kunden_K_SAP | 234.159 | Kennung |
| **5** | **9019** | **Bestellnummer vom Kunden_K_SAP** | **190.216** | **Beleg** |
| 6 | 9004 | Unsere Material-Nr._L_SAP | 186.684 | Kennung |
| 7 | 9028 | Material-Nr. beim Kunden_FORS | 176.203 | Kennung |
| 8 | 9029 | Material-Nr.beim Lieferanten_FORS | 176.203 | Kennung |
| 9 | 9004 | Unsere Material-Nr._L_SAP | 173.450 | Kennung |
| **10** | **9019** | **Bestellnummer vom Kunden_K_SAP** | **161.067** | **Beleg** |

### M49‑2b Je Typ

Dieselbe Aggregation mit `WHERE MessageBAMType = ?`. **`EXPLAIN` je Typ** — hier greift der
zusammengesetzte Index, anders als in M49‑2a:

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---:|---|---:|---|
| 1 | `MessageBAM` | **`ref`** | **`MessageBAM_BAMValue`** | 2 | `const` | 5.429.833 | `Using where; Using index; Using temporary; Using filesort` |

`key_len` 2 ist der `smallint`-Typ allein: Der Index `(MessageBAMType, MessageBAMValue)` trägt den
Filter, die Gruppierung über den Ausdruck bleibt eine temporäre Tabelle.

| Typ | Beschreibung | tragend bei (M39) | Zeilen | **k = 4**: Präfixe / schlimmster / ≥1k / ≥10k | **k = 6**: Präfixe / schlimmster / ≥1k / ≥10k |
|---:|---|---|---:|---|---|
| **9018** | Kundenmaterialnummer_K_SAP | `NEXANS` | 2.311.236 | 1.401 / **766.103** / 257 / 26 | 3.758 / **81.876** / 331 / 40 |
| **0** | Bestellnummer | `IBIS`, `IBISGUS` | 167.463 | 3.699 / **25.227** / 39 / 1 | 13.506 / **1.801** / 21 / 0 |
| **2000** | OrderNumber | `SUTTONS` | 12.434 | 487 / **250** / 0 / 0 | 1.755 / **59** / 0 / 0 |
| **2001** | VendorReference | `SUTTONS` | 11.191 | 12 / **7.048** / 3 / 0 | 56 / **7.048** / 1 / 0 |
| **3** | Rechnungsnummer | `ZAST` | 2.268.697 | 432 / **9.917** / 364 / 0 | 41.767 / **194** / 0 / 0 |
| **2002** | InvoiceNumber VTG | `VOTG` | 456 | 4 / **326** / 0 / 0 | 4 / **326** / 0 / 0 |
| **9014** | Lieferantennummer beim Kunden_K_SAP | `WOC` | 432.227 | 72 / **234.159** / 23 / 5 | 86 / **234.159** / 25 / 5 |
| **9006** | Lieferschein-Nr._L_SAP | kuratiert seit Schritt 4 | 155.809 | 1.586 / **4.139** / 13 / 0 | 23.365 / **149** / 0 / 0 |
| **9015** | Kundenwerk_K_SAP | `NEXANS` | 435.690 | 284 / **62.011** / 54 / 7 | 285 / **62.011** / 54 / 7 |

**Die Antwort ist je Typ eine andere, und zwar nicht graduell:**

| Wo die schlimmste Trefferzahl bei **k = 6** landet | Typen |
|---|---|
| **unter 1.000** — eine Mindestlänge von 6 trüge | 2002 (326), 3 (194), 9006 (149), 2000 (59) |
| **zwischen 1.000 und 10.000** | 0 (1.801), 2001 (7.048) |
| **fünf- bis sechsstellig — keine Länge hilft** | 9015 (62.011), 9018 (81.876), **9014 (234.159)** |

**Drei Typen haben einen Boden, den mehr Zeichen nicht senken:** 2001 bleibt bei 7.048, 9015 bei
62.011 und 9014 bei 234.159, unverändert von k = 4 auf k = 6. Bei ihnen steckt die Trefferzahl in
**einem exakten Wert**, und ein Präfix kann nicht feiner werden als der Wert selbst.

### Befunde — vor der Messung formuliert

| Befund | trifft zu | Konsequenz |
|---|---|---|
| **Es gibt eine Präfixlänge, ab der die schlimmste Trefferzahl unter 1.000 fällt** | **nein — typlos in keiner Länge** | Bei k = 6 steht sie bei **234.159**, und tiefer kann sie nie fallen: Ein einziger *exakter* Wert trägt diese Zeilen (M33). **Eine Mindestlänge ist als Schutz typlos wertlos** |
| **Es gibt sie nicht innerhalb brauchbarer Längen** | **ja — und auch außerhalb nicht** | **Dann schützt nur die Trefferzahl selbst**, und die Mindestlänge bleibt draußen — wie in Teil 2b, aber **aus einem neuen Grund**: dort, weil die Werte je Typ 1 bis 35 Zeichen lang sind (M38); hier, weil auch eine erfüllte Mindestlänge den Bösfall nicht verkleinert |
| **Die Grenze liegt je Typ verschieden** | **ja, um Faktor 3.969** | Bei k = 6 zwischen **59** (2000) und **234.159** (9014). Eine Mindestlänge müsste je Typ gelten — **und dann ist sie nur durchsetzbar, wenn der Nutzer einen Typ gewählt hat. Die Suche ist typlos voreingestellt (M36).** Diese Spannung wird hier benannt und **nicht aufgelöst**: Ein je Typ verschiedener Schutz, der in der Voreinstellung nicht greift, ist kein Schutz, sondern eine Bedienbedingung |
| **Die typlose und die typgebundene Grenze fallen zusammen** | **nein** | Typlos liegt die schlimmste Trefferzahl bei k = 4 bei **1.332.180**, der schlimmste Einzeltyp bei **766.103**. Der typlose Fall ist durchgängig der schlechtere — was zu erwarten war und trotzdem gemessen gehört, weil er die Voreinstellung ist |
| **Die schlimmsten Präfixe gehören zu Kennungsfeldern und nicht zu Belegnummern** | **überwiegend ja — aber nicht durchgängig** | Sieben der zehn schlimmsten Paare gehören Kennungsfeldern (Lieferplan-, Kundenmaterial-, Lieferanten-, Material-Nr.) und decken sich mit M33 (9014, 9000, 9005, 9036). **Drei gehören 9019 „Bestellnummer vom Kunden"** — siehe den Absatz unten |

**Wo die vorformulierte Zeile nicht passt (3) — der Boden.** Keine der fünf Zeilen fragt, ob die
schlimmste Trefferzahl eine **untere Schranke** hat. Sie hat eine, und die Schranke ist der Kern der
ganzen Frage: Weil ein exakter Wert 234.159 Zeilen trägt, ist jede Präfixlänge nach oben durch
diesen Wert begrenzt. **Eine Mindestlänge kann den Bösfall nicht wegregeln, weil der Bösfall kein
Präfixproblem ist.** Er ist bereits im exakten Bestand da — und die exakte Suche lebt heute damit,
weil Zeitfenster und Limit ihn auffangen (M35, M47).

**Wo die vorformulierte Zeile nicht passt (4) — 9019 ist eine Belegnummer.** Die Zeile erwartet den
sauberen Schnitt „Kennungen gefährlich, Belegnummern harmlos". **Drei der zehn schlimmsten Paare
tragen Typ 9019 „Bestellnummer vom Kunden"** mit 410.030, 190.216 und 161.067 Zeilen. Eine
Bestellnummer ist genau das, was ein Nutzer in das Suchfeld tippt — der Schnitt trennt also nicht
die gefährlichen von den gesuchten Feldern. Der Grund ist sichtbar, sobald man die Zahl
danebenhält: 9019 hat innerhalb seiner Werte offenbar einen sehr gleichförmigen Anfang. **Für die
Entscheidung heißt das, dass „gefährlich sind nur die Kennungsfelder" als Beruhigung nicht trägt.**

### Belegvermerk (Regel L10)

> *Gemessen:* Die Verteilung der BAM-Werte auf Präfixe der Längen 3, 4, 5 und 6 über den **gesamten
> Bestand** (n = 15.406.350 Zeilen, Vollerhebung, kein Zeitfenster, kein Mandantenfilter), dazu die
> Bänder bei k = 4 und k = 6 und dieselbe Erhebung je Typ für **neun** Typen bei k = 4 und k = 6.
> Die Reihe bricht bei k = 8 an der 60‑Sekunden-Grenze ab.
>
> *Behauptet wird:* dass eine Mindestlänge die Präfixsuche nicht absichern kann.
>
> **Die Lücke, und sie ist vierfach.** Erstens ist die schärfste Aussage — der Boden bei 234.159 —
> **kein Messergebnis dieser Runde**, sondern ein Schluss aus M49‑2a (k = 6) und M33 (der häufigste
> exakte Wert). Der Schluss ist zwingend, aber er ist einer. Zweitens sind die Trefferzahlen
> **Kandidatenmengen über den Bestand**: Was der Nutzer sähe, ginge zusätzlich durch Mandantenfilter,
> Zeitfenster und Limit 50 — die Zahl bestimmt den **Preis**, nicht die Antwortlänge. Drittens sagt
> keine Zahl, **welche** Präfixe Nutzer tatsächlich eingeben; gemessen ist der schlimmste Fall, nicht
> der wahrscheinliche. Und viertens ist der Abbruch bei k = 8 eine echte Lücke für alles, was nicht
> aus dem Boden folgt — etwa die Frage, wie schnell die **Zahl** der gefährlichen Präfixe zwischen
> k = 6 und k = 10 abnimmt. Was trägt: *Typlos gibt es keine schützende Mindestlänge.* Was **nicht**
> gemessen ist: *ob eine typgebundene Mindestlänge im Betrieb überhaupt greifen würde, weil die
> Voreinstellung typlos ist.*

---

## M49‑3 Das gebaute Statement mit `LIKE` statt `IN`

**Frage.** Wie verhält sich der **gebaute** Endpunkt, wenn nur das Wertprädikat getauscht wird?

### Statement

Gemessen ist der von jOOQ **gerenderte** Text aus `BamSucheRepository`, gegen eine jOOQ-Attrappe
erzeugt wie in M47 und in `BamSucheStatementsTest`. **Ersetzt ist ausschließlich das Wertprädikat**
— `` `b1`.`MessageBAMValue` in (?) `` wird zu `` `b1`.`MessageBAMValue` like concat(?, '%') ``.
Mandantenfilter als `EXISTS`, `GROUP BY`, Deckelung auf 51 und die vier Anzeigetabellen **über** der
Deckelung stehen unverändert. **Die Repository-Klasse ist dabei nicht geändert worden** (siehe die
Abnahme unten).

```sql
select `treffer`.`MessageID`, `treffer`.`MessageLastUpdate`, `treffer`.`MessageStatus`,
       `treffer`.`ProcessID`, `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`,
       `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`,
       `treffer`.`Source`, `treffer`.`SourceMessageID`, `treffer`.`TargetMessageID`, `treffer`.`Target`
from (select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`,
             `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`,
             `GlassfishDB`.`Message`.`SOSID`, `GlassfishDB`.`Message`.`SOSActionID`,
             `GlassfishDB`.`Message`.`Source`, `GlassfishDB`.`Message`.`SourceMessageID`,
             `GlassfishDB`.`Message`.`TargetMessageID`, `GlassfishDB`.`Message`.`Target`
      from `GlassfishDB`.`MessageBAM` as `b1`
      join `GlassfishDB`.`Message` on `GlassfishDB`.`Message`.`MessageID` = `b1`.`MessageID`
      where (`b1`.`MessageBAMValue` like concat(@w_typisch,'%')          -- HIER, und nur hier
             and `GlassfishDB`.`Message`.`MessageLastUpdate` >= '2025-11-30 00:00:00'
             and `GlassfishDB`.`Message`.`MessageLastUpdate` <= '2025-12-30 00:00:00'
             and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process`
                         join `GlassfishDB`.`ProjectMandant`
                           on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID`
                         where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID`
                                and `GlassfishDB`.`ProjectMandant`.`MandantID` = 'NEXANS')))
      group by `GlassfishDB`.`Message`.`MessageID`
      order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc
      fetch next 51 rows only) as `treffer`
left outer join `GlassfishDB`.`Process`   on `GlassfishDB`.`Process`.`ProcessID` = `treffer`.`ProcessID`
left outer join `GlassfishDB`.`Project`   on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID`
left outer join `GlassfishDB`.`SOS`       on `GlassfishDB`.`SOS`.`SOSID` = `treffer`.`SOSID`
left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `treffer`.`SOSID`
                                          and `GlassfishDB`.`SOSAction`.`SOSActionID` = `treffer`.`SOSActionID`)
order by `treffer`.`MessageLastUpdate` desc, `treffer`.`MessageID` desc
```

### Die Prüfwerte

Hergeleitet wie in M34, M42‑0 und M47‑1, je Sitzung neu. Abgedruckt sind die Eigenschaften:

| Rolle | Typ | Länge | exakt | **als Präfix** |
|---|---:|---:|---:|---:|
| der typische | 9017 | 8 | **1** | **23** |
| … auf 6 Zeichen verkürzt | | 6 | — | **5.425** |
| … auf 4 Zeichen verkürzt | | 4 | — | **155.871** |
| der schlimmste | 9014 | 8 | **234.159** | — |
| selten (Ankernachricht) | 9022 | 12 | 1 | — |
| häufig (Ankernachricht) | 9016 | 8 | 1.182 | — |
| `IBIS`, typisch | 0 | 10 | **1** | **1** |
| `IBIS`, um zwei verkürzt | | 8 | — | **1** |

### `EXPLAIN`

Der Index und die Gestalt bleiben; die Zugriffsart wechselt.

| Fall | führende Tabelle | `type` | `key` | `key_len` | `rows` |
|---|---|---|---|---:|---:|
| typischer Wert, vollständig als Präfix | `b1` | **`range`** | `MessageBAM_BAMValueOnly` | 282 | **23** |
| … um zwei Zeichen verkürzt | `b1` | `range` | `MessageBAM_BAMValueOnly` | 282 | 13.086 |
| … um vier Zeichen verkürzt | `b1` | `range` | `MessageBAM_BAMValueOnly` | 282 | **383.068** |
| schlimmster Wert, vollständig | `b1` | `range` | `MessageBAM_BAMValueOnly` | 282 | **443.830** |
| zwei Begriffe, **seltener zuerst** | **`b1`** | `range` | `MessageBAM_BAMValueOnly` | 282 | **1** |
| zwei Begriffe, **seltener zuletzt** | **`b2`** | `range` | `MessageBAM_BAMValueOnly` | 282 | **1** |
| fünf Begriffe, **seltener zuerst** | **`b1`** | `range` | `MessageBAM_BAMValueOnly` | 282 | **1** |
| fünf Begriffe, **seltener an fünfter Stelle** | **`b5`** | `range` | `MessageBAM_BAMValueOnly` | 282 | **1** |
| Nullen im Muster, vier `LIKE` im selben `OR` | `b1` | `range` | `MessageBAM_BAMValueOnly` | 282 | 5 |
| `IBIS`, vollständig / verkürzt | `b1` | `range` | `MessageBAM_BAMValueOnly` | 282 | 1 |

Die äußere Abfrage ist in **jedem** Fall unverändert: `<derived2>` als `ALL` mit höchstens 51 Zeilen,
danach vier `eq_ref` auf `PRIMARY`. **Die Deckelung steht, wo sie stand.**

### Laufzeiten

Alle Werte in Millisekunden, beste von fünf nach einem Aufwärmlauf; bei Statements über einer
Sekunde beste von drei nach einem Aufwärmlauf.

| Fall | Kandidaten­zeilen | Treffer | **30 Tage** | **ein Jahr** | M47 mit `IN` | Aufschlag |
|---|---:|---:|---:|---:|---:|---:|
| **typischer Wert, vollständig als Präfix** | 23 | **1** (30 T) · **23** (Jahr) | **1,374** | **1,585** | 1,095 / 1,089 | **+25,5 %** |
| **… um zwei Zeichen verkürzt** | 5.425 | 51 (gedeckelt) | **49,863** | — | — | — |
| **… um vier Zeichen verkürzt** | 155.871 | 51 (gedeckelt) | **1.361,436** | **3.399,650** | — | — |
| **der schlimmste Wert, vollständig** | 234.159 | 51 (gedeckelt) | **1.823,054** | — | 1.655,827 | **+10,1 %** |
| zwei Begriffe, selten × häufig | — | 1 | **1,270** | — | 1,209 | +5,0 % |
| … umgekehrte Reihenfolge | — | 1 | **1,279** | — | 1,202 | +6,4 % |
| fünf Begriffe, seltener zuerst | — | 1 | **1,840** | — | 1,622 | +13,4 % |
| fünf Begriffe, seltener zuletzt | — | 1 | **1,910** | — | 1,607 | +18,9 % |
| **Nullen im Muster, vier `LIKE` im `OR`** | 5 | 0 | **1,205** | — | — | — |
| `IBIS`, vollständig als Präfix | 1 | 1 | **1,177** | — | 1,185 | −0,7 % |
| `IBIS`, um zwei Zeichen verkürzt | 1 | 1 | **1,177** | — | 1,214 | −3,0 % |

### Befunde — vor der Messung formuliert

| Befund | trifft zu | Konsequenz |
|---|---|---|
| **Der Index bleibt `MessageBAM_BAMValueOnly`, die Zugriffsart ist `range`** | **ja, in allen elf Fällen** | Wie bei der `IN`-Liste mit mehreren Varianten (M47). **Die Umstellung ändert den Plan nicht grundsätzlich** |
| **Der Optimierer steigt weiter über den seltensten Begriff ein** | **ja** | Belegt am `EXPLAIN` (`b1` / `b2` / `b5` je nach Stellung) **und** an der Laufzeit: 1,270 gegen 1,279 ms bei zwei, 1,840 gegen 1,910 ms bei fünf Begriffen. **M42‑1 und M47 gelten weiter, kein `STRAIGHT_JOIN`.** Regel L15 ist geprüft und nicht angenommen |
| **Er tut es nicht mehr, weil die `rows`-Schätzung eines `LIKE`-Bereichs schlechter ist als die einer `IN`-Liste** | **nein** | Die Schätzung bleibt `rows` 1 auf dem seltenen Begriff. Die Verundung mit Präfix ist **dieselbe** Bauform, und M42/M47 tragen sie |
| **Die Deckelung auf 51 rettet die Laufzeit des verkürzten Präfixes** | **nein — wie erwartet** | 1.361 ms für 51 angezeigte Zeilen. Sortierung und Gruppierung liegen **vor** der Deckelung; alle 155.871 Kandidatenzeilen müssen erzeugt werden. Die Laufzeit folgt der Kandidatenzahl, nicht der Trefferzahl |
| **Der Vierzeichenfall bleibt mit 30 Tagen unter einer Sekunde** | **nein — 1,361 s** | E6 misst 1,299 s über Fenster B **ohne** die Joins der Anwendung; der gebaute Aufbau legt 4,8 % dazu. **Mit einem Jahr sind es 3,400 s.** Beide bleiben unter der 10‑Sekunden-Grenze des Lese-Pools — der Vierzeichenfall ist also **teuer, aber nicht tödlich** |
| **`IBIS` verhält sich wie `NEXANS`** | **ja** | 1,177 ms in beiden Fällen, sogar minimal **unter** den `IN`-Zahlen aus M47. **Regel L7 erfüllt** — und mit einem Zusatzbefund: Bei `IBIS` findet das um zwei Zeichen verkürzte Präfix **denselben einen** Treffer. Der Bösfall der Verkürzung ist dort so wenig vorhanden wie der Bösfall der Trefferzahl (M42‑4) |

**Wo die vorformulierte Zeile nicht passt (5) — schon der *vollständige* Wert als Präfix findet 23
statt 1.** Keine Zeile des Auftrags stellt diese Frage; sie behandelt „vollständiger Wert als
Präfix" als den Fall, in dem Präfix und exakt dasselbe liefern. **Sie liefern es nicht.** Gemessen
über den Bestand: derselbe achtstellige Prüfwert hat **exakt einen** Treffer und **als Präfix 23** —
weil 22 längere Werte mit ihm beginnen. Über 30 Tage bleibt bei `NEXANS` davon eine Nachricht übrig,
**über ein Jahr sind es 23**.

Das entwertet M34 nicht, es präzisiert es. M34s Zeile „Präfix kostet 3 bis 27 % mehr" trägt den
Zusatz **„aber nur bei gleicher Trefferzahl"** — und E6s Tabellenzeile „8 (vollständig) → 1 Treffer"
ist die **exakte** Zählung aus M34, nicht die eines `LIKE`. Die Bedingung „gleiche Trefferzahl" ist
also **bei keiner Eingabelänge erfüllt**, nicht einmal bei der vollständigen. Für die Leistung ist
das folgenlos (23 Zeilen sind nichts); für die **Fachlichkeit** ist es der Kern: *Eine Präfixsuche
liefert auch bei vollständig eingetippter Belegnummer ein anderes Ergebnis als heute.*

### Belegvermerk (Regel L10)

> *Gemessen:* Die Laufzeit des **gebauten** Statements mit getauschtem Wertprädikat über **elf**
> Fälle — neun bei `NEXANS`, zwei bei `IBIS` —, je mit 30‑Tage- und in drei Fällen mit Jahresfenster,
> auf einer ruhenden Testkopie; dazu **elf** `EXPLAIN`. Die Prüfwerte stammen aus **drei**
> hergeleiteten Quellen (n = 3): dem typischen und dem schlimmsten Wert von `NEXANS` aus M34, der
> Ankernachricht aus M42‑0 und einer `IBIS`-Nachricht.
>
> *Behauptet wird:* dass eine Präfixsuche denselben Zugriffspfad nimmt wie die heutige exakte Suche
> und dass ihre Kosten allein an der Kandidatenzahl hängen.
>
> **Die Lücke, und sie ist vierfach.** Erstens gilt die 10‑Sekunden-Grenze in **Produktion**, und
> gemessen ist eine **ruhende** Testkopie — dieselbe benannte Lücke wie im Belegvermerk zu M35 und
> M47; die Zahlen sind Untergrenzen und keine Zusagen. Zweitens ist der teuerste gemessene Fall
> **nicht der schlimmste denkbare**: Gemessen ist der schlimmste *Wert* (234.159) und das auf vier
> Zeichen verkürzte Präfix *eines* Werts (155.871) — M49‑2a kennt einen Präfix mit **1.332.180**
> Zeilen, und der ist **nicht** durch das Statement gelaufen. Drittens ist die Nullen-im-Muster-Fassung
> mit **vier** `LIKE` gemessen, nicht mit sieben, und bei **einem** Begriff, nicht bei acht. Und
> viertens misst M49‑3 die **Statements** und nicht den **Aufruf**. Was trägt: *Mit Präfix statt
> exakt kostet jeder gemessene Normalfall unter 2 ms und der schlimmste bekannte Wert 1,8 s über 30
> Tage.* Was **nicht** gemessen ist: *derselbe Satz für den schlimmsten Präfix des Bestands, für
> sieben Fassungen und unter Last.*

---

## M49‑4 Sonderzeichen im Bestand

**Frage.** Mit `=` sind `%` und `_` in der Eingabe harmlos. Mit `LIKE` werden sie zu Platzhaltern.
Kommen sie im Bestand vor — und kann ein BAM-Wert einen Doppelpunkt tragen?

> **Regel L9 ist berührt**, mit derselben Begründung wie bei M49‑2a: Die Frage gilt dem **Bestand**.
> Ein Zeitfenster sagte, welche Zeichen *ein Monat* enthält, und für eine Eingabekennzeichnung, die
> dauerhaft im Code steht, ist das die falsche Grundgesamtheit. **Die Sonde über eine einzige Spalte
> kostet 7,798 s**, das vollständige Statement **21,412 s** — beide unter der Grenze.

### Statement

```sql
SELECT COUNT(*)                                       AS zeilen,
       SUM(MessageBAMValue LIKE '%#%%' ESCAPE '#')    AS mit_prozent,
       SUM(MessageBAMValue LIKE '%#_%' ESCAPE '#')    AS mit_unterstrich,
       SUM(MessageBAMValue LIKE '%*%')                AS mit_stern,
       SUM(MessageBAMValue LIKE '%:%')                AS mit_doppelpunkt,
       SUM(MessageBAMValue LIKE '% ')                 AS folgendes_leerzeichen
FROM MessageBAM;
```

**`EXPLAIN`:** `MessageBAM`, `type` `index`, `key` `PRIMARY`, `key_len` 430, `rows` 10.859.666,
`Using index` — ein vollständiger, index-naher Durchlauf ohne temporäre Tabelle.

### Ergebnis

| | Zeilen | Anteil |
|---|---:|---:|
| `MessageBAM` insgesamt | **15.406.350** | 100 % |
| **mit `%`** | **0** | **0 %** |
| mit `_` | **2.696** | 0,0175 % |
| mit `*` | **1.738** | 0,0113 % |
| **mit `:`** | **585** | **0,0038 %** |
| **mit folgendem Leerzeichen** | **602.794** | **3,913 %** |

**Die Zeile mit dem folgenden Leerzeichen ist gegengeprüft**, in einer Form **ohne** `LIKE` — weil
die PAD-SPACE-Regel in diesem Projekt schon einmal eine Zahl verdorben hat (M46‑0):

```sql
SELECT SUM(LENGTH(MessageBAMValue) <> LENGTH(TRIM(TRAILING ' ' FROM MessageBAMValue)))
         AS folgendes_leerzeichen_ohne_like,
       SUM(LENGTH(MessageBAMValue) = 0) AS leere_werte
FROM MessageBAM;
```

**602.794** — Zeichen für Zeichen dieselbe Zahl. Und **`leere_werte` ist `0`**, was M46 bestätigt:
Einen leeren BAM-Wert gibt es im ganzen Bestand nicht.

### Der Beleg statt der Behauptung

Ohne Tabellenzugriff, damit die `ESCAPE`-Semantik nicht behauptet, sondern gezeigt ist:

| Ausdruck | Ergebnis |
|---|:---:|
| `'50%' LIKE '50%'` | **1** — das `%` der Eingabe ist ein Platzhalter |
| `'50%' LIKE '50#%' ESCAPE '#'` | **1** — maskiert trifft es das Zeichen selbst |
| `'5_0' LIKE '5#_0' ESCAPE '#'` | **1** |
| `'5X0' LIKE '5#_0' ESCAPE '#'` | **0** — maskiert trifft `_` **nicht** jedes Zeichen |
| `'5X0' LIKE '5_0'` | **1** — unmaskiert schon |

### Der Doppelpunkt — der Punkt ist **bestätigt**, nicht geschlossen

[`bam-suche.md`](bam-suche.md) §1 führt als ausdrücklich ungemessen, **ob ein BAM-Wert selbst einen
Doppelpunkt tragen kann**; die Parameterform `<typ>:<wert>` weicht dem aus, indem sie am **ersten**
Doppelpunkt teilt.

**Er kann: 585 BAM-Werte tragen einen Doppelpunkt.** Damit ist der offene Punkt **bestätigt und
nicht geschlossen** — und die Bauentscheidung von Teil 2b war die richtige. Hätte die Anwendung am
**letzten** Doppelpunkt geteilt oder den Trenner freigestellt, wären diese 585 Werte unsuchbar oder
falsch zerlegt worden. Die Teilung am ersten Doppelpunkt ist damit **kein Vorsichtsmaßnahme mehr,
sondern eine gemessen notwendige**.

### Was `%`, `*` und `_` für eine Präfixkennzeichnung bedeuten

Käme die Präfixsuche, bräuchte sie eine Form, in der der Nutzer sie **anfordert** — etwa ein
angehängtes `*`. Dazu gehört die Auskunft, ob das Zeichen im Bestand vorkommt:

| Zeichen | Zeilen | als Kennzeichen brauchbar? |
|---|---:|---|
| **`%`** | **0** | **ja** — es kommt im ganzen Bestand nicht vor. Es ist zugleich das Zeichen, das `LIKE` ohnehin benutzt |
| `*` | 1.738 | **nur mit Regel**: 1.738 Werte trügen es selbst. Ein Wert, der auf `*` endet, wäre nicht mehr eindeutig als „exakt" formulierbar |
| `_` | 2.696 | als Kennzeichen ungeeignet; **als Eingabezeichen aber gefährlich**, weil `LIKE` es als Platzhalter liest — 2.696 Werte tragen es |

**Unabhängig von der Kennzeichnung gilt: Sobald `LIKE` in das Statement kommt, braucht jede Eingabe
ein `ESCAPE`.** Die 2.696 Werte mit `_` sind der Beweis, dass die Falle nicht theoretisch ist: Wer
heute einen solchen Wert exakt sucht, findet ihn; mit `LIKE` ohne `ESCAPE` fände er zusätzlich alle
Werte, die an dieser Stelle ein beliebiges Zeichen tragen. Es ist dieselbe Falle wie
`LIKE 'ERROR\_%' ESCAPE '\'` in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1 und
Regel Q1.

### Die Randleerzeichen — die Folgerung, nicht die Messung

M43‑3 hat die Semantik zweifach belegt und wird hier **nicht wiederholt**. Was dazugehört, ist die
**Konsequenz**, und sie ist jetzt beziffert:

| Ausdruck | Ergebnis | was das heißt |
|---|:---:|---|
| `'123 ' = '123'` | **1** | Mit `=` ist ein folgendes Leerzeichen **im Bestand** harmlos (PAD SPACE) |
| `'123' = '123 '` | **1** | … und ebenso eines **in der Eingabe** |
| `'123 ' LIKE '123'` | **0** | `LIKE` folgt der PAD-SPACE-Regel **nicht** |
| `'123 ' LIKE '123%'` | **1** | Ein Präfixmuster findet den Wert mit folgendem Leerzeichen |
| **`'123' LIKE '123 %'`** | **0** | **Aber eine Eingabe *mit* folgendem Leerzeichen findet den Wert ohne nicht mehr** |

**Ein Trim auf der Eingabe wird also Pflicht, sobald Präfix dazukommt** — und die Zahl daneben sagt,
wie groß der Bestand ist, der an dieser Regel hängt: **602.794 Werte (3,91 %) tragen selbst ein
folgendes Leerzeichen.** Heute fängt PAD SPACE beide Richtungen ab; mit `LIKE` fällt die eine
Richtung weg.

**Wo die vorformulierte Zeile nicht passt (6).** Der Auftrag stellt die Randleerzeichen ausdrücklich
als *Folgerung* und nicht als Messung — sein eigenes Statement zählt sie aber mit, und die Zahl ist
größer, als eine Folgerung nahelegt. **3,91 Prozent des Bestands sind kein Randfall.** Zum Vergleich:
`bam_sollaenge` führt das **führende** Leerzeichen für zwei Paare mit 1,349624 % (9018) und
0,000100 % (9020) — das folgende ist über den ganzen Bestand fast dreimal häufiger als das führende
beim auffälligsten Typ. Es steht heute in keiner Kuratierung, weil es keine braucht; mit `LIKE`
bräuchte es eine Regel.

### Belegvermerk (Regel L10)

> *Gemessen:* Über den **gesamten Bestand** (n = 15.406.350, Vollerhebung, kein Zeitfenster) das
> Vorkommen von `%`, `_`, `*`, `:` und folgendem Leerzeichen; die Zeile zum folgenden Leerzeichen
> zusätzlich in einer zweiten, `LIKE`-freien Form. Dazu neun Ausdrücke ohne Tabellenzugriff für die
> `ESCAPE`- und die PAD-SPACE-Semantik.
>
> *Behauptet wird:* dass ein BAM-Wert einen Doppelpunkt tragen kann, dass `%` als Kennzeichen frei
> ist und dass ein Trim auf der Eingabe mit `LIKE` zur Pflicht wird.
>
> **Die Lücke, und sie ist dreifach.** Erstens ist „`%` kommt nicht vor" eine Aussage über **diese
> Testkopie mit Stand 08.07.2026** — nicht über die Produktion und nicht über morgen; ein einziger
> neuer Wert mit `%` machte die Kennzeichnung mehrdeutig, und nichts im Quellsystem verbietet ihn.
> Zweitens sagt die Zahl 585 **nicht**, dass diese Werte heute unsuchbar wären: Sie sind es nicht,
> weil die Teilung am ersten Doppelpunkt sie korrekt zerlegt — gemessen ist die **Existenz**, nicht
> ein Schaden. Und drittens sind die 602.794 folgenden Leerzeichen **typlos** gezählt; welche Typen
> sie tragen und ob es dieselben sind, die **M43‑3** mit 24,83 % *folgenden* bei 9018 ausweist (von
> den 25,88 % Randleerzeichen aus M38), ist **nicht** erhoben.
> Was trägt: *`%` ist frei, `:` `*` `_` sind es nicht, und ein Trim wird Pflicht.* Was **nicht**
> gemessen ist: *ob das für die Produktion ebenso gilt.*

---

## Für das Sparring

Ohne Zahlenkolonnen, in drei Punkten.

**1. Technisch tragbar — ja, aber nur mit einem Zusatzbau, den es heute nicht gibt.** Der
Zugriffspfad ändert sich nicht, der Optimierer wählt weiter richtig, und die üblichen Fälle bleiben
im Millisekundenbereich. Was die Präfixsuche kostet, ist nicht die Suchform, sondern die Zahl der
Kandidatenzeilen — und die steigt mit jedem weggelassenen Zeichen dramatisch. Entscheidend ist
etwas anderes: **Ohne Zusatzbau findet eine Präfixsuche bei den kuratierten Typen gar nichts.**
Auffüllen und Präfix ankern beide vorn und schließen einander aus. Der gemessene Ausweg ist, die
Nullen in das Muster zu ziehen; er funktioniert in jedem geprüften Fall und kostet drei bis sieben
Muster je Begriff statt der heutigen fünf Fassungen insgesamt.

**2. Nicht tragbar ist sie dort, wo der Wertevorrat einen gleichförmigen Anfang hat.** Bei den
Typen, deren Werte alle ähnlich beginnen — Kennungsfelder wie die Lieferantennummer, die
Kundenmaterialnummer, das Kundenwerk —, bleibt die schlimmste Trefferzahl auch bei sechs Zeichen
sechsstellig, und mehr Zeichen senken sie nicht mehr, weil sie in einem einzelnen exakten Wert
steckt. **Eine Mindestlänge löst das Problem also nicht**, und sie wäre ohnehin nur je Typ
formulierbar — während die Suche typlos voreingestellt ist. Zwei Typen sind zusätzlich heikel, weil
ihre Belegnummern nach dem Abziehen der führenden Nullen nur ein bis drei Zeichen übrig lassen; dort
erzeugt schon die ungekürzte Eingabe über eine Million Kandidatenzeilen. Und ein Feld, das man für
harmlos hielte, gehört auf diese Liste: die Bestellnummer vom Kunden.

**3. Beim Auftraggeber liegen drei Entscheidungen, und keine davon entscheidet diese Runde.** Erstens:
ob die Präfixsuche überhaupt kommt — die Zahlen sagen, dass sie machbar und dass sie teuer ist, aber
nicht, ob der Nutzen sie rechtfertigt; **die Zahl, die das entschiede, existiert nicht** (siehe unten,
Punkt 1 der Lücken). Zweitens: falls ja, ob sie **immer** gilt oder nur als zweite Geste im
Nulltreffer-Fall — die zweite Form kostet nichts, solange sie nicht ausgelöst wird. Drittens, und das
ist die unangenehmste: **Eine Präfixsuche verändert die Antwort auch dort, wo der Nutzer vollständig
tippt.** Schon der ganze Wert als Muster findet mehr als die exakte Suche. Wer Präfix zuschaltet,
tauscht nicht Bequemlichkeit gegen Laufzeit, sondern ändert, was „gefunden" heißt.

---

| Frage | Antwort |
|---|---|
| Ist der Index auf `MessageBAMValue` ein Präfixindex? | **Nein — Vollindex über 70 Zeichen**, wie alle fünf Indizes der Tabelle (M32) |
| Gibt es einen Index, der den Typ mitträgt? | **Ja**, `(MessageBAMType, MessageBAMValue)` — aber mit dem **Typ voran**, für die typlose Suche also unbrauchbar (M32) |
| Wie groß ist `MessageBAM` wirklich? | **15.406.350 Zeilen**, nicht die dokumentierten 10.859.666 (M33‑0) |
| Wie viele Treffer hat ein Wert? | Median **1**, 99. Perzentil **75**, Maximum **234.159** (M33) |
| Wird die Rückfalloption A9 gebraucht? | **Nein.** Der direkte Weg trägt (M33, M34) |
| Was kostet die exakte Suche? | **0,8 ms** im Normalfall, **1,4 ms** am 99. Perzentil, **10,6 s** im schlimmsten Fall (M34) |
| Kostet Präfix mehr als exakt? | **Bei gleicher Trefferzahl nein** (+3 bis +27 %). Bei verkürztem Präfix ja, und zwar über die Trefferzahl: vier statt acht Zeichen → 155.871 statt 1 Treffer → 3,4 s statt 1 ms (M34, E6) |
| Was kostet `enthält`? | **40,2 s** für einen Wert mit **einem** Treffer. Ausgeschlossen (M34) |
| Braucht die Suche `STRAIGHT_JOIN`? | **Bei exakt und Präfix nein** — der Optimierer wählt von sich aus richtig. Bei `enthält` ja (Faktor 4,3), aber die Form fällt ohnehin weg (M34) |
| Schützt das Pflicht-Zeitfenster? | **Ja, und zwar deutlich** — aber nur eng: 24 h → 90 ms, ein Monat → 1,65 s, ein Jahr → 8,66 s, ohne → 10,75 s (M35) |
| Reißt der schlimmste Fall die Zeitgrenze des Lese-Pools? | **Ohne Fenster ja** (10,75 s > 10 s). Mit einem Jahr bleiben 13 % Reserve (M35) |
| Beschleunigt die Typeinschränkung? | **Nein.** Reine Ergebnisverfeinerung, +1,5 bis +4 % (M36) |
| Steht ein Wert unter mehreren Typen? | **Bei 4,17 % der Paare.** Nie mehr als vier Typen (M37) |
| Erklären 9032/9033 und 9022/9023 die Doppelungen? | **Nein — beide teilen null Werte.** Es ist 9028/9029 mit 15.790 (M37) |
| Ist exakte Suche fachlich benutzbar? | **Allein nicht.** Sechs Typen tragen auf **100 %** ihrer Werte eine führende Null, die kuratierte Lieferschein-Nr. auf 33,88 % (M38) |
| Tragen Werte Leerzeichen am Rand? | **Ja, bei 9018 auf 25,88 %** — dem tragenden Suchtyp von `NEXANS` (M38) |
| Trägt eine feste Mindestlänge? | **Nein.** Längen von 1 bis 35 Zeichen, je Typ verschieden (M38, E6) |
| Welcher Typ trägt je Mandant? | 9018 (`NEXANS`, 92,26 % der Wurzeln), Typ 0 (`IBIS` 92,59 %, `IBISGUS` 100 %), 2000/2001 (`SUTTONS`, 100 % der **Kinder**), Typ 3 (`ZAST`, 100 % der **Kinder**), 2002 (`VOTG`), 9014 (`WOC`) — **vier verschiedene Rollen** (M39) |
| Tragen Merge-Eingänge BAM-Werte? | **Null**, über Fenster B und alle Mandanten: 38.628 von 38.628 (M39, M41) |
| Ist `MessageBAMMandant` Bestand oder Sichtbarkeit? | **Sichtbarkeit.** `WOC` trägt 2.067 BAM-Zeilen unter einem Typ, der für ihn nicht konfiguriert ist (M40) |
| Wie viele Werte hat eine Nachricht? | Median **0**, auf Wurzeln **9**, 99. Perzentil **186** (Wurzel) und **1.345** (Merge-Ergebnis), Maximum **9.296** (M41) |
| Wie viele Typen hat eine Nachricht? | Median **8** unter denen mit Werten, Maximum **18** (M41) |
| Wer erzwingt die Deckelung? | **`ZAST`** — 441 BAM-Werte je Nachricht, der zweitkleinste Mandant (M40, M41) |
| Was kostet ein zusätzlicher Suchbegriff? | **0,094 ms** — 0,666 ms bei zwei, 0,947 ms bei fünf Begriffen (M42‑2) |
| Steigt der Optimierer über den seltensten Begriff ein? | **Ja, in jeder gemessenen Konstellation und in beiden Reihenfolgen** — solange kein `STRAIGHT_JOIN` im Weg steht (M42‑1) |
| Entlastet die Verundung? | **Gegenüber dem häufigeren Begriff um den Faktor 15.843** (0,672 ms gegen 10.646 ms). Gegenüber dem **selteneren** kostet sie 0,06 bis 16 ms mehr (M42‑1) |
| Gibt es einen Fall, in dem sie belastet? | **Ja: zwei Begriffe ähnlicher Häufigkeit.** K3a 27,3 ms gegen 11,1 ms allein — weil 9015 und 9016 auf denselben Nachrichten stehen (M42‑1, M37) |
| Braucht die Verundung `STRAIGHT_JOIN`? | **Nein — er ist hier der Schaden.** Mit falscher Reihenfolge Faktor 219 (K2) bis 1.094 (fünf Begriffe) (M42‑1, M42‑2) |
| Ist ein Zusatzbegriff immer fast umsonst? | **Nein.** Auf Nachrichten mit 2.499 BAM-Werten kostet er den **Faktor 112** — 926,7 ms gegen 8,3 ms (M42‑1 K5) |
| Warum? | Der Plan kippt von `MessageBAM_BAMValueOnly` (`rows` 1) auf `PRIMARY` (`rows` 8), und die 8 ist dort um den Faktor 312 zu klein (M42‑1) |
| Trägt die Bauform ohne Selbstjoin? | **Gegenläufig.** 954× langsamer bei fünf Begriffen mit einem häufigen, **87× schneller** auf fetten Nachrichten (M42‑3) |
| Wie verhält sich der kleine Mandant? | Genauso — und ohne Bösfall: bei `IBIS` ist die **größte** globale Trefferzahl **216** (M42‑4) |
| Gibt es je Typ eine Sollänge? | **Bei den sechs Typen mit durchgängig führender Null ja** (genau eine Länge, über den Bestand). Bei den tragenden Suchtypen **nein** — 9003 über 33 Längen (M43‑1) |
| Hält eine aus einem Monat abgeleitete Sollänge? | **Bei fünf von sechs ja, bei 2001 nicht**: 100 % über Fenster B, **67,21 %** über den Bestand (M43‑1) |
| Trägt dieselbe Nummer beide Schreibweisen? | **Ja.** Bei 9017 auf **52,98 %** der numerischen Werte, mit bis zu **fünf** Varianten; bei 9020 auf 20,10 % (M43‑2) |
| Sind die Leerzeichen bei 9018 ein Problem? | **Überwiegend nicht** — 24,83 % folgende sind unter PAD SPACE beim `=` unsichtbar. **1,05 % führende sind es** (M43‑3) |
| Ist das belegt oder behauptet? | Belegt: `'a ' = 'a'` → 1, `' a' = 'a'` → 0, `'a' LIKE 'a '` → 0. Und an echten Werten: 12 gegen 12 beim folgenden, **23 gegen 3** beim führenden (M43‑3) |
| Wirkt das Auffüllen? | **Bei sechs von acht Typen findet die rohe Fassung null Treffer**, die aufgefüllte 1 bis 3. Bei kurzen Kernen dreht es sich um: 9006 roh **1.642**, aufgefüllt **4** (M43‑4) |
| Wie groß ist `MessageProperty` wirklich? | **75.571.462 Zeilen**, nicht die dokumentierten 46.964.279 — die Schätzung lag **60,9 % zu niedrig** (M44) |
| Was kostet diese Zählung? | **199,380 s** kalt, **20,633 s** warm — Faktor 9,66. Der Optimierer wählt `PRIMARY` und liest damit die vollen 15,09 GB (M44) |
| Wie viele Eigenschaften hat eine Nachricht? | **22,62** über den Gesamtbestand — dasselbe wie die in M17 direkt gemessenen 22,57 und 22,88. Die „rund vierzehn" waren die Schätzung und **kein** Dichteeffekt (M44) |
| Wie viele Zeilen des Mengengerüsts waren gezählt? | **Eine von sieben** (`Message`). Auch `Process` (1.503 statt 1.490) und `Project` (140 statt 142) sind Schätzungen gewesen (M44) |
| Welche Endungen tragen die Typbeschreibungen? | **Genau drei** — `_L_SAP` (20 Typen), `_K_SAP` (13), `_FORS` (7); **22 von 62 tragen keine** (M45) |
| Bleiben die Beschreibungen ohne Endung eindeutig? | **Nein — 62 werden 57.** Drei Kollisionen über acht Typen, und zwei davon treffen die **endungslosen** Grundtypen 0 und 3 (M45) |
| Welche kollidieren? | `Abladestelle` (9000/9016/9025), `Bestellnummer` (0/9027/9034), `Rechnungsnummer` (3/9024) (M45) |
| Träfen zwei gleich beschriftete Gruppen je zusammen? | **Ja.** 9000 und 9016 stehen über Fenster B auf **3.405 Nachrichten gemeinsam** — 11,3 % aller Nachrichten mit `Abladestelle` (M45) |
| Meint die Endung das System und nicht das Feld? | **Als Vermutung ja** — **jede** der drei Kollisionen paart *verschiedene* Endungen, keine dieselbe. Bestätigen kann das nur das Altsystem (M45) |
| Tragen BAM-Werte ein Leerzeichen **innen**? | **Ja, bei 2,97 %** — 27.792 von 936.529 über Fenster B, verteilt auf 16 Typen. Angeführt von 9016, 9003 und **9018**, dem tragenden Typ von `NEXANS` (E7) |
| Wie viele Typen kennt der **Bestand**? | **55**, nicht die 46 aus Fenster B. Die neun zusätzlichen sind winzig (1 bis 108 Zeilen) — drei davon tragen auf 100 % führende Nullen (M46‑1) |
| Wie viele Typen tragen über den Bestand eine dominante Länge ≥ 95 %? | **27 von 55.** Davon tragen **13** überhaupt Werte mit führender Null und sind damit Kandidaten (M46‑1) |
| **Ist die Sollänge je Mandant dieselbe?** | **Nein — an drei unabhängigen Stellen.** 2000 (`SUTTONS` Länge 6, `VOTG` Länge 7), 9014 (Bestand 58,45 %, `WOC` **95,21 %**), 2001 (`VOTG` ohne eine einzige führende Null). Der Schlüssel ist `(mandant_id, bam_typ)` (M46‑2) |
| Wie viele Paare werden kuratiert? | **14 von 45** gemessenen — plus zwei Zeilen für das führende Leerzeichen (M46‑2) |
| Wirken alle vierzehn? | **Zwölf.** Bei `IBIS`/1 und `SUTTONS`/2000 liegt kein Wert **mit** führender Null auf der Sollänge (M46‑1c, M46‑2c) |
| Trifft die Regel die kuratierte Lieferschein-Nr. 9006? | **Nein — 94,21 %**, 0,79 Prozentpunkte unter der Schwelle. Ausgerechnet den Typ, für den M43‑4 die Wirkung belegt hat (M46‑1) |
| Wie viele Typen tragen ein **führendes** Leerzeichen über den Bestand? | **Zwei.** 9018 mit 31.193 von 2.311.236 (**1,349624 %**), 9020 mit **einer** Zeile. Leerstrings gibt es **keine** (M46‑3) |
| **Was kostet das gebaute Statement im Normalfall?** | **1,1 ms** über 30 Tage — mit Normalisierung, Deckelung und allen Anzeigespalten (M47) |
| Was kostet der schlimmste Wert im gebauten Statement? | **1,656 s** über 30 Tage, **8,940 s** über ein Jahr. Faktor 6,0 Reserve gegen 11 % (M47) |
| **Fängt das 30-Tage-Fenster K3b und K5c?** | **Beide, und deutlich.** K3b 5.275 → **882 ms**, K5c 926,7 → **6,9 ms**. Offene Frage 10 ist beantwortet (M47) |
| Steigt der Optimierer auch im gebauten Statement über den seltensten Begriff ein? | **Ja, in jeder Konstellation** — auch wenn er an fünfter Stelle steht; der `EXPLAIN` führt dann `b5` an (M47) |
| Ändert die `IN`-Liste die Wahl der führenden Tabelle? | **Nein.** Die Zugriffsart wechselt von `ref` auf `range`, Index und Einstieg bleiben (M47) |
| Wie viele Suchvarianten entstehen bei `NEXANS` höchstens? | **Fünf**, und nur bei ein- bis zweistelliger Eingabe — drei *verschiedene* Sollängen auf acht kuratierten Zeilen (M47) |
| Was kostet die Normalisierung? | **Zehntelmillisekunden.** 0,956 → 1,221 ms von einer auf drei Fassungen; damit ist auch der Preis einer **wirkungslosen** Variante beziffert (M47) |
| **Was kostet es, die Anzeigespalten neben statt über der Deckelung zu hängen?** | **Faktor 1,48** — 2.443 gegen 1.656 ms, bei **identisch gutem** `EXPLAIN`. Derselbe Fehler wie in Teil 1, dort Faktor 18,4 (M47) |
| **Gehen Auffüllen und Präfixsuche zusammen?** | **Nein.** In **allen acht** prüfbaren Fällen findet die rohe Präfixfassung den aufgefüllten Originalwert **nicht** — beide ankern vorn (M49‑1) |
| Gibt es einen Ausweg? | **Ja: die Nullen ins Muster ziehen.** Findet in allen acht Fällen das Original — Preis sind **3 bis 7** Fassungen je Begriff statt heute fünf insgesamt (M49‑1) |
| Was kostet der Ausweg an Treffern? | **1 bis 1.245.618.** Zweistellig, solange der Kern lang ist; über eine Million, sobald er ein bis drei Zeichen hat (9036, 9006) (M49‑1) |
| **Gibt es eine schützende Mindestlänge?** | **Typlos nein, in keiner Länge.** Bei k = 6 steht die schlimmste Trefferzahl auf **234.159** und kann nie darunter fallen — ein einziger *exakter* Wert trägt diese Zeilen (M49‑2a, M33) |
| Wie viel bringen zwei Zeichen mehr? | **Sechs Prozent.** Präfixe ≥ 1.000 Treffer: 1.731 (k = 4) gegen 1.624 (k = 6), bei 18,7-fach mehr Präfixen (M49‑2a) |
| **Liegt die Grenze je Typ verschieden?** | **Ja, um Faktor 3.969.** Bei k = 6 zwischen **59** (2000) und **234.159** (9014). Durchsetzbar wäre sie nur mit gewähltem Typ — die Suche ist **typlos voreingestellt** (M49‑2b, M36) |
| Sind nur Kennungsfelder gefährlich? | **Nein.** Drei der zehn schlimmsten Präfixpaare gehören 9019 „Bestellnummer vom Kunden" (M49‑2a) |
| Ändert `LIKE` statt `IN` den Plan? | **Nein.** `MessageBAM_BAMValueOnly`, `range`, und der Optimierer steigt weiter über den seltensten Begriff ein — auch an fünfter Stelle (M49‑3) |
| Was kostet Präfix im gebauten Statement? | Normalfall **1,4 ms**, schlimmster Wert **1,823 s** über 30 Tage, vier Zeichen verkürzt **1,361 s** / **3,400 s** über ein Jahr (M49‑3) |
| **Findet der vollständige Wert als Präfix dasselbe wie exakt?** | **Nein — 23 statt 1.** M34s „bei gleicher Trefferzahl" ist bei **keiner** Eingabelänge erfüllt (M49‑3) |
| **Kann ein BAM-Wert einen Doppelpunkt tragen?** | **Ja, 585 tun es.** Der offene Punkt aus `bam-suche.md` §1 ist damit **bestätigt**, nicht geschlossen — und die Teilung am *ersten* Doppelpunkt gemessen notwendig (M49‑4) |
| Kommt `%` im Bestand vor? | **Nein, kein einziges Mal.** `_` steht in 2.696 Werten, `*` in 1.738 (M49‑4) |
| **Wie viele Werte tragen ein folgendes Leerzeichen?** | **602.794 — 3,91 %.** Mit `=` harmlos (PAD SPACE), mit `LIKE` nicht: Ein Trim auf der Eingabe wird Pflicht, sobald Präfix dazukommt (M49‑4, M43‑3) |
| **Was kostet der schlimmste bekannte Präfix im gebauten Statement?** | **3,851 s** über 30 Tage — und über ein Jahr **Abbruch an der 60‑Sekunden-Grenze**. Der Faktor‑8,5-Fall aus M49‑2a ist damit gelaufen (M50) |
| **Bleibt der Einstieg über `MessageBAM_BAMValueOnly`?** | **Nein — hier kippt der Plan.** Bei 1.332.180 Kandidatenzeilen steigt der Optimierer über `MessageLastUpdateIDX` ein und probt `MessageBAM` über den Primärschlüssel (M50 gegen M49‑3) |
| **Kann ein BAM-Wert ein Komma tragen?** | **Ja, 55.989 tun es (0,363 %).** Und Spring zerlegt einen wiederholbaren Parameter am Komma — diese Werte sind heute **unsuchbar**, in beiden Modi (M50‑5) |
| **Wo im Bestand sitzen die Kommas?** | **In drei von 62 Typen**, und 96,62 % davon in einem einzigen: 9003 „Material-Nr. beim Lieferanten" (54.096), dazu 9016 (1.888) und 9018 (**5**). Alle drei bei **einem** Mandanten (M51) |
| **Traf der Kommadefekt die tragenden Suchtypen?** | **Praktisch nicht.** Von den sieben aus M39 ist nur 9018 betroffen — mit **fünf** Werten von 2.311.236, also 0,0002 % des Typs (M51‑2, M51‑3) |
| **Bindet noch ein Endpunkt eine Liste, deren Werte ein Komma tragen könnten?** | **Zwei tun es** (`status`, `prozess` der Nachrichtenliste), **beide heute folgenlos**: Einordnungen sind Aufzählungsnamen, und **0 von 1.503** `ProcessID` tragen ein Komma (M51‑4) |

---

# M50 — Was kostet der schlimmste bekannte Präfix im gebauten Statement?

***Achter** Nachtrag, 14.08.2026. Schritt 7, Teil 4 — die Messung **vor** dem Bau (Regel L7).*

> **Die Nummern sind geprüft und nicht gleichgesetzt.** Höchste vergebene **Messnummer** in dieser
> Datei: **M49**; höchste **ergänzende** Nummer: **E7**; höchste **Nachtragsnummer**: **sieben**
> (M49). Messnummern und Nachtragsnummern laufen in dieser Datei getrennt — im siebten Nachtrag ist
> genau das schiefgegangen. Diese Runde ist deshalb der **achte** Nachtrag mit der Messnummer
> **M50**. Eine neue ergänzende Nummer ist nicht vergeben worden.

**Frage.** M49‑2a kennt einen Vierzeichen-Präfix mit **1.332.180** Zeilen über den Bestand. Durch
das gebaute Statement gelaufen ist bisher höchstens ein Fall mit 155.871 Zeilen (M49‑3: 1,361 s über
30 Tage, 3,400 s über ein Jahr). **Der bekannte Bösfall ist Faktor 8,5 größer, und er ist nie
gelaufen.** Diese Runde lässt ihn laufen.

## Die vorregistrierte Lesart — sie stand vor der Messung fest

| Ergebnis Jahresfenster | Konsequenz für den Bau |
|---|---|
| **unter 9 s** | Die Präfixsuche darf **dasselbe** Zeitfenster anbieten wie die exakte, bis zum Maximum aus Regel L1 (Zweig **A**) |
| **9 s oder mehr** | Die Präfixsuche ist auf **30 Tage gedeckelt**; das Jahresfenster steht ihr nicht offen (Zweig **B**) |
| **Abbruch an der 60‑s‑Grenze** | Zweig **B**, und der Abbruch ist das Ergebnis — nicht wiederholen, nicht die Grenze aussetzen |

Die 9 Sekunden sind 90 % der Zeitgrenze des Lese-Pools und derselbe Maßstab, den M47 an den exakten
Bösfall gelegt hat (8,940 s, „11 % Reserve").

> **Eingetreten ist der dritte Fall.** Das Jahresfenster ist an der 60‑Sekunden-Grenze abgebrochen —
> **schon im Aufwärmlauf**. Er ist nicht wiederholt und die Grenze nicht ausgesetzt worden.
> **Gebaut wird Zweig B.**

---

## M50‑0 Rahmen

Unverändert aus §0 übernommen, am **14.08.2026** in **elf** eigenen Sitzungen erhoben — neun für die
Messung, zwei als Diagnose beim Bau (ihre Zahlen sind in M50‑5 profiliert wiederholt). Jeder Aufruf
des Clients ist eine neue Sitzung; **`SELECT @@global.read_only` steht deshalb in jedem Skript als
erste Abfrage** und lieferte **jedes Mal `1`**.

| | |
|---|---|
| Ziel | **Testkopie**, `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| `@@global.read_only` **zu Beginn** | **`1`** (erste Abfrage der Runde, 09:52:30) |
| `@@global.read_only` **am Ende** | **`1`** (letzte Abfrage der Runde, 10:31:02) |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Serverzeit zu Beginn | `2026-08-14 09:52:30` (`UTC_TIMESTAMP` `07:52:30`, also UTC+2) |
| Serverzeit am Ende | `2026-08-14 10:31:02` (`UTC_TIMESTAMP` `08:31:02`) |
| `@@global.max_statement_time` | **`0`** — die 60 Sekunden je Sitzung über `SET max_statement_time = 60` |
| `@@global.event_scheduler` | **`ON`**, unverändert |
| Laufzeitmessung | serverseitig, `SET profiling = 1` / `SHOW PROFILES`, `profiling_history_size = 100` |
| Wiederholungen | **beste von drei nach einem Aufwärmlauf für den Endpunktfall** (M50‑3); die Erhebungen über den Bestand einmalig — sie messen eine Verteilung und keine Uhr, wie in M49 |
| Zugangsdaten | ausschließlich aus `OVERLORD_DB_*`, an den Client über `MYSQL_PWD` |

### `@@sql_mode` — unverändert

```
STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION
```

**Kein `ONLY_FULL_GROUP_BY`**, wie in §0, M47 und M49.

### `SHOW GRANTS FOR CURRENT_USER()`

Drei Zeilen, unverändert. Der Hash der ersten wird nach Regel G1 **nicht** abgedruckt:

```
GRANT USAGE ON *.* TO `monitor_read`@`%` IDENTIFIED BY PASSWORD '<Hash>'
GRANT SELECT ON `GlassfishDB`.* TO `monitor_read`@`%`
GRANT SELECT ON `overlord_monitor`.* TO `monitor_read`@`%`
```

### Die Testkopie ist unverändert — der **neunte** Messtag in Folge

| | `DATA_LENGTH` | `INDEX_LENGTH` |
|---|---:|---:|
| `Message` | 740.851.712 | 2.157.330.432 |
| `MessageAction` | 2.226.634.752 | 819.855.360 |
| **`MessageBAM`** | **1.826.422.784** | **5.254.217.728** |

**Byteidentisch mit M23‑1, M31‑0, §0, M44‑0, M46‑0, M47‑0 und M49‑0**, zu Beginn *und* am Ende der
Runde geprüft. Dazu zwei unabhängige Kontrollen, die beide aufgehen:

| Kontrolle | erwartet aus | gemessen |
|---|---|---|
| Zeilen in `MessageBAM` (dreimal in dieser Runde) | **15.406.350** (M33) | **15.406.350** ✔ |
| Der häufigste Vierzeichen-Präfix | **1.332.180** (M49‑2a) | **1.332.180** ✔ |

### Skills

**Es ist keiner benutzt worden**, und das war die Vorgabe des Auftrags. Installiert und geprüft sind
unter anderem `edi-field-mapping`, `find-skills`, `shadcn`, `frontend-design`, `dataviz`,
`code-review`, `security-review`, `simplify`, `run`, `artifact-design` und `claude-in-chrome`;
projektlokale Skills gibt es nicht. **`frontend-design` und `shadcn` sind für Teil 4 ausdrücklich
ausgeschlossen** — er fasst die Oberfläche nicht an —, die übrigen betreffen andere Werkzeuge.

### Anonymisierung

Wie in §0 und M49: **Kein BAM-Wert und keine Belegnummer steht in diesem Nachtrag.** Der Präfix
dieser Runde ist ein Anfangsstück echter Belegnummern; er steht ausschließlich in der
Sitzungsvariablen `@p`, wird dort **hergeleitet und dort verbraucht** und ist nie aus der Datenbank
herausgekommen. Abgedruckt sind seine **Gestalt**, seine Trefferzahl und die Typen, unter denen er
steht — Typnummern und Beschreibungen aus `MessageBAMType` sind nach der Regel in
[`README.md`](README.md) Konfigurationsvokabular.

### Die eine Abweichung vom Rahmen

**Der Auftrag verlangt `--skip-ssl`; gemessen ist mit `--ssl-mode=DISABLED`.** Der Client dieser
Runde ist `mysql` 8.0.46 aus MySQL Workbench — derselbe wie in Schritt 4 bis 7 —, und er kennt
`--skip-ssl` nicht; die Option ist die MariaDB-Schreibweise und bricht vor dem Sitzungsaufbau ab.
Ein Wechsel auf den MariaDB-Client kostete die Vergleichbarkeit mit **allen** früheren Runden. Die
Wirkung ist dieselbe: keine Transportverschlüsselung. Zusätzlich `--default-character-set=utf8mb4`,
weil die Runde über Leerzeichen und Maskierungszeichen misst.

---

## M50‑1 Der Präfix — hergeleitet, nicht abgeschrieben

Der Auftrag nennt die Herleitung und nicht den Wert:

```sql
SELECT LEFT(MessageBAMValue,4) AS p, COUNT(*) AS n INTO @p, @n
  FROM MessageBAM GROUP BY p ORDER BY n DESC, p ASC LIMIT 1;
```

> **Regel L9 ist berührt, und die Begründung steht vorher.** Die Abfrage läuft **ohne Zeitfenster
> über `MessageBAM`**. Ein Zeitfenster kann die Frage nicht beantworten: Gesucht ist der schlimmste
> Präfix des **Bestands** — genau der Wert, den M49‑2a gefunden und ausdrücklich *nicht* durch das
> Statement geschickt hat. Ein Monatsausschnitt lieferte den schlimmsten Präfix *eines Monats*, und
> der ist nicht die Größe, um die es geht. **Die Kosten stehen unten**: 16,062 bis 16,243 s je
> Sitzung. **Vier Sitzungen haben sie gelaufen, alle vier mit demselben Ergebnis** — drei davon
> protokolliert, die vierte ist die abgebrochene Jahressitzung. Die zweite Sortierstufe `p ASC` macht
> die Auswahl über Sitzungen hinweg reproduzierbar.

### Seine Gestalt

| Eigenschaft | Wert |
|---|---|
| Zeichen | **4** |
| rein numerisch | **ja** |
| beginnt mit einer Null | **nein** |
| enthält `%`, `_` oder ein Leerzeichen | **nein**, keines davon |
| Zeilen über den Bestand | **1.332.180** |

**Die 1.332.180 reproduzieren M49‑2a Ziffer für Ziffer** — dort steht dieselbe Zahl als Rang 1 bei
k = 4. Die Herleitung ist damit unabhängig bestätigt.

### Welchen Typen er gehört

```sql
SELECT b.MessageBAMType, t.MessageBAMTypeDescription, COUNT(*) AS n
FROM MessageBAM b LEFT JOIN MessageBAMType t ON t.MessageBAMType = b.MessageBAMType
WHERE b.MessageBAMValue LIKE CONCAT(@p,'%')
GROUP BY b.MessageBAMType, t.MessageBAMTypeDescription ORDER BY n DESC LIMIT 5;
```

| Typ | Beschreibung | Zeilen | Kennung oder Beleg? |
|---:|---|---:|---|
| 9002 | Lieferplannummer_L_SAP | 789.416 | Kennung |
| **9019** | **Bestellnummer vom Kunden_K_SAP** | **410.030** | **Beleg** |
| **9034** | **Bestellnummer_L_SAP** | **132.693** | **Beleg** |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 39 | Kennung |
| 0 | Bestellnummer | 2 | Beleg |

**Summe: 1.332.180** — die Aufschlüsselung geht auf, es fehlt kein Typ. Laufzeit 4,062 s.

**Zwei Drittel dieses Präfixes sind Belegnummern.** Die 410.030 sind Zeichen für Zeichen die Zahl,
die M49‑2a auf Rang 3 seiner schlimmsten Präfixpaare führt. **Der Befund aus M49‑2a — „gefährlich
sind nur die Kennungsfelder" trägt nicht — wird hier zum zweiten Mal und schärfer bestätigt:** Der
schlimmste Präfix des ganzen Bestands trägt mit 9019, 9034 und 0 gleich **drei** Belegnummerntypen
und 542.725 Zeilen davon. Das ist genau das, was ein Nutzer eintippt.

---

## M50‑2 `EXPLAIN` — der Plan kippt, und das ist der eigentliche Befund

Gemessen ist der von jOOQ **gerenderte** Text aus `BamSucheRepository`, mit getauschtem
Wertprädikat — dieselbe Bauform wie in M49‑3, nur ein anderer Prüfwert. Mandantenfilter als
`EXISTS`, `GROUP BY m.MessageID`, Deckelung auf 51, die vier Anzeigetabellen **über** der Deckelung,
**kein `STRAIGHT_JOIN`**.

**30 Tage** (Fenster B) — und **ein Jahr** unterscheidet sich nur in einer Zahl:

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---:|---|---:|---|
| 1 | PRIMARY | `<derived2>` | `ALL` | — | — | — | 51 | `Using filesort` |
| 1 | PRIMARY | `Process` | `eq_ref` | `PRIMARY` | 146 | `treffer.ProcessID` | 1 | `Using where` |
| 1 | PRIMARY | `Project` | `eq_ref` | `PRIMARY` | 146 | `Process.ProjectID` | 1 | `Using where` |
| 1 | PRIMARY | `SOS` | `eq_ref` | `PRIMARY` | 146 | `treffer.SOSID` | 1 | `Using where` |
| 1 | PRIMARY | `SOSAction` | `eq_ref` | `PRIMARY` | 148 | `treffer.SOSID,treffer.SOSActionID` | 1 | `Using where` |
| 2 | DERIVED | **`Message`** | **`range`** | **`MessageLastUpdateIDX`** | **5** | — | **409.758** (Jahr: **1.780.243**) | `Using index condition; Using where; Using temporary; Using filesort` |
| 2 | DERIVED | `mandanten_process` | `eq_ref` | `PRIMARY` | 146 | `Message.ProcessID` | 1 | `Using where` |
| 2 | DERIVED | `ProjectMandant` | `eq_ref` | `PRIMARY` | 292 | `mandanten_process.ProjectID,const` | 1 | `Using where; Using index` |
| 2 | DERIVED | **`b1`** | **`ref`** | **`PRIMARY`** | **146** | `Message.MessageID` | **8** | `Using where; Using index` |

### Was daran neu ist

**M49‑3 misst über elf Fälle, dass der Einstieg `b1` als `range` über `MessageBAM_BAMValueOnly`
bleibt. Hier bleibt er es nicht.** Bei 1.332.180 Kandidatenzeilen dreht der Optimierer die Reihenfolge
um: Er steigt über das **Zeitfenster** ein (`MessageLastUpdateIDX`, `key_len` 5) und probt
`MessageBAM` erst am Ende über den **Präfix des Primärschlüssels**. `MessageBAM_BAMValueOnly` steht
nur noch unter `possible_keys`.

**Das ist keine Fehlwahl, sondern die richtige.** Der Wertindex läge bei 1,33 Millionen Einträgen;
das Fenster liefert 409.758 geschätzte Zeilen. Der Optimierer nimmt den kleineren Einstieg — genau
das, wofür in diesem Statement seit M42‑1 **kein `STRAIGHT_JOIN`** steht. Eine festgeschriebene
Reihenfolge nähme ihm diese Wahl ausgerechnet im teuersten Fall.

> **Regel L15: Die Einstiegstabelle ist belegt und nicht angenommen** — und sie ist eine andere als
> erwartet. Wer nach M49‑3 „der Plan ändert sich nicht" fortgeschrieben hätte, hätte hier danebengelegen.
> Der Satz aus M49‑3 bleibt richtig **für seine elf Fälle**; er trägt nicht über sie hinaus.

**Und die Deckelung steht, wo sie stand:** `<derived2>` mit 51 Zeilen, darüber vier `eq_ref` auf
`PRIMARY`. Die Gestalt aus M47 ist unberührt.

---

## M50‑3 Laufzeiten

| Fall | Kandidatenzeilen | Aufwärmlauf | Läufe | **beste von drei** | Treffer |
|---|---:|---:|---|---:|---|
| **30 Tage** | 1.332.180 | 3,913 s | 3,865 / 3,885 / 3,851 s | **3,851 s** | 51 → **gedeckelt** |
| **ein Jahr** | 1.332.180 | — | — | **Abbruch bei 60 s** | — |

**Der Abbruch ist ein Ergebnis und kein Fehlschlag.** MariaDB meldet `ERROR 1969 (70100): Query
execution was interrupted (max_statement_time exceeded)` — und zwar bereits im **Aufwärmlauf**. Nach
der vorregistrierten Lesart ist er damit die Antwort; er ist nicht wiederholt worden, und die Grenze
ist nicht ausgesetzt worden.

> **Dass es genau der Fehler ist, den der Endpunkt bereits behandelt, ist kein Zufall und kein
> Trost.** `1969`/`70100` ist die Ausnahme, aus der `BamSucheRepository.anDerZeitgrenze` den
> Problemtyp `suche-abgebrochen` macht. In Produktion liefe sie **früher**: Der Lese-Pool setzt
> `max_statement_time=10` ([`datenzugriff.md`](datenzugriff.md) §1), nicht 60. Der Jahresfall wäre
> dort ein Abbruch nach zehn Sekunden — **ein Endpunkt, der zuverlässig abbricht, ist kein
> Endpunkt**, und genau deshalb steht der Deckel im Code und nicht der Abbruchpfad allein.

### Was die 30 Tage im Verhältnis bedeuten

| Fall | 30 Tage | Anteil an der 10‑s‑Grenze des Lese-Pools |
|---|---:|---:|
| exakt, schlimmster Wert (M47) | 1,656 s | 16,6 % |
| Präfix, schlimmster **Wert** (M49‑3) | 1,823 s | 18,2 % |
| Präfix, vier Zeichen eines Werts (M49‑3) | 1,361 s | 13,6 % |
| **Präfix, schlimmster Präfix des Bestands (M50)** | **3,851 s** | **38,5 %** |

**Faktor 2,1 gegenüber dem bisher teuersten Präfixfall über 30 Tage** — und der Faktor zwischen den
Kandidatenzahlen ist 8,5. Die Laufzeit wächst also **langsamer als linear**, was zum gekippten Plan
passt: Nicht mehr der Wertindex bestimmt die Arbeit, sondern das Fenster.

---

## M50‑4 Die tatsächlich gerenderte Fassung — mit `ESCAPE`

M49‑3 hat mit `like concat(?, '%')` gemessen. **Gebaut wird `like ? escape '\'`**, weil die
Maskierung Pflicht ist (M49‑4: 2.696 Werte mit `_`). jOOQ rendert die Klausel für MariaDB als
`escape '\\'` — der Rückstrich ist im Zeichenkettenliteral verdoppelt, wie es MariaDB verlangt.

**Der `EXPLAIN` ist Zeile für Zeile derselbe**, sowohl mit einem `LIKE` als auch mit **zwei
veroderten** (die Gestalt der Nullen-im-Muster-Fassungen): `Message` als `range` über
`MessageLastUpdateIDX` mit denselben 409.758 Zeilen, `b1` als `ref` über `PRIMARY` mit `rows` 8,
darüber die vier `eq_ref`. **Die Maskierung kostet den Plan nichts.**

### Der Beleg statt der Behauptung

Ohne Tabellenzugriff, damit die `ESCAPE`-Semantik der **gerenderten** Fassung gezeigt und nicht
behauptet ist:

| Ausdruck | Ergebnis | was das heißt |
|---|:---:|---|
| `'50%' LIKE '50\%%' ESCAPE '\'` | **1** | maskiert trifft `%` das Zeichen selbst |
| `'50X' LIKE '50\%%' ESCAPE '\'` | **0** | … und eben **nicht** jedes Zeichen |
| `'5_0' LIKE '5\_0%' ESCAPE '\'` | **1** | dasselbe für `_` |
| `'5X0' LIKE '5\_0%' ESCAPE '\'` | **0** | die Falle aus Regel Q1, geschlossen |
| `'a\b' LIKE 'a\\b%' ESCAPE '\'` | **1** | das Maskierungszeichen selbst ist maskierbar |
| `' 4711X' LIKE ' 4711%' ESCAPE '\'` | **1** | ein **führendes** Leerzeichen trägt und bleibt |
| `'4711 ' LIKE '4711%' ESCAPE '\'` | **1** | ein Wert **mit** folgendem Leerzeichen wird gefunden |
| **`'4711' LIKE '4711 %' ESCAPE '\'`** | **0** | **aber eine Eingabe mit folgendem Leerzeichen findet den Wert ohne nicht mehr** |

Die letzte Zeile ist die, an der der Schnitt in `Suchbedingung.muster()` hängt — M49‑4 hat sie als
Folgerung ausgewiesen, hier steht sie an der gebauten Fassung.

---

## M50‑5 Der Doppelpunkt hat einen Nachbarn: das Komma

**Gefunden beim Bau, nicht gesucht.** Die Herleitung des erweiterten Isolationstests nimmt die
**längsten** `NEXANS`-Werte im Fenster — und der Endpunkt antwortete darauf mit `400`. Der Grund
liegt nicht im neuen Pfad, sondern in der Parameterform aus Teil 2b: **Spring zerlegt einen
`@RequestParam List<String>` am Komma.** Ein BAM-Wert mit Komma zerfällt damit in zwei Begriffe, von
denen der zweite keinen Pflichttrenner mehr trägt — die Antwort ist `400 suchbegriff-ohne-typtrenner`.

> **Regel L9 ist berührt**, mit derselben Begründung wie bei M49‑4: Die Frage gilt dem **Bestand**.
> Eine Parameterform steht dauerhaft im Code; was *ein Monat* enthält, ist dafür die falsche
> Grundgesamtheit. **Kosten: 7,731 s**, Gegenprobe 7,614 s.

```sql
SELECT COUNT(*) AS zeilen, SUM(MessageBAMValue LIKE '%,%') AS mit_komma FROM MessageBAM;
```

**`EXPLAIN`:** `MessageBAM`, `type` `index`, `key` `PRIMARY`, `key_len` 430, `rows` 10.859.666,
`Using index` — dieselbe Gestalt wie M49‑4.

| | Zeilen | Anteil |
|---|---:|---:|
| `MessageBAM` insgesamt | **15.406.350** | 100 % |
| **mit `,`** | **55.989** | **0,363 %** |

**Gegengeprüft in einer zweiten, `LIKE`-freien Form** — wie bei M49‑4, weil eine `LIKE`-Zählung in
diesem Projekt schon einmal eine Zahl verdorben hat:

```sql
SELECT SUM(LOCATE(',', MessageBAMValue) > 0) AS mit_komma_ohne_like FROM MessageBAM;
```

**55.989** — Zeichen für Zeichen dieselbe Zahl. Und `COUNT(*)` liefert zum dritten Mal in dieser
Runde **15.406.350**.

**Das Komma ist damit 95‑mal häufiger als der Doppelpunkt** (55.989 gegen 585, M49‑4) — und anders
als beim Doppelpunkt ist die Lage **nicht** entschärft: Der Doppelpunkt ist durch die Teilung am
*ersten* Vorkommen behandelt, das Komma ist es nicht. **Diese 55.989 Werte sind heute unsuchbar, in
beiden Modi.**

> **Teil 4 behebt das nicht, und das ist eine Entscheidung.** Eine Änderung an der Bindung träfe den
> **exakten** Pfad, der gebaut, getestet und in M47 gemessen ist; §2.2 des Auftrags verlangt
> ausdrücklich, dass dort Zeichen für Zeichen nichts geändert wird. Der Befund ist stattdessen in
> `BamSucheDbIT.ein_komma_im_wert_ist_heute_400` festgeschrieben und steht als offener Punkt in
> [`bam-suche.md`](bam-suche.md) §9.

---

## Befunde — vor der Messung formuliert

| Befund | trifft zu | Konsequenz |
|---|---|---|
| **Der Jahresfall bleibt unter 9 s** | **nein — Abbruch bei 60 s** | **Zweig B.** Der Präfixmodus ist auf 30 Tage gedeckelt; die exakte Suche behält ihr Jahresmaximum |
| **Der 30‑Tage-Fall bleibt unter 9 s** | **ja — 3,851 s** | Der Deckel ist damit nicht nur eine Grenze, sondern eine **tragfähige** Grenze: 38,5 % der Zeitgrenze im schlimmsten bekannten Fall |
| **Der Plan bleibt der aus M49‑3** | **nein** | Der Optimierer kippt auf `MessageLastUpdateIDX` und probt `MessageBAM` über `PRIMARY`. **Kein `STRAIGHT_JOIN`** — er wählt weiter selbst, und er wählt richtig |
| **Die Laufzeit wächst linear mit der Kandidatenzahl** | **nein, langsamer** | Faktor 8,5 an Kandidaten ergibt Faktor 2,1 an Laufzeit gegenüber M49‑3. Passt zum gekippten Plan: Die Arbeit hängt am Fenster, nicht mehr am Wertindex |
| **Die schlimmsten Präfixe gehören Kennungsfeldern** | **nein — zwei Drittel sind Belegnummern** | 9019, 9034 und 0 tragen 542.725 der 1.332.180 Zeilen. **M49‑2a wird zum zweiten Mal bestätigt**: „nur Kennungsfelder" trägt nicht |

## Belegvermerk (Regel L10)

> *Gemessen:* Die Laufzeit des **gebauten** Statements mit getauschtem Wertprädikat für **einen**
> Präfix — den häufigsten Vierzeichen-Präfix des Bestands (1.332.180 Zeilen, hergeleitet und dreimal
> reproduziert) — bei `NEXANS`, über 30 Tage (beste von drei nach einem Aufwärmlauf) und über ein
> Jahr (Abbruch im Aufwärmlauf); dazu **vier** `EXPLAIN` (zwei Fenster × zwei Fassungen des
> Wertprädikats), acht Ausdrücke ohne Tabellenzugriff für die `ESCAPE`- und PAD-SPACE-Semantik und
> zwei Vollerhebungen über `MessageBAM` (Präfixherleitung, Kommazählung).
>
> *Behauptet wird:* dass der Präfixmodus über 30 Tage tragbar und über ein Jahr nicht tragbar ist.
>
> **Die Lücke, und sie ist vierfach.** **Erstens** ist `n = 1`: Gemessen ist **ein** Präfix, nämlich
> der schlimmste bekannte — nicht eine Verteilung. Der Satz „30 Tage sind tragbar" stützt sich darauf,
> dass dieser eine Fall der obere Rand ist, und das ist ein **Schluss aus M49‑2a** und keine Messung
> dieser Runde. **Zweitens** gilt die 10‑Sekunden-Grenze in **Produktion**, gemessen ist eine
> **ruhende** Testkopie — dieselbe benannte Lücke wie in M35, M47 und M49‑3; die Zahlen sind
> Untergrenzen und keine Zusagen. Unter Last liegt der 30‑Tage-Fall näher an der Grenze als 38,5 %.
> **Drittens** ist der gemessene Fall **ein Begriff ohne Typ und ohne Nullen-im-Muster-Fassungen** —
> die gebaute Höchstform (acht Begriffe, bis zu sieben Fassungen je Begriff) ist **nicht** gelaufen;
> M49‑3 misst vier verodere `LIKE` bei *einem* Begriff mit 1,205 ms, und mehr ist dazu nicht bekannt.
> **Viertens** sagt keine Zahl, **welche** Präfixe Nutzer tatsächlich eingeben; gemessen ist der
> schlimmste Fall, nicht der wahrscheinliche.
>
> Was trägt: *Der schlimmste bekannte Präfix des Bestands kostet über 30 Tage 3,851 s und reißt über
> ein Jahr die 60‑Sekunden-Grenze.* Was **nicht** gemessen ist: *ob ein anderer Präfix über 30 Tage
> teurer ist, und was die Höchstform mit acht Begriffen kostet.*

---

# M51 — Wo im Bestand sitzen die Kommas?

***Neunter** Nachtrag, 14.08.2026. Die Messung **zur** Behebung des Kommadefekts (Regel L7).*

> **Die Nummern sind geprüft und nicht gleichgesetzt.** Höchste vergebene **Messnummer** in dieser
> Datei: **M50**; höchste **ergänzende** Nummer: **E7**; höchste **Nachtragsnummer**: **acht**
> (M50). Messnummern und Nachtragsnummern laufen in dieser Datei getrennt — im siebten Nachtrag ist
> genau das schiefgegangen. Diese Runde ist deshalb der **neunte** Nachtrag mit der Messnummer
> **M51**. Eine neue ergänzende Nummer ist nicht vergeben worden.

**Frage.** M50‑5 hat gezählt, **wie viele** Werte ein Komma tragen: 55.989, also 0,363 % des
Bestands. **Wo sie sitzen, hat niemand gezählt** — und davon hängt ab, ob „ein Defekt im Suchweg"
trägt oder ob eine große Zahl nur groß aussieht. Ein Komma in einer Freitextspalte ist etwas anderes
als eines in der Nummer, die ein Mandant vom Beleg abtippt.

> **Diese Messung entscheidet den Bau nicht.** Repariert wird so oder so; der Defekt liegt in einem
> Pfad, der seit Teil 2b als fertig gilt. Sie entscheidet, **was in der Dokumentation über die
> Tragweite stehen darf**, statt dass es geschätzt wird.

## Die vorregistrierte Lesart — sie stand vor der Messung fest

| Befund | trifft zu | Konsequenz |
|---|---|---|
| **Die Kommas sitzen überwiegend in Kennungs- und Freitextfeldern** | **ja — 100 %** | Der Defekt ist ärgerlich, aber selten im Weg dessen, der eine **Belegnummer** sucht |
| **Sie sitzen in tragenden Suchtypen** (M39: 9018, 0, 2000, 2001, 3, 2002, 9014) | **fast nicht — 5 von 55.989** | Nur **Typ 9018** ist betroffen, mit **fünf** Werten. Die Dringlichkeit steigt dadurch nicht |
| **Ein einzelner Typ trägt den Großteil** | **ja — 96,62 %** | In der Dokumentation steht **Typ 9003** und keine Gesamtzahl |

---

## M51‑0 Rahmen

Unverändert aus §0 übernommen, am **14.08.2026** in **zehn** eigenen Sitzungen erhoben — neun für
die Messung, eine als Diagnose beim Bau der Testherleitung (ihre beiden Laufzeiten stehen in der
Laufzeittabelle). Jeder Aufruf des Clients ist eine neue Sitzung; **`SELECT @@global.read_only`
steht deshalb in jedem Skript als erste Abfrage** und lieferte **jedes Mal `1`**.

| | |
|---|---|
| Ziel | **Testkopie**, `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| `@@global.read_only` **zu Beginn** | **`1`** (erste Abfrage der Runde, 11:11:11) |
| `@@global.read_only` **am Ende** | **`1`** (letzte Abfrage der Runde, 11:26:15) |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Serverzeit zu Beginn | `2026-08-14 11:11:11` (`UTC_TIMESTAMP` `09:11:11`, also UTC+2) |
| Serverzeit am Ende | `2026-08-14 11:26:15` (`UTC_TIMESTAMP` `09:26:15`) |
| `@@global.max_statement_time` | **`0`** — die 60 Sekunden je Sitzung über `SET max_statement_time = 60` |
| `@@global.event_scheduler` | **`ON`**, unverändert |
| `@@div_precision_increment` | **`4`** — hier ohne Wirkung: Diese Runde rechnet **keine** Division im Server, alle Anteile sind aus ganzen Zahlen abgeleitet |
| Laufzeitmessung | serverseitig, `SET profiling = 1` / `SHOW PROFILES`, `profiling_history_size = 100` |
| Wiederholungen | **einmalig**. Der Gegenstand ist eine **Verteilung und keine Uhr** — dieselbe Begründung wie bei den Bestandserhebungen in M49 und M50; die Laufzeiten sind Obergrenzen mit Kaltlaufanteil und tragen keinen Befund |
| Zugangsdaten | ausschließlich aus `OVERLORD_DB_*`, an den Client über `MYSQL_PWD` |

### `@@sql_mode` — unverändert

```
STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION
```

### `SHOW GRANTS FOR CURRENT_USER()`

Drei Zeilen, unverändert. Der Hash der ersten wird nach Regel G1 **nicht** abgedruckt:

```
GRANT USAGE ON *.* TO `monitor_read`@`%` IDENTIFIED BY PASSWORD '<Hash>'
GRANT SELECT ON `GlassfishDB`.* TO `monitor_read`@`%`
GRANT SELECT ON `overlord_monitor`.* TO `monitor_read`@`%`
```

### Die Testkopie ist unverändert — der **zehnte** Messtag in Folge

| | `DATA_LENGTH` | `INDEX_LENGTH` |
|---|---:|---:|
| `Message` | 740.851.712 | 2.157.330.432 |
| `MessageAction` | 2.226.634.752 | 819.855.360 |
| **`MessageBAM`** | **1.826.422.784** | **5.254.217.728** |

**Byteidentisch mit M23‑1, M31‑0, §0, M44‑0, M46‑0, M47‑0, M49‑0 und M50‑0**, zu Beginn *und* am
Ende der Runde geprüft. Dazu zwei Kontrollen aus M50‑5, die beide aufgehen:

| Kontrolle | erwartet aus | gemessen |
|---|---|---|
| Zeilen in `MessageBAM` | **15.406.350** (M33) | **15.406.350** ✔ |
| Werte mit Komma | **55.989** (M50‑5) | **55.989** ✔ |

### Skills

**Es ist keiner benutzt worden**, und das war die Vorgabe des Auftrags. Installiert und geprüft sind
`edi-field-mapping`, `find-skills` und `shadcn` als Nutzer-Skills, dazu die mitgelieferten
`frontend-design`, `dataviz`, `artifact-design`, `artifact-diagramming`, `artifact-capabilities`,
`code-review`, `security-review`, `simplify`, `run`, `init`, `loop`, `schedule`, `claude-api`,
`claude-in-chrome`, `update-config`, `keybindings-help` und `fewer-permission-prompts`;
**projektlokale Skills gibt es nicht** (`.claude/skills/` fehlt). Keiner davon passt auf einen
Auftrag, der eine Parameterbindung repariert und die Oberfläche nicht anfasst.

### Anonymisierung

Wie in §0, M49 und M50: **Kein BAM-Wert und keine Belegnummer steht in diesem Nachtrag.** Abgedruckt
sind ausschließlich **Typnummern, Typbeschreibungen und Zählungen** — Typnummern und Beschreibungen
aus `MessageBAMType` sind nach der Regel in [`README.md`](README.md) Konfigurationsvokabular.

### Die eine Abweichung vom Rahmen

**Der Auftrag verlangt `--skip-ssl`; gemessen ist mit `--ssl-mode=DISABLED`.** Zum dritten Mal
dieselbe Abweichung aus demselben Grund: Der Client ist `mysql` 8.0.46 aus MySQL Workbench, und die
Option ist die MariaDB-Schreibweise. Die Wirkung ist dieselbe: keine Transportverschlüsselung.
Zusätzlich `--default-character-set=utf8mb4`.

---

## M51‑1 Die Sonde — und sie reproduziert M50‑5 auf die Stelle

Der Auftrag verlangt eine Sonde vor dem Lauf: **Reißt sie die Grenze, ist der Abbruch das
Ergebnis.** Genommen ist dafür nicht irgendein billiges Statement, sondern **die Zählung aus M50‑5**
— sie kostet dieselbe Art Vollindex-Durchlauf wie die eigentliche Messung und ist zugleich die
Kontrolle, ob der Bestand noch derselbe ist.

```sql
SELECT COUNT(*) AS zeilen, SUM(MessageBAMValue LIKE '%,%') AS mit_komma FROM MessageBAM;
```

| | Zeilen | Anteil |
|---|---:|---:|
| `MessageBAM` insgesamt | **15.406.350** | 100 % |
| **mit `,`** | **55.989** | **0,363 %** |

**7,731 s** — und das ist nicht nur „unter der Grenze", sondern **dieselbe Zahl wie in M50‑5**, dort
ebenfalls 7,731 s. Die Sonde ist damit durchgelaufen; die Messung durfte folgen.

> **Regel L9 ist berührt**, mit derselben Begründung wie bei M49‑4 und M50‑5: Die Frage gilt dem
> **Bestand**. Eine Parameterform steht dauerhaft im Code; was *ein Monat* enthält, ist dafür die
> falsche Grundgesamtheit.

---

## M51‑2 Das Statement — und der Befund ist schmaler als die Zahl

```sql
SELECT MessageBAMType, COUNT(*) AS zeilen
FROM MessageBAM
WHERE MessageBAMValue LIKE '%,%'
GROUP BY MessageBAMType
ORDER BY zeilen DESC
LIMIT 15;
```

**`EXPLAIN`:** `MessageBAM`, `type` `index`, `key` **`MessageBAM_BAMValue`**, `key_len` 284, `rows`
10.859.666, `Using where; Using index; Using temporary; Using filesort`.

> **Der Plan ist ein anderer als in M49‑4 und M50‑5**, und der Grund ist die Gruppierung: Dort lief
> der Durchlauf über `PRIMARY` (`key_len` 430), hier über den **Wertindex**. Er trägt als
> Sekundärindex die Spalten des Primärschlüssels mit und deckt damit `MessageBAMType` ab — der
> schmalere Schlüssel (284 statt 430) ist der billigere Durchlauf. **Er kostet 6,716 s und damit
> weniger als die Sonde**, obwohl er zusätzlich gruppiert und sortiert.

**Laufzeit: 6,716 s.** Das Ergebnis hat **drei** Zeilen — `LIMIT 15` schneidet nichts ab:

| `MessageBAMType` | `MessageBAMTypeDescription` | Zeilen mit Komma | Anteil an allen 55.989 |
|---:|---|---:|---:|
| **9003** | **`Material-Nr. beim Lieferanten_L_SAP`** | **54.096** | **96,62 %** |
| 9016 | `Abladestelle_K_SAP` | 1.888 | 3,37 % |
| 9018 | `Kundenmaterialnummer_K_SAP` | **5** | 0,01 % |

Die Beschreibungen stehen unverändert, samt Endung — dieselbe Regel wie in M45.

**Kontrolle in zweiter Form**, ohne `LIMIT` und ohne Verlass auf die Sortierung:

```sql
SELECT COUNT(*) AS typen_mit_komma, SUM(zeilen) AS summe FROM (
  SELECT MessageBAMType, COUNT(*) AS zeilen
  FROM MessageBAM WHERE MessageBAMValue LIKE '%,%' GROUP BY MessageBAMType
) t;
```

**3 Typen, Summe 55.989** (6,612 s) — Zeile für Zeile dieselbe Menge. Von den 62 Typen der
Konfiguration (M45) tragen also **drei** überhaupt ein Komma.

---

## M51‑3 Wie dicht sitzen sie? — über den Auftrag hinaus

Der Auftrag verlangt ein Statement. Ohne den **Nenner je Typ** bliebe aber offen, ob 54.096 für
Typ 9003 viel oder wenig ist — und genau daran hängt die dritte Zeile der Lesart. Ein Statement
mehr:

```sql
SELECT MessageBAMType, COUNT(*) AS zeilen_des_typs, SUM(MessageBAMValue LIKE '%,%') AS mit_komma
FROM MessageBAM WHERE MessageBAMType IN (9003, 9016, 9018) GROUP BY MessageBAMType;
```

| Typ | Zeilen des Typs | mit Komma | Anteil **innerhalb** des Typs |
|---:|---:|---:|---:|
| **9003** | 1.071.249 | 54.096 | **5,05 %** |
| 9016 | 412.886 | 1.888 | 0,46 % |
| 9018 | 2.311.236 | 5 | **0,0002 %** |

**3,016 s** — deutlich billiger als die Vollerhebungen, weil das Typprädikat den Durchlauf
begrenzt. Die Spalte `mit_komma` reproduziert die drei Zahlen aus M51‑2 ein drittes Mal.

**Und alle drei Typen gehören demselben Mandanten.** `MessageBAMMandant` führt 9003, 9016 und 9018
bei **genau einem** der sieben konfigurierten Mandanten, nämlich `NEXANS` (1,1 ms). Der Defekt war
damit kein Breitenproblem: Er traf **einen** Mandanten, dort aber jeden zwanzigsten Wert des
Typs 9003.

---

## M51‑4 Die Nachbarschaft — bindet noch ein Endpunkt eine Liste?

Der Defekt ist an einer Stelle gefunden worden; der Auftrag verlangt, nachzusehen, ob er anderswo
genauso liegt. **Durchgesehen sind alle zehn Controller.** Anfrageparameter mit Listentyp gibt es
außer `begriff` genau **zwei**, beide in der Nachrichtenliste:

| Endpunkt | Parameter | Werte kommen aus | Komma möglich? |
|---|---|---|---|
| `/api/nachrichten` | `status` | Namen von `MessageStatusKind` (`FEHLER`, `WARTEND`, `LAEUFT`, `AUFGETEILT`, `ZUSAMMENGEFUEHRT`, `ABGESCHLOSSEN`, `QUITTIERT`) | **nein** — Aufzählungsnamen; ein Komma darin gäbe es nur als Tippfehler und ergibt `400 status-unbekannt` |
| `/api/nachrichten` | `prozess` | `Process.ProcessID` | **nein, gemessen** — siehe unten |

Alle übrigen Anfrageparameter des Projekts sind **einzelne** Werte (`zeitraum`, `von`, `bis`,
`suche`, `langeSuche`, `sortierung`, `cursor`, `limit`, `modus`) und werden nicht zerlegt; die
Administration, die Anmeldung und der Mandantenwechsel nehmen JSON-Rümpfe entgegen, Detail, Kette
und Belegdaten eine Kennung im Pfad.

**`ProcessID` ist gemessen und nicht geschätzt** (Regel Q4):

```sql
SELECT COUNT(*), SUM(ProcessID LIKE '%,%'), SUM(LOCATE(',', ProcessID) > 0) FROM Process;
```

**1.503 Prozesse, davon 0 mit Komma** — in beiden Formen, `LIKE` und `LOCATE`, wie bei M49‑4 und
M50‑5 gegengeprüft. Die längste `ProcessID` hat 36 Zeichen. **3,0 ms**, weil `Process` eine
Stammdatentabelle ist.

> **Nicht mitrepariert, und das ist Absicht.** Ein Auftrag, ein Eingriff. Die Zahl sagt, dass dort
> heute nichts kaputt ist — nicht, dass die Bindung dort richtig wäre. **Ein Prozessname mit Komma
> würde die Nachrichtenliste morgen genauso zerlegen**, und dann ist dieser Abschnitt die Stelle,
> an der es steht.

---

## Befunde — vor der Messung formuliert

| Befund | trifft zu | Konsequenz |
|---|---|---|
| **Die Kommas sitzen überwiegend in Kennungs- und Freitextfeldern** | **ja, ausschließlich** | Material-Nr. beim Lieferanten und Abladestelle tragen 99,99 % davon. Keiner der drei Typen ist eine Belegnummer im Sinne des Leitsatzes |
| **Sie sitzen in tragenden Suchtypen** | **fast nicht** | Von den sieben Typen aus M39 ist einer betroffen — 9018 mit **fünf** Werten von 2.311.236 |
| **Ein einzelner Typ trägt den Großteil** | **ja** | 9003 trägt 54.096 der 55.989 |

**Ein Befund passte in keine vorformulierte Zeile: Der Defekt war ein Mandantenproblem und kein
Bestandsproblem.** Alle drei Typen sind bei **genau einem** der sieben Mandanten konfiguriert. Die
Zahl „0,363 % des Bestands" liest sich wie ein gleichmäßig verteiltes Rauschen; tatsächlich saß
sie bei einem Mandanten auf **jedem zwanzigsten Wert** eines seiner Anzeigetypen. **Das ist kein
kleinerer, sondern ein anderer Befund** — und er ist der Grund, weshalb die Herleitung des
Isolationstests über ihn gestolpert ist und nicht irgendeine Suche.

**Ein zweiter passte ebenfalls nicht: Der Plan ist billiger als der der Sonde.** Die Gruppierung
über `MessageBAMType` schickt den Optimierer auf den **Wertindex** statt auf `PRIMARY` — 284 statt
430 Byte Schlüssellänge und 6,716 s statt 7,731 s, obwohl zusätzlich gruppiert und sortiert wird.
Das war nicht erwartet und ändert an keiner Folgerung etwas; es steht hier, weil es in keine Zeile
passte.

## Belegvermerk (Regel L10)

> *Gemessen:* Die Verteilung der kommahaltigen Werte über die BAM-Typen — **eine** Vollerhebung über
> `MessageBAM` nach der Vorgabe des Auftrags, **eine** Sonde als Kontrolle gegen M50‑5, **eine**
> zweite Form der Gruppierung ohne `LIMIT`, **eine** Erhebung der Nenner je betroffenem Typ, dazu
> die Beschreibungen aus `MessageBAMType`, die Zuordnung aus `MessageBAMMandant` und eine
> Vollerhebung über `Process` für die Nachbarschaft.
>
> *Behauptet wird:* dass der Kommadefekt **einen** Mandanten und dort im Wesentlichen **einen** Typ
> traf, und dass er die tragenden Suchtypen aus M39 praktisch nicht berührte.
>
> **Die Lücke, und sie ist dreifach.** **Erstens** — und das ist die Lücke, die der Auftrag selbst
> benennt — sagt keine dieser Zahlen, **wie oft solche Werte gesucht werden**. Gemessen ist die
> Existenz im Bestand, nicht ein Schaden im Betrieb; dieses Projekt erhebt Suchanfragen bewusst
> nicht, und deshalb ist auch keine Zahl unterwegs, die die Lücke später schlösse. **Der Satz „der
> Defekt war selten im Weg" ist damit ein Schluss aus der Datenlage und keine Beobachtung.**
> **Zweitens** ist die Zuordnung „Kennungs- oder Freitextfeld" aus den **Beschreibungen** gelesen
> und nicht aus dem Inhalt: Dass „Material-Nr. beim Lieferanten" keine Belegnummer im Sinne des
> Leitsatzes ist, folgt aus ihrem Namen — und Namen sind in diesem Projekt Konfiguration und keine
> Zusage. **Drittens** ist die Nachbarschaft **heute** sauber; `Process` ist eine gepflegte
> Stammdatentabelle, und 0 von 1.503 ist eine Momentaufnahme und keine Eigenschaft des Schemas.
>
> Was trägt: *Drei von 62 Typen tragen Kommas, 96,62 % davon ein einziger, und alle drei gehören
> einem Mandanten.* Was **nicht** gemessen ist: *ob und wie oft jemand diese Werte gesucht hat.*

---

# Alle Laufzeiten

Serverseitig über `SET profiling = 1`, beste von fünf nach einem Aufwärmlauf; bei Statements über
zehn Sekunden beste von drei.

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M32 Indizes, Spalten, `SHOW CREATE TABLE` | — | < 10 ms | einmalig (Katalog) |
| **M33‑0 `COUNT(*)` über `MessageBAM`** | **ohne** | **18,795 s** kalt · **3,623 s** warm | zwei Läufe |
| **M33 Grundaggregation** | **ohne** | **62,780 s** ⚠️ kalt · **10,421 s** warm | einmalig kalt — **Grenze gerissen** · warm beste von 3 |
| M33 Histogramm der Trefferzahl | ohne | 10,633 s | beste von 3 |
| M33‑1 Klassenverteilung | ohne | 13,403 s | beste von 3 |
| M33‑1 Perzentile | ohne | 10,694 s | beste von 3 |
| **M33‑2 je Typ, mit Join** | **ohne** | **198,596 s** ⚠️ kalt · **17,631 s** warm | einmalig kalt — **Grenze gerissen** · warm beste von 3 |
| **Auswahl der Prüfwerte, erste Fassung** | ohne | **abgebrochen nach 679 s** ⚠️ | — |
| Auswahl der Prüfwerte, zweite Fassung | A bzw. B | 1,569–1,680 s je Wert | einmalig |
| M34 exakt, typisch / unangenehm / schlimm (`NEXANS`) | ohne | **0,804 / 1,426 / 10.617,6 ms** | beste von 5 · 5 · 3 |
| M34 exakt + `STRAIGHT_JOIN` (dieselben) | ohne | 0,704 / 1,423 / 10.667,3 ms | beste von 5 · 5 · 3 |
| M34 Präfix (dieselben) | ohne | 1,022 / 1,534 / 11.020,5 ms | beste von 5 · 5 · 3 |
| M34 Präfix + `STRAIGHT_JOIN` | ohne | 0,954 / 1,485 / 11.048,1 ms | beste von 5 · 5 · 3 |
| M34 exakt / Präfix, `IBIS` | ohne | 0,712 / 0,744 ms | beste von 5 |
| M34 exakt / Präfix, `ZAST` | ohne | 0,708 / 0,709 ms | beste von 5 |
| **M34 `enthält`, typisch** | ohne | **40.246,9 ms** · mit `SJ` **9.273,3 ms** | beste von 3 |
| **M34 `enthält`, schlimm** | ohne | **48.504,6 ms** · mit `SJ` **20.108,3 ms** | beste von 3 |
| E6 Präfix 6 Zeichen (5.425 Treffer) | ohne / B | 59 / 48 ms | beste von 5 |
| E6 Präfix 4 Zeichen (155.871 Treffer) | ohne / B | 3.441 / 1.299 ms | beste von 5 |
| M35 typisch, ohne / A / B / Jahr | alle vier | 0,729 / 0,757 / 0,796 / 0,787 ms | beste von 5 |
| **M35 schlimm, ohne / A / B / Jahr** | alle vier | **10.752,8 / 90,5 / 1.652,0 / 8.664,4 ms** | beste von 3 |
| M36 typisch, ohne / mit Typ | ohne | 0,738 / 0,769 ms | beste von 5 |
| M36 schlimm, ohne / mit Typ | ohne | 10.459,3 / 10.617,0 ms | beste von 3 |
| M37 Typenzahl je Paar | B | 7,616 s | beste von 3 |
| M37 Typenkombinationen | B | 6,943 s | beste von 3 |
| M37 gemeinsame Werte je Typenpaar | B | 3,411 s | beste von 3 |
| M38 Gestalt der Werte je Typ | B | 8,057 s | beste von 3 |
| M39‑0 Nenner je Mandant und Rolle | B | 3,384 s | beste von 3 |
| M39‑1 Typ je Rolle, alle Mandanten | B | 13,925 s | beste von 3 |
| M40‑1 Konfiguration / Stammdaten | — | 0,001 / 0,003 s | beste von 3 |
| M40‑2 vorkommende Typen | B | 7,847 s | beste von 3 |
| **M40‑3 vier kleine Mandanten** | **ohne** | **21,698 s** | beste von 3 (Regel L9) |
| M41 Werte je Nachricht | B | 17,352 s | beste von 3 |
| M41 Typen je Nachricht | B | 5,001 s | beste von 3 |

### Nachtrag vom 12.08.2026 — M42 und M43

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M42‑0 Auswahl der Prüfwerte K1–K4 (`NEXANS`) | A | **83 ms** | beste von 5 |
| M42‑0 Auswahl der Prüfwerte K5 (`NEXANS`, Merge-Ergebnis) | B | 1.687 ms | beste von 5 |
| M42‑0 Auswahl der Prüfwerte (`IBIS`) | B | 1.588 ms | beste von 5 |
| M42‑1 Anker 9022 / 9023 / 9020 (1 / 1 / 4 Treffer) | ohne | 0,611 / 0,612 / 0,679 ms | beste von 5 |
| M42‑1 Anker 9018 / 9015 / 9016 (540 / 1.182 / 1.182) | ohne | 6,349 / 11,191 / 11,117 ms | beste von 5 |
| M42‑1 Anker 9032 / 9033 (100.343 / 124.793) | ohne | 3.830,9 / 4.906,5 ms | beste von 5 |
| **M42‑1 Anker 9014 (234.159)** | **ohne** | **10.646,2 ms** | beste von 3 |
| M42‑1 **K1** 1 × 1, vier Fassungen | ohne | 0,663 / 0,710 / 0,669 / 0,635 ms | beste von 5 |
| M42‑1 **K2** 1 × 234.159, ohne `SJ`, beide Reihenfolgen | ohne | **0,672 / 0,710 ms** | beste von 5 |
| **M42‑1 K2 mit `SJ`, häufig zuerst** | ohne | **155,563 ms** ⚠ | beste von 5 |
| M42‑1 **K3a** 1.182 × 1.182, vier Fassungen | ohne | 28,770 / 27,310 / 27,829 / 28,317 ms | beste von 5 |
| **M42‑1 K3b** 100.343 × 124.793, vier Fassungen | ohne | **5.312,6 / 5.275,4 / 5.258,5 / 5.685,4 ms** | beste von 5 |
| M42‑1 **K3c** 1.182 × 234.159, ohne `SJ` | ohne | 27,546 / 26,656 ms | beste von 5 |
| **M42‑1 K3c mit `SJ`, häufig zuerst** | ohne | **2.641,5 ms** ⚠ | beste von 3 |
| M42‑1 **K5a** 1 × 1 auf der fetten Nachricht | ohne (Auswahl über B) | 0,677 / 0,695 / 0,661 / 0,689 ms | beste von 5 |
| M42‑1 **K5b** 1 × 719 | ohne (Auswahl über B) | 0,692 / 0,699 / 0,656 / 1,184 ms | beste von 5 |
| **M42‑1 K5c** 719 × 654, fette Kandidaten | ohne (Auswahl über B) | **940,8 / 926,7 / 1.278,5 / 934,2 ms** | beste von 5 |
| M42‑1 K5 Anker 9020 / 9024 / 9018 / 9019 | ohne | 0,632 / 0,593 / **8,289 / 7,797 ms** | beste von 5 |
| M42‑2 zwei / drei / fünf Begriffe, ohne `SJ` | ohne | **0,666 / 0,759 / 0,947 ms** | beste von 5 |
| M42‑2 zwei / drei / fünf Begriffe, mit `SJ` | ohne | 0,645 / 0,730 / 0,876 ms | beste von 5 |
| M42‑2 fünf, seltener Begriff zuletzt, ohne `SJ` | ohne | 1,031 ms | beste von 5 |
| **M42‑2 fünf, seltener Begriff zuletzt, mit `SJ`** | ohne | **1.128,3 ms** ⚠ | beste von 3 |
| M42‑3 Bauform ohne Selbstjoin — K1 | ohne | 0,858 ms | beste von 5 |
| **M42‑3 Bauform ohne Selbstjoin — K2** | ohne | **586,4 ms** | beste von 5 |
| M42‑3 Bauform ohne Selbstjoin — K3a | ohne | **15,962 ms** | beste von 5 |
| M42‑3 Bauform ohne Selbstjoin — K3b | ohne | **4.348,5 ms** | beste von 3 |
| M42‑3 Bauform ohne Selbstjoin — K3c | ohne | 612,4 ms | beste von 3 |
| **M42‑3 Bauform ohne Selbstjoin — fünf Begriffe** | ohne | **903,4 ms** | beste von 5 |
| **M42‑3 Bauform ohne Selbstjoin — K5c** | ohne (Auswahl über B) | **10,654 ms** | beste von 5 |
| M42‑4 `IBIS` K1, vier Fassungen | ohne | 0,713 / 0,686 / 0,694 / 0,681 ms | beste von 5 |
| M42‑4 `IBIS` zwei / drei / fünf Begriffe | ohne | 0,675 / 0,753 / 0,992 ms | beste von 5 |
| M42‑4 `IBIS` fünf mit `SJ` / Bauform ohne Selbstjoin | ohne | 0,986 / 0,878 ms | beste von 5 |
| M42‑4 `IBIS` Anker (drei Werte) | ohne | 0,606 / 0,655 / 0,647 ms | beste von 5 |
| M42‑4 `IBIS` Trefferzahlen aller 9.017 Werte des Fensters | B | 1.138,5 ms | beste von 5 |
| M43‑1 Längenverteilung, roh | B | 4.624,6 ms | beste von 5 |
| M43‑1 Längenverteilung, verdichtet | B | 4.632,9 ms | beste von 5 |
| **M43‑1 sechs Typen über den Bestand** | **ohne** | **209,9 ms** | beste von 5 (Regel L9) |
| M43‑2 gemischte Schreibweisen | B | 4.833,7 ms | beste von 5 |
| M43‑2 Nenner (numerische Werte je Typ) | B | 4.943,7 ms | beste von 5 |
| M43‑3 führende gegen folgende Leerzeichen | B | 3.249,9 ms | beste von 5 |
| M43‑3 Kollationsbeleg | — | < 1 ms | einmalig |
| M43‑4 Gegenprobe, acht Typen | ohne | 4,384 ms | beste von 5 |

**Kein Statement dieses Nachtrags hat die 60-Sekunden-Grenze erreicht**, und keines ist abgebrochen
worden. Das teuerste war der Vergleichsanker mit 234.159 Treffern (10,6 s) — dasselbe Statement, das
in M34 die Zeitgrenze des Lese-Pools reißt.

### Zweiter Nachtrag vom 12.08.2026 — M44

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M44‑0 Bytegrößen, Gegenkontrolle | — | 2,785 ms | einmalig |
| M44‑1 Spaltenliste `information_schema.COLUMNS` | — | < 5 ms | einmalig |
| M44‑1 Indexliste `information_schema.STATISTICS` | — | < 5 ms | einmalig |
| M44‑1 `EXPLAIN` der Zählung | — | < 5 ms | einmalig |
| **M44‑2 `COUNT(*)` über `MessageProperty`, kalt** | **ohne** | **199.379,5 ms** ⚠ | einmalig (Regel L9) |
| **M44‑2 `COUNT(*)` über `MessageProperty`, warm** | **ohne** | **20.632,6 ms** | einmalig |
| M44‑3 `COUNT(*)` über `Process` | ohne | 1,592 ms | einmalig |
| M44‑3 `COUNT(*)` über `User` | ohne | 1,386 ms | einmalig |
| M44‑3 `COUNT(*)` über `Project` | ohne | 0,678 ms | einmalig |

**Das teuerste Statement des zweiten Nachtrags ist zugleich das teuerste durchgelaufene Statement
der ganzen Runde** — und es liegt mit 199,380 s nur **0,4 %** über den 198,596 s von M33‑2 je Typ.
Die Nähe der beiden Zahlen sieht wie ein Zufall aus und ist keiner: Beide lesen einen mehrstelligen
Gigabyte-Bereich erstmals von der Platte, und beide kosten warm rund ein Zehntel davon (20,633 s
gegen 17,631 s). Teurer war in der ganzen Runde nur die Auswahlabfrage, die nach 679 s **ohne
Ergebnis** abgebrochen wurde.

### Dritter Nachtrag vom 13.08.2026 — M45

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M45‑0 Spalten `information_schema.COLUMNS` | — | < 5 ms | einmalig |
| M45‑1 alle 62 Zeilen, Längen | — | 1,261 ms | einmalig |
| M45‑2 Endungsverteilung, Unterstriche | — | < 3 ms | einmalig |
| M45‑3 Eindeutigkeit unter drei Vergleichsarten | — | < 3 ms | einmalig |
| M45‑3 Kollisionen, namentlich | — | < 3 ms | einmalig |
| M45‑4 Beinahe-Kollisionen | — | < 3 ms | einmalig |
| M45‑5 Konfiguration je Mandant (`MessageBAMMandant`) | — | **3,330 ms** | einmalig |
| **M45‑5 gemeinsames Vorkommen je Nachricht** | **B** | **3.516,2 ms** | einmalig |
| **M45‑5 Typkombination `Abladestelle`** | **B** | **4.650,4 ms** | einmalig |
| **E7 Werte mit innerem Leerzeichen je Typ** | **B** | **2.692,4 ms** | einmalig |
| E7 Nenner (BAM-Zeilen im Fenster) | B | 2.288,6 ms | einmalig |

### Vierter Nachtrag vom 13.08.2026 — M46

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M46‑0 Rahmen, Spaltentyp, PAD-SPACE-Beleg | — | < 3 ms | einmalig |
| **M46‑1 Grundaggregation, Optimiererwahl (`PRIMARY`)** | **ohne** | **12,238 s** | beste von 2 nach Aufwärmlauf |
| **M46‑1 dieselbe mit `FORCE INDEX (MessageBAM_BAMValue)`** | **ohne** | **11,703 s** | einmalig (Regel L15) |
| **M46‑1 Verdichtung je Typ und Schreibweise** | **ohne** | **12,369 s** | beste von 3 |
| **M46‑1 Verdichtung je Typ (Entscheidungsgrundlage)** | **ohne** | **11,878 s** | beste von 3 |
| M46‑1c Längen mit führender Null, typgebunden | ohne | **0,177 s** | beste von 2 |
| M46‑2 Sondierung, 13 Typen | ohne | 6,951 s | Aufwärmlauf |
| **M46‑2 je (Mandant, Typ), 13 Typen** | **ohne** | **6,663 s** | beste von 5 |
| **M46‑2 vollständig, Stapel 1 (9018)** | **ohne** | **34,855 s** | einmalig |
| **M46‑2 vollständig, Stapel 2 (9019, 9020)** | **ohne** | **43,647 s** | einmalig |
| **M46‑2 vollständig, Stapel 3 (9003, 9004)** | **ohne** | **41,908 s** | einmalig |
| **M46‑2 vollständig, Stapel 4 (9014–9017)** | **ohne** | **28,334 s** | einmalig |
| **M46‑2 vollständig, Stapel 5 (27 Typen)** | **ohne** | **42,891 s** | einmalig |
| M46‑2c Wirksamkeit je Paar | ohne | 3,033 s | einmalig |
| **M46‑3 erste Fassung (Join + `TRIM`)** | **ohne** | **abgebrochen bei 60 s** ⚠️ | — |
| **M46‑3 schlanke Fassung** | **ohne** | **9,558 s** | beste von 3 |
| M46‑3 Anteile in Summenform | ohne | 8,541 s | einmalig |

**Zwei Abweichungen vom Rahmen, beide ausgewiesen.** Die fünf Stapel von M46‑2 sind **einmalig**
gelaufen statt „beste von drei": Sie liegen zwischen 28,3 s und 43,6 s, drei Läufe je Stapel hätten
die Erhebung auf rund zehn Minuten gedehnt, und die Frage ist eine nach Verteilungen und nicht nach
Laufzeiten. Die Zerlegung in Stapel selbst ist die zweite: Ein Lauf über alle 36 Typen (rund
11,4 Mio. Zeilen) reißt die 60-Sekunden-Grenze rechnerisch, und die Grenze wird **nicht** ausgesetzt.

### Fünfter Nachtrag vom 13.08.2026 — M47

Gegen das **gebaute** Statement, gerendert aus dem Repository. Beste von fünf nach einem Aufwärmlauf;
bei Statements über einer Sekunde beste von drei nach einem Aufwärmlauf.

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M47‑0 Rahmen, `@@sql_mode`, Bytegrößen | — | < 5 ms | einmalig |
| M47‑1 Herleitung der Prüfwerte (vier Nachrichten) | A bzw. B | 0,1–2,5 s je Herleitung | einmalig je Sitzung |
| M47‑2 alle dreizehn `EXPLAIN` | 30 T / Jahr | < 10 ms | einmalig |
| **M47‑3 ein Begriff, typischer Wert** | **30 T / Jahr** | **1,095 / 1,089 ms** | beste von 5 |
| **M47‑3 ein Begriff, schlimmster Wert** | **30 T / Jahr** | **1.655,827 / 8.939,745 ms** | beste von 3 |
| M47‑3 Kern, 1 / 2 / 3 Fassungen | 30 T | 0,956 / 1,131 / **1,221 ms** | beste von 5 |
| M47‑3 Kern, 3 Fassungen | Jahr | 1,263 ms | beste von 5 |
| M47‑3 Kern, 2 Fassungen mit Typangabe | 30 T | 1,187 ms | beste von 5 |
| M47‑3 zwei Zeichen, 1 / 5 Fassungen | 30 T | 20,228 / **22,439 ms** | beste von 5 |
| M47‑3 **K2** selten × schlimmster, beide Reihenfolgen | 30 T | **1,209 / 1,202 ms** | beste von 5 |
| **M47‑3 K3b** häufig × häufig | **30 T / Jahr** | **881,975 / 4.202,697 ms** | beste von 3 |
| **M47‑3 K5c** fette Nachricht | **30 T / Jahr** | **6,911 / 935,046 ms** | beste von 5 · 3 |
| M47‑3 fünf Begriffe, seltener zuerst / zuletzt | 30 T | **1,622 / 1,607 ms** | beste von 5 |
| M47‑3 fünf Begriffe | Jahr | 1,834 ms | beste von 3 |
| M47‑3 `IBIS`, ein / zwei Begriffe | 30 T | **1,185 / 1,214 ms** | beste von 5 |
| M47‑3 zweite Abfrage (Trefferwerte) | — | **0,650 ms** | beste von 5 |
| M47‑3 Vollabzug der Kuratierung | — | **0,336 ms** | beste von 5 |
| **M47‑3 Gegenform: Anzeigespalten flach, schlimmster Wert** | **30 T** | **2.443,334 ms** | beste von 3 |

**Keine Abweichung vom Rahmen.** Kein Statement hat die 60-Sekunden-Grenze erreicht, keines ist
abgebrochen worden, keines lief ohne Zeitfenster über eine große Tabelle — Regel L9 ist nicht
berührt. Das teuerste Statement dieses Nachtrags ist der schlimmste Wert über ein Jahr mit
**8,940 s**; er liegt bei **89 Prozent** der Zeitgrenze des Lese-Pools und ist genau der Fall, für
den der Abbruchpfad `suche-abgebrochen` existiert.

**Der dritte Nachtrag ist der billigste der fünf.** Sein teuerstes Statement kostet **4,650 s** und
bleibt damit bei 7,8 % der 60-Sekunden-Grenze; die Stammdatenabfragen, aus denen der Hauptbefund
stammt, liegen sämtlich unter 3 ms. Kein Statement ist abgebrochen worden, und keines lief ohne
Zeitfenster über eine große Tabelle — Regel L9 ist hier gar nicht berührt.

**Die Statements der Anwendung stehen im Millisekundenbereich — bis auf einen.** Der schlimmste Wert
kostet ohne Zeitfenster 10,6 s und ist der einzige gemessene Fall, der die Zeitgrenze des Lese-Pools
reißt. Mit dem Standard-Zeitfenster von 24 Stunden kostet er 90,5 ms.

> **Fortgeschrieben am 13.08.2026 (M47).** Der Satz gilt weiter, und er ist jetzt am **gebauten**
> Statement gemessen statt an einer Nachbildung: Der Normalfall kostet **1,1 ms**, der schlimmste
> Wert **1,656 s** über die gebaute Vorgabe von 30 Tagen und **8,940 s** über ein Jahr. Der
> Abbruchpfad ist damit kein Vorbehalt für den Ausnahmefall, sondern die Zusicherung für die
> **Obergrenze** von Regel L1.

### Siebter Nachtrag vom 13.08.2026 — M49

*Der **sechste** Nachtrag (M48, Typenauswahl) hat in dieser Übersicht keinen eigenen Abschnitt; seine
drei Laufzeiten stehen in M48‑3. Das ist eine Lücke von damals und wird hier nicht stillschweigend
geschlossen — siehe die offene Frage am Ende.*

Die Erhebung zur Präfixsuche. **Zwei Blöcke laufen nach Regel L9 ohne Zeitfenster über
`MessageBAM`** (M49‑2 und M49‑4); die Begründung steht jeweils vor der Messung, die Kosten hier.

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M49‑0 Rahmen, Grants, Bytegrößen, `@@sql_mode` | — | < 5 ms | einmalig |
| **M49‑0 Vergleichsanker (M47, `IN`, typischer Wert)** | **30 T** | **1,160 ms** | beste von 5 |
| M49‑1 Herleitung der Prüfwerte (acht Typen) | — | 1,2–19,1 ms je Typ | einmalig je Sitzung |
| M49‑1 dominante Länge für 2001 / 9006 | — | 383 / 144 ms | einmalig |
| M49‑1 `UNION ALL`, 3 bis 6 Fassungen (acht Läufe) | **ohne** | 0,661–1,174 ms | einmalig |
| **M49‑1 `UNION ALL` 9036 ungekürzt / 9006** | **ohne** | **679,599 / 739,704 ms** | einmalig |
| **M49‑2a `k` = 3 / 4 / 5 / 6, Top-10** | **ohne** | **14.620 / 16.144 / 19.659 / 23.829 ms** | einmalig |
| **M49‑2a Bänder `k` = 4 / 6** | **ohne** | **15.996 / 23.770 ms** | einmalig |
| **M49‑2a Bänder `k` = 8** | **ohne** | **Abbruch bei 60 s** | — |
| **M49‑2a Typzuordnung der schlimmsten Präfixe** | **ohne** | **21.489 ms** | einmalig |
| M49‑2b je Typ, `k` = 4 / 6 — 9018 | **ohne** | 2.098 / 2.289 ms | einmalig |
| M49‑2b je Typ, `k` = 4 / 6 — Typ 3 | **ohne** | 2.000 / 2.350 ms | einmalig |
| M49‑2b je Typ, `k` = 4 / 6 — 9014 / 9015 | **ohne** | 383–428 / 354–358 ms | einmalig |
| M49‑2b je Typ, `k` = 4 / 6 — 0 / 9006 | **ohne** | 158–187 / 144–195 ms | einmalig |
| M49‑2b je Typ, `k` = 4 / 6 — 2000 / 2001 / 2002 | **ohne** | 1,0–15,9 ms | einmalig |
| M49‑3 elf `EXPLAIN` | 30 T / Jahr | < 10 ms | einmalig |
| **M49‑3 typischer Wert, vollständig als Präfix** | **30 T / Jahr** | **1,374 / 1,585 ms** | beste von 5 |
| **M49‑3 … um zwei Zeichen verkürzt** | **30 T** | **49,863 ms** | beste von 5 |
| **M49‑3 … um vier Zeichen verkürzt** | **30 T / Jahr** | **1.361,436 / 3.399,650 ms** | beste von 3 |
| **M49‑3 schlimmster Wert, vollständig** | **30 T** | **1.823,054 ms** | beste von 3 |
| M49‑3 zwei Begriffe, beide Reihenfolgen | 30 T | **1,270 / 1,279 ms** | beste von 5 |
| M49‑3 fünf Begriffe, seltener zuerst / zuletzt | 30 T | **1,840 / 1,910 ms** | beste von 5 |
| M49‑3 Nullen im Muster, vier `LIKE` im `OR` | 30 T | **1,205 ms** | beste von 5 |
| M49‑3 `IBIS`, vollständig / verkürzt | 30 T | **1,177 / 1,177 ms** | beste von 5 |
| **M49‑4 Sonde, eine Spalte** | **ohne** | **7.798 ms** | einmalig |
| **M49‑4 vollständiges Statement, fünf Spalten** | **ohne** | **21.412 ms** | einmalig |
| **M49‑4 Gegenprobe ohne `LIKE`** | **ohne** | **9.534 ms** | einmalig |
| M49‑4 `ESCAPE`- und PAD-SPACE-Belege | — | < 1 ms | einmalig |

**Das teuerste nicht abgebrochene Statement dieses Nachtrags kostet 23,829 s** und liegt damit bei
39,7 % der 60-Sekunden-Grenze. **Kein Statement der Anwendung** ist darunter: Die Erhebungen über den
Bestand sind Messwerkzeuge und kämen so nie in Anwendungscode (Regel L9, letzter Satz). Die
teuerste Fassung, die *als Endpunkt denkbar* wäre, ist der auf vier Zeichen verkürzte Präfix über
ein Jahr mit **3,400 s** — 34 % der Zeitgrenze des Lese-Pools.

### Achter Nachtrag vom 14.08.2026 — M50

Die Messung vor dem Bau von Teil 4. **Zwei Blöcke laufen nach Regel L9 ohne Zeitfenster über
`MessageBAM`** (die Präfixherleitung und die Kommazählung); die Begründung steht jeweils vor der
Messung, die Kosten hier.

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M50‑0 Rahmen, Grants, Bytegrößen, `@@sql_mode` | — | < 5 ms | einmalig |
| M50‑0 `COUNT(*)` über `MessageBAM` | **ohne** | **nicht gemessen** — in der Rahmensitzung ohne Profiling gelaufen | einmalig |
| **M50‑1 Herleitung des Präfixes** | **ohne** | **16.062 / 16.232 / 16.243 ms** | vier Sitzungen, **drei** protokolliert |
| M50‑1 Typzuordnung des Präfixes | **ohne** | **4.062 ms** | einmalig |
| M50‑2 vier `EXPLAIN` (zwei Fenster × zwei Fassungen) | 30 T / Jahr | < 3 ms | einmalig |
| **M50‑3 schlimmster Präfix des Bestands** | **30 T** | **3.851,152 ms** | **beste von drei** nach einem Aufwärmlauf (3,913 / 3,865 / 3,885 / 3,851 s) |
| **M50‑3 schlimmster Präfix des Bestands** | **ein Jahr** | **Abbruch bei 60 s** | im Aufwärmlauf, nicht wiederholt |
| M50‑4 `ESCAPE`- und PAD-SPACE-Belege (acht Ausdrücke) | — | < 1 ms | einmalig |
| **M50‑5 Kommazählung** | **ohne** | **7.731 ms** | Aufwärmlauf 7,824 s + ein Lauf |
| **M50‑5 Gegenprobe ohne `LIKE`** | **ohne** | **7.614 ms** | einmalig |

*Die vierte Herleitung ist die der abgebrochenen Jahressitzung: Sie hat ihr Ergebnis noch gemeldet
(`n = 1.332.180`, wie die drei anderen), ihre Laufzeit aber nicht — der Client hat die Sitzung beim
Abbruch des Folgestatements beendet, bevor `SHOW PROFILES` lief. **Die Zahl fehlt und wird nicht
geschätzt.***

**Das teuerste nicht abgebrochene Statement dieses Nachtrags kostet 16,243 s** — die
Präfixherleitung, ein Messwerkzeug und kein Anwendungsstatement. **Die teuerste Fassung, die als
Endpunkt tatsächlich läuft, ist der schlimmste Präfix über 30 Tage mit 3,851 s** und liegt damit bei
38,5 % der Zeitgrenze des Lese-Pools. Der Jahresfall ist **nicht** als Endpunkt denkbar — er ist
abgebrochen, und genau deshalb steht der Deckel im Code.

### Neunter Nachtrag vom 14.08.2026 — M51

Die Messung zur Behebung des Kommadefekts. **Drei Blöcke laufen nach Regel L9 ohne Zeitfenster über
`MessageBAM`** (Sonde, Gruppierung, Gegenprobe) und einer über `Process`; die Begründung steht
jeweils vor der Messung, die Kosten hier. **Alle Zeilen sind einmalig gemessen** — der Gegenstand
ist eine Verteilung und keine Uhr, wie in M49 und M50.

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M51‑0 Rahmen, Grants, Bytegrößen, `@@sql_mode` | — | < 5 ms | einmalig, ohne Profiling |
| **M51‑1 Sonde: Kommazählung (Kontrolle gegen M50‑5)** | **ohne** | **7.731 ms** | einmalig — **dieselbe Zahl wie M50‑5** |
| M51‑2 `EXPLAIN` der Gruppierung | **ohne** | 0,72 ms | einmalig |
| **M51‑2 Kommas je Typ** | **ohne** | **6.716 ms** | einmalig |
| **M51‑2 Gegenprobe ohne `LIMIT`** | **ohne** | **6.612 ms** | einmalig |
| M51‑3 Beschreibungen aus `MessageBAMType` | — | 0,82 ms | einmalig |
| M51‑3 Zuordnung aus `MessageBAMMandant` (zwei Abfragen) | — | < 1,1 ms | einmalig |
| **M51‑3 Nenner je betroffenem Typ** | **ohne** | **3.016 ms** | einmalig |
| **M51‑4 `ProcessID` mit Komma** | **ohne** | **3,0 ms** | einmalig |
| *Diagnose beim Bau: Kandidaten für die Testherleitung* | *B* | *2.574 / 2.531 ms* | *einmalig, zwei Abfragen* |

**Das teuerste Statement dieses Nachtrags kostet 7,731 s** — die Sonde, also ein Messwerkzeug und
kein Anwendungsstatement. **Kein Statement dieser Runde läuft je als Endpunkt**: M51 misst den
Bestand und nicht die Suche. Die Behebung selbst ändert am Statement der Suche nichts (siehe
[`bam-suche.md`](bam-suche.md) §9), und deshalb ist zu ihr auch nichts zu messen gewesen.

## Die sechs Abweichungen vom Rahmen

Sie stehen hier zusammen, damit sie nicht in den Tabellen untergehen. **Die vierte unterscheidet
sich von den übrigen grundsätzlich: Sie war vorher entschieden und begründet, die anderen sind
passiert.** Die fünfte und die sechste sind die beiden, bei denen die Grenze tatsächlich
**gegriffen** hat.

| | Statement | was passiert ist |
|---|---|---|
| 1 | M33 Grundaggregation | **62,780 s** — Grenze um 4,6 % gerissen, Kaltlauf, nicht abgebrochen |
| 2 | M33‑2 je Typ, mit Join | **198,596 s** — Grenze um Faktor 3,3 gerissen, Kaltlauf, nicht abgebrochen |
| 3 | Auswahl der Prüfwerte, erste Fassung | **679 s ohne Ergebnis**, client-seitig abgebrochen, serverseitig mit `KILL QUERY` beendet |
| **4** | **M44‑2 `COUNT(*)` über `MessageProperty`** | **199,380 s** — die 60-Sekunden-Grenze war für dieses eine Statement **vorab ausgesetzt** und durch `SET max_statement_time = 900` ersetzt. Kein Abbruch: 22,2 % der gesetzten Grenze |
| **5** | **M46‑3, erste Fassung** | **serverseitig abgebrochen bei 60 s** (`ERROR 1969`). Die Grenze war gesetzt und hat gegriffen — kein Blockieren, kein `KILL QUERY`. Die schlanke Fassung ohne `LEFT JOIN` und ohne `TRIM` kostet **9,558 s**, also 15,9 % der Grenze |
| **6** | **M49‑2a, Bänder bei `k` = 8** | **serverseitig abgebrochen bei 60 s** (`ERROR 1969`). **Kein Ausweichen und keine zweite Fassung** — der Abbruch ist als Ergebnis stehen geblieben. Die Zahl der Präfixe wächst von 12.915 (k = 4) über 241.737 (k = 6) auf eine Größe, bei der die temporäre Tabelle die Grenze reißt; die Reihe endet damit bei k = 6. **Die Frage dahinter bleibt trotzdem beantwortet**, weil der Boden bei 234.159 aus M49‑2a und M33 folgt und nicht aus k = 8 |

Fall 1 und 2 sind vor der Einführung von `SET max_statement_time = 60` gelaufen; der Client blockiert
bis zum Ergebnis, ein Abbruch „bei 60 Sekunden" war deshalb nicht möglich. Ab Fall 3 lief jede
Sitzung mit der serverseitigen Grenze, und danach ist kein Statement mehr über sie gekommen. **Beide
Kaltläufe sind warm um den Faktor 6,0 beziehungsweise 11,3 billiger** (10,421 s und 17,631 s, je
beste von drei) — das ist der eigentliche Befund und der Grund, die Zahlen nicht einfach zu
verwerfen.

**Fall 5 ist der erste, bei dem der Schutz getan hat, wofür er da ist.** Er steht hier nicht als
Malheur, sondern weil er den Unterschied zu Fall 1 und 2 zeigt: Dort lief der Client ins Offene, hier
kam nach 60 Sekunden ein Fehler und die Sitzung war frei. Der Auslöser war zudem kein teurer
Zugriffspfad, sondern drei Kleinigkeiten in einem Statement — ein `LEFT JOIN` auf 62 Stammdatenzeilen,
eine Gruppierung über deren Beschreibung und ein `TRIM` je Zeile. **Dieselbe Frage ohne sie kostet
ein Sechstel.**

**Fall 4 ist keine Panne, sondern eine ausgewiesene Ausnahme nach Regel L9.** Die Begründung stand
vor der Messung fest — eine Tabelle von 61,03 GB lässt sich in 60 Sekunden nicht zählen, und die
Frage gilt dem Bestand — und die gemessenen Kosten stehen in M44. Sie gilt für **dieses eine
Statement** und nicht für die Runde: Alle übrigen Statements des zweiten Nachtrags liegen unter
5 ms. Und sie ist ausdrücklich **kein** Statement der Anwendung.

### Zwei weitere Abweichungen des siebten Nachtrags (M49) — beide methodisch, keine an einem Statement

Sie stehen getrennt, weil sie nicht in die Spalte „was passiert ist" gehören: Kein Statement ist an
ihnen gescheitert, es ist **anders gemessen worden als der Rahmen es vorschreibt**.

**A — `--ssl-mode=DISABLED` statt `--skip-ssl`.** Der Auftrag schreibt `--skip-ssl` vor. Der Client
dieser Runde — `mysql` 8.0.46 aus MySQL Workbench, **derselbe wie in Schritt 4, 5, 6 und in M32 bis
M48** — kennt diese Option nicht und bricht mit `unknown option '--skip-ssl'` ab, bevor eine Sitzung
zustande kommt. Gewählt ist die wirkungsgleiche Option desselben Clients, mit der auch alle früheren
Runden gemessen haben. **Die Alternative wäre ein anderer Client gewesen** (der MariaDB-Client kennt
`--skip-ssl`) — und damit der Verlust der Vergleichbarkeit mit M32 bis M48, also genau das, was der
Vergleichsanker in M49‑0 sicherstellen soll. Die Verschlüsselung ist in beiden Fassungen aus.

**B — die Erhebungen über den Bestand sind *einmalig* gemessen, nicht als beste von drei.** Der
Rahmen verlangt bei Statements über einer Sekunde die beste von drei nach einem Aufwärmlauf.
Eingehalten ist das für **alle** Statements aus M49‑3, also für jede Zahl, die den Endpunkt betrifft.
Nicht eingehalten ist es für die zwölf Erhebungen aus M49‑2 und M49‑4, die zwischen 2 und 24 Sekunden
kosten. Der Grund steht hier und nicht in einer Fußnote: **Ihr Gegenstand ist die Verteilung und
nicht die Uhr** — die Trefferzahlen sind exakt und wiederholungsunabhängig, und vier Läufe je
Statement hätten der geteilten Testkopie rund fünf Minuten zusätzliche Last aufgeladen, ohne eine
einzige Folgerung zu ändern. **Die Laufzeiten dieser zwölf Zeilen sind deshalb Obergrenzen mit
Kaltlaufanteil und keine Bestwerte** — sie sind in der Laufzeittabelle als „einmalig"
gekennzeichnet und tragen keinen Befund.

### Die eine Abweichung des achten Nachtrags (M50) — dieselbe wie A, und aus demselben Grund

**`--ssl-mode=DISABLED` statt `--skip-ssl`.** Der Auftrag zu Teil 4 schreibt `--skip-ssl` erneut
vor, und der Client kennt die Option erneut nicht. Gemessen ist mit derselben wirkungsgleichen
Option wie in M49 und in allen Runden davor. **Sie steht hier ein zweites Mal, weil sie ein zweites
Mal aufgetreten ist** — und nicht, weil sie neu wäre.

> **Der Rest des Rahmens ist eingehalten**, und zwar auch dort, wo M49 abgewichen ist: Die beiden
> Erhebungen über den Bestand sind zwar einmalig gemessen (Präfixherleitung, Typzuordnung), **die
> Kommazählung aber zweimal und der Endpunktfall als beste von drei nach einem Aufwärmlauf** — für
> jede Zahl, die den gebauten Endpunkt betrifft, gilt die Regel des Rahmens ohne Einschränkung.

### Die eine Abweichung des neunten Nachtrags (M51) — zum dritten Mal dieselbe

**`--ssl-mode=DISABLED` statt `--skip-ssl`.** Derselbe Client, dieselbe fehlende Option, dieselbe
wirkungsgleiche Ersetzung. **Sie steht hier ein drittes Mal, weil sie ein drittes Mal aufgetreten
ist.**

> **Und eine zweite, die keine Ausnahme ist, sondern eine Ansage:** M51 misst **jede** Zeile nur
> **einmal**. Das ist keine Abweichung vom Rahmen, sondern die Regel, die M49 und M50 für
> Bestandserhebungen schon angewandt haben — nur gilt sie hier für die **ganze** Runde, weil sie
> keine einzige Zahl über einen laufenden Endpunkt erhebt. **Die Laufzeiten dieses Nachtrags sind
> deshalb durchweg Obergrenzen mit Kaltlaufanteil und tragen keinen Befund.**

---

# Wo die vorformulierte Zeile nicht passte

**Achtunddreißig** Stellen, einzeln benannt und jeweils als eigener Absatz unter der Tabelle
ausgewiesen — elf aus der Hauptrunde vom 11.08.2026, sieben aus dem ersten und drei aus dem zweiten
Nachtrag vom 12.08.2026, drei aus dem vierten, vier aus dem fünften und **sechs aus dem siebten**
vom 13.08.2026, dazu **zwei aus dem achten** und **zwei aus dem neunten** vom 14.08.2026.

> **Der sechste Nachtrag (M48) ist in dieser Tabelle nicht geführt**, obwohl er einen solchen Befund
> hat — dass `MessageBAMTypeSortIndex` **nicht eindeutig** ist, stand in keiner vorformulierten
> Zeile. Er ist in M48 als eigener Absatz ausgewiesen und nur hier nicht eingetragen. **Das ist eine
> Lücke von damals; sie wird hier benannt und nicht nachträglich gefüllt** — die Zählung dieser
> Tabelle gehört zu der Runde, die sie geschrieben hat. Siehe die offene Frage am Ende.

| # | Messung | Der Befund, der in keine Zeile passte |
|---:|---|---|
| 1 | M32 | Der zusammengesetzte Index trägt den **Typ voran** und ist für die typlose Suche unbrauchbar |
| 2 | M32 | Drei der fünf Indizes stehen auf `MessageID`; 5,25 GB Index gegen 1,83 GB Daten |
| 3 | M33 | `MessageBAM` hat **15.406.350** Zeilen statt der dokumentierten 10.859.666 |
| 4 | M33 | Der schlimmste Einzelwert gehört **nicht** zur Abladestelle, sondern zu Typ 9014 |
| 5 | M34 | `STRAIGHT_JOIN` kippt den Plan **zwischen Suchformen**, nicht zwischen Mandanten |
| 6 | M35 | „Weite Vorgabe" wäre die falsche Folgerung — nur ein **enges** Fenster schützt |
| 7 | M35 | Das Fenster verändert die **Antwort**: 279 von 234.159 Treffern bei 24 Stunden |
| 8 | M36 | „Gleich schnell" trifft zu, aber der zusammengesetzte Index **wird** benutzt |
| 9 | M37 | Die Erklärung stimmt, das Paar nicht: 9028/9029 statt 9032/9033 |
| 10 | M39 | Die tragende Rolle ist je Mandant eine andere — **vier** verschiedene, nicht nur die Wurzel |
| 11 | M40 / M41 | `ZAST` trägt **441 Werte je Nachricht** und erzwingt die Deckelung — der zweitkleinste Mandant |
| **12** | **M42** | Der Zusatzbegriff läuft über `MessageBAM_BAMValueOnly` mit `rows` **1**, nicht über den Primärschlüssel-Präfix mit `rows` 8. Die Vermutung des Auftrags ist im Ergebnis richtig und in der Begründung falsch |
| **13** | **M42** | **`STRAIGHT_JOIN` erzeugt die Reihenfolgeabhängigkeit, statt sie zu beheben** — Faktor 219 (K2) bis 1.094 (fünf Begriffe) |
| **14** | **M42** | Die Verundung entlastet nicht nur oder gar nicht — sie kann auch **belasten**: zwei Begriffe ähnlicher Häufigkeit kosten mehr als jeder allein (K3a 27,3 gegen 11,1 ms) |
| **15** | **M42** | Bei `IBIS` gibt es den häufigen Wert nicht (größte Trefferzahl **216**), und die Nachrichten tragen bis zu **158** Werte statt der vom Auftrag erwarteten vier |
| **16** | **M43** | „Eine Länge dominiert" und „die Längen streuen" sind keine Alternative, sondern eine **Aufteilung** der Typen — und die Trennlinie verläuft quer zu den tragenden Suchtypen |
| **17** | **M43** | Typ **2001** widerlegt die Ableitung aus einem Monat **innerhalb derselben Erhebung**: 100 % über Fenster B, **67,21 %** über den Bestand |
| **18** | **M43** | Bei kurzen Kernen dreht die Gegenprobe ihr Vorzeichen um: 9006 roh **1.642** Treffer, aufgefüllt **4** — die rohe Fassung findet mehr und davon fast nichts Richtiges |
| **19** | **M44** | Die neue Zahl **löst einen dokumentierten Widerspruch auf, statt einen zu erzeugen**: 14,05 gegen 22,57 Zeilen je Nachricht war nie ein Dichteeffekt, sondern die um 60,9 % zu niedrige Schätzung. Gezählt sind es **22,62** über den Gesamtbestand |
| **20** | **M44** | Die „1,3 Kilobyte je Zeile" sind aus derselben Schätzung gerechnet und werden zu **808 Byte**. Die Verhältnisaussage „zu drei Vierteln Index" hält (75,3 %), die absoluten Werte nicht |
| **21** | **M44** | **Sechs der sieben Zeilen** des Mengengerüsts in §8 waren `information_schema`-Schätzungen, nicht zwei — `Process` (1.503 statt 1.490) und `Project` (140 statt 142) sind seit dem 01.08.2026 gezählt und nie nachgezogen worden |
| **22** | **M46** | **Die Regel kennt die Wirksamkeit nicht.** Sie fragt nach der Längendominanz und trifft zwei Paare, bei denen kein Wert *mit* führender Null auf der Sollänge liegt: `IBIS`/1 und `SUTTONS`/2000 |
| **23** | **M46** | **Die Schwelle schließt ausgerechnet 9006 aus** — 94,21 %, 0,79 Prozentpunkte darunter. Genau den Typ, für den M43‑4 die Wirkung des Auffüllens belegt hat (roh 1.642, aufgefüllt 4) |
| **24** | **M46** | **Der Bestand kennt 55 Typen, Fenster B kannte 46.** Drei der neun zusätzlichen tragen auf 100 % ihrer Werte eine führende Null und werden kuratiert — eine Kuratierung aus Fenster B hätte sie nicht gekannt |
| **25** | **M47** | **Wo die Anzeigespalten hängen, ist der teuerste Fehler dieses Statements — und er steht in keiner Zeile des Auftrags.** Neben statt über der Deckelung kostet **Faktor 1,48** (2.443 gegen 1.656 ms), bei **identisch gutem** `EXPLAIN`. Derselbe Fehler wie in Teil 1, dort Faktor 18,4 |
| **26** | **M47** | **Bei K5c greift die `rows`-8-Falle aus M42‑1 K5 mit Zeitfenster nicht mehr:** 926,7 ms werden **6,9 ms**. Nicht die Bauform fängt den Fall auf, sondern Regel L1 |
| **27** | **M47** | **`NEXANS` hat acht Sollängen auf zehn Zeilen, nicht elf.** Der Auftrag zu Teil 2b nennt „elf Sollängen"; die Zahl der *verschiedenen* ist mit drei richtig, und nur auf die kommt es an |
| **28** | **M47** | **Die zweite Abfrage wählt `MessageBAM_MessageFK` und nicht das Präfix des Primärschlüssels.** Derselbe Zugriffstyp, dasselbe `Using index`, ein anderer der drei Indizes, die auf `MessageID` beginnen (M32) |
| **29** | **M49‑1** | **Zwei der acht Prüftypen füllen gar nicht auf.** 2001 und 9006 stehen nicht in `bam_sollaenge` — für sie entsteht der gemessene Widerspruch nicht, weil es keine Normalisierung gibt, gegen die ein Präfix ankern könnte. **Bei ihnen wäre Präfix sofort wirksam und sofort teuer** |
| **30** | **M49‑1** | **Bei 9036 lässt sich der Prüfwert nicht verkürzen** — sein Kern ist **ein** Zeichen lang, `LEFT(kern, −1)` ist die leere Zeichenkette und `LIKE '%'` der ganze Bestand. Der Auftrag setzt voraus, dass zwei Zeichen immer abziehbar sind. Die ungekürzte Fassung kostet **1.086.626** Treffer in der ersten Fassung |
| **31** | **M49‑2a** | **Die schlimmste Trefferzahl hat einen Boden, und keine Zeile fragt danach.** Bei k = 6 steht sie auf **234.159** — der Trefferzahl **eines exakten Werts** (M33). Keine Präfixlänge unterschreitet ihn. **Der Bösfall ist kein Präfixproblem**, er ist im exakten Bestand schon da |
| **32** | **M49‑2a** | **Drei der zehn schlimmsten Präfixpaare gehören Typ 9019 „Bestellnummer vom Kunden"** (410.030 / 190.216 / 161.067). Die Zeile erwartet den Schnitt „Kennungen gefährlich, Belegnummern harmlos" — er trennt nicht die gefährlichen von den **gesuchten** Feldern |
| **33** | **M49‑3** | **Schon der vollständige Wert als Präfix findet 23 statt 1.** Die Bedingung „bei gleicher Trefferzahl" aus M34 ist damit **bei keiner Eingabelänge** erfüllt; E6s Zeile „8 (vollständig) → 1 Treffer" ist die exakte Zählung und keine `LIKE`-Zählung. Für die Leistung folgenlos, für die Fachlichkeit der Kern |
| **34** | **M49‑4** | **602.794 Werte (3,91 %) tragen ein folgendes Leerzeichen.** Der Auftrag führt die Randleerzeichen ausdrücklich als *Folgerung* und nicht als Messung — sein eigenes Statement zählt sie mit, und 3,91 % sind kein Randfall. Zum Vergleich: Das **führende** Leerzeichen steht bei 9018 mit 1,349624 % in der Kuratierung |
| **35** | **M50‑2** | **Der Plan kippt, und der Auftrag setzt das Gegenteil voraus.** Er schreibt, M49‑3 habe belegt, dass der Optimierer „auch mit `LIKE` über den seltensten Begriff einsteigt". Beim schlimmsten Präfix des Bestands tut er es **nicht**: Er steigt über `MessageLastUpdateIDX` ein und probt `MessageBAM` über den Primärschlüssel; `MessageBAM_BAMValueOnly` steht nur noch unter `possible_keys`. **Die Wahl ist richtig** — und sie ist das schärfste Argument gegen ein `STRAIGHT_JOIN`, das dieses Projekt hat |
| **36** | **M50‑5** | **Das Komma, und keine Zeile fragt danach.** **55.989 Werte (0,363 %) tragen eines** — 95‑mal so viele wie den Doppelpunkt (585, M49‑4). Anders als der Doppelpunkt ist es **nicht** entschärft: Spring zerlegt den wiederholbaren Parameter daran, und die Werte sind heute **unsuchbar** — in **beiden** Modi und seit Teil 2b. Gefunden beim Bau des Isolationstests, nicht beim Messen |
| **37** | **M51‑3** | **Der Kommadefekt war ein Mandantenproblem und kein Bestandsproblem.** Die drei betroffenen Typen (9003, 9016, 9018) sind bei **genau einem** der sieben konfigurierten Mandanten geführt. „0,363 % des Bestands" liest sich wie gleichmäßiges Rauschen; tatsächlich saß der Defekt bei **einem** Mandanten auf **jedem zwanzigsten** Wert seines Typs 9003 (5,05 %). Keine Zeile fragte nach dem Mandanten — und genau deshalb ist der Defekt der Herleitung eines **Isolations**tests aufgefallen |
| **38** | **M51‑2** | **Der gruppierende Durchlauf ist billiger als der zählende.** `GROUP BY MessageBAMType` schickt den Optimierer auf `MessageBAM_BAMValue` (`key_len` 284) statt auf `PRIMARY` (430, M49‑4 und M50‑5) — **6,716 s gegen 7,731 s**, obwohl zusätzlich gruppiert und sortiert wird. Erwartet war „dieselbe Gestalt, etwas teurer". Folgenlos für jede Aussage dieser Runde, aber es passte in keine Zeile |

Dazu **fünf** Befunde, die eine Zeile zwar treffen, aber über sie hinausreichen und deshalb ebenfalls
als eigener Absatz stehen: die Null der Merge-Eingänge über Fenster B (M39), die fehlende
Tragfähigkeit jedes Typs auf Zeilen ohne Kette (M39), das Maximum von 9.296 Werten auf einem
**Kind** (M41), die nur **1,9-fache** Reserve von K3b zur Zeitgrenze des Lese-Pools (M42) und die
**1,05 %** führenden Leerzeichen bei 9018, die neben den 24,83 % folgenden fast verschwinden und
trotzdem rund 1.500 Zeilen unerreichbar machen (M43‑3).

---

# Was diese Runde ausdrücklich **nicht** misst

Streichungen stehen als Streichung; sie sind durch den Nachtrag vom 12.08.2026 oder durch das
Sparring desselben Tages erledigt und **nicht** entfernt worden.

- **Die Verteilung der Werte je (Nachricht, Typ).** M41 misst je Nachricht, M11 je Typ über die
  ganze Menge. Die Zahl, die eine Deckelung **je Typgruppe** bemessen würde, fehlt — offene Frage 6.
  *Der Nachtrag liefert eine benachbarte, aber nicht dieselbe Zahl: Die 654 Kandidatennachrichten in
  M42‑1 K5 tragen zusammen 1.634.605 Werte, also **2.499 je Nachricht** über alle Typen.*
- **Ob ein Wert unter mehreren Typen stark ungleich verteilt ist.** M37 zählt die Fälle, M36 misst
  den Nutzen der Typeinschränkung an Werten mit nur einem Typ. Der Fall, in dem sie wirklich hilft,
  ist ungemessen.
- **Wie sich der schlimmste Fall unter Last verhält.** Alle Zahlen stammen von einer ruhenden
  Testkopie. Die 10-Sekunden-Grenze gilt in Produktion. *Der Nachtrag ändert daran nichts und fügt
  einen zweiten Fall hinzu: K3b liegt mit 5,275 s bei **Faktor 1,9** Reserve.*
- **Welche zwei weiteren Typen `ZAST` über den Bestand trägt.** M40‑3 nennt die Anzahl (3), nicht
  die Nummern.
- ~~**Ob die Suche exakt, präfixweise oder wahlweise arbeitet.** Das ist eine fachliche Entscheidung
  nach den Zahlen und fällt im Sparring.~~ **Entschieden im Sparring vom 12.08.2026:** exakt, mit
  typgebundener und sichtbar gemachter Normalisierung. Siehe offene Frage 3.
- ~~**Ob und wie normalisiert wird.** M38 belegt, dass führende Nullen und Randleerzeichen vorkommen.
  Welche Behandlung daraus folgt, ist nicht entschieden.~~ **Das *Ob* ist im Sparring vom 12.08.2026
  entschieden** (offene Frage 4), das *Womit* misst **M43**: Sollänge je Typ, Trennung von führenden
  und folgenden Leerzeichen, Gegenprobe.

### Was der Nachtrag neu offen lässt

- ~~**Die Verundung mit Zeitfenster.** Alle Zahlen aus M42 sind **ohne** Fenster gemessen, damit sie
  gegen M34 stehen. Ob das Fenster K3b (5,275 s) und K5c (926,7 ms) ebenso senkt wie in M35 den
  schlimmsten Einzelwert, ist ungemessen — und es ist die Zahl, die eine Deckelung der Verundung
  überflüssig machen oder erzwingen würde.~~ ✔ **Gemessen am 13.08.2026 in M47:** Das Fenster senkt
  **beide**, K3b auf **882 ms** (Faktor 6,0) und K5c auf **6,9 ms** (Faktor 134). Eine eigene
  Deckelung der Verundung wird damit **nicht** gebraucht.
- **Kombinationen, die es im Bestand nicht gibt.** M42‑0 wählt Werte, die **gemeinsam** auf einer
  Nachricht stehen. Der Fall „leeres Ergebnis" — genau der, den ein Nutzer erzeugt, der sich
  vertippt — ist nicht gemessen.
- **Mehr als fünf Begriffe.** Die Reihe endet bei fünf. Acht oder zehn sind die Zahlen, die ein
  Schutzgeländer tragen soll; sie stehen hier nicht.
- **Ob die Sollänge über die Zeit stabil ist — für die anderen 40 Typen.** M43‑1 misst den Bestand
  nur für sechs. Typ 2001 zeigt, dass die Frage nicht rhetorisch ist.
- **M43‑2 über den Bestand.** Die gemischten Schreibweisen (9017 mit 52,98 %, 9020 mit 20,10 %) sind
  ausschließlich über Fenster B gemessen.
- **Die Laufzeit einer normalisierenden Suche.** M43‑4 misst die **Trefferzahl** der aufgefüllten
  Fassung, nicht die Laufzeit einer Suche, die beide Fassungen absucht. Nach E6 ist die Trefferzahl
  die Kostengröße — aber gemessen ist die Kombination nicht.
- **Die Behandlung des führenden Leerzeichens.** M43‑3 misst, dass es bei 9018 auf 1,05 % der Zeilen
  vorkommt und dass es beim `=`-Vergleich zählt. Ob es wie die führende Null sichtbar behandelt
  wird, ist eine Entscheidung.

### Was der zweite Nachtrag (M44) neu offen lässt

- **Ob die Testkopie wirklich eine Vollkopie ist.** Die Annahme trägt jede Zahl dieses Projekts, das
  Mengengerüst voran, und ist nie geprüft worden — es besteht kein Zugang zur Produktion. Sie steht
  im Belegvermerk zu M44 als benannte Lücke und wäre über eine einzige Zeilenzahl aus der Produktion
  zu erledigen.
- **Die Zeilenzahlen der übrigen 17 Basistabellen.** M44 zählt vier. Welche der restlichen Zeilen in
  `information_schema` brauchbar sind, sagt diese Runde nicht — sie sagt nur, dass die Frage bei
  großen Tabellen anders ausgeht als bei kleinen.
- **Ob ein Sekundärindex die Zählung billiger machen würde.** Der Optimierer wählt `PRIMARY` und
  liest damit 15,09 GB; die fünf Sekundärindizes belegen im Mittel rund 9 GB. Erzwungen und
  gegengemessen ist das nicht — für die Frage nach der Zeilenzahl war es gleichgültig.

### Was der dritte Nachtrag (M45) neu offen lässt

- **Was `_L_SAP`, `_K_SAP` und `_FORS` bedeuten.** M45‑5 belegt, dass die Endung **unterscheidet** —
  jede der drei Kollisionen paart verschiedene Endungen, keine dieselbe. **Was** sie unterscheidet,
  ist erschlossen und nicht gemessen; die Datenbank kann es nicht sagen. Zu klären wäre es beim
  Betreuer des Altsystems, nicht mit einer weiteren Abfrage.
- **Ob `Bestellnummer` und `Rechnungsnummer` je auf derselben Nachricht zusammentreffen.** Über
  Fenster B tun sie es nicht. Ein Monat ist kein Bestand, und die Aussage ist eine **negative** —
  sie trägt schwächer als die 3.405 Fälle bei `Abladestelle`.
- **Ob ein Nutzer `Transport-Nummer` von `Transportnummer` unterscheidet.** M45‑4 findet zwei Paare,
  die sich um ein einziges Satzzeichen unterscheiden. Als Zeichenketten sind sie verschieden, für
  den Leser vermutlich nicht — gemessen ist nur das Erste.
- **Ob die Stammdaten der Produktion dieselben 62 Zeilen führen.** Die Kopie ist vom 08.07.2026. Ein
  seither angelegter Typ könnte eine heute eindeutige gekürzte Form kollidieren lassen, ohne dass es
  jemand bemerkte.

### Was der vierte Nachtrag (M46) neu offen lässt

- **Welche der drei Lesarten der 95-Prozent-Regel die richtige ist.** M46 entscheidet an der Dominanz
  über **alle** Werte eines Paares. Die Gruppendominanz („nur die Werte mit führender Null" oder „die
  größere der beiden Gruppen", wie sie M43 in seinen Bändern verwendet) ergäbe eine **andere**
  Kuratierung — 9020 käme mit 98,78 % hinein, 9015 mit 97,56 %. Die Zahlen stehen in M46‑1, die
  Festlegung ist keine Messung.
- ~~**Ob ein Eintrag ohne Wirkung schadet.** `IBIS`/1 und `SUTTONS`/2000 fügen je Suche eine Variante
  hinzu, die im Bestand nichts trifft. Was diese Variante an Laufzeit kostet, ist **nicht gemessen**
  — M46 misst Verteilungen, keine Suchen. Die Zahl fehlt und gehört in M47.~~ ✔ **Gemessen am
  13.08.2026 in M47:** Eine zusätzliche Variante kostet rund **0,1 ms** (0,956 → 1,131 ms von einer
  auf zwei Fassungen). **Der Preis ist damit kein Argument in der offenen Frage** — die Frage ist,
  ob die Kuratierungsregel sagen soll, was sie meint, und das bleibt eine Entscheidung.
- **Ob die Kuratierung 9006 fehlt.** Der Typ liegt 0,79 Prozentpunkte unter der Schwelle, trägt auf
  32,99 % führende Nullen und ist der einzige, für den M43‑4 die Wirkung an echten Werten belegt hat.
  Ob die Schwelle richtig gesetzt ist oder 9006 eine benannte Ausnahme bekommt, ist eine
  **Entscheidung** und steht als offene Frage 14.
- **Ob die Sollänge über die Zeit stabil ist.** M43 ließ das für 40 Typen offen; M46 hat den Bestand
  für alle erhoben und die Lücke damit verschoben, nicht geschlossen — gemessen ist der Stand vom
  08.07.2026. `BamSollaengeDriftDbIT` prüft es, läuft aber nicht in der CI.
- **Ob die Produktion dieselbe Verteilung trägt.** Dieselbe benannte Lücke wie im Belegvermerk zu
  M44: Es besteht kein Zugang zur Produktion.
- **Ob ein Mandant, der heute einen Typ nicht trägt, ihn morgen trägt.** Die Kuratierung ist nach
  `(mandant_id, bam_typ)` geschlüsselt und kennt nur die 45 gemessenen Paare. Ein neuer Mandant oder
  ein neuer Typ erscheint schlicht nicht darin, und der Drift-Test bemerkt es nicht — er prüft, was
  dasteht, nicht was fehlt.

### Was der sechste Nachtrag (M49) neu offen lässt

**Die drei Lücken, die der Auftrag selbst benennt, stehen zuerst — sie sind die wichtigsten, und
keine davon ist eine Nachlässigkeit dieser Runde:**

1. **Wie oft die exakte Suche heute leer ausgeht.** Das ist die Zahl, die die Empfehlung „Präfix nur
   als zweite Geste im Nulltreffer-Fall" tragen würde — **und sie steht in keinem Bestand.** Es gäbe
   sie nur aus einem Suchprotokoll, das die Anwendung nicht führt und das zu führen eine eigene
   Entscheidung wäre (die BAM-Suche schreibt heute nichts ins `audit_log`; protokolliert werden
   Anmeldung, Sperre, Passwortwechsel, Katalogänderung und Download).
   **Ohne diese Zahl ist der Nutzen der Präfixsuche unbeziffert, während ihre Kosten es sind.**
2. **Wie sich all das unter Last verhält.** Dieselbe benannte Lücke wie im Belegvermerk zu M35, M47
   und in jeder Runde davor: Gemessen ist eine **ruhende** Testkopie, die 10-Sekunden-Grenze des
   Lese-Pools gilt in der **Produktion**. Alle Zahlen sind Untergrenzen und keine Zusagen.
3. **Ob die Produktion dieselbe Werteverteilung trägt.** Der Bestand hier hat den Stand
   **08.07.2026**. Die gesamte Aussage über Präfixhäufigkeiten hängt daran — insbesondere die
   Feststellung, dass `%` im Bestand **nicht vorkommt**.

**Dazu vier, die aus den Messungen selbst entstanden sind:**

- **Wie viele Fassungen die Anwendung im Betrieb bilden müsste.** M49‑1 zählt drei bis sieben je
  Prüfwert bei **einem** Begriff. Die Zahl hängt an der Eingabelänge des Nutzers, und das Geländer
  der Suche steht bei **acht** Begriffen. Gemessen ist die Nullen-im-Muster-Fassung mit **vier**
  `LIKE` bei einem Begriff (1,205 ms) — nicht mit sieben und nicht bei acht Begriffen.
- **Was der schlimmste Präfix des Bestands durch das gebaute Statement kostet.** M49‑2a kennt einen
  Vierzeichen-Präfix mit **1.332.180** Zeilen. Gemessen ist durch das Statement der schlimmste
  *Wert* (234.159) und ein Vierzeichen-Präfix mit 155.871. **Der teuerste bekannte Fall ist nicht
  gelaufen**, und er ist um Faktor 8,5 größer als der teuerste gemessene.
- **Wie schnell die Zahl der gefährlichen Präfixe zwischen sechs und zehn Zeichen abnimmt.** Die
  Reihe bricht bei k = 8 an der 60-Sekunden-Grenze ab (Abweichung 6). Der **Boden** bei 234.159 folgt
  ohne diese Zahlen; die **Menge** der Bösfälle nicht.
- **Welche Typen die 602.794 folgenden Leerzeichen tragen.** M49‑4 zählt sie typlos. Ob es dieselben
  sind, die **M43‑3** mit 24,83 % *folgenden* bei 9018 ausweist, ist nicht erhoben — und davon hinge ab, ob ein Trim
  auf der Eingabe genügt oder ob auch die **Werte** eine Behandlung bräuchten.

### Was der achte Nachtrag (M50) neu offen lässt

**Eine Lücke schließt er:** Der zweite Punkt oben — „Was der schlimmste Präfix des Bestands durch das
gebaute Statement kostet" — ist beantwortet. **3,851 s über 30 Tage, Abbruch über ein Jahr.** Die
übrigen sechs bleiben unverändert offen.

**Neu offen sind vier:**

- **Ob es einen Präfix gibt, der über 30 Tage teurer ist als dieser.** Gemessen ist der schlimmste
  Präfix über den **Bestand** — nicht der schlimmste über *ein Fenster von 30 Tagen*. Das sind zwei
  verschiedene Größen: Der Bestandssieger verteilt seine 1,33 Millionen Zeilen über ein Jahr, ein
  anderer Präfix könnte sie in einem Monat bündeln. **Die Zahl, die den Deckel wirklich trüge, wäre
  die zweite** — und ihre Erhebung bräuchte eine Gruppierung über `LEFT(v,4)` **mit** Zeitfenster,
  die diese Runde nicht gelaufen ist.
- **Was die gebaute Höchstform kostet.** Acht Begriffe zu je bis zu sieben Fassungen sind
  **56 `LIKE`-Zweige**. Gemessen sind ein Begriff ohne Zusatzfassungen (M50) und vier verodere `LIKE`
  bei einem Begriff (M49‑3, 1,205 ms). Die Kombination ist nicht gelaufen. **Das Geländer bei acht
  Begriffen ist damit im Präfixmodus noch weniger belegt als im exakten** (dort endet die Messung bei
  fünf, M47).
- **Ob der gekippte Plan stabil ist.** M50 zeigt den Umschlag bei 1,33 Millionen Kandidatenzeilen,
  M49‑3 den alten Plan bei 443.830. **Wo genau der Optimierer umschaltet, ist nicht gemessen** — und
  damit auch nicht, ob es dazwischen eine Zone gibt, in der er die *schlechtere* der beiden Wahlen
  trifft.
- **Wie viele der 55.989 Werte mit Komma jemals gesucht würden.** Dieselbe Gestalt wie bei den 585
  Doppelpunkten (M49‑4): Gemessen ist die **Existenz**, nicht ein Schaden. Anders als dort ist die
  Lage aber **nicht** entschärft — die Frage, ob die Parameterform geändert wird, steht unten.

### Was der neunte Nachtrag (M51) neu offen lässt

**Eine halbe Lücke schließt er.** Der vierte Punkt oben — „Wie viele der 55.989 Werte mit Komma
jemals gesucht würden" — ist **nicht** beantwortet und wird es in diesem Projekt auch nicht: Es
erhebt Suchanfragen bewusst nicht. Beantwortet ist die andere Hälfte, die M50 gar nicht gestellt
hatte: **wo** diese Werte sitzen. Drei Typen von 62, davon einer mit 96,62 %, alle drei bei einem
einzigen Mandanten. **Und die Frage, ob die Parameterform geändert wird, ist entschieden** — sie
ist geändert (Frage 22 unten).

**Neu offen sind drei:**

- **Ob weitere Zeichen dieselbe Falle stellen.** Untersucht sind der Doppelpunkt (M49‑4, durch die
  Teilung am ersten Vorkommen entschärft) und das Komma (M50‑5, M51, behoben). **Nicht untersucht
  ist, was ein Client oder ein Zwischenstück sonst noch zerlegt** — `;`, `|`, ein rohes `+` in einer
  URL. Die Frage ist nicht theoretisch: Der Kommadefekt ist nicht durch Nachdenken gefunden worden,
  sondern durch einen Test, der zufällig über ihn stolperte.
- **Ob `MessageBAM` weitere Typen mit Kommas bekommen kann.** Gemessen sind drei von 62 — heute.
  Welche Typen ein Mandant befüllt und womit, entscheidet die Anbindung und nicht dieses Werkzeug;
  **die Zahl ist eine Momentaufnahme und keine Eigenschaft des Schemas.** Dasselbe gilt für die
  0 von 1.503 `ProcessID` in M51‑4.
- **Ob die Nachrichtenliste dieselbe Behandlung bekommt.** `status` und `prozess` trennen weiterhin
  am Komma; heute ist das folgenlos (M51‑4). **Ob die Bindung dort angeglichen wird, bevor es das
  nicht mehr ist, ist nicht entschieden** — der Auftrag hat es ausdrücklich ausgeschlossen (ein
  Auftrag, ein Eingriff), und der Zustand ist in `BamSucheDbIT` festgeschrieben.

---

# Offene Fragen für das Sparring zu Schritt 7

1. **Wird die Zeilenzahl von `MessageBAM` in den verbindlichen Dateien korrigiert?**
   [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 und [`datenmodell.md`](datenmodell.md) §3
   und §8 führen **10.859.666** als „gemessen am 27.07.2026". Gezählt sind **15.406.350** (+41,9 %).
   Die Zahl stammt aus `information_schema` und war nie gezählt — `MessageBAM` war die Tabelle, die
   Regel L8 als „nicht erhoben" führte.

   > ✔ **Entschieden und erledigt am 12.08.2026: ja.** Die Frage bleibt hier stehen, weil die Zahlen,
   > auf denen die Entscheidung ruht, es ebenfalls tun. Bei der Umsetzung hat sich die Frage
   > erweitert: Nicht eine Zeile des Mengengerüsts war falsch, sondern seine **Überschrift** — von
   > sieben Zeilen war eine gezählt. `MessageProperty` ist daraufhin in **M44** nachgezählt worden
   > (75.571.462 statt 46.964.279). Korrigiert sind `PROJEKTBESCHREIBUNG.md` §3.2, §8, A7 und A8,
   > `datenmodell.md` §3 und §8 sowie die Regelliste L8 in `DEVELOPMENT_GUIDELINES.md` §4.4 — je
   > datiert, mit der alten Zahl im Kasten daneben und mit einer Spalte `Herkunft` in beiden
   > Mengengerüst-Tabellen. Festgehalten in
   > [`annahmen-korrekturen.md`](annahmen-korrekturen.md), Abschnitt „Erhebung 11./12.08.2026".

2. **Ist ein Standard-Zeitfenster von 24 Stunden an einer Belegsuche zumutbar?** Es ist die einzige
   Fassung mit echtem Abstand zur Zeitgrenze (90,5 ms gegen 8,66 s bei einem Jahr) — und es liefert
   beim schlimmsten Wert **279 von 234.159** Treffern. Der Nutzer, der eine Nummer sucht, hat kein
   Datum.

3. **Sucht Schritt 7 exakt oder präfixweise?** Beide kosten bei gleicher Trefferzahl dasselbe
   (M34). Exakt allein findet die sechs Typen mit durchgängig führender Null nur, wenn der Nutzer
   die Null mittippt (M38). Präfix braucht eine Mindestlänge, und die muss **je Typ** gelten
   (E6, M38).

   > ✔ **Entschieden im Sparring vom 12.08.2026: exakt**, mit einer typgebundenen und sichtbar
   > gemachten Normalisierung. Die Frage bleibt hier stehen, weil die Zahlen, auf denen die
   > Entscheidung ruht, es ebenfalls tun. Die Normalisierung ist ab hier **M43**.
   >
   > **Nachgemessen am 13.08.2026 in M49 — die Entscheidung wird davon nicht berührt, ihre
   > Begründung schon.** Die Frage nach der **Nachrüstung** steht als offener Punkt 1 in
   > `bam-suche.md` §9, und M49 legt die Zahlen darunter. Drei davon verschieben, was oben steht:
   > Erstens ist die getroffene Normalisierung **kein neutraler Nachbar** einer späteren Präfixsuche,
   > sondern ihr Gegenspieler — beide ankern vorn, und die rohe Präfixfassung findet den aufgefüllten
   > Wert in **allen acht** geprüften Fällen nicht (M49‑1). Zweitens ist „Präfix braucht eine
   > Mindestlänge" zwar richtig, aber **keine Lösung**: Es gibt keine Länge, ab der die schlimmste
   > Trefferzahl beherrschbar wird (M49‑2). Und drittens gilt „beide kosten bei gleicher Trefferzahl
   > dasselbe" zwar weiter — die Bedingung ist nur **nie erfüllt**, nicht einmal beim vollständig
   > eingetippten Wert (M49‑3).

4. **Wird normalisiert — und wenn ja, wo?** Führende Nullen bei sechs Typen auf 100 % der Werte,
   Randleerzeichen bei 9018 auf 25,88 %. Eine stille Korrektur verbietet sich; eine sichtbare
   („gesucht wurde auch nach …") ist Oberfläche und keine Datenbankfrage.

   > ✔ **Entschieden im Sparring vom 12.08.2026: ja, typgebunden und sichtbar.** **M43** liefert
   > das Maß dazu und schränkt es zugleich ein: Die Sollänge existiert bei den sechs Typen und
   > **nicht** bei den tragenden Suchtypen (M43‑1); die Randleerzeichen bei 9018 sind zu 24,83 %
   > folgende und damit beim `=`-Vergleich folgenlos, zu 1,05 % führende und damit nicht (M43‑3);
   > und der Bestand selbst trägt bei 9017 auf 52,98 % der Werte **beide** Schreibweisen, was keine
   > Normalisierung heilt (M43‑2).

5. **Folgen Suche und Detail-Block der Konfiguration in `MessageBAMMandant`?** Sie ist eine
   **Sichtbarkeits**entscheidung und keine Aussage über den Bestand (M40): `WOC` trägt 2.067
   BAM-Zeilen unter einem Typ, den seine Konfiguration nicht kennt. Folgt der Block ihr, sieht `WOC`
   nichts.

6. **Womit wird die Deckelung je Typgruppe bemessen?** Die Zahl fehlt (Belegvermerk zu M41). Ohne
   sie ist „je Gruppe deckeln" eine Form ohne Grenze.

7. **Wird die Kuratierung in `V4__bam_spalte.sql` rollenabhängig?** Die Frage steht seit M28‑2 offen
   und ist durch M39 schärfer geworden: Bei `SUTTONS` und `ZAST` sitzt die Belegnummer zu **100 %**
   auf den **Kindern**, bei `NEXANS` und `IBIS` auf den **Wurzeln**, bei `WOC` auf Zeilen **ohne
   Kette**. Eine Kuratierung ohne Rolle trifft mindestens zwei Mandanten mit null Prozent.

8. **Braucht die Suche einen Abbruchpfad wie die Nachrichtenliste (`suche-abgebrochen`)?** Mit
   Jahresfenster bleiben 13 % Reserve zur Zeitgrenze (M35). Auf einer belasteten
   Produktionsdatenbank ist das keine. **M42 fügt einen zweiten Fall hinzu:** K3b liegt ohne
   Fenster bei 5,275 s und damit bei Faktor 1,9 Reserve — und anders als der schlimmste Einzelwert
   entsteht er aus einer Eingabe, die der Nutzer für eine *Einschränkung* hält.

   > ✔ **Entschieden und gebaut am 13.08.2026: ja.** Die Frage bleibt hier stehen, weil die Zahlen
   > darunter es ebenfalls tun — und **M47 hat eine davon verschoben**: K3b liegt mit der gebauten
   > Vorgabe von 30 Tagen bei **882 ms** und ist damit kein Grund mehr. Der Grund, der bleibt, ist
   > der **Jahresfall**: 8,940 s am gebauten Statement, **89 Prozent** der Zeitgrenze, auf einer
   > *ruhenden* Testkopie. Der Endpunkt benutzt denselben Problemtyp `suche-abgebrochen` wie die
   > Nachrichtenliste und baut keinen zweiten ([`bam-suche.md`](bam-suche.md) §6).

### Neu aus dem Nachtrag vom 12.08.2026

9. **Welche Bauform bekommt die Verundung?** Die beiden gemessenen sind **gegenläufig**, nicht
   gestuft (M42‑3): Der Selbstjoin gewinnt, wo ein Begriff selten ist (0,947 gegen 903,4 ms bei
   fünf Begriffen), die Bauform mit `IN` + `HAVING COUNT(DISTINCT …)` gewinnt auf fetten
   Nachrichten (10,654 gegen 926,7 ms). Eine feste Wahl trifft in jedem Fall auch den Fall, in dem
   sie die schlechtere ist. Eine Wahl je Lage bräuchte eine Größe, die der Endpunkt vorab nicht
   kennt — dieselbe Sackgasse, die bei der Reihenfolge (Frage aus M42) gerade **nicht** eingetreten
   ist, weil der Optimierer sie dort selbst löst.

10. **Braucht die Verundung eine eigene Grenze?** Nicht wegen der Zahl der Begriffe — die kostet
    0,094 ms je Stück. Sondern wegen zweier Fälle: zwei sehr häufige Begriffe (K3b, 5,275 s) und
    zwei mittlere Begriffe auf fetten Nachrichten (K5c, 926,7 ms). Ob das Pflicht-Zeitfenster beide
    auffängt, ist **ungemessen**.

    > ✔ **Gemessen und beantwortet am 13.08.2026 in M47: nein, es braucht keine.** Das
    > Pflicht-Zeitfenster fängt **beide** — K3b fällt auf **881,975 ms**, K5c auf **6,911 ms**. Bei
    > K5c sagt der `EXPLAIN`, warum: Mit Fenster kippt der zweite Begriff nicht mehr auf den
    > `PRIMARY`-Zugriff mit `rows` 8, sondern bleibt bei `MessageBAM_BAMValueOnly` mit `key_len` 428
    > und `rows` 1. Gebaut ist deshalb **eine** Grenze — das Zeitfenster —, und das Schutzgeländer
    > von acht Begriffen begrenzt die Join-Reihenfolgen und keine Laufzeit
    > ([`bam-suche.md`](bam-suche.md) §1).

11. **Woher kommt die Sollänge je Typ, und wie wird sie fortgeschrieben?** M43‑1 belegt sie für
    sechs Typen über den Bestand — und widerlegt sie im selben Atemzug für einen davon (2001:
    100 % über einen Monat, 67,21 % über den Bestand). Eine Kuratierung in `bam_spalte` oder einer
    Nachbartabelle ist eine Momentaufnahme. **Ob es eine wiederkehrende Prüfung gegen den Bestand
    geben muss**, damit die Suche nicht still weniger findet, ist eine Entscheidung und steht hier
    als Frage (Belegvermerk zu M43).

    > ✔ **Beantwortet und gebaut am 13.08.2026.** *Woher:* aus **M46**, über den Bestand und je
    > Mandant — und die Frage nach dem „je Typ" ist dabei mit **nein** beantwortet worden, die
    > Kuratierung ist nach `(mandant_id, bam_typ)` geschlüsselt. *Wo:* in einer **eigenen** Tabelle
    > `overlord_monitor.bam_sollaenge` (`V5__bam_sollaenge.sql`) und nicht in `bam_spalte` — die
    > beantwortet eine andere Frage (welche zwei Spalten die Liste zeigt) und hat einen anderen
    > Schlüssel. *Wie fortgeschrieben:* über `BamSollaengeDriftDbIT`, der je kuratiertem Eintrag
    > prüft, ob die dominante Länge über den Bestand noch bei mindestens 95 % liegt. **Die
    > Garantiestufe ist begrenzt und das ist ausgewiesen:** Der Test läuft nicht in der CI, weil sie
    > das interne Netz nicht erreicht — dieselbe Stufe wie beim Statustest. Vollständig in
    > [`bam-sollaengen.md`](bam-sollaengen.md).

12. **Wird das führende Leerzeichen genauso behandelt wie die führende Null?** Bei 9018 tragen
    1,05 % der Zeilen eines — rund 1.500 — und die exakte Suche erreicht sie nicht (M43‑3:
    23 gegen 3 Treffer). Es ist derselbe Sachverhalt wie bei der Null, nur unsichtbarer: Ein
    Leerzeichen sieht der Nutzer weder in seiner Eingabe noch in der Anzeige.

13. **Was zeigt die Suche, wenn der Bestand *selbst* beide Schreibweisen trägt?** Bei 9017 sind
    52,98 % der numerischen Werte betroffen, mit bis zu **fünf** Varianten derselben Nummer
    (M43‑2). Keine Normalisierung heilt das — sie wählt nur, welche der fünf Varianten gefunden
    wird. Der Nutzer muss erfahren, dass es die anderen gibt, und das ist eine Frage der
    Antwortform und nicht der Datenbank.

### Neu aus dem vierten Nachtrag vom 13.08.2026

14. **Bekommt 9006 eine benannte Ausnahme?** Die 95-Prozent-Regel schließt die kuratierte
    Lieferschein-Nr. mit **94,21 %** aus — 0,79 Prozentpunkte unter der Schwelle. Ausgerechnet für
    sie belegt M43‑4 die Wirkung des Auffüllens an echten Werten (roh **1.642** Treffer, aufgefüllt
    **4**), und sie trägt auf **32,99 %** ihrer Werte eine führende Null. Die Schwelle zu senken
    hilft nicht: Bei 94 % kämen 9028, 9029 und 9004 mit, und die tragen 0,21 %, 0,21 % und 5,71 %.
    Entweder 9006 wird namentlich aufgenommen — dann ist die Regel keine Regel mehr, sondern eine
    Regel plus Liste — oder die Suche findet dort ohne Auffüllen 1.642 statt 4 Treffer.

15. **Wird die Regel um die Wirksamkeitsbedingung ergänzt?** `IBIS`/1 und `SUTTONS`/2000 stehen
    mechanisch in der Kuratierung und können nachweislich nichts finden (M46‑1c). Eine Ergänzung
    „bei der Sollänge kommt mindestens ein Wert mit führender Null vor" nähme beide heraus. Was die
    beiden Einträge kosten, ist ungemessen — es ist je eine zusätzliche Variante in der `IN`-Liste.

    > **Der Preis ist seit M47 beziffert und beantwortet die Frage nicht:** rund **0,1 ms** je
    > zusätzlicher Variante. Damit ist die Frage keine Leistungsfrage mehr, sondern nur noch die,
    > ob die Kuratierungsregel sagen soll, was sie meint. Sie bleibt offen.

16. **Wie erfährt die Kuratierung von einem neuen Mandanten oder Typ?** Sie kennt die 45 Paare vom
    13.08.2026. `BamSollaengeDriftDbIT` prüft, was dasteht — **nicht, was fehlt**. Ein Mandant, der
    morgen einen Typ mit durchgängig führender Null bekommt, wird ohne Sollänge gesucht, und niemand
    bemerkt es.

---

### Neu aus dem siebten Nachtrag vom 13.08.2026 (M49)

17. **Kommt die Präfixsuche — und wenn ja, immer oder nur im Nulltreffer-Fall?** Die Kosten sind
    beziffert (M49‑1 bis M49‑3), **der Nutzen ist es nicht**: Wie oft die exakte Suche heute leer
    ausgeht, steht in keinem Bestand und stünde nur in einem Suchprotokoll, das die Anwendung nicht
    führt. **Diese Runde entscheidet die Frage ausdrücklich nicht.** Was sie liefert, ist der Rahmen:
    Ohne die Nullen im Muster findet Präfix bei kuratierten Typen **nichts** (M49‑1); eine
    Mindestlänge schützt **nicht** (M49‑2); und die Antwort ändert sich auch bei vollständig
    eingetippter Nummer (M49‑3, 23 statt 1).

18. **Wenn Präfix kommt: Wie wird sie in der Parameterform angefordert?** M49‑4 sagt, welche Zeichen
    frei sind: **`%` kommt im ganzen Bestand nicht vor**, `*` steht in 1.738 und `_` in 2.696 Werten.
    Ein angehängtes `*` bräuchte eine Regel für die Werte, die selbst eines tragen. **Unabhängig
    davon würde jede Eingabe ein `ESCAPE` brauchen** — dieselbe Falle wie Regel Q1.

19. **Wenn Präfix kommt: Wird die Eingabe getrimmt?** Sie müsste. **602.794 Werte (3,91 %) tragen ein
    folgendes Leerzeichen** (M49‑4). Heute fängt PAD SPACE beide Richtungen ab; mit `LIKE` findet
    eine Eingabe *mit* folgendem Leerzeichen den Wert ohne nicht mehr. Ob der Trim in der
    Parameterprüfung oder in der Variantenbildung sitzt, ist eine Bauentscheidung und keine Messung.

20. **Kann eine Mindestlänge gelten, solange die Suche typlos voreingestellt ist?** M49‑2b misst die
    Grenze je Typ zwischen **59** und **234.159** — sie müsste also je Typ gelten, und je Typ gilt
    sie nur, wenn der Nutzer einen Typ gewählt hat. Die Vorgabe der Oberfläche ist **kein Typ**
    (M36). **Diese Spannung ist in M49‑2 benannt und ausdrücklich nicht aufgelöst.**

21. **Wird die Nachtrags- und Befundzählung dieser Datei nachgezogen?** Zwei Buchführungslücken sind
    beim Schreiben des siebten Nachtrags aufgefallen und **nicht** eigenmächtig gefüllt worden:
    Der sechste Nachtrag (**M48**) hat in *Alle Laufzeiten* keinen eigenen Abschnitt, und sein
    Befund zum nicht eindeutigen `MessageBAMTypeSortIndex` fehlt in *Wo die vorformulierte Zeile
    nicht passte*, obwohl er dort in M48 selbst als solcher ausgewiesen ist. Beides ist eine
    Feststellung über die **Buchführung**, nicht über eine Messung — keine Zahl ist davon berührt.

    > **Eine dritte kommt aus dem achten Nachtrag hinzu, und sie ist derselben Art:** Die Überschrift
    > *„Was der **sechste** Nachtrag (M49) neu offen lässt"* trägt die falsche Ordnungszahl — M49 ist
    > der siebte, wie M49‑0 selbst feststellt. Auch das bleibt stehen und wird hier benannt.

---

### Neu aus dem achten Nachtrag vom 14.08.2026 (M50)

22. **Wird die Parameterform um das Komma erweitert?** **55.989 BAM-Werte (0,363 %) tragen eines**
    (M50‑5), und Spring zerlegt den wiederholbaren `begriff`-Parameter daran: Diese Werte sind heute
    **unsuchbar** — in beiden Modi und seit Teil 2b. Der Doppelpunkt ist durch die Teilung am
    *ersten* Vorkommen behandelt (M49‑4), das Komma ist es nicht. **Teil 4 behebt es ausdrücklich
    nicht**, weil die Änderung den exakten Pfad träfe; der heutige Stand ist in
    `BamSucheDbIT.ein_komma_im_wert_ist_heute_400` festgeschrieben. **Die Entscheidung liegt beim
    Auftraggeber** — und mit ihr die Frage, ob dieselbe Prüfung für weitere Zeichen fällig ist, die
    Spring oder ein Client zerlegt.

    > **Beantwortet am 14.08.2026: ja, und zwar sofort.** Der Auftraggeber hat die Erweiterung noch
    > am selben Tag beauftragt — **vor** dem Frontend-Teil der Präfixsuche, damit dessen
    > Sichtprüfung nicht zufällig darüber stolpert. Die Bindung von `begriff` trennt nicht mehr am
    > Komma; der Test heißt jetzt `BamSucheDbIT.ein_komma_im_wert_wird_gefunden` und behauptet das
    > Gegenteil von vorher. **Die Frage nach den weiteren Zeichen bleibt offen** und steht als
    > Punkt 25 unten. Der Vorgang selbst — ein als fertig gemeldeter Pfad, der 0,363 % der Werte
    > nicht fand — steht in [`annahmen-korrekturen.md`](annahmen-korrekturen.md).

23. **Reicht der 30‑Tage-Deckel, oder braucht der Präfixmodus ein noch engeres Fenster?** M50 misst
    den schlimmsten Präfix des **Bestands** mit 3,851 s über 30 Tage — 38,5 % der Zeitgrenze auf einer
    **ruhenden** Testkopie. **Nicht gemessen ist der schlimmste Präfix über ein 30‑Tage-Fenster**, und
    das ist die Zahl, die den Deckel wirklich trüge. Ob sie erhoben wird, bevor die Oberfläche den
    Modus anbietet, ist eine Entscheidung des Auftraggebers.

24. **Wird Regel L5 in `PROJEKTBESCHREIBUNG.md` vermerkt?** Die Mindestlänge des Suchbegriffs wird
    mit Teil 4 zum **zweiten Mal** begründet abgewichen — im exakten Modus wegen M38 und E6, im
    Präfixmodus zusätzlich wegen M49‑2a (die schlimmste Trefferzahl hat einen Boden, den keine Länge
    unterschreitet). **Der Auftrag lässt `PROJEKTBESCHREIBUNG.md` ausdrücklich unangetastet** und
    stellt die Frage hierher: Ob eine zweimal begründet abgewichene Regel in der verbindlichen Datei
    einen Vermerk bekommt, ist nicht entschieden.

---

### Neu aus dem neunten Nachtrag vom 14.08.2026 (M51)

25. **Welche Zeichen zerlegt sonst noch jemand?** Der Kommadefekt ist **nicht durch Nachdenken
    gefunden worden**, sondern weil die Herleitung eines Isolationstests zufällig über ihn
    stolperte — und er lebte seit Teil 2b. Untersucht sind seither zwei Zeichen: der Doppelpunkt
    (M49‑4, durch die Teilung am *ersten* Vorkommen entschärft) und das Komma (M50‑5, M51, behoben).
    **Ungeprüft ist, was auf dem Weg zwischen Tastatur und Statement sonst noch zerlegt oder
    umgedeutet wird** — `;`, `|`, ein rohes `+` in einer URL, ein Zeilenumbruch aus der
    Zwischenablage. Ob eine solche Prüfung als eigener Auftrag läuft, ist eine Entscheidung des
    Auftraggebers; die Zählung dazu wäre billig (ein Statement in der Gestalt von M49‑4), die
    Aufzählung der Zerlegungsstellen nicht.

26. **Bekommt die Nachrichtenliste dieselbe Bindung?** `status` und `prozess` trennen weiterhin am
    Komma. **Heute ist das folgenlos und gemessen** (M51‑4: Einordnungen sind Aufzählungsnamen,
    0 von 1.503 `ProcessID` tragen ein Komma) — und deshalb hat der Auftrag es ausdrücklich
    ausgeschlossen: ein Auftrag, ein Eingriff. Der Zustand ist in
    `BamSucheDbIT.die_nachrichtenliste_trennt_weiterhin_am_komma` festgeschrieben, damit eine
    spätere Änderung dort auffällt. **Ob angeglichen wird, bevor der erste Prozessname mit Komma
    entsteht, ist nicht entschieden.**

27. **Wie kommt ein solcher Defekt künftig früher heraus?** Der Kommafall war in der CI **nicht**
    zu sehen: Die Tests gegen die Testkopie tragen `@Tag("db")` und sind dort ausgeschlossen, und
    die reinen Einheitstests kannten die Bindung nicht — sie beginnen erst bei `BamSuchfilter.aus`.
    Mit `BamSucheKommabindungTest` gibt es jetzt für **diesen** Endpunkt eine Prüfung der Bindung
    ohne Datenbank. **Ob dieselbe Sorte Prüfung für die übrigen Endpunkte angelegt wird, ist eine
    Entscheidung des Auftraggebers** — sie kostet je Endpunkt wenige Zeilen und fängt genau die
    Klasse von Fehler, die zwischen HTTP und Fachlogik verschwindet.
