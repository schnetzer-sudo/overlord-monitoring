# Die Sollängen-Kuratierung

Stand: 13.08.2026 · Schritt 7, Teil 2a · Erhebung:
[`messungen-schritt7.md`](messungen-schritt7.md) **M46** · Migration:
`backend/src/main/resources/db/migration/V5__bam_sollaenge.sql`

Diese Datei beschreibt die Tabelle `overlord_monitor.bam_sollaenge`: warum es sie gibt, wie ihre
sechzehn Zeilen entstanden sind, wo die Regel dahinter an ihre Grenze stößt, und wie sie gegen
Veralten gesichert ist. **Sie beschreibt keinen Endpunkt.** Die Suche, die sie benutzt, entsteht in
Teil 2b ([`bam-suche.md`](bam-suche.md)); die Oberfläche in Teil 3.

> ### 📌 Nachgetragen am 13.08.2026 — die Tabelle wird seit Teil 2b benutzt
>
> **Sie ist gebaut**: `GET /api/bam/suche` liest sie als **Vollabzug für den Mandanten der Sitzung**
> und bildet daraus die Suchvarianten — vollständig in [`bam-suche.md`](bam-suche.md) §3.
> **An der Tabelle ändert sich nichts**, weder am Schnitt noch am Inhalt; die zwei wirkungslosen
> Einträge bleiben, 9006 kommt nicht dazu. Beides sind weiterhin Entscheidungen des Auftraggebers.
>
> **Was M47 gemessen und dabei an dieser Datei korrigiert hat, steht in §8.**

---

## 1. Wozu

Der Nutzer hat einen Beleg vor sich und tippt die Nummer ab. Führende Nullen stehen auf dem Beleg
nicht — im Bestand sehr wohl. Die exakte Suche findet dann nichts, und **eine leere Trefferliste
sieht aus wie „gibt es nicht", nicht wie „falsch getippt"**. Das ist die Leitfrage des Werkzeugs,
verfehlt.

Wie groß das ist, hat M43‑4 an echten Werten gemessen: Bei **sechs von acht** geprüften Typen findet
die rohe Fassung **null** Treffer und die auf Sollänge aufgefüllte die richtigen. Nicht „weniger" —
**keine**.

Damit die Suche auffüllen kann, braucht sie eine Sollänge. Die wird **kuratiert, nicht geraten**
(Regel Q4): Sie kommt aus einer Messung über den Bestand, und die Messung steht mit Datum und
Fallzahl neben jeder Zeile.

---

## 2. Der Schlüssel trägt den Mandanten — und das ist gemessen

Es liegt nahe, die Sollänge als Eigenschaft des **Typs** zu behandeln. Sie ist es nicht. M46‑2 hat es
an **drei unabhängigen Stellen** widerlegt:

| Fall | Was gemessen ist | Was ein typweiter Schlüssel getan hätte |
|---|---|---|
| **Typ 2000** (OrderNumber) | `SUTTONS` dominiert mit Länge **6** (97,33 %), `VOTG` mit Länge **7** (84,06 %) | Eine Sollänge von 6 für `VOTG` — schlicht falsch |
| **Typ 9014** (Lieferantennummer beim Kunden) | über den Bestand **58,45 %**, bei `WOC` aber **95,21 %** bei 79,15 % führender Null | Den Eintrag verworfen, der für `WOC` der nützlichste ist |
| **Typ 2001** (VendorReference) | bei `VOTG` 100 % Dominanz und **null** Werte mit führender Null | Über die Gesamtdominanz (79,12 %) entschieden und die 8 Zeilen nie gesehen |

Deshalb `PRIMARY KEY (mandant_id, message_bam_type)`. **Das ist der teurere Schnitt und der
einzige, der alle drei Fälle richtig trifft.**

> **Belegvermerk (Regel L10).** *Gemessen:* die dominante Länge je (Mandant, Typ) über den gesamten
> Bestand, für alle 36 Typen mit führender Null — 45 Paare, rund 11,4 Mio. Zeilen (M46‑2).
> *Behauptet wird:* dass der Mandantenschnitt dauerhaft der richtige ist. **Die Lücke:** Gemessen
> sind drei Abweichungen bei 45 Paaren. Ob der Schnitt auch dann noch nötig wäre, wenn diese drei
> Typen verschwänden, ist nicht gemessen — und es ist die falsche Frage: Ein Schlüssel, der einmal
> zu grob war, wird nicht dadurch richtig, dass er es meistens nicht ist.

