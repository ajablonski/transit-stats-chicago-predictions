package com.github.ajablonski.server

import com.github.ajablonski.model.RoutePrediction
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PredictionResponse(
    val predictions: Map<String, RoutePrediction>,
    val currentTime: Instant
)

