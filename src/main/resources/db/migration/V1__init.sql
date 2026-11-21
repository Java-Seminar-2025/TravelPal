-- Primary keys are random UUIDs (not 1, 2, 3...) so IDs in URLs cannot be guessed or counted.
-- The app (Hibernate) generates them in Java, so this script works on PostgreSQL and on the H2 test database.

CREATE TABLE users (
       id UUID PRIMARY KEY,
       username VARCHAR(50) NOT NULL UNIQUE,
       email VARCHAR(255) NOT NULL UNIQUE,
       password_hash VARCHAR(255) NOT NULL,
       role VARCHAR(20) NOT NULL DEFAULT 'USER',
       xp INT NOT NULL DEFAULT 0,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plans (
       id UUID PRIMARY KEY,
       owner_id UUID NOT NULL,
       copied_from_id UUID NULL,
       title VARCHAR(150) NOT NULL,
       city VARCHAR(100) NOT NULL,
       days INT NOT NULL,
       visibility VARCHAR(10) NOT NULL DEFAULT 'PRIVATE',
       ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
       FOREIGN KEY (copied_from_id) REFERENCES plans(id) ON DELETE SET NULL
);

CREATE TABLE plan_items (
        id UUID PRIMARY KEY,
        plan_id UUID NOT NULL,
        tripadvisor_location_id VARCHAR(30) NOT NULL,
        day_number INT NOT NULL,
        order_index INT NOT NULL,
        note VARCHAR(500),
        FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE TABLE adventures (
        id UUID PRIMARY KEY,
        user_id UUID NOT NULL,
        plan_id UUID NOT NULL,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE TABLE visits (
        id UUID PRIMARY KEY,
        adventure_id UUID NOT NULL,
        plan_item_id UUID NOT NULL,
        visited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE (adventure_id, plan_item_id),
        FOREIGN KEY (adventure_id) REFERENCES adventures(id) ON DELETE CASCADE,
        FOREIGN KEY (plan_item_id) REFERENCES plan_items(id) ON DELETE CASCADE
);

CREATE TABLE ratings (
     id UUID PRIMARY KEY,
     user_id UUID NOT NULL,
     plan_id UUID NOT NULL,
     stars SMALLINT NOT NULL CHECK (stars BETWEEN 1 AND 5),
     comment VARCHAR(500),
     UNIQUE (user_id, plan_id),
     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
     FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE TABLE user_badges (
         user_id UUID NOT NULL,
         badge_code VARCHAR(40) NOT NULL,
         awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
         PRIMARY KEY (user_id, badge_code),
         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);