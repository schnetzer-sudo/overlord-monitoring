package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Die Kopfzahlen ueber den ganzen Baum — damit die Oberflaeche nichts zusammenrechnen muss
 * (Richtlinie §5.1: ein Endpunkt liefert eine beantwortbare Frage vollstaendig).
 *
 * <p>{@link #bewegt}, {@link #still} und {@link #nie} sind <b>disjunkt und vollstaendig</b>: Ihre
 * Summe ist {@link #anzahlProzesse}. Das ist der ganze Zweck von {@link Prozesszustand} — die drei
 * Zustaende duerfen nie in einen Eimer.
 *
 * @param anzahlProzesse alle Prozesse des Mandanten, <b>unabhaengig vom Fenster</b>
 * @param bewegt wie viele davon innerhalb der Stilleschwelle Verkehr hatten
 * @param still wie viele schon einmal etwas trugen, aber seit laenger als der Schwelle nichts mehr
 * @param nie wie viele noch nie etwas getragen haben
 * @param nachrichten die Summe im gewaehlten Fenster, ueber den ganzen Baum
 * @param fehler davon eingeordnet als {@code FEHLER}
 */
public record BaumsummeResponse(
    int anzahlProzesse, int bewegt, int still, int nie, long nachrichten, long fehler) {}
