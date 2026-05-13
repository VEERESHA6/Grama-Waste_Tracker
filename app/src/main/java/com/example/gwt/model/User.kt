package com.example.gwt.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "Resident", // "Resident" or "Driver"
    val tractorId: String? = null // Only for Drivers
)
