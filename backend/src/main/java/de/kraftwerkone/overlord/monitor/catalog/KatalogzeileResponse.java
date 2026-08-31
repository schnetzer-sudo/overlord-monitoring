package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
import java.time.LocalDateTime;

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
 * @param traegtNachrichten ob im Bestand mindestens eine Nachricht an diesem Prozess hängt (E14).
 *     <b>{@code null} ist ein eigener Zustand und wird nicht auf {@code false} abgebildet:</b>
 *     {@code null} heißt „noch nie geprüft", {@code false} heißt „geprüft und ohne Verkehr". Der
 *     Filter aus E20 muss die ungeprüften Zeilen <b>zeigen</b>, sonst verschwindet eine nie
 *     gemessene Zeile aus beiden Filterstellungen. Deshalb {@link Boolean} und nicht {@code
 *     boolean} — anders als bei {@code pflegestatus} und {@code vorschlagHerkunft}, wo das {@code
 *     null} der Spalte einen sinnvollen Ersatzwert hat
 * @param bestandGeprueftAm wann der Bestandslauf diese Zeile zuletzt angesehen hat, UTC — {@code
 *     null}, solange das nie geschehen ist. <b>Die Oberfläche leitet daraus das Alter der Erhebung
 *     für die ganze Liste ab</b>; ein zusätzliches Feld im Umschlag gibt es dafür bewusst nicht,
 *     denn die Angabe gehört an die Zeile, die sie beschreibt
 */
public record KatalogzeileResponse(
    String processId,
    String projectId,
    String projectName,
    String processName,
    String partner,
    Richtung richtung,
    Pflegestatus pflegestatus,
    VorschlagHerkunft vorschlagHerkunft,
    Boolean traegtNachrichten,
    LocalDateTime bestandGeprueftAm) {}
