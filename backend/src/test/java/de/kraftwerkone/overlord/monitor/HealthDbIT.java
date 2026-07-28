package de.kraftwerkone.overlord.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueft, dass {@code /actuator/health} gruen ist und beide Datenbankverbindungen getrennt unter
 * ihren Bean-Namen ausweist. Im dev-Profil sind die Details freigeschaltet. Der Zugriff laeuft
 * ueber den JDK-HttpClient, um keine zusaetzliche Test-Abhaengigkeit einzufuehren.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Tag("db")
class HealthDbIT {

  @Value("${local.server.port}")
  private int port;

  @Test
  @DisplayName("Healthcheck ist gruen und zeigt beide Verbindungen getrennt")
  void health_zeigt_beide_verbindungen() throws Exception {
    HttpResponse<String> antwort =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertThat(antwort.statusCode()).isEqualTo(200);
    String koerper = antwort.body();
    assertThat(koerper).contains("\"status\":\"UP\"");
    // Beide DataSource-Beans erscheinen als eigene Health-Contributoren unter ihren Bean-Namen.
    assertThat(koerper).contains("glassfishDataSource");
    assertThat(koerper).contains("monitorDataSource");
  }
}
