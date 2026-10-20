package io.github.yina.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.yina.core.engine.CollectionEngine
import io.github.yina.core.engine.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(private val engine: CollectionEngine) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val snapshot = engine.open()
            _state.update {
                it.copy(
                    isLoading = false,
                    decks = snapshot.decks,
                    totalDue = snapshot.totalDue,
                )
            }
        }
    }

    fun onImportPicked(uri: Uri, displayName: String?) {
        if (_state.value.isImporting) return
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            val result = engine.importPackage(uri, displayName)
            _state.update { it.copy(isImporting = false) }
            when (result) {
                is ImportResult.Success -> {
                    _state.update {
                        it.copy(message = "${result.cardCount} kart, ${result.deckCount} deste içe aktarıldı.")
                    }
                    refresh()
                }

                is ImportResult.Failure -> _state.update { it.copy(message = result.reason) }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    companion object {
        fun factory(engine: CollectionEngine): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(engine) }
        }
    }
}
