# Messungen vor Schritt 8 — Rohdaten und Filestore

Erhoben am **17.08.2026** gegen die Testkopie (`GlassfishDB`) und gegen die Filestores
**`FILESTOREPROD09`** und **`FILESTOREPROD10`**.
Auftrag: [`messungen-schritt8-auftrag.md`](messungen-schritt8-auftrag.md), **Fassung 3** vom
17.08.2026.

**Diese Runde baut nichts und entscheidet nichts.** Die vorregistrierten Deutungen standen
vollständig in dieser Datei, **bevor** das erste Statement lief; die Ergebnisteile waren dabei leer.
Wo ein Ergebnis eine Entscheidung verlangt, steht sie unter „Offene Punkte" und ist nicht getroffen.

> **Diese Fassung ersetzt eine gleichnamige Erhebung vom 14.08.2026.** Der Auftraggeber hat die
> vollständige Wiederholung angeordnet, weil während des ersten Laufs Last auf den Filestores lag
> und die dort gemessenen Zeiten deshalb als verfälscht gelten. Was das für die übernommenen und
> nicht übernommenen Teile der alten Fassung bedeutet, steht unter „Abweichungen vom Auftrag".

> ## Das Ergebnis vorweg
>
> **Teil A ist vollständig gefahren (M52–M58, M64a). Teil B endet nach M59.**
>
> 1. **Der Abrufweg ist nicht der angenommene.** Der `ServiceConnectString` beider Ablagen endet auf
>    `/WebApplication/FileStoreSoapReceiver` — ein **SOAP-Empfänger**, keine Dateiadresse. Alle
>    sechzehn geprüften `GET`-Formen liefern an beiden Knoten dieselben 185 Byte mit leerer
>    `FileList`; die Pfadform antwortet mit `404`. **Die Annahme des Auftrags, eine Datei werde mit
>    `curl <adresse>` geholt, trägt nicht** (M59 (1)).
> 1a. **Auch der freigegebene SOAP-Abruf holt nichts.** Zehn `RETRIEVE`-Anfragen in der am
>    17.08.2026 freigegebenen Form, über sechs Familien und beide Ablagen, liefern **dieselben
>    185 Byte wie ein blankes `GET`** — leere `FileList`, kein Anhang, kein `Fault`, und die
>    gesendete Kennung kommt nicht zurück. Der Empfänger verarbeitet den Rumpf nicht. **Warum, ist
>    offen** (M59 (2)).
> 2. **Die beiden Ablagen sind gleich.** Gleiche Kopfzeilen, gleiche Statuscodes, byteidentische
>    Basisantwort. Der Proxy braucht **einen** Codepfad, nicht elf (M59 Befund 1).
> 3. **Die Ablage hängt am Zeitraum, nicht am Mandanten.** Je Zeitraum sind **zwei** Filestores
>    gleichzeitig aktiv; in Fenster A und B sind es `09`/`10`, in Fenster C `07`/`08`. **Jeder**
>    Mandant verweist auf beide (M53). Es gibt **elf** Ablagen, nicht die eine des Beispiels (M52).
> 4. **Jede Nachricht trägt Artefakte, und jede trägt ein Protokoll.** Minimum 3, Maximum 15;
>    `nachrichten_mit_protokoll = nachrichten` bei **jedem** Mandanten in beiden Fenstern. Der
>    deaktivierte Knopf hat die gemessene Häufigkeit **null** (M55). Auch die elf Fehlernachrichten
>    tragen sämtlich ein Protokoll (M64a).
> 5. **Mehr als die Hälfte der Artefakte hat keinen lesbaren Schrittnamen** — 55,98 % in Fenster A,
>    51,72 % in Fenster B, bei `ohne_schrittzeile = 0` (M57).
> 6. **Der Zugriffsweg kostet nichts.** Artefaktliste 0,748 ms, Auflösung einer Kennung 0,336 ms,
>    Existenznachweis 0,512 ms — zusammen unter 1,6 ms und Faktor 67 unter der 50-ms-Erwartung
>    (M58).
> 7. **Eine Vorabgröße gibt es nur für 57,2 % des Monatsfensters**, und der größte Wert liegt bei
>    118 MiB (M56).
>
> **Neun Stellen**, an denen ein Befund in keine vorformulierte Zeile passte, sind unten einzeln
> benannt und gezählt. Sie sind nicht in die nächstliegende Zeile gedrückt worden.
>
> ### Nachtrag vom 17.08.2026 — Teil B ist abgeschlossen
>
> Nach M71 sind **M60, M61, M63, M64, M65, M66 (2), M67 und M68** gefahren worden, zusammen
> **1.180 Abrufe in rund 183 Sekunden**, sequenziell, alle mit dem unveränderten Alt-Client.
> Die tragenden Befunde:
>
> - **Die Ablagen sind keine Spiegel.** 20 von 20 Kreuzabrufen scheitern, dieselben 20 Verweise
>   gelingen am eigenen Knoten (M68). Die Auflösung über `Service` ist tragend.
> - **`Error (Skipped)` ist die Antwort auf „Datei nicht vorhanden"** — im Rumpf, nicht im
>   HTTP-Status. Damit ist **V5** beantwortet (M68, M66 (2)).
> - **Am jüngeren Ende laufen die Kopien auseinander:** 153 von 153 Verweisen des `2026-07-08`
>   liefern nichts (M66 (2)).
> - **Die Rotationsgrenze ist taggenau der `2025-07-23`.** 63,2 % des Bestands liegen davor und
>   sind heute nicht abrufbar, weil `FILESTOREPROD07`/`08` aus sind.
> - **`FileReader.FileProperty.Size` zählt Bytes**, exakt in 7 von 7 Fällen (M60).
> - **Zwei Protokollfamilien tragen keine Marken** — `HTTPSender` in 30 von 30, `FTPSender` in 28
>   von 30 (M63). Wo Marken stehen, sind sie ausnahmslos vollständig gepaart, ohne Abweichung.
> - **Der Beschnitt hält weniger zurück als gedacht:** Pfade und Dienstkennungen stehen
>   überwiegend **innerhalb** der Marken (M65), und der Innenbereich ist mit **90,4 %** der Bytes
>   der überwiegende Teil (M67).
> - **18,2 % der Nutzdateien sind binär** (M61), und die Anzeige ist **nicht** kodierungsfrei.
> - **Fehlerprotokolle sind vollständig gepaart** — 33 von 33 (M64). Der befürchtete Fall tritt
>   nicht ein.

---

## Nummernvergabe

Vor der Vergabe über `docs\` und das Wurzelverzeichnis geprüft.

| Bereich | Stand |
|---|---|
| Höchste projektweit vergebene M-Nummer außerhalb dieser Runde | **M51** (`messungen-schritt7.md`) |
| Höchste ergänzende Nummer außerhalb dieser Runde | **E7** (`messungen-schritt7.md`) |
| **M52 bis M68** | Kommen außerhalb dieser Datei **in keiner Datei** von `docs\` oder des Wurzelverzeichnisses vor — mit der einen Ausnahme des Eintrags zu dieser Datei in `docs/README.md`, der sie referenziert statt sie zu vergeben |
| **M62** | Im Auftrag ausdrücklich **durch M65 ersetzt**. Bleibt vergeben und wird nicht umgewidmet |
| **M64a** | Teilnummer von M64 (SQL-Teil), im Auftrag so benannt |
| **E8** | War durch die ersetzte Fassung vom 14.08.2026 belegt. Da diese Fassung sie ersetzt, ist die Nummer wieder frei — sie wird in dieser Runde **nur** vergeben, wenn eine ergänzende Messung tatsächlich entsteht, und dann mit Begründung |
| **M69** | **Nicht vergeben.** War für eine Messung zur Kennungsform vorgesehen und ist durch Q1 gegenstandslos geworden. Die Nummer bleibt frei und wird **nicht umgewidmet** — eine Nummer mit gewechselter Bedeutung ist genau der Fall, den dieses Projekt bei M62 vermieden hat. Siehe „Offene Punkte" 12 |
| **M70** | Neu vergeben am 17.08.2026 für den Mitschnitt des SAAJ-Aufrufs. Vor der Vergabe geprüft: `M70`, `M71` und `M72` kommen in `docs\` und im Wurzelverzeichnis **in keiner Datei** vor |
| **M71** | Neu vergeben am 17.08.2026 für den Abruf mit dem echten Client. Bei derselben Prüfung als frei festgestellt |
| **M66 (2)** | **Keine neue Nummer.** Die nachgeholte jüngste Zeitscheibe läuft als **Teilmessung von M66**, weil sie dieselbe Frage mit derselben Deutung stellt. Sie ist **nicht** in M66 eingearbeitet: M66 ist gefahren, und eine nachträglich veränderte Scheibe darin würde die Datei über ihre eigene Geschichte täuschen |
| Rotationsgrenze | **Keine Nummer.** Kein eigener Befund, sondern die Präzisierung von M53 Befund 1 und M66 Befund 4 aus denselben Statements |

Prüfbefehl:
`grep -rnoE "\bM(5[2-9]|6[0-8])\b" --include="*.md" . --exclude="messungen-schritt8*.md"` →
ausschließlich Treffer in `docs/README.md:141`.

---

## Rahmen

### Teil A — SQL

| | |
|---|---|
| Ziel | Testkopie, MariaDB — Versionsstring siehe unten |
| Nachweis Testkopie | `SELECT @@global.read_only` als **erstes Statement jeder Sitzung**, in der Schlusssitzung erneut |
| Benutzer | `monitor_read@%`, ausschließlich `SELECT` |
| Sitzungen | sequenziell, jede eine eigene `mysql`-Ausführung mit einer Skriptdatei, Serverzeit je Sitzung notiert |
| Client | siehe unten |
| Grenze | `SET SESSION max_statement_time = 60` in jeder Sitzung |
| Laufzeit | `SET profiling = 1` / `SHOW PROFILES` |
| **S1** | ausschließlich `SELECT`, `SET`, `EXPLAIN`, `SHOW PROFILES` |
| **L4** | jeder Einstieg in `MessageProperty` über `Message.MessageID` aus einem Zeitfenster |
| **L9** | jedes Statement gegen `Message`/`MessageProperty` trägt ein Fenster. Ausnahme `Service` (M52) |
| **L10** | jeder Befundsatz trägt *Gemessen war* / *Behauptet wird* |
| **G1** | keine `MessageID`, keine UUID, kein `ServiceConnectString`, kein `ServiceName`, kein Dateiname im Klartext |

| Angabe | Wert |
|---|---|
| Versionsstring | `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| Serverzeit zu Beginn | `2026-08-17 11:51:22` |
| **`@@global.read_only` Beginn** | **`1`** — erste Abfrage der ersten Sitzung |
| `@@global.read_only` Ende | *(wird in Sitzung 6 erhoben)* |
| `@@div_precision_increment` | **4** — deshalb steht in keiner Abfrage dieser Runde ein `AVG` über einen Wahrheitswert; Anteile sind aus den Zählwerten gerechnet (`messungen-schritt7.md` M46) |
| `@@session.sql_mode` | `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION` |
| Client und Version | `mysql.exe` **Ver 8.0.46 for Win64 on x86_64** aus MySQL Workbench 8.0 CE, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t` |
| Passwortübergabe | über `MYSQL_PWD` aus der Umgebungsvariablen `OVERLORD_DB_READ_PASSWORD`; **kein Passwort auf der Befehlszeile, keines in einer Skriptdatei** |
| Sitzungszahl | 6 geplant |
| `profiling_history_size` | Vorgabe der Instanz ist **15**, nicht 100. In jeder Sitzung auf **100** gesetzt, sonst gehen die Laufzeiten der frühen Statements verloren |

### Teil B — Filestore

| | |
|---|---|
| Ziel | **`FILESTOREPROD09` und `FILESTOREPROD10`**, beides Produktionskopien, ausschließlich lesend. **Jede Messung wird je Ablage getrennt ausgewiesen, niemals zusammengefasst** |
| Werkzeug | `curl`, kein Browser |
| Kopfzeilen | `curl -sS -o /dev/null -D - <adresse>` |
| Inhalt | ausschließlich über das Auswertungsskript, das zählt und klassifiziert. **Kein Dateiinhalt erreicht Bildschirm, Kontext oder diese Datei** |
| Holregel | **immer bei der Ablage, die der Verweis nennt.** Kein Rückfall auf die andere. Einzige Ausnahme: M68 |
| Arbeitsverzeichnis | *(wird bei Beginn von Teil B genannt; am Ende gelöscht)* |
| Auswertungsskript | `scripts/messung-schritt8/auswertung.ps1`, geprüft an selbst erfundenen Testdaten unter `scripts/messung-schritt8/testdaten/` |

### Die drei Zeitfenster

| Fenster | Grenzen | n | Herkunft |
|---|---|---|---|
| **A** | `MessageLastUpdate >= '2025-12-29 00:00:00'` und `< '2025-12-30 00:00:00'` | 6.249 | Auftrag §1; deckt sich mit `messungen-schritt5.md` Z. 69–70 |
| **B** | `MessageLastUpdate >= '2025-11-30 00:00:00'` und `< '2025-12-30 00:00:00'` | 214.330 | **wörtlich** aus `messungen-schritt5.md` Z. 73 (Grenzen) und M17 (1), Z. 944 (n) |
| **C** | `MessageLastUpdate >= '2024-10-01 00:00:00'` und `< '2024-10-02 00:00:00'` | offen | Auftrag §1, neu |

*Belegstelle Fenster B, wörtlich:* „**Fenster B — der dichte Monat.** `>= '2025-11-30 00:00:00'` und
`< '2025-12-30 00:00:00'`" (`messungen-schritt5.md`, Zeile 73).

### Der kleine Mandant für Regel L15

*(wird aus M53 Fenster A hergeleitet: der Mandant mit den wenigsten Nachrichten, der überhaupt
Artefaktverweise trägt; nicht `SYSTEM`, nicht `WOC` — beide sind technisch)*

---

# Teil A — SQL gegen die Testkopie

## M52 — `Service`: die Werte, nicht nur die Spalten

**Frage.** Ist die Kennung aus `Message.Payload.GUID` eine `Service.ServiceID`?

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Lesbare Codes der Form `FILESTOREPROD<nn>` | Auflösung ist ein **Primärschlüsselzugriff**. Baubar wie geplant |
| UUIDs | Der dokumentierte Auflösungsweg ist unbelegt; es braucht zuerst eine Messung, welche Spalte trägt. **Abbruchbedingung** |
| `cs_http`/`cs_https` durchgängig | Der Proxy spricht HTTP. Der geplante Zuschnitt trägt |
| Weder noch | `ServiceConnectString` ist kein URL. Nicht raten (Q4) — M59 klärt es am lebenden Objekt |

**Ausgeführtes Statement.** `scripts/messung-schritt8/sitzung1.sql`, unverändert wie im Auftrag.

**Ergebnis.** **20 Zeilen.** Für **alle 20** gilt `id_code_form = 1` und `id_uuid_form = 0` — es gibt
keine einzige UUID in `Service.ServiceID`. Die elf Filestore-Zeilen sind über **jede** ausgewiesene
Spalte identisch, einschließlich `ServiceLastUpdate`; sie sind deshalb zusammengefasst, ohne dass
etwas verlorengeht.

| `ServiceID` | Anzahl | `id_laenge` | `ServiceTypeID` | `ServiceStatus` | `ServiceTimeout` | `ohne_default` | `cs_laenge` | `cs_http` | `cs_https` | `cs_schraegstriche` | `cs_mit_fragezeichen` | `ServiceLastUpdate` |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `FILESTOREPROD00` … `FILESTOREPROD10` | **11** | 15 | 1 | `HEARTBEAT` | 0 | **1** | **64** | **1** | 0 | **4** | 0 | `2012-06-07 18:23:42` |
| `MPSERVICEPROD00` | 1 | 15 | 2 | `ERROR_TIMEOUT` | 600 | 0 | 100 | 0 | 0 | 19 | 0 | `2025-12-30 04:10:18` |
| `MPSERVICEPROD01` | 1 | 15 | 2 | `ERROR_TIMEOUT` | 600 | 0 | 100 | 0 | 0 | 19 | 0 | `2026-07-13 15:01:44` |
| `MPSERVICEPROD02` | 1 | 15 | 2 | `SHUTDOWN` | 600 | 0 | 129 | 0 | 0 | 21 | 0 | `2025-09-19 12:37:06` |
| `MPSERVICEPROD03` | 1 | 15 | 2 | `ERROR_TIMEOUT` | 600 | 0 | 129 | 0 | 0 | 21 | 0 | `2025-12-30 04:09:43` |
| `DATAWAREHOUSE00` | 1 | 15 | 3 | `HEARTBEAT` | 0 | 0 | **0** | 0 | 0 | 0 | 0 | `2013-02-06 14:00:26` |
| `COMSERVICEPROD00` | 1 | 16 | 4 | `SHUTDOWN` | 600 | 0 | 137 | 0 | 0 | 0 | 0 | `2025-09-19 12:36:08` |
| `COMSERVICEPROD01` | 1 | 16 | 4 | `ERROR_TIMEOUT` | 600 | 0 | 137 | 0 | 0 | 0 | 0 | `2025-12-30 04:10:02` |
| `HTTPSERVICEPROD00` | 1 | 17 | 4 | `ERROR_TIMEOUT` | 600 | 0 | 169 | 0 | 0 | 27 | 0 | `2025-12-30 04:09:56` |
| `HTTPSERVICEPROD01` | 1 | 17 | 4 | `HEARTBEAT` | 0 | 0 | 217 | 0 | 0 | 3 | 0 | `2020-04-14 15:09:48` |

**Befund 1 — die Kennung ist ein lesbarer Code.**
*Gemessen war:* 20 von 20 `ServiceID` erfüllen `^[A-Z0-9_]+$`, keine erfüllt die UUID-Form; elf
davon tragen die Gestalt `FILESTOREPROD<nn>` mit `nn` von `00` bis `10`.
*Behauptet wird:* Die Auflösung einer Kennung aus einem Artefaktverweis ist ein
**Primärschlüsselzugriff auf `Service`**. Die Abbruchbedingung „UUIDs statt lesbarer Codes" ist
**nicht** eingetreten.

**Befund 2 — es sind elf Filestores, nicht einer.**
*Gemessen war:* `ServiceTypeID = 1` trägt elf Zeilen, `FILESTOREPROD00` bis `FILESTOREPROD10`.
*Behauptet wird:* Der in `datenmodell.md` §3, `PROJEKTBESCHREIBUNG.md` §7 und
`IMPLEMENTIERUNGSPLAN_MVP.md` als Beispiel geführte `FILESTOREPROD09` ist **eine** von elf
Ablagen. Ob zur Laufzeit alle elf erreichbar sein müssen, sagt M52 nicht — das sagt M53.

**Befund 3 — `cs_http` ist nicht durchgängig, und das passt in keine der vier Zeilen.**
*Gemessen war:* `cs_http = 1` bei **11 von 20** Zeilen — genau den Filestores. Die übrigen neun
haben weder `cs_http` noch `cs_https`; `DATAWAREHOUSE00` hat sogar `cs_laenge = 0`.
*Behauptet wird:* Für die **Filestore-Familie** trägt die Zeile „`cs_http` durchgängig → der Proxy
spricht HTTP". Über die anderen Diensttypen trägt sie **nicht**, und für sie gilt die vierte Zeile
(„kein URL"). Da Schritt 8 ausschließlich Filestores anspricht, ist der geplante Zuschnitt davon
nicht berührt — aber die Deutung „durchgängig" darf nicht als Aussage über `Service` insgesamt
gelesen werden. **Erste Stelle, an der der Befund in keine vorformulierte Zeile passt.**

**Befund 4 — die Gestalt der Verbindungszeichenkette.**
*Gemessen war:* Alle elf Filestore-Zeichenketten sind **64 Zeichen** lang, beginnen mit `http://`,
enthalten **vier** Schrägstriche und **kein** Fragezeichen.
*Behauptet wird:* Sie sind gleich gebaut. Ob hinter der Adresse eine Datei liegt, sagt die Gestalt
nicht — das ist M59. *(Der Wert selbst ist nach G1 gesperrt und steht nirgends in dieser Datei.)*

**Laufzeit.** **1,348 ms**, einmalig.

---

## M53 — Welche Ablage gehört zu welchem Mandanten?

**Frage.** Müssen über `FILESTOREPROD09` und `FILESTOREPROD10` hinaus weitere Ablagen hochgefahren
werden (V7)? Dreimal fahren: Fenster A, B und C.

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Alle Mandanten verweisen auf eine der beiden erreichbaren Kennungen | **Kein weiterer Filestore nötig.** Die Runde ist vollständig fahrbar, und die Abnahme ebenso |
| Verschiedene Mandanten verweisen auf verschiedene Kennungen | Genau die vermutete Lage. Die Liste sagt, **welche** Ablagen hochzufahren sind — und der kleinste betroffene Mandant genügt für die Kontrolle nach L15 |
| Dieselbe Kennung, aber `nicht_aufloesbar > 0` schon in Fenster A | Der Auflösungsweg ist unvollständig. **Anhalten** |
| `= 0` in A und B, **`> 0` in C** | Die Ablagen **rotieren**. Alte Nachrichten sind nicht abrufbar; die Oberfläche braucht dafür einen benannten Zustand, und die Abnahme darf nicht an einer alten Nachricht hängen |

**Ausgeführte Statements.** `scripts/messung-schritt8/sitzung2.sql` (Fenster A und C) und
`scripts/messung-schritt8/sitzung3.sql` (Fenster B), unverändert wie im Auftrag; einzige Änderung
je Lauf ist das eingesetzte Fensterpaar.

**Ergebnis Fenster A** — `2025-12-29` bis `2025-12-30`

| `MandantID` | Kennung | `verweise` | `nachrichten` | `namen` | `nicht_aufloesbar` |
|---|---|---|---|---|---|
| `IBIS` | `FILESTOREPROD10` | 1.405 | 233 | 11 | **0** |
| `IBIS` | `FILESTOREPROD09` | 1.012 | 213 | 9 | **0** |
| `IBISGUS` | `FILESTOREPROD10` | 259 | 81 | 5 | **0** |
| `IBISGUS` | `FILESTOREPROD09` | 160 | 80 | 2 | **0** |
| `NEXANS` | `FILESTOREPROD09` | 34.641 | 5.043 | 23 | **0** |
| `NEXANS` | `FILESTOREPROD10` | 202 | 202 | 1 | **0** |
| `SUTTONS` | `FILESTOREPROD10` | 5.441 | 685 | 8 | **0** |
| `VOTG` | `FILESTOREPROD10` | 609 | 206 | 8 | **0** |
| `VOTG` | `FILESTOREPROD09` | 591 | 132 | 9 | **0** |
| `WOC` | `FILESTOREPROD09` | 9 | 1 | 7 | **0** |

**Ergebnis Fenster B** — `2025-11-30` bis `2025-12-30`

| `MandantID` | Kennung | `verweise` | `nachrichten` | `namen` | `nicht_aufloesbar` |
|---|---|---|---|---|---|
| `IBIS` | `FILESTOREPROD10` | 25.983 | 4.331 | 11 | **0** |
| `IBIS` | `FILESTOREPROD09` | 19.522 | 3.960 | 9 | **0** |
| `IBISGUS` | `FILESTOREPROD10` | 5.420 | 1.722 | 5 | **0** |
| `IBISGUS` | `FILESTOREPROD09` | 3.400 | 1.700 | 2 | **0** |
| `NEXANS` | `FILESTOREPROD09` | 1.247.763 | 180.251 | 33 | **0** |
| `NEXANS` | `FILESTOREPROD10` | 6.513 | 6.355 | 5 | **0** |
| `SUTTONS` | `FILESTOREPROD10` | 169.651 | 21.516 | 8 | **0** |
| `SUTTONS` | `FILESTOREPROD09` | 140 | 70 | 2 | **0** |
| `SYSTEM` | `FILESTOREPROD09` | 15 | 5 | 3 | **0** |
| `VOTG` | `FILESTOREPROD10` | 18.116 | 6.104 | 9 | **0** |
| `VOTG` | `FILESTOREPROD09` | 17.116 | 3.888 | 10 | **0** |
| `WOC` | `FILESTOREPROD09` | 1.062 | 118 | 7 | **0** |
| `ZAST` | `FILESTOREPROD10` | 1.941 | 283 | 7 | **0** |

**Ergebnis Fenster C** — `2024-10-01` bis `2024-10-02`

| `MandantID` | Kennung | `verweise` | `nachrichten` | `namen` | `nicht_aufloesbar` |
|---|---|---|---|---|---|
| `IBIS` | `FILESTOREPROD08` | 1.412 | 237 | 9 | **0** |
| `IBIS` | `FILESTOREPROD07` | 1.099 | 217 | 9 | **0** |
| `IBISGUS` | `FILESTOREPROD08` | 268 | 84 | 3 | **0** |
| `IBISGUS` | `FILESTOREPROD07` | 168 | 84 | 2 | **0** |
| `NEXANS` | `FILESTOREPROD07` | 28.342 | 4.252 | 21 | **0** |
| `NEXANS` | `FILESTOREPROD08` | 214 | 206 | 3 | **0** |
| `VOTG` | `FILESTOREPROD08` | 1.596 | 322 | 8 | **0** |
| `VOTG` | `FILESTOREPROD07` | 560 | 149 | 7 | **0** |
| `WOC` | `FILESTOREPROD07` | 135 | 15 | 7 | **0** |
| `ZAST` | `FILESTOREPROD08` | 295 | 43 | 7 | **0** |

**Befund 1 — die Ablage hängt nicht am Mandanten, sondern am Zeitraum.**
*Gemessen war:* In Fenster A und B kommen ausschließlich `FILESTOREPROD09` und `FILESTOREPROD10`
vor, in Fenster C ausschließlich `FILESTOREPROD07` und `FILESTOREPROD08`. **Jeder** Mandant mit mehr
als einer Handvoll Nachrichten verweist in jedem Fenster auf **beide** dort vorkommenden Kennungen.
*Behauptet wird:* Die Zuordnung Ablage↔Mandant, nach der V7 fragt, existiert nicht. Zu jedem
Zeitpunkt sind **zwei** Ablagen gleichzeitig aktiv, und die Verteilung darauf ist nicht
mandantengebunden. Welcher Mechanismus die Verteilung steuert, sagt M53 **nicht**.

**Befund 2 — für Fenster A und B genügen die beiden erreichbaren Ablagen.**
*Gemessen war:* In A und B tritt keine andere Kennung als `FILESTOREPROD09` und `FILESTOREPROD10`
auf, bei keinem der sieben Mandanten.
*Behauptet wird:* Über die beiden hochgefahrenen Ablagen hinaus ist für **Fenster A und B** keine
weitere nötig. Teil B ist damit fahrbar. Für **Fenster C gilt das nicht** — dort läge alles auf
`FILESTOREPROD07`/`08`, und die sind nicht erreichbar.

