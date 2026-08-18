package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.payload.RohdatenEigenschaften;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Verdrahtung des Rohdatenzugriffs.
 *
 * <p>Sie besteht aus einer Zeile, und die steht hier statt im Fachpaket, weil {@code config} das
 * Paket fuer Verdrahtung ist — genauso wie {@link DataSourceConfig} die {@link DatabaseProperties}
 * anmeldet. Umgekehrt darf niemand aus {@code config} importieren; {@code payload} kennt diese
 * Klasse deshalb nicht, sondern bekommt {@link RohdatenEigenschaften} als Bean gereicht.
 *
 * <p>Die SOAP-Fabriken werden hier <b>nicht</b> als Beans gebaut: Sie sind Implementierungsdetail
 * des Zugriffs und wuerden {@code jakarta.xml.soap} in die Verdrahtung ziehen. Der Zugriff legt sie
 * sich einmal im Konstruktor an.
 */
@Configuration
@EnableConfigurationProperties(RohdatenEigenschaften.class)
public class RohdatenConfig {}
