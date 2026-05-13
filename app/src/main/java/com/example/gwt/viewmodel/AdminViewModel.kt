package com.example.gwt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gwt.model.BlackspotReport
import com.example.gwt.repository.TractorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: TractorRepository = TractorRepository()) : ViewModel() {
    private val _blackspots = MutableStateFlow<List<BlackspotReport>>(emptyList())
    val blackspots: StateFlow<List<BlackspotReport>> = _blackspots

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    init {
        fetchBlackspots()
    }

    private fun fetchBlackspots() {
        viewModelScope.launch {
            repository.getAllBlackspots().collect {
                _blackspots.value = it.sortedByDescending { report -> report.timestamp }
            }
        }
    }

    fun updateReportStatus(reportId: String, status: String, comment: String) {
        viewModelScope.launch {
            _isUpdating.value = true
            repository.updateBlackspotStatus(reportId, status, comment)
            _isUpdating.value = false
        }
    }
}
