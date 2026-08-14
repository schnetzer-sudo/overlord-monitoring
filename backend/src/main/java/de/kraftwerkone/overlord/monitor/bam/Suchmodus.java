package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.util.Locale;
import org.springframework.http.HttpStatus;

/**
 * Wie die BAM-Suche den Wert vergleicht: <b>exakt</b> (die Vorgabe) oder über den <b>Anfang</b> des
 * Werts.
 *
 * <p><b>Er gilt für die ganze Suche und nicht je Begriff</b>, und das ist keine Bequemlichkeit. Ein
 * Kennzeichen <i>im</i> Begriff schiede aus: Die Parameterform {@code <typ>:<wert>} teilt am
 * <b>ersten</b> Doppelpunkt, und M49‑4 hat gezählt, dass <b>585 BAM-Werte</b> einen Doppelpunkt
 * tragen — ein zweites Trennzeichen wäre nach Regel Q4 geraten, solange nicht gemessen ist, ob
 * Werte es enthalten. Fachlich braucht es das auch nicht: Der Rückfall auf die Präfixsuche feuert,
 * wenn die <b>ganze</b> Suche leer ausgegangen ist.
 *
 * <h2>Warum {@link #PRAEFIX} nicht die Vorgabe ist</h2>
 *
 * <p><b>Der Grund ist nicht der Preis.</b> M49‑3 misst ihn als tragbar (1,374 ms im Normalfall).
 * Der Grund ist der Befund derselben Messung: <b>Ein vollständig eingetippter Wert findet als
 * Präfix 23 Nachrichten statt einer</b>, weil 22 längere Werte mit ihm beginnen. Als Voreinstellung
 * änderte die Präfixsuche damit die Antwort auch für den Nutzer, der nichts falsch macht. Geht die
 * exakte Suche dagegen <i>leer</i> aus, gibt es diese Kehrseite nicht: Dort ist die heutige Antwort
 * leer, und jeder Treffer ist rein zusätzlich.
 *
 * <p><b>Daraus folgt nicht, dass die Präfixsuche billig ist.</b> Sie ist die teuerste Zugriffsform
 * dieses Projekts: <b>M50</b> hat den schlimmsten bekannten Präfix des Bestands (1.332.180 Zeilen)
 * durch das gebaute Statement geschickt — <b>3,851 s</b> über 30 Tage, und über ein Jahr ist er an
 * der 60‑Sekunden-Grenze <b>abgebrochen</b>. Deshalb der Deckel in {@link
 * BamSuchfilter#PRAEFIX_FENSTER_MAXIMUM}.
 */
public enum Suchmodus {

  /** Der Wert wird als Ganzes verglichen — {@code MessageBAMValue IN (…)}. */
  EXAKT("exakt"),

  /**
   * Der Wert wird als <b>Anfang</b> verglichen — {@code MessageBAMValue LIKE ? ESCAPE '\'}.
   *
   * <p>Die Maskierung ist Pflicht und keine Vorsichtsmaßnahme: M49‑4 hat gezählt, dass {@code _} in
   * <b>2.696</b> Werten steht. Sie geschieht in {@link Suchbedingung#muster()}.
   */
  PRAEFIX("praefix");

  /**
   * <b>Fehlt der Parameter, verhält sich der Endpunkt Zeichen für Zeichen wie vor Teil 4.</b> Das
   * ist die Zusage, an der {@code BamSucheStatementsTest} und {@code BamSucheDbIT} hängen.
   */
  public static final Suchmodus VORGABE = EXAKT;

  private final String parameter;

  Suchmodus(String parameter) {
    this.parameter = parameter;
  }

  /**
   * Der Wert, unter dem dieser Modus am Endpunkt angefordert wird — <b>klein und deutsch</b>, wie
   * jeder andere Parameter dieses Projekts.
   */
  public String parameter() {
    return parameter;
  }

  /**
   * Liest den rohen Parameterwert.
   *
   * <p><b>Kein freier Text und keine stille Rückfallregel.</b> Ein unbekannter Wert ist {@code 400}
   * mit eigenem Problemtyp — wer {@code modus=prefix} schreibt, soll das erfahren und nicht
   * stillschweigend exakt suchen. <b>Ein fehlender oder leerer Parameter ist dagegen keine
   * Angabe</b> und damit die Vorgabe, genau wie {@code ?begriff=} kein Begriff ist.
   *
   * <p>Gelesen wird ohne Rücksicht auf Groß- und Kleinschreibung; die Antwort nennt anschließend
   * den tatsächlich verwendeten Modus.
   */
  public static Suchmodus ausParameter(String roh) {
    if (roh == null || roh.isBlank()) {
      return VORGABE;
    }
    String gesucht = roh.strip().toLowerCase(Locale.ROOT);
    for (Suchmodus modus : values()) {
      if (modus.parameter.equals(gesucht)) {
        return modus;
      }
    }
    throw new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "suchmodus-ungueltig",
        "Suchart unbekannt",
        "Die Suchart ist entweder " + EXAKT.parameter + " oder " + PRAEFIX.parameter + ".",
        "Unbekannter Wert fuer den Parameter modus");
  }
}
