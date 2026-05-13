# Transit Secrets Engine Configuration
# Encryption-as-a-service for sensitive data (PII, credentials in DB)

# Enable transit engine
# vault secrets enable transit

# Create encryption key for user data (PII)
# vault write -f transit/keys/rheosim-data \
#   type=aes256-gcm96 \
#   auto_rotate_period=720h \
#   min_decryption_version=1 \
#   min_encryption_version=0 \
#   deletion_allowed=false

# Create encryption key for JWT signing
# vault write -f transit/keys/rheosim-jwt \
#   type=rsa-4096 \
#   auto_rotate_period=24h \
#   min_decryption_version=1 \
#   deletion_allowed=false

# Example usage:
# Encrypt: vault write transit/encrypt/rheosim-data plaintext=$(echo "sensitive" | base64)
# Decrypt: vault write transit/decrypt/rheosim-data ciphertext="vault:v1:..."
# Sign JWT: vault write transit/sign/rheosim-jwt input=$(echo "jwt_payload" | base64)
# Verify:   vault write transit/verify/rheosim-jwt input=... signature=...

# Key rotation policy:
# - rheosim-data: auto-rotates every 30 days
# - rheosim-jwt: auto-rotates every 24 hours
#   * Old keys remain valid for 48h (grace period for token verification)
#   * Minimum decryption version prevents using very old keys
