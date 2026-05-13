-- RheoSim Database Initialization
-- This runs once when PostgreSQL container is first created

CREATE SCHEMA IF NOT EXISTS rheosim;

-- Grant privileges to the application user
GRANT ALL PRIVILEGES ON SCHEMA rheosim TO rheosim;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA rheosim TO rheosim;
ALTER DEFAULT PRIVILEGES IN SCHEMA rheosim GRANT ALL ON TABLES TO rheosim;
ALTER DEFAULT PRIVILEGES IN SCHEMA rheosim GRANT ALL ON SEQUENCES TO rheosim;

-- Set default search path
ALTER ROLE rheosim SET search_path TO rheosim, public;
