package de.kraftwerkone.overlord.monitor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

/**
 * Der Sitzungsentzug ohne Datenbank — die Auswahl, welche Sitzungen fallen.
 *
 * <p><b>Warum das neben {@code SitzungsentzugDbIT} steht.</b> Der Integrationstest beweist, dass
 * der Entzug am laufenden System wirkt. Er beweist <b>nicht</b>, dass genau die richtigen Sitzungen
 * fallen, wenn der Fall knapp wird: {@code findByPrincipalName} liefert bei einem unbekannten Index
 * eine <i>leere</i> Menge statt einer Ausnahme, und ein Entzug, der aus Versehen null oder alle
 * verwirft, sieht von aussen in vielen Faellen gleich aus. Diese drei Faelle sind hier billiger und
 * schaerfer zu pruefen als ueber HTTP.
 */
class SitzungsentzugTest {

  private static final String NUTZER = "lukas";

  private FindByIndexNameSessionRepository<Session> sitzungen;
  private Sitzungsentzug sitzungsentzug;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void aufsetzen() {
    sitzungen = mock(FindByIndexNameSessionRepository.class);
    sitzungsentzug = new Sitzungsentzug(sitzungen);
  }

  private void vorhanden(String... ids) {
    Map<String, Session> gefundene = new LinkedHashMap<>();
    for (String id : ids) {
      gefundene.put(id, new MapSession(id));
    }
    // Gestellt wird findByPrincipalName und nicht findByIndexNameAndIndexValue: Erstere ist eine
    // Default-Methode der Schnittstelle, und ein Mockito-Mock fuehrt Default-Methoden nicht aus.
    when(sitzungen.findByPrincipalName(NUTZER)).thenReturn(gefundene);
  }

  @Test
  @DisplayName("verwirfAlle loescht jede Sitzung des Kontos und zaehlt sie")
  void verwirft_alle() {
    vorhanden("a", "b", "c");

    assertThat(sitzungsentzug.verwirfAlle(NUTZER)).isEqualTo(3);

    verify(sitzungen).deleteById("a");
    verify(sitzungen).deleteById("b");
    verify(sitzungen).deleteById("c");
  }

  @Test
  @DisplayName("verwirfUebrige laesst genau die angegebene Sitzung stehen")
  void verwirft_uebrige_und_behaelt_eine() {
    vorhanden("a", "b", "c");

    assertThat(sitzungsentzug.verwirfUebrige(NUTZER, "b")).isEqualTo(2);

    verify(sitzungen).deleteById("a");
    verify(sitzungen).deleteById("c");
    verify(sitzungen, never()).deleteById("b");
  }

  /**
   * Der Fall, der ein stiller Defekt waere: Wenn der Index nichts liefert, darf nichts geloescht
   * werden — und die Zahl muss {@code 0} sein, damit sie im Protokoll auffaellt.
   */
  @Test
  @DisplayName("Ohne gefundene Sitzung wird nichts geloescht und nichts gemeldet")
  void leere_menge_loescht_nichts() {
    vorhanden();

    assertThat(sitzungsentzug.verwirfAlle(NUTZER)).isZero();
    assertThat(sitzungsentzug.verwirfUebrige(NUTZER, "a")).isZero();

    verify(sitzungen, never()).deleteById(org.mockito.ArgumentMatchers.anyString());
  }
}
