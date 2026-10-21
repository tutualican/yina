package io.github.tutualican.yina.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tutualican.yina.BuildConfig
import io.github.tutualican.yina.R
import io.github.tutualican.yina.core.model.Deck
import io.github.tutualican.yina.di.AppContainer
import io.github.tutualican.yina.ui.components.pressable
import io.github.tutualican.yina.ui.theme.Motion
import io.github.tutualican.yina.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(
    onOpenDeck: (Long) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(AppContainer.engine(context), context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAbout by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }
    val isBusy = state.isImporting || state.isWorking

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onImportPicked(uri, queryDisplayName(context, uri))
        }
    }

    val pickPackage: () -> Unit = {
        importLauncher.launch(arrayOf("*/*"))
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    when (val active = dialog) {
        HomeDialog.NewDeck -> NewDeckDialog(
            existingNames = state.decks.map { it.name },
            isWorking = state.isWorking,
            onDismiss = { dialog = null },
            onCreate = { name ->
                dialog = null
                viewModel.createDeck(name)
            },
        )

        is HomeDialog.AddCard -> AddCardDialog(
            deck = active.deck,
            isWorking = state.isWorking,
            onDismiss = { dialog = null },
            onAdd = { front, back ->
                dialog = null
                viewModel.addCard(active.deck.id, front, back)
            },
        )

        is HomeDialog.RenameDeck -> RenameDeckDialog(
            deck = active.deck,
            existingNames = state.decks.map { it.name },
            isWorking = state.isWorking,
            onDismiss = { dialog = null },
            onRename = { name ->
                dialog = null
                viewModel.renameDeck(active.deck.id, name)
            },
        )

        is HomeDialog.DeleteDeck -> DeleteDeckDialog(
            deck = active.deck,
            isWorking = state.isWorking,
            onDismiss = { dialog = null },
            onDelete = {
                dialog = null
                viewModel.deleteDeck(active.deck.id)
            },
        )

        HomeDialog.Theme -> ThemeDialog(
            current = themeMode,
            onSelect = {
                onThemeModeChange(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }

    val startDeck = state.decks.firstOrNull { it.dueCards > 0 }
        ?: state.decks.firstOrNull()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Header(
                    onImport = pickPackage,
                    onTheme = { dialog = HomeDialog.Theme },
                )
            }

            item {
                FocusCard(
                    dueCount = state.totalDue,
                    isBusy = isBusy,
                    onStart = {
                        val deck = startDeck
                        if (deck != null) onOpenDeck(deck.id) else pickPackage()
                    },
                    onImport = pickPackage,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.decks_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.decks.isNotEmpty()) {
                        Text(
                            text = state.decks.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    RoundIconButton(
                        onClick = { dialog = HomeDialog.NewDeck },
                        description = stringResource(R.string.new_deck),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (state.decks.isEmpty()) {
                item {
                    EmptyState(
                        onImport = pickPackage,
                        onCreateDeck = { dialog = HomeDialog.NewDeck },
                    )
                }
            } else {
                items(state.decks, key = { it.id }) { deck ->
                    SwipeableDeckRow(
                        deck = deck,
                        onOpen = { onOpenDeck(deck.id) },
                        onStudy = { onOpenDeck(deck.id) },
                        onAddCard = { dialog = HomeDialog.AddCard(deck) },
                        onRename = { dialog = HomeDialog.RenameDeck(deck) },
                        onDelete = { dialog = HomeDialog.DeleteDeck(deck) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showAbout = true }) {
                    Text(
                        text = "${stringResource(R.string.about)} · v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    onImport: () -> Unit,
    onTheme: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            )
            Text(
                text = rememberTodayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RoundIconButton(
            onClick = onTheme,
            description = stringResource(R.string.theme_title),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        RoundIconButton(
            onClick = onImport,
            description = stringResource(R.string.import_content_description),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .pressable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun FocusCard(
    dueCount: Int,
    isBusy: Boolean,
    onStart: () -> Unit,
    onImport: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(container, container.copy(alpha = 0.55f), surface),
                    start = Offset.Zero,
                    end = Offset(900f, 1100f),
                ),
            )
            .padding(24.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.today_label).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.6.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )

            Spacer(Modifier.height(10.dp))

            if (dueCount > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = dueCount,
                        transitionSpec = {
                            (fadeIn(Motion.fade()) + scaleIn(initialScale = 0.94f, animationSpec = Motion.settle())) togetherWith
                                (fadeOut(Motion.fade()) + scaleOut(targetScale = 1.04f, animationSpec = Motion.quick()))
                        },
                        label = "due-count",
                    ) { count ->
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.due_now),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))

                PrimaryPill(
                    label = stringResource(R.string.start_focus),
                    icon = Icons.Filled.PlayArrow,
                    enabled = !isBusy,
                    onClick = onStart,
                )
            } else {
                Text(
                    text = stringResource(R.string.due_none),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.due_none_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
                Spacer(Modifier.height(18.dp))
                PrimaryPill(
                    label = stringResource(R.string.import_action),
                    icon = Icons.Filled.Add,
                    enabled = !isBusy,
                    onClick = onImport,
                )
            }
        }
    }
}

@Composable
private fun PrimaryPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.pressable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 18.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SwipeableDeckRow(
    deck: Deck,
    onOpen: () -> Unit,
    onStudy: () -> Unit,
    onAddCard: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val thresholdPx = with(LocalDensity.current) { Motion.SWIPE_THRESHOLD_DP.dp.toPx() }
    var offsetX by remember(deck.id) { mutableFloatStateOf(0f) }
    var menuOpen by remember(deck.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val reveal = (offsetX / thresholdPx).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(22.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(start = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .alpha(reveal),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.swipe_to_study),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(reveal),
            )
        }

        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(deck.id, thresholdPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(0f, thresholdPx * 1.7f)
                        },
                        onDragEnd = {
                            if (offsetX >= thresholdPx) {
                                offsetX = 0f
                                onStudy()
                            } else {
                                settleBack(offsetX, scope) { offsetX = it }
                            }
                        },
                        onDragCancel = {
                            settleBack(offsetX, scope) { offsetX = it }
                        },
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(onClick = onOpen)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = deck.name.take(1).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    val cardsLabel = pluralStringResource(
                        R.plurals.deck_card_count,
                        deck.totalCards,
                        deck.totalCards,
                    )
                    val dueLabel = if (deck.dueCards > 0) {
                        pluralStringResource(
                            R.plurals.deck_due_count,
                            deck.dueCards,
                            deck.dueCards,
                        )
                    } else {
                        null
                    }
                    Text(
                        text = if (dueLabel != null) "$cardsLabel · $dueLabel" else cardsLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(12.dp))

                if (deck.dueCards > 0) {
                    Text(
                        text = deck.dueCards.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.deck_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_card)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Add, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onAddCard()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename_deck)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onRename()
                            },
                        )
                        if (deck.id != 1L) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.delete_deck),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a dragged row to rest with a single spring. Kept as a helper so the
 * gesture code stays readable and the motion spec lives in one place.
 */
private fun settleBack(
    from: Float,
    scope: kotlinx.coroutines.CoroutineScope,
    onUpdate: (Float) -> Unit,
) {
    scope.launch {
        Animatable(from).animateTo(0f, Motion.settle()) {
            onUpdate(value)
        }
    }
}

@Composable
private fun EmptyState(
    onImport: () -> Unit,
    onCreateDeck: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(26.dp)) {
            Text(
                text = stringResource(R.string.empty_state_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_state_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            PrimaryPill(
                label = stringResource(R.string.new_deck),
                icon = Icons.Filled.Add,
                enabled = true,
                onClick = onCreateDeck,
            )
            TextButton(onClick = onImport) {
                Text(stringResource(R.string.import_action))
            }
        }
    }
}

private sealed interface HomeDialog {
    data object NewDeck : HomeDialog
    data class AddCard(val deck: Deck) : HomeDialog
    data class RenameDeck(val deck: Deck) : HomeDialog
    data class DeleteDeck(val deck: Deck) : HomeDialog
    data object Theme : HomeDialog
}

@Composable
private fun NewDeckDialog(
    existingNames: List<String>,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val cleanName = name.trim()
    val duplicate = existingNames.any { it.equals(cleanName, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_deck_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.deck_name_label)) },
                singleLine = true,
                isError = duplicate,
                supportingText = if (duplicate) {
                    { Text(stringResource(R.string.deck_name_exists)) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = cleanName.isNotEmpty() && !duplicate && !isWorking,
                onClick = { onCreate(cleanName) },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun AddCardDialog(
    deck: Deck,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
) {
    var front by remember(deck.id) { mutableStateOf("") }
    var back by remember(deck.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_card_title, deck.name)) },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text(stringResource(R.string.front_label)) },
                    minLines = 2,
                    maxLines = 5,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text(stringResource(R.string.back_label)) },
                    minLines = 3,
                    maxLines = 8,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = front.isNotBlank() && back.isNotBlank() && !isWorking,
                onClick = { onAdd(front.toAnkiField(), back.toAnkiField()) },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun RenameDeckDialog(
    deck: Deck,
    existingNames: List<String>,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember(deck.id) { mutableStateOf(deck.name) }
    val cleanName = name.trim()
    val duplicate = existingNames.any {
        !it.equals(deck.name, ignoreCase = true) &&
            it.equals(cleanName, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_deck_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.deck_name_label)) },
                singleLine = true,
                isError = duplicate,
                supportingText = if (duplicate) {
                    { Text(stringResource(R.string.deck_name_exists)) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = cleanName.isNotEmpty() && !duplicate && !isWorking,
                onClick = { onRename(cleanName) },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun DeleteDeckDialog(
    deck: Deck,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_deck_title)) },
        text = { Text(stringResource(R.string.delete_deck_message, deck.name)) },
        confirmButton = {
            TextButton(enabled = !isWorking, onClick = onDelete) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun ThemeDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_title)) },
        text = {
            Column {
                ThemeChoice(
                    label = stringResource(R.string.theme_system),
                    selected = current == ThemeMode.SYSTEM,
                    onClick = { onSelect(ThemeMode.SYSTEM) },
                )
                ThemeChoice(
                    label = stringResource(R.string.theme_light),
                    selected = current == ThemeMode.LIGHT,
                    onClick = { onSelect(ThemeMode.LIGHT) },
                )
                ThemeChoice(
                    label = stringResource(R.string.theme_dark),
                    selected = current == ThemeMode.DARK,
                    onClick = { onSelect(ThemeMode.DARK) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun ThemeChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * The simple editor is plain text. Anki stores fields as HTML, so escape the
 * user's text and turn line breaks into paragraphs instead of letting the
 * WebView interpret accidental markup.
 */
private fun String.toAnkiField(): String = trim()
    .replace("\r\n", "\n")
    .replace("\r", "")
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\n", "<br>")

private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? =
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.about_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun rememberTodayLabel(): String {
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.getDefault())
    }
    return remember(formatter) { LocalDate.now().format(formatter) }
}
