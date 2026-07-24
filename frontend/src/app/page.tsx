import Link from "next/link";

/**
 * Platzhalter-Einstieg. Ab Schritt 3 fuehrt eine nicht angemeldete Anfrage zur
 * Anmeldung, ab Schritt 10 ist das Dashboard die Landingpage.
 */
const ROUTEN = [
  { pfad: "/anmeldung", name: "Anmeldung" },
  { pfad: "/dashboard", name: "Dashboard" },
  { pfad: "/nachrichten", name: "Nachrichten" },
  { pfad: "/prozesse", name: "Prozesse" },
  { pfad: "/administration", name: "Administration" },
];

export default function StartseitePage() {
  return (
    <main>
      <h1>Overlord Monitoring</h1>
      <p>Schritt 1 — Fundament. Die folgenden Routen sind noch leere Platzhalter.</p>
      <ul>
        {ROUTEN.map((route) => (
          <li key={route.pfad}>
            <Link href={route.pfad}>{route.name}</Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
