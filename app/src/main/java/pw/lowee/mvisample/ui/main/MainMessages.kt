package pw.lowee.mvisample.ui.main

import pw.lowee.mvikungfu.msgEffects
import pw.lowee.mvikungfu.msgState
import pw.lowee.mvisample.UserInput
import pw.lowee.mvisample.any
import pw.lowee.mvisample.models.GifObject

object MainMessages {

    fun msgLoadMore(): MainMsg = msgEffects(
        { s -> s.copy(loading = true) },
        { s -> listOf(MainEffects.effectLoadMore()).takeIf { !s.loading }.orEmpty() }
    )

    fun msgRefresh(): MainMsg = msgEffects(
        { it.copy(loading = true) },
        { listOf(MainEffects.effectSearch()) })

    fun msgQuery(query: UserInput<String>): MainMsg = msgState { it.copy(query = query) }

    fun msgSearch(): MainMsg = msgEffects(
        { s -> s.copy(loading = true, resultsQuery = s.query.any()) },
        { listOf(MainEffects.effectSearch()) }
    )

    fun msgFound(items: List<GifObject>): MainMsg = msgState { s -> s.copy(loading = false, results = items) }

    fun msgLoaded(items: List<GifObject>): MainMsg = msgState { s ->
        s.copy(loading = false, results = s.results + items)
    }

    fun msgFail(e: Exception): MainMsg = msgEffects(
        { it.copy(loading = false, results = emptyList()) },
        { listOf(MainEffects.effectFail(e)) }
    )
}