package com.amchiyatri.rider.di

import com.amchiyatri.rider.data.repository.AuthRepository
import com.amchiyatri.rider.data.repository.DefaultFareRepository
import com.amchiyatri.rider.data.repository.FakeAuthRepository
import com.amchiyatri.rider.data.repository.FakeLocationRepository
import com.amchiyatri.rider.data.repository.FakeProfileRepository
import com.amchiyatri.rider.data.repository.FakeRideRepository
import com.amchiyatri.rider.data.repository.FareRepository
import com.amchiyatri.rider.data.repository.LocationRepository
import com.amchiyatri.rider.data.repository.ProfileRepository
import com.amchiyatri.rider.data.repository.RideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Every binding below points at an in-memory "Fake*" implementation so the app is fully usable
 * offline, with no Maps/OTP/payment API keys. To go live, write a real implementation of the
 * interface (e.g. `RetrofitAuthRepository`, `GoogleMapsLocationRepository`,
 * `RazorpayAwarePaymentFlow`) and change only the `@Binds` line here — nothing in `ui/` changes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: FakeProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: FakeLocationRepository): LocationRepository

    @Binds
    @Singleton
    abstract fun bindFareRepository(impl: DefaultFareRepository): FareRepository

    @Binds
    @Singleton
    abstract fun bindRideRepository(impl: FakeRideRepository): RideRepository
}
