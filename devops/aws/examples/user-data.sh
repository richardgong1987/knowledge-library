#!/bin/bash
# Runs once, as root, on the first boot of a Lemonade web server (Amazon Linux 2023).
dnf install -y nginx
echo '<h1>Lemonade Stand</h1><p>Classic lemonade: $2.50</p>' > /usr/share/nginx/html/index.html
systemctl enable --now nginx
