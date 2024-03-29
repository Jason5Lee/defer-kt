package me.jason5lee.defer

/**
 * A defer scope. It is not thread-safe.
 */
public class DeferScope @PublishedApi internal constructor() {
    private var defers: ArrayList<() -> Unit>? = ArrayList()

    /**
     * Add a deferred task.
     *
     * @throws DeferScopeClosedException if out of scope or cancelled.
     */
    public fun defer(task: () -> Unit) {
        (defers ?: throw DeferScopeClosedException("DeferScope")).add(task)
    }

    /**
     * Add a deferred task which operates [this].
     *
     * @return [this]
     * @throws DeferScopeClosedException if out of scope or cancelled.
     */
    public inline fun <T> T.defer(crossinline task: T.() -> Unit): T {
        this@DeferScope.defer { task() }
        return this
    }

    /**
     * Cancel all deferred tasks.
     */
    public fun cancelDeferred() {
        defers = null
    }

    @PublishedApi
    internal fun runDeferred() {
        val defers = this.defers ?: return
        var exception: Throwable? = null
        for (i in (defers.size - 1) downTo 0) {
            try {
                defers[i]()
            } catch (e: Throwable) {
                if (exception == null) {
                    exception = e
                } else {
                    exception.addSuppressed(e)
                }
            }
        }
        this.defers = null
        if (exception != null) throw exception
    }

    @PublishedApi
    internal fun runDeferred(exception: Throwable): Nothing {
        this.defers?.let { defers ->
            for (i in (defers.size - 1) downTo 0) {
                try {
                    defers[i]()
                } catch (e: Throwable) {
                    exception.addSuppressed(e)
                }
            }
        }
        this.defers = null

        throw exception
    }
}

/**
 * Creates a defer scope. Within this scope, you can use `defer` to add deferred tasks.
 * When exiting the defer scope, these tasks are executed in reverse order of their addition.
 */
public inline fun <R> deferScope(block: DeferScope.() -> R): R {
    val scope = DeferScope()
    val result = try {
        block(scope)
    } catch (e: Throwable) {
        scope.runDeferred(e)
    }
    scope.runDeferred()
    return result
}
