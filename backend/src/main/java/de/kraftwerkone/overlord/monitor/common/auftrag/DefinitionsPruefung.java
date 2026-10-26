package de.kraftwerkone.overlord.monitor.common.auftrag;

import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Eindeutig;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Feld;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.MindestensEins;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Regel;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Rueckgaben;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Schluessel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import tools.jackson.databind.JsonNode;

/**
 * Liest die Struktur eines Auftragstyps und nennt jeden Grund, aus dem sie nicht angeboten werden
 * darf (E‑1019). Beschrieben in {@code docs/tasks.md} §3.2 und §3.6.
 *
 * <p><b>Alle Gruende, nicht nur der erste.</b> Wer eine Definition per SQL anlegt, sieht sie im Log
 * und soll sie in einem Durchgang berichtigen koennen.
 *
 * <p><b>Gelesen wird trotzdem so weit wie moeglich.</b> Die Kopie eines Auftrags (E‑1023) wird
 * ueber denselben Weg gelesen, und sie muss lesbar bleiben, auch wenn dieser Code eines Tages
 * strenger prueft als zu ihrer Zeit. Was nicht lesbar ist, faellt weg oder bekommt einen leeren
 * Wert — nie eine Ausnahme.
 *
 * <p><b>Unbekannte Schluessel machen die Definition ungueltig</b>, auf jeder Ebene. Ein Tippfehler
 * ({@code pflichT}, {@code muser}) faellt damit auf, statt still wirkungslos zu bleiben.
 */
public final class DefinitionsPruefung {

  static final int MAX_AKTIONEN = 10;
  static final int MAX_AKTIONSLAENGE = 64;
  static final int MAX_EINTRAEGE_OBERGRENZE = 500;

  private static final Pattern FELDNAME = Pattern.compile("[a-z][a-z0-9]*");

  private static final Set<String> STRUKTUR_SCHLUESSEL =
      Set.of("aktionen", "maxEintraege", "felder", "schluessel", "regeln", "zeile", "rueckgaben");
  private static final List<String> STRUKTUR_PFLICHT =
      List.of("aktionen", "maxEintraege", "felder", "zeile", "rueckgaben");
  private static final Set<String> FELD_SCHLUESSEL =
      Set.of("name", "label", "pflicht", "muster", "nurBei", "beispiel", "hinweis");
  private static final Set<String> SCHLUESSEL_SCHLUESSEL = Set.of("felder", "trenner");
  private static final Set<String> RUECKGABEN_SCHLUESSEL = Set.of("ok", "weitere");
  private static final String MINDESTENS_EINS = "mindestensEins";
  private static final String EINDEUTIG = "eindeutig";
  private static final Set<String> MINDESTENS_EINS_SCHLUESSEL = Set.of("art", "felder", "bei");
  private static final Set<String> EINDEUTIG_SCHLUESSEL = Set.of("art", "felder");

  private DefinitionsPruefung() {}

  /**
   * Das Ergebnis: die gelesene Definition und die Gruende, aus denen sie nicht angeboten wird.
   * Gueltig heisst: kein Grund.
   */
  public record Ergebnis(TypDefinition definition, List<String> gruende) {

    public Ergebnis {
      gruende = List.copyOf(gruende);
    }

    public boolean gueltig() {
      return gruende.isEmpty();
    }
  }

  /**
   * @param struktur die Struktur als JSON-Baum; {@code null} oder kein Objekt ergibt eine leere,
   *     ungueltige Definition
   */
  public static Ergebnis pruefe(String typ, String name, String beschreibung, JsonNode struktur) {
    List<String> gruende = new ArrayList<>();
    if (name == null || name.isBlank()) {
      gruende.add("Der Name ist leer.");
    }
    if (struktur == null || !struktur.isObject()) {
      gruende.add("Die Struktur ist kein JSON-Objekt.");
      return new Ergebnis(
          new TypDefinition(
              typ, name, beschreibung, List.of(), 0, List.of(), null, List.of(), Map.of(), null),
          gruende);
    }

    unbekannteSchluessel(struktur, STRUKTUR_SCHLUESSEL, "der Struktur", gruende);
    for (String pflicht : STRUKTUR_PFLICHT) {
      if (!struktur.has(pflicht)) {
        gruende.add("Der Pflichtschlüssel „" + pflicht + "“ fehlt.");
      }
    }

    List<String> aktionen = aktionen(struktur.get("aktionen"), gruende);
    int maxEintraege = maxEintraege(struktur.get("maxEintraege"), gruende);
    List<Feld> felder = felder(struktur.get("felder"), aktionen, gruende);
    Schluessel schluessel = schluessel(struktur.get("schluessel"), felder, gruende);
    List<Regel> regeln = regeln(struktur.get("regeln"), felder, aktionen, schluessel, gruende);
    Map<String, String> zeilen =
        zeilen(struktur.get("zeile"), felder, aktionen, schluessel, gruende);
    Rueckgaben rueckgaben = rueckgaben(struktur.get("rueckgaben"), gruende);

    return new Ergebnis(
        new TypDefinition(
            typ,
            name,
            beschreibung,
            aktionen,
            maxEintraege,
            felder,
            schluessel,
            regeln,
            zeilen,
            rueckgaben),
        gruende);
  }

