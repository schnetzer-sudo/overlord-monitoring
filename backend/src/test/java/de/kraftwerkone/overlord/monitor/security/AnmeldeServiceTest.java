package de.kraftwerkone.overlord.monitor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.AnmeldungAbgelehntException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Sperrlogik und Auskunftsdisziplin — ohne Datenbank.
 *
 * <p>Geprueft wird beim Dummy-Vergleich <b>der Aufruf</b>, nicht die Laufzeit: Ein Zeitvergleich
 * waere auf einer Build-Maschine unzuverlaessig und der Test damit schlimmer als keiner.
 */
class AnmeldeServiceTest {

  private static final String IP = "203.0.113.7";
  private static final LocalDateTime JETZT = LocalDateTime.of(2026, 7, 29, 10, 0);

  private AppUserRepository appUserRepository;
  private MandantRepository mandantRepository;
  private AnmeldeSperre anmeldeSperre;
  private PasswordEncoder passwortKodierer;
  private AuditLogWriter auditLogWriter;
  private AnmeldeService service;

  @BeforeEach
  void aufsetzen() {
    appUserRepository = mock(AppUserRepository.class);
    mandantRepository = mock(MandantRepository.class);
    anmeldeSperre = mock(AnmeldeSperre.class);
    passwortKodierer = mock(PasswordEncoder.class);
    auditLogWriter = mock(AuditLogWriter.class);
    when(passwortKodierer.encode(anyString())).thenReturn("$2a$12$dummy");
    Clock uhr = Clock.fixed(JETZT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    service =
        new AnmeldeService(
            appUserRepository,
            mandantRepository,
            anmeldeSperre,
            passwortKodierer,
            auditLogWriter,
            uhr);
  }

  private AppUserZeile nutzer(int fehlversuche, LocalDateTime gesperrtBis) {
    return new AppUserZeile(
        7L, "lukas", "$2a$12$echt", Rolle.MANDANT, true, false, true, fehlversuche, gesperrtBis);
  }

  private void kontoVorhanden(AppUserZeile zeile) {
    when(appUserRepository.findeNachBenutzername("lukas")).thenReturn(Optional.of(zeile));
  }

  private void passwortStimmt(boolean stimmt) {
    when(passwortKodierer.matches(anyString(), anyString())).thenReturn(stimmt);
  }

  @Test
  @DisplayName("Der fuenfte Fehlversuch sperrt das Konto")
  void fuenfter_fehlversuch_sperrt() {
    kontoVorhanden(nutzer(4, null));
    passwortStimmt(false);

    assertThatThrownBy(() -> service.anmelden("lukas", "falsch", IP))
        .isInstanceOf(AnmeldungAbgelehntException.class);

    verify(appUserRepository)
        .setzeFehlversuche(7L, 5, JETZT.plus(AnmeldeService.SPERRDAUER), JETZT);
    assertThat(protokollierteTypen()).contains(AuditEventType.KONTO_GESPERRT);
  }

  @Test
  @DisplayName("Der vierte Fehlversuch sperrt noch nicht")
  void vierter_fehlversuch_sperrt_nicht() {
    kontoVorhanden(nutzer(3, null));
    passwortStimmt(false);

    assertThatThrownBy(() -> service.anmelden("lukas", "falsch", IP))
        .isInstanceOf(AnmeldungAbgelehntException.class);

    verify(appUserRepository).setzeFehlversuche(7L, 4, null, JETZT);
    assertThat(protokollierteTypen()).doesNotContain(AuditEventType.KONTO_GESPERRT);
  }

  @Test
  @DisplayName("Eine erfolgreiche Anmeldung setzt den Zaehler zurueck")
  void erfolgreiche_anmeldung_setzt_zurueck() {
    kontoVorhanden(nutzer(4, null));
    passwortStimmt(true);
    when(mandantRepository.findeFuerNutzer(7L))
        .thenReturn(List.of(new MandantResponse("VOTG", "VOTG Tanktainer GmbH")));

    AnmeldeService.Ergebnis ergebnis = service.anmelden("lukas", "richtig", IP);

    assertThat(ergebnis.nutzer().username()).isEqualTo("lukas");
    assertThat(ergebnis.aktiverMandant()).isEqualTo("VOTG");
    verify(appUserRepository).merkeAnmeldung(7L, JETZT);
    verify(anmeldeSperre).zuruecksetzen(IP);
    verify(appUserRepository, never())
        .setzeFehlversuche(anyLong(), any(Integer.class), any(), any());
  }

  @Test
  @DisplayName("Eine abgelaufene Sperre setzt den Zaehler zurueck, statt sofort neu zu sperren")
  void abgelaufene_sperre_setzt_zaehler_zurueck() {
    kontoVorhanden(nutzer(5, JETZT.minusMinutes(1)));
    passwortStimmt(false);

    assertThatThrownBy(() -> service.anmelden("lukas", "falsch", IP))
        .isInstanceOf(AnmeldungAbgelehntException.class);

    verify(appUserRepository).setzeFehlversuche(7L, 1, null, JETZT);
  }

  @Test
  @DisplayName("Ein unbekannter Benutzername rechnet trotzdem einen BCrypt-Vergleich")
  void unbekannter_benutzername_rechnet_bcrypt() {
    when(appUserRepository.findeNachBenutzername("gibtesnicht")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.anmelden("gibtesnicht", "geheim", IP))
        .isInstanceOf(AnmeldungAbgelehntException.class);

    // Der Aufruf ist der Nachweis: ohne ihn antwortete der unbekannte Fall in zwei statt
    // zweihundert Millisekunden und Benutzernamen liessen sich ueber die Laufzeit finden.
    verify(passwortKodierer).matches(eq("geheim"), anyString());
  }

  @Test
  @DisplayName("Ein Konto ohne Passwort-Hash rechnet trotzdem einen BCrypt-Vergleich")
  void konto_ohne_hash_rechnet_bcrypt() {
    kontoVorhanden(new AppUserZeile(7L, "lukas", null, Rolle.MANDANT, true, false, true, 0, null));

    assertThatThrownBy(() -> service.anmelden("lukas", "geheim", IP))
        .isInstanceOf(AnmeldungAbgelehntException.class);

    verify(passwortKodierer).matches(eq("geheim"), anyString());
  }

  @Test
  @DisplayName("Richtiges Passwort auf gesperrtem Konto darf den Grund benennen")
  void gesperrtes_konto_nennt_den_grund() {
    kontoVorhanden(nutzer(5, JETZT.plusMinutes(10)));
    passwortStimmt(true);

    assertThatThrownBy(() -> service.anmelden("lukas", "richtig", IP))
        .isInstanceOfSatisfying(
            AnmeldungAbgelehntException.class,
            ex -> assertThat(ex.problemTyp()).isEqualTo("konto-gesperrt"));
    // Der Zaehler wird dabei NICHT erhoeht: sonst verlaengerte der berechtigte Nutzer
    // seine eigene Sperre, indem er es nochmal versucht.
    verify(appUserRepository, never())
        .setzeFehlversuche(anyLong(), any(Integer.class), any(), any());
  }

  @Test
  @DisplayName("Falsches Passwort auf gesperrtem Konto bleibt unspezifisch")
  void falsches_passwort_auf_gesperrtem_konto_bleibt_unspezifisch() {
    kontoVorhanden(nutzer(5, JETZT.plusMinutes(10)));
    passwortStimmt(false);

    assertThatThrownBy(() -> service.anmelden("lukas", "falsch", IP))
        .isInstanceOfSatisfying(
            AnmeldungAbgelehntException.class,
            // Wer das Passwort nicht kennt, erfaehrt auch nicht, dass das Konto existiert.
            ex -> assertThat(ex.problemTyp()).isEqualTo("anmeldung-abgelehnt"));
  }

  @Test
  @DisplayName("Richtiges Passwort auf deaktiviertem Konto darf den Grund benennen")
  void deaktiviertes_konto_nennt_den_grund() {
    kontoVorhanden(
        new AppUserZeile(7L, "lukas", "$2a$12$echt", Rolle.MANDANT, false, false, true, 0, null));
    passwortStimmt(true);

    assertThatThrownBy(() -> service.anmelden("lukas", "richtig", IP))
        .isInstanceOfSatisfying(
            AnmeldungAbgelehntException.class,
            ex -> assertThat(ex.problemTyp()).isEqualTo("konto-deaktiviert"));
  }

  @Test
  @DisplayName("Ein Fehlversuch auf unbekannten Namen wird ohne actor_user_id protokolliert")
  void fehlversuch_auf_unbekannten_namen_ohne_id() {
    when(appUserRepository.findeNachBenutzername("gibtesnicht")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.anmelden("gibtesnicht", "geheim", IP))
        .isInstanceOf(AnmeldungAbgelehntException.class);

    ArgumentCaptor<AuditEvent> ereignis = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditLogWriter).schreibe(ereignis.capture());
    assertThat(ereignis.getValue().typ()).isEqualTo(AuditEventType.ANMELDUNG_FEHLVERSUCH);
    assertThat(ereignis.getValue().actorUserId()).isNull();
    assertThat(ereignis.getValue().actorUsername()).isEqualTo("gibtesnicht");
  }

