package lt.skautai.android.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import lt.skautai.android.ui.common.SkautaiInlineErrorBanner

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: PasswordResetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    AuthScreenLayout(
        heroTitle = "Atkurkite prieigą prie paskyros",
        heroDescription = "Į el. paštą atsiųsime saugią, vieną valandą galiojančią nuorodą.",
        cardTitle = if (state.message == null) "Slaptažodžio atkūrimas" else "Patikrinkite el. paštą",
        onBack = { navController.popBackStack() }
    ) {
        if (state.message == null) {
            Text("Įveskite su paskyra susietą el. pašto adresą.")
            state.error?.let { SkautaiInlineErrorBanner(it) }
            AuthTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "El. paštas",
                icon = Icons.Outlined.AlternateEmail,
                keyboardType = KeyboardType.Email
            )
            AuthPrimaryButton(
                text = "Siųsti atkūrimo nuorodą",
                loading = state.isLoading,
                onClick = viewModel::requestReset
            )
        } else {
            Text(
                text = state.message.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Jei laiško nematote, patikrinkite šlamšto aplanką.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ResetPasswordScreen(
    token: String,
    navController: NavController,
    viewModel: PasswordResetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    AuthScreenLayout(
        heroTitle = "Sukurkite naują slaptažodį",
        heroDescription = "Pakeitus slaptažodį, visi ankstesni prisijungimai bus atšaukti.",
        cardTitle = if (state.message == null) "Naujas slaptažodis" else "Slaptažodis pakeistas",
        onBack = { navController.popBackStack() }
    ) {
        if (state.message == null) {
            state.error?.let { SkautaiInlineErrorBanner(it) }
            AuthTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = "Naujas slaptažodis",
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true
            )
            AuthTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Pakartokite slaptažodį",
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true
            )
            AuthPrimaryButton(
                text = "Pakeisti slaptažodį",
                loading = state.isLoading,
                onClick = { viewModel.resetPassword(token) }
            )
        } else {
            Text(
                text = state.message.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Naudokite viršuje esantį mygtuką „Atgal“, kad grįžtumėte į prisijungimą.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
