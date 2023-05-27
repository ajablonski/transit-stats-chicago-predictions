package com.github.ajablonski

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.reflect.full.memberProperties

class FileSecretKeyProviderTest {
    @Test
    fun shouldReadKeysFromProvidedFile() {
        val secretProvider = FileSecretKeyProvider(secretPath = Path(this.javaClass.classLoader.getResource("testsecrets.json")!!.path))

        assertThat(secretProvider.getBusTrackerApiKey()).isEqualTo("fakeBusTrackerApiKey")
        assertThat(secretProvider.getTrainTrackerApiKey()).isEqualTo("fakeTrainTrackerApiKey")
    }

    @Test
    fun shouldReadTrainTrackerKeyFromEnvironmentVariable() {
        val environment = mockk<Environment> {
            every { getEnv("SECRET_PATH") }.returns(this.javaClass.classLoader.getResource("testsecrets.json")!!.path)
        }
        val secretProvider = FileSecretKeyProvider(environment)

        assertThat(secretProvider.getBusTrackerApiKey()).isEqualTo("fakeBusTrackerApiKey")
        assertThat(secretProvider.getTrainTrackerApiKey()).isEqualTo("fakeTrainTrackerApiKey")
    }

    @Test
    fun shouldDefaultToEtcPath() {
        val secretProvider = FileSecretKeyProvider()

        val declaredField = secretProvider::class.java.getDeclaredField("secretPath")
        declaredField.trySetAccessible()
        assertThat(declaredField.get(secretProvider)).isEqualTo(Constants.defaultSecretPath)
    }
}