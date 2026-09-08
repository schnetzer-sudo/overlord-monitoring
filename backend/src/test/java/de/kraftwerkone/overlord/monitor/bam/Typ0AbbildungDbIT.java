package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>Der rote Test zur Abbildung der Typ‑0‑Namen</b> — Bauform wie die Sicherung gegen neue
 * Statuswerte in {@code PROJEKTBESCHREIBUNG.md} §4.1 ({@code
 * DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge}).
 *
 * <p><b>Dieser Test ist der Grund, warum ein unbekannter Typ‑0‑Name still aus dem Angebot fallen
 * darf.</b> {@code SuchfelderService} bietet einen Typ‑0‑Namen nur an, wenn {@link Typ0Feld} ihn
 * kennt — für alle anderen gibt es kein Spaltenprädikat, und ein Angebot ohne Prädikat wäre ein
 * Feld, das nie etwas findet. Ohne diesen Test wäre das Verschwinden unsichtbar: Niemand sähe in
 * der Oberfläche, dass ein konfigurierter Name fehlt. Mit ihm ist es ein roter Lauf mit dem Namen
 * darin — und damit ein Bauauftrag mit einer Zeile.
 *
 * <p>{@code @Tag("db")}: braucht die Konfigurationstabelle der Testkopie und ist in der CI
 * ausgeschlossen. Was ohne Datenbank feststeht, prüft {@code Typ0FeldTest}.
 *
 * <p><b>Kein Prüfwert steht in dieser Datei.</b> Die Namen kommen aus der Tabelle; die Abbildung
 * ist der einzige Sollwert, und sie steht im Code (Regel T2: geprüft wird eine Eigenschaft, keine
 * Zahl aus dem Pflegestand).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class Typ0AbbildungDbIT {

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private List<String> konfigurierteNamenVomTyp(int typ) {
    return glassfishDsl
        .resultQuery(
            """
            select MessagePropertyName
            from MessagePropertySearchListEntry
            where MessagePropertyType = ?
            order by MessagePropertyName
            """,
            typ)
        .fetch(satz -> satz.get(0, String.class));
  }

  /**
   * <b>Die eigentliche Sicherung.</b> Jeder Typ‑0‑Name der Konfiguration hat einen Eintrag in der
   * Abbildung. Taucht einer ohne auf, ist der Test rot und nennt ihn — bis dahin fällt der Name
   * still aus dem Angebot, und genau das darf er nur, weil dieser Test existiert.
   */
  @Test
  @DisplayName("Jeder konfigurierte Typ-0-Name ist in der Abbildung — sonst rot, mit Namen")
  void jeder_typ0_name_ist_abgebildet() {
    List<String> konfiguriert = konfigurierteNamenVomTyp(0);
    assertThat(konfiguriert)
        .as("ohne einen einzigen Typ-0-Namen pruefte dieser Test nichts (M154: acht)")
        .isNotEmpty();

    List<String> unbekannt = new ArrayList<>();
    for (String name : konfiguriert) {
      if (Typ0Feld.fuer(name).isEmpty()) {
        unbekannt.add(name);
      }
    }

    assertThat(unbekannt)
        .as(
            "Typ-0-Namen in MessagePropertySearchListEntry, die Typ0Feld nicht kennt — sie fallen"
                + " bis zur Ergaenzung der Abbildung still aus dem Angebot: %s",
            unbekannt)
        .isEmpty();
  }

  /**
   * <b>Es gibt genau zwei Typen, und beide sind bekannt</b> (M154). Ein dritter Typ oder {@code
   * NULL} wäre eine Zeile, für die weder Spaltenprädikat noch EAV-Zugriff feststeht — der Service
   * ließe sie still weg, und dieser Fall meldet es.
   */
  @Test
  @DisplayName("Jeder Eintrag traegt den Typ 0 oder 1")
  void jeder_eintrag_hat_einen_bekannten_typ() {
    List<Integer> typen =
        glassfishDsl
            .resultQuery("select distinct MessagePropertyType from MessagePropertySearchListEntry")
            .fetch(satz -> satz.get(0, Integer.class));

    assertThat(typen).as("NULL oder ein dritter Typ in der Konfiguration").doesNotContainNull();
    assertThat(Set.copyOf(typen)).isSubsetOf(Set.of(0, 1));
  }

  /**
   * <b>Die Voraussetzung von E‑101, laufend geprüft</b>: Ein abgebildeter Typ‑0‑Name kommt in
   * {@code MessageProperty} nicht vor. M155 hat das für alle acht gemessen (0,455 ms für zwölf
   * {@code EXISTS}-Proben, jede bricht am ersten Treffer ab); hier steht es als Test, damit ein
   * Altsystem, das eines Tages {@code Message.Status} <i>auch</i> als Zeile schreibt, nicht
   * unbemerkt bleibt — die Suche fände dann die Spalte und nie die Zeile.
   *
   * <p>Als {@code EXISTS} je Name und ausdrücklich <b>nicht</b> als {@code DISTINCT} über die
   * Tabelle: Letzteres läse gegen 75,6 Millionen Zeilen an (M155).
   */
  @Test
  @DisplayName("Kein abgebildeter Typ-0-Name kommt als Zeile in MessageProperty vor (E-101)")
  void kein_typ0_name_ist_eine_eav_zeile() {
    List<String> alsZeileVorhanden = new ArrayList<>();
    for (Typ0Feld feld : Typ0Feld.values()) {
      Integer kommtVor =
          glassfishDsl
              .resultQuery(
                  "select exists (select 1 from MessageProperty where MessagePropertyName = ?)",
                  feld.feldname())
              .fetchOne(0, Integer.class);
      if (kommtVor != null && kommtVor != 0) {
        alsZeileVorhanden.add(feld.feldname());
      }
    }

    assertThat(alsZeileVorhanden)
        .as("Typ-0-Namen, die entgegen M155 als EAV-Zeile existieren — E-101 waere damit gekippt")
        .isEmpty();
  }
}
