package de.kraftwerkone.overlord.monitor.common;

/**
 * Fachliche Einordnung eines {@code MessageStatus}. Die Zuordnung von Rohwert zu Einordnung
 * entsteht ausschliesslich im {@link MessageStatusClassifier} und wird nirgends nachgebaut.
 *
 * <p>{@code UNGEKLAERT} ist der Auffang: unbekannte oder fachlich nicht verifizierte Statuswerte
 * werden niemals in einen geratenen Wert gedraengt.
 */
public enum MessageStatusKind {
  FEHLER,
  WARTEND,
  LAEUFT,
  ZWISCHENSCHRITT,
  ABGESCHLOSSEN,
  QUITTIERT,
  UNGEKLAERT
}
