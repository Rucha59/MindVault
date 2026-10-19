package com.mindvault.domain.repository

import com.mindvault.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun getById(id: String): Category?

    /** Returns the existing category if the name (case-insensitive) is already taken. */
    suspend fun create(name: String): Category

    /** @throws com.mindvault.domain.error.DuplicateNameException */
    suspend fun rename(id: String, name: String): Category?

    /** Items in the category are kept; their category becomes null. */
    suspend fun delete(id: String)
}