# Assistent im Frontend: Chatbox und Bestätigungskarte (A3)

*Angelegt am 19.10.2026 mit Teilschritt A3 der Linie `assistent`* (Zweig `assistent-a3-chatbox`,
[`ROADMAP.md`](ROADMAP.md) §2). Er baut auf den Endpunkten aus A1 ([`tasks.md`](tasks.md) §6) und A2
([`assistent.md`](assistent.md) §6) auf. **Kein Backend-Umbau.**

A3 baut die Oberfläche des Assistenten: ein Symbol in der Kopfleiste, eine Chatbox als Overlay und
die Karte, auf der der Kunde einen Vorschlag prüft, ändert und anlegt. Der Bereich „Ihre Aufträge“
folgt mit A4.

> **Der Assistent bleibt abgeschaltet**, bis der Datenschutzbeauftragte die Auftragsverarbeitung
> mit Anthropic geprüft hat (E‑1029, E‑1030, Punkt 1009). Abgeschaltet meldet
> `GET /api/assistent` `verfuegbar: false`, und das Symbol fehlt (E‑1068). Ohne Einschalten sieht
> also niemand etwas von A3.

---

## 1. Ablauf aus Sicht des Kunden

1. **Öffnen.** In der Kopfleiste steht links neben dem Knopf für die Benutzereinstellungen ein
   Sprechblasen-Symbol. Ein Klick öffnet die Chatbox oben rechts. Der Fokus steht im Eingabefeld.
2. **Begrüßung.** Die Chatbox nennt die Aufträge, die dieser Mandant über den Assistenten anlegen
   kann. Die Begrüßung ist ein fester Text; das Modell wird dafür nicht gefragt.
3. **Schreiben.** Der Kunde schreibt in eigenen Worten, was er ändern will, auch mehrzeilig oder
   aus Excel eingefügt. Enter sendet, Umschalt+Enter beginnt eine neue Zeile.
4. **Warten.** Während der Assistent antwortet, steht „Der Assistent antwortet …“ im Verlauf;
   Eingabe und Senden sind gesperrt. Es gibt kein Streaming.
5. **Rückfrage oder Karte.** Der Assistent fragt nach oder legt einen Vorschlag als Karte in den
   Chat: Auftragstyp, Aktion, „Gilt für: <Mandant>“, je Eintrag die Felder, leere ausdrücklich als
   „leer“ mit ihrer Bedeutung darunter.
6. **Ändern.** „Ändern“ macht die Felder der Karte zu Eingabefeldern. Jede Änderung prüft das
   Backend kurz nach der letzten Taste; Fehler stehen am Feld. „Übernehmen“ ist frei, sobald die
   Prüfung nichts mehr findet; „Verwerfen“ stellt den vorigen Stand wieder her.
7. **Anlegen.** „Auftrag anlegen“ legt den Auftrag an. Danach ist die Karte gesperrt und zeigt
   „Angelegt, Status: Offen“. Den weiteren Stand zeigt ab A4 „Ihre Aufträge“.

**Anlegen tut immer der Klick, nie das Modell** (E‑1033). Was der Kunde auf der Karte ändert oder
anlegt, erfährt das Gespräch nicht (E‑1078, Punkt 1012).

---

## 2. Entscheidungen

Herkunft: **A** = vom Auftraggeber entschieden, mit Datum. **V** = Vorschlag vom 19.10.2026, ohne
Einspruch übernommen. Die E‑Nummer ist `E‑1064` plus die Nummer der Entscheidung im Auftrag zu A3;
nächste freie Nummer war E‑1065, per `grep` über das Repository geprüft.

### Aus dem Auftrag

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1065** | Der einzige Eingang ist ein Symbol in der Kopfleiste, **links neben dem Knopf für die Benutzereinstellungen**. Kein Menüpunkt, keine eigene Seite. | A (15.10.2026) |
| **E‑1066** | Ein Klick öffnet eine **Chatbox als Overlay** oben rechts, rund 420 Pixel breit. Kein getrenntes Entwurfsfenster. | A (15.10.2026) |
| **E‑1067** | Ein Vorschlag erscheint als **kleine Karte im Chat** mit „Auftrag anlegen“ und „Ändern“. Felder sind auf der Karte änderbar. | A (15.10.2026) |
| **E‑1068** | Das Symbol erscheint nur, wenn `GET /api/assistent` `verfuegbar: true` meldet. Sonst fehlt es ganz, ohne Hinweis. | V |
| **E‑1069** | Die Karte nennt den **aktiven Mandanten** („Gilt für: …“), aus derselben Quelle wie die Kopfleiste. | V (Hinweis aus A2, [`assistent.md`](assistent.md) §12) |
| **E‑1070** | Die Karte zeigt **jedes Feld** des Vorschlags. Ein leeres Feld steht ausdrücklich als „leer“ da, mit seinem `hinweis` aus der Definition darunter. Die Karte kennt keinen Auftragstyp. | V |
| **E‑1071** | **Prüfung auf der Karte nur über `POST /api/tasks/pruefung`**, verzögert während der Eingabe. Keine Regeln, Muster oder Längen im Frontend nachgebaut. Fehler stehen am Feld, mit dem Klartext des Backends. „Übernehmen“ ist gesperrt, solange die Prüfung Fehler meldet oder noch läuft. | V |
| **E‑1072** | Je Karte genau eine `anfrageId` (UUID), erzeugt beim Erscheinen der Karte. Ein erneuter Klick nach Fehler oder Zeitüberschreitung schickt dieselbe. | V |
| **E‑1073** | Nach dem Anlegen ist die Karte gesperrt und zeigt „Angelegt, Status: Offen“. Ein zweites Anlegen derselben Karte ist nicht möglich. | V |
| **E‑1074** | Texte des Assistenten werden als **reiner Text** gerendert, als Textknoten mit erhaltenen Zeilenumbrüchen. Kein HTML, kein Markdown, keine Links. | V |
| **E‑1075** | Kein Streaming. Während der Antwort steht eine Warteanzeige; Eingabe und Senden sind gesperrt. | V (A2, E‑1043) |
| **E‑1076** | Das Gespräch lebt im Zustand der Anwendungshülle: Es übersteht Schließen der Chatbox und Navigation, nicht aber ein Neuladen. „Neues Gespräch“ beendet das alte über `DELETE` und eröffnet ein neues. Ein Mandantenwechsel setzt die Chatbox zurück. Ein `404` auf ein Gespräch heißt: abgelaufen; die Chatbox sagt das und bietet ein neues an. | V |
| **E‑1077** | Auf schmalem Bildschirm wird die Chatbox zur Fläche über die ganze Breite, nach den Regeln aus [`visuelles-konzept.md`](visuelles-konzept.md) §6. | V |
| **E‑1078** | Ändert der Kunde die Karte oder legt er an, erfährt das Gespräch davon nichts. Das bleibt ein offener Punkt aus A2 (Punkt 1012). | V |
| **E‑1079** | Alle Texte in den Sprachdateien. Knöpfe: „Auftrag anlegen“, „Ändern“, „Übernehmen“, „Verwerfen“, „Neues Gespräch“. | V |

