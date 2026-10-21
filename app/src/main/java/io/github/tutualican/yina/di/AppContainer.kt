package io.github.tutualican.yina.di

import android.content.Context
import io.github.tutualican.yina.core.engine.AnkiCollectionEngine
import io.github.tutualican.yina.core.engine.CollectionEngine

/**
 * Minimal manual DI. There is exactly one engine per process because the
 * Anki backend owns a single open collection.
 */
object AppContainer {

    @Volatile
    private var engine: CollectionEngine? = null

    fun engine(context: Context): CollectionEngine =
        engine ?: synchronized(this) {
            engine ?: AnkiCollectionEngine(context.applicationContext).also { engine = it }
        }

    /** Test hook. */
    fun setEngineForTest(replacement: CollectionEngine?) {
        engine = replacement
    }
}
