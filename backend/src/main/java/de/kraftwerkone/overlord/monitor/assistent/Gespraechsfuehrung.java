package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.assistent.modell.Modellanfrage;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellantwort;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Kundennachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Modellnachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Werkzeugergebnisse;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugaufruf;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeugergebnis;
import de.kraftwerkone.overlord.monitor.common.auftrag.AuftragsPruefung;
import de.kraftwerkone.overlord.monitor.common.auftrag.Prueffehler;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Die Gespraechsfuehrung je Nachricht des Kunden ({@code docs/assistent.md} §4.6): Modell rufen,
 * Werkzeugaufrufe pruefen und beantworten, hoechstens {@code maxRunden} Runden.
 *
 * <p><b>Das Modell legt nie an und beurteilt Werte nicht selbst</b> (E‑1033). Jeder Vorschlag geht
 * durch {@link AuftragsPruefung}; Fehler gehen als Werkzeugergebnis zurueck, ein gueltiger
 * Vorschlag wird zur Karte. Diese Klasse kennt weder {@code task} noch die Datenbank; sie bekommt
 * das aktuelle Angebot des Mandanten gereicht.
 *
 * <p><b>Haengt nur an der neutralen Schnittstelle</b> ({@link Sprachmodell}), nie an einem Adapter
 * (E‑1032, {@code PaketstrukturTest}).
 *
 * <p><b>Der Verlauf waechst nur am Ende.</b> Erst eine vollstaendig beantwortete Nachricht kommt in
 * das Gespraech; wirft das Modell einen Fehler, bleibt es unveraendert (F8). Bricht die Nachricht
 * ab (Runden ausgeschoepft, abgeschnitten, verweigert), stehen danach nur die Nachricht des Kunden
 * und die feste Antwort im Verlauf (F5 bis F7).
 */
public class Gespraechsfuehrung {

  static final String RUNDEN_AUSGESCHOEPFT =
      "Daraus konnte ich noch keinen gültigen Vorschlag machen. Bitte nennen Sie mir die Angaben"
          + " noch einmal, am besten Eintrag für Eintrag.";

  static final String ABGESCHNITTEN =
      "Das sind zu viele Einträge für einen Vorschlag auf einmal. Bitte teilen Sie sie auf mehrere"
          + " Aufträge auf und nennen Sie mir zuerst einen Teil.";

  static final String NUR_KARTE =
      "Bitte prüfen Sie den Vorschlag auf der Karte. Dort können Sie ihn noch ändern und den"
          + " Auftrag anlegen.";

  static final String OHNE_TEXT = "Bitte beschreiben Sie mir, was Sie ändern möchten.";

  static final String GUELTIG =
      "Der Vorschlag ist gültig. Der Kunde sieht ihn jetzt als Karte, kann ihn dort ändern und legt"
          + " den Auftrag dort selbst an. Sag ihm das. Du legst nichts an und sagst nie, du hättest"
          + " etwas angelegt.";

  static final String NICHT_ANGEBOTEN =
      "Diesen Auftragstyp gibt es hier nicht. Nutze nur die Werkzeuge der angebotenen Typen.";

  static final String SCHON_EIN_VORSCHLAG =
      "In dieser Antwort steht schon ein gültiger Vorschlag. Je Antwort wird nur ein Vorschlag"
          + " gezeigt; bereite den nächsten vor, wenn der Kunde diesen erledigt hat.";

  /** Wie eine Nachricht des Kunden geendet hat — fuer Antwort und Protokoll. */
  public enum Ausgang {
    TEXT,
    VORSCHLAG,
    RUNDEN_AUSGESCHOEPFT,
    ABGESCHNITTEN,
    VERWEIGERT
  }

  /** Das Ergebnis einer Nachricht. {@code vorschlag} ist {@code null}, wenn keiner entstand. */
  public record Ergebnis(String text, Vorschlag vorschlag, Ausgang ausgang) {}

  /** Was das Protokoll braucht (E‑1039), auch wenn die Nachricht mit einem Fehler endet. */
  public static final class Zaehler {
    private long tokenEin;
    private long tokenAus;
    private int werkzeugaufrufe;
    private String modell;

    void zaehle(Modellantwort antwort) {
      tokenEin += antwort.tokenEin();
      tokenAus += antwort.tokenAus();
      werkzeugaufrufe += antwort.aufrufe().size();
      if (antwort.modell() != null) {
        modell = antwort.modell();
      }
    }

    /** Zaehlt einen anderen Zaehler dazu, etwa ueber mehrere Nachrichten hinweg. */
    public void addiere(Zaehler anderer) {
      tokenEin += anderer.tokenEin;
      tokenAus += anderer.tokenAus;
      werkzeugaufrufe += anderer.werkzeugaufrufe;
      if (anderer.modell != null) {
        modell = anderer.modell;
      }
    }

