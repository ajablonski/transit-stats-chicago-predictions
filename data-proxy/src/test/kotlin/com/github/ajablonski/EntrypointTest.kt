package com.github.ajablonski

import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.StringReader
import java.io.StringWriter

class EntrypointTest {
    @Test
    fun testResponseFormat() {
        val request = mockk<HttpRequest> {
            every { reader }.returns(BufferedReader(StringReader("FOO")))
        }
        val outputWriter = StringWriter()
        val response = mockk<HttpResponse>(relaxed = true) {
            every { writer }.returns(BufferedWriter(outputWriter))
        }
        Entrypoint().service(request, response)

        response.writer.flush()

        assertThat(outputWriter.toString()).isEqualToIgnoringWhitespace(
            """
            {
            "response": "Hello World!"
            }
        """.trimIndent()
        )
        verify { response.setContentType("application/json") }
    }
}