package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.message.NachrichtenlisteEigenschaften;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Meldet {@link NachrichtenlisteEigenschaften} an.
 *
 * <p>Im Paket {@code config}, weil dort die Verdrahtung sitzt und kein Fachpaket aus {@code config}
 * importiert — dieselbe Aufteilung wie bei {@code RollupConfig} und {@code RohdatenConfig}.
 */
@Configuration
@EnableConfigurationProperties(NachrichtenlisteEigenschaften.class)
public class NachrichtenlisteConfig {}
