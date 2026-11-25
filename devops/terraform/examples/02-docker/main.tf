provider "docker" {}

resource "docker_image" "nginx" {
  name = "nginx:1.30.8-alpine"
}

resource "docker_container" "stand" {
  for_each = var.stands

  name  = "lemonade-${each.key}"
  image = docker_image.nginx.image_id

  ports {
    internal = 80
    external = each.value
  }

  upload {
    file    = "/usr/share/nginx/html/index.html"
    content = "<h1>Lemonade Stand ${each.key}</h1><p>Lemonade: ${format("$%.2f", var.price)}</p>\n"
  }
}
