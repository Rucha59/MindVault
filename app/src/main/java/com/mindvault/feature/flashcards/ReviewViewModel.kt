package com.mindvault.feature.flashcards

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.core.navigation.DECK_ID_ARG
import com.mindvault.core.navigation.REVIEW_MODE_ARG
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewMode
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewSession
import com.mindvault.domain.scheduler.ReviewScheduler
import com.mindvault.domain.usecase.LoadReviewQueueUseCase
import com.mindvault.domain.usecase.RecordCardReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ReviewVM"

/**
 * Takes one snapshot of the cards when the screen opens and runs the session on it. Every scheduled answer is
 * saved before the session moves on: a card's first rating through [RecordCardReviewUseCase], and the answer when
 * a card marked Again comes back through its relearn path. Extra-practice cards save nothing. Session rules live
 * in [ReviewSession].
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadReviewQueue: LoadReviewQueueUseCase,
    private val recordCardReview: RecordCardReviewUseCase,
    private val scheduler: ReviewScheduler,
) : ViewModel() {

    private val deckId: String = checkNotNull(savedStateHandle.get<String>(DECK_ID_ARG))
    private val mode: ReviewMode =
        ReviewMode.entries.firstOrNull { it.name == savedStateHandle.get<String>(REVIEW_MODE_ARG) }
            ?: ReviewMode.DAILY

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val queue = loadReviewQueue(deckId, mode)
                _uiState.value = when {
                    queue == null -> ReviewUiState.Error
                    queue.cards.isEmpty() -> ReviewUiState.Empty(queue.deckName, mode)
                    else -> ReviewUiState.Active(
                        queue.deckName,
                        mode,
                        ReviewSession.start(queue.cards, queue.extraCardIds),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load the review queue", e)
                _uiState.value = ReviewUiState.Error
            }
        }
    }

    /** Days until [card] returns if rated [rating]; for the button labels. */
    fun intervalDays(card: FlashcardDetail, rating: ReviewRating): Int =
        scheduler.intervalDays(card.reviewState, rating)

    /** Days until a card returns when it comes back after Again and is answered with [rating]. */
    fun relearnIntervalDays(rating: ReviewRating): Int = scheduler.relearnIntervalDays(rating)

    fun onFlip() = updateActive { it.copy(session = it.session.flip(), message = null) }

    fun onRate(rating: ReviewRating) {
        val active = _uiState.value as? ReviewUiState.Active ?: return
        val session = active.session
        val card = session.current ?: return
        if (!session.isRevealed || active.isSaving) return
        if (session.isExtra) return // extra practice is never rated

        if (session.isRepeat) {
            // A card comes back only once after Again, so Again is not an answer here.
            if (rating == ReviewRating.AGAIN) return
            saveThenAnswer(rating) { recordCardReview.relearn(card.flashcard.id, rating) }
        } else {
            saveThenAnswer(rating) { recordCardReview(card.flashcard.id, rating) }
        }
    }

    /** Extra practice: nothing is saved and the schedule never changes. */
    fun onExtraAnswer(gotIt: Boolean) =
        updateActive { it.copy(session = it.session.answerExtra(gotIt), message = null) }

    /** Saves first; the session moves on only if the save worked, so a failed save can be retried. */
    private fun saveThenAnswer(rating: ReviewRating, save: suspend () -> Unit) {
        updateActive { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                save()
                updateActive { it.copy(session = it.session.answer(rating), isSaving = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save a review", e)
                updateActive { it.copy(isSaving = false, message = "Couldn't save your answer. Please try again.") }
            }
        }
    }

    private fun updateActive(transform: (ReviewUiState.Active) -> ReviewUiState.Active) {
        _uiState.update { state -> if (state is ReviewUiState.Active) transform(state) else state }
    }
}