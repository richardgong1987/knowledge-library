#!/bin/bash
# Compute Engine runs this as root on every boot of the VM.
apt-get update
apt-get install -y nginx
curl -s -H "Metadata-Flavor: Google" \
  "http://metadata.google.internal/computeMetadata/v1/instance/attributes/menu" \
  > /var/www/html/index.html
