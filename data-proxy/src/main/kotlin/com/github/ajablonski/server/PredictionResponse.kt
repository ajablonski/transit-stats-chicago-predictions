package com.github.ajablonski.server

import com.github.ajablonski.model.RoutePrediction
import kotlinx.serialization.Serializable

@Serializable
data class PredictionResponse(
    val predictions: List<RoutePrediction>
)

