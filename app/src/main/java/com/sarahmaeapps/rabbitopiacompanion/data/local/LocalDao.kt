package com.sarahmaeapps.rabbitopiacompanion.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDao {
    // Housing
    @Query("SELECT * FROM housing")
    fun getAllHousing(): Flow<List<HousingEntity>>

    @Query("SELECT * FROM housing WHERE hutchId = :hutchId")
    suspend fun getHousingById(hutchId: String): HousingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousing(housing: HousingEntity): Long

    // Feed
    @Query("SELECT * FROM feed_entries ORDER BY date DESC")
    fun getAllFeedEntries(): Flow<List<FeedEntry>>

    @Insert
    suspend fun insertFeedEntry(entry: FeedEntry): Long

    // Medical
    @Query("SELECT * FROM medical_care WHERE rabbitId = :rabbitId ORDER BY date DESC")
    fun getMedicalCareForRabbit(rabbitId: String): Flow<List<MedicalCareEntry>>

    @Query("SELECT * FROM medical_care ORDER BY date DESC")
    fun getAllMedicalCare(): Flow<List<MedicalCareEntry>>

    @Insert
    suspend fun insertMedicalCare(entry: MedicalCareEntry): Long
}
