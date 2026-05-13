package com.example.gwt.model

data class TractorLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val tractorId: String = "Tractor_001"
)
