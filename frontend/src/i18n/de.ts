/**
 * Deutsch — die Leitsprache. `en.ts` hat denselben Schlüsselsatz; ein Test
 * blockiert jede Abweichung in beide Richtungen.
 *
 * Hier steht **jede** Zeichenkette, die ein Nutzer je zu sehen bekommt. In einer
 * Komponente steht keine.
 *
 * Der Abschnitt `fehler` ist nach dem **maschinenlesbaren `type`** der
 * RFC-9457-Antwort geschlüsselt, nicht nach dem Text. Das Backend liefert
 * `https://overlord.kraftwerkone.de/probleme/<schlüssel>`; übersetzt wird der
 * letzte Pfadabschnitt. Fehlt ein Schlüssel, zeigt die Oberfläche `detail` aus
 * der Antwort — deutsch, aber immerhin richtig.
 */
export const de = {
  anwendung: {
    name: "Overlord Monitoring",
    beschreibung: "Zustand der EDI-Übertragungen der Integrationsplattform Overlord",
  },

  navigation: {
    bezeichnung: "Hauptnavigation",
    oeffnen: "Menü öffnen",
    schliessen: "Menü schließen",
    // Die Schlüssel hier sind zugleich die Schlüssel in lib/navigation.ts.
    //
    // Der erste Eintrag hieß bis zum 01.09.2026 „Startseite" — ein Wort über den
    // Ort, nicht über den Inhalt. Seit dort das Dashboard steht, sagt er, was er
    // zeigt. **Der Schlüssel bleibt `startseite`**: Er gehört zur Route und
    // nicht zur Beschriftung, und ein zweiter Eintrag entsteht hier nicht
    // (`lib/navigation.ts`).
    eintraege: {
      startseite: "Übersicht",
      nachrichten: "Nachrichten",
      prozesse: "Prozesse",
      administration: "Administration",
    },
  },

  kopfzeile: {
    mandantBezeichnung: "Aktiver Mandant",
    keinMandant: "Kein Mandant gewählt",
    mandantWechseln: "Mandant wechseln",
    spracheBezeichnung: "Sprache",
    spracheDeutsch: "Deutsch",
    spracheDeutschKurz: "DE",
    spracheEnglisch: "English",
    spracheEnglischKurz: "EN",
  },

  nutzermenue: {
    oeffnen: "Benutzermenü",
    angemeldetAls: "Angemeldet als",
    passwortAendern: "Passwort ändern",
    abmelden: "Abmelden",
    abmeldenLaeuft: "Wird abgemeldet …",
  },

  // Im Code heißen die Stufen `xs`, `s`, `m`, `l` (`src/dichte/`) — in der
  // Oberfläche nicht. Ein Nutzer, der einen Beleg sucht, liest „Anzeigegröße"
  // und nicht „Dichte": Das eine ist, was er sieht, das andere ist, wie ein
  // Gestalter es nennt.
  //
  // Die vier Stufen nennen alle vier eine Größe — seit dem 01.09.2026 auch die
  // dritte. Sie hieß „Standard", und das war ein Name für eine Rolle in einer
  // Reihe, die sonst Größen nennt. Vor allem kodierte er eine Entscheidung, die
  // ausdrücklich offen ist: Ob `m` die Vorgabe bleibt, ist laut
  // `docs/dichte-umschalter.md` E‑z eine eigene und spätere Entscheidung —
  // wanderte sie, hieße „Standard" nicht mehr Standard. Welche Stufe die
  // Vorgabe ist, muss die Beschriftung nicht tragen: Wer nie umgestellt hat,
  // sieht den Haken dort.
  dichte: {
    bezeichnung: "Anzeigegröße",
    stufen: {
      xs: "Sehr klein",
      s: "Klein",
      m: "Mittel",
      l: "Groß",
    },
  },

  // „Erscheinungsbild" und nicht „Thema", und erst recht nicht „Dunkelmodus":
  // Die Wahl hat drei Werte, und einer davon ist *hell*. „Dunkelmodus" wäre der
  // Name eines der drei Zustände als Name für die Wahl selbst. „Thema" wiederum
  // ist das Wort des Codes (`src/thema/`) — der Nutzer liest, was er sieht.
  //
  // Der dritte Wert heißt „Systemeinstellung" und nicht „Automatisch": Er sagt,
  // WOHER die Auskunft kommt, und das ist die nützlichere Angabe. „Automatisch"
  // ließe offen, ob nach Uhrzeit, nach Umgebungslicht oder nach dem Gerät
  // geschaltet wird.
  thema: {
    bezeichnung: "Erscheinungsbild",
    werte: {
      hell: "Hell",
      dunkel: "Dunkel",
      system: "Systemeinstellung",
    },
  },

  rolle: {
    ADMIN: "EDI-Betreuung",
    MANDANT: "Mandant",
  },

  /**
   * Die acht Einordnungen aus `common/MessageStatusKind` — **auf oberster Ebene,
   * seit dem 01.09.2026.**
   *
   * Sie standen bis dahin unter `nachrichten.status`, und das war richtig,
   * solange die Liste ihr einziger Verbraucher war. Mit dem Dashboard sind es
   * zwei: Die Liste beschriftet eine Plakette an einer Zeile, der Verlauf
   * beschriftet dieselbe Einordnung über viele; seit dem 03.09.2026 nimmt das
   * Dashboard sie auch für die beiden Zustandskacheln. **Alle drei müssen
   * dasselbe Wort sagen**, sonst heißt dieselbe Sache an drei Stellen
   * verschieden — und der Nutzer hält sie für drei Sachen.
   *
   * Auf derselben Ebene stand bis zum 03.09.2026 `problem` mit dem Wort für
   * *Überfällig*. Die Kategorie ist mit E‑71 widerlegt; der Block ist ohne
   * Verbraucher entfallen. **Anders als die Farbrolle `--ueberfaellig`, die
   * nach E‑77 bleibt** — die ist gerechnet und gegengeprobt, ein übersetztes
   * Wort ist an dem Tag, an dem eine echte Schwelle zurückkommt, in einer
   * Minute wieder da.
   *
   * AUFGETEILT und ZUSAMMENGEFUEHRT sind am 11.08.2026 an die Stelle des einen
   * Wertes „Zwischenschritt" getreten. Technisch waren SPLITTED und MERGED
   * dasselbe; für den Nutzer bedeuten sie Gegenteiliges — aus eins wurde viel
   * gegen aus viel wurde eins. Das Wort „Zwischenschritt" kommt in keiner
   * Oberflächenzeichenkette mehr vor.
   */
  einordnung: {
    FEHLER: "Fehler",
    WARTEND: "Wartend",
    LAEUFT: "Läuft",
    AUFGETEILT: "Aufgeteilt",
    ZUSAMMENGEFUEHRT: "Zusammengeführt",
    ABGESCHLOSSEN: "Abgeschlossen",
    QUITTIERT: "Quittiert",
    UNGEKLAERT: "Ungeklärt",
  },

  anmeldung: {
    titel: "Anmeldung",
    einleitung: "Melde dich an, um den Zustand deiner EDI-Übertragungen zu sehen.",
    benutzername: "Benutzername",
    passwort: "Passwort",
    absenden: "Anmelden",
    laeuft: "Anmeldung läuft …",
    felderFehlen: "Bitte Benutzername und Passwort eingeben.",
  },

  passwort: {
    titel: "Passwort ändern",
    zwangEinleitung:
      "Dieses Konto verwendet noch ein Einmalpasswort. Vergib zuerst ein eigenes; bis dahin sind die übrigen Funktionen gesperrt.",
    freiwilligEinleitung: "Vergib ein neues Passwort für dieses Konto.",
    alt: "Bisheriges Passwort",
    neu: "Neues Passwort",
    wiederholung: "Neues Passwort wiederholen",
    regel: "Mindestens zwölf Zeichen. Länge wirkt, Sonderzeichen kaum.",
    absenden: "Passwort speichern",
    laeuft: "Wird gespeichert …",
    wiederholungFalsch: "Die beiden Eingaben stimmen nicht überein.",
    zurueck: "Zurück",
  },

  mandantenauswahl: {
    titel: "Mandant wählen",
    einleitung: "Alle Ansichten zeigen ausschließlich Daten des hier gewählten Mandanten.",
    aktiv: "Aktiv",
    waehlen: "Auswählen",
    laeuft: "Wird gewechselt …",
    leerTitel: "Kein Mandant verfügbar",
    leerHinweis:
      "Diesem Konto ist kein Mandant zugeordnet. Wende dich an die EDI-Betreuung, damit die Zuordnung ergänzt wird.",
  },

  /**
   * Die drei Rollup-Paare — **an einer Stelle für beide Ansichten**, die sie
   * tragen: das Dashboard und die Prozessansicht.
   *
   * Bis zum 02.09.2026 standen sie unter `dashboard.zeitraum`. Der Umschalter
   * ist an diesem Tag nach `components/` gewandert, weil ein Feature nicht aus
   * einem Nachbarfeature importiert (`docs/frontend-grundlagen.md` §8) — und ein
   * Baustein in `components/` liest keinen Textblock eines Features.
   *
   * Die Schlüssel sind die **Codes des Backends** und keine Namen: `48H`, `30T`,
   * `12M` stehen so in der URL und in der Antwort.
   */
  zeitraum: {
    bezeichnung: "Zeitraum",
    "48H": "48 Stunden",
    "30T": "30 Tage",
    "12M": "12 Monate",
    // Der vierte Knopf, freiwillig — nur die Prozessansicht ruft ihn
    // (`components/zeitraum-umschalter.tsx`).
    FREI: "Frei",
    von: "Von",
    bis: "Bis",
    // Ein datetime-local liefert erst dann einen Wert, wenn Datum UND Uhrzeit
    // vollständig sind — erkannt an validity.badInput (`docs/nachrichtenliste.md` §8.2).
    unvollstaendig: "Bitte Datum und Uhrzeit vollständig eintragen.",
    beideNoetig: "Für ein freies Zeitfenster fehlt noch der zweite Zeitpunkt.",
  },

  /**
   * Die Landingpage (`docs/dashboard-frontend.md`).
   *
   * **Sie hat den Block `startseite` abgelöst.** Dort standen bis zum 01.09.2026
   * `titel`, `platzhalterTitel` und `platzhalterHinweis` der bewusst leeren
   * Startseite — Texte, die es nicht mehr gibt, weil es den Platzhalter nicht
   * mehr gibt. Die Route ist dieselbe geblieben (Entscheidung E‑r).
   */
  dashboard: {
    titel: "Übersicht",
    // Ein Text für beide Fälle — „im Zeitraum nichts" und „dieser Mandant hat
    // keine Daten" werden nicht unterschieden (Entscheidung E‑p). Der Satz ist
    // in beiden wahr, und die Antwort trägt kein Feld, das sie trennt
    // (`docs/dashboard.md` §6, bekannte Grenze 2).
    leerTitel: "Nichts im Zeitraum",
    leerHinweis:
      "In diesem Zeitraum ist keine Nachricht bewegt worden. Wähle einen größeren Zeitraum — " +
      "bleibt es dabei, hat dieser Mandant keine Daten.",

    verlauf: {
      titel: "Verlauf",
      // Vier Reihen, eine je Farbrolle (Entscheidung E‑l). Die Beschriftungen
      // gehören der Rolle und nicht einer Einordnung: Die Rolle `offen` fasst
      // vier Einordnungen zusammen, und die stehen einzeln im Tooltip.
      //
      // Entscheidung E‑82 vom 04.09.2026: Eine Rolle, die mehrere Einordnungen
      // bündelt, trägt eine Beschriftung, die für alle ihre Mitglieder gilt und
      // keines von ihnen wiederholt. Bis dahin stand hier „Offen" und
      // „Abgeschlossen" — das erste war falsch (AUFGETEILT und ZUSAMMENGEFUEHRT
      // sind Endstatus, `docs/message-status.md`), das zweite wiederholte den
      // Namen einer seiner beiden Einordnungen.
      //
      // Der Tokenname bleibt `--status-offen` beziehungsweise
      // `--status-abgeschlossen`; Name und Beschriftung fallen ab hier mit
      // Absicht auseinander (`docs/visuelles-konzept.md` §2).
      rollen: {
        fehler: "Fehler",
        offen: "Ohne Ergebnis",
        abgeschlossen: "Erledigt",
        ungeklaert: "Ungeklärt",
      },
      gesamt: "Gesamt",
      achseAnzahl: "Nachrichten",
      streifenTitel: "Fehler im Zeitverlauf",
      streifenAchse: "Fehler",
      // Ohne diesen Satz liest jemand die Höhe des Streifens gegen den Balken
      // darüber und hält fünf Fehler für ein Drittel des Verkehrs.
      streifenHinweis:
        "Eigene Skala: Die Höhe dieses Streifens ist nicht mit dem Verlauf darüber vergleichbar.",
      streifenLeer: "Im Zeitraum ist keine Nachricht als Fehler eingeordnet.",
    },

    kacheln: {
      nachrichten: "Nachrichten",
      // Bekannte Grenze 3 aus `docs/dashboard.md` §2: Der Rollup gruppiert nach
      // MessageLastUpdate. Ein Batchlauf, der alte Nachrichten anfasst, hebt den
      // Balken der Nachtstunde, ohne dass eine neue eingegangen wäre.
      nachrichtenHinweis:
        "Gezählt wird Bewegung: Eine Nachricht erscheint in dem Zeitraum, in dem sie zuletzt " +
        "verändert wurde — nicht in dem, in dem sie eingegangen ist.",
      fehler: "Fehler",
      fehlerVerweis: "Diese Nachrichten in der Liste öffnen",
      artenAufklappen: "Nach Art aufschlüsseln",
      artenZuklappen: "Aufschlüsselung schließen",
      // Die Wörter für die beiden Zustandskacheln stehen NICHT hier, sondern in
      // `einordnung.LAEUFT` und `einordnung.WARTEND` — dieselben, die der
      // Statusfilter und die Plakette der Liste tragen. Ein eigenes Wort hier
      // wäre dieselbe Sache zum zweiten Mal benannt.
      laeuftVerweis: "Laufende Nachrichten in der Liste öffnen",
      wartendVerweis: "Wartende Nachrichten in der Liste öffnen",
      // Entscheidung E‑75: Das Alter der ältesten Zeile ist die laufende Prüfung
      // der Auskunft, auf der E‑71 ruht. Es entfällt bei `anzahl = 0` — ohne
      // Zeile gibt es kein Alter, und die Null steht für sich.
      aeltesterSeit: "ältester seit {dauer}",
      // Die Notbremse (E‑80): Über der Höchstspanne der Liste (ein Jahr, L1)
      // klickt die Kachel nicht. Ein Link, der weniger zeigt als die Kachel
      // nennt, entsteht nicht — auch nicht still.
      wartendOhneVerweis:
        "Die älteste dieser Nachrichten liegt länger als ein Jahr zurück. Die Nachrichtenliste " +
        "zeigt höchstens ein Jahr — ein Verweis führte deshalb auf eine kleinere Zahl als hier " +
        "steht.",
      // Ohne diesen Satz widersprechen sich zwei Zahlen auf derselben Seite
      // sichtbar, sobald 48 Stunden gewählt sind — der Normalfall.
      //
      // **Er steht IN der Kachel und nicht als geteilte Zeile darunter.** Eine
      // geteilte Zeile müsste die beiden Kacheln benennen — und nennte damit bei
      // einem Mandanten ohne suspendierende Abläufe eine Kachel, die es auf
      // seiner Seite gar nicht gibt.
      bestandHinweis: "Gezählt wird der gesamte Bestand, nicht der gewählte Zeitraum.",
      // Entscheidung E‑q. Bewusst kein Rot und keine Fehler-Kennung: Für den
      // Nutzer ist das eine Auskunft und kein technischer Fehler. Sie gilt seit
      // dem 03.09.2026 für `laeuft` und `wartend` — und je Kachel einzeln: Die
      // beiden Zahlen EINER Kachel fallen zusammen, die beiden Kacheln nicht.
      nichtErmittelbar: "—",
      nichtErmittelbarHinweis:
        "Diese Zahl wird bei jedem Aufruf frisch gezählt, und die Zählung ist an der Zeitgrenze " +
        "der Datenbank abgebrochen. Die übrigen Zahlen dieser Seite stehen.",
      /**
       * Dieselben fünf Bausteine wie unter `nachrichten.detail.dauer` und
       * `katalog.lauf.dauer` — die **dritte** Kopie.
       *
       * **Bewusst noch einmal und nicht von dort gelesen.** Sie gehören keiner
       * Ansicht und müssten auf die oberste Ebene; sie dorthin zu heben, fasst
       * die Schlüssel des Nachrichtendetails an und ist eine eigene Runde. Der
       * offene Punkt steht seit dem 26.08.2026 in
       * `docs/prozess-katalog-frontend.md` §11 (Punkt 8) — mit dieser Kopie
       * betrifft er drei Stellen statt zwei.
       *
       * **Der Formatierer bleibt derselbe** (`formatiereDauer`): Ein zweiter
       * wäre eine zweite Wahrheit über dieselbe Größe.
       */
      dauer: {
        unterSekunde: "< 1 s",
        sekunden: "{wert} s",
        minuten: "{wert} min",
        stunden: "{wert} h",
        tage: "{wert} d",
      },
    },

    // C.4: Der Endpunkt liefert Rohwert und Art. Beschriftet wird über den
    // ROHWERT und nicht über die gelieferte Art — die ist ein deutscher
    // Festtext aus dem Backend und stünde sonst auch im englischen Baum.
    // Unbekannte Rohwerte bleiben Rohwerte (Regel Q4).
    fehlerarten: {
      COMMIT_REJECTED: "Vom Partner abgelehnt",
    },

    verteilung: {
      titelPartner: "Nach Partner",
      titelRichtung: "Nach Richtung",
      bezeichnung: "Verteilung",
      partner: "Partner",
      richtung: "Richtung",
      EINGEHEND: "Eingehend",
      AUSGEHEND: "Ausgehend",
      uebrige: "Übrige ({anzahl})",
      // Eine Aussage über den KATALOG: Null heißt „alles kuratiert". Wird die
      // Zeile bei null ausgeblendet, ist „vollständig gepflegt" nicht mehr von
      // „diese Ansicht zeigt das nicht" zu unterscheiden.
      nichtZugeordnet: "nicht zugeordnet",
      nichtZugeordnetHinweis:
        "Prozesse ohne gepflegten Eintrag im Katalog. Null heißt: Es ist alles zugeordnet.",
      keineVerweise:
        "Die Nachrichtenliste kennt keinen Partnerfilter — diese Zeilen klicken nicht.",
    },

    aufgefallen: {
      titel: "Zuletzt aufgefallen",
      leer: "Im Zeitraum ist nichts aufgefallen.",
      // Entscheidung E‑90 vom 04.09.2026: Der Block trägt seither eine Zeile je
      // PROZESS mit Anzahl und jüngstem Zeitpunkt. `ohneProzess` ist damit
      // entfallen — eine Zeile ohne Prozess kann es nicht geben, sie käme durch
      // die Mandantenkette nicht hindurch.
      //
      // Sichtbar steht an der Zeile nur die Ziffer; der Blockkopf sagt schon,
      // worum es geht, und das Wort an jeder Zeile sagte es zehnmal. Diese
      // beiden Sätze stehen im `aria-label`, wo die nackte Zahl keine Auskunft
      // wäre.
      anzahlEins: "1 Nachricht",
      anzahlViele: "{anzahl} Nachrichten",
      zeileOeffnen: "Diese {anzahl} in der Nachrichtenliste zeigen",
      // Das Wort zum Zeichen (E‑91). Es steht als **Verzeichnis über
      // `Auffaelligkeit`** und nicht als ein Wort: `kategorie` ist heute eine
      // Aufzählung mit einem Wert (offener Punkt 133), und Regel Q3 verlangt,
      // dass Problemkategorien getrennt geführt werden. Kommt eine zweite
      // zurück, ist hier eine Zeile zu ergänzen — und der Zeilenkopf im
      // Quelltext meldet sich, wenn sie fehlt.
      //
      // NICHT `einordnung.FEHLER` wiederverwendet, obwohl dort dasselbe Wort
      // steht: Das ist die `Statusart`, dies ist die `Auffaelligkeit`. Zwei
      // Aufzählungen, die heute zufällig einen Namen teilen — E‑82 ist genau
      // daran entstanden, dass ein Name aus der einen Menge in die
      // Beschriftungsposition der anderen gerutscht ist.
      kategorie: {
        FEHLER: "Fehler",
      },
    },

    stand: {
      // Absolut und nicht relativ (Entscheidung E‑o): Die Antwort trägt kein
      // `jetzt`-Feld, und der Browser rechnet gegen die echte Uhr — im Profil
      // `dev` stünde dort „vor acht Monaten".
      satz: "Zahlen vom {zeitpunkt}",
      artVOLL: "vollständiger Lauf",
      artDELTA: "laufende Fortschreibung",
      ohneLauf: "Es hat noch keinen abgeschlossenen Rollup-Lauf gegeben.",
    },
  },

  platzhalter: {
    titel: "In Arbeit",
    hinweis: "Diese Ansicht entsteht in einem späteren Schritt.",
  },

  /**
   * Der Administrationsbereich — **ein** Navigationseintrag, zwei Unterseiten
   * (`docs/frontend-grundlagen.md` §2).
   *
   * Die Beschriftung des Menüpunkts steht weiterhin unter
   * `navigation.eintraege.administration`; hier steht, was *innerhalb* des
   * Bereichs zu lesen ist. Beide sagen dasselbe Wort — Beschriftung und Route
   * heißen seit dem 24.08.2026 beide „Administration".
   */
  administration: {
    einleitung: "Die Bereiche, die der EDI-Betreuung vorbehalten sind.",
    bereichsnavigation: "Bereiche der Administration",
    bereiche: {
      katalog: {
        titel: "Prozess-Katalog",
        beschreibung:
          "Partner und Richtung je Prozess pflegen — die Grundlage dafür, dass sich Übertragungen später nach Partner auswerten lassen.",
      },
      benutzer: {
        titel: "Benutzer",
        beschreibung: "Konten anlegen, sperren und ihre Mandanten pflegen.",
      },
    },
  },

  nachrichten: {
    spalten: {
      zeitpunkt: "Zeitpunkt",
      status: "Status",
      // Der Anzeigename des Ablaufs (SOSName). „Prozess" ist als Spalte
      // entfallen — ProcessName steht jetzt im Tooltip dieser Zelle.
      ablauf: "Ablauf",
      projekt: "Projekt",
    },
    // Der Prozessname als Beschriftung des Tooltips: Ohne sie stünde dort ein
    // technischer Bezeichner ohne Auskunft darüber, was er ist.
    prozessName: "Prozess",
    // „Nicht zugeordnet heißt nicht zugeordnet" (Regel Q4). Das Backend liefert
    // hier null und erfindet nichts; der Ersatztext ist eine Entscheidung der
    // Oberfläche und steht deshalb hier.
    nichtZugeordnet: "nicht zugeordnet",
    ohneWert: "—",
    rohwert: "Statuswert des Altsystems",
    // Der Schritt neben der Statusplakette. Er kommt aus SOSAction und ist
    // Klartext (Messung M13) — die Sprache ist die des Quellsystems und wird
    // nicht eingedeutscht.
    //
    // **Ohne Präposition, seit dem 11.08.2026.** Hier standen vier Texte: „wartet
    // vor: …" und „läuft auf: …", je mit eigenem Tooltip. Messung M29 hat das
    // erste über alle 538 wartenden Nachrichten widerlegt — sie warten *in* dem
    // Schritt, der sie schlafen gelegt hat, nicht davor. Ob in oder vor,
    // entscheidet der Vergleich mit dem zuletzt gelaufenen Schritt; der steht in
    // MessageAction und wird von der Liste nicht je Seite gejoint (L2, L3). Die
    // Zelle nennt deshalb Status und Schritt und behauptet nichts darüber, wie
    // die Nachricht zu ihm steht.
    schrittZusatz: "Schritt: {schritt}",
    zeileOeffnen: "Details dieser Nachricht anzeigen",
    bedeutungNichtVerifiziert: "Bedeutung nicht verifiziert",
    // Steht unter der Tabelle, sobald eine solche Zeile auf der Seite ist. Bis
    // zum 07.08.2026 stand der Hinweis nur im title-Attribut — auf einem
    // Touchgerät gibt es keinen Hover, und dort erfuhr der Nutzer nie, was das
    // Fragezeichen bedeutet.
    ungeklaertFusszeile:
      "Eine Plakette mit Fragezeichen zeigt den Statuswert des Altsystems unverändert: Seine " +
      "fachliche Bedeutung ist nicht belegt, und es wird nichts geraten.",
    zeitRelativ: "Abstand zu jetzt",
    sortierungUmschalten: "Nach Zeitpunkt sortieren",
    sortierungNeueste: "Neueste zuerst",
    sortierungAelteste: "Älteste zuerst",

    zeitfenster: {
      bezeichnung: "Zeitfenster",
      h24: "24 Stunden",
      d7: "7 Tage",
      d30: "30 Tage",
      frei: "Frei",
      von: "Von",
      bis: "Bis",
      // Bewusst ohne Zahl: Die Vorgabe steht im Backend (Regel L1), und ein
      // zweiter Wert hier liefe dem ersten irgendwann hinterher.
      standardHinweis: "Ohne Auswahl gilt das Standardfenster des Servers.",
      zuruecksetzen: "Zeitfenster zurücksetzen",
      // Ein datetime-local liefert erst dann einen Wert, wenn Datum UND Uhrzeit
      // vollstaendig sind. Wer nur das Datum eintippt, sieht es im Feld stehen —
      // und die Anwendung tut nichts und sagt nichts. Genau das war der Befund
      // vom 07.08.2026; erkannt wird der Zustand an validity.badInput.
      unvollstaendig: "Bitte Datum und Uhrzeit vollständig eintragen.",
      // Beide Zeitpunkte fehlen noch: kein Fehler, sondern ein Zwischenzustand.
      beideNoetig: "Für ein freies Zeitfenster fehlt noch der zweite Zeitpunkt.",
    },

    statusfilter: {
      bezeichnung: "Status",
      alle: "Alle Status",
      gewaehlt: "{anzahl} gewählt",
      zuruecksetzen: "Statusfilter leeren",
    },

    prozessfilter: {
      bezeichnung: "Prozess",
      alle: "Alle Prozesse",
      gewaehlt: "{anzahl} gewählt",
      suchen: "Prozess einschränken",
      keineTreffer: "Kein Prozess passt zu dieser Eingabe.",
      leer: "Für diesen Mandanten ist kein Prozess hinterlegt.",
      unbekannt: "Prozess eines anderen Mandanten",
      zuruecksetzen: "Prozessfilter leeren",
    },

    suche: {
      // **Sie benennt, worin gesucht wird** — angepasst in Schritt 7, Teil 3.
      // Seitdem steht ein zweites Suchfeld in der Kopfzeile, und das sucht etwas
      // anderes: Belegnummern. Zwei Felder auf demselben Bildschirm, die
      // verschiedene Dinge tun und verschieden fehlschlagen, sind eine Falle —
      // besonders für den Nutzer, der kein EDI-Spezialist ist. Hier stand bis
      // dahin „Suche".
      bezeichnung: "Prozess, Projekt oder Ablauf durchsuchen",
      platzhalter: "Prozess-, Projekt- oder Ablaufname",
      zuKurz: "Noch {anzahl} Zeichen, dann wird gesucht.",
      leeren: "Suche leeren",
      // Beide Zahlen kommen aus der Antwort des Backends, nicht aus diesem Text.
      // Die Grenze gehört dorthin, wo sie gemessen wurde.
      fensterZuGross:
        "Die Suche gilt für höchstens {grenze} Tage; gewählt sind {angefragt}. Verkleinere das Zeitfenster.",
      trotzdemSuchen: "Trotzdem suchen",
      // Bewusst eine Auskunft und keine Warnung: Der Nutzer hat das gerade selbst
      // entschieden, er soll nur wissen, was ihn erwartet.
      langeSucheLaeuft: "Die Suche läuft über einen längeren Zeitraum und kann etwas dauern.",
    },

    // Hier stand bis zum 11.08.2026 der Textblock des Ausblende-Chips. Er ist mit
    // dem Schalter entfallen; die Liste blendet nichts mehr aus und hat deshalb
    // auch nichts mehr anzukündigen (docs/nachrichtenliste.md §5).

    blaettern: {
      zurueck: "Vorherige Seite",
      vor: "Nächste Seite",
      seiteEins: "Seite 1",
      weitereSeite: "weitere Seite",
    },

    aktualisierung: {
      schalter: "Automatisch aktualisieren",
      jetztAktualisieren: "Jetzt aktualisieren",
      stand: "Stand {zeit}",
      standUnbekannt: "Noch nicht geladen",
      pausiertGeblaettert: "Pausiert, solange geblättert wird.",
      laeuft: "Wird aktualisiert …",
    },

    leer: {
      titel: "Keine Nachricht in diesem Ausschnitt",
      zeitfenster: "Erweitere das Zeitfenster.",
      // Die Klausel zu den ausgeblendeten Zwischenschritten ist mit dem Schalter
      // entfallen. Die übrigen bleiben vollzählig — genannt werden alle
      // greifenden Einschränkungen, nicht nur die erste.
      suche: "Der Suchbegriff schränkt die Liste ein — leere ihn.",
      status: "Der Statusfilter schränkt die Liste ein — leere ihn.",
      prozess: "Der Prozessfilter schränkt die Liste ein — leere ihn.",
      fensterErweitern: "Auf 30 Tage erweitern",
    },

    // Die Detailansicht (Schritt 5). Sie beantwortet in verständlicher Sprache,
    // was mit einer Nachricht passiert ist.
    detail: {
      titel: "Nachricht",
      schliessen: "Ansicht schließen",
      zurueckZurListe: "Zurück zur Liste",
      // Der Umschalter zwischen den beiden Einhängepunkten. Er steht neben dem
      // Schließen-Knopf und nicht an seiner Stelle: „diese Nachricht anders
      // zeigen" ist etwas anderes als „diese Nachricht schließen".
      ansichtOhneListe: "Ohne Liste anzeigen",
      ansichtNebenListe: "Neben der Liste anzeigen",
      // Der Absprung in den Prozessbaum (E-103), im Panel und nicht als
      // Kontextmenü: Tastatur- und Berührungserreichbarkeit.
      imProzessbaum: "Im Prozessbaum anzeigen",
      zeitpunkt: "Zuletzt geändert",
      start: "Beginn",
      // Die Gesamtdauer steht im Kopf neben Beginn und Zeitpunkt. Sie ist die
      // Abdeckung für Zeit, die zwischen zwei Schritten steckt und in keiner
      // Schrittdauer auftaucht — siehe nachrichtendetail.md §3a.
      gesamtdauer: "Gesamtdauer",
      projekt: "Projekt",
      prozess: "Prozess",
      // Die MessageID kehrt hier zurück, nachdem sie aus der Liste geflogen ist:
      // Sie ist Beiwerk nach dem Leitsatz, aber sie ist das, was jemand in eine
      // E-Mail an die EDI-Betreuung schreibt. Deshalb klein und mit Kopierknopf.
      kennung: "Nachrichten-Kennung",
      kennungKopieren: "Kennung kopieren",
      kennungKopiert: "Kopiert",

      ablaufTitel: "Was passiert ist",
      // Ohne Prozessschritt, aber abgeschlossen: kein Fehler, nur nichts zu
      // zeigen. Der Metadaten-Schritt zählt nicht als Prozessschritt.
      keineSchritte: "Für diese Nachricht ist kein Prozessschritt aufgezeichnet.",
      // EMPFANGEN: angekommen und seitdem nicht weitergelaufen — eine Auskunft
      // über die Plattform. Der Zeitpunkt ist der Metadaten-Schritt; er ist kein
      // Verarbeitungsschritt, aber er IST das Ereignis mit einem Zeitpunkt, und
      // deshalb steht er hier statt eines vagen „ist offen".
      empfangen: "Empfangen am {zeitpunkt} — seitdem ist kein Schritt ausgeführt worden.",
      // Derselbe Satz ohne Datum. MessageActionStart ist auf keiner der 10,3
      // Millionen Zeilen leer (M22), die Spalte lässt es aber zu — und ein
      // Platzhalter im Satz wäre schlechter als der Satz ohne ihn.
      empfangenOhneZeitpunkt:
        "Die Nachricht ist empfangen worden — seitdem ist kein Schritt ausgeführt worden.",
      // OHNE_AKTION: eine Auskunft über die Datenlage, nicht über die Plattform.
      // Deshalb ein eigener Satz und nicht derselbe wie oben.
      ohneAktion: "Zu dieser Nachricht ist kein Ablauf protokolliert.",
      ohneDauer: "keine Dauer aufgezeichnet",
      laeuftGerade: "läuft gerade",
      nochNichtBegonnen: "noch nicht begonnen",
      // Die Wartezeile am offenen Zustand. Sie ersetzt die Lückenzeile zwischen
      // zwei Schritten, die über rund 700 geprüfte Nachrichten nie erschienen
      // ist — die Wartezeit steckt in der Dauer des WAITUNTIL-Schritts, nicht
      // zwischen zwei Schritten (nachrichtendetail.md §10.12).
      wartetSeit: "wartet seit {dauer}",
      laeuftSeit: "läuft seit {dauer}",
      frist: "Frist {dauer}",
      // WARTET_VOR ohne nächsten Schritt: Das wird benannt und nicht
      // weggelassen. Die Nachricht wartet, wir wissen nur nicht worauf.
      wartetVorUnbekannt:
        "Die Nachricht wartet — worauf, ist in der Ablaufdefinition nicht hinterlegt.",
      // WARTET_IN — der gemessene Normalfall (M29, 538 von 538): Der Verweis
      // zeigt auf den Schritt, der gerade gelaufen ist, den SUSPEND-Schritt
      // selbst. Sein Name steht eine Zeile darüber; ihn zu wiederholen ergäbe
      // „noch nicht begonnen" unter „2 min" — ein Widerspruch für jeden, der
      // kein EDI-Spezialist ist. Der Verweis bleibt im Tooltip nachlesbar.
      wartetWeiterhin: "Die Nachricht wartet — von selbst geht es hier nicht weiter.",
      verweistAuf: "Der Ablauf verweist auf: {schritt}",
      baustein: "Baustein",
      // Die Herkunft steht ausschließlich im Tooltip. Wer „Send File by FTP"
      // liest, soll nicht mit der Frage belastet werden, wie wir darauf gekommen
      // sind; wer nachsehen will, findet es.
      herkunft: {
        DIREKT: "Name aus der Ablaufdefinition",
        HERGELEITET: "Name über den Baustein aus dem Ablauf hergeleitet",
        ROHWERT: "Im Ablauf ist kein Name hinterlegt — angezeigt wird der Baustein",
      },

      // Die Beschriftung der kuratierten Felder ist eine Übersetzung und lebt
      // deshalb hier. Der Schlüssel ist der Rohname aus MessagePropertyName.
      // Ein kuratiertes Feld ohne Eintrag erscheint mit seinem Rohnamen,
      // sichtbar unfertig — besser als lautlos zu fehlen.
      kuratiert: {
        "Message.SendingPartner": "Absender",
        "Message.SplitCount": "Aufteilungszahl",
      },

      // Die Belegdaten (Schritt 7, Teil 1) — nach dem Leitsatz die
      // Hauptinformation dieser Ansicht und deshalb vor der Zeitleiste. Der
      // Block erscheint gar nicht, wenn `bamAnzahl` null ist; bei 80,6 Prozent
      // der Nachrichten ist das der Fall (M41).
      bam: {
        // Die Zahl steht in der Überschrift, damit erkennbar ist, ob sich das
        // Aufklappen lohnt — sie kommt aus dem Kopf und kostet keine Anfrage.
        titel: "Belegdaten ({anzahl})",
        aufklappen: "Belegdaten anzeigen",
        zuklappen: "Belegdaten ausblenden",
        // Die ehrliche Restangabe. Kein „mehr laden": Das bräuchte einen Cursor
        // und kommt erst, wenn jemand es braucht.
        weitere: "und {anzahl} weitere",
        // Kann nur eintreten, wenn sich der Bestand zwischen Kopf und Block
        // ändert. Ein Block, der dann nichts sagt, sähe nach einem Fehler aus.
        leer: "Zu dieser Nachricht ist keine Belegnummer hinterlegt.",
      },

      eigenschaften: {
        titel: "Technische Eigenschaften ({anzahl})",
        keine: "Keine technischen Eigenschaften",
        aufklappen: "Technische Eigenschaften anzeigen",
        zuklappen: "Technische Eigenschaften ausblenden",
        name: "Name",
        wert: "Wert",
        // Ein stillschweigend abgeschnittener Wert ist schlimmer als ein
        // sichtbar abgeschnittener: Sonst liest jemand eine halbe Belegnummer
        // als ganze.
        gekappt: "gekürzt",
        gekapptHinweis: "Gekürzt — im Original {bytes} Bytes.",
        leer: "Zu dieser Nachricht ist keine Eigenschaft hinterlegt.",
        // Der Kopf einer Gruppe (Nacharbeit vom 17.08.2026): Schrittname und
        // Anzahl. Die Zahl steht in der Sprachdatei und nicht im JSX, weil
        // Klammern und Wortstellung Sache der Übersetzung sind.
        gruppe: "{name} ({anzahl})",
        // Die Gruppe zu `position === 0`. Sie sagt, **wo** die Werte hängen — an
        // der Nachricht statt an einem Schritt — und behauptet nichts über ihren
        // Inhalt. „Metadaten der Nachricht" oder „Allgemeine Angaben" wären
        // genau das: Dass dort *ausschließlich* die `Message.*`-Familie steht,
        // ist nicht gemessen. Gemessen ist die Gegenrichtung — die
        // `Message.*`-Familie steht ausnahmslos dort (M17 3).
        gruppeNachricht: "Nachricht",
        // Eine Position, zu der kein Schritt geliefert wurde. Kein erfundener
        // Name: Die Nummer ist das einzige, was über sie bekannt ist.
        gruppeSchritt: "Schritt {nummer}",
      },

      // Rohdaten und Protokolle (Schritt 8, Teil Frontend) — die Dateien, die an
      // einer Nachricht hängen: die eingegangene Datei, die umgewandelten
      // Fassungen und die Protokolle der Schritte (docs/rohdaten.md).
      dateien: {
        // Die Beschriftung der Zeile über der Zeitleiste. Auf Schritt 0 liegt
        // das Paar des Lesedienstes, Datei und Protokoll (M57). Schritt 0 ist
        // der Ort der Metadaten und kein Ablaufschritt (M57, M17 3) — er kommt
        // in schritte[] nicht vor und steht deshalb in keiner Zeile der
        // Zeitleiste.
        //
        // Bis zum 19.08.2026 stand hier zusätzlich `eingangTitel`
        // („Eingegangene Datei") für Message.Payload.GUID. Der Name trägt nach
        // M73 den Verweis der Nutzdatenzeile mit dem höchsten MessageActionID
        // derselben Nachricht; das Artefakt ist entfallen, der Text mit ihm.
        //
        // Belegvermerk (L10): Gemessen ist, WELCHE Namen auf Schritt 0 liegen
        // (M57). Dass das Paar des Lesedienstes den Eingang der Nachricht
        // bezeichnet, ist eine Sichtprüfung des Auftraggebers an EINER Nachricht
        // vom 19.08.2026 und keine Messung.
        eingang: "Eingang",
        eingangHinweis:
          "Was beim Eingang der Nachricht abgelegt worden ist. Der Eingang ist kein Schritt des Ablaufs und steht deshalb nicht in der Zeitleiste.",
        // Der Rückfall für die 55,98 Prozent der Artefakte, deren Schritt zu
        // keinem Namen auflöst (M57). Die Familie ist ein technischer Wert aus
        // dem MessagePropertyName und wird nicht übersetzt, ergänzt oder
        // gedeutet (Regel Q4) — „Converter" bleibt „Converter".
        schrittFamilie: "Schritt {nummer} · {familie}",
        // Artefakte, deren Schritt in der Zeitleiste keine Zeile hat. Gemessen
        // kommt das nicht vor (M57, Befund 1: ohne_schrittzeile = 0) — ohne
        // diese Zeile fiele ein solches Artefakt aber lautlos aus der
        // Oberfläche.
        ohneZeile: "Ohne Schritt in der Zeitleiste",
        // Die Artefaktliste konnte nicht geladen werden. Die Zeitleiste steht
        // trotzdem: Sie hängt an einem anderen Endpunkt.
        zieleFehlgeschlagen: "Die Dateien zu dieser Nachricht konnten nicht geladen werden.",

        // Die Ziele an Zeitleiste und Eingangszeile. Sichtbar ist nur das
        // Zeichen; das hier ist der Name für Vorleseprogramme.
        ziel: "{art} · {name}",
        zielAusschnitt: "{ziel} — {marke}",
        // Der Weg von einer Zeile der Zeitleiste zu ihrer Gruppe im
        // Eigenschaftenblock. Der sichtbare Schrittname steht darin, wie es
        // WCAG 2.5.3 für den zugänglichen Namen verlangt.
        zuEigenschaften: "Technische Eigenschaften zu {name} anzeigen",

        // Die Ankündigung am Ziel. Sie steht dort, damit der Nutzer es vor dem
        // Öffnen weiß, statt beim Öffnen überrascht zu werden.
        ausschnittMarke: "Ausschnitt",
        ausschnittAnkuendigung:
          "Von diesem Protokoll wird dir der freigegebene Abschnitt gezeigt, nicht die vollständige Datei.",

        // ── Die Ansicht auf ihrer eigenen Route ──────────────────────────────
        // Der Rückfall der Überschrift, solange die Artefaktliste noch lädt und
        // die Beschriftung deshalb fehlt.
        ansichtTitel: "Datei",
        zurueck: "Zurück zur Nachricht",
        art: {
          NUTZDATEN: "Nutzdaten",
          PROTOKOLL: "Protokoll",
        },
        // Rohe Bytes, ohne Umrechnung in KB oder MB. Das größte gemessene
        // Artefakt hat 609.995 Byte (M60); eine gerundete Angabe verlöre genau
        // die Genauigkeit, mit der jemand zwei Fassungen vergleicht.
        groesse: "{bytes} Bytes",
        kodierung: "Kodierung {name}",
        herunterladen: "Herunterladen",
        // Der Inhalt selbst — als beschriftetes Feld für Vorleseprogramme.
        inhalt: "Dateiinhalt",

        // Die Vermerke. Jeder sagt, dass hier nicht die ganze Datei steht — und
        // jeder sagt es aus einem anderen Grund.
        vermerkAusschnitt:
          "Du siehst den freigegebenen Ausschnitt dieses Protokolls. Der Download enthält denselben Ausschnitt.",
        vermerkGekappt: "Die Anzeige endet an der Längengrenze. Der Download ist nicht gekappt.",
        // In 693 geholten Dateien nie vorgekommen. Er wird trotzdem gezeigt: Das
        // Altsystem verwirft den Rest stillschweigend (docs/rohdaten.md §4).
        vermerkMehrereEintraege: "Das Archiv enthielt {anzahl} Einträge. Angezeigt wird der erste.",

        // Die vier benannten Zustände aus docs/rohdaten.md §8. KEINER davon ist
        // ein leeres Feld — genau das macht das Altsystem, und genau das ist der
        // Unterschied. „Datei nicht vorhanden" und „Ablage nicht erreichbar"
        // verschmelzen ausdrücklich NICHT zu „Fehler beim Laden": Für den
        // Betrieb ist diese Unterscheidung die wichtigere.
        binaerTitel: "Binärdatei",
        binaerText:
          "Diese Datei besteht nicht aus lesbarem Text und wird deshalb nicht angezeigt. Sie ist {bytes} Bytes groß.",
        // Der häufigste der vier: FTPSender trägt in 28 von 30 Fällen keine
        // Marken und hängt an rund 69 Prozent der Nachrichten, HTTPSender in 30
        // von 30 (M63). Für MANDANT ist das der Normalfall — der Text muss das
        // aushalten, ohne wie ein Defekt zu klingen.
        keinProtokollteilTitel: "Kein anzeigbarer Protokollteil",
        keinProtokollteilText:
          "Dieses Protokoll enthält keinen Abschnitt, der dir gezeigt wird. Bei vielen Schritten ist das der Normalfall und bedeutet nicht, dass etwas fehlgeschlagen ist.",
        nichtVorhandenTitel: "Datei nicht vorhanden",
        nichtVorhandenText:
          "Die Dateiablage hat geantwortet und zu diesem Eintrag keine Datei geliefert. Möglicherweise ist ihre Aufbewahrungsfrist abgelaufen.",
        // Etwas anderes als „Datei weg", und für den Betrieb die wichtigere
        // Unterscheidung. Als einziger der vier lohnt hier ein zweiter Versuch —
        // deshalb steht die Schaltfläche nur an diesem Zustand.
        ablageTitel: "Ablage nicht erreichbar",
        ablageText:
          "Die Dateiablage antwortet gerade nicht. Die Datei kann es weiterhin geben — versuche es in einigen Minuten erneut.",
        // Eine Datei mit null Byte. Nicht gemessen — das kleinste beobachtete
        // Artefakt hat 2 Byte (M60) —, aber möglich. Ohne diesen Satz stünde
        // dort ein leerer Kasten, und genau den schließt §8 aus.
        leerTitel: "Leere Datei",
        leerText: "Diese Datei enthält kein einziges Zeichen.",
      },

      // Für eine unbekannte und für eine fremde Kennung derselbe Text. Das
      // Backend macht „gibt es nicht" und „gehört einem anderen Mandanten"
      // absichtlich ununterscheidbar; ein Wort über Berechtigungen gäbe genau
      // das preis, was die 404-Regel schützt. Genannt wird stattdessen der
      // Mandant in der Kopfzeile — für beide Fälle wahr.
      nichtGefunden:
        "Unter dem Mandanten in der Kopfzeile gibt es diese Nachricht nicht. Stammt der Link von jemand anderem, prüfe zuerst den Mandanten dort oben.",

      dauer: {
        unterSekunde: "< 1 s",
        sekunden: "{wert} s",
        minuten: "{wert} min",
        stunden: "{wert} h",
        tage: "{wert} d",
      },
    },

    // Die Kette im Detailpanel (Schritt 6, Teil 2b) — „was hängt an dieser
    // Nachricht". Die beiden Überschriften folgen dem **Datenfluss** und nicht
    // der Richtung der API: Beim Merge liegt das Ergebnis in `aufwaerts` und
    // die Eingänge in `abwaerts`, im Fluss ist es umgekehrt
    // (`docs/verkettung.md`).
    kette: {
      // Die Zahl steht nur dort, wo der Endpunkt eine liefert — für den Abstieg
      // (`abwaertsGesamt`, gemessen genau, M30‑1). Der Aufstieg hat keine:
      // Seine Länge ist die der Liste, und die ist bei `tiefeErreicht` gerade
      // nicht die Gesamtzahl.
      kommtVon: {
        titel: "Kommt von",
        titelEins: "Kommt von — 1 Eingang",
        titelZahl: "Kommt von — {anzahl} Eingänge",
      },
      wurdeZu: {
        titel: "Wurde zu",
        titelEins: "Wurde zu — 1 Teil",
        titelZahl: "Wurde zu — {anzahl} Teile",
      },
      // Steht im Tooltip jeder Zeile. Der Unterschied zwischen „aufgeteilt" und
      // „zusammengeführt" ist das, was der Nutzer verstehen soll — er kommt
      // fertig aus dem Backend und wird hier nicht zusammengerechnet.
      beziehung: {
        AUFTEILUNG: "Aufteilung",
        ZUSAMMENFUEHRUNG: "Zusammenführung",
      },
      // Eingerückt wird nicht — das Panel ist 26 rem breit, und eine Einrückung
      // je Ebene fräße die Breite der Ablaufnamen. Die Reihenfolge trägt die
      // Ebene; ab der zweiten Stufe steht sie zusätzlich als Beiwerk daneben.
      stufe: "Stufe {stufe}",
      gliedOeffnen: "{ablauf} öffnen",
      weitereLaden: "Weitere laden",
      laedtWeitere: "Wird geladen …",
      geladen: "{anzahl} Glieder geladen",
      // Eine Kette, die stillschweigend abbricht, ist schlimmer als eine, die
      // sagt, dass sie abbricht. Zwei Sätze und nicht einer: „tief" beschreibt,
      // wo abgebrochen wurde, „im Kreis" warum.
      tiefeErreicht: "Die Kette ist länger als hier gezeigt.",
      zyklusErkannt: "Die Kette führt im Kreis — hier ist sie abgebrochen.",
    },
  },

  /**
   * Die Prozessansicht (`docs/process-view.md`, Schritt 10c‑2).
   *
   * **Der Baum links, die Übertragungen rechts.** Die Beschriftungen der
   * Nachrichtenliste stehen weiterhin unter `nachrichten` — sie ist dieselbe
   * Liste, und ein zweiter Satz Spaltenüberschriften wäre der Anfang zweier
   * Listen.
   */
  prozesse: {
    /** Regel Q4: Was der Nutzer anstelle einer fehlenden Zuordnung liest, ist eine
     * Oberflächenentscheidung und gehört hierher, nicht in eine Abfrage. */
    nichtZugeordnet: "nicht zugeordnet",

    /**
     * Ein Prozess **ohne Namen** — und ausdrücklich nicht „nicht zugeordnet".
     *
     * Die beiden Wörter stehen im selben Baum eine Ebene höher für etwas
     * anderes: Beim Partner fehlt die **Zuordnung**, hier fehlt der **Name**.
     * Derselbe Text für beides wäre auf dem Bildschirm nicht zu unterscheiden.
     *
     * Steht auf oberster Ebene dieses Blocks, weil ihn **Baum und Überschrift**
     * benutzen — dieselbe Tatsache soll links und rechts gleich heißen.
     */
    ohneNamen: "Prozess ohne Namen",

    /**
     * Die Richtung. **`nichtErmittelt` ist ein eigenes Wort und keine leere
     * Stelle** — bei `VOTG` trägt keine einzige der 390 Katalogzeilen eine
     * Richtung, und eine Ansicht, die dort nichts hinschriebe, behauptete, es
     * gäbe keine.
     *
     * Ein gepflegter, aber unbekannter Wert steht hier **nicht** und wird auch
     * nicht geraten: Er erscheint, wie er im Katalog steht.
     */
    richtung: {
      EINGEHEND: "Eingehend",
      AUSGEHEND: "Ausgehend",
      nichtErmittelt: "nicht ermittelt",
    },

    baum: {
      bezeichnung: "Prozessbaum",
      eingrenzung: "Partner oder Prozess eingrenzen",
      eingrenzungLeeren: "Eingrenzung aufheben",
      keineTreffer: "Kein Partner und kein Prozess passt dazu.",
      leerTitel: "Kein Prozess",
      leerHinweis:
        "Für diesen Mandanten steht in der Quelle kein Prozess. Ohne Prozess gibt es auch keine " +
        "Übertragung — das ist keine Einstellung dieser Ansicht.",
      // Vorgabe aus (Entscheidung E‑48). Mit Vorgabe *an* wäre genau der Prozess
      // unauffindbar, den jemand sucht, weil er nichts trägt.
      nurMitVerkehr: "Nur mit Verkehr im Zeitraum",
      nurMitVerkehrHinweis:
        "Blendet Prozesse aus, über die im gewählten Zeitraum nichts gelaufen ist. Wer wissen " +
        "will, warum nichts ankommt, lässt den Schalter aus.",
      // Die drei Zustände nebeneinander — sie sind disjunkt und vollständig,
      // ihre Summe ist die Prozesszahl (`docs/process-view.md` §4).
      verteilung: "{prozesse} Prozesse — {bewegt} bewegt, {still} still, {nie} noch nie",
      gezeigt: "{sichtbar} von {gesamt} gezeigt",
      // Die Ebenennamen stehen nur im vorgelesenen Namen einer Zeile, nicht auf
      // dem Bildschirm: Dort sagt die Einrückung, was eine Zeile ist.
      ebenePartner: "Partner",
      ebeneRichtung: "Richtung",
      ebeneProzess: "Prozess",
      // „Prozesse: 12" statt „12 Prozesse": Diese Anwendung kennt keine
      // Pluralregeln, und „1 Prozesse" wäre der Preis dafür.
      anzahlProzesse: "Prozesse: {anzahl}",
      anzahlNachrichten: "Nachrichten: {anzahl}",
      anzahlFehler: "Fehler: {anzahl}",
      // Die Zahl kommt aus der Antwort (`stilleSchwelleMonate`, Entscheidung
      // E‑37) — die Oberfläche beschriftet damit und rechnet nichts nach.
      //
      // **Ein Gegenstück `nie` gibt es seit dem 02.09.2026 nicht mehr** (E‑56):
      // „noch nie" ist eine Katalogfrage und kein Vorfall; in der Zeile steht
      // dafür weder ein Wort noch eine Dämpfung. Die Zahl bleibt — sie steht in
      // `verteilung` und im Schalter darunter.
      still: "seit über {monate} Monaten nichts",
    },

    liste: {
      // Vor der Wahl eines Prozesses steht rechts **nicht** die ganze Liste des
      // Mandanten. Dafür gibt es die Nachrichtenliste, und dorthin führt der Weg.
      leerTitel: "Kein Prozess gewählt",
      leerHinweis:
        "Wähle links einen Prozess. Rechts stehen dann seine Übertragungen im gewählten Zeitraum.",
      zurNachrichtenliste: "Alle Übertragungen in der Nachrichtenliste",
      zurueckZumBaum: "Zurück zum Baum",
      // `undefined` heißt „nicht gefunden", `null` heißt „ohne Namen" — die
      // beiden fallen ausdrücklich nicht zusammen (Regel Q4). Ein geteilter
      // Link kann eine Kennung eines anderen Mandanten tragen; über deren
      // Namen ist dann gar nichts bekannt.
      nichtGefunden: "Zu dieser Kennung steht in diesem Mandanten kein Prozess",
      leerImZeitraumTitel: "Nichts im Zeitraum",
      leerImZeitraum:
        "Über diesen Prozess ist im gewählten Zeitraum nichts gelaufen. Ein größerer Zeitraum " +
        "zeigt mehr — bleibt es dabei, hat der Prozess nichts getragen.",
      fenster: "Zeitraum {von} bis {bis}",
    },
  },

  /**
   * Die Belegsuche (Schritt 7, Teil 3) — **auf oberster Ebene, nicht unter
   * `nachrichten`.**
   *
   * Ihr Feld steht in der Kopfzeile und damit auf jeder Seite; ihre Route ist eine
   * eigene. Unter `nachrichten` stünde sie neben dem Freitextfilter der Liste,
   * und genau diese Verwechslung soll sie nicht einladen.
   */
  suche: {
    titel: "Belegsuche",

    feld: {
      // Beschriftung und Platzhalter sagen dasselbe, und das ist Absicht: Der
      // Platz in der Kopfzeile trägt keine sichtbare Beschriftung, der
      // Platzhalter übernimmt sie — und beide müssen benennen, **worin** gesucht
      // wird (siehe `nachrichten.suche.bezeichnung`).
      bezeichnung: "Belegnummer suchen",
      platzhalter: "Belegnummer suchen",
      // Ist ein Feld gewählt, wäre „Belegnummer" eine falsche Auskunft: Der
      // Wert erreicht dann eine Spalte oder eine Eigenschaft, nie MessageBAM.
      platzhalterFeld: "Wert suchen",
      hinzufuegen: "Begriff hinzufügen",
    },

    // Seit Teil 2 der Property-Suche zwei Gruppen in einem Menü (E-99), seit
    // dem 09.09.2026 als zwei Untermenüs (E-112). Die Feldnamen selbst bleiben
    // technisch und unübersetzt (E-105); übersetzt sind nur die beiden
    // Untermenü-Auslöser und die Beschriftung des Schalters. „Technische
    // Eigenschaften" ist die Beschriftung des Blocks im Nachrichtendetail
    // (E-113) — sie benennt eine Art und keinen Speicherort: Acht der Einträge
    // sind Spalten (Typ 0) und erscheinen in jenem Block nie.
    typwahl: {
      alle: "Alle Belegarten",
      gewaehlt: "Belegart: {belegart}",
      gewaehltesFeld: "Eigenschaft: {feld}",
      gruppeBelegarten: "Belegarten",
      gruppeFelder: "Technische Eigenschaften",
    },

    marken: {
      bezeichnung: "Gesuchte Begriffe",
      entfernen: "Begriff entfernen",
      // Die Zahl kommt aus dem Code, damit sie nicht zweimal gepflegt wird. Der
      // Satz nennt sie als das, was sie ist — ein Schutzgeländer und keine
      // fachliche Grenze (docs/bam-suche.md §1).
      grenzeErreicht:
        "Mehr als {anzahl} Begriffe nimmt die Suche nicht an — ein Schutzgeländer, keine fachliche Grenze. Entferne einen, um einen anderen zu suchen.",
    },

    spalten: {
      treffer: "Treffer",
      kette: "Kette",
    },

    treffer: {
      // Der erste Typ und die Zahl der übrigen. Mehrere sind kein Randfall: Bei
      // 4,17 Prozent der Paare steht derselbe Wert unter mehreren Typen (M37).
      weitere: "{erste} +{anzahl}",
      alleTypen: "Getroffen als: {typen}",
    },

    // Die Stellung in der Verkettung. Sie ist hier wichtiger als in der Liste:
    // Die Suche findet fast immer die Wurzel (96,87 Prozent der Wurzeln tragen
    // BAM-Werte gegen 2,42 Prozent der Kinder, M26‑1b), und die trägt bei einer
    // Aufteilung einen Endstatus, der die Frage „ist der Beleg angekommen" nicht
    // beantwortet. Kurz in der Zelle, als Satz im Tooltip — auf einem Touchgerät
    // gibt es keinen Hover.
    kette: {
      kurz: {
        SPLIT_WURZEL: "Aufgeteilt",
        SPLIT_KIND: "Teil",
        MERGE_EINGANG: "Eingang",
        MERGE_ERGEBNIS: "Ergebnis",
      },
      satz: {
        SPLIT_WURZEL: "Diese Nachricht wurde aufgeteilt — die Teile laufen einzeln weiter.",
        SPLIT_KIND: "Diese Nachricht ist ein Teil einer Aufteilung.",
        MERGE_EINGANG: "Diese Nachricht ist in eine Zusammenführung eingegangen.",
        MERGE_ERGEBNIS: "Diese Nachricht ist aus einer Zusammenführung entstanden.",
      },
    },

    // Keine stille Korrektur: Wer 4711815 tippt und 004711815 findet, muss
    // erfahren, warum. Die Fassung mit führendem Leerzeichen steht bewusst nicht
    // darin — sie wäre für den Nutzer nicht nachvollziehbar (docs/bam-suche.md §3).
    varianten: "Gesucht nach {eingabe} und {fassungen}.",

    fenster: {
      aendern: "Zeitfenster ändern",
      einJahr: "Auf ein Jahr erweitern",
      // Bewusst ohne Zahl: Die Vorgabe steht im Backend, und ein zweiter Wert
      // hier liefe dem ersten irgendwann hinterher.
      vorgabe: "Vorgabe wiederherstellen",
      gilt: "Es gilt das Zeitfenster {von} bis {bis}.",
    },

    ergebnis: {
      anzahl: "{anzahl} Treffer im Zeitfenster {von} bis {bis}.",
      // **Abschneidung und Fenster zusammen.** Nur eines von beidem ist
      // irreführend: „mehr als 50" ohne Fenster liest sich wie eine Aussage über
      // den ganzen Bestand.
      abgeschnitten:
        "Mehr als {anzahl} Treffer — gezeigt werden die {anzahl} neuesten im Zeitfenster {von} bis {bis}. Verkleinere den Zeitraum oder nenne eine zweite Belegnummer.",
      // Derselbe Befund, ein anderer Rat. Im Präfixmodus ist die Trefferzahl die
      // Kostengröße (E6), und mehr Zeichen senken sie — der Zeitraum ist hier
      // schon auf 30 Tage gedeckelt, dort ist weniger zu holen. Deshalb steht der
      // Rat zum Zeitraum daneben und nicht vorn.
      abgeschnittenPraefix:
        "Mehr als {anzahl} Treffer — gezeigt werden die {anzahl} neuesten im Zeitfenster {von} bis {bis}. Tippe mehr Zeichen der Nummer, das grenzt am stärksten ein. Ein kleinerer Zeitraum hilft hier weniger.",
      keine: "Keine Nachricht mit diesem Beleg",
      keineHinweis:
        "Erweitere das Zeitfenster oder entferne einen Begriff. Eine führende Null musst du nicht tippen — die ergänzt die Suche selbst.",
      // Jede Marke verengt. Landet die dritte bei null, sieht der Nutzer sonst
      // nicht, welche es war.
      nulltreffer: "Mit diesem Begriff: 0. Ohne ihn: {anzahl}.",
      // War die vorige Runde abgeschnitten, ist die gelieferte Zahl die
      // Seitengröße und nicht die Trefferzahl. Sie als solche auszugeben wäre ein
      // falscher Schluss in genau der Zeile, die vor einem bewahren soll.
      nulltrefferAbgeschnitten: "Mit diesem Begriff: 0. Ohne ihn: mehr als {anzahl}.",
    },

    leer: {
      titel: "Wonach suchst du?",
      was: "Tippe eine Belegnummer in das Feld oben — Lieferschein-, Bestell- oder Transportnummer, Charge, Werk oder Materialnummer. Die Eingabetaste startet die Suche.",
      belegarten: "Für diesen Mandanten sind diese Belegarten hinterlegt:",
      // Die Feldnamen stehen darunter unverändert (E-105). Der Satz nennt die
      // Weiche aus E-100: Ohne gewählte Eigenschaft wird nie eine durchsucht.
      // Seit dem 09.09.2026 heißt die Gruppe „Technische Eigenschaften" (E-113).
      felder:
        "Dazu diese technischen Eigenschaften, mit ihrem Namen unverändert aus dem System. Wähle die Eigenschaft neben dem Suchfeld — ein Wert ohne gewählte Eigenschaft sucht immer eine Belegnummer:",
      hilfe:
        "Führende Nullen und ein führendes Leerzeichen sucht die Suche von selbst mit — sie stehen auf dem Beleg nicht, im Bestand aber sehr wohl. Mehrere Begriffe werden mit UND verknüpft.",
      // Der einzige Ort, an dem die Belegart erwähnt wird (docs/bam-suche.md
      // §24). Kein Zwang und keine Belehrung: Das Angebot erscheint mit und ohne
      // gewählte Belegart. Der Satz erscheint nur, wenn dieser Mandant überhaupt
      // Belegarten hat — sonst nennte er ein Bedienelement, das es nicht gibt.
      belegartHilfe:
        "Suchst du nur den Anfang einer Nummer, findet eine gewählte Belegart zusätzlich über führende Nullen hinweg. Ohne Belegart geht das nicht.",
      // Der Zeitraum ist im Präfixmodus auf 30 Tage gedeckelt; wer weiter zurück
      // sucht, schiebt das Fenster über die vorhandenen Zeitraum-Bedienelemente
      // dorthin. Der Satz steht hier, weil er sonst nirgends stünde.
      zeitraumHilfe:
        "Die Suche über den Anfang einer Nummer gilt jeweils für dreißig Tage. Wer weiter zurück sucht, verschiebt den Zeitraum über der Trefferliste.",
    },

    // Schritt 7, Teil 4 — der Rückfall auf die Suche über den Anfang der Nummer.
    // **Kein Fachwort im Text**: „Präfix" hilft dem Nutzer nicht, der kein
    // EDI-Spezialist ist. Benannt wird, was passiert, nicht wie es heißt.
    praefix: {
      angebot: "Soll nach Nummern gesucht werden, die damit anfangen?",
      // Die Erwartung gehört dazu, weil das Ergebnis anders aussehen wird.
      angebotErwartung: "Dabei erscheinen auch längere Nummern, die mit der eingegebenen beginnen.",
      // Eine Beschriftung und kein bloßes Symbol: Der Knopf ist selten sichtbar
      // und muss beim ersten Mal verständlich sein.
      angebotKnopf: "Nach dem Anfang der Nummer suchen",
      // Steht einmal über der Liste und nicht an jeder Marke — der Modus gilt für
      // die ganze Suche und nicht je Begriff.
      laeuft: "Gesucht wird nach Nummern, die mit dem Eingegebenen anfangen.",
      zurueck: "Wieder genau suchen",
      // Beide Datumsangaben, und der Satz, ohne den „nicht gefunden" als „nicht
      // vorhanden" gelesen wird. Der Zeitraum kommt aus der Antwort.
      zeitraum:
        "Durchsucht wurde der Zeitraum {von} bis {bis} — ältere Nachrichten sind dabei nicht erfasst.",
      // Die Oberfläche löst das nie selbst aus; der Fall kommt aus einer von Hand
      // gebauten URL. Beide Zahlen stammen aus der Fehlerantwort und nicht aus
      // diesem Text — die Grenze gehört dorthin, wo sie gemessen wurde.
      fensterZuGross:
        "Die Suche über den Anfang einer Nummer gilt für höchstens {grenze} Tage; gewählt sind {angefragt}.",
      fensterVerkleinern: "Zeitraum auf {grenze} Tage verkleinern",
    },

    // Derselbe Problemtyp wie in der Nachrichtenliste, aber der andere
    // Handlungshinweis: Ein BAM-Wert lässt sich nicht schärfen, ein zweiter
    // Begriff senkt die Laufzeit dagegen um Größenordnungen (M42‑1). Welcher der
    // beiden Sätze erscheint, entscheidet die Ansicht und nicht der Fehlerkatalog.
    abgebrochen:
      "Die Suche hat zu lange gedauert und wurde abgebrochen. Verkleinere den Zeitraum oder nenne eine zweite Belegnummer.",
    // Mit Feldbegriffen hilft der Zeitraum — für Status, Ablauf-Kennung und
    // Ablaufname ist der Abbruch über ein Jahr der Regelfall (docs/property-suche.md
    // §6.4). Eine zweite Belegnummer wäre dort eine Antwort auf eine andere Frage.
    abgebrochenMitFeld:
      "Die Suche hat zu lange gedauert und wurde abgebrochen. Verkleinere den Zeitraum — bei Feldern wie Status oder Ablaufname ist das über ein Jahr der Regelfall.",
  },

  /**
   * Die Katalogpflege — **auf oberster Ebene, nicht unter `administration`.**
   *
   * Dort steht die Möblierung des Bereichs: Unternavigation, Übersicht, die
   * Namen der beiden Seiten. Hier steht das Feature selbst, wie `nachrichten`
   * und `suche` daneben. Der **Name** der Seite kommt weiterhin aus
   * `administration.bereiche.katalog.titel` — es gibt genau einen Wortlaut
   * dafür, und er steht dort, wo auch die Navigation ihn liest.
   */
  katalog: {
    spalten: {
      prozess: "Prozess",
      projekt: "Projekt",
      partner: "Partner",
      richtung: "Richtung",
      bestand: "Nachrichten",
      pflege: "Pflege",
    },
    tabelle: "Prozesse des aktiven Mandanten",
    nichtZugeordnet: "nicht zugeordnet",
    ohneNamen: "ohne Namen",
    auffangprozess: "Auffangprozess",
    auffangprozessHinweis:
      "Hier landet, was keinem Prozess zugeordnet werden konnte. Er wird gepflegt und gezählt wie jede andere Zeile.",
    richtungen: {
      EINGEHEND: "Eingehend",
      AUSGEHEND: "Ausgehend",
    },
    pflegestatus: {
      OFFEN: "offen",
      GEPFLEGT: "gepflegt",
    },
    /**
     * Die Vermerke unter dem Partner — sie machen die drei Bedeutungen aus E4
     * sichtbar, die aus zwei Feldern entstehen.
     *
     * `keinVorschlag` ist der Hinweis an der **einzelnen Zeile** aus E9. Über
     * der Liste steht ein anderer Satz (E17); beide meinen den fehlenden
     * Partner, aber der eine spricht von dieser Zeile und der andere vom
     * ganzen Mandanten.
     */
    vermerk: {
      vorschlag: "Vorschlag",
      keinVorschlag: "kein Vorschlag ableitbar",
      ohnePartner: "hingesehen, es gibt keinen",
    },
    /**
     * Die drei Zustände von `traegtNachrichten` (E14) — **drei Wörter, keine
     * Farbe.** `null` ist ausdrücklich nicht dasselbe wie `false`.
     */
    bestand: {
      traegt: "trägt Nachrichten",
      traegtNicht: "trägt keine Nachrichten",
      ungeprueft: "nicht geprüft",
      geprueftAm: "Bestand geprüft am {zeitpunkt}",
      nieGeprueft: "Für diese Zeile hat noch kein Bestandslauf stattgefunden.",
    },
    filter: {
      bezeichnung: "Filter",
      nurOffene: "nur offene",
      nurOffeneHinweis: "Blendet die bereits gepflegten Zeilen aus.",
      nurMitNachrichten: "nur mit Nachrichten",
      nurMitNachrichtenHinweis: "Ungeprüfte Zeilen bleiben sichtbar.",
      gesperrt: "Während eine Zeile bearbeitet wird, bleibt die Liste stehen.",
    },
    /**
     * Die Bearbeitung in der Zeile (E19).
     *
     * **`ohnePartnerHinweis` ist der wichtigste Text dieses Abschnitts.** Er
     * erscheint genau dann, wenn das Feld leer ist, und sagt, was das Speichern
     * dann bedeutet: „hingesehen, es gibt keinen" (E4). In jeder anderen
     * Oberfläche heißt ein leeres Feld „noch nicht ausgefüllt" — hier ist es
     * eine Angabe, und zwar die einzige, die ein toter Prozess je bekommt.
     */
    bearbeiten: {
      oeffnen: "Bearbeiten",
      gesperrt: "Solange diese Zeile offen ist, lässt sich keine zweite öffnen.",
      partner: "Partner",
      partnerPlatzhalter: "Name eintippen oder wählen",
      partnerLeeren: "Partner leeren",
      vorschlaege: "Partnervorschläge",
      richtung: "Richtung",
      ohneRichtung: "keine",
      ohnePartnerHinweis: "Gespeichert heißt hier: hingesehen, es gibt keinen Partner.",
      speichern: "Speichern",
      speichernLaeuft: "Wird gespeichert …",
      abbrechen: "Abbrechen",
    },
    /**
     * Der eine Knopf, der drei Schritte fährt (E13, E14, E15): fehlende Zeilen
     * anlegen, offene Vorschläge auffrischen, und für **alle** Zeilen erheben,
     * ob Nachrichten daran hängen.
     *
     * **Alle acht Zahlen werden gezeigt.** Ohne `bestandGeprueft` und
     * `ohneNachrichten` wäre ein reihenweise wirkungsloser Lauf von einem
     * erfolgreichen nicht zu unterscheiden — dieselbe Falle wie bei der Zahl
     * verworfener Sitzungen in Schritt 9a.
     */
    /**
     * **Eine** Zahl über **alle** Prozesse des Mandanten (E18) — im Browser aus
     * der vollen Liste gezählt, ohne eigene Abfrage.
     *
     * Bei `VOTG` steht dort lange eine schlechte Zahl, und sie ist richtig: 350
     * von 390 Prozessen tragen keine Nachricht, gepflegt werden sie trotzdem.
     */
    fortschritt: {
      satz: "{gepflegt} von {gesamt} Prozessen gepflegt",
      alleZaehlenMit: "Tote Prozesse und der Auffangprozess zählen mit.",
    },
    /**
     * Der Hinweis über der Liste (E17). Der **Wortlaut ist festgelegt**; der
     * zweite Satz sagt, was daraus für die Arbeit folgt.
     */
    hinweis: {
      ohnePartnervorschlag: "für keinen Prozess konnte ein Partner vorgeschlagen werden",
      ohnePartnervorschlagFolge:
        "Die Partner dieses Mandanten sind von Hand einzutragen. Die Auswahl füllt sich mit jedem, den du speicherst.",
    },
    /**
     * Die Massenzuordnung nach Projekt (E11, E12).
     *
     * **`verlorenViele` ist die Zahl, die verloren geht.** Sie wird in Worten
     * gesagt und nicht nur angezeigt: Dass gepflegte Zeilen überschrieben
     * werden, ist gewollt — ein Schutzmodus machte genau die Korrektur
     * unmöglich, für die man das Werkzeug braucht.
     */
    masse: {
      oeffnen: "Massenzuordnung …",
      titel: "Massenzuordnung nach Projekt",
      einleitung:
        "Setzt ein Feld für alle Prozesse eines Projekts. Bereits gepflegte Zeilen werden dabei überschrieben — das ist gewollt und der Grund für die Vorschau.",
      projekt: "Projekt",
      projektWaehlen: "Projekt wählen",
      feld: "Feld",
      felder: {
        PARTNER: "Partner",
        RICHTUNG: "Richtung",
      },
      wert: "Wert",
      leerHinweis: "Ein leerer Wert löscht das Feld in allen betroffenen Zeilen.",
      vorschauHolen: "Vorschau",
      vorschauLaeuft: "Wird geprüft …",
      ausfuehren: "Ausführen",
      ausfuehrenLaeuft: "Wird gesetzt …",
      abbrechen: "Abbrechen",
      vorschauNoetig: "Hole zuerst die Vorschau — sie nennt, wie viele Zeilen es trifft.",
      betroffenKeine: "Dieses Projekt trägt keinen Prozess. Es gibt nichts zu setzen.",
      betroffenEins: "Betrifft einen Prozess.",
      betroffenViele: "Betrifft {betroffen} Prozesse.",
      verlorenKeine: "Keiner davon ist bereits gepflegt.",
      verlorenEins: "Einer davon ist bereits gepflegt — sein bisheriger Wert geht verloren.",
      verlorenViele: "{gepflegt} davon sind bereits gepflegt — ihr bisheriger Wert geht verloren.",
    },
    /**
     * Die Übernahme der Partnervorschläge (E22 bis E24).
     *
     * **`ohneVorschlagBleibtOffen` ist der Satz, ohne den der Dialog falsch
     * gelesen wird.** Bei `NEXANS` stehen 509 Vorschläge über 733 Prozessen;
     * ohne ihn sucht ein Administrator die fehlenden 224 in einem Fehler statt
     * in E22. Sie tragen eine Richtung aus dem Projektnamen, aber nie einen
     * Partnervorschlag — und „gepflegt mit leerem Partner" hieße in diesem
     * Katalog „hingesehen, es gibt keinen" (E4).
     *
     * **`filterWirktNicht` steht daneben, sobald „nur mit Nachrichten" gesetzt
     * ist.** Der Filter wird im Browser gerechnet (E20), die Übernahme läuft im
     * Backend über alle Prozesse des Mandanten (E23). Das ist die Stelle, an der
     * E23 sonst überrascht.
     */
    uebernahme: {
      oeffnen: "Vorschläge übernehmen",
      titel: "Partnervorschläge übernehmen",
      einleitung:
        "Bestätigt die Vorschläge, die die Regeln beim letzten Lauf gefunden haben. Es wird kein Wert geändert — nur der Pflegestatus.",
      keine: "Es gibt keine unbestätigten Partnervorschläge.",
      uebernimmtEins: "Übernimmt einen Partnervorschlag",
      uebernimmtViele: "Übernimmt {betroffen} Partnervorschläge",
      aufteilung: "— {regelA} aus Regel A, {regelB} aus Regel B.",
      keinen: "keinen",
      einen: "einen",
      folge:
        "Die Zeilen werden als gepflegt gekennzeichnet; Partner und Richtung bleiben, wie die Regel sie vorgeschlagen hat.",
      ohneVorschlagBleibtOffen:
        "Prozesse ohne Partnervorschlag bleiben offen, auch wenn sie eine Richtung tragen.",
      filterWirktNicht:
        "Der Filter „nur mit Nachrichten“ wirkt hier nicht — übernommen wird für alle Prozesse des Mandanten.",
      uebernehmen: "Übernehmen",
      uebernehmenLaeuft: "Wird übernommen …",
      abbrechen: "Abbrechen",
    },
    lauf: {
      starten: "Vorschläge und Bestand erheben",
      hinweis:
        "Legt fehlende Zeilen an, frischt offene Vorschläge auf und erhebt für jede Zeile, ob Nachrichten daran hängen.",
      laeuft: "Läuft …",
      laeuftSeit: "läuft seit {dauer}",
      ergebnisTitel: "Der letzte Lauf",
      ergebnisDauer: "gebraucht: {dauer}",
      zahlen: {
        angelegt: "angelegt",
        aufgefrischt: "aufgefrischt",
        unberuehrt: "unberührt",
        regelA: "Partner aus Regel A",
        regelB: "Partner aus Regel B",
        keine: "ohne Partnervorschlag",
        bestandGeprueft: "Bestand geprüft",
        ohneNachrichten: "davon ohne Nachrichten",
      },
      /**
       * Dieselben fünf Bausteine wie unter `nachrichten.detail.dauer`.
       *
       * **Bewusst noch einmal und nicht von dort gelesen.** Sie gehören keiner
       * Ansicht und müssten auf der obersten Ebene stehen — genau wie `suche`.
       * Sie dorthin zu heben, hieße die Schlüssel des
       * Nachrichtendetails anzufassen, und das ist eine eigene Runde. Als
       * offener Punkt vermerkt.
       */
      dauer: {
        unterSekunde: "< 1 s",
        sekunden: "{wert} s",
        minuten: "{wert} min",
        stunden: "{wert} h",
        tage: "{wert} d",
      },
    },
    leer: {
      titel: "Kein Prozess",
      ohneProzesse: "Für diesen Mandanten sind keine Prozesse hinterlegt.",
      allesGepflegt:
        "Jede Zeile dieses Mandanten ist gepflegt. Nimm den Haken bei „nur offene“ heraus, um alle zu sehen.",
      filterLeer:
        "Kein Prozess passt zu den gesetzten Filtern. Nimm einen Haken heraus, um mehr zu sehen.",
    },
  },

  /**
   * Die Benutzerverwaltung (Schritt 9a) — **das Feature selbst**.
   *
   * Der **Name** der Seite kommt weiterhin aus
   * `administration.bereiche.benutzer.titel`; es gibt genau einen Wortlaut
   * dafür, und er steht dort, wo auch die Navigation ihn liest.
   *
   * Die **Rollennamen** stehen nicht hier, sondern unter `rolle` — sie gelten
   * im ganzen Werkzeug gleich und werden seit Schritt 3 auch vom Nutzermenü
   * gelesen. Ein zweiter Wortlaut daneben wäre genau die Doppelpflege, vor der
   * `docs/frontend-grundlagen.md` §6 warnt.
   */
  benutzer: {
    tabelle: "Alle Konten, mandantenübergreifend",
    mandantenfrei:
      "Diese Liste gilt mandantenübergreifend: Sie zeigt alle Konten, unabhängig vom Mandanten, der oben eingestellt ist. Ein Wechsel dort ändert an ihr nichts.",
    spalten: {
      benutzer: "Benutzer",
      rolle: "Rolle",
      mandanten: "Mandanten",
      sperre: "Sperre",
      zeitsperre: "Zeitsperre",
      aktiv: "Konto",
      passwort: "Passwort",
      letzteAnmeldung: "Letzte Anmeldung",
      aktionen: "Bearbeiten",
    },
    ohneMandanten: "keine",
    bearbeiten: "Bearbeiten",
    bearbeitenFuer: "Konto {benutzer} bearbeiten",
    bearbeitenGesperrt:
      "Erst die offene Zeile schließen. Sonst gingen die dort begonnenen Eingaben verloren — auch ein bereits getipptes Passwort.",
    formular: {
      sperre: "Konto gesperrt",
      sperreHinweis:
        "Sperrt das Konto unbefristet. Das Entsperren räumt zugleich eine laufende Zeitsperre und den Fehlversuchszähler ab.",
      aktiv: "Konto aktiv",
      aktivHinweis:
        "Deaktivierte Konten kommen nicht mehr herein. Gelöscht wird nie — sonst wären ihre Protokollzeilen nicht mehr lesbar.",
      rolle: "Rolle",
      rolleOhneMandant:
        "Ohne Mandanten lässt sich dieses Konto nicht auf „Mandant“ herabstufen. Ordne ihm zuerst mindestens einen zu.",
      passwort: "Neues Passwort vergeben",
      passwortSetzen: "Passwort setzen",
      passwortHinweis:
        "Mindestens {laenge} Zeichen, und es darf nicht dem bisherigen entsprechen. Das Konto muss es beim nächsten Anmelden selbst ändern. Gib es dem Nutzer auf einem Weg, den du selbst wählst — hier steht es danach nirgends mehr.",
      eigenesKonto:
        "Das ist dein eigenes Konto. Jede Änderung daran meldet dich ab, und du musst dich neu anmelden.",
    },
    // Die Anlegemaske über der Liste (9c). Sie teilt sich die Sperre mit den
    // Zeilenformularen — der gesperrte Zustand trägt deshalb denselben Satz wie
    // dort, denn es ist derselbe Grund: ein bereits getipptes Einmalpasswort.
    anlegen: {
      oeffnen: "Konto anlegen",
      schliessen: "Maske schließen",
      gesperrt:
        "Erst die offene Zeile schließen. Sonst gingen die dort begonnenen Eingaben verloren — auch ein bereits getipptes Passwort.",
      titel: "Neues Konto",
      // Beide Sätze stehen über den Feldern und nicht darunter: Es sind die
      // Fragen, die sonst erst nach dem Tippen kämen.
      passwortHinweis:
        "Das Einmalpasswort tippst du selbst — mindestens {laenge} Zeichen. Das Konto muss es bei der ersten Anmeldung ändern.",
      mandantenHinweis:
        "Beim Anlegen genau ein Mandant. Weitere kommen danach an der Zeile des Kontos hinzu.",
      benutzername: "Benutzername",
      rolle: "Rolle",
      mandant: "Mandant",
      waehlen: "Bitte wählen",
      passwort: "Einmalpasswort",
      absenden: "Anlegen",
      laeuft: "Wird angelegt …",
      erfolgTitel: "Konto angelegt",
      erfolgText:
        "{benutzer} — {rolle}, Mandant {mandant}. Das Konto ist aktiv und muss sein Passwort bei der ersten Anmeldung ändern. Gib das Einmalpasswort auf einem Weg weiter, den du selbst wählst: Hier steht es nicht mehr.",
    },
    mandanten: {
      titel: "Mandanten",
      letzteZuordnung:
        "Ein Konto braucht mindestens einen Mandanten. Setze erst einen zweiten Haken, dann lässt sich dieser abwählen.",
      unbekannt: "steht nicht mehr in der Mandantenliste",
      speichern: "Mandanten speichern",
      verwerfen: "Verwerfen",
    },
    vorwarnung: {
      titel: "Das ist dein eigenes Konto",
      text: "„{vorgang}“ trifft dein eigenes Konto.",
      folge:
        "Der Vorgang wird ausgeführt — und weil dabei alle Sitzungen dieses Kontos verworfen werden, wirst du sofort abgemeldet und landest auf der Anmeldung. Neu anmelden kannst du dich anschließend ganz normal.",
      vorgaenge: {
        sperre: "Sperre ändern",
        aktiv: "Konto aktivieren oder deaktivieren",
        rolle: "Rolle ändern",
        mandanten: "Mandanten ändern",
        passwort: "Passwort setzen",
      },
      bestaetigen: "Ausführen und abmelden",
      laeuft: "Wird ausgeführt …",
      abbrechen: "Abbrechen",
    },
    sperre: {
      gesperrt: "gesperrt",
      offen: "nicht gesperrt",
    },
    // Die zweite, ganz andere Sperre: nach fünf Fehlversuchen, und sie endet von
    // selbst. Der Text nennt deshalb einen Zeitpunkt und keinen Zustand.
    zeitsperre: {
      bis: "bis {zeitpunkt}",
      keine: "keine",
    },
    aktiv: {
      aktiv: "aktiv",
      deaktiviert: "deaktiviert",
    },
    passwort: {
      wechselNoetig: "Wechsel erforderlich",
      keinWechsel: "gesetzt",
    },
    // `null` heißt „noch nie angemeldet" und wird ausgeschrieben. Eine leere
    // Zelle sähe aus wie eine fehlende Angabe und beantwortet die häufigste
    // Supportfrage nicht.
    anmeldung: {
      nie: "nie angemeldet",
    },
    leer: {
      titel: "Keine Konten",
      hinweis:
        "Die Liste ist leer. Das kann im Betrieb nicht vorkommen — wer sie sieht, hat selbst ein Konto. Bitte melde das der EDI-Betreuung.",
    },
  },

  zustand: {
    laedt: "Wird geladen …",
    leerTitel: "Nichts anzuzeigen",
    fehlerTitel: "Das hat nicht geklappt",
    erneutVersuchen: "Erneut versuchen",
    kennung: "Fehler-Kennung",
    kennungHinweis: "Gib diese Kennung an, wenn du die Störung meldest.",
    /**
     * Der vierte Zustand, und er ist keiner der drei anderen: **kein Zugriff.**
     *
     * Der *Satz* dazu steht nicht hier, sondern unter
     * `fehler["zugriff-verweigert"]` — er kommt vom Backend als Problemtyp und
     * gilt überall gleich. Hier steht allein die Überschrift, wie bei
     * {@link leerTitel} und {@link fehlerTitel} auch. Zwei Wortlaute für
     * dieselbe Sache wären genau die Doppelpflege, die `docs/frontend-grundlagen.md`
     * §6 an anderer Stelle verbietet.
     */
    keinZugriffTitel: "Kein Zugriff",
  },

  fehler: {
    // Anmeldung — bewusst unspezifisch. Der Text unterscheidet nicht zwischen
    // unbekanntem Benutzernamen und falschem Passwort, weil das Backend es auch
    // nicht tut.
    "anmeldung-abgelehnt": "Benutzername oder Passwort ist falsch.",
    "konto-gesperrt":
      "Das Konto ist nach mehreren Fehlversuchen für einige Zeit gesperrt. Versuche es später erneut oder wende dich an die EDI-Betreuung.",
    // Seit Schritt 9a: die **administrative** Sperre, und sie braucht einen
    // eigenen Text. „Nach mehreren Fehlversuchen" wäre bei einem Verwaltungsakt
    // eine falsche Auskunft — und der entscheidende Unterschied für den, der
    // davorsteht, ist, dass diese hier **nicht** von selbst abläuft. Ohne den
    // zweiten Satz wartete er eine Viertelstunde umsonst.
    "konto-administrativ-gesperrt":
      "Das Konto wurde von der EDI-Betreuung gesperrt. Diese Sperre läuft nicht von selbst ab — wende dich an sie.",
    "konto-deaktiviert": "Das Konto ist deaktiviert. Wende dich an die EDI-Betreuung.",
    "zu-viele-anmeldeversuche":
      "Von dieser Adresse kamen zu viele Anmeldeversuche. Versuche es in einigen Minuten erneut.",

    // Sitzung und Berechtigung
    "nicht-angemeldet": "Die Sitzung ist abgelaufen. Melde dich erneut an.",
    "zugriff-verweigert": "Dieser Bereich ist für deine Rolle nicht freigegeben.",
    // Ebenfalls 403, aber das Gegenteil von „zugriff-verweigert": Hier hilft ein
    // erneuter Versuch. Der Text nennt bewusst nicht, warum der Token nicht trug.
    "csrf-token-ungueltig":
      "Die Anfrage konnte nicht angenommen werden. Lade die Seite neu und sende sie erneut.",
    "kein-mandant-gewaehlt": "Wähle zuerst einen Mandanten aus.",
    "passwortwechsel-erforderlich": "Vergib zuerst ein neues Passwort.",

    // 404 — sagt niemals etwas über Berechtigung aus. Eine fremde und eine nicht
    // vorhandene Ressource müssen ununterscheidbar bleiben; ein Hinweis auf
    // fehlenden Zugriff verriete genau den Unterschied, den das Backend verbirgt.
    "nicht-gefunden": "Das Gesuchte gibt es nicht.",

    // Eingaben
    "eingabe-ungueltig": "Bitte prüfe die markierten Felder.",
    "anfrage-ungueltig": "Diese Anfrage konnte nicht verarbeitet werden.",

    // Nachrichtenliste — die Fehlertypen aus docs/nachrichtenliste.md §1.
    // Jeder nennt, was zu tun ist; keiner nennt einen Tabellennamen.
    "zeitfenster-mehrdeutig":
      "Wähle entweder eine Vorwahl oder ein freies Zeitfenster, nicht beides.",
    "zeitfenster-unvollstaendig": "Ein freies Zeitfenster braucht beide Zeitpunkte.",
    "zeitfenster-ungueltig": "Der Zeitpunkt „bis“ muss nach „von“ liegen.",
    "zeitfenster-zu-gross": "Das Zeitfenster darf höchstens ein Jahr umfassen.",
    // Prozessansicht, freies Fenster: abgewiesen statt gerundet (`docs/process-view.md` §37 ff.).
    "zeitfenster-zu-genau": "Beide Zeitpunkte müssen auf einer vollen Stunde liegen.",
    "zeitpunkt-ungueltig": "Einer der beiden Zeitpunkte ist nicht lesbar.",
    "zeitraum-unbekannt": "Diesen Zeitraum gibt es nicht.",
    "sortierung-unbekannt": "Diese Sortierung gibt es nicht.",
    "status-unbekannt": "Diesen Status gibt es nicht.",
    "suchbegriff-zu-kurz": "Der Suchbegriff braucht mindestens drei Zeichen.",
    // Bewusst als Hinweis am Suchfeld und nicht als Fehlerzustand der Ansicht:
    // Der Nutzer hat nichts falsch gemacht, sein Begriff ist nur zu weit.
    "suchbegriff-zu-unscharf": "Der Suchbegriff trifft zu viele Prozesse. Verenge ihn.",
    // Der allgemeine Satz ohne Zahlen. Die konkrete Meldung mit Grenze und
    // gewähltem Zeitraum baut das Suchfeld aus der Antwort — hier stünde die
    // Grenze sonst ein zweites Mal und liefe der ersten irgendwann hinterher.
    "suche-fenster-zu-gross": "Für die Suche ist dieses Zeitfenster zu groß.",
    // Kein technischer Text und keine Fehler-Kennung: Der Abbruch an der
    // Zeitgrenze ist bei gesetztem Suchbegriff ein absehbarer Fall, und was hilft,
    // steht im Satz.
    "suche-abgebrochen":
      "Die Suche hat zu lange gedauert und wurde abgebrochen. Verkleinere den Zeitraum oder schärfe den Suchbegriff.",

    // Belegsuche mit Feldbegriffen (docs/property-suche.md §2.2). Die Oberfläche
    // erzeugt die ersten beiden nie selbst — ein Feld muss gewählt sein, bevor
    // eine Feld-Marke entsteht —; sie stehen hier für die von Hand gebaute URL.
    // Die Texte sind die des Backends, nicht neu formuliert.
    "feldname-fehlt":
      "Vor dem Doppelpunkt steht der Feldname — ohne Feld wird nur nach Belegnummern gesucht.",
    "feldbegriff-ohne-trenner": "Ein Feldbegriff hat die Form feldname:wert.",
    "suchbegriff-fehlt":
      "Gib mindestens eine Belegnummer (typ:wert oder :wert) oder ein Feld (feldname:wert) an.",
    "zu-viele-suchbegriffe":
      "Es lassen sich höchstens 8 Begriffe gleichzeitig suchen — Belegnummern und Felder zusammen.",

    // Katalogpflege — beide kommen aus `PUT /api/katalog/prozesse/{processId}`.
    // Ein unbekannter Wert faellt dort ausdruecklich nicht stillschweigend auf
    // leer zurueck; sonst bliebe ein Tippfehler unbemerkt.
    "partner-zu-lang": "Ein Partnername darf höchstens 100 Zeichen haben.",
    "richtung-unbekannt": "Wähle „Eingehend“, „Ausgehend“ — oder gar keine Richtung.",
    "feld-unbekannt": "Wähle genau eines der Felder Partner oder Richtung.",
    "modus-unbekannt": "Diesen Modus gibt es nicht.",
    "limit-ungueltig": "Diese Seitengröße ist nicht zulässig.",
    "cursor-ungueltig": "Die Seitenposition ist nicht mehr gültig. Beginne wieder auf Seite eins.",
    "altes-passwort-falsch": "Das bisherige Passwort stimmt nicht.",
    "passwort-zu-kurz": "Das neue Passwort braucht mindestens zwölf Zeichen.",
    "passwort-unveraendert": "Das neue Passwort muss sich vom bisherigen unterscheiden.",

    // Benutzerverwaltung (Schritt 9a). **Die vier `409` sind kein Rechteproblem:**
    // Die Eingabe ist in Ordnung, der Zustand des Kontos verbietet sie. Die Texte
    // dürfen deshalb nicht wie „zugriff-verweigert" klingen — und jeder nennt,
    // was zu tun ist, damit es doch geht. Genau darin liegt der Unterschied zu
    // einem `403`: Dort gibt es nichts zu tun.
    selbstschutz:
      "Am eigenen Konto geht das nicht — sperren, deaktivieren und herabstufen sind dort ausgeschlossen. Ein anderer Administrator kann es.",
    "letzter-admin":
      "Das ist der letzte nutzbare Administrator. Mach zuerst ein anderes Konto zum aktiven, nicht gesperrten Administrator, dann geht es.",
    "letzte-mandantenzuordnung":
      "Ein Konto braucht mindestens einen Mandanten. Ordne einen anderen zu, bevor du diesen entfernst.",
    "rolle-ohne-mandant":
      "Dieses Konto hat keinen Mandanten. Ordne ihm zuerst mindestens einen zu, dann lässt es sich herabstufen.",
    "unbekannte-rolle": "Diese Rolle gibt es nicht. Wähle „EDI-Betreuung“ oder „Mandant“.",

    // Anlegen über die Oberfläche (9c). Beide kommen aus `POST /api/admin/users`
    // und standen dort seit Schritt 3 — übersetzt sind sie erst jetzt, weil es
    // vorher keinen Bedienweg dorthin gab. Ohne Schlüssel fiele die Anzeige auf
    // `detail` zurück: ein richtiger Satz, aber ein deutscher in einer
    // englischen Oberfläche.
    "benutzername-vergeben":
      "Diesen Benutzernamen gibt es schon. Wähle einen anderen — ein bestehendes Konto wird nie überschrieben.",
    "benutzername-zu-lang": "Der Benutzername darf höchstens 100 Zeichen haben.",

    // Technisch
    "technischer-fehler":
      "Ein technischer Fehler ist aufgetreten. Bitte melde die angegebene Fehler-Kennung.",
    netzwerk: "Das Backend ist gerade nicht erreichbar.",
    unbekannt: "Ein unerwarteter Fehler ist aufgetreten.",
  },
};

/**
 * Die Form beider Sprachdateien. `en.ts` wird gegen diesen Typ geprüft: Ein
 * fehlender Schlüssel ist ein Typfehler, ein überzähliger ebenfalls (überschüssige
 * Eigenschaften eines Objektliterals).
 *
 * Bewusst **ohne** `as const` an `de` — sonst wären die Werte Literaltypen und
 * jede englische Übersetzung „nicht zuweisbar an 'Anmeldung'".
 */
export type Texte = typeof de;
