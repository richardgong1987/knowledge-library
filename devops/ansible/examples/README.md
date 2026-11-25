# Lemonade Stand Ansible lab

This lab pretends to be the whole Lemonade Stand fleet on **one laptop**. You need no SSH, no sudo and no servers. Every host uses `ansible_connection: local` and writes into its own folder, `/tmp/lemonade-lab/<host>/`, which stands in for that server's disk.

| Host   | Groups        | Pretend address |
| ------ | ------------- | --------------- |
| `web1` | `web`, `prod` | 192.0.2.11      |
| `web2` | `web`, `prod` | 192.0.2.12      |
| `db1`  | `db`, `prod`  | 192.0.2.21      |

Requires `ansible-core` 2.17 or newer (tested with 2.21.6): `pipx install ansible-core` or `brew install ansible`.

## Files

| File | Shows |
| ---- | ----- |
| `ansible.cfg`, `inventory.yml` | Project settings and the fleet |
| `group_vars/`, `host_vars/` | Variables per group and per host |
| `site.yml` + `roles/lemonade_web/` | A role with templates and a handler |
| `rolling-update.yml` | `serial: 1` upgrade, one server at a time |
| `vault-demo.yml` + `secrets/` | An encrypted password |

## Run it

Run from this folder so Ansible finds `ansible.cfg`.

```bash
ansible-inventory --graph
ansible all -m ansible.builtin.ping
ansible-playbook site.yml                 # first run: changed > 0
ansible-playbook site.yml                 # second run: changed=0
ansible-playbook site.yml --check --diff -e lemonade_app_port=9090
ansible-playbook rolling-update.yml -e lemonade_version=1.1.0
ansible-playbook vault-demo.yml --vault-password-file secrets/demo-vault-password.txt
```

Add `-e lab_root=/some/folder` to write somewhere else. Clean up with `rm -r /tmp/lemonade-lab`.

## About the vault password

`secrets/demo-vault-password.txt` (`throwaway-lemonade-demo`) is committed **on purpose** so the demo runs. It protects a fake password only. Never commit a real vault password file.
