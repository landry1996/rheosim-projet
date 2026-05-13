# Policy for the RheoSim ML Service
# Grants read access to ML-specific secrets

# MLflow tracking credentials
path "secret/data/rheosim/mlflow" {
  capabilities = ["read"]
}

# Object storage credentials (model artifacts)
path "secret/data/rheosim/s3" {
  capabilities = ["read"]
}

# Database credentials (read-only for ML)
path "database/creds/rheosim-ml-readonly" {
  capabilities = ["read"]
}

# Kafka credentials
path "secret/data/rheosim/kafka" {
  capabilities = ["read"]
}

# Token self-renewal
path "auth/token/renew-self" {
  capabilities = ["update"]
}

path "auth/token/lookup-self" {
  capabilities = ["read"]
}
