package me.jason5lee.defer

internal fun throwDeferScopeClosed(): Nothing {
    throw IllegalStateException("DeferScope used out of scope or cancelled")
}

public expect class DeferScope @PublishedApi internal constructor() {
    public fun defer(task: () -> Unit)
    public fun cancelDefers()

    @PublishedApi
    internal fun runDefers()

    @PublishedApi
    internal fun runDefers(exception: Throwable): Nothing
}

public inline fun <R> deferScope(block: DeferScope.() -> R): R {
    val scope = DeferScope()
    val result = try {
        block(scope)
    } catch (e: Throwable) {
        scope.runDefers(e)
    }
    scope.runDefers()
    return result
}
