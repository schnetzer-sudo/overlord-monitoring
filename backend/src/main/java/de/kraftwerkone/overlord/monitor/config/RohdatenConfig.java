package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.common.Ablagegrenzen;
import de.kraftwerkone.overlord.monitor.payload.RohdatenEigenschaften;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Verdrahtung des Zugriffs auf die Ablagen.
 *
 * <p>Sie besteht aus zwei Zeilen, und die stehen hier statt im Fachpaket, weil {@code config} das
 * Paket fuer Verdrahtung ist — genauso wie {@link DataSourceConfig} die {@link DatabaseProperties}
 * anmeldet. Umgekehrt darf niemand aus {@code config} importieren; weder {@code payload} noch
 * {@code common} kennen diese Klasse, sie bekommen ihre Datensaetze als Bean gereicht.
 *
 * <p><b>Zwei Datensaetze auf demselben Zweig {@code overlord.rohdaten.*}, und das ist Absicht.</b>
 * {@link Ablagegrenzen} traegt die drei Werte, die der <b>Transport</b> braucht — er liegt seit
 * Schritt 10d in {@code common}, weil das Dashboard ihn ebenfalls ruft, und {@code common} darf
 * {@code payload} nicht kennen. {@link RohdatenEigenschaften} traegt die vier Grenzen des
 * <b>Features</b> Rohdaten. <b>Kein Schluessel ist umbenannt worden</b>; die Ueberschneidung ist
 * {@code maximalgroesse-bytes}, ein Wert mit einer Vorgabe an einer Stelle ({@link
 * Ablagegrenzen#VORGABE_MAXIMALGROESSE_BYTES}) und zwei Durchsetzungspunkten.
 *
 * <p>Die SOAP-Fabriken werden hier <b>nicht</b> als Beans gebaut: Sie sind Implementierungsdetail
 * des Zugriffs und wuerden {@code jakarta.xml.soap} in die Verdrahtung ziehen. Der Zugriff legt sie
 * sich einmal im Konstruktor an.
 */
@Configuration
@EnableConfigurationProperties({Ablagegrenzen.class, RohdatenEigenschaften.class})
public class RohdatenConfig {}