---

## 3. Die Regel

Ein Paar aus Mandant und BAM-Typ bekommt eine Sollänge, wenn **beides** gilt:

1. Der Typ trägt bei diesem Mandanten **überhaupt** einen Wert mit führender Null. Ohne das wäre
   Auffüllen eine Rechenvorschrift ohne Anlass.
2. Seine häufigste Länge stellt über den **Bestand** mindestens **95 %** der Zeilen dieses Paares.

Die Sollänge ist dann diese häufigste Länge.

**Woher die 95 % kommen.** Aus dem 2001-Fall (M43): Dort sagte Fenster B 100 % führende Null bei
einer einzigen Länge, der Bestand 67,21 % bei vier Längen. Eine aus einem Monat abgeleitete
Kuratierung hätte für ein Drittel des Bestands eine Länge erzwungen, die es dort nie gab. Die
Schwelle fängt diesen Fall — 2001 kommt über den Bestand auf 79,12 % Längendominanz.

**Bezugsgröße ist die Dominanz über *alle* Werte des Paares**, nicht die innerhalb einer
Schreibweisengruppe. Der Grund ist die Anwendung: Sie füllt eine **Eingabe** auf und weiß nicht, ob
der gesuchte Wert im Bestand mit oder ohne Null steht. Maßgeblich ist deshalb, ob die Sollänge für
das ganze Paar gilt.

> ⚠️ **Das ist eine Festlegung und keine Messung.** Die drei möglichen Lesarten fallen auseinander:
> 9020 hat über alle Werte 82,31 %, in der Gruppe ohne Null aber 98,78 %; 9015 hat 58,51 % gegen
> 97,56 % in der Gruppe mit Null. Nach Gruppendominanz sähe die Tabelle anders aus. Die Zahlen für
> beide Alternativen stehen in M46‑1; die Frage steht als offener Punkt (§8).

---

## 4. Was in der Tabelle steht

Sechzehn Zeilen: vierzehn mit einer Sollänge, zwei mit dem Kennzeichen für ein führendes Leerzeichen.

### 4.1 Die vierzehn Sollängen

„Dominanz" ist der Anteil der Sollänge an allen Zeilen des Paares, „f. Null" der Anteil der Werte mit
führender Null, „n" die Zeilenzahl des Paares über den Bestand.

| Mandant | Typ | Beschreibung | Sollänge | Dominanz | f. Null | n | wirkt |
|---|---:|---|---:|---:|---:|---:|---|
| `VOTG` | 2002 | InvoiceNumber VTG | 10 | 100,00 % | 100 % | 456 | ja |
| `VOTG` | 2005 | CustRef1 | 10 | 100,00 % | 100 % | 9 | ja |
| `VOTG` | 2007 | LoadNo | 10 | 100,00 % | 100 % | 9 | ja |
| `NEXANS` | 9009 | Beleg-Nr. GS_L_SAP | 10 | 100,00 % | 34,35 % | 17.913 | ja |
| `NEXANS` | 9011 | Anlieferungs-Nr. ae_L_SAP | 10 | 100,00 % | 100 % | 1.412 | ja |
| `NEXANS` | 9012 | Charge_L_SAP | 10 | 100,00 % | 100 % | 17.934 | ja |
| `NEXANS` | 9013 | Nr. TSL_L_SAP | 10 | 100,00 % | 9,48 % | 6.148 | ja |
| `NEXANS` | 9024 | Rechnungsnummer_K_SAP | 10 | 100,00 % | 100 % | 25.359 | ja |
| `NEXANS` | 9021 | Transportnummer_K_SAP | 10 | 99,24 % | 94,67 % | 55.250 | ja |
| `NEXANS` | 9036 | Lagerort Kunde_L_SAP | 4 | 99,23 % | 98,94 % | 22.847 | ja |
| `NEXANS` | 9000 | Abladestelle_L_SAP | 3 | 96,68 % | 32,05 % | 151.063 | ja |
| **`WOC`** | **9014** | Lieferantennummer beim Kunden_K_SAP | **6** | **95,21 %** | 79,15 % | 2.067 | ja |
| **`IBIS`** | **1** | Auftragsnummer | 7 | 99,03 % | *1 Zeile* | 155.875 | **nein** |
| **`SUTTONS`** | **2000** | OrderNumber | 6 | 97,33 % | 1,24 % | 12.296 | **nein** |

