package de.kraftwerkone.overlord.monitor.common;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Der Ersatz fuer das vollstaendige {@code switch}</b> — die einzige Sicherung, die es bei einer
 * Menge geben kann.
 *
 * <p>Beim {@link MessageStatusKind} schuetzt ein {@code switch} ohne {@code default} davor, dass
 * ein neuer Wert eine stille Voreinstellung erbt. Bei einer <b>Menge</b> gibt es diesen Schutz
 * nicht: Jede der sechzehn Spaltenbelegungen ist syntaktisch gueltig, und keine loest einen
 * Compilerfehler aus. Dieser Test schliesst die Luecke von der Datenseite her — er erhebt die
 * <i>vorkommenden</i> Kombinationen und wird rot, sobald eine auftaucht, fuer die es keine
 * Formulierung gibt.
 *
 * <p><b>Die Rollen werden ueber {@link Kettenrollen} bestimmt und nicht in SQL nachgebaut.</b>
 * Damit prueft der Test die Regel, die die Anwendung tatsaechlich benutzt, und nicht eine zweite
 * Fassung derselben Regel — sonst pruefte er, ob zwei Nachbauten uebereinstimmen.
 *
 * <p><b>Gruppiert wird in der Datenbank, uebertragen werden hoechstens sechzehn Zeilen.</b> Die
 * Alternative — 214.330 Zeilen holen und in Java gruppieren — pruefte dasselbe und kostete das
 * Hundertfache.
 *
 * <p><b>Kein Mandantenfilter</b>, aus demselben Grund wie in M23 bis M28: Die Frage betrifft das
 * <i>Datenmodell</i> und nicht den Ausschnitt eines Kunden. Ein Filter beantwortete sie fuer einen
 * Mandanten und liesse die uebrigen ungeprueft — und {@code IBISGUS} ist gerade der Mandant mit dem
 * hoechsten Doppelrollen-Anteil (1,278 %, M28‑1c). Der Test greift damit nicht in die
 * Mandantentrennung ein; er liest keine fachlichen Werte, sondern zaehlt Spaltenbelegungen.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class KettenrollenBestandTest {

  /**
   * Fenster B aus {@code messungen-schritt6.md} §0 — der dichte Monat, 214.330 Nachrichten. Absolut
   * und nicht relativ: Ausser {@code NEXANS} endet jeder Mandant am 30.12.2025, ein relatives
   * Fenster haenge damit am Datenstand der Testkopie.
   */
  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-11-30T00:00:00");

  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  /** Eine vorkommende Kombination samt der Zahl der Zeilen, die sie tragen. */
  private Map<Set<Kettenrolle>, Integer> vorkommendeKombinationen() {
    Field<Boolean> istKind = belegt(MESSAGE.SOURCEMESSAGEID).as("ist_kind");
    Field<Boolean> istEingang = belegt(MESSAGE.TARGETMESSAGEID).as("ist_eingang");

    Map<Set<Kettenrolle>, Integer> gefunden = new LinkedHashMap<>();
    for (Record satz :
        glassfishDsl
            .select(MESSAGE.SOURCE, istKind, istEingang, MESSAGE.TARGET, DSL.count())
            .from(MESSAGE)
            .where(MESSAGE.MESSAGELASTUPDATE.ge(FENSTER_VON))
            .and(MESSAGE.MESSAGELASTUPDATE.lt(FENSTER_BIS))
            .groupBy(MESSAGE.SOURCE, istKind, istEingang, MESSAGE.TARGET)
            .fetch()) {
      Set<Kettenrolle> rollen =
          Kettenrollen.aus(
              satz.get(MESSAGE.SOURCE),
              Boolean.TRUE.equals(satz.get(istKind)) ? "belegt" : null,
              Boolean.TRUE.equals(satz.get(istEingang)) ? "belegt" : null,
              satz.get(MESSAGE.TARGET));
      gefunden.merge(rollen, satz.get(DSL.count()), Integer::sum);
    }
    return gefunden;
  }

  /**
   * „Belegt" heisst: nicht {@code null} und nicht leer — dieselbe Regel wie in {@link
   * Kettenrollen}.
   */
  private static Field<Boolean> belegt(Field<String> kennung) {
    Condition bedingung = kennung.isNotNull().and(kennung.ne(""));
    return DSL.field(bedingung);
  }

  @Test
  @DisplayName("Jede vorkommende Rollenkombination hat eine Formulierung")
  void jede_vorkommende_kombination_ist_formuliert() {
    Map<Set<Kettenrolle>, Integer> gefunden = vorkommendeKombinationen();

    assertThat(gefunden)
        .as("ohne Zeilen im Fenster bewiese der Test nur, dass leer leer ist")
        .isNotEmpty();
    assertThat(gefunden.values().stream().mapToInt(Integer::intValue).sum())
        .as("Fenster B hat 214.330 Nachrichten (M23-2 b)")
        .isEqualTo(214_330);

    assertThat(gefunden.keySet())
        .as(
            "Eine Kombination ohne Formulierung bekaeme in der Oberflaeche still ein falsches"
                + " Etikett. Bei einer Menge faengt das kein switch ab — nur dieser Test."
                + " Gefunden: %s",
            gefunden)
        .allSatisfy(
            kombination ->
                assertThat(Kettenrollen.FORMULIERTE_KOMBINATIONEN).contains(kombination));
  }

  /**
   * <b>Nie mehr als zwei Rollen je Zeile</b> — eine Messung (M28‑1c), keine Garantie. Der Test
   * haelt sie fest, statt dass der Code sie voraussetzt.
   */
  @Test
  @DisplayName("Keine Zeile traegt drei oder vier Rollen")
  void keine_zeile_traegt_mehr_als_zwei_rollen() {
    Map<Set<Kettenrolle>, Integer> gefunden = vorkommendeKombinationen();

    assertThat(gefunden.keySet())
        .as("Gefunden: %s", gefunden)
        .allSatisfy(
            kombination ->
                assertThat(kombination.size())
                    .isLessThanOrEqualTo(Kettenrollen.HOECHSTENS_ROLLEN_JE_ZEILE));
  }

  /**
   * Die Gegenprobe zu {@code KettenrollenTest}: Die vier Einzelrollen und mindestens eine
   * Doppelrolle kommen in der Testkopie tatsaechlich vor. Ohne sie pruefte der Test oben eine leere
   * oder triviale Menge und saehe trotzdem gruen aus.
   */
  @Test
  @DisplayName("Alle vier Einzelrollen und mindestens eine Doppelrolle kommen vor")
  void alle_einzelrollen_und_eine_doppelrolle_kommen_vor() {
    Map<Set<Kettenrolle>, Integer> gefunden = vorkommendeKombinationen();

    assertThat(gefunden.keySet())
        .contains(
            Set.of(),
            Set.of(Kettenrolle.SPLIT_WURZEL),
            Set.of(Kettenrolle.SPLIT_KIND),
            Set.of(Kettenrolle.MERGE_EINGANG),
            Set.of(Kettenrolle.MERGE_ERGEBNIS));
    assertThat(gefunden.keySet().stream().filter(rollen -> rollen.size() == 2).toList())
        .as("514 von 214.330 Zeilen tragen zwei Rollen (M28-1c) — in drei Kombinationen")
        .hasSize(3);
  }
}
