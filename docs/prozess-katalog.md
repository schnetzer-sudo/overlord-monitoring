# Prozess-Katalog

Stand: 20.08.2026 · Schritt 9b · Fachliche Festlegung
Messgrundlage: [`messungen-schritt9.md`](messungen-schritt9.md), M74 bis M79 · Nachtrag **M80**

---

## 1. Wozu

**Der Katalog macht aus etwas Lesbarem etwas Gruppierbares.** Das ist seine einzige Aufgabe.

`SOSName` ist im Altsystem bereits in Klartext gepflegt und wird seit Schritt 4 als Anzeigename
verwendet — „Eingehender IFTMIN BAYER" ist aber ein Satz und kein Schlüssel. Ein `GROUP BY` darauf
ginge nur mit Parsen, und Parsen ist nach `PROJEKTBESCHREIBUNG.md` §4.4 ausgeschlossen.

**Abnehmer ist ausschließlich Schritt 10:** die Verteilung nach Partner, die nach Partner und
Richtung, und die nach kuratiertem Partner gruppierte Prozessansicht.

> **Warum auch tote Prozesse kuratiert werden.** 765 von 1.503 Prozessen tragen im Bestand der
> Testkopie keine einzige Nachricht (M74b). Sie werden trotzdem gepflegt — nicht aus
> Vollständigkeitsdrang, sondern weil ein Monitoring auch zeigen muss, was **nicht** passiert ist.
> Ein Partner, der aufhört zu senden, fällt niemandem auf: Nichts erzeugt keine Zeile, keine Kachel,
> keinen Alarm.
>
> **Erst die Kuratierung macht das Schweigen benennbar.** Ohne zugeordneten Partner lässt sich „von
> X kam seit acht Wochen nichts" nicht formulieren, weil es kein X gibt. Der tote Prozess ist nicht
> die Ausnahme von der Kuratierung, er ist ihr Anlass.
>
> Die Auswertung selbst gehört nicht hierher, sondern nach Schritt 10 — siehe §8.

---

## 2. Datenmodell

`overlord_monitor.process_catalog`, eine Zeile je `ProcessID`.

| Feld | |
|---|---|
| `ProcessID` | Schlüssel, `varchar(36)`, Zeichenkette ohne Fremdschlüssel über die Schemagrenze |
| Partner | kuratiert, **darf leer sein** |
| Richtung | kuratiert, geschlossene Menge: eingehend / ausgehend |
| Pflegestatus | **offen** / **gepflegt** |

**E1 — Zwei kuratierte Felder, nicht vier.** `PROJEKTBESCHREIBUNG.md` §4.4 nannte Partner,
Standort, Richtung und Belegart. Standort und Belegart entfallen: Im MVP liest sie nichts. Das
halbiert die Kuratierungsarbeit über 1.503 Prozesse.

*Nebenwirkung, die eine Namenskollision auflöst:* „Belegart" ist seit Schritt 7 in der Oberfläche
vergeben — `GET /api/bam/typen` liefert „die konfigurierten Belegarten" als Auswahl neben dem
Suchfeld, gemeint sind BAM-Typen wie *Lieferschein-Nr.* Der Plantext meinte mit demselben Wort die
EDI-Dokumentart. Zwei Bedeutungen in derselben Oberfläche, für einen Nutzer, der laut Leitsatz kein
EDI-Spezialist ist. Mit E1 entsteht das zweite Feld nicht.

**E2 — Keine Tabelle `partner`.** Die Auswahlliste ist ein `SELECT DISTINCT` über die Katalogzeilen
des aktiven Mandanten. Ohne eigene Pflegeoberfläche und ohne Attribute über den Namen hinaus liefert
eine Tabelle nichts, was die Ableitung nicht auch liefert — sie kostete eine Migration und eine
Sicherungspflicht. Eine spätere Tabelle ließe sich aus den vorhandenen Werten befüllen.

**E3 — Partner sind mandantengebunden.** `BAYER` bei VOTG und bei SUTTONS sind zwei Werte: dieselbe
Firma, zwei EDI-Beziehungen. Der Mandantenbezug fällt aus dem Join über `ProjectMandant` und braucht
**keine eigene Spalte**.

