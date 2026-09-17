package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.LiveRestKorrektur;
import de.kraftwerkone.overlord.monitor.common.LiveRestZeile;
import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * <b>Die Korrektur des Live-Rests in den Eimern des Dashboards</b> (E-190, E-191, {@code
 * docs/live-rest.md} §9b).
 *
 * <p>Der Baustein {@code common/LiveRestService} liefert vorzeichenbehaftete Zeilen je {@code
 * (stunde, process_id, message_status)}. Das Dashboard rechnet in <b>seinen</b> Eimern — Stunde,
 * Tag oder Kalendermonat, je nach Paar — und in Block 5 je <b>Schluessel</b> einer Sicht. Diese
 * Klasse ordnet die Zeilen einmal zu; Verlauf, Kacheln und Verteilung rechnen danach mit Zeilen
 * derselben Gestalt, die sie auch aus dem Rollup bekommen. <b>Kein Block rechnet den Live-Rest
 * selbst nach.</b>
 *
 * <h2>Die Zuordnung zum Eimer ist die des Rollups</h2>
 *
 * <p>Die Tagesebene entsteht aus der Stundenebene ueber {@code DATE(stunde)}, die Monatsebene aus
 * der Tagesebene ueber den Monatsersten ({@code docs/rollup.md} §5, Schritte 4 und 5) — beide in
 * derselben Transaktion wie die Stundenebene. Eine Korrekturzeile der Stunde {@code h} gehoert
 * deshalb in den Tageseimer {@code DATE(h)} und in den Monatseimer des Monatsersten: Was der
 * Delta-Lauf beim naechsten Mal in diese Eimer schreibt, ist genau die Summe der Stunden.
 *
 * <h2>Nur im Fenster, und keine Zahl unter null</h2>
 *
 * <p>Wie im Prozessbaum (E-188) zaehlt eine Zeile nur, wenn ihre <b>Stunde</b> im Fenster liegt
 * ({@code von} einschliessend, {@code bis} ausschliessend). Das Fenster liegt auf Eimergrenzen,
 * also liegt mit der Stunde auch ihr Eimer darin. Geklemmt wird <b>je (Eimer, Rohstatus)</b> und
 * <b>je Schluessel</b> auf null: Zwischen der Lesung des Rollups und der Zaehlung aus {@code
 * Message} kann Bestand weglaufen, und eine negative Kachel sagte nichts Wahres. Was auf null
 * faellt, verschwindet — ein Eimer ohne Zeile ist im Verlauf keiner, und eine Zeile der Verteilung
 * mit null waere ein Rang ohne Inhalt.
 *
 * <h2>Die Schluessel der Verteilung, Gross- und Kleinschreibung</h2>
 *
 * <p>Das Verteilungsstatement gruppiert in der Datenbank unter {@code utf8mb4_general_ci}: Zwei
 * Schreibweisen desselben Partners sind dort <b>eine</b> Gruppe, und welche Schreibweise sie
 * vertritt, entscheidet die Datenbank. Die Nachlesung liefert die Schreibweise der einzelnen
 * Katalogzeile. Verglichen wird deshalb <b>ohne Gross- und Kleinschreibung</b>, und die Zeile
 * behaelt die Schreibweise, die zuerst da war — sonst zerfiele eine Gruppe in zwei, sobald eine
 * Katalogzeile anders geschrieben ist als die Gruppe, die sie vertritt.
 */
final class Liveverrechnung {

  /** Ohne Korrektur: nichts zuzuordnen, keine Nachlesung. */
  static final Liveverrechnung KEINE = new Liveverrechnung(List.of(), Map.of());

  /** Die Korrekturzeilen im Fenster, auf den Eimer des Paares gehoben — vorzeichenbehaftet. */
  private final List<Rollupsumme> zeilen;

  /** Die Korrektur je Prozess ueber das ganze Fenster — Block 5 gruppiert ueber das Fenster. */
  private final Map<String, Long> jeProzess;

  private Liveverrechnung(List<Rollupsumme> zeilen, Map<String, Long> jeProzess) {
    this.zeilen = List.copyOf(zeilen);
    this.jeProzess = Map.copyOf(jeProzess);
  }

  /** Die Korrektur, zugeschnitten auf Paar und Fenster. */
  static Liveverrechnung im(
      Rollupzeitraum zeitraum, Zeitfenster fenster, LiveRestKorrektur korrektur) {
    List<Rollupsumme> zeilen = new ArrayList<>();
    Map<String, Long> jeProzess = new HashMap<>();
    for (LiveRestZeile zeile : korrektur.zeilen()) {
      if (zeile.stunde().isBefore(fenster.von()) || !zeile.stunde().isBefore(fenster.bis())) {
        continue;
      }
      zeilen.add(
          new Rollupsumme(eimer(zeitraum, zeile.stunde()), zeile.messageStatus(), zeile.anzahl()));
      jeProzess.merge(zeile.processId(), zeile.anzahl(), Long::sum);
    }
    return zeilen.isEmpty() ? KEINE : new Liveverrechnung(zeilen, jeProzess);
  }

