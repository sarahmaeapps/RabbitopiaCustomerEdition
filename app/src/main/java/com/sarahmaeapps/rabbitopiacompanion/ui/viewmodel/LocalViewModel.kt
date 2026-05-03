package com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarahmaeapps.rabbitopiacompanion.data.local.*
import com.sarahmaeapps.rabbitopiacompanion.data.repository.LocalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LocalRepository

    val allHousing: StateFlow<List<HousingEntity>>
    val allFeedEntries: StateFlow<List<FeedEntry>>
    val allMedicalCare: StateFlow<List<MedicalCareEntry>>

    init {
        val localDao = LocalDatabase.getDatabase(application).localDao()
        repository = LocalRepository(localDao)
        allHousing = repository.allHousing.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allFeedEntries = repository.allFeedEntries.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allMedicalCare = repository.allMedicalCare.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun insertHousing(housing: HousingEntity) = viewModelScope.launch {
        repository.insertHousing(housing)
    }

    fun insertFeedEntry(entry: FeedEntry) = viewModelScope.launch {
        repository.insertFeedEntry(entry)
    }

    fun insertMedicalCare(entry: MedicalCareEntry) = viewModelScope.launch {
        repository.insertMedicalCare(entry)
    }
}
