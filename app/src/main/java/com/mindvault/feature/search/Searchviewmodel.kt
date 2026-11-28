package com.mindvault.feature.search

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.QuickLookup
import com.mindvault.domain.model.SaveSearchResult
import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchScope
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.model.savedItemKey
import com.mindvault.domain.model.toSaveContent
import com.mindvault.domain.usecase.AddRecentSearchUseCase
import com.mindvault.domain.usecase.ClearRecentSearchesUseCase
import com.mindvault.domain.usecase.LoadConceptSummaryUseCase
import com.mindvault.domain.usecase.ObserveRecentSearchesUseCase
import com.mindvault.domain.usecase.ObserveSavedTitlesUseCase
import com.mindvault.domain.usecase.ObserveSearchSuggestionsEnabledUseCase
import com.mindvault.domain.usecase.QuickLookupInbox
import com.mindvault.domain.usecase.RankSearchResultsUseCase
import com.mindvault.domain.usecase.SaveSearchResultUseCase
import com.mindvault.domain.usecase.SearchDefinitionsUseCase
import com.mindvault.domain.usecase.SuggestSearchTermsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SearchVM"

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchDefinitions: SearchDefinitionsUseCase,
    private val loadConceptSummary: LoadConceptSummaryUseCase,
    private val saveSearchResult: SaveSearchResultUseCase,
    observeSavedTitles: ObserveSavedTitlesUseCase,
    private val rankSearchResults: RankSearchResultsUseCase,
    private val suggestSearchTerms: SuggestSearchTermsUseCase,
    observeSuggestionsEnabled: ObserveSearchSuggestionsEnabledUseCase,
    observeRecentSearches: ObserveRecentSearchesUseCase,
    private val addRecentSearch: AddRecentSearchUseCase,
    private val clearRecentSearches: ClearRecentSearchesUseCase,
    private val quickLookupInbox: QuickLookupInbox,
) : ViewModel() {

    /** Compose state (not a StateFlow) so typing doesn't lag, as in the Vault search box. */
    var query by mutableStateOf("")
        private set

    private val request = MutableStateFlow(RequestState())
    private var searchJob: Job? = null

    private val suggestions = MutableStateFlow<List<String>>(emptyList())
    private var suggestionJob: Job? = null

    /** The text a search was just run for. No suggestions are shown for it (they would only repeat it). */
    private var searchedText: String? = null

    /** The word that came from another app, while the shown search still belongs to it. */
    private val quickLookupText = MutableStateFlow<String?>(null)

    /** A lookup sent from another app and not yet run. The screen runs it with [onQuickLookup] once it is on screen. */
    val pendingQuickLookup: StateFlow<QuickLookup?> = quickLookupInbox.pending

    /** Eagerly, so the current value is known the moment typing starts. On unless the user turned it off. */
    private val suggestionsEnabled: StateFlow<Boolean> =
        observeSuggestionsEnabled()
            .catch { emit(true) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events: Flow<SearchEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SearchUiState> = combine(
        request,
        observeSavedTitles().catch {
            Log.e(TAG, "Failed to observe saved titles", it)
            emit(emptyMap())
        },
        observeRecentSearches().catch {
            Log.e(TAG, "Failed to observe recent searches", it)
            emit(emptyList())
        },
        suggestions,
        quickLookupText,
    ) { req, savedByKey, recent, typed, quick -> buildState(req, savedByKey, recent, typed, quick) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query = value
        if (value.trim() != quickLookupText.value) quickLookupText.value = null
        if (value.isBlank()) {
            searchJob?.cancel()
            searchedText = null
            request.value = RequestState(scope = request.value.scope)
        }
        updateSuggestions(value)
    }

    /** Runs the search for a tapped autocomplete suggestion or recent search. */
    fun onSuggestionSelected(text: String) {
        query = text
        onSearch()
    }

    /**
     * Runs a lookup sent from another app: filter reset to All, the text in the field, the search started.
     * The inbox hands each lookup to one caller only, so it runs once.
     */
    fun onQuickLookup(lookup: QuickLookup) {
        if (!quickLookupInbox.consume(lookup.id)) return
        searchJob?.cancel()
        request.value = RequestState(scope = SearchScope.ALL)
        query = lookup.text
        quickLookupText.value = lookup.text
        onSearch()
    }

    fun onClearRecentSearches() {
        viewModelScope.launch {
            try {
                clearRecentSearches()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear recent searches", e)
            }
        }
    }

    /** Changing the filter after a search runs the search again for the new sources. */
    fun onScopeChange(scope: SearchScope) {
        val current = request.value
        if (scope == current.scope) return
        request.update { it.copy(scope = scope) }
        if (current.submittedQuery.isNotEmpty() && query.isNotBlank()) onSearch() else updateSuggestions(query)
    }

    /** Runs on the keyboard's Search action and on "Try again". Not search-as-you-type, to spare the sources. */
    fun onSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        if (q != quickLookupText.value) quickLookupText.value = null
        val current = request.value
        if (current.loading && current.submittedQuery == q) return

        val scope = current.scope
        searchJob?.cancel()
        suggestionJob?.cancel()
        suggestions.value = emptyList()
        searchedText = q
        request.value = RequestState(scope = scope, submittedQuery = q, loading = true)
        searchJob = viewModelScope.launch {
            try {
                val outcomes = searchDefinitions(q, scope.sources)
                request.update { it.copy(loading = false, outcomes = outcomes) }
                // Only searches that found something are remembered, so typos don't fill the list.
                if (outcomes.values.any { it is SearchOutcome.Success }) rememberSearch(q)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                request.update { state ->
                    state.copy(loading = false, outcomes = scope.sources.associateWith { SearchOutcome.Failure })
                }
            }
        }
    }

    /** Retries one failed source and keeps the results the other source already returned. */
    fun onRetrySource(source: SearchSource) {
        val current = request.value
        if (current.submittedQuery.isEmpty() || source in current.retrying) return
        val submitted = current.submittedQuery
        request.update { it.copy(retrying = it.retrying + source) }
        viewModelScope.launch {
            val fresh: Map<SearchSource, SearchOutcome> = try {
                searchDefinitions(submitted, setOf(source))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Retry failed", e)
                mapOf(source to SearchOutcome.Failure)
            }
            request.update { state ->
                // Ignore the answer if a newer search has replaced the one this retry belongs to.
                if (state.submittedQuery != submitted) state
                else state.copy(outcomes = state.outcomes + fresh, retrying = state.retrying - source)
            }
        }
    }

    /** Opens or closes a card. Opening a concept that has no summary yet loads it. */
    fun onToggleExpanded(result: SearchResult) {
        val key = result.savedItemKey()
        val expanding = key !in request.value.expandedKeys
        request.update {
            it.copy(expandedKeys = if (expanding) it.expandedKeys + key else it.expandedKeys - key)
        }
        if (expanding && result.needsSummary) onLoadSummary(result)
    }

    /** Loads a concept's summary (also used by the card's "Try again"). */
    fun onLoadSummary(result: SearchResult) {
        val key = result.savedItemKey()
        if (key in request.value.loadingSummaries) return
        viewModelScope.launch {
            try {
                resolveSummary(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load summary", e)
                request.update { it.copy(summaryFailures = it.summaryFailures + key) }
            }
        }
    }

    fun onSave(result: SearchResult) {
        val key = result.savedItemKey()
        if (key in request.value.savingKeys) return
        request.update { it.copy(savingKeys = it.savingKeys + key, saveError = null) }

        viewModelScope.launch {
            try {
                val ready = resolveSummary(result)
                if (ready == null) {
                    request.update {
                        it.copy(saveError = "Couldn\u2019t load the full summary. Check your connection and try again.")
                    }
                    return@launch
                }
                when (saveSearchResult(ready)) {
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

    /** Opens the Add form with the result. A concept's summary is loaded first, so the form is never half empty. */
    fun onEditFirst(result: SearchResult) {
        if (result.savedItemKey() in request.value.loadingSummaries) return
        viewModelScope.launch {
            try {
                val ready = resolveSummary(result) ?: return@launch
                _events.send(SearchEvent.OpenEditForm(ready.toSaveContent()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open the editor", e)
                request.update { it.copy(saveError = "Couldn't open the editor. Please try again.") }
            }
        }
    }

    /** The result with its summary, loading it if needed. Null when the summary can't be loaded. */
    private suspend fun resolveSummary(result: SearchResult): SearchResult? {
        val key = result.savedItemKey()
        request.value.enriched[key]?.let { return it }
        if (!result.needsSummary) return result

        request.update {
            it.copy(loadingSummaries = it.loadingSummaries + key, summaryFailures = it.summaryFailures - key)
        }
        try {
            val loaded = (loadConceptSummary(result) as? SearchOutcome.Success)?.results?.firstOrNull()
            request.update {
                if (loaded != null) it.copy(enriched = it.enriched + (key to loaded))
                else it.copy(summaryFailures = it.summaryFailures + key)
            }
            return loaded
        } finally {
            request.update { it.copy(loadingSummaries = it.loadingSummaries - key) }
        }
    }

    private suspend fun rememberSearch(text: String) {
        try {
            addRecentSearch(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store the recent search", e)
        }
    }

    /**
     * Autocomplete: after [SearchLimits.MIN_SUGGESTION_LENGTH] characters and a pause of
     * [SearchLimits.SUGGESTION_DEBOUNCE_MILLIS] ms, from the sources the filter allows. Each keystroke cancels the
     * pending request, so fast typing sends one request, not one per letter.
     */
    private fun updateSuggestions(value: String) {
        suggestionJob?.cancel()
        val text = value.trim()
        if (!suggestionsEnabled.value || text.length < SearchLimits.MIN_SUGGESTION_LENGTH || text == searchedText) {
            suggestions.value = emptyList()
            return
        }
        val sources = request.value.scope.sources
        suggestionJob = viewModelScope.launch {
            delay(SearchLimits.SUGGESTION_DEBOUNCE_MILLIS)
            try {
                suggestions.value = suggestSearchTerms(text, sources)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Suggestions failed", e)
                suggestions.value = emptyList()
            }
        }
    }

    private fun buildState(
        req: RequestState,
        savedByKey: Map<String, String>,
        recentSearches: List<String>,
        suggestions: List<String>,
        quickLookup: String?,
    ): SearchUiState {
        val found = req.outcomes.values.filterIsInstance<SearchOutcome.Success>().flatMap { it.results }
        val ranked = rankSearchResults(req.submittedQuery, found)
        val failed = req.outcomes.filterValues { it.isFailure() }

        val status = when {
            req.submittedQuery.isEmpty() -> SearchStatus.Idle
            req.loading -> SearchStatus.Loading
            ranked.isNotEmpty() -> SearchStatus.Results
            failed.isEmpty() || failed.size < req.outcomes.size -> SearchStatus.NoResults
            failed.values.all { it is SearchOutcome.Offline } -> SearchStatus.Offline
            else -> SearchStatus.Error
        }
        val showNotices = status == SearchStatus.Results || status == SearchStatus.NoResults

        return SearchUiState(
            status = status,
            scope = req.scope,
            submittedQuery = req.submittedQuery,
            results = ranked.map { original ->
                val key = original.savedItemKey()
                SearchResultUi(
                    result = req.enriched[key] ?: original,
                    savedItemId = savedByKey[key],
                    isExpanded = key in req.expandedKeys,
                    isLoadingSummary = key in req.loadingSummaries,
                    summaryFailed = key in req.summaryFailures,
                )
            },
            notices = if (!showNotices) emptyList() else failed.entries
                .sortedBy { it.key.ordinal }
                .map { (source, outcome) ->
                    SourceNotice(source, outcome.toReason(), isRetrying = source in req.retrying)
                },
            suggestions = suggestions,
            recentSearches = recentSearches,
            savingKeys = req.savingKeys,
            saveError = req.saveError,
            quickLookupText = quickLookup,
        )
    }

    private fun SearchOutcome.isFailure(): Boolean =
        this is SearchOutcome.Offline || this is SearchOutcome.Busy || this is SearchOutcome.Failure

    private fun SearchOutcome.toReason(): SourceFailureReason = when (this) {
        SearchOutcome.Offline -> SourceFailureReason.OFFLINE
        SearchOutcome.Busy -> SourceFailureReason.BUSY
        else -> SourceFailureReason.ERROR
    }

    private data class RequestState(
        val scope: SearchScope = SearchScope.ALL,
        val submittedQuery: String = "",
        val loading: Boolean = false,
        val outcomes: Map<SearchSource, SearchOutcome> = emptyMap(),
        val retrying: Set<SearchSource> = emptySet(),
        /** Concept results whose summary was loaded after the search, by result key. */
        val enriched: Map<String, SearchResult> = emptyMap(),
        val expandedKeys: Set<String> = emptySet(),
        val loadingSummaries: Set<String> = emptySet(),
        val summaryFailures: Set<String> = emptySet(),
        val savingKeys: Set<String> = emptySet(),
        val saveError: String? = null,
    )
}