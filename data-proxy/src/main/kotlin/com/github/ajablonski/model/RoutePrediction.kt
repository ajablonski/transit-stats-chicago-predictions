package com.github.ajablonski.model

import kotlinx.serialization.Serializable

@Serializable
data class RoutePrediction(
    val route: String,
    val destinationPrediction: List<DestinationPrediction>
)

@Serializable
data class DestinationPrediction(
    val destination: String,
    val arrivalTimes: List<ArrivalTime>
)

@Serializable
data class ArrivalTime(
    val timeInMinutes: Int?,
    val realTimeTracked: Boolean,
    val delayed: Boolean = false
)