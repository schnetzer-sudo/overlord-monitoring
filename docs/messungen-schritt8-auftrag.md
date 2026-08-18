# Messrunde vor Schritt 8 — Aufgabenstellung, **Fassung 3**
### Ergänzung 5, 17.08.2026 — der Alt-Client über Stichproben

M71 hat belegt, dass der Abrufweg trägt. Die ausstehenden Messungen von Teil B brauchen
denselben Weg über mehrere Verweise.

**Freigegeben:** der unveränderte `FilestoreClient`, Operation ausschließlich RETRIEVE,
über die Stichproben der Messungen M66, M60, M61, M63, M64, M65, M67, M68. Sequenziell,
je Verweis bei der Ablage, die er nennt.

**M66 wird als erste gefahren**, mit 25 Nachrichten je Zeitscheibe statt 50. Begründung:
Sie beantwortet die Frage, ob zu einer Nachricht überhaupt eine Datei existiert, und
bestimmt damit die Stichprobenauswahl aller folgenden Messungen. 25 genügen dafür.

**Gesperrt:** CREATE, APPEND, DELETE. Jede Änderung an den vier Dateien unter `alterCode/`.
Jeder Rückfall auf die andere Ablage. Jede Wiederholung eines Fehlversuchs mit
abgewandelten Werten.


### Ergänzung 4, 17.08.2026 — ein Lauf des Alt-Clients gegen den Filestore

M70 hat zwölf Unterschiede zwischen dem handgebauten Envelope und SAAJ gezeigt, darunter
ein BOM vor dem Rumpf. Statt den Nachbau zu korrigieren, wird der echte Client verwendet.

**Freigegeben:** ein Lauf des unveränderten `FilestoreClient` gegen `FILESTOREPROD09`,
mit genau einem Verweis — dem vom Auftraggeber benannten, dessen Anzeige im Altwerkzeug
beobachtet wurde. Operation ausschließlich RETRIEVE.

**Gesperrt:** CREATE, APPEND, DELETE. Jede andere Kennung. Jede Wiederholung über einen
zweiten Verweis ohne neue Freigabe. Jede Änderung an den vier Dateien unter `alterCode/`.

### Ergänzung 3, 17.08.2026 — ein lokaler Lauf des Alt-Clients

Endpunkt, Operation und Kennungsform sind über Q1 belegt. Verbleibende Differenz zu M59 (2)
ist die Transportform; was SAAJ sendet, steht in keinem Quelltext.

**Freigegeben:** ein einmaliger Lauf des vorhandenen `FilestoreClient` gegen einen Lauscher
auf `127.0.0.1`, mit einer frei erfundenen GUID.

**Gesperrt bleibt:** jede Verbindung zum Filestore aus diesem Lauf, jede Änderung an den
vier Dateien unter `alterCode/`, jedes erneute Senden nach dem Vergleich.

### Ergänzung 17.08.2026 — eine einzige POST-Form für Teil B

Teil B ist bei M59 stehen geblieben: Der Filestore antwortet ausschließlich über SOAP,
und SOAP kennt nur POST. **Das Verb sagt nichts über die Operation** — entscheidend ist
die Operation im Envelope.

**Freigegeben ist genau eine Form:**

- Endpunkt: `<vollständige Adresse aus ServiceConnectString>`
- Operation: `<Name aus Schritt 1>`, Namensraum `<...>`
- Envelope: die Vorlage aus `<Datei>:<Zeile>` des Altwerkzeugs, unverändert bis auf
  die eingesetzte GUID
- Nur mit GUIDs aus der Stichprobe der jeweiligen Messung

**Gesperrt bleibt:** jede andere Operation, jeder selbst gebaute Envelope, jede
Abwandlung „zum Ausprobieren", jeder Aufruf ohne GUID aus der Stichprobe. Insbesondere
jede ablegende Operation. Findest du im Envelope ein Feld, dessen Bedeutung du nicht
kennst, füllst du es nicht — du hältst an und meldest.

**Der Envelope ist Bestandteil des Befundes**, nicht ein Werkzeugdetail: Er wird vor der
ersten Ausführung im Volltext in `docs/messungen-schritt8.md` aufgenommen, mit Herkunft.
Ohne ihn weiß später niemand, was gesendet wurde.

Die Holregel aus M68 gilt unverändert: geholt wird bei der Ablage, die der Verweis nennt.

Stand: 17.08.2026 · **Ersetzt Fassung 2 vom selben Tag.**
Ergebnisdatei: `messungen-schritt8.md` (entsteht beim Fahren dieser Runde)

**Diese Runde baut nichts und entscheidet nichts.** Die Lesarten stehen unten vor der Erhebung fest;
welche Bauform daraus wird, entscheidet der Auftraggeber danach.

---

## 0a. Was sich gegenüber Fassung 2 geändert hat

**Auslöser:** `FILESTOREPROD09` ist ebenfalls erreichbar. Damit stehen **zwei** Ablagen zur
Verfügung, und das ändert mehr als den Umfang der Stichprobe.

| | Änderung |
|---|---|
| **Rahmen Teil B** | Ziel sind **beide** Ablagen, `FILESTOREPROD09` und `FILESTOREPROD10` |
| **M59** | Wird **je Ablage** gefahren und die Ergebnisse werden gegeneinander gestellt. Aus einer Beobachtung an einem Knoten wird ein **Vergleich** |
| **M66** | Ebenfalls je Ablage — eine Aufbewahrungsfrist kann je Knoten verschieden sein |
| **M68** | **Neu: Sind die Ablagen austauschbar?** Der Kreuzabruf. Er beantwortet, ob die Kennung im Verweis überhaupt etwas bedeutet |
| **Stichprobenliste** | Trägt ab jetzt die **Ablagenkennung** neben der UUID. Ohne sie lässt sich nicht mehr sagen, wo eine Datei geholt werden muss |
| **Nummernbereich** | **M52 bis M68** statt M52 bis M67 |

