package io.github.yina.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.yina.R
import io.github.yina.core.engine.CollectionEngine
import io.github.yina.core.engine.ImportResult
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
        if (_state.value.isImporting) return
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
