"use client";

import { X } from "lucide-react";

import { cn } from "@/lib/utils";

/**
 * **Die Marke — eine Gestalt, ein Ort.**
 *
 * Gedämpfte Fläche, kleiner Radius, kein Rahmen. Sie entstand in Schritt 7,
 * Teil 1 für die Belegdaten im Detail und trägt seit Teil 3 auch die Begriffe der
 * Suche. Genau deshalb steht sie hier und nicht in einem Feature: **Es soll keine
 * zweite Marken-Gestalt im Projekt geben**, und zwei Nachbauten desselben
 * Aussehens liefen irgendwann auseinander.
 *
 * ## Die Marke ist die Wortgrenze, nicht Schmuck
 *
 * Das ist der tragende Satz dieser Bauform, und er ist gemessen: **2,97 Prozent**
 * der BAM-Werte tragen ein Leerzeichen *innen* (E7, 27.792 von 936.529 über einen
 * Monat), angeführt von Typ 9018 — dem, den `NEXANS` auf 92,26 Prozent seiner
 * Wurzeln trägt (M39). `0Z3 915 902 D` ist **ein** Wert und nicht vier. Ein
 * Trennzeichen taugt dafür nicht: Das naheliegende wäre das Leerzeichen, und
 * genau das steht in den Daten; für jedes andere ist ungemessen, ob ein Wert es
 * enthält (Regel Q4). Die Marke macht die Grenze zu einer Eigenschaft der
 * **Darstellung** und muss die Frage gar nicht beantworten.
 *
 * ## Keine Statusfarbe, keine neue Farbrolle
 *
 * Sie sagt nichts über einen Zustand und nutzt deshalb den vorhandenen gedämpften
 * Flächenton (`docs/visuelles-konzept.md` §3).
 *
 * ## Die Bedienbarkeit ist ein Schalter — und sie springt nicht von selbst an
 *
 * Ohne {@link Marke.aufEntfernen} ist die Marke **kein Knopf**: kein
 * Zeigerwechsel, kein Fokusrahmen, kein `title`. So steht sie im Belegdaten-Block
 * ({@code docs/bam-werte.md} §11), und daran ändert sich nichts. Erst wer eine
 * Schließen-Schaltfläche mitgibt, bekommt ein Bedienelement — und dann ist der
 * Knopf **in** der Marke der Bedienbare, nicht die Marke selbst. Ein Klick auf
 * den Text tut auch dort nichts.
 */

/**
 * Die Gestalt als Klassenliste — für Stellen, die ihr eigenes Element rendern.
 *
 * Der Belegdaten-Block setzt sie unmittelbar auf sein `<li>`, statt eine Marke
 * darin zu verschachteln: Sein Baum bleibt damit derselbe wie vor dieser
 * Zusammenführung, und geteilt wird trotzdem genau eine Zeichenkette.
 */
export const MARKE_GESTALT = "bg-muted text-beiwerk min-w-0 rounded-sm px-1.5 py-0.5 break-words";

export function Marke({
  children,
  aufEntfernen,
  entfernenText,
  hervorgehoben = false,
  className,
}: {
  children: React.ReactNode;
  /**
   * Macht die Marke zum Bedienelement. **Ohne sie bleibt sie eine Anzeige** —
   * siehe oben.
   */
  aufEntfernen?: () => void;
  /** Beschriftung der Schließen-Schaltfläche; Pflicht, sobald es eine gibt. */
  entfernenText?: string;
  /**
   * Kurz hervorgehoben — für den Fall, dass derselbe Begriff ein zweites Mal
   * abgelegt werden sollte. Es entsteht dann **keine zweite Marke**; die
   * vorhandene meldet sich stattdessen.
   */
  hervorgehoben?: boolean;
  className?: string;
}) {
  return (
    <span
      className={cn(
        MARKE_GESTALT,
        // Mit Schaltfläche wird aus der Marke eine Zeile: Der Knopf braucht die
        // Mindestfläche am Finger (`--dichte-bedienelement` fällt am
        // Berührungsgerät auf 44 px zurück), und die Marke wächst mit ihm.
        aufEntfernen !== undefined && "min-h-bedienelement inline-flex items-center gap-1 py-0",
        // Kein Übergang und keine Bewegung — das visuelle Konzept lässt außer
        // Schublade und Menü keine zu. Der Ring ist da oder er ist es nicht.
        hervorgehoben && "ring-ring ring-2",
        className,
      )}
    >
      <span className="min-w-0 break-words">{children}</span>
      {aufEntfernen === undefined ? null : (
        <button
          type="button"
          onClick={aufEntfernen}
          // `aria-label` **und** `title`: das eine für das Vorleseprogramm, das
          // andere für den Zeiger.
          aria-label={entfernenText}
          title={entfernenText}
          className="hover:bg-background focus-visible:ring-ring size-bedienelement -mr-1 flex shrink-0 cursor-pointer items-center justify-center rounded-sm focus-visible:ring-2 focus-visible:outline-none"
        >
          <X aria-hidden="true" className="size-3.5 opacity-70" />
        </button>
      )}
    </span>
  );
}
