package com.mapzone.mapzonealertview.presentation.feature.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapzone.mapzonealertview.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "search_history")
private val HISTORY_KEY = stringPreferencesKey("history_json")
private const val MAX_HISTORY = 10

class SearchHistoryStore(private val context: Context) {
    private val gson = Gson()
    private val type = object : TypeToken<List<HistoryEntry>>() {}.type

    val history: Flow<List<HistoryEntry>> = context.dataStore.data.map { prefs ->
        prefs[HISTORY_KEY]?.let { json ->
            runCatching { gson.fromJson<List<HistoryEntry>>(json, type) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun add(entry: HistoryEntry) {
        context.dataStore.edit { prefs ->
            val current: List<HistoryEntry> = prefs[HISTORY_KEY]?.let {
                runCatching { gson.fromJson<List<HistoryEntry>>(it, type) }.getOrNull()
            } ?: emptyList()
            val deduped = current.filter { it.refId != entry.refId }
            val updated = (listOf(entry) + deduped).take(MAX_HISTORY)
            prefs[HISTORY_KEY] = gson.toJson(updated)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(HISTORY_KEY) }
    }
}
