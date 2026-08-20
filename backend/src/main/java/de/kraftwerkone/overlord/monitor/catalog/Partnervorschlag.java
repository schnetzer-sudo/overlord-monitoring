package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Was die Heuristik zu einem Prozess sagen kann — <b>ein Vorschlag, keine Wahrheit</b> ({@code
 * docs/prozess-katalog.md} §3).
 *
 * <p>Partner und Richtung duerfen {@code null} sein. Ein leeres Feld ist nach Regel Q4 ein
 * <b>gueltiges Ergebnis</b> und kein fehlender Wert: Wo eine Regel nicht sicher trifft, liefert sie
 * nichts statt eines wahrscheinlichen Werts.
 *
 * @param partner der Partnerkandidat, oder {@code null}
 * @param richtung die abgeleitete Richtung, oder {@code null}
 * @param herkunft woher der <b>Partner</b> stammt — niemals {@code null}, siehe {@link
 *     VorschlagHerkunft}
 */
public record Partnervorschlag(String partner, Richtung richtung, VorschlagHerkunft herkunft) {

  /** Nichts abgeleitet: kein Partner, keine Richtung. */
  public static final Partnervorschlag KEINER =
      new Partnervorschlag(null, null, VorschlagHerkunft.KEINE);

  public Partnervorschlag {
    if (herkunft == null) {
      throw new IllegalArgumentException("Partnervorschlag.herkunft darf nicht null sein");
    }
    if (partner != null && herkunft == VorschlagHerkunft.KEINE) {
      throw new IllegalArgumentException(
          "Ein Partner ohne Herkunft waere ein geratener Wert (Regel Q4): " + partner);
    }
  }
}
