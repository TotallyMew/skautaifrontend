package lt.skautai.android.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import lt.skautai.android.data.remote.DirectItemLoanDto
import lt.skautai.android.data.remote.ItemDto
import lt.skautai.android.data.remote.LocationDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorSnackbarHost
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.ui.common.itemConditionLabel
import lt.skautai.android.util.QrDestination
import lt.skautai.android.util.QrPayload

@Composable
fun InventoryQrScannerScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: InventoryQrScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isResolving by viewModel.isResolving.collectAsStateWithLifecycle()
    val scannedItem by viewModel.scannedItem.collectAsStateWithLifecycle()
    val directLoans by viewModel.directLoans.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var launchScan by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showWriteOffDialog by remember { mutableStateOf(false) }
    var returnLoan by remember { mutableStateOf<DirectItemLoanDto?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val parsed = QrPayload.parse(result.contents)
        when (parsed) {
            is QrDestination.ScanToken -> viewModel.resolveToken(parsed.token, onOpenItem)
            QrDestination.Unknown -> {
                viewModel.showMessage(
                    if (result.contents.isNullOrBlank()) {
                        "Skenavimas nutrauktas. Gali bandyti dar karta."
                    } else {
                        "Sitas kodas neatpazintas. Skenuok inventoriaus QR arba barkoda."
                    }
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchScan = true
        else viewModel.showMessage("Be kameros leidimo kodo nuskenuoti nepavyks.")
    }

    fun requestScan() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) launchScan = true else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(launchScan) {
        if (!launchScan) return@LaunchedEffect
        launchScan = false
        val options = ScanOptions().apply {
            setPrompt("Nukreipk kamera i inventoriaus QR arba barkoda")
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    LaunchedEffect(Unit) { requestScan() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SkautaiErrorSnackbarHost(hostState = snackbarHostState)
        when {
            scannedItem != null -> {
                QuickScanActions(
                    item = scannedItem!!,
                    directLoans = directLoans,
                    isWorking = isResolving,
                    onOpenItem = { viewModel.openScannedItem(onOpenItem) },
                    onScanAgain = {
                        viewModel.clearScannedItem()
                        requestScan()
                    },
                    onCondition = viewModel::updateCondition,
                    onLocation = { showLocationDialog = true },
                    onNote = { showNoteDialog = true },
                    onWriteOff = { showWriteOffDialog = true },
                    onReturnLoan = { returnLoan = it },
                    onBack = onBack
                )
            }

            message != null -> {
                SkautaiErrorState(
                    message = message ?: "",
                    onRetry = {
                        viewModel.clearMessage()
                        requestScan()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                SkautaiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Paruosiame skeneri", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Atidarysime kamera ir ieskosime inventoriaus QR arba barkodo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isResolving) CircularProgressIndicator()
                    }
                }
            }
        }
    }

    if (showLocationDialog && scannedItem != null) {
        QuickLocationDialog(
            locations = locations,
            isSubmitting = isResolving,
            onDismiss = { if (!isResolving) showLocationDialog = false },
            onConfirm = { locationId, label ->
                showLocationDialog = false
                viewModel.updateLocation(locationId, label)
            }
        )
    }

    if (showNoteDialog && scannedItem != null) {
        TextInputDialog(
            title = "Prideti pastaba",
            label = "Pastaba / incidentas",
            confirmLabel = "Issaugoti",
            isSubmitting = isResolving,
            onDismiss = { if (!isResolving) showNoteDialog = false },
            onConfirm = {
                showNoteDialog = false
                viewModel.addIncidentNote(it)
            }
        )
    }

    if (showWriteOffDialog && scannedItem != null) {
        TextInputDialog(
            title = "Nurasyti daikta",
            label = "Priezastis",
            confirmLabel = "Nurasyti",
            isSubmitting = isResolving,
            onDismiss = { if (!isResolving) showWriteOffDialog = false },
            onConfirm = {
                showWriteOffDialog = false
                viewModel.writeOff(it)
            }
        )
    }

    returnLoan?.let { loan ->
        ReturnLoanDialog(
            loan = loan,
            isSubmitting = isResolving,
            onDismiss = { if (!isResolving) returnLoan = null },
            onConfirm = { quantity ->
                returnLoan = null
                viewModel.returnDirectLoan(loan, quantity)
            }
        )
    }
}

@Composable
private fun QuickScanActions(
    item: ItemDto,
    directLoans: List<DirectItemLoanDto>,
    isWorking: Boolean,
    onOpenItem: () -> Unit,
    onScanAgain: () -> Unit,
    onCondition: (String) -> Unit,
    onLocation: () -> Unit,
    onNote: () -> Unit,
    onWriteOff: () -> Unit,
    onReturnLoan: (DirectItemLoanDto) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(
                                    "${item.quantity} ${item.unitOfMeasure}",
                                    itemConditionLabel(item.condition),
                                    item.locationPath ?: item.temporaryStorageLabel
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(onClick = onOpenItem, modifier = Modifier.fillMaxWidth()) {
                        Text("Atidaryti daikto kortele")
                    }
                }
            }
        }

        item {
            SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Greiti veiksmai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = false, onClick = { onCondition("DAMAGED") }, label = { Text("Sugadinta") })
                        FilterChip(selected = false, onClick = { onCondition("MISSING") }, label = { Text("Pamesta") })
                        FilterChip(selected = false, onClick = { onCondition("UNDER_REPAIR") }, label = { Text("Taisoma") })
                    }
                    OutlinedButton(onClick = onLocation, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Pakeisti vieta")
                    }
                    OutlinedButton(onClick = onNote, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Prideti komentara / incidenta")
                    }
                    OutlinedButton(onClick = onWriteOff, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Nurasyti")
                    }
                }
            }
        }

        item {
            SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tiesioginiai isdavimai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (directLoans.isEmpty()) {
                        Text("Sitas daiktas dabar nera tiesiogiai isduotas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(directLoans, key = { it.id }) { loan ->
            SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(loan.issuedToUserName ?: loan.issuedToUserId, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(
                            "Liko ${loan.outstandingQuantity} is ${loan.quantity}",
                            loan.dueAt?.take(10)?.let { "iki $it" },
                            loan.notes
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onReturnLoan(loan) }, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                        Text("Pazymeti grazinima")
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                    Text("Skenuoti kita")
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Grizti i inventoriu")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLocationDialog(
    locations: List<LocationDto>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LocationDto?>(locations.firstOrNull()) }
    var temporaryLabel by remember { mutableStateOf("") }
    var useTemporary by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pakeisti vieta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !useTemporary, onClick = { useTemporary = false }, label = { Text("Lokacija") })
                    FilterChip(selected = useTemporary, onClick = { useTemporary = true }, label = { Text("Laikina etikete") })
                }
                if (useTemporary) {
                    SkautaiTextField(
                        value = temporaryLabel,
                        onValueChange = { temporaryLabel = it },
                        label = "Pvz. deze pas Arna",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLocation?.fullPath.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Lokacija") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            locations.forEach { location ->
                                DropdownMenuItem(
                                    text = { Text(location.fullPath) },
                                    onClick = {
                                        selectedLocation = location
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    if (useTemporary) onConfirm(null, temporaryLabel) else onConfirm(selectedLocation?.id, null)
                }
            ) { Text("Issaugoti") }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Atsaukti") }
        }
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    confirmLabel: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            SkautaiTextField(
                value = text,
                onValueChange = { text = it },
                label = label,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = !isSubmitting, onClick = { onConfirm(text) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Atsaukti") }
        }
    )
}

@Composable
private fun ReturnLoanDialog(
    loan: DirectItemLoanDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityText by remember { mutableStateOf(loan.outstandingQuantity.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grazinti") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pas ${loan.issuedToUserName ?: loan.issuedToUserId}: ${loan.outstandingQuantity} vnt.")
                SkautaiTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter(Char::isDigit)
                        error = null
                    },
                    label = "Kiekis",
                    supportingText = error,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val quantity = quantityText.toIntOrNull()
                    when {
                        quantity == null || quantity < 1 -> error = "Iveskite teigiama kieki"
                        quantity > loan.outstandingQuantity -> error = "Kiekis per didelis"
                        else -> onConfirm(quantity)
                    }
                }
            ) { Text("Grazinti") }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Atsaukti") }
        }
    )
}
