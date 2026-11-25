#!/usr/bin/env bash
# Practise the core Part 4 commands against LocalStack, a local AWS emulator. Never real AWS.
# Start LocalStack first (4.12 is the last community image that needs no account):
#   docker run -d --name localstack -p 4566:4566 \
#     -v /var/run/docker.sock:/var/run/docker.sock localstack/localstack:4.12
set -euo pipefail
cd "$(dirname "$0")"

# Dummy credentials and a local endpoint: nothing here can reach a real AWS account.
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=eu-west-2
export AWS_ENDPOINT_URL="${AWS_ENDPOINT_URL:-http://localhost:4566}" AWS_PAGER=""
BUCKET=lemonade-menu-7f3a

echo "== STS: which account am I using?"
aws sts get-caller-identity --query Account --output text

echo "== S3: bucket, upload, list, website, presigned link"
aws s3 mb "s3://$BUCKET"
aws s3 cp index.html "s3://$BUCKET/index.html"
aws s3 ls "s3://$BUCKET"
aws s3 website "s3://$BUCKET" --index-document index.html
aws s3 presign "s3://$BUCKET/index.html" --expires-in 300

echo "== IAM: create the menu-editor policy"
aws iam create-policy --policy-name lemonade-menu-editor \
  --policy-document file://menu-editor-policy.json --query Policy.Arn --output text

echo "== DynamoDB: an orders table, one order, one query"
aws dynamodb create-table --table-name lemonade-orders --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=order_date,AttributeType=S AttributeName=order_id,AttributeType=S \
  --key-schema AttributeName=order_date,KeyType=HASH AttributeName=order_id,KeyType=RANGE \
  --query TableDescription.TableStatus --output text
aws dynamodb put-item --table-name lemonade-orders \
  --item '{"order_date":{"S":"2026-11-25"},"order_id":{"S":"0001"},"cups":{"N":"2"}}'
aws dynamodb query --table-name lemonade-orders --key-condition-expression "order_date = :d" \
  --expression-attribute-values '{":d":{"S":"2026-11-25"}}' --query Count

echo "== Lambda: count today's orders"
python3 -m zipfile -c count_orders.zip count_orders.py
aws lambda create-function --function-name count-orders --runtime python3.13 \
  --handler count_orders.handler --zip-file fileb://count_orders.zip \
  --role arn:aws:iam::000000000000:role/lemonade-lambda --query FunctionName --output text
aws lambda wait function-active-v2 --function-name count-orders
aws lambda invoke --function-name count-orders --cli-binary-format raw-in-base64-out \
  --payload '{"orders":[{"price":2.5,"cups":2},{"price":3.0,"cups":1}]}' result.json >/dev/null
cat result.json; echo

echo "== Clean up: always delete what you created"
aws lambda delete-function --function-name count-orders >/dev/null
aws dynamodb delete-table --table-name lemonade-orders >/dev/null
aws s3 rb "s3://$BUCKET" --force
rm -f count_orders.zip result.json
