package de.kraftwerkone.overlord.monitor.security;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER_MANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.AUDIT_LOG;

import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zugriff auf {@code overlord_monitor.app_user} und {@code app_user_mandant} — das <b>eigene</b>
 * Schema, deshalb ueber {@code monitorDsl} und deshalb schreibend.
 *
 * <p>Hier gilt Regel M2 <b>nicht</b>: Diese Klasse fasst {@code jooq.glassfish} nicht an. Ein
 * {@link MandantContext} waere hier auch inhaltlich falsch — ein Konto ist keinem Mandanten
 * unterworfen, es <i>traegt</i> seine Mandanten.
 *
 * <p>Die Zeitpunkte kommen von aussen und sind immer UTC aus der <b>Systemuhr</b>. Sperrfristen
 * duerfen niemals mit der Anwendungsuhr rechnen, die im Dev-Profil um Wochen zurueckversetzt ist.
 */
@Repository
public class AppUserRepository {

  private final DSLContext monitorDsl;

  AppUserRepository(@Qualifier("monitorDsl") DSLContext monitorDsl) {
    this.monitorDsl = monitorDsl;
  }

  public Optional<AppUserZeile> findeNachBenutzername(String username) {
    return monitorDsl
        .select(
            APP_USER.ID,
            APP_USER.USERNAME,
            APP_USER.PASSWORD_HASH,
            APP_USER.ROLE,
            APP_USER.ENABLED,
            APP_USER.MUST_CHANGE_PASSWORD,
            APP_USER.LOCKED_BY_ADMIN,
            APP_USER.FAILED_ATTEMPTS,
            APP_USER.LOCKED_UNTIL)
        .from(APP_USER)
        .where(APP_USER.USERNAME.eq(username))
        .fetchOptional()
        .map(AppUserRepository::zuZeile);
  }

  public Optional<AppUserZeile> findeNachId(long id) {
    return monitorDsl
        .select(
            APP_USER.ID,
            APP_USER.USERNAME,
            APP_USER.PASSWORD_HASH,
            APP_USER.ROLE,
            APP_USER.ENABLED,
            APP_USER.MUST_CHANGE_PASSWORD,
            APP_USER.LOCKED_BY_ADMIN,
            APP_USER.FAILED_ATTEMPTS,
            APP_USER.LOCKED_UNTIL)
        .from(APP_USER)
        .where(APP_USER.ID.eq(id))
        .fetchOptional()
        .map(AppUserRepository::zuZeile);
  }

  public boolean existiertBenutzername(String username) {
    return monitorDsl.fetchExists(
        monitorDsl.selectOne().from(APP_USER).where(APP_USER.USERNAME.eq(username)));
  }

