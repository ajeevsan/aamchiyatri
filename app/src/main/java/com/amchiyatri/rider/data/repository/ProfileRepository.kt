package com.amchiyatri.rider.data.repository

import com.amchiyatri.rider.data.model.EmergencyContact
import com.amchiyatri.rider.data.model.Gender
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.SavedPlace
import com.amchiyatri.rider.data.model.SavedPlaceLabel
import com.amchiyatri.rider.data.model.UserProfile
import com.amchiyatri.rider.data.remote.fromFirestore
import com.amchiyatri.rider.data.remote.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the logged-in rider: basic info, emergency contacts (used by the
 * in-ride SOS/share-trip feature) and saved places (Home/Work quick-fill).
 *
 * [FirestoreProfileRepository] persists this to `users/{uid}` in Firestore, live-updated via a
 * snapshot listener - so profile edits made on one device show up on another. [FakeProfileRepository]
 * keeps everything in memory for offline dev - see SETUP.md.
 */
interface ProfileRepository {
    val profile: StateFlow<UserProfile?>

    /** Called by [AuthRepository] right after OTP verification succeeds. */
    fun onLoggedIn(uid: String, phoneNumber: String)

    fun clear()

    suspend fun updateBasicInfo(name: String, email: String?, gender: Gender?)

    suspend fun addEmergencyContact(name: String, phoneNumber: String)
    suspend fun removeEmergencyContact(contactId: String)

    suspend fun addSavedPlace(label: SavedPlaceLabel, customName: String?, address: String, point: GeoPoint)
    suspend fun removeSavedPlace(placeId: String)
}

@Singleton
class FirestoreProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ProfileRepository {

    private val repoScope = CoroutineScope(SupervisorJob())
    private val _profile = MutableStateFlow<UserProfile?>(null)
    override val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    override fun onLoggedIn(uid: String, phoneNumber: String) {
        val doc = userDoc(uid)
        repoScope.launch {
            val snapshot = doc.get().await()
            if (!snapshot.exists()) {
                doc.set(UserProfile(id = uid, name = "Mumbai Rider", phoneNumber = phoneNumber).toFirestoreMap()).await()
            }
        }
        listenToDoc(uid)
    }

    private var listenerRegistration: ListenerRegistration? = null

    private fun listenToDoc(uid: String) {
        listenerRegistration?.remove()
        listenerRegistration = userDoc(uid).addSnapshotListener { snapshot, _ ->
            _profile.value = snapshot?.let { UserProfile.fromFirestore(it) }
        }
    }

    override fun clear() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _profile.value = null
    }

    override suspend fun updateBasicInfo(name: String, email: String?, gender: Gender?) {
        val uid = _profile.value?.id ?: return
        userDoc(uid).update(
            mapOf(
                "name" to name,
                "email" to email,
                "gender" to gender?.name,
            ),
        ).await()
    }

    override suspend fun addEmergencyContact(name: String, phoneNumber: String) {
        val current = _profile.value ?: return
        val updated = current.emergencyContacts + EmergencyContact(name = name, phoneNumber = phoneNumber)
        userDoc(current.id).update("emergencyContacts", updated.map { it.toFirestoreMap() }).await()
    }

    override suspend fun removeEmergencyContact(contactId: String) {
        val current = _profile.value ?: return
        val updated = current.emergencyContacts.filterNot { it.id == contactId }
        userDoc(current.id).update("emergencyContacts", updated.map { it.toFirestoreMap() }).await()
    }

    override suspend fun addSavedPlace(label: SavedPlaceLabel, customName: String?, address: String, point: GeoPoint) {
        val current = _profile.value ?: return
        val withoutSameLabel = if (label != SavedPlaceLabel.OTHER) {
            current.savedPlaces.filterNot { it.label == label }
        } else {
            current.savedPlaces
        }
        val updated = withoutSameLabel + SavedPlace(label = label, customName = customName, address = address, point = point)
        userDoc(current.id).update("savedPlaces", updated.map { it.toFirestoreMap() }).await()
    }

    override suspend fun removeSavedPlace(placeId: String) {
        val current = _profile.value ?: return
        val updated = current.savedPlaces.filterNot { it.id == placeId }
        userDoc(current.id).update("savedPlaces", updated.map { it.toFirestoreMap() }).await()
    }
}

@Singleton
class FakeProfileRepository @Inject constructor() : ProfileRepository {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    override val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    override fun onLoggedIn(uid: String, phoneNumber: String) {
        _profile.value = UserProfile(
            id = uid,
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
