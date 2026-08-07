package de.kraftwerkone.overlord.monitor.message;

import java.time.Instant;

/**
 * Ein Schritt der Schrittfolge, so wie der Aufrufer ihn sieht.
 *
 * <p>Der Metadaten-Schritt ({@code SOSActionID = 0}) erscheint hier <b>nicht</b> — er ist kein
 * Prozessschritt (S1, M17 3). Seine Eigenschaften gehen dabei nicht verloren; die werden ueber die
 * {@code MessageID} gelesen und nicht ueber die Aktion.
 *
 * @param position {@code MessageActionID} — die laufende Nummer je Nachricht, <b>nicht</b> die
 *     Kennung aus der Ablaufdefinition. Sie beginnt bei {@code 0}, und weil der Metadaten-Schritt
 *     entfaellt, beginnt die sichtbare Folge in aller Regel bei {@code 1}
 * @param name der lesbare Schrittname
 * @param namensherkunft {@code DIREKT}, {@code HERGELEITET} oder {@code ROHWERT} — Nachweis, woher
 *     der Name stammt, keine Warnung an den Nutzer
 * @param rohwert {@code SOSActionServiceProperties}, unveraendert. Kommt immer mit, auch wenn der
 *     Name direkt aufgeloest ist
 * @param start Beginn des Schritts als UTC-Zeitpunkt
 * @param ende Ende als UTC-Zeitpunkt; {@code null}, solange keines protokolliert ist
 * @param dauerSekunden im Backend gerechnet (Richtlinie §5.3: Dauern als ganze Sekunden). <b>{@code
 *     null} statt einer negativen Zahl</b>, falls Ende vor Start liegt — im dichten Tag kommt das
 *     auf keiner der 20.352 Aktionen vor (M16 1), die Regel ist aber billig und haelt eine
 *     Unmoeglichkeit aus der Antwort heraus. {@code null} auch, solange {@link #ende()} fehlt
 * @param timeoutSekunden {@code SOSActionTimeout}, Dauer in Sekunden (Regel Z2). Im Tagesfenster
 *     traegt jeder echte Schritt {@code 1800} (M16 4); 124 von 14.063 beendeten Schritten
 *     ueberschreiten diese Frist, und zwar um mehr als das Achtundvierzigfache — ein Dazwischen
 *     gibt es nicht. <b>Ob ein Schritt ueber seiner Frist gekennzeichnet wird, entscheidet die
 *     Oberflaeche</b>; das Backend liefert beide Zahlen und deutet sie nicht
 * @param laeuftAuf ob die Nachricht <b>gerade auf diesem Schritt steht</b>. Nur wahr, wenn der
 *     offene Zustand {@link OffenerZustand#LAEUFT_AUF} ist und dieser Aktion das Ende fehlt.
 *     <p><b>Bewusst nicht dasselbe wie {@code ende == null}.</b> Ein fehlendes Ende gibt es auch
 *     auf abgeschlossenen Nachrichten — 39 der 95 offenen Aktionen des Gesamtbestands gehoeren zu
 *     {@code FINISHED}, sieben zu {@code CHECKED} (M22). Dort ist das eine Protokolluecke und kein
 *     Haenger, und ein Feld, das beides gleich benennt, waere eine falsche Auskunft. Wer das rohe
 *     Merkmal braucht, liest {@link #ende()}
 */
public record SchrittResponse(
    int position,
    String name,
    Namensherkunft namensherkunft,
    String rohwert,
    Instant start,
    Instant ende,
    Long dauerSekunden,
    Integer timeoutSekunden,
    boolean laeuftAuf) {}
