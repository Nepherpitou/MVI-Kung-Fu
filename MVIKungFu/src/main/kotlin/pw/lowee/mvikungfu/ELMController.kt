package pw.lowee.mvikungfu

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce

typealias Effect<EffectCtx, State> = suspend MessagesChannelProvider<EffectCtx, State>.(EffectCtx, State) -> Unit
typealias Msg<EffectCtx, State> = (State) -> Pair<State, List<Effect<EffectCtx, State>>>
typealias RenderState<View, State> = (View, State) -> Unit
typealias RenderEquals<State> = (old: State, new: State) -> Boolean
typealias Render<View, State> = Pair<RenderEquals<State>, RenderState<View, State>>


fun <EffectCtx, State> msgState(update: (State) -> State): Msg<EffectCtx, State> =
    { update(it) to emptyList() }

fun <EffectCtx, State> msgEffects(
    update: (State) -> State,
    effects: (State) -> List<Effect<EffectCtx, State>>
): Msg<EffectCtx, State> = { update(it) to effects(it) }

fun <EffectCtx, State> msgEffect(effect: Effect<EffectCtx, State>): Msg<EffectCtx, State> = { it to listOf(effect) }

fun <View, State> render(
    sendIf: RenderEquals<State>,
    render: RenderState<View, State>
): Render<View, State> = sendIf to render

fun <V, S, R> renderT(
    transform: (S) -> R,
    render: RenderState<V, R>,
    sendIf: RenderEquals<R> = { o, n -> o != n }
): Render<V, S> = render(
    { o, n -> sendIf(transform(o), transform(n)) },
    { v, s -> render(v, transform(s)) }
)

suspend fun <View, EffectCtx, State> renderStateEffect(
    view: View,
    renders: List<Render<View, State>>
): Effect<EffectCtx, State> =
    { _, state -> withContext(Dispatchers.Main) { renders.forEach { (_, r) -> r(view, state) } } }

@ExperimentalCoroutinesApi
fun <V, S> CoroutineScope.renderStates(
    states: ReceiveChannel<S>,
    renders: List<Render<V, S>>
): ReceiveChannel<(V) -> Unit> = flatten(
    produce {
        for ((o, n) in windowPaired(states)) send(
            renders.filter { (f, _) -> f(o, n) }.map { (_, r) -> { v: V -> r(v, n) } }
        )
    }
)

interface MessagesChannelProvider<EffectCtx, State> {
    val messages: SendChannel<Msg<EffectCtx, State>>
}

private fun <EffectCtx, State> wrap(
    channel: SendChannel<Msg<EffectCtx, State>>
): MessagesChannelProvider<EffectCtx, State> = object : MessagesChannelProvider<EffectCtx, State> {
    override val messages: SendChannel<Msg<EffectCtx, State>> get() = channel
}

@Suppress("EXPERIMENTAL_API_USAGE")
abstract class ElmController<EffectCtx, State>(default: State) {

    protected val supervisor = SupervisorJob()
    protected val scope = CoroutineScope(Dispatchers.Default + supervisor)

    private var state: State = default
    private val _messages = Channel<Msg<EffectCtx, State>>(Channel.UNLIMITED)
    private val _states = Channel<State>(Channel.UNLIMITED)

    val messages: SendChannel<Msg<EffectCtx, State>> get() = _messages
    val states: ReceiveChannel<State> get() = _states

    fun start() {
        scope.launch {
            for (msg in _messages) {
                val (new, effects) = msg(state)
                state = new
                _states.send(new)
                effects.forEach { launch { it(wrap(messages), effectContext(), state) } }
            }
        }
        messages.offer(init())
    }

    fun stop() {
        supervisor.cancelChildren()
    }

    protected abstract fun effectContext(): EffectCtx
    protected abstract fun init(): Msg<EffectCtx, State>
}