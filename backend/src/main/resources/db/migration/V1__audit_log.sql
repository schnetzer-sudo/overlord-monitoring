-- Overlord Monitoring — erste Migration fuer overlord_monitor.
--
-- Flyway verwaltet AUSSCHLIESSLICH dieses Schema (Regel S2). Zeichensatz und
-- Sortierung stehen in JEDER Migration explizit (nie geerbt), sonst bricht ein
-- schemauebergreifender Join, sobald der Server-Default sich aendert oder die
-- Instanz auf MariaDB 11 gehoben wird. Begruendungen: docs/datenzugriff.md.
--
-- Keine Fremdschluessel ueber Schemagrenzen — in keiner Migration.

CREATE TABLE audit_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  occurred_at    DATETIME(3)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  actor_user_id  BIGINT       NULL,
  actor_username VARCHAR(190) NULL,
  mandant_id     VARCHAR(36)  NULL,
  target_type    VARCHAR(64)  NULL,
  target_id      VARCHAR(190) NULL,
  ip             VARCHAR(45)  NULL,
  detail         LONGTEXT     NULL,
  PRIMARY KEY (id),
  KEY idx_audit_occurred (occurred_at),
  KEY idx_audit_actor    (actor_username, occurred_at),
  KEY idx_audit_type     (event_type, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
