package com.github.ajablonski

import com.github.ajablonski.model.Secrets
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText

class FileSecretKeyProvider(private val environmentProvider: Environment = Environment(),
    private val secretPath: Path =
        environmentProvider.getEnv("SECRET_PATH")?.let { Path.of(it) } ?: Constants.defaultSecretPath) : KeyProvider {
    override fun getBusTrackerApiKey(): String {
        return Json.decodeFromString<Secrets>(secretPath.readText())
            .busTrackerApiKey
    }

    override fun getTrainTrackerApiKey(): String {
        return Json.decodeFromString<Secrets>(secretPath.readText())
            .trainTrackerApiKey
    }
}
