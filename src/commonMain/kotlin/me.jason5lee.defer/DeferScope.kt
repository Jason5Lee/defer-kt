package me.jason5lee.defer

internal fun throwDeferScopeClosed(): Nothing {
    throw IllegalStateException("DeferScope used out of scope or cancelled")
}

/**
 * A defer scope. It is not thread-safe.
 */
public expect class DeferScope @PublishedApi internal constructor() {
    /**
     * Add the code to the defer list.
     */
    public fun defer(task: () -> Unit)

    /**
     * Cancel all defer.
     */
    public fun cancelDefers()

    @PublishedApi
    internal fun runDefers()

    @PublishedApi
    internal fun runDefers(exception: Throwable): Nothing
}

/**
 * Create a deferScope, you can use defer to add code to the defer list. When the deferScope is exited,
 * the code is executed in reverse order.
 */
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
