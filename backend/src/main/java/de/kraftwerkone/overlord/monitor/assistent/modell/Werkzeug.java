package de.kraftwerkone.overlord.monitor.assistent.modell;

import tools.jackson.databind.JsonNode;

/**
 * Ein Werkzeug des Modells: Name, Beschreibung und die Eingabe als JSON Schema (E‑1034). Im
 * Assistenten hat jedes Werkzeug <b>keine Nebenwirkung</b>; es nimmt einen Vorschlag entgegen.
 */
public record Werkzeug(String name, String beschreibung, JsonNode eingabeschema) {}
