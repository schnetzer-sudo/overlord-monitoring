package de.kraftwerkone.overlord.monitor.payload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Die Artefaktkennung: stabil, URL-tauglich — und <b>ohne GUID und ohne Ablagenkennung</b>.
 *
 * <p>Der letzte Punkt ist die sicherheitskritische Zeile dieses Features. Der Test haelt ihn
 * maschinell fest, damit ein spaeteres „waere doch bequemer" auffaellt.
 */
class ArtefaktIdTest {

  @Test
  @DisplayName("Kodieren und wieder zerlegen ergibt dasselbe")
  void hin_und_zurueck() {
    ArtefaktId kennung = new ArtefaktId((short) 2, "FileReader.Log.GUID");

    assertThat(kennung.kodiere()).isEqualTo("2-FileReader.Log.GUID");
    assertThat(ArtefaktId.entschluessle(kennung.kodiere())).contains(kennung);
  }

  @Test
  @DisplayName("Der Metadaten-Schritt 0 ebenso")
  void schritt_null() {
    ArtefaktId kennung = new ArtefaktId((short) 0, "FileReader.Payload.GUID");

    assertThat(kennung.kodiere()).isEqualTo("0-FileReader.Payload.GUID");
    assertThat(ArtefaktId.entschluessle("0-FileReader.Payload.GUID")).contains(kennung);
  }

  /**
   * <b>Die Kennung des Zeigers zerfaellt weiterhin sauber — und findet trotzdem nichts.</b>
   *
   * <p>Seit dem 19.08.2026 fuehrt die Artefaktliste {@code Message.Payload.GUID} nicht mehr (M73,
   * {@link Artefaktnamen#NAME_ZEIGER}). Diese Klasse ist rein syntaktisch und weiss davon nichts:
   * Die Form ist unveraendert gueltig. Was fehlt, ist die <i>Zeile</i>, auf die sie passt — und
   * damit antwortet der Endpunkt {@code 404}, wie bei jeder unbekannten Kennung.
   *
   * <p><b>Das ist kein Sonderpfad, sondern das Ausbleiben eines Sonderpfads.</b> Es ist bewusst
   * kein Umleitungspfad fuer alte Kennungen gebaut worden: Das Feature ist einen Tag alt, ein
   * bereits geteilter Verweis auf diese Kennung ist praktisch ausgeschlossen.
   */
  @Test
  @DisplayName("Die Kennung des Zeigers zerfaellt weiterhin — sie trifft nur keine Zeile mehr")
  void zeiger_zerfaellt_weiterhin() {
    ArtefaktId kennung = new ArtefaktId((short) 0, Artefaktnamen.NAME_ZEIGER);

    assertThat(kennung.kodiere()).isEqualTo("0-Message.Payload.GUID");
    assertThat(ArtefaktId.entschluessle("0-Message.Payload.GUID")).contains(kennung);
  }

  @Test
  @DisplayName("Sie enthaelt weder GUID noch Ablagenkennung")
  void ohne_verweis() {
    String kodiert = new ArtefaktId((short) 3, "Converter.Payload.GUID").kodiere();

    assertThat(kodiert)
        .as(
            "Naehme ein Endpunkt einen Verweis entgegen, waere er ein offener Proxy vor einer"
                + " Produktionsablage — genau der Fehler des Altsystems (JsonServlet.java:165–:171)")
        .doesNotContain("FILESTORE")
        .doesNotContain("|")
        .doesNotMatch(".*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-.*");
  }

  @Test
  @DisplayName("Sie besteht ausschliesslich aus unreservierten URL-Zeichen")
  void url_tauglich() {
    String kodiert = new ArtefaktId((short) 12, "FTPSender.Log.GUID").kodiere();

    assertThat(kodiert).matches("[A-Za-z0-9._-]+");
  }

  @ParameterizedTest
  @DisplayName("Unbrauchbare Kennungen werden nicht zurechtgebogen")
  @ValueSource(
      strings = {
        "", // leer
        "-", // beide Haelften leer
        "-Message.Payload.GUID", // ohne Schritt
        "2-", // ohne Namen
        "zwei-Message.Payload.GUID", // Schritt keine Zahl
        "-1-Message.Payload.GUID", // negativer Schritt
        "999999-Message.Payload.GUID", // passt nicht in SMALLINT
        "٢-Message.Payload.GUID", // arabisch-indische Ziffer: zweite Schreibweise derselben Kennung
        "2-../../etc/passwd", // Pfadwechsel
        "2-Message Payload GUID", // Leerzeichen
        "2-Message.Payload.GUID%00", // Prozentzeichen
        "2-Message.Payload.GUID\nX-Kopf: wert" // Zeilenumbruch
      })
  void unbrauchbar(String kodiert) {
    assertThat(ArtefaktId.entschluessle(kodiert)).isEmpty();
  }

  @Test
  @DisplayName("Auch null")
  void null_ist_leer() {
    assertThat(ArtefaktId.entschluessle(null)).isEqualTo(Optional.empty());
  }

  @Test
  @DisplayName("Ein Name laenger als die Spalte kann keine Zeile treffen")
  void zu_langer_name() {
    assertThat(ArtefaktId.entschluessle("2-" + "a".repeat(101))).isEmpty();
    assertThat(ArtefaktId.entschluessle("2-" + "a".repeat(100))).isPresent();
  }

  @Test
  @DisplayName("Zerlegt wird am ersten Bindestrich — ein Name mit Bindestrich bleibt heil")
  void erster_bindestrich() {
    assertThat(ArtefaktId.entschluessle("7-Ein-Dienst.Log.GUID"))
        .contains(new ArtefaktId((short) 7, "Ein-Dienst.Log.GUID"));
  }
}
