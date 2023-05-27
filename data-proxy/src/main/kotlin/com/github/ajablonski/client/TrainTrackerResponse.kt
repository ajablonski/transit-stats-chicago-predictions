package com.github.ajablonski.client

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainTrackerResponse(
    @SerialName("ctatt")
    val ctaData: CtaData
)

@Serializable
data class CtaData(
    @SerialName("eta")
    val etas: List<TrainEta>
)

/*
      {
    "staId": "41380",
    "stpId": "30267",
    "staNm": "Bryn Mawr",
    "stpDe": "Service toward Howard",
    "rn": "803",
    "rt": "Red",
    "destSt": "30173",
    "destNm": "Howard",
    "trDr": "1",
    "prdt": "2023-05-26T13:22:36",
    "arrT": "2023-05-26T14:00:36",
    "isApp": "0",
    "isSch": "0",
    "isDly": "0",
    "isFlt": "0",
    "flags": null,
    "lat": "41.84494",
    "lon": "-87.63127",
    "heading": "358"
  },

 */
@Serializable
data class TrainEta(
    @SerialName("destNm")
    val destination: String,

    @SerialName("prdt")
    val predictionTime: LocalDateTime,

    @SerialName("arrT")
    val arrivalTime: LocalDateTime,

    @SerialName("rt")
    val route: String,

    @SerialName("isSch")
    val isScheduled: String,

    @SerialName("isDly")
    val isDelayed: String
)
