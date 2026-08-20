package de.kraftwerkone.overlord.monitor.payload;

import java.util.List;

/**
 * Die Artefakte einer Nachricht, <b>zweigeteilt</b>: Nutzdaten und Protokolle.
 *
 * <p>Die Aufteilung steht hier und nicht in der Oberflaeche, weil sie aus dem Datenmodell folgt und
 * nicht aus einer Gestaltungsentscheidung: Der Beschnitt greift ausschliesslich bei {@link
 * Artefaktart#PROTOKOLL} ({@link Protokollbeschnitt}), und {@code beschnittMoeglich} haengt daran.
 *
 * <h2>Das dritte Feld ist am 19.08.2026 entfallen</h2>
 *
 * <p>Bis dahin stand hier zusaetzlich {@code eingang} — {@code Message.Payload.GUID}, gefuehrt als
 * „die eingegangene Datei". <b>Nach M73 zeigt dieser Name auf die Nutzdatenzeile mit dem hoechsten
 * {@code MessageActionID} derselben Nachricht</b>, in 6.249 von 6.249 und 214.330 von 214.330
 * gemessenen Nachrichten ohne Gegenfall. Er benennt kein eigenes Artefakt (siehe {@link
 * Artefaktnamen#NAME_ZEIGER}) und steht deshalb in keiner der beiden Listen und in keinem eigenen
 * Feld.
 *
 * <p><b>Ein Feld dieses Namens waere die Falschauskunft eine Schicht tiefer.</b> An diesen
 * Endpunkten haengt nicht nur die Oberflaeche: Der Chatbot der Ausbaustufe 1 greift laut {@code
 * docs/PROJEKTBESCHREIBUNG.md} §10 auf dieselben zu, und dort faellt eine falsche Benennung
 * niemandem auf.
 *
 * <p>Jede Nachricht traegt <b>3 bis 15</b> Artefakte und mindestens ein Protokoll, bei jedem
 * Mandanten (M55). Ein leerer Kasten ist deshalb kein erwarteter Zustand — kommt er vor, ist er ein
 * Befund.
 *
 * <p><b>Vermerk 20.08.2026 zur Spanne.</b> <b>3 bis 15</b> stammt aus M55 und zaehlt die Zeile
 * {@code Message.Payload.GUID} mit, die diese Antwort seit dem 19.08.2026 nicht mehr fuehrt; jede
 * gemessene Nachricht traegt genau eine solche Zeile (M73). <b>Diese Antwort traegt also eine
 * weniger.</b> Neu ausgezaehlt ist die Spanne nicht — das waere eine Messung. Am Satz darueber
 * aendert das nichts: Das Minimum bleibt ueber null.
 *
 * @param messageId die Nachricht, zu der die Liste gehoert
 * @param nutzdaten die Dateien, nach Schritt geordnet
 * @param protokolle die Protokolle je Schritt, nach Schritt geordnet
 */
public record ArtefaktlisteResponse(
    String messageId, List<ArtefaktResponse> nutzdaten, List<ArtefaktResponse> protokolle) {}
