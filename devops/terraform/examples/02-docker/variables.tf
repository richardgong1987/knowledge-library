variable "stands" {
  description = "Stand name => port on your laptop"
  type        = map(number)
  default     = { web1 = 8088, web2 = 8089 }
}

variable "price" {
  description = "Price of one cup, in dollars"
  type        = number
  default     = 2.5

  validation {
    condition     = var.price > 0
    error_message = "A cup of lemonade cannot be free or cost less than nothing."
  }
}
