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
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.OhneMandantenkontext;
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
import javax.sql.DataSource;
import org.jooq.DSLContext;
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
 * <p>Die Regeln zu {@code jooq.glassfish} stehen seit Schritt 2 im Abschnitt „Datenzugriff" am Ende
 * dieser Klasse.
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

  /** Das generierte Paket. Entstand in Schritt 2 durch Codegenerierung, nie von Hand gepflegt. */
  private static final String GENERIERT = "jooq";

  /**
   * <b>Die namentliche Ausnahme von Regel M2 fuer den Rollup-Job</b> (Schritt 10a, Teil 3).
   *
   * <p>Der Rollup liest bewusst <b>ueber alle Mandanten</b>: {@code message_rollup} kennt nach
   * Entscheidung E-a keinen Mandanten, der wird erst in 10b beim Lesen ueber {@code Process ->
   * ProjectMandant} gejoint. Ein {@code MandantContext.alle()} waere eine Luege im Typsystem und
   * wuerde die Regel entwerten, deren einziger Zweck es ist, dass so etwas nicht existiert.
   *
   * <p><b>Die Ausnahme steht namentlich und nicht als Paketfreibrief.</b> Eine zweite Klasse in
   * {@code rollup}, die {@code jooq.glassfish} anfasst, faellt <b>nicht</b> von selbst darunter —
   * sie faellt bei {@link #mandantcontext_ist_erster_parameter} durch, bis jemand sie hier
   * eintraegt und begruendet. Das ist der Unterschied zwischen einer Ausnahme und einer
   * aufgeweichten Regel.
   *
   * <p>Vollstaendig gefuehrt in {@code docs/mandantentrennung.md} §4 und {@code docs/rollup.md}.
   * <b>Taucht hier jemals eine zweite auf, ist das ein Signal und keine Kleinigkeit</b> — genau wie
   * eine vierte Endpunkt-Ausnahme in §3 derselben Datei.
   */
  private static final List<String> ROLLUP_AUSNAHME = List.of("RollupLeseRepository");

  /** Das Paket, in dem {@link #ROLLUP_AUSNAHME} allein gilt. */
  private static final String ROLLUP_PAKET = BASIS + ".rollup";

  /**
   * Die schreibenden Einstiegspunkte von {@code DSLContext}.
   *
   * <p>Sie stehen namentlich, weil ArchUnit nicht sehen kann, <b>welcher</b> {@code DSLContext} an
   * einer Aufrufstelle steht. Die Liste greift deshalb nur dort, wo ohnehin feststeht, dass es der
   * Lese-Kontext ist: in Klassen, die {@code jooq.glassfish} anfassen (siehe {@link
   * #rollup_schreibt_nicht_auf_glassfish}).
   */
  private static final List<String> JOOQ_SCHREIBEND =
      List.of(
          "insertInto",
          "insertQuery",
          "update",
          "updateQuery",
          "delete",
          "deleteFrom",
          "deleteQuery",
          "mergeInto",
          "truncate",
          "batch",
          "batchInsert",
          "batchUpdate",
          "batchDelete",
          "batchStore",
          "batched",
          "execute",
          "executeInsert",
          "executeUpdate",
          "executeDelete",
          "loadInto",
          "createTable",
          "dropTable",
          "alterTable");

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
            .and()
            .resideOutsideOfPackage(musterFuer(GENERIERT))
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
            .that()
            .resideOutsideOfPackage(musterFuer(GENERIERT))
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
            .that()
            .resideOutsideOfPackage(musterFuer(GENERIERT))
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
            .that()
            .resideOutsideOfPackage(musterFuer(GENERIERT))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.fasterxml.jackson..")
            .because(
                "Spring Boot 4 bringt Jackson 3 unter tools.jackson. Ein Import auf"
                    + " com.fasterxml.jackson ist fast immer ein Muster aus Spring Boot 3.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  // ───────────────────────────────────────────────────────────────────────────────────────────
  // Datenzugriff (ab Schritt 2)
  // ───────────────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Generierte glassfish-Typen nur in Repository-Klassen (Regeln M2, Schreibschutz)")
  void jooq_glassfish_nur_in_repository_klassen() {
    ArchRule regel =
        noClasses()
            .that()
            .resideOutsideOfPackage(musterFuer(GENERIERT))
            .and()
            .haveSimpleNameNotEndingWith("Repository")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BASIS + ".jooq.glassfish..")
            .because(
                "am Import einer Tabelle aus dem Quellschema muss sofort erkennbar sein, dass sie"
                    + " nur gelesen wird. Nur Repository-Klassen fassen jooq.glassfish an; Services"
                    + " und Controller sehen ausschliesslich eigene Typen.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }

  /**
   * Regel M2, maschinell: Wer das Quellschema anfasst, bekommt den Mandanten als <b>ersten</b>
   * Parameter. Nicht als zweiten, nicht optional, nicht in einer Ueberladung daneben.
   *
   * <p>Es gibt <b>zwei</b> zulaessige Abweichungen, und beide sind namentlich gefuehrt:
   *
   * <ol>
   *   <li>{@link OhneMandantenkontext} — fuer die Methoden, die den Kontext erst herstellen. Der
   *       Test {@link #ausnahme_nur_im_mandantrepository} haelt fest, dass die Markierung nirgends
   *       sonst auftaucht.
   *   <li>{@link #ROLLUP_AUSNAHME} — fuer die Klasse, die ueber alle Mandanten aggregiert. Der Test
   *       {@link #rollup_ausnahme_ist_namentlich_und_eng} haelt fest, dass sie eng bleibt und nicht
   *       ins Leere laeuft.
   * </ol>
   */
  @Test
  @DisplayName("MandantContext ist erster Parameter jeder Methode auf jooq.glassfish (Regel M2)")
  void mandantcontext_ist_erster_parameter() {
    List<String> verstoesse = new ArrayList<>();
    for (JavaClass klasse : klassenMitGlassfishZugriff()) {
      if (istRollupAusnahme(klasse)) {
        continue;
      }
      for (JavaMethod methode : klasse.getMethods()) {
        if (!methode.getModifiers().contains(JavaModifier.PUBLIC)
            || methode.isAnnotatedWith(OhneMandantenkontext.class)) {
          continue;
        }
        List<JavaClass> parameter = methode.getRawParameterTypes();
        boolean inOrdnung =
            !parameter.isEmpty() && parameter.getFirst().isEquivalentTo(MandantContext.class);
        if (!inOrdnung) {
          verstoesse.add(klasse.getSimpleName() + "." + methode.getName());
        }
      }
    }
    assertThat(verstoesse)
        .as(
            "Diese oeffentlichen Methoden fassen jooq.glassfish an, ohne MandantContext als ersten"
                + " Parameter zu verlangen. Entweder fehlt der Parameter — oder die Methode stellt"
                + " den Kontext erst her und braucht @OhneMandantenkontext mit Begruendung — oder"
                + " sie aggregiert bewusst ueber alle Mandanten und gehoert namentlich in"
                + " ROLLUP_AUSNAHME. Kein Schein-Kontext, keine dritte Umgehung.")
        .isEmpty();
  }

  /**
   * Die Rollup-Ausnahme, von beiden Seiten festgenagelt: Sie ist <b>nicht leer</b> (sonst prueft
   * die Ausnahme nichts mehr und gehoert geloescht) und <b>nicht breiter</b> als die Liste, die sie
   * benennt.
   *
   * <p>Und sie ist <b>ehrlich</b>: Die ausgenommene Klasse traegt den {@code MandantContext}
   * nirgends in einer Signatur. Ein Schein-Kontext — ein Parameter, der entgegengenommen und nicht
   * verwendet wird — waere schlimmer als gar keiner, weil er von aussen wie Mandantentrennung
   * aussieht.
   */
  @Test
  @DisplayName("Die Rollup-Ausnahme von Regel M2 ist namentlich, eng und nicht leer")
  void rollup_ausnahme_ist_namentlich_und_eng() {
    List<String> ausgenommen =
        klassenMitGlassfishZugriff().stream()
            .filter(PaketstrukturTest::istRollupAusnahme)
            .map(JavaClass::getSimpleName)
            .toList();

    assertThat(ausgenommen)
        .as(
            "Die Ausnahme nennt %s. Ist die Klasse verschwunden oder umbenannt, prueft die Ausnahme"
                + " nichts mehr und gehoert aus PaketstrukturTest, docs/mandantentrennung.md §4 und"
                + " docs/rollup.md entfernt — nicht stehengelassen.",
            ROLLUP_AUSNAHME)
        .containsExactlyInAnyOrderElementsOf(ROLLUP_AUSNAHME);

    for (JavaClass klasse : klassenMitGlassfishZugriff()) {
      if (!istRollupAusnahme(klasse)) {
        continue;
      }
      for (JavaMethod methode : klasse.getMethods()) {
        assertThat(methode.getRawParameterTypes())
            .as(
                "%s.%s nimmt einen MandantContext entgegen, obwohl die Klasse von Regel M2"
                    + " ausgenommen ist. Entweder gilt die Regel — dann gehoert die Klasse aus der"
                    + " Ausnahme —, oder sie gilt nicht, dann ist der Parameter ein Schein-Kontext.",
                klasse.getSimpleName(), methode.getName())
            .noneMatch(parameter -> parameter.isEquivalentTo(MandantContext.class));
      }
    }
  }

  /**
   * <b>Nichts im Paket {@code rollup}, das {@code jooq.glassfish} anfasst, schreibt darauf.</b>
   *
   * <p>Die drei Schichten des Schreibschutzes ({@code docs/datenzugriff.md} §4) fangen den Fall zur
   * Laufzeit: die Rechte von {@code monitor_read}, der {@code readOnly}-Pool und der {@code
   * ReadOnlyExecuteListener} auf {@code glassfishDsl}. Dieser Test faengt ihn <b>vor</b> dem ersten
   * Lauf.
   *
   * <p><b>Warum die Regel auf {@code rollup} beschraenkt ist und nicht projektweit gilt.</b>
   * ArchUnit sieht nicht, <b>welcher</b> {@code DSLContext} an einer Aufrufstelle steht — nur den
   * Typ. In Klassen, die beide Kontexte halten, waere die Regel deshalb falsch: {@code
   * ProzessKatalogRepository} liest schemauebergreifend ueber {@code glassfishDsl} und schreibt
   * ueber {@code monitorDsl}, in einer Klasse und mit Begruendung ({@code
   * docs/prozess-katalog-backend.md}). Im Rollup ist das anders <b>gebaut</b>: {@code
   * RollupLeseRepository} haelt ausschliesslich den Lese-Kontext, {@code RollupSchreibRepository}
   * ausschliesslich den Schreib-Kontext. Genau diese Trennung haelt dieser Test fest — sie ist auch
   * der Grund, warum {@link #ROLLUP_AUSNAHME} nur eine Klasse nennt und nicht das Paket.
   *
   * <p>Wer beides in einer Rollup-Klasse braucht, hebt damit auch diese Regel auf. Dass das
   * auffaellt, ist der Zweck.
   */
  @Test
  @DisplayName("Nichts in rollup, das jooq.glassfish anfasst, schreibt darauf")
  void rollup_schreibt_nicht_auf_glassfish() {
    List<String> verstoesse = new ArrayList<>();
    List<JavaClass> geprueft = new ArrayList<>();
    for (JavaClass klasse : klassenMitGlassfishZugriff()) {
      if (!klasse.getPackageName().equals(ROLLUP_PAKET)) {
        continue;
      }
      geprueft.add(klasse);
      klasse.getMethodCallsFromSelf().stream()
          .filter(aufruf -> aufruf.getTargetOwner().isAssignableTo(DSLContext.class))
          .filter(aufruf -> JOOQ_SCHREIBEND.contains(aufruf.getName()))
          .forEach(
              aufruf ->
                  verstoesse.add(klasse.getSimpleName() + " -> DSLContext." + aufruf.getName()));
    }
    assertThat(geprueft)
        .as("Ohne eine Rollup-Klasse, die jooq.glassfish anfasst, prueft diese Regel nichts")
        .isNotEmpty();
    assertThat(verstoesse)
        .as(
            "Diese Rollup-Klassen lesen aus GlassfishDB und rufen zugleich eine schreibende"
                + " jOOQ-Methode auf. Im Rollup sind Lesen und Schreiben auf zwei Klassen"
                + " verteilt — wer sie zusammenzieht, hebt den Schutz auf.")
        .isEmpty();
  }

  /** Gehoert die Klasse zur namentlichen Rollup-Ausnahme? Paket <b>und</b> Name muessen passen. */
  private static boolean istRollupAusnahme(JavaClass klasse) {
    return klasse.getPackageName().equals(ROLLUP_PAKET)
        && ROLLUP_AUSNAHME.contains(klasse.getSimpleName());
  }

  @Test
  @DisplayName("@OhneMandantenkontext steht ausschliesslich im MandantRepository")
  void ausnahme_nur_im_mandantrepository() {
    List<String> fundstellen = new ArrayList<>();
    for (JavaClass klasse : KLASSEN) {
      for (JavaMethod methode : klasse.getMethods()) {
        if (methode.isAnnotatedWith(OhneMandantenkontext.class)) {
          fundstellen.add(klasse.getSimpleName());
        }
      }
    }
    assertThat(fundstellen)
        .as(
            "Die Ausnahme von Regel M2 ist namentlich gefuehrt. Taucht sie ausserhalb des"
                + " MandantRepository auf, ist das ein Signal und keine Kleinigkeit — genau wie"
                + " eine dritte Endpunkt-Ausnahme in docs/mandantentrennung.md.")
        .containsOnly("MandantRepository");
  }

  /** Alle handgeschriebenen Klassen, die irgendeinen Typ aus {@code jooq.glassfish} verwenden. */
  private static List<JavaClass> klassenMitGlassfishZugriff() {
    String glassfishPaket = BASIS + ".jooq.glassfish";
    List<JavaClass> treffer = new ArrayList<>();
    for (JavaClass klasse : KLASSEN) {
      if (klasse.getPackageName().startsWith(BASIS + "." + GENERIERT)) {
        continue;
      }
      boolean fasstAn =
          klasse.getDirectDependenciesFromSelf().stream()
              .anyMatch(
                  abhaengigkeit ->
                      abhaengigkeit.getTargetClass().getPackageName().startsWith(glassfishPaket));
      if (fasstAn) {
        treffer.add(klasse);
      }
    }
    assertThat(treffer)
        .as(
            "Es gibt keine Klasse mehr, die jooq.glassfish anfasst — dann prueft diese Regel nichts")
        .isNotEmpty();
    return treffer;
  }

  @Test
  @DisplayName("javax.sql.DataSource wird nur in config verdrahtet")
  void datasource_nur_in_config() {
    ArchRule regel =
        noClasses()
            .that()
            .resideOutsideOfPackage(musterFuer(VERDRAHTUNG))
            .and()
            .resideOutsideOfPackage(musterFuer(GENERIERT))
            .should()
            .dependOnClassesThat()
            .areAssignableTo(DataSource.class)
            .because(
                "die DataSources sind reine Verdrahtung. Fachcode kennt nur DSLContext, niemals die"
                    + " DataSource selbst.")
            .allowEmptyShould(true);
    regel.check(KLASSEN);
  }
}
