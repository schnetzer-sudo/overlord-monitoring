# Aufträge — Schnittstelle für die EDI-Plattform

*Stand 16.10.2026, Teilschritt A1.* Diese Datei ist der Vertrag zwischen Overlord Monitoring und der
EDI-Plattform. Sie geht an das Plattform-Team und ist ohne die übrige Dokumentation verständlich.
Hintergrund und Entscheidungen stehen auf Seiten des Monitors in [`tasks.md`](tasks.md).

---

## 1. Worum es geht

Kunden beauftragen im Monitor wiederkehrende Konfigurationsänderungen an der EDI-Plattform — zum
Beispiel Einträge ihrer Werk-/Abladestellentabelle anlegen, anpassen oder löschen. Der Monitor prüft
die Angaben und legt je Auftrag eine Zeile in der Tabelle `overlord_monitor.task` an. **Die
Plattform holt offene Aufträge dort ab, führt sie aus und meldet je Eintrag zurück.** Der Kunde sieht
das Ergebnis im Monitor.

Welche Art Auftrag eine Zeile ist, sagt die Spalte `task_typ`. Je Typ baut die Plattform einen
eigenen Ablauf.

---

## 2. Verbindung und was erlaubt ist

Die Plattform verbindet sich mit dem Datenbankbenutzer **`monitor_write`** und arbeitet
ausschließlich auf der Tabelle **`overlord_monitor.task`**. Zugangsdaten übergibt die Betreuung
gesondert; sie stehen in keinem Dokument.

**Der Vertrag erlaubt der Plattform genau drei Dinge:**

1. offene Aufträge lesen,
2. einen Auftrag abholen,
3. einen abgeholten Auftrag zurückmelden.

**Alles andere ist untersagt, auch wenn die Rechte es zuließen.** `monitor_write` hat alle Rechte
auf `overlord_monitor`, einschließlich der Benutzerkonten und Sitzungen des Monitors. Die Plattform
legt keine Aufträge an, ändert `parameter` nicht, ändert keine anderen Spalten als die in §3
genannten und fasst keine andere Tabelle an.

---

## 3. Die vier Statements

Wörtlich diese. Sie stehen so im Auftrag an die Plattform-Entwicklung vom 16.10.2026 und laufen so
in den Tests des Monitors. Ändert sich eins, ist das eine Änderung dieses Vertrags.

```sql
SELECT task_id, mandant_id, task_typ, aktion, parameter
FROM overlord_monitor.task
WHERE status = 'OFFEN'
ORDER BY eingestellt_am, task_id;

UPDATE overlord_monitor.task
SET status = 'IN_AUSFUEHRUNG', abgeholt_am = UTC_TIMESTAMP(3)
WHERE task_id = ? AND status = 'OFFEN';

UPDATE overlord_monitor.task
SET status = 'ERLEDIGT', rueckgabe = ?, ausgefuehrt_am = UTC_TIMESTAMP(3)
WHERE task_id = ? AND status = 'IN_AUSFUEHRUNG';

UPDATE overlord_monitor.task
SET status = 'FEHLGESCHLAGEN', fehlercode = ?, fehlertext = ?,
    ausgefuehrt_am = UTC_TIMESTAMP(3)
WHERE task_id = ? AND status = 'IN_AUSFUEHRUNG';
```

1. **Offene Aufträge lesen.** Ältester zuerst. Ein Ablauf je Typ darf die Abfrage um
   `AND task_typ = ?` ergänzen.
2. **Abholen.** Meldet das Update **eine** geänderte Zeile, gehört der Auftrag diesem Lauf. Meldet
   es **null**, hat ihn schon jemand abgeholt — dann nicht ausführen. So wird kein Auftrag doppelt
   ausgeführt, auch wenn zwei Läufe gleichzeitig lesen.
3. **Zurückmelden, ausgeführt:** `ERLEDIGT` mit der Rückgabe (§5).
4. **Zurückmelden, nicht ausführbar:** `FEHLGESCHLAGEN` mit Fehlercode und Fehlertext (§6).

Alle Zeitstempel sind UTC und entstehen in der Datenbank über `UTC_TIMESTAMP(3)`; die Plattform
übergibt keine Uhrzeit.

---

## 4. Was ein Auftrag bedeutet

- **`mandant_id` bestimmt, wessen Konfiguration der Ablauf ändert.** Der Mandant wird nie aus dem
  Parametertext abgeleitet.
- **`task_typ` bestimmt den Ablauf.**
- **`aktion`** ist die Aktion des Auftrags; sie steht zusätzlich in der ersten Zeile des
  Parametertexts.
- **`parameter`** ist der Parametertext:

  - erste Zeile `Aktion=<Aktion>`,
  - danach **eine Zeile je Eintrag**, aufgebaut nach der Vorlage des Typs,
  - Kodierung UTF-8, Zeilenende LF, **kein** Zeilenende nach der letzten Zeile.

  Ein Auftrag trägt genau eine Aktion und mindestens einen Eintrag. Der Monitor hat jeden Wert
  vorher geprüft: Kein Wert enthält einen Zeilenumbruch, ein Steuerzeichen, den Schlüsseltrenner des
  Typs oder ein Zeichen, das die Zeilenvorlage selbst als Trenner benutzt. Eine Zeile lässt sich
  deshalb an diesen Trennern sicher zerlegen.

---

## 5. Die Rückgabe

