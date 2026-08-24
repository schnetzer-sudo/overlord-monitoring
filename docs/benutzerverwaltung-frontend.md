# Benutzerverwaltung — die Oberfläche

Stand: 24.08.2026 · Schritt 9a, Teil Frontend
Vorgabe: [`benutzerverwaltung.md`](benutzerverwaltung.md) (E1–E21, **E19 bis E21 betreffen genau
diese Oberfläche**) · Backend: [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md)
Unterbau: [`frontend-grundlagen.md`](frontend-grundlagen.md) · Gestalt:
[`visuelles-konzept.md`](visuelles-konzept.md)

**Mit diesem Teil ist Schritt 9 gebaut.** Was zur Abnahme noch fehlt, steht in §12 und in
[`README.md`](README.md) unter „Offene Sichtprüfungen".

---

## 1. Zweck

**Über zwanzig externe Nutzer ohne Datenbankzugang bekommen gepflegte Konten** (E1). Jedes
vergessene Passwort, jede Sperre, jeder Austritt landete sonst per Zuruf beim Betreiber.

Die Seite beantwortet vor allem eine Frage, und sie ist der Grund für fast jede Entscheidung
darin: **„Warum kommt der da nicht hinein?"** Jemand ruft an, und der Admin muss zwischen drei
Ursachen unterscheiden können — ich habe dich gesperrt, du hast dich fünfmal vertippt, dein Konto
ist deaktiviert. Welche Spalte bei welcher Fensterbreite weicht, entscheidet sich an dieser Frage
und nicht daran, was passt (§4).

---

## 2. Wo sie hängt

`/administration/benutzer`, die zweite der beiden Unterseiten des Administrationsbereichs
([`frontend-grundlagen.md`](frontend-grundlagen.md) §2). **Hier ist ergänzt und nicht danebengebaut
worden:** Route, Navigationseintrag und Platzhalterseite standen seit Schritt 9b, die
Unternavigation führte den Eintrag bereits, und die Übersicht beschreibt ihn. Gewechselt hat allein
der Inhalt der `page.tsx`.

**Sie prüft die Rolle nicht.** Das könnte sie auch nicht: Eine Prüfung gegen `role` aus der
Selbstauskunft wäre eine Berechtigungsentscheidung im Browser — dieselbe Grenze, die `src/proxy.ts`
mit einem eigenen Absatz zieht. Verbindlich entscheidet `/api/admin/**`, und die Ansicht zeigt
dessen `403` als eigenen Zustand (§9).

---

## 3. Endpunkte

Alle sechs stehen seit dem 21.08.2026 und sind **unverändert** übernommen worden; die eine Änderung
am Vertrag ist `lockedUntil` (§4, [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md)
§4).

| Methode | Pfad | Rumpf | Wofür |
|---|---|---|---|
| `GET` | `/api/admin/users` | — | die Liste |
| `PUT` | `/api/admin/users/{id}/lock` | `{"locked": true}` | der Umschalter je Zeile |
| `PUT` | `/api/admin/users/{id}/active` | `{"active": false}` | deaktivieren, **nie löschen** (E8) |
| `PUT` | `/api/admin/users/{id}/role` | `{"role": "MANDANT"}` | das Auswahlfeld |
| `PUT` | `/api/admin/users/{id}/tenants` | `{"tenants": ["VOTG","SUTTONS"]}` | die Mehrfachauswahl — **dritte M1-Ausnahme** (E4) |
| `POST` | `/api/admin/users/{id}/password` | `{"initialPassword": "…"}` | das Passwortfeld |

Dazu **ein siebter, der nicht dieser Seite gehört**: `GET /api/mandanten` liefert die wählbaren
Mandanten für die Mehrfachauswahl. Er steht seit Schritt 3 und wird hier **wiederverwendet**, nicht
nachgebaut (§6).

**Jeder der fünf schreibenden antwortet mit derselben Zeile wie die Liste.** Damit wird der
Zwischenspeicher gesetzt, statt die Liste neu zu holen — und am eigenen Konto ginge das Nachholen
gar nicht: Der Vorgang verwirft dabei die eigene Sitzung (E5), und die nachgeschobene Abfrage liefe
in ein `401`.

---

## 4. Die Liste

**Mandantenfrei** (E2), **ein Fetch, keine Paginierung, keine serverseitige Suche** (E16) —
Größenordnung dreißig Konten. Sortiert nach Benutzername, **und die Reihenfolge kommt vom Backend**;
hier wird nicht umsortiert. Eine Spaltensortierung wäre eine neue Entscheidung und keine Ausbaustufe.

### Der Hinweis über der Liste (E21)

**Der Mandantenumschalter in der Kopfzeile bleibt stehen.** Damit steht über einer mandantenfreien
Liste ein Umschalter, der aussieht, als filtere er sie — und wer das ausprobiert, sieht *keine
Änderung*. Das ist die schlechteste Art, es zu erfahren. Deshalb trägt die Liste den Satz, dass sie
mandantenübergreifend gilt.

> **Ihn auszublenden ist verworfen.** Er steht in der Kopfzeile auf **jeder** Seite; ihn hier
> verschwinden zu lassen hieße, den Rahmen von einer Ansicht abhängig zu machen — und der aktive
> Mandant darf nach [`visuelles-konzept.md`](visuelles-konzept.md) §6 nie unsichtbar werden, weil er
> in der Sitzung steht und nicht in der URL. **Ein Satz kostet weniger als eine Ausnahme im Rahmen.**

