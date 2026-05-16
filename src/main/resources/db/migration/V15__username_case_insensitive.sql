CREATE UNIQUE INDEX uk_users_username_ci ON users (LOWER(username));
