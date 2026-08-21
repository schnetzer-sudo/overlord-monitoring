package de.kraftwerkone.overlord.monitor.security;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

/**
 * Der Nachweis, dass sich <b>alle</b> Sitzungen eines Kontos finden lassen — die Eigenschaft, an
 * der der Sitzungsentzug aus Schritt 9a haengt (E7, Messung M81).
 *
 * <p><b>Warum das ein bleibender Test ist und kein einmaliger Blick.</b> Die Eigenschaft ist bis
 * zum 21.08.2026 aus der Spring-Session-Dokumentation <i>gelesen</i> und nie geprueft worden, und
 * {@code V3__spring_session.sql} ist nicht selbst entworfen, sondern aus der Distribution
 * uebernommen. Wenn der Entzug an einer fremden Eigenschaft haengt, gehoert sie bewacht: Faellt der
 * Index, aendert eine Spring-Session-Fassung den Indexnamen, oder landet nach einem Umbau der
 * Anmeldung nichts mehr in {@code PRINCIPAL_NAME}, dann verwirft der Entzug still <b>null</b>
 * Sitzungen und meldet trotzdem Erfolg. Genau dieser Fall wird von keinem Endpunkttest gefunden.
 *
 * <p>Vier Punkte, alle am laufenden System:
 *
 * <ol>
 *   <li><b>a</b> — Traegt {@code SPRING_SESSION} einen Index auf {@code PRINCIPAL_NAME}? Geprueft
 *       gegen {@code information_schema.STATISTICS}, <b>nicht</b> durch Lesen der Migrationsdatei:
 *       Was in der Datei steht, sagt nichts darueber, was in der Datenbank steht.
 *   <li><b>b</b> — Steht nach der Anmeldung ein Wert darin? Die Anmeldung laeuft ueber einen
 *       <b>eigenen Controller</b> statt {@code formLogin} ({@code AuthController}), der Index wird
 *       also nicht von einem Standardfilter gefuellt.
 *   <li><b>c</b> — Ist das Sitzungsrepository als {@link FindByIndexNameSessionRepository}
 *       injizierbar, wo zwei DataSources stehen und <b>keine</b> {@code @Primary} ist?
 *   <li><b>d</b> — Stimmt der Wert zeichengenau mit {@code app_user.username} ueberein, und findet
 *       die Suche die Sitzung auch nach einer Anmeldung mit abweichender Gross- und
 *       Kleinschreibung?
 * </ol>
 */
class SitzungssucheDbIT extends SicherheitsTestbasis {

  private static final String NUTZER = PRAEFIX + "SitzungsFall";
  private static final String PASSWORT = "einLangesPasswort1";

  /**
   * <b>Punkt c ist schon diese Zeile.</b> Scheitert die Aufloesung, laedt der Anwendungskontext
   * nicht und die gesamte Klasse wird rot — das ist der Nachweis, nicht eine Zusicherung im Rumpf
   * einer Methode.
   */
  @Autowired private FindByIndexNameSessionRepository<? extends Session> sitzungsSuche;

  /** Der Schemaname aus derselben Quelle, aus der ihn auch die Anwendung bezieht. */
  @Value("${overlord.db.monitor-schema}")
  private String monitorSchema;

  private Result<Record> indizesAufSpringSession() {
    return monitorDsl.fetch(
        """
        select INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, INDEX_TYPE
          from information_schema.STATISTICS
         where TABLE_SCHEMA = ? and TABLE_NAME = 'SPRING_SESSION'
         order by INDEX_NAME, SEQ_IN_INDEX""",
        monitorSchema);
  }

  private String sortierungVonPrincipalName() {
    return monitorDsl
        .fetchOne(
            """
            select COLLATION_NAME
              from information_schema.COLUMNS
             where TABLE_SCHEMA = ? and TABLE_NAME = 'SPRING_SESSION'
               and COLUMN_NAME = 'PRINCIPAL_NAME'""",
            monitorSchema)
        .get("COLLATION_NAME", String.class);
  }

