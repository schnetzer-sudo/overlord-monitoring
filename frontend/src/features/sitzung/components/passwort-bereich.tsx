"use client";

import { PasswortFormular } from "./passwort-formular";
import { useSelbstauskunft } from "../hooks";

/**
 * Verbindet die Selbstauskunft mit dem Formular: erzwungen oder freiwillig.
 *
 * Der Anwendungsrahmen rendert seine Kinder erst, wenn die Auskunft vorliegt —
 * hier kommt sie deshalb aus dem Zwischenspeicher und erzeugt keine zweite
 * Anfrage.
 */
export function PasswortBereich() {
  const { data: auskunft } = useSelbstauskunft();
  return <PasswortFormular zwang={auskunft?.mustChangePassword ?? false} />;
}
