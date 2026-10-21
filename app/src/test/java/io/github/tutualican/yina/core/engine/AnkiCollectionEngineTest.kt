package io.github.tutualican.yina.core.engine

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.testing.RustBackendLoader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * End-to-end test against Anki's real Rust core on the host.
 *
 * Robolectric supplies the Android APIs; the `-testing` artifact supplies the
 * desktop build of librsdroid. This exercises exactly the same code path as the
 * phone: import -> deck tree -> queue -> render -> answer -> persist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
// Conscrypt's native library name is built with Locale.toLowerCase(), which
// breaks on a Turkish host ("windows" -> "wındows"). BouncyCastle is fine here.
@ConscryptMode(ConscryptMode.Mode.OFF)
class AnkiCollectionEngineTest {

    private lateinit var context: Context
    private lateinit var engine: AnkiCollectionEngine

    @Before
    fun setUp() {
        RustBackendLoader.ensureSetup()
        context = ApplicationProvider.getApplicationContext()
        engine = AnkiCollectionEngine(context)
    }

    @After
    fun tearDown() = runBlocking {
        engine.close()
    }

    @Test
    fun importsPackageAndAnswersCard() = runBlocking {
        val empty = engine.open()
        assertTrue("fresh collection opens", empty.isOpen.also { println("snapshot=$empty") })

        val apkg = copyResource("media.apkg")
        val imported = engine.importPackage(Uri.fromFile(apkg), "media.apkg")
        assertTrue("import succeeded: $imported", imported is ImportResult.Success)

        val snapshot = engine.open()
        println("decks=${snapshot.decks}")
        assertTrue("decks imported", snapshot.decks.isNotEmpty())
        assertTrue("cards imported", snapshot.totalCards > 0)

        val deck = snapshot.decks.maxBy { it.totalCards }
        val session = engine.startSession(deck.id)
        println("session=$session")

        val card = engine.nextCard()
        assertNotNull("queue returned a card", card)
        requireNotNull(card)
        println("card=${card.cardId} remaining=${card.remaining} options=${card.options}")
        println("html-prefix=${card.questionHtml.take(120).replace("\n", "\\n")}")
        assertTrue(
            "unexpected prefix: ${card.questionHtml.take(40)}",
            card.questionHtml.startsWith("<!DOCTYPE html>"),
        )
        assertTrue(card.questionHtml.contains("<body"))
        assertTrue("question has content", card.questionHtml.length > 500)
        assertTrue("answer side is rendered", card.answerHtml.length > 0)
        assertEquals(4, card.options.size)

        engine.answer(StudyRating.GOOD, millisecondsTaken = 1_200)
        // The queue may return the same card if it has learning steps, or the
        // next card. Either is valid; the important part is that answering
        // persisted and the queue still works.
        val after = engine.nextCard()
        println("after=${after?.cardId}")
        engine.close()

        val reopened = AnkiCollectionEngine(context)
        try {
            val persisted = reopened.open()
            println("persisted=${persisted.decks}")
            assertTrue(persisted.decks.isNotEmpty())
        } finally {
            reopened.close()
        }
    }

    @Test
    fun createsAddsRenamesAndDeletesDeck() = runBlocking {
        engine.open()
        val deckId = engine.createDeck("Yina management test")
        assertTrue("deck id was returned", deckId > 0)

        var snapshot = engine.open()
        val created = snapshot.decks.firstOrNull { it.id == deckId }
        assertNotNull("created deck is visible", created)
        assertEquals("new deck is empty", 0, created!!.totalCards)

        val noteId = engine.addBasicCard(deckId, "Front side", "Back side")
        assertTrue("note id was returned", noteId > 0)

        snapshot = engine.open()
        val withCard = snapshot.decks.first { it.id == deckId }
        assertEquals("one card was added", 1, withCard.totalCards)

        engine.renameDeck(deckId, "Yina renamed")
        snapshot = engine.open()
        assertTrue(
            "renamed deck kept its id",
            snapshot.decks.any { it.id == deckId && it.name == "Yina renamed" },
        )

        val removed = engine.deleteDeck(deckId)
        assertTrue("deleting the deck removed its card", removed >= 1)
        snapshot = engine.open()
        assertTrue("deleted deck is gone", snapshot.decks.none { it.id == deckId })
    }

    @Test
    fun deletesCurrentCardDuringReview() = runBlocking<Unit> {
        engine.open()
        val deckId = engine.createDeck("Yina card delete test")
        engine.addBasicCard(deckId, "Delete me", "Please")
        engine.startSession(deckId)

        assertNotNull("a new card is waiting", engine.nextCard())
        assertTrue("the pending card is deleted", engine.deleteCurrentCard())

        val snapshot = engine.open()
        assertEquals(
            "the deck is empty after deleting its only card",
            0,
            snapshot.decks.first { it.id == deckId }.totalCards,
        )
        assertTrue("there is no card left to delete", !engine.deleteCurrentCard())

        engine.deleteDeck(deckId)
    }

    @Test
    fun deletingParentDeckRemovesChildDecks() = runBlocking<Unit> {
        engine.open()
        val parentId = engine.createDeck("Yina parent")
        val childId = engine.createDeck("Yina parent::Yina child")
        engine.addBasicCard(childId, "Child front", "Child back")

        engine.deleteDeck(parentId)

        val snapshot = engine.open()
        assertTrue("parent deck is gone", snapshot.decks.none { it.id == parentId })
        assertTrue("child deck is gone", snapshot.decks.none { it.id == childId })
    }

    private fun copyResource(name: String): File {
        val target = File(context.cacheDir, name)
        javaClass.classLoader!!.getResourceAsStream(name)!!.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }

    @Test
    fun importsCollectionPackageAndReplacesCollection() = runBlocking {
        val apkg = copyResource("media.apkg")
        assertTrue(engine.importPackage(Uri.fromFile(apkg), "media.apkg") is ImportResult.Success)
        engine.close()

        val collection = File(context.filesDir, "collection.anki2")
        val media = File(context.filesDir, "collection.media").apply { mkdirs() }
        val mediaDb = File(context.filesDir, "collection.media.db")
        val exported = File(context.cacheDir, "yina-roundtrip.colpkg")

        // Export through the same backend so the fixture is a genuine colpkg.
        val exporter = Backend(listOf("en"))
        try {
            exporter.openCollection(collection.absolutePath, media.absolutePath, mediaDb.absolutePath)
            exporter.exportCollectionPackage(exported.absolutePath, true, false)
        } finally {
            runCatching { exporter.close() }
        }
        assertTrue("colpkg was written", exported.length() > 0)

        val result = engine.importPackage(Uri.fromFile(exported), "yina-roundtrip.colpkg")
        assertTrue("colpkg import succeeded: $result", result is ImportResult.Success)
        val snapshot = engine.open()
        assertTrue("deck survived colpkg import", snapshot.decks.isNotEmpty())
        assertTrue("cards survived colpkg import", snapshot.totalCards > 0)
    }
}
