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
    aktuellerSchritt: "Current step",
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
      ZWISCHENSCHRITT: "Intermediate",
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
      bezeichnung: "Search",
      platzhalter: "Process, project or flow name",
      zuKurz: "{anzahl} more characters, then the search runs.",
      leeren: "Clear search",
      fensterZuGross:
        "Search covers at most {grenze} days; {angefragt} are selected. Narrow the time window.",
      trotzdemSuchen: "Search anyway",
      langeSucheLaeuft: "The search covers a longer period and may take a moment.",
    },

    zwischenschritte: {
      chipAus: "Intermediate steps hidden",
      chipAn: "Intermediate steps shown",
      erklaerung: "Split and merged messages are not included.",
      einblenden: "Show intermediate steps",
      ausblenden: "Hide intermediate steps",
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
      zwischenschritte: "Intermediate steps are hidden — show them.",
      suche: "The search term narrows the list — clear it.",
      status: "The status filter narrows the list — clear it.",
      prozess: "The process filter narrows the list — clear it.",
      fensterErweitern: "Widen to 30 days",
    },
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
