package de.loxitation.pht.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.loxitation.pht.data.local.BloodPressureMeasurementEntity
import de.loxitation.pht.data.repository.MeasurementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MeasurementsViewModel(repository: MeasurementRepository) : ViewModel() {
    val measurements: StateFlow<List<BloodPressureMeasurementEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
