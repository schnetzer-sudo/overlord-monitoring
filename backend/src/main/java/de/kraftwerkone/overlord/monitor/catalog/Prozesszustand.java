package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Der Zustand eines Prozesses in der Prozessansicht — <b>drei Werte, und sie duerfen nie in einen
 * Eimer</b>.
 *
 * <p>Ein Prozess, der nie etwas trug, ist <b>nicht still — er war nie laut</b>, und die Handlung
 * daraus ist eine andere: Der eine ist ein Vorfall, der andere eine Katalogfrage.
 *
 * <h2>Alle drei haengen an <i>einem</i> Wert, und der ist fensterunabhaengig</h2>
 *
 * <p>Der Wert ist die <b>letzte Bewegung</b> — {@code MAX(stunde)} ueber {@code message_rollup} je
 * Prozess, ohne jedes Zeitfenster. Wer 48 Stunden waehlt, bekommt trotzdem „still" im Sinne von
 * <i>seit drei Monaten nichts</i> und nicht im Sinne von <i>in diesen 48 Stunden nichts</i>. Eine
 * fensterabhaengige Markierung markierte bei 48 Stunden den Normalfall.
 *
 * <h2>Warum {@link #BEWEGT} nicht „hat im Zeitraum getragen" heisst</h2>
 *
 * <p>Der Auftrag beschreibt {@code BEWEGT} als <i>„hat im Zeitraum Nachrichten getragen"</i>.
 * Woertlich genommen waeren die drei Werte weder disjunkt noch vollstaendig: Ein Prozess mit
 * Verkehr vor fuenf Monaten waere in einem Zwoelf-Monats-Fenster <b>beides</b> (bewegt und still),
 * und ein Prozess, dessen letzte Bewegung zehn Tage zurueckliegt, waere in einem 48-Stunden-Fenster
 * <b>keines von dreien</b>.
 *
 * <p><b>Aufgeloest ist das ueber die Reihenfolge</b>, und sie folgt dem tragenden Satz des
 * Auftrags: <i>„Stille Prozesse werden markiert."</i> Markiert werden {@code NIE} und {@code
 * STILL}; {@code BEWEGT} ist der unmarkierte Normalfall und damit genau das, was uebrig bleibt. Die
 * Zahl <i>Nachrichten im Zeitraum</i> steht daneben und beantwortet die fensterabhaengige Frage —
 * sie ist eine Kennzahl und keine Markierung.
 */
public enum Prozesszustand {

  /** Traegt seit weniger als der Stilleschwelle Nachrichten. Der Normalfall, unmarkiert. */
  BEWEGT,

  /**
   * Trug schon einmal etwas, aber seit mehr als der Stilleschwelle nichts mehr.
   *
   * <p>Das ist ein <b>Vorfall</b>: Es gab eine Beziehung, und sie ist verstummt.
   */
  STILL,

  /**
   * Hat noch <b>nie</b> etwas getragen — kein einziger Rollupeimer, ueber den ganzen Bestand.
   *
   * <p>Das ist eine <b>Katalogfrage</b> und kein Vorfall: Der Vertrag steht, und es ist nie etwas
   * darueber gelaufen.
   */
  NIE
}
