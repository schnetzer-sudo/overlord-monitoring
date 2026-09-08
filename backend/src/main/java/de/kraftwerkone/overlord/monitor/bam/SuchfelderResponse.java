package de.kraftwerkone.overlord.monitor.bam;

import java.util.List;

/**
 * Das Angebot der Suchfläche — <b>zwei Quellen, zwei Gruppen</b> (E‑99).
 *
 * <p>Die Oberfläche zeigt beide als Gruppen in einem Feld; deshalb kommen sie als zwei Listen und
 * nicht als eine gemischte. Die Quelle steht trotzdem an jedem Eintrag, damit ein Eintrag auch
 * außerhalb seiner Gruppe eindeutig bleibt.
 *
 * @param bam die für den Mandanten konfigurierten Belegarten aus {@code MessageBAMMandant} —
 *     <b>dieselbe Abfrage wie {@code GET /api/bam/typen}</b>, wiederverwendet, nicht nachgebaut.
 *     Immer vorhanden, leer statt fehlend: drei Mandanten haben keinen konfigurierten Typ (M40)
 * @param felder die für den Mandanten sichtbaren Feldnamen aus {@code
 *     MessagePropertySearchListEntry} — die globalen ({@code MandantID IS NULL}) und die ihm
 *     zugeordneten. Immer vorhanden; für neun von zehn Mandanten enthält die Gruppe nur die
 *     globalen Typ‑0‑Namen (M154, Punkt 142)
 */
public record SuchfelderResponse(List<SuchfeldBamResponse> bam, List<SuchfeldFeldResponse> felder) {

  /** Die Quelle eines Eintrags aus {@code MessageBAMMandant}. */
  public static final String QUELLE_BAM = "bam";

  /** Die Quelle eines Eintrags aus {@code MessagePropertySearchListEntry}. */
  public static final String QUELLE_FELD = "feld";

  public SuchfelderResponse {
    bam = List.copyOf(bam);
    felder = List.copyOf(felder);
  }
}
