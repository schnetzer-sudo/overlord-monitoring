# Messungen vor Schritt 5

Erhebung gegen die **Testkopie** am 07.08.2026, vor dem Bau des Nachrichtendetails (Regel L7).
Gegenstück zu [`messungen-schritt4.md`](messungen-schritt4.md).

> **Was dieses Dokument ist und was nicht.** In **M14 bis M17** stehen erhobene Zahlen. Geschlossen
> wird ausschließlich an den **drei Stellen, an denen die Aufgabenstellung eine Schlussfolgerung
> ausdrücklich verlangt** — M15 gesamt, M16 (2) und (3), M17 (3). Sie sind als
> „Schlussfolgerung — ausdrücklich" gekennzeichnet und tragen je einen eigenen Absatz zu ihrer
> Schwachstelle. Alles andere, was aus den Zahlen folgen könnte, steht unter „Offene Entscheidungen"
> als **Frage**, nicht als Antwort.
>
> **Der Anlass.** Der Plantext zu Schritt 5 beauftragt eine handgepflegte Zuordnungstabelle, die
> `SOSActionServiceProperties` in Klartext übersetzt. Er stammt aus der Zeit vor
> [M13](messungen-schritt4.md#m13--trägt-sosactionid-einen-lesbaren-namen), das gezeigt hat, dass
> `SOSAction.SOSActionName` bereits durchgängig gepflegt und lesbar ist. Damit stand nicht mehr die
> Frage im Raum, *wie* übersetzt wird, sondern **ob eine `MessageAction`-Zeile diesen Namen
> überhaupt erreicht**. M15 beantwortet sie.

---

## 0. Rahmen der Messung

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` (Ubuntu 22.04) — niemals die Produktion |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**. Erste Abfrage der Sitzung, vor jedem anderen Statement |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Serverzeit zu Beginn der Messung | `2026-08-07 12:24:10` (`UTC_TIMESTAMP` `10:24:10`, also UTC+2) |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `SHOW PROFILES` — nie um den Client-Aufruf herum |
| Wiederholungen | beste von fünf Läufen nach einem Aufwärmlauf; bei Statements über zehn Sekunden beste von drei. Wo N kleiner als fünf ist, steht es dabei |
| Zugangsdaten | ausschließlich aus Umgebungsvariablen (`OVERLORD_DB_*`), an den Client über `MYSQL_PWD` übergeben. Nie in eine Datei, nie auf die Kommandozeile |

### `SHOW GRANTS FOR CURRENT_USER()`

Drei Zeilen, wörtlich bis auf den Passwort-Hash der ersten, der hier nach Regel G1 **nicht**
abgedruckt wird:

```
GRANT USAGE ON *.* TO `monitor_read`@`%` IDENTIFIED BY PASSWORD '<Hash>'
GRANT SELECT ON `GlassfishDB`.* TO `monitor_read`@`%`
GRANT SELECT ON `overlord_monitor`.* TO `monitor_read`@`%`
```

`SELECT` auf beide Schemata, sonst nichts — unverändert gegenüber
[M4](messungen-schritt4.md#m4--verhalten-der-view-messagemandantid). Insbesondere kein `SHOW VIEW`.

### Abweichung: der Kommandozeilen-Client

Wie in Schritt 4 gelaufen ist der `mysql`-Client `8.0.46` aus MySQL Workbench mit
**`--ssl-mode=DISABLED`** (der Server bietet kein TLS, Annahme A11).

**Neu gegenüber Schritt 4:** Auf diesem Rechner ist inzwischen zusätzlich ein
`mariadb`-Client `12.3.2` installiert. Er wurde **nicht** verwendet — die Zahlen dieser Erhebung
sollen mit denen aus `messungen-schritt4.md` vergleichbar bleiben, und ein Clientwechsel mitten in
einer Messreihe wäre eine Variable ohne Nutzen. Dass er verfügbar ist, gehört trotzdem hierhin: Die
in Schritt 4 dokumentierte Not, `--skip-ssl` durch `--ssl-mode=DISABLED` ersetzen zu müssen, besteht
für künftige Erhebungen nicht mehr.

### Bezugsfenster

**Fenster A — der dichte Tag.** `MessageLastUpdate >= '2025-12-29 00:00:00'` und
`< '2025-12-30 00:00:00'`. **6.249 Nachrichten**, **20.352 `MessageAction`-Zeilen**, **141.037
`MessageProperty`-Zeilen**. Standardfenster für alle Statements.

**Fenster B — der dichte Monat.** `>= '2025-11-30 00:00:00'` und `< '2025-12-30 00:00:00'`.
**214.330 Nachrichten**, **684.075 `MessageAction`-Zeilen**, **4.904.303 `MessageProperty`-Zeilen**.
Nur zur Verbreiterung der Auflösungsquoten in M15 und als Gegenprobe zur Wertgröße in M17.

### Warum kein Mandantenfilter — und was das an Fenster B ändert

Abweichend von L8/L11/L13/L14 ist in M14 bis M17 **kein Mandantenfilter** gesetzt. Die Fragen dieser
Erhebung betreffen das Datenmodell, nicht den Ausschnitt eines Kunden; ein Filter auf `NEXANS`
verengte die Vielfalt der Abläufe und machte die Auflösungsquoten unbrauchbar. Gerade der Vergleich
der ausgeführten mit den geplanten Bausteinen lebt davon, dass Abläufe mehrerer Mandanten darin
vorkommen.

**Daraus folgt eine Abweichung von der Aufgabenstellung, die benannt gehört.** Sie beschreibt
Fenster B als „Mandant `NEXANS`, rund 180.251 Nachrichten" — das ist die Bezugsmenge aus
[M11](messungen-schritt4.md#m11--wie-oft-ist-welcher-bam-typ-überhaupt-befüllt) und
[M12](messungen-schritt4.md#m12--zwischenschritte-je-mandant). Da die Regel „kein Mandantenfilter"
ausdrücklich für M14 bis M17 gilt und begründet ist, wiegt sie hier schwerer als die übernommene
Beschreibung. Fenster B ist deshalb **ohne** Mandantenfilter gelaufen und enthält damit
**214.330 statt 180.251** Nachrichten — dieselbe Zahl, die
[L13](messungen-schritt4.md#l13--freitext-über-lange-fenster) für dieses Fenster nennt.

### Betriebsregeln und eine bewusste Abweichung davon

Eingehalten wurden: kein `LIKE '%…%'` über `SOSActionServiceProperties` (M8 maß dafür 97,976 s);
Zugriff auf `MessageProperty` ausschließlich über `MessageID` aus einem Zeitfenster auf `Message`;
kein Statement über 60 Sekunden.

**Eine Abweichung, offen ausgewiesen.** Die Gegenprobe in M16 (3) — „gibt es im Gesamtbestand
überhaupt eine offene Aktion?" — läuft über `MessageAction` **ohne** eingegrenzte `MessageID`-Menge
und verstößt damit gegen Betriebsregel 1. Sie ist trotzdem gelaufen, weil sie die Leitfrage von M16
(„woran ist der Hänger erkennbar?") als einzige beantwortet: Ein Fenster, das keine offene Aktion
enthält, sagt nichts darüber, ob es sie gibt. Der Preis ist gemessen und klein — **3,013 s** für den
Zähler, **23,144 s** für die Zuordnung nach Status, beides auf einer Tabelle von 2,8 GB und nicht
auf den 61 GB von `MessageProperty`. Beide Statements sind **kein Muster für Anwendungscode**.

> **Regel L4 gilt in M17 sinngemäß:** Die Längen- und Gestaltauswertung über
> `MessagePropertyValue` ist eine **einmalige Erhebung über eine eingegrenzte Menge und kein Muster
> für Anwendungscode.** Im Betrieb wird über diese Spalte niemals gefiltert, gruppiert oder
> sortiert.

### Anonymisierung

Keine Belegnummer, kein `MessagePropertyValue`-Inhalt und kein Partnername steht in diesem Dokument.
Wo die Gestalt eines Werts zu beschreiben war, ist sie **beschrieben und nicht abgedruckt** (M17 4).

Zwei Stellen brauchen eine ausdrückliche Begründung:

- **Dienstnamen (M15 3)** tragen in Klammern eine Knotenkennung, die wie ein Adressfragment eines
  internen Servers aussieht. Diese Kennungen sind nach Regel G1 durchgängig als `<Knoten>` maskiert.
- **Bausteinmarken (M15 4)** sind unmaskiert abgedruckt. Geprüft: Unter den zwanzig häufigsten
  kommt **kein Partnername** vor. Die Präfixe `NXS`, `IBIS`, `VTG` und `STN` sind Mandantenkürzel
  beziehungsweise technische Kürzel; die Mandanten werden in
  [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 ohnehin namentlich geführt, und
  `NXS_FILE_CONVERT` sowie `NXS_MERGE` stehen dort und in [`datenmodell.md`](datenmodell.md) §3
  bereits wörtlich als Beispiele. Diese Einschätzung ist eine Ermessensentscheidung und steht als
  Frage 12 unter „Offene Entscheidungen".

---

## M14 — Spalten, Indizes und Größe von `MessageAction`, `MessageProperty` und `Service`

**Zuerst gelaufen.** Alle folgenden Statements sind gegen dieses Ergebnis abgeglichen worden.

### Statement

```sql
SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('MessageAction','MessageProperty','Service')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

### Ergebnis — Spalten

| Tabelle | # | Spalte | Typ | NULL | Schlüssel |
|---|---|---|---|---|---|
| `MessageAction` | 1 | `MessageID` | `varchar(36)` | NO | PRI |
| `MessageAction` | 2 | `MessageActionID` | `smallint(6)` | NO | PRI |
| `MessageAction` | 3 | **`SOSID`** | `varchar(36)` | **NO** | |
| `MessageAction` | 4 | **`SOSActionID`** | `smallint(6)` | **NO** | |
| `MessageAction` | 5 | `MessageActionStart` | `timestamp` | YES | |
| `MessageAction` | 6 | `MessageActionEnd` | `timestamp` | YES | |
| `MessageAction` | 7 | `ServiceID` | `varchar(36)` | YES | |
| `MessageAction` | 8 | `SOSActionServiceProperties` | `mediumtext` | YES | |
| `MessageAction` | 9 | `SOSActionTimeout` | `smallint(6)` | YES | |
| `MessageProperty` | 1 | `MessageID` | `varchar(36)` | NO | PRI |
| `MessageProperty` | 2 | `MessageActionID` | `smallint(6)` | NO | PRI |
| `MessageProperty` | 3 | `MessagePropertyName` | `varchar(100)` | NO | PRI |
| `MessageProperty` | 4 | `MessagePropertyValue` | `mediumtext` | YES | MUL |
| `Service` | 1 | `ServiceID` | `varchar(36)` | NO | PRI |
| `Service` | 2 | `ServiceTypeID` | `smallint(6)` | YES | MUL |
| `Service` | 3 | `ServiceConnectString` | `text` | YES | |
| `Service` | 4 | **`ServiceName`** | `varchar(255)` | YES | |
| `Service` | 5 | `ServiceDescription` | `text` | YES | |
| `Service` | 6 | `ServiceStatus` | `varchar(30)` | YES | |
| `Service` | 7 | `ServiceLastUpdate` | `timestamp` | YES | |
| `Service` | 8 | `ServiceLastStatusMessage` | `text` | YES | |
| `Service` | 9 | `ServiceTimeout` | `smallint(6)` | YES | |
| `Service` | 10 | `ServiceDefaultFileStore` | `varchar(36)` | YES | |

Laufzeit: **1,260 ms** (ein Lauf nach Aufwärmlauf 2,808 ms).

### Die drei Leitfragen, ausdrücklich beantwortet

**Hat `MessageAction` eine Spalte `SOSActionID`?** **Ja** — Position 4, `smallint(6)`, **`NOT NULL`**.
Dazu, ebenfalls undokumentiert, ein **eigenes `SOSID`** an Position 3, ebenfalls `NOT NULL`. Die
`MessageAction`-Zeile trägt damit **beide Hälften des zusammengesetzten Primärschlüssels von
`SOSAction`** aus sich heraus und braucht für die Auflösung keinen Umweg über `Message`. M15 muss
nichts raten.

**Trägt `Service` eine Namensspalte?** **Ja** — `ServiceName varchar(255)`, `NULL`-fähig. Ob der
Name etwas taugt, entscheidet M15 (3), nicht diese Zeile.

**Welche Indizes, mit welcher `SUB_PART`?** Siehe unten. Die Angabe der Projektbeschreibung
(„Präfix-Indizes über 50 Zeichen auf `MessagePropertyValue`") ist **belegt**.

### Ergebnis — Indizes

```sql
SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART,
       NON_UNIQUE, CARDINALITY, INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('MessageAction','MessageProperty','Service')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
```

| Tabelle | Index | Spalten (in Reihenfolge) | `SUB_PART` | eindeutig | Kardinalität |
|---|---|---|---|---|---|
| `MessageAction` | `PRIMARY` | `MessageID`, `MessageActionID` | — | ja | 10.215.743 |
| `MessageAction` | `MessageAction_MessageFK` | `MessageID` | — | nein | 10.215.743 |
| `MessageProperty` | `PRIMARY` | `MessageID`, `MessagePropertyName`, `MessageActionID` | — | ja | 46.964.279 |
| `MessageProperty` | `MessageProperty_MessageFK` | `MessageID` | — | nein | 7.827.379 |
| `MessageProperty` | `MessageProperty_MessageActionFK` | `MessageID`, `MessageActionID` | — | nein | 15.654.759 |
| `MessageProperty` | `MessagePropertyNameIDX` | `MessagePropertyName` | — | nein | 18 |
| `MessageProperty` | `MessagePropertyValueIDX` | `MessagePropertyValue` | **50** | nein | 46.964.279 |
| `MessageProperty` | `MessagePropertyNameValueIDX` | `MessagePropertyName`, `MessagePropertyValue` | —, **50** | nein | 46.964.279 |
| `Service` | `PRIMARY` | `ServiceID` | — | ja | 20 |
| `Service` | `Service_ServiceTypeFK` | `ServiceTypeID` | — | nein | 10 |

Alle Indizes `BTREE`. Laufzeit: **0,721 ms** (ein Lauf nach Aufwärmlauf 1,859 ms).

Drei Beobachtungen ohne Entscheidung:

- **Der Präfix-Index über 50 Zeichen existiert zweimal**, einmal allein auf dem Wert und einmal
  hinter dem Namen. Regel L4 ist damit belegt und nicht nur behauptet.
- **`MessageAction` hat keinen Index auf `SOSID`, `SOSActionID` oder `ServiceID`.** Alle drei Joins
  in M15 laufen deshalb zwangsläufig von `MessageID` aus in die kleine Zieltabelle hinein und nie
  umgekehrt. Für das Detail einer einzelnen Nachricht ist das die richtige Richtung; für eine
  Auswertung „alle Nachrichten, die Dienst X berührt haben" gäbe es keinen Zugriffspfad.
- **`MessagePropertyNameIDX` ist mit Kardinalität 18 geführt** — bei 101 gemessenen Namen in einem
  einzigen Tag (M17 2). Dieselbe unbrauchbare Statistik wie bei `MessageStatusIDX` in
  [L15](messungen-schritt4.md#l15--gibt-es-beim-mandanten-überhaupt-zwischenschritte).

### Ergebnis — Größe

| Tabelle | `TABLE_ROWS` (Schätzung) | `DATA_LENGTH` | `INDEX_LENGTH` | Summe | Kollation |
|---|---|---|---|---|---|
| `MessageProperty` | 46.964.279 | 15.088.615.424 | **45.945.946.112** | **61,03 GB** | `utf8mb4_general_ci` |
| `MessageAction` | 10.215.743 | 2.226.634.752 | 819.855.360 | **3,05 GB** | `utf8mb4_general_ci` |
| `Service` | 20 | 16.384 | 16.384 | 0,00 GB | `utf8mb4_general_ci` |

Laufzeit: **0,569 ms** (ein Lauf nach Aufwärmlauf 1,061 ms).

`SELECT COUNT(*)` je Tabelle, außer über `MessageProperty` (Auffälligkeit G — die Schätzung genügt,
ein Zähllauf über 47 Millionen Zeilen ist den Aufwand nicht wert):

| Tabelle | gezählt | Schätzung | Abweichung | Laufzeit |
|---|---|---|---|---|
| `MessageAction` | **10.308.590** | 10.215.743 | Schätzung **0,90 % zu niedrig** | 28,341 s (ein Lauf) |
| `Service` | **20** | 20 | keine | 0,585 ms |
| `MessageProperty` | *nicht gezählt* | 46.964.279 | — | — |

> **Die Statistik irrt hier in die andere Richtung als in Schritt 4.** Auffälligkeit G hielt fest,
> dass `information_schema` `Message` um 6,5 Prozent **über**schätzt. Bei `MessageAction`
> **unter**schätzt sie um 0,9 Prozent. „Veraltet" heißt also nicht „zu hoch", sondern nur
> „unzuverlässig" — eine `rows`-Angabe im `EXPLAIN` trägt kein Vorzeichen, auf das man sich
> verlassen könnte.

**Zur Größe von `MessageProperty`:** Der Index belegt mit 45,9 GB **das Dreifache der Nutzdaten**
(15,1 GB). Die in der Projektbeschreibung genannten „1,3 Kilobyte je Zeile" sind damit zu drei
Vierteln Index und zu einem Viertel Daten — siehe „Widersprüche zur Dokumentation".

### Die Spalten von `Service` — Vorarbeit für Schritt 8

`Service` hat **20 Zeilen**. Der Aufwand ist null, der Nutzen liegt in Schritt 8
(Filestore-Auflösung des Rohdaten-Downloads):

| `ServiceTypeID` | Zeilen | Namensform | `ServiceStatus` | `ServiceTimeout` | `ServiceDefaultFileStore` | `ServiceConnectString` |
|---|---|---|---|---|---|---|
| 1 | **11** | `TOMCAT Filestore (<Knoten>) Produktion` | `HEARTBEAT` | 0 | leer | 64 Zeichen |
| 2 | 4 | `MULTI Service (<Knoten>) Produktion` | 3× `ERROR_TIMEOUT`, 1× `SHUTDOWN` | 600 | gesetzt | 100–129 Zeichen |
| 3 | 1 | `DATAWAREHOUSE Service (Embedded) Produktion` | `HEARTBEAT` | 0 | gesetzt | **0 Zeichen** |
| 4 | 4 | `COMMUNICATION Service (<Knoten>) Produktion`, `HTTP Service (<Knoten>) Produktion` | 2× `ERROR_TIMEOUT`, 1× `SHUTDOWN`, 1× `HEARTBEAT` | 0 oder 600 | gesetzt | 137–217 Zeichen |

**Die elf Zeilen mit `ServiceTypeID = 1` sind die Filestores**, auf die
`Message.Payload.GUID` verweist (M17 2 belegt die Form: 52 Zeichen mit Pipe, also
`FILESTOREPROD09|<uuid>`). Alle elf tragen einen `ServiceConnectString` von exakt 64 Zeichen.
Der Inhalt ist hier nicht abgedruckt — er enthält Hostnamen (Regel G1).

---

## M15 — Lässt sich einer `MessageAction`-Zeile ein lesbarer Name zuordnen?

Die entscheidende Messung dieser Erhebung.

### (1) Gestalt von `MessageActionID`

```sql
SELECT n.aktionen AS aktionen_je_nachricht, COUNT(*) AS nachrichten
FROM (
  SELECT ma.MessageID, COUNT(*) AS aktionen
  FROM Message m
  JOIN MessageAction ma ON ma.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY ma.MessageID
) n
GROUP BY n.aktionen ORDER BY n.aktionen;
```

| Aktionen je Nachricht | Nachrichten |
|---|---|
| 1 | 1 |
| 2 | 619 |
| 3 | **4.363** |
| 4 | 380 |
| 5 | 820 |
| 6 | 58 |
| 7 | 8 |
| **Summe** | **6.249** |

Laufzeit: Aufwärmlauf 140,361 ms, beste von fünf **137,772 ms**.
`EXPLAIN`: `m` `range` über `MessageLastUpdateIDX` (`key_len` 5, 11.812 geschätzt, `Using index`),
`ma` `ref` über `PRIMARY` (`key_len` 146, `Using index`), darüber `<derived2>` mit
`Using temporary; Using filesort`.

**Wertebereich:**

```sql
SELECT MIN(ma.MessageActionID) AS kleinste, MAX(ma.MessageActionID) AS groesste,
       COUNT(DISTINCT ma.MessageActionID) AS verschiedene,
       SUM(ma.MessageActionID = 0) AS wert_null, COUNT(*) AS aktionen
FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00';
```

| kleinste | größte | verschiedene | `= 0` | Aktionen |
|---|---|---|---|---|
| **0** | **502** | 9 | **6.249** | 20.352 |

Laufzeit: Aufwärmlauf 84,689 ms, beste von fünf **80,367 ms**.

Neun Werte bei einem Maximum von 502 — die Verteilung erklärt das:

| `MessageActionID` | Aktionen | Nachrichten |
|---|---|---|
| **0** | **6.249** | 6.249 |
| 1 | 6.248 | 6.248 |
| 2 | 5.629 | 5.629 |
| 3 | 1.257 | 1.257 |
| 4 | 866 | 866 |
| 5 | 55 | 55 |
| 6 | 8 | 8 |
| **500** | 20 | 20 |
| **502** | 20 | 20 |

Laufzeit: Aufwärmlauf 95,268 ms, beste von fünf **94,416 ms**. Je Nachricht kommt jeder Wert
**höchstens einmal** vor (Aktionen = Nachrichten in jeder Zeile).

**Und die Frage, die beide Lesarten unterscheidet:**

```sql
SELECT k.kleinste AS kleinste_je_nachricht, COUNT(*) AS nachrichten
FROM (
  SELECT ma.MessageID, MIN(ma.MessageActionID) AS kleinste
  FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  GROUP BY ma.MessageID
) k
GROUP BY k.kleinste ORDER BY k.kleinste;
```

| kleinste je Nachricht | Nachrichten |
|---|---|
| **0** | **6.249** |

Eine einzige Zeile. Laufzeit: Aufwärmlauf 149,496 ms, beste von fünf **137,665 ms**.

> **Der Schluss, ausdrücklich: `MessageActionID` ist eine laufende Nummer je Nachricht und
> **keine** Kennung aus der Ablaufdefinition.** Jede Nachricht beginnt bei `0` — nicht bei `1`, wie
> die Aufgabenstellung als eine der beiden Möglichkeiten angenommen hatte, und schon gar nicht bei
> `2` oder `3`.
>
> Die Aufgabenstellung musste diese Frage stellen, weil sie `MessageAction.SOSActionID` nicht kannte.
> M14 hat die Spalte gefunden, und damit ist der Schluss nicht mehr nur plausibel, sondern **direkt
> belegbar**: Die beiden Kennungen laufen in denselben Daten auseinander.

| `MessageActionID` | zugehörige `SOSActionID` | Aktionen |
|---|---|---|
| 0 | 0 | 6.249 |
| 1 | 1 | 6.248 |
| 2 | **1** | 157 |
| 2 | 2 | 5.472 |
| 3 | **2** | 153 |
| 3 | 3 | 980 |
| 3 | **10** | 124 |
| 4 | **3** | 117 |
| 4 | 4 | 749 |
| 5 | **4** | 55 |
| 6 | **5** | 8 |
| 500 | 500 | 20 |
| 502 | 502 | 20 |

Laufzeit: Aufwärmlauf 253,865 ms, beste von fünf **250,910 ms**.

**Der dritte Schritt einer Nachricht trägt die Ablaufkennung 2, 3 oder 10.** In **614 von 20.352**
Aktionen des Fensters (157 + 153 + 124 + 117 + 55 + 8) weichen die beiden Kennungen voneinander ab.
Wer über `MessageActionID` in die Ablaufdefinition joint, greift dort nach der falschen Zeile — und
merkt es nicht, weil eine Zeile zurückkommt. Wie viele dieser Fehlgriffe tatsächlich einen falschen
Namen liefern, misst der Markenvergleich in (2): **375**.

**Zwei Randbefunde, ohne Entscheidung:**

- **Schritt `0` ist kein Ablaufschritt.** Er existiert auf **jeder** Nachricht, trägt
  `SOSActionID = 0`, hat **nie** `SOSActionServiceProperties` (6.249 von 6.249 `NULL`) und löst
  nie zu einer `SOSAction`-Zeile auf. M17 (3) zeigt, dass an ihm die Metadaten der Nachricht hängen.
- **`500` und `502`** kommen je 20-mal vor, tragen Bausteine, lösen aber nie auf.

### (2) Pfad a — Join über die Ablaufdefinition

Der Primärschlüssel von `SOSAction` ist zusammengesetzt (M13). Gejoint wird immer über **beide**
Spalten. Weil M14 zwei bisher unbekannte Spalten gefunden hat, sind **drei** Fassungen gelaufen:

| Fassung | Join-Bedingung |
|---|---|
| **a1** | `sa.SOSID = m.SOSID AND sa.SOSActionID = ma.MessageActionID` — die Vorlage der Aufgabenstellung |
| **a2** | `sa.SOSID = m.SOSID AND sa.SOSActionID = ma.SOSActionID` — von der Aufgabenstellung für den Fall verlangt, dass die Spalte existiert |
| **a3** | `sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID` — ganz aus `MessageAction`, erst durch M14 möglich |

```sql
SELECT COUNT(*) AS aktionen,
       SUM(sa.SOSID IS NULL)     AS ohne_sosaction_zeile,
       SUM(sa.SOSID IS NOT NULL) AS aufgeloest,
       SUM(ma.SOSActionServiceProperties IS NULL) AS ma_ohne_bausteine,
       SUM(sa.SOSActionServiceProperties IS NULL) AS sa_ohne_bausteine,
       SUM(sa.SOSID IS NOT NULL
           AND ma.SOSActionServiceProperties IS NOT NULL
           AND sa.SOSActionServiceProperties IS NOT NULL
           AND SUBSTRING_INDEX(ma.SOSActionServiceProperties, '|', 1)
             = SUBSTRING_INDEX(sa.SOSActionServiceProperties, '|', 1)) AS erste_marke_gleich,
       SUM(… <> … ) AS erste_marke_verschieden
FROM Message m
JOIN MessageAction ma ON ma.MessageID = m.MessageID
LEFT JOIN SOSAction sa ON <Join-Bedingung>
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00';
```

**Die drei Fassungen nebeneinander, Fenster A:**

| | **a1** (Vorlage) | **a2** | **a3** |
|---|---|---|---|
| Aktionen | 20.352 | 20.352 | 20.352 |
| ohne `SOSAction`-Zeile | 10.554 | 10.274 | 10.274 |
| **aufgelöst** | 9.798 | **10.078** | **10.078** |
| `MessageAction` ohne Bausteine | 6.249 | 6.249 | 6.249 |
| aufgelöste Zeile ohne Bausteine | 0 | 0 | 0 |
| **erste Marke gleich** | 9.423 (96,17 %) | 9.921 (98,44 %) | **10.078 (100 %)** |
| **erste Marke verschieden** | **375 (3,83 %)** | **157 (1,56 %)** | **0** |
| Laufzeit, beste von fünf | 242,113 ms | 247,607 ms | **214,397 ms** |

Aufwärmläufe: 258,164 · 252,927 · 219,265 ms.

`EXPLAIN` ist in allen drei Fassungen gleich aufgebaut — `m` `range` über `MessageLastUpdateIDX`
(`key_len` 5), `ma` `ref` über `PRIMARY` (`key_len` 146), `sa` **`eq_ref` über `PRIMARY` mit
`key_len` 148**. Die 148 sind der Beleg, dass über den **ganzen** zusammengesetzten Primärschlüssel
gejoint wird: 146 Bytes `varchar(36)` plus 2 Bytes `smallint`. Nur a3 kommt dabei ohne
`Using where` auf `sa` aus, weil beide Join-Spalten aus derselben Tabelle stammen.

> **Der Vergleich der ersten Marke ist der Beweis, nicht die Auflösungsquote.** a1 und a2 lösen
> ähnlich viel auf — und liegen trotzdem in 375 beziehungsweise 157 Fällen fachlich falsch. Zwei
> Nummerierungen, die beide bei 0 beginnen, treffen sich zwangsläufig; dass eine Zeile zurückkommt,
> sagt nichts. Erst **a3 mit null Abweichungen** zeigt, dass der ausgeführte Baustein mit dem
> geplanten übereinstimmt und der Join semantisch richtig ist.

**Verbreiterung über Fenster B** (Fenster A blieb mit 0,21 s deutlich unter zehn Sekunden):

| Kennzahl | Fenster B |
|---|---|
| Aktionen | **684.075** |
| ohne `SOSAction`-Zeile | 317.732 |
| **aufgelöst** | **366.343** |
| `MessageAction` ohne Bausteine | 214.330 (= genau eine je Nachricht) |
| **erste Marke gleich** | **366.336 — 99,998 %** |
| **erste Marke verschieden** | **7 — 0,002 %** |
| ohne Namen (`SOSActionName` `NULL` oder leer) | 317.732 (= genau die nicht aufgelösten) |
| verschiedene Namen | **282** |

Laufzeit: Aufwärmlauf 7.756,999 ms, beste von fünf **7.874,559 ms**
(7.988,972 · 7.874,559 · 7.989,186 · 7.966,365 · 8.012,300 ms).
`EXPLAIN` unverändert, nur die Schätzung wächst auf 409.756.

**Sieben Abweichungen auf 366.343 aufgelöste Zeilen über einen ganzen Monat.** Der Befund aus
Fenster A hält bei 34-facher Menge.

**Die Auflösung zum Namen, Fenster A, beide Fassungen:**

| | a3 (über `MessageAction`) | a1 (Vorlage) |
|---|---|---|
| Aktionen | 20.352 | 20.352 |
| ohne Namen | **10.274** | 10.554 |
| verschiedene Namen | **163** | 161 |
| Laufzeit, beste von fünf | **175,690 ms** | 210,908 ms |

Aufwärmläufe: 179,912 · 213,567 ms.

**Jede aufgelöste `SOSAction`-Zeile trägt einen Namen** — „ohne Namen" ist in beiden Fassungen exakt
gleich der Zahl der nicht aufgelösten Zeilen. Das deckt sich mit M13: `SOSActionName` ist in allen
3.944 Zeilen gepflegt, nie `NULL`, nie leer.

**Was davon in der Zeitleiste ankommt.** Schritt `0` ist kein Ablaufschritt und wird nie als solcher
gezeigt; die Quote muss deshalb über die **echten** Schritte gerechnet werden:

```sql
SELECT COUNT(*) AS aktionen_gesamt,
       SUM(ma.MessageActionID = 0) AS schritt_null,
       SUM(ma.MessageActionID > 0) AS echte_schritte,
       SUM(ma.MessageActionID > 0 AND sa.SOSID IS NOT NULL) AS echte_schritte_mit_namen,
       SUM(ma.MessageActionID > 0 AND sa.SOSID IS NULL)     AS echte_schritte_ohne_namen,
       SUM(ma.MessageActionID > 0 AND sa.SOSID IS NULL
           AND ma.SOSActionServiceProperties IS NOT NULL)   AS davon_mit_bausteinen_als_rueckfall
FROM Message m
JOIN MessageAction ma ON ma.MessageID = m.MessageID
LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
WHERE …;
```

| | Fenster A | Fenster B |
|---|---|---|
| Aktionen gesamt | 20.352 | 684.075 |
| davon Schritt `0` | 6.249 | 214.330 |
| **echte Schritte** | **14.103** | **469.745** |
| **mit Namen** | **10.078 — 71,46 %** | **366.343 — 77,99 %** |
| **ohne Namen** | 4.025 — 28,54 % | 103.402 — 22,01 % |
| davon mit Bausteinen als Rückfall | **4.025 — alle** | **103.402 — alle** |

Laufzeiten: Fenster A Aufwärmlauf 210,424 ms, beste von fünf **203,647 ms**; Fenster B Aufwärmlauf
6.960,607 ms, beste von drei **6.809,064 ms**.

**Kein einziger namenloser Schritt steht ohne Rückfallwert da.** Die nicht aufgelösten echten
Schritte tragen sämtlich ihre `SOSActionServiceProperties`.

### (3) Pfad b — Join über den Dienst

`Service` hat laut M14 eine Namensspalte, also ist dieser Pfad geprüft worden.

```sql
SELECT COUNT(*) AS aktionen,
       SUM(ma.ServiceID IS NULL) AS ohne_serviceid,
       SUM(ma.ServiceID IS NOT NULL AND s.ServiceID IS NULL) AS verwaist,
       COUNT(DISTINCT ma.ServiceID) AS verschiedene_dienste,
       SUM(s.ServiceID IS NOT NULL
           AND (s.ServiceName IS NULL OR s.ServiceName = '')) AS ohne_dienstnamen
FROM Message m
JOIN MessageAction ma ON ma.MessageID = m.MessageID
LEFT JOIN Service s ON s.ServiceID = ma.ServiceID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00';
```

| Aktionen | ohne `ServiceID` | verwaist | verschiedene Dienste | ohne Dienstnamen |
|---|---|---|---|---|
| 20.352 | **950** | **40** | **7** | **0** |

Laufzeit: Aufwärmlauf 127,937 ms, beste von fünf **129,162 ms**.
`EXPLAIN`: `s` `eq_ref` über `PRIMARY`, `key_len` 146.

**Die Dienste mit Häufigkeit** (Knotenkennungen maskiert, siehe Abschnitt 0):

| `ServiceName` | `ServiceTypeID` | Aktionen | Nachrichten |
|---|---|---|---|
| `MULTI Service (<Knoten>) Produktion` | 2 | 14.086 | 5.314 |
| `MULTI Service (<Knoten>) Produktion` | 2 | 2.231 | 1.205 |
| `MULTI Service (<Knoten>) Produktion` | 2 | 1.064 | 980 |
| *(kein Dienst — `ServiceID` `NULL` oder verwaist)* | — | 990 | 970 |
| `DATAWAREHOUSE Service (Embedded) Produktion` | 3 | 950 | 950 |
| `HTTP Service (<Knoten>) Produktion` | 4 | 696 | 696 |
| `COMMUNICATION Service (<Knoten>) Produktion` | 4 | 335 | 334 |

Laufzeit: Aufwärmlauf 215,946 ms, beste von fünf **217,479 ms**.

> **Beurteilung, ausdrücklich: Nein. Ein Dienstname ist einem Nutzer ohne EDI-Kenntnis nicht
> zeigbar.** Er benennt den **Server**, auf dem ein Schritt lief, nicht den Schritt. Drei von sieben
> Werten heißen gleich und unterscheiden sich nur in der maskierten Knotenkennung — für den Nutzer
> wären das drei identische Zeilen. „`MULTI Service (…) Produktion`" ist keine Antwort auf „wo steht
> mein Beleg", sondern eine Auskunft über unsere Infrastruktur an jemanden außerhalb des Hauses.
>
> Hinzu kommt: 990 von 20.352 Aktionen (4,9 %) haben überhaupt keinen auflösbaren Dienst, und die
> **Auflösung ist gröber als die Frage** — sieben Dienste stehen 163 verschiedenen Schrittnamen
> gegenüber.

### (4) Pfad c — wie groß ist das Vokabular der Bausteine?

```sql
SELECT COUNT(*) AS aktionen,
       COUNT(DISTINCT ma.SOSActionServiceProperties) AS verschiedene_ganze_werte,
       COUNT(DISTINCT SUBSTRING_INDEX(ma.SOSActionServiceProperties, '|', 1))
         AS verschiedene_erste_marken
FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00';
```

| Aktionen | verschiedene ganze Werte | verschiedene erste Marken |
|---|---|---|
| 20.352 | **169** | **78** |

Laufzeit: Aufwärmlauf 626,740 ms, beste von fünf **617,770 ms**.

**Die zwanzig häufigsten Marken:**

| erste Marke | Aktionen |
|---|---|
| *(`NULL` — Schritt `0`)* | 6.249 |
| `FTPSender` | 4.490 |
| `NXS_FILE_CONVERT` | 4.210 |
| `RETRIEVE` | 950 |
| `HTTPSender` | 696 |
| `STN_KV_STATUS` | 672 |
| `STN_KV_SXSPLIT` | 576 |
| `NXS_MERGE` | 557 |
| `NXS_MIDOC_SIDOC` | 381 |
| `NXS_4905_DELFOR` | 169 |
| `NXS_CBROUTER` | 128 |
| `NXS_G07B_GSVERF` | 126 |
| `IBISORDERS` | 118 |
| `STN_KV_RSPSPLIT` | 96 |
| `NXS_SPLITTER` | 83 |
| `IBISGUSORDERS` | 72 |
| `ROUTER` | 40 |
| `Ibis` | 38 |
| `FORWARDProfile` | 32 |
| `VTG_DMR_CSV` | 32 |

Laufzeit: Aufwärmlauf 261,097 ms, beste von fünf **257,349 ms**.

**Häufigkeitsklassen** (79 Gruppen = 78 Marken plus die `NULL`-Gruppe von Schritt `0`):

| Kennzahl | Wert |
|---|---|
| Marken gesamt (mit `NULL`-Gruppe) | 79 |
| Marken mit **weniger als zehn** Zeilen | **32** |
| Marken mit genau **einer** Zeile | 7 |
| Marken mit **1.000 und mehr** Zeilen | 3 |

**Der Abstand zwischen 169 ganzen Werten und 78 ersten Marken ist Faktor 2,2.** Die
Projektbeschreibung nennt Beispiele wie `NXS_MERGE|KE_OSTROV_734973|WAIT|30M|…`, in denen alles
hinter der ersten Marke Parameter ist — die Messung stützt das: Auf jede Marke kommen im Schnitt
gut zwei ganze Werte, die sich in den Parametern unterscheiden.

### Schlussfolgerung — ausdrücklich

> **Ja, die Zeitleiste in Schritt 5 trägt Klartext aus dem Quellsystem. Der Pfad ist der Join
> `SOSAction ON (SOSID, SOSActionID)` — beide Spalten aus `MessageAction`, nicht aus `Message`.
> 71 bis 78 Prozent der echten Schritte bekommen damit einen Namen; der Rest zeigt seinen Rohwert.**
>
> **Die im Plantext beauftragte Zuordnungstabelle von `SOSActionServiceProperties` nach Klartext
> entfällt.** Sie war für einen Zustand geplant, den es nicht gibt.

Die Belegkette:

1. **`MessageAction` trägt `SOSID` und `SOSActionID` selbst**, beide `NOT NULL` (M14). Die
   `SOSAction`-Zeile ist ohne Umweg über `Message` erreichbar.
2. **`MessageActionID` ist nicht `SOSActionID`.** Jede Nachricht nummeriert ab `0` (M15 1), und in
   denselben Daten trägt der dritte Schritt die Ablaufkennung 2, 3 oder 10. Wer über
   `MessageActionID` joint, trifft nachweislich falsche Zeilen und merkt es nicht.
3. **Der Join ist semantisch richtig, nicht zufällig.** Über `(ma.SOSID, ma.SOSActionID)` stimmt der
   ausgeführte Baustein in **10.078 von 10.078** Fällen (Fenster A) und **366.336 von 366.343**
   (Fenster B, 99,998 %) mit dem geplanten überein. Die beiden anderen Fassungen liegen in 3,83 %
   beziehungsweise 1,56 % der Fälle daneben — bei fast gleicher Auflösungsquote. Die Quote allein
   hätte nichts belegt.
4. **Jede aufgelöste Zeile hat einen Namen** (M15 2), und der Name ist lesbar — M13 hat 575
   verschiedene, mehrwortige, nie rein numerische Werte gemessen; hier sind es 163 im Tag und 282
   im Monat.
5. **Die Alternativen tragen nicht.** Der Dienstname benennt den Server und nicht den Schritt
   (M15 3). Das Bausteinvokabular ist mit 78 ersten Marken zwar handhabbar, aber es ist ein
   **Rohwert und keine Übersetzung** — und es wird nur noch für die 22 bis 29 Prozent gebraucht, die
   der Join nicht erreicht.
6. **Kein namenloser Schritt steht leer da:** Alle 4.025 (Fenster A) beziehungsweise 103.402
   (Fenster B) nicht aufgelösten echten Schritte tragen ihre `SOSActionServiceProperties` als
   Rückfall.

**Die Schwachstelle, die dazugehört.** Der Beweis über die erste Marke ist ein Vergleich des
Quellsystems **mit sich selbst**: Er zeigt, dass die `SOSAction`-Zeile, die wir treffen, dieselbe
ist, die der Ablauf ausgeführt hat. Er sagt **nicht**, dass `SOSActionName` fachlich beschreibt, was
dort geschah — nur, dass wir den richtigen Namen aus dem richtigen Datensatz holen. Ob „Send Message
to Partner" für einen Nutzer ohne EDI-Kenntnis die Frage „wo steht mein Beleg" beantwortet, ist eine
fachliche Frage, die keine Datenbankabfrage entscheidet.

Zweitens ist die Auflösungsquote **an einem Bestand gemessen, dessen Ablaufdefinitionen zum
Datenstand passen**. Die 22 bis 29 Prozent nicht aufgelösten Schritte sind fast alle Zeilen, deren
`SOSAction` es im heutigen Stand nicht mehr gibt — 3.985 der 4.025 in Fenster A hängen an einer
einzigen Konstellation (`MessageActionID = 2`, `SOSActionID = 2`). In der Produktion, wo Abläufe
laufend geändert werden, kann dieser Anteil anders liegen, und er wird für alte Nachrichten mit der
Zeit wachsen. **Der Rückfall auf den Rohwert ist deshalb kein Randfall, sondern ein Regelweg**, und
die Anzeige muss ihn tragen können.

---

## M16 — Gestalt der Schrittfolge

### (1) Offene Aktionen und Dauer

```sql
SELECT COUNT(*) AS aktionen,
       SUM(ma.MessageActionEnd IS NULL)   AS ohne_ende,
       SUM(ma.MessageActionStart IS NULL) AS ohne_start,
       MIN(TIMESTAMPDIFF(SECOND, ma.MessageActionStart, ma.MessageActionEnd)) AS kuerzeste_sek,
       MAX(TIMESTAMPDIFF(SECOND, ma.MessageActionStart, ma.MessageActionEnd)) AS laengste_sek,
       ROUND(AVG(TIMESTAMPDIFF(SECOND, ma.MessageActionStart, ma.MessageActionEnd))) AS mittel_sek,
       SUM(TIMESTAMPDIFF(SECOND, ma.MessageActionStart, ma.MessageActionEnd) < 0) AS negative_dauer
FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00';
```

| Aktionen | ohne Ende | ohne Start | kürzeste | längste | Mittel | **negative Dauer** |
|---|---|---|---|---|---|---|
| 20.352 | **0** | **0** | 0 s | **662.463 s** (7 T 16 h) | 4.009 s (67 min) | **0** |

Laufzeit: Aufwärmlauf 134,080 ms, beste von fünf **130,069 ms**.

**`negative_dauer` ist null.** Start und Ende stehen durchgängig in der richtigen Reihenfolge; die
Zeitleiste braucht dafür **keine** Sonderregel.

Die Dauerverteilung zeigt, dass das Mittel von 67 Minuten irreführend ist:

| Dauerklasse | Aktionen | Anteil |
|---|---|---|
| 0 s | 7.241 | 35,6 % |
| 1–10 s | 699 | 3,4 % |
| 11–60 s | 386 | 1,9 % |
| **1–30 min** | **11.902** | **58,5 %** |
| 30 min – 24 h | **0** | 0 % |
| über 24 h | 124 | 0,6 % |

Laufzeit: Aufwärmlauf 137,284 ms, beste von fünf **129,907 ms**.

**Zwischen 30 Minuten und 24 Stunden liegt keine einzige Aktion.** Die Verteilung ist zweigipflig:
fast alles ist entweder sofort fertig oder wartet bis zu 30 Minuten — und dann kommt lange nichts.
Die 30-Minuten-Kante ist genau `SOSActionTimeout = 1800` (M16 4).

### (2) Die größte Schrittfolge im Fenster

```sql
SELECT MAX(n.aktionen) AS meiste_aktionen_einer_nachricht
FROM ( SELECT ma.MessageID, COUNT(*) AS aktionen
       FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
       WHERE … GROUP BY ma.MessageID ) n;
```

| meiste Aktionen einer Nachricht |
|---|
| **7** |

Laufzeit: Aufwärmlauf 142,123 ms, beste von fünf **133,824 ms**. Die Verteilung dazu steht in
M15 (1): 4.363 der 6.249 Nachrichten haben genau drei Aktionen.

> ### Schlussfolgerung — ausdrücklich: Die Zeitleiste braucht **keine** Obergrenze.
>
> Die Folge ist von Natur aus kurz. Das Maximum im dichten Tag liegt bei **sieben** Aktionen, davon
> ist eine der Metadaten-Schritt `0` — also **höchstens sechs echte Schritte**. **98,9 Prozent** der
> Nachrichten (6.183 von 6.249) haben fünf oder weniger Aktionen. Eine Deckelung schützte vor nichts
> und kostete die Vollständigkeit, die eine Zeitleiste erst brauchbar macht.

**Der Abgleich mit M13, der ausdrücklich verlangt war:** M13 hat für die Ablaufdefinition ein bis
sechs Schritte gemessen (442 Abläufe mit einem, 694 mit zwei, 480 mit drei, 134 mit vier, 24 mit
fünf, 3 mit sechs). Die ausgeführten Aktionen liegen mit **eins bis sieben** genau darüber — und der
eine Schritt Unterschied ist der Metadaten-Schritt `0`, der in der Ablaufdefinition nicht existiert.
**Die beiden Messungen decken sich; `MessageAction` liegt nicht „deutlich darüber".**

**Die Schwachstelle, die dazugehört.** Gemessen ist ein Tag mit 6.249 Nachrichten, kein
Jahresbestand. Sieben ist das Maximum **dieses Fensters**, nicht des Systems — die
Ablaufdefinitionen erlauben laut M13 sechs Schritte, aber eine Nachricht, die einen Ablauf mehrfach
durchläuft, wäre davon nicht begrenzt. Der Wert `MessageActionID = 502` zeigt, dass die
Nummerierung ohnehin bis dreistellig geht; welche Bedeutung `500` und `502` haben, ist nicht
gemessen. **Die Aussage „keine Obergrenze nötig" ruht darauf, dass eine Detailansicht ohnehin nur
eine einzige Nachricht lädt** — selbst ein unerwarteter Ausreißer von hundert Schritten wäre dort
kein Leistungsproblem, sondern nur eine lange Liste.

### (3) Der Fall, der die Anzeige entscheidet

```sql
SELECT m.MessageStatus, COUNT(*) AS nachrichten,
       SUM((SELECT COUNT(*) FROM MessageAction ma
            WHERE ma.MessageID = m.MessageID
              AND ma.MessageActionEnd IS NULL) = 0) AS alle_aktionen_beendet,
       SUM((SELECT COUNT(*) FROM MessageAction ma
            WHERE ma.MessageID = m.MessageID) = 0)  AS ohne_jede_aktion
FROM Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY m.MessageStatus;
```

| `MessageStatus` | Nachrichten | alle Aktionen beendet | ohne jede Aktion |
|---|---|---|---|
| `SUSPENDED` | **538** | **538** | **0** |

Laufzeit: Aufwärmlauf 35,267 ms, beste von fünf **34,932 ms**. `EXPLAIN`: `m` `range` über
`MessageStatusIDX` (`key_len` 123, `Using index`), beide Unterabfragen `ref` über
`MessageAction.PRIMARY`. `RUNNING` kommt wie dokumentiert null Mal vor.

**Dieselbe Auswertung über die Fehlerzustände:**

| `MessageStatus` | Nachrichten | alle Aktionen beendet | ohne jede Aktion |
|---|---|---|---|
| `ERROR_DUPLICATE` | 3.248 | 3.248 | 0 |
| `COMMIT_REJECTED` | 111 | 111 | 0 |
| **`ERROR_TIMEOUT`** | 52 | **3** | 0 |

Laufzeit: Aufwärmlauf 219,081 ms, beste von fünf **216,274 ms**.

**49 der 52 `ERROR_TIMEOUT`-Nachrichten haben eine offene Aktion** — der einzige Status im Bestand,
bei dem das der Normalfall ist. Das führte auf die Gegenprobe (Abweichung von Betriebsregel 1, siehe
Abschnitt 0):

| Gegenprobe über den Gesamtbestand | Wert | Laufzeit |
|---|---|---|
| `MessageAction` mit `MessageActionEnd IS NULL` | **95** von 10.308.590 | 3,013 s |
| `MessageAction` mit `MessageActionStart IS NULL` | **0** | 2,940 s |

Die 95 nach Status zugeordnet (23,144 s, ein Lauf):

| `MessageStatus` | offene Aktionen | Nachrichten | ältester Start | jüngster Start |
|---|---|---|---|---|
| `ERROR_TIMEOUT` | **49** | 49 | `2025-12-30 04:08:45` | `2025-12-30 04:09:47` |
| `FINISHED` | 39 | 39 | `2024-12-01 13:40:07` | `2025-07-06 22:50:23` |
| `CHECKED` | 7 | 7 | `2024-10-18 08:57:17` | `2025-07-30 06:52:25` |

**Eine Auffälligkeit, die zur Zahl gehört und nicht gedeutet wird:** Die 49 offenen Aktionen liegen
sämtlich in einer Spanne von 62 Sekunden, und deren Ende — `2025-12-30 04:09:47` — ist auf die
Sekunde derselbe Zeitpunkt, den [M9](messungen-schritt4.md#m9--anker-für-die-dev-uhr) als jüngsten
Zeitpunkt des dichten Bestands ermittelt hat. Auch die Nachrichten selbst ballen sich dort:

| Tag | `ERROR_TIMEOUT`-Nachrichten | Spanne |
|---|---|---|
| 2025-12-13 | 1 | — |
| 2025-12-20 | 1 | — |
| 2025-12-27 | 1 | — |
| **2025-12-30** | **49** | `04:08:45` bis `04:09:47` |

Laufzeit 1,577 ms. Ob das ein fachliches Muster ist oder der Schnittkante der Testkopie geschuldet,
ist **nicht gemessen** und steht als Frage 6 unter „Offene Entscheidungen".

> ### Schlussfolgerung — ausdrücklich: Eine wartende Nachricht steht **zwischen** zwei Schritten, nicht auf einem laufenden.
>
> Bei **allen 538** `SUSPENDED`-Nachrichten ist jede Aktion beendet. Das ist genau der Fall, den die
> Aufgabenstellung beim Warten auf eine Zusammenführung erwartet hat, und er ist **ein eigener
> Zustand in der Zeitleiste**: Der letzte Schritt ist abgeschlossen, der nächste hat nicht begonnen,
> und dazwischen steht die Nachricht.
>
> Die Zeitleiste braucht damit **drei** Zustände je Schritt und nicht zwei — beendet, laufend
> (`MessageActionEnd IS NULL`), und den Zwischenraum nach dem letzten beendeten Schritt einer
> offenen Nachricht. Der Zwischenraum ist kein Schritt und darf keiner Zeile zugeschlagen werden.
>
> **Der Hänger ist am offenen Ende erkennbar**, also an `MessageActionEnd IS NULL` — nicht an einer
> Frist und nicht am Status. Im gesamten Bestand tragen 95 von 10,3 Millionen Aktionen dieses
> Merkmal, und 49 davon gehören zu `ERROR_TIMEOUT`.
>
> **`ohne_jede_aktion` ist über alle geprüften Status null.** Der Fall „Nachricht existiert, aber
> kein Schritt ist protokolliert" kommt in der Testkopie nicht vor. Die Detailansicht muss ihn
> trotzdem aushalten — er ist nicht widerlegt, sondern nur nicht beobachtet, und `MessageAction` hat
> keinen Zwang, der ihn ausschlösse.

**Die Schwachstelle, die dazugehört.** Der aussagekräftige Status fehlt: **`RUNNING` kommt in der
Testkopie null Mal vor**, in der Produktion aber sehr wohl (Projektbeschreibung §4.1). Gemessen ist
damit ausschließlich das Verhalten von `SUSPENDED`, und alle 538 stehen zudem auf **demselben**
Schritt (M13 4: `Send Message to Pool`) und stammen aus einem Zeitraum von sieben Tagen. Die
Aussage „eine offene Nachricht steht zwischen zwei Schritten" ist für `SUSPENDED` belegt und für
`RUNNING` **unbelegt** — und gerade bei `RUNNING` wäre der laufende Schritt der zu erwartende Fall.
Die Zeitleiste muss deshalb beide Zustände können, obwohl lokal nur einer prüfbar ist. Dasselbe
Prüfbarkeitsproblem hat M13 (3) bereits für die Anzeige des aktuellen Schritts festgehalten.

### (4) Der Timeout je Schritt

```sql
SELECT ma.SOSActionTimeout, COUNT(*) AS aktionen
FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
WHERE … GROUP BY ma.SOSActionTimeout ORDER BY aktionen DESC;
```

| `SOSActionTimeout` | Aktionen | auf Schritt `0` | auf echtem Schritt |
|---|---|---|---|
| `1800` | 14.063 | 0 | **14.063** |
| `0` | 6.289 | **6.249** | 40 |

Laufzeiten: Aufwärmlauf 82,945 ms, beste von fünf **82,238 ms**; die Aufschlüsselung nach Schritt
104,577 ms (ein Lauf).

**Genau zwei Werte, nie `NULL`** — dasselbe Bild wie in den Stammdaten (M8: 3.743 von 3.944 Zeilen
auf `1800`), mit einem Unterschied: In `SOSAction` kommen zusätzlich `NULL` (11 Zeilen) und `300`
(2 Zeilen) vor, in den ausgeführten Aktionen dieses Fensters **keines von beidem**. Und die `0`
steht fast ausschließlich auf dem Metadaten-Schritt `0`, der keine Frist braucht.

**Wie viele Schritte ihre eigene Frist überschreiten:**

```sql
SELECT COUNT(*) AS beendete_aktionen,
       SUM(TIMESTAMPDIFF(SECOND, ma.MessageActionStart, ma.MessageActionEnd)
             > ma.SOSActionTimeout) AS ueber_der_frist
FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
WHERE … AND ma.MessageActionEnd IS NOT NULL
      AND ma.SOSActionTimeout IS NOT NULL AND ma.SOSActionTimeout > 0;
```

| beendete Aktionen | über der Frist | Anteil |
|---|---|---|
| 14.063 | **124** | **0,88 %** |

Laufzeit: Aufwärmlauf 98,213 ms, beste von fünf **97,197 ms**.

**Die 124 sind dieselben 124, die über 24 Stunden laufen.** Nachgeprüft: Zwischen 1.800 s (der
Frist) und 86.400 s liegt keine einzige Aktion. Ein Schritt hält seine Frist entweder ein oder
überschreitet sie um mehr als das Achtundvierzigfache — ein Dazwischen gibt es in diesem Fenster
nicht. Laufzeit 100,3 ms.

---

## M17 — `MessageProperty` je Nachricht

### (1) Zeilen je Nachricht

```sql
SELECT z.zeilen AS zeilen_je_nachricht, COUNT(*) AS nachrichten
FROM ( SELECT mp.MessageID, COUNT(*) AS zeilen
       FROM Message m JOIN MessageProperty mp ON mp.MessageID = m.MessageID
       WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
         AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
       GROUP BY mp.MessageID ) z
GROUP BY z.zeilen ORDER BY z.zeilen;
```

`EXPLAIN`: `m` `range` über `MessageLastUpdateIDX`, **`mp` `ref` über `PRIMARY` (`key_len` 146,
`Using index`)** — der Zugriff läuft über das Präfix des Primärschlüssels, also ausschließlich über
`MessageID` (Regel L4).

| Zeilen je Nachricht | Nachrichten | | Zeilen je Nachricht | Nachrichten |
|---|---|---|---|---|
| 14 | 252 | | 26 | 73 |
| 15 | 1 | | 27 | 2 |
| 16 | 1 | | 28 | 21 |
| 18 | 233 | | 29 | 225 |
| 19 | 312 | | 30 | 5 |
| 20 | 3 | | 31 | 11 |
| 21 | 24 | | 32 | 46 |
| **22** | **4.077** | | 33 | 185 |
| 23 | 4 | | 34 | 24 |
| 24 | 19 | | 35 | 14 |
| 25 | 701 | | 36 | 7 |
| | | | 37 | 8 |
| | | | 38 | 1 |

Summe **6.249** — jede Nachricht des Fensters hat Eigenschaften, keine ist leer.

| Kennzahl | Fenster A | Fenster B |
|---|---|---|
| Zeilen gesamt | 141.037 | 4.904.303 |
| Nachrichten | 6.249 | 214.330 |
| **Zeilen je Nachricht** | **22,57** | **22,88** |
| Minimum / Maximum | 14 / 38 | — |
| häufigster Wert | 22 (bei 4.077 Nachrichten) | — |

Laufzeit: Aufwärmlauf 483,281 ms, beste von fünf **472,966 ms**.

**Beide dokumentierten Angaben liegen darunter** — die Projektbeschreibung nennt „rund vierzehn",
`datenmodell.md` §5 „rund zehn". Die vierzehn sind rekonstruierbar: 46.964.279 Zeilen geteilt durch
3.341.519 Nachrichten ergibt **14,05** — ein Mittel über den **gesamten** Aufbewahrungszeitraum.
In beiden gemessenen Fenstern des dichten Bestands sind es rund 22,6 beziehungsweise 22,9. Näheres
unter „Widersprüche zur Dokumentation".

### (2) Namen, Häufigkeit und Wertlängen

Vollständige Liste, Fenster A — **101 verschiedene Namen**. Laufzeit: Aufwärmlauf 1.304,512 ms,
beste von fünf **1.165,737 ms**.

| Name | Zeilen | Nachrichten | `NULL` | leer | kürzester | Mittel | längster | Bytes |
|---|---|---|---|---|---|---|---|---|
| `Service.Type` | 20.312 | 6.249 | 0 | 0 | 9 | 10 | 13 | 13 |
| `Converter.Payload.GUID` | 7.862 | 6.149 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Converter.Log.GUID` | 7.862 | 6.149 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Converter.TransactionID` | 7.862 | 6.149 | 0 | 0 | 3 | 6 | 8 | 8 |
| `Message.GUID` | 6.249 | 6.249 | 0 | 0 | 36 | 36 | 36 | 36 |
| `Message.SOS` | 6.249 | 6.249 | 0 | 0 | 9 | 18 | 33 | 33 |
| `Message.Payload.GUID` | 6.249 | 6.249 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Message.MessageActionID` | 6.249 | 6.249 | 0 | 0 | 1 | 1 | 1 | 1 |
| `Message.SOSActionID` | 6.249 | 6.249 | 0 | 0 | 1 | 1 | 3 | 3 |
| `Message.SOSActionServiceProperties` | 6.248 | 6.248 | 0 | 0 | 11 | 30 | 208 | 210 |
| `FTPSender.Log.GUID` | 4.490 | 4.316 | 0 | 0 | 52 | 52 | 52 | 52 |
| `FTPSender.TransactionID` | 4.490 | 4.316 | 0 | 0 | 4 | 6 | 8 | 8 |
| `FTPSender.Payload.GUID` | 4.490 | 4.316 | 0 | 0 | 52 | 52 | 52 | 52 |
| `FileReader.FileProperty.Size` | 4.352 | 4.351 | 0 | 0 | 3 | 4 | 8 | 8 |
| `FileReader.FileProperty.OriginalFilename` | 4.352 | 4.351 | 0 | 0 | 26 | 29 | 71 | 71 |
| `FileReader.TransactionID` | 4.352 | 4.351 | 0 | 0 | 5 | 7 | 7 | 7 |
| `FileReader.PickupTime` | 4.352 | 4.351 | 0 | 0 | 19 | 19 | 19 | 19 |
| `FileReader.Payload.GUID` | 4.352 | 4.351 | 0 | 0 | 52 | 52 | 52 | 52 |
| `FileReader.Log.GUID` | 4.352 | 4.351 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Message.SourceMessageID` | 4.096 | 4.096 | 0 | 0 | 36 | 36 | 36 | 36 |
| `Message.SplitCount` | 1.053 | 1.053 | 0 | 0 | 1 | 1 | 3 | 3 |
| `DataWarehouse.RecordsRead` | 950 | 950 | 0 | 0 | 1 | 1 | 1 | 1 |
| `DataWarehouse.Payload.GUID` | 950 | 950 | 0 | 0 | 52 | 52 | 52 | 52 |
| `HTTPSender.URL` | 696 | 696 | 0 | 0 | 43 | 80 | 81 | 81 |
| `HTTPSender.Result` | 696 | 696 | 0 | 0 | 3 | 3 | 3 | 3 |
| `HTTPSender.Payload.GUID` | 696 | 696 | 0 | 0 | 52 | 52 | 52 | 52 |
| `HTTPSender.Method` | 696 | 696 | 0 | 0 | 3 | 15 | 15 | 15 |
| `HTTPSender.Log.GUID` | 696 | 696 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Message.SendingPartner` | 688 | 688 | 0 | 0 | 2 | 5 | 17 | 17 |
| `Message.ReceivingPartner` | 515 | 515 | 0 | 0 | 3 | 6 | 12 | 12 |
| `Message.DestinationFilename` | 480 | 480 | 0 | 0 | 3 | 39 | 73 | 73 |
| `Message.ReceiverID` | 449 | 449 | 0 | 0 | 10 | 10 | 10 | 10 |
| `SAPReader.DOCNUM` | 443 | 443 | 0 | 0 | 16 | 16 | 16 | 16 |
| `SAPReader.TransactionID` | 443 | 443 | 0 | 0 | 8 | 8 | 8 | 8 |
| `SAPReader.ReceiverID` | 443 | 443 | 0 | 0 | 10 | 10 | 10 | 10 |
| `SAPReader.Payload.GUID` | 443 | 443 | 0 | 0 | 52 | 52 | 52 | 52 |
| `SAPReader.Log.GUID` | 443 | 443 | 0 | 0 | 52 | 52 | 52 | 52 |
| **`Message.VFN`** | 397 | 397 | 0 | 0 | 4 | 13 | 21 | 21 |
| `Message.SNDPRN` | 383 | 383 | 0 | 0 | 6 | 6 | 6 | 6 |
| `Converter.DestinationFilename` | 318 | 318 | 0 | 0 | 15 | 45 | 57 | 57 |
| `AS2Reader.Payload.GUID` | 244 | 244 | 0 | 0 | 52 | 52 | 52 | 52 |
| `AS2Reader.MessageID` | 244 | 244 | 0 | 0 | 31 | 53 | 76 | 76 |
| `AS2Reader.Log.GUID` | 244 | 244 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Message.Destination` | 218 | 218 | 0 | 0 | 21 | 21 | 21 | 21 |
| `OFTPReader.SenderID` | 218 | 218 | 0 | 0 | 8 | 18 | 25 | 25 |
| `OFTPReader.Payload.GUID` | 218 | 218 | 0 | 0 | 52 | 52 | 52 | 52 |
| `OFTPReader.Log.GUID` | 218 | 218 | 0 | 0 | 52 | 52 | 52 | 52 |
| `OFTPReader.VFNCounter` | 218 | 218 | 0 | 0 | 1 | 1 | 4 | 4 |
| `OFTPReader.VFN` | 218 | 218 | 0 | 0 | 4 | 17 | 21 | 21 |
| `OFTPReader.TransactionID` | 218 | 218 | 0 | 0 | 8 | 8 | 8 | 8 |
| `Message.DestinationFolder` | 216 | 216 | 0 | 0 | 7 | 21 | 28 | 28 |
| `Message.CheckDuplicates` | 126 | 126 | 0 | 0 | 1 | 1 | 1 | 1 |
| `FTPReader.FileProperty.Size` | 111 | 111 | 0 | 0 | 3 | 4 | 6 | 6 |
| `FTPReader.FileProperty.OriginalFilename` | 111 | 111 | 0 | 0 | 25 | 29 | 39 | 39 |
| `FTPReader.TransactionID` | 111 | 111 | 0 | 0 | 6 | 6 | 6 | 6 |
| `FTPReader.PickupTime` | 111 | 111 | 0 | 0 | 19 | 19 | 19 | 19 |
| `FTPReader.Payload.GUID` | 111 | 111 | 0 | 0 | 52 | 52 | 52 | 52 |
| `FTPReader.Log.GUID` | 111 | 111 | 0 | 0 | 52 | 52 | 52 | 52 |
| `OFTP2Reader.Payload.GUID` | 46 | 46 | 0 | 0 | 52 | 52 | 52 | 52 |
| `OFTP2Reader.MessageID` | 46 | 46 | 0 | 0 | 24 | 32 | 39 | 39 |
| `OFTP2Reader.Log.GUID` | 46 | 46 | 0 | 0 | 52 | 52 | 52 | 52 |
| `AS2Sender.Payload.GUID` | 39 | 39 | 0 | 0 | 52 | 52 | 52 | 52 |
| `AS2Sender.MessageID` | 39 | 39 | 0 | 0 | 62 | 70 | 80 | 80 |
| `AS2Sender.Log.GUID` | 39 | 39 | 0 | 0 | 52 | 52 | 52 | 52 |
| `AS2Sender.SendTime` | 39 | 39 | 0 | 0 | 19 | 19 | 19 | 19 |
| `AS2Sender.Result` | 39 | 39 | 0 | 0 | 13 | 13 | 13 | 13 |
| `MailReader.Subject` | 32 | 32 | 0 | 0 | 20 | 49 | 76 | 76 |
| `Message.Filename` | 32 | 32 | 0 | 0 | 47 | 54 | 80 | 80 |
| `MailReader.SenderID` | 32 | 32 | 0 | 0 | 19 | 22 | 26 | 26 |
| `MailReader.Payload.GUID` | 32 | 32 | 0 | 0 | 52 | 52 | 52 | 52 |
| `MailReader.Log.GUID` | 32 | 32 | 0 | 0 | 52 | 52 | 52 | 52 |
| `MailReader.Filename` | 32 | 32 | 0 | 0 | 47 | 54 | 80 | 80 |
| `MailReader.TransactionID` | 32 | 32 | 0 | 0 | 8 | 8 | 8 | 8 |
| `Message.DestinationURI` | 24 | 24 | 0 | 0 | 21 | 21 | 21 | 21 |
| `Message.Location` | 23 | 23 | 0 | 0 | 9 | 10 | 18 | 18 |
| `OFTPSender.MessageID` | 20 | 20 | 0 | 0 | 39 | 47 | 56 | 56 |
| `OFTPSender.MessageGroupID` | 20 | 20 | 0 | 0 | 35 | 43 | 52 | 52 |
| `OFTPSender.TransactionID` | 20 | 20 | 0 | 0 | 8 | 8 | 8 | 8 |
| `OFTPSender.Log.GUID` | 20 | 20 | 0 | 0 | 52 | 52 | 52 | 52 |
| `OFTPSender.RemoteID` | 20 | 20 | 0 | 0 | 12 | 16 | 25 | 25 |
| `OFTPSender.RemoteAddress` | 20 | 20 | 0 | 0 | 25 | 30 | 33 | 33 |
| `OFTPSender.Payload.GUID` | 20 | 20 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Message.RCVPRN` | 8 | 8 | 0 | 0 | 10 | 10 | 10 | 10 |
| **`Message.IDOCNr`** | 8 | 8 | 0 | 0 | 16 | 758 | **2.124** | **2.124** |
| `OFTP2Sender.Payload.GUID` | 6 | 6 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Message.Type` | 6 | 6 | 0 | 0 | 7 | 14 | 16 | 16 |
| `OFTP2Sender.Log.GUID` | 6 | 6 | 0 | 0 | 52 | 52 | 52 | 52 |
| `OFTP2Sender.SendTime` | 6 | 6 | 0 | 0 | 19 | 19 | 19 | 19 |
| `OFTP2Sender.Result` | 6 | 6 | 0 | 0 | 13 | 13 | 13 | 13 |
| `SSHReader.TransactionID` | 5 | 5 | 0 | 0 | 8 | 8 | 8 | 8 |
| `SSHReader.SenderID` | 5 | 5 | 0 | 0 | 3 | 4 | 6 | 6 |
| `SSHReader.Payload.GUID` | 5 | 5 | 0 | 0 | 52 | 52 | 52 | 52 |
| `SSHReader.Log.GUID` | 5 | 5 | 0 | 0 | 52 | 52 | 52 | 52 |
| `SSHReader.Filename` | 5 | 5 | 0 | 0 | 31 | 33 | 41 | 41 |
| `Message.CommitInterchangeNumber` | 2 | 2 | 0 | 0 | 30 | 151 | 271 | 271 |
| `Message.CheckSequence` | 2 | 2 | 0 | 0 | 1 | 1 | 1 | 1 |
| `HTTPReader.SenderID` | 1 | 1 | 0 | 0 | 15 | 15 | 15 | 15 |
| `HTTPReader.Payload.GUID` | 1 | 1 | 0 | 0 | 52 | 52 | 52 | 52 |
| `HTTPReader.Log.GUID` | 1 | 1 | 0 | 0 | 52 | 52 | 52 | 52 |
| `Message.DestinationFilenameFTP` | 1 | 1 | 0 | 0 | 15 | 15 | 15 | 15 |
| `HTTPReader.TransactionID` | 1 | 1 | 0 | 0 | 8 | 8 | 8 | 8 |

**Kein einziger Wert ist `NULL` oder leer** — in keiner der 101 Namensgruppen, über alle 141.037
Zeilen.

**Die Größenfrage, um die es ging:**

| Kennzahl | Fenster A | Fenster B |
|---|---|---|
| verschiedene Namen | 101 | **119** |
| Zeilen | 141.037 | 4.904.303 |
| mittlere Wertlänge | **26,4 Zeichen** | — |
| **längster Wert** | **2.124 Zeichen** | **12.732 Zeichen** |
| Bytes über alle Eigenschaften **je Nachricht** | **595** | **595** |
| größte Nachricht (Summe aller Eigenschaften) | **2.895 Bytes** | — |

Laufzeiten: Fenster A Aufwärmlauf 553,484 ms, beste von fünf **550,498 ms**; Fenster B Aufwärmlauf
19.807,753 ms, beste von drei **19.821,696 ms** (19.903,146 · 20.037,493 · 19.821,696 ms).

> **Ein Detail-Aufruf schiebt rund 595 Byte an Eigenschaften durch die Antwort, nicht Megabyte.**
> Der Wert ist in beiden Fenstern **auf das Byte gleich**. Der Typ `mediumtext` erlaubt 16 MB je
> Zelle; der größte tatsächlich gemessene Wert liegt bei **12,4 KB** über einen Monat und bei
> 2,1 KB über einen Tag. Die größte einzelne Nachricht des Tagesfensters trägt insgesamt 2.895 Byte.

Dass das Maximum von 2.124 auf 12.732 Zeichen wächst, wenn das Fenster von einem Tag auf einen Monat
geht, gehört zur Ehrlichkeit der Zahl dazu: **Das Maximum ist eine Eigenschaft des Fensters, das
Mittel nicht.**

**Abgleich mit den zehn Namen aus `datenmodell.md`:**

| Dokumentierter Name | Fenster A | Anmerkung |
|---|---|---|
| `Message.GUID` | ✓ 6.249 | auf jeder Nachricht |
| `Message.SendingPartner` | ✓ 688 | 11,0 % der Nachrichten |
| `Message.SNDPRN` | ✓ 383 | 6,1 % |
| `Message.VFN` | ✓ 397 | 6,4 % |
| `Message.SOS` | ✓ 6.249 | auf jeder Nachricht |
| `Message.SplitCount` | ✓ 1.053 | 16,9 % |
| `Message.Payload.GUID` | ✓ 6.249 | auf jeder Nachricht, 52 Zeichen |
| **`Message.InterchangeNumber`** | **✗ fehlt** | im Tagesfenster nicht vorhanden; über Fenster B **800 Zeilen** |
| `Message.CommitInterchangeNumber` | ✓ 2 | im Tagesfenster praktisch leer |
| `Message.SourceMessageID` | ✓ 4.096 | 65,5 % |

**Neun der zehn dokumentierten Namen kommen vor, `Message.InterchangeNumber` erst im
Monatsfenster.** Neu und undokumentiert sind damit **92 weitere** in Fenster A (101 minus die neun
bekannten). Die `Message.*`-Familie umfasst im Fenster **26** Namen, von denen **17** in keiner
Dokumentationsdatei stehen: `Message.MessageActionID`, `Message.SOSActionID`,
`Message.SOSActionServiceProperties`, `Message.ReceivingPartner`, `Message.DestinationFilename`,
`Message.ReceiverID`, `Message.Destination`, `Message.DestinationFolder`,
`Message.CheckDuplicates`, `Message.Filename`, `Message.DestinationURI`, `Message.Location`,
`Message.RCVPRN`, `Message.IDOCNr`, `Message.Type`, `Message.CheckSequence`,
`Message.DestinationFilenameFTP`. Die übrigen tragen ein Dienstpräfix (`FTPSender.*`, `Converter.*`,
`FileReader.*`, `SAPReader.*`, `OFTPReader.*`, …).

> **`Message.Payload.GUID` ist mit 52 Zeichen auf jeder Nachricht belegt** und trägt in **allen**
> 6.249 Fällen ein Pipe-Zeichen (M17 4). Das deckt sich Zeichen für Zeichen mit der dokumentierten
> Form `FILESTOREPROD09|<uuid>` — 15 + 1 + 36 = 52. Vorarbeit für Schritt 8.

### (3) Hängen die Eigenschaften an einem Schritt?

```sql
SELECT mp.MessageActionID, COUNT(*) AS zeilen,
       COUNT(DISTINCT mp.MessagePropertyName) AS namen,
       COUNT(DISTINCT mp.MessageID) AS nachrichten
FROM Message m JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE … GROUP BY mp.MessageActionID ORDER BY mp.MessageActionID;
```

| `MessageActionID` | Zeilen | Anteil | verschiedene Namen | Nachrichten |
|---|---|---|---|---|
| **0** | **82.943** | **58,8 %** | **71** | 6.249 |
| 1 | 24.364 | 17,3 % | 16 | 6.248 |
| 2 | 23.517 | 16,7 % | 31 | 5.629 |
| 3 | 6.415 | 4,5 % | 25 | 1.257 |
| 4 | 3.466 | 2,5 % | 12 | 866 |
| 5 | 220 | 0,2 % | 4 | 55 |
| 6 | 32 | 0,0 % | 4 | 8 |
| 500 | 80 | 0,1 % | 4 | 20 |

Laufzeit: Aufwärmlauf 551,530 ms, beste von fünf **533,039 ms**. `mp` ist `ref` über `PRIMARY` und
`Using index`. **`MessageActionID = 502` kommt in `MessageProperty` nicht vor**, obwohl es in
`MessageAction` existiert.

**Wie eindeutig ein Name zu einem Schritt gehört:**

| verschiedene Schritte je Name | Namen |
|---|---|
| **1** | **70** |
| 2 | 12 |
| 3 | 12 |
| 4 | 3 |
| 6 | 3 |
| 7 | 1 |

Laufzeit: Aufwärmlauf 598,858 ms, beste von fünf **596,590 ms**.

**Und die neun dokumentierten `Message.*`-Namen des Fensters:**

| Name | steht auf Schritt | Zeilen |
|---|---|---|
| `Message.GUID` | **0** | 6.249 |
| `Message.Payload.GUID` | **0** | 6.249 |
| `Message.SOS` | **0** | 6.249 |
| `Message.SourceMessageID` | **0** | 4.096 |
| `Message.SplitCount` | **0** | 1.053 |
| `Message.SendingPartner` | **0** | 688 |
| `Message.VFN` | **0** | 397 |
| `Message.SNDPRN` | **0** | 383 |
| `Message.CommitInterchangeNumber` | **0** | 2 |

**Ausnahmslos Schritt `0`.** Laufzeit: Aufwärmlauf 238,556 ms, beste von fünf **239,937 ms**.

> ### Schlussfolgerung — ausdrücklich: Es verteilt sich. Aber nicht beliebig — es zerfällt in zwei Familien.
>
> **Nicht alles steht auf einer Kennung:** 41,2 Prozent der Zeilen hängen an echten Schritten,
> verteilt über sieben verschiedene Kennungen, und 70 der 101 Namen kommen auf genau **einem**
> Schritt vor. Die Eigenschaften **können** in der Zeitleiste an der richtigen Stelle stehen statt
> in einem Sammelblock.
>
> **Aber die Trennlinie läuft nicht zwischen den Schritten, sondern zwischen zwei Arten von
> Eigenschaft:**
>
> | Familie | wo sie steht | was sie ist |
> |---|---|---|
> | **`Message.*`** | ausnahmslos Schritt `0` | Metadaten der **Nachricht** — Kennung, Ablauf, Nutzdatenverweis, Partner, Verkettung |
> | **`<Dienst>.*`** (`FTPSender.*`, `Converter.*`, `FileReader.*`, …) | auf dem Schritt, der sie erzeugt hat | Protokoll des **Schritts** — Transaktionsnummer, Protokollverweis, Dateiname, Ergebnis |
>
> Schritt `0` ist damit kein Ablaufschritt (M15 1 hat das schon von der anderen Seite gezeigt: keine
> Bausteine, keine `SOSAction`-Zeile, auf jeder Nachricht genau einmal), sondern **der Ort, an dem
> die Nachricht ihre eigenen Metadaten ablegt**. Die 58,8 Prozent der Zeilen, die dort hängen,
> gehören in einen Kopfbereich der Detailansicht und nicht in die Zeitleiste; der Rest kann an seinen
> Schritt.

**Die Schwachstelle, die dazugehört.** Die Zuordnung ist über **Namen** gemessen, nicht über
Bedeutung. Dass `FTPSender.TransactionID` auf den Schritten 1 bis 6 vorkommt, heißt nur, dass ein
FTP-Versand an verschiedenen Stellen des Ablaufs stehen kann — nicht, dass der Wert dort etwas
anderes bedeutet. Umgekehrt ist bei den 31 Namen auf mehr als einem Schritt **nicht geprüft**, ob
derselbe Name auf derselben Nachricht mehrfach mit verschiedenen Werten vorkommt; der
Primärschlüssel `(MessageID, MessagePropertyName, MessageActionID)` erlaubt das ausdrücklich, und
`Converter.Payload.GUID` mit 7.862 Zeilen auf 6.149 Nachrichten zeigt, dass es geschieht.
**Eine Anzeige, die je Nachricht einen Wert je Namen erwartet, wäre damit falsch** — und das ist
gemessen, nicht vermutet.

Zweitens: Die Aufteilung 58,8 zu 41,2 Prozent gilt für **dieses** Fenster. Sie hängt daran, wie
viele Schritte die Nachrichten des Tages hatten, und ein Tag mit längeren Abläufen verschöbe sie.

### (4) Die Gestalt der fünf häufigsten Namen

**Kein Wert ist abgedruckt** — beschrieben statt gezeigt (Regel G1 und `docs/README.md`). Die
Beschreibung entsteht aus Zeichenklassen, gemessen als Aggregat über die bereits eingegrenzte Menge:

| Name | Zeilen | verschiedene Werte | nur Ziffern | UUID-Form | mit Pipe | mit Dateiendung | nur `A–Z 0–9 _` |
|---|---|---|---|---|---|---|---|
| `Service.Type` | 20.312 | **17** | 0 | 0 | 0 | 0 | **20.312** |
| `Converter.TransactionID` | 7.862 | 7.857 | **7.862** | 0 | 0 | 0 | 7.862 |
| `Converter.Payload.GUID` | 7.862 | 7.862 | 0 | 0 | **7.862** | 0 | 0 |
| `Converter.Log.GUID` | 7.862 | 7.862 | 0 | 0 | **7.862** | 0 | 0 |
| `Message.Payload.GUID` | 6.249 | 6.249 | 0 | 0 | **6.249** | 0 | 0 |
| `Message.GUID` | 6.249 | 6.249 | 0 | **6.249** | 0 | 0 | 0 |
| `Message.VFN` | 397 | **43** | 0 | 0 | 0 | 11 | 192 |

Laufzeit: Aufwärmlauf 1.747,926 ms, beste von fünf **1.730,715 ms**.

**Die fünf häufigsten, je als Beschreibung:**

1. **`Service.Type`** — 20.312 Zeilen, eine je Aktion (20.352 Aktionen, Differenz 40 = die
   verwaisten `ServiceID` aus M15 3). **Nur 17 verschiedene Werte**, sämtlich aus Großbuchstaben,
   Ziffern und Unterstrich, 9 bis 13 Zeichen. Es ist eine geschlossene technische Aufzählung, die
   die **Art** eines Schritts benennt. Die Werte sind keine Kundendaten und deshalb hier vollständig
   nennbar, samt der Schritte, auf denen sie stehen:

   | `Service.Type` | Zeilen | steht auf Schritt |
   |---|---|---|
   | `Converter` | 7.709 | 1, 2, 3, 4 |
   | `FTPSender` | 4.490 | 1–6 |
   | `FileReader` | 4.352 | 0, 1, 2 |
   | `DataWarehouse` | 950 | 1 |
   | `ScheduleBean` | 950 | 0 |
   | `HTTPSender` | 696 | 2, 3 |
   | `SAPReader` | 443 | 0 |
   | `AS2Reader` | 244 | 0 |
   | `OFTPReader` | 218 | 0 |
   | `FTPReader` | 111 | 0 |
   | `OFTP2Reader` | 46 | 0 |
   | `AS2Sender` | 39 | 2, 3, 4 |
   | `MailReader` | 32 | 0 |
   | `OFTPSender` | 20 | 2, 3 |
   | `OFTP2Sender` | 6 | 2, 3 |
   | `SSHReader` | 5 | 0 |
   | `HTTPReader` | 1 | 0 |

   Laufzeit 0,221 s (ein Lauf). Die `*Reader` stehen fast ausschließlich auf Schritt `0` — dort
   kommt die Nachricht ins System —, die `*Sender` und `Converter` auf den Folgeschritten.

2. **`Converter.Payload.GUID`** und **3. `Converter.Log.GUID`** — je 7.862 Zeilen auf 6.149
   Nachrichten, also **mehr als eine je Nachricht**. Länge konstant **52 Zeichen**, jeder Wert
   enthält ein Pipe-Zeichen, jeder Wert ist verschieden. Das ist dieselbe Gestalt wie
   `Message.Payload.GUID`, also ein Filestore-Verweis der Form `<Filestore-Kennung>|<UUID>`. Der
   eine zeigt auf die umgewandelten Nutzdaten, der andere auf ein Protokoll.

3. **`Converter.TransactionID`** — 7.862 Zeilen, **ausschließlich Ziffern**, 3 bis 8 Zeichen,
   Mittel 6. 7.857 von 7.862 Werten sind verschieden: eine laufende technische Nummer, keine
   fachliche Kennung, keine Belegnummer.

4. **`Message.GUID`** — 6.249 Zeilen, eine je Nachricht, konstant **36 Zeichen**, und **alle 6.249
   in kanonischer UUID-Form** (`8-4-4-4-12` Hexadezimalziffern). Durchgängig verschieden.

**Und `Message.VFN`, das die Dokumentation nicht kennt.** 397 Zeilen auf 397 Nachrichten (6,4 %),
4 bis 21 Zeichen, Mittel 13. Nur **43 verschiedene Werte** — der Wert wiederholt sich also stark und
ist keine belegindividuelle Nummer. 192 der 397 bestehen ausschließlich aus Großbuchstaben, Ziffern
und Unterstrich; 11 enden auf eine Dateiendung. Die Längen ballen sich an zwei Stellen: 148 Zeilen
mit 6 Zeichen (6 verschiedene Werte) und 151 Zeilen mit 20 Zeichen (6 verschiedene Werte).

Was den Namen einordnet, ist eine **gemessene Nachbarschaft**: Im selben Fenster gibt es
`OFTPReader.VFN` (218 Zeilen) und `OFTPReader.VFNCounter` (218 Zeilen), und wo beide auf derselben
Nachricht stehen, sind die Werte fast immer gleich:

| Nachrichten mit `Message.VFN` **und** `OFTPReader.VFN` | Werte gleich | Werte verschieden |
|---|---|---|
| 218 | **204 (93,6 %)** | 14 |

Laufzeit 0,211 s (ein Lauf). `Message.VFN` spiegelt also den Wert, den der OFTP-Empfangsdienst
mitbringt. **Was die Abkürzung bedeutet, ist damit nicht gemessen** — die naheliegende Auflösung
„Virtual File Name" aus dem OFTP-/ODETTE-Umfeld ist eine Vermutung aus dem Präfix des Nachbarnamens
und steht als Frage 10 unter „Offene Entscheidungen".

---

# Nachtrag vom 07.08.2026 — S1 und M18 bis M22

> **Was hier nachgetragen wird und warum.** Die Ersterhebung (M14 bis M17) hat drei Dinge offen
> gelassen, die vor Schritt 5 entschieden sein müssen: **warum** ein Viertel der Schritte namenlos
> bleibt, **wie groß** eine Zuordnungstabelle für diesen Rest sein müsste, und ob der Nebenbefund zu
> den offenen Aktionen ein Muster oder ein einzelner Vorgang ist. Dazu kommt eine Frage, die erst
> durch M14 entstanden ist: ob `MessageAction.SOSID` die 43,9 Prozent verwaister Verweise aus
> [M13](messungen-schritt4.md#m13--trägt-sosactionid-einen-lesbaren-namen) erklärt.
>
> **Diese Abschnitte ziehen ihre Schlussfolgerung ausdrücklich**, anders als M14 bis M17 — wie M8
> und M9 in [`messungen-schritt4.md`](messungen-schritt4.md). Jede ist als benannte Entscheidung
> gekennzeichnet und trägt einen Absatz zu ihrer Schwachstelle.
>
> **An M14 bis M17 ist nichts geändert**, auch dort nicht, wo eine Zahl sich im Licht dieses
> Nachtrags anders liest. Widersprüche werden benannt und verlinkt, nicht überschrieben. Ebenso
> bleiben die Zahlen von M13 unverändert — sie sind richtig gemessen, nur anders zu lesen als
> damals angenommen (M20).
>
> **Der Join lautet ab hier durchgängig `sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID`.**
> Über `Message.SOSID` wird nicht mehr gejoint; M15 hat diese Fassung widerlegt. Der Rahmen aus
> [Abschnitt 0](#0-rahmen-der-messung) gilt unverändert. Server zu Beginn des Nachtrags:
> `2026-08-07 13:29:06` (UTC `11:29:06`), `SELECT @@global.read_only` → **`1`**, Benutzer
> `monitor_read@%`.

---

## S1 — Woran ist der Metadaten-Schritt erkennbar?

M18 bis M21 müssen den Schritt ausnehmen, der keiner ist. Die Ersterhebung hat ihn über
`MessageActionID = 0` beschrieben und nebenbei festgestellt, dass dieselben Zeilen `SOSActionID = 0`
tragen und keine Bausteine haben. **Welches der drei Merkmale das Kriterium ist, wird hier gemessen
und nicht geraten** — sie könnten auseinanderfallen.

### Statement

```sql
SELECT COUNT(*) AS aktionen,
       SUM(ma.MessageActionID = 0)                AS a_messageactionid_null,
       SUM(ma.SOSActionID = 0)                    AS b_sosactionid_null,
       SUM(ma.SOSActionServiceProperties IS NULL) AS c_ohne_bausteine,
       SUM((ma.MessageActionID = 0) <> (ma.SOSActionID = 0))                    AS a_gegen_b_verschieden,
       SUM((ma.MessageActionID = 0) <> (ma.SOSActionServiceProperties IS NULL)) AS a_gegen_c_verschieden,
       SUM((ma.SOSActionID = 0)     <> (ma.SOSActionServiceProperties IS NULL)) AS b_gegen_c_verschieden
FROM Message m
JOIN MessageAction ma ON ma.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00';
```

### EXPLAIN

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | — | 11.812 | `Using where; Using index` |
| 1 | `ma` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 1 | — |

### Ergebnis

| Kennzahl | Fenster A | Fenster B |
|---|---|---|
| Aktionen | 20.352 | 684.075 |
| **(a)** `MessageActionID = 0` | 6.249 | 214.330 |
| **(b)** `SOSActionID = 0` | 6.249 | 214.330 |
| **(c)** `SOSActionServiceProperties IS NULL` | 6.249 | 214.330 |
| a gegen b verschieden | **0** | **0** |
| a gegen c verschieden | **0** | **0** |
| b gegen c verschieden | **0** | **0** |
| Nachrichten im Fenster | 6.249 | 214.330 |

Laufzeiten: Fenster A Aufwärmlauf 114,402 ms, beste von fünf **113,355 ms**; Fenster B Aufwärmlauf
4.978,810 ms, beste von drei **4.761,621 ms**.

> **Festgelegt: Das Kriterium ist `MessageAction.SOSActionID = 0`.**
>
> Die drei Merkmale treffen über 704.427 geprüfte Aktionen in beiden Fenstern **dieselbe Menge** —
> null Abweichungen in allen drei Paarvergleichen. Es ist also gleichgültig, welches man nimmt; die
> Wahl wird trotzdem begründet und festgeschrieben, damit M18 bis M21 nicht drei verschiedene
> Definitionen benutzen.
>
> **Gewählt ist `SOSActionID = 0`, weil es die Spalte ist, über die gejoint wird.** Wer sie
> ausschließt, entfernt genau die Zeilen, die den Join gar nicht bedienen können — der Ausschluss
> und die Wirkung stehen damit in derselben Spalte. `MessageActionID = 0` beschriebe dasselbe über
> eine Spalte, die am Join nicht beteiligt ist, und `SOSActionServiceProperties IS NULL` wäre eine
> Bedingung auf einem `mediumtext`.
>
> Und die Menge ist **exakt eine Zeile je Nachricht** in beiden Fenstern (6.249 von 6.249 und
> 214.330 von 214.330) — der Metadaten-Schritt fehlt keiner Nachricht und kommt keiner doppelt vor.

---

## M18 — Warum bleibt ein Viertel der Aktionen ohne Namen?

### Statement

```sql
SELECT ma.SOSActionID,
       COUNT(*) AS aktionen,
       SUM(sa.SOSID IS NULL) AS ohne_sosaction_zeile,
       SUM(sa.SOSID IS NOT NULL
           AND (sa.SOSActionName IS NULL OR sa.SOSActionName = '')) AS zeile_ohne_namen,
       SUM(ma.SOSActionServiceProperties IS NULL) AS ohne_bausteine
FROM Message m
JOIN MessageAction ma ON ma.MessageID = m.MessageID
LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY ma.SOSActionID
ORDER BY ma.SOSActionID;
```

### EXPLAIN

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | — | 11.812 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | `ma` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 1 | — |
| 1 | `sa` | `eq_ref` | `PRIMARY` | **148** | `ma.SOSID`, `ma.SOSActionID` | 1 | — |

### Ergebnis — Fenster A

| `SOSActionID` | Aktionen | ohne `SOSAction`-Zeile | Zeile ohne Namen | ohne Bausteine |
|---|---|---|---|---|
| **0** | 6.249 | **6.249** | 0 | **6.249** |
| 1 | 6.405 | 0 | 0 | 0 |
| **2** | 5.625 | **3.985** | 0 | 0 |
| 3 | 1.097 | 0 | 0 | 0 |
| 4 | 804 | 0 | 0 | 0 |
| 5 | 8 | 0 | 0 | 0 |
| 10 | 124 | 0 | 0 | 0 |
| **500** | 20 | **20** | 0 | 0 |
| **502** | 20 | **20** | 0 | 0 |

Laufzeit: Aufwärmlauf 226,401 ms, beste von fünf **223,599 ms**.

### Ergebnis — Fenster B

| `SOSActionID` | Aktionen | ohne `SOSAction`-Zeile | Zeile ohne Namen | ohne Bausteine |
|---|---|---|---|---|
| **0** | 214.330 | **214.330** | 0 | **214.330** |
| 1 | 218.980 | 0 | 0 | 0 |
| **2** | 169.703 | **99.290** | 0 | 0 |
| 3 | 41.130 | 0 | 0 | 0 |
| 4 | 23.432 | 0 | 0 | 0 |
| 5 | 127 | 0 | 0 | 0 |
| 10 | 12.260 | 0 | 0 | 0 |
| 20 | 1 | 0 | 0 | 0 |
| **500** | 2.055 | **2.055** | 0 | 0 |
| **501** | 2 | **2** | 0 | 0 |
| **502** | 2.055 | **2.055** | 0 | 0 |

Laufzeit: Aufwärmlauf 7.871,646 ms, beste von drei **7.825,334 ms**.

`zeile_ohne_namen` ist in **jeder** Zeile beider Tabellen null: Wo der Join eine `SOSAction`-Zeile
findet, trägt sie immer einen Namen. Und `ohne_bausteine` steht ausschließlich beim Metadaten-Schritt
— das bestätigt S1 ein zweites Mal, diesmal aufgeschlüsselt.

### Schlussfolgerung — ausdrücklich

> **Die Auflösungsquote ohne den Metadaten-Schritt ist 71,46 Prozent (Fenster A) und 77,99 Prozent
> (Fenster B) — dieselben Zahlen wie in der Ersterhebung. Die Aufgabenstellung ging davon aus, dass
> die 71 bis 78 Prozent den Metadaten-Schritt noch enthalten. Das ist nicht der Fall; M15 hat ihn
> bereits ausgenommen, nur über das Kriterium `MessageActionID > 0`. S1 zeigt, dass beide Kriterien
> dieselbe Menge treffen — die Quote ändert sich deshalb nicht.**

| | Fenster A | Fenster B |
|---|---|---|
| Aktionen gesamt | 20.352 | 684.075 |
| davon Metadaten-Schritt (`SOSActionID = 0`) | 6.249 | 214.330 |
| **echte Schritte** | **14.103** | **469.745** |
| mit Namen | 10.078 — **71,46 %** | 366.343 — **77,99 %** |
| ohne Namen | 4.025 — 28,54 % | 103.402 — 22,01 % |

**Der Beitrag von M18 ist nicht die Quote, sondern ihre Verteilung — und die ist das eigentliche
Ergebnis.** Die Lücke ist nicht über die Schritte gestreut, sie sitzt auf drei Kennungen:

| Herkunft der namenlosen echten Schritte | Fenster A | Anteil | Fenster B | Anteil |
|---|---|---|---|---|
| `SOSActionID = 2` | **3.985** | **99,01 %** | **99.290** | **96,02 %** |
| `SOSActionID = 500` | 20 | 0,50 % | 2.055 | 1,99 % |
| `SOSActionID = 501` | — | — | 2 | 0,00 % |
| `SOSActionID = 502` | 20 | 0,50 % | 2.055 | 1,99 % |
| **Summe** | **4.025** | 100 % | **103.402** | 100 % |

**Alle übrigen Kennungen lösen zu 100 Prozent auf** — 1, 3, 4, 5, 10 und (in Fenster B) 20, zusammen
8.438 beziehungsweise 295.930 Aktionen ohne einen einzigen Fehlgriff. Von einem „Viertel der
Zeitleiste, das seinen Rohwert zeigt" kann deshalb nicht die Rede sein: Es ist **ein einziger
Schritt je betroffener Nachricht**, und welcher, ist vorhersagbar.

**Die Schwachstelle, die dazugehört.** `SOSActionID = 2` löst in Fenster A zu 70,8 Prozent **nicht**
auf (3.985 von 5.625), in Fenster B zu 58,5 Prozent (99.290 von 169.703) — die Kennung ist also
nicht per se unauflösbar, sondern nur bei bestimmten Abläufen. Welche das sind und warum, beantwortet
M18 nicht; das tut M20. Solange diese Ursache nicht feststeht, ist die Konzentration auf drei
Kennungen ein **beobachtetes Muster dieses Bestands** und keine Eigenschaft des Quellsystems, auf
die sich Anzeigecode verlassen dürfte.

---

## M19 — Wie groß müsste die Zuordnungstabelle sein?

### Statement

```sql
SELECT SUBSTRING_INDEX(ma.SOSActionServiceProperties, '|', 1) AS erste_marke,
       COUNT(*) AS aktionen,
       COUNT(DISTINCT m.MessageID) AS nachrichten
FROM Message m
JOIN MessageAction ma ON ma.MessageID = m.MessageID
LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND sa.SOSID IS NULL
  AND ma.SOSActionID <> 0          -- Kriterium aus S1
GROUP BY erste_marke
ORDER BY aktionen DESC;
```

### EXPLAIN

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | — | 11.812 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | `ma` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 1 | `Using where` |
| 1 | `sa` | `eq_ref` | `PRIMARY` | 148 | `ma.SOSID`, `ma.SOSActionID` | 1 | `Using where; Using index; Not exists` |

### Ergebnis — die vollständige Liste

**Fenster A — drei Marken, mehr gibt es nicht:**

| erste Marke | Aktionen | Nachrichten | kumuliert | ganze Werte | Länge |
|---|---|---|---|---|---|
| `FTPSender` | **3.985** | 3.985 | **99,01 %** | 1 | 33 Zeichen |
| `EERP received` | 20 | 20 | 99,50 % | 1 | 13 Zeichen |
| `Message has been sent` | 20 | 20 | **100 %** | 1 | 21 Zeichen |

**Fenster B — vier Marken:**

| erste Marke | Aktionen | Nachrichten | kumuliert | ganze Werte | Länge |
|---|---|---|---|---|---|
| `FTPSender` | **99.290** | 99.290 | **96,02 %** | 1 | 33 Zeichen |
| `EERP received` | 2.055 | 2.055 | 98,01 % | 1 | 13 Zeichen |
| `Message has been sent` | 2.055 | 2.055 | **99,998 %** | 1 | 21 Zeichen |
| `EERP pending` | 2 | 2 | 100 % | 1 | 12 Zeichen |

Laufzeiten: Fenster A Aufwärmlauf 185,013 ms, beste von fünf **177,370 ms**; Fenster B Aufwärmlauf
5.959,607 ms, beste von drei **5.914,010 ms**. Die Spalten „ganze Werte" und „Länge" stammen aus
derselben Abfrage mit `COUNT(DISTINCT ma.SOSActionServiceProperties)` statt der Gruppierung
(Fenster A: Aufwärmlauf 247,152 ms, beste von fünf **242,770 ms**; Fenster B: Aufwärmlauf
7.644,562 ms, beste von drei **7.292,557 ms**).

**Jede Marke steht für genau einen ganzen Wert.** Es gibt hinter diesen drei beziehungsweise vier
Marken keine Parametervielfalt — anders als bei den benannten Schritten, wo 78 Marken auf 169 ganze
Werte kommen (M15 4). Eine Tabelle über den **ganzen** Wert wäre hier also genauso groß wie eine über
die Marke.

### Zwei Auswertungen über die Vorlage hinaus

**(1) Dieselbe Marke trägt anderswo sehr wohl einen Namen.**

```sql
SELECT SUBSTRING_INDEX(ma.SOSActionServiceProperties,'|',1) AS erste_marke,
       SUM(sa.SOSID IS NOT NULL) AS mit_namen,
       SUM(sa.SOSID IS NULL)     AS ohne_namen,
       COUNT(DISTINCT sa.SOSActionName) AS verschiedene_namen
FROM … WHERE … AND ma.SOSActionID <> 0
  AND SUBSTRING_INDEX(ma.SOSActionServiceProperties,'|',1) IN (…)
GROUP BY erste_marke;
```

| erste Marke | Fenster A: mit Namen | ohne Namen | verschiedene Namen | Fenster B: mit Namen | ohne Namen | verschiedene Namen |
|---|---|---|---|---|---|---|
| `FTPSender` | **505** | 3.985 | **17** | **10.746** | 99.290 | **25** |
| `EERP received` | 0 | 20 | 0 | 0 | 2.055 | 0 |
| `Message has been sent` | 0 | 20 | 0 | 0 | 2.055 | 0 |
| `EERP pending` | — | — | — | 0 | 2 | 0 |

Laufzeiten 154,958 ms und 5.169,711 ms (je ein Lauf).

**Das ist die Zahl, die eine Zuordnungstabelle begrenzt.** Der Baustein `FTPSender` löst dort, wo er
auflöst, auf **17 beziehungsweise 25 verschiedene** `SOSActionName` auf. Eine Tabellenzeile
`FTPSender → <ein Text>` könnte diese Vielfalt nicht abbilden; sie lieferte einen Sammelbegriff wie
„Datei per FTP versendet", wo der Ablauf 25 unterschiedliche Schritte kennt.

**(2) Der Baustein ist im selben Ablauf eindeutig auffindbar — nur unter einer anderen Kennung.**

```sql
SELECT COUNT(*) AS namenlose_aktionen,
       SUM(t.treffer = 1) AS genau_ein_schritt_mit_gleicher_marke,
       SUM(t.treffer = 0) AS kein_schritt_mit_gleicher_marke,
       SUM(t.treffer > 1) AS mehrere_schritte_mit_gleicher_marke
FROM (
  SELECT (SELECT COUNT(*) FROM SOSAction sx
          WHERE sx.SOSID = ma.SOSID
            AND SUBSTRING_INDEX(sx.SOSActionServiceProperties,'|',1)
              = SUBSTRING_INDEX(ma.SOSActionServiceProperties,'|',1)) AS treffer
  FROM Message m
  JOIN MessageAction ma ON ma.MessageID = m.MessageID
  LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
  WHERE … AND sa.SOSID IS NULL AND ma.SOSActionID <> 0
) t;
```

| | Fenster A | Fenster B |
|---|---|---|
| namenlose echte Schritte | 4.025 | 103.402 |
| **genau ein Schritt mit gleicher Marke im selben Ablauf** | **3.985 — 99,01 %** | **99.290 — 96,02 %** |
| kein Schritt mit gleicher Marke | 40 | 4.112 |
| **mehrere Schritte mit gleicher Marke** | **0** | **0** |

Laufzeiten: Fenster A Aufwärmlauf 586,035 ms, beste von fünf **558,195 ms**; Fenster B Aufwärmlauf
15.971,877 ms, beste von drei **15.042,243 ms**.
`EXPLAIN` der Unterabfrage: `sx` `ref` über `SOSAction.PRIMARY` (`key_len` 146, Einstieg über `SOSID`).

**Nie mehrdeutig.** Wo eine Marke im Ablauf vorkommt, kommt sie genau einmal vor. Die restlichen 40
beziehungsweise 4.112 sind die Pseudoschritte 500/501/502 — für sie gibt es im Ablauf keinen
Gegenpart, weil sie keiner sind.

### Schlussfolgerung — ausdrücklich

> **Eine Zuordnungstabelle bräuchte für 90 Prozent der namenlosen Schritte genau eine Zeile, und für
> 99 Prozent drei.** Über Fenster A deckt die eine Zeile `FTPSender` bereits 99,01 Prozent ab, über
> Fenster B 96,02 Prozent; drei Zeilen erreichen 100 beziehungsweise 99,998 Prozent, vier Zeilen
> 100 Prozent.
>
> **Die Entscheidung, ob sie gebaut wird, fällt hier nicht.** Sie gehört ins Sparring. Was die
> Messung dazu beiträgt, sind drei Zahlen und ein Einwand.

Die Belegkette:

1. **Das Vokabular ist winzig.** Über den ganzen dichten Monat gibt es hinter 103.402 namenlosen
   Schritten **vier** verschiedene Marken und **vier** verschiedene ganze Werte — kein Verhältnis von
   1:2,2 wie bei den benannten Schritten (M15 4).
2. **Drei der vier sind bereits Klartext.** `EERP received`, `Message has been sent` und
   `EERP pending` sind keine technischen Bezeichner, sondern lesbare englische Sätze. Für sie wäre
   eine „Übersetzung" allenfalls eine Eindeutschung, keine Auflösung. Übrig bleibt als echter
   Übersetzungsfall **genau ein Wert: `FTPSender`**.
3. **Und ausgerechnet dieser eine ist der, den eine Tabelle am schlechtesten abbildet.** `FTPSender`
   steht im selben Fenster 10.746-mal *mit* Namen und löst dort auf **25 verschiedene**
   `SOSActionName` auf. Eine Tabellenzeile müsste diese 25 auf einen Sammelbegriff eindampfen — sie
   wäre also nicht die fehlende Übersetzung, sondern eine gröbere.
4. **Ein zweiter Weg ist gemessen und trägt weiter.** 96 bis 99 Prozent der namenlosen Schritte
   finden im **selben Ablauf** genau einen Schritt mit derselben Marke, nie mehrere. Über diesen Weg
   käme der **echte** `SOSActionName` heraus statt eines Sammelbegriffs — ohne eine einzige gepflegte
   Zeile.

**Die Schwachstelle, die dazugehört.** Der zweite Weg ist **nicht auf Richtigkeit geprüft, nur auf
Eindeutigkeit.** Gemessen ist, dass es je Ablauf genau einen Schritt mit passender Marke gibt — nicht,
dass es **derselbe** Schritt ist, der ausgeführt wurde. Genau der Beweis, der M15 trägt (der Vergleich
des ausgeführten mit dem geplanten Baustein), ist hier **konstruktionsbedingt nicht führbar**: Er
setzt eine aufgelöste `SOSAction`-Zeile voraus, und dass es keine gibt, ist der Anlass. Bei einem
Ablauf mit zwei verschiedenen Bausteinen ist die Zuordnung zwingend; bei einem, der denselben
Baustein zweimal ausführte, wäre sie es nicht — dieser Fall kommt in beiden Fenstern **null Mal** vor,
aber die Produktion muss sich daran nicht halten.

Zweitens gilt die Zahl „eine Zeile für 90 Prozent" für **diesen Bestand**. Sie hängt daran, dass ein
einziger Ablauf die 3.985 beziehungsweise 99.290 Zeilen stellt (M20). Ändert sich dieser eine Ablauf,
ändert sich die ganze Rechnung — eine Tabelle mit einer Zeile ist billig zu bauen und genauso billig
falsch.

---

## M20 — Erklärt `MessageAction.SOSID` die 43,9 Prozent aus M13?

### Statement

```sql
SELECT COUNT(*) AS aktionen,
       SUM(ma.SOSID  = m.SOSID) AS gleicher_ablauf,
       SUM(ma.SOSID <> m.SOSID) AS anderer_ablauf,
       SUM(ma.SOSID <> m.SOSID AND sa.SOSID IS NULL) AS anderer_ablauf_und_verwaist,
       SUM(ma.SOSID  = m.SOSID AND sa.SOSID IS NULL) AS gleicher_ablauf_und_verwaist
FROM Message m
JOIN MessageAction ma ON ma.MessageID = m.MessageID
LEFT JOIN SOSAction sa ON sa.SOSID = m.SOSID AND sa.SOSActionID = m.SOSActionID
WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00';
```

Der `LEFT JOIN` bildet hier bewusst die **M13-Fassung** nach (über `Message.SOSID` und
`Message.SOSActionID`) — es geht darum, deren Ergebnis zu erklären.

### EXPLAIN

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | — | 11.812 | `Using index condition` |
| 1 | `sa` | `eq_ref` | `PRIMARY` | 148 | `m.SOSID`, `m.SOSActionID` | 1 | `Using where; Using index` |
| 1 | `ma` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 1 | — |

### Ergebnis — Fenster A

| Kennzahl | Wert |
|---|---|
| Aktionen | 20.352 |
| `ma.SOSID = m.SOSID` | 20.037 — 98,45 % |
| `ma.SOSID <> m.SOSID` | 315 — 1,55 % |
| **anderer Ablauf **und** verwaist** | **0** |
| **gleicher Ablauf **und** verwaist** | **11.956** |

Laufzeit: Aufwärmlauf 145,904 ms, beste von fünf **142,961 ms**.

**Die Vermutung ist damit widerlegt.** Wäre sie richtig, stünde die Zahl der verwaisten Verweise in
der Zeile `anderer_ablauf_und_verwaist`. Sie steht vollständig in der anderen: **Jeder einzelne
verwaiste Verweis liegt auf einer Zeile, deren `MessageAction.SOSID` mit `Message.SOSID`
übereinstimmt.** Ein abweichender Ablauf kommt vor (315 Aktionen, 1,55 Prozent), aber er führt in
**keinem** Fall zu einem verwaisten Verweis.

### Was es stattdessen ist

Auf Nachrichtenebene, also in der Zählweise von M13:

```sql
SELECT COUNT(*) AS nachrichten, SUM(sa.SOSID IS NULL) AS verwaist,
       ROUND(100*SUM(sa.SOSID IS NULL)/COUNT(*),2) AS verwaist_prozent
FROM Message m
LEFT JOIN SOSAction sa ON sa.SOSID = m.SOSID AND sa.SOSActionID = m.SOSActionID
WHERE …;
```

| Nachrichten | verwaist | Anteil |
|---|---|---|
| 6.249 | 3.986 | **63,79 %** |

Laufzeit: Aufwärmlauf 61,408 ms, beste von fünf **60,434 ms**.

Aufgeschlüsselt nach `Message.SOSActionID` (101,515 ms, ein Lauf):

| `Message.SOSActionID` | Nachrichten | verwaist |
|---|---|---|
| 1 | 623 | 0 |
| **2** | 4.404 | **3.985** |
| 3 | 293 | 0 |
| 4 | 796 | 0 |
| 5 | 8 | 0 |
| 10 | 124 | 0 |

**Dieselbe Konzentration wie in M18** — und dieselbe Zahl, 3.985. Der Blick auf die betroffenen
Abläufe schließt die Kette (174,253 ms, ein Lauf):

| Schritte im Ablauf | größte Kennung im Ablauf | namenlose Aktionen | betroffene Abläufe |
|---|---|---|---|
| 3 | **99** | **3.985** | **1** |
| 2 | 2 | 20 | 5 |
| 3 | 3 | 20 | 2 |

Und der eine Ablauf, der 3.985 der 4.025 stellt, definiert seine Schritte so (2,395 ms):

| `SOSActionID` | erste Marke des geplanten Schritts | Länge des Namens |
|---|---|---|
| **1** | `NXS_FILE_CONVERT` | 17 Zeichen |
| **98** | **`FTPSender`** | 23 Zeichen |
| **99** | `SAPSender` | 19 Zeichen |

**Der ausgeführte Schritt trägt `SOSActionID = 2`, der geplante trägt `98` — und beide tragen die
Marke `FTPSender`.** Die Ablaufdefinition nummeriert nicht lückenlos; die Ausführung schreibt eine
fortlaufende Position. Die Namen sind hier nicht abgedruckt, weil sie Kundennamen enthalten können
(M13); ihre Länge belegt, dass sie gepflegt sind.

Wie verbreitet die Lücken in der Nummerierung sind (4,270 ms):

| Kennzahl | Wert |
|---|---|
| Abläufe mit mindestens einem Schritt in `SOSAction` | 1.777 |
| davon mit `MAX(SOSActionID) > COUNT(*)`, also Lücke oder Versatz | **257 — 14,5 %** |
| davon mit einer Kennung ≥ 99 | 233 |
| größte Kennung überhaupt | 99 |

### Kann eine Nachricht Schritte aus mehreren Abläufen haben?

```sql
SELECT g.ablaeufe AS verschiedene_ablaeufe_je_nachricht, COUNT(*) AS nachrichten
FROM ( SELECT ma.MessageID, COUNT(DISTINCT ma.SOSID) AS ablaeufe
       FROM Message m JOIN MessageAction ma ON ma.MessageID = m.MessageID
       WHERE … GROUP BY ma.MessageID ) g
GROUP BY g.ablaeufe ORDER BY g.ablaeufe;
```

| Abläufe je Nachricht | Fenster A | Fenster B |
|---|---|---|
| 1 | 6.092 | 209.581 |
| **2** | **156** | **4.728** |
| **3** | **1** | **21** |
| **mehr als einer** | **157 — 2,51 %** | **4.749 — 2,22 %** |

Laufzeiten: Fenster A Aufwärmlauf 121,362 ms, beste von fünf **118,165 ms**; Fenster B Aufwärmlauf
5.395,299 ms, beste von drei **5.259,616 ms**.

### Schlussfolgerung — ausdrücklich

> **Nein, die Erklärung trägt nicht. `MessageAction.SOSID` erklärt die 43,9 Prozent aus M13 nicht —
> die Ursache ist eine andere: Die Ablaufdefinition nummeriert ihre Schritte nicht lückenlos, die
> Ausführung dagegen fortlaufend. `Message.SOSActionID` und `MessageAction.SOSActionID` sind
> Positionen, `SOSAction.SOSActionID` ist ein Schlüssel.**
>
> **Und ja, es gibt Nachrichten mit mehr als einem Ablauf** — 2,51 Prozent (Fenster A) und
> 2,22 Prozent (Fenster B), bis zu drei Abläufe auf einer Nachricht. Für sie hat kein einzelnes
> Gerüst Gültigkeit.

Die Belegkette zur ersten Hälfte:

1. **Null verwaiste Verweise auf Zeilen mit abweichendem Ablauf.** Wäre die Vermutung richtig, wäre
   diese Zahl die ganze Erklärung; sie ist null. Alle 11.956 verwaisten Aktionen liegen auf Zeilen
   mit **gleichem** `SOSID`.
2. **Die Verwaisung hängt an der Kennung, nicht am Ablauf.** 3.985 von 3.986 verwaisten Nachrichten
   in Fenster A tragen `Message.SOSActionID = 2`; alle anderen Kennungen verwaisen null Mal.
3. **Der betroffene Ablauf hat keinen Schritt 2.** Er definiert 1, 98 und 99. Ein Verweis auf 2 muss
   deshalb ins Leere laufen — nicht weil sich etwas geändert hätte, sondern weil zwei verschiedene
   Zählweisen aufeinandertreffen.
4. **Der ausgeführte Baustein ist derselbe.** Der geplante Schritt 98 trägt die Marke `FTPSender`,
   die 3.985 namenlosen Aktionen tragen sie ebenfalls. Es fehlt nicht der Schritt, es fehlt die
   Übersetzung seiner Nummer.
5. **Lücken in der Nummerierung sind kein Einzelfall:** 257 von 1.777 Abläufen (14,5 %) haben eine
   größte Kennung, die über ihrer Schrittzahl liegt; 233 nutzen Kennungen ab 99.

**Die Schwachstelle, die dazugehört.** Die Belegkette erklärt die 43,9 Prozent aus M13 **plausibel
und vollständig für Fenster A**, aber sie ist nicht über den Gesamtbestand nachgerechnet. M13 hat
99,995 Prozent der verwaisten Verweise bei `FINISHED` gemessen; ob dort dieselbe Mechanik greift oder
zusätzlich eine zweite (etwa tatsächlich geänderte Abläufe), ist **nicht gemessen** — ein
Gesamtdurchlauf hätte 15 bis 23 Sekunden gekostet und war für diese Frage nicht freigegeben. Die
Aussage ist also: *Die Vermutung „anderer Ablauf" ist widerlegt, und für den gemessenen Ausschnitt ist
die Ursache die Nummerierung.* Sie ist nicht: *Es gibt keine geänderten Abläufe.*

Zweitens beruht Punkt 3 und 4 auf **einem einzigen Ablauf**. Er stellt in beiden Fenstern die
erdrückende Mehrheit der Fälle, aber ein Befund an einer einzigen Ablaufdefinition ist ein Befund an
einer einzigen Ablaufdefinition.

---

## M21 — Trägt das Gerüst der geplanten Schritte?

### Statement

```sql
SELECT v.geplant, v.ausgefuehrt, COUNT(*) AS nachrichten
FROM (
  SELECT x.MessageID, x.ausgefuehrt,
         (SELECT COUNT(*) FROM SOSAction sa WHERE sa.SOSID = x.sosid) AS geplant
  FROM (
    SELECT ma.MessageID, MIN(ma.SOSID) AS sosid, COUNT(*) AS ausgefuehrt
    FROM Message m
    JOIN MessageAction ma ON ma.MessageID = m.MessageID
    WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
      AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
      AND ma.SOSActionID <> 0          -- Kriterium aus S1
    GROUP BY ma.MessageID
    HAVING COUNT(DISTINCT ma.SOSID) = 1
  ) x
) v
GROUP BY v.geplant, v.ausgefuehrt
ORDER BY v.geplant, v.ausgefuehrt;
```

### EXPLAIN

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | `<derived4>` | `ALL` | — | — | — | 11.812 | `Using temporary; Using filesort` |
| 4 | `m` | `range` | `MessageLastUpdateIDX` | 5 | — | 11.812 | `Using where; Using index; Using temporary; Using filesort` |
| 4 | `ma` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 1 | `Using where` |
| 3 | `sa` | `ref` | `PRIMARY` | 146 | `x.sosid` | 1 | `Using index` |

### Ergebnis — Fenster A

| geplant | ausgeführt | Nachrichten | |
|---|---|---|---|
| 1 | 1 | 532 | passt |
| 2 | 1 | 85 | weniger |
| 2 | 2 | 109 | passt |
| 2 | **4** | **9** | **mehr als geplant** |
| 3 | 1 | 2 | weniger |
| 3 | 2 | **4.249** | weniger |
| 3 | 3 | 319 | passt |
| 3 | **5** | **10** | **mehr als geplant** |
| 4 | 2 | 1 | weniger |
| 4 | 3 | 26 | weniger |
| 4 | 4 | 749 | passt |

Laufzeit: Aufwärmlauf 119,327 ms, beste von fünf **117,094 ms**.

### Ergebnis — Fenster B

| geplant | ausgeführt | Nachrichten | |
|---|---|---|---|
| 1 | 1 | 41.749 | passt |
| 2 | 1 | 2.115 | weniger |
| 2 | 2 | 5.258 | passt |
| 2 | **3** | **1** | **mehr** |
| 2 | **4** | **1.134** | **mehr** |
| 2 | **5** | **1** | **mehr** |
| 3 | 1 | 43 | weniger |
| 3 | 2 | **108.265** | weniger |
| 3 | 3 | 25.567 | passt |
| 3 | **4** | **12** | **mehr** |
| 3 | **5** | **767** | **mehr** |
| 3 | **6** | **3** | **mehr** |
| 4 | 2 | 30 | weniger |
| 4 | 3 | 2.321 | weniger |
| 4 | 4 | 22.091 | passt |
| 5 | 3 | 74 | weniger |
| 5 | 4 | 117 | weniger |

Laufzeit: Aufwärmlauf 5.442,490 ms, beste von drei **5.455,085 ms**.

### Zusammengefasst

| | Fenster A | Fenster B |
|---|---|---|
| Nachrichten mit genau einem Ablauf | 6.091 | 209.548 |
| **ausgeführt = geplant** | 1.709 — **28,06 %** | 94.665 — **45,18 %** |

| ausgeführt < geplant | 4.363 — 71,63 % | 112.965 — 53,91 % |
| **ausgeführt > geplant** | **19 — 0,31 %** | **1.918 — 0,92 %** |

Laufzeiten: Fenster A Aufwärmlauf 560,447 ms, beste von fünf **557,190 ms**; Fenster B Aufwärmlauf
21.395,741 ms, beste von drei **21.782,202 ms**.

> **Zur Grundgesamtheit, damit die Zahl nicht gegen M20 stolpert.** M20 zählt **6.092** Nachrichten
> mit genau einem Ablauf, M21 rechnet mit **6.091**. Der Unterschied ist der Ausschluss des
> Metadaten-Schritts, den M21 **vor** dem `HAVING` anwendet — nachgerechnet statt vermutet
> (593,3 ms, ein Lauf):
>
> | Kennzahl | Nachrichten |
> |---|---|
> | trägt **nur** den Metadaten-Schritt und fällt ganz heraus | **1** |
> | hat mit und ohne Metadaten-Schritt genau einen Ablauf | **6.091** |
> | wird **erst durch** den Ausschluss zur Ein-Ablauf-Nachricht | **0** |
>
> Die eine herausfallende Nachricht ist die mit genau einer Aktion aus M15 (1). Beide Zahlen messen
> also leicht Verschiedenes und sind beide richtig.

### Schlussfolgerung — ausdrücklich

> **Das Gerüst trägt in weniger als der Hälfte der Fälle vollständig, und in 0,31 bis 0,92 Prozent
> ist es nachweislich falsch.** In 19 (Fenster A) beziehungsweise 1.918 Nachrichten (Fenster B) sind
> **mehr** Schritte ausgeführt worden, als der Ablauf heute definiert — dort ist die
> Ablaufdefinition seit der Ausführung geändert worden, und ein Gerüst aus ihr wäre eine Lüge über
> eine Nachricht, die es anders erlebt hat.

Die Belegkette und ihre Größenordnungen:

1. **Vollständige Übereinstimmung ist der Minderheitsfall:** 28,06 Prozent (Fenster A) und
   45,18 Prozent (Fenster B).
2. **Der Regelfall ist „weniger ausgeführt als geplant"** — 71,63 beziehungsweise 53,91 Prozent. Das
   ist **kein** Fehler: Eine Nachricht, die einen Zweig nicht nimmt oder deren Ablauf früher endet,
   führt weniger Schritte aus als definiert. Für ein Gerüst ist genau das der Normalfall, den es
   darstellen soll — geplante Schritte, von denen einige nicht erreicht wurden.
3. **Der ausschließende Fall ist „mehr ausgeführt als geplant".** Er ist selten, aber er existiert in
   beiden Fenstern und wächst mit dem Zeitraum: 0,31 Prozent über einen Tag, 0,92 Prozent über einen
   Monat. Dass der Anteil mit der Fensterbreite steigt, passt zu der Lesart, dass Abläufe zwischen
   Ausführung und heute geändert werden — je älter die Nachricht, desto wahrscheinlicher.
4. **Die zweite Bedingung steht in M20:** 2,2 bis 2,5 Prozent der Nachrichten haben Schritte aus
   **mehr als einem** Ablauf. Für sie gibt es kein einzelnes Gerüst, unabhängig davon, ob die Zahlen
   zusammenpassen. Diese Nachrichten sind in der Auswertung oben gar nicht enthalten — `HAVING
   COUNT(DISTINCT ma.SOSID) = 1` schließt sie aus.

Zusammen heißt das: Ein Gerüst darf nur gezeigt werden, wenn **beide** Bedingungen erfüllt sind —
genau ein Ablauf **und** nicht mehr ausgeführte als geplante Schritte. Wie viele Nachrichten das
gemeinsam trifft, ist hier nicht gemessen; die beiden Bedingungen sind an unterschiedlichen
Grundgesamtheiten erhoben und dürfen nicht multipliziert werden.

**Die Schwachstelle, die dazugehört.** Verglichen werden **Anzahlen**, nicht Schritte. Dass eine
Nachricht drei geplante und drei ausgeführte Schritte hat, heißt nicht, dass es dieselben drei sind
— M20 hat gerade gezeigt, dass die Kennungen auseinanderlaufen können. Ein Gerüst, das sich auf die
Gleichheit der Zahlen verlässt, könnte drei richtige Kästchen mit drei falschen Beschriftungen
füllen. Der Beweis, den M15 für die benannten Schritte führt (Vergleich des ausgeführten mit dem
geplanten Baustein), wäre auch hier zu führen, **bevor** ein Gerüst gebaut wird; diese Messung führt
ihn nicht.

Zweitens ist `MIN(ma.SOSID)` in der inneren Abfrage nur deshalb unbedenklich, weil `HAVING
COUNT(DISTINCT ma.SOSID) = 1` bereits sicherstellt, dass es nur einen Wert gibt. Wer die
`HAVING`-Zeile entfernt, bekommt eine stillschweigend falsche Auswertung.

---

## M22 — Ein Vorgang oder ein Muster?

**Freigegebener Verstoß gegen Betriebsregel 1** (Begründung und Kosten siehe
[Abschnitt 0](#betriebsregeln-und-eine-bewusste-abweichung-davon) und die Freigabe in der
Aufgabenstellung): Es gibt im gesamten Bestand nur 95 offene Aktionen; ein Zeitfenster über `Message`
schnitte gerade die Zeilen weg, um die es geht.

### Statement (1) — offene Aktionen nach Status

```sql
SELECT m.MessageStatus,
       COUNT(*) AS offene_aktionen,
       MIN(ma.MessageActionStart) AS frueheste,
       MAX(ma.MessageActionStart) AS spaeteste,
       COUNT(DISTINCT DATE(ma.MessageActionStart)) AS verschiedene_tage
FROM MessageAction ma
JOIN Message m ON m.MessageID = ma.MessageID
WHERE ma.MessageActionEnd IS NULL
GROUP BY m.MessageStatus
ORDER BY offene_aktionen DESC;
```

**EXPLAIN**

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | `m` | `index` | `MessageStatusIDX` | 123 | — | 3.560.486 | `Using index; Using temporary; Using filesort` |
| 1 | `ma` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 1 | `Using where` |

**Ergebnis**

| `MessageStatus` | offene Aktionen | früheste | späteste | **verschiedene Tage** |
|---|---|---|---|---|
| `ERROR_TIMEOUT` | 49 | `2025-12-30 04:08:45` | `2025-12-30 04:09:47` | **1** |
| `FINISHED` | 39 | `2024-12-01 13:40:07` | `2025-07-06 22:50:23` | **29** |
| `CHECKED` | 7 | `2024-10-18 08:57:17` | `2025-07-30 06:52:25` | **7** |

**Laufzeiten, neu gemessen:**

| Statement | Vorgängerfassung | jetzt | N |
|---|---|---|---|
| `COUNT(*)` der offenen Aktionen (Ergebnis unverändert **95**) | 3.013 ms | **2.847,888 ms** | 3 (Aufwärmlauf 2.886,394 ms) |
| Aufschlüsselung nach Status | 23.144 ms | **23.273,352 ms** | 3 (Aufwärmlauf 23.595,204 ms) |

### Statement (2) — die zeitliche Lage der 52 `ERROR_TIMEOUT`

Nutzt `MessageStatusIDX` und betrifft 52 Zeilen; fällt nicht unter die Freigabe, sondern ist von
sich aus billig. Um die entscheidende Spalte erweitert — ob die Nachricht eine offene Aktion trägt:

```sql
SELECT DATE(m.MessageLastUpdate) AS tag, DAYNAME(m.MessageLastUpdate) AS wochentag,
       COUNT(*) AS nachrichten,
       MIN(m.MessageLastUpdate) AS frueheste, MAX(m.MessageLastUpdate) AS spaeteste,
       SUM((SELECT COUNT(*) FROM MessageAction ma
            WHERE ma.MessageID = m.MessageID
              AND ma.MessageActionEnd IS NULL) > 0) AS mit_offener_aktion
FROM Message m
WHERE m.MessageStatus = 'ERROR_TIMEOUT'
GROUP BY tag, wochentag ORDER BY tag;
```

**EXPLAIN**: `m` `ref` über `MessageStatusIDX` (`key_len` 123, `ref = const`, 52 Zeilen,
`Using index condition; Using where; Using temporary; Using filesort`), Unterabfrage `ma` `ref` über
`MessageAction.PRIMARY`.

| Tag | Wochentag | Nachrichten | früheste | späteste | **mit offener Aktion** |
|---|---|---|---|---|---|
| 2025-12-13 | **Samstag** | 1 | `07:36:05` | `07:36:05` | **0** |
| 2025-12-20 | **Samstag** | 1 | `07:21:11` | `07:21:11` | **0** |
| 2025-12-27 | **Samstag** | 1 | `07:22:11` | `07:22:11` | **0** |
| 2025-12-30 | Dienstag | **49** | `04:08:45` | `04:09:47` | **49** |

Laufzeit: Aufwärmlauf 3,750 ms, beste von fünf **2,922 ms**.

### Schlussfolgerung — ausdrücklich

> **Nein, nicht alle 52 liegen in derselben Minute — und die Antwort ist deshalb nicht „Muster" oder
> „Vorgang", sondern beides, für zwei verschiedene Dinge.**
>
> **Der Status `ERROR_TIMEOUT` hat ein Muster:** Drei der 52 Nachrichten liegen als Einzelfälle auf
> **drei aufeinanderfolgenden Samstagen** (13., 20. und 27.12.2025), jeweils zwischen 07:21 und
> 07:36 Uhr. Ein Wochenrhythmus zur selben Tageszeit ist kein Zufall zweier Zahlen.
>
> **Der offene Schritt ist ein einziger Vorgang:** Alle 49 übrigen liegen am Dienstag, dem
> 30.12.2025, in einer Spanne von **62 Sekunden**, an genau einem Tag — und **nur sie** tragen eine
> offene Aktion. Die drei Samstags-Einzelfälle tragen **keine**.

Was daraus für die Anzeige folgt, als Einschränkung neben jede Aussage über den Hänger — in der Form,
die [M13](messungen-schritt4.md#m13--trägt-sosactionid-einen-lesbaren-namen) für den aktuellen
Schritt festgehalten hat:

> ⚠️ **Die Kombination „Fehler durch Zeitüberschreitung mit offen gebliebenem Schritt" ist in der
> Testkopie genau einmal beobachtet worden** — in einer Minute, an einem Tag, mit 49 gleichzeitig
> betroffenen Nachrichten. Sie ist damit **kein** Beleg dafür, wie ein Hänger im Normalbetrieb
> aussieht. Was sich lokal vorführen lässt, ist ein Massenereignis; was die Produktion zeigen wird,
> ist vermutlich der Einzelfall.

**Die Gegenprobe, die die Aussage rettet:** Offene Aktionen sind **nicht** auf dieses Ereignis
beschränkt. Die 39 bei `FINISHED` verteilen sich über **29 verschiedene Tage**, die 7 bei `CHECKED`
über **sieben** — zusammen 46 der 95, gestreut über den gesamten Bestand von Oktober 2024 bis Juli
2025. **Das Merkmal `MessageActionEnd IS NULL` selbst ist also alltäglich**, nur seine Verbindung mit
`ERROR_TIMEOUT` ist es nicht. Die Anzeige eines offenen Schritts lässt sich damit an echten,
verstreuten Daten prüfen — nur eben nicht an einem Fehlerfall.

**Die Schwachstelle, die dazugehört.** Der 30.12.2025 04:09:47 ist auf die Sekunde der jüngste
Zeitpunkt des dichten Bestands ([M9](messungen-schritt4.md#m9--anker-für-die-dev-uhr)). Dass 49
Nachrichten genau dort mit offenem Schritt stehen bleiben, lässt **zwei** Lesarten zu, die diese
Messung nicht trennt: ein echter Vorfall im Quellsystem, oder der Schnitt der Kopie mitten in den
laufenden Betrieb hinein. Gegen die zweite spricht, dass die Nachrichten bereits `ERROR_TIMEOUT`
tragen und nicht `RUNNING`; für sie spricht die exakte Koinzidenz mit dem Datenende. **Beides ist
Deutung, keine Messung** — die Frage ließe sich nur an der Produktion klären, und dorthin geht keine
Erhebung.

---

## Korrekturen an den Vorlage-Statements

**Keine Tabelle und keine Spalte aus der Aufgabenstellung fehlt.** Sämtliche verwendeten Namen
existieren genau so: `MessageAction.MessageID`, `MessageAction.MessageActionID`,
`MessageAction.MessageActionStart`, `MessageAction.MessageActionEnd`, `MessageAction.ServiceID`,
`MessageAction.SOSActionServiceProperties`, `MessageAction.SOSActionTimeout`,
`MessageProperty.MessageID`, `MessageProperty.MessageActionID`,
`MessageProperty.MessagePropertyName`, `MessageProperty.MessagePropertyValue`, `Service.ServiceID`,
`SOSAction.SOSID`, `SOSAction.SOSActionID`, `SOSAction.SOSActionName`,
`SOSAction.SOSActionServiceProperties`, `Message.SOSID`, `Message.MessageLastUpdate`,
`Message.MessageStatus`.

**Das ist ein Ergebnis und keine Formalie** — und es ist zugleich unvollständig, denn M14 hat das
Gegenteil des erwarteten Problems gefunden: **nicht fehlende, sondern zusätzliche Spalten.** Drei
Änderungen an den Vorlagen folgen daraus:

| # | Vorlage | Was tatsächlich gelaufen ist | Warum |
|---|---|---|---|
| 1 | M15 (2), Join `sa.SOSActionID = ma.MessageActionID` | zusätzlich **a2** über `ma.SOSActionID` | Die Aufgabenstellung verlangt das ausdrücklich für den Fall, dass die Spalte existiert. Sie existiert (M14) |
| 2 | — | zusätzlich **a3** über `sa.SOSID = ma.SOSID` statt `m.SOSID` | `MessageAction` trägt ein **eigenes** `SOSID`, das in keiner Vorlage und in keiner Dokumentationsdatei vorkam. Erst diese Fassung ergibt null Abweichungen bei der ersten Marke |
| 3 | M15 (3), „Setze den tatsächlichen Namen ein; rate ihn nicht" | `s.ServiceName` | Die Spalte heißt so (M14). Zusätzlich erhoben: `s.ServiceTypeID`, weil drei Dienste denselben Namensstamm tragen |

Vier weitere Auswertungen sind **über die Vorlagen hinaus** gelaufen, jede mit ihrem Anlass:

| Auswertung | Anlass |
|---|---|
| Verteilung der `MessageActionID`-Werte und ihre Paarung mit `SOSActionID` (M15 1) | Der Wertebereich 0 bis 502 bei neun verschiedenen Werten war ohne Aufschlüsselung nicht lesbar |
| Abdeckung über die **echten** Schritte (M15 2) | Die Quote über alle Aktionen zählt Schritt `0` mit, der nie angezeigt wird, und ist damit systematisch zu niedrig |
| Offene Aktionen im Gesamtbestand (M16 3) | Fenster A enthält keine. Ohne diese Gegenprobe wäre die Leitfrage „woran ist der Hänger erkennbar" unbeantwortet geblieben. Abweichung von Betriebsregel 1, in Abschnitt 0 ausgewiesen |
| `Message.VFN` gegen `OFTPReader.VFN` (M17 4) | Die Aufgabenstellung nennt `Message.VFN` als „von besonderem Interesse", und die Nachbarschaft ist der einzige Hinweis, den die Daten hergeben |

---

## Widersprüche zur Dokumentation

Was von der bestehenden Dokumentation abweicht. **Hier wird nichts geändert** — das Nachziehen der
Dateien ist ein eigener Vorgang.

| Datei | Was dort steht | Was gemessen ist |
|---|---|---|
| [`datenmodell.md`](datenmodell.md) §3 (`MessageAction`) | „PK `(MessageID, MessageActionID)` · `MessageActionStart` / `MessageActionEnd` · `ServiceID` · `SOSActionServiceProperties` · `SOSActionTimeout`" | **Zwei Spalten fehlen in der Aufzählung: `SOSID` und `SOSActionID`, beide `NOT NULL`** (M14). Genau sie tragen die Auflösung des Schrittnamens |
| [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 (`MessageAction`) | dieselbe Aufzählung | dasselbe |
| [`datenmodell.md`](datenmodell.md) §3 (`MessageProperty`) | „Rund **zehn** Zeilen pro Nachricht" | **22,57** (Fenster A) bzw. **22,88** (Fenster B); Minimum 14, Maximum 38 (M17 1) |
| [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 | „Rund **vierzehn** Zeilen je Nachricht" | 14,05 ist das Mittel über den **Gesamtbestand** (46.964.279 / 3.341.519) und damit rekonstruierbar, aber nicht der Wert im dichten Bestand |
| [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 | „durchschnittlich **1,3 Kilobyte je Zeile**" | Richtig als **Speicherbedarf**, irreführend als Datenmenge: 1.299,6 B/Zeile bestehen aus **321,3 B Daten und 978,3 B Index** (M14). Die tatsächliche mittlere **Wertlänge** beträgt **26,4 Zeichen**, und alle Eigenschaften einer Nachricht zusammen wiegen **595 Byte** (M17 2) |
| [`datenmodell.md`](datenmodell.md) §3 | zehn bekannte `MessageProperty`-Namen | **101** im Tagesfenster, **119** im Monatsfenster. Neun der zehn kommen vor; **`Message.InterchangeNumber` fehlt im Tagesfenster** und hat im Monatsfenster 800 Zeilen (M17 2) |
| [`datenmodell.md`](datenmodell.md) §3, [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 | „Präfix-Indizes über 50 Zeichen" auf `MessagePropertyValue` | **Bestätigt.** `SUB_PART = 50`, und zwar in **zwei** Indizes: `MessagePropertyValueIDX` und `MessagePropertyNameValueIDX` (M14) |
| [`datenmodell.md`](datenmodell.md) §3 | `Message.Payload.GUID` im Format `FILESTOREPROD09\|<uuid>` | **Bestätigt.** 6.249 von 6.249 Werten sind exakt 52 Zeichen lang und enthalten ein Pipe-Zeichen (M17 2, M17 4) |
| [`messungen-schritt4.md`](messungen-schritt4.md) Auffälligkeit G | `information_schema` **überschätzt** (`Message` 3.560.486 gegen 3.341.519 gezählt) | Bei `MessageAction` **unter**schätzt sie: 10.215.743 gegen **10.308.590** gezählt (M14). Die Richtung des Fehlers ist nicht konstant |
| [`messungen-schritt4.md`](messungen-schritt4.md) M8 | `SOSAction.SOSActionTimeout` kennt `NULL` (11 Zeilen) und `300` (2 Zeilen) neben `1800` | In den **ausgeführten** Aktionen des Fensters kommen nur `1800` und `0` vor, nie `NULL`, nie `300` (M16 4) |
| [`docs/README.md`](README.md) Vorschau | `prozessschritte-uebersetzung.md` — „Zuordnungstabelle `SOSActionServiceProperties` → Klartext" für Schritt 5 | Die Datei wird nach der Schlussfolgerung von M15 **nicht gebraucht**. Der Klartext kommt aus `SOSAction.SOSActionName` |

---

## Offene Entscheidungen

Was aus den Zahlen folgen *könnte* — als Frage formuliert, nicht als Antwort.

### Zur Zeitleiste

1. Schritt `0` ist kein Ablaufschritt, sondern der Ort der Nachrichten-Metadaten (M15 1, M17 3).
   Wird er in der Zeitleiste **ganz weggelassen**, oder erscheint er als „Nachricht eingegangen" mit
   seinem Zeitstempel — der der einzige gemessene Anhaltspunkt für den fachlichen Start ist
   (Regel Q2: `MIN(MessageAction.MessageActionStart)`)?
2. Die 22 bis 29 Prozent nicht aufgelösten echten Schritte tragen alle ihre
   `SOSActionServiceProperties`. Erscheint dort der **ganze** Rohwert
   (`NXS_MERGE|…|WAIT|30M|…`, bis 208 Zeichen) oder nur die **erste Marke**? Das Vokabular der
   ersten Marken ist mit 78 Werten handhabbar, 32 davon mit weniger als zehn Zeilen.
   > **Geschärft durch [M19](#m19--wie-groß-müsste-die-zuordnungstabelle-sein):** Die 78 Marken sind
   > das Vokabular **aller** Schritte. Unter den **namenlosen** sind es drei (Fenster A) beziehungsweise
   > vier (Fenster B), und jede steht für genau einen ganzen Wert — die Unterscheidung „ganzer Wert
   > oder erste Marke" macht für den Rückfall also keinen Unterschied.
3. Wenn nur die erste Marke erscheint: Bekommt sie ein kleines Glossar, oder bleibt sie unübersetzt?
   Das wäre eine Zuordnungstabelle in ganz anderem Umfang als die im Plantext beauftragte — 78
   Einträge statt 169 ganzer Werte, und nur für den Rückfall.
   > **Geschärft durch [M19](#m19--wie-groß-müsste-die-zuordnungstabelle-sein):** Für den Rückfall
   > wären es **eine** Zeile für 90 Prozent und **drei** für 99 Prozent. Drei der vier Marken sind
   > bereits lesbarer Klartext; zu übersetzen wäre genau eine (`FTPSender`) — ausgerechnet die, die
   > anderswo auf 25 verschiedene `SOSActionName` auflöst. Die Frage bleibt offen, aber sie lautet
   > jetzt anders: nicht „wie groß", sondern „**ob überhaupt**, oder stattdessen über die Marke im
   > selben Ablauf" (neue Frage 13).
4. Der Zwischenraum bei wartenden Nachrichten (M16 3) ist ein eigener Zustand. Wird er als eigene
   Zeile in der Zeitleiste gezeigt („wartet seit …"), oder als Zustand der Zeitleiste als Ganzes?
5. `MessageActionID = 500` und `502` kommen je 20-mal vor, tragen Bausteine, lösen nie auf, und
   `502` hat keine einzige `MessageProperty`-Zeile. Was bedeuten diese beiden Kennungen, und
   gehören sie in die Zeitleiste?

### Zum Hänger und zum Timeout

6. ~~Die 49 offenen Aktionen bei `ERROR_TIMEOUT` liegen sämtlich in 62 Sekunden, die auf die Sekunde
   am Ende des dichten Bestands enden (M16 3). Ist das ein fachliches Muster oder die Schnittkante
   der Testkopie?~~
   **Beantwortet durch [M22](#m22--ein-vorgang-oder-ein-muster): beides, für zwei verschiedene
   Dinge.** Der **Status** `ERROR_TIMEOUT` hat ein Muster — drei Einzelfälle an drei
   aufeinanderfolgenden Samstagen zwischen 07:21 und 07:36 Uhr, keiner davon mit offener Aktion. Der
   **offene Schritt** ist ein einziger Vorgang: 49 Nachrichten in 62 Sekunden an einem Tag, und nur
   sie tragen eine offene Aktion. Für die Anzeige heißt das: Die Kombination „Fehler durch
   Zeitüberschreitung **mit** offenem Schritt" ist lokal genau einmal beobachtbar und taugt nicht als
   Vorlage für den Normalbetrieb — das Merkmal `MessageActionEnd IS NULL` selbst dagegen ist mit 46
   von 95 Fällen über 36 verschiedene Tage gestreut und prüfbar.
   **Offen bleibt** die Herkunft der Häufung selbst: echter Vorfall im Quellsystem oder Schnitt der
   Kopie in den laufenden Betrieb. Das ließe sich nur an der Produktion klären, und dorthin geht keine
   Erhebung.
7. 124 von 14.063 Schritten überschreiten ihre eigene Frist um mehr als das Achtundvierzigfache,
   während zwischen Frist und 24 Stunden nichts liegt (M16 1, M16 4). Wird ein Schritt über der
   Frist in der Zeitleiste gekennzeichnet — und wenn ja, ist das dieselbe Kennzeichnung wie die
   Problemkategorie „Überfällig" aus Regel Q3, oder eine andere Ebene?
8. `SOSActionTimeout = 0` steht auf allen 6.249 Schritt-`0`-Zeilen und auf 40 echten Schritten
   (M16 4). Bedeutet die `0` hier „kein Timeout" wie bei `Message.MessageTimeout` (offene Frage 7
   aus Schritt 4), oder „keine Frist vorgesehen"?

### Zu den Eigenschaften

9. 101 Namen im Tag, 119 im Monat, davon 91 undokumentiert. Nach welchem Kriterium entsteht die
   kuratierte Auswahl — nach Abdeckung, nach Familie (`Message.*` gegen `<Dienst>.*`), oder
   namentlich? Und was prüft der Wächter-Test dann: dass die kuratierten Namen noch vorkommen, oder
   dass keine neuen unbemerkt dazukommen?
10. `Message.VFN` spiegelt in 204 von 218 Fällen `OFTPReader.VFN` (M17 4). Reicht das, um das Feld
    als „Dateiname aus der OFTP-Übertragung" zu beschriften, oder braucht es eine Auskunft aus dem
    Altsystem, bevor ein Etikett vergeben wird? Regel Q4 („nicht geraten") spricht für das Zweite.
11. Der Primärschlüssel erlaubt denselben Namen mehrfach je Nachricht auf verschiedenen Schritten,
    und `Converter.Payload.GUID` nutzt das (7.862 Zeilen auf 6.149 Nachrichten). Zeigt die
    Detailansicht alle Vorkommen, das letzte, oder das je Schritt?

### Zur Darstellung dieses Dokuments

12. ~~Die zwanzig häufigsten Bausteinmarken sind unmaskiert abgedruckt (Begründung in Abschnitt 0).
    Bleibt das so, oder werden die Mandantenkürzel `NXS`, `IBIS` und `VTG` wie Partnernamen als
    `<Kunde>` maskiert?~~
    **Entschieden am 07.08.2026: Sie bleiben unmaskiert.** `docs/README.md` ist entsprechend
    geschärft und unterscheidet jetzt ausdrücklich zwischen **Nutzdaten und Namen Dritter** (geschützt:
    Belegnummern, `MessagePropertyValue`-Inhalte, **Partner**namen, Zugangsdaten, Hostnamen) und dem
    **Konfigurationsvokabular des Altsystems** (nicht geschützt: Bausteinmarken, Prozess- und
    Ablaufnamen, Mandantenkürzel). Begründung: Die zehn Mandanten stehen mit vollem Firmennamen in
    [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2; sie andernorts zu schwärzen, hilft
    niemandem. Enthält eine Marke oder ein Ablaufname den Namen eines **Partners**, wird dieser
    Bestandteil weiterhin als `<Kunde>` maskiert, wie in
    [M13](messungen-schritt4.md#m13--trägt-sosactionid-einen-lesbaren-namen) geschehen. Die
    Maskierung der Knotenkennungen in M15 (3) bleibt unberührt — sie sind Hostnamen und damit
    geschützt.

### Aus dem Nachtrag vom 07.08.2026

13. **Statt einer Zuordnungstabelle: Auflösung über die Marke im selben Ablauf?** 96 bis 99 Prozent
    der namenlosen Schritte finden dort genau einen Schritt mit derselben ersten Marke, nie mehrere
    (M19). Dieser Weg lieferte den **echten** `SOSActionName` statt eines Sammelbegriffs und bräuchte
    keine gepflegte Zeile. Er ist aber nur auf **Eindeutigkeit** geprüft, nicht auf Richtigkeit —
    der Beweis, der M15 trägt, ist hier konstruktionsbedingt nicht führbar. Ist eine Auflösung, die
    plausibel und eindeutig, aber unbeweisbar ist, besser als ein Rohwert? Regel Q4 („nicht geraten")
    spricht dagegen, die Nutzbarkeit dafür.
14. **Wie viele Nachrichten erfüllen beide Gerüst-Bedingungen gemeinsam?** M21 misst „ausgeführt
    gegen geplant" nur an Nachrichten mit genau einem Ablauf, M20 misst den Anteil mit mehreren
    Abläufen an allen Nachrichten. Die beiden Zahlen stehen an verschiedenen Grundgesamtheiten und
    dürfen nicht multipliziert werden. Vor dem Bau eines Gerüsts wäre die gemeinsame Zahl zu erheben.
15. **Trägt das Gerüst auch inhaltlich?** M21 vergleicht Anzahlen, nicht Schritte. Drei geplante und
    drei ausgeführte Schritte müssen nicht dieselben drei sein — M20 zeigt gerade, dass die
    Kennungen auseinanderlaufen. Vor dem Bau wäre derselbe Markenvergleich zu führen, der M15 trägt.
16. **Ist die Nummerierungslücke ein Muster mit Bedeutung?** 257 von 1.777 Abläufen (14,5 %) haben
    eine größte Schrittkennung über ihrer Schrittzahl, 233 nutzen Kennungen ab 99 (M20). Steht `99`
    im Quellsystem für „letzter Schritt" und `98` für „vorletzter"? Falls ja, wäre die Zuordnung von
    ausgeführter Position zu geplanter Kennung berechenbar statt geraten — falls nein, ist sie es
    nicht. Nicht gemessen; es wäre eine Auskunft aus dem Altsystem, keine Abfrage.

---

## Anhang: Laufzeiten auf einen Blick

Beste von N Läufen nach einem Aufwärmlauf, serverseitig über `SET profiling = 1` gemessen.

| Messung | Statement | N | Laufzeit | Zugriffspfad |
|---|---|---|---|---|
| M14 | Spalten aus `information_schema.COLUMNS` | 1 | **1,3 ms** | — |
| M14 | Indizes aus `information_schema.STATISTICS` | 1 | **0,7 ms** | — |
| M14 | Größen aus `information_schema.TABLES` | 1 | **0,6 ms** | — |
| M14 | `COUNT(*)` über `MessageAction` | 1 | 28.341 ms | voller Durchlauf, 10,3 Mio. Zeilen |
| M14 | `COUNT(*)` über `Service` | 1 | 0,6 ms | 20 Zeilen |
| M15 (1) | Aktionen je Nachricht, Fenster A | 5 | **137,8 ms** | `range` `MessageLastUpdateIDX` + `ref` `MessageAction.PRIMARY`, `temporary` + `filesort` |
| M15 (1) | Wertebereich `MessageActionID` | 5 | **80,4 ms** | wie oben, `Using index` |
| M15 (1) | Verteilung der `MessageActionID`-Werte | 5 | **94,4 ms** | wie oben |
| M15 (1) | kleinste `MessageActionID` je Nachricht | 5 | **137,7 ms** | wie oben |
| M15 (1) | `MessageActionID` gegen `SOSActionID` | 5 | **250,9 ms** | wie oben, zusätzlich `eq_ref` auf `SOSAction.PRIMARY` |
| M15 (2) | **a1** Vorlage (`m.SOSID` + `ma.MessageActionID`) | 5 | **242,1 ms** | `eq_ref` `SOSAction.PRIMARY`, `key_len` **148**, `Using where` |
| M15 (2) | **a2** (`m.SOSID` + `ma.SOSActionID`) | 5 | **247,6 ms** | wie a1 |
| M15 (2) | **a3** (`ma.SOSID` + `ma.SOSActionID`) | 5 | **214,4 ms** | wie a1, **ohne** `Using where` auf `sa` |
| M15 (2) | a3 über **Fenster B** | 5 | **7.874,6 ms** | wie a3, Schätzung 409.756 |
| M15 (2) | Namensauflösung a3, Fenster A | 5 | **175,7 ms** | wie a3 |
| M15 (2) | Namensauflösung a1, Fenster A | 5 | **210,9 ms** | wie a1 |
| M15 (2) | Abdeckung echter Schritte, Fenster A | 5 | **203,6 ms** | wie a3 |
| M15 (2) | Abdeckung echter Schritte, Fenster B | 3 | **6.809,1 ms** | wie a3 |
| M15 (3) | Auflösung über `Service` | 5 | **129,2 ms** | `eq_ref` `Service.PRIMARY`, `key_len` 146 |
| M15 (3) | Dienstnamen mit Häufigkeit | 5 | **217,5 ms** | wie oben, `temporary` + `filesort` |
| M15 (4) | Vokabulargröße der Bausteine | 5 | **617,8 ms** | `range` + `ref`, `SUBSTRING_INDEX` über `mediumtext` |
| M15 (4) | zwanzig häufigste Marken | 5 | **257,3 ms** | wie oben |
| M16 (1) | offene Aktionen und Dauer | 5 | **130,1 ms** | `range` + `ref`, `Using index` auf `m` |
| M16 (1) | Dauerklassen | 5 | **129,9 ms** | wie oben |
| M16 (2) | größte Schrittfolge | 5 | **133,8 ms** | wie M15 (1) |
| M16 (3) | `SUSPENDED` / `RUNNING` | 5 | **34,9 ms** | `range` `MessageStatusIDX` (`key_len` 123) + zwei abhängige Unterabfragen |
| M16 (3) | Fehlerzustände | 5 | **216,3 ms** | wie oben |
| M16 (3) | **offene Aktionen im Gesamtbestand** | 1 | 3.013 ms | voller Durchlauf über `MessageAction` — **kein Muster für Code** |
| M16 (3) | Aktionen ohne Start, Gesamtbestand | 1 | 2.940 ms | wie oben |
| M16 (3) | die 95 offenen Aktionen nach Status | 1 | 23.144 ms | voller Durchlauf + `eq_ref` auf `Message.PRIMARY` |
| M16 (3) | zeitliche Lage der `ERROR_TIMEOUT` | 1 | 1,6 ms | `range` `MessageStatusIDX` |
| M16 (4) | `SOSActionTimeout` je Wert | 5 | **82,2 ms** | `range` + `ref` |
| M16 (4) | `SOSActionTimeout` gegen Schritt `0` | 1 | 104,6 ms | wie oben |
| M16 (4) | Schritte über der Frist | 5 | **97,2 ms** | wie oben, `Using where` auf `ma` |
| M16 (4) | Frist gegen 24-Stunden-Grenze | 1 | 100,3 ms | wie oben |
| M17 (1) | Zeilen je Nachricht | 5 | **473,0 ms** | `range` + **`ref` `MessageProperty.PRIMARY`, `Using index`** |
| M17 (2) | Namen, Häufigkeit, Wertlängen (101 Zeilen) | 5 | **1.165,7 ms** | `range` + `ref` `PRIMARY`, `temporary` + `filesort` |
| M17 (2) | Kennzahlen und Bytes je Nachricht | 5 | **550,5 ms** | wie oben |
| M17 (2) | größte Nachricht in Bytes | 5 | **727,8 ms** | wie oben |
| M17 (2) | dieselben Kennzahlen über **Fenster B** | 3 | **19.821,7 ms** | wie oben, 4,9 Mio. Zeilen |
| M17 (3) | Verteilung über `MessageActionID` | 5 | **533,0 ms** | `ref` `PRIMARY`, `Using index` |
| M17 (3) | Schritte je Name | 5 | **596,6 ms** | wie oben |
| M17 (3) | die neun dokumentierten `Message.*`-Namen | 5 | **239,9 ms** | wie oben |
| M17 (4) | Zeichenklassen der häufigsten Namen | 5 | **1.730,7 ms** | wie oben, `REGEXP` über `mediumtext` — **kein Muster für Code** |
| M17 (4) | Längenverteilung `Message.VFN` | 1 | 74,3 ms | wie oben |
| M17 (4) | `Message.VFN` gegen `OFTPReader.VFN` | 1 | 211,5 ms | `ref` `PRIMARY` zweimal |
| M17 (4) | die 17 Werte von `Service.Type` | 1 | 221,2 ms | wie oben |

### Nachtrag vom 07.08.2026

| Messung | Statement | N | Laufzeit | Zugriffspfad |
|---|---|---|---|---|
| S1 | drei Kriterien gegeneinander, Fenster A | 5 | **113,4 ms** | `range` `MessageLastUpdateIDX` + `ref` `MessageAction.PRIMARY` |
| S1 | dieselbe Auswertung über **Fenster B** | 3 | **4.761,6 ms** | wie oben |
| M18 | Auflösung je `SOSActionID`, Fenster A | 5 | **223,6 ms** | wie oben + `eq_ref` `SOSAction.PRIMARY`, `key_len` 148 |
| M18 | dieselbe Auswertung über **Fenster B** | 3 | **7.825,3 ms** | wie oben |
| M19 | Marken der namenlosen Schritte, Fenster A | 5 | **177,4 ms** | wie M18, `sa` mit `Not exists` |
| M19 | dieselbe Auswertung über **Fenster B** | 3 | **5.914,0 ms** | wie oben |
| M19 | ganze Werte je Marke, Fenster A | 5 | **242,8 ms** | wie oben |
| M19 | ganze Werte je Marke, **Fenster B** | 3 | **7.292,6 ms** | wie oben |
| M19 + | trägt dieselbe Marke anderswo einen Namen, Fenster A | 1 | 155,0 ms | wie M18 |
| M19 + | dieselbe Frage über **Fenster B** | 1 | 5.169,7 ms | wie oben |
| M19 + | **Markenauflösung im selben Ablauf**, Fenster A | 5 | **558,2 ms** | wie M18 + abhängige Unterabfrage `ref` `SOSAction.PRIMARY` (`key_len` 146) |
| M19 + | dieselbe Auswertung über **Fenster B** | 3 | **15.042,2 ms** | wie oben |
| M20 | Vorlage-Statement (M13-Fassung), Fenster A | 5 | **143,0 ms** | `range` `MessageLastUpdateIDX` + `eq_ref` `SOSAction.PRIMARY` + `ref` `MessageAction.PRIMARY` |
| M20 + | M13-Fassung auf Nachrichtenebene, Fenster A | 5 | **60,4 ms** | `range` + `eq_ref`, ohne `MessageAction` |
| M20 + | verwaiste je `Message.SOSActionID` | 1 | 101,5 ms | wie oben + abgeleitete Tabelle über `SOSAction` |
| M20 + | Ablaufstruktur der namenlosen Schritte | 1 | 174,3 ms | wie M18 + abgeleitete Tabelle über `SOSAction` |
| M20 + | Schrittkennungen des einen betroffenen Ablaufs | 1 | 2,4 ms | `ref` `SOSAction_SOSFK` |
| M20 + | Lückenstatistik über alle 1.777 Abläufe | 1 | 4,3 ms | voller Durchlauf über `SOSAction` (3.944 Zeilen) |
| M20 | Abläufe je Nachricht, Fenster A | 5 | **118,2 ms** | wie M18, `temporary` + `filesort` |
| M20 | dieselbe Auswertung über **Fenster B** | 3 | **5.259,6 ms** | wie oben |
| M21 | geplant gegen ausgeführt, Fenster A | 5 | **117,1 ms** | `range` + `ref` + abhängige Unterabfrage `ref` `SOSAction.PRIMARY`, `Using index` |
| M21 | dieselbe Auswertung über **Fenster B** | 3 | **5.455,1 ms** | wie oben |
| M21 | Zusammenfassung, Fenster A | 5 | **557,2 ms** | wie oben |
| M21 + | Grundgesamtheit 6.092 gegen 6.091 nachgerechnet | 1 | 593,3 ms | wie M18, abgeleitete Tabelle je Nachricht |
| M21 | Zusammenfassung, **Fenster B** | 3 | **21.782,2 ms** | wie oben |
| M22 | `COUNT(*)` der offenen Aktionen, Gesamtbestand | 3 | **2.847,9 ms** | voller Durchlauf über `MessageAction` — **freigegeben, kein Muster für Code** |
| M22 | die 95 offenen Aktionen nach Status | 3 | **23.273,4 ms** | `index` `MessageStatusIDX` + `ref` `MessageAction.PRIMARY` — **freigegeben, kein Muster für Code** |
| M22 | zeitliche Lage der 52 `ERROR_TIMEOUT` | 5 | **2,9 ms** | `ref` `MessageStatusIDX`, `ref = const`, 52 Zeilen |

**Was der Nachtrag an Laufzeiten hinzufügt, bestätigt das Muster ein drittes Mal.** Jede Auswertung
mit Zeitfenster bleibt im Millisekundenbereich für Fenster A und im einstelligen Sekundenbereich für
Fenster B — der Faktor zwischen den Fenstern liegt durchgängig zwischen 30 und 45 und damit nahe am
Mengenverhältnis von 34. Die beiden Ausreißer nach oben sind erklärbar und beide gewollt: die
Markenauflösung in M19 (15,0 s) trägt eine abhängige Unterabfrage je namenloser Zeile, und die
M21-Zusammenfassung (21,8 s) eine je Nachricht. **Die zwei teuersten Statements sind die beiden
freigegebenen Durchläufe ohne Zeitfenster** (2,8 s und 23,3 s) — sie liegen innerhalb eines Prozents
der in der Ersterhebung gemessenen Werte (3,0 s und 23,1 s), was die Vergleichbarkeit der beiden
Messreihen belegt.

**Das Muster aus Schritt 4 gilt unverändert und wird hier um eine zweite Achse ergänzt.** Sobald ein
Zeitfenster gesetzt ist, arbeitet MariaDB im `range`-Zugriff über `MessageLastUpdate` und braucht
Millisekunden; ohne Zeitfenster wird jede Abfrage zu einem vollen Durchlauf von 3 bis 28 Sekunden
(M14, M16 3).

**Die zweite Achse ist die Zieltabelle.** `MessageAction` und `MessageProperty` hängen beide als
`ref` über das **Präfix ihres Primärschlüssels** an der vorgewählten `MessageID`-Menge — derselbe
Zugriffspfad wie die BAM-Nachladung in L9/L10. Er skaliert flach: 20.352 `MessageAction`-Zeilen
kosten 0,13 s, 141.037 `MessageProperty`-Zeilen 0,47 s, und der Sprung auf Fenster B (34-fache
Menge) kostet das 40-Fache — also linear und ohne böse Überraschung. **Der teuerste Posten dieser
Erhebung ist kein Join, sondern `SUBSTRING_INDEX` und `REGEXP` über `mediumtext`** (M15 4 mit
617,8 ms, M17 4 mit 1.730,7 ms bei sonst identischem Plan). Beides ist eine einmalige Erhebung und
kein Muster für Anwendungscode.