### Rückfragen zu A3 (19.10.2026)

Drei Punkte ließen sich aus Auftrag und Backend nicht entscheiden; **vom Auftraggeber am
19.10.2026 entschieden**, jeweils der vorgeschlagene Weg.

| E | Entscheidung | Anlass | Herkunft |
|---|---|---|---|
| **E‑1080** | **Schlüssel als Überschrift mit Rückfall.** Die Überschrift eines Eintrags ist der `schluessel` aus dem Vorschlag, solange die Werte des Eintrags unverändert sind; nach einer übernommenen Änderung heißt er „Eintrag n“. Die Schlüsselfelder stehen ohnehin darunter. | `POST /api/tasks/pruefung` liefert nur `errors`, keinen Schlüssel. Ihn selbst zu bilden hieße, die Schlüsselregel der Typdefinition im Frontend nachzubauen (E‑1071). Offen als Punkt 1017 | A (19.10.2026) |
| **E‑1081** | **Fehler nach Problemtyp, nicht nach Statuscode.** `429 zu-viele-nachrichten` und jede andere `429`: Bitte, kurz zu warten. `429 gespraech-ausgeschoepft`: der Hinweis aus dem Fehlerformat mit der Höchstzahl aus `hoechstensNachrichten` und „Neues Gespräch“. In beiden Fällen bleibt die Eingabe erhalten. | Die Grenze von 20 Nachrichten je Gespräch kommt als `429` und nicht als `400` ([`assistent.md`](assistent.md) §6.6); Warten hilft dort nicht. Weicht von der Fehlertabelle des Auftrags ab | A (19.10.2026) |
| **E‑1082** | **Eröffnet wird mit der ersten Nachricht.** Öffnen der Chatbox und „Neues Gespräch“ zeigen die Begrüßung; `POST /api/assistent/gespraeche` geht erst beim ersten Senden hinaus, unmittelbar vor der Nachricht. „Neues Gespräch“ beendet das alte sofort über `DELETE`. | Kein Gespräch und kein Statement der Selbstauskunft für Kunden, die nur hineinschauen; die Grenze von 3 Gesprächen je Sitzung (E‑1053) wird nicht verbraucht. Für den Kunden kein Unterschied zu E‑1076 | A (19.10.2026) |

### Festlegungen der Umsetzung

Was der Auftrag offenließ und beim Bau festgelegt wurde, vorgeschlagen als **F1 bis F15**, zur
Bestätigung durch den Auftraggeber. Eine E‑Nummer bekommen sie erst mit der Bestätigung, wie in A2.

