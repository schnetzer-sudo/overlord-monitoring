package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import java.util.List;

/**
 * Die ganze Prozessansicht: <b>ein Aufruf, eine Antwort</b>.
 *
 * <p><b>Kein Knoten wird nachgeladen.</b> Der Baum steht vollstaendig in der Antwort — beim
 * groessten gemessenen Mandanten sind das 154 Partner, 290 Richtungsgruppen und 733 Blaetter
 * (M110). Ein Nachladen je Partner waere bei 154 Knoten 154 Anfragen mit je einer Sitzungspruefung
 * und je einem Verbindungsgriff; auf der Testkopie schreibt ausserdem jede Anfrage die Sitzung
 * fort.
 *
 * <p><b>Ueberfaelligkeit steht nicht im Baum</b>, und das ist eine Festlegung mit Begruendung:
 * {@code docs/dashboard.md} §5 fuehrt sie als erste benannte Ausnahme von Regel L2 — sie haengt an
 * {@code MessageLastUpdate + MessageTimeout} gegen {@code jetzt} und ist im Rollup nicht abbildbar.
 * Je Prozess waere das eine Live-Aggregation ueber {@code Message} mit {@code GROUP BY process_id}
 * ueber bis zu 733 Prozesse — eine zweite und deutlich groessere Ausnahme, fuer eine Zahl, die in
 * der Uebertragungsliste ohnehin steht. Vollstaendig in {@code docs/process-view.md} §5.
 *
 * <h2>Seit dem 15.09.2026: zwei Gliederungen, eine Form (E-140)</h2>
 *
 * <p>Bis dahin trug die Antwort die oberste Ebene als {@code partner}, darunter {@code richtungen}
 * und {@code prozesse}. Mit der zweiten Gliederung {@code PROJEKT} ist daraus <b>eine rekursive
 * Form</b> geworden: {@link #knoten} an der Wurzel, {@code kinder} an jeder Gruppe, und die
 * Ebenennamen stehen als {@link #ebenen} daneben — von aussen nach innen, damit die Oberflaeche die
 * Tiefe nicht aus der Gliederung ableiten muss.
 *
 * @param zeitraum das gewaehlte Paar als Code ({@code 48H}, {@code 30T}, {@code 12M}) oder {@code
 *     FREI} fuer ein freies Fenster — <b>immer gesetzt</b>, auch wenn der Aufrufer keinen genannt
 *     hat. {@code FREI} verraet keine Ebene: Die Oberflaeche braucht das Feld nur, um zu wissen,
 *     welcher Knopf hervorgehoben ist
 * @param gliederung die <b>aktive</b> Gliederung — immer gesetzt, genau wie {@code zeitraum}. Fehlt
 *     {@code ?gliederung=}, ist es die Vorgabe des angemeldeten Kontos, und die Oberflaeche liest
 *     sie von hier und nicht aus der URL (E-143)
 * @param fenster die gelesenen Grenzen, UTC, {@code bis} ausschliessend — auch im freien Fenster,
 *     dessen {@code bis} in der Anfrage einschliessend war ({@code common/Baumfenster#ausAnfrage})
 * @param stilleSchwelleMonate ab wie vielen Monaten ohne Bewegung ein Prozess als {@link
 *     Prozesszustand#STILL} gilt. <b>Die Zahl steht in der Antwort und nicht nur im Code</b>: Die
 *     Oberflaeche muss „seit ueber drei Monaten" formulieren koennen, ohne die Drei selbst zu
 *     kennen — sonst stuende dieselbe fachliche Festlegung an zwei Orten und driftete
 * @param gesamt die Kopfzahlen ueber den ganzen Baum — in beiden Gliederungen dieselben
 * @param ebenen die Namen der Ebenen, von aussen nach innen; die letzte ist immer {@code PROZESS}
 * @param knoten die oberste Ebene, sortiert
 */
public record ProzessbaumResponse(
    String zeitraum,
    Baumgliederung gliederung,
    ZeitfensterResponse fenster,
    int stilleSchwelleMonate,
    BaumsummeResponse gesamt,
    List<Baumebene> ebenen,
    List<BaumknotenResponse> knoten) {}
