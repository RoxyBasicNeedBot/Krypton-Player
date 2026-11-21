package dev.roxy.kryptonplayer.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.data.repository.DeviceAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthProviderType {
    EMAIL,
    GOOGLE,
    GITHUB
}

sealed interface DeviceAuthState {
    object Initial : DeviceAuthState
    data class SigningIn(val provider: AuthProviderType = AuthProviderType.EMAIL) : DeviceAuthState
    data class EmailVerificationRequired(val email: String) : DeviceAuthState
    data class Banned(val reason: String) : DeviceAuthState
    data class Approved(
        val token: String,
        val email: String? = null,
        val displayName: String? = null,
        val isAnonymous: Boolean = false,
        val uid: String = ""
    ) : DeviceAuthState
    data class Error(val message: String) : DeviceAuthState
}

@HiltViewModel
class DeviceAuthViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val deviceAuthRepository: DeviceAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceAuthState>(
        run {
            val user = firebaseAuthRepository.currentUser
            if (user != null && !user.isAnonymous) {
                DeviceAuthState.Approved(
                    token = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    isAnonymous = false,
                    uid = user.uid
                )
            } else if (deviceAuthRepository.isUserSignedIn()) {
                DeviceAuthState.Approved(
                    token = deviceAuthRepository.getApprovalToken() ?: "approved",
                    email = deviceAuthRepository.getUserEmail(),
                    displayName = deviceAuthRepository.getUserName()
                )
            } else {
                DeviceAuthState.Initial
            }
        }
    )
    val uiState: StateFlow<DeviceAuthState> = _uiState.asStateFlow()

    private val _isVerifyingInitial = MutableStateFlow(true)
    val isVerifyingInitial: StateFlow<Boolean> = _isVerifyingInitial.asStateFlow()

    init {
        checkCurrentAuthState()
    }

    fun checkCurrentAuthState() {
        viewModelScope.launch {
            _isVerifyingInitial.value = true
            when (val status = firebaseAuthRepository.checkCurrentUserStatus()) {
                is UserBanStatus.Active -> {
                    val user = status.user
                    deviceAuthRepository.saveUserAccount(
                        email = user.email.orEmpty(),
                        displayName = user.displayName.orEmpty(),
                        photoUrl = user.photoUrl,
                        provider = user.providerId
                    )
                    _uiState.value = DeviceAuthState.Approved(
                        token = user.uid,
                        email = user.email,
                        displayName = user.displayName,
                        isAnonymous = user.isAnonymous,
                        uid = user.uid
                    )
                }
                is UserBanStatus.Banned -> {
                    _uiState.value = DeviceAuthState.Banned(status.reason)
                }
                is UserBanStatus.EmailNotVerified -> {
                    _uiState.value = DeviceAuthState.EmailVerificationRequired(status.email)
                }
                is UserBanStatus.SignedOut -> {
                    if (deviceAuthRepository.isUserSignedIn()) {
                        _uiState.value = DeviceAuthState.Approved(
                            token = deviceAuthRepository.getApprovalToken() ?: "approved",
                            email = deviceAuthRepository.getUserEmail(),
                            displayName = deviceAuthRepository.getUserName()
                        )
                    } else {
                        _uiState.value = DeviceAuthState.Initial
                    }
                }
            }
            _isVerifyingInitial.value = false
        }
    }

    val userEmail: String? get() = firebaseAuthRepository.currentUser?.email ?: deviceAuthRepository.getUserEmail()
    val userName: String? get() = firebaseAuthRepository.currentUser?.displayName ?: deviceAuthRepository.getUserName()
    val userPhoto: String? get() = firebaseAuthRepository.currentUser?.photoUrl?.toString() ?: deviceAuthRepository.getUserPhoto()
    val userUid: String? get() = firebaseAuthRepository.currentUser?.uid

    fun onSignInStarted(provider: AuthProviderType = AuthProviderType.EMAIL) {
        _uiState.value = DeviceAuthState.SigningIn(provider)
    }

    fun onGoogleSignInStarted() {
        _uiState.value = DeviceAuthState.SigningIn(AuthProviderType.GOOGLE)
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = DeviceAuthState.SigningIn(AuthProviderType.GOOGLE)
            firebaseAuthRepository.signInWithGoogle(idToken).fold(
                onSuccess = { status -> handleUserBanStatus(status) },
                onFailure = { error ->
                    _uiState.value = DeviceAuthState.Error(error.localizedMessage ?: "Google Sign-In failed")
                }
            )
        }
    }

    fun signInWithGitHub(activity: android.app.Activity) {
        viewModelScope.launch {
            _uiState.value = DeviceAuthState.SigningIn(AuthProviderType.GITHUB)
            firebaseAuthRepository.signInWithGitHub(activity).fold(
                onSuccess = { status -> handleUserBanStatus(status) },
                onFailure = { error ->
                    _uiState.value = DeviceAuthState.Error(error.localizedMessage ?: "GitHub Sign-In failed")
                }
            )
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = DeviceAuthState.SigningIn(AuthProviderType.EMAIL)
            firebaseAuthRepository.signInWithEmail(email, pass).fold(
                onSuccess = { status -> handleUserBanStatus(status) },
                onFailure = { error ->
                    _uiState.value = DeviceAuthState.Error(error.localizedMessage ?: "Sign-in failed")
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = DeviceAuthState.SigningIn(AuthProviderType.EMAIL)
            firebaseAuthRepository.signUpWithEmail(email, pass).fold(
                onSuccess = { status -> handleUserBanStatus(status) },
                onFailure = { error ->
                    _uiState.value = DeviceAuthState.Error(error.localizedMessage ?: "Registration failed")
                }
            )
        }
    }

    fun sendPasswordReset(email: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            firebaseAuthRepository.sendPasswordReset(email).fold(
                onSuccess = { onDone(true, "Password reset link sent to your email.") },
                onFailure = { onDone(false, it.localizedMessage ?: "Could not send reset link.") }
            )
        }
    }

    fun resendVerificationEmail(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            firebaseAuthRepository.resendEmailVerification().fold(
                onSuccess = { onResult(true, "Verification email resent.") },
                onFailure = { onResult(false, it.localizedMessage ?: "Failed to resend email.") }
            )
        }
    }

    fun checkEmailVerified() {
        viewModelScope.launch {
            val status = firebaseAuthRepository.reloadAndCheckVerification()
            handleUserBanStatus(status)
        }
    }


    fun onGoogleSignInFailure(errorMsg: String) {
        _uiState.value = DeviceAuthState.Error(errorMsg)
    }

    fun onSignInError(errorMsg: String) {
        _uiState.value = DeviceAuthState.Error(errorMsg)
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseAuthRepository.signOut()
            deviceAuthRepository.signOut()
            _uiState.value = DeviceAuthState.Initial
        }
    }

    fun cancelSignIn() {
        if (_uiState.value is DeviceAuthState.SigningIn) {
            _uiState.value = DeviceAuthState.Initial
        }
    }

    fun clearError() {
        if (_uiState.value is DeviceAuthState.Error || _uiState.value is DeviceAuthState.SigningIn) {
            _uiState.value = DeviceAuthState.Initial
        }
    }

    private fun handleUserBanStatus(status: UserBanStatus) {
        when (status) {
            is UserBanStatus.Active -> {
                val user = status.user
                deviceAuthRepository.saveUserAccount(
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty(),
                    photoUrl = user.photoUrl,
                    provider = user.providerId
                )
                _uiState.value = DeviceAuthState.Approved(
                    token = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    isAnonymous = user.isAnonymous,
                    uid = user.uid
                )
            }
            is UserBanStatus.Banned -> {
                _uiState.value = DeviceAuthState.Banned(status.reason)
            }
            is UserBanStatus.EmailNotVerified -> {
                _uiState.value = DeviceAuthState.EmailVerificationRequired(status.email)
            }
            is UserBanStatus.SignedOut -> {
                _uiState.value = DeviceAuthState.Initial
            }
        }
    }
}
