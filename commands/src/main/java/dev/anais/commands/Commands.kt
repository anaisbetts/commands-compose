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
    var result by mutableStateOf<Result<T>?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    private val runRequests = Channel<Unit>(1)
    private val mutex = Mutex()

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

                isRunning = true
                try {
                    result = runCatching { onCommand() }.also { result ->
                        // rethrow CancellationException to break the loop,
                        // but only if the runner's context is no longer active.
                        if (!currentCoroutineContext().isActive) {
                            val ex = result.exceptionOrNull()
                            if (ex is CancellationException) throw ex
                            // Throw our own if the action returned normally but the runner is cancelled
                            throw CancellationException("CommandRunner.run was cancelled", cause = ex)
                        }

                    }
                } finally {
                    isRunning = false
                    runRequests.tryReceive()
                }
            }
        } finally {
            mutex.unlock()
        }
    }

    fun tryRun() {
        runRequests.trySend(Unit) // ignore failed send
    }

    protected abstract suspend fun onCommand(): T
}

inline fun <T> CommandRunner(
    crossinline action: suspend () -> T
): CommandRunner<T> = object : CommandRunner<T>() {
    override suspend fun onCommand(): T = action()
}

@Composable
fun <T> rememberCommand(key: Any?, action: suspend () -> T): CommandRunner<T> {
    val runner = remember(key) { CommandRunner(action) }
    LaunchedEffect(runner) {
        runner.run()
    }

    return runner
}