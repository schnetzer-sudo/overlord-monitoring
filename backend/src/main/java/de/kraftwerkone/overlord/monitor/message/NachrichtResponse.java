package de.kraftwerkone.overlord.monitor.message;

import java.time.Instant;

/**
 * Eine Zeile der Nachrichtenliste, so wie der Aufrufer sie sieht.
 *
 * <p><b>Status doppelt, und das mit Absicht.</b> {@code status} ist der Rohwert des Altsystems —
 * damit ein Anwender ihn gegen die alte Oberflaeche halten kann und damit ein unbekannter Wert
 * ueberhaupt sichtbar wird. {@code statusKind} ist die fachliche Einordnung, an der die Oberflaeche
 * Farbe und Sortierung festmacht. Die Zuordnung entsteht ausschliesslich im {@code
 * MessageStatusClassifier} und wird nirgends nachgebaut.
 *
 * <p><b>Keine BAM-Werte mehr</b> (Nachbesserung zu Schritt 4). Messung M11 hat der kuratierten
 * Auswahl den Boden entzogen: Die beiden Typen, die {@code NEXANS} sieht, sind auf 98,93 und 96,97
 * Prozent der Zeilen leer, und kein einziger der 40 konfigurierten Typen erreicht auch nur ein
 * Fuenftel der Nachrichten. Die zweite Abfrage je Seite entfaellt damit ebenfalls. {@code
 * bam_spalte} und {@code common/BamSpaltenRegel} bleiben — Schritt 7 braucht beides fuer die
 * BAM-Suche.
 *
 * <p>Keine Verkettung und kein Timeout: Die Liste beantwortet „wo steht mein Beleg", nicht „was ist
 * im Einzelnen passiert" (Schritt 5) und nicht „was haengt daran" (Schritt 6).
 *
 * @param zeitpunkt {@code MessageLastUpdate} als UTC-Zeitpunkt. <b>Kein Anlagedatum</b> — die
 *     Quelle hat keines (Regel Q2).
 * @param bedeutungNichtVerifiziert bei {@code UNGEKLAERT}: Der Wert kommt so aus dem Altsystem,
 *     seine fachliche Bedeutung ist nicht belegt. Die Oberflaeche kennzeichnet das, statt einen
 *     plausiblen Text zu erfinden.
 * @param processName {@code null}, wenn die Quelle keinen Namen fuehrt — nicht zugeordnet heisst
 *     nicht zugeordnet (Regel Q4); den Ersatztext waehlt die Oberflaeche. Er steht seit der
 *     Nachbesserung <b>nicht mehr in einer eigenen Spalte</b>, sondern im Tooltip des Ablaufs: Er
 *     ist nur zufaellig lesbar („Kunde A Lieferschein (VDA)" neben „KUNDE_B_MX_000000_LAB"),
 *     waehrend {@code sosName} laut Projektbeschreibung §3.2 der Anzeigename ist.
 * @param sosName der Anzeigename des Ablaufs ({@code SOS.SOSName}), die Spalte „Ablauf". Gemessen
 *     in L14: durchgaengig gepflegt und auf allen 180.251 Nachrichten des dichten Monats
 *     aufloesbar; der Join ist ein {@code eq_ref} und kostet 0,2 ms. Trotzdem nullable — die
 *     Produktion muss sich nicht daran halten, was die Testkopie zufaellig enthaelt.
 * @param schritt der Schritt, auf dem die Nachricht <b>gerade steht</b> ({@code
 *     SOSAction.SOSActionName}) — <b>ausschliesslich bei {@code WARTEND} und {@code LAEUFT}</b>,
 *     sonst {@code null}.
 *     <p><b>Das ist keine Anzeigeentscheidung, sondern eine fachliche.</b> {@code SOSActionID} ist
 *     auf <i>jeder</i> Zeile gesetzt (M13: 3.341.519 von 3.341.519), auch auf abgeschlossenen —
 *     dort benennt sie aber den <i>letzten</i> Schritt und nicht den aktuellen. Das Feld heisst
 *     „Schritt, auf dem sie steht"; einen solchen gibt es nur, solange sie laeuft. Ihn trotzdem
 *     mitzuschicken hiesse, dem Aufrufer einen Wert zu geben, der etwas anderes bedeutet, als sein
 *     Name sagt — und der Chatbot aus Ausbaustufe 1 ruft dieselben Endpunkte auf wie die
 *     Oberflaeche.
 *     <p>Auch bei offenen Nachrichten darf er {@code null} sein: Der Verweis kann ins Leere laufen.
 *     In der Testkopie tut er das bei den offenen Status kein einziges Mal (M13), aber die
 *     Produktion muss sich daran nicht halten.
 */
public record NachrichtResponse(
    String messageId,
    Instant zeitpunkt,
    String status,
    String statusKind,
    boolean bedeutungNichtVerifiziert,
    String processId,
    String processName,
    String projectName,
    String sosName,
    String schritt) {}
