# Assistent im Frontend: Chatbox, Bestätigungskarte und „Ihre Aufträge“ (A3, A4)

*Angelegt am 19.10.2026 mit Teilschritt A3 der Linie `assistent`* (Zweig `assistent-a3-chatbox`,
[`ROADMAP.md`](ROADMAP.md) §2). Er baut auf den Endpunkten aus A1 ([`tasks.md`](tasks.md) §6) und A2
([`assistent.md`](assistent.md) §6) auf. **Kein Backend-Umbau.**

A3 baut die Oberfläche des Assistenten: ein Symbol in der Kopfleiste, eine Chatbox als Overlay und
die Karte, auf der der Kunde einen Vorschlag prüft, ändert und anlegt. Der Bereich „Ihre Aufträge“
folgt mit A4.

> **Ergänzt am 19.10.2026 mit A4** (Zweig `assistent-a4-ihre-auftraege`): Der Block „Ihre Aufträge“
> in der Übersicht zeigt, was aus den Aufträgen geworden ist, und lässt einen offenen Auftrag
> löschen — §12. Dafür kommen ein Endpunkt (`DELETE /api/tasks/{taskId}`) und die `label` der
> Felder in der Antwort dazu ([`tasks.md`](tasks.md) §6.5, §6.6). **Mit A4 ist die Ausbaustufe
> Aufträge vollständig.** Chatbox und Karte ändern sich nur an einer Stelle: Nach dem Anlegen lädt
> die Liste der Aufträge neu.

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

> **Bestätigt am 19.10.2026** (Auftraggeber, vor A4): F1 bis F15 sind **E‑1083 bis E‑1097**, nach
> der Regel aus A2 — letzte vergebene Nummer (E‑1082) plus F-Nummer. Die F-Nummer steht in Klammern
> dahinter, damit Verweise von vorher auffindbar bleiben.
>
> **Nachtrag zu E‑1084 vom selben Tag (E‑1124, A):** Gesiezt wird in der Chatbox **und im Block
> „Ihre Aufträge“** der Übersicht (§12). Die übrige Oberfläche duzt weiter.

| E (F) | Festlegung | Grund |
|---|---|---|
| **E‑1083** (F1) | **Keine Chat-Bausteine aus dem Generator, kein Generator-`textarea`.** Chatbox und Karte bestehen aus den vorhandenen Bausteinen (`Popover`, `Button`, `Badge`, `Alert`, `Input`, `Label`); das Eingabefeld ist ein `textarea` im Feature mit den Klassen von `components/ui/input.tsx`. | `shadcn add message-scroller message bubble marker` brächte am 19.10.2026 zwei neue Laufzeitabhängigkeiten (`@shadcn/react`, `cn`) und überschriebe `button.tsx`; selbst `textarea` zieht inzwischen `cn` nach. Der Scroller folgt dem Ende eines Streams, das Projekt holt aber die **Oberkante** des Neuen ins Bild (`lib/in-sicht-bringen.ts`, [`frontend-grundlagen.md`](frontend-grundlagen.md) §7, Bedingung 4) |
| **E‑1084** (F2) | **In der Chatbox wird gesiezt**, wie der Assistent selbst (E‑1044); die übrige Oberfläche duzt weiter. | Begrüßung, Hinweise und Antworten stehen im selben Verlauf; ein Wechsel der Anrede mitten im Gespräch läse sich wie zwei Absender |
| **E‑1085** (F3) | **Verlauf und Karten liegen im Arbeitsspeicher der Kopfzeile**, als ein Zustand über `useReducer` mit einer reinen Übergangsfunktion (`features/assistent/gespraech.ts`). Nicht in TanStack Query, nicht im lokalen Speicher, nicht in der URL. | Kein Endpunkt liefert den Verlauf zurück; das Backend hält ihn nur für die Gesprächsführung (E‑1038). Die Kopfzeile lebt im Layout der angemeldeten Routen und übersteht jede Navigation (E‑1076). Schlüssel ist der Mandant: Ein Wechsel hängt den Zustand neu ein |
| **E‑1086** (F4) | **Gesendet heißt: im Verlauf.** Die Nachricht steht sofort im Verlauf, das Eingabefeld wird leer. Scheitert der Aufruf, verschwindet sie aus dem Verlauf und steht wieder im Eingabefeld. | Das Backend nimmt eine gescheiterte Nachricht nicht in den Verlauf (E‑1052); stünde sie trotzdem im Chat, zeigte er ein anderes Gespräch als das, das der Assistent kennt. „Die Eingabe bleibt erhalten“ (Fehlertabelle) heißt damit: wörtlich im Feld |
| **E‑1087** (F5) | **Geprüft wird 400 ms nach der letzten Änderung.** Jede Änderung sperrt „Übernehmen“ sofort; eine Antwort auf einen älteren Stand wird verworfen. | Gewählt, nicht gemessen. Kurz genug, dass die Prüfung beim Weiterlesen schon da ist, lang genug, dass beim Tippen nicht je Taste ein Aufruf hinausgeht |
| **E‑1088** (F6) | **Mehr als fünf Einträge:** Die ersten fünf stehen offen, darunter „Alle <n> anzeigen“. Im Änderungsmodus und wenn ein verborgener Eintrag einen Fehler trägt, stehen alle offen. | „Alle n anzeigen“ setzt voraus, dass schon welche zu sehen sind; ein Fehler und ein Eingabefeld dürfen nicht hinter einem Schalter liegen |
| **E‑1089** (F7) | **Nach dem Anlegen zeigt die Karte die Werte aus der Antwort** von `POST /api/tasks`, nicht die eigenen; der Status steht als „Offen“, jede andere Einordnung als ihr Rohwert. | Mit derselben `anfrageId` (E‑1072) antwortet das Backend nach einer Zeitüberschreitung mit dem **vorhandenen** Auftrag, auch wenn die Karte inzwischen andere Werte trägt ([`tasks.md`](tasks.md) §6.2). Die Karte zeigt, was angelegt ist. Die Wörter für die übrigen Einordnungen gehören zu A4 |
| **E‑1090** (F8) | **„Auftrag anlegen“ ist gesperrt**, solange Prüffehler aus dem Anlegen am Feld stehen, und im Änderungsmodus nicht angeboten: Erst „Übernehmen“ oder „Verwerfen“. | Dieselben Werte noch einmal zu schicken, brächte dieselben Fehler. Angelegt werden soll, was auf der Karte steht, nicht ein halber Entwurf |
| **E‑1091** (F9) | **Der Kopf der Chatbox nennt den Mandanten** (Code), zusätzlich zur Karte. | Unter 768 px deckt die Chatbox die zweite Zeile der Kopfzeile ab, in der der Mandant steht ([`visuelles-konzept.md`](visuelles-konzept.md) §6: „Der aktive Mandant bleibt in der Kopfzeile“). Der Assistent verweist bei einem fremden Mandanten auf den Wechsel dort (E‑1059) |
| **E‑1092** (F10) | **Feste Höhe** `min(40rem, verfügbare Höhe)`; nur der Verlauf scrollt. | Eine Box, die mit jeder Nachricht wächst, bewegt die Fläche unter der Hand (Leitsatz „ruhig“, [`visuelles-konzept.md`](visuelles-konzept.md) §1) |
| **E‑1093** (F11) | **Gesperrt heißt `readOnly` und `aria-disabled`, nicht `disabled`:** das Eingabefeld während der Antwort, „Senden“, „Übernehmen“, „Auftrag anlegen“ und „Neues Gespräch“. Der Klick prüft die Sperre selbst. | Ein gesperrtes Element verlöre unter der Tastatur den Fokus, gerade wenn es eben gedrückt wurde — wie beim Knopf „Neu laden“ ([`neu-laden.md`](neu-laden.md)); nach der Antwort schreibt der Kunde dort weiter, wo er war |
| **E‑1094** (F12) | **`409 nachricht-laeuft` wird nicht angezeigt**, nur mit `console.warn` gemeldet; die Eingabe kommt ins Feld zurück. | Fehlertabelle: „darf durch die Sperre nicht vorkommen; wenn doch, still ignorieren und loggen“. `console.error` ließe die Tests fehlschlagen und ist für Befunde da |
| **E‑1095** (F13) | **„Neues Gespräch“ geht auch während einer Antwort.** Die späte Antwort kommt nicht in das neue Gespräch; ein Gespräch, das erst danach eröffnet wurde, wird sofort beendet. | Der Kunde soll nicht auf eine Antwort warten müssen, die er nicht mehr will. Gezählt wird über eine Generation je Gespräch im Haken, nicht über die Antwort |
| **E‑1096** (F14) | **„Neues Gespräch“ ist gesperrt, solange das Gespräch frisch ist** — nur die Begrüßung, kein Hinweis, nichts eröffnet. | Ein Knopf, der nichts tut, wäre ein Klickversprechen ins Leere (dieselbe Regel wie beim Mandantenumschalter, [`visuelles-konzept.md`](visuelles-konzept.md) §5) |
| **E‑1097** (F15) | **Die `anfrageId` entsteht auch ohne sicheren Kontext**: `crypto.randomUUID`, sonst eine UUID der Version 4 aus `crypto.getRandomValues`. | `randomUUID` gibt es nur unter HTTPS oder auf `localhost`; hinter einem Proxy ohne TLS fehlte sie still, und das Anlegen schiene zu gehen |

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
| `GET /api/assistent` | `useQuery`, Schlüssel `["assistent", "stand", <Mandant>]` (seit 1.1.0 je Mandant, §13; vorher `["assistent", "stand"]`) | beim Einhängen der Kopfzeile; entscheidet über das Symbol und liefert die Typen der Begrüßung |
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