### Neun Spalten, und welche weicht, entscheidet die Frage der Seite

| Spalte | Feld | sichtbar ab |
|---|---|---|
| Benutzer | `username` | immer |
| Rolle | `role` | `md` |
| Mandanten | `tenants` | `md` |
| **Sperre** | `locked` | **immer** |
| **Zeitsperre** | `lockedUntil` | **immer** |
| **Konto** | `active` | **immer** |
| Passwort | `mustChangePassword` | `lg` |
| Letzte Anmeldung | `lastLogin` | `lg` |
| Bearbeiten | — | immer |

**Die drei fett gesetzten sind die Antwort auf „warum kommt der nicht hinein".** Sie bleiben bei
jeder Breite stehen — zusammen mit dem **Benutzernamen**, ohne den keine Zeile zuzuordnen ist, und
dem **Bearbeiten-Knopf**, ohne den die Seite nichts mehr könnte. Alles Übrige weicht. Das ist die Regel aus
[`frontend-grundlagen.md`](frontend-grundlagen.md) §8 — *„Wer entscheiden muss, welche weicht, fragt
nicht ‚was passt‘, sondern ‚was sucht der Nutzer‘."*

**Nichts wird dadurch unerreichbar.** Alles Bedienbare steht im Formular unter der Zeile und nicht
in den Zellen (§5); ausgeblendet wird ausschließlich **Anzeige**. `md` und `lg` sind die
Umbruchpunkte, die das Projekt ohnehin führt — es kommt keiner dazu.

### Die zwei Sperren stehen in zwei Spalten und werden nie zu einer

Das ist die tragende Entscheidung der Zeile (E14, E20):

| Spalte | Was sie sagt | Wie sie endet |
|---|---|---|
| **Sperre** | ein **Verwaltungsakt** — jemand hat dieses Konto gesperrt | nur durch einen zweiten Verwaltungsakt |
| **Zeitsperre** | **fünf Fehlversuche** — das Konto hat sich selbst ausgesperrt | von selbst, nach fünfzehn Minuten |

Sie zusammenzufassen wäre nicht knapper, sondern falsch: Die beiden haben verschiedene Ursachen und
verschiedene Behebungen, und eine gemeinsame Spalte nähme genau die Auskunft weg, für die
`lockedUntil` überhaupt gebaut wurde.

**Und nie durch Farbe allein.** Die Zeile führt gar keine Farbrolle — die vier Statusfarben des
Projekts sind fachlich an den `MessageStatus` vergeben
([`visuelles-konzept.md`](visuelles-konzept.md) §3), und *„es gibt keine zweite Bedeutung von Grün
oder Rot"*. Die Unterscheidung liegt vollständig im Wort; gedämpft wird nur der ruhige Fall.

**`lockedUntil` wird nur dargestellt.** Ob die Sperre noch läuft, hat das Backend entschieden — mit
der **Systemuhr**, weil das sicherheitsnahe Zeit ist
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §7). Steht ein Wert in der Antwort, läuft sie.
Hier wird nichts gegen die Browseruhr verglichen; ein verstellter Rechner soll keine falsche
Auskunft erzeugen.

### `lastLogin = null` heißt „nie angemeldet" und wird ausgeschrieben

Keine leere Zelle. Bei über zwanzig externen Nutzern ist *„hat der sich überhaupt je angemeldet"*
die häufigste Supportfrage (E17), und eine leere Zelle beantwortet sie nicht — **sie sieht aus wie
eine fehlende Angabe.** Dieselbe Regel, nach der die Katalogpflege `false` und `null`
auseinanderhält: „nichts da" und „nicht erhoben" sind zwei Auskünfte.

### Die Mandanten sind Kennungen und keine Anzeigenamen

`VOTG`, nicht „VOTG Tanktainer GmbH" — bewusst so, denn die Kennung *ist* der sprechende Code, den
auch `POST /api/admin/users` entgegennimmt; die Namen lägen in `GlassfishDB.Mandant` hinter einem
schemaübergreifenden Join je Zeile. Sie stehen als **Marke**, der einen Gestalt des Projekts
([`visuelles-konzept.md`](visuelles-konzept.md) §5) — kein Knopf, keine Statusfarbe.

### Der Tabellenbau ist der der Pflegeliste

Unverändert aus [`prozess-katalog-frontend.md`](prozess-katalog-frontend.md) §3, und das ist der
Sinn: kein zweites `overflow-y-auto`, **kein `overflow-x-auto` darüber** (es stufte `overflow-y`
hoch und ließe die klebende Kopfzeile am Container statt am Fenster kleben), deshalb **kein
`<Table>` aus `components/ui`**, `border-separate` statt `border-collapse`, und `-top-4` statt
`top-0`, weil `main` `py-4` trägt. Die Zahl gehört zum Rahmen und nicht zu dieser Tabelle.

---

## 5. Alles Bedienbare steht im Formular unter der Zeile

Ein Knopf je Zeile klappt es auf; darin stehen **alle fünf Vorgänge**.

**Der Grund ist nicht Geschmack, sondern §4:** Unter `md` sind Rolle und Mandanten ausgeblendet,
unter `lg` zusätzlich Passwortwechsel und letzte Anmeldung. Stünden die Bedienelemente in den
Zellen, wären drei der fünf Vorgänge am schmalen Fenster gar nicht erreichbar — genau die
Begründung, mit der die Katalogpflege dieselbe Bauform gewählt hat, hier aber mit einer schwereren
Folge.

