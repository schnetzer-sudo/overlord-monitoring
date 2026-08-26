package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.rollup.RollupEigenschaften;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Verdrahtung des Rollup-Jobs.
 *
 * <p>Sie besteht aus zwei Zeilen, und die stehen hier statt im Fachpaket, weil {@code config} das
 * Paket fuer Verdrahtung ist — genauso wie {@link RohdatenConfig} die {@code RohdatenEigenschaften}
 * anmeldet. Umgekehrt darf niemand aus {@code config} importieren; {@code rollup} kennt diese
 * Klasse deshalb nicht, sondern bekommt {@link RollupEigenschaften} als Bean gereicht.
 *
 * <p><b>{@code @EnableScheduling} steht ohne Profilbindung</b>, und das ist Absicht: Die
 * Zeitsteuerung selbst ist harmlos, solange es keine {@code @Scheduled}-Bean gibt — und die
 * entsteht ausschliesslich im Profil {@link RollupEigenschaften#PROFIL}. Eine Profilbindung
 * <i>hier</i> haette den unangenehmen Nebeneffekt, dass ein vergessenes Profil sich als „der Job
 * laeuft nicht" zeigt statt als „die Bean fehlt", und das ist der schwerer zu findende Fehler.
 *
 * <p><b>Warum kein eigenes Wurzelpaket.</b> {@code PROJEKTBESCHREIBUNG.md} §6: „Der Rollup-Job
 * bekommt trotz separater Startbarkeit kein eigenes Wurzelpaket. Die Trennung laeuft ueber das
 * Spring-Profil, nicht ueber den Namensraum."
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(RollupEigenschaften.class)
public class RollupConfig {}
