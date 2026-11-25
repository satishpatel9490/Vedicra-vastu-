package com.example.data.local

import com.example.model.ClientProblemItem
import com.example.model.EntranceDetail
import com.example.model.WorkspaceMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonCodec {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encodeEntrances(list: List<EntranceDetail>): String =
        runCatching { json.encodeToString(list) }.getOrDefault("[]")

    fun decodeEntrances(raw: String): List<EntranceDetail> =
        runCatching { json.decodeFromString<List<EntranceDetail>>(raw) }.getOrDefault(emptyList())

    fun encodeStringList(list: List<String>): String =
        runCatching { json.encodeToString(list) }.getOrDefault("[]")

    fun decodeStringList(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())

    fun encodeProblems(list: List<ClientProblemItem>): String =
        runCatching { json.encodeToString(list) }.getOrDefault("[]")

    fun decodeProblems(raw: String): List<ClientProblemItem> =
        runCatching { json.decodeFromString<List<ClientProblemItem>>(raw) }.getOrDefault(emptyList())

    fun encodeWorkspaceMetadata(meta: WorkspaceMetadata): String =
        runCatching { json.encodeToString(meta) }.getOrDefault("{}")

    fun decodeWorkspaceMetadata(raw: String): WorkspaceMetadata =
        runCatching { json.decodeFromString<WorkspaceMetadata>(raw) }.getOrDefault(WorkspaceMetadata())
}
