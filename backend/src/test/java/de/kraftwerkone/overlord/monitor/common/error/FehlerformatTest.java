package de.kraftwerkone.overlord.monitor.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prueft das einheitliche Fehlerformat (RFC 9457) ohne Datenbank: {@code application/problem+json},
 * eine {@code traceId}, ein stabiles {@code type} und keine internen Details in der Antwort.
 */
class FehlerformatTest {

  /** Praefix jeder Problemtyp-URI dieser Anwendung. */
  private static final String TYP_BASIS = "https://overlord.kraftwerkone.de/probleme/";

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
        .andExpect(jsonPath("$.type").value(TYP_BASIS + "technischer-fehler"))
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
        .andExpect(jsonPath("$.type").value(TYP_BASIS + "nicht-gefunden"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  /**
   * Auch die Faelle, die Spring selbst beantwortet, tragen einen Schluessel.
   *
   * <p>Ohne ihn stuende dort {@code about:blank}, und die Oberflaeche haette nichts zum Uebersetzen
   * — sie uebersetzt anhand des {@code type}, nicht anhand des deutschen {@code detail}.
   */
  @Test
  @DisplayName("Eine von Spring beantwortete Ausnahme traegt ebenfalls einen stabilen Problemtyp")
  void rueckfall_typ() throws Exception {
    mvc.perform(post("/test/nurGet"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.type").value(TYP_BASIS + "anfrage-ungueltig"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  /**
   * Ein abgewiesener CSRF-Token ist kein Berechtigungsproblem.
   *
   * <p>Beide Faelle sind {@code 403} und {@code CsrfException} ist eine {@code
   * AccessDeniedException} — mit einem gemeinsamen Problemtyp koennte die Oberflaeche nicht
   * unterscheiden, ob sie den Token nachholen und erneut senden soll oder ob jeder weitere Versuch
   * sinnlos ist. Die beiden folgenden Tests halten genau diesen Unterschied fest.
   */
  @Test
  @DisplayName("Ein abgewiesener CSRF-Token liefert 403 mit eigenem Problemtyp")
  void csrf_token_ungueltig() throws Exception {
    mvc.perform(post("/test/csrf"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.title").value("Anfrage nicht angenommen"))
        .andExpect(jsonPath("$.type").value(TYP_BASIS + "csrf-token-ungueltig"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        // Unspezifisch (Regel A3): Die Meldung sagt, dass erneut gesendet werden muss — nicht,
        // ob der Token fehlte, abgelaufen war oder nicht passte.
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Token"))));
  }

  @Test
  @DisplayName("Eine nicht ausreichende Rolle behaelt zugriff-verweigert")
  void zugriff_verweigert() throws Exception {
    mvc.perform(get("/test/rolle"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.title").value("Zugriff verweigert"))
        .andExpect(jsonPath("$.type").value(TYP_BASIS + "zugriff-verweigert"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  @Test
  @DisplayName("Die beiden 403-Faelle liefern verschiedene type-Werte")
  void csrf_und_rolle_sind_unterscheidbar() throws Exception {
    String csrf = typVon(mvc.perform(post("/test/csrf")).andReturn());
    String rolle = typVon(mvc.perform(get("/test/rolle")).andReturn());

    assertThat(csrf)
        .as(
            "Gleicher Status, entgegengesetzte Handlung: Der Aufrufer muss am type erkennen"
                + " koennen, ob ein erneuter Versuch Sinn hat.")
        .isNotEqualTo(rolle);
  }

  private static String typVon(MvcResult ergebnis) throws Exception {
    return JsonPath.read(ergebnis.getResponse().getContentAsString(), "$.type");
  }

  @RestController
  static class TestController {

    @GetMapping("/test/technisch")
    String technisch() {
      throw new IllegalStateException("geheim: interne Ursache, darf nie nach aussen");
    }

    /** Wirft, was {@code CsrfFilter} bei fehlendem Token wirft. */
    @PostMapping("/test/csrf")
    String csrf() {
      throw new MissingCsrfTokenException("kein Token in der Anfrage");
    }

    /** Wirft, was die Autorisierung bei nicht ausreichender Rolle wirft. */
    @GetMapping("/test/rolle")
    String rolle() {
      throw new AuthorizationDeniedException("Rolle ADMIN erforderlich");
    }

    @GetMapping("/test/nichtgefunden")
    String nichtGefunden() {
      throw new RessourceNichtGefundenException("Nachricht 42 gehoert einem fremden Mandanten");
    }

    @GetMapping("/test/nurGet")
    String nurGet() {
      return "ok";
    }
  }
}
