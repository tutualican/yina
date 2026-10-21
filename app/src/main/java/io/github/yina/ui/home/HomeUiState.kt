package io.github.yina.ui.home

import io.github.yina.core.model.Deck

data class HomeUiState(
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val isWorking: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val totalDue: Int = 0,
    val message: String? = null,
)