> **Warum `FILESTOREPROD09` mehr wert ist als ein zweiter Datenpunkt.** Die Anwendung löst zur
> Laufzeit **elf** Ablagen über `ServiceConnectString` auf und muss mit allen dieselbe Sprache
> sprechen. Mit einem erreichbaren Knoten misst M59 „so ruft man **an diesem Knoten** ab". Mit zwei
> misst sie, ob die Abrufform **knotenübergreifend gleich** ist — und das ist die Frage, an der
> hängt, ob der Proxy einen Codepfad hat oder elf.
>
> `FILESTOREPROD09` ist zudem der Knoten, den `PROJEKTBESCHREIBUNG.md` §3.2 als Beispiel führt. Das
> kanonische Beispiel der verbindlichen Datei wird damit erstmals prüfbar.

---

## 0b. Was sich gegenüber Fassung 1 geändert hatte

Fassung 1 ist **nicht gefahren worden**. Die Nummern M52 bis M61 behalten trotzdem ihre Bedeutung,
damit nichts durcheinandergerät, falls sie bereits notiert wurden.

| | Änderung |
|---|---|
| **V1, V2** | **Beantwortet.** `FILESTOREPROD10` ist erreichbar, die Filestores sind Produktionskopien, lesender Zugriff ist erlaubt |
| **V3, V4, V5** | Von Fragen zu **Messungen** geworden — sie sind jetzt am erreichbaren Filestore selbst zu beantworten |
| **Teil B** | **Entsperrt.** Er ist damit der Schwerpunkt dieser Runde und nicht mehr ihr Anhang |
| **M53** | Erweitert um die Dimension `MandantID` — beantwortet ohne einen zweiten Filestore, ob mehrere Ablagen hochgefahren werden müssen |
| ~~**M62**~~ | **Ersetzt durch M65.** Die Frage lautete „Was steht in einem Protokoll?"; sie lautet jetzt „Was steht **innerhalb** und was **außerhalb** der Marken?" — das ist eine andere Messung, und die alte Nummer wird nicht stillschweigend umgewidmet |
| **M63 bis M67** | **Neu.** Markenbilanz je Familie, Markenbilanz bei Fehlernachrichten, Inhaltsklassen diesseits und jenseits der Marken, Deckungsgleichheit der beiden Kopien, Größe des Beschnitts |

### Die Regel, wie sie jetzt gilt

1. Ein Nutzer erreicht die Artefakte **genau der Nachrichten, die er ohnehin erreicht.** Die
   Mandantentrennung bleibt unverändert an der Nachricht und wird durch Schritt 8 **nicht**
   angefasst. Es entsteht keine dritte Ausnahme von Regel M1.
2. Innerhalb dieser Menge gibt es **keine zweite Berechtigungsstufe** für Dateien: Nutzdaten,
   umgewandelte Fassungen und Protokolle sind für beide Rollen erreichbar.
3. **Einzige Abstufung:** Bei Protokollen sieht `MANDANT` ausschließlich den Bereich zwischen
   `***StartOfLog***` und `***EndOfLog***`. `ADMIN` sieht die vollständige Datei.

---

## 1. Rahmen

