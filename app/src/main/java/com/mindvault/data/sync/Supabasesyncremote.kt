package com.mindvault.data.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * The only sync class that touches Supabase types. Calls sync_push / sync_pull as the signed-in user and refuses
 * to run when the signed-in user is not [userId] (the owner of the vault being synced).
 */
@Singleton
class SupabaseSyncRemote @Inject constructor(
    private val client: dagger.Lazy<SupabaseClient>,
) : SyncRemote {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun push(userId: String, changes: List<PushChange>): PushResponse = call(userId) {
        val payload = buildJsonObject {
            putJsonArray("changes") { changes.forEach { add(it.toJson()) } }
        }
        val result = client.get().postgrest.rpc("sync_push", buildJsonObject { put("p", payload) })
        val body = json.parseToJsonElement(result.data).jsonObject
        val rejected = body["rejected"]?.jsonArray.orEmpty().mapNotNull { element ->
            val entry = element.jsonObject
            val type = entry["type"]?.jsonPrimitive?.contentOrNull
            val id = entry["id"]?.jsonPrimitive?.contentOrNull
            if (type != null && id != null) outboxKey(type, id) else null
        }.toSet()
        PushResponse(rejected)
    }

    override suspend fun pull(
        userId: String,
        table: SyncTable,
        since: String,
        afterId: String,
        limit: Int,
    ): List<SyncRow> = call(userId) {
        val params = buildJsonObject {
            put("p_table", table.wire)
            put("p_since", since)
            put("p_after_id", afterId)
            put("p_limit", limit)
        }
        val result = client.get().postgrest.rpc("sync_pull", params)
        json.parseToJsonElement(result.data).jsonArray.map { row ->
            row.jsonObject.mapValues { (_, value) -> value.toAny() }
        }
    }

    private suspend fun <T> call(userId: String, block: suspend () -> T): T {
        // Data of one account must never reach another account's cloud rows.
        if (client.get().auth.currentUserOrNull()?.id != userId) throw SyncRemoteException(SyncFailureKind.AUTH)
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SyncRemoteException) {
            throw e
        } catch (e: Exception) {
            throw SyncRemoteException(classify(e), e)
        }
    }

    // If this file fails to compile, it is most likely the exception types or PostgrestResult.data
    // (supabase-kt 3.0.2 names; the same spots as AuthRepositoryImpl.toAuthError).
    private fun classify(e: Exception): SyncFailureKind = when (e) {
        is HttpRequestException, is IOException -> SyncFailureKind.NETWORK
        is RestException -> if (e.statusCode == 401 || e.statusCode == 403) SyncFailureKind.AUTH else SyncFailureKind.SERVER
        else -> SyncFailureKind.UNKNOWN
    }

    private fun PushChange.toJson(): JsonObject = buildJsonObject {
        put("type", type)
        put("op", operation)
        put("id", id)
        put("changed_at", changedAt)
        if (data != null) put("data", data.toJsonElement())
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

    private fun JsonElement.toAny(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
        is JsonObject -> mapValues { it.value.toAny() }
        is JsonArray -> map { it.toAny() }
    }
}