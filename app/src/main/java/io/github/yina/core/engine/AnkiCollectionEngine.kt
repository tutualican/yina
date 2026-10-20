package io.github.yina.core.engine

import android.content.Context
import android.net.Uri
import android.os.Build
import android.system.Os
import anki.card_rendering.RenderCardResponse
import anki.decks.DeckTreeNode
import anki.scheduler.CardAnswer
import anki.scheduler.SchedulingStates
import io.github.yina.R
import io.github.yina.core.model.CollectionSnapshot
import io.github.yina.core.model.Deck
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ankiweb.rsdroid.Backend

/**
 * The real engine: Anki's Rust core, compiled for Android and packaged by
 * AnkiDroid, driven directly through its protobuf backend.
 *
 * Scheduling, queue building, deck limits, FSRS and persistence are all Anki's
 * own code. This class only moves data between that core and the UI.
 *
 * Every backend call is serialized with a mutex. The bridge is fast, but it is
 * not re-entrant, and letting two screens answer at once would corrupt the
 * queue.
 */
class AnkiCollectionEngine(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : CollectionEngine {

    private val appContext = context.applicationContext

    private val collectionFile = File(appContext.filesDir, "collection.anki2")
    private val mediaDir = File(appContext.filesDir, "collection.media")
    private val mediaDb = File(appContext.filesDir, "collection.media.db")

    private val mutex = Mutex()

    private var backend: Backend? = null
    private var currentDeckId: Long = 0L
    private var currentDeckName: String = ""
    private var pending: PendingCard? = null

    private data class PendingCard(
        val cardId: Long,
        val states: SchedulingStates,
    )

    override suspend fun open(): CollectionSnapshot = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureOpen()
            readDecks()
        }
    }

    override suspend fun importPackage(uri: Uri, displayName: String?): ImportResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val file = copyToCache(uri) ?: return@withLock ImportResult.Failure("")
                try {
                    ensureOpen()
                    val value = backend ?: error(appContext.getString(R.string.study_error))
                    val isCollectionPackage =
                        (displayName ?: uri.toString()).endsWith(".colpkg", ignoreCase = true)

                    if (isCollectionPackage) {
                        value.closeCollection(false)
                        try {
                            value.importCollectionPackage(
                                collectionFile.absolutePath,
                                file.absolutePath,
                                mediaDir.absolutePath,
                                mediaDb.absolutePath,
                            )
                        } finally {
                            value.openCollection(
                                collectionFile.absolutePath,
                                mediaDir.absolutePath,
                                mediaDb.absolutePath,
                            )
                        }
                    } else {
                        value.importAnkiPackage(
                            file.absolutePath,
                            value.getImportAnkiPackagePresets(),
                        )
                    }

                    pending = null
                    val root = value.deckTree(clock())
                    val decks = mapDecks(root)
                    ImportResult.Success(
                        deckCount = countDecks(root),
                        cardCount = decks.sumOf { it.totalCards },
                    )
                } catch (t: Throwable) {
                    ImportResult.Failure(friendlyError(t))
                } finally {
                    file.delete()
                }
            }
        }

    override suspend fun startSession(deckId: Long): SessionInfo = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureOpen()
            val value = backend ?: error(appContext.getString(R.string.study_error))
            value.setCurrentDeck(deckId)
            currentDeckId = deckId
            currentDeckName = runCatching { value.getDeck(deckId).name }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: appContext.getString(R.string.app_name)
            pending = null
            SessionInfo(deckId = deckId, deckName = currentDeckName)
        }
    }

    override suspend fun nextCard(): StudyCard? = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureOpen()
            val value = backend ?: return@withLock null
            val queue = value.getQueuedCards(1, false)
            if (queue.cardsCount == 0) {
                pending = null
                return@withLock null
            }
            val queued = queue.getCards(0)
            val states = queued.states
            val cardId = queued.card.id
            val rendered = value.renderExistingCard(
                cardId = cardId,
                browser = false,
                partialRender = false,
            )
            pending = PendingCard(cardId = cardId, states = states)

            val described = runCatching { value.describeNextStates(states) }
                .getOrDefault(emptyList())
            val deckName = queued.context.deckName
                .takeIf { it.isNotBlank() }
                ?: currentDeckName

            StudyCard(
                cardId = cardId,
                deckName = deckName,
                questionHtml = renderDocument(
                    nodes = rendered,
                    answerSide = false,
                    cardClass = "card card${queued.card.templateIdx + 1}",
                    css = rendered.css,
                ),
                answerHtml = renderDocument(
                    nodes = rendered,
                    answerSide = true,
                    cardClass = "card card${queued.card.templateIdx + 1}",
                    css = rendered.css,
                ),
                mediaBaseUrl = Uri.fromFile(mediaDir).toString() + "/",
                options = StudyRating.entries.mapIndexed { index, rating ->
                    AnswerOption(
                        rating = rating,
                        interval = described.getOrNull(index)?.toPlainLabel().orEmpty(),
                    )
                },
                remaining = queue.newCount + queue.learningCount + queue.reviewCount,
                newCount = queue.newCount,
                learningCount = queue.learningCount,
                reviewCount = queue.reviewCount,
            )
        }
    }

    override suspend fun answer(rating: StudyRating, millisecondsTaken: Long) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val value = backend ?: error(appContext.getString(R.string.study_error))
                val card = pending ?: return@withLock
                val newState = when (rating) {
                    StudyRating.AGAIN -> card.states.again
                    StudyRating.HARD -> card.states.hard
                    StudyRating.GOOD -> card.states.good
                    StudyRating.EASY -> card.states.easy
                }
                value.answerCard(
                    CardAnswer.newBuilder()
                        .setCardId(card.cardId)
                        .setCurrentState(card.states.current)
                        .setNewState(newState)
                        .setRating(rating.toBackend())
                        .setAnsweredAtMillis(clock())
                        .setMillisecondsTaken(
                            millisecondsTaken.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                        )
                        .build(),
                )
                pending = null
            }
        }

    override suspend fun undo(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val value = backend ?: return@withLock false
            try {
                value.undo()
                pending = null
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            pending = null
            currentDeckId = 0L
            currentDeckName = ""
            val value = backend
            backend = null
            if (value != null) {
                runCatching { value.closeCollection(false) }
                runCatching { value.close() }
            }
        }
    }

    // ---------------------------------------------------------------- internals

    private fun ensureOpen() {
        if (backend?.isOpen() == true && collectionFile.exists()) {
            return
        }
        if (backend != null) {
            // The backend object exists but no collection is open (for example
            // after a failed import). Reuse it.
            backend?.openCollection(
                collectionFile.absolutePath,
                mediaDir.absolutePath,
                mediaDb.absolutePath,
            )
            return
        }

        collectionFile.parentFile?.mkdirs()
        mediaDir.mkdirs()

        loadNativeLibrary()
        val languages = backendLanguages()
        val value = Backend(languages)
        try {
            value.openCollection(
                collectionFile.absolutePath,
                mediaDir.absolutePath,
                mediaDb.absolutePath,
            )
        } catch (t: Throwable) {
            runCatching { value.close() }
            throw t
        }
        backend = value
        if (currentDeckId == 0L) {
            currentDeckId = runCatching { value.getCurrentDeck().id }.getOrDefault(0L)
        }
        if (currentDeckName.isEmpty()) {
            currentDeckName = runCatching { value.getCurrentDeck().name }.getOrDefault("")
        }
    }

    private fun readDecks(): CollectionSnapshot {
        val value = backend ?: return CollectionSnapshot()
        val root = value.deckTree(clock())
        return CollectionSnapshot(
            decks = mapDecks(root),
            isOpen = true,
        )
    }

    private fun mapDecks(root: DeckTreeNode): List<Deck> =
        root.childrenList.mapNotNull { node ->
            val emptyDefault = node.deckId == 1L &&
                node.totalIncludingChildren == 0 &&
                node.name.equals("Default", ignoreCase = true)
            if (emptyDefault) {
                null
            } else {
                Deck(
                    id = node.deckId,
                    name = node.name,
                    totalCards = node.totalIncludingChildren,
                    dueCards = node.reviewCount + node.learnCount + node.newCount,
                    newCards = node.newCount,
                )
            }
        }

    private fun countDecks(node: DeckTreeNode): Int =
        node.childrenList.sumOf { child ->
            1 + countDecks(child)
        }

    private fun copyToCache(uri: Uri): File? {
        val name = uri.lastPathSegment
        val extension = when {
            name?.endsWith(".colpkg", ignoreCase = true) == true -> ".colpkg"
            else -> ".apkg"
        }
        return try {
            val target = File(appContext.cacheDir, "yina-${UUID.randomUUID()}$extension")
            val input = appContext.contentResolver.openInputStream(uri) ?: return null
            input.use { source ->
                target.outputStream().use { output -> source.copyTo(output) }
            }
            target
        } catch (_: Throwable) {
            null
        }
    }

    private fun renderDocument(
        nodes: RenderCardResponse,
        answerSide: Boolean,
        cardClass: String,
        css: String,
    ): String {
        val raw = if (answerSide) {
            nodes.answerNodesList.joinToString("") { it.asHtml() }
        } else {
            nodes.questionNodesList.joinToString("") { it.asHtml() }
        }
        val body = raw
            // Anki's desktop webview serves media from `/_media/`; a phone
            // WebView can load the files directly from the media folder.
            .replace("_media/", "")
            // There is no audio pipeline yet; showing the raw marker would be
            // worse than hiding it.
            .replace(Regex("\\[sound:[^\\]]*\\]"), "")

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
              <style>
                :root { color-scheme: light dark; }
                html, body { margin: 0; padding: 0; }
                body.card {
                  box-sizing: border-box;
                  min-height: 100vh;
                  padding: 22px 18px 30px;
                  font-family: system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans", sans-serif;
                  font-size: 19px;
                  line-height: 1.55;
                  color: #1A1D1B;
                  background: transparent;
                  text-align: center;
                  overflow-wrap: anywhere;
                  -webkit-text-size-adjust: 100%;
                }
                body.card.nightMode { color: #E8E6E0; }
                img, video { max-width: 100%; height: auto; }
                audio { max-width: 100%; }
                hr#answer {
                  border: 0;
                  border-top: 1px solid rgba(0, 0, 0, 0.12);
                  margin: 22px 0;
                }
                body.card.nightMode hr#answer {
                  border-top-color: rgba(255, 255, 255, 0.14);
                }
                $css
              </style>
            </head>
            <body class="$cardClass">
              $body
            </body>
            </html>
        """.trimIndent().trimStart()
    }

    private fun loadNativeLibrary() {
        if (loaded) return
        if (Build.FINGERPRINT == "robolectric") {
            // Host tests load the desktop library through
            // RustBackendLoader.ensureSetup() before constructing the engine.
            loaded = true
            return
        }
        // SQLite and the Rust core both need a writable temp directory.
        val inherited = runCatching { Os.getenv("TMPDIR") }.getOrNull()
        if (inherited.isNullOrBlank()) {
            runCatching { Os.setenv("TMPDIR", appContext.cacheDir.absolutePath, true) }
        }
        runCatching { Os.setenv("PLATFORM", "android", false) }
        System.loadLibrary("rsdroid")
        loaded = true
    }

    private fun backendLanguages(): List<String> {
        val language = Locale.getDefault().language.ifBlank { "en" }
        return if (language == "en") listOf("en") else listOf(language, "en")
    }

    private fun friendlyError(t: Throwable): String {
        val raw = t.message?.trim().orEmpty()
        return when {
            raw.isEmpty() -> ""
            raw.length > 260 -> raw.take(260)
            else -> raw
        }
    }

    companion object {
        @Volatile
        private var loaded = false
    }
}

private fun StudyRating.toBackend(): CardAnswer.Rating = when (this) {
    StudyRating.AGAIN -> CardAnswer.Rating.AGAIN
    StudyRating.HARD -> CardAnswer.Rating.HARD
    StudyRating.GOOD -> CardAnswer.Rating.GOOD
    StudyRating.EASY -> CardAnswer.Rating.EASY
}

private fun anki.card_rendering.RenderedTemplateNode.asHtml(): String = when {
    hasText() -> text
    hasReplacement() -> replacement.currentText
    else -> ""
}

private fun String.toPlainLabel(): String = this
    .replace(Regex("<[^>]*>"), "")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&amp;", "&")
    .trim()

