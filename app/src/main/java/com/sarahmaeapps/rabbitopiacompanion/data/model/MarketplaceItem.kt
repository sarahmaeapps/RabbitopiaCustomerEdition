package com.sarahmaeapps.rabbitopiacompanion.data.model

data class MarketplaceItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val category: String = ""
)