    public long tokenEin() {
      return tokenEin;
    }

    public long tokenAus() {
      return tokenAus;
    }

    public int werkzeugaufrufe() {
      return werkzeugaufrufe;
    }

    public String modell() {
      return modell;
    }
  }

  private final Sprachmodell modell;
  private final JsonMapper json;
  private final int maxRunden;
  private final int maxTokens;

  public Gespraechsfuehrung(Sprachmodell modell, JsonMapper json, int maxRunden, int maxTokens) {
    this.modell = modell;
    this.json = json;
    this.maxRunden = maxRunden;
    this.maxTokens = maxTokens;
  }

  /**
   * Beantwortet eine Nachricht des Kunden. Der Aufrufer haelt das Gespraech fuer diese Nachricht
   * gesperrt.
   *
   * @param angebot die dem Mandanten jetzt angebotenen Typen, Kennung auf Definition
   * @throws de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler wenn der Anbieter
   *     scheitert; das Gespraech bleibt dann unveraendert
   */
  public Ergebnis fuehre(
      Gespraech gespraech, String text, Map<String, TypDefinition> angebot, Zaehler zaehler) {
    List<Nachricht> neu = new ArrayList<>();
    neu.add(new Kundennachricht(text));
    Vorschlag vorschlag = null;

    for (int runde = 0; ; runde++) {
      List<Nachricht> verlauf = new ArrayList<>(gespraech.verlauf());
      verlauf.addAll(neu);
      Modellantwort antwort =
          modell.rufe(
              new Modellanfrage(gespraech.anweisung(), verlauf, gespraech.werkzeuge(), maxTokens));
      zaehler.zaehle(antwort);

      if (antwort.ende() == Modellantwort.Ende.VERWEIGERT) {
        return abbruch(gespraech, text, verweigert(angebot), Ausgang.VERWEIGERT);
      }
      boolean werkzeug = !antwort.aufrufe().isEmpty();
      if (werkzeug && antwort.ende() == Modellantwort.Ende.ABGESCHNITTEN) {
        return abbruch(gespraech, text, ABGESCHNITTEN, Ausgang.ABGESCHNITTEN);
      }
      if (!werkzeug) {
        String antworttext = antwort.text().isBlank() ? null : antwort.text();
        if (antworttext == null) {
          antworttext = vorschlag != null ? NUR_KARTE : OHNE_TEXT;
          neu.add(Modellnachricht.nurText(antworttext));
        } else {
          neu.add(antwort.alsNachricht());
        }
        gespraech.schliesseAb(neu);
        return new Ergebnis(
            antworttext, vorschlag, vorschlag != null ? Ausgang.VORSCHLAG : Ausgang.TEXT);
      }
      if (runde >= maxRunden) {
        return abbruch(gespraech, text, RUNDEN_AUSGESCHOEPFT, Ausgang.RUNDEN_AUSGESCHOEPFT);
      }

      neu.add(antwort.alsNachricht());
      List<Werkzeugergebnis> ergebnisse = new ArrayList<>();
      for (Werkzeugaufruf aufruf : antwort.aufrufe()) {
        Beantwortet beantwortet = beantworte(aufruf, angebot, vorschlag != null);
        if (beantwortet.vorschlag() != null) {
          vorschlag = beantwortet.vorschlag();
        }
        ergebnisse.add(beantwortet.ergebnis());
      }
      neu.add(new Werkzeugergebnisse(ergebnisse));
    }
  }

  // ─── Ein Werkzeugaufruf ────────────────────────────────────────────────────────────────────

  private record Beantwortet(Werkzeugergebnis ergebnis, Vorschlag vorschlag) {}

