package de.kraftwerkone.overlord.monitor.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.Baumgliederung;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.AppUserRepository;
import de.kraftwerkone.overlord.monitor.security.KontoZeile;
import de.kraftwerkone.overlord.monitor.security.MandantRepository;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.Sitzungsentzug;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Die zweite Stufe des Selbstschutzes (E12): <b>der letzte nutzbare Administrator</b>.
 *
 * <p><b>Warum das hier steht und nicht in einem {@code DbIT} — eine bewusste Abweichung, benannt
 * statt verschwiegen.</b> Die Regel greift, wenn es im <i>gesamten</i> Bestand keinen zweiten
 * aktiven, nicht gesperrten Administrator mehr gibt. Die Testkopie traegt aber die echten
 * Administratorkonten des Betreibers; um den Zustand „genau ein nutzbarer ADMIN" herzustellen,
 * muesste ein Test sie deaktivieren oder sperren. Das ist ein Eingriff in eine gemeinsam genutzte
 * Umgebung, den kein Testlauf wert ist — ein misslungener Rollback naehme dem Betreiber den Zugang
 * zu seinem eigenen Werkzeug.
 *
 * <p>Geprueft wird deshalb hier die <b>Entscheidung</b> mit einem gestellten Repository, und im
 * Integrationstest die erste Stufe (das eigene Konto) sowie alles uebrige. Die Bedingung selbst —
 * <i>aktiv und nicht administrativ gesperrt</i> — steht als eine Zeile in {@code
 * AppUserRepository.existiertAndererNutzbarerAdmin}.
 *
 * <p><b>Seit dem 15.09.2026 steht hier auch die Baumgliederung</b> (E26), und aus einem verwandten
 * Grund: Ihre tragende Aussage ist ein <i>ausbleibender</i> Aufruf — kein Sitzungsentzug, kein
 * Selbstschutz. Am gestellten Repository ist das eine Zeile; an der Testkopie waere es eine
 * Behauptung ueber Sitzungen, die nebenher niemand anlegt.
 */
class BenutzerverwaltungServiceTest {

  private static final String IP = "203.0.113.7";
  private static final LocalDateTime JETZT = LocalDateTime.of(2026, 8, 21, 9, 0);

  /** Der handelnde Admin. Ein <b>anderer</b> als das Ziel — sonst griffe die erste Stufe. */
  private static final AngemeldeterNutzer HANDELNDER =
      new AngemeldeterNutzer(1L, "it-handelnder", Rolle.ADMIN, false);

  private AppUserRepository appUserRepository;
  private AuditLogWriter auditLogWriter;
  private Sitzungsentzug sitzungsentzug;
  private BenutzerverwaltungService service;

