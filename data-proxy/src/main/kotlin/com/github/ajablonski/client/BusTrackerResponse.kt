package com.github.ajablonski.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusTrackerResponse(
    @SerialName("bustime-response")
    val busTimeResponse: BusTimeResponse
)

@Serializable
data class BusTimeResponse(
    @SerialName("prd")
    val predictions: List<Prediction> = emptyList()
)

@Serializable
data class Prediction(
    @SerialName("prdctdn")
    val predictionTime: String,

    @SerialName("des")
    val destination: String,

    @SerialName("tmstmp")
    val timestamp: String,

    @SerialName("rt")
    val route: String,

    @SerialName("dly")
    val isDelayed: Boolean
)