  @Test
  @DisplayName("Ein ADMIN bekommt beim Anmelden keinen Mandanten gesetzt")
  void admin_bekommt_keinen_mandanten() {
    when(appUserRepository.findeNachBenutzername("admin"))
        .thenReturn(
            Optional.of(
                new AppUserZeile(
                    7L, "admin", "$2a$12$echt", Rolle.ADMIN, true, false, true, 0, null)));
    passwortStimmt(true);

    AnmeldeService.Ergebnis ergebnis = service.anmelden("admin", "richtig", IP);

    assertThat(ergebnis.aktiverMandant()).isNull();
    verify(mandantRepository, never()).findeFuerNutzer(anyLong());
  }

  @Test
  @DisplayName("Bei mehreren zulaessigen Mandanten bleibt die Auswahl offen")
  void mehrere_mandanten_bleiben_offen() {
    kontoVorhanden(nutzer(0, null));
    passwortStimmt(true);
    when(mandantRepository.findeFuerNutzer(7L))
        .thenReturn(
            List.of(
                new MandantResponse("IBIS", "IBIS GmbH"),
                new MandantResponse("IBISGUS", "IBIS GmbH mit GUS WW")));

    assertThat(service.anmelden("lukas", "richtig", IP).aktiverMandant()).isNull();
  }