**Die Zeile bleibt dabei stehen**, statt durch Eingabefelder ersetzt zu werden. Bei einer
Verwaltungsmaske wiegt das mehr als bei einer Pflegeliste: Wer eine Sperre umlegt, will die Zeile,
die er umlegt, noch lesen können.

### Ein Vorgang zur Zeit, und die Meldung steht an ihm

Alle Bedienelemente hängen an **einer** Mutation, und solange sie läuft, sind sie gesperrt. Das ist
keine Sparsamkeit: Zwei gleichzeitige Aufrufe auf dieselbe Zeile bekämen zwei Antworten mit
demselben Anspruch, den Zwischenspeicher zu setzen — und welche zuletzt ankommt, entschiede dann das
Netz.

Der zweite Grund wiegt schwerer und ist derselbe wie im Backend: **E19 ist eine Regel und keine
Aufzählung.** Läuft jeder Vorgang durch dieselbe Stelle, kann die Vorwarnung nicht an einem von fünf
Aufrufwegen vergessen werden.

**Die Fehlermeldung steht am auslösenden Abschnitt** und nicht über dem Formular. Vier der neun
Problemtypen dieser Seite sind `409` und sagen etwas über den *Zustand* des Kontos; welcher Vorgang
gemeint ist, geht ohne die Nähe verloren.

**Und genau deshalb lässt sich die Zeile nicht zuklappen, solange etwas läuft.** Die Meldung steht
*im* Formular; verschwände es mitten im Aufruf, wäre ein `409` nirgends zu sehen und die Zeile sähe
schlicht unverändert aus. Aus demselben Grund ist **jede andere Zeile gesperrt, solange eine offen
ist** — dieselbe Regel wie bei der Katalogpflege (`darfOeffnen`), hier mit einem schwereren Einsatz:
Das Formular hält ein bereits **getipptes Einmalpasswort**, und das steht danach an keiner Stelle
mehr, auch nicht im Protokoll.

### Die Vorwarnung, wenn es das eigene Konto trifft (E19)

Nach E5 verwirft **jeder** der fünf Vorgänge *alle* Sitzungen des betroffenen Kontos, ohne
Fallunterscheidung. Trifft es das eigene Konto, meldet der Admin sich mit dem Klick selbst ab und
landet nach [`frontend-grundlagen.md`](frontend-grundlagen.md) §5 **wortlos** auf der Anmeldung —
dort wird bei `401` umgeleitet und nicht gemeldet. **Ohne Vorwarnung sähe das aus wie ein Absturz.**

**Beides steht da, und die Arbeitsteilung ist der Punkt.** Im Formular steht ein ruhiger Satz, sobald
die Zeile das eigene Konto ist — er erklärt die Lage, bevor jemand etwas anfasst. Der **Dialog**
kommt erst beim Auslösen, und er tut etwas anderes: **Er unterbricht und verlangt eine Antwort.**
Ein Satz allein trüge das nicht — man überliest ihn, und genau für den Moment, in dem er zählt, wäre
er gebaut und verfehlte ihn. Der Dialog steht *vor* dem Aufruf, denn danach gibt es keine Oberfläche
mehr, die ihn zeigen könnte.

**Und danach wird wirklich abgemeldet.** Der Vorgang verwirft die eigene Sitzung, das Backend weiß
es — die Oberfläche von selbst noch nicht: Der Zwischenspeicher hält seine Daten dreißig Sekunden,
die Selbstauskunft wird ohne Fensterfokus nicht nachgeladen, und bis zum nächsten Aufruf stünde eine
Seite da, die es nicht mehr gibt. Nach einem erfolgreichen Vorgang am eigenen Konto geht die Ansicht
deshalb denselben Weg wie das Abmelden: Zwischenspeicher leeren, dann **harte** Navigation auf die
Anmeldung (`lib/zwischenspeicher.ts`). **Der Dialog verspricht es, also muss es auch passieren.**

**Verglichen wird über den Benutzernamen, ohne Rücksicht auf Groß- und Kleinschreibung.** Das ist
keine Bequemlichkeit, sondern die einzige verfügbare Möglichkeit: `GET /api/auth/me` liefert **keine
`id`**, und die Verwaltungsliste führt eine — es gibt keinen Wert, über den sich die beiden sonst
verknüpfen ließen. Ohne Schreibungsrücksicht, **weil die Anmeldung sie ebenfalls nicht beachtet**:
Ein Konto `Admin`, das sich als `admin` anmeldet, sähe seine eigene Zeile sonst als fremde und
bekäme die Warnung genau im Grenzfall nicht, für den sie da ist.

> `toLowerCase` und **nicht** `toLocaleLowerCase`: Letzteres hängt an der Spracheinstellung — im
> Türkischen wird aus `I` ein `ı`. Der Vergleich soll überall dasselbe ergeben wie die Datenbank und
> nicht das, was der Browser gerade eingestellt hat.

### Welche Vorgänge am eigenen Konto überhaupt durchlaufen

**Am Backend abgelesen und nicht geraten.** `pruefeEntwertung` läuft nur in der *entwertenden*
Richtung:

