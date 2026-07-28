package de.kraftwerkone.overlord.monitor.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prueft das einheitliche Fehlerformat (RFC 9457) ohne Datenbank: {@code application/problem+json},
 * eine {@code traceId} und keine internen Details in der Antwort.
 */
class FehlerformatTest {

  private MockMvc mvc;

  @BeforeEach
  void aufsetzen() {
    mvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("Ein technischer Fehler liefert problem+json mit traceId und ohne interne Details")
  void technischer_fehler() throws Exception {
    mvc.perform(get("/test/technisch"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.title").value("Technischer Fehler"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        // Keine internen Details: die Ursachennachricht taucht nicht auf.
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("geheim"))));
  }

  @Test
  @DisplayName("Eine nicht gefundene Ressource liefert 404 mit traceId")
  void nicht_gefunden() throws Exception {
    mvc.perform(get("/test/nichtgefunden"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Nicht gefunden"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @RestController
  static class TestController {

    @GetMapping("/test/technisch")
    String technisch() {
      throw new IllegalStateException("geheim: interne Ursache, darf nie nach aussen");
    }

    @GetMapping("/test/nichtgefunden")
    String nichtGefunden() {
      throw new RessourceNichtGefundenException("Nachricht 42 gehoert einem fremden Mandanten");
    }
  }
}
