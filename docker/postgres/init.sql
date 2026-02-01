-- Create user and database for Keycloak
CREATE USER keycloak WITH PASSWORD 'keycloak';
CREATE DATABASE keycloak WITH OWNER keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- Create user and database for application
CREATE USER buurman WITH PASSWORD 'buurman';
CREATE DATABASE buurman WITH OWNER buurman;
GRANT ALL PRIVILEGES ON DATABASE buurman TO buurman;
