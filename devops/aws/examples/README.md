# Part 4 lab: AWS without an AWS account

These files go with [Part 4: AWS](../index.qmd). The demo script runs the core commands against
[LocalStack](https://www.localstack.cloud/), an AWS emulator in Docker. It uses dummy credentials and a
local endpoint, so it can never create real resources or cost money.

| File | Used in | What it is |
| --- | --- | --- |
| `localstack-demo.sh` | lessons 2, 5, 7, 9 | STS, S3, IAM, DynamoDB and Lambda commands, then clean-up |
| `menu-editor-policy.json` | lesson 2 | least-privilege IAM policy: read and replace the menu, nothing else |
| `menu-bucket-policy.json` | lesson 5 | bucket policy: anyone may read the menu |
| `index.html` | lesson 5 | the Lemonade menu page |
| `user-data.sh` | lesson 4 | first-boot script for an EC2 web server (Amazon Linux 2023) |
| `count_orders.py` | lesson 9 | Lambda handler that counts orders and adds up the money |

## Run it

You need Docker, the [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
and Python 3.

```bash
# 4.12 is the last LocalStack community image that starts without an account token.
docker run -d --name localstack -p 4566:4566 \
  -v /var/run/docker.sock:/var/run/docker.sock localstack/localstack:4.12
./localstack-demo.sh
docker rm -f localstack
```

The Docker socket lets LocalStack start a real Python container for the Lambda function. Expected output, trimmed:

```text
== STS: which account am I using?
000000000000
== S3: bucket, upload, list, website, presigned link
make_bucket: lemonade-menu-7f3a
...
== DynamoDB: an orders table, one order, one query
ACTIVE
1
== Lambda: count today's orders
count-orders
{"orders": 2, "revenue": 8.0}
== Clean up: always delete what you created
remove_bucket: lemonade-menu-7f3a
```

LocalStack's free edition does not include the IAM policy simulator, ELB, Auto Scaling, RDS, ECR, ECS or Budgets, so
lessons 2 (simulator), 6, 7 (RDS), 8 and 10 (Budgets) label those outputs as illustrative.

## On a real account

The same commands work against real AWS if you remove the two `export` lines (test credentials and the local
endpoint) and replace the made-up role ARN in the Lambda step with a real execution role. Do that only after
creating a budget (lesson 10), and run the clean-up section every time.
