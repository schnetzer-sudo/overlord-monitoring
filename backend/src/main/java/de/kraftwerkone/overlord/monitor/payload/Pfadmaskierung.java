package de.kraftwerkone.overlord.monitor.payload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ersetzt zwei Serverpfad-Praefixe durch {@code /IS/} — wie das Altsystem ({@code
 * JsonServlet.java:824}–{@code :828}, Q3).
 *
 * <p><b>Sie greift ausschliesslich im beschnittenen Zweig.</b> Im Altsystem stehen beide
 * Ersetzungen innerhalb der inneren Schleife; der {@code else}-Zweig maskiert nicht. {@code ADMIN}
 * bekommt das Protokoll vollstaendig und unmaskiert, und dabei bleibt es.
 *
 * <h2>Ein Muster statt eines Literals</h2>
 *
 * <p>Das Altsystem ersetzt {@code /opt/txp/users/<dienstkonto>/}, mit dem Kontonamen als fester
 * Zeichenfolge im Quelltext. <b>Dieser Name steht hier nicht.</b> Er ist ein produktiver Bezeichner
 * (Regel G1), und der Auftraggeber hat ihn aus demselben Grund schon aus {@code
 * docs/messungen-schritt8.md} entfernt — ihn hier einzutragen machte diese Entscheidung
 * wirkungslos, weil er ueber diese Datei doch in die Historie geriete.
 *
 * <p>Stattdessen steht ein Muster: <b>ein</b> Pfadabschnitt unter {@code /opt/txp/users/}. Das ist
 * kein Kompromiss, sondern eine Verbesserung — es maskiert auch ein zweites Dienstkonto, das das
 * Altsystem ungeschuetzt durchliesse, und es kann nicht veralten, wenn das Konto umbenannt wird.
 *
 * <h2>Was diese Klasse ausdruecklich nicht ist</h2>
 *
 * <p><b>Keine Vertraulichkeitszusage.</b> Sie ersetzt zwei bekannte Praefixe und sonst nichts;
 * Pfade in anderer Form bleiben stehen. Der Beschnitt insgesamt ist eine Lesbarkeitsregel — 808
 * Pfadzeilen und 331 Dienstkennungen liegen ohnehin <b>innerhalb</b> der Marken (M65). Wer darauf
 * eine Zusage gruendet, gruendet sie auf nichts.
 */
public final class Pfadmaskierung {

  private Pfadmaskierung() {}

  /** Wodurch beide Praefixe ersetzt werden. */
  private static final String ERSATZ = "/IS/";

  /**
   * Das Dienstkonto-Praefix als Muster. Ein Abschnitt, keine Schraegstriche darin — damit trifft es
   * genau die eine Ebene, die das Altsystem mit einem Literal trifft.
   */
  private static final Pattern DIENSTKONTO = Pattern.compile("/opt/txp/users/[^/]+/");

  /** Das zweite Praefix. Es traegt keinen Kontonamen und steht deshalb im Klartext. */
  private static final String LOBSTER = "/srv/lobster/IS/";

  /**
   * Maskiert eine Zeile.
   *
   * <p><b>Beide Ersetzungen laufen, nicht nur die erste.</b> Das Altsystem verwendet {@code else
   * if} und maskiert je Zeile hoechstens ein Praefix; eine Zeile mit beiden bliebe dort halb
   * unmaskiert. Der Unterschied ist im gemessenen Bestand nicht beobachtet, kostet nichts und
   * beseitigt einen Fall, in dem das Ergebnis von der Reihenfolge im Quelltext abhinge.
   */
  public static String maskiere(String zeile) {
    if (zeile == null || zeile.isEmpty()) {
      return zeile;
    }
    String maskiert = DIENSTKONTO.matcher(zeile).replaceAll(Matcher.quoteReplacement(ERSATZ));
    return maskiert.replace(LOBSTER, ERSATZ);
  }
}
