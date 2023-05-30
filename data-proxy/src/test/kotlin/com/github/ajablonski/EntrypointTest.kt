package com.github.ajablonski

import com.github.ajablonski.model.ArrivalTime
import com.github.ajablonski.model.DestinationPrediction
import com.github.ajablonski.model.RoutePrediction
import com.github.ajablonski.server.PredictionResponse
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.apache.hc.core5.net.URIBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedWriter
import java.io.StringWriter
import java.net.http.HttpClient
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse as JHttpResponse

class EntrypointTest() {
    private val outputWriter = StringWriter()
    private val response = mockk<HttpResponse>(relaxed = true) {
        every { writer }.returns(BufferedWriter(outputWriter))
    }
    private val httpClient = mockk<HttpClient>(relaxed = true)
    private val busTrackerKey = "fakeBusTrackerKey"
    private val trainTrackerKey = "fakeTrainTrackerKey"
    private val keyProvider = mockk<KeyProvider> {
        every { getBusTrackerApiKey() }.returns(busTrackerKey)
        every { getTrainTrackerApiKey() }.returns(trainTrackerKey)
    }
    private lateinit var entrypoint: Entrypoint

    @BeforeEach
    fun beforeEach() {
        entrypoint = Entrypoint(httpClient, keyProvider)
    }

