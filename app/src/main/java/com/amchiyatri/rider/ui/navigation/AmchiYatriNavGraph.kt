package com.amchiyatri.rider.ui.navigation

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amchiyatri.rider.data.model.UserRole
import com.amchiyatri.rider.ui.screens.auth.OtpVerificationScreen
import com.amchiyatri.rider.ui.screens.auth.PhoneEntryScreen
import com.amchiyatri.rider.ui.screens.booking.PaymentMethodScreen
import com.amchiyatri.rider.ui.screens.booking.RideOptionsScreen
import com.amchiyatri.rider.ui.screens.driver.DriverActiveTripScreen
import com.amchiyatri.rider.ui.screens.driver.DriverHomeScreen
import com.amchiyatri.rider.ui.screens.driver.DriverOnboardingScreen
import com.amchiyatri.rider.ui.screens.history.RideDetailScreen
import com.amchiyatri.rider.ui.screens.history.RideHistoryScreen
import com.amchiyatri.rider.ui.screens.home.HomeScreen
import com.amchiyatri.rider.ui.screens.location.LocationSearchScreen
import com.amchiyatri.rider.ui.screens.onboarding.LanguageSelectionScreen
import com.amchiyatri.rider.ui.screens.profile.EditProfileScreen
import com.amchiyatri.rider.ui.screens.profile.EmergencyContactsScreen
import com.amchiyatri.rider.ui.screens.profile.LanguageSettingsScreen
import com.amchiyatri.rider.ui.screens.profile.ProfileScreen
import com.amchiyatri.rider.ui.screens.profile.SavedPlacesScreen
import com.amchiyatri.rider.ui.screens.ride.FareSummaryScreen
import com.amchiyatri.rider.ui.screens.ride.RateDriverScreen
import com.amchiyatri.rider.ui.screens.ride.RideTrackingScreen
import com.amchiyatri.rider.ui.screens.splash.SplashScreen
import com.amchiyatri.rider.ui.screens.support.HelpSupportScreen
import com.amchiyatri.rider.ui.viewmodel.BookingViewModel
import com.amchiyatri.rider.ui.viewmodel.DriverViewModel
import com.amchiyatri.rider.ui.viewmodel.ProfileViewModel
import com.amchiyatri.rider.ui.viewmodel.RideViewModel
import com.amchiyatri.rider.ui.viewmodel.SettingsViewModel

