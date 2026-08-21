package de.kraftwerkone.overlord.monitor.security;

import java.util.Map;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Verwirft die serverseitigen Sitzungen eines Kontos — <b>fremde</b> Sitzungen, nicht die eigene
 * der laufenden Anfrage. Dafuer ist {@link SitzungsVerwaltung} zustaendig.
 *
 * <p><b>Das ist die Einloesung der Zusage, mit der dieses Projekt JWT abgelehnt hat:</b> „bei
 * externen Nutzern wiegt sofortige Ruecknehmbarkeit schwerer als Zustandslosigkeit". Ohne Entzug
 * waere eine Sperre erst beim naechsten Anmelden wirksam — also genau dann nicht, wenn sie
 * gebraucht wird. Alle fuenf schreibenden Vorgaenge der Benutzerverwaltung rufen hier durch (E5):
 * sperren, deaktivieren, Rolle aendern, Mandanten aendern, Passwort zuruecksetzen. <b>Eine Regel,
 * keine Fallunterscheidung.</b>
 *
 * <p><b>Warum das ueber {@code PRINCIPAL_NAME} geht und nicht ueber einen eigenen Zaehler.</b> Die
 * Rueckfallebene aus E7 waere eine Spalte {@code sitzungs_generation} an {@code app_user}, je
 * Anfrage gegen die Sitzung geprueft — eine Datenbankabfrage bei <i>jedem</i> Aufruf als Preis
 * dafuer, die Spring-Session-Interna nicht anzufassen. Sie ist nicht gebaut worden, weil die vier
 * Punkte aus E7 am laufenden System <b>gemessen</b> sind und alle vier halten (M81, {@code
 * SitzungssucheDbIT}): Der Index auf {@code SPRING_SESSION.PRINCIPAL_NAME} existiert, die Anmeldung
 * ueber den eigenen Controller fuellt ihn, das Repository ist als {@link
 * FindByIndexNameSessionRepository} injizierbar, und der Wert stimmt zeichengenau mit {@code
 * app_user.username} ueberein.
 *
 * <p><b>Der Typ des Feldes ist kein Zufall.</b> {@code JdbcIndexedSessionRepository$JdbcSession}
 * ist paketprivat; ueber die Schnittstelle mit {@code ? extends Session} ist der Rueckgabetyp
 * benennbar, ueber die konkrete Klasse nicht.
 *
 * <p><b>Eine stille Falle, die dieser Klasse gilt:</b> {@code findByIndexNameAndIndexValue} liefert
 * bei einem unbekannten Indexnamen eine <i>leere Map</i> statt einer Ausnahme. Ein Entzug, der
 * nichts findet, sieht deshalb genauso aus wie einer, der nichts zu tun hatte. Genau deshalb geht
 * die Zahl verworfener Sitzungen in das ausloesende Protokollereignis (E15) — sie ist die einzige
 * Stelle, an der ein reihenweise wirkungsloser Entzug auffallen wuerde.
 */
@Component
public class Sitzungsentzug {

  private final FindByIndexNameSessionRepository<? extends Session> sitzungen;

  Sitzungsentzug(FindByIndexNameSessionRepository<? extends Session> sitzungen) {
    this.sitzungen = sitzungen;
  }

  /**
   * Verwirft <b>alle</b> Sitzungen des Kontos und liefert deren Zahl.
   *
   * @param username der Benutzername aus {@code app_user}, nicht der eingetippte. {@code
   *     SPRING_SESSION.PRINCIPAL_NAME} traegt die Schreibweise der Datenbankzeile, weil das
   *     Principal aus der gefundenen Zeile gebaut wird — dieselbe Person haette sonst je nach
   *     Tippweise mehrere Namen im Index (M81 d)
   */
  public int verwirfAlle(String username) {
    return verwirf(username, null);
  }

  /**
   * Verwirft die <b>uebrigen</b> Sitzungen des Kontos und behaelt die angegebene (E6).
   *
   * <p>Der Fall ist die eigene Passwortaenderung: Wer sein Passwort aendert, weil er einen fremden
   * Zugriff vermutet, will die fremden Sitzungen los sein und nicht sich selbst. Die
   * Ungleichbehandlung zum Admin-Reset — der <b>alle</b> verwirft — ist damit begruendet und nicht
   * vergessen.
   *
   * <p><b>Der Aufrufer muss die Sitzungs-ID lesen, bevor er sie erneuert.</b> {@code
   * SitzungsVerwaltung.erneuere} tauscht sie aus; wuerde hier die <i>alte</i> ID geschont,
   * verwuerfe der Entzug die eben entstandene neue mit — und der Nutzer waere nach der
   * Passwortaenderung abgemeldet.
   *
   * @param behalteSitzungsId die rohe Sitzungs-ID aus {@code HttpSession#getId()}, <b>nicht</b> der
   *     Cookie-Wert: Spring Session kodiert ihn Base64
   */
  public int verwirfUebrige(String username, String behalteSitzungsId) {
    return verwirf(username, behalteSitzungsId);
  }

  private int verwirf(String username, String behalteSitzungsId) {
    Map<String, ? extends Session> gefundene = sitzungen.findByPrincipalName(username);
    int verworfen = 0;
    for (String sitzungsId : gefundene.keySet()) {
      if (sitzungsId.equals(behalteSitzungsId)) {
        continue;
      }
      sitzungen.deleteById(sitzungsId);
      verworfen++;
    }
    return verworfen;
  }
}
