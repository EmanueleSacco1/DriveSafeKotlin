package com.example.drivesafe.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivesafe.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for the Profile screen.
 * Manages the fetching, creation, and saving of the user's profile data
 * from Firebase Firestore.
 */
class ProfileViewModel : ViewModel() {

    /**
     * Firebase Authentication instance for accessing the current user.
     */
    private val auth = FirebaseAuth.getInstance()

    /**
     * Firebase Firestore instance for database operations.
     */
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Reference to the "users" collection in Firebase Firestore.
     */
    private val usersCollection = firestore.collection("users")

    /**
     * Private MutableLiveData for the user's profile data.
     * Holds the [User] object or null if not loaded/found.
     */
    private val _userProfile = MutableLiveData<User?>()

    /**
     * Public LiveData ([userProfile]) exposed to the UI for observing user profile changes.
     */
    val userProfile: LiveData<User?> = _userProfile

    /**
     * Private MutableLiveData for the loading state.
     * Indicates whether a data fetching or saving operation is in progress.
     */
    private val _isLoading = MutableLiveData<Boolean>()

    /**
     * Public LiveData ([isLoading]) exposed to the UI for observing the loading state.
     */
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Private MutableLiveData for indicating the success status of a save operation.
     * Can be null (initial/reset), true (success), or false (failure).
     */
    private val _saveSuccess = MutableLiveData<Boolean?>()

    /**
     * Public LiveData ([saveSuccess]) exposed to the UI for observing save success.
     */
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    /**
     * Private MutableLiveData for holding error messages.
     * Contains a string message if an error occurred, otherwise null.
     */
    private val _errorMessage = MutableLiveData<String?>()

    /**
     * Public LiveData ([errorMessage]) exposed to the UI for observing error messages.
     */
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Initialization block: Called when the ViewModel is created.
     * Initiates the process of fetching the user profile.
     */
    init {
        fetchUserProfile()
    }

    /**
     * Fetches the user profile data from Firebase Firestore.
     * If the profile does not exist for the current authenticated user, a new basic profile is created.
     */
    fun fetchUserProfile() {
        _isLoading.value = true
        _errorMessage.value = null
        _saveSuccess.value = null

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "Utente non autenticato."
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Attempting to fetch user profile for UID: $userId")
                val documentSnapshot = usersCollection.document(userId).get().await()
                val user = documentSnapshot.toObject(User::class.java)

                if (user == null) {
                    Log.d("ProfileViewModel", "User profile not found for UID: $userId. Creating new one.")
                    val email = auth.currentUser?.email ?: ""
                    val newUser = User(
                        uid = userId,
                        email = email,
                        username = "",
                        favoriteCarBrand = "",
                        placeOfBirth = "",
                        streetAddress = "",
                        cityOfResidence = "",
                        yearOfBirth = null,
                        licenses = emptyList(),
                        registrationTimestamp = System.currentTimeMillis()
                    )
                    usersCollection.document(userId).set(newUser).await()
                    _userProfile.value = newUser
                    Log.d("ProfileViewModel", "New user profile created and saved for UID: $userId")
                } else {
                    _userProfile.value = user
                    Log.d("ProfileViewModel", "User profile fetched successfully for UID: $userId")
                }

            } catch (e: Exception) {
                _errorMessage.value = "Errore nel caricamento del profilo: ${e.message}"
                Log.e("ProfileViewModel", "Error fetching user profile for UID: $userId", e)
                _userProfile.value = null
            } finally {
                _isLoading.value = false
                Log.d("ProfileViewModel", "Fetch user profile operation finished for UID: $userId")
            }
        }
    }

    /**
     * Saves the provided user profile data to Firebase Firestore.
     * @param user The [User] object containing the updated profile data to save.
     */
    fun saveUserProfile(user: User) {
        _isLoading.value = true
        _errorMessage.value = null
        _saveSuccess.value = null

        val userId = auth.currentUser?.uid
        if (userId == null || user.uid != userId) {
            _errorMessage.value = "Errore: ID utente non valido per il salvataggio."
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Attempting to save user profile for UID: $userId")
                usersCollection.document(userId).set(user).await()
                _saveSuccess.value = true
                Log.d("ProfileViewModel", "User profile saved successfully for UID: $userId")
                _userProfile.value = user
            } catch (e: Exception) {
                _errorMessage.value = "Errore nel salvataggio del profilo: ${e.message}"
                Log.e("ProfileViewModel", "Error saving user profile for UID: $userId", e)
                _saveSuccess.value = false
            } finally {
                _isLoading.value = false
                Log.d("ProfileViewModel", "Save user profile operation finished for UID: $userId")
            }
        }
    }

    /**
     * Resets the [_saveSuccess] LiveData value to null.
     * Used by the UI after handling a save success event.
     */
    fun resetSaveSuccess() {
        _saveSuccess.value = null
    }

    /**
     * Resets the [_errorMessage] LiveData value to null.
     * Used by the UI after displaying an error message.
     */
    fun resetErrorMessage() {
        _errorMessage.value = null
    }
}
