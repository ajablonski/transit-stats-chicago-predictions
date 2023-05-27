package com.github.ajablonski

class Environment {
    fun getEnv(key: String): String? {
        return System.getenv(key)
    }
}
