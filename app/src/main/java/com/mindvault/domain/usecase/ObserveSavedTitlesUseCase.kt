package com.mindvault.domain.usecase

import com.mindvault.domain.model.toTitleKey
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Emits saved titles (as case-insensitive keys) mapped to item IDs, so Search can flag saved results live. */
class ObserveSavedTitlesUseCase @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
) {
    operator fun invoke(): Flow<Map<String, String>> =
        knowledgeRepository.observeAll().map { items ->
            buildMap { items.forEach { putIfAbsent(it.title.toTitleKey(), it.id) } }
        }
}