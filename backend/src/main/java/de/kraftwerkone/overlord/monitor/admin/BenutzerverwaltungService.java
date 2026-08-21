package de.kraftwerkone.overlord.monitor.admin;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.AppUserRepository;
import de.kraftwerkone.overlord.monitor.security.KontoZeile;
import de.kraftwerkone.overlord.monitor.security.MandantRepository;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.Sitzungsentzug;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Auflisten, sperren, deaktivieren, Rolle aendern, Mandanten pflegen, Passwort zuruecksetzen —
 * Schritt 9a. Das Anlegen bleibt in {@link AdminUserService} (E4), <b>geloescht wird nie</b> (E8).
 *
 * <p><b>Ein Endpunkt, ein Vorgang, eine Ereignisart.</b> Der Grund ist das Protokoll: Ein
 * gemeinsames {@code PATCH}, das Rolle, Mandanten und Sperrzustand in einem Aufruf aendern koennte,
 * erzeugte eine Zeile, die entweder aufgespalten werden muss oder zu einem nichtssagenden {@code
 * NUTZER_GEAENDERT} verwaessert — und dann ist es bei einem Vorfall nicht mehr lesbar.
 *
 * <p><b>Jeder der fuenf schreibenden Vorgaenge verwirft alle Sitzungen des betroffenen Kontos</b>
 * (E5) — eine Regel, keine Fallunterscheidung, und <b>immer erst, nachdem die Aenderung
 * festgeschrieben ist</b> (Begruendung an {@link #abschluss}). Ohne sie wirkte eine Sperre erst
 * beim naechsten Anmelden, und die Zusage, mit der dieses Projekt JWT abgelehnt hat, waere nicht
 * eingeloest. Die Zahl verworfener Sitzungen steht im Detail des ausloesenden Ereignisses und
 * bekommt keine eigene Art (E15).
 *
 * <p><b>Die Rollengrenze steht in {@code config/SecurityConfig}</b> ({@code /api/admin/**} verlangt
 * {@code ADMIN}) und nicht als Annotation hier — dieselbe Entscheidung wie bei {@link
 * AdminUserController} und {@code ProzessKatalogController}: Die Regel soll an einer Stelle stehen.
 *
 * <p><b>Statuscodes.</b> Keine der verbindlichen Dateien legt sie fuer diese Endpunkte fest; die
 * folgende Zuordnung ist am 21.08.2026 aus dem Bestand fortgeschrieben und in {@code
 * docs/benutzerverwaltung-backend.md} §4 begruendet. {@code 404} fuer eine unbekannte Kennung wie
 * bei {@code POST /api/admin/users}; {@code 400} fuer eine unbrauchbare Eingabe (zu kurzes
 * Passwort, unbekannte Rolle) mit den <b>bestehenden</b> Problemtypen; {@code 409} fuer die vier
 * Faelle, in denen die Eingabe in Ordnung ist und der <i>Zustand</i> sie verbietet — Selbstschutz,
 * letzter nutzbarer ADMIN, letzte Mandantenzuordnung, Herabstufung ohne Zuordnung. Bewusst kein
 * {@code 403} dafuer: Das bedeutet in diesem Projekt „die Rolle reicht nicht" und waere von der
 * Rollengrenze nicht zu unterscheiden.
 */
@Service
public class BenutzerverwaltungService {

  private final AppUserRepository appUserRepository;
  private final MandantRepository mandantRepository;
  private final PasswordEncoder passwortKodierer;
  private final AuditLogWriter auditLogWriter;
  private final Sitzungsentzug sitzungsentzug;
  private final Clock systemClock;

  BenutzerverwaltungService(
      AppUserRepository appUserRepository,
      MandantRepository mandantRepository,
      PasswordEncoder passwortKodierer,
      AuditLogWriter auditLogWriter,
      Sitzungsentzug sitzungsentzug,
      @Qualifier("systemClock") Clock systemClock) {
    this.appUserRepository = appUserRepository;
    this.mandantRepository = mandantRepository;
    this.passwortKodierer = passwortKodierer;
    this.auditLogWriter = auditLogWriter;
    this.sitzungsentzug = sitzungsentzug;
    this.systemClock = systemClock;
  }

  /** Alle Konten, mandantenfrei, ohne Paginierung und ohne serverseitige Suche (E2, E16). */
  public List<NutzerzeileResponse> liste() {
    return appUserRepository.findeAlleKonten().stream().map(NutzerzeileResponse::fuer).toList();
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Die fuenf schreibenden Vorgaenge
  // ───────────────────────────────────────────────────────────────────────────

  /** Sperren und Entsperren — ein Endpunkt mit Zustand, zwei Ereignisarten. */
  public NutzerzeileResponse setzeSperre(
      AngemeldeterNutzer admin, long id, boolean gesperrt, String ip) {
    KontoZeile ziel = konto(id);
    if (gesperrt) {
      // Nur die entwertende Richtung ist geschuetzt. Sich selbst zu ENTsperren ist ohnehin
      // unmoeglich — wer gesperrt ist, kommt nicht herein; ein Verbot darauf waere Laerm.
      pruefeEntwertung(admin, ziel, "sperren");
    }
    appUserRepository.setzeAdminSperre(id, gesperrt, jetztUtc());
    return abschluss(
        admin,
        ziel,
        gesperrt ? AuditEventType.SPERRE_DURCH_ADMIN : AuditEventType.ENTSPERRT_DURCH_ADMIN,
        gesperrt ? "gesperrt" : "entsperrt; Zeitsperre und Fehlversuchszaehler zurueckgesetzt",
        ip);
  }

  /** Deaktivieren und Reaktivieren. <b>Es gibt kein Loeschen</b> (E8). */
  public NutzerzeileResponse setzeAktiv(
      AngemeldeterNutzer admin, long id, boolean aktiv, String ip) {
    KontoZeile ziel = konto(id);
    if (!aktiv) {
      pruefeEntwertung(admin, ziel, "deaktivieren");
    }
    appUserRepository.setzeAktiv(id, aktiv, jetztUtc());
    return abschluss(
        admin,
        ziel,
        aktiv ? AuditEventType.NUTZER_REAKTIVIERT : AuditEventType.NUTZER_DEAKTIVIERT,
        aktiv ? "reaktiviert" : "deaktiviert",
        ip);
  }

  /** Rollenwechsel. Die Herabstufung ist der Fall mit den Bedingungen (E11, E12). */
  public NutzerzeileResponse setzeRolle(
      AngemeldeterNutzer admin, long id, String rolleText, String ip) {
    KontoZeile ziel = konto(id);
    Rolle neu = rolleAus(rolleText);
    if (ziel.rolle() == Rolle.ADMIN && neu == Rolle.MANDANT) {
      pruefeEntwertung(admin, ziel, "herabstufen");
      if (ziel.mandanten().isEmpty()) {
        // Kein Sonderpfad, nur eine Reihenfolge. Das Bootstrap-Konto ist der Anlass: Es ist das
        // einzige ohne jede Zuordnung, weil BootstrapAdminRunner keine anlegt.
        throw new FachlicheAusnahme(
            HttpStatus.CONFLICT,
            "rolle-ohne-mandant",
            "Erst Mandanten zuordnen",
            "Dieses Konto hat keinen Mandanten. Ordne ihm zuerst mindestens einen zu, dann laesst"
                + " es sich auf MANDANT herabstufen.",
            "Herabstufung ohne Mandantenzuordnung, Konto " + id);
      }
    }
    appUserRepository.setzeRolle(id, neu, jetztUtc());
    return abschluss(
        admin,
        ziel,
        AuditEventType.ROLLE_GEAENDERT,
        "Rolle " + ziel.rolle().name() + " -> " + neu.name(),
        ip);
  }

  /**
   * Die Mandantenmenge eines Kontos — <b>die dritte Ausnahme von Regel M1</b>.
   *
   * <p>Sie ist zulaessig aus demselben Grund wie die beiden anderen: Hier wird eine <i>Berechtigung
   * definiert</i> und kein <i>Datenausschnitt abgefragt</i>. Die uebergebenen IDs sagen nichts
   * darueber aus, was der pflegende Admin lesen darf, sondern nur, fuer wen das fremde Konto
   * kuenftig gilt. Geprueft wird ueber {@code MandantRepository.existiert} — die Methode ist seit
   * Schritt 3 als {@code @OhneMandantenkontext} gefuehrt, es braucht keine neue Markierung.
   *
   * <p><b>Eine unbekannte Kennung ergibt {@code 404}</b>, wie beim Anlegen: Ein ADMIN kennt die
   * Mandantenliste ohnehin, hier ist also nichts zu verbergen.
   */
  public NutzerzeileResponse setzeMandanten(
      AngemeldeterNutzer admin, long id, List<String> mandantIds, String ip) {
    KontoZeile ziel = konto(id);
    // Reihenfolge stabil und Doppelte weg: Zweimal dieselbe ID ist keine Eingabe, die einen Fehler
    // wert waere, aber der Primaerschluessel von app_user_mandant wuerde sich daran verschlucken.
    Set<String> gewuenscht = new LinkedHashSet<>(mandantIds == null ? List.of() : mandantIds);
    gewuenscht.removeIf(wert -> wert == null || wert.isBlank());
    if (gewuenscht.isEmpty()) {
      // Fuer BEIDE Rollen. Schritt 3 speichert die Zuordnung auch fuer ADMIN, "damit sie bei einer
      // spaeteren Herabstufung nicht ins Leere faellt" — duerfte man sie dort entfernen, entstuende
      // beim naechsten Rollenwechsel genau der Zustand, den E11 verbietet.
      throw new FachlicheAusnahme(
          HttpStatus.CONFLICT,
          "letzte-mandantenzuordnung",
          "Mindestens ein Mandant",
          "Ein Konto braucht mindestens einen Mandanten. Ordne einen anderen zu, bevor du diesen"
              + " entfernst.",
          "Versuch, die Mandantenmenge von Konto " + id + " zu leeren");
    }
    for (String mandantId : gewuenscht) {
      if (!mandantRepository.existiert(mandantId)) {
        throw new RessourceNichtGefundenException(
            "Unbekannte MandantID bei der Mandantenpflege von Konto " + id);
      }
    }
    appUserRepository.ersetzeMandanten(id, gewuenscht);
    return abschluss(
        admin,
        ziel,
        AuditEventType.MANDANTEN_GEAENDERT,
        "Mandanten " + ziel.mandanten() + " -> " + List.copyOf(gewuenscht),
        ip);
  }

  /**
   * Passwort zuruecksetzen: <b>der Admin tippt es</b> (E13), mindestens zwoelf Zeichen, und es darf
   * nicht dem aktuellen entsprechen. Das Konto bekommt Aenderungszwang.
   *
   * <p><b>Steht bewusst nicht unter Selbstschutz.</b> Fuer das eigene Passwort gibt es {@code POST
   * /api/auth/password}; wer es hier auf sein eigenes Konto anwendet, wirft sich zwar aus allen
   * Sitzungen (E5), sperrt sich aber nicht aus — er kennt das eben getippte Passwort.
   */
  public NutzerzeileResponse setzePasswort(
      AngemeldeterNutzer admin, long id, String einmalpasswort, String ip) {
    KontoZeile ziel = konto(id);
    // Gemeinsam mit dem Anlegen: Laenge pruefen und kodieren (E13).
    String hash = AdminUserService.kodiereEinmalpasswort(passwortKodierer, einmalpasswort);
    // Und das eine, was das Zuruecksetzen zusaetzlich kann: der Vergleich gegen den gespeicherten
    // Hash. Beim Anlegen gibt es keinen, gegen den sich vergleichen liesse.
    appUserRepository
        .findeNachId(id)
        .map(zeile -> zeile.passwortHash())
        .filter(bisher -> bisher != null && passwortKodierer.matches(einmalpasswort, bisher))
        .ifPresent(
            bisher -> {
              throw new FachlicheAusnahme(
                  HttpStatus.BAD_REQUEST,
                  "passwort-unveraendert",
                  "Passwort unveraendert",
                  "Das neue Passwort muss sich vom bisherigen unterscheiden.",
                  "Passwort-Reset mit dem aktuellen Passwort, Konto " + id);
            });
    appUserRepository.setzePasswort(id, hash, true, jetztUtc());
    // Niemals das Passwort im Detail — weder im Klartext noch als Hash noch abgekuerzt.
    return abschluss(
        admin, ziel, AuditEventType.PASSWORT_ZURUECKGESETZT, "Passwort gesetzt, Wechselzwang", ip);
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Gemeinsames
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Der Abschluss jedes schreibenden Vorgangs: Sitzungen verwerfen, protokollieren, geaenderte
   * Zeile liefern.
   *
   * <p>Er steht an einer Stelle, weil E5 <b>eine Regel</b> ist. Waere der Entzug in jedem der fuenf
   * Vorgaenge einzeln aufgerufen, waere die Regel eine Aufzaehlung — und eine Aufzaehlung verliert
   * beim sechsten Vorgang einen Eintrag.
   *
   * <p><b>Der Aufrufer hat seine Aenderung bereits festgeschrieben, wenn er hier ankommt — und das
   * ist der Grund, weshalb keine der fuenf Methoden {@code @Transactional} traegt.</b> Die
   * Reihenfolge ist am 21.08.2026 umgedreht worden, nachdem eine Nachpruefung das Fenster gefunden
   * hat:
   *
   * <p>Lief der Entzug <i>innerhalb</i> der offenen Transaktion, verwarf er die Sitzungen sofort —
   * {@code JdbcIndexedSessionRepository.deleteById} laeuft ueber eine eigene Transaktion mit {@code
   * PROPAGATION_REQUIRES_NEW} und committet also unabhaengig —, waehrend die Sperre selbst noch
   * <b>uncommitted</b> war. In diesem Fenster sieht die Anmeldung auf einer anderen Verbindung
   * weiterhin {@code locked_by_admin = 0}: Der eben ausgesperrte Nutzer konnte sich neu anmelden
   * und bekam eine Sitzung, <b>die kein Entzug mehr traf</b> — sie entstand nach ihm. Auf der
   * Testkopie dauert ein {@code COMMIT} zehn bis fuenfundzwanzig Sekunden; das Fenster ist keine
   * theoretische Groesse.
   *
   * <p>Jetzt ist es umgekehrt: Wer sich im Fenster zwischen Festschreiben und Entzug anmeldet, wird
   * bereits abgewiesen — die Sperre steht. Uebrig bleibt nur, dass eine <i>bestehende</i> Sitzung
   * einen Wimpernschlag laenger lebt, und die faellt unmittelbar danach.
   *
   * <p>Die einzelnen Schreibwege brauchen dafuer keine Transaktion: Vier sind ein einzelnes {@code
   * UPDATE} und damit von selbst atomar. Der fuenfte — die Mandantenmenge — ist {@code DELETE} plus
   * {@code INSERT}s und traegt seine Transaktion in {@code AppUserRepository.ersetzeMandanten}, wo
   * sie hingehoert und wo sie geschlossen ist, bevor der Entzug beginnt.
   */
  private NutzerzeileResponse abschluss(
      AngemeldeterNutzer admin, KontoZeile ziel, AuditEventType typ, String was, String ip) {
    int verworfen = sitzungsentzug.verwirfAlle(ziel.username());
    auditLogWriter.schreibe(
        new AuditEvent(
            typ,
            admin.id(),
            admin.username(),
            null,
            "app_user",
            String.valueOf(ziel.id()),
            ip,
            ziel.username() + ": " + was + "; Sitzungen verworfen: " + verworfen));
    return NutzerzeileResponse.fuer(konto(ziel.id()));
  }

  /**
   * Der Selbstschutz, zweistufig (E12) — und die Reihenfolge ist Absicht.
   *
   * <p><b>Zuerst der letzte nutzbare Administrator, danach das eigene Konto.</b> Beide Faelle
   * ueberschneiden sich fast immer: Wer als einziger nutzbarer Admin angemeldet ist, trifft mit
   * jedem entwertenden Vorgang auf sich selbst. Die Meldung „das ist der letzte nutzbare
   * Administrator" sagt in diesem Fall mehr als „du kannst dich nicht selbst sperren" — sie nennt
   * die Bedingung, unter der es ginge. Stuende der Selbstschutz vorn, waere die zweite Stufe
   * praktisch unerreichbar und damit ungetestet.
   *
   * <p><b>Nutzbar heisst aktiv und nicht gesperrt.</b> Zaehlte man gesperrte Administratoren mit,
   * waere der zweite Admin ein Feigenblatt. Die <i>automatische</i> Sperre nach fuenf Fehlversuchen
   * zaehlt hier <b>nicht</b> als „gesperrt": Sie laeuft nach fuenfzehn Minuten ab, und eine
   * Herabstufung fuer eine Viertelstunde zu verweigern, weil jemand sich vertippt hat, waere eine
   * Regel, die von der Uhr abhaengt.
   *
   * <p>Das ist kein Sicherheitsnetz, sondern die Vermeidung eines vermeidbaren Supportfalls — der
   * Betreiber hat einen SQL-Client.
   */
  private void pruefeEntwertung(AngemeldeterNutzer admin, KontoZeile ziel, String vorgang) {
    if (ziel.istNutzbarerAdmin() && !appUserRepository.existiertAndererNutzbarerAdmin(ziel.id())) {
      throw new FachlicheAusnahme(
          HttpStatus.CONFLICT,
          "letzter-admin",
          "Letzter Administrator",
          "Das ist der letzte nutzbare Administrator. Mach zuerst ein anderes Konto zum aktiven,"
              + " nicht gesperrten Administrator.",
          "Vorgang " + vorgang + " auf den letzten nutzbaren ADMIN, Konto " + ziel.id());
    }
    if (ziel.id() == admin.id()) {
      throw new FachlicheAusnahme(
          HttpStatus.CONFLICT,
          "selbstschutz",
          "Nicht am eigenen Konto",
          "Das eigene Konto laesst sich nicht sperren, deaktivieren oder herabstufen. Ein anderer"
              + " Administrator kann es.",
          "Vorgang " + vorgang + " auf das eigene Konto " + admin.id());
    }
  }

  private KontoZeile konto(long id) {
    return appUserRepository
        .findeKonto(id)
        .orElseThrow(
            () ->
                new RessourceNichtGefundenException(
                    "Unbekannte Konto-ID in der Verwaltung: " + id));
  }

  private static Rolle rolleAus(String rolleText) {
    try {
      return Rolle.valueOf(rolleText);
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "unbekannte-rolle",
          "Unbekannte Rolle",
          "Gueltige Rollen sind MANDANT und ADMIN.");
    }
  }

  private LocalDateTime jetztUtc() {
    return LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
  }
}
