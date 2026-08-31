package de.kraftwerkone.overlord.monitor.rollup;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Wo die Transaktionsklammern des Rollups sitzen — und wo ausdruecklich keine sitzt.</b>
 *
 * <p>Dieser Test ist am 31.08.2026 aus einem Befund entstanden, und der Befund war <b>still</b>:
 * {@code RollupNachzug} sagte in seinem Kopf, in {@code docs/rollup.md} §6a und im Protokolltext
 * fuer den Betreiber, <i>jede Scheibe sei ihre eigene Transaktion</i> — und rief die beiden
 * Ebenenmethoden einzeln auf, von aussen, also im Autocommit. Ein Abbruch zwischen dem {@code
 * DELETE} und dem {@code INSERT … SELECT} der Monatsebene haette den Monat leer stehen lassen,
 * waehrend die Tagesebene ihn vollstaendig traegt. <b>Kein Test war rot, und die Dokumentation
 * behauptete das Gegenteil.</b>
 *
 * <h2>Warum das hier ueber Reflexion geprueft wird und nicht ueber Verhalten</h2>
 *
 * <p>{@code RollupDbIT.keine_ebene_bleibt_bei_einem_abbruch_zurueck} prueft das Verhalten und
 * prueft es richtig — aber es kann eine <b>fehlende</b> Klammer nicht finden: Es bringt seine
 * eigene Transaktion mit, und {@code monitorDsl} haengt ueber {@code
 * TransactionAwareDataSourceProxy} an ihr. Jedes Statement nimmt daran teil, ob die Methode
 * annotiert ist oder nicht. <b>Genau die Luecke, durch die der Befund gefallen ist.</b>
 *
 * <p>Ein Verhaltenstest ohne aeussere Transaktion muesste einen Fehler <i>mitten</i> in {@code
 * ersetzeFenster} erzwingen. Dafuer gibt es keinen Weg, der nicht den Anwendungscode aendert: Alle
 * Werte der abgeleiteten Ebenen stammen aus der jeweils naechstfeineren und koennen deshalb keine
 * Spaltenbreite verletzen. <b>Die Annotation ist hier die pruefbare Ursache</b> — dasselbe
 * Vorgehen, das Regel T1 fuer Laufzeiteigenschaften verlangt.
 */
class RollupTransaktionsgrenzenTest {

  private static Method methode(String name, Class<?>... parameter) {
    try {
      return RollupSchreibRepository.class.getMethod(name, parameter);
    } catch (NoSuchMethodException nichtGefunden) {
      throw new AssertionError(
          "Die Methode " + name + " gibt es nicht mehr. Dieser Test prueft dann nichts.",
          nichtGefunden);
    }
  }

  /**
   * <b>Die beiden Klammern.</b> {@code ersetzeFenster} umfasst den Lauf, {@code
   * rechneAbgeleiteteEbenenNeu} die Scheibe des Rueckwaertslaufs.
   */
  @Test
  @DisplayName("Beide Klammern des Rollups tragen @Transactional")
  void beide_klammern_tragen_transactional() {
    assertThat(
            methode("ersetzeFenster", RollupFenster.class, java.util.List.class)
                .isAnnotationPresent(Transactional.class))
        .as(
            "Ohne diese Klammer koennte ein Lauf die Stundenebene schreiben und die abgeleiteten"
                + " Ebenen nicht — eine halb geleerte Rolluptabelle um 03:00 waere ein leeres"
                + " Dashboard")
        .isTrue();
    assertThat(
            methode(
                    "rechneAbgeleiteteEbenenNeu",
                    RollupFenster.Tagesbereich.class,
                    RollupFenster.Monatsbereich.class)
                .isAnnotationPresent(Transactional.class))
        .as(
            "Ohne diese Klammer waere die Zusicherung „jede Scheibe ist ihre eigene Transaktion\""
                + " aus RollupNachzug und docs/rollup.md §6a nicht umgesetzt — und ein Abbruch"
                + " liesse den Monat leer, waehrend die Tagesebene ihn vollstaendig traegt")
        .isTrue();
  }

  /**
   * <b>Und die beiden Bausteine tragen ausdruecklich keine eigene.</b> Sie sind Teil beider
   * Klammern; eine eigene Transaktion machte aus einer Klammer zwei und hoebe sie damit auf.
   */
  @Test
  @DisplayName("Die beiden Ebenenmethoden tragen kein eigenes @Transactional")
  void die_ebenenmethoden_tragen_keine_eigene() {
    assertThat(
            methode("rechneTageEbeneNeu", RollupFenster.Tagesbereich.class)
                .isAnnotationPresent(Transactional.class))
        .as("Sie ist ein Baustein und macht keine zweite Transaktion auf")
        .isFalse();
    assertThat(
            methode("rechneMonatsEbeneNeu", RollupFenster.Monatsbereich.class)
                .isAnnotationPresent(Transactional.class))
        .as("Sie ist ein Baustein und macht keine zweite Transaktion auf")
        .isFalse();
  }
}
