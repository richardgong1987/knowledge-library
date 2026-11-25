# Terraform lab: the Lemonade Stand as code

Small, runnable examples for [Part 6 of the DevOps book](../index.qmd). You only need Docker. Terraform itself runs from the official image.

## One-time setup

```bash
tf() {
  docker run --rm -it -e CHECKPOINT_DISABLE=1 \
    -v "$PWD":/work -w /work \
    -v /var/run/docker.sock:/var/run/docker.sock \
    hashicorp/terraform:1.17.2 "$@"
}
```

`CHECKPOINT_DISABLE=1` stops Terraform's online version check, which can hang inside a container. The Docker socket mount is only needed for `02-docker`.

## The examples

| Folder | Lesson | Try |
| --- | --- | --- |
| `01-local/` | 1, 3, 4 | `tf init && tf plan && tf apply`, then `rm menu.txt && tf plan` (drift), then `tf destroy` |
| `02-docker/` | 2, 4, 5, 6 | `tf init && tf apply`, open <http://localhost:8088> and <http://localhost:8089>, try `tf plan -var price=0`, then `tf destroy` |
| `03-module/` | 8 | `tf init && tf apply && cat menu-web1.txt menu-web2.txt`, then `tf destroy` |
| `aws/` | 9 | `tf init -backend=false && tf validate`; `apply` needs an AWS account and costs money |
| `gcp/` | 9 | `tf init -backend=false && tf validate`; `apply` needs a Google Cloud project |

If `validate` in `aws/` stops with `Plugin did not respond`, the Docker VM is short of memory. Give Docker more memory, or add `-e GOMEMLIMIT=450MiB -e GOGC=20` to the `docker run` line.

## What is committed and what is not

- Committed: the `.tf` files and each folder's `.terraform.lock.hcl`. Terraform's own `init` message asks you to commit the lock file, so that everyone installs the same provider versions.
- Ignored (see `.gitignore`): `.terraform/` (downloaded plugins), `*.tfstate*` (state, which can contain secrets) and the generated `menu*.txt` files.

Always run `tf destroy` when you finish an example.
