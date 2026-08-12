package com.amchiyatri.rider.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Phone-number + OTP login, mirroring how Namma Yatri and most Indian ride apps onboard riders.
 *
 * [FirebaseAuthRepository] is the real implementation, using Firebase Phone Auth (needs
 * app/google-services.json and Phone sign-in enabled in the Firebase console - see SETUP.md).
 * It needs an [Activity] to show Google Play Services' SafetyNet/reCAPTCHA fallback UI if silent
 * SMS-retrieval isn't available on the device, which is why [sendOtp]/[resendOtp] take one.
 *
 * [FakeAuthRepository] remains for offline dev - the demo OTP is always `1234`.
 */
interface AuthRepository {
    val isLoggedIn: StateFlow<Boolean>

    /** Sends a one-time password to [phoneNumber]. Returns failure if the number looks invalid. */
    suspend fun sendOtp(phoneNumber: String, activity: Activity): Result<Unit>

    /** Verifies [otp] for [phoneNumber]. On success the rider is considered logged in. */
    suspend fun verifyOtp(phoneNumber: String, otp: String): Result<Unit>

    suspend fun resendOtp(phoneNumber: String, activity: Activity): Result<Unit>

    fun logout()
}

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val profileRepository: ProfileRepository,
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        firebaseAuth.currentUser?.let { onAuthenticated(it) }
    }

    override suspend fun sendOtp(phoneNumber: String, activity: Activity): Result<Unit> =
        requestVerification(phoneNumber, activity, forceResend = false)

    override suspend fun resendOtp(phoneNumber: String, activity: Activity): Result<Unit> =
        requestVerification(phoneNumber, activity, forceResend = true)

    private suspend fun requestVerification(
        phoneNumber: String,
        activity: Activity,
        forceResend: Boolean,
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Play Services silently auto-retrieved the SMS - sign in right away without
                // waiting for the rider to type anything. The OTP screen watches [isLoggedIn]
                // and moves on by itself when this fires.
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) task.result?.user?.let { onAuthenticated(it) }
                }
                if (cont.isActive) cont.resume(Result.success(Unit))
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                if (cont.isActive) cont.resume(Result.failure(exception))
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                resendToken = token
                if (cont.isActive) cont.resume(Result.success(Unit))
            }
        }

        val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResend) {
            resendToken?.let { builder.setForceResendingToken(it) }
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    override suspend fun verifyOtp(phoneNumber: String, otp: String): Result<Unit> {
        val id = verificationId
            ?: return Result.failure(IllegalStateException("Request an OTP before verifying it"))
        return try {
            val credential = PhoneAuthProvider.getCredential(id, otp)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let { onAuthenticated(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
        _isLoggedIn.value = false
        profileRepository.clear()
    }

    private fun onAuthenticated(user: FirebaseUser) {
        profileRepository.onLoggedIn(user.uid, user.phoneNumber.orEmpty().removePrefix("+91"))
        _isLoggedIn.value = true
    }
}

@Singleton
class FakeAuthRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // A fixed demo OTP so the flow is fully testable without a real SMS gateway.
    private val demoOtp = "1234"
    private var pendingPhoneNumber: String? = null

    override suspend fun sendOtp(phoneNumber: String, activity: Activity): Result<Unit> {
        delay(700)
        return if (phoneNumber.filter { it.isDigit() }.length == 10) {
            pendingPhoneNumber = phoneNumber
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number"))
        }
    }

    override suspend fun resendOtp(phoneNumber: String, activity: Activity): Result<Unit> = sendOtp(phoneNumber, activity)

    override suspend fun verifyOtp(phoneNumber: String, otp: String): Result<Unit> {
        delay(700)
        return if (otp == demoOtp) {
            profileRepository.onLoggedIn(uid = "fake-uid-$phoneNumber", phoneNumber = phoneNumber)
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
