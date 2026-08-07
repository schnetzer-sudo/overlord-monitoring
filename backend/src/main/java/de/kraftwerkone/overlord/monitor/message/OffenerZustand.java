package de.kraftwerkone.overlord.monitor.message;

/**
 * Woran eine <b>offene</b> Nachricht gerade steht — benannt, statt die Oberflaeche raten zu lassen.
 *
 * <p>Der Anlass ist gemessen. M16 (3) hat fuer <b>alle 538</b> {@code SUSPENDED}-Nachrichten
 * festgestellt, dass <b>jede</b> ihrer Aktionen beendet ist: Eine wartende Nachricht steht
 * <i>zwischen</i> zwei Schritten und nicht auf einem laufenden. Genau deshalb muss die Zeitleiste
 * drei Zustaende je Schritt tragen — beendet, laufend, und den Zwischenraum nach dem letzten
 * beendeten Schritt. <b>Der Zwischenraum ist kein Schritt und wird keiner Zeile zugeschlagen.</b>
 *
 * <p><b>Nicht ueber {@code MessageStatus = 'RUNNING'} definiert</b> — diesen Wert gibt es in der
 * Testkopie null Mal. <b>Und nicht auf {@code ERROR_TIMEOUT} gestuetzt</b>: M8 hat gezeigt, dass
 * dieser Status nicht das Ablaufen von {@code MessageTimeout} ist, sondern eine kuerzere Frist auf
 * Dienstebene.
 *
 * <p>„Offen" heisst hier {@code WARTEND} oder {@code LAEUFT} aus dem {@code
 * MessageStatusClassifier}. Das ist dieselbe Zweierauswahl, die {@code
 * MessageStatusClassifier.istEndstatus} trifft — sie wird aber <b>ausdruecklich aufgezaehlt und
 * nicht ueber jene Methode geholt</b>. {@code docs/message-status.md} fuehrt dazu eine eigene
 * Warnung: Jene Methode gehoert der Ueberfaelligkeitsrechnung, und fuer {@code UNGEKLAERT}
 * antwortet sie {@code true} — in ihrem Zusammenhang die vorsichtige Antwort, hier waere dieselbe
 * {@code true} die unvorsichtige. Dieselbe Entscheidung trifft die Liste in {@code
 * NachrichtenService.aktuellerSchritt}, aus demselben Grund.
 */
public enum OffenerZustand {

  /**
   * Die Nachricht ist offen, und mindestens eine Aktion hat kein {@code MessageActionEnd}. Die
   * betroffene Aktion ist in der Schrittfolge markiert.
   *
   * <p>⚠️ <b>Lokal kaum pruefbar.</b> Im gesamten Bestand tragen 95 von 10,3 Millionen Aktionen
   * dieses Merkmal, und die 49 davon, die zu einem <i>offenen</i> Fehlerfall gehoeren, liegen
   * saemtlich in einer Spanne von 62 Sekunden an einem einzigen Tag (M22) — ein Massenereignis,
   * kein Vorbild fuer den Normalbetrieb. Die uebrigen 46 gehoeren zu {@code FINISHED} und {@code
   * CHECKED}, sind also nach dieser Definition <b>nicht</b> offen und ergeben {@link #KEINER}.
   */
  LAEUFT_AUF,

  /**
   * Die Nachricht ist offen, aber <b>jede</b> Aktion ist beendet — sie wartet vor dem naechsten
   * Schritt.
   *
   * <p><b>Der gemessene Normalfall des Wartens, nicht der Sonderfall</b> (M16 3): Alle 538
   * wartenden Nachrichten der Testkopie stehen so. Nur in diesem Zustand wird der naechste Schritt
   * mitgeliefert.
   */
  WARTET_VOR,

  /**
   * Die Nachricht ist offen, aber es gibt gar keine Aktion.
   *
   * <p>Kommt in der Testkopie ueber alle geprueften Status <b>null Mal</b> vor (M16 3). Er ist
   * damit nicht widerlegt, sondern nur nicht beobachtet — {@code MessageAction} hat keinen Zwang,
   * der ihn ausschloesse, und die Detailansicht haelt ihn deshalb aus.
   */
  OHNE_SCHRITT,

  /** Die Nachricht ist nicht offen. Dann gibt es keinen Schritt, auf dem sie stuende. */
  KEINER
}
