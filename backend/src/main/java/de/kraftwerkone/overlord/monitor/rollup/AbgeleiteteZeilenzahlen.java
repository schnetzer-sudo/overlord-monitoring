package de.kraftwerkone.overlord.monitor.rollup;

/**
 * Wie viele Zeilen ein Schritt in den beiden <b>abgeleiteten</b> Ebenen geschrieben hat.
 *
 * <p><b>Warum das nicht {@link RollupZeilenzahlen} ist.</b> Dort steht die Stundenebene als erstes
 * Feld, und der Rueckwaertslauf schreibt sie gar nicht an. Eine Null hineinzuschreiben hiesse, „hat
 * keine geschrieben" und „faellt hier nicht an" unter demselben Wert zu fuehren — und der Zeile
 * waere das nicht anzusehen.
 *
 * @param tageszeilen Zeilen in {@code message_rollup_tag}
 * @param monatszeilen Zeilen in {@code message_rollup_monat}
 */
public record AbgeleiteteZeilenzahlen(int tageszeilen, int monatszeilen) {}
