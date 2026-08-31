package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Was ein Zeitraumpaar fuer diesen Mandanten hergibt — die beiden Zahlen, an denen die Wahl des
 * Standardfensters haengt (D.3).
 *
 * @param belegteEimer wie viele Eimer des Paares ueberhaupt eine Zeile tragen. Ein Mandant ohne
 *     eine einzige Rollupzeile im Fenster hat null
 * @param groessterEimer die Nachrichtenzahl des groessten Eimers. Sie ist die zweite Bedingung: Ein
 *     Diagramm, in dem jeder Balken aus drei Nachrichten besteht, hat Punkte und zeigt nichts
 */
record Belegung(int belegteEimer, long groessterEimer) {}