  private Result<Record> sitzungszeilen(String principalName) {
    return monitorDsl.fetch(
        "select SESSION_ID, PRINCIPAL_NAME from SPRING_SESSION where PRINCIPAL_NAME = ?",
        principalName);
  }

  /**
   * Die Sitzungs-ID, wie sie in {@code SPRING_SESSION.SESSION_ID} und in den Schluesseln von {@code
   * findByPrincipalName} steht.
   *
   * <p><b>Das Cookie traegt sie nicht so.</b> Der {@code DefaultCookieSerializer} von Spring
   * Session kodiert den Wert Base64 — {@code OWMzY2QzNzQt…} im Cookie ist {@code 9c3cd374-…} in der
   * Tabelle. Am 21.08.2026 ist genau daran der erste Lauf dieses Tests gescheitert, und die Zeile
   * steht hier, damit der naechste Leser den Unterschied nicht fuer einen Defekt haelt: Wer die
   * beiden Darstellungen gleichsetzt, vergleicht zwei Zeichenketten, die nie uebereinstimmen
   * koennen.
   */
  private static String rohesitzungsId(Sitzung sitzung) {
    return new String(Base64.getDecoder().decode(sitzung.sitzungsId()), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName(
      "a — SPRING_SESSION traegt einen Index auf PRINCIPAL_NAME (gegen information_schema)")
  void spring_session_traegt_index_auf_principal_name() {
    Result<Record> indizes = indizesAufSpringSession();
    System.out.println(
        "[M81 a] information_schema.STATISTICS, " + monitorSchema + ".SPRING_SESSION");
    System.out.println(indizes);
    System.out.println("[M81 d] PRINCIPAL_NAME COLLATION_NAME = " + sortierungVonPrincipalName());

    assertThat(indizes)
        .as("Ohne Index auf PRINCIPAL_NAME ist der Sitzungsentzug ein voller Tabellendurchlauf")
        .anySatisfy(
            zeile -> {
              assertThat(zeile.get("COLUMN_NAME", String.class)).isEqualTo("PRINCIPAL_NAME");
              assertThat(zeile.get("SEQ_IN_INDEX", Integer.class))
                  .as("PRINCIPAL_NAME muss die erste Spalte des Index sein, nicht die zweite")
                  .isEqualTo(1);
            });
  }

  @Test
  @DisplayName("b — Nach der Anmeldung steht der Benutzername in SPRING_SESSION.PRINCIPAL_NAME")
  void nach_der_anmeldung_steht_ein_wert_in_principal_name()
      throws IOException, InterruptedException {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    anmelden(NUTZER, PASSWORT);

    Result<Record> zeilen = sitzungszeilen(NUTZER);
    System.out.println("[M81 b] SPRING_SESSION nach der Anmeldung ueber AuthController:");
    System.out.println(zeilen);

    assertThat(zeilen)
        .as(
            "Die Anmeldung laeuft ueber einen eigenen Controller statt formLogin — dass der"
                + " Sitzungsindex trotzdem gefuellt wird, ist nicht selbstverstaendlich")
        .hasSize(1);
    assertThat(zeilen.getFirst().get("PRINCIPAL_NAME", String.class)).isEqualTo(NUTZER);
  }

  @Test
  @DisplayName(
      "c — Das Sitzungsrepository ist ein JdbcIndexedSessionRepository und findet ueber den Index")
  void sitzungsrepository_ist_als_findbyindexname_injizierbar()
      throws IOException, InterruptedException {
    System.out.println("[M81 c] injizierter Typ: " + sitzungsSuche.getClass().getName());
    assertThat(sitzungsSuche)
        .as(
            "Zwei DataSources, keine @Primary — dass Spring Session hier trotzdem das indizierte"
                + " Repository verdrahtet, haengt an @SpringSessionDataSource in DataSourceConfig")
        .isInstanceOf(JdbcIndexedSessionRepository.class);

    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung erste = anmelden(NUTZER, PASSWORT);
    Sitzung zweite = anmelden(NUTZER, PASSWORT);

    Map<String, ? extends Session> gefunden = sitzungsSuche.findByPrincipalName(NUTZER);
    System.out.println("[M81 c] findByPrincipalName(\"" + NUTZER + "\") -> " + gefunden.keySet());

    assertThat(gefunden)
        .as("Der Entzug muss ALLE Sitzungen eines Kontos finden, nicht die zuletzt angelegte")
        .hasSize(2)
        .containsKeys(rohesitzungsId(erste), rohesitzungsId(zweite));
  }

  @Test
  @DisplayName("d — Der Wert stimmt zeichengenau mit app_user.username ueberein")
  void principal_name_stimmt_zeichengenau_mit_app_user_username()
      throws IOException, InterruptedException {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    anmelden(NUTZER, PASSWORT);

    String ausAppUser =
        monitorDsl
            .select(APP_USER.USERNAME)
            .from(APP_USER)
            .where(APP_USER.USERNAME.eq(NUTZER))
            .fetchOne(APP_USER.USERNAME);
    String ausSitzung = sitzungszeilen(NUTZER).getFirst().get("PRINCIPAL_NAME", String.class);
    System.out.println(
        "[M81 d] app_user.username = '" + ausAppUser + "', PRINCIPAL_NAME = '" + ausSitzung + "'");

    assertThat(ausSitzung)
        .as(
            "Der Entzug schlaegt app_user.username in SPRING_SESSION nach. Waere das nicht"
                + " derselbe Text, faende er nichts — und meldete trotzdem Erfolg")
        .isEqualTo(ausAppUser);
  }

  @Test
  @DisplayName(
      "d — Gegenprobe: nach Anmeldung mit abweichender Schreibweise wird trotzdem gefunden")
  void anmeldung_mit_abweichender_schreibweise_wird_trotzdem_gefunden()
      throws IOException, InterruptedException {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_A);
    String abweichend = NUTZER.toLowerCase(java.util.Locale.ROOT);
    assertThat(abweichend).isNotEqualTo(NUTZER);

    Sitzung sitzung = anmelden(abweichend, PASSWORT);

    String ausSitzung = sitzungszeilen(NUTZER).getFirst().get("PRINCIPAL_NAME", String.class);
    List<String> ueberKanonisch = List.copyOf(sitzungsSuche.findByPrincipalName(NUTZER).keySet());
    List<String> ueberAbweichend =
        List.copyOf(sitzungsSuche.findByPrincipalName(abweichend).keySet());
    System.out.println(
        "[M81 d] Anmeldung als '"
            + abweichend
            + "' -> PRINCIPAL_NAME = '"
            + ausSitzung
            + "'; findByPrincipalName kanonisch "
            + ueberKanonisch
            + ", abweichend "
            + ueberAbweichend);

    assertThat(ausSitzung)
        .as(
            "Der Wert stammt aus der gefundenen Zeile in app_user und nicht aus der Eingabe —"
                + " sonst haette dieselbe Person je nach Tippweise zwei Namen im Index")
        .isEqualTo(NUTZER);
    assertThat(ueberKanonisch)
        .as("Der Entzug sucht mit app_user.username und muss diese Sitzung finden")
        .containsExactly(rohesitzungsId(sitzung));
    assertThat(ueberAbweichend)
        .as(
            "SPRING_SESSION.PRINCIPAL_NAME traegt utf8mb4_general_ci und vergleicht ohne Ruecksicht"
                + " auf Gross- und Kleinschreibung. Das ist hier erwuenscht — aber es gehoert"
                + " gesehen und nicht angenommen")
        .containsExactly(rohesitzungsId(sitzung));
  }
}