**E4 — Zwei Pflegezustände, und ein leerer Partner ist bewusst speicherbar.**

| Zustand | Bedeutung |
|---|---|
| offen, Feld leer | noch nicht angesehen, Heuristik hat nichts gefunden |
| offen, Feld gefüllt | **Vorschlag** der Heuristik, unbestätigt |
| gepflegt, Feld gefüllt | kuratiert |
| gepflegt, Feld leer | **hingesehen, es gibt nichts** |

Drei Bedeutungen aus zwei vorhandenen Feldern — ein dritter Pflegestatus wird nicht gebraucht. Ohne
die Speicherbarkeit des leeren Werts stünden Auffangprozesse dauerhaft auf „offen" und der
Fortschritt erreichte nie sein Ende.

**Der Schlüssel `ProcessID` allein trägt.** M74a hat es geprüft: `projekte_mit_mehreren_mandanten =
0`, 1.490 Joinzeilen auf 1.490 verschiedene `ProcessID`. Eine Katalogzeile gehört immer genau einem
Mandanten.

> **13 Prozesse sind für niemanden erreichbar.** 1.490 der 1.503 hängen an einem Projekt mit
> Mandantenzeile; sechs Projekte ohne Zuordnung tragen die übrigen 13 (M74a). Sie erscheinen in
> keiner Pflegeliste und sind nie kuratierbar. Das ist dieselbe Lage wie in Annahme A8 — **A8 zählt
> Projekte, hier sind es Prozesse**, die Zahlen sind nicht vergleichbar. Es wird kein Sonderpfad
> gebaut. Der Eintrag steht hier, damit später jemand weiß, warum ein Prozess fehlt.

---

## 3. Die Heuristik

**Sie schlägt vor, sie entscheidet nicht.** Ein Vorschlag landet in einer Zeile mit Status *offen*.
Was sie nicht sicher ableiten kann, lässt sie **leer** — Regel Q4, es wird nichts geraten.

### 3.1 Warum es zwei Regeln sind

M75 hat gemessen, ob die `ProcessID` mit `<Ziffern>_` beginnt: **100 % bei drei Mandanten, 0 % bei
sieben, kein Zwischenwert.** Daraus folgt aber nicht, dass die Ableitung für drei trägt und für
sieben nicht. M76 zeigt beide Hälften als falsch:

| | |
|---|---|
| `SUTTONS` **hat** den Präfix — und keinen Partner im Prozessnamen | *„Der Prozessname trägt keinen Partner, sondern einen Vorgang (Buchung, Status)."* Siebzehn Prozesse mit perfektem Muster, aus dem nichts zu holen ist |
| `IBIS`/`IBISGUS` haben **keinen** Präfix — und tragen Richtung *und* Partner im Prozessnamen | CamelCase ohne Trennzeichen: `AuslagerungAusgehend<PARTNER>`. **281 Prozesse**, der zweitgrößte Block |

**Die Zweiteilung aus M75 ist eine über Zeichensetzung, nicht über Ableitbarkeit.** Die Null bei
IBIS heißt nicht „kein Aufbau", sondern „Aufbau ohne Trennzeichen".

### 3.2 Regel A — Präfix und Position

Führenden `<Ziffern>_`-Präfix abschneiden, **Token 2 ist der Partnerkandidat.**

Feuert bei **zwei oder drei** Unterstrichen in der `ProcessID`:

| Gestalt | Beispiel | Mandant |
|---|---|---|
| `<Nr>_<Partner>_<Belegart>_<Norm>` | `40000_AMG_LAB_VDA` | NEXANS |
| `<Nr>_<Partner>_<Nachrichtenart>` | — | VOTG, Partnerprojekte |
| `<Nr>_<Partner>_<Nummer>_INVOICE` | — | VOTG, `110_VTG_SalesInvoice` |

