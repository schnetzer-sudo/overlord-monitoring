package de.kraftwerkone.overlord.monitor.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Service aus einem Abruf macht — <b>ohne Datenbank und ohne Filestore</b>.
 *
 * <p>{@link Ablagezugriff} ist eine Schnittstelle, damit genau das geht: Kein Test dieses Projekts
 * baut eine Verbindung nach draussen. Was hier zurueckgegeben wird, ist erfunden.
 *
 * <p>Der Schwerpunkt liegt auf den drei Zusagen, die man nicht am Text eines Statements sieht:
 * <b>die Rolle kommt aus der Sitzung</b>, <b>Anzeige und Download liefern dasselbe</b>, und
 * <b>jeder Zustand ist benannt</b>.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArtefaktServiceTest {

  private static final MandantContext MANDANT = new MandantContext("VOTG");
  private static final String MESSAGE_ID = "eine-erfundene-nachricht";
  private static final String VERBINDUNG = "http://beispielknoten.invalid/WebApplication/Receiver";

  private static final AngemeldeterNutzer MANDANT_NUTZER =
      new AngemeldeterNutzer(1L, "it-mandant", Rolle.MANDANT, false, true);
  private static final AngemeldeterNutzer ADMIN_NUTZER =
      new AngemeldeterNutzer(2L, "it-admin", Rolle.ADMIN, false, true);

  /** Ein erfundenes Protokoll mit vollstaendigem Markenpaar und einem maskierbaren Pfad. */
  private static final String PROTOKOLL =
      String.join(
          "\n",
          "aussen: /opt/txp/users/ERFUNDENESKONTO/aussen.dat",
          "***StartOfLog***",
          "innen: /opt/txp/users/ERFUNDENESKONTO/innen.dat",
          "innen: zweite Zeile",
          "***EndOfLog***",
          "danach: nicht mehr sichtbar");

  private static final String PROTOKOLL_OHNE_MARKEN =
      "2025-12-29 10:00:00 ERFUNDEN-Verbindung aufgebaut\n";

  @Mock private ArtefaktRepository repository;
  @Mock private AuditLogWriter auditLogWriter;

  private final ErfundeneAblage ablage = new ErfundeneAblage();
  private ArtefaktService service;

  private static final RohdatenEigenschaften GRENZEN =
      new RohdatenEigenschaften(
          8L * 1024 * 1024, 1024L * 1024, Duration.ofSeconds(5), Duration.ofSeconds(15));

  @BeforeEach
  void aufbauen() {
    service = new ArtefaktService(repository, ablage, auditLogWriter, GRENZEN);
    when(repository.findeVerbindung(any(), anyString(), anyString()))
        .thenReturn(Optional.of(VERBINDUNG));
    when(repository.findeOriginaldateiname(any(), anyString(), anyShort()))
        .thenReturn(Optional.empty());
  }

  /** Eine Attrappe der Ablage. Sie spricht nichts an und merkt sich, wonach gefragt wurde. */
  private static final class ErfundeneAblage implements Ablagezugriff {

    private Abrufergebnis antwort = Abrufergebnis.nichtVorhanden();
    private final List<String> gefragteVerbindungen = new ArrayList<>();
    private final List<String> gefragteUuids = new ArrayList<>();

    void liefert(String inhalt) {
      this.antwort = Abrufergebnis.geholt(alsZip(inhalt));
    }

    void liefert(Abrufergebnis ergebnis) {
      this.antwort = ergebnis;
    }

    @Override
    public Abrufergebnis hole(String verbindung, String uuid) {
      gefragteVerbindungen.add(verbindung);
      gefragteUuids.add(uuid);
      return antwort;
    }
  }

  private static byte[] alsZip(String inhalt) {
    return alsZip(inhalt.getBytes(StandardCharsets.ISO_8859_1));
  }

  private static byte[] alsZip(byte[]... eintraege) {
    ByteArrayOutputStream gesammelt = new ByteArrayOutputStream();
    try (ZipOutputStream strom = new ZipOutputStream(gesammelt)) {
      for (int i = 0; i < eintraege.length; i++) {
        strom.putNextEntry(new ZipEntry("erfunden-" + i + ".dat"));
        strom.write(eintraege[i]);
        strom.closeEntry();
      }
    } catch (IOException unmoeglich) {
      throw new UncheckedIOException(unmoeglich);
    }
    return gesammelt.toByteArray();
  }

  private void artefakte(Artefaktzeile... zeilen) {
    when(repository.findeArtefakte(any(), anyString())).thenReturn(List.of(zeilen));
    when(repository.existiert(any(), anyString())).thenReturn(zeilen.length > 0);
  }

  private static Artefaktzeile protokoll(short schritt) {
    return new Artefaktzeile(
        "FileReader.Log.GUID", schritt, "BEISPIELPROD42|deadbeef-0000-4000-8000-0123456789ab");
  }

  private static Artefaktzeile nutzdatei(short schritt) {
    return new Artefaktzeile(
        "FileReader.Payload.GUID", schritt, "BEISPIELPROD42|deadbeef-0000-4000-8000-0123456789ac");
  }

  // Es gibt hier keine Zeile fuer Message.Payload.GUID mehr. Sie kaeme im Betrieb nie an: Das
  // Repository laesst sie seit dem 19.08.2026 nicht durch (M73, Artefaktnamen#NAME_ZEIGER), und
  // eine Attrappe, die sie trotzdem liefert, pruefte einen Zustand, den es nicht gibt. Belegt ist
  // der Ausschluss dort, wo er stattfindet: ArtefaktStatementsTest.

  // ─── Die Liste ────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Artefaktliste")
  class Liste {

    /**
     * <b>Zweigeteilt: Nutzdaten und Protokolle.</b> Das dritte Feld {@code eingang} ist am
     * 19.08.2026 entfallen (M73) — es fuehrte {@code Message.Payload.GUID} als „die eingegangene
     * Datei", und der Name zeigt in beiden gemessenen Fenstern ohne Gegenfall auf die
     * Nutzdatenzeile mit dem hoechsten {@code MessageActionID} derselben Nachricht.
     *
     * <p>Die Trennlinie ist damit die einzige, die aus dem Datenmodell folgt: Der Beschnitt greift
     * ausschliesslich bei Protokollen. <b>Schritt {@code 0} ist keine Trennlinie</b> — die Zeilen
     * des Lesedienstes liegen dort und stehen in denselben beiden Listen wie alle anderen.
     */
    @Test
    @DisplayName("Zweigeteilt: Nutzdaten und Protokolle, ueber alle Schritte")
    void zweigeteilt() {
      artefakte(
          nutzdatei((short) 0),
          protokoll((short) 0),
          nutzdatei((short) 1),
          protokoll((short) 1),
          protokoll((short) 2));

      ArtefaktlisteResponse antwort = service.liste(MANDANT, Rolle.MANDANT, MESSAGE_ID);

      assertThat(antwort.nutzdaten()).hasSize(2);
      assertThat(antwort.protokolle()).hasSize(3);
      assertThat(antwort.nutzdaten())
          .as("Schritt 0 bekommt keinen Sonderplatz mehr — er steht in derselben Liste")
          .anyMatch(a -> a.schritt() == 0);
    }

    @Test
    @DisplayName("Sie traegt keinen Verweis — weder GUID noch Ablagenkennung")
    void ohne_verweis() {
      artefakte(nutzdatei((short) 0), protokoll((short) 1));

      ArtefaktlisteResponse antwort = service.liste(MANDANT, Rolle.MANDANT, MESSAGE_ID);

      assertThat(antwort.toString())
          .doesNotContain("BEISPIELPROD42")
          .doesNotContain("deadbeef-0000-4000-8000");
    }

    @Test
    @DisplayName("beschnittMoeglich haengt an Rolle und Art, nicht an einem Parameter")
    void beschnitt_angekuendigt() {
      artefakte(nutzdatei((short) 1), protokoll((short) 1));

      ArtefaktlisteResponse fuerMandant = service.liste(MANDANT, Rolle.MANDANT, MESSAGE_ID);
      ArtefaktlisteResponse fuerAdmin = service.liste(MANDANT, Rolle.ADMIN, MESSAGE_ID);

      assertThat(fuerMandant.protokolle().getFirst().beschnittMoeglich()).isTrue();
      assertThat(fuerMandant.nutzdaten().getFirst().beschnittMoeglich()).isFalse();
      assertThat(fuerAdmin.protokolle().getFirst().beschnittMoeglich()).isFalse();
    }

    @Test
    @DisplayName("Sie spricht die Ablage nicht an")
    void ohne_abruf() {
      artefakte(nutzdatei((short) 0), protokoll((short) 1));

      service.liste(MANDANT, Rolle.MANDANT, MESSAGE_ID);

      assertThat(ablage.gefragteVerbindungen)
          .as("Drei bis fuenfzehn SOAP-Aufrufe je Liste (M55) waeren der falsche Preis")
          .isEmpty();
    }

    @Test
    @DisplayName("Eine unsichtbare Nachricht ist 404 — keine leere Liste")
    void unsichtbar_ist_404() {
      when(repository.findeArtefakte(any(), anyString())).thenReturn(List.of());
      when(repository.existiert(any(), anyString())).thenReturn(false);

      assertThatThrownBy(() -> service.liste(MANDANT, Rolle.MANDANT, MESSAGE_ID))
          .isInstanceOf(RessourceNichtGefundenException.class);
    }
  }

  // ─── Die Rolle ────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Rolle kommt aus der Sitzung")
  class Rollenherkunft {

    @Test
    @DisplayName("MANDANT sieht nur den Innenbereich, maskiert")
    void mandant_sieht_beschnitten() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      AnzeigeResponse antwort =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");

      assertThat(antwort.beschnitten()).isTrue();
      assertThat(antwort.zustand()).isEqualTo(Artefaktzustand.ANZEIGBAR);
      assertThat(antwort.text()).isEqualTo("innen: /IS/innen.dat\ninnen: zweite Zeile");
      assertThat(antwort.text()).doesNotContain("aussen").doesNotContain("danach");
    }

    @Test
    @DisplayName("ADMIN sieht die vollstaendige, unmaskierte Datei")
    void admin_sieht_alles() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      AnzeigeResponse antwort =
          service.anzeige(MANDANT, ADMIN_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");

      assertThat(antwort.beschnitten()).isFalse();
      assertThat(antwort.text()).isEqualTo(PROTOKOLL);
      assertThat(antwort.text())
          .as("ADMIN bekommt unmaskiert — identisch zum Altsystem (Q3)")
          .contains("/opt/txp/users/ERFUNDENESKONTO/");
    }

    @Test
    @DisplayName("Der Service nimmt keine Rolle entgegen — nur den angemeldeten Nutzer")
    void keine_rolle_als_parameter() {
      // Die Signatur ist der Nachweis: anzeige(...) und download(...) bekommen einen
      // AngemeldeterNutzer aus dem SecurityContext, keinen Rollennamen und keinen
      // "downloadUser"-Parameter wie das Altsystem (JsonServlet.java:788–:790).
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      AnzeigeResponse alsMandant =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");
      AnzeigeResponse alsAdmin =
          service.anzeige(MANDANT, ADMIN_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");

      assertThat(alsMandant.text()).isNotEqualTo(alsAdmin.text());
    }

    @Test
    @DisplayName("Nutzdaten werden nie beschnitten, auch nicht fuer MANDANT")
    void nutzdaten_nie_beschnitten() {
      artefakte(nutzdatei((short) 1));
      ablage.liefert(PROTOKOLL);

      AnzeigeResponse antwort =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(antwort.beschnitten()).isFalse();
      assertThat(antwort.text()).isEqualTo(PROTOKOLL);
    }
  }

  // ─── Der Gleichlauf ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Gleichlauf von Anzeige und Download (Entscheidung 9)")
  class Gleichlauf {

    @Test
    @DisplayName("MANDANT bekommt auch als Datei nur den beschnittenen Ausschnitt")
    void mandant_download_ist_beschnitten() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");
      ArtefaktService.Download download =
          service.download(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");

      String alsText = new String(download.bytes(), StandardCharsets.ISO_8859_1);
      assertThat(alsText).isEqualTo(anzeige.text());
      assertThat(alsText)
          .as("Es gibt keinen Pfad, auf dem ein Mandantennutzer die vollstaendige Datei bekommt")
          .doesNotContain("aussen")
          .doesNotContain("danach")
          .doesNotContain("ERFUNDENESKONTO");
    }

    @Test
    @DisplayName("ADMIN bekommt die Bytes des ZIP-Eintrags unveraendert")
    void admin_download_ist_vollstaendig() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      ArtefaktService.Download download =
          service.download(MANDANT, ADMIN_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");

      assertThat(download.bytes()).isEqualTo(PROTOKOLL.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Der Weg, auf dem der Gleichlauf am ehesten bricht: Wird ein <b>Protokoll</b> als binaer
     * eingestuft, darf {@code MANDANT} trotzdem nicht die vollstaendige Datei bekommen.
     *
     * <p>Die Einstufung haengt am Inhalt, und der Inhalt eines Protokolls stammt teilweise aus der
     * EDI-Datei des Partners — ein einziges Nullbyte in einem echoten Wert genuegte. Dass 0 % der
     * gemessenen Protokolle binaer sind (M61), ist eine Beobachtung an acht Dateien und keine
     * Zusage.
     */
    @Test
    @DisplayName("Ein binaeres Protokoll oeffnet MANDANT keinen Weg an dem Beschnitt vorbei")
    void binaeres_protokoll_umgeht_den_beschnitt_nicht() {
      artefakte(protokoll((short) 1));
      byte[] binaeresProtokoll =
          (PROTOKOLL + "\0" + "\0".repeat(200)).getBytes(StandardCharsets.ISO_8859_1);
      ablage.liefert(Abrufergebnis.geholt(alsZip(binaeresProtokoll)));

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");

      assertThat(anzeige.zustand()).isEqualTo(Artefaktzustand.BINAERDATEI);
      assertThat(anzeige.text()).isEmpty();
      assertThat(anzeige.beschnitten())
          .as("Der Beschnitt galt fuer dieses Artefakt und diesen Nutzer — er faellt nicht weg")
          .isTrue();

      assertThatThrownBy(
              () ->
                  service.download(
                      MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1"))
          .as(
              "Bekaeme MANDANT hier Bytes, waere es die vollstaendige, unmaskierte Protokolldatei"
                  + " — genau der Pfad, den Entscheidung 9 ausschliesst")
          .isInstanceOf(AbrufFehlgeschlagenException.class)
          .hasMessageContaining("BINAERDATEI");
    }

    @Test
    @DisplayName("ADMIN bekommt dasselbe binaere Protokoll dagegen als Datei")
    void binaeres_protokoll_fuer_admin() {
      artefakte(protokoll((short) 1));
      byte[] binaeresProtokoll =
          (PROTOKOLL + "\0" + "\0".repeat(200)).getBytes(StandardCharsets.ISO_8859_1);
      ablage.liefert(Abrufergebnis.geholt(alsZip(binaeresProtokoll)));

      ArtefaktService.Download download =
          service.download(MANDANT, ADMIN_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");

      assertThat(download.bytes())
          .as("Fuer ADMIN gibt es keinen Beschnitt, an dem etwas vorbeifuehren koennte")
          .isEqualTo(binaeresProtokoll);
    }

    @Test
    @DisplayName("Eine binaere Nutzdatei bleibt fuer beide Rollen herunterladbar")
    void binaere_nutzdatei_bleibt_ladbar() {
      artefakte(nutzdatei((short) 1));
      byte[] binaer = new byte[] {0x41, 0x00, 0x42};
      ablage.liefert(Abrufergebnis.geholt(alsZip(binaer)));

      assertThat(
              service
                  .download(
                      MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1")
                  .bytes())
          .as("Nutzdaten werden nie beschnitten — hier gibt es nichts zu umgehen")
          .isEqualTo(binaer);
    }

    @Test
    @DisplayName("Ohne anzeigbaren Teil gibt es auch keinen Download")
    void ohne_teil_kein_download() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL_OHNE_MARKEN);

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1");
      assertThat(anzeige.zustand()).isEqualTo(Artefaktzustand.KEIN_ANZEIGBARER_PROTOKOLLTEIL);

      assertThatThrownBy(
              () ->
                  service.download(
                      MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "127.0.0.1"))
          .isInstanceOf(AbrufFehlgeschlagenException.class)
          .hasMessageContaining("KEIN_ANZEIGBARER_PROTOKOLLTEIL");
    }
  }

  // ─── Die Zustaende ────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die vier Zustaende")
  class Zustaende {

    @Test
    @DisplayName("Binaerdatei: benannt, nicht angezeigt — aber herunterladbar")
    void binaer() {
      artefakte(nutzdatei((short) 1));
      byte[] binaer = new byte[] {0x41, 0x00, 0x42, 0x00, 0x43};
      ablage.liefert(Abrufergebnis.geholt(alsZip(binaer)));

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.zustand()).isEqualTo(Artefaktzustand.BINAERDATEI);
      assertThat(anzeige.text()).isEmpty();
      assertThat(anzeige.groesseBytes()).isEqualTo(5);

      ArtefaktService.Download download =
          service.download(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");
      assertThat(download.bytes()).isEqualTo(binaer);
    }

    @Test
    @DisplayName("Datei nicht vorhanden: die Ablage antwortet, liefert aber nichts")
    void nicht_vorhanden() {
      artefakte(nutzdatei((short) 1));
      ablage.liefert(Abrufergebnis.nichtVorhanden());

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.zustand()).isEqualTo(Artefaktzustand.DATEI_NICHT_VORHANDEN);
      assertThat(anzeige.text()).isEmpty();
    }

    @Test
    @DisplayName("Ablage nicht erreichbar, wenn die Kennung nicht aufloest — ohne Rueckfall")
    void kennung_loest_nicht_auf() {
      artefakte(nutzdatei((short) 1));
      when(repository.findeVerbindung(any(), anyString(), anyString()))
          .thenReturn(Optional.empty());

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.zustand()).isEqualTo(Artefaktzustand.ABLAGE_NICHT_ERREICHBAR);
      assertThat(ablage.gefragteVerbindungen)
          .as("Kein Rueckfall auf eine andere Ablage: 20 von 20 Kreuzabrufen scheitern (M68)")
          .isEmpty();
    }

    @Test
    @DisplayName("Ein unzerlegbarer Verweis ebenso — und ohne Abruf")
    void verweis_unzerlegbar() {
      artefakte(new Artefaktzeile("FileReader.Payload.GUID", (short) 1, "ohne-pipe"));

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.zustand()).isEqualTo(Artefaktzustand.ABLAGE_NICHT_ERREICHBAR);
      assertThat(ablage.gefragteVerbindungen).isEmpty();
    }
  }

  // ─── Abruf und Aufbereitung ───────────────────────────────────────────────────

  @Nested
  @DisplayName("Abruf und Aufbereitung")
  class Aufbereitung {

    @Test
    @DisplayName("Gesendet wird die nackte UUID hinter der Pipe, kein Verweis (Q1)")
    void nackte_uuid() {
      artefakte(nutzdatei((short) 1));
      ablage.liefert("Erfundener Inhalt");

      service.anzeige(
          MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(ablage.gefragteUuids).containsExactly("deadbeef-0000-4000-8000-0123456789ac");
      assertThat(ablage.gefragteVerbindungen)
          .as("Der ServiceConnectString geht unveraendert und ohne Anhaengsel hinaus")
          .containsExactly(VERBINDUNG);
    }

    @Test
    @DisplayName("Dekodiert wird mit ISO-8859-1, nicht mit UTF-8")
    void kodierung() {
      artefakte(nutzdatei((short) 1));
      byte[] umlaut = new byte[] {(byte) 0xC4, (byte) 0xD6, (byte) 0xDC}; // AeOeUe in ISO-8859-1
      ablage.liefert(Abrufergebnis.geholt(alsZip(umlaut)));

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.kodierung()).isEqualTo("ISO-8859-1");
      assertThat(anzeige.text())
          .as("Als UTF-8 gelesen waeren diese drei Bytes ungueltig (M61)")
          .isEqualTo("ÄÖÜ");
    }

    @Test
    @DisplayName("Zwei ZIP-Eintraege: der erste wird verwendet, die Zahl steht in der Antwort")
    void zwei_zip_eintraege() {
      artefakte(nutzdatei((short) 1));
      ablage.liefert(
          Abrufergebnis.geholt(
              alsZip(
                  "erster".getBytes(StandardCharsets.ISO_8859_1),
                  "zweiter".getBytes(StandardCharsets.ISO_8859_1))));

      AnzeigeResponse anzeige =
          service.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.zipEintraege())
          .as("Nicht stillschweigend verworfen wie im Altsystem (:801–:802)")
          .isEqualTo(2);
      assertThat(anzeige.text()).isEqualTo("erster");
    }

    @Test
    @DisplayName("Die Anzeige wird gekappt, der Download bleibt vollstaendig")
    void kappung() {
      ArtefaktService klein =
          new ArtefaktService(
              repository,
              ablage,
              auditLogWriter,
              new RohdatenEigenschaften(1024, 10, Duration.ofSeconds(5), Duration.ofSeconds(15)));
      artefakte(nutzdatei((short) 1));
      ablage.liefert("x".repeat(50));

      AnzeigeResponse anzeige =
          klein.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");
      ArtefaktService.Download download =
          klein.download(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.gekuerzt()).isTrue();
      assertThat(anzeige.text()).hasSize(10);
      assertThat(anzeige.groesseBytes())
          .as(
              "Die Groesse ist die der vollstaendigen Datei — sonst ist nicht ablesbar, wie viel"
                  + " fehlt")
          .isEqualTo(50);
      assertThat(download.bytes())
          .as("Die Kappung schuetzt den Browser, nicht die Vertraulichkeit")
          .hasSize(50);
    }

    @Test
    @DisplayName("Ueber der Obergrenze gibt es nichts")
    void obergrenze() {
      ArtefaktService winzig =
          new ArtefaktService(
              repository,
              ablage,
              auditLogWriter,
              new RohdatenEigenschaften(10, 10, Duration.ofSeconds(5), Duration.ofSeconds(15)));
      artefakte(nutzdatei((short) 1));
      ablage.liefert("x".repeat(5000));

      AnzeigeResponse anzeige =
          winzig.anzeige(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(anzeige.zustand()).isEqualTo(Artefaktzustand.ABLAGE_NICHT_ERREICHBAR);
    }

    /**
     * Die Datei des Lesedienstes auf Schritt {@code 0} traegt ihren Originalnamen: {@code
     * FileReader.FileProperty.OriginalFilename} auf demselben Schritt (M56 a).
     *
     * <p><b>Belegvermerk (L10).</b> <i>Gemessen ist:</i> welche Namen den Originalnamen tragen (M56
     * a) und welche Namen auf Schritt {@code 0} liegen (M57). <i>Behauptet wird:</i> dass die Datei
     * des Lesedienstes die <b>eingegangene</b> ist. Das beruht auf einer Sichtpruefung des
     * Auftraggebers an <b>einer</b> Nachricht vom 19.08.2026 und ist <b>nicht gemessen</b>. Fuer
     * diesen Test ist es ohne Belang — geprueft wird, dass der Name gefunden wird, nicht was die
     * Datei ist.
     *
     * <p>Der Test hiess bis zum 19.08.2026 „Der Eingang bekommt den Originalnamen des Lesedienstes"
     * und lief ueber {@code 0-Message.Payload.GUID}. Diese Kennung gibt es nicht mehr (M73);
     * geprueft wird jetzt dieselbe Sache an dem Artefakt, das den Namen wirklich traegt.
     */
    @Test
    @DisplayName("Die Datei des Lesedienstes bekommt seinen Originalnamen")
    void lesedienst_bekommt_originalnamen() {
      artefakte(nutzdatei((short) 0));
      ablage.liefert("Erfundener Inhalt");
      when(repository.findeOriginaldateiname(any(), anyString(), anyShort()))
          .thenReturn(Optional.of("ERFUNDEN-Beleg.001"));

      ArtefaktService.Download download =
          service.download(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "0-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(download.dateiname())
          .as(
              "Ohne Originalnamen bekaeme er den konstruierten mit der MessageID darin — genau"
                  + " das, was rohdaten.md §9 dem Altsystem vorwirft")
          .isEqualTo("ERFUNDEN-Beleg.001");
    }

    @Test
    @DisplayName("Ohne Originalnamen auf dem Schritt wird konstruiert")
    void ohne_originalnamen_konstruiert() {
      artefakte(nutzdatei((short) 2));
      ablage.liefert("Erfundener Inhalt");

      ArtefaktService.Download download =
          service.download(
              MANDANT, MANDANT_NUTZER, MESSAGE_ID, "2-FileReader.Payload.GUID", "127.0.0.1");

      assertThat(download.dateiname()).isEqualTo("FileReader-schritt2-nutzdaten-" + MESSAGE_ID);
    }

    @Test
    @DisplayName("Eine erfundene Artefaktkennung ist 404")
    void erfundene_kennung() {
      artefakte(nutzdatei((short) 1));

      assertThatThrownBy(
              () ->
                  service.anzeige(
                      MANDANT, MANDANT_NUTZER, MESSAGE_ID, "9-Gibt.Es.Nicht", "127.0.0.1"))
          .isInstanceOf(RessourceNichtGefundenException.class);
      assertThatThrownBy(
              () -> service.anzeige(MANDANT, MANDANT_NUTZER, MESSAGE_ID, "kaputt", "127.0.0.1"))
          .isInstanceOf(RessourceNichtGefundenException.class);
    }
  }

  // ─── Die Protokollierung ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("Protokollierung")
  class Protokoll {

    private AuditEvent letztesEreignis() {
      ArgumentCaptor<AuditEvent> fang = ArgumentCaptor.forClass(AuditEvent.class);
      verify(auditLogWriter).schreibe(fang.capture());
      return fang.getValue();
    }

    @Test
    @DisplayName("Angesehen — mit der Fassung „beschnitten\"")
    void angesehen_beschnitten() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      service.anzeige(MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "1.2.3.4");

      AuditEvent ereignis = letztesEreignis();
      assertThat(ereignis.typ()).isEqualTo(AuditEventType.ROHDATEN_ANGESEHEN);
      assertThat(ereignis.detail()).isEqualTo("Fassung: beschnitten");
      assertThat(ereignis.actorUserId()).isEqualTo(1L);
      assertThat(ereignis.mandantId()).isEqualTo("VOTG");
      assertThat(ereignis.ip()).isEqualTo("1.2.3.4");
      assertThat(ereignis.targetId()).contains(MESSAGE_ID).contains("1-FileReader.Log.GUID");
    }

    @Test
    @DisplayName("Angesehen — mit der Fassung „vollstaendig\"")
    void angesehen_vollstaendig() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      service.anzeige(MANDANT, ADMIN_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "1.2.3.4");

      assertThat(letztesEreignis().detail()).isEqualTo("Fassung: vollstaendig");
    }

    @Test
    @DisplayName("Heruntergeladen — eigene Ereignisart")
    void heruntergeladen() {
      artefakte(nutzdatei((short) 1));
      ablage.liefert("Erfundener Inhalt");

      service.download(MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "1.2.3.4");

      assertThat(letztesEreignis().typ()).isEqualTo(AuditEventType.ROHDATEN_DOWNLOAD);
    }

    @Test
    @DisplayName("Fehlgeschlagen — mit dem Zustand statt der Fassung")
    void fehlgeschlagen() {
      artefakte(nutzdatei((short) 1));
      ablage.liefert(Abrufergebnis.nichtErreichbar());

      service.anzeige(MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Payload.GUID", "1.2.3.4");

      AuditEvent ereignis = letztesEreignis();
      assertThat(ereignis.typ()).isEqualTo(AuditEventType.ROHDATEN_ABRUF_FEHLGESCHLAGEN);
      assertThat(ereignis.detail()).isEqualTo("Zustand: ABLAGE_NICHT_ERREICHBAR");
    }

    @Test
    @DisplayName("Kein Dateiinhalt, keine UUID, keine Ablagenkennung im Eintrag")
    void ohne_inhalt_und_verweis() {
      artefakte(protokoll((short) 1));
      ablage.liefert(PROTOKOLL);

      service.anzeige(MANDANT, MANDANT_NUTZER, MESSAGE_ID, "1-FileReader.Log.GUID", "1.2.3.4");

      AuditEvent ereignis = letztesEreignis();
      assertThat(ereignis.toString())
          .doesNotContain("BEISPIELPROD42")
          .doesNotContain("deadbeef-0000-4000-8000")
          .doesNotContain("innen:")
          .doesNotContain("beispielknoten.invalid");
    }

    @Test
    @DisplayName("Die Liste wird nicht protokolliert — sie holt keine Datei")
    void liste_ohne_eintrag() {
      artefakte(nutzdatei((short) 0), protokoll((short) 1));

      service.liste(MANDANT, Rolle.MANDANT, MESSAGE_ID);

      verify(auditLogWriter, never()).schreibe(any());
    }
  }
}
