package lt.skautai.android.ui.tuntas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import lt.skautai.android.data.remote.UserTuntasDto
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.util.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuntasSelectScreen(
    navController: NavController,
    viewModel: TuntasSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigateHome by viewModel.navigateHome.collectAsStateWithLifecycle()
    val navigateLogin by viewModel.navigateLogin.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var tuntasToLeave by remember { mutableStateOf<UserTuntasDto?>(null) }

    LaunchedEffect(navigateHome) {
        if (navigateHome) {
            navController.navigate(NavRoutes.Home.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            viewModel.onNavigatedHome()
        }
    }

    LaunchedEffect(navigateLogin) {
        if (navigateLogin) {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            viewModel.onNavigatedLogin()
        }
    }

    val successState = uiState as? TuntasSelectUiState.Success
    LaunchedEffect(successState?.message) {
        successState?.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    tuntasToLeave?.let { tuntas ->
        AlertDialog(
            onDismissRequest = { tuntasToLeave = null },
            title = { Text("Palikti tuntą?") },
            text = {
                Text("Tuntas nebus ištrintas iš sistemos. Bus uždaryta tik tavo prieiga prie šio tunto; vėl prisijungti galėsi tik gavęs pakvietimą.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tuntasToLeave = null
                        viewModel.leaveTuntas(tuntas.id)
                    }
                ) {
                    Text("Palikti")
                }
            },
            dismissButton = {
                TextButton(onClick = { tuntasToLeave = null }) {
                    Text("Atšaukti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tuntai ir kvietimai") },
                actions = {
                    TextButton(onClick = viewModel::logout) {
                        Text("Atsijungti")
                    }
                }
            )
        },
        snackbarHost = { SkautaiErrorSnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is TuntasSelectUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is TuntasSelectUiState.Empty -> {
                    EmptyTuntasContent(
                        inviteCode = "",
                        isAcceptingInvite = false,
                        onInviteCodeChange = viewModel::onInviteCodeChange,
                        onAcceptInvite = viewModel::acceptInvitation,
                        onRetry = viewModel::loadTuntai,
                        onLogout = viewModel::logout
                    )
                }

                is TuntasSelectUiState.Error -> {
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = viewModel::loadTuntai,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TuntasSelectUiState.Success -> {
                    if (state.tuntai.isEmpty()) {
                        EmptyTuntasContent(
                            inviteCode = state.inviteCode,
                            isAcceptingInvite = state.isAcceptingInvite,
                            onInviteCodeChange = viewModel::onInviteCodeChange,
                            onAcceptInvite = viewModel::acceptInvitation,
                            onRetry = viewModel::loadTuntai,
                            onLogout = viewModel::logout
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                        ) {
                            item {
                                InviteCard(
                                    inviteCode = state.inviteCode,
                                    isAcceptingInvite = state.isAcceptingInvite,
                                    onInviteCodeChange = viewModel::onInviteCodeChange,
                                    onAcceptInvite = viewModel::acceptInvitation
                                )
                            }
                            item {
                                Text(
                                    text = "Mano tuntai",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            items(state.tuntai, key = { it.id }) { tuntas ->
                                TuntasCard(
                                    tuntas = tuntas,
                                    selected = state.activeTuntasId == tuntas.id,
                                    isLeaving = state.isLeavingTuntas,
                                    onSelect = { viewModel.selectTuntas(tuntas.id) },
                                    onLeave = { tuntasToLeave = tuntas }
                                )
                            }
                            item {
                                OutlinedButton(onClick = viewModel::loadTuntai, modifier = Modifier.fillMaxWidth()) {
                                    Text("Atnaujinti")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTuntasContent(
    inviteCode: String,
    isAcceptingInvite: Boolean,
    onInviteCodeChange: (String) -> Unit,
    onAcceptInvite: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Neturi aktyvių tuntų",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Jei ką tik sukūrei tuntą, jis atsiras čia kaip laukiantis patvirtinimo. Atnaujink sąrašą arba priimk pakvietimą.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        InviteCard(
            inviteCode = inviteCode,
            isAcceptingInvite = isAcceptingInvite,
            onInviteCodeChange = onInviteCodeChange,
            onAcceptInvite = onAcceptInvite
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Atnaujinti")
        }
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Atsijungti")
        }
    }
}

@Composable
private fun InviteCard(
    inviteCode: String,
    isAcceptingInvite: Boolean,
    onInviteCodeChange: (String) -> Unit,
    onAcceptInvite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MarkEmailUnread, contentDescription = null)
                Text(
                    text = "Prisijungti su pakvietimu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            SkautaiTextField(
                value = inviteCode,
                onValueChange = onInviteCodeChange,
                label = "Pakvietimo kodas",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onAcceptInvite,
                enabled = !isAcceptingInvite && inviteCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAcceptingInvite) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text("Priimti pakvietimą")
                }
            }
        }
    }
}

@Composable
private fun TuntasCard(
    tuntas: UserTuntasDto,
    selected: Boolean,
    isLeaving: Boolean,
    onSelect: () -> Unit,
    onLeave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Flag,
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tuntas.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (tuntas.krastas.isNotBlank()) {
                        Text(
                            text = tuntas.krastas,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Text(
                            text = "Aktyvus tuntas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (tuntas.status == "PENDING") {
                        Text(
                            text = "Laukia patvirtinimo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (tuntas.status == "REJECTED") {
                        Text(
                            text = "Registracija atmesta",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isActive = tuntas.status == "ACTIVE"
                Button(
                    onClick = onSelect,
                    enabled = isActive && !selected && !isLeaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when {
                            selected -> "Pasirinktas"
                            isActive -> "Naudoti"
                            tuntas.status == "PENDING" -> "Laukia"
                            else -> "Negalima"
                        }
                    )
                }
                OutlinedButton(
                    onClick = onLeave,
                    enabled = !isLeaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Text("Palikti", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
