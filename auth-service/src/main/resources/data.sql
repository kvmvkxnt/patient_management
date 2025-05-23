-- DROP TYPE IF EXISTS role;
-- CREATE TYPE role AS ENUM ('ADMIN', 'PATIENT', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'BILLING', 'PHARMACIST', 'LAB_TECH');
-- -- Ensure the 'users' table exists
CREATE TABLE IF NOT EXISTS "users" (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    roles role[] NOT NULL
);

-- Insert the user if no existing user with the same id or email exists
INSERT INTO "users" (id, username, email, password, roles)
SELECT '223e4567-e89b-12d3-a456-426614174006', 'testuser', 'testuser@test.com',
       '$2b$12$7hoRZfJrRKD2nIm2vHLs7OBETy.LWenXXMLKf99W8M4PUwO6KB7fu', ARRAY['ADMIN']::role[]
WHERE NOT EXISTS (
    SELECT 1
    FROM "users"
    WHERE id = '223e4567-e89b-12d3-a456-426614174006'
       OR email = 'testuser@test.com' OR username = 'testuser'
);



