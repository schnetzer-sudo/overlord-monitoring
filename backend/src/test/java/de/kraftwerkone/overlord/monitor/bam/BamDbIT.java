package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Der BAM-Block gegen <b>echte</b> Daten der Testkopie.
 *
 * <p><b>Keine fest eingetragene {@code MessageID}.</b> Die Bezugsnachrichten werden ueber den
 * Listen- und den Detail-Endpunkt <i>gefunden</i> — nach ihrer Gestalt, nicht nach ihrer Kennung.
 * Eine eingetragene Kennung waere beim naechsten Befuellen der Testkopie ein rot gewordener Test,
 * der nichts ueber den Code aussagt. Dieselbe Regel wie in {@code NachrichtendetailDbIT}.
 *
 * <p><b>Drei Mandanten, drei verschiedene Fragen</b> — und jeder von ihnen beantwortet genau eine,
 * die die anderen nicht koennen:
 *
 * <table>
 *   <caption>Warum genau diese drei</caption>
 *   <tr><th>Mandant</th><th>Gestalt</th><th>Was er belegt</th></tr>
 *   <tr><td>{@code NEXANS}</td><td>Wurzeln mit 9 bis 13 Typen (M41)</td>
 *       <td>die Zahl im Kopf deckt sich mit der Summe der Gruppen</td></tr>
 *   <tr><td>{@code ZAST}</td><td>441 Werte je Nachricht unter <i>einem</i> Typ (M40‑3)</td>
 *       <td>die Deckelung greift, und {@code weitereVorhanden} sagt es</td></tr>
 *   <tr><td>{@code WOC}</td><td>keine Konfiguration, Typ 9014 im Bestand (M40)</td>
 *       <td>ein unkonfigurierter Typ erscheint statt zu verschwinden</td></tr>
 * </table>
 *
 * <p><b>Die Zeitfenster sind absolut.</b> Die Testkopie endet Ende 2025; ein relatives Fenster
 * saehe null Zeilen, und der Test bewiese nur, dass leer leer ist.
 */
class BamDbIT extends SicherheitsTestbasis {

  private static final String PASSWORT = "einLangesPasswort1";

  /** Der grosse Mandant: Wurzeln mit vielen Typen, Fenster A aus den Messungen. */
  private static final String NEXANS = "NEXANS";

  /** Der zweitkleinste Mandant — und der, der die Deckelung erzwingt (M40‑3). */
  private static final String ZAST = "ZAST";

  /** Das Auffangbecken ohne BAM-Konfiguration (M40). */
  private static final String WOC = "WOC";

  private static final LocalDateTime TAG_VON = LocalDateTime.parse("2025-12-29T00:00:00");
  private static final LocalDateTime TAG_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  /** {@code ZAST} und {@code WOC} sind duenn besetzt — sie brauchen den ganzen Dezember. */
  private static final LocalDateTime MONAT_VON = LocalDateTime.parse("2025-11-30T00:00:00");

