package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.common.auftrag.AuftragsPruefung;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Baut den Parametertext, den die EDI-Plattform ausfuehrt ({@code docs/tasks.md} §5.2, {@code
 * docs/task-schnittstelle.md} §4). <b>Ausschliesslich hier</b> und ausschliesslich aus geprueften
 * Einzelwerten (E‑1006); kein Endpunkt nimmt Parametertext entgegen.
 *
 * <p>Erste Zeile {@code Aktion=<Aktion>}, dann je Eintrag die Zeile nach der Vorlage seiner Aktion,
 * in Eingabereihenfolge. Zeilenende LF, <b>kein</b> abschliessendes LF.
 *
 * <p><b>Ersetzt wird in einem Durchgang.</b> Ein Wert, der wie ein Platzhalter aussieht ({@code
 * {werk}}), bleibt stehen, wie er ist, und wird nicht noch einmal ersetzt.
 */
public final class Parametertext {

  /** Die feste Kopfzeile (Punkt 1006: ein Typ mit anderer Kopfzeile braucht Code). */
  static final String KOPF = "Aktion=";

  static final String ZEILENENDE = "\n";

  private Parametertext() {}

  /**
   * @param aktion eine der Aktionen der Definition — vorher von {@link AuftragsPruefung} geprueft
   */
  public static String baue(
      TypDefinition definition, String aktion, List<Map<String, String>> eintraege) {
    String vorlage = definition.zeilen().get(aktion);
    if (vorlage == null) {
      throw new IllegalArgumentException(
          "Keine Zeilenvorlage fuer die Aktion " + aktion + " — ungepruefter Auftrag?");
    }
    List<String> zeilen = new ArrayList<>();
    zeilen.add(KOPF + aktion);
    for (Map<String, String> eintrag : eintraege) {
      zeilen.add(zeile(definition, vorlage, eintrag));
    }
    return String.join(ZEILENENDE, zeilen);
  }

  private static String zeile(TypDefinition definition, String vorlage, Map<String, String> werte) {
    Matcher treffer = TypDefinition.PLATZHALTER.matcher(vorlage);
    StringBuilder zeile = new StringBuilder();
    while (treffer.find()) {
      String name = treffer.group(1);
      String wert =
          TypDefinition.SCHLUESSEL.equals(name)
              ? definition.schluesselText(werte)
              : werte.get(name);
      treffer.appendReplacement(zeile, Matcher.quoteReplacement(wert == null ? "" : wert));
    }
    treffer.appendTail(zeile);
    return zeile.toString();
  }
}