| F | Festlegung | Grund |
|---|---|---|
| **F1** | **Keine Chat-Bausteine aus dem Generator, kein Generator-`textarea`.** Chatbox und Karte bestehen aus den vorhandenen Bausteinen (`Popover`, `Button`, `Badge`, `Alert`, `Input`, `Label`); das Eingabefeld ist ein `textarea` im Feature mit den Klassen von `components/ui/input.tsx`. | `shadcn add message-scroller message bubble marker` brächte am 19.10.2026 zwei neue Laufzeitabhängigkeiten (`@shadcn/react`, `cn`) und überschriebe `button.tsx`; selbst `textarea` zieht inzwischen `cn` nach. Der Scroller folgt dem Ende eines Streams, das Projekt holt aber die **Oberkante** des Neuen ins Bild (`lib/in-sicht-bringen.ts`, [`frontend-grundlagen.md`](frontend-grundlagen.md) §7, Bedingung 4) |
| **F2** | **In der Chatbox wird gesiezt**, wie der Assistent selbst (E‑1044); die übrige Oberfläche duzt weiter. | Begrüßung, Hinweise und Antworten stehen im selben Verlauf; ein Wechsel der Anrede mitten im Gespräch läse sich wie zwei Absender |
| **F3** | **Verlauf und Karten liegen im Arbeitsspeicher der Kopfzeile**, als ein Zustand über `useReducer` mit einer reinen Übergangsfunktion (`features/assistent/gespraech.ts`). Nicht in TanStack Query, nicht im lokalen Speicher, nicht in der URL. | Kein Endpunkt liefert den Verlauf zurück; das Backend hält ihn nur für die Gesprächsführung (E‑1038). Die Kopfzeile lebt im Layout der angemeldeten Routen und übersteht jede Navigation (E‑1076). Schlüssel ist der Mandant: Ein Wechsel hängt den Zustand neu ein |
| **F4** | **Gesendet heißt: im Verlauf.** Die Nachricht steht sofort im Verlauf, das Eingabefeld wird leer. Scheitert der Aufruf, verschwindet sie aus dem Verlauf und steht wieder im Eingabefeld. | Das Backend nimmt eine gescheiterte Nachricht nicht in den Verlauf (E‑1052); stünde sie trotzdem im Chat, zeigte er ein anderes Gespräch als das, das der Assistent kennt. „Die Eingabe bleibt erhalten“ (Fehlertabelle) heißt damit: wörtlich im Feld |
| **F5** | **Geprüft wird 400 ms nach der letzten Änderung.** Jede Änderung sperrt „Übernehmen“ sofort; eine Antwort auf einen älteren Stand wird verworfen. | Gewählt, nicht gemessen. Kurz genug, dass die Prüfung beim Weiterlesen schon da ist, lang genug, dass beim Tippen nicht je Taste ein Aufruf hinausgeht |
| **F6** | **Mehr als fünf Einträge:** Die ersten fünf stehen offen, darunter „Alle <n> anzeigen“. Im Änderungsmodus und wenn ein verborgener Eintrag einen Fehler trägt, stehen alle offen. | „Alle n anzeigen“ setzt voraus, dass schon welche zu sehen sind; ein Fehler und ein Eingabefeld dürfen nicht hinter einem Schalter liegen |
| **F7** | **Nach dem Anlegen zeigt die Karte die Werte aus der Antwort** von `POST /api/tasks`, nicht die eigenen; der Status steht als „Offen“, jede andere Einordnung als ihr Rohwert. | Mit derselben `anfrageId` (E‑1072) antwortet das Backend nach einer Zeitüberschreitung mit dem **vorhandenen** Auftrag, auch wenn die Karte inzwischen andere Werte trägt ([`tasks.md`](tasks.md) §6.2). Die Karte zeigt, was angelegt ist. Die Wörter für die übrigen Einordnungen gehören zu A4 |
| **F8** | **„Auftrag anlegen“ ist gesperrt**, solange Prüffehler aus dem Anlegen am Feld stehen, und im Änderungsmodus nicht angeboten: Erst „Übernehmen“ oder „Verwerfen“. | Dieselben Werte noch einmal zu schicken, brächte dieselben Fehler. Angelegt werden soll, was auf der Karte steht, nicht ein halber Entwurf |
| **F9** | **Der Kopf der Chatbox nennt den Mandanten** (Code), zusätzlich zur Karte. | Unter 768 px deckt die Chatbox die zweite Zeile der Kopfzeile ab, in der der Mandant steht ([`visuelles-konzept.md`](visuelles-konzept.md) §6: „Der aktive Mandant bleibt in der Kopfzeile“). Der Assistent verweist bei einem fremden Mandanten auf den Wechsel dort (E‑1059) |
| **F10** | **Feste Höhe** `min(40rem, verfügbare Höhe)`; nur der Verlauf scrollt. | Eine Box, die mit jeder Nachricht wächst, bewegt die Fläche unter der Hand (Leitsatz „ruhig“, [`visuelles-konzept.md`](visuelles-konzept.md) §1) |
| **F11** | **Gesperrt heißt `readOnly` und `aria-disabled`, nicht `disabled`:** das Eingabefeld während der Antwort, „Senden“, „Übernehmen“, „Auftrag anlegen“ und „Neues Gespräch“. Der Klick prüft die Sperre selbst. | Ein gesperrtes Element verlöre unter der Tastatur den Fokus, gerade wenn es eben gedrückt wurde — wie beim Knopf „Neu laden“ ([`neu-laden.md`](neu-laden.md)); nach der Antwort schreibt der Kunde dort weiter, wo er war |
| **F12** | **`409 nachricht-laeuft` wird nicht angezeigt**, nur mit `console.warn` gemeldet; die Eingabe kommt ins Feld zurück. | Fehlertabelle: „darf durch die Sperre nicht vorkommen; wenn doch, still ignorieren und loggen“. `console.error` ließe die Tests fehlschlagen und ist für Befunde da |
| **F13** | **„Neues Gespräch“ geht auch während einer Antwort.** Die späte Antwort kommt nicht in das neue Gespräch; ein Gespräch, das erst danach eröffnet wurde, wird sofort beendet. | Der Kunde soll nicht auf eine Antwort warten müssen, die er nicht mehr will. Gezählt wird über eine Generation je Gespräch im Haken, nicht über die Antwort |
| **F14** | **„Neues Gespräch“ ist gesperrt, solange das Gespräch frisch ist** — nur die Begrüßung, kein Hinweis, nichts eröffnet. | Ein Knopf, der nichts tut, wäre ein Klickversprechen ins Leere (dieselbe Regel wie beim Mandantenumschalter, [`visuelles-konzept.md`](visuelles-konzept.md) §5) |
| **F15** | **Die `anfrageId` entsteht auch ohne sicheren Kontext**: `crypto.randomUUID`, sonst eine UUID der Version 4 aus `crypto.getRandomValues`. | `randomUUID` gibt es nur unter HTTPS oder auf `localhost`; hinter einem Proxy ohne TLS fehlte sie still, und das Anlegen schiene zu gehen |

---

## 3. Aufbau

```
frontend/src/
├─ components/kopfzeile.tsx            setzt das Symbol links neben das Nutzermenü
└─ features/assistent/
   ├─ api.ts                           Typen, Schlüssel und Aufrufe der sechs Endpunkte
   ├─ gespraech.ts                     Zustand und Übergänge: Verlauf, Karten, Hinweise (rein)
   ├─ hooks.ts                         Abfrage des Stands, Mutationen, verzögerte Prüfung
   └─ components/
      ├─ assistent-eingang.tsx         Symbol, Overlay, Kopf der Chatbox
      ├─ verlauf.tsx                   Begrüßung, Nachrichten, Warteanzeige, Hinweis
      ├─ eingabe.tsx                   Eingabefeld und Senden
      └─ vorschlag-karte.tsx           die Karte
```

**Ein eigenes Modul**, nach der Regel „pro Ansicht ein eigenes Modul“; es spiegelt das Fachpaket
`assistent` im Backend. Es importiert aus keinem Nachbarfeature. Den aktiven Mandanten bekommt es
von der Kopfzeile als Eigenschaft — es ist `auskunft.mandant` aus `GET /api/auth/me`, dieselbe
Angabe, die die Kopfzeile anzeigt (E‑1069); der Typ liegt in `lib/mandanten.ts`.

**Daten:**

| Endpunkt | Weg | Wann |
|---|---|---|
| `GET /api/assistent` | `useQuery`, Schlüssel `["assistent", "stand"]` | beim Einhängen der Kopfzeile; entscheidet über das Symbol und liefert die Typen der Begrüßung |
| `POST /api/assistent/gespraeche` | Mutation | vor der ersten Nachricht eines Gesprächs (E‑1082) |
| `POST /api/assistent/gespraeche/{id}/nachrichten` | Mutation | je gesendeter Nachricht |
| `DELETE /api/assistent/gespraeche/{id}` | Mutation | „Neues Gespräch“, wenn es ein offenes gibt; ein Fehler dabei wird nicht angezeigt |
| `POST /api/tasks/pruefung` | Mutation | 400 ms nach der letzten Änderung auf der Karte (F5) |
| `POST /api/tasks` | Mutation | „Auftrag anlegen“, mit der `anfrageId` der Karte |