### 4.2 Die zwei Leerzeichen-Kennzeichen

`utf8mb4_general_ci` ist eine **PAD SPACE**-Kollation: `'123 ' = '123'` ist wahr, `' 123' = '123'`
ist falsch (M43‑3). **Folgende** Leerzeichen brauchen deshalb keine Behandlung, **führende** schon.

| Mandant | Typ | Beschreibung | führendes Leerzeichen | n |
|---|---:|---|---:|---:|
| `NEXANS` | 9018 | Kundenmaterialnummer_K_SAP | **31.193** — 1,349624 % | 2.311.236 |
| `NEXANS` | 9020 | Lieferschein, Entnahme, PUS_K_SAP | **1** — 0,000100 % | 998.686 |

Beide tragen **keine** Sollänge: Ihre Längendominanz liegt bei 43,61 % und 82,31 % und damit weit
unter der Schwelle. 9018 ist ausgerechnet der Typ, der bei `NEXANS` auf 92,26 % der Wurzeln sitzt
(M39) — der naheliegendste Suchtyp trägt also die Leerzeichen und keine Sollänge.

**Über den ganzen Bestand tragen nur diese beiden Typen ein führendes Leerzeichen**, und es gibt in
`MessageBAM` **keinen einzigen** leeren Wert (M46‑3).

### 4.3 Was *nicht* in der Tabelle steht — und warum das die interessantere Hälfte ist

Von 36 Typen mit führender Null fallen **23** durch die Schwelle. Der Auftrag nennt das keinen
Mangel, und für die meisten stimmt das: 9003 (17,95 % Dominanz über 35 Längen), 9038 (24,07 %),
9016 (27,29 %) — dort gibt es schlicht keine Sollänge, und `LPAD` wäre eine Rechenvorschrift ohne
Bezugsgröße. Drei Fälle verdienen den Namen trotzdem:

| Typ | Dominanz | f. Null | warum er wehtut |
|---:|---:|---:|---|
| **9006** Lieferschein-Nr._L_SAP | **94,21 %** | 32,99 % | **0,79 Prozentpunkte unter der Schwelle** — und der einzige Typ, für den M43‑4 die Wirkung an echten Werten belegt hat: roh **1.642** Treffer, aufgefüllt **4**. Er ist zudem seit Schritt 4 als Listenspalte kuratiert |
| **9020** Lieferschein, Entnahme, PUS | 82,31 % | 18,10 % | 180.752 Zeilen mit führender Null, davon 92,13 % auf Länge 10. In der Gruppendominanz käme er hinein |
| **9015** Kundenwerk_K_SAP | 58,51 % | 25,34 % | 110.403 Zeilen mit führender Null, davon **97,56 %** auf Länge 3 |

**Die Schwelle zu senken hilft bei 9006 nicht.** Bei 94 % kämen 9028, 9029 und 9004 mit, und die
tragen 0,21 %, 0,21 % und 5,71 % führende Nullen — drei Einträge ohne Anlass für einen mit. Entweder
9006 wird **namentlich** aufgenommen, dann ist die Regel eine Regel plus Liste, oder die Suche findet
dort ohne Auffüllen 1.642 statt 4 Treffer. **Das ist eine Entscheidung und hier nicht getroffen**
(§8).

---

## 5. Zwei Einträge, die nachweislich nichts finden

**Die Regel kennt die Wirksamkeit nicht.** Sie fragt nach der Längendominanz. Ob auf dieser Länge
überhaupt ein Wert **mit** führender Null steht, fragt sie nicht — und bei zwei der vierzehn Paare
steht dort keiner (M46‑1c, M46‑2c):

| Paar | Sollänge | Wo die Werte mit führender Null wirklich liegen |
|---|---:|---|
| `IBIS`/1 | 7 | Länge **8** — und es ist **eine einzige** Zeile im ganzen Bestand |
| `SUTTONS`/2000 | 6 | Längen **12** (138 Zeilen), **8** (30), **10** (7) |

Beide Zeilen stehen trotzdem in der Tabelle. **Die Befüllung ist mechanisch, und sie wird nicht
nachgebessert** — sonst stünde in der Migration eine Regel, die nirgends aufgeschrieben ist. Was die
beiden kosten, ist eine zusätzliche Variante je Suche auf diesen Typen; was sie einbringen, ist
nichts. Ob die Regel um die Bedingung ergänzt wird, steht als offener Punkt (§8).

---

## 6. Die Tabelle

