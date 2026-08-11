package dev.anais.commands

sealed interface CommandStatus<out T> {
    data object Idle : CommandStatus<Nothing>
    data object Running : CommandStatus<Nothing>
    data class Failure(val throwable: Throwable) : CommandStatus<Nothing>
    data class Success<out T>(val data: T) : CommandStatus<Nothing>
}

@Suppress("UNCHECKED_CAST")
fun <T> CommandStatus<T>.toResult(): Result<T>? = when (val state = this) {
    is CommandStatus.Idle -> null
    is CommandStatus.Running -> null
    is CommandStatus.Failure -> Result.failure(state.throwable)
    is CommandStatus.Success<*> -> Result.success(state.data as T)
}