**Befund 3 — `nicht_aufloesbar` ist überall null, auch in C, und das passt in keine der vier
vorformulierten Zeilen.**
*Gemessen war:* `nicht_aufloesbar = 0` in **allen drei** Fenstern, einschließlich C.
*Behauptet wird:* Die vorregistrierte Zeile „`= 0` in A und B, `> 0` in C → die Ablagen rotieren"
trifft in ihrer **Folgerung** zu, in ihrer **Bedingung** aber nicht: Die Rotation ist da, sie zeigt
sich nur nicht in `nicht_aufloesbar`. Der Grund steht in M52 — alle elf Kennungen haben eine Zeile
in `Service`, also löst auch eine Kennung auf, deren Knoten nicht läuft. **`nicht_aufloesbar` misst
die Auflösbarkeit der Kennung, nicht die Erreichbarkeit der Ablage.** Die dritte Zeile
(„`nicht_aufloesbar > 0` schon in Fenster A → Anhalten") ist damit ebenfalls nicht ausgelöst.
**Zweite Stelle, an der der Befund in keine vorformulierte Zeile passt.**

**Befund 4 — die Mandantenmenge ist je Fenster verschieden.**
*Gemessen war:* `SUTTONS` kommt in A und B vor, in C nicht. `ZAST` kommt in B und C vor, in A nicht.
`SYSTEM` kommt nur in B vor (5 Nachrichten).
*Behauptet wird:* Eine Stichprobe aus einem Fenster deckt nicht alle Mandanten ab. Für M66 und M60
ist die Mandantenauswahl je Zeitscheibe zu prüfen und nicht aus einem Fenster fortzuschreiben.

**Laufzeit.** Fenster A **876,5 ms**, Fenster C **745,6 ms**, je einmalig. Fenster B als
kostenrelevante Messung mit Aufwärmlauf und zwei Wiederholungen: 32,071 s (Aufwärmlauf), 31,491 s,
**31,334 s (beste von zwei nach Aufwärmlauf)**. Die Grenze von 60 s ist nicht erreicht.

**Der kleine Mandant für Regel L15** ist damit bestimmt: **`IBISGUS`** — der Mandant mit den
wenigsten Nachrichten in Fenster A, der Artefaktverweise trägt (81 bzw. 80 je Ablage). `WOC` (eine
Nachricht) und `SYSTEM` sind nach Auftrag als technisch ausgeschlossen.

---

## M54 — Die vollständige Artefakt-Inventur, gestaltbasiert

**Frage.** Darf die Artefaktmenge über die zwei Namensendungen definiert werden, oder muss sie
gestaltbasiert definiert werden? Fenster A und B, je Teil (a) über das Namensmuster und (b) über die
Gestalt.

**Vorregistrierte Deutung.** (b) leer → die Artefaktmenge darf über die zwei Namensendungen
definiert werden. (b) nicht leer → die Definition muss gestaltbasiert sein, sonst verschwinden
Artefakte **still**. In (a) `mit_pipe < zeilen` → der Code muss je Wert prüfen, nicht je Name.

**Ausgeführte Statements.** `scripts/messung-schritt8/sitzung4.sql`, unverändert wie im Auftrag,
dazu `scripts/messung-schritt8/sitzung4b-gegenprobe.sql` (Begründung unten).

**Ergebnis (a) — Fenster A.** **32 Namen**, zusammen **44.329 Zeilen**. Für **jeden** Namen gilt
`kuerzester = laengster = 52` und `mit_pipe = zeilen`.

| `MessagePropertyName` | `zeilen` | `nachrichten` | `verschiedene` |
|---|---|---|---|
| `Converter.Payload.GUID` | 7.862 | 6.149 | 7.862 |
| `Converter.Log.GUID` | 7.862 | 6.149 | 7.862 |
| `Message.Payload.GUID` | 6.249 | 6.249 | 6.249 |
| `FTPSender.Payload.GUID` | 4.490 | 4.316 | 4.490 |
| `FTPSender.Log.GUID` | 4.490 | 4.316 | 4.490 |
| `FileReader.Payload.GUID` | 4.352 | 4.351 | 4.352 |
| `FileReader.Log.GUID` | 4.352 | 4.351 | 4.352 |
| `DataWarehouse.Payload.GUID` | 950 | 950 | 950 |
| `HTTPSender.Payload.GUID` / `.Log.GUID` | je 696 | je 696 | je 696 |
| `SAPReader.Payload.GUID` / `.Log.GUID` | je 443 | je 443 | je 443 |
| `AS2Reader.Payload.GUID` / `.Log.GUID` | je 244 | je 244 | je 244 |
| `OFTPReader.Payload.GUID` / `.Log.GUID` | je 218 | je 218 | je 218 |
| `FTPReader.Payload.GUID` / `.Log.GUID` | je 111 | je 111 | je 111 |
| `OFTP2Reader.Payload.GUID` / `.Log.GUID` | je 46 | je 46 | je 46 |
| `AS2Sender.Payload.GUID` / `.Log.GUID` | je 39 | je 39 | je 39 |
| `MailReader.Payload.GUID` / `.Log.GUID` | je 32 | je 32 | je 32 |
| `OFTPSender.Payload.GUID` / `.Log.GUID` | je 20 | je 20 | je 20 |
| `OFTP2Sender.Payload.GUID` / `.Log.GUID` | je 6 | je 6 | je 6 |
| `SSHReader.Payload.GUID` / `.Log.GUID` | je 5 | je 5 | je 5 |
| `HTTPReader.Payload.GUID` / `.Log.GUID` | je 1 | je 1 | je 1 |

**Ergebnis (a) — Fenster B.** **34 Namen**. Wieder durchgängig `kuerzester = laengster = 52` und
`mit_pipe = zeilen`. Neu gegenüber A ist ausschließlich das Paar `DBReader.*` mit je 4 Zeilen.

| `MessagePropertyName` | `zeilen` | `nachrichten` | `verschiedene` |
|---|---|---|---|
| `Converter.Payload.GUID` | 298.436 | 211.370 | 298.436 |
| `Converter.Log.GUID` | 298.436 | 211.370 | 298.436 |
| `Message.Payload.GUID` | **214.330** | **214.330** | 214.330 |
| `FileReader.Payload.GUID` / `.Log.GUID` | je 122.609 | je 122.604 | je 122.609 |
| `FTPSender.Payload.GUID` / `.Log.GUID` | je 110.036 | je 106.796 | je 110.036 |
| `SAPReader.Payload.GUID` / `.Log.GUID` | je 45.333 | je 45.333 | je 45.333 |
| `DataWarehouse.Payload.GUID` | 28.616 | 28.616 | 28.616 |
| `HTTPSender.Payload.GUID` / `.Log.GUID` | je 21.089 | je 21.088 | je 21.089 |
| `AS2Reader.Payload.GUID` / `.Log.GUID` | je 15.096 | je 15.096 | je 15.096 |
| `OFTPReader.Payload.GUID` / `.Log.GUID` | je 11.236 | je 11.236 | je 11.236 |
| `OFTP2Sender.Payload.GUID` | 4.577 | 4.573 | **4.573** |
| `OFTP2Sender.Log.GUID` | 4.577 | 4.573 | 4.577 |
| `OFTP2Reader.Payload.GUID` / `.Log.GUID` | je 2.125 | je 2.125 | je 2.125 |
| `OFTPSender.Payload.GUID` / `.Log.GUID` | je 2.057 | je 2.055 | je 2.057 |
| `FTPReader.Payload.GUID` / `.Log.GUID` | je 2.034 | je 2.034 | je 2.034 |
| `MailReader.Payload.GUID` / `.Log.GUID` | je 900 | je 900 | je 900 |
| `AS2Sender.Payload.GUID` / `.Log.GUID` | je 822 | je 822 | je 822 |
| `SSHReader.Payload.GUID` / `.Log.GUID` | je 360 | je 360 | je 360 |
| `HTTPReader.Payload.GUID` / `.Log.GUID` | je 134 | je 134 | je 134 |
| `DBReader.Payload.GUID` / `.Log.GUID` | je 4 | je 4 | je 4 |

**Ergebnis (b) — Fenster A und B: keine Zeile.**

**Gegenprobe, und warum sie nötig war.** Eine leere Ausgabe ist kein Beleg für eine leere
Ergebnismenge — sie kann auch von einem defekten Regulärausdruck kommen, und dann sähe „keine
Artefakte außerhalb des Namensmusters" genauso aus wie „der Filter trifft nichts". Deshalb dreimal
gezählt statt einmal geschaut:

| Gegenprobe | Ergebnis |
|---|---|
| Gestaltfilter **mit** Namensausschluss, Fenster A | **0** |
| Gestaltfilter **mit** Namensausschluss, Fenster B | **0** |
| Gestaltfilter **ohne** Namensausschluss, Fenster A *(Positivkontrolle)* | **44.329** |

Die 44.329 der Positivkontrolle sind Zeichen für Zeichen die Summe der 32 Namen aus (a) Fenster A.

**Befund 1 — die Artefaktmenge darf über die zwei Namensendungen definiert werden.**
*Gemessen war:* Der gestaltbasierte Filter findet in Fenster A und B **null** Werte, die nicht schon
über `%.Payload.GUID` oder `%.Log.GUID` gefunden werden; derselbe Filter ohne Namensausschluss
findet in Fenster A genau die 44.329 Zeilen der Namensvariante.
*Behauptet wird:* In den gemessenen Fenstern deckt sich die Namensdefinition mit der
Gestaltdefinition vollständig. Artefakte verschwinden dadurch **nicht** still. Über Fenster C und
den übrigen Bestand sagt das nichts — dort ist (b) nicht gefahren.

**Befund 2 — `mit_pipe = zeilen` und Länge konstant 52.**
*Gemessen war:* In allen 32 bzw. 34 Namen ist `mit_pipe = zeilen` und `kuerzester = laengster = 52`.
*Behauptet wird:* Die Zerlegung `SUBSTRING_INDEX(wert, '|', 1)` trifft in den gemessenen Fenstern
jeden Wert. Die vorregistrierte Bedingung `mit_pipe < zeilen` ist **nicht** eingetreten; der Code
muss deshalb nicht je Wert prüfen — er sollte es trotzdem, denn 52 ist eine gemessene Regelmäßigkeit
und keine zugesicherte.

**Befund 3 — zwei Familien tragen kein Protokoll, und sie sind keine Kleinigkeit.**
*Gemessen war:* `Message.Payload.GUID` und `DataWarehouse.Payload.GUID` haben **kein** Gegenstück
auf `.Log.GUID`. In Fenster B sind das 214.330 bzw. 28.616 Verweise. Alle übrigen 16 Familien treten
paarweise auf.
*Behauptet wird:* Es gibt **18** Nutzdaten-Familien und **16** Protokollfamilien. Ein Artefakt ohne
Protokoll ist der Normalfall und kein Defekt — die Oberfläche darf aus dem Fehlen eines Protokolls
nicht auf einen Fehler schließen.

**Befund 4 — `verschiedene < zeilen` in genau einem Fall.**
*Gemessen war:* `OFTP2Sender.Payload.GUID` in Fenster B: 4.577 Zeilen, 4.573 Nachrichten, aber nur
**4.573 verschiedene Werte**. In allen 33 übrigen Namen ist `verschiedene = zeilen`.
*Behauptet wird:* Vier Zeilen tragen einen Verweis, der schon an anderer Stelle steht. Ein Verweis
ist damit **nicht** eindeutig, auch wenn er es in 99,99 % der Fälle ist. Wer die Artefaktliste über
den Wert schlüsselt statt über die Zeile, verliert in diesem Fall einen Eintrag. Der Fall ist neu
und in keiner vorformulierten Zeile vorgesehen. **Dritte Stelle, an der der Befund in keine
vorformulierte Zeile passt.**

**Befund 5 — M63 wird nicht in allen Familien 30 Dateien finden.**
*Gemessen war:* Von den 16 Protokollfamilien liegen in Fenster B **vier** unter 1.000 Zeilen, und
`DBReader.Log.GUID` hat **4**.
*Behauptet wird:* Die Auftragsvorgabe „mindestens 30 je Familie, **sofern vorhanden**" ist bei
`DBReader` nicht erfüllbar. Das ist keine Abweichung, sondern der im Auftrag mitgedachte Fall; der
Stichprobenumfang wird bei M63 je Familie ausgewiesen.

**Laufzeit.**

| Lauf | Zeit | Wiederholungen |
|---|---|---|
| (a) Fenster A | **1,808 s** | einmalig |
| (b) Fenster A | **0,406 s** | einmalig |
| (a) Fenster B | Aufwärmlauf 59,592 s, dann **59,325 s** | beste von zwei nach Aufwärmlauf |
| (b) Fenster B | Aufwärmlauf 13,905 s, dann **13,911 s** | beste von zwei nach Aufwärmlauf |
| Gegenprobe A / Positivkontrolle / B | 0,395 s / 0,357 s / 13,801 s | einmalig |

> ⚠️ **(a) Fenster B liegt bei 98,9 % der Grenze.** 59,325 s von 60 s. Das Statement ist **nicht**
> abgebrochen worden, es hat vollständig geliefert — aber es ist das teuerste der Runde, und der
> Auftrag hat es auf „~7,6 s im M17-Vergleich" veranschlagt. Die Grenze ist **nicht** angehoben
> worden. *Gemessen war:* 59,325 s beste von zwei. *Behauptet wird:* Diese Abfrage ist in dieser
> Form kein Kandidat für einen Endpunkt und war auch nie als solcher gedacht; sie ist eine
> Inventur über ein Monatsfenster. Für die Kostenfrage des Endpunkts gilt **M58**, nicht M54.
>
> Die Wiederholungszahl **zwei** ist eine Setzung dieser Runde: Der Auftrag führt M54 als
> kostenrelevant, nennt aber keine Zahl (anders als M58 mit „beste von fünf"). Gewählt ist dieselbe
> Zahl wie bei M53 Fenster B, damit die beiden teuren Messungen vergleichbar bleiben.

---

## M55 — Wie viele Artefakte trägt eine Nachricht?

**Frage.** Trägt eine flache Liste, oder braucht die Anzeige eine Gruppierung nach Schritt? Fenster A
und B, je Teil (a) Verteilung und (b) je Mandant.

**Vorregistrierte Deutung.** Maximum ≤ 8 → flache Liste trägt. Maximum > 15 → Gruppierung nach
Schritt nötig; der Wunsch „Anzeige als Standardfall" trifft dann auf eine Auswahl, bevor überhaupt
etwas angezeigt wird. Nachrichten mit null Verweisen → der deaktivierte Knopf bekommt eine
bezifferte Häufigkeit. `nachrichten_mit_protokoll` bei einem Mandanten nahe null → die
Protokollansicht ist dort leer, und das muss **vorher** bekannt sein.

**Ausgeführte Statements.** `scripts/messung-schritt8/sitzung5.sql`, unverändert wie im Auftrag.

**Ergebnis (a) — Verteilung.** Vorkommende Werte sind `3,4,5,6,7,8,9,11,13,15`. **`10`, `12` und
`14` kommen nicht vor, `0`, `1` und `2` ebenfalls nicht.**

| Verweise je Nachricht | Nachrichten Fenster A | Nachrichten Fenster B |
|---|---|---|
| **0** | **0** | **0** |
| 3 | 1 | 33 |
| 4 | 252 | 7.707 |
| 5 | 619 | 43.907 |
| 6 | 26 | 763 |
| **7** | **4.116** | **107.057** |
| 8 | 672 | 20.146 |
| 9 | 221 | 16.829 |
| 11 | 286 | 16.612 |
| 13 | 48 | 1.149 |
| **15** | **8** | **127** |
| **Summe** | **6.249** | **214.330** |

Beide Summen decken sich mit der Nachrichtenzahl des jeweiligen Fensters.

**Ergebnis (b) — je Mandant, Fenster A**

| `MandantID` | `nachrichten` | `nutzdaten_verweise` | `protokoll_verweise` | `nachrichten_mit_protokoll` |
|---|---|---|---|---|
| `NEXANS` | 5.043 | 20.044 | 14.799 | **5.043** |
| `SUTTONS` | 685 | 3.399 | 2.042 | **685** |
| `IBIS` | 233 | 1.326 | 1.091 | **233** |
| `VOTG` | 206 | 740 | 460 | **206** |
| `IBISGUS` | 81 | 250 | 169 | **81** |
| `WOC` | 1 | 5 | 4 | **1** |

**Ergebnis (b) — je Mandant, Fenster B**

| `MandantID` | `nachrichten` | `nutzdaten_verweise` | `protokoll_verweise` | `nachrichten_mit_protokoll` |
|---|---|---|---|---|
| `NEXANS` | 180.251 | 720.370 | 533.906 | **180.251** |
| `SUTTONS` | 21.516 | 105.726 | 64.065 | **21.516** |
| `VOTG` | 6.104 | 21.776 | 13.456 | **6.104** |
| `IBIS` | 4.331 | 24.939 | 20.566 | **4.331** |
| `IBISGUS` | 1.722 | 5.271 | 3.549 | **1.722** |
| `ZAST` | 283 | 1.112 | 829 | **283** |
| `WOC` | 118 | 590 | 472 | **118** |
| `SYSTEM` | 5 | 10 | 5 | **5** |

**Befund 1 — der deaktivierte Knopf hat die gemessene Häufigkeit null.**
*Gemessen war:* Keine Nachricht in Fenster A oder B trägt null Artefaktverweise; das Minimum ist
**3**.
*Behauptet wird:* In den beiden gemessenen Fenstern gibt es den Fall „Nachricht ohne Artefakt"
nicht. Der Zustand muss trotzdem gebaut werden — gemessen ist die Häufigkeit in zwei Fenstern, nicht
die Unmöglichkeit.

**Befund 2 — das Maximum ist genau 15, und dafür gibt es keine vorformulierte Zeile.**
*Gemessen war:* Maximum **15** in beiden Fenstern, bei 8 bzw. 127 Nachrichten. Der häufigste Wert
ist **7** (65,9 % in A, 49,9 % in B).
*Behauptet wird:* Die vorregistrierte Deutung kennt „≤ 8 → flache Liste trägt" und „> 15 →
Gruppierung nötig". **15 liegt in der Lücke dazwischen** und löst weder die eine noch die andere
Zeile aus. Was gemessen ist: Die flache Liste müsste im Regelfall 7 Einträge tragen und im
schlechtesten gemessenen Fall 15. Ob 15 noch eine flache Liste ist, ist eine
Gestaltungsentscheidung und wird hier **nicht** getroffen. **Vierte Stelle, an der der Befund in
keine vorformulierte Zeile passt.**

**Befund 3 — jede Nachricht trägt ein Protokoll, bei jedem Mandanten.**
*Gemessen war:* `nachrichten_mit_protokoll = nachrichten` bei **allen** sechs Mandanten in Fenster A
und **allen** acht in Fenster B, ohne eine einzige Ausnahme.
*Behauptet wird:* Die vorregistrierte Sorge „`nachrichten_mit_protokoll` bei einem Mandanten nahe
null" tritt in den gemessenen Fenstern nirgends ein. Die Protokollansicht ist bei keinem Mandanten
strukturell leer.

**Befund 4 — Nutzdaten überwiegen die Protokolle im Verhältnis von rund 4 zu 3.**
*Gemessen war:* Fenster B insgesamt 879.794 Nutzdaten- gegen 636.848 Protokollverweise; das
Verhältnis liegt bei jedem Mandanten zwischen 1,20 (`IBIS`) und 2,00 (`SYSTEM`).
*Behauptet wird:* Die Differenz erklärt sich aus M54 Befund 3 — `Message` und `DataWarehouse` tragen
Nutzdaten ohne Protokoll. Eine Aussage über die Nützlichkeit der einen oder anderen Familie folgt
daraus nicht.

**Laufzeit.** (a) Fenster A **0,433 s**, Fenster B **26,664 s**; (b) Fenster A **0,515 s**,
Fenster B **19,597 s**. Alle einmalig.

---

## M56 — Was die Datenbank vorab über Größe und Dateinamen weiß

**Frage.** Ist eine Vorabgröße verlässlich verfügbar, und was sagt sie? Fenster A und B, je Teil (a)
welche Namen es gibt, (b) die Größenordnung, (c) die Endungen.

**Vorregistrierte Deutung.** Deckt `…FileProperty.Size` nur die FileReader-Nachrichten (M17: 69,6 %
in Fenster A), ist eine Vorabgröße **nicht** verlässlich verfügbar — die Kappung muss **während** des
Datenstroms greifen. `ueber_10_mib > 0` → die Kappung ist der Normalfall und keine Vorsicht.

> ⚠️ **Nicht belegt** ist damit, dass die Zahl **Bytes** zählt. Das entscheidet erst der Abgleich mit
> `Content-Length` in **M60**.

**Ausgeführte Statements.** `scripts/messung-schritt8/sitzung5.sql`. (a) und (b) unverändert wie im
Auftrag; **(c) mit verschärfter Klassifikation** — Begründung unten und unter „Abweichungen".

**Ergebnis (a) — Fenster A** (10 Namen)

| `MessagePropertyName` | `zeilen` | `nachrichten` | `kuerzester` | `laengster` | `nur_ziffern` |
|---|---|---|---|---|---|
| `FileReader.FileProperty.OriginalFilename` | 4.352 | 4.351 | 26 | 71 | 0 |
| `FileReader.FileProperty.Size` | 4.352 | 4.351 | 3 | 8 | **4.352** |
| `Message.DestinationFilename` | 480 | 480 | 3 | 73 | 6 |
| `Converter.DestinationFilename` | 318 | 318 | 15 | 57 | 0 |
| `FTPReader.FileProperty.Size` | 111 | 111 | 3 | 6 | **111** |
| `FTPReader.FileProperty.OriginalFilename` | 111 | 111 | 25 | 39 | 0 |
| `Message.Filename` | 32 | 32 | 47 | 80 | 0 |
| `MailReader.Filename` | 32 | 32 | 47 | 80 | 0 |
| `SSHReader.Filename` | 5 | 5 | 31 | 41 | 0 |
| `Message.DestinationFilenameFTP` | 1 | 1 | 15 | 15 | 0 |

**Ergebnis (a) — Fenster B** (dieselben 10 Namen)

| `MessagePropertyName` | `zeilen` | `nachrichten` | `kuerzester` | `laengster` | `nur_ziffern` |
|---|---|---|---|---|---|
| `FileReader.FileProperty.Size` | 122.609 | 122.604 | 1 | 9 | **122.609** |
| `FileReader.FileProperty.OriginalFilename` | 122.609 | 122.604 | 18 | 71 | 0 |
| `Message.DestinationFilename` | 20.765 | 20.765 | 3 | 73 | 166 |
| `Converter.DestinationFilename` | 6.520 | 6.520 | 15 | 64 | 0 |
| `FTPReader.FileProperty.OriginalFilename` | 2.034 | 2.034 | 17 | 66 | 0 |
| `FTPReader.FileProperty.Size` | 2.034 | 2.034 | 3 | 8 | **2.034** |
| `MailReader.Filename` | 898 | 898 | 47 | 83 | 0 |
| `Message.Filename` | 898 | 898 | 47 | 83 | 0 |
| `SSHReader.Filename` | 360 | 360 | 27 | 127 | 0 |
| `Message.DestinationFilenameFTP` | 30 | 30 | 15 | 15 | 0 |

**Ergebnis (b) — `FileReader.FileProperty.Size`**

| Kennzahl | Fenster A | Fenster B |
|---|---|---|
| `zeilen` | 4.352 | 122.609 |
| `kleinster` | 100 | **0** |
| `mittel` | 27.010,3015 | 43.643,8736 |
| `groesster` | 25.429.968 | **123.981.293** |
| `ueber_64_kib` | 146 | 4.682 |
| `ueber_1_mib` | 6 | 457 |
| **`ueber_10_mib`** | **1** | **81** |

**Ergebnis (c) — Endungen der Originaldateinamen**

| Klasse | Fenster A | Fenster B |
|---|---|---|
| `<ziffernfolge>` | **4.307** | **119.599** |
| `<sonstige>` | 0 | 272 |
| `txt` | 0 | 4 |
| `cod` | 0 | 1 |
| **Summe mit Punkt** | **4.307** | **119.876** |
| Zeilen ohne Punkt (nicht erfasst) | 45 | 2.733 |

**Befund 1 — eine Vorabgröße ist nicht verlässlich verfügbar.**
*Gemessen war:* `FileReader.FileProperty.Size` deckt **4.351 von 6.249** Nachrichten in Fenster A
(**69,63 %**) und **122.604 von 214.330** in Fenster B (**57,20 %**). Mit `FTPReader.FileProperty.Size`
zusammen sind es 71,4 % bzw. 58,2 %. Für alle übrigen Nachrichten gibt es **keine** Größenangabe in
der Datenbank.
*Behauptet wird:* Die vorregistrierte Bedingung ist eingetreten — und in Fenster B **deutlicher** als
der Auftrag erwartet hat (57,2 % statt 69,6 %). Eine Kappung, die sich auf eine Vorabgröße stützt,
greift bei rund vier von zehn Nachrichten nicht. **Die Kappung muss während des Datenstroms greifen.**

**Befund 2 — `ueber_10_mib > 0`, und in Fenster B mit Abstand.**
*Gemessen war:* 81 Artefakte über 10 MiB in Fenster B, 457 über 1 MiB, 4.682 über 64 KiB; der größte
Wert ist **123.981.293** (rund 118 MiB).
*Behauptet wird:* Die Kappung ist der Normalfall und keine Vorsicht. Ein Artefakt von 118 MiB durch
einen Anzeigepfad zu schicken, ist keine Randbetrachtung.

**Befund 3 — es gibt Artefakte der Größe null.**
*Gemessen war:* `kleinster = 0` in Fenster B (in Fenster A ist es 100).
*Behauptet wird:* Die leere Datei kommt vor. Ob die Anzeige „leer" von „nicht abrufbar"
unterscheidet, ist eine Gestaltungsfrage, die diese Runde nicht entscheidet — sie ist aber jetzt
beziffert und nicht mehr hypothetisch.

**Befund 4 — die Originaldateinamen tragen praktisch keine Dateiendung.**
*Gemessen war:* Von den 4.307 Namen mit Punkt in Fenster A fällt **jeder einzelne** in die Klasse
`<ziffernfolge>`; in Fenster B sind es 119.599 von 119.876 (**99,77 %**). Echte Endungen gibt es
genau zwei Sorten, `txt` (4-mal) und `cod` (1-mal).
*Behauptet wird:* Was hinter dem letzten Punkt steht, ist in aller Regel **keine Dateiendung**,
sondern ein Zahlenbestandteil des Namens. Ein Rückschluss vom Dateinamen auf den Dateityp ist damit
nicht möglich. Der `Content-Type` muss aus etwas anderem kommen — §7 setzt ohnehin
`application/octet-stream` fest.

> **Warum (c) verschärft gefahren ist.** Der beauftragte Filter `^[a-z0-9]{1,5}$` lässt reine
> Ziffernfolgen von ein bis fünf Stellen als eigene Gruppe durch — und da 99,8 % der Werte genau
> das sind, hätte die Abfrage die Zahlenfragmente **einzeln und im Klartext** in diese Datei
> geschrieben. Genau davor warnt die Begründung des Filters im Auftrag selbst („darin kann eine
> Belegnummer stecken"). Die gefahrene Fassung verlangt zusätzlich einen **Buchstaben an erster
> Stelle** und zählt alles Übrige als `<ziffernfolge>`. Der Befund wird dadurch nicht schwächer,
> sondern erst mitteilbar. **Wie viele verschiedene Ziffernfolgen die Klasse enthält, ist bewusst
> nicht gemessen** — die Zahl wird für die Deutung nicht gebraucht, und der Auftrag verlangt sie
> nicht.

**Laufzeit.** (a) Fenster A **0,516 s**, Fenster B **16,596 s**; (b) Fenster A **0,088 s**, Fenster B
**3,047 s**; (c) Fenster A **0,172 s**, Fenster B **4,974 s**. Alle einmalig.

---

## M57 — An welchem Schritt hängt welches Artefakt?

**Frage.** Bekommt jedes Artefakt den lesbaren Schrittnamen aus Schritt 5? Fenster A, bei
Auffälligkeit auch B. Der Join auf `SOSAction` läuft über **beide** Spalten von `MessageAction`,
niemals über `Message.SOSID` — M15 belegt das mit 100 % gegen 96,17 %.

**Vorregistrierte Deutung.** `ohne_schrittzeile = 0` und `ohne_schrittnamen` klein → jedes Artefakt
bekommt den lesbaren Schrittnamen aus Schritt 5. Andernfalls bleibt nur die technische Familie, und
die Grenze aus M15 (3) — Dienstnamen sind nicht zeigbar — ist im Auge zu behalten. Dass
`Message.Payload.GUID` auf Schritt `0` steht, ist bereits gemessen (M17 3) und wird nur bestätigt:
Die Originaldatei gehört in den **Kopf** der Detailansicht, nicht in die Zeitleiste.

**Ausgeführte Statements.** `scripts/messung-schritt8/sitzung6.sql` (Fenster A) und
`scripts/messung-schritt8/sitzung7.sql` (Fenster B), unverändert wie im Auftrag. Fenster B ist
gefahren, **weil Fenster A eine Auffälligkeit hat** — der Auftrag sieht B genau dafür vor.

**Ergebnis Fenster A** — 63 Kombinationen aus Name und `MessageActionID`. `ohne_schrittzeile`
ist in **jeder einzelnen Zeile 0** und deshalb aus der Tabelle herausgezogen statt 63-mal
wiederholt.

| `MessagePropertyName` | `MessageActionID` | `zeilen` | `verschiedene_schrittnamen` | `ohne_schrittnamen` |
|---|---|---|---|---|
| `AS2Reader.Log.GUID` / `.Payload.GUID` | 0 | je 244 | 0 | **je 244** |
| `AS2Sender.Log.GUID` / `.Payload.GUID` | 2 | je 36 | 22 | 0 |
| `AS2Sender.Log.GUID` / `.Payload.GUID` | 3 | je 2 | 2 | 0 |
| `AS2Sender.Log.GUID` / `.Payload.GUID` | 4 | je 1 | 1 | 0 |
| `Converter.Log.GUID` / `.Payload.GUID` | 1 | je 5.199 | 76 | 0 |
| `Converter.Log.GUID` / `.Payload.GUID` | 2 | je 1.537 | 35 | 0 |
| `Converter.Log.GUID` / `.Payload.GUID` | 3 | je 443 | 14 | 0 |
| `Converter.Log.GUID` / `.Payload.GUID` | 4 | je 683 | 10 | 0 |
| `DataWarehouse.Payload.GUID` | 1 | 950 | 4 | 0 |
| `FileReader.Log.GUID` / `.Payload.GUID` | 0 | je 4.199 | 0 | **je 4.199** |
| `FileReader.Log.GUID` / `.Payload.GUID` | 1 | je 29 | 2 | 0 |
| `FileReader.Log.GUID` / `.Payload.GUID` | 2 | je 124 | 1 | 0 |
| `FTPReader.Log.GUID` / `.Payload.GUID` | 0 | je 111 | 0 | **je 111** |
| `FTPSender.Log.GUID` / `.Payload.GUID` | 1 | je 99 | 4 | 0 |
| **`FTPSender.Log.GUID` / `.Payload.GUID`** | **2** | **je 4.019** | **10** | **je 3.985** |
| `FTPSender.Log.GUID` / `.Payload.GUID` | 3 | je 127 | 3 | 0 |
| `FTPSender.Log.GUID` / `.Payload.GUID` | 4 | je 182 | 3 | 0 |
| `FTPSender.Log.GUID` / `.Payload.GUID` | 5 | je 55 | 2 | 0 |
| `FTPSender.Log.GUID` / `.Payload.GUID` | 6 | je 8 | 1 | 0 |
| `HTTPReader.Log.GUID` / `.Payload.GUID` | 0 | je 1 | 0 | **je 1** |
| `HTTPSender.Log.GUID` / `.Payload.GUID` | 2 | je 24 | 1 | 0 |
| `HTTPSender.Log.GUID` / `.Payload.GUID` | 3 | je 672 | 1 | 0 |
| `MailReader.Log.GUID` / `.Payload.GUID` | 0 | je 32 | 0 | **je 32** |
| **`Message.Payload.GUID`** | **0** | **6.249** | **0** | **6.249** |
| `OFTP2Reader.Log.GUID` / `.Payload.GUID` | 0 | je 46 | 0 | **je 46** |
| `OFTP2Sender.Log.GUID` / `.Payload.GUID` | 2 | je 4 | 1 | 0 |
| `OFTP2Sender.Log.GUID` / `.Payload.GUID` | 3 | je 2 | 1 | 0 |
| `OFTPReader.Log.GUID` / `.Payload.GUID` | 0 | je 218 | 0 | **je 218** |
| `OFTPSender.Log.GUID` / `.Payload.GUID` | 2 | je 9 | 1 | 0 |
| `OFTPSender.Log.GUID` / `.Payload.GUID` | 3 | je 11 | 1 | 0 |
| `SAPReader.Log.GUID` / `.Payload.GUID` | 0 | je 443 | 0 | **je 443** |
| `SSHReader.Log.GUID` / `.Payload.GUID` | 0 | je 5 | 0 | **je 5** |

Summiert: **44.329 Zeilen**, davon **24.817 ohne Schrittnamen** = **55,98 %**.

**Ergebnis Fenster B** — dieselbe Abfrage, dazu eine Verdichtung im selben Lauf:

| Kennzahl | Wert |
|---|---|
| `zeilen_gesamt` | **1.516.642** *(deckt sich Zeichen für Zeichen mit der Summe aus M54 (a) Fenster B)* |
| `ohne_schrittzeile_gesamt` | **0** |
| `ohne_schrittnamen_gesamt` | **784.338** = **51,72 %** |

Die Aufschlüsselung von Fenster B folgt derselben Gestalt wie A: Die Reader-Familien und
`Message.Payload.GUID` sitzen auf `MessageActionID = 0` und tragen dort geschlossen keinen
Schrittnamen — allein `Message.Payload.GUID` sind 214.330 Zeilen, `SAPReader.*` je 45.333,
`OFTPReader.*` je 11.236.

**Befund 1 — `ohne_schrittzeile = 0`, ausnahmslos.**
*Gemessen war:* In allen 63 Kombinationen in Fenster A und in der Summe über Fenster B ist
`ohne_schrittzeile` **0**.
*Behauptet wird:* Zu jedem Artefaktverweis gibt es die zugehörige `MessageAction`-Zeile. Der Join
über **beide** Spalten von `MessageAction` trägt vollständig; die Warnung aus M15, nicht über
`Message.SOSID` zu gehen, ist eingehalten und kostet nichts.

**Befund 2 — mehr als die Hälfte der Artefakte hat keinen lesbaren Schrittnamen.**
*Gemessen war:* **55,98 %** in Fenster A (24.817 von 44.329), **51,72 %** in Fenster B (784.338 von
1.516.642).
*Behauptet wird:* Die vorregistrierte Bedingung lautet „`ohne_schrittzeile = 0` **und**
`ohne_schrittnamen` klein". Die erste Hälfte ist erfüllt, die zweite **deutlich nicht**. Damit gilt
die Gegenzeile: Für die Mehrheit der Artefakte bleibt nur die **technische Familie** als
Beschriftung. Ob diese Familie zeigbar ist, entscheidet die Grenze aus M15 (3) und **nicht** diese
Runde.

**Befund 3 — es sind zwei verschiedene Ursachen, und sie dürfen nicht verrechnet werden.**
*Gemessen war:* 17.335 der 24.817 namenlosen Zeilen in Fenster A stehen auf `MessageActionID = 0`
und betreffen die Reader-Familien und `Message.Payload.GUID`. Die übrigen **7.970** stehen auf
`MessageActionID = 2` und gehören zu `FTPSender` — dort haben 3.985 von 4.019 Zeilen je Richtung
**eine** `MessageAction`-Zeile, aber **keinen** Namen dazu, während dieselbe Kombination gleichzeitig
10 verschiedene Schrittnamen kennt.
*Behauptet wird:* Schritt `0` ohne Namen ist erwartbar und bereits in M17 (3) gemessen — dort hängen
die Metadaten der Nachricht, und die Originaldatei gehört in den **Kopf** der Detailansicht. Der
`FTPSender`-Fall ist etwas anderes: ein echter Schritt, dessen Name in 99,2 % der Fälle fehlt.
Woran das liegt, sagt M57 nicht. **Fünfte Stelle, an der der Befund in keine vorformulierte Zeile
passt** — die Deutung kennt nur „klein" oder „nicht klein" und nicht zwei Ursachen mit
verschiedener Tragweite.

**Laufzeit.** Fenster A **0,900 s**, Fenster B **33,296 s** (Aufschlüsselung) und **27,473 s**
(Verdichtung). Alle einmalig.

---

## M58 — Kostet das etwas? (Regel L7)

**Frage.** Was kosten die drei Abfragen, die der Endpunkt fahren wird — Artefaktliste einer
Nachricht, Auflösung einer Kennung, Existenznachweis der Nachricht für den Mandanten? Je mit
`EXPLAIN` und Laufzeit, beste von fünf nach einem Aufwärmlauf, je einmal für `NEXANS` und einen
kleinen Mandanten (L15).

**Vorregistrierte Deutung.** Die erste Abfrage muss `Using index` auf dem Primärschlüssel von
`MessageProperty` zeigen und deutlich unter 50 ms bleiben — derselbe Zugriffsweg wie im
Nachrichtendetail. Tut sie das nicht, gehört die Namensbedingung nach dem Abruf in den Code.

**Ausgeführte Statements.** `scripts/messung-schritt8/sitzung6.sql`.

**Auswahl der Prüfnachrichten.** Je Mandant die Nachricht aus Fenster A mit den **meisten**
Artefaktverweisen — der teuerste Fall, wie L7 ihn verlangt —, Gleichstand nach kleinster
`MessageID`. Die Auswahl läuft als Abfrage in der Sitzung selbst; in der Skriptdatei und in dieser
Datei steht **keine** `MessageID` (G1). Gemessen trägt die `NEXANS`-Nachricht **13** Artefakte, die
`IBISGUS`-Nachricht **7**.

**`EXPLAIN` (1) — Artefaktliste einer Nachricht, gegen das rohe Statement:**

```
+------+-------------+-------+-------+-------------------------------------------------------------------+---------+---------+-------------+------+-------------+
| id   | select_type | table | type  | possible_keys                                                     | key     | key_len | ref         | rows | Extra       |
+------+-------------+-------+-------+-------------------------------------------------------------------+---------+---------+-------------+------+-------------+
|    1 | SIMPLE      | m     | const | PRIMARY,ProejctIDIDX,Message_ProcessFK                            | PRIMARY | 146     | const       | 1    |             |
|    1 | SIMPLE      | p     | const | PRIMARY,Process_ProjectFK                                         | PRIMARY | 146     | const       | 1    |             |
|    1 | SIMPLE      | pm    | const | PRIMARY,ProjectMandant_Mandant_idx                                | PRIMARY | 292     | const,const | 1    | Using index |
|    1 | SIMPLE      | mp    | ref   | PRIMARY,MessageProperty_MessageActionFK,MessageProperty_MessageFK | PRIMARY | 146     | const       | 38   | Using where |
+------+-------------+-------+-------+-------------------------------------------------------------------+---------+---------+-------------+------+-------------+
```

**`EXPLAIN` (2) — Auflösung einer Kennung:**

```
+------+-------------+---------+-------+---------------+---------+---------+-------+------+-------+
| id   | select_type | table   | type  | possible_keys | key     | key_len | ref   | rows | Extra |
+------+-------------+---------+-------+---------------+---------+---------+-------+------+-------+
|    1 | SIMPLE      | Service | const | PRIMARY       | PRIMARY | 146     | const | 1    |       |
+------+-------------+---------+-------+---------------+---------+---------+-------+------+-------+
```

**`EXPLAIN` (3) — Existenznachweis der Nachricht für den Mandanten:**

```
+------+-------------+-------+-------+----------------------------------------+---------+---------+-------------+------+-------------+
| id   | select_type | table | type  | possible_keys                          | key     | key_len | ref         | rows | Extra       |
+------+-------------+-------+-------+----------------------------------------+---------+---------+-------------+------+-------------+
|    1 | SIMPLE      | m     | const | PRIMARY,ProejctIDIDX,Message_ProcessFK | PRIMARY | 146     | const       | 1    |             |
|    1 | SIMPLE      | p     | const | PRIMARY,Process_ProjectFK              | PRIMARY | 146     | const       | 1    |             |
|    1 | SIMPLE      | pm    | const | PRIMARY,ProjectMandant_Mandant_idx     | PRIMARY | 292     | const,const | 1    | Using index |
+------+-------------+-------+-------+----------------------------------------+---------+---------+-------------+------+-------------+
```

**Laufzeiten** — beste von fünf nach einem Aufwärmlauf, wie beauftragt:

| Abfrage | Mandant | Artefakte | Aufwärmlauf | **beste von fünf** |
|---|---|---|---|---|
| (1) Artefaktliste | `NEXANS` | 13 | 0,904 ms | **0,748 ms** |
| (1) Artefaktliste | `IBISGUS` | 7 | 0,739 ms | **0,702 ms** |
| (2) Auflösung einer Kennung | — *(mandantenunabhängig)* | — | 0,358 ms | **0,336 ms** |
| (3) Existenznachweis | `NEXANS` | — | 0,602 ms | **0,512 ms** |
| (3) Existenznachweis | `IBISGUS` | — | 0,517 ms | **0,504 ms** |

**Befund 1 — der Zugriffsweg kostet praktisch nichts.**
*Gemessen war:* Alle fünf Messreihen liegen zwischen **0,336 ms** und **0,748 ms**. Der teuerste
Fall — 13 Artefakte, größter Mandant — liegt bei 0,748 ms.
*Behauptet wird:* Die Erwartung „deutlich unter 50 ms" ist mit Faktor **67** unterschritten. Der
gesamte Datenbankteil eines Artefakt-Endpunkts liegt zusammengenommen (1 + 2 + 3) unter **1,6 ms**.
Was der Endpunkt kostet, entscheidet damit der Filestore und nicht die Datenbank.

**Befund 2 — der Unterschied zwischen großem und kleinem Mandanten ist keiner.**
*Gemessen war:* 0,748 ms gegen 0,702 ms bei (1), 0,512 ms gegen 0,504 ms bei (3).
*Behauptet wird:* Die Kontrolle nach L15 ist unauffällig. Der Zugriff hängt an der `MessageID` und
nicht an der Größe des Mandanten — was der `EXPLAIN` mit `const` auf `m`, `p` und `pm` auch zeigt.

**Befund 3 — `Using index` erscheint nicht, und die vorformulierte Folgerung passt trotzdem nicht.**
*Gemessen war:* `mp` läuft als `ref` über `PRIMARY` mit `key_len 146` und `rows 38`, aber mit
`Using where` statt `Using index`.
*Behauptet wird:* Die vorregistrierte Bedingung ist zweiteilig — „`Using index` **und** deutlich
unter 50 ms". Die zweite Hälfte ist mit Faktor 67 erfüllt, die erste nicht. **Die dafür
vorgesehene Folgerung („dann gehört die Namensbedingung nach dem Abruf in den Code") trifft die
Ursache aber nicht:** `Using index` fehlt, weil das Statement `MessagePropertyValue` **auswählt** und
diese Spalte nicht im Index liegt — nicht wegen der Namensbedingung. M17 (1) hat an derselben Stelle
`Using index` gemessen, weil dort nur gezählt wurde. Die Namensbedingung ins Programm zu ziehen
würde `Using index` **nicht** herstellen. Was zu tun ist, entscheidet diese Runde nicht.
**Sechste Stelle, an der der Befund in keine vorformulierte Zeile passt.**

> **Abweichung bei der Laufzeitmessung.** Die fünf Messläufe von (1) laufen gegen eine **zählende
> Hülle** um das echte Statement (`SELECT COUNT(*), SUM(CHAR_LENGTH(v)) FROM ( <echtes Statement> ) x`),
> weil das echte Statement `MessagePropertyValue` liefert und diese Werte nach G1 nicht auf den
> Bildschirm dürfen. Der **`EXPLAIN` läuft gegen das rohe Statement**, unverändert. Die Hülle
> materialisiert 13 bzw. 7 Zeilen; sie ist nicht der Grund für die gemessenen 0,7 ms. Dasselbe gilt
> für (2), wo `ServiceConnectString` durch `CHAR_LENGTH(...)` ersetzt ist.

---

## M64a — Kandidatenauswahl für die Fehlerprotokolle

**Frage.** Tragen Fehlernachrichten überhaupt ein Protokoll? Fenster B. **Ohne Fenster wird
ausdrücklich nicht gefahren** — die Fehlerzahlen des Gesamtbestands stehen bereits in §4.1 der
Projektbeschreibung.

**Vorregistrierte Deutung.** Tragen Fehlernachrichten **kein** Protokoll, ist die Protokollansicht
gerade dort leer, wo sie am meisten gebraucht wird — und das wäre der schwerwiegendste Befund der
ganzen Runde für den Nutzen des Features.

**Ausgeführtes Statement.** `scripts/messung-schritt8/sitzung6.sql`, unverändert wie im Auftrag.
Der Lauf **ohne** Fenster ist wie beauftragt **nicht** gefahren.

**Ergebnis Fenster B**

| `MessageStatus` | `nachrichten` | `mit_protokoll` | `familien` |
|---|---|---|---|
| `COMMIT_REJECTED` | 5 | **5** | 2 |
| `ERROR_TIMEOUT` | 3 | **3** | 3 |
| `ERROR_DUPLICATE` | 3 | **3** | 2 |
| **Summe** | **11** | **11** | — |

**Befund 1 — Fehlernachrichten tragen ein Protokoll, ausnahmslos.**
*Gemessen war:* `mit_protokoll = nachrichten` bei allen drei Status. Elf von elf.
*Behauptet wird:* Der schwerwiegendste vorregistrierte Befund — „die Protokollansicht ist gerade
dort leer, wo sie am meisten gebraucht wird" — ist **nicht** eingetreten. Bei elf Nachrichten ist
das eine schwache Stichprobe; sie ist aber der Vollbestand des Fensters und nicht eine Auswahl
daraus.

**Befund 2 — Fenster B enthält nur elf Fehlernachrichten, und M64 kann deshalb nicht 200 Dateien
prüfen.**
*Gemessen war:* 11 Fehlernachrichten in Fenster B. Der Auftrag setzt für M64 „alles, was M64a
findet, bis maximal 200 Dateien" an.
*Behauptet wird:* Die Obergrenze 200 wird nicht erreicht; M64 läuft über die Protokolle dieser elf
Nachrichten. Der Grund ist bekannt und liegt nicht an der Abfrage: `messungen-schritt5.md`
(Z. 812–826) misst die 49 `ERROR_TIMEOUT` des Dezembers in einer Spanne von 62 Sekunden am
**`2025-12-30` um 04:08–04:09** — und Fenster B endet am `2025-12-30 00:00:00`, also **vier Stunden
davor**. Ob das Fenster für M64 zu erweitern ist, ist eine Entscheidung und steht unter „Offene
Punkte".

**Befund 3 — Widerspruch zu `PROJEKTBESCHREIBUNG.md` §4.1.**
*Gemessen war:* In Fenster B 3 × `ERROR_DUPLICATE`, 5 × `COMMIT_REJECTED`, 3 × `ERROR_TIMEOUT`.
*Behauptet wird:* Nichts über den Gesamtbestand — der Lauf ohne Fenster ist bewusst nicht gefahren.
Die Zahlen aus §4.1 (`ERROR_DUPLICATE` 659, `COMMIT_REJECTED` 111, `ERROR_TIMEOUT` 52) sind
Bestandszahlen und mit diesen Fensterzahlen **nicht** vergleichbar. Sie werden hier weder bestätigt
noch bestritten.

**Laufzeit.** **0,023 s**, einmalig.

---

## Abschluss Teil A

| Nachweis | Wert |
|---|---|
| `@@global.read_only` in der Schlusssitzung | **`1`** |
| `@@read_only` in der Schlusssitzung | **`1`** |
| Serverzeit Ende | `2026-08-17 12:07:11` |

Die Testkopie war zu Beginn (`11:51:22`) und am Ende (`12:07:11`) von Teil A schreibgeschützt.
**Kein Statement der Runde war etwas anderes als `SELECT`, `SET`, `EXPLAIN` oder `SHOW PROFILES`.**
Sieben Sitzungen, jede mit eigener Verbindung, jede mit `read_only`-Nachweis als erstem Statement.

---

# Teil B — Der Filestore

## Der freigegebene Envelope

Der Auftraggeber hat am 17.08.2026 **genau eine** POST-Form freigegeben (Auftrag, „Ergänzung
17.08.2026"). Der Auftrag verlangt, den Envelope **vor der ersten Ausführung** im Volltext
aufzunehmen, damit später nachvollziehbar ist, was gesendet wurde. Das ist hiermit geschehen.

**Die Platzhalter der Freigabe, ausgefüllt aus dem Altcode:**

| Platzhalter der Freigabe | Eingesetzter Wert | Fundstelle |
|---|---|---|
| Endpunkt | `ServiceConnectString` der jeweiligen Ablage, unverändert | `Service`, M52 |
| Operation | **`RETRIEVE`** | `FilestoreFile.java:18` (Enum `filestoreAction`), als Attribut `Action` gesetzt in `FilestoreClient.java:76` |
| Namensraum | `http://filestore.kraftwerkone.de` | `FilestoreClient.java:68` |
| Envelope-Vorlage | `FilestoreClient.java:67–92` | `SOAPBody` → `FileList` → `File`-Kinder |
| `ID` | die GUID **hinter** der Pipe aus dem Artefaktverweis | QT1 (`url.split("\|")` im Altwerkzeug); der Teil **vor** der Pipe wählt den Endpunkt |

**Der gesendete Envelope, im Volltext.** `<GUID>` ist die einzige Stelle, die je Anfrage wechselt:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
<SOAP-ENV:Body>
<m:FileList xmlns:m="http://filestore.kraftwerkone.de">
<File Counter="0" ID="<GUID>" Action="RETRIEVE"/>
</m:FileList>
</SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

**Warum er genau so aussieht** — jede Eigenschaft ist gelesen, keine geraten:

| Eigenschaft | Begründung |
|---|---|
| **kein `<SOAP-ENV:Header/>`** | `soapHeader.detachNode()`, `FilestoreClient.java:31–32` |
| `FileList` **qualifiziert** mit Präfix `m` | `new QName("http://filestore.kraftwerkone.de", "FileList", "m")`, Z. 68 |
| `File` und seine Attribute **unqualifiziert** | `addChildElement(new QName("File"))` ohne Namensraum, Z. 73; `addAttribute(new QName(...))` Z. 74–76 |
| Attributreihenfolge `Counter`, `ID`, `Action` | Reihenfolge der `addAttribute`-Aufrufe, Z. 74–76 |
| `Counter="0"` | `filestoreFilesCounter` beginnt bei `0`, Z. 70 |
| **kein Anhang in der Anfrage** | Der `AttachmentPart` entsteht ausschließlich im Zweig `CREATE`, Z. 79–85. `RETRIEVE` hat einen **leeren** Zweig, Z. 86–87 |
| `Content-Type: text/xml; charset=utf-8`, `SOAPAction: ""` | **von Hand gesetzt, nicht aus dem Quelltext belegt** — siehe Korrektur unten |

> **Korrektur vom 17.08.2026.** Die letzte Zeile der Tabelle lautete ursprünglich:
>
> > | `Content-Type: text/xml; charset=utf-8`, `SOAPAction: ""` | Voreinstellung von SAAJ SOAP 1.1 für `SOAPConnection.call`, Z. 94 |
>
> **Das war sachlich falsch.** Zeile 94 belegt den Versand (`soapConnection.call(...)`), **nicht die
> Werte**. `FilestoreClient.java` setzt weder `Content-Type` noch `SOAPAction`: Es gibt in der Datei
> keinen Aufruf von `getMimeHeaders()`, `setHeader(...)` oder `addHeader(...)`. Beide Kopfzeilen
> bestimmt SAAJ, und **was SAAJ auf die Leitung legt, steht in keinem der vorliegenden
> Quelltexte**. Die in M59 (2) gesendeten Werte waren damit gesetzt, nicht gelesen. Die alte Zeile
> steht oben im Kasten, damit die Änderung nachvollziehbar bleibt; überschrieben wird sie nicht.

> **Warum das keine ablegende Operation sein kann.** Eine Anfrage mit `Action="RETRIEVE"` trägt
> nach Z. 78–90 **keinen** `AttachmentPart` — es gibt in der Anfrage also keine Nutzdaten, die
> abgelegt werden könnten. `CREATE`, `APPEND` und `DELETE` werden in dieser Runde **nie** gesendet.

**Eingehalten wird die Sperrklausel der Freigabe:** keine andere Operation, kein selbst gebauter
Envelope, keine Abwandlung „zum Ausprobieren", kein Aufruf ohne GUID aus der Stichprobe der
jeweiligen Messung. Die Holregel aus M68 gilt unverändert — geholt wird bei der Ablage, die der
Verweis nennt.

---

## M59 — Abruf, Antwortform und der Fehlerfall *(beantwortet V3 und V5)*

**Frage.** Wie wird eine Datei abgerufen, wie sieht die Antwort aus, und was antwortet eine nicht
vorhandene Datei? Vollständig für **beide** Ablagen, Ergebnisse nebeneinander: Statuscode,
`Content-Type`, `Content-Length`, `Content-Disposition`, Authentifizierung ja/nein, Antwortzeit.
Dazu **drei Fehlerfälle je Ablage**: eine erfundene UUID, eine syntaktisch falsche UUID, eine leere
Kennung.

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Beide Ablagen antworten in **Form und Fehlerverhalten gleich** | Der Proxy braucht **einen** Codepfad. Die Verallgemeinerung auf elf Knoten bleibt eine begründete Annahme, aber sie hat jetzt einen Beleg statt keinen |
| Sie unterscheiden sich | Der Proxy braucht Fallunterscheidung je Knoten — und damit ist der geplante Zuschnitt zu klein. **Das ist ein Anhaltegrund**, denn neun weitere Knoten sind ungesehen |

**Weitere vorregistrierte Deutung.** Ein gelieferter `Content-Type` wird **nicht** durchgereicht —
§7 schreibt `application/octet-stream` fest; gemessen wird er, um zu wissen, ob er überhaupt etwas
trägt. Antwortet eine nicht vorhandene Datei mit `200` und einer HTML-Seite, ist das der
**wichtigste Befund dieser Runde**: Der Proxy müsste dann am Rumpf erkennen, ob er eine Datei oder
eine Fehlerseite weiterreicht — und die Oberfläche könnte „Datei weg" nicht von „Filestore kaputt"
unterscheiden. **Abbruchbedingung.**

**Ausgeführte Anfragen.** `curl.exe` 8.19.0, ausschließlich `GET`, `HEAD` und `OPTIONS`. Je Ablage
16 GET-Formen, 6 Fehlerfälle, dazu Kopfzeilen, `OPTIONS` und `HEAD`. **44 Anfragen insgesamt**,
sequenziell, keine gleichzeitig. Kein `POST` — Begründung unten.

**Was hinter dem `ServiceConnectString` steht.** Beide Werte sind 64 Zeichen lang, beginnen mit
`http://`, tragen vier Schrägstriche (M52 Befund 4) und enden auf
**`/WebApplication/FileStoreSoapReceiver`**. Das ist ein **SOAP-Empfänger**, keine Dateiadresse.
*(Der Wert selbst steht nach G1 nirgends in dieser Datei; die Knoten heißen hier `<Knoten09>` und
`<Knoten10>`.)*

**Ergebnis — Kopfzeilen der Basisanfrage, beide Ablagen**

| Kopfzeile | `FILESTOREPROD09` | `FILESTOREPROD10` |
|---|---|---|
| Statuscode | **200 OK** | **200 OK** |
| `Server` | `Apache-Coyote/1.1` | `Apache-Coyote/1.1` |
| `Content-Type` | `text/xml;charset=utf-8` | `text/xml;charset=utf-8` |
| `Content-Length` | **185** | **185** |
| `Content-Disposition` | **fehlt** | **fehlt** |
| `WWW-Authenticate` | **fehlt** | **fehlt** |
| `Accept` (in der Antwort) | `text/xml, text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2` | identisch |
| Authentifizierung | **keine** — `200` ohne jede Zugangsangabe | **keine** |
| `OPTIONS` → `Allow` | `GET, HEAD, POST, TRACE, OPTIONS` | *(nicht gefahren, siehe unten)* |

**Ergebnis — die sechzehn GET-Formen, beide Ablagen**

| Form | Statuscode | `Content-Type` | Länge |
|---|---|---|---|
| Basis, `?wsdl`, `?xsd=1` | **200** | `text/xml` | **185** |
| `?id=`, `?guid=`, `?file=`, `?fileId=`, `?filename=`, `?fileName=`, `?FileName=`, `?name=`, `?key=`, `?uuid=`, `?path=`, `?action=RETRIEVE&file=` — je mit echter UUID | **200** | `text/xml` | **185** |
| `/<uuid>` im Pfad | **404** | `text/html` | 1.174 |

**Alle sechzehn Formen liefern dieselben 185 Byte**, an beiden Knoten, mit und ohne Parameter, mit
echter und ohne Kennung.

**Ergebnis — die drei Fehlerfälle, je Ablage und je Form**

| Fehlerfall | Pfadform | Abfrageform (`?file=`) |
|---|---|---|
| 1 erfundene, syntaktisch gültige UUID | **404**, `text/html`, 1.174 B | **200**, `text/xml`, **185 B** |
| 2 syntaktisch falsche UUID | **404**, `text/html`, 1.123 B | **200**, `text/xml`, **185 B** |
| 3 leere Kennung | **404**, `text/html`, 1.066 B | **200**, `text/xml`, **185 B** |

Die drei 404-Längen unterscheiden sich um genau die Länge der angefragten Zeichenkette — die Seite
gibt den angefragten Pfad wieder. Das ist die Standardseite des Containers, keine Antwort der
Anwendung.

**Klassifikation der 185-Byte-Antwort** — ohne den Rumpf auszugeben, ausschließlich über Prüfungen:

| Prüfung | Ergebnis |
|---|---|
| Rumpf von `09` und `10` byteidentisch (SHA-256) | **ja** |
| enthält `Envelope` | ja |
| enthält `FileList` | ja |
| enthält `<File ` | **nein** |
| enthält `Fault` | **nein** |
| Länge | 185 |

**Befund 1 — die beiden Ablagen sind in Form und Fehlerverhalten gleich.**
*Gemessen war:* Gleiche Statuscodes, gleiche Kopfzeilen, gleiche Längen in allen 22 Anfragen je
Knoten; die Basisantwort ist zwischen beiden **byteidentisch**.
*Behauptet wird:* Die erste vorregistrierte Zeile trifft zu — der Proxy braucht **einen** Codepfad,
keine Fallunterscheidung je Knoten. Die Verallgemeinerung auf elf Knoten bleibt eine begründete
Annahme; sie hat jetzt einen Beleg aus zwei Knoten statt keinen. **Der Anhaltegrund „sie
unterscheiden sich" ist nicht eingetreten.**

**Befund 2 — der Abruf ist kein `GET`, und die Annahme des Auftrags trägt nicht.**
*Gemessen war:* Keine der sechzehn GET-Formen liefert etwas anderes als dieselben 185 Byte mit einer
**leeren `FileList`**; die Pfadform antwortet mit der 404-Seite des Containers. Es gibt keine
Schnittstellenbeschreibung (`?wsdl` liefert dieselben 185 Byte).
*Behauptet wird:* **V3 ist mit `GET` nicht beantwortbar.** Der Auftrag setzt in §1 voraus, eine
Datei werde mit `curl <adresse>` geholt; diese Voraussetzung ist gemessen **falsch**. Was der
richtige Abrufweg ist, sagt M59 nicht — sie sagt nur, dass er nicht über `GET` läuft.

**Befund 3 — keine Authentifizierung.**
*Gemessen war:* `200` ohne jede Zugangsangabe, keine `WWW-Authenticate`-Kopfzeile, an beiden Knoten.
*Behauptet wird:* Der Endpunkt ist im Netz ohne Anmeldung erreichbar. Für den Proxy heißt das, dass
die Zugriffsprüfung **vollständig** im neuen Backend liegt — der Filestore prüft nichts. Das ist
eine Feststellung über den Ist-Zustand und keine Bewertung.

**Befund 4 — die Abbruchbedingung „nicht vorhandene Datei antwortet mit `200`" ist der Form nach
erfüllt, der Sache nach nicht.**
*Gemessen war:* Eine erfundene UUID über die Abfrageform ergibt **`200`** — aber mit
`text/xml` und einer leeren `FileList`, nicht mit einer HTML-Seite. Über die Pfadform ergibt sie
`404` mit HTML.
*Behauptet wird:* Die vorregistrierte Zeile lautet „`200` **und einer HTML-Seite**". Gemessen ist
`200` **ohne** HTML beziehungsweise HTML **ohne** `200`. Vor allem aber: Die `200` ist keine Antwort
auf eine Dateianfrage, sondern die Standardantwort des Empfängers auf **jedes** `GET` — auch auf
eines ganz ohne Kennung. Solange es keinen Abrufweg gibt, gibt es auch keine „Antwort auf eine nicht
vorhandene Datei". **Die Abbruchbedingung wird deshalb als nicht eingetreten geführt, und diese
Einordnung ist ausdrücklich als solche kenntlich gemacht statt stillschweigend getroffen.**
**Siebte Stelle, an der der Befund in keine vorformulierte Zeile passt.**

**Was nicht gemessen ist, und warum nicht.** `OPTIONS` weist `POST` als erlaubt aus, und der Name
des Endpunkts nennt SOAP. Der nächste Schritt ist also ein `POST` mit einem SOAP-Rumpf.

Beim ersten Durchgang dieser Messung war er aus zwei Gründen nicht gegangen worden: Der Auftrag sah
kein `POST` vor, und ein *Receiver* ist die Stelle, an der Dateien **abgelegt** werden. **Beide
Gründe sind seit der Freigabe vom 17.08.2026 entfallen** — der erste, weil der Auftraggeber genau
eine POST-Form freigegeben hat; der zweite, weil im Altcode nachgelesen ist, dass eine
`RETRIEVE`-Anfrage konstruktionsbedingt keinen Anhang trägt und damit nichts ablegen kann.

**Der `POST` ist am selben Tag gefahren worden.** Er steht als eigene Messung unter **M59 (2)**,
weil er eine eigene Frage, eine eigene Anfrageform und ein eigenes Ergebnis hat. Kurz: Er liefert
dieselben 185 Byte wie das `GET`.

**Laufzeit.** Beste Antwortzeit der Basisanfrage: **6,61 ms** (`FILESTOREPROD09`) und **6,71 ms**
(`FILESTOREPROD10`), je beste aus den Läufen dieser Messung. Die 404-Antworten liegen bei
3,8–5,2 ms. Alle Zeiten stammen aus `curl -w %{time_total}`.

---

## M68 — Sind die Ablagen austauschbar? *(Kreuzabruf)*

**Frage.** Bedeutet die Kennung im Verweis etwas? Zehn Verweise auf `FILESTOREPROD09` zusätzlich bei
`FILESTOREPROD10` abrufen und umgekehrt. Gemessen wird ausschließlich Statuscode und
`Content-Length` — **kein Rumpf, kein Inhaltsvergleich.**

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Der Kreuzabruf schlägt durchgehend fehl | Die Kennung im Verweis **bedeutet etwas**. Die Auflösung über `Service` ist tragend, und sie richtig zu bauen ist Pflicht, nicht Höflichkeit |
| Der Kreuzabruf gelingt durchgehend, gleiche `Content-Length` | Die Ablagen sind **Spiegel**. Die Auflösung über `Service` wäre dann heute funktional entbehrlich — sie wird **trotzdem gebaut**, weil elf Zeilen in `Service` stehen und ein Spiegel eine Betriebsentscheidung ist, die sich ändern kann. Der Befund gehört aber dokumentiert, sonst hält ihn später jemand für eine Zusage |
| Er gelingt teilweise | Der unangenehmste Fall: teilweise gespiegelt, teilweise nicht. Dann ist jede Aussage über Vorhandensein aus M66 **je Ablage** zu lesen und nicht über beide |

**Gefahren am 17.08.2026** mit dem echten Client. *(Über `GET` war die Messung sinnlos — dort
liefert jede Ablage auf jede Anfrage dieselben 185 Byte, und der Kreuzabruf hätte „durchgehend
gespiegelt" ergeben, ohne dass eine Datei im Spiel gewesen wäre.)*

**Stichprobe:** die je zehn ersten Verweise aus Fenster A, sortiert nach GUID, die
`FILESTOREPROD09` bzw. `FILESTOREPROD10` nennen. **Übergangene Verweise: 0.**

**Dazu eine Gegenprobe, die der Auftrag nicht verlangt.** Dieselben zwanzig Verweise sind
**zusätzlich am eigenen Knoten** geholt worden. Ohne sie ist ein Fehlschlag des Kreuzabrufs nicht
deutbar: Er könnte „nicht gespiegelt" heißen oder „die Datei gibt es überhaupt nicht". **40 Abrufe
insgesamt**, Dauer 1.545 ms (38 ms je Verweis).

> **M68 ist die einzige vorgesehene Ausnahme von der Holregel** und im Auftrag als eigene Messung
> benannt. Sie ist genau so gefahren worden und **nicht** nebenbei aus einem anderen Lauf heraus.

**Ergebnis**

| Lauf | Abrufe | `SUCCESS (RETRIEVE)` | `Error (Skipped)` | Datei geliefert |
|---|---|---|---|---|
| **gerade** (eigener Knoten, Gegenprobe) | 20 | **20** | 0 | **20** |
| **gekreuzt** (andere Ablage) | 20 | **0** | **20** | **0** |

**Befund 1 — der Kreuzabruf schlägt durchgehend fehl.**
*Gemessen war:* 20 von 20 Kreuzabrufen antworten `Error (Skipped)` und liefern keine Datei —
während dieselben 20 Verweise am eigenen Knoten **ausnahmslos** `SUCCESS (RETRIEVE)` liefern.
*Behauptet wird:* Die erste vorregistrierte Zeile ist eingetreten: **Die Kennung im Verweis bedeutet
etwas.** Die Ablagen sind **keine Spiegel**. Die Auflösung über `Service` ist tragend, und sie
richtig zu bauen ist Pflicht, nicht Höflichkeit. Die dritte Zeile („teilweise gespiegelt") ist
nicht eingetreten — es ist **0 von 20**, nicht ein Teil.

**Befund 2 — die Gegenprobe schließt die Lücke in der Deutung.**
*Gemessen war:* Beide Läufe betreffen dieselben zwanzig Verweise. Der eine liefert 20 Dateien, der
andere keine.
*Behauptet wird:* Der Fehlschlag des Kreuzabrufs liegt **an der Ablage, nicht an der Datei**. Ohne
die Gegenprobe wäre genau diese Unterscheidung offen geblieben.

**Befund 3 — `Error (Skipped)` ist die Antwort auf „Datei nicht vorhanden".**
*Gemessen war:* Derselbe Wert erscheint hier bei 20 Kreuzabrufen und in **M66 (2)** bei allen 153
Verweisen der jüngsten Zeitscheibe; `SUCCESS (RETRIEVE)` erscheint in allen erfolgreichen.
*Behauptet wird:* Der Dienst unterscheidet die beiden Fälle **im Rumpf**, über das Attribut
`Response`, nicht über den HTTP-Status. **Damit ist V5 beantwortet** — die Antwort auf eine nicht
vorhandene Datei ist ein wohlgeformter SOAP-Rumpf mit `Error (Skipped)` und **ohne** Anhang, nicht
eine HTML-Fehlerseite. Der Proxy kann „Datei weg" von „Filestore kaputt" unterscheiden.

---

## M66 — Decken sich die beiden Kopien? *(beantwortet V4)*

**Frage.** Trägt der Filestore dieselben Artefakte wie die Datenbank? Stichprobe von je 50
Nachrichten aus **fünf** Zeitscheiben: `2024-10`, `2025-04`, `2025-12`, `2026-05` und die letzte
Woche vor `2026-07-08`. Je Nachricht: Existiert die Datei zu `Message.Payload.GUID`? Zu den
`*.Log.GUID`? **Getrennt je Ablage ausweisen.**

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Alle fünf Scheiben vollständig | Der Filestore hält mindestens 22 Monate. Annahme A-neu: keine eigene Aufbewahrungsgrenze. Die Abnahme darf an jeder Nachricht hängen |
| Die ältesten Scheiben leer | Der Filestore hat eine **kürzere** Frist als die Datenbank. Dann trägt ein bezifferbarer Teil der Nachrichten einen Knopf, hinter dem nichts liegt — das ist ein Oberflächenzustand und ein Eintrag in die Annahmenliste |
| Die **jüngste** Scheibe unvollständig | Die Kopien laufen auseinander. Für die Abnahme ist dann eine Nachricht zu wählen, die in beiden liegt, und das ist zu notieren, damit es niemand später für einen Fehler hält |

**Gefahren am 17.08.2026** mit dem echten Client, nachdem M71 belegt hat, dass der Abrufweg trägt.
*(Der frühere Vermerk „nicht gefahren" galt für den `GET`-Weg aus M59 (1), über den die Frage keine
unterscheidbare Antwort hatte.)*

**Aufbau.** Wie M71: Projekt `scripts/mitschnitt-saaj/`, Einstiegspunkt `mitschnitt.Deckung`, die
vier Alt-Dateien byteidentisch. SHA-256 vor dem Lauf erneut geprüft und gleich den in M70
dokumentierten Summen. Die `<mainClass>`-Vorgabe im `pom.xml` bleibt entfernt, die Klasse wird bei
jedem Aufruf genannt — Abweichung 11 wiederholt sich nicht. Der Aufruf liegt als Skript vor:
`m66.ps1` mit `m66-auswahl.sql`, `m66-scheiben.sql` und `m66-grenzen.sql`.

**Stichprobe.** Fünf Zeitscheiben wie im Auftrag, **25 statt 50** Nachrichten je Scheibe
(Abweichung, Begründung unten). Je Nachricht `Message.Payload.GUID` und **alle** `*.Log.GUID`. Je
Scheibe die 25 ältesten Nachrichten, sortiert nach `MessageLastUpdate, MessageID` — deterministisch
und in Indexordnung. **Geholt wurde ausnahmslos bei der Ablage, die der Verweis nennt; kein
Rückfall, auch nicht bei Misserfolg.** Sequenziell, ein Abruf nach dem anderen.

Verbindungszeitlimit **3.000 ms**, Lesezeitlimit **20.000 ms**, gesetzt über
`sun.net.client.default*Timeout`. Ohne sie hängt ein Abruf gegen eine nicht laufende Ablage
unbegrenzt; gesetzt sind sie über Systemeigenschaften, damit `FilestoreClient.java` byteidentisch
bleibt.

### Die Scheiben vor dem Abruf

| Zeitscheibe | Nachrichten in der Datenbank |
|---|---|
| `2024-10` | 241.203 |
| `2025-04` | 226.421 |
| `2025-12` | 209.408 |
| **`2026-05`** | **0** |
| **`2026-07-01` bis `2026-07-08`** | **0** |

Bestandsgrenzen (ohne Fenster, ausschließlich über `Message`, Regel L9):
**ältester `2024-10-01 02:00:28`, jüngster `2026-07-08 17:21:10`**.

*Gemessen war:* Zwei der fünf beauftragten Scheiben tragen null Nachrichten, während der Bestand
bis zum `2026-07-08 17:21` reicht.
*Behauptet wird:* Die Kopie ist **nicht** früher abgeschnitten — sie hat eine **Lücke**. Die
jüngsten Nachrichten liegen auf dem `2026-07-08` selbst und damit **außerhalb** der beauftragten
Scheibe „letzte Woche vor `2026-07-08`". Die Scheibe ist wie beauftragt gefahren und nicht
verschoben worden; ob sie zu verschieben ist, steht unter „Offene Punkte" 17.

### Ergebnis, getrennt nach Zeitscheibe, Ablage und Art

**293 Verweise** insgesamt, davon 75 `Message.Payload.GUID` (25 je nicht leerer Scheibe) und 218
`*.Log.GUID`.

| Zeitscheibe | Ablage | Art | Versuche | Datei da | keine Datei | Fehler | Bytes | ZIP-Einträge |
|---|---|---|---|---|---|---|---|---|
| `2024-10` | `FILESTOREPROD07` | Protokoll | 77 | 0 | 0 | **77** | 0 | 0 |
| `2024-10` | `FILESTOREPROD07` | Nutzdaten | 23 | 0 | 0 | **23** | 0 | 0 |
| `2024-10` | `FILESTOREPROD08` | Protokoll | 3 | 0 | 0 | **3** | 0 | 0 |
| `2024-10` | `FILESTOREPROD08` | Nutzdaten | 2 | 0 | 0 | **2** | 0 | 0 |
| `2025-04` | `FILESTOREPROD07` | Protokoll | 44 | 0 | 0 | **44** | 0 | 0 |
| `2025-04` | `FILESTOREPROD07` | Nutzdaten | 17 | 0 | 0 | **17** | 0 | 0 |
| `2025-04` | `FILESTOREPROD08` | Protokoll | 23 | 0 | 0 | **23** | 0 | 0 |
| `2025-04` | `FILESTOREPROD08` | Nutzdaten | 8 | 0 | 0 | **8** | 0 | 0 |
| **`2025-12`** | **`FILESTOREPROD09`** | **Protokoll** | 46 | **46** | 0 | **0** | 64.610 | 46 |
| **`2025-12`** | **`FILESTOREPROD09`** | **Nutzdaten** | 16 | **16** | 0 | **0** | 258.387 | 16 |
| **`2025-12`** | **`FILESTOREPROD10`** | **Protokoll** | 25 | **25** | 0 | **0** | 24.673 | 25 |
| **`2025-12`** | **`FILESTOREPROD10`** | **Nutzdaten** | 9 | **9** | 0 | **0** | 2.463 | 9 |

**Attribut `Response`:** 96 × `SUCCESS (RETRIEVE)`, 197 × nicht gesetzt (die Fehlversuche).
Jede der 96 gelieferten Dateien war ein ZIP mit **genau einem** Eintrag — 96 Dateien, 96 Einträge.

**Dauer**

| | Verweise | Dauer |
|---|---|---|
| `2024-10` | 105 | 5.837 ms |
| `2025-04` | 92 | 179 ms |
| `2025-12` | 96 | 6.673 ms |
| **gesamt** | **293** | **12.690 ms** (43 ms je Verweis) |

Die 43 ms je Verweis liegen weit unter den 497 ms des Einzelabrufs aus M71; jene Zahl enthielt den
Start von JVM und SAAJ. Der Lauf ist **nicht** aus dem Ruder gelaufen und ist zu keinem Zeitpunkt
parallelisiert worden.

### Befunde

**Befund 1 — in `2025-12` deckt sich der Filestore vollständig mit der Datenbank.**
*Gemessen war:* 96 von 96 Verweisen geliefert, `SUCCESS (RETRIEVE)`, je eine Datei, je ein
ZIP-Eintrag — auf **beiden** Ablagen und für **beide** Arten. Kein einziges „keine Datei".
*Behauptet wird:* Für diese eine Zeitscheibe und diese Stichprobe gibt es zu jedem Verweis eine
Datei. Über andere Zeitscheiben sagt das nichts.

**Befund 2 — die beiden ältesten Scheiben sind nicht leer, sondern nicht messbar.**
*Gemessen war:* Alle 197 Verweise aus `2024-10` und `2025-04` nennen `FILESTOREPROD07` oder
`FILESTOREPROD08`. Jeder Abruf endete mit `Message send failed` — die Verbindung kam nicht zustande.
`Response` blieb in allen 197 Fällen ungesetzt.
*Behauptet wird:* **Nicht**, dass dort keine Dateien liegen. Gemessen ist, dass die Ablagen **nicht
erreichbar** sind — was M53 vorhergesagt hat und was eine Betriebsentscheidung ist, keine
Aufbewahrungsfrist. **Die vorregistrierte Zeile „die ältesten Scheiben leer → der Filestore hat eine
kürzere Frist als die Datenbank" ist damit nicht ausgelöst**, obwohl das Zahlenbild ihr ähnelt: Sie
setzt voraus, dass die Ablage antwortet und die Datei fehlt. Hier antwortet die Ablage nicht.
Die Unterscheidung ist mit dieser Messung **nicht auflösbar**, weil die Holregel einen Rückfall auf
die andere Ablage verbietet — richtigerweise, denn genau dieser Rückfall verwandelte „die Datei ist
weg" in „die Datei war woanders". **Neunte Stelle, an der der Befund in keine vorformulierte Zeile
passt.**

**Befund 3 — die jüngste Scheibe ist nicht unvollständig, sondern nicht vorhanden.**
*Gemessen war:* `2026-05` und die Woche vor dem `2026-07-08` tragen null Nachrichten; der Bestand
reicht dennoch bis `2026-07-08 17:21:10`.
*Behauptet wird:* Die dritte vorregistrierte Zeile („die jüngste Scheibe unvollständig → die Kopien
laufen auseinander") ist **nicht prüfbar** — es gibt in diesen Scheiben nichts abzurufen. Warum die
Datenbank dort keine Nachrichten trägt, sagt M66 nicht.

**Befund 4 — die Rotationsgrenze liegt zwischen `2025-04` und `2025-12`.**
*Gemessen war:* `2024-10` und `2025-04` nennen ausschließlich `FILESTOREPROD07`/`08`, `2025-12`
ausschließlich `09`/`10`.
*Behauptet wird:* Der Wechsel liegt zwischen April und Dezember 2025. Das ist eine Beobachtung aus
der Stichprobe von M66, **keine eigene Messung** — wann genau umgeschaltet wurde und in welchem
Takt, bleibt offen (siehe „Offene Punkte" 11).

**Befund 5 — Nutzdaten sind größer als Protokolle, und die Ablagen unterscheiden sich darin.**
*Gemessen war:* In `2025-12` je Datei im Mittel — `FILESTOREPROD09` Nutzdaten 16.149 B, Protokolle
1.405 B; `FILESTOREPROD10` Nutzdaten 274 B, Protokolle 987 B. Alle Werte sind **gepackte**
ZIP-Größen.
*Behauptet wird:* nichts über die entpackten Größen und nichts über die Verteilung — dafür ist
**M60** zuständig, und die ist nicht gefahren. Die Zahlen stehen hier, weil sie beim Zählen ohnehin
anfielen.

> **Grenze des Befundes nach L10.**
> *Gemessen war:* die Deckung für **eine** von fünf beauftragten Zeitscheiben, mit 25 Nachrichten
> und 96 Verweisen, auf zwei Ablagen.
> *Behauptet wird ausdrücklich **nichts** über eine Aufbewahrungsfrist des Filestores.* Die
> Messung, die sie beantworten würde, verlangt eine erreichbare Ablage aus dem alten Zeitraum —
> `FILESTOREPROD07` und `08` sind es nicht.

**Löschung.** Die 96 geholten Dateien enthalten produktive Nutzdaten (G1 Stufe 2). Sie sind zu
keinem Zeitpunkt geöffnet oder ausgegeben worden; das Programm hat je Datei die Bytegröße und die
Zahl der ZIP-Einträge gezählt und **keinen Eintrag geöffnet**. Jede Datei ist **unmittelbar nach dem
Zählen** gelöscht worden; das Zielverzeichnis war nach dem Lauf leer (`true`). Das
Arbeitsverzeichnis mit der Stichprobenliste (1 Datei, 47.748 Byte) ist am **17.08.2026 um 15:19:30**
gelöscht worden, Löschung geprüft.

> ### Nachtrag vom 17.08.2026 — die Ursache der 197 Fehlversuche
>
> Der Fehlschlag der Zeitscheiben `2024-10` und `2025-04` geht auf **abgeschaltete Ablagen**
> zurück: **`FILESTOREPROD07` und `FILESTOREPROD08` sind aus**, Auskunft des Auftraggebers vom
> 17.08.2026. Von den elf Ablagen laufen **nur `FILESTOREPROD09` und `FILESTOREPROD10`**.
>
> **V4 — die Aufbewahrungsfrist im Filestore — bleibt damit unbeantwortet**, und zwar nicht mangels
> Daten, sondern weil die zuständigen Ablagen nicht antworten. **Die daraus vorgesehene Annahme
> entsteht nicht.** Messbar wird die Frage erst, wenn `07`/`08` laufen, oder sie ist bei der
> Betreuung des Altsystems zu erfragen.
>
> **Warum dieser Nachtrag hier steht und nicht in einer Fußnote:** Die Tabelle oben zeigt 197
> Fehlversuche. Ohne diesen Satz liest das in vier Wochen jemand als „ältere Nachrichten sind nicht
> abrufbar". Das stimmt heute — aber es hat eine andere Ursache und eine andere Folge: Ein
> eingeschalteter Filestore löst es auf, eine abgelaufene Aufbewahrungsfrist nicht.
>
> **Die Zahlen oben sind unverändert.** Es ist nichts nachgemessen und nichts korrigiert worden.

**Nicht gefahren.** M60, M61, M63, M64, M65, M67 und M68 blieben zunächst offen; M66 ist allein
gefahren worden. Sie sind am selben Tag nachgezogen worden — siehe unten.

---

## M66 (2) — die nachgeholte jüngste Zeitscheibe

**Warum eine eigene Messung und keine Korrektur an M66.** Die jüngste Scheibe aus M66 war leer,
weil sie außerhalb des tatsächlichen Bestandsendes lag. M66 ist gefahren; eine nachträglich
veränderte Scheibe **darin** würde die Datei über ihre eigene Geschichte täuschen. Die Zahlen von
M66 sind unverändert.

**Frage.** Laufen Datenbankkopie und Filestore-Kopie am **jüngeren** Ende auseinander?

**Stichprobe.** Der tatsächlich letzte Tag mit Daten: **`2026-07-08`** (285 Nachrichten, sämtlich
zwischen `17:16:26` und `17:21:10`). **50 Nachrichten** — der Auftragsumfang; die Kürzung aus
Ergänzung 5 gilt nach Auskunft des Auftraggebers nicht mehr. Je Nachricht `Message.Payload.GUID`
und alle `*.Log.GUID` → **153 Verweise**. Skript `m66-2-auswahl.sql`.
**Übergangene Verweise: 0 von 153** — die Scheibe liegt vollständig auf `FILESTOREPROD09`/`10`.

**Ergebnis**

| Kennzahl | Wert |
|---|---|
| Verweise | 153 |
| Abrufe ohne Verbindungsfehler | **153** |
| Attribut `Response` | **153 × `Error (Skipped)`** |
| **Datei geliefert** | **0** |
| Dauer | 19.489 ms (127 ms je Verweis) |

**Befund 1 — am jüngeren Ende laufen die Kopien vollständig auseinander.**
*Gemessen war:* Die Ablagen sind erreichbar und antworten auf **alle 153** Verweise — aber mit
`Error (Skipped)` und **ohne** Anhang. **Null von 153** Dateien.
*Behauptet wird:* Die dritte vorregistrierte Zeile von M66 („die jüngste Scheibe unvollständig →
die Kopien laufen auseinander") ist eingetreten, und zwar **nicht unvollständig, sondern
vollständig leer**. Die Datenbankkopie kennt Nachrichten und Verweise, zu denen die
Filestore-Kopie keine Datei hat. **Für die Abnahme ist eine Nachricht zu wählen, die in beiden
liegt** — und das ist zu notieren, damit es niemand später für einen Fehler hält.

**Befund 2 — der Unterschied zu M66 ist ein anderer als der zu den alten Scheiben.**
*Gemessen war:* In M66 scheiterten 197 Verweise an **nicht erreichbaren** Ablagen (`Response`
ungesetzt). Hier antworten die Ablagen und melden ausdrücklich `Error (Skipped)`.
*Behauptet wird:* Zwei verschiedene Lagen, die in einer Ergebnistabelle gleich aussähen: „Ablage
aus" gegen „Ablage da, Datei nicht". **Nur die zweite ist eine Aussage über den Bestand des
Filestores.**

> **Grenze nach L10.** *Gemessen war:* der `2026-07-08` mit 50 Nachrichten.
> *Behauptet wird nicht,* dass der gesamte 2026er-Ausläufer ohne Dateien ist — `2026-06` ist
> **nicht** gemessen. Wo der Filestore-Bestand endet, ist offen.

---

## Die Rotationsgrenze *(Nachtrag zu M53 und M66, ohne eigene M-Nummer)*

Der Bericht zu M66 hatte sie angekündigt und nicht genannt. Sie ist keine eigene Messung — sie
präzisiert M53 Befund 1 und M66 Befund 4 und fällt aus denselben Statements ab. Skript
`rotation.sql`.

**Verfahren.** Je Monat die ersten 200 Nachrichten und die Ablage ihrer `Message.Payload.GUID`;
danach der Umschaltmonat tagesweise über den Vollbestand des Monats.

| Monat | genannte Ablagen |
|---|---|
| `2025-05` | `07` (170) · `08` (30) |
| `2025-06` | `07` (144) · `08` (56) |
| `2025-07` | `07` (200) |
| **`2025-08`** | **`09` (168) · `10` (32)** |
| `2025-09` bis `2025-11` | `09` / `10` |

Tagesweise im Juli 2025:

| Tag | genannte Ablagen |
|---|---|
| `2025-07-15` bis `2025-07-22` | ausschließlich `07` und `08` |
| **`2025-07-23`** | **`07` (6.398) · `08` (861) · `09` (1.372) · `10` (61)** |
| `2025-07-24` und später | ausschließlich `09` und `10` |

**Befund — die Grenze ist ein einziger Tag.**
*Gemessen war:* Der `2025-07-23` ist der **einzige** Tag, an dem alle vier Ablagen vorkommen. Davor
keine `09`/`10`, danach keine `07`/`08`.
*Behauptet wird:* Der Übergang ist **taggenau und scharf**, kein gleitender Wechsel. Ein
Überlapptag, dann ist die Rotation vollzogen.

**Was das für den heute abrufbaren Bestand heißt**

| | Nachrichten | Anteil |
|---|---|---|
| vor dem `2025-07-23` — auf `07`/`08`, **abgeschaltet** | **2.111.355** | **63,2 %** |
| ab dem `2025-07-23` — auf `09`/`10`, eingeschaltet | 1.230.164 | 36,8 % |
| gesamt | 3.341.519 | 100 % |

*Gemessen war:* 2.111.355 von 3.341.519 Nachrichten liegen vor der Rotationsgrenze.
*Behauptet wird:* **Bei 63,2 % des Bestands zeigt heute kein Abruf eine Datei** — nicht weil sie
fehlte, sondern weil die zuständige Ablage aus ist (M66-Nachtrag). Ob das so bleibt, ist eine
Betriebsentscheidung. **Ob die Dateien dort noch liegen, ist weiterhin ungemessen.**

---

## M60 — Größenverteilung echter Dateien

**Frage.** Welche Größen kommen tatsächlich vor? Stichprobe über beide Familien, mindestens zwei
Mandanten, mehrere `Service.Type`, mindestens 200 Artefakte gleichmäßig über die in M54 gefundenen
Namen. Nur `Content-Length`, kein Rumpf.

**Vorregistrierte Deutung.** Diese Verteilung setzt die Kappungsgrenze der Anzeige und die
Größenbegrenzung des Downloads — **abgeleitet, nicht gesetzt.** Stimmt `Content-Length` mit
`FileReader.FileProperty.Size` überein, ist belegt, dass jene Spalte Bytes zählt; erst dann darf
M56 (b) als Byteangabe gelesen werden.

**Gefahren am 17.08.2026.** Stichprobe: Fenster A, je `MessagePropertyName` bis zu 7 Verweise —
**206 Artefakte** über **32 Familien** (17 Nutzdaten, 15 Protokoll), `FILESTOREPROD09` 154,
`FILESTOREPROD10` 52. Skript `m60-auswahl.sql` / `messung.ps1`.
**Übergangene Verweise: 0 von 44.329** — Fenster A liegt vollständig auf den beiden
eingeschalteten Ablagen.

**Abruf:** 206 von 206 geliefert, 206 × `SUCCESS (RETRIEVE)`, kein Fehler, **keine Datei mit mehr
als einem ZIP-Eintrag**. Dauer **50.405 ms** kalt (244 ms je Verweis); ein zweiter Lauf derselben
Stichprobe brauchte 3.001 ms (14 ms je Verweis).

**Ergebnis — Größe des ersten ZIP-Eintrags, entpackt**

| Art | n | kleinster | Mittel | größter | > 64 KiB | > 1 MiB | > 10 MiB | ZIP-Summe |
|---|---|---|---|---|---|---|---|---|
| Nutzdaten | 110 | **2** | 35.321 | **609.995** | 12 | 0 | 0 | 1.735.886 |
| Protokoll | 96 | 648 | 7.785 | 145.743 | 1 | 0 | 0 | 133.058 |

Entpackt zusammen **4.632.686 Byte**, gepackt **1.868.944 Byte** — der Transport spart rund 60 %.

**Befund 1 — `FileReader.FileProperty.Size` zählt Bytes, und zwar exakt.**
*Gemessen war:* In **7 von 7** Fällen, in denen die Datenbank eine Größe führt, stimmt sie
**zeichengleich** mit der entpackten Bytegröße des ersten ZIP-Eintrags überein. Differenz überall 0.
*Behauptet wird:* Die vorregistrierte Zeile ist erfüllt — jene Spalte ist eine **Byteangabe**.
**Offener Punkt 6 ist damit beantwortet**, und M56 (b) darf als Byteangabe gelesen werden. Die
Stichprobe ist mit sieben Werten klein; sie ist aber in allen sieben exakt und nicht nur nahe.

**Befund 2 — die gemessene Verteilung ist deutlich zahmer als die Datenbank erwarten ließ.**
*Gemessen war:* Größter Wert **609.995 Byte**; **kein** Artefakt über 1 MiB, keines über 10 MiB.
M56 (b) hatte in Fenster B 81 Werte über 10 MiB und ein Maximum von 123.981.293 gemessen.
*Behauptet wird:* Die 206 Artefakte dieser Stichprobe sagen **nichts** über jene Ausreißer — sie
sind gleichmäßig über die Familien gezogen, nicht über die Größe. Eine Kappungsgrenze aus dieser
Verteilung abzuleiten wäre falsch; **M56 (b) bleibt die Quelle für den schlechtesten Fall**, und
jetzt belegt als Byteangabe.

**Befund 3 — es gibt Artefakte von 2 Byte.**
*Gemessen war:* Kleinster entpackter Eintrag **2 Byte** (Nutzdaten).
*Behauptet wird:* Der praktisch leere Nutzdatensatz kommt vor. Zusammen mit `kleinster = 0` aus
M56 (b) heißt das: Die Anzeige muss „leer" darstellen können, ohne es für einen Fehler zu halten.

> **Grenze nach L10.** *Gemessen war:* die Verteilung über 206 gleichmäßig über 32 Familien
> gezogene Artefakte eines Tages. *Behauptet wird nicht,* dass sie die Verteilung des Bestands
> abbildet — die Ziehung ist nach Familie gleichverteilt und damit **bewusst** nicht repräsentativ
> für die Häufigkeit. Der Mandantenbezug ist in der Stichprobe **nicht mitgeführt** worden; die im
> Auftrag genannte Bedingung „mindestens zwei Mandanten" ist damit nicht nachgewiesen, sondern nur
> über das Fenster (sechs Mandanten) plausibel.

---

## M61 — Zeichenkodierung

**Frage.** Für dieselbe Stichprobe, als Zählung: BOM vorhanden? Bytes über `0x7F`? Gültiges UTF-8?
Deklariert ein UNB-Segment einen EDIFACT-Zeichensatz (`UNOA`/`UNOB`/`UNOC`/`UNOY`)? Zeilenende
`CRLF` oder `LF`? **Getrennt für Nutzdaten und Protokolle.**

**Vorregistrierte Deutung.** Alles gültiges UTF-8 **und** keine Bytes über `0x7F` → die Anzeige ist
kodierungsfrei, der Punkt entfällt. Andernfalls braucht sie eine **benannte und sichtbare**
Kodierungsentscheidung samt Umschalter. Still auf UTF-8 zu raten zeigt falsche Umlaute, als wären
sie richtig — der Q4-Fall in seiner unangenehmsten Form: Es sieht nicht kaputt aus.

**Gefahren am 17.08.2026** über dieselbe Stichprobe wie M60 (206 Artefakte, 110 Nutzdaten, 96
Protokolle). Ausgewertet wird der **erste ZIP-Eintrag** (Q4).

**Ergebnis 1 — binär gegen Text, vor jeder Kodierungsfrage**

| Art | n | binär | mit Nullbytes |
|---|---|---|---|
| **Nutzdaten** | 110 | **20 (18,2 %)** | 12 |
| Protokoll | 96 | **0 (0,0 %)** | 0 |

Die 20 binären Nutzdateien verteilen sich auf `MailReader.Payload.GUID` (7),
`SSHReader.Payload.GUID` (5), `OFTPSender.Payload.GUID` (4), `OFTPReader.Payload.GUID` (2),
`OFTP2Sender.Payload.GUID` (2).

**Ergebnis 2 — Kodierung, und zwar nur dort, wo sie überhaupt entscheidbar ist**

| Art | n | reines ASCII *(entscheidet nichts)* | mit Bytes über `0x7F` | davon gültiges UTF-8 | davon **nicht** |
|---|---|---|---|---|---|
| Nutzdaten | 110 | 74 | 36 | 8 | **28** |
| Protokoll | 96 | 88 | 8 | **0** | **8** |

Nach Abzug der binären Dateien — über die eine Kodierungsaussage sinnlos wäre:

| Art | nicht binär **und** entscheidbar | gültiges UTF-8 | **nicht** |
|---|---|---|---|
| Nutzdaten | 16 | 7 | **9** |
| Protokoll | 8 | **0** | **8** |

Von den 28 nicht-UTF-8-Nutzdateien sind **19 binär** und **9 Text**.

**Ergebnis 3 — Zeilenenden, BOM, EDIFACT-Zeichensatz** (über alle 206)

| Zeilenende | LF 95 · CRLF 38 · **gemischt 37** · keines 36 |
|---|---|
| mit BOM | **1** |
| UNB-Zeichensatz | `UNOC` 26 · `UNOA` 3 · `UNOY` 1 · keiner 176 |

**Befund 1 — nicht jede Nutzdatei ist Text, und der Auftrag kennt diesen Fall nicht.**
*Gemessen war:* **18,2 %** der Nutzdateien sind binär, 12 davon mit Nullbytes; bei den Protokollen
**keine einzige**.
*Behauptet wird:* Die Beobachtung des Auftraggebers — eine Nutzdatei erscheint im Altwerkzeug als
unlesbare Zeichenfolge — ist bestätigt und beziffert. Eine Anzeige, die Nutzdaten als Text
darstellt, trifft in rund jedem fünften Fall auf etwas, das keiner ist. **Was daraus folgt, ist
nicht entschieden.** *Was die Dateien sind, sagt die Messung nicht* — „vermutlich IDOC" ist eine
Vermutung des Auftraggebers und **nicht gemessen**.

**Befund 2 — die Anzeige ist nicht kodierungsfrei.**
*Gemessen war:* 44 der 206 Dateien tragen Bytes über `0x7F`. Von den entscheidbaren, nicht-binären
Dateien sind **8 von 8** Protokollen und **9 von 16** Nutzdateien **kein** gültiges UTF-8.
*Behauptet wird:* Die vorregistrierte Zeile „alles gültiges UTF-8 **und** keine Bytes über `0x7F`
→ die Anzeige ist kodierungsfrei" ist **nicht** eingetreten. Es braucht eine benannte und sichtbare
Kodierungsentscheidung. Der Befund deckt sich mit Q4: Das Altsystem dekodiert hart mit
`ISO-8859-1` (`JsonServlet.java:812`–`:813`), und `setCharacterEncoding("UTF-8")` steht dort
auskommentiert (`:808`).

**Befund 3 — die Quote über alle Dateien wäre irreführend gewesen.**
*Gemessen war:* **162 von 206** Dateien sind reines ASCII und damit zugleich gültiges UTF-8 **und**
gültiges ISO-8859-1.
*Behauptet wird:* Eine Angabe wie „96 % gültiges UTF-8" wäre richtig gerechnet und falsch gelesen —
sie entstünde fast vollständig aus Dateien, die gar nichts entscheiden. Deshalb ist die Quote hier
**nur über die 44 entscheidbaren** ausgewiesen. M71 hatte an einer einzigen Datei genau diesen
Fall: reines ASCII, entschied nichts.

**Befund 4 — die Zeilenenden sind uneinheitlich, und 37 Dateien mischen sie.**
*Gemessen war:* LF 95, CRLF 38, gemischt 37, keines 36.
*Behauptet wird:* Eine Anzeige, die auf ein bestimmtes Zeilenende baut, trifft auf alle vier Fälle.
Was daraus folgt, ist nicht entschieden.

---

## M63 — Markenbilanz je Familie

**Frage.** Über eine Stichprobe von Protokollen aus **jeder** in M54 gefundenen
`*.Log.GUID`-Familie, mindestens 30 je Familie sofern vorhanden. Gezählt wird je Datei: Vorkommen
`***StartOfLog***` (0/1/mehr), Vorkommen `***EndOfLog***` (0/1/mehr), Paar vollständig und in
richtiger Reihenfolge, Marke mit abweichenden Leerzeichen oder Groß-/Kleinschreibung, Marke in einer
erkennbar **echoten** Zeile (Zeile enthält zusätzlich `=` oder `Message.`).

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Genau ein Paar in **allen** Familien | Der Beschnitt ist verlässlich. Er bleibt trotzdem *fail closed*, weil die Marken beeinflussbar sind |
| Eine Familie ohne Marken | Für sie gibt es keinen Beschnitt. Dann ist ihr Protokoll für `MANDANT` **gesperrt**, nicht „ungeschnitten sichtbar" |
| Mehr als ein Paar | Die Regel „erste Start- bis nächste Endmarke" liefert nur einen Ausschnitt. Ob das genügt, entscheidet der Auftraggeber — **nicht** durch Aneinanderhängen aller Abschnitte |
| Abweichende Schreibweisen | Ein exakter Vergleich scheitert still. Die Erkennung braucht dann eine benannte Normalisierung, und die gehört gemessen |

**Gefahren am 17.08.2026.** Stichprobe: Fenster B, je `*.Log.GUID`-Familie bis zu 30 Protokolle —
**454 Dateien** über **alle 16 Familien**. Fenster B statt A, weil A vier Familien mit weniger als
30 Zeilen trägt. `DBReader.Log.GUID` hat auch in B nur **4** — der im Auftrag mitgedachte Fall
„sofern vorhanden". Skript `m63-auswahl.sql`.
**Übergangene Verweise: 0 von 636.848.**

**Abruf:** 454 von 454 geliefert, 454 × `SUCCESS (RETRIEVE)`, **keine Datei mit mehr als einem
ZIP-Eintrag**, **keine binär**. Dauer **94.355 ms** (207 ms je Verweis).

**Ergebnis — Markenbilanz je Familie**

| Familie | n | genau 1 Startmarke | 0 Startmarken | genau 1 Endmarke | Paar vollständig | abweichende Schreibweise | Marke in echoter Zeile |
|---|---|---|---|---|---|---|---|
| `AS2Reader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `AS2Sender.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `Converter.Log.GUID` | 30 | 29 | **1** | 29 | 29 | 0 | 0 |
| `DBReader.Log.GUID` | **4** | 4 | 0 | 4 | **4** | 0 | 0 |
| `FileReader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `FTPReader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| **`FTPSender.Log.GUID`** | 30 | 2 | **28** | 2 | **2** | 0 | 0 |
| `HTTPReader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| **`HTTPSender.Log.GUID`** | 30 | **0** | **30** | 0 | **0** | 0 | 0 |
| `MailReader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `OFTP2Reader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `OFTP2Sender.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `OFTPReader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `OFTPSender.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `SAPReader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| `SSHReader.Log.GUID` | 30 | 30 | 0 | 30 | **30** | 0 | 0 |
| **Summe** | **454** | 395 | **59** | 395 | **395** | **0** | **0** |

**Befund 1 — kein Protokoll hat mehr als ein Markenpaar, und keines eine Startmarke ohne Endmarke.**
*Gemessen war:* Wo eine Startmarke steht, steht **genau eine**, und wo eine steht, folgt **genau
eine** Endmarke. 395 von 395 mit Startmarke sind vollständig gepaart. **Null** Fälle „Start ohne
Ende", **null** abweichende Schreibweisen, **null** Marken in echoten Zeilen.
*Behauptet wird:* Wo der Beschnitt greift, greift er verlässlich. **Ein exakter Vergleich genügt** —
eine Normalisierung ist in dieser Stichprobe nicht nötig. Die Regel bleibt trotzdem *fail closed*,
weil die Marken nach Q2 mit `contains` erkannt werden und der Protokollinhalt beeinflussbar ist;
gemessen ist die Häufigkeit null, nicht die Unmöglichkeit.

**Befund 2 — zwei Familien haben keine Marken, und eine davon durchgängig.**
*Gemessen war:* `HTTPSender.Log.GUID` **30 von 30 ohne Startmarke**, `FTPSender.Log.GUID` **28 von
30 ohne**, `Converter.Log.GUID` 1 von 30 ohne.
*Behauptet wird:* Die vorregistrierte Zeile „eine Familie ohne Marken → für sie gibt es keinen
Beschnitt, ihr Protokoll ist für `MANDANT` **gesperrt**, nicht ungeschnitten sichtbar" ist
**eingetreten**, und zwar für `HTTPSender` vollständig. `FTPSender` ist der unangenehmere Fall:
Dort hängt es an der einzelnen Datei, ob überhaupt etwas anzuzeigen wäre. **Was daraus folgt, ist
nicht entschieden.**

> **Grenze nach L10.** *Gemessen war:* 30 Protokolle je Familie aus **einem** Monatsfenster.
> *Behauptet wird nicht,* dass `HTTPSender` nie Marken schreibt — gemessen sind 30 von 30 ohne, in
> Fenster B.

---

## M65 — Was steht innerhalb, was außerhalb? *(ersetzt M62)*

**Frage.** Über dieselbe Stichprobe wie M63, klassifizierend gezählt, **je Bereich getrennt** — vor
der ersten Startmarke, zwischen den Marken, nach der Endmarke. Klassen: absolute Pfade (`/opt/`,
`/var/`, `/usr/` oder vergleichbar), Dienstkennungen (`[A-Z]+PROD[0-9]+`), Hostnamen/Knoten
(punktgetrennter Bezeichner ohne Dateiendung), aus der Nachricht echote Werte (Zeile beginnt mit
`Message.`), UUIDs (kanonische Form), Zeitstempel (`Program start` / `Program end`).
**Ausgegeben werden ausschließlich Zählungen je Klasse und Bereich. Kein Inhalt.**

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Pfade und Dienstkennungen fast nur außerhalb | Die Marken taugen als **Lesbarkeitsregel** — sie entfernen genau das Beiwerk, das der Leitsatz ohnehin wegwünscht. Die Regel wie beschrieben ist baubar |
| Nennenswert viele Pfade **innerhalb** | Der Beschnitt hält weniger zurück als gedacht. Er bleibt eine Lesbarkeitsregel, aber die Beschreibung „Mandantentrennung" wäre dann irreführend und gehört korrigiert |
| `Message.`-Zeilen innerhalb | **Erwartet** (im Beispiel drei). Bestätigt die Beeinflussbarkeit und damit die konservative Schnittregel |
| Etwas **außerhalb**, dessen Offenlegung schadet | Dann ist der Beschnitt keine Milderung, sondern eine Notwendigkeit — und der Download muss für `MANDANT` dieselbe beschnittene Fassung liefern oder gesperrt sein |

**Gefahren am 17.08.2026** über dieselbe Stichprobe wie M63, ausgewertet über die **395 Dateien mit
vollständigem Markenpaar** — nur dort gibt es die drei Bereiche. Gezählt wird je **Zeile**: Eine
Zeile zählt einmal je Klasse, die sie erfüllt.

**Ergebnis — Zeilen je Klasse und Bereich**

| Bereich | Zeilen | absolute Pfade | Dienstkennungen | Hostnamen | UUIDs | `Message.`-Zeilen |
|---|---|---|---|---|---|---|
| **vor** der Startmarke | 715 | 252 | 93 | 0 | 1 | 0 |
| **innerhalb** | **29.381** | **808** | **331** | **11.080** | **1.070** | **714** |
| **nach** der Endmarke | 3.069 | 414 | 0 | 519 | 312 | 0 |

Je Datei betrachtet: **334 von 395** tragen mindestens einen absoluten Pfad **innerhalb**,
**61 von 395** mindestens eine Dienstkennung innerhalb, **180 von 395** mindestens eine
`Message.`-Zeile innerhalb.

**Befund 1 — Pfade und Dienstkennungen stehen überwiegend *innerhalb*, nicht außerhalb.**
*Gemessen war:* 808 Pfadzeilen innerhalb gegen 252 davor und 414 danach; 331 Dienstkennungen
innerhalb gegen 93 davor und **0** danach. 334 von 395 Dateien haben einen Pfad im Innenbereich.
*Behauptet wird:* Die erhoffte Zeile — „Pfade und Dienstkennungen fast nur außerhalb → die Marken
taugen als Lesbarkeitsregel" — ist **nicht** eingetreten. Es gilt die zweite: **Der Beschnitt hält
weniger zurück als gedacht.** Er bleibt eine Lesbarkeitsregel; **die Beschreibung
„Mandantentrennung" wäre irreführend und gehört korrigiert.** Wo genau, entscheidet der
Auftraggeber.

**Befund 2 — aus der Nachricht echote Werte stehen erwartungsgemäß innerhalb.**
*Gemessen war:* **714** `Message.`-Zeilen innerhalb, **0** davor und **0** danach; sie kommen in
180 der 395 Dateien vor.
*Behauptet wird:* Die vorregistrierte Zeile „`Message.`-Zeilen innerhalb — **erwartet**" ist
bestätigt und beziffert. Der Protokollinhalt ist von außen beeinflussbar, und damit sind es auch
die Marken. Das ist die Begründung für die konservative Schnittregel, und sie steht jetzt auf
gemessenen Zahlen statt auf einem Beispielbild.

**Befund 3 — Hostnamen gibt es *nur* innerhalb und danach, nie davor.**
*Gemessen war:* 11.080 Zeilen mit Hostnamen innerhalb, 519 danach, **0** davor.
*Behauptet wird:* wenig. **Diese Klasse ist die unschärfste der Messung** — erkannt wird ein
punktgetrennter Bezeichner, dessen letztes Glied keine bekannte Dateiendung ist; dieselbe Zeile
kann dabei aus anderen Gründen treffen. Die Zahl steht hier vollständigkeitshalber und trägt keinen
Schluss.

**Befund 4 — der Bereich *nach* der Endmarke ist klein und enthält keine Dienstkennungen.**
*Gemessen war:* 3.069 Zeilen nach der Endmarke, davon 414 mit Pfad, **0** mit Dienstkennung.
*Behauptet wird:* Der vierte vorregistrierte Fall — „etwas außerhalb, dessen Offenlegung schadet" —
lässt sich mit einer Zählung **nicht** entscheiden. Ob 414 Pfadzeilen außerhalb schaden, ist eine
Bewertung und keine Messung; sie steht unter „Offene Punkte".

---

## M67 — Wie viel nimmt der Beschnitt weg?

**Frage.** Über die Stichprobe aus M63: Größe der ganzen Datei gegen Größe des Innenbereichs, in
Bytes und Zeilen, je Familie.

**Vorregistrierte Deutung.** Bleibt vom Protokoll nach dem Beschnitt nur ein Bruchteil, ist die
Anzeige für `MANDANT` kurz und ein Ausklapp-Block im Detail genügt. Ist der Innenbereich der
überwiegende Teil, braucht auch die Mandantensicht die volle Anzeigefläche — und die Frage nach
Panel gegen eigene Route stellt sich für **beide** Rollen gleich.

**Gefahren am 17.08.2026** über dieselbe Stichprobe wie M63, ausgewertet über die 395 Dateien mit
vollständigem Markenpaar.

**Ergebnis — Anteil des Innenbereichs je Familie**

| Familie | n | Bytes gesamt | Bytes innen | Anteil | Zeilen gesamt | Zeilen innen | Anteil |
|---|---|---|---|---|---|---|---|
| `AS2Reader.Log.GUID` | 30 | 117.571 | 116.551 | **99,1 %** | 1.024 | 934 | 91,2 % |
| `AS2Sender.Log.GUID` | 30 | 116.077 | 114.989 | **99,1 %** | 964 | 874 | 90,7 % |
| `Converter.Log.GUID` | 29 | 136.030 | 99.713 | 73,3 % | 2.696 | 1.569 | 58,2 % |
| **`DBReader.Log.GUID`** | 4 | 6.154 | 572 | **9,3 %** | 184 | 28 | 15,2 % |
| **`FileReader.Log.GUID`** | 30 | 65.007 | 19.629 | **30,2 %** | 1.691 | 461 | 27,3 % |
| `FTPReader.Log.GUID` | 30 | 1.206.957 | 1.158.334 | 96,0 % | 14.695 | 13.422 | 91,3 % |
| `FTPSender.Log.GUID` | 2 | 83.111 | 82.653 | 99,4 % | 400 | 392 | 98,0 % |
| `HTTPReader.Log.GUID` | 30 | 25.466 | 23.914 | 93,9 % | 622 | 562 | 90,4 % |
| `MailReader.Log.GUID` | 30 | 32.573 | 30.930 | 95,0 % | 713 | 653 | 91,6 % |
| `OFTP2Reader.Log.GUID` | 30 | 122.843 | 121.823 | 99,2 % | 1.610 | 1.520 | 94,4 % |
| `OFTP2Sender.Log.GUID` | 30 | 72.899 | 71.909 | 98,6 % | 916 | 826 | 90,2 % |
| `OFTPReader.Log.GUID` | 30 | 27.614 | 25.994 | 94,1 % | 690 | 630 | 91,3 % |
| `OFTPSender.Log.GUID` | 30 | 1.280.434 | 1.273.135 | 99,4 % | 6.429 | 6.309 | 98,1 % |
| `SAPReader.Log.GUID` | 30 | 27.896 | 26.276 | 94,2 % | 690 | 630 | 91,3 % |
| `SSHReader.Log.GUID` | 30 | 25.320 | 23.759 | 93,8 % | 631 | 571 | 90,5 % |
| **alle 454 Dateien** | 454 | **3.527.951** | **3.190.181** | **90,4 %** | — | — | — |

**Befund 1 — der Beschnitt nimmt wenig weg; der Innenbereich ist der überwiegende Teil.**
*Gemessen war:* Über alle Dateien bleiben **90,4 %** der Bytes im Innenbereich. In 11 von 15
Familien mit Paar liegt der Anteil über 93 %.
*Behauptet wird:* Die vorregistrierte Zeile „ist der Innenbereich der überwiegende Teil, braucht
auch die Mandantensicht die volle Anzeigefläche" ist eingetreten. **Die Frage Panel gegen eigene
Route stellt sich für beide Rollen gleich** — der Beschnitt verkürzt die Anzeige nicht nennenswert.
Ein Ausklapp-Block im Detail genügt **nicht** deshalb, weil der beschnittene Teil kurz wäre.

**Befund 2 — zwei Familien fallen deutlich aus der Reihe.**
*Gemessen war:* `DBReader.Log.GUID` **9,3 %** und `FileReader.Log.GUID` **30,2 %** — dort nimmt der
Beschnitt den größten Teil weg. `DBReader` beruht allerdings auf **vier** Dateien.
*Behauptet wird:* Der Anteil ist **familienabhängig** und schwankt zwischen 9,3 % und 99,4 %. Eine
einzige Zahl für „den Beschnitt" gibt es nicht; wer die Anzeige darauf auslegt, legt sie auf einen
Mittelwert aus, den es je Familie nicht gibt.

---

## M64 — Markenbilanz bei Fehlernachrichten

**Frage.** Dieselben Kennzahlen wie M63, aber über Protokolle von Nachrichten mit `ERROR_*` oder
`COMMIT_REJECTED`. Umfang: alles, was M64a findet, bis maximal 200 Dateien.

**Vorregistrierte Deutung.** Ist die Paarquote bei Fehlernachrichten deutlich niedriger als in M63,
braucht der Fall eine eigene Behandlung — entweder eine benannte Ersatzausgabe („Der Lauf ist
abgebrochen; der lesbare Teil ist unvollständig" mit dem Teil ab der Startmarke) oder die Freigabe
des vollständigen Protokolls für diesen Fall. Beides ist eine Entscheidung, keine Messung.

**Gefahren am 17.08.2026.** Stichprobe: die Protokolle **aller** Fehlernachrichten aus Fenster B,
die M64a gefunden hat (`ERROR_*` und `COMMIT_REJECTED`, Fehlerprüfung über `LIKE 'ERROR\_%' ESCAPE`
nach Regel 9) — **33 Protokolle** zu 11 Nachrichten. Skript `m64-auswahl.sql`.
**Übergangene Verweise: 0 von 33** — alle elf Fehlernachrichten liegen auf `FILESTOREPROD09`/`10`.
Die im Auftrag genannte Obergrenze von 200 Dateien wird nicht erreicht; der Grund steht in M64a
Befund 2.

**Abruf:** 33 von 33 geliefert, 33 × `SUCCESS (RETRIEVE)`, keine Datei mit mehr als einem
ZIP-Eintrag, keine binär, alle reines ASCII. Dauer **1.366 ms** (41 ms je Verweis).

**Ergebnis**

| Kennzahl | Wert |
|---|---|
| Protokolle | 33 |
| **Paar vollständig** | **33 von 33 (100 %)** |
| ohne Startmarke | **0** |
| Startmarke ohne Endmarke | **0** |
| abweichende Schreibweise | 0 |
| Marke in echoter Zeile | 0 |
| Bytes gesamt / innen | 127.803 / 90.953 (**71,2 %**) |

**Befund 1 — der befürchtete Fall tritt nicht ein.**
*Gemessen war:* **33 von 33** Fehlerprotokollen haben ein vollständiges Markenpaar. Kein einziges
hat eine Startmarke ohne Endmarke.
*Behauptet wird:* Die Sorge des Auftrags — „ein abgebrochener Lauf schreibt plausibel eine
Startmarke und keine Endmarke, und bei *fail closed* sähe ein Mandantennutzer dann nichts" — ist in
dieser Stichprobe **nicht** eingetreten. Die Paarquote liegt bei **100 %** und damit **über** der
von M63 (395 von 454 = 87,0 %). Eine eigene Behandlung des Falls ist nach dieser Messung **nicht**
nötig; ob sie trotzdem gebaut wird, ist eine Entscheidung.

**Befund 2 — die Stichprobe ist klein, aber sie ist der Vollbestand.**
*Gemessen war:* 11 Fehlernachrichten, 33 Protokolle — alles, was Fenster B trägt.
*Behauptet wird:* Es ist keine Auswahl, sondern alles Vorhandene. Der Auftrag setzt „liegt die Zahl
unter drei, ist die Messung nicht aussagekräftig" — mit 33 Protokollen zu 11 Nachrichten ist diese
Schwelle überschritten. **Aussagekräftig ist sie damit für Fenster B**, nicht für den Bestand:
`messungen-schritt5.md` (Z. 812–826) zeigt, dass die 49 `ERROR_TIMEOUT` des Dezembers vier Stunden
nach dem Fensterende liegen.

> **Grenze nach L10.** *Gemessen war:* die Markenbilanz von 33 Fehlerprotokollen aus einem
> Monatsfenster. *Behauptet wird nicht,* dass ein abgebrochener Lauf nie eine Endmarke schuldig
> bleibt — gemessen ist, dass es in diesen 33 nicht vorkommt.

---

## Warum Teil B nach M59 endet

**M59 ist in beiden Teilen vollständig gefahren und beantwortet die Frage, die sie stellt.** Die
Antwort ist, dass **weder `GET` noch die freigegebene `RETRIEVE`-Form** eine Datei liefert: Beide
ergeben dieselben 185 Byte mit leerer `FileList`. Damit fehlt allen übrigen Messungen von Teil B
ihre Grundlage — sie brauchen sämtlich eine geholte Datei:

| Messung | Was sie braucht | Warum es nicht geht |
|---|---|---|
| **M68** Kreuzabruf | Statuscode und `Content-Length` derselben Kennung an der anderen Ablage | Jede Anfrage ergibt `200 / 185` — der Kreuzabruf sähe „durchgehend gespiegelt" aus, ohne eine Datei berührt zu haben |
| **M66** Deckungsgleichheit | Existiert die Datei zu einer Kennung? | Jede Kennung ergibt `200 / 185`. „Existiert" und „existiert nicht" sind ununterscheidbar |
| **M60** Größenverteilung | `Content-Length` echter Dateien | Immer 185, unabhängig von der Kennung |
| **M61** Kodierung | die Bytes der Datei | keine Datei abrufbar |
| **M63** Markenbilanz | der Inhalt von Protokollen | keine Datei abrufbar |
| **M65** Inhaltsklassen | dasselbe | keine Datei abrufbar |
| **M67** Größe des Beschnitts | dasselbe | keine Datei abrufbar |
| **M64** Fehlerprotokolle | dasselbe, für die elf Nachrichten aus M64a | keine Datei abrufbar |

**Sie sind deshalb nicht gefahren, und ihre Nummern bleiben vergeben.** Ein Lauf, der die immer
gleichen 185 Byte gezählt hätte, hätte Tabellen gefüllt, die plausibel aussehen und nichts über den
Filestore aussagen. Das ist der teuerste Fehler, den diese Runde machen könnte, weil er von außen
nicht zu sehen ist.

> ### Nachtrag vom 17.08.2026 — dieser Abschnitt ist überholt
>
> Er beschreibt den Stand **vor M71**. Nachdem M71 belegt hat, dass der Abruf mit dem echten
> Client trägt, sind **alle** hier als nicht fahrbar geführten Messungen am selben Tag gefahren
> worden: **M60, M61, M63, M64, M65, M66, M66 (2), M67 und M68.** Der Abschnitt bleibt stehen,
> weil er die damalige Begründung festhält; die Ergebnisse stehen bei den Messungen selbst.
>
> **Was von ihm gültig bleibt:** Der `GET`-Weg aus M59 (1) beantwortet keine dieser Fragen, und ein
> Lauf über ihn hätte Tabellen gefüllt, die nichts aussagen.

### Der Stand nach der Freigabe vom 17.08.2026

Der Auftraggeber hat noch am selben Tag **genau eine POST-Form freigegeben** (Auftrag, „Ergänzung
17.08.2026"). Daraufhin ist geschehen, was die Freigabe verlangt:

| Schritt | Stand |
|---|---|
| Operation, Namensraum und Envelope aus dem Altcode bestimmt | ✔ vollständig, jede Eigenschaft belegt — siehe „Der freigegebene Envelope" |
| Envelope **vor der ersten Ausführung** im Volltext aufgenommen | ✔ |
| Geprüft, dass kein Feld ungeklärt bleibt | ✔ — das einzige offene war das Format von `ID`; QT1 belegt „die GUID hinter der Pipe" |
| Geprüft, dass `RETRIEVE` nichts ablegen kann | ✔ — der `AttachmentPart` entsteht nur im Zweig `CREATE` (`FilestoreClient.java:78–90`) |
| Stichprobe deterministisch erneut beschafft | ✔ — dieselben zwanzig Kennungen wie im ersten Anlauf |
| **Die Anfrage abgesetzt** | ✔ — nach Aufnahme einer Werkzeugregel für `curl.exe` am 17.08.2026 |
| **Eine Datei geholt** | ✖ — der Empfänger liefert auf `RETRIEVE` dieselbe leere `FileList` wie auf ein blankes `GET` |

Der erste Versuch war noch an der **Berechtigungsschicht der Entwicklungsumgebung** gescheitert
(nicht am Auftrag und nicht am Filestore). Es ist dabei **kein Umweg gesucht worden** — weder
`Invoke-WebRequest` (vom Auftrag ausgeschlossen) noch ein Browser (von §7 ausgeschlossen) noch eine
selbst gesetzte Berechtigung. Nach Aufnahme der Regel durch den Auftraggeber ist der Aufruf
durchgegangen; das Ergebnis steht in **M59 (2)**.

---

## M59 (2) — Der Abruf über die freigegebene POST-Form

**Frage.** Liefert der freigegebene `RETRIEVE`-Envelope eine Datei?

**Ausgeführte Anfragen.** **Zwölf**, sequenziell: ein erster `RETRIEVE` gegen `FILESTOREPROD09`, ein
`GET` gegen dieselbe Adresse als Vergleichsabzug, dann **zehn `RETRIEVE`** — fünf mit Kennungen, die
`FILESTOREPROD09` nennen, fünf mit Kennungen, die `FILESTOREPROD10` nennen, **jede bei der Ablage,
die ihr Verweis nennt** (Holregel aus M68). Alle GUIDs stammen aus der Stichprobe dieser Messung.
Der Envelope ist in allen zwölf Anfragen **unverändert** bis auf die eingesetzte GUID.

**Ergebnis**

| Ablage | Familie der Kennung | Statuscode | `Content-Type` | Länge | byteidentisch mit der `GET`-Antwort |
|---|---|---|---|---|---|
| `FILESTOREPROD09` | `FileReader.Payload.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD09` | `Converter.Log.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD09` | `FileReader.Payload.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD09` | `Converter.Payload.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD09` | `FTPSender.Log.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD10` | `Converter.Log.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD10` | `Converter.Log.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD10` | `DataWarehouse.Payload.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD10` | `Converter.Log.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |
| `FILESTOREPROD10` | `Converter.Payload.GUID` | 200 | `text/xml;charset=utf-8` | 185 | **ja** |

**Klassifikation der Antwort** — ohne den Rumpf auszugeben:

| Prüfung | Ergebnis |
|---|---|
| enthält `Envelope` / `FileList` | ja / ja |
| enthält `<File` | **nein** |
| enthält `Counter=` / `ID=` / `Response=` | **nein / nein / nein** |
| enthält `Fault` | **nein** |
| Antwort `multipart/related` oder mit Anhang | **nein** |
| byteidentisch mit der Antwort auf ein blankes `GET` | **ja**, in allen zehn Fällen |

**Befund 1 — der freigegebene Envelope holt in dieser Form keine Datei.**
*Gemessen war:* Zehn `RETRIEVE`-Anfragen in der freigegebenen Form, über sechs Artefaktfamilien und
beide Ablagen, liefern `200` mit **denselben 185 Byte wie ein blankes `GET`** — leere `FileList`,
kein Anhang, kein `Fault`.
*Behauptet wird:* Der von Hand nachgebaute Envelope in **genau dieser Gestalt** bewirkt an **diesen
beiden Knoten** nichts. **Nicht behauptet wird**, dass der aus dem Altcode gelesene Vertrag falsch
ist — dazu sagt diese Messung nichts.

**Befund 2 — der Empfänger echot die Anfrage nicht zurück, und das grenzt die Ursache ein.**
*Gemessen war:* Die Antwort enthält **kein** `File`-Element, also auch nicht die gesendete `ID` mit
einem `Response`-Vermerk. `FilestoreClient.java:96–103` liest genau diese Attribute aus der Antwort;
sie kommen nicht.
*Behauptet wird:* Der Empfänger verarbeitet den gesendeten Rumpf **nicht** — hätte er ihn gelesen
und die Datei nicht gefunden, wäre ein `File`-Element mit `Response`-Vermerk zu erwarten gewesen,
und hätte er ihn nicht verstanden, ein `Fault`. Er antwortet stattdessen auf `POST` wie auf `GET`.
Ob das an der Gestalt des Rumpfes liegt, an den Kopfzeilen oder daran, dass der Empfänger einen
anderen Einstieg erwartet, ist **nicht gemessen**. **Achte Stelle, an der der Befund in keine
vorformulierte Zeile passt** — der Auftrag sieht für M59 „liefert eine Datei" und „liefert einen
Fehler" vor, nicht „antwortet, als wäre nichts gesendet worden".

**Warum hier nicht weiterprobiert worden ist.** Naheliegend wären mehrere Abwandlungen: die `ID` als
zusammengesetzten Verweis statt als bloße GUID, ein qualifiziertes `File`-Element, ein anderer
`SOAPAction`-Wert, SOAP 1.2 statt 1.1. **Jede davon ist durch die Freigabe ausdrücklich gesperrt**
(„jeder selbst gebaute Envelope, jede Abwandlung ‚zum Ausprobieren'"). Sie stehen als Vorschläge
unter „Offene Punkte" und sind nicht gefahren.

**Laufzeit.** Beste Antwortzeit **6,22 ms** (`FILESTOREPROD09`) und **6,39 ms** (`FILESTOREPROD10`).
Der `POST` ist damit so schnell wie das `GET` — ein weiteres Zeichen dafür, dass nichts gesucht
wurde.

> **Nachtrag vom 17.08.2026.** Die drei Annahmen, auf denen diese Messung beruht — Endpunkt,
> Operation und Kennungsform —, sind seither aus dem Quelltext **belegt** und waren **alle drei
> richtig** (siehe **Q1**). **Der Nullbefund bleibt damit unerklärt.** Die einzige verbliebene
> Differenz zum Altsystem ist die **Transportform**: SAAJ gegen einen von Hand gebauten Envelope.
> Die Ergebnistabelle oben bleibt unverändert. Was SAAJ tatsächlich sendet, misst **M70**.

---

## M70 — Was SAAJ tatsächlich auf die Leitung legt

**Frage.** Die einzige verbliebene Differenz zwischen dem Altsystem und M59 (2) ist die
Transportform. Endpunkt, Operation und Kennungsform sind über Q1 belegt und waren richtig. Was SAAJ
sendet, steht in **keinem** Quelltext — es ist eine Eigenschaft der Implementierung und nur zu
beobachten. Also: Was legt der vorhandene `FilestoreClient` auf die Leitung?

**Aufbau.** Der Alt-Client läuft **einmal** gegen einen Lauscher auf `127.0.0.1`, der die Rohbytes
mitschreibt und mit einem leeren SOAP-Umschlag antwortet.

> ⚠️ **Der Filestore ist in diesem Lauf nicht angesprochen worden.** Die Zieladresse zeigt
> ausschließlich auf `127.0.0.1`; der `ServiceConnectString` ist nicht gelesen, nicht aufgelöst und
> nicht verwendet worden. Der Lauscher bindet auf `127.0.0.1`, nicht auf `0.0.0.0`. Es gab keinen
> Rückfall und keine „Gegenprobe am echten Endpunkt".

| Angabe | Wert |
|---|---|
| Projekt | `scripts/mitschnitt-saaj/`, eigenes `pom.xml`, **nicht** in den Hauptbau eingehängt |
| Java | **21.0.11**, Eclipse Adoptium (Temurin-21.0.11+10) |
| SOAP-API | `javax.xml.soap:javax.xml.soap-api:1.4.0` — geladen aus `javax.xml.soap-api-1.4.0.jar` |
| SAAJ-Implementierung | `com.sun.xml.messaging.saaj:saaj-impl:**1.5.3**` — geladen aus `saaj-impl-1.5.3.jar`, `Implementation-Version 1.5.3` |
| Activation | `com.sun.activation:javax.activation:1.2.0` (dazu transitiv `com.sun.activation:jakarta.activation:1.2.2`) |
| transitiv zusätzlich | `jakarta.xml.soap:jakarta.xml.soap-api:1.4.2`, `org.jvnet.staxex:stax-ex:1.8.3` |
| Fabriken zur Laufzeit | `…saaj.soap.ver1_1.SOAPMessageFactory1_1Impl`, `…saaj.client.p2p.HttpSOAPConnectionFactory` |
| Läufe | **einer** |

**Der `javax`-Zweig ist bewusst gewählt.** Ein Wechsel auf `jakarta` hätte die `import`-Zeilen in
`FilestoreClient.java` erzwungen — und damit wäre der Lauf wieder ein Nachbau gewesen, nur einer,
dem man es weniger ansieht. Die Hauptversion entscheidet: `1.x` trägt das Paket `javax.xml.soap`,
erst `2.x` heißt `jakarta`. Beide geladenen Jars liegen im `1.x`-Zweig.

> **Zwei Jars liefern das Paket `javax.xml.soap`** — die ausdrücklich deklarierte
> `javax.xml.soap-api:1.4.0` und, transitiv über `saaj-impl`, die `jakarta.xml.soap-api:1.4.2`,
> deren Hauptversion `1.x` noch das `javax`-Paket trägt. Der Lauf gibt deshalb aus, **welches Jar
> tatsächlich geladen wurde**: `javax.xml.soap-api-1.4.0.jar`. Ohne diese Angabe wäre der Befund
> nicht wiederholbar.

**Die vier Alt-Dateien sind byteidentisch kopiert**, nicht verändert — keine Formatierung, keine
Importsortierung, kein ergänztes `@Override`. Geprüft über SHA-256 gegen die Originale unter
`alterCode/`:

| Datei | SHA-256 (Original = Kopie) |
|---|---|
| `FilestoreClient.java` | `d0f96d61eb5be2f4cdf33a71fbf9cbf10186e7637a7cb35239cfacf1c5c6109e` |
| `FilestoreFile.java` | `cc9f0400329f8a5ee4bc039eca7c595d630b4cf6508c528db20674e0afdeb2be` |
| `PayloadReader.java` | `9db3b8c098dfff47eda918fb67e9cb02af64613f30ef4113c7fd080b976d18af` |
| `PayloadWriter.java` | `5e4cdd3065b55a7d3ace86dcbc7c65040d38919c9e9be0c6efb3bef403d9ff7c` |

Die aufrufende `main` stellt die Kette aus `JsonServlet.java:793`–`:797` nach, in derselben
Reihenfolge und mit denselben Argumenten.

**Die GUID ist frei erfunden** — `deadbeef-0000-4000-8000-0123456789ab`. Sie kommt in keiner
Stichprobe vor und gehört zu keiner Nachricht. **Deshalb darf der Mitschnitt im Wortlaut hier
stehen:** Er enthält keinerlei echte Daten. `requestFilestoreActions()` lieferte `true`.

### Der Mitschnitt im Wortlaut

**579 Byte.** Kopfzeilen mit `CRLF`, Rumpf ohne jeden Zeilenumbruch. Umbrüche unten nur zur
Lesbarkeit; die Grenze zwischen Kopf und Rumpf ist die Leerzeile.

```http
POST /WebApplication/FileStoreSoapReceiver HTTP/1.1
Accept: text/xml, text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2
Content-Type: text/xml; charset=utf-8
Cache-Control: no-cache
Pragma: no-cache
User-Agent: Java/21.0.11
Host: 127.0.0.1:<port>
Connection: keep-alive
Content-Length: 276

<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"><SOAP-ENV:Body><m:FileList xmlns:m="http://filestore.kraftwerkone.de"><File Action="RETRIEVE" Counter="0" ID="deadbeef-0000-4000-8000-0123456789ab"/></m:FileList></SOAP-ENV:Body></SOAP-ENV:Envelope>
```

*(Der Port ist bei jedem Lauf ein anderer und hier als `<port>` gesetzt; im Mitschnitt steht die
Zahl. Es ist ein Loopback-Port, keine Adresse nach G1.)*

**Die Kopfzeilenreihenfolge ist die gesendete** und steht hier, weil sie zu dem gehört, was wir
gerade nicht wussten: `Accept`, `Content-Type`, `Cache-Control`, `Pragma`, `User-Agent`, `Host`,
`Connection`, `Content-Length`. **`Host` steht an sechster Stelle**, nicht an erster.

| Form | Befund |
|---|---|
| Länge | **feste Länge** über `Content-Length: 276`; **kein** `Transfer-Encoding: chunked` |
| Mehrteiligkeit | **keine MIME-Grenze**, kein `boundary` — einteilig. Passt zu `FilestoreClient.java:86`–`:87`: `RETRIEVE` erzeugt keinen `AttachmentPart` |
| Zeilenenden | Kopf `CRLF`; **im Rumpf keine**, er ist eine einzige Zeile |
| Kodierung, deklariert | ausschließlich über `Content-Type: text/xml; charset=utf-8`; **keine XML-Deklaration im Rumpf**, **kein BOM** |
| Kodierung, tatsächlich | reines ASCII, **0 Bytes über `0x7F`** |

### Gegenüberstellung zu M59 (2)

Was **gleich** ist: Verb, Pfad, HTTP-Version, der Wert von `Content-Type`, die Elementstruktur
(`m:FileList` qualifiziert, `File` unqualifiziert), dieselben drei Attribute mit denselben Werten.

Was **verschieden** ist — vollständig, auch das scheinbar Bedeutungslose:

| # | Merkmal | Altsystem (SAAJ) | M59 (2) (`curl`, Nachbau) |
|---|---|---|---|
| 1 | **`SOAPAction`** | **wird nicht gesendet** | `SOAPAction: ""` ausdrücklich gesetzt |
| 2 | `Accept` | `text/xml, text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2` | `*/*` |
| 3 | `Cache-Control` | `no-cache` | nicht gesendet |
| 4 | `Pragma` | `no-cache` | nicht gesendet |
| 5 | `Connection` | `keep-alive` | nicht gesendet |
| 6 | `User-Agent` | `Java/21.0.11` | `curl/8.19.0` |
| 7 | Kopfzeilenreihenfolge | `Accept`, `Content-Type`, `Cache-Control`, `Pragma`, `User-Agent`, `Host`, `Connection`, `Content-Length` | `Host`, `User-Agent`, `Accept`, `Content-Type`, `SOAPAction`, `Content-Length` |
| 8 | **BOM im Rumpf** | **keiner** | **`EF BB BF`** — `Set-Content -Encoding utf8` schreibt unter Windows PowerShell 5.1 eine BOM |
| 9 | **XML-Deklaration** | **keine** | `<?xml version="1.0" encoding="UTF-8"?>` |
| 10 | **Leerraum im Rumpf** | keiner, eine Zeile | 7 Zeilenumbrüche (`LF`) zwischen den Elementen |
| 11 | **Attributreihenfolge in `File`** | `Action`, `Counter`, `ID` — **alphabetisch** | `Counter`, `ID`, `Action` — Quelltextreihenfolge aus `FilestoreClient.java:74`–`:76` |
| 12 | `Content-Length` | 276 | 324 |

**Befund 1 — der geratene `Content-Type` war richtig, die geratene `SOAPAction` gibt es gar nicht.**
*Gemessen war:* SAAJ sendet `Content-Type: text/xml; charset=utf-8` — Zeichen für Zeichen der Wert,
den M59 (2) von Hand gesetzt hatte — und **keine** `SOAPAction`-Kopfzeile.
*Behauptet wird:* Die eine der beiden Annahmen aus der Korrektur bei „Der freigegebene Envelope" ist
bestätigt, die andere ist **falsch**: Es wurde eine Kopfzeile gesendet, die das Altsystem nicht
sendet.

**Befund 2 — die Attributreihenfolge ist alphabetisch, nicht Quelltextreihenfolge.**
*Gemessen war:* `Action`, `Counter`, `ID` — obwohl `FilestoreClient.java:74`–`:76` sie als
`Counter`, `ID`, `Action` hinzufügt.
*Behauptet wird:* Die Reihenfolge im `addAttribute`-Aufruf sagt **nichts** über die Reihenfolge auf
der Leitung; die Serialisierung ordnet um. Aus dem Quelltext allein wäre das nicht abzuleiten
gewesen.

**Befund 3 — der Rumpf trägt weder BOM noch XML-Deklaration.**
*Gemessen war:* 276 Byte reines ASCII, beginnend mit `<SOAP-ENV:Envelope`.
*Behauptet wird:* Der Rumpf aus M59 (2) begann mit drei Bytes, die das Altsystem nie sendet. Ob ein
Empfänger daran scheitert, ist **nicht gemessen**.

**Befund 4 — eine Beobachtung, ausdrücklich ohne Schluss.**
*Gemessen war:* Die `Accept`-Kopfzeile, die SAAJ **sendet**, ist zeichengleich mit der
`Accept`-Kopfzeile, die beide Filestores in M59 (1) in ihren Antworten **zurückgaben**.
*Behauptet wird:* nichts. Es ist eine Übereinstimmung zweier gemessener Zeichenketten, mehr nicht.

> **Grenze des Befundes nach L10.**
> *Gemessen war:* was **diese** SAAJ-Fassung (`saaj-impl 1.5.3`, API `javax.xml.soap-api 1.4.0`)
> auf **dieser** Java-Fassung (21.0.11, Temurin) sendet.
> *Behauptet wird ausdrücklich **nicht**,* dass das Altsystem im GlassFish-Container dasselbe
> sendet. Dort läuft eine andere Fassung, und die Serialisierung — Attributreihenfolge,
> Kopfzeilensatz, Leerraum — ist eine Eigenschaft der Implementierung, nicht des Vertrags.

**Hier endet die Messung.** Der Envelope ist **nicht** korrigiert, es ist **nichts erneut gesendet**
worden, und es steht hier keine Fassung, die man „gleich mal probieren" könnte. Was aus den zwölf
Unterschieden folgt, entscheidet der Auftraggeber.

---

## M71 — Ein Abruf mit dem echten Client

**Frage.** Der Nachbau aus M59 (2) holt nichts, obwohl Endpunkt, Operation und Kennungsform belegt
richtig sind (Q1); die Transportform ist die einzige verbliebene Differenz (M70). **Liefert der
echte Client eine Datei?**

**Aufbau.** Zweiter Einstiegspunkt `mitschnitt.Abruf` im Projekt aus M70. Kette wie
`JsonServlet.java:784`–`:797`: Verweis an der Pipe zerlegen, **die nackte GUID dahinter** senden,
Endpunkt ist der `ServiceConnectString` der Ablage **unverändert und ohne Anhängsel**.
`filestoreRetrieveDir` zeigt auf ein Wegwerfverzeichnis, das vor dem Lauf geleert wird.

**Ein Lauf, ein Verweis.** Gewählt ist deterministisch der erste Verweis aus Fenster A, der
`FILESTOREPROD09` nennt, sortiert nach GUID, Familie `FileReader.Payload.GUID` — dieselbe Zeile, die
schon die Stichprobe von M59 (2) anführte. Geholt wurde **bei der Ablage, die der Verweis nennt**
(Holregel aus M68).

**Der Aufruf liegt als Skript vor**, `scripts/mitschnitt-saaj/abruf.ps1` samt
`abruf-auswahl.sql`, und ist von dort ausgeführt worden. Damit wiederholt sich die Lücke aus Teil B
nicht, wo die `curl`-Aufrufe von M59 nie als Skript festgehalten waren. Weder Skript noch
SQL-Datei enthalten eine Kennung, einen Verweis, eine Adresse oder ein Passwort — alles wird in der
Sitzung hergeleitet.

**Die vier Alt-Dateien sind unverändert**, SHA-256 vor dem Lauf erneut gegen die Originale unter
`alterCode/` geprüft und Zeichen für Zeichen gleich den in M70 dokumentierten Summen.

| Umgebung | Wert |
|---|---|
| Java | 21.0.11, Eclipse Adoptium |
| SAAJ | `saaj-impl 1.5.3`, API `javax.xml.soap-api 1.4.0` |
| `@@global.read_only` bei der Auswahlabfrage | **`1`** |

### Ergebnis

| Kennzahl | Wert |
|---|---|
| `requestFilestoreActions()` | **`true`** |
| Dauer des Abrufs | **497 ms** |
| **HTTP-Status** | **nicht unmittelbar beobachtbar** — siehe unten |
| Attribut **`Response`** aus dem Rumpf | **`SUCCESS (RETRIEVE)`** |
| **Anhänge in der Antwort** | **1** |
| Antwort, neu serialisiert | 1.167 Byte |
| **Datei unter `filestoreRetrieveDir` entstanden** | **ja, genau eine** |
| Größe der Datei | **639 Byte** |
| Endung `.zip` | ja |

**ZIP-Verzeichnis** (Q4) — Namen sind nach G1 nicht abgedruckt, kein Eintrag ist geöffnet worden:

| Eintrag | entpackt | gepackt | Verzeichnis |
|---|---|---|---|
| 1 | **2.329 Byte** | 435 Byte | nein |
| **Einträge gesamt** | **1** | | |

**Byteklassen des ersten Eintrags** — gelesen, um zu zählen; kein Byte davon ist ausgegeben worden:

| Klasse | Wert |
|---|---|
| Bytes gesamt | 2.329 |
| Bytes über `0x7F` | **0** (0,00 %) |
| Nullbytes | **0** (0,00 %) |
| druckbare Zeichen | 2.329 (**100,00 %**) |
| vollständig gültiges UTF-8 | **ja** |
| Einschätzung binär | **nein** |

### Befunde

**Befund 1 — der echte Client holt die Datei, der Nachbau nicht.**
*Gemessen war:* `Response = SUCCESS (RETRIEVE)`, ein Anhang, eine Datei von 639 Byte im
Zielverzeichnis — bei demselben Verweis, derselben Ablage und derselben Operation, mit denen
M59 (2) nichts als eine leere `FileList` bekam.
*Behauptet wird:* Der Abrufweg **funktioniert**. Was M59 (2) fehlschlagen ließ, liegt in der
Transportform und nicht im Vertrag. Welcher der zwölf Unterschiede aus M70 den Ausschlag gibt, ist
**nicht gemessen** — dazu müsste man sie einzeln verändern, und das ist nicht geschehen.

**Befund 2 — die Datei kommt als ZIP mit genau einem Eintrag.**
*Gemessen war:* 639 Byte ZIP, ein Eintrag, 2.329 Byte entpackt, 435 Byte gepackt.
*Behauptet wird:* Q4 ist für diesen einen Verweis bestätigt: Die Auslieferung ist gepackt, und der
Entpackschritt ist nicht wegzudenken. Die Frage, was das Altsystem bei **mehr als einem** Eintrag
tut, bleibt offen — hier war es einer.

**Befund 3 — der Inhalt ist Text, nicht binär.**
*Gemessen war:* 0 Bytes über `0x7F`, 0 Nullbytes, 100 % druckbare Zeichen, gültiges UTF-8.
*Behauptet wird:* Dieser eine Eintrag ist reiner ASCII-Text. **Das sagt nichts über die Kodierung
im Allgemeinen:** Reines ASCII ist zugleich gültiges UTF-8 **und** gültiges ISO-8859-1, die beiden
Kodierungen sind hier nicht unterscheidbar. Die Frage aus M61 bleibt offen; sie entscheidet sich
erst an einer Datei mit Bytes über `0x7F`.

**Befund 4 — der Anhang ist größer als die Antwort, in der er steckt.**
*Gemessen war:* Anhang 639 Byte, neu serialisierter Umschlag 1.167 Byte, ein Anhang.
*Behauptet wird:* nichts über die Bytes auf der Leitung. Die 1.167 Byte sind eine **erneute
Serialisierung** der geparsten Antwort, nicht ihr Mitschnitt; sie sind mit den 579 Byte der Anfrage
aus M70 nicht vergleichbar.

**Zum HTTP-Status.** Er ist mit diesem Aufbau nicht zu beobachten, ohne `FilestoreClient.java` zu
verändern — SAAJ kapselt die Verbindung, und die Klasse gibt weder Status noch Rohbytes heraus. Was
sich sagen lässt: `HttpSOAPConnection` wirft bei einem Status außerhalb 2xx eine `SOAPException`,
`requestFilestoreActions()` fängt sie und lieferte dann `false`. Der Lauf lieferte `true` **und**
eine geparste Antwort mit Anhang. *Gemessen war:* `true` samt Anhang. *Behauptet wird:* Der Status
lag im 2xx-Bereich. **Die genaue Zahl ist nicht gemessen.**

> **Grenze des Befundes nach L10.**
> *Gemessen war:* das Verhalten für **einen** Verweis — eine Familie (`FileReader.Payload.GUID`),
> eine Ablage (`FILESTOREPROD09`), ein Zeitfenster (A).
> *Behauptet wird ausdrücklich **nichts** über die zehn Verweise aus M59 (2)*, nichts über
> `FILESTOREPROD10`, nichts über Protokolle und nichts über andere Zeitscheiben. M60, M61, M63,
> M64, M65, M66, M67 und M68 sind **nicht** gefahren.

**Danach ist angehalten worden.** Kein zweiter Verweis, keine Wiederholung, keine der ausstehenden
Messungen.

**Löschung.** Die abgerufene Datei enthält produktive Nutzdaten (G1 Stufe 2). Sie ist **zu keinem
Zeitpunkt geöffnet, angezeigt oder ausschnittweise ausgegeben** worden; gelesen hat sie
ausschließlich das Programm, um Bytes zu zählen. **Am 17.08.2026 um 14:59:17 gelöscht**, mit ihr die
Verzeichnisse `retrieve\` (1 Datei, 639 Byte), `arbeit\` (2 Dateien, 6.891 Byte) und `mitschnitt\`
(1 Datei, 579 Byte). Die Löschung ist geprüft: `Test-Path` liefert für alle drei `False`.

---

## Das Auswertungsskript

`scripts/messung-schritt8/auswertung.ps1` ist **geschrieben und geprüft**, obwohl M61, M63, M65 und
M67 nicht gefahren sind. Es ist damit einsatzbereit, sobald die Freigabe vorliegt.

**Was es tut.** Es liest die Dateien aus einem Arbeitsverzeichnis und schreibt **eine CSV mit
Zählungen** nach `scripts/messung-schritt8/ergebnis/`. Es deckt M61 (BOM, Bytes über `0x7F`,
UTF-8-Gültigkeit, Zeilenende, EDIFACT-Zeichensatz), M63 (Markenbilanz), M65 (Inhaltsklassen je
Bereich) und M67 (Bytes und Zeilen des Innenbereichs) ab. Es läuft ohne Netzzugriff; Holen und
Auswerten sind getrennt.

**Was es nicht tut.** Es gibt **keinen** Dateiinhalt aus — nicht auf stdout, nicht in die CSV, nicht
in eine Protokolldatei. Jedes Ausgabefeld ist eine Zahl, ein Ja/Nein-Wert oder ein Wort aus einem im
Skript fest verdrahteten Vokabular. Das einzige Textfeld ist `unb_zeichensatz` mit den fünf
möglichen Werten `UNOA`, `UNOB`, `UNOC`, `UNOY`, `keiner`; diese Wörter stehen als Literal im
Skript, die Datei entscheidet nur, welches davon erscheint.

**Die Dreiteilung von M65.** Der Innenbereich ist nach der **konservativen** Regel geschnitten: von
der **ersten** Startmarke bis zur **nächsten darauffolgenden** Endmarke. Ohne vollständiges Paar
gibt es keinen Innenbereich; die Zeilen ab der Startmarke werden dann **nicht** auf „nach" verteilt,
sondern in einer eigenen Spalte `unbestimmt_zeilen` gezählt. Eine Verteilung wäre eine Deutung, und
diese Runde deutet nicht.

**Selbstprüfung.** Unter `scripts/messung-schritt8/testdaten/` liegen **drei selbst erfundene**
Protokolldateien. Sie enthalten keinen einzigen echten Wert — Partner, Knoten, Pfade und Kennungen
sind frei erfunden und als solche benannt (`.invalid`, `ERFUNDEN-…`, `BEISPIELPROD42`).

| Datei | Aufbau | Erwartet | Gemessen |
|---|---|---|---|
| `1.log` | vollständiges Markenpaar | 1 Start, 1 Ende, Paar, 4 Innenzeilen | **1 / 1 / ja / 4** ✔ |
| `2.log` | Startmarke ohne Endmarke | 1 Start, 0 Ende, kein Paar, 7 unbestimmte Zeilen | **1 / 0 / nein / 7** ✔ |
| `3.log` | zwei Paare, Marke in einer echoten Zeile, dazu eine abweichende Schreibweise | 2 Start, 3 Ende, Paar schließt an der **echoten** Zeile, 0 Innenzeilen, 1 abweichend, 1 in Echozeile | **2 / 3 / ja / 0 / 1 / 1** ✔ |

**`3.log` ist der wichtigste der drei.** Er enthält die Zeile
`Message.DestinationFilename=erfunden***EndOfLog***.txt` **innerhalb** des ersten Markenpaars. Die
konservative Regel schließt daraufhin sofort: `ende_zeilennr` ist 7 statt 10, und der Innenbereich
ist **leer**. *Gemessen war:* Ein Dateiname mit der Zeichenfolge `***EndOfLog***` verkürzt den
Innenbereich auf null Zeilen. *Behauptet wird:* Die im Auftrag beschriebene Beeinflussbarkeit der
Marken ist am Skript **nachvollzogen** — an erfundenen Daten, nicht an echten. Ob sie in echten
Protokollen vorkommt, sagt erst M63.

**Gegenprobe zu G1.** Die erzeugte CSV ist danach auf neun Zeichenfolgen aus den Eingabedateien
geprüft worden (`erfunden`, `Erfundene`, `Program`, `StartOfLog`, `EndOfLog`, `Message.`, `opt`,
`invalid`, `PROD`). **Keine einzige kommt vor.**

**Ein Fehler im Skript, gefunden und behoben.** Die Verdichtungszeilen zählten unter Windows
PowerShell 5.1 bei **genau einem** Treffer nichts und blieben still leer — bei null und bei zwei
Treffern stimmten sie. Der Fehler ist durch `@(…)` um die Pipeline behoben. Er steht hier, weil er
ohne die Selbstprüfung nicht aufgefallen wäre und weil er jede künftige Auswertung betrifft.

---

## Laufzeiten im Überblick

| Messung | Fenster A | Fenster B | Fenster C | Wiederholungen |
|---|---|---|---|---|
| M52 | — | — | — | einmalig, **1,348 ms** |
| M53 | **876,5 ms** | **31,334 s** | **745,6 ms** | A/C einmalig; B Aufwärmlauf + 2 |
| M54 (a) | 1,808 s | **59,325 s** | — | A einmalig; B Aufwärmlauf + 2 |
| M54 (b) | 0,406 s | 13,911 s | — | A einmalig; B Aufwärmlauf + 2 |
| M54 Gegenprobe | 0,395 s / 0,357 s | 13,801 s | — | einmalig |
| M55 (a) | 0,433 s | 26,664 s | — | einmalig |
| M55 (b) | 0,515 s | 19,597 s | — | einmalig |
| M56 (a) | 0,516 s | 16,596 s | — | einmalig |
| M56 (b) | 0,088 s | 3,047 s | — | einmalig |
| M56 (c) | 0,172 s | 4,974 s | — | einmalig |
| M57 | 0,900 s | 33,296 s / 27,473 s | — | einmalig |
| M58 (1) | 0,748 ms / 0,702 ms | — | — | Aufwärmlauf + **5** |
| M58 (2) | 0,336 ms | — | — | Aufwärmlauf + **5** |
| M58 (3) | 0,512 ms / 0,504 ms | — | — | Aufwärmlauf + **5** |
| M64a | — | 0,023 s | — | einmalig |
| M59 (1) `GET` | 6,61 ms / 6,71 ms | — | — | beste aus 22 Anfragen je Ablage |
| M59 (2) `POST` `RETRIEVE` | 6,22 ms / 6,39 ms | — | — | beste aus 5 Anfragen je Ablage |

**Teil B mit dem echten Client, 17.08.2026** — je Messung Gesamtdauer und Dauer je Verweis:

| Messung | Verweise | Gesamtdauer | je Verweis | übergangen (abgeschaltete Ablage) |
|---|---|---|---|---|
| M71 | 1 | 497 ms | 497 ms | 0 |
| M66 | 293 | 12.690 ms | 43 ms | 0 |
| **M66 (2)** | 153 | 19.489 ms | 127 ms | **0 von 153** |
| **M60 / M61** | 206 | **50.405 ms** (kalt) | 244 ms | **0 von 44.329** |
| M60 / M61 (Wiederholung, warm) | 206 | 3.001 ms | 14 ms | — |
| **M63 / M65 / M67** | 454 | **94.355 ms** | 207 ms | **0 von 636.848** |
| **M64** | 33 | 1.366 ms | 41 ms | **0 von 33** |
| **M68** (20 gekreuzt + 20 gerade) | 40 | 1.545 ms | 38 ms | **0** |
| **Summe Teil B** | **1.180** | **rund 183 s** | — | **0** |

**Übergangen wurde kein einziger Verweis.** Alle Stichproben dieses Laufs sind aus Zeiträumen
gezogen, die vollständig auf `FILESTOREPROD09`/`10` liegen — die Rotationsgrenze
(`2025-07-23`) liegt vor allen verwendeten Fenstern. Die Zahl sagt damit **nichts** über den
Anteil des abrufbaren Bestands; jene Zahl steht unter „Die Rotationsgrenze" und beträgt **36,8 %**.

**Kein Statement hat die Grenze von 60 Sekunden gerissen.** Das teuerste — M54 (a) Fenster B —
liegt bei **98,9 %** davon. Die Grenze ist an keiner Stelle angehoben worden.

## Löschung des Arbeitsverzeichnisses

Die Anfragen von Teil B liefen aus dem Sitzungs-Scratchpad, Unterverzeichnis `…\scratchpad\arbeit\`
— **außerhalb des Repositorys**. Darin lagen drei Dateien: die beiden 185-Byte-Antworten der Dienste
(`body09.bin`, `body10.bin`) und die Stichprobenliste (`stichprobe.txt`, 1.772 Byte) mit den
Verbindungszeichenketten und zwanzig Artefaktkennungen. **Kein Dateiinhalt lag darin, weil keiner
abrufbar war.**

**Das Verzeichnis ist am 17.08.2026 um 12:20:42 gelöscht worden**, mit ihm alle drei Dateien. Die
Löschung ist geprüft: `Test-Path` liefert danach `False`.

Nach der Freigabe vom selben Tag ist es **ein zweites Mal** angelegt und die Stichprobe
deterministisch erneut beschafft worden (`stichprobe.txt`, 1.772 Byte, identischer Inhalt). Da der
`POST` an der Berechtigungsschicht scheiterte, lag auch darin **kein Dateiinhalt**. **Zweite
Löschung am 17.08.2026 um 13:01:47**, geprüft.

Nach Aufnahme der Werkzeugregel ist es **ein drittes Mal** angelegt worden — für die zwölf Anfragen
von M59 (2). Darin lagen 16 Dateien mit zusammen **4.854 Byte**: die Stichprobenliste, die
Rumpfdateien der Anfragen und die zwölf Antworten zu je 185 Byte. **Auch darin lag kein
Dateiinhalt** — der Empfänger hat keine Datei geliefert, weshalb M60 bis M68 nicht laufen konnten.
**Dritte Löschung am 17.08.2026 um 13:11:53**, geprüft.

Eingecheckt bleiben ausschließlich die `.sql`-Sitzungsdateien, das Auswertungsskript und die
erfundenen Testdaten. Die `.sql`-Dateien enthalten **keine** Kennung, **keinen** Wert und **keine**
Verbindungszeichenkette — jeder Prüfwert wird in der Sitzung selbst über eine deterministische
Auswahlabfrage hergeleitet. `scripts/messung-schritt8/ergebnis/` und `scripts/messung-schritt8/arbeit/`
sind in `.gitignore` eingetragen.

## Offene Punkte

Keiner davon ist entschieden. Sie sind der Ertrag dieser Runde, nicht ihr Rest.

1. **Warum antwortet der Empfänger auf `RETRIEVE` wie auf ein blankes `GET`?** ~~Freigabe für den
   SOAP-Abruf~~ ✔ erteilt am 17.08.2026, ~~Werkzeugregel für `curl.exe`~~ ✔ am selben Tag
   aufgenommen, ~~Envelope~~ ✔ gelesen, dokumentiert und **abgesetzt**. Gemessen ist: Er holt
   nichts (M59 (2)). **Das ist jetzt eine fachliche Frage und keine Berechtigungsfrage mehr.**

   Vier Abwandlungen liegen nahe, **keine ist gefahren**, weil die Freigabe sie sperrt. Sie sind
   der Vorschlag für den nächsten Anlauf, und der Auftraggeber entscheidet, welche davon er
   freigibt:

   | Vorschlag | Warum |
   |---|---|
   | `ID` als **zusammengesetzter** Verweis `<Ablage>\|<GUID>` statt als bloße GUID | Das Format ist aus QT1 gelesen, nicht gemessen; der Altcode selbst legt es nicht fest |
   | `File` **qualifiziert** im Namensraum `http://filestore.kraftwerkone.de` | SAAJ serialisiert `new QName("File")` möglicherweise anders, als der Nachbau es tut |
   | Ein anderer `SOAPAction`-Wert als `""` | SAAJ setzt ihn selbst; welchen Wert der Empfänger erwartet, ist ungelesen |
   | Der Aufruf über den **echten Client** statt über einen nachgebauten Envelope | Der sicherste Weg: `alterCode/` liegt vor, ein SAAJ-Lauf schickt genau das, was das Altsystem schickt — und beantwortet damit zugleich, ob der Nachbau oder der Vertrag das Problem ist |

   **Nachtrag vom 17.08.2026:** Die ersten drei Vorschläge sind durch **Q1** erledigt — Endpunkt,
   Operation und Kennungsform sind aus dem Quelltext belegt und waren in M59 (2) bereits richtig
   gesetzt. Offen bleibt allein der vierte, die **Transportform**; siehe „Offene Punkte" 13.

   Der letzte Vorschlag ist der aussagekräftigste und zugleich der aufwendigste: Der Altclient
   nutzt `javax.xml.soap` (SAAJ) und `javax.activation` gegen JavaSE-1.7
   (`FilestoreClient.java:8–10`); beide Pakete sind seit Java 11 nicht mehr im JDK und heißen unter
   Jakarta EE `jakarta.xml.soap` und `jakarta.activation`. Ein Lauf mit dem echten Client braucht
   also eine eigens gebaute Umgebung. *Das ist eine Folgerung aus dem Versionsstand, keine Messung
   dieser Runde.* Solange keiner der vier Vorschläge freigegeben ist, bleibt Teil B bei M59 stehen. Danach laufen
   M60, M61, M63, M64, M65, M66, M67 und M68 ohne weitere Vorbedingung; das Auswertungsskript
   steht geprüft bereit.
2. **Fenster C liegt auf nicht erreichbaren Ablagen.** M53 zeigt, dass der 01.10.2024 vollständig
   auf `FILESTOREPROD07`/`08` liegt. Ob diese beiden für die Abnahme hochgefahren werden, ist eine
   Betriebsentscheidung. Ohne sie ist M66 für die älteste Zeitscheibe nicht messbar.
3. **Das Fenster für M64 trägt nur elf Fehlernachrichten.** Der Auftrag sieht bis zu 200 vor. Die
   49 `ERROR_TIMEOUT` des Dezembers liegen ausweislich `messungen-schritt5.md` (Z. 812–826) am
   `2025-12-30` um 04:08–04:09 und damit **vier Stunden nach dem Ende von Fenster B**. Ob das
   Fenster für M64 zu erweitern ist, ist eine Entscheidung des Auftraggebers — eine Erweiterung
   würde die Vergleichbarkeit mit M53 bis M57 aufgeben.
4. **`FTPSender` verliert seinen Schrittnamen in 99,2 % der Fälle** (M57 Befund 3), obwohl dieselbe
   Kombination zehn verschiedene Schrittnamen kennt und die `MessageAction`-Zeile vorhanden ist.
   Woran das liegt, sagt M57 nicht. Solange das offen ist, darf „technische Familie statt
   Schrittname" nicht als bewusste Gestaltung verkauft werden.
5. **Ein Artefaktverweis ist nicht eindeutig** (M54 Befund 4): In `OFTP2Sender.Payload.GUID` tragen
   4.577 Zeilen nur 4.573 verschiedene Werte. Wer die Artefaktliste über den Wert schlüsselt,
   verliert einen Eintrag. Ob das eine Datenanomalie oder ein zulässiger Fall ist, ist offen.
6. **Ist `FileReader.FileProperty.Size` eine Byteangabe?** Ohne M60 unbelegt. Die Kappungsgrenze
   darf bis dahin **nicht** aus M56 (b) abgeleitet werden — auch nicht aus den 81 Werten über
   10 MiB.
7. **Die Vorabgröße deckt nur 57,2 % des Monatsfensters** (M56 Befund 1), nicht die vom Auftrag
   erwarteten 69,6 %. Dass die Kappung während des Datenstroms greifen muss, folgt daraus; wie sie
   greift, nicht.
8. **Das Maximum von 15 Artefakten je Nachricht** (M55 Befund 2) fällt in die Lücke der
   vorregistrierten Deutung. Ob 15 noch eine flache Liste ist, ist eine Gestaltungsentscheidung.
9. **`Using index` erscheint bei M58 (1) nicht**, und die dafür vorgesehene Folgerung trifft die
   Ursache nicht (M58 Befund 3). Ob überhaupt etwas zu tun ist, ist bei 0,748 ms fraglich — aber
   die vorregistrierte Zeile ist damit nicht abgearbeitet, sondern offen.
10. **Widerspruch, der hier nur notiert und nicht bereinigt wird.** `datenmodell.md` §3,
    `PROJEKTBESCHREIBUNG.md` §7 und `IMPLEMENTIERUNGSPLAN_MVP.md` führen `FILESTOREPROD09` als
    **das** Beispiel. M52 misst **elf** Ablagen (`FILESTOREPROD00`–`10`), M53 zeigt, dass je
    Zeitraum **zwei** gleichzeitig aktiv sind und die Zuordnung nicht am Mandanten hängt. **Keine
    dieser Dateien ist angefasst worden** — der Widerspruch gehört ins Sparring, nicht in eine
    stille Korrektur.
11. **Vorschlag für eine ergänzende Messung ab E8.** M53 misst die Rotation an zwei Zeitpunkten
    (09/10 in A und B, 07/08 in C). Wann sie umschaltet, in welchem Takt und ob weitere Wechsel
    folgen, ist über zwei Stützstellen nicht bestimmbar — davon hängt ab, mit wie vielen
    unerreichbaren Ablagen die Anzeige rechnen muss. **Die Messung ist in dieser Runde nicht
    gefahren**, weil sie im Auftrag nicht steht; die Nummer **E8** ist frei und dafür vorgemerkt.

### Nachträge vom 17.08.2026, nach Aufnahme der Quelltextbefunde

12. **Eine Messung zur Kennungsform (M69) wäre gegenstandslos.** Sie war der naheliegende nächste
    Schritt, solange offen war, ob die `ID` als nackte GUID oder als zusammengesetzter Verweis
    übergeben wird. **Q1 beantwortet das aus der Quelle** (`JsonServlet.java:784`–`:786`, `:793`):
    nackte GUID. Eine Messung darüber misst nichts mehr. Die Nummer **M69 ist nicht vergeben** und
    bleibt frei. **Der Auftrag ist nicht angepasst worden** — §8 behält die Fortschreibung dem
    Auftraggeber vor.
13. **Die Transportform ist der einzige verbliebene offene Punkt in Teil B.** SAAJ
    (`FilestoreClient.java:94`, `soapConnection.call`) gegen einen von Hand gebauten Envelope mit
    `curl`. Endpunkt, Operation, Kennungsform und Envelope-Aufbau sind belegt (Q1), der Abruf
    liefert trotzdem nichts (M59 (2)). Die Aufrufkette liegt mit `JsonServlet.java:792`–`:797`
    vollständig vor; ein **Mitschnitt des vorhandenen Clients gegen einen lokalen Lauscher** wäre
    der Weg, die tatsächlich gesendeten MIME-Kopfzeilen und die Serialisierung zu sehen.
    **Nicht vorbereitet und nicht gefahren.**

    *Nachtrag vom 17.08.2026:* Auf gesonderte Freigabe hin **ist dieser Mitschnitt gefahren
    worden** — gegen einen Lauscher auf `127.0.0.1`, nicht gegen den Filestore. Ergebnis in
    **M70**: zwölf Unterschiede zur handgebauten Anfrage aus M59 (2), darunter eine gesendete
    `SOAPAction`, die das Altsystem gar nicht sendet, eine BOM, eine XML-Deklaration und eine
    andere Attributreihenfolge. **Was daraus folgt, ist nicht entschieden**; der Envelope ist nicht
    korrigiert und nichts erneut gesendet worden.

    *Zweiter Nachtrag vom 17.08.2026:* **M71** hat den echten Client einmal gegen die Ablage
    laufen lassen, die der Verweis nennt — er **holt die Datei** (`SUCCESS (RETRIEVE)`, ein Anhang,
    639-Byte-ZIP). Damit ist belegt, dass der Abrufweg trägt und die Ursache des Nullbefunds in der
    Transportform liegt. **Welcher der zwölf Unterschiede den Ausschlag gibt, ist offen** — dazu
    müsste man sie einzeln verändern, und das ist nicht geschehen.
14. ~~**Ein Dienstkontoname steht im Klartext in dieser Datei.**~~ ✔ **Entschieden am 17.08.2026.**
    Der Auftraggeber hat `alterCode/` von der Aufnahme ins Repository ausgenommen; damit ist auch
    der Dienstkontoname in Q3 **maskiert** worden (`/opt/txp/users/<dienstkonto>/`). Andernfalls
    wäre die Ausnahme wirkungslos gewesen. Die Regel bleibt dokumentierbar; der genaue Wert steht
    im Quelltext.
15. **Die Envelope-Tabelle trug eine falsche Herkunftsangabe.** `Content-Type` und `SOAPAction`
    waren als aus Z. 94 gelesen ausgewiesen; sie waren gesetzt. Korrigiert am 17.08.2026, die alte
    Zeile steht im Kasten bei „Der freigegebene Envelope". Was SAAJ tatsächlich sendet, ist in
    keinem vorliegenden Quelltext enthalten und bleibt offen (siehe Punkt 13).
16. **Für die Sparring-Runde vermerkt, hier nicht entschieden: die konservative Schnittregel des
    Auftrags ist strenger als das heutige Verhalten.** Bei mehreren Markenpaaren zeigt sie
    **weniger** (das Altsystem hängt alle Blöcke aneinander, `JsonServlet.java:841`), bei fehlender
    Endmarke **nichts statt alles** (`:823`). Das ist eine Verhaltensänderung gegenüber dem, was
    die Nutzer heute kennen, und sie ist bewusst zu entscheiden.

### Nachträge vom 17.08.2026, nach M66

17. **Die Zeitscheibe „letzte Woche vor `2026-07-08`" ist leer, die Daten liegen auf dem
    `2026-07-08` selbst.** M66 hat sie wie beauftragt gefahren und **nicht verschoben**. Damit ist
    die dritte vorregistrierte Zeile von M66 („die jüngste Scheibe unvollständig") ungeprüft
    geblieben. Ob die Scheibe auf den `2026-07-08` zu legen ist, **entscheidet der Auftraggeber**;
    es wäre eine Änderung an der Stichprobendefinition des Auftrags.
18. **Warum fehlen `2026-01` bis `2026-05`?** Der Bestand reicht bis `2026-07-08 17:21:10`, trägt
    aber in `2026-05` und in der Woche vor dem `2026-07-08` keine einzige Nachricht. Unvollständige
    Kopie oder Betriebspause — die Antwort entscheidet, ob die Aufbewahrungsfrage an dieser Kopie
    überhaupt messbar ist. **Nicht gemessen**; M66 hat nur die Scheibengrößen erhoben.

    *Nachtrag vom 17.08.2026 — es ist dieselbe Lücke, die `PROJEKTBESCHREIBUNG.md` §8 führt.*
    Gemessen sind für 2026 genau zwei Monate mit Daten: **`2026-06` 4.848** und **`2026-07` 285**
    Nachrichten; `2026-01` bis `2026-05` tragen null. Das sind **fünf Monate** und deckt sich mit
    der „fünfmonatigen Datenlücke", die §8 in der Korrektur vom 07.08.2026 nennt — dort als Grund
    dafür, dass der Schnitt von „rund 5.000 Nachrichten pro Tag" an keinem Tag existierte
    (`messungen-schritt4.md`, Auffälligkeit A: dichter Bestand `2024-10-01` bis `2025-12-30`,
    3.336.386 Zeilen über 456 Tage). **Der Punkt wird nicht doppelt geführt**, sondern hierüber
    verknüpft. `PROJEKTBESCHREIBUNG.md` ist dabei **nur gelesen und nicht geändert** worden.
    Offen bleibt allein die Ursache der Lücke.
19. **Ohne eine erreichbare Ablage aus dem alten Zeitraum bleibt V4 offen.** M66 konnte die
    Aufbewahrungsfrist nicht messen, weil alle 197 Verweise der beiden ältesten Scheiben auf
    `FILESTOREPROD07`/`08` zeigen und diese nicht antworten. Ob sie für die Abnahme hochgefahren
    werden, ist eine Betriebsentscheidung (siehe auch Punkt 2).

### Nachträge vom 17.08.2026, nach dem Abschluss von Teil B

20. **63,2 % des Bestands sind heute nicht abrufbar.** Die Rotationsgrenze liegt taggenau auf dem
    `2025-07-23`; alles davor nennt `FILESTOREPROD07`/`08`, und die sind aus. Das sind 2.111.355
    von 3.341.519 Nachrichten. **Ob die Dateien dort noch liegen, ist ungemessen** — die Frage ist
    nicht dieselbe wie die nach der Aufbewahrungsfrist. Welchen Zeitraum die Abnahme abdecken soll,
    ist eine Entscheidung.
21. **Am jüngeren Ende laufen die Kopien vollständig auseinander** (M66 (2)): 153 von 153 Verweisen
    des `2026-07-08` antworten `Error (Skipped)`. Für die Abnahme ist eine Nachricht zu wählen, die
    in beiden Kopien liegt. Wo der Filestore-Bestand endet, ist **nicht** gemessen — `2026-06` ist
    ungeprüft.
22. **`HTTPSender.Log.GUID` hat keine Marken** (M63, 30 von 30), `FTPSender.Log.GUID` in 28 von 30
    Fällen nicht. Nach der vorregistrierten Zeile wäre ihr Protokoll für `MANDANT` **gesperrt**,
    nicht ungeschnitten sichtbar. **Nicht entschieden.**
23. **Der Beschnitt hält weniger zurück als gedacht** (M65): 808 Pfadzeilen und 331
    Dienstkennungen liegen **innerhalb** der Marken, 334 von 395 Dateien tragen dort mindestens
    einen Pfad. Die Beschreibung „Mandantentrennung" für den Beschnitt ist damit irreführend und
    gehört korrigiert — **wo, entscheidet der Auftraggeber.**
24. **Ob die 414 Pfadzeilen *außerhalb* der Marken schaden, ist eine Bewertung und keine Messung**
    (M65 Befund 4). Der vierte vorregistrierte Fall lässt sich mit einer Zählung nicht entscheiden.
25. **18,2 % der Nutzdateien sind binär** (M61) — ein Fall, den der Auftrag nicht kennt. Was die
    Dateien sind, ist **nicht gemessen**; „vermutlich IDOC" ist eine Vermutung des Auftraggebers.
    Wie die Anzeige damit umgeht, ist offen.
26. **Die Anzeige ist nicht kodierungsfrei** (M61): Von den entscheidbaren, nicht-binären Dateien
    sind 8 von 8 Protokollen und 9 von 16 Nutzdateien **kein** gültiges UTF-8. Q4 zeigt, dass das
    Altsystem hart `ISO-8859-1` dekodiert. Es braucht eine benannte Kodierungsentscheidung samt
    Umschalter — **nicht entschieden.**
27. **Der Beschnitt verkürzt die Anzeige nicht nennenswert** (M67): 90,4 % der Bytes liegen im
    Innenbereich, je Familie zwischen 9,3 % und 99,4 %. Die Frage Panel gegen eigene Route stellt
    sich für beide Rollen gleich. Eine einzige Zahl für „den Beschnitt" gibt es nicht.
28. **Die Zeilenenden sind uneinheitlich** (M61): LF 95, CRLF 38, gemischt 37, keines 36 von 206.
    Was die Anzeige daraus macht, ist offen.

## Abweichungen vom Auftrag

Zwanzig, alle vorsätzlich und alle hier statt in einer Fußnote.

1. **Der Prompt nennt Fassung 2, die vorliegende Auftragsdatei ist Fassung 3** vom 17.08.2026 und
   ersetzt Fassung 2 ausdrücklich („Ersetzt Fassung 2 vom selben Tag", Z. 3). Gefahren ist
   **Fassung 3**: beide Ablagen statt einer, M59 und M66 je Ablage, M68 neu, Nummernbereich bis
   M68 statt M67. Die Nummernangabe des Prompts (§3, §7: „M52–M67") ist damit überholt.
2. **Diese Fassung ersetzt eine gleichnamige Erhebung vom 14.08.2026.** Der Auftraggeber hat die
   vollständige Wiederholung angeordnet. Alle Messungen sind **neu erhoben**; keine Zahl ist aus
   der alten Fassung übernommen. Übernommen ist ausschließlich der Abschnitt **QT** — siehe
   Abweichung 8.
3. **`--skip-ssl` → `--ssl-mode=DISABLED`.** Der Workbench-Client kennt die MariaDB-Schreibweise
   nicht. Dieselbe Abweichung ist in `messungen-schritt7.md` M49 ausgewiesen.
4. **`profiling_history_size` von 15 auf 100 gehoben.** Die Vorgabe der Instanz ist **15**
   (in Sitzung 1 gemessen). Ohne die Anhebung verlieren lange Sitzungen die Laufzeiten ihrer frühen
   Statements. Es ist eine Sitzungseinstellung, kein Schreibzugriff.
5. **`--force` in den Sitzungen 5, 6 und 7.** Damit ein Statement, das die 60-Sekunden-Grenze
   reißt, die restlichen Statements derselben Sitzung nicht mitreißt. Gerissen hat die Grenze
   **keines**; die Einstellung hat sich also nicht ausgewirkt.
6. **M56 (c) ist mit verschärfter Klassifikation gefahren.** Der beauftragte Filter
   `^[a-z0-9]{1,5}$` lässt reine Ziffernfolgen durch — und 99,8 % der Werte sind genau das. Er
   hätte die Zahlenfragmente einzeln in diese Datei geschrieben, wovor die Begründung des Filters
   im Auftrag selbst warnt. Die gefahrene Fassung verlangt einen Buchstaben an erster Stelle.
   Begründung bei M56.
7. **Zwei Zusätze, die der Auftrag nicht nennt, beide zur Absicherung eines Befunds:**
   die **Gegenprobe zu M54 (b)** (eine leere Ausgabe ist kein Beleg für eine leere Ergebnismenge)
   und die **Verdichtungsabfrage zu M57 Fenster B** (eine Summe statt 68 von Hand addierter
   Zeilen). Beide fügen keine Frage hinzu, sie sichern eine vorhandene ab.
   Die **Laufzeitmessung von M58** läuft gegen eine zählende Hülle, der `EXPLAIN` gegen das rohe
   Statement — Begründung bei M58.
8. **Der Abschnitt QT ist aus der ersetzten Fassung übernommen.** Er ist **gelesen, nicht
   gemessen**, war nie Teil des Auftrags und ist in dieser Runde **nicht nachgeprüft**. Er steht
   hier, weil die Anweisung „bisherige Ergebnisse überschreiben" ihn sonst ersatzlos gelöscht
   hätte und er die einzige Beschreibung des Abrufvertrags ist — also genau dessen, woran Teil B
   scheitert. Er ist als Anhang und als Übernahme gekennzeichnet und trägt zu keiner Zahl dieser
   Runde bei.

   *Nachtrag vom 17.08.2026:* Der Abschnitt steht seither als **Q6** im Abschnitt Q; der Anhang
   als eigene Ebene entfällt. **Vier seiner Aussagen — QT1, QT4, QT6 und QT7 — sind inzwischen aus
   der Quelle belegt**, die Fundstellen stehen bei Q6. Die Einordnung „gelesen, nicht gemessen"
   bleibt für den ganzen Abschnitt Q bestehen.

9. **M70 führt erstmals Alt-Code aus.** Der Messauftrag sieht das nicht vor — bis dahin ist
   `alterCode/` ausschließlich **gelesen** worden. Der Lauf vom **17.08.2026** beruht auf einer
   **gesonderten Freigabe** des Auftraggebers und ist eng gefasst: ein einziger Lauf, Ziel
   ausschließlich `127.0.0.1`, der `ServiceConnectString` weder gelesen noch aufgelöst, eine frei
   erfundene GUID, kein Rückfall und keine Gegenprobe am echten Endpunkt. Die vier Alt-Dateien sind
   byteidentisch kopiert; die Prüfsummen stehen bei M70. Das Wegwerfprojekt liegt unter
   `scripts/mitschnitt-saaj/` und ist **nicht** in den Hauptbau eingehängt.

10. **M71 ruft erstmals eine produktive Nutzdatei ab.** Auch das sieht der Messauftrag in dieser
    Form nicht vor; der Lauf beruht auf einer **gesonderten Freigabe** vom **17.08.2026** und ist
    eng gefasst: **ein** Lauf, **ein** Verweis, Operation `RETRIEVE` (die nach
    `FilestoreClient.java:78`–`:90` konstruktionsbedingt nichts ablegen kann), geholt bei der
    Ablage, die der Verweis nennt. Die abgerufene Datei ist zu keinem Zeitpunkt geöffnet oder
    ausgegeben worden und noch am selben Tag gelöscht. Der Aufruf liegt als Skript vor.
11. **Am `pom.xml` des Wegwerfprojekts ist nach M70 eine Zeile geändert worden.** Die feste
    `<mainClass>`-Vorgabe des `exec-maven-plugin` überstimmte `-Dexec.mainClass` auf der
    Befehlszeile — der erste Anlauf von M71 führte dadurch **still M70 erneut aus**, gegen den
    lokalen Lauscher und nicht gegen den Filestore. Die Vorgabe ist entfernt; die Klasse wird jetzt
    bei jedem Aufruf ausdrücklich genannt. Der Fehllauf ist ohne Wirkung auf beide Befunde: Er lief
    gegen `127.0.0.1` mit der erfundenen GUID und hat M70 lediglich mit identischem Ergebnis
    wiederholt.

12. **M66 ist mit 25 statt 50 Nachrichten je Zeitscheibe gefahren.** Grund ist **Ergänzung 5** des
    Auftrags vom 17.08.2026: M66 beantwortet, ob zu einer Nachricht überhaupt eine Datei existiert,
    und bestimmt damit die Stichprobenauswahl aller folgenden Messungen — dafür genügen 25. Die
    Halbierung betrifft alle fünf Scheiben gleichmäßig. Sie schwächt Befund 1 (96 von 96 in
    `2025-12`) nur im Umfang, nicht in der Richtung; auf die Befunde 2 und 3 hat sie keine Wirkung,
    weil dort **kein** Verweis abrufbar war beziehungsweise **keine** Nachricht existierte.
13. **Zwei Zeitlimits sind gesetzt worden**, `sun.net.client.defaultConnectTimeout` 3.000 ms und
    `defaultReadTimeout` 20.000 ms. Ohne sie hängt ein Abruf gegen eine nicht laufende Ablage
    unbegrenzt. Gesetzt über Systemeigenschaften, damit `FilestoreClient.java` byteidentisch
    bleibt. Ausgewirkt haben sie sich nicht: Die 197 Fehlversuche endeten sofort mit
    `Message send failed`, nicht am Zeitlimit.
14. **Zwei Statements ohne Zeitfenster** (Regel L9): die Scheibengrößen und die Bestandsgrenzen
    (`MIN`/`MAX` über `MessageLastUpdate`). Beide laufen ausschließlich über `Message`, nicht über
    `MessageProperty`, und beide waren nötig, um „leere Scheibe" von „Kopie endet früher" zu
    unterscheiden — genau die Unterscheidung, die die vorregistrierte Deutung von M66 verlangt.

15. **Die Kürzung aus Abweichung 12 gilt ab dem Abschlusslauf nicht mehr.** Nach Auskunft des
    Auftraggebers vom 17.08.2026 war sie unnötig — M66 lief mit 43 ms je Verweis statt der aus
    M71 hochgerechneten 497 ms. **M66 (2) und alle Messungen des Abschlusslaufs verwenden die
    Stichprobenumfänge des Auftrags unverändert.** M66 selbst bleibt bei 25; es ist gefahren und
    wird nicht nachträglich verändert.
16. **Alle Stichproben des Abschlusslaufs ziehen ausschließlich Verweise auf `FILESTOREPROD09`
    und `FILESTOREPROD10`.** Grund ist die Auskunft des Auftraggebers vom 17.08.2026, dass alle
    übrigen Ablagen abgeschaltet sind. Ein Fehlversuch gegen eine abgeschaltete Ablage ist kein
    Befund, sondern Rauschen — und in einer Ergebnistabelle sähe er aus wie einer. **Übergangen
    wurde dadurch kein einziger Verweis**, weil alle verwendeten Fenster nach der Rotationsgrenze
    liegen; die Zahl ist je Messung ausgewiesen.
17. **Das Auswertungsskript hat einen Entpackschritt bekommen.** Es war vor dem
    `JsonServlet`-Fund geschrieben und erwartete eine Protokolldatei; geliefert wird ein ZIP.
    Ausgewertet wird jetzt der **erste** Eintrag — wie im Altsystem (`JsonServlet.java:801`–`:802`)
    —, die Zahl der Einträge wird je Datei festgehalten. Neu sind außerdem die Spalten
    `nullbytes`, `druckbar`, `binaer`, `unterscheidbar` und `zusatz`. Die drei erfundenen
    Testdateien sind **zusätzlich in ZIP-Form** geprüft worden, dazu ein vierter Fall mit **zwei**
    Einträgen; die Erkennung ist verpackt wie unverpackt identisch. **In keiner Messung dieses
    Laufs ist eine Datei mit mehr als einem ZIP-Eintrag vorgekommen** (0 von 693).
18. **M68 ist um eine Gegenprobe erweitert worden**, die der Auftrag nicht verlangt: dieselben
    zwanzig Verweise zusätzlich am **eigenen** Knoten. Ohne sie wäre ein Fehlschlag des
    Kreuzabrufs nicht deutbar gewesen — „nicht gespiegelt" und „Datei gibt es nicht" sähen gleich
    aus. Sie fügt keine Frage hinzu, sie sichert die vorhandene ab.
19. **M60 ist zweimal gefahren worden.** Der erste Lauf hat die Spalte `zusatz` (die
    DB-Größenangabe) nicht durchgereicht, weil das Auswertungsskript sie noch nicht kannte; ohne
    sie wäre der Abgleich mit `FileReader.FileProperty.Size` nicht möglich gewesen. Der zweite Lauf
    lieferte **dieselben Zahlen**; berichtet ist die Dauer des **ersten** (kalten) Laufs.
20. **Drei Statements ohne Zeitfenster** (Regel L9), alle ausschließlich über `Message`: die
    Monatsverteilung 2026, die letzten Tage mit Daten und die Zählung vor/nach der
    Rotationsgrenze. Sie waren nötig, um die leeren Zeitscheiben einzuordnen und die Grenze zu
    beziffern.

**Nicht abgewichen** ist die Runde bei den Fenstergrenzen (wörtlich aus `messungen-schritt5.md`),
bei der Statementform von M52 bis M55, M57 und M64a, bei der Sequenzialität (kein Statement lief
gleichzeitig mit einem anderen, keine Anfrage parallel) und bei S1 (ausschließlich `SELECT`, `SET`,
`EXPLAIN`, `SHOW PROFILES`).

---

# Q — Quelltextbefunde (gelesen, nicht gemessen)

Aufgenommen am **17.08.2026**.

**Quellen**

| Datei | Umfang |
|---|---|
| `alterCode/Backend/JsonServlet.java` | 1.087 Zeilen |
| `alterCode/OverlordFilestoreClient/src/de/kraftwerkone/filestore/client/FilestoreClient.java` | 121 Zeilen |
| `alterCode/OverlordFilestoreClient/src/…/FilestoreFile.java` | 110 Zeilen |
| `alterCode/OverlordFilestoreClient/src/…/PayloadReader.java` | 50 Zeilen |
| `alterCode/OverlordFilestoreClient/src/…/PayloadWriter.java` | 27 Zeilen |
| `alterCode/FrontendExtJS/` (ExtJS 4.1, u. a. `app/controller/MainController.js`, `app/view/data_show_window.js`) | 47 Dateien |

*(Unter `alterCode/OverlordFilestoreClient/` liegt eine zweite, byteidentische Kopie der vier
Java-Dateien; die Zeilennummern gelten für beide.)*

> ⚠️ **`alterCode/` liegt nicht im Repository.** Der Auftraggeber hat es am 17.08.2026 von der
> Aufnahme ausgenommen, weil `JsonServlet.java` drei produktive absolute Pfade mit einem
> Produktionsknoten- und einem Dienstkontonamen trägt — CLAUDE.md schließt Hostnamen und
> Produktionsdaten im Repository aus, und ein Push ist in der Historie praktisch dauerhaft. Das
> Verzeichnis ist in `.gitignore` eingetragen und liegt ausschließlich lokal.
>
> **Alle Fundstellen dieses Abschnitts beziehen sich damit auf Dateien außerhalb des Repositorys.**
> Wer sie nachschlagen will, braucht den Quelltext vom Auftraggeber. Die vier Java-Dateien des
> Filestore-Clients sind zusätzlich unter `scripts/mitschnitt-saaj/src/main/java/de/kraftwerkone/`
> byteidentisch abgelegt — **diese Kopie ist eingecheckt** und trägt keine produktiven Pfade; ihre
> Prüfsummen stehen bei M70.

**Was dieser Abschnitt belegen kann — und was nicht.** Gelesener Quelltext belegt, **was das
Altsystem tut**. Er belegt **nicht**, was der laufende Filestore antwortet. Wo ein Befund eine
Messung ersetzt, steht das dabei; wo er sie nur vorwegnimmt, bleibt die Messung offen.

> **Abweichung von Regel L10, und warum sie nötig ist.** L10 verlangt den Belegvermerk
> *gemessen war X / behauptet wird Y*. In diesem Abschnitt lautet er
> ***Gelesen war:*** … (mit Datei und Zeile) / ***Behauptet wird:*** …
>
> Der Grund ist nicht Formtreue, sondern Haltbarkeit: Diese Befunde stehen in derselben Datei wie
> Zahlen aus `SHOW PROFILES`. Ein Belegvermerk, der „gemessen" sagt, wo gelesen wurde, macht beides
> in zwei Wochen ununterscheidbar — das wäre schlimmer als gar keiner. Aus demselben Grund trägt
> dieser Abschnitt **keine M-Nummer**: Eine Nummer stellte ihn in genau die Reihe, aus der er
> herausgehalten werden soll.

**G1 gilt unverändert.** Keine GUID, keine `MessageID`, keine Adresse — auch nicht als Beispiel,
auch nicht aus dem Quelltext. Die beiden maskierten Pfadpräfixe in Q3 stehen im Wortlaut, weil die
Regel ohne sie nicht dokumentierbar ist; einer davon enthält einen Dienstkontonamen und steht
deshalb unter „Offene Punkte" 14.

---

## Q1 — Die Aufrufkette `getData`

Einstieg: `JsonServlet.java:165`–`:171` ruft `getData(response, request, datei, propertyName, download)`.
Die Methode steht ab `:781`.

| Frage | Gelesen | Fundstelle |
|---|---|---|
| Kennung zerlegen | `url.split("\\|")`, `filestore = split[0]`, GUID `= split[1]` | `JsonServlet.java:784`–`:786` |
| Endpunkt | `serviceBean.find(filestore).getServiceConnectString()`, unverändert an `new java.net.URL(...)` | `:792`, `:795` |
| Operation | `FilestoreFile.filestoreAction.RETRIEVE` | `:793` |
| Kennungsform | `new FilestoreFile(messagePayloadGUID, null, RETRIEVE, messagePayloadGUID)` | `:793` |
| Ausführung | `addFileStoreAction(...)`, `requestFilestoreActions()` | `:796`–`:797` |

*Gelesen war:* Der Endpunkt ist der `ServiceConnectString` **ohne jeden Zusatz**
(`JsonServlet.java:792`, `:795`; im Client zwischen `FilestoreClient.java:36` und `:94` nicht
angefasst). Die Operation ist `RETRIEVE`. Als `ID` geht die **nackte GUID hinter der Pipe**.
*Behauptet wird:* **Alle drei Annahmen aus M59 (2) sind belegt und waren richtig.** Der Nullbefund
von M59 (2) liegt damit nicht an Endpunkt, Operation oder Kennungsform.

**Ein Indiz von mir ist widerlegt.** Aus der Tatsache, dass `FilestoreFile` `id` und `messageUUID`
als zwei getrennte Felder führt (`FilestoreFile.java:20`, `:23`), hatte ich geschlossen, `id` könne
der zusammengesetzte Verweis sein. `JsonServlet.java:793` befüllt **beide Felder mit derselben
nackten GUID**. Das Indiz war falsch und steht hier, weil sonst der Eindruck entstünde, es sei nie
eines gewesen.

**Vom Frontend geht der vollständige Verweis mit Pipe**, nicht die GUID: `MainController.js:958`
(Download) und `:457` / `:502` (Anzeige) nehmen `MessagePropertyValue` unverändert; gesendet an
`OverlordWebApplication/JsonServlet` in `:975`, `:462`–`:465`, `:508`–`:511`. **Das Frontend zerlegt
nicht** — die Pipe wird nur auf Vorhandensein geprüft (`MainController.js:451`, `:501`;
`mprocess_panel.js:192` und `msgInfo_popup.js:206` nur zur Zeilenfärbung). Die Zerlegung liegt
allein in `JsonServlet.java:784`.

## Q2 — Die Schnittregel des Altsystems

**Bedingung** (`JsonServlet.java:818`):
`propertyName != null && propertyName.contains("Log.GUID") && !role.equals("Admin")`.
Nur dann wird beschnitten; sonst läuft alles durch den `else`-Zweig `:843`–`:853` **vollständig**
hinaus.

**Algorithmus** (`:819`–`:842`): äußere Schleife über die Zeilen; bei einer Zeile mit
`***StartOfLog***` (`:820`) beginnt eine innere Schleife, die die **folgende** Zeile liest (`:822`)
und anhängt, solange sie nicht `***EndOfLog***` enthält (`:823`), sonst abbricht (`:837`). Danach
liest die äußere Schleife weiter (`:841`).

| Fall | Verhalten | Fundstelle |
|---|---|---|
| **Mehrere Markenpaare** | **Alle Blöcke werden aneinandergehängt** — die äußere Schleife läuft nach dem Abbruch weiter und beginnt bei der nächsten Startmarke einen neuen Block | `:841` in Verbindung mit `:820` |
| **Startmarke ohne Endmarke** | Die innere Schleife läuft bis Dateiende → **alles ab der Startmarke** wird ausgegeben | `:823`, Bedingung `line != null` |
| **Keine Startmarke** | Die innere Schleife wird nie betreten, `result` bleibt leer → **es wird nichts ausgegeben** | `:820` |

**Erkennung mit `contains`, nicht mit Gleichheit** (`:820`, `:823`) — eine Marke innerhalb einer
echoten Zeile greift also. **Die Marken selbst erscheinen nicht in der Ausgabe:** `:822` liest die
nächste Zeile, bevor angehängt wird, und die `EndOfLog`-Zeile bricht ab, statt angehängt zu werden.

*Gelesen war:* dieser Algorithmus. *Behauptet wird:* Das Altsystem beschneidet Protokolle für
Nicht-`Admin`, aber **nicht** nach der Regel „erste Start- bis nächste Endmarke". Ob die
konservative Regel des Auftrags übernommen wird, ist eine Entscheidung und steht unter „Offene
Punkte" 16.

## Q3 — Die Pfadmaskierung

Zwei Ersetzungen, `JsonServlet.java:824`–`:828`:
`/opt/txp/users/<dienstkonto>/` → `/IS/` und `/srv/lobster/IS/` → `/IS/`.

> **Maskiert am 17.08.2026.** Hier stand bis dahin der Dienstkontoname im Klartext. Er ist nach der
> Entscheidung des Auftraggebers, `alterCode/` nicht ins Repository aufzunehmen, auch hier ersetzt
> worden — sonst wäre die Ausnahme wirkungslos, weil dieselbe Zeichenfolge über diese Datei doch
> in die Historie gelangte. **Die Regel bleibt dokumentierbar:** Was zählt, ist, dass zwei feste
> Präfixe auf `/IS/` abgebildet werden, nicht wie das Konto heißt. Der genaue Wert steht im
> Quelltext an der genannten Stelle.

*Gelesen war:* Beide stehen **innerhalb** der inneren Schleife, also ausschließlich im beschnittenen
Zweig. Der `else`-Zweig `:843`–`:853` maskiert **nicht**.
*Behauptet wird:* Die Maskierung greift nur dort, wo ohnehin beschnitten wird. **`Admin` bekommt das
Protokoll vollständig und unmaskiert.**

## Q4 — Auslieferung

| Befund | Fundstelle |
|---|---|
| Die Datei kommt als **ZIP**; gelesen wird **nur der erste Eintrag**, weitere werden stillschweigend übergangen | `:801`–`:802` (`ZipInputStream`, einmaliges `getNextEntry()`) |
| Entpackt wird **im Servlet**, vor der Ausgabe; die Zwischendatei wird danach gelöscht | `:799`, `:801`, `:858` |
| Dekodiert wird hart mit **`ISO-8859-1`** | `:812`–`:813` |
| `response.setCharacterEncoding("UTF-8")` ist **auskommentiert** | `:808` |
| `Content-Type` ist **`application/octet-stream`** — für Anzeige *und* Download | `:807` |
| `Content-Disposition: attachment;filename=<msgID>` nur bei `download=true`; **der Dateiname ist die MessageID**, nicht der Originalname | `:809`–`:811` |
| Ausgabe in **500-Zeilen-Blöcken**, **keine Größenbegrenzung** in irgendeiner Form | `:831`, `:847` |

*Gelesen war:* dies. *Behauptet wird:* Die Frage „wo wird entpackt?" ist beantwortet — im Servlet,
ohne Behandlung mehrerer ZIP-Einträge und ohne Kappung.

## Q5 — Was das neue Werkzeug nicht übernimmt

Drei Befunde, nüchtern und mit Fundstelle:

| Befund | Fundstelle |
|---|---|
| Die Rolle, die über das Beschneiden entscheidet, stammt aus dem **Anfrageparameter** `downloadUser`, nicht aus der Sitzung | `:789`–`:790`, verwendet in `:818` |
| Der `datei`-Zweig liegt auf oberster Ebene neben der Anmeldung; die Sitzungsprüfung `:94`–`:100` greift **nur, wenn `sessionID` mitgeschickt wird**. Die Anzeige im ExtJS-Teil schickt sie nicht | `JsonServlet.java:94`, `:165`; `MainController.js:462`–`:465`, `:508`–`:511` |
| `mandant` (gesetzt in `:123`) wird in `getData` **nirgends verwendet** | `getData`, `:781`–`:861` |

> Diese Befunde betreffen das **laufende Altsystem**, nicht das neue Werkzeug. Ihre Behandlung ist
> nicht Gegenstand dieser Runde und liegt beim Auftraggeber.

**Was daraus für Schritt 8 folgt:** Regel M1 und die Mandantenprüfung im Statement (M58 (1)) bauen
an dieser Stelle **nichts nach, sondern ersetzen etwas**. Es gibt auf dem Dateipfad des Altsystems
keine Mandantentrennung, die zu übernehmen wäre.

---

## Q6 — Der Vertrag aus dem Altsystem *(zuvor Anhang QT)*

Dieser Teil stammt aus der Erhebung vom 14.08.2026 und ist dort aus `getData` und dem
Filestore-Client gelesen worden. **Die Kennzeichnung „ungeprüft übernommen" entfällt:** Vier seiner
Aussagen sind am 17.08.2026 aus der Quelle belegt worden.

| Befund | Beleg |
|---|---|
| **QT1** — Abrufweg, `RETRIEVE`, GUID hinter der Pipe | `JsonServlet.java:784`–`:786`, `:792`–`:797` (siehe Q1) |
| **QT4** — die Schnittregel ist **nicht** die konservative | `JsonServlet.java:819`–`:842`, insbesondere `:841` (siehe Q2) |
| **QT6** — fehlende Startmarke schließt zu, fehlende Endmarke öffnet auf | `JsonServlet.java:820` bzw. `:823` (siehe Q2) |
| **QT7** — Maskierung absoluter Pfade innerhalb der Marken | `JsonServlet.java:824`–`:828` (siehe Q3) |

QT2, QT3, QT5, QT8 und QT9 sind unverändert übernommen; ihre Belege stehen bei ihnen.

### QT1 — Der Abrufweg, vollständig *(beantwortet V3)*

Die Methode löst die Kennung wie dokumentiert auf (`url.split("\\|")` → Filestore und GUID,
`serviceBean.find(filestore).getServiceConnectString()`), baut daraus aber **keinen** einfachen
HTTP-Aufruf. Sie erzeugt ein `FilestoreClient` aus der Verbindungszeichenkette und einem lokalen
Ablageverzeichnis, hängt eine `FilestoreFile` mit der Aktion **`RETRIEVE`** an und ruft
`requestFilestoreActions()`.

**Der Quelltext des Clients ist am 14.08.2026 nachgereicht worden** (`alterCode/`,
`de.kraftwerkone.filestore.client`). Damit ist das Format auf der Leitung nicht mehr abzuleiten,
sondern gelesen: **SAAJ, SOAP 1.1 mit Anhängen (SwA)**, `SOAPHeader` ausdrücklich abgehängt.

**Die Anfrage** ist ein `FileList`-Element im Namensraum `http://filestore.kraftwerkone.de` — genau
das Element, das M59 als **leere** Antwort auf ein blankes `GET` gemessen hat — mit einem
`File`-Kind je Aktion. Bemerkenswert: **`File` und seine drei Attribute sind unqualifiziert**
(`addChildElement(new QName("File"))` ohne Namensraum), nur `FileList` trägt den Präfix `m`:

| Attribut | Inhalt |
|---|---|
| `Counter` | laufende Nummer ab `0` — zugleich die `Content-ID` des zugehörigen Anhangs |
| `ID` | die GUID hinter der Pipe aus `Message.Payload.GUID` |
| `Action` | `CREATE`, `APPEND`, `DELETE` oder **`RETRIEVE`** |

**Die Antwort** trägt dasselbe `FileList` mit `File`-Kindern, deren Attribute `Counter`, `ID` und
zusätzlich **`Response`** gelesen werden — dort steht offenbar der Erfolgsvermerk je Datei. **Der
Dateiinhalt kommt nicht im Umschlag**, sondern als **SOAP-Anhang**; der Client ordnet Anhang und
Anforderung über `Content-ID` gleich `Counter` zu und schreibt ihn unter
`<Ablageverzeichnis><messageUUID>.zip`.

> **Nur `CREATE` hängt etwas an.** Die `switch`-Anweisung erzeugt einen `AttachmentPart` — und damit
> überhaupt Nutzdaten in der Anfrage — ausschließlich im Zweig `CREATE`. `RETRIEVE` und `DELETE`
> haben leere Zweige. **Eine `RETRIEVE`-Anfrage kann konstruktionsbedingt nichts ablegen**, und
> damit ist die Sorge entkräftet, die den Abbruch von Teil B ausgelöst hat.

*Gelesen war:* dieser Aufbau. *Behauptet wird:* dass eine Anfrage dieser Gestalt eine Datei liefert
— **das ist nicht gemessen.** Der Aufruf gegen den laufenden Filestore ist an der
Berechtigungsprüfung der Entwicklungsumgebung gescheitert (siehe „Abweichungen vom Rahmen", Punkt 9).
Was fehlt, ist damit **eine einzige Freigabe**, nicht mehr eine Information.

### QT2 — Die Datei kommt als ZIP, nicht als Rohdatei

Der Client legt das Ergebnis unter `<Ablageverzeichnis><GUID>.zip` ab; die Methode öffnet es mit
einem `ZipInputStream` und liest **genau einen** Eintrag (`getNextEntry()` ohne Schleife).

**Die Gegenrichtung erklärt, warum.** `FilestoreFile.prepareFile()` packt vor einem `CREATE` jede
Datei in ein ZIP mit **genau einem** Eintrag, und dieser Eintrag trägt den **ursprünglichen
Dateinamen**. Die Bedingung dafür lautet `file.length() >= MAXSIZE_COMPRESS` bei
`MAXSIZE_COMPRESS = 0` — sie ist also **immer** erfüllt. **Jede** Datei im Filestore ist gepackt,
ohne Ausnahme und unabhängig von ihrer Größe.

> **Zwei nützliche Folgen.** Erstens: Der ZIP-Eintrag trägt den echten Dateinamen — die Quelle, aus
> der ein Download seinen Namen nehmen kann, statt ihn wie das Altwerkzeug aus einem Anfrageparameter
> zu übernehmen (QT8). Zweitens: „genau ein Eintrag" ist keine Annahme des Lesers, sondern die Zusage
> des Schreibers.

> ⚠️ **Damit ist die Bauform von M60 hinfällig, wie sie im Auftrag steht.** Ihre vorregistrierte
> Deutung lautet: „Stimmt `Content-Length` mit `FileReader.FileProperty.Size` überein, ist belegt,
> dass jene Spalte Bytes zählt." Wenn der Filestore ein ZIP liefert, ist `Content-Length` die
> **gepackte** Größe und kann mit einer Byteangabe der Rohdatei gar nicht übereinstimmen. Die Frage
> nach der Einheit von M56 (b) braucht deshalb einen anderen Vergleich — die Größe des
> **entpackten** Eintrags gegen die Spalte.

**Und es ist eine Vorgabe für Schritt 8, die im Implementierungsplan nicht steht:** Der Proxy muss
**entpacken**. Ein reines Durchreichen des Bytestroms liefert dem Nutzer ein ZIP, wo er eine
EDI-Datei erwartet.

### QT3 — Die Kodierung ist ISO-8859-1, und der Rückweg ist ungesichert

Der Datenstrom wird mit einem ausdrücklich auf **ISO-8859-1** gesetzten `CharsetDecoder` gelesen.
Die Zeile, die die Antwort auf UTF-8 setzen würde, steht als **Kommentar** daneben und ist nicht
aktiv. Ausgegeben wird mit `result.getBytes()` — also in der **Standardkodierung der Laufzeit**, die
nirgends festgelegt ist.

**Und es ist nicht die einzige Stelle.** `PayloadReader` — die Klasse, mit der die Plattform selbst
Nutzdaten liest — setzt in **beiden** Betriebsarten (`ZIPPED` und `UNZIPPED`) fest `"8859_1"`. Die
Annahme ist also nicht ein Versehen in der Anzeige, sondern durchgängig.

`PayloadWriter` nimmt die Kodierung dagegen als **Parameter** entgegen; welchen Wert die Aufrufer
übergeben, steht in den vorliegenden Klassen nicht.

*Gelesen war:* der fest verdrahtete ISO-8859-1-Dekodierer an drei Stellen und die auskommentierte
UTF-8-Festlegung. *Behauptet wird:* dass das Altwerkzeug Umlaute nur dann richtig zeigt, wenn die
Standardkodierung der Laufzeit zufällig passt — **nicht**, in welcher Kodierung die Dateien im
Filestore tatsächlich liegen. Genau das misst **M61**, und die Messung bleibt nötig: Der Quelltext
sagt, was das Altsystem **annimmt**, nicht was **zutrifft**. Sollte M61 Bytes über `0x7F` in
gültigem UTF-8 finden, zeigte das Altsystem sie heute falsch — und zwar unbemerkt, weil
ISO-8859-1 jedes Byte annimmt und nie scheitert.

### QT4 — Die Schnittregel des Altsystems ist **nicht** die konservative

Die äußere Schleife läuft nach dem Ende eines Abschnitts **weiter**. Trifft sie eine weitere
Startmarke, hängt sie den nächsten Abschnitt an dasselbe Ergebnis. Das Altsystem liefert also
**alle Abschnitte aneinander**.

> ⚠️ **Das ist genau die Fassung, die die Aufgabenstellung ausschließt.** Dort steht: „Die
> Schnittregel ist die konservativste mögliche: von der **ersten** Startmarke bis zur **nächsten
> darauffolgenden** Endmarke, nichts sonst. Nicht ‚alle Abschnitte aneinander' […] — beide ließen
> sich durch eine eingeschleuste Marke dazu bringen, Bereiche freizugeben, die außen liegen."
>
> Die geplante Regel ist damit **strenger als die heutige**. Das ist vertretbar und vermutlich
> richtig, aber es ist eine **Verhaltensänderung gegenüber dem, was Nutzer heute sehen**, und keine
> Nachbildung. Wie viel dabei wegfällt, sagt **M63** (mehr als ein Paar je Datei?) — und M63 ist
> nicht gefahren.

### QT5 — Die Markenerkennung ist `contains`, ohne Normalisierung

Geprüft wird mit `line.contains("***StartOfLog***")` beziehungsweise `…("***EndOfLog***")` — ein
Teilzeichenketten-Vergleich über die **ganze Zeile**, ohne Trimmen, ohne Groß-/Kleinschreibungs­regel,
ohne Verankerung am Zeilenanfang.

**Damit ist die Beeinflussbarkeit aus der Aufgabenstellung nicht mehr eine Ableitung, sondern
gelesen:** Eine echote Zeile, die die Markentexte irgendwo enthält — etwa ein Dateiname aus der
Nachricht —, öffnet oder schließt im Altsystem einen Abschnitt. Die Warnung des Auftrags trifft den
Mechanismus genau.

Nebenbei beantwortet das eine Teilfrage von M63: Nach abweichenden Schreibweisen sucht das
Altsystem **nicht**. Ob es welche gibt, bleibt ungemessen.

### QT6 — Fehlende **Start**marke schließt zu, fehlende **End**marke öffnet auf

Zwei Fälle, und sie verhalten sich gegenläufig:

| Fall | Was das Altsystem für `MANDANT` liefert |
|---|---|
| **keine Startmarke** | **nichts** — das Ergebnis bleibt leer |
| **Startmarke ohne Endmarke** | **alles ab der Startmarke bis zum Dateiende** |

Der zweite Fall entsteht so: Findet die innere Schleife keine Endmarke, liest sie bis `null` und
bricht dann über denselben `else`-Zweig ab — alle gelesenen Zeilen sind da bereits im Ergebnis.

> ⚠️ **Und das ist genau der Fall, um den M64 gebaut war.** Ein abgebrochener Lauf schreibt plausibel
> eine Startmarke und keine Endmarke. Das Altsystem zeigt dort **alles**; die geplante
> *fail-closed*-Regel zeigt **nichts**. Die Aufgabenstellung nennt das „den Hauptanwendungsfall" und
> verlangt, ihn vor dem Bau zu beziffern. **Beziffert ist er nicht** — M64 ist nicht gefahren. Was
> beziffert ist: **170 von 170** Fehlernachrichten im erreichbaren Zeitraum tragen ein Protokoll
> (M64a‑3), es geht also nicht um einen Randfall.

### QT7 — Das Altsystem maskiert absolute Pfade **innerhalb** der Marken

Im Innenbereich ersetzt es **zwei** absolute Pfadpräfixe durch dasselbe kurze Ersatzstück, bevor die
Zeile ins Ergebnis geht. Die beiden Regeln stehen als `if`/`else if` — eine Zeile, die beide Präfixe
enthält, wird nur einmal maskiert.

> **Damit ist die zweite vorregistrierte Zeile von M65 vorweggenommen, ohne sie zu ersetzen.** Sie
> lautet: „Nennenswert viele Pfade **innerhalb** → Der Beschnitt hält weniger zurück als gedacht."
> Dass das Altsystem eigens eine Maskierung **im Innenbereich** braucht, ist ein starkes Indiz, dass
> dort Pfade stehen — sonst wäre die Regel sinnlos. **Wie viele, sagt nur M65**, und M65 ist nicht
> gefahren.
>
> Für Schritt 8 heißt das: Der Beschnitt an den Marken **allein** ist nicht das, was das Altsystem
> tut. Es beschneidet **und** maskiert. Ob die neue Fassung diese Maskierung übernimmt, ist eine
> Entscheidung, die bisher niemand getroffen hat, weil niemand wusste, dass es sie gibt.

### QT8 — Drei Stellen, die das neue Werkzeug **nicht** nachbauen darf

Der Quelltext zeigt nebenbei, warum die Regeln aus Abschnitt 4 der Richtlinien so scharf formuliert
sind:

1. **Die Rolle kommt aus einem Anfrageparameter.** Der Benutzer wird über
   `request.getParameter("downloadUser")` nachgeschlagen und daraus die Rolle gelesen. Wer den
   Parameter setzt, wählt seine Rolle. Regel **M1** und die serverseitige Sitzung aus **A1**
   schließen genau das aus.
2. **Der Dateiname der Antwort kommt ungeprüft aus einem Anfrageparameter**
   (`Content-Disposition: attachment;filename=` + `request.getParameter("msgID")`). Ohne
   Maskierung ist das eine Kopfzeilen-Einschleusung.
3. **`Content-Disposition` wird nur gesetzt, wenn ein Parameter `download=true` ankommt.** Sonst
   geht die Antwort ohne Disposition heraus. Regel **R2** verlangt sie **immer** — eine EDI-Datei
   kann gültiges HTML oder SVG sein.

`response.setContentType("application/octet-stream")` entspricht dagegen Regel **R3**, und die
Rollenprüfung selbst (`role.equals("Admin")`) entspricht der geplanten Abstufung — mit dem Hinweis,
dass die Zeichenkette dort `Admin` lautet und in diesem Projekt `ADMIN`.

### QT9 — Auch `ADMIN` bekommt heute nicht die Rohdatei

Beide Zweige — der beschnittene für `MANDANT` **und** der vollständige — laufen über denselben
`BufferedReader`, dieselbe ISO-8859-1-Dekodierung und dieselbe Zusammensetzung mit `"\n"`. Das
Altwerkzeug liefert damit **in keinem Fall** die Bytes, die im Filestore liegen: Zeilenenden werden
auf `LF` vereinheitlicht, und die Kodierung läuft durch einen ungesicherten Rückweg (QT3). Dasselbe
gilt für **Nutzdaten**, denn dieselbe Methode bedient beide Artefaktarten — nur über den `else`-Zweig.

> **Zwei Folgen.** Erstens: Der in Schritt 8 geplante **Bytestrom-Proxy ist keine Nachbildung,
> sondern eine Verbesserung** — er liefert erstmals die Datei, wie sie liegt. Zweitens: **Ein
> Vergleich der neuen Ausgabe mit der alten wird Unterschiede zeigen**, und diese Unterschiede sind
> kein Fehler. Wer die Abnahme über einen Dateivergleich fahren will, muss das vorher wissen.

---