```sql
CREATE TABLE bam_sollaenge (
  mandant_id             VARCHAR(36)      NOT NULL,
  message_bam_type       SMALLINT         NOT NULL,
  sollaenge              TINYINT UNSIGNED NULL,
  fuehrendes_leerzeichen BOOLEAN          NOT NULL DEFAULT FALSE,
  dominanz_prozent       DECIMAL(5,2)     NULL,
  leerzeichen_prozent    DECIMAL(9,6)     NULL,
  zeilen                 INT              NOT NULL,
  gemessen_am            DATE             NOT NULL,
  PRIMARY KEY (mandant_id, message_bam_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

| Entscheidung | Begründung |
|---|---|
| `message_bam_type SMALLINT` | Gegen `information_schema` **erhoben**, nicht übernommen (Regel L8): `GlassfishDB.MessageBAM.MessageBAMType` und `GlassfishDB.MessageBAMType.MessageBAMType` sind beide `smallint(6) NOT NULL` (M46‑0) |
| `mandant_id VARCHAR(36)` | Passend zu `GlassfishDB.Mandant.MandantID`, wie in `bam_spalte`. Die MandantID ist ein lesbarer Code, keine UUID |
| `sollaenge TINYINT UNSIGNED NULL` | Werte zwischen 1 und 70 (`MessageBAMValue` ist `varchar(70)`, M32). **Nicht `TINYINT(1)`** — der Codegen bildet genau das auf `Boolean` ab; `tinyint(3) unsigned` bleibt eine Zahl. `NULL` heißt „für dieses Paar wird nicht aufgefüllt" |
| `utf8mb4` / `utf8mb4_general_ci` **explizit** | §5 der Projektbeschreibung. Nie geerbt, sonst bricht der schemaübergreifende Join, sobald der Server-Default sich ändert oder die Instanz auf MariaDB 11 gehoben wird |
| Keine `_bin`-Spalte | Hier steht nichts Tokenartiges (§5) |
| **Kein Fremdschlüssel** auf `GlassfishDB` | §6. Verschwindet ein Typ im Altsystem, bleibt die Kuratierung bestehen, statt rückwirkend zu verschwinden — dieselbe Regel wie bei `bam_spalte` und `process_catalog` |
| `zeilen` als eigene Spalte | Regel L10: „`n` sichert den Umfang, der Vermerk den Schluss". Eine Dominanz von 100 % auf 9 Zeilen ist etwas anderes als dieselben 100 % auf 793.588 |

### Zugriffspfad

`overlord_monitor.bam_sollaenge` wird **nur gelesen**, über den Primärschlüssel oder als
Vollabzug (16 Zeilen). Sie enthält keine Quelldaten und wird nie gegen `GlassfishDB` gejoint — die
Normalisierung in Teil 2b bildet aus ihr die Suchvarianten **vor** dem Statement, nicht darin.

> **Für Teil 2b:** Der generierte Typ von `sollaenge` ist `org.jooq.types.UByte`; die Sollänge kommt
> über `.intValue()`. Das ist der Preis für `TINYINT UNSIGNED` und hier bewusst gezahlt.

### Abgrenzung gegen `bam_spalte`

Zwei Tabellen, zwei Fragen, zwei Schlüssel — sie werden nicht zusammengelegt:

| | `bam_spalte` (V4, Schritt 4) | `bam_sollaenge` (V5, Schritt 7) |
|---|---|---|
| Frage | Welche zwei BAM-Werte zeigt die **Liste** als Spalten? | Auf welche Länge füllt die **Suche** auf? |
| Schlüssel | `(mandant_id, position)` | `(mandant_id, message_bam_type)` |
| Befüllt für | nur die Ausnahme (`NEXANS`) | jedes Paar, das die Regel trifft |

---

## 7. Sicherung

### 7.1 Der Drift-Test

`BamSollaengeDriftDbIT` (`@Tag("db")`, Paket `bam`), gebaut wie
`DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge`. Zwei Prüfungen:

1. **Sollängen-Drift** — je kuratiertem Eintrag: Ist die kuratierte Länge über den **Bestand** noch
   die häufigste, und stellt sie noch mindestens **95 %** der Zeilen? Fällt eines von beidem, wird
   der Test rot und nennt Paar, Ist-Wert, Soll-Wert und `n`.
2. **Leerzeichen-Drift** — je gesetztem Kennzeichen: Gibt es im Bestand überhaupt noch einen Wert mit
   führendem Leerzeichen? Geprüft mit `LIMIT 1`, weil die Existenz gefragt ist und nicht die Zahl.

**Eine Abfrage je Paar, nicht eine über alle.** Die Sammelform aus M46‑2 kostet 6,8 s und ist im
Lese-Pool gestorben — der setzt `SET SESSION max_statement_time=10`
([`datenzugriff.md`](datenzugriff.md) §1). Je Paar bleibt jede Abfrage weit darunter; der ganze Test
läuft in **18,2 s**.

**Ohne Zeitfenster, und das ist hier richtig** (Regel L9): Gefragt ist, ob die Sollänge über den
*Bestand* hält. Ein Fenster blendete genau die Zeiträume aus, in denen sie sich geändert haben
könnte.

**Die Gegenprobe ist gelaufen**, beide Zweige einzeln:

| Verfälschung von Hand | Ergebnis |
|---|---|
| `NEXANS`/9036 `sollaenge` 4 → 5 | rot: *„die häufigste Länge im Bestand ist 4, kuratiert ist 5"* |
| `NEXANS`/9020 `sollaenge` NULL → 8 | rot: *„die Dominanz der Länge 8 ist auf 82.31 % gefallen … Unter 95 % trägt die Sollänge nicht mehr"* |
| beide zurückgesetzt | grün, 2 Tests, 18,2 s |

> ⚠️ **Der Vorbehalt, und er gehört hierhin und nicht in eine Fußnote.** Dieser Test **läuft nicht in
> der CI** — sie erreicht das interne Netz nicht und schließt die Gruppe `db` über
> `-DexcludedGroups=db` aus. Er läuft, wenn jemand ihn laufen lässt. Das ist **dieselbe
> Garantiestufe wie beim Statustest**, die das Projekt seit Schritt 2 bewusst akzeptiert. Ein Test,
> der nur lokal läuft, ist mehr als kein Test und weniger als eine Zusicherung.

**Und was er nicht kann:** Er prüft, was dasteht — **nicht, was fehlt**. Ein Mandant, der morgen
einen Typ mit durchgängig führender Null bekommt, wird ohne Sollänge gesucht, und der Test bleibt
grün.

### 7.2 Handkuratierte, nicht wiederherstellbare Daten

`bam_sollaenge` ist **kuratiert und nicht ableitbar**. Sie lässt sich zwar aus einer Messung
*neu erzeugen* — aber nur, solange der Bestand dieselbe Gestalt hat; eine Zeile, die einmal von Hand
korrigiert wurde, ist unwiederbringlich. Dieselbe Kategorie wie `process_catalog` (Schritt 9) und
`partner`.

> 📌 **Befund, 13.08.2026: Eine Sicherungsregel für diese Kategorie gab es nicht.** Der Auftrag zu
> Teil 2a verlangt, die Tabelle „in dieselbe Sicherungsregel einzutragen und keine zweite
> anzulegen". Geprüft über `docs/`, das Wurzelverzeichnis und die Migrationen: **es gab keine.**
> `process_catalog` selbst existiert noch nicht (Schritt 9). Statt eine zweite Regel anzulegen,
> entsteht die eine hier — und führt von Anfang an beide Tabellen.

**Die Regel.** Tabellen in `overlord_monitor`, deren Inhalt von Hand gepflegt und nicht aus einer
Quelle wiederherstellbar ist, werden vor jedem Schemaeingriff gesichert und stehen in dieser Liste.
Es gibt genau **eine** Liste, und sie steht hier:

| Tabelle | seit | Inhalt | wiederherstellbar aus |
|---|---|---|---|
| `bam_sollaenge` | Schritt 7 (V5) | Sollänge und Leerzeichen-Kennzeichen je (Mandant, Typ) | einer Neumessung nach M46 — **nur bei unverändertem Bestand** |
| `process_catalog` | Schritt 9 *(noch nicht angelegt)* | Partner, Standort, Richtung, Belegart je `ProcessID` | **gar nicht** — Heuristik befüllt vor, die Wahrheit ist gepflegt |
| `partner` | Schritt 9 *(noch nicht angelegt)* | kuratierte Partnerstammdaten | **gar nicht** |

Nicht in dieser Liste: `app_user` (Konten, aber über `POST /api/admin/users` neu anlegbar),
`audit_log` (Protokoll, wächst von selbst), `message_rollup` (aus `Message` neu berechenbar),
`saved_view` (Bequemlichkeit), `SPRING_SESSION` (flüchtig).

---

## 8. Offene Punkte

1. **Bekommt 9006 eine benannte Ausnahme?** 94,21 % — 0,79 Prozentpunkte unter der Schwelle, bei
   32,99 % führender Null und der einzigen an echten Werten belegten Wirkung (M43‑4). Senken der
   Schwelle hilft nicht (§4.3). Entweder namentlich aufnehmen oder 1.642 statt 4 Treffer hinnehmen.
2. **Wird die Regel um die Wirksamkeitsbedingung ergänzt?** `IBIS`/1 und `SUTTONS`/2000 können
   nachweislich nichts finden (§5).
3. **Welche der drei Lesarten der 95-Prozent-Regel ist die richtige?** Über alle Werte, über die
   Gruppe mit Null, oder über die größere Gruppe. Die Festlegung auf die erste ist begründet, aber
   nicht gemessen (§3).
4. **Wie erfährt die Kuratierung von einem neuen Mandanten oder Typ?** Der Drift-Test bemerkt es
   nicht (§7.1).
5. ~~**Was kostet eine Variante, die nichts trifft?** Ungemessen — M46 misst Verteilungen, keine
   Suchen. Die Zahl gehört in M47 (Teil 2b).~~ ✔ **Gemessen am 13.08.2026 in M47: rund 0,1 ms.**
   Von einer auf zwei Fassungen kostet dieselbe Suche 0,956 → 1,131 ms, von einer auf fünf an einem
   Wert mit 2.256 Treffern 20,228 → 22,439 ms. **Damit ist die Frage 2 keine Leistungsfrage mehr**,
   sondern nur noch die, ob die Regel sagen soll, was sie meint. Sie bleibt offen.

### Was Teil 2b an dieser Datei sichtbar gemacht hat

*Nachgetragen am 13.08.2026 nach M47. Keine dieser drei Zeilen ändert die Tabelle.*

1. **Die Zahl der Sollängen von `NEXANS` ist gezählt worden, und sie ist eine andere als im
   Auftrag.** Der Auftrag zu Teil 2b nennt „elf Sollängen, aber nur drei verschiedene". Die Tabelle
   führt für `NEXANS` **zehn Zeilen**, davon **acht mit einer Sollänge** und zwei nur mit dem
   Leerzeichen-Kennzeichen. **Die Zahl der verschiedenen ist mit drei richtig** (10, 4 und 3), und
   nur auf sie kommt es an — die Normalisierung bildet aus den *verschiedenen* Sollängen. §4.1 ist
   damit bestätigt und nicht geändert.
2. **Die Obergrenze der Variantenzahl ist damit belegt.** Bei `NEXANS` — dem Mandanten mit den
   meisten kuratierten Zeilen — sind es **höchstens fünf** gesuchte Fassungen, und die erreicht nur
   eine ein- oder zweistellige Eingabe: drei aufgefüllte, die rohe und die mit Leerzeichen. Für eine
   siebenstellige Eingabe sind es drei, für eine zehnstellige zwei.
3. **`sollaenge IS NULL` ist im Endpunkt kein Sonderfall, sondern ein Zweig.** Die beiden Zeilen aus
   §4.2 erzeugen die Leerzeichen-Fassung und **keine** aufgefüllte. Das trifft ausgerechnet 9018 —
   den Typ, der bei `NEXANS` auf 92,26 % der Wurzeln sitzt (M39) und damit den naheliegendsten
   Suchtyp überhaupt. **Für ihn wird nie aufgefüllt**, und das ist die praktische Folge der
   95-Prozent-Regel, die §4.3 beschreibt.

---

## 9. Was diese Datei nicht beschreibt

- **Den Suchendpunkt.** `GET /api/bam/suche`, seine Parameterform, das Zeitfenster und die
  Abfrageform stehen in [`bam-suche.md`](bam-suche.md) (Teil 2b, seit 13.08.2026 gebaut).
- **Wie die Varianten gebildet werden.** Roh, aufgefüllt, mit führendem Leerzeichen — das ist die
  Normalisierung des Endpunkts und nicht die Kuratierung; sie steht in
  [`bam-suche.md`](bam-suche.md) §3 samt der Begründung, warum die Leerzeichen-Fassung gesucht und
  **nicht gemeldet** wird.
- **Welche Typen die Oberfläche zur Auswahl anbietet.** Das ist Teil 3.