    @Test
    fun shouldRetrieveBusSingleRouteResponse() {
        setupBusResponse(listOf("11474"), "samples/84.json")

        val request = buildHttpRequest(listOf("84"))
        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "84" to RoutePrediction(
                    route = "84",
                    mapOf(
                        "Caldwell/Central" to DestinationPrediction(
                            destination = "Caldwell/Central",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 4, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 27, realTimeTracked = true)
                            )
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveBusMultiStopResponse() {
        setupBusResponse(listOf("14792", "14786"), "samples/22.json")

        val request = buildHttpRequest(listOf("22"))
        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())
        val expectedResponse = PredictionResponse(
            mapOf(
                "22" to RoutePrediction(
                    route = "22",
                    destinationPrediction = mapOf(
                        "Harrison" to DestinationPrediction(
                            destination = "Harrison",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 2, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 9, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 25, realTimeTracked = true)
                            )
                        ),
                        "Howard" to DestinationPrediction(
                            destination = "Howard",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 11, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 25, realTimeTracked = true)
                            )
                        )
                    )
                )
            )
        )

        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveBusMultiRouteResponse() {
        setupBusResponse(listOf("1802", "11474"), "samples/50_84_combined.json")

        val request = buildHttpRequest(listOf("50", "84"))
        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "50" to RoutePrediction(
                    route = "50",
                    mapOf(
                        "35th/Archer Orange Line" to DestinationPrediction(
                            destination = "35th/Archer Orange Line",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 9, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 28, realTimeTracked = true)
                            )
                        )
                    )
                ),
                "84" to RoutePrediction(
                    route = "84",
                    mapOf(
                        "Caldwell/Central" to DestinationPrediction(
                            destination = "Caldwell/Central",
                            arrivalTimes = listOf(ArrivalTime(timeInMinutes = 29, realTimeTracked = true))
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldSplitCommaSeparatedParameters() {
        setupBusResponse(listOf("1802", "11474"), "samples/50_84_combined.json")

        val request = buildHttpRequest(listOf("50,84"))
        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "50" to RoutePrediction(
                    route = "50",
                    mapOf(
                        "35th/Archer Orange Line" to DestinationPrediction(
                            destination = "35th/Archer Orange Line",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 9, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 28, realTimeTracked = true)
                            )
                        )
                    )
                ),
                "84" to RoutePrediction(
                    route = "84",
                    mapOf(
                        "Caldwell/Central" to DestinationPrediction(
                            destination = "Caldwell/Central",
                            arrivalTimes = listOf(ArrivalTime(timeInMinutes = 29, realTimeTracked = true))
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveBusMultiRouteMultiStopResponse() {
        setupBusResponse(listOf("1802", "11474"), "samples/50_84_combined.json")

        val request = buildHttpRequest(listOf("50", "84"))
        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "50" to RoutePrediction(
                    "50",
                    mapOf(
                        "35th/Archer Orange Line" to DestinationPrediction(
                            "35th/Archer Orange Line",
                            listOf(ArrivalTime(9, true), ArrivalTime(28, true))
                        )
                    )
                ),
                "84" to RoutePrediction(
                    "84",
                    mapOf(
                        "Caldwell/Central" to DestinationPrediction(
                            "Caldwell/Central",
                            listOf(ArrivalTime(29, true))
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveBusStopsWithMultipleRoutes() {
        setupBusResponse(listOf("1038"), "samples/147_mixed.json")

        val request = buildHttpRequest(listOf("147"))
        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "147" to RoutePrediction(
                    route = "147",
                    mapOf(
                        "Congress Plaza" to DestinationPrediction(
                            destination = "Congress Plaza",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 11, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 25, realTimeTracked = true)
                            )
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveBusDelayedResponse() {
        setupBusResponse(listOf("11474"), "samples/84_delayed.json")

        val request = buildHttpRequest(listOf("84"))
        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "84" to RoutePrediction(
                    route = "84",
                    mapOf(
                        "Caldwell/Central" to DestinationPrediction(
                            destination = "Caldwell/Central",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = null, realTimeTracked = true, delayed = true),
                                ArrivalTime(timeInMinutes = 27, realTimeTracked = true)
                            )
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveTrainResponse() {
        setupTrainResponse(listOf("41380"), listOf("samples/Red.json"))

        val request = buildHttpRequest(listOf("Red"))
        entrypoint.service(request, response)
        response.writer.flush()
        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "Red" to RoutePrediction(
                    route = "Red",
                    mapOf(
                        "Howard" to DestinationPrediction(
                            destination = "Howard",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 2, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 5, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 7, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 19, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 28, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 38, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 50, realTimeTracked = true)
                            )
                        ),
                        "95th/Dan Ryan" to DestinationPrediction(
                            destination = "95th/Dan Ryan",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 4, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 11, realTimeTracked = true),
                            )
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveTrainResponseWithDelay() {
        setupTrainResponse(listOf("41380"), listOf("samples/Red_delayed.json"))

        val request = buildHttpRequest(listOf("Red"))
        entrypoint.service(request, response)
        response.writer.flush()
        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "Red" to RoutePrediction(
                    route = "Red",
                    mapOf(
                        "Howard" to DestinationPrediction(
                            destination = "Howard",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 2, realTimeTracked = true, delayed = true),
                            )
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveTrainResponseWithScheduled() {
        setupTrainResponse(listOf("41380"), listOf("samples/Red_scheduled.json"))

        val request = buildHttpRequest(listOf("Red"))
        entrypoint.service(request, response)
        response.writer.flush()
        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "Red" to RoutePrediction(
                    route = "Red",
                    mapOf(
                        "Howard" to DestinationPrediction(
                            destination = "Howard",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 2, realTimeTracked = false),
                            )
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun shouldRetrieveTrainMultiRouteResponse() {
        setupTrainResponse(listOf("41380"), listOf("samples/Red.json"))
        setupTrainResponse(listOf("40090"), listOf("samples/Brown.json"))

        val request = buildHttpRequest(listOf("Red", "Brn"))

        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "Red" to RoutePrediction(
                    route = "Red",
                    mapOf(
                        "Howard" to DestinationPrediction(
                            destination = "Howard",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 2, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 5, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 7, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 19, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 28, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 38, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 50, realTimeTracked = true)
                            )
                        ),
                        "95th/Dan Ryan" to DestinationPrediction(
                            destination = "95th/Dan Ryan",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 4, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 11, realTimeTracked = true),
                            )
                        )
                    )
                ),
                "Brn" to RoutePrediction(
                    route = "Brn",
                    mapOf(
                        "Loop" to DestinationPrediction(
                            destination = "Loop",
                            arrivalTimes = listOf(ArrivalTime(timeInMinutes = 5, realTimeTracked = true))
                        ),
                        "Kimball" to DestinationPrediction(
                            destination = "Kimball",
                            arrivalTimes = listOf(ArrivalTime(timeInMinutes = 16, realTimeTracked = true))
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun testRetrieveTrainAndBusResponse() {
        setupTrainResponse(listOf("41380"), listOf("samples/Red.json"))
        setupBusResponse(listOf("11474"), "samples/84.json")

        val request = buildHttpRequest(listOf("Red", "84"))

        entrypoint.service(request, response)
        response.writer.flush()

        val result = Json.decodeFromString<PredictionResponse>(outputWriter.toString())

        val expectedResponse = PredictionResponse(
            mapOf(
                "Red" to RoutePrediction(
                    route = "Red",
                    mapOf(
                        "Howard" to DestinationPrediction(
                            destination = "Howard",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 2, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 5, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 7, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 19, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 28, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 38, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 50, realTimeTracked = true)
                            )
                        ),
                        "95th/Dan Ryan" to DestinationPrediction(
                            destination = "95th/Dan Ryan",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 4, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 11, realTimeTracked = true),
                            )
                        )
                    )
                ),
                "84" to RoutePrediction(
                    route = "84",
                    mapOf(
                        "Caldwell/Central" to DestinationPrediction(
                            destination = "Caldwell/Central",
                            arrivalTimes = listOf(
                                ArrivalTime(timeInMinutes = 4, realTimeTracked = true),
                                ArrivalTime(timeInMinutes = 27, realTimeTracked = true)
                            )
                        )
                    )
                )
            )
        )
        assertThat(result).isEqualTo(expectedResponse)
    }

    private fun buildHttpRequest(routes: List<String>): HttpRequest {
        return mockk<HttpRequest> {
            every { queryParameters }.returns(mapOf("routes" to routes))
            every { method }.returns("GET")
        }
    }

    private fun setupBusResponse(stopIds: List<String>, file: String) {
        val sampleResponse = this::class.java.classLoader.getResource(file)!!.readText()
        every {
            httpClient.send(
                match {
                    val uri = it.uri()
                    val parsedUri = URIBuilder(it.uri())
                    uri.path == "/bustime/api/v2/getpredictions"
                            && uri.host == "www.ctabustracker.com"
                            && uri.scheme == "https"
                            && parsedUri.getFirstQueryParam("stpid").value == stopIds.joinToString(",")
                            && parsedUri.getFirstQueryParam("key").value == busTrackerKey
                            && parsedUri.getFirstQueryParam("format").value == "json"
                },
                any<BodyHandler<String>>()
            )
        }.returns(
            mockk<JHttpResponse<String>> {
                every { body() }.returns(sampleResponse)
            }
        )
    }

    private fun setupTrainResponse(stationIds: List<String>, files: List<String>) {
        stationIds
            .zip(files)
            .forEach { (stationId, file) ->
                val sampleResponse = this::class.java.classLoader.getResource(file)!!.readText()

                every {
                    httpClient.send(
                        match {
                            val uri = it.uri()
                            val parsedUri = URIBuilder(it.uri())
                            uri.path == "/api/1.0/ttarrivals.aspx"
                                    && uri.host == "lapi.transitchicago.com"
                                    && uri.scheme == "https"
                                    && parsedUri.getFirstQueryParam("mapid").value == stationId
                                    && parsedUri.getFirstQueryParam("outputType").value == "JSON"
                                    && parsedUri.getFirstQueryParam("key").value == trainTrackerKey
                        },
                        any<BodyHandler<String>>()
                    )
                }.returns(
                    mockk<JHttpResponse<String>> {
                        every { body() }.returns(sampleResponse)
                    }
                )
            }
    }
}