  // ─── aktionen und maxEintraege ─────────────────────────────────────────────────────────────

  private static List<String> aktionen(JsonNode knoten, List<String> gruende) {
    List<String> aktionen = new ArrayList<>();
    if (knoten == null) {
      return aktionen;
    }
    if (!knoten.isArray()) {
      gruende.add("„aktionen“ ist keine Liste.");
      return aktionen;
    }
    for (JsonNode eintrag : knoten) {
      String aktion = text(eintrag);
      if (aktion == null) {
        gruende.add("„aktionen“ enthält etwas anderes als Text.");
        continue;
      }
      int laenge = aktion.codePointCount(0, aktion.length());
      if (laenge == 0 || laenge > MAX_AKTIONSLAENGE) {
        gruende.add(
            "Die Aktion „" + aktion + "“ hat nicht 1 bis " + MAX_AKTIONSLAENGE + " Zeichen.");
      }
      if (enthaeltUmbruchOderSteuerzeichen(aktion)) {
        gruende.add(
            "Die Aktion „" + aktion + "“ enthält einen Zeilenumbruch oder ein Steuerzeichen.");
      }
      if (aktionen.contains(aktion)) {
        gruende.add("Die Aktion „" + aktion + "“ steht doppelt in „aktionen“.");
        continue;
      }
      aktionen.add(aktion);
    }
    if (aktionen.isEmpty() || aktionen.size() > MAX_AKTIONEN) {
      gruende.add("„aktionen“ muss 1 bis " + MAX_AKTIONEN + " Aktionen enthalten.");
    }
    return aktionen;
  }

  private static int maxEintraege(JsonNode knoten, List<String> gruende) {
    if (knoten == null) {
      return 0;
    }
    if (!knoten.isIntegralNumber()
        || !knoten.canConvertToInt()
        || knoten.intValue() < 1
        || knoten.intValue() > MAX_EINTRAEGE_OBERGRENZE) {
      gruende.add(
          "„maxEintraege“ ist keine ganze Zahl von 1 bis " + MAX_EINTRAEGE_OBERGRENZE + ".");
      return knoten.isIntegralNumber() && knoten.canConvertToInt() ? knoten.intValue() : 0;
    }
    return knoten.intValue();
  }

  // ─── felder ────────────────────────────────────────────────────────────────────────────────

