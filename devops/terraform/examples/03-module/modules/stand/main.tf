terraform {
  required_providers {
    local = { source = "hashicorp/local", version = "~> 2.9" }
  }
}

variable "name" { type = string }
variable "price" { type = number }

resource "local_file" "menu" {
  filename = "${path.root}/menu-${var.name}.txt"
  content  = "Stand ${var.name}: lemonade ${format("$%.2f", var.price)}\n"
}

output "menu_file" {
  value = local_file.menu.filename
}
