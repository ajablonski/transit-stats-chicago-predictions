package com.github.ajablonski.model

import kotlinx.serialization.Serializable

@Serializable
data class Secrets(val trainTrackerApiKey: String, val busTrackerApiKey: String)