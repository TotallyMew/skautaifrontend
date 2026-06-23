package lt.skautai.android.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiPrimaryButton
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiSurfaceRole
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.skautaiOverlayTone
import lt.skautai.android.ui.common.skautaiSupportingTone
import lt.skautai.android.ui.common.skautaiSurfaceTone
import lt.skautai.android.BuildConfig

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val context = LocalContext.current

    if (uiState.showAccountDeletionDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideAccountDeletionDialog,
            title = { Text("Ištrinti paskyrą?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "El. paštu atsiųsime vienkartinę patvirtinimo nuorodą. " +
                            "Paskyra nebus ištrinta, kol nepatvirtinsite ištrynimo puslapyje."
                    )
                    Text(
                        "Šio veiksmo atšaukti negalima.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    SkautaiTextField(
                        value = uiState.accountDeletionPassword,
                        onValueChange = viewModel::onAccountDeletionPasswordChange,
                        label = "Dabartinis slaptažodis",
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::requestAccountDeletion,
                    enabled = !uiState.isRequestingAccountDeletion &&
                        uiState.accountDeletionPassword.isNotBlank()
                ) {
                    Text(
                        if (uiState.isRequestingAccountDeletion) "Siunčiama..." else "Siųsti patvirtinimą",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::hideAccountDeletionDialog,
                    enabled = !uiState.isRequestingAccountDeletion
                ) {
                    Text("Atšaukti")
                }
            }
        )
    }

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

                    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SkautaiSectionHeader(title = "Pagrindiniai duomenys")
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

                    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SkautaiSectionHeader(title = "Slaptažodis")
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
                            OutlinedButton(
                                onClick = viewModel::changePassword,
                                enabled = !uiState.isSavingPassword,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (uiState.isSavingPassword) "Keičiama..." else "Keisti slaptažodį")
                            }
                        }
                    }

                    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SkautaiSectionHeader(title = "Privatumas ir duomenys")
                            HorizontalDivider()
                            Text(
                                "Sužinokite, kokius duomenis programėlė tvarko, kodėl jie reikalingi, " +
                                    "kiek laiko saugomi ir kokias teises turite.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Privatumo politika")
                            }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_SENDTO,
                                            Uri.parse("mailto:${BuildConfig.PRIVACY_EMAIL}")
                                        ).putExtra(Intent.EXTRA_SUBJECT, "Privatumo klausimas")
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Kreiptis dėl privatumo")
                            }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_SENDTO,
                                            Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}")
                                        ).putExtra(Intent.EXTRA_SUBJECT, "Skautų inventoriaus pagalba")
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Susisiekti su pagalba")
                            }
                        }
                    }

                    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SkautaiSectionHeader(title = "Paskyros ištrynimas")
                            HorizontalDivider()
                            Text(
                                "Ištrynus paskyrą bus panaikinti prisijungimo ir asmeniniai duomenys, " +
                                    "aktyvios narystės bei rolės. Anonimizuoti veiksmų įrašai gali likti " +
                                    "bendro inventoriaus apskaitos vientisumui.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = viewModel::showAccountDeletionDialog,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ištrinti paskyrą", color = MaterialTheme.colorScheme.error)
                            }
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
    SkautaiCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = skautaiSurfaceTone(SkautaiSurfaceRole.Identity)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = skautaiOverlayTone(SkautaiSurfaceRole.Default)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = listOf(name, surname)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                            .joinToString("")
                            .take(2)
                            .ifBlank { "P" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = listOf(name, surname).filter { it.isNotBlank() }.joinToString(" "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = skautaiSupportingTone(SkautaiSurfaceRole.Identity)
                )
            }
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
