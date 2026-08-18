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
 * <p><b>{@code Message.Payload.GUID} ist der Sonderfall</b>: die eingegangene Datei. Sie haengt auf
 * Schritt {@code 0}, dem Ort der Metadaten, und ist kein Ablaufschritt (M57, M17 (3)). Sie ist
 * deshalb <i>nicht</i> Teil der Schrittfolge, sondern steht im Kopf der Liste.
 */
public final class Artefaktnamen {

  private Artefaktnamen() {}

  /** Namensmuster der Nutzdateien. */
  static final String MUSTER_NUTZDATEN = ".Payload.GUID";

  /** Namensmuster der Protokolle. */
  static final String MUSTER_PROTOKOLL = ".Log.GUID";

  /** Die Familie der eingegangenen Datei — sie allein steht im Kopf der Liste. */
  static final String FAMILIE_EINGANG = "Message";

  /** Der volle Name der eingegangenen Datei. */
  static final String NAME_EINGANG = FAMILIE_EINGANG + MUSTER_NUTZDATEN;

  /**
   * Die beiden {@code LIKE}-Muster fuer die Abfrage. {@code %} ist der einzige Platzhalter darin;
   * ein {@code _} kaeme in einem Muster mit derselben Wirkung vor und ist deshalb hier bewusst
   * nicht enthalten — dieselbe Falle, die {@code MessageStatusClassifier} bei {@code ERROR_%}
   * behandelt.
   */
  static List<String> likeMuster() {
    return List.of("%" + MUSTER_NUTZDATEN, "%" + MUSTER_PROTOKOLL);
  }

  /** Ob der Name eines der beiden gemessenen Muster traegt. */
  public static boolean istArtefakt(String name) {
    return art(name) != null;
  }

  /**
   * Die Art, oder {@code null}, wenn der Name keines der beiden Muster traegt.
   *
   * <p>Die Reihenfolge der Pruefung ist gleichgueltig: Die beiden Muster schliessen einander aus,
   * weil ein Name nicht auf beide Endungen zugleich enden kann.
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
   * Ob dies die eingegangene Datei ist — {@code Message.Payload.GUID}.
   *
   * <p>Gross-/kleinschreibungsempfindlich, wie bei {@code KuratierteEigenschaften}: {@code
   * MessagePropertyName} ist Teil des Primaerschluessels und kommt in den gemessenen Namen in genau
   * einer Schreibweise vor (M17 2, M54). Wuerde hier angeglichen, verdeckte das einen kuenftigen
   * zweiten Schreibweisen-Fall, statt ihn sichtbar zu machen.
   */
  public static boolean istEingang(String name) {
    return NAME_EINGANG.equals(name);
  }
}
