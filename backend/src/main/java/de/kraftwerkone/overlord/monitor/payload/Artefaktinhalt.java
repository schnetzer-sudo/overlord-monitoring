package de.kraftwerkone.overlord.monitor.payload;

/**
 * Das aufbereitete Artefakt — die <b>eine</b> Fassung, aus der sowohl die Anzeige als auch der
 * Download entsteht.
 *
 * <p><b>Darin steckt der Gleichlauf</b> ({@code docs/rohdaten.md} §3, Entscheidung 9). Es gibt
 * nicht zwei Wege durch die Aufbereitung, einen fuer die Anzeige und einen fuer den Download,
 * sondern einen. Waeren es zwei, muesste jemand sie synchron halten — und der Tag, an dem das
 * misslingt, ist der Tag, an dem ein Mandantennutzer ueber den Download die vollstaendige
 * Protokolldatei bekommt.
 *
 * @param zustand einer der fuenf Zustaende
 * @param bytes was der Download ausliefert. Bei {@link Artefaktzustand#ANZEIGBAR} ohne Beschnitt
 *     die rohen Bytes des ZIP-Eintrags; mit Beschnitt der beschnittene Text nach {@code ISO-8859-1}
 *     zurueckkodiert; bei {@link Artefaktzustand#BINAERDATEI} ebenfalls die rohen Bytes. In allen
 *     anderen Zustaenden {@code null} — dann gibt es nichts auszuliefern
 * @param text was die Anzeige zeigt. Leer, wenn {@code zustand} nicht {@link
 *     Artefaktzustand#ANZEIGBAR} ist
 * @param groesseBytes die Groesse der vollstaendigen entpackten Datei. Auch dann, wenn beschnitten
 *     oder gekuerzt wurde — nur so ist ablesbar, wie viel fehlt
 * @param gekuerzt ob die <b>Anzeige</b> an der Laengengrenze gekappt wurde. Der Download bleibt
 *     vollstaendig: Die Kappung schuetzt den Browser, nicht die Vertraulichkeit
 * @param beschnitten ob der Markenbeschnitt gegriffen hat. Diese Angabe geht ins {@code audit_log}
 * @param zipEintraege wie viele Eintraege das Archiv trug. {@code 0}, wenn nicht entpackt wurde
 */
public record Artefaktinhalt(
    Artefaktzustand zustand,
    byte[] bytes,
    String text,
    long groesseBytes,
    boolean gekuerzt,
    boolean beschnitten,
    int zipEintraege) {

  /** Ein Zustand ohne Inhalt — Ablage aus, Datei weg, oder nichts zwischen den Marken. */
  static Artefaktinhalt ohneInhalt(Artefaktzustand zustand, boolean beschnitten) {
    return new Artefaktinhalt(zustand, null, "", 0, false, beschnitten, 0);
  }

  /** Ob es Bytes zum Ausliefern gibt. */
  public boolean lieferbar() {
    return bytes != null;
  }

  /** Bewusst ueberschrieben: kein Byte und kein Zeichen des Inhalts geraet ins Protokoll. */
  @Override
  public String toString() {
    return "Artefaktinhalt["
        + zustand
        + ", "
        + groesseBytes
        + " Byte, beschnitten="
        + beschnitten
        + ", gekuerzt="
        + gekuerzt
        + "]";
  }
}
