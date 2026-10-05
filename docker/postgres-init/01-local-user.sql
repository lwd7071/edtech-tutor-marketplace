\getenv edtech_db_password DB_PASSWORD

CREATE ROLE edtech_user WITH LOGIN PASSWORD :'edtech_db_password';
ALTER DATABASE edtech_db OWNER TO edtech_user;
