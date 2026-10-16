package de.kraftwerkone.overlord.monitor.assistent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicEigenschaften;
import de.kraftwerkone.overlord.monitor.assistent.anthropic.AnthropicStub;
import de.kraftwerkone.overlord.monitor.assistent.modell.Modellfehler;
import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypKatalog;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.config.AssistentConfig;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Der Assistent hinter den Endpunkten, ohne Datenbank: der Schalter (E‑1030), die Abbildung der
 * Fehler des Anbieters und die Grenzen je Nachricht. Den Typkatalog ersetzt ein Mock.
 */
class AssistentServiceTest {

  private static final MandantContext NEXANS = new MandantContext("NEXANS");
  private static final AngemeldeterNutzer NUTZER =
      new AngemeldeterNutzer(7L, "it-service", Rolle.MANDANT, false);
  private static final String SITZUNG = "sitzung-1";

  private TypKatalog typKatalog;

  @BeforeEach
  void katalog() {
    typKatalog = mock(TypKatalog.class);
    when(typKatalog.angeboten(any(MandantContext.class)))
        .thenReturn(
            List.of(
                new TypKatalog.Angebot(Testdefinitionen.werk(), Testdefinitionen.werkStruktur())));
  }

  private static AssistentEigenschaften eigenschaften(
      boolean aktiv, String schluessel, String basisUrl) {
    return new AssistentEigenschaften(
        aktiv,
        "anthropic",
        null,
        schluessel,
        basisUrl,
        Duration.ofSeconds(2),
        0,
        50,
        2,
        0,
        0,
        null,
        0);
  }

  private AssistentService service(AssistentEigenschaften eigenschaften, Sprachmodell modell) {
    return service(
        eigenschaften, modell, new Gespraechsspeicher(new Stelluhr(), Duration.ofMinutes(30), 3));
  }

  private AssistentService service(
      AssistentEigenschaften eigenschaften, Sprachmodell modell, Gespraechsspeicher speicher) {
    Stelluhr uhr = new Stelluhr();
    return new AssistentService(
        eigenschaften,
        modell,
        typKatalog,
        speicher,
        new Nachrichtenbegrenzung(uhr, eigenschaften.nachrichtenJeStunde()),
        Skriptmodell.JSON,
        uhr);
  }

  @Nested
  @DisplayName("Schalter aus (E‑1030)")
  class Abgeschaltet {

    private AnthropicStub stub;

    @BeforeEach
    void starte() throws IOException {
      stub = new AnthropicStub();
    }

    @AfterEach
    void stoppe() {
      stub.close();
    }

    /** Wie im Betrieb: Das Modell baut {@code AssistentConfig}, gerichtet auf den Stub. */
    private Sprachmodell ausDerKonfiguration(AssistentEigenschaften eigenschaften) {
      return new AssistentConfig()
          .sprachmodell(eigenschaften, new AnthropicEigenschaften(null, null), Skriptmodell.JSON);
    }

    @Test
    @DisplayName("Schalter aus, Schluessel gesetzt: nicht verfuegbar, kein HTTP-Aufruf")
    void schalter_aus() {
      AssistentEigenschaften aus = eigenschaften(false, "schluessel", stub.basisUrl());
      pruefeAbgeschaltet(aus);
    }

    @Test
    @DisplayName("Schalter an, ohne Schluessel: ebenfalls aus, kein HTTP-Aufruf")
    void ohne_schluessel() {
      AssistentEigenschaften ohneSchluessel = eigenschaften(true, " ", stub.basisUrl());
      pruefeAbgeschaltet(ohneSchluessel);
    }