  @Test
  @DisplayName("Die IP-Begrenzung wird vor allem anderen geprueft")
  void ip_begrenzung_zuerst() {
    org.mockito.Mockito.doThrow(
            new de.kraftwerkone.overlord.monitor.common.error.ZuVieleAnmeldeversucheException(
                "Test"))
        .when(anmeldeSperre)
        .pruefe(IP);

    assertThatThrownBy(() -> service.anmelden("lukas", "richtig", IP))
        .isInstanceOf(
            de.kraftwerkone.overlord.monitor.common.error.ZuVieleAnmeldeversucheException.class);

    verify(appUserRepository, never()).findeNachBenutzername(anyString());
  }

  @Test
  @DisplayName("Ein Fehlversuch zaehlt auch gegen die IP-Begrenzung")
  void fehlversuch_zaehlt_gegen_ip() {
    when(appUserRepository.findeNachBenutzername(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.anmelden("wer", "auch", IP))
        .isInstanceOf(AnmeldungAbgelehntException.class);

    verify(anmeldeSperre).zaehleFehlversuch(IP);
    // Und niemals in die Datenbank: die Begrenzung liegt ausschliesslich im Arbeitsspeicher.
    verify(appUserRepository, never())
        .setzeFehlversuche(anyLong(), any(Integer.class), isNull(), any());
  }

  private List<AuditEventType> protokollierteTypen() {
    ArgumentCaptor<AuditEvent> ereignisse = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditLogWriter, org.mockito.Mockito.atLeastOnce()).schreibe(ereignisse.capture());
    return ereignisse.getAllValues().stream().map(AuditEvent::typ).toList();
  }
}
