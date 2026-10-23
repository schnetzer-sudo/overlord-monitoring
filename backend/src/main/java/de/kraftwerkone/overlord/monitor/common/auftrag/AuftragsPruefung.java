package de.kraftwerkone.overlord.monitor.common.auftrag;

import static de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.hatWert;

import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Eindeutig;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Feld;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.MindestensEins;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Regel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prueft einen Auftrag gegen die Definition seines Typs — mit den Regelbausteinen aus dem festen
 * Satz (E‑1021) und dem Zeichenschutz (E‑1022). Beschrieben in {@code docs/tasks.md} §5.3.
 *
 * <p><b>Alle Fehler gesammelt</b>, in der Reihenfolge: Aktion, Zahl der Eintraege, je Eintrag die
 * Felder, dann die Regeln. Der Assistent soll in einer Rueckfrage alles nennen koennen, was fehlt.
 *
 * <p><b>Gekuerzt werden nur Schluesselteile</b>, um {@code U+0020} am Rand, vor jeder Pruefung
 * (E‑1137, E‑1143, E‑1144). Das ist die eine Stelle: Anlegen, Pruefen und der Vorschlag des
 * Assistenten nehmen die Eintraege aus dem {@link Ergebnis}. Sonst wird kein Wert veraendert;
 * {@code null} und leerer Text heissen „fehlend“, ein Leerzeichen in einem anderen Feld ist ein
 * Wert und faellt ueber das Muster.
 *
 * <p>Hier steht kein Typ. Was ein Typ verlangt, steht in seiner {@link TypDefinition}.
 */
public final class AuftragsPruefung {

  private AuftragsPruefung() {}

  /**
   * Das Ergebnis der Pruefung.
   *
   * @param eintraege die Eintraege, wie sie gelten: Schluesselteile am Rand gekuerzt, sonst
   *     unveraendert, ein {@code null}-Eintrag als leerer. Angelegt und vorgeschlagen wird nur
   *     diese Fassung.
   * @param fehler leer, wenn der Auftrag angelegt werden darf
   */
  public record Ergebnis(List<Map<String, String>> eintraege, List<Prueffehler> fehler) {

    public Ergebnis {
      eintraege = List.copyOf(eintraege);
      fehler = List.copyOf(fehler);
    }
  }

  /**
   * @param eintraege je Eintrag Feldname auf Wert; {@code null} wie eine leere Liste, ein {@code
   *     null}-Eintrag wie ein leerer
   */
  public static Ergebnis pruefe(
      TypDefinition definition, String aktion, List<Map<String, String>> eintraege) {
    List<Prueffehler> fehler = new ArrayList<>();
    List<Map<String, String>> liste = gekuerzt(definition, eintraege);

    // 1. Die Aktion. Ist sie unbekannt, entfallen unten die Pruefungen, die an ihr haengen.
    boolean aktionBekannt = aktion != null && definition.aktionen().contains(aktion);
    if (!aktionBekannt) {
      String moeglich = String.join(", ", definition.aktionen());
      fehler.add(
          new Prueffehler(
              null,
              null,
              Fehlercode.AKTION_UNBEKANNT,
              hatWert(aktion)
                  ? "Die Aktion „"
                      + aktion
                      + "“ gibt es für diesen Auftrag nicht. Möglich sind: "
                      + moeglich
                      + "."
                  : "Es ist keine Aktion angegeben. Möglich sind: " + moeglich + "."));
    }

    // 2. Die Zahl der Eintraege.
    if (liste.isEmpty()) {
      fehler.add(
          new Prueffehler(
              null, null, Fehlercode.ZU_WENIGE_EINTRAEGE, "Der Auftrag enthält keinen Eintrag."));
    } else if (liste.size() > definition.maxEintraege()) {
      fehler.add(
          new Prueffehler(
              null,
              null,
              Fehlercode.ZU_VIELE_EINTRAEGE,
              "Der Auftrag enthält "
                  + liste.size()
                  + " Einträge; möglich sind höchstens "
                  + definition.maxEintraege()
                  + "."));
    }

    // 3. Je Eintrag die Felder.
    Set<Integer> woertlich = definition.woertlicheZeichen();
    for (int index = 0; index < liste.size(); index++) {
      Map<String, String> eintrag = werte(liste.get(index));
      for (String name : eintrag.keySet()) {
        if (definition.feld(name).isEmpty()) {
          fehler.add(
              new Prueffehler(
                  index,
                  name,
                  Fehlercode.FELD_UNBEKANNT,
                  eintragsname(index)
                      + ": Das Feld „"
                      + name
                      + "“ gibt es in diesem Auftrag nicht."));
        }
      }
      for (Feld feld : definition.felder()) {
        pruefeFeld(index, feld, eintrag.get(feld.name()), aktion, aktionBekannt, woertlich, fehler);
      }
    }

    // 4. Die Regeln der Definition.
    for (Regel regel : definition.regeln()) {
      switch (regel) {
        case MindestensEins mindestens ->
            pruefeMindestensEins(definition, mindestens, liste, aktion, aktionBekannt, fehler);
        case Eindeutig eindeutig -> pruefeEindeutig(definition, eindeutig, liste, fehler);
      }
    }
    return new Ergebnis(liste, fehler);
  }

