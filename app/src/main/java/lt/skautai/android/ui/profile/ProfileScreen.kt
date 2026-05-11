package lt.skautai.android.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiPrimaryButton
import lt.skautai.android.ui.common.SkautaiTextField

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            if (uiState.profile != null) {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SkautaiErrorSnackbarHost(hostState = snackbarHostState)
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.profile == null -> {
                SkautaiErrorState(
                    message = uiState.error ?: "Profilis nerastas.",
                    onRetry = viewModel::loadProfile,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileHeader(
                        name = uiState.name,
                        surname = uiState.surname,
                        email = uiState.email
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Pagrindiniai duomenys",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            HorizontalDivider()
                            SkautaiTextField(
                                value = uiState.name,
                                onValueChange = viewModel::onNameChange,
                                label = "Vardas",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SkautaiTextField(
                                value = uiState.surname,
                                onValueChange = viewModel::onSurnameChange,
                                label = "Pavardė",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SkautaiTextField(
                                value = uiState.email,
                                onValueChange = viewModel::onEmailChange,
                                label = "El. paštas",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            SkautaiTextField(
                                value = uiState.phone,
                                onValueChange = viewModel::onPhoneChange,
                                label = "Telefono numeris",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                            SkautaiPrimaryButton(
                                text = if (uiState.isSavingProfile) "Saugoma..." else "Išsaugoti pakeitimus",
                                onClick = viewModel::saveProfile,
                                enabled = !uiState.isSavingProfile,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Slaptažodis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            HorizontalDivider()
                            SkautaiTextField(
                                value = uiState.currentPassword,
                                onValueChange = viewModel::onCurrentPasswordChange,
                                label = "Dabartinis slaptažodis",
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            SkautaiTextField(
                                value = uiState.newPassword,
                                onValueChange = viewModel::onNewPasswordChange,
                                label = "Naujas slaptažodis",
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            SkautaiTextField(
                                value = uiState.repeatPassword,
                                onValueChange = viewModel::onRepeatPasswordChange,
                                label = "Pakartokite naują slaptažodį",
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            SkautaiPrimaryButton(
                                text = if (uiState.isSavingPassword) "Keičiama..." else "Keisti slaptažodį",
                                onClick = viewModel::changePassword,
                                enabled = !uiState.isSavingPassword,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ProfileMetaRow("Sukurta", uiState.profile?.createdAt?.take(10).orEmpty())
                    ProfileMetaRow("Atnaujinta", uiState.profile?.updatedAt?.take(10).orEmpty())
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    surname: String,
    email: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = listOf(name, surname).filter { it.isNotBlank() }.joinToString(" "),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ProfileMetaRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
