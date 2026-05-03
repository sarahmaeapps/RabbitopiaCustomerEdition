package com.sarahmaeapps.rabbitopiacompanion

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.sarahmaeapps.rabbitopiacompanion.ui.navigation.Screen
import com.sarahmaeapps.rabbitopiacompanion.ui.screens.*
import com.sarahmaeapps.rabbitopiacompanion.ui.theme.RabbitopiaCompanionTheme
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.RabbitViewModel
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.MessagingViewModel
import com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel.LocalViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RabbitopiaCompanionTheme {
                RabbitopiaApp()
            }
        }
    }
}

@Composable
fun RequestPermissions() {
    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionsToRequest)
    }
}

@Composable
fun RabbitopiaApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val rabbitViewModel: RabbitViewModel = viewModel()
    val localViewModel: LocalViewModel = viewModel()
    
    // Request Permissions on load
    RequestPermissions()
    
    // Check if user is already logged in
    val startDestination = if (auth.currentUser != null) Screen.Main.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Main.route) {
            MainScreen(
                onMyRabbitsClick = { navController.navigate(Screen.MyRabbits.route) },
                onMyPurchasesClick = { navController.navigate(Screen.MyPurchases.route) },
                onMessagingClick = { navController.navigate(Screen.Messaging.route) },
                onForSaleClick = { navController.navigate(Screen.ForSale.route) },
                onWishListClick = { navController.navigate(Screen.WishList.route) }
            )
        }
        composable(Screen.MyRabbits.route) {
            MyRabbitsScreen(
                onBackClick = { navController.popBackStack() },
                onRabbitClick = { rabbitId ->
                    navController.navigate(Screen.RabbitDetail.createRoute(rabbitId, "my_rabbits"))
                },
                onHousingClick = { navController.navigate(Screen.Housing.route) },
                onFeedClick = { navController.navigate(Screen.Feed.route) },
                onMedicalHealthClick = { navController.navigate(Screen.MedicalHealth.route) },
                viewModel = rabbitViewModel
            )
        }
        composable(Screen.RabbitDetail.route) { backStackEntry ->
            val rabbitId = backStackEntry.arguments?.getString("rabbitId") ?: ""
            val source = backStackEntry.arguments?.getString("source") ?: ""
            RabbitDetailScreen(
                rabbitId = rabbitId,
                onBackClick = { navController.popBackStack() },
                onWeighInsClick = { navController.navigate(Screen.WeighIns.createRoute(rabbitId)) },
                onMedicalClick = { navController.navigate(Screen.MedicalRecords.createRoute(rabbitId)) },
                onContactClick = { navController.navigate(Screen.Messaging.route) },
                onEvaluationHistoryClick = { id -> navController.navigate(Screen.SopHistory.createRoute(id)) },
                viewModel = rabbitViewModel,
                isFromMyRabbits = source == "my_rabbits"
            )
        }
        composable(Screen.WeighIns.route) {
            val rabbit = rabbitViewModel.selectedRabbit.collectAsState().value
            RabbitDataListScreen(
                title = "Weigh-In's",
                data = rabbit?.weighIns?.map { it.date to it.weight } ?: emptyList(),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.MedicalRecords.route) {
            val rabbit = rabbitViewModel.selectedRabbit.collectAsState().value
            RabbitDataListScreen(
                title = "Medical Records",
                data = rabbit?.medicalRecords?.map { it.date to it.note } ?: emptyList(),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.MyPurchases.route) {
            MyPurchasesScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Messaging.route) {
            MessagingScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.ForSale.route) {
            ForSaleScreen(
                onBackClick = { navController.popBackStack() },
                onRabbitClick = { rabbitId ->
                    navController.navigate(Screen.RabbitDetail.createRoute(rabbitId, "for_sale"))
                },
                viewModel = rabbitViewModel
            )
        }
        composable(Screen.WishList.route) {
            WishListScreen(
                onBackClick = { navController.popBackStack() },
                onRabbitClick = { rabbitId ->
                    navController.navigate(Screen.RabbitDetail.createRoute(rabbitId, "wishlist"))
                },
                viewModel = rabbitViewModel
            )
        }
        composable(Screen.SopHistory.route) {
            val rabbit = rabbitViewModel.selectedRabbit.collectAsState().value
            RabbitDataListScreen(
                title = "Evaluation History",
                data = rabbit?.sopEvaluations?.map { it.date to it.score } ?: emptyList(),
                onBackClick = { navController.popBackStack() },
                onItemClick = { index -> 
                    val date = rabbit?.sopEvaluations?.getOrNull(index)?.date ?: ""
                    navController.navigate(Screen.SopDetail.createRoute(rabbit?.id ?: "", date))
                }
            )
        }
        composable(Screen.SopDetail.route) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("evaluationDate") ?: ""
            val rabbit = rabbitViewModel.selectedRabbit.collectAsState().value
            val evaluation = rabbit?.sopEvaluations?.find { it.date == date }
            
            RabbitDataListScreen(
                title = "Evaluation Details ($date)",
                data = evaluation?.checklist?.toList() ?: emptyList(),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Housing.route) {
            HousingScreen(
                onBackClick = { navController.popBackStack() },
                onHousingClick = { hutchId -> navController.navigate(Screen.HousingDetail.createRoute(hutchId)) },
                viewModel = localViewModel
            )
        }
        composable(Screen.HousingDetail.route) { backStackEntry ->
            val hutchId = backStackEntry.arguments?.getString("hutchId") ?: ""
            HousingDetailScreen(
                hutchId = hutchId,
                onBackClick = { navController.popBackStack() },
                viewModel = localViewModel
            )
        }
        composable(Screen.Feed.route) {
            FeedScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = localViewModel
            )
        }
        composable(Screen.MedicalHealth.route) {
            MedicalHealthScreen(
                onBackClick = { navController.popBackStack() },
                localViewModel = localViewModel,
                rabbitViewModel = rabbitViewModel
            )
        }
    }
}