Alle sechs gehen über `lib/http.ts`; dort kommen dafür `entferne` (`DELETE`) und die Fehlerliste
mit `index` und `code` dazu. **Keiner bekommt eine Mandanten-ID** — keine Anfrage dieses Moduls
trägt eine. Die Karte schickt `typ`, `aktion` und die Werte, für `POST /api/tasks` dazu die
`anfrageId` ([`assistent.md`](assistent.md) §4.7).

**Der Zustand** (F3) ist eine Liste von Beiträgen — Begrüßung, Nachricht des Kunden, Antwort des
Assistenten mit oder ohne Karte —, dazu Gesprächskennung, Entwurf im Eingabefeld, Warten, Hinweis
und „abgelaufen“. Je Karte stehen dort `anfrageId`, die übernommenen Werte, Modus, Entwurf, Stand
der Prüfung und Stand des Anlegens. Weil die Karte selbst keinen Zustand hält, übersteht auch eine
halb geänderte Karte das Schließen der Chatbox.

---

## 4. Die Chatbox

**Das Symbol** ist ein Geisterknopf wie das Nutzermenü, mit `aria-label` und Tooltip
(`title`) „Assistent“. Es erscheint erst, wenn `GET /api/assistent` `verfuegbar: true` geliefert
hat — solange die Abfrage läuft oder scheitert, fehlt es (E‑1068), ohne Platzhalter: lieber ein
Bedienelement, das einen Moment später erscheint, als eines, das wieder verschwindet (dieselbe
Regel wie beim Mandantenumschalter). Es steht nur, wenn ein Mandant aktiv ist und kein
Passwortwechsel ansteht; dort lehnte das Backend ohnehin ab.

**Das Overlay** ist ein `Popover`, rechtsbündig unter dem Symbol, 26,25 rem breit (420 px in der
Stufe `m`) und `min(40rem, verfügbare Höhe)` hoch (F10). Es schließt mit Esc, mit dem Knopf „×“ im
Kopf und mit einem Klick daneben; das Gespräch bleibt dabei stehen (E‑1076).

```
┌──────────────────────────────────────────────┐
│ Assistent  ▣ NEXANS       Neues Gespräch   × │  Kopf
├──────────────────────────────────────────────┤
│ Guten Tag, ich bin der Assistent des …       │  Verlauf — scrollt
│ Ich nehme Ihre Änderungswünsche … möglich:   │
│  • Anpassen der Werk-/Abladestellentabelle   │
│ Beschreiben Sie einfach, was Sie …           │
│                  ┌─────────────────────────┐ │
│                  │ Bitte für Werk 72332 …  │ │  Nachricht des Kunden
│                  └─────────────────────────┘ │
│ Bitte prüfen Sie den Vorschlag …             │  Antwort, reiner Text
│ ┌──────────────────────────────────────────┐ │
│ │ Anpassen der Werk-/Abladestellentabelle  │ │  Karte (§5)
│ │ …                                        │ │
│ └──────────────────────────────────────────┘ │
│ ⌛ Der Assistent antwortet …                 │  Warteanzeige
├──────────────────────────────────────────────┤
│ ┌──────────────────────────────────┐ [Senden]│  Eingabe
│ └──────────────────────────────────┘         │
│ Enter sendet, Umschalt+Enter: neue Zeile     │
└──────────────────────────────────────────────┘
```

**Der Verlauf** ist der einzige Scrollbereich der Chatbox und trägt `relative`
([`frontend-grundlagen.md`](frontend-grundlagen.md) §7, Bedingung 3). Er liegt nicht unter `main`,
sondern im Portal des Overlays; die Zahl der Scrollbereiche im Rahmen ändert sich nicht. Was neu
erscheint — die eigene Nachricht, die Warteanzeige, die Antwort, ein Hinweis —, kommt mit
`useInSicht` ins Bild, Oberkante zuerst (Bedingung 4); bewegt wird nur der Verlauf.

**Nachrichten:** Die des Kunden stehen rechts auf der gedämpften Fläche (`bg-muted`), die des
Assistenten links ohne Fläche. Beide als **ein Textknoten** in einem Absatz mit
`white-space: pre-wrap` (E‑1074): Was wie HTML oder Markdown aussieht, steht wörtlich da. Wer
spricht, steht für Vorleseprogramme als verborgene Beschriftung („Sie:“, „Assistent:“) davor.

**Die Begrüßung** (Punkt 2 in §1) eröffnet jedes Gespräch, auch nach „Neues Gespräch“. Drei Zeilen
aus der Sprachdatei (`texte.assistent.begruessung`: `gruss`, `einleitung`, `schluss`), zwischen der
zweiten und der dritten die Typen aus `typen` von `GET /api/assistent` als Liste. Wortlaut seit dem
19.10.2026, Vorgabe des Auftraggebers: „Guten Tag, ich bin der Assistent des Overlord Monitorings.“
· „Ich nehme Ihre Änderungswünsche an der EDI-Konfiguration entgegen und bereite daraus einen
Auftrag vor. Derzeit möglich:“ · Liste · „Beschreiben Sie einfach, was Sie ändern möchten. Bevor
etwas angelegt wird, zeige ich Ihnen den Auftrag zur Prüfung.“

**Die Warteanzeige** ist ein Zeichen (Sanduhr) und ein Satz, ohne Drehen und ohne Übergang
([`visuelles-konzept.md`](visuelles-konzept.md) §7).

**Die Eingabe:** ein mehrzeiliges Feld mit Beschriftung für Vorleseprogramme, „Senden“ daneben
und der Tastenhinweis darunter. Senden ist gesperrt, solange das Feld nur Leerraum enthält, der
Assistent antwortet oder das Gespräch abgelaufen ist. **Eine Länge prüft das Feld nicht**: Die
Grenze von 4.000 Zeichen kennt das Backend und meldet sie mit `hoechstensZeichen` (E‑1071 sinngemäß,
[`frontend-grundlagen.md`](frontend-grundlagen.md) §6). Der Entwurf im Feld bleibt beim Schließen
stehen.

