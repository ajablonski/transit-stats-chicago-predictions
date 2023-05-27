package com.github.ajablonski

import java.nio.file.Path

object Constants {
    val defaultSecretPath: Path = Path.of("/etc/secrets/gtfs_secrets.json")
    const val trainTrackerBaseUrl = "https://lapi.transitchicago.com/api/1.0/ttarrivals.aspx"
    const val busTrackerBaseUrl = "https://www.ctabustracker.com/bustime/api/v2/getpredictions"
}