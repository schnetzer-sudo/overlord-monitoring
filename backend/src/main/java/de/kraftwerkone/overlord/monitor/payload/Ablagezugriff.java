package de.kraftwerkone.overlord.monitor.payload;

/**
 * Der Zugriff auf eine Ablage — die eine Stelle, an der dieses Werkzeug eine fremde Maschine
 * anspricht.
 *
 * <p><b>Sie ist eine Schnittstelle, damit die Tests den Filestore niemals ansprechen.</b> Kein Test
 * dieses Projekts baut eine Verbindung nach draussen; ersetzt wird genau dieser Typ. Was darunter
 * liegt — SOAP, SAAJ, ein ZIP-Anhang — ist Sache der Umsetzung und steht in keinem Test.
 *
 * <p><b>Die Adresse kommt als Parameter und wird nicht nachgeschlagen.</b> Sie stammt aus {@code
 * ArtefaktRepository.findeVerbindung}, also aus einem Statement, das die Mandantenpruefung traegt.
 * Diese Schnittstelle prueft nichts nach: Was hier ankommt, ist bereits freigegeben.
 */
public interface Ablagezugriff {

  /**
   * Holt eine Datei per SOAP-{@code RETRIEVE}.
   *
   * @param verbindung der {@code ServiceConnectString} der Ablage, <b>unveraendert und ohne
   *     Anhaengsel</b> (Q1). Er enthaelt einen Hostnamen und erscheint deshalb in keiner
   *     Fehlerantwort und in keiner Protokollzeile oberhalb von {@code DEBUG}
   * @param uuid die <b>nackte</b> Kennung hinter der Pipe — ohne Ablagenkennung, ohne Pipe (Q1,
   *     {@code JsonServlet.java:784}–{@code :786})
   * @return niemals {@code null}. Ein Fehlschlag ist ein benannter Zustand, keine Ausnahme
   */
  Abrufergebnis hole(String verbindung, String uuid);
}
