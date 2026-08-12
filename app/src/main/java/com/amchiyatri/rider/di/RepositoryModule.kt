package com.amchiyatri.rider.di

import com.amchiyatri.rider.data.repository.AuthRepository
import com.amchiyatri.rider.data.repository.DirectionsFareRepository
import com.amchiyatri.rider.data.repository.FareRepository
import com.amchiyatri.rider.data.repository.FirebaseAuthRepository
import com.amchiyatri.rider.data.repository.FirestoreProfileRepository
import com.amchiyatri.rider.data.repository.FirestoreRideRepository
import com.amchiyatri.rider.data.repository.GoogleLocationRepository
import com.amchiyatri.rider.data.repository.LocationRepository
import com.amchiyatri.rider.data.repository.PaymentRepository
import com.amchiyatri.rider.data.repository.ProfileRepository
import com.amchiyatri.rider.data.repository.RazorpayPaymentRepository
import com.amchiyatri.rider.data.repository.RideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Every binding below points at the real, network-backed implementation: Firebase Phone Auth,
 * Firestore (profile + ride dispatch), Google Maps/Places/Directions, and Razorpay via Cloud
 * Functions. This needs app/google-services.json and app/secrets.properties in place - see
 * SETUP.md - otherwise the app will crash on launch trying to reach a Firebase project that
 * doesn't exist yet.
 *
 * Each interface also still has a `Fake*` in-memory implementation (see the matching repository
 * file) for offline development. To fall back to those temporarily, swap the `impl` type on the
 * `@Binds` method below - nothing in `ui/` changes either way.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: FirestoreProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: GoogleLocationRepository): LocationRepository

    @Binds
    @Singleton
    abstract fun bindFareRepository(impl: DirectionsFareRepository): FareRepository

    @Binds
    @Singleton
    abstract fun bindRideRepository(impl: FirestoreRideRepository): RideRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: RazorpayPaymentRepository): PaymentRepository
}
