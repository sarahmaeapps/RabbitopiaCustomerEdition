package com.sarahmaeapps.rabbitopiacompanion.data.model

import com.google.firebase.firestore.PropertyName

data class Rabbit(
    val id: String = "",
    val name: String = "",
    @get:PropertyName("earTattoo") @set:PropertyName("earTattoo") var earTattooId: String = "",
    var purchaseDate: String = "",
    @get:PropertyName("dateOfBirth") @set:PropertyName("dateOfBirth") var birthDate: String? = null,
    @get:PropertyName("imagePath") @set:PropertyName("imagePath") var pictureUrl: String? = null,
    @get:PropertyName("forSale") @set:PropertyName("forSale") var isForSale: Boolean = false,
    @get:PropertyName("salePrice") @set:PropertyName("salePrice") var price: Double = 0.0,
    val estWeaningDate: String = "",
    val kitsInLitter: Int = 0,
    @get:PropertyName("arbaScore") @set:PropertyName("arbaScore") var arbaScore: String = "",
    @get:PropertyName("grade") @set:PropertyName("grade") var grade: String = "",
    @get:PropertyName("breed") @set:PropertyName("breed") var breed: String = "",
    @get:PropertyName("color") @set:PropertyName("color") var color: String = "",
    @get:PropertyName("sex") @set:PropertyName("sex") var sex: String = "",
    @get:PropertyName("hutchId") @set:PropertyName("hutchId") var hutchId: String = "",
    @get:PropertyName("genCount") @set:PropertyName("genCount") var generation: Int = 1,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "",
    @get:PropertyName("source") @set:PropertyName("source") var source: String = "",
    @get:PropertyName("notes") @set:PropertyName("notes") var notes: String = "",
    var sopScore: String = "",
    val sopEvaluations: List<SopEvaluation> = emptyList(),
    val weighIns: List<WeighIn> = emptyList(),
    val medicalRecords: List<MedicalRecord> = emptyList(),
    val pedigree: Pedigree? = null
)

data class WeighIn(val date: String = "", val weight: String = "")

data class MedicalRecord(val date: String = "", val note: String = "")

data class SopEvaluation(
    val date: String = "",
    val score: String = "",
    val checklist: Map<String, String> = emptyMap()
)

data class Pedigree(
    val father: RabbitRelative? = null,
    val mother: RabbitRelative? = null
)

data class RabbitRelative(
    val name: String = "",
    val father: RabbitRelative? = null,
    val mother: RabbitRelative? = null
)
