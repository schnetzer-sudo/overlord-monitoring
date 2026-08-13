import type { Texte } from "./de";

/**
 * English. Same key set as `de.ts` — enforced twice: by the type below at build
 * time, and by a test that compares both key sets at runtime (the type alone
 * would not catch a surplus key nested behind an index signature).
 */
export const en: Texte = {
  anwendung: {
    name: "Overlord Monitoring",
    beschreibung: "State of the EDI transfers of the Overlord integration platform",
  },

  navigation: {
    bezeichnung: "Main navigation",
    oeffnen: "Open menu",
    schliessen: "Close menu",
    eintraege: {
      startseite: "Home",
      nachrichten: "Messages",
      prozesse: "Processes",
      administration: "Administration",
    },
  },

  kopfzeile: {
    mandantBezeichnung: "Active tenant",
    keinMandant: "No tenant selected",
    mandantWechseln: "Switch tenant",
    spracheBezeichnung: "Language",
    spracheDeutsch: "Deutsch",
    spracheDeutschKurz: "DE",
    spracheEnglisch: "English",
    spracheEnglischKurz: "EN",
  },

  nutzermenue: {
    oeffnen: "User menu",
    angemeldetAls: "Signed in as",
    passwortAendern: "Change password",
    abmelden: "Sign out",
    abmeldenLaeuft: "Signing out …",
  },

  rolle: {
    ADMIN: "EDI support",
    MANDANT: "Tenant",
  },

  problem: {
    ueberfaellig: "Overdue",
    ueberfaelligHinweis: "The deadline for this message has passed.",
  },

  anmeldung: {
    titel: "Sign in",
    einleitung: "Sign in to see the state of your EDI transfers.",
    benutzername: "User name",
    passwort: "Password",
    absenden: "Sign in",
    laeuft: "Signing in …",
    felderFehlen: "Please enter user name and password.",
  },

  passwort: {
    titel: "Change password",
    zwangEinleitung:
      "This account still uses a one-time password. Set your own first; until then the remaining functions stay locked.",
    freiwilligEinleitung: "Set a new password for this account.",
    alt: "Current password",
    neu: "New password",
    wiederholung: "Repeat new password",
    regel: "At least twelve characters. Length helps, special characters barely do.",
    absenden: "Save password",
    laeuft: "Saving …",
    wiederholungFalsch: "The two entries do not match.",
    zurueck: "Back",
  },

  mandantenauswahl: {
    titel: "Select tenant",
    einleitung: "Every view shows data of the selected tenant only.",
    aktiv: "Active",
    waehlen: "Select",
    laeuft: "Switching …",
    leerTitel: "No tenant available",
    leerHinweis:
      "This account has no tenant assigned. Contact EDI support so the assignment gets added.",
  },

  startseite: {
    titel: "Home",
    platzhalterTitel: "Nothing here yet",
    platzhalterHinweis:
      "The overview arrives in a later step. Sign-in, tenant separation and the application frame are already in place.",
  },

  platzhalter: {
    titel: "Work in progress",
    hinweis: "This view arrives in a later step.",
  },

  nachrichten: {
    spalten: {
      zeitpunkt: "Time",
      status: "Status",
      ablauf: "Workflow",
      projekt: "Project",
    },
    prozessName: "Process",
    nichtZugeordnet: "not assigned",
    ohneWert: "—",
    rohwert: "Status value of the legacy system",
    schrittZusatz: "Step: {schritt}",
    zeileOeffnen: "Show details of this message",
    bedeutungNichtVerifiziert: "Meaning not verified",
    ungeklaertFusszeile:
      "A badge with a question mark shows the legacy system's status value unchanged: its " +
      "business meaning is not documented, and nothing is guessed.",
    zeitRelativ: "Time from now",
    sortierungUmschalten: "Sort by time",
    sortierungNeueste: "Newest first",
    sortierungAelteste: "Oldest first",

    status: {
      FEHLER: "Error",
      WARTEND: "Waiting",
      LAEUFT: "Running",
      AUFGETEILT: "Split",
      ZUSAMMENGEFUEHRT: "Merged",
      ABGESCHLOSSEN: "Completed",
      QUITTIERT: "Acknowledged",
      UNGEKLAERT: "Unclear",
    },

    zeitfenster: {
      bezeichnung: "Time window",
      h24: "24 hours",
      d7: "7 days",
      d30: "30 days",
      frei: "Custom",
      von: "From",
      bis: "To",
      standardHinweis: "Without a choice the server's default window applies.",
      zuruecksetzen: "Reset time window",
      unvollstaendig: "Please enter both date and time.",
      beideNoetig: "A custom time window still needs its second point in time.",
    },

    statusfilter: {
      bezeichnung: "Status",
      alle: "All statuses",
      gewaehlt: "{anzahl} selected",
      zuruecksetzen: "Clear status filter",
    },

    prozessfilter: {
      bezeichnung: "Process",
      alle: "All processes",
      gewaehlt: "{anzahl} selected",
      suchen: "Narrow processes",
      keineTreffer: "No process matches this input.",
      leer: "No process is set up for this tenant.",
      unbekannt: "Process of another tenant",
      zuruecksetzen: "Clear process filter",
    },

    suche: {
      // Names what it searches — there is a second search field in the header
      // since step 7 part 3, and it searches something else.
      bezeichnung: "Search processes, projects and flows",
      platzhalter: "Process, project or flow name",
      zuKurz: "{anzahl} more characters, then the search runs.",
      leeren: "Clear search",
      fensterZuGross:
        "Search covers at most {grenze} days; {angefragt} are selected. Narrow the time window.",
      trotzdemSuchen: "Search anyway",
      langeSucheLaeuft: "The search covers a longer period and may take a moment.",
    },

    blaettern: {
      zurueck: "Previous page",
      vor: "Next page",
      seiteEins: "Page 1",
      weitereSeite: "more page",
    },

    aktualisierung: {
      schalter: "Refresh automatically",
      jetztAktualisieren: "Refresh now",
      stand: "As of {zeit}",
      standUnbekannt: "Not loaded yet",
      pausiertGeblaettert: "Paused while paging.",
      laeuft: "Refreshing …",
    },

    leer: {
      titel: "No message in this range",
      zeitfenster: "Widen the time window.",
      suche: "The search term narrows the list — clear it.",
      status: "The status filter narrows the list — clear it.",
      prozess: "The process filter narrows the list — clear it.",
      fensterErweitern: "Widen to 30 days",
    },

    detail: {
      titel: "Message",
      schliessen: "Close view",
      zurueckZurListe: "Back to the list",
      ansichtOhneListe: "Show without list",
      ansichtNebenListe: "Show next to list",
      zeitpunkt: "Last changed",
      start: "Started",
      gesamtdauer: "Total duration",
      projekt: "Project",
      prozess: "Process",
      kennung: "Message reference",
      kennungKopieren: "Copy reference",
      kennungKopiert: "Copied",

      ablaufTitel: "What happened",
      keineSchritte: "No process step is recorded for this message.",
      empfangen: "Received on {zeitpunkt} — no step has run since.",
      empfangenOhneZeitpunkt: "The message has been received — no step has run since.",
      ohneAktion: "No workflow run is recorded for this message.",
      ohneDauer: "no duration recorded",
      laeuftGerade: "running now",
      nochNichtBegonnen: "not started yet",
      wartetSeit: "waiting for {dauer}",
      laeuftSeit: "running for {dauer}",
      frist: "deadline {dauer}",
      wartetVorUnbekannt:
        "The message is waiting — what for is not recorded in the workflow definition.",
      wartetWeiterhin: "The message is waiting — it will not continue on its own.",
      verweistAuf: "The workflow points at: {schritt}",
      baustein: "Building block",
      herkunft: {
        DIREKT: "Name from the workflow definition",
        HERGELEITET: "Name derived from the building block within the workflow",
        ROHWERT: "The workflow holds no name — the building block is shown instead",
      },

      kuratiert: {
        "Message.SendingPartner": "Sender",
        "Message.SplitCount": "Split count",
      },

      bam: {
        titel: "Document data ({anzahl})",
        aufklappen: "Show document data",
        zuklappen: "Hide document data",
        weitere: "and {anzahl} more",
        leer: "No document number is recorded for this message.",
      },

      eigenschaften: {
        titel: "Technical properties ({anzahl})",
        keine: "No technical properties",
        aufklappen: "Show technical properties",
        zuklappen: "Hide technical properties",
        name: "Name",
        wert: "Value",
        gekappt: "shortened",
        gekapptHinweis: "Shortened — {bytes} bytes in the original.",
        leer: "No property is recorded for this message.",
      },

      nichtGefunden:
        "Under the tenant shown in the header this message does not exist. If the link came from someone else, check that tenant first.",

      dauer: {
        unterSekunde: "< 1 s",
        sekunden: "{wert} s",
        minuten: "{wert} min",
        stunden: "{wert} h",
        tage: "{wert} d",
      },
    },

    kette: {
      kommtVon: {
        titel: "Came from",
        titelEins: "Came from — 1 input",
        titelZahl: "Came from — {anzahl} inputs",
      },
      wurdeZu: {
        titel: "Became",
        titelEins: "Became — 1 part",
        titelZahl: "Became — {anzahl} parts",
      },
      beziehung: {
        AUFTEILUNG: "Split",
        ZUSAMMENFUEHRUNG: "Merge",
      },
      stufe: "Level {stufe}",
      gliedOeffnen: "Open {ablauf}",
      weitereLaden: "Load more",
      laedtWeitere: "Loading …",
      geladen: "{anzahl} links loaded",
      tiefeErreicht: "The chain is longer than shown here.",
      zyklusErkannt: "The chain runs in a circle — it stops here.",
    },
  },

  /** The document search (step 7, part 3) — its own view, not part of the list. */
  suche: {
    titel: "Document search",

    feld: {
      bezeichnung: "Search document number",
      platzhalter: "Search document number",
      hinzufuegen: "Add term",
    },

    typwahl: {
      alle: "All document types",
      gewaehlt: "Document type: {belegart}",
    },

    marken: {
      bezeichnung: "Search terms",
      entfernen: "Remove term",
      grenzeErreicht:
        "The search takes at most {anzahl} terms — a guard rail, not a business limit. Remove one to search for another.",
    },

    spalten: {
      treffer: "Match",
      kette: "Chain",
    },

    treffer: {
      weitere: "{erste} +{anzahl}",
      alleTypen: "Matched as: {typen}",
    },

    kette: {
      kurz: {
        SPLIT_WURZEL: "Split",
        SPLIT_KIND: "Part",
        MERGE_EINGANG: "Input",
        MERGE_ERGEBNIS: "Result",
      },
      satz: {
        SPLIT_WURZEL: "This message was split — the parts continue on their own.",
        SPLIT_KIND: "This message is one part of a split.",
        MERGE_EINGANG: "This message went into a merge.",
        MERGE_ERGEBNIS: "This message came out of a merge.",
      },
    },

    varianten: "Searched for {eingabe} and {fassungen}.",

    fenster: {
      aendern: "Change time window",
      einJahr: "Extend to one year",
      vorgabe: "Restore default",
      gilt: "The time window {von} to {bis} applies.",
    },

    ergebnis: {
      anzahl: "{anzahl} matches in the time window {von} to {bis}.",
      abgeschnitten:
        "More than {anzahl} matches — showing the {anzahl} most recent in the time window {von} to {bis}. Narrow the period or name a second document number.",
      keine: "No message carries this document",
      keineHinweis:
        "Extend the time window or remove a term. You do not need to type a leading zero — the search adds it.",
      nulltreffer: "With this term: 0. Without it: {anzahl}.",
      nulltrefferAbgeschnitten: "With this term: 0. Without it: more than {anzahl}.",
    },

    leer: {
      titel: "What are you looking for?",
      was: "Type a document number into the field above — delivery note, order or transport number, batch, plant or material number. Enter starts the search.",
      belegarten: "These document types are set up for this tenant:",
      hilfe:
        "The search also looks for leading zeros and a leading space — they are not on the printed document but they are in the data. Several terms are combined with AND.",
    },

    abgebrochen:
      "The search took too long and was cancelled. Narrow the period or name a second document number.",
  },

  zustand: {
    laedt: "Loading …",
    leerTitel: "Nothing to show",
    fehlerTitel: "That did not work",
    erneutVersuchen: "Try again",
    kennung: "Error reference",
    kennungHinweis: "Quote this reference when you report the problem.",
  },

  fehler: {
    // Sign-in — deliberately unspecific. The wording does not distinguish an
    // unknown user name from a wrong password, because the backend does not
    // either.
    "anmeldung-abgelehnt": "User name or password is wrong.",
    "konto-gesperrt":
      "The account is locked for a while after several failed attempts. Try again later or contact EDI support.",
    "konto-deaktiviert": "The account is disabled. Contact EDI support.",
    "zu-viele-anmeldeversuche":
      "Too many sign-in attempts came from this address. Try again in a few minutes.",

    // Session and permission
    "nicht-angemeldet": "The session has expired. Please sign in again.",
    "zugriff-verweigert": "This area is not enabled for your role.",
    // Also a 403, but the opposite of "zugriff-verweigert": here a retry helps.
    // The wording deliberately does not say why the token did not hold.
    "csrf-token-ungueltig": "The request could not be accepted. Reload the page and send it again.",
    "kein-mandant-gewaehlt": "Select a tenant first.",
    "passwortwechsel-erforderlich": "Set a new password first.",

    // 404 — never says anything about permission. A foreign and a non-existent
    // resource must stay indistinguishable; a hint about missing access would
    // reveal exactly the difference the backend hides.
    "nicht-gefunden": "What you are looking for does not exist.",

    // Input
    "eingabe-ungueltig": "Please check the marked fields.",
    "anfrage-ungueltig": "This request could not be processed.",

    // Message list — the problem types from docs/nachrichtenliste.md §1. Each
    // says what to do; none names a table.
    "zeitfenster-mehrdeutig": "Choose either a preset range or a custom one, not both.",
    "zeitfenster-unvollstaendig": "A custom time window needs both points in time.",
    "zeitfenster-ungueltig": "“To” must lie after “from”.",
    "zeitfenster-zu-gross": "The time window may span at most one year.",
    "zeitpunkt-ungueltig": "One of the two points in time cannot be read.",
    "zeitraum-unbekannt": "This range does not exist.",
    "sortierung-unbekannt": "This sort order does not exist.",
    "status-unbekannt": "This status does not exist.",
    "suchbegriff-zu-kurz": "The search term needs at least three characters.",
    // Deliberately a hint at the search field, not an error state of the view:
    // the user did nothing wrong, the term is just too broad.
    "suchbegriff-zu-unscharf": "The search term matches too many processes. Narrow it down.",
    // The generic sentence without numbers — the concrete message with the limit
    // and the selected range is built at the search field from the response.
    "suche-fenster-zu-gross": "This time window is too large for a search.",
    "suche-abgebrochen":
      "The search took too long and was cancelled. Shorten the time range or narrow the search term.",
    "limit-ungueltig": "This page size is not allowed.",
    "cursor-ungueltig": "The page position is no longer valid. Start again on page one.",
    "altes-passwort-falsch": "The current password is not correct.",
    "passwort-zu-kurz": "The new password needs at least twelve characters.",
    "passwort-unveraendert": "The new password must differ from the current one.",

    // Technical
    "technischer-fehler":
      "A technical error occurred. Please report the error reference shown below.",
    netzwerk: "The backend cannot be reached right now.",
    unbekannt: "An unexpected error occurred.",
  },
};
