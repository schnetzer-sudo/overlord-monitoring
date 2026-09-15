-- Overlord Monitoring — die Vorgabe der Baumgliederung je Konto (Prozessbaum, zweite Gliederung).
--
-- WOZU. Der Prozessbaum kennt zwei Gliederungen: PARTNER (Partner -> Richtung ->
-- Prozess, am kuratierten Katalog) und PROJEKT (Projektbeschreibung -> Prozess,
-- ohne Kuratierung). Welche ein Nutzer beim ersten Aufruf sieht, setzt ein ADMIN
-- je Konto; umschalten darf jeder. Fehlt ?gliederung= am Baum-Endpunkt, setzt der
-- Server den Wert dieser Spalte ein und nennt ihn in der Antwort
-- (docs/process-view.md).
--
-- KEINE BERECHTIGUNG. Die Spalte sagt, womit der Baum beginnt, und nicht, was ein
-- Konto sehen darf. Sie steht deshalb nicht in der Selbstauskunft, und ihr Aendern
-- verwirft keine Sitzung (docs/benutzerverwaltung.md).
--
-- VARCHAR UND KEIN ENUM — dieselbe Bauform wie process_catalog.richtung und
-- process_catalog.pflegestatus (V6): Ein weiterer Wert soll keine Migration kosten,
-- die Whitelist steht im Code (common/Baumgliederung).
--
-- ZEICHENSATZ UND SORTIERUNG STEHEN AN DER SPALTE. V2 setzt sie an der Tabelle, und
-- ein ADD COLUMN erbte sie von dort. Geerbt ist genau das, was
-- PROJEKTBESCHREIBUNG.md §5 ausschliesst: Aendert sich die Vorgabe der Tabelle oder
-- der Instanz, aendert sich die Spalte mit, ohne dass es in einer Migration steht.
-- utf8mb4_general_ci wie in V6 — hier steht nichts Tokenartiges.
--
-- NOT NULL DEFAULT 'PARTNER'. Bestandskonten bekommen PARTNER, und das ist ihr
-- wahrer Zustand: Bis heute gab es nur diese Gliederung. Neue Konten ueber
-- POST /api/admin/users ebenso — das Anlegen bleibt unangetastet.
--
-- ENGLISCHER SPALTENNAME wie die Nachbarn in app_user (must_change_password,
-- locked_by_admin) und wie der Pflegeendpunkt PUT /api/admin/users/{id}/tree-layout:
-- Unter /api/admin/users stehen englische Unterpfade (docs/benutzerverwaltung.md §5).
-- Die Werte bleiben deutsch, wie role = 'MANDANT'.
--
-- KEINE DOWN-MIGRATION, wie ueberall in diesem Projekt.

ALTER TABLE app_user
  ADD COLUMN tree_layout VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
    NOT NULL DEFAULT 'PARTNER' AFTER locked_by_admin;
