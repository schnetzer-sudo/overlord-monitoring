package de.kraftwerkone.overlord.monitor.catalog;

import java.util.List;

/**
 * Eine Gruppe des Prozessbaums — ein Partner, eine Richtung oder ein Projekt.
 *
 * <p><b>Welche davon, sagt die Position</b> und nicht der Knoten: Die n-te Ebene heisst {@code
 * ProzessbaumResponse.ebenen().get(n)}. Die Summen entstehen von den Blaettern nach oben, im Dienst
 * und nicht beim Aufrufer (Richtlinie §5.1).
 *
 * <p><b>{@code name = null} traegt je Ebene eine eigene Bedeutung</b>, und beide sind seit Schritt
 * 10c festgelegt: bei {@code PARTNER} <i>nicht zugeordnet</i> (steht am Ende, E-39), bei {@code
 * RICHTUNG} <i>nicht ermittelt</i> (E-40). Bei {@code PROJEKT} entsteht kein solcher Knoten: Jeder
 * Prozess hat aus dem Schema heraus genau ein Projekt, und eine Rueckfallregel fuer eine leere
 * Beschreibung gibt es bewusst nicht (E-144).
 *
 * @param schluessel der hochgestellte Gruppenschluessel (E-41), {@code null} fuer die Gruppe ohne
 *     Wert
 * @param name der Rohwert in der zuerst angetroffenen Schreibweise, oder {@code null}
 * @param anzahlProzesse wie viele Blaetter unter diesem Knoten haengen, ueber alle Ebenen darunter
 * @param nachrichten die Summe der Blaetter im Fenster
 * @param fehler die Summe der Blaetter im Fenster
 * @param kinder die naechste Ebene, sortiert — oder die Blaetter in der Reihenfolge der Abfrage
 */
public record GruppenknotenResponse(
    String schluessel,
    String name,
    int anzahlProzesse,
    long nachrichten,
    long fehler,
    List<BaumknotenResponse> kinder)
    implements BaumknotenResponse {}
