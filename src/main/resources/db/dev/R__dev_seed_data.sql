-- Development-only test data (loaded from db/dev, see application.properties).
-- Repeatable migration: re-runs whenever this file changes, so every insert skips rows that already exist.
-- All users log in with the password: password
-- Quick test accounts: admin / password (ADMIN) and user / password (USER)

INSERT INTO users (id, username, email, password_hash, role, xp) VALUES
    (gen_random_uuid(), 'bkalinic', 'bartul@travelpal.com', '{bcrypt}$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 1000),
    (gen_random_uuid(), 'renco13',  'renzo@travelpal.com',  '{bcrypt}$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 1000),
    (gen_random_uuid(), 'sarah',    'sarah@travelpal.com',  '{bcrypt}$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER',  320),
    (gen_random_uuid(), 'admin',    'admin@travelpal.com',  '{bcrypt}$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 0),
    (gen_random_uuid(), 'user',     'user@travelpal.com',   '{bcrypt}$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER',  0)
ON CONFLICT DO NOTHING;

INSERT INTO plans (id, owner_id, title, city, days, visibility, ai_generated)
SELECT gen_random_uuid(), u.id, p.title, p.city, p.days, p.visibility, p.ai_generated
FROM (VALUES
    ('bkalinic', 'Weekend Getaway in Paris', 'Paris', 3, 'PUBLIC',  FALSE),
    ('renco13',  'Epic Tokyo Adventure',     'Tokyo', 7, 'PUBLIC',  TRUE),
    ('sarah',    'Hidden Gems of Rome',      'Rome',  4, 'PRIVATE', FALSE)
) AS p(owner, title, city, days, visibility, ai_generated)
JOIN users u ON u.username = p.owner
WHERE NOT EXISTS (SELECT 1 FROM plans x WHERE x.title = p.title AND x.owner_id = u.id);
