package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Was ein Heuristik-Lauf fuer den aktiven Mandanten bewirkt hat (E13).
 *
 * <p>Die drei ersten Zahlen ergeben zusammen die Zahl der Prozesse des Mandanten. Die drei letzten
 * zaehlen nur die <b>geschriebenen</b> Zeilen ({@code angelegt + aufgefrischt}) nach der Herkunft
 * ihres Partnervorschlags — unberuehrte Zeilen tragen ihre eigene, aeltere Herkunft und werden hier
 * nicht mitgezaehlt.
 *
 * <p>Auf einem leeren Katalog reproduzieren die drei Herkunftszahlen damit genau die Tabelle aus
 * {@code docs/prozess-katalog.md} §3.5.
 *
 * @param angelegt fehlende Zeilen, die der Lauf neu angelegt hat
 * @param aufgefrischt Zeilen mit {@link Pflegestatus#OFFEN}, die er neu berechnet hat — <b>auch
 *     solche, die schon einen Vorschlag trugen.</b> Sonst friert der erste Lauf jeden spaeteren
 *     Regelfehler ein (E13)
 * @param unberuehrt Zeilen mit {@link Pflegestatus#GEPFLEGT}. Sie ruehrt der Lauf <b>nie</b> an
 * @param regelA geschriebene Zeilen mit Partner aus Regel A
 * @param regelB geschriebene Zeilen mit Partner aus Regel B
 * @param keine geschriebene Zeilen ohne Partnervorschlag. <b>„Geprueft, nichts abgeleitet"</b> —
 *     nach Regel Q4 ein Ergebnis und kein fehlender Wert
 */
public record VorschlagslaufResponse(
    int angelegt, int aufgefrischt, int unberuehrt, int regelA, int regelB, int keine) {}