  private static List<Feld> felder(JsonNode knoten, List<String> aktionen, List<String> gruende) {
    List<Feld> felder = new ArrayList<>();
    if (knoten == null) {
      return felder;
    }
    if (!knoten.isArray() || knoten.isEmpty()) {
      gruende.add("„felder“ ist keine Liste mit mindestens einem Feld.");
      return felder;
    }
    int position = 0;
    for (JsonNode eintrag : knoten) {
      String ort = "felder[" + position++ + "]";
      if (!eintrag.isObject()) {
        gruende.add(ort + " ist kein Objekt.");
        continue;
      }
      unbekannteSchluessel(eintrag, FELD_SCHLUESSEL, ort, gruende);

      String name = text(eintrag.get("name"));
      if (name == null || !FELDNAME.matcher(name).matches()) {
        gruende.add(ort + ": „name“ fehlt oder folgt nicht [a-z][a-z0-9]*.");
        continue;
      }
      ort = "Feld „" + name + "“";
      if (TypDefinition.SCHLUESSEL.equals(name)) {
        gruende.add(ort + ": Der Name ist für den Schlüssel reserviert.");
      }
      if (felder.stream().anyMatch(feld -> feld.name().equals(name))) {
        gruende.add(ort + " steht doppelt in „felder“.");
        continue;
      }

      String label = text(eintrag.get("label"));
      if (label == null || label.isBlank()) {
        gruende.add(ort + ": „label“ fehlt oder ist leer.");
      }

      boolean pflicht = false;
      JsonNode pflichtKnoten = eintrag.get("pflicht");
      if (pflichtKnoten != null) {
        if (pflichtKnoten.isBoolean()) {
          pflicht = pflichtKnoten.booleanValue();
        } else {
          gruende.add(ort + ": „pflicht“ ist kein Wahrheitswert.");
        }
      }

      String muster = null;
      Pattern ausdruck = null;
      JsonNode musterKnoten = eintrag.get("muster");
      if (musterKnoten != null) {
        muster = text(musterKnoten);
        if (muster == null) {
          gruende.add(ort + ": „muster“ ist kein Text.");
        } else {
          try {
            ausdruck = Pattern.compile(muster);
          } catch (PatternSyntaxException fehler) {
            gruende.add(
                ort + ": „muster“ ist nicht übersetzbar (" + fehler.getDescription() + ").");
          }
        }
      }

      List<String> nurBei = null;
      JsonNode nurBeiKnoten = eintrag.get("nurBei");
      if (nurBeiKnoten != null) {
        nurBei = aktionsliste(nurBeiKnoten, aktionen, ort + ", „nurBei“", gruende);
      }

      felder.add(
          new Feld(
              name,
              label,
              pflicht,
              muster,
              ausdruck,
              nurBei,
              optionalerText(eintrag.get("beispiel"), ort + ", „beispiel“", gruende),
              optionalerText(eintrag.get("hinweis"), ort + ", „hinweis“", gruende)));
    }
    return felder;
  }

  // ─── schluessel ────────────────────────────────────────────────────────────────────────────

  private static Schluessel schluessel(JsonNode knoten, List<Feld> felder, List<String> gruende) {
    if (knoten == null) {
      return null;
    }
    if (!knoten.isObject()) {
      gruende.add("„schluessel“ ist kein Objekt.");
      return null;
    }
    unbekannteSchluessel(knoten, SCHLUESSEL_SCHLUESSEL, "„schluessel“", gruende);

    List<String> teile = new ArrayList<>();
    JsonNode teileKnoten = knoten.get("felder");
    if (teileKnoten == null || !teileKnoten.isArray() || teileKnoten.isEmpty()) {
      gruende.add("„schluessel.felder“ ist keine Liste mit mindestens einem Feld.");
    } else {
      for (JsonNode teil : teileKnoten) {
        String name = text(teil);
        if (name == null) {
          gruende.add("„schluessel.felder“ enthält etwas anderes als Text.");
          continue;
        }
        if (teile.contains(name)) {
          gruende.add("Das Feld „" + name + "“ steht doppelt im Schlüssel.");
          continue;
        }
        teile.add(name);
      }
    }

    // Jeder Teil darf optional sein, an jeder Position (E‑1164); einer muss Pflicht sein, damit
    // jeder Eintrag einen Schluessel hat (E‑1165).
    boolean einerPflicht = false;
    for (String name : teile) {
      Feld feld = felder.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
      if (feld == null) {
        gruende.add("Der Schlüssel nennt das unbekannte Feld „" + name + "“.");
        continue;
      }
      if (feld.nurBei() != null) {
        gruende.add("Das Schlüsselfeld „" + name + "“ darf kein „nurBei“ tragen.");
      }
      if (feld.pflicht()) {
        einerPflicht = true;
      }
    }
    if (!teile.isEmpty() && !einerPflicht) {
      gruende.add("Mindestens ein Teil des Schlüssels muss Pflicht sein.");
    }

    String trenner = text(knoten.get("trenner"));
    if (trenner == null
        || trenner.codePointCount(0, trenner.length()) != 1
        || enthaeltUmbruchOderSteuerzeichen(trenner)) {
      gruende.add(
          "„schluessel.trenner“ ist nicht genau ein Zeichen (ohne Umbruch und Steuerzeichen).");
      if (trenner == null) {
        trenner = "";
      }
    }
    return new Schluessel(teile, trenner);
  }

  // ─── regeln ────────────────────────────────────────────────────────────────────────────────