  @BeforeEach
  void aufsetzen() {
    appUserRepository = mock(AppUserRepository.class);
    auditLogWriter = mock(AuditLogWriter.class);
    sitzungsentzug = mock(Sitzungsentzug.class);
    service =
        new BenutzerverwaltungService(
            appUserRepository,
            mock(MandantRepository.class),
            mock(PasswordEncoder.class),
            auditLogWriter,
            sitzungsentzug,
            Clock.fixed(JETZT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
  }

  private static KontoZeile konto(long id, String username, boolean adminGesperrt) {
    return new KontoZeile(
        id,
        username,
        Rolle.ADMIN,
        List.of("VOTG"),
        adminGesperrt,
        null,
        true,
        false,
        null,
        Baumgliederung.PARTNER);
  }

  /** Das Ziel: ein nutzbarer Administrator mit Mandant, also ohne Nebenbedingung aus E11. */
  private KontoZeile zielIstNutzbarerAdmin() {
    KontoZeile ziel = konto(2L, "it-ziel", false);
    when(appUserRepository.findeKonto(2L)).thenReturn(Optional.of(ziel));
    return ziel;
  }

  private void nochEinNutzbarerAdmin(boolean vorhanden) {
    when(appUserRepository.existiertAndererNutzbarerAdmin(2L)).thenReturn(vorhanden);
  }

  private void abgelehnt(org.assertj.core.api.ThrowableAssert.ThrowingCallable aufruf) {
    assertThatThrownBy(aufruf)
        .isInstanceOf(FachlicheAusnahme.class)
        .satisfies(
            ausnahme ->
                assertThat(((FachlicheAusnahme) ausnahme).titel())
                    .isEqualTo("Letzter Administrator"));
  }

  @Test
  @DisplayName("Der letzte nutzbare ADMIN laesst sich nicht sperren")
  void letzter_admin_nicht_sperrbar() {
    zielIstNutzbarerAdmin();
    nochEinNutzbarerAdmin(false);

    abgelehnt(() -> service.setzeSperre(HANDELNDER, 2L, true, IP));

    verify(appUserRepository, never()).setzeAdminSperre(anyLong(), anyBoolean(), any());
    verify(sitzungsentzug, never()).verwirfAlle(any());
  }

  @Test
  @DisplayName("Der letzte nutzbare ADMIN laesst sich nicht deaktivieren")
  void letzter_admin_nicht_deaktivierbar() {
    zielIstNutzbarerAdmin();
    nochEinNutzbarerAdmin(false);

    abgelehnt(() -> service.setzeAktiv(HANDELNDER, 2L, false, IP));

    verify(appUserRepository, never()).setzeAktiv(anyLong(), anyBoolean(), any());
  }

  @Test
  @DisplayName("Der letzte nutzbare ADMIN laesst sich nicht herabstufen")
  void letzter_admin_nicht_herabstufbar() {
    zielIstNutzbarerAdmin();
    nochEinNutzbarerAdmin(false);

    abgelehnt(() -> service.setzeRolle(HANDELNDER, 2L, "MANDANT", IP));

    verify(appUserRepository, never()).setzeRolle(anyLong(), any(), any());
  }

  /**
   * Die Gegenprobe, ohne die der Test nur bewiese, dass immer abgelehnt wird: Gibt es einen zweiten
   * nutzbaren Administrator, laeuft derselbe Vorgang durch.
   */
  @Test
  @DisplayName("Mit einem zweiten nutzbaren ADMIN laeuft derselbe Vorgang durch")
  void mit_zweitem_admin_laeuft_es_durch() {
    zielIstNutzbarerAdmin();
    nochEinNutzbarerAdmin(true);

    service.setzeSperre(HANDELNDER, 2L, true, IP);

    verify(appUserRepository).setzeAdminSperre(2L, true, JETZT);
    verify(sitzungsentzug).verwirfAlle("it-ziel");
  }

  /**
   * <b>Ein gesperrter zweiter ADMIN zaehlt nicht als nutzbar.</b> Die Zaehlung selbst steht als
   * Bedingung in {@code existiertAndererNutzbarerAdmin}; hier wird festgehalten, dass der Dienst
   * genau diese Frage stellt und keine andere — etwa „gibt es noch einen ADMIN", was einen
   * gesperrten mitzaehlte und den zweiten Admin zum Feigenblatt machte.
   */
  @Test
  @DisplayName("Gefragt wird nach einem NUTZBAREN zweiten ADMIN, nicht nach irgendeinem")
  void gefragt_wird_nach_nutzbarkeit() {
    zielIstNutzbarerAdmin();
    nochEinNutzbarerAdmin(false);

    abgelehnt(() -> service.setzeSperre(HANDELNDER, 2L, true, IP));

    verify(appUserRepository).existiertAndererNutzbarerAdmin(2L);
    verify(appUserRepository, never()).existiertAdmin();
  }

  /**
   * <b>Die Reihenfolge der beiden Stufen, und warum sie einen eigenen Test braucht.</b> Beide
   * Faelle ueberschneiden sich fast immer: Wer als einziger nutzbarer Admin angemeldet ist, trifft
   * mit jedem entwertenden Vorgang auf sich selbst. Die uebrigen Faelle dieser Klasse halten Ziel
   * und Handelnden bewusst auseinander (id 1 gegen id 2) und beruehren die Ueberschneidung deshalb
   * nie — genau hier faellt sie zusammen.
   *
   * <p>Erwartet wird die Meldung der <i>zweiten</i> Stufe: Sie nennt die Bedingung, unter der es
   * ginge („mach zuerst ein anderes Konto zum Administrator"), waehrend „nicht am eigenen Konto"
   * den Nutzer im Dunkeln liesse. Kippte die Reihenfolge, waere die zweite Stufe praktisch
   * unerreichbar und damit ungeprueft.
   */
  @Test
  @DisplayName("Faellt beides zusammen, meldet der letzte Administrator — nicht der Selbstschutz")
  void letzter_admin_geht_dem_selbstschutz_vor() {
    KontoZeile selbst = konto(1L, "it-handelnder", false);
    when(appUserRepository.findeKonto(1L)).thenReturn(Optional.of(selbst));
    when(appUserRepository.existiertAndererNutzbarerAdmin(1L)).thenReturn(false);

    assertThatThrownBy(() -> service.setzeRolle(HANDELNDER, 1L, "MANDANT", IP))
        .isInstanceOf(FachlicheAusnahme.class)
        .satisfies(
            ausnahme ->
                assertThat(((FachlicheAusnahme) ausnahme).titel())
                    .isEqualTo("Letzter Administrator"));

    verify(appUserRepository, never()).setzeRolle(anyLong(), any(), any());
  }

  @Test
  @DisplayName("Ohne den letzten Administrator greift auf dem eigenen Konto der Selbstschutz")
  void selbstschutz_greift_wenn_es_noch_einen_admin_gibt() {
    KontoZeile selbst = konto(1L, "it-handelnder", false);
    when(appUserRepository.findeKonto(1L)).thenReturn(Optional.of(selbst));
    when(appUserRepository.existiertAndererNutzbarerAdmin(1L)).thenReturn(true);

    assertThatThrownBy(() -> service.setzeRolle(HANDELNDER, 1L, "MANDANT", IP))
        .isInstanceOf(FachlicheAusnahme.class)
        .satisfies(
            ausnahme ->
                assertThat(((FachlicheAusnahme) ausnahme).titel())
                    .isEqualTo("Nicht am eigenen Konto"));
  }

  @Test
  @DisplayName(
      "Ein bereits gesperrtes Ziel ist kein nutzbarer ADMIN und faellt nicht unter die Regel")
  void gesperrtes_ziel_faellt_nicht_unter_die_regel() {
    when(appUserRepository.findeKonto(2L)).thenReturn(Optional.of(konto(2L, "it-ziel", true)));

    service.setzeAktiv(HANDELNDER, 2L, false, IP);

    verify(appUserRepository).setzeAktiv(2L, false, JETZT);
    verify(appUserRepository, never()).existiertAndererNutzbarerAdmin(anyLong());
  }

  // ─── Die Baumgliederung (15.09.2026, E26) ──────────────────────────────────────

  /**
   * <b>Der sechste schreibende Vorgang verwirft keine Sitzung</b> (E26). Geschrieben und
   * protokolliert wird trotzdem, mit eigener Ereignisart und altem wie neuem Wert — ein Endpunkt,
   * ein Vorgang, eine Ereignisart gilt weiter.
   */
  @Test
  @DisplayName("Die Baumgliederung wird geschrieben und protokolliert, ohne Sitzungsentzug")
  void baumgliederung_ohne_sitzungsentzug() {
    zielIstNutzbarerAdmin();

    service.setzeBaumgliederung(HANDELNDER, 2L, "PROJEKT", IP);

    verify(appUserRepository).setzeBaumgliederung(2L, Baumgliederung.PROJEKT, JETZT);
    verify(auditLogWriter)
        .schreibe(
            argThat(
                ereignis ->
                    ereignis.typ() == AuditEventType.BAUMGLIEDERUNG_GEAENDERT
                        && ereignis.detail().contains("PARTNER -> PROJEKT")
                        && ereignis.detail().contains("keine Sitzung verworfen")));
    verify(sitzungsentzug, never()).verwirfAlle(any());
  }

  /**
   * <b>Am eigenen Konto: kein Selbstschutz.</b> Die Vorgabe entwertet nichts — der Dienst fragt
   * deshalb nicht einmal nach dem letzten Administrator, und abgemeldet wird niemand.
   */
  @Test
  @DisplayName("Auch am eigenen Konto laeuft sie durch — ohne Selbstschutz, ohne Abmeldung")
  void baumgliederung_am_eigenen_konto() {
    when(appUserRepository.findeKonto(1L))
        .thenReturn(Optional.of(konto(1L, "it-handelnder", false)));

    service.setzeBaumgliederung(HANDELNDER, 1L, "projekt", IP);

    verify(appUserRepository).setzeBaumgliederung(1L, Baumgliederung.PROJEKT, JETZT);
    verify(appUserRepository, never()).existiertAndererNutzbarerAdmin(anyLong());
    verify(sitzungsentzug, never()).verwirfAlle(any());
  }

  /**
   * Ein unbekannter Wert ist {@code 400} — und nicht still die Vorgabe. Geschrieben wird nichts.
   */
  @Test
  @DisplayName("Ein unbekannter Wert ist 400 und schreibt nichts")
  void unbekannte_baumgliederung() {
    zielIstNutzbarerAdmin();

    assertThatThrownBy(() -> service.setzeBaumgliederung(HANDELNDER, 2L, "SOS", IP))
        .isInstanceOf(FachlicheAusnahme.class)
        .satisfies(
            ausnahme ->
                assertThat(((FachlicheAusnahme) ausnahme).titel())
                    .isEqualTo("Gliederung unbekannt"));

    verify(appUserRepository, never()).setzeBaumgliederung(anyLong(), any(), any());
    verify(auditLogWriter, never()).schreibe(any());
  }
}
