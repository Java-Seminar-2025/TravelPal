-- Insert default test users (Passwords are BCrypt hashes for 'password123')
INSERT INTO users (id, username, email, password_hash, role, xp, created_at) VALUES
(1, 'bkalinic', 'bartul@travelpal.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 1000, CURRENT_TIMESTAMP),
(2, 'renco13', 'renzo@travelpal.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 1000, CURRENT_TIMESTAMP),
(3, 'sarah', 'sarah@travelpal.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 320, CURRENT_TIMESTAMP);

-- Insert default starter travel plans
INSERT INTO plans (id, owner_id, title, city, days, visibility, ai_generated, created_at) VALUES
(1, 1, 'Weekend Getaway in Paris', 'Paris', 3, 'PUBLIC', FALSE, CURRENT_TIMESTAMP),
(2, 2, 'Epic Tokyo Adventure', 'Tokyo', 7, 'PUBLIC', TRUE, CURRENT_TIMESTAMP),
(3, 1, 'Hidden Gems of Rome', 'Rome', 4, 'PRIVATE', FALSE, CURRENT_TIMESTAMP);