CREATE TABLE users (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       username VARCHAR(50) NOT NULL UNIQUE,
       email VARCHAR(255) NOT NULL UNIQUE,
       password_hash VARCHAR(255) NOT NULL,
       role VARCHAR(20) NOT NULL DEFAULT 'USER',
       xp INT NOT NULL DEFAULT 0,
       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plans (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       owner_id BIGINT NOT NULL,
       copied_from_id BIGINT NULL,
       title VARCHAR(150) NOT NULL,
       city VARCHAR(100) NOT NULL,
       days INT NOT NULL,
       visibility VARCHAR(10) NOT NULL DEFAULT 'PRIVATE',
       ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
       FOREIGN KEY (copied_from_id) REFERENCES plans(id) ON DELETE SET NULL
);

CREATE TABLE plan_items (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        plan_id BIGINT NOT NULL,
        tripadvisor_location_id VARCHAR(30) NOT NULL,
        day_number INT NOT NULL,
        order_index INT NOT NULL,
        note VARCHAR(500),
        FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE TABLE adventures (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL,
        plan_id BIGINT NOT NULL,
        started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE TABLE visits (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        adventure_id BIGINT NOT NULL,
        plan_item_id BIGINT NOT NULL,
        visited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE (adventure_id, plan_item_id),
        FOREIGN KEY (adventure_id) REFERENCES adventures(id) ON DELETE CASCADE,
        FOREIGN KEY (plan_item_id) REFERENCES plan_items(id) ON DELETE CASCADE
);

CREATE TABLE ratings (
         id BIGINT AUTO_INCREMENT PRIMARY KEY,
         user_id BIGINT NOT NULL,
         plan_id BIGINT NOT NULL,
         stars TINYINT NOT NULL CHECK (stars BETWEEN 1 AND 5),
         comment VARCHAR(500),
         UNIQUE (user_id, plan_id),
         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
         FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE TABLE user_badges (
         user_id BIGINT NOT NULL,
         badge_code VARCHAR(40) NOT NULL,
         awarded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
         PRIMARY KEY (user_id, badge_code),
         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);