# Database Secrets Engine Configuration
# Generates dynamic PostgreSQL credentials with automatic rotation

# Enable the database secrets engine
# vault secrets enable database

# Configure PostgreSQL connection
# vault write database/config/rheosim-postgresql \
#   plugin_name=postgresql-database-plugin \
#   allowed_roles="rheosim-backend,rheosim-ml-readonly" \
#   connection_url="postgresql://{{username}}:{{password}}@postgresql.rheosim.svc.cluster.local:5432/rheosim?sslmode=require" \
#   username="vault_admin" \
#   password="initial_password" \
#   password_authentication="scram-sha-256"

# Backend role: full access with 1h TTL
# vault write database/roles/rheosim-backend \
#   db_name=rheosim-postgresql \
#   creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
#     GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\"; \
#     GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\";" \
#   revocation_statements="REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM \"{{name}}\"; \
#     REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM \"{{name}}\"; \
#     DROP ROLE IF EXISTS \"{{name}}\";" \
#   default_ttl="1h" \
#   max_ttl="4h"

# ML readonly role: SELECT only with 1h TTL
# vault write database/roles/rheosim-ml-readonly \
#   db_name=rheosim-postgresql \
#   creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
#     GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
#   revocation_statements="REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM \"{{name}}\"; \
#     DROP ROLE IF EXISTS \"{{name}}\";" \
#   default_ttl="1h" \
#   max_ttl="2h"

# Root credential rotation (Vault rotates its own admin creds)
# vault write -force database/rotate-root/rheosim-postgresql
