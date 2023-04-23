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

resource "google_cloudbuild_trigger" "cloudbuild_trigger" {
  name = "predictions-data-proxy-pipeline-build"
  include_build_logs = "INCLUDE_BUILD_LOGS_WITH_STATUS"
  filename = "cloudbuild.yaml"

  github {
    name = "transit-stats-chicago-predictions"
    owner = "ajablonski"
    push {
      branch = "^main$"
    }
  }
}
