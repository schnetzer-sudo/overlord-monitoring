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
    "kein-mandant-gewaehlt": "Select a tenant first.",
    "passwortwechsel-erforderlich": "Set a new password first.",

    // 404 — never says anything about permission. A foreign and a non-existent
    // resource must stay indistinguishable; a hint about missing access would
    // reveal exactly the difference the backend hides.
    "nicht-gefunden": "What you are looking for does not exist.",

    // Input
    "eingabe-ungueltig": "Please check the marked fields.",
    "anfrage-ungueltig": "This request could not be processed.",
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
