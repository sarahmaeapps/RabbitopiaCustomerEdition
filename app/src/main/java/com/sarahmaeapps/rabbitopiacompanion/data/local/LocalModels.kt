package com.sarahmaeapps.rabbitopiacompanion.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "housing")
data class HousingEntity(
    @PrimaryKey val hutchId: String, // e.g. "A1"
    val condition: String = "Average",
    val signsOfPredators: Boolean = false,
    val upgrades: String = "",
    val residents: List<String> = emptyList(),
    val repairHistory: List<RepairEntry> = emptyList()
)

data class RepairEntry(
    val date: Long,
    val task: String
)

@Entity(tableName = "feed_entries")
data class FeedEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val supplier: String,
    val price: Double,
    val lbs: Double,
    val prot: Double,
    val fib: Double,
    val fat: Double,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "medical_care")
data class MedicalCareEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rabbitId: String,
    val date: Long,
    val treatment: String,
    val cost: Double,
    val medications: String = "",
    val vetNotes: String = ""
)

class Converters {
    private val gson = Gson()

    @androidx.room.TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value)

    @androidx.room.TypeConverter
    fun toStringList(value: String): List<String> = gson.fromJson(value, object : TypeToken<List<String>>() {}.type)

    @androidx.room.TypeConverter
    fun fromRepairList(value: List<RepairEntry>?): String = gson.toJson(value)

    @androidx.room.TypeConverter
    fun toRepairList(value: String): List<RepairEntry> = gson.fromJson(value, object : TypeToken<List<RepairEntry>>() {}.type)
}