**„Neues Gespräch“** beendet das offene Gespräch (`DELETE`, E‑1082), leert Verlauf und Karten und
zeigt die Begrüßung neu. Der Entwurf im Eingabefeld bleibt.

**Schmal** (unter 768 px, E‑1077): Das Overlay nimmt die volle Fensterbreite, ohne Rundung, und
reicht bis zum unteren Rand. Es deckt die Zeilen der Kopfzeile unter dem Symbol ab; den Mandanten
nennen deshalb Kopf und Karte (F9). Kein horizontales Scrollen.

---

## 5. Die Karte

```
┌──────────────────────────────────────────────┐
│ Anpassen der Werk-/Abladestellentabelle      │  Name des Typs
│ [Anpassung]  Gilt für: NEXANS Nexans …       │  Aktion, Mandant (E‑1069)
├──────────────────────────────────────────────┤
│ 1  72332/DDB0                                │  Nummer und Schlüssel (E‑1080)
│    Lieferantennummer  ┊leer┊                 │
│    Werk               72332                  │
│    Abladestelle       DDB0                   │
│    SNDPRN             120764                 │
│    SNDPRN_4908        ┊leer┊                 │
│                       ⓘ Für VDA-4908-Prozesse. Bei Anpassung heißt leer: der Wert wird entfernt.
│ …                                            │
│ Alle 20 anzeigen                             │  ab sechs Einträgen (F6)
├──────────────────────────────────────────────┤
│ [Auftrag anlegen]  [Ändern]                  │
└──────────────────────────────────────────────┘
```

**Kopf:** `name` des Vorschlags, die `aktion` als Marke, „Gilt für:“ mit Code und Anzeigename des
aktiven Mandanten wie in der Kopfzeile.

**Einträge:** je Eintrag die Nummer ab 1 — dieselbe Zählung wie im Klartext der Prüfung
(„Eintrag 3, SNDPRN: …“, [`tasks.md`](tasks.md) §5.3) — und der Schlüssel als Überschrift
(E‑1080), darunter **jedes Feld aus `felder`** in deren Reihenfolge mit seinem `label` (E‑1070).
Die Karte kennt keinen Typ: Sie zeigt, was der Vorschlag mitbringt, und sonst nichts.

**Leer** heißt `null` oder leerer Text, dieselbe Lesart wie im Backend ([`tasks.md`](tasks.md)
§5.3). Ein leeres Feld zeigt das Wort „leer“ in gestrichelter Umrandung und darunter, mit einem
Infozeichen und in voller Schriftfarbe, den `hinweis` des Felds — bei `WERK_ABLADESTELLE` etwa
„Bei Anpassung heißt leer: der Wert wird entfernt.“ Ein Feld ohne `hinweis` zeigt nur „leer“.
Werte stehen in fester Laufweite, Leerraum bleibt sichtbar erhalten.

**Ändern** (E‑1067, E‑1071): Jedes Feld wird ein Eingabefeld mit seinem `label`; unter einem leeren
steht weiterhin sein `hinweis`. Jede Änderung sperrt „Übernehmen“ und schickt 400 ms nach der letzten
Taste `POST /api/tasks/pruefung` mit `typ`, `aktion` und allen Werten der Karte (F5). Die Antwort
verteilt sich so:

| Fehler mit | steht |
|---|---|
| `index` und `feld` | unter dem Feld, `aria-invalid` am Eingabefeld |
| `index`, ohne `feld` (etwa `DOPPELT`, `MINDESTENS_EINS`) | unter der Überschrift des Eintrags |
| ohne `index` (etwa `ZU_VIELE_EINTRAEGE`) | oben auf der Karte |

Angezeigt wird die `meldung` des Backends, unverändert. „Übernehmen“ ist frei, wenn die letzte
Prüfung zum aktuellen Stand leer zurückkam; „Verwerfen“ ist immer frei. Scheitert die Prüfung
technisch, bleibt „Übernehmen“ gesperrt und „Erneut prüfen“ steht daneben. Ein `404` heißt: Diesen
Typ gibt es für den Mandanten nicht mehr; dann bleibt nur „Verwerfen“.

**Anlegen** (E‑1072, E‑1073): „Auftrag anlegen“ schickt `POST /api/tasks` mit der `anfrageId`, die
beim Erscheinen der Karte entstanden ist, und den übernommenen Werten. Während des Aufrufs ist der
Knopf gesperrt. Danach:

| Antwort | Karte |
|---|---|
| `201`, oder `200` mit dem vorhandenen Auftrag | gesperrt, „Angelegt, Status: Offen“ mit den Werten aus der Antwort (F7); kein Knopf mehr |
| `400` mit Prüffehlern | Fehler am Feld wie bei der Prüfung; „Ändern“ frei, „Auftrag anlegen“ gesperrt bis zur nächsten Übernahme (F8) |
| `404` | „Diesen Auftragstyp gibt es hier nicht.“ |
| Zeitüberschreitung, `5xx`, Netz | Hinweis auf der Karte, „Auftrag anlegen“ wieder frei — **mit derselben `anfrageId`**; ein schon angelegter Auftrag entsteht dabei nicht doppelt |

**Kein Wert wird verändert**, auch nicht um Leerraum gekürzt: Die Karte schickt, was im Feld
steht.

---

## 6. Verhalten bei Fehlern

| Antwort | Anzeige in der Chatbox |
|---|---|
| `429 zu-viele-nachrichten` und jede andere `429` | „Gerade kamen zu viele Nachrichten. Bitte warten Sie einen Moment und senden Sie dann erneut.“ Eingabe bleibt erhalten (E‑1081) |
| `429 gespraech-ausgeschoepft` | Hinweis mit der Höchstzahl aus `hoechstensNachrichten` und „Neues Gespräch“; Eingabe bleibt erhalten (E‑1081) |
| `400 nachricht-ungueltig` | Hinweis mit der Höchstzahl aus `hoechstensZeichen`; Eingabe bleibt erhalten |
| `409 nachricht-laeuft` | nichts angezeigt, `console.warn` (F12); Eingabe bleibt erhalten |
| `502`, `503` (auch beim Eröffnen), Netz | „Der Assistent ist gerade nicht erreichbar.“ Eingabe bleibt erhalten, erneut senden ist möglich |
| `404` auf das Gespräch | „Dieses Gespräch ist abgelaufen.“ mit „Neues Gespräch“; Senden gesperrt bis dahin, die Eingabe bleibt (E‑1076) |
| `401` | wie überall: Umleitung auf die Anmeldung, ohne Meldung ([`frontend-grundlagen.md`](frontend-grundlagen.md) §5) |
| Prüffehler beim Anlegen | wie bei der Prüfung am Feld, Karte bleibt änderbar (§5) |

