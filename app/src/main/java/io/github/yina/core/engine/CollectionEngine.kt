package io.github.yina.core.engine

import android.net.Uri
import io.github.yina.core.model.CollectionSnapshot

/**
 * Boundary between the Android UI and the Anki-compatible engine.
 *
 * Implementation plan (docs/ARCHITECTURE.md):
 *  - Phase 1: [StubCollectionEngine] (this file's sibling) keeps the UI alive.
 *  - Phase 2: [RustCollectionEngine] wraps Anki's `rslib` through UniFFI.
 *
 * Nothing outside `core.engine` may touch the collection file directly.
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

    /** Flushes pending writes and releases the database handle. */
    suspend fun close()
}

sealed interface ImportResult {
    data class Success(val deckCount: Int, val cardCount: Int) : ImportResult
    data class Failure(val reason: String) : ImportResult
}
