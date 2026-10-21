package com.mindvault.domain.usecase

import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.repository.CategoryRepository
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Saved items (most recently updated first) with category names, re-emitting on any change. */
class ObserveVaultItemsUseCase @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<VaultEntry>> =
        combine(knowledgeRepository.observeAll(), categoryRepository.observeAll()) { items, categories ->
            val names = categories.associate { it.id to it.name }
            items.map { item -> VaultEntry(item, item.categoryId?.let { id -> names[id] }) }
        }
}