**Feuert nicht** bei einem, vier oder fünf Unterstrichen. Bei vier und fünf ist der Partnername
mehrteilig — genau der Fall, vor dem §4.4 warnt (`KE_OSTROV`, `DAS_DRAEXLMAIER`,
`DELFINGEN_DE_HA`), und dort ist unentscheidbar, wo der Partner endet.

**`SUTTONS` ist namentlich ausgenommen**, obwohl das Muster passt.

### 3.3 Regel B — CamelCase mit Richtungsanker

Für Prozessnamen ohne Trennzeichen. An Großbuchstaben zerlegen, `Eingehend` oder `Ausgehend`
suchen: **Was dahinter steht, ist der Partner; das Ankerwort selbst ist die Richtung.**

Fehlt das Ankerwort, gibt es keinen Vorschlag — weder für den Partner noch für die Richtung.

> **Gemessen 20.08.2026 — „an Großbuchstaben zerlegen" ist wörtlich zu nehmen.** Beim Bau hat die
> erste Fassung zusätzlich verlangt, dass vor dem Ankerwort ein *Klein*buchstabe steht. Das kostete
> **26** Prozesse (`IBIS` 20, `IBISGUS` 6), die unmittelbar davor ein zweibuchstabiges Kürzel in
> Großschreibung tragen. Ein Großbuchstabe *ist* in CamelCase ein Wortanfang; eine Bedingung davor
> gibt es nicht. Gebraucht wird nur die Grenze **dahinter**, damit `Eingehende…` nicht als Anker
> durchgeht.
>
> **„Für Prozessnamen ohne Trennzeichen" ist der Zweck und nicht die Bedingung.** Der Anker
> entscheidet. Im Bestand ist die Unterscheidung folgenlos — die Prozesse mit Trennzeichen tragen
> dort ohnehin keinen Anker (M80‑6).

### 3.4 Richtung aus dem Projektnamen

Derselbe Anker wie Regel B, auf die Projektkennung angewandt. `300_KundenEingehend` bei NEXANS
(17 Projekte, 733 Prozesse), `OrdersVerarbeitungEingehendNL` bei IBIS/IBISGUS (65 Projekte).

> **Das revidiert eine Entscheidung desselben Tages.** Am 20.08.2026 war die Richtungs-Heuristik
> verworfen worden, weil sie über den `SOSName` nur für VOTG trägt (M77) — Begründung: „eine
> Nachmittagsarbeit gegen Code, der gepflegt sein will". **Das Argument steht, die Rechnung nicht:**
> Der Anker entsteht mit Regel B ohnehin, und auf Projektnamen angewandt deckt er rund 82 der etwa
> 140 Projekte.
>
> **Der `SOSName`-Weg wird trotzdem nicht nachgezogen.** Er braucht den Join auf `SOS` und eine
> Konfliktregel für die acht Prozesse mit mehreren SOS. Der Projektname braucht beides nicht. VOTG
> bleibt damit der einzige Mandant, dessen Richtung von Hand kommt — 39 Projekte.

### 3.5 Was die Heuristik erreicht

Von den 1.490 erreichbaren Prozessen:

| | Prozesse | |
|---|---:|---|
| Regel A | **887** | NEXANS 509, VOTG 378 |
| Regel B | **281** | IBIS 192, IBISGUS 89 |
| **Vorschlag insgesamt** | **1.168** | 78,4 % |
| ohne Vorschlag | **322** | siehe unten |

Die 322 im Einzelnen: NEXANS 224 (mehrteilige Partner und Kurzformen), `SUTTONS` 17, `ZAST` 35,
`NXHBE` 17, VOTG 12, `EDITIONLINGERI` 9, `WOC` 4, `SYSTEM` 4.

