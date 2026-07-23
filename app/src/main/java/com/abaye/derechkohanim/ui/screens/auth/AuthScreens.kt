package com.abaye.derechkohanim.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkTextField
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun LoginRoute(
    onSignedIn: () -> Unit,
    onGoToRegister: () -> Unit,
    onContinueAsGuest: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    AuthScreen(
        state = state,
        register = false,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
        onSubmit = viewModel::login,
        onGoogle = {
            scope.launch {
                viewModel.loginWithGoogle(
                    GoogleSignIn.requestIdToken(context, GOOGLE_SERVER_CLIENT_ID)
                )
            }
        },
        onSwitchMode = onGoToRegister,
        onContinueAsGuest = onContinueAsGuest,
    )
}

@Composable
fun RegisterRoute(
    onSignedIn: () -> Unit,
    onGoToLogin: () -> Unit,
    onContinueAsGuest: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    AuthScreen(
        state = state,
        register = true,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
        onSubmit = viewModel::register,
        onGoogle = {
            scope.launch {
                viewModel.loginWithGoogle(
                    GoogleSignIn.requestIdToken(context, GOOGLE_SERVER_CLIENT_ID)
                )
            }
        },
        onSwitchMode = onGoToLogin,
        onContinueAsGuest = onContinueAsGuest,
    )
}

@Composable
fun AuthScreen(
    state: AuthState,
    register: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoogle: () -> Unit,
    onSwitchMode: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    Scaffold(containerColor = DkTheme.colors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(DkTheme.spacing.s4)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            Text(
                text = stringResource(
                    if (register) R.string.auth_register_title else R.string.auth_login_title
                ),
                style = MaterialTheme.typography.displayMedium,
                color = DkTheme.colors.text,
            )

            if (register) {
                DkTextField(
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    label = stringResource(R.string.auth_display_name),
                )
            }

            DkTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )

            DkTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_password),
                supportingText = if (register) stringResource(R.string.auth_password_hint) else null,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            if (register) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.termsAccepted,
                            role = Role.Checkbox,
                            onValueChange = onTermsAcceptedChange,
                        ),
                ) {
                    Checkbox(checked = state.termsAccepted, onCheckedChange = null)
                    Text(
                        text = stringResource(R.string.onboarding_terms_accept),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DkTheme.colors.text,
                        modifier = Modifier.padding(start = DkTheme.spacing.s3),
                    )
                }
            }

            state.errorCode?.let { code ->
                Text(
                    text = stringResource(authErrorRes(code)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DkTheme.colors.danger,
                )
            }

            DkButton(
                text = stringResource(
                    if (register) R.string.auth_register_action else R.string.auth_login_action
                ),
                onClick = onSubmit,
                loading = state.busy,
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DkButton(
                text = stringResource(R.string.auth_google),
                onClick = onGoogle,
                variant = DkButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )

            DkButton(
                text = stringResource(
                    if (register) R.string.auth_to_login else R.string.auth_to_register
                ),
                onClick = onSwitchMode,
                variant = DkButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )

            DkButton(
                text = stringResource(R.string.auth_continue_as_guest),
                onClick = onContinueAsGuest,
                variant = DkButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun authErrorRes(code: String): Int = when (code) {
    "terms_required" -> R.string.auth_error_terms_required
    "google_unavailable" -> R.string.auth_error_google_unavailable
    "email_taken" -> R.string.auth_error_email_taken
    ErrorCode.UNAUTHORIZED, "invalid_credentials" -> R.string.auth_error_invalid_credentials
    ErrorCode.NETWORK -> R.string.error_network
    else -> R.string.error_generic
}

/**
 * The backend OAuth client id. Left empty on purpose: it belongs to the deployment, not to
 * the source tree. Supply it through a build config field once the API exposes one.
 */
private val GOOGLE_SERVER_CLIENT_ID: String = ""

@Preview(name = "Login", showBackground = true, locale = "he")
@Composable
private fun LoginPreview() {
    DerechKohanimTheme {
        AuthScreen(
            state = AuthState(email = "kohen@example.org"),
            register = false,
            onEmailChange = {},
            onPasswordChange = {},
            onDisplayNameChange = {},
            onTermsAcceptedChange = {},
            onSubmit = {},
            onGoogle = {},
            onSwitchMode = {},
            onContinueAsGuest = {},
        )
    }
}

@Preview(name = "Register English", showBackground = true, locale = "en")
@Composable
private fun RegisterPreview() {
    DerechKohanimTheme {
        AuthScreen(
            state = AuthState(termsAccepted = true),
            register = true,
            onEmailChange = {},
            onPasswordChange = {},
            onDisplayNameChange = {},
            onTermsAcceptedChange = {},
            onSubmit = {},
            onGoogle = {},
            onSwitchMode = {},
            onContinueAsGuest = {},
        )
    }
}
