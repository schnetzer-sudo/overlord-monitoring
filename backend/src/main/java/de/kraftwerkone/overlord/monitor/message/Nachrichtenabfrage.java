package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.util.List;
import java.util.Set;

/**
 * Der Filter, wie ihn das Repository braucht: gepruefte Parameter <b>plus</b> der bereits
 * aufgeloeste Suchbegriff.
 *
 * <p>Getrennt von {@link NachrichtenFilter}, weil die Aufloesung des Suchbegriffs selbst eine
 * Abfrage ist. Das Repository bekommt damit ausschliesslich Werte, aus denen es Bedingungen bauen
 * kann, und muss keine Reihenfolge von Abfragen kennen.
 *
 * @param suchtreffer {@code null}, wenn nicht gesucht wurde — leere Listen hiessen „nichts
 *     gefunden" und waeren etwas anderes
 */
public record Nachrichtenabfrage(
    Zeitfenster fenster,
    Set<MessageStatusKind> status,
    List<String> prozessIds,
    Suchtreffer suchtreffer,
    boolean zwischenschritte,
    boolean absteigend,
    Seitenposition cursor,
    int limit) {

  public static Nachrichtenabfrage aus(NachrichtenFilter filter, Suchtreffer suchtreffer) {
    return new Nachrichtenabfrage(
        filter.fenster(),
        filter.status(),
        filter.prozessIds(),
        suchtreffer,
        filter.zwischenschritte(),
        filter.sortierung().absteigend(),
        filter.cursor(),
        filter.limit());
  }
}
