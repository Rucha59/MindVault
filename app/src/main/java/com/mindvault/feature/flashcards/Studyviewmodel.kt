package com.mindvault.feature.flashcards

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.DECK_ID_ARG
import com.mindvault.core.navigation.STUDY_SHUFFLE_ARG
import com.mindvault.domain.model.StudySession
import com.mindvault.domain.usecase.ObserveDeckDetailUseCase
import com.mindvault.domain.usecase.RecordCardReviewUseCase
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
 * Takes one snapshot of the deck when the screen opens and runs the session on it. Each card's
 * first reveal in a pass is saved as a review; the session logic itself lives in [StudySession].
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDeckDetail: ObserveDeckDetailUseCase,
    private val recordCardReview: RecordCardReviewUseCase,
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
                Log.e(TAG, "Failed to load the deck for study", e)
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
        val before = current.session
        val after = transform(before)
        _uiState.value = current.copy(session = after, message = null)

        // Cards revealed for the first time in this pass are saved as reviewed.
        (after.reviewedThisRun - before.reviewedThisRun).forEach { flashcardId -> saveReview(flashcardId) }
    }

    private fun saveReview(flashcardId: String) {
        viewModelScope.launch {
            try {
                recordCardReview(flashcardId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save a review", e)
                val state = _uiState.value
                if (state is StudyUiState.Active) {
                    _uiState.value = state.copy(message = "Couldn't save your progress for a card.")
                }
            }
        }
    }
}