| Vorgang | Richtung | am eigenen Konto |
|---|---|---|
| `lock` | sperren | **409** |
| `lock` | entsperren | läuft durch → **Vorwarnung** |
| `active` | deaktivieren | **409** |
| `active` | reaktivieren | läuft durch → **Vorwarnung** |
| `role` | auf `MANDANT` (herabstufen) | **409** |
| `role` | auf `ADMIN` | läuft durch → **Vorwarnung** |
| `tenants` | — | läuft durch → **Vorwarnung** |
| `password` | — | läuft durch → **Vorwarnung**; ausdrücklich ohne Selbstschutz |

**Für die verbotenen gibt es keinen Dialog**, sondern die Übersetzung des Problemtyps. Ein Dialog
verspräche, dass es nach dem Bestätigen passiert, und es passiert nicht.

> **Zwei Anmerkungen, die am Bestand hängen.**
>
> **Erstens:** Wer angemeldet ist, ist per Definition weder administrativ gesperrt noch deaktiviert
> — `AnmeldeService` weist beides vor der Sitzung ab. Die eigene Zeile trägt deshalb im Betrieb
> immer `locked: false` und `active: true`, und *entsperren* und *reaktivieren* sind dort
> **Leerläufe**: Sie schreiben den Zustand, der ohnehin gilt; spürbar ist an ihnen allein der
> Sitzungsentzug. Sie stehen trotzdem als „läuft durch" in der Tabelle — **die Regel folgt der
> Richtung und nicht der Erreichbarkeit** und bleibt damit auch dann richtig, wenn eine veraltete
> Liste einmal etwas anderes zeigt.
>
> **Zweitens:** Am eigenen Konto können *beide* `409` fallen. `pruefeEntwertung` prüft **zuerst** den
> letzten nutzbaren Administrator und **danach** das eigene Konto.

### Die verbotenen Richtungen werden aufgefangen und nicht vorweggenommen

Die Oberfläche sperrt die drei entwertenden Schalter am eigenen Konto **nicht**, und das ist eine
Entscheidung mit einem Grund, der genau an der eben genannten Reihenfolge hängt:

> Wer als einziger nutzbarer Admin angemeldet ist, bekommt `letzter-admin` und nicht `selbstschutz`
> — und diese Meldung sagt mehr: **Sie nennt die Bedingung, unter der es ginge.** Ein selbst gebauter
> Riegel „nicht am eigenen Konto" zeigte an dieser Stelle den *falschen* der beiden Sätze.

**Vorweggenommen wird nur, was die Oberfläche allein aus der Zeile ablesen kann** — und das sind
genau zwei Fälle: die Herabstufung ohne Mandantenzuordnung (E11, §5) und die letzte Zuordnung (E10,
§6).

### Das Passwort

Der Admin tippt es (E13), mindestens zwölf Zeichen. **Beide Sätze stehen da, bevor sie gebraucht
werden:** die Mindestlänge, und dass das Konto es beim nächsten Anmelden selbst ändern muss. Das ist
die Folge, nach der der Nutzer als Nächstes fragt.

Geprüft wird im Browser **nur die Länge**. Ob es sich vom bisherigen unterscheidet, kann er nicht
wissen — das prüft das Backend per BCrypt-Vergleich und antwortet mit `passwort-unveraendert`. Ein
Nachbau wäre nicht nur unmöglich, er wäre die zweite Stelle für dieselbe Regel.

---

## 6. Die Mandantenmenge — die dritte M1-Ausnahme

`PUT /api/admin/users/{id}/tenants` nimmt Mandanten-IDs entgegen (E4). Zulässig aus demselben Grund
wie die beiden anderen: **Hier wird eine Berechtigung definiert und kein Datenausschnitt abgefragt.**

**Die Menge wird vollständig ersetzt**, und die Bauform folgt der Bauart des Endpunkts: eine
**Mehrfachauswahl mit Häkchen**. Was auf dem Bildschirm steht, *ist* der Rumpf der Anfrage. Zwei
Knöpfe für Hinzufügen und Entfernen zeigten stattdessen zwei Vorgänge, die es nicht gibt.

### Woher die wählbaren Mandanten kommen

Aus **`GET /api/mandanten`** — derselben Quelle wie die Mandantenauswahl seit Schritt 3, unter
demselben Schlüssel und damit meist schon im Zwischenspeicher. Für einen ADMIN sind das alle
Mandanten aus `GlassfishDB.Mandant`.

**`SYSTEM` und `WOC` werden nicht ausgesiebt.** Beide sind technisch und kein Kunde
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3), und die Mandantenauswahl zeigt sie
trotzdem: *„Das Frontend filtert nichts nach und kennt keine Regel darüber, wer was sehen darf."*
Hier gilt dasselbe, und hier wiegt es schwerer — wer für einen technischen Mandanten zuständig ist,
muss ihm zugeordnet werden können. **Dieselbe Quelle, dieselbe Behandlung**, statt einer zweiten
Regel daneben.

> **Damit ist `lib/mandanten.ts` entstanden** und `features/sitzung` liest die Liste ab jetzt von
> dort. Ein Feature importiert nicht aus einem Nachbarfeature
> ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8), und dieselbe Datei nennt auch, was
> stattdessen gilt: *„der gemeinsame Teil nach `components/` oder `lib/` — nicht ins
> Nachbarfeature."* **Der Schlüssel wandert mit, und das ist der eigentliche Punkt:** Zwei Schlüssel
> nebeneinander hielten dieselbe Antwort zweimal und holten sie zweimal.

### Die letzte Zuordnung ist erkennbar und nicht nur aufgefangen (E10)

