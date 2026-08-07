package de.kraftwerkone.overlord.monitor.message;

/**
 * Eine Zeile aus {@code MessageProperty}, roh — vor der Kappung.
 *
 * @param name {@code MessagePropertyName}. 101 verschiedene im Tagesfenster, 119 im Monatsfenster;
 *     die Dokumentation kannte zehn (M17 2)
 * @param wert {@code MessagePropertyValue}, <b>bereits in der Abfrage auf {@link
 *     NachrichtendetailRepository#WERT_GRENZE_BYTES} Zeichen begrenzt</b> und deshalb hoechstens
 *     das Vierfache davon an Bytes lang. Die genaue Kappung auf Bytes geschieht danach im Backend
 * @param messageActionId der Schritt, an dem die Eigenschaft haengt. Wird durchgereicht, aber
 *     nichts danach gefiltert — ob die Oberflaeche die Zuordnung nutzt, entscheidet Teil 2
 * @param laengeBytes die <b>ungekappte</b> Laenge des Werts in Bytes ({@code LENGTH()}). Nur so ist
 *     ablesbar, ob und wie viel gekappt wurde
 */
public record MessageEigenschaft(
    String name, String wert, short messageActionId, int laengeBytes) {}
