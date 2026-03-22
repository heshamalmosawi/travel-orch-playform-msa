# SSL Role

Manages SSL/TLS certificates for the Travel Management System.

## Requirements

- Ansible 2.9+
- SSL certificates exist in `frontend/travel-orch/certs/`
- OpenSSL installed (for certificate expiry checking)
- Write permissions for `ansible/files/certs/`

## Role Variables

### Default Variables

| Variable | Default | Description |
|-----------|----------|-------------|
| `ssl_certs_path` | `"{{ playbook_dir }}/../files/certs"` | Destination directory for certificates |
| `ssl_cert_file` | `"{{ ssl_certs_path }}/{{ ssl_certs.cert }}"` | Full path to certificate file |
| `ssl_key_file` | `"{{ ssl_certs_path }}/{{ ssl_certs.key }}"` | Full path to key file |
| `ssl_cert_mode` | `"0644"` | Certificate file permissions (readable, not writable) |
| `ssl_key_mode` | `"0600"` | Key file permissions (owner read/write only) |
| `ssl_owner` | `"{{ ansible_user }}"` | Certificate owner |
| `ssl_group` | `"{{ ansible_user }}"` | Certificate group |
| `ssl_source_cert_path` | `"{{ project_root }}/frontend/travel-orch/certs/{{ ssl_certs.cert }}"` | Source certificate from frontend |
| `ssl_source_key_path` | `"{{ project_root }}/frontend/travel-orch/certs/{{ ssl_certs.key }}"` | Source key from frontend |
| `ssl_expiry_warning_days` | `30` | Days before expiry to warn |

### Variable Overrides

These can be overridden in `group_vars/all.yml`:

```yaml
ssl_certs:
  cert: "angular-dev.crt"
  key: "angular-dev.key"
```

## Dependencies

- None

## Example Usage

### Using in Playbook

```yaml
---
- hosts: localhost
  become: true
  roles:
    - ssl
```

### Standalone Execution

```bash
ansible-playbook playbooks/test_ssl.yml
```

## Tasks

This role performs the following tasks:

1. **Verify Source Certificates** - Checks that certificates exist in frontend project
2. **Ensure Certs Directory** - Creates `ansible/files/certs/` if it doesn't exist
3. **Copy Certificate** - Copies SSL certificate to `ansible/files/certs/`
4. **Copy Key** - Copies SSL key to `ansible/files/certs/`
5. **Set Permissions** - Sets restrictive permissions (cert: 0644, key: 0600)
6. **Verify Copies** - Confirms certificates were copied successfully
7. **Check Expiry** - Uses OpenSSL to check certificate expiry date
8. **Warn on Expiry** - Warns if certificate expires within 30 days
9. **Display Details** - Shows certificate subject/issuer information

## Important Notes

- **Source Location**: Certificates are copied from `frontend/travel-orch/certs/`
- **Destination Location**: Certificates are placed in `ansible/files/certs/`
- **Permissions**: Key files have 0600 permissions (owner only), cert files have 0644
- **Expiry Check**: Requires OpenSSL to be installed on the system
- **Docker Usage**: These certificates will be mounted into Docker containers via volumes

## Security Considerations

- Key files have restrictive permissions (0600) - owner read/write only
- Certificate files have 0644 permissions - readable by owner/group, not writable
- Certificates should be stored securely and not committed to version control
- Private keys should NEVER be exposed in logs or error messages

## Troubleshooting

**Issue**: "SSL certificates not found in source location"
- **Solution**: Ensure certificates exist at `frontend/travel-orch/certs/angular-dev.{crt,key}`

**Issue**: Permission denied copying certificates
- **Solution**: Check permissions on `ansible/files/certs/` directory

**Issue**: "openssl: command not found"
- **Solution**: Install OpenSSL: `sudo apt install openssl`

**Issue**: Certificate expiry check fails
- **Solution**: Verify certificate is valid PEM format: `openssl x509 -in angular-dev.crt -text -noout`

**Issue**: Certificates already exist with correct permissions
- **Solution**: This is fine - role will show "ok" (not "changed") for existing correct files

## License

MIT

