package de.kraftwerkone.overlord.monitor.message;

/**
 * Ein <b>geplanter</b> Schritt aus der Ablaufdefinition — eine Zeile aus {@code SOSAction}.
 *
 * <p>Nicht zu verwechseln mit {@link MessageAktion}, dem <b>ausgefuehrten</b> Schritt. Die beiden
 * Kennungen laufen auseinander: {@code MessageAction.MessageActionID} ist eine laufende Nummer je
 * Nachricht und beginnt bei {@code 0}, {@code SOSAction.SOSActionID} ist der Schluessel in die
 * Ablaufdefinition, und die nummeriert nicht lueckenlos — 257 von 1.777 Ablaeufen haben eine
 * groesste Kennung ueber ihrer Schrittzahl, 233 nutzen Kennungen ab 99 (M20).
 *
 * @param sosId der Ablauf, zu dem der Schritt gehoert
 * @param sosActionId die Kennung des Schritts <b>innerhalb</b> dieses Ablaufs
 * @param name {@code SOSActionName}. In allen 3.944 Zeilen der Testkopie gepflegt, nie {@code
 *     NULL}, nie leer (M13) — trotzdem als nullbar behandelt, weil die Spalte es zulaesst
 * @param bausteine {@code SOSActionServiceProperties} des geplanten Schritts, pipe-getrennt. Aus
 *     seiner ersten Marke entsteht die Stufe {@link Namensherkunft#HERGELEITET}
 */
public record Ablaufschritt(String sosId, short sosActionId, String name, String bausteine) {}
