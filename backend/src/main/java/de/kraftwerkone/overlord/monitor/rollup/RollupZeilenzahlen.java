package de.kraftwerkone.overlord.monitor.rollup;

/**
 * Wie viele Zeilen ein Lauf in jeder der <b>beiden</b> Ebenen geschrieben hat.
 *
 * <p><b>Warum die beiden getrennt gefuehrt werden und nicht addiert.</b> {@code
 * rollup_lauf.zeilen_geschrieben} traegt seit Schritt 10a die Zahl der <b>Stundenzeilen</b>, und
 * die Zeilen aus jenen Laeufen stehen heute noch in der Tabelle. Eine Summe beider Ebenen ab jetzt
 * hineinzuschreiben hiesse, dieselbe Spalte in alten und neuen Zeilen verschieden zu lesen — ohne
 * dass der Zeile das anzusehen waere. Die Spalte bleibt deshalb, was sie war; die Tageszeilen
 * stehen im Protokoll und in {@link RollupErgebnis}.
 *
 * @param stundenzeilen Zeilen in {@code message_rollup} — die Zahl, die in {@code
 *     rollup_lauf.zeilen_geschrieben} landet
 * @param tageszeilen Zeilen in {@code message_rollup_tag}. <b>Nicht</b> die Zahl der beruehrten
 *     Tage: Ein Tag traegt so viele Zeilen, wie es an ihm Prozess-Status-Paare gab
 */
public record RollupZeilenzahlen(int stundenzeilen, int tageszeilen) {}
