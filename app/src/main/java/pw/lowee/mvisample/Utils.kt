package pw.lowee.mvisample


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

typealias EitherE<R> = Either<Exception, R>

sealed class Either<out E, out V> {

    companion object {
        inline fun <V> of(action: () -> V): Either<Exception, V> {
            return try {
                Right(action())
            } catch (ex: Exception) {
                ex.printStackTrace()
                Left(ex)
            }
        }
    }
}

data class Right<out R>(val v: R) : Either<Nothing, R>()
data class Left<out L>(val v: L) : Either<L, Nothing>()

infix fun <L, R, V> Either<L, R>.bind(io: (R) -> Either<L, V>): Either<L, V> = when (this) {
    is Right -> io(v)
    is Left -> this
}

suspend inline fun <R> io(
    dispatcher: CoroutineContext = Dispatchers.IO,
    crossinline block: suspend CoroutineScope.() -> R
): Either<Exception, R> = withContext(dispatcher) { Either.of { block() } }

fun <L, R> Either<L, R>.orNull(): R? = when (this) {
    is Right -> this.v
    else -> null
}