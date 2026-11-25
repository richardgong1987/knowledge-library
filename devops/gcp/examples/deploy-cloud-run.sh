#!/usr/bin/env bash
# Ship the Lemonade Stand to Cloud Run. This creates billable resources: read it before running.
#   ./deploy-cloud-run.sh first v1    build, push and serve v1 to everyone
#   ./deploy-cloud-run.sh canary v2   deploy v2 with no traffic, then send it 10%
#   ./deploy-cloud-run.sh promote     send 100% to the newest revision
set -euo pipefail

PROJECT=lemonade-stand-dev
REGION=europe-west2
IMAGE="$REGION-docker.pkg.dev/$PROJECT/lemonade/lemonade-stand:${2:-v1}"
APP_DIR="$(dirname "$0")/../../ci-cd/examples/lemonade-stand"

push_image() {
  docker build --platform=linux/amd64 -t "$IMAGE" "$APP_DIR"
  docker push "$IMAGE"
}

case "${1:-first}" in
  first)
    gcloud config set project "$PROJECT"
    gcloud services enable run.googleapis.com artifactregistry.googleapis.com
    gcloud artifacts repositories create lemonade --repository-format=docker --location="$REGION"
    gcloud auth configure-docker "$REGION-docker.pkg.dev"
    push_image
    gcloud run deploy lemonade-web --image="$IMAGE" --region="$REGION" \
      --port=8080 --concurrency=80 --max-instances=10 --allow-unauthenticated
    ;;
  canary)
    push_image
    gcloud run deploy lemonade-web --image="$IMAGE" --region="$REGION" --no-traffic --tag=canary
    gcloud run services update-traffic lemonade-web --region="$REGION" --to-tags=canary=10
    ;;
  promote)
    gcloud run services update-traffic lemonade-web --region="$REGION" --to-latest
    ;;
esac
