# Rollup — `message_rollup`, `message_rollup_tag`, `message_rollup_monat`
und der Job, der sie fortschreibt

Schritt 10a, gebaut am 26.08.2026. Baut auf [`messungen-schritt10.md`](messungen-schritt10.md)
(M86–M92) und der Sparringsrunde vom 24.08.2026.

## 1. Zweck, in zwei Sätzen

**Leistungsregel L2 verbietet Live-Aggregation über `Message` für Dashboard-Kennzahlen.**
`message_rollup` ist die Tabelle, aus der das Dashboard stattdessen liest; ein stündlicher
Delta-Lauf und ein nächtlicher Volllauf schreiben sie fort.

**Der Rollup existiert nicht, weil eine Live-Abfrage scheitern würde.** Bei 3,3 Millionen Zeilen und
2,9 GB wäre sie technisch möglich — das Altsystem macht sie tagesweise. Er existiert, damit das
Dashboard unter einer halben Sekunde lädt und die Produktionsdatenbank nichts davon merkt
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Begründung korrigiert am 27.07.2026).

> **Nachtrag 27.08.2026 (Schritt 10b‑1).** Seit diesem Tag sind es **zwei** materialisierte
> Ebenen: `message_rollup` je Stunde und `message_rollup_tag` je Kalendertag. Der Grund steht in §2
> und ist gemessen: Die Zwölf‑Monats‑Ansicht kostete über die Stundenebene 2,2 bis 2,5 Sekunden.
> **Beide Ebenen entstehen im selben Lauf und in derselben Transaktion**; die Tagesebene ist die
> Summe der Stundenebene und kann nichts anderes sein.
>
> **Nachtrag 31.08.2026 (Schritt 10b‑2). Seit diesem Tag sind es drei.** `message_rollup_monat`
> je Kalendermonat kommt dazu, weil das Tor aus §9a ausgelöst hat: Über die Tagesebene kostete die
> Zwölf‑Monats‑Ansicht beim größten Mandanten weiterhin **767,128 ms** und **908,539 ms** gegen
> eine Schwelle von 700 ms. **Über die Monatsebene sind es 65,350 ms und 88,672 ms** (M107, §9d).
> Die Kette setzt sich fort und wiederholt sich nicht: **Jede Ebene ist die Summe der
> nächstfeineren** — die Monatsebene entsteht aus der Tagesebene, nicht aus der Stundenebene und
> erst recht nicht aus `Message`.

### Was 10a ist und was nicht

| | |
|---|---|
| **Gebaut** | die Tabellen `message_rollup` und `rollup_lauf`, das Leserepository, der Job, die Auslösung, die Tests |
| **Nicht gebaut** | kein Endpunkt, keine Oberfläche, keine Kachel, kein Diagramm, kein neuer Parameter an `GET /api/nachrichten`. Das ist 10b |

---

## 2. Die vier Tabellen (`V9__message_rollup.sql`, `V10__message_rollup_tag.sql`,
`V12__message_rollup_monat.sql`)

### `message_rollup`

