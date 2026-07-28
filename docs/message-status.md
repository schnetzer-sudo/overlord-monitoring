# Statuskatalog `Message.MessageStatus`

Erhebung gegen die Testkopie, vorgezogen aus Schritt 3 (die Verbindung steht in Schritt 2 ohnehin).
Die Einordnung entsteht **ausschließlich** in `common/MessageStatusClassifier` und wird nirgends
nachgebaut — sie wird in Liste, Dashboard, Rollup und später im Chatbot verwendet.

`MessageStatus` ist **freier Text, kein Aufzählungstyp.** `CKECKED` ist der Beweis (ein nie
korrigierter Tippfehler in den Produktivdaten). Unbekannte Werte werden zu `UNGEKLAERT`, niemals zu
einem geratenen Wert.

---

## Erhobene Werte (seit 01.01.2025, Stand der Projektbeschreibung)

| Status | Anzahl | Einordnung (`MessageStatusKind`) | Anzeige |
|---|---|---|---|
| `FINISHED` | 1.663.884 | `ABGESCHLOSSEN` | grün |
| `MERGED` | 607.277 | `ZWISCHENSCHRITT` | neutral |
| `SPLITTED` | 310.263 | `ZWISCHENSCHRITT` | neutral |
| `EERP_RECEIVED` | 116.828 | `QUITTIERT` | grün |
| `COMMIT_RECEIVED` | 10.126 | `QUITTIERT` | grün |
| `COMMIT_SENT` | 979 | `UNGEKLAERT` | neutral |
| `ERROR_DUPLICATE` | 659 | **`FEHLER`** | rot |
| `SUSPENDED` | 538 | `WARTEND` | neutral |
| `CHECKED` | 257 | `UNGEKLAERT` | neutral |
| `COMMIT_REJECTED` | 111 | **`FEHLER`** | rot |
| `ERROR_TIMEOUT` | 52 | **`FEHLER`** | rot |
| `CKECKED` | 2 | `UNGEKLAERT` (Tippfehler) | neutral |
| `RUNNING` | 0 | `LAEUFT` | neutral |

13 dokumentierte Werte. Die `MessageStatusKind`-Einordnung ist in `MessageStatusClassifier`
hinterlegt.

## Nachprüfung am 28.07.2026 (unbefristet, ganze Testkopie)

`SELECT DISTINCT MessageStatus FROM Message` lieferte **12 Werte** — exakt die obige Menge **ohne
`RUNNING`**. Kein neuer, undokumentierter Wert. Die Anzahlen liegen höher als oben (die Testkopie
reicht jetzt bis 08.07.2026), aber die *Menge* der Statuswerte ist unverändert.

---

## Anmerkungen, die mitzudenken sind

- **`COMMIT_REJECTED` zählt als Fehler**, obwohl der Wert **nicht** mit `ERROR_` beginnt. Der Partner
  hat die Übertragung abgelehnt, der Beleg ist nicht angekommen. Damit ist Annahme A6 der
  Projektbeschreibung widerlegt (siehe `annahmen-korrekturen.md`).
- **`RUNNING` kommt in der Testkopie null Mal vor**, in der Produktion aber sehr wohl. Der Status ist
  flüchtig und existiert nur, solange eine Nachricht tatsächlich in Arbeit ist. Folgen:
  - Die Problemkategorie „Überfällig" ist gegen die Testkopie nicht prüfbar.
  - „läuft noch" darf **nicht** als `MessageStatus = 'RUNNING'` definiert werden, sondern als „nicht in
    einem Endstatus".
- **`CHECKED`, `CKECKED` und `COMMIT_SENT`** gelten als bekannt, aber fachlich **ungeklärt**. Sie
  werden neutral behandelt und mit Rohwert plus Hinweis „Bedeutung nicht verifiziert" angezeigt. Es
  wird nichts geraten.

---

## Die Fehlerbedingung — eine Stelle, ein Ausdruck

`MessageStatusClassifier.fehlerBedingung(Field<String>)` liefert:

```sql
MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus = 'COMMIT_REJECTED'
```

**Nicht** `LEFT(MessageStatus, 6) = 'ERROR_'`: In SQL ist `_` ein Platzhalter für ein beliebiges
Zeichen, und die `LEFT`-Form kann `MessageStatusIDX` nicht nutzen — sie erzwingt in Kombination mit
der Oder-Bedingung einen vollen Durchlauf über 2,9 GB. Die `LIKE`-Fassung ergibt zwei Indexbereiche,
die MariaDB zusammenführen kann.

Das Feld wird als Parameter übergeben, damit dieser gemeinsame Baustein nicht auf generierte
`jooq.glassfish`-Typen zugreifen muss — die Fehlerbedingung bleibt in `common`.

---

## Sicherung gegen neue Statuswerte

`DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge` vergleicht `SELECT DISTINCT
MessageStatus` gegen die bekannte Menge:

1. **Kein unbekannter Wert** darf auftauchen (`bekannt ⊇ vorhanden`) — die eigentliche Sicherung.
2. Die vorhandenen Werte sind **genau** die bekannte Menge ohne `RUNNING`.

Der Test wird rot, sobald im Altsystem ein neuer Status auftaucht **oder** ein dokumentierter Wert
verschwindet. Nur deshalb ist es vertretbar, unbekannte Werte neutral zu behandeln, ohne dass ein
neuer Status unbemerkt aus der Problemsicht verschwindet. Reagiert wird durch Pflege dieser Datei und
des `MessageStatusClassifier`, nie durch Raten.
