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