> **Korrigiert 20.08.2026 — die Tabelle oben ist eine Projektion, gemessen ist sie erst jetzt.**
> Sie ist beim Bau von Schritt 9b gegen den Bestand nachgerechnet worden (M80‑6,
> `HeuristikBestandDbIT`). Die alten Zahlen bleiben stehen, weil sie den Stand vor der Messung
> richtig wiedergeben.
>
> | | projiziert | **gemessen** |
> |---|---:|---:|
> | Regel A | 887 | **887** ✔ |
> | Regel B | 281 | **280** |
> | Vorschlag insgesamt | 1.168 | **1.167** (78,32 %) |
> | ohne Vorschlag | 322 | **323** |
>
> **Regel A trifft auf den Prozess genau, beide Mandanten.** Regel B weicht an *zwei* Stellen ab,
> und die beiden heben einander fast auf:
>
> - **`IBIS` 187 statt 192 und `IBISGUS` 88 statt 89.** Fünf bzw. ein Prozess tragen das Ankerwort
>   **überhaupt nicht** — im Wesentlichen die mit Unterstrich aus M75. Keine Fassung der Regel kann
>   sie erreichen; die Projektion hatte für diese beiden Mandanten schlicht *alle* Prozesse gezählt.
> - **`EDITIONLINGERI` 5 statt 0.** Regel B trifft dort sehr wohl, bei fünf der neun Prozesse. Die
>   Projektion hatte die Regel stillschweigend auf `IBIS`/`IBISGUS` beschränkt — sie gilt aber, wo
>   ihr Muster steht, und nicht, wo man sie gemeint hat.
>
> **Wer nur die Summe geprüft hätte, hätte beide Abweichungen für nicht vorhanden gehalten** — sie
> unterscheiden sich um eins. Der Regressionstest zählt deshalb je Mandant.

**Die Richtung — gezählt, nicht projiziert.** Die Tabelle oben zählt allein den **Partner**. Die
Heuristik füllt daneben für **1.020 der 1.490** Prozesse eine **Richtung**, also **68,5 %**.
Diese Zahl ist nirgends veranschlagt worden; sie ist am 20.08.2026 über den gefüllten Katalog
**gezählt** (M80‑7).

| | Prozesse | mit Richtung | Quelle |
|---|---:|---:|---|
| `NEXANS` | 733 | **722** | Projektname (§3.4) — ohne einen einzigen Partner von dort |
| `IBIS` | 192 | 188 | Prozessname, Regel B |
| `IBISGUS` | 89 | 88 | dieselbe |
| `NXHBE` | 17 | **alle 17** | Projektname — der einzige Mandant mit Richtung und **ohne** jeden Partnervorschlag |
| `EDITIONLINGERI` | 9 | 5 | Prozessname, Regel B |
| `VOTG` | 390 | **keiner** | siehe unten |
| `ZAST`, `SUTTONS`, `WOC`, `SYSTEM` | 60 | 0 | kein Anker, weder im Projekt- noch im Prozessnamen |
| **Summe** | **1.490** | **1.020** | **68,5 %** |

**Dass `VOTG` keinen einzigen Richtungsvorschlag bekommt, ist kein Befund, sondern die
Entscheidung.** Die Richtung steht dort im `SOSName`, und dieser Weg ist in §9 ausdrücklich
verworfen — er bräuchte den Join auf `SOS` und eine Konfliktregel für die acht Prozesse mit
mehreren SOS. **Die 39 Projekte von `VOTG` werden von Hand gesetzt**, so wie §3.4 es ansagt.
Der Vermerk steht hier, damit die Null später niemand für eine Lücke hält —
sie ist eine Entscheidung, die man sieht.

**Eine Folge für E9:** Vollständig ohne jede Ableitung bleiben `SUTTONS`, `ZAST`, `WOC` und
`SYSTEM` — **nicht** `NXHBE` und `EDITIONLINGERI`, die E9 dafür nennt.

> **Zwei Einschränkungen, die gekennzeichnet bleiben.** Erstens: Dass Regel A auch bei **zwei**
> Unterstrichen trifft, ist für die neun NEXANS-Prozesse dieser Gestalt **nicht geprüft** — die
> Zuordnung stützt sich dort auf die Form, nicht auf einen Befund. Zweitens: `WOC` und `SYSTEM` sind
> laut `PROJEKTBESCHREIBUNG.md` §3.2 technische Mandanten und keine Kunden; ob ihre acht Prozesse
> überhaupt kuratiert werden sollen, ist offen (§10, Punkt 2).
>
> *Fundstellen berichtigt 20.08.2026:* Hier stand „laut §3.2" und „(§9)". Beides las sich als
> Verweis in diese Datei, und dort führt §3.2 die Regel A und §9 die verworfenen Möglichkeiten —
> gemeint waren die Mandantentabelle in `PROJEKTBESCHREIBUNG.md` §3.2 und der offene Punkt 2 in
> §10. Derselbe Fehler wie beim Korrekturkasten in §5, nur eine Datei weiter.

