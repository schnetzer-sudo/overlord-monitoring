package de.kraftwerkone.overlord.monitor;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.AUDIT_LOG;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prueft, dass der {@link AuditLogWriter} in einer eigenen Transaktion schreibt: rollt die
 * umgebende Fachtransaktion zurueck, bleibt der Protokolleintrag bestehen ({@code REQUIRES_NEW}).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class AuditLogWriterDbIT {

  @Autowired private AuditLogWriter writer;
  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired
  @Qualifier("monitorDsl") private DSLContext monitorDsl;

  @Test
  @DisplayName("Der Protokolleintrag ueberlebt den Rollback der Fachtransaktion")
  void protokolleintrag_ueberlebt_rollback() {
    String markierung = "IT-" + UUID.randomUUID();
    try {
      TransactionTemplate aeussere = new TransactionTemplate(transactionManager);
      aeussere.executeWithoutResult(
          status -> {
            writer.schreibe(
                new AuditEvent(
                    AuditEventType.ANMELDUNG_FEHLVERSUCH,
                    null,
                    markierung,
                    null,
                    "TEST",
                    null,
                    "127.0.0.1",
                    "Integrationstest REQUIRES_NEW"));
            // Die Fachtransaktion scheitert — der Protokolleintrag muss trotzdem bleiben.
            status.setRollbackOnly();
          });

      int treffer = monitorDsl.fetchCount(AUDIT_LOG, AUDIT_LOG.ACTOR_USERNAME.eq(markierung));
      assertThat(treffer).isEqualTo(1);
    } finally {
      monitorDsl.deleteFrom(AUDIT_LOG).where(AUDIT_LOG.ACTOR_USERNAME.eq(markierung)).execute();
    }
  }
}
