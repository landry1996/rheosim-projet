# Policy for the RheoSim Backend service
# Grants read access to secrets needed by the Spring Boot application

# JWT signing key
path "secret/data/rheosim/jwt" {
  capabilities = ["read"]
}

# Database credentials (dynamic)
path "database/creds/rheosim-backend" {
  capabilities = ["read"]
}

# Stripe API keys
path "secret/data/rheosim/stripe" {
  capabilities = ["read"]
}

# Email service credentials
path "secret/data/rheosim/email" {
  capabilities = ["read"]
}

# Kafka credentials
path "secret/data/rheosim/kafka" {
  capabilities = ["read"]
}

# Transit encryption for sensitive data
path "transit/encrypt/rheosim-data" {
  capabilities = ["update"]
}

path "transit/decrypt/rheosim-data" {
  capabilities = ["update"]
}

# Token self-renewal
path "auth/token/renew-self" {
  capabilities = ["update"]
}

path "auth/token/lookup-self" {
  capabilities = ["read"]
}
