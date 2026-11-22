package com.mindvault.feature.flashcards

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.DECK_ID_ARG
import com.mindvault.core.navigation.STUDY_SHUFFLE_ARG
import com.mindvault.domain.model.StudySession
import com.mindvault.domain.usecase.ObserveDeckDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "StudyVM"

/**
 * Practice mode: takes one snapshot of the deck when the screen opens and runs the session on it.
 * Practice records nothing (no ratings, no review count, no schedule change); the session logic lives in
 * [StudySession]. Rated reviews use the Review screen.
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDeckDetail: ObserveDeckDetailUseCase,
) : ViewModel() {

    private val deckId: String = checkNotNull(savedStateHandle.get<String>(DECK_ID_ARG))
    private val startShuffled: Boolean = savedStateHandle.get<Boolean>(STUDY_SHUFFLE_ARG) ?: false
    private val random: Random = Random.Default

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val detail = observeDeckDetail(deckId).first()
                _uiState.value = when {
                    detail == null -> StudyUiState.Error
                    detail.cards.isEmpty() -> StudyUiState.Empty(detail.deck.name)
                    else -> StudyUiState.Active(
                        deckName = detail.deck.name,
                        session = StudySession.start(detail.cards, startShuffled, random),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load the deck for practice", e)
                _uiState.value = StudyUiState.Error
            }
        }
    }

    fun onFlip() = updateSession { it.flip() }
    fun onNext() = updateSession { it.next() }
    fun onPrevious() = updateSession { it.previous() }
    fun onToggleShuffle() = updateSession { it.withShuffle(!it.isShuffled, random) }
    fun onRestart() = updateSession { it.restart(shuffle = it.isShuffled, random = random) }

    private fun updateSession(transform: (StudySession) -> StudySession) {
        val current = _uiState.value as? StudyUiState.Active ?: return
        _uiState.value = current.copy(session = transform(current.session), message = null)
    }
}