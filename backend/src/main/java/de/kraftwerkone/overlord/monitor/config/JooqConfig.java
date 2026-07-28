package de.kraftwerkone.overlord.monitor.config;

import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * Die beiden {@link DSLContext}-Beans, Dialekt {@link SQLDialect#MARIADB}.
 *
 * <p>Regel: Der <b>Lese-Kontext darf beide Schemata lesen</b>, der <b>Schreib-Kontext darf
 * ausschliesslich {@code overlord_monitor}</b> und ausschliesslich dort schreiben.
 *
 * <p>Bewusst <b>kein {@code defaultSchema}</b>: Schemanamen werden immer voll qualifiziert
 * gerendert, sonst funktionieren schemauebergreifende Abfragen nicht. Bezeichner bleiben gequotet
 * (jOOQ-Standard), weil die Tabellennamen im Quellschema gemischt geschrieben und auf einem
 * Linux-Server case-sensitiv sind.
 */
@Configuration
public class JooqConfig {

  /**
   * Lese-Kontext. Traegt den {@link ReadOnlyExecuteListener} (dritte Schicht des Schreibschutzes).
   * Ohne Spring-Transaktionsbindung: Lesezugriffe holen je Statement eine Verbindung aus dem
   * Lese-Pool.
   */
  @Bean
  public DSLContext glassfishDsl(@Qualifier("glassfishDataSource") DataSource glassfishDataSource) {
    DefaultConfiguration cfg = new DefaultConfiguration();
    cfg.set(SQLDialect.MARIADB);
    cfg.set(new DataSourceConnectionProvider(glassfishDataSource));
    cfg.set(new DefaultExecuteListenerProvider(new ReadOnlyExecuteListener()));
    return new DefaultDSLContext(cfg);
  }

  /**
   * Schreib-Kontext. Ueber {@link TransactionAwareDataSourceProxy} an die Spring-Transaktionen des
   * einen Transaktionsmanagers gebunden, damit {@code @Transactional} (auch {@code REQUIRES_NEW})
   * greift.
   */
  @Bean
  public DSLContext monitorDsl(@Qualifier("monitorDataSource") DataSource monitorDataSource) {
    DefaultConfiguration cfg = new DefaultConfiguration();
    cfg.set(SQLDialect.MARIADB);
    cfg.set(
        new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(monitorDataSource)));
    return new DefaultDSLContext(cfg);
  }
}
