package com.github.ajablonski

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse

class Entrypoint : HttpFunction {
    override fun service(request: HttpRequest?, response: HttpResponse?) {
        response?.run {
            setContentType("application/json")
            setStatusCode(200)
            writer.write("""
                {
                    "response": "Hello World!"
                }
            """.trimIndent())
        }
    }
}