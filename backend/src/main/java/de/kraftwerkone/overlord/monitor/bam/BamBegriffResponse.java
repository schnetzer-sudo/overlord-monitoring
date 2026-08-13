package de.kraftwerkone.overlord.monitor.bam;

import java.util.List;

/**
 * Ein Suchbegriff, so wie die Suche ihn verstanden hat — <b>samt der Fassungen, nach denen
 * tatsächlich gesucht wurde</b>.
 *
 * <p><b>Keine stille Korrektur.</b> Wer {@code 4711815} tippt und {@code 004711815} findet, muss
 * erfahren, warum: <i>„gesucht nach 4711815 und 004711815"</i>. Ohne diese Angabe sähe die
 * Trefferliste aus, als hätte die Datenbank etwas anderes enthalten als sie enthält — und ein
 * Nutzer, der die Nummer anschließend im Altwerkzeug nachschlägt, fände sie dort nicht.
 *
 * @param eingabe der Wert, wie der Nutzer ihn getippt hat, an den Rändern beschnitten
 * @param typ die genannte Belegart oder {@code null} für „unter jedem Typ"
 * @param varianten die gesuchten Fassungen, mit der Eingabe an erster Stelle. <b>Ohne die Fassung
 *     mit führendem Leerzeichen</b> — die Begründung steht bei {@link Varianten#gemeldet()}
 */
public record BamBegriffResponse(String eingabe, Short typ, List<String> varianten) {

  public BamBegriffResponse {
    varianten = List.copyOf(varianten);
  }
}
