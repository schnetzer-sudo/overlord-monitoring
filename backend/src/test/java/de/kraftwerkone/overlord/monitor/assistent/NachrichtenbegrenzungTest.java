package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Hoechstens n Nachrichten je Nutzer, gleitend ueber die letzte Stunde (E‑1036), Uhr gestellt. */
class NachrichtenbegrenzungTest {

  @Test
  @DisplayName("Die n+1-te Nachricht in einer Stunde ist 429; eine Stunde nach der ersten geht es")
  void gleitendes_fenster() {
    Stelluhr uhr = new Stelluhr();
    Nachrichtenbegrenzung begrenzung = new Nachrichtenbegrenzung(uhr, 3);

    begrenzung.pruefeUndZaehle(1);
    uhr.vor(Duration.ofMinutes(10));
    begrenzung.pruefeUndZaehle(1);
    begrenzung.pruefeUndZaehle(1);
    assertThatThrownBy(() -> begrenzung.pruefeUndZaehle(1))
        .isInstanceOfSatisfying(
            FachlicheAusnahme.class,
            fehler -> {
              assertThat(fehler.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
              assertThat(fehler.problemTyp()).isEqualTo("zu-viele-nachrichten");
              assertThat(fehler.zusatz()).containsEntry("hoechstensJeStunde", 3);
            });

    // Ein anderer Nutzer hat sein eigenes Kontingent.
    assertThatCode(() -> begrenzung.pruefeUndZaehle(2)).doesNotThrowAnyException();

    uhr.vor(Duration.ofMinutes(49));
    assertThatThrownBy(() -> begrenzung.pruefeUndZaehle(1)).isInstanceOf(FachlicheAusnahme.class);
    uhr.vor(Duration.ofMinutes(1));
    assertThatCode(() -> begrenzung.pruefeUndZaehle(1)).doesNotThrowAnyException();
    assertThatThrownBy(() -> begrenzung.pruefeUndZaehle(1)).isInstanceOf(FachlicheAusnahme.class);
  }
}
