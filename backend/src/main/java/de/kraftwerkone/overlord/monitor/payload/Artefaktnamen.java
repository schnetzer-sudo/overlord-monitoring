package de.kraftwerkone.overlord.monitor.payload;

import java.util.List;

/**
 * Was sich aus einem {@code MessagePropertyName} ablesen laesst — und was nicht.
 *
 * <p>Gemessen sind genau zwei Namensmuster (M54): {@code <Dienst>.Payload.GUID} und {@code
 * <Dienst>.Log.GUID}, durchgaengig, ohne Ausnahme. Diese Klasse kennt beide und leitet daraus die
 * {@link Artefaktart} und die <b>technische Familie</b> ab — den Teil vor dem Muster, also {@code
 * FileReader}, {@code Converter}, {@code FTPSender} und so fort.
 *
 * <p><b>Die Familie ist keine Deutung.</b> Sie ist die Zeichenkette, die im Namen steht, und wird
 * nirgends uebersetzt, ergaenzt oder erraten (Regel Q4). Ob daraus eine deutsche Beschriftung wird,
 * entscheidet die Oberflaeche.
 *
 * <h2>Die eine Ausnahme: {@code Message.Payload.GUID} ist kein Artefakt</h2>
 *
 * <p>Sie steht hier und nicht im Statement, damit sie neben ihrer Begruendung steht. Siehe {@link
 * #NAME_ZEIGER} und {@link #istZeiger(String)}; angewandt wird sie in {@link #istArtefakt(String)}
 * und damit an genau einer Stelle des Datenzugriffs ({@code ArtefaktRepository#findeArtefakte}).
 */
public final class Artefaktnamen {

  private Artefaktnamen() {}

  /** Namensmuster der Nutzdateien. */
  static final String MUSTER_NUTZDATEN = ".Payload.GUID";

  /** Namensmuster der Protokolle. */
  static final String MUSTER_PROTOKOLL = ".Log.GUID";

  /**
   * Der Name, der <b>kein eigenes Artefakt benennt</b>, sondern eine Datei, die ohnehin an ihrem
   * Schritt haengt.
   *
   * <p><b>Gemessen (M73, 19.08.2026):</b> {@code Message.Payload.GUID} traegt in <b>6.249 von
   * 6.249</b> Nachrichten (Fenster A) und <b>214.330 von 214.330</b> (Fenster B) denselben Verweis
   * wie die Nutzdatenzeile mit dem <b>hoechsten {@code MessageActionID}</b> derselben Nachricht.
   * Kein Gegenfall in beiden Fenstern. Zwei gleiche Verweise heissen dieselbe Datei und nicht zwei
   * aehnliche — der Verweis ist {@code <Ablagenkennung>|<UUID>} und durchgaengig so gebaut (M54).
   *
   * <p><b>„Hoechster {@code MessageActionID}" ist die gemessene Groesse.</b> „Zuletzt erzeugt"
   * waere eine Deutung darueber, dass die Schrittnummer die Ausfuehrungsreihenfolge ist; sie ist
   * plausibel und nicht gemessen und steht deshalb in keinem Namen und in keinem Kommentar.
   *
   * <p><b>Er faellt aus der Liste, statt umbenannt zu werden.</b> Eine Liste, die jede Datei genau
   * einmal fuehrt, ist die richtige Liste. Verloren geht dabei nichts: 6.248 von 6.249 bzw. 214.297
   * von 214.330 sind ueber einen Schritt ab {@code 1} erreichbar, der Rest ueber die Zeilen auf
   * Schritt {@code 0} (M73).
   *
   * <p>Bis zum 19.08.2026 hiess diese Konstante {@code NAME_EINGANG} und galt als „die eingegangene
   * Datei". Die Beschriftung traf in 0,016 % bzw. 0,015 % der Nachrichten zu.
   */
  static final String NAME_ZEIGER = "Message" + MUSTER_NUTZDATEN;

  /**
   * Die beiden {@code LIKE}-Muster fuer die Abfrage. {@code %} ist der einzige Platzhalter darin;
   * ein {@code _} kaeme in einem Muster mit derselben Wirkung vor und ist deshalb hier bewusst
   * nicht enthalten — dieselbe Falle, die {@code MessageStatusClassifier} bei {@code ERROR_%}
   * behandelt.
   *
   * <p><b>{@link #NAME_ZEIGER} steht hier nicht als Ausnahme drin.</b> Die Muster gehen
   * unveraendert ins Statement; aussortiert wird in {@link #istArtefakt(String)}, also im Code.
   */
  static List<String> likeMuster() {
    return List.of("%" + MUSTER_NUTZDATEN, "%" + MUSTER_PROTOKOLL);
  }

  /**
   * Ob dieser Name ein Artefakt benennt: eines der beiden gemessenen Muster — und nicht {@link
   * #NAME_ZEIGER}.
   */
  public static boolean istArtefakt(String name) {
    return art(name) != null && !istZeiger(name);
  }

  /**
   * Die Art, oder {@code null}, wenn der Name keines der beiden Muster traegt.
   *
   * <p>Die Reihenfolge der Pruefung ist gleichgueltig: Die beiden Muster schliessen einander aus,
   * weil ein Name nicht auf beide Endungen zugleich enden kann.
   *
   * <p><b>Sie kennt {@link #NAME_ZEIGER} nicht.</b> Diese Methode beantwortet die Frage nach dem
   * Muster, und die Antwort auf sie ist fuer {@code Message.Payload.GUID} unveraendert {@link
   * Artefaktart#NUTZDATEN}. Ob daraus ein Artefakt wird, entscheidet {@link #istArtefakt(String)}.
   */
  public static Artefaktart art(String name) {
    if (name == null) {
      return null;
    }
    if (name.endsWith(MUSTER_NUTZDATEN) && name.length() > MUSTER_NUTZDATEN.length()) {
      return Artefaktart.NUTZDATEN;
    }
    if (name.endsWith(MUSTER_PROTOKOLL) && name.length() > MUSTER_PROTOKOLL.length()) {
      return Artefaktart.PROTOKOLL;
    }
    return null;
  }

  /**
   * Die technische Familie — der Teil vor dem Muster, unveraendert.
   *
   * @return {@code null}, wenn der Name keines der beiden Muster traegt
   */
  public static String familie(String name) {
    Artefaktart art = art(name);
    if (art == null) {
      return null;
    }
    String muster = art == Artefaktart.NUTZDATEN ? MUSTER_NUTZDATEN : MUSTER_PROTOKOLL;
    return name.substring(0, name.length() - muster.length());
  }

  /**
   * Ob dies der Zeiger ist — {@code Message.Payload.GUID}, der Name aus {@link #NAME_ZEIGER}.
   *
   * <p>Gross-/kleinschreibungsempfindlich, wie bei {@code KuratierteEigenschaften}: {@code
   * MessagePropertyName} ist Teil des Primaerschluessels und kommt in den gemessenen Namen in genau
   * einer Schreibweise vor (M17 2, M54). Wuerde hier angeglichen, verdeckte das einen kuenftigen
   * zweiten Schreibweisen-Fall, statt ihn sichtbar zu machen.
   */
  public static boolean istZeiger(String name) {
    return NAME_ZEIGER.equals(name);
  }
}
