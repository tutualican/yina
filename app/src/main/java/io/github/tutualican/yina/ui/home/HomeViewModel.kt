package io.github.tutualican.yina.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.tutualican.yina.R
import io.github.tutualican.yina.core.engine.CollectionEngine
import io.github.tutualican.yina.core.engine.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val engine: CollectionEngine,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            runCatching { engine.open() }
                .onSuccess { snapshot ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            decks = snapshot.decks,
                            totalDue = snapshot.totalDue,
                        )
                    }
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

    fun onImportPicked(uri: Uri, displayName: String?) {
        if (_state.value.isImporting || _state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            val result = runCatching { engine.importPackage(uri, displayName) }
                .getOrElse { ImportResult.Failure(it.message ?: "") }
            _state.update { it.copy(isImporting = false) }
            when (result) {
                is ImportResult.Success -> {
                    _state.update {
                        it.copy(
                            message = appContext.getString(
                                R.string.import_success,
                                result.cardCount,
                                result.deckCount,
                            ),
                        )
                    }
                    refresh()
                }

                is ImportResult.Failure -> _state.update {
                    it.copy(
                        message = appContext.getString(
                            R.string.import_failed,
                            result.reason.ifBlank {
                                appContext.getString(R.string.import_failed_generic)
                            },
                        ),
                    )
                }
            }
        }
    }

    fun createDeck(name: String) {
        runOperation(R.string.deck_created) { engine.createDeck(name) }
    }

    fun addCard(deckId: Long, front: String, back: String) {
        runOperation(R.string.card_added) { engine.addBasicCard(deckId, front, back) }
    }

    fun renameDeck(deckId: Long, name: String) {
        runOperation(R.string.deck_renamed) { engine.renameDeck(deckId, name) }
    }

    fun deleteDeck(deckId: Long) {
        runOperation(R.string.deck_deleted) { engine.deleteDeck(deckId) }
    }

    private fun runOperation(
        successMessage: Int,
        operation: suspend () -> Any?,
    ) {
        if (_state.value.isWorking || _state.value.isImporting) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            val result = runCatching { operation() }
            _state.update { it.copy(isWorking = false) }
            result
                .onSuccess {
                    _state.update {
                        it.copy(message = appContext.getString(successMessage))
                    }
                    refresh()
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            message = appContext.getString(
                                R.string.action_failed,
                                t.message?.takeIf { reason -> reason.isNotBlank() }
                                    ?: appContext.getString(R.string.action_failed_generic),
                            ),
                        )
                    }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    companion object {
        fun factory(
            engine: CollectionEngine,
            context: Context,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(engine, context.applicationContext) }
        }
    }
}
