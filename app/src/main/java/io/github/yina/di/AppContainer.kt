package io.github.yina.di

import android.content.Context
import io.github.yina.core.engine.CollectionEngine
import io.github.yina.core.engine.StubCollectionEngine

/**
 * Minimal manual DI. Swap [StubCollectionEngine] for the Rust-backed engine
 * here once it exists; no other call site should change.
 */
object AppContainer {

    @Volatile
    private var engine: CollectionEngine? = null

    fun engine(context: Context): CollectionEngine =
        engine ?: synchronized(this) {
            engine ?: StubCollectionEngine(context.applicationContext).also { engine = it }
        }

    /** Test hook. */
    fun setEngineForTest(replacement: CollectionEngine?) {
        engine = replacement
    }
}
