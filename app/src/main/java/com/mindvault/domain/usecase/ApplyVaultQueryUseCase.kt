package com.mindvault.domain.usecase

import com.mindvault.domain.model.CategoryFilter
import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.model.VaultQuery
import com.mindvault.domain.model.VaultSort
import javax.inject.Inject

/**
 * Filters and sorts the saved items. Search is case-insensitive and every word must match
 * somewhere in the title, definition, notes, example or category name.
 */
class ApplyVaultQueryUseCase @Inject constructor() {

    operator fun invoke(entries: List<VaultEntry>, query: VaultQuery): List<VaultEntry> {
        val words = query.searchText.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

        return entries
            .filter { entry -> query.type == null || entry.item.type == query.type }
            .filter { entry -> matchesCategory(entry, query.category) }
            .filter { entry ->
                words.isEmpty() || searchableText(entry).let { text -> words.all { it in text } }
            }
            .sortedWith(comparatorFor(query.sort))
    }

    private fun matchesCategory(entry: VaultEntry, filter: CategoryFilter): Boolean = when (filter) {
        CategoryFilter.All -> true
        CategoryFilter.Uncategorized -> entry.item.categoryId == null
        is CategoryFilter.Specific -> entry.item.categoryId == filter.categoryId
    }

    private fun searchableText(entry: VaultEntry): String = with(entry.item) {
        listOf(title, definition, personalNotes, example.orEmpty(), entry.categoryName.orEmpty())
            .joinToString("\n")
            .lowercase()
    }

    private fun comparatorFor(sort: VaultSort): Comparator<VaultEntry> = when (sort) {
        VaultSort.RECENTLY_UPDATED ->
            compareByDescending<VaultEntry> { it.item.updatedAt }.thenBy { it.item.id }
        VaultSort.RECENTLY_ADDED ->
            compareByDescending<VaultEntry> { it.item.createdAt }.thenBy { it.item.id }
        VaultSort.A_TO_Z ->
            compareBy<VaultEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.item.title }.thenBy { it.item.id }
        VaultSort.Z_TO_A ->
            compareByDescending<VaultEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.item.title }.thenBy { it.item.id }
    }
}