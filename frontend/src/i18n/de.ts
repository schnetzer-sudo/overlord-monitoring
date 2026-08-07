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
    eintraege: {
      startseite: "Startseite",
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

  rolle: {
    ADMIN: "EDI-Betreuung",
    MANDANT: "Mandant",
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

  startseite: {
    titel: "Startseite",
    platzhalterTitel: "Noch nichts zu sehen",
    platzhalterHinweis:
      "Die Übersicht entsteht in einem späteren Schritt. Anmeldung, Mandantentrennung und der Anwendungsrahmen stehen bereits.",
  },

  platzhalter: {
    titel: "In Arbeit",
    hinweis: "Diese Ansicht entsteht in einem späteren Schritt.",
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
    bedeutungNichtVerifiziert: "Bedeutung nicht verifiziert",
    zeitRelativ: "Abstand zu jetzt",
    sortierungUmschalten: "Nach Zeitpunkt sortieren",
    sortierungNeueste: "Neueste zuerst",
    sortierungAelteste: "Älteste zuerst",

    status: {
      FEHLER: "Fehler",
      WARTEND: "Wartend",
      LAEUFT: "Läuft",
      ZWISCHENSCHRITT: "Zwischenschritt",
      ABGESCHLOSSEN: "Abgeschlossen",
      QUITTIERT: "Quittiert",
      UNGEKLAERT: "Ungeklärt",
    },

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
      bezeichnung: "Suche",
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

    zwischenschritte: {
      chipAus: "Zwischenschritte ausgeblendet",
      chipAn: "Zwischenschritte eingeblendet",
      erklaerung: "Gesplittete und zusammengeführte Nachrichten — rund ein Drittel aller Zeilen.",
      einblenden: "Zwischenschritte einblenden",
      ausblenden: "Zwischenschritte ausblenden",
    },

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
      zwischenschritte: "Zwischenschritte sind ausgeblendet — blende sie ein.",
      suche: "Der Suchbegriff schränkt die Liste ein — leere ihn.",
      status: "Der Statusfilter schränkt die Liste ein — leere ihn.",
      prozess: "Der Prozessfilter schränkt die Liste ein — leere ihn.",
      fensterErweitern: "Auf 30 Tage erweitern",
    },
  },

  zustand: {
    laedt: "Wird geladen …",
    leerTitel: "Nichts anzuzeigen",
    fehlerTitel: "Das hat nicht geklappt",
    erneutVersuchen: "Erneut versuchen",
    kennung: "Fehler-Kennung",
    kennungHinweis: "Gib diese Kennung an, wenn du die Störung meldest.",
  },

  fehler: {
    // Anmeldung — bewusst unspezifisch. Der Text unterscheidet nicht zwischen
    // unbekanntem Benutzernamen und falschem Passwort, weil das Backend es auch
    // nicht tut.
    "anmeldung-abgelehnt": "Benutzername oder Passwort ist falsch.",
    "konto-gesperrt":
      "Das Konto ist nach mehreren Fehlversuchen für einige Zeit gesperrt. Versuche es später erneut oder wende dich an die EDI-Betreuung.",
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
    "limit-ungueltig": "Diese Seitengröße ist nicht zulässig.",
    "cursor-ungueltig": "Die Seitenposition ist nicht mehr gültig. Beginne wieder auf Seite eins.",
    "altes-passwort-falsch": "Das bisherige Passwort stimmt nicht.",
    "passwort-zu-kurz": "Das neue Passwort braucht mindestens zwölf Zeichen.",
    "passwort-unveraendert": "Das neue Passwort muss sich vom bisherigen unterscheiden.",

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
