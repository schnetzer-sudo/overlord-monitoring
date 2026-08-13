package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Normalisierung — <b>ohne Datenbank</b>. Sie ist reine Rechenlogik und genau deshalb hier
 * prüfbar: Die Sollängen entstehen aus einem Vollabzug <i>vor</i> dem Statement, nicht darin.
 *
 * <p><b>Kein Prüfwert dieser Datei stammt aus dem Bestand</b> (Regel G1). Die Ziffernfolgen sind
 * erfunden; beschrieben ist jeweils die <i>Eigenschaft</i>, auf die es ankommt — ihre Länge im
 * Verhältnis zur Sollänge. Die kuratierten Paare selbst (Mandant, Typ, Sollänge) sind
 * Konfigurationsvokabular und stehen offen in {@code docs/bam-sollaengen.md}.
 */
class SollaengenTest {

  private static BamSollaengeZeile mitLaenge(int typ, int sollaenge) {
    return new BamSollaengeZeile((short) typ, sollaenge, false);
  }

  private static BamSollaengeZeile nurLeerzeichen(int typ) {
    return new BamSollaengeZeile((short) typ, null, true);
  }

  /**
   * Die Kuratierung von {@code NEXANS}, Zeile für Zeile wie in {@code V5__bam_sollaenge.sql}:
   * <b>zehn</b> Zeilen, davon acht mit einer Sollänge — aber nur <b>drei verschiedene</b> Längen
   * (10, 4, 3) —, dazu die beiden Zeilen mit Leerzeichen-Kennzeichen und ohne Sollänge.
   *
   * <p>Die Zahlen sind in M47 gegen die Tabelle gezählt und nicht übernommen: Der Auftrag zu Teil
   * 2b spricht von „elf Sollängen".
   */
  private static Sollaengen nexans() {
    return new Sollaengen(
        List.of(
            mitLaenge(9000, 3),
            mitLaenge(9009, 10),
            mitLaenge(9011, 10),
            mitLaenge(9012, 10),
            mitLaenge(9013, 10),
            mitLaenge(9021, 10),
            mitLaenge(9024, 10),
            mitLaenge(9036, 4),
            nurLeerzeichen(9018),
            nurLeerzeichen(9020)));
  }

  private static Suchbegriff ohneTyp(String wert) {
    return new Suchbegriff(null, wert);
  }

  private static Suchbegriff mitTyp(int typ, String wert) {
    return new Suchbegriff((short) typ, wert);
  }

  // ─── Der Normalfall: die führende Null ────────────────────────────────────────

  /**
   * <b>Der Fall, für den es die ganze Kuratierung gibt.</b> Ein wirksames Paar — 100 Prozent
   * Längendominanz und 100 Prozent führende Null (M46‑1) —, eine Eingabe ohne die Nullen.
   */
  @Test
  @DisplayName("Mit Typ gilt die Sollaenge genau dieses Paares")
  void mit_typ_gilt_die_sollaenge_des_paares() {
    Varianten varianten = nexans().fuer(mitTyp(9012, "12345678"));

    assertThat(varianten.gemeldet())
        .as("die Eingabe steht an erster Stelle, die aufgefuellte Fassung dahinter")
        .containsExactly("12345678", "0012345678");
    assertThat(varianten.gesucht())
        .as("ohne Leerzeichen-Kennzeichen wird genau nach diesen beiden gesucht")
        .containsExactlyElementsOf(varianten.gemeldet());
  }

  /**
   * <b>Die Zahl, die der Auftrag belegt haben will.</b> Ohne Typ gelten alle kuratierten Sollängen
   * — aber die <b>verschiedenen</b>, und nur die größeren. Bei {@code NEXANS} sind das für eine
   * siebenstellige Eingabe genau <b>eine</b>: die 10. Die 4 und die 3 entfallen, weil nur
   * aufgefüllt und nie gekürzt wird.
   */
  @Test
  @DisplayName("Ohne Typ entsteht bei NEXANS aus einer siebenstelligen Eingabe genau eine Variante")
  void typlos_bei_nexans_bleibt_klein() {
    Varianten varianten = nexans().fuer(ohneTyp("1234567"));

    assertThat(varianten.gemeldet()).containsExactly("1234567", "0001234567");
    assertThat(varianten.gesucht())
        .as(
            "zehn kuratierte Zeilen, drei verschiedene Sollaengen, zwei davon nicht groesser als"
                + " die Eingabe — und ein Leerzeichen-Kennzeichen")
        .containsExactly("1234567", "0001234567", " 1234567");
  }

  /**
   * <b>Aus den verschiedenen Sollängen bilden, nicht je Typ.</b> Sechs der acht Zeilen tragen die
   * Sollänge 10; sie ergeben <i>eine</i> Variante und nicht sechs.
   */
  @Test
  @DisplayName("Gleiche Sollaengen mehrerer Typen ergeben eine Variante, nicht mehrere")
  void doppelte_sollaengen_ergeben_eine_variante() {
    Varianten varianten = nexans().fuer(ohneTyp("12"));

    assertThat(varianten.gemeldet())
        .as("drei verschiedene Sollaengen — 3, 4 und 10 — und alle drei groesser als zwei Zeichen")
        .containsExactly("12", "012", "0012", "0000000012");
  }

  // ─── Nur auffuellen, nie kuerzen ──────────────────────────────────────────────

