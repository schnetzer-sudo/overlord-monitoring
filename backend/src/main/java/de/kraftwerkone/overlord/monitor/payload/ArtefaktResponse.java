package de.kraftwerkone.overlord.monitor.payload;

/**
 * Ein Artefakt in der Liste.
 *
 * <p><b>Kein Feld traegt einen Filestore-Verweis</b> — weder die GUID noch die Ablagenkennung. Was
 * hier steht, reicht aus, um das Artefakt <i>innerhalb seiner Nachricht</i> zu benennen, und fuer
 * nichts sonst.
 *
 * <h2>Warum hier kein lesbarer Schrittname steht</h2>
 *
 * <p>Weil er schon woanders steht. {@code GET /api/nachrichten/&#123;messageId&#125;} liefert die
 * Schrittfolge mit {@code position} — und {@code position} <i>ist</i> die {@code MessageActionID},
 * also genau {@link #schritt()}. Die Oberflaeche verbindet die beiden ueber diese Zahl. Die
 * dreistufige Namensaufloesung liegt im Paket {@code message}, und Fachpakete kennen einander
 * nicht; sie hierher zu kopieren hiesse, dieselbe Regel zweimal zu pflegen, und sie nach {@code
 * common} zu verschieben hiesse, einen Umbau in denselben Diff zu legen wie ein neues Feature.
 *
 * <p>Fuer die 55,98 % der Artefakte ohne aufloesbaren Schrittnamen (M57) traegt {@link #familie()}
 * die technische Herkunft — {@code FileReader}, {@code Converter}, {@code FTPSender}. Sie ist die
 * Zeichenkette aus dem Namen und keine Deutung; eine deutsche Beschriftung daraus zu machen, ist
 * Sache der Oberflaeche (Regel Q4: nichts wird geraten).
 *
 * @param artefaktId die Kennung fuer die beiden anderen Endpunkte. Enthaelt Schritt und Name, sonst
 *     nichts
 * @param name {@code MessagePropertyName}, unveraendert
 * @param familie der Teil vor dem Namensmuster — die technische Herkunft
 * @param art {@link Artefaktart#NUTZDATEN} oder {@link Artefaktart#PROTOKOLL}
 * @param schritt {@code MessageActionID}. {@code 0} ist der Metadaten-Schritt und kein
 *     Ablaufschritt; die Oberflaeche verbindet jede groessere Zahl mit {@code position} aus der
 *     Schrittfolge des Nachrichtendetails
 * @param beschnittMoeglich ob dieses Artefakt fuer den <b>aufrufenden</b> Nutzer beschnitten wird.
 *     Wahr nur bei Protokollen und nur fuer {@code MANDANT}. Steht hier, damit die Oberflaeche es
 *     ankuendigen kann, statt den Nutzer erst beim Oeffnen zu ueberraschen
 */
public record ArtefaktResponse(
    String artefaktId,
    String name,
    String familie,
    Artefaktart art,
    short schritt,
    boolean beschnittMoeglich) {}
