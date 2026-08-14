package de.kraftwerkone.overlord.monitor.bam;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Die Sollängen-Kuratierung <b>eines</b> Mandanten und die eine Rechenvorschrift, die daraus
 * Suchvarianten macht — <b>reine Logik, ohne Datenbankzugriff</b>.
 *
 * <p><b>Die Varianten entstehen vor dem Statement, nicht darin.</b> {@code bam_sollaenge} wird nie
 * gegen {@code GlassfishDB} gejoint ({@code docs/bam-sollaengen.md} §6); die Tabelle hat sechzehn
 * Zeilen, ein Vollabzug für den Mandanten der Sitzung ist der richtige Zugriff.
 *
 * <h2>Warum überhaupt aufgefüllt wird</h2>
 *
 * <p>Der Nutzer tippt die Nummer vom Beleg ab, und führende Nullen stehen dort nicht. M43‑4 misst
 * die Folge an echten Werten: Bei <b>sechs von acht</b> geprüften Typen findet die rohe Fassung
 * <b>null</b> Treffer und die aufgefüllte die richtigen. Nicht „weniger" — <b>keine</b>. Und eine
 * leere Trefferliste sieht aus wie „gibt es nicht", nicht wie „falsch getippt".
 *
 * <h2>Die drei Regeln, die die Variantenmenge klein halten</h2>
 *
 * <ol>
 *   <li><b>Aus den verschiedenen Sollängen bilden, nicht je Typ.</b> {@code NEXANS} hat zehn
 *       kuratierte Zeilen, davon acht mit einer Sollänge — aber nur <b>drei verschiedene</b>: 10, 4
 *       und 3 (gezählt in M47; der Auftrag zu Teil 2b nennt „elf Sollängen", die Tabelle führt
 *       acht).
 *   <li><b>Nur auffüllen, nie kürzen.</b> Sollängen, die nicht größer sind als die Eingabe,
 *       entfallen. Für eine siebenstellige Eingabe bei {@code NEXANS} bleibt damit genau
 *       <b>eine</b> aufgefüllte Variante.
 *   <li><b>Doppelte entfernen.</b> Erzeugen zwei Regeln denselben Wert, steht er einmal in der
 *       Liste.
 * </ol>
 *
 * <p><b>Warum nur aufgefüllt und nie gekürzt wird.</b> Auffüllen macht einen Wert länger und damit
 * spezifischer. M43‑4 zeigt die Gegenrichtung: Bei 9036 und 9006 fand die <i>rohe</i>, kurze
 * Fassung <b>1.642</b> beziehungsweise 2.371 Treffer statt der wenigen richtigen — kurze Kerne
 * kommen unter anderen Typen vielfach vor. Die kurze Fassung ist dort nicht die großzügigere,
 * sondern die unbrauchbare.
 *
 * <h2>Was nicht behandelt wird</h2>
 *
 * <p><b>Folgende Leerzeichen.</b> {@code utf8mb4_general_ci} ist eine PAD-SPACE-Kollation: {@code
 * 'a ' = 'a'} ist wahr, der {@code =}-Vergleich ignoriert sie (M43‑3, an echten Werten gegengeprüft
 * — 12 Treffer mit wie ohne Leerzeichen). <b>Hier ist nichts nachzurüsten.</b>
 *
 * <p><b>Und die Kehrseite, damit sie niemand übersieht:</b> {@code LIKE} folgt der PAD-SPACE-Regel
 * <i>nicht</i> ({@code 'a' LIKE 'a '} ist falsch). Sie ist seit Teil 4 kein offener Punkt mehr,
 * sondern behandelt — der Schnitt sitzt in {@link Suchbedingung#muster()}.
 *
 * <h2>Der Präfixmodus rechnet anders, und das ist gemessen</h2>
 *
 * <p><b>Auffüllen und ein Präfixmuster ankern gegeneinander.</b> M49‑1 hat es in <b>allen acht</b>
 * prüfbaren Fällen gemessen: Wer {@code 47118} tippt und {@code 0004711815} sucht, findet mit
 * {@code LIKE '47118%'} nichts — und mit der auf die Sollänge aufgefüllten Fassung {@code LIKE
 * '0000047118%'} erst recht nicht. <b>Die Variantenbildung des exakten Modus ist im Präfixmodus
 * wirkungslos</b> und darf dort nicht einfach weiterlaufen.
 *
 * <p>Der Ausweg ist gemessen und funktioniert ebenfalls in allen acht Fällen: <b>die Nullen ins
 * Muster ziehen</b> — ein Muster je plausibler Nullenzahl. Er entsteht aber <b>nur für Begriffe mit
 * Typ</b>, und dafür gibt es drei gemessene Gründe:
 *
 * <ol>
 *   <li><b>Der Hauptfall braucht ihn gar nicht.</b> 9018 sitzt bei {@code NEXANS} auf <b>92,26
 *       Prozent</b> der Wurzeln (M39) und hat <b>keine</b> Sollänge — dort ist die rohe Präfixsuche
 *       wirksam und kostet keine einzige Zusatzfassung.
 *   <li><b>Ohne Typ müssten alle Sollängen des Mandanten bedient werden.</b> M49‑1 zählt <b>3 bis
 *       7</b> Fassungen je Begriff bei <i>einer</i> Sollänge; die heutige Obergrenze liegt bei fünf
 *       <b>insgesamt</b> (M47). Bei acht erlaubten Begriffen wären es sonst bis zu 56 {@code
 *       LIKE}-Zweige.
 *   <li><b>Bei kurzen Kernen kippt der Ausweg.</b> 9006 (Kern 3 Zeichen) erzeugt <b>1.245.618</b>,
 *       9036 (Kern 1 Zeichen) <b>1.156.360</b> Kandidatenzeilen. Ein gewählter Typ macht daraus
 *       eine bewusste Eingrenzung statt einer Nebenwirkung.
 * </ol>
 *
 * <p><b>Die Typwahl bekommt damit eine zweite Rolle.</b> Bisher war sie reine Ergebnisverfeinerung
 * (M36: +1,5 bis +4 %). Das bleibt für die exakte Suche richtig; im Präfixmodus ist sie zusätzlich
 * die Voraussetzung dafür, dass über führende Nullen hinweg gesucht werden kann. Das ist
 * <b>kein</b> Widerspruch zu M36 — dort geht es um Geschwindigkeit, hier darum, welche Fassungen
 * bildbar sind.
 */
public record Sollaengen(List<BamSollaengeZeile> zeilen) {

  /** Das Zeichen, mit dem aufgefüllt wird. */
  private static final char FUELLZEICHEN = '0';

  /** Die Fassung, die ein Paar mit gesetztem Kennzeichen zusätzlich sucht. */
  private static final String FUEHRENDES_LEERZEICHEN = " ";

  public Sollaengen {
    zeilen = List.copyOf(zeilen);
  }

  /** Ein Mandant ohne jede kuratierte Zeile — dann wird ausschließlich roh gesucht. */
  public static Sollaengen keine() {
    return new Sollaengen(List.of());
  }

  /**
   * Die Fassungen, mit denen dieser Begriff gesucht wird — <b>je Modus eine andere Rechnung</b>.
   *
   * <p><b>Der Schlüssel trägt den Mandanten, und das ist gemessen</b> (M46‑2): Typ 2000 dominiert
   * bei {@code SUTTONS} mit Länge 6 und bei {@code VOTG} mit Länge 7; Typ 9014 fällt über den
   * Bestand mit 58,45 % durch die Schwelle und erreicht bei {@code WOC} 95,21 %. Eine typweite
   * Sollänge träfe beide Fälle falsch. Deshalb kennt diese Klasse ausschließlich die Zeilen
   * <i>eines</i> Mandanten — in beiden Modi.
   *
   * <p>Die Liste trägt in beiden Modi <b>lesbare Werte und keine Muster</b>. Der Platzhalter kommt
   * erst in {@link Suchbedingung#muster()} dazu; damit steht in der Antwort dasselbe, was der
   * Nutzer lesen kann.
   */
  public Varianten fuer(Suchbegriff begriff, Suchmodus modus) {
    return modus == Suchmodus.PRAEFIX ? praefix(begriff) : exakt(begriff);
  }

  /**
   * Die Fassungen des exakten Modus — <b>unverändert seit Teil 2b</b>.
   *
   * <p><b>Mit Typ</b> gilt die Sollänge genau dieses Paares aus {@code (mandant_id,
   * message_bam_type)}; <b>ohne Typ</b> alle für diesen Mandanten kuratierten <i>verschiedenen</i>
   * Sollängen. Existiert für ein Paar keine Zeile oder ist die Sollänge {@code null}, wird für
   * diesen Typ <b>nur roh</b> gesucht — kein Fehlerfall.
   */
  private Varianten exakt(Suchbegriff begriff) {
    String roh = begriff.wert();
    Set<String> gemeldet = new LinkedHashSet<>();
    gemeldet.add(roh);
    for (int sollaenge : sollaengen(begriff.typ())) {
      if (sollaenge > roh.length()) {
        gemeldet.add(aufgefuellt(roh, sollaenge));
      }
    }

    Set<String> gesucht = new LinkedHashSet<>(gemeldet);
    if (fuehrendesLeerzeichen(begriff.typ())) {
      gesucht.add(FUEHRENDES_LEERZEICHEN + roh);
    }
    return new Varianten(List.copyOf(gesucht), List.copyOf(gemeldet));
  }

  /**
   * Die Fassungen des Präfixmodus — <b>die Nullen wandern ins Muster, statt den Wert zu
   * verlängern</b>.
   *
   * <p><b>Ein Begriff ohne Typ bekommt genau eine Fassung: die rohe.</b> Die drei Gründe stehen
   * oben am Typ und sind gemessen; hier steht nur die Regel. Das gilt auch für die
   * Leerzeichen-Fassung — sie hängt an einem kuratierten Paar, und ohne Typ gibt es kein Paar.
   *
   * <p><b>Mit Typ und Sollänge</b> entsteht ein Muster je plausibler Nullenzahl: {@code j} von 0
   * bis {@code sollaenge − länge(eingabe) − 1}. Das sind {@code sollaenge − länge(eingabe)}
   * Fassungen einschließlich der rohen — für eine siebenstellige Eingabe auf der Sollänge 10 also
   * <b>drei</b>, für eine dreistellige <b>sieben</b>. Genau die Spanne, die M49‑1 gemessen hat.
   *
   * <p><b>Warum die Reihe eine Null vor der Sollänge endet.</b> Bei {@code j = sollaenge −
   * länge(eingabe)} wäre das Muster selbst schon so lang wie die Sollänge — der Kern des Werts wäre
   * dann vollständig eingetippt, und das ist der Fall, den die <i>exakte</i> Suche mit ihrer
   * aufgefüllten Fassung bereits vollständig trifft.
   *
   * <p><b>Nur auffüllen, nie kürzen — auch hier.</b> Ist die Eingabe so lang wie die Sollänge oder
   * länger, bleibt es bei der rohen Fassung; die Schleife läuft dann nicht.
   */
  private Varianten praefix(Suchbegriff begriff) {
    String roh = begriff.wert();
    Set<String> gemeldet = new LinkedHashSet<>();
    gemeldet.add(roh);
    if (begriff.mitTyp()) {
      for (int sollaenge : sollaengen(begriff.typ())) {
        for (int nullen = 1; nullen <= sollaenge - roh.length() - 1; nullen++) {
          gemeldet.add(String.valueOf(FUELLZEICHEN).repeat(nullen) + roh);
        }
      }
    }

    Set<String> gesucht = new LinkedHashSet<>(gemeldet);
    if (begriff.mitTyp() && fuehrendesLeerzeichen(begriff.typ())) {
      gesucht.add(FUEHRENDES_LEERZEICHEN + roh);
    }
    return new Varianten(List.copyOf(gesucht), List.copyOf(gemeldet));
  }

  /**
   * Die in Frage kommenden Sollängen, aufsteigend und ohne Doppelte.
   *
   * <p>Ohne Typ sind es die <b>verschiedenen</b> Sollängen des Mandanten und nicht eine je Zeile —
   * bei {@code NEXANS} also drei statt acht.
   */
  private SortedSet<Integer> sollaengen(Short typ) {
    SortedSet<Integer> laengen = new TreeSet<>();
    for (BamSollaengeZeile zeile : zeilen) {
      if (zeile.sollaenge() != null && (typ == null || zeile.typ() == typ)) {
        laengen.add(zeile.sollaenge());
      }
    }
    return laengen;
  }

  /**
   * Ob für diesen Begriff zusätzlich die Fassung mit führendem Leerzeichen gesucht wird.
   *
   * <p>Ohne Typ genügt <b>ein</b> Paar mit Kennzeichen: Die Fassung ist unabhängig vom Typ dieselbe
   * und kostet deshalb genau eine Variante, egal wie viele Paare sie tragen. Über den Bestand sind
   * es ohnehin nur zwei, beide bei {@code NEXANS} (M46‑3).
   */
  private boolean fuehrendesLeerzeichen(Short typ) {
    for (BamSollaengeZeile zeile : zeilen) {
      if (zeile.fuehrendesLeerzeichen() && (typ == null || zeile.typ() == typ)) {
        return true;
      }
    }
    return false;
  }

  private static String aufgefuellt(String wert, int sollaenge) {
    return String.valueOf(FUELLZEICHEN).repeat(sollaenge - wert.length()) + wert;
  }
}
