package de.kraftwerkone.overlord.monitor.task;

import java.util.List;
import java.util.Map;

/**
 * Der Rumpf von {@code POST /api/tasks} — der Klick auf der Bestaetigungskarte, nie das Modell.
 *
 * <p><b>Kein Mandant</b> (Regel M1) und <b>kein Parametertext</b> (E‑1006): Den Mandanten liefert
 * die Sitzung, den Text baut das Backend. Ein untergeschobenes Feld {@code mandantId} faellt beim
 * Lesen weg.
 *
 * @param anfrageId UUID, vom Client je Absenden erzeugt; eine Wiederholung mit derselben Kennung
 *     legt keinen zweiten Auftrag an
 * @param eintraege je Eintrag Feldname auf Wert
 */
public record AuftragRequest(
    String anfrageId, String typ, String aktion, List<Map<String, String>> eintraege) {}
