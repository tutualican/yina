package io.github.yina.core.engine

/** The four answers Anki understands. */
enum class StudyRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

/**
 * One answer button. [interval] comes from Anki itself
 * (`describeNextStates`), so it always matches the deck's scheduler settings
 * and language.
 */
data class AnswerOption(
    val rating: StudyRating,
    val interval: String,
)

/**
 * A card that is ready to be shown. HTML is already a complete document with
 * the notetype CSS; the UI only needs to hand it to a WebView.
 */
data class StudyCard(
    val cardId: Long,
    val deckName: String,
    val questionHtml: String,
    val answerHtml: String,
    val mediaBaseUrl: String,
    val options: List<AnswerOption>,
    val remaining: Int,
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
)

data class SessionInfo(
    val deckId: Long,
    val deckName: String,
)
