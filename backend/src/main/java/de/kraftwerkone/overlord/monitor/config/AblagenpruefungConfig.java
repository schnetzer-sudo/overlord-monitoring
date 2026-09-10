package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.dashboard.AblagenpruefungEigenschaften;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Verdrahtung der Ablagenpruefung (Schritt 10d, Teil A).
 *
 * <p>Sie besteht aus einer Zeile, und die steht hier statt im Fachpaket, weil {@code config} das
 * Paket fuer Verdrahtung ist — genauso wie {@link RollupConfig} die {@code RollupEigenschaften} und
 * {@link RohdatenConfig} die {@code Ablagegrenzen} anmeldet. Umgekehrt darf niemand aus {@code
 * config} importieren; {@code dashboard} kennt diese Klasse nicht, sondern bekommt den Datensatz
 * als Bean gereicht.
 *
 * <p><b>{@code @EnableScheduling} steht nicht hier</b>, sondern unverändert einmal in {@link
 * RollupConfig} — es gilt fuer die ganze Anwendung, und eine zweite Angabe waere eine zweite
 * Stelle, an der jemand sie vermuten muesste. Ob ein zeitgesteuerter Lauf entsteht, haengt ohnehin
 * an der Bean und nicht an dieser Annotation.
 */
@Configuration
@EnableConfigurationProperties(AblagenpruefungEigenschaften.class)
public class AblagenpruefungConfig {}
