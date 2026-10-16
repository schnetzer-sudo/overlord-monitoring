package de.kraftwerkone.overlord.monitor.assistent.modell;

import tools.jackson.databind.JsonNode;

/**
 * Ein Werkzeugaufruf des Modells.
 *
 * @param id die Kennung des Aufrufs beim Anbieter; das Ergebnis nennt sie wieder
 * @param eingabe die Eingabe, wie das Modell sie geliefert hat — ungeprueft
 */
public record Werkzeugaufruf(String id, String name, JsonNode eingabe) {}
