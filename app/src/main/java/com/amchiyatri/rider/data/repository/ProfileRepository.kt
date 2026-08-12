package com.amchiyatri.rider.data.repository

import com.amchiyatri.rider.data.model.EmergencyContact
import com.amchiyatri.rider.data.model.Gender
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.SavedPlace
import com.amchiyatri.rider.data.model.SavedPlaceLabel
import com.amchiyatri.rider.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the logged-in rider: basic info, emergency contacts (used by the
 * in-ride SOS/share-trip feature) and saved places (Home/Work quick-fill).
 *
 * Swap [FakeProfileRepository] for a real implementation backed by your user-profile API once
 * you have one; the interface is deliberately backend-agnostic.
 */
interface ProfileRepository {
    val profile: StateFlow<UserProfile?>

    /** Called by [AuthRepository] right after OTP verification succeeds. */
    fun onLoggedIn(phoneNumber: String)

    fun clear()

    suspend fun updateBasicInfo(name: String, email: String?, gender: Gender?)

    suspend fun addEmergencyContact(name: String, phoneNumber: String)
    suspend fun removeEmergencyContact(contactId: String)

    suspend fun addSavedPlace(label: SavedPlaceLabel, customName: String?, address: String, point: GeoPoint)
    suspend fun removeSavedPlace(placeId: String)
}

@Singleton
class FakeProfileRepository @Inject constructor() : ProfileRepository {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    override val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    override fun onLoggedIn(phoneNumber: String) {
        _profile.value = UserProfile(
            name = "Mumbai Rider",
            phoneNumber = phoneNumber,
        )
    }

    override fun clear() {
        _profile.value = null
    }

    override suspend fun updateBasicInfo(name: String, email: String?, gender: Gender?) {
        _profile.update { it?.copy(name = name, email = email, gender = gender) }
    }

    override suspend fun addEmergencyContact(name: String, phoneNumber: String) {
        _profile.update { profile ->
            profile?.copy(
                emergencyContacts = profile.emergencyContacts + EmergencyContact(name = name, phoneNumber = phoneNumber)
            )
        }
    }

    override suspend fun removeEmergencyContact(contactId: String) {
        _profile.update { profile ->
            profile?.copy(emergencyContacts = profile.emergencyContacts.filterNot { it.id == contactId })
        }
    }

    override suspend fun addSavedPlace(label: SavedPlaceLabel, customName: String?, address: String, point: GeoPoint) {
        _profile.update { profile ->
            val withoutSameLabel = if (label != SavedPlaceLabel.OTHER) {
                profile?.savedPlaces?.filterNot { it.label == label } ?: emptyList()
            } else {
                profile?.savedPlaces ?: emptyList()
            }
            profile?.copy(
                savedPlaces = withoutSameLabel + SavedPlace(
                    label = label,
                    customName = customName,
                    address = address,
                    point = point,
                )
            )
        }
    }

    override suspend fun removeSavedPlace(placeId: String) {
        _profile.update { profile ->
            profile?.copy(savedPlaces = profile.savedPlaces.filterNot { it.id == placeId })
        }
    }
}