  /**
   * <b>Eine Sollänge, die nicht größer ist als die Eingabe, erzeugt keine Variante.</b> Auffüllen
   * macht einen Wert spezifischer; kürzen machte ihn unbrauchbar — M43‑4 misst für kurze Kerne
   * 1.642 Treffer statt der vier richtigen.
   */
  @Test
  @DisplayName("Eine Sollaenge kleiner oder gleich der Eingabelaenge erzeugt keine Variante")
  void sollaenge_nicht_groesser_erzeugt_nichts() {
    Sollaengen kurz = new Sollaengen(List.of(mitLaenge(4711, 4)));

    assertThat(kurz.fuer(mitTyp(4711, "1234")).gemeldet())
        .as("gleich lang: nichts aufzufuellen")
        .containsExactly("1234");
    assertThat(kurz.fuer(mitTyp(4711, "123456")).gemeldet())
        .as("laenger als die Sollaenge: es wird nicht gekuerzt")
        .containsExactly("123456");
  }

  // ─── Der Mandantenschnitt ─────────────────────────────────────────────────────

  /**
   * <b>Derselbe Typ, zwei Mandanten, zwei Sollängen</b> — der Fall, an dem M46‑2 die Typannahme
   * widerlegt hat: Typ 2000 dominiert bei {@code SUTTONS} mit Länge 6 (97,33 %) und bei {@code
   * VOTG} mit Länge 7 (84,06 %).
   */
  @Test
  @DisplayName("Typ 2000 erzeugt bei SUTTONS und VOTG verschiedene Varianten")
  void die_sollaenge_greift_je_mandant() {
    Sollaengen suttons = new Sollaengen(List.of(mitLaenge(2000, 6)));
    Sollaengen votg = new Sollaengen(List.of(mitLaenge(2000, 7)));

    assertThat(suttons.fuer(mitTyp(2000, "12345")).gemeldet()).containsExactly("12345", "012345");
    assertThat(votg.fuer(mitTyp(2000, "12345")).gemeldet()).containsExactly("12345", "0012345");
  }

  // ─── Die NULL-faehige Sollaenge ───────────────────────────────────────────────

  /**
   * <b>{@code sollaenge IS NULL} ist kein Fehlerfall.</b> {@code NEXANS}/9018 trägt ausschließlich
   * das Leerzeichen-Kennzeichen — seine Längendominanz liegt bei 43,61 Prozent und damit weit unter
   * der Schwelle. Ausgerechnet dieser Typ sitzt auf 92,26 Prozent der {@code NEXANS}-Wurzeln (M39):
   * Der naheliegendste Suchtyp trägt die Leerzeichen und keine Sollänge.
   */
  @Test
  @DisplayName("Ein Paar ohne Sollaenge erzeugt die Leerzeichen-Variante und keine aufgefuellte")
  void ohne_sollaenge_nur_das_leerzeichen() {
    Varianten varianten = nexans().fuer(mitTyp(9018, "1234567"));

    assertThat(varianten.gemeldet())
        .as("keine aufgefuellte Fassung — es gibt keine Sollaenge fuer dieses Paar")
        .containsExactly("1234567");
    assertThat(varianten.gesucht())
        .as("gesucht wird trotzdem zusaetzlich mit fuehrendem Leerzeichen")
        .containsExactly("1234567", " 1234567");
  }

  /**
   * <b>Die Leerzeichen-Variante wird gesucht und nicht gemeldet.</b> Sie ist für den Nutzer nicht
   * nachvollziehbar; die führende Null ist es.
   */
  @Test
  @DisplayName("Die Leerzeichen-Variante steht nie in der gemeldeten Liste")
  void leerzeichen_variante_wird_nicht_gemeldet() {
    for (Varianten varianten :
        List.of(
            nexans().fuer(ohneTyp("1234567")),
            nexans().fuer(mitTyp(9018, "1234567")),
            nexans().fuer(mitTyp(9012, "12345678")))) {
      assertThat(varianten.gemeldet()).noneMatch(fassung -> fassung.startsWith(" "));
    }
  }

  /**
   * <b>Ein Typ ohne Kennzeichen bekommt sie nicht</b> — auch nicht, weil ein <i>anderer</i> Typ
   * desselben Mandanten eines trägt.
   */
  @Test
  @DisplayName("Mit Typ gilt das Leerzeichen-Kennzeichen nur fuer dieses Paar")
  void leerzeichen_gilt_nur_fuer_sein_paar() {
    assertThat(nexans().fuer(mitTyp(9012, "12345678")).gesucht())
        .noneMatch(fassung -> fassung.startsWith(" "));
  }

  // ─── Kein Eintrag ─────────────────────────────────────────────────────────────

  /**
   * <b>Ein Mandant ohne Kuratierung sucht roh</b>, und ein Typ ohne Zeile ebenfalls. Von 36 Typen
   * mit führender Null fallen 23 durch die Schwelle (M46‑1) — das ist der Normalfall und kein
   * Mangel.
   */
  @Test
  @DisplayName("Ohne kuratierte Zeile wird ausschliesslich roh gesucht")
  void ohne_kuratierung_nur_roh() {
    assertThat(Sollaengen.keine().fuer(ohneTyp("1234567")).gesucht()).containsExactly("1234567");
    assertThat(nexans().fuer(mitTyp(9006, "12345")).gesucht())
        .as("9006 ist mit 94,21 Prozent durch die Schwelle gefallen und steht nicht in der Tabelle")
        .containsExactly("12345");
  }
}
