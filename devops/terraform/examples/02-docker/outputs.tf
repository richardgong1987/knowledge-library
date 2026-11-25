output "urls" {
  description = "Open these in a browser"
  value       = { for name, port in var.stands : name => "http://localhost:${port}" }
}
