terraform {
  required_providers {
    local  = { source = "hashicorp/local", version = "~> 2.9" }
    random = { source = "hashicorp/random", version = "~> 3.9" }
  }
}

resource "random_pet" "stand" {}

resource "local_file" "menu" {
  filename = "${path.module}/menu.txt"
  content  = "Welcome to the ${random_pet.stand.id} lemonade stand!\nLemonade: $2.50\n"
}
