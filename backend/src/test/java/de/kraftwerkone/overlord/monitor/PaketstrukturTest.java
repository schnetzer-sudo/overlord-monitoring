package de.kraftwerkone.overlord.monitor;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Haelt die Paketstruktur aus Abschnitt 6 der Projektbeschreibung maschinell fest.
 *
 * <p>ArchUnit wird hier bewusst ueber das Kern-Artefakt und normale {@code @Test}-Methoden benutzt,
 * nicht ueber {@code archunit-junit5}: Spring Boot 4.1 liefert JUnit Jupiter 6 und damit JUnit
 * Platform 6, waehrend die ArchUnit-JUnit5-Engine auf Platform 1.x zielt.
 *
 * <p>Ein neues Paket wird an genau einer Stelle eingetragen — in {@link #VERDRAHTUNG}, {@link
 * #GEMEINSAME_PAKETE} oder {@link #FACHPAKETE}. Alle Regeln leiten ihre Paketlisten daraus ab, ein
 * Paket kann also nicht versehentlich aus der Pruefung fallen.
 *
 * <p>Die Regeln zu {@code jooq.glassfish} folgen in Schritt 2, sobald es generierten Code gibt.
 */
class PaketstrukturTest {

  private static final String BASIS = "de.kraftwerkone.overlord.monitor";

  /** Verdrahtung: darf alles sehen, wird von niemandem importiert. */
  private static final String VERDRAHTUNG = "config";

  /** Gemeinsames: darf von jedem Fachpaket genutzt werden. */
  private static final List<String> GEMEINSAME_PAKETE = List.of("common", "security", "audit");

  /** Fachpakete: kennen einander nicht. */
  private static final List<String> FACHPAKETE =
      List.of("message", "bam", "payload", "catalog", "rollup", "dashboard", "admin");

  /** Das generierte Paket. Wird nicht von Hand angelegt, entsteht in Schritt 2. */
  private static final String GENERIERT = "jooq";

  private static final JavaClasses KLASSEN =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASIS);

  /**
   * Alle Pakete aus Abschnitt 6, die von Hand angelegt werden. {@code jooq} fehlt absichtlich: Es
   * entsteht in Schritt 2 durch Codegenerierung.
   */
  private static List<String> erwartetePakete() {
    List<String> alle = new ArrayList<>();
    alle.add(VERDRAHTUNG);
    alle.addAll(GEMEINSAME_PAKETE);
    alle.addAll(FACHPAKETE);
    return alle;
  }

  /** Macht aus Paketnamen ArchUnit-Muster: {@code common} wird zu {@code ...monitor.common..}. */
  private static String[] muster(List<String> pakete) {
    return pakete.stream().map(paket -> BASIS + "." + paket + "..").toArray(String[]::new);
  }

  private static String musterFuer(String paket) {
    return BASIS + "." + paket + "..";
  }

  /** Gemeinsames plus das generierte Paket — Abhaengigkeiten dorthin sind erlaubt. */
  private static String[] gemeinsamesUndGeneriertes() {
    List<String> pakete = new ArrayList<>(GEMEINSAME_PAKETE);
    pakete.add(GENERIERT);
    return muster(pakete);
  }

  /** Alle Anwendungspakete ausser {@code common} selbst und dem generierten Paket. */
  private static String[] anwendungspaketeAusserCommon() {
    List<String> pakete = erwartetePakete();
    pakete.remove("common");
    return muster(pakete);
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Struktur
  // ───────────────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Alle Pakete aus Abschnitt 6 existieren")
  void alle_pakete_aus_abschnitt_6_existieren() {
    for (String paket : erwartetePakete()) {
      String vollerName = BASIS + "." + paket;
      boolean vorhanden = false;
      for (JavaClass klasse : KLASSEN) {
        if (klasse.getPackageName().equals(vollerName)) {
          vorhanden = true;
          break;
        }
      }
      assertThat(vorhanden)
          .as(
              "Paket %s fehlt oder ist leer. Jedes Paket aus Abschnitt 6 der Projektbeschreibung"
                  + " braucht mindestens eine package-info.java.",
              vollerName)
          .isTrue();
    }
  }

  @Test
  @DisplayName("Die Hauptklasse liegt direkt im Basispaket")
  void hauptklasse_liegt_im_basispaket() {
    ArchRule regel =
        classes()
            .that()
            .areAnnotatedWith(SpringBootApplication.class)
            .should()
            .resideInAPackage(BASIS)
            .because(
                "die Hauptklasse liegt direkt im Basispaket, damit der Component-Scan die gesamte"
                    + " Anwendung erfasst");
    regel.check(KLASSEN);
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Abhaengigkeiten zwischen den Paketen
  // ───────────────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Fachpakete kennen einander nicht")
  void fachpakete_kennen_einander_nicht() {
    ArchRule regel =
        slices()
            .matching(BASIS + ".(*)..")
            .namingSlices("Paket '$1'")
            .should()
            .notDependOnEachOther()
            // Gemeinsames darf jeder nutzen.
            .ignoreDependency(alwaysTrue(), resideInAnyPackage(gemeinsamesUndGeneriertes()))
            // Verdrahtung darf alles sehen.
            .ignoreDependency(resideInAPackage(musterFuer(VERDRAHTUNG)), alwaysTrue())
            .because(
                "Fachpakete kennen einander nicht. Braucht ein zweites Fachpaket einen Typ, wandert"
                    + " der Typ nach common, nie ins Nachbarpaket.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  @Test
  @DisplayName("common haengt an keinem anderen Anwendungspaket")
  void common_haengt_an_keinem_anderen_anwendungspaket() {
    ArchRule regel =
        noClasses()
            .that()
            .resideInAPackage(musterFuer("common"))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(anwendungspaketeAusserCommon())
            .because("common ist das Fundament. Haengt es an einem Fachpaket, ist es keines mehr.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  @Test
  @DisplayName("Niemand haengt an config")
  void niemand_haengt_an_config() {
    ArchRule regel =
        noClasses()
            .that()
            .resideOutsideOfPackage(musterFuer(VERDRAHTUNG))
            .should()
            .dependOnClassesThat()
            .resideInAPackage(musterFuer(VERDRAHTUNG))
            .because(
                "config ist reine Verdrahtung. Wer daraus importiert, verdrahtet an der falschen"
                    + " Stelle.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Unverhandelbare Regeln, die sich maschinell pruefen lassen
  // ───────────────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Die Systemuhr wird nirgends direkt gelesen (Regel Z1)")
  void die_systemuhr_wird_nirgends_direkt_gelesen() {
    ArchRule regel =
        noClasses()
            .that()
            .resideOutsideOfPackage(musterFuer("common"))
            .should()
            .callMethod(LocalDateTime.class, "now")
            .orShould()
            .callMethod(LocalDate.class, "now")
            .orShould()
            .callMethod(LocalTime.class, "now")
            .orShould()
            .callMethod(Instant.class, "now")
            .orShould()
            .callMethod(ZonedDateTime.class, "now")
            .orShould()
            .callMethod(OffsetDateTime.class, "now")
            .orShould()
            .callMethod(Year.class, "now")
            .orShould()
            .callMethod(YearMonth.class, "now")
            .orShould()
            .callMethod(System.class, "currentTimeMillis")
            .orShould()
            .callConstructor(Date.class)
            .because(
                "die Testkopie endet Ende 2025. Wer die Systemuhr direkt liest, bekommt dort ein"
                    + " leeres Zeitfenster. Stattdessen TimeProvider aus common.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  @Test
  @DisplayName("Kein JPA, kein Hibernate")
  void kein_jpa_kein_hibernate() {
    ArchRule regel =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..", "org.hibernate..")
            .because(
                "das Quellschema gehoert uns nicht, ist lesegetrieben und teilweise EAV."
                    + " Datenzugriff laeuft ueber jOOQ.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  @Test
  @DisplayName("Kein javax.* — das Projekt laeuft auf Jakarta EE 11")
  void kein_javax() {
    ArchRule regel =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("javax.servlet..", "javax.validation..", "javax.annotation..")
            .because(
                "Spring Boot 4 setzt Jakarta EE 11 voraus. javax.* ist ein Muster der"
                    + " Vorgaengergeneration.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  @Test
  @DisplayName("Kein Jackson 2 — Spring Boot 4 bringt Jackson 3")
  void kein_jackson_2() {
    ArchRule regel =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.fasterxml.jackson..")
            .because(
                "Spring Boot 4 bringt Jackson 3 unter tools.jackson. Ein Import auf"
                    + " com.fasterxml.jackson ist fast immer ein Muster aus Spring Boot 3.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }
}
