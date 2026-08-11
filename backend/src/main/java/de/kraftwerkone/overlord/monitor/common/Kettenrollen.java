package de.kraftwerkone.overlord.monitor.common;

import java.util.EnumSet;
import java.util.Set;

/**
 * Die <b>eine</b> Stelle, an der aus den vier Verkettungsspalten einer {@code Message}-Zeile ihre
 * Rollen werden — reine Rechenlogik, <b>ohne Datenbankzugriff und ohne {@code MandantContext}</b>.
 *
 * <p><b>Warum das in {@code common} liegt.</b> Dieselbe Aufteilung wie bei {@link BamSpaltenRegel}
 * und {@code message/Schrittnamen}: die Regel gemeinsam, die Statements je Fachpaket. Ab Schritt 7
 * braucht {@code bam} die Rollen fuer die Trefferliste, und Fachpakete kennen einander nicht — ein
 * Import aus {@code message} waere der Regelbruch, ein zweiter Nachbau die Drift. Der
 * <b>Datenzugriff</b> kann hier nicht liegen: {@code common} darf von keinem anderen
 * Anwendungspaket abhaengen und damit auch nicht vom {@code MandantContext}, den Regel M2 als
 * ersten Pflichtparameter jeder Methode auf {@code jooq.glassfish} verlangt.
 *
 * <p><b>Die Rolle ist eine Menge und kein Aufzaehlungswert.</b> Ueber Fenster B tragen 514 von
 * 214.330 Zeilen zwei Rollen (0,240 %), bei {@code IBISGUS} sind es 1,278 % (M28‑1c). Selten, aber
 * nicht nie — ein einzelner Wert gaebe diesen Zeilen still ein falsches Etikett. <b>Nie mehr als
 * zwei</b> Rollen je Zeile: Das ist gemessen und keine Garantie, weshalb {@code
 * KettenrollenBestandTest} es gegen die Testkopie festhaelt, statt dass der Code es voraussetzt.
 *
 * <h2>Der Ersatz fuer das vollstaendige {@code switch}</h2>
 *
 * <p>Beim {@link MessageStatusKind} schuetzt ein {@code switch} ohne {@code default} davor, dass
 * ein neuer Wert eine stille Voreinstellung erbt. <b>Bei einer Menge gibt es diesen Schutz
 * nicht</b> — jede der sechzehn Kombinationen ist syntaktisch gueltig, und keine loest einen
 * Compilerfehler aus. Ersatz ist {@code KettenrollenBestandTest}: Er erhebt die <i>vorkommenden</i>
 * Kombinationen ueber ein Bezugsfenster und vergleicht sie gegen die Liste in {@link
 * #FORMULIERTE_KOMBINATIONEN}. Taucht eine auf, fuer die es keine Formulierung gibt, wird er rot.
 *
 * <h2>Die {@code bit(1)}-Falle greift hier nicht</h2>
 *
 * <p>{@code Message.Source} und {@code Message.Target} sind {@code bit(1)}, und {@code SUM(Source)}
 * liefert in SQL keinen brauchbaren Wert — dort braucht es {@code SUM(Source + 0)} ({@code
 * datenmodell.md} §5.8, M23‑2). <b>Im Anwendungscode tritt das nicht auf:</b> Der jOOQ-Codegen
 * bildet {@code bit(1)} per {@code forcedType} auf {@link Boolean} ab ({@code datenzugriff.md} §9),
 * die Felder heissen hier also schlicht {@code Boolean}. Wer in Java ein {@code + 0} schreibt, hat
 * die Erhebung von Hand mit dem Anwendungscode verwechselt.
 */
public final class Kettenrollen {

  private Kettenrollen() {}