1. Einen Zweig mit A3 auschecken — bis zum Merge `assistent-a3-chatbox`, seit dem 19.10.2026
   `assistent`.
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

---

## 12. Ihre Aufträge (A4)

*Teilschritt A4 der Linie `assistent`, 19.10.2026, Zweig `assistent-a4-ihre-auftraege`.* Der Kunde
legt Aufträge über den Assistenten an; die Plattform holt sie ab, führt sie aus und meldet je Eintrag
zurück ([`task-schnittstelle.md`](task-schnittstelle.md)). Bis A4 sah er davon nur „Angelegt,
Status: Offen“ auf der Karte. Der Block **„Ihre Aufträge“** in der Übersicht zeigt kompakt, was aus
seinen Aufträgen geworden ist, und lässt ihn einen Auftrag löschen, solange die Plattform ihn noch
nicht abgeholt hat.

**Der Block hängt nicht am Assistenten** (E‑1098): Auch bei abgeschaltetem Assistenten zeigt die
Übersicht vorhandene Aufträge. Neu im Backend sind nur `DELETE /api/tasks/{taskId}` und das Feld
`felder` in der Antwort ([`tasks.md`](tasks.md) §6.5, §6.6).

### 12.1 Entscheidungen der Oberfläche

Herkunft: **A** = vom Auftraggeber entschieden, mit Datum. **V** = Vorschlag vom 19.10.2026 (Entwurf
aus dem Design-Canvas, vom Auftraggeber abgenommen), ohne Einspruch übernommen. Die E‑Nummer ist
`E‑1097` plus die Nummer der Entscheidung im Auftrag zu A4; nächste freie Nummer war E‑1098, nachdem
F1 bis F15 als E‑1083 bis E‑1097 bestätigt waren (§2), per `grep` über das Repository geprüft. Die
Entscheidungen 13, 16 bis 18 und 20 bis 22 betreffen Endpunkt und Plattform und stehen in
[`tasks.md`](tasks.md) §2 (E‑1110, E‑1113 bis E‑1115, E‑1117 bis E‑1119).

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1098** | Der Block erscheint **nur, wenn `GET /api/tasks` mindestens einen Auftrag liefert**. Sonst fehlt er ganz, ohne leeren Rahmen, und die Übersicht sieht aus wie vorher. Er hängt nicht an der Verfügbarkeit des Assistenten: Vorhandene Aufträge bleiben sichtbar, auch wenn der Assistent abgeschaltet ist. | A (19.10.2026) |
| **E‑1099** | Der Block steht **in der Reihe von „Zuletzt aufgefallen“, zwischen „Zuletzt aufgefallen“ und Verteilung**. „Zuletzt aufgefallen“ teilt sich mit ihm den Platz, den es vorher allein hatte; die Verteilung behält ihre Breite. Ohne Auftrag ist die Reihe wie vorher. Am schmalen Fenster stehen die drei untereinander, in derselben Reihenfolge. | A (19.10.2026) |
| **E‑1100** | **Kein Knopf „Neuer Auftrag“**, keine Filter, keine eigene Seite. Aufträge entstehen nur über das Symbol des Assistenten in der Kopfleiste. | A (15. und 19.10.2026) |
| **E‑1101** | Der Block zeigt, was `GET /api/tasks` liefert, neueste zuerst. **Fünf Aufträge sichtbar**, darüber „1 weiteren anzeigen“ oder „<n> weitere anzeigen“, danach „Weniger anzeigen“. | V |
| **E‑1102** | **Jede Zeile hat zwei Zeilen und ist zugeklappt.** Oben: Stand (Zeichen und Wort), Aktion, Zahl der Einträge („Neuanlage · 2 Einträge“). Darunter links die ersten zwei Schlüssel, bei mehr „und <n> weitere“, in Festbreitenschrift und bei Platzmangel mit Auslassung; rechts der Zeitpunkt des Einstellens. Bei „Teilweise erledigt“ steht statt der Schlüssel „<x> von <n> nicht erfolgreich“ in der Farbe des Stands. | V (Entwurf 19.10.2026) |
| **E‑1103** | **Der Name des Typs** steht in der aufgeklappten Zeile. Kommen in der Liste Aufträge mehrerer Typen vor, steht er zusätzlich oben vor der Aktion. | V |
| **E‑1104** | **Aufgeklappt:** Name des Typs, „eingestellt <Zeit>“ und gegebenenfalls „ausgeführt <Zeit>“, bei „Fehlgeschlagen“ der `fehlertext` als „Meldung der Plattform“, der Hinweis des Stands, dann die Einträge: Schlüssel, die Werte mit ihrem `label` und, wo es eines gibt, das Ergebnis. Bis fünf Einträge, darüber „Alle <n> Einträge anzeigen“. Aufklappen mit der Tastatur, `aria-expanded` am Auslöser. | V |
| **E‑1105** | Stände und ihre Anzeige nach §12.4. Jeder Stand hat Zeichen **und** Wort, Farbe nie allein. „Fehlgeschlagen“ trägt die Fehlerrolle. **„Teilweise erledigt“ fällt auf, ohne wie ein Fehler auszusehen** (im Entwurf Bernstein); welche Rolle, ist F16. | V |
| **E‑1106** | Das Ergebnis je Eintrag zeigt den Rohwert der Plattform („Schlüssel nicht gefunden“) mit Zeichen; „erfolgreich“ oder „nicht erfolgreich“ aus `ergebnis` steht für Vorleseprogramme dabei. Ein nicht erfolgreicher Eintrag hat einen leicht getönten Grund. | V |
| **E‑1107** | `fehlertext` und Rückgabewerte schreibt die Plattform: **reiner Text**, nie HTML. | V |
| **E‑1108** | Zeitpunkte kommen in UTC und werden im Format von „Zuletzt aufgefallen“ gezeigt. In welcher Zone, sagt E‑1122. | V |
| **E‑1109** | Aktualisierung über den vorhandenen Mechanismus der Übersicht: „Neu laden“ lädt den Block mit. Kein eigener Takt. Nach dem Anlegen in der Chatbox und nach dem Löschen wird die Liste sofort neu geladen. Zum Abfrageschlüssel E‑1123. | V |
| **E‑1111** | **„Auftrag löschen“ steht nur in der aufgeklappten Zeile**, unter den Einträgen rechts, als Knopf mit Papierkorb und Wort. In der zugeklappten Zeile steht nichts dafür. | A (19.10.2026) |
| **E‑1112** | Ein Klick zeigt an derselben Stelle die Rückfrage „Auftrag löschen? Die Plattform hat ihn noch nicht abgeholt. Er wird nicht ausgeführt.“ mit „Abbrechen“ und „Löschen“. Der Fokus geht auf „Abbrechen“, Esc bricht ab. Danach steht oben im Block „Auftrag gelöscht. Er wird nicht ausgeführt.“ (`role="status"`). Kein Rückgängig. | A (Rückfrage, 19.10.2026), V (Wortlaut) |
| **E‑1116** | Die Übersicht lädt nicht von selbst neu (E‑164), der Stand „Offen“ kann also veraltet sein. Bei `409` steht in der Zeile „Die Plattform hat den Auftrag gerade übernommen. Löschen geht nicht mehr.“, und die Liste wird neu geladen. Bei `404` „Der Auftrag ist nicht mehr vorhanden.“ und Neuladen. | V |

