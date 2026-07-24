import type { Metadata } from "next";

export const metadata: Metadata = { title: "Administration" };

/** Platzhalter. Benutzerverwaltung und Katalogpflege entstehen in Schritt 9, nur fuer Rolle ADMIN. */
export default function AdministrationPage() {
  return (
    <main>
      <h1>Administration</h1>
    </main>
  );
}
