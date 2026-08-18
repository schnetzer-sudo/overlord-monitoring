package de.kraftwerkone.overlord.monitor.payload;

/**
 * Die beiden Arten von Artefakten, die eine Nachricht traegt. Abgeleitet aus dem {@code
 * MessagePropertyName} und aus sonst nichts.
 *
 * <p>Die Unterscheidung ist nicht kosmetisch: <b>Der Beschnitt greift ausschliesslich bei {@link
 * #PROTOKOLL}</b> (siehe {@link Protokollbeschnitt}), und die Oberflaeche setzt die beiden Teile
 * sichtbar voneinander ab.
 */
public enum Artefaktart {

  /**
   * Die Datei selbst — eingegangen oder umgewandelt. Namensmuster {@code <Dienst>.Payload.GUID}.
   */
  NUTZDATEN,

  /** Das Protokoll eines Schritts. Namensmuster {@code <Dienst>.Log.GUID}. */
  PROTOKOLL
}
