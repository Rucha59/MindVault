package com.mindvault.feature.search

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.SaveSearchResult
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.toTitleKey
import com.mindvault.domain.usecase.ObserveSavedTitlesUseCase
import com.mindvault.domain.usecase.SaveSearchResultUseCase
import com.mindvault.domain.usecase.SearchDefinitionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SearchVM"

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchDefinitions: SearchDefinitionsUseCase,
    private val saveSearchResult: SaveSearchResultUseCase,
    observeSavedTitles: ObserveSavedTitlesUseCase,
) : ViewModel() {

    /** Compose state (not a StateFlow) so typing doesn't lag, as in the Vault search box. */
    var query by mutableStateOf("")
        private set

    private val request = MutableStateFlow(RequestState())
    private var searchJob: Job? = null

    val uiState: StateFlow<SearchUiState> = combine(
        request,
        observeSavedTitles().catch {
            Log.e(TAG, "Failed to observe saved titles", it)
            emit(emptyMap())
        },
    ) { req, savedByTitle ->
        SearchUiState(
            status = req.status,
            submittedQuery = req.submittedQuery,
            results = req.results.map { SearchResultUi(it, savedByTitle[it.title.toTitleKey()]) },
            savingKeys = req.savingKeys,
            saveError = req.saveError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query = value
        if (value.isBlank()) {
            searchJob?.cancel()
            request.value = RequestState()
        }
    }

    /** Runs on the keyboard's Search action and on "Try again". Not search-as-you-type, to spare the provider. */
    fun onSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        val current = request.value
        if (current.status == SearchStatus.Loading && current.submittedQuery == q) return

        searchJob?.cancel()
        request.value = RequestState(status = SearchStatus.Loading, submittedQuery = q)
        searchJob = viewModelScope.launch {
            try {
                val outcome = searchDefinitions(q)
                request.update { state ->
                    when (outcome) {
                        is SearchOutcome.Success -> state.copy(status = SearchStatus.Results, results = outcome.results)
                        SearchOutcome.NoResults -> state.copy(status = SearchStatus.NoResults)
                        SearchOutcome.Offline -> state.copy(status = SearchStatus.Offline)
                        SearchOutcome.Failure -> state.copy(status = SearchStatus.Error)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                request.update { it.copy(status = SearchStatus.Error) }
            }
        }
    }

    fun onSave(result: SearchResult) {
        val key = result.title.toTitleKey()
        if (key in request.value.savingKeys) return
        request.update { it.copy(savingKeys = it.savingKeys + key, saveError = null) }

        viewModelScope.launch {
            try {
                when (saveSearchResult(result)) {
                    // On success the saved-titles flow flips the card to "Already in your Vault".
                    is SaveSearchResult.Saved, is SaveSearchResult.AlreadySaved -> Unit
                    SaveSearchResult.Invalid -> request.update {
                        it.copy(saveError = "This result can't be saved as is. Use \u201CEdit first\u201D to adjust it.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save search result", e)
                request.update { it.copy(saveError = "Couldn't save. Please try again.") }
            } finally {
                request.update { it.copy(savingKeys = it.savingKeys - key) }
            }
        }
    }

    private data class RequestState(
        val status: SearchStatus = SearchStatus.Idle,
        val submittedQuery: String = "",
        val results: List<SearchResult> = emptyList(),
        val savingKeys: Set<String> = emptySet(),
        val saveError: String? = null,
    )
}