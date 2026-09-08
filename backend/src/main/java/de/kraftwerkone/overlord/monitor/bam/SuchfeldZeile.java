package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Eintrag aus {@code MessagePropertySearchListEntry}, roh: der Feldname und sein Typ.
 *
 * <p><b>Ohne die {@code MandantID}.</b> Sie ist die Bedingung des Statements und keine Auskunft; ob
 * ein Eintrag global gilt oder dem Mandanten der Sitzung gehört, verlässt das Repository nicht —
 * die Antwort beschreibt Felder und nicht, für wen sie gelten.
 *
 * @param name {@code MessagePropertyName}, unverändert (E‑105)
 * @param typ {@code MessagePropertyType} — {@code 0} für eine Spalte, {@code 1} für eine Zeile in
 *     {@code MessageProperty} (M154). <b>Nullbar</b>, weil die Spalte es ist; was mit einer solchen
 *     Zeile geschieht, entscheidet {@link SuchfelderService}
 */
public record SuchfeldZeile(String name, Integer typ) {}
