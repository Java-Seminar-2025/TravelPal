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

INSERT INTO plans (id, owner_id, title, city, city_latitude, city_longitude, days, visibility, ai_generated)
SELECT gen_random_uuid(), u.id, p.title, p.city, p.lat, p.lon, p.days, p.visibility, p.ai_generated
FROM (VALUES
    ('bkalinic', 'Weekend Getaway in Paris', 'Paris',    48.853495,   2.348391, 3, 'PUBLIC',  FALSE),
    ('renco13',  'Epic Tokyo Adventure',     'Tokyo',    35.676860, 139.763895, 7, 'PUBLIC',  TRUE),
    ('sarah',    'Hidden Gems of Rome',      'Rome',     41.893320,  12.482932, 4, 'PRIVATE', FALSE),
    ('bkalinic', 'Florence in 3 Days',       'Florence', 43.769795,  11.255640, 3, 'PUBLIC',  FALSE),
    ('renco13',  'Split Old Town & Marjan',  'Split',    43.511638,  16.439966, 2, 'PUBLIC',  FALSE)
) AS p(owner, title, city, lat, lon, days, visibility, ai_generated)
JOIN users u ON u.username = p.owner
WHERE NOT EXISTS (SELECT 1 FROM plans x WHERE x.title = p.title AND x.owner_id = u.id);

-- Demo plans created before plans had city coordinates (V3): fill them in.
UPDATE plans SET city_latitude = 48.853495, city_longitude =   2.348391 WHERE title = 'Weekend Getaway in Paris' AND city_latitude IS NULL;
UPDATE plans SET city_latitude = 35.676860, city_longitude = 139.763895 WHERE title = 'Epic Tokyo Adventure'     AND city_latitude IS NULL;
UPDATE plans SET city_latitude = 41.893320, city_longitude =  12.482932 WHERE title = 'Hidden Gems of Rome'      AND city_latitude IS NULL;
UPDATE plans SET city_latitude = 43.769795, city_longitude =  11.255640 WHERE title = 'Florence in 3 Days'       AND city_latitude IS NULL;
UPDATE plans SET city_latitude = 43.511638, city_longitude =  16.439966 WHERE title = 'Split Old Town & Marjan'  AND city_latitude IS NULL;

-- Stops for the Florence and Split demo plans (coordinates and ids from OpenStreetMap).
INSERT INTO plan_items (id, plan_id, name, latitude, longitude, osm_id, day_number, order_index, note)
SELECT gen_random_uuid(), p.id, i.name, i.lat, i.lon, i.osm_id, i.day_number, i.order_index, i.note
FROM (VALUES
    ('Florence in 3 Days', 'Cattedrale di Santa Maria del Fiore', 43.773102, 11.256574, 'w43768260',  1, 1, 'Climb Brunelleschi''s dome early in the morning'),
    ('Florence in 3 Days', 'Palazzo Vecchio',                     43.769144, 11.256139, 'r1461712',   1, 2, NULL),
    ('Florence in 3 Days', 'Galleria degli Uffizi',               43.768313, 11.255801, 'w477279033', 1, 3, 'Book tickets online'),
    ('Florence in 3 Days', 'Ponte Vecchio',                       43.768031, 11.253161, 'r5342094',   2, 1, NULL),
    ('Florence in 3 Days', 'Palazzo Pitti',                       43.765281, 11.250257, 'r1637338',   2, 2, NULL),
    ('Florence in 3 Days', 'Giardino di Boboli',                  43.763278, 11.249899, 'w24267141',  2, 3, NULL),
    ('Florence in 3 Days', 'Basilica di Santa Croce',             43.768365, 11.263020, 'w777563049', 3, 1, NULL),
    ('Florence in 3 Days', 'Piazzale Michelangelo',               43.762828, 11.264993, 'w233265215', 3, 2, 'Sunset view over the city'),
    ('Split Old Town & Marjan', 'Diocletian''s Palace',           43.508505, 16.440249, 'w124205148', 1, 1, NULL),
    ('Split Old Town & Marjan', 'Peristyle',                      43.508239, 16.440169, 'w29704912',  1, 2, NULL),
    ('Split Old Town & Marjan', 'Cathedral of Saint Domnius',     43.508047, 16.440428, 'w272588434', 1, 3, 'Climb the bell tower'),
    ('Split Old Town & Marjan', 'Statue of Grgur Ninski',         43.509432, 16.440794, 'n4221604987',1, 4, 'Rub his toe for luck'),
    ('Split Old Town & Marjan', 'Riva Promenade',                 43.507797, 16.438172, 'r10034116',  2, 1, NULL),
    ('Split Old Town & Marjan', 'Prva vidilica (Marjan viewpoint)', 43.507706, 16.430328, 'n850150112', 2, 2, 'Walk up Marjan hill')
) AS i(plan_title, name, lat, lon, osm_id, day_number, order_index, note)
JOIN plans p ON p.title = i.plan_title
WHERE NOT EXISTS (SELECT 1 FROM plan_items x WHERE x.plan_id = p.id AND x.name = i.name);
