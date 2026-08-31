package de.kraftwerkone.overlord.monitor.rollup;

/**
 * Wie viele Zeilen ein Lauf in jeder der <b>drei</b> Ebenen geschrieben hat.
 *
 * <p><b>Warum die drei getrennt gefuehrt werden und nicht addiert.</b> {@code
 * rollup_lauf.zeilen_geschrieben} traegt seit Schritt 10a die Zahl der <b>Stundenzeilen</b>, und
 * die Zeilen aus jenen Laeufen stehen heute noch in der Tabelle. Eine Summe der Ebenen ab jetzt
 * hineinzuschreiben hiesse, dieselbe Spalte in alten und neuen Zeilen verschieden zu lesen — ohne
 * dass der Zeile das anzusehen waere. Die Spalte bleibt deshalb, was sie war; Tages- und
 * Monatszeilen stehen im Protokoll und in {@link RollupErgebnis}.
 *
 * @param stundenzeilen Zeilen in {@code message_rollup} — die Zahl, die in {@code
 *     rollup_lauf.zeilen_geschrieben} landet
 * @param tageszeilen Zeilen in {@code message_rollup_tag}. <b>Nicht</b> die Zahl der beruehrten
 *     Tage: Ein Tag traegt so viele Zeilen, wie es an ihm Prozess-Status-Paare gab
 * @param monatszeilen Zeilen in {@code message_rollup_monat} (Schritt 10b-2). Dieselbe Lesart wie
 *     bei den Tageszeilen, eine Ebene hoeher
 */
public record RollupZeilenzahlen(int stundenzeilen, int tageszeilen, int monatszeilen) {}
