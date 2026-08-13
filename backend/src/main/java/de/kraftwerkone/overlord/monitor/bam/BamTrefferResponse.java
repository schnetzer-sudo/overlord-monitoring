package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import java.time.Instant;
import java.util.List;

/**
 * Eine gefundene Nachricht, so wie der Aufrufer sie sieht: <b>dieselben Felder wie in der
 * Nachrichtenliste</b>, dazu {@code treffer} und {@code rollen}.
 *
 * <p>Die zehn Listenfelder sind Wort für Wort die aus {@code message/NachrichtResponse} — <b>ohne
 * dass ein Fachpaket aus dem anderen importierte</b>. Wer eine Belegnummer sucht, landet danach in
 * derselben Ansicht wie aus der Liste; eine zweite, ähnliche Zeilengestalt wäre für den Nutzer eine
 * zweite Sprache.
 *
 * <p><b>Kein Sammelstatus über die Kette.</b> Er kostete je Trefferzeile eine Abwärtsauflösung und
 * stünde bei 11,38 Prozent der Merge-Ergebnisse auf einer gedeckelten Menge — also auf einer Zahl,
 * die nicht die ganze Wahrheit ist. Wer wissen will, was an der Nachricht hängt, öffnet sie.
 *
 * @param zeitpunkt {@code MessageLastUpdate} als UTC-Zeitpunkt. <b>Kein Anlagedatum</b> — die
 *     Quelle hat keines (Regel Q2)
 * @param bedeutungNichtVerifiziert bei {@code UNGEKLAERT}: Der Wert kommt so aus dem Altsystem,
 *     seine fachliche Bedeutung ist nicht belegt
 * @param schritt der Schritt, auf dem die Nachricht <b>gerade steht</b> — ausschließlich bei {@code
 *     WARTEND} und {@code LAEUFT}, sonst {@code null}. Dieselbe fachliche Entscheidung wie in der
 *     Liste: {@code SOSActionID} ist auf jeder Zeile gesetzt (M13), benennt bei abgeschlossenen
 *     Nachrichten aber den <i>letzten</i> Schritt
 * @param rollen die Stellung in der Verkettung — <b>immer vorhanden, leer statt fehlend</b>.
 *     <p><b>Sie steht hier, weil die Suche fast immer die Wurzel findet:</b> 96,87 Prozent der
 *     Wurzeln tragen BAM-Werte, nur 2,42 Prozent der Kinder (M26‑1b). Ohne die Rolle wüsste der
 *     Nutzer nicht, dass er das Bündel gefunden hat und nicht das Einzelstück. Sie kostet
 *     <b>keinen</b> zusätzlichen Zugriff (E4): Die vier Spalten liegen auf der Zeile, die die
 *     Abfrage ohnehin liest
 * @param treffer welcher Typ mit welchem Wert getroffen hat. Mehrere sind kein Randfall — bei 4,17
 *     Prozent der Paare steht derselbe Wert unter mehreren Typen (M37)
 */
public record BamTrefferResponse(
    String messageId,
    Instant zeitpunkt,
    String status,
    String statusKind,
    boolean bedeutungNichtVerifiziert,
    String processId,
    String processName,
    String projectName,
    String sosName,
    String schritt,
    List<Kettenrolle> rollen,
    List<BamTrefferWertResponse> treffer) {

  public BamTrefferResponse {
    rollen = List.copyOf(rollen);
    treffer = List.copyOf(treffer);
  }
}