  private static List<Regel> regeln(
      JsonNode knoten,
      List<Feld> felder,
      List<String> aktionen,
      Schluessel schluessel,
      List<String> gruende) {
    List<Regel> regeln = new ArrayList<>();
    if (knoten == null) {
      return regeln;
    }
    if (!knoten.isArray()) {
      gruende.add("„regeln“ ist keine Liste.");
      return regeln;
    }
    int position = 0;
    for (JsonNode eintrag : knoten) {
      String ort = "regeln[" + position++ + "]";
      if (!eintrag.isObject()) {
        gruende.add(ort + " ist kein Objekt.");
        continue;
      }
      String art = text(eintrag.get("art"));
      if (MINDESTENS_EINS.equals(art)) {
        unbekannteSchluessel(eintrag, MINDESTENS_EINS_SCHLUESSEL, ort, gruende);
        List<String> regelfelder = regelfelder(eintrag.get("felder"), felder, false, ort, gruende);
        JsonNode beiKnoten = eintrag.get("bei");
        List<String> bei =
            beiKnoten == null ? null : aktionsliste(beiKnoten, aktionen, ort + ", „bei“", gruende);
        regeln.add(new MindestensEins(regelfelder, bei));
      } else if (EINDEUTIG.equals(art)) {
        unbekannteSchluessel(eintrag, EINDEUTIG_SCHLUESSEL, ort, gruende);
        regeln.add(
            new Eindeutig(
                regelfelder(eintrag.get("felder"), felder, schluessel != null, ort, gruende)));
      } else {
        gruende.add(
            ort
                + ": „art“ ist nicht „"
                + MINDESTENS_EINS
                + "“ oder „"
                + EINDEUTIG
                + "“ — andere Regelbausteine gibt es nicht.");
      }
    }
    return regeln;
  }

  private static List<String> regelfelder(
      JsonNode knoten,
      List<Feld> felder,
      boolean schluesselErlaubt,
      String ort,
      List<String> gruende) {
    List<String> namen = new ArrayList<>();
    if (knoten == null || !knoten.isArray() || knoten.isEmpty()) {
      gruende.add(ort + ": „felder“ ist keine Liste mit mindestens einem Feld.");
      return namen;
    }
    for (JsonNode eintrag : knoten) {
      String name = text(eintrag);
      boolean bekannt =
          name != null
              && (felder.stream().anyMatch(feld -> feld.name().equals(name))
                  || (schluesselErlaubt && TypDefinition.SCHLUESSEL.equals(name)));
      if (!bekannt) {
        gruende.add(ort + ": „" + name + "“ ist kein bekanntes Feld.");
        continue;
      }
      namen.add(name);
    }
    return namen;
  }

  // ─── zeile ─────────────────────────────────────────────────────────────────────────────────

  private static Map<String, String> zeilen(
      JsonNode knoten,
      List<Feld> felder,
      List<String> aktionen,
      Schluessel schluessel,
      List<String> gruende) {
    Map<String, String> zeilen = new LinkedHashMap<>();
    if (knoten == null) {
      return zeilen;
    }
    if (!knoten.isObject()) {
      gruende.add("„zeile“ ist kein Objekt.");
      return zeilen;
    }
    for (String aktion : knoten.propertyNames()) {
      if (!aktionen.contains(aktion)) {
        gruende.add("„zeile“ hat eine Vorlage für die unbekannte Aktion „" + aktion + "“.");
        continue;
      }
      String vorlage = text(knoten.get(aktion));
      if (vorlage == null) {
        gruende.add("Die Zeilenvorlage für „" + aktion + "“ ist kein Text.");
        continue;
      }
      pruefeVorlage(aktion, vorlage, felder, schluessel != null, gruende);
      zeilen.put(aktion, vorlage);
    }
    for (String aktion : aktionen) {
      if (!knoten.has(aktion)) {
        gruende.add("Für die Aktion „" + aktion + "“ fehlt die Zeilenvorlage.");
      }
    }
    return zeilen;
  }

