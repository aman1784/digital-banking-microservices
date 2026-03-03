-- Use INSERT IGNORE to skip if the role already exists
INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');

-- Use INSERT IGNORE to skip if the username 'aman' already exists
INSERT IGNORE INTO users (username, password, enabled)
VALUES ('aman', '$2a$10$6Ng/XzLCShLdBGeuqH5tnOucfMe5G0DWaND8jgea/5OglSClafMM.', true);

-- We use IGNORE here too, just in case 'aman' already has the admin role
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (1, 2);