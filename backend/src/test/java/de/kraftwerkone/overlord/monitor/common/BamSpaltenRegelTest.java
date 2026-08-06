package de.kraftwerkone.overlord.monitor.common;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.BamSpaltenRegel.Konfiguriert;
import de.kraftwerkone.overlord.monitor.common.BamSpaltenRegel.Kuratiert;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Auflösungsregel aus {@code docs/datenzugriff.md} §5 — mit den echten Konstellationen der
 * Testkopie (Messung M7) als Beispielen.
 */
class BamSpaltenRegelTest {

  /**
   * {@code NEXANS}: kuratiert auf Lieferschein-Nr. und Abrufnummer, weil der Index danebengreift.
   */
  @Test
  @DisplayName("Die Kuratierung schlaegt den Sortierindex")
  void kuratierung_schlaegt_sortierindex() {
    List<Kuratiert> kuratiert =
        List.of(new Kuratiert(2, (short) 9001), new Kuratiert(1, (short) 9006));
    List<Konfiguriert> konfiguriert =
        List.of(
            new Konfiguriert((short) 9000, (short) 9000),
            new Konfiguriert((short) 9001, (short) 9001),
            new Konfiguriert((short) 9006, (short) 9006));

    assertThat(BamSpaltenRegel.waehle(kuratiert, konfiguriert))
        .as("position 1 zuerst, unabhaengig von der Reihenfolge der Zeilen")
        .containsExactly((short) 9006, (short) 9001);
  }

  @Test
  @DisplayName("Ohne Kuratierung gelten die zwei kleinsten Sortierindizes")
  void ohne_kuratierung_gilt_der_sortierindex() {
    List<Konfiguriert> ibis =
        List.of(
            new Konfiguriert((short) 3, (short) 3),
            new Konfiguriert((short) 0, (short) 0),
            new Konfiguriert((short) 2, (short) 2),
            new Konfiguriert((short) 1, (short) 1));

    assertThat(BamSpaltenRegel.waehle(List.of(), ibis)).containsExactly((short) 0, (short) 1);
  }

  /** {@code VOTG} vergibt den Sortierindex 2002 zweimal — ohne Tiebreaker waere das Zufall. */
  @Test
  @DisplayName("Bei gleichem Sortierindex entscheidet der Typ aufsteigend")
  void gleichstand_entscheidet_der_typ() {
    List<Konfiguriert> votg =
        List.of(
            new Konfiguriert((short) 2011, (short) 2002),
            new Konfiguriert((short) 2002, (short) 2002),
            new Konfiguriert((short) 2003, (short) 2003));

    assertThat(BamSpaltenRegel.waehle(List.of(), votg)).containsExactly((short) 2002, (short) 2011);
  }

  /** {@code ZAST} hat genau einen Typ. */
  @Test
  @DisplayName("Ein einziger Typ ergibt eine einzige Spalte")
  void ein_typ_ergibt_eine_spalte() {
    assertThat(BamSpaltenRegel.waehle(List.of(), List.of(new Konfiguriert((short) 3, (short) 3))))
        .containsExactly((short) 3);
  }

  /** {@code EDITIONLINGERI}, {@code SYSTEM} und {@code WOC} haben keine Konfiguration. */
  @Test
  @DisplayName("Kein Typ ergibt keine Spalte — keine leere, kein Platzhalter")
  void kein_typ_ergibt_keine_spalte() {
    assertThat(BamSpaltenRegel.waehle(List.of(), List.of())).isEmpty();
  }

  @Test
  @DisplayName("Es sind hoechstens zwei Spalten")
  void hoechstens_zwei() {
    List<Konfiguriert> viele =
        List.of(
            new Konfiguriert((short) 1, (short) 1),
            new Konfiguriert((short) 2, (short) 2),
            new Konfiguriert((short) 3, (short) 3));

    assertThat(BamSpaltenRegel.waehle(List.of(), viele)).hasSize(BamSpaltenRegel.HOECHSTENS);
  }

  @Test
  @DisplayName("Ein Typ ohne Beschreibung bekommt seine Nummer, keinen geratenen Klartext")
  void fehlende_beschreibung_wird_nicht_geraten() {
    assertThat(BamSpalte.mitBeschreibung(1, (short) 9006, null).beschreibung())
        .isEqualTo("BAM-Typ 9006");
    assertThat(BamSpalte.mitBeschreibung(1, (short) 9006, "  ").beschreibung())
        .isEqualTo("BAM-Typ 9006");
    assertThat(BamSpalte.mitBeschreibung(1, (short) 9006, "Lieferschein-Nr.").beschreibung())
        .isEqualTo("Lieferschein-Nr.");
  }
}
