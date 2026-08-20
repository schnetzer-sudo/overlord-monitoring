package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Eine Zeile der Pflegeliste: ein Prozess des aktiven Mandanten mit dem, was der Katalog ueber ihn
 * weiss.
 *
 * <p><b>Auch Prozesse ohne Katalogzeile stehen hier</b> (E5) — mit {@link Pflegestatus#OFFEN},
 * leeren Feldern und {@link VorschlagHerkunft#KEINE}. 765 von 1.503 Prozessen tragen im Bestand
 * keine einzige Nachricht (M74b) und werden trotzdem gepflegt: Ein Monitoring muss auch zeigen, was
 * <b>nicht</b> passiert ist, und ohne zugeordneten Partner laesst sich „von X kam seit acht Wochen
 * nichts" nicht formulieren, weil es kein X gibt.
 *
 * @param processId die Prozesskennung — der Schluessel der Katalogzeile
 * @param projectId die Projektkennung, lesbar ({@code 300_KundenEingehend}) und der Wert, den die
 *     Massenzuordnung entgegennimmt
 * @param projectName der Projektname. <b>Er steht hier fuer die Anzeige</b> — sortiert wird seit
 *     dem 20.08.2026 nach {@code ProjectID} und {@code ProcessID} (E6), die Reihenfolge braucht ihn
 *     also nicht mehr. Die Zeile bleibt trotzdem lesbar nur mit ihm: {@code ProjectName} und {@code
 *     ProjectID} sind verschiedene Zeichenketten (M75). Darf {@code null} sein — die Spalte laesst
 *     es zu.
 * @param processName der Anzeigename des Prozesses. Darf {@code null} sein. <b>Kein
 *     Ersatzschluessel und keine zweite Chance fuer die Heuristik:</b> Er traegt in keinem einzigen
 *     Fall einen Nummernpraefix und ist praktisch nie mit der {@code ProcessID} identisch (M75)
 * @param partner der kuratierte oder vorgeschlagene Partner. {@code null} ist ein gueltiger Wert —
 *     zusammen mit {@link Pflegestatus#GEPFLEGT} heisst er „hingesehen, es gibt nichts" (E4)
 * @param richtung die kuratierte oder abgeleitete Richtung, oder {@code null}
 * @param pflegestatus niemals {@code null} — ohne Katalogzeile {@link Pflegestatus#OFFEN}
 * @param vorschlagHerkunft niemals {@code null} — ohne Katalogzeile {@link VorschlagHerkunft#KEINE}
 */
public record KatalogzeileResponse(
    String processId,
    String projectId,
    String projectName,
    String processName,
    String partner,
    Richtung richtung,
    Pflegestatus pflegestatus,
    VorschlagHerkunft vorschlagHerkunft) {}
