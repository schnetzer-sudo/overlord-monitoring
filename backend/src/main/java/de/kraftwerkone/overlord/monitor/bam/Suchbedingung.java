package de.kraftwerkone.overlord.monitor.bam;

import java.util.List;

/**
 * Ein Begriff, so wie das Statement ihn sieht: die Werte, die gemeinsam über {@code IN} gesucht
 * werden, und der BAM-Typ, falls der Nutzer einen genannt hat.
 *
 * <p>Die Umrechnung von {@link Suchbegriff} zu dieser Form ist die Normalisierung aus {@link
 * Sollaengen} — sie geschieht <b>vor</b> dem Statement, weil {@code bam_sollaenge} nie gegen {@code
 * GlassfishDB} gejoint wird.
 *
 * @param typ {@code null} heißt „unter jedem Typ". Ist er gesetzt, kommt {@code AND
 *     bN.MessageBAMType = ?} dazu — <b>Ergebnisverfeinerung, keine Entlastung</b> (M36: +1,5 bis +4
 *     Prozent)
 * @param werte mindestens einer (der rohe Wert), ohne Doppelte
 */
public record Suchbedingung(Short typ, List<String> werte) {

  public Suchbedingung {
    werte = List.copyOf(werte);
    if (werte.isEmpty()) {
      throw new IllegalArgumentException("Eine Suchbedingung ohne Werte gibt es nicht");
    }
  }
}
