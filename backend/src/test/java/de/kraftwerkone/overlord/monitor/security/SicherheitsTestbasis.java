package de.kraftwerkone.overlord.monitor.security;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Gemeinsame Grundlage der datenbankgebundenen Sicherheitstests.
 *
 * <p><b>Bewusst ein echter Server und ein echter HTTP-Client, kein MockMvc.</b> Ohne eingebetteten
 * Server konfiguriert Spring Boot den {@code CookieSerializer} von Spring Session nicht aus {@code
 * server.servlet.session.cookie.*} — das Sitzungs-Cookie hiesse dann {@code SESSION} statt {@code
 * OVERLORD_SESSION}, und der Test bewiese etwas, das in Produktion anders laeuft. Genau diese Art
 * Abweichung soll ein Test finden und nicht verstecken.
 *
 * <p>Der Client haelt einen eigenen Cookie-Speicher je Sitzung und schickt den {@code XSRF-TOKEN}
 * aus dem Cookie als Header zurueck — exakt das, was der BFF in Teil 2 tun wird.
 *
 * <p><b>Geschrieben wird ausschliesslich in {@code overlord_monitor}.</b> Nach jedem Test
 * verschwinden die angelegten Konten wieder; der Praefix {@link #PRAEFIX} stellt sicher, dass dabei
 * nichts anderes getroffen wird.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Tag("db")
public abstract class SicherheitsTestbasis {

  /** Praefix aller Testkonten. Was damit anfaengt, raeumt der Test wieder weg. */
  protected static final String PRAEFIX = "it-";

  protected static final String SITZUNGSCOOKIE = "OVERLORD_SESSION";
  protected static final String CSRF_COOKIE = "XSRF-TOKEN";
  protected static final String CSRF_HEADER = "X-XSRF-TOKEN";

  /**
   * Zwei Mandanten aus verschiedenen Haeusern. Bewusst <b>nicht</b> {@code NEXANS}/{@code NXHBE}
   * oder {@code IBIS}/{@code IBISGUS}: Zwei Mandanten desselben Konzerns sind ein schlechter Beweis
   * fuer eine Trennung, die zwischen Firmen greifen soll.
   */
  protected static final String MANDANT_A = "VOTG";

  protected static final String MANDANT_B = "SUTTONS";

  @LocalServerPort protected int port;

  @Autowired
  @Qualifier("monitorDsl") protected DSLContext monitorDsl;

  @Autowired protected PasswordEncoder passwortKodierer;
  @Autowired protected MandantRepository mandantRepository;
  @Autowired protected AppUserRepository appUserRepository;
  @Autowired protected AnmeldeSperre anmeldeSperre;

  @Autowired
  @Qualifier("systemClock") protected Clock systemClock;

  /**
   * Alle Tests kommen von derselben Adresse. Ohne dieses Zuruecksetzen liefe die IP-Begrenzung
   * irgendwann voll und ein spaeterer Test schluege aus einem Grund fehl, der nichts mit ihm zu tun
   * hat. Welche Schreibweise der Loopback-Adresse ankommt, haengt am Netzwerkstapel — deshalb
   * beide.
   */
  @BeforeEach
  protected void setzeIpBegrenzungZurueck() {
    anmeldeSperre.zuruecksetzen("127.0.0.1");
    anmeldeSperre.zuruecksetzen("0:0:0:0:0:0:0:1");
  }

  @AfterEach
  protected void raeumeTestkontenWeg() {
    // ON DELETE CASCADE raeumt app_user_mandant mit.
    monitorDsl.deleteFrom(APP_USER).where(APP_USER.USERNAME.like(PRAEFIX + "%")).execute();
    // Und die Sitzungen der Testkonten. Sie liefen zwar von selbst ab, wuerden aber bis dahin in
    // einer gemeinsam genutzten Datenbank stehen. Moeglich ist das nur, weil PRINCIPAL_NAME der
    // Benutzername ist — SPRING_SESSION gehoert Spring Session und ist bewusst nicht im
    // jOOQ-Modell, deshalb rohes SQL.
    monitorDsl.execute("delete from SPRING_SESSION where PRINCIPAL_NAME like ?", PRAEFIX + "%");
  }

  protected LocalDateTime jetztUtc() {
    return LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
  }

  /** Legt ein aktives Konto ohne Aenderungszwang an. */
  protected long legeNutzerAn(String username, String passwort, Rolle rolle, String... mandanten) {
    return legeNutzerAn(username, passwort, rolle, false, mandanten);
  }

  protected long legeNutzerAn(
      String username, String passwort, Rolle rolle, boolean aenderungszwang, String... mandanten) {
    assertThat(username).startsWith(PRAEFIX);
    long id =
        appUserRepository.legeAn(
            username, passwortKodierer.encode(passwort), rolle, true, aenderungszwang, jetztUtc());
    for (String mandant : mandanten) {
      appUserRepository.ordneMandantZu(id, mandant);
    }
    return id;
  }

  /** Eine Antwort des Servers. */
  protected record Antwort(int status, String rumpf, HttpResponse<String> roh) {

    /** Liest einen Wert per JSON-Pfad, etwa {@code $.mandant.id}. */
    public <T> T json(String pfad) {
      return JsonPath.read(rumpf, pfad);
    }

    /** Ob der Pfad im Rumpf vorkommt und nicht {@code null} ist. */
    public boolean hatFeld(String pfad) {
      try {
        return JsonPath.read(rumpf, pfad) != null;
      } catch (RuntimeException ex) {
        return false;
      }
    }

    /** Die {@code traceId} ist je Antwort verschieden und darf einen Vergleich nicht stoeren. */
    public String rumpfOhneTraceId() {
      return rumpf.replaceAll("\"traceId\"\\s*:\\s*\"[^\"]*\"", "\"traceId\":\"-\"");
    }

    public List<String> setCookieHeader() {
      return roh.headers().allValues("set-cookie");
    }
  }

  /** Eine laufende Sitzung: eigener Cookie-Speicher, eigener Client. */
  protected final class Sitzung {

    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient client;

    private Sitzung() {
      this.client = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    /** Der Wert des Sitzungs-Cookies — er muss sich beim Anmelden aendern. */
    public String sitzungsId() {
      return cookieWert(SITZUNGSCOOKIE).orElse(null);
    }

    /** Schiebt dem Client ein Cookie unter — fuer den Nachweis gegen Sitzungsfestschreibung. */
    public void setzeCookie(String name, String wert) {
      HttpCookie cookie = new HttpCookie(name, wert);
      cookie.setPath("/");
      cookie.setDomain("localhost");
      cookies.getCookieStore().add(URI.create("http://localhost"), cookie);
    }

    public Optional<String> cookieWert(String name) {
      return cookies.getCookieStore().getCookies().stream()
          .filter(cookie -> cookie.getName().equals(name))
          .map(HttpCookie::getValue)
          .filter(wert -> !wert.isEmpty())
          .findFirst();
    }

    public Antwort hole(String pfad) throws IOException, InterruptedException {
      return fuehreAus(HttpRequest.newBuilder(uri(pfad)).GET());
    }

    public Antwort sende(String pfad, String json) throws IOException, InterruptedException {
      HttpRequest.Builder anfrage =
          HttpRequest.newBuilder(uri(pfad))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json));
      cookieWert(CSRF_COOKIE).ifPresent(token -> anfrage.header(CSRF_HEADER, token));
      return fuehreAus(anfrage);
    }

    /** Bewusst ohne CSRF-Header — fuer den Nachweis, dass die Pruefung greift. */
    public Antwort sendeOhneCsrf(String pfad, String json)
        throws IOException, InterruptedException {
      return fuehreAus(
          HttpRequest.newBuilder(uri(pfad))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    private Antwort fuehreAus(HttpRequest.Builder anfrage)
        throws IOException, InterruptedException {
      HttpResponse<String> antwort =
          client.send(anfrage.build(), HttpResponse.BodyHandlers.ofString());
      return new Antwort(antwort.statusCode(), antwort.body(), antwort);
    }

    private URI uri(String pfad) {
      return URI.create("http://localhost:" + port + pfad);
    }
  }

  /** Holt sich ein CSRF-Token, ohne angemeldet zu sein — der erste Schritt jedes Aufrufers. */
  protected Sitzung neueSitzung() throws IOException, InterruptedException {
    Sitzung sitzung = new Sitzung();
    sitzung.hole("/api/auth/me");
    assertThat(sitzung.cookieWert(CSRF_COOKIE))
        .as("Ohne CSRF-Token im Cookie kann kein Aufrufer jemals anmelden")
        .isPresent();
    return sitzung;
  }

  /** Meldet an und liefert die laufende Sitzung. */
  protected Sitzung anmelden(String username, String passwort)
      throws IOException, InterruptedException {
    Sitzung sitzung = neueSitzung();
    Antwort antwort = sitzung.sende("/api/auth/login", anmeldung(username, passwort));
    assertThat(antwort.status()).as("Anmeldung fehlgeschlagen: %s", antwort.rumpf()).isEqualTo(200);
    return sitzung;
  }

  protected static String anmeldung(String username, String passwort) {
    return """
        {"username":"%s","password":"%s"}"""
        .formatted(username, passwort);
  }
}
