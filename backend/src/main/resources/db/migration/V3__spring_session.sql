-- Overlord Monitoring — Ablage der serverseitigen Sitzungen (Schritt 3).
--
-- HERKUNFT: Diese Datei ist NICHT selbst entworfen. Sie ist die mitgelieferte
-- Datei org/springframework/session/jdbc/schema-mysql.sql aus der Distribution
-- spring-session-jdbc 4.1.0, uebernommen am 29.07.2026. Ein selbst geschriebenes
-- Sitzungsschema ist ein stiller Fehler: Spalten, Typen und Indizes muessen exakt
-- zu JdbcIndexedSessionRepository passen.
--
-- Angepasst wurden GENAU ZWEI Dinge, beide begruendet:
--
--  1. Zeichensatz und Sortierung stehen explizit an jeder Tabelle (nie geerbt),
--     wie in jeder Migration dieses Projekts.
--  2. SESSION_ID und PRIMARY_ID bekommen COLLATE utf8mb4_bin. Die
--     Standardsortierung utf8mb4_general_ci vergleicht ohne Ruecksicht auf Gross-
--     und Kleinschreibung — Sitzungs-IDs wuerden damit unscharf verglichen und
--     der Schluesselraum schrumpfte erheblich. Fuer Tokenartiges gilt binaer.
--     SPRING_SESSION_ATTRIBUTES.SESSION_PRIMARY_ID zeigt per Fremdschluessel auf
--     PRIMARY_ID und MUSS dieselbe Sortierung tragen, sonst legt MariaDB den
--     Fremdschluessel nicht an. Das ist Folge von (2), keine dritte Aenderung.
--
-- Spring Session legt dieses Schema NICHT selbst an: application.yml setzt
-- spring.session.jdbc.initialize-schema=never. Sonst arbeitete es an Flyway vorbei.
--
-- Der Fremdschluessel bleibt drin: er liegt innerhalb von overlord_monitor und
-- ueberschreitet keine Schemagrenze.

CREATE TABLE SPRING_SESSION (
  PRIMARY_ID            CHAR(36)     COLLATE utf8mb4_bin NOT NULL,
  SESSION_ID            CHAR(36)     COLLATE utf8mb4_bin NOT NULL,
  CREATION_TIME         BIGINT       NOT NULL,
  LAST_ACCESS_TIME      BIGINT       NOT NULL,
  MAX_INACTIVE_INTERVAL INT          NOT NULL,
  EXPIRY_TIME           BIGINT       NOT NULL,
  PRINCIPAL_NAME        VARCHAR(100),
  CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
  SESSION_PRIMARY_ID CHAR(36)     COLLATE utf8mb4_bin NOT NULL,
  ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
  ATTRIBUTE_BYTES    BLOB         NOT NULL,
  CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
  CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
    REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
