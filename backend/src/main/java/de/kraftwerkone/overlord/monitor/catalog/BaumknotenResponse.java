package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Ein Knoten des Prozessbaums — <b>eine Gruppe oder ein Prozess</b>, und beide Gliederungen liefern
 * dieselbe rekursive Form.
 *
 * <p><i>(seit 15.09.2026, loest {@code PartnerknotenResponse} und {@code RichtungsknotenResponse}
 * ab.)</i> Bis dahin hatte jede Ebene ihren eigenen Typ mit eigenem Feldnamen ({@code partner},
 * {@code richtungen}, {@code prozesse}). Mit der zweiten Gliederung waeren das zwei Formen fuer
 * dieselbe Sache gewesen — und zwei Renderpfade in der Oberflaeche, die auseinanderlaufen. Seither
 * ist die Schachtelung <b>{@code kinder}</b> an jeder Gruppe, und welche Ebene welche ist, steht in
 * {@code ProzessbaumResponse.ebenen}.
 *
 * <p><b>Keine Typangabe im JSON.</b> Eine Gruppe traegt {@code kinder}, ein Prozess traegt {@code
 * processId}; mehr braucht die Oberflaeche zum Unterscheiden nicht, und die Tiefe kennt sie aus
 * {@code ebenen}. Serialisiert wird der Laufzeittyp jedes Elements.
 */
public sealed interface BaumknotenResponse permits GruppenknotenResponse, ProzessknotenResponse {

  /**
   * Stabil und unter Geschwistern eindeutig. Bei einer Gruppe der <b>hochgestellte
   * Gruppenschluessel</b> (E-41) oder {@code null} fuer die eine Gruppe ohne Wert; bei einem
   * Prozess die {@code ProcessID}.
   */
  String schluessel();

  /**
   * Der Anzeigewert, <b>roh</b> und in der zuerst angetroffenen Schreibweise. Darf {@code null}
   * sein — was der Nutzer dann liest, gehoert in die Sprachdateien (Regel Q4).
   */
  String name();

  long nachrichten();

  long fehler();
}
