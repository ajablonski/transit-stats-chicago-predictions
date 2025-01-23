package com.github.ajablonski

import com.github.ajablonski.client.BusTrackerResponse
import com.github.ajablonski.client.TrainTrackerResponse
import com.github.ajablonski.model.ArrivalTime
import com.github.ajablonski.model.DestinationPrediction
import com.github.ajablonski.model.RoutePrediction
import com.github.ajablonski.server.PredictionResponse
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.json.Json
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.net.URIBuilder
import java.net.http.HttpClient
import java.time.temporal.ChronoUnit
import java.net.http.HttpRequest as JHttpRequest
import java.net.http.HttpResponse as JHttpResponse

class Entrypoint(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val keyProvider: KeyProvider = FileSecretKeyProvider(),
    private val clock: Clock = Clock.System,
) : HttpFunction {

    private val json = Json { ignoreUnknownKeys = true }

    override fun service(request: HttpRequest?, response: HttpResponse?) {
        response?.run {
            val requestedRoutes = request?.queryParameters?.get("routes")?.flatMap { it.split(",") }.orEmpty()
            val busRoutes = requestedRoutes.filter(busRouteToStopMap::containsKey)
            val busStops = requestedRoutes.flatMap { busRouteToStopMap[it].orEmpty() }

            val predictedBusArrivalTimes = getBusResponses(busRoutes, busStops)

            val trainStops =
                requestedRoutes.flatMap { trainRouteToStationMap.entries.filter { entry -> entry.key == it } }
            val predictedTrainArrivalTimes = trainStops.associate {
                getTrainResponse(it.key, it.value)
            }

            setContentType(ContentType.APPLICATION_JSON.mimeType)
            setStatusCode(200)
            writer.write(
                json.encodeToString(
                    PredictionResponse(
                        predictions = predictedTrainArrivalTimes + predictedBusArrivalTimes,
                        currentTime = clock.now(),
                    )
                )
            )
        }
    }

    private fun getTrainResponse(trainRoute: String, trainStationId: String): Pair<String, RoutePrediction> {
        val uri = URIBuilder(Constants.trainTrackerBaseUrl)
            .addParameter("key", keyProvider.getTrainTrackerApiKey())
            .addParameter("mapid", trainStationId)
            .addParameter("rt", trainRoute)
            .addParameter("outputType", "JSON")
            .build()

        val trainTrackerRequest = JHttpRequest.newBuilder().uri(uri).GET().build()
        val responseBody = httpClient
            .send(trainTrackerRequest, JHttpResponse.BodyHandlers.ofString())
            .body()

        return trainRoute to RoutePrediction(trainRoute, json.decodeFromString<TrainTrackerResponse>(responseBody)
            .ctaData
            .etas
            .groupBy { it.destination }
            .map { (destination, etas) ->
                destination to DestinationPrediction(destination, etas.map {
                    val minutesToArrival = ChronoUnit.MINUTES.between(
                        it.predictionTime.toJavaLocalDateTime(),
                        it.arrivalTime.toJavaLocalDateTime()
                    ).toInt()
                    ArrivalTime(minutesToArrival, it.isScheduled != "1", it.isDelayed == "1")
                })
            }.toMap()
        )
    }

    private fun getBusResponses(busRouteIds: List<String>, busStopIds: List<String>): Map<String, RoutePrediction> {
        if (busRouteIds.isEmpty() || busStopIds.isEmpty()) {
            return emptyMap()
        }
        val uri = URIBuilder(Constants.busTrackerBaseUrl)
            .addParameter("key", keyProvider.getBusTrackerApiKey())
            .addParameter("stpid", busStopIds.joinToString(","))
            .addParameter("format", "json")
            .build()


        val busTrackerRequest = JHttpRequest.newBuilder().uri(uri).GET().build()
        val responseBody = httpClient
            .send(busTrackerRequest, JHttpResponse.BodyHandlers.ofString())
            .body()
        return json.decodeFromString<BusTrackerResponse>(responseBody)
            .busTimeResponse
            .predictions
            .groupBy { it.route }
            .filter { (route, _) -> busRouteIds.contains(route) }
            .map { (route, predictions) ->
                route to RoutePrediction(
                    route,
                    predictions
                        .groupBy { it.destination }
                        .map { (destination, predictions) ->
                            destination to DestinationPrediction(
                                destination,
                                predictions.map {
                                    ArrivalTime(
                                        it.predictionTime.toIntOrNull(),
                                        true,
                                        delayed = it.isDelayed
                                    )
                                })
                        }
                        .toMap()
                )
            }
            .toMap()
    }

    companion object Entrypoint {
        private val busRouteToStopMap: Map<String, List<String>> = mapOf(
            "84" to listOf("11476"),
            "22" to listOf("14792", "14786"),
            "50" to listOf("1802"),
            "92" to listOf("4796"),
            "36" to listOf("5338"),
            "147" to listOf("1038"),
            "136" to listOf("1038")
        )

        private val trainRouteToStationMap: Map<String, String> = mapOf(
            "Red" to "41380",
            "Brn" to "40090"
        )
    }
}

