# Vault Role

Configures HashiCorp Vault for secrets management in the Travel Management System.

## Requirements

- Ansible 2.9+
- HashiCorp Vault binary installed on target system
- Vault must be initialized (unseal keys available)
- Write permissions for `~/.vault-keys/`

## Role Variables

### Default Variables

| Variable | Default | Description |
|-----------|----------|-------------|
| `vault_address` | `"http://127.0.0.1:8200"` | Vault API address |
| `vault_config_path` | `"{{ ansible_env.HOME }}/.vault-token"` | Vault token config path |
| `vault_keys_path` | `"{{ ansible_env.HOME }}/.vault-keys"` | Unseal keys directory |
| `vault_init` | `false` | Initialize Vault if not already |
| `vault_kv_path` | `"secret"` | KV secrets engine path |
| `vault_kv_mount` | `"travel-system"` | KV secrets mount point |
| `vault_health_endpoint` | `"http://127.0.0.1:8200/v1/sys/health"` | Health check URL |

### Secrets Variables

| Variable | Default | Description |
|-----------|----------|-------------|
| `vault_secrets.postgres_password` | `"mysecretpassword"` | PostgreSQL database password |
| `vault_secrets.neo4j_password` | `"mysecretpassword"` | Neo4j database password |
| `vault_secrets.jwt_secret` | Generated random string | JWT signing secret |
| `vault_secrets.stripe_api_key` | `""` (empty) | Stripe API key (from env) |
| `vault_secrets.paypal_client_id` | `""` (empty) | PayPal client ID (from env) |
| `vault_secrets.paypal_client_secret` | `""` (empty) | PayPal client secret (from env) |

### Variable Overrides

These can be overridden in `group_vars/all.yml`:

```yaml
vault:
  address: "http://localhost:8200"
  token: "{{ lookup('env', 'VAULT_TOKEN') }}"

vault_secrets:
  postgres_password: "your_custom_password"
  neo4j_password: "your_custom_password"
```

## Dependencies

None

## Example Usage

### Using in Playbook

```yaml
---
- hosts: localhost
  become: true
  roles:
    - vault
```

### Standalone Execution

```bash
ansible-playbook playbooks/test_vault.yml
```

## Tasks

This role performs the following tasks:

1. **Check Vault Status** - Verifies Vault is running and accessible
2. **Check Initialization** - Checks if Vault is already initialized
3. **Unseal Vault** - Unseals Vault if needed (using stored unseal keys)
4. **Verify Unseal** - Confirms Vault is unsealed and ready
5. **Get Root Token** - Retrieves Vault root token for API access
6. **Enable KV Secrets** - Enables KV secrets engine v2
7. **Store Secrets** - Stores PostgreSQL, Neo4j, and JWT secrets in Vault
8. **Verify Secrets** - Confirms secrets were stored successfully
9. **Export Token** - Adds VAULT_TOKEN to .bashrc for shell access

## Important Notes

- **Vault Initialization**: This role assumes Vault is initialized (unseal keys exist). If Vault is not initialized, you must run `vault operator init` first.
- **Unseal Keys**: Should be stored securely in `~/.vault-keys/` directory
- **Token Persistence**: VAULT_TOKEN is exported to .bashrc for persistent access across sessions
- **Security**: Never commit unseal keys or root tokens to version control
- **Password Defaults**: Default passwords in `defaults/main.yml` should be changed for production

## Vault Secret Paths

After role execution, secrets will be stored at:

- `secret/data/travel-system/postgres` - PostgreSQL password
- `secret/data/travel-system/neo4j` - Neo4j password
- `secret/data/travel-system/jwt` - JWT signing secret
- `secret/data/travel-system/stripe` - Stripe API key (if provided)
- `secret/data/travel-system/paypal` - PayPal credentials (if provided)

## Retrieving Secrets

To retrieve secrets from Vault:

```bash
# Using Vault CLI
vault kv get secret/travel-system/postgres

# Using curl
curl -H "X-Vault-Token: $VAULT_TOKEN" \
     http://localhost:8200/v1/secret/data/travel-system/postgres
```

## Troubleshooting

**Issue**: "Vault is not running"
- **Solution**: Start Vault service: `vault server -config=/path/to/config.hcl`

**Issue**: "Vault is sealed"
- **Solution**: Provide unseal keys: `vault operator unseal <key1> <key2> <key3>`

**Issue**: "permission denied accessing vault keys"
- **Solution**: Check permissions on `~/.vault-keys/` directory

**Issue**: "Token not found"
- **Solution**: Ensure unseal keys exist and Vault is initialized

**Issue**: "Secrets not stored"
- **Solution**: Check VAULT_TOKEN is set correctly and Vault is unsealed

**Issue**: "403 Forbidden" when storing secrets
- **Solution**: Verify token has proper permissions (root token recommended)

## License

MIT

