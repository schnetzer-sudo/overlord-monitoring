/**
 * Was Auftraege und Assistent gemeinsam brauchen (Linie {@code assistent}, seit A2): die
 * Typdefinition, ihre Pruefung beim Laden, der Typkatalog samt Lesen der Typzeilen und die Pruefung
 * eines Auftrags mit ihren Fehlercodes.
 *
 * <p><b>Hier gewandert aus {@code task}</b>, weil der Assistent sie als zweiter Verbraucher braucht
 * und Fachpakete einander nicht kennen (E‑1037, {@code docs/assistent.md} §4.1). Verhalten und
 * Statement sind unveraendert; beschrieben in {@code docs/tasks.md} §3 und §5.
 *
 * <p><b>Kein Typ steht im Code</b> (E‑1018), und {@code GlassfishDB} fasst dieses Paket nicht an.
 */
package de.kraftwerkone.overlord.monitor.common.auftrag;
