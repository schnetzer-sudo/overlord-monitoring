package de.kraftwerkone.overlord.monitor.common;

/**
 * Wird geworfen, wenn auf dem Lese-{@code DSLContext} etwas anderes als ein Lesezugriff versucht
 * wird. Getragen wird die Schreibgarantie von den DB-Rechten; diese Ausnahme faengt den Fall
 * zusaetzlich frueh im Code. Siehe {@code config.ReadOnlyExecuteListener}.
 */
public class ReadOnlyViolationException extends RuntimeException {

  public ReadOnlyViolationException(String message) {
    super(message);
  }
}
