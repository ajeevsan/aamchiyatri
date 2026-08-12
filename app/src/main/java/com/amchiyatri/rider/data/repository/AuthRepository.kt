package com.amchiyatri.rider.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone-number + OTP login, mirroring how Namma Yatri and most Indian ride apps onboard riders.
 *
 * This is the seam to swap for a real backend: implement the same interface against your
 * SMS-OTP provider (e.g. Firebase Auth phone sign-in, MSG91, 2Factor) and your user service,
 * then re-bind it in [com.amchiyatri.rider.di.RepositoryModule] instead of [FakeAuthRepository].
 * On successful verification, the profile itself is created/loaded through [ProfileRepository]
 * so there's a single source of truth for who the rider is.
 */
interface AuthRepository {
    val isLoggedIn: StateFlow<Boolean>

    /** Sends a one-time password to [phoneNumber]. Returns failure if the number looks invalid. */
    suspend fun sendOtp(phoneNumber: String): Result<Unit>

    /** Verifies [otp] for [phoneNumber]. On success the rider is considered logged in. */
    suspend fun verifyOtp(phoneNumber: String, otp: String): Result<Unit>

    suspend fun resendOtp(phoneNumber: String): Result<Unit>

    fun logout()
}

@Singleton
class FakeAuthRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // A fixed demo OTP so the flow is fully testable without a real SMS gateway.
    private val demoOtp = "1234"

    override suspend fun sendOtp(phoneNumber: String): Result<Unit> {
        delay(700)
        return if (phoneNumber.filter { it.isDigit() }.length == 10) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number"))
        }
    }

    override suspend fun resendOtp(phoneNumber: String): Result<Unit> = sendOtp(phoneNumber)

    override suspend fun verifyOtp(phoneNumber: String, otp: String): Result<Unit> {
        delay(700)
        return if (otp == demoOtp) {
            profileRepository.onLoggedIn(phoneNumber)
            _isLoggedIn.value = true
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Incorrect OTP. Try $demoOtp for this demo build."))
        }
    }

    override fun logout() {
        _isLoggedIn.value = false
        profileRepository.clear()
    }
}
