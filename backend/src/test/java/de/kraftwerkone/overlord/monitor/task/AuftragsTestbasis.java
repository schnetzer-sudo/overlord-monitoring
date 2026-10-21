package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testfreischaltungen;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.TestPropertySource;

/**
 * Gemeinsame Grundlage der datenbankgebundenen Auftragstests.
 *
 * <p><b>Jeder Test legt sich an, was er braucht</b> (Regel T2): Konten mit dem Praefix {@code it-}
 * aus {@link SicherheitsTestbasis}, Auftraege ueber den Endpunkt unter diesen Konten, Testtypen mit
 * dem Praefix {@code it-} per reinem {@code INSERT}, und seit {@code V103} die Freischaltung von
 * {@code WERK_ABLADESTELLE} fuer {@code NEXANS} und {@code IBIS} ({@link Testfreischaltungen}).
 * Weggeraeumt wird genau das: Auftraege, deren {@code angelegt_von} das Praefix traegt, Typen,
 * deren Kennung es traegt, und die beiden Freischaltungen. Die Zeile {@code WERK_ABLADESTELLE}
 * selbst wird nie angefasst.
 *
 * <p><b>Der Assistent ist eingeschaltet</b>, weil Anlegen und Pruefen nur fuer einen Mandanten mit
 * Assistenten gehen ({@code AssistentFreigabe}, E‑1126). Der Schluessel ist ein Platzhalter, die
 * Basis-URL ein geschlossener Port auf dem eigenen Rechner: Kein Auftragstest ruft das Modell, und
 * geschaehe es doch, ginge nichts hinaus.
 *
 * <p>Laeuft vor der Aufraeumung der Basisklasse (JUnit ruft {@code @AfterEach} der Unterklasse
 * zuerst), die Konten stehen also noch, wenn ihre Auftraege verschwinden.
 */
@TestPropertySource(
    properties = {
      "overlord.assistent.aktiv=true",
      "overlord.assistent.api-key=platzhalter-ohne-wert",
      "overlord.assistent.basis-url=http://127.0.0.1:9"
    })
abstract class AuftragsTestbasis extends SicherheitsTestbasis {

  protected static final String PASSWORT = "einLangesPasswort1";
  protected static final String NEXANS = "NEXANS";
  protected static final String IBIS = "IBIS";
  protected static final String SUTTONS = "SUTTONS";
  protected static final String WERK = Testdefinitionen.WERK;

  private Testfreischaltungen freischaltungen;

  /** Vor dem {@code @BeforeEach} der Unterklasse: Deren Vorbedingungen sehen die Freischaltung. */
  @BeforeEach
  protected void schalteWerkFrei() {
    freischaltungen = new Testfreischaltungen(monitorDsl);
    freischaltungen.schalteFrei(WERK, NEXANS, IBIS);
  }

  @AfterEach
  protected void raeumeAuftraegeWeg() {
    monitorDsl.deleteFrom(TASK).where(TASK.ANGELEGT_VON.like(PRAEFIX + "%")).execute();
    // ON DELETE CASCADE raeumt task_type_mandant mit.
    monitorDsl.deleteFrom(TASK_TYPE).where(TASK_TYPE.TASK_TYP.like(PRAEFIX + "%")).execute();
    if (freischaltungen != null) {
      freischaltungen.raeumeAuf();
    }
  }

  protected static String neueAnfrageId() {
    return UUID.randomUUID().toString();
  }

  /** Der Rumpf von {@code POST /api/tasks}. */
  protected static String auftrag(
      String anfrageId, String typ, String aktion, List<Map<String, String>> eintraege) {
    Map<String, Object> rumpf = new LinkedHashMap<>();
    rumpf.put("anfrageId", anfrageId);
    rumpf.put("typ", typ);
    rumpf.put("aktion", aktion);
    rumpf.put("eintraege", eintraege);
    return Testdefinitionen.JSON.writeValueAsString(rumpf);
  }

  /** Zwei Eintraege einer Neuanlage von {@code WERK_ABLADESTELLE}. */
  protected static String neuanlage(String anfrageId) {
    return auftrag(
        anfrageId,
        WERK,
        "Neuanlage",
        List.of(
            Map.of("werk", "72332", "abladestelle", "DDB0", "sndprn", "120764"),
            Map.of(
                "lieferantennummer",
                "0101159903",
                "werk",
                "22",
                "abladestelle",
                "001N0",
                "sndprn4908",
                "100050")));
  }

  /** Legt eine Neuanlage an und liefert die Task-ID. */
  protected String legeNeuanlageAn(Sitzung sitzung) throws Exception {
    Antwort antwort = sitzung.sende("/api/tasks", neuanlage(neueAnfrageId()));
    assertThat(antwort.status()).as("Anlegen: %s", antwort.rumpf()).isEqualTo(201);
    return antwort.json("$.taskId");
  }
}
