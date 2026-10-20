package io.github.yina.core.engine

import android.content.Context
import android.net.Uri
import io.github.yina.R
import io.github.yina.core.model.CollectionSnapshot
import kotlinx.coroutines.delay

/**
 * Phase 0 placeholder. Keeps the whole app honest: the import button is wired,
 * the state machine is real, only the engine is missing.
 */
class StubCollectionEngine(private val context: Context) : CollectionEngine {

    override suspend fun open(): CollectionSnapshot = CollectionSnapshot(
        decks = emptyList(),
        isOpen = false,
    )

    override suspend fun importPackage(uri: Uri, displayName: String?): ImportResult {
        delay(400)
        return ImportResult.Failure(context.getString(R.string.import_not_ready))
    }

    override suspend fun close() = Unit
}
