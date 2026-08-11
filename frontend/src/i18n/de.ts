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

  /**
   * Die drei Problemkategorien aus `PROJEKTBESCHREIBUNG.md` §4.2 — **auf oberster
   * Ebene, nicht unter `nachrichten`.**
   *
   * Sie gehören keiner Ansicht: Das Detail benennt sie an einer Nachricht, das
   * Dashboard zählt sie über viele. Beide müssen dasselbe Wort sagen, sonst heißt
   * dieselbe Sache an zwei Stellen verschieden — und der Nutzer hält sie für zwei
   * Sachen. Die Kategorien werden nie zu „Fehler" zusammengefasst (Regel Q3).
   *
   * Angelegt ist nur, was heute gezeigt wird: `ueberfaellig`. „Fehler" hat seine
   * Beschriftung bereits an der Statusplakette, „Unquittiert" gibt es noch nicht.
   */
  problem: {
    ueberfaellig: "Überfällig",
    ueberfaelligHinweis: "Die Frist für diese Nachricht ist abgelaufen.",
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

    // AUFGETEILT und ZUSAMMENGEFUEHRT sind am 11.08.2026 an die Stelle des einen
    // Wertes „Zwischenschritt" getreten. Technisch waren SPLITTED und MERGED
    // dasselbe; für den Nutzer bedeuten sie Gegenteiliges — aus eins wurde viel
    // gegen aus viel wurde eins. Das Wort „Zwischenschritt" kommt in keiner
    // Oberflächenzeichenkette mehr vor.
    status: {
      FEHLER: "Fehler",
      WARTEND: "Wartend",
      LAEUFT: "Läuft",
      AUFGETEILT: "Aufgeteilt",
      ZUSAMMENGEFUEHRT: "Zusammengeführt",
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