### 12.2 Rückfragen zu A4 (19.10.2026)

Fünf Punkte widersprachen beim Lesen einer Regel des Projekts oder ließen sich aus Auftrag und
Backend nicht entscheiden; **vom Auftraggeber am 19.10.2026 entschieden**, jeweils der vorgeschlagene
Weg. Dazu kam die Vorbedingung des Auftrags: A3 war nicht nach `assistent` gemergt. Auf Wunsch des
Auftraggebers hat Claude Code den Merge gemacht (`6ec3563`, `--no-ff`, lokal), und F1 bis F15 sind
bestätigt (§2).

| E | Entscheidung | Anlass | Herkunft |
|---|---|---|---|
| **E‑1120** | **Die `label` der Felder kommen aus der Kopie der Definition in der Antwort** — steht in [`tasks.md`](tasks.md) §2 und §6.5. | `GET /api/tasks` lieferte je Eintrag nur Feldnamen; der Auftrag verlangte, bei einer fehlenden Angabe anzuhalten | A |
| **E‑1121** | **Im Leer- und im Fehlerzustand der Übersicht steht der Block allein**, wenn es Aufträge gibt oder ihre Abfrage scheitert: im Leerzustand zwischen Satz und Stand, im Fehlerzustand unter der Meldung. E‑p bekommt dafür einen Nachtrag ([`dashboard-frontend.md`](dashboard-frontend.md) §5.7). | Die Reihe gibt es nur, wenn die Übersicht Nachrichten hat. Aufträge hängen nicht am Zeitraum: Ohne diese Regel wäre ein eben angelegter Auftrag bei leerem Zeitraum unsichtbar | A |
| **E‑1122** | **Zeitpunkte in der Anzeigezone** mit `formatiereZeitpunkt`, wie in „Zuletzt aufgefallen“, die Sekunden im `title`. „Ortszeit des Browsers“ aus Entscheidung 11 ist als „Ortszeit = Anzeigezone“ gelesen. | `lib/format.ts` formatiert in der festen Zone aus der Selbstauskunft und schließt die Browserzone ausdrücklich aus; auf derselben Seite stünden sonst zwei Uhren | A |
| **E‑1123** | **Der Abfrageschlüssel trägt keinen Mandanten**: `["auftraege", "liste"]`. Entscheidung 12 verlangte ihn mit dem aktiven Mandanten. | Projektregel: Beim Mandantenwechsel wird der ganze Zwischenspeicher geleert, und der Mandant steht deshalb in keinem Schlüssel (`lib/zwischenspeicher.ts`, [`frontend-grundlagen.md`](frontend-grundlagen.md) §5). Das Leeren leistet, was die Entscheidung sichern will. Die Übersicht kennt den Mandanten nicht; dafür bräuchte es eine neue Naht | A |
| **E‑1124** | **Im Block wird gesiezt**, wie vorgegeben („Ihre Aufträge“, „können Sie ihn noch löschen“). Nachtrag zu E‑1084 (§2). | E‑1084 sagte: „die übrige Oberfläche duzt weiter“. Der Block setzt fort, was im Gespräch entstanden ist, und „Ihre Aufträge“ ist sein Name seit A1 | A |

### 12.3 Festlegungen der Umsetzung (F16 bis F25)

Was der Auftrag offenließ, beim Bau festgelegt und **zur Bestätigung** vorgeschlagen, wie in A3;
eine E‑Nummer bekommen sie erst damit. Die Zählung setzt die F-Nummern aus A3 fort, damit jede
F-Nummer in dieser Datei eindeutig bleibt. F26 bis F28 betreffen das Backend und stehen in
[`tasks.md`](tasks.md) §2; F29 ist beim Messen dazugekommen.

