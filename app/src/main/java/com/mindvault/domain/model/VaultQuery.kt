package com.mindvault.domain.model

sealed interface CategoryFilter {
    data object All : CategoryFilter
    data object Uncategorized : CategoryFilter
    data class Specific(val categoryId: String) : CategoryFilter
}

enum class VaultSort { RECENTLY_UPDATED, RECENTLY_ADDED, A_TO_Z, Z_TO_A }

data class VaultQuery(
    val searchText: String = "",
    val type: KnowledgeType? = null,
    val category: CategoryFilter = CategoryFilter.All,
    val sort: VaultSort = VaultSort.RECENTLY_UPDATED,
) {
    /** Search, type and category count as filters; sort does not. */
    val hasActiveFilters: Boolean
        get() = searchText.isNotBlank() || type != null || category != CategoryFilter.All
}