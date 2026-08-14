package de.kraftwerkone.overlord.monitor.bam;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Begriff, so wie das Statement ihn sieht: die Werte, die gemeinsam gesucht werden, der
 * BAM-Typ, falls der Nutzer einen genannt hat, und die Art des Vergleichs.
 *
 * <p>Die Umrechnung von {@link Suchbegriff} zu dieser Form ist die Normalisierung aus {@link
 * Sollaengen} — sie geschieht <b>vor</b> dem Statement, weil {@code bam_sollaenge} nie gegen {@code
 * GlassfishDB} gejoint wird.
 *
 * @param typ {@code null} heißt „unter jedem Typ". Ist er gesetzt, kommt {@code AND
 *     bN.MessageBAMType = ?} dazu — <b>Ergebnisverfeinerung, keine Entlastung</b> (M36: +1,5 bis +4
 *     Prozent). <b>Im Präfixmodus hat er eine zweite Rolle</b>: Nur mit ihm entstehen die
 *     Nullen-im-Muster-Fassungen ({@link Sollaengen})
 * @param werte mindestens einer (der rohe Wert), ohne Doppelte. <b>Immer lesbare Werte, nie
 *     Muster</b> — der Platzhalter entsteht erst in {@link #muster()}
 * @param modus exakt oder über den Anfang — er gilt für die ganze Suche und ist je Bedingung
 *     derselbe
 */
public record Suchbedingung(Short typ, List<String> werte, Suchmodus modus) {

  /**
   * Das Maskierungszeichen der {@code LIKE}-Muster.
   *
   * <p><b>Es ist derselbe wie in Regel Q1</b> ({@code LIKE 'ERROR\_%' ESCAPE '\'}, {@code
   * PROJEKTBESCHREIBUNG.md} §4.1) — aus Gründen der Wiedererkennung und nicht, weil er der einzig
   * mögliche wäre. Wer im Projekt auf ein maskiertes {@code LIKE} stößt, soll überall dasselbe
   * Zeichen sehen.
   */
  public static final char ESCAPE = '\\';

  public Suchbedingung {
    werte = List.copyOf(werte);
    if (werte.isEmpty()) {
      throw new IllegalArgumentException("Eine Suchbedingung ohne Werte gibt es nicht");
    }
  }

  /**
   * Die Werte als {@code LIKE}-Muster — <b>maskiert und mit angehängtem Platzhalter</b>.
   *
   * <h2>Die Maskierung ist Pflicht und keine Vorsichtsmaßnahme</h2>
   *
   * <p>M49‑4 hat gezählt: {@code _} steht in <b>2.696</b> Werten des Bestands, {@code %} in
   * <b>keinem</b>. Ohne Maskierung fände die Eingabe {@code 5_0} zusätzlich jeden Wert, der an
   * dieser Stelle ein beliebiges Zeichen trägt — <b>dieselbe Falle wie das naive {@code LIKE
   * 'ERROR_%'} aus Regel Q1</b>, nur an der Eingabe statt an einer Konstanten. Maskiert werden
   * {@code \}, {@code %} und {@code _}; das Maskierungszeichen selbst zuerst, sonst maskierte die
   * Maskierung sich gegenseitig.
   *
   * <h2>Folgende Leerzeichen werden abgeschnitten — hier und nur hier</h2>
   *
   * <p><b>Der Grund ist die Kollation.</b> {@code utf8mb4_general_ci} ist PAD SPACE: {@code '4711 '
   * = '4711'} ist wahr, {@code LIKE} folgt dieser Regel aber <b>nicht</b> — {@code '4711' LIKE
   * '4711 %'} ist falsch (M43‑3, in M49‑4 an fünf Ausdrücken belegt). Ein folgendes Leerzeichen
   * wäre unter {@code =} folgenlos und unter {@code LIKE} der Unterschied zwischen Treffer und
   * Leere.
   *
   * <p><b>Im exakten Modus wird nichts abgeschnitten</b> — nicht weil es dort schadete, sondern
   * weil dieser Pfad gebaut, getestet und in M47 gemessen ist und ohne Anlass nicht angefasst wird.
   *
   * <p>⚠️ <b>Über den Endpunkt feuert der Schnitt heute nie</b>, und das gehört dazu, damit niemand
   * ihn für wirkungslos hält und entfernt: {@link Suchbegriff#ausParameter} beschneidet den
   * Wertteil bereits an <i>beiden</i> Rändern, und keine der gebildeten Fassungen hängt hinten
   * etwas an. Der Schnitt steht hier, weil die Zusage an <b>dieser</b> Stelle gilt — wer je eine
   * Fassung baut, die auf ein Leerzeichen endet, soll sie nicht in ein stilles Nullergebnis laufen
   * lassen.
   *
   * <p><b>Führende Leerzeichen bleiben unangetastet.</b> Sie sind bedeutungstragend: Die
   * Leerzeichen-Fassung aus {@code bam_sollaenge} wird auch im Präfixmodus gebildet und ergibt
   * {@code LIKE ' 4711%'} (M46‑3).
   */
  public List<String> muster() {
    List<String> muster = new ArrayList<>(werte.size());
    for (String wert : werte) {
      muster.add(alsMuster(wert));
    }
    return List.copyOf(muster);
  }

  static String alsMuster(String wert) {
    String ohneFolgendeLeerzeichen = wert.stripTrailing();
    StringBuilder gebaut = new StringBuilder(ohneFolgendeLeerzeichen.length() + 4);
    for (int i = 0; i < ohneFolgendeLeerzeichen.length(); i++) {
      char zeichen = ohneFolgendeLeerzeichen.charAt(i);
      if (zeichen == ESCAPE || zeichen == '%' || zeichen == '_') {
        gebaut.append(ESCAPE);
      }
      gebaut.append(zeichen);
    }
    return gebaut.append('%').toString();
  }
}