---

## 4. Die Pflegeliste

**E5 — Alle Prozesse des aktiven Mandanten**, auch die ohne Nachrichten (§1).

**E6 — Sortierung nach `ProjectID`, dann `ProcessID`.** Das im Plantext vorgesehene Aufkommen der
letzten 30 Tage entfällt: Es wäre eine Live-Aggregation über `Message` (Leistungsregel 2), von
[`prozessauswahl.md`](prozessauswahl.md) §9 schon einmal aus demselben Grund abgelehnt — und in der
Testkopie unsichtbar, weil dreißig Tage vor dem 08.07.2026 nur 285 Zeilen eines einzigen Mandanten
enthalten.

> **Eindeutig gemacht 20.08.2026.** Hier stand „Sortierung nach **Projekt und Name**". Der Satz
> lässt beide Lesarten zu — Kennung oder Name —, und **das ist der Fehler, nicht seine Auflösung**.
> Sortiert wird nach **`ProjectID`, dann `ProcessID`**.
>
> **Warum die Kennungen:** Beides sind Schlüssel und damit stabil. `ProcessName` ist eine
> prosaische Bezeichnung und über den Bestand **nicht eindeutig** — 460 verschiedene Werte auf
> 1.503 Prozesse *(vom Auftraggeber am 20.08.2026 genannt, in dieser Runde nicht nachgezählt)*.
> Eine Liste **ohne Paginierung** (E8), die nach einem nicht eindeutigen Feld ohne Zweitschlüssel
> sortiert, hat keine feste Reihenfolge.
>
> **Der Plantext trägt dieselbe Mehrdeutigkeit** — „Pflegeliste sortiert nach Projekt und Name",
> [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) Schritt 9b und Korrektur 5 desselben
> Abschnitts. Er wird ab hier über E6 gelesen; geändert ist er nicht.
>
> **Gemessen ist die andere Fassung.** L12 und M80‑1 sind mit `ORDER BY ProjectName, ProcessName`
> gefahren, die Entscheidung fiel danach. Nachgemessen ist sie nicht —
> [`prozess-katalog-backend.md`](prozess-katalog-backend.md) §9, Abweichung 2.

**E7 — Filter „nur offene" ist Pflicht, nicht Bequemlichkeit.** 733 Prozesse bei NEXANS. Eine
Sortierung ersetzt keinen Arbeitsmodus.

**E8 — Keine Paginierung.** L12 misst 4,2 ms beim größten Mandanten. Die Fortschrittszahl bekommt
deshalb **keine eigene Abfrage** — die volle Liste kommt zurück und wird vorne gezählt. Ein
`COUNT(*)` daneben führe denselben Filter ein zweites Mal.

**E9 — „Kein Vorschlag ableitbar" steht an zwei Stellen:** an der einzelnen Zeile, und einmal über
der Liste, wenn für **keine** Zeile des Mandanten etwas abgeleitet werden konnte. Das betrifft
`SUTTONS`, `ZAST`, `NXHBE` und `EDITIONLINGERI` vollständig. Ein leeres Feld ohne Erklärung wäre
nach Q4 nicht zulässig — der Nutzer muss nicht raten, ob nichts gefunden wurde oder nichts da ist.

### Fortschritt

Ungewichtete Zählung: wie viele Zeilen sind gepflegt, wie viele offen. **Nicht gewichtet nach
Aufkommen** — das behauptete eine Genauigkeit, die die Datenlage nicht hergibt.

**E10 — Abnahmekriterium.** Statt „Die Prozesse mit dem höchsten Aufkommen sind zugeordnet":
*Jeder Prozess des Mandanten ist entweder zugeordnet oder als gepflegt-ohne-Partner gekennzeichnet.*

