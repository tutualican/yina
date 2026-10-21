package io.github.tutualican.yina.core.engine

import android.net.Uri
import io.github.tutualican.yina.core.model.CollectionSnapshot

/**
 * Boundary between the Android UI and Anki's engine.
 *
 * The implementation talks to Anki's real Rust core (the same backend
 * AnkiDroid uses) through its protobuf/JNI bridge. Nothing outside
 * `core.engine` may touch the collection file directly.
 */
interface CollectionEngine {

    /** Opens the local collection and returns a UI projection. */
    suspend fun open(): CollectionSnapshot

    /**
     * Imports a `.apkg` / `.colpkg` picked by the user.
     *
     * @param uri content:// URI granted by the Storage Access Framework.
     * @param displayName best-effort filename for logs and error messages.
     */
    suspend fun importPackage(uri: Uri, displayName: String?): ImportResult

    /**
     * Creates a normal deck and returns its id.
     *
     * Anki treats `::` in a name as the deck hierarchy separator, so
     * `Languages::Japanese` creates the parent as needed.
     */
    suspend fun createDeck(name: String): Long

    /**
     * Adds one basic note to [deckId].
     *
     * [front] and [back] are Anki field HTML. The caller decides whether the
     * user's input is plain text or formatted HTML.
     */
    suspend fun addBasicCard(deckId: Long, front: String, back: String): Long

    /** Renames a deck, preserving its id and cards. */
    suspend fun renameDeck(deckId: Long, name: String)

    /**
     * Deletes a deck and the cards Anki removes with it.
     *
     * @return the number of cards Anki removed.
     */
    suspend fun deleteDeck(deckId: Long): Int

    /**
     * Selects [deckId] and starts a review session.
     *
     * Anki keeps the selected deck in the collection, so this also makes the
     * deck the current one for subsequent queue requests.
     */
    suspend fun startSession(deckId: Long): SessionInfo

    /**
     * Fetches and renders the next card from Anki's queue.
     *
     * Returns null when the queue is empty. Each call advances the queue, so
     * the UI should only call it after an answer or at session start.
     */
    suspend fun nextCard(): StudyCard?

    /** Answers the currently pending card with Anki's own scheduler. */
    suspend fun answer(rating: StudyRating, millisecondsTaken: Long)

    /**
     * Deletes the card currently waiting for an answer.
     *
     * Returns false when there is no pending card. Anki keeps the note, so a
     * note that produces several cards only loses the card being reviewed.
     */
    suspend fun deleteCurrentCard(): Boolean

    /** Undoes the most recent answer. Returns false if there was nothing to undo. */
    suspend fun undo(): Boolean

    /** Flushes pending writes and releases the database handle. */
    suspend fun close()
}

sealed interface ImportResult {
    data class Success(val deckCount: Int, val cardCount: Int) : ImportResult
    data class Failure(val reason: String) : ImportResult
}
