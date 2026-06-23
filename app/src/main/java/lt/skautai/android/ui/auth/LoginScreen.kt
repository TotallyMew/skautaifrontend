package lt.skautai.android.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import lt.skautai.android.ui.common.SkautaiInlineErrorBanner
import lt.skautai.android.util.NavRoutes

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            val destination = when {
                uiState.isSuperAdmin -> NavRoutes.SuperAdminDashboard.route
                uiState.hasActiveTuntas -> NavRoutes.Home.route
                else -> NavRoutes.TuntasSelect.route
            }
            navController.navigate(destination) {
                popUpTo(NavRoutes.Login.route) { inclusive = true }
            }
        }
    }

    AuthScreenLayout(
        heroTitle = "Prisijunk prie savo tunto inventoriaus",
        heroDescription = "Vienoje vietoje matysi bendrą tunto, vieneto ir savo siūlomą inventorių.",
        cardTitle = "Prisijungimas"
    ) {
        uiState.formError?.let { SkautaiInlineErrorBanner(message = it) }

        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "El. paštas",
            icon = Icons.Outlined.AlternateEmail,
            keyboardType = KeyboardType.Email,
            errorText = uiState.emailError
        )
        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Slaptažodis",
            icon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            errorText = uiState.passwordError
        )

        TextButton(
            onClick = { navController.navigate(NavRoutes.ForgotPassword.route) },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 2.dp, bottom = 10.dp)
        ) {
            Text("Pamiršote slaptažodį?")
        }

        AuthPrimaryButton(
            text = "Prisijungti",
            loading = uiState.isLoading,
            onClick = viewModel::login
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            AuthInlineAction(
                prompt = "Turite pakvietimą?",
                action = "Susikurkite paskyrą",
                onClick = { navController.navigate(NavRoutes.RegisterInvite.route) }
            )
            AuthInlineAction(
                prompt = "Tunto dar nėra?",
                action = "Užregistruokite jį",
                onClick = { navController.navigate(NavRoutes.Register.route) }
            )
        }
    }
}
