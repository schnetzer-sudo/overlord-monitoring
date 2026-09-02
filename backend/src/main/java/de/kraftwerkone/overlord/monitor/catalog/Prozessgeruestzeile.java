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
 * @param processId die {@code ProcessID} — Schluessel gegen {@link Prozesskennzahlzeile}
 * @param processName der Anzeigename. Darf {@code null} sein; was der Nutzer dann liest, gehoert in
 *     die Sprachdateien (Regel Q4)
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
    String partner,
    String richtung,
    String pflegestatus,
    LocalDateTime letzteBewegung) {}