| F | Festlegung | Grund |
|---|---|---|
| **F16** | **„Teilweise erledigt“ bekommt eine eigene Rolle `--teilweise`** mit Vordergrund, Fläche und Kontur in beiden Blöcken von `globals.css` — **mit den Werten von `--ueberfaellig`** (Bernstein, Ton 80). Zugeordnet wird sie in `lib/status-farbe.ts` über eine eigene Tabelle `AUFTRAG_ZUORDNUNG`; gerechnet und gehalten wie die übrigen Rollen (`scripts/farbwerte/rechne.mjs`, `tests/farbkontrast.test.ts`). | **`--ueberfaellig` ist der Kategorie *Überfällig* vorbehalten**, auch seit sie entfallen ist und die Rolle keinen Verbraucher hat (E‑71, E‑77): „Sie hier zu nehmen hieße, einen gerechneten Wert für einen zweiten Sachverhalt zu vergeben“ — so begründet `prozess-baum.tsx`, warum der stille Prozess sie nicht bekommt, und die Sprachdatei hält die Rolle ausdrücklich für den Tag bereit, an dem eine echte Schwelle zurückkommt. Statusfarben sind fachlich vergeben ([`visuelles-konzept.md`](visuelles-konzept.md) §3), und jede Rolle trägt genau eine Bedeutung; deshalb ein Name aus der Fachlichkeit und keine allgemeine „Achtung“-Farbe, die die nächste zweite Bedeutung einlädt. **Die Werte** sind die von Ton 80, weil sie genau das schon leisten, was E‑1105 verlangt — auffallen, ohne Rot zu sein: OKLab-Abstand zu Rot 0,152, Kontrast 5,59 : 1 hell und 6,53 : 1 dunkel auf `--card`, Kontur 3,29 : 1 (§7a dort). Getrennt lässt sich jede Rolle für sich ändern |
| **F17** | Rollen und Zeichen der übrigen Stände und der Ergebnisse nach §12.4: Offen und In Ausführung neutral (`offen`), Erledigt grün, Fehlgeschlagen rot, Rückmeldung unvollständig und ein unbekannter Stand gedämpft (`ungeklaert`). Jeder Stand hat ein eigenes Zeichen in eigener Form. | Grün und Rot nur dort, wo sie dasselbe heißen wie überall (§3 dort: „keine zweite Bedeutung“). „Rückmeldung unvollständig“ ist weder Fehler noch teilweise Ausführung: Was die Plattform getan hat, ist unbekannt — „nicht zugeordnet heißt nicht zugeordnet“ (Q4). Die Zeichen lehnen sich an die Statusplakette der Nachrichten an (Uhr für wartend, Abspielen für läuft) |
| **F18** | **Beim ersten Laden steht kein Platzhalter.** Der Block erscheint, sobald die Liste da und nicht leer ist; lädt sie neu, bleibt er mit dem bisherigen Stand stehen. So ist „mit Ladezustand“ aus dem Auftrag gelesen. | Ein Platzhalter wäre genau der leere Rahmen, den E‑1098 ausschließt — für jeden Kunden ohne Aufträge bei jedem Aufruf, und die Reihe spränge von drei auf zwei Spalten. Dieselbe Regel wie beim Symbol des Assistenten (E‑1068): lieber einen Moment später erscheinen als wieder verschwinden. Beim Neuladen bleibt alles stehen, wie in der übrigen Übersicht (E‑170) |
| **F19** | **Der Block liegt in `features/dashboard`**, sein Abfrageschlüssel in `lib/auftraege.ts`. Typen und Aufrufe von `GET` und `DELETE` stehen in `features/dashboard/api.ts`. | Pro Ansicht ein Modul ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8): Der Block ist Teil der Übersicht. Den Schlüssel braucht auch der Assistent, der nach dem Anlegen neu laden lässt; ein Feature importiert nicht aus dem Nachbarfeature, also liegt er in der Naht |
| **F20** | **Jede Zeile klappt für sich auf**; der Zustand liegt im Arbeitsspeicher des Blocks, je `taskId`, nicht in der URL. Er übersteht „Neu laden“ und das Löschen eines anderen Auftrags. | Aufgeklappte Abschnitte gehören nicht in die URL ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §8, Filterzustand). Wer nachsieht, ob sich ein Stand geändert hat, will die Zeile offen behalten |
| **F21** | **Aufgeklappt stehen nur die Felder mit Wert**, in der Reihenfolge der Definition, mit dem `label` aus `felder`. Ein leeres Feld fehlt — auch bei einer Anpassung, wo leer „wird entfernt“ heißt. | `eintraege[].werte` trägt nur Felder mit Wert, und welche Felder bei welcher Aktion vorkommen (`nurBei`), liefert die Antwort nicht. Ein „leer“ an jedem fehlenden Feld stünde bei einer Löschung an SNDPRN, wo es keinen Sinn hat. Offen als Punkt 1027 |
| **F22** | **Wo die Sätze nach dem Löschen stehen:** „Auftrag gelöscht …“ und der Satz zu `404` oben im Block, der zu `409` in der Zeile. Der Block bleibt für die Sätze oben stehen, auch wenn danach kein Auftrag mehr da ist — bis zum nächsten „Neu laden“ oder Aufruf der Seite. Der Fokus geht auf den Satz. Jeder andere Fehler beim Löschen steht in der Zeile („Der Auftrag konnte gerade nicht gelöscht werden. Bitte versuchen Sie es erneut.“), und „Auftrag löschen“ ist wieder frei. | Nach einem `404` verschwindet die Zeile mit dem Neuladen und mit ihr jeder Satz darin. Ohne stehenbleibenden Block verschwände nach dem Löschen des letzten Auftrags die Bestätigung — und mit dem gedrückten Knopf der Fokus |
| **F23** | **Ohne Schlüssel** (ein Typ ohne `schluessel`) steht „Eintrag <n>“, wie auf der Karte (E‑1080). | Kein Schlüssel wird im Frontend gebildet (E‑1071) |
| **F24** | **„Neu laden“ holt beide Abfragen** — Übersicht und Aufträge — und zeigt die Sanduhr, solange eine davon läuft. | Ein Knopf, der nur die Hälfte lädt, ließe glauben, der Stand der Aufträge sei aktuell |
| **F25** | **„Weniger anzeigen“** auch bei den Einträgen, nach „Alle <n> Einträge anzeigen“. | Dieselbe Bedienung wie bei der Liste (E‑1101) |
| **F29** | **Die Zeile bricht um, statt zu kürzen** — Stand, Aktion, Zahl der Einträge und „<x> von <n> nicht erfolgreich“. Gekürzt werden allein die Schlüssel; Zeichen und Wort des Stands bleiben beisammen. „und <n> weitere“ steht in der Schrift der Umgebung, nur die Schlüssel in Festbreitenschrift. *Nachgetragen nach M1006 (§12.13).* | Bei 1.280 px ist die Spalte 300 px breit; dort verschwanden „3 Einträge“ und sogar „1 von 3 nicht erfolgreich“ hinter der Auslassung. E‑1102 erlaubt sie nur für die Schlüssel. Der Preis ist eine dritte oder vierte Zeile bei 1.280 px |

### 12.4 Die Stände

| Einordnung | Wort | Zeichen | Rolle | Hinweis in der aufgeklappten Zeile |
|---|---|---|---|---|
| `OFFEN` | Offen | Uhr | `offen`, neutral | „Wartet auf die Plattform. Bis sie ihn abholt, können Sie ihn noch löschen.“ |
| `IN_AUSFUEHRUNG` | In Ausführung | Abspielen im Kreis | `offen`, neutral | „Die Plattform bearbeitet den Auftrag.“ |
| `ERLEDIGT` | Erledigt | Haken im Kreis | `abgeschlossen`, grün | kein Hinweis, Ergebnis je Eintrag |
| `TEILWEISE_ERLEDIGT` | Teilweise erledigt | Ausrufezeichen im Kreis | `teilweise`, Bernstein (F16) | kein Hinweis, Ergebnis je Eintrag; die nicht erfolgreichen fallen auf |
| `FEHLGESCHLAGEN` | Fehlgeschlagen | Warndreieck | `fehler`, rot | `fehlertext` als „Meldung der Plattform“ |
| `UNSTIMMIG` | Rückmeldung unvollständig | gestrichelter Kreis | `ungeklaert`, gedämpft | „Die Rückmeldung der Plattform passt nicht zum Auftrag. Bitte wenden Sie sich an die Betreuung.“ Fehlt zu einem Eintrag die Rückmeldung: „keine Rückmeldung“ |
| `UNBEKANNT` | Rohwert des Status | Fragezeichen im Kreis | `ungeklaert`, gedämpft | „Unbekannter Stand.“ |

**Die Einordnung kommt allein aus dem Backend** (`TaskStatusClassifier`, [`tasks.md`](tasks.md)
§5.5). Das Frontend leitet keinen Stand ab: „Teilweise erledigt“ und „Rückmeldung unvollständig“
stehen in `einordnung`. Gezählt wird nur für „<x> von <n> nicht erfolgreich“: x sind die Einträge,
deren `ergebnis` nicht `OK` ist, n alle Einträge; `UNBEKANNT` zählt wie im Backend als nicht Ok. Ist
kein Eintrag Ok, steht „<n> von <n> nicht erfolgreich“ unter „Teilweise erledigt“ — das Wort passt
dann nicht, entschieden ist es nicht (Punkt 1000, [`tasks.md`](tasks.md) §11).

**Das Ergebnis je Eintrag** (E‑1106):

