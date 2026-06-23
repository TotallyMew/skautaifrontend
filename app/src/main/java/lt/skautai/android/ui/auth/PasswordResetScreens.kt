package lt.skautai.android.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import lt.skautai.android.ui.common.SkautaiInlineErrorBanner
import lt.skautai.android.util.NavRoutes

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: PasswordResetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    PasswordResetScaffold(title = "Atkurti slaptažodį", onBack = { navController.popBackStack() }) {
        Text("Įveskite su paskyra susietą el. pašto adresą. Atsiųsime vienkartinę nuorodą.")
        state.error?.let { SkautaiInlineErrorBanner(it) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("El. paštas") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = viewModel::requestReset,
            enabled = !state.isLoading && state.message == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text("Siųsti atkūrimo nuorodą")
        }
        if (state.message != null) {
            TextButton(onClick = {
                navController.navigate(NavRoutes.Login.route) {
                    popUpTo(NavRoutes.Login.route) { inclusive = true }
                }
            }) { Text("Grįžti į prisijungimą") }
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
    PasswordResetScaffold(title = "Naujas slaptažodis", onBack = { navController.popBackStack() }) {
        Text("Sukurkite naują slaptažodį. Po pakeitimo visi ankstesni prisijungimai bus atšaukti.")
        state.error?.let { SkautaiInlineErrorBanner(it) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        PasswordField("Naujas slaptažodis", state.newPassword, viewModel::onNewPasswordChange)
        PasswordField("Pakartokite slaptažodį", state.confirmPassword, viewModel::onConfirmPasswordChange)
        Button(
            onClick = { viewModel.resetPassword(token) },
            enabled = !state.isLoading && state.message == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text("Pakeisti slaptažodį")
        }
        if (state.message != null) {
            TextButton(onClick = {
                navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
            }) { Text("Prisijungti") }
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordResetScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("Atgal") }
            Text(title, style = MaterialTheme.typography.headlineMedium)
            content()
        }
    }
}