  /**
   * Alle Rollenkombinationen, fuer die es eine Formulierung gibt — <b>vollstaendig</b>, nicht nur
   * die gemessenen.
   *
   * <p>Sie enthaelt <b>alle sechs Paare</b>, die vier Einzelrollen und die leere Menge. <b>Drei</b>
   * der sechs Paare sind in der Testkopie belegt (M28‑1c, M30‑4): {@code Wurzel + Kind} (456 ueber
   * Fenster B, 4 ueber Fenster A), {@code Kind + Ergebnis} (33) und {@code Wurzel + Ergebnis} (25).
   * <b>Drei</b> kommen <b>nicht</b> vor — und alle drei enthalten {@code MERGE_EINGANG}: {@code
   * Wurzel + Eingang}, {@code Kind + Eingang} und {@code Eingang + Ergebnis}, je null ueber beide
   * Fenster.
   *
   * <p><b>Sie stehen trotzdem darin, und das ist kein Versehen.</b> „Kommt nicht vor" und „ist
   * nicht formulierbar" sind zweierlei: Die Liste sagt, worueber die Oberflaeche etwas sagen kann,
   * nicht, was gemessen wurde. {@code Kind + Eingang} ist zusaetzlich durch M28‑1 ausgeschlossen —
   * keine Zeile traegt beide ID-Spalten —, die beiden anderen sind nur <i>gemessen</i> null; {@code
   * Source} und {@code Target} sind Flags und keine ID-Spalten, aus M28‑1 folgt fuer sie nichts.
   *
   * <p><b>Kombinationen mit drei oder vier Rollen fehlen bewusst.</b> Sie sind in beiden Fenstern
   * null (M28‑1c). Taucht eine auf, wird {@code KettenrollenBestandTest} rot — und das ist der
   * Zweck: Eine Modellierung, die drei oder vier zulaesst, hat keinen Beleg.
   */
  public static final Set<Set<Kettenrolle>> FORMULIERTE_KOMBINATIONEN =
      Set.of(
          EnumSet.noneOf(Kettenrolle.class),
          EnumSet.of(Kettenrolle.SPLIT_WURZEL),
          EnumSet.of(Kettenrolle.SPLIT_KIND),
          EnumSet.of(Kettenrolle.MERGE_EINGANG),
          EnumSet.of(Kettenrolle.MERGE_ERGEBNIS),
          EnumSet.of(Kettenrolle.SPLIT_WURZEL, Kettenrolle.SPLIT_KIND),
          EnumSet.of(Kettenrolle.SPLIT_WURZEL, Kettenrolle.MERGE_EINGANG),
          EnumSet.of(Kettenrolle.SPLIT_WURZEL, Kettenrolle.MERGE_ERGEBNIS),
          EnumSet.of(Kettenrolle.SPLIT_KIND, Kettenrolle.MERGE_EINGANG),
          EnumSet.of(Kettenrolle.SPLIT_KIND, Kettenrolle.MERGE_ERGEBNIS),
          EnumSet.of(Kettenrolle.MERGE_EINGANG, Kettenrolle.MERGE_ERGEBNIS));

  /**
   * Wie viele Rollen eine Zeile hoechstens traegt — <b>gemessen, nicht garantiert</b>.
   *
   * <p>Ueber beide Bezugsfenster traegt keine Zeile drei oder vier Rollen (M28‑1c). Der Code setzt
   * das nirgends voraus; die Zahl steht hier, damit {@code KettenrollenBestandTest} sie gegen die
   * Testkopie halten kann und ein Gegenbeleg auffaellt, statt still durchzulaufen.
   */
  public static final int HOECHSTENS_ROLLEN_JE_ZEILE = 2;

  /**
   * Die Rollen einer {@code Message}-Zeile aus ihren vier Verkettungsspalten.
   *
   * <p>Das Ergebnis ist ein frisches {@link EnumSet} und laeuft damit in der
   * Deklarationsreihenfolge von {@link Kettenrolle} — die Reihenfolge in der Antwort haengt nicht
   * daran, in welcher Reihenfolge hier geprueft wird.
   *
   * <p><b>„Belegt" heisst: nicht {@code null} und nicht leer.</b> Gemessen ist, dass unbelegte
   * Verkettung ausnahmslos {@code NULL} ist — ueber 220.579 gepruefte Zeilen kein einziger leerer
   * String ({@code datenmodell.md} §5.9, M23‑2). Die Leerstring-Pruefung bleibt trotzdem: Die
   * Produktion muss sich nicht daran halten, was die Testkopie zufaellig enthaelt.
   *
   * <p><b>Ein {@code null}-Flag zaehlt als nicht gesetzt.</b> Beide Spalten sind {@code NULL}-fähig
   * mit Vorgabe {@code b'0'} (M23‑1); ein fehlender Wert ist keine Behauptung, es gebe Kinder.
   *
   * @param source {@code Message.Source} — der Rueckwaertsindex der Aufteilung. Deckt sich exakt
   *     mit „hat mindestens ein Kind" (E4: 479/479 und 0 von 5.770), <b>ohne Abfrage</b>
   * @param sourceMessageId {@code Message.SourceMessageID} — der Verweis des Kindes auf seinen
   *     Elternteil
   * @param targetMessageId {@code Message.TargetMessageID} — der Verweis des Eingangs auf das
   *     Ergebnis
   * @param target {@code Message.Target} — der Rueckwaertsindex der Zusammenfuehrung (E4: 6/6 und 0
   *     von 6.243)
   */
  public static EnumSet<Kettenrolle> aus(
      Boolean source, String sourceMessageId, String targetMessageId, Boolean target) {
    EnumSet<Kettenrolle> rollen = EnumSet.noneOf(Kettenrolle.class);
    if (gesetzt(source)) {
      rollen.add(Kettenrolle.SPLIT_WURZEL);
    }
    if (belegt(sourceMessageId)) {
      rollen.add(Kettenrolle.SPLIT_KIND);
    }
    if (belegt(targetMessageId)) {
      rollen.add(Kettenrolle.MERGE_EINGANG);
    }
    if (gesetzt(target)) {
      rollen.add(Kettenrolle.MERGE_ERGEBNIS);
    }
    return rollen;
  }

  /** Ein Flag zaehlt nur als gesetzt, wenn es tatsaechlich {@code true} ist. */
  private static boolean gesetzt(Boolean flag) {
    return Boolean.TRUE.equals(flag);
  }

  /** Eine Kennung zaehlt als belegt, wenn sie weder {@code null} noch leer ist. */
  private static boolean belegt(String kennung) {
    return kennung != null && !kennung.isBlank();
  }
}
