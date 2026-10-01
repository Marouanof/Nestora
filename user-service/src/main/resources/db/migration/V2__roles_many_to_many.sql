-- Migration manuelle : rôles many-to-many (roles / user_roles)
-- À exécuter sur la base user_db AVANT de démarrer le nouveau build.

-- 1. Table des rôles
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 2. Les 3 rôles existants
INSERT INTO roles (name)
VALUES ('ROLE_ADMIN'), ('ROLE_OWNER'), ('ROLE_TENANT')
ON CONFLICT (name) DO NOTHING;

-- 3. Table de jointure user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. Backfill depuis l'ancienne colonne users.role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = u.role
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 5. Suppression de l'ancienne colonne (NOT NULL -> sinon toute insertion échoue)
ALTER TABLE users DROP COLUMN IF EXISTS role;