Hinweise stehen am Ende des Verlaufs als `Alert`; ein neuer Versuch räumt sie weg. Fehlt eine Zahl
im Rumpf, steht derselbe Satz ohne sie ([`frontend-grundlagen.md`](frontend-grundlagen.md) §6:
„Eigene Felder neben `type`“).

---

## 7. Barrierefreiheit

- **Tastatur:** Das Symbol ist mit Tab erreichbar, Enter oder Leertaste öffnet. Beim Öffnen steht der
  Fokus im Eingabefeld, beim Schließen (Esc, „×“, Klick daneben) wieder auf dem Symbol. Enter sendet,
  Umschalt+Enter bricht um. Während der Antwort bleibt der Fokus im Feld (F11).
- **Karte:** „Ändern“ setzt den Fokus in das erste Eingabefeld, „Übernehmen“ und „Verwerfen“ zurück
  auf „Ändern“, nach dem Anlegen auf die Zeile „Angelegt, Status: Offen“.
- **Ansage:** Eine verborgene `aria-live="polite"`-Region sagt „Der Assistent antwortet …“ und
  danach die Antwort an, bei einer Karte mit Typ und Zahl der Einträge — nicht die ganze Karte.
  Hinweise sind `Alert` (`role="alert"`) und sagen sich selbst an.
- **Beschriftungen:** Die Chatbox ist ein Dialog mit dem Titel „Assistent“; Verlauf, Eingabefeld,
  „×“ und jede Karte haben einen zugänglichen Namen. Felder der Karte sind über `label` und
  `aria-describedby` mit Hinweis und Fehler verbunden. Kein Zustand nur über Farbe: Fehler tragen
  ein Zeichen und Text, „leer“ ein Wort.
- **Dichte und Thema:** nur Maße in `rem` und Farbrollen aus `globals.css`; hell und dunkel, alle
  vier Stufen. Am Berührungsgerät haben Knöpfe die Mindestfläche (`--dichte-bedienelement`).

---

## 8. Tests

**Gelaufen am 19.10.2026:** `pnpm check` grün — Lint, Typprüfung, Format, **1.424 Fälle in 52
Dateien**, davon **58 neu** (35 rein, 23 gerendert). `next build` grün. Die Zahl der gerenderten Fälle
(jetzt 256 in 27 Dateien) steht im Kopf von `frontend/vitest.config.mts`, aus dem Lauf gezählt.

| Datei | Was |
|---|---|
| `tests/assistent.test.ts` | **35 Fälle, rein.** Das Gespräch: Begrüßung ohne Gespräch beim Backend; gesendet heißt im Verlauf, Feld leer, warten; nur Leerraum geht nicht hinaus; die Antwort mit Vorschlag wird eine Karte mit ihrer `anfrageId`; gescheitert heißt zurück ins Feld und aus dem Verlauf (F4); abgelaufen sperrt bis „Neues Gespräch“, das den Entwurf behält; eine späte Antwort kommt nicht mehr an. Die Karte: Prüfung sperrt „Übernehmen“, Fehler sperren weiter, frei gibt frei; die Antwort auf einen älteren Stand verfällt (F5); „Verwerfen“; `typUnbekannt` und `gestoert` sperren; angelegt mit den Werten der Antwort (F7) und danach unveränderlich; die Sperre nach Prüffehlern bis zur nächsten Übernahme (F8); dieselbe `anfrageId` nach einer Störung; der Rumpf trägt nur `typ`, `aktion` und Werte. Wohin ein Fehler gehört; die Überschrift mit Rückfall (E‑1080), `null` und `""` gleich; fünf offen, sonst alle (F6). **Jede Zeile der Fehlertabelle nach Problemtyp** (E‑1081), die Zahl in beiden Sprachen und der Satz ohne sie; die Ansage; die Fehlerliste; die `anfrageId` mit und ohne `randomUUID` |
| `tests/assistent-chatbox.test.tsx` | **23 Fälle, gerendert**, gegen ein gestelltes `fetch`. Das Symbol fehlt bei `verfuegbar: false` (und es geht keine Anfrage außer dem Stand hinaus) und bei einem Fehler; es steht mit Beschriftung und Tooltip links neben dem Nutzermenü. Fokus im Feld beim Öffnen, am Symbol nach Esc und „×“; die Begrüßung mit den Typen ohne Aufruf. Erst eröffnen, dann senden, mit dem Text als einzigem Inhalt; während der Antwort `readOnly`, `aria-disabled`, Warteanzeige, Ansage, kein zweites Senden; Umschalt+Enter sendet nicht. **Ein Text mit `<img src=x onerror=alert(1)>`, `**fett**` und einem Markdown-Verweis steht als genau ein Textknoten da, kein Element** (E‑1074). Schließen und Wiederöffnen behalten Verlauf, Entwurf und Gespräch; „Neues Gespräch“ schickt `DELETE`. Die Karte mit Typ, Aktion, „Gilt für: NEXANS …“, jedem Feld, „leer“ mit und ohne Hinweis; Ändern, „Übernehmen“ gesperrt vor dem Aufruf und während er läuft, der Fehler des Backends am Feld mit `aria-invalid`, nach Korrektur frei, danach der Rückfall der Überschrift und der Fokus auf „Ändern“; fünf von 20 Einträgen und „Alle 20 anzeigen“. Anlegen mit den Werten der Karte und der `anfrageId`, nach einem `503` dieselbe, danach gesperrt mit „Angelegt, Status: Offen“ und dem Fokus darauf; Prüffehler beim Anlegen am Feld. **Jede Zeile der Fehlertabelle**: `429` zweimal, `400`, `502`, `503` (auch beim Eröffnen), `409`, `404`. Der Mandantenwechsel setzt zurück und eröffnet beim nächsten Senden neu |
| `tests/sprachdateien.test.ts` | ein Fall erweitert: auch die beiden `404` des Assistenten („abgelaufen“, „gibt es hier nicht“) tragen kein Wort über Berechtigung |

