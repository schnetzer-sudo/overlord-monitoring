package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Was der plattformweite Block nicht ausliefert</b> — Regel G1 und Entscheidung E‑122, an den
 * Typen festgemacht statt an einem Antwortrumpf.
 *
 * <h2>Warum an den Typen und nicht (nur) am Rumpf</h2>
 *
 * <p>Ein Rumpf zeigt, was <i>heute</i> drinsteht. Diese Pruefung zeigt, was ueberhaupt drinstehen
 * <b>kann</b>: Ein Feld, das es nicht gibt, kann auch kein Wert fuellen. Dieselbe Bauform wie der
 * Test ueber die Feldnamen bei „Unquittiert" ({@code docs/dashboard.md} §2).
 *
 * <p>Die Gegenprobe am echten Rumpf steht in {@code DashboardIsolationDbIT} — beides zusammen, weil
 * beides Verschiedenes prueft.
 */
class PlattformAntwortTest {

  /**
   * Die vier Spalten von {@code Service}, die keine Antwort verlassen duerfen.
   *
   * <p>Die ersten drei sind Betriebstexte des Altsystems, die vierte traegt einen Hostnamen (Regel
   * G1). Verglichen wird kleingeschrieben und ohne Trennzeichen, damit auch {@code
   * service_connect_string} oder {@code connectString} auffiele.
   */
  private static final List<String> VERBOTEN =
      List.of(
          "connectstring", "connect", "servicename", "description", "statusmessage", "verbindung");

  /** Alle Typen, die ueber die Antwort nach draussen gehen. */
  private static final List<Class<?>> ANTWORTTYPEN =
      List.of(
          PlattformResponse.class,
          DienstResponse.class,
          AblagenResponse.class,
          AblagenzielResponse.class);

  @Test
  @DisplayName("Kein Feld der Antwort traegt ServiceConnectString, ServiceName oder die Meldung")
  void kein_verbotenes_feld() {
    for (Class<?> typ : ANTWORTTYPEN) {
      assertThat(typ.isRecord()).as("%s ist ein Record", typ.getSimpleName()).isTrue();
      for (RecordComponent feld : typ.getRecordComponents()) {
        String name = feld.getName().toLowerCase(Locale.ROOT).replace("_", "");
        assertThat(VERBOTEN)
            .as(
                "%s.%s: Diese Spalte von Service verlaesst das Backend nicht — die drei Texte sind"
                    + " Betriebstexte des Altsystems, die Verbindungszeichenkette faellt unter G1",
                typ.getSimpleName(), feld.getName())
            .noneMatch(name::contains);
      }
    }
  }

  @Test
  @DisplayName("Die Antworttypen tragen auch keinen Typ, der eine Verbindung enthaelt")
  void kein_verbotener_typ() {
    // Ablagenziel traegt den ServiceConnectString und darf deshalb in keinem Antworttyp stehen --
    // auch nicht verschachtelt in einer Liste.
    for (Class<?> typ : ANTWORTTYPEN) {
      assertThat(Arrays.stream(typ.getRecordComponents()).map(RecordComponent::getType))
          .as("%s reicht ein Ablagenziel oder eine Dienstzeile durch", typ.getSimpleName())
          .doesNotContain(Ablagenziel.class, Dienstzeile.class);
      assertThat(Arrays.stream(typ.getRecordComponents()).map(RecordComponent::getGenericType))
          .as("%s reicht eine Liste von Ablagenzielen oder Dienstzeilen durch", typ.getSimpleName())
          .noneMatch(art -> enthaeltTyp(art.getTypeName(), Ablagenziel.class))
          .noneMatch(art -> enthaeltTyp(art.getTypeName(), Dienstzeile.class));
    }
  }

  /**
   * Ob der Typname wirklich <b>diesen</b> Typ nennt und nicht bloss einen, der so anfaengt.
   *
   * <p>{@code AblagenzielResponse} beginnt mit {@code Ablagenziel} — ein blosses {@code contains}
   * schluege hier an und behauptete ein Leck, wo eine Antwortzeile steht. Der Wortgrenzen-Ausdruck
   * trennt beide.
   */
  private static boolean enthaeltTyp(String typname, Class<?> gesucht) {
    return Pattern.compile("\b" + Pattern.quote(gesucht.getName()) + "\b").matcher(typname).find();
  }

  @Test
  @DisplayName("toString() des Zieltyps gibt keine Verbindungszeichenkette preis")
  void tostring_verdeckt_die_verbindung() {
    // Die Adresse ist erfunden und existiert nicht -- geprueft wird die Bauform, nicht ein Wert
    // aus dem Bestand.
    String erfundeneAdresse = "http://erfunden.invalid/WebApplication/Empfaenger";
    Ablagenziel ziel = new Ablagenziel("ABLAGE_ERFUNDEN", erfundeneAdresse);

    assertThat(ziel.toString())
        .as("Auch ein versehentliches log.info(\"{}\", ziel) darf nichts preisgeben")
        .doesNotContain(erfundeneAdresse)
        .doesNotContain("erfunden.invalid")
        .contains("ABLAGE_ERFUNDEN");
  }

  @Test
  @DisplayName("Und er verraet den Unterschied zwischen „verdeckt“ und „gibt es nicht“")
  void tostring_unterscheidet_fehlende_verbindung() {
    // Eine Protokollzeile, die nicht sagt, ob ueberhaupt eine Adresse da war, waere nutzlos:
    // "nicht aufloesbar" ist der haeufigste Grund fuer ein rotes Ziel.
    assertThat(new Ablagenziel("ABLAGE_ERFUNDEN", null).toString()).contains("fehlt");
    assertThat(new Ablagenziel("ABLAGE_ERFUNDEN", "http://erfunden.invalid/x").toString())
        .contains("verdeckt");
  }

  @Test
  @DisplayName("Die Dienstzeile liest die drei Spalten und keine vierte")
  void dienstzeile_hat_drei_spalten() {
    // Eine Spalte, die gar nicht erst gelesen wird, kann in keiner Antwort landen -- das ist der
    // strengere Schutz als ein Feld, das man beim Zusammenbau weglaesst.
    assertThat(Arrays.stream(Dienstzeile.class.getRecordComponents()).map(RecordComponent::getName))
        .containsExactly("serviceId", "rohwert", "stand");
  }
}
