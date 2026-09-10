package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.LocalDateTime;

/**
 * Eine Zeile aus {@code Service}, wie {@link DienstLeseRepository} sie liefert — <b>drei Spalten
 * und keine vierte</b>.
 *
 * <p><b>Was hier fehlt, fehlt absichtlich</b> (E‑122): {@code ServiceName}, {@code
 * ServiceDescription}, {@code ServiceLastStatusMessage} und {@code ServiceConnectString} werden
 * nicht einmal gelesen. Die drei Texte sind Betriebstexte des Altsystems und fuer den Nutzer ohne
 * Wert; die Verbindungszeichenkette traegt einen Hostnamen und faellt unter Regel G1. <b>Eine
 * Spalte, die gar nicht gelesen wird, kann in keiner Antwort landen</b> — das ist der strengere
 * Schutz als ein Feld, das man beim Zusammenbau weglaesst.
 *
 * <p>Der Satz traegt deshalb auch <b>keinen</b> eigenen {@code toString()}: Es gibt nichts zu
 * verdecken.
 *
 * @param serviceId die Kennung, und sie ist das Einzige, was angezeigt wird
 * @param rohwert {@code ServiceStatus}, unveraendert — auch {@code null}. Die Einordnung bildet
 *     {@link DienstStatusClassifier}
 * @param stand {@code ServiceLastUpdate} als <b>Wanduhrzeit des Quellservers</b>, nicht konvertiert
 *     ({@code docs/datenzugriff.md} §7). Auch {@code null} zulaessig
 */
public record Dienstzeile(String serviceId, String rohwert, LocalDateTime stand) {}
