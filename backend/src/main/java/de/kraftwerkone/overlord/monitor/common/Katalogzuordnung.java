package de.kraftwerkone.overlord.monitor.common;

import java.util.Locale;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * Wann ein kuratiertes Katalogfeld als <b>zugeordnet</b> gilt — <b>Entscheidung E-i, an genau einer
 * Stelle und in beiden Sprachen</b>.
 *
 * <p>Zugeordnet ist ein Prozess nur, wenn <b>alle drei</b> Bedingungen halten; faellt eine, ist der
 * Wert „nicht zugeordnet":
 *
 * <ol>
 *   <li>Es gibt ueberhaupt eine Katalogzeile.
 *   <li>Sie traegt {@link Pflegestatus#GEPFLEGT} — ein offener Regelvorschlag ist eine Vermutung
 *       und keine Zuordnung (Regel Q4: es wird nichts geraten).
 *   <li>Das Feld ist gefuellt — „gepflegt mit leerem Partner" heisst <i>hingesehen, es gibt
 *       keinen</i> ({@code docs/prozess-katalog.md} E4) und faellt fachlich mit „nicht zugeordnet"
 *       zusammen.
 * </ol>
 *
 * <h2>Warum beide Sprachen hier nebeneinanderstehen</h2>
 *
 * <p><i>(seit 02.09.2026, Schritt 10c-1.)</i> Die SQL-Fassung stand bis dahin als private Methode
 * in {@code dashboard/DashboardRepository}. Die <b>Prozessansicht</b> braucht dieselbe Regel, aber
 * in <b>Java</b>: Sie liest Partner, Richtung und Pflegestatus in den Speicher und gruppiert den
 * Baum dort. Die Regel deshalb ein zweites Mal zu schreiben hiesse, sie zweimal richtig halten zu
 * muessen — und die beiden Fassungen liefen beim naechsten Katalogzustand auseinander, ohne dass es
 * jemandem auffiele: Die Verteilung des Dashboards und der Baum zeigten dieselbe Zahl verschieden.
 *
 * <p><b>Das ist dieselbe Bauform wie in {@link MessageStatusClassifier}</b> — {@code
 * istUeberfaellig} und {@code ueberfaelligBedingung} stehen dort aus demselben Grund nebeneinander:
 * eine Regel, zwei Ausdrucksformen, ein Ort.
 *
 * <p><b>Die Felder werden uebergeben und nicht importiert.</b> {@code process_catalog} liegt zwar
 * im eigenen Schema und nicht in {@code jooq.glassfish}, aber {@code common} soll auch von
 * generierten Tabellen frei bleiben — sonst haengt das Fundament an der Codegenerierung. Dieselbe
 * Ueberlegung wie bei {@link MessageStatusClassifier#fehlerBedingung(Field)}.
 */
public final class Katalogzuordnung {

  private Katalogzuordnung() {}

  /**
   * Die Java-Fassung: Traegt diese Katalogzeile einen zugeordneten Wert?
   *
   * @param pflegestatus der Rohwert aus {@code process_catalog.pflegestatus}, {@code null} wenn es
   *     gar keine Katalogzeile gibt
   * @param wert der Rohwert des kuratierten Feldes ({@code partner} oder {@code richtung})
   * @return {@code true} nur, wenn alle drei Bedingungen halten
   */
  public static boolean zugeordnet(String pflegestatus, String wert) {
    return Pflegestatus.GEPFLEGT.name().equals(pflegestatus) && wert != null && !wert.isEmpty();
  }

  /**
   * Der zugeordnete Wert oder {@code null} — der <b>Anzeigewert</b>, unveraendert wie im Katalog.
   *
   * <p><b>{@code null} und nicht ein Ersatztext.</b> Was der Nutzer anstelle einer fehlenden
   * Zuordnung liest, ist eine Oberflaechenentscheidung und gehoert in die Sprachdateien, nicht in
   * eine Abfrage (Regel Q4).
   */
  public static String schluessel(String pflegestatus, String wert) {
    return zugeordnet(pflegestatus, wert) ? wert : null;
  }

  /**
   * Derselbe Wert als <b>Gruppierungsschluessel</b> — hochgestellt, damit Java so gruppiert, wie
   * die Datenbank vergleicht.
   *
   * <h2>Der Anlass, und er ist gemessen (M117, 02.09.2026)</h2>
   *
   * <p>{@code process_catalog.partner} traegt die Sortierung {@code utf8mb4_general_ci}. Fuer die
   * Datenbank sind zwei Schreibweisen desselben Namens <b>ein</b> Wert; fuer {@link String#equals}
   * sind es zwei. Bei {@code NEXANS} steht genau ein Partner in zwei Schreibweisen im Katalog — die
   * Verteilung des Dashboards zaehlt deshalb <b>154</b> Partner, der Baum zaehlte ohne diese Zeile
   * <b>155</b> und zeigte denselben Partner zweimal, mit geteilten Zahlen.
   *
   * <p><b>Zwei Ansichten derselben Daten duerfen sich nicht widersprechen.</b> Welche Werte gleich
   * sind, entscheidet die Sortierung der Spalte und nicht das Gruppierungsverfahren des Aufrufers.
   *
   * <h2>Was diese Naeherung nicht leistet, und das steht hier statt in einer Fussnote</h2>
   *
   * <p>{@code utf8mb4_general_ci} ignoriert <b>mehr</b> als die Gross- und Kleinschreibung: Es
   * behandelt auch {@code a} und {@code ä} als gleich. {@link String#toUpperCase} tut das nicht.
   * <b>Zwei Werte, die sich nur in einem Akzent unterscheiden, blieben hier zwei Gruppen und waeren
   * fuer die Datenbank eine.</b> Der exakte Weg waere {@code WEIGHT_STRING} in SQL — der
   * Sortierungsschluessel selbst. Er ist nicht gebaut: Der gemessene Fall ist eine
   * Schreibweisenkollision, kein Akzent, und ein MariaDB-eigener Funktionsaufruf im Antwortpfad
   * will eigens gemessen sein. Der Punkt steht offen in {@code docs/process-view.md} §10.
   *
   * <p>{@link Locale#ROOT}, damit die Umwandlung nicht an der Systemsprache haengt: Im tuerkischen
   * Gebietsschema wird aus {@code i} ein {@code İ}. Dieselbe Ueberlegung wie in {@code
   * MessageStatusClassifier.einordnung}.
   */
  public static String gruppenschluessel(String pflegestatus, String wert) {
    String anzeige = schluessel(pflegestatus, wert);
    return anzeige == null ? null : anzeige.toUpperCase(Locale.ROOT);
  }

  /**
   * Die SQL-Fassung derselben Regel.
   *
   * <p>Der Aufrufer setzt sie entweder als Bedingung ein oder — wie die Verteilung des Dashboards —
   * in ein {@code CASE WHEN … THEN wert}. <b>Dort steht sie als <i>ein</i> Ausdruck in {@code
   * SELECT}, {@code GROUP BY} und {@code ORDER BY}</b>, und das ist kein Stil: MariaDB loest {@code
   * GROUP BY} zuerst gegen Tabellenspalten auf und erst danach gegen Ausdrucksaliasse. Hiesse der
   * Alias wie die Spalte, gruppierte die Datenbank still nach dem Rohwert.
   *
   * @param schluessel die Schluesselspalte der Katalogzeile — ueber sie wird gefragt, ob der {@code
   *     LEFT JOIN} ueberhaupt getroffen hat
   * @param pflegestatus die Spalte {@code pflegestatus}
   * @param wert die kuratierte Spalte ({@code partner} oder {@code richtung})
   */
  public static Condition zugeordnet(
      Field<String> schluessel, Field<String> pflegestatus, Field<String> wert) {
    return schluessel
        .isNotNull()
        .and(pflegestatus.eq(Pflegestatus.GEPFLEGT.name()))
        .and(wert.isNotNull())
        .and(wert.ne(""));
  }
}
