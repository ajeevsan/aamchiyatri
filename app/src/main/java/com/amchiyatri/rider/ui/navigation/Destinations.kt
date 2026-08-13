package com.amchiyatri.rider.ui.navigation

/** Field being edited by the location search screen. */
enum class LocationField { PICKUP, DROP }

object Destinations {
    const val SPLASH = "splash"
    const val LANGUAGE_SELECTION = "language_selection"
    const val PHONE_ENTRY = "phone_entry"
    const val OTP_VERIFICATION = "otp_verification/{phone}"
    fun otpVerification(phone: String) = "otp_verification/$phone"

    const val HOME = "home"
    const val LOCATION_SEARCH = "location_search/{field}"
    fun locationSearch(field: LocationField) = "location_search/${field.name}"

    const val RIDE_OPTIONS = "ride_options"
    const val RIDE_TRACKING = "ride_tracking"
    const val FARE_SUMMARY = "fare_summary"
    const val RATE_DRIVER = "rate_driver"

    const val RIDE_HISTORY = "ride_history"
    const val RIDE_DETAIL = "ride_detail/{rideId}"
    fun rideDetail(rideId: String) = "ride_detail/$rideId"

    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val SAVED_PLACES = "saved_places"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val HELP_SUPPORT = "help_support"

    const val DRIVER_ONBOARDING = "driver_onboarding"
    const val DRIVER_ACTIVE_TRIP = "driver_active_trip"

    /** Top-level destinations that own the bottom navigation bar. */
    val bottomBarRoutes = setOf(HOME, RIDE_HISTORY, PROFILE)
}