### Teil A — SQL (unverändert gegenüber Fassung 1)

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-…` |
| Nachweis | `SELECT @@global.read_only` → **`1`**, **erste Abfrage jeder Sitzung**; in der Schlusssitzung erneut |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Sitzungen | sequenziell, jede eine eigene Verbindung, Serverzeit notieren |
| Client | `mysql`, `--ssl-mode=DISABLED` bzw. `--skip-ssl`, `--default-character-set=utf8mb4` |
| Grenze | `SET max_statement_time = 60` bei Zugriff auf `MessageProperty` |
| Laufzeit | `SET profiling = 1` / `SHOW PROFILES` |
| **S1** | ausschließlich `SELECT` |
| **L4** | Einstieg in `MessageProperty` immer über `Message.MessageID` aus einem Zeitfenster |
| **L9** | jedes Statement gegen `Message`/`MessageProperty` trägt ein Fenster. Ausnahme `Service` |
| **G1** | siehe unten |
| **L10** | jeder Befundsatz trägt *gemessen war X / behauptet wird Y* |

### Teil B — Filestore (neu)

| | |
|---|---|
| Ziel | **`FILESTOREPROD09` und `FILESTOREPROD10`**, beides Produktionskopien, ausschließlich lesend. Jede Messung von Teil B wird **je Ablage getrennt** ausgewiesen, niemals zusammengefasst |
| Werkzeug | `curl`, **niemals ein Browser.** Ein Browser rendert, und genau das schließt §7 aus |
| Kopfzeilenmessung | `curl -sS -o /dev/null -D - <adresse>` — Kopfzeilen lesen, Rumpf verwerfen |
| Inhaltsmessung | **ausschließlich über ein Skript, das zählt und klassifiziert.** Kein Dateiinhalt erscheint auf dem Bildschirm, in einer Zwischenablage oder in der Ergebnisdatei |
| Ablage | Heruntergeladene Dateien liegen in einem Verzeichnis, das am Ende der Runde **gelöscht** wird. Der Pfad wird in der Ergebnisdatei genannt, damit die Löschung nachvollziehbar ist |
| Stichprobe | je Messung benannt; die `MessageID` der Stichprobe wird **nicht** abgedruckt |

> **Teil B liest produktive Nutzdaten.** Das ist erlaubt (V2), aber es ändert die Beweisführung:
> Ein Befund in Teil B ist eine **Zählung über eine Stichprobe**, kein Vollbestand wie in Teil A.
> Jeder Satz braucht den Stichprobenumfang neben sich.

### Was unter G1 gezeigt werden darf

**Freigegeben** (Konfigurationsvokabular des Altsystems, in `README.md` so entschieden):
`Service.ServiceID` (`FILESTOREPROD09`, `FILESTOREPROD10`, `MPSERVICEPROD01`), `MessagePropertyName` in voller Länge,
`Service.Type`-Werte, `MessageActionID`, die Markentexte `***StartOfLog***` / `***EndOfLog***`.

**Gesperrt:** `ServiceConnectString` (Hostnamen — nur Länge und Gestalt), `ServiceName`
(Knotenkennung als `<Knoten>`), **jeder Dateiinhalt**, jeder Dateiname, jeder Partnername, jede
Belegnummer, jeder absolute Pfad, jede UUID.

> **Hinweis zum Beispielbild vom 14.08.2026.** Es trägt einen Partnernamen, einen Zieldateinamen und
> vier absolute Pfade mit Benutzerkennung. Nach der eigenen Regel gehört es **nicht unmaskiert** in
> `messungen-schritt8.md` oder eine Feature-Datei. Für die Dokumentation genügt eine Beschreibung
> der Struktur; das Bild selbst ist Arbeitsmaterial dieser Runde.

### Die drei Zeitfenster

| Fenster | Umfang | Herkunft |
|---|---|---|
| **A** | ein Tag, `2025-12-29 00:00:00` bis `2025-12-30 00:00:00`, n = 6.249 | wie M15/M17 |
| **B** | ein Monat, n = 214.330 | Grenzen **wörtlich** aus `messungen-schritt5.md` M17 übernehmen |
| **C** | ein Tag am Anfang des Bestands, `2024-10-01` bis `2024-10-02` | neu |

---

## V. Vorbedingung — Stand

| # | Frage | Stand |
|---|---|---|
| **V1** | Erreicht die Entwicklungsmaschine einen Filestore? | ✔ **Ja**, `FILESTOREPROD09` **und** `FILESTOREPROD10` |
| **V2** | Darf sie produktive Nutzdaten lesen? | ✔ **Ja**, Produktionskopien, lesend |
| **V3** | Wie wird eine Datei abgerufen? | → **M59** |
| **V4** | Aufbewahrungsfrist im Filestore, deckt sie die 22 Monate der Datenbank? | → **M66** |
| **V5** | Antwort auf eine nicht vorhandene Datei? | → **M59** |
| **V6** | **Neu:** Schreibt die **Plattform** die Marken oder das jeweilige **Programm**? | → **M63** misst die Wirkung. Die Ursache kann nur das Altsystem beantworten — und die Antwort entscheidet, ob die Marken verlässlich sind oder zufällig vorhanden |
| **V7** | **Neu:** Müssen weitere Filestores hochgefahren werden? | → **M53** beantwortet das, **bevor** einer hochgefahren wird |

---

## ⚠️ Der Befund, der die Schnittregel entscheidet

Das Beispielprotokoll gibt **innerhalb** der Marken Werte aus, die aus der Nachricht stammen —
`Message.DestinationFilename`, `Message.ReceivingPartner`, `Message.SourceMessageID`. Diese Werte
kommen aus der EDI-Datei und damit **vom Partner**.

*Gemessen ist:* Ein Protokoll der Familie `FileReader` enthält im Innenbereich drei aus der Nachricht
abgeleitete Werte. *Behauptet wird:* Der Protokollinhalt ist von außen **beeinflussbar**, und damit
sind es auch die Marken — ein Dateiname, der die Zeichenfolge `***EndOfLog***` oder
`***StartOfLog***` enthält, landet als echote Zeile im Protokoll.

**Drei Folgen, und sie sind Konstruktionsvorgaben, keine Messergebnisse:**

1. **Die Marken sind eine Lesbarkeitsregel, keine Vertraulichkeitsgrenze.** Was hinter ihnen liegt,
   darf für einen Mandantennutzer unangenehm, aber nicht gefährlich sein. Steht dort etwas, dessen
   Offenlegung schadet, trägt der Beschnitt es nicht — dann gehört das Protokoll für `MANDANT`
   ganz gesperrt.
2. **Die Schnittregel ist die konservativste mögliche:** von der **ersten** Startmarke bis zur
   **nächsten darauffolgenden** Endmarke, nichts sonst. Nicht „alle Abschnitte aneinander", nicht
   „erste Start- bis letzte Endmarke" — beide ließen sich durch eine eingeschleuste Marke dazu
   bringen, Bereiche freizugeben, die außen liegen.
3. **Ohne vollständiges Paar wird für `MANDANT` nichts geliefert**, mit benanntem Hinweis. Nicht
   „dann eben alles" — das ist der Fehlgriff, den die naheliegende Umsetzung macht.

---

## Teil A — SQL gegen die Testkopie

### M52 — `Service`: die Werte, nicht nur die Spalten

Unverändert gegenüber Fassung 1. Die Frage: Ist die Kennung aus `Message.Payload.GUID` eine
`Service.ServiceID`?

```sql
SELECT ServiceID,
       CHAR_LENGTH(ServiceID)                                AS id_laenge,
       ServiceID REGEXP '^[A-Z0-9_]+$'                       AS id_code_form,
       ServiceID REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                                                             AS id_uuid_form,
       ServiceTypeID, ServiceStatus, ServiceTimeout,
       (ServiceDefaultFileStore IS NULL OR ServiceDefaultFileStore = '') AS ohne_default,
       CHAR_LENGTH(ServiceConnectString)                     AS cs_laenge,
       ServiceConnectString LIKE 'http://%'                  AS cs_http,
       ServiceConnectString LIKE 'https://%'                 AS cs_https,
       CHAR_LENGTH(ServiceConnectString)
         - CHAR_LENGTH(REPLACE(ServiceConnectString, '/', '')) AS cs_schraegstriche,
       ServiceConnectString LIKE '%?%'                       AS cs_mit_fragezeichen,
       ServiceLastUpdate
