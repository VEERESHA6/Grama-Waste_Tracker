package com.example.gwt.viewmodel

import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gwt.model.BlackspotReport
import com.example.gwt.model.TractorLocation
import com.example.gwt.repository.TractorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(private val repository: TractorRepository = TractorRepository()) : ViewModel() {
    private val _tractorLocation = MutableStateFlow<TractorLocation?>(null)
    val tractorLocation: StateFlow<TractorLocation?> = _tractorLocation

    private val _distanceToTractor = MutableStateFlow<Float?>(null)
    val distanceToTractor: StateFlow<Float?> = _distanceToTractor

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _blackspots = MutableStateFlow<List<BlackspotReport>>(emptyList())
    val blackspots: StateFlow<List<BlackspotReport>> = _blackspots

    init {
        observeTractorLocation("Tractor_001")
        observeBlackspots()
    }

    private fun observeTractorLocation(tractorId: String) {
        viewModelScope.launch {
            repository.getTractorLocation(tractorId).collect { location ->
                _tractorLocation.value = location
                calculateDistance()
            }
        }
    }

    private fun observeBlackspots() {
        viewModelScope.launch {
            repository.getAllBlackspots().collect {
                _blackspots.value = it
            }
        }
    }

    private var userLocation: Location? = null

    fun updateUserLocation(location: Location) {
        userLocation = location
        calculateDistance()
    }

    private fun calculateDistance() {
        val tractor = _tractorLocation.value
        val user = userLocation
        if (tractor != null && user != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                user.latitude, user.longitude,
                tractor.latitude, tractor.longitude,
                results
            )
            val distance = results[0]
            _distanceToTractor.value = distance
        }
    }

    fun reportBlackspot(imageUri: Uri, description: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val user = userLocation ?: return
        viewModelScope.launch {
            _isUploading.value = true
            val success = repository.uploadBlackspotReport(imageUri, user.latitude, user.longitude, description)
            _isUploading.value = false
            if (success) onSuccess() else onError()
        }
    }

    fun startSharingLocation(tractorId: String, lat: Double, lng: Double) {
        val newLocation = TractorLocation(
            latitude = lat,
            longitude = lng,
            lastUpdated = System.currentTimeMillis(),
            tractorId = tractorId
        )
        repository.updateTractorLocation(newLocation)
    }
}
