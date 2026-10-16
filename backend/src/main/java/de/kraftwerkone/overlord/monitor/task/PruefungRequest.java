package de.kraftwerkone.overlord.monitor.task;

import java.util.List;
import java.util.Map;

/**
 * Der Rumpf von {@code POST /api/tasks/pruefung} (E‑1042): wie {@link AuftragRequest}, ohne {@code
 * anfrageId} — es wird nichts angelegt. Eine mitgeschickte {@code anfrageId} und ein
 * untergeschobenes {@code mandantId} fallen beim Lesen weg (Regel M1).
 *
 * @param eintraege je Eintrag Feldname auf Wert
 */
public record PruefungRequest(String typ, String aktion, List<Map<String, String>> eintraege) {}
