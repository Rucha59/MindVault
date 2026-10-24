package com.mindvault.feature.search

import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.usecase.FakeKnowledgeRepository
import com.mindvault.domain.usecase.FakeSearchRepository
import com.mindvault.domain.usecase.ObserveSavedTitlesUseCase
import com.mindvault.domain.usecase.SearchDefinitionsUseCase
import com.mindvault.domain.usecase.newSaveSearchResultUseCase
import com.mindvault.domain.usecase.sampleSearchResult
import com.mindvault.domain.usecase.savedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = SearchViewModel(
        searchDefinitions = SearchDefinitionsUseCase(searchRepository),
        saveSearchResult = newSaveSearchResultUseCase(knowledgeRepository),
        observeSavedTitles = ObserveSavedTitlesUseCase(knowledgeRepository),
    )

    /** uiState only runs while collected, so keep one collector alive for the whole test. */
    private fun TestScope.collecting(viewModel: SearchViewModel): SearchViewModel {
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun SearchViewModel.search(text: String) {
        onQueryChange(text)
        onSearch()
    }

    @Test
    fun startsIdle() = runTest(dispatcher) {
        val vm = collecting(newViewModel())
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
    }

    @Test
    fun successShowsResultsNotYetSaved() = runTest(dispatcher) {
        searchRepository.outcome = SearchOutcome.Success(listOf(sampleSearchResult()))
        val vm = collecting(newViewModel())

        vm.search("hello")

        val state = vm.uiState.value
        assertEquals(SearchStatus.Results, state.status)
        assertEquals("hello", state.submittedQuery)
        assertNull(state.results.single().savedItemId)
    }

    @Test
    fun noResults_offline_andFailure_mapToTheirStates() = runTest(dispatcher) {
        val vm = collecting(newViewModel())
        val expected = listOf(
            SearchOutcome.NoResults to SearchStatus.NoResults,
            SearchOutcome.Offline to SearchStatus.Offline,
            SearchOutcome.Failure to SearchStatus.Error,
        )
        expected.forEach { (outcome, status) ->
            searchRepository.outcome = outcome
            vm.search("word")
            assertEquals(status, vm.uiState.value.status)
            vm.onQueryChange("") // reset between cases
        }
    }

    @Test
    fun blankQuery_doesNotCallTheProvider() = runTest(dispatcher) {
        val vm = collecting(newViewModel())
        vm.search("   ")
        assertTrue(searchRepository.queries.isEmpty())
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
    }

    @Test
    fun clearingTheField_resetsToIdle() = runTest(dispatcher) {
        searchRepository.outcome = SearchOutcome.Success(listOf(sampleSearchResult()))
        val vm = collecting(newViewModel())
        vm.search("hello")

        vm.onQueryChange("")

        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
        assertTrue(vm.uiState.value.results.isEmpty())
    }

    @Test
    fun resultAlreadyInTheVault_isFlaggedWithItsItemId() = runTest(dispatcher) {
        knowledgeRepository.items.value = listOf(savedItem("existing", "Hello"))
        searchRepository.outcome = SearchOutcome.Success(listOf(sampleSearchResult("hello")))
        val vm = collecting(newViewModel())

        vm.search("hello")

        assertEquals("existing", vm.uiState.value.results.single().savedItemId)
    }

    @Test
    fun saving_createsTheItem_flipsTheCard_andClearsTheSavingFlag() = runTest(dispatcher) {
        val result = sampleSearchResult()
        searchRepository.outcome = SearchOutcome.Success(listOf(result))
        val vm = collecting(newViewModel())
        vm.search("hello")

        vm.onSave(result)

        assertEquals(1, knowledgeRepository.created.size)
        val state = vm.uiState.value
        assertNotNull(state.results.single().savedItemId)
        assertTrue(state.savingKeys.isEmpty())
        assertNull(state.saveError)
    }
}