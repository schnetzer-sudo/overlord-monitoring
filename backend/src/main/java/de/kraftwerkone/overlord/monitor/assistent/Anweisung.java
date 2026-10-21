package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Die feste Anweisung an das Modell (E‑1040, {@code docs/assistent.md} §4.4): der Text aus der
 * versionierten Ressource {@value #RESSOURCE}, dahinter der Name des aktiven Mandanten und je
 * angebotenem Typ Name, Werkzeug und Beschreibung aus {@code task_type}.
 *
 * <p><b>Nichts sonst</b> (E‑1035, E‑1059): kein Benutzername, keine Mandanten-ID, keine Daten aus
 * {@code GlassfishDB} ausser dem Namen des Mandanten, keine Auftraege. Die Methode bekommt den
 * Namen als Text und weder Nutzer noch {@code MandantContext}.
 */
public final class Anweisung {

  static final String RESSOURCE = "/assistent/anweisung.txt";

  private static final String TEXT = lies();

  private Anweisung() {}

  /**
   * Die Anweisung fuer den aktiven Mandanten und diese angebotenen Typen, in ihrer Reihenfolge.
   *
   * @param mandantenname der Name des aktiven Mandanten; {@code null} oder leer, wenn er keinen
   *     hat, dann entfaellt der Abschnitt (nie ersatzweise die Mandanten-ID)
   */
  public static String fuer(String mandantenname, List<TypDefinition> typen) {
    StringBuilder anweisung = new StringBuilder(TEXT.strip());
    if (mandantenname != null && !mandantenname.isBlank()) {
      anweisung
          .append("\n\n# Der aktive Mandant\n\nDer Kunde arbeitet im Mandanten „")
          .append(mandantenname.strip())
          .append("“.");
    }
    anweisung.append("\n\n# Die Auftragstypen, die du vorbereiten kannst\n");
    for (TypDefinition typ : typen) {
      anweisung
          .append("\n## ")
          .append(typ.name())
          .append("\n\nWerkzeug: ")
          .append(typ.typ())
          .append("\n\n")
          .append(typ.beschreibung() == null ? "" : typ.beschreibung().strip())
          .append('\n');
    }
    return anweisung.toString();
  }

  private static String lies() {
    try (InputStream quelle = Anweisung.class.getResourceAsStream(RESSOURCE)) {
      if (quelle == null) {
        throw new IllegalStateException(RESSOURCE + " fehlt im Klassenpfad");
      }
      return new String(quelle.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException fehler) {
      throw new UncheckedIOException(fehler);
    }
  }
}
