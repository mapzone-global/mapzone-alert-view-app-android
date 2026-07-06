package com.mapzone.mapzonealertview.presentation.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapzone.mapzonealertview.config.AppConfig
import com.mapzone.mapzonealertview.domain.model.AutocompleteItem
import com.mapzone.mapzonealertview.domain.model.HistoryEntry
import com.mapzone.mapzonealertview.domain.repository.SearchApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val suggestions: List<AutocompleteItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val api = SearchApi.create()
    private val historyStore = SearchHistoryStore(app)

    private val _query = MutableStateFlow("")
    private val _suggestions = MutableStateFlow<List<AutocompleteItem>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SearchUiState> = MutableStateFlow(SearchUiState()).also { state ->
        viewModelScope.launch {
            combine(
                _query, _suggestions, _loading, _error
            ) { q, s, l, e -> SearchUiState(q, s, l, e) }
                .collect { state.value = it }
        }
    }.asStateFlow()

    val history: StateFlow<List<HistoryEntry>> = historyStore.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var focus: Pair<Double, Double>? = null

    private var searchJob: Job? = null

    init {
        _query
            .debounce(400L)
            .distinctUntilChanged()
            .onEach { text -> runSearch(text) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) {
        _query.value = q
        if (q.length < 2) {
            _suggestions.value = emptyList()
            _loading.value = false
        }
    }

    fun clearQuery() {
        _query.value = ""
        _suggestions.value = emptyList()
        _error.value = null
    }

    private fun runSearch(text: String) {
        searchJob?.cancel()
        if (text.length < 2) return
        searchJob = viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val focusStr = focus?.let { "${it.first},${it.second}" }
                _suggestions.value = api.autocomplete(
                    apikey = AppConfig.VIETMAP_API_KEY,
                    text = text,
                    focus = focusStr,
                )
            } catch (t: Throwable) {
                _error.value = t.message
                _suggestions.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun resolveAndRemember(item: AutocompleteItem): HistoryEntry? = try {
        val place = api.place(AppConfig.VIETMAP_API_KEY, item.refId)
        val entry = HistoryEntry(
            refId = item.refId,
            display = item.display ?: place.display ?: "",
            lat = place.lat,
            lng = place.lng,
        )
        historyStore.add(entry)
        entry
    } catch (t: Throwable) {
        _error.value = t.message
        null
    }

    fun pickFromHistory(entry: HistoryEntry) {
        viewModelScope.launch { historyStore.add(entry) }
    }
}
