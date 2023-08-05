terraform {
  required_version = "1.5.4"
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

provider "google-beta" {
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

data "google_project" "project" {}

data "google_service_account" "gen2_compute_user" {
  account_id = "${data.google_project.project.number}-compute@developer.gserviceaccount.com"
}

resource "google_api_gateway_api" "predictions-data-proxy" {
  provider = google-beta

  api_id = "predictions-data-proxy-api"
  display_name = "Predictions Data Proxy API"
}

resource "random_id" "api-config-suffix" {
  byte_length = 10

  keepers = {
    api_config_contents = filebase64("${path.root}/../data-proxy/openapi-spec.yaml")
  }
}

resource "google_api_gateway_api_config" "predictions-data-proxy" {
  provider = google-beta

  api = google_api_gateway_api.predictions-data-proxy.api_id
  api_config_id = "predictions-data-proxy-api-config-${lower(random_id.api-config-suffix.id)}"

  openapi_documents {
    document {
      contents = filebase64("${path.root}/../data-proxy/openapi-spec.yaml")
      path     = "spec.yaml"
    }
  }

  gateway_config {
    backend_config {
      google_service_account = data.google_service_account.gen2_compute_user.email
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_api_gateway_gateway" "predictions-data-proxy" {
  provider = google-beta

  api_config = google_api_gateway_api_config.predictions-data-proxy.id
  gateway_id = "predictions-data-proxy-gateway"
  region = "us-central1"
}
