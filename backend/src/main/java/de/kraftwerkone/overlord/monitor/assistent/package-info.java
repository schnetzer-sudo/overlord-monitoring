/**
 * Der Assistent im Backend (Linie {@code assistent}, Teilschritt A2, {@code docs/assistent.md}):
 * die Endpunkte unter {@code /api/assistent}, die Gespraechsfuehrung, die Werkzeuge aus den
 * Typdefinitionen, die feste Anweisung, der Gespraechsspeicher und die Begrenzung je Nutzer.
 *
 * <p><b>Der Assistent legt nie einen Auftrag an.</b> Er schlaegt vor; anlegen tut der Klick des
 * Kunden ueber {@code POST /api/tasks}. Dieses Paket kennt weder {@code task} noch die Datenbank
 * ({@code PaketstrukturTest}); Typdefinition, Typkatalog und Pruefung kommen aus {@code
 * common.auftrag} (E‑1037).
 *
 * <p><b>Das Modell ist austauschbar</b> (E‑1032): {@code assistent.modell} ist die neutrale
 * Schnittstelle, {@code assistent.anthropic} der erste Adapter. Hier im Paket haengt nichts an
 * einem Adapter.
 *
 * <p><b>Abgeschaltet, bis der Datenschutzbeauftragte geprueft hat</b> (E‑1029, E‑1030).
 */
package de.kraftwerkone.overlord.monitor.assistent;
