package pw.lowee.mvikungfu

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

typealias ElmEffect<EffectCtx, State> = suspend ChannelWrapper<EffectCtx, State>.(EffectCtx, State) -> Unit
typealias ElmMsg<EffectCtx, State> = (State) -> Pair<State, List<ElmEffect<EffectCtx, State>>>
typealias RenderState<State> = (State) -> Unit
typealias RenderEquals<State> = (old: State, new: State) -> Boolean
typealias ElmRender<State> = Pair<RenderEquals<State>, RenderState<State>>


fun <EffectCtx, State> msgState(update: (State) -> State): ElmMsg<EffectCtx, State> =
    { update(it) to emptyList() }

fun <EffectCtx, State> msgEffects(
    update: (State) -> State,
    effects: (State) -> List<ElmEffect<EffectCtx, State>>
): ElmMsg<EffectCtx, State> = { update(it) to effects(it) }

fun <EffectCtx, State> msgEffect(effect: ElmEffect<EffectCtx, State>) =
    msgEffects<EffectCtx, State>({ it }, { listOf(effect) })

fun <EffectCtx, State> msgEmpty(): ElmMsg<EffectCtx, State> = msgState { it }

fun <State> render(
    sendIf: RenderEquals<State>,
    render: RenderState<State>
): ElmRender<State> = sendIf to render

fun <S, R> renderT(
    transform: (S) -> R,
    render: RenderState<R>,
    sendIf: RenderEquals<R> = { o, n -> o != n }
): ElmRender<S> = render(
    { o, n -> sendIf(transform(o), transform(n)) },
    { s -> render(transform(s)) }
)

interface ChannelWrapper<Ctx, State> {
    val messages: SendChannel<ElmMsg<Ctx, State>>
}

fun <S> renderWatcher(renders: List<ElmRender<S>>): suspend (S) -> Unit {
    var oldState: S? = null
    return { s ->
        when (val o = oldState) {
            null -> renders.forEach { (_, f) -> withContext(Dispatchers.Main) { f(s) } }
            else -> renders.filter { (f, _) -> f(o, s) }.forEach { (_, f) -> withContext(Dispatchers.Main) { f(s) } }
        }
        oldState = s
    }
}

fun <S> debugWatcher(log: (String) -> Unit): suspend (S) -> Unit = { log("New state is $it") }

suspend fun <S> watchState(controller: ElmController<*, S>, watchers: List<suspend (S) -> Unit>) {
    withContext(Dispatchers.Default) {
        for (s in controller.states) watchers.forEach { w -> w(s) }
    }
}

suspend fun <EffectCtx, State> renderStateEffect(
    rs: List<ElmRender<State>>
): ElmEffect<EffectCtx, State> = { _, s -> withContext(Dispatchers.Main) { rs.forEach { (_, r) -> r(s) } } }

fun <EffectCtx, State> defaultController(state: State, ctx: () -> EffectCtx): ElmController<EffectCtx, State> {
    return object : ElmController<EffectCtx, State>(state) {
        override fun effectContext(): EffectCtx = ctx()
    }
}

fun <C, S> CoroutineScope.sendMessage(controller: ElmController<C, S>, message: ElmMsg<C, S>) {
    launch { controller.messages.send(message) }
}

@Suppress("EXPERIMENTAL_API_USAGE")
abstract class ElmController<EffectCtx, State>(default: State) {

    protected val supervisor = SupervisorJob()
    protected val scope = CoroutineScope(Dispatchers.Default + supervisor)

    private var state: State = default
    private val _messages = Channel<ElmMsg<EffectCtx, State>>(Channel.UNLIMITED)
    private val _states = Channel<State>(Channel.UNLIMITED)

    val messages: SendChannel<ElmMsg<EffectCtx, State>> get() = _messages
    val states: ReceiveChannel<State> get() = _states

    fun start(init: ElmMsg<EffectCtx, State>) {
        scope.launch {
            launch { messages.send(init) }
            for (msg in _messages) {
                val (new, effects) = msg(state)
                if (new !== state) {
                    state = new
                    _states.send(new)
                }
                effects.forEach { launch { it(wrapContext(_messages), effectContext(), state) } }
            }
        }
    }

    fun stop() {
        supervisor.cancelChildren()
    }

    private fun wrapContext(channel: SendChannel<ElmMsg<EffectCtx, State>>) =
        object : ChannelWrapper<EffectCtx, State> {
            override val messages: SendChannel<ElmMsg<EffectCtx, State>> get() = channel
        }

    protected abstract fun effectContext(): EffectCtx
}