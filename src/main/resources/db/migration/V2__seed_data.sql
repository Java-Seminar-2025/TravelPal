-- Insert default test users (Passwords are BCrypt hashes for 'password123')
INSERT INTO users (username, email, password_hash, role, xp, created_at) VALUES
('bkalinic', 'bartul@travelpal.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 1000, CURRENT_TIMESTAMP),
('renco13', 'renzo@travelpal.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 1000, CURRENT_TIMESTAMP),
('sarah', 'sarah@travelpal.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', 320, CURRENT_TIMESTAMP);

-- Insert default starter travel plans
INSERT INTO plans (owner_id, title, city, days, visibility, ai_generated, created_at) VALUES
((SELECT id FROM users WHERE username = 'bkalinic'), 'Weekend Getaway in Paris', 'Paris', 3, 'PUBLIC', FALSE, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE username = 'renco13'), 'Epic Tokyo Adventure', 'Tokyo', 7, 'PUBLIC', TRUE, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE username = 'sarah'), 'Hidden Gems of Rome', 'Rome', 4, 'PRIVATE', FALSE, CURRENT_TIMESTAMP);