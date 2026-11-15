package dev.anilbeesetti.nextplayer.auth

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean,
    val isEmailVerified: Boolean,
    val providerId: String
)

sealed interface UserBanStatus {
    data class Active(val user: AuthUser) : UserBanStatus
    data class Banned(val reason: String) : UserBanStatus
    data class EmailNotVerified(val email: String) : UserBanStatus
    object SignedOut : UserBanStatus
}

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            cont.cancel()
        }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun checkCurrentUserStatus(): UserBanStatus = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext UserBanStatus.SignedOut
        syncAndVerifyUser(user)
    }

    suspend fun signInWithGoogle(idToken: String): Result<UserBanStatus> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val current = auth.currentUser
            val authResult = try {
                if (current != null && current.isAnonymous) {
                    // Try linking guest account with Google credential
                    current.linkWithCredential(credential).awaitTask()
                } else {
                    auth.signInWithCredential(credential).awaitTask()
                }
            } catch (collision: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                // If account already exists with Google, sign in directly
                auth.signInWithCredential(credential).awaitTask()
            }
            val user = authResult.user ?: throw IllegalStateException("Google sign in returned no user")
            val status = syncAndVerifyUser(user)
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun signInWithGitHub(activity: Activity): Result<UserBanStatus> = withContext(Dispatchers.Main) {
        try {
            val provider = OAuthProvider.newBuilder("github.com").apply {
                setScopes(listOf("read:user", "user:email"))
            }

            val pendingResultTask = auth.pendingAuthResult
            val authResult = if (pendingResultTask != null) {
                pendingResultTask.awaitTask()
            } else {
                auth.startActivityForSignInWithProvider(activity, provider.build()).awaitTask()
            }

            val user = authResult.user ?: throw IllegalStateException("GitHub sign in returned no user")
            val status = withContext(Dispatchers.IO) { syncAndVerifyUser(user) }
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserBanStatus> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).awaitTask()
            val user = authResult.user ?: throw IllegalStateException("Email sign in returned no user")
            val status = syncAndVerifyUser(user)
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<UserBanStatus> = withContext(Dispatchers.IO) {
        try {
            val current = auth.currentUser
            val trimmedEmail = email.trim()
            val user = if (current != null && current.isAnonymous) {
                try {
                    val credential = EmailAuthProvider.getCredential(trimmedEmail, pass)
                    val linkResult = current.linkWithCredential(credential).awaitTask()
                    linkResult.user ?: throw IllegalStateException("Linking returned no user")
                } catch (collision: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    val createResult = auth.createUserWithEmailAndPassword(trimmedEmail, pass).awaitTask()
                    createResult.user ?: throw IllegalStateException("Sign up returned no user")
                }
            } else {
                val createResult = auth.createUserWithEmailAndPassword(trimmedEmail, pass).awaitTask()
                createResult.user ?: throw IllegalStateException("Sign up returned no user")
            }

            // Send verification email
            try {
                user.sendEmailVerification().awaitTask()
            } catch (_: Exception) {}

            val status = syncAndVerifyUser(user)
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.sendPasswordResetEmail(email.trim()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    suspend fun resendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: throw IllegalStateException("No authenticated user")
            user.sendEmailVerification().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }


    suspend fun reloadAndCheckVerification(): UserBanStatus = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext UserBanStatus.SignedOut
        syncAndVerifyUser(user)
    }

    fun signOut() {
        auth.signOut()
    }

    private suspend fun syncAndVerifyUser(user: FirebaseUser): UserBanStatus {
        // Disallow and sign out any anonymous users
        if (user.isAnonymous) {
            auth.signOut()
            return UserBanStatus.SignedOut
        }

        // 1. Reload user to catch server-side disabling from Firebase Admin SDK
        try {
            user.reload().awaitTask()
        } catch (e: FirebaseAuthInvalidUserException) {
            return UserBanStatus.Banned("Account has been disabled by administrator.")
        } catch (_: Exception) {}

        // 2. Email verification check for email/password provider users
        // Note: Do not block app access if email is not yet verified so users can immediately use the app
        val isPasswordProvider = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }

        // 3. Firestore server-side check for banned flag (Rule 3 & Rule 7)
        val docRef = firestore.collection("users").document(user.uid)
        val snapshot = try {
            docRef.get().awaitTask()
        } catch (_: Exception) {
            null
        }

        if (snapshot != null && snapshot.exists()) {
            val isBanned = snapshot.getBoolean("banned") ?: false
            if (isBanned) {
                val reason = snapshot.getString("bannedReason") ?: "Account suspended by administrator."
                return UserBanStatus.Banned(reason)
            }
        } else {
            // First time registration: create users/{uid} document with banned: false
            val initialProfile = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "isAnonymous" to user.isAnonymous,
                "createdAt" to System.currentTimeMillis(),
                "banned" to false
            )
            try {
                docRef.set(initialProfile, SetOptions.merge()).awaitTask()
            } catch (_: Exception) {}
        }

        val authUser = AuthUser(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName ?: if (user.isAnonymous) "Guest" else null,
            photoUrl = user.photoUrl?.toString(),
            isAnonymous = user.isAnonymous,
            isEmailVerified = user.isEmailVerified,
            providerId = user.providerData.firstOrNull()?.providerId ?: "firebase"
        )
        return UserBanStatus.Active(authUser)
    }

    private fun mapAuthException(e: Exception): Exception {
        val userFriendlyMessage = when (e) {
            is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
            is FirebaseAuthInvalidUserException -> "No account found with this email. Please switch to Create Account."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please choose Sign In or use Google/GitHub."
            is FirebaseAuthInvalidCredentialsException -> {
                val rawMsg = e.message.orEmpty().lowercase()
                if (rawMsg.contains("badly formatted") || rawMsg.contains("format")) {
                    "Please enter a valid email address."
                } else {
                    "Invalid email or password. Please verify your credentials."
                }
            }
            is FirebaseNetworkException -> "Network error: Please check your internet connection."
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
        return Exception(userFriendlyMessage)
    }
}