FROM Service
ORDER BY ServiceTypeID, ServiceID;
```

> `ServiceName` **nicht** mitziehen. `ServiceConnectString` nur als Länge und Ja/Nein-Spalten.

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Lesbare Codes der Form `FILESTOREPROD<nn>` | Auflösung ist ein **Primärschlüsselzugriff**. Baubar wie geplant |
| UUIDs | Der dokumentierte Auflösungsweg ist unbelegt; es braucht zuerst eine Messung, welche Spalte trägt |
| `cs_http`/`cs_https` durchgängig | Der Proxy spricht HTTP. Der geplante Zuschnitt trägt |
| Weder noch | `ServiceConnectString` ist kein URL. Nicht raten (Q4) — M59 klärt es am lebenden Objekt |

> Das Beispielbild zeigt `ServiceID = MPSERVICEPROD01` in derselben Form wie `FILESTOREPROD09` und
> `FILESTOREPROD10`. Das
> macht die erste Zeile **plausibel**; ein Bildschirmfoto ist keine Messung, und M52 kostet
> Millisekunden.

---

### M53 — Welche Ablage gehört zu welchem Mandanten? *(erweitert)*

**Die Erweiterung ist der Punkt.** Sie beantwortet ohne Hochfahren, ob ein zweiter Filestore nötig
ist (V7).

```sql
SELECT pm.MandantID,
       SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1) AS filestore_kennung,
       COUNT(*)                                AS verweise,
       COUNT(DISTINCT mp.MessageID)            AS nachrichten,
       COUNT(DISTINCT mp.MessagePropertyName)  AS namen,
       SUM(s.ServiceID IS NULL)                AS nicht_aufloesbar
FROM Message m
JOIN Process p          ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm  ON pm.ProjectID = p.ProjectID
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
LEFT JOIN Service s
       ON s.ServiceID = SUBSTRING_INDEX(mp.MessagePropertyValue, '|', 1)
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY pm.MandantID, filestore_kennung
ORDER BY pm.MandantID, verweise DESC;
```

**Dreimal fahren: Fenster A, B und C.**

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Alle Mandanten verweisen auf eine der beiden erreichbaren Kennungen | **Kein weiterer Filestore nötig.** Die Runde ist vollständig fahrbar, und die Abnahme ebenso |
| Verschiedene Mandanten verweisen auf verschiedene Kennungen | Genau die vermutete Lage. Die Liste sagt, **welche** Ablagen hochzufahren sind — und der kleinste betroffene Mandant genügt für die Kontrolle nach L15 |
| Dieselbe Kennung, aber `nicht_aufloesbar > 0` schon in Fenster A | Der Auflösungsweg ist unvollständig. **Anhalten** |
| `= 0` in A und B, **`> 0` in C** | Die Ablagen **rotieren**. Alte Nachrichten sind nicht abrufbar; die Oberfläche braucht dafür einen benannten Zustand, und die Abnahme darf nicht an einer alten Nachricht hängen |

---

### M54 — Die vollständige Artefakt-Inventur, gestaltbasiert

**(a) über das Namensmuster**

```sql
SELECT mp.MessagePropertyName,
       COUNT(*)                                     AS zeilen,
       COUNT(DISTINCT mp.MessageID)                 AS nachrichten,
       COUNT(DISTINCT mp.MessagePropertyValue)      AS verschiedene,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue))    AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue))    AS laengster,
       SUM(mp.MessagePropertyValue LIKE '%|%')      AS mit_pipe
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;
```

**(b) über die Gestalt** — findet Verweise, die dem Namensmuster nicht folgen:

```sql
SELECT mp.MessagePropertyName,
       COUNT(*) AS zeilen, COUNT(DISTINCT mp.MessageID) AS nachrichten,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue)) AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue)) AS laengster
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  AND mp.MessagePropertyValue REGEXP
      '^[^|]+\\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
  AND mp.MessagePropertyName NOT LIKE '%.Payload.GUID'
  AND mp.MessagePropertyName NOT LIKE '%.Log.GUID'
GROUP BY mp.MessagePropertyName
ORDER BY zeilen DESC;
```

Fenster A und B.

**Vorregistrierte Deutung:** (b) leer → die Artefaktmenge darf über die zwei Namensendungen
definiert werden. (b) nicht leer → die Definition muss gestaltbasiert sein, sonst verschwinden
Artefakte **still**. In (a) `mit_pipe < zeilen` → der Code muss je Wert prüfen, nicht je Name.

---

### M55 — Wie viele Artefakte trägt eine Nachricht?

**(a) Verteilung**

```sql
SELECT verweise, COUNT(*) AS nachrichten FROM (
  SELECT m.MessageID, COUNT(mp.MessagePropertyName) AS verweise
  FROM Message m
  LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
       AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
         OR mp.MessagePropertyName LIKE '%.Log.GUID')
  WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  GROUP BY m.MessageID
) x GROUP BY verweise ORDER BY verweise;
```

**(b) je Mandant, getrennt nach Nutzdaten und Protokoll**

```sql
SELECT pm.MandantID,
       COUNT(DISTINCT m.MessageID)                          AS nachrichten,
       SUM(mp.MessagePropertyName LIKE '%.Payload.GUID')    AS nutzdaten_verweise,
       SUM(mp.MessagePropertyName LIKE '%.Log.GUID')        AS protokoll_verweise,
       COUNT(DISTINCT CASE WHEN mp.MessagePropertyName LIKE '%.Log.GUID'
                           THEN mp.MessageID END)           AS nachrichten_mit_protokoll
FROM Message m
JOIN Process p         ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
     AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
       OR mp.MessagePropertyName LIKE '%.Log.GUID')
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
```

Fenster A und B.

**Vorregistrierte Deutung:** Maximum ≤ 8 → flache Liste trägt. Maximum > 15 → Gruppierung nach
Schritt nötig; der Wunsch „Anzeige als Standardfall" trifft dann auf eine Auswahl, bevor überhaupt
etwas angezeigt wird. Nachrichten mit null Verweisen → der deaktivierte Knopf bekommt eine
bezifferte Häufigkeit. `nachrichten_mit_protokoll` bei einem Mandanten nahe null → die
Protokollansicht ist dort leer, und das muss **vorher** bekannt sein.

---

### M56 — Was die Datenbank vorab über Größe und Dateinamen weiß

**(a) welche Namen es gibt**

```sql
SELECT mp.MessagePropertyName,
       COUNT(*) AS zeilen, COUNT(DISTINCT mp.MessageID) AS nachrichten,
       MIN(CHAR_LENGTH(mp.MessagePropertyValue)) AS kuerzester,
       MAX(CHAR_LENGTH(mp.MessagePropertyValue)) AS laengster,
       SUM(mp.MessagePropertyValue REGEXP '^[0-9]+$') AS nur_ziffern
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  AND (mp.MessagePropertyName LIKE '%Size%'
    OR mp.MessagePropertyName LIKE '%Filename%'
    OR mp.MessagePropertyName LIKE '%FileProperty%')
