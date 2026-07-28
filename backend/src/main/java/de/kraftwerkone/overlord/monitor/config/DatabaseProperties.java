package de.kraftwerkone.overlord.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Verbindungsangaben fuer beide Schemata. Werte kommen ausschliesslich aus Umgebungsvariablen (in
 * {@code application.yml} auf {@code overlord.db.*} gemappt) — niemals aus einer versionierten
 * Datei (Regel G1). Es gibt bewusst getrennte Zugangsdaten fuer den Lese- und den Schreibbenutzer.
 *
 * <p>Die JDBC-URL traegt <b>keine Zeitzonenparameter</b>: Zeitstempel aus {@code GlassfishDB}
 * werden als Wanduhrzeit des Servers behandelt und nirgends konvertiert. Siehe {@code
 * docs/datenzugriff.md}.
 */
@ConfigurationProperties("overlord.db")
public record DatabaseProperties(
    String host,
    int port,
    String glassfishSchema,
    String monitorSchema,
    String readUser,
    String readPassword,
    String writeUser,
    String writePassword) {

  /** JDBC-URL fuer ein Schema auf dieser Instanz — ohne Zeitzonenparameter. */
  public String jdbcUrl(String schema) {
    return "jdbc:mariadb://" + host + ":" + port + "/" + schema;
  }
}
