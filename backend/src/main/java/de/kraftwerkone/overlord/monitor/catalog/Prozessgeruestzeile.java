package de.kraftwerkone.overlord.monitor.catalog;

import java.time.LocalDateTime;

/**
 * Eine Zeile des <b>Baumgeruests</b>: ein Prozess des Mandanten mit seinen kuratierten Feldern und
 * seiner letzten Bewegung — <b>ohne jede Kennzahl des Zeitfensters</b>.
 *
 * <p>Das Geruest steht fuer sich, weil es fensterunabhaengig ist: Es zaehlt <b>alle</b> Prozesse
 * des Mandanten, auch die, die im gewaehlten Zeitfenster keine einzige Nachricht getragen haben —
 * und gerade die sind der interessante Fall, wenn jemand wissen will, warum nichts ankommt
 * (dieselbe Ueberlegung wie in {@code docs/prozessauswahl.md} §3).
 *
 * <p><b>Eine Zeile fuer beide Gliederungen</b> <i>(seit 15.09.2026)</i>. Partner- und Projektbaum
 * brauchen dieselben Zeilen; gruppiert wird im Dienst. Die Zeile traegt dafuer beide Felder, nach
 * denen gegliedert werden kann — die kuratierten und {@link #projectDescription}.
 *
 * @param processId die {@code ProcessID} — Schluessel gegen {@link Prozesskennzahlzeile}
 * @param processName der Anzeigename. Darf {@code null} sein; was der Nutzer dann liest, gehoert in
 *     die Sprachdateien (Regel Q4)
 * @param projectDescription {@code Project.ProjectDescription} als Rohwert — der Gruppenschluessel
 *     der Gliederung {@code PROJEKT}. <b>Nicht die {@code ProjectID}</b>: Mehrere Projekte koennen
 *     dieselbe Beschreibung tragen und sollen dann ein Knoten sein (E-141). Die Spalte ist {@code
 *     TEXT} und im Schema {@code NULL}-faehig; eine Rueckfallregel dafuer gibt es nicht (E-144)
 * @param partner der kuratierte Partner als <b>Rohwert</b>. Ob er als zugeordnet gilt, entscheidet
 *     {@code common/Katalogzuordnung} und nicht diese Zeile
 * @param richtung die kuratierte Richtung als Rohwert, ebenso
 * @param pflegestatus der Rohwert aus {@code process_catalog.pflegestatus}, {@code null} wenn es
 *     gar keine Katalogzeile gibt
 * @param letzteBewegung der juengste Rollupeimer dieses Prozesses in der <b>Wanduhrzeit des
 *     Quellservers</b>, {@code null} wenn es keinen gibt. {@code null} heisst {@link
 *     Prozesszustand#NIE}
 */
public record Prozessgeruestzeile(
    String processId,
    String processName,
    String projectDescription,
    String partner,
    String richtung,
    String pflegestatus,
    LocalDateTime letzteBewegung) {}
