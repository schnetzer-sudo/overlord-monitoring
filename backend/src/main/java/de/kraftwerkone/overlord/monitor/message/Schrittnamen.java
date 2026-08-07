package de.kraftwerkone.overlord.monitor.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Die dreistufige Aufloesung eines Prozessschritts in Klartext — <b>ohne Datenbankzugriff</b>.
 *
 * <p>Diese Klasse ist der Grund, warum die Detailansicht ueberhaupt Klartext liefert, und sie
 * arbeitet ausschliesslich aus uebergebenen Daten. Was sie braucht, sind die {@code SOSAction}-
 * Zeilen <b>aller</b> Ablaeufe, die die Nachricht beruehrt hat; was sie liefert, ist je
 * ausgefuehrter Aktion ein {@link Schrittname} samt {@link Namensherkunft}. Dadurch ist sie einzeln
 * pruefbar — die Tests dazu brauchen keine Datenbank und tragen deshalb kein {@code @Tag("db")}.
 *
 * <h2>Die drei Stufen, in dieser Reihenfolge</h2>
 *
 * <ol>
 *   <li>{@link Namensherkunft#DIREKT} — es gibt eine {@code SOSAction}-Zeile zu {@code (ma.SOSID,
 *       ma.SOSActionID)}; ihr {@code SOSActionName} wird verwendet.
 *   <li>{@link Namensherkunft#HERGELEITET} — es gibt keine. Dann wird die <b>erste Marke</b> aus
 *       {@code ma.SOSActionServiceProperties} genommen und im <b>selben</b> Ablauf nach {@code
 *       SOSAction}-Zeilen mit derselben ersten Marke gesucht. <b>Nur bei genau einem Treffer</b>
 *       wird dessen Name verwendet.
 *   <li>{@link Namensherkunft#ROHWERT} — {@code ma.SOSActionServiceProperties} unveraendert.
 * </ol>
 *
 * <p><b>Warum die Eindeutigkeitsbedingung in Stufe 2 nicht verhandelbar ist</b>, steht bei {@link
 * Namensherkunft#HERGELEITET}. Die Kurzfassung: Sie kostet nichts (M19 hat 96 bis 99 Prozent
 * Eindeutigkeit und <b>null</b> mehrdeutige Faelle gemessen) und sie verhindert genau den Fall, in
 * dem geraten wuerde — {@code FTPSender} loest anderswo auf 25 verschiedene Namen auf.
 *
 * <p><b>Die Zeilen mehrerer Ablaeufe gehoeren zusammen geladen.</b> 2,51 Prozent (Fenster A)
 * beziehungsweise 2,22 Prozent (Fenster B) der Nachrichten haben Schritte aus <b>mehr als einem</b>
 * Ablauf, bis zu drei (M20). Die Aufloesung geht deshalb nie von einem angenommenen einzigen Ablauf
 * aus: Beide Stufen schlagen ausschliesslich innerhalb des Ablaufs nach, den die jeweilige Aktion
 * selbst nennt.
 *
 * <p><b>Verglichen wird gross-/kleinschreibungsunabhaengig</b> ({@link Locale#ROOT}). Die
 * Sortierung des Quellschemas ist {@code utf8mb4_general_ci}, die Gleichheit in SQL also
 * unabhaengig von der Schreibweise — {@link Map#get} ist es nicht. Ohne diese Angleichung faende
 * Java weniger als die Messung gemessen hat. Dieselbe Falle und dieselbe Loesung wie in {@code
 * MessageStatusClassifier.einordnung}; {@code Locale.ROOT}, damit die Umwandlung nicht an der
 * Systemsprache haengt.
 */
public final class Schrittnamen {

  /** Das Trennzeichen der Bausteinliste in {@code SOSActionServiceProperties}. */
  private static final char TRENNER = '|';

  private record Schrittschluessel(String sosId, short sosActionId) {}

  private record Markenschluessel(String sosId, String marke) {}

  private final Map<Schrittschluessel, String> nachKennung;
  private final Map<Markenschluessel, List<String>> nachMarke;

  private Schrittnamen(
      Map<Schrittschluessel, String> nachKennung, Map<Markenschluessel, List<String>> nachMarke) {
    this.nachKennung = nachKennung;
    this.nachMarke = nachMarke;
  }

  /**
   * Baut die Aufloesung aus den Ablaufdefinitionen auf.
   *
   * @param ablaufschritte alle {@code SOSAction}-Zeilen der Ablaeufe, die die Nachricht beruehrt
   *     hat. Eine leere Menge ist zulaessig — dann traegt ausschliesslich Stufe 3.
   */
  public static Schrittnamen aus(Collection<Ablaufschritt> ablaufschritte) {
    Map<Schrittschluessel, String> nachKennung = new HashMap<>();
    Map<Markenschluessel, List<String>> nachMarke = new HashMap<>();
    for (Ablaufschritt schritt : ablaufschritte) {
      if (schritt == null || schritt.sosId() == null) {
        continue;
      }
      String ablauf = angeglichen(schritt.sosId());
      // Nur brauchbare Namen kommen in die Karte. Eine Zeile ohne Namen faellt damit auf Stufe 2
      // und 3 durch, statt den Schritt leer zu lassen — „gibt es nicht" und „hat keinen Namen"
      // sind hier bewusst dasselbe. In der Testkopie kommt der zweite Fall nicht vor: Wo der Join
      // eine Zeile findet, traegt sie immer einen Namen (M18, in jeder Zeile beider Fenster).
      if (brauchbar(schritt.name())) {
        nachKennung.put(new Schrittschluessel(ablauf, schritt.sosActionId()), schritt.name());
      }

      String marke = ersteMarke(schritt.bausteine());
      if (marke != null) {
        // Auch namenlose Zeilen zaehlen mit: Gemessen ist die Eindeutigkeit ueber ALLE Zeilen des
        // Ablaufs mit dieser Marke (M19). Wer nur die benannten zaehlte, machte eine mehrdeutige
        // Lage still eindeutig.
        nachMarke
            .computeIfAbsent(
                new Markenschluessel(ablauf, angeglichen(marke)), unused -> new ArrayList<>())
            .add(brauchbar(schritt.name()) ? schritt.name() : "");
      }
    }
    return new Schrittnamen(Map.copyOf(nachKennung), Map.copyOf(nachMarke));
  }

  /**
   * Loest eine ausgefuehrte Aktion auf.
   *
   * @param sosId {@code MessageAction.SOSID} — <b>nicht</b> {@code Message.SOSID}. Ueber die zweite
   *     zu joinen liefert in 3,83 Prozent der Faelle einen fachlich falschen Namen, ohne dass es
   *     auffiele, weil trotzdem eine Zeile zurueckkommt (M15)
   * @param sosActionId {@code MessageAction.SOSActionID}
   * @param rohwert {@code MessageAction.SOSActionServiceProperties}
   */
  public Schrittname loese(String sosId, short sosActionId, String rohwert) {
    String ablauf = sosId == null ? null : angeglichen(sosId);

    if (ablauf != null) {
      String direkt = nachKennung.get(new Schrittschluessel(ablauf, sosActionId));
      if (brauchbar(direkt)) {
        return new Schrittname(direkt, Namensherkunft.DIREKT, rohwert);
      }
      String hergeleitet = ueberMarke(ablauf, rohwert);
      if (hergeleitet != null) {
        return new Schrittname(hergeleitet, Namensherkunft.HERGELEITET, rohwert);
      }
    }
    return new Schrittname(rohwert, Namensherkunft.ROHWERT, rohwert);
  }

  /** Stufe 2 — und {@code null}, sobald sie nicht eindeutig traegt. */
  private String ueberMarke(String ablauf, String rohwert) {
    String marke = ersteMarke(rohwert);
    if (marke == null) {
      return null;
    }
    List<String> treffer = nachMarke.get(new Markenschluessel(ablauf, angeglichen(marke)));
    if (treffer == null || treffer.size() != 1) {
      return null;
    }
    String name = treffer.getFirst();
    return brauchbar(name) ? name : null;
  }

  /**
   * Die erste Marke eines Bausteinwerts: alles vor dem ersten {@code |}, oder der ganze Wert, wenn
   * keiner vorkommt.
   *
   * <p>{@code null} bei {@code null} und bei einem Wert, dessen erste Marke leer ist — aus nichts
   * laesst sich nichts herleiten. Ein Wert wie {@code |WAIT|30M} hat keine erste Marke, und ein
   * Ablauf, in dem <i>mehrere</i> Schritte keine haetten, waere ueber sie ohnehin nicht
   * unterscheidbar.
   *
   * <p>Der Trenner ist derselbe wie im Quellsystem, das die Liste erzeugt: {@code
   * NXS_FILE_CONVERT|E2A|UNWRAP}. Gemessen ist das Verhaeltnis 78 erste Marken zu 169 ganzen Werten
   * im Tagesfenster — alles hinter der ersten Marke sind Parameter (M15).
   */
  static String ersteMarke(String bausteine) {
    if (bausteine == null) {
      return null;
    }
    int trenner = bausteine.indexOf(TRENNER);
    String marke = trenner < 0 ? bausteine : bausteine.substring(0, trenner);
    return marke.isBlank() ? null : marke;
  }

  private static boolean brauchbar(String name) {
    return name != null && !name.isBlank();
  }

  private static String angeglichen(String wert) {
    return wert.toUpperCase(Locale.ROOT);
  }
}
