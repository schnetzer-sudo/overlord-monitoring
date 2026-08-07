"use client";

import { useEffect, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useSelbstauskunft } from "@/features/sitzung/hooks";
import { useTexte } from "@/i18n/provider";
import { zielRoute, type Sitzungszustand } from "@/lib/ablauf";
import { istNichtAngemeldet } from "@/lib/http";
import { Skeleton } from "@/components/ui/skeleton";

import { Fehler } from "./zustand";
import { Kopfzeile } from "./kopfzeile";
import { NavigationsListe } from "./navigations-liste";
import { ZeitzoneProvider } from "./zeitzone";

/**
 * Der Anwendungsrahmen: Kopfzeile, Navigation, Inhalt — und der Ablauf nach dem
 * Anmelden.
 *
 * **Der Rahmen steht, nur der Inhalt scrollt.** Er füllt Breite und Höhe des
 * Fensters; Kopfzeile und Navigationsspalte bleiben stehen, die Bildlaufleiste
 * gehört allein dem Inhaltsbereich. Eine Maximalbreite gibt es hier bewusst
 * nicht — sie darf nur *innerhalb* einer Ansicht mit Fließtext stehen. Ab
 * Schritt 4 sitzt in diesem Inhaltsbereich eine Nachrichtenliste mit
 * feststehender Tabellenkopfzeile; ohne den eigenen Scrollbereich wäre die
 * nachträglich teuer.
 *
 * **Er trifft keine Berechtigungsentscheidung.** Er zeigt dem Nutzer den Weg
 * (Änderungszwang → Mandantenauswahl → Startseite), damit er nicht gegen eine
 * Fehlermeldung läuft. Was er tatsächlich darf, entscheidet ausschließlich das
 * Backend; die Entscheidung hier ließe sich in der Entwicklerkonsole in zwei
 * Sekunden umgehen und schützt deshalb nichts.
 *
 * Die Verzweigung selbst steht in `lib/ablauf.ts` — ohne React, ohne Netzwerk,
 * damit sie prüfbar ist.
 */