GROUP BY mp.MessagePropertyName ORDER BY zeilen DESC;
```

**(b) die Größenordnung**

```sql
SELECT COUNT(*) AS zeilen,
       MIN(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS kleinster,
       AVG(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS mittel,
       MAX(CAST(mp.MessagePropertyValue AS UNSIGNED))  AS groesster,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) >    65536) AS ueber_64_kib,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) >  1048576) AS ueber_1_mib,
       SUM(CAST(mp.MessagePropertyValue AS UNSIGNED) > 10485760) AS ueber_10_mib
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  AND mp.MessagePropertyName = 'FileReader.FileProperty.Size';
```

**(c) die Endungen der Originaldateinamen — nur die Endung, und auch die gefiltert**

```sql
SELECT CASE WHEN endung REGEXP '^[a-z0-9]{1,5}$' THEN endung ELSE '<sonstige>' END AS endung,
       COUNT(*) AS zeilen
FROM (
  SELECT LOWER(SUBSTRING_INDEX(mp.MessagePropertyValue, '.', -1)) AS endung
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
    AND mp.MessagePropertyName = 'FileReader.FileProperty.OriginalFilename'
    AND mp.MessagePropertyValue LIKE '%.%'
) x GROUP BY 1 ORDER BY zeilen DESC;
```

> `^[a-z0-9]{1,5}$` ist kein Schönheitsfilter, sondern G1: Ohne echte Endung stünde hinter dem
> letzten Punkt ein Stück Dateiname, und darin kann eine Belegnummer stecken.

**Vorregistrierte Deutung:** Deckt `…FileProperty.Size` nur die FileReader-Nachrichten (M17: 69,6 %
in Fenster A), ist eine Vorabgröße **nicht** verlässlich verfügbar — die Kappung muss **während** des
Datenstroms greifen. `ueber_10_mib > 0` → die Kappung ist der Normalfall und keine Vorsicht.

> ⚠️ **Nicht belegt** ist damit, dass die Zahl **Bytes** zählt. Das entscheidet erst der Abgleich mit
> `Content-Length` in **M60**.

---

### M57 — An welchem Schritt hängt welches Artefakt?

```sql
SELECT mp.MessagePropertyName, mp.MessageActionID,
       COUNT(*) AS zeilen,
       SUM(ma.MessageID IS NULL) AS ohne_schrittzeile,
       COUNT(DISTINCT sa.SOSActionName) AS verschiedene_schrittnamen,
       SUM(sa.SOSID IS NULL) AS ohne_schrittnamen
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
LEFT JOIN MessageAction ma ON ma.MessageID = mp.MessageID
                          AND ma.MessageActionID = mp.MessageActionID
LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY mp.MessagePropertyName, mp.MessageActionID
ORDER BY mp.MessagePropertyName, mp.MessageActionID;
```

Der Join auf `SOSAction` läuft über **beide** Spalten von `MessageAction`, niemals über
`Message.SOSID` — M15 belegt das mit 100 % gegen 96,17 %. Fenster A, bei Auffälligkeit auch B.

**Vorregistrierte Deutung:** `ohne_schrittzeile = 0` und `ohne_schrittnamen` klein → jedes Artefakt
bekommt den lesbaren Schrittnamen aus Schritt 5. Andernfalls bleibt nur die technische Familie, und
die Grenze aus M15 (3) — Dienstnamen sind nicht zeigbar — ist im Auge zu behalten.
Dass `Message.Payload.GUID` auf Schritt `0` steht, ist bereits gemessen (M17 3) und wird nur
bestätigt: Die Originaldatei gehört in den **Kopf** der Detailansicht, nicht in die Zeitleiste.

---

### M58 — Kostet das etwas? (Regel L7)

Die drei Abfragen, die der Endpunkt fahren wird, je mit `EXPLAIN` und Laufzeit, beste von fünf nach
einem Aufwärmlauf:

1. **Artefaktliste einer Nachricht** — `MessageProperty` über `MessageID`, gefiltert auf die beiden
   Namensmuster, plus Mandantenprüfung **im Statement** (Regel M3)
2. **Auflösung einer Kennung** — `Service` über den Primärschlüssel
3. **Existenznachweis der Nachricht für den Mandanten**, falls er nicht aus dem vorhandenen
   `NachrichtendetailRepository` fällt

Je einmal für `NEXANS` und einen kleinen Mandanten (L15).

**Vorregistrierte Deutung:** Die erste Abfrage muss `Using index` auf dem Primärschlüssel von
`MessageProperty` zeigen und deutlich unter 50 ms bleiben — derselbe Zugriffsweg wie im
Nachrichtendetail. Tut sie das nicht, gehört die Namensbedingung nach dem Abruf in den Code.

---

### M64a — Kandidatenauswahl für die Fehlerprotokolle *(SQL-Teil von M64)*

```sql
SELECT m.MessageStatus, COUNT(DISTINCT m.MessageID) AS nachrichten,
       COUNT(DISTINCT CASE WHEN mp.MessagePropertyName LIKE '%.Log.GUID'
                           THEN mp.MessageID END) AS mit_protokoll,
       COUNT(DISTINCT mp.MessagePropertyName)     AS familien
FROM Message m
LEFT JOIN MessageProperty mp ON mp.MessageID = m.MessageID
     AND mp.MessagePropertyName LIKE '%.Log.GUID'
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
  AND (m.MessageStatus LIKE 'ERROR\_%' ESCAPE '\\'
    OR m.MessageStatus = 'COMMIT_REJECTED')