---

## 5. Massenzuordnung nach Projekt

**E11 — Feldweise, nicht alle Felder zugleich.** Gemessen (M76):

| Mandant | Was das Projekt trägt | Wofür die Massenzuordnung taugt |
|---|---|---|
| NEXANS | Richtung und Rolle (`300_KundenEingehend`) | **Richtung.** Für den Partner unbrauchbar — innerhalb eines Projekts stehen durchgehend verschiedene Partner |
| VOTG | in 36 von 39 Projekten den Partner | **Partner**, aber nur für 157 von 390 Prozessen |
| SUTTONS | einen Partner für alle 17 | strukturell **wirkungslos** — ein Projekt, ein Wert |

> **Korrektur am Implementierungsplan, Schritt 9b.** Dort ist die Massenzuordnung nach Projekt als
> Hebel für den **Partner** benannt: „Bei Mandanten, deren Projekte den Partner tragen, wird das
> der Haupthebel." Gemessen greift sie für den Partner bei VOTG und für die **Richtung** bei
> NEXANS. *Der Hebel greift — aber nicht für das Feld, für das er dort benannt ist.*
>
> Und die Lücke des einen ist die Stärke des anderen: Die 226 VOTG-Prozesse unter
> `110_VTG_SalesInvoice`, die die Massenzuordnung nicht erreicht, deckt Regel A.
>
> **Fundstelle berichtigt 20.08.2026 — nicht stillschweigend umgehängt.** Dieser Kasten war an
> `PROJEKTBESCHREIBUNG.md` §4.4 adressiert. Der Satz steht dort nicht: §4.4 beschreibt die
> Namenskonventionen und entscheidet „es wird nicht geparst, es wird kuratiert" — von einem Hebel
> ist keine Rede. Die Korrektur trifft
> [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md), Schritt 9b, und steht seither auch
> dort (Korrektur 7 des Abschnitts).

**E12 — Sie überschreibt gepflegte Zeilen, mit Vorschau.** Der Schutzmodus „nur offene Zeilen"
machte genau die Korrektur unmöglich, für die man sie braucht: einem ganzen Projekt einen falschen
Partner in einem Zug richtigzustellen.

**Vorschau und Ausführung teilen ein Statement**, zwei Modi. Getrennt gebaut driften sie
auseinander, und der Nutzer bestätigt dann eine Zahl, die nicht die ist, die passiert. Die Vorschau
nennt die Zahl der betroffenen **gepflegten** Zeilen.

Größtes Projekt: 226 Prozesse bei VOTG, 161 bei NEXANS. Die Vorschau ist deshalb Pflicht und kein
Komfort.

---

## 6. Der Heuristik-Lauf

**E13 — Wiederholbar, per Knopf, für den aktiven Mandanten.**

Der Plantext sagt „einmaliger Import". Prozesse entstehen fortlaufend; ein einmaliger Lauf ließe
jeden neuen für immer unkatalogisiert — und niemand merkte es, weil „nicht zugeordnet" nach Q4 ein
zulässiger Zustand ist.

| | |
|---|---|
| **Legt an** | fehlende Zeilen |
| **Frischt auf** | offene Zeilen — auch solche, die schon einen Vorschlag tragen. Sonst friert der erste Lauf jeden späteren Regelfehler ein |
| **Rührt nie an** | Zeilen mit Status *gepflegt* |

**Nicht beim Anwendungsstart.** Er liefe bei jedem Neustart über 1.503 Zeilen, obwohl neue Prozesse
selten entstehen — und ein Schreibzugriff im Startpfad ist die Sorte Nebenwirkung, die man ein Jahr
später nicht mehr erwartet.

---

## 7. Mandantentrennung

Der Katalog liest `Process`, `Project` und `ProjectMandant` aus `GlassfishDB` und **erbt M1 bis M4
unverändert**: kein Endpunkt nimmt eine Mandanten-ID entgegen, `MandantContext` ist erster
Pflichtparameter jeder Repository-Methode, der Filter über `ProjectMandant` ist Bestandteil jedes
Statements, und je Endpunkt existiert ein Isolationstest.

