package dev.roxy.kryptonplayer.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.roxy.kryptonplayer.core.ui.R
import kotlinx.coroutines.launch

private fun getGoogleWebClientId(context: android.content.Context): String {
    // 1. Check default_web_client_id with runtime packageName
    val resId1 = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId1 != 0) {
        val id = context.getString(resId1).trim()
        if (id.isNotBlank()) return id
    }
    // 2. Check default_web_client_id with module namespace
    val resId2 = context.resources.getIdentifier("default_web_client_id", "string", "dev.roxy.kryptonplayer")
    if (resId2 != 0) {
        val id = context.getString(resId2).trim()
        if (id.isNotBlank()) return id
    }
    // 3. Check custom string google_web_client_id with runtime packageName
    val resId3 = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
    if (resId3 != 0) {
        val id = context.getString(resId3).trim()
        if (id.isNotBlank()) return id
    }
    // 4. Check custom string google_web_client_id with module namespace
    val resId4 = context.resources.getIdentifier("google_web_client_id", "string", "dev.roxy.kryptonplayer")
    if (resId4 != 0) {
        val id = context.getString(resId4).trim()
        if (id.isNotBlank()) return id
    }
    // 5. Fallback if web client ID is not configured in strings or google-services
    return ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceApprovalGateScreen(
    viewModel: DeviceAuthViewModel,
    onApproved: () -> Unit,
    onSkip: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()

    val isDark = isSystemInDarkTheme()

    // Screen color palette (pure AMOLED pitch black in dark mode)
    val screenBgTop = if (isDark) Color(0xFF000000) else Color(0xFFEFF5FA)
    val screenBgBottom = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF080808) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textMuted = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val fieldBg = if (isDark) Color(0xFF1A1D2A) else Color(0xFFF8FAFC)
    val fieldBorder = if (isDark) Color(0xFF2B3044) else Color(0xFFE2E8F0)
    val accentBlue = Color(0xFF2563EB)
    val primaryButtonBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val primaryButtonText = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val socialButtonBg = if (isDark) Color(0xFF1A1D2A) else Color(0xFFFFFFFF)
    val socialButtonBorder = if (isDark) Color(0xFF2B3044) else Color(0xFFE2E8F0)
    val errorColor = Color(0xFFEF4444)

    // Form states
    var isSignUpMode by remember { mutableStateOf(false) }
    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }

    // Dialogs & Bottom Sheet
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var showTermsBottomSheet by remember { mutableStateOf(false) }
    var termsSheetTab by remember { mutableIntStateOf(0) } // 0 = Terms of Service, 1 = Privacy Policy

    // When signed in or approved, navigate into main app
    LaunchedEffect(uiState) {
        if (uiState is DeviceAuthState.Approved) {
            onApproved()
        }
    }

    // Modern Credential Manager for Google Sign-In
    val credentialManager = remember { CredentialManager.create(context) }

    fun launchGoogleSignIn() {
        val activity = context as? Activity
        if (activity == null) {
            viewModel.onSignInError("Activity context unavailable")
            return
        }

        coroutineScope.launch {
            viewModel.onGoogleSignInStarted()
            try {
                val webClientId = getGoogleWebClientId(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.signInWithGoogleIdToken(googleIdTokenCredential.idToken)
                } else {
                    viewModel.onSignInError("Unsupported credential type received.")
                }
            } catch (_: GetCredentialCancellationException) {
                viewModel.cancelSignIn()
            } catch (_: NoCredentialException) {
                viewModel.onSignInError("No Google account found on this device.")
            } catch (e: Exception) {
                viewModel.onSignInError(e.localizedMessage ?: "Google Sign-In failed.")
            }
        }
    }

    fun submitEmailForm() {
        focusManager.clearFocus()
        val trimmedEmail = emailInput.trim()
        if (trimmedEmail.isEmpty()) {
            viewModel.onSignInError("Please enter your email address.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            viewModel.onSignInError("Please enter a valid email address.")
            return
        }
        if (passwordInput.isEmpty()) {
            viewModel.onSignInError("Please enter your password.")
            return
        }
        if (passwordInput.length < 6) {
            viewModel.onSignInError("Password must be at least 6 characters.")
            return
        }

        if (isSignUpMode) {
            if (passwordInput != confirmPasswordInput) {
                viewModel.onSignInError("Passwords do not match. Please verify.")
                return
            }
            if (!termsAccepted) {
                viewModel.onSignInError("Please accept the Terms of Service to continue.")
                return
            }
            viewModel.signUpWithEmail(trimmedEmail, passwordInput)
        } else {
            viewModel.signInWithEmail(trimmedEmail, passwordInput)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(screenBgTop, screenBgBottom)
                )
            )
    ) {
        when (val state = uiState) {
            is DeviceAuthState.Banned -> {
                BannedScreen(
                    reason = state.reason,
                    onSignOut = { viewModel.signOut() }
                )
            }
            is DeviceAuthState.EmailVerificationRequired -> {
                EmailVerificationRequiredScreen(
                    email = state.email,
                    onCheckVerified = { viewModel.checkEmailVerified() },
                    onResendEmail = {
                        viewModel.resendVerificationEmail { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSignOut = { viewModel.signOut() }
                )
            }
            else -> {
                val signingInProvider = (uiState as? DeviceAuthState.SigningIn)?.provider
                val isAnySigningIn = uiState is DeviceAuthState.SigningIn
                val isEmailSigningIn = isAnySigningIn && signingInProvider == AuthProviderType.EMAIL
                val isGoogleSigningIn = isAnySigningIn && signingInProvider == AuthProviderType.GOOGLE
                val isGitHubSigningIn = isAnySigningIn && signingInProvider == AuthProviderType.GITHUB

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Playful Top Banner Area (Full-Bleed Edge-to-Edge Stretched Artwork)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.auth_welcome_banner),
                            contentDescription = "Welcome illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )

                        // Smooth multi-stop gradient blending overlay stretching into card below
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            cardBg.copy(alpha = 0.30f),
                                            cardBg.copy(alpha = 0.80f),
                                            cardBg
                                        )
                                    )
                                )
                        )

                        if (onSkip != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(top = 12.dp, end = 16.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .clickable(onClick = onSkip)
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Skip",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "Skip",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Main Authentication Card (Rounded, Crystal-Clear with Smooth Seamless Overlap)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-16).dp)
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                        color = cardBg,
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title & Subtitle with smooth transitions
                            AnimatedContent(
                                targetState = isSignUpMode,
                                transitionSpec = {
                                    (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                                },
                                label = "AuthTitleAnimation"
                            ) { isSignUp ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isSignUp) "Create your account" else "Welcome back",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isSignUp) "Let's get to know you better" else "Sign in to continue",
                                        fontSize = 14.sp,
                                        color = textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Error notice banner
                            AnimatedVisibility(
                                visible = uiState is DeviceAuthState.Error,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                val errorMsg = (uiState as? DeviceAuthState.Error)?.message.orEmpty()
                                if (errorMsg.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = errorColor.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, errorColor.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = errorColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = errorMsg,
                                                fontSize = 13.sp,
                                                color = errorColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Form Fields
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Full Name (Sign Up only)
                                if (isSignUpMode) {
                                    OutlinedTextField(
                                        value = fullNameInput,
                                        onValueChange = {
                                            fullNameInput = it
                                            viewModel.clearError()
                                        },
                                        placeholder = { Text("Full name", color = textMuted, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_custom_user),
                                                contentDescription = null,
                                                tint = textMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = accentBlue,
                                            unfocusedBorderColor = fieldBorder,
                                            cursorColor = accentBlue
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Email Address
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = {
                                        emailInput = it
                                        viewModel.clearError()
                                    },
                                    placeholder = { Text("Email address", color = textMuted, fontSize = 14.sp) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_custom_mail),
                                            contentDescription = null,
                                            tint = textMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = fieldBg,
                                        unfocusedContainerColor = fieldBg,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                        focusedBorderColor = accentBlue,
                                        unfocusedBorderColor = fieldBorder,
                                        cursorColor = accentBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Password
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = {
                                        passwordInput = it
                                        viewModel.clearError()
                                    },
                                    placeholder = {
                                        Text(
                                            text = if (isSignUpMode) "Password (min 6 chars)" else "Password",
                                            color = textMuted,
                                            fontSize = 14.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_custom_lock),
                                            contentDescription = null,
                                            tint = textMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                painter = painterResource(
                                                    id = if (passwordVisible) R.drawable.ic_custom_eye_off else R.drawable.ic_custom_eye
                                                ),
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                tint = textMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (!isSignUpMode) submitEmailForm()
                                    }),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = fieldBg,
                                        unfocusedContainerColor = fieldBg,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                        focusedBorderColor = accentBlue,
                                        unfocusedBorderColor = fieldBorder,
                                        cursorColor = accentBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Confirm Password (Sign Up only)
                                if (isSignUpMode) {
                                    OutlinedTextField(
                                        value = confirmPasswordInput,
                                        onValueChange = {
                                            confirmPasswordInput = it
                                            viewModel.clearError()
                                        },
                                        placeholder = { Text("Confirm password", color = textMuted, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_custom_lock),
                                                contentDescription = null,
                                                tint = textMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (confirmPasswordVisible) R.drawable.ic_custom_eye_off else R.drawable.ic_custom_eye
                                                    ),
                                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                                    tint = textMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = {
                                            submitEmailForm()
                                        }),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = accentBlue,
                                            unfocusedBorderColor = fieldBorder,
                                            cursorColor = accentBlue
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Sub-form actions: Forgot password (Sign In) OR Terms checkbox (Sign Up)
                            if (!isSignUpMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Forgot password?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = accentBlue,
                                        modifier = Modifier
                                            .clickable {
                                                resetEmailInput = emailInput.trim()
                                                showResetDialog = true
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = termsAccepted,
                                        onCheckedChange = { termsAccepted = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accentBlue,
                                            uncheckedColor = textMuted
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "I agree to the ",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                        Text(
                                            text = "Terms of Service",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentBlue,
                                            modifier = Modifier.clickable {
                                                termsSheetTab = 0
                                                showTermsBottomSheet = true
                                            }
                                        )
                                        Text(
                                            text = " and ",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                        Text(
                                            text = "Privacy Policy",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentBlue,
                                            modifier = Modifier.clickable {
                                                termsSheetTab = 1
                                                showTermsBottomSheet = true
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Primary Action Button (Sign in / Sign up)
                            Button(
                                onClick = { submitEmailForm() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(percent = 50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryButtonBg,
                                    contentColor = primaryButtonText
                                ),
                                enabled = !isAnySigningIn && emailInput.isNotBlank() && passwordInput.isNotBlank()
                            ) {
                                if (isEmailSigningIn) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = primaryButtonText,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isSignUpMode) "Signing up..." else "Signing in...",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isSignUpMode) "Sign up" else "Sign in",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // "or continue with" Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = fieldBorder)
                                Text(
                                    text = if (isSignUpMode) "or sign up with" else "or continue with",
                                    fontSize = 13.sp,
                                    color = textMuted,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = fieldBorder)
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Social Auth Buttons (Google & GitHub)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Continue with Google
                                Button(
                                    onClick = { launchGoogleSignIn() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(percent = 50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = socialButtonBg,
                                        contentColor = textPrimary
                                    ),
                                    border = BorderStroke(1.dp, socialButtonBorder),
                                    enabled = !isAnySigningIn
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isGoogleSigningIn) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color(0xFF4285F4),
                                                strokeWidth = 2.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (isSignUpMode) "Signing up with Google..." else "Signing in with Google...",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimary
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_google_logo),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (isSignUpMode) "Sign up with Google" else "Continue with Google",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimary
                                            )
                                        }
                                    }
                                }

                                // 2. Continue with GitHub
                                Button(
                                    onClick = {
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            viewModel.signInWithGitHub(activity)
                                        } else {
                                            viewModel.onSignInError("Activity context unavailable")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(percent = 50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = socialButtonBg,
                                        contentColor = textPrimary
                                    ),
                                    border = BorderStroke(1.dp, socialButtonBorder),
                                    enabled = !isAnySigningIn
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isGitHubSigningIn) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = textPrimary,
                                                strokeWidth = 2.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (isSignUpMode) "Signing up with GitHub..." else "Signing in with GitHub...",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimary
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_github),
                                                contentDescription = null,
                                                tint = textPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (isSignUpMode) "Sign up with GitHub" else "Continue with GitHub",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Bottom Mode Switcher (Sign In <-> Sign Up)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                                    fontSize = 14.sp,
                                    color = textSecondary
                                )
                                Text(
                                    text = if (isSignUpMode) "Sign in" else "Sign up",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentBlue,
                                    modifier = Modifier
                                        .clickable(enabled = !isAnySigningIn) {
                                            viewModel.clearError()
                                            isSignUpMode = !isSignUpMode
                                        }
                                        .padding(4.dp)
                                )
                            }

                            if (onSkip != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Skip for now → Continue as Guest",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary,
                                    modifier = Modifier
                                        .clickable(onClick = onSkip)
                                        .padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Terms of Service & Privacy Policy Bottom Sheet
    if (showTermsBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTermsBottomSheet = false },
            sheetState = sheetState,
            containerColor = cardBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Legal & Privacy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(onClick = { showTermsBottomSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Switcher for Sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(fieldBg, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { termsSheetTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (termsSheetTab == 0) primaryButtonBg else Color.Transparent,
                            contentColor = if (termsSheetTab == 0) primaryButtonText else textSecondary
                        ),
                        elevation = null
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_custom_document),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Terms of Service",
                            fontSize = 13.sp,
                            fontWeight = if (termsSheetTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Button(
                        onClick = { termsSheetTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (termsSheetTab == 1) primaryButtonBg else Color.Transparent,
                            contentColor = if (termsSheetTab == 1) primaryButtonText else textSecondary
                        ),
                        elevation = null
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_custom_shield),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Privacy Policy",
                            fontSize = 13.sp,
                            fontWeight = if (termsSheetTab == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Legal Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (termsSheetTab == 0) {
                        // Terms of Service Content
                        LegalSection(
                            title = "1. Acceptance of Terms",
                            content = "By downloading, installing, or creating an account on Krypton Player, you agree to be bound by these Terms of Service. If you do not agree, please do not use the application.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "2. Media Playback & Local Library",
                            content = "Krypton Player is designed as an offline and cloud-enabled media player. You retain full ownership and responsibility for the media files you store, index, or stream. Krypton Player does not host, distribute, or license any copyrighted video or audio content.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "3. User Accounts & Security",
                            content = "You are responsible for maintaining the confidentiality of your authentication credentials. Krypton Player allows authentication via Google, GitHub, and Email for seamless sync of bookmarks and playback preferences.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "4. Prohibited Conduct",
                            content = "You agree not to reverse engineer, decompile, or attempt to derive source code maliciously, or use the service for unauthorized network intrusion or distributing malicious software.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "5. Limitation of Liability",
                            content = "Krypton Player is provided on an 'AS IS' and 'AS AVAILABLE' basis without warranties of any kind. Developers shall not be liable for any direct or indirect loss resulting from software usage.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        // Privacy Policy Content
                        LegalSection(
                            title = "1. Privacy-First Commitment",
                            content = "Your privacy is our core priority. Krypton Player does not sell, lease, or monetize your personal information or media playback data.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "2. Information We Collect",
                            content = "We only collect data necessary to provide service features:\n• Account Data: Email, display name, and avatar URL provided via Google, GitHub, or Email auth.\n• Preferences: Subtitle settings, video aspect ratio preferences, and watch history stored locally.\n• Diagnostics: Anonymous crash logs through Firebase Crashlytics to detect and fix runtime bugs.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "3. Storage & Device Permissions",
                            content = "Device storage permissions are requested strictly to scan, read, and play local video files on your device. Your local media files are never transferred to external servers.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "4. Data Security",
                            content = "All authentication requests are encrypted using industry-standard TLS protocols through Google Firebase Authentication and Google Cloud infrastructure.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        LegalSection(
                            title = "5. User Control & Deletion",
                            content = "You can sign out, reset preferences, or delete your account data at any time from application settings.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Confirmation Button
                Button(
                    onClick = {
                        termsAccepted = true
                        showTermsBottomSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonBg,
                        contentColor = primaryButtonText
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I Understand & Accept", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Password Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Password",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your account email to receive a password reset link.",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        placeholder = { Text("Email address", color = textMuted, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = accentBlue,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = accentBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmailInput.isNotBlank()) {
                            viewModel.sendPasswordReset(resetEmailInput) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) showResetDialog = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonBg,
                        contentColor = primaryButtonText
                    )
                ) {
                    Text("Send Link", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun LegalSection(
    title: String,
    content: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 13.sp,
            color = textSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun BannedScreen(
    reason: String,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Account Suspended",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = reason.ifBlank { "Your account access has been revoked by the administrator." },
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        OutlinedButton(
            onClick = onSignOut,
            border = BorderStroke(1.dp, Color(0xFF2E2E32)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            shape = RoundedCornerShape(50)
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun EmailVerificationRequiredScreen(
    email: String,
    onCheckVerified: () -> Unit,
    onResendEmail: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF2563EB).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_custom_mail),
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Verify Your Email",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We sent a verification link to:\n$email\nPlease click the link in your email inbox to activate your account.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onCheckVerified,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("I Have Verified", fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onResendEmail) {
            Text("Resend Verification Email", color = Color(0xFF94A3B8))
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onSignOut) {
            Text("Back to Sign In", color = Color(0xFF94A3B8))
        }
    }
}
