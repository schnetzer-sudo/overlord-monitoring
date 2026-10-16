package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.common.auftrag.Prueffehler;
import java.util.List;

/**
 * Die Antwort von {@code POST /api/tasks/pruefung}: die Fehler in derselben Form wie die Liste
 * {@code errors} beim Anlegen ({@code docs/tasks.md} §6.2). <b>Leer heisst gueltig.</b>
 */
public record PruefungResponse(List<Prueffehler> errors) {}
