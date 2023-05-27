package com.github.ajablonski

interface KeyProvider {
    fun getBusTrackerApiKey(): String

    fun getTrainTrackerApiKey(): String
}


