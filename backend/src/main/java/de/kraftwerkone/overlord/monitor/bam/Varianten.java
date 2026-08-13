package de.kraftwerkone.overlord.monitor.bam;

import java.util.List;

/**
 * Die Fassungen <b>eines</b> Suchbegriffs, mit denen tatsächlich gesucht wird — und die Teilmenge
 * davon, die der Antwort beiliegt.
 *
 * <p><b>Die beiden Listen sind nicht dieselbe, und der Unterschied ist genau eine Fassung.</b>
 *
 * @param gesucht alle Werte der {@code IN}-Liste: der rohe Wert, die aufgefüllten Fassungen und —
 *     wo das Kennzeichen gesetzt ist — die Fassung mit führendem Leerzeichen. Doppelte sind
 *     entfernt, die Reihenfolge ist roh, aufgefüllt (aufsteigend), Leerzeichen
 * @param gemeldet dieselbe Liste <b>ohne</b> die Leerzeichen-Fassung. Sie steht in der Antwort:
 *     <i>„gesucht nach 4711815 und 004711815"</i> — keine stille Korrektur.
 *     <p><b>Warum die Leerzeichen-Fassung nicht gemeldet wird.</b> Sie ist für den Nutzer nicht
 *     nachvollziehbar: Ein führendes Leerzeichen sieht er weder in seiner Eingabe noch im
 *     angezeigten Wert, und eine Zeile <i>„gesucht nach 4711 und ␣4711"</i> läse sich wie ein
 *     Anzeigefehler. Die führende Null ist das Gegenteil — sie steht auf dem Beleg nicht, im
 *     Bestand aber sichtbar, und die Meldung erklärt dem Nutzer genau den Unterschied. Wohin das
 *     Leerzeichen gehört, ist die Hilfe zur Suche; sie entsteht in Teil 3.
 */
public record Varianten(List<String> gesucht, List<String> gemeldet) {

  public Varianten {
    gesucht = List.copyOf(gesucht);
    gemeldet = List.copyOf(gemeldet);
  }
}
