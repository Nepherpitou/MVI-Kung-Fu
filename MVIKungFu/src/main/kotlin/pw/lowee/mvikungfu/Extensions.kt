package pw.lowee.mvikungfu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce


@ExperimentalCoroutinesApi
fun <T> CoroutineScope.windowed(channel: ReceiveChannel<T>, size: Int) = produce {
    var buffer = emptyList<T>()
    for (t in channel) {
        buffer = (buffer + t).takeLast(size)
        if (buffer.size == size) send(buffer)
    }
}

@ExperimentalCoroutinesApi
fun <T> CoroutineScope.windowPaired(channel: ReceiveChannel<T>) = produce {
    for (l in windowed(channel, 2)) send(l.first() to l.last())
}

@ExperimentalCoroutinesApi
fun <T> CoroutineScope.flatten(channel: ReceiveChannel<List<T>>) = produce {
    for (l in channel) l.forEach { send(it) }
}