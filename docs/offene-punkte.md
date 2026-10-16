# Offene Punkte — Register

*Angelegt am 16.10.2026 mit Version 1.0.0* (Entscheidung 4 vom 15./16.10.2026, Auftraggeber).

## Zweck und Regeln

**Dieses Register ist das Verzeichnis aller offenen Punkte des Projekts, nicht ihre Beschreibung.**
Es gibt an einer Stelle Auskunft, was offen ist, wo es beschrieben steht und welcher Art es ist.
Fixes der Release-Linie 1.x kommen aus diesem Register (`DEVELOPMENT_GUIDELINES.md` §12).

- **Die Feature-Datei bleibt die Quelle der Beschreibung.** Begründung, Zahlen und Vorgeschichte
  stehen dort; hier steht eine Zeile mit Verweis.
- **Ein Statuswechsel wird an beiden Stellen nachgeführt**: in der Fundstelle (datierter Vermerk,
  wie bisher) und hier in der Spalte *Stand*, mit Verweis auf den Vermerk.
- **Ein neuer Punkt** wird in seiner Feature-Datei angelegt und im selben Commit hier eingetragen.
- **Erledigt oder gegenstandslos** steht hier nur, wenn die Fundstelle oder eine spätere Datei es
  sagt — mit Verweis. Nichts wird nach eigenem Urteil geschlossen. Wo die Fundstellen uneins
  sind, steht *offen* mit dem Widerspruch.
- **Die Spalte *Art* ist eine Einordnung durch Claude Code und von Lukas zu bestätigen.** Werte:
  Fehler, Verbesserung, Entscheidung offen, Sichtprüfung oder Messung offen.

### Nummern

- Offene Punkte zählen **projektweit fortlaufend**; vor jeder Vergabe wird per `grep` über `docs/`
  geprüft ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12). Die projektweite Zählung
  beginnt bei **33** ([`messungen-schritt9.md`](messungen-schritt9.md), Offene Punkte); Nummern darunter
  sind **lokale Listen** einzelner Dateien.
- Die Einträge der früheren README-Tabelle „Offene Sichtprüfungen“ hatten keine Nummer und sind hier als
  **234 bis 246** vergeben (16.10.2026). **Nächste freie Nummer auf `main`: 264.**
- **Auf `assistent` beginnen die Nummern bei 1000** (Entscheidung 7).
- **Doppelvergaben sind aufgelöst** (16.10.2026): 136 und 186 bis 189 waren je zweimal vergeben. Die
  Nummer behielt der Punkt, auf den andere Dateien öfter verweisen; der andere bekam 247 bzw. 248 bis
  251, mit datiertem Vermerk an seiner Fundstelle.
- **Lokale Punkte ohne projektweite Nummer** (Teil 3) haben die Fundstelle als Kennung und den Stand
  **ungeprüft**: Niemand hat geprüft, ob sie noch offen sind. Ein lokaler Punkt bekommt eine
  projektweite Nummer erst, wenn jemand ihn wieder aufgreift — auf `main` die nächste freie, auf
  `assistent` ab 1000 — und wird dann in Teil 1 geführt (Entscheidung vom 16.10.2026).

### Zählung

| | Teil 1: projektweit | Teil 2: Sichtprüfungen | Teil 3: lokal |
|---|---:|---:|---:|
| offen — Fehler | 34 | 0 | — |
| offen — Verbesserung | 35 | 0 | — |
| offen — Entscheidung offen | 47 | 0 | — |
| offen — Sichtprüfung oder Messung offen | 49 | 13 | — |
| **offen gesamt** | **165** | **13** | — |
| ungeprüft | — | — | 357 |
| erledigt | 50 | 0 | 64 |
| gegenstandslos | 3 | 0 | 5 |
| **Zeilen** | 218 | 13 | 426 |

Offen sind damit **178** nummerierte Punkte; dazu kommen 357 ungeprüfte lokale.

### Wie das Register erhoben ist

Am 16.10.2026 über alle Dateien in `docs/`: Suche nach den Schreibweisen „offener Punkt“, „Punkt
<n>“, Abschnitten „Offene Punkte“, „Offene Entscheidungen“, „Offene Fragen“, „Neu offen“,
Tabellenzeilen mit fett gesetzter Nummer und Statuswörtern („erledigt am“, „entschieden am“,
„gegenstandslos“, Durchstreichung); jeder Treffer gelesen. Führende Fundstelle ist die Datei,
deren Abschnitt den Punkt beschreibt; wo die Nummer anderswo vergeben wurde, steht das dabei. Die
Stände widersprüchlicher Fundstellen hat Lukas am 16.10.2026 entschieden (unter anderem 70, 88, 142).

---

## Teil 1 — Projektweit nummerierte Punkte