@Composable
fun AmchiYatriNavGraph() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity

    // Scoped to the single Activity (not to each nav-graph entry) so booking/ride/profile state
    // survives moving between screens - the same pattern you'd use for any single-Activity app.
    val bookingViewModel: BookingViewModel = hiltViewModel(activity)
    val rideViewModel: RideViewModel = hiltViewModel(activity)
    val profileViewModel: ProfileViewModel = hiltViewModel(activity)
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val driverViewModel: DriverViewModel = hiltViewModel(activity)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) bookingViewModel.startLocationUpdates() }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun goTo(route: String) = navController.navigate(route) {
        popUpTo(Destinations.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen(
                onNavigateToLanguageSelection = { navController.navigate(Destinations.LANGUAGE_SELECTION) { popUpTo(Destinations.SPLASH) { inclusive = true } } },
                onNavigateToLogin = { navController.navigate(Destinations.PHONE_ENTRY) { popUpTo(Destinations.SPLASH) { inclusive = true } } },
                onNavigateToHome = { navController.navigate(Destinations.HOME) { popUpTo(Destinations.SPLASH) { inclusive = true } } },
            )
        }

        composable(Destinations.LANGUAGE_SELECTION) {
            LanguageSelectionScreen(
                onLanguageConfirmed = {
                    navController.navigate(Destinations.PHONE_ENTRY) { popUpTo(Destinations.LANGUAGE_SELECTION) { inclusive = true } }
                },
            )
        }

        composable(Destinations.PHONE_ENTRY) {
            PhoneEntryScreen(onOtpSent = { phone -> navController.navigate(Destinations.otpVerification(phone)) })
        }

        composable(
            route = Destinations.OTP_VERIFICATION,
            arguments = listOf(navArgument("phone") { type = NavType.StringType }),
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone").orEmpty()
            OtpVerificationScreen(
                phoneNumber = phone,
                onVerified = { navController.navigate(Destinations.HOME) { popUpTo(Destinations.PHONE_ENTRY) { inclusive = true } } },
            )
        }

        composable(Destinations.HOME) {
            // Same bottom-nav position for both roles; which screen renders here depends only on
            // the signed-in account's current mode, so switching roles never needs its own routes.
            val profile by profileViewModel.profile.collectAsState()
            if (profile?.activeRole == UserRole.DRIVER) {
                DriverHomeScreen(
                    onNavigate = ::goTo,
                    onRideAccepted = { navController.navigate(Destinations.DRIVER_ACTIVE_TRIP) },
                    driverViewModel = driverViewModel,
                )
            } else {
                HomeScreen(
                    onNavigate = ::goTo,
                    onOpenLocationSearch = { field -> navController.navigate(Destinations.locationSearch(field)) },
                    onGoToRideOptions = { navController.navigate(Destinations.RIDE_OPTIONS) },
                    bookingViewModel = bookingViewModel,
                )
            }
        }

        composable(Destinations.DRIVER_ONBOARDING) {
            DriverOnboardingScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.navigate(Destinations.HOME) { popUpTo(Destinations.HOME) { inclusive = true } } },
                driverViewModel = driverViewModel,
            )
        }

        composable(Destinations.DRIVER_ACTIVE_TRIP) {
            DriverActiveTripScreen(
                onDone = { navController.navigate(Destinations.HOME) { popUpTo(Destinations.HOME) { inclusive = true } } },
                driverViewModel = driverViewModel,
            )
        }

        composable(
            route = Destinations.LOCATION_SEARCH,
            arguments = listOf(navArgument("field") { type = NavType.StringType }),
        ) { backStackEntry ->
            val field = LocationField.valueOf(backStackEntry.arguments?.getString("field") ?: LocationField.DROP.name)
            LocationSearchScreen(
                field = field,
                onBack = { navController.popBackStack() },
                onPlaceSelected = {
                    navController.popBackStack()
                    if (field == LocationField.DROP && bookingViewModel.uiState.value.pickup != null) {
                        navController.navigate(Destinations.RIDE_OPTIONS)
                    }
                },
                bookingViewModel = bookingViewModel,
            )
        }

        composable(Destinations.RIDE_OPTIONS) {
            RideOptionsScreen(
                onBack = { navController.popBackStack() },
                onChangePaymentMethod = { navController.navigate("payment_method") },
                onRideRequested = {
                    navController.navigate(Destinations.RIDE_TRACKING) { popUpTo(Destinations.HOME) }
                },
                bookingViewModel = bookingViewModel,
            )
        }

        composable("payment_method") {
            PaymentMethodScreen(onBack = { navController.popBackStack() }, bookingViewModel = bookingViewModel)
        }

        composable(Destinations.RIDE_TRACKING) {
            RideTrackingScreen(
                onNoDriverFound = {
                    rideViewModel.clearActiveRide()
                    navController.navigate(Destinations.HOME) { popUpTo(Destinations.HOME) { inclusive = true } }
                },
                onCancelled = {
                    rideViewModel.clearActiveRide()
                    navController.navigate(Destinations.HOME) { popUpTo(Destinations.HOME) { inclusive = true } }
                },
                onCompleted = {
                    navController.navigate(Destinations.FARE_SUMMARY) { popUpTo(Destinations.RIDE_TRACKING) { inclusive = true } }
                },
                onGiveUp = {
                    rideViewModel.clearActiveRide()
                    navController.navigate(Destinations.HOME) { popUpTo(Destinations.HOME) { inclusive = true } }
                },
                rideViewModel = rideViewModel,
            )
        }

        composable(Destinations.FARE_SUMMARY) {
            FareSummaryScreen(
                onContinueToRating = { navController.navigate(Destinations.RATE_DRIVER) { popUpTo(Destinations.FARE_SUMMARY) { inclusive = true } } },
                rideViewModel = rideViewModel,
            )
        }

        composable(Destinations.RATE_DRIVER) {
            RateDriverScreen(
                onDone = {
                    rideViewModel.clearActiveRide()
                    navController.navigate(Destinations.HOME) { popUpTo(Destinations.HOME) { inclusive = true } }
                },
                rideViewModel = rideViewModel,
            )
        }

        composable(Destinations.RIDE_HISTORY) {
            RideHistoryScreen(
                onNavigate = ::goTo,
                onOpenRide = { rideId -> navController.navigate(Destinations.rideDetail(rideId)) },
                rideViewModel = rideViewModel,
            )
        }

        composable(
            route = Destinations.RIDE_DETAIL,
            arguments = listOf(navArgument("rideId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId").orEmpty()
            RideDetailScreen(rideId = rideId, onBack = { navController.popBackStack() }, rideViewModel = rideViewModel)
        }

        composable(Destinations.PROFILE) {
            ProfileScreen(
                onNavigate = ::goTo,
                onOpenEditProfile = { navController.navigate(Destinations.EDIT_PROFILE) },
                onOpenEmergencyContacts = { navController.navigate(Destinations.EMERGENCY_CONTACTS) },
                onOpenSavedPlaces = { navController.navigate(Destinations.SAVED_PLACES) },
                onOpenLanguageSettings = { navController.navigate(Destinations.LANGUAGE_SETTINGS) },
                onOpenHelp = { navController.navigate(Destinations.HELP_SUPPORT) },
                onOpenDriverOnboarding = { navController.navigate(Destinations.DRIVER_ONBOARDING) },
                onLoggedOut = {
                    navController.navigate(Destinations.PHONE_ENTRY) { popUpTo(0) { inclusive = true } }
                },
                profileViewModel = profileViewModel,
                driverViewModel = driverViewModel,
            )
        }

        composable(Destinations.EDIT_PROFILE) {
            EditProfileScreen(onBack = { navController.popBackStack() }, profileViewModel = profileViewModel)
        }

        composable(Destinations.EMERGENCY_CONTACTS) {
            EmergencyContactsScreen(onBack = { navController.popBackStack() }, profileViewModel = profileViewModel)
        }

        composable(Destinations.SAVED_PLACES) {
            SavedPlacesScreen(onBack = { navController.popBackStack() }, profileViewModel = profileViewModel)
        }

        composable(Destinations.LANGUAGE_SETTINGS) {
            LanguageSettingsScreen(onBack = { navController.popBackStack() }, settingsViewModel = settingsViewModel)
        }

        composable(Destinations.HELP_SUPPORT) {
            HelpSupportScreen(onBack = { navController.popBackStack() })
        }
    }
}
