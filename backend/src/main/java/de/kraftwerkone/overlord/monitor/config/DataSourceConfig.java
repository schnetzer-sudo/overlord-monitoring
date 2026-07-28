package de.kraftwerkone.overlord.monitor.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Die beiden DataSources und der einzige Transaktionsmanager.
 *
 * <p><b>Keine der beiden DataSources ist {@link Primary}.</b> Ein vergessener {@link Qualifier}
 * soll beim Start eine {@code NoUniqueBeanDefinitionException} ausloesen und nicht still den
 * falschen Pool verdrahten — ein Startfehler ist besser als ein Laufzeitfehler in einem selten
 * begangenen Codepfad.
 *
 * <p>Die Schreibgarantie fuer {@code GlassfishDB} haengt an den <b>DB-Rechten</b>, nicht an dieser
 * Trennung. Der Lese-Pool ist zusaetzlich {@code readOnly} markiert (Zusatz, kein Nachweis), und
 * ein jOOQ-{@code ExecuteListener} weist auf dem Lese-Kontext alles ausser Lesen ab. Siehe {@code
 * docs/datenzugriff.md}.
 */
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DataSourceConfig {

  /**
   * Lese-Pool {@code glassfish-read} auf den Lesebenutzer. {@code readOnly}, klein, mit harter
   * Laufzeitgrenze je Statement. Darf beide Schemata lesen (fuer schemauebergreifende Joins ueber
   * eine einzige Verbindung).
   *
   * <p>{@code connectionInitSql} nutzt die kurze Form: {@code transaction_read_only} ist auf
   * MariaDB 10.6 keine bekannte Systemvariable (Fehler 1193) und wurde bewusst weggelassen. Siehe
   * {@code docs/datenzugriff.md}.
   */
  @Bean
  public DataSource glassfishDataSource(DatabaseProperties props) {
    HikariConfig cfg = new HikariConfig();
    cfg.setPoolName("glassfish-read");
    cfg.setJdbcUrl(props.jdbcUrl(props.glassfishSchema()));
    cfg.setUsername(props.readUser());
    cfg.setPassword(props.readPassword());
    cfg.setReadOnly(true);
    cfg.setMaximumPoolSize(5);
    cfg.setConnectionInitSql("SET SESSION max_statement_time=10");
    cfg.setConnectionTimeout(Duration.ofSeconds(5).toMillis());
    cfg.setMaxLifetime(Duration.ofMinutes(25).toMillis());
    return new HikariDataSource(cfg);
  }

  /**
   * Schreib-Pool {@code monitor-write} auf den Schreibbenutzer. Schreibt ausschliesslich in {@code
   * overlord_monitor}. Von Flyway ueber {@link FlywayDataSource} genutzt.
   */
  @Bean
  @FlywayDataSource
  public DataSource monitorDataSource(DatabaseProperties props) {
    HikariConfig cfg = new HikariConfig();
    cfg.setPoolName("monitor-write");
    cfg.setJdbcUrl(props.jdbcUrl(props.monitorSchema()));
    cfg.setUsername(props.writeUser());
    cfg.setPassword(props.writePassword());
    cfg.setMaximumPoolSize(5);
    cfg.setConnectionInitSql("SET SESSION max_statement_time=30");
    return new HikariDataSource(cfg);
  }

  /**
   * Der <b>einzige</b> Transaktionsmanager, gebunden an {@code monitorDataSource} und {@link
   * Primary}. Damit bedeutet {@code @Transactional} im gesamten Projekt eindeutig „schreibt ins
   * eigene Schema". Fuer den Lese-Pool wird bewusst kein Transaktionsmanager angelegt.
   */
  @Bean
  @Primary
  public PlatformTransactionManager transactionManager(
      @Qualifier("monitorDataSource") DataSource monitorDataSource) {
    return new JdbcTransactionManager(monitorDataSource);
  }
}
