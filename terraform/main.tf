terraform {
  required_version = "1.4.5"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.27.0"
    }
  }
  backend "gcs" {
    bucket = "tsc-terraform-state"
    prefix = "predictions-data-proxy"
  }
}

provider "google" {
  project = "transit-stats-chicago"
  region  = "us-central1"
}