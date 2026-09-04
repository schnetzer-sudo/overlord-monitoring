package de.kraftwerkone.overlord.monitor.message;

/**
 * Was eine {@link Nachrichtenabfrage} traegt — <b>ein Wert je Bestandteil des Records</b>, in
 * derselben Reihenfolge.
 *
 * <h2>Wozu es dieses Enum gibt</h2>
 *
 * <p>Die Fensterverengung ({@link Fensterverengung}) darf nur greifen, wenn der Rollup <b>jedes</b>
 * Merkmal der Abfrage mittragen kann. Traegt er eines nicht und verengt trotzdem, verschwinden
 * Zeilen — <b>lautlos, ohne Fehlermeldung</b>. Gemessen am 27.08.2026 (offener Punkt 72 in {@code
 * docs/messungen-liste-verengung.md}): Mit {@code status=FEHLER} lieferte {@code SUTTONS} <b>null
 * statt fuenf</b> Zeilen und {@code NEXANS} <b>49 statt 51</b>, weil die Verengung den Statusfilter
 * nicht mitgerechnet hatte. In einem Ueberwachungswerkzeug ist „keine Fehler" die schlimmste
 * falsche Antwort, die es gibt.
 *
 * <h2>Warum ein Enum und kein Kommentar</h2>
 *
 * <p>Weil ein Kommentar nicht bricht. {@link Fensterverengung} entscheidet ueber dieses Enum in
 * <b>zwei vollstaendigen {@code switch}-Ausdruecken ohne {@code default}</b>. Ein neuer Wert hier
 * loest damit zwei Compilerfehler aus, und niemand kann ein neues Merkmal einfuehren, ohne fuer die
 * Verengung ausdruecklich zu entscheiden. Dieselbe Bauform wie {@code
 * MessageStatusClassifier.istEndstatus} und {@code bedingung(MessageStatusKind, Field)}, aus
 * demselben Grund.
 *
 * <p><b>Der zweite Riegel liegt im Test.</b> Ein Enum-Wert schuetzt nicht davor, dass jemand dem
 * Record {@link Nachrichtenabfrage} ein <i>Feld</i> hinzufuegt, ohne hier einen Wert zu ergaenzen.
 * {@code FensterverengungMerkmaleTest} geht deshalb ueber {@code
 * Nachrichtenabfrage.class.getRecordComponents()} und verlangt fuer jeden Bestandteil einen
 * zugeordneten Wert dieses Enums. Erst beide Riegel zusammen schliessen die Luecke.
 */
public enum Abfragemerkmal {

  /** {@code fenster} — das Pflicht-Zeitfenster. Die Verengung ist genau eine Operation darauf. */
  ZEITFENSTER,

  /** {@code status} — die Auswahl ueber {@code MessageStatusKind}, leer heisst „alle". */
  STATUS,

  /** {@code prozessIds} — die ausdrueckliche Prozessauswahl des Nutzers. */
  PROZESSE,

  /** {@code suchtreffer} — der aufgeloeste Freitext, {@code null} heisst „nicht gesucht". */
  SUCHBEGRIFF,

  /*
   * Hier standen bis zum 03.09.2026 `UEBERFAELLIG` und `JETZT`. Beide sind mit ihren
   * Bestandteilen in `Nachrichtenabfrage` entfallen (E-71): Die Problemkategorie `Ueberfaellig`
   * ist widerlegt, und `jetzt` hatte ausser ihr keinen Verbraucher.
   *
   * DAMIT VERLIERT DIE VERENGUNG IHRE EINZIGE ABSCHALTUNG UEBER DEN STATUS: `UEBERFAELLIG` war das
   * eine Merkmal, das der Rollup nicht mittragen konnte, weil `MessageTimeout` dort nicht steht.
   * Uebrig bleibt `SUCHBEGRIFF` als einziges `false` -- und die Begruendung dort ist eine andere
   * (message_rollup hat keine sos_id). Das ist kein Verlust an Sicherheit: Ein Merkmal, das es
   * nicht mehr gibt, kann auch nicht falsch als tragbar gelten.
   */

  /** {@code absteigend} — die Sortierrichtung. Sie entscheidet, an welchem Ende gezaehlt wird. */
  SORTIERUNG,

  /** {@code cursor} — die Seitenposition, {@code null} heisst „erste Seite". */
  CURSOR,

  /** {@code limit} — wie viele Zeilen die Seite traegt. Er setzt die Schwelle der Verengung. */
  LIMIT
}
