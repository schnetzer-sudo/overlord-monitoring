package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeug;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition.Feld;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Macht aus einer Typdefinition ein Werkzeug des Modells (E‑1034, {@code docs/assistent.md} §4.5),
 * ohne einen Typ zu kennen.
 *
 * <p><b>Kein Werkzeug hat einen Mandantenparameter.</b> Die Eingabe besteht aus {@code aktion} und
 * {@code eintraege}; jeder Eintrag traegt genau die Felder des Typs. Ein Typ mit einem Feld, dessen
 * Name mit {@value #MANDANT} beginnt, wird gar nicht erst angeboten (E‑1054) — fuer das Modell
 * saehe es wie ein Mandantenparameter aus.
 *
 * <p><b>Kein {@code strict}:</b> Anthropic garantiert schemagueltige Eingaben nur ohne Grenzen an
 * Listen wie {@code maxItems}. Geprueft wird ohnehin im Backend.
 */
public final class Werkzeugbau {

  private static final Logger log = LoggerFactory.getLogger(Werkzeugbau.class);

  /** Was Anthropic als Werkzeugnamen annimmt. */
  static final Pattern WERKZEUGNAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

  static final String MANDANT = "mandant";

  /** Typen, deren Ausschluss schon im Protokoll steht — einmal je Kennung, nicht je Gespraech. */
  private static final Set<String> GEMELDET = ConcurrentHashMap.newKeySet();

  private final JsonMapper json;

  public Werkzeugbau(JsonMapper json) {
    this.json = json;
  }

  /**
   * Ob der Assistent diesen Typ anbieten kann: Die Kennung taugt als Werkzeugname, und kein Feld
   * beginnt mit {@value #MANDANT} (E‑1054). Ein ungeeigneter Typ bleibt ueber {@code POST
   * /api/tasks} anlegbar.
   */
  public static boolean geeignet(TypDefinition typ) {
    boolean geeignet =
        typ.typ() != null
            && WERKZEUGNAME.matcher(typ.typ()).matches()
            && typ.felder().stream()
                .noneMatch(feld -> feld.name().toLowerCase(Locale.ROOT).startsWith(MANDANT));
    if (!geeignet && GEMELDET.add(String.valueOf(typ.typ()))) {
      log.warn(
          "Auftragstyp {} wird vom Assistenten nicht angeboten: Die Kennung taugt nicht als"
              + " Werkzeugname, oder ein Feld beginnt mit \"mandant\" (docs/assistent.md E‑1054)",
          typ.typ());
    }
    return geeignet;
  }

  /** Die geeigneten unter den angebotenen Typen, in ihrer Reihenfolge. */
  public static List<TypDefinition> geeignete(List<TypDefinition> typen) {
    return typen.stream().filter(Werkzeugbau::geeignet).toList();
  }

  /** Je Typ ein Werkzeug, in der Reihenfolge der Typen. */
  public List<Werkzeug> werkzeuge(List<TypDefinition> typen) {
    List<Werkzeug> werkzeuge = new ArrayList<>();
    for (TypDefinition typ : typen) {
      werkzeuge.add(werkzeug(typ));
    }
    return werkzeuge;
  }

  public Werkzeug werkzeug(TypDefinition typ) {
    return new Werkzeug(typ.typ(), beschreibung(typ), schema(typ));
  }

  ObjectNode schema(TypDefinition typ) {
    ObjectNode schema = json.createObjectNode();
    schema.put("type", "object");
    ObjectNode eigenschaften = schema.putObject("properties");

    ObjectNode aktion = eigenschaften.putObject("aktion");
    aktion.put("type", "string");
    ArrayNode aktionen = aktion.putArray("enum");
    typ.aktionen().forEach(aktionen::add);
    aktion.put("description", "Die eine Aktion dieses Auftrags.");

    ObjectNode eintraege = eigenschaften.putObject("eintraege");
    eintraege.put("type", "array");
    eintraege.put(
        "description",
        "Die Einträge des Auftrags, in der Reihenfolge, in der der Kunde sie nennt.");
    eintraege.put("minItems", 1);
    eintraege.put("maxItems", typ.maxEintraege());
    ObjectNode eintrag = eintraege.putObject("items");
    eintrag.put("type", "object");
    ObjectNode felder = eintrag.putObject("properties");
    for (Feld feld : typ.felder()) {
      ObjectNode eigenschaft = felder.putObject(feld.name());
      eigenschaft.put("type", "string");
      eigenschaft.put("description", feldbeschreibung(feld));
    }
    eintrag.put("additionalProperties", false);

    ArrayNode pflicht = schema.putArray("required");
    pflicht.add("aktion");
    pflicht.add("eintraege");
    schema.put("additionalProperties", false);
    return schema;
  }

  static String beschreibung(TypDefinition typ) {
    StringBuilder text =
        new StringBuilder("Reicht einen Vorschlag für den Auftrag „")
            .append(typ.name())
            .append("“ ein. Rufe dieses Werkzeug, sobald die Angaben des Kunden vollständig sind.")
            .append(" Das Backend prüft den Vorschlag: Meldet es Fehler, erkläre sie dem Kunden")
            .append(" und frage nach; ist er gültig, sieht der Kunde ihn als Karte und legt den")
            .append(" Auftrag dort selbst an. Felder ohne Wert lässt du weg. Werte übergibst du")
            .append(" als Text, genau so, wie der Kunde sie nennt.");
    if (typ.schluessel() != null) {
      text.append(" Der Schlüssel eines Eintrags besteht aus ")
          .append(
              typ.schluessel().felder().stream()
                  .map(name -> label(typ, name))
                  .collect(Collectors.joining(", ")))
          .append(", in dieser Reihenfolge.")
          .append(" Schreibt der Kunde einen Schlüssel zusammengesetzt mit dem Trenner „")
          .append(typ.schluessel().trenner())
          .append("“, zerlegst du ihn ohne Rückfrage in diese Felder in ihrer Reihenfolge.");
      // Jeder Teil des Schluessels kann optional sein, an jeder Position (E‑1164). Fehlen Teile,
      // ist nicht immer eindeutig, welche; dann fragt das Modell, statt zu raten.
      List<String> optionale =
          typ.schluessel().felder().stream()
              .filter(name -> typ.feld(name).map(feld -> !feld.pflicht()).orElse(false))
              .map(name -> label(typ, name))
              .toList();
      if (!optionale.isEmpty()) {
        text.append(optionale.size() == 1 ? " Optional ist der Teil " : " Optional sind die Teile ")
            .append(aufzaehlung(optionale))
            .append('.')
            .append(" Hat ein zusammengesetzter Schlüssel weniger Teile als der Schlüssel Felder,")
            .append(" zerlege ihn nur, wenn eindeutig ist, welche fehlen; sonst frage nach, statt")
            .append(" zu raten.");
      }
      // Leerzeichen am Rand eines Teils kuerzt das Backend (E‑1137); das Modell gibt weiter, was
      // der Kunde schreibt, und zieht nichts zusammen (E‑1141).
      text.append(" Schlüsselteile übergibst du zeichengenau, mit jedem Leerzeichen, auch mehreren")
          .append(" hintereinander; Leerzeichen am Anfang und Ende entfernt das Backend.");
    }
    return text.toString();
  }

  private static String label(TypDefinition typ, String name) {
    return typ.feld(name).map(Feld::label).orElse(name);
  }

  /** „A“, „A und B“, „A, B und C“. */
  private static String aufzaehlung(List<String> namen) {
    if (namen.size() == 1) {
      return namen.getFirst();
    }
    return String.join(", ", namen.subList(0, namen.size() - 1)) + " und " + namen.getLast();
  }

  static String feldbeschreibung(Feld feld) {
    List<String> teile = new ArrayList<>();
    teile.add(feld.label() + ".");
    teile.add(feld.pflicht() ? "Pflichtfeld." : "Optional.");
    if (feld.nurBei() != null) {
      teile.add(
          "Nur bei "
              + feld.nurBei().stream()
                  .map(aktion -> "„" + aktion + "“")
                  .collect(Collectors.joining(" oder "))
              + "; bei anderen Aktionen weglassen.");
    }
    if (feld.muster() != null) {
      teile.add("Format (regulärer Ausdruck für den ganzen Wert): `" + feld.muster() + "`.");
    }
    if (feld.beispiel() != null) {
      teile.add("Beispiel: " + feld.beispiel() + ".");
    }
    if (feld.hinweis() != null) {
      teile.add(feld.hinweis());
    }
    return String.join(" ", teile);
  }
}
