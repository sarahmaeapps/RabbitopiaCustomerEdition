package com.sarahmaeapps.rabbitopiacompanion.data.model

import com.google.firebase.firestore.PropertyName

data class Purchase(
    val id: String = "",
    val itemName: String = "",
    val rabbitId: String = "",
    val customerId: String = "",
    val date: Long = 0L,
    var amount: Double = 0.0,
    val details: String = "",
    val isLocal: Boolean = false
)
