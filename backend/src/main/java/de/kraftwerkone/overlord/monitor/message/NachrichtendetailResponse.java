package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import java.time.Instant;
import java.util.List;

/**
 * Die Antwort von {@code GET /api/nachrichten/&#123;messageId&#125;} — Kopf, Schrittfolge und die
 * kuratierten Eigenschaften in einem Stueck.
 *
 * <p>Sie beantwortet die Frage, die die Liste offen laesst: <b>Was ist im Einzelnen passiert?</b>
 * Nicht „wo steht mein Beleg" (das ist die Liste) und nicht „was haengt daran" (das ist Schritt 6).
 *
 * @param messageId {@code Message.MessageID}
 * @param status der Rohwert des Altsystems — damit ein Anwender ihn gegen die alte Oberflaeche
 *     halten kann und damit ein unbekannter Wert ueberhaupt sichtbar wird
 * @param statusKind die fachliche Einordnung aus dem {@code MessageStatusClassifier}. <b>Nicht neu
 *     klassifiziert</b>: Die Einordnung entsteht an genau einer Stelle im Code und wird nirgends
 *     nachgebaut
 * @param processId {@code Message.ProcessID}
 * @param processName nullbar (Regel Q4). Nur zufaellig lesbar — „Kunde A Lieferschein (VDA)" steht
 *     neben „KUNDE_B_MX_000000_LAB"
 * @param projectName nullbar
 * @param sosName der Anzeigename des Ablaufs, {@code SOS.SOSName}
 * @param rollen die Stellung dieser Nachricht in der Verkettung — <b>immer vorhanden, leer statt
 *     fehlend</b>. Ein fehlendes Feld hiesse „unbekannt", ein leeres heisst „nicht in einer Kette",
 *     und das ist eine Aussage, die die Zeile tatsaechlich macht: Die vier Verkettungsspalten
 *     stehen auf ihr, und die Flags decken sich exakt mit der Verkettung (E4).
 *     <p><b>Das kostet keinen Join und kein zweites Statement</b> — die Spalten liegen auf der
 *     {@code Message}-Zeile, die {@code findeKopf} ohnehin liest. Abgeleitet wird in {@code
 *     common/Kettenrollen}: <b>die eine Stelle</b>, an der aus den vier Spalten Rollen werden. Hier
 *     wird nicht neu abgeleitet, so wie hier auch nicht neu klassifiziert wird.
 *     <p>Die Oberflaeche entscheidet daran, <b>ob sie die Kette ueberhaupt laedt</b>: Ist die Liste
 *     leer, entsteht keine Anfrage auf {@code /kette}. Rund 60 Prozent aller Zeilen tragen keine
 *     Kette, fuer sie faellt damit keine zweite Anfrage an ({@code verkettung.md})
 * @param zeitpunkt {@code MessageLastUpdate} als UTC-Zeitpunkt. <b>Kein Anlagedatum</b> — die
 *     Quelle hat keines (Regel Q2)
 * @param start der <b>fachliche</b> Start: {@code MIN(MessageAction.MessageActionStart)} (Regel
 *     Q2), gerechnet ueber <b>alle</b> Aktionen einschliesslich des Metadaten-Schritts. Ihn
 *     auszunehmen ergaebe einen zu spaeten Start — an ihm kommt die Nachricht ins System (M17 3).
 *     {@code null}, wenn es keine Aktion mit Start gibt
 * @param gesamtdauerSekunden vom fachlichen Start bis {@code zeitpunkt}, in ganzen Sekunden — fuer
 *     <b>jede</b> Nachricht, offen oder nicht. {@code null}, wenn es keinen Start gibt oder das
 *     Ende davor laege.
 *     <p><b>Sie ist die Abdeckung fuer den Fall, den die gestrichene Lueckenzeile fangen
 *     sollte.</b> Endet Schritt 3 und beginnt Schritt 4 drei Stunden spaeter, weil ein Dienst weg
 *     war, steht diese Zeit in keiner Schrittdauer. Passt die Summe der Schrittdauern nicht zur
 *     Gesamtdauer, steckt sie dazwischen — und das ist sichtbar, ohne dass es ein Element dafuer
 *     gibt, das nie jemand ausgeloest hat.
 *     <p><b>Der fachliche Start rechnet ueber alle Aktionen einschliesslich des
 *     Metadaten-Schritts</b>, anders als der „letzte Schritt" weiter unten. Zwei verschiedene
 *     Fragen, zwei verschiedene Mengen: Der Start ist der Zeitpunkt, an dem die Nachricht ins
 *     System kommt — und das ist der Metadaten-Schritt. Der letzte Schritt ist dagegen etwas, das
 *     der Nutzer in der Leiste sieht
 * @param fristSekunden {@code Message.MessageTimeout}, Dauer in <b>Sekunden</b> (Regel Z2, M8 —
 *     nicht Minuten). <b>{@code null} bei {@link
 *     de.kraftwerkone.overlord.monitor.common.MessageStatusKind#WARTEND}</b> <i>(seit dem
 *     03.09.2026, E‑76)</i>: Eine wartende Nachricht wird vom Waechter des Altsystems nie beendet,
 *     die Frist wird auf sie also <b>nicht angewendet</b> — und ein Feld, das eine Frist nennt, die
 *     niemand durchsetzt, ist eine falsche Auskunft. Bis dahin stand dort {@code 1800}. <b>Bei
 *     {@code RUNNING} bleibt es und wird erst jetzt richtig:</b> zusammen mit {@code
 *     wartetSeitSekunden} sagt es, wann die Nachricht in {@code ERROR_TIMEOUT} kippt. <b>{@code
 *     null} auch, wenn keine Frist gesetzt ist</b>: bei {@code NULL} und bei {@code 0}. Keine
 *     erfundene Frist, und keine {@code 0}, die als „sofort faellig" gelesen werden koennte.
 *     <p>Das Feld hiess bis zum 10.08.2026 {@code timeoutSekunden} und lieferte die {@code 0} roh.
 *     Umbenannt statt ergaenzt: Zwei Felder aus derselben Spalte mit verschiedener {@code
 *     null}-Bedeutung waeren eine zweite Wahrheit. Der Timeout <i>je Schritt</i> heisst weiterhin
 *     {@code timeoutSekunden} und bleibt roh — dort ist die Bedeutung der {@code 0} eine offene
 *     Frage (Frage 8 in {@code messungen-schritt5.md}), hier ist sie geklaert
 * @param eigenschaftenAnzahl wie viele technische Eigenschaften die Nachricht hat.
 *     <p><b>Die Zahl gehoert in den Kopf, obwohl die Eigenschaften selbst nicht mitkommen.</b>
 *     Sonst kann die Oberflaeche den eingeklappten Block nicht beschriften, ohne ihn zu laden —
 *     womit der zweite Endpunkt seinen Zweck verloere. Gemessen sind rund 22,6 Eigenschaften je
 *     Nachricht (M17 1)
 * @param bamAnzahl wie viele BAM-Werte — Belegnummern, Kennungen, Stellen — auf der Nachricht
 *     stehen. <b>Immer vorhanden, {@code 0} statt fehlend.</b>
 *     <p><b>Sie gehoert in den Kopf und nicht in den BAM-Endpunkt.</b> M41 misst, dass 80,6 Prozent
 *     aller Nachrichten in Fenster B keinen BAM-Wert tragen — bei Merge-Eingaengen 38.628 von
 *     38.628, also alle. Ohne die Zahl im Kopf muesste die Oberflaeche einen Block zeichnen und
 *     eine Anfrage stellen, um festzustellen, dass er leer ist. Dieselbe Begruendung wie bei {@code
 *     rollen} und {@code eigenschaftenAnzahl}, und dieselbe Bauform: eine zaehlende Unterabfrage
 *     ueber den Primaerschluessel-Praefix, {@code Using index}, ohne einen einzigen gelesenen Wert.
 *     <p>Die Werte selbst liegen unter {@code GET /api/nachrichten/&#123;id&#125;/bam} ({@code
 *     docs/bam-werte.md}) — je Typgruppe gedeckelt, weil auf einer einzigen Nachricht bis zu 9.296
 *     Werte stehen koennen
 * @param offenerZustand woran die Nachricht steht, benannt statt erraten
 * @param naechsterSchritt der Schritt, auf den {@code Message.SOSID}/{@code SOSActionID} zeigen —
 *     bei {@link OffenerZustand#WARTET_IN} der zuletzt <i>gelaufene</i>, bei {@link
 *     OffenerZustand#WARTET_VOR} ein noch nicht begonnener. Sonst {@code null}.
 *     <p><b>Er kommt in beiden Wartezustaenden mit</b>, obwohl die Oberflaeche ihn bei {@code
 *     WARTET_IN} nur in den Tooltip schreibt: Der Name ist die Auskunft, welchen Schritt das
 *     Altsystem meint, und die gehoert nicht davon ab, wie sie dargestellt wird.
 *     <p>Auch dort nullbar: Ueber den Gesamtbestand laeuft dieser Verweis zu 43,9 Prozent ins Leere
 *     (M13). Bei allen 538 wartenden Nachrichten der Testkopie loest er auf (M29 3) — die
 *     Produktion muss sich daran nicht halten
 * @param wartetSeitSekunden wie lange die Nachricht schon steht, in ganzen Sekunden. Bei {@link
 *     OffenerZustand#WARTET_IN} und {@link OffenerZustand#WARTET_VOR} vom <b>Ende der letzten
 *     Aktion der Schrittfolge</b> bis jetzt, bei {@link OffenerZustand#LAEUFT_AUF} vom <b>Beginn
 *     der offenen Aktion</b> bis jetzt, bei {@link OffenerZustand#EMPFANGEN} vom <b>Ende des
 *     Schritts {@code 0}</b> — ist es nicht gesetzt, von dessen Beginn. {@code null} bei {@link
 *     OffenerZustand#OHNE_AKTION} und {@link OffenerZustand#KEINER}.
 *     <p><b>{@code EMPFANGEN} ist am 10.08.2026 korrigiert worden.</b> Vorher lieferte der Fall
 *     „nur der Metadaten-Schritt" ein {@code null} — mit der Begruendung, jener Schritt zaehle
 *     nicht zu den Schritten. Das war auf definitorische Sauberkeit optimiert und nicht auf die
 *     Frage des Nutzers: Eine Nachricht, die um 14:32 angekommen und seitdem nicht angefasst worden
 *     ist, <b>haengt</b>. Der Schritt {@code 0} ist kein Verarbeitungsschritt, aber er ist ein
 *     Ereignis mit einem Zeitpunkt — aus genau diesem Grund rechnet der fachliche Start ja auch
 *     ueber ihn (M17 3). Bei {@link OffenerZustand#OHNE_AKTION} bleibt es bei {@code null}, und
 *     zwar konsequent: Dort ist auch {@code start} {@code null}, und eine Dauer aus {@code
 *     MessageLastUpdate} waere eine erfundene Zahl.
 *     <p><b>Gerechnet wird hier und niemals im Browser</b> (Regel Z1). Bezugspunkt ist die
 *     <b>Anwendungsuhr</b> — im Profil {@code dev} die um Monate zurueckversetzte. Eine
 *     Oberflaeche, die {@code Date.now()} gegen einen gelieferten Zeitstempel rechnete, zeigte dort
 *     Monate statt Stunden; genau dafuer gibt es die Uhr
 * @param schritte die Schrittfolge, nach {@code MessageActionStart} sortiert, bei Gleichstand nach
 *     {@code MessageActionID}. <b>Ohne Obergrenze</b>: M16 (2) hat hoechstens sieben Aktionen je
 *     Nachricht gemessen, davon eine der Metadaten-Schritt. Eine Deckelung schuetzte vor nichts und
 *     kostete die Vollstaendigkeit, die eine Zeitleiste erst brauchbar macht — und eine
 *     Detailansicht laedt ohnehin nur eine einzige Nachricht
 * @param kuratierteEigenschaften wenige, ausgewaehlte technische Werte. <b>Leere Werte sind nicht
 *     enthalten</b>; die Liste ist haeufig leer, und das ist gewollt
 */
public record NachrichtendetailResponse(
    String messageId,
    String status,
    String statusKind,
    String processId,
    String processName,
    String projectName,
    String sosName,
    List<Kettenrolle> rollen,
    Instant zeitpunkt,
    Instant start,
    Long gesamtdauerSekunden,
    Integer fristSekunden,
    int eigenschaftenAnzahl,
    int bamAnzahl,
    OffenerZustand offenerZustand,
    String naechsterSchritt,
    Long wartetSeitSekunden,
    List<SchrittResponse> schritte,
    List<KuratierteEigenschaftResponse> kuratierteEigenschaften) {}
