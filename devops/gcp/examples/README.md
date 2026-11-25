# Google Cloud lab: the Lemonade Stand

These files go with [Part 5: Google Cloud](../index.qmd). They are deliberately small: each file shows one idea.

| File | Lesson | Purpose |
| --- | --- | --- |
| `deploy-cloud-run.sh` | 6, 7 | build and push the image, deploy to Cloud Run, run a 10% canary, promote |
| `index.html` | 4, 5 | the static menu |
| `startup-script.sh` | 4 | VM startup script: installs nginx and serves the menu from metadata |
| `lemonade-role.yaml` | 3 | custom IAM role with four read permissions |
| `lifecycle.json` | 5 | bucket lifecycle: Nearline after 30 days, delete after 365 |

## Before you run anything

**These commands create billable resources.** Read each line first.

1. Create a project you can delete afterwards, link billing, and set a budget (lessons 1 and 9).
2. Change `PROJECT` in `deploy-cloud-run.sh` if your project ID is not `lemonade-stand-dev`. Project IDs are global, so yours will differ.
3. Log in: `gcloud auth login`.

## Deploy the Lemonade Stand to Cloud Run

The script builds the Node.js app from Part 3 (`../../ci-cd/examples/lemonade-stand`), so you need Docker.

```bash
./deploy-cloud-run.sh first v1     # repo, push, deploy v1 to 100%
./deploy-cloud-run.sh canary v2    # deploy v2 with no traffic, then 10%
./deploy-cloud-run.sh promote      # 100% to the newest revision
```

## A VM with the menu

```bash
gcloud compute instances create web1 --zone=europe-west2-a \
  --machine-type=e2-micro --image-family=debian-12 --image-project=debian-cloud \
  --tags=web --metadata-from-file=startup-script=startup-script.sh,menu=index.html
```

Lesson 4 creates the network and the firewall rules this VM needs first.

## Clean up

```bash
gcloud projects delete lemonade-stand-dev
```

This deletes everything in the project. To keep the project, delete resources one by one, as each lesson shows.

## How these files were checked

- `bash -n` on both shell scripts; `json.load` on `lifecycle.json`; `yaml.safe_load` on `lemonade-role.yaml`.
- Every `gcloud` command and flag was checked with `--help` in `gcr.io/google.com/cloudsdktool/google-cloud-cli:slim` (Google Cloud SDK 594.0.1).
- No real Google Cloud resources were created while writing the book.
