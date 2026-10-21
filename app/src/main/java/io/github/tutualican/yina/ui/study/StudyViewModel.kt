package io.github.tutualican.yina.ui.study

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.tutualican.yina.R
import io.github.tutualican.yina.core.engine.CollectionEngine
import io.github.tutualican.yina.core.engine.StudyCard
import io.github.tutualican.yina.core.engine.StudyRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyUiState(
    val deckName: String = "",
    val card: StudyCard? = null,
    val showAnswer: Boolean = false,
    val remaining: Int = 0,
    val studied: Int = 0,
    val isLoading: Boolean = true,
    val isAnswering: Boolean = false,
    val isCompleted: Boolean = false,
    val canUndo: Boolean = false,
    val message: String? = null,
)

class StudyViewModel(
    private val engine: CollectionEngine,
    private val deckId: Long,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private var cardShownAt = SystemClock.elapsedRealtime()

    /** Starts (or restarts) the review session for this screen entry. */
    fun start() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    message = null,
                    card = null,
                    showAnswer = false,
                    remaining = 0,
                    isCompleted = false,
                    studied = 0,
                    canUndo = false,
                )
            }
            runCatching { engine.startSession(deckId) }
                .onSuccess { info ->
                    _state.update { it.copy(deckName = info.deckName) }
                    loadNext()
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = t.message ?: appContext.getString(R.string.study_error),
                        )
                    }
                }
        }
    }

    fun revealAnswer() {
        if (_state.value.card == null || _state.value.showAnswer) return
        _state.update { it.copy(showAnswer = true) }
    }

    fun answer(rating: StudyRating) {
        val current = _state.value
        if (current.card == null || current.isAnswering) return
        val elapsed = SystemClock.elapsedRealtime() - cardShownAt
        _state.update { it.copy(isAnswering = true) }
        viewModelScope.launch {
            runCatching { engine.answer(rating, elapsed) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            studied = it.studied + 1,
                            canUndo = true,
                        )
                    }
                    loadNext()
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            isAnswering = false,
                            message = t.message ?: appContext.getString(R.string.study_error),
                        )
                    }
                }
        }
    }

    fun deleteCurrentCard() {
        val current = _state.value
        if (current.card == null || current.isAnswering) return
        _state.update { it.copy(isAnswering = true) }
        viewModelScope.launch {
            val deleted = runCatching { engine.deleteCurrentCard() }.getOrDefault(false)
            if (deleted) {
                _state.update { it.copy(canUndo = false) }
                loadNext(appContext.getString(R.string.card_deleted))
            } else {
                _state.update {
                    it.copy(
                        isAnswering = false,
                        message = appContext.getString(R.string.study_delete_failed),
                    )
                }
            }
        }
    }

    fun undo() {
        if (_state.value.isAnswering || _state.value.studied == 0) return
        _state.update { it.copy(isAnswering = true) }
        viewModelScope.launch {
            val undone = runCatching { engine.undo() }.getOrDefault(false)
            if (undone) {
                _state.update {
                    it.copy(
                        studied = (it.studied - 1).coerceAtLeast(0),
                        canUndo = it.studied - 1 > 0,
                    )
                }
                loadNext()
            } else {
                _state.update {
                    it.copy(
                        isAnswering = false,
                        message = appContext.getString(R.string.study_undo_failed),
                    )
                }
            }
        }
    }

    fun retry() {
        if (_state.value.card == null) {
            start()
        } else {
            viewModelScope.launch { loadNext() }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private suspend fun loadNext(message: String? = null) {
        runCatching { engine.nextCard() }
            .onSuccess { card ->
                cardShownAt = SystemClock.elapsedRealtime()
                val finished = card == null
                _state.update {
                    it.copy(
                        card = card,
                        showAnswer = false,
                        remaining = card?.remaining ?: 0,
                        isLoading = false,
                        isAnswering = false,
                        isCompleted = finished,
                        message = message,
                    )
                }
            }
            .onFailure { t ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isAnswering = false,
                        message = t.message ?: appContext.getString(R.string.study_error),
                    )
                }
            }
    }

    companion object {
        fun factory(
            engine: CollectionEngine,
            deckId: Long,
            context: Context,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { StudyViewModel(engine, deckId, context.applicationContext) }
        }
    }
}
