package com.mindvault.feature.search

import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResultKind
import com.mindvault.domain.model.SearchScope
import com.mindvault.domain.model.SearchSource
import com.mindvault.domain.usecase.AddRecentSearchUseCase
import com.mindvault.domain.usecase.ClearRecentSearchesUseCase
import com.mindvault.domain.usecase.FakeKnowledgeRepository
import com.mindvault.domain.usecase.FakeSearchRepository
import com.mindvault.domain.usecase.FakeSettingsRepository
import com.mindvault.domain.usecase.LoadConceptSummaryUseCase
import com.mindvault.domain.usecase.ObserveRecentSearchesUseCase
import com.mindvault.domain.usecase.ObserveSavedTitlesUseCase
import com.mindvault.domain.usecase.ObserveSearchSuggestionsEnabledUseCase
import com.mindvault.domain.usecase.RankSearchResultsUseCase
import com.mindvault.domain.usecase.SearchDefinitionsUseCase
import com.mindvault.domain.usecase.SuggestSearchTermsUseCase
import com.mindvault.domain.usecase.newSaveSearchResultUseCase
import com.mindvault.domain.usecase.sampleConceptResult
import com.mindvault.domain.usecase.sampleSearchResult
import com.mindvault.domain.usecase.savedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val searchRepository = FakeSearchRepository()
    private val knowledgeRepository = FakeKnowledgeRepository()
    private val settings = FakeSettingsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = SearchViewModel(
        searchDefinitions = SearchDefinitionsUseCase(searchRepository),
        loadConceptSummary = LoadConceptSummaryUseCase(searchRepository),
        saveSearchResult = newSaveSearchResultUseCase(knowledgeRepository),
        observeSavedTitles = ObserveSavedTitlesUseCase(knowledgeRepository),
        rankSearchResults = RankSearchResultsUseCase(),
        suggestSearchTerms = SuggestSearchTermsUseCase(searchRepository),
        observeSuggestionsEnabled = ObserveSearchSuggestionsEnabledUseCase(settings),
        observeRecentSearches = ObserveRecentSearchesUseCase(settings),
        addRecentSearch = AddRecentSearchUseCase(settings),
        clearRecentSearches = ClearRecentSearchesUseCase(settings),
    )

    /** uiState only runs while collected, so keep one collector alive for the whole test. */
    private fun TestScope.collecting(viewModel: SearchViewModel): SearchViewModel {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TestScope.collectEvents(viewModel: SearchViewModel): List<SearchEvent> {
        val events = mutableListOf<SearchEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it } }
        return events
    }

    private fun SearchViewModel.search(text: String) {
        onQueryChange(text)
        onSearch()
    }

    private fun words(vararg titles: String) =
        SearchOutcome.Success(titles.map { sampleSearchResult(it) })

    private fun concepts(vararg results: com.mindvault.domain.model.SearchResult) =
        SearchOutcome.Success(results.toList())

    @Test
    fun startsIdle_withTheAllFilter() = runTest(dispatcher) {
        val vm = collecting(newViewModel())
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
        assertEquals(SearchScope.ALL, vm.uiState.value.scope)
    }

    @Test
    fun allFilter_showsWordsAndConceptsInOneRankedList() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.WORDS] = words("entropy")
        searchRepository.outcomes[SearchSource.CONCEPTS] =
            concepts(sampleConceptResult("Cross-entropy"), sampleConceptResult("Entropy"))
        val vm = collecting(newViewModel())

        vm.search("entropy")

        val state = vm.uiState.value
        assertEquals(SearchStatus.Results, state.status)
        assertEquals("entropy", state.submittedQuery)
        assertEquals(
            listOf("entropy" to SearchResultKind.WORD, "Entropy" to SearchResultKind.CONCEPT, "Cross-entropy" to SearchResultKind.CONCEPT),
            state.results.map { it.result.title to it.result.kind },
        )
        assertTrue(state.notices.isEmpty())
        assertEquals(setOf(SearchSource.WORDS, SearchSource.CONCEPTS), searchRepository.sources.toSet())
    }

    @Test
    fun wordsFilter_queriesOnlyWiktionary() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.WORDS] = words("entropy")
        val vm = collecting(newViewModel())

        vm.onScopeChange(SearchScope.WORDS)
        vm.search("entropy")

        assertEquals(listOf(SearchSource.WORDS), searchRepository.sources)
        assertEquals(SearchScope.WORDS, vm.uiState.value.scope)
    }

    @Test
    fun changingTheFilterAfterASearch_searchesAgainForTheNewSources() = runTest(dispatcher) {
        searchRepository.outcome = words("entropy")
        val vm = collecting(newViewModel())
        vm.search("entropy")
        assertEquals(2, searchRepository.calls.size)

        vm.onScopeChange(SearchScope.CONCEPTS)

        assertEquals(3, searchRepository.calls.size)
        assertEquals(SearchSource.CONCEPTS, searchRepository.sources.last())
    }

    @Test
    fun changingTheFilterBeforeAnySearch_doesNotSearch() = runTest(dispatcher) {
        val vm = collecting(newViewModel())

        vm.onScopeChange(SearchScope.CONCEPTS)

        assertTrue(searchRepository.calls.isEmpty())
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
    }

    @Test
    fun whenEverySourceFails_theStatusIsOfflineOrError() = runTest(dispatcher) {
        val vm = collecting(newViewModel())
        val expected = listOf(
            SearchOutcome.NoResults to SearchStatus.NoResults,
            SearchOutcome.Offline to SearchStatus.Offline,
            SearchOutcome.Busy to SearchStatus.Error,
            SearchOutcome.Failure to SearchStatus.Error,
        )
        expected.forEach { (outcome, status) ->
            searchRepository.outcome = outcome
            vm.search("word")
            assertEquals(status, vm.uiState.value.status)
            assertTrue(vm.uiState.value.notices.isEmpty()) // the full-screen message already says it
            vm.onQueryChange("") // reset between cases
        }
    }

    @Test
    fun oneSourceFailing_stillShowsTheOthersResults_withANotice() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.WORDS] = words("entropy")
        searchRepository.outcomes[SearchSource.CONCEPTS] = SearchOutcome.Offline
        val vm = collecting(newViewModel())

        vm.search("entropy")

        val state = vm.uiState.value
        assertEquals(SearchStatus.Results, state.status)
        assertEquals(1, state.results.size)
        assertEquals(listOf(SourceNotice(SearchSource.CONCEPTS, SourceFailureReason.OFFLINE, false)), state.notices)
    }

    @Test
    fun oneSourceFailing_andTheOtherFindingNothing_isNoResultsWithTheNotice() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.WORDS] = SearchOutcome.NoResults
        searchRepository.outcomes[SearchSource.CONCEPTS] = SearchOutcome.Busy
        val vm = collecting(newViewModel())

        vm.search("entropy")

        val state = vm.uiState.value
        assertEquals(SearchStatus.NoResults, state.status)
        assertEquals(listOf(SourceNotice(SearchSource.CONCEPTS, SourceFailureReason.BUSY, false)), state.notices)
    }

    @Test
    fun retryingAFailedSource_fillsItIn_andKeepsTheOtherResults() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.WORDS] = words("entropy")
        searchRepository.outcomes[SearchSource.CONCEPTS] = SearchOutcome.Offline
        val vm = collecting(newViewModel())
        vm.search("entropy")

        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy"))
        vm.onRetrySource(SearchSource.CONCEPTS)

        val state = vm.uiState.value
        assertTrue(state.notices.isEmpty())
        assertEquals(listOf("entropy", "Entropy"), state.results.map { it.result.title })
        assertEquals(3, searchRepository.calls.size) // the retry asked only the failed source
        assertEquals(SearchSource.CONCEPTS, searchRepository.sources.last())
    }

    @Test
    fun retryThatFailsAgain_keepsTheNotice() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.WORDS] = words("entropy")
        searchRepository.outcomes[SearchSource.CONCEPTS] = SearchOutcome.Offline
        val vm = collecting(newViewModel())
        vm.search("entropy")

        searchRepository.outcomes[SearchSource.CONCEPTS] = SearchOutcome.Busy
        vm.onRetrySource(SearchSource.CONCEPTS)

        assertEquals(
            listOf(SourceNotice(SearchSource.CONCEPTS, SourceFailureReason.BUSY, false)),
            vm.uiState.value.notices,
        )
    }

    @Test
    fun blankQuery_doesNotCallAnySource() = runTest(dispatcher) {
        val vm = collecting(newViewModel())
        vm.search("   ")
        assertTrue(searchRepository.calls.isEmpty())
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
    }

    @Test
    fun clearingTheField_resetsToIdle_butKeepsTheFilter() = runTest(dispatcher) {
        searchRepository.outcome = words("hello")
        val vm = collecting(newViewModel())
        vm.onScopeChange(SearchScope.WORDS)
        vm.search("hello")

        vm.onQueryChange("")

        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
        assertTrue(vm.uiState.value.results.isEmpty())
        assertEquals(SearchScope.WORDS, vm.uiState.value.scope)
    }

    @Test
    fun resultAlreadyInTheVault_isFlaggedWithItsItemId() = runTest(dispatcher) {
        knowledgeRepository.items.value = listOf(savedItem("existing", "Hello"))
        searchRepository.outcomes[SearchSource.WORDS] = words("hello")
        val vm = collecting(newViewModel())

        vm.search("hello")

        assertEquals("existing", vm.uiState.value.results.single().savedItemId)
    }

    @Test
    fun aSavedWord_doesNotFlagTheConceptWithTheSameTitle() = runTest(dispatcher) {
        knowledgeRepository.items.value = listOf(savedItem("w", "Entropy", KnowledgeType.WORD))
        searchRepository.outcomes[SearchSource.WORDS] = words("Entropy")
        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy"))
        val vm = collecting(newViewModel())

        vm.search("entropy")

        val results = vm.uiState.value.results
        assertEquals(listOf(SearchResultKind.WORD, SearchResultKind.CONCEPT), results.map { it.result.kind })
        assertEquals("w", results[0].savedItemId)
        assertNull(results[1].savedItemId)
    }

    @Test
    fun savingAWord_createsTheItem_flipsTheCard_andClearsTheSavingFlag() = runTest(dispatcher) {
        val result = sampleSearchResult()
        searchRepository.outcomes[SearchSource.WORDS] = SearchOutcome.Success(listOf(result))
        val vm = collecting(newViewModel())
        vm.search("hello")

        vm.onSave(result)

        assertEquals(1, knowledgeRepository.created.size)
        assertEquals(KnowledgeType.WORD, knowledgeRepository.created.single().type)
        val state = vm.uiState.value
        assertNotNull(state.results.single().savedItemId)
        assertTrue(state.savingKeys.isEmpty())
        assertNull(state.saveError)
    }

    @Test
    fun savingAConceptWithoutItsSummary_loadsTheSummaryFirst_thenSavesAConcept() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy", summary = null))
        val vm = collecting(newViewModel())
        vm.onScopeChange(SearchScope.CONCEPTS)
        vm.search("entropy")

        vm.onSave(vm.uiState.value.results.single().result)

        assertEquals(1, searchRepository.summaryCalls.size)
        val draft = knowledgeRepository.created.single()
        assertEquals(KnowledgeType.CONCEPT, draft.type)
        assertEquals("Loaded summary of Entropy.", draft.definition) // no source footnote in the saved text
        assertNotNull(vm.uiState.value.results.single().savedItemId)
    }

    @Test
    fun savingAConcept_whenTheSummaryCannotBeLoaded_savesNothingAndSaysSo() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy", summary = null))
        searchRepository.summaryOutcome = SearchOutcome.Offline
        val vm = collecting(newViewModel())
        vm.onScopeChange(SearchScope.CONCEPTS)
        vm.search("entropy")

        vm.onSave(vm.uiState.value.results.single().result)

        assertTrue(knowledgeRepository.created.isEmpty())
        val state = vm.uiState.value
        assertNotNull(state.saveError)
        assertTrue(state.savingKeys.isEmpty())
        assertTrue(state.results.single().summaryFailed)
    }

    @Test
    fun openingAConceptCard_loadsItsSummary_andClosingKeepsIt() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy", summary = null))
        val vm = collecting(newViewModel())
        vm.onScopeChange(SearchScope.CONCEPTS)
        vm.search("entropy")

        vm.onToggleExpanded(vm.uiState.value.results.single().result)

        var card = vm.uiState.value.results.single()
        assertTrue(card.isExpanded)
        assertFalse(card.isLoadingSummary)
        assertEquals("Loaded summary of Entropy.", card.result.summary)

        vm.onToggleExpanded(card.result)
        card = vm.uiState.value.results.single()
        assertFalse(card.isExpanded)
        assertEquals(1, searchRepository.summaryCalls.size)
    }

    @Test
    fun aSummaryThatFailsToLoad_isFlagged_andCanBeRetried() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy", summary = null))
        searchRepository.summaryOutcome = SearchOutcome.Offline
        val vm = collecting(newViewModel())
        vm.onScopeChange(SearchScope.CONCEPTS)
        vm.search("entropy")
        vm.onToggleExpanded(vm.uiState.value.results.single().result)
        assertTrue(vm.uiState.value.results.single().summaryFailed)

        searchRepository.summaryOutcome = null
        vm.onLoadSummary(vm.uiState.value.results.single().result)

        val card = vm.uiState.value.results.single()
        assertFalse(card.summaryFailed)
        assertEquals("Loaded summary of Entropy.", card.result.summary)
    }

    @Test
    fun editFirst_onAConcept_opensTheFormWithTheSummaryAndTheConceptType() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy", summary = null))
        val vm = collecting(newViewModel())
        val events = collectEvents(vm)
        vm.onScopeChange(SearchScope.CONCEPTS)
        vm.search("entropy")

        vm.onEditFirst(vm.uiState.value.results.single().result)

        val content = (events.single() as SearchEvent.OpenEditForm).content
        assertEquals("Entropy", content.title)
        assertEquals(KnowledgeType.CONCEPT, content.type)
        assertEquals("Loaded summary of Entropy.", content.definition)
        assertTrue(knowledgeRepository.created.isEmpty())
    }

    @Test
    fun editFirst_onAWord_opensTheFormAtOnce() = runTest(dispatcher) {
        val result = sampleSearchResult()
        searchRepository.outcomes[SearchSource.WORDS] = SearchOutcome.Success(listOf(result))
        val vm = collecting(newViewModel())
        val events = collectEvents(vm)
        vm.onScopeChange(SearchScope.WORDS)
        vm.search("hello")

        vm.onEditFirst(result)

        val content = (events.single() as SearchEvent.OpenEditForm).content
        assertEquals(KnowledgeType.WORD, content.type)
        assertEquals("(noun) used as a greeting", content.definition)
        assertTrue(searchRepository.summaryCalls.isEmpty())
    }

    @Test
    fun editFirst_whenTheSummaryCannotBeLoaded_opensNothing() = runTest(dispatcher) {
        searchRepository.outcomes[SearchSource.CONCEPTS] = concepts(sampleConceptResult("Entropy", summary = null))
        searchRepository.summaryOutcome = SearchOutcome.Failure
        val vm = collecting(newViewModel())
        val events = collectEvents(vm)
        vm.onScopeChange(SearchScope.CONCEPTS)
        vm.search("entropy")

        vm.onEditFirst(vm.uiState.value.results.single().result)

        assertTrue(events.isEmpty())
        assertTrue(vm.uiState.value.results.single().summaryFailed)
    }

    // ------------------------------------------------------------------ autocomplete

    /** Lets the debounce pass (virtual time in the real test library). */
    private fun TestScope.pastDebounce() = advanceTimeBy(SearchLimits.SUGGESTION_DEBOUNCE_MILLIS + 1)

    @Test
    fun fewerThanTwoCharacters_asksForNoSuggestions() = runTest(dispatcher) {
        val vm = collecting(newViewModel())

        vm.onQueryChange("e")
        pastDebounce()

        assertTrue(searchRepository.suggestCalls.isEmpty())
        assertTrue(vm.uiState.value.suggestions.isEmpty())
    }

    @Test
    fun nothingIsRequestedBeforeTheTypingPause() = runTest(dispatcher) {
        val vm = collecting(newViewModel())

        vm.onQueryChange("en")
        advanceTimeBy(100)

        assertTrue(searchRepository.suggestCalls.isEmpty())
    }

    @Test
    fun afterThePause_suggestionsComeFromBothSourcesInTheAllFilter() = runTest(dispatcher) {
        searchRepository.suggestions[SearchSource.WORDS] = listOf("entropy", "entry")
        searchRepository.suggestions[SearchSource.CONCEPTS] = listOf("Entropy", "Enthalpy")
        val vm = collecting(newViewModel())

        vm.onQueryChange("en")
        pastDebounce()

        assertEquals(listOf("entropy", "entry", "Enthalpy"), vm.uiState.value.suggestions)
        assertEquals(setOf(SearchSource.WORDS, SearchSource.CONCEPTS), searchRepository.suggestCalls.map { it.second }.toSet())
    }

    @Test
    fun fastTyping_sendsOnlyOneSuggestionRequestPerSource() = runTest(dispatcher) {
        val vm = collecting(newViewModel())
        vm.onScopeChange(SearchScope.WORDS)

        vm.onQueryChange("en")
        advanceTimeBy(100)
        vm.onQueryChange("ent")
        advanceTimeBy(100)
        vm.onQueryChange("entr")
        pastDebounce()

        assertEquals(listOf("entr" to SearchSource.WORDS), searchRepository.suggestCalls)
    }

    @Test
    fun wordsFilter_suggestsFromWiktionaryOnly_conceptsFromWikipediaOnly() = runTest(dispatcher) {
        val vm = collecting(newViewModel())

        vm.onScopeChange(SearchScope.WORDS)
        vm.onQueryChange("en")
        pastDebounce()
        assertEquals(listOf(SearchSource.WORDS), searchRepository.suggestCalls.map { it.second })

        searchRepository.suggestCalls.clear()
        vm.onScopeChange(SearchScope.CONCEPTS) // typed text is still there: suggestions are refreshed
        pastDebounce()
        assertEquals(listOf(SearchSource.CONCEPTS), searchRepository.suggestCalls.map { it.second })
    }

    @Test
    fun suggestionsSwitchedOff_noRequestIsSent() = runTest(dispatcher) {
        settings.searchSuggestionsEnabled.value = false
        searchRepository.suggestions[SearchSource.WORDS] = listOf("entropy")
        val vm = collecting(newViewModel())

        vm.onQueryChange("en")
        pastDebounce()

        assertTrue(searchRepository.suggestCalls.isEmpty())
        assertTrue(vm.uiState.value.suggestions.isEmpty())
    }

    @Test
    fun selectingASuggestion_runsTheSearch_andHidesTheSuggestions() = runTest(dispatcher) {
        searchRepository.suggestions[SearchSource.WORDS] = listOf("entropy")
        searchRepository.outcomes[SearchSource.WORDS] = words("entropy")
        val vm = collecting(newViewModel())
        vm.onQueryChange("en")
        pastDebounce()
        assertEquals(listOf("entropy"), vm.uiState.value.suggestions)
        searchRepository.suggestCalls.clear()

        vm.onSuggestionSelected("entropy")
        pastDebounce()

        assertEquals("entropy", vm.query)
        assertEquals("entropy", vm.uiState.value.submittedQuery)
        assertEquals(SearchStatus.Results, vm.uiState.value.status)
        assertTrue(vm.uiState.value.suggestions.isEmpty())
        assertTrue(searchRepository.suggestCalls.isEmpty()) // no suggestions for the text just searched
    }

    @Test
    fun afterASearch_newTypingBringsSuggestionsBack() = runTest(dispatcher) {
        searchRepository.outcome = words("entropy")
        val vm = collecting(newViewModel())
        vm.search("entropy")

        vm.onQueryChange("entropy ") // same text once trimmed
        pastDebounce()
        assertTrue(searchRepository.suggestCalls.isEmpty())

        vm.onQueryChange("entropy x")
        pastDebounce()
        assertEquals(2, searchRepository.suggestCalls.size) // both sources of the All filter
    }

    @Test
    fun clearingTheField_removesTheSuggestions() = runTest(dispatcher) {
        searchRepository.suggestions[SearchSource.WORDS] = listOf("entropy")
        val vm = collecting(newViewModel())
        vm.onQueryChange("en")
        pastDebounce()
        assertEquals(listOf("entropy"), vm.uiState.value.suggestions)

        vm.onQueryChange("")

        assertTrue(vm.uiState.value.suggestions.isEmpty())
    }

    // ------------------------------------------------------------------ recent searches

    @Test
    fun aSearchThatFindsSomething_isRemembered_newestFirst_atMostFour() = runTest(dispatcher) {
        searchRepository.outcome = words("x")
        val vm = collecting(newViewModel())

        listOf("one", "two", "three", "four", "five").forEach { vm.search(it) }

        assertEquals(listOf("five", "four", "three", "two"), vm.uiState.value.recentSearches)
    }

    @Test
    fun aSearchThatFindsNothing_orFails_isNotRemembered() = runTest(dispatcher) {
        val vm = collecting(newViewModel())

        searchRepository.outcome = SearchOutcome.NoResults
        vm.search("typo")
        searchRepository.outcome = SearchOutcome.Offline
        vm.search("offline")

        assertTrue(vm.uiState.value.recentSearches.isEmpty())
    }

    @Test
    fun repeatingASearch_movesItToTheTop_withoutDuplicates() = runTest(dispatcher) {
        searchRepository.outcome = words("x")
        val vm = collecting(newViewModel())

        vm.search("run")
        vm.search("entropy")
        vm.search("Run")

        assertEquals(listOf("Run", "entropy"), vm.uiState.value.recentSearches)
    }

    @Test
    fun selectingARecentSearch_runsItAgain() = runTest(dispatcher) {
        settings.recentSearches.value = listOf("entropy")
        searchRepository.outcome = words("entropy")
        val vm = collecting(newViewModel())

        vm.onSuggestionSelected(vm.uiState.value.recentSearches.single())

        assertEquals("entropy", vm.uiState.value.submittedQuery)
        assertEquals(SearchStatus.Results, vm.uiState.value.status)
    }

    @Test
    fun clearingRecentSearches_emptiesTheList() = runTest(dispatcher) {
        settings.recentSearches.value = listOf("entropy", "run")
        val vm = collecting(newViewModel())
        assertEquals(2, vm.uiState.value.recentSearches.size)

        vm.onClearRecentSearches()

        assertTrue(vm.uiState.value.recentSearches.isEmpty())
    }
}