  private static void pruefeVorlage(
      String aktion,
      String vorlage,
      List<Feld> felder,
      boolean mitSchluessel,
      List<String> gruende) {
    String ort = "Zeilenvorlage für „" + aktion + "“";
    Matcher treffer = TypDefinition.PLATZHALTER.matcher(vorlage);
    while (treffer.find()) {
      String name = treffer.group(1);
      boolean bekannt =
          felder.stream().anyMatch(feld -> feld.name().equals(name))
              || (mitSchluessel && TypDefinition.SCHLUESSEL.equals(name));
      if (!bekannt) {
        gruende.add(ort + ": Der Platzhalter {" + name + "} nennt kein Feld.");
      }
    }
    String woertlich = TypDefinition.PLATZHALTER.matcher(vorlage).replaceAll("");
    if (woertlich.indexOf('{') >= 0 || woertlich.indexOf('}') >= 0) {
      gruende.add(ort + ": Eine geschweifte Klammer steht außerhalb eines Platzhalters.");
    }
    if (enthaeltUmbruchOderSteuerzeichen(woertlich)) {
      gruende.add(ort + ": Sie enthält einen Zeilenumbruch oder ein Steuerzeichen.");
    }
  }

  // ─── rueckgaben ────────────────────────────────────────────────────────────────────────────

  private static Rueckgaben rueckgaben(JsonNode knoten, List<String> gruende) {
    if (knoten == null) {
      return null;
    }
    if (!knoten.isObject()) {
      gruende.add("„rueckgaben“ ist kein Objekt.");
      return null;
    }
    unbekannteSchluessel(knoten, RUECKGABEN_SCHLUESSEL, "„rueckgaben“", gruende);

    String ok = text(knoten.get("ok"));
    if (!istRueckgabewert(ok)) {
      gruende.add(
          "„rueckgaben.ok“ fehlt, ist leer, hat Leerraum am Rand oder einen Zeilenumbruch.");
    }

    List<String> weitere = new ArrayList<>();
    JsonNode weitereKnoten = knoten.get("weitere");
    if (weitereKnoten != null) {
      if (!weitereKnoten.isArray()) {
        gruende.add("„rueckgaben.weitere“ ist keine Liste.");
      } else {
        for (JsonNode eintrag : weitereKnoten) {
          String wert = text(eintrag);
          if (!istRueckgabewert(wert)) {
            gruende.add(
                "„rueckgaben.weitere“ enthält einen Wert, der kein Text ist, leer ist, Leerraum am"
                    + " Rand oder einen Zeilenumbruch hat.");
            continue;
          }
          weitere.add(wert);
        }
      }
    }
    if (ok != null && weitere.contains(ok)) {
      gruende.add("„rueckgaben.ok“ steht auch in „rueckgaben.weitere“.");
    }
    return new Rueckgaben(ok, weitere);
  }

  /** Die Rueckgabe wird zeilenweise und ohne Leerraum am Rand verglichen (docs/tasks.md §5.4). */
  private static boolean istRueckgabewert(String wert) {
    return wert != null
        && !wert.isEmpty()
        && wert.strip().equals(wert)
        && !enthaeltUmbruchOderSteuerzeichen(wert);
  }

  // ─── Hilfen ────────────────────────────────────────────────────────────────────────────────

  private static List<String> aktionsliste(
      JsonNode knoten, List<String> aktionen, String ort, List<String> gruende) {
    List<String> liste = new ArrayList<>();
    if (!knoten.isArray() || knoten.isEmpty()) {
      gruende.add(ort + " ist keine Liste mit mindestens einer Aktion.");
      return liste;
    }
    for (JsonNode eintrag : knoten) {
      String aktion = text(eintrag);
      if (aktion == null || !aktionen.contains(aktion)) {
        gruende.add(ort + ": „" + aktion + "“ ist keine Aktion dieses Typs.");
        continue;
      }
      liste.add(aktion);
    }
    return liste;
  }

  private static void unbekannteSchluessel(
      JsonNode objekt, Set<String> bekannt, String ort, List<String> gruende) {
    Set<String> gemeldet = new HashSet<>();
    for (String name : objekt.propertyNames()) {
      if (!bekannt.contains(name) && gemeldet.add(name)) {
        gruende.add("Unbekannter Schlüssel „" + name + "“ in " + ort + ".");
      }
    }
  }

  private static String optionalerText(JsonNode knoten, String ort, List<String> gruende) {
    if (knoten == null) {
      return null;
    }
    String wert = text(knoten);
    if (wert == null) {
      gruende.add(ort + " ist kein Text.");
    }
    return wert;
  }

  private static String text(JsonNode knoten) {
    return knoten != null && knoten.isString() ? knoten.stringValue() : null;
  }

  private static boolean enthaeltUmbruchOderSteuerzeichen(String text) {
    return text.codePoints().anyMatch(TypDefinition::istUmbruchOderSteuerzeichen);
  }
}
