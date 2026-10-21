package de.kraftwerkone.overlord.monitor.assistent;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.assistent.modell.Sprachmodell;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testfreischaltungen;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * <b>Start ohne Schluessel</b> ({@code docs/tasks.md} §3.9, {@code docs/betrieb.md} §3): Der
 * Kontext startet wie sonst, und der Assistent ist fuer <b>alle</b> Mandanten aus — auch fuer
 * einen, der freigeschaltet ist. Keine Pflicht-Umgebungsvariable kommt mit 1.1.0 dazu.
 *
 * <p><b>Der Schalter ist an, der Schluessel ausdruecklich leer.</b> Damit ist der Test unabhaengig
 * davon, ob in der Umgebung ein Schluessel steht: Die Eigenschaft hier geht der aus {@code
 * application.yml} vor. Ohne Schluessel baut {@code AssistentConfig} keinen Adapter, sondern {@link
 * Sprachmodell#ABGESCHALTET}; ein Aufruf beim Anbieter ist damit nicht einmal moeglich.
 *
 * <p><b>Was bleibt:</b> Liste und Typkatalog antworten weiter — was der Mandant beauftragt hat,
 * sieht er auch ohne Assistenten.
 */
@TestPropertySource(
    properties = {
      "overlord.assistent.aktiv=true",
      "overlord.assistent.api-key=",
      "overlord.assistent.basis-url=http://127.0.0.1:9"
    })
class AssistentOhneSchluesselDbIT extends SicherheitsTestbasis {

  private static final String PASSWORT = "einLangesPasswort1";
  private static final String NEXANS = "NEXANS";
  private static final String WERK = Testdefinitionen.WERK;
  private static final String NUTZER = PRAEFIX + "assistent-ohne-schluessel";

  private static final String ANPASSUNG =
      "{\"typ\":\""
          + WERK
          + "\",\"aktion\":\"Anpassung\",\"eintraege\":[{\"werk\":\"010\",\"abladestelle\":"
          + "\"18VZE\",\"sndprn\":\"123456\"}]}";

  @Autowired private Sprachmodell sprachmodell;

  private Testfreischaltungen freischaltungen;
  private Sitzung sitzung;

  @BeforeEach
  void freischaltenUndAnmelden() throws Exception {
    freischaltungen = new Testfreischaltungen(monitorDsl);
    freischaltungen.schalteFrei(WERK, NEXANS);
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, NEXANS);
    sitzung = anmelden(NUTZER, PASSWORT);
  }

  @AfterEach
  void raeumeAuf() {
    monitorDsl.deleteFrom(TASK).where(TASK.ANGELEGT_VON.eq(NUTZER)).execute();
    if (freischaltungen != null) {
      freischaltungen.raeumeAuf();
    }
  }

  @Test
  @DisplayName(
      "Ohne Schluessel: kein Adapter, verfuegbar false trotz Freischaltung, Gespraeche, Pruefen und"
          + " Anlegen 404; Liste und Typkatalog antworten")
  void ohne_schluessel_fuer_alle_aus() throws Exception {
    assertThat(sprachmodell).isSameAs(Sprachmodell.ABGESCHALTET);

    Antwort stand = sitzung.hole("/api/assistent");
    assertThat(stand.status()).isEqualTo(200);
    assertThat(stand.rumpf()).isEqualTo("{\"verfuegbar\":false,\"typen\":[]}");

    String anlage = ANPASSUNG.replaceFirst("\\{", "{\"anfrageId\":\"" + UUID.randomUUID() + "\",");
    for (Antwort gesperrt :
        List.of(
            sitzung.sende("/api/assistent/gespraeche", ""),
            sitzung.sende(
                "/api/assistent/gespraeche/" + UUID.randomUUID() + "/nachrichten",
                "{\"text\":\"Hallo\"}"),
            sitzung.sende("/api/tasks/pruefung", ANPASSUNG),
            sitzung.sende("/api/tasks", anlage))) {
      assertThat(gesperrt.status()).as(gesperrt.rumpf()).isEqualTo(404);
      assertThat(gesperrt.<String>json("$.type")).endsWith("/nicht-gefunden");
    }
    assertThat(monitorDsl.fetchCount(TASK, TASK.ANGELEGT_VON.eq(NUTZER))).isZero();

    assertThat(sitzung.hole("/api/tasks").status()).isEqualTo(200);
    Antwort typen = sitzung.hole("/api/tasks/typen");
    assertThat(typen.status()).isEqualTo(200);
    assertThat(typen.<List<String>>json("$[*].typ")).contains(WERK);
  }
}
