package pw.lowee.mvisample.ui.main

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pw.lowee.mvisample.EitherE
import pw.lowee.mvisample.data.DataSource
import pw.lowee.mvisample.models.GifObject

class MainRepository(private val dataSource: DataSource) {

    var searchJob: Job? = null

    suspend fun search(query: String, offset: Int, limit: Int): EitherE<List<GifObject>> {
        searchJob?.cancel()
        return coroutineScope {
            async {
                dataSource.search(
                    query,
                    offset,
                    limit
                )
            }.also { searchJob = it }
        }.await()
    }

}