Der Heuristik-Knopf läuft **für den aktiven Mandanten**, nicht für alle. Damit gilt dasselbe für ihn.

---

## 8. Was hier nicht gebaut wird

**Die Auswertung des Schweigens gehört nach Schritt 10.** „Seit wann kam von diesem Partner nichts"
ist kein `EXISTS`, sondern ein `MAX(MessageLastUpdate)` je Prozess über 3,34 Millionen Zeilen, bei
jedem Aufruf — Leistungsregel 2 unmittelbar. `message_rollup` aggregiert ohnehin stündlich je
Mandant, Prozess, Partner, Richtung und Status: **eine Lücke in den Stundenzeilen *ist* das
Schweigen.**

Zur Bauform gehört eine Unterscheidung: Die drei Problemkategorien aus §4.2 klassifizieren
**Nachrichten**. Schweigen hat keine Zeile, die man klassifizieren könnte. Es ist deshalb keine
vierte Kategorie neben Fehler, Überfällig und Unquittiert, sondern eine andere Achse mit eigenem
Mechanismus.

Ebenfalls nicht: eine Partner-Pflegeoberfläche (E2), Standort und Belegart (E1), eine
Ableitung der Richtung aus dem `SOSName` (§3.4).

---

## 9. Verworfene Möglichkeiten

Mit Grund festgehalten, damit sie in der Umsetzung nicht wieder aufkommen.

| Verworfen | Grund |
|---|---|
| Pflegeliste nur für Prozesse **mit** Nachrichten | Sie war am 20.08.2026 entschieden und noch am selben Tag umgekehrt. Die stille Prämisse war, ein Monitoring zeige, was passiert ist. Siehe §1 |
| Sortierung nach Aufkommen | E6 |
| Eigene Tabelle `partner` | E2 |
| Dritter Pflegestatus „nicht zuordenbar" | E4 — „gepflegt mit leerem Partner" sagt dasselbe |
| Massenzuordnung nur auf offene Zeilen | E12 |
| Heuristik beim Anwendungsstart | E13 |
| Richtung aus dem `SOSName` | §3.4 — Join und Konfliktregel für acht Prozesse mit mehreren SOS |
| Gespeicherte Spalte „trägt Nachrichten" | Sie ginge still veraltet; mit E5 ist sie ohnehin gegenstandslos |
| Katalogfeld „Belegart" umbenennen | E1 — das Feld entsteht nicht |

---

## 10. Offene Punkte

| | |
|---|---|
| 1 | **`ZAST` (35), `NXHBE` (17), `EDITIONLINGERI` (9) — 61 Zeilen ohne Verfahren.** `ZAST` trägt bei **allen 35** genau einen Unterstrich, `NXHBE` bei 14 von 17 genau zwei — das sind durchgängigere Muster als bei NEXANS. Ob sich daraus eine dritte Regel ergibt, entscheidet ein Blick auf die 61 Namen, nicht eine Messung |
| 2 | Sollen `WOC` (4) und `SYSTEM` (4) überhaupt kuratiert werden? Beide sind laut `PROJEKTBESCHREIBUNG.md` §3.2 technisch und keine Kunden |
| 3 | Regel A bei **zwei** Unterstrichen ist für die neun NEXANS-Prozesse dieser Gestalt nicht geprüft |
| 4 | Der Anker `Eingehend`/`Ausgehend` in Projektnamen war für `ZAST`, `NXHBE`, `EDITIONLINGERI`, `WOC` und `SYSTEM` nicht erhoben. **Vier der fünf sind mit M80‑7 beantwortet** (20.08.2026): Er wirkt bei `NXHBE` für **alle 17** Prozesse und bei `ZAST`, `WOC` und `SYSTEM` **gar nicht**. **Offen bleibt allein `EDITIONLINGERI`** — seine fünf Zeilen mit Richtung sind genau die fünf, die Regel B über den **Prozess**namen trifft; über den Projektnamen sagt die Zählung dort nichts |
