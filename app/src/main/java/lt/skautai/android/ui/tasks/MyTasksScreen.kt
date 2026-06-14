package lt.skautai.android.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import lt.skautai.android.data.remote.LeadershipChangeRequestDto
import lt.skautai.android.data.remote.MemberDto
import lt.skautai.android.data.remote.MyTaskDto
import lt.skautai.android.ui.common.SkautaiCard
import lt.skautai.android.ui.common.SkautaiEmptyState
import lt.skautai.android.ui.common.SkautaiSectionHeader
import lt.skautai.android.ui.common.SkautaiStatusPill
import lt.skautai.android.ui.common.SkautaiStatusTone

@Composable
fun MyTasksScreen(
    navController: NavController,
    viewModel: MyTasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    when (val state = uiState) {
        MyTasksUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        MyTasksUiState.Empty -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                SkautaiEmptyState(
                    title = "Šiuo metu užduočių nėra",
                    subtitle = "Kai atsiras tvirtinimų, grąžinimų ar stebėtinų srautų, jie pasirodys čia.",
                    icon = Icons.Default.Flag
                )
            }
        }
        is MyTasksUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                SkautaiEmptyState(
                    title = "Nepavyko užkrauti užduočių",
                    subtitle = state.message,
                    icon = Icons.Default.Flag
                )
            }
        }
        is MyTasksUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                state.actionError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                LeadershipChangeReviewSection(
                    requests = state.leadershipChangeRequests,
                    members = state.members,
                    isSaving = state.isSaving,
                    onApprove = viewModel::approveLeadershipChange,
                    onReject = viewModel::rejectLeadershipChange
                )
                TaskBucketSection("URGENT", "Skubu", "Vėluojantys ar blokuojantys darbai.", state.tasks, navController)
                TaskBucketSection("TODAY", "Šiandien", "Veiksmai, kuriuos verta užbaigti šiandien.", state.tasks, navController)
                TaskBucketSection("NEXT", "Toliau", "Artimiausi sprendimai ir peržiūros.", state.tasks, navController)
                TaskBucketSection("WATCH", "Stebėti", "Atviri srautai, kuriuos verta sekti.", state.tasks, navController)
            }
        }
    }
}

@Composable
private fun LeadershipChangeReviewSection(
    requests: List<LeadershipChangeRequestDto>,
    members: List<MemberDto>,
    isSaving: Boolean,
    onApprove: (String, String) -> Unit,
    onReject: (String) -> Unit
) {
    if (requests.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkautaiSectionHeader(
            title = "Vadovų pasikeitimai",
            subtitle = "Pasirink pakeitėją iš to vieneto narių sąrašo"
        )
        requests.forEach { request ->
            val candidates = members.filter { member ->
                member.userId != request.requesterUserId &&
                    member.unitAssignments.orEmpty().any { it.organizationalUnitId == request.organizationalUnitId }
            }
            SkautaiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${request.requesterName} nori atsistatydinti",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${request.organizationalUnitName} · ${request.roleName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    request.reason?.takeIf { it.isNotBlank() }?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                    if (candidates.isEmpty()) {
                        Text(
                            text = "Šiame vienete nėra kito nario, kurį galima paskirti. Pirmiausia pakviesk arba perkelk narį į vienetą.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            candidates.forEach { candidate ->
                                Button(
                                    onClick = { onApprove(request.id, candidate.userId) },
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Paskirti ${candidate.name} ${candidate.surname}")
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onReject(request.id) }, enabled = !isSaving) {
                            Text("Atmesti")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyTaskPreviewList(
    tasks: List<MyTaskDto>,
    onTaskClick: (MyTaskDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tasks.forEach { task ->
            MyTaskTile(task = task, onClick = { onTaskClick(task) })
        }
    }
}

@Composable
fun MyTaskTile(
    task: MyTaskDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkautaiCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        tonal = MaterialTheme.colorScheme.surfaceBright
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = task.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                task.dueLabel()?.let { label ->
                    SkautaiStatusPill(label = label, tone = task.urgencyTone())
                }
                task.count?.let { count ->
                    SkautaiStatusPill(label = "$count", tone = task.urgencyTone())
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskBucketSection(
    bucket: String,
    title: String,
    subtitle: String,
    tasks: List<MyTaskDto>,
    navController: NavController
) {
    val bucketTasks = tasks.filter { it.bucket == bucket }
    if (bucketTasks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkautaiSectionHeader(title = title, subtitle = subtitle)
        MyTaskPreviewList(
            tasks = bucketTasks,
            onTaskClick = { navController.navigate(it.routeTarget) }
        )
    }
}

private fun MyTaskDto.icon(): ImageVector = when (type) {
    "INVENTORY_APPROVAL_PENDING" -> Icons.Default.PendingActions
    "RESERVATION_APPROVAL_PENDING", "RESERVATION_MOVEMENT_OPEN" -> Icons.Default.EventAvailable
    "MY_RETURN_OVERDUE", "MY_RETURN_DUE_TODAY" -> Icons.Default.Inventory2
    "REQUISITION_REVIEW_PENDING" -> Icons.Default.Assignment
    "EVENT_PACKING_GENERATE", "EVENT_PACKING_OPEN", "EVENT_PACKING_RETURN_OPEN" -> Icons.Default.Inventory2
    "EVENT_LOGISTICS_OPEN", "EVENT_RECONCILIATION_OPEN" -> Icons.Default.EventNote
    else -> Icons.Default.Flag
}

private fun MyTaskDto.urgencyTone(): SkautaiStatusTone = when (urgency) {
    "CRITICAL" -> SkautaiStatusTone.Danger
    "HIGH" -> SkautaiStatusTone.Warning
    "MEDIUM" -> SkautaiStatusTone.Info
    else -> SkautaiStatusTone.Neutral
}

private fun MyTaskDto.dueLabel(): String? {
    val due = dueAt ?: return null
    return try {
        val dueDate = Instant.parse(due).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        when {
            dueDate.isBefore(today) -> "Vėluoja"
            dueDate.isEqual(today) -> "Šiandien"
            dueDate.isEqual(today.plusDays(1)) -> "Rytoj"
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
