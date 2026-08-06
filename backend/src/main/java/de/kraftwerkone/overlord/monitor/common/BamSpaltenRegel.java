package de.kraftwerkone.overlord.monitor.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Welche BAM-Typen ein Mandant als Spalten sieht — die <b>eine</b> Stelle, an der das entschieden
 * wird.
 *
 * <p>Die Regel steht in {@code docs/datenzugriff.md} §5 und lautet:
 *
 * <ol>
 *   <li>Gibt es kuratierte Zeilen in {@code overlord_monitor.bam_spalte} fuer den aktiven
 *       Mandanten, gelten <b>sie</b> — in der Reihenfolge {@code position}.
 *   <li>Sonst die zwei <b>kleinsten</b> {@code MessageBAMTypeSortIndex} aus {@code
 *       MessageBAMMandant}. Bei Gleichstand entscheidet {@code MessageBAMType} aufsteigend — {@code
 *       VOTG} vergibt den Sortierindex {@code 2002} zweimal (Messung M7), ohne diesen Zusatz waere
 *       die Reihenfolge dort zufaellig und bei jeder Abfrage womoeglich eine andere.
 *   <li>Hat ein Mandant nur <b>einen</b> Typ, gibt es <b>eine</b> Spalte.
 *   <li>Hat er <b>keinen</b>, gibt es <b>keine</b>. Keine leere Spalte, kein Platzhalter — eine
 *       Spalte ohne Inhalt behauptet, es gaebe dort etwas zu sehen.
 * </ol>
 *
 * <p><b>Warum ueberhaupt kuratiert wird.</b> Fuer sechs der sieben konfigurierten Mandanten trifft
 * der Sortierindex die richtige Wahl. Bei {@code NEXANS} nicht: Die beiden kleinsten Indizes sind
 * „Abladestelle" und „Abrufnummer" — die Abladestelle ist genau das Feld, das millionenfach
 * denselben Wert traegt. Die Lieferschein-Nr. liegt auf Platz sieben. Also kuratiert statt geraten
 * (Regel Q4): Die Heuristik befuellt vor, die Wahrheit steht in der Tabelle.
 *
 * <p><b>Warum die Regel in {@code common} steht und der Zugriff nicht.</b> Sie wird von zwei
 * Fachpaketen gebraucht — von {@code message} fuer die Spalten der Liste und ab Schritt 7 von
 * {@code bam} fuer die Suchfelder —, und Fachpakete kennen einander nicht. Der <b>Datenzugriff</b>
 * kann hier trotzdem nicht liegen: {@code common} darf von keinem anderen Anwendungspaket abhaengen
 * und damit auch nicht vom {@code MandantContext} aus {@code security}, den Regel M2 als ersten
 * Pflichtparameter jeder Methode auf {@code jooq.glassfish} verlangt. Beides zugleich ist nicht
 * moeglich; getrennt wird deshalb dort, wo die Entscheidung liegt — die Regel gemeinsam, die zwei
 * Statements je Fachpaket.
 */
public final class BamSpaltenRegel {

  /**
   * Hoechstens zwei Spalten. Mehr passen in eine Liste, die auch auf einem Notebook lesbar bleiben
   * soll, nicht sinnvoll hinein — und die dritte Kennung findet der Nutzer ueber die Suche.
   */
  public static final int HOECHSTENS = 2;

  private BamSpaltenRegel() {}

  /**
   * Eine kuratierte Zeile aus {@code overlord_monitor.bam_spalte}.
   *
   * @param position 1 oder 2
   */
  public record Kuratiert(int position, short typ) {}

  /**
   * Eine Zeile aus {@code GlassfishDB.MessageBAMMandant} — die Konfiguration des Altsystems.
   *
   * @param sortIndex {@code MessageBAMTypeSortIndex}, laut Schema {@code NOT NULL}
   */
  public record Konfiguriert(short typ, short sortIndex) {}

  /**
   * Die gewaehlten Typen in Anzeigereihenfolge — leer, eine oder zwei.
   *
   * @param kuratiert Zeilen aus {@code bam_spalte}, Reihenfolge egal
   * @param konfiguriert Zeilen aus {@code MessageBAMMandant}, Reihenfolge egal
   */
  public static List<Short> waehle(List<Kuratiert> kuratiert, List<Konfiguriert> konfiguriert) {
    if (!kuratiert.isEmpty()) {
      return kuratiert.stream()
          .sorted(Comparator.comparingInt(Kuratiert::position))
          .map(Kuratiert::typ)
          .distinct()
          .limit(HOECHSTENS)
          .toList();
    }
    List<Short> gewaehlt = new ArrayList<>();
    konfiguriert.stream()
        .sorted(
            Comparator.comparingInt((Konfiguriert eintrag) -> eintrag.sortIndex())
                .thenComparingInt(Konfiguriert::typ))
        .limit(HOECHSTENS)
        .forEach(eintrag -> gewaehlt.add(eintrag.typ()));
    return List.copyOf(gewaehlt);
  }
}
