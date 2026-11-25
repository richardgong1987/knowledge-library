# Lemonade Stand: CI/CD lab

The running example of the CI/CD part of the DevOps book. It is a dependency-free
Node.js service with one business rule: an order of 10 cups or more gets 10% off.

| Path | Purpose |
| --- | --- |
| `server.mjs` | `priceOrder()` (pure pricing rule) and `createHandler()` (`/`, `/health`, `/price`) |
| `server.test.mjs` | Tests of the rule and of the HTTP endpoints (`node:test`) |
| `Dockerfile` | Image on a pinned `node:26.13.1-alpine3.24` base, running as the `node` user |
| `k8s/deployment.yaml` | Namespace `lemonade`, Deployment and Service `lemonade-web` (label `app: lemonade`) |
| `workflows/ci.yml` | Test, build and push to GHCR, deploy to staging, then production |
| `workflows/deploy.yml` | Reusable deploy: apply manifests, wait for the rollout, roll back on failure |

## Run it locally

```bash
npm test                                  # or: node --test
PORT=8080 APP_VERSION=local node server.mjs
curl localhost:8080/health                # {"status":"ok","version":"local"}
curl 'localhost:8080/price?classic=6&mint=4'   # {"cups":10,"totalCents":2610}
```

## Build the image

```bash
docker build -t lemonade-stand:dev .
docker run --rm -p 8080:8080 lemonade-stand:dev
```

## Use the pipeline in your own repository

1. Copy every file in this folder to the root of a new GitHub repository.
2. Move `workflows/ci.yml` and `workflows/deploy.yml` to `.github/workflows/`.
3. Create two environments in **Settings → Environments**: `staging` and `production`.
   Add a required reviewer to `production`.
4. In each environment, add a secret `KUBECONFIG_B64`: the base64-encoded kubeconfig of
   that environment's k3s cluster (`base64 < k3s.yaml`). The cluster API must be reachable
   from the runner, or use a self-hosted runner inside your network.
5. Make the GHCR package public, or give the cluster an image pull secret.

The workflows are examples: they are not active in the knowledge-library repository.
