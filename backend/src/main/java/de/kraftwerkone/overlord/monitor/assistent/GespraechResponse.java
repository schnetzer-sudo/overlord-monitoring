package de.kraftwerkone.overlord.monitor.assistent;

/** Die Antwort von {@code POST /api/assistent/gespraeche}: die Kennung des neuen Gespraechs. */
public record GespraechResponse(String gespraechId) {}