export function Anwendungsrahmen({ children }: { children: ReactNode }) {
  const texte = useTexte();
  const router = useRouter();
  const pfad = usePathname();
  const { data: auskunft, error, isPending, refetch } = useSelbstauskunft();

  const zustand: Sitzungszustand = isPending
    ? { art: "laedt" }
    : auskunft
      ? {
          art: "angemeldet",
          passwortwechselNoetig: auskunft.mustChangePassword,
          mandantAktiv: auskunft.mandant !== null,
        }
      : istNichtAngemeldet(error)
        ? { art: "abgemeldet" }
        : { art: "laedt" };

  const ziel = zielRoute(zustand, pfad);

  useEffect(() => {
    if (ziel !== null) {
      // Bei „abgemeldet" leitet zusätzlich der QueryClient hart um. Die
      // Doppelung ist Absicht: Dass ein abgemeldeter Nutzer den Rahmen eines
      // anderen zu sehen bekommt, ist der eine Fall, der nie eintreten darf.
      router.replace(ziel);
    }
  }, [ziel, router]);

  useEffect(() => {
    /*
     * Zurück im Browser darf nach dem Abmelden keine alten Daten zeigen.
     *
     * Der übliche Weg dahin wäre `Cache-Control: no-store`. Den vergibt bei
     * dynamisch gerenderten Seiten aber Next.js selbst und überschreibt dabei
     * sowohl `proxy.ts` als auch `headers()` aus `next.config.ts` — beides
     * ausprobiert, beides wirkungslos (siehe `proxy.ts`). Ohne `no-store` legt
     * der Browser die Seite in den Zurück-Vorwärts-Zwischenspeicher: Ein
     * „Zurück" holt sie samt eingefrorenem Speicherzustand hervor, **ohne** eine
     * einzige Anfrage — an der ein `401` auffallen könnte.
     *
     * `event.persisted` ist genau dieser Fall. Neu laden schickt die Anfrage
     * durch die Routensperre, und ohne Sitzungs-Cookie landet der Nutzer auf der
     * Anmeldung.
     */
    function beiWiederherstellung(ereignis: PageTransitionEvent) {
      if (ereignis.persisted) {
        window.location.reload();
      }
    }
    window.addEventListener("pageshow", beiWiederherstellung);
    return () => window.removeEventListener("pageshow", beiWiederherstellung);
  }, []);

  // Technischer Fehler — nicht 401, denn der bedeutet „abgemeldet" und wird
  // umgeleitet, nicht angezeigt.
  if (!isPending && !auskunft && !istNichtAngemeldet(error)) {
    return (
      <main className="max-w-inhalt mx-auto px-3 py-8 md:px-6">
        <Fehler fehler={error} aufWiederholen={() => void refetch()} />
      </main>
    );
  }

  if (!auskunft || ziel !== null) {
    return <RahmenSkelett hinweis={texte.zustand.laedt} />;
  }

  // Solange das Passwort gewechselt werden muss, führt jede Navigation ohnehin
  // hierher zurück — ein Menü wäre dann eine Einladung in die Sackgasse.
  const frei = !auskunft.mustChangePassword && auskunft.mandant !== null;

  return (
    // Die Anzeigezone kommt aus der Selbstauskunft und gilt für alles darunter.
    // Der Rahmen ist die einzige Stelle, die sie hat, und jede Zeitangabe in
    // jedem Feature braucht sie — siehe `components/zeitzone.tsx`.
    <ZeitzoneProvider zone={auskunft.anzeigezone}>
      {/* `relative` ist hier kein Feinschliff — siehe den Kommentar an `main`. */}
      <div className="relative flex h-dvh flex-col overflow-hidden">
        <Kopfzeile
          auskunft={auskunft}
          navigationSichtbar={frei}
          mandantenwechselErlaubt={!auskunft.mustChangePassword}
        />
        {/* `min-h-0` ist hier kein Feinschliff, sondern die Bedingung: Ohne ihn
            wächst ein Flex-Kind über seinen Container hinaus, statt zu scrollen —
            und das Fenster bekommt eine zweite Bildlaufleiste. */}
        <div className="flex min-h-0 flex-1">
          {frei ? (
            <aside className="border-border w-navspalte relative hidden shrink-0 overflow-y-auto border-r px-2.5 py-2 md:block">
              <NavigationsListe rolle={auskunft.role} />
            </aside>
          ) : null}
          {/*
           * `relative` an jedem Scrollbereich, und zwar aus einem gemessenen Grund
           * (06.08.2026): Ein absolut positioniertes Element ohne positionierten
           * Vorfahren hängt am *Ursprungsblock der Seite* — und wird deshalb von
           * `overflow-hidden` weiter oben **nicht** beschnitten. Sein Platz zählt
           * dann zur Scrollfläche des Dokuments.
           *
           * Das ist kein theoretischer Fall: Tailwinds `sr-only` ist
           * `position: absolute`. Die verborgene Beschriftung im Aktualisieren-Knopf
           * unter der Tabelle lag damit 2.243 px unter dem Seitenanfang, das Dokument
           * bekam 1.354 px Scrollfläche ohne einen einzigen sichtbaren Inhalt, und
           * wer über das Listenende hinausscrollte, schob den gesamten
           * Anwendungsrahmen aus dem Bild.
           *
           * Mit `relative` ist der Scrollbereich selbst der Bezug: Was in ihm liegt,
           * scrollt mit ihm und wird von ihm beschnitten. Begründung und Messung in
           * `docs/frontend-grundlagen.md` §7.
           */}
          <main className="relative min-h-0 min-w-0 flex-1 overflow-y-auto px-3 py-4 md:px-5">
            {children}
          </main>
        </div>
      </div>
    </ZeitzoneProvider>
  );
}

function RahmenSkelett({ hinweis }: { hinweis: string }) {
  return (
    <div className="relative flex h-dvh flex-col overflow-hidden" aria-busy="true">
      <span className="sr-only">{hinweis}</span>
      <div className="bg-card border-border h-kopfzeile shrink-0 border-b" />
      <div className="relative min-h-0 flex-1 space-y-3 overflow-y-auto px-3 py-4 md:px-5">
        <Skeleton className="h-zeile w-48" />
        <Skeleton className="h-zeile w-full" />
        <Skeleton className="h-zeile w-2/3" />
      </div>
    </div>
  );
}
