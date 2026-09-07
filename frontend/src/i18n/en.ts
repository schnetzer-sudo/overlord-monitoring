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
      startseite: "Overview",
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

  dichte: {
    bezeichnung: "Display size",
    stufen: {
      xs: "Very small",
      s: "Small",
      m: "Medium",
      l: "Large",
    },
  },

  thema: {
    bezeichnung: "Appearance",
    werte: {
      hell: "Light",
      dunkel: "Dark",
      system: "System setting",
    },
  },

  rolle: {
    ADMIN: "EDI support",
    MANDANT: "Tenant",
  },

  einordnung: {
    FEHLER: "Error",
    WARTEND: "Waiting",
    LAEUFT: "Running",
    AUFGETEILT: "Split",
    ZUSAMMENGEFUEHRT: "Merged",
    ABGESCHLOSSEN: "Completed",
    QUITTIERT: "Acknowledged",
    UNGEKLAERT: "Unclear",
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

  zeitraum: {
    bezeichnung: "Period",
    "48H": "48 hours",
    "30T": "30 days",
    "12M": "12 months",
    FREI: "Custom",
    von: "From",
    bis: "To",
    unvollstaendig: "Please enter both date and time.",
    beideNoetig: "A custom time window still needs its second point in time.",
  },

  dashboard: {
    titel: "Overview",
    leerTitel: "Nothing in this period",
    leerHinweis:
      "No message moved during this period. Pick a longer period — if it stays empty, this tenant " +
      "has no data.",

    verlauf: {
      titel: "History",
      // Entscheidung E‑82, siehe `de.ts`: Die Beschriftung gilt für alle
      // Mitglieder der Rolle und wiederholt keines von ihnen. „Completed" stand
      // hier zugleich als `einordnung.ABGESCHLOSSEN`.
      rollen: {
        fehler: "Errors",
        offen: "No outcome",
        abgeschlossen: "Done",
        ungeklaert: "Unclear",
      },
      gesamt: "Total",
      achseAnzahl: "Messages",
      streifenTitel: "Errors over time",
      streifenAchse: "Errors",
      streifenHinweis:
        "Separate scale: the height of this strip cannot be compared with the history above it.",
      streifenLeer: "No message in this period is classified as an error.",
    },

    kacheln: {
      nachrichten: "Messages",
      nachrichtenHinweis:
        "What is counted is movement: a message appears in the period in which it last changed — " +
        "not in the one in which it arrived.",
      fehler: "Errors",
      fehlerVerweis: "Open these messages in the list",
      artenAufklappen: "Break down by kind",
      artenZuklappen: "Close the breakdown",
      laeuftVerweis: "Open the running messages in the list",
      wartendVerweis: "Open the waiting messages in the list",
      aeltesterSeit: "oldest for {dauer}",
      wartendOhneVerweis:
        "The oldest of these messages goes back more than a year. The message list shows at most " +
        "one year — a link would therefore lead to a smaller number than the one shown here.",
      bestandHinweis: "What is counted is the entire stock, not the selected period.",
      nichtErmittelbar: "—",
      nichtErmittelbarHinweis:
        "This number is counted afresh on every request, and the count hit the database time " +
        "limit. The other numbers on this page are in place.",
      dauer: {
        unterSekunde: "< 1 s",
        sekunden: "{wert} s",
        minuten: "{wert} min",
        stunden: "{wert} h",
        tage: "{wert} d",
      },
    },

    fehlerarten: {
      COMMIT_REJECTED: "Rejected by partner",
    },

    verteilung: {
      titelPartner: "By partner",
      titelRichtung: "By direction",
      bezeichnung: "Distribution",
      partner: "Partner",
      richtung: "Direction",
      EINGEHEND: "Inbound",
      AUSGEHEND: "Outbound",
      uebrige: "Others ({anzahl})",
      nichtZugeordnet: "not assigned",
      nichtZugeordnetHinweis:
        "Processes without a curated catalogue entry. Zero means everything is assigned.",
      keineVerweise: "The message list has no partner filter — these rows do not link anywhere.",
    },

    aufgefallen: {
      titel: "Recently noticed",
      leer: "Nothing was noticed during this period.",
      anzahlEins: "1 message",
      anzahlViele: "{anzahl} messages",
      zeileOeffnen: "Show these {anzahl} in the message list",
      kategorie: {
        FEHLER: "Error",
      },
    },

    stand: {
      satz: "Figures as of {zeitpunkt}",
      artVOLL: "full run",
      artDELTA: "incremental update",
      ohneLauf: "There has not been a completed rollup run yet.",
    },
  },

  platzhalter: {
    titel: "Work in progress",
    hinweis: "This view arrives in a later step.",
  },

  administration: {
    einleitung: "The areas reserved for EDI support.",
    bereichsnavigation: "Administration areas",
    bereiche: {
      katalog: {
        titel: "Process catalogue",
        beschreibung:
          "Curate partner and direction per process — the basis for evaluating transfers by partner later on.",
      },
      benutzer: {
        titel: "Users",
        beschreibung: "Create and lock accounts, and curate the tenants they may use.",
      },
    },
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
        gruppe: "{name} ({anzahl})",
        // Says *where* the values hang — on the message rather than on a step —
        // and claims nothing about what they are.
        gruppeNachricht: "Message",
        gruppeSchritt: "Step {nummer}",
      },

      // Raw data and logs (step 8). The family (`Converter`, `FTPSender`) is a
      // technical value from the property name and is never translated.
      dateien: {
        eingang: "Arrival",
        eingangHinweis:
          "What was stored when the message arrived. The arrival is not a step of the flow and therefore does not appear in the timeline.",
        schrittFamilie: "Step {nummer} · {familie}",
        ohneZeile: "Without a step in the timeline",
        zieleFehlgeschlagen: "The files for this message could not be loaded.",

        ziel: "{art} · {name}",
        zielAusschnitt: "{ziel} — {marke}",
        zuEigenschaften: "Show technical properties for {name}",

        ausschnittMarke: "Extract",
        ausschnittAnkuendigung:
          "Of this log you are shown the released section, not the complete file.",

        ansichtTitel: "File",
        zurueck: "Back to the message",
        art: {
          NUTZDATEN: "Payload",
          PROTOKOLL: "Log",
        },
        groesse: "{bytes} bytes",
        kodierung: "Encoding {name}",
        herunterladen: "Download",
        inhalt: "File content",

        vermerkAusschnitt:
          "You are seeing the released extract of this log. The download contains the same extract.",
        vermerkGekappt: "The view ends at the length limit. The download is not truncated.",
        vermerkMehrereEintraege: "The archive contained {anzahl} entries. The first one is shown.",

        binaerTitel: "Binary file",
        binaerText:
          "This file does not consist of readable text and is therefore not displayed. Its size is {bytes} bytes.",
        keinProtokollteilTitel: "No displayable log section",
        keinProtokollteilText:
          "This log contains no section that is shown to you. For many steps that is the normal case and does not mean that anything failed.",
        nichtVorhandenTitel: "File not present",
        nichtVorhandenText:
          "The file store answered and delivered no file for this entry. Its retention period may have expired.",
        ablageTitel: "File store unreachable",
        ablageText:
          "The file store is not answering right now. The file may still exist — try again in a few minutes.",
        leerTitel: "Empty file",
        leerText: "This file contains no characters at all.",
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

  prozesse: {
    nichtZugeordnet: "not assigned",
    ohneNamen: "Process without a name",

    richtung: {
      EINGEHEND: "Inbound",
      AUSGEHEND: "Outbound",
      nichtErmittelt: "not recorded",
    },

    baum: {
      bezeichnung: "Process tree",
      eingrenzung: "Narrow down by partner or process",
      eingrenzungLeeren: "Clear the filter",
      keineTreffer: "No partner and no process matches that.",
      leerTitel: "No process",
      leerHinweis:
        "The source holds no process for this tenant. Without a process there is no transfer " +
        "either — that is not a setting of this view.",
      nurMitVerkehr: "Only with traffic in the period",
      nurMitVerkehrHinweis:
        "Hides processes that carried nothing during the selected period. Leave it off when you " +
        "want to find out why nothing arrives.",
      verteilung: "{prozesse} processes — {bewegt} active, {still} silent, {nie} never used",
      gezeigt: "{sichtbar} of {gesamt} shown",
      ebenePartner: "Partner",
      ebeneRichtung: "Direction",
      ebeneProzess: "Process",
      anzahlProzesse: "Processes: {anzahl}",
      anzahlNachrichten: "Messages: {anzahl}",
      anzahlFehler: "Errors: {anzahl}",
      still: "nothing for over {monate} months",
    },

    liste: {
      leerTitel: "No process selected",
      leerHinweis:
        "Pick a process on the left. Its transfers for the selected period will show up here.",
      zurNachrichtenliste: "All transfers in the message list",
      zurueckZumBaum: "Back to the tree",
      nichtGefunden: "No process with this id in this tenant",
      leerImZeitraumTitel: "Nothing in this period",
      leerImZeitraum:
        "Nothing ran through this process during the selected period. A longer period shows " +
        "more — if it stays empty, the process carried nothing.",
      fenster: "Period {von} to {bis}",
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
      abgeschnittenPraefix:
        "More than {anzahl} matches — showing the {anzahl} most recent in the time window {von} to {bis}. Type more characters of the number, that narrows it down most. A smaller period helps less here.",
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
      belegartHilfe:
        "When you search for the beginning of a number only, a chosen document type also looks past leading zeros. Without a document type it cannot.",
      zeitraumHilfe:
        "Searching for the beginning of a number covers thirty days at a time. To look further back, move the period above the result list.",
    },

    praefix: {
      angebot: "Search for numbers that start with this?",
      angebotErwartung: "Longer numbers that begin with the one you typed will show up as well.",
      angebotKnopf: "Search for the beginning of the number",
      laeuft: "Searching for numbers that start with what you typed.",
      zurueck: "Search exactly again",
      zeitraum: "The period searched was {von} to {bis} — older messages are not covered by this.",
      fensterZuGross:
        "Searching for the beginning of a number covers at most {grenze} days; {angefragt} are selected.",
      fensterVerkleinern: "Narrow the period to {grenze} days",
    },

    abgebrochen:
      "The search took too long and was cancelled. Narrow the period or name a second document number.",
  },

  katalog: {
    spalten: {
      prozess: "Process",
      projekt: "Project",
      partner: "Partner",
      richtung: "Direction",
      bestand: "Messages",
      pflege: "Curation",
    },
    tabelle: "Processes of the active tenant",
    nichtZugeordnet: "not assigned",
    ohneNamen: "unnamed",
    auffangprozess: "Catch-all process",
    auffangprozessHinweis:
      "Whatever could not be assigned to a process ends up here. It is curated and counted like every other row.",
    richtungen: {
      EINGEHEND: "Inbound",
      AUSGEHEND: "Outbound",
    },
    pflegestatus: {
      OFFEN: "open",
      GEPFLEGT: "curated",
    },
    vermerk: {
      vorschlag: "Suggestion",
      keinVorschlag: "no suggestion could be derived",
      ohnePartner: "looked at, there is none",
    },
    bestand: {
      traegt: "carries messages",
      traegtNicht: "carries no messages",
      ungeprueft: "not checked",
      geprueftAm: "Stock checked on {zeitpunkt}",
      nieGeprueft: "No stock run has ever covered this row.",
    },
    filter: {
      bezeichnung: "Filters",
      nurOffene: "open only",
      nurOffeneHinweis: "Hides the rows that are already curated.",
      nurMitNachrichten: "with messages only",
      nurMitNachrichtenHinweis: "Unchecked rows stay visible.",
      gesperrt: "While a row is being edited, the list holds still.",
    },
    bearbeiten: {
      oeffnen: "Edit",
      gesperrt: "While this row is open, no second one can be opened.",
      partner: "Partner",
      partnerPlatzhalter: "Type a name or pick one",
      partnerLeeren: "Clear partner",
      vorschlaege: "Partner suggestions",
      richtung: "Direction",
      ohneRichtung: "none",
      ohnePartnerHinweis: "Saving now means: looked at, there is no partner.",
      speichern: "Save",
      speichernLaeuft: "Saving …",
      abbrechen: "Cancel",
    },
    fortschritt: {
      satz: "{gepflegt} of {gesamt} processes curated",
      alleZaehlenMit: "Dead processes and the catch-all process count too.",
    },
    hinweis: {
      ohnePartnervorschlag: "no partner could be suggested for any process",
      ohnePartnervorschlagFolge:
        "The partners of this tenant have to be typed by hand. The suggestion list fills up with every one you save.",
    },
    masse: {
      oeffnen: "Bulk assignment …",
      titel: "Bulk assignment by project",
      einleitung:
        "Sets one field for every process of a project. Rows that are already curated get overwritten — that is intended, and it is the reason for the preview.",
      projekt: "Project",
      projektWaehlen: "Pick a project",
      feld: "Field",
      felder: {
        PARTNER: "Partner",
        RICHTUNG: "Direction",
      },
      wert: "Value",
      leerHinweis: "An empty value clears the field in every affected row.",
      vorschauHolen: "Preview",
      vorschauLaeuft: "Checking …",
      ausfuehren: "Apply",
      ausfuehrenLaeuft: "Applying …",
      abbrechen: "Cancel",
      vorschauNoetig: "Fetch the preview first — it names how many rows this hits.",
      betroffenKeine: "This project carries no process. There is nothing to set.",
      betroffenEins: "Affects one process.",
      betroffenViele: "Affects {betroffen} processes.",
      verlorenKeine: "None of them is curated yet.",
      verlorenEins: "One of them is already curated — its current value is lost.",
      verlorenViele: "{gepflegt} of them are already curated — their current values are lost.",
    },
    uebernahme: {
      oeffnen: "Accept suggestions",
      titel: "Accept partner suggestions",
      einleitung:
        "Confirms the suggestions the rules found during the last run. No value changes — only the curation status.",
      keine: "There are no unconfirmed partner suggestions.",
      uebernimmtEins: "Accepts one partner suggestion",
      uebernimmtViele: "Accepts {betroffen} partner suggestions",
      aufteilung: "— {regelA} from rule A, {regelB} from rule B.",
      keinen: "none",
      einen: "one",
      folge:
        "The rows get marked as curated; partner and direction stay exactly as the rule suggested them.",
      ohneVorschlagBleibtOffen:
        "Processes without a partner suggestion stay open, even when they carry a direction.",
      filterWirktNicht:
        "The “with messages only” filter has no effect here — the tenant’s processes are all taken.",
      uebernehmen: "Accept",
      uebernehmenLaeuft: "Accepting …",
      abbrechen: "Cancel",
    },
    lauf: {
      starten: "Derive suggestions and check stock",
      hinweis:
        "Creates missing rows, refreshes open suggestions and checks for every row whether messages hang off it.",
      laeuft: "Running …",
      laeuftSeit: "running for {dauer}",
      ergebnisTitel: "The last run",
      ergebnisDauer: "took: {dauer}",
      zahlen: {
        angelegt: "created",
        aufgefrischt: "refreshed",
        unberuehrt: "untouched",
        regelA: "partner from rule A",
        regelB: "partner from rule B",
        keine: "without a partner suggestion",
        bestandGeprueft: "stock checked",
        ohneNachrichten: "of those without messages",
      },
      dauer: {
        unterSekunde: "< 1 s",
        sekunden: "{wert} s",
        minuten: "{wert} min",
        stunden: "{wert} h",
        tage: "{wert} d",
      },
    },
    leer: {
      titel: "No process",
      ohneProzesse: "No processes are on file for this tenant.",
      allesGepflegt:
        "Every row of this tenant is curated. Clear the “open only” box to see all of them.",
      filterLeer: "No process matches the filters in place. Clear a box to see more.",
    },
  },

  /**
   * User administration (step 9a) — the feature itself. The page **name** stays
   * in `administration.bereiche.benutzer.titel`, the **role** names in `rolle`.
   */
  benutzer: {
    tabelle: "All accounts, across all tenants",
    mandantenfrei:
      "This list spans all tenants: it shows every account regardless of the tenant selected above. Switching there does not change it.",
    spalten: {
      benutzer: "User",
      rolle: "Role",
      mandanten: "Tenants",
      sperre: "Lock",
      zeitsperre: "Timed lock",
      aktiv: "Account",
      passwort: "Password",
      letzteAnmeldung: "Last sign-in",
      aktionen: "Edit",
    },
    ohneMandanten: "none",
    bearbeiten: "Edit",
    bearbeitenFuer: "Edit account {benutzer}",
    bearbeitenGesperrt:
      "Close the open row first. Otherwise the entries started there would be lost — including a password already typed.",
    formular: {
      sperre: "Account locked",
      sperreHinweis:
        "Locks the account indefinitely. Unlocking also clears a running timed lock and the failed-attempt counter.",
      aktiv: "Account active",
      aktivHinweis:
        "Deactivated accounts can no longer sign in. Nothing is ever deleted — their audit entries would become unreadable.",
      rolle: "Role",
      rolleOhneMandant:
        "Without tenants this account cannot be downgraded to “Tenant”. Assign at least one first.",
      passwort: "Set a new password",
      passwortSetzen: "Set password",
      passwortHinweis:
        "At least {laenge} characters, and it must differ from the current one. The account has to change it at the next sign-in. Pass it on by a route you choose yourself — it is not shown here again.",
      eigenesKonto:
        "This is your own account. Any change to it signs you out, and you will have to sign in again.",
    },
    anlegen: {
      oeffnen: "Add account",
      schliessen: "Close the form",
      gesperrt:
        "Close the open row first. Otherwise the entries started there would be lost — including a password already typed.",
      titel: "New account",
      passwortHinweis:
        "You type the one-time password yourself — at least {laenge} characters. The account has to change it at the first sign-in.",
      mandantenHinweis:
        "Exactly one tenant when the account is created. Further ones are added afterwards on the account’s row.",
      benutzername: "Username",
      rolle: "Role",
      mandant: "Tenant",
      waehlen: "Please choose",
      passwort: "One-time password",
      absenden: "Create",
      laeuft: "Creating …",
      erfolgTitel: "Account created",
      erfolgText:
        "{benutzer} — {rolle}, tenant {mandant}. The account is active and has to change its password at the first sign-in. Pass the one-time password on by a route you choose yourself: it is not shown here again.",
    },
    mandanten: {
      titel: "Tenants",
      letzteZuordnung:
        "An account needs at least one tenant. Tick a second one first, then this one can be cleared.",
      unbekannt: "no longer in the tenant list",
      speichern: "Save tenants",
      verwerfen: "Discard",
    },
    vorwarnung: {
      titel: "This is your own account",
      text: "“{vorgang}” affects your own account.",
      folge:
        "The operation will go through — and because it discards every session of this account, you will be signed out at once and land on the sign-in page. You can sign in again right afterwards.",
      vorgaenge: {
        sperre: "Change lock",
        aktiv: "Activate or deactivate account",
        rolle: "Change role",
        mandanten: "Change tenants",
        passwort: "Set password",
      },
      bestaetigen: "Run and sign me out",
      laeuft: "Running …",
      abbrechen: "Cancel",
    },
    sperre: {
      gesperrt: "locked",
      offen: "not locked",
    },
    zeitsperre: {
      bis: "until {zeitpunkt}",
      keine: "none",
    },
    aktiv: {
      aktiv: "active",
      deaktiviert: "deactivated",
    },
    passwort: {
      wechselNoetig: "change required",
      keinWechsel: "set",
    },
    anmeldung: {
      nie: "never signed in",
    },
    leer: {
      titel: "No accounts",
      hinweis:
        "The list is empty. That cannot happen in practice — whoever sees it has an account. Please report this to EDI support.",
    },
  },

  zustand: {
    laedt: "Loading …",
    leerTitel: "Nothing to show",
    fehlerTitel: "That did not work",
    erneutVersuchen: "Try again",
    kennung: "Error reference",
    kennungHinweis: "Quote this reference when you report the problem.",
    keinZugriffTitel: "No access",
  },

  fehler: {
    // Sign-in — deliberately unspecific. The wording does not distinguish an
    // unknown user name from a wrong password, because the backend does not
    // either.
    "anmeldung-abgelehnt": "User name or password is wrong.",
    "konto-gesperrt":
      "The account is locked for a while after several failed attempts. Try again later or contact EDI support.",
    "konto-administrativ-gesperrt":
      "EDI support has locked this account. This lock does not expire on its own — please contact them.",
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
    "zeitfenster-zu-genau": "Both points in time must be on the full hour.",
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

    "partner-zu-lang": "A partner name may hold at most 100 characters.",
    "richtung-unbekannt": "Pick “Inbound”, “Outbound” — or no direction at all.",
    "feld-unbekannt": "Pick exactly one of the fields partner or direction.",
    "modus-unbekannt": "There is no such mode.",
    "limit-ungueltig": "This page size is not allowed.",
    "cursor-ungueltig": "The page position is no longer valid. Start again on page one.",
    "altes-passwort-falsch": "The current password is not correct.",
    "passwort-zu-kurz": "The new password needs at least twelve characters.",
    "passwort-unveraendert": "The new password must differ from the current one.",

    // User administration (step 9a). The four `409` are not permission problems:
    // the input is fine, the state of the account rules it out. Each says what to
    // do so that it works — that is the difference from a `403`.
    selbstschutz:
      "Not on your own account — locking, deactivating and downgrading are ruled out there. Another administrator can do it.",
    "letzter-admin":
      "This is the last usable administrator. Make another account an active, unlocked administrator first, then it will work.",
    "letzte-mandantenzuordnung":
      "An account needs at least one tenant. Assign a different one before removing this one.",
    "rolle-ohne-mandant":
      "This account has no tenant. Assign at least one first, then it can be downgraded.",
    "unbekannte-rolle": "No such role. Choose “EDI support” or “Tenant”.",

    // Creating accounts from the interface (step 9c).
    "benutzername-vergeben":
      "That username already exists. Choose a different one — an existing account is never overwritten.",
    "benutzername-zu-lang": "A username may have at most 100 characters.",

    // Technical
    "technischer-fehler":
      "A technical error occurred. Please report the error reference shown below.",
    netzwerk: "The backend cannot be reached right now.",
    unbekannt: "An unexpected error occurred.",
  },
};
