package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import java.util.List;

/**
 * Die Antwort von {@code GET /api/nachrichten/&#123;messageId&#125;/kette} — <b>was an dieser
 * Nachricht haengt</b>.
 *
 * <p>Sie beantwortet die dritte der drei Fragen des Werkzeugs: nicht „wo steht mein Beleg" (das ist
 * die Liste) und nicht „was ist im Einzelnen passiert" (das ist das Detail), sondern <b>„was haengt
 * daran"</b>.
 *
 * <h2>Die Auflösung ist bewusst unsymmetrisch</h2>
 *
 * <p><b>Nach oben vollstaendig, nach unten eine Ebene.</b> Der Grund steht in den Messungen und
 * nicht im Geschmack:
 *
 * <ul>
 *   <li><b>Aufwaerts</b> kostet jede Ebene genau einen Primaerschluesselzugriff — {@code
 *       SourceMessageID} bei einem Split-Kind, {@code TargetMessageID} bei einem Merge-Eingang. Nie
 *       beide auf derselben Zeile (M28‑1), es gibt also je Ebene genau <i>ein</i> Glied darueber.
 *       Gemessen: 0,48 bis 0,51 ms je Ebene, die tiefste Kette der Testkopie hat vier Glieder
 *       (M30‑1, M30‑3).
 *   <li><b>Abwaerts</b> faechert es auf: bis 3.350 Kinder an einer Wurzel und bis 897 Eingaenge an
 *       einem Merge-Ergebnis (M30‑2). Zwei Ebenen abwaerts waeren im schlechtesten Fall Millionen
 *       Zeilen.
 * </ul>
 *
 * <p>Der Nutzer bekommt damit die Herkunft vollstaendig und die Aufteilung Schritt fuer Schritt.
 * Weiter nach unten fuehrt ein neuer Aufruf auf das jeweilige Glied.
 *
 * <h2>Die beiden Listen heissen nach dem Mechanismus, nicht nach einer Bedeutung</h2>
 *
 * <p><b>{@code aufwaerts} und {@code abwaerts} — umbenannt am 11.08.2026.</b> Die frueheren Namen
 * benannten eine <i>Bedeutung</i>, und die stimmt nur bei der <i>Aufteilung</i>: Beim Merge-Eingang
 * steht im Aufstieg das <b>Ergebnis</b> — also das, was aus ihm <i>wurde</i> —, und beim
 * Merge-Ergebnis stehen im Abstieg seine <b>Eingaenge</b>, also das, woher es <i>kommt</i>. Ueber
 * Fenster B sind das 38.628 Zeilen (M30‑4). Der Vermerk dazu steht datiert in {@code
 * docs/verkettung.md} §2.
 *
 * <p>Ein Name, der in fuenf von sechs Faellen stimmt, ist gefaehrlicher als einer, der nichts
 * behauptet. Die <b>Bedeutung</b> tragen {@link KettengliedResponse#beziehung} und {@link
 * KettengliedResponse#ebene}; die Oberflaeche wertet ohnehin diese beiden aus. Der naechste Leser
 * ist ausserdem kein Kollege: Ausbaustufe 1 haengt den Chatbot an genau diesen Endpunkt, und ein
 * Werkzeugfeld, das die Herkunft behauptet, braechte jedes Modell dazu, bei jedem Merge-Eingang die
 * Flussrichtung umzudrehen.
 *
 * @param messageId die angefragte Nachricht — die Wurzel dieser Antwort, nicht die Wurzel der Kette
 * @param rollen ihre Stellung in der Verkettung. <b>Immer vorhanden, leer statt fehlend</b>
 * @param aufwaerts der Aufstieg, vollstaendig — Ebene {@code -1} zuerst. Leer, wenn die Nachricht
 *     selbst am oberen Ende steht. <b>Nicht „woher es kommt"</b>: Beim Merge-Eingang steht hier das
 *     Ergebnis
 * @param abwaerts die Kinder <b>und</b> die Merge-Eingaenge dieser Nachricht, gemeinsam sortiert
 *     nach {@code (MessageLastUpdate, MessageID)}, hoechstens {@link KettenService#BREITE_GRENZE}.
 *     Beide Richtungen in einer Liste, weil eine Zeile beides sein kann: 25 Zeilen sind zugleich
 *     Wurzel und Merge-Ergebnis (M30‑4)
 * @param abwaertsGesamt wie viele es insgesamt sind — <b>die genaue Zahl</b>, nicht „mehr als 50".
 *     Die Entscheidung haengt an einer Messung: Die Zaehlung kostet an der breitesten Wurzel des
 *     gesamten Bestands 19,8 ms (M30‑1), also nicht die Haelfte der Grenze von 50 ms, ab der auf
 *     eine Ersatzform umgestellt worden waere. <b>Ebenfalls mandantengefiltert</b> — sonst nennte
 *     die Antwort 27 Teile und lieferte 25
 * @param abwaertsCursor die Position, ab der {@code GET
 *     /api/nachrichten/&#123;messageId&#125;/kette/abwaerts?cursor=…} weiterblaettert — <b>die
 *     letzte hier ausgelieferte Abwaertszeile</b> in der Ordnung {@code (MessageLastUpdate,
 *     MessageID)}.
 *     <p><b>Er kommt aus derselben Stelle wie die Cursor des Blaetter-Endpunkts</b> ({@code
 *     KettenService.position}). Ein zweiter Kodierer waere die Drift, die das Blaettern zerlegt.
 *     <p>{@code null}, wenn {@code weitereVorhanden} falsch ist — ein Cursor ohne naechste Seite
 *     behauptete, es gaebe eine. Ebenfalls {@code null}, wenn die letzte ausgelieferte Zeile keinen
 *     {@code MessageLastUpdate} traegt: Sie hat in der Ordnung keine Position. Gemessen kommt das
 *     {@code 0} von 3.341.519 Mal vor (M30‑6); die Spalte laesst es zu
 *     <p><b>Ergaenzt am 11.08.2026.</b> Ohne ihn holte die Oberflaeche beim ersten Nachladen die
 *     erste Seite ein zweites Mal, nur um an eine Position zu kommen — zwei Anfragen fuer einen
 *     Klick
 * @param weitereVorhanden ob {@code abwaertsGesamt} groesser ist als die Zahl der gelieferten
 *     Glieder. Dann fuehrt {@code GET
 *     /api/nachrichten/&#123;messageId&#125;/kette/abwaerts?cursor=…} weiter.
 *     <p><b>Beim Merge ist das der Regelfall, nicht die Ausnahme:</b> 11,38 Prozent der
 *     Merge-Ergebnisse haben mehr als 50 Eingaenge, gegen 1,07 Prozent der Wurzeln (M30‑2)
 * @param tiefeErreicht ob der Aufstieg an der Tiefengrenze abgebrochen ist. Dann ist die Kette
 *     <b>nicht</b> vollstaendig ausgegeben — in der Testkopie kommt das nie vor (tiefste Kette:
 *     vier Glieder, M30‑3), aber die Kette entsteht durch Datenbank-Events, die uns nicht gehoeren
 * @param zyklusErkannt ob der Aufstieg auf eine bereits besuchte {@code MessageID} gestossen ist.
 *     <b>Ein eigenes Feld neben {@code tiefeErreicht}</b>, weil die Tiefengrenze allein zwar
 *     abbraeche, aber „tief" saegte statt „im Kreis". Gemessen kommt kein Zyklus vor (M30‑3) — der
 *     Schutz ist die Zusicherung, nicht die Beobachtung
 */
public record KetteResponse(
    String messageId,
    List<Kettenrolle> rollen,
    List<KettengliedResponse> aufwaerts,
    List<KettengliedResponse> abwaerts,
    int abwaertsGesamt,
    String abwaertsCursor,
    boolean weitereVorhanden,
    boolean tiefeErreicht,
    boolean zyklusErkannt) {}