  /**
   * Die Eintraege mit gekuerzten Schluesselteilen: <b>nur {@code U+0020}</b> am Anfang und am Ende,
   * nur an den Feldern des Schluessels. {@link String#strip()} und {@link String#trim()} naehmen
   * auch Tabulator oder {@code U+00A0} weg und liessen so einen Wert durch, den die Pruefung sonst
   * ablehnt. Innen bleibt jedes Zeichen, auch mehrere Leerzeichen hintereinander (E‑1138). Ein
   * Teil, der danach leer ist, fehlt (E‑1139).
   */
  private static List<Map<String, String>> gekuerzt(
      TypDefinition definition, List<Map<String, String>> eintraege) {
    if (eintraege == null) {
      return List.of();
    }
    List<String> teile =
        definition.schluessel() == null ? List.of() : definition.schluessel().felder();
    List<Map<String, String>> liste = new ArrayList<>();
    for (Map<String, String> eintrag : eintraege) {
      Map<String, String> werte = new LinkedHashMap<>(werte(eintrag));
      for (String teil : teile) {
        werte.computeIfPresent(teil, (name, wert) -> ohneRandleerzeichen(wert));
      }
      liste.add(Collections.unmodifiableMap(werte));
    }
    return liste;
  }

  private static String ohneRandleerzeichen(String wert) {
    int anfang = 0;
    int ende = wert.length();
    while (anfang < ende && wert.charAt(anfang) == ' ') {
      anfang++;
    }
    while (ende > anfang && wert.charAt(ende - 1) == ' ') {
      ende--;
    }
    return wert.substring(anfang, ende);
  }

  private static void pruefeFeld(
      int index,
      Feld feld,
      String wert,
      String aktion,
      boolean aktionBekannt,
      Set<Integer> woertlich,
      List<Prueffehler> fehler) {
    String ort = eintragsname(index) + ", " + feld.label();
    if (aktionBekannt && !feld.kommtVorBei(aktion)) {
      if (hatWert(wert)) {
        fehler.add(
            new Prueffehler(
                index,
                feld.name(),
                Fehlercode.FELD_NICHT_ERLAUBT,
                ort + ": Bei „" + aktion + "“ wird hier kein Wert angegeben."));
      }
      return;
    }
    if (!hatWert(wert)) {
      if (aktionBekannt && feld.pflicht()) {
        fehler.add(
            new Prueffehler(
                index, feld.name(), Fehlercode.PFLICHT_FEHLT, ort + ": Der Wert fehlt."));
      }
      return;
    }
    if (feld.musterAusdruck() != null && !feld.musterAusdruck().matcher(wert).matches()) {
      fehler.add(
          new Prueffehler(
              index,
              feld.name(),
              Fehlercode.FORMAT,
              ort
                  + ": Der Wert „"
                  + wert
                  + "“ hat nicht die erwartete Form"
                  + (feld.beispiel() == null ? "" : ", zum Beispiel „" + feld.beispiel() + "“")
                  + "."));
    }
    wert.codePoints()
        .filter(
            zeichen ->
                TypDefinition.istUmbruchOderSteuerzeichen(zeichen) || woertlich.contains(zeichen))
        .findFirst()
        .ifPresent(
            zeichen ->
                fehler.add(
                    new Prueffehler(
                        index,
                        feld.name(),
                        Fehlercode.UNZULAESSIGES_ZEICHEN,
                        ort
                            + ": Der Wert enthält ein unzulässiges Zeichen ("
                            + beschreibe(zeichen)
                            + ").")));
  }