GROUP BY m.MessageStatus ORDER BY nachrichten DESC;
```

Fenster B, und zusätzlich **ohne Fenster** nicht fahren — die Fehlerzahlen des Gesamtbestands stehen
bereits in §4.1 der Projektbeschreibung (`ERROR_DUPLICATE` 659, `COMMIT_REJECTED` 111,
`ERROR_TIMEOUT` 52).

**Vorregistrierte Deutung:** Tragen Fehlernachrichten **kein** Protokoll, ist die Protokollansicht
gerade dort leer, wo sie am meisten gebraucht wird — und das wäre der schwerwiegendste Befund der
ganzen Runde für den Nutzen des Features.

---

## Teil B — Der Filestore

### M59 — Abruf, Antwortform und der Fehlerfall *(beantwortet V3 und V5)*

**Vollständig für beide Ablagen**, `FILESTOREPROD09` und `FILESTOREPROD10`, und die Ergebnisse
nebeneinander abgedruckt: Statuscode, `Content-Type`, `Content-Length`, `Content-Disposition`,
Authentifizierung ja/nein, Antwortzeit. Dazu **drei Fehlerfälle** je Ablage: eine erfundene UUID,
eine syntaktisch falsche UUID, eine leere Kennung.

**Die Gegenüberstellung ist der Zweck, nicht die Zugabe.** Zur Laufzeit löst die Anwendung **elf**
Ablagen auf und muss mit allen dieselbe Sprache sprechen. Zwei Knoten sind eine kleine Stichprobe —
aber sie ist der Unterschied zwischen „so ruft man an diesem Knoten ab" und „so ruft man ab".

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Beide Ablagen antworten in **Form und Fehlerverhalten gleich** | Der Proxy braucht **einen** Codepfad. Die Verallgemeinerung auf elf Knoten bleibt eine begründete Annahme, aber sie hat jetzt einen Beleg statt keinen |
| Sie unterscheiden sich | Der Proxy braucht Fallunterscheidung je Knoten — und damit ist der geplante Zuschnitt zu klein. **Das ist ein Anhaltegrund**, denn neun weitere Knoten sind ungesehen |

**Weitere vorregistrierte Deutung:** Ein gelieferter `Content-Type` wird **nicht** durchgereicht — §7 schreibt
`application/octet-stream` fest; gemessen wird er, um zu wissen, ob er überhaupt etwas trägt.
Antwortet eine nicht vorhandene Datei mit `200` und einer HTML-Seite, ist das der **wichtigste
Befund dieser Runde**: Der Proxy müsste dann am Rumpf erkennen, ob er eine Datei oder eine
Fehlerseite weiterreicht — und die Oberfläche könnte „Datei weg" nicht von „Filestore kaputt"
unterscheiden.

---

### M60 — Größenverteilung echter Dateien

Stichprobe über beide Familien (Nutzdaten und Protokoll), mindestens zwei Mandanten, mehrere
`Service.Type`. Nur `Content-Length`, kein Rumpf. Umfang: mindestens 200 Artefakte, gleichmäßig über
die in M54 gefundenen Namen verteilt.

**Vorregistrierte Deutung:** Diese Verteilung setzt die Kappungsgrenze der Anzeige und die
Größenbegrenzung des Downloads — **abgeleitet, nicht gesetzt.** Stimmt `Content-Length` mit
`FileReader.FileProperty.Size` überein, ist belegt, dass jene Spalte Bytes zählt; erst dann darf
M56 (b) als Byteangabe gelesen werden.

---

### M61 — Zeichenkodierung

Für dieselbe Stichprobe, **als Zählung**: BOM vorhanden? Bytes über `0x7F`? Gültiges UTF-8?
Deklariert ein UNB-Segment einen EDIFACT-Zeichensatz (`UNOA`/`UNOB`/`UNOC`/`UNOY`)? Zeilenende
`CRLF` oder `LF`? **Getrennt für Nutzdaten und Protokolle** — es ist nicht gesagt, dass beide
dieselbe Kodierung tragen.

**Vorregistrierte Deutung:** Alles gültiges UTF-8 **und** keine Bytes über `0x7F` → die Anzeige ist
kodierungsfrei, der Punkt entfällt. Andernfalls braucht sie eine **benannte und sichtbare**
Kodierungsentscheidung samt Umschalter. Still auf UTF-8 zu raten zeigt falsche Umlaute, als wären
sie richtig — der Q4-Fall in seiner unangenehmsten Form: Es sieht nicht kaputt aus.

---

### M63 — Markenbilanz je Familie *(neu)*

Über eine Stichprobe von Protokollen aus **jeder** in M54 gefundenen `*.Log.GUID`-Familie
(`FileReader`, `Converter`, `FTPSender`, `HTTPSender`, `DataWarehouse`, die Reader …), mindestens
30 je Familie, sofern vorhanden. Gezählt wird je Datei:

| Kennzahl | Warum |
|---|---|
| Vorkommen `***StartOfLog***` | 0 / 1 / mehr |
| Vorkommen `***EndOfLog***` | 0 / 1 / mehr |
| Paar vollständig und in richtiger Reihenfolge? | die Grundbedingung des Beschnitts |
| Marke mit abweichenden Leerzeichen oder Groß-/Kleinschreibung | ob ein exakter Vergleich genügt |
| Marke steht in einer erkennbar **echoten** Zeile (Zeile enthält zusätzlich `=` oder `Message.`) | der eingeschleuste Fall |

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Genau ein Paar in **allen** Familien | Der Beschnitt ist verlässlich. Er bleibt trotzdem *fail closed*, weil die Marken beeinflussbar sind |
| Eine Familie ohne Marken | Für sie gibt es keinen Beschnitt. Dann ist ihr Protokoll für `MANDANT` **gesperrt**, nicht „ungeschnitten sichtbar" |
| Mehr als ein Paar | Die Regel „erste Start- bis nächste Endmarke" liefert nur einen Ausschnitt. Ob das genügt, entscheidet der Auftraggeber — **nicht** durch Aneinanderhängen aller Abschnitte |
| Abweichende Schreibweisen | Ein exakter Vergleich scheitert still. Die Erkennung braucht dann eine benannte Normalisierung, und die gehört gemessen |

---

### M64 — Markenbilanz bei **Fehlernachrichten** *(neu, Fortsetzung von M64a)*

Dieselben Kennzahlen wie M63, aber über Protokolle von Nachrichten mit `ERROR_*` oder
`COMMIT_REJECTED`. Umfang: alles, was M64a findet, bis maximal 200 Dateien.

> **Warum das eine eigene Messung ist.** Ein abgebrochener Lauf schreibt plausibel eine Startmarke
> und keine Endmarke. Bei *fail closed* sähe ein Mandantennutzer dann **nichts** — und zwar genau
> bei den Nachrichten, deretwegen er das Protokoll überhaupt öffnet. Das ist kein Randfall, sondern
> der Hauptanwendungsfall, und er ist vor dem Bau zu beziffern.

**Vorregistrierte Deutung:** Ist die Paarquote bei Fehlernachrichten deutlich niedriger als in M63,
braucht der Fall eine eigene Behandlung — entweder eine benannte Ersatzausgabe („Der Lauf ist
abgebrochen; der lesbare Teil ist unvollständig" mit dem Teil ab der Startmarke) oder die Freigabe
des vollständigen Protokolls für diesen Fall. Beides ist eine Entscheidung, keine Messung.

---

### M65 — Was steht innerhalb, was außerhalb? *(neu; ersetzt M62)*

Über dieselbe Stichprobe wie M63, **klassifizierend gezählt**, je Bereich getrennt:

| Klasse | Erkennung |
|---|---|
| absolute Pfade | Zeile enthält `/opt/`, `/var/`, `/usr/` oder vergleichbar |
| Dienstkennungen | Zeile enthält eine Zeichenfolge der Form `[A-Z]+PROD[0-9]+` |
| Hostnamen / Knoten | Zeile enthält einen Punkt-getrennten Bezeichner ohne Dateiendung |
| **aus der Nachricht echote Werte** | Zeile beginnt mit `Message.` |
| UUIDs | kanonische Form |
| Zeitstempel | `Program start` / `Program end` |

**Ausgegeben werden ausschließlich Zählungen je Klasse und Bereich. Kein Inhalt.**

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Pfade und Dienstkennungen fast nur außerhalb | Die Marken taugen als **Lesbarkeitsregel** — sie entfernen genau das Beiwerk, das der Leitsatz ohnehin wegwünscht. Die Regel wie beschrieben ist baubar |
| Nennenswert viele Pfade **innerhalb** | Der Beschnitt hält weniger zurück als gedacht. Er bleibt eine Lesbarkeitsregel, aber die Beschreibung „Mandantentrennung" wäre dann irreführend und gehört korrigiert |
| `Message.`-Zeilen innerhalb | **Erwartet** (im Beispiel drei). Bestätigt die Beeinflussbarkeit und damit die konservative Schnittregel |
| Etwas **außerhalb**, dessen Offenlegung schadet | Dann ist der Beschnitt keine Milderung, sondern eine Notwendigkeit — und der Download muss für `MANDANT` dieselbe beschnittene Fassung liefern oder gesperrt sein |

---

### M66 — Decken sich die beiden Kopien? *(neu; beantwortet V4)*

Der Screenshot stammt aus der Produktion, der Filestore ist eine **Kopie**, die Datenbank ist eine
**andere** Kopie mit Stand 08.07.2026. Ob beide dieselben Artefakte tragen, ist nicht gesagt.

Stichprobe von je 50 Nachrichten aus **fünf** Zeitscheiben: `2024-10`, `2025-04`, `2025-12`,
`2026-05` und die letzte Woche vor `2026-07-08`. Je Nachricht: Existiert die Datei zu
`Message.Payload.GUID`? Zu den `*.Log.GUID`?

**Getrennt je Ablage ausweisen.** Eine Aufbewahrungsfrist kann je Knoten verschieden eingestellt
sein; eine Zusammenfassung über beide verstiege sich zu einer Aussage über „den Filestore", die aus
den Zahlen nicht folgt. Geholt wird **immer bei der Ablage, die der Verweis nennt** — siehe M68.

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Alle fünf Scheiben vollständig | Der Filestore hält mindestens 22 Monate. Annahme A-neu: keine eigene Aufbewahrungsgrenze. Die Abnahme darf an jeder Nachricht hängen |
| Die ältesten Scheiben leer | Der Filestore hat eine **kürzere** Frist als die Datenbank. Dann trägt ein bezifferbarer Teil der Nachrichten einen Knopf, hinter dem nichts liegt — das ist ein Oberflächenzustand und ein Eintrag in die Annahmenliste |
| Die **jüngste** Scheibe unvollständig | Die Kopien laufen auseinander. Für die Abnahme ist dann eine Nachricht zu wählen, die in beiden liegt, und das ist zu notieren, damit es niemand später für einen Fehler hält |

---

### M67 — Wie viel nimmt der Beschnitt weg? *(neu)*

Über die Stichprobe aus M63: Größe der ganzen Datei gegen Größe des Innenbereichs, in Bytes und
Zeilen, je Familie.

**Vorregistrierte Deutung:** Bleibt vom Protokoll nach dem Beschnitt nur ein Bruchteil, ist die
Anzeige für `MANDANT` kurz und ein Ausklapp-Block im Detail genügt. Ist der Innenbereich der
überwiegende Teil, braucht auch die Mandantensicht die volle Anzeigefläche — und die Frage nach
Panel gegen eigene Route stellt sich für **beide** Rollen gleich.

---

### M68 — Sind die Ablagen austauschbar? *(neu)*

**Die Regel für alle übrigen Messungen von Teil B lautet: Es wird immer bei der Ablage geholt, die
der Verweis nennt.** Niemals bei der gerade bequemeren. Ein Rückfall auf die andere Ablage, wenn die
erste nichts liefert, würde M53 und M66 stillschweigend entwerten — er verwandelte „die Datei ist
weg" in „die Datei war woanders", ohne dass es jemand sähe.

**M68 ist die eine, bewusste Ausnahme.** Zehn Verweise, die auf `FILESTOREPROD09` zeigen, werden
zusätzlich bei `FILESTOREPROD10` abgerufen, und zehn Verweise auf `FILESTOREPROD10` zusätzlich bei
`FILESTOREPROD09`. Gemessen wird ausschließlich der Statuscode und die `Content-Length` — **kein
Rumpf, kein Inhaltsvergleich.**

**Vorregistrierte Deutung**

| Ergebnis | Was daraus folgt |
|---|---|
| Der Kreuzabruf schlägt durchgehend fehl | Die Kennung im Verweis **bedeutet etwas**. Die Auflösung über `Service` ist tragend, und sie richtig zu bauen ist Pflicht, nicht Höflichkeit |
| Der Kreuzabruf gelingt durchgehend, gleiche `Content-Length` | Die Ablagen sind **Spiegel**. Die Auflösung über `Service` wäre dann heute funktional entbehrlich — sie wird **trotzdem gebaut**, weil elf Zeilen in `Service` stehen und ein Spiegel eine Betriebsentscheidung ist, die sich ändern kann. Der Befund gehört aber dokumentiert, sonst hält ihn später jemand für eine Zusage |
| Er gelingt teilweise | Der unangenehmste Fall: teilweise gespiegelt, teilweise nicht. Dann ist jede Aussage über Vorhandensein aus M66 **je Ablage** zu lesen und nicht über beide |

> **Warum das eine eigene Nummer bekommt und keine Fußnote bei M59.** Die Frage „bedeutet die
> Kennung etwas?" hat eine eigene Antwort, ein eigenes Statement und eine eigene Deutung. In eine
> Fußnote gepackt wäre sie das, was dieses Projekt an anderer Stelle schon einmal teuer bezahlt hat:
> eine Angabe ohne Herkunft, die später als Tatsache gelesen wird.

---

## Sitzungsplan

**Teil A**

| Sitzung | Inhalt | erwartete Kosten |
|---|---|---|
| 1 | `read_only`, Serverzeit, **M52** | Millisekunden |
| 2 | **M53** Fenster A und C | Sekunden |
| 3 | **M53** Fenster B | zweistellige Sekunden erwartbar |
| 4 | **M54** (a) und (b), Fenster A und B | ~1,2 s in A, ~7,6 s in B (M17-Vergleich) |
| 5 | **M55**, **M56**, Fenster A und B | Sekunden bis zweistellige Sekunden |
| 6 | **M57**, **M58**, **M64a**, Abschluss-`read_only` | Sekunden |

**Teil B** — erst nach Sitzung 2, weil M53 sagt, ob über `FILESTOREPROD09` und `FILESTOREPROD10`
hinaus weitere Ablagen nötig sind.

| Lauf | Inhalt |
|---|---|
| 1 | **M59** — Abruf und Fehlerfälle, **beide Ablagen**. Ohne dieses Ergebnis funktioniert kein weiterer Lauf |
| 2 | **M68** — Kreuzabruf. Vorgezogen, weil er die Holregel aller folgenden Läufe bestätigt oder widerlegt |
| 3 | **M66** — Deckungsgleichheit, je Ablage. Beeinflusst die Stichprobenauswahl aller weiteren Läufe |
| 4 | **M60**, **M61** — Größen und Kodierung |
| 5 | **M63**, **M65**, **M67** — Marken, Inhaltsklassen, Beschnittgröße |
| 6 | **M64** — Fehlerprotokolle |
| — | Arbeitsverzeichnis löschen, Löschung in der Ergebnisdatei vermerken |

Sequenziell. Wird `max_statement_time = 60` erreicht, wird das **als Befund notiert** und nicht durch
Hochsetzen umgangen.

---

## Was diese Runde nicht tut

- **Sie baut nichts** und ändert keine Zeile vorhandenen Codes.
- **Sie entscheidet nichts.** Die Lesarten stehen oben vor der Erhebung fest.
- **Sie klärt den Widerspruch zwischen `PROJEKTBESCHREIBUNG.md` §9 und dem Implementierungsplan
  nicht.** §9 schließt die **aufbereitete** Anzeige aus, der Plan **jede**. Nach der Präambel gilt
  §9. Das ist eine Dokumentationsfrage, keine Messfrage — und sie ist datiert zu entscheiden.
- **Sie beantwortet V6 nicht.** Ob die Marken von der Plattform oder vom einzelnen Programm
  geschrieben werden, sieht man den Dateien nicht an. M63 misst die **Wirkung**; die **Ursache**
  kann nur das Altsystem liefern, und davon hängt ab, ob die Marken eine Zusage oder ein Zufall sind.

## Was dabei **entschieden** werden muss, sobald die Zahlen vorliegen

Vier Punkte, die aus dem Beispielbild folgen und keine Messung brauchen — sie stehen hier, damit sie
nicht zwischen den Zahlen untergehen:

1. **Der Download bei Protokollen.** Liefert er für `MANDANT` dieselbe beschnittene Fassung, oder ist
   er bei Protokollen `ADMIN` vorbehalten? „Beschneiden und trotzdem vollständig herunterladbar" ist
   keine der beiden Möglichkeiten.
2. **Fail closed.** Ohne vollständiges Markenpaar liefert der Protokollpfad für `MANDANT` nichts,
   mit benanntem Hinweis.
3. **Der Protokollpfad ist kein Bytestrom-Proxy.** Für `MANDANT` muss das Backend lesen, schneiden
   und neu ausgeben — das ist ein anderer Endpunkt als der Download aus dem Plan.
4. **Die Protokollierung im `audit_log`** muss festhalten, **welche Fassung** ausgeliefert wurde.
   Ein Eintrag, der beschnitten und vollständig nicht unterscheidet, ist bei einer Rückfrage wertlos.
