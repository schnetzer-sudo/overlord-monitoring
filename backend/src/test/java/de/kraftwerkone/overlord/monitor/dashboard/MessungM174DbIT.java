package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SERVICE;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.kraftwerkone.overlord.monitor.common.Ablagezugriff;
import de.kraftwerkone.overlord.monitor.common.Abrufergebnis;
import de.kraftwerkone.overlord.monitor.common.SaajAblagezugriff;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * <b>M174 — was eine Ablage auf die Null-UUID antwortet</b> (Schritt 10d Teil A, Teil 2).
 *
 * <h2>Die Frage</h2>
 *
 * <p>Die Ablagenkachel des Dashboards soll die Erreichbarkeit einer Ablage feststellen, ohne eine
 * echte Datei zu holen: ein {@code RETRIEVE} mit der <b>Null-UUID</b> {@value #NULL_UUID}. <b>Trägt
 * die Einordnung des Rohdatenabrufs diese Frage?</b> Eine erreichbare Ablage muss „Datei nicht
 * vorhanden" antworten, eine abgeschaltete „Ablage nicht erreichbar". Antwortet ein Ziel anders —
 * mit einem {@code Fault}, mit Daten oder mit einem sonstigen Zustand —, wird die Null-UUID anders
 * behandelt als ein fehlender Verweis, und die Kachel darf so nicht gebaut werden.
 *
 * <h2>Die vorregistrierte Deutung — sie steht im Auftrag und <b>vor</b> dem Lauf fest</h2>
 *
 * <table border="1">
 *   <caption>Was aus welchem Ergebnis folgt</caption>
 *   <tr><th>Ergebnis</th><th>Folge</th></tr>
 *   <tr><td>Jedes Ziel „Datei nicht vorhanden", {@code 07} „Ablage nicht erreichbar"</td>
 *       <td>Die Einordnung trägt. Weiter mit Teil 3</td></tr>
 *   <tr><td>Ein Ziel antwortet anders (Fault, Daten, sonstiger Zustand)</td>
 *       <td><b>Anhalten und melden</b></td></tr>
 *   <tr><td>{@code 07} antwortet „Datei nicht vorhanden"</td>
 *       <td>{@code 07} läuft wieder, die Auskunft vom 17.08.2026 ist überholt. Melden,
 *       weiterbauen</td></tr>
 *   <tr><td>Ein Ziel ist {@code 07} oder {@code 08} oder löst auf keine Zeile auf</td>
 *       <td>Melden, nicht auflösen. Die Kachel zeigt dann zu Recht rot</td></tr>
 *   <tr><td>Die Ziele sind nicht genau {@code 09} und {@code 10}</td>
 *       <td>Melden — M53 Befund 1 fand in jedem Fenster zwei gleichzeitig beschriebene
 *       Ablagen</td></tr>
 *   <tr><td>Ein erreichbares Ziel braucht deutlich länger als die 38 bis 244 ms aus M66/M60, oder
 *       die Gegenprobe endet nicht sofort</td><td>Melden</td></tr>
 * </table>
 *
 * <h2>Warum über {@link Ablagezugriff} und nicht über den Alt-Client</h2>
 *
 * <p>Weil der Dauerlauf der Kachel genau diesen Weg nimmt. M66 und M71 sind mit dem Alt-Client
 * gefahren ({@code scripts/mitschnitt-saaj/}); was dort gemessen wurde, gilt für den Alt-Client.
 * Diese Messung schließt die Lücke zwischen ihm und dem, was das Werkzeug wirklich schickt — und
 * nimmt dabei die <b>Zeitgrenzen der Anwendung</b> mit, nicht die des Messclients.
 *
 * <h2>Sie ist eine Messung und kein Test</h2>
 *
 * <p>Sie sichert nichts über Wanduhrzeit zu (Regel T1): Die Zeiten gehen nach {@code System.out}
 * und in keine Zusicherung. Zugesichert ist nur, dass überhaupt Ziele gefunden und Abrufe gefahren
 * wurden — sonst maße sie nichts. <b>Sie ist außerdem die einzige Stelle des Testbestands, die eine
 * Ablage anspricht</b>; jeder eigentliche Test dieses Projekts ersetzt {@link Ablagezugriff}.
 *
 * <h2>Regel G1</h2>
 *
 * <p><b>Keine Verbindungszeichenkette verlässt diesen Lauf.</b> Ausgegeben werden ausschließlich
 * Kennungen ({@code FILESTOREPROD09} und dergleichen), der eingeordnete Zustand, das Attribut
 * {@code Response} aus dem Rumpf und die Dauer. Die Werte der Spalte {@code
 * ServiceDefaultFileStore} werden <b>nicht erhoben</b> — die Ziele werden als Angabe zum Lauf
 * genannt, damit nachvollziehbar ist, wogegen gemessen wurde.
 *
 * <p>Sie schreibt nichts: beide Abfragen sind Lesezugriffe über {@code glassfishDsl}, und der
 * {@code ReadOnlyExecuteListener} hängt daran.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class MessungM174DbIT {

  /**
   * Die Kennung, mit der die Prüfung fragt — <b>nicht</b> die eines Artefakts.
   *
   * <p>Sie ist eine syntaktisch gültige UUID und zeigt mit Sicherheit auf keine Datei. Der Sinn ist
   * genau der: Die Prüfung will wissen, ob der Knoten antwortet, und nicht, was er hat. Eine echte
   * Kennung zu nehmen hieße, eine fremde Nutzdatei im Minutentakt anzufassen.
   */
  private static final String NULL_UUID = "00000000-0000-0000-0000-000000000000";

  /**
   * Die Gegenprobe: laut Auskunft des Auftraggebers vom 17.08.2026 <b>aus</b> (Nachtrag zu M66).
   * Sie steht daneben, damit der Lauf beide Seiten zeigt — ein Ergebnis „alles erreichbar" ohne
   * einen einzigen unerreichbaren Fall bewiese nicht, dass die Einordnung überhaupt unterscheidet.
   */
  private static final String GEGENPROBE = "FILESTOREPROD07";

  /** Was M53 Befund 1 und die Rotationsgrenze für den heutigen Bestand erwarten lassen. */
  private static final List<String> ERWARTETE_ZIELE = List.of("FILESTOREPROD09", "FILESTOREPROD10");

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Autowired private Ablagezugriff ablagezugriff;

  private static void melde(String schluessel, String wert) {
    System.out.println("M174 | " + schluessel + " | " + wert);
  }

  private static String ms(long nanos) {
    return String.format(Locale.GERMANY, "%.3f", nanos / 1_000_000.0);
  }

  @Test
  @DisplayName("M174 — die Null-UUID gegen jedes Ziel aus ServiceDefaultFileStore")
  void nulluuid_gegen_jedes_ziel() {
    // ─── (a) Die Ziele: die verschiedenen, nicht leeren Werte der Spalte ────────────────
    List<String> ziele =
        glassfishDsl
            .selectDistinct(SERVICE.SERVICEDEFAULTFILESTORE)
            .from(SERVICE)
            .where(SERVICE.SERVICEDEFAULTFILESTORE.isNotNull())
            .and(SERVICE.SERVICEDEFAULTFILESTORE.ne(""))
            .orderBy(SERVICE.SERVICEDEFAULTFILESTORE)
            .fetch(SERVICE.SERVICEDEFAULTFILESTORE);

    melde("ziele", String.join(", ", ziele));
    melde("zahl der ziele", String.valueOf(ziele.size()));
    melde(
        "ziele wie erwartet (09 und 10)",
        String.valueOf(ziele.equals(ERWARTETE_ZIELE)) + " — erwartet " + ERWARTETE_ZIELE);

    assertThat(ziele)
        .as("Ohne ein einziges Ziel maesse dieser Lauf nichts")
        .isNotEmpty()
        .doesNotContainNull();

    // ─── (b) Je Ziel ein RETRIEVE mit der Null-UUID, sequenziell ────────────────────────
    int gefahren = 0;
    for (String ziel : ziele) {
      gefahren += pruefe("ziel", ziel) ? 1 : 0;
    }

    // ─── (c) Die Gegenprobe gegen eine laut Auskunft abgeschaltete Ablage ───────────────
    gefahren += pruefe("gegenprobe", GEGENPROBE) ? 1 : 0;

    assertThat(gefahren)
        .as("Ohne einen einzigen gefahrenen Abruf maesse der Lauf nichts")
        .isPositive();
  }

  /**
   * Ein Ziel auflösen und einmal fragen.
   *
   * @return {@code true}, wenn ein Abruf tatsächlich gefahren worden ist
   */
  private boolean pruefe(String art, String kennung) {
    Optional<String> verbindung =
        glassfishDsl
            .select(SERVICE.SERVICECONNECTSTRING)
            .from(SERVICE)
            .where(SERVICE.SERVICEID.eq(kennung))
            .fetchOptional(SERVICE.SERVICECONNECTSTRING)
            .filter(adresse -> !adresse.isBlank());

    if (verbindung.isEmpty()) {
      // Nicht aufloesbar. Fuer den Rohdatenabruf ist das ABLAGE_NICHT_ERREICHBAR, und die Kachel
      // wird es genauso halten. Die Adresse steht hier ohnehin nirgends (G1) -- gemeldet wird nur,
      // DASS es keine gibt.
      melde(art + " " + kennung, "nicht aufloesbar (keine Zeile oder leerer ConnectString)");
      return false;
    }

    List<ILoggingEvent> mitschrift = new ArrayList<>();
    long begonnen = System.nanoTime();
    Abrufergebnis ergebnis =
        mitDebugMitschrift(mitschrift, () -> ablagezugriff.hole(verbindung.get(), NULL_UUID));
    long gedauert = System.nanoTime() - begonnen;

    melde(
        art + " " + kennung,
        "zustand="
            + ergebnis.zustand()
            + " · anhang="
            + (ergebnis.zip() == null ? "keiner" : ergebnis.zip().length + " Byte")
            + " · dauer="
            + ms(gedauert)
            + " ms");
    for (String zeile : rohantwort(mitschrift)) {
      melde(art + " " + kennung + " · rohantwort", zeile);
    }
    return true;
  }

  /**
   * Die Zeilen, die ausgegeben werden dürfen — <b>und die Auswahl ist Regel G1 und kein Filter aus
   * Bequemlichkeit</b>.
   *
   * <p>Ausgegeben werden alle Zeilen ab {@code WARN} (sie nennen die Adresse von Bauart wegen
   * nicht) und aus {@code DEBUG} <b>ausschließlich</b> die Zeile mit dem Attribut {@code Response}.
   * Die zweite DEBUG-Zeile des Zugriffs — {@code "Abruf fehlgeschlagen gegen {}"} — trägt die
   * vollständige Adresse samt Hostnamen. Sie ist dort zulässig, weil G1 erst <i>oberhalb</i> von
   * {@code DEBUG} verbietet; in der Ausgabe einer Messung, die in eine Datei wandert, hat sie
   * nichts zu suchen.
   */
  private static List<String> rohantwort(List<ILoggingEvent> mitschrift) {
    return mitschrift.stream()
        .filter(
            zeile ->
                zeile.getLevel().toInt() >= Level.WARN.toInt()
                    || zeile.getFormattedMessage().startsWith(ANTWORTZEILE))
        .map(ILoggingEvent::getFormattedMessage)
        .toList();
  }

  /** Der Anfang der einen DEBUG-Zeile, die keine Adresse trägt. */
  private static final String ANTWORTZEILE = "Antwort der Ablage:";

  /**
   * Führt den Abruf mit eingeschaltetem {@code DEBUG} auf {@link SaajAblagezugriff} aus und sammelt
   * dessen Zeilen ein.
   *
   * <p><b>Das ist die Stelle, an der die Rohantwort sichtbar wird.</b> Der Zugriff schreibt das
   * Attribut {@code Response} des ersten {@code File}-Elements auf {@code DEBUG} — es ist eine
   * Auskunft der fremden Anlage über sich selbst und gehört weder in eine Antwort noch ins
   * Protokoll darüber. Für eine Messung ist es genau das, was M66 und M66 (2) festgehalten haben:
   * {@code SUCCESS (RETRIEVE)}, {@code Error (Skipped)} oder gar nichts.
   *
   * <p><b>Die Weitergabe nach oben wird dabei abgeschaltet</b> ({@code setAdditive(false)}), und
   * das ist ebenfalls Regel G1: Sonst schriebe der Zugriff seine zweite DEBUG-Zeile — die mit der
   * vollständigen Adresse — über den Wurzelanhang mit in die Konsole und damit in jedes
   * Bauprotokoll. So sieht sie nur {@link ListAppender}, und {@link #rohantwort} lässt sie liegen.
   *
   * <p>Pegel und Weitergabe werden nach dem Lauf zurückgesetzt, damit ein folgender Abruf in
   * derselben JVM nicht plötzlich auf {@code DEBUG} und ohne Konsole steht.
   */
  private <T> T mitDebugMitschrift(List<ILoggingEvent> ziel, Supplier<T> lauf) {
    Logger protokoll = (Logger) LoggerFactory.getLogger(SaajAblagezugriff.class);
    Level vorherPegel = protokoll.getLevel();
    boolean vorherWeitergabe = protokoll.isAdditive();
    ListAppender<ILoggingEvent> anhang = new ListAppender<>();
    anhang.start();
    protokoll.addAppender(anhang);
    protokoll.setLevel(Level.DEBUG);
    protokoll.setAdditive(false);
    try {
      return lauf.get();
    } finally {
      protokoll.setLevel(vorherPegel);
      protokoll.setAdditive(vorherWeitergabe);
      protokoll.detachAppender(anhang);
      anhang.stop();
      ziel.addAll(anhang.list);
    }
  }
}
