package com.sarahmaeapps.rabbitopiacompanion.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object MyRabbits : Screen("my_rabbits")
    object RabbitDetail : Screen("rabbit_detail/{rabbitId}/{source}") {
        fun createRoute(rabbitId: String, source: String = "general") = "rabbit_detail/$rabbitId/$source"
    }
    object MyPurchases : Screen("my_purchases")
    object Messaging : Screen("messaging")
    object ForSale : Screen("for_sale")
    object WishList : Screen("wish_list")
    object WeighIns : Screen("weigh_ins/{rabbitId}") {
        fun createRoute(rabbitId: String) = "weigh_ins/$rabbitId"
    }
    object MedicalRecords : Screen("medical_records/{rabbitId}") {
        fun createRoute(rabbitId: String) = "medical_records/$rabbitId"
    }
    object SopHistory : Screen("sop_history/{rabbitId}") {
        fun createRoute(rabbitId: String) = "sop_history/$rabbitId"
    }
    object SopDetail : Screen("sop_detail/{rabbitId}/{evaluationDate}") {
        fun createRoute(rabbitId: String, evaluationDate: String) = "sop_detail/$rabbitId/$evaluationDate"
    }
    object Housing : Screen("housing")
    object HousingDetail : Screen("housing_detail/{hutchId}") {
        fun createRoute(hutchId: String) = "housing_detail/$hutchId"
    }
    object Feed : Screen("feed")
    object MedicalHealth : Screen("medical_health")
}
