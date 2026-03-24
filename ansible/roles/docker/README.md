# Docker Role

Verifies Docker installation and configuration for the Travel Management System.

## Requirements

- Ansible 2.9+
- Docker installed on target system
- Root/sudo privileges for adding users to docker group

## Role Variables

### Default Variables

| Variable | Default | Description |
|-----------|----------|-------------|
| `docker_required_packages` | `['docker.io', 'docker-compose']` | Docker packages to check |
| `docker_service_name` | `docker` | Docker service name |
| `docker_service_state` | `started` | Desired service state |
| `docker_group` | `docker` | Docker group name |
| `docker_compose_v2_plugin` | `"docker compose"` | Docker Compose v2 command |

### Variable Overrides

These can be overridden in `group_vars/all.yml` or playbook:

```yaml
docker_users:
  - hesham
  - another_user

docker_group: docker
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
    - docker
```

### Standalone Execution

```bash
ansible-playbook playbooks/site.yml --tags docker
```

## Tasks

This role performs the following tasks:

1. **Check Docker Installation** - Verifies Docker is installed and shows version
2. **Check Docker Compose** - Verifies Docker Compose v2 plugin is available
3. **Verify Docker Service** - Ensures Docker service is running
4. **Ensure Docker Group** - Creates docker group if it doesn't exist
5. **Add Users to Group** - Adds users from `docker_users` variable to docker group
6. **Verify Membership** - Confirms users are members of docker group
7. **Notify for Logout** - Informs users they need to log out for group changes

## Important Notes

- **Logout Required**: After adding users to the docker group, they must log out and log back in for changes to take effect
- **No Docker Installation**: This role verifies Docker is already installed. It does NOT install Docker (assumed to be pre-installed)
- **Docker Compose v2**: This role checks for Docker Compose v2 plugin (not standalone docker-compose binary)

## Troubleshooting

**Issue**: User cannot run docker without sudo after playbook
- **Solution**: Log out and log back in for group changes to take effect

**Issue**: "docker: command not found"
- **Solution**: Install Docker on the target system first

**Issue**: Docker service not running
- **Solution**: Run `sudo systemctl start docker` or `sudo systemctl enable docker`

## License

MIT