**Verletzungsproben**, je ein Eingriff in den Code, Lauf über beide neuen Dateien, Rücknahme per
`git checkout` gegen den committeten Stand:

| Eingriff | rot |
|---|---|
| Symbol ohne Prüfung auf `verfuegbar` | „fehlt bei verfuegbar: false“ |
| Antwort als HTML (`dangerouslySetInnerHTML`) | „wörtlich als einen Textknoten“ |
| neue `anfrageId` je Versuch | „beim zweiten Versuch dieselbe“ |
| „Übernehmen“ ohne Prüfstand | 5 Fälle, gerendert und rein |
| Mandant nicht als `key` | „Mandantenwechsel setzt die Chatbox zurück“ |
| Feld während der Antwort nicht `readOnly` | „sperrt während der Antwort“ |
| Fokus beim Öffnen nicht ins Feld | „öffnet mit dem Fokus im Eingabefeld“ |
| `429` nach Statuscode | 2 Fälle (ausgeschöpft, gerendert und rein) |
| `DELETE` auch für das abgelaufene Gespräch | „404 auf das Gespräch“ |
| Hinweis auch bei gefülltem Feld | „zeigt jedes Feld, leere ausdrücklich mit Hinweis“ |
| Eingabe nach einem Fehler nicht zurück | 10 Fälle |
| **Eichung:** ein Wort in einem Kommentar | **0** — überlebt, wie es soll |

### 8.1 Sichtprobe M1004 (19.10.2026)

**Wie:** Ein eigenes Chrome mit Debug-Port, angemeldet von Lukas (ADMIN), Mandant `NEXANS`, gefahren
über das DevTools-Protokoll (`Emulation.setDeviceMetricsOverride`, Thema und Dichte über
`data-thema` und `data-dichte`). **Der Assistent blieb aus** (`OVERLORD_ASSISTENT_AKTIV=false`):
`GET /api/assistent`, die Gespräche, `POST /api/tasks/pruefung` und `POST /api/tasks` beantwortete
ein Ersatz von `window.fetch` in der Seite, mit erfundenen Werten (Werk `47110`, Abladestellen
`TOR3`, `TOR4`). Kein Text ging an Anthropic, kein Auftrag in `task`. Die Aufnahmen liegen nur
lokal und sind nicht eingecheckt.

**Befund, behoben:** Unter 768 px war die Chatbox nur `min(40rem, verfügbare Höhe)` hoch; bei
390 × 844 endete sie bei 686 px, und darunter schien die Seite durch. Das widersprach E‑1077 und §4.
Seither nimmt sie dort die ganze verfügbare Höhe: **798 px** (844 − 46) bei 390 × 844, volle Breite,
`scrollWidth` 390.

**Lage der Chatbox** bei 1440 × 900: rechtsbündig unter dem Symbol, links 915, oben 47; 420 × 640
in `m`, **368 × 560 in `xs`, 473 × 720 in `l`** — Breite und Höhe gehen in `rem` mit der Dichte.
`scrollWidth` des Dokuments in allen Aufnahmen gleich der Fensterbreite.

**Kopfzeile mit dem Symbol** (Punkt 1018, zweiter Teil): In allen 16 Lagen — `xs`, `s`, `m`, `l` je bei
768, 800, 1.024 und 1.280 px — ist die Zeile der Kopfzeile genau so breit wie das Fenster
(`scrollWidth = clientWidth`), **kein Überlauf**. Nachgegeben hat, wie in E‑232 vorgesehen, allein der
Produktname. Gegenprobe mit ausgeblendetem Symbol, Breite des Produktnamens in px:

| Dichte | 768 ohne / mit | 800 ohne / mit | 1.024 ohne / mit |
|---|---|---|---|
| `xs` | 151 / 151 | 151 / 151 | 151 / 151 |
| `s` | 161 / 134 | 161 / 161 | 161 / 161 |
| `m` | 131 / 93 | 163 / 125 | 172 / 172 |
| `l` | **52 / 9** | 84 / 41 | 120 / 77 |

**Bei 768 px in `l` bleibt vom Produktnamen nur die Auslassung** (9 px); der volle Name steht im
`title`. Ob das so bleiben darf, entscheidet die Sichtprüfung 1018 — die Alternative wäre, dort etwas
anderes nachgeben zu lassen, etwa den Benutzernamen im Nutzermenü, und das ist eine Entscheidung
über die Kopfzeile, nicht über A3.

**Gesehen** (Aufnahmen hell und dunkel, 1440 × 900 und 390 × 844, dazu `xs`, `l`, Änderungsmodus mit
Prüffehler, 20 Einträge): Kopf mit Mandant, Verlauf, Karte mit „leer“ samt Hinweis, Fehler des
Backends am Feld mit rotem Rand und Zeichen, „Alle 20 anzeigen“ unter fünf Einträgen. **Beobachtet,
kein Befund:** Kommt eine Antwort mit Karte, bleibt der Verlauf stehen, wenn die Oberkante der
Antwort schon im Bild ist — die Karte beginnt dann weiter unten, und „Auftrag anlegen“ liegt unter
dem Rand. Das ist die Regel aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §7, Bedingung 4
(„steht es schon im Bild, bewegt sich nichts“); ob sie im Chat trägt, gehört zu Sichtprüfung 1019.

**Nicht geprüft:** echte Antworten des Modells, ein Vorleseprogramm, die Bedienung von Hand mit der
Tastatur und am Finger. Dafür stehen die Sichtprüfungen in §11.

---