| `ergebnis` | Anzeige | für Vorleseprogramme |
|---|---|---|
| `OK` | Haken, Rohwert („Ok“), grün | „erfolgreich“ |
| `NICHT_OK` | Ausrufezeichen im Kreis, Rohwert, Bernstein; der Eintrag hat einen getönten Grund | „nicht erfolgreich“ |
| `UNBEKANNT` | wie `NICHT_OK`; der Rohwert steht wörtlich da | „nicht erfolgreich“ |
| `null` bei `UNSTIMMIG` | „keine Rückmeldung“, gedämpft | — |
| `null` sonst | nichts — Offen, In Ausführung und Fehlgeschlagen haben keine Rückgabe | — |

### 12.5 Aufbau

```
frontend/src/
├─ lib/auftraege.ts                    Abfrageschlüssel der Liste (F19)
├─ lib/status-farbe.ts                 AUFTRAG_ZUORDNUNG: Einordnung → Rolle (F16, F17)
├─ app/globals.css                     die Rolle --teilweise, hell und dunkel (F16)
├─ features/assistent/hooks.ts         lädt nach dem Anlegen die Liste neu (E‑1109)
└─ features/dashboard/
   ├─ api.ts                           Typen, GET /api/tasks, DELETE /api/tasks/{taskId}
   ├─ auftraege.ts                     reine Regeln: Sichtbare, Schlüssel, Zählung, Löschbarkeit
   ├─ hooks.ts                         Abfrage und Mutation
   └─ components/
      ├─ auftraege-block.tsx           Block, Zeile, Löschen
      └─ dashboard-ansicht.tsx         Reihe und Zustände der Übersicht (E‑1099, E‑1121)
```

| Endpunkt | Weg | Wann |
|---|---|---|
| `GET /api/tasks` | `useQuery`, Schlüssel `["auftraege", "liste"]` | beim Aufruf der Übersicht; „Neu laden“ (F24); nach dem Anlegen in der Chatbox und nach dem Löschen (E‑1109) |
| `DELETE /api/tasks/{taskId}` | Mutation | „Löschen“ in der Rückfrage |

**Keine Anfrage trägt eine Mandanten-ID** (M1). Beim Mandantenwechsel leert die Anwendung den
ganzen Zwischenspeicher, auch die Liste der Aufträge (E‑1123).

### 12.6 Die Zeile

```
┌──────────────────────────────────────────────────┐
│ Ihre Aufträge                                    │
│ Auftrag gelöscht. Er wird nicht ausgeführt.      │  role="status", nach dem Löschen (F22)
│ ▸ ◷ Offen · Neuanlage · 2 Einträge               │  zugeklappt: Stand, Aktion, Zahl
│   72332/DDB0, 010/18VZE        19.10.2026, 14:32 │  Schlüssel, Zeitpunkt
│ ▾ ⓘ Teilweise erledigt · Anpassung · 3 Einträge  │
│   1 von 3 nicht erfolgreich    19.10.2026, 09:10 │  statt der Schlüssel (E‑1102)
│   Anpassen der Werk-/Abladestellentabelle        │  aufgeklappt (E‑1104)
│   eingestellt 19.10.2026, 09:10                  │
│   ausgeführt 19.10.2026, 09:15                   │
│   72332/DDB0                                     │  Eintrag: Schlüssel
│     Werk 72332 · Abladestelle DDB0 · SNDPRN …    │  Werte mit label
│     ✓ Ok                                         │  Ergebnis
│   ▒ 010/18VZE                                  ▒ │  getönter Grund
│   ▒   ⓘ Schlüssel nicht gefunden               ▒ │
│ 2 weitere anzeigen                               │  ab sechs Aufträgen (E‑1101)
└──────────────────────────────────────────────────┘
```

**Zugeklappt** ist die ganze Zeile ein Knopf (E‑1102, E‑1104). Oben Zeichen und Wort des Stands in
seiner Rolle, dahinter — nur wenn die Liste mehrere Typen enthält — der Name des Typs, dann Aktion
und Zahl der Einträge. Darunter links die ersten zwei Schlüssel in Festbreitenschrift, bei mehr
„und <n> weitere“, gekürzt mit Auslassung; der volle Text steht im `title`. Rechts der Zeitpunkt
des Einstellens, die Sekunden im `title` (E‑1122).

**Aufgeklappt** steht darunter, eingerückt: der Name des Typs; „eingestellt <Zeit>“ und, wenn die
Plattform zurückgemeldet hat, „ausgeführt <Zeit>“; bei „Fehlgeschlagen“ „Meldung der Plattform“ mit
dem `fehlertext`; der Hinweis des Stands (§12.4); dann die Einträge — je Eintrag der Schlüssel, die
Werte mit ihrem `label` (F21) und, wo es eines gibt, das Ergebnis. Bis fünf Einträge, darüber „Alle
<n> Einträge anzeigen“ und danach „Weniger anzeigen“ (F25). Bei „Offen“ zuletzt rechts „Auftrag
löschen“ (E‑1111).

**Reiner Text** (E‑1107): `fehlertext`, Rückgabe, Schlüssel und Werte stehen als Textknoten, der
`fehlertext` mit erhaltenen Zeilenumbrüchen (`white-space: pre-wrap`). Was wie HTML aussieht, steht
wörtlich da — dieselbe Bauform wie die Antworten des Assistenten (E‑1074).

### 12.7 Löschen

1. **„Auftrag löschen“** steht nur bei „Offen“ und nur aufgeklappt (E‑1110, E‑1111). Der Knopf ist
   eine Bequemlichkeit, keine Berechtigung: Ob gelöscht wird, entscheidet das Statement im Backend
   ([`tasks.md`](tasks.md) §6.6).
2. **Ein Klick** ersetzt den Knopf an derselben Stelle durch die Rückfrage (E‑1112). Der Fokus steht
   auf „Abbrechen“. „Abbrechen“ oder Esc stellen den Knopf wieder her, mit dem Fokus darauf.
3. **„Löschen“** schickt `DELETE /api/tasks/{taskId}`. Während des Aufrufs sind beide Knöpfe
   gesperrt (`aria-disabled`, wie E‑1093).
4. **Die Antwort:**

| Antwort | Anzeige |
|---|---|
| `204` | Die Liste lädt neu; oben im Block „Auftrag gelöscht. Er wird nicht ausgeführt.“ (`role="status"`), der Fokus darauf (F22) |
| `409` | in der Zeile „Die Plattform hat den Auftrag gerade übernommen. Löschen geht nicht mehr.“; die Liste lädt neu, die Zeile zeigt danach den neuen Stand (E‑1116) |
| `404` | oben im Block „Der Auftrag ist nicht mehr vorhanden.“; die Liste lädt neu (E‑1116, F22) |
| Netz, `5xx` | in der Zeile „Der Auftrag konnte gerade nicht gelöscht werden. Bitte versuchen Sie es erneut.“; „Auftrag löschen“ wieder frei (F22) |
| `401` | wie überall: Umleitung auf die Anmeldung, ohne Meldung |

**Kein Rückgängig** (E‑1112, Punkt 1024). Der Satz zu `404` sagt nichts über Berechtigung: Ein
fremder und ein unbekannter Auftrag sind auch in der Anzeige ununterscheidbar
([`frontend-grundlagen.md`](frontend-grundlagen.md) §6).

### 12.8 Die Reihe und die Zustände der Übersicht

