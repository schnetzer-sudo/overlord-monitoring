package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.time.LocalDateTime;
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
 * @param ueberfaellig nur ueberfaellige Nachrichten (E-j)
 * @param jetzt der Stichtag der Ueberfaelligkeit, aus der <b>Anwendungsuhr</b> (Regel Z1). Er steht
 *     hier und nicht im Repository, weil ein Repository keine Uhr liest — und er steht auch dann
 *     hier, wenn {@code ueberfaellig} aus ist: Ein Feld, das je nach Nachbarfeld gesetzt ist oder
 *     nicht, ist die Sorte Zustand, die man beim Lesen uebersieht
 */
public record Nachrichtenabfrage(
    Zeitfenster fenster,
    Set<MessageStatusKind> status,
    List<String> prozessIds,
    Suchtreffer suchtreffer,
    boolean ueberfaellig,
    LocalDateTime jetzt,
    boolean absteigend,
    Seitenposition cursor,
    int limit) {

  public static Nachrichtenabfrage aus(
      NachrichtenFilter filter, Suchtreffer suchtreffer, LocalDateTime jetzt) {
    return new Nachrichtenabfrage(
        filter.fenster(),
        filter.status(),
        filter.prozessIds(),
        suchtreffer,
        filter.ueberfaellig(),
        jetzt,
        filter.sortierung().absteigend(),
        filter.cursor(),
        filter.limit());
  }
}
