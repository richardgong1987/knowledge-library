module "web1" {
  source = "./modules/stand"
  name   = "web1"
  price  = 2.5
}

module "web2" {
  source = "./modules/stand"
  name   = "web2"
  price  = 3
}

output "menus" {
  value = [module.web1.menu_file, module.web2.menu_file]
}