  /** Ob ueberhaupt schon ein Konto mit Rolle ADMIN existiert — die Bedingung des Bootstraps. */
  public boolean existiertAdmin() {
    return monitorDsl.fetchExists(
        monitorDsl.selectOne().from(APP_USER).where(APP_USER.ROLE.eq(Rolle.ADMIN.name())));
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Die Kontenliste (Schritt 9a)
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Die abgeleitete Tabelle mit der letzten erfolgreichen Anmeldung je Konto (E17).
   *
   * <p><b>Warum das im selben Statement steht und nicht in einer zweiten Abfrage.</b> Zwei Gruende,
   * beide zwingend:
   *
   * <ol>
   *   <li>Gruppiert wird ueber {@code actor_user_id} und nicht ueber {@code actor_username}. {@code
   *       AnmeldeService} protokolliert den Namen so, <b>wie er eingetippt wurde</b> — und {@code
   *       app_user.username} vergleicht ohne Ruecksicht auf Gross- und Kleinschreibung. Eine
   *       Zuordnung ueber den Namen im Anwendungscode traefe genau die Konten nicht, deren Nutzer
   *       sich mit abweichender Schreibweise anmelden. Die Kennung ist der einzige verlaessliche
   *       Schluessel, und ein Join im Statement macht die Sortierung der Datenbank zur Wahrheit
   *       statt einer Zeichenkette in Java.
   *   <li>Eine <b>abgeleitete Tabelle</b> und keine korrelierte Unterabfrage: Sie liest die
   *       Anmeldezeilen einmal statt einmal je Konto. Gemessen in M82.
   * </ol>
   *
   * <p><b>Regel L2 ist nicht beruehrt.</b> Sie verbietet Live-Aggregation ueber {@code Message} im
   * Quellschema; {@code audit_log} ist unser eigenes und um Groessenordnungen kleiner.
   */
  private static Table<?> letzteAnmeldungen(Long nurKonto) {
    var abfrage =
        DSL.select(AUDIT_LOG.ACTOR_USER_ID, DSL.max(AUDIT_LOG.OCCURRED_AT).as("letzte_anmeldung"))
            .from(AUDIT_LOG)
            .where(AUDIT_LOG.EVENT_TYPE.eq(AuditEventType.ANMELDUNG_ERFOLG.name()))
            .and(AUDIT_LOG.ACTOR_USER_ID.isNotNull());
    if (nurKonto != null) {
      // Die Einschraenkung gehoert IN die abgeleitete Tabelle und nicht nur an die aeussere
      // Bedingung: Sonst gruppierte jede Einzelabfrage ueber den gesamten Protokollbestand und
      // warfe das Ergebnis anschliessend weg. Gemessen sind das 17,9 ms je Aufruf (M82), und die
      // fuenf schreibenden Vorgaenge lesen zweimal.
      abfrage = abfrage.and(AUDIT_LOG.ACTOR_USER_ID.eq(nurKonto));
    }
    return abfrage.groupBy(AUDIT_LOG.ACTOR_USER_ID).asTable("anmeldung");
  }

  /**
   * <b>Alle</b> Konten, mandantenfrei (E2), ohne Paginierung und ohne serverseitige Suche (E16).
   *
   * <p>Die Nutzerverwaltung ist die eine Flaeche, an der Regel M2 nicht greift: {@code app_user}
   * liegt in {@code overlord_monitor}, und ein ADMIN-Konto traegt eine wirkungslose
   * Mandantenzuordnung. In einer mandantengefilterten Liste stuende es unter einem Mandanten, fuer
   * den es nicht gilt — die mandantenfreie Liste ist deshalb nicht die bequemere, sondern die
   * richtige.
   *
   * <p>Sortiert nach Benutzername: Er ist eindeutig, und damit ist die Reihenfolge ohne Paginierung
   * trotzdem stabil.
   */
  public List<KontoZeile> findeAlleKonten() {
    return konten(DSL.noCondition(), null);
  }

  /**
   * Genau ein Konto, in derselben Gestalt wie eine Zeile der Liste.
   *
   * <p>Bewusst dieselbe Abfrage mit einer zusaetzlichen Bedingung und keine zweite, schlankere: Die
   * schreibenden Endpunkte antworten mit der geaenderten Zeile, und die muss zeichengleich zu der
   * sein, die die Liste liefert. Zwei Abfragen wuerden mit der Zeit auseinanderlaufen, und der
   * Unterschied faellt erst auf, wenn jemand die beiden Antworten nebeneinanderlegt.
   */
  public Optional<KontoZeile> findeKonto(long id) {
    return konten(APP_USER.ID.eq(id), id).stream().findFirst();
  }

  /**
   * Ob es <b>ausser</b> diesem Konto noch einen nutzbaren Administrator gibt — aktiv und nicht
   * administrativ gesperrt (E12).
   *
   * <p>Als eigene Existenzabfrage und nicht ueber {@link #findeAlleKonten()}: Der Selbstschutz
   * laeuft bei jedem der drei entwertenden Vorgaenge mit, und die Kontenliste zieht die Aggregation
   * ueber {@code audit_log} nach sich, die hier nichts zu suchen hat.
   *
   * <p><b>Gesperrte zaehlen nicht mit.</b> Taeten sie es, waere der zweite Admin ein Feigenblatt —
   * das ist der Satz, an dem E12 haengt.
   */
  public boolean existiertAndererNutzbarerAdmin(long ausserId) {
    return monitorDsl.fetchExists(
        monitorDsl
            .selectOne()
            .from(APP_USER)
            .where(APP_USER.ROLE.eq(Rolle.ADMIN.name()))
            .and(APP_USER.ENABLED.isTrue())
            .and(APP_USER.LOCKED_BY_ADMIN.isFalse())
            .and(APP_USER.ID.ne(ausserId)));
  }

  private List<KontoZeile> konten(Condition bedingung, Long nurKonto) {
    Table<?> anmeldung = letzteAnmeldungen(nurKonto);
    Field<Long> anmeldungUserId = anmeldung.field(AUDIT_LOG.ACTOR_USER_ID);
    Field<LocalDateTime> anmeldungZeitpunkt =
        anmeldung.field("letzte_anmeldung", LocalDateTime.class);

    Map<Long, List<String>> mandanten =
        nurKonto == null ? mandantenJeKonto() : Map.of(nurKonto, mandantenVon(nurKonto));
    return monitorDsl
        .select(
            APP_USER.ID,
            APP_USER.USERNAME,
            APP_USER.ROLE,
            APP_USER.ENABLED,
            APP_USER.MUST_CHANGE_PASSWORD,
            APP_USER.LOCKED_BY_ADMIN,
            // Seit dem 24.08.2026 mitgelesen. Bis dahin blieb die Spalte hier aus, und das war eine
            // Entscheidung und kein Versehen — sie ist am selben Tag umgekehrt worden
            // (docs/benutzerverwaltung.md E20, docs/benutzerverwaltung-backend.md §4). Sie kostet
            // nichts: dieselbe Zeile, dieselbe Tabelle, kein zusaetzlicher Join.
            APP_USER.LOCKED_UNTIL,
            anmeldungZeitpunkt)
        .from(APP_USER)
        .leftJoin(anmeldung)
        .on(anmeldungUserId.eq(APP_USER.ID))
        .where(bedingung)
        .orderBy(APP_USER.USERNAME)
        .fetch(
            satz ->
                new KontoZeile(
                    satz.get(APP_USER.ID),
                    satz.get(APP_USER.USERNAME),
                    Rolle.ausDatenbank(satz.get(APP_USER.ROLE)),
                    mandanten.getOrDefault(satz.get(APP_USER.ID), List.of()),
                    Boolean.TRUE.equals(satz.get(APP_USER.LOCKED_BY_ADMIN)),
                    satz.get(APP_USER.LOCKED_UNTIL),
                    Boolean.TRUE.equals(satz.get(APP_USER.ENABLED)),
                    Boolean.TRUE.equals(satz.get(APP_USER.MUST_CHANGE_PASSWORD)),
                    satz.get(anmeldungZeitpunkt)));
  }

  /**
   * Die Mandantenkennungen aller Konten in <b>einer</b> Abfrage.
   *
   * <p>Nicht ueber {@code MandantRepository.findeFuerNutzer}: Das joint schemauebergreifend gegen
   * {@code GlassfishDB.Mandant} und liefe hier einmal je Konto. Fuer die Liste werden die Kennungen
   * gebraucht, und die stehen vollstaendig in {@code app_user_mandant} — im eigenen Schema, ohne
   * Regel M2 und ohne Ausnahme.
   */
  private Map<Long, List<String>> mandantenJeKonto() {
    Map<Long, List<String>> je = new HashMap<>();
    monitorDsl
        .select(APP_USER_MANDANT.USER_ID, APP_USER_MANDANT.MANDANT_ID)
        .from(APP_USER_MANDANT)
        .orderBy(APP_USER_MANDANT.MANDANT_ID)
        .forEach(
            satz ->
                je.computeIfAbsent(satz.value1(), unused -> new ArrayList<>()).add(satz.value2()));
    return je;
  }

  /** Die Mandantenkennungen genau eines Kontos, aufsteigend. */
  public List<String> mandantenVon(long userId) {
    return monitorDsl
        .select(APP_USER_MANDANT.MANDANT_ID)
        .from(APP_USER_MANDANT)
        .where(APP_USER_MANDANT.USER_ID.eq(userId))
        .orderBy(APP_USER_MANDANT.MANDANT_ID)
        .fetch(APP_USER_MANDANT.MANDANT_ID);
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Schreiben
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Schreibt Fehlversuchszaehler und Sperrfrist fort.
   *
   * <p><b>Fasst {@code locked_by_admin} nicht an</b> — und genau deshalb steht die administrative
   * Sperre in einer eigenen Spalte. Diese Methode schreibt {@code LOCKED_UNTIL} bedingungslos, auch
   * mit {@code null}; laege die Admin-Sperre dort, loeschte ein einziger falscher Anmeldeversuch
   * sie.
   *
   * @param gesperrtBisUtc {@code null} loescht eine bestehende Zeitsperre
   */
  public void setzeFehlversuche(
      long id, int anzahl, LocalDateTime gesperrtBisUtc, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.FAILED_ATTEMPTS, anzahl)
        .set(APP_USER.LOCKED_UNTIL, gesperrtBisUtc)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  /** Erfolgreiche Anmeldung: Zaehler und Zeitsperre zuruecksetzen, Zeitpunkt merken. */
  public void merkeAnmeldung(long id, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.FAILED_ATTEMPTS, 0)
        .set(APP_USER.LOCKED_UNTIL, (LocalDateTime) null)
        .set(APP_USER.LAST_LOGIN_AT, jetztUtc)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  /**
   * Neues Passwort setzen.
   *
   * @param aenderungszwang {@code false} nach der eigenen Aenderung, {@code true} nach dem Reset
   *     durch einen Admin. <b>Der Zwang ist Parameter und nicht fest verdrahtet</b> — dieselbe
   *     Bauform wie bei {@link #legeAn}, damit Anlegen und Zuruecksetzen denselben Weg gehen (E13)
   */
  public void setzePasswort(long id, String hash, boolean aenderungszwang, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.PASSWORD_HASH, hash)
        .set(APP_USER.MUST_CHANGE_PASSWORD, aenderungszwang)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  /**
   * Setzt die <b>administrative</b> Sperre.
   *
   * <p><b>Entsperren raeumt zusaetzlich die Zeitsperre ab.</b> Das ist kein Beiwerk, sondern der
   * Supportfall, um dessentwillen der Endpunkt existiert: Wer anruft, ist nach fuenf Fehlversuchen
   * ausgesperrt und nicht durch einen Verwaltungsakt. Ein Entsperren, das den haeufigeren der
   * beiden Zustaende stehen liesse, waere eine Falle.
   */
  public void setzeAdminSperre(long id, boolean gesperrt, LocalDateTime jetztUtc) {
    var anweisung =
        monitorDsl
            .update(APP_USER)
            .set(APP_USER.LOCKED_BY_ADMIN, gesperrt)
            .set(APP_USER.UPDATED_AT, jetztUtc);
    if (!gesperrt) {
      anweisung =
          anweisung
              .set(APP_USER.LOCKED_UNTIL, (LocalDateTime) null)
              .set(APP_USER.FAILED_ATTEMPTS, 0);
    }
    anweisung.where(APP_USER.ID.eq(id)).execute();
  }

  /** Aktiviert oder deaktiviert ein Konto. Geloescht wird nie (E8). */
  public void setzeAktiv(long id, boolean aktiv, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.ENABLED, aktiv)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  public void setzeRolle(long id, Rolle rolle, LocalDateTime jetztUtc) {
    monitorDsl
        .update(APP_USER)
        .set(APP_USER.ROLE, rolle.name())
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .where(APP_USER.ID.eq(id))
        .execute();
  }

  /**
   * Legt ein Konto an und liefert dessen ID. Aufrufer pruefen den Benutzernamen vorher; der
   * eindeutige Index ist die Wahrheit und schlaegt bei einem Wettlauf trotzdem zu.
   */
  public long legeAn(
      String username,
      String passwortHash,
      Rolle rolle,
      boolean aktiv,
      boolean passwortwechselErforderlich,
      LocalDateTime jetztUtc) {
    return monitorDsl
        .insertInto(APP_USER)
        .set(APP_USER.USERNAME, username)
        .set(APP_USER.PASSWORD_HASH, passwortHash)
        .set(APP_USER.ROLE, rolle.name())
        .set(APP_USER.ENABLED, aktiv)
        .set(APP_USER.MUST_CHANGE_PASSWORD, passwortwechselErforderlich)
        .set(APP_USER.CREATED_AT, jetztUtc)
        .set(APP_USER.UPDATED_AT, jetztUtc)
        .returningResult(APP_USER.ID)
        .fetchOne(APP_USER.ID);
  }

  /**
   * Ordnet einem Konto einen Mandanten zu. Kein Fremdschluessel auf {@code GlassfishDB.Mandant}.
   */
  public void ordneMandantZu(long userId, String mandantId) {
    monitorDsl
        .insertInto(APP_USER_MANDANT)
        .set(APP_USER_MANDANT.USER_ID, userId)
        .set(APP_USER_MANDANT.MANDANT_ID, mandantId)
        .execute();
  }

  /**
   * Ersetzt die Mandantenmenge eines Kontos vollstaendig.
   *
   * <p>Bewusst loeschen und neu schreiben statt Differenzen zu bilden: Die Menge ist bei dreissig
   * Konten und zehn Mandanten winzig, und eine Mengenersetzung hat genau ein Ergebnis, waehrend
   * eine Differenzbildung zwei Fehlerarten hat. Der Aufrufer stellt sicher, dass {@code mandantIds}
   * nicht leer ist (E10).
   *
   * <p><b>Die einzige Methode dieser Klasse mit {@link Transactional}</b>, und das ist kein
   * Versehen: Sie ist die einzige, die mehr als eine Anweisung ausfuehrt. Ein Abbruch zwischen dem
   * {@code DELETE} und dem letzten {@code INSERT} liesse ein Konto ohne jede Zuordnung zurueck —
   * genau den Zustand, den E10 verbietet. Alle uebrigen Schreibwege sind ein einzelnes {@code
   * UPDATE} und damit von selbst atomar; sie tragen bewusst <b>keine</b> Transaktion, damit der
   * Sitzungsentzug erst nach dem Festschreiben laufen kann (Begruendung in {@code
   * BenutzerverwaltungService.abschluss}).
   */
  @Transactional
  public void ersetzeMandanten(long userId, Collection<String> mandantIds) {
    monitorDsl.deleteFrom(APP_USER_MANDANT).where(APP_USER_MANDANT.USER_ID.eq(userId)).execute();
    for (String mandantId : mandantIds) {
      ordneMandantZu(userId, mandantId);
    }
  }

  private static AppUserZeile zuZeile(Record satz) {
    return new AppUserZeile(
        satz.get(APP_USER.ID),
        satz.get(APP_USER.USERNAME),
        satz.get(APP_USER.PASSWORD_HASH),
        Rolle.ausDatenbank(satz.get(APP_USER.ROLE)),
        Boolean.TRUE.equals(satz.get(APP_USER.ENABLED)),
        Boolean.TRUE.equals(satz.get(APP_USER.MUST_CHANGE_PASSWORD)),
        Boolean.TRUE.equals(satz.get(APP_USER.LOCKED_BY_ADMIN)),
        satz.get(APP_USER.FAILED_ATTEMPTS),
        satz.get(APP_USER.LOCKED_UNTIL));
  }
}