Ist genau ein Häkchen gesetzt, lässt es sich nicht abnehmen, und darunter steht, warum. Der Fall
steht vollständig im Entwurf, den der Nutzer vor sich hat — ihn in ein
`409 letzte-mandantenzuordnung` laufen zu lassen wäre eine Fehlermeldung für etwas, das die Auswahl
selbst sagen kann. **Der Serverfehler bleibt trotzdem übersetzt:** Er ist die verbindliche Prüfung.

**Ein Tausch bleibt möglich**, und das ist der Grund für die genaue Bedingung: Erst das neue Häkchen
setzen, dann ist das alte wieder abwählbar. Ohne diesen Weg wäre die Sperre ein Riegel und keine
Bedingung.

### Der Rollenwechsel: E11 ebenso

Eine Herabstufung ohne Zuordnung verbietet E11 (`409 rolle-ohne-mandant`). Trägt die Zeile keinen
Mandanten, ist der Eintrag „Mandant" im Auswahlfeld gesperrt und darunter steht, was zuerst zu tun
ist. Der Fall steht — wie E10 — vollständig in der Zeile.

### Zwei Dinge, die beim Bauen aufgefallen sind

1. **Ein Mandant, der am Konto hängt und nicht mehr in der wählbaren Menge steht, bekommt trotzdem
   einen Eintrag.** Im Betrieb kommt das nicht vor. Es steht trotzdem da, und der Grund ist nicht
   Vorsicht, sondern die Bauart des Endpunkts: Bei einer **Mengenersetzung** löschte eine Auswahl,
   die einen aktuellen Wert gar nicht darstellen kann, ihn beim nächsten Speichern — lautlos und
   ohne dass jemand ihn je gesehen hätte.
2. **Verglichen wird als Menge und nicht als Liste.** Ein Vergleich über die Reihenfolge meldete eine
   Änderung, wo keine ist, und schickte ein `PUT` für nichts — und **jedes `PUT` verwirft alle
   Sitzungen des Kontos** (E5). Eines für nichts ist deshalb kein leerer Aufruf, sondern eine
   Abmeldung ohne Anlass.

**Doppelte Kennungen prüft die Oberfläche nicht.** Häkchen sind eine Menge, und das Backend zieht sie
ohnehin still zusammen; eine eigene Prüfung wäre die zweite Stelle für dieselbe Regel.

**Der Entwurf setzt sich nach dem Speichern über den React-`key` zurück** und nicht über einen
Effekt: `setState` im Effekt ist im Projekt durch `react-hooks/set-state-in-effect` ausgeschlossen,
und er hätte einen Zwischenzustand, in dem zwei Wahrheiten nebeneinanderstehen.

---

## 7. Die Fehlerabbildung

Nach [`frontend-grundlagen.md`](frontend-grundlagen.md) §6: übersetzt wird anhand des
maschinenlesbaren `type`, nicht anhand von `detail`.

| Problemtyp | Status | Woher |
|---|---|---|
| `selbstschutz` | `409` | **neu** |
| `letzter-admin` | `409` | **neu** |
| `letzte-mandantenzuordnung` | `409` | **neu** |
| `rolle-ohne-mandant` | `409` | **neu** |
| `unbekannte-rolle` | `400` | **neu** — siehe unten |
| `passwort-zu-kurz` | `400` | bestand, **wiederverwendet** |
| `passwort-unveraendert` | `400` | bestand, **wiederverwendet** |
| `nicht-gefunden` | `404` | bestand, **wiederverwendet** |
| `konto-administrativ-gesperrt` | `401` | **neu — auf der Anmeldeseite** |

**`409` ist kein Rechteproblem.** Die Eingabe ist in Ordnung, der *Zustand* verbietet sie. Die vier
Texte nennen deshalb jeweils, **was zu tun ist, damit es doch geht** — genau darin liegt der
Unterschied zu `zugriff-verweigert`, wo es nichts zu tun gibt. Klängen sie gleich, wäre die
Unterscheidung im Backend umsonst gewesen.

