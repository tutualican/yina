package io.github.yina

import io.github.yina.core.model.CollectionSnapshot
import io.github.yina.core.model.Deck
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionSnapshotTest {

    @Test
    fun `totals are summed across decks`() {
        val snapshot = CollectionSnapshot(
            decks = listOf(
                Deck(id = 1, name = "A", totalCards = 100, dueCards = 12),
                Deck(id = 2, name = "B", totalCards = 50, dueCards = 8),
            ),
            isOpen = true,
        )

        assertEquals(150, snapshot.totalCards)
        assertEquals(20, snapshot.totalDue)
    }

    @Test
    fun `empty collection has zero totals`() {
        assertEquals(0, CollectionSnapshot().totalCards)
        assertEquals(0, CollectionSnapshot().totalDue)
    }
}
