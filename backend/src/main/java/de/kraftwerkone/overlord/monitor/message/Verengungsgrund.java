package de.kraftwerkone.overlord.monitor.message;

/**
 * Warum die Fensterverengung so ausgegangen ist, wie sie ausgegangen ist.
 *
 * <p>Der Wert ist <b>kein Protokollrauschen</b>: Er ist das, woran die Tests festmachen, dass der
 * richtige Pfad genommen wurde. Ein Test, der nur die gelieferten Zeilen prueft, kann nicht
 * unterscheiden, ob die Verengung gegriffen hat oder ob sie stillschweigend ausgefallen ist — beide
 * liefern dasselbe Ergebnis, und genau das ist die Falle. {@link #NULLFALL} und {@link #VERENGT}
 * sind die einzigen beiden Werte, bei denen ueberhaupt etwas anders laeuft als vor dem 30.08.2026.
 */
public enum Verengungsgrund {

  /** Das Fenster ist enger geworden. Die Quellabfrage laeuft ueber den kleineren Bereich. */
  VERENGT,

  /**
   * Der Rollup sagt fuer Mandant und Fenster <b>null Zeilen</b> — die Quellabfrage wird gar nicht
   * gestellt. Der {@code EDITIONLINGERI}-Fall und die billigste Zeile des Baus: 2.126,876 ms
   * gespart, ohne ein einziges Statement gegen {@code Message}.
   */
  NULLFALL,

  /** Der Schalter {@code overlord.nachrichtenliste.verengung} steht auf {@code false}. */
  ABGESCHALTET,

  /**
   * Mindestens ein gesetztes {@link Abfragemerkmal} traegt der Rollup nicht — {@code ueberfaellig},
   * ein Suchbegriff oder die Sortierrichtung {@code AELTESTE}. Es wird unverengt gefragt.
   */
  MERKMAL_NICHT_TRAGBAR,

  /**
   * Der Rollup hat geantwortet, aber keine Untergrenze gefunden: Ueber alle vollstaendig im Fenster
   * liegenden Stunden kommen weniger als {@code limit + 1} Zeilen zusammen. Das Fenster bleibt, wie
   * es war — es gibt nichts wegzuschneiden. Dieser Fall greift auch, wenn der <b>Wasserstand</b>
   * unter dem Fenster liegt und der Rollup deshalb nichts weiss.
   */
  KEINE_UNTERGRENZE
}
