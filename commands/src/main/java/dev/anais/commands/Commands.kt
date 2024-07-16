package dev.anais.commands

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex

abstract class CommandRunner<T> {
    /**
     * The state of the command.
     */
    var state by mutableStateOf<CommandStatus<T>>(CommandStatus.Idle)
        private set

    /**
     * The result of the command, either a value or the error thrown by the block.
     */
    val result get() = state.toResult()

    /**
     * Whether the command is currently running.
     */
    val isRunning get() = state is CommandStatus.Running


    /**
     * If the command has run but has failed, this will be true.
     */
    val hasFailed: Boolean get() = state is CommandStatus.Failure

    /**
     * If the command has run and has a value, this will be true.
     */
    val hasValue: Boolean get() = state is CommandStatus.Success<*>

    /**
     * If the command hasn't run yet, this will be true.
     */
    val notStarted: Boolean get() = state is CommandStatus.Idle

    /**
     * The result of the command, or an error if the command failed. If the command hasn't run yet, this will throw.
     */
    @Suppress("UNCHECKED_CAST")
    val require: T
        get() = requireNotNull(state as? CommandStatus.Success<*>) {
            "CommandRunner has no result"
        }.data as T

    private val runRequests = Channel<Unit>(1)
    private val mutex = Mutex()


    /**
     * Executes the command action and returns the result. You likely want tryRun instead.
     */
    suspend fun run() {
        if (!mutex.tryLock()) {
            error("CommandRunner is already running")
        }

        try {
            // replace this with a runRequests.receiveAsFlow().collectLatest {}
            // to cancel the last request in flight and replace it with a new one
            for (_dontcare in runRequests) {
                // Push a dummy item on the queue to prevent any other runs from queuing
                runRequests.send(Unit)

                state = CommandStatus.Running
                try {
                    runCatching { onCommand() }.also { result ->
                        // rethrow CancellationException to break the loop,
                        // but only if the runner's context is no longer active.
                        if (!currentCoroutineContext().isActive) {
                            val ex = result.exceptionOrNull()
                            if (ex is CancellationException) throw ex
                            // Throw our own if the action returned normally but the runner is cancelled
                            throw CancellationException(
                                "CommandRunner.run was cancelled",
                                cause = ex
                            )
                        }
                    }.onSuccess { data ->
                        state = CommandStatus.Success(data)
                    }.onFailure { throwable ->
                        state = CommandStatus.Failure(throwable)
                    }
                } catch (e: Throwable) {
                    state = CommandStatus.Failure(e)
                    throw e
                } finally {
                    runRequests.tryReceive()
                }
            }
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Tries to run the command action. If the command is already running, this does nothing.
     */
    fun tryRun() {
        runRequests.trySend(Unit) // ignore failed send
    }

    /*
     * Resets the command runner to its initial state, clearing any result.
     */
    fun reset() {
        state = CommandStatus.Idle
    }

    protected abstract suspend fun onCommand(): T
}

inline fun <T> CommandRunner(
    crossinline action: suspend () -> T
): CommandRunner<T> = object : CommandRunner<T>() {
    override suspend fun onCommand(): T = action()
}


/**
 * Initializes a [CommandRunner] for executing asynchronous operations within a Composable function.
 *
 * Sets up a [CommandRunner] instance to execute the asynchronous operation defined by the [action] lambda.
 * This is where you define the asynchronous operation that the [CommandRunner] will execute, and then you
 * will typically call `runner.tryRun()` to start the operation, then get the result in `runner.result`.
 *
 * @param T The type of the result produced by the asynchronous operation.
 * @param key An optional key to uniquely identify and remember the [CommandRunner] instance. Changing this key
 *            will result in the creation of a new [CommandRunner] instance.
 * @param action The suspend function to be executed, encapsulated within the [CommandRunner]. This function
 *               should define the asynchronous operation and return a result of type [T].
 * @return A [CommandRunner] instance
 */
@Composable
fun <T> rememberCommand(key: Any?, action: suspend () -> T): CommandRunner<T> {
    val runner = remember(key) { CommandRunner(action) }
    LaunchedEffect(runner) {
        runner.run()
    }

    return runner
}
