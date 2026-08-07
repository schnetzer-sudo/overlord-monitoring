package de.kraftwerkone.overlord.monitor.message;

/**
 * Das Ergebnis der Namensaufloesung: der Name, seine Herkunft und der Rohwert.
 *
 * <p><b>Der Rohwert kommt immer mit, auch bei {@link Namensherkunft#DIREKT}.</b> Die Oberflaeche
 * zeigt ihn nicht — aber ohne ihn ist im Zweifelsfall nicht pruefbar, ob ein Name zur Sache passt.
 * Genau diese Pruefung ist der Grund, warum M15 den richtigen Join vom zufaellig treffenden
 * unterscheiden konnte.
 *
 * @param name der lesbare Schrittname. Bei {@link Namensherkunft#ROHWERT} ist er mit {@link
 *     #rohwert()} identisch. {@code null} ist nur moeglich, wenn auch der Rohwert {@code null} ist
 *     — ein Fall, den die Testkopie nicht kennt (jeder namenlose echte Schritt traegt seine
 *     Bausteine, M15/M18), den die Spalte aber zulaesst. <b>Es wird hier kein Ersatztext
 *     erfunden</b>: Beschriftungen sind deutsch und gehoeren in die Sprachdatei der Oberflaeche,
 *     nicht ins Backend
 * @param herkunft welche der drei Stufen getragen hat
 * @param rohwert {@code MessageAction.SOSActionServiceProperties}, unveraendert
 */
public record Schrittname(String name, Namensherkunft herkunft, String rohwert) {}
