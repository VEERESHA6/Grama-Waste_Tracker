package com.example.gwt.model

data class BlackspotReport(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val reporterId: String = "User_Anonymous",
    val status: String = "Pending", // "Pending", "In Progress", "Cleaned"
    val adminComment: String = ""
)
