-- Overlord Monitoring — Benutzer und ihre Mandantenzuordnung (Schritt 3).
--
-- Zeichensatz und Sortierung stehen explizit in JEDER Tabelle (nie geerbt), sonst
-- bricht ein schemauebergreifender Join, sobald der Server-Default sich aendert
-- oder die Instanz auf MariaDB 11 gehoben wird. Begruendungen: docs/datenzugriff.md.
--
-- Keine Fremdschluessel ueber Schemagrenzen: app_user_mandant.mandant_id zeigt
-- fachlich auf GlassfishDB.Mandant.MandantID, traegt aber bewusst KEINEN
-- Fremdschluessel dorthin.

CREATE TABLE app_user (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  -- Die Standardsortierung utf8mb4_general_ci vergleicht ohne Ruecksicht auf
  -- Gross- und Kleinschreibung. Fuer den Benutzernamen ist das erwuenscht: der
  -- eindeutige Index verhindert damit von selbst, dass "Lukas" und "lukas" zwei
  -- Konten werden.
  username             VARCHAR(190) NOT NULL,
  -- NULLABLE: ein Konto darf ohne Passwort existieren (es kann sich dann nie
  -- anmelden, durchlaeuft aber denselben Dummy-Vergleich). BCrypt ist von Gross-
  -- und Kleinschreibung abhaengig, deshalb utf8mb4_bin — sonst gaelte "$2A$..."
  -- als gleich zu "$2a$...".
  password_hash        VARCHAR(60)  COLLATE utf8mb4_bin NULL,
  -- MANDANT oder ADMIN. Whitelist im Code (security/Rolle), kein ENUM: ein
  -- neuer Wert soll keine Migration kosten — dieselbe Begruendung wie bei
  -- audit_log.event_type.
  role                 VARCHAR(16)  NOT NULL,
  enabled              BOOLEAN      NOT NULL DEFAULT FALSE,
  must_change_password BOOLEAN      NOT NULL DEFAULT TRUE,
  -- Fuer Schritt 8 vorbereitet: fachlich sind alle Mandantennutzer zum Download
  -- berechtigt, das Flag existiert von Anfang an, damit ein spaeterer Entzug
  -- keine Migration erfordert (Regel R6).
  download_allowed     BOOLEAN      NOT NULL DEFAULT TRUE,
  failed_attempts      INT          NOT NULL DEFAULT 0,
  -- Alle Zeitpunkte in UTC. DATETIME(3) statt TIMESTAMP wegen Zeitzonen und der
  -- 2038-Grenze. Sicherheitsrelevante Zeit rechnet mit der Systemuhr, nie mit
  -- der Anwendungsuhr aus Schritt 2.
  locked_until         DATETIME(3)  NULL,
  last_login_at        DATETIME(3)  NULL,
  created_at           DATETIME(3)  NOT NULL,
  updated_at           DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE app_user_mandant (
  user_id    BIGINT      NOT NULL,
  -- VARCHAR(36) passend zu GlassfishDB.Mandant.MandantID. Die MandantID ist ein
  -- lesbarer Code (NEXANS, VOTG, ...), keine UUID.
  mandant_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (user_id, mandant_id),
  CONSTRAINT fk_app_user_mandant_user
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