**Die Reihe** (E‑1099): Am breiten Fenster (ab `xl`, 1.280 px) stehen „Zuletzt aufgefallen“, „Ihre
Aufträge“ und Verteilung nebeneinander. „Zuletzt aufgefallen“ und der Block teilen sich die drei
Fünftel, die „Zuletzt aufgefallen“ vorher allein hatte; die Verteilung behält zwei Fünftel. Darunter
stehen die drei untereinander, in derselben Reihenfolge. **Prozessnamen in „Zuletzt aufgefallen“
brechen um, statt abgeschnitten zu werden** — die Spalte ist schmaler geworden, und ein gekürzter
Name ist ein anderer Name ([`dashboard-frontend.md`](dashboard-frontend.md) §5.6).

Gebaut als `xl:grid-cols-[3fr_3fr_4fr]` mit Block und unverändert `xl:grid-cols-[3fr_2fr]` ohne ihn.
**Gemessen in M1006** (§12.13), Breiten in px:

| Fenster | mit Block: Zuletzt aufgefallen · Ihre Aufträge · Verteilung | ohne Block: Zuletzt aufgefallen · Verteilung |
|---|---|---|
| 1.920 | 492 · 492 · 656 | 994 · 662 |
| 1.440 | 348 · 348 · 464 | 706 · 470 |
| 1.280 | 300 · 300 · 400 | 610 · 406 |
| 390 | untereinander, je 351 | untereinander, je 351 |

Die Verteilung behält ihre Breite bis auf 6 px — zwei Fünftel der zweiten Lücke zwischen den Karten.

**Die Zustände:**

| Übersicht | Aufträge | Block |
|---|---|---|
| lädt | beliebig | nicht da; die Übersicht zeigt ihren Ladezustand |
| Daten | lädt zum ersten Mal | nicht da, die Reihe wie vorher (F18) |
| Daten | leer | nicht da (E‑1098) |
| Daten | mindestens einer | in der Reihe (E‑1099) |
| Daten | Fehler | in der Reihe, mit einem Satz und „Erneut versuchen“; „Zuletzt aufgefallen“ bleibt unberührt |
| leer oder Fehler | mindestens einer, oder Fehler | allein: im Leerzustand zwischen Satz und Stand, im Fehlerzustand unter der Meldung (E‑1121) |
| beliebig | lädt neu | bleibt mit dem bisherigen Stand stehen (F18, E‑170) |

**Der Fehlerzustand des Blocks** ist der Baustein `Fehler` aus `components/zustand.tsx` mit dem Satz
„Ihre Aufträge konnten gerade nicht geladen werden.“, der Fehler-Kennung und „Erneut versuchen“.

### 12.9 Barrierefreiheit

- **Aufklappen:** Der Auslöser je Zeile ist ein `button` mit `aria-expanded` und `aria-controls`;
  Enter oder Leertaste klappt auf. Der aufgeklappte Teil steht nur dann im Baum.
- **Stand:** Das Zeichen ist `aria-hidden`, das Wort steht sichtbar daneben. Das Ergebnis je Eintrag
  trägt „erfolgreich“ oder „nicht erfolgreich“ als verborgene Beschriftung (E‑1106).
- **Rückfrage:** Fokus auf „Abbrechen“, Esc bricht ab, danach Fokus auf „Auftrag löschen“. Nach dem
  Löschen steht der Fokus auf dem Satz oben (F22).
- **Meldungen** sind `role="status"` und sagen sich selbst an.
- **Der Block** ist ein Abschnitt mit der Überschrift „Ihre Aufträge“ als Namen.
- **Dichte und Thema:** nur Maße in `rem` und Rollen aus `globals.css`; hell und dunkel, alle vier
  Stufen. Am Berührungsgerät haben die Knöpfe die Mindestfläche.

### 12.10 Tests

**Gelaufen am 19.10.2026:** `pnpm check` grün — Lint, Typprüfung, Format, **1.505 Fälle in 54
Dateien**, davon **69 in den Dateien von A4** (41 rein, 27 gerendert in `tests/auftraege-block.test.tsx`,
einer in `tests/assistent-chatbox.test.tsx`) und zwölf mehr in vorhandenen Dateien
(`tests/farbkontrast.test.ts` führt die sechste Rolle, `tests/farbwerte.test.ts` und
`tests/serverbausteine.test.ts` zählen je Quelldatei). `pnpm build` grün, `next build` eingeschlossen.
Die Zahl der gerenderten Fälle — jetzt **284 in 28 Dateien** — steht im Kopf von
`frontend/vitest.config.mts`, aus dem Lauf gezählt. Die Tests des Backends stehen in
[`tasks.md`](tasks.md) §9.5.

| Datei | Was |
|---|---|
| `tests/auftraege.test.ts` | **41 Fälle, rein.** Die Einordnung kommt aus dem Backend, eine unbekannte wird `UNBEKANNT` (Q4), ebenso ein unbekanntes Ergebnis; fünf von sechs sichtbar; zwei Schlüssel und der Rest, ohne Schlüssel „Eintrag <n>“ (F23); mehrere Typen über die ganze Liste; „<x> von <n> nicht erfolgreich“ zählt `NICHT_OK` und `UNBEKANNT`, auch n von n; Werte in der Reihenfolge der Definition, ein unbekanntes Feld mit seinem Namen; **löschbar nur `OFFEN`, je Stand**; der Ausgang eines Löschens nach Problemtyp und Status, und wann neu geladen wird; der Zustand des Blocks (Aufklappen, Rückfrage, Sätze oben und in der Zeile, `obenNummer`, „Neu laden“ nimmt die Sätze und lässt Aufgeklapptes); ob der Block steht (F18, F22); die Farbrollen der Stände — und **keine Klasse nennt `ueberfaellig`** (F16); die Wörter in beiden Sprachen und der vorgegebene Wortlaut |
| `tests/auftraege-block.test.tsx` | **27 Fälle, gerendert**, gegen ein gestelltes `fetch`. **Ort:** ohne Auftrag kein Block, und die Reihe trägt genau die Klassen von vorher; mit Auftrag zwischen „Zuletzt aufgefallen“ und Verteilung; beim ersten Laden kein Platzhalter; im Leerzustand allein; im Fehler Satz und „Erneut versuchen“, „Zuletzt aufgefallen“ bleibt; kein Knopf zum Anlegen und keine Anfrage an den Assistenten. **Stände:** je Stand Wort und Zeichen in der Farbe der Rolle, aufgeklappt sein Hinweis, die Meldung der Plattform, „keine Rückmeldung“ (sieben Fälle), und sieben verschiedene Zeichen. **Zeile:** Aktion, Zahl, zwei Schlüssel mit „und 1 weiterer“, Zeitpunkt in der Anzeigezone mit Sekunden im `title` (fester Wert, keine Uhr, T1); „1 von 2 nicht erfolgreich“; der Typ oben nur bei mehreren Typen; aufgeklappt Typ, Zeiten, Werte mit `label`, Ergebnis mit „(erfolgreich)“ für Vorleseprogramme, getönter Grund nur am nicht erfolgreichen; **`fehlertext` und Rückgabe mit `<img src=x onerror=alert(1)>` als Textknoten, kein `img`**; fünf von sechs Aufträgen, fünf von sieben Einträgen. **Löschen:** nur bei Offen und nur aufgeklappt; Rückfrage mit dem Fokus auf „Abbrechen“, Esc bricht ab, Fokus zurück, kein `DELETE`; „Löschen“ schickt `DELETE`, lädt neu, der Satz oben mit Fokus, der Block bleibt ohne Auftrag für ihn stehen, „Neu laden“ nimmt ihn mit; `409` mit Satz in der Zeile, Neuladen, neuer Stand; `404` mit Satz oben und Neuladen; Störung ohne Neuladen, „Auftrag löschen“ wieder frei |
| `tests/assistent-chatbox.test.tsx` | **ein Fall mehr:** Chatbox und Übersicht im selben Zwischenspeicher — nach dem Anlegen erscheint „Ihre Aufträge“, ohne die Seite neu zu laden (E‑1109) |
| `tests/dashboard-bloecke.test.tsx`, `tests/neu-laden.test.tsx` | **angepasst, nicht aufgeweicht:** Die Übersicht fragt seit A4 auch `/api/tasks`. Für `/api/dashboard` gilt die Zählung wörtlich weiter; dazu genau eine Anfrage an `/api/tasks` beim Laden und bei „Neu laden“ (F24), keine beim Sichtwechsel und keine beim Zeitraum. Die gestellte Liste ist dort leer, damit die Reihe die von vorher bleibt |
| `tests/farbkontrast.test.ts` | die Rolle `teilweise` in der Liste der Rollen: drei Werte in beiden Blöcken, Kontraste, Farbraum, Verdrahtung in `@theme inline` |