**`unbekannte-rolle` führt der Auftrag unter „teils bestehend" — im Repository gab es ihn nicht.**
Gemeldet und angelegt. Er nennt die Rollen so, wie die Oberfläche sie schreibt („EDI-Betreuung",
„Mandant"), und nicht wie die Datenbank sie speichert.

**`konto-administrativ-gesperrt` gehört auf die Anmeldeseite und nicht hierher.** Er steht neben
`konto-gesperrt`, und der Unterschied ist der zweite Satz: **Diese Sperre läuft nicht von selbst
ab.** Der bestehende Text sagt „nach mehreren Fehlversuchen"; ohne den eigenen wartete der
Ausgesperrte eine Viertelstunde umsonst. Das Anmeldeformular brauchte dafür **keine Zeile Code** —
es zeigt seinen Fehler über `fehleranzeige`, und die liest den Katalog.

---

## 8. Datenquelle und Kosten

**Diese Oberfläche legt keine Abfrage an.** Sie ruft die sechs Endpunkte auf, die seit dem
21.08.2026 stehen, plus `GET /api/mandanten` aus Schritt 3.

| Aufruf | gemessen | wo |
|---|---|---|
| `GET /api/admin/users` | **17,87 ms** — `MAX(occurred_at)` je Konto über `audit_log` als abgeleitete Tabelle | **M82**, [`messungen-schritt9.md`](messungen-schritt9.md) |
| die fünf schreibenden | je ein `UPDATE` (bzw. `DELETE` + `INSERT`s), die Zeile danach einzeln gelesen — **ohne** die volle Aggregation | [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md) §4 |
| `GET /api/mandanten` | Stammdaten, seit Schritt 3 | [`mandantentrennung.md`](mandantentrennung.md) |

**Die eine Änderung am Backend in diesem Teil kostet nichts:** `APP_USER.LOCKED_UNTIL` wird in
`AppUserRepository.konten` **mitgelesen** — dieselbe Zeile, dieselbe Tabelle, kein zusätzlicher Join
und keine zweite Abfrage. Regel L7 ist damit nicht berührt: Es entsteht kein neues Statement,
sondern eine Spalte mehr in einem gemessenen.

**Die Liste zählt nichts nach und aggregiert nichts** (Regel L2 ist ohnehin auf `Message` bezogen);
sie zeigt, was der Endpunkt liefert.

---

## 9. Die vier Zustände

**„Kein Zugriff" steht vor allem anderen und außerhalb der Kette**, wie bei der Katalogpflege
([`prozess-katalog-frontend.md`](prozess-katalog-frontend.md) §9): Er ist kein Fehler — nichts ist
kaputt, der Nutzer steht vor einer Grenze, die für ihn gilt —, er ist kein Leerzustand, und er kennt
keinen zweiten Versuch. Er nimmt alles Bedienbare mit weg.

**Erkannt am Problemtyp und nicht am Statuscode.** `403` ist dreifach vergeben
(`zugriff-verweigert`, `csrf-token-ungueltig`, `kein-mandant-gewaehlt`); eine Statusprüfung zeigte
allen dreien dieselbe Meldung, und zwei davon wären falsch. **Dieselbe Bauform wie bei der
Katalogpflege und keine zweite** — `components/kein-zugriff.tsx` und `istKeinZugriff` sind
unverändert.

Danach die übliche Kette: **Laden, Fehler, Leer, Daten.**

### „Leer" hat hier genau eine Ursache — und trotzdem einen eigenen Satz

Anders als die Pflegeliste kennt diese Ansicht keinen Filter (E16): Ist die Antwort leer, gibt es
keine Konten. **Das kann im Betrieb nicht vorkommen** — wer die Liste sieht, ist selbst eines —, und
genau deshalb steht dort ein Satz, der das sagt, statt eines allgemeinen „nichts anzuzeigen": Ein
leerer Bildschirm ohne Erklärung ließe den Nutzer den Bestand verdächtigen, wo in Wahrheit etwas
nicht stimmen kann.

---

## 10. Zwei neue Nahtstellen, beide nach vorhandenem Muster

`features/benutzer` importiert weder aus `features/katalog` noch aus `features/sitzung`
([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Zwei Dinge braucht es trotzdem von dort:

| Was | Weg | Muster |
|---|---|---|
| die wählbaren Mandanten | **`lib/mandanten.ts`** — Typ, Aufruf und Schlüssel; `features/sitzung` liest sie ab jetzt von dort | wie `lib/filter.ts`: geteilte Infrastruktur, nie Fachlichkeit |
| der eigene Benutzername | **`components/angemeldet.tsx`** — ein Kontext, den der Anwendungsrahmen füllt | wie `components/zeitzone.tsx` und `components/suchsignal.tsx` |
| die Anzeigezone | `components/zeitzone.tsx`, unverändert | bestand |

**Der Name ist eine Auskunft und keine Berechtigung.** Was im Kontext steht, entscheidet nichts — es
steuert einen Hinweis. Wer ihn im Browser verstellt, bekommt einen falschen Hinweis und sonst gar
nichts: Verbindlich prüft das Backend, und der Selbstschutz aus E12 hängt dort an der Konto-ID.

**Nur der Name und keine `id`**, weil es keine gibt: `GET /api/auth/me` liefert sie nicht, und die
Selbstauskunft dafür zu erweitern ist verworfen (E19) — sie ist seit Schritt 3 ein Vertrag, der in
9a schon einmal gebrochen worden ist.

---

## 11. Die Dateien

```
frontend/src/
├─ app/(app)/administration/benutzer/page.tsx   rendert `BenutzerAnsicht` (war Platzhalter)
├─ components/
│  ├─ angemeldet.tsx                 NEU — der eigene Benutzername als Kontext
│  └─ anwendungsrahmen.tsx           füllt ihn
├─ features/benutzer/                NEU
│  ├─ api.ts                         Typ `Nutzerzeile`, `BENUTZER_SCHLUESSEL`, sechs Aufrufe
│  ├─ selbstschutz.ts                `Vorgang`, das eigene Konto, die Vorwarnung, das Passwort
│  ├─ zuordnung.ts                   die Mandantenmenge: Auswahl, Umschalten, E10, Mengenvergleich
│  ├─ zeilen.ts                      die geänderte Zeile an ihrer Stelle, `darfOeffnen`
│  ├─ hooks.ts                       eine Abfrage, **eine** Mutation für alle fünf Vorgänge
│  └─ components/
│     ├─ benutzer-ansicht.tsx        die Zustände, der Hinweis aus E21, die offene Zeile
│     ├─ benutzer-tabelle.tsx        klebende Kopfzeile, neun Spalten, die zweite `<tr>`
│     ├─ benutzer-zeile.tsx          die Zellen — die zwei Sperren, „nie angemeldet", die Marken
│     ├─ zeilen-formular.tsx         die fünf Vorgänge, ein Aufrufweg
│     ├─ mandanten-auswahl.tsx       Mehrfachauswahl, vollständige Zielmenge, E10
│     └─ vorwarnung.tsx              der Dialog aus E19
├─ features/sitzung/{api,hooks}.ts   liest die Mandanten ab jetzt aus `lib/mandanten.ts`;
│                                    `downloadAllowed` gestrichen
├─ lib/mandanten.ts                  NEU — Typ, Aufruf und Schlüssel der wählbaren Mandanten
└─ i18n/{de,en}.ts                   ein neuer Zweig `benutzer`, sechs neue Fehlerschlüssel
```

Backend (Teil 1 dieses Auftrags): `KontoZeile.gesperrtBisUtc`, `AppUserRepository.konten` liest
`LOCKED_UNTIL` mit, `NutzerzeileResponse.lockedUntil` samt Vergleichszeitpunkt aus der `systemClock`.

---

## 12. Tests

`pnpm test`, nach [`frontend-grundlagen.md`](frontend-grundlagen.md) §9. Ein `console.error` lässt
den Lauf fehlschlagen. **531 Fälle in 25 Dateien**, davon neu:

| Datei | Art | Was |
|---|---|---|
| `tests/benutzer.test.ts` | rein | das eigene Konto samt abweichender Schreibweise und fehlender Selbstauskunft; **alle acht Richtungen** der Frage „läuft das am eigenen Konto"; die Vorwarnung in allen fünf Lagen; das Einmalpasswort und die Grenze zur Backend-Prüfung; die Mandantenmenge (technische Mandanten, der Mandant außerhalb der Wahl, E10 **mit dem Tausch als Gegenprobe**, Mengen- statt Reihenfolgevergleich); die Zeile im Zwischenspeicher; **alle neun Übersetzungen in beiden Sprachen** samt der Probe, dass die vier `409` nicht wie `403` klingen; „kein Zugriff" bei `zugriff-verweigert` und bei keinem der beiden anderen `403` |
| `tests/benutzer-tabelle.test.tsx` | **gerenderter Baum**, zehn Fälle | `lastLogin = null` als Satz und nicht als leere Zelle, mit Gegenprobe; die zwei Sperren in zwei Zellen, in beide Richtungen; die **Verdrahtung** der Vorwarnung (Dialog **vor** dem Aufruf, kein Aufruf beim eigenen Konto, danach die **wirkliche Abmeldung**; sofortiger Aufruf beim fremden und **keine** Abmeldung), die vollständige Zielmenge auf der Leitung, und die **Sperre der übrigen Zeilen**, solange eine offen ist — samt Gegenprobe |
| `backend/.../NutzerzeileResponseTest` | Einheit, sieben Fälle | `lockedUntil` nur, wenn die Sperre noch läuft — laufend, abgelaufen, **der Grenzfall „genau jetzt"**, ohne Sperre, beide Sperren getrennt, die Umrechnung über UTC, `lastLogin = null` |

### Warum `benutzer-tabelle.test.tsx` einen Baum rendern darf

Die Bedingung ist nicht „ein Baum wäre bequemer", sondern **„es gibt keinen anderen Ort, an dem der
Satz belegbar wäre"**. Drei Klassen erfüllen sie:

1. **Anwesenheit und Abwesenheit im Baum.** Dass `lastLogin = null` einen *Satz* ergibt und keine
   leere Zelle, und dass **beide** Sperren mit je eigenem Wortlaut dastehen — beides entsteht erst in
   der Zelle. Es gibt nichts zu rechnen, nur etwas hinzuschreiben; die naheliegende Zusammenfassung
   zu einer Spalte „gesperrt" bestünde jede Prüfung an einer reinen Funktion.
2. **Die Verdrahtung der Vorwarnung.** Die Regeln sind reine Funktionen und stehen daneben; belegt
   wird, dass jemand sie **abfragt**, bevor der Aufruf losläuft — und dass danach wirklich
   abgemeldet wird.
3. **Die Verdrahtung der Sperre.** Dass die Schaltfläche jeder *anderen* Zeile gesperrt ist,
   solange eine offen ist, samt Gegenprobe. *Eine richtige Regel, die niemand abfragt, sieht von
   außen aus wie keine.*

**Ein Fund beim Schreiben, der hier stehen bleibt:** Die erste Fassung prüfte die Sperrzelle mit
`toContain`. Das geht durch, ohne etwas zu belegen — **„nicht gesperrt" enthält „gesperrt"**.
Geprüft wird deshalb der ganze Wortlaut der Zelle.

Die Gesamtzahl der gerenderten Fälle steht **ausschließlich** im Kopf von
`frontend/vitest.config.mts` und ist dort von 39 auf **49** in neun Dateien fortgeschrieben.

### Alle sichtbaren Texte stehen in den Sprachdateien

Ein neuer Zweig `benutzer` auf oberster Ebene und sechs neue Fehlerschlüssel. **Keiner steht in einer
Komponente.** Die Rollennamen stehen weiterhin unter `rolle` — sie gelten im ganzen Werkzeug gleich
und werden seit Schritt 3 auch vom Nutzermenü gelesen.

---

## 13. Abweichungen von Vorgabe und Auftrag

| | Abweichung | Grund |
|---|---|---|
| 1 | **Alles Bedienbare steht im Formular unter der Zeile**, nicht in den Zellen. E21 sagt „ein Umschalter je Zeile" | Am schmalen Fenster wären drei der fünf Vorgänge sonst gar nicht erreichbar (§5). Der Umschalter gehört weiterhin **genau einer Zeile** — er steht nur einen Klick tiefer, und dafür bei jeder Breite |
| 2 | **`unbekannte-rolle` ist neu angelegt**, nicht wiederverwendet | Der Auftrag führt ihn unter „teils bestehend". Im Repository gab es ihn nicht — gemeldet und angelegt |
| 3 | **Ein Mandant außerhalb der wählbaren Menge bekommt trotzdem einen Eintrag** — im Auftrag nicht genannt | Folge der Mengenersetzung: Eine Auswahl, die einen aktuellen Wert nicht darstellen kann, löschte ihn lautlos (§6) |
| 4 | **`lib/mandanten.ts` ist entstanden**, `features/sitzung` ist dafür angefasst worden | Der Auftrag verlangt „dieselbe Quelle"; ohne die Verschiebung hieße das entweder ein Import aus dem Nachbarfeature oder ein zweiter Schlüssel für dieselbe Antwort (§6, §10) |
| 5 | **Die drei verbotenen Selbstvorgänge sind nicht gesperrt**, nur aufgefangen | Ausdrücklich so im Auftrag — und es gibt einen zweiten Grund, der beim Lesen des Backends dazukam: Ein Riegel zeigte den falschen der beiden `409`-Sätze (§5) |

---

## 14. Offene Punkte

| | |
|---|---|
| 1 | **Die Sichtprüfung im Browser steht aus.** Kein Pfad dieser Seite ist am laufenden System durchgeklickt — insbesondere nicht die fünf Vorgänge, denn jeder von ihnen **schreibt auf die geteilte Testkopie** und wirft ein echtes Konto aus seinen Sitzungen. Eintrag in [`README.md`](README.md) |
| 2 | **Das schmale Fenster ist ungeprüft.** Die Browsersteuerung kann das Fenster nicht verkleinern ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8); geprüft ist das **Regelwerk** — welche Spalte an welchem Umbruchpunkt weicht —, nicht die Darstellung. Bei 360 px stehen fünf Spalten nebeneinander, und ob das trägt, ist von Hand nachzusehen |
| 3 | **Die Vorwarnung ist nie ausgelöst worden.** Sie ist unit- und baumgeprüft; der Weg *durch* sie hindurch endet mit einer echten Abmeldung des prüfenden Kontos und ist deshalb nicht nebenbei zu machen |
| 4 | **`lockedUntil` ist am laufenden System nie gefüllt gesehen worden.** Der Zustand entsteht nach fünf Fehlversuchen an einem echten Konto; hergestellt wurde er nicht |
| 5 | **Keine Suche und kein Blättern** (E16). Bei dreißig Konten ist das richtig. Wächst der Bestand deutlich, ist es eine neue Entscheidung und kein Nachziehen |
| 6 | **Kein Löschen von Konten** (E8) und **keine Änderung am Vertrag von `POST /api/admin/users`** (E4) — beides ausdrücklich außerhalb dieses Auftrags. Das Anlegen eines Kontos ist über die Oberfläche damit **nicht** möglich; es geht weiterhin nur über den Endpunkt aus Schritt 3 |
| 7 | Die offenen Punkte **3, 4 und 6** aus [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md) §9 — der Index aus M82, die tote Spalte `last_login_at`, der falsche Migrationskommentar — bleiben offen. Alle drei sind Entscheidungen und keine Ableitungen |

---

## 15. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | Fünf der sechs Aufrufe kennen keinen Mandantenparameter. Der sechste — `tenants` — ist die **dritte und namentlich geführte Ausnahme** (E4): Er definiert eine Berechtigung, statt einen Datenausschnitt abzufragen |
| **M2/M3** Der Mandantenfilter gehört ins Statement | Hier ausdrücklich **nicht anwendbar**: `app_user` liegt in `overlord_monitor`, die Liste ist mandantenfrei (E2). Die Oberfläche filtert nichts nach |
| **M4** Isolationstest je Endpunkt | Im Backend als **Rollengrenze** gefahren (E3, `BenutzerverwaltungIsolationDbIT`). Die Oberfläche zeigt deren `403` als eigenen Zustand und trifft selbst keine Berechtigungsentscheidung |
| **L1** Pflicht-Zeitfenster je Listen-Endpunkt | Gilt hier nicht: Die Kontenliste ist keine Nachrichtenliste und hat kein Zeitfenster (E16) |
| **L2** Keine Live-Aggregation über `Message` | Nicht berührt. Die eine Aggregation der Seite läuft über `audit_log` — unser eigenes Schema, gemessen in M82 |
| **L7** Jede Abfrage gemessen | Diese Oberfläche legt **keine** Abfrage an. Die eine Backend-Änderung ist eine zusätzliche Spalte in einem gemessenen Statement (§8) |
| **Z1** Kein direkter `now()`-Aufruf | Betrifft das Backend, und dort ist es der Kern von `lockedUntil`: Der Vergleichszeitpunkt kommt als Parameter aus der `systemClock`. **Im Browser wird für die Sperrfrist keine Uhr gelesen** — die Oberfläche stellt nur dar |
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | `lastLogin = null` heißt „nie angemeldet" und wird so geschrieben; die zwei Sperren werden nie zusammengefasst; ein unbekannter Rollenwert wird **roh** gezeigt und nicht auf einen bekannten gebogen |
| Farben nur über Tokens | Diese Ansicht führt **keine** Farbrolle ein und nutzt keine; `tests/farbwerte.test.ts` deckt die neuen Dateien mit ab |
