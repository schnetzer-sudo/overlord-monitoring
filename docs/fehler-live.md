# Fehler live — Teil A: der Baustein und die Übersicht

*18.09.2026.* Auftrag „Fehler live, Teil A (Baustein und Übersicht)", Stand 18.09.2026, Zweig
`feat/fehler-live`. Schritt **10f** im [Implementierungsplan](IMPLEMENTIERUNGSPLAN_MVP.md).
Entscheidungen ab **E‑208**, Messung **M188**, offene Punkte ab **209**.

**Die Frage dieser Datei:** Wie hört die Übersicht auf, eine Nachricht als Fehler zu zählen, die
nach einer Nachverarbeitung keiner mehr ist — ohne den Rollup anzufassen und ohne dass Kachel,
Verlauf und Verteilung auseinanderlaufen?

---

## Nummernvergabe

Gesucht am 18.09.2026 vor der ersten Vergabe mit `git grep -E` über `docs/**/*.md`,
`DEVELOPMENT_GUIDELINES.md`, `backend/src`, `frontend/src` und `frontend/tests` auf `HEAD`, über die
zwei Zweige, die nicht in `main` sind (`feat/suchfeld-untermenues`, `test/indexbestand-e37`), und mit
`grep -rE` über die Arbeitsbäume `overlord-monitoring-e37` und `overlord-monitoring-neu-laden`. Der
Strich als `-` **oder** U+2011 über `E.{0,3}<Zahl>\b` — die Zeichenklasse `[‑-]` findet den
geschützten Bindestrich nicht. **Geeicht** an bekannt vergebenen Nummern: auf `HEAD` E‑207 (10
Treffer), M187 (9) und Punkt `**208**` (3); auf `feat/suchfeld-untermenues` M150 (9), auf
`test/indexbestand-e37` M44 (66). Jeder Treffer ist gelesen, nicht gezählt.

