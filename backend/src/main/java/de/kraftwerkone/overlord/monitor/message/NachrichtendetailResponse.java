package de.kraftwerkone.overlord.monitor.message;

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
 * @param zeitpunkt {@code MessageLastUpdate} als UTC-Zeitpunkt. <b>Kein Anlagedatum</b> — die
 *     Quelle hat keines (Regel Q2)
 * @param start der <b>fachliche</b> Start: {@code MIN(MessageAction.MessageActionStart)} (Regel
 *     Q2), gerechnet ueber <b>alle</b> Aktionen einschliesslich des Metadaten-Schritts. Ihn
 *     auszunehmen ergaebe einen zu spaeten Start — an ihm kommt die Nachricht ins System (M17 3).
 *     {@code null}, wenn es keine Aktion mit Start gibt
 * @param timeoutSekunden {@code Message.MessageTimeout}, Dauer in <b>Sekunden</b> (Regel Z2,
 *     korrigiert am 01.08.2026). {@code 0} bedeutet „kein Timeout"
 * @param eigenschaftenAnzahl wie viele technische Eigenschaften die Nachricht hat.
 *     <p><b>Die Zahl gehoert in den Kopf, obwohl die Eigenschaften selbst nicht mitkommen.</b>
 *     Sonst kann die Oberflaeche den eingeklappten Block nicht beschriften, ohne ihn zu laden —
 *     womit der zweite Endpunkt seinen Zweck verloere. Gemessen sind rund 22,6 Eigenschaften je
 *     Nachricht (M17 1)
 * @param offenerZustand woran die Nachricht steht, benannt statt erraten
 * @param naechsterSchritt der Schritt, <b>vor</b> dem die Nachricht wartet — ausschliesslich bei
 *     {@link OffenerZustand#WARTET_VOR}, sonst {@code null}. Aufgeloest aus {@code Message.SOSID}
 *     und {@code Message.SOSActionID}.
 *     <p>Auch dort nullbar: Ueber den Gesamtbestand laeuft dieser Verweis zu 43,9 Prozent ins Leere
 *     (M13). Bei allen 538 wartenden Nachrichten der Testkopie loest er auf — die Produktion muss
 *     sich daran nicht halten
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
    Instant zeitpunkt,
    Instant start,
    Integer timeoutSekunden,
    int eigenschaftenAnzahl,
    OffenerZustand offenerZustand,
    String naechsterSchritt,
    List<SchrittResponse> schritte,
    List<KuratierteEigenschaftResponse> kuratierteEigenschaften) {}
