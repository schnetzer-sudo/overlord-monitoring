package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Heuristik, geprueft <b>ohne Datenbank</b> — sie ist eine reine Funktion, und genau deshalb
 * laesst sie sich so pruefen.
 *
 * <p>Alle Gestalten stammen aus {@code docs/prozess-katalog.md} und {@code messungen-schritt9.md};
 * die Partnernamen darin stehen bereits im Repository. Erfundene Werte sind als solche erkennbar
 * (Regel G1).
 *
 * <p><b>Der Schwerpunkt liegt auf den Negativfaellen.</b> Eine Heuristik, die zu viel liefert, ist
 * schlimmer als eine, die zu wenig liefert: Nach Regel Q4 ist ein leeres Feld ein gueltiges
 * Ergebnis, ein geratener Partner dagegen eine falsche Auskunft, die niemand mehr als Vermutung
 * erkennt.
 */
class PartnerheuristikTest {

  private static final String NEXANS = "NEXANS";
  private static final String VOTG = "VOTG";
  private static final String IBIS = "IBIS";

  // ─── Regel A — Praefix und Position ──────────────────────────────────────────

  @Test
  @DisplayName("Regel A: Token 2 ist der Partner, bei drei Unterstrichen")
  void regel_a_drei_unterstriche() {
    assertThat(Partnerheuristik.regelA(NEXANS, "40000_AMG_LAB_VDA")).contains("AMG");
    assertThat(Partnerheuristik.regelA(VOTG, "19900_BAYER_4711_INVOICE")).contains("BAYER");
    assertThat(Partnerheuristik.regelA(VOTG, "19900_BAYER_4711_INVOICEATT")).contains("BAYER");
  }

  @Test
  @DisplayName("Regel A: Token 2 ist der Partner, auch bei zwei Unterstrichen")
  void regel_a_zwei_unterstriche() {
    // Die Gestalt <Nr>_<PARTNER>_<Nachrichtenart> der VOTG-Partnerprojekte.
    assertThat(Partnerheuristik.regelA(VOTG, "100_BAYER_ORDERS")).contains("BAYER");
    // Und die neun NEXANS-Prozesse dieser Gestalt. docs/prozess-katalog.md §3.5 haelt
    // ausdruecklich fest, dass die Zuordnung sich dort auf die Form stuetzt und nicht auf einen
    // Befund — dieser Test bildet die Regel ab, nicht die ungeprueften neun Namen.
    assertThat(Partnerheuristik.regelA(NEXANS, "40000_AMG_VDA")).contains("AMG");
  }

  @Test
  @DisplayName("Regel A feuert nicht bei einem Unterstrich — es gaebe kein Token 2")
  void regel_a_ein_unterstrich() {
    assertThat(Partnerheuristik.regelA(VOTG, "00000_NONBASFRouter")).isEmpty();
    assertThat(Partnerheuristik.regelA(VOTG, "00000_XMLSplitter")).isEmpty();
  }

  @Test
  @DisplayName("Regel A feuert nicht bei vier oder fuenf Unterstrichen — mehrteilige Partner")
  void regel_a_vier_und_fuenf_unterstriche() {
    // Genau der Fall, vor dem PROJEKTBESCHREIBUNG.md §4.4 warnt: Es ist unentscheidbar, wo der
    // Partnername endet. "KE" statt "KE_OSTROV" waere ein geratener Wert.
    assertThat(Partnerheuristik.regelA(NEXANS, "40000_KE_OSTROV_LAB_VDA")).isEmpty();
    assertThat(Partnerheuristik.regelA(NEXANS, "40000_DAS_DRAEXLMAIER_LAB_VDA")).isEmpty();
    assertThat(Partnerheuristik.regelA(NEXANS, "40000_DELFINGEN_DE_HA_LAB_VDA")).isEmpty();
  }

  @Test
  @DisplayName("Regel A feuert nicht ohne Nummernpraefix — sonst traefe sie NXHBE und WOC")
  void regel_a_ohne_nummernpraefix() {
    // NXHBE traegt bei 14 von 17 Prozessen genau zwei Unterstriche und hat keinen Praefix (M75).
    // docs/prozess-katalog.md §3.5 fuehrt alle 17 unter "ohne Vorschlag" — ohne diese Bedingung
    // bekaemen 14 davon einen.
    assertThat(Partnerheuristik.regelA("NXHBE", "ABC_DEF_GHI")).isEmpty();
    assertThat(Partnerheuristik.regelA("WOC", "Lager_Bestand_Meldung")).isEmpty();
    // Auch ein Praefix, der nicht am Anfang steht, zaehlt nicht.
    assertThat(Partnerheuristik.regelA(NEXANS, "AB_40000_LAB_VDA")).isEmpty();
  }

