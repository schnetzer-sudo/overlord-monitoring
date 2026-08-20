package de.kraftwerkone.overlord.monitor.message;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Die kuratierte Auswahl technischer Eigenschaften, die im Kopf der Detailansicht erscheinen.
 *
 * <p><b>Eine Konstante im Code und keine Tabelle.</b> Es sind wenige Namen, sie sind nicht
 * mandantenabhaengig, und sie aendern sich nicht. Vorbild ist der {@code MessageStatusClassifier},
 * nicht {@code bam_spalte} — dort ging es um eine je Mandant <i>verschiedene</i> Konfiguration,
 * hier nicht. Eine Tabelle waere eine zweite Wahrheit, die der ersten irgendwann hinterherliefe.
 *
 * <h2>Warum diese zwei und keine dritte</h2>
 *
 * <p>Die Auswahl ist aus der Namensliste in M17 hergeleitet und gegen die Testkopie nachgemessen.
 * Der Massstab ist die Lehre aus den BAM-Spalten in Schritt 4: <b>Ein kuratiertes Feld, das fast
 * immer leer ist, verschlechtert die Ansicht.</b> Gemessen wurde deshalb die Befuellung je
 * <i>Nachricht</i> und <b>je Mandant</b> — die Quote ueber den Gesamtbestand verdeckt den
 * entscheidenden Befund, weil {@code NEXANS} das Aufkommen traegt:
 *
 * <table border="1">
 *   <caption>Anteil der Nachrichten mit dem Namen, dichter Tag / dichter Monat</caption>
 *   <tr><th>Name</th><th>gesamt</th><th>je Mandant</th><th>drin?</th></tr>
 *   <tr><td>{@code Message.SendingPartner}</td><td>11,0 % / 17,0 %</td>
 *       <td><b>IBISGUS 98,8 % / 98,7 %</b>, IBIS 75,1 % / 75,5 %, VOTG 17,5 %, NEXANS 7,9 %</td>
 *       <td><b>ja</b></td></tr>
 *   <tr><td>{@code Message.SplitCount}</td><td>16,9 % / 22,2 %</td>
 *       <td><b>SUTTONS 98,1 % / 93,9 %</b>, NEXANS 7,6 % / 15,2 %, sonst 0 %</td>
 *       <td><b>ja</b></td></tr>
 *   <tr><td>{@code Message.InterchangeNumber}</td><td>0 % / 0,37 %</td>
 *       <td>hoechster Mandantenwert 0,41 %</td><td>nein</td></tr>
 *   <tr><td>{@code Message.CommitInterchangeNumber}</td><td>0,03 % / 0,28 %</td>
 *       <td>hoechster Mandantenwert 0,40 %</td><td>nein</td></tr>
 *   <tr><td>{@code Message.SNDPRN}</td><td>6,1 %</td>
 *       <td>nur NEXANS, dort 7,6 %</td><td>nein</td></tr>
 *   <tr><td>{@code Message.VFN}</td><td>6,4 %</td>
 *       <td>—</td><td>nein, zusaetzlich Bedeutung unbelegt</td></tr>
 * </table>
 *
 * <p><b>Die Austauschnummer faellt heraus</b>, und zwar auf jeder Lesart: Ihr hoechster gemessener
 * Wert liegt bei 0,4 Prozent, fuer keinen einzigen Mandanten hoeher. Sie waere genau die BAM-Spalte
 * gewesen, die auf 98,93 Prozent der Zeilen leer stand.
 *
 * <p><b>{@code Message.VFN} bleibt draussen, auch unabhaengig von der Quote.</b> Was die Abkuerzung
 * bedeutet, ist <i>nicht</i> gemessen; die naheliegende Auflösung ist eine Vermutung aus dem
 * Praefix eines Nachbarnamens (M17 4, offene Frage 10). Regel Q4 — nicht geraten.
 *
 * <h2>Was ausdruecklich ausgeschlossen ist</h2>
 *
 * <p>Interne Kennungen, obwohl sie auf <b>jeder</b> Nachricht stehen: {@code Message.GUID}, {@code
 * Message.SOS}, {@code Message.Payload.GUID} und {@code Message.SourceMessageID} (je 100 bzw. 65,6
 * Prozent). Sie sind nach dem Leitsatz Beiwerk, keine Hauptinformation. {@code
 * Message.SourceMessageID} wird in Schritt 6 eine <i>Verkettung</i>. Ebenso draussen: {@code
 * Message.MessageActionID}, {@code Message.SOSActionID} und {@code
 * Message.SOSActionServiceProperties} — sie stehen ebenfalls auf jeder Nachricht, wiederholen aber
 * nur, was die Schrittfolge ohnehin zeigt.
 *
 * <p><b>Berichtigt am 19.08.2026.</b> Hier stand bis heute, {@code Message.Payload.GUID} werde „in
 * Schritt 8 ein <i>Knopf</i> und kein Anzeigewert". Er wird in Schritt 8 <b>gar nichts</b>: Nach
 * M73 traegt der Name den Verweis der Nutzdatenzeile mit dem hoechsten {@code MessageActionID}
 * derselben Nachricht und faellt aus der Artefaktliste ({@code Artefaktnamen#NAME_ZEIGER}). Der
 * Ausschluss hier bleibt richtig — nur seine Begruendung nicht.
 *
 * <p><b>Keine deutschen Beschriftungen hier.</b> Geliefert werden Rohname, Wert und Rang; die
 * Beschriftung kommt aus der Sprachdatei der Oberflaeche. Das ist dieselbe Aufteilung, die die API
 * schon bei Status und Fehlertypen haelt.
 */
public final class KuratierteEigenschaften {

  private KuratierteEigenschaften() {}

  /**
   * Die Namen in Anzeigereihenfolge. Der <b>Rang</b> ist die Position in dieser Liste, beginnend
   * bei 1 — die Reihenfolge steht damit an einer Stelle und nicht zusaetzlich in der Oberflaeche.
   */
  private static final List<String> NAMEN =
      List.of(
          // Absenderkennung — traegt IBISGUS (98,8 %) und IBIS (75,5 %).
          "Message.SendingPartner",
          // Aufteilungszahl — traegt SUTTONS (93,9 bis 98,1 %).
          "Message.SplitCount");

  /** Name auf Rang, ab 1. */
  private static final Map<String, Integer> RANG =
      IntStream.range(0, NAMEN.size())
          .boxed()
          .collect(Collectors.toUnmodifiableMap(NAMEN::get, index -> index + 1));

  /** Die kuratierten Namen, in Anzeigereihenfolge. */
  public static List<String> namen() {
    return NAMEN;
  }

  /**
   * Der Rang eines Namens, oder {@code null}, wenn er nicht kuratiert ist.
   *
   * <p>Der Vergleich ist <b>gross-/kleinschreibungsempfindlich</b>, anders als bei den
   * Bausteinmarken in {@link Schrittnamen}: {@code MessagePropertyName} ist Teil des
   * Primaerschluessels und kommt in den 101 gemessenen Namen in genau einer Schreibweise vor (M17
   * 2). Wuerde hier angeglichen, verdeckte das einen kuenftigen zweiten Schreibweisen-Fall, statt
   * ihn sichtbar zu machen.
   */
  public static Integer rang(String name) {
    return RANG.get(name);
  }
}