| | |
|---|---|
| **Entscheidungen** | **ab E‑208.** Höchste vergebene: **E‑207** ([`dateiansicht-darstellung.md`](dateiansicht-darstellung.md), 18.09.2026). Der einzige Treffer auf E‑208/E‑209 ist dort der Suchvermerk *„die Suche nach E‑206 bis E‑209 … war leer"* — gelesen, keine Vergabe. **E‑780** ist der bekannte Falschtreffer. Kein Zweig und kein Arbeitsbaum trägt E‑208 oder höher. *Der Auftrag erwartete „ab E‑193" — vergeben seit dem 17.09.2026 an die Darstellungswahl der Dateiansicht (E‑193 bis E‑207)* |
| **Messung** | **M188.** Höchste: **M187** (Sichtprüfung der Darstellungswahl, [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §11). Kein Treffer auf `\bM18[89]\b` oder `\bM19[0-9]\b` — weder auf `HEAD` noch in den Zweigen oder Arbeitsbäumen. *Der Auftrag erwartete M187* |
| **Offene Punkte** | **ab 209.** Höchster: **208** ([`dateiansicht-darstellung.md`](dateiansicht-darstellung.md)). Die Treffer auf `**209**` bis `**219**` sind Zahlen in Tabellenzellen und im Fließtext der Messdateien (`messungen-schritt7.md` **216**, **219**; `messungen-schritt9.md` **217**; `messungen-schritt10.md` **216**; `messungen-schritt10b.md` **212**; die Zeile zu `messungen-schritt7.md` in [`README.md`](README.md)) — gelesen, keine Punkte. *Der Auftrag erwartete Punkt 196 — vergeben an dieselbe Runde (196 bis 208)* |
| **Schritt** | **10f.** Kein Treffer auf `\b10f\b` oder `10‑f` in keiner Quelle; die Darstellungswahl ist ein Nachtrag zu Schritt 8 und kein eigener Schritt |

---

## 1. Anlass

**Gemeldet vom Auftraggeber am 18.09.2026, aus der Produktion:** Eine Nachricht stand auf
`ERROR_TIMEOUT` und wurde korrekt als Fehler gezählt. Nach der Nachverarbeitung stand sie auf
`RUNNING`. „Zuletzt aufgefallen" und die Kachel *Läuft* zeigten das sofort — beide lesen live. **Die
Fehlerkachel und der Verlauf zählten sie weiter als Fehler.**

**Die Ursache — hergeleitet, nicht an der Produktion gemessen.** Der Rollup bucht eine Nachricht in
den Stundeneimer von `MessageLastUpdate` ([`rollup.md`](rollup.md) §7), und ein Statuswechsel setzt
`MessageLastUpdate` neu: Die Nachricht wandert in die aktuelle Stunde. Der Delta-Lauf schreibt nur die
vorige und die laufende Stunde neu (§3, §5 dort), der Live-Rest korrigiert nur ab G
([`live-rest.md`](live-rest.md) §2). **Der alte Eimer behält den Fehler deshalb bis zum Volllauf um
03:00.** Das ist kein *Nachschreiben* im Sinne von Punkt 49 in [`rollup.md`](rollup.md) §13 — dort
wandert ein Zeitstempel um Sekunden, und das Nachlauffenster fängt ihn —, sondern ein **Abgang aus
einem alten Eimer**, und kein Nachlauffenster erreicht ihn. Die bekannte Grenze 3 in
[`dashboard.md`](dashboard.md) §2 (*„doppelt gezählt wird also nichts"*) gilt erst nach dem Volllauf.

**Vorhergesagt war der Mechanismus**, gemessen werden konnte er nicht: M96
([`messungen-schritt10b.md`](messungen-schritt10b.md)) hält fest, dass eine Nachricht ihren Status
ändern kann, *nachdem* ihr Eimer gerollt wurde, und dass der Satz *„im Betrieb springt nichts"*
damit nicht belegt ist — auf der eingefrorenen Testkopie tritt der Fall nie ein.

---

## 2. Die Regel

> **Die Übersicht liest die Einordnung `FEHLER` nicht mehr aus dem Rollup, sondern aus einer
> Live-Lesung über `Message`. Alle anderen Einordnungen kommen weiter aus Rollup und Live-Rest.**

Das ist die **vierte benannte Ausnahme** von Regel L2 ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
§8, Regel 2) — nach den Kacheln *Läuft* und *Wartend* (E‑72, E‑73) und dem Live-Rest der laufenden
Stunde ([`live-rest.md`](live-rest.md)).

---

## 3. Die Entscheidung des Auftraggebers — Weg B (E‑208)

**Entschieden vom Auftraggeber am 18.09.2026, per Auswahl aus vier Wegen.** Alle vier gehören ins
Protokoll, auch die verworfenen; beschrieben ist jeder nur so weit, wie der Auftrag ihn benennt.

| Weg | Was er täte | Entschieden |
|---|---|---|
| **A** | Abgleich im Delta-Lauf | verworfen |
| **B** | **Fehler live** — die Einordnung `FEHLER` der Übersicht aus einer Live-Lesung über `Message` | **genommen (E‑208)** |
| **C** | das Nachlauffenster des Delta-Laufs auf 48 Stunden | verworfen |
| **D** | nur dokumentieren | verworfen |

---

## 4. Der Baustein in `common`

*(folgt mit dem Bau, A2)*

## 5. Die Übersicht

*(folgt mit dem Bau, A4)*

## 6. Die Oberfläche

*(folgt mit dem Bau, A5)*

## 7. Die Dev-Zeile

*(folgt mit der Messung)*

---

## 8. Die Messung — M188 (Regeln L7, L15)

### Vorregistriert — eingetragen und eingecheckt vor dem ersten Lauf

> **Rahmen, für alle drei Tore.** Gegen die Testkopie, Profil `dev`, am Anker `2025-12-30 04:09:47`
> — derselbe Stichtag wie M146 und M178. Die Fenster je Paar aus `Rollupzeitraum.fenster(Anker)`,
> Wanduhrzeit der Quelle, `bis` ausschließend: `48H` `2025-12-28 05:00` bis `2025-12-30 05:00`,
> `30T` `2025-12-01` bis `2025-12-31`, `12M` `2025-01-01` bis `2026-01-01`.
>
> - **Sequenziell, nie parallel** — kein zweiter Lauf gegen die Testkopie zur selben Zeit.
> - Je Lage **ein Aufwärmlauf, dann die beste von fünf**.
> - **In SQL:** `information_schema.PROFILING` **und** die Wanduhr des Servers (`SYSDATE(6)` vor und
>   nach dem Statement, E‑107), Eichung mit `SELECT 1`; `EXPLAIN` je Statement (Regel L15); dazu die
>   Handler-Zähler aus `information_schema.SESSION_STATUS` für einen Lauf je Lage (gelesene
>   Indexsätze, gemessen statt geschätzt). **Die Statementtexte kommen gerendert aus dem Code**
>   (`StatementType.STATIC_STATEMENT`, nur die Bindewerte als Literale) und sind nicht abgetippt.
> - **Am Code:** `MessungM188DbIT`, Bauform von M178 und M186. Zeiten nach `System.out`, in keine
>   Zusicherung (Regel T1). Die Uhr über `@TestBean` gestellt; kein Schreibzugriff auf
>   `GlassfishDB` oder `rollup_lauf` (S1, T2).
>
> #### Tor 1 — die Lesung (vor dem Einbau gemessen)
>
> **Lagen:** `NEXANS`, `SUTTONS`, `VOTG` und **ein Mandant ohne eine einzige Fehlerzeile**, je
> `48H`, `30T`, `12M` — zwölf Lagen. **Der vierte Mandant wird per Zählung bestimmt, nach einer vorab
> festgelegten Regel:** unter den Mandanten, bei denen im **ganzen** Bestand keine Zeile die
> Fehlerbedingung erfüllt, der mit den meisten Nachrichten im ganzen Bestand; bei Gleichstand der
> alphabetisch erste. Die Zählung steht als erste Abfrage der Sitzung im Protokoll.
>
> **Erwartet:**
>
> | | |
> |---|---|
> | **Plan** | `Message` wird über **`MessageStatusIDX`** gelesen, mit **6.257** geschätzten Zeilen — dieselbe Zahl wie Block 6 in M146, in allen zwölf Lagen; die Kette hängt per Primärschlüssel an. Welche Tabelle den Einstieg macht, darf wie in M146 am Mandanten hängen (`SUTTONS`: `ProjectMandant`) |
> | **Gelesen** | in allen zwölf Lagen **dieselbe Zahl** gelesener Indexsätze über den Statusbereich — unabhängig von Fenster und Mandant |
> | **Laufzeit** | **20 bis 26 ms** in SQL (M145, M146: 20,0 bis 26,2 ms für Block 6), am Code in der Größenordnung von M146 (23,3 bis 30,2 ms); unabhängig von Fenster und Mandant, auch beim Mandanten ohne Fehler |
>
> **Grenze:** In **keiner** Planzeile steht `MessageLastUpdateIDX` als `key` — auch nicht als
> Rowid-Filter (`MessageStatusIDX|MessageLastUpdateIDX`). *Zusätzlich zum Auftrag, als dieselbe
> Frage:* auch nicht `MessageLastUpdateProcessMessageIDX`, der zweite Zeitindex. Und **höchstens
> 100 ms je Lage** — in SQL (Profil und Wanduhr) und am Code.
>
> #### Tor 2 — die Verteilung ohne Fehler, beide Sichten
>
> **Lagen:** `NEXANS`, `SUTTONS`, je `48H`, `30T`, `12M`, je Sicht — zwölf Statements; in derselben
> Sitzung daneben jeweils die Form **ohne** Zusatzbedingung.
>
> **Erwartet:** **Der Plan ist der aus M178** — Zeile für Zeile (Tabelle, Zugriffsart, Index) derselbe
> wie der Plan der Form ohne Zusatzbedingung: bei `NEXANS` die Rollup-Ebene `range` über `PRIMARY`,
> bei `SUTTONS` `ProjectMandant` über `ProjectMandant_Mandant_idx`, der Katalog `eq_ref` über
> `PRIMARY`. Die Bedingung `NOT (…)` auf `message_status` ist ein Filter auf der gelesenen Zeile und
> kein Zugriffspfad. **Die Laufzeit liegt im Rauschen** der Form ohne Zusatzbedingung derselben
> Sitzung.
>
> **Grenze:** **Plan unverändert** — Zeile für Zeile wie oben. **Höchstens das 1,2‑Fache von M178
> je Lage.** Gemessen wird dafür **am Repository**, wie M178 gemessen hat (jOOQ-Rendering,
> Verbindung, Zeilenabbildung; beste von fünf); Profil und Wanduhr in SQL stehen daneben. **Bezug je
> Lage ist der kleinere der beiden M178-Läufe derselben Form** — die strengere Lesart, festgelegt vor
> dem Lauf. Gerechnet, nicht gemessen:
>
> | Lage | Partnersicht: Bezug → Grenze | Richtungssicht: Bezug → Grenze |
> |---|---:|---:|
> | `NEXANS` `48H` | 12,380 → **14,856 ms** | 12,685 → **15,222 ms** |
> | `NEXANS` `30T` | 71,205 → **85,446 ms** | 70,195 → **84,234 ms** |
> | `NEXANS` `12M` | 97,744 → **117,293 ms** | 95,738 → **114,886 ms** |
> | `SUTTONS` `48H` | 10,139 → **12,167 ms** | 9,968 → **11,962 ms** |
> | `SUTTONS` `30T` | 51,136 → **61,363 ms** | 51,391 → **61,669 ms** |
> | `SUTTONS` `12M` | 66,862 → **80,234 ms** | 66,444 → **79,733 ms** |
>
> #### Tor 3 — die Seite durch den Endpunkt
>
> **Lagen:** `NEXANS`, `SUTTONS`, je `48H`, `30T`, `12M` — sechs Lagen, am Anker, mit dem
> Wasserstand der Testkopie (Live-Rest `NICHT_NOETIG`, [`live-rest.md`](live-rest.md) §11). Je Lage
> **durch den Endpunkt** (`GET /api/dashboard?zeitraum=…`, HTTP-Umlauf im Testclient samt Sitzung
> und Serialisierung), **am Dienst** (`DashboardService.landingpage`) und als **Bezug in derselben
> Sitzung**: dieselbe Seite am Dienst mit ausgesetzter Fehlerlesung — das ist die Seite, wie sie vor
> diesem Schritt war. **Zuschlag** = Dienst minus Bezug.
>
> **Erwartet: der bisherige Wert plus die Lesung.** Gerechnet, nicht gemessen — die Seite aus M178
> (am Dienst), plus 10 bis 25 ms für den Endpunkt über dem Dienst (M186), plus die Lesung am Code
> (23,3 bis 30,2 ms, M146), plus 0 bis 2 ms für Wasserstand und Nachlesung:
>
> | Lage | M178, Seite | **erwartet, Endpunkt** |
> |---|---:|---:|
> | `NEXANS` `48H` | 65,4–69,9 ms | **98,7–127,1 ms** |
> | `NEXANS` `30T` | 223,3–225,5 ms | **256,6–282,7 ms** |
> | `NEXANS` `12M` | 294,2–297,6 ms | **327,5–354,8 ms** |
> | `SUTTONS` `48H` | 58,7–61,8 ms | **92,0–119,0 ms** |
> | `SUTTONS` `30T` | 166,3–167,5 ms | **199,6–224,7 ms** |
> | `SUTTONS` `12M` | 206,7–209,4 ms | **240,0–266,6 ms** |
>
> **Der Zuschlag** liegt erwartet bei **23 bis 32 ms** je Lage — die Lesung, bei Fehlern im Fenster
> die Nachlesung; die Verteilung ohne Fehler kostet, was die Verteilung kostet (Tor 2).
>
> *Zusätzlich zum Auftrag:* die teuerste Lage aus M186 — `NEXANS` im dichtesten Vierstundenbereich
> (G `2024-10-09 18:00`, jetzt G + 3 h, Live-Rest `ANGEWANDT` über den gesetzten Wasserstand), je
> Paar, damit beide Ausnahmen derselben Seite zusammen gemessen sind. Erwartet: M186 plus der
> Zuschlag, also **399,1–408,0 / 411,9–420,8 / 382,4–391,3 ms** für `48H` / `30T` / `12M`.
>
> **Grenze:** **höchstens 500 ms je Lage** durch den Endpunkt — das Tor aus M186, für alle neun Lagen.
>
> #### Die Dev-Zeile — die Eichung
>
> Auf der Testkopie ist der Rollup vollständig, und ein Abgang ist nicht herstellbar (`RUNNING` kommt
> null Mal vor, Tests schreiben nicht). **Der Ersatz ist hier deshalb eine Identität: Keine Zahl der
> Übersicht darf sich ändern** — Verlauf, Kacheln *Nachrichten* und *Fehler* samt Fehlerarten, beide
> Sichten der Verteilung, Leerzustand — für `NEXANS`, `SUTTONS` und `VOTG` in allen drei Paaren. Die
> Kachel *Fehler* ist danach, was Block 6 in M146 je Paar zählte: `NEXANS` **50 / 55 / 711**,
> `SUTTONS` **0 / 5 / 103**, `VOTG` **0 / 0 / 8**. Weicht eine Zahl ab: zuerst den Wasserstand
> prüfen, dann melden.
>
> #### Wie gelesen wird — festgelegt vor dem Lauf
>
> - **Liegt eine Lage über ihrer Grenze, oder steigt die Lesung über einen Zeitindex ein: anhalten
>   und berichten, nicht nachjustieren.** Ein Indexhinweis wäre eine Entscheidung (offener Punkt 82
>   in [`dashboard.md`](dashboard.md) §11) und keine Korrektur.
> - Der **Zuschlag** trägt die Aussage über diesen Bau; der **Endpunkt** trägt Tor 3; die
>   **Rechnungen** oben tragen nur, ob die Größenordnung hält. Die Form **ohne** Zusatzbedingung
>   derselben Sitzung trägt, ob eine Abweichung in Tor 2 dem Bau gehört oder dem Tag.
> - Abweichungen werden benannt und nicht umgedeutet; sie stehen neben diesem Kasten, nicht in ihm.
> - **Zugesichert wird im Läufer nur**, dass jede Antwort `200` ist, dass die Fehlerlesung
>   angewandt ist (in der Zusatzlage auch der Live-Rest) und dass Kachel *Nachrichten* und beide
>   Sichten dieselbe Zahl tragen — sonst mäße er die Laufzeit einer falschen Seite.

---

## 9. Tests

*(folgt)*

## 10. Regelbezug

*(folgt)*

## 11. Die Entscheidungen dieser Runde

*(folgt)*

## 12. Offene Punkte

*(folgt)*

## 13. Was nicht gebaut ist

*(folgt)*