    private void pruefeAbgeschaltet(AssistentEigenschaften eigenschaften) {
      Sprachmodell modell = ausDerKonfiguration(eigenschaften);
      assertThat(modell).isSameAs(Sprachmodell.ABGESCHALTET);
      AssistentService service = service(eigenschaften, modell);

      assertThat(service.stand(NEXANS, SITZUNG)).isEqualTo(new AssistentResponse(false, List.of()));
      assertThatThrownBy(() -> service.eroeffne(NUTZER, NEXANS, SITZUNG))
          .isInstanceOfSatisfying(
              FachlicheAusnahme.class,
              fehler -> {
                assertThat(fehler.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(fehler.problemTyp()).isEqualTo("assistent-nicht-verfuegbar");
              });
      assertThatThrownBy(
              () -> service.sende(NUTZER, NEXANS, SITZUNG, UUID.randomUUID().toString(), "Hallo"))
          .isInstanceOf(FachlicheAusnahme.class);

      assertThat(stub.anfragen()).as("kein Aufruf beim Anbieter").isEmpty();
      verifyNoInteractions(typKatalog);
    }

    @Test
    @DisplayName("Eingeschaltet baut die Konfiguration den Adapter, und der ruft den Stub")
    void gegenprobe_eingeschaltet() {
      stub.antworte(
          200,
          """
          {"model":"claude-sonnet-5-5","content":[{"type":"text","text":"Guten Tag."}],
           "stop_reason":"end_turn","usage":{"input_tokens":1,"output_tokens":1}}
          """);
      AssistentEigenschaften an = eigenschaften(true, "schluessel", stub.basisUrl());
      AssistentService service = service(an, ausDerKonfiguration(an));

      String id = service.eroeffne(NUTZER, NEXANS, SITZUNG).gespraechId();
      NachrichtResponse antwort = service.sende(NUTZER, NEXANS, SITZUNG, id, "Hallo");

      assertThat(antwort.text()).isEqualTo("Guten Tag.");
      assertThat(stub.anfragen()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Eingeschaltet, mit geskriptetem Modell")
  class Eingeschaltet {

    private Skriptmodell modell;
    private Gespraechsspeicher speicher;
    private AssistentService service;

    /** Nach dem {@code @BeforeEach} der aeusseren Klasse, der den Typkatalog anlegt. */
    @BeforeEach
    void aufbau() {
      modell = new Skriptmodell();
      speicher = new Gespraechsspeicher(new Stelluhr(), Duration.ofMinutes(30), 3);
      service = service(eigenschaften(true, "schluessel", "http://127.0.0.1:9"), modell, speicher);
    }

    private String eroeffne() {
      return service.eroeffne(NUTZER, NEXANS, SITZUNG).gespraechId();
    }

    @Test
    @DisplayName("Voruebergehende Stoerungen sind 503, die uebrigen 502 — ohne Anbieterdetails")
    void fehler_des_anbieters() {
      String id = eroeffne();
      for (Modellfehler.Art art : Modellfehler.Art.values()) {
        modell.fehler(art);
        assertThatThrownBy(() -> service.sende(NUTZER, NEXANS, SITZUNG, id, "Hallo"))
            .as("%s", art)
            .isInstanceOfSatisfying(
                FachlicheAusnahme.class,
                fehler -> {
                  if (art.voruebergehend()) {
                    assertThat(fehler.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(fehler.problemTyp()).isEqualTo("assistent-voruebergehend-gestoert");
                  } else {
                    assertThat(fehler.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(fehler.problemTyp()).isEqualTo("assistent-gestoert");
                  }
                  assertThat(fehler.detail()).doesNotContain("geskripteter", "Anthropic");
                });
      }
      assertThat(
              List.of(
                  Modellfehler.Art.ZEITGRENZE,
                  Modellfehler.Art.NICHT_ERREICHBAR,
                  Modellfehler.Art.UEBERLASTET))
          .allMatch(Modellfehler.Art::voruebergehend);
    }

    @Test
    @DisplayName("Leer oder zu lang: 400 nachricht-ungueltig, ohne Aufruf des Modells")
    void textgrenzen() {
      String id = eroeffne();

      for (String text : List.of("", "   ", "x".repeat(51))) {
        assertThatThrownBy(() -> service.sende(NUTZER, NEXANS, SITZUNG, id, text))
            .isInstanceOfSatisfying(
                FachlicheAusnahme.class,
                fehler -> {
                  assertThat(fehler.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                  assertThat(fehler.problemTyp()).isEqualTo("nachricht-ungueltig");
                  assertThat(fehler.zusatz()).containsEntry("hoechstensZeichen", 50);
                });
      }
      assertThat(modell.anfragen()).isEmpty();
      modell.text("Gern.");
      assertThat(service.sende(NUTZER, NEXANS, SITZUNG, id, "x".repeat(50)).text())
          .isEqualTo("Gern.");
    }

    @Test
    @DisplayName("Nach der Hoechstzahl je Gespraech: 429 gespraech-ausgeschoepft")
    void gespraech_ausgeschoepft() {
      String id = eroeffne();
      modell.text("Eins.").text("Zwei.");
      service.sende(NUTZER, NEXANS, SITZUNG, id, "a");
      service.sende(NUTZER, NEXANS, SITZUNG, id, "b");

      assertThatThrownBy(() -> service.sende(NUTZER, NEXANS, SITZUNG, id, "c"))
          .isInstanceOfSatisfying(
              FachlicheAusnahme.class,
              fehler -> {
                assertThat(fehler.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(fehler.problemTyp()).isEqualTo("gespraech-ausgeschoepft");
              });
    }

    @Test
    @DisplayName("Laeuft schon eine Nachricht: 409")
    void nachricht_laeuft() {
      String id = eroeffne();
      Gespraech gespraech =
          speicher.finde(NEXANS, NUTZER.id(), SITZUNG, UUID.fromString(id)).orElseThrow();
      assertThat(gespraech.beginne()).isTrue();

      assertThatThrownBy(() -> service.sende(NUTZER, NEXANS, SITZUNG, id, "Hallo"))
          .isInstanceOfSatisfying(
              FachlicheAusnahme.class,
              fehler -> assertThat(fehler.status()).isEqualTo(HttpStatus.CONFLICT));
      gespraech.gibFrei();
    }

    @Test
    @DisplayName("Unbekannt, keine UUID, fremder Nutzer oder Mandant: 404, gleich")
    void nicht_gefunden() {
      String id = eroeffne();
      AngemeldeterNutzer anderer = new AngemeldeterNutzer(8L, "it-anderer", Rolle.MANDANT, false);

      for (Runnable zugriff :
          List.<Runnable>of(
              () -> service.sende(NUTZER, NEXANS, SITZUNG, UUID.randomUUID().toString(), "a"),
              () -> service.sende(NUTZER, NEXANS, SITZUNG, "keine-uuid", "a"),
              () -> service.sende(anderer, NEXANS, SITZUNG, id, "a"),
              () -> service.sende(NUTZER, NEXANS, "sitzung-2", id, "a"),
              () -> service.beende(anderer, NEXANS, SITZUNG, id))) {
        assertThatThrownBy(zugriff::run).isInstanceOf(RessourceNichtGefundenException.class);
      }
      assertThat(modell.anfragen()).isEmpty();
      service.beende(NUTZER, NEXANS, SITZUNG, id);
      assertThatThrownBy(() -> service.beende(NUTZER, NEXANS, SITZUNG, id))
          .isInstanceOf(RessourceNichtGefundenException.class);
    }

    @Test
    @DisplayName("Ohne angebotenen Typ: verfuegbar false, Eroeffnen 503")
    void ohne_typ() {
      when(typKatalog.angeboten(any(MandantContext.class))).thenReturn(List.of());

      assertThat(service.stand(NEXANS, SITZUNG).verfuegbar()).isFalse();
      assertThatThrownBy(() -> service.eroeffne(NUTZER, NEXANS, SITZUNG))
          .isInstanceOfSatisfying(
              FachlicheAusnahme.class,
              fehler -> assertThat(fehler.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
  }
}
