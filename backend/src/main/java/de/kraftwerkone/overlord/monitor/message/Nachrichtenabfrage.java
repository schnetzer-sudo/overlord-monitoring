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
 * <p><b>Hier standen bis zum 03.09.2026 zwei weitere Bestandteile:</b> {@code ueberfaellig} (die
 * zweite Abfrageform, E‑j) und {@code jetzt} (ihr Stichtag aus der Anwendungsuhr). Beide sind mit
 * E‑71 entfallen — die Problemkategorie <i>Ueberfaellig</i> ist widerlegt, und {@code jetzt} hatte
 * ausser ihr keinen Verbraucher. <b>Die Liste liest damit keine Uhr mehr</b>, ausser fuer die
 * Aufloesung des Zeitfensters, und die geschieht in {@code NachrichtenFilter}.
 *
 * @param suchtreffer {@code null}, wenn nicht gesucht wurde — leere Listen hiessen „nichts
 *     gefunden" und waeren etwas anderes
 */
public record Nachrichtenabfrage(
    Zeitfenster fenster,
    Set<MessageStatusKind> status,
    List<String> prozessIds,
    Suchtreffer suchtreffer,
    boolean absteigend,
    Seitenposition cursor,
    int limit) {

  public static Nachrichtenabfrage aus(NachrichtenFilter filter, Suchtreffer suchtreffer) {
    return new Nachrichtenabfrage(
        filter.fenster(),
        filter.status(),
        filter.prozessIds(),
        suchtreffer,
        filter.sortierung().absteigend(),
        filter.cursor(),
        filter.limit());
  }
}