**Verletzungsproben**, je ein Eingriff in den Code, Lauf über die fünf betroffenen Dateien (130
Fälle), Rücknahme per `git checkout` gegen den committeten Stand:

| Eingriff | rot |
|---|---|
| Block auch ohne Auftrag | 4 Fälle, gerendert und rein |
| `fehlertext` als HTML (`dangerouslySetInnerHTML`) | „wörtlich als Text“ |
| „Auftrag löschen“ in jedem Stand | 8 Fälle |
| kein Neuladen nach dem Löschen | `204`, `409`, `404` |
| kein Fokus auf „Abbrechen“ | „fragt nach …“ |
| Assistent lädt die Liste nicht neu | „erscheinen, ohne die Seite neu zu laden“ |
| Reihe ohne dritte Spalte | „zwischen Zuletzt aufgefallen und Verteilung“ |
| „Teilweise erledigt“ in der Rolle `ueberfaellig` | 2 Fälle, rein |
| kein Block im Leerzustand | „im Leerzustand … allein“ |
| „Neu laden“ nur für die Übersicht | 3 Fälle |
| Satz oben ohne Fokus | `204` und `404` |
| mehrere Typen nie oben | 2 Fälle |
| **Eichung:** ein Wort in einem Kommentar | **0** — überlebt, wie es soll |

### 12.11 Offene Punkte

Nummern ab 1023 (Linie `assistent`, per `grep` geprüft); dieselben Zeilen stehen im Register
[`offene-punkte.md`](offene-punkte.md). Punkt 1026 betrifft die Plattform und steht in
[`tasks.md`](tasks.md) §11.

| Nr. | Punkt | Art |
|---|---|---|
| **1023** | Ein fehlgeschlagener oder teilweise erledigter Auftrag bleibt sieben Tage sichtbar ([`tasks.md`](tasks.md) §6.3), auch wenn der Kunde ihn längst neu angelegt hat. Ausblenden ist bewusst nicht gebaut. | Verbesserung |
| **1024** | Löschen hat kein Rückgängig (E‑1112). | Verbesserung |
| **1025** | Der Stand „Offen“ kann bis zum nächsten „Neu laden“ veraltet sein (E‑1116); die Übersicht lädt nicht von selbst neu (E‑164). | Verbesserung |
| **1027** | Aufgeklappt fehlen leere Felder, auch bei einer Anpassung, wo leer „wird entfernt“ heißt (F21). Die Antwort könnte je Feld `nurBei` und `hinweis` aus der Kopie mitliefern, wie die Karte sie zeigt (E‑1070). | Verbesserung |
| **1028** | Sichtprüfung 1: der Block in hell und dunkel, breit und schmal, mit je einem Auftrag in jedem Stand (§12.12) | Sichtprüfung oder Messung offen |
| **1029** | Sichtprüfung 2: Sieht die Übersicht ohne Aufträge aus wie vorher? (§12.12) | Sichtprüfung oder Messung offen |
| **1030** | Sichtprüfung 3: Fällt ein fehlgeschlagener oder teilweise erledigter Auftrag auf, ohne dass die Übersicht unruhig wird? (§12.12) | Sichtprüfung oder Messung offen |
| **1031** | Sichtprüfung 4: einen offenen Auftrag aufklappen und löschen (§12.12) | Sichtprüfung oder Messung offen |
| **1032** | Sichtprüfung 5: einen Auftrag abholen lassen, während die Übersicht ihn noch als „Offen“ zeigt, dann löschen — kommt der Satz aus E‑1116? (§12.12) | Sichtprüfung oder Messung offen |

**Fortgeschrieben:** Punkt 1000 (A1, „Teilweise erledigt“ ohne einen einzigen Ok) bleibt offen; A4
zeigt dann „<n> von <n> nicht erfolgreich“ unter dem Wort „Teilweise erledigt“ (§12.4).

### 12.12 Sichtprüfungen für Lukas

Auf der **Testkopie**, lokal. Der Block selbst braucht den Assistenten nicht (E‑1098) — die Aufträge,
die er zeigt, entstehen aber nur über ihn.

**Vorbereitung:**

1. `assistent` auschecken und mit `.\scripts\dev-start.ps1 -Assistent` starten (§11, Schritt 2).
2. Anmelden, Mandant **`NEXANS`** wählen.
3. **Sechs Aufträge über den Assistenten anlegen**, jeder mit zwei oder drei Einträgen und
   erfundenen Werten — etwa „Bitte neu anlegen: Werk 47110 Abladestelle TOR3 SNDPRN 120764, Werk 47110
   Abladestelle TOR4 SNDPRN 120764“. Der Text geht an Anthropic (E‑1029). Nach jedem Anlegen
   erscheint der Block, ohne die Seite neu zu laden.
4. **`scripts/auftraege-simulation.sql`** in MySQL Workbench auf der Testkopie öffnen, oben den
   eigenen Benutzernamen und die Bestätigung eintragen und **Teil 1** ausführen. Er nimmt die sechs
   jüngsten offenen Aufträge des eigenen Kontos unter `NEXANS` und bringt fünf davon mit den
   Statements des Plattformvertrags in die Stände In Ausführung, Erledigt, Teilweise erledigt (eine
   Zeile „Schlüssel nicht gefunden“), Fehlgeschlagen und Rückmeldung unvollständig (eine Zeile zu
   wenig); der sechste bleibt offen. Danach in der Übersicht „Neu laden“.
5. Ein zweiter Mandant ohne Aufträge, für Prüfung 2: `IBIS` oder `SUTTONS`.

| Nr. | Prüfung | Vorgehen |
|---|---|---|
| **1028** | Der Block in hell und dunkel, breit und schmal | Nach Schritt 4 je Thema (Nutzermenü) bei 1.280, 1.440 und 1.920 px und bei rund 360 px. Jeder Stand mit Zeichen und Wort; jede Zeile aufklappen. Kein horizontales Scrollen, „Zuletzt aufgefallen“ ohne abgeschnittenen Prozessnamen |
| **1029** | Ohne Aufträge | Mandant ohne Aufträge wählen. Sieht die Übersicht aus wie vor A4 — Reihe aus „Zuletzt aufgefallen“ und Verteilung, kein leerer Rahmen? |
| **1030** | Fällt auf, ohne zu stören | Nach Schritt 4: Fallen „Fehlgeschlagen“ und „Teilweise erledigt“ beim ersten Blick auf? Wird die Übersicht dadurch unruhig, wirkt „Teilweise erledigt“ wie ein Fehler? |
| **1031** | Offen aufklappen und löschen | Den offenen Auftrag aufklappen, „Auftrag löschen“, die Rückfrage lesen, einmal „Abbrechen“ (auch mit Esc), dann „Löschen“. Steht „Auftrag gelöscht. Er wird nicht ausgeführt.“ oben, und ist die Zeile weg? Dasselbe nur mit der Tastatur |
| **1032** | Löschen, nachdem die Plattform abgeholt hat | Einen weiteren Auftrag anlegen und in der Übersicht aufklappen. **Ohne neu zu laden** in Workbench **Teil 2** des Skripts ausführen (holt den jüngsten offenen Auftrag ab). Dann „Auftrag löschen“ und „Löschen“: Steht „Die Plattform hat den Auftrag gerade übernommen. Löschen geht nicht mehr.“ in der Zeile, und zeigt sie danach „In Ausführung“? |

