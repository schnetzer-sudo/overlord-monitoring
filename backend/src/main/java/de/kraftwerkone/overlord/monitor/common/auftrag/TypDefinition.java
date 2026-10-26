package de.kraftwerkone.overlord.monitor.common.auftrag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Eine gelesene Typdefinition: Name, Beschreibung und die Struktur aus {@code task_type}.
 *
 * <p><b>Unveraenderlich, aber nicht zwingend gueltig.</b> {@link DefinitionsPruefung} liest auch
 * eine fehlerhafte Struktur so weit wie moeglich und nennt die Gruende daneben. Angeboten wird nur,
 * was ohne Grund gelesen wurde; die Kopie eines Auftrags dagegen wird immer gelesen, auch wenn eine
 * spaetere Fassung dieses Codes strenger pruefte (E‑1023).
 *
 * @param schluessel {@code null}, wenn der Typ keinen Schluessel hat
 * @param zeilen Aktion auf Zeilenvorlage
 * @param rueckgaben {@code null}, wenn die Struktur keine lesbaren Rueckgabewerte traegt
 */
public record TypDefinition(
    String typ,
    String name,
    String beschreibung,
    List<String> aktionen,
    int maxEintraege,
    List<Feld> felder,
    Schluessel schluessel,
    List<Regel> regeln,
    Map<String, String> zeilen,
    Rueckgaben rueckgaben) {

  /** Der Name, unter dem der Schluessel in Vorlagen und Regeln steht. */
  public static final String SCHLUESSEL = "schluessel";

  /** Ein Platzhalter {@code {name}} in einer Zeilenvorlage. */
  public static final Pattern PLATZHALTER = Pattern.compile("\\{([^{}]*)\\}");

  public TypDefinition {
    aktionen = List.copyOf(aktionen);
    felder = List.copyOf(felder);
    regeln = List.copyOf(regeln);
    zeilen = Map.copyOf(zeilen);
  }

  /**
   * Ein Feld eines Eintrags.
   *
   * @param musterAusdruck das uebersetzte {@code muster}, {@code null} ohne Muster
   * @param nurBei {@code null} heisst: bei allen Aktionen
   */
  public record Feld(
      String name,
      String label,
      boolean pflicht,
      String muster,
      Pattern musterAusdruck,
      List<String> nurBei,
      String beispiel,
      String hinweis) {

    public Feld {
      nurBei = nurBei == null ? null : List.copyOf(nurBei);
    }

    /** Ob das Feld bei dieser Aktion vorkommt. */
    public boolean kommtVorBei(String aktion) {
      return nurBei == null || nurBei.contains(aktion);
    }
  }

  /** Die Felder des Schluessels in Reihenfolge und ihr Trenner (genau ein Zeichen). */
  public record Schluessel(List<String> felder, String trenner) {

    public Schluessel {
      felder = List.copyOf(felder);
    }
  }

  /** Ein Regelbaustein aus dem festen Satz (E‑1021). */
  public sealed interface Regel permits MindestensEins, Eindeutig {}

  /**
   * In jedem Eintrag hat mindestens eines der Felder einen Wert.
   *
   * @param bei {@code null} heisst: bei allen Aktionen
   */
  public record MindestensEins(List<String> felder, List<String> bei) implements Regel {

    public MindestensEins {
      felder = List.copyOf(felder);
      bei = bei == null ? null : List.copyOf(bei);
    }
  }

  /** Keine zwei Eintraege tragen dieselbe Kombination dieser Werte; {@link #SCHLUESSEL} erlaubt. */
  public record Eindeutig(List<String> felder) implements Regel {

    public Eindeutig {
      felder = List.copyOf(felder);
    }
  }

  /**
   * Die Rueckgabewerte der Plattform.
   *
   * @param ok der Wert, der Erfolg heisst; {@code null}, wenn die Struktur keinen lesbaren traegt
   */
  public record Rueckgaben(String ok, List<String> weitere) {

    public Rueckgaben {
      weitere = List.copyOf(weitere);
    }
  }

  public Optional<Feld> feld(String name) {
    return felder.stream().filter(feld -> feld.name().equals(name)).findFirst();
  }

  /**
   * Der Schluessel eines Eintrags: die Teile mit Wert, verbunden mit dem Trenner. Fehlt ein
   * optionaler Teil, entfaellt er mit seinem Trenner, an jeder Position. {@code null} ohne
   * Schluessel.
   */
  public String schluesselText(Map<String, String> werte) {
    if (schluessel == null) {
      return null;
    }
    List<String> teile = new ArrayList<>();
    for (String feld : schluessel.felder()) {
      String wert = werte.get(feld);
      if (hatWert(wert)) {
        teile.add(wert);
      }
    }
    return String.join(schluessel.trenner(), teile);
  }

  /**
   * Die Zeichen, die in keinem Wert stehen duerfen, weil sie im Parametertext woertlich als Trenner
   * dienen (E‑1022): alles, was nach Entfernen der Platzhalter in irgendeiner Zeilenvorlage steht,
   * und der Schluesseltrenner. Umbrueche und Steuerzeichen kommen in {@link
   * #istUmbruchOderSteuerzeichen} hinzu.
   */
  public Set<Integer> woertlicheZeichen() {
    Set<Integer> zeichen = new TreeSet<>();
    for (String vorlage : zeilen.values()) {
      Matcher treffer = PLATZHALTER.matcher(vorlage);
      treffer.replaceAll("").codePoints().forEach(zeichen::add);
    }
    if (schluessel != null) {
      schluessel.trenner().codePoints().forEach(zeichen::add);
    }
    return zeichen;
  }

  /**
   * Zeilenumbrueche und Steuerzeichen (E‑1022): Unicode-Kategorie Cc ({@code U+0000} bis {@code
   * U+001F}, {@code U+007F} bis {@code U+009F}, darunter LF, CR, Tabulator und {@code U+0085}) und
   * die beiden Unicode-Trenner {@code U+2028} und {@code U+2029}.
   */
  public static boolean istUmbruchOderSteuerzeichen(int zeichen) {
    return Character.getType(zeichen) == Character.CONTROL
        || zeichen == 0x2028
        || zeichen == 0x2029;
  }

  /** {@code null} und leerer Text heissen gleichermassen „fehlend“. Sonst zaehlt jeder Wert. */
  public static boolean hatWert(String wert) {
    return wert != null && !wert.isEmpty();
  }
}
