package com.sarahmaeapps.rabbitopiacompanion.data.repository

import com.sarahmaeapps.rabbitopiacompanion.data.local.*
import kotlinx.coroutines.flow.Flow

class LocalRepository(private val localDao: LocalDao) {
    val allHousing: Flow<List<HousingEntity>> = localDao.getAllHousing()
    val allFeedEntries: Flow<List<FeedEntry>> = localDao.getAllFeedEntries()
    val allMedicalCare: Flow<List<MedicalCareEntry>> = localDao.getAllMedicalCare()

    suspend fun getHousingById(hutchId: String) = localDao.getHousingById(hutchId)
    suspend fun insertHousing(housing: HousingEntity) = localDao.insertHousing(housing)
    suspend fun insertFeedEntry(entry: FeedEntry) = localDao.insertFeedEntry(entry)
    suspend fun insertMedicalCare(entry: MedicalCareEntry) = localDao.insertMedicalCare(entry)
    fun getMedicalCareForRabbit(rabbitId: String) = localDao.getMedicalCareForRabbit(rabbitId)
}
