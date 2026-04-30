package com.sarahmaeapps.rabbitopiacompanion.data.model

import com.google.firebase.firestore.PropertyName

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    var text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L
)
