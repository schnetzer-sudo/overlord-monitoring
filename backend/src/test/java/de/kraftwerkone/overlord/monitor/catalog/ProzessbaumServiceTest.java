package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Dienst aus den Rohzeilen macht — <b>ohne Datenbank</b>.
 *
 * <p>Die Statements prueft {@code ProzessbaumStatementsTest}, die Plaene {@code
 * ProzessbaumPlanDbIT}, die Trennung {@code ProzessbaumIsolationDbIT}. <b>Hier steht, was zwischen
 * Rohzeile und Antwort passiert:</b> die Schachtelung, die drei Zustaende, die Stilleschwelle, die
 * Summen und die Reihenfolge.
 *
 * <p><b>Alle Pruefwerte sind erfunden</b> (Regel G1) — der Test haengt an keiner Zahl aus dem
 * Bestand und an keinem Pflegestand (Regel T2). Er baut sich jede Zeile selbst.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProzessbaumServiceTest {

  private static final MandantContext MANDANT = new MandantContext("ERFUNDEN");
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  /**
   * Der Stichtag. In dieser Zone entspricht {@code 2025-12-30T04:09:47Z} der Wanduhrzeit {@code
   * 2025-12-30T05:09:47} — die Zeitzone steht hier bewusst schief zur UTC-Angabe, damit ein
   * versehentliches Rechnen in UTC auffiele.
   */
  private static final Instant JETZT = Instant.parse("2025-12-30T04:09:47Z");

  private static final LocalDateTime WANDUHR = LocalDateTime.parse("2025-12-30T05:09:47");

  @Mock private ProzessbaumRepository repository;

  private ProzessbaumService service() {
    return new ProzessbaumService(
        repository, new MessageStatusClassifier(), Clock.fixed(JETZT, ZONE));
  }

  private void bestandMit(List<Prozessgeruestzeile> geruest, List<Prozesskennzahlzeile> zahlen) {
    when(repository.geruest(any())).thenReturn(geruest);
    when(repository.kennzahlen(any(), any(), any())).thenReturn(zahlen);
  }

  private ProzessbaumResponse antwort() {
    return service().baum(MANDANT, null);
  }

  /** Eine gepflegte Zeile mit Partner und Richtung. */
  private static Prozessgeruestzeile gepflegt(
      String id, String name, String partner, String richtung, LocalDateTime letzteBewegung) {
    return new Prozessgeruestzeile(
        id, name, partner, richtung, Pflegestatus.GEPFLEGT.name(), letzteBewegung);
  }

  private static Prozesskennzahlzeile zahl(String id, String status, long anzahl) {
    return new Prozesskennzahlzeile(id, status, anzahl);
  }

  // ─── Die Schachtelung ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Partner → Richtung → Prozess")
  class Schachtelung {

    @Test
    @DisplayName("Unter einem Partner erscheinen nur die Richtungen, die vorkommen")
    void kein_leerer_ast() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      List<PartnerknotenResponse> partner = antwort().partner();

      assertThat(partner).hasSize(1);
      assertThat(partner.getFirst().richtungen()).hasSize(1);
      assertThat(partner.getFirst().richtungen().getFirst().richtung()).isEqualTo("EINGEHEND");
      assertThat(partner.getFirst().richtungen().getFirst().prozesse()).hasSize(2);
    }

    /**
     * Der Fall, der die Gliederung ueberhaupt zu einer Entscheidung macht: Partner ungepflegt ist
     * eine <b>eigene Gruppe auf oberster Ebene</b> und wird nicht auf die Partner verteilt.
     */
    @Test
    @DisplayName("Ohne Partner: eigene Gruppe ganz oben, am Ende")
    void ohne_partner_eigene_gruppe() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", null, "EINGEHEND", WANDUHR)),
          List.of());

      List<PartnerknotenResponse> partner = antwort().partner();

      assertThat(partner).hasSize(2);
      assertThat(partner.getFirst().partner()).isEqualTo("ALPHA");
      assertThat(partner.getLast().partner()).isNull();
      assertThat(partner.getLast().anzahlProzesse()).isEqualTo(1);
    }

    /**
     * Und der zweite, davon verschiedene Fall: Partner gepflegt, Richtung ungepflegt — eine eigene
     * Gruppe <b>unter diesem Partner</b>, neben eingehend und ausgehend.
     */
    @Test
    @DisplayName("Ohne Richtung: eigene Gruppe unter dem Partner, nicht ganz oben")
    void ohne_richtung_unter_dem_partner() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", null, WANDUHR)),
          List.of());

      List<PartnerknotenResponse> partner = antwort().partner();

      assertThat(partner).hasSize(1);
      assertThat(partner.getFirst().richtungen()).hasSize(2);
      assertThat(partner.getFirst().richtungen().getFirst().richtung()).isEqualTo("EINGEHEND");
      assertThat(partner.getFirst().richtungen().getLast().richtung()).isNull();
    }

    /**
     * <b>Entscheidung E-i.</b> Ein Regelvorschlag der Heuristik steht mit {@code OFFEN} in
     * derselben Spalte wie eine kuratierte Zuordnung. Gruppierte der Baum darueber, stuenden
     * Prozesse unter einem Partner, den niemand bestaetigt hat.
     */
    @Test
    @DisplayName("Ein offener Vorschlag ist keine Zuordnung")
    void offener_vorschlag_gilt_nicht() {
      bestandMit(
          List.of(
              new Prozessgeruestzeile(
                  "p1", "A", "VORSCHLAG", "EINGEHEND", Pflegestatus.OFFEN.name(), WANDUHR)),
          List.of());

      List<PartnerknotenResponse> partner = antwort().partner();

      assertThat(partner).hasSize(1);
      assertThat(partner.getFirst().partner()).isNull();
      assertThat(partner.getFirst().richtungen().getFirst().richtung()).isNull();
    }

    /** „Gepflegt und leer" faellt fachlich mit „nicht zugeordnet" zusammen (E4). */
    @Test
    @DisplayName("Gepflegt mit leerem Feld zaehlt wie nicht zugeordnet")
    void gepflegt_und_leer() {
      bestandMit(List.of(gepflegt("p1", "A", "", "", WANDUHR)), List.of());

      assertThat(antwort().partner().getFirst().partner()).isNull();
    }

    /** Ohne Katalogzeile — {@code SUTTONS} und {@code WOC} sind genau dieser Fall. */
    @Test
    @DisplayName("Ohne Katalogzeile faellt alles in die eine Gruppe")
    void ohne_katalogzeile() {
      bestandMit(
          List.of(
              new Prozessgeruestzeile("p1", "A", null, null, null, WANDUHR),
              new Prozessgeruestzeile("p2", "B", null, null, null, WANDUHR)),
          List.of());

      List<PartnerknotenResponse> partner = antwort().partner();

      assertThat(partner).hasSize(1);
      assertThat(partner.getFirst().partner()).isNull();
      assertThat(partner.getFirst().anzahlProzesse()).isEqualTo(2);
    }
  }

  // ─── Die Reihenfolge ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Reihenfolge")
  class Reihenfolge {

    @Test
    @DisplayName("Partner alphabetisch, „nicht zugeordnet\" am Ende")
    void partner_alphabetisch() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ZULU", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", null, "EINGEHEND", WANDUHR),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(antwort().partner())
          .extracting(PartnerknotenResponse::partner)
          .containsExactly("ALPHA", "ZULU", null);
    }

    /**
     * Die Reihenfolge ist die der Aufzaehlung {@link Richtung} und danach „nicht ermittelt" — ueber
     * alle Partner dieselbe. Eine Oberflaeche, die Symbole nach Position vergibt, bekaeme sonst
     * unter jedem Partner eine andere.
     */
    @Test
    @DisplayName("Richtungen: eingehend, ausgehend, dann „nicht ermittelt\"")
    void richtungen_in_der_reihenfolge_der_aufzaehlung() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", null, WANDUHR),
              gepflegt("p2", "B", "ALPHA", "AUSGEHEND", WANDUHR),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(antwort().partner().getFirst().richtungen())
          .extracting(RichtungsknotenResponse::richtung)
          .containsExactly("EINGEHEND", "AUSGEHEND", null);
    }

    /**
     * Ein gepflegter, aber unbekannter Richtungswert — die Spalte ist {@code varchar}, die
     * Whitelist steht im Code. Er faellt hinter die bekannten und <b>vor</b> „nicht ermittelt": Er
     * verschwindet nicht und wird nicht mit dem leeren Feld verwechselt.
     */
    @Test
    @DisplayName("Ein unbekannter Richtungswert steht hinter den bekannten, vor „nicht ermittelt\"")
    void unbekannte_richtung() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", null, WANDUHR),
              gepflegt("p2", "B", "ALPHA", "QUERGEHEND", WANDUHR),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(antwort().partner().getFirst().richtungen())
          .extracting(RichtungsknotenResponse::richtung)
          .containsExactly("EINGEHEND", "QUERGEHEND", null);
    }

    /**
     * Die Blaetter behalten die Reihenfolge der Abfrage — dort steht {@code ORDER BY ProcessName}.
     */
    @Test
    @DisplayName("Die Blaetter behalten die Reihenfolge der Abfrage")
    void blaetter_behalten_die_reihenfolge() {
      bestandMit(
          List.of(
              gepflegt("p1", "aaa", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "bbb", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p3", "ccc", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of());

      assertThat(antwort().partner().getFirst().richtungen().getFirst().prozesse())
          .extracting(ProzessknotenResponse::processName)
          .containsExactly("aaa", "bbb", "ccc");
    }
  }

  // ─── Die drei Zustaende ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die drei Zustaende")
  class Zustaende {

    @Test
    @DisplayName("Keine Rollupzeile heisst NIE und nicht STILL")
    void nie() {
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", null)), List.of());

      ProzessknotenResponse blatt = erstesBlatt();
      assertThat(blatt.zustand()).isEqualTo(Prozesszustand.NIE);
      assertThat(blatt.letzteBewegung()).isNull();
    }

    @Test
    @DisplayName("Aelter als die Schwelle heisst STILL")
    void still() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(4))), List.of());

      assertThat(erstesBlatt().zustand()).isEqualTo(Prozesszustand.STILL);
    }

    @Test
    @DisplayName("Juenger als die Schwelle heisst BEWEGT")
    void bewegt() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(2))), List.of());

      assertThat(erstesBlatt().zustand()).isEqualTo(Prozesszustand.BEWEGT);
    }

    /**
     * <b>Die Grenze liegt zugunsten von „bewegt".</b> Genau auf der Schwelle wird nicht markiert;
     * eine Sekunde davor schon. Die Grenze muss irgendwo liegen, und im Zweifel <b>nicht</b> zu
     * markieren haelt die Markierung aussagekraeftig.
     */
    @Test
    @DisplayName("Genau auf der Schwelle gilt ein Prozess als bewegt")
    void die_grenze_selbst() {
      LocalDateTime schwelle = WANDUHR.minus(ProzessbaumService.STILLE_SCHWELLE);

      assertThat(ProzessbaumService.zustand(schwelle, WANDUHR)).isEqualTo(Prozesszustand.BEWEGT);
      assertThat(ProzessbaumService.zustand(schwelle.minusSeconds(1), WANDUHR))
          .isEqualTo(Prozesszustand.STILL);
    }

    /**
     * <b>Der Zustand haengt nicht am Zeitfenster.</b> Derselbe Prozess, drei Fensterbreiten, ein
     * Zustand — das ist die Festlegung, und sie ist ohne diesen Test nur eine Absichtserklaerung.
     */
    @Test
    @DisplayName("Der Zustand ist ueber alle drei Zeitraeume derselbe")
    void zustand_ist_fensterunabhaengig() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(4))), List.of());

      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        ProzessbaumResponse antwort = service().baum(MANDANT, zeitraum);
        assertThat(
                antwort
                    .partner()
                    .getFirst()
                    .richtungen()
                    .getFirst()
                    .prozesse()
                    .getFirst()
                    .zustand())
            .as("Paar %s", zeitraum.code())
            .isEqualTo(Prozesszustand.STILL);
      }
    }

    /** Die drei Zaehler sind disjunkt und vollstaendig: Ihre Summe ist die Prozesszahl. */
    @Test
    @DisplayName("bewegt + still + nie ergibt die Prozesszahl")
    void die_drei_zaehler_sind_vollstaendig() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", "EINGEHEND", WANDUHR.minusMonths(4)),
              gepflegt("p3", "C", "ALPHA", "EINGEHEND", WANDUHR.minusYears(2)),
              gepflegt("p4", "D", "ALPHA", "EINGEHEND", null)),
          List.of());

      BaumsummeResponse gesamt = antwort().gesamt();

      assertThat(gesamt.anzahlProzesse()).isEqualTo(4);
      assertThat(gesamt.bewegt()).isEqualTo(1);
      assertThat(gesamt.still()).isEqualTo(2);
      assertThat(gesamt.nie()).isEqualTo(1);
      assertThat(gesamt.bewegt() + gesamt.still() + gesamt.nie())
          .isEqualTo(gesamt.anzahlProzesse());
    }

    /** Die Schwelle steht in der Antwort, damit die Oberflaeche sie nicht zweitens kennen muss. */
    @Test
    @DisplayName("Die Stilleschwelle steht in der Antwort")
    void schwelle_steht_in_der_antwort() {
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)), List.of());

      assertThat(antwort().stilleSchwelleMonate())
          .isEqualTo((int) ProzessbaumService.STILLE_SCHWELLE.toTotalMonths());
    }
  }

  // ─── Die Kennzahlen ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Die Kennzahlen")
  class Kennzahlen {

    /**
     * <b>Die Einordnung wird gerufen, nicht nachgebaut.</b> {@code ERROR_TIMEOUT} und {@code
     * COMMIT_REJECTED} sind Fehler, {@code FINISHED} und {@code SUSPENDED} nicht — und {@code
     * ERROR_ETWAS_NEUES} ist einer, obwohl der Klassifizierer den Wert nicht kennt.
     */
    @Test
    @DisplayName("Fehler entstehen aus dem Rohwert, ueber den Klassifizierer")
    void fehler_aus_dem_rohwert() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(
              zahl("p1", "FINISHED", 100),
              zahl("p1", "SUSPENDED", 10),
              zahl("p1", "ERROR_TIMEOUT", 3),
              zahl("p1", "COMMIT_REJECTED", 2),
              zahl("p1", "ERROR_ETWAS_NEUES", 1)));

      ProzessknotenResponse blatt = erstesBlatt();

      assertThat(blatt.nachrichten()).isEqualTo(116);
      assertThat(blatt.fehler()).isEqualTo(6);
    }

    /** Ein Prozess ohne Zeile im Fenster steht trotzdem im Baum — mit Null. */
    @Test
    @DisplayName("Ohne Zeile im Fenster steht der Prozess mit Null da")
    void ohne_zeile_im_fenster() {
      bestandMit(List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)), List.of());

      ProzessknotenResponse blatt = erstesBlatt();

      assertThat(blatt.nachrichten()).isZero();
      assertThat(blatt.fehler()).isZero();
      assertThat(blatt.zustand()).isEqualTo(Prozesszustand.BEWEGT);
    }

    /**
     * Die Summen jeder Ebene sind die Summen ihrer Kinder — der Aufrufer soll nichts
     * zusammenrechnen muessen (Richtlinie §5.1).
     */
    @Test
    @DisplayName("Jede Ebene traegt die Summe ihrer Kinder")
    void summen_auf_jeder_ebene() {
      bestandMit(
          List.of(
              gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR),
              gepflegt("p2", "B", "ALPHA", "AUSGEHEND", WANDUHR),
              gepflegt("p3", "C", "BETA", "EINGEHEND", WANDUHR)),
          List.of(
              zahl("p1", "FINISHED", 10),
              zahl("p1", "ERROR_TIMEOUT", 1),
              zahl("p2", "FINISHED", 20),
              zahl("p3", "FINISHED", 5),
              zahl("p3", "ERROR_TIMEOUT", 2)));

      ProzessbaumResponse antwort = antwort();
      PartnerknotenResponse alpha = antwort.partner().getFirst();

      assertThat(alpha.partner()).isEqualTo("ALPHA");
      assertThat(alpha.anzahlProzesse()).isEqualTo(2);
      assertThat(alpha.nachrichten()).isEqualTo(31);
      assertThat(alpha.fehler()).isEqualTo(1);
      assertThat(alpha.richtungen().getFirst().nachrichten()).isEqualTo(11);
      assertThat(alpha.richtungen().getLast().nachrichten()).isEqualTo(20);

      assertThat(antwort.gesamt().nachrichten()).isEqualTo(38);
      assertThat(antwort.gesamt().fehler()).isEqualTo(3);
    }

    /**
     * Eine Kennzahlzeile zu einem Prozess, der nicht im Geruest steht, darf die Kopfzahl nicht
     * heben. Der Fall kann nur auftreten, wenn die Mandantenkette der beiden Statements
     * auseinanderliefe — und dann soll die Zahl <b>nicht</b> stillschweigend mitwachsen.
     */
    @Test
    @DisplayName("Eine Zeile ohne Prozess im Geruest zaehlt nirgends mit")
    void fremde_kennzahlzeile_zaehlt_nicht() {
      bestandMit(
          List.of(gepflegt("p1", "A", "ALPHA", "EINGEHEND", WANDUHR)),
          List.of(zahl("p1", "FINISHED", 10), zahl("fremd", "FINISHED", 999)));

      assertThat(antwort().gesamt().nachrichten()).isEqualTo(10);
    }
  }

  // ─── Zeitraum und Fenster ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("Zeitraum und Fenster")
  class ZeitraumUndFenster {

    @Test
    @DisplayName("Ohne Parameter gilt die Vorgabe, und die Antwort nennt sie")
    void vorgabe_wird_genannt() {
      bestandMit(List.of(), List.of());

      assertThat(antwort().zeitraum()).isEqualTo(ProzessbaumService.VORGABE.code());
    }

    @Test
    @DisplayName("Mit Parameter steht das gewaehlte Paar in der Antwort")
    void gewaehltes_paar_wird_genannt() {
      bestandMit(List.of(), List.of());

      for (Rollupzeitraum zeitraum : Rollupzeitraum.values()) {
        assertThat(service().baum(MANDANT, zeitraum).zeitraum()).isEqualTo(zeitraum.code());
      }
    }

    /**
     * Die Grenzen kommen aus {@code Rollupzeitraum.fenster} und werden in UTC uebersetzt — nicht in
     * der Zone des Servers, auf dem der Test laeuft.
     */
    @Test
    @DisplayName("Das Fenster steht in UTC in der Antwort")
    void fenster_in_utc() {
      bestandMit(List.of(), List.of());

      ZeitfensterResponse fenster = service().baum(MANDANT, Rollupzeitraum.STUNDEN_48).fenster();

      assertThat(fenster.von())
          .isEqualTo(LocalDateTime.parse("2025-12-28T06:00:00").atZone(ZONE).toInstant());
      assertThat(fenster.bis())
          .isEqualTo(LocalDateTime.parse("2025-12-30T06:00:00").atZone(ZONE).toInstant());
    }

    /**
     * <b>Ein Mandant ohne Prozesse ist ein leerer Baum und kein Fehler.</b> Die Antwort ist
     * vollstaendig, alle Zaehler stehen auf null.
     */
    @Test
    @DisplayName("Ein Mandant ohne Prozesse bekommt einen leeren Baum")
    void leerer_baum() {
      bestandMit(List.of(), List.of());

      ProzessbaumResponse antwort = antwort();

      assertThat(antwort.partner()).isEmpty();
      assertThat(antwort.gesamt().anzahlProzesse()).isZero();
      assertThat(antwort.gesamt().nachrichten()).isZero();
      assertThat(antwort.fenster()).isNotNull();
    }
  }

  private ProzessknotenResponse erstesBlatt() {
    return antwort().partner().getFirst().richtungen().getFirst().prozesse().getFirst();
  }
}