  /** Der Eimer des Paares, in den eine Stunde faellt — so, wie der Rollup ihn bildet. */
  static LocalDateTime eimer(Rollupzeitraum zeitraum, LocalDateTime stunde) {
    return switch (zeitraum) {
      case STUNDEN_48 -> stunde;
      case TAGE_30 -> stunde.toLocalDate().atStartOfDay();
      case MONATE_12 -> stunde.toLocalDate().withDayOfMonth(1).atStartOfDay();
    };
  }

  /** Gibt es ueberhaupt etwas zu verrechnen? Sonst laeuft keine Nachlesung. */
  boolean leer() {
    return zeilen.isEmpty();
  }

  /** Die Prozesse der Korrekturzeilen im Fenster — die Eingabe der Katalog-Nachlesung. */
  Set<String> betroffeneProzesse() {
    return new TreeSet<>(jeProzess.keySet());
  }

  /**
   * Bloecke 1 bis 3: die Rollupzeilen plus die Korrektur, je (Eimer, Rohstatus) auf null geklemmt,
   * ohne Nullen, sortiert wie die Abfrage.
   */
  List<Rollupsumme> verlauf(List<Rollupsumme> ausDemRollup) {
    if (zeilen.isEmpty()) {
      return ausDemRollup;
    }
    Map<LocalDateTime, Map<String, Long>> summe = new TreeMap<>();
    for (Rollupsumme zeile : ausDemRollup) {
      summe
          .computeIfAbsent(zeile.eimer(), eimer -> new TreeMap<>())
          .merge(zeile.messageStatus(), zeile.anzahl(), Long::sum);
    }
    for (Rollupsumme zeile : zeilen) {
      summe
          .computeIfAbsent(zeile.eimer(), eimer -> new TreeMap<>())
          .merge(zeile.messageStatus(), zeile.anzahl(), Long::sum);
    }
    List<Rollupsumme> verrechnet = new ArrayList<>();
    summe.forEach(
        (eimer, jeStatus) ->
            jeStatus.forEach(
                (status, anzahl) -> {
                  if (anzahl > 0) {
                    verrechnet.add(new Rollupsumme(eimer, status, anzahl));
                  }
                }));
    return verrechnet;
  }

  /**
   * Block 5, eine Sicht: die Zeilen der Abfrage plus die Korrektur je Schluessel, auf null
   * geklemmt, ohne Nullen. <b>Nicht zugeordnet</b> ist der Schluessel {@code null} — dort landet,
   * was keine Katalogzeile hat oder nach E-i keinen Wert traegt.
   *
   * @param schluesselJeProzess der Schluessel dieser Sicht je Prozess aus der Nachlesung; ein
   *     Prozess ohne Eintrag gilt als nicht zugeordnet
   */
  List<Verteilungssumme> verteilung(
      List<Verteilungssumme> ausDemRollup, Map<String, String> schluesselJeProzess) {
    if (zeilen.isEmpty()) {
      return ausDemRollup;
    }
    // Schluessel: die Schreibweise ohne Gross/Klein; Wert: die Schreibweise, die zuerst da war.
    Map<String, String> anzeige = new LinkedHashMap<>();
    Map<String, Long> summe = new LinkedHashMap<>();
    for (Verteilungssumme zeile : ausDemRollup) {
      String norm = normiert(zeile.schluessel());
      anzeige.putIfAbsent(norm, zeile.schluessel());
      summe.merge(norm, zeile.anzahl(), Long::sum);
    }
    for (Map.Entry<String, Long> eintrag : jeProzess.entrySet()) {
      String schluessel = schluesselJeProzess.get(eintrag.getKey());
      String norm = normiert(schluessel);
      anzeige.putIfAbsent(norm, schluessel);
      summe.merge(norm, eintrag.getValue(), Long::sum);
    }
    List<Verteilungssumme> verrechnet = new ArrayList<>();
    summe.forEach(
        (norm, anzahl) -> {
          if (anzahl > 0) {
            verrechnet.add(new Verteilungssumme(anzeige.get(norm), anzahl));
          }
        });
    return verrechnet;
  }

  /** {@code null} bleibt {@code null} — es ist der Schluessel von „nicht zugeordnet". */
  private static String normiert(String schluessel) {
    return schluessel == null ? null : schluessel.toUpperCase(Locale.ROOT);
  }
}