  @Test
  @DisplayName("SUTTONS ist namentlich ausgenommen, obwohl das Muster passt")
  void regel_a_suttons_ausgenommen() {
    // Siebzehn Prozesse mit perfektem Muster: Der Prozessname traegt einen Vorgang (Buchung,
    // Status) und keinen Partner (M76).
    assertThat(Partnerheuristik.regelA("SUTTONS", "100_ABC_BUCHUNG")).isEmpty();
    assertThat(Partnerheuristik.regelA("SUTTONS", "100_ABC_STATUS_EDI")).isEmpty();
    // Gegenprobe: dieselbe Gestalt bei einem anderen Mandanten feuert sehr wohl.
    assertThat(Partnerheuristik.regelA(VOTG, "100_ABC_BUCHUNG")).contains("ABC");
    // Und die Ausnahme haengt am Mandanten, nicht an der Zeichenkette im Prozessnamen.
    assertThat(Partnerheuristik.regelA(VOTG, "100_SUTTONS_ORDERS")).contains("SUTTONS");
  }

  @Test
  @DisplayName("Regel A auf leerer und fehlender Eingabe liefert nichts")
  void regel_a_leere_eingabe() {
    assertThat(Partnerheuristik.regelA(NEXANS, null)).isEmpty();
    assertThat(Partnerheuristik.regelA(NEXANS, "")).isEmpty();
    assertThat(Partnerheuristik.regelA(NEXANS, "   ")).isEmpty();
    assertThat(Partnerheuristik.regelA(null, "40000_AMG_LAB_VDA")).contains("AMG");
    // Ein leeres Token 2 ist kein Partner.
    assertThat(Partnerheuristik.regelA(NEXANS, "40000__LAB_VDA")).isEmpty();
  }

  // ─── Regel B — CamelCase mit Richtungsanker ──────────────────────────────────

  @Test
  @DisplayName("Regel B: was hinter dem Anker steht, ist der Partner")
  void regel_b_partner_hinter_dem_anker() {
    Partnervorschlag ausgehend =
        Partnerheuristik.vorschlag(IBIS, "AuslagerungAusgehendBAYER", null);
    assertThat(ausgehend.partner()).isEqualTo("BAYER");
    assertThat(ausgehend.richtung()).isEqualTo(Richtung.AUSGEHEND);
    assertThat(ausgehend.herkunft()).isEqualTo(VorschlagHerkunft.REGEL_B);

    Partnervorschlag eingehend =
        Partnerheuristik.vorschlag(IBIS, "InternRoutingEingehendBAYER", null);
    assertThat(eingehend.partner()).isEqualTo("BAYER");
    assertThat(eingehend.richtung()).isEqualTo(Richtung.EINGEHEND);
    assertThat(eingehend.herkunft()).isEqualTo(VorschlagHerkunft.REGEL_B);
  }

  @Test
  @DisplayName("Regel B feuert auch mit einem Unterstrich im Namen — der Anker ist die Bedingung")
  void regel_b_mit_trennzeichen() {
    // docs/prozess-katalog.md §3.5 zaehlt fuer Regel B IBIS 192 und IBISGUS 89, waehrend M75 dort
    // 188 bzw. 88 Prozesse OHNE Unterstrich findet. Die fuenf mit Unterstrich sind mitgezaehlt.
    Partnervorschlag vorschlag =
        Partnerheuristik.vorschlag(IBIS, "Bestand_AuslagerungAusgehendBAYER", null);
    assertThat(vorschlag.partner()).isEqualTo("BAYER");
    assertThat(vorschlag.herkunft()).isEqualTo(VorschlagHerkunft.REGEL_B);
  }