**Probleme einzelner Einträge gehören in die Rückgabe, mit Status `ERLEDIGT`.** Die Rückgabe hat
**eine Zeile je Eintrag, in derselben Reihenfolge** wie die Einträge im Parametertext (die Kopfzeile
zählt nicht mit), getrennt durch LF. Jede Zeile enthält einen der Rückgabewerte des Typs.

Der Monitor liest so:

- Er trennt an LF, entfernt ein CR am Zeilenende und Leerraum am Rand. Ein LF nach der letzten Zeile
  schadet nicht.
- Eine Zeile mit dem Erfolgswert des Typs heißt Ok, eine mit einem anderen bekannten Wert heißt
  nicht Ok, und jeder andere Text gilt ebenfalls als nicht Ok und wird dem Kunden roh gezeigt.
- Passt die Zahl der Zeilen nicht zur Zahl der Einträge oder fehlt die Rückgabe, zeigt der Monitor
  den Auftrag als unstimmig.

**Die Plattform setzt nie „teilweise erledigt“.** Den Gesamtstand leitet der Monitor ab: alle Ok
heißt erledigt, mindestens einer nicht Ok heißt teilweise erledigt. Die Plattform setzt nur
`ERLEDIGT` oder `FEHLGESCHLAGEN`.

---

## 6. Fehlgeschlagen

**`FEHLGESCHLAGEN` heißt: Der Auftrag als Ganzes war nicht ausführbar** — etwa weil der
Parametertext nicht lesbar war oder das Zielsystem nicht erreichbar. Dazu gehören:

- `fehlercode` (höchstens 64 Zeichen): ein kurzer, stabiler Code der Plattform;
- `fehlertext` (höchstens 1.000 Zeichen): ein Klartext.

**Den `fehlertext` sieht der externe Kunde.** Keine Stacktraces, keine Pfade, keine Server- oder
Ablaufnamen, keine internen Kennungen. Ein Satz, der dem Kunden sagt, was passiert ist — „Die
Tabelle konnte nicht gespeichert werden. Bitte wenden Sie sich an die Betreuung.“

---

## 7. Neue Typen

Die Betreuung (Lukas) legt neue Typen per SQL im Monitor an. **Die Plattform bekommt die Definition,
bevor der Typ aktiviert wird, und baut ihren Ablauf.** Erst danach wird der Typ aktiviert und für
Mandanten freigeschaltet. Ein aktivierter Typ ohne Ablauf führt zu Aufträgen, die niemand abholt.

Eine Definition nennt je Typ: die Aktionen, die Felder, den Aufbau des Schlüssels mit seinem
Trenner, die Zeilenvorlage je Aktion und die Rückgabewerte (Erfolgswert und bekannte andere Werte).

---

## 8. Beispiel: der Typ `WERK_ABLADESTELLE`

**Anpassen der Werk-/Abladestellentabelle**, freigeschaltet für `NEXANS` und `IBIS`.

Ein Eintrag besteht aus einem **Schlüssel** und zwei Werten. Der Schlüssel ist `Werk/Abladestelle`
oder `Lieferantennummer/Werk/Abladestelle`, Trenner `/`. Die Werte sind **SNDPRN** und
**SNDPRN_4908**, jeweils leer oder sechs Ziffern. Kein Schlüsselteil enthält `/`, `,` oder
Leerraum; jeder ist 1 bis 35 Zeichen lang, Groß- und Kleinschreibung wie eingegeben.

| Aktion | Zeile je Eintrag |
|---|---|
| `Neuanlage` | `<Schlüssel>,<SNDPRN>,<SNDPRN_4908>` |
| `Anpassung` | `<Schlüssel>,<SNDPRN>,<SNDPRN_4908>` |
| `Löschung` | `<Schlüssel>` |

**Neuanlage**

```
Aktion=Neuanlage
72332/DDB0,120764,
72261/DDR0,120764,
010/18VZE,120055,100050
0101159903/22/001N0,123085,
```

**Löschung**

```
Aktion=Löschung
010/18VZE
0101159903/22/001N0
```

**Anpassung**

```
Aktion=Anpassung
010/18VZE,123456,123456
0101159903/22/001N0,234567,
```

**Bedeutung:**

- **Neuanlage:** mindestens einer der beiden Werte ist gesetzt. **Ist der Schlüssel schon
  vorhanden, bleibt der Eintrag unverändert**, und die Rückgabe lautet
  `Schlüssel bereits vorhanden`.
- **Anpassung:** Der Schlüssel benennt einen vorhandenen Eintrag. **Ein leerer Wert heißt „wird
  entfernt“**, nicht „bleibt wie er ist“: In `0101159903/22/001N0,234567,` wird SNDPRN auf `234567`
  gesetzt und SNDPRN_4908 entfernt. Mindestens einer der beiden Werte ist immer gesetzt.
- **Löschung:** nur der Schlüssel.
- Ein Auftrag hat höchstens 20 Einträge (seit 16.10.2026, E‑1058; vorher 50); kein
  Schlüssel kommt in einem Auftrag zweimal vor.

**Die drei Rückgabewerte:**

| Wert | Bedeutung |
|---|---|
| `Ok` | ausgeführt |
| `Schlüssel nicht gefunden` | Anpassung oder Löschung eines Schlüssels, den es nicht gibt |
| `Schlüssel bereits vorhanden` | Neuanlage eines Schlüssels, den es schon gibt; der Eintrag bleibt unverändert |

Beispiel für einen Auftrag mit drei Einträgen:

```
Ok
Schlüssel nicht gefunden
Ok
```
