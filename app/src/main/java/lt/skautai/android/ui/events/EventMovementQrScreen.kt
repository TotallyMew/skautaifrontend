package lt.skautai.android.ui.events

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import lt.skautai.android.data.remote.EventInventoryCustodyDto
import lt.skautai.android.data.remote.EventInventoryItemDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiErrorState
import lt.skautai.android.ui.common.SkautaiTextField
import lt.skautai.android.util.QrDestination
import lt.skautai.android.util.QrPayload

private enum class EventQrMode {
    Item,
    Custody
}

private enum class ItemQrAction {
    Checkout,
    Assign
}

private enum class CustodyQrAction {
    Transfer,
    Return
}

@Composable
fun EventMovementQrScreen(
    eventId: String,
    mode: String,
    onBack: () -> Unit,
    viewModel: EventMovementQrViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val qrMode = remember(mode) { if (mode.equals("custody", ignoreCase = true)) EventQrMode.Custody else EventQrMode.Item }

    var launchScan by remember { mutableStateOf(false) }
    var resolvedItemId by remember { mutableStateOf<String?>(null) }
    var selectedEventItemId by remember { mutableStateOf<String?>(null) }
    var selectedCustodyId by remember { mutableStateOf<String?>(null) }
    var itemAction by remember { mutableStateOf(ItemQrAction.Checkout) }
    var custodyAction by remember { mutableStateOf(CustodyQrAction.Return) }
    var quantityText by remember { mutableStateOf("1") }
    var selectedPastovykleId by remember { mutableStateOf<String?>(null) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedReturnType by remember { mutableStateOf("RETURN_TO_EVENT_STORAGE") }
    var notes by remember { mutableStateOf("") }

    fun resetSelection() {
        resolvedItemId = null
        selectedEventItemId = null
        selectedCustodyId = null
        itemAction = ItemQrAction.Checkout
        custodyAction = CustodyQrAction.Return
        quantityText = "1"
        selectedPastovykleId = null
        selectedUserId = null
        selectedReturnType = "RETURN_TO_EVENT_STORAGE"
        notes = ""
    }

    LaunchedEffect(eventId) {
        viewModel.load(eventId)
    }

    LaunchedEffect((uiState as? EventMovementQrUiState.Success)?.message) {
        val message = (uiState as? EventMovementQrUiState.Success)?.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val parsed = QrPayload.parse(result.contents)
        when (parsed) {
            is QrDestination.ScanToken -> viewModel.resolveToken(parsed.token) { itemId ->
                resetSelection()
                resolvedItemId = itemId
            }
            QrDestination.Unknown -> {
                viewModel.showMessage(
                    if (result.contents.isNullOrBlank()) {
                        "Skenavimas nutrauktas."
                    } else {
                        "Šis QR kodas neatpažintas."
                    }
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScan = true
        } else {
            viewModel.showMessage("Be kameros leidimo QR kodo nuskenuoti nepavyks.")
        }
    }

    LaunchedEffect(launchScan) {
        if (!launchScan) return@LaunchedEffect
        launchScan = false
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(
                if (qrMode == EventQrMode.Item) {
                    "Skenuok daikto QR kodą paėmimui arba išdavimui"
                } else {
                    "Skenuok daikto QR kodą perdavimui arba grąžinimui"
                }
            )
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    val title = if (qrMode == EventQrMode.Item) "Pasiimti / Išduoti" else "Perduoti / Grąžinti"

    EventScreenScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is EventMovementQrUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EventMovementQrUiState.Error -> {
                    SkautaiErrorState(
                        message = state.message,
                        onRetry = { viewModel.load(eventId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is EventMovementQrUiState.Success -> {
                    val myRoles = state.event.eventRoles
                        .filter { it.userId == state.currentUserId }
                        .map { it.role }
                        .toSet()
                    val readOnly = isEventReadOnlyStatus(state.event.status)
                    val canManage = !readOnly &&
                        ("events.inventory.distribute:ALL" in permissions ||
                            myRoles.any { it in setOf("VIRSININKAS", "KOMENDANTAS", "UKVEDYS") })
                    val coLeaderPastovykleIds = state.event.eventRoles
                        .filter { it.role == "PASTOVYKLES_GURU" && it.userId == state.currentUserId && it.pastovykleId != null }
                        .mapNotNull { it.pastovykleId }
                        .toSet()
                    val responsiblePastovykleIds = state.pastovykles
                        .filter { it.responsibleUserId == state.currentUserId || it.id in coLeaderPastovykleIds }
                        .map { it.id }
                        .toSet()
                    val itemMatches = state.inventoryPlan.items.filter { it.itemId == resolvedItemId }
                    val eventItemById = state.inventoryPlan.items.associateBy { it.id }
                    val custodyMatches = state.custody.filter { row ->
                        row.status == "OPEN" &&
                            row.remainingQuantity > 0 &&
                            eventItemById[row.eventInventoryItemId]?.itemId == resolvedItemId
                    }

                    LaunchedEffect(resolvedItemId, qrMode) {
                        if (resolvedItemId == null) return@LaunchedEffect
                        when (qrMode) {
                            EventQrMode.Item -> {
                                selectedEventItemId = itemMatches.firstOrNull()?.id
                                if (itemMatches.isEmpty()) {
                                    viewModel.showMessage("Šis daiktas šiame renginyje dar nėra įtrauktas į inventoriaus planą.")
                                }
                            }
                            EventQrMode.Custody -> {
                                selectedCustodyId = custodyMatches.firstOrNull()?.id
                                if (custodyMatches.isEmpty()) {
                                    viewModel.showMessage("Šiam daiktui renginyje nėra aktyvaus perdavimo ar paėmimo įrašo.")
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (qrMode == EventQrMode.Item) {
                                        "Skenuok daikto QR ir pasirink veiksmą: pasiimti arba išduoti."
                                    } else {
                                        "Skenuok jau judėjime esantį daiktą ir pasirink: perduoti arba grąžinti."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilledTonalButton(onClick = {
                                        viewModel.clearMessage()
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            launchScan = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }) {
                                        Text("Skenuoti QR")
                                    }
                                    Button(onClick = {
                                        resetSelection()
                                        viewModel.clearMessage()
                                    }) {
                                        Text("Valyti")
                                    }
                                }
                                if (state.isResolving) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        if (qrMode == EventQrMode.Item && itemMatches.isNotEmpty()) {
                            ItemQrActionCard(
                                matches = itemMatches,
                                pastovykles = state.pastovykles,
                                members = state.members,
                                canManage = canManage,
                                responsiblePastovykleIds = responsiblePastovykleIds,
                                selectedEventItemId = selectedEventItemId,
                                onSelectedEventItemId = { selectedEventItemId = it },
                                action = itemAction,
                                onActionChange = { itemAction = it },
                                quantityText = quantityText,
                                onQuantityTextChange = { quantityText = it.filter(Char::isDigit) },
                                selectedPastovykleId = selectedPastovykleId,
                                onSelectedPastovykleId = { selectedPastovykleId = it },
                                selectedUserId = selectedUserId,
                                onSelectedUserId = { selectedUserId = it },
                                notes = notes,
                                onNotesChange = { notes = it },
                                isWorking = state.isWorking,
                                onSubmit = {
                                    val selectedItem = itemMatches.firstOrNull { it.id == selectedEventItemId }
                                    val quantity = quantityText.toIntOrNull()
                                    if (selectedItem == null || quantity == null || quantity <= 0) {
                                        viewModel.showMessage("Pasirink daiktą ir teisingą kiekį.")
                                        return@ItemQrActionCard
                                    }
                                    val request = when (itemAction) {
                                        ItemQrAction.Checkout -> viewModel.newRequest(
                                            eventInventoryItemId = selectedItem.id,
                                            movementType = "CHECKOUT_TO_PERSON",
                                            quantity = quantity,
                                            pastovykleId = selectedPastovykleId,
                                            toUserId = if (canManage || selectedPastovykleId in responsiblePastovykleIds) selectedUserId else null,
                                            notes = notes.ifBlank { null }
                                        )
                                        ItemQrAction.Assign -> {
                                            if (selectedPastovykleId == null) {
                                                viewModel.showMessage("Išdavimui pasirink pastovyklę.")
                                                return@ItemQrActionCard
                                            }
                                            viewModel.newRequest(
                                                eventInventoryItemId = selectedItem.id,
                                                movementType = "ASSIGN_TO_PASTOVYKLE",
                                                quantity = quantity,
                                                pastovykleId = selectedPastovykleId,
                                                notes = notes.ifBlank { null }
                                            )
                                        }
                                    }
                                    viewModel.createMovement(eventId, request) {
                                        resetSelection()
                                        viewModel.showMessage("Judėjimas užregistruotas.")
                                    }
                                }
                            )
                        }

                        if (qrMode == EventQrMode.Custody && custodyMatches.isNotEmpty()) {
                            CustodyQrActionCard(
                                matches = custodyMatches,
                                pastovykles = state.pastovykles,
                                members = state.members,
                                canManage = canManage,
                                responsiblePastovykleIds = responsiblePastovykleIds,
                                selectedCustodyId = selectedCustodyId,
                                onSelectedCustodyId = { selectedCustodyId = it },
                                action = custodyAction,
                                onActionChange = { custodyAction = it },
                                quantityText = quantityText,
                                onQuantityTextChange = { quantityText = it.filter(Char::isDigit) },
                                selectedPastovykleId = selectedPastovykleId,
                                onSelectedPastovykleId = { selectedPastovykleId = it },
                                selectedUserId = selectedUserId,
                                onSelectedUserId = { selectedUserId = it },
                                selectedReturnType = selectedReturnType,
                                onSelectedReturnType = { selectedReturnType = it },
                                notes = notes,
                                onNotesChange = { notes = it },
                                isWorking = state.isWorking,
                                onSubmit = {
                                    val selectedCustody = custodyMatches.firstOrNull { it.id == selectedCustodyId }
                                    val quantity = quantityText.toIntOrNull()
                                    if (selectedCustody == null || quantity == null || quantity <= 0) {
                                        viewModel.showMessage("Pasirink įrašą ir teisingą kiekį.")
                                        return@CustodyQrActionCard
                                    }
                                    val request = when (custodyAction) {
                                        CustodyQrAction.Transfer -> {
                                        if (!canManage && selectedCustody.pastovykleId !in responsiblePastovykleIds) {
                                            viewModel.showMessage("Perdavimui reikia platesnių teisių.")
                                            return@CustodyQrActionCard
                                        }
                                            val targetPastovykleId = selectedPastovykleId ?: selectedUserId?.let {
                                                selectedCustody.pastovykleId
                                            }
                                            if (targetPastovykleId == null && selectedUserId == null) {
                                                viewModel.showMessage("Perdavimui pasirink bent pastovyklę arba žmogų.")
                                                return@CustodyQrActionCard
                                            }
                                            viewModel.newRequest(
                                                eventInventoryItemId = selectedCustody.eventInventoryItemId,
                                                movementType = "TRANSFER",
                                                quantity = quantity,
                                                pastovykleId = targetPastovykleId,
                                                toUserId = selectedUserId,
                                                fromCustodyId = selectedCustody.id,
                                                notes = notes.ifBlank { null }
                                            )
                                        }
                                        CustodyQrAction.Return -> viewModel.newRequest(
                                            eventInventoryItemId = selectedCustody.eventInventoryItemId,
                                            movementType = selectedReturnType,
                                            quantity = quantity,
                                            fromCustodyId = selectedCustody.id,
                                            notes = notes.ifBlank { null }
                                        )
                                    }
                                    viewModel.createMovement(eventId, request) {
                                        resetSelection()
                                        viewModel.showMessage("Judėjimas užregistruotas.")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemQrActionCard(
    matches: List<EventInventoryItemDto>,
    pastovykles: List<lt.skautai.android.data.remote.PastovykleDto>,
    members: List<lt.skautai.android.data.remote.MemberDto>,
    canManage: Boolean,
    responsiblePastovykleIds: Set<String>,
    selectedEventItemId: String?,
    onSelectedEventItemId: (String) -> Unit,
    action: ItemQrAction,
    onActionChange: (ItemQrAction) -> Unit,
    quantityText: String,
    onQuantityTextChange: (String) -> Unit,
    selectedPastovykleId: String?,
    onSelectedPastovykleId: (String?) -> Unit,
    selectedUserId: String?,
    onSelectedUserId: (String?) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isWorking: Boolean,
    onSubmit: () -> Unit
) {
    val selectedItem = matches.firstOrNull { it.id == selectedEventItemId } ?: matches.first()
    val canSelectTargetUser = canManage || selectedPastovykleId in responsiblePastovykleIds
    LaunchedEffect(matches) {
        if (selectedEventItemId == null) onSelectedEventItemId(matches.first().id)
    }

    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Nuskenuotas daiktas renginyje", style = MaterialTheme.typography.titleMedium)
            DropdownField(
                label = "Inventoriaus įrašas",
                value = selectedItem.name,
                options = matches.map { it.id to "${it.name} (${it.availableQuantity}/${it.plannedQuantity})" },
                onSelect = onSelectedEventItemId
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { onActionChange(ItemQrAction.Checkout) }, modifier = Modifier.weight(1f)) {
                    Text("Pasiimti")
                }
                if (canManage) {
                    FilledTonalButton(onClick = { onActionChange(ItemQrAction.Assign) }, modifier = Modifier.weight(1f)) {
                        Text("Išduoti")
                    }
                }
            }
            SkautaiTextField(
                value = quantityText,
                onValueChange = onQuantityTextChange,
                label = "Kiekis",
                modifier = Modifier.fillMaxWidth()
            )
            if (action == ItemQrAction.Checkout) {
                DropdownField(
                    label = "Iš kur",
                    value = pastovykles.firstOrNull { it.id == selectedPastovykleId }?.name ?: "Renginio sandėlis",
                    options = listOf("" to "Renginio sandėlis") + pastovykles.map { it.id to it.name },
                    onSelect = { onSelectedPastovykleId(it.ifBlank { null }) }
                )
                if (canSelectTargetUser) {
                    DropdownField(
                        label = "Kam",
                        value = members.firstOrNull { it.userId == selectedUserId }?.fullName() ?: "Sau",
                        options = listOf("" to "Sau") + members.map { it.userId to it.fullName() },
                        onSelect = { onSelectedUserId(it.ifBlank { null }) }
                    )
                }
            } else {
                DropdownField(
                    label = "Pastovyklei",
                    value = pastovykles.firstOrNull { it.id == selectedPastovykleId }?.name ?: "Pasirinkti",
                    options = pastovykles.map { it.id to it.name },
                    onSelect = { onSelectedPastovykleId(it) }
                )
            }
            SkautaiTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = "Pastabos",
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSubmit, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                Text(if (action == ItemQrAction.Checkout) "Registruoti paėmimą" else "Registruoti išdavimą")
            }
        }
    }
}

@Composable
private fun CustodyQrActionCard(
    matches: List<EventInventoryCustodyDto>,
    pastovykles: List<lt.skautai.android.data.remote.PastovykleDto>,
    members: List<lt.skautai.android.data.remote.MemberDto>,
    canManage: Boolean,
    responsiblePastovykleIds: Set<String>,
    selectedCustodyId: String?,
    onSelectedCustodyId: (String) -> Unit,
    action: CustodyQrAction,
    onActionChange: (CustodyQrAction) -> Unit,
    quantityText: String,
    onQuantityTextChange: (String) -> Unit,
    selectedPastovykleId: String?,
    onSelectedPastovykleId: (String?) -> Unit,
    selectedUserId: String?,
    onSelectedUserId: (String?) -> Unit,
    selectedReturnType: String,
    onSelectedReturnType: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isWorking: Boolean,
    onSubmit: () -> Unit
) {
    val selectedCustody = matches.firstOrNull { it.id == selectedCustodyId } ?: matches.first()
    val returnOptions = buildReturnOptions(selectedCustody)
    val canTransfer = canManage || selectedCustody.pastovykleId in responsiblePastovykleIds
    val effectiveAction = if (action == CustodyQrAction.Transfer && !canTransfer) CustodyQrAction.Return else action

    LaunchedEffect(matches) {
        if (selectedCustodyId == null) onSelectedCustodyId(matches.first().id)
    }
    LaunchedEffect(selectedCustody.id) {
        onSelectedPastovykleId(null)
        onSelectedUserId(null)
        onSelectedReturnType(returnOptions.first().first)
    }

    SkautaiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Aktyvus judėjimo įrašas", style = MaterialTheme.typography.titleMedium)
            DropdownField(
                label = "Įrašas",
                value = custodyLabel(selectedCustody),
                options = matches.map { it.id to custodyLabel(it) },
                onSelect = onSelectedCustodyId
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canTransfer) {
                    FilledTonalButton(onClick = { onActionChange(CustodyQrAction.Transfer) }, modifier = Modifier.weight(1f)) {
                        Text("Perduoti")
                    }
                }
                FilledTonalButton(onClick = { onActionChange(CustodyQrAction.Return) }, modifier = Modifier.weight(1f)) {
                    Text("Grąžinti")
                }
            }
            SkautaiTextField(
                value = quantityText,
                onValueChange = onQuantityTextChange,
                label = "Kiekis",
                modifier = Modifier.fillMaxWidth()
            )
            if (effectiveAction == CustodyQrAction.Transfer) {
                DropdownField(
                    label = "Tikslinė pastovykla",
                    value = pastovykles.firstOrNull { it.id == selectedPastovykleId }?.name ?: "Nekeisti / nėra",
                    options = listOf("" to "Nekeisti / nėra") + pastovykles.map { it.id to it.name },
                    onSelect = { onSelectedPastovykleId(it.ifBlank { null }) }
                )
                DropdownField(
                    label = "Tikslinis žmogus",
                    value = members.firstOrNull { it.userId == selectedUserId }?.fullName() ?: "Nepasirinkta",
                    options = listOf("" to "Nepasirinkta") + members.map { it.userId to it.fullName() },
                    onSelect = { onSelectedUserId(it.ifBlank { null }) }
                )
            } else {
                DropdownField(
                    label = "Grąžinimo tipas",
                    value = returnOptions.firstOrNull { it.first == selectedReturnType }?.second ?: returnOptions.first().second,
                    options = returnOptions,
                    onSelect = onSelectedReturnType
                )
            }
            SkautaiTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = "Pastabos",
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSubmit, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                Text(if (effectiveAction == CustodyQrAction.Transfer) "Registruoti perdavimą" else "Registruoti grąžinimą")
            }
        }
    }
}

private fun custodyLabel(custody: EventInventoryCustodyDto): String {
    val holder = listOfNotNull(custody.pastovykleName, custody.holderUserName).joinToString(" / ")
    return "${custody.itemName} • ${holder.ifBlank { "Renginio sandėlis" }} • ${custody.remainingQuantity}/${custody.quantity}"
}

private fun buildReturnOptions(custody: EventInventoryCustodyDto): List<Pair<String, String>> {
    return when {
        custody.holderUserId != null && custody.pastovykleId != null -> listOf(
            "RETURN_TO_PASTOVYKLE" to "Grąžinti į pastovyklę",
            "RETURN_TO_EVENT_STORAGE" to "Grąžinti į renginio sandėlį"
        )
        else -> listOf("RETURN_TO_EVENT_STORAGE" to "Grąžinti į renginio sandėlį")
    }
}