```sql
CREATE TABLE message_rollup (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

**Die Definition ist übernommen und nicht entworfen.** M89 hat genau diese Spalten, denselben
Schlüssel und dieselbe Sortierung als Probetabelle `message_rollup_probe` angelegt und über den
Gesamtbestand gefüllt; die Zahlen unten in §9 halten den gebauten Lauf gegen sie.

**Der Schlüssel ist `(stunde, process_id, message_status)` — Entscheidung E‑a.** Mandant, Partner
und Richtung stehen **nicht** in der Zeile. Der Mandant kommt in 10b aus dem Join über
`Process → ProjectMandant`, Partner und Richtung aus `process_catalog`.

> **Widerspruch zu [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §5, und er ist bekannt.**
> Dort steht „stündliche Aggregate je **Mandant, Prozess, Partner, Richtung, Status**". Das ist die
> ältere Fassung; E‑a hebt sie auf, und M89 stützt E‑a: 11,30 ms für das Standardfenster mit
> Katalog-Join, 237,67 ms für ein Monatsfenster — bei einem Budget von 500 ms für die ganze
> Landingpage. **Der Nachtrag in §5 ist nicht Sache dieses Schritts**; er steht als offener Punkt
> 51 unten.

**In `message_status` steht der Rohwert, nicht die Einordnung — Entscheidung E‑g.** Der
`MessageStatusClassifier` läuft beim **Lesen**, also in 10b. Grund: Die Einordnung ist eine Regel,
die sich ändern kann; der Rohwert ist eine Tatsache. Wäre die Kategorie materialisiert, müsste jede
Regeländerung die ganze Tabelle neu rechnen. **Der Preis dafür ist gemessen und beträgt eine
Zeile:** 335.610 statt 335.609 (M87, Variante 1 gegen Variante 2).

~~**Kein Sekundärindex.** Die Probetabelle aus M89 hatte 0 Byte Indexanteil, und beide dort gemessenen
Pläne steigen über den Primärschlüssel ein. Die gebaute Tabelle bestätigt das: **0 Byte** auch nach
dem Volllauf (§9). Ein Index für die Prozesssicht (10c) ist nicht gemessen und wird nicht auf
Verdacht gebaut.~~

> ### ⚠️ Korrigiert am 30.08.2026 — die Tabelle trägt jetzt einen Sekundärindex
>
> `V11__message_rollup_prozess_index.sql`:
>
> ```sql
> CREATE INDEX message_rollup_prozess_idx ON message_rollup (process_id, stunde);
> ```
>
> **Beide Hälften des durchgestrichenen Satzes waren richtig**, und die zweite ist der Grund für die
> Migration: *„nicht gemessen und wird nicht auf Verdacht gebaut."* Er ist jetzt gemessen, zweimal —
> und erst danach gebaut.
>
> | | |
> |---|---|
> | **Was er dem Leser bringt** (M104, [`messungen-liste-verengung.md`](messungen-liste-verengung.md)) | Die Vorabfrage der Nachrichtenliste über dreißig Tage fällt für `NXHBE` von 97,756 auf **0,962 ms** (Faktor 101,6), für `SYSTEM` um 98,5, für `EDITIONLINGERI` um 94,5, für `WOC` um 59,2, für `ZAST` um 38,3. Auf engen Fenstern kostet er nichts (0,87× bis 1,05×) |
> | **Was er den Läufen kostet** (M105, §9c) | Der **stündliche** Delta-Lauf zahlt nichts: 9,539 → 8,997 ms bei vierzehn Rollupzeilen. Der **nächtliche** Volllauf zahlt: Schreibpfad einer Monatsscheibe +32,1 %, über alle 23 Scheiben +52,8 %. Größte Scheibe samt Aggregation **3,669 s** gegen die vor der Messung gesetzte Schranke von 5 s |
> | **Was er kostet** | **16,6 MiB** neben 21,6 MiB Daten, Aufbau 1,084 s |
> | **Warum `(process_id, stunde)` und nicht umgekehrt** | Die umgekehrte Reihenfolge steht schon im Primärschlüssel und beantwortet die Frage nicht: Sie führt über die Zeit und muss danach jede Zeile auf ihren Prozess prüfen. Gebraucht wird der Einstieg über den Prozess mit anschließendem Zeitbereich |
>
> **Der eine Verdacht, der ausdrücklich geprüft worden ist:** Die Tagesebene wird aus dieser Tabelle
> abgeleitet (`rechneTageEbeneNeu` liest `message_rollup`). Ein neuer Index hätte ihren Plan kippen
> können. Er tut es nicht — `range` über `PRIMARY`, `key_len` 5, mit Index wie ohne, +0,6 % Laufzeit.
>
> **Nur die Stundenebene.** `message_rollup_tag` bekommt keinen Index: Die Verengung liest die
> Tagesebene nicht, und ein Index auf Verdacht ist genau das, was der durchgestrichene Satz zu Recht
> abgelehnt hat.
>
> Der Satz bleibt lesbar, weil er die Lage bis zum 30.08.2026 richtig beschreibt und weil die
> Begründung, mit der er fiel, ohne ihn nicht zu verstehen wäre.

**`utf8mb4_general_ci` ist Absicht, nicht Nachlässigkeit.** `message_status` muss dieselbe Sortierung
tragen wie `GlassfishDB.Message.MessageStatus`, sonst gruppieren Quelle und Rollup verschieden;
`process_id` dieselbe wie `Process.ProcessID` und `process_catalog.process_id`, sonst bricht der
Join in 10b. Das ist **keine** tokenartige Spalte im Sinne der `_bin`-Regel aus §5 — hier steht kein
Sitzungsschlüssel, sondern ein lesbarer Prozessname und ein Statuswort.

**Zeitzone:** `stunde` ist **Wanduhrzeit des Quellservers** und nicht UTC. Zeitstempel aus
`GlassfishDB` werden nirgends konvertiert ([`datenzugriff.md`](datenzugriff.md) §7). Die beiden
UTC-Zeitstempel dieses Schritts stehen in `rollup_lauf` und heißen bewusst anders.

### `message_rollup_tag` *(neu am 27.08.2026, Schritt 10b‑1)*

```sql
CREATE TABLE message_rollup_tag (
  tag            DATE        NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (tag, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

**Dieselbe Bauform wie `message_rollup`, nur mit `tag` statt `stunde`** — derselbe Schlüssel,
derselbe Rohwert (E‑g), kein Sekundärindex, Zeichensatz und Sortierung explizit. Die Begründungen
oben gelten Wort für Wort; sie sind hier nicht wiederholt.

> **„Kein Sekundärindex" gilt für die Tagesebene weiterhin** *(Stand 30.08.2026)* — anders als für
> die Stundenebene, die seit `V11` einen trägt. Die Fensterverengung der Nachrichtenliste liest
> ausschließlich Stundeneimer; für die Tagesebene gibt es keine gemessene Frage, die der
> Primärschlüssel nicht bedient.

> ### ⚠️ Warum es sie überhaupt gibt: E‑b trägt bei zwölf Monaten nicht
>
> **M94 hat die Zwölf‑Monats‑Ansicht über die Stundenebene mit 2.206,854 ms (Verlauf) und
> 2.491,443 ms (Verteilung) gemessen** — das 5,5‑ bis 6,2‑Fache der vorregistrierten
> 400‑ms‑Schwelle und das 4,4‑ bis 5,0‑Fache des 500‑ms‑Budgets der ganzen Landingpage. Das ist
> offener Punkt 55.
>
> **Und es gibt nichts wegzuoptimieren.** M94 hat die Kosten je gelesener Rollupzeile über einen
> Mengenbereich von Faktor 302 gemessen: **5,3 bis 11,5 µs**, also sauber linear. Der Katalog‑Join,
> der naheliegende Verdächtige, trägt bei zwölf Monaten nur 5 bis 13 % (Befund 15). Die einzige
> Stellschraube ist die **Zahl der gelesenen Zeilen**.

**Zwei Zahlen, nachgerechnet statt übernommen** (27.08.2026, gegen die gefüllte Stundenebene):

| | Stundenebene | Tagesebene | Faktor |
|---|---:|---:|---:|
| Zeilen über den Gesamtbestand | 335.610 | **123.049** | 2,73 |
| `SUM(anzahl)` | 3.341.519 | **3.341.519** | — |
| Zeilen, die ein Zwölf‑Monats‑Fenster liest | 280.186 | **100.270** | 2,79 |
| Zeilen, die ein 30‑Tage‑Fenster liest | 20.971 | **6.843** | 3,06 |

M87 hatte 123.000 vorhergesagt; gemessen sind 123.049. **`SUM(anzahl)` ist in beiden Ebenen
dieselbe Zahl wie in `Message` selbst** — das ist die Summenprobe, und sie ist der einzige Grund,
dem Faktor 2,73 zu trauen.

> ### Eine eigene Tabelle und keine Ebenen-Spalte in `message_rollup`
>
> Lägen beide Ebenen in einer Tabelle, stünden ein Tageseimer und seine 24 Stundeneimer
> **nebeneinander**. Jede Summe über die Tabelle zählte damit doppelt — und zwar lautlos:
> `SUM(anzahl)` über den Gesamtbestand ergäbe **6.683.038 statt 3.341.519**, und niemand sähe der
> Zahl an, dass sie zwei Ebenen addiert. Der Primärschlüssel trüge außerdem zwei Bedeutungen: In
> der einen Zeile wäre die erste Spalte ein Stundenanfang, in der anderen ein Tagesanfang, und der
> Unterschied stünde in einer Nachbarspalte.
>
> Zwei Tabellen kosten dafür eine zusätzliche `DELETE`/`INSERT`‑Runde je Lauf und **39,6 % des
> Platzes der Stundenebene** (8,53 gegen 21,53 MiB, gemessen — §9); der Volllauf wird dadurch um
> **12,7 Sekunden** länger. Beides ist billig; eine Summe, die still das Doppelte ergibt, ist es
> nicht.

**`DATE` und nicht `DATETIME`:** Ein Tageseimer hat keine Uhrzeit. Ein `DATETIME` mit `00:00:00`
sagte dasselbe und lüde dazu ein, ihn irgendwann mit einer Stundengrenze zu verwechseln.

**Zeitzone wie bei `stunde`:** Wanduhrzeit des Quellservers, nicht UTC. Ein Tageseimer ist der Tag,
wie ihn der Quellserver sieht.

### `message_rollup_monat` *(neu am 31.08.2026, Schritt 10b‑2)*

```sql
CREATE TABLE message_rollup_monat (
  monat          DATE        NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (monat, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

**Dieselbe Bauform, nur mit `monat`** — derselbe Schlüssel, derselbe Rohwert (E‑g), kein
Sekundärindex, Zeichensatz und Sortierung explizit. Jede Begründung von oben gilt Wort für Wort;
sie ist hier nicht ein drittes Mal wiederholt.

**`monat` ist der *erste Tag* des Monats als `DATE`.** Nicht der letzte, nicht ein `VARCHAR(7)` mit
`'2025-12'`. Der erste Tag ist selbst ein Datum und lässt sich mit `tag` und `stunde` vergleichen,
ohne umgerechnet zu werden; eine Zeichenkette müsste dafür jedes Mal geparst werden. Der Test
`RollupFensterTest.BetroffeneMonate.nur_monatserste_sind_grenzen` weist einen anderen Tag
ausdrücklich ab — **er wäre kein Formfehler, sondern eine zweite Bedeutung derselben Spalte**, und
die fände niemand.

> **„Kein Sekundärindex" gilt auch hier** *(Stand 31.08.2026)*. Der Index aus `V11` liegt
> ausschließlich auf der Stundenebene, weil nur sie von der Fensterverengung der Nachrichtenliste
> gelesen wird. Für die Monatsebene gibt es keine gemessene Frage, die der Primärschlüssel nicht
> bedient — der Plan aus §9d steigt über `PRIMARY` ein, `key_len 3`, und die Tabelle trägt nach dem
> Rückwärtslauf **0 Byte** Indexanteil.

**Zwei Zahlen, gemessen statt übernommen** (31.08.2026, gegen die gefüllte Tagesebene):

| | Stundenebene | Tagesebene | **Monatsebene** | Faktor gegen die Stundenebene |
|---|---:|---:|---:|---:|
| Zeilen über den Gesamtbestand | 335.610 | 123.049 | **11.957** | **28,07** |
| `SUM(anzahl)` | 3.341.519 | 3.341.519 | **3.341.519** | — |
| Zeilen, die ein Zwölf‑Monats‑Fenster liest | 280.186 | 100.270 | **9.649** | 29,04 |

M87 Variante 4 hat **11.957** vorhergesagt — und zwar nicht geschätzt, sondern an der Quelle
gezählt. Der Rückwärtslauf hat **11.957** geschrieben. **`SUM(anzahl)` ist in allen drei Ebenen
dieselbe Zahl wie in `Message` selbst** — das ist die Summenprobe, und sie ist der einzige Grund,
dem Faktor 28,07 zu trauen.

**Platz:** 1,52 MiB Daten, 0 Byte Index — gegen 8,53 MiB der Tagesebene und 21,53 MiB der
Stundenebene. Die dritte Ebene kostet **7,0 %** des Platzes der Stundenebene.

**Zeitzone wie bei `stunde` und `tag`:** Wanduhrzeit des Quellservers, nicht UTC.

> ### Eine dritte Tabelle und keine Ebenen-Spalte — der Grund ist derselbe und wiegt schwerer
>
> Lägen alle drei Ebenen in einer Tabelle, stünden ein Monatseimer, seine Tageseimer und deren
> Stundeneimer **nebeneinander**. `SUM(anzahl)` über den Gesamtbestand ergäbe dann **10.024.557
> statt 3.341.519** — das Dreifache, und niemand sähe der Zahl an, dass sie drei Ebenen addiert.

### `rollup_lauf`

```sql
CREATE TABLE rollup_lauf (
  id                 BIGINT      NOT NULL AUTO_INCREMENT,
  art                VARCHAR(10) NOT NULL,
  fenster_von        DATETIME    NOT NULL,
  fenster_bis        DATETIME    NOT NULL,
  gestartet_am       DATETIME(3) NOT NULL,
  beendet_am         DATETIME(3) NULL,
  zeilen_geschrieben INT         NULL,
  fehler             TEXT        NULL,
  PRIMARY KEY (id),
  KEY rollup_lauf_stand_idx (beendet_am, fenster_bis)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

Wasserstand und Protokoll in einer Tabelle. `art` ist `DELTA` oder `VOLL`.

> ### Warum der Wasserstand eine eigene Tabelle braucht und nicht `MAX(stunde)` ist
>
> **Eine Stunde ohne Verkehr erzeugt keine Rollup-Zeile.** `MAX(stunde)` sagt darum „letzte Stunde
> mit Verkehr", nicht „bis hierhin ist gerechnet". Der Unterschied ist genau der zwischen *nichts
> passiert* und *noch nicht nachgesehen* — und den darf das Dashboard später nicht verwechseln.
>
> Auf der Testkopie ist das keine Feinheit: **Fünf zusammenhängende Monate** (2026‑01 bis 2026‑05)
> tragen null Zeilen (M92). Ein Wasserstand aus `MAX(stunde)` bliebe dort auf dem 30.12.2025 stehen
> und ließe den Job jede Nacht dieselben fünf Monate neu rechnen.

**Der Wasserstand ist `MAX(fenster_bis)` über Läufe, die abgeschlossen *und* fehlerfrei sind:**

```sql
SELECT MAX(fenster_bis) FROM rollup_lauf
WHERE beendet_am IS NOT NULL AND fehler IS NULL;
```

- `beendet_am IS NULL` heißt **abgebrochen oder noch laufend** — beides zählt nicht. Sonst
  übersprünge der nächste Lauf einen Bereich, der nur zur Hälfte geschrieben ist, und die Lücke
  fände niemand mehr.
- `fehler IS NOT NULL` heißt **abgeschlossen, aber nicht verlässlich gerechnet**. `beendet_am` wird
  im Fehlerfall trotzdem gesetzt, damit die Zeile nicht als „läuft gerade" den nächsten Lauf sperrt.

Ist die Tabelle leer, fällt der Delta-Lauf auf `MIN(Message.MessageLastUpdate)` zurück — der erste
Delta-Lauf holt damit den ganzen Bestand nach, ohne dass jemand einen Volllauf ansteuern müsste.

> **Das heißt: Der allererste Lauf nach der Migration ist ein Volllauf, und er trägt trotzdem
> `art = DELTA`.** Er kostet dann nicht die 20 bis 30 ms eines Delta-Laufs, sondern die rund 46 s
> eines Volllaufs (§9), und er hält dabei den ganzen Bestand im Speicher. **Das ist gewollt** — die
> Alternative wäre eine leere Rolluptabelle bis zum ersten Nachtlauf —, aber es ist beim ersten
> Start nach der Auslieferung zu wissen: Der stündliche Job braucht dort einmalig eine knappe
> Minute, und `rollup_lauf` weist ihn als `DELTA` aus. Wer die Zeile später liest, erkennt ihn an
> seinem Fenster und nicht an seiner Art.

---

## 3. Der Zeitschnitt

`rollup/RollupFenster` — ein reiner Wertetyp, ohne Datenbank prüfbar.

**Beide Grenzen liegen immer auf einem vollen Stundenanfang**, `von` einschließlich, `bis`
ausschließlich. Der Konstruktor weist alles andere ab; darauf ruht Löschen-und-Neuschreiben.

### Fensterberechnung des Delta-Laufs

1. Wasserstand `W` (siehe §2). Ist `rollup_lauf` leer, ist `W` = `MIN(Message.MessageLastUpdate)`.
2. Rohes Fenster: von `W − 15 Minuten` bis `jetzt` (**Anwendungsuhr**).
3. **Auf ganze Stunden ausdehnen:** Der Beginn wird auf den Anfang seiner Stunde abgerundet, das
   Ende auf den Anfang der *nächsten* Stunde aufgerundet.
4. Verarbeitet werden die vollständigen Stundeneimer in diesem Bereich.

Fenster des Volllaufs: von `MIN(Message.MessageLastUpdate)` bis `jetzt`, ebenso ausgedehnt. **Kein
Nachlauf** — er begänne ohnehin vor der ersten Zeile.

> ### Schritt 3 ist der Kern und keine Formalie
>
> Die 15 Minuten sind ein Rückgriff auf den **Zeilenzeitstempel**, aber die Schreibeinheit ist der
> **ganze Stundeneimer**. Würde nur der rohe Bereich gelesen und dazugezählt, zählte der Anteil
> zwischen `W − 15 min` und `W` **doppelt**. Deshalb: ganze Eimer, gelöscht und neu geschrieben.
>
> **Daraus folgt die Eigenschaft, auf der der ganze Ablauf ruht.** Weil `W` selbst immer ein
> Stundenanfang ist, fällt `W − 15 min` **stets in die vorige Stunde**. Der zuletzt geschriebene
> Eimer wird also bei jedem Delta-Lauf noch einmal vollständig gerechnet — und genau das macht es
> unbedenklich, dass der obere Rand eine **angebrochene** Stunde einschließt. Sie wird beim nächsten
> Lauf ersetzt, nicht ergänzt.

**Das Ende ist der Anfang der *nächsten* Stunde, auch genau auf der Stundengrenze.** Läge `jetzt`
auf `14:00:00`, wäre das Ende `15:00` und nicht `14:00`. Sonst hätte der Lauf genau auf der
Stundengrenze ein leeres Fenster, abhängig davon, ob der Auslöser eine Sekunde vor oder nach der
vollen Stunde feuert — ein Verhalten, das an der Sekunde des Weckers hängt, ist kein Verhalten.

**Ein leeres Fenster ist ein Zustand und kein Fehler.** Es entsteht, wenn der Wasserstand *mehr als
eine Stunde* vor der Anwendungsuhr liegt. Dass ein Lauf unmittelbar auf den vorigen folgt, genügt
dafür **nicht** — der Rückgriff zieht `von` dann in die Stunde vor `bis`. Wirklich leer wird es
erst, wenn die **Uhr zurückspringt**: im Profil `dev` der praktische Fall, wenn die Testkopie neu
befüllt wird und der ermittelte Anker früher liegt als zuvor. Der Lauf schreibt trotzdem seine
Protokollzeile.

### Das Nachlauffenster: 15 Minuten, gemessen und nicht gewählt

M86 hat über **220.579 Nachrichten** und alle elf Statuswerte geprüft, wie weit
`MessageLastUpdate` nachträglich wandert:

| | |
|---|---:|
| Anteil nachgeschriebener Zeilen | 0,62 % bis 2,17 % |
| **größte beobachtete Differenz** | **4 Sekunden** |
| Werte über 60 Sekunden | **0** |
| nachgeschriebene Zeilen in Fenster B | 1.548 |

Ein Nachlauffenster von **48 Stunden** — als Möglichkeit mitgeführt — wäre um den Faktor **43.200**
überdimensioniert. Ein Wasserstand **ohne** Nachlauffenster ist aber ebenfalls nicht gedeckt: Die
1.548 nachgeschriebenen Zeilen sind gemessen und nicht null.

> **Belegvermerk (Regel L10).**
> *Gemessen ist:* Auf dem Bestand der Testkopie wandert `MessageLastUpdate` um höchstens vier
> Sekunden.
> *Behauptet wird:* Fünfzehn Minuten genügen.
> **Die Lücke:** `RUNNING` kommt auf der Testkopie **null Mal** vor, und `MatchInterchange` — der
> Job, der `COMMIT_RECEIVED` nachträglich setzt — läuft dort **nicht sichtbar** (M31‑3, Takt
> ungedeckt). Der zulässige Satz lautet „auf diesem Bestand nicht beobachtet", nicht „findet nicht
> statt". Der Sicherheitsabstand von Faktor 225 gegenüber der gemessenen Obergrenze trägt dem
> Rechnung — und deshalb ist der nächtliche Volllauf keine Zierde, sondern die zweite Sicherung.

**Der Wert ist eine Konstante und kein Konfigurationsschlüssel** (`RollupFenster.NACHLAUF_MINUTEN`).
Es ist eine Entscheidung mit einer Messung dahinter, keine Betriebseinstellung; ein Drehknopf lüde
dazu ein, sie ohne neue Messung zu verstellen. Zeigt die Produktion eine längere Wanderung, ist das
ein Befund und eine Codeänderung wert — offener Punkt 49.

---

## 4. Die zwei Uhren

**Das ist die Stelle, an der bei diesem Schritt am ehesten etwas schiefgeht.** In derselben Zeile
von `rollup_lauf` stehen zwei Zeitpunkte aus **verschiedenen** Uhren. Die Zuordnung steht deshalb in
einer eigenen, ohne Datenbank prüfbaren Klasse (`rollup/RollupUhren`) und nicht verteilt im Job.

| Spalte | Uhr | Warum |
|---|---|---|
| `fenster_von`, `fenster_bis` | **Anwendungsuhr** (`Clock`, `@Primary`) | **Datenzeit.** Sie beschreiben, welchen Ausschnitt aus `GlassfishDB` der Lauf verarbeitet hat, und werden gegen `Message.MessageLastUpdate` gehalten. Im Profil `dev` liegt sie beim Anker der Testkopie — und genau so soll der Lauf dort auch rechnen |
| `gestartet_am`, `beendet_am` | **`systemClock`** (echte Uhr, UTC) | **Protokollzeit**, wie `geaendert_am` und das `audit_log` (Regel A5). Eine Protokollzeile mit zurückversetzter Uhr wäre im Betrieb unlesbar |

**Im Profil `dev` liegen die beiden Paare Monate auseinander** — am 26.08.2026 rund 239 Tage. **Das
ist der erwartete Anblick und kein Fehler.** Die gemessene Laufzeile Nr. 25 zeigt es:

| `fenster_von` | `fenster_bis` | `gestartet_am` | `beendet_am` |
|---|---|---|---|
| `2024-10-01 02:00:00` | `2025-12-30 05:00:00` | `2026-08-26 15:05:06.806` | `2026-08-26 15:05:52.800` |

**Regel Z1 ist eingehalten:** `LocalDateTime.now()` wird nicht aufgerufen. Beide Werte entstehen aus
`Clock.instant()` — dieselbe Form wie in `audit/AuditLogWriter`.

> ### Eine Folge, die vor der Abnahme zu kennen ist
>
> **Im Profil `dev` kann ein Volllauf den Gesamtbestand nicht abdecken.** Seine obere Grenze ist die
> Anwendungsuhr, und die steht am Anker `2025-12-30 04:09:47`; die 5.133 Zeilen des Jahres 2026
> liegen dahinter. Gemessen (§9): dev-Lauf **335.595 Zeilen / 3.336.386 Nachrichten**, Lauf gegen
> die Systemuhr **335.610 / 3.341.519**. Die Differenz ist Zeichen für Zeichen der Rest aus M92:
> **15 Rollupzeilen** (13 im Juni, 2 im Juli 2026) und **5.133 Nachrichten**.
>
> **In Produktion tritt das nicht auf**, weil die Anwendungsuhr dort die Systemuhr ist und damit
> hinter dem jüngsten Datensatz liegt. Wer die Abnahmezahlen lokal reproduzieren will, fährt den
> Lauf gegen die Systemuhr — siehe §8.

---

## 5. Der Lauf

`rollup/RollupJob` — **eine** Methode für beide Laufarten:

```java
RollupErgebnis fuehreAus(LocalDateTime von, LocalDateTime bis, LaufArt art)
```

Delta-Lauf und Volllauf unterscheiden sich **ausschließlich im Fenster** und laufen danach durch
denselben Code und dieselbe Abfrage. **Zwei getrennte Abfragen zu schreiben ist ausdrücklich
verboten** — sie liefen auseinander, und der Nachtlauf änderte dann still die Zahlen, die tagsüber
jemand gesehen hat. Die Laufart steht nur im Protokoll.

### Ablauf

1. **Zeile in `rollup_lauf` anlegen** (`gestartet_am`, `art`, Fenster), `beendet_am` bleibt `NULL`.
   **Vor** dem Rechnen: Stürzt die Anwendung mitten im Lauf ab, steht die Zeile trotzdem da — ohne
   `beendet_am`, damit sie den Wasserstand nicht hebt, und mit ihrem Fenster, damit ablesbar ist,
   woran der Lauf war.
2. **Aggregation über den Lese-Pool holen**, scheibenweise (§6). Das liegt **außerhalb** jeder
   Transaktion: Der Lesezugriff läuft auf einer anderen Verbindung und wäre ohnehin nicht Teil von
   ihr ([`datenzugriff.md`](datenzugriff.md) §2).
3. **Alle Rollup-Zeilen im Fenster löschen, dann die neuen einfügen — in genau einer Transaktion.**
4. **Die Tageseimer der berührten Kalendertage neu rechnen** — *neu am 27.08.2026*, **in derselben
   Transaktion** und **aus der eben geschriebenen Stundenebene**, nicht aus `Message`.
5. **Die Monatseimer der berührten Kalendermonate neu rechnen** — *neu am 31.08.2026*, ebenfalls in
   derselben Transaktion und **aus der eben geschriebenen Tagesebene**. **Die Reihenfolge ist der
   Punkt:** Jede Ebene entsteht aus der nächstfeineren, *nachdem* diese geschrieben ist. Stünde sie
   davor, trüge sie den Stand von vor diesem Lauf — und der Fehler wäre still.
6. **`beendet_am` und `zeilen_geschrieben` nachtragen.** Erst danach zählt das Fenster zum
   Wasserstand.
7. **Beim Volllauf: den Bestandsanfang prüfen** — *neu am 31.08.2026, Schritt 10b‑2*. Liegt
   `MIN(message_rollup.stunde)` **vor** `MIN(Message.MessageLastUpdate)`, stehen Eimer unterhalb des
   Bestandsanfangs; sie werden **gemeldet und nicht gelöscht** (offener Punkt 54, §13). Einzelheiten
   unten unter „Schritt 7".

Bei einer Ausnahme: `fehler` füllen, `beendet_am` setzen, Ausnahme weiterreichen. Schlägt *das*
fehl, wird es der ursprünglichen Ausnahme angehängt und nicht an ihre Stelle gesetzt.

> ### Löschen und neu schreiben — nie `anzahl = anzahl + n`
>
> Zwei Gründe, und beide sind Fehler, die still bleiben:
>
> 1. **Der 15‑Minuten‑Rückgriff überlappt mit dem letzten Lauf.** Hochzählen verdoppelte den
>    Überlapp — und zwar bei jedem Lauf aufs Neue, sodass der Fehler wächst statt aufzufallen.
> 2. **Wechselt eine Nachricht innerhalb desselben Eimers den Status**, verschwindet die alte
>    Statuszeile nicht von selbst. Ein `INSERT … ON DUPLICATE KEY UPDATE` schriebe die neue Zeile
>    und ließe die alte mit ihrem alten Zählstand stehen. Die Summe wäre zu hoch, die Verteilung
>    nach Status falsch — beides ohne jede Fehlermeldung.
>
> Beide Fälle haben einen eigenen Datenbanktest (§10).

**Schritt 3, 4 und 5 laufen in genau einer Transaktion.** Beim Volllauf umfasst sie 335.610
Stunden-, 123.049 Tages- und 11.957 Monatszeilen. Das ist gewollt: **Eine halb geleerte
Rolluptabelle um 03:00 wäre ein leeres Dashboard** — und Ebenen auf verschiedenen Ständen wären
schlimmer als das: Das Dashboard liest je nach Fensterbreite aus der einen oder der anderen, und
zwei Ansichten desselben Zeitraums zeigten dann verschiedene Zahlen, ohne dass irgendwo ein Fehler
protokolliert wäre. **Ein Datenbanktest hält das fest**
(`RollupDbIT.keine_ebene_bleibt_bei_einem_abbruch_zurueck`, §10): Er nimmt die Transaktion von außen
zurück und verlangt, dass danach *keine* der drei Ebenen eine Zeile behalten hat.

### Schritt 4: die Tagesebene, aus der Stundenebene

**Abgeleitet und nicht zweitgelesen.** Zwei Gründe. *Der erste ist Geld:* Eine zweite Quelllesung
kostete noch einmal, was die erste kostet — beim Volllauf 45,772 s über den Lese-Pool, den sich die
Anwendung mit der Produktion teilt. *Der zweite ist Wahrheit:* Zwei getrennte Lesungen derselben
Quelle können abweichen. Aus der Stundenebene abgeleitet ist die Tagesebene **per Konstruktion**
konsistent — sie ist deren Summe und kann gar nichts anderes sein.

**Über *ganze* Tage, nicht über das Fenster.** Ein Tageseimer ist die Summe seiner 24
Stundeneimer; aus einem Zwei‑Stunden‑Fenster gerechnet trüge er zwei Stunden und behauptete, ein
Tag zu sein. Der Rechenbereich läuft deshalb von Mitternacht bis Mitternacht, unabhängig davon, wie
schmal das Fenster des Laufs ist.

> **Der letzte berührte Tag ist der Tag der letzten *verarbeiteten* Stunde**, also `bis` minus einer
> Stunde — und nicht der Tag von `bis`. `bis` ist ausschließend: Ein Fenster von `23:00` bis
> `00:00` verarbeitet eine Stunde und berührt **einen** Tag. **Ohne diese Unterscheidung räumte
> jeder Lauf um Mitternacht einen Tageseimer aus, den er anschließend nicht neu schreibt** — die
> Stundenebene bliebe richtig, die Tagesebene verlöre einen Tag, und auffallen würde es erst im
> Dashboard.

**Ein Rückgriff über eine Mitternachtsgrenze berührt zwei Tage.** Der praktische Fall ist der
Delta-Lauf in der ersten Stunde eines Tages: Sein 15‑Minuten‑Rückgriff liegt im Vortag.

**Hier steht ein `INSERT … SELECT`, und in Schritt 2 keines.** Der Unterschied ist die
Schemagrenze: Quelle und Ziel liegen hier **beide** in `overlord_monitor`, und
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6 verbietet dem Schreib-Kontext genau das
Verlassen dieses Schemas. Ein Test hält es fest: In keinem der **sechs** Statements eines Laufs steht
das Wort `GlassfishDB`.

**Weiterhin löschen und neu schreiben, nie `anzahl = anzahl + n`** — dieselben zwei Gründe wie oben,
und ein dritter kommt dazu: Ein Prozess, der an einem Tag einmal Zeilen hatte und heute keine mehr,
verlässt die Stundenebene; sein Tageseimer verschwände ohne das `DELETE` nie.

**`rollup_lauf.zeilen_geschrieben` bleibt die Zahl der Stundenzeilen.** Die Zeilen aus den Läufen
vor dem 27.08.2026 stehen noch in der Tabelle; eine Summe beider Ebenen ab jetzt hineinzuschreiben
hieße, dieselbe Spalte in alten und neuen Zeilen verschieden zu lesen, ohne dass der Zeile das
anzusehen wäre. Die Tageszeilen stehen im Protokoll und in `RollupErgebnis`.

**Eingefügt wird in Stapeln zu 1.000 Zeilen** (`monitorDsl.batch(…)`, dasselbe Muster wie in
`ProzessKatalogRepository.speichereBestandsflags`). Die Größe ist eine Größenordnung und kein
gemessenes Optimum — gemessen ist der Lauf als Ganzes (§9).

### Schritt 5: die Monatsebene, aus der Tagesebene

**Wort für Wort Schritt 4, eine Ebene höher** — abgeleitet statt zweitgelesen, über *ganze* Monate
statt über das Fenster, gelöscht und neu geschrieben statt hochgezählt, der volle Ausdruck im
`GROUP BY`. Zwei Dinge sind neu:

**Aus der Tagesebene und nicht aus der Stundenebene.** Beide Wege ergäben dieselben Zahlen; einer
liest 123.049 Zeilen, der andere 335.610 (M87, Variante 3 gegen Variante 1). Die Regel ist damit
nicht *„aus der feinsten Ebene"*, sondern **„aus der nächstfeineren"** — und sie ist es aus beiden
Gründen zugleich: Sie ist billiger, und sie macht jede Ebene per Konstruktion zur Summe genau einer
anderen.

**Der Monatsausdruck ist `DATE(DATE_FORMAT(tag, '%Y-%m-01'))`.** Die Tagesebene kommt mit
`DATE(stunde)` aus; ein Monatsanfang braucht mehr. **Das umschließende `DATE(…)` ist kein
Beiwerk:** Ohne es stünde eine Zeichenkette in einer `DATE`-Spalte, und MariaDB wandelte sie beim
Einfügen still um — genau die Art Fehler, die dieses Projekt in Befund 11 der Vorrunde schon einmal
Zeilen gekostet hat. `RollupMonatStatementsTest` hält beides fest, den Ausdruck und seine
Wiederholung im `GROUP BY`.

### Schritt 7: der Bestandsanfang, geprüft und gemeldet *(31.08.2026, Schritt 10b‑2)*

**Das ist die Erkennung, die offener Punkt 54 seit dem 27.08.2026 verlangt** — und nichts darüber
hinaus. Gelöscht wird weiterhin nichts.

```
Liegt MIN(message_rollup.stunde) vor MIN(Message.MessageLastUpdate), auf die Stunde abgerundet?
  → nein: fertig, es hat zwei Indexspitzen gekostet
  → ja:   zähle die Eimer unterhalb, in allen drei Ebenen, und schreibe eine WARN-Zeile
```

| | |
|---|---|
| **Wo** | `RollupJob.meldeEingefroreneEimer`, aufgerufen **nach** `beendet_am` |
| **Wann** | nur beim **Volllauf** |
| **Was** | eine `WARN`-Zeile mit der Zahl der betroffenen Eimer je Ebene und dem Bestandsanfang |
| **Was nicht** | keine Spalte, keine Migration, kein Löschen, kein Abbruch des Laufs |

**Warum nach `beendet_am` und nicht davor.** Ein Befund ist erst dann einer: Was *jetzt* noch
unterhalb liegt, hat dieser Lauf nachweislich nicht angefasst. Der Schritt liegt außerdem
ausdrücklich **außerhalb** der Transaktion aus Schritt 3 bis 5 — eine Diagnose darf einen
erfolgreichen Lauf nicht zurücknehmen.

**Warum nur der Volllauf.** Der Delta-Lauf beginnt beim Wasserstand und damit noch später; er sähe
denselben Befund und meldete ihn **stündlich**. Eine `WARN`-Zeile, die jede Stunde kommt, wird nach
dem zweiten Tag nicht mehr gelesen. Der nächtliche Volllauf meldet sie einmal je Nacht, und das ist
die Frequenz, in der ein Bestandsanfang wandert.

**Warum abgerundet verglichen wird.** `MIN(Message.MessageLastUpdate)` ist sekundengenau — auf der
Testkopie `2024-10-01 02:00:28` —, `stunde` liegt immer auf einem Stundenanfang. Ohne das Abrunden
läge die Stundenebene mit `02:00` **immer** davor, und **jeder einzelne Lauf meldete einen
Fehlbefund**. Ein Test hält genau das fest (`ohne_eingefrorene_eimer_keine_warnung`).

**Alle drei Ebenen, nicht nur die Stundenebene.** Das schließt offenen Punkt **68**: Die
abgeleiteten Ebenen frieren mit ein, weil ihre Rechenbereiche (`betroffeneTage`,
`betroffeneMonate`) ebenfalls aus dem Fenster kommen. Ein Erkennungsweg, der nur eine Ebene prüft,
meldete eine Abweichung nur für eine von dreien.

**Was es im Normalbetrieb kostet: zwei Indexspitzen.** `MIN(MessageLastUpdate)` ist `Select tables
optimized away` (0,272 ms, §9), `MIN/MAX(stunde)` läuft über den Primärschlüssel. **Die drei
Zählungen laufen nur, wenn der Fall eingetreten ist.**

> ### Und die Abnahmeprobe ist damit eingegrenzt
>
> **`SUM(anzahl)` = `COUNT(Message)` gilt ab dem 31.08.2026 über den *überlappenden Bereich*, nicht
> über die ganze Tabelle** — `RollupSchreibRepository.ueberlappenderBereich(bestandsanfang)`, von der
> späteren der beiden unteren Grenzen bis zum Ende der Stundenebene.
>
> **Ohne diese Eingrenzung wäre die schärfste Kontrolle dieses Baus nach dem ersten produktiven
> Löschlauf dauerhaft rot** — und ein dauerhaft roter Test wird abgeschaltet. Das ist derselbe
> Mechanismus, den [`testfestigkeit.md`](testfestigkeit.md) §1 für die Katalogtests beschreibt.
>
> **Die Aussage wird dadurch enger, nicht schwächer.** Über dem Bestandsanfang ist sie unverändert
> scharf: jede Nachricht genau einmal. Was sie nicht mehr behauptet, ist etwas über einen Zeitraum,
> den die Quelle gar nicht mehr kennt — dort gibt es nichts, wogegen sich prüfen ließe.

### Kein `INSERT … SELECT` über die Schemagrenze

M89 hat die Probetabelle mit einem schemaübergreifenden `INSERT … SELECT` gefüllt: **51,242 s für
22 Monate.** Dieser Bau macht es anders — er liest in den Speicher und schreibt gebündelt —, weil
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6 sagt: *„Der Schreib-DSLContext darf
ausschließlich `overlord_monitor`."* Ein `INSERT … SELECT` aus `GlassfishDB` über den Schreib-Pool
widerspricht dem Satz, auch wenn `monitor_write` dort `SELECT` besitzt und die Schreibgarantie
unberührt bliebe.

**Der gebaute Weg ist gegen die 51,242 s gemessen: 45,772 s.** Die Faktor‑drei‑Schranke des
Auftrags ist damit nicht in Sicht, und es gibt nichts vorzulegen. **Belastbar ist der Satz „nicht
teurer" — nicht der Satz „schneller":** Die 51,242 s stammen aus einer anderen Sitzung an einem
anderen Tag, und der Abstand von 10,7 % liegt in derselben Größenordnung wie die Aufwärmaufschläge
dieser Runde. Der vollständige Belegvermerk nach Regel L10 steht in §9.

---

## 6. Die Scheiben — über einen Kalender, nicht über die Daten

**Der Volllauf liest in Monatsscheiben.** Das ist keine Vorsicht, sondern eine gemessene Grenze:

> **Der Lese-Pool setzt `SET SESSION max_statement_time=10`** ([`datenzugriff.md`](datenzugriff.md)
> §1). Ein einziges Statement über den Gesamtbestand reißt sie — **gemessen am 26.08.2026:
> `ERROR 1969 (70100) Query execution was interrupted (max_statement_time exceeded)`** nach zehn
> Sekunden. Der Volllauf ist ohne Schnitt nicht baubar.

Eine Monatsscheibe kostet **2,575 s** (größte Scheibe 2025‑07, 248.320 Zeilen, beste von fünf) —
**Faktor 3,9** Luft zur Zeitgrenze.

**Die Form ist die Bauvorgabe aus M92 im Wortlaut:** *„Er iteriert über einen Kalender, nicht über
die vorhandenen Daten."* Eine leere Scheibe wird also **mitgerechnet** und nicht übersprungen — auf
der Testkopie sind das die fünf Monate 2026‑01 bis 2026‑05. Sie kosten 1 ms je Scheibe; sie zu
überspringen wäre eine Optimierung ohne Gegenwert und genau die, die den Lauf falsch machte: **Ein
Eimer, der einmal Zeilen hatte und heute keine mehr hat, würde sonst nie geleert.**

Die erste und die letzte Scheibe sind in der Regel angebrochen. Das ändert nichts an der
Eimertreue: Beide Grenzen bleiben Stundenanfänge, und ein Monatserster um Mitternacht ist einer.

**Ein Delta-Fenster ist in aller Regel genau eine Scheibe** — der stündliche Lauf zahlt für den
Kalender fast nichts. **Die Ausnahme ist der Monatswechsel:** Fällt das Fenster über einen
Monatsersten, schneidet der Kalender es in zwei. Das trifft einen Lauf im Monat, kostet eine
zusätzliche sehr kleine Abfrage — und einmal die Drosselpause aus §8. Es ist gewollt und nicht zu
reparieren: Ein Kalender, der die Monatsgrenze für kleine Fenster überspringt, wäre nicht mehr
derselbe Kalender.

---

## 6a. Der einmalige Rückwärtslauf der abgeleiteten Ebenen *(27.08.2026, erweitert 31.08.2026)*

Nach einer Migration steht die neue Ebene leer da, während die nächstfeinere den Gesamtbestand
trägt. Der laufende Job zieht nur den Zeitraum nach, den sein Fenster berührt — die übrigen 646 Tage
beziehungsweise 22 Monate bekäme er nie zu fassen. `rollup/RollupNachzug` holt sie einmal nach.

```
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments=--overlord.rollup.abgeleitete-ebenen-nachziehen=true
```

> ### Umbenannt am 31.08.2026 — und warum ein Schalter für beide Ebenen
>
> Die Klasse hieß bis dahin `RollupTagNachzug`, der Schalter `tagesebene-nachziehen`. Beides ist mit
> der dritten Ebene falsch geworden: Der Lauf zieht jetzt **Tages- und Monatsebene** nach.
>
> **Zwei getrennte Schalter wären die Einladung, die Tagesebene neu zu rechnen und die Monatsebene
> stehen zu lassen.** Danach stünden zwei Ebenen auf zwei Ständen, und das Dashboard zeigte je nach
> Fensterbreite verschiedene Zahlen, ohne dass irgendwo ein Fehler protokolliert wäre — genau der
> Zustand, gegen den `ersetzeFenster` seine eine Transaktion hält. **Was zusammengehört, wird
> zusammen ausgelöst.**
>
> Der Schalter ist ein einmaliger Betriebsschalter ohne Eintrag in `application.yml`; ein Umbenennen
> kostet deshalb nichts als diesen Absatz.

**Je Scheibe erst der Tag, dann der Monat** — und nicht erst alle Tage und danach alle Monate. Zwei
Gründe, und der zweite ist der wichtigere:

1. Ein Monatseimer ist die Summe seiner Tageseimer. Eine Scheibe ist an den Monatsgrenzen
   geschnitten und berührt deshalb **genau einen** Kalendermonat; wenn sie an der Reihe ist, stehen
   alle Tage dieses Monats geschrieben, die es überhaupt gibt.
2. **Nach jeder Scheibe sind beide Ebenen für diesen Monat auf demselben Stand.** Bräche der Lauf
   ab, wäre das Ergebnis eine *kürzere* Historie und keine widersprüchliche.

**Kein Volllauf, und das aus zwei Gründen.** Ein Volllauf könnte dasselbe — er rechnet alle drei
Ebenen über den ganzen Bestand — und ist hier trotzdem der falsche Weg:

1. **Er liest `Message` noch einmal**, 45,772 s über den Lese-Pool, den sich die Anwendung mit der
   Produktion teilt. Die abgeleiteten Ebenen stehen vollständig in der jeweils nächstfeineren; die
   Quelle dafür noch einmal zu befragen, ist Arbeit ohne Erkenntnisgewinn.
2. **Im Profil `dev` deckte er den Bestand gar nicht ab.** Seine obere Grenze ist die Anwendungsuhr
   (§4); die 15 Rollupzeilen aus Juni und Juli 2026 lägen dahinter. **Dieser Lauf richtet sich nach
   dem, was in der Stundenebene *steht*, und nicht nach einer Uhr** —
   `MIN(stunde)` bis `MAX(stunde)`.

**In Monatsscheiben über einen Kalender**, mit der Drosselung aus §6 — dieselbe Form wie der
Volllauf. **Anders als dort ist jede Scheibe ihre eigene Transaktion:** Der Ausgangszustand ist eine
**leere** neue Ebene, und eine halb gefüllte ist besser als eine leere. Wiederholen lässt er sich
ohnehin — jede Scheibe löscht ihren Bereich, bevor sie ihn schreibt.

> ### ⚠️ Und diese Klammer hat vier Stunden lang nur in der Dokumentation gestanden
>
> **Der Satz oben war am 31.08.2026 zunächst falsch, und kein Test war rot.** `RollupNachzug` rief
> `rechneTageEbeneNeu` und `rechneMonatsEbeneNeu` **einzeln** auf; beide tragen bewusst kein eigenes
> `@Transactional` (sie sind Bausteine von `ersetzeFenster`), und von außen aufgerufen lief jede im
> **Autocommit**. Vier unabhängig committende Anweisungen je Scheibe. Bräche es zwischen dem
> `DELETE` und dem `INSERT … SELECT` der Monatsebene ab, stünde der Monat leer da, während die
> Tagesebene ihn vollständig trägt — **genau die zwei Stände, gegen die dieser Lauf antritt** —,
> und `RollupStarter` meldete dem Betreiber, was schon geschrieben sei, bleibe richtig.
>
> **Behoben mit einer benannten Klammer:** `RollupSchreibRepository.rechneAbgeleiteteEbenenNeu`
> trägt `@Transactional` und ruft beide Ebenen nacheinander. Der Nachzug ruft **diese eine** Methode
> über den Spring-Proxy; die Drosselung liegt bewusst außerhalb. Ein `@Transactional` an
> `RollupNachzug.fuehreAus` wäre falsch gewesen — es machte den ganzen Lauf samt seiner 21 Sekunden
> `Thread.sleep` zu **einer** Transaktion.
>
> **Gefunden hat es eine Durchsicht, nicht ein Test** — und das ist der eigentliche Befund. Der
> Datenbanktest zum Abbruch (`RollupDbIT.keine_ebene_bleibt_bei_einem_abbruch_zurueck`) **kann**
> eine fehlende Klammer nicht finden: Er bringt seine eigene Transaktion mit, und `monitorDsl` hängt
> über `TransactionAwareDataSourceProxy` an ihr. Deshalb prüft seit dem 31.08.2026
> `RollupTransaktionsgrenzenTest` die **Ursache** statt der Wirkung — wo die Klammern sitzen und wo
> ausdrücklich keine sitzt. Dieselbe Bauform, die Regel **T1** für Laufzeiteigenschaften verlangt.

**Er hinterlässt keine Zeile in `rollup_lauf`, und das ist Absicht.** `rollup_lauf` trägt den
**Wasserstand**: bis wohin ist aus `Message` gerechnet worden. Dieser Lauf rechnet nichts aus
`Message`; er ordnet um, was schon da ist. Eine Zeile mit `art = 'VOLL'` und einem Fenster über den
Gesamtbestand behauptete einen Wasserstand, den er nicht erarbeitet hat — und der nächste Delta-Lauf
übersähe daraufhin einen Bereich. Seine Spur steht im Protokoll.

**Gefahren am 27.08.2026 (nur Tagesebene) und am 31.08.2026 (beide Ebenen) gegen die Testkopie:**

| | 27.08.2026 | **31.08.2026** |
|---|---:|---:|
| Bereich | `2024-10-01 02:00` bis `2026-07-08 18:00` (15.496 Stundeneimer) | **derselbe** |
| Scheiben | 22 | **22** |
| berührte Kalendertage | 646 — alle, auch die leeren | **646** |
| geschriebene Tageszeilen | 123.049 | **123.049** |
| geschriebene Monatszeilen | — | **11.957** |
| `SUM(anzahl)` danach | 3.341.519 | **3.341.519** in allen drei Ebenen |
| Laufzeit | 26.026 ms, davon 21 s Drosselung | **27.483 ms**, davon 21 s Drosselung |

**Die zweite Ebene kostet 1,5 Sekunden.** Die Arbeit ohne Drosselung liegt bei rund 6,5 Sekunden
gegen vorher rund 5 — für 11.957 zusätzliche Zeilen, gruppiert aus 123.049, in einer Datenbank und
ohne die Quelle anzufassen. Der Vergleich mit dem Volllauf (45,772 s ohne Drosselung, und der
läse `Message`) ist die eigentliche Begründung dieses Wegs.

**Die 11.957 treffen M87 Variante 4 auf die Zeile** — die Zahl war dort an der Quelle gezählt und
nicht geschätzt. Dass der Rückwärtslauf sie aus der Tagesebene noch einmal erreicht, ist die
Summenprobe der neuen Ebene.

**`overlord.rollup.abgeleitete-ebenen-nachziehen` steht neben `beim-start` und nicht darin.**
`beim-start` nimmt eine `LaufArt`, und dieser Lauf ist keine: Er liest `Message` nicht an,
hinterlässt keine Protokollzeile und hebt keinen Wasserstand. Ihn als dritten Wert dort einzureihen
hieße, drei verschiedene Dinge unter einem Namen zu führen. Beides zusammen ist zulässig und in
dieser Reihenfolge sinnvoll: erst der Lauf, dann der Nachzug.

---

## 7. Die Abfragen (Regel L7)

### Die Aggregation

`rollup/RollupLeseRepository.aggregiere(von, bis)`, gelesen über `glassfishDsl`. **Wörtlich die
Fassung aus M88**, übernommen und nicht nachgebaut:

```sql
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= ? AND MessageLastUpdate < ?
GROUP BY stunde, ProcessID, MessageStatus;
```

**Ein Unterschied zur Messfassung, und er ist der bessere Weg.** jOOQ schreibt in `GROUP BY` den
**vollen Ausdruck** aus, wo M88 den Alias `stunde` verwendet. Semantisch ist das dasselbe — und es
ist genau die Fassung, die **Befund 11** empfiehlt: Dort hat `GROUP BY` an eine gleichnamige
*Tabellenspalte* gebunden statt an den Ausdrucksalias, und bei zwei von drei Mandanten sah das
Ergebnis trotzdem richtig aus. `Message` hat zwar keine Spalte `stunde`, aber die sichere Form
kostet hier nichts. Ein Test hält das gerenderte Statement Zeichen für Zeichen fest
(`RollupStatementsTest`).

**Kein `STRAIGHT_JOIN`, in keiner Fassung** (M42: Faktor 219 bis 1094).

**`EXPLAIN` gegen die Testkopie, 26.08.2026** — Zeichen für Zeichen der Plan aus M88:

```
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
| id   | select_type | table   | type  | possible_keys                                           | key                  | key_len | ref  | rows  | Extra                                                  |
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
|    1 | SIMPLE      | Message | range | MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL | 17730 | Using index condition; Using temporary; Using filesort |
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
```

**`MessageLastUpdateProcessMessageIDX` bringt dem Rollup nichts.** Er ist für den Cursor der
Nachrichtenliste gebaut, und `MessageStatus` steht in keinem der beiden Indizes — jede Fassung
braucht den Rückgriff auf die Tabellenzeile. Ein deckender Index müsste `MessageStatus` enthalten;
ihn anzulegen hieße, auf `GlassfishDB` zu schreiben, und das ist ausgeschlossen (Regel S1).

**Oberhalb einer geschätzten Bereichsgröße von rund einer halben Million Sätzen kippt der Optimierer
doch auf den zusammengesetzten Index** (M92, Monatsscheiben). Auf die Laufzeit wirkt sich das nicht
sichtbar aus: 11,4 µs je Zeile mit dem schmalen, 11,1 µs mit dem breiten.

### Der früheste Zeitstempel

```sql
SELECT MIN(MessageLastUpdate) FROM GlassfishDB.Message;
```

`EXPLAIN`: **`Select tables optimized away`** — der Wert kommt aus dem ersten Blatt von
`MessageLastUpdateIDX`, ohne einen einzigen Tabellenzugriff.

**Ohne Zeitfenster, und das ist gedeckt (Regel L9):** Gefragt ist der Anfang des Bestands, und ein
Zeitfenster schnitte genau die Zeile weg, um die es geht. Es ist außerdem keine Aggregation über
Zeilen, sondern eine Indexspitze — dieselbe Form, die die Dev-Uhr seit Schritt 2 beim Start liest.
Kosten: **0,272 ms** (§9).

---

## 8. Auslösung, Profil, Konfiguration

### Zwei Riegel, und beide sind Absicht

1. **Das Spring-Profil `rollup`.** Ohne es gibt es `RollupPlaner` gar nicht — und damit im Profil
   `dev` standardmäßig keine Auslösung. **Ein Job, der beim lokalen Start unaufgefordert in die
   geteilte Testkopie schreibt, ist eine Überraschung.** Im Betrieb läuft der Job damit als eigener
   Prozess derselben Anwendung (`SPRING_PROFILES_ACTIVE=prod,rollup`), ohne zweites Artefakt und
   **ohne eigenes Wurzelpaket** ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6: „Die
   Trennung läuft über das Spring-Profil, nicht über den Namensraum.").
2. **`overlord.rollup.aktiv`.** Damit lässt sich die Auslösung abschalten, ohne das Profil zu
   ändern — im Betrieb kommt das Profil aus einer Umgebungsvariablen, und eine Abschaltung über sie
   kostete eine Neubereitstellung. **Fehlt der Schlüssel, ist die Auslösung aus.**

Der `dev`-Block in `application.yml` setzt `aktiv: false` **zusätzlich ausdrücklich**, damit auch
ein neugieriges `dev,rollup` nicht schreibt.

### Die Schlüssel unter `overlord.rollup`

| Schlüssel | Vorgabe | Wirkung |
|---|---|---|
| `aktiv` | `false`, wenn nicht gesetzt (`application.yml`: `true`, im `dev`-Block `false`) | zeitgesteuerte Auslösung an/aus |
| `delta-plan` | `0 5 * * * *` | Cron des stündlichen Laufs |
| `voll-plan` | `0 0 3 * * *` | Cron des nächtlichen Laufs — **ungemessen** |
| `scheiben-pause` | `1s` | Drosselung zwischen zwei Monatsscheiben (L6) — **ungemessen** |
| `beim-start` | — | `DELTA` oder `VOLL`: ein einmaliger Lauf beim Start |
| `abgeleitete-ebenen-nachziehen` | `false` | der **einmalige** Rückwärtslauf der Tages- **und** der Monatsebene (§6a). Hieß bis zum 31.08.2026 `tagesebene-nachziehen` |

**Beide Ausdrücke stehen als Platzhalter in `application.yml` und nicht als Literal im Code.** Fehlt
einer, scheitert der Start — das ist besser als ein Zeitplan, den niemand kennt.

> **Die Uhrzeit des Nachtlaufs ist ungemessen (Regel Q4).** Die Verteilung von `MessageLastUpdate`
> über die Tagesstunde ist in M86 bis M92 **nicht erhoben** worden. `03:00` ist die übliche Wahl und
> keine hergeleitete. Offener Punkt 50.

**Die Minute des Delta-Laufs (5) ist ebenfalls nicht gemessen — aber folgenlos.** Weil jeder Lauf
ganze Stundeneimer ersetzt und 15 Minuten zurückgreift, wird die zuletzt geschriebene Stunde beim
nächsten Lauf ohnehin neu gerechnet. Der Abstand zur vollen Stunde hält den Job nur aus dem Gedränge
der Jobs heraus, die dort üblicherweise starten.

### Nur eine Ausführung gleichzeitig

Läuft bereits ein Lauf — Zeile in `rollup_lauf` ohne `beendet_am`, jünger als **eine Stunde** —,
wird der neue übersprungen und das **auf WARN** protokolliert. Ein übersprungener Lauf ist kein
Normalfall, sondern der Hinweis darauf, dass ein Lauf länger braucht als sein Takt.

Die Altersgrenze von einer Stunde ist der Takt des Delta-Laufs. Ohne sie sperrte ein einziger
abgestürzter Lauf den Job **dauerhaft und lautlos**: Jeder folgende würde übersprungen, das
Dashboard veraltete, und im Protokoll stünde nichts als „übersprungen".

Zwei gleichzeitige Läufe wären fachlich nicht einmal falsch — sie löschen und schreiben dieselben
Eimer —, aber sie verdoppelten die Last auf einer Instanz, die sich mit der Produktion teilt (L6).

### Der Weg von Hand

**Als Startparameter, ausdrücklich nicht als HTTP-Endpunkt:**

```
java -jar overlord-monitor.jar --overlord.rollup.beim-start=VOLL
./mvnw spring-boot:run -Dspring-boot.run.arguments=--overlord.rollup.beim-start=DELTA
```

**Warum kein Endpunkt.** Ein Endpunkt, der die Rolluptabelle neu schreibt, wäre eine Schreibfläche
im Anfragepfad — und die gäbe es dann auch für den, der sie nicht bedienen soll. Der Startparameter
steht nur dem offen, der die Anwendung startet, und er hinterlässt dieselbe Spur in `rollup_lauf`
wie jeder andere Lauf.

Er hängt **weder am Profil `rollup` noch an `aktiv`** — beide regeln die *zeitgesteuerte* Auslösung;
dieser Lauf ist ausdrücklich angefordert worden. Ein gescheiterter Lauf verhindert den Start der
Anwendung **nicht**: Die Oberfläche hängt nicht am Rollup, und ein Backend, das wegen eines
Aggregationsfehlers gar nicht hochkommt, wäre der größere Schaden.

> **Für die Abnahmezahlen lokal** braucht es zusätzlich die Systemuhr statt des dev-Ankers (§4):
> `--spring.profiles.active=prod --server.port=0 --overlord.rollup.beim-start=VOLL`.

### Leistungsregel L6, und wo sie greift

> **L6 — „Der Rollup-Job läuft gedrosselt. Er teilt sich die Instanz mit der Produktion."**

**Beim Delta-Lauf ist sie Zeremonie statt Schutz, und das gehört so gesagt.** Ein Statement, das
88 ms braucht und einmal je Stunde läuft, belegt die Instanz zu **0,0024 %** — das ist die
*dichteste* Stunde von rund 15.000, nicht die durchschnittliche. Wovor die Drosselung schützen soll,
tritt beim stündlichen Lauf nicht ein. *Eine Regel, die ohne Grund zitiert wird, verliert ihre
Wirkung dort, wo sie gebraucht wird.*

**Gebraucht wird sie beim Nachtlauf**, und dort ist sie umgesetzt: eine Pause zwischen zwei
Monatsscheiben. Gewartet wird **zwischen** den Scheiben, nicht vor der ersten oder nach der letzten
— **ein Delta-Lauf hat in aller Regel genau eine Scheibe und wartet dann nicht.** Einmal im Monat,
wenn sein Fenster über den Monatsersten fällt, wartet er eine Sekunde. Das ist der Preis dafür, dass
Delta- und Volllauf durch denselben Kalender gehen, und er ist gering genug, um ihn zu zahlen.

> **Belegvermerk (Regel L10).**
> *Gemessen ist:* Der Volllauf kostet ohne Pause 45,772 s, mit einer Sekunde je Scheibengrenze
> 69,186 s (22 Pausen).
> *Behauptet wird:* Eine Sekunde je Scheibe ist die richtige Drosselung.
> **Die Lücke:** Wie viel Last die Produktionsinstanz nachts verträgt, ist **nicht erhoben**. Der
> Wert ist eine Vorgabe ohne Messung und deshalb konfigurierbar. Offener Punkt 50.

---

## 9. Messungen (Regel L7)

Alle Werte gegen die Testkopie am **26.08.2026**, Client `mysql.exe` 8.0.46 mit
`--ssl-mode=DISABLED` (dieselbe Abweichung A1 wie seit M32), Laufzeiten aus `SHOW PROFILES`,
je ein Aufwärmlauf und dann **beste von fünf**.

### Die Aggregation, so wie der Code sie schickt

| Fenster | Nachrichten | Aufwärmlauf | **beste von fünf** | alle fünf (ms) | µs je Zeile |
|---|---:|---:|---:|---|---:|
| **dichteste Stunde des Bestands** `2025-12-07 17:00` | 8.630 | 94,424 ms | **84,999 ms** | 88,481 · 85,833 · 84,999 · 89,341 · 87,140 | 9,85 |
| **typischer Delta-Lauf** `2025-12-30 03:00`–`05:00` | 358 | 5,451 ms | **4,095 ms** | 4,095 · 4,460 · 4,385 · 4,273 · 4,174 | 11,4 |
| **letzte Stunde des Bestands** `2026-07-08 17:00` | 285 | 3,848 ms | **3,626 ms** | 3,903 · 3,860 · 4,913 · 4,223 · 3,626 | 12,7 |
| **24 h ab dem Anker** `2025-12-29 05:00`–`2025-12-30 05:00` | 6.248 | 68,364 ms | **64,150 ms** | 65,288 · 64,150 · 64,935 · 69,661 · 70,201 | 10,3 |
| **größte Monatsscheibe** `2025-07` | 248.320 | 2.622,968 ms | **2.575,006 ms** | 2.715,502 · 2.654,878 · 2.575,006 · 2.654,528 · 2.687,443 | 10,4 |
| `MIN(MessageLastUpdate)` | — | 0,508 ms | **0,272 ms** | 0,303 · 0,296 · 0,295 · 0,373 · 0,272 | — |

**9,85 bis 12,7 µs je Zeile über einen Mengenbereich von Faktor 871.** Das deckt sich mit M88
(10,2–11,3 µs) und M92 (11,0–11,4 µs), die unabhängig davon erhoben wurden. **Die Laufzeit hängt an
der Zeilenzahl, nicht an der Fensterbreite.**

Die dichteste Stunde des Bestands trägt hier wie in M88 **8.630 Zeilen** — die Testkopie ist seit
der Messrunde unverändert.

### Die Zeitgrenze des Lese-Pools

| | |
|---|---|
| Gesamtbereich in **einem** Statement, `max_statement_time=10` | **`ERROR 1969` nach 10 s** |
| größte Monatsscheibe | 2,575 s — Faktor 3,9 Luft |

**Das ist der Grund für den Kalender aus §6**, und es ist eine Messung und keine Auslegung.

### Der Volllauf, gebaut gegen gemessen

| Lauf | Scheiben | Zeilen | `SUM(anzahl)` | **Laufzeit** |
|---|---:|---:|---:|---:|
| M89, `INSERT … SELECT` über die Schemagrenze (22 Monatsscheiben) | 22 | 335.610 | 3.341.519 | 51,242 s |
| **gebaut**, Systemuhr, ohne Drosselung | 23 | **335.610** | **3.341.519** | **45,772 s** |
| **gebaut**, Systemuhr, mit `scheiben-pause: 1s` | 23 | 335.610 | 3.341.519 | **69,186 s** |
| gebaut, dev-Anker, ohne Drosselung | 15 | 335.595 | 3.336.386 | 45,994 s |
| **gebaut mit Tagesebene**, Systemuhr, mit `scheiben-pause: 1s` *(27.08.2026)* | 23 | **335.610** + **123.049** | **3.341.519** | **81,903 s** |

> **Der Auftrag verlangt, den gebauten Weg gegen die 51,242 s zu messen und das Ergebnis zu melden;
> läge er um mehr als Faktor drei darüber, wäre das eine Entscheidung für den Auftraggeber.**
> **Er liegt darunter: 45,772 s gegen 51,242 s, also 10,7 % schneller.** Es gibt nichts vorzulegen.
>
> ### Was die Tagesebene den Volllauf kostet *(27.08.2026)*
>
> **12,7 Sekunden, also +18,4 %** — 81,903 s gegen 69,186 s, beide mit derselben Drosselung, beide
> gegen die Systemuhr, beide über 23 Scheiben. Das ist der Preis für 22 zusätzliche
> `DELETE`/`INSERT`-Paare über eine Tabelle von 8,53 MiB, und er fällt **nachts** an.
>
> **Der Delta-Lauf zahlt ihn nicht in dieser Größenordnung**: Er berührt einen oder zwei
> Kalendertage, nicht 646. Gemessen ist er in dieser Form nicht — der Datenbanktest fährt ihn, aber
> mit dem Aufbau einer JVM darin, und das ist keine Laufzeitmessung. **Ungemessen, und hier als
> solches benannt** (Regel Q4).
>
> **Belegvermerk (Regel L10).** *Gemessen ist:* zwei Läufe auf derselben Instanz, derselbe
> Bestand,
> beide warm. *Behauptet wird:* Der gebaute Weg ist nicht teurer als der verworfene. **Die Lücke:**
> Die 51,242 s stammen aus einer anderen Sitzung an einem anderen Tag; ein Aufwärmunterschied ist
> nicht auszuschließen. Der Abstand von 10,7 % liegt in derselben Größenordnung wie die
> Aufwärmaufschläge dieser Runde — **1,9 % bis 33,1 %** über die fünf Aggregationsfenster
> (und 86,8 % bei der Indexspitze, wo die absolute Zeit unter einer Millisekunde liegt).
> **Belastbar ist deshalb nur der Satz „nicht teurer", nicht der Satz „schneller".**

### Der Delta-Lauf, durch die ganze Anwendung

Gemessen im Datenbanktest, also einschließlich Spring, jOOQ, Netz und Schreibtransaktion:

| Fenster | Nachrichten | Rollupzeilen | Laufzeit |
|---|---:|---:|---:|
| `2025-12-30 03:00`–`05:00`, erster Lauf der JVM | 358 | 28 | 387 ms |
| dasselbe Fenster, danach | 358 | 28 | **20 bis 31 ms** |
| `2025-12-29 05:00`–`2025-12-30 05:00` | 6.248 | 570 | **129 ms** |

**Der erste Lauf einer JVM kostet das Zwölffache.** Das ist der kalte Abfrageplan, die kalte
Verbindung und die erste Transaktion — kein mengenabhängiger Anteil.

### Die Tabelle, nach dem Volllauf

| | Wert | M89 |
|---|---:|---:|
| Zeilen in `message_rollup` | **335.610** | 335.610 |
| `SUM(anzahl)` | **3.341.519** | 3.341.519 |
| verschiedene `process_id` | **738** | 738 |
| verschiedene `message_status` | **12** | 12 |
| früheste Stunde | `2024-10-01 02:00:00` | `2024-10-01 02:00:00` |
| späteste Stunde | `2026-07-08 17:00:00` | `2026-07-08 17:00:00` |
| Größe (`DATA_LENGTH` + `INDEX_LENGTH`) | 21,61 MiB | 21,59 MiB |
| **davon Indexanteil** | **0 Byte** | 0 Byte |
| `TABLE_COLLATION` | `utf8mb4_general_ci` | — |
| `rollup_lauf` | 3 Zeilen, 0,03 MiB | — |

**Und die Tagesebene daneben** *(gemessen 27.08.2026, nach dem Volllauf Nr. 407)*:

| | `message_rollup` | `message_rollup_tag` | Verhältnis |
|---|---:|---:|---:|
| Zeilen | 335.610 | **123.049** | 36,7 % |
| `SUM(anzahl)` | 3.341.519 | **3.341.519** | **gleich** |
| Größe | 21,53 MiB | **8,53 MiB** | **39,6 %** |
| davon Indexanteil | 0 Byte | **0 Byte** | — |

**`SUM(anzahl)` ist in beiden Ebenen dieselbe Zahl wie in `Message`** — das ist die Summenprobe über beide Tabellen, und sie ist die Abnahmebedingung dieses Schritts. Der Indexanteil ist auch hier **0 Byte**, wie bei der Stundenebene und aus demselben Grund.

**Und die Monatsebene daneben** *(gemessen 31.08.2026, nach dem Volllauf Nr. 907)*:

| | `message_rollup` | `message_rollup_tag` | `message_rollup_monat` |
|---|---:|---:|---:|
| Zeilen | 335.610 | 123.049 | **11.957** |
| `SUM(anzahl)` | 3.341.519 | 3.341.519 | **3.341.519** |
| Größe | 21,53 MiB | 8,53 MiB | **1,52 MiB** |
| davon Indexanteil | 0 Byte | 0 Byte | **0 Byte** |

> ### Der Volllauf schreibt alle drei Ebenen — und die Zahlen, die er dabei nennt, sind kleiner als die der Tabellen
>
> **Gefahren am 31.08.2026** über `--overlord.rollup.beim-start=VOLL`, Profil `dev`:
>
> ```
> Rollup-Lauf VOLL (Nr. 907) fertig: 335595 Stundenzeilen, 123041 Tageszeilen und
> 11952 Monatszeilen fuer 3336386 Nachrichten in 75754 ms
> Fenster 2024-10-01T02:00 bis 2025-12-30T05:00 (10923 Stundeneimer), 15 Scheiben
> ```
>
> **335.595 gegen 335.610, und das ist kein Fehler, sondern §4.** Im Profil `dev` ist die obere
> Fenstergrenze die **Anwendungsuhr**, und die steht am Anker der Testkopie
> (`2025-12-30 04:09:47`). Die **15 Rollupzeilen aus Juni und Juli 2026** liegen dahinter; der
> Volllauf sieht sie nicht, löscht sie aber auch nicht — sein `DELETE` reicht nur bis zur
> Fenstergrenze. **Deshalb stehen in der Tabelle danach wieder 335.610.** Dasselbe gilt eine und
> zwei Ebenen höher: 123.041 gegen 123.049 und 11.952 gegen 11.957.
>
> **Die Abnahmezahlen brauchen die Systemuhr** — sie stehen in der Tabelle darüber und nicht in
> dieser Protokollzeile.
>
> **Nach dem Lauf tragen alle drei Ebenen wieder `SUM(anzahl) = 3.341.519`**, also die Zeilenzahl
> von `Message` selbst. Das ist die Summenprobe über drei Tabellen, und sie ist die
> Abnahmebedingung dieses Schritts.

> Die 21,53 MiB gegen die 21,61 MiB oben sind `information_schema`-Rauschen und keine Abweichung — dieselbe Statistik hat in M89 zwischen zwei Ablesungen um 16 % geschwankt.

**Alle sechs Kontrollen aus M89 treffen auf die Einheit genau.** `SUM(anzahl) = 3.341.519` heißt:
jede Nachricht ist genau einmal gezählt, keine doppelt, keine verloren.

> Die 0,02 MiB Unterschied in der Größe sind `information_schema`-Rauschen und keine Abweichung —
> dieselbe Statistik hat in M89 zwischen zwei Ablesungen um 16 % geschwankt. **Wer die Tabelle
> wirklich vermessen will, misst sie nicht über `information_schema`.**

---

## 9a. ⚠️ Das Tor bei zwölf Monaten — gemessen, und es löst aus *(27.08.2026)*

**Der Auftrag zu Schritt 10b‑1 hat ein Tor gesetzt, bevor gemessen wurde:**

> | Ergebnis (12 Monate, warm, größter Mandant) | Folge |
> |---|---|
> | unter **700 ms** | die Tagesebene genügt. Zahl in `rollup.md` festhalten |
> | **700 ms oder mehr** | **anhalten und melden.** Die Monatsebene ist dann eine Vorlage an den Auftraggeber und **nicht** zu bauen |
>
> Woher die 700 ms kommen: Der Lese-Pool bricht ein Statement nach **10 s** ab, der Kaltfaktor aus
> M44 geht bis **9,66**. Warm muss die Ansicht also unter rund **1,0 s** bleiben, sonst stirbt sie
> kalt in Produktion. 700 ms sind 70 % davon.

### Das Ergebnis

Beide Ebenen in **derselben** Sitzung, abwechselnd gefahren — die Werte aus M94 stammen aus einer
anderen Sitzung an einem anderen Tag. Aufwärmlauf, dann beste von fünf; Sitzungen
`scripts/messung-schritt10b-1/c4-tor-*.sql`.

| Ansicht | Mandant | **Stundenebene** | **Tagesebene** | Faktor |
|---|---|---:|---:|---:|
| **Verlauf, 12 Monate / Monat** | **NEXANS** | 2.203,423 ms | **767,128 ms** | **2,87** |
| **Verteilung, 12 Monate** | **NEXANS** | 2.487,394 ms | **908,539 ms** | **2,74** |
| Verlauf, 30 Tage / Tag | NEXANS | 149,663 ms | **45,990 ms** | 3,25 |
| Verteilung, 30 Tage | NEXANS | 180,547 ms | **62,454 ms** | 2,89 |
| Verlauf, 12 Monate / Monat | SUTTONS | 1.473,612 ms | **425,689 ms** | 3,46 |
| Verteilung, 12 Monate | SUTTONS | 1.540,673 ms | **431,657 ms** | 3,57 |
| Verlauf, 30 Tage / Tag | SUTTONS | 113,641 ms | **29,954 ms** | 3,79 |
| Verteilung, 30 Tage | SUTTONS | 128,204 ms | **31,504 ms** | 4,07 |

**Die Ersparnis ist über alle acht Paare Faktor 2,7 bis 4,1** — und sie deckt sich mit dem, was die
Zeilenzahl vorhersagt: Die Tagesebene liest über zwölf Monate 100.270 statt 280.186 Zeilen (Faktor
2,79) und über 30 Tage 6.843 statt 20.971 (Faktor 3,06). **Die Kosten hängen an der Zahl gelesener
Zeilen und an nichts sonst** — genau wie M94 es gemessen hat.

**Die Pläne sind Zeile für Zeile dieselben**, nur mit weniger Zeilen: `range` über `PRIMARY` mit
`Using where; Using temporary; Using filesort` bei `NEXANS` (61.705 geschätzte Zeilen statt 167.152),
Einstieg über `pm` bei `SUTTONS` — dasselbe Muster, das M94 in allen zwölf Plänen beschreibt.

**Die Vorbedingung, ohne die der Rest nichts wert wäre:** Beide Ebenen liefern über alle zwölf
Monate und für beide Mandanten **dieselben Zahlen**. Die Gleichheitsprobe steht als erste Abfrage in
beiden Sitzungen.

### ⚠️ Das Tor löst aus

**Der größte Mandant liegt bei zwölf Monaten über 700 ms: 767,128 ms für den Verlauf und
908,539 ms für die Verteilung.** Nach der vorregistrierten Lesart heißt das: **anhalten und melden.
Die Monatsebene ist eine Vorlage an den Auftraggeber und wird hier nicht gebaut.**

**Der Auftrag hat diesen Ausgang vorhergesehen** und ihn ausdrücklich als Absicht benannt: *„Die
Schätzung, auf der die Entscheidung ruht, lag bei 780 ms — sie kann also schon knapp danebenliegen
und das Tor auslösen."* Gemessen sind 767,128 ms; die Schätzung lag **1,7 % daneben**.

**Was die Tagesebene trotzdem gebracht hat, und es ist nicht wenig:**

| | |
|---|---|
| **30 Tage / Tag** — die Ansicht, die heute die teuerste gebaute wäre | von 149,663 auf **45,990 ms** (Verlauf) und von 180,547 auf **62,454 ms** (Verteilung). **Beide Paare unter 100 ms** |
| **48 Stunden / Stunde** — die Ansicht beim Öffnen | unberührt, sie liest weiter die Stundenebene (6,834 ms, M94) |
| **12 Monate / Monat** | von 2,2–2,5 s auf **0,77–0,91 s**. Tragbar wäre es fast; das Tor ist es nicht |

**Zwei von drei Paaren sind damit erledigt.** Was offen bleibt, ist genau eines — und genau dafür
ist das Tor da.

> **Belegvermerk (Regel L10).**
> *Gemessen ist:* acht Paare, beide Ebenen, in derselben Sitzung, **warm**, Aufwärmlauf und dann
> beste von fünf.
> *Behauptet wird:* Die Tagesebene senkt die Kosten um Faktor 2,7 bis 4,1, und das Tor löst
> trotzdem aus.
> **Die Lücke:** Alle acht Werte sind **Warmwerte**. `FLUSH TABLES` steht `monitor_read` nicht zu,
> und der Puffer fasst die beiden Rolluptabellen um ein Vielfaches. Der Kaltfaktor aus M44 (bis
> 9,66) ist auf **767 ms** angewandt rund 7,4 s und damit unter der 10‑s‑Grenze des Lese-Pools —
> aber das ist eine **Übertragung und keine Messung**; der Faktor stammt aus einer anderen Abfrage
> über eine andere Tabelle. **Genau dieser Abstand ist der Grund, warum das Tor bei 700 ms und
> nicht bei 1,0 s steht.** Die zweite Lücke: Alle Zahlen stammen von der Testkopie, und die
> Zwölf‑Monats‑Ansicht liest dort einen Bestand von 22 Monaten. In einem Bestand, der über zwölf
> Monate dichter ist, liest sie mehr.

### Warum nicht einfach die Monatsebene dazubauen

Sie wäre klein: **11.957 Zeilen über den ganzen Bestand** (M87, Variante 4) — weniger, als die
Zwölf‑Monats‑Ansicht heute allein für ihr Fenster liest. Der Bau wäre dieselbe Form noch einmal:
eine dritte Tabelle, ein vierter Schritt im Lauf, ein zweiter Rückwärtslauf.

**Er wird trotzdem nicht gebaut, und der Grund ist nicht Aufwand, sondern Verfahren.** Das Tor ist
vor der Messung gesetzt worden, damit die Entscheidung nicht davon abhängt, wie nah die Zahl an der
Schwelle liegt. 767 ms sind näher an 700 als an 900 — und genau deshalb wäre es jetzt die falsche
Zeit, die Schwelle noch einmal anzusehen. **Die Entscheidung gehört dem Auftraggeber.** Offener
Punkt 55 trägt sie weiter; die Zahlen dafür stehen vollständig hier.

**Drei Dinge, die er dabei wissen sollte:**

1. **Eine Monatsebene löste das Paar 12 Monate/Monat vollständig.** Zwölf Monate lesen dann zwölf
   Monatszeilen je Prozess und Status statt 100.270 Tageszeilen — bei linearen Kosten wäre das der
   Sprung in den einstelligen Millisekundenbereich, nicht eine weitere Halbierung.
2. **Sie kostet eine dritte Ebene, die konsistent gehalten werden muss.** Aus der Tagesebene
   abgeleitet wäre sie es per Konstruktion, so wie die Tagesebene aus der Stundenebene — der Lauf
   bekäme einen fünften Schritt in derselben Transaktion.
3. **Oder das Paar wird geändert statt der Tabelle.** Die Entscheidung „12 Monate / Monat" ist eine
   Gestaltungsentscheidung; ein Jahr in **Wochen** läge bei rund 52 Eimern und läse dieselbe
   Tagesebene. Das ist keine technische Frage und gehört deshalb erst recht dem Auftraggeber.

---

## 9b. Der Rollup als Beschleuniger der Nachrichtenliste — gebaut *(30.08.2026)*

> ### ⚠️ Diese Tabelle hat seit dem 30.08.2026 **zwei** Verbraucher
>
> Bis dahin war `message_rollup` ausschließlich die Datenquelle des Dashboards. Seither verengt die
> **Nachrichtenliste** ihr Zeitfenster über dieselbe Tabelle
> ([`nachrichtenliste.md`](nachrichtenliste.md) §5d): Sie fragt vorab, in welchen Stunden ein Mandant
> überhaupt Nachrichten hat, und stellt die Quellabfrage nur für diese Stunden.
>
> **Wer diese Tabelle künftig ändert, ändert zwei Dinge** — und das zweite ist die Kernabfrage des
> Werkzeugs, nicht ein Diagramm. Konkret betroffen wären:
>
> | Änderung | Was sie an der Liste bricht |
> |---|---|
> | Eine Spalte mit anderer Bedeutung füllen (etwa `message_status` klassifiziert statt roh) | Der Statusfilter der Verengung ist **derselbe Ausdruck** wie auf `Message` (`MessageStatusClassifier.bedingung`). Driftet die Bedeutung, verliert die Liste Zeilen — lautlos |
> | Die Eimergrenze ändern (etwa auf Viertelstunden) | `Verengungsgrenzen` rechnet auf ganzen Stunden. Die Randstunden-Regel wäre falsch, und die gezählte Menge keine Unterschranke mehr |
> | `rollup_lauf` anders schreiben (etwa `fenster_bis` einschließend) | Der Wasserstand verschöbe sich um eine Stunde nach oben, und die Verengung ginge über gerechnete Daten hinaus |
> | Den Index aus `V11` entfernen | Die Vorabfrage fällt für die dünnen Mandanten um Faktor 38 bis 102 zurück (M104) |
>
> Der Schutz dagegen ist kein Kommentar, sondern `FensterverengungDbIT`: Er stellt verengt und
> unverengt über alle zehn Mandanten gegeneinander. Wer eine dieser Änderungen macht, sieht ihn rot.

Alle Zahlen der Prüfung stehen in
[`messungen-liste-verengung.md`](messungen-liste-verengung.md) (M99 bis M104), die Kosten für die
beiden Läufe in §9c, die Bewertung und die Abschlussmessung in
[`nachrichtenliste.md`](nachrichtenliste.md) §5c und §5d.

Drei Dinge aus dieser Prüfung gehören hierher, weil sie den Rollup betreffen und sonst untergehen:

1. **Der Rollup ist für `MessageLastUpdate` nachweislich lückenlos.** `SUM(anzahl)` über die
   Stundenebene trifft `COUNT(*)` über `Message` auf die Zeile genau (**3.341.519**), und
   `MessageLastUpdate` ist im Bestand nirgends `NULL`. Die Stundeneimer sind an drei Stichproben
   gegen die Quelle geprüft und dreimal gleich. Wer die Tabelle als Zählwerk benutzt, darf sich
   darauf verlassen — **solange die Eimergrenzen halboffen gelesen werden**: `[stunde, stunde+1h)`.
   Ein Fenster, dessen Grenzen nicht auf einer vollen Stunde liegen, hat zwei **angebrochene**
   Randstunden, und wer sie voll mitzählt, bekommt eine Überschätzung.

2. **Der fehlende Sekundärindex ist der Grund, warum die Prüfung gescheitert ist.** §2 hält
   ausdrücklich fest, dass `message_rollup` keinen Sekundärindex trägt. Für das Dashboard ist das
   richtig — es fragt immer über einen Zeitbereich, und `stunde` führt den Primärschlüssel. Für eine
   Frage der Form „hat *dieser Mandant* im Fenster überhaupt 51 Zeilen?" ist es teuer: Sie muss alle
   **21.274** Rollupzeilen des 30‑Tage-Fensters lesen, auch wenn der Mandant nur **47** davon
   besitzt. Das kostet 13 bis 18 ms und trifft ausgerechnet die dünnen Mandanten.

   **Ein Index `(process_id, stunde)` ist gemessen** (M104, an einer Probetabelle nach dem Vorbild
   von M89 — angelegt, befüllt, gemessen, gelöscht; die Löschung ist nachgewiesen, die echte
   Tabelle ist unberührt). Er senkt die 30‑Tage‑Stufe der Vorabfrage um **Faktor 38 bis 102**
   (`NXHBE` 97,756 → 0,962 ms), weil der Plan sich umdreht: von den wenigen Prozessen des Mandanten
   in den Rollup, statt alle Rollupzeilen zu lesen. Er kostet **16,6 MiB** neben 21,6 MiB Daten und
   **1,3 s** Aufbauzeit. **Was er den beiden Läufen kostet, ist inzwischen gemessen** — §9c: Der
   stündliche Delta‑Lauf zahlt nichts, der nächtliche Volllauf +52,8 % im Schreibpfad, die größte
   Scheibe 3,669 s gegen die vor der Messung gesetzte Schranke von 5 s. **Angelegt ist er seit dem
   30.08.2026** (`V11__message_rollup_prozess_index.sql`, §2). Offener Punkt 73 trägt seinen
   Erledigt-Vermerk in [`nachrichtenliste.md`](nachrichtenliste.md) §9; **76 bleibt offen** — die
   24‑Stunden‑Stufe der Vorabfrage rührt der Index nicht an.

3. **Der Mandant steht nicht in der Zeile, und das hat einen Preis, den 10a nicht sehen konnte.**
   Entscheidung E‑a hält Mandant, Partner und Richtung bewusst aus dem Schlüssel heraus; der Mandant
   wird erst beim Lesen über `Process → Project → ProjectMandant` angebunden. Für das Dashboard ist
   das eine Verbindung je Anfrage. Für die Verengung ist es der Hauptkostenblock: Entweder man
   löst die Prozessliste vorab auf und schickt sie als Literalliste mit — bei `NEXANS` sind das
   **733** Kennungen und ein Statement von **19.285 Zeichen**, dessen bloßes Parsen 1,5 ms kostet —,
   oder man hängt eine `EXISTS`-Kette an jede Rollupzeile, was bei weitem Bereich das Sechs- bis
   Achtfache kostet. **Die Mandantenkette muss `EXISTS` sein und darf kein `JOIN` sein:**
   `ProjectMandant` ist n:m, ein `JOIN` vervielfachte Zeilen und damit die Summe.

**Und eine vierte Sache, die erst der Bau gezeigt hat.** Die `EXISTS`-Fassung ist nicht nur die
sichere (n:m), sondern seit `V11` auch die schnellere: Mit dem Index macht die **Literal**fassung
die engen Stufen sogar *teurer* (`NEXANS` 2,204 → 7,813 ms, M104). **Index und Abfragefassung sind
eine Entscheidung, nicht zwei** — wer die eine ändert, misst die andere neu.

---

## 9c. Was der Index den beiden Läufen kostet — M105 *(30.08.2026)*

Offene Punkte 73 und 76 halten fest, dass der Index `(process_id, stunde)` dem **Leser** um Faktor
bis 102 hilft und was er an Platz kostet — **nicht** aber, was er dem **Schreiber** kostet. Das ist
hier nachgeholt, vor der Migration und mit einem Tor.

> ### ⚠️ Die benannte Ausnahme vom Satz „es wird nur gelesen"
>
> M105 legt **zwei** Probetabellen in `overlord_monitor` an — eine Stunden- und eine Tagesebene —,
> befüllt sie aus den echten Tabellen, misst gegen sie und löscht sie wieder. Bauform **M89** und
> **M104**. Die Löschung ist unten nachgewiesen. Die echten Tabellen sind nur **gelesen** worden;
> auf `GlassfishDB` ist in dieser Runde überhaupt nicht zugegriffen worden.
>
> **Nummernvergabe:** `grep -rnoE '\bM10[5-9]\b'` → **null Treffer**; Gegenprobe auf `M10[0-4]` →
> **61** Treffer. **M105 ist hier vergeben.**

### Was überhaupt teurer werden kann

**Die Aggregation nicht.** Die Bezugswerte **84,999 ms** (dichteste Stunde) und **2.575,006 ms**
(größte Monatsscheibe) aus §9 messen die Abfrage über `GlassfishDB.Message`. Ein Index auf
`message_rollup` kann sie nicht berühren — sie liest diese Tabelle nicht. Teurer werden kann nur der
**Schreibpfad**, und der besteht aus vier Schritten (`RollupSchreibRepository.ersetzeFenster`):

| | Schritt | Warum der Index ihn treffen könnte |
|---|---|---|
| 1 | `DELETE` auf der Stundenebene | jede gelöschte Zeile muss aus dem Index entfernt werden |
| 2 | Einfügen auf der Stundenebene | jede neue Zeile muss in den Index |
| 3 | `DELETE` auf der Tagesebene | gar nicht — andere Tabelle |
| 4 | Einfügen auf der Tagesebene | **es liest die Stundenebene.** Ein neuer Index kann seinen Plan kippen — der eigentliche Verdacht |

### Die Zahlen

Aufwärmlauf, dann beste von fünf, dieselbe Probetabelle vor und nach `ADD INDEX`.

| Schritt | **ohne Index** | **mit Index** | |
|---|---:|---:|---|
| **Delta-Lauf, dichteste Stunde** `2025-12-07 17:00` — 14 Stunden-, 49 Tageszeilen | | | |
| 1 `DELETE` Stunden | 2,236 ms | 2,130 ms | |
| 2 `INSERT` Stunden | 2,387 ms | 2,318 ms | |
| 3 `DELETE` Tage | 2,211 ms | 2,026 ms | |
| 4 `INSERT` Tage | 2,705 ms | 2,523 ms | |
| **zusammen** | **9,539 ms** | **8,997 ms** | **unverändert** |
| **Monatsscheibe `2025-07`** — 26.365 Stunden-, 9.201 Tageszeilen | | | |
| 1 `DELETE` Stunden | 157,586 ms | **287,965 ms** | +82,7 % |
| 2 `INSERT` Stunden | 358,759 ms | **489,587 ms** | +36,5 % |
| 3 `DELETE` Tage | 56,127 ms | 59,116 ms | +5,3 % |
| 4 `INSERT` Tage | 255,415 ms | 257,071 ms | **+0,6 %** |
| **zusammen** | **827,887 ms** | **1.093,739 ms** | **+32,1 %** |
| **Volllauf, Schreibpfad über alle 23 Scheiben** | **7,737 s** | **11,818 s** | +52,8 % |
| Index selbst | — | 16,6 MiB, 1,084 s Aufbau | |

**Der Verdacht gegen Schritt 4 bestätigt sich nicht.** Die Tagesableitung bleibt bei `range` über
`PRIMARY` mit `key_len 5` — der Optimierer greift den neuen Index nicht auf, obwohl er da ist. Das
war die eine Stelle, an der ein Index auf der Stundenebene die Tagesebene hätte mitreißen können.

**Der Delta-Lauf zahlt nichts.** Er berührt vierzehn Rollupzeilen; der Unterschied liegt unter dem
Rauschen. Er ist der Lauf, der **stündlich** läuft — der teure ist der nächtliche.

### Das Tor

Eine Scheibe = Aggregation (unverändert) + Schreibpfad:

| | ohne Index | mit Index |
|---|---:|---:|
| Aggregation der größten Scheibe | 2,575 s | 2,575 s |
| Schreibpfad derselben Scheibe | 0,828 s | 1,094 s |
| **zusammen** | **3,403 s** | **3,669 s** |

**Die Schranke des Auftrags ist 5 s, und sie wird nicht erreicht.** Zur Zeitgrenze des Lese-Pools
(10 s) bleibt Faktor **2,7** statt vorher 3,9. **Das Tor öffnet: bauen.**

> **Belegvermerk (Regel L10).** *Gemessen ist:* der Schreibpfad als SQL, mit `INSERT … SELECT` aus
> der echten in die Probetabelle. *Der gebaute Weg ist ein anderer:* Er liest in die JVM und schreibt
> in Stapeln zu 1.000 Zeilen zurück, zahlt also je Stapel eine Netzwerkrunde. **Die Lücke:** Die
> absoluten Schreibzeiten des gebauten Wegs liegen höher als die hier gemessenen — der Quervergleich
> sagt um Faktor rund 1,4 (der gebaute Volllauf kostet 45,772 s, davon rund 34,8 s Aggregation bei
> 10,4 µs je Zeile, also rund 11 s Schreiben gegen die hier gemessenen 7,7 s). **Übertragbar ist
> deshalb das Verhältnis, nicht der Absolutwert.** Rechnet man den Aufschlag von 32,1 % auf einen um
> Faktor 1,4 größeren Schreibanteil, liegt die größte Scheibe bei rund **3,9 s** — weiterhin unter
> der Schranke, aber mit weniger Luft, als die Tabelle oben nahelegt.
>
> **Was daraus folgt und hier steht, damit es nicht untergeht:** Wächst der Bestand, wächst der
> Schreibanteil linear mit, die Aggregation ebenso. Die Schranke von 5 s ist bei **rund einem
> Drittel** mehr Bestand erreicht. Der nächtliche Volllauf ist damit die Stelle, die als Erste
> anschlägt — nicht die Liste.

### Der Löschnachweis

| Schritt | Ergebnis |
|---|---|
| `@@global.read_only` zu Beginn und am Ende | **`1`** |
| `DROP TABLE` beider Probetabellen | ausgeführt |
| **nachher** — Tabellen mit `probe` im Namen | **`0`** |
| **nachher** — Tabellen in `overlord_monitor` | die **zwölf**, die vorher bestanden |
| `message_rollup` | **335.610** Zeilen, `SUM(anzahl)` **3.341.519** |
| `message_rollup_tag` | **123.049** Zeilen, `SUM(anzahl)` **3.341.519** |
| Sekundärindizes auf `message_rollup` **vor der Migration** | **`0`** |

---

## 9d. Was die Monatsebene bringt — M107 *(31.08.2026)*

§9a endet mit einem Tor, das ausgelöst hat: Die Zwölf‑Monats‑Ansicht kostet über die Tagesebene
**767,128 ms** (Verlauf) und **908,539 ms** (Verteilung) beim größten Mandanten, bei einer vor der
Messung gesetzten Schwelle von 700 ms. Entscheidung des Auftraggebers vom 27.08.2026:
**Monatsebene bauen.** `V12__message_rollup_monat.sql`, gebaut am 31.08.2026.

**Nummernvergabe.** `grep -rnoE '\bM10[7-9]\b|\bM1[1-9][0-9]\b' docs/ scripts/ *.md` → **null
Treffer**; Gegenprobe auf `M10[0-6]` → Treffer. **M107 ist hier vergeben.**

### Die Bauform

Übernommen aus §9a, Punkt für Punkt: beide Ebenen **in derselben Sitzung**, abwechselnd gefahren —
die 767,128 ms stammen aus einer anderen Sitzung an einem anderen Tag. Aufwärmlauf, dann beste von
fünf. Fenstergrenzen als Literal (Z1). Laufzeiten aus `information_schema.PROFILING`,
`anzahl_laeufe` muss **6** sein. Sitzungen: `scripts/messung-monatsebene/m107-monat-nexans.sql`
und `-suttons.sql`.

**Ein Fall ist neu, und er ist eine Gegenprobe.** Auf der Monatsebene **fällt der Eimerausdruck
weg** — `monat` *ist* der Eimer, `GROUP BY DATE_FORMAT(…)` wird zu `GROUP BY r.monat`. Damit die
Ersparnis der **Zeilenzahl** zugeschrieben werden kann und nicht dem weggefallenen `DATE_FORMAT`,
ist der Verlauf **zweimal** gemessen: einmal mit `r.monat`, einmal mit
`DATE_FORMAT(r.monat, '%Y-%m-01')`.

### Die Vorbedingung: alle drei Ebenen sagen dasselbe

Erste Fachabfrage beider Sitzungen, vor jedem `SET profiling`.

| | `NEXANS` | `SUTTONS` |
|---|---:|---:|
| Eimer, Stundenebene | 12 | 12 |
| Eimer, Tagesebene | 12 | 12 |
| Eimer, **Monatsebene** | **12** | **12** |
| `SUM(anzahl)`, alle drei Ebenen | **2.308.005** | **196.536** |

**Zeile für Zeile gleich, Monat für Monat** — und die Eimerzahl steht daneben, weil ein Eimer, den
eine Ebene *nicht* hätte, im `JOIN` der Gleichheitsprobe lautlos verschwände.

### Was jede Ebene liest

Der Bereich ist **mandantenunabhängig**: Der Range‑Zugriff liest ihn ganz, der Mandantenfilter
wirkt erst danach (derselbe Befund wie in M94).

| | Zeilen im Zwölf‑Monats‑Fenster | gegen die Tagesebene |
|---|---:|---:|
| `message_rollup` | 280.186 | — |
| `message_rollup_tag` | 100.270 | — |
| **`message_rollup_monat`** | **9.649** | **Faktor 10,39** |

**Die 9.649 sind Zeichen für Zeichen M87‑5, Jahresscheibe 2025, Variante 4** — dieselbe Herleitung,
mit der §9a die 100.270 der Tagesebene belegt.

### Das Ergebnis

| Ansicht | Mandant | Tagesebene (§9a) | Tagesebene (M107) | **Monatsebene** | Faktor |
|---|---|---:|---:|---:|---:|
| **Verlauf, 12 Monate / Monat** | **NEXANS** | 767,128 ms | 743,461 ms | **65,350 ms** | **11,4** |
| **Verteilung, 12 Monate** | **NEXANS** | 908,539 ms | 892,025 ms | **88,672 ms** | **10,1** |
| Verlauf, 12 Monate / Monat | SUTTONS | 425,689 ms | 428,219 ms | **38,745 ms** | 11,05 |
| Verteilung, 12 Monate | SUTTONS | 431,657 ms | 434,417 ms | **39,420 ms** | 11,02 |

**Die vierte Spalte ist der Grund, der Messung zu trauen.** Sie ist die Tagesebene, gefahren in
*dieser* Sitzung — sie liegt zwischen **0,6 % und 3,1 %** neben den Werten aus §9a. Die beiden
Sitzungen sind also vergleichbar, und der Faktor ist kein Artefakt des Messtages.

**Der Eimerausdruck ist nicht die Ursache der Ersparnis:**

| Verlauf über die Monatsebene | `GROUP BY r.monat` | `GROUP BY DATE_FORMAT(…)` | Aufschlag |
|---|---:|---:|---:|
| NEXANS | 65,350 ms | 73,520 ms | +8,170 ms (+12,5 %) |
| SUTTONS | 38,745 ms | 39,739 ms | +0,994 ms (+2,6 %) |

Selbst mit dem Ausdruck bleibt der Faktor bei **10,1** beziehungsweise **10,8**. **Die Ersparnis
kommt aus den gelesenen Zeilen und aus nichts sonst** — genau die Aussage, die M94 über einen
Mengenbereich von Faktor 302 belegt hat.

### Die Pläne (Regel L15)

Zeile für Zeile dieselbe Form wie eine Ebene tiefer: `range` über `PRIMARY` mit
`Using where; Using temporary; Using filesort`, danach die Mandantenkette als drei `eq_ref`.

```
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key     | key_len | ref                           | rows | Extra                                        |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY | 3       | NULL                          | 6132 | Using where; Using temporary; Using filesort |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1    | Using where                                  |
|    1 | SIMPLE      | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1    | Using where; Using index                     |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1    | Using index                                  |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
```

`key_len 3` statt der 5 auf Stunden- und Tagesebene: Der Primärschlüssel steigt über `monat` ein,
und ein `DATE` ist drei Byte. **Kein Sekundärindex im Spiel** — die Tabelle trägt keinen (0 Byte
Indexanteil nach dem Rückwärtslauf), und der Plan braucht keinen.

### ⚠️ Das Tor ist offen — und die vorregistrierte Erwartung trifft trotzdem nicht zu

**Das Tor:** Schwelle 700 ms, gemessen **88,672 ms** im teuersten Fall. **Faktor 7,9 Luft** statt
einer Überschreitung. Offener Punkt 55 ist damit erledigt.

**Und die Erwartung des Auftrags stimmt nicht.** Sie lautete *„einstelliger bis niedriger
zweistelliger Millisekundenbereich — rund 6.500 gelesene Monatszeilen statt 100.270 Tageszeilen"*.
Beide Zahlen sind falsch, und beide Fehler sind **Rechenfehler in der Erwartung** und keine Befunde
über den Bau:

| | Auftrag | gemessen | woher der Unterschied kommt |
|---|---:|---:|---|
| gelesene Monatszeilen | rund 6.500 | **9.649** | 6.500 entsteht, wenn man die **11.957 Zeilen des Gesamtbestands** gleichmäßig auf dessen 22 Monate verteilt und mit zwölf multipliziert (11.957 × 12/22 ≈ 6.522). Auf diesem Bestand ist diese Mittelung falsch: 2024 trägt nur drei Monate (2.303 Zeilen), 2026 ist praktisch leer (5 Zeilen), **2025 allein trägt 9.649** |
| Laufzeit | einstellig bis niedrig zweistellig | **65,4 / 88,7 ms** | 9.649 Zeilen × die **5,3 bis 11,5 µs je gelesener Rollupzeile** aus M94 ergeben **51 bis 111 ms**. Die Erwartung ist um Faktor 1.000 verrechnet |

> ### Und das ist die eigentliche Nachricht dieser Messung: M94s Linearität hält über die dritte Ebene
>
> | | µs je gelesener Rollupzeile |
> |---|---|
> | Verlauf, NEXANS | Tag **7,41** · **Monat 6,77** |
> | Verteilung, NEXANS | Tag **8,90** · **Monat 9,19** |
> | Verlauf, SUTTONS | Tag 4,27 · **Monat 4,01** |
> | Verteilung, SUTTONS | Tag 4,33 · **Monat 4,09** |
>
> **Die Kosten je Zeile ändern sich nicht, wenn man eine Ebene höher geht.** Bei `NEXANS` liegen
> sie mitten im M94‑Band von 5,3 bis 11,5 µs, bei `SUTTONS` knapp darunter (M94 maß dort 5,32 bis
> 6,43 µs, heute 4,0 bis 4,3 µs — dieselbe Größenordnung, andere Sitzung). Die Ansicht wird
> schneller, **weil sie weniger liest**, und um genau den Faktor, um den sie weniger liest: 10,39
> gelesene Zeilen gegen 10,1 bis 11,4 Laufzeit.
>
> **Das ist zugleich die Warnung für die nächste Ebene, falls sie je erwogen wird:** Über der
> Monatsebene gibt es nichts mehr zu verdichten, was eine Zwölf‑Monats‑Ansicht noch bräuchte — sie
> liest zwölf Eimer je Prozess und Status. Eine Jahresebene spart der Zwölf‑Monats‑Ansicht nichts.

> **Belegvermerk** (Regel L10). *Gemessen ist:* vier Fälle über zwei Mandanten, jeder mit einem
> Aufwärmlauf und fünf Läufen, **warm**, in einer Sitzung neben der Tagesebene. *Behauptet wird:*
> die Zwölf‑Monats‑Ansicht liegt mit der Monatsebene unter dem Tor von 700 ms. **Die Lücke:** Alle
> Werte sind Warmwerte; `FLUSH TABLES` steht `monitor_read` nicht zu (dieselbe Lücke wie in §9a).
> Der Kaltfaktor aus M44 (bis 9,66) auf 88,672 ms angewandt ergibt rund **0,86 s** — das ist eine
> Übertragung und keine Messung. Sie ist trotzdem der Vergleich, auf den es ankommt: **Dieselbe
> Rechnung ergab für die Tagesebene rund 7,4 s**, also nahe an der Zeitgrenze des Lese‑Pools von
> zehn Sekunden. Aus einem Fall, der kalt beinahe abbricht, wird einer mit Faktor elf Luft.

> ### Was M107 **nicht** sagt
>
> - **Nichts über die anderen acht Mandanten.** Gemessen sind `NEXANS` (der größte) und `SUTTONS`.
> - **Nichts über engere Fenster.** Dreißig Tage lesen auf der Monatsebene ein bis zwei Eimer und
>   wären eine andere Frage; dort liest die Ansicht ohnehin die Tagesebene (45,990 ms, §9a).
> - **Nichts über den kalten Fall.** Siehe Belegvermerk.
> - **Nichts über einen Endpunkt.** Es gibt keinen; das Dashboard ist 10b.

---

## 10. Tests

### Ohne Datenbank

| Test | Was er sichert |
|---|---|
| `RollupFensterTest` (39) | Rückgriff, Ausdehnung auf ganze Stunden, leerer Wasserstand, zurückspringende Uhr, Scheibenzerlegung (lückenlos, ohne Überlapp, Summe der Eimer gleich dem Fenster), die Invarianten des Fensters. **Dazu die berührten Kalendertage und -monate**: `bis` ist ausschließend, ein Rückgriff über Mitternacht bzw. über den Monatswechsel berührt zwei Eimer, der Rechenbereich umfasst ganze Tage bzw. ganze Monate, und ein Monatsbereich, dessen Grenze nicht der Monatserste ist, wird abgewiesen |
| `RollupUhrenTest` (4) | **Die Zuordnung der beiden Uhren.** Die Uhren stehen im Test acht Monate auseinander, damit ein vertauschter Aufruf auffällt — in Produktion wäre er unsichtbar |
| `RollupStatementsTest` (6) | Das **gerenderte** Statement, Zeichen für Zeichen. Regel L7 verlangt die Messung *der* Abfrage, nicht einer ähnlichen |
| `RollupTagStatementsTest` (7) | **Die sechs Statements eines Laufs**, gerendert: die Reihenfolge (Stundenebene vor Tagesebene vor Monatsebene — genau dieser Test fällt, wenn jemand die Ableitung umdreht), ausschließlich `overlord_monitor`, **ganze Tage statt des Fensters**, `DATE(stunde)` auch im `GROUP BY`, kein Hochzählen, das Fenster über Mitternacht, das leere Fenster |
| `RollupMonatStatementsTest` (7) | Dasselbe eine Ebene höher: **aus `message_rollup_tag` und nie aus `GlassfishDB`**, ganze Monate statt des Fensters, `DATE(DATE_FORMAT(tag, '%Y-%m-01'))` auch im `GROUP BY`, kein Hochzählen, der Monatswechsel, die ausschließende obere Grenze, das leere Fenster |
| `RollupTransaktionsgrenzenTest` (2) | **Wo die Transaktionsklammern sitzen und wo ausdrücklich keine sitzt.** `ersetzeFenster` und `rechneAbgeleiteteEbenenNeu` tragen `@Transactional`, die beiden Ebenenmethoden nicht. Über Reflexion und nicht über Verhalten — der Datenbanktest kann eine **fehlende** Klammer nicht finden, weil er seine eigene Transaktion mitbringt |
| `PaketstrukturTest` (+2) | Die namentliche Ausnahme ist eng und nicht leer; nichts in `rollup`, das `jooq.glassfish` anfasst, ruft eine schreibende jOOQ-Methode auf |

### Mit Datenbank (`RollupDbIT`, `@Tag("db")`, 25 Fälle)

| Test | Was er sichert |
|---|---|
| **Idempotenz** | Derselbe Lauf zweimal über dasselbe Fenster ergibt **zeilengleich** dasselbe. **Der wichtigste Test dieses Schritts** — er ist der einzige Wächter über Löschen-und-Neuschreiben |
| **Überlapp** | Zwei aufeinanderfolgende Delta-Läufe verdoppeln den Überlapp nicht: `SUM(anzahl)` über den gesamten berührten Bereich trifft die direkte Zeilenzahl aus `Message` |
| **Statuswechsel im Eimer** | Eine Statuszeile, die die Quelle nicht mehr hergibt, bleibt nicht stehen |
| **Summenprobe** | Über ein abgeschlossenes Tagesfenster gilt `SUM(anzahl)` = direkte Zeilenzahl aus `Message` |
| **Delta gegen Voll** | Über dasselbe Fenster liefern beide zeilengleich dasselbe |
| **Wasserstand** | Ein abgebrochener Lauf hebt ihn nicht — und ein abgeschlossener mit `fehler` ebenfalls nicht |
| **Leere Stunde** | Eine Stunde ohne Verkehr erzeugt keine Zeile und hebt den Wasserstand trotzdem |
| **Anschluss** | Das Fenster des nächsten Delta-Laufs baut auf dem Wasserstand des vorigen auf, mit genau einem Eimer Rückgriff |
| **Tagesebene: Summe der Stundenebene** | Zeile für Zeile — die materialisierte Tagesebene gegen die aus der Stundenebene gerechnete, für beide berührten Tage |
| **Tagesebene: ganzer Tag** | Ein Zwei-Stunden-Fenster rechnet den **ganzen** Tageseimer. **Der Test, an dem es sonst still schiefginge** |
| **Tagesebene: Statuswechsel** | Eine Tageszeile, die die Stundenebene nicht mehr hergibt, bleibt nicht stehen |
| **Tagesebene: Idempotenz** | Derselbe Lauf zweimal ergibt zeilengleich dasselbe |
| **Tagesebene: Mitternacht** | Ein Fenster über die Tagesgrenze schreibt **beide** Tage richtig |
| **Summenprobe über beide Ebenen** | Beide tragen dieselbe Zahl wie `Message` — und die Tagesebene weniger Zeilen als die Stundenebene |
| **Monatsebene: Summe der Tagesebene** | Zeile für Zeile — die materialisierte Monatsebene gegen die aus der Tagesebene gerechnete |
| **Monatsebene: ganzer Monat** | Ein Zwei-Stunden-Fenster rechnet den **ganzen** Monatseimer — dieselbe Falle wie eine Ebene tiefer |
| **Monatsebene: Statuswechsel** | Eine Monatszeile, die die Tagesebene nicht mehr hergibt, bleibt nicht stehen |
| **Monatsebene: Idempotenz** | Derselbe Lauf zweimal ergibt zeilengleich dasselbe |
| **Monatsebene: Monatswechsel** | Ein Fenster über die Monatsgrenze schreibt **beide** Monate richtig, und zwar jeden ganz |
| **Summenprobe über alle drei Ebenen** | Alle drei tragen dieselbe Zahl wie `Message`, und jede Ebene weniger Zeilen als die vorige |
| **Abbruch** | Wird die Transaktion von außen zurückgenommen, behält **keine** der drei Ebenen eine Zeile. Die Zähne stecken im ersten Teil: Innerhalb der Transaktion muss die Zeile in *jeder* Ebene sichtbar sein — sonst prüfte der zweite Teil nur, dass nichts da ist, was nie da war |
| **Bestandsanfang** | `MIN(MessageLastUpdate)` ist `2024-10-01 02:00:28` — weicht er ab, ist die Testkopie neu befüllt und die Zahlen dieser Datei sind neu zu erheben |
| **Punkt 54: gemeldet und stehen gelassen** | Der Test legt in **jeder** der drei Ebenen eine Zeile vor dem Bestandsanfang an und verlangt vom Volllauf zweierlei: Er *meldet* sie, und er *lässt sie stehen*. Beides gehört zusammen — eine Meldung, die den Zustand anschließend beseitigt, wäre genau das Löschen, das der Auftraggeber abgelehnt hat. Geprüft wird die `WARN`-Zeile selbst, über einen Logback-Anhang |
| **Punkt 54: die Gegenprobe** | Ohne eine eingefrorene Zeile erscheint **keine** Warnung — sonst zeigte die Meldung oben nur, dass der Lauf immer warnt. Zugleich der Nachweis, dass der **abgerundete** Vergleich nötig ist |
| **Punkt 54: die eingegrenzte Summenprobe** | Über den überlappenden Bereich ändert die eingefrorene Zeile **nichts**, über die ganze Tabelle genau ihre `anzahl`. Der Test nennt dabei **keine Zahl aus dem Bestand** (Regel T2) — er prüft nur die Differenz und hängt damit an keinem Rollup-Stand |

> ### Kein Mandantentrennungstest, und das ist Absicht
>
> **Schritt 10a hat keine Fläche, die einen Mandanten kennt.** `message_rollup` trägt nach E‑a keine
> Mandantenspalte, es gibt keinen Endpunkt, und der Job liest ausdrücklich über alle Mandanten. Die
> Mandantentrennung entsteht in **10b** beim Join über `ProjectMandant` — dort ist der
> Isolationstest Pflicht (Regel M4). **Er fehlt hier nicht, es gibt ihn hier nicht.**

**Was die Tests auf der Testkopie hinterlassen.** Sie schreiben ausschließlich in
`overlord_monitor`; `GlassfishDB` wird nur gelesen (Regel S1). Ihre `rollup_lauf`-Zeilen räumen sie
**namentlich** wieder ab — nur die Kennungen, die sie selbst bekommen haben. Ein Aufräumen über ein
Zeitfenster oder über die Laufart löschte fremde Zeilen. Die geschriebenen
`message_rollup`-Zeilen bleiben stehen, und das ist richtig: Sie sind korrekt gerechnet.

---

## 11. Die zweite benannte Ausnahme von Regel M2

`rollup/RollupLeseRepository` bekommt **keinen `MandantContext`** — und das bricht eine
ArchUnit-Regel, die [`mandantentrennung.md`](mandantentrennung.md) §4 durchsetzt.

**Der Rollup-Job hat keinen Mandanten.** Er liest bewusst über alle Mandanten, weil
`message_rollup` nach E‑a keinen kennt.

**Ein `MandantContext.alle()` wäre eine Lüge im Typsystem** und würde die Regel entwerten, deren
einziger Zweck es ist, dass so etwas nicht existiert. Stattdessen steht in `PaketstrukturTest` eine
**namentliche** Ausnahme: `ROLLUP_AUSNAHME = List.of("RollupLeseRepository")`. Sie nennt die Klasse
beim Namen und gilt für keine andere — **auch nicht für eine zweite im selben Paket.** Die fiele
weiter durch, bis jemand sie einträgt und begründet.

**Zwei Tests halten die Ausnahme fest, von beiden Seiten:**

- Sie ist **nicht leer** (sonst prüft sie nichts mehr und gehört gelöscht) und **nicht breiter** als
  die Liste, die sie benennt.
- Sie ist **ehrlich**: Die ausgenommene Klasse trägt den `MandantContext` in keiner Signatur. Ein
  Schein-Kontext — ein Parameter, der entgegengenommen und nicht verwendet wird — wäre schlimmer als
  gar keiner, weil er von außen wie Mandantentrennung aussieht.

**Warum Lesen und Schreiben auf zwei Klassen verteilt sind.** Der Katalog aus Schritt 9b hält beide
`DSLContext` in *einer* Repository-Klasse, und das ist dort richtig. Hier nicht: Lägen beide Hälften
in einer Klasse, fiele **auch der Schreibpfad** unter die Ausnahme, und sie wäre breiter als ihr
Grund. `RollupSchreibRepository` fasst `jooq.glassfish` gar nicht erst an.

**Was die Ausnahme nicht aufweicht:** `message_rollup` selbst enthält keine Mandantenangabe. Die
Trennung entsteht in 10b beim Join über `ProjectMandant` — unverändert im Statement, nicht
nachgelagert.

> **Eine Ungenauigkeit im Auftrag, gemeldet und nicht stillschweigend übergangen.** Er nennt diese
> Ausnahme „die dritte benannte Ausnahme des Projekts … wie die beiden Endpunkt-Ausnahmen dort".
> Das trifft seit dem 20.08.2026 nicht mehr: [`mandantentrennung.md`](mandantentrennung.md) §3
> führt **drei** Endpunkt-Ausnahmen von Regel **M1**, und diese hier ist die **zweite** Ausnahme von
> Regel **M2** (die erste ist `@OhneMandantenkontext`). Sie steht deshalb in §4 und nicht in §3 —
> die Zählung des Auftrags ist überholt, die Sache ist es nicht.

---

## 12. Regelbezug

| Regel | Stand |
|---|---|
| **L2** Keine Live-Aggregation über `Message` | **erfüllt** — das ist der Zweck dieses Schritts. 10b liest ausschließlich aus `message_rollup` |
| **L6** Der Rollup-Job läuft gedrosselt | **erfüllt beim Volllauf** (Pause zwischen den Scheiben), **beim Delta-Lauf Zeremonie** und als solche benannt (§8) |
| **L7** Jede neue Abfrage gemessen | **erfüllt** — §9 für die Aggregation, **§9a für Stunden- und Tagesebene**, **§9c für den Index** und **§9d (M107) für die Monatsebene**, je `EXPLAIN` und Laufzeit gegen das gerenderte Statement |
| **L9** Durchlauf ohne Zeitfenster nur begründet | **erfüllt** — nur `MIN(MessageLastUpdate)`, begründet in §7, und es ist eine Indexspitze ohne Tabellenzugriff |
| **L10** Belegvermerk | **erfüllt** — fünf Vermerke: Nachlauffenster (§3), Drosselung (§8), Volllauf gegen M89 (§9), das Tor (§9a), die Monatsebene (§9d) |
| **S1** Kein Schreibzugriff auf `GlassfishDB` | **erfüllt** — der Job liest über `glassfishDsl` mit `ReadOnlyExecuteListener`; eine ArchUnit-Regel verbietet zusätzlich schreibende jOOQ-Aufrufe in den Rollup-Klassen, die das Quellschema anfassen |
| **M2** `MandantContext` erster Pflichtparameter | **namentliche Ausnahme**, §11 |
| **M4** Isolationstest je Endpunkt | **gegenstandslos** — 10a hat keinen Endpunkt, §10 |
| **Z1** Kein `now()` | **erfüllt** — beide Zeitpunkte aus `Clock.instant()`, ArchUnit prüft es |
| **A5** Protokollzeit aus `systemClock` | **erfüllt** — §4 |
| **Q4** Nichts raten | **eingehalten, indem das Geratene benannt ist**: Nachtlauf-Uhrzeit und Drosselung stehen als ungemessen im Code, in `application.yml` und unter den offenen Punkten |
| **S2** Flyway nur für `overlord_monitor` | **erfüllt** — `V9__message_rollup.sql`, `V10__message_rollup_tag.sql`, `V11__message_rollup_prozess_index.sql`, `V12__message_rollup_monat.sql` |

---

## 13. Offene Punkte

### Zur Tagesebene (Stand 27.08.2026, Schritt 10b‑1 Teil C)

> **Wo der Vermerk zu Punkt 55 steht und warum hier.** Der Punkt ist in
> [`messungen-schritt10b.md`](messungen-schritt10b.md) vergeben; diese Datei ist für Schritt 10b‑1
> ausdrücklich **nicht anzufassen**. Der Vermerk steht deshalb dort, wo die Sache hingehört.

- ~~**Offener Punkt 55 ist zur Hälfte erledigt und bleibt zur Hälfte offen** *(27.08.2026)*.
  **Erledigt:** Die materialisierte **Tagesebene** ist gebaut, rückwärts gefüllt und gemessen (§2,
  §5, §6a, §9a). Sie senkt die Zwölf‑Monats‑Ansicht um Faktor 2,7 bis 3,6 und die
  30‑Tage‑Ansicht unter 100 ms. **Offen:** Die Zwölf‑Monats‑Ansicht liegt beim größten Mandanten
  weiterhin bei **767,128 ms** (Verlauf) und **908,539 ms** (Verteilung) und damit über dem Tor von
  700 ms. **Die Monatsebene ist eine Vorlage an den Auftraggeber und ist hier nicht gebaut worden**
  — die Zahlen, die er dafür braucht, stehen vollständig in §9a.~~

  > ### ✔ Vollständig erledigt am 31.08.2026 (Schritt 10b‑2)
  >
  > Der Auftraggeber hat am 27.08.2026 entschieden, die Monatsebene zu bauen.
  > `V12__message_rollup_monat.sql` ist gebaut, rückwärts gefüllt (11.957 Zeilen, §6a) und gemessen
  > (**M107, §9d**):
  >
  > | Ansicht, 12 Monate, `NEXANS` | Stundenebene | Tagesebene | **Monatsebene** | Tor |
  > |---|---:|---:|---:|---:|
  > | Verlauf | 2.203,423 ms | 767,128 ms | **65,350 ms** | 700 ms |
  > | Verteilung | 2.487,394 ms | 908,539 ms | **88,672 ms** | 700 ms |
  >
  > **Faktor 7,9 Luft** statt einer Überschreitung. Der durchgestrichene Absatz bleibt lesbar, weil
  > er die Lage bis zum 31.08.2026 richtig beschreibt und weil ohne ihn nicht zu verstehen wäre,
  > warum es drei Ebenen gibt und nicht zwei.
  >
  > **Was der Punkt nicht deckt:** Der kalte Fall ist weiterhin nicht gemessen (Belegvermerk in
  > §9d), und die anderen acht Mandanten sind es auch nicht. Beides steht als eigener Punkt unten.

- **79. Der kalte Fall der Monatsebene ist nicht gemessen** *(31.08.2026)*. M107 misst warm;
  `FLUSH TABLES` steht `monitor_read` nicht zu. Der Kaltfaktor aus M44 (bis 9,66) auf 88,672 ms
  angewandt ergibt rund **0,86 s** — eine Übertragung und keine Messung. Sie ist trotzdem der
  Vergleich, auf den es ankommt: **Dieselbe Rechnung ergab für die Tagesebene rund 7,4 s**, also
  nahe an der Zehn‑Sekunden‑Grenze des Lese‑Pools.

- **81. Der Rückwärtslauf rechnet Monate über *ganze* Monate, seine Scheiben aber nur über den
  Bereich, den die Stundenebene trägt** *(31.08.2026)*. Die **erste** Scheibe beginnt bei
  `MIN(stunde)`; liegt der mitten im Monat, deckt ihr Tagesbereich nur die Tage ab diesem Tag ab,
  während der Monatseimer über den **ganzen** Monat gerechnet wird. Stünden vor `MIN(stunde)` noch
  Tageszeilen aus einem früheren Stand, zählte der Monatseimer sie mit — **die Monatsebene macht
  eine Unstimmigkeit sichtbar, die vorher nur die Tagesebene trug.** Auf der Testkopie tritt der
  Fall nicht auf (`MIN(stunde)` ist der Monatserste `2024-10-01 02:00`), und eine korrekt
  abgeleitete Tagesebene hat dort ohnehin nichts stehen. **Gerechnet, nicht gemessen** — und die
  Abhilfe wäre dieselbe wie für offenen Punkt 68: ein Erkennungsweg, der alle drei Ebenen prüft,
  nicht nur eine.

  > **Zur Hälfte erledigt am 31.08.2026.** Der Erkennungsweg über alle drei Ebenen ist gebaut
  > (§5 „Schritt 7", Punkt 68). **Er greift hier aber nicht:** Er vergleicht gegen den
  > *Bestandsanfang*, und Punkt 81 handelt von einer Unstimmigkeit **innerhalb** des Bereichs, den
  > die Stundenebene trägt. **Der Punkt bleibt damit offen** — er ist jetzt nur genauer abgegrenzt.

- **80. Die Monatsebene ist für zwei von zehn Mandanten gemessen** *(31.08.2026)*. `NEXANS` (der
  größte) und `SUTTONS`. Dass die Kosten linear an der Zahl gelesener Zeilen hängen, ist in M94
  über einen Mengenbereich von Faktor 302 belegt und in M107 über die dritte Ebene bestätigt —
  **ein Mandant, der aus der Reihe fiele, wäre trotzdem erst gefunden, wenn jemand nachsieht.**

- **67. Der Rückwärtslauf ist ein einmaliger Weg ohne Wächter.** Er lässt sich beliebig oft fahren
  und ist dabei harmlos — jede Scheibe löscht ihren Bereich, bevor sie ihn schreibt. Was ihm fehlt,
  ist die Sperre aus §8: Zwei gleichzeitige Rückwärtsläufe wären fachlich nicht falsch, aber sie
  verdoppelten die Last. Er hat sie nicht, weil er keine Zeile in `rollup_lauf` schreibt und die
  Sperre genau daran hängt. **Für einen Lauf, den ein Mensch beim Start auslöst, ist das
  vertretbar**; für einen zeitgesteuerten wäre es das nicht — und zeitgesteuert ist er nicht.

- ~~**68. Die Tagesebene kennt denselben Fall wie offener Punkt 54, und dort ist er schärfer.**
  Der Rückwärtslauf richtet sich nach `MIN(stunde)`/`MAX(stunde)` der Stundenebene; der laufende
  Job nach seinem Fenster. **Bleiben nach Punkt 54 Stundenzeilen unterhalb des Bestandsanfangs
  eingefroren stehen, frieren die zugehörigen Tageszeilen mit ein** — und zwar konsistent, weil sie
  aus jenen abgeleitet sind. Das ist die gutartige Richtung: Die beiden Ebenen laufen dabei nicht
  auseinander. **Der Erkennungsweg aus der Entscheidung zu Punkt 54 sollte die Tagesebene
  trotzdem mitprüfen**, sonst meldet er eine Abweichung nur für eine der beiden.~~

  > **✔ Erledigt am 31.08.2026 (Schritt 10b‑2 Teil C).** Die Erkennung zählt **alle drei** Ebenen
  > und nennt jede einzeln in der `WARN`-Zeile. Die Grenzen sind je Ebene verschieden und
  > absichtlich: Stundeneimer **vor der Stunde** des Bestandsanfangs, Tageseimer **vor seinem
  > Kalendertag**, Monatseimer **vor seinem Kalendermonat** — der Tag und der Monat des
  > Bestandsanfangs selbst werden von jedem Volllauf neu gerechnet und sind damit nicht
  > eingefroren. Der Datenbanktest legt in jeder Ebene eine Zeile an und prüft alle drei Zahlen.

- **69. Der Rückwärtslauf ist gegen 335.610 Stundenzeilen gemessen, nicht gegen mehr.**
  *(fortgeschrieben 31.08.2026: mit der Monatsebene sind es 27.483 ms, die Arbeit ohne Drosselung
  rund 6,5 s statt 5 s — die Rechnung unten ändert das nicht.)* 26.026 ms
  für 22 Scheiben, davon 21 s Drosselung — die Arbeit selbst rund 5 s. Wächst die Stundenebene um
  eine Zehnerpotenz, wächst er linear mit; eine einzelne Scheibe liefe dann bei rund 2,3 s und
  läge damit weiterhin weit unter der 30‑s‑Grenze des Schreib-Pools. **Gerechnet, nicht gemessen.**

### Aus Schritt 10a

49. **Das Nachlauffenster von 15 Minuten ist gegen einen Bestand gemessen, in dem der wahrscheinlichste
    Verursacher einer Wanderung nicht läuft.** `MatchInterchange` setzt `MessageStatus =
    'COMMIT_RECEIVED'` nachträglich und ist auf der Testkopie nicht sichtbar (M31‑3, Takt
    **ungedeckt**); `RUNNING` kommt null Mal vor. **In Produktion ist zu prüfen, ob die Wanderung
    dort größer ist** — die Prüfung selbst ist einfach: dieselbe Abfrage wie M86 (b), gegen den
    Produktionsbestand. Bis dahin trägt der nächtliche Volllauf.
50. **Zwei ungemessene Vorgaben, beide in `application.yml` markiert.** (a) Die Uhrzeit des
    Nachtlaufs (`03:00`) — die Verteilung von `MessageLastUpdate` über die Tagesstunde ist nicht
    erhoben. (b) Die Drosselung (`1s` je Scheibengrenze) — wie viel Last die Produktionsinstanz
    nachts verträgt, ist nicht erhoben. Beide sind konfigurierbar, damit eine Korrektur keine
    Codeänderung kostet.
51. **[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §5 beschreibt `message_rollup` weiterhin
    als „stündliche Aggregate je Mandant, Prozess, Partner, Richtung, Status".** E‑a hebt das auf,
    M89 stützt E‑a — der Widerspruch ist zugunsten von E‑a aufzulösen. **Nicht in diesem Schritt
    ausgeführt**, weil die Korrekturen aus der Messrunde eine eigene Runde sind
    ([`messungen-schritt10.md`](messungen-schritt10.md), Abschnitt „Korrekturen").
52. **Die Monatsscheibe hat Faktor 3,9 Luft zur Zeitgrenze — warm gemessen.** M44 nennt für den
    Kaltfall einen übertragenen Faktor von bis zu **9,66**; auf die größte Scheibe angewandt wären
    das rund 24,9 s und damit **über** der Grenze von 10 s. **Das ist eine Übertragung und keine
    Messung** — der Faktor stammt aus einer anderen Abfrage, und `FLUSH TABLES` steht
    `monitor_read` nicht zu. Reißt eine Scheibe in Produktion die Grenze, steht es sichtbar in
    `rollup_lauf.fehler`, und die Scheibengröße ist die Stellschraube. Sie ist heute eine Konstante
    im Code (`RollupFenster.monatsscheiben()`).
53. **Der Volllauf hält 335.610 Zeilen auf einmal im Speicher, und wie viel das ist, ist gerechnet
    und nicht gemessen.** Eine `RollupZeile` trägt neben dem Datensatzkopf einen `LocalDateTime`
    (drei Objekte) und zwei `String` — rund **220 Byte**, also etwa **70 MiB** für den Bestand der
    Testkopie; im Spitzenwert mehr, weil Scheibenliste und Gesamtliste kurz nebeneinanderstehen.
    **Hier stand zuerst „rund 30 MiB", und das war eine Schätzung ohne Rechnung.** Gemessen ist
    keine der beiden Zahlen — es wäre eine Heap-Messung wert, bevor der Bestand deutlich wächst.
    Das ist die Kehrseite der einen Transaktion und ausdrücklich gewollt. Die Stelle, an der bei
    Wachstum neu zu entscheiden wäre, ist dieselbe wie bei Punkt 52 — die Scheibengröße —, nur mit
    einer anderen Folge: Kleinere Scheiben helfen dort nichts, weil alle Scheiben zusammen in
    **eine** Transaktion gehen.

54. **Der Volllauf kann Rollup-Zeilen unterhalb von `MIN(Message.MessageLastUpdate)` nie löschen —
    und das ist der gewichtigste Punkt dieser Liste.** Die untere Grenze **beider** Laufarten kommt
    aus dem **Quellbestand**: Der Volllauf beginnt bei `MIN(Message.MessageLastUpdate)`, der
    Delta-Lauf beim Wasserstand (Rückfall ebenfalls dorthin). Gelöscht wird ausschließlich
    innerhalb dieses Fensters, und einen zweiten Löschweg auf `message_rollup` gibt es nicht.

    **Wandert `MIN` nach vorn, weil das Altsystem alte Nachrichten entfernt, bleiben die
    Rollup-Zeilen davor für immer stehen.** Jeder künftige Lauf beginnt noch später; die
    eingefrorenen Eimer sind für den Kalender unerreichbar. Danach liegt `SUM(anzahl)` über der
    Zeilenzahl von `Message` — **genau die Abweichung, die dieser Bau an anderer Stelle als „ein
    Befund und kein Rundungsfehler" bezeichnet** —, und das Dashboard aus 10b zeigte für den
    abgelaufenen Zeitraum Nachrichten, die es nicht mehr gibt.

    **Belegvermerk (Regel L10).** *Gemessen ist:* nichts — der Fall kann auf der Testkopie nicht
    eintreten, weil dort nichts entfernt wird. *Behauptet wird:* Er tritt produktiv ein. Die
    Grundlage dafür ist [`rohdaten.md`](rohdaten.md) §12 („produktiv rund 18 Monate, danach ein
    Archivsystem") — eine **Auskunft, keine Messung**; die 22 Monate aus
    [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 beschreiben nur, wie weit die *Kopie*
    zurückreicht (M92, V6). Ob und wie produktiv gelöscht wird, ist damit **nicht belegt** — dass
    der Rollup gegen diesen Fall nicht gewappnet ist, schon.

    **Der Bau ist absichtlich nicht geändert worden.** Das Fenster des Volllaufs ist eine Vorgabe
    des Auftrags („von `MIN(Message.MessageLastUpdate)` bis jetzt"), und für Vorgaben gilt dort:
    melden, nicht umbauen. **Die naheliegende Lösung ist klein:** Die untere Grenze des Volllaufs
    wird das **Minimum aus `MIN(Message.MessageLastUpdate)` und `MIN(message_rollup.stunde)`** —
    dann deckt der Löschbereich immer auch die eigene Tabellenausdehnung ab. Sie ist prüfbar: Ein
    Datenbanktest kann eine Rollup-Zeile vor den Bestandsanfang setzen und verlangen, dass der
    Volllauf sie entfernt. **Das ist eine Entscheidung für den Auftraggeber.**

    > ### ✔ Entschieden am 27.08.2026: **nicht löschen** — erkennen
    >
    > **Der Auftraggeber hat entschieden. Die naheliegende Lösung oben wird nicht gebaut:** Die
    > untere Grenze des Volllaufs bleibt `MIN(Message.MessageLastUpdate)`, und Rollup-Zeilen
    > unterhalb des Bestandsanfangs werden **nicht** entfernt. Stattdessen wird der Fall **erkannt
    > und sichtbar gemacht**.
    >
    > **Warum nicht löschen.** Die eingefrorenen Eimer sind kein Müll, sondern der letzte Rest einer
    > Auskunft, die es sonst nirgends mehr gibt: Hat das Altsystem die Nachrichten entfernt, ist
    > `message_rollup` die einzige Stelle im Haus, an der noch steht, wie viel in jenem Zeitraum
    > gelaufen ist. **Ein Monitoring, das seine eigene Geschichte wegräumt, sobald die Quelle sie
    > vergisst, räumt genau das weg, wofür es gebaut wurde.** Das Löschen wäre außerdem
    > unumkehrbar — und es würde einen Vorgang stillschweigend vollziehen, den niemand angeordnet
    > hat: Der Rollup-Job entschiede von sich aus, dass eine Aufbewahrungsfrist abgelaufen ist,
    > obwohl er sie nicht kennt (die Frist selbst ist ungedeckt, siehe
    > [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 und V6 der Messrunde).
    >
    > **Was stattdessen gebaut wird — und was den Punkt offen hält.** Eine **Erkennung**: Der Lauf
    > vergleicht `MIN(message_rollup.stunde)` mit `MIN(Message.MessageLastUpdate)`. Liegt die erste
    > vor der zweiten, ist der Fall eingetreten; er gehört sichtbar in `rollup_lauf` und von dort in
    > die Anzeige des Aktualisierungsstands aus 10b. **Der Schaden, den Punkt 54 beschreibt, ist
    > nicht das Dastehen der Zeilen, sondern dass niemand es merkt** — `SUM(anzahl)` läuft gegen die
    > Zeilenzahl von `Message` auseinander, und das Dashboard zeigte für den abgelaufenen Zeitraum
    > Nachrichten, die es nicht mehr gibt, ohne dazuzusagen, dass sie nur noch hier stehen. Erkannt
    > und benannt ist derselbe Zustand eine Auskunft statt eines Fehlers.
    >
    > **Der Punkt bleibt offen, bis die Erkennung gebaut ist.** Sie ist eine Codeänderung und gehört
    > nach **10b** oder in eine eigene kleine Runde — die Korrekturrunde vom 27.08.2026 ändert
    > ausschließlich Dokumentation. Prüfbar ist sie wie die verworfene Lösung: Ein Datenbanktest
    > setzt eine Rollup-Zeile vor den Bestandsanfang und verlangt, dass der Lauf sie **meldet** und
    > **stehen lässt**.
    >
    > > ### ✔ Gebaut am 31.08.2026 (Schritt 10b‑2 Teil C) — der Punkt ist geschlossen
    > >
    > > **Die Erkennung steht** (§5, „Schritt 7"): Der Volllauf vergleicht
    > > `MIN(message_rollup.stunde)` mit `MIN(Message.MessageLastUpdate)` — **auf die Stunde
    > > abgerundet**, sonst meldete jeder Lauf einen Fehlbefund — und schreibt bei einem Befund eine
    > > `WARN`-Zeile mit der Zahl der betroffenen Eimer **je Ebene**. Gelöscht wird nichts.
    > >
    > > **Drei Datenbanktests** halten das fest, genau in der Form, die der Absatz darüber verlangt:
    > > gemeldet **und** stehen gelassen, die Gegenprobe ohne Befund, und die eingegrenzte
    > > Summenprobe.
    > >
    > > **Und die Abnahmeprobe ist mit eingegrenzt:** `SUM(anzahl)` = `COUNT(Message)` gilt seit dem
    > > 31.08.2026 über den **überlappenden Bereich**. Ohne das wäre sie nach dem ersten produktiven
    > > Löschlauf dauerhaft rot — und ein dauerhaft roter Test wird abgeschaltet.
    > >
    > > **Was der Punkt weiterhin nicht deckt und auch nicht decken soll:** eine **Spalte** in
    > > `rollup_lauf` und eine Anzeige des Zustands im Frontend. Beides ist eine Migration und eine
    > > Gestaltungsentscheidung; es kommt, wenn das Frontend es braucht. Heute steht der Befund im
    > > Protokoll, und das ist die Stelle, an der ein Betreiber ihn sucht.
    >
    > **Der Absatz darüber bleibt vollständig stehen.** Die verworfene Lösung ist die bessere
    > Beschreibung des Problems, und ohne sie wäre nicht mehr erkennbar, wogegen entschieden wurde.
    >
    > > **Zur Herkunft dieses Textes.** Die Entscheidung — *nicht löschen* — kommt vom
    > > Auftraggeber und ist am 27.08.2026 zusammen mit dem Auftrag der Korrekturrunde ergangen.
    > > **Die Begründung ist hier ausgeschrieben und nicht mitgeliefert worden**; sie folgt aus
    > > diesem Punkt, aus §8 der Projektbeschreibung und aus V6 der Messrunde. Wer sie anders
    > > begründet sähe, ändert diesen Text — die Entscheidung ändert das nicht.

---

## 14. Was dieser Schritt nicht zeigt

1. **Nichts über die Produktion.** Alle Zahlen stammen von der Testkopie, und M92 rechnet
   ausdrücklich nicht hoch. Weder Aufbewahrungsdauer noch Umfang noch Rückstand sind dort bekannt.
2. **Den Kaltlauf nicht.** `FLUSH TABLES` steht `monitor_read` nicht zu; der Puffer fasst `Message`
   9,26‑mal. Gemessen ist der Warmfall — mit Ausnahme des ersten Delta-Laufs einer JVM (387 ms
   gegen 20–31 ms), und der misst den kalten Abfrageplan, nicht die kalte Platte.
3. **Ob der Zeitplan trägt.** Weder der stündliche noch der nächtliche Lauf ist im Betrieb
   ausgelöst worden — sie sind von Hand gefahren. Was der Zeitplan tut, ist nicht beobachtet,
   sondern konfiguriert.
4. **Nichts über 10b.** Die Leseabfrage des Dashboards ist in M89 gemessen (11,30 ms im
   Standardfenster mit Katalog-Join), aber nicht gebaut.
5. **Ob die Zwölf‑Monats‑Ansicht kalt trägt** *(ergänzt 27.08.2026)*. §9a misst sie warm mit
   767,128 ms. Was sie kalt kostet, ist nicht gemessen und mit dem übertragenen Faktor aus M44 nur
   geschätzt — genau die Unsicherheit, für die das Tor bei 700 ms und nicht bei 1,0 s steht.
6. ~~**Ob die Monatsebene das Tor lösen würde** *(ergänzt 27.08.2026)*. M87 nennt für sie 11.957
   Zeilen über den Gesamtbestand; **gemessen ist keine einzige Abfrage über sie**. Die Aussage in
   §9a, sie brächte den einstelligen Millisekundenbereich, ist aus der Linearität der Kosten
   gerechnet und nicht erhoben.~~
   **✔ Erledigt am 31.08.2026: M107 misst vier Abfragen über sie** (§9d) — 65,350 ms und 88,672 ms
   bei `NEXANS`, das Tor öffnet mit Faktor 7,9 Luft. **Die damalige Aussage war dabei zu
   optimistisch**: Es ist nicht der einstellige Millisekundenbereich, sondern der niedrige
   zweistellige, und das ist genau, was die Linearität hergibt (9.649 gelesene Zeilen × 5,3 bis
   11,5 µs = 51 bis 111 ms). Ungemessen bleibt der **kalte** Fall — offener Punkt 79.
7. **Was geschieht, wenn das Altsystem alte Nachrichten entfernt.** Die Testkopie wird nicht
   beschnitten; der Fall ist hier weder eingetreten noch prüfbar. Er ist der Grund für offenen
   Punkt 54 und der einzige bekannte Weg, auf dem die Summenprobe dieses Baus auseinanderlaufen
   kann.
