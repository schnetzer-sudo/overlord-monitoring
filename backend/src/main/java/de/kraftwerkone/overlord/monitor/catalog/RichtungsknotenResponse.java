package de.kraftwerkone.overlord.monitor.catalog;

import java.util.List;

/**
 * Die mittlere Ebene des Baums: eine <b>Uebertragungsrichtung</b> unter einem Partner.
 *
 * <p><b>Es erscheinen nur die Richtungen, die tatsaechlich vorkommen</b> — kein leerer Ast. Ein
 * Partner, unter dem es nur Eingehendes gibt, bekommt keinen leeren Ausgehend-Knoten.
 *
 * <h2>Ein leeres Richtungsfeld heisst hier „nicht ermittelt", nie „gibt es nicht"</h2>
 *
 * <p>Das ist der Unterschied zum Partner. {@code docs/prozess-katalog.md} E4 liest „gepflegt und
 * leer" als <i>hingesehen, es gibt nichts</i> — bei einer Uebertragungsrichtung gibt es das nicht,
 * jede Uebertragung hat eine. <b>Beide Faelle fallen deshalb in denselben Knoten</b> ({@code
 * richtung = null}), und die Antwort unterscheidet sie nicht.
 *
 * <p><b>Das ist eine Einschraenkung und keine Loesung</b>, und sie ist bewusst so gebaut: Sie zu
 * unterscheiden braeuchte ein drittes Feld im Katalog, und dieser Schritt erfindet keines. Der
 * Befund steht in {@code docs/process-view.md} §4.
 *
 * @param richtung der kuratierte Rohwert ({@code EINGEHEND} oder {@code AUSGEHEND}), oder {@code
 *     null} fuer <b>nicht ermittelt</b>. Der Wert wird durchgereicht und nicht gegen {@link
 *     Richtung} geprueft: Die geschlossene Menge gilt beim <i>Schreiben</i> der Katalogpflege; beim
 *     Lesen einen unbekannten Wert abzuweisen hiesse, eine Ansicht an einem Datenzustand scheitern
 *     zu lassen, den sie nur anzeigen soll (Regel Q4)
 * @param anzahlProzesse wie viele Blaetter unter diesem Knoten haengen
 * @param nachrichten die Summe der Blaetter
 * @param fehler die Summe der Blaetter
 * @param prozesse die Blaetter, nach {@code ProcessName} sortiert
 */
public record RichtungsknotenResponse(
    String richtung,
    int anzahlProzesse,
    long nachrichten,
    long fehler,
    List<ProzessknotenResponse> prozesse) {}
