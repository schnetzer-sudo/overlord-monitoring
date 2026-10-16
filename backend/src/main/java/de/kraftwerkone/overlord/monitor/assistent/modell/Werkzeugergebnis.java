package de.kraftwerkone.overlord.monitor.assistent.modell;

/**
 * Die Antwort des Backends auf einen Werkzeugaufruf.
 *
 * @param aufrufId die Kennung des beantworteten Aufrufs
 * @param inhalt Text an das Modell: die Pruefergebnisse oder die Meldung, dass der Vorschlag
 *     gueltig ist
 * @param fehler ob der Vorschlag abgewiesen ist
 */
public record Werkzeugergebnis(String aufrufId, String inhalt, boolean fehler) {}
