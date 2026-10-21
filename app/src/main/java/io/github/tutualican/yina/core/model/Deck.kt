package io.github.tutualican.yina.core.model

/**
 * Lightweight projection of an Anki deck, used by the UI layer.
 *
 * Deliberately not the storage model: the real collection lives in Anki's
 * schema-18 SQLite database and is owned by the Rust core. Anything in this
 * file must stay cheap to build and free of Android dependencies.
 */
data class Deck(
    val id: Long,
    val name: String,
    val totalCards: Int,
    val dueCards: Int,
    val newCards: Int = 0,
) {
    val isFinishedForToday: Boolean get() = dueCards == 0

    val progressLabel: String
        get() = "$totalCards kart · $dueCards tekrar"
}

data class CollectionSnapshot(
    val decks: List<Deck> = emptyList(),
    val isOpen: Boolean = false,
) {
    val totalCards: Int get() = decks.sumOf { it.totalCards }
    val totalDue: Int get() = decks.sumOf { it.dueCards }
}