**Aufräumen**, nur die eigenen und nur auf der Testkopie: Teil 3 des Skripts löscht alle Aufträge des
eigenen Kontos unter `NEXANS`, gleich in welchem Stand — vorher mit dem `SELECT` dort ansehen.

**Das Skript ist am 19.10.2026 auf der Testkopie gelaufen**, mit sieben eigenen Testaufträgen
(`angelegt_von = 'it-sim-probe'`, eingefügt statt über den Assistenten angelegt) und einer Kopie, in
der die beiden Variablen gesetzt waren: Der jüngste blieb offen, die übrigen fünf standen danach in
den fünf Ständen, die Rückgaben mit so vielen Zeilen wie Einträgen („Ok | Ok“, „Schlüssel nicht
gefunden | Ok | Ok“, bei „unvollständig“ mit einem Eintrag leer); Teil 2 holte den siebten ab,
Teil 3 hinterließ null Zeilen. Wie die Übersicht diese Stände zeigt, ist Sichtprüfung 1028.

### 12.13 Sichtprobe M1006 (19.10.2026)

**Wie — ohne Anmeldung:** Das Markup der echten `DashboardAnsicht` mit gestellten Daten (sechs
Aufträge in sechs Ständen; drei Prozesse in „Zuletzt aufgefallen“, einer mit einem Namen aus 70
Zeichen ohne Leerzeichen) über `renderToStaticMarkup`, erzeugt von einer Wegwerfdatei, die nicht
eingecheckt ist. Eingesetzt in `/anmeldung` des Produktionsbaus (`next start`, die Seite ist
öffentlich und trägt das echte Stylesheet), in einem nachgebauten Rahmen aus Navigationsspalte
(`w-navspalte`) und `main` mit dessen Klassen. Kopfloses Chrome über das DevTools-Protokoll,
`Emulation.setDeviceMetricsOverride` bei 1.920, 1.440, 1.280 und 390 px, Dichte `m`, hell und dunkel
über `data-thema`. Alle Werte erfunden (Werk 47110, Tore, „Beispiel_…“). Die Aufnahmen liegen nur
lokal.

**Befund, behoben (F29):** Bei 1.280 px kürzte die obere Zeile „In Ausführung · Anpassung · 3
Eint…“, bei „Teilweise erledigt“ auch „1 von 3 nicht erfol…“. Seither bricht beides um.

**Nach der Behebung**, in allen 16 Lagen: **kein waagrechtes Scrollen** (`scrollWidth` gleich der
Fensterbreite); **kein Prozessname abgeschnitten** — der Name aus 70 Zeichen braucht bei 1.280 px
sechs Zeilen, bei 1.440 px vier, ohne Block zwei; im Block **gekürzt allein die Schlüssel**, ab
1.440 px abwärts. Eine Zeile des Blocks ist 50 px hoch, bei 1.280 px bis 90 px (Teilweise erledigt,
vier Zeilen). Die Breiten der Reihe stehen in §12.8.

**Gesehen**, ohne Befund: „Teilweise erledigt“ fällt in beiden Themen auf und ist vom Rot
„Fehlgeschlagen“ in Ton und Zeichen getrennt. Dunkel wirkt das Bernstein heller als das Rot — die
Lage aus Punkt 122, die für `--teilweise` mit denselben Werten ebenso gilt; ob das stört, ist
Sichtprüfung 1030.

**Nicht geprüft:** aufgeklappte Zeilen, Rückfrage und Löschen im Browser (das statische Markup ist
nicht bedienbar; im Baum belegt in §12.10), echte Daten, Tastatur und Vorleseprogramm von Hand, die
Dichten `xs` und `l`. Dafür stehen die Sichtprüfungen 1028 bis 1032 (§12.12).

---

## 13. Verfügbarkeit je Mandant (1.1.0)

*Zweig `assistent-merge`, 21.10.2026.* Mit 1.1.0 ist der Assistent nur für Mandanten verfügbar, für
die ein Auftragstyp freigeschaltet ist, zunächst nur `SYSTEM` ([`tasks.md`](tasks.md) §3.9, E‑1125,
E‑1126). **Das Frontend entscheidet darüber nicht**: Es zeigt das Symbol nur, wenn `GET /api/assistent`
`verfuegbar: true` meldet (E‑1068); verbindlich prüft das Backend, das ohne Freischaltung bei
Gesprächen, Prüfen und Anlegen `404` antwortet. „Ihre Aufträge“ (§12) bleibt unverändert: Ohne
Aufträge erscheint der Block nicht (E‑1121), und ein Mandant ohne Freischaltung hat keine.

**Der Stand gilt je Mandant** (F36): Der Abfrageschlüssel ist `["assistent", "stand", <Mandant>]`.
Nach einem Wechsel fragt das Symbol für den neuen Mandanten neu — unabhängig davon, dass der Wechsel
den Zwischenspeicher ohnehin leert (`lib/zwischenspeicher.ts`). Die Anfrage selbst trägt keinen
Mandanten (M1). **Eine offene Chatbox schließt sich beim Wechsel**: Die Kopfzeile hängt den Eingang
über den Mandanten als `key` neu ein (E‑1076), und ohne Verfügbarkeit des neuen Mandanten steht er
gar nicht erst.

**Seit 1.1.0 ist „nicht verfügbar“ `404` statt `503`** ([`assistent.md`](assistent.md) §6, F33). Die
Chatbox liest einen `404` als abgelaufenes Gespräch (§6) — das passt, solange das Symbol nur bei
Verfügbarkeit steht. Den Rest beschreibt Punkt 1034.

**Tests** (`tests/assistent-chatbox.test.tsx`): „Das Symbol (E‑1068)“ hält, dass es nur bei
`verfuegbar: true` steht und bei `verfuegbar: false` oder einem Fehler fehlt. Neu: Unter
„Mandantenwechsel (E‑1076)“ schließt ein Wechsel mit offener Chatbox die Chatbox, fragt den Stand für
den neuen Mandanten ein zweites Mal ab und zeigt ohne Verfügbarkeit kein Symbol — **ohne** geleerten
Zwischenspeicher. Verletzungsprobe am 21.10.2026: Mit dem mandantenfreien Schlüssel von vorher ist
genau dieser Fall rot („der Stand ist neu gefragt“), mit dem neuen grün. Der Fall „503 schon beim
Eröffnen“ arbeitet seitdem mit dem Problemtyp `assistent-voruebergehend-gestoert`, den es noch gibt.

| Nr. | Punkt | Art |
|---|---|---|
| **1034** | Wird einem Mandanten die Freischaltung entzogen, während seine Chatbox offen ist, meldet die nächste Nachricht „Gespräch abgelaufen“, und „Neues Gespräch“ scheitert genauso; das Symbol verschwindet erst nach Neuladen der Seite oder einem Mandantenwechsel. Selten — die Freischaltung pflegt die Betreuung per SQL —, aber ohne passenden Satz. | Verbesserung |
