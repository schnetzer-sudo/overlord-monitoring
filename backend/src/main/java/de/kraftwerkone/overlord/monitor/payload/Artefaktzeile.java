package de.kraftwerkone.overlord.monitor.payload;

/**
 * Eine Zeile aus {@code MessageProperty}, so wie das Repository sie liefert.
 *
 * <p><b>{@link #verweis()} ist der Rohwert und bleibt im Backend.</b> Er geht nie in eine Antwort,
 * nie in eine Kennung und nie in eine Protokollzeile.
 *
 * @param name {@code MessagePropertyName} — eines der beiden gemessenen Muster (M54)
 * @param schritt {@code MessageActionID}. {@code 0} ist der Metadaten-Schritt
 * @param verweis {@code MessagePropertyValue} in der Form {@code <Ablagenkennung>|<UUID>}
 */
public record Artefaktzeile(String name, short schritt, String verweis) {

  /** Die Kennung dieses Artefakts, wie sie im Pfad steht. */
  public ArtefaktId id() {
    return new ArtefaktId(schritt, name);
  }

  /** Bewusst ueberschrieben: Der Verweis darf auch versehentlich nicht ins Protokoll. */
  @Override
  public String toString() {
    return "Artefaktzeile[" + name + ", Schritt " + schritt + ", Verweis verdeckt]";
  }
}
