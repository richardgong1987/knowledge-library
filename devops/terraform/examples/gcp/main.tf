terraform {
  required_providers {
    google = { source = "hashicorp/google", version = "~> 8.0" }
  }
}

provider "google" {
  project = var.project
  region  = var.region
}

variable "project" {
  type    = string
  default = "lemonade-stand-dev"
}

variable "region" {
  type    = string
  default = "europe-west2"
}

variable "image" {
  description = "Any container that listens on $PORT (8080). Google's sample works out of the box."
  type        = string
  default     = "us-docker.pkg.dev/cloudrun/container/hello"
}

resource "google_cloud_run_v2_service" "web" {
  name                = "lemonade-web"
  location            = var.region
  deletion_protection = false

  template {
    containers {
      image = var.image
    }
  }
}

# Let anyone on the internet call the service (no Google login needed).
resource "google_cloud_run_v2_service_iam_member" "public" {
  name     = google_cloud_run_v2_service.web.name
  location = google_cloud_run_v2_service.web.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}

output "url" {
  value = google_cloud_run_v2_service.web.uri
}