  private static final LocalDateTime MONAT_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private Sitzung alsMandant(String mandant) throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(mandant))
        .as("Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code")
        .isTrue();
    String nutzer = PRAEFIX + "bam-db-" + mandant.toLowerCase(Locale.ROOT);
    legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, mandant);
    return anmelden(nutzer, PASSWORT);
  }

  private String liste(LocalDateTime von, LocalDateTime bis, int limit) {
    return "/api/nachrichten?limit="
        + limit
        + "&von="
        + URLEncoder.encode(iso(von), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(bis), StandardCharsets.UTF_8);
  }

  /** Eine Nachricht mit Belegdaten, samt der Zahl aus dem Kopf. */
  private record MitBam(String messageId, int bamAnzahl) {}

  /**
   * Sucht in der Liste die erste Nachricht, deren Kopf {@code bamAnzahl > 0} meldet.
   *
   * <p><b>Ueber den Kopf und nicht ueber den BAM-Endpunkt</b> — genau so entscheidet auch die
   * Oberflaeche, ob sie den Block ueberhaupt laedt. 80,6 Prozent aller Nachrichten tragen keinen
   * Wert (M41); dass hier gesucht werden muss, ist der Normalfall und kein Mangel der Testkopie.
   */
  private Optional<MitBam> ersteMitBelegdaten(Sitzung sitzung, LocalDateTime von, LocalDateTime bis)
      throws IOException, InterruptedException {
    Antwort liste = sitzung.hole(liste(von, bis, 50));
    assertThat(liste.status()).isEqualTo(200);
    List<String> kennungen = liste.json("$.items[*].messageId");
    assertThat(kennungen)
        .as("Ohne Daten im Fenster bewiese der Test nur, dass leer leer ist")
        .isNotEmpty();

    for (String messageId : kennungen) {
      Antwort kopf = sitzung.hole("/api/nachrichten/" + messageId);
      assertThat(kopf.status()).isEqualTo(200);
      int anzahl = kopf.<Integer>json("$.bamAnzahl");
      if (anzahl > 0) {
        return Optional.of(new MitBam(messageId, anzahl));
      }
    }
    return Optional.empty();
  }

  private MitBam erforderlicheMitBelegdaten(
      Sitzung sitzung, LocalDateTime von, LocalDateTime bis, String mandant)
      throws IOException, InterruptedException {
    return ersteMitBelegdaten(sitzung, von, bis)
        .orElseThrow(
            () ->
                new AssertionError(
                    "Keine Nachricht mit BAM-Werten fuer "
                        + mandant
                        + " im Fenster — dann prueft dieser Test nichts. Erwartet nach M39/M40."));
  }

  /**
   * Die <b>breiteste</b> Nachricht der ersten Seite.
   *
   * <p><b>Nicht die erste mit Werten</b>, und der Unterschied ist der Test: Die Deckelung zeigt
   * sich erst an einer Nachricht, die die Grenze reisst. Welche das ist, haengt am Datenstand —
   * gesucht wird deshalb nach der <i>Gestalt</i> und nicht nach einer Kennung.
   */
  private MitBam breitesteMitBelegdaten(
      Sitzung sitzung, LocalDateTime von, LocalDateTime bis, String mandant)
      throws IOException, InterruptedException {
    Antwort liste = sitzung.hole(liste(von, bis, 50));
    assertThat(liste.status()).isEqualTo(200);
    List<String> kennungen = liste.json("$.items[*].messageId");
    assertThat(kennungen)
        .as("Ohne Daten im Fenster bewiese der Test nur, dass leer leer ist")
        .isNotEmpty();

    MitBam breiteste = null;
    for (String messageId : kennungen) {
      int anzahl = sitzung.hole("/api/nachrichten/" + messageId).<Integer>json("$.bamAnzahl");
      if (breiteste == null || anzahl > breiteste.bamAnzahl()) {
        breiteste = new MitBam(messageId, anzahl);
      }
    }
    assertThat(breiteste)
        .as("Keine Nachricht mit BAM-Werten fuer %s im Fenster (erwartet nach M39/M40)", mandant)
        .isNotNull();
    assertThat(breiteste.bamAnzahl()).isPositive();
    return breiteste;
  }

  private static String bam(String messageId) {
    return "/api/nachrichten/" + messageId + "/bam";
  }

  // ─── Die Zahl im Kopf ──────────────────────────────────────────────────────────

  /**
   * <b>{@code bamAnzahl} im Kopf und die Summe der Gruppen sind dieselbe Zahl.</b> Sie kommen aus
   * <i>zwei verschiedenen Statements</i> — einer zaehlenden Unterabfrage am Kopf und der Zaehlung
   * je Typ im BAM-Endpunkt. Laufen sie auseinander, entscheidet die Oberflaeche anhand einer Zahl,
   * die der Block nicht einloest.
   */
  @Test
  @DisplayName("Die Zahl im Kopf deckt sich mit der Summe der Gruppen")
  void kopfzahl_deckt_sich_mit_den_gruppen() throws Exception {
    Sitzung sitzung = alsMandant(NEXANS);
    MitBam nachricht = erforderlicheMitBelegdaten(sitzung, TAG_VON, TAG_BIS, NEXANS);

    Antwort antwort = sitzung.hole(bam(nachricht.messageId()));

    assertThat(antwort.status()).isEqualTo(200);
    List<Integer> gesamt = antwort.json("$.gruppen[*].gesamt");
    assertThat(gesamt).as("eine Nachricht mit Werten hat mindestens eine Gruppe").isNotEmpty();
    assertThat(gesamt.stream().mapToInt(Integer::intValue).sum())
        .as("die Zahl im Kopf ist die Summe ueber alle Typen dieser Nachricht")
        .isEqualTo(nachricht.bamAnzahl());
  }

  /**
   * <b>Jede Gruppe traegt eine Beschriftung</b>, auch wenn {@code MessageBAMType} keine Zeile
   * haette — dann steht dort die Typnummer. Eine Gruppe ohne Ueberschrift ist die eine Darstellung,
   * die schlechter waere als eine unfertige.
   */
  @Test
  @DisplayName("Jede Gruppe traegt Typ, Beschriftung, Zahl und Werte")
  void jede_gruppe_ist_vollstaendig() throws Exception {
    Sitzung sitzung = alsMandant(NEXANS);
    MitBam nachricht = erforderlicheMitBelegdaten(sitzung, TAG_VON, TAG_BIS, NEXANS);

    Antwort antwort = sitzung.hole(bam(nachricht.messageId()));

    List<String> bezeichnungen = antwort.json("$.gruppen[*].bezeichnung");
    List<Integer> typen = antwort.json("$.gruppen[*].typ");
    assertThat(bezeichnungen).isNotEmpty().doesNotContainNull();
    assertThat(bezeichnungen).allSatisfy(text -> assertThat(text).isNotBlank());
    assertThat(typen).hasSameSizeAs(bezeichnungen);
    assertThat(antwort.rumpf())
        .as("werte ist immer vorhanden, leer statt fehlend")
        .contains("\"werte\"");
  }

  // ─── Die Deckelung, gegen echte Daten ──────────────────────────────────────────

  /**
   * <b>Der zweitkleinste Mandant erzwingt die Deckelung.</b> {@code ZAST} traegt 441 Werte je
   * Nachricht unter einem einzigen Typ (M40‑3) — hier greift der Deckel, und {@code
   * weitereVorhanden} sagt es. Die Restangabe der Oberflaeche ist genau {@code gesamt −
   * werte.length}.
   */
  @Test
  @DisplayName("Bei ZAST liefert eine Gruppe genau 20 Werte und meldet weitere")
  void zast_wird_gedeckelt() throws Exception {
    Sitzung sitzung = alsMandant(ZAST);
    MitBam nachricht = breitesteMitBelegdaten(sitzung, MONAT_VON, MONAT_BIS, ZAST);
    assertThat(nachricht.bamAnzahl())
        .as("ohne eine breite Nachricht bewiese dieser Test nichts (M40-3: 441 je Nachricht)")
        .isGreaterThan(BamRepository.WERTE_JE_GRUPPE);

    Antwort antwort = sitzung.hole(bam(nachricht.messageId()));

    List<List<String>> werteJeGruppe = antwort.json("$.gruppen[*].werte");
    List<Integer> anzahlJeGruppe = werteJeGruppe.stream().map(List::size).toList();
    List<Integer> gesamtJeGruppe = antwort.json("$.gruppen[*].gesamt");
    List<Boolean> weitere = antwort.json("$.gruppen[*].weitereVorhanden");

    assertThat(anzahlJeGruppe)
        .as("keine Gruppe liefert mehr als die Grenze — die Deckelung sitzt im Statement")
        .allSatisfy(
            anzahl -> assertThat(anzahl).isLessThanOrEqualTo(BamRepository.WERTE_JE_GRUPPE));
    assertThat(anzahlJeGruppe)
        .as("und mindestens eine erreicht sie")
        .contains(BamRepository.WERTE_JE_GRUPPE);

    for (int nummer = 0; nummer < gesamtJeGruppe.size(); nummer++) {
      assertThat(weitere.get(nummer))
          .as("weitereVorhanden ist genau gesamt > gelieferte Werte, Gruppe %d", nummer)
          .isEqualTo(gesamtJeGruppe.get(nummer) > anzahlJeGruppe.get(nummer));
    }
    assertThat(weitere).as("bei ZAST ist der gedeckelte Fall der Regelfall").contains(true);
  }

  /**
   * <b>Die Zahl der ausgelieferten Werte haengt an der Zahl der <i>Gruppen</i>, nicht an der Zahl
   * der Werte.</b> Das ist der Nachweis, dass die Deckelung im Statement sitzt und nicht in der
   * Anzeige — und er gilt fuer <i>jede</i> Nachricht und nicht nur fuer die, die der Testlauf
   * gerade findet.
   *
   * <p><b>Bewusst kein Groessenvergleich zweier Ruempfe.</b> Die naheliegende Fassung suchte eine
   * schmale und eine breite Nachricht und hielt ihre Antwortlaengen nebeneinander. Sie ist am
   * 12.08.2026 rot geworden, und zwar zu Recht: Welche Nachricht die Liste zuerst liefert, haengt
   * am Datenstand, und ein Test, dessen Aussage von einer zufaellig getroffenen Zeile abhaengt,
   * misst den Datenstand und nicht den Code. Der Groessenvergleich steht als <b>Messung</b> in
   * {@code docs/bam-werte.md} §5 — dort mit benannten Nachrichten, hier als Eigenschaft.
   */
  @Test
  @DisplayName("Die Antwort ist durch die Zahl der Gruppen begrenzt, nicht durch die der Werte")
  void antwortgroesse_haengt_nicht_an_der_wertzahl() throws Exception {
    Sitzung zast = alsMandant(ZAST);
    MitBam breit = breitesteMitBelegdaten(zast, MONAT_VON, MONAT_BIS, ZAST);

    Antwort antwort = zast.hole(bam(breit.messageId()));

    List<List<String>> werteJeGruppe = antwort.json("$.gruppen[*].werte");
    int ausgeliefert = werteJeGruppe.stream().mapToInt(List::size).sum();
    int obergrenze = werteJeGruppe.size() * BamRepository.WERTE_JE_GRUPPE;

    assertThat(ausgeliefert)
        .as(
            "%d Werte auf der Nachricht, %d Gruppen — ausgeliefert werden hoechstens %d",
            breit.bamAnzahl(), werteJeGruppe.size(), obergrenze)
        .isLessThanOrEqualTo(obergrenze);
    assertThat(breit.bamAnzahl())
        .as("und die Nachricht traegt deutlich mehr, als die Antwort zeigt")
        .isGreaterThan(ausgeliefert);
  }

  // ─── Die Konfiguration siebt nicht ─────────────────────────────────────────────

  /**
   * <b>{@code WOC} hat keinen konfigurierten BAM-Typ und sieht trotzdem seine Belegdaten.</b> Der
   * Fall ist real und der Grund fuer die Entscheidung gegen den Konfigurationsfilter (M40): {@code
   * WOC} traegt 2.067 BAM-Zeilen unter Typ 9014, den seine Konfiguration nicht kennt. Folgte der
   * Block ihr, saehe dieser Mandant <b>nichts</b>.
   *
   * <p><b>Der Typ wird nicht markiert.</b> Ob er konfiguriert ist, ist eine interne Angabe — die
   * Antwort traegt kein Feld dafuer.
   */
  @Test
  @DisplayName("WOC sieht seinen nicht konfigurierten Typ — ohne Markierung")
  void unkonfigurierter_typ_erscheint() throws Exception {
    Sitzung sitzung = alsMandant(WOC);
    MitBam nachricht = erforderlicheMitBelegdaten(sitzung, MONAT_VON, MONAT_BIS, WOC);

    Antwort antwort = sitzung.hole(bam(nachricht.messageId()));

    assertThat(antwort.status()).isEqualTo(200);
    List<Integer> typen = antwort.json("$.gruppen[*].typ");
    assertThat(typen)
        .as(
            "WOC hat null konfigurierte Typen (M40-1) und traegt trotzdem Werte — folgte der Block"
                + " der Konfiguration, waere diese Liste leer")
        .isNotEmpty();
    assertThat(antwort.rumpf())
        .as("und nichts an der Antwort sagt, ob ein Typ konfiguriert ist")
        .doesNotContain("konfiguriert")
        .doesNotContain("sortIndex");
  }

  // ─── Der leere Fall ────────────────────────────────────────────────────────────

  /**
   * <b>Eine eigene Nachricht ohne BAM-Werte ist {@code 200} mit leerer Liste — nicht {@code
   * 404}.</b> Der Unterschied ist der ganze Grund fuer die vorgelagerte Existenzpruefung: 80,6
   * Prozent aller Nachrichten tragen keinen Wert (M41), und „leer" ist dort eine Aussage ueber die
   * Nachricht und nicht ueber ihre Sichtbarkeit.
   */
  @Test
  @DisplayName("Eine eigene Nachricht ohne Belegdaten antwortet 200 mit leerer Gruppenliste")
  void ohne_belegdaten_ist_200_und_leer() throws Exception {
    Sitzung sitzung = alsMandant(NEXANS);
    Antwort liste = sitzung.hole(liste(TAG_VON, TAG_BIS, 50));
    List<String> kennungen = liste.json("$.items[*].messageId");

    String ohneWerte = null;
    for (String messageId : kennungen) {
      if (sitzung.hole("/api/nachrichten/" + messageId).<Integer>json("$.bamAnzahl") == 0) {
        ohneWerte = messageId;
        break;
      }
    }
    assertThat(ohneWerte)
        .as("bei 80,6 Prozent ohne Werte (M41) sollte sich unter 50 Zeilen eine finden")
        .isNotNull();

    Antwort antwort = sitzung.hole(bam(ohneWerte));

    assertThat(antwort.status())
        .as("leer und „gibt es nicht\" sind zwei verschiedene Auskuenfte")
        .isEqualTo(200);
    assertThat(antwort.<List<Object>>json("$.gruppen")).isEmpty();
    assertThat(antwort.<String>json("$.messageId")).isEqualTo(ohneWerte);
  }
}
