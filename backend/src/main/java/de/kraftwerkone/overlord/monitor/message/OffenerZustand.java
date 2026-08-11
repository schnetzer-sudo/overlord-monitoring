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
 * <p><b>Der Wartezustand ist am 10.08.2026 aufgeteilt worden</b> (M29). Bis dahin verglich die
 * Oberflaeche zwei gelieferte Kennungen und entschied daraus, welchen Satz sie schreibt — das
 * widerspricht der Regel, dass das Backend den Zustand liefert und die Oberflaeche ihn nur
 * darstellt. Die Entscheidung liegt jetzt hier: {@link #WARTET_IN} gegen {@link #WARTET_VOR}.
 *
 * <p><b>Und {@code OHNE_SCHRITT} ist am selben Tag verschwunden</b> — aufgeteilt in {@link
 * #EMPFANGEN} und {@link #OHNE_AKTION}. Er trug zwei Faelle, die verschiedene Fragen beantworten:
 * eine Auskunft ueber die <b>Plattform</b> („angekommen, seitdem nichts passiert") und eine ueber
 * die <b>Datenlage</b> („kein Ablauf protokolliert"). Ein gemeinsamer Text muesste so vage sein,
 * dass er beides abdeckt — und waere dann fuer keinen der beiden brauchbar. <b>Der Name wird fuer
 * keinen der beiden weiterverwendet</b>, sonst ueberlebt die alte, unscharfe Bedeutung.
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
   * Die Nachricht ist offen, jede Aktion ist beendet, und {@code Message.SOSID}/{@code SOSActionID}
   * zeigen auf den <b>zuletzt ausgefuehrten</b> Schritt: Sie wartet <i>in</i> einem Schritt, der
   * sie schlafen gelegt hat.
   *
   * <p><b>Der gemessene Normalfall des Wartens</b> (M29): Bei <b>allen 538</b> wartenden
   * Nachrichten der Testkopie zeigt der Verweis auf den Schritt, der zuletzt gelaufen ist — den mit
   * {@code WAITUNTIL|…|SUSPEND}. Der Fall {@link #WARTET_VOR} kommt dort <b>null Mal</b> vor.
   *
   * <p>Der Verweis wird auch hier mitgeliefert. Die Oberflaeche zeigt ihn im Tooltip und nicht als
   * Zeile: Zwei gleich benannte Zeilen untereinander — „Send Message to Pool · 2 min" und darunter
   * „Send Message to Pool · noch nicht begonnen" — sind fuer einen Leser, der laut Leitsatz kein
   * EDI-Spezialist ist, schlicht ein Widerspruch.
   */
  WARTET_IN,

  /**
   * Die Nachricht ist offen, jede Aktion ist beendet, und der Verweis zeigt auf einen
   * <b>anderen</b> Schritt — sie wartet <i>vor</i> einem, der noch nicht begonnen hat.
   *
   * <p>⚠️ <b>In der Testkopie null Mal beobachtet</b> (M29, {@code n = 538}). Nicht widerlegt,
   * sondern nicht gesehen: Gemessen ist ausschliesslich {@code SUSPENDED}, und diese 538 sind
   * <b>eine</b> Gestalt — zwei echte Schritte, ein Ablauf, eine {@code SOSActionID}, sieben Tage.
   * Der Zweig bleibt deshalb gebaut und unit-getestet.
   */
  WARTET_VOR,

  /**
   * Die Nachricht ist offen, es gibt eine {@code MessageAction} mit {@code SOSActionID = 0} und
   * <b>keine</b> mit {@code SOSActionID <> 0}: Sie ist im System angekommen und seitdem nicht
   * weitergelaufen.
   *
   * <p><b>Eine Auskunft ueber die Plattform.</b> Der Schritt {@code 0} ist kein
   * Verarbeitungsschritt (S1) und erscheint deshalb nicht in der Zeitleiste — er <b>ist</b> aber
   * ein Ereignis mit einem Zeitpunkt, und zwar genau der, an dem die Nachricht ins System kommt
   * (M17 3). Aus demselben Grund rechnet der fachliche Start ueber ihn. {@code wartetSeitSekunden}
   * rechnet deshalb <b>ab dem Schritt {@code 0}</b> und ist hier nicht {@code null}: Eine
   * Nachricht, die um 14:32 angekommen und seitdem nicht angefasst worden ist, <b>haengt</b> — und
   * das ist der Zustand, in dem ein Nutzer das Werkzeug oeffnet.
   *
   * <p><b>Mit ihm bekommt {@code ueberfaellig} zum ersten Mal einen Zustand, in dem die Kategorie
   * wirklich etwas sagt:</b> eine Nachricht, die eingegangen und ueber ihre Frist hinaus nicht
   * weitergelaufen ist.
   *
   * <p>⚠️ <b>Lokal nicht vorfuehrbar, und zwar nicht aus Versaeumnis.</b> Im Produktivbetrieb tritt
   * der Fall auf, sobald Nachrichten eingehen und noch nicht weitergelaufen sind. In der Testkopie
   * ist der Bestand abgeschnitten, und {@code RUNNING} kommt null Mal vor — dort steht nichts mehr
   * am Anfang seiner Verarbeitung. Der Zweig ist gebaut und unit-getestet, wie {@link #LAEUFT_AUF}.
   */
  EMPFANGEN,

  /**
   * Die Nachricht ist offen, und es gibt <b>gar keine</b> {@code MessageAction}: Zu ihr ist kein
   * Ablauf protokolliert.
   *
   * <p><b>Eine Auskunft ueber die Datenlage</b>, nicht ueber die Plattform — deshalb ein eigener
   * Wert neben {@link #EMPFANGEN} und nicht derselbe Text fuer beide.
   *
   * <p><b>{@code wartetSeitSekunden} ist hier {@code null}, und das konsequent:</b> Es gibt keinen
   * Anker. Der fachliche Start ist aus demselben Grund ebenfalls {@code null} ({@code
   * MIN(MessageActionStart)} ueber eine leere Menge). Eine Dauer aus {@code MessageLastUpdate} zu
   * rechnen waere eine erfundene Zahl — der Zeitpunkt der letzten Aenderung ist nicht der Zeitpunkt
   * des Eingangs.
   *
   * <p>⚠️ <b>In der Testkopie nicht beobachtet.</b> M16 (3) hat {@code ohne_jede_aktion = 0} ueber
   * die geprueften Status gemessen. Der Fall ist damit <b>nicht widerlegt, nur nicht beobachtet</b>
   * — {@code MessageAction} kennt keinen Zwang, der ihn ausschloesse.
   */
  OHNE_AKTION,

  /** Die Nachricht ist nicht offen. Dann gibt es keinen Schritt, auf dem sie stuende. */
  KEINER;

  /**
   * Wartet die Nachricht — <i>in</i> einem Schritt oder <i>vor</i> einem?
   *
   * <p>Genau in diesen beiden Faellen wird der Verweis aus {@code Message.SOSID}/{@code
   * SOSActionID} mitgeliefert. Als Methode und nicht als Aufzaehlung an der Aufrufstelle, damit ein
   * kuenftiger siebter Zustand hier entschieden wird und nicht dort.
   */
  public boolean istWartend() {
    return this == WARTET_IN || this == WARTET_VOR;
  }
}