## 9. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | kein neuer Endpunkt; keine Anfrage des Moduls trägt eine Mandanten-ID |
| **Regel 3** | keine; das Frontend schreibt nur über `POST /api/tasks` in `overlord_monitor` |
| **Keine Zeichenkette in einer Komponente** | alle Texte in `de.ts` und `en.ts` unter `assistent` (E‑1079) |
| **Kein Farbwert** | nur Farbrollen und Tokens; `tests/farbwerte.test.ts` |
| **Keine eigene Datenhaltung** | kein lokaler Speicher, keine URL; der Verlauf liegt im Arbeitsspeicher und endet mit dem Neuladen (F3) |
| **Mandantenwechsel leert** | der Zustand hängt am Mandanten und wird beim Wechsel neu eingehängt; der Zwischenspeicher wird wie bisher geleert |

---

## 10. Offene Punkte

Nummern ab 1017 (Linie `assistent`, per `grep` geprüft); dieselben Zeilen stehen im Register
[`offene-punkte.md`](offene-punkte.md).

| Nr. | Punkt | Art |
|---|---|---|
| **1017** | Nach einer übernommenen Änderung kennt die Karte den Schlüssel des Eintrags nicht mehr und zeigt „Eintrag n“ (E‑1080). `POST /api/tasks/pruefung` könnte den Schlüssel je Eintrag mitliefern, wie der Vorschlag es tut. | Verbesserung |
| **1018** | Sichtprüfung 1: Chatbox mit Karte hell und dunkel, breit und schmal, in `xs` und `l` (§11). Dazu, ob die Kopfzeile mit dem zusätzlichen Symbol bei 768 px in `l` noch ohne Überlauf steht. **Gemessen am 19.10.2026 (M1004, §8.1):** kein Überlauf in 16 Lagen; der Produktname schrumpft bei 768 px in `l` von 52 auf 9 px. Offen bleibt das Urteil von Hand. | Sichtprüfung oder Messung offen |
| **1019** | Sichtprüfung 2: Ist eine Karte mit 20 Einträgen noch zu überblicken? (§11) | Sichtprüfung oder Messung offen |
| **1020** | Sichtprüfung 3: Ist bei einer Anpassung mit leerem Feld „wird entfernt“ unübersehbar? (§11) | Sichtprüfung oder Messung offen |
| **1021** | Sichtprüfung 4: Öffnen, schreiben, Karte ändern und anlegen nur mit der Tastatur (§11) | Sichtprüfung oder Messung offen |
| **1022** | Sichtprüfung 5: Werden Antworten und Karte mit einem Vorleseprogramm verständlich angesagt? (§11) | Sichtprüfung oder Messung offen |

**Aus A2 fortgeschrieben:**

- **Punkt 1011** (Streaming für A3) ist für A3 **entschieden**: kein Streaming, eine Warteanzeige
  (E‑1075). *Vermerk vom 19.10.2026.*
- **Punkt 1012** (was das Gespräch von Karte und Anlegen erfährt) bleibt **offen** (E‑1078).

---

## 11. Sichtprüfungen für Lukas

Mit eingeschaltetem Assistenten, lokal. **Der Text, den man schreibt, geht an Anthropic in die USA**
(E‑1029); also nur erfundene Werte verwenden.

**Einschalten** ([`START-LOKAL.md`](START-LOKAL.md) §6, [`assistent.md`](assistent.md) §7):

1. Einen Zweig mit A3 auschecken — bis zum Merge `assistent-a3-chatbox`.
2. Backend und Frontend mit dem Startskript starten, **mit dem Schalter**:

   ```
   .\scripts\dev-start.ps1 -Assistent
   ```

   Das Skript setzt im Backend-Fenster `OVERLORD_ASSISTENT_AKTIV=true` und meldet beim Start den
   Zweig und ob `OVERLORD_ASSISTENT_API_KEY` gesetzt ist. Ohne Schlüssel bleibt der Assistent aus;
   ohne `-Assistent` ebenso, auch wenn die Variable in der Umgebung steht.
3. Anmelden und einen Mandanten wählen, für den `WERK_ABLADESTELLE` freigeschaltet ist: `NEXANS` oder
   `IBIS` (Testkopie, Stand 19.10.2026, [`tasks.md`](tasks.md) §4.2). Bei jedem anderen meldet
   `GET /api/assistent` `verfuegbar: false`, und das Symbol fehlt — das ist zugleich die Gegenprobe
   zu E‑1068.
4. Das Symbol links neben dem Nutzermenü öffnet die Chatbox.

**Angelegte Aufträge** stehen danach mit Status `OFFEN` in `overlord_monitor.task` der Testkopie
und werden von keiner Plattform abgeholt. Aufräumen nach der Prüfung, nur die eigenen:
`DELETE FROM overlord_monitor.task WHERE angelegt_von = '<eigener Benutzername>' AND status = 'OFFEN';`
— vorher mit `SELECT` ansehen ([`tasks.md`](tasks.md) §5.1 nennt die Spalten).

| Nr. | Prüfung | Vorgehen |
|---|---|---|
| **1018** | Hell und dunkel, breit und schmal, kleinste und größte Dichtestufe | Je Thema (Nutzermenü) in `xs` und `l`: bei voller Breite und bei rund 360 px eine Nachricht schicken, die zu einer Karte führt (etwa „Bitte bei Werk 72332, Abladestelle DDB0 die SNDPRN auf 120764 setzen“). Lesbar, kein horizontales Scrollen, Karte in der Box, Kopfzeile ohne Überlauf bei 768 px in `l` |
| **1019** | Ein Vorschlag mit 20 Einträgen | 20 Zeilen aus Excel einfügen, alle fünf Felder. Erst fünf offen, „Alle 20 anzeigen“; ist die Karte noch zu überblicken, auch im Änderungsmodus? |
| **1020** | Eine Anpassung mit leerem Feld | Eine Anpassung, bei der SNDPRN_4908 nicht genannt wird. Ist „leer“ mit „der Wert wird entfernt“ unübersehbar? |
| **1021** | Nur mit der Tastatur | Tab bis zum Symbol, Enter, schreiben, Enter; auf der Karte „Ändern“, einen Wert ändern, auf die Prüfung warten, „Übernehmen“, „Auftrag anlegen“; Esc. Wo steht der Fokus jeweils? |
| **1022** | Mit Vorleseprogramm (NVDA oder die Sprachausgabe von Windows) | Werden Warteanzeige, Antwort und „Vorschlag auf der Karte …“ angesagt? Sind Felder, „leer“ mit Hinweis und Fehler am Feld verständlich? |