| Nr. | Kurztext | Fundstelle | Art | Stand |
|---|---|---|---|---|
| 33 | Indexliste von Message in PROJEKTBESCHREIBUNG.md §3.2 und datenmodell.md unvollständig | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Fehler | **erledigt** — `messungen-schritt9.md` Offene Punkte: „Erledigt am 20.08.2026 (Vermerk nachgetragen am 27.08.2026).…“ |
| 34 | Kein Schritt erzwingt die Übernahme einer erhobenen Tabelle in die verbindliche Datei | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Verbesserung | offen |
| 35 | PROJEKTBESCHREIBUNG §4.4 nennt vier Angaben und listet drei; Abschnitt zu überarbeiten | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Fehler | offen |
| 36 | §4.4 kennt zwei von mindestens vier Namenskonventionen (IBIS/IBISGUS, 110_VTG_SalesInvoice) | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Fehler | offen |
| 37 | Satz zur Richtung im SOSName für SUTTONS falsch, zu teilen | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Fehler | offen |
| 38 | SOSName als Partnerquelle bei IBIS, IBISGUS und ZAST für die Heuristik nutzen | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Entscheidung offen | offen |
| 39 | Ein Prozess hält 44,08 % aller Nachrichten: trägt eine Verteilung nach Partner | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt9.md` Offene Punkte: „Beantwortet am 26.08.2026 durch M91 ... Die Entscheidung für 10b, gefallen am 26.08.2026: Top-10 plus Rest.…“ |
| 40 | mandantentrennung.md §2 nennt weiterhin 142 statt 140 Projekte | [`messungen-schritt9.md`](messungen-schritt9.md) Offene Punkte | Fehler | offen |
| 41 | Offener Punkt 33 trägt keinen Erledigt-Vermerk | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Fehler | **erledigt** — `messungen-schritt10.md` Offene Punkte: „Erledigt am 27.08.2026. Offener Punkt 33 trägt seinen Erledigt-Vermerk, datiert auf den 20.08.2026…“ |
| 42 | PROJEKTBESCHREIBUNG §3.2: MessageLastUpdate nicht wörtlich Zeitpunkt der letzten Änderung | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Fehler | offen |
| 43 | Überfällig-Kachel gegen die Testkopie: prüfbar, Größe am geltenden Anker unbekannt | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Sichtprüfung oder Messung offen | offen — teilweise beantwortet in `messungen-schritt10b.md` (Runde 10b), nicht geschlossen |
| 44 | Sieben von zehn Mandanten ohne kuratierte Katalogzeilen; Pflege vor 10b | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Verbesserung | offen — Rest weitergeführt als Punkt 58 (`messungen-schritt10b.md`) |
| 45 | Elf NEXANS-Katalogzeilen ohne Richtung pflegen (Abdeckung auf 100 %) | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Verbesserung | offen |
| 46 | Alias-Fallstrick bei GROUP BY als Bauvorgabe für 10b | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Verbesserung | offen |
| 47 | Drei Mandanten bekommen nach E-f ein fast leeres Dashboard | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Entscheidung offen | offen |
| 48 | Project ist aus der Mandantenkette entbehrlich (notiert, nicht empfohlen) | [`messungen-schritt10.md`](messungen-schritt10.md) Offene Punkte | Verbesserung | offen |
| 49 | Nachlauffenster 15 min gegen Bestand ohne wahrscheinlichsten Verursacher gemessen; Produktion prüfen | [`rollup.md`](rollup.md) §13 | Sichtprüfung oder Messung offen | offen |
| 50 | Uhrzeit des Nachtlaufs (03:00) und Drosselung (1 s je Scheibe) ungemessen | [`rollup.md`](rollup.md) §13 | Sichtprüfung oder Messung offen | offen |
| 51 | PROJEKTBESCHREIBUNG §5 beschreibt message_rollup noch mit Mandant, Partner, Richtung (Widerspruch zu E-a) | [`rollup.md`](rollup.md) §13 | Fehler | **erledigt** — `PROJEKTBESCHREIBUNG.md` §6: „die Ausführung ist diese Korrekturrunde“ |
| 52 | Monatsscheibe nur warm gemessen; kalt übertragen rund 24,9 s über der 10-s-Grenze | [`rollup.md`](rollup.md) §13 | Sichtprüfung oder Messung offen | offen |
| 53 | Speicherbedarf des Volllaufs (rund 70 MiB) gerechnet, nicht gemessen | [`rollup.md`](rollup.md) §13 | Sichtprüfung oder Messung offen | offen |
| 54 | Volllauf kann Rollup-Zeilen unterhalb von MIN(MessageLastUpdate) nie löschen | [`rollup.md`](rollup.md) §13 | Entscheidung offen | **erledigt** — `rollup.md` §13: „✔ Entschieden am 27.08.2026: nicht löschen — erkennen ... ✔ Gebaut am 31.08.2026 (Schritt 10b‑2 Teil C) — der…“ |
| 55 | E-b trägt bei zwölf Monaten nicht; materialisierte Tages- oder Monatsebene vorlegen | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Entscheidung offen | **erledigt** — `messungen-schritt10b.md` Offene Punkte: „Erledigt am 31.08.2026 — beide Ebenen sind gebaut und gemessen. Der Auftraggeber hat am 27.08.2026 entschieden…“ |
| 56 | ueberfaellig ist zweite Abfrageform, kein Filter; in nachrichtenliste.md benennen | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Verbesserung | **erledigt** — `nachrichtenliste.md` §9, 27.08.2026 |
| 57 | Listenabfrage kippt bei kleinen Mandanten über 30 Tage auf 167-fach teureren Plan | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Fehler | **erledigt** — `nachrichtenliste.md` §9, 30.08.2026 (§5d) |
| 58 | SUTTONS ohne gepflegte Katalogzeile; Pflege vor Abnahme von 10b | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Verbesserung | offen — `dashboard.md` §11: „berührt, nicht erledigt“ |
| 59 | VOTG mit 378 Partnern und keiner Richtung; Umschalter Partner/Richtung einseitig | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Verbesserung | offen |
| 60 | Elf NEXANS-Katalogzeilen ohne Richtung stehen unverändert | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Verbesserung | offen |
| 61 | Schwelle für das Standardfenster braucht zwei Bedingungen; Festlegung in 10b | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Entscheidung offen | **erledigt** — `dashboard.md` §11 |
| 62 | Verteilungsblock muss fehlende Restzeilen und dominante Zeile Übrige vertragen | [`messungen-schritt10b.md`](messungen-schritt10b.md) Offene Punkte | Entscheidung offen | **erledigt** — `dashboard.md` §11 |
| 63 | EDITIONLINGERI (2,3 s) und ZAST (289 ms) zahlen über 30 Tage für ihre Dünne | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 64 | Prozessfilter über 30 Tage bei NEXANS 7,46 s = 74,6 % der Pool-Zeitgrenze, liefert sonst 500 | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 65 | Planwahl ruht auf gleicher CARDINALITY-Schätzung für jeden Mandanten (Testkopie) | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Entscheidung offen | offen |
| 66 | Zweite Abfrageform ueberfaellig nur billig bei wenigen überfälligen Zeilen (539 gemessen) | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Sichtprüfung oder Messung offen | offen |
| 67 | Rückwärtslauf ist ein einmaliger Weg ohne Sperre gegen gleichzeitige Läufe | [`rollup.md`](rollup.md) §13 | Verbesserung | offen |
| 68 | Erkennung eingefrorener Eimer (Punkt 54) soll auch die Tagesebene prüfen | [`rollup.md`](rollup.md) §13 | Verbesserung | **erledigt** — `rollup.md` §13: „✔ Erledigt am 31.08.2026 (Schritt 10b‑2 Teil C). Die Erkennung zählt alle drei Ebenen und nennt jede einzeln i…“ |
| 69 | Rückwärtslauf nur gegen 335.610 Stundenzeilen gemessen; Wachstum gerechnet | [`rollup.md`](rollup.md) §13 | Sichtprüfung oder Messung offen | offen |
| 70 | Verengung hilft einem bereits zeitgetriebenen Mandanten (ZAST) strukturell nicht | [`messungen-liste-verengung.md`](messungen-liste-verengung.md) Offene Punkte | Entscheidung offen | **erledigt** — `nachrichtenliste.md` §9: entschieden 27.08.2026, bestätigt 30.08.2026 — „beantwortet“, kein Auftrag mehr |
| 71 | Prozessfilter mit verkehrsreichen Prozessen: nur dort verengen ungemessen auf Schaden geprüft | [`messungen-liste-verengung.md`](messungen-liste-verengung.md) Offene Punkte | Sichtprüfung oder Messung offen | offen |
| 72 | Verengung, die nicht jeden Filter mitträgt, verliert stillschweigend Zeilen | [`messungen-liste-verengung.md`](messungen-liste-verengung.md) Offene Punkte | Fehler | **erledigt** — `nachrichtenliste.md` §9, 30.08.2026 |
| 73 | Ob ein Index (process_id, stunde) auf message_rollup die dünnen Mandanten rettet | [`messungen-liste-verengung.md`](messungen-liste-verengung.md) Offene Punkte | Sichtprüfung oder Messung offen | **erledigt** — `nachrichtenliste.md` §9, 30.08.2026 (V11) |
| 74 | BAM-Suche als Rückfallpfad der Verengung nicht gemessen | [`messungen-liste-verengung.md`](messungen-liste-verengung.md) Offene Punkte | Sichtprüfung oder Messung offen | offen |
| 75 | Vorabfrage hat zwei Fassungen (EXISTS/Literalliste), keine gewinnt überall | [`messungen-liste-verengung.md`](messungen-liste-verengung.md) Offene Punkte | Entscheidung offen | offen |
| 76 | 24-Stunden-Stufe der Vorabfrage (3,4–4,4 ms) vom Index unberührt, Plan nicht erhoben | [`messungen-liste-verengung.md`](messungen-liste-verengung.md) Offene Punkte | Sichtprüfung oder Messung offen | offen |
| 77 | Sortierrichtung AELTESTE fällt auf unverengten Pfad zurück; gespiegelte Rechnung ungemessen | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Sichtprüfung oder Messung offen | offen |
| 78 | Liste über 24 h teurer als über 30 Tage (Faktor 60–97 bei vier Mandanten); WOC bleibt offen | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 79 | Kalter Fall der Monatsebene nicht gemessen (übertragen rund 0,86 s) | [`rollup.md`](rollup.md) §13 | Sichtprüfung oder Messung offen | offen |
| 80 | Monatsebene nur für zwei von zehn Mandanten gemessen | [`rollup.md`](rollup.md) §13 | Sichtprüfung oder Messung offen | offen |
| 81 | Rückwärtslauf rechnet Monate über ganze Monate, Scheiben nur ab MIN(stunde) | [`rollup.md`](rollup.md) §13 | Fehler | offen |
| 82 | Indexhinweis IGNORE INDEX FOR ORDER BY ist eine Wette auf die heutige Statistik, ohne Wächter | [`dashboard.md`](dashboard.md) §11 | Sichtprüfung oder Messung offen | offen |
| 83 | Aufwand von Zuletzt aufgefallen wächst mit auffälligen Zeilen im Gesamtbestand; gerechnet, nicht gemessen | [`dashboard.md`](dashboard.md) §11 | Sichtprüfung oder Messung offen | offen |
| 84 | Kachel Überfällig kostet doppelt so viel wie M90; Nachmessung am geltenden Anker steht aus | [`dashboard.md`](dashboard.md) §11 | Sichtprüfung oder Messung offen | offen |
| 85 | Acht von zehn Mandanten für das Dashboard ungemessen (gemessen NEXANS, SUTTONS) | [`dashboard.md`](dashboard.md) §11 | Sichtprüfung oder Messung offen | offen |
| 86 | Leerzustand unterscheidet nicht zwischen leerem Zeitraum und Mandant ohne Daten | [`dashboard.md`](dashboard.md) §11 | Verbesserung | offen |
| 87 | Kachel Nachrichten zählt Aktivität (MessageLastUpdate) und nicht Nachrichten | [`dashboard.md`](dashboard.md) §11 | Verbesserung | offen |
| 88 | Nachrichtendetail nutzt die gebaute Farbrolle --ueberfaellig nicht | [`visuelles-konzept.md`](visuelles-konzept.md) §7a | Verbesserung | **gegenstandslos** — `visuelles-konzept.md` §7a, 03.09.2026 (E‑71); „bleibt offen“ in `dunkelmodus.md` §10 grenzt nur die Runde 11a ab (Entscheidung vom 16.10.2026) |
| 89 | Kein Weg, „nur überfällige“ in der Liste selbst einzuschalten | [`nachrichtenliste.md`](nachrichtenliste.md) §9 · auch in `dashboard-frontend.md` §9 geführt | Entscheidung offen | **gegenstandslos** — `nachrichtenliste.md` §9: „Punkt 89 unten ist am 03.09.2026 gegenstandslos geworden. … eine Abfrageform, die es nicht mehr gibt (E‑71)…“ |
| 90 | Achsendichte des Verlaufs nur am breiten, nicht am schmalsten Fenster angesehen | [`dashboard-frontend.md`](dashboard-frontend.md) §9 | Sichtprüfung oder Messung offen | **erledigt** — `dashboard-frontend.md` §9: „✔ Erledigt am 01.09.2026, noch vor der ersten Abgabe. Bei 360 px überlappten die Beschriftungen um 12 Pixel; d…“ |
| 91 | Verlauf ist nur ein Bild ohne Textfassung oder Tabelle für Vorleseprogramme | [`dashboard-frontend.md`](dashboard-frontend.md) §9 | Verbesserung | offen |
| 92 | Gedrückte Schaltfläche des Zeitraumumschalters mit Kontrast 1,07:1 kaum sichtbar (toggle-group) | [`dashboard-frontend.md`](dashboard-frontend.md) §9 | Entscheidung offen | offen |
| 93 | Kein Test verbietet feste px-Schriftgrößen in eigenen Komponenten (Analogon zu farbwerte.test.ts fehlt) | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Verbesserung | offen |
| 94 | Dichtestufe liegt nur im Cookie (je Gerät/Browser); Nachrüstweg Spalte an app_user | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Verbesserung | offen |
| 95 | Recharts-Baum und Tailwind-Umbruchpunkte skalieren nicht mit der Dichte; Rahmen nicht abgesucht | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Verbesserung | offen |
| 96 | Sortierknopf, Tabellenzeile, Switch und --dichte-feld bleiben am Berührungsgerät unter 44 px | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Entscheidung offen | offen |
| 97 | Tabellenkopfzeile der Nachrichtenliste hält nicht (position: static), Doku behauptet das Gegenteil | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Fehler | offen |
| 98 | Dichte- und Sprachumschalter im Rahmen ohne JavaScript nicht erreichbar (Rahmen im Browser gebaut) | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Fehler | offen |
| 99 | Buchstabenreihe der E-IDs mit E‑z aufgebraucht, keine festgelegte Fortsetzungsregel | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Entscheidung offen | offen |
| 100 | Nach Auswahl im Dichtemenü springt der Tastaturfokus aus der Gruppe (Neurendern durch revalidatePath) | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Fehler | offen |
| 101 | revalidatePath läuft auch bei erneuter Wahl der bereits aktiven Stufe | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Verbesserung | offen |
| 102 | Kein Test rendert den Dichteumschalter (Komponente, Provider, Aktion, Server ohne Abdeckung) | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Verbesserung | offen |
| 103 | Cookie-Eigenschaften, Leseweg und revalidatePath der Dichte gemessen, aber nicht per Test zugesichert | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Verbesserung | offen |
| 104 | Register der Nummernkreise in README.md kennt die projektweite Buchstabenreihe E‑a…E‑z nicht | [`dichte-umschalter.md`](dichte-umschalter.md) §9 | Entscheidung offen | offen |
| 105 | toUpperCase ist nur Näherung an utf8mb4_general_ci (Akzente); WEIGHT_STRING ungemessen | [`process-view.md`](process-view.md) §13 | Verbesserung | offen |
| 106 | Ein Partner steht bei NEXANS im Katalog in zwei Schreibweisen (Kuratierungsfrage) | [`process-view.md`](process-view.md) §13 | Entscheidung offen | offen |
| 107 | Baum ordnet bei IBIS kaum (139 Gruppen auf 192 Prozesse) – Bauform fraglich | [`process-view.md`](process-view.md) §13 | Entscheidung offen | **erledigt** — `process-view.md` §13: „Beantwortet am 02.09.2026, und die Antwort ist eine Zahl und keine Umgestaltung: E‑45 nimmt IBIS 19 der 139 Gr…“ |
| 108 | VOTG trägt keine kuratierte Richtung – mittlere Baumebene ohne Aussage | [`process-view.md`](process-view.md) §13 | Verbesserung | **erledigt** — `process-view.md` §13: „Geschlossen am 02.09.2026 durch E‑45: Die Ebene wird übersprungen, wo sie nur einen Knoten trüge…“ |
| 109 | Renderkosten des 150,3‑KiB-Rumpfs bei NEXANS ungemessen | [`process-view.md`](process-view.md) §13 | Sichtprüfung oder Messung offen | **erledigt** — `process-view.md` §13: „Geschlossen am 02.09.2026 … über die Leitung sind es 14,5 KiB … React braucht 98 ms…“ |
| 110 | Antwort unterscheidet bei der Richtung nicht zwischen gepflegt-leer und offen | [`process-view.md`](process-view.md) §13 | Entscheidung offen | offen |
| 111 | Feldnamen der Antwort deutsch, Richtlinie §5.2 verlangt englisch | [`process-view.md`](process-view.md) §13 | Entscheidung offen | offen |
| 112 | Überfälligkeit je Prozess nicht gemessen (Plan einer Gruppierung je Prozess) | [`process-view.md`](process-view.md) §13 | Sichtprüfung oder Messung offen | offen |
| 113 | Ebenenzuordnung Paar → Rolluptabelle doppelt in DashboardRepository und ProzessbaumRepository | [`process-view.md`](process-view.md) §13 | Verbesserung | offen |
| 114 | Übertragungsliste bei 1280 px unvollständig: Ablaufspalte 0 px, Kopf „Ablauf“ über „Projekt“ | [`process-view.md`](process-view.md) §13 | Fehler | **erledigt** — `spaltenwahl.md` §11, 16.09.2026 (E‑148); Rest als Punkt 181 |
| 115 | Keine Sichtprüfung im Browser für die Prozessansicht (10c‑2) | [`process-view.md`](process-view.md) §13 | Sichtprüfung oder Messung offen | **erledigt** — `process-view.md` §13: „Nachgeholt am 02.09.2026, §21.…“ |
| 116 | Verzeichnis features/nachrichten trägt drei Ansichten, der Name sagt das nicht | [`process-view.md`](process-view.md) §13 | Verbesserung | offen |
| 117 | Dichteumschalter bewegt im Baum fast nichts (Baumzeile hält --dichte-beruehrung) | [`process-view.md`](process-view.md) §13 | Verbesserung | **erledigt** — `process-view.md` §13: „Geschlossen am 02.09.2026 durch E‑54 (§24)…“ |
| 118 | Auswahl weit unten im Baum: Ergebnis rechts außerhalb des Bildes | [`process-view.md`](process-view.md) §13 | Fehler | **erledigt** — `process-view.md` §13, 09.09.2026 (E‑114) |
| 119 | Rückweg unter md verliert den Fokus (landet auf document.body) | [`process-view.md`](process-view.md) §13 | Fehler | offen |
| 120 | Zahl „noch nie“ aus der Kopfzeile im Baum nicht einzeln auffindbar (E‑56) | [`process-view.md`](process-view.md) §13 | Entscheidung offen | offen |
| 121 | Verdeckte Liste unter xl aktualisiert weiter, ihr Schalter ist unerreichbar | [`process-view.md`](process-view.md) §13 | Fehler | offen |
| 122 | Im Dunkelblock kippt die Rangfolge Überfällig/Fehler, Grün wird zur lautesten Kachel | [`dunkelmodus.md`](dunkelmodus.md) §10 | Entscheidung offen | offen |
| 123 | --status-fehler-flaeche liegt im hellen Block außerhalb des sRGB-Farbraums | [`dunkelmodus.md`](dunkelmodus.md) §10 | Fehler | **erledigt** — `visuelles-konzept.md` §7a und `dunkelmodus.md` §10, 04.09.2026 (E‑88) |
| 124 | --border und --input tragen im Dunkelblock Alpha und sind nicht als deckende Farbe nachgerechnet | [`dunkelmodus.md`](dunkelmodus.md) §10 | Sichtprüfung oder Messung offen | offen |
| 125 | Sichtprobe des Dunkelmodus deckt nicht die ganze Anwendung ab | [`dunkelmodus.md`](dunkelmodus.md) §10 | Sichtprüfung oder Messung offen | offen |
| 126 | Dialog-Überlagerung (bg-black/10) dunkelt im Dunkelzustand praktisch nicht ab | [`dunkelmodus.md`](dunkelmodus.md) §22 | Fehler | offen |
| 127 | Themaumschalter rendert per revalidatePath den ganzen Baum neu, obwohl die Farbe es nicht bräuchte | [`dunkelmodus.md`](dunkelmodus.md) §22 | Verbesserung | offen |
| 128 | Kein Test rendert den Themaumschalter (wortgleich zu Punkt 102) | [`dunkelmodus.md`](dunkelmodus.md) §22 | Verbesserung | offen |
| 129 | Cookie-Eigenschaften des Themas weder gemessen noch zugesichert (nur fehlendes HttpOnly gemessen) | [`dunkelmodus.md`](dunkelmodus.md) §22 | Sichtprüfung oder Messung offen | offen |
| 130 | Hängt hier etwas zu lange? seit E-71 unbeantwortet; Läuft und Wartend ohne Schwelle | [`dashboard.md`](dashboard.md) §11 | Entscheidung offen | offen |
| 131 | Nach Wegfall von ueberfaellig (E‑71) stand die Oberfläche halb nachgezogen; Kachel verwies ins Leere | [`nachrichtenliste.md`](nachrichtenliste.md) §5e | Fehler | **erledigt** — `nachrichtenliste.md` §5e: „sie fällt mit Schritt 10b‑5 (✔ am 03.09.2026 geschehen, siehe unten); ✔ Die Bedienung ist am 03.09.2026 gefall…“ |
| 132 | Ob fristSekunden auch bei abgeschlossenen Einordnungen null werden soll | [`nachrichtendetail.md`](nachrichtendetail.md) §3a | Entscheidung offen | offen |
| 133 | Aufzählung Auffaelligkeit mit nur einem Wert; Oberfläche durfte am Feld kategorie nicht brechen | [`dashboard.md`](dashboard.md) §11 | Verbesserung | **erledigt** — `dashboard.md` §11: „erledigt — die Aufzählung hat weiterhin einen Wert, und das Feld kategorie bleibt im Vertrag (Regel Q3). ... S…“ |
| 134 | Verteilungsblock bei SUTTONS ohne Codeänderung 27 ms teurer; Ursache nicht gemessen | [`dashboard.md`](dashboard.md) §11 | Sichtprüfung oder Messung offen | offen |
| 135 | Isolation der Kachel Läuft lokal nicht nachweisbar (RUNNING fehlt auf der Testkopie); gegen Produktion | [`dashboard.md`](dashboard.md) §11 | Sichtprüfung oder Messung offen | offen |
| 136 | Rohstatus je Zeile in Zuletzt aufgefallen ist mit der Plakette gefallen | [`dashboard-frontend.md`](dashboard-frontend.md) §9 | Entscheidung offen | **gegenstandslos** — `dashboard-frontend.md` §9 und `dashboard.md`, 04.09.2026 (E‑90) |
| 137 | Tagesgenaues Jahresfenster ohne Zerlegung nicht tragbar – Fassung E als Sonderfall in Z? | [`process-view.md`](process-view.md) §35 | Entscheidung offen | **erledigt** — `process-view.md`, beantwortet 07.09.2026 (E‑92) |
| 138 | Tagesebene ohne Sekundärindex; Wirkung eines Index (process_id, tag) nicht gemessen | [`process-view.md`](process-view.md) §35 | Sichtprüfung oder Messung offen | offen |
| 139 | Jahresgrenze am ausschließenden statt einschließenden bis: M149-Fenster 13 Stunden zu groß | [`process-view.md`](process-view.md) §43 | Entscheidung offen | offen |
| 140 | Keine Sichtprüfung im Browser für den freien Zeitfenster-Modus | [`process-view.md`](process-view.md) §43 | Sichtprüfung oder Messung offen | **erledigt** — `process-view.md` §43: „Nachgeholt am 07.09.2026, §45…“ |
| 141 | Erster Aufruf über fehlerhafte Adresse zeigt keinen Baum, nur den Hinweis an den Feldern | [`process-view.md`](process-view.md) §43 | Entscheidung offen | offen |
| 142 | Alle vier Typ‑1-Namen nur für NEXANS konfiguriert – Kuratierungslage der Testkopie oder Produktion? | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Sichtprüfung oder Messung offen | **erledigt** — `messungen-property-suche.md`: „erledigt in der Sache“ (M161), mit Vorbehalt; Auskunft aus dem Handabgleich, nicht gemessen |
| 143 | Typ‑0-Namen sind keine ableitbaren Spaltennamen; Abbildung im Code nötig, Umgang mit unbekanntem Namen offen | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen — fachliche Bestätigung steht aus (`property-suche.md` §9) |
| 144 | Konfigurationstabelle beschreibt die Daten nicht – Angebot folgt Konfiguration oder Daten? | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen |
| 145 | Service.Type bricht zwischen einem Tag und dreißig ab; Grenze nicht erhoben | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Sichtprüfung oder Messung offen | offen — `messungen-property-suche.md`: gegenstandslos nur für Fassung C, für A offen |
| 146 | Künftiger Feldname mit Werten über 50 Zeichen trägt die gemessenen Kosten nicht (Präfixindex als Vorfilter) | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Sichtprüfung oder Messung offen | offen |
| 147 | Feld-Angebot ohne Beschriftung und Ordnung; Reihenfolge und gemeinsame Liste offen | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen — in `property-suche.md` gebaut, aber nicht als erledigt geführt |
| 148 | Service.Type ist Kategorie, kein Suchschlüssel – aus dem Angebot, Trefferdeckel oder Filter? | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen — `messungen-property-suche.md`: gegenstandslos, „solange `Service.Type` nicht konfiguriert ist“ |
| 149 | Optimizer-Statistik von MessageProperty um 37,9 % veraltet; Wirkung von ANALYZE TABLE ungeprüft (Betrieb) | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Sichtprüfung oder Messung offen | offen |
| 150 | Konfigurationstabelle der Testkopie von Hand geändert; Neubefüllung macht den Abgleich still rückgängig | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Verbesserung | offen |
| 151 | Message.DestinationFilename fehlt in PROJEKTBESCHREIBUNG §3.2 und in datenmodell.md | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Verbesserung | offen |
| 152 | Message.VFN reißt den Maßstab der BAM-Suche über 30 Tage – anbieten, anders behandeln oder engeres Fenster? | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen |
| 153 | Message.DestinationFilename liegt unter der 20-%-Deckungsschwelle (wie ReceiverID) | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen |
| 154 | Standardfenster der Property-Suche entscheidet über den Zugriffspfad; Fenster je Name offen | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen — verschoben in Punkt 156 |
| 155 | SNDPRN und VFN finden nur Wurzeln – reicht der Absprung (E‑103) oder soll die Suche die Kette liefern? | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen |
| 156 | Ausnahmekasten zu L4 oder Tagesfenster – L4-Pfad trägt die Suche über 30 Tage nicht | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | **erledigt** — `property-suche.md` §1: entschieden (E‑102) |
| 157 | Zwei Codepfade oder keiner: erzwungene Form verteuert Schlüsselnamen um Faktor 54 bis 9.011; hängt an 156 | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen |
| 158 | Profil als Laufzeitmaß bei LATERAL DERIVED/DEPENDENT SUBQUERY unbrauchbar; Wanduhr in Messkonvention? | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | **erledigt** — `property-suche.md` §1: entschieden (E‑107, Messkonvention) |
| 159 | Zwischen 30 und 90 Tagen gibt der Optimizer den Zeitindex auf Message auf; Schwelle nicht erhoben | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Sichtprüfung oder Messung offen | offen |
| 160 | Faktor 611 aus dem Auftrag hat keine Quelle in den Messdateien – benennen oder streichen | [`messungen-property-suche.md`](messungen-property-suche.md) Offene Punkte | Entscheidung offen | offen |
| 161 | Blättern in der Prozessansicht: neue Seite beginnt oberhalb des Sichtfensters | [`process-view.md`](process-view.md) §46 | Entscheidung offen | offen |
| 162 | Klebende Spalte auf /prozesse hängt am oberen Anschlag 30–36 px unten über | [`process-view.md`](process-view.md) §47 | Fehler | offen |
| 163 | Ablage mit Zeitgrenze erschiene zusätzlich als Lampe | [`dienste.md`](dienste.md) §15 | Verbesserung | offen |
| 164 | Einheit und Takt von ServiceTimeout im Altsystem unbekannt, nicht gemessen | [`dienste.md`](dienste.md) §15 | Sichtprüfung oder Messung offen | offen |
| 165 | Antwortverarbeitung des jakarta-Zweigs mit gelieferter Datei ungemessen | [`dienste.md`](dienste.md) §15 | Sichtprüfung oder Messung offen | offen |
| 166 | ServiceDefaultFileStore nennt nur eine der zwei beschriebenen Ablagen | [`dienste.md`](dienste.md) §15 | Entscheidung offen | offen |
| 167 | Abruf gegen abgeschaltete Ablage endet nach rund 2,7 s, Ursache ungemessen | [`dienste.md`](dienste.md) §15 | Sichtprüfung oder Messung offen | offen |
| 168 | Ein offener Tab der Übersicht lädt nicht nach (kein Takt, kein Refetch bei Fokus) | [`dashboard-frontend.md`](dashboard-frontend.md) §9 · auch in `dienste.md` §15 geführt | Entscheidung offen | offen |
| 169 | Im Leerzustand steht die Kachel Plattform nicht (E-p müsste geändert werden) | [`dashboard-frontend.md`](dashboard-frontend.md) §9 · auch in `dienste.md` §15 geführt | Entscheidung offen | offen |
| 170 | Nirgends sichtbar, dass die Kachel Plattform für jeden Mandanten gleich ist | [`dashboard-frontend.md`](dashboard-frontend.md) §9 | Entscheidung offen | offen |
| 171 | Bei fünf Kacheln geht die Höhe der Kachelreihe nicht auf (Plattform-Kachel 44 px höher) | [`dashboard-frontend.md`](dashboard-frontend.md) §9 | Verbesserung | **erledigt** — `dashboard-frontend.md` §9: „✔ Erledigt am 16.09.2026 durch E‑160 (§5.8): höchstens drei Zeilen je Spalte, heruntergefahrene Dienste ausgeb…“ |
| 172 | Jeder Seitenaufruf protokolliert 404 /favicon.ico | [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 | Verbesserung | offen |
| 173 | Trefferliste bekommt bei 768 px eine fünfte Spalte ohne Platz | [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 | Entscheidung offen | **erledigt** — `messungen-sichtprobe-schmal.md` und `spaltenwahl.md` §11, 15.09.2026 (E‑147) |
| 174 | Benutzerverwaltung verliert bei 768 px die Mandantenspalte, scrollt unter md | [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 | Entscheidung offen | **erledigt** — `messungen-sichtprobe-schmal.md` §5: „Geschlossen am 15.09.2026 (E‑147, spaltenwahl.md §5.2 und §7.2)…“ |
| 175 | Prozess-Katalog verliert bei 768 px die Partnerspalte, Zeilen 79 bis 739 px hoch | [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 | Entscheidung offen | **erledigt** — `messungen-sichtprobe-schmal.md` §5: „Geschlossen am 15.09.2026 (E‑147, spaltenwahl.md §5.3 und §7.2)…“ |
| 176 | Projekt und Prozess im Kopf des Details ohne title gekürzt (360/390 px) | [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 | Fehler | **erledigt** — `messungen-sichtprobe-schmal.md` §5: „Geschlossen am 15.09.2026 (spaltenwahl.md §7.3, M177): Beide Feld-Aufrufe tragen den Wert als hinweis.…“ |
| 177 | Produktname der Kopfzeile bei 768 px ohne title gekürzt | [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 | Entscheidung offen | **erledigt** — `messungen-sichtprobe-schmal.md` §5: „Geschlossen am 15.09.2026 (spaltenwahl.md §7.3): gekürzt mit title…“ |
| 178 | Vier Klassen von Bedienelementen bleiben am Finger unter 44 px (Erweiterung zu 96) | [`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §5 | Entscheidung offen | offen |
| 179 | ProjectDescription (TEXT) legt Temptabelle des Gerüsts auf die Platte, Kosten ungemessen | [`process-view.md`](process-view.md) §48 | Sichtprüfung oder Messung offen | offen |
| 180 | Native Formularelemente zeichnen aufgeklappte Fläche selbst, keine Prüfung erreicht sie | [`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) §18 · Nummer vergeben in `spaltenwahl.md` §11 | Fehler | offen |
| 181 | Scrollbreite 591/647 px des Tabellenkastens durch sr-only-Spannen der Ablaufzelle | [`spaltenwahl.md`](spaltenwahl.md) §11 | Entscheidung offen | offen |
| 182 | DashboardIsolationDbIT.keine_mandanten_id hängt an der Wanduhr (alterSekunden im Rumpfvergleich) | [`dashboard.md`](dashboard.md) §11 | Fehler | offen |
| 183 | Trefferliste trägt noch die Spaltenbreiten aus M177, nicht die aus M180 | [`spaltenwahl.md`](spaltenwahl.md) §11 | Verbesserung | offen |
| 184 | Automatischer Abruf verlängert die Sitzung; Tab mit Auto auf Seite eins läuft nie ab | [`neu-laden.md`](neu-laden.md) §9 | Entscheidung offen | offen |
| 185 | Neu-laden-Knopf: Name und gleiche Beschreibung, Tooltip nur per Maus einzige sichtbare Beschriftung | [`neu-laden.md`](neu-laden.md) §9 | Verbesserung | offen |
| 186 | Wo die mandantengefilterte Live-Lesung liegt | [`live-rest.md`](live-rest.md) §15 | Entscheidung offen | **erledigt** — `live-rest.md` §15: entschieden 17.09.2026 (E‑183) |
| 187 | rollup_lauf wird an drei Stellen gelesen (Wasserstand, Rollup-Job, Dashboard-Stand) | [`live-rest.md`](live-rest.md) §15 | Entscheidung offen | **erledigt** — `live-rest.md` §15: entschieden 17.09.2026, „so lassen“ |
| 188 | Was passiert, wenn die Live-Lesung ausfällt | [`live-rest.md`](live-rest.md) §15 | Entscheidung offen | **erledigt** — `live-rest.md` §15: entschieden 17.09.2026 (E‑185) |
| 189 | Nachladen des Prozessbaums im Takt der Übertragungsliste | [`live-rest.md`](live-rest.md) §15 | Entscheidung offen | **erledigt** — `live-rest.md` §15: entschieden 17.09.2026 (E‑186) |
| 190 | Preis des dichtesten Live-Bereichs beim größten Mandanten nur an der Testkopie belegt | [`live-rest.md`](live-rest.md) §15 | Sichtprüfung oder Messung offen | offen |
| 191 | Bis Teil B zählen Baum und Dashboard die laufende Stunde verschieden | [`live-rest.md`](live-rest.md) §15 | Fehler | **erledigt** — `live-rest.md` §15: „Erledigt am 17.09.2026 mit Teil B (§9b): Beide rufen denselben Baustein…“ |
| 192 | Wasserstandsabfrage ohne Rückfall: Ausfall lässt den Prozessbaum mit 500 scheitern | [`live-rest.md`](live-rest.md) §15 | Entscheidung offen | offen |
| 193 | Dichte Stunde am Bestandsende auf der Testkopie nicht messbar; gerechnet rund 570 ms über Tor | [`live-rest.md`](live-rest.md) §15 | Sichtprüfung oder Messung offen | offen |
| 194 | Summenprobe des Dashboards belegt den Weg, nicht die Arithmetik der Live-Rest-Verrechnung | [`live-rest.md`](live-rest.md) §15 | Sichtprüfung oder Messung offen | offen |
| 195 | Zahl der Statements einer Dashboard-Seite hängt am Bestand (zwölf oder dreizehn) | [`live-rest.md`](live-rest.md) §15 | Verbesserung | offen |
| 196 | IDoc-Satzlängen und Ebenenstellen an keiner echten Datei geprüft | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Sichtprüfung oder Messung offen | offen |
| 197 | Welche Formate im Bestand vorkommen, ist nicht erhoben | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Sichtprüfung oder Messung offen | offen |
| 198 | VDA und IDoc zählen Zeichen statt Bytes; UTF-8-Dateien mit Bytes über 0x7F würden nicht erkannt | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Fehler | offen |
| 199 | Hex bei UTF_8 nicht verfügbar (E‑204); Auftraggeber soll zwischen drei Wegen entscheiden | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Entscheidung offen | offen |
| 200 | Verhalten sehr großer Textknoten im pre mit pre-wrap ist ungemessen | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Sichtprüfung oder Messung offen | offen — `dateiansicht-darstellung.md` §10: Annahme überholt, „gemessen ist weiterhin nichts“ |
| 201 | Sichtprüfung Teil D der Darstellungswahl (acht Prüfschritte im angemeldeten Browser) steht aus | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Sichtprüfung oder Messung offen | **erledigt** — `dateiansicht-darstellung.md` §10: „Erledigt am 18.09.2026 (§11, M187) — alle acht Schritte bestanden ... Offen bleibt das schmale Fenster beim Au…“ |
| 202 | Grund der Abschaltung der Formatanzeige im Altwerkzeug bleibt unbekannt | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Entscheidung offen | offen |
| 203 | Formaterkennung läuft über den ganzen Text, Rechenzeit ungemessen (XML/JSON 1 MiB) | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Sichtprüfung oder Messung offen | offen |
| 204 | IDoc-Dateien mit leerer Ebene werden nicht erkannt | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Entscheidung offen | offen |
| 205 | Download-Vermerk steht auch, wo die Darstellung nichts ändert | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Fehler | **erledigt** — `dateiansicht-darstellung.md` §10: „Erledigt am 18.09.2026 (E‑206): an „Ergebnis ≠ Original“ gebunden, wie vorgeschlagen…“ |
| 206 | Englische Beschriftung ANSI X12 (detected) passt nicht ins Auswahlfeld w-44 | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Fehler | **erledigt** — `dateiansicht-darstellung.md` §10: „Erledigt am 18.09.2026 (E‑207): w-48; der Eintrag passt in beiden Sprachen und allen vier Dichten (M187‑7)…“ |
| 207 | AuswahlFeld nennt seinen Wert nicht im Barrierefreiheitsbaum | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Fehler | offen |
| 208 | Größengrenze der Ablage (8 MiB) knapper als begründet; Dateigrößen über den Bestand prüfen | [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §10 | Sichtprüfung oder Messung offen | offen |
| 209 | Prozessbaum und Übersicht zählen Fehler verschieden (bis Teil B) | [`fehler-live.md`](fehler-live.md) §12 | Fehler | **erledigt** — `fehler-live.md` §12: „Erledigt am 18.09.2026 mit Teil B (§5b): Der Baum ruft denselben Baustein…“ |
| 210 | Abgänge aus anderen Status zählen bis zum Volllauf doppelt (benannte Grenze) | [`fehler-live.md`](fehler-live.md) §12 | Verbesserung | offen |
| 211 | Block 6 und die Fehler-live-Lesung lesen dieselbe Menge zweimal | [`fehler-live.md`](fehler-live.md) §12 | Verbesserung | offen |
| 212 | Fehler live: an der Produktion weder Mechanismus noch Kosten gemessen | [`fehler-live.md`](fehler-live.md) §12 | Sichtprüfung oder Messung offen | offen |
| 213 | SQL und Java uneinig bei Rohwerten mit Leerzeichen am Ende (PAD SPACE) | [`fehler-live.md`](fehler-live.md) §12 | Fehler | offen |
| 214 | Ausfall des Live-Rests (E‑185) hat keinen Test seines catch-Zweigs | [`fehler-live.md`](fehler-live.md) §12 | Verbesserung | offen |
| 215 | Rohausgabe mit Prozesskennungen eingecheckt (messung-live-rest/ergebnis/s1.txt, G1) | [`fehler-live.md`](fehler-live.md) §12 | Fehler | offen |
| 216 | Fensterverengung der Nachrichtenliste liest FEHLER aus dem Rollup, bricht bei Abgängen | [`fehler-live.md`](fehler-live.md) §12 | Fehler | offen |
| 217 | Beschriftung „Eingang“ deckt seit 21.09.2026 auch Eigenschaften von Schritt 0 (ungemessen) | [`nachrichtendetail.md`](nachrichtendetail.md) §13 | Sichtprüfung oder Messung offen | offen |
| 218 | Wächter gegen neue Statuswerte erreicht die Produktion nicht | [`message-status.md`](message-status.md) Offene Punkte | Sichtprüfung oder Messung offen | offen |
| 219 | Laufzeitsignal für unbekannte Status | [`message-status.md`](message-status.md) Offene Punkte | Entscheidung offen | **erledigt** — `message-status.md` Offene Punkte: „Entschieden: nicht gebaut — kein Logeintrag, kein Adminhinweis.…“ |
| 220 | Dauerhaft auf EERP_PENDING stehende Nachricht zählt in keiner Problemkategorie | [`message-status.md`](message-status.md) Offene Punkte | Entscheidung offen | offen |
| 221 | Jeder unverengte Pfad bricht bei NEXANS in der Produktion vermutlich ab (Plan belegt, Laufzeit nicht) | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 222 | Suche plus Prozessfilter ohne Hinweis: Produktionsplan ungesehen | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Sichtprüfung oder Messung offen | **erledigt** — `nachrichtenliste.md` §9: „Beantwortet 14.10.2026 (M197) … der Punkt bleibt als Beobachtung stehen, nicht als Befund.…“ |
| 223 | Suche hängt an zwei Indexnamen des Altsystems (IGNORE INDEX, Fehler 1176 → 500) | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 224 | anDerZeitgrenze bildet jeden SQLState 70100 ab, nicht genau eine Ausnahme | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 225 | Begriff, der nur Prozesse trifft, liest unter F4 über 90 Tage ganz Message (Testkopie) | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Sichtprüfung oder Messung offen | offen |
| 226 | Automatische Aktualisierung schickt abgebrochene Suche alle 60 s erneut | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 227 | „Neu laden“ von Seite zwei schickt abbrechende Suche zweimal | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 228 | Nach Abbruch bringt folgende Rückmeldung am Feld die alte Seite samt Σ zurück | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Entscheidung offen | **erledigt** — `nachrichtenliste.md` §9: „Erledigt 14.10.2026 (E‑240): Ja. Ein Abbruch verwirft Seite und Σ…“ |
| 229 | Belegsuche/Feldsuche lassen nach Abbruch die Trefferzeile der verworfenen Antwort stehen | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Fehler | offen |
| 230 | Rückweg über eine Datei hält Seitenstapel, Σ, Versatz und Schalter der Liste nicht | [`nachrichtenliste.md`](nachrichtenliste.md) §9 | Entscheidung offen | **erledigt** — `nachrichtenliste.md` §9: entschieden 14.10.2026, „so lassen“ |
| 231 | Rückweg aus Datei endet in Prozessansicht und Belegsuche auf Vollseite und Standardliste | [`nachrichtendetail.md`](nachrichtendetail.md) §13 | Fehler | **erledigt** — `nachrichtendetail.md` §13: entschieden 14.10.2026 (E‑243); Rest als Punkt 233 |
| 232 | Vier Widersprüche zwischen Doku und Code (Fokus, Rückweg samt Filtern, bam-suche §13, Cursor-Regel) | [`nachrichtendetail.md`](nachrichtendetail.md) §13 | Fehler | offen |
| 233 | Baumzustand und Seitenstapel gehen nach dem Umweg über eine Datei verloren | [`process-view.md`](process-view.md) §18 · Nummer vergeben in `nachrichtendetail.md` §10.18 | Verbesserung | offen |
| 247 | Ob die Fensterverengung aufsteigend (AELTESTE) rechnen kann, ist nicht geprüft | [`nachrichtenliste.md`](nachrichtenliste.md) §9 · bis 16.10.2026 doppelt als 136 geführt | Sichtprüfung oder Messung offen | offen |
| 248 | Schwelle 90 % für das EBCDIC-Muster gesetzt, nicht gemessen | [`rohdaten.md`](rohdaten.md) §13 · bis 16.10.2026 doppelt als 186 geführt | Sichtprüfung oder Messung offen | offen |
| 249 | Ob EBCDIC im Bestand vorkommt, nicht gemessen | [`rohdaten.md`](rohdaten.md) §13 · bis 16.10.2026 doppelt als 187 geführt | Sichtprüfung oder Messung offen | offen |
| 250 | EBCDIC-Datei mit Nullbytes bleibt BINAERDATEI | [`rohdaten.md`](rohdaten.md) §13 · bis 16.10.2026 doppelt als 188 geführt | Fehler | offen — hingenommen, nicht geschlossen |
| 251 | Einstufung über alle Bytes (bis 8 MiB) nicht gemessen | [`rohdaten.md`](rohdaten.md) §13 · bis 16.10.2026 doppelt als 189 geführt | Sichtprüfung oder Messung offen | offen |
| 252 | IndexbestandDbIT rot: Sollliste führt message_rollup, _tag, _monat und rollup_lauf nicht (seit V9–V12) | [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md), Ausnahme vom Vorrang · festgestellt 16.10.2026 | Fehler | offen |
| 253 | Block 6 „Zuletzt aufgefallen“ zählt seit E‑90 live über Message und ist nicht als Ausnahme von Regel 2 eingeordnet | [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 Regel 2 · [`dashboard.md`](dashboard.md) §7a | Entscheidung offen | offen |
| 254 | DEVELOPMENT_GUIDELINES zählt die Ausnahmen von M1 („genau zwei“) und M2 („die einzige“) veraltet; es sind je drei | [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §4.1 · [`mandantentrennung.md`](mandantentrennung.md) §3, §4 | Fehler | offen |
| 255 | Betriebsform (systemd oder Container), Serverpfade, Dienstbenutzer und Units nicht belegt | [`betrieb.md`](betrieb.md) §9 | Entscheidung offen | offen |
| 256 | Reverse Proxy: Konfiguration, Domain, Zertifikat und Routing von /api nicht im Repository | [`betrieb.md`](betrieb.md) §9 | Entscheidung offen | offen |
| 257 | Ob IP-Begrenzung und audit_log hinter dem Proxy die echte Client-IP sehen, ist ungeprüft | [`betrieb.md`](betrieb.md) §9 | Sichtprüfung oder Messung offen | offen |
| 258 | Rollup-Prozess prod,rollup: Port, abgeschaltete Ablagenprüfung und Flyway im zweiten Prozess nicht belegt | [`betrieb.md`](betrieb.md) §9 | Entscheidung offen | offen |
| 259 | Sicherung von overlord_monitor (process_catalog, bam_sollaenge) und Wiederherstellung nicht beschrieben | [`betrieb.md`](betrieb.md) §9 | Entscheidung offen | offen |
| 260 | Release-Bau und Auslieferung nach dem Tag nicht beschrieben; die CI liefert nichts aus | [`betrieb.md`](betrieb.md) §9 | Entscheidung offen | offen |
| 261 | Version steht auf 0.1.0(-SNAPSHOT) statt 1.0.0; JAR-Name weicht von der Doku ab (kein finalName) | [`betrieb.md`](betrieb.md) §9 | Fehler | offen |
| 262 | Bootstrap des ersten Kontos in der Produktion nicht belegt; OVERLORD_BOOTSTRAP_ADMIN_* fehlen in .env.example | [`betrieb.md`](betrieb.md) §9 | Verbesserung | offen |
| 263 | Ziel und Aufbewahrung der Protokollausgabe in der Produktion nicht belegt | [`betrieb.md`](betrieb.md) §9 | Entscheidung offen | offen |

---

## Teil 2 — Sichtprüfungen aus der früheren README-Tabelle (234 bis 246)

Bis 1.0.0 standen diese Posten ohne Nummer in der README-Tabelle „Offene Sichtprüfungen“
([Archiv](archiv/README_bis-1.0.0.md)). Die Beschreibung steht in der jeweils genannten Feature-Datei.
Offen sind **13** Sichtprüfungen; die meisten betreffen das schmale Fenster, eine das
Eingabegerät (klebende Spalte).

| Nr. | Kurztext | Fundstelle | Art | Stand |
|---|---|---|---|---|
| 234 | Filterleiste und Statuszelle der Nachrichtenliste unter 768 px ungesehen | beschrieben in nachrichtenliste.md, offener Punkt „Das schmale Fenster ist weiterhin ungesehen" (fortgeschrieben 11.08.2026) · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *4 — Nachrichtenliste* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen |
| 235 | Detailpanel am schmalen Fenster (Panel gegen eigene Route) ungesehen | beschrieben in nachrichtendetail.md §10.10, Punkt 15 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *5 — Nachrichtendetail* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen |
| 236 | Kettenblock im Panel am schmalen Fenster ungesehen | beschrieben in verkettung.md §8.11, Punkt 18 · offener Punkt in §11 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *6, Teil 2b — Verkettung* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen |
| 237 | Belegsuche schmal: bei 360 px gemessen, offen nur die Berührungsfläche am Finger | beschrieben in bam-suche.md §11.9 · §11.5 · offener Punkt 8 in §13 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *7, Teil 3 — Belegsuche* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — teilweise gesehen oder gemessen, siehe Fundstelle |
| 238 | BAM-Block am schmalen Fenster: gestapelte Form, Marken, keine zweite Bildlaufleiste | beschrieben in bam-werte.md §15, Punkt 6 · §11a · offener Punkt in §13 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *7, Teil 1 — Belegdaten* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen |
| 239 | Präfixsuche-Rückfall am schmalen Fenster: Angebot, Zeitraumhinweis, Präfixhinweis mit Rückweg | beschrieben in bam-suche.md §24 · offener Punkt 3 in §27 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *7, Teil 4 — Präfixsuche* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen |
| 240 | Zeitleiste mit 15 Artefakten und Dateiansicht samt Darstellungsauswahl bei 360 px | beschrieben in rohdaten-frontend.md §7 · §11, Punkte 1 und 9 · dateiansicht-darstellung.md §10, Punkt 201 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *8 — Rohdaten und Protokolle* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — teilweise gesehen oder gemessen, siehe Fundstelle |
| 241 | Katalogpflege bei 360 px: Zeilenformular, Vorschlagsliste, Dialog Vorschläge übernehmen | beschrieben in prozess-katalog-frontend.md §12, Punkte 3, 4 und 9 bis 12 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *9b — Katalogpflege* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — teilweise gesehen oder gemessen, siehe Fundstelle |
| 242 | Benutzerverwaltung: Zeilenformular bei 360 px und Zustand kein Zugriff (Anmeldung als MANDANT) | beschrieben in benutzerverwaltung-frontend.md §14 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *9a — Benutzerverwaltung* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — zwei Punkte offen laut `benutzerverwaltung-frontend.md` §14: das Zeilenformular bei 360 px (die Tabelle ist seit M176 gemessen) und der Zustand „kein Zugriff“ |
| 243 | Klebende Spalte von Hand: Mausrad, Touchpad, Bild-ab im Panel, Bildlaufleiste der Spalte | beschrieben in process-view.md §47, Teil D · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *Klebende Spalte (10.09.2026)* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — teilweise gesehen oder gemessen, siehe Fundstelle |
| 244 | Untermenüs der Auswahl neben dem Suchfeld unter 768 px und am Finger | beschrieben in property-suche.md §11.1, Korrekturblock vom 09.09.2026 · offener Punkt 22 in §15 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *Property-Suche — Untermenüs im Suchfeld (09.09.2026)* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — teilweise gesehen oder gemessen, siehe Fundstelle |
| 245 | Eigenschaften an der Zeitleiste: sieben Zustände, die Testkopie oder Fenster nicht zeigen | beschrieben in nachrichtendetail.md §10.16, „Nicht zu sehen war" · verkettung.md §8.15 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *Nachrichtendetail — Eigenschaften an der Zeitleiste (21.09.2026)* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — teilweise gesehen oder gemessen, siehe Fundstelle |
| 246 | Rückweg aus der Datei (Escape, Verweis, Zurück, Fokus) in Belegsuche und Prozessansicht | beschrieben in nachrichtendetail.md §10.18 · bis 1.0.0 in der README-Tabelle „Offene Sichtprüfungen“, Zeile *Rückweg aus der Datei in Belegsuche und Prozessansicht (14.10.2026, E‑243)* ([Archiv](archiv/README_bis-1.0.0.md)) | Sichtprüfung oder Messung offen | offen — teilweise gesehen oder gemessen, siehe Fundstelle |

---

## Teil 3 — Punkte ohne projektweite Nummer

Kennung ist die Fundstelle mit der lokalen Nummer der Datei, sofern sie eine führt. Nach Datei
geordnet, innerhalb der Datei in der Reihenfolge des Textes. **Stand *ungeprüft*:** Viele davon stammen
aus den Messrunden vor Schritt 9 und sind in der Sache vermutlich durch spätere Schritte beantwortet,
aber nirgends als erledigt vermerkt. *Erledigt* oder *gegenstandslos* steht nur, wo eine Datei es sagt.

### [`annahmen-korrekturen.md`](annahmen-korrekturen.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Soll dem Muster (Zahlen ohne Herkunft, nicht nachgezogen) eine Vorschrift folgen, etwa L8 erweitert? | Erhebung 11./12.08.2026 — Der Befund: das Mus | Entscheidung offen | ungeprüft |
| ohne Nummer | mandantentrennung.md §2 nennt weiterhin 142 statt der gezählten 140 Projekte | Erhebung 11./12.08.2026 — Was noch offen ist | Fehler | ungeprüft |
| ohne Nummer | Vollkopie-Annahme nie geprüft: Testkopie-Zahlen werden als Aussage über die Produktion geführt | Erhebung 11./12.08.2026 — Was noch offen ist | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | 95-Prozent-Regel um eine Wirksamkeitsbedingung ergänzen? | Erhebung 13.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | Benannte Ausnahme für Typ 9006 (94,21 % unter der Schwelle)? | Erhebung 13.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | Sicherungsregel für handkuratierte Daten fehlte | Erhebung 13.08.2026 — Neu offen | Verbesserung | **erledigt** — `annahmen-korrekturen.md` Erhebung 13.08.2026 — Neu offen: „Sie ist mit Teil 2a in bam-sollaengen.md §7.2 angelegt worden…“ |
| ohne Nummer | Welche Zeichen außer Doppelpunkt und Komma die Parameterbindung zerlegt (; \| + Zeilenumbruch) | Korrektur 14.08.2026 — Neu offen | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Wie oft Werte mit solchen Zeichen gesucht werden (kein Suchprotokoll) | Korrektur 14.08.2026 — Neu offen | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Angaben (a), (b), (c) zum schreibenden Prüfagenten fehlen; (a), (b) nur vom Auftraggeber | Vorfall 18.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | Ob die 39 Befunde des Prüflaufs vom 18.08.2026 neu erhoben werden | Vorfall 18.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | Ob Regel S3 einen erzwingenden Mechanismus (Hook, Build-Schritt) bekommt | Vorfall 18.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | Ob die übrigen achtzehn Testdateien ihre Fallzahlen einzeln führen | Vorfall 18.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | Eingang-Deutung des Paars am Lesedienst: messen oder als Sichtbefund führen | Korrektur 19.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | M73 nur in zwei Fenstern gemessen, Fenster C (2024-10-01) nicht gefahren | Korrektur 19.08.2026 — Neu offen | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Deckungsangabe 220.579 Nachrichten zählt Fenster A doppelt, abgeleitete Stellen nicht korrigiert | Korrektur 19.08.2026 — Neu offen | Fehler | ungeprüft |
| ohne Nummer | Ob Muster- und Familiensuche des Dateinamens noch auseinandergehen | Korrektur 19.08.2026 — Neu offen | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Deutung „zuletzt erzeugt“ (Schrittnummer = Ausführungsreihenfolge) nie belegt | Korrektur 19.08.2026 — Neu offen | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Diskrepanz aus M57: 63 genannte gegen 62 gezählte Kombinationen | Korrektur 19.08.2026 — Neu offen | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Ob Messungen unter Produktionslast anders ausfielen | Erhebung 21.08.2026 — Neu offen | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Message_ProcessFK und ProejctIDIDX deckungsgleich, einer könnte entfallen (Altsystem) | Erhebung 21.08.2026 — Neu offen | Entscheidung offen | ungeprüft |
| ohne Nummer | Ob Integrationstests älterer Branches über die mit V7 entfallene Spalte stolpern | Befund 21.08.2026 — Testkopie den älteren Bra | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Regel R6 in DEVELOPMENT_GUIDELINES §4.3 beschreibt das mit V7 entfallene Download-Flag | Befund 21.08.2026 — Offen, bewusst nicht ange | Fehler | ungeprüft |

### [`authentifizierung.md`](authentifizierung.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §12 Aufzählung Nr. 1 | Deaktiviertes oder gesperrtes Konto verliert laufende Sitzung nicht automatisch | §12 | Fehler | ungeprüft |
| §12 Aufzählung Nr. 2 | Zurücksetzen eines Passworts durch den Admin fehlt | §12 | Verbesserung | ungeprüft |
| §12 Aufzählung Nr. 3 | IP-Begrenzung gilt je Instanz, vervielfacht sich bei mehreren Instanzen | §12 | Verbesserung | ungeprüft |

### [`bam-sollaengen.md`](bam-sollaengen.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §8 Nr. 1 | Bekommt Typ 9006 eine benannte Ausnahme von der 95-%-Regel? | §8 | Entscheidung offen | ungeprüft |
| §8 Nr. 2 | Wird die Kuratierungsregel um die Wirksamkeitsbedingung ergänzt (IBIS/1, SUTTONS/2000)? | §8 | Entscheidung offen | ungeprüft |
| §8 Nr. 3 | Welche der drei Lesarten der 95-Prozent-Regel gilt? | §8 | Entscheidung offen | ungeprüft |
| §8 Nr. 4 | Kuratierung erfährt nicht von neuem Mandanten oder Typ; Drift-Test bemerkt es nicht | §8 | Verbesserung | ungeprüft |
| §8 Nr. 5 | Was kostet eine Variante, die nichts trifft? | §8 | Sichtprüfung oder Messung offen | **erledigt** — `bam-sollaengen.md` §8: „Gemessen am 13.08.2026 in M47: rund 0,1 ms.…“ |

### [`bam-suche.md`](bam-suche.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §9 Nr. 1 | Präfixsuche nicht gebaut | §9 | Entscheidung offen | ungeprüft — Fundstellen uneinig, siehe `bam-suche.md` §9 |
| §9 Nr. 2 | ODER zwischen Begriffen nicht gebaut und nicht gemessen | §9 | Verbesserung | ungeprüft |
| §9 Nr. 3 | Cursor und Nachladen in der BAM-Suche nicht gebaut; Kosten der zweiten Seite ungemessen | §9 | Verbesserung | ungeprüft |
| §9 Nr. 4 | Zwei wirkungslose Sollängen-Einträge (IBIS/1, SUTTONS/2000) – Wirksamkeitsbedingung? | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 5 | Typ 9006 knapp unter der Sollängen-Schwelle – benannte Ausnahme? | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 6 | GROUP BY hängt an ONLY_FULL_GROUP_BY; Unabhängigkeit und Kosten nicht gemessen | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| §9 Nr. 7 | anDerZeitgrenze steht zweimal im Code | §9 | Verbesserung | ungeprüft |
| §9 Nr. 8 | Schlimmster Fall der BAM-Suche unter Last nicht gemessen (Jahresfall 89 % der Zeitgrenze) | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| §9 Nr. 9 | Mehr als fünf Begriffe nicht gemessen (Geländer bei acht) | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| §9 Nr. 10 | Kuratierung der Suchfeldtypen | §9 | Entscheidung offen | **erledigt** — `bam-suche.md` §9: „Entschieden in Teil 3, und zwar gegen eine neue Kuratierung (§10). Die Auswahl kommt aus MessageBAMM…“ |
| §9 Nr. 11 | Komma im BAM-Wert macht ihn unsuchbar (Spring zerlegt den Listenparameter) | §9 | Fehler | **erledigt** — `bam-suche.md` §9: „Behoben am 14.08.2026, noch am selben Tag (§22).…“ |
| §9 Nr. 12 | Präfix-Rückfall über großes Fenster ist 400 – Fenster mitverkleinern oder ausbleiben? | §9 | Entscheidung offen | **erledigt** — `bam-suche.md` §9: „Entschieden am 14.08.2026 in der Oberfläche (§23), und zwar für das Mitverkleinern.…“ |
| §9 Nr. 13 | Gebaute Höchstform der Präfixsuche (56 LIKE-Zweige) nicht gemessen | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Nr. 1 | Kein Verweis von einer Wert-Marke im Detail in die Suche | §13 | Entscheidung offen | ungeprüft |
| §13 Nr. 2 | Keine Hervorhebung des getroffenen Werts im Belegdaten-Block | §13 | Verbesserung | ungeprüft |
| §13 Nr. 3 | Gekoppelte Typen werden weder ausgeblendet noch gewarnt; Kopplung nur teilweise gemessen | §13 | Verbesserung | ungeprüft |
| §13 Nr. 4 | Keine Präfixsuche in der Oberfläche | §13 | Verbesserung | **erledigt** — `bam-suche.md` §13: „Geschlossen am 14.08.2026 (§23 bis §27). Gebaut ist der Rückfall im Leerzustand…“ |
| §13 Nr. 5 | Kein ODER, keine Klammern, kein Abfragebaukasten | §13 | Verbesserung | ungeprüft |
| §13 Nr. 6 | Kein Cursor und kein Nachladen in der Trefferliste | §13 | Verbesserung | ungeprüft |
| §13 Nr. 7 | Marke im Code an einer Stelle, Bauform aber in zwei Dateien beschrieben | §13 | Verbesserung | ungeprüft |
| §13 Nr. 8 | Schmales Fenster der Belegsuche nicht gesehen (Feld, Marken, Trefferliste) | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (§21 Regelbezug, L5) | Zweite begründete Abweichung von L5 (keine Mindestlänge) – Vermerk in PROJEKTBESCHREIBUNG? | §21 | Entscheidung offen | ungeprüft |
| §27 Nr. 1 | Nutzen der Präfixsuche unbeziffert – kein Suchzähler gebaut | §27 | Verbesserung | ungeprüft |
| §27 Nr. 2 | Abbruchfall der Präfixsuche am laufenden System ungesehen (nur Testfall) | §27 | Sichtprüfung oder Messung offen | ungeprüft |
| §27 Nr. 3 | Präfix-Angebot, Zeitraumhinweis und Rückweg am schmalen Fenster nicht gesehen | §27 | Sichtprüfung oder Messung offen | ungeprüft |
| §27 Nr. 4 | suche-fenster-zu-gross der Nachrichtenliste wird weiterhin wiederholt | §27 | Verbesserung | ungeprüft |
| §27 Nr. 5 | Rückweg aus dem Präfixmodus lässt das verkleinerte Fenster stehen | §27 | Verbesserung | ungeprüft |
| §27 Nr. 6 | Kein Angebot in der Gegenrichtung, wenn die Präfixsuche zu viel findet | §27 | Verbesserung | ungeprüft |
| §27 Nr. 7 | Gebaute Höchstform auch von der Oberfläche aus nicht gemessen | §27 | Sichtprüfung oder Messung offen | ungeprüft |

### [`bam-werte.md`](bam-werte.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer (§11a Überschrift) | Marke gehört ins visuelle Konzept, sobald die BAM-Suche sie braucht | §11a | Verbesserung | **erledigt** — `bam-werte.md` §11a: „~~Warum die Marke nicht in visuelles-konzept.md steht~~ — ✔ sie steht seit dem 13.08.2026 dort. Erle…“ |
| §13 Punkt 1 | Bleiben die BAM-Typbeschreibungen ohne Endung (_K_SAP, _L_SAP, _FORS) eindeutig? | §13 | Sichtprüfung oder Messung offen | **erledigt** — `bam-werte.md` §13 selbst; `messungen-schritt7.md` führt die Kürzung der Endung als ausgeschlossen |
| §13 Punkt 2 | Gedeckelte BAM-Gruppen (20 Werte) lassen sich nicht nachladen | §13 | Verbesserung | ungeprüft |
| §13 Punkt 3 | Verteilung der Werte je (Nachricht, Typ) nicht erhoben – Bemessung der Deckelung | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Punkt 4 | Belegdaten-Block am schmalen Fenster (gestapelte Form unter 768 px) nicht gesehen | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Punkt 5 | Unkonfigurierte BAM-Typen nur bei WOC beobachtet; großer Mandant in Produktion unbekannt | §13 | Sichtprüfung oder Messung offen | ungeprüft |

### [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §9 Nr. 1 | Kein Frontend zu 9a, ohne Oberfläche keine Sichtprüfung | §9 | Verbesserung | **erledigt** — `benutzerverwaltung-backend.md` §9: „Geschlossen am 24.08.2026 — benutzerverwaltung-frontend.md. Die Sichtprüfung selbst steht weiterhin…“ |
| §9 Nr. 2 | Typzeile downloadAllowed steht noch im Frontend | §9 | Fehler | **erledigt** — `benutzerverwaltung-backend.md` §9: „Geschlossen am 24.08.2026 im Frontend-Auftrag zu 9a…“ |
| §9 Nr. 3 | Index-Vorschlag aus M82 für audit_log nicht angelegt und nicht gemessen | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 4 | app_user.last_login_at bleibt gepflegte tote Spalte | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 5 | Automatische Zeitsperre in der Liste nicht sichtbar | §9 | Fehler | **erledigt** — `benutzerverwaltung-backend.md` §9: „Geschlossen am 24.08.2026. lockedUntil steht in der Zeile, sobald es in der Zukunft liegt…“ |
| §9 Nr. 6 | Kommentar in V7__benutzerverwaltung.sql trägt widerlegte Behauptung | §9 | Entscheidung offen | ungeprüft |

### [`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §14 Offen Nr. 1 | Benutzerverwaltung am schmalen Fenster nicht gesehen | §14 | Sichtprüfung oder Messung offen | ungeprüft |
| §14 Offen Nr. 2 | Zustand „kein Zugriff" braucht Anmeldung als MANDANT, von Hand | §14 | Sichtprüfung oder Messung offen | ungeprüft |
| §16 Offen | Sichtprüfung der Anlegemaske (9c) nicht gefahren | §16 | Sichtprüfung oder Messung offen | ungeprüft |

### [`benutzerverwaltung.md`](benutzerverwaltung.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §9 Nr. 1 | E7 (Spring-Session-Verifikation) fahren, bevor an 9a Code entsteht | §9 | Sichtprüfung oder Messung offen | **erledigt** — `benutzerverwaltung-backend.md` §3: „E7 ist gefahren, bevor eine Zeile geschrieben wurde (M81). Alle vier Punkte halten am laufenden Syst…“ |
| §9 Nr. 2 | Trägt das Bootstrap-Konto keine Mandantenzuordnung? | §9 | Sichtprüfung oder Messung offen | **erledigt** — `benutzerverwaltung.md` §9: „geschlossen am 07.09.2026: ja, es trägt keine.…“ |
| §9 Nr. 3 | Messung zu E17: MAX(zeitpunkt) je Nutzer über audit_log | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| §9 Nr. 4 | Wird der MandantContext je Anfrage gegen die zulässige Menge geprüft? | §9 | Entscheidung offen | ungeprüft |

### [`dashboard-frontend.md`](dashboard-frontend.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Sichtprüfung des Fehler-live-Hinweises mit gestellter Antwort steht aus | §2 | Sichtprüfung oder Messung offen | ungeprüft |

### [`datenmodell.md`](datenmodell.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Ob Schritte ohne SOSActionName zusätzlich übersetzt werden, ist offen | §3 | Entscheidung offen | ungeprüft |
| ohne Nummer | Zahl „rund 5.000 Nachrichten pro Tag“ hier nicht nachgezogen | §8 | Verbesserung | **erledigt** — `datenmodell.md` §8: „Damit nennen alle drei Dateien dieselbe Zahl; die Korrektur ist abgeschlossen und nicht mehr offen.…“ |

### [`fehler-live.md`](fehler-live.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer (Sichtprüfung Hinweis AUSGESETZT) | Sichtprüfung des Hinweises bei AUSGESETZT mit gestellter Antwort (Übersicht und Baum) | §6 | Sichtprüfung oder Messung offen | ungeprüft |

### [`frontend-grundlagen.md`](frontend-grundlagen.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer (§2 Route /verwaltung) | Route /verwaltung gegen /administration: Umbenennung gemeldet, nicht aufgelöst | §2 | Entscheidung offen | **erledigt** — `frontend-grundlagen.md` §2: „Zur Route — entschieden am 24.08.2026 ... Beschriftung und Route heißen beide Administration…“ |
| §10 Nr. 1 | Änderungszwang-Pfad in der Oberfläche nicht end-to-end durchgeklickt | §10 | Sichtprüfung oder Messung offen | ungeprüft |
| §10 Nr. 2 | components/ui/sheet.tsx enthält feste Zeichenkette „Close" aus dem Generator | §10 | Verbesserung | ungeprüft |
| §10 Nr. 3 | Kein Dunkelmodus, keine Barrierefreiheit über die Grundlagen hinaus | §10 | Verbesserung | **gegenstandslos** — `frontend-grundlagen.md` §10: „Die erste Hälfte ist am 03.09.2026 entfallen (Schritt 11b). Es gibt einen Dunkelmodus…“ |
| §10 Nr. 4 | Sprachwahl liegt nur im Cookie, je Gerät und Browser | §10 | Verbesserung | ungeprüft |

### [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Gilt 10c als erledigt? Teilungstabelle sagt weiter „offen“ und ist nicht nachgezogen | Kopf: Nachtrag 10.09.2026 | Entscheidung offen | ungeprüft |
| ohne Nummer | Hält eine ausstehende Sichtprüfung einen Schritt offen? Maßstab für Schritte 4 bis 7 uneinheitlich | Kopf: Korrektur 19.08.2026 | Entscheidung offen | ungeprüft |
| ohne Nummer (Sichtprüfung 9c) | Sichtprüfung 9c: Konto anlegen, Anmeldung und Passwortwechsel von Hand | Schritt 9c | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (Abnahme 10d Teil B) | 10d Teil B gilt erst als erledigt, wenn der Auftraggeber die Aufnahmen gesehen hat | Schritt 10 — Teilungstabelle 10d | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (Abnahme 10e Teil A) | 10e Teil A (Live-Rest im Prozessbaum) erst erledigt nach Ansicht der Aufnahmen durch den Auftraggeber | Schritt 10e | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (Abnahme 10e Teil B) | 10e Teil B (Live-Rest im Dashboard) erst erledigt nach Ansicht der Aufnahmen durch den Auftraggeber | Schritt 10e | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (Abnahme 10f Teil A) | 10f Teil A (Fehler live in der Übersicht) erst erledigt nach Ansicht der Aufnahmen | Schritt 10f | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (Abnahme 10f Teil B) | 10f Teil B (Fehler live im Prozessbaum) erst erledigt nach Ansicht der Aufnahmen | Schritt 10f | Sichtprüfung oder Messung offen | ungeprüft |

### [`mandantentrennung.md`](mandantentrennung.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §8 Nr. 1 | M3 in Schritt 3 noch nicht anwendbar (Filter über ProjectMandant ab Schritt 4) | §8 | Verbesserung | ungeprüft |
| §8 Nr. 2 | Entzug der Mandantenzuordnung während laufender Sitzung | §8 | Entscheidung offen | ungeprüft |

### [`message-status.md`](message-status.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Statusspalte unter der Schwelle zu schmal für Quittung ausstehend, ungemessen in vier Dichtestufen | EERP_PENDING — Die geänderten Statements | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer („Die offene Prüfung“) | Alter von RUNNING/SUSPENDED gegen die Produktion prüfen (Grundlage von E-71) | Die offene Prüfung | Sichtprüfung oder Messung offen | ungeprüft |

### [`messungen-schritt4.md`](messungen-schritt4.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| Offene Entscheidungen Nr. 1 | Bezugspunkt der Dev-Uhr: MAX(MessageLastUpdate) oder Ende des dichten Bestands | Offene Entscheidungen — Zum Zeitbezug | Entscheidung offen | **erledigt** — `messungen-schritt4.md` Offene Entscheidungen — Zum Zeitbezug: „Erledigt durch M9: Der Bezugspunkt wird umgestellt auf den jüngsten Zeitpunkt an einem Tag mit minde…“ |
| Offene Entscheidungen Nr. 2 | Fünf leere Monate: Fehler der Testkopie oder Zustand, gegen den zu entwickeln ist | Offene Entscheidungen — Zum Zeitbezug | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 3 | Leerzustand: zu enges Zeitfenster von unbeeinflussbarer Datenlücke unterscheiden | Offene Entscheidungen — Zum Zeitbezug | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 4 | Zweiter Mandant für den Isolationstest: VOTG oder SUTTONS, nach welcher Zahl | Offene Entscheidungen — Zum Isolationstest | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 5 | Isolationstest mit relativem Zeitfenster: sieht er Daten oder braucht er ein absolutes | Offene Entscheidungen — Zum Isolationstest | Entscheidung offen | **erledigt** — `messungen-schritt4.md` Offene Entscheidungen — Zum Isolationstest: „Erledigt durch M9: Mit dem neuen Anker der Dev-Uhr ... liefert das relative 24-Stunden-Fenster VOTG…“ |
| Offene Entscheidungen Nr. 6 | Einheit von MessageTimeout 1800: Minuten oder Sekunden | Offene Entscheidungen — Zu MessageTimeout | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt4.md` Offene Entscheidungen — Zu MessageTimeout: „Erledigt durch M8: Sekunden ... Offen bleibt der direkte Beleg an Message.MessageTimeout selbst (Z.…“ |
| Offene Entscheidungen Nr. 7 | Bedeutet MessageTimeout 0 kein Timeout oder nicht gesetzt | Offene Entscheidungen — Zu MessageTimeout | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 8 | Behandelt die Timeout-Rechnung NULL, obwohl es im Bestand nie vorkommt | Offene Entscheidungen — Zu MessageTimeout | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 9 | Mandantenkette über View MessageMandantID oder handgeschriebenes EXISTS | Offene Entscheidungen — Zur Mandantenkette | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 10 | Soll der Lesebenutzer SHOW VIEW auf GlassfishDB bekommen (Regel L7 für Views) | Offene Entscheidungen — Zur Mandantenkette | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 11 | Trägt eine an 285 Zeilen gemessene Laufzeit die Wahl des Zugriffspfads | Offene Entscheidungen — Zur Mandantenkette | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Entscheidungen Nr. 12 | Bleibt EXISTS-Fassung ohne filesort, wenn Cursor-Paginierung dazukommt | Offene Entscheidungen — Zur Mandantenkette | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Entscheidungen Nr. 13 | datenzugriff.md §4 um die View als vierten möglichen Schreibweg ergänzen | Offene Entscheidungen — Zur Mandantenkette | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 14 | Reicht die Verteilung als Beleg für SPLITTED/MERGED terminal oder Stichprobe in Produktion | Offene Entscheidungen — Zu SPLITTED und MERGE | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Entscheidungen Nr. 15 | Zwischenprodukte (34,38 %) in der ungefilterten Liste ausblenden oder kennzeichnen | Offene Entscheidungen — Zu SPLITTED und MERGE | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 16 | Freitextfilter über SOSName ohne Index: Vorfilter auf SOS oder Join mit Filter | Offene Entscheidungen — Zu SOSName und BAM | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 17 | BAM-Sortierindex bei VOTG nicht eindeutig: was entscheidet bei Gleichstand | Offene Entscheidungen — Zu SOSName und BAM | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 18 | Was zeigen die BAM-Spalten bei Mandanten mit einem oder keinem BAM-Typ | Offene Entscheidungen — Zu SOSName und BAM | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 19 | Ist die Auswahl der BAM-Spalten über den Sortierindex bei NEXANS fachlich gewollt | Offene Entscheidungen — Zu SOSName und BAM | Entscheidung offen | **erledigt** — `messungen-schritt4.md` Offene Entscheidungen — Zu SOSName und BAM: „Beantwortet durch M11: Nein. Der Sortierindex stellt die schwächste der drei Gruppen nach vorn…“ |
| Offene Entscheidungen Nr. 20 | Gilt der Ausblenden-Schalter für Zwischenschritte für alle Mandanten | Offene Entscheidungen — Zu den Zwischenschrit | Entscheidung offen | **erledigt** — `messungen-schritt4.md` Offene Entscheidungen — Zu den Zwischenschrit: „Beantwortet durch M12: Nein. ... Offen bleibt, ob der Grenzfall VOTG (40 von 145.840 Zeilen) fachlic…“ |
| Offene Entscheidungen Nr. 21 | Trägt Message.SOSActionID einen zeigbaren Namen; Herkunft verwaister Verweise | Offene Entscheidungen — Zum aktuellen Schritt | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt4.md` Offene Entscheidungen — Zum aktuellen Schritt: „Beantwortet durch M13: Ja. ... Auch die Herkunft ist erledigt, am 07.08.2026 durch messungen-schritt…“ |

### [`messungen-schritt5.md`](messungen-schritt5.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| Offene Entscheidungen Nr. 1 | Schritt 0 in der Zeitleiste weglassen oder als Nachricht eingegangen zeigen | Offene Entscheidungen — Zur Zeitleiste | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 2 | Unaufgelöste Schritte: ganzen Rohwert SOSActionServiceProperties oder nur erste Marke zeigen | Offene Entscheidungen — Zur Zeitleiste | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 3 | Glossar für die erste Marke im Rückfall oder unübersetzt lassen | Offene Entscheidungen — Zur Zeitleiste | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 4 | Wartende Nachrichten: Zwischenraum als eigene Zeile oder Zustand der Zeitleiste | Offene Entscheidungen — Zur Zeitleiste | Entscheidung offen | **erledigt** — `messungen-schritt5.md` Offene Entscheidungen — Zur Zeitleiste: „Beantwortet am 10.08.2026: als Zustand, mit einer Zeile daran — und die Zahl kommt aus dem Backend.…“ |
| Offene Entscheidungen Nr. 5 | Bedeutung von MessageActionID 500 und 502 und ob sie in die Zeitleiste gehören | Offene Entscheidungen — Zur Zeitleiste | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 6 | 49 offene Aktionen bei ERROR_TIMEOUT: fachliches Muster oder Schnittkante der Testkopie | Offene Entscheidungen — Zum Hänger und zum Ti | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt5.md` Offene Entscheidungen — Zum Hänger und zum Ti: „Beantwortet durch M22: beides, für zwei verschiedene Dinge. ... Offen bleibt die Herkunft der Häufun…“ |
| Offene Entscheidungen Nr. 7 | Schritt über der Frist in der Zeitleiste kennzeichnen; gleich oder anders als Überfällig | Offene Entscheidungen — Zum Hänger und zum Ti | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 8 | SOSActionTimeout 0: kein Timeout oder keine Frist vorgesehen | Offene Entscheidungen — Zum Hänger und zum Ti | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 9 | Kriterium der kuratierten Eigenschaftsauswahl und was der Wächter-Test prüft | Offene Entscheidungen — Zu den Eigenschaften | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 10 | Message.VFN als Dateiname aus OFTP beschriften oder erst Auskunft aus dem Altsystem | Offene Entscheidungen — Zu den Eigenschaften | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 11 | Mehrfach vorkommende Eigenschaft: Detail zeigt alle, das letzte oder je Schritt | Offene Entscheidungen — Zu den Eigenschaften | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 12 | Bausteinmarken mit Mandantenkürzeln unmaskiert lassen oder maskieren | Offene Entscheidungen — Zur Darstellung diese | Entscheidung offen | **erledigt** — `messungen-schritt5.md` Offene Entscheidungen — Zur Darstellung diese: „Entschieden am 07.08.2026: Sie bleiben unmaskiert.…“ |
| Offene Entscheidungen Nr. 13 | Namenlose Schritte über die Marke im selben Ablauf auflösen statt Zuordnungstabelle | Offene Entscheidungen — Aus dem Nachtrag vom | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 14 | Gemeinsame Zahl der Nachrichten, die beide Gerüst-Bedingungen erfüllen, erheben | Offene Entscheidungen — Aus dem Nachtrag vom | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Entscheidungen Nr. 15 | Trägt das Gerüst inhaltlich: Markenvergleich geplanter gegen ausgeführte Schritte | Offene Entscheidungen — Aus dem Nachtrag vom | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Entscheidungen Nr. 16 | Bedeutet die Nummerierungslücke (98/99) etwas im Quellsystem | Offene Entscheidungen — Aus dem Nachtrag vom | Entscheidung offen | ungeprüft |
| Offene Entscheidungen Nr. 17 | Gibt es WARTET_VOR in der Produktion; Zweig gebaut, aber unbeobachtet | Offene Entscheidungen — Aus dem Nachtrag vom | Sichtprüfung oder Messung offen | ungeprüft |

### [`messungen-schritt6.md`](messungen-schritt6.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| Offene Fragen Nr. 1 | Wird die Vorgabe zwischenschritte=false gedreht, ersetzt oder abgeschafft | Offene Fragen für das Sparring zu Schritt 6 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 2 | Sichtbarkeit in der Liste am Status oder an der Stellung in der Kette festmachen | Offene Fragen für das Sparring zu Schritt 6 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 3 | Breitengrenze der Kettenliste im Detailpanel und Sprung darüber hinaus | Offene Fragen für das Sparring zu Schritt 6 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 4 | Chip-Text und merkmale-Auskunft an die Verkettung anpassen | Offene Fragen für das Sparring zu Schritt 6 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 5 | Zeigt die Detailansicht BAM-Werte und mit welcher Deckelung | Offene Fragen für das Sparring zu Schritt 6 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 6 | Kuratierte Listenspalten (E1) auf der Vorgabe-Ansicht leer: was passiert damit | Offene Fragen für das Sparring zu Schritt 6 | Entscheidung offen | ungeprüft |
| zu Offene Fragen Nr. 6 (Neu offen aus Block 2) | Kuratierung in V4__bam_spalte.sql rollenabhängig machen | Was Block 2 an diesen Fragen geändert hat | Entscheidung offen | ungeprüft |

### [`messungen-schritt7.md`](messungen-schritt7.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Verundung mit Zeitfenster ungemessen (K3b, K5c) | Was der Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt7.md` Was der Nachtrag neu offen lässt: „Gemessen am 13.08.2026 in M47: Das Fenster senkt beide…“ |
| ohne Nummer | Verundung bei Kombinationen ohne Treffer (leeres Ergebnis) ungemessen | Was der Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Verundung mit mehr als fünf Begriffen ungemessen | Was der Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Zeitliche Stabilität der Sollänge für die übrigen 40 Typen | Was der Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | M43-2 (gemischte Schreibweisen) nur über Fenster B, nicht über den Bestand | Was der Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Laufzeit einer normalisierenden Suche über beide Fassungen ungemessen | Was der Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Behandlung des führenden Leerzeichens bei 9018 nicht entschieden | Was der Nachtrag neu offen lässt | Entscheidung offen | ungeprüft |
| ohne Nummer | Ist die Testkopie wirklich eine Vollkopie der Produktion | Was der zweite Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Zeilenzahlen der übrigen 17 Basistabellen nicht gezählt | Was der zweite Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Würde ein Sekundärindex die Zählung von MessageProperty billiger machen | Was der zweite Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Bedeutung der Endungen _L_SAP, _K_SAP und _FORS nur erschlossen | Was der dritte Nachtrag neu offen lässt | Entscheidung offen | ungeprüft |
| ohne Nummer | Treffen Bestellnummer und Rechnungsnummer je auf derselben Nachricht zusammen | Was der dritte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Unterscheidet ein Nutzer Transport-Nummer von Transportnummer | Was der dritte Nachtrag neu offen lässt | Entscheidung offen | ungeprüft |
| ohne Nummer | Führen die Stammdaten der Produktion dieselben 62 BAM-Typen | Was der dritte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Welche Lesart der 95-Prozent-Regel für die Sollänge gilt | Was der vierte Nachtrag neu offen lässt | Entscheidung offen | ungeprüft |
| ohne Nummer | Kosten eines Sollängen-Eintrags ohne Wirkung (IBIS/1, SUTTONS/2000) | Was der vierte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt7.md` Was der vierte Nachtrag neu offen lässt: „Gemessen am 13.08.2026 in M47: Eine zusätzliche Variante kostet rund 0,1 ms…“ |
| ohne Nummer | Fehlt 9006 in der Sollängen-Kuratierung | Was der vierte Nachtrag neu offen lässt | Entscheidung offen | ungeprüft |
| ohne Nummer | Zeitliche Stabilität der Sollänge; Drift-Test läuft nicht in der CI | Was der vierte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Trägt die Produktion dieselbe Längenverteilung | Was der vierte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Neuer Mandant oder Typ erscheint nicht in der Sollängen-Kuratierung | Was der vierte Nachtrag neu offen lässt | Verbesserung | ungeprüft |
| Lücke Nr. 1 (sechster/siebter Nachtrag) | Wie oft die exakte BAM-Suche leer ausgeht (Nutzen der Präfixsuche unbeziffert) | Was der sechste Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| Lücke Nr. 2 (sechster/siebter Nachtrag) | Verhalten der Suche unter Last in der Produktion ungemessen | Was der sechste Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| Lücke Nr. 3 (sechster/siebter Nachtrag) | Trägt die Produktion dieselbe Werteverteilung (Präfixhäufigkeiten, kein %) | Was der sechste Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Zahl der Fassungen im Betrieb (bis sieben je Begriff, acht Begriffe) ungemessen | Was der sechste Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Kosten des schlimmsten Präfix des Bestands im gebauten Statement | Was der sechste Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt7.md`, „Was der achte Nachtrag (M50) neu offen lässt“: beantwortet |
| ohne Nummer | Abnahme der gefährlichen Präfixe zwischen sechs und zehn Zeichen ungemessen | Was der sechste Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Welche Typen die 602.794 folgenden Leerzeichen tragen | Was der sechste Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Gibt es einen Präfix, der über 30 Tage teurer ist als der Bestandssieger | Was der achte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Kosten der gebauten Höchstform (56 LIKE-Zweige) ungemessen | Was der achte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Wo der Optimierer im Präfixmodus umschaltet; Stabilität des gekippten Plans | Was der achte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Wie viele der 55.989 Werte mit Komma jemals gesucht würden | Was der achte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Stellen weitere Zeichen dieselbe Falle wie das Komma | Was der neunte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Kann MessageBAM weitere Typen mit Kommas bekommen (Momentaufnahme) | Was der neunte Nachtrag neu offen lässt | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Bekommt die Nachrichtenliste dieselbe Kommabindung | Was der neunte Nachtrag neu offen lässt | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 1 | Zeilenzahl von MessageBAM in den verbindlichen Dateien korrigieren | Offene Fragen für das Sparring zu Schritt 7 | Fehler | **erledigt** — `messungen-schritt7.md` Offene Fragen für das Sparring zu Schritt 7: „Entschieden und erledigt am 12.08.2026: ja.…“ |
| Offene Fragen Nr. 2 | Ist ein Standard-Zeitfenster von 24 Stunden an einer Belegsuche zumutbar | Offene Fragen für das Sparring zu Schritt 7 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 3 | Sucht Schritt 7 exakt oder präfixweise | Offene Fragen für das Sparring zu Schritt 7 | Entscheidung offen | **erledigt** — `messungen-schritt7.md` Offene Fragen für das Sparring zu Schritt 7: „Entschieden im Sparring vom 12.08.2026: exakt, mit einer typgebundenen und sichtbar gemachten Normal…“ |
| Offene Fragen Nr. 4 | Wird normalisiert, und wenn ja, wo | Offene Fragen für das Sparring zu Schritt 7 | Entscheidung offen | **erledigt** — `messungen-schritt7.md` Offene Fragen für das Sparring zu Schritt 7: „Entschieden im Sparring vom 12.08.2026: ja, typgebunden und sichtbar.…“ |
| Offene Fragen Nr. 5 | Folgen Suche und Detail-Block der Konfiguration in MessageBAMMandant | Offene Fragen für das Sparring zu Schritt 7 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 6 | Womit wird die Deckelung der BAM-Werte je Typgruppe bemessen | Offene Fragen für das Sparring zu Schritt 7 | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Fragen Nr. 7 | Wird die Kuratierung in V4__bam_spalte.sql rollenabhängig | Offene Fragen für das Sparring zu Schritt 7 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 8 | Braucht die BAM-Suche einen Abbruchpfad wie die Nachrichtenliste | Offene Fragen für das Sparring zu Schritt 7 | Entscheidung offen | **erledigt** — `messungen-schritt7.md` Offene Fragen für das Sparring zu Schritt 7: „Entschieden und gebaut am 13.08.2026: ja.…“ |
| Offene Fragen Nr. 9 | Bauform der Verundung: Selbstjoin oder IN plus HAVING COUNT(DISTINCT) | Offene Fragen — Neu aus dem Nachtrag vom 12.0 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 10 | Braucht die Verundung eine eigene Grenze neben dem Pflicht-Zeitfenster | Offene Fragen — Neu aus dem Nachtrag vom 12.0 | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt7.md` Offene Fragen — Neu aus dem Nachtrag vom 12.0: „Gemessen und beantwortet am 13.08.2026 in M47: nein, es braucht keine.…“ |
| Offene Fragen Nr. 11 | Herkunft und Fortschreibung der Sollänge je Typ | Offene Fragen — Neu aus dem Nachtrag vom 12.0 | Entscheidung offen | **erledigt** — `messungen-schritt7.md` Offene Fragen — Neu aus dem Nachtrag vom 12.0: „Beantwortet und gebaut am 13.08.2026. ... in einer eigenen Tabelle overlord_monitor.bam_sollaenge…“ |
| Offene Fragen Nr. 12 | Führendes Leerzeichen (9018) wie führende Null behandeln | Offene Fragen — Neu aus dem Nachtrag vom 12.0 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 13 | Anzeige, wenn der Bestand selbst mehrere Schreibweisen derselben Nummer trägt | Offene Fragen — Neu aus dem Nachtrag vom 12.0 | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 14 | Bekommt Typ 9006 eine benannte Ausnahme von der 95-Prozent-Regel | Offene Fragen — Neu aus dem vierten Nachtrag | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 15 | Kuratierungsregel um die Wirksamkeitsbedingung ergänzen | Offene Fragen — Neu aus dem vierten Nachtrag | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 16 | Wie erfährt die Sollängen-Kuratierung von neuem Mandanten oder Typ | Offene Fragen — Neu aus dem vierten Nachtrag | Verbesserung | ungeprüft |
| Offene Fragen Nr. 17 | Kommt die Präfixsuche, und wenn ja immer oder nur im Nulltreffer-Fall | Offene Fragen — Neu aus dem siebten Nachtrag | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 18 | Präfixsuche: Anforderung in der Parameterform und ESCAPE für jede Eingabe | Offene Fragen — Neu aus dem siebten Nachtrag | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 19 | Präfixsuche: Eingabe trimmen, in Parameterprüfung oder Variantenbildung | Offene Fragen — Neu aus dem siebten Nachtrag | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 20 | Mindestlänge je Typ bei typlos voreingestellter Suche | Offene Fragen — Neu aus dem siebten Nachtrag | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 21 | Buchführungslücken der Datei nachziehen (M48-Laufzeiten, Befund, falsche Ordnungszahl) | Offene Fragen — Neu aus dem siebten Nachtrag | Fehler | ungeprüft |
| Offene Fragen Nr. 22 | Parameterform der BAM-Suche um das Komma erweitern (Werte mit Komma unsuchbar) | Offene Fragen — Neu aus dem achten Nachtrag v | Fehler | **erledigt** — `messungen-schritt7.md` Offene Fragen — Neu aus dem achten Nachtrag v: „Beantwortet am 14.08.2026: ja, und zwar sofort. ... Die Bindung von begriff trennt nicht mehr am Kom…“ |
| Offene Fragen Nr. 23 | 30-Tage-Deckel im Präfixmodus: schlimmster Präfix über ein 30-Tage-Fenster ungemessen | Offene Fragen — Neu aus dem achten Nachtrag v | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Fragen Nr. 24 | Zweimal begründete Abweichung von Regel L5 in PROJEKTBESCHREIBUNG.md vermerken | Offene Fragen — Neu aus dem achten Nachtrag v | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 25 | Welche weiteren Zeichen zerlegt Client oder Zwischenstück (; \| + Zeilenumbruch) | Offene Fragen — Neu aus dem neunten Nachtrag | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Fragen Nr. 26 | Nachrichtenliste: status und prozess trennen weiterhin am Komma, angleichen | Offene Fragen — Neu aus dem neunten Nachtrag | Entscheidung offen | ungeprüft |
| Offene Fragen Nr. 27 | Bindungsprüfung ohne Datenbank auch für die übrigen Endpunkte anlegen | Offene Fragen — Neu aus dem neunten Nachtrag | Verbesserung | ungeprüft |

### [`messungen-schritt8-auftrag.md`](messungen-schritt8-auftrag.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Widerspruch PROJEKTBESCHREIBUNG §9 gegen Implementierungsplan zur aufbereiteten Anzeige | Was diese Runde nicht tut | Entscheidung offen | ungeprüft |
| V6 | V6: Schreibt die Plattform oder das einzelne Programm die Protokollmarken? | Was diese Runde nicht tut / V. Vorbedingung V | Sichtprüfung oder Messung offen | ungeprüft |
| Entscheidungsliste Nr. 1 | Download bei Protokollen: beschnittene Fassung für MANDANT oder nur ADMIN | Was dabei entschieden werden muss | Entscheidung offen | ungeprüft |
| Entscheidungsliste Nr. 2 | Fail closed: ohne vollständiges Markenpaar liefert der Protokollpfad für MANDANT nichts | Was dabei entschieden werden muss | Entscheidung offen | ungeprüft |
| Entscheidungsliste Nr. 3 | Protokollpfad ist kein Bytestrom-Proxy: eigener Endpunkt zum Lesen und Schneiden | Was dabei entschieden werden muss | Entscheidung offen | ungeprüft |
| Entscheidungsliste Nr. 4 | audit_log muss festhalten, welche Fassung (beschnitten/vollständig) ausgeliefert wurde | Was dabei entschieden werden muss | Entscheidung offen | ungeprüft |

### [`messungen-schritt8.md`](messungen-schritt8.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| Offene Punkte Nr. 1 | Warum antwortet der Filestore auf RETRIEVE wie auf ein blankes GET | Offene Punkte | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 2 | Fenster C liegt auf nicht erreichbaren Ablagen FILESTOREPROD07/08 | Offene Punkte | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 3 | Fenster für M64 trägt nur elf Fehlernachrichten; Erweiterung des Fensters | Offene Punkte | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 4 | FTPSender verliert seinen Schrittnamen in 99,2 % der Fälle, Ursache unbekannt | Offene Punkte | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 5 | Artefaktverweis OFTP2Sender.Payload.GUID nicht eindeutig: Anomalie oder zulässig | Offene Punkte | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 6 | Ist FileReader.FileProperty.Size eine Byteangabe | Offene Punkte | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt8.md` M60 — Befund 1: „Offener Punkt 6 ist damit beantwortet, und M56 (b) darf als Byteangabe gelesen werden.…“ |
| Offene Punkte Nr. 7 | Vorabgröße deckt nur 57,2 % des Monatsfensters; wie die Kappung im Datenstrom greift | Offene Punkte | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 8 | Bis zu 15 Artefakte je Nachricht: noch eine flache Liste | Offene Punkte | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 9 | Using index fehlt bei M58 (1); vorregistrierte Zeile nicht abgearbeitet | Offene Punkte | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 10 | FILESTOREPROD09 als einziges Beispiel in drei Dateien; es gibt elf Ablagen | Offene Punkte | Fehler | ungeprüft |
| Offene Punkte Nr. 11 | Ergänzende Messung E8 zum Takt der Filestore-Rotation | Offene Punkte | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 12 | Messung zur Kennungsform (M69) durch Q1 gegenstandslos; Auftrag nicht angepasst | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | **gegenstandslos** — `messungen-schritt8.md` Offene Punkte — Nachträge vom 17.08.2026, nac: „Eine Messung zur Kennungsform (M69) wäre gegenstandslos. ... Der Auftrag ist nicht angepasst worden…“ |
| Offene Punkte Nr. 13 | Transportform SAAJ gegen handgebauten Envelope: welcher Unterschied den Ausschlag gibt | Offene Punkte — Nachträge vom 17.08.2026, nac | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 14 | Dienstkontoname im Klartext in dieser Datei | Offene Punkte — Nachträge vom 17.08.2026, nac | Fehler | **erledigt** — `messungen-schritt8.md` Offene Punkte — Nachträge vom 17.08.2026, nac: „Entschieden am 17.08.2026. ... damit ist auch der Dienstkontoname in Q3 maskiert worden…“ |
| Offene Punkte Nr. 15 | Envelope-Tabelle trug falsche Herkunftsangabe für Content-Type und SOAPAction | Offene Punkte — Nachträge vom 17.08.2026, nac | Fehler | **erledigt** — `messungen-schritt8.md` Offene Punkte — Nachträge vom 17.08.2026, nac: „Korrigiert am 17.08.2026 ... Was SAAJ tatsächlich sendet ... bleibt offen (siehe Punkt 13).…“ |
| Offene Punkte Nr. 16 | Konservative Schnittregel strenger als das heutige Verhalten des Altsystems | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 17 | Jüngste Zeitscheibe von M66 leer; auf den 2026-07-08 verschieben | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 18 | Ursache der Datenlücke 2026-01 bis 2026-05 | Offene Punkte — Nachträge vom 17.08.2026, nac | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 19 | V4 (Aufbewahrungsfrist im Filestore) ohne erreichbare alte Ablage nicht messbar | Offene Punkte — Nachträge vom 17.08.2026, nac | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 20 | 63,2 % des Bestands heute nicht abrufbar; Zeitraum der Abnahme festlegen | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 21 | Am jüngeren Ende laufen DB- und Filestore-Kopie auseinander; Ende des Filestore-Bestands | Offene Punkte — Nachträge vom 17.08.2026, nac | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 22 | Protokolle ohne Marken: für MANDANT gesperrt oder ungeschnitten sichtbar | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 23 | Beschreibung Mandantentrennung für den Beschnitt irreführend, Korrekturort offen | Offene Punkte — Nachträge vom 17.08.2026, nac | Fehler | ungeprüft |
| Offene Punkte Nr. 24 | Schaden der 414 Pfadzeilen außerhalb der Marken bewerten | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 25 | 18,2 % der Nutzdateien binär; Art unbekannt, Umgang der Anzeige offen | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 26 | Anzeige nicht kodierungsfrei; benannte Kodierungsentscheidung samt Umschalter | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 27 | Beschnitt verkürzt die Anzeige kaum; Panel gegen eigene Route | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 28 | Uneinheitliche Zeilenenden; Behandlung in der Anzeige offen | Offene Punkte — Nachträge vom 17.08.2026, nac | Entscheidung offen | ungeprüft |
| Offene Punkte Nr. 29 | Antwortverarbeitung des jakarta-Zweigs (ZIP-Anhang) ungemessen | Offene Punkte — Nachtrag vom 18.08.2026, nach | Sichtprüfung oder Messung offen | ungeprüft |
| Offene Punkte Nr. 30 | Korrektur von rohdaten-frontend.md §2 und rohdaten.md §5 (Eingangszeile) nicht gefahren | Offene Punkte — Nachträge vom 19.08.2026, nac | Fehler | ungeprüft |
| Offene Punkte Nr. 31 | Downloaddateiname hängt an derselben falschen Annahme über das Artefakt auf Schritt 0 | Offene Punkte — Nachträge vom 19.08.2026, nac | Fehler | ungeprüft |
| Offene Punkte Nr. 32 | Regel aus M73 nur in zwei Fenstern gemessen, Fenster C nicht gefahren | Offene Punkte — Nachträge vom 19.08.2026, nac | Sichtprüfung oder Messung offen | ungeprüft |

### [`messungen-schritt9.md`](messungen-schritt9.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Aufräumregel unterscheidet nicht zwischen von Test angelegt und zuletzt angefasst | M93-6 — eine VOTG-Katalogzeile ist verlorenge | Verbesserung | ungeprüft |

### [`nachrichtendetail.md`](nachrichtendetail.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Rückfall „Schritt N“ im Eigenschaftenblock nicht an echten Daten gesehen | §10.5 | Sichtprüfung oder Messung offen | ungeprüft |
| §10.10 Sichtprüfung Nr. 15 | Schmales Fenster des Nachrichtendetails nicht gesehen | §10.10 | Sichtprüfung oder Messung offen | ungeprüft |
| §10.15 Sichtprüfung Nr. 11 | Umschalter unter 1280 px: nur über Regelwerk belegt, nicht gesehen | §10.15 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Restzeile mit Eigenschaften (E‑223) nicht gesehen | §10.16 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Abbruchsätze bei zugeklappten Abschnitten (tiefeErreicht, zyklusErkannt) nicht gesehen | §10.16 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Für sich stehende Nachladen-Schaltfläche der Kette nicht gesehen | §10.16 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Schritt ohne Namen mit Eigenschaften (Rückfall Schritt N an der Zeile) nicht gesehen | §10.16 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Laufender Schritt aufgeklappt (LAEUFT_AUF) nicht gesehen | §10.16 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Gescheiterte Abfrage der Eigenschaften nicht am laufenden System gesehen | §10.16 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Aufklapp-Bewegung gemessen, vom Auftraggeber nicht angesehen | §10.16 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Rücksprung aus Belegsuche und Prozessansicht nicht im Browser gesehen | §10.18 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | LAEUFT_AUF gebaut, lokal nicht prüfbar (RUNNING null Mal) | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Weg über die Marke plausibel, aber unbeweisbar (Auskunft Altsystem nötig) | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | MessageActionID 500 und 502: Bedeutung nicht gemessen | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Kuratierte Auswahl nur an zwei Namen und zwei Mandanten gemessen | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Mehrere Werte eines kuratierten Namens: Darstellung | §13 | Entscheidung offen | **erledigt** — `nachrichtendetail.md` §13: „Entschieden in Teil 2 (§10.3): Sie zeigt sie alle … Der Fall bleibt trotzdem unbeobachtet…“ |
| ohne Nummer | Kappung der Eigenschaftswerte nie ausgelöst, nur unit-getestet | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Zeitzonen-Umrechnung setzt gleiche Zone von Anwendungs- und DB-Server voraus | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Beschriftung der Nachrichtenliste (wartet vor / läuft auf) | §13 | Fehler | **erledigt** — `nachrichtendetail.md` §13: „Erledigt am 11.08.2026 (Schritt 6, Teil 2a). … Damit ist auch der zweite Teil des Punktes erledigt…“ |
| ohne Nummer | Teile der Oberfläche nicht gegen echte Daten gesehen (LAEUFT_AUF, WARTET_VOR, EMPFANGEN, OHNE_AKTION, Kappung) | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | EMPFANGEN beruht auf Auskunft, nicht auf Messung | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | WARTET_VOR gebaut, in der Testkopie null Mal beobachtet | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Liste sagt „wartet vor“, Detail „wartet in“ | §13 | Fehler | **erledigt** — `nachrichtendetail.md` §13: „Erledigt am 11.08.2026 (Schritt 6, Teil 2a).…“ |
| ohne Nummer | Problemkategorie „Überfällig“ hat keine Farbrolle | §13 | Entscheidung offen | ungeprüft |
| ohne Nummer (§10.10 Nr. 15) | Schmales Fenster des Nachrichtendetails nicht gesehen | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Escape schließt Panel, eigene Route hat keine solche Taste | §13 | Fehler | **erledigt** — `nachrichtendetail.md` §13: „Nachgezogen am 10.08.2026 (§10.11)…“ |

### [`nachrichtenliste.md`](nachrichtenliste.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §8.4 Sichtprüfung Nr. 9 (und Nr. 1 halb) | Schmales Fenster nicht gesehen; Listenende nur über Inhaltslänge, nicht über Fensterhöhen geprüft | §8.4 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Freitextfilter bleibt teuer; Kennungsliste und UNION widerlegt, Index auf Message.SOSID unzulässig | §9 | Verbesserung | ungeprüft |
| ohne Nummer | Nachrichten wechseln den Prozess, SOSID bleibt: Folgen für Mandantengrenze der Kette | §9 | Entscheidung offen | **erledigt** — `nachrichtenliste.md` §9: „Geprüft am 10.08.2026 — M27: Quell- und Zielprozess führen in allen 9.101 Zeilen zum selben Mandante…“ |
| ohne Nummer | Hängende SPLITTED-Nachricht erscheint nie als überfällig | §9 | Verbesserung | ungeprüft |
| ohne Nummer | Status sagt nichts Verlässliches über Stellung in der Kette, Liste zeigt nur ihn | §9 | Entscheidung offen | **erledigt** — `nachrichtenliste.md` §9: „Rollenkennzeichnung in der Liste ist ausgeschlossen — Entscheidung des Auftraggebers … kein Fehler…“ |
| ohne Nummer | Groß-/Kleinschreibung bei der Fehlerbedingung (SQL vs. Java) | §9 | Fehler | **erledigt** — `nachrichtenliste.md` §9: „Erledigt am 06.08.2026.…“ |
| ohne Nummer | suche-abgebrochen unterscheidet zu große Anfrage nicht von Datenbanklast | §9 | Verbesserung | ungeprüft |
| ohne Nummer | BAM-Spaltenauflösung läuft je Anfrage | §9 | Verbesserung | **gegenstandslos** — `nachrichtenliste.md` §9: „Gegenstandslos seit dem 07.08.2026…“ |
| ohne Nummer | Kein SOSName in der Liste | §9 | Entscheidung offen | **erledigt** — `nachrichtenliste.md` §9: „Umgekehrt entschieden am 07.08.2026.…“ |
| ohne Nummer, Teil 1 | Statuszelle sagt „steht auf“, gemeint ist „wartet vor“ | §9 | Fehler | **erledigt** — `nachrichtenliste.md` §9: „Erledigt am 07.08.2026 in Schritt 5, Teil 2…“ |
| ohne Nummer, Teil 2 | Beschriftung nur für SUSPENDED belegt, für LAEUFT nicht (RUNNING null Mal) | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | „wartet vor“ widerlegt und steht trotzdem in der Zelle | §9 | Fehler | **erledigt** — `nachrichtenliste.md` §9: „Erledigt am 11.08.2026 (Schritt 6, Teil 2a).…“ |
| ohne Nummer | Schmales Fenster der Liste weiterhin ungesehen (Umbruch unter 768 px) | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Zeitzonen-Übergang setzt gleiche Zone von Anwendungs- und DB-Server voraus (Produktion ungeprüft) | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer | Zeilenklick hat keine Funktion | §9 | Fehler | **erledigt** — `nachrichtenliste.md` §9: „Erledigt in Schritt 5, Teil 2…“ |
| ohne Nummer | Relativer Tooltip rechnet gegen Browseruhr statt Anwendungsuhr | §9 | Verbesserung | ungeprüft |
| ohne Nummer | Prozessauswahl lädt alle Prozesse des Mandanten (NEXANS 733) und filtert im Speicher | §9 | Verbesserung | ungeprüft |
| ohne Nummer | Kein Virtualisieren der Tabelle | §9 | Verbesserung | ungeprüft |
| ohne Nummer | Leerzustand unterscheidet zu enges Fenster nicht von Datenlücke | §9 | Verbesserung | ungeprüft |

### [`process-view.md`](process-view.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer (Posten in der README-Tabelle der offenen Sichtprüfungen) | Sichtprüfung klebende Spalte von Hand: Mausrad, Touchpad, Bild-ab, Bildlaufleiste | §47 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (Befundtabelle §48) | datenmodell.md §3 führt Process/Project ohne Spalten, nicht nachgezogen | §48 | Verbesserung | ungeprüft |
| ohne Nummer | Gliederung: Schalter „Nach Projekt“ oder kompakte Auswahl | §48 | Entscheidung offen | **erledigt** — `process-view.md` §48: „Entschieden am 15.09.2026 — der Schalter bleibt … Der Auftraggeber hat sich für den Schalter entschi…“ |

### [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer (§3.3 Takt MatchInterchange) | Takt von MatchInterchange (stündlich) ist ungedeckt, Quelle beim Altsystem einzuholen | §3.3 | Sichtprüfung oder Messung offen | ungeprüft |
| ohne Nummer (§8 Aufbewahrungsdauer) | Aufbewahrungsdauer der Produktion (rund 18 Monate) ist ungedeckt, Quelle beim Betreiber einzuholen | §8 | Sichtprüfung oder Messung offen | ungeprüft |
| Annahme A11 | Unverschlüsselte DB-Verbindung: have_ssl vor dem Produktivbetrieb gegen die Produktion prüfen | §11 | Sichtprüfung oder Messung offen | **erledigt** — tragbar laut Auskunft vom 16.10.2026 (Entscheidung 5), nicht gemessen; `PROJEKTBESCHREIBUNG.md` §11 |

### [`property-suche.md`](property-suche.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §9 Nr. 1 | Zuordnung Typ‑0-Name → Spalte ist Vermutung; fachliche Bestätigung des Auftraggebers fehlt | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 2 | Kein Präfixmodus für Feldwerte; Bedarf erst durch die Oberfläche zu klären | §9 | Verbesserung | ungeprüft |
| §9 Nr. 3 | Präfix-Rückfall bei gemischter Suche wirkt nur auf BAM-Begriffe – richtige Antwort? | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 4 | PROJEKTBESCHREIBUNG Abschnitt 9 nennt die Property-Suche nicht (bewusst, E‑108) | §9 | Verbesserung | ungeprüft |
| §9 Nr. 5 | GET /api/bam/typen bleibt neben /api/bam/suchfelder; Ablösung offen | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 6 | Konfigurationstabelle der Testkopie von Hand geändert; Typ0AbbildungDbIT merkt Neubefüllung nicht | §9 | Verbesserung | ungeprüft |
| §9 Nr. 7 | Feld-Gruppe des Isolationstests prüft mangels Daten nur eine Richtung scharf | §9 | Verbesserung | ungeprüft |
| §9 Nr. 8 | Geländer von acht Begriffen für gemischte Suchen nicht gemessen | §9 | Sichtprüfung oder Messung offen | ungeprüft |
| §9 Nr. 9 | SNDPRN und VFN finden nur Wurzeln; Kette nur über den Absprung | §9 | Entscheidung offen | ungeprüft |
| §9 Nr. 10 | Vier Typ‑0-Felder brechen für NEXANS über ein Jahr ab (Status, SOSID, ProcessName, SOSName) | §9 | Entscheidung offen | ungeprüft — Fundstellen uneinig, siehe `property-suche.md` §9 |
| §9 Nr. 11 | Message.ProcessName vorab in Kennungen auflösen (Form der Nachrichtenliste) | §9 | Verbesserung | **erledigt** — `property-suche.md` §9: „Erledigt am 08.09.2026 (Teil 2): gebaut und gemessen, §10.…“ |
| §9 Nr. 12 | Feldbegriff ohne Treffer kostet den vollen Wertbereich (1,35 s für leere Antwort); Warnung offen | §9 | Verbesserung | ungeprüft |
| §15 Nr. 13 | Prozessname mit mehreren Kennungen (IN (?, ?)) nicht gemessen | §15 | Sichtprüfung oder Messung offen | ungeprüft |
| §15 Nr. 14 | Antwort der Nachrichtenliste nennt ihr aufgelöstes Fenster nicht; Absprung nur im freien Modus | §15 | Verbesserung | ungeprüft |
| §15 Nr. 15 | Präfix-Knopf bei gemischter Suche wirkt nur auf Belegnummern; Oberfläche sagt es nicht | §15 | Entscheidung offen | ungeprüft |
| §15 Nr. 16 | Kein Hinweis auf geringe Deckung eines Feldes (DestinationFilename, ReceiverID) | §15 | Verbesserung | ungeprüft |
| §15 Nr. 17 | Drei über ein Jahr abbrechende Felder im Angebot nicht gekennzeichnet | §15 | Verbesserung | ungeprüft |
| §15 Nr. 18 | Menü der Suchfeld-Auswahl mit bis zu 54 Einträgen; Feld schwer zu finden | §15 | Verbesserung | **erledigt** — `property-suche.md` §15: „Erledigt am 09.09.2026 (E‑112): Die Gruppen sind Untermenüs, die oberste Ebene trägt drei Einträge…“ |
| §15 Nr. 19 | Schmales Fenster bei 360 px gemessen; Berührungsfläche am Finger offen | §15 | Sichtprüfung oder Messung offen | ungeprüft |
| §15 Nr. 20 | Abschneidemeldung rät auch bei reiner Feldsuche zu einer zweiten Belegnummer | §15 | Verbesserung | ungeprüft |
| §15 Nr. 21 | Alle Belegarten wirkt wie Gruppenmitglied und ist einziger Weg zurück auf keine Auswahl | §15 | Entscheidung offen | ungeprüft |
| §15 Nr. 22 | Untermenüs der Suchfeld-Auswahl unter 768 px ungesehen | §15 | Sichtprüfung oder Messung offen | ungeprüft |
| §15 Nr. 23 | Fehler- und Abbruchtexte sagen Feld/Feldname statt Technische Eigenschaften (E‑113) | §15 | Verbesserung | ungeprüft |

### [`prozess-katalog-backend.md`](prozess-katalog-backend.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §10 Kasten | Zwei Tests in ProzessKatalogIsolationDbIT seit 27.08.2026 rot (keine übernehmbare Zeile) | §10 | Fehler | ungeprüft |
| §10 Nr. 2 | geaendert_von trägt den letzten Anfasser, kein „von wem angelegt" | §10 | Verbesserung | ungeprüft |
| §10 Nr. 3 | Kein Sekundärindex auf pflegestatus | §10 | Verbesserung | ungeprüft |
| §10 Nr. 4 | Sonderbehandlung von 00001_Undefined im Backend nicht gebaut | §10 | Verbesserung | ungeprüft |
| §10 Nr. 5 | Sichtprüfung des Katalog-Backends im Browser | §10 | Sichtprüfung oder Messung offen | ungeprüft |
| §10 Nr. 6 | Schreiben des Bestandslaufs (UPDATE-Stapel) nicht gemessen | §10 | Sichtprüfung oder Messung offen | ungeprüft |
| §10 Nr. 7 | Bestandslauf ohne Zeitlimit und ohne Fortschritt | §10 | Verbesserung | ungeprüft |
| §10 Nr. 8 | bestandGeprueft zählt Anweisungen, nicht geänderte Zeilen | §10 | Verbesserung | ungeprüft |
| §10 Nr. 9 | Filter aus E20 ist clientseitig, kein Serverparameter | §10 | Verbesserung | ungeprüft |
| §10 Nr. 10 | Kein Weg von GEPFLEGT zurück nach OFFEN; Übernahme verbreitert die Einbahnstraße | §10 | Entscheidung offen | ungeprüft |
| §10 Nr. 11 | „Per Regel übernommen" und „von Hand kuratiert" in der Zeile nicht unterscheidbar | §10 | Entscheidung offen | ungeprüft |
| §10 Nr. 12 | Aufräumregel der Tests unterscheidet nicht „angelegt" und „zuletzt angefasst" | §10 | Fehler | ungeprüft |
| §10 Nr. 13 | Schreiben der Vorschlagsübernahme nicht gemessen | §10 | Sichtprüfung oder Messung offen | ungeprüft |
| §10 Nr. 14 | Vorregistrierte Zahlen von M93 auf kuratierter Testkopie nicht reproduzierbar | §10 | Sichtprüfung oder Messung offen | ungeprüft |

### [`prozess-katalog-frontend.md`](prozess-katalog-frontend.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §12 Nr. 1 | Handlauf M85 nur an einem Mandanten gemessen (L7 verlangt zwei) | §12 | Sichtprüfung oder Messung offen | ungeprüft |
| §12 Nr. 2 | Anzeige der Laufdauer rundet auf ganze Sekunden | §12 | Verbesserung | ungeprüft |
| §12 Nr. 3 | PUT und AUSFUEHREN in der Sichtprüfung bewusst nicht gedrückt | §12 | Sichtprüfung oder Messung offen | ungeprüft |
| §12 Nr. 4 | Katalog am schmalen Fenster ungesehen (Formular, Vorschlagsliste) | §12 | Sichtprüfung oder Messung offen | ungeprüft |
| §12 Nr. 5 | Dauereinheiten stehen zweimal in den Sprachdateien | §12 | Verbesserung | ungeprüft |
| §12 Nr. 6 | Entfernter Partner bleibt bis zum nächsten Holen in der Auswahl | §12 | Verbesserung | ungeprüft |
| §12 Nr. 7 | Keine Rückfrage beim Verlassen der Seite mit offener Zeile (beforeunload) | §12 | Entscheidung offen | ungeprüft |
| §12 Nr. 8 | Partnervorschläge: localeCompare gegen utf8mb4_general_ci | §12 | Verbesserung | ungeprüft |
| §12 Nr. 9 | Übernahmedialog weist nicht auf fehlenden Rückweg hin | §12 | Verbesserung | ungeprüft |
| §12 Nr. 10 | Vorschlagsübernahme im Browser nur als Vorschau angesehen | §12 | Sichtprüfung oder Messung offen | ungeprüft |
| §12 Nr. 11 | Sichtprüfung auf NEXANS zeigt nur betroffen = 0 | §12 | Sichtprüfung oder Messung offen | ungeprüft |
| §12 Nr. 12 | Übernahmedialog am schmalen Fenster ungesehen | §12 | Sichtprüfung oder Messung offen | ungeprüft |

### [`prozess-katalog.md`](prozess-katalog.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §10 Nr. 1 | ZAST, NXHBE, EDITIONLINGERI: 61 Zeilen ohne Verfahren für den Partner | §10 | Entscheidung offen | ungeprüft |
| §10 Nr. 2 | Sollen WOC und SYSTEM überhaupt kuratiert werden? | §10 | Entscheidung offen | ungeprüft |
| §10 Nr. 3 | Regel A bei zwei Unterstrichen für neun NEXANS-Prozesse nicht geprüft | §10 | Sichtprüfung oder Messung offen | ungeprüft |
| §10 Nr. 4 | Richtungsanker in Projektnamen nicht erhoben; offen nur noch EDITIONLINGERI | §10 | Sichtprüfung oder Messung offen | ungeprüft |
| §10 Nr. 5 | Kein Weg von gepflegt zurück nach offen | §10 | Entscheidung offen | ungeprüft |
| §10 Nr. 6 | „Per Regel übernommen" und „von Hand kuratiert" in der Zeile nicht unterscheidbar | §10 | Entscheidung offen | ungeprüft |

### [`prozessauswahl.md`](prozessauswahl.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §9 Aufzählung Nr. 1 | Keine serverseitige Suche in der Prozessauswahl (nur örtliche Eingrenzung) | §9 | Verbesserung | ungeprüft |
| §9 Aufzählung Nr. 2 | Kein kuratierter Partner, Auswahl gruppiert nach ProjectName bis Schritt 9 | §9 | Verbesserung | ungeprüft |
| §9 Aufzählung Nr. 3 | Keine Angabe, ob ein Prozess im Zeitfenster Zeilen hat (ab Schritt 10 aus message_rollup) | §9 | Verbesserung | ungeprüft |

### [`archiv/README_bis-1.0.0.md`](archiv/README_bis-1.0.0.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Projektweite E-Nummern im Plan und weiteren Dateien zeigen auf falsche Entscheidungen, einzeln berichtigen | Die Nummernkreise — Kasten „Was das nicht hei | Fehler | ungeprüft |
| README: Wegwerfkonto der 9a-Prüfung | Wegwerfkonto der 9a-Prüfung deaktivieren, sobald der Zustand kein Zugriff gesehen ist | Offene Sichtprüfungen | Verbesserung | ungeprüft |
| ohne Nummer (Nachtrag 19.08.2026, Ding 1) | Leerzeilen trennen Tabellen der Datei (Verzeichnis, Offene Sichtprüfungen), Zeilen erscheinen als Text | Nachtrag 19.08.2026 — Verzeichnis gegen docs/ | Fehler | **gegenstandslos** — README am 16.10.2026 neu gefasst ([`archiv/konsolidierung-1.0.0.md`](archiv/konsolidierung-1.0.0.md)) |
| ohne Nummer (Nachtrag 19.08.2026, Ding 2) | Verzeichniszeile zu messungen-schritt8.md steht auf dem Stand 14.08.2026 (Teil B, M60 ff.) | Nachtrag 19.08.2026 — Verzeichnis gegen docs/ | Fehler | **gegenstandslos** — README am 16.10.2026 neu gefasst ([`archiv/konsolidierung-1.0.0.md`](archiv/konsolidierung-1.0.0.md)) |

### [`rohdaten-backend.md`](rohdaten-backend.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §11 Nr. 1 | docs/README.md fehlen Einträge für rohdaten-backend.md und START-LOKAL.md | §11 | Fehler | **erledigt** — `README.md` Nachtrag 19.08.2026 — Verzeichnis gegen docs/: „Er ist damit erledigt, steht in seiner Datei aber weiterhin als offen…“ |
| §11 Nr. 2 | Paket payload heißt nicht mehr passend (Vorschlag rohdaten) | §11 | Verbesserung | ungeprüft |
| §11 Nr. 3 | Antwortverarbeitung des jakarta-SAAJ-Zweigs ungemessen | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 4 | Zeitgrenze des Abrufs gesetzt, auf der Leitung nicht erprobt | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 5 | Mehr als ein ZIP-Eintrag: ob alle angeboten werden | §11 | Entscheidung offen | ungeprüft |
| §11 Nr. 6 | Schwelle der Binärerkennung (95 %) gesetzt, nicht gemessen | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 7 | Umschalter auf UTF-8 nicht gebaut | §11 | Verbesserung | ungeprüft |
| §11 Nr. 8 | app_user.download_allowed wird nicht geprüft (Widerspruch zweier Vorgaben) | §11 | Entscheidung offen | ungeprüft — Fundstellen uneinig, siehe `rohdaten-backend.md` §11, `rohdaten.md` §3 |
| §11 Nr. 9 | Anlassfall „Muster statt Familie“ entfallen, ein anderer Fall bleibt möglich | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 10 | Downloaddateiname lieferte einen Tag lang falsch benannte Datei | §11 | Fehler | **erledigt** — `rohdaten-backend.md` §11: „Behoben durch den Wegfall des Artefakts…“ |
| §11 Nr. 11 | Aussage „FileReader und FTPReader schließen einander aus“ war ungedeckt | §11 | Fehler | **erledigt** — `rohdaten-backend.md` §11: „Der Kommentar ist berichtigt, das Verhalten nicht…“ |

### [`rohdaten-frontend.md`](rohdaten-frontend.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §11 Nr. 1 | Sichtprüfung im Browser steht aus (beide Einhängepunkte, Ansicht, große Datei, schmales Fenster) | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 2 | Keine Zeilennummern in der Dateiansicht | §11 | Verbesserung | ungeprüft |
| §11 Nr. 3 | Kein Umschalter auf UTF-8 | §11 | Verbesserung | ungeprüft |
| §11 Nr. 4 | Fehlgeschlagener Download zeigt RFC-9457-Rumpf statt übersetzter Meldung | §11 | Entscheidung offen | ungeprüft |
| §11 Nr. 5 | Kein Sheet über der Detailansicht (spätere Zugabe) | §11 | Verbesserung | ungeprüft |
| §11 Nr. 6 | Block ordnet nach MessageActionID, Zeitleiste nach MessageActionStart | §11 | Fehler | **erledigt** — `rohdaten-frontend.md` §11: „Erledigt am 18.08.2026.…“ |
| §11 Nr. 7 | Escape schließt die Dateiansicht nicht | §11 | Entscheidung offen | **erledigt** — `rohdaten-frontend.md` §11: „Entschieden am 21.09.2026 (Auftraggeber) und am selben Tag gebaut.…“ |
| §11 Nr. 8 | app_user.download_allowed wird nicht geprüft | §11 | Entscheidung offen | **erledigt** — `rohdaten-frontend.md` §11: „Geschlossen 20.08.2026: Spalte entfernt.…“ |
| §11 Nr. 9 | Verhalten bei 610 KB im pre ungemessen | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 10 | rohdaten.md fehlt im Verzeichnis von docs/README.md | §11 | Fehler | **erledigt** — `rohdaten-frontend.md` §11: „Erledigt am 18.08.2026…“ |
| §11 Nr. 11 | Sprung in die Eigenschaften findet Gruppe über document.getElementById | §11 | Entscheidung offen | ungeprüft |
| §11 Nr. 12 | Ob der Schrittname als Schaltfläche gelesen wird, ungeprüft | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 13 | Ab etwa sieben Zielen an einer Zeitleistenzeile wird es eng | §11 | Entscheidung offen | ungeprüft |
| §11 Nr. 14 | Schrittname im Panel schmaler; ob das reicht, sagt erst die Sichtprüfung | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Nr. 15 | components/ui/separator.tsx wird nirgends mehr verwendet | §11 | Verbesserung | ungeprüft |
| §11 Nr. 16 | IMPLEMENTIERUNGSPLAN_MVP.md beschreibt Schritt 8 weiterhin als reinen Download | §11 | Entscheidung offen | ungeprüft |
| §11 Nr. 17 | Lesedienst-Paar als „Eingang“ beruht auf Sichtprüfung an einer Nachricht, nicht Messung | §11 | Entscheidung offen | ungeprüft |
| §11 Nr. 18 | Drei Zahlen der Datei rechnen über eine Menge, die die Oberfläche nicht mehr zeigt | §11 | Fehler | ungeprüft |
| §11 Nr. 19 | Regel aus M73 in zwei Fenstern gemessen, nicht im Bestand | §11 | Sichtprüfung oder Messung offen | ungeprüft |

### [`rohdaten.md`](rohdaten.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §12 Nr. 1 | Dev-Zeitanker ins abrufbare Filestore-Fenster legen | §12 | Verbesserung | ungeprüft |
| §12 Nr. 2 | PROJEKTBESCHREIBUNG.md §8 nachziehen (Aufbewahrung 22 vs. 18 Monate) | §12 | Verbesserung | ungeprüft |
| §12 Nr. 3 | Archivsystem als Annahme aufnehmen | §12 | Verbesserung | ungeprüft |
| §12 Nr. 4 | START-LOKAL.md um nicht abrufbare Ablagen ergänzen | §12 | Verbesserung | ungeprüft |
| §13 Nr. 1 | SOAP-Abhängigkeit: jakarta-Variante auf der Leitung ungeprüft | §13 | Sichtprüfung oder Messung offen | **erledigt** — `messungen-schritt8.md` M72 — Befund 1: „Offener Punkt 1 aus rohdaten.md ist damit beantwortet.…“ |
| §13 Nr. 2 | Handgebauter SOAP-Envelope unbelegt (Ursache des Scheiterns ungemessen) | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Nr. 3 | Warum die vier Formate im Altsystem abgeschaltet sind | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Nr. 4 | V4: Aufbewahrungsfrist im Filestore nicht gemessen | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Nr. 5 | Abnahme braucht eine Nachricht aus 2025-07-24 bis 2025-12-30 | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Nr. 6 | Mehr als ein ZIP-Eintrag: ob alle Einträge angeboten werden | §13 | Entscheidung offen | ungeprüft |
| §13 Nr. 7 | Lesedienst-Paar als „Eingang“ ist Sichtprüfung, keine Messung | §13 | Entscheidung offen | ungeprüft |
| §13 Nr. 8 | M73 in zwei Fenstern gemessen, nicht im Bestand | §13 | Sichtprüfung oder Messung offen | ungeprüft |
| §13 Nr. 9 | Downloaddateiname hängt an derselben Annahme (Muster statt Familie) | §13 | Sichtprüfung oder Messung offen | ungeprüft |

### [`testfestigkeit.md`](testfestigkeit.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| T-1 | Gleich viele DB-Zugriffe könnten verschieden lange dauern; Zugriffszählung deckt das nicht | §6 | Sichtprüfung oder Messung offen | ungeprüft |
| T-2 | Übernahme auf Mandant mit mehreren übernehmbaren Zeilen ungeprüft | §6 | Sichtprüfung oder Messung offen | ungeprüft |
| T-6 | Kennungsvergleiche der übrigen Isolationstests nicht auf ihre Richtung geprüft | §6 | Sichtprüfung oder Messung offen | ungeprüft |
| T-5 | Übernahme mit mehreren Zeilen weiterhin ungeprüft, nicht mehr per AUSFUEHREN nachholbar | §6 | Sichtprüfung oder Messung offen | ungeprüft |
| T-7 | Zur Laufzeit zusammengesetzter Farbwert für farbwerte.test.ts unsichtbar | §6 | Verbesserung | ungeprüft |
| T-8 | Aus Variable gebauter Importpfad für serverbausteine.test.ts unsichtbar | §6 | Verbesserung | ungeprüft |
| T-9 | Handgeschriebene Datei in components/ui/ von farbwerte.test.ts ausgenommen | §6 | Entscheidung offen | ungeprüft |
| T-10 | Über Farbwerte in globals.css sichert kein Test etwas zu (Sollwert je Token) | §6 | Entscheidung offen | ungeprüft |
| T-11 | Unsichere Menge in serverbausteine.test.ts als radix-Menge definiert, nicht als client-only | §6 | Entscheidung offen | ungeprüft |
| T-12 | Für „Modulebene greift auf den Browser zu“ hält kein Test etwas | §6 | Verbesserung | ungeprüft |
| T-13 | thema.test.ts liest Quelltext, nicht das gebaute CSS (dark-Variante) | §6 | Entscheidung offen | ungeprüft |
| T-3 | Test aus §2 braucht dauerhaft einen Prozess ohne Katalogzeile | §6 | Verbesserung | **erledigt** — `testfestigkeit.md` §6: „erledigt am 31.08.2026, siehe §9…“ |
| T-4 | KettenIsolationDbIT behauptet etwas über Wanduhrzeit (Regel T1) | §7 | Fehler | ungeprüft — Fundstellen uneinig, siehe `testfestigkeit.md` §7 |

### [`verkettung.md`](verkettung.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| §11 Aufzählung Nr. 1 | RUNNING in der Kette unbeobachtbar (Testkopie null Treffer), Stichprobe Produktion fehlt | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Aufzählung Nr. 2 | Tiefengrenze der Kette ungetestet gegen echte Daten | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Aufzählung Nr. 3 | Wird die Kette über mehr als eine Ebene abwärts gebraucht? | §11 | Entscheidung offen | ungeprüft |
| §11 Aufzählung Nr. 4 | Reihenfolge der Vorgänger im Kettenblock | §11 | Entscheidung offen | **erledigt** — `verkettung.md` §11: „Entschieden in Teil 2b: Sie bleibt die des Aufstiegs (Ebene -1 zuerst)…“ |
| §11 Aufzählung Nr. 5 | /kette liefert keinen Cursor; erste nachgeladene Seite wiederholt die gezeigten fünfzig | §11 | Verbesserung | **erledigt** — `verkettung.md` §11: „Erledigt am 11.08.2026: abwaertsCursor ist gebaut (§2)…“ |
| §11 Aufzählung Nr. 6 | Hinweis auf Verzögerung des MatchInterchange-Events fehlt (Nachlauf ungemessen) | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Aufzählung Nr. 7 | Zwischen „Kinder existieren“ und „Flag gesetzt“ zeigt das Panel gar nichts | §11 | Entscheidung offen | ungeprüft |
| §11 Aufzählung Nr. 8 | Kettenblock nie gegen tiefe oder abgebrochene Kette aus echten Daten gesehen | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Aufzählung Nr. 9 | Breiteste Wurzel (3.350 Teile) nicht gesehen; ob Nachladen dort trägt, offen | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Aufzählung Nr. 10 | Schmales Fenster für den Kettenblock nicht gesehen | §11 | Sichtprüfung oder Messung offen | ungeprüft |
| §11 Aufzählung Nr. 11 | Ganze Seite Abwärtsglieder ohne MessageLastUpdate nicht blätterbar (kein Cursor) | §11 | Verbesserung | ungeprüft |

### [`visuelles-konzept.md`](visuelles-konzept.md)

| Kennung | Kurztext | Abschnitt | Art | Stand |
|---|---|---|---|---|
| ohne Nummer | Keine Farbrolle für die Problemkategorie unquittiert | §7 | Entscheidung offen | ungeprüft |
| ohne Nummer | Problemkategorie Überfällig hat keine Farbrolle | §7a | Entscheidung offen | **erledigt** — `visuelles-konzept.md` §7a: „„Überfällig“ hat keine Farbrolle — ✔ entschieden am 31.08.2026 (Schritt 10b‑3a)…“ |