  private static void pruefeMindestensEins(
      TypDefinition definition,
      MindestensEins regel,
      List<Map<String, String>> liste,
      String aktion,
      boolean aktionBekannt,
      List<Prueffehler> fehler) {
    if (regel.bei() != null && !(aktionBekannt && regel.bei().contains(aktion))) {
      return;
    }
    String namen =
        regel.felder().stream()
            .map(name -> label(definition, name))
            .collect(Collectors.joining(" oder "));
    for (int index = 0; index < liste.size(); index++) {
      Map<String, String> eintrag = werte(liste.get(index));
      if (regel.felder().stream().noneMatch(name -> hatWert(eintrag.get(name)))) {
        fehler.add(
            new Prueffehler(
                index,
                null,
                Fehlercode.MINDESTENS_EINS,
                eintragsname(index)
                    + ": Mindestens eines der Felder "
                    + namen
                    + " braucht einen Wert."));
      }
    }
  }

  private static void pruefeEindeutig(
      TypDefinition definition,
      Eindeutig regel,
      List<Map<String, String>> liste,
      List<Prueffehler> fehler) {
    boolean nurSchluessel =
        regel.felder().size() == 1 && TypDefinition.SCHLUESSEL.equals(regel.felder().getFirst());
    Map<List<String>, Integer> gesehen = new HashMap<>();
    for (int index = 0; index < liste.size(); index++) {
      Map<String, String> eintrag = werte(liste.get(index));
      List<String> kombination = new ArrayList<>();
      for (String name : regel.felder()) {
        String wert =
            TypDefinition.SCHLUESSEL.equals(name)
                ? definition.schluesselText(eintrag)
                : eintrag.get(name);
        kombination.add(wert == null ? "" : wert);
      }
      if (kombination.stream().allMatch(String::isEmpty)) {
        continue;
      }
      Integer frueher = gesehen.putIfAbsent(kombination, index);
      if (frueher == null) {
        continue;
      }
      String meldung =
          nurSchluessel
              ? eintragsname(index)
                  + " hat denselben Schlüssel wie Eintrag "
                  + (frueher + 1)
                  + " („"
                  + kombination.getFirst()
                  + "“)."
              : eintragsname(index)
                  + " hat dieselben Werte in "
                  + regel.felder().stream()
                      .map(name -> label(definition, name))
                      .collect(Collectors.joining(", "))
                  + " wie Eintrag "
                  + (frueher + 1)
                  + ".";
      String feld = regel.felder().size() == 1 && !nurSchluessel ? regel.felder().getFirst() : null;
      fehler.add(new Prueffehler(index, feld, Fehlercode.DOPPELT, meldung));
    }
  }

  private static Map<String, String> werte(Map<String, String> eintrag) {
    return eintrag == null ? Map.of() : eintrag;
  }

  /** Fuer den Menschen zaehlt der Eintrag ab 1. */
  private static String eintragsname(int index) {
    return "Eintrag " + (index + 1);
  }

  private static String label(TypDefinition definition, String name) {
    if (TypDefinition.SCHLUESSEL.equals(name)) {
      return "Schlüssel";
    }
    return definition.feld(name).map(Feld::label).orElse(name);
  }

  private static String beschreibe(int zeichen) {
    if (zeichen == '\n'
        || zeichen == '\r'
        || zeichen == 0x85
        || zeichen == 0x2028
        || zeichen == 0x2029) {
      return "Zeilenumbruch";
    }
    if (Character.getType(zeichen) == Character.CONTROL) {
      return String.format("Steuerzeichen U+%04X", zeichen);
    }
    return "„" + new String(Character.toChars(zeichen)) + "“";
  }
}