  private Beantwortet beantworte(
      Werkzeugaufruf aufruf, Map<String, TypDefinition> angebot, boolean schonEinVorschlag) {
    TypDefinition definition = aufruf.name() == null ? null : angebot.get(aufruf.name());
    if (definition == null) {
      return abgewiesen(aufruf, List.of(NICHT_ANGEBOTEN));
    }
    if (schonEinVorschlag) {
      return abgewiesen(aufruf, List.of(SCHON_EIN_VORSCHLAG));
    }

    List<String> formfehler = new ArrayList<>();
    JsonNode eingabe = aufruf.eingabe();
    String aktion = eingabe == null ? null : text(eingabe.get("aktion"));
    List<Map<String, String>> eintraege = eintraege(eingabe, definition, formfehler);
    if (!formfehler.isEmpty()) {
      return abgewiesen(aufruf, formfehler);
    }

    List<Prueffehler> fehler = AuftragsPruefung.pruefe(definition, aktion, eintraege);
    if (!fehler.isEmpty()) {
      ObjectNode inhalt = json.createObjectNode();
      inhalt.put("gueltig", false);
      ArrayNode liste = inhalt.putArray("fehler");
      for (Prueffehler einzeln : fehler) {
        ObjectNode eintrag = liste.addObject();
        if (einzeln.index() == null) {
          eintrag.putNull("index");
        } else {
          eintrag.put("index", einzeln.index());
        }
        eintrag.put("feld", einzeln.feld());
        eintrag.put("code", einzeln.code().name());
        eintrag.put("meldung", einzeln.meldung());
      }
      return new Beantwortet(
          new Werkzeugergebnis(aufruf.id(), json.writeValueAsString(inhalt), true), null);
    }

    ObjectNode inhalt = json.createObjectNode();
    inhalt.put("gueltig", true);
    inhalt.put("hinweis", GUELTIG);
    return new Beantwortet(
        new Werkzeugergebnis(aufruf.id(), json.writeValueAsString(inhalt), false),
        Vorschlag.aus(definition, aktion, eintraege));
  }

  /**
   * Die Eintraege aus der Eingabe des Modells. <b>Nur Text zaehlt als Wert</b> (F3); {@code null}
   * ist „fehlend“. Alles andere wird nicht umgewandelt, sondern als Formfehler gemeldet — eine Zahl
   * verloere fuehrende Nullen.
   */
  private static List<Map<String, String>> eintraege(
      JsonNode eingabe, TypDefinition definition, List<String> formfehler) {
    JsonNode liste = eingabe == null ? null : eingabe.get("eintraege");
    if (liste == null || liste.isNull()) {
      return List.of();
    }
    if (!liste.isArray()) {
      formfehler.add("„eintraege“ muss eine Liste von Objekten sein.");
      return List.of();
    }
    List<Map<String, String>> eintraege = new ArrayList<>();
    int nummer = 0;
    for (JsonNode knoten : liste) {
      nummer++;
      if (!knoten.isObject()) {
        formfehler.add("Eintrag " + nummer + " ist kein Objekt.");
        continue;
      }
      Map<String, String> werte = new LinkedHashMap<>();
      for (Map.Entry<String, JsonNode> feld : knoten.properties()) {
        JsonNode wert = feld.getValue();
        if (wert.isNull()) {
          continue;
        }
        if (!wert.isString()) {
          String name =
              definition.feld(feld.getKey()).map(TypDefinition.Feld::label).orElse(feld.getKey());
          formfehler.add(
              "Eintrag "
                  + nummer
                  + ", "
                  + name
                  + ": Werte werden als Text übergeben, in Anführung.");
          continue;
        }
        werte.put(feld.getKey(), wert.stringValue());
      }
      eintraege.add(werte);
    }
    return eintraege;
  }

  private Beantwortet abgewiesen(Werkzeugaufruf aufruf, List<String> meldungen) {
    ObjectNode inhalt = json.createObjectNode();
    inhalt.put("gueltig", false);
    ArrayNode liste = inhalt.putArray("fehler");
    meldungen.forEach(meldung -> liste.addObject().put("meldung", meldung));
    return new Beantwortet(
        new Werkzeugergebnis(aufruf.id(), json.writeValueAsString(inhalt), true), null);
  }

  // ─── Abbruch ───────────────────────────────────────────────────────────────────────────────

  /**
   * Beendet die Nachricht mit einer festen Antwort ohne Karte. Die Zwischenschritte entfallen; im
   * Verlauf stehen danach die Nachricht des Kunden und diese Antwort. So bleibt er gueltig (jeder
   * Werkzeugaufruf haette sonst ein Ergebnis gebraucht) und waechst nur am Ende.
   */
  private static Ergebnis abbruch(
      Gespraech gespraech, String text, String antwort, Ausgang ausgang) {
    gespraech.schliesseAb(List.of(new Kundennachricht(text), Modellnachricht.nurText(antwort)));
    return new Ergebnis(antwort, null, ausgang);
  }

  /** Die feste Antwort auf eine Verweigerung, mit den Namen der angebotenen Typen. */
  static String verweigert(Map<String, TypDefinition> angebot) {
    String typen =
        angebot.values().stream()
            .map(definition -> "„" + definition.name() + "“")
            .collect(Collectors.joining(", "));
    return typen.isEmpty()
        ? "Dabei kann ich nicht helfen."
        : "Dabei kann ich nicht helfen. Ich bereite Aufträge dieser Art vor: " + typen + ".";
  }

  private static String text(JsonNode knoten) {
    return knoten != null && knoten.isString() ? knoten.stringValue() : null;
  }
}
