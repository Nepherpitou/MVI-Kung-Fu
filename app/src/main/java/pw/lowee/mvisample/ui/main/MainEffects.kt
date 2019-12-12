package pw.lowee.mvisample.ui.main

import pw.lowee.mvisample.Left
import pw.lowee.mvisample.Right

object MainEffects {


    fun effectSearch(): MainEffect = { ctx, s ->
        val msg = when (val r = ctx.repository.search(s.resultsQuery, 0, 20)) {
            is Right -> MainMessages.msgFound(r.v)
            is Left -> MainMessages.msgFail(r.v)
        }
        messages.send(msg)
    }

    fun effectFail(e: Exception): MainEffect = { ctx, _ -> ctx.errorToast(e) }

    fun effectLoadMore(): MainEffect = { c, s ->
        val msg = when (val r = c.repository.search(s.resultsQuery, s.results.count(), 20)) {
            is Right -> MainMessages.msgLoaded(r.v)
            is Left -> MainMessages.msgFail(r.v)
        }
        messages.send(msg)
    }
}