  @Test
  @DisplayName("Fehlt der Anker, gibt es keinen Vorschlag — weder Partner noch Richtung")
  void regel_b_ohne_anker() {
    Partnervorschlag vorschlag = Partnerheuristik.vorschlag(IBIS, "AuslagerungOhneHinweis", null);
    assertThat(vorschlag.partner()).isNull();
    assertThat(vorschlag.richtung()).isNull();
    assertThat(vorschlag.herkunft()).isEqualTo(VorschlagHerkunft.KEINE);
    assertThat(vorschlag).isEqualTo(Partnervorschlag.KEINER);
  }

  @Test
  @DisplayName("Der Anker gilt als Wort: Eingehende ist kein Eingehend")
  void regel_b_anker_ist_ein_wort() {
    // Das Folgezeichen gehoert noch zum selben Wort. Was "dahinter steht", waere dann geraten.
    assertThat(Partnerheuristik.richtungAus("LagerEingehendeMeldung")).isEmpty();
    assertThat(Partnerheuristik.richtungAus("AusgehendeSammlung")).isEmpty();
    // Grossschreibung ist nicht CamelCase und wird nicht mitgenommen. Im Bestand kommt sie kein
    // einziges Mal vor (gemessen 20.08.2026: null Namen mit abweichender Schreibweise).
    assertThat(Partnerheuristik.richtungAus("AUSLAGERUNGAUSGEHENDBAYER")).isEmpty();
  }

  @Test
  @DisplayName("Ein Grossbuchstabe unmittelbar vor dem Anker ist eine Wortgrenze, keine Sperre")
  void regel_b_anker_hinter_einem_kuerzel() {
    // KORRIGIERT 20.08.2026 an den Daten. Hier stand die Gegenprobe, dass "XEingehendY" NICHT
    // trifft — eine Erfindung, die 26 Prozesse gekostet hat: IBIS 20 und IBISGUS 6 tragen genau
    // diese Gestalt, ein zweibuchstabiges Kuerzel unmittelbar vor dem Anker. Die Vorgabe sagt "an
    // Grossbuchstaben zerlegen", und dann ist das E von Eingehend ein Wortanfang wie jeder andere.
    Partnervorschlag vorschlag =
        Partnerheuristik.vorschlag(IBIS, "VerarbeitungNLEingehendBAYER", null);
    assertThat(vorschlag.partner()).isEqualTo("BAYER");
    assertThat(vorschlag.richtung()).isEqualTo(Richtung.EINGEHEND);
    assertThat(vorschlag.herkunft()).isEqualTo(VorschlagHerkunft.REGEL_B);
    assertThat(Partnerheuristik.richtungAus("300_KundenNLAusgehend")).contains(Richtung.AUSGEHEND);
  }

  @Test
  @DisplayName("Steht hinter dem Anker nichts, gibt es eine Richtung und keinen Partner")
  void regel_b_anker_am_ende() {
    Partnervorschlag vorschlag = Partnerheuristik.vorschlag(IBIS, "AuslagerungAusgehend", null);
    assertThat(vorschlag.richtung()).isEqualTo(Richtung.AUSGEHEND);
    assertThat(vorschlag.partner()).isNull();
    assertThat(vorschlag.herkunft())
        .as("Die Herkunft beschreibt den Partner — und den gibt es hier nicht")
        .isEqualTo(VorschlagHerkunft.KEINE);
  }

  @Test
  @DisplayName("Ein fuehrender Trenner gehoert zur Zeichensetzung und nicht zum Partnernamen")
  void regel_b_fuehrender_trenner() {
    Partnervorschlag vorschlag =
        Partnerheuristik.vorschlag(IBIS, "AuslagerungAusgehend_BAYER", null);
    assertThat(vorschlag.partner()).isEqualTo("BAYER");
  }

  @Test
  @DisplayName("Regel B auf leerer und fehlender Eingabe liefert nichts")
  void regel_b_leere_eingabe() {
    assertThat(Partnerheuristik.richtungAus(null)).isEmpty();
    assertThat(Partnerheuristik.richtungAus("")).isEmpty();
    assertThat(Partnerheuristik.richtungAus("   ")).isEmpty();
    assertThat(Partnerheuristik.vorschlag(IBIS, null, null)).isEqualTo(Partnervorschlag.KEINER);
    assertThat(Partnerheuristik.vorschlag(null, null, null)).isEqualTo(Partnervorschlag.KEINER);
  }

  // ─── Die Richtung aus dem Projektnamen ───────────────────────────────────────

  @Test
  @DisplayName("Derselbe Anker, auf die Projektkennung angewandt")
  void richtung_aus_dem_projektnamen() {
    assertThat(Partnerheuristik.richtungAus("300_KundenEingehend")).contains(Richtung.EINGEHEND);
    assertThat(Partnerheuristik.richtungAus("300_LieferantenAusgehend"))
        .contains(Richtung.AUSGEHEND);
    assertThat(Partnerheuristik.richtungAus("OrdersVerarbeitungEingehendNL"))
        .contains(Richtung.EINGEHEND);
    assertThat(Partnerheuristik.richtungAus("RechnungsVerarbeitungAusgehendDE"))
        .contains(Richtung.AUSGEHEND);
  }

  @Test
  @DisplayName("Projektnamen ohne Anker liefern keine Richtung — VOTG bleibt Handarbeit")
  void richtung_ohne_anker_im_projektnamen() {
    // VOTG ist damit der einzige Mandant, dessen Richtung von Hand kommt — 39 Projekte (§3.4).
    assertThat(Partnerheuristik.richtungAus("100_VTG_BAYER")).isEmpty();
    assertThat(Partnerheuristik.richtungAus("110_VTG_SalesInvoice")).isEmpty();
    assertThat(Partnerheuristik.richtungAus("LagerBAYER")).isEmpty();
  }

  // ─── Das Zusammenspiel ───────────────────────────────────────────────────────

  @Test
  @DisplayName("NEXANS: Partner aus dem Prozess, Richtung aus dem Projekt")
  void zusammenspiel_nexans() {
    Partnervorschlag vorschlag =
        Partnerheuristik.vorschlag(NEXANS, "40000_AMG_LAB_VDA", "300_KundenEingehend");
    assertThat(vorschlag.partner()).isEqualTo("AMG");
    assertThat(vorschlag.richtung()).isEqualTo(Richtung.EINGEHEND);
    assertThat(vorschlag.herkunft()).isEqualTo(VorschlagHerkunft.REGEL_A);
  }

  @Test
  @DisplayName("Kein Partner und trotzdem eine Richtung — die 224 NEXANS-Prozesse")
  void zusammenspiel_ohne_partner_mit_richtung() {
    // Vier Unterstriche: Regel A faellt aus, das Projekt traegt die Richtung trotzdem.
    Partnervorschlag vorschlag =
        Partnerheuristik.vorschlag(NEXANS, "40000_KE_OSTROV_LAB_VDA", "300_KundenEingehend");
    assertThat(vorschlag.partner()).isNull();
    assertThat(vorschlag.richtung()).isEqualTo(Richtung.EINGEHEND);
    assertThat(vorschlag.herkunft())
        .as("KEINE zaehlt den Partner, nicht die Richtung — so zaehlt auch §3.5")
        .isEqualTo(VorschlagHerkunft.KEINE);
  }

  @Test
  @DisplayName("Die Prozesskennung schlaegt die Projektkennung bei der Richtung")
  void zusammenspiel_prozess_schlaegt_projekt() {
    // Der Prozess sagt ausgehend, das Projekt eingehend. Der naeher am Vorgang stehende Name gilt.
    Partnervorschlag vorschlag =
        Partnerheuristik.vorschlag(
            IBIS, "AuslagerungAusgehendBAYER", "OrdersVerarbeitungEingehendNL");
    assertThat(vorschlag.richtung()).isEqualTo(Richtung.AUSGEHEND);
    assertThat(vorschlag.partner()).isEqualTo("BAYER");
  }

  @Test
  @DisplayName("SUTTONS bekommt aus beiden Regeln nichts")
  void zusammenspiel_suttons() {
    Partnervorschlag vorschlag =
        Partnerheuristik.vorschlag("SUTTONS", "100_ABC_BUCHUNG", "100_SUTTONS_ABC");
    assertThat(vorschlag).isEqualTo(Partnervorschlag.KEINER);
  }

  @Test
  @DisplayName("Ein Partner ohne Herkunft ist nicht baubar — Regel Q4 im Typ")
  void partner_ohne_herkunft_ist_unmoeglich() {
    assertThatThrownBy(() -> new Partnervorschlag("BAYER", null, VorschlagHerkunft.KEINE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Q4");
    assertThatThrownBy(() -> new Partnervorschlag(null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
