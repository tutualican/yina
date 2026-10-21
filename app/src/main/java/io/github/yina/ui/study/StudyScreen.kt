package io.github.yina.ui.study

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.yina.R
import io.github.yina.core.engine.AnswerOption
import io.github.yina.core.engine.StudyRating
import io.github.yina.di.AppContainer
import io.github.yina.ui.components.pressable
import io.github.yina.ui.theme.Motion
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun StudyScreen(
    deckId: Long,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: StudyViewModel = viewModel(
        key = "study-$deckId",
        factory = StudyViewModel.factory(AppContainer.engine(context), deckId, context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    LaunchedEffect(state.message, state.card) {
        val message = state.message
        if (message != null && state.card != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    BackHandler(onBack = onExit)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            StudyTopBar(
                deckName = state.deckName,
                remaining = state.remaining,
                canUndo = state.canUndo && !state.isCompleted,
                onBack = onExit,
                onUndo = viewModel::undo,
            )

            AnimatedContent(
                targetState = when {
                    state.isCompleted -> StudyPhase.DONE
                    state.card == null && state.isLoading -> StudyPhase.LOADING
                    state.card == null -> StudyPhase.ERROR
                    else -> StudyPhase.CARD
                },
                transitionSpec = {
                    fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade())
                },
                label = "study-phase",
                modifier = Modifier.fillMaxSize(),
            ) { phase ->
                when (phase) {
                    StudyPhase.LOADING -> LoadingPhase()
                    StudyPhase.ERROR -> ErrorPhase(
                        message = state.message ?: stringResource(R.string.study_error),
                        onRetry = viewModel::retry,
                        onExit = onExit,
                    )

                    StudyPhase.DONE -> DonePhase(
                        studied = state.studied,
                        onExit = onExit,
                    )

                    StudyPhase.CARD -> {
                        val card = state.card
                        if (card != null) {
                            CardPhase(
                                state = state,
                                onReveal = viewModel::revealAnswer,
                                onAnswer = viewModel::answer,
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class StudyPhase { LOADING, ERROR, CARD, DONE }

@Composable
private fun StudyTopBar(
    deckName: String,
    remaining: Int,
    canUndo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .size(44.dp)
                .pressable(onClick = onBack),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.study_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = deckName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (remaining > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.study_remaining,
                        remaining,
                        remaining,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(
            modifier = Modifier.width(92.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (canUndo) {
                TextButton(onClick = onUndo) {
                    Text(
                        text = stringResource(R.string.study_undo),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardPhase(
    state: StudyUiState,
    onReveal: () -> Unit,
    onAnswer: (StudyRating) -> Unit,
) {
    val card = state.card ?: return
    val dark = isSystemInDarkTheme()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            CardWebView(
                html = if (state.showAnswer) card.answerHtml else card.questionHtml,
                baseUrl = card.mediaBaseUrl,
                dark = dark,
                scrollToAnswer = state.showAnswer,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            AnimatedContent(
                targetState = state.showAnswer,
                transitionSpec = {
                    fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade())
                },
                label = "answer-controls",
            ) { revealed ->
                if (revealed) {
                    RatingRow(
                        options = card.options,
                        enabled = !state.isAnswering,
                        onAnswer = onAnswer,
                    )
                } else {
                    RevealButton(
                        enabled = !state.isAnswering,
                        onClick = onReveal,
                    )
                }
            }
        }
    }
}

@Composable
private fun RevealButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .pressable(enabled = enabled, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.study_show_answer),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RatingRow(
    options: List<AnswerOption>,
    enabled: Boolean,
    onAnswer: (StudyRating) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            RatingButton(
                option = option,
                enabled = enabled,
                onClick = { onAnswer(option.rating) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RatingButton(
    option: AnswerOption,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = when (option.rating) {
        StudyRating.AGAIN -> colors.error.copy(alpha = 0.12f) to colors.error
        StudyRating.HARD -> colors.tertiaryContainer to colors.onTertiaryContainer
        StudyRating.GOOD -> colors.primary to colors.onPrimary
        StudyRating.EASY -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    val label = when (option.rating) {
        StudyRating.AGAIN -> stringResource(R.string.study_again)
        StudyRating.HARD -> stringResource(R.string.study_hard)
        StudyRating.GOOD -> stringResource(R.string.study_good)
        StudyRating.EASY -> stringResource(R.string.study_easy)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = background,
        contentColor = foreground,
        modifier = modifier
            .heightIn(min = 66.dp)
            .pressable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (option.interval.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = option.interval,
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.78f),
                    maxLines = 1,
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CardWebView(
    html: String,
    baseUrl: String,
    dark: Boolean,
    scrollToAnswer: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val answerState = rememberUpdatedState(scrollToAnswer)
    val darkState = rememberUpdatedState(dark)
    var loadedHtml by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webView = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(backgroundColor)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = false
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        val nightClass = if (darkState.value) "true" else "false"
                        val scroll = if (answerState.value) {
                            "const a=document.getElementById('answer');" +
                                "if(a){a.scrollIntoView({block:'start'})}else{" +
                                "window.scrollTo(0,document.body.scrollHeight)};"
                        } else {
                            ""
                        }
                        view.evaluateJavascript(
                            "document.body.classList.toggle('nightMode',$nightClass);" +
                                "document.body.style.background='transparent';$scroll",
                            null,
                        )
                    }
                }
                webView = this
            }
        },
        update = { view ->
            if (loadedHtml != html) {
                loadedHtml = html
                view.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
            }
            view.setBackgroundColor(backgroundColor)
            if (loadedHtml == html) {
                val nightClass = if (dark) "true" else "false"
                view.evaluateJavascript(
                    "document.body.classList.toggle('nightMode',$nightClass);",
                    null,
                )
            }
        },
    )
}

@Composable
private fun LoadingPhase() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun ErrorPhase(
    message: String,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.study_error_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.pressable(onClick = onRetry),
        ) {
            Text(
                text = stringResource(R.string.study_retry),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onExit) {
            Text(stringResource(R.string.study_back_home))
        }
    }
}

@Composable
private fun DonePhase(
    studied: Int,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.study_done_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (studied > 0) {
                pluralStringResource(R.plurals.study_done_body, studied, studied)
            } else {
                stringResource(R.string.due_none)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .pressable(onClick = onExit),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.study